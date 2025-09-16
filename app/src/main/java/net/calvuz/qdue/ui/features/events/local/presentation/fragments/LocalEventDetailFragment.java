package net.calvuz.qdue.ui.features.events.local.presentation.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * LocalEventDetailFragment - Event Detail View (MVVM)
 *
 * <p>Fragment for displaying comprehensive event details with actions. Provides intuitive
 * interface for viewing all event information and accessing common operations like edit,
 * duplicate, delete, and export following MVVM architecture patterns.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Complete Event View</strong>: Shows all event properties in organized cards</li>
 *   <li><strong>Action Menu</strong>: Quick access to edit, duplicate, delete, export, share</li>
 *   <li><strong>MVVM Integration</strong>: Reactive updates through LocalEventsViewModel</li>
 *   <li><strong>Material Design 3</strong>: Modern card-based layout with proper elevation</li>
 *   <li><strong>Navigation Support</strong>: Seamless transitions to edit and other screens</li>
 *   <li><strong>Error Handling</strong>: Graceful degradation for missing or deleted events</li>
 *   <li><strong>Internationalization</strong>: Localized text using LocaleManager</li>
 * </ul>
 *
 * <h3>Navigation Arguments:</h3>
 * <ul>
 *   <li><code>eventId</code> (String, required): Event ID to display</li>
 *   <li><code>sourceDate</code> (String, optional): Date context for navigation</li>
 *   <li><code>sourceFragment</code> (String, optional): Source fragment identifier</li>
 * </ul>
 *
 * <h3>UI States:</h3>
 * <ul>
 *   <li><strong>Loading</strong>: Shows progress indicator while fetching event</li>
 *   <li><strong>Content</strong>: Displays event details in organized card layout</li>
 *   <li><strong>Error</strong>: Shows error message with retry option</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventDetailFragment extends Fragment
{

    private static final String TAG = "LocalEventDetailFragment";

    // Navigation arguments
    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_SOURCE_DATE = "sourceDate";
    private static final String ARG_SOURCE_FRAGMENT = "sourceFragment";

    // ==================== UI COMPONENTS ====================

    // Toolbar
    private Toolbar mToolbar;

    // Content Cards
    private MaterialCardView mTitleCard;
    private MaterialCardView mDateTimeCard;
    private MaterialCardView mLocationCard;
    private MaterialCardView mDescriptionCard;
    private MaterialCardView mActionsCard;

    // Content Views
    private TextView mEventTitleText;
    private Chip mEventTypeChip;
    private Chip mPriorityChip;
    private Chip mAllDayChip;
    private TextView mEventDateText;
    private TextView mEventTimeText;
    private TextView mEventLocationText;
    private TextView mEventDescriptionText;
    private View mTimeContainer;

    // Action Buttons
    private MaterialButton mEditEventButton;
    private MaterialButton mDuplicateEventButton;
    private MaterialButton mExportEventButton;
    private MaterialButton mShareEventButton;
    private MaterialButton mDeleteEventButton;
    private FloatingActionButton mFabEditEvent;

    // State Views
    private View mLoadingStateView;
    private View mErrorStateView;
    private TextView mErrorMessageText;
    private MaterialButton mRetryButton;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;

    // ==================== STATE ====================

    private String mEventId;
    private String mSourceDate;
    private String mSourceFragment;
    private LocalEvent mCurrentEvent;

    // ==================== DATE/TIME FORMATTERS ====================

    private final DateTimeFormatter mDateFormatter = DateTimeFormatter.ofLocalizedDate(
            FormatStyle.FULL );
    private final DateTimeFormatter mTimeFormatter = DateTimeFormatter.ofLocalizedTime(
            FormatStyle.SHORT );

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
        if (getActivity() instanceof LocalEventsActivity activity) {
            mEventsViewModel = activity.getEventsViewModel();
        }

        if (mEventsViewModel == null) {
            Log.e( TAG, "Could not get LocalEventsViewModel from parent Activity" );
            // Handle error - navigate back or show error
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
        return inflater.inflate( R.layout.fragment_local_event_detail, container, false );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        initializeViews( view );
        setupToolbar();
        setupClickListeners();

        // Load event details
        loadEventDetails();
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
        inflater.inflate( R.menu.menu_event_detail, menu );
        super.onCreateOptionsMenu( menu, inflater );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            handleBackNavigation();
            return true;
        } else if (itemId == R.id.action_edit_event) {
            navigateToEdit();
            return true;
        } else if (itemId == R.id.action_duplicate_event) {
            navigateToDuplicate();
            return true;
        } else if (itemId == R.id.action_share_event) {
            shareEvent();
            return true;
        } else if (itemId == R.id.action_delete_event) {
            confirmDeleteEvent();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    // ==================== INITIALIZATION ====================

    private void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            mEventId = args.getString( ARG_EVENT_ID );
            mSourceDate = args.getString( ARG_SOURCE_DATE );
            mSourceFragment = args.getString( ARG_SOURCE_FRAGMENT, "list" );
        }

        if (mEventId == null) {
            Log.e( TAG, "No event ID provided in arguments" );
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_id_missing",
                                                         "Event ID is missing" ) );
        }
    }

    private void setupViewModelListeners() {
        if (mEventsViewModel == null) return;

        // Loading state listener
        mLoadingStateListener = (operation, loading) -> {
            if (LocalEventsViewModel.OP_DELETE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_LOAD_EVENTS.equals( operation )) {
                handleLoadingState( operation, loading );
            }
        };
        mEventsViewModel.addLoadingStateListener( mLoadingStateListener );

        // Error state listener
        mErrorStateListener = (operation, error) -> {
            if (LocalEventsViewModel.OP_DELETE_EVENT.equals( operation ) ||
                    LocalEventsViewModel.OP_LOAD_EVENTS.equals( operation )) {
                handleErrorState( operation, error );
            }
        };
        mEventsViewModel.addErrorStateListener( mErrorStateListener );

        // Event listener for UI actions
        mEventListener = event -> {
            if (event instanceof BaseViewModel.UIActionEvent uiEvent) {
                handleUIActionEvent( uiEvent );
            }
        };
        mEventsViewModel.addEventListener( mEventListener );
    }

    private void initializeViews(@NonNull View view) {
        // Toolbar
        mToolbar = view.findViewById( R.id.toolbar_event_detail );

        // Content Cards
        mTitleCard = view.findViewById( R.id.title_card );
        mDateTimeCard = view.findViewById( R.id.datetime_card );
        mLocationCard = view.findViewById( R.id.location_card );
        mDescriptionCard = view.findViewById( R.id.description_card );
        mActionsCard = view.findViewById( R.id.actions_card );

        // Content Views
        mEventTitleText = view.findViewById( R.id.event_title_text );
        mEventTypeChip = view.findViewById( R.id.event_type_chip );
        mPriorityChip = view.findViewById( R.id.priority_chip );
        mAllDayChip = view.findViewById( R.id.all_day_chip );
        mEventDateText = view.findViewById( R.id.event_date_text );
        mEventTimeText = view.findViewById( R.id.event_time_text );
        mEventLocationText = view.findViewById( R.id.event_location_text );
        mEventDescriptionText = view.findViewById( R.id.event_description_text );
        mTimeContainer = view.findViewById( R.id.time_container );

        // Action Buttons
        mEditEventButton = view.findViewById( R.id.edit_event_button );
        mDuplicateEventButton = view.findViewById( R.id.duplicate_event_button );
        mExportEventButton = view.findViewById( R.id.export_event_button );
        mShareEventButton = view.findViewById( R.id.share_event_button );
        mDeleteEventButton = view.findViewById( R.id.delete_event_button );
        mFabEditEvent = view.findViewById( R.id.fab_edit_event );

        // State Views
        mLoadingStateView = view.findViewById( R.id.loading_state_view );
        mErrorStateView = view.findViewById( R.id.error_state_view );
        mErrorMessageText = view.findViewById( R.id.error_message_text );
        mRetryButton = view.findViewById( R.id.retry_button );
    }

    private void setupToolbar() {
        if (mToolbar != null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                activity.setSupportActionBar( mToolbar );
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled( true );
                    activity.getSupportActionBar().setTitle( LocaleManager.getLocalizedString(
                            getContext(), "local_event_detail_title", "Event Details" ) );
                }
            }
        }
    }

    private void setupClickListeners() {
        // Action Buttons
        mEditEventButton.setOnClickListener( v -> navigateToEdit() );
        mDuplicateEventButton.setOnClickListener( v -> navigateToDuplicate() );
        mExportEventButton.setOnClickListener( v -> navigateToExport() );
        mShareEventButton.setOnClickListener( v -> shareEvent() );
        mDeleteEventButton.setOnClickListener( v -> confirmDeleteEvent() );
        mFabEditEvent.setOnClickListener( v -> navigateToEdit() );

        // Error State
        mRetryButton.setOnClickListener( v -> loadEventDetails() );
    }

    // ==================== EVENT LOADING ====================

    private void loadEventDetails() {
        if (mEventId == null || mEventsViewModel == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_invalid_event",
                                                         "Invalid event" ) );
            return;
        }

        // Show loading state
        setUIState( UIState.LOADING );

        // Find event in current events list
        List<LocalEvent> allEvents = mEventsViewModel.getAllEvents();
        for (LocalEvent event : allEvents) {
            if (mEventId.equals( event.getId() )) {
                mCurrentEvent = event;
                populateEventDetails( event );
                setUIState( UIState.CONTENT );
                return;
            }
        }

        // Event not found
        showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_found",
                                                     "Event not found or has been deleted" ) );
    }

    private void populateEventDetails(@NonNull LocalEvent event) {
        // Title and basic info
        mEventTitleText.setText( event.getTitle() );

        // Event type chip
        setupEventTypeChip( event.getEventType() );

        // Priority chip
        setupPriorityChip( event.getPriority() );

        // All-day indicator
        mAllDayChip.setVisibility( event.isAllDay() ? View.VISIBLE : View.GONE );

        // Date and time
        setupDateTimeDisplay( event );

        // Location
        setupLocationDisplay( event );

        // Description
        setupDescriptionDisplay( event );

        Log.d( TAG, "Event details populated for: " + event.getTitle() );
    }

    private void setupEventTypeChip(@Nullable EventType eventType) {
        if (eventType != null) {
            String typeText = getEventTypeDisplayName( eventType );
            mEventTypeChip.setText( typeText );
            mEventTypeChip.setVisibility( View.VISIBLE );
        } else {
            mEventTypeChip.setVisibility( View.GONE );
        }
    }

    private void setupPriorityChip(@Nullable Priority priority) {
        if (priority != null && priority != Priority.NORMAL) {
            String priorityText = getPriorityDisplayName( priority );
            mPriorityChip.setText( priorityText );
            mPriorityChip.setVisibility( View.VISIBLE );

            // Set appropriate color for priority
            if (priority == Priority.OVERRIDE) {
                String color = Priority.OVERRIDE.getColorHex();
                mPriorityChip.setBackgroundColor( Color.parseColor(
                        color ) ); // .setChipBackgroundColor( Color.parseColor(color));
            } else if (priority == Priority.HIGH) {
                String color = Priority.HIGH.getColorHex();
                mPriorityChip.setBackgroundColor( Color.parseColor(
                        color ) ); // .setChipBackgroundColor( Color.parseColor(color));
            } else if (priority == Priority.LOW) {
                String color = Priority.LOW.getColorHex();
                mPriorityChip.setBackgroundColor( Color.parseColor(
                        color ) ); // .setChipBackgroundColorResource(R.color.priority_low_background);
            }
        } else {
            mPriorityChip.setVisibility( View.GONE );
        }
    }

    private void setupDateTimeDisplay(@NonNull LocalEvent event) {
        if (event.getStartTime() != null) {
            // Date
            mEventDateText.setText( event.getStartTime().format( mDateFormatter ) );

            // Time (hidden for all-day events)
            if (event.isAllDay()) {
                mTimeContainer.setVisibility( View.GONE );
            } else {
                mTimeContainer.setVisibility( View.VISIBLE );
                String timeText = formatEventTime( event.getStartTime(), event.getEndTime() );
                mEventTimeText.setText( timeText );
            }
        }
    }

    private void setupLocationDisplay(@NonNull LocalEvent event) {
        String location = event.getLocation();
        if (location != null && !location.trim().isEmpty()) {
            mEventLocationText.setText( location );
            mLocationCard.setVisibility( View.VISIBLE );
        } else {
            mLocationCard.setVisibility( View.GONE );
        }
    }

    private void setupDescriptionDisplay(@NonNull LocalEvent event) {
        String description = event.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            mEventDescriptionText.setText( description );
            mDescriptionCard.setVisibility( View.VISIBLE );
        } else {
            mDescriptionCard.setVisibility( View.GONE );
        }
    }

    // ==================== NAVIGATION ACTIONS ====================

    private void navigateToEdit() {
        if (mCurrentEvent == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_loaded",
                                                         "Event not loaded" ) );
            return;
        }

        NavController navController = Navigation.findNavController( requireView() );
        Bundle args = new Bundle();
        args.putString( "eventId", mCurrentEvent.getId() );
        args.putString( "sourceDate", mSourceDate );
        args.putString( "sourceFragment", "detail" );
        args.putString( "editMode", "existing_event" );

        navController.navigate( R.id.action_event_detail_to_edit_event, args );
    }

    private void navigateToDuplicate() {
        if (mCurrentEvent == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_loaded",
                                                         "Event not loaded" ) );
            return;
        }

        NavController navController = Navigation.findNavController( requireView() );
        Bundle args = new Bundle();
        args.putString( "duplicateFromEventId", mCurrentEvent.getId() );

        navController.navigate( R.id.action_event_detail_to_duplicate, args );
    }

    private void navigateToExport() {
        if (mCurrentEvent == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_loaded",
                                                         "Event not loaded" ) );
            return;
        }

        NavController navController = Navigation.findNavController( requireView() );
        Bundle args = new Bundle();
        args.putString( "exportType", "single" );
        args.putString( "eventId", mCurrentEvent.getId() );

        navController.navigate( R.id.action_event_detail_to_export, args );
    }

    private void handleBackNavigation() {
        NavController navController = Navigation.findNavController( requireView() );
        if (!navController.navigateUp()) {
            // Fallback navigation to events list
            navController.navigate( R.id.action_event_detail_to_events_list );
        }
    }

    // ==================== ACTION HANDLERS ====================

    private void shareEvent() {
        if (mCurrentEvent == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_loaded",
                                                         "Event not loaded" ) );
            return;
        }

        String shareText = buildShareText( mCurrentEvent );
        Intent shareIntent = new Intent( Intent.ACTION_SEND );
        shareIntent.setType( "text/plain" );
        shareIntent.putExtra( Intent.EXTRA_TEXT, shareText );
        shareIntent.putExtra( Intent.EXTRA_SUBJECT, mCurrentEvent.getTitle() );

        Intent chooser = Intent.createChooser( shareIntent,
                                               LocaleManager.getLocalizedString( getContext(),
                                                                                 "share_event",
                                                                                 "Share Event" ) );
        startActivity( chooser );
    }

    private void confirmDeleteEvent() {
        if (mCurrentEvent == null) {
            showError( LocaleManager.getLocalizedString( getContext(), "error_event_not_loaded",
                                                         "Event not loaded" ) );
            return;
        }

        new AlertDialog.Builder( requireContext() )
                .setTitle( LocaleManager.getLocalizedString( getContext(), "confirm_delete_title",
                                                             "Delete Event" ) )
                .setMessage(
                        LocaleManager.getLocalizedString( getContext(), "confirm_delete_message",
                                                          "Are you sure you want to delete this event? This action cannot be undone." ) )
                .setPositiveButton( LocaleManager.getLocalizedString( getContext(), "action_delete",
                                                                      "Delete" ),
                                    (dialog, which) -> deleteEvent() )
                .setNegativeButton( LocaleManager.getLocalizedString( getContext(), "action_cancel",
                                                                      "Cancel" ), null )
                .show();
    }

    private void deleteEvent() {
        if (mCurrentEvent != null && mEventsViewModel != null) {
            mEventsViewModel.deleteEvent( mCurrentEvent.getId() );
        }
    }

    // ==================== EVENT HANDLERS ====================

    private void handleLoadingState(@NonNull String operation, boolean loading) {
        // Handle loading for delete operation
        if (LocalEventsViewModel.OP_DELETE_EVENT.equals( operation )) {
            // Disable action buttons during delete
            setActionButtonsEnabled( !loading );
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
                // Navigate back on successful delete
                if (message.contains( "deleted" )) {
                    handleBackNavigation();
                }
            }
        } else if ("SHOW_ERROR".equals( action )) {
            String message = event.getData( "message", String.class );
            if (message != null) {
                showError( message );
            }
        }
    }

    // ==================== UI STATE MANAGEMENT ====================

    private enum UIState
    {
        LOADING, CONTENT, ERROR
    }

    private void setUIState(@NonNull UIState state) {
        mLoadingStateView.setVisibility( state == UIState.LOADING ? View.VISIBLE : View.GONE );
        mErrorStateView.setVisibility( state == UIState.ERROR ? View.VISIBLE : View.GONE );

        // Content visibility
        boolean showContent = state == UIState.CONTENT;
        mTitleCard.setVisibility( showContent ? View.VISIBLE : View.GONE );
        mDateTimeCard.setVisibility( showContent ? View.VISIBLE : View.GONE );
        mActionsCard.setVisibility( showContent ? View.VISIBLE : View.GONE );
        mFabEditEvent.setVisibility( showContent ? View.VISIBLE : View.GONE );
    }

    private void setActionButtonsEnabled(boolean enabled) {
        mEditEventButton.setEnabled( enabled );
        mDuplicateEventButton.setEnabled( enabled );
        mExportEventButton.setEnabled( enabled );
        mShareEventButton.setEnabled( enabled );
        mDeleteEventButton.setEnabled( enabled );
        mFabEditEvent.setEnabled( enabled );
    }

    // ==================== UTILITY METHODS ====================

    private String getEventTypeDisplayName(@NonNull EventType eventType) {
        switch (eventType) {
            case MEETING:
                return LocaleManager.getLocalizedString( getContext(), "event_type_meeting",
                                                         "Reminder" );
            case GENERAL:
            default:
                return LocaleManager.getLocalizedString( getContext(), "event_type_general",
                                                         "General" );
        }
    }

    private String getPriorityDisplayName(@NonNull Priority priority) {
        switch (priority) {
            case HIGH:
                return LocaleManager.getLocalizedString( getContext(), "priority_high",
                                                         "High Priority" );
            case LOW:
                return LocaleManager.getLocalizedString( getContext(), "priority_low",
                                                         "Low Priority" );
            case NORMAL:
            default:
                return LocaleManager.getLocalizedString( getContext(), "priority_normal",
                                                         "Normal" );
        }
    }

    private String formatEventTime(@Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) {
        if (startTime == null) {
            return "";
        }

        String formattedStartTime = startTime.format( mTimeFormatter );

        if (endTime != null) {
            String formattedEndTime = endTime.format( mTimeFormatter );
            return formattedStartTime + " - " + formattedEndTime;
        }

        return formattedStartTime;
    }

    private String buildShareText(@NonNull LocalEvent event) {
        StringBuilder shareText = new StringBuilder();

        shareText.append( event.getTitle() ).append( "\n\n" );

        if (event.getStartTime() != null) {
            shareText.append( LocaleManager.getLocalizedString( getContext(), "date", "Date" ) )
                    .append( ": " ).append( event.getStartTime().format( mDateFormatter ) ).append(
                            "\n" );

            if (!event.isAllDay()) {
                shareText.append( LocaleManager.getLocalizedString( getContext(), "time", "Time" ) )
                        .append( ": " ).append(
                                formatEventTime( event.getStartTime(), event.getEndTime() ) ).append(
                                "\n" );
            }
        }

        if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
            shareText.append(
                            LocaleManager.getLocalizedString( getContext(), "location", "Location" ) )
                    .append( ": " ).append( event.getLocation() ).append( "\n" );
        }

        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            shareText.append( "\n" ).append( event.getDescription() );
        }

        return shareText.toString();
    }

    private void showError(@NonNull String message) {
        mErrorMessageText.setText( message );
        setUIState( UIState.ERROR );

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
     * Create new instance for displaying event details
     */
    public static LocalEventDetailFragment newInstance(
            @NonNull String eventId,
            @Nullable String sourceDate,
            @Nullable String sourceFragment
    ) {
        LocalEventDetailFragment fragment = new LocalEventDetailFragment();
        Bundle args = new Bundle();
        args.putString( ARG_EVENT_ID, eventId );
        if (sourceDate != null) {
            args.putString( ARG_SOURCE_DATE, sourceDate );
        }
        if (sourceFragment != null) {
            args.putString( ARG_SOURCE_FRAGMENT, sourceFragment );
        }
        fragment.setArguments( args );
        return fragment;
    }
}