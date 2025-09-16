package net.calvuz.qdue.ui.features.events.local.presentation.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;

/**
 * EditLocalEventFragment - Full-screen Event Editor (MVVM)
 *
 * <p>Full-screen Fragment for creating and editing LocalEvent entities following MVVM architecture.
 * Provides comprehensive form interface for all core event properties excluding custom properties
 * and package-related fields.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Full-screen Experience</strong>: Complete event editing interface</li>
 *   <li><strong>MVVM Integration</strong>: Reactive updates through LocalEventsViewModel</li>
 *   <li><strong>Input Validation</strong>: Real-time validation with user feedback</li>
 *   <li><strong>Date/Time Pickers</strong>: Material Design date and time selection</li>
 *   <li><strong>All-day Support</strong>: Toggle between timed and all-day events</li>
 *   <li><strong>Internationalization</strong>: Uses LocaleManager for all text</li>
 * </ul>
 *
 * <h3>Navigation Arguments:</h3>
 * <ul>
 *   <li><code>event_id</code> (String, optional): Event ID for editing mode</li>
 *   <li><code>default_date</code> (String, optional): Default date in ISO format</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class EditLocalEventFragment extends Fragment
{

    private static final String TAG = "EditLocalEventFragment";

    // Navigation arguments
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_DEFAULT_DATE = "default_date";

    // ==================== UI COMPONENTS ====================

    private Toolbar mToolbar;
    private TextInputLayout mTitleInputLayout;
    private TextInputEditText mTitleEditText;
    private TextInputLayout mDescriptionInputLayout;
    private TextInputEditText mDescriptionEditText;
    private TextInputLayout mLocationInputLayout;
    private TextInputEditText mLocationEditText;

    // Date and Time Controls
    private MaterialButton mDateButton;
    private MaterialButton mStartTimeButton;
    private MaterialButton mEndTimeButton;
    private CheckBox mAllDayCheckBox;

    // Action Buttons
    private MaterialButton mSaveButton;
    private MaterialButton mCancelButton;
    private MaterialButton mDeleteButton;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;

    // ==================== STATE ====================

    private LocalEvent mCurrentEvent;
    private boolean mIsEditMode = false;
    private LocalDate mSelectedDate = LocalDate.now();
    private LocalTime mStartTime = LocalTime.of( 9, 0 );
    private LocalTime mEndTime = LocalTime.of( 10, 0 );

    // ==================== DATE/TIME FORMATTERS ====================

    private final DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern( "dd/MM/yyyy" );
    private final DateTimeFormatter mTimeFormatter = DateTimeFormatter.ofPattern( "HH:mm" );

    // ==================== LISTENERS ====================

    private BaseViewModel.LoadingStateListener mLoadingStateListener;
    private BaseViewModel.ErrorStateListener mErrorStateListener;
    private BaseViewModel.EventListener mEventListener;

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );

        // Get ViewModel from parent Activity
        if (getActivity() instanceof LocalEventsActivity) {
            LocalEventsActivity activity = (LocalEventsActivity) getActivity();
            mEventsViewModel = activity.getEventsViewModel();
        }

        if (mEventsViewModel == null) {
            Log.e( TAG, "Could not get LocalEventsViewModel from parent Activity" );
            // Handle error - perhaps show error state or finish
            return;
        }

        // Parse arguments
        parseArguments();

        // Setup listeners
        setupViewModelListeners();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate( R.layout.fragment_edit_local_event, container, false );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        initializeViews( view );
        setupToolbar();
        setupClickListeners();

        if (mIsEditMode) {
            loadEventForEditing();
        } else {
            setupNewEventDefaults();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove listeners to prevent memory leaks
        if (mEventsViewModel != null) {
            if (mLoadingStateListener != null) {
                mEventsViewModel.removeLoadingStateListener( mLoadingStateListener );
            }
            if (mErrorStateListener != null) {
                mEventsViewModel.removeErrorStateListener( mErrorStateListener );
            }
            if (mEventListener != null) {
                mEventsViewModel.removeEventListener( mEventListener );
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate( R.menu.edit_event_menu, menu );
        super.onCreateOptionsMenu( menu, inflater );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            handleBackNavigation();
            return true;
        } else if (itemId == R.id.action_save_event) {
            saveEvent();
            return true;
        } else if (itemId == R.id.action_delete_event && mIsEditMode) {
            deleteEvent();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    // ==================== INITIALIZATION ====================

    private void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            String eventId = args.getString( ARG_EVENT_ID );
            String defaultDateStr = args.getString( ARG_DEFAULT_DATE );

            mIsEditMode = eventId != null;

            if (defaultDateStr != null) {
                try {
                    mSelectedDate = LocalDate.parse( defaultDateStr );
                } catch (DateTimeParseException e) {
                    Log.w( TAG, "Invalid default date format: " + defaultDateStr );
                }
            }
        }
    }

    private void setupViewModelListeners() {
        if (mEventsViewModel == null) return;

        // Loading state listener
        mLoadingStateListener = (operation, loading) -> {
            if (LocalEventsViewModel.OP_CREATE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_UPDATE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_DELETE_EVENT.equals( operation )) {
                handleLoadingState( operation, loading );
            }
        };
        mEventsViewModel.addLoadingStateListener( mLoadingStateListener );

        // Error state listener
        mErrorStateListener = (operation, error) -> {
            if (LocalEventsViewModel.OP_CREATE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_UPDATE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_DELETE_EVENT.equals( operation )) {
                handleErrorState( operation, error );
            }
        };
        mEventsViewModel.addErrorStateListener( mErrorStateListener );

        // Event listener for UI actions
        mEventListener = event -> {
            if (event instanceof BaseViewModel.UIActionEvent) {
                BaseViewModel.UIActionEvent uiEvent = (BaseViewModel.UIActionEvent) event;
                handleUIActionEvent( uiEvent );
            }
        };
        mEventsViewModel.addEventListener( mEventListener );
    }

    private void initializeViews(@NonNull View view) {
        // Toolbar
        mToolbar = view.findViewById( R.id.toolbar_edit_event );

        // Input Fields
        mTitleInputLayout = view.findViewById( R.id.title_input_layout );
        mTitleEditText = view.findViewById( R.id.title_edit_text );
        mDescriptionInputLayout = view.findViewById( R.id.description_input_layout );
        mDescriptionEditText = view.findViewById( R.id.description_edit_text );
        mLocationInputLayout = view.findViewById( R.id.location_input_layout );
        mLocationEditText = view.findViewById( R.id.location_edit_text );

        // Date and Time Controls
        mDateButton = view.findViewById( R.id.date_button );
        mStartTimeButton = view.findViewById( R.id.start_time_button );
        mEndTimeButton = view.findViewById( R.id.end_time_button );
        mAllDayCheckBox = view.findViewById( R.id.all_day_checkbox );

        // Action Buttons
        mSaveButton = view.findViewById( R.id.save_button );
        mCancelButton = view.findViewById( R.id.cancel_button );
        mDeleteButton = view.findViewById( R.id.delete_button );

        // Show/hide delete button based on mode
        mDeleteButton.setVisibility( mIsEditMode ? View.VISIBLE : View.GONE );
    }

    private void setupToolbar() {
        if (mToolbar != null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                activity.setSupportActionBar( mToolbar );
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled( true );
                    activity.getSupportActionBar().setTitle( mIsEditMode ?
                                                                     LocaleManager.getLocalizedString(
                                                                             getContext(),
                                                                             "edit_event",
                                                                             "Edit Event" ) :
                                                                     LocaleManager.getLocalizedString(
                                                                             getContext(),
                                                                             "create_event",
                                                                             "Create Event" ) );
                }
            }
        }
    }

    private void setupClickListeners() {
        // Date and Time Buttons
        mDateButton.setOnClickListener( v -> showDatePicker() );
        mStartTimeButton.setOnClickListener( v -> showStartTimePicker() );
        mEndTimeButton.setOnClickListener( v -> showEndTimePicker() );

        // All-day toggle
        mAllDayCheckBox.setOnCheckedChangeListener( (buttonView, isChecked) -> {
            updateTimeControlsVisibility( isChecked );
        } );

        // Action Buttons
        mSaveButton.setOnClickListener( v -> saveEvent() );
        mCancelButton.setOnClickListener( v -> handleBackNavigation() );
        mDeleteButton.setOnClickListener( v -> deleteEvent() );
    }

    // ==================== EVENT OPERATIONS ====================

    private void loadEventForEditing() {
        Bundle args = getArguments();
        if (args != null) {
            String eventId = args.getString( ARG_EVENT_ID );
            if (eventId != null && mEventsViewModel != null) {
                // Find event in current events list
                List<LocalEvent> allEvents = mEventsViewModel.getAllEvents();
                for (LocalEvent event : allEvents) {
                    if (eventId.equals( event.getId() )) {
                        mCurrentEvent = event;
                        populateFields( event );
                        return;
                    }
                }
                // Event not found
                showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_found",
                                                             "Event not found" ) );
                handleBackNavigation();
            }
        }
    }

    private void setupNewEventDefaults() {
        mCurrentEvent = LocalEvent.builder()
                .allDay( true )
                .eventType( EventType.GENERAL )
                .priority( Priority.NORMAL )
                .build();
//        mCurrentEvent.setAllDay(true);
//        mCurrentEvent.setEventType(EventType.GENERAL);
//        mCurrentEvent.setPriority(Priority.NORMAL);

        updateDateTimeDisplay();
        updateTimeControlsVisibility( true );
        mAllDayCheckBox.setChecked( true );
    }

    private void populateFields(@NonNull LocalEvent event) {
        // Basic fields
        mTitleEditText.setText( event.getTitle() );
        mDescriptionEditText.setText( event.getDescription() );
        mLocationEditText.setText( event.getLocation() );

        // Date and time
        if (event.getStartTime() != null) {
            mSelectedDate = event.getStartTime().toLocalDate();
            if (!event.isAllDay()) {
                mStartTime = event.getStartTime().toLocalTime();
                if (event.getEndTime() != null) {
                    mEndTime = event.getEndTime().toLocalTime();
                }
            }
        }

        // All-day checkbox
        mAllDayCheckBox.setChecked( event.isAllDay() );
        updateTimeControlsVisibility( event.isAllDay() );
        updateDateTimeDisplay();
    }

    /**
     * Save event to database
     * LocalEvent class is immutable so lets create new instances every time..
     */
    private void saveEvent() {
        if (!validateInput()) {
            return;
        }

        LocalEvent.Builder builder;

        // Create or update event
        if (mCurrentEvent != null) {
            builder = mCurrentEvent.toBuilder();
        } else {
            builder = LocalEvent.builder();
        }

        // Set basic properties
        builder.title( mTitleEditText.getText().toString().trim() );
        builder.description( mDescriptionEditText.getText().toString().trim() );
        builder.location( mLocationEditText.getText().toString().trim() );

        // Set date and time
        builder.allDay( mAllDayCheckBox.isChecked() );
        if (mAllDayCheckBox.isChecked()) {
            builder.startTime( mSelectedDate.atStartOfDay() );
            builder.endTime( mSelectedDate.atTime( 23, 59 ) );
        } else {
            builder.startTime( LocalDateTime.of( mSelectedDate, mStartTime ) );
            builder.endTime( LocalDateTime.of( mSelectedDate, mEndTime ) );
        }

        // Set default values for required fields
        builder.eventType( EventType.GENERAL );
        builder.priority( Priority.NORMAL );

        // Current Event
        mCurrentEvent = builder.build();

        // Save through ViewModel
        if (mEventsViewModel != null) {
            if (mIsEditMode) {
                mEventsViewModel.updateEvent( mCurrentEvent );
            } else {
                mEventsViewModel.createEvent( mCurrentEvent );
            }
        }
    }

    private void deleteEvent() {
        if (mCurrentEvent != null && mEventsViewModel != null) {
            mEventsViewModel.deleteEvent( mCurrentEvent.getId() );
        }
    }

    // ==================== INPUT VALIDATION ====================

    private boolean validateInput() {
        boolean isValid = true;

        // Title validation
        String title = mTitleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            mTitleInputLayout.setError( LocaleManager.getLocalizedString( getContext(),
                                                                          "error_title_required",
                                                                          "Title is required" ) );
            isValid = false;
        } else {
            mTitleInputLayout.setError( null );
        }

        // Time validation for timed events
        if (!mAllDayCheckBox.isChecked()) {
            if (mStartTime.isAfter( mEndTime )) {
                showError( LocaleManager.getLocalizedString( getContext(),
                                                             "error_end_time_before_start",
                                                             "End time must be after start time" ) );
                isValid = false;
            }
        }

        return isValid;
    }

    // ==================== DATE/TIME PICKERS ====================

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.set( mSelectedDate.getYear(), mSelectedDate.getMonthValue() - 1,
                      mSelectedDate.getDayOfMonth() );

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    mSelectedDate = LocalDate.of( year, month + 1, dayOfMonth );
                    updateDateTimeDisplay();
                },
                calendar.get( Calendar.YEAR ),
                calendar.get( Calendar.MONTH ),
                calendar.get( Calendar.DAY_OF_MONTH )
        );

        datePickerDialog.show();
    }

    private void showStartTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    mStartTime = LocalTime.of( hourOfDay, minute );
                    // Auto-adjust end time to be 1 hour after start time
                    if (mStartTime.isAfter( mEndTime ) || mStartTime.equals( mEndTime )) {
                        mEndTime = mStartTime.plusHours( 1 );
                    }
                    updateDateTimeDisplay();
                },
                mStartTime.getHour(),
                mStartTime.getMinute(),
                true
        );

        timePickerDialog.show();
    }

    private void showEndTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    mEndTime = LocalTime.of( hourOfDay, minute );
                    updateDateTimeDisplay();
                },
                mEndTime.getHour(),
                mEndTime.getMinute(),
                true
        );

        timePickerDialog.show();
    }

    // ==================== UI UPDATES ====================

    private void updateDateTimeDisplay() {
        mDateButton.setText( mSelectedDate.format( mDateFormatter ) );
        mStartTimeButton.setText( mStartTime.format( mTimeFormatter ) );
        mEndTimeButton.setText( mEndTime.format( mTimeFormatter ) );
    }

    private void updateTimeControlsVisibility(boolean isAllDay) {
        int visibility = isAllDay ? View.GONE : View.VISIBLE;
        mStartTimeButton.setVisibility( visibility );
        mEndTimeButton.setVisibility( visibility );
    }

    // ==================== EVENT HANDLERS ====================

    private void handleLoadingState(@NonNull String operation, boolean loading) {
        // Disable/enable UI during operations
        if (mSaveButton != null) {
            mSaveButton.setEnabled( !loading );
        }
        if (mDeleteButton != null) {
            mDeleteButton.setEnabled( !loading );
        }

        Log.d( TAG, "Loading state: " + operation + " - " + loading );
    }

    private void handleErrorState(@NonNull String operation, @Nullable String error) {
        if (error != null) {
            showError( error );
        }
    }

    private void handleUIActionEvent(@NonNull BaseViewModel.UIActionEvent event) {
        String action = event.getAction();

        if ("SHOW_SUCCESS".equals( action )) {
            String message = event.getData( "message", String.class );
            if (message != null) {
                showSuccess( message );
                // Navigate back on success
                handleBackNavigation();
            }
        } else if ("SHOW_ERROR".equals( action )) {
            String message = event.getData( "message", String.class );
            if (message != null) {
                showError( message );
            }
        }
    }

    private void handleBackNavigation() {
        NavController navController = Navigation.findNavController( requireView() );
        navController.navigateUp();
    }

    // ==================== UTILITY METHODS ====================

    private void showError(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make( getView(), message, Snackbar.LENGTH_LONG ).show();
        }
        Log.e( TAG, "Error: " + message );
    }

    private void showSuccess(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make( getView(), message, Snackbar.LENGTH_SHORT ).show();
        }
        Log.d( TAG, "Success: " + message );
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create new instance for creating an event
     */
    public static EditLocalEventFragment newInstanceForCreate(@Nullable LocalDate defaultDate) {
        EditLocalEventFragment fragment = new EditLocalEventFragment();
        Bundle args = new Bundle();
        if (defaultDate != null) {
            args.putString( ARG_DEFAULT_DATE, defaultDate.toString() );
        }
        fragment.setArguments( args );
        return fragment;
    }

    /**
     * Create new instance for editing an event
     */
    public static EditLocalEventFragment newInstanceForEdit(@NonNull String eventId) {
        EditLocalEventFragment fragment = new EditLocalEventFragment();
        Bundle args = new Bundle();
        args.putString( ARG_EVENT_ID, eventId );
        fragment.setArguments( args );
        return fragment;
    }
}