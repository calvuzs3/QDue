package net.calvuz.qdue.ui.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.ui.events.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.events.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsEventOperationsInterface;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * EventEditFragment - Edit existing events
 * <p>
 * Features:
 * - Form-based editing of all event fields
 * - Date/time pickers
 * - Validation
 * - Save to database
 * - Cancel with confirmation
 */
public class EventEditFragment extends Fragment implements
        BackPressHandler {

    private static final String TAG = "EventEditFrg";
    private static final String ARG_EVENT_ID = "eventId";

    // CORREZIONE 1: AGGIUNGERE INTERFACES
    private EventsDatabaseOperationsInterface mEventsDatabaseOperationsInterface;
    private EventsEventOperationsInterface mEventsEventOperationsInterface;

    // Date formatters
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Views
    private TextInputEditText mTitleEdit;
    private TextInputEditText mDescriptionEdit;
    private TextInputEditText mLocationEdit;
    private MaterialButton mStartDateButton;
    private MaterialButton mStartTimeButton;
    private MaterialButton mEndDateButton;
    private MaterialButton mEndTimeButton;
    private MaterialSwitch mAllDaySwitch;
    private MaterialAutoCompleteTextView mEventTypeSpinner;
    private MaterialAutoCompleteTextView mPrioritySpinner;

    // Data
    private LocalEvent mEvent;
    private LocalEvent mOriginalEvent; // NUOVO: per tracking changes
    private String mEventId;
    private LocalDateTime mStartDateTime;
    private LocalDateTime mEndDateTime;
    private boolean mHasUnsavedChanges = false; // NUOVO: per confirmation dialog

    // Event types and priorities
    private static final String[] EVENT_TYPES = {
            "GENERAL", "STOP_PLANNED", "STOP_EMERGENCY", "MAINTENANCE", "MEETING"
    };

    private static final String[] PRIORITIES = {
            "NORMAL", "HIGH", "LOW"
    };


    /**
     * Factory method
     */
    public static EventEditFragment newInstance(String eventId) {
        EventEditFragment fragment = new EventEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Init interfaces
        initializeInterfaces();

        if (getArguments() != null) {
            mEventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    /**
     * Initialize interfaces for communication with activity
     */
    private void initializeInterfaces() {
        final String mTAG = "initializeInterfaces: ";

        try {
            if (getActivity() instanceof EventsDatabaseOperationsInterface) {
                mEventsDatabaseOperationsInterface = (EventsDatabaseOperationsInterface) getActivity();
                Log.d(TAG, mTAG + "EventsDatabaseOperationsInterface initialized");
            } else {
                Log.w(TAG, mTAG + "Activity does not implement EventsDatabaseOperationsInterface");
            }

            if (getActivity() instanceof EventsEventOperationsInterface) {
                mEventsEventOperationsInterface = (EventsEventOperationsInterface) getActivity();
                Log.d(TAG, mTAG + "EventsEventOperationsInterface initialized");
            } else {
                Log.w(TAG, mTAG + "Activity does not implement EventsEventOperationsInterface");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error initializing interfaces: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupSpinners();
        setupDateTimePickers();
        loadEventData();
    }

    /**
     * Initialize view references
     */
    private void initializeViews(View view) {
        mTitleEdit = view.findViewById(R.id.edit_event_title);
        mDescriptionEdit = view.findViewById(R.id.edit_event_description);
        mLocationEdit = view.findViewById(R.id.edit_event_location);
        mStartDateButton = view.findViewById(R.id.btn_start_date);
        mStartTimeButton = view.findViewById(R.id.btn_start_time);
        mEndDateButton = view.findViewById(R.id.btn_end_date);
        mEndTimeButton = view.findViewById(R.id.btn_end_time);
        mAllDaySwitch = view.findViewById(R.id.switch_all_day);
        mEventTypeSpinner = view.findViewById(R.id.spinner_event_type);
        mPrioritySpinner = view.findViewById(R.id.spinner_priority);
    }

    /**
     * Setup dropdown spinners
     */
    private void setupSpinners() {
        // Event type spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, EVENT_TYPES);
        mEventTypeSpinner.setAdapter(typeAdapter);

        // Priority spinner
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, PRIORITIES);
        mPrioritySpinner.setAdapter(priorityAdapter);
    }

    /**
     * Setup date/time picker buttons
     */
    private void setupDateTimePickers() {
        mStartDateButton.setOnClickListener(v -> showDatePicker(true));
        mStartTimeButton.setOnClickListener(v -> showTimePicker(true));
        mEndDateButton.setOnClickListener(v -> showDatePicker(false));
        mEndTimeButton.setOnClickListener(v -> showTimePicker(false));

        // All day switch listener
        mAllDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mStartTimeButton.setEnabled(!isChecked);
            mEndTimeButton.setEnabled(!isChecked);

            if (isChecked) {
                // Set times to full day
                if (mStartDateTime != null) {
                    mStartDateTime = mStartDateTime.withHour(0).withMinute(0);
                }
                if (mEndDateTime != null) {
                    mEndDateTime = mEndDateTime.withHour(23).withMinute(59);
                }
                updateTimeButtons();
            }
        });
    }

    /**
     * Load event data from database
     * ENHANCED: Load event data with backup creation
     */
    private void loadEventData() {
        final String mTAG = "loadEventData: ";

        if (TextUtils.isEmpty(mEventId)) {
            Log.e(TAG, mTAG + "No event ID provided");
            showError("Errore: ID evento mancante");
            return;
        }

        Log.d(TAG, mTAG + "Loading event for editing: " + mEventId);

        new Thread(() -> {
            try {
                EventDao eventDao = QDueDatabase.getInstance(requireContext()).eventDao();
                LocalEvent event = eventDao.getEventById(mEventId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            if (event != null) {
                                mEvent = event;
                                // NUOVO: Create backup for change detection
                                mOriginalEvent = createEventCopy(event);
                                populateForm();
                                setupChangeTracking();
                                Log.d(TAG, mTAG + "✅ Event loaded for editing: " + event.getTitle());
                            } else {
                                showError("Evento non trovato nel database");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error in UI update: " + e.getMessage());
                            showError("Errore aggiornamento interfaccia");
                        }
                    });
                } else {
                    Log.w(TAG, mTAG + "Activity is null, cannot update UI");
                }

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error loading event: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            showError("Errore caricamento evento dal database"));
                }
            }
        }).start();
    }

    /**
     * NUOVO: Create a copy of event for change tracking
     */
    private LocalEvent createEventCopy(LocalEvent original) {
        try {
            // Create a deep copy of the event for comparison
            LocalEvent copy = new LocalEvent();
            copy.setId(original.getId());
            copy.setTitle(original.getTitle());
            copy.setDescription(original.getDescription());
            copy.setLocation(original.getLocation());
            copy.setStartTime(original.getStartTime());
            copy.setEndTime(original.getEndTime());
            copy.setAllDay(original.isAllDay());
            copy.setEventType(original.getEventType());
            copy.setPriority(original.getPriority());
            return copy;
        } catch (Exception e) {
            Log.e(TAG, "Error creating event copy: " + e.getMessage());
            return null;
        }
    }

    /**
     * NUOVO: Setup change tracking for unsaved changes detection
     */
    private void setupChangeTracking() {
        final String mTAG = "setupChangeTracking: ";

        try {
            // Add text watchers to detect changes
            android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mHasUnsavedChanges = true;
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            };

            if (mTitleEdit != null) mTitleEdit.addTextChangedListener(textWatcher);
            if (mDescriptionEdit != null) mDescriptionEdit.addTextChangedListener(textWatcher);
            if (mLocationEdit != null) mLocationEdit.addTextChangedListener(textWatcher);

            // Add listeners to other controls
            if (mAllDaySwitch != null) {
                mAllDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    mHasUnsavedChanges = true;
                    // Existing all day logic...
                    mStartTimeButton.setEnabled(!isChecked);
                    mEndTimeButton.setEnabled(!isChecked);

                    if (isChecked) {
                        if (mStartDateTime != null) {
                            mStartDateTime = mStartDateTime.withHour(0).withMinute(0);
                        }
                        if (mEndDateTime != null) {
                            mEndDateTime = mEndDateTime.withHour(23).withMinute(59);
                        }
                        updateTimeButtons();
                    }
                });
            }

            Log.d(TAG, mTAG + "Change tracking setup completed");

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error setting up change tracking: " + e.getMessage());
        }
    }

    /**
     * Populate form with event data
     */
    private void populateForm() {
        if (mEvent == null) return;

        // Basic fields
        mTitleEdit.setText(mEvent.getTitle());
        mDescriptionEdit.setText(mEvent.getDescription());
        mLocationEdit.setText(mEvent.getLocation());

        // Date/time
        mStartDateTime = mEvent.getStartTime();
        mEndDateTime = mEvent.getEndTime();
        updateDateTimeButtons();

        // All day
        mAllDaySwitch.setChecked(mEvent.isAllDay());

        // Type and priority
        if (mEvent.getEventType() != null) {
            mEventTypeSpinner.setText(mEvent.getEventType().toString(), false);
        }
        if (mEvent.getPriority() != null) {
            mPrioritySpinner.setText(mEvent.getPriority().toString(), false);
        }
    }

    /**
     * Update date/time button texts
     */
    private void updateDateTimeButtons() {
        if (mStartDateTime != null) {
            mStartDateButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (mEndDateTime != null) {
            mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    /**
     * Update only time buttons
     */
    private void updateTimeButtons() {
        if (mStartDateTime != null) {
            mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (mEndDateTime != null) {
            mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    /**
     * Show date picker with validation
     */
    private void showDatePicker(boolean isStart) {
        final String mTAG = "showDatePicker: ";

        try {
            LocalDateTime dateTime = isStart ? mStartDateTime : mEndDateTime;
            if (dateTime == null) {
                dateTime = LocalDateTime.now();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(dateTime.getYear(), dateTime.getMonthValue() - 1, dateTime.getDayOfMonth());

            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        try {
                            // Mark as changed
                            mHasUnsavedChanges = true;

                            if (isStart) {
                                if (mStartDateTime == null) {
                                    mStartDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, 9, 0);
                                } else {
                                    mStartDateTime = mStartDateTime.withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth);
                                }
                                mStartDateButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                                Log.d(TAG, mTAG + "Start date updated: " + mStartDateTime.toLocalDate());
                            } else {
                                if (mEndDateTime == null) {
                                    mEndDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, 17, 0);
                                } else {
                                    mEndDateTime = mEndDateTime.withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth);
                                }
                                mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                                Log.d(TAG, mTAG + "End date updated: " + mEndDateTime.toLocalDate());
                            }

                            // NUOVO: Auto-adjust end date if it becomes before start date
                            if (mStartDateTime != null && mEndDateTime != null && mEndDateTime.isBefore(mStartDateTime)) {
                                if (isStart) {
                                    // If start date was changed and now after end, adjust end date
                                    mEndDateTime = mStartDateTime.plusHours(1);
                                    mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                                    updateTimeButtons();
                                    Toast.makeText(getContext(), "Data fine automaticamente aggiustata", Toast.LENGTH_SHORT).show();
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error updating date: " + e.getMessage());
                            Toast.makeText(getContext(), "Errore aggiornamento data", Toast.LENGTH_SHORT).show();
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error showing date picker: " + e.getMessage());
            Toast.makeText(getContext(), "Errore apertura calendario", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show time picker with validation
     */
    private void showTimePicker(boolean isStart) {
        final String mTAG = "showTimePicker: ";

        try {
            LocalDateTime dateTime = isStart ? mStartDateTime : mEndDateTime;
            if (dateTime == null) {
                dateTime = LocalDateTime.now();
            }

            TimePickerDialog dialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        try {
                            // Mark as changed
                            mHasUnsavedChanges = true;

                            if (isStart) {
                                if (mStartDateTime == null) {
                                    mStartDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                                } else {
                                    mStartDateTime = mStartDateTime.withHour(hourOfDay).withMinute(minute);
                                }
                                mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                                Log.d(TAG, mTAG + "Start time updated: " + mStartDateTime.toLocalTime());
                            } else {
                                if (mEndDateTime == null) {
                                    mEndDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                                } else {
                                    mEndDateTime = mEndDateTime.withHour(hourOfDay).withMinute(minute);
                                }
                                mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                                Log.d(TAG, mTAG + "End time updated: " + mEndDateTime.toLocalTime());
                            }

                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error updating time: " + e.getMessage());
                            Toast.makeText(getContext(), "Errore aggiornamento orario", Toast.LENGTH_SHORT).show();
                        }
                    },
                    dateTime.getHour(),
                    dateTime.getMinute(),
                    true
            );

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error showing time picker: " + e.getMessage());
            Toast.makeText(getContext(), "Errore apertura selezione orario", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== MENU HANDLING ====================

    /**
     * Method called by EventsActivity
     */
    public void onHomePressed() {
        // Same logic as back pressed
        if (mHasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            // If no active changes - let it go
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            Log.d(TAG, "🏠 HOME BUTTON PRESSED in EventsActivity");
            // Intercepts toolbar's Home/Back button
            cancelEdit(); // modify check same logic
            return true;
        } else if (itemId == R.id.action_save_event) {
            saveEvent();
            return true;
        } else if (itemId == R.id.action_cancel_edit) {
            cancelEdit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Save event to database
     * ENHANCED: Save event with proper notification system
     */
    private void saveEvent() {
        final String mTAG = "saveEvent: ";

        if (!validateForm()) {
            return;
        }

        Log.d(TAG, mTAG + "Saving event: " + mEvent.getTitle());

        // Update event with form data
        updateEventFromForm();

        // Save to database with enhanced error handling
        new Thread(() -> {
            try {
                EventDao eventDao = QDueDatabase.getInstance(requireContext()).eventDao();
                int rowsAffected = eventDao.updateEvent(mEvent);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            if (rowsAffected > 0) {
                                Log.d(TAG, mTAG + "✅ Event updated successfully: " + mEvent.getTitle());

                                // CORREZIONE: Notify activity of the change
                                notifyActivityOfEventUpdate();

                                // Show success message
                                Toast.makeText(getContext(), "✅ Evento salvato", Toast.LENGTH_SHORT).show();

                                // Mark as saved
                                mHasUnsavedChanges = false;

                                // Navigate back
                                navigateBack();

                            } else {
                                Log.w(TAG, mTAG + "No rows affected during update");
                                Toast.makeText(getContext(), "⚠️ Nessuna modifica salvata", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error in UI update after save: " + e.getMessage());
                            Toast.makeText(getContext(), "Errore post-salvataggio", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.w(TAG, mTAG + "Activity is null, cannot update UI after save");
                }

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error updating event in database: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "❌ Errore salvataggio", Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    /**
     * NUOVO: Notify activity of event update for proper state management
     */
    private void notifyActivityOfEventUpdate() {
        final String mTAG = "notifyActivityOfEventUpdate: ";

        try {
            // Method 1: Update via EventsListFragment if activity has reference
            if (getActivity() instanceof EventsActivity) {
                EventsActivity eventsActivity = (EventsActivity) getActivity();
                eventsActivity.updateEventInList(mEvent);
                Log.d(TAG, mTAG + "✅ Notified EventsActivity via updateEventInList");
            }

            // Method 2: Notify MainActivity via activity result system
            // This ensures MainActivity's fragments get refreshed when we return
            if (getActivity() != null) {
                // Set activity result to indicate event was modified
                getActivity().setResult(android.app.Activity.RESULT_OK, createResultIntent());
                Log.d(TAG, mTAG + "✅ Set activity result for MainActivity notification");
            }

            // Method 3: Direct interface notification if available
            if (mEventsEventOperationsInterface != null) {
                // Could add a method like triggerEventUpdated(LocalEvent event) to the interface
                Log.d(TAG, mTAG + "Event operations interface available for future enhancements");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error notifying activity: " + e.getMessage());
        }
    }

    /**
     * NUOVO: Create result intent for MainActivity notification
     */
    private android.content.Intent createResultIntent() {
        android.content.Intent resultIntent = new android.content.Intent();
        resultIntent.putExtra(EventsActivity.EXTRA_EVENTS_CHANGED, true);
        resultIntent.putExtra(EventsActivity.EXTRA_EVENTS_COUNT, 1);
        resultIntent.putExtra(EventsActivity.EXTRA_CHANGE_TYPE, EventsActivity.CHANGE_TYPE_MODIFY);
        resultIntent.putExtra("modified_event_id", mEvent.getId());
        return resultIntent;
    }

    /**
     * Validate form fields
     * with better error messages
     */
    private boolean validateForm() {
        final String mTAG = "validateForm: ";
        boolean isValid = true;

        try {
            // Title required
            if (mTitleEdit != null && TextUtils.isEmpty(mTitleEdit.getText())) {
                mTitleEdit.setError("Titolo richiesto");
                mTitleEdit.requestFocus();
                isValid = false;
                Log.w(TAG, mTAG + "Validation failed: Title is empty");
            }

            // Start date required
            if (mStartDateTime == null) {
                Toast.makeText(getContext(), "Data inizio richiesta", Toast.LENGTH_SHORT).show();
                if (mStartDateButton != null) mStartDateButton.requestFocus();
                isValid = false;
                Log.w(TAG, mTAG + "Validation failed: Start date is null");
            }

            // End date required
            if (mEndDateTime == null) {
                Toast.makeText(getContext(), "Data fine richiesta", Toast.LENGTH_SHORT).show();
                if (mEndDateButton != null) mEndDateButton.requestFocus();
                isValid = false;
                Log.w(TAG, mTAG + "Validation failed: End date is null");
            }

            // End must be after start
            if (mStartDateTime != null && mEndDateTime != null && mEndDateTime.isBefore(mStartDateTime)) {
                Toast.makeText(getContext(), "La data fine deve essere dopo l'inizio", Toast.LENGTH_SHORT).show();
                if (mEndDateButton != null) mEndDateButton.requestFocus();
                isValid = false;
                Log.w(TAG, mTAG + "Validation failed: End date before start date");
            }

            // NUOVO: Additional validation for reasonable duration
            if (mStartDateTime != null && mEndDateTime != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(mStartDateTime, mEndDateTime);
                if (daysBetween > 365) {
                    Toast.makeText(getContext(), "⚠️ Durata evento superiore a 1 anno", Toast.LENGTH_LONG).show();
                    Log.w(TAG, mTAG + "Warning: Event duration > 1 year (" + daysBetween + " days)");
                }
            }

            Log.d(TAG, mTAG + "Validation result: " + (isValid ? "PASSED" : "FAILED"));
            return isValid;

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during validation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update event object with form data
     */
    private void updateEventFromForm() {
        if (mEvent == null) return;

        mEvent.setTitle(mTitleEdit.getText().toString().trim());
        mEvent.setDescription(mDescriptionEdit.getText().toString().trim());
        mEvent.setLocation(mLocationEdit.getText().toString().trim());
        mEvent.setStartTime(mStartDateTime);
        mEvent.setEndTime(mEndDateTime);
        mEvent.setAllDay(mAllDaySwitch.isChecked());

        // Event type
        String eventTypeStr = mEventTypeSpinner.getText().toString();
        if (!TextUtils.isEmpty(eventTypeStr)) {
            try {
                mEvent.setEventType(EventType.valueOf(eventTypeStr));
            } catch (IllegalArgumentException e) {
                mEvent.setEventType(EventType.GENERAL);
            }
        }

        // Priority
        String priorityStr = mPrioritySpinner.getText().toString();
        if (!TextUtils.isEmpty(priorityStr)) {
            try {
                mEvent.setPriority(EventPriority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                mEvent.setPriority(EventPriority.NORMAL);
            }
        }

        // Update last modified
        mEvent.setLastUpdated(LocalDateTime.now());
    }

    /**
     * Cancel editing with confirmation for unsaved changes
     */
    private void cancelEdit() {
        final String mTAG = "cancelEdit: ";

        if (mHasUnsavedChanges) {
            Log.d(TAG, mTAG + "Has unsaved changes, showing confirmation dialog");
            showUnsavedChangesDialog();
        } else {
            Log.d(TAG, mTAG + "No unsaved changes, navigating back directly");
            navigateBack();
        }
    }

    /**
     * NUOVO: Show confirmation dialog for unsaved changes
     */
    private void showUnsavedChangesDialog() {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Modifiche non salvate")
                    .setMessage("Hai modifiche non salvate. Vuoi uscire senza salvare?")
                    .setPositiveButton("Esci senza salvare", (dialog, which) -> {
                        Log.d(TAG, "User chose to exit without saving");
                        mHasUnsavedChanges = false; // Reset flag
                        navigateBack();
                    })
                    .setNegativeButton("Continua modifica", (dialog, which) -> {
                        Log.d(TAG, "User chose to continue editing");
                        dialog.dismiss();
                    })
                    .setNeutralButton("Salva e esci", (dialog, which) -> {
                        Log.d(TAG, "User chose to save and exit");
                        saveEvent(); // This will navigate back on success
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing unsaved changes dialog: " + e.getMessage());
            // Fallback to direct navigation
            navigateBack();
        }
    }

    /**
     * ENHANCED: Navigate back with proper error handling
     */
    private void navigateBack() {
        final String mTAG = "navigateBack: ";

        try {
            if (getView() != null) {
                Navigation.findNavController(requireView()).popBackStack();
                Log.d(TAG, mTAG + "✅ Successfully navigated back via Navigation Component");
            } else {
                throw new IllegalStateException("View is null");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error with Navigation Component: " + e.getMessage());

            // Fallback to activity back press
            if (getActivity() != null) {
                getActivity().onBackPressed();
                Log.d(TAG, mTAG + "Fallback: used activity back press");
            } else {
                Log.e(TAG, mTAG + "Cannot navigate back - both Navigation and Activity are unavailable");
            }
        }
    }

    /**
     * Show error with better UX
     */
    private void showError(String message) {
        final String mTAG = "showError: ";

        try {
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                Log.e(TAG, mTAG + message);

                // Navigate back after showing error
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    navigateBack();
                }, 2000); // 2 second delay to let user read the error

            } else {
                Log.e(TAG, mTAG + "Context is null, cannot show error: " + message);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error showing error message: " + e.getMessage());
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method to check fragment state
     */
    public void debugFragmentState() {
        Log.d(TAG, "=== EVENT EDIT FRAGMENT DEBUG ===");
        Log.d(TAG, "Event ID: " + mEventId);
        Log.d(TAG, "Event Loaded: " + (mEvent != null ? mEvent.getTitle() : "null"));
        Log.d(TAG, "Has Unsaved Changes: " + mHasUnsavedChanges);
        Log.d(TAG, "Start DateTime: " + mStartDateTime);
        Log.d(TAG, "End DateTime: " + mEndDateTime);
        Log.d(TAG, "Database Operations Interface: " + (mEventsDatabaseOperationsInterface != null ? "available" : "null"));
        Log.d(TAG, "Event Operations Interface: " + (mEventsEventOperationsInterface != null ? "available" : "null"));
        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * @return true if there are unsaved changes, false otherwise
     */
    @Override
    public boolean onBackPressed() {
        if (mHasUnsavedChanges) {
            showUnsavedChangesDialog();
            return true; // Handled - non uscire
        }
        return false; // Non handled - può uscire
    }
}