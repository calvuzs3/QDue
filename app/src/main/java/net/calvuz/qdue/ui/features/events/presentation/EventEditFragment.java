package net.calvuz.qdue.ui.features.events.presentation;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.core.common.interfaces.EventsOperationsInterface;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.actions.EventAction;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.metadata.EventEditMetadataManager;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.quickevents.QuickEventLogicAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UPDATED: Fragment for editing existing events with complete EventAction integration.
 *
 * <p>Enhanced Features with EventAction Framework:
 * <ul>
 *   <li>Complete EventAction integration for business logic validation</li>
 *   <li>Advanced metadata tracking with turn impact analysis</li>
 *   <li>EventAction-based constraint checking and validation</li>
 *   <li>Enhanced change detection with business rule compliance</li>
 *   <li>Dynamic EventType and EventPriority loading from enums</li>
 *   <li>User-friendly display names with intelligent fallback</li>
 *   <li>Comprehensive turn impact warnings and guidance</li>
 *   <li>Advanced business impact assessment and analytics</li>
 *   <li>Seamless backward compatibility with existing systems</li>
 * </ul>
 *
 * <p>Migration Benefits:
 * - EventAction business logic integration for accurate validation
 * - Enhanced metadata with turn exception tracking
 * - Improved user experience with intelligent warnings
 * - Better analytics and reporting capabilities
 * - Future-proof architecture with clean separation of concerns
 */
public class EventEditFragment extends Fragment implements UnsavedChangesHandler {

    private static final String TAG = "EventEdit";
    private static final String ARG_EVENT_ID = "eventId";

    // ==================== INTERFACES ====================

    private EventsDatabaseOperationsInterface mEventsDatabaseOperationsInterface;
    private EventsOperationsInterface mEventsOperationsInterface;

    // ==================== UI COMPONENTS ====================

    private TextInputEditText mEditTitle;
    private TextInputEditText mEditDescription;
    private TextInputEditText mEditLocation;
    private MaterialButton mStartDateButton;
    private MaterialButton mStartTimeButton;
    private MaterialButton mEndDateButton;
    private MaterialButton mEndTimeButton;
    private MaterialSwitch mAllDaySwitch;
    private MaterialAutoCompleteTextView mEventTypeSpinner;
    private MaterialAutoCompleteTextView mPrioritySpinner;

    // ==================== DATA ====================

    private LocalEvent mEvent;
    private LocalEvent mOriginalEvent;
    private String mEventId;
    private LocalDateTime mStartDateTime;
    private LocalDateTime mEndDateTime;

    // Backup times for all-day toggle
    private LocalDateTime mBackupStartDateTime;
    private LocalDateTime mBackupEndDateTime;

    // Enhanced enum management
    private List<EventType> mAvailableEventTypes;
    private List<EventPriority> mAvailablePriorities;
    private ArrayAdapter<String> mEventTypeAdapter;
    private ArrayAdapter<String> mPriorityAdapter;

    // ==================== STATE ====================

    private boolean mHasUnsavedChanges = false;
    private boolean mIsSaving = false;

    // ==================== ENHANCED METADATA WITH EVENTACTION ====================

    private String mEditSessionId;
    private long mEditSessionStartTime;
    private int mSaveAttempts = 0;
    private List<String> mChangedFields;
    private boolean mMetadataInitialized = false;

    // EventAction integration
    private EventAction mOriginalEventAction;
    private EventAction mCurrentEventAction;
    private boolean mEventActionChanged = false;

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Factory method for creating fragment with event ID.
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

        // Initialize enhanced tracking
        mChangedFields = new ArrayList<>();
        mEditSessionStartTime = System.currentTimeMillis();

        initializeInterfaces();
        initializeEnumData();

        if (getArguments() != null) {
            mEventId = getArguments().getString(ARG_EVENT_ID);
        }

        Log.d(TAG, "Enhanced EventEditFragment initialized with EventAction integration");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupBackHandling();
        initializeViews(view);
        setupEnhancedSpinners();
        setupDateTimePickers();
        loadEventData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Enhanced cleanup with EventAction logging
        if (mMetadataInitialized && mEvent != null) {
            try {
                Log.d(TAG, "Final enhanced metadata summary before cleanup:");
                logEnhancedMetadataSummary();
            } catch (Exception e) {
                Log.e(TAG, "Error in final enhanced metadata logging: " + e.getMessage());
            }
        }

        // Cleanup resources
        if (mChangedFields != null) {
            mChangedFields.clear();
        }

        mEditSessionId = null;
        mMetadataInitialized = false;
        mOriginalEventAction = null;
        mCurrentEventAction = null;

        if (getActivity() instanceof EventsActivity) {
            ((EventsActivity) getActivity()).unregisterBackHandler(this);
        }

