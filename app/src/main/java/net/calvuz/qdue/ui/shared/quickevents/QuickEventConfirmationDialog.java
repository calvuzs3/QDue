package net.calvuz.qdue.ui.shared.quickevents;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * QuickEventConfirmationDialog - Preview and confirm quick event creation
 *
 * FEATURES:
 * ✅ Event preview with template data
 * ✅ Editable time selection for timed events
 * ✅ All-day toggle
 * ✅ Priority and type display
 * ✅ Real database persistence via EventDao
 * ✅ User feedback and error handling
 * ✅ Async operations with proper threading
 *
 * WORKFLOW:
 * 1. Show event preview based on QuickEventTemplate
 * 2. Allow user to modify times/details if needed
 * 3. Confirm and save to database via EventDao
 * 4. Provide feedback and trigger refresh
 */
public class QuickEventConfirmationDialog {

    private static final String TAG = "QuickEventDialog";

    private final Context mContext;
    private final ToolbarAction mAction;
    private final LocalDate mDate;
    private final Long mUserId;
    private final QuickEventTemplate mTemplate;
    private final QuickEventTemplate.EventPreview mPreview;
    private final EventCreationListener mListener;

    // Database components
    private final EventDao mEventDao;

    // Dialog components
    private AlertDialog mDialog;
    private View mDialogView;

    // UI Components
    private TextInputEditText mTitleEdit;
    private TextInputEditText mDescriptionEdit;
    private MaterialSwitch mAllDaySwitch;
    private MaterialButton mStartTimeButton;
    private MaterialButton mEndTimeButton;
    private Chip mEventTypeChip;
    private Chip mPriorityChip;
    private MaterialButton mConfirmButton;
    private MaterialButton mCancelButton;

    // Event data (mutable during editing)
    private LocalEvent mEventData;
    private LocalTime mStartTime;
    private LocalTime mEndTime;
    private boolean mAllDay;

    // ==================== INTERFACE DEFINITIONS ====================

    /**
     * Callback interface for event creation results
     */
    public interface EventCreationListener {
        /**
         * Called when event is successfully created and saved
         */
        void onEventCreated(LocalEvent createdEvent, ToolbarAction sourceAction, LocalDate date);

        /**
         * Called when event creation is cancelled by user
         */
        void onEventCreationCancelled(ToolbarAction sourceAction, LocalDate date);

        /**
         * Called when event creation fails
         */
        void onEventCreationFailed(ToolbarAction sourceAction, LocalDate date, String errorMessage);

        /**
         * Called when events need to be refreshed (after successful creation)
         */
        void onEventsRefreshNeeded();
    }

    // ==================== CONSTRUCTOR ====================

    /**
     * Create confirmation dialog for quick event
     */
    public QuickEventConfirmationDialog(@NonNull Context context,
                                        @NonNull ToolbarAction action,
                                        @NonNull LocalDate date,
                                        Long userId,
                                        @NonNull QuickEventTemplate template,
                                        @NonNull EventCreationListener listener) {
        this.mContext = context;
        this.mAction = action;
        this.mDate = date;
        this.mUserId = userId;
        this.mTemplate = template;
        this.mListener = listener;
        this.mPreview = template.getPreview(date);

        // Initialize database
        this.mEventDao = QDueDatabase.getInstance(context).eventDao();

        // Initialize event data from template
        initializeEventData();

        Log.d(TAG, "QuickEventConfirmationDialog created for " + action + " on " + date);
    }

    /**
     * Initialize mutable event data from template
     */
    private void initializeEventData() {
        try {
            // Create event from template (this validates business rules)
            mEventData = mTemplate.createEvent(mDate, mUserId);

            // Extract mutable properties
            mAllDay = mEventData.isAllDay();
            if (!mAllDay && mEventData.getStartTime() != null) {
                mStartTime = mEventData.getStartTime().toLocalTime();
                mEndTime = mEventData.getEndTime() != null ?
                        mEventData.getEndTime().toLocalTime() : mStartTime.plusHours(8);
            } else {
                // Default times for conversion from all-day
                mStartTime = mTemplate.getEventType().getDefaultStartTime();
                mEndTime = mTemplate.getEventType().getDefaultEndTime();
            }

            Log.d(TAG, "Event data initialized: " + mEventData.getTitle() +
                    " (allDay=" + mAllDay + ", start=" + mStartTime + ")");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize event data: " + e.getMessage());
            throw new RuntimeException("Cannot create event preview", e);
        }
    }

