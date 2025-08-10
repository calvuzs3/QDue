package net.calvuz.qdue.ui.features.swipecalendar.presentation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.ServiceProviderImpl;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.WorkScheduleService;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.ui.features.swipecalendar.adapters.MonthPagerAdapter;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;
import net.calvuz.qdue.ui.features.swipecalendar.di.SwipeCalendarModule;
import net.calvuz.qdue.ui.features.events.presentation.EventsActivity;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * SwipeCalendarFragment - Main fragment for swipe-based calendar navigation.
 *
 * <p>Provides a simplified month-to-month calendar view with horizontal swipe navigation
 * as an alternative to infinite scrolling. Features discrete month boundaries with
 * clear range limits (1900-2100) and persistent position state.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Finite Range Navigation</strong>: January 1900 to December 2100</li>
 *   <li><strong>Swipe Boundaries</strong>: Automatic blocking at range limits</li>
 *   <li><strong>State Persistence</strong>: Remembers last viewed month across sessions</li>
 *   <li><strong>"Go to Today"</strong>: Quick navigation to current month</li>
 *   <li><strong>Event Integration</strong>: Full events and work schedule display</li>
 * </ul>
 *
 * <h3>Navigation Logic:</h3>
 * <ul>
 *   <li><strong>First Launch</strong>: Navigate to current month (today)</li>
 *   <li><strong>Return Session</strong>: Restore last viewed month position</li>
 *   <li><strong>Range Limits</strong>: Prevent swiping beyond 1900-2100 range</li>
 *   <li><strong>Smooth Animation</strong>: Hardware-accelerated ViewPager2 transitions</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class SwipeCalendarFragment extends Fragment  {

    private static final String TAG = "SwipeCalendarFragment";

    // Fragment arguments
    public static final String ARG_INITIAL_DATE = "initial_date";
    public static final String ARG_USER_ID = "user_id";

    // ==================== DEPENDENCIES ====================

    private SwipeCalendarModule mCalendarModule;
    private EventsService mEventsService;
    private UserService mUserService;
    private WorkScheduleService mWorkScheduleService;
    private LocaleManager mLocaleManager;

    // ==================== UI COMPONENTS ====================

    // Main layout
    private View mRootView;
    private MaterialCardView mCalendarCard;

    // Header
    private MaterialToolbar mToolbar;
    private TextView mMonthYearText;
    private MaterialButton mPreviousMonthButton;
    private MaterialButton mNextMonthButton;
    private MaterialButton mTodayButton;

    // ViewPager
    private ViewPager2 mViewPager;
    private MonthPagerAdapter mPagerAdapter;

    // FAB
    private FloatingActionButton mFabQuickEvent;

    // ==================== STATE ====================

    private SwipeCalendarStateManager mStateManager;
    private YearMonth mCurrentVisibleMonth;
    private boolean mIsInitialized = false;
    private Handler mMainHandler;

    // User configuration
    private Long mUserId;
    private LocalDate mInitialDate;

    // ==================== FRAGMENT LIFECYCLE ====================

    /**
     * Create new SwipeCalendarFragment instance with optional parameters.
     *
     * @param initialDate Optional initial date to display
     * @param userId      Optional user ID for data filtering
     * @return New fragment instance
     */
    @NonNull
    public static SwipeCalendarFragment newInstance(@Nullable LocalDate initialDate, @Nullable Long userId) {
        SwipeCalendarFragment fragment = new SwipeCalendarFragment();
        Bundle args = new Bundle();

        if ( initialDate != null ) {
            args.putString( ARG_INITIAL_DATE, initialDate.toString() );
        }
        if ( userId != null ) {
            args.putLong( ARG_USER_ID, userId );
        }

        fragment.setArguments( args );
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize handlers
        mMainHandler = new Handler( Looper.getMainLooper() );

        // Parse arguments
        parseArguments();

        Log.d( TAG, "SwipeCalendarFragment created" );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate( R.layout.fragment_swipe_calendar, container, false );
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        // Initialize dependency injection
        initializeDependencyInjection();

        // Initialize UI components
        initializeViews( view );

        // Setup calendar components
        setupCalendar();

        Log.d( TAG, "SwipeCalendarFragment view created and initialized" );
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( mIsInitialized ) {
            // Refresh current month data in case events changed
            refreshCurrentMonth();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Mark session as inactive for proper state restoration
        if ( mCalendarModule != null ) {
            mCalendarModule.onFragmentPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cleanup resources
        cleanup();

        Log.d( TAG, "SwipeCalendarFragment view destroyed" );
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * Initialize dependency injection using ServiceProvider pattern.
     */
    private void initializeDependencyInjection() {
        try {
            // Get service provider from application
            ServiceProvider serviceProvider = getServiceProvider();

            // Inject services
            mEventsService = serviceProvider.getEventsService();
            mUserService = serviceProvider.getUserService();
            mWorkScheduleService = serviceProvider.getWorkScheduleService();

            // Create locale manager
            mLocaleManager = new LocaleManager( requireContext() );

            // Create calendar module with dependencies
            mCalendarModule = new SwipeCalendarModule(
                    requireActivity(),  // pass the ActivityContext for theme resolution
                    mEventsService,
                    mUserService,
                    mWorkScheduleService
            );

            // Configure user ID if provided
            if ( mUserId != null ) {
                mCalendarModule.setCurrentUserId( mUserId );
            }

            Log.d( TAG, "Dependency injection completed successfully" );

        } catch (Exception e) {
            Log.e( TAG, "Failed to initialize dependency injection", e );
            showError( getString( R.string.error_calendar_initialization_failed ) );
        }
    }

    /**
     * Get ServiceProvider from hosting activity.
     */
    @NonNull
    private ServiceProvider getServiceProvider() {
        Context context = requireActivity(); // requireContext();
        if ( context instanceof Injectable ) {
            // Activity implements Injectable, get ServiceProvider from it
            //return ((Injectable) context).getServiceProvider(); // Activity doesnt provide ServiceProvider
            Log.e(TAG, "Activity doesn't provide ServiceProvider");
            throw new RuntimeException("Activity doesn't provide ServiceProvider");
        }

        // Fallback to application-level ServiceProvider
        Log.w(TAG, "Fallback to ServiceProviderImpl.getInstance( context.getApplicationContext() )");
        return ServiceProviderImpl.getInstance( context.getApplicationContext() );
    }

    // ==================== UI INITIALIZATION ====================

    /**
     * Parse fragment arguments.
     */
    private void parseArguments() {
        Bundle args = getArguments();
        if ( args != null ) {
            // Parse initial date
            String initialDateStr = args.getString( ARG_INITIAL_DATE );
            if ( initialDateStr != null ) {
                try {
                    mInitialDate = LocalDate.parse( initialDateStr );
                } catch (Exception e) {
                    Log.w( TAG, "Invalid initial date: " + initialDateStr, e );
                    mInitialDate = null;
                }
            }

            // Parse user ID
            if ( args.containsKey( ARG_USER_ID ) ) {
                mUserId = args.getLong( ARG_USER_ID );
            }
        }
    }

    /**
     * Initialize UI components from layout.
     */
    private void initializeViews(@NonNull View rootView) {
        // Main layout
        mCalendarCard = rootView.findViewById( R.id.calendar_card );

        // Header components
        mToolbar = rootView.findViewById( R.id.calendar_toolbar );
        mMonthYearText = rootView.findViewById( R.id.month_year_text );
        mPreviousMonthButton = rootView.findViewById( R.id.btn_previous_month );
        mNextMonthButton = rootView.findViewById( R.id.btn_next_month );
        mTodayButton = rootView.findViewById( R.id.btn_today );

        // ViewPager
        mViewPager = rootView.findViewById( R.id.calendar_view_pager );

        // FAB
        mFabQuickEvent = rootView.findViewById( R.id.fab_quick_event );

        // Setup click listeners
        setupClickListeners();

        // Configure ViewPager
        configureViewPager();
    }

    /**
     * Setup click listeners for UI components.
     */
    private void setupClickListeners() {
        // Previous month button
        if ( mPreviousMonthButton != null ) {
            mPreviousMonthButton.setOnClickListener( v -> navigateToPreviousMonth() );
        }

        // Next month button
        if ( mNextMonthButton != null ) {
            mNextMonthButton.setOnClickListener( v -> navigateToNextMonth() );
        }

        // Today button
        if ( mTodayButton != null ) {
            mTodayButton.setOnClickListener( v -> navigateToToday() );
        }

        // Quick event FAB
        if ( mFabQuickEvent != null ) {
            mFabQuickEvent.setOnClickListener( v -> openQuickEventCreation() );
        }
    }

    /**
     * Configure ViewPager2 settings.
     */
    private void configureViewPager() {
        if ( mViewPager != null ) {
            // Basic configuration
            mViewPager.setOffscreenPageLimit( 1 ); // Keep 1 page on each side
            mViewPager.setOverScrollMode( View.OVER_SCROLL_NEVER );

            // Page change callback
            mViewPager.registerOnPageChangeCallback( new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected( position );
                    onMonthPageSelected( position );
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged( state );

                    if ( state == ViewPager2.SCROLL_STATE_DRAGGING ) {
                        // Check if we're at boundaries and prevent swipe if needed
                        checkAndHandleBoundaries();
                    }
                }
            } );
        }
    }

    // ==================== CALENDAR SETUP ====================

    /**
     * Setup calendar components and initialize state.
     */
    private void setupCalendar() {
        if ( mCalendarModule == null ) {
            Log.e( TAG, "Calendar module not initialized" );
            return;
        }

        try {
            // Get state manager
            mStateManager = mCalendarModule.provideStateManager();

            // Get pager adapter
            mPagerAdapter = mCalendarModule.providePagerAdapter();
            mPagerAdapter.setOnMonthInteractionListener( new MonthInteractionListener() );

            // Set adapter to ViewPager
            if ( mViewPager != null ) {
                mViewPager.setAdapter( mPagerAdapter );
            }

            // Determine initial position
            int initialPosition;
            if ( mInitialDate != null ) {
                // Use provided initial date
                YearMonth initialMonth = YearMonth.from( mInitialDate );
                initialPosition = SwipeCalendarStateManager.getPositionForMonth( initialMonth );
                Log.d( TAG, "Using provided initial date: " + mInitialDate );
            } else {
                // Use state manager to determine position
                initialPosition = mStateManager.initializeAndGetInitialPosition();
                Log.d( TAG, "Using state manager initial position: " + initialPosition );
            }

            // Navigate to initial position
            navigateToPosition( initialPosition, false );

            mIsInitialized = true;
            Log.d( TAG, "Calendar setup completed successfully" );

        } catch (Exception e) {
            Log.e( TAG, "Failed to setup calendar", e );
            showError( getString( R.string.error_calendar_setup_failed ) );
        }
    }

    // ==================== NAVIGATION ====================

    /**
     * Called when ViewPager page is selected.
     */
    private void onMonthPageSelected(int position) {
        YearMonth month = SwipeCalendarStateManager.getMonthForPosition( position );
        mCurrentVisibleMonth = month;

        // Update state manager
        if ( mStateManager != null ) {
            mStateManager.updatePosition( position );
        }

        // Update header
        updateHeaderForMonth( month );

        // Update navigation button states
        updateNavigationButtons( position );

        Log.v( TAG, "Month page selected: " + month + " (position " + position + ")" );
    }

    /**
     * Navigate to specific position.
     */
    private void navigateToPosition(int position, boolean smooth) {
        if ( mViewPager != null && SwipeCalendarStateManager.isValidPosition( position ) ) {
            mViewPager.setCurrentItem( position, smooth );
        }
    }

    /**
     * Navigate to previous month.
     */
    private void navigateToPreviousMonth() {
        if ( mViewPager != null ) {
            int currentPosition = mViewPager.getCurrentItem();
            if ( currentPosition > 0 ) {
                navigateToPosition( currentPosition - 1, true );
            } else {
                showBoundaryMessage( true );
            }
        }
    }

    /**
     * Navigate to next month.
     */
    private void navigateToNextMonth() {
        if ( mViewPager != null ) {
            int currentPosition = mViewPager.getCurrentItem();
            int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;
            if ( currentPosition < maxPosition ) {
                navigateToPosition( currentPosition + 1, true );
            } else {
                showBoundaryMessage( false );
            }
        }
    }

    /**
     * Navigate to today's month.
     */
    private void navigateToToday() {
        if ( mStateManager != null ) {
            int todayPosition = mStateManager.navigateToToday();
            navigateToPosition( todayPosition, true );

            // Show feedback
            Snackbar.make( mRootView, R.string.calendar_navigated_to_today, Snackbar.LENGTH_SHORT ).show();
        }
    }

    /**
     * Check boundaries and handle swipe blocking.
     */
    private void checkAndHandleBoundaries() {
        if ( mViewPager != null ) {
            int currentPosition = mViewPager.getCurrentItem();
            int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;

            // At start boundary
            if ( currentPosition == 0 ) {
                // Prevent further swiping left (previous)
                // ViewPager2 handles this automatically, but we can show feedback
                Log.v( TAG, "At start boundary, preventing further left swipe" );
            }

            // At end boundary
            else if ( currentPosition == maxPosition ) {
                // Prevent further swiping right (next)
                // ViewPager2 handles this automatically, but we can show feedback
                Log.v( TAG, "At end boundary, preventing further right swipe" );
            }
        }
    }

    // ==================== UI UPDATES ====================

    /**
     * Update header text for current month.
     */
    private void updateHeaderForMonth(@NonNull YearMonth month) {
        if ( mMonthYearText != null && mLocaleManager != null ) {
            // Format month and year according to current locale
            String monthName = month.getMonth().getDisplayName( TextStyle.FULL, mLocaleManager.getCurrentLocale() );
            String yearText = String.valueOf( month.getYear() );

            String headerText = getString( R.string.calendar_month_year_format, monthName, yearText );
            mMonthYearText.setText( headerText );
        }
    }

    /**
     * Update navigation button states based on position.
     */
    private void updateNavigationButtons(int position) {
        int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;

        // Previous button
        if ( mPreviousMonthButton != null ) {
            mPreviousMonthButton.setEnabled( position > 0 );
            mPreviousMonthButton.setAlpha( position > 0 ? 1.0f : 0.5f );
        }

        // Next button
        if ( mNextMonthButton != null ) {
            mNextMonthButton.setEnabled( position < maxPosition );
            mNextMonthButton.setAlpha( position < maxPosition ? 1.0f : 0.5f );
        }

        // Today button - enable if not already at current month
        if ( mTodayButton != null && mStateManager != null ) {
            int todayPosition = mStateManager.getTodayPosition();
            boolean isAtToday = position == todayPosition;
            mTodayButton.setEnabled( !isAtToday );
            mTodayButton.setAlpha( isAtToday ? 0.5f : 1.0f );
        }
    }

    /**
     * Show boundary message when user tries to swipe beyond limits.
     */
    private void showBoundaryMessage(boolean isStartBoundary) {
        String message = isStartBoundary ?
                getString( R.string.calendar_boundary_start_message ) :
                getString( R.string.calendar_boundary_end_message );

        Toast.makeText( requireContext(), message, Toast.LENGTH_SHORT ).show();
    }


    // ==================== EVENT HANDLING ====================

    /**
     * Month interaction listener implementation.
     */
    private class MonthInteractionListener implements MonthPagerAdapter.OnMonthInteractionListener {

        @Override
        public void onDayClick(@NonNull LocalDate date, @Nullable Day day, @NonNull List<LocalEvent> events) {
            // Handle day click - could open day detail view or event creation
            Log.d( TAG, "Day clicked: " + date + ", events count: " + events.size() );

            if ( !events.isEmpty() ) {
                // Has events - could show events list or detail
                openDayEventsView( date, events );
            } else {
                // No events - could show quick event creation
                openQuickEventCreation( date );
            }
        }

        @Override
        public void onDayLongClick(@NonNull LocalDate date, @Nullable Day day, @NonNull View view) {
            // Handle day long click - could open context menu or quick actions
            Log.d( TAG, "Day long clicked: " + date );

            // Provide haptic feedback
            view.performHapticFeedback( android.view.HapticFeedbackConstants.LONG_PRESS );

            // Open quick event creation
            openQuickEventCreation( date );
        }

        @Override
        public void onMonthLoadError(@NonNull YearMonth month, @NonNull Exception error) {
            Log.e( TAG, "Month load error for " + month, error );

            String errorMessage = getString( R.string.error_calendar_month_load_failed,
                    month.getMonth().getDisplayName( TextStyle.FULL, Locale.getDefault() ) );
            showError( errorMessage );
        }
    }

    /**
     * Open day events view.
     */
    private void openDayEventsView(@NonNull LocalDate date, @NonNull List<LocalEvent> events) {
        // TODO: Implement day events view
        // Could open EventsActivity filtered to specific date
        Intent intent = new Intent( requireContext(), EventsActivity.class );
        intent.putExtra( EventsActivity.EXTRA_FILTER_DATE, date.toString() );
        startActivity( intent );
    }

    /**
     * Open quick event creation.
     */
    private void openQuickEventCreation() {
        openQuickEventCreation( mCurrentVisibleMonth != null ? mCurrentVisibleMonth.atDay( 1 ) : LocalDate.now() );
    }

    /**
     * Open quick event creation for specific date.
     */
    private void openQuickEventCreation(@NonNull LocalDate date) {
        // TODO: Implement quick event creation
        // Could open EventsActivity in creation mode
        Intent intent = new Intent( requireContext(), EventsActivity.class );
        //intent.putExtra( EventsActivity.EXTRA_CREATE_EVENT_DATE, date.toString() );
        startActivity( intent );
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Refresh current month data.
     */
    private void refreshCurrentMonth() {
        if ( mPagerAdapter != null && mCurrentVisibleMonth != null ) {
            mPagerAdapter.refreshMonth( mCurrentVisibleMonth );
        }
    }

    /**
     * Show error message to user.
     */
    private void showError(@NonNull String message) {
        if ( mRootView != null ) {
            Snackbar.make( mRootView, message, Snackbar.LENGTH_LONG )
                    .setAction( R.string.action_retry, v -> setupCalendar() )
                    .show();
        } else {
            Toast.makeText( requireContext(), message, Toast.LENGTH_LONG ).show();
        }
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        // Cleanup calendar module
        if ( mCalendarModule != null ) {
            mCalendarModule.onDestroy();
            mCalendarModule = null;
        }

        // Clear ViewPager adapter
        if ( mViewPager != null ) {
            mViewPager.setAdapter( null );
        }

        // Clear references
        mPagerAdapter = null;
        mStateManager = null;
        mCurrentVisibleMonth = null;
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Get current visible month.
     *
     * @return Current visible month, or null if not initialized
     */
    @Nullable
    public YearMonth getCurrentVisibleMonth() {
        return mCurrentVisibleMonth;
    }

    /**
     * Navigate to specific month programmatically.
     *
     * @param month  Target month
     * @param smooth Whether to use smooth animation
     */
    public void navigateToMonth(@NonNull YearMonth month, boolean smooth) {
        if ( SwipeCalendarStateManager.isValidPosition( SwipeCalendarStateManager.getPositionForMonth( month ) ) ) {
            int position = SwipeCalendarStateManager.getPositionForMonth( month );
            navigateToPosition( position, smooth );
        } else {
            Log.w( TAG, "Invalid month for navigation: " + month );
        }
    }
}