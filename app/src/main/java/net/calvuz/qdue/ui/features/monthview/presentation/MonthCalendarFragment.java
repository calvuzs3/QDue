package net.calvuz.qdue.ui.features.monthview.presentation;

import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.ui.features.monthview.adapters.MonthPagerAdapter;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;
import net.calvuz.qdue.ui.features.swipecalendar.di.CalendarSharedViewModelModule;
import net.calvuz.qdue.ui.features.monthview.di.MonthViewModule;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.swipecalendar.viewmodels.CalendarSharedViewModel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;

/**
 * MonthCalendarFragment - Main fragment for swipe-based calendar navigation.
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
 */
public class MonthCalendarFragment
        extends Fragment
        implements Injectable, MonthPagerAdapter.OnMonthInteractionListener
{

    private static final String TAG = "MonthCalendarFragment";

    // Fragment arguments
    public static final String ARG_INITIAL_DATE = "initial_date";
    public static final String ARG_USER_ID = "user_id";

    // ==================== DEPENDENCIES ====================

    private CalendarServiceProvider mCalendarServiceProvider;
    private MonthViewModule mCalendarModule;
    private EventsService mEventsService;
    private QDueUserService mQDueUserService;
    private WorkScheduleRepository mWorkScheduleRepository;
    private LocaleManager mLocaleManager;

    // Add SharedViewModel field
    private CalendarSharedViewModel mSharedViewModel;
    private CalendarSharedViewModelModule mSharedViewModelModule;

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
//    private FloatingActionButton mFabQuickEvent;

    // ==================== STATE ====================

    private SwipeCalendarStateManager mStateManager;
    private YearMonth mCurrentVisibleMonth;
    private boolean mIsInitialized = false;
    private Handler mMainHandler;

    // User configuration
    private String mUserId;
    private LocalDate mInitialDate;

    // ==================== LISTENER ====================

    private OnMonthCalendarListener mListener;

    /**
     * Called when user clicks on a day.
     *
     * @param date
     * @param day
     * @param events
     */
    @Override
    public void onDayClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull List<LocalEvent> events) {
        Log.d(TAG, "Day clicked: " + date);

        // Update SharedViewModel
        if (mSharedViewModel != null) {
            mSharedViewModel.navigateToDate(date);
        }

        // Navigate to day view
        if (mListener != null) {
            mListener.onNavigateToDayView(date);
            Log.d(TAG, "Day clicked, navigating to day view: " + date);
        } else {
            Log.w(TAG, "OnMonthCalendarListener not set, cannot navigate to day view");
        }
    }

    /**
     * Called when user long-clicks on a day.
     *
     * @param date
     * @param day
     * @param view
     */
    @Override
    public void onDayLongClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull View view) {
        Log.d(TAG, "Day long clicked: " + date);

        // Handle long click for quick event creation
        if (mListener != null) {
            mListener.onCreateQuickEvent(date);
            Log.d(TAG, "Day long clicked, creating quick event: " + date);
        }
    }

    /**
     * Called when month data loading fails.
     *
     * @param month
     * @param error
     */
    @Override
    public void onMonthLoadError(@NonNull YearMonth month, @NonNull Exception error) {
        Log.e(TAG, "Month load error for " + month, error);
        showError("Failed to load data for " + month);
    }

    // ==================== LISTENER INTERFACE ====================

    /**
     * Interface for communicating with parent Activity
     */
    public interface OnMonthCalendarListener
    {
        /**
         * Called when user wants to view day details
         */
        void onNavigateToDayView(@NonNull LocalDate date);

        /**
         * Called when user wants to create quick event
         */
        void onCreateQuickEvent(@NonNull LocalDate date);
    }

    // ==================== FRAGMENT LIFECYCLE ====================

    /**
     * Create new MonthCalendarFragment instance with optional parameters.
     *
     * @param initialDate Optional initial date to display
     * @param userId      Optional user ID for data filtering
     * @return New fragment instance
     */
    @NonNull
    public static MonthCalendarFragment newInstance(
            @Nullable LocalDate initialDate,
            @Nullable String userId
    ) {
        MonthCalendarFragment fragment = new MonthCalendarFragment();
        Bundle args = new Bundle();

        if (initialDate != null) {
            args.putString( ARG_INITIAL_DATE, initialDate.toString() );
        }
        if (userId != null) {
            args.putString( ARG_USER_ID, userId );
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

        // Menu
        setHasOptionsMenu( true );

        Log.d( TAG, "MonthCalendarFragment created" );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate( R.layout.fragment_month_calendar, container, false );
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        // Initialize dependency injection
        initializeDependencyInjection();

        // Initialize feature components
        initializeFeatureComponents();

        // Initialize UI components
        initializeViews( view );

        // Setup calendar components
        setupCalendar();

        // ✅ FINALLY: Setup SharedViewModel observers AFTER everything is initialized
        setupSharedViewModelObservers();

        Log.d( TAG, "MonthCalendarFragment view created and initialized" );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsInitialized && mStateManager != null) {
            // ✅ IMPROVED: Sync with SharedViewModel first
            if (mSharedViewModel != null) {
                YearMonth sharedMonth = mSharedViewModel.getCurrentMonth().getValue();
                Integer sharedPosition = mSharedViewModel.getCurrentPosition().getValue();

                if (sharedMonth != null && sharedPosition != null) {
                    // Sync ViewPager with SharedViewModel state
                    if (mViewPager != null && mViewPager.getCurrentItem() != sharedPosition) {
                        mViewPager.setCurrentItem( sharedPosition, false );
                    }

                    // Update local state
                    mCurrentVisibleMonth = sharedMonth;
                    updateHeaderForMonth( sharedMonth );

                    Log.d( TAG, "Resumed and synced with SharedViewModel: " + sharedMonth );
                }
            }

            // Refresh current month data
            refreshCurrentMonth();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Mark session as inactive for proper state restoration
        if (mCalendarModule != null) {
            mCalendarModule.onFragmentPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cleanup resources
        cleanup();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach( context );
        if (context instanceof OnMonthCalendarListener) {
            mListener = (OnMonthCalendarListener) context;
        } else {
            throw new RuntimeException(
                    context.toString() + " must implement OnMonthCalendarListener" );
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * ✅ NEW: Receive dependencies from ServiceProvider
     */
    @Override
    public void inject(ServiceProvider serviceProvider) {
        mCalendarServiceProvider = serviceProvider.getCalendarServiceProvider();
        mEventsService = serviceProvider.getEventsService();
        mQDueUserService = serviceProvider.getQDueUserService();
        mWorkScheduleRepository = serviceProvider
                .getCalendarService()
                .getCalendarServiceProvider()
                .getWorkScheduleRepository();

        Log.d( TAG, "✅ Services injected via DependencyInjector" );
    }

    /**
     * ✅ NEW: Validate all dependencies are present
     */
    @Override
    public boolean areDependenciesReady() {
        return mCalendarServiceProvider != null &&
                mEventsService != null &&
                mQDueUserService != null &&
                mWorkScheduleRepository != null;
    }

    // ==================== INITIALIZATION ====================

    /**
     * ✅ NEW: Standardized dependency injection using DependencyInjector
     */
    private void initializeDependencyInjection() {
        try {
            Log.d( TAG, "Initializing dependencies with DependencyInjector..." );

            // ✅ ONE LINE INJECTION
            DependencyInjector.inject( this, requireActivity() );

            // ✅ VERIFICATION
            if (!DependencyInjector.verifyInjection( this, requireActivity() )) {
                throw new RuntimeException( "Dependency injection verification failed" );
            }

            // ✅ CORRECT ORDER: Initialize SharedViewModel module AFTER core dependencies
            mSharedViewModelModule = new CalendarSharedViewModelModule(requireContext(),
                                                                       mCalendarServiceProvider);

            // ✅ Get SharedViewModel from Activity scope
            ViewModelProvider.Factory factory = mSharedViewModelModule.getViewModelFactory();
            mSharedViewModel = new ViewModelProvider(requireActivity(), factory).get(
                    CalendarSharedViewModel.class);


            Log.d(TAG, "Dependencies injected and SharedViewModel initialized successfully");

        } catch (Exception e) {
            Log.e( TAG, "❌ Failed to initialize dependency injection", e );
            showError( getString( R.string.error_calendar_initialization_failed ) );
            throw new RuntimeException( "Dependency injection failed", e );
        }

//        // Initialize SharedViewModel module
//        mSharedViewModelModule = new CalendarSharedViewModelModule( requireContext(),
//                                                                    mCalendarServiceProvider );
//
//        // Get SharedViewModel from Activity scope
//        ViewModelProvider.Factory factory = mSharedViewModelModule.getViewModelFactory();
//        mSharedViewModel = new ViewModelProvider( requireActivity(), factory ).get(
//                CalendarSharedViewModel.class );
//
//        Log.d( TAG, "SharedViewModel integration initialized" );
    }

    // In onViewCreated() - Setup observers
    private void setupSharedViewModelObservers() {
        // Observe current month changes
        mSharedViewModel.getCurrentMonth().observe( getViewLifecycleOwner(),
                                                    this::onSharedMonthChanged );

        // Observe selected date changes
        mSharedViewModel.getSelectedDate().observe( getViewLifecycleOwner(),
                                                    this::onSharedDateChanged );

        // Observe navigation events from DayView
        mSharedViewModel.getNavigationEvent().observe( getViewLifecycleOwner(),
                                                       this::onNavigationEvent );

        // Observe position changes
        mSharedViewModel.getCurrentPosition().observe( getViewLifecycleOwner(),
                                                       this::onSharedPositionChanged );
    }

    /**
     * ✅ NEW: Initialize feature components after successful injection
     */
    private void initializeFeatureComponents() {
        try {
            mLocaleManager = new LocaleManager( requireContext() );

            mCalendarModule = new MonthViewModule(
                    requireActivity(),          // Activity context for theming
                    mCalendarServiceProvider    // Injected service
            );

            if (!mCalendarModule.areDependenciesReady()) {
                throw new IllegalStateException( "MonthViewModule dependencies not ready" );
            }

            Log.d( TAG, "✅ Feature components initialized successfully" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to initialize feature components", e );
            throw new RuntimeException( "Feature components initialization failed", e );
        }
    }

    // ========================== MVVM ==========================

    // Handle shared month changes
    private void onSharedMonthChanged(@NonNull YearMonth month) {
        if (mCurrentVisibleMonth == null || !mCurrentVisibleMonth.equals( month )) {
            mCurrentVisibleMonth = month;
            updateHeaderForMonth( month );

            // Update ViewPager position if needed
            int targetPosition = SwipeCalendarStateManager.getPositionForMonth( month );
            if (mViewPager != null && mViewPager.getCurrentItem() != targetPosition) {
                mViewPager.setCurrentItem( targetPosition, false );
            }

            Log.d( TAG, "Month updated from SharedViewModel: " + month );
        }
    }

    // Handle shared date changes
    private void onSharedDateChanged(@NonNull LocalDate date) {
        // Update any date-specific UI if needed
        Log.d( TAG, "Selected date updated from SharedViewModel: " + date );
    }

    // Handle navigation events from DayView
    private void onNavigationEvent(@NonNull CalendarSharedViewModel.NavigationEvent event) {
        if (event.targetViewMode == CalendarSharedViewModel.ViewMode.MONTH) {
            YearMonth targetMonth = YearMonth.from( event.targetDate );
            int targetPosition = SwipeCalendarStateManager.getPositionForMonth( targetMonth );

            if (mViewPager != null) {
                mViewPager.setCurrentItem( targetPosition, event.shouldAnimate );
            }

            Log.d( TAG, "Navigation event processed: " + event.targetDate );
        }
    }

    // Handle position changes from ViewModel
    private void onSharedPositionChanged(@NonNull Integer position) {
        if (mViewPager != null && mViewPager.getCurrentItem() != position) {
            mViewPager.setCurrentItem( position, false );
            Log.v( TAG, "ViewPager position synced: " + position );
        }
    }

    // ==================== UI INITIALIZATION ====================

    /**
     * Parse fragment arguments.
     */
    private void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            // Parse initial date
            String initialDateStr = args.getString( ARG_INITIAL_DATE );
            if (initialDateStr != null) {
                try {
                    mInitialDate = LocalDate.parse( initialDateStr );
                } catch (Exception e) {
                    Log.w( TAG, "Invalid initial date: " + initialDateStr, e );
                    mInitialDate = null;
                }
            }

            // Parse user ID
            if (args.containsKey( ARG_USER_ID )) {
                mUserId = args.getString( ARG_USER_ID );
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
//        mFabQuickEvent = rootView.findViewById( R.id.fab_quick_event );

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
        if (mPreviousMonthButton != null) {
            mPreviousMonthButton.setOnClickListener( v -> navigateToPreviousMonth() );
        }

        // Next month button
        if (mNextMonthButton != null) {
            mNextMonthButton.setOnClickListener( v -> navigateToNextMonth() );
        }

        // Today button
        if (mTodayButton != null) {
            mTodayButton.setOnClickListener( v -> navigateToToday() );
        }

        // Quick event FAB
//        if ( mFabQuickEvent != null ) {
//            mFabQuickEvent.setOnClickListener( v -> openQuickEventCreation() );
//        }
    }

    /**
     * Configure ViewPager2 settings.
     */
    private void configureViewPager() {
        if (mViewPager != null) {
            // Basic configuration
            mViewPager.setOffscreenPageLimit( 1 ); // Keep 1 page on each side
            mViewPager.setOverScrollMode( View.OVER_SCROLL_NEVER );

            // Page change callback
            mViewPager.registerOnPageChangeCallback( new ViewPager2.OnPageChangeCallback()
            {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected( position );
                    onMonthPageSelected( position );
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged( state );

                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        // Check if we're at boundaries and prevent swipe if needed
                        checkAndHandleBoundaries();
                    }
                }
            } );
        }
    }

    // ========================= MENU =========================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu( menu, inflater );

        inflater.inflate( R.menu.menu_main, menu );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected( item );
    }

    // ==================== CALENDAR SETUP ====================

    /**
     * Setup calendar components and initialize state.
     */
    private void setupCalendar() {
        if (mCalendarModule == null) {
            Log.e( TAG, "Calendar module not initialized" );
            return;
        }

        try {
            // Get state manager
            mStateManager = mCalendarModule.provideStateManager();

            // Get pager adapter
            mPagerAdapter = mCalendarModule.providePagerAdapter();

            // ✅ FIXED: Use 'this' instead of new MonthInteractionListener()
            mPagerAdapter.setOnMonthInteractionListener(this);

            // Set adapter to ViewPager
            if (mViewPager != null) {
                mViewPager.setAdapter(mPagerAdapter);
            }

            // ✅ IMPROVED: Better initial position logic
            loadInitialMonth();

            mIsInitialized = true;
            Log.d( TAG, "Calendar setup completed successfully" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to setup calendar", e );
            showError( getString( R.string.error_calendar_setup_failed ) );
        }
    }

    // ✅ FIX 5: ADD NEW loadInitialMonth() METHOD
    private void loadInitialMonth() {
        int initialPosition;

        // ✅ IMPROVEMENT: Check for initial date parameter or SharedViewModel state
        if (mInitialDate != null) {
            // Use provided initial date
            YearMonth targetMonth = YearMonth.from(mInitialDate);
            initialPosition = SwipeCalendarStateManager.getPositionForMonth(targetMonth);
            mCurrentVisibleMonth = targetMonth;

            // Update SharedViewModel
            if (mSharedViewModel != null) {
                mSharedViewModel.navigateToMonth(targetMonth, initialPosition);
            }

            Log.d(TAG, "Loading initial date from parameter: " + mInitialDate);
        } else if (mSharedViewModel != null) {
            // Try to get state from SharedViewModel
            YearMonth sharedMonth = mSharedViewModel.getCurrentMonth().getValue();
            if (sharedMonth != null) {
                initialPosition = SwipeCalendarStateManager.getPositionForMonth(sharedMonth);
                mCurrentVisibleMonth = sharedMonth;
                Log.d(TAG, "Loading initial date from SharedViewModel: " + sharedMonth);
            } else {
                // ✅ FIXED: Default to today instead of state manager (which might go to 1900)
                YearMonth today = YearMonth.now();
                initialPosition = SwipeCalendarStateManager.getPositionForMonth(today);
                mCurrentVisibleMonth = today;

                // Update both state manager and SharedViewModel
                if (mStateManager != null) {
                    mStateManager.updatePosition(initialPosition);
                }
                mSharedViewModel.navigateToMonth(today, initialPosition);

                Log.d(TAG, "Loading today as initial date: " + today);
            }
        } else {
            // ✅ FIXED: Fallback to today instead of state manager
            YearMonth today = YearMonth.now();
            initialPosition = SwipeCalendarStateManager.getPositionForMonth(today);
            mCurrentVisibleMonth = today;

            if (mStateManager != null) {
                mStateManager.updatePosition(initialPosition);
            }

            Log.d(TAG, "Loading today as fallback initial date: " + today);
        }

        // Set ViewPager position
        if (mViewPager != null) {
            mViewPager.setCurrentItem(initialPosition, false);
        }

        // Update header
        updateHeaderForMonth(mCurrentVisibleMonth);

        Log.d(TAG, "Initial month loaded: " + mCurrentVisibleMonth + " at position: " + initialPosition);
    }

    // ==================== DATA UPDATES ====================

    /**
     * Public method to refresh calendar data when called from parent activity.
     * This method should be called when assignment data changes.
     */
    public void refreshData() {
        Log.d( TAG, "Refreshing calendar data after assignment update" );

        try {
            // 🔄 Metodo più specifico: refresh del mese corrente usando l'adapter
            if (mPagerAdapter != null && mCurrentVisibleMonth != null) {
                // Usa il metodo specifico dell'adapter che gestisce correttamente cache e stato
                mPagerAdapter.refreshMonth( mCurrentVisibleMonth );
                Log.d( TAG, "Refreshed current month: " + mCurrentVisibleMonth );
            }

            // 🔄 Opzionale: refresh anche dei mesi adiacenti se necessario
            if (mPagerAdapter != null && mCurrentVisibleMonth != null) {
                YearMonth previousMonth = mCurrentVisibleMonth.minusMonths( 1 );
                YearMonth nextMonth = mCurrentVisibleMonth.plusMonths( 1 );

                mPagerAdapter.refreshMonth( previousMonth );
                mPagerAdapter.refreshMonth( nextMonth );

                Log.d( TAG, "Refreshed adjacent months for better UX" );
            }

            // 🔄 Se hai bisogno di invalidare completamente la cache
            // mPagerAdapter.clearCache(); // Solo in casi estremi

            Log.d( TAG, "Calendar data refresh completed successfully" );
        } catch (Exception e) {
            Log.e( TAG, "Error during calendar data refresh", e );
            showError( "Failed to refresh calendar data" );
        }
    }

    // ==================== NAVIGATION ====================

    /**
     * Called when ViewPager page is selected.
     */
    private void onMonthPageSelected(int position) {
        YearMonth month = SwipeCalendarStateManager.getMonthForPosition( position );
        mCurrentVisibleMonth = month;

        // Update state manager (existing code)
        if (mStateManager != null) {
            mStateManager.updatePosition( position );
        }

        // ✅ NEW: Notify SharedViewModel
        if (mSharedViewModel != null) {
            mSharedViewModel.navigateToMonth( month, position );
        }

        // Update header (existing code)
        updateHeaderForMonth( month );
        updateNavigationButtons( position );

        Log.v( TAG, "Month page selected and SharedViewModel notified: " + month );
    }

    /**
     * Navigate to specific position.
     */
    private void navigateToPosition(int position, boolean smooth) {
        if (mViewPager != null && SwipeCalendarStateManager.isValidPosition( position )) {
            mViewPager.setCurrentItem( position, smooth );
        }
    }

    /**
     * Navigate to previous month.
     */
    private void navigateToPreviousMonth() {
        if (mViewPager != null) {
            int currentPosition = mViewPager.getCurrentItem();
            if (currentPosition > 0) {
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
        if (mViewPager != null) {
            int currentPosition = mViewPager.getCurrentItem();
            int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;
            if (currentPosition < maxPosition) {
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
        if (mStateManager != null) {
            int todayPosition = mStateManager.navigateToToday();
            navigateToPosition( todayPosition, true );

            // Show feedback
            Snackbar.make( mRootView, R.string.calendar_navigated_to_today,
                           Snackbar.LENGTH_SHORT ).show();
        }
    }

    /**
     * Check boundaries and handle swipe blocking.
     */
    private void checkAndHandleBoundaries() {
        if (mViewPager != null) {
            int currentPosition = mViewPager.getCurrentItem();
            int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;

            // At start boundary
            if (currentPosition == 0) {
                // Prevent further swiping left (previous)
                // ViewPager2 handles this automatically, but we can show feedback
                Log.v( TAG, "At start boundary, preventing further left swipe" );
            }

            // At end boundary
            else if (currentPosition == maxPosition) {
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
        if (mMonthYearText != null && mLocaleManager != null) {
            // Format month and year according to current locale
            String monthName = month.getMonth().getDisplayName( TextStyle.FULL,
                                                                mLocaleManager.getCurrentLocale() );
            String yearText = String.valueOf( month.getYear() );

            String headerText = getString( R.string.calendar_month_year_format, monthName,
                                           yearText );
            mMonthYearText.setText( headerText );

            Log.i( TAG, "Header text updated: " + headerText );
        } else {

            Log.e( TAG, "MonthYearText or LocaleManager not initialized" );
        }
    }

    /**
     * Update navigation button states based on position.
     */
    private void updateNavigationButtons(int position) {
        int maxPosition = SwipeCalendarStateManager.getTotalMonths() - 1;

        // Previous button
        if (mPreviousMonthButton != null) {
            mPreviousMonthButton.setEnabled( position > 0 );
            mPreviousMonthButton.setAlpha( position > 0 ? 1.0f : 0.5f );
        }

        // Next button
        if (mNextMonthButton != null) {
            mNextMonthButton.setEnabled( position < maxPosition );
            mNextMonthButton.setAlpha( position < maxPosition ? 1.0f : 0.5f );
        }

        // Today button - enable if not already at current month
        if (mTodayButton != null && mStateManager != null) {
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

    // ==================== UTILITY METHODS ====================

    /**
     * Refresh current month data.
     */
    private void refreshCurrentMonth() {
        if (mPagerAdapter != null && mCurrentVisibleMonth != null) {
            mPagerAdapter.refreshMonth( mCurrentVisibleMonth );
        }
    }

    /**
     * Show error message to user.
     */
    private void showError(@NonNull String message) {
        if (mRootView != null) {
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
        if (mCalendarModule != null) {
            mCalendarModule.onDestroy();
            mCalendarModule = null;
        }

        // Clear ViewPager adapter
        if (mViewPager != null) {
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
        if (SwipeCalendarStateManager.isValidPosition(
                SwipeCalendarStateManager.getPositionForMonth( month ) )) {
            int position = SwipeCalendarStateManager.getPositionForMonth( month );
            navigateToPosition( position, smooth );
        } else {
            Log.w( TAG, "Invalid month for navigation: " + month );
        }
    }
}