        Log.d(TAG, "Enhanced EventEditFragment cleanup completed");
    }

    // ==================== ENHANCED INITIALIZATION ====================

    /**
     * Enhanced metadata tracking initialization with EventAction detection.
     */
    private void initializeEnhancedMetadataTracking() {
        try {
            Long currentUserId = getCurrentUserId();

            // Initialize enhanced edit session metadata
            mEvent = EventEditMetadataManager.initializeEditSessionMetadata(mEvent, currentUserId);
            mMetadataInitialized = true;

            // Extract session ID for tracking
            if (mEvent.getCustomProperties() != null) {
                mEditSessionId = mEvent.getCustomProperties().get("edit_session_id");
            }

            // Enhanced: Detect original EventAction
            mOriginalEventAction = detectOriginalEventAction();
            mCurrentEventAction = mOriginalEventAction;

            Log.d(TAG, "Enhanced metadata tracking initialized - Session: " + mEditSessionId +
                    ", Original EventAction: " + mOriginalEventAction);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing enhanced metadata tracking: " + e.getMessage());
            mMetadataInitialized = false;
        }
    }

    /**
     * Detect original EventAction from event metadata and properties.
     */
    private EventAction detectOriginalEventAction() {
        if (mEvent == null) return null;

        try {
            // Strategy 1: Check for EventAction in metadata
            EventAction fromMetadata = EventEditMetadataManager.getOriginalEventAction(mEvent);
            if (fromMetadata != null) {
                Log.d(TAG, "Original EventAction detected from metadata: " + fromMetadata);
                return fromMetadata;
            }

            // Strategy 2: Use QuickEventLogicAdapter detection
            EventAction fromAdapter = QuickEventLogicAdapter.getCreatorEventAction(mEvent);
            if (fromAdapter != null) {
                Log.d(TAG, "Original EventAction detected from adapter: " + fromAdapter);
                return fromAdapter;
            }

            // Strategy 3: Try ToolbarAction bridge conversion
            ToolbarAction toolbarAction = QuickEventLogicAdapter.getCreatorToolbarAction(mEvent);
            if (toolbarAction != null) {
                EventAction fromBridge = ToolbarActionBridge.mapToEventAction(toolbarAction);
                Log.d(TAG, "Original EventAction derived from ToolbarAction: " + toolbarAction + " -> " + fromBridge);
                return fromBridge;
            }

            // Strategy 4: Derive from EventType
            if (mEvent.getEventType() != null) {
                EventAction derived = deriveEventActionFromEventType(mEvent.getEventType());
                Log.d(TAG, "Original EventAction derived from EventType: " + mEvent.getEventType() + " -> " + derived);
                return derived;
            }

            Log.d(TAG, "No original EventAction could be detected");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error detecting original EventAction: " + e.getMessage());
            return null;
        }
    }

    /**
     * Derive EventAction from EventType for backward compatibility.
     */
    private EventAction deriveEventActionFromEventType(EventType eventType) {
        if (eventType == null) return null;

        switch (eventType) {
            case VACATION:
                return EventAction.VACATION;
            case SICK_LEAVE:
                return EventAction.SICK_LEAVE;
            case PERSONAL_LEAVE:
                return EventAction.PERSONAL_LEAVE;
            case SPECIAL_LEAVE:
                return EventAction.SPECIAL_LEAVE;
            case SYNDICATE_LEAVE:
                return EventAction.SYNDICATE_LEAVE;
            case OVERTIME:
                return EventAction.OVERTIME;
            case SHIFT_SWAP:
                return EventAction.SHIFT_SWAP;
            case COMPENSATION:
                return EventAction.COMPENSATION;
            case STOP_PLANNED:
                return EventAction.PLANNED_STOP;
            case STOP_UNPLANNED:
                return EventAction.UNPLANNED_STOP;
            case MAINTENANCE:
                return EventAction.MAINTENANCE;
            case EMERGENCY:
                return EventAction.EMERGENCY;
            case TRAINING:
                return EventAction.TRAINING;
            case MEETING:
                return EventAction.MEETING;
            default:
                return EventAction.GENERAL;
        }
    }

    private void initializeInterfaces() {
        try {
            if (getActivity() instanceof EventsDatabaseOperationsInterface) {
                mEventsDatabaseOperationsInterface = (EventsDatabaseOperationsInterface) getActivity();
            }

            if (getActivity() instanceof EventsOperationsInterface) {
                mEventsOperationsInterface = (EventsOperationsInterface) getActivity();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing interfaces: " + e.getMessage());
        }
    }

    /**
     * Initialize enum data by loading all available EventType and EventPriority values.
     */
    private void initializeEnumData() {
        try {
            mAvailableEventTypes = Arrays.asList(EventType.values());
            Log.d(TAG, "Loaded " + mAvailableEventTypes.size() + " EventType values");

            mAvailablePriorities = Arrays.asList(EventPriority.values());
            Log.d(TAG, "Loaded " + mAvailablePriorities.size() + " EventPriority values");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing enum data: " + e.getMessage());
            mAvailableEventTypes = new ArrayList<>();
            mAvailablePriorities = new ArrayList<>();
        }
    }

    /**
     * Sets up modern back handling with OnBackPressedDispatcher.
     */
    private void setupBackHandling() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (hasUnsavedChanges()) {
                            handleUnsavedChanges(
                                    () -> {
                                        mHasUnsavedChanges = false;
                                        navigateBack();
                                    },
                                    () -> {
                                        // User cancelled - stay in fragment
                                    }
                            );
                        } else {
                            navigateBack();
                        }
                    }
                }
        );
    }

    private void initializeViews(View view) {
        mEditTitle = view.findViewById(R.id.edit_event_title);
        mEditDescription = view.findViewById(R.id.edit_event_description);
        mEditLocation = view.findViewById(R.id.edit_event_location);
        mStartDateButton = view.findViewById(R.id.btn_start_date);
        mStartTimeButton = view.findViewById(R.id.btn_start_time);
        mEndDateButton = view.findViewById(R.id.btn_end_date);
        mEndTimeButton = view.findViewById(R.id.btn_end_time);
        mAllDaySwitch = view.findViewById(R.id.switch_all_day);
        mEventTypeSpinner = view.findViewById(R.id.spinner_event_type);
        mPrioritySpinner = view.findViewById(R.id.spinner_priority);
    }

    // ==================== ENHANCED SPINNER SETUP ====================

    /**
     * Enhanced spinner setup using actual enum values with display names.
     */
    private void setupEnhancedSpinners() {
        try {
            setupEventTypeSpinner();
            setupEventPrioritySpinner();
            Log.d(TAG, "Enhanced spinners setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up enhanced spinners: " + e.getMessage());
            setupFallbackSpinners();
        }
    }

    /**
     * Setup EventType spinner with dynamic enum loading and display names.
     */
    private void setupEventTypeSpinner() {
        List<String> eventTypeDisplayNames = new ArrayList<>();

        for (EventType eventType : mAvailableEventTypes) {
            String displayName = eventType.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                eventTypeDisplayNames.add(displayName);
            } else {
                eventTypeDisplayNames.add(eventType.name());
            }
        }

        mEventTypeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                eventTypeDisplayNames
        );
        mEventTypeSpinner.setAdapter(mEventTypeAdapter);

        // Enhanced selection listener with EventAction tracking
        mEventTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            mHasUnsavedChanges = true;
            trackFieldChange("eventType");
            handleEnhancedEventTypeChange();
        });

        Log.d(TAG, "EventType spinner setup with " + eventTypeDisplayNames.size() + " options");
    }

    /**
     * Setup EventPriority spinner with dynamic enum loading and display names.
     */
    private void setupEventPrioritySpinner() {
        List<String> priorityDisplayNames = new ArrayList<>();

        for (EventPriority priority : mAvailablePriorities) {
            String displayName = priority.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                priorityDisplayNames.add(displayName);
            } else {
                priorityDisplayNames.add(priority.name());
            }
        }

        mPriorityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                priorityDisplayNames
        );
        mPrioritySpinner.setAdapter(mPriorityAdapter);

        mPrioritySpinner.setOnItemClickListener((parent, view, position, id) -> {
            mHasUnsavedChanges = true;
            trackFieldChange("priority");
        });

        Log.d(TAG, "EventPriority spinner setup with " + priorityDisplayNames.size() + " options");
    }

    /**
     * Fallback spinner setup in case of enum loading errors.
     */
    private void setupFallbackSpinners() {
        Log.w(TAG, "Using fallback spinner setup");

        String[] fallbackEventTypes = {"Generale", "Fermata Pianificata", "Emergenza", "Manutenzione", "Riunione"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, fallbackEventTypes);
        mEventTypeSpinner.setAdapter(typeAdapter);

        String[] fallbackPriorities = {"Bassa", "Normale", "Alta", "Urgente"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, fallbackPriorities);
        mPrioritySpinner.setAdapter(priorityAdapter);
    }

    // ==================== ENHANCED DATA LOADING ====================

    /**
     * Enhanced event data loading with metadata and EventAction integration.
     */
    private void loadEventData() {
        if (TextUtils.isEmpty(mEventId)) {
            Library.showError(getContext(), "Errore: ID evento mancante");
            return;
        }

        new Thread(() -> {
            try {
                EventDao eventDao = QDueDatabase.getInstance(requireContext()).eventDao();
                LocalEvent event = eventDao.getEventById(mEventId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            if (event != null) {
                                mEvent = event;
                                mOriginalEvent = createEventCopy(event);

                                // Enhanced: Initialize metadata tracking with EventAction
                                initializeEnhancedMetadataTracking();

                                populateFormWithEnhancedEnums();
                                setupEnhancedChangeTracking();
                            } else {
                                Library.showError(getContext(), "Evento non trovato nel database");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in UI update: " + e.getMessage());
                            Library.showError(getContext(), "Errore aggiornamento interfaccia");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading event: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Library.showError(getContext(), "Errore caricamento evento dal database"));
                }
            }
        }).start();
    }

    private LocalEvent createEventCopy(LocalEvent original) {
        try {
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

            // Enhanced: Copy custom properties for metadata preservation
            if (original.getCustomProperties() != null) {
                copy.setCustomProperties(new java.util.HashMap<>(original.getCustomProperties()));
            }

            return copy;
        } catch (Exception e) {
            Log.e(TAG, "Error creating event copy: " + e.getMessage());
            return null;
        }
    }

    // ==================== ENHANCED CHANGE TRACKING ====================

    /**
     * Enhanced change tracking setup with EventAction integration.
     */
    private void setupEnhancedChangeTracking() {
        // Setup field-specific text watchers
        if (mEditTitle != null) {
            mEditTitle.addTextChangedListener(createFieldSpecificTextWatcher("title"));
        }
        if (mEditDescription != null) {
            mEditDescription.addTextChangedListener(createFieldSpecificTextWatcher("description"));
        }
        if (mEditLocation != null) {
            mEditLocation.addTextChangedListener(createFieldSpecificTextWatcher("location"));
        }

        // Enhanced spinner tracking already setup in setupEnhancedSpinners()

        // Register legacy back handling for compatibility
        if (getActivity() instanceof EventsActivity) {
            ((EventsActivity) getActivity()).registerUnsavedChangesHandler(this, this);
        }

        Log.d(TAG, "Enhanced change tracking setup completed");
    }

    /**
     * Create field-specific text watcher for granular tracking.
     */
    private TextWatcher createFieldSpecificTextWatcher(String fieldName) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mHasUnsavedChanges = true;
                trackFieldChange(fieldName);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        };
    }

    /**
     * Enhanced EventType change handling with EventAction analysis.
     */
    private void handleEnhancedEventTypeChange() {
        try {
            EventType newEventType = convertDisplayNameToEventType(mEventTypeSpinner.getText().toString());
            EventType originalEventType = mOriginalEvent != null ? mOriginalEvent.getEventType() : null;

            if (newEventType != originalEventType) {
                Log.d(TAG, "EventType changed from " + originalEventType + " to " + newEventType);

                // Enhanced: Update current EventAction
                EventAction newEventAction = deriveEventActionFromEventType(newEventType);
                if (newEventAction != mCurrentEventAction) {
                    mCurrentEventAction = newEventAction;
                    mEventActionChanged = true;
                    Log.d(TAG, "EventAction changed to: " + newEventAction);
                }

                // Enhanced: Check EventAction business rule impacts
                if (hasEventActionBusinessImpact(newEventAction)) {
                    showEnhancedTurnImpactWarning(originalEventType, newEventType, mOriginalEventAction, newEventAction);
                }

                // Update metadata immediately for real-time analysis
                updateMetadataForCurrentChanges();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling enhanced EventType change: " + e.getMessage());
        }
    }

    /**
     * Check if EventAction change has business impact.
     */
    private boolean hasEventActionBusinessImpact(EventAction newAction) {
        if (mOriginalEventAction == null || newAction == null) return false;

        // Check various business impact factors
        if (mOriginalEventAction.getCategory() != newAction.getCategory()) return true;
        if (mOriginalEventAction.affectsWorkSchedule() != newAction.affectsWorkSchedule())
            return true;
        if (mOriginalEventAction.requiresApproval() != newAction.requiresApproval()) return true;
        if (mOriginalEventAction.requiresShiftCoverage() != newAction.requiresShiftCoverage())
            return true;

        return false;
    }

    /**
     * Track individual field changes for granular analytics.
     */
    private void trackFieldChange(String fieldName) {
        if (!mChangedFields.contains(fieldName)) {
            mChangedFields.add(fieldName);
            Log.v(TAG, "Field changed: " + fieldName + " (total changes: " + mChangedFields.size() + ")");

            if (mMetadataInitialized) {
                updateMetadataForCurrentChanges();
            }
        }
    }

    /**
     * Update metadata for current changes in real-time.
     */
    private void updateMetadataForCurrentChanges() {
        if (!mMetadataInitialized || mEvent == null || mOriginalEvent == null) {
            return;
        }

        try {
            long editDurationSeconds = (System.currentTimeMillis() - mEditSessionStartTime) / 1000;

            // Enhanced: Update change tracking metadata with EventAction context
            mEvent = EventEditMetadataManager.updateChangeTrackingMetadata(
                    mEvent, mOriginalEvent, mChangedFields, editDurationSeconds);

            Log.v(TAG, "Enhanced metadata updated for current changes");

        } catch (Exception e) {
            Log.e(TAG, "Error updating enhanced metadata for current changes: " + e.getMessage());
        }
    }

    // ==================== ENHANCED DATE/TIME PICKERS ====================

    /**
     * Enhanced date/time picker setup with change tracking and EventAction validation.
     */
    private void setupDateTimePickers() {
        mStartDateButton.setOnClickListener(v -> {
            trackFieldChange("startDate");
            showEnhancedDatePicker(true);
        });

        mStartTimeButton.setOnClickListener(v -> {
            trackFieldChange("startTime");
            showTimePicker(true);
        });

        mEndDateButton.setOnClickListener(v -> {
            trackFieldChange("endDate");
            showEnhancedDatePicker(false);
        });

        mEndTimeButton.setOnClickListener(v -> {
            trackFieldChange("endTime");
            showTimePicker(false);
        });

        mAllDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mHasUnsavedChanges = true;
            trackFieldChange("allDay");

            mStartTimeButton.setEnabled(!isChecked);
            mEndTimeButton.setEnabled(!isChecked);

            if (isChecked) {
                if (mStartDateTime != null) {
                    mBackupStartDateTime = mStartDateTime;
                    mStartDateTime = mStartDateTime.withHour(0).withMinute(0).withSecond(0);
                }
                if (mEndDateTime != null) {
                    mBackupEndDateTime = mEndDateTime;
                    mEndDateTime = mEndDateTime.withHour(23).withMinute(59).withSecond(59);
                }
                updateTimeButtons();
            } else {
                if (mBackupStartDateTime != null) {
                    mStartDateTime = mBackupStartDateTime;
                }
                if (mBackupEndDateTime != null) {
                    mEndDateTime = mBackupEndDateTime;
                }
                updateTimeButtons();
            }

            updateMetadataForCurrentChanges();
        });
    }

    /**
     * Enhanced date picker with EventAction constraint validation.
     */
    private void showEnhancedDatePicker(boolean isStart) {
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
                            mHasUnsavedChanges = true;
                            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);

                            // Enhanced: Validate against EventAction constraints
                            if (mCurrentEventAction != null && !validateDateAgainstEventAction(selectedDate)) {
                                return; // Validation failed, don't update
                            }

                            // Update the datetime
                            if (isStart) {
                                if (mStartDateTime == null) {
                                    mStartDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, 9, 0);
                                } else {
                                    mStartDateTime = mStartDateTime.withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth);
                                }
                                mStartDateButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                            } else {
                                if (mEndDateTime == null) {
                                    mEndDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, 17, 0);
                                } else {
                                    mEndDateTime = mEndDateTime.withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth);
                                }
                                mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                            }

                            // Auto-adjust end date if needed
                            autoAdjustEndDate(isStart);

                        } catch (Exception e) {
                            Log.e(TAG, "Error updating date: " + e.getMessage());
                            Library.showError(getContext(), "Errore aggiornamento data");
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing enhanced date picker: " + e.getMessage());
            Library.showError(getContext(), "Errore apertura calendario");
        }
    }

    /**
     * Validate selected date against EventAction business rules.
     */
    private boolean validateDateAgainstEventAction(LocalDate selectedDate) {
        if (mCurrentEventAction == null) return true;

        try {
            // Check if EventAction can be performed on this date
            if (!mCurrentEventAction.canPerformOnDate(selectedDate)) {
                String message = "L'azione '" + mCurrentEventAction.getDisplayName() +
                        "' non può essere eseguita in data " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                // Add specific reason based on EventAction constraints
                int minAdvanceNotice = mCurrentEventAction.getMinimumAdvanceNoticeDays();
                if (minAdvanceNotice > 0) {
                    long daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), selectedDate);
                    if (daysFromNow < minAdvanceNotice) {
                        message += "\nRichiede almeno " + minAdvanceNotice + " giorni di preavviso.";
                    }
                }

                Library.showError(getContext(), message);
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating date against EventAction: " + e.getMessage());
            return true; // Allow if validation fails
        }
    }

    /**
     * Auto-adjust end date logic.
     */
    private void autoAdjustEndDate(boolean isStart) {
        if (mStartDateTime != null && mEndDateTime != null) {
            if (mAllDaySwitch.isChecked()) {
                if (mEndDateTime.toLocalDate().isBefore(mStartDateTime.toLocalDate())) {
                    if (isStart) {
                        mEndDateTime = mStartDateTime.withHour(23).withMinute(59).withSecond(59);
                        mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        Library.showSuccess(getContext(), "Data fine automaticamente aggiustata");
                    }
                }
            } else {
                if (mEndDateTime.isBefore(mStartDateTime)) {
                    if (isStart) {
                        mEndDateTime = mStartDateTime.plusHours(1);
                        mEndDateButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        updateTimeButtons();
                        Library.showSuccess(getContext(), "Data fine automaticamente aggiustata");
                    }
                }
            }
        }
    }

    /**
     * Shows time picker dialog with validation.
     */
    private void showTimePicker(boolean isStart) {
        try {
            LocalDateTime dateTime = isStart ? mStartDateTime : mEndDateTime;
            if (dateTime == null) {
                dateTime = LocalDateTime.now();
            }

            TimePickerDialog dialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        try {
                            mHasUnsavedChanges = true;

                            if (isStart) {
                                if (mStartDateTime == null) {
                                    mStartDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                                } else {
                                    mStartDateTime = mStartDateTime.withHour(hourOfDay).withMinute(minute);
                                }
                                mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));

                                if (!mAllDaySwitch.isChecked()) {
                                    mBackupStartDateTime = mStartDateTime;
                                }
                            } else {
                                if (mEndDateTime == null) {
                                    mEndDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                                } else {
                                    mEndDateTime = mEndDateTime.withHour(hourOfDay).withMinute(minute);
                                }
                                mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));

                                if (!mAllDaySwitch.isChecked()) {
                                    mBackupEndDateTime = mEndDateTime;
                                }
                            }

                            // Validate time consistency for non-all-day events
                            if (!mAllDaySwitch.isChecked() && mStartDateTime != null && mEndDateTime != null) {
                                if (mEndDateTime.isBefore(mStartDateTime) || mEndDateTime.equals(mStartDateTime)) {
                                    if (isStart) {
                                        mEndDateTime = mStartDateTime.plusHours(1);
                                        mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                                        Library.showSuccess(getContext(), "Orario fine automaticamente aggiustato");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating time: " + e.getMessage());
                            Library.showError(getContext(), "Errore aggiornamento orario");
                        }
                    },
                    dateTime.getHour(),
                    dateTime.getMinute(),
                    true
            );

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker: " + e.getMessage());
            Library.showError(getContext(), "Errore apertura selezione orario");
        }
    }

    // ==================== ENHANCED FORM POPULATION ====================

    /**
     * Enhanced form population with proper enum handling and display names.
     */
    private void populateFormWithEnhancedEnums() {
        if (mEvent == null) return;

        // Populate basic text fields
        mEditTitle.setText(mEvent.getTitle());
        mEditDescription.setText(mEvent.getDescription());
        mEditLocation.setText(mEvent.getLocation());

        // Populate date/time fields
        mStartDateTime = mEvent.getStartTime();
        mEndDateTime = mEvent.getEndTime();

        mBackupStartDateTime = mStartDateTime;
        mBackupEndDateTime = mEndDateTime;

        updateDateTimeButtons();

        // Set all-day switch
        mAllDaySwitch.setChecked(mEvent.isAllDay());

        // Enhanced EventType selection with display name matching
        setEventTypeSelection(mEvent.getEventType());

        // Enhanced EventPriority selection with display name matching
        setEventPrioritySelection(mEvent.getPriority());

        mHasUnsavedChanges = false;
        Log.d(TAG, "Form populated successfully with enhanced enum handling");
    }

    /**
     * Set EventType selection using display name matching for better UX.
     */
    private void setEventTypeSelection(EventType eventType) {
        if (eventType == null || mEventTypeSpinner == null) {
            return;
        }

        try {
            String displayName = eventType.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                mEventTypeSpinner.setText(displayName, false);
                Log.d(TAG, "Set EventType selection to: " + displayName);
            } else {
                mEventTypeSpinner.setText(eventType.name(), false);
                Log.d(TAG, "Set EventType selection to enum name: " + eventType.name());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting EventType selection: " + e.getMessage());
            if (mEventTypeAdapter != null && mEventTypeAdapter.getCount() > 0) {
                mEventTypeSpinner.setText(mEventTypeAdapter.getItem(0), false);
            }
        }
    }

    /**
     * Set EventPriority selection using display name matching for better UX.
     */
    private void setEventPrioritySelection(EventPriority priority) {
        if (priority == null || mPrioritySpinner == null) {
            return;
        }

        try {
            String displayName = priority.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                mPrioritySpinner.setText(displayName, false);
                Log.d(TAG, "Set EventPriority selection to: " + displayName);
            } else {
                mPrioritySpinner.setText(priority.name(), false);
                Log.d(TAG, "Set EventPriority selection to enum name: " + priority.name());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting EventPriority selection: " + e.getMessage());
            if (mPriorityAdapter != null && mPriorityAdapter.getCount() > 0) {
                mPrioritySpinner.setText(mPriorityAdapter.getItem(0), false);
            }
        }
    }

    // ==================== UI UPDATES ====================

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

    private void updateTimeButtons() {
        if (mStartDateTime != null) {
            mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (mEndDateTime != null) {
            mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    // ==================== MENU HANDLING ====================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            cancelEdit();
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

    // ==================== ENHANCED SAVE/CANCEL OPERATIONS ====================

    /**
     * Enhanced save operation with EventAction validation and metadata finalization.
     */
    private void saveEvent() {
        if (!validateEnhancedForm()) {
            mSaveAttempts++;
            return;
        }

        updateEventFromFormWithEnhancedEnums();
        mSaveAttempts++;
        mIsSaving = true;

        new Thread(() -> {
            try {
                // Enhanced: Finalize metadata with EventAction context
                if (mMetadataInitialized) {
                    long totalEditDuration = (System.currentTimeMillis() - mEditSessionStartTime) / 1000;
                    mEvent = EventEditMetadataManager.finalizeEditSessionMetadata(
                            mEvent, mSaveAttempts, totalEditDuration);
                }

                EventDao eventDao = QDueDatabase.getInstance(requireContext()).eventDao();
                int rowsAffected = eventDao.updateEvent(mEvent);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            if (rowsAffected > 0) {
                                // Enhanced: Log metadata summary for analytics
                                logEnhancedMetadataSummary();

                                notifyActivityOfEventUpdate();
                                Library.showSuccess(getContext(), "Evento salvato con successo");
                                mHasUnsavedChanges = false;
                                navigateBack();
                            } else {
                                Library.showError(getContext(), "Nessuna modifica salvata");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in UI update after save: " + e.getMessage());
                            Library.showError(getContext(), "Errore post-salvataggio");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating event in database: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Library.showError(getContext(), "Errore durante il salvataggio"));
                }
            }
        }).start();
    }

    private void notifyActivityOfEventUpdate() {
        try {
            if (getActivity() instanceof EventsActivity) {
                EventsActivity eventsActivity = (EventsActivity) getActivity();
                eventsActivity.updateEventInList(mEvent);
            }

            if (getActivity() != null) {
                getActivity().setResult(android.app.Activity.RESULT_OK, createResultIntent());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error notifying activity: " + e.getMessage());
        }
    }

    private android.content.Intent createResultIntent() {
        android.content.Intent resultIntent = new android.content.Intent();
        resultIntent.putExtra(EventsActivity.EXTRA_EVENTS_CHANGED, true);
        resultIntent.putExtra(EventsActivity.EXTRA_EVENTS_COUNT, 1);
        resultIntent.putExtra(EventsActivity.EXTRA_CHANGE_TYPE, EventsActivity.CHANGE_TYPE_MODIFY);
        resultIntent.putExtra("modified_event_id", mEvent.getId());
        return resultIntent;
    }

    // ==================== ENHANCED VALIDATION ====================

    /**
     * Enhanced form validation with EventAction business rules.
     */
    private boolean validateEnhancedForm() {
        boolean isValid = true;
        List<String> validationErrors = new ArrayList<>();

        try {
            // Basic validation
            if (mEditTitle != null && TextUtils.isEmpty(mEditTitle.getText())) {
                mEditTitle.setError("Titolo richiesto");
                mEditTitle.requestFocus();
                validationErrors.add("title_required");
                Library.showError(getContext(), "Il titolo dell'evento è obbligatorio");
                isValid = false;
            }

            if (mStartDateTime == null) {
                validationErrors.add("start_date_required");
                Library.showError(getContext(), "Data di inizio richiesta");
                if (mStartDateButton != null) mStartDateButton.requestFocus();
                isValid = false;
            }

            // Enhanced: EventAction-specific validation
            if (mCurrentEventAction != null && mStartDateTime != null) {
                if (!validateEventActionConstraints(validationErrors)) {
                    isValid = false;
                }
            }

            // Date/time validation
            if (!validateDateTimeConstraints(validationErrors)) {
                isValid = false;
            }

            // Store validation errors in metadata
            if (mMetadataInitialized && !validationErrors.isEmpty()) {
                storeValidationErrors(validationErrors);
            }

            return isValid;

        } catch (Exception e) {
            Log.e(TAG, "Error during enhanced validation: " + e.getMessage());
            validationErrors.add("validation_exception");
            Library.showError(getContext(), "Errore durante la validazione");

            if (mMetadataInitialized) {
                storeValidationErrors(validationErrors);
            }

            return false;
        }
    }

    /**
     * Validate EventAction-specific constraints.
     */
    private boolean validateEventActionConstraints(List<String> validationErrors) {
        boolean isValid = true;

        try {
            // Check minimum advance notice
            LocalDate eventDate = mStartDateTime.toLocalDate();
            long daysUntilEvent = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), eventDate);
            int requiredNotice = mCurrentEventAction.getMinimumAdvanceNoticeDays();

            if (daysUntilEvent < requiredNotice) {
                validationErrors.add("insufficient_advance_notice");
                Library.showError(getContext(),
                        "L'azione '" + mCurrentEventAction.getDisplayName() +
                                "' richiede almeno " + requiredNotice + " giorni di preavviso");
                isValid = false;
            }

            // Check maximum duration
            if (mEndDateTime != null) {
                long durationDays = java.time.temporal.ChronoUnit.DAYS.between(eventDate, mEndDateTime.toLocalDate()) + 1;
                int maxDuration = mCurrentEventAction.getMaximumDurationDays();

                if (durationDays > maxDuration) {
                    validationErrors.add("duration_exceeds_maximum");
                    Library.showError(getContext(),
                            "L'azione '" + mCurrentEventAction.getDisplayName() +
                                    "' non può durare più di " + maxDuration + " giorni");
                    isValid = false;
                }
            }

            // Check if action can be performed on this date
            if (!mCurrentEventAction.canPerformOnDate(eventDate)) {
                validationErrors.add("action_not_allowed_on_date");
                Library.showError(getContext(),
                        "L'azione '" + mCurrentEventAction.getDisplayName() +
                                "' non può essere eseguita in questa data");
                isValid = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error validating EventAction constraints: " + e.getMessage());
            validationErrors.add("eventaction_validation_error");
        }

        return isValid;
    }

    /**
     * Validate date/time constraints.
     */
    private boolean validateDateTimeConstraints(List<String> validationErrors) {
        boolean isValid = true;

        try {
            if (mAllDaySwitch.isChecked()) {
                if (mEndDateTime == null) {
                    validationErrors.add("end_date_required_allday");
                    Library.showError(getContext(), "Data di fine richiesta");
                    if (mEndDateButton != null) mEndDateButton.requestFocus();
                    isValid = false;
                } else if (mStartDateTime != null) {
                    LocalDate startDate = mStartDateTime.toLocalDate();
                    LocalDate endDate = mEndDateTime.toLocalDate();

                    if (endDate.isBefore(startDate)) {
                        validationErrors.add("end_before_start_allday");
                        Library.showError(getContext(), "La data di fine deve essere uguale o successiva alla data di inizio");
                        if (mEndDateButton != null) mEndDateButton.requestFocus();
                        isValid = false;
                    }

                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                    if (daysBetween > 365) {
                        validationErrors.add("duration_too_long");
                        Library.showError(getContext(), "Durata evento superiore a 1 anno");
                    }
                }
            } else {
                if (mEndDateTime == null) {
                    validationErrors.add("end_datetime_required_timed");
                    Library.showError(getContext(), "Data e orario di fine richiesti");
                    if (mEndDateButton != null) mEndDateButton.requestFocus();
                    isValid = false;
                } else if (mStartDateTime != null) {
                    if (mEndDateTime.isBefore(mStartDateTime)) {
                        validationErrors.add("end_before_start_timed");
                        Library.showError(getContext(), "L'orario di fine deve essere successivo all'orario di inizio");
                        if (mEndTimeButton != null) mEndTimeButton.requestFocus();
                        isValid = false;
                    } else if (mEndDateTime.equals(mStartDateTime)) {
                        validationErrors.add("start_equals_end_timed");
                        Library.showError(getContext(), "L'orario di fine deve essere diverso dall'orario di inizio");
                        if (mEndTimeButton != null) mEndTimeButton.requestFocus();
                        isValid = false;
                    }

                    long hoursBetween = java.time.temporal.ChronoUnit.HOURS.between(mStartDateTime, mEndDateTime);
                    if (hoursBetween > 8760) {
                        validationErrors.add("timed_duration_too_long");
                        Library.showError(getContext(), "Durata evento superiore a 1 anno");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating date/time constraints: " + e.getMessage());
            validationErrors.add("datetime_validation_error");
        }

        return isValid;
    }

    /**
     * Store validation errors in metadata for analytics.
     */
    private void storeValidationErrors(List<String> validationErrors) {
        try {
            if (mEvent != null && mEvent.getCustomProperties() != null) {
                Map<String, String> props = mEvent.getCustomProperties();
                props.put("validation_errors", String.join(",", validationErrors));
                mEvent.setCustomProperties(props);
                Log.d(TAG, "Enhanced validation errors stored in metadata: " + validationErrors.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing validation errors: " + e.getMessage());
        }
    }

    /**
     * Log EventAction-specific analytics.
     */
    private void logEventActionAnalytics() {
        try {
            Log.i(TAG, "=== EVENTACTION ANALYTICS ===");
            Log.i(TAG, "Original EventAction: " + mOriginalEventAction);
            Log.i(TAG, "Current EventAction: " + mCurrentEventAction);
            Log.i(TAG, "EventAction Changed: " + mEventActionChanged);

            if (mCurrentEventAction != null) {
                Log.i(TAG, "Action Category: " + mCurrentEventAction.getCategory());
                Log.i(TAG, "Affects Work Schedule: " + mCurrentEventAction.affectsWorkSchedule());
                Log.i(TAG, "Requires Approval: " + mCurrentEventAction.requiresApproval());
                Log.i(TAG, "Requires Shift Coverage: " + mCurrentEventAction.requiresShiftCoverage());
                Log.i(TAG, "Cost Impact Factor: " + mCurrentEventAction.getCostImpactFactor());
                Log.i(TAG, "Min Advance Notice: " + mCurrentEventAction.getMinimumAdvanceNoticeDays() + " days");
                Log.i(TAG, "Max Duration: " + mCurrentEventAction.getMaximumDurationDays() + " days");
            }

            // Log QuickEventLogicAdapter metadata if available
            if (QuickEventLogicAdapter.isEventActionCreatedEvent(mEvent)) {
                Log.i(TAG, "Event was created via EventAction system");
                String metadataSummary = QuickEventLogicAdapter.getEventActionMetadataSummary(mEvent);
                Log.i(TAG, metadataSummary);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error logging EventAction analytics: " + e.getMessage());
        }
    }

    /**
     * Log enhanced alerts for critical changes that may need attention.
     */
    private void logEnhancedCriticalChangeAlerts() {
        try {
            if (mEvent == null) return;

            // Check if approval is required due to changes
            if (EventEditMetadataManager.requiresApprovalDueToChanges(mEvent)) {
                Log.w(TAG, "⚠️ ALERT: Event changes require approval workflow");
            }

            // Check business impact score
            int businessImpact = EventEditMetadataManager.getBusinessImpactScore(mEvent);
            if (businessImpact >= 7) {
                Log.w(TAG, "⚠️ ALERT: High business impact detected (score: " + businessImpact + ")");
            }

            // Check change severity
            EventEditMetadataManager.ChangeSeverity severity = EventEditMetadataManager.getChangeSeverity(mEvent);
            if (severity == EventEditMetadataManager.ChangeSeverity.CRITICAL) {
                Log.w(TAG, "⚠️ ALERT: Critical changes detected - may require immediate attention");
            }

            // Enhanced: Check EventAction-specific alerts
            if (mEventActionChanged && mCurrentEventAction != null) {
                Log.w(TAG, "⚠️ ALERT: EventAction changed to " + mCurrentEventAction.getDisplayName());

                if (mCurrentEventAction.requiresApproval() && (mOriginalEventAction == null || !mOriginalEventAction.requiresApproval())) {
                    Log.w(TAG, "⚠️ ALERT: Change now requires approval due to EventAction");
                }

                if (mCurrentEventAction.affectsWorkSchedule() && (mOriginalEventAction == null || !mOriginalEventAction.affectsWorkSchedule())) {
                    Log.w(TAG, "⚠️ ALERT: Change now affects work schedule due to EventAction");
                }
            }

            // Check if originally a quick event
            if (EventEditMetadataManager.wasOriginallyQuickEvent(mEvent)) {
                Log.i(TAG, "ℹ️ INFO: Originally created as quick event, now manually edited");
            }

            // Enhanced: Check EventAction business rule violations
            List<String> violations = EventEditMetadataManager.getEventActionViolations(mEvent);
            if (!violations.isEmpty()) {
                Log.w(TAG, "⚠️ ALERT: EventAction business rule violations: " + violations);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error logging enhanced critical change alerts: " + e.getMessage());
        }
    }

    // ==================== ENHANCED EVENT UPDATE ====================

    /**
     * Enhanced method to update event from form with EventAction integration.
     */
    private void updateEventFromFormWithEnhancedEnums() {
        if (mEvent == null) return;

        // Update basic fields
        mEvent.setTitle(mEditTitle.getText().toString().trim());
        mEvent.setDescription(mEditDescription.getText().toString().trim());
        mEvent.setLocation(mEditLocation.getText().toString().trim());
        mEvent.setStartTime(mStartDateTime);
        mEvent.setEndTime(mEndDateTime);
        mEvent.setAllDay(mAllDaySwitch.isChecked());

        // Enhanced EventType conversion
        EventType selectedEventType = convertDisplayNameToEventType(mEventTypeSpinner.getText().toString());
        mEvent.setEventType(selectedEventType);

        // Enhanced EventPriority conversion
        EventPriority selectedPriority = convertDisplayNameToEventPriority(mPrioritySpinner.getText().toString());
        mEvent.setPriority(selectedPriority);

        // Enhanced: Update EventAction metadata if changed
        if (mEventActionChanged && mCurrentEventAction != null) {
            updateEventActionMetadata();
        }

        // Enhanced: Apply EventAction business logic if appropriate
        if (mCurrentEventAction != null) {
            applyEventActionBusinessLogic();
        }

        mEvent.setLastUpdated(LocalDateTime.now());

        Log.d(TAG, "Enhanced event updated from form - Type: " + selectedEventType +
                ", Priority: " + selectedPriority + ", EventAction: " + mCurrentEventAction);
    }

    /**
     * Update EventAction-specific metadata.
     */
    private void updateEventActionMetadata() {
        try {
            Map<String, String> props = mEvent.getCustomProperties();
            if (props == null) props = new HashMap<>();

            // Update current EventAction metadata
            props.put("current_event_action", mCurrentEventAction.name());
            props.put("action_category", mCurrentEventAction.getCategory().name());
            props.put("event_action_changed", "true");
            props.put("action_change_timestamp", String.valueOf(System.currentTimeMillis()));

            // Update business rule metadata
            props.put("affects_work_schedule", String.valueOf(mCurrentEventAction.affectsWorkSchedule()));
            props.put("requires_approval", String.valueOf(mCurrentEventAction.requiresApproval()));
            props.put("requires_shift_coverage", String.valueOf(mCurrentEventAction.requiresShiftCoverage()));
            props.put("cost_impact_factor", String.valueOf(mCurrentEventAction.getCostImpactFactor()));
            props.put("requires_documentation", String.valueOf(mCurrentEventAction.requiresDocumentation()));
            props.put("manager_approval_required", String.valueOf(mCurrentEventAction.requiresManagerApproval()));

            mEvent.setCustomProperties(props);
            Log.d(TAG, "EventAction metadata updated for: " + mCurrentEventAction);

        } catch (Exception e) {
            Log.e(TAG, "Error updating EventAction metadata: " + e.getMessage());
        }
    }

    /**
     * Apply EventAction business logic to the event.
     */
    private void applyEventActionBusinessLogic() {
        try {
            // Adjust priority based on EventAction defaults if not explicitly set by user
            if (mEvent.getPriority() == null || !mChangedFields.contains("priority")) {
                EventPriority defaultPriority = mCurrentEventAction.getDefaultPriority();
                if (defaultPriority != null) {
                    mEvent.setPriority(defaultPriority);
                    Log.d(TAG, "Applied EventAction default priority: " + defaultPriority);
                }
            }

            // Adjust all-day setting based on EventAction defaults if not explicitly set
            if (!mChangedFields.contains("allDay")) {
                boolean defaultAllDay = mCurrentEventAction.isDefaultAllDay();
                if (mEvent.isAllDay() != defaultAllDay) {
                    mEvent.setAllDay(defaultAllDay);
                    Log.d(TAG, "Applied EventAction default all-day setting: " + defaultAllDay);
                }
            }

            // Apply default times if not set and not all-day
            if (!mEvent.isAllDay() && mStartDateTime != null && mEndDateTime != null) {
                // Check if we should apply default times
                if (!mChangedFields.contains("startTime") || !mChangedFields.contains("endTime")) {
                    applyEventActionDefaultTimes();
                }
            }

            // Add EventAction-specific description notes
            addEventActionDescriptionNotes();

        } catch (Exception e) {
            Log.e(TAG, "Error applying EventAction business logic: " + e.getMessage());
        }
    }

    /**
     * Apply EventAction default times if appropriate.
     */
    private void applyEventActionDefaultTimes() {
        try {
            java.time.LocalTime defaultStartTime = mCurrentEventAction.getDefaultStartTime();
            java.time.LocalTime defaultEndTime = mCurrentEventAction.getDefaultEndTime();

            if (defaultStartTime != null && !mChangedFields.contains("startTime")) {
                mStartDateTime = mStartDateTime.with(defaultStartTime);
                mEvent.setStartTime(mStartDateTime);
                updateTimeButtons();
                Log.d(TAG, "Applied EventAction default start time: " + defaultStartTime);
            }

            if (defaultEndTime != null && !mChangedFields.contains("endTime")) {
                mEndDateTime = mEndDateTime.with(defaultEndTime);
                mEvent.setEndTime(mEndDateTime);
                updateTimeButtons();
                Log.d(TAG, "Applied EventAction default end time: " + defaultEndTime);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error applying EventAction default times: " + e.getMessage());
        }
    }

    /**
     * Add EventAction-specific description notes.
     */
    private void addEventActionDescriptionNotes() {
        try {
            String currentDescription = mEvent.getDescription();
            if (currentDescription == null) currentDescription = "";

            List<String> notes = new ArrayList<>();

            if (mCurrentEventAction.requiresApproval() && !currentDescription.contains("approvazione")) {
                String approvalNote = mCurrentEventAction.requiresManagerApproval() ?
                        "Richiede approvazione manager" : "Richiede approvazione";
                notes.add(approvalNote);
            }

            if (mCurrentEventAction.requiresDocumentation() && !currentDescription.contains("documentazione")) {
                notes.add("Documentazione richiesta");
            }

            if (mCurrentEventAction.requiresShiftCoverage() && !currentDescription.contains("copertura")) {
                notes.add("Richiede copertura turno");
            }

            if (!notes.isEmpty()) {
                String notesString = String.join(", ", notes);
                if (!currentDescription.isEmpty()) {
                    currentDescription += " (" + notesString + ")";
                } else {
                    currentDescription = "(" + notesString + ")";
                }
                mEvent.setDescription(currentDescription);
                Log.d(TAG, "Added EventAction description notes: " + notesString);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding EventAction description notes: " + e.getMessage());
        }
    }

    // ==================== ENHANCED ENUM CONVERSION ====================

    /**
     * Convert display name back to EventType enum with intelligent matching.
     */
    private EventType convertDisplayNameToEventType(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            Log.w(TAG, "Empty display name for EventType, using default");
            return EventType.GENERAL;
        }

        try {
            // First try: exact display name matching
            for (EventType eventType : mAvailableEventTypes) {
                if (displayName.equals(eventType.getDisplayName())) {
                    Log.d(TAG, "EventType matched by display name: " + displayName + " -> " + eventType);
                    return eventType;
                }
            }

            // Second try: case-insensitive display name matching
            for (EventType eventType : mAvailableEventTypes) {
                if (displayName.equalsIgnoreCase(eventType.getDisplayName())) {
                    Log.d(TAG, "EventType matched by case-insensitive display name: " + displayName + " -> " + eventType);
                    return eventType;
                }
            }

            // Third try: enum name matching (fallback compatibility)
            for (EventType eventType : mAvailableEventTypes) {
                if (displayName.equals(eventType.name()) || displayName.equalsIgnoreCase(eventType.name())) {
                    Log.d(TAG, "EventType matched by enum name: " + displayName + " -> " + eventType);
                    return eventType;
                }
            }

            Log.w(TAG, "No EventType match found for: " + displayName + ", using default GENERAL");
            return EventType.GENERAL;

        } catch (Exception e) {
            Log.e(TAG, "Error converting display name to EventType: " + e.getMessage());
            return EventType.GENERAL;
        }
    }

    /**
     * Convert display name back to EventPriority enum with intelligent matching.
     */
    private EventPriority convertDisplayNameToEventPriority(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            Log.w(TAG, "Empty display name for EventPriority, using default");
            return EventPriority.NORMAL;
        }

        try {
            // First try: exact display name matching
            for (EventPriority priority : mAvailablePriorities) {
                if (displayName.equals(priority.getDisplayName())) {
                    Log.d(TAG, "EventPriority matched by display name: " + displayName + " -> " + priority);
                    return priority;
                }
            }

            // Second try: case-insensitive display name matching
            for (EventPriority priority : mAvailablePriorities) {
                if (displayName.equalsIgnoreCase(priority.getDisplayName())) {
                    Log.d(TAG, "EventPriority matched by case-insensitive display name: " + displayName + " -> " + priority);
                    return priority;
                }
            }

            // Third try: enum name matching (fallback compatibility)
            for (EventPriority priority : mAvailablePriorities) {
                if (displayName.equals(priority.name()) || displayName.equalsIgnoreCase(priority.name())) {
                    Log.d(TAG, "EventPriority matched by enum name: " + displayName + " -> " + priority);
                    return priority;
                }
            }

            Log.w(TAG, "No EventPriority match found for: " + displayName + ", using default NORMAL");
            return EventPriority.NORMAL;

        } catch (Exception e) {
            Log.e(TAG, "Error converting display name to EventPriority: " + e.getMessage());
            return EventPriority.NORMAL;
        }
    }

    // ==================== NAVIGATION AND CLEANUP ====================

    private void cancelEdit() {
        if (mHasUnsavedChanges) {
            handleUnsavedChanges(this::navigateBack, () -> {
                // User cancelled navigation
            });
        } else {
            navigateBack();
        }
    }

    /**
     * Navigates back with proper error handling and fallbacks.
     */
    private void navigateBack() {
        try {
            if (getView() != null) {
                Navigation.findNavController(requireView()).popBackStack();
            } else {
                throw new IllegalStateException("View is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error with Navigation Component: " + e.getMessage());
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    private void showErrorAndNavigateBack(String message) {
        try {
            if (getContext() != null) {
                Library.showError(getContext(), message);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    navigateBack();
                }, 2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message: " + e.getMessage());
        }
    }

    // ==================== UNSAVED CHANGES INTERFACE ====================

    @Override
    public boolean hasUnsavedChanges() {
        return mHasUnsavedChanges;
    }

    /**
     * Handles unsaved changes with user confirmation dialog.
     */
    @Override
    public void handleUnsavedChanges(@NonNull Runnable onProceed, @NonNull Runnable onCancel) {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Modifiche non salvate")
                    .setMessage("Hai modifiche non salvate. Vuoi uscire senza salvare?")
                    .setPositiveButton("Esci senza salvare", (dialog, which) -> {
                        mHasUnsavedChanges = false;
                        onProceed.run();
                    })
                    .setNegativeButton("Continua modifica", (dialog, which) -> {
                        onCancel.run();
                    })
                    .setNeutralButton("Salva e esci", (dialog, which) -> {
                        saveEvent();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing unsaved changes dialog: " + e.getMessage());
            navigateBack();
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get current user ID (implement based on your user management system).
     */
    private Long getCurrentUserId() {
        try {
            // TODO: Implement based on your user management system
            return 1L; // Placeholder
        } catch (Exception e) {
            Log.w(TAG, "Could not retrieve current user ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Legacy method for toolbar home button handling.
     */
    public void onHomePressed() {
        cancelEdit();
    }

    // ==================== ENHANCED DEBUG AND TESTING METHODS ====================

    /**
     * Get enhanced metadata summary for debugging purposes.
     */
    public String getEnhancedMetadataSummaryForDebugging() {
        if (mEvent != null) {
            return EventEditMetadataManager.getEnhancedEditSessionSummary(mEvent);
        }
        return "No enhanced metadata available";
    }

    /**
     * Get current EventAction for debugging/testing purposes.
     */
    public EventAction getCurrentEventAction() {
        return mCurrentEventAction;
    }

    /**
     * Get original EventAction for debugging/testing purposes.
     */
    public EventAction getOriginalEventAction() {
        return mOriginalEventAction;
    }

    /**
     * Check if EventAction changed for debugging/testing purposes.
     */
    public boolean hasEventActionChanged() {
        return mEventActionChanged;
    }

    /**
     * Get current EventType selection for debugging/testing purposes.
     */
    public EventType getCurrentEventType() {
        if (mEventTypeSpinner == null) return null;
        return convertDisplayNameToEventType(mEventTypeSpinner.getText().toString());
    }

    /**
     * Get current EventPriority selection for debugging/testing purposes.
     */
    public EventPriority getCurrentEventPriority() {
        if (mPrioritySpinner == null) return null;
        return convertDisplayNameToEventPriority(mPrioritySpinner.getText().toString());
    }

    /**
     * Get changed fields list for testing.
     */
    public List<String> getChangedFieldsForTesting() {
        return new ArrayList<>(mChangedFields);
    }

    /**
     * Check if metadata tracking is properly initialized for testing.
     */
    public boolean isMetadataTrackingInitialized() {
        return mMetadataInitialized;
    }

    /**
     * Get edit session ID for testing.
     */
    public String getEditSessionIdForTesting() {
        return mEditSessionId;
    }

    /**
     * Force metadata update for testing purposes.
     */
    public void forceMetadataUpdateForTesting() {
        if (mMetadataInitialized) {
            updateMetadataForCurrentChanges();
        }
    }

    /**
     * Get available EventType options for debugging/testing purposes.
     */
    public List<EventType> getAvailableEventTypes() {
        return new ArrayList<>(mAvailableEventTypes);
    }

    /**
     * Get available EventPriority options for debugging/testing purposes.
     */
    public List<EventPriority> getAvailablePriorities() {
        return new ArrayList<>(mAvailablePriorities);
    }

// ==================== ENHANCED METADATA LOGGING ====================

    /**
     * Log comprehensive enhanced metadata summary for analytics.
     */
    private void logEnhancedMetadataSummary() {
        try {
            if (mEvent != null) {
                String summary = EventEditMetadataManager.getEnhancedEditSessionSummary(mEvent);
                Log.i(TAG, "=== ENHANCED EDIT SESSION COMPLETED ===");
                Log.i(TAG, summary);

                // Enhanced: Log EventAction-specific information
                logEventActionAnalytics();

                // Check for critical changes that need attention
                logEnhancedCriticalChangeAlerts();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging enhanced metadata summary: " + e.getMessage());
        }
    }

// ==================== ENHANCED WARNING DIALOGS ====================

    /**
     * Enhanced turn impact warning with EventAction business context.
     */
    private void showEnhancedTurnImpactWarning(EventType originalType, EventType newType,
                                               EventAction originalAction, EventAction newAction) {
        try {
            String message = buildEnhancedTurnImpactMessage(originalType, newType, originalAction, newAction);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Impatto Business dell'Azione")
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("Continua", (dialog, which) -> {
                        trackUserOverride("enhanced_turn_impact_acknowledged");
                    })
                    .setNegativeButton("Rivedi", (dialog, which) -> {
                        restoreOriginalEventType();
                    })
                    .setNeutralButton("Dettagli", (dialog, which) -> {
                        showEventActionDetailsDialog(newAction);
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing enhanced turn impact warning: " + e.getMessage());
        }
    }

    /**
     * Build enhanced impact message with EventAction business context.
     */
    private String buildEnhancedTurnImpactMessage(EventType originalType, EventType newType,
                                                  EventAction originalAction, EventAction newAction) {
        StringBuilder message = new StringBuilder();

        message.append("Modifica dell'azione da ");

        if (originalAction != null) {
            message.append("'").append(originalAction.getDisplayName()).append("' (").append(originalAction.getCategory().getDisplayName()).append(")");
        } else {
            message.append("'").append(originalType != null ? originalType.getDisplayName() : "Non specificato").append("'");
        }

        message.append(" a ");

        if (newAction != null) {
            message.append("'").append(newAction.getDisplayName()).append("' (").append(newAction.getCategory().getDisplayName()).append(")");
        } else {
            message.append("'").append(newType != null ? newType.getDisplayName() : "Non specificato").append("'");
        }

        message.append("\n\n🔍 Analisi Impatti:\n");

        if (originalAction != null && newAction != null) {
            // Category change impact
            if (originalAction.getCategory() != newAction.getCategory()) {
                message.append("• Cambio categoria: ").append(originalAction.getCategory().getDisplayName())
                        .append(" → ").append(newAction.getCategory().getDisplayName()).append("\n");
            }

            // Work schedule impact
            if (originalAction.affectsWorkSchedule() != newAction.affectsWorkSchedule()) {
                if (newAction.affectsWorkSchedule()) {
                    message.append("• ⚠️ Ora impatta la schedulazione dei turni\n");
                } else {
                    message.append("• ✅ Non impatta più la schedulazione dei turni\n");
                }
            }

            // Approval requirement change
            if (originalAction.requiresApproval() != newAction.requiresApproval()) {
                if (newAction.requiresApproval()) {
                    message.append("• 📋 Ora richiede approvazione");
                    if (newAction.requiresManagerApproval()) {
                        message.append(" (manager)");
                    }
                    message.append("\n");
                } else {
                    message.append("• ✅ Non richiede più approvazione\n");
                }
            }

            // Shift coverage change
            if (originalAction.requiresShiftCoverage() != newAction.requiresShiftCoverage()) {
                if (newAction.requiresShiftCoverage()) {
                    message.append("• 👥 Richiede copertura del turno\n");
                } else {
                    message.append("• ✅ Non richiede più copertura del turno\n");
                }
            }

            // Cost impact change
            double originalCost = originalAction.getCostImpactFactor();
            double newCost = newAction.getCostImpactFactor();
            if (Math.abs(originalCost - newCost) > 0.1) {
                message.append("• 💰 Impatto economico: ").append(originalCost)
                        .append(" → ").append(newCost).append("\n");
            }

            // Documentation requirement
            if (originalAction.requiresDocumentation() != newAction.requiresDocumentation()) {
                if (newAction.requiresDocumentation()) {
                    message.append("• 📄 Richiede documentazione aggiuntiva\n");
                }
            }

            // Constraints differences
            if (originalAction.getMinimumAdvanceNoticeDays() != newAction.getMinimumAdvanceNoticeDays()) {
                message.append("• ⏰ Preavviso richiesto: ").append(originalAction.getMinimumAdvanceNoticeDays())
                        .append(" → ").append(newAction.getMinimumAdvanceNoticeDays()).append(" giorni\n");
            }

            if (originalAction.getMaximumDurationDays() != newAction.getMaximumDurationDays()) {
                message.append("• 📅 Durata massima: ").append(originalAction.getMaximumDurationDays())
                        .append(" → ").append(newAction.getMaximumDurationDays()).append(" giorni\n");
            }
        }

        message.append("\n🤔 Vuoi continuare con questa modifica?");

        return message.toString();
    }

    /**
     * Show EventAction details dialog.
     */
    private void showEventActionDetailsDialog(EventAction action) {
        if (action == null) return;

        StringBuilder details = new StringBuilder();
        details.append("📋 Dettagli Azione: ").append(action.getDisplayName()).append("\n\n");
        details.append("🏷️ Categoria: ").append(action.getCategory().getDisplayName()).append("\n");
        details.append("💼 Impatta turni: ").append(action.affectsWorkSchedule() ? "Sì" : "No").append("\n");
        details.append("✅ Richiede approvazione: ").append(action.requiresApproval() ? "Sì" : "No").append("\n");
        details.append("👥 Richiede copertura: ").append(action.requiresShiftCoverage() ? "Sì" : "No").append("\n");
        details.append("💰 Fattore costo: ").append(action.getCostImpactFactor()).append("\n");
        details.append("📄 Richiede documentazione: ").append(action.requiresDocumentation() ? "Sì" : "No").append("\n");
        details.append("⏰ Preavviso minimo: ").append(action.getMinimumAdvanceNoticeDays()).append(" giorni\n");
        details.append("📅 Durata massima: ").append(action.getMaximumDurationDays()).append(" giorni\n");

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Dettagli Azione")
                .setMessage(details.toString())
                .setPositiveButton("Chiudi", null)
                .show();
    }

    /**
     * Restore original EventType if user wants to review.
     */
    private void restoreOriginalEventType() {
        try {
            if (mOriginalEvent != null && mOriginalEvent.getEventType() != null) {
                setEventTypeSelection(mOriginalEvent.getEventType());

                // Reset EventAction tracking
                mCurrentEventAction = mOriginalEventAction;
                mEventActionChanged = false;

                // Remove the EventType change from tracked changes
                mChangedFields.remove("eventType");
                trackUserOverride("event_type_restored");

                Log.d(TAG, "EventType restored to original value");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring original EventType: " + e.getMessage());
        }
    }

    /**
     * Track user overrides and manual corrections.
     */
    private void trackUserOverride(String overrideType) {
        try {
            if (mEvent != null && mEvent.getCustomProperties() != null) {
                Map<String, String> props = mEvent.getCustomProperties();

                String existingOverrides = props.get("user_overrides");
                if (existingOverrides == null) {
                    existingOverrides = overrideType;
                } else {
                    existingOverrides += "," + overrideType;
                }

                props.put("user_overrides", existingOverrides);
                mEvent.setCustomProperties(props);

                Log.d(TAG, "User override tracked: " + overrideType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking user override: " + e.getMessage());
        }
    }
}

