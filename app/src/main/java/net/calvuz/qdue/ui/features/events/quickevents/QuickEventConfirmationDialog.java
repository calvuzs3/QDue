package net.calvuz.qdue.ui.features.events.quickevents;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.infrastructure.di.Injectable;
import net.calvuz.qdue.core.infrastructure.di.ServiceProvider;
import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.models.EventPreview;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;
import net.calvuz.qdue.core.infrastructure.services.models.QuickEventRequest;
import net.calvuz.qdue.core.domain.events.actions.EventAction;
import net.calvuz.qdue.core.domain.events.actions.EventActionManager;
import net.calvuz.qdue.core.domain.events.models.EventPriority;
import net.calvuz.qdue.core.domain.events.models.EventType;
import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * PHASE 2: QuickEventConfirmationDialog Refactored - DI Integration
 * <p>
 * REFACTORED VERSION:
 * - ❌ REMOVED: Direct database access (EventDao)
 * - ❌ REMOVED: Direct database instance creation
 * - ✅ ADDED: Dependency injection support (Injectable)
 * - ✅ ADDED: Service-based event creation
 * - ✅ ADDED: Proper error handling with OperationResult
 * - ✅ ADDED: Async operations with CompletableFuture
 * - ✅ KEPT: All UI functionality and user experience
 * <p>
 * FEATURES:
 * - Service-based event creation with validation
 * - Dependency injection compliant
 * - Consistent error handling
 * - Automatic backup through services
 * - Thread-safe operations
 * - Clean separation of UI and business logic
 */
public class QuickEventConfirmationDialog implements Injectable {

    private static final String TAG = "QuickEventDialog";

    private static final String QUICK_EVENT_CONFIRMATION_DIALOG_VERSION = "2.2";

    // ==================== DEPENDENCIES (DI) ====================

    // ✅ Service dependencies (injected)
    private EventsService mEventsService;
    private UserService mUserService;

    // ==================== COMPONENT PROPERTIES ====================

    // ✅ UI-focused properties
    private final Context mContext;
    private final QuickEventTemplate mTemplate;
    private final EventCreationListener mListener;
    private final LocalDate mDate;
    private final Long mUserId;

    // ✅ UI state (no business logic)
    private EventPreview mPreview;
    private AlertDialog mDialog;
    private View mDialogView;

    // ==================== UI COMPONENTS ====================

    private TextInputEditText mTitleEdit;
    private TextInputEditText mDescriptionEdit;
    private MaterialSwitch mAllDaySwitch;
    private MaterialButton mStartTimeButton;
    private MaterialButton mEndTimeButton;
    private Chip mEventTypeChip;
    private Chip mPriorityChip;
    private MaterialButton mConfirmButton;
    private MaterialButton mCancelButton;

    // ==================== MUTABLE UI DATA ====================

