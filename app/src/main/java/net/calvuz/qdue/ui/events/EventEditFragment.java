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
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * EventEditFragment - Edit existing events
 *
 * Features:
 * - Form-based editing of all event fields
 * - Date/time pickers
 * - Validation
 * - Save to database
 * - Cancel with confirmation
 */
public class EventEditFragment extends Fragment {

    private static final String TAG = "EventEditFragment";
    private static final String ARG_EVENT_ID = "eventId";

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
    private String mEventId;
    private LocalDateTime mStartDateTime;
    private LocalDateTime mEndDateTime;

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

        if (getArguments() != null) {
            mEventId = getArguments().getString(ARG_EVENT_ID);
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
     */
    private void loadEventData() {
        if (TextUtils.isEmpty(mEventId)) {
            Log.e(TAG, "No event ID provided");
            showError("Errore: ID evento mancante");
            return;
        }

        new Thread(() -> {
            try {
                EventDao eventDao = EventsDatabase.getInstance(requireContext()).eventDao();
                LocalEvent event = eventDao.getEventById(mEventId);

                requireActivity().runOnUiThread(() -> {
                    if (event != null) {
                        mEvent = event;
                        populateForm();
                        Log.d(TAG, "Event loaded for editing: " + event.getTitle());
                    } else {
                        showError("Evento non trovato");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading event: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        showError("Errore caricamento evento"));
            }
        }).start();
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
     * Show date picker dialog
     */
    private void showDatePicker(boolean isStart) {
        LocalDateTime dateTime = isStart ? mStartDateTime : mEndDateTime;
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(dateTime.getYear(), dateTime.getMonthValue() - 1, dateTime.getDayOfMonth());

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
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
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /**
     * Show time picker dialog
     */
    private void showTimePicker(boolean isStart) {
        LocalDateTime dateTime = isStart ? mStartDateTime : mEndDateTime;
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    if (isStart) {
                        if (mStartDateTime == null) {
                            mStartDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                        } else {
                            mStartDateTime = mStartDateTime.withHour(hourOfDay).withMinute(minute);
                        }
                        mStartTimeButton.setText(mStartDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                    } else {
                        if (mEndDateTime == null) {
                            mEndDateTime = LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
                        } else {
                            mEndDateTime = mEndDateTime.withHour(hourOfDay).withMinute(minute);
                        }
                        mEndTimeButton.setText(mEndDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                    }
                },
                dateTime.getHour(),
                dateTime.getMinute(),
                true
        );

        dialog.show();
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

        if (itemId == R.id.action_save_event) {
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
     */
    private void saveEvent() {
        if (!validateForm()) {
            return;
        }

        // Update event with form data
        updateEventFromForm();

        // Save to database
        new Thread(() -> {
            try {
                EventDao eventDao = EventsDatabase.getInstance(requireContext()).eventDao();
                eventDao.updateEvent(mEvent);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Evento salvato", Toast.LENGTH_SHORT).show();

                    // Navigate back to detail
                    Navigation.findNavController(requireView()).popBackStack();
                });

                Log.d(TAG, "Event updated successfully: " + mEvent.getTitle());

            } catch (Exception e) {
                Log.e(TAG, "Error updating event: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Errore salvataggio", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * Validate form fields
     */
    private boolean validateForm() {
        // Title required
        if (TextUtils.isEmpty(mTitleEdit.getText())) {
            mTitleEdit.setError("Titolo richiesto");
            return false;
        }

        // Start date required
        if (mStartDateTime == null) {
            Toast.makeText(getContext(), "Data inizio richiesta", Toast.LENGTH_SHORT).show();
            return false;
        }

        // End date required
        if (mEndDateTime == null) {
            Toast.makeText(getContext(), "Data fine richiesta", Toast.LENGTH_SHORT).show();
            return false;
        }

        // End must be after start
        if (mEndDateTime.isBefore(mStartDateTime)) {
            Toast.makeText(getContext(), "La data fine deve essere dopo l'inizio", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
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
     * Cancel editing with confirmation
     */
    private void cancelEdit() {
        // TODO: Show confirmation dialog if changes were made
        Navigation.findNavController(requireView()).popBackStack();
    }

    /**
     * Show error and navigate back
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}