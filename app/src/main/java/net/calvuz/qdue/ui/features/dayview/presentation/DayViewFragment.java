package net.calvuz.qdue.ui.features.dayview.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.features.dayview.di.DayViewModule;
import net.calvuz.qdue.ui.features.dayview.components.DayViewStateManager;
import net.calvuz.qdue.ui.features.dayview.components.DayViewEventOperations;
import net.calvuz.qdue.ui.features.dayview.adapters.DayViewEventsAdapter;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.swipecalendar.di.CalendarSharedViewModelModule;
import net.calvuz.qdue.ui.features.swipecalendar.viewmodels.CalendarSharedViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * DayViewFragment - Daily Calendar View with Multi-Event Support
 *
 * <p>Provides comprehensive daily calendar view with support for multiple event types
 * (LocalEvent, WorkScheduleDay) and extensible architecture for future event types.
 * Features complete CRUD operations, bulk selection, sharing, and interactive capabilities.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-Event Display</strong>: LocalEvent and WorkScheduleDay with unified interface</li>
 *   <li><strong>Interactive Operations</strong>: Create, edit, delete, share, copy to clipboard</li>
 *   <li><strong>Bulk Selection</strong>: Multi-select mode for batch operations</li>
 *   <li><strong>Pull-to-Refresh</strong>: Manual refresh capability</li>
 *   <li><strong>Empty States</strong>: Elegant handling of days with no events</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error state management</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <ul>
 *   <li><strong>Clean Architecture</strong>: Full dependency injection compliance</li>
 *   <li><strong>State Management</strong>: Reactive state updates through DayViewStateManager</li>
 *   <li><strong>Bundle Navigation</strong>: Receives target date via Bundle arguments</li>
 *   <li><strong>Event Operations</strong>: Comprehensive CRUD through DayViewEventOperations</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * // Create fragment for specific date:
 * DayViewFragment fragment = DayViewFragment.newInstance(LocalDate.now(), userId);
 *
 * // Navigation from month view:
 * Bundle args = new Bundle();
 * args.putString(DayViewFragment.ARG_TARGET_DATE, selectedDate.toString());
 * fragment.setArguments(args);
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewFragment
        extends Fragment
        implements Injectable,
        DayViewStateManager.DayViewStateListener
{

    private static final String TAG = "DayViewFragment";

    // ==================== FRAGMENT ARGUMENTS ====================

    /**
     * Bundle argument keys for fragment configuration
     */
    public static final String ARG_TARGET_DATE = "target_date";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_ALLOW_CREATION = "allow_creation";
    public static final String ARG_SELECTION_MODE = "selection_mode";

    // ==================== DEPENDENCIES ====================

    private ServiceProvider mServiceProvider;
    private CalendarServiceProvider mCalendarServiceProvider;
    private DayViewModule mDayViewModule;
    private LocalEventsService mLocalEventsService;
    private QDueUserService mQDueUserService;
    private LocaleManager mLocaleManager;

    // ==================== MVVM INTEGRATION ====================

    private CalendarSharedViewModel mSharedViewModel;
    private CalendarSharedViewModelModule mSharedViewModelModule;
    private boolean mIsObservingSharedViewModel = false;

    // ==================== FEATURE COMPONENTS ====================

    private DayViewStateManager mStateManager;
    private DayViewEventOperations mEventOperations;
    private DayViewEventsAdapter mEventsAdapter;

    // ==================== UI COMPONENTS ====================

    // Main layout
    private View mRootView;
    private MaterialCardView mDayViewCard;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Header
    private MaterialToolbar mToolbar;
    private TextView mDateHeaderText;
    private TextView mEventCountText;
    private MaterialButton mPreviousDayButton;
    private MaterialButton mNextDayButton;
    private MaterialButton mTodayButton;

    // Content
    private RecyclerView mEventsRecyclerView;
    private View mEmptyStateView;
    private TextView mEmptyStateText;
    private View mErrorStateView;
    private TextView mErrorStateText;
    private MaterialButton mRetryButton;

    // Action components
    private FloatingActionButton mFabCreateEvent;
    private View mSelectionToolbar;
    private TextView mSelectionCountText;
    private MaterialButton mSelectAllButton;
    private MaterialButton mClearSelectionButton;
    private MaterialButton mDeleteSelectedButton;
    private MaterialButton mShareSelectedButton;

    // ==================== STATE ====================

    private QDueUser mQDueUser;
    private LocalDate mTargetDate;
    private String mUserId;
    private boolean mAllowCreation = true;
    private boolean mStartInSelectionMode = false;
    private boolean mIsInitialized = false;
    private Handler mMainHandler;

    // ==================== FRAGMENT LIFECYCLE ====================

    /**
     * Create new DayViewFragment instance with specified parameters.
     *
     * @param targetDate    Date to display
     * @param userId        User ID for data filtering (null for default user)
     * @param allowCreation Allow event creation via FAB
     * @param selectionMode Start in selection mode
     * @return New fragment instance
     */
    @NonNull
    public static DayViewFragment newInstance(
            @NonNull LocalDate targetDate,
            @Nullable String userId,
            @NonNull QDueUser qDueUser,
            boolean allowCreation,
            boolean selectionMode
    ) {
        DayViewFragment fragment = new DayViewFragment();
        Bundle args = new Bundle();

        args.putString( ARG_TARGET_DATE, targetDate.toString() );
        if (userId != null) {
            args.putString( ARG_USER_ID, userId );
        }
        args.putBoolean( ARG_ALLOW_CREATION, allowCreation );
        args.putBoolean( ARG_SELECTION_MODE, selectionMode );

        fragment.setArguments( args );
        fragment.setQDueUser( qDueUser );
        return fragment;
    }

    private void setQDueUser(QDueUser qDueUser) {
        this.mQDueUser = qDueUser;
    }

    /**
     * Create new DayViewFragment instance with basic parameters.
     *
     * @param targetDate Date to display
     * @param userId     User ID for data filtering (null for default user)
     * @return New fragment instance
     */
    @NonNull
    public static DayViewFragment newInstance(
            @NonNull LocalDate targetDate,
            @Nullable String userId,
            @NonNull QDueUser qDueUser
    ) {
        return newInstance( targetDate, userId, qDueUser, true, false );
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize handlers
        mMainHandler = new Handler( Looper.getMainLooper() );

        // Parse arguments
        parseArguments();

        // Menu
        setHasOptionsMenu( true );

        Log.d( TAG, "DayViewFragment created for date: " + mTargetDate );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate( R.layout.fragment_day_view, container, false );
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        // Initialize dependency injection
        initializeDependencyInjection();

        // Initialize UI components
        initializeViews( view );

        // Setup SharedViewModel integration
        setupSharedViewModelIntegration();

        // Initialize feature components
        initializeFeatureComponents();

        // Setup day view components
        setupDayView();

        Log.d( TAG,
               "DayViewFragment view created and configured with SharedViewModel integration" );
    }

    @Override
    public void onResume() {
        super.onResume();

        // ✅ FIXED: Complete sync with SharedViewModel on resume
        if (mSharedViewModel != null) {
            LocalDate sharedDate = mSharedViewModel.getSelectedDate().getValue();
            if (sharedDate != null && (mTargetDate == null || !mTargetDate.equals( sharedDate ))) {
                Log.d( TAG,
                       "Syncing with SharedViewModel on resume: " + sharedDate + " (was: " + mTargetDate + ")" );

                // ✅ CRITICAL: Reconfigure entire module with new date
                reconfigureForNewDate( sharedDate );
            }

            // Set view mode to DAY
            mSharedViewModel.setViewMode( CalendarSharedViewModel.ViewMode.DAY );
        }

        if (mIsInitialized) {
            // Refresh data when returning to fragment
            refreshEvents();
        }

        Log.d( TAG, "DayViewFragment resumed with SharedViewModel sync" );
    }

    @Override
    public void onPause() {
        super.onPause();

        // Set view mode back to MONTH when leaving DayView
        if (mSharedViewModel != null) {
            mSharedViewModel.setViewMode( CalendarSharedViewModel.ViewMode.MONTH );
        }

        Log.d( TAG, "DayViewFragment paused, view mode reset to MONTH" );
    }

    @Override
    public void onDestroyView() {
        // Clean up state listener
        if (mStateManager != null) {
            mStateManager.removeStateListener( this );
        }

        // Clean up module
        if (mDayViewModule != null) {
            mDayViewModule.destroy();
            mDayViewModule = null;
        }

        super.onDestroyView();
        Log.d( TAG, "DayViewFragment view destroyed" );
    }

    // ==================== SHARED VIEW MODEL INTEGRATION ====================

    /**
     * Setup SharedViewModel integration with bidirectional synchronization.
     */
    private void setupSharedViewModelIntegration() {
        if (mSharedViewModel != null && !mIsObservingSharedViewModel) {
            // ✅ CRITICAL: Observe date changes from MonthView
            mSharedViewModel.getSelectedDate().observe( getViewLifecycleOwner(),
                                                        this::onSharedDateChanged );

            // Set view mode to DAY when DayView is active
            mSharedViewModel.setViewMode( CalendarSharedViewModel.ViewMode.DAY );

            // Sync with initial SharedViewModel date if different
            LocalDate sharedDate = mSharedViewModel.getSelectedDate().getValue();
            if (sharedDate != null && !sharedDate.equals( mTargetDate )) {
                Log.d( TAG, "Initial sync with SharedViewModel: " + sharedDate );
                reconfigureForNewDate( sharedDate );
            }

            mIsObservingSharedViewModel = true;
            Log.d( TAG, "SharedViewModel observers configured" );
        }
    }

    /**
     * ✅ FIXED: Handle shared date changes with complete module reconfiguration.
     */
    private void onSharedDateChanged(@NonNull LocalDate newDate) {
        if (mTargetDate == null || !mTargetDate.equals( newDate )) {
            Log.d( TAG, "SharedViewModel date changed: " + mTargetDate + " → " + newDate );

            // ✅ CRITICAL: Reconfigure entire module infrastructure
            reconfigureForNewDate( newDate );
        }
    }

    /**
     * ✅ NEW: Complete module reconfiguration for new date.
     * This is the key method that fixes the synchronization issue.
     */
    private void reconfigureForNewDate(@NonNull LocalDate newDate) {
        Log.d( TAG, "Reconfiguring DayViewModule for new date: " + newDate );

        // Update target date
        mTargetDate = newDate;

        // ✅ STEP 1: Reconfigure DayViewModule with new date
        if (mDayViewModule != null) {
            mDayViewModule.configure( newDate, mQDueUser );
            Log.d( TAG, "DayViewModule reconfigured for date: " + newDate );
        }

        // ✅ STEP 2: Update StateManager with new date
        if (mStateManager != null) {
            mStateManager.setCurrentDate( newDate );
            Log.d( TAG, "DayViewStateManager updated for date: " + newDate );
        }

        // ✅ STEP 3: Update UI
        updateDateHeader( newDate );

        // ✅ STEP 4: Reload data with correct date
        loadDayEvents();

        Log.d( TAG, "Complete reconfiguration completed for date: " + newDate );
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(ServiceProvider serviceProvider) {
        mServiceProvider = serviceProvider;

        // Get calendar service provider
        mCalendarServiceProvider = serviceProvider.getCalendarServiceProvider();

        // Get core services
        mLocaleManager = serviceProvider.getLocaleManager();
        mLocalEventsService = mCalendarServiceProvider.getLocalEventsService();
        mQDueUserService = mCalendarServiceProvider.getQDueUserService();

        // ✅ FIXED: Initialize SharedViewModel integration during injection
        initializeSharedViewModel();

        Log.d( TAG, "Dependencies injected successfully" );
    }

    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null &&
                mCalendarServiceProvider != null &&
                mLocalEventsService != null &&
                mQDueUserService != null &&
                mLocaleManager != null;
    }

    /**
     * ✅ NEW: Initialize SharedViewModel during dependency injection.
     */
    private void initializeSharedViewModel() {
        try {
            // Initialize SharedViewModel module
            mSharedViewModelModule = new CalendarSharedViewModelModule( requireContext(),
                                                                        mCalendarServiceProvider );

            // Get SharedViewModel from Activity scope
            ViewModelProvider.Factory factory = mSharedViewModelModule.getViewModelFactory();
            mSharedViewModel = new ViewModelProvider( requireActivity(), factory ).get(
                    CalendarSharedViewModel.class );

            Log.d( TAG, "SharedViewModel initialized successfully" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to initialize SharedViewModel", e );
            throw new RuntimeException( "Failed to initialize SharedViewModel", e );
        }
    }

    // ==================== STATE LISTENER IMPLEMENTATION ====================

    @Override
    public void onDateChanged(@NonNull LocalDate newDate) {
        mMainHandler.post( () -> {
            mTargetDate = newDate;

            // ✅ FIXED: Notify SharedViewModel for bidirectional sync
            if (mSharedViewModel != null) {
                mSharedViewModel.navigateToDate( newDate );
            }

            // Update UI (existing code)
            updateDateHeader( newDate );
//        updateNavigationButtons();

            Log.d( TAG, "Date changed in DayView and SharedViewModel notified: " + newDate );
        } );
    }

    @Override
    public void onEventsUpdated(
            @NonNull List<LocalEvent> localEvents,
            @NonNull List<WorkScheduleDay> workScheduleDays
    ) {
        mMainHandler.post( () -> {
            mEventsAdapter.updateEvents( localEvents, workScheduleDays );
            updateEventCount( localEvents.size() + workScheduleDays.size() );
            Log.d( TAG,
                   "Events updated: " + localEvents.size() + " local, " + workScheduleDays.size() + " work schedule" );
        } );
    }

    @Override
    public void onLoadingStateChanged(boolean isLoading) {
        mMainHandler.post( () -> {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing( isLoading );
            }
            Log.d( TAG, "Loading state changed: " + isLoading );
        } );
    }

    @Override
    public void onErrorStateChanged(@Nullable String errorMessage) {
        mMainHandler.post( () -> {
            showErrorState( errorMessage );
            Log.d( TAG, "Error state changed: " + errorMessage );
        } );
    }

    @Override
    public void onSelectionChanged(@NonNull Set<String> selectedEventIds, boolean isSelectionMode) {
        mMainHandler.post( () -> {
            updateSelectionUI( selectedEventIds.size(), isSelectionMode );
            Log.d( TAG,
                   "Selection changed: " + selectedEventIds.size() + " selected, mode: " + isSelectionMode );
        } );
    }

    @Override
    public void onEmptyStateChanged(boolean isEmpty) {
        mMainHandler.post( () -> {
            showEmptyState( isEmpty );
            Log.d( TAG, "Empty state changed: " + isEmpty );
        } );
    }

    // ==================== MENU HANDLING ====================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate( R.menu.menu_day_view, menu );
        super.onCreateOptionsMenu( menu, inflater );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            refreshEvents();
            return true;
        } else if (itemId == R.id.action_go_to_today) {
            navigateToToday();
            return true;
        } else if (itemId == R.id.action_export_day) {
            exportDayEvents();
            return true;
        } else if (itemId == R.id.action_selection_mode) {
            toggleSelectionMode();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    // ==================== PRIVATE INITIALIZATION METHODS ====================

    private void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            // Parse target date (required)
            String dateString = args.getString( ARG_TARGET_DATE );
            if (dateString != null) {
                mTargetDate = LocalDate.parse( dateString );
            } else {
                mTargetDate = LocalDate.now();
                Log.w( TAG, "No target date provided, using current date" );
            }

            // Parse optional parameters
            mUserId = args.getString( ARG_USER_ID );
            mAllowCreation = args.getBoolean( ARG_ALLOW_CREATION, true );
            mStartInSelectionMode = args.getBoolean( ARG_SELECTION_MODE, false );
        } else {
            mTargetDate = LocalDate.now();
            Log.w( TAG, "No arguments provided, using default values" );
        }
    }

    private void initializeDependencyInjection() {
        // Inject dependencies if not already done
        if (!areDependenciesReady()) {
            DependencyInjector.inject( this, getContext() );
        }

        if (!areDependenciesReady()) {
            throw new IllegalStateException( "Failed to inject dependencies for DayViewFragment" );
        }

//        // Initialize SharedViewModel module
//        mSharedViewModelModule = new CalendarSharedViewModelModule( requireContext(),
//                                                                    mCalendarServiceProvider );
//
//        // Get SharedViewModel from Activity scope
//        ViewModelProvider.Factory factory = mSharedViewModelModule.getViewModelFactory();
//        mSharedViewModel = new ViewModelProvider( requireActivity(), factory ).get(
//                CalendarSharedViewModel.class );

        Log.d( TAG, "SharedViewModel integration initialized" );
    }

