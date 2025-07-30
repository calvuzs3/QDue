package net.calvuz.qdue.ui.core.architecture.base;

import static net.calvuz.qdue.QDue.Debug.DEBUG_BASEFRAGMENT;
import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_RADIUS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.ui.features.events.interfaces.EventsRefreshInterface;
import net.calvuz.qdue.ui.core.common.interfaces.FragmentCommunicationInterface;
import net.calvuz.qdue.ui.core.common.interfaces.NotifyUpdatesInterface;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base fragment that implements common logic for DayslistViewFragment and CalendarViewFragment.
 * <p>
 * This class provides:
 * - Infinite scrolling with intelligent caching
 * - Unified data management through CalendarDataManager
 * - Layout manager agnostic operations (works with LinearLayoutManager and GridLayoutManager)
 * - Thread-safe operations with proper scroll callback handling
 * - Memory management with automatic cache cleanup
 * <p>
 * Subclasses must implement abstract methods to define specific behavior for:
 * - View initialization
 * - Data conversion
 * - Adapter setup
 * - FAB visibility logic
 */
public abstract class BaseFragment extends Fragment implements
        NotifyUpdatesInterface,
        EventsRefreshInterface {

    // TAG
    private static final String TAG = "BaseFragment";

    // ID
    protected final String mFragmentId = this.toString();

    // NEW: Registration flag to avoid double registration
    private boolean mIsRegisteredForEventsRefresh = false;

    // NEW: Events data support
    protected Map<LocalDate, List<LocalEvent>> mEventsCache = new ConcurrentHashMap<>();
    protected final AtomicBoolean mIsLoadingEvents = new AtomicBoolean( false );
    protected QDueDatabase mDatabase;

    // DEBUG it is for initial scrolling bug. now resolved
    protected boolean mIsInitialScrolling = false;

    // Members
    protected CalendarDataManager mDataManager;
    protected RecyclerView mRecyclerView;
    protected GridLayoutManager mGridLayoutManager;
    protected FloatingActionButton mFabGoToToday;

    // ==================== INFINITE SCROLLING CACHE ==============

    // Cache of view items for infinite scrolling
    protected List<SharedViewModels.ViewItem> mItemsCache;

    // Current center position in cache
    protected int mCurrentCenterPosition;

    // Current date cursor
    protected LocalDate mCurrentDate;

    // ==================== CONCURRENCY CONTROL ===================

    // Atomic flag to prevent concurrent cache updates
    protected final AtomicBoolean mIsUpdatingCache = new AtomicBoolean( false );

    // Atomic flag for pending top load operations
    protected final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean( false );

    // Atomic flag for pending bottom load operations
    protected final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean( false );

    // ==================== ASYNC OPERATION HANDLERS ==============

    // Main thread handler for UI operations
    protected Handler mMainHandler;

    // Background handler for data operations
    protected Handler mBackgroundHandler;

    // ==================== POSITION TRACKING =====================

    // Position of today in the cache (-1 if not found)
    protected int mTodayPosition = -1;

    // ==================== LOADING INDICATORS ====================

    // Flag indicating top loader is currently showing
    protected boolean mShowingTopLoader = false;

    // Flag indicating bottom loader is currently showing
    protected boolean mShowingBottomLoader = false;

    // ==================== SCROLL VELOCITY CONTROL ===============

    // Last scroll timestamp for throttling
    protected long mLastScrollTime = 0;

    // Current scroll velocity for performance optimization
    protected int mScrollVelocity = 0;

    // Maximum scroll velocity before ignoring updates
    protected static final int MAX_SCROLL_VELOCITY = 25;

    // Delay before processing operations after scroll settles
    protected static final long SCROLL_SETTLE_DELAY = 150;

    // ==================== ENHANCED CONSTANTS ====================

    // Number of months to load in a single operation
    private static final int MONTHS_PER_LOAD = 3;

    // Preload trigger zone (in calendar weeks)
    private static final int PRELOAD_TRIGGER_WEEKS = 6; // ~1.5 months

    // Maximum number of months to keep in cache
    private static final int MAX_CACHED_MONTHS = 18; // ~1.5 years

    // ==================== INTERFACES INTERACTION ================

    //Reference to the communication interface
    protected FragmentCommunicationInterface communicationInterface;

    // =======================================================
    // === ABSTRACT METHODS THAT SUBCLASSES MUST IMPLEMENT ===
    // =======================================================

    /**
     * Find and setup views.
     */
    protected abstract void findViews(View rootView);

    /**
     * Convert data in fragment specific format.
     */
    protected abstract List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate);

    /**
     * Fragment specific adapter setup.
     * Subclass create a specific Adapter instance
     * which is given to the RecyclerView with mRecyclerView,setAdapter( adapter )
     */
    protected abstract void setupAdapter();

    /**
     * Get the current adapter.
     */
    protected abstract BaseAdapter getFragmentAdapter();

    /**
     * Set adapter, an extension of BaseAdapter
     */
    protected abstract void setFragmentAdapter(BaseAdapter adapter);

    /**
     * Abstract method for subclasses to specify column count.
     * DayslistViewFragment returns 1, CalendarViewFragment returns 7.
     */
    protected abstract int getGridColumnCount();

    // ============================================================

    /**
     * Initialize fragment components and handlers.
     * Sets up async handlers and data manager.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize handlers for async operations
        mMainHandler = new Handler( Looper.getMainLooper() );
        mBackgroundHandler = new Handler( Looper.getMainLooper() );

        // Initialize centralized data manager
        mDataManager = CalendarDataManager.getInstance();

        // Initialize events database
        mDatabase = QDueDatabase.getInstance( requireContext() );

        // Start initial events load
        loadEventsForCurrentPeriod();
    }

    /**
     * Complete view setup after inflation.
     * Coordinates view finding, RecyclerView setup, and infinite scrolling initialization.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        // Subclasses must implement findViews() to initialize their specific views
        findViews( view );

        // Configure the RecyclerView
        setupRecyclerView();

        // Initialize infinite scrolling
        if (getActivity() != null) {
            setupInfiniteScrolling();
        } else {
            Log.e( TAG, "Activity is null, cannot initialize infinite scrolling" );
        }

        // Configure the TODAY fab button
        setupScrollToToday();
    }

    // ==================== COMMUNICATION INTERFACE ===============

    /**
     * Get reference to communication interface during attachment.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach( context );

        try {
            // Get reference to activity's communication interface
            communicationInterface = (FragmentCommunicationInterface) context;
            Log.v( TAG, "Communication interface attached" );
        } catch (ClassCastException e) {
            Log.e( TAG, "Error in attaching Communication Interface" );
            throw new ClassCastException( context
                    + " must implement FragmentCommunicationInterface" );
        }
    }

    /**
     * Clear reference when fragment is detached.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        communicationInterface = null;
        Log.v( TAG, "Communication interface detached" );
    }

    // ==================== RECYCLER ===========================

    /**
     * Setup RecyclerView with unified GridLayoutManager approach.
     * Both DayslistViewFragment (1) and CalendarViewFragment (7) will use GridLayoutManager.
     * This unifies scroll handling, infinite loading, and sticky header logic.
     */
    protected void setupRecyclerView() {

        if (mRecyclerView == null) {
            Log.e( TAG, "RecyclerView is null - subclass must implement findViews()" );
            return;
        }

        // Get column count from subclass
        int columnCount = getGridColumnCount();

        // Create unified GridLayoutManager
        GridLayoutManager gridLayoutManager = getGridLayoutManager( columnCount );

        mRecyclerView.setLayoutManager( gridLayoutManager );

        // Store reference for position calculations
        mGridLayoutManager = gridLayoutManager;

        // Apply common optimizations
        mRecyclerView.setHasFixedSize( true );
        mRecyclerView.setItemAnimator( null );

        Log.d( TAG, MessageFormat.format(
                "Configured GridLayoutManager with {0} columns", columnCount ) );
    }

    @NonNull
    private GridLayoutManager getGridLayoutManager(int columnCount) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager( getContext(), columnCount );

        // Configure span size for headers and loading items
        gridLayoutManager.setSpanSizeLookup( new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < mItemsCache.size()) {

                    SharedViewModels.ViewItem item = mItemsCache.get( position );

                    // Headers and loading items always span full width
                    if (item instanceof SharedViewModels.MonthHeader ||
                            item instanceof SharedViewModels.LoadingItem) {
                        return columnCount; // Full width
                    }
                }
                return 1; // Regular items take 1 column
            }
        } );
        return gridLayoutManager;
    }

    // ============================================================

    /**
     * Unified scroll listener for both fragment types.
     * Handles infinite scrolling and sticky header updates.
     */
    private class UnifiedGridScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled( recyclerView, dx, dy );

            long currentTime = System.currentTimeMillis();

            // Calculate scroll velocity for performance optimization
            if (mLastScrollTime > 0) {
                long timeDiff = currentTime - mLastScrollTime;
                if (timeDiff > 0) {
                    mScrollVelocity = (int) (Math.abs( dy ) / timeDiff * 16); // Normalized to 60fps
                }
            }

            // Throttling to avoid too frequent updates
            if (currentTime - mLastScrollTime < 100) return;
            mLastScrollTime = currentTime;

            // Skip updates if scrolling too fast
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) return;

            // Get visible positions using GridLayoutManager
            int firstVisible = getFirstVisiblePosition();
            int lastVisible = getLastVisiblePosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Handle infinite scrolling
            mMainHandler.post( () -> handleUnifiedScrollBasedLoading( firstVisible, lastVisible, dy ) );

            // Update FAB visibility
            updateFabVisibility( firstVisible, lastVisible );

            // Update sticky header in toolbar
            updateStickyHeader( firstVisible, lastVisible );
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged( recyclerView, newState );

            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;

                // Process pending operations after scroll stops
                mMainHandler.postDelayed( () -> {
                    if (mScrollVelocity == 0) {
                        processPendingOperations();
                        scheduleCleanupIfNeeded();
                    }
                }, SCROLL_SETTLE_DELAY );
            }
        }
    }

    /**
     * Unified scroll-based loading logic that works for both 1-column and 7-column grids.
     */
    private void handleUnifiedScrollBasedLoading(int firstVisible,
                                                 int lastVisible,
                                                 int scrollDirection) {
        // Conservative trigger zones
        int loadTriggerZone = getLoadTriggerZone();

        // Load upward (previous months) when near top
        if (firstVisible <= loadTriggerZone && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {
            Log.d( TAG, "Triggering top load at position: " + firstVisible );

            mMainHandler.postDelayed( () -> {
                if (!mIsUpdatingCache.get() && !mIsPendingTopLoad.get()) {
                    triggerTopLoad();
                }
            }, 100 );
        }

        // Load downward (next months) when near bottom
        if (lastVisible >= mItemsCache.size() - loadTriggerZone && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {
            Log.d( TAG, "Triggering bottom load at position: " + lastVisible );

            mMainHandler.postDelayed( () -> {
                if (!mIsUpdatingCache.get() && !mIsPendingBottomLoad.get()) {
                    triggerBottomLoad();
                }
            }, 100 );
        }
    }

    /**
     * Get load trigger zone based on column count.
     * More conservative for calendar view due to grid density.
     */
    private int getLoadTriggerZone() {
        int columnCount = getGridColumnCount();
        return columnCount == 1 ? 10 : 21; // 10 items for list, 21 items (~3 weeks) for calendar
    }

    /**
     * Update sticky header in toolbar based on currently visible month.
     */
    private void updateStickyHeader(int firstVisible,
                                    int lastVisible) {

        // Find the month header that's currently most visible
        LocalDate currentMonth = findCurrentVisibleMonth( firstVisible, lastVisible );

        if (currentMonth != null) {
            updateToolbarTitle( currentMonth );
        }
    }

    /**
     * Find which month is currently most visible in the viewport.
     */
    private LocalDate findCurrentVisibleMonth(int firstVisible,
                                              int lastVisible) {
        // Find the first month header in the visible range
        for (int i = firstVisible; i <= lastVisible && i < mItemsCache.size(); i++) {
            SharedViewModels.ViewItem item = mItemsCache.get( i );

            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }

            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }

        // Fallback: search backwards from first visible
        for (int i = firstVisible - 1; i >= 0; i--) {
            SharedViewModels.ViewItem item = mItemsCache.get( i );

            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
        }

        return null;
    }

    /**
     * Update toolbar title with formatted month name.
     * Shows only month name for current year, "Month Year" for other years.
     */
    private void updateToolbarTitle(LocalDate monthDate) {
        if (getActivity() == null) return;

        try {
            LocalDate today = LocalDate.now();
            String formattedTitle;

            if (monthDate.getYear() == today.getYear()) {
                // Current year: show only month name
                formattedTitle = monthDate.format( DateTimeFormatter.ofPattern( "MMMM", Locale.getDefault() ) );
            } else {
                // Different year: show month and year
                formattedTitle = monthDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy", QDue.getLocale() ) );
            }

            // Update toolbar title - this will be implemented in the activity
            if (communicationInterface != null) {
                Bundle data = new Bundle();
                data.putString( "title", formattedTitle );
                communicationInterface.onFragmentCustomAction( "update_toolbar_title", data );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error updating toolbar title", e );
        }
    }

    /**
     * Infinite scrolling
     * FIXED: Setup infinite scrolling with proper initial positioning
     */
    protected void setupInfiniteScrolling() {
        try {
            // Reset all control flags
            resetControlFlags();

            // Initialize cache
            mCurrentDate = QDue.getQuattrodue().getCursorDate();
            mItemsCache = new ArrayList<>();

            // Pre-load months in cache around current date
            mDataManager.preloadMonthsAround( mCurrentDate, QD_MONTHS_CACHE_RADIUS );

            // Generate initial months for display
            // Here the items are added to mItemsCache
            for (int i = -QD_MONTHS_CACHE_RADIUS; i <= QD_MONTHS_CACHE_RADIUS; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths( i );
                addMonthToCache( monthDate );
            }

            setupAdapter();
        } catch (Exception e) {
            Log.e( TAG, "Error during infinite scrolling setup", e );
            return;
        }

        try {
            // Check
            if (!areComponentsReady()) {
                Log.e( TAG, "Components not ready, deferring infinite scroll setup" );
                scheduleInfiniteScrollingSetup();
                return;
            } else {
                Log.d( TAG, "Components ready, starting infinite scroll setup" );
            }

            // 1. Calcola la posizione di today
            mTodayPosition = SharedViewModels.DataConverter.findTodayPosition( mItemsCache );

            if (DEBUG_BASEFRAGMENT) {
                Log.d( TAG, "Today position calculated: " + mTodayPosition );
                Log.d( TAG, "Cache size: " + (mItemsCache != null ? mItemsCache.size() : "null") );
                Log.d( TAG, "Adapter items: " + (getFragmentAdapter() != null ? getFragmentAdapter().getItemCount() : "null") );
            }

            // 2. Forza sincronizzazione cache-adapter
            ensureAdapterSyncWithCache();

            // 3. Scroll ritardato con verifica
            scheduleVerifiedScrollToToday();

            // 4. Setup infinite scroll listeners
            setupScrollListeners();

            if (DEBUG_BASEFRAGMENT) {
                Log.d( TAG, "Infinite scroll setup completed: " + mItemsCache.size() +
                        " elements, today at position: " + mTodayPosition );
            }
        } catch (Exception e) {
            Log.e( "TAG", "Error in setupInfiniteScrolling", e );
        }
    }

    // ===== Check Prerequisites =====

    private boolean areComponentsReady() {

        // Here every fragment implements its own adapter, so we ask for it
        if (getFragmentAdapter() == null) {
            Log.e( TAG, "getFragmentAdapter() returns null" );
            return false;
        }

        if (mItemsCache == null) {
            Log.e( TAG, "mmItemsCache is null" );
            return false;
        }

        if (mRecyclerView == null) {
            Log.e( TAG, "mRecyclerView is null" );
            return false;
        }

        if (mGridLayoutManager == null) {
            Log.e( TAG, "mGridLayoutManager is null" );
            return false;
        }

        return true;
    }

    // ===== Forced sync =====

    /**
     * Ensure cache and adapter are in sync.
     */
    protected void ensureAdapterSyncWithCache() {

        if (getFragmentAdapter() == null) {
            return;
        }

        if (mItemsCache == null) {
            return;
        }

        try {
            // Check
            if (getFragmentAdapter().getItemCount() == 0 && !mItemsCache.isEmpty()) {

                // Populate the adapter with cached data
                getFragmentAdapter().setItems( new ArrayList<>( mItemsCache ) );
            } else if (getFragmentAdapter().getItemCount() != mItemsCache.size()) {

                getFragmentAdapter().setItems( new ArrayList<>( mItemsCache ) );
            } else {

                Log.d( TAG, MessageFormat.format(
                        "Cache and adapter already in sync: {0} items", mItemsCache.size() ) );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error in cache synchronization", e );
        }
    }

    /**
     * Schedule infinite scrolling setup after a delay.
     */
    private void scheduleInfiniteScrollingSetup() {
        // Retry every 100ms
        mRecyclerView.postDelayed( new Runnable() {
            private int attempts = 0;

            @Override
            public void run() {
                attempts++;

                // 5 secondi max
                int MAX_ATTEMPTS = 50;
                if (areComponentsReady()) {
                    setupInfiniteScrolling();
                } else if (attempts < MAX_ATTEMPTS) {
                    mRecyclerView.postDelayed( this, 100 );
                } else {
                    Log.e( TAG, MessageFormat.format(
                            "Failed to setup infinite scrolling after {0} attempts", MAX_ATTEMPTS ) );
                }
            }
        }, 100 );
    }

    /**
     * Scroll with check
     */
    private void scheduleVerifiedScrollToToday() {
        if (mTodayPosition < 0) {
            return;
        }

        if (mRecyclerView == null) {
            Log.e( TAG, "mRecyclerView is null, cannot schedule scroll" );
            return;
        }

        // Check if the adapter has enough elements
        if (getFragmentAdapter().getItemCount() > mTodayPosition) {
            // Data ready - immediate scroll (little delay for the layout)
            mRecyclerView.postDelayed( this::performVerifiedScrollToToday, 100 );
        } else {
            // Adapter non ancora pronto - usa retry con timeout
            scheduleRetryScrollToToday( 0 );
        }
    }

    /**
     * Retry scroll to today
     *
     * @param attemptCount mu number of attempts
     */
    private void scheduleRetryScrollToToday(int attemptCount) {
        final int MAX_ATTEMPTS = 5; // old - 20 * 50ms = 1 secondo max

        if (attemptCount >= MAX_ATTEMPTS) {
            Log.e( TAG, "Failed to scroll to today after " + MAX_ATTEMPTS + " attempts" );
            return;
        }

        mRecyclerView.postDelayed( () -> {
            if (getFragmentAdapter().getItemCount() > mTodayPosition) {
                Log.d( TAG, MessageFormat.format(
                        "Adapter ready after {0} attempts", attemptCount + 1 ) );
                performVerifiedScrollToToday();
            } else {
                Log.d( TAG, MessageFormat.format(
                        "Retry {0}/{1} - Adapter: {2}, Need: {3}",
                        attemptCount + 1,
                        MAX_ATTEMPTS,
                        getFragmentAdapter().getItemCount(),
                        mTodayPosition + 1 ) );
                scheduleRetryScrollToToday( attemptCount + 1 );
            }
        }, 50 ); // Retry ogni 50ms
    }

    /**
     * Perform verified scroll to today
     */
    private void performVerifiedScrollToToday() {
        try {
            // Final check before scrolling
            if (mTodayPosition < 0 || mTodayPosition >= getFragmentAdapter().getItemCount()) {
                Log.e( TAG, MessageFormat.format( "Invalid today position: {0} (adapter size: {1})",
                        mTodayPosition,
                        getFragmentAdapter().getItemCount() ) );
                return;
            }

            // temporarily disable infinite loading
            mIsInitialScrolling = true;

            if (mGridLayoutManager != null) {
                GridLayoutManager gridManager = mGridLayoutManager;

                // Calculate offset
                int offset = getGridColumnCount() == 1 ? 11 : 21;

                // Calculate optimal position
                int optimalPosition = Math.max( 0, mTodayPosition - offset );
                gridManager.scrollToPositionWithOffset( optimalPosition, 20 );
            } else {
                // Fallback with LinearLayoutManager?
                mRecyclerView.scrollToPosition( Math.max( 0, mTodayPosition - 3 ) );
                Log.e( TAG, MessageFormat.format(
                        "Using fallback mRecyclerView positioning for today: {0}", mTodayPosition ) );
            }

            // Enable infinite loading after a delay
            mRecyclerView.postDelayed( () -> {
                mIsInitialScrolling = false;
                Log.d( TAG, "Initial scroll completed, infinite loading re-enabled" );
            }, 500 );
        } catch (Exception e) {
            Log.e( TAG, "Error performing verified scroll", e );
            mIsInitialScrolling = false;
        }
    }

    // ============================================================

    /**
     * Setup scroll listeners separately for better organization
     * set to protected instead of private
     */
    protected void setupScrollListeners() {
        if (mRecyclerView != null) {
            //mRecyclerView.addOnScrollListener( new debugScrollListener() );
            mRecyclerView.clearOnScrollListeners();
            mRecyclerView.addOnScrollListener( new UnifiedGridScrollListener() );
        }
    }

    /**
     * Updated position methods to work with GridLayoutManager.
     */
    protected int getFirstVisiblePosition() {
        if (mGridLayoutManager != null) {
            return mGridLayoutManager.findFirstVisibleItemPosition();
        }
        return RecyclerView.NO_POSITION;
    }

    protected int getLastVisiblePosition() {
        if (mGridLayoutManager != null) {
            return mGridLayoutManager.findLastVisibleItemPosition();
        }
        return RecyclerView.NO_POSITION;
    }

    public void scrollToToday() {
        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            if (mGridLayoutManager != null) {
                //mRecyclerView.smoothScrollToPosition( mTodayPosition );
                performVerifiedScrollToToday();
                Log.d( TAG, MessageFormat.format( "Scrolling to today (position: {0})", mTodayPosition ) );
            }
        } else {
            // Rebuild cache centered on today
            mCurrentDate = LocalDate.now().withDayOfMonth( 1 );
            setupInfiniteScrolling();
        }
    }

    /**
     * Maintain scroll position for GridLayoutManager.
     *
     * @param itemsAdded number of items added at the beginning
     */

    private void maintainScrollPosition(int itemsAdded) {
        if (mGridLayoutManager != null) {
            try {
                mGridLayoutManager.scrollToPositionWithOffset( itemsAdded, 0 );
            } catch (Exception e) {
                Log.e( TAG, "Error maintaining scroll position", e );
            }
        }
    }

    /**
     * Setup scrolling to TODAY position separately for better organization
     */
    protected void setupScrollToToday() {
        setupFAB();
    }

    /**
     * Enhanced FAB setup that works with both integrated and separate FAB modes.
     * Add this method to BaseFragment.java
     */
    protected void setupFAB() {
        if (getActivity() instanceof QDueMainActivity) {
            mFabGoToToday = ((QDueMainActivity) getActivity()).getFabGoToToday();

            if (mFabGoToToday != null) {
                mFabGoToToday.setOnClickListener( v -> scrollToToday() );
            } else {
                // Riprova dopo che l'Activity ha finito l'inizializzazione
                mRecyclerView.post( this::retrySetupFAB );
            }
        }
    }

    private void retrySetupFAB() {
        if (mFabGoToToday == null && getActivity() instanceof QDueMainActivity) {
            mFabGoToToday = ((QDueMainActivity) getActivity()).getFabGoToToday();
            if (mFabGoToToday != null) {
                mFabGoToToday.setOnClickListener( v -> scrollToToday() );
            }
        }
    }

    /**
     * Enhanced FAB visibility update that respects integrated mode.
     */
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        // Logica di visibilità standard
        boolean showFab = true;
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        // Applica visibilità
        if ((showFab && mFabGoToToday.getVisibility() != View.VISIBLE)
                || (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE)) {
            toggleFabVisibility( mFabGoToToday );
        }
    }

    /**
     * Toggle FAB visibility.
     */
    protected static void toggleFabVisibility(@NonNull FloatingActionButton fab) {
        if (fab.getVisibility() != View.VISIBLE) {
            fab.setVisibility( View.VISIBLE );
            fab.animate()
                    .alpha( 1f )
                    .scaleX( 1f )
                    .scaleY( 1f )
                    .setDuration( 200 )
                    .start();
            fab.show();
        } else {
            fab.animate()
                    .alpha( 0f )
                    .scaleX( 0f )
                    .scaleY( 0f )
                    .setDuration( 200 )
                    .withEndAction( () -> fab.setVisibility( View.GONE ) )
                    .start();
            fab.hide();
        }
    }

    /**
     * Execute top load operation (loading previous months).
     * <p>
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     * FIXED: Execute top load operation with proper error handling and debugging
     * ENHANCED: Execute top load operation - loads multiple months at once
     * ENHANCED: Better today position updates during cache operations
     */
    protected void executeTopLoad() {
        if (!mIsPendingTopLoad.compareAndSet( true, false )) {
            Log.w( TAG, "No pending top load - aborting" );
            hideTopLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet( false, true )) {
            Log.w( TAG, "Cache update in progress - aborting" );
            mIsPendingTopLoad.set( true );
            return;
        }

        Log.d( TAG, "Starting multi-month top load execution" );

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                Log.e( TAG, "Cannot find first month in cache" );
                mIsUpdatingCache.set( false );
                hideTopLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths( i );

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems( monthToLoad );
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll( 0, monthItems );
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w( TAG, "No items generated for any previous months" );
                mIsUpdatingCache.set( false );
                hideTopLoader();
                return;
            }

            mMainHandler.post( () -> {
                try {
                    if (mItemsCache == null) {
                        Log.e( TAG, "Items cache is null during UI update" );
                        return;
                    }

                    mItemsCache.addAll( 0, allNewItems );

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition >= 0) {
                        mTodayPosition += allNewItems.size();
                    } else {
                        // Today position was lost - try to find it again
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition( mItemsCache );
                    }

                    mCurrentCenterPosition += allNewItems.size();

                    mMainHandler.post( () -> {
                        try {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted( 0, allNewItems.size() );
                            }
                        } catch (Exception e) {
                            Log.e( TAG, "Error notifying adapter", e );
                        }
                    } );

                    maintainScrollPosition( allNewItems.size() );

                    mMainHandler.postDelayed( () -> {
                        hideTopLoader();
                        Log.d( TAG, "Multi-month top load completed successfully" );
                        schedulePreloadingCheck();
                    }, 100 );
                } catch (Exception e) {
                    Log.e( TAG, "Error during UI update: " + e.getMessage() );
                    hideTopLoader();
                } finally {
                    mIsUpdatingCache.set( false );
                }
            } );
        } catch (Exception e) {
            Log.e( TAG, "Error loading previous months: " + e.getMessage() );
            mIsUpdatingCache.set( false );
            hideTopLoader();
        }
    }

    /**
     * Execute bottom load operation (loading next months).
     * <p>
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     * FIXED: Execute bottom load operation with proper error handling and debugging
     * ENHANCED: Execute bottom load operation - loads multiple months at once
     * ENHANCED: Better today position updates during bottom loading
     */
    protected void executeBottomLoad() {

        if (!mIsPendingBottomLoad.compareAndSet( true, false )) {
            Log.w( TAG, "No pending bottom load - aborting" );
            hideBottomLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet( false, true )) {
            Log.w( TAG, "Cache update in progress - aborting" );
            mIsPendingBottomLoad.set( true );
            return;
        }

        Log.d( TAG, "Starting multi-month bottom load execution" );

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                Log.e( TAG, "Cannot find last month in cache" );
                mIsUpdatingCache.set( false );
                hideBottomLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths( i );
                Log.d( TAG, "Loading next month " + i + ": " + monthToLoad );

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems( monthToLoad );
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll( monthItems );
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w( TAG, "No items generated for any next months" );
                mIsUpdatingCache.set( false );
                hideBottomLoader();
                return;
            }

            mMainHandler.post( () -> {
                try {
                    if (mItemsCache == null) {
                        Log.e( TAG, "Items cache is null during UI update" );
                        return;
                    }

                    int insertPos = mItemsCache.size();
                    mItemsCache.addAll( allNewItems );

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition < 0) {
                        // Today position not set - try to find it in the expanded cache
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition( mItemsCache );
                    }

                    mMainHandler.post( () -> {
                        try {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted( insertPos, allNewItems.size() );
                            }
                        } catch (Exception e) {
                            Log.e( TAG, "Error notifying adapter", e );
                        }
                    } );

                    mMainHandler.postDelayed( () -> {
                        hideBottomLoader();
                        schedulePreloadingCheck();
                    }, 100 );
                } catch (Exception e) {
                    Log.e( TAG, "Error during UI update", e );
                    hideBottomLoader();
                } finally {
                    mIsUpdatingCache.set( false );
                }
            } );
        } catch (Exception e) {
            Log.e( TAG, "Error loading next months", e );
            mIsUpdatingCache.set( false );
            hideBottomLoader();
        }
    }

    /**
     * NEW: Schedule preloading check to maintain buffer zones
     */
    private void schedulePreloadingCheck() {
        // Schedule preloading check after a short delay
        mMainHandler.postDelayed( () -> {
            if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                performPreloadingCheck();
            }
        }, 500 ); // Check after scrolling settles
    }

    /**
     * NEW: Perform silent preloading to maintain buffer zones
     */
    private void performPreloadingCheck() {
        if (mIsUpdatingCache.get()) return;

        // Get current scroll position
        int currentPosition = getCurrentVisiblePosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            return;
        }

        int cacheSize = mItemsCache.size();
        int bufferZone = PRELOAD_TRIGGER_WEEKS * 7; // Convert weeks to items

        // Check if we need to preload at the top
        if (currentPosition < bufferZone && !mIsPendingTopLoad.get()) {
            triggerSilentTopLoad();
        }

        // Check if we need to preload at the bottom
        if (currentPosition > cacheSize - bufferZone && !mIsPendingBottomLoad.get()) {
            triggerSilentBottomLoad();
        }

        // Check if cache is too large and needs cleanup
        if (cacheSize > MAX_CACHED_MONTHS * 35) { // ~35 items per month
            scheduleCleanupIfNeeded();
        }
    }

    /**
     * NEW: Silent top load without visible loader
     */
    private void triggerSilentTopLoad() {
        if (mIsUpdatingCache.get() || mIsPendingTopLoad.get()) {
            return;
        }

        mIsPendingTopLoad.set( true );

        // Execute without showing loader
        mBackgroundHandler.postDelayed( this::executeSilentTopLoad, 100 );
    }

    /**
     * NEW: Silent bottom load without visible loader
     */
    private void triggerSilentBottomLoad() {
        if (mIsUpdatingCache.get() || mIsPendingBottomLoad.get()) {
            return;
        }

        Log.d( TAG, "Starting silent bottom load" );
        mIsPendingBottomLoad.set( true );

        // Execute without showing loader
        mBackgroundHandler.postDelayed( this::executeSilentBottomLoad, 100 );
    }

    /**
     * NEW: Execute silent top load (similar to executeTopLoad but without loader UI)
     */
    private void executeSilentTopLoad() {
        if (!mIsPendingTopLoad.compareAndSet( true, false )) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet( false, true )) {
            mIsPendingTopLoad.set( true );
            return;
        }

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                mIsUpdatingCache.set( false );
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths( i );
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems( monthToLoad );
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll( 0, monthItems );
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post( () -> {
                    try {
                        mItemsCache.addAll( 0, allNewItems );
                        if (mTodayPosition >= 0) mTodayPosition += allNewItems.size();
                        mCurrentCenterPosition += allNewItems.size();

                        mMainHandler.post( () -> {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted( 0, allNewItems.size() );
                            }
                        } );

                        maintainScrollPosition( allNewItems.size() );
                    } finally {
                        mIsUpdatingCache.set( false );
                    }
                } );
            } else {
                mIsUpdatingCache.set( false );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error in silent top load", e );
            mIsUpdatingCache.set( false );
        }
    }

    /**
     * NEW: Execute silent bottom load (similar to executeBottomLoad but without loader UI)
     */
    private void executeSilentBottomLoad() {
        if (!mIsPendingBottomLoad.compareAndSet( true, false )) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet( false, true )) {
            mIsPendingBottomLoad.set( true );
            return;
        }

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                mIsUpdatingCache.set( false );
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths( i );
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems( monthToLoad );
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll( monthItems );
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post( () -> {
                    try {
                        int insertPos = mItemsCache.size();
                        mItemsCache.addAll( allNewItems );

                        if (mTodayPosition < 0) {
                            int todayInNew = SharedViewModels.DataConverter.findTodayPosition( allNewItems );
                            if (todayInNew >= 0) {
                                mTodayPosition = insertPos + todayInNew;
                            }
                        }

                        mMainHandler.post( () -> {
                            if (getFragmentAdapter() != null) {
                                getFragmentAdapter().notifyItemRangeInserted( insertPos, allNewItems.size() );
                            }
                        } );
                    } finally {
                        mIsUpdatingCache.set( false );
                    }
                } );
            } else {
                mIsUpdatingCache.set( false );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error in silent bottom load", e );
            mIsUpdatingCache.set( false );
        }
    }

    /**
     * Trigger top load operation (previous months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger top load with better state management
     */
    protected void triggerTopLoad() {
        if (mIsInitialScrolling) return;

        // NULL CHECK aggiunto
        if (mDataManager == null) {
            Log.e( TAG, "mDataManager is null" );
            return;
        }

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w( TAG, "Cache update in progress - ignoring trigger" );
            return;
        }

        if (mIsPendingTopLoad.get()) {
            Log.w( TAG, "Top load already pending - ignoring trigger" );
            return;
        }

        if (mShowingTopLoader) {
            Log.w( TAG, "Top loader already showing - ignoring trigger" );
            return;
        }

        mIsPendingTopLoad.set( true );
        showTopLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed( this::executeTopLoad, 200 ); // Increased delay to ensure loader visibility
    }

    /**
     * Trigger bottom load operation (next months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger bottom load with better state management
     */
    protected void triggerBottomLoad() {
        if (mIsInitialScrolling) return; // SKIP durante scroll iniziale

        // NULL CHECK aggiunto
        if (mDataManager == null) {
            Log.e( TAG, "mDataManager is null" );
            return;
        }

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w( TAG, "Cache update in progress - ignoring trigger" );
            return;
        }

        if (mIsPendingBottomLoad.get()) {
            Log.w( TAG, "Bottom load already pending - ignoring trigger" );
            return;
        }

        if (mShowingBottomLoader) {
            Log.w( TAG, "Bottom loader already showing - ignoring trigger" );
            return;
        }

        mIsPendingBottomLoad.set( true );
        showBottomLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed( this::executeBottomLoad, 200 ); // Increased delay to ensure loader visibility
    }

    /**
     * Generate view items for a specific month using the fragment-specific converter.
     *
     * @param monthDate the month to generate items for
     * @return list of view items for the month
     */
    private List<SharedViewModels.ViewItem> generateMonthItems(LocalDate monthDate) {
        List<Day> monthDays = mDataManager.getMonthDays( monthDate );
        return convertMonthData( monthDays, monthDate );
    }

    /**
     * Process pending operations when scroll stops.
     * Executes any pending load operations that were deferred during scrolling.
     */
    protected void processPendingOperations() {
        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            executeTopLoad();
        }
        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            executeBottomLoad();
        }
    }

    /**
     * Schedule cache cleanup if cache size exceeds limits.
     * Prevents memory issues by cleaning old cache entries when cache grows too large.
     */
    protected void scheduleCleanupIfNeeded() {
        int maxElements = QD_MAX_CACHE_SIZE * 35; // ~35 elements per month
        if (mItemsCache.size() > maxElements) {
            mBackgroundHandler.postDelayed( () -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000 );
        }
    }

    /**
     * Clean up cache by removing elements far from current position.
     * Works with both LinearLayoutManager and GridLayoutManager.
     */
    private void cleanupCache() {
        if (!mIsUpdatingCache.compareAndSet( false, true )) return;

        mMainHandler.post( () -> {
            try {
                // Get current position safely for any LayoutManager type
                int currentPos = getCurrentVisiblePosition();
                int targetSize = QD_MONTHS_CACHE_RADIUS * 35;

                // If we can't determine position, use a safe fallback
                if (currentPos == RecyclerView.NO_POSITION) {
                    currentPos = mItemsCache.size() / 2; // Use middle as fallback
                    if (DEBUG_BASEFRAGMENT)
                        Log.w( TAG, "Using fallback position for cleanup" );
                }

                // Remove from start if necessary
                while (mItemsCache.size() > targetSize && currentPos > targetSize / 2) {
                    mItemsCache.remove( 0 );
                    currentPos--;
                    if (mTodayPosition > 0) mTodayPosition--;
                    if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;

                    // POST adapter notification to next frame
                    mMainHandler.post( () -> {
                        if (getFragmentAdapter() != null) {
                            getFragmentAdapter().notifyItemRemoved( 0 );
                        }
                    } );
                }

                // Remove from end if necessary
                while (mItemsCache.size() > targetSize) {
                    mItemsCache.size();
                    int lastIndex = mItemsCache.size() - 1;
                    mItemsCache.remove( lastIndex );

                    // POST adapter notification to next frame
                    final int finalLastIndex = lastIndex;
                    mMainHandler.post( () -> {
                        if (getFragmentAdapter() != null) {
                            getFragmentAdapter().notifyItemRemoved( finalLastIndex );
                        }
                    } );
                }
            } finally {
                mIsUpdatingCache.set( false );
            }
        } );
    }

    /**
     * Get current visible position safely for any LayoutManager type.
     * Handles both LinearLayoutManager and GridLayoutManager with fallbacks.
     *
     * @return current visible position or NO_POSITION if not determinable
     */
    private int getCurrentVisiblePosition() {
        if (mRecyclerView == null) return RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;

        try {
            // Handle LinearLayoutManager (DaysListFragment)
            if (layoutManager instanceof LinearLayoutManager linearManager) {
                int firstVisible = linearManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    return firstVisible;
                }
                // Fallback to partially visible
                return linearManager.findFirstVisibleItemPosition();
            }

            // Handle GridLayoutManager (CalendarFragment)
            else {// Try to get first visible child
                View firstChild = layoutManager.getChildAt( 0 );
                if (firstChild != null) {
                    return mRecyclerView.getChildAdapterPosition( firstChild );
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "Error getting current position: " + e.getMessage() );
        }

        return RecyclerView.NO_POSITION;
    }

    // =============== LOADING INDICATOR MANAGEMENT ===============

    /**
     * Show top loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showTopLoader with more debugging
     */
    private void showTopLoader() {
        if (mShowingTopLoader) {
            Log.w( TAG, "Top loader already showing - ignoring request" );
            return;
        }

        mShowingTopLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.TOP );
        mItemsCache.add( 0, loader );

        if (mTodayPosition >= 0) mTodayPosition++;
        mCurrentCenterPosition++;

        Log.w( TAG, "Added top loader - cache size now: " + mItemsCache.size() );

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post( () -> {
            if (getFragmentAdapter() != null) {
                getFragmentAdapter().notifyItemInserted( 0 );
                Log.w( TAG, "Notified adapter of top loader insertion" );
            }
        } );
    }

    /**
     * Show bottom loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showBottomLoader with more debugging
     */
    private void showBottomLoader() {
        if (mShowingBottomLoader) {
            Log.w( TAG, "Bottom loader already showing - ignoring request" );
            return;
        }

        mShowingBottomLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.BOTTOM );
        mItemsCache.add( loader );

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post( () -> {
            if (getFragmentAdapter() != null) {
                getFragmentAdapter().notifyItemInserted( mItemsCache.size() - 1 );
                Log.w( TAG, "Notified adapter of bottom loader insertion" );
            }
        } );
    }

    /**
     * FIXED: Hide top loader with better error handling
     */
    private void hideTopLoader() {
        if (!mShowingTopLoader) {
            Log.d( TAG, "Top loader not showing - nothing to hide" );
            return;
        }

        try {
            for (int i = 0; i < mItemsCache.size(); i++) {
                if (mItemsCache.get( i ) instanceof SharedViewModels.LoadingItem loading) {
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.TOP) {
                        mItemsCache.remove( i );
                        if (mTodayPosition > i) mTodayPosition--;
                        if (mCurrentCenterPosition > i) mCurrentCenterPosition--;

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post( () -> {
                            try {
                                if (getFragmentAdapter() != null) {
                                    getFragmentAdapter().notifyItemRemoved( finalI );
                                }
                            } catch (Exception e) {
                                Log.e( TAG, "Error notifying adapter of loader removal: " + e.getMessage() );
                            }
                        } );
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "Error hiding top loader", e );
        } finally {
            mShowingTopLoader = false;
        }
    }

    /**
     * Hide bottom loading indicator.
     * Removes loading item from cache and notifies adapter on next frame.
     */
    private void hideBottomLoader() {
        if (!mShowingBottomLoader) return;

        try {
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                if (mItemsCache.get( i ) instanceof SharedViewModels.LoadingItem loading) {
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.BOTTOM) {
                        mItemsCache.remove( i );

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post( () -> {
                            try {
                                if (getFragmentAdapter() != null) {
                                    getFragmentAdapter().notifyItemRemoved( finalI );
                                }
                            } catch (Exception e) {
                                Log.e( TAG, "Error notifying adapter of loader removal", e );
                            }
                        } );
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "Error hiding bottom loader", e );
        } finally {
            mShowingBottomLoader = false;
        }
    }

    // ======================== HELPER METHODS ==========================

    /**
     * Reset all control flags to initial state.
     * Used during initialization and cleanup.
     * DEBUG: made protected insteadof private
     */
    protected void resetControlFlags() {
        mIsUpdatingCache.set( false );
        mIsPendingTopLoad.set( false );
        mIsPendingBottomLoad.set( false );
        mShowingTopLoader = false;
        mShowingBottomLoader = false;
    }

    /**
     * Find the first month date in the cache.
     * Used for determining what previous month to load.
     *
     * @return first month date or null if cache is empty
     */
    private LocalDate findFirstMonthInCache() {
        for (SharedViewModels.ViewItem item : mItemsCache) {
            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }
        return null;
    }

    /**
     * Find the last month date in the cache.
     * Used for determining what next month to load.
     *
     * @return last month date or null if cache is empty
     */
    private LocalDate findLastMonthInCache() {
        for (int i = mItemsCache.size() - 1; i >= 0; i--) {
            SharedViewModels.ViewItem item = mItemsCache.get( i );
            if (item instanceof SharedViewModels.MonthHeader) {
                return ((SharedViewModels.MonthHeader) item).monthDate;
            }
            if (item instanceof SharedViewModels.DayItem) {
                return ((SharedViewModels.DayItem) item).monthDate;
            }
        }
        return null;
    }

    /// === LIFECYCLE MANAGEMENT ===

    /**
     * Handle fragment start lifecycle.
     * Updates preferences and triggers refresh if needed.
     */
    @Override
    public void onStart() {
        super.onStart();

        if (QDue.getQuattrodue() != null) {
            QDue.getQuattrodue().updatePreferences( requireActivity() );
            if (QDue.getQuattrodue().isRefresh()) {
                QDue.getQuattrodue().setRefresh( false );
                refreshData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check for lazy refresh in base implementation
        if (shouldPerformBaseLazyRefresh()) {
            Log.d( TAG, String.format( "Performing lazy refresh for %s", getClass().getSimpleName() ) );
            onForceEventsRefresh();
        }

        // Register when fragment is visible
        registerForEventsRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister when fragment is not visible
        unregisterForEventsRefresh();
    }

    /**
     * Register this fragment for events refresh notifications
     */
    private void registerForEventsRefresh() {
        if (!mIsRegisteredForEventsRefresh && getActivity() instanceof QDueMainActivity mainActivity) {

            // Register
            mainActivity.registerEventsRefreshFragment( this );

            mIsRegisteredForEventsRefresh = true;
            Log.d( TAG, String.format( "Registered %s for events refresh", mFragmentId ) );
        }
    }

    /**
     * Unregister this fragment from events refresh notifications
     */
    private void unregisterForEventsRefresh() {
        if (mIsRegisteredForEventsRefresh && getActivity() instanceof QDueMainActivity mainActivity) {

            // Unregister
            mainActivity.unregisterEventsRefreshFragment( this );

            mIsRegisteredForEventsRefresh = false;
            Log.d( TAG, String.format( "Unregistered %s from events refresh", mFragmentId ) );
        }
    }

    /**
     * Handle fragment view destruction.
     * Cleans up handlers, cache, and listeners to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cancel all pending operations
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages( null );
        }
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages( null );
        }

        // Clean up resources
        if (mItemsCache != null) {
            mItemsCache.clear();
        }
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }
    }

    /**
     * API: Refresh data when preferences change.
     * - clears data manager
     * - regenerates data.
     */
    public void refreshData() {
        // Clear data manager cache
        mDataManager.clearCache();

        // Regenerate data
        setupInfiniteScrolling();
    }

    /**
     * API: Notify updates from external sources.
     * Updates user team and refreshes adapter.
     */
    @SuppressLint ("NotifyDataSetChanged")
    public void notifyUpdates() {
        // Update user team if necessary
        onUserTeamChanged();

        if (getFragmentAdapter() != null) {
            getFragmentAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Called when user team changes.
     * Subclasses can override for specific logic.
     */
    protected void onUserTeamChanged() {
    }

    // ==================== CACHE LOADING ====================

    /**
     * Enhanced cache loading that also loads events for new periods.
     * - loadEventsForCurrentPeriod()
     *
     * @param monthDate month date to load
     */
    //@Override
    protected void addMonthToCache(LocalDate monthDate) {
        List<SharedViewModels.ViewItem> monthItems = generateMonthItems( monthDate );
        mItemsCache.addAll( monthItems );

        // Load events for the new month
        loadEventsForMonth( monthDate );
    }

    /**
     * Update events cache (#and notify adapter).
     * FIX: merge instead of overwriting
     */
    protected void updateEventsCache(Map<LocalDate, List<LocalEvent>> newEventsMap) {
        if (newEventsMap != null && !newEventsMap.isEmpty()) {
            // FIX: Fare MERGE invece di putAll che può sovrascrivere
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : newEventsMap.entrySet()) {
                LocalDate date = entry.getKey();
                List<LocalEvent> newEvents = entry.getValue();

                if (mEventsCache.containsKey( date )) {
                    // Merge con eventi esistenti per questa data
                    List<LocalEvent> existingEvents = mEventsCache.get( date );
                    if (existingEvents != null) {
                        // Creare lista combinata evitando duplicati
                        List<LocalEvent> mergedEvents = new ArrayList<>( existingEvents );
                        for (LocalEvent newEvent : newEvents) {
                            if (!mergedEvents.contains( newEvent )) {
                                mergedEvents.add( newEvent );
                            }
                        }
                        mEventsCache.put( date, mergedEvents );
                    } else {
                        mEventsCache.put( date, new ArrayList<>( newEvents ) );
                    }
                } else {
                    // New date, add directly
                    mEventsCache.put( date, new ArrayList<>( newEvents ) );
                }
            }
        }
    }

    /**
     * API: Enhanced refresh events data method
     * Update cache
     * Notify adapter
     */
    public void refreshEventsData() {
        // Clear existing cache
        mEventsCache.clear();

        // Reload events for current period
        loadEventsForCurrentPeriod();

        // Notify UI already done
        // TODO: if the usr leaves the events page before the  time to abort changes, the UI isn't updated
    }

    // ==================== EVENTS LOADING METHODS ====================

    /**
     * Load events for a specific month. This method:
     * - runs in background
     * - updates UI when complete.
     *
     * @param monthDate The month to load events for
     */
    private void loadEventsForMonth(LocalDate monthDate) {
        CompletableFuture.supplyAsync( () -> {
            try {
                LocalDate startOfMonth = monthDate.withDayOfMonth( 1 );
                LocalDate endOfMonth = monthDate.withDayOfMonth( monthDate.lengthOfMonth() );

                // Convert to LocalDateTime for DAO method
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = endOfMonth.atTime( 23, 59, 59 );

                return mDatabase.eventDao().getEventsForDateRange( startDateTime, endDateTime );
            } catch (Exception e) {
                Log.e( TAG, "Error loading events for month " + monthDate, e );
                return new ArrayList<LocalEvent>();
            }
        } ).thenAccept( events -> {
            Map<LocalDate, List<LocalEvent>> monthEvents = groupEventsByDate( events );
            mMainHandler.post( () -> {
                updateEventsCache( monthEvents );
                notifyEventsDataChanged();
            } );
        } );
    }

    /**
     * Load events for the current visible period (non-blocking). This method:
     * - runs in background
     * - updates UI when complete.
     */
    private void loadEventsForCurrentPeriod() {
        if (mIsLoadingEvents.get()) return;

        mIsLoadingEvents.set( true );

        // Calculate date range for current cache
        LocalDate startDate = getCurrentPeriodStart();
        LocalDate endDate = getCurrentPeriodEnd();

        // Load events asynchronously
        CompletableFuture.supplyAsync( () -> {
            try {
                // Convert LocalDate to LocalDateTime for DAO method
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime( 23, 59, 59 );
                return mDatabase.eventDao().getEventsForDateRange( startDateTime, endDateTime );
            } catch (Exception e) {
                Log.e( TAG, "Error loading events from database", e );
                return new ArrayList<LocalEvent>();
            }
        } ).thenAccept( events -> {
            if (events.isEmpty()) return;

            Log.d( TAG, MessageFormat.format( "✅ Loaded {0} events from database", events.size() ) );

            // Process events into date-grouped map
            Map<LocalDate, List<LocalEvent>> eventsMap = groupEventsByDate( events );

            Log.d( TAG, MessageFormat.format( "✅ Grouped into {0} dates with events", eventsMap.size() ) );

            // Debug: Log alcuni esempi
            int debugCount = 0;
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsMap.entrySet()) {
                if (debugCount < 3) { // Solo primi 3 per debug
                    LocalDate date = entry.getKey();
                    List<LocalEvent> dateEvents = entry.getValue();
                    Log.d( TAG, MessageFormat.format( "DEBUG: Date {0} has {1} events", date, dateEvents.size() ) );
                    debugCount++;
                }
            }

            // Update cache on main thread
            mMainHandler.post( () -> {
                updateEventsCache( eventsMap );
                notifyEventsDataChanged();
                mIsLoadingEvents.set( false );
            } );
        } ).exceptionally( throwable -> {
            Log.e( TAG, "Failed to load events", throwable );
            mMainHandler.post( () -> mIsLoadingEvents.set( false ) );
            return null;
        } );
    }

    /**
     * Group events by their date range for efficient lookup.
     * FIX: Now handles multi-day events correctly by adding them to all affected dates
     */
    private Map<LocalDate, List<LocalEvent>> groupEventsByDate(List<LocalEvent> events) {
        Map<LocalDate, List<LocalEvent>> grouped = new HashMap<>();

        for (LocalEvent event : events) {
            LocalDate startDate = event.getStartDate();
            LocalDate endDate = event.getEndDate();

            // SINGLE DAY EVENT: Add only to start date
            if (endDate == null || startDate.equals( endDate )) {
                grouped.computeIfAbsent( startDate, k -> new ArrayList<>() ).add( event );
            }
            // MULTI-DAY EVENT: Add to all dates in range
            else {
                LocalDate currentDate = startDate;
                int dayCount = 0;

                while (!currentDate.isAfter( endDate ) && dayCount < 365) { // Safety limit
                    grouped.computeIfAbsent( currentDate, k -> new ArrayList<>() ).add( event );

                    currentDate = currentDate.plusDays( 1 );
                    dayCount++;
                }

                Log.d( TAG, MessageFormat.format(
                        "Multi-day event ''{0}'' spans {1} days ({2} to {3})",
                        event.getTitle(), dayCount, startDate, endDate ) );
            }
        }

        return grouped;
    }

    /**
     * Get start date of current cached period.
     */
    private LocalDate getCurrentPeriodStart() {
        if (mCurrentDate != null) {
            return mCurrentDate.minusMonths( QD_MONTHS_CACHE_RADIUS );
        }
        return LocalDate.now().minusMonths( QD_MONTHS_CACHE_RADIUS );
    }

    /**
     * Get end date of current cached period.
     */
    private LocalDate getCurrentPeriodEnd() {
        if (mCurrentDate != null) {
            return mCurrentDate.plusMonths( QD_MONTHS_CACHE_RADIUS );
        }
        return LocalDate.now().plusMonths( QD_MONTHS_CACHE_RADIUS );
    }

    /**
     * Get events for a specific date.
     *
     * @param date The date to get events for
     * @return List of events for the date, or empty list if none
     */
    protected List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> events = mEventsCache.get( date );
        return events != null ? events : new ArrayList<>();
    }

    // ==================== COMPLETE INTERFACE IMPLEMENTATION ====================

    /**
     * Called when events change.
     * Refresh Events Data
     * Notify Adapter
     *
     * @param changeType Type of change (import, delete, create, modify)
     * @param eventCount Number of events affected by the change
     */
    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.d( TAG, String.format( QDue.getLocale(),
                "BaseFragment (%s): Events changed %s (%d events)",
                getClass().getSimpleName(), changeType, eventCount ) );

        // Only refresh if fragment is active
        if (isFragmentActive()) {
            refreshEventsData();
        } else {
            Log.d( TAG, String.format( "Fragment %s is inactive, skipping immediate refresh",
                    getClass().getSimpleName() ) );
        }
    }

    /**
     * Called when events data needs to be refreshed.
     */
    @Override
    public void onForceEventsRefresh() {
        Log.d( TAG, String.format( "BaseFragment (%s): Force refresh requested",
                getClass().getSimpleName() ) );

        // Always refresh on force request, regardless of fragment state
        refreshEventsData();
    }

    @Override
    public boolean isFragmentActive() {
        // Enhanced check for fragment activity state
        return isAdded() &&
                isVisible() &&
                !isRemoving() &&
                !isDetached() &&
                getUserVisibleHint() && // For ViewPager compatibility
                getActivity() != null &&
                !getActivity().isFinishing();
    }

    /**
     * Get a description of the fragment for debugging.
     *
     * @return Fragment description
     */
    @Override
    public String getFragmentDescription() {
        // Child classe should implement this
        return String.format( "%s (ID: %s)", getClass().getSimpleName(), "unknown" );
    }

    /**
     * Notify subclasses that events data has changed.
     * Subclasses should override this to update their adapters.
     */
    @SuppressLint ("NotifyDataSetChanged")
    protected void notifyEventsDataChanged() {
        try {
            // Update adapter with new events data
            if (getFragmentAdapter() != null) {
                getFragmentAdapter().notifyDataSetChanged();
                Log.d( TAG, String.format( QDue.getLocale(), "✅ Notified adapter of events changes (%d dates with events)",
                        mEventsCache.size() ) );
            }

            // Additional subclass-specific notifications
            onEventsDataRefreshed();
        } catch (Exception e) {
            Log.e( TAG, "notifyEventsDataChanged", e );
        }
    }

    /**
     * Hook for subclasses to perform additional actions when events data is refreshed
     */
    protected void onEventsDataRefreshed() {
        // Default implementation - subclasses can override
        Log.d( TAG, String.format( "onEventsDataRefreshed: ✅ Events data refreshed for %s", getClass().getSimpleName() ) );
    }

    /**
     * Check if lazy refresh should be performed
     *
     * @return true if events might have changed while fragment was inactive
     */
    private boolean shouldPerformBaseLazyRefresh() {
        // Simple approach: always refresh on resume
        // Could be enhanced with timestamp checking or other logic
        return true;
    }

    // ==================== PUBLIC API FOR SUBCLASSES ====================

    /**
     * Get events cache for adapter integration.
     *
     * @return Current events cache
     */
    protected Map<LocalDate, List<LocalEvent>> getEventsCache() {
        return new HashMap<>( mEventsCache ); // Return copy for thread safety
    }
}