    private String mCurrentTitle;
    private String mCurrentDescription;
    private boolean mCurrentAllDay;
    private LocalTime mCurrentStartTime;
    private LocalTime mCurrentEndTime;

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
                                        @NonNull ServiceProvider serviceProvider,
                                        @NonNull QuickEventTemplate template,
                                        @NonNull LocalDate date,
                                        @Nullable Long userId,
                                        @NonNull EventCreationListener listener) {
        this.mContext = context;
        this.mTemplate = template;
        this.mDate = date;
        this.mUserId = userId;
        this.mListener = listener;

        // ✅ Inject dependencies
        inject(serviceProvider);

        if (!areDependenciesReady()) {
            throw new RuntimeException("QuickEventConfirmationDialog: Dependencies not ready");
        }

        // ✅ Initialize UI preview (no business logic)
        initializePreview();

        Log.d(TAG, "QuickEventConfirmationDialog created with DI for " + template.getDisplayName() + " on " + date);
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(ServiceProvider serviceProvider) {
        mEventsService = serviceProvider.getEventsService();
        mUserService = serviceProvider.getUserService();

        Log.d(TAG, "Dependencies injected successfully");
    }

    @Override
    public boolean areDependenciesReady() {
        return mEventsService != null && mUserService != null;
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize UI preview from template
     */
    private void initializePreview() {
        try {
            mPreview = mTemplate.getPreview(mDate);

            // ✅ Initialize mutable UI data from preview
            mCurrentTitle = mPreview.getTitle();
            mCurrentDescription = mPreview.getDescription();
            mCurrentAllDay = mPreview.isAllDay();
            mCurrentStartTime = mPreview.getStartTime();
            mCurrentEndTime = mPreview.getEndTime();

            Log.d(TAG, "Preview initialized: " + mCurrentTitle + " (" + (mCurrentAllDay ? "all-day" : "timed") + ")");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize preview: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize dialog preview", e);
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Show the confirmation dialog
     */
    public void show() {
        try {
            createDialog();
            if (mDialog != null) {
                mDialog.show();
                Log.d(TAG, "Dialog shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show dialog: " + e.getMessage(), e);
            mListener.onEventCreationFailed(mTemplate.getSourceAction(), mDate, "Failed to show dialog");
        }
    }

    /**
     * Dismiss the dialog
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            Log.d(TAG, "Dialog dismissed");
        }
    }

    // ==================== DIALOG CREATION ====================

    /**
     * Create the confirmation dialog
     */
    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mDialogView = inflater.inflate(R.layout.dialog_quick_event_confirmation, null);

        initializeViews();
        setupViewsWithPreview();
        setupClickListeners();

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.dialog_quick_action_confirm_title)
                .setView(mDialogView)
                .setCancelable(true)
                .setOnCancelListener(dialog -> handleDialogCancellation())
                .create();
    }

    /**
     * Initialize all view references
     */
    private void initializeViews() {
        mTitleEdit = mDialogView.findViewById(R.id.edit_event_title); // edit_title
        mDescriptionEdit = mDialogView.findViewById(R.id.edit_event_description); // edit_description
        mAllDaySwitch = mDialogView.findViewById(R.id.switch_all_day);
        mStartTimeButton = mDialogView.findViewById(R.id.button_start_time);
        mEndTimeButton = mDialogView.findViewById(R.id.button_end_time);
        mEventTypeChip = mDialogView.findViewById(R.id.chip_event_type);
        mPriorityChip = mDialogView.findViewById(R.id.chip_priority);
        mConfirmButton = mDialogView.findViewById(R.id.button_confirm);
        mCancelButton = mDialogView.findViewById(R.id.button_cancel);
    }

    /**
     * Setup views with preview data
     */
    private void setupViewsWithPreview() {
        // ✅ Basic information
        mTitleEdit.setText(mCurrentTitle);
        mDescriptionEdit.setText(mCurrentDescription);

        // ✅ Event type and priority (read-only)
        mEventTypeChip.setText(mPreview.getEventTypeDisplayName());
        mPriorityChip.setText(mPreview.getPriorityDisplayName());

        // Event type and priority chips
        EventType eventType = mPreview.getEventType();
        if (eventType != null) {
            mEventTypeChip.setText(eventType.getDisplayName());
            mEventTypeChip.setChipIconResource(getEventTypeIcon(eventType));
            mEventTypeChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                    eventType.getColor()));
        }

        EventPriority priority = mPreview.getPriority();
        if (priority != null) {
            mPriorityChip.setText(priority.getDisplayName());
            mPriorityChip.setChipBackgroundColor( android.content.res.ColorStateList.valueOf(
                    priority.getColor()));
        }
        // ✅ All-day toggle
        mAllDaySwitch.setChecked(mCurrentAllDay);
        updateTimeButtonsVisibility();

        // ✅ Time buttons
        updateTimeButtons();
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // ✅ All-day toggle
        mAllDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCurrentAllDay = isChecked;
            updateTimeButtonsVisibility();
            Log.d(TAG, "All-day toggle changed: " + isChecked);
        });

        // ✅ Start time button
        mStartTimeButton.setOnClickListener(v -> showStartTimePicker());

        // ✅ End time button
        mEndTimeButton.setOnClickListener(v -> showEndTimePicker());

        // ✅ Confirm button
        mConfirmButton.setOnClickListener(v -> handleConfirmation());

        // ✅ Cancel button
        mCancelButton.setOnClickListener(v -> handleCancellation());
    }

    // ==================== UI UPDATE METHODS ====================

    /**
     * Update time buttons visibility based on all-day setting
     */
    private void updateTimeButtonsVisibility() {
        mStartTimeButton.setEnabled(!mCurrentAllDay);
        mEndTimeButton.setEnabled(!mCurrentAllDay);
    }

    /**
     * Update time button labels
     */
    private void updateTimeButtons() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        if (mCurrentStartTime != null) {
            mStartTimeButton.setText(mCurrentStartTime.format(formatter));
        } else {
            mStartTimeButton.setText(R.string.text_select_hour_begin);
        }

        if (mCurrentEndTime != null) {
            mEndTimeButton.setText( mCurrentEndTime.format(formatter));
        } else {
            mEndTimeButton.setText(R.string.text_select_hour_end);
        }
    }

    // ==================== TIME PICKER METHODS ====================

    /**
     * Show start time picker
     */
    private void showStartTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(mCurrentStartTime.getHour())
                .setMinute(mCurrentStartTime.getMinute())
                .setTitleText("Seleziona ora di inizio")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            mCurrentStartTime = LocalTime.of(picker.getHour(), picker.getMinute());

            // Auto-adjust end time if it's before start time
            if (mCurrentEndTime.isBefore(mCurrentStartTime)) {
                mCurrentEndTime = mCurrentStartTime.plusHours(8); // Default 8-hour duration
            }

            updateTimeButtonsText();
            Log.d(TAG, "Start time updated: " + mCurrentStartTime);
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
                .setHour(mCurrentEndTime.getHour())
                .setMinute(mCurrentEndTime.getMinute())
                .setTitleText("Seleziona ora di fine")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            LocalTime newEndTime = LocalTime.of(picker.getHour(), picker.getMinute());

            // Validate end time is after start time
            if (newEndTime.isAfter(mCurrentStartTime)) {
                mCurrentEndTime = newEndTime;
                updateTimeButtonsText();
                Log.d(TAG, "End time updated: " + mCurrentEndTime);
            } else {
                Library.showWarning(mContext, R.string.long_text_finish_hour_must_be_post_begin_hour,
                        Toast.LENGTH_SHORT);
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

    // ==================== EVENT CREATION (SERVICE-BASED) ====================

    /**
     * ✅ Handle confirmation - Service-based event creation
     */
    private void handleConfirmation() {
        if (!areDependenciesReady()) {
            handleCreationError("Services not available");
            return;
        }

        try {
            // ✅ Get current UI values
            updateCurrentValuesFromUI();

            // ✅ Validate UI input
            if (!validateUIInput()) {
                return; // Error already shown
            }

            // ✅ Create request from UI input
            QuickEventRequest request = createRequestFromUI();

            // ✅ Show loading state
            setLoadingState(true);

            // ✅ Use service for creation
            mEventsService.createQuickEvent(request)
                    .thenAccept(this::handleCreationResult)
                    .exceptionally(this::handleCreationException);

        } catch (Exception e) {
            Log.e(TAG, "Error during confirmation: " + e.getMessage(), e);
            handleCreationError(MessageFormat.format(
                    mContext.getString(R.string.format_text_error_creating_event_semicolon_0), e.getMessage()));
        }
    }

    /**
     * Update current values from UI inputs
     */
    private void updateCurrentValuesFromUI() {
        mCurrentTitle = mTitleEdit.getText() != null ? mTitleEdit.getText().toString().trim() : "";
        mCurrentDescription = mDescriptionEdit.getText() != null ? mDescriptionEdit.getText().toString().trim() : "";

        // Use template defaults if UI values are empty
        if (mCurrentTitle.isEmpty()) {
            mCurrentTitle = mTemplate.getDisplayName();
        }
    }

    /**
     * ✅ UI validation (no business logic)
     */
    private boolean validateUIInput() {
        // Title validation
        if (mCurrentTitle.isEmpty()) {
            mTitleEdit.setError("Il titolo è obbligatorio");
            mTitleEdit.requestFocus();
            return false;
        }

        // Time validation for timed events
        if (!mCurrentAllDay) {
            if (mCurrentStartTime == null) {
                Library.showError(mContext, R.string.text_select_hour_begin, Toast.LENGTH_SHORT);
                return false;
            }

            if (mCurrentEndTime == null) {
                Library.showError(mContext, R.string.text_select_hour_end);
                return false;
            }

            if (!mCurrentStartTime.isBefore(mCurrentEndTime)) {
                Library.showError(mContext, R.string.long_text_finish_hour_must_be_post_begin_hour);
                return false;
            }
        }

        // Aggiungere validazione EventAction
        EventAction eventAction = ToolbarActionBridge.mapToEventAction(mTemplate.getSourceAction());
        if (!EventActionManager.canPerformAction(eventAction, mDate, mUserId)) {
            Library.showError(mContext, R.string.long_text_action_not_permitted_on_this_date);
            return false;
        }

        return true;
    }

    /**
     * ✅ Create QuickEventRequest from UI input
     */
    private QuickEventRequest createRequestFromUI() {
        return QuickEventRequest.builder()
                .templateId(mTemplate.getTemplateId())
                .sourceAction(mTemplate.getSourceAction())
                .eventType(mTemplate.getEventType())
                .date(mDate)
                .userId(mUserId)
                .displayName(mCurrentTitle.isEmpty() ?
                        QuickEventLogicAdapter.getRecommendedTitle(mTemplate.getSourceAction(), mDate) :
                        mCurrentTitle)
                .description(mCurrentDescription.isEmpty() ? null : mCurrentDescription)
                .priority(mTemplate.getDefaultPriority())
                .allDay(mCurrentAllDay)
                .startTime(mCurrentAllDay ? null : mCurrentStartTime)
                .endTime(mCurrentAllDay ? null : mCurrentEndTime)
                .customProperty("ui_created", "true")
                .customProperty("dialog_version", QUICK_EVENT_CONFIRMATION_DIALOG_VERSION)
                .build();
    }

    /**
     * ✅ Handle service creation result
     */
    private void handleCreationResult(OperationResult<LocalEvent> result) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(() -> {
                setLoadingState(false);

                if (result.isSuccess()) {
                    LocalEvent createdEvent = result.getData();
                    Log.d(TAG, "Event created successfully: " + createdEvent.getId());

                    Library.showSuccess(mContext, "Evento creato con successo!");
                    mListener.onEventCreated(createdEvent, mTemplate.getSourceAction(), mDate);

                    // Auto-dismiss after short delay
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            this::dismiss, 1000);

                } else {
                    Log.w(TAG, "Event creation failed: " + result.getFormattedErrorMessage());
                    handleCreationError(result.getFormattedErrorMessage());
                }
            });
        }
    }

    /**
     * ✅ Handle service creation exception
     */
    private Void handleCreationException(Throwable throwable) {
        Log.e(TAG, "Quick event creation failed", throwable);

        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(() -> {
                setLoadingState(false);
                handleCreationError(throwable.getMessage());
            });
        }

        return null;
    }

    /**
     * Handle creation error
     */
    private void handleCreationError(String errorMessage) {
        Log.e(TAG, "Creation error: " + errorMessage);
        Library.showDialogError(mContext, mContext.getString(R.string.title_text_creation_error), errorMessage);
        mListener.onEventCreationFailed(mTemplate.getSourceAction(), mDate, errorMessage);
    }

    /**
     * Handle dialog cancellation
     */
    private void handleCancellation() {
        Log.d(TAG, "Dialog cancelled by user");
        mListener.onEventCreationCancelled(mTemplate.getSourceAction(), mDate);
        dismiss();
    }

    /**
     * Handle dialog system cancellation
     */
    private void handleDialogCancellation() {
        Log.d(TAG, "Dialog cancelled by system");
        mListener.onEventCreationCancelled(mTemplate.getSourceAction(), mDate);
    }

    // ==================== UI STATE MANAGEMENT ====================

    /**
     * Set loading state
     */
    private void setLoadingState(boolean loading) {
        if (mConfirmButton != null) {
            mConfirmButton.setEnabled(!loading);
            mConfirmButton.setText(loading ?
                    mContext.getString(R.string.text_creation_dots) :
                    mContext.getString(R.string.text_confirm));
        }

        if (mCancelButton != null) {
            mCancelButton.setEnabled(!loading);
        }

        // Disable other inputs during loading
        setInputsEnabled(!loading);
    }

    /**
     * Enable/disable input fields
     */
    private void setInputsEnabled(boolean enabled) {
        if (mTitleEdit != null) mTitleEdit.setEnabled(enabled);
        if (mDescriptionEdit != null) mDescriptionEdit.setEnabled(enabled);
        if (mAllDaySwitch != null) mAllDaySwitch.setEnabled(enabled);
        if (mStartTimeButton != null) mStartTimeButton.setEnabled(enabled);
        if (mEndTimeButton != null) mEndTimeButton.setEnabled(enabled);
    }

    // ==================== CLEANUP ====================

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        // Clear references
        mDialog = null;
        mDialogView = null;

        Log.d(TAG, "Dialog cleanup completed");
    }

    // ==================== TIME MANAGEMENT ====================

    /**
     * Update text on time buttons
     */
    private void updateTimeButtonsText() {
        if (mCurrentStartTime != null) {
            mStartTimeButton.setText(formatTimeForDisplay(mCurrentStartTime));
        }
        if (mCurrentEndTime != null) {
            mEndTimeButton.setText(formatTimeForDisplay(mCurrentEndTime));
        }
    }

    /**
     * Fallback simple time input dialog
     */
    private void showSimpleTimeInputDialog(boolean isStartTime) {
        // Simple fallback implementation using basic TimePickerDialog
        LocalTime currentTime = isStartTime ? mCurrentStartTime : mCurrentEndTime;
        String title = isStartTime ? mContext.getString(R.string.text_hour_begin) : mContext.getString(R.string.text_hour_end);

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                mContext,
                (view, hourOfDay, minute) -> {
                    LocalTime newTime = LocalTime.of(hourOfDay, minute);

                    if (isStartTime) {
                        mCurrentStartTime = newTime;
                        if (mCurrentEndTime.isBefore(mCurrentStartTime)) {
                            mCurrentEndTime = mCurrentStartTime.plusHours(8);
                        }
                    } else {
                        if (newTime.isAfter(mCurrentStartTime)) {
                            mCurrentEndTime = newTime;
                        } else {
                            Library.showWarning(mContext, R.string.long_text_finish_hour_must_be_post_begin_hour);
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
        return switch (eventType) {
            case VACATION -> R.drawable.ic_rounded_beach_access_24;
            case SICK_LEAVE -> R.drawable.ic_rounded_local_hospital_24;
            case OVERTIME -> R.drawable.ic_rounded_engineering_24;
            case PERSONAL_LEAVE -> R.drawable.ic_rounded_schedule_24;
            case SPECIAL_LEAVE -> R.drawable.ic_rounded_accessible_24;
            case SYNDICATE_LEAVE -> R.drawable.ic_rounded_clock_loader_40_24;
            default -> R.drawable.ic_rounded_event_24;
        };
    }
}