    // ==================== DIALOG LIFECYCLE ====================

    /**
     * Show the confirmation dialog
     */
    public void show() {
        Log.d(TAG, "Showing confirmation dialog for " + mAction);

        try {
            createDialog();
            setupUIComponents();
            populateUIWithEventData();
            setupEventListeners();

            mDialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to show confirmation dialog: " + e.getMessage());
            mListener.onEventCreationFailed(mAction, mDate, "Errore nell'apertura del dialog: " + e.getMessage());
        }
    }

    /**
     * Dismiss the dialog
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    /**
     * Create the AlertDialog with custom layout
     */
    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mDialogView = inflater.inflate(R.layout.dialog_quick_event_confirmation, null);

        String title = "Conferma " + mTemplate.getDisplayName();
        String subtitle = "Data: " + formatDateForDisplay(mDate);

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setMessage(subtitle)
                .setView(mDialogView)
                .setCancelable(true)
                .setOnCancelListener(dialog -> {
                    Log.d(TAG, "Dialog cancelled by user");
                    mListener.onEventCreationCancelled(mAction, mDate);
                })
                .create();
    }

    /**
     * Setup UI component references
     */
    private void setupUIComponents() {
        // Text fields
        mTitleEdit = mDialogView.findViewById(R.id.edit_event_title);
        mDescriptionEdit = mDialogView.findViewById(R.id.edit_event_description);

        // Time controls
        mAllDaySwitch = mDialogView.findViewById(R.id.switch_all_day);
        mStartTimeButton = mDialogView.findViewById(R.id.button_start_time);
        mEndTimeButton = mDialogView.findViewById(R.id.button_end_time);

        // Info chips
        mEventTypeChip = mDialogView.findViewById(R.id.chip_event_type);
        mPriorityChip = mDialogView.findViewById(R.id.chip_priority);

        // Action buttons
        mConfirmButton = mDialogView.findViewById(R.id.button_confirm);
        mCancelButton = mDialogView.findViewById(R.id.button_cancel);

        Log.d(TAG, "UI components setup completed");
    }

    /**
     * Populate UI with event data from template
     */
    private void populateUIWithEventData() {
        // Basic info
        mTitleEdit.setText(mEventData.getTitle());
        mDescriptionEdit.setText(mEventData.getDescription());

        // Time settings
        mAllDaySwitch.setChecked(mAllDay);
        updateTimeButtonsVisibility();
        updateTimeButtonsText();

        // Event type and priority chips
        EventType eventType = mEventData.getEventType();
        if (eventType != null) {
            mEventTypeChip.setText(eventType.getDisplayName());
            mEventTypeChip.setChipIconResource(getEventTypeIcon(eventType));
            mEventTypeChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(eventType.getColor()));
        }

        EventPriority priority = mEventData.getPriority();
        if (priority != null) {
            mPriorityChip.setText(getPriorityDisplayName(priority));
            mPriorityChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getPriorityColor(priority)));
        }

        // Action buttons
        String confirmText = "Crea " + mTemplate.getDisplayName();
        mConfirmButton.setText(confirmText);

        Log.d(TAG, "UI populated with event data");
    }

    /**
     * Setup event listeners for UI interactions
     */
    private void setupEventListeners() {
        // All-day toggle
        mAllDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAllDay = isChecked;
            updateTimeButtonsVisibility();
            Log.d(TAG, "All-day toggled: " + isChecked);
        });

        // Time buttons
        mStartTimeButton.setOnClickListener(v -> showStartTimePicker());
        mEndTimeButton.setOnClickListener(v -> showEndTimePicker());

        // Action buttons
        mConfirmButton.setOnClickListener(v -> handleConfirmAction());
        mCancelButton.setOnClickListener(v -> handleCancelAction());

        Log.d(TAG, "Event listeners setup completed");
    }

    // ==================== TIME MANAGEMENT ====================

    /**
     * Update visibility of time buttons based on all-day setting
     */
    private void updateTimeButtonsVisibility() {
        int visibility = mAllDay ? View.GONE : View.VISIBLE;
        mStartTimeButton.setVisibility(visibility);
        mEndTimeButton.setVisibility(visibility);
    }

    /**
     * Update text on time buttons
     */
    private void updateTimeButtonsText() {
        if (mStartTime != null) {
            mStartTimeButton.setText("Inizio: " + formatTimeForDisplay(mStartTime));
        }
        if (mEndTime != null) {
            mEndTimeButton.setText("Fine: " + formatTimeForDisplay(mEndTime));
        }
    }

    /**
     * Show start time picker
     */
    private void showStartTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(mStartTime.getHour())
                .setMinute(mStartTime.getMinute())
                .setTitleText("Seleziona ora di inizio")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            mStartTime = LocalTime.of(picker.getHour(), picker.getMinute());

            // Auto-adjust end time if it's before start time
            if (mEndTime.isBefore(mStartTime)) {
                mEndTime = mStartTime.plusHours(8); // Default 8-hour duration
            }

            updateTimeButtonsText();
            Log.d(TAG, "Start time updated: " + mStartTime);
        });

        // Show picker if we have fragment manager access
        try {
            if (mContext instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.fragment.app.FragmentManager fragmentManager =
                        ((androidx.appcompat.app.AppCompatActivity) mContext).getSupportFragmentManager();
                picker.show(fragmentManager, "start_time_picker");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not show time picker: " + e.getMessage());
            // Fallback to simple time input dialog could be implemented here
            showSimpleTimeInputDialog(true);
        }
    }

    /**
     * Show end time picker
     */
    private void showEndTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(mEndTime.getHour())
                .setMinute(mEndTime.getMinute())
                .setTitleText("Seleziona ora di fine")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            LocalTime newEndTime = LocalTime.of(picker.getHour(), picker.getMinute());

            // Validate end time is after start time
            if (newEndTime.isAfter(mStartTime)) {
                mEndTime = newEndTime;
                updateTimeButtonsText();
                Log.d(TAG, "End time updated: " + mEndTime);
            } else {
                Toast.makeText(mContext, "L'ora di fine deve essere successiva all'ora di inizio",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Show picker if we have fragment manager access
        try {
            if (mContext instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.fragment.app.FragmentManager fragmentManager =
                        ((androidx.appcompat.app.AppCompatActivity) mContext).getSupportFragmentManager();
                picker.show(fragmentManager, "end_time_picker");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not show time picker: " + e.getMessage());
            showSimpleTimeInputDialog(false);
        }
    }

    /**
     * Fallback simple time input dialog
     */
    private void showSimpleTimeInputDialog(boolean isStartTime) {
        // Simple fallback implementation using basic TimePickerDialog
        LocalTime currentTime = isStartTime ? mStartTime : mEndTime;
        String title = isStartTime ? "Ora di inizio" : "Ora di fine";

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                mContext,
                (view, hourOfDay, minute) -> {
                    LocalTime newTime = LocalTime.of(hourOfDay, minute);

                    if (isStartTime) {
                        mStartTime = newTime;
                        if (mEndTime.isBefore(mStartTime)) {
                            mEndTime = mStartTime.plusHours(8);
                        }
                    } else {
                        if (newTime.isAfter(mStartTime)) {
                            mEndTime = newTime;
                        } else {
                            Toast.makeText(mContext, "L'ora di fine deve essere successiva all'ora di inizio",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    updateTimeButtonsText();
                    Log.d(TAG, (isStartTime ? "Start" : "End") + " time updated: " + newTime);
                },
                currentTime.getHour(),
                currentTime.getMinute(),
                true // 24-hour format
        );

        timePickerDialog.setTitle(title);
        timePickerDialog.show();
    }

    // ==================== ACTION HANDLERS ====================

    /**
     * Handle confirm button click - create and save event
     */
    private void handleConfirmAction() {
        Log.d(TAG, "Confirm action triggered");

        try {
            // Update event data with user modifications
            updateEventDataFromUI();

            // Validate the final event
            if (!validateEventData()) {
                return; // Validation failed, stay in dialog
            }

            // Disable confirm button to prevent double-clicks
            mConfirmButton.setEnabled(false);
            mConfirmButton.setText("Creazione...");

            // Save event to database asynchronously
            saveEventToDatabase();

        } catch (Exception e) {
            Log.e(TAG, "Error in confirm action: " + e.getMessage());
            mListener.onEventCreationFailed(mAction, mDate, "Errore nella conferma: " + e.getMessage());
            dismiss();
        }
    }

    /**
     * Handle cancel button click
     */
    private void handleCancelAction() {
        Log.d(TAG, "Cancel action triggered");
        mListener.onEventCreationCancelled(mAction, mDate);
        dismiss();
    }

    /**
     * Update event data with user modifications from UI
     */
    private void updateEventDataFromUI() {
        // Update basic fields
        String title = mTitleEdit.getText() != null ? mTitleEdit.getText().toString().trim() : "";
        String description = mDescriptionEdit.getText() != null ? mDescriptionEdit.getText().toString().trim() : "";

        if (!title.isEmpty()) {
            mEventData.setTitle(title);
        }

        if (!description.isEmpty()) {
            mEventData.setDescription(description);
        }

        // Update timing
        mEventData.setAllDay(mAllDay);

        if (mAllDay) {
            mEventData.setStartTime(mDate.atStartOfDay());
            mEventData.setEndTime(mDate.atTime(23, 59, 59));
        } else {
            mEventData.setStartTime(mDate.atTime(mStartTime));
            mEventData.setEndTime(mDate.atTime(mEndTime));
        }

        // Update modification timestamp
        mEventData.setLastUpdated(LocalDateTime.now());

        Log.d(TAG, "Event data updated from UI");
    }

    /**
     * Validate event data before saving
     */
    private boolean validateEventData() {
        // Title validation
        if (mEventData.getTitle() == null || mEventData.getTitle().trim().isEmpty()) {
            Toast.makeText(mContext, "Il titolo dell'evento è obbligatorio", Toast.LENGTH_SHORT).show();
            mTitleEdit.requestFocus();
            return false;
        }

        // Time validation for timed events
        if (!mAllDay) {
            if (mStartTime == null || mEndTime == null) {
                Toast.makeText(mContext, "Orari di inizio e fine sono obbligatori", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!mStartTime.isBefore(mEndTime)) {
                Toast.makeText(mContext, "L'ora di fine deve essere successiva all'ora di inizio", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // EventType-specific validation
        EventType eventType = mEventData.getEventType();
        if (eventType != null) {
            if (!eventType.isValidConfiguration(mAllDay, mStartTime, mEndTime)) {
                Log.w(TAG, "Event configuration may not be optimal for event type: " + eventType);
                // Don't block creation, just warn
            }
        }

        Log.d(TAG, "Event data validation passed");
        return true;
    }

    /**
     * Save event to database asynchronously
     */
    private void saveEventToDatabase() {
        Log.d(TAG, "Saving event to database: " + mEventData.getId());

        // Use CompletableFuture for async database operation
        CompletableFuture.runAsync(() -> {
            try {
                // Save to database
                long rowId = mEventDao.insertEvent(mEventData);

                Log.d(TAG, "Event saved to database with rowId: " + rowId);

                // Switch back to main thread for UI updates
                if (mContext instanceof androidx.appcompat.app.AppCompatActivity) {
                    ((androidx.appcompat.app.AppCompatActivity) mContext).runOnUiThread(() -> {
                        onEventSavedSuccessfully();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to save event to database: " + e.getMessage());

                // Switch back to main thread for error handling
                if (mContext instanceof androidx.appcompat.app.AppCompatActivity) {
                    ((androidx.appcompat.app.AppCompatActivity) mContext).runOnUiThread(() -> {
                        onEventSaveFailed(e);
                    });
                }
            }
        });
    }

    /**
     * Called on main thread when event is saved successfully
     */
    private void onEventSavedSuccessfully() {
        Log.d(TAG, "Event saved successfully, notifying listener");

        // Dismiss dialog
        dismiss();

        // Notify listener
        mListener.onEventCreated(mEventData, mAction, mDate);
        mListener.onEventsRefreshNeeded();

        // Show success feedback
        Toast.makeText(mContext,
                "✅ Evento '" + mEventData.getTitle() + "' creato con successo",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Called on main thread when event save fails
     */
    private void onEventSaveFailed(Exception error) {
        Log.e(TAG, "Event save failed, notifying listener");

        // Re-enable confirm button
        mConfirmButton.setEnabled(true);
        mConfirmButton.setText("Crea " + mTemplate.getDisplayName());

        // Notify listener
        String errorMessage = "Errore nel salvataggio: " + error.getMessage();
        mListener.onEventCreationFailed(mAction, mDate, errorMessage);

        // Show error feedback
        Toast.makeText(mContext, "❌ " + errorMessage, Toast.LENGTH_LONG).show();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Format date for display
     */
    private String formatDateForDisplay(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        return date.format(formatter);
    }

    /**
     * Format time for display
     */
    private String formatTimeForDisplay(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    /**
     * Get icon resource for event type
     */
    private int getEventTypeIcon(EventType eventType) {
        switch (eventType) {
            case VACATION: return R.drawable.ic_rounded_beach_access_24;
            case SICK_LEAVE: return R.drawable.ic_rounded_local_hospital_24;
            case OVERTIME: return R.drawable.ic_rounded_engineering_24;
            case PERSONAL_LEAVE: return R.drawable.ic_rounded_schedule_24;
            case SPECIAL_LEAVE: return R.drawable.ic_rounded_accessible_24;
            case SYNDICATE_LEAVE: return R.drawable.ic_rounded_clock_loader_40_24;
            default: return R.drawable.ic_rounded_event_24;
        }
    }

    /**
     * Get display name for priority
     */
    private String getPriorityDisplayName(EventPriority priority) {
        switch (priority) {
            case HIGH: return "Alta";
            case URGENT: return "Urgente";
            case LOW: return "Bassa";
            case NORMAL:
            default: return "Normale";
        }
    }

    /**
     * Get color for priority
     */
    private int getPriorityColor(EventPriority priority) {
        switch (priority) {
            case URGENT: return android.graphics.Color.parseColor("#D32F2F");
            case HIGH: return android.graphics.Color.parseColor("#F57C00");
            case LOW: return android.graphics.Color.parseColor("#616161");
            case NORMAL:
            default: return android.graphics.Color.parseColor("#1976D2");
        }
    }

    // ==================== STATIC FACTORY METHODS ====================

    /**
     * Create and show confirmation dialog for quick event
     */
    public static void showConfirmationDialog(@NonNull Context context,
                                              @NonNull ToolbarAction action,
                                              @NonNull LocalDate date,
                                              Long userId,
                                              @NonNull QuickEventTemplate template,
                                              @NonNull EventCreationListener listener) {
        try {
            QuickEventConfirmationDialog dialog = new QuickEventConfirmationDialog(
                    context, action, date, userId, template, listener);
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create confirmation dialog: " + e.getMessage());
            listener.onEventCreationFailed(action, date, "Errore nell'apertura del dialog: " + e.getMessage());
        }
    }

    /**
     * Check if confirmation dialog can be shown for given template
     */
    public static boolean canShowConfirmationDialog(@NonNull QuickEventTemplate template,
                                                    @NonNull LocalDate date,
                                                    Long userId) {
        return template.isValid() && template.canUseOnDate(date, userId);
    }
}