//    // In onViewCreated() - Setup observers
//    private void setupSharedViewModelObservers() {
//        // Observe selected date changes from MonthView
//        mSharedViewModel.getSelectedDate().observe( getViewLifecycleOwner(),
//                                                    this::onSharedDateChanged );
//
//        // Set view mode to DAY
//        mSharedViewModel.setViewMode( CalendarSharedViewModel.ViewMode.DAY );
//    }

    private void initializeFeatureComponents() {
        try {

            // Create and configure day view module
            mDayViewModule = new DayViewModule( getContext(), mCalendarServiceProvider );
            mDayViewModule.configure( mTargetDate, null ); // User will be set later if needed

            // Get feature components
            mStateManager = mDayViewModule.provideStateManager();
            mEventOperations = mDayViewModule.provideEventOperations();
            mEventsAdapter = mDayViewModule.provideEventsAdapter( getContext() );

            // Add state listener
            mStateManager.addStateListener( this );

            // Set initial selection mode if requested
            if (mStartInSelectionMode) {
                mStateManager.setSelectionMode( true );
            }

            mIsInitialized = true;
            Log.d( TAG, "Feature components initialized successfully" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to initialize feature components", e );
            throw new RuntimeException( "Failed to initialize DayViewFragment components", e );
        }
    }

    private void initializeViews(@NonNull View rootView) {
        // Main layout
        mDayViewCard = rootView.findViewById( R.id.day_view_card );
        mSwipeRefreshLayout = rootView.findViewById( R.id.swipe_refresh_layout );

        // Header
        mToolbar = rootView.findViewById( R.id.day_view_toolbar );
        mDateHeaderText = rootView.findViewById( R.id.date_header_text );
        mEventCountText = rootView.findViewById( R.id.event_count_text );
        mPreviousDayButton = rootView.findViewById( R.id.previous_day_button );
        mNextDayButton = rootView.findViewById( R.id.next_day_button );
        mTodayButton = rootView.findViewById( R.id.today_button );

        // Content
        mEventsRecyclerView = rootView.findViewById( R.id.events_recycler_view );
        mEmptyStateView = rootView.findViewById( R.id.empty_state_view );
        mEmptyStateText = rootView.findViewById( R.id.empty_state_text );
        mErrorStateView = rootView.findViewById( R.id.error_state_view );
        mErrorStateText = rootView.findViewById( R.id.error_state_text );
        mRetryButton = rootView.findViewById( R.id.retry_button );

        // Action components
        mFabCreateEvent = rootView.findViewById( R.id.fab_create_event );
        mSelectionToolbar = rootView.findViewById( R.id.selection_toolbar );
        mSelectionCountText = rootView.findViewById( R.id.selection_count_text );
        mSelectAllButton = rootView.findViewById( R.id.select_all_button );
        mClearSelectionButton = rootView.findViewById( R.id.clear_selection_button );
        mDeleteSelectedButton = rootView.findViewById( R.id.delete_selected_button );
        mShareSelectedButton = rootView.findViewById( R.id.share_selected_button );

        Log.d( TAG, "Views initialized" );
    }

    private void setupDayView() {
        // Setup RecyclerView
        mEventsRecyclerView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        mEventsRecyclerView.setAdapter( mEventsAdapter );

        // Setup pull-to-refresh
        mSwipeRefreshLayout.setOnRefreshListener( this::refreshEvents );

        // Setup date navigation
        mPreviousDayButton.setOnClickListener( v -> navigateToPreviousDay() );
        mNextDayButton.setOnClickListener( v -> navigateToNextDay() );
        mTodayButton.setOnClickListener( v -> navigateToToday() );

        // Setup FAB
        if (mAllowCreation) {
            mFabCreateEvent.setOnClickListener( v -> createNewEvent() );
            mFabCreateEvent.setVisibility( View.VISIBLE );
        } else {
            mFabCreateEvent.setVisibility( View.GONE );
        }

        // Setup selection toolbar
        mSelectAllButton.setOnClickListener( v -> mStateManager.selectAllEvents() );
        mClearSelectionButton.setOnClickListener( v -> mStateManager.clearSelection() );
        mDeleteSelectedButton.setOnClickListener( v -> deleteSelectedEvents() );
        mShareSelectedButton.setOnClickListener( v -> shareSelectedEvents() );

        // Setup error retry
        mRetryButton.setOnClickListener( v -> refreshEvents() );

        // Update initial UI
        updateDateHeader( mTargetDate );

        // Load initial data
        loadDayEvents();

        Log.d( TAG, "Day view setup complete" );
    }

    // ==================== NAVIGATION METHODS (FIXED) ====================

    /**
     * ✅ FIXED: Navigate to previous day via SharedViewModel.
     */
    private void navigateToPreviousDay() {
        if (mTargetDate != null && mSharedViewModel != null) {
            LocalDate previousDay = mTargetDate.minusDays( 1 );
            mSharedViewModel.navigateToDate( previousDay );
            Log.d( TAG, "Navigate to previous day via SharedViewModel: " + previousDay );
        }
    }

    /**
     * ✅ FIXED: Navigate to next day via SharedViewModel.
     */
    private void navigateToNextDay() {
        if (mTargetDate != null && mSharedViewModel != null) {
            LocalDate nextDay = mTargetDate.plusDays( 1 );
            mSharedViewModel.navigateToDate( nextDay );
            Log.d( TAG, "Navigate to next day via SharedViewModel: " + nextDay );
        }
    }

    /**
     * ✅ FIXED: Navigate to today via SharedViewModel.
     */
    private void navigateToToday() {
        if (mSharedViewModel != null) {
            LocalDate today = LocalDate.now();
            mSharedViewModel.navigateToDate( today );
            Log.d( TAG, "Navigate to today via SharedViewModel: " + today );
        }
    }

    // ==================== PRIVATE ACTION METHODS ====================

    private void loadDayEvents() {
        if (mDayViewModule != null) {
            mDayViewModule.loadDayEvents()
                    .exceptionally( throwable -> {
                        Log.e( TAG, "Failed to load day events", throwable );
                        showErrorSnackbar( "Failed to load events: " + throwable.getMessage() );
                        return null;
                    } );
        }
    }

    private void refreshEvents() {
        if (mDayViewModule != null) {
            mDayViewModule.refreshEvents()
                    .exceptionally( throwable -> {
                        Log.e( TAG, "Failed to refresh events", throwable );
                        showErrorSnackbar( "Failed to refresh events: " + throwable.getMessage() );
                        return null;
                    } );
        }
    }

//    private void navigateToPreviousDay() {
//        if (mTargetDate != null) {
//            LocalDate previousDay = mTargetDate.minusDays(1);
//
//            // Update through SharedViewModel instead of direct state change
//            if (mSharedViewModel != null) {
//                mSharedViewModel.navigateToDate(previousDay);
//            } else {
//                // Fallback to direct update
//                onDateChanged(previousDay);
//            }
//        }
////        LocalDate previousDay = mTargetDate.minusDays( 1 );
////        navigateToDate( previousDay );
//    }
//
//    private void navigateToNextDay() {
//        if (mTargetDate != null) {
//            LocalDate nextDay = mTargetDate.plusDays(1);
//
//            // Update through SharedViewModel instead of direct state change
//            if (mSharedViewModel != null) {
//                mSharedViewModel.navigateToDate(nextDay);
//            } else {
//                // Fallback to direct update
//                onDateChanged(nextDay);
//            }
//        }
////        LocalDate nextDay = mTargetDate.plusDays( 1 );
////        navigateToDate( nextDay );
//    }
//
//    private void navigateToToday() {
//        LocalDate today = LocalDate.now();
//
//        // Update through SharedViewModel
//        if (mSharedViewModel != null) {
//            mSharedViewModel.navigateToDate(today);
//        } else {
//            // Fallback to direct update
//            onDateChanged(today);
//        }

    /// /        navigateToDate( LocalDate.now() );
//    }

//    private void navigateToDate(@NonNull LocalDate newDate) {
//        if (!newDate.equals( mTargetDate )) {
//            mTargetDate = newDate;
//            mDayViewModule.configure( newDate, null );
//            mStateManager.setCurrentDate( newDate );
//            loadDayEvents();
//        }
//    }
    private void createNewEvent() {
        // Launch LocalEventsActivity for event creation with pre-selected date
        Intent intent = new Intent( getContext(), LocalEventsActivity.class );
        intent.putExtra( LocalEventsActivity.EXTRA_FILTER_DATE, mTargetDate.toString() );
        startActivity( intent );
    }

    private void toggleSelectionMode() {
        boolean newMode = !mStateManager.isSelectionMode();
        mStateManager.setSelectionMode( newMode );
    }

    private void deleteSelectedEvents() {
        Set<String> selectedIds = mStateManager.getSelectedEventIds();
        if (selectedIds.isEmpty()) return;

        mEventOperations.deleteSelectedEvents( selectedIds )
                .thenRun( () -> mMainHandler.post( () -> {
                    mStateManager.clearSelection();
                    showSuccessSnackbar( "Events deleted successfully" );
                    refreshEvents();
                } ) )
                .exceptionally( throwable -> {
                    mMainHandler.post( () -> {
                        Log.e( TAG, "Failed to delete selected events", throwable );
                        showErrorSnackbar( "Failed to delete events: " + throwable.getMessage() );
                    } );
                    return null;
                } );
    }

    private void shareSelectedEvents() {
        Set<String> selectedIds = mStateManager.getSelectedEventIds();
        if (selectedIds.isEmpty()) return;

        mEventOperations.shareSelectedEvents( selectedIds )
                .thenAccept( shareText -> mMainHandler.post( () -> {
                    Intent shareIntent = new Intent( Intent.ACTION_SEND );
                    shareIntent.setType( "text/plain" );
                    shareIntent.putExtra( Intent.EXTRA_TEXT, shareText );
                    startActivity( Intent.createChooser( shareIntent, "Share Events" ) );
                } ) )
                .exceptionally( throwable -> {
                    mMainHandler.post( () -> {
                        Log.e( TAG, "Failed to share selected events", throwable );
                        showErrorSnackbar( "Failed to share events: " + throwable.getMessage() );
                    } );
                    return null;
                } );
    }

    private void exportDayEvents() {
        // Export all events for current day
        if (mEventOperations != null) {
            mEventOperations.exportDayEvents( mTargetDate )
                    .thenAccept( exportedFile -> mMainHandler.post(
                            () -> showSuccessSnackbar( "Events exported to: " + exportedFile ) ) )
                    .exceptionally( throwable -> {
                        mMainHandler.post( () -> {
                            Log.e( TAG, "Failed to export day events", throwable );
                            showErrorSnackbar(
                                    "Failed to export events: " + throwable.getMessage() );
                        } );
                        return null;
                    } );
        }
    }

    // ==================== PRIVATE UI UPDATE METHODS ====================

    private void updateDateHeader(@NonNull LocalDate date) {
        if (mDateHeaderText != null) {
            // Format: "Monday, January 15, 2025"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "EEEE, MMMM d, yyyy" );
            String formattedDate = date.format( formatter );
            mDateHeaderText.setText( formattedDate );
        }
    }

    private void updateEventCount(int eventCount) {
        if (mEventCountText != null) {
            String countText = getResources().getQuantityString(
                    R.plurals.event_count_text, eventCount, eventCount );
            mEventCountText.setText( countText );
        }
    }

    private void updateSelectionUI(int selectedCount, boolean isSelectionMode) {
        if (mSelectionToolbar != null) {
            mSelectionToolbar.setVisibility( isSelectionMode ? View.VISIBLE : View.GONE );
        }

        if (mSelectionCountText != null && isSelectionMode) {
            String countText = getResources().getQuantityString(
                    R.plurals.selection_count_text, selectedCount, selectedCount );
            mSelectionCountText.setText( countText );
        }

        // Update FAB visibility
        if (mFabCreateEvent != null) {
            mFabCreateEvent.setVisibility(
                    (mAllowCreation && !isSelectionMode) ? View.VISIBLE : View.GONE );
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (mEmptyStateView != null && mEventsRecyclerView != null) {
            mEmptyStateView.setVisibility( isEmpty ? View.VISIBLE : View.GONE );
            mEventsRecyclerView.setVisibility( isEmpty ? View.GONE : View.VISIBLE );

            if (isEmpty && mEmptyStateText != null) {
                String emptyMessage = getString( R.string.day_view_empty_state,
                                                 mTargetDate.format( DateTimeFormatter.ofPattern(
                                                         "MMMM d" ) ) );
                mEmptyStateText.setText( emptyMessage );
            }
        }
    }

    private void showErrorState(@Nullable String errorMessage) {
        if (mErrorStateView != null) {
            boolean hasError = errorMessage != null;
            mErrorStateView.setVisibility( hasError ? View.VISIBLE : View.GONE );

            if (hasError && mErrorStateText != null) {
                mErrorStateText.setText( errorMessage );
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    private void showSuccessSnackbar(@NonNull String message) {
        if (mRootView != null) {
            Snackbar.make( mRootView, message, Snackbar.LENGTH_SHORT )
                    .setBackgroundTint( getResources().getColor( R.color.success_color, null ) )
                    .show();
        }
    }

    private void showErrorSnackbar(@NonNull String message) {
        if (mRootView != null) {
            Snackbar.make( mRootView, message, Snackbar.LENGTH_LONG )
                    .setBackgroundTint( getResources().getColor( R.color.error_color, null ) )
                    .setAction( R.string.retry, v -> refreshEvents() )
                    .show();
        }
    }
}