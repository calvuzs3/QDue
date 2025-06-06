package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.QDue.Debug.DEBUG_FRAGMENT;
import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

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
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
 *
 * @author calvuzs3
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    // === COMMON COMPONENTS ===

    /**
     * Core QuattroDue instance for shift data
     */
    protected QuattroDue mQD;

    /**
     * Centralized data manager for calendar operations
     */
    protected CalendarDataManager mDataManager;

    /**
     * Main RecyclerView component
     */
    protected RecyclerView mRecyclerView;

    /**
     * LinearLayoutManager reference (null for GridLayoutManager)
     */
    protected LinearLayoutManager mLayoutManager;

    /**
     * Floating Action Button for "go to today" functionality
     */
    protected FloatingActionButton mFabGoToToday;

    // === INFINITE SCROLLING CACHE ===

    /**
     * Cache of view items for infinite scrolling
     */
    protected List<SharedViewModels.ViewItem> mItemsCache;

    /**
     * Current center position in cache
     */
    protected int mCurrentCenterPosition;

    /**
     * Current date cursor
     */
    protected LocalDate mCurrentDate;

    // === CONCURRENCY CONTROL ===

    /**
     * Atomic flag to prevent concurrent cache updates
     */
    protected final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);

    /**
     * Atomic flag for pending top load operations
     */
    protected final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);

    /**
     * Atomic flag for pending bottom load operations
     */
    protected final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // === ASYNC OPERATION HANDLERS ===

    /**
     * Main thread handler for UI operations
     */
    protected Handler mMainHandler;

    /**
     * Background handler for data operations
     */
    protected Handler mBackgroundHandler;

    // === POSITION TRACKING ===

    /**
     * Position of today in the cache (-1 if not found)
     */
    protected int mTodayPosition = -1;

    // === LOADING INDICATORS ===

    /**
     * Flag indicating top loader is currently showing
     */
    protected boolean mShowingTopLoader = false;

    /**
     * Flag indicating bottom loader is currently showing
     */
    protected boolean mShowingBottomLoader = false;

    // === SCROLL VELOCITY CONTROL ===

    /**
     * Last scroll timestamp for throttling
     */
    protected long mLastScrollTime = 0;

    /**
     * Current scroll velocity for performance optimization
     */
    protected int mScrollVelocity = 0;

    /**
     * Maximum scroll velocity before ignoring updates
     */
    protected static final int MAX_SCROLL_VELOCITY = 25;

    /**
     * Delay before processing operations after scroll settles
     */
    protected static final long SCROLL_SETTLE_DELAY = 150;

    // ==================== ENHANCED CONSTANTS ====================

    /**
     * Number of months to load in a single operation
     */
    private static final int MONTHS_PER_LOAD = 3;

    /**
     * Preload trigger zone (in calendar weeks)
     */
    private static final int PRELOAD_TRIGGER_WEEKS = 6; // ~1.5 months

    /**
     * Maximum number of months to keep in cache
     */
    private static final int MAX_CACHED_MONTHS = 18; // ~1.5 years

    // ==================== INTERFACES INTERACTION ================

    /**
     * Reference to the communication interface
     */
    protected FragmentCommunicationInterface communicationInterface;

    // ==================== COMMUNICATION INTERFACE ====================

    /**
     * Get reference to communication interface during attachment.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            // Get reference to activity's communication interface
            communicationInterface = (FragmentCommunicationInterface) context;
            Log.v(TAG, "onAttach: Communication interface attached");
        } catch (ClassCastException e) {
            Log.e(TAG, "onAttach: Error in attacching Comunication Interface");
            throw new ClassCastException(context.toString()
                    + " must implement FragmentCommunicationInterface");
        }
    }

    /**
     * Clear reference when fragment is detached.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        communicationInterface = null;
        Log.d(TAG, "onDetach: Communication interface detached");
    }

    // ==================== COMMUNICATION HELPER METHODS ====================

    /**
     * Request navigation to another destination.
     */
    protected void navigateTo(int destinationId) {
        navigateTo(destinationId, null);
    }

    /**
     * Request navigation with data bundle.
     */
    protected void navigateTo(int destinationId, Bundle data) {
        if (communicationInterface != null) {
            communicationInterface.onFragmentNavigationRequested(destinationId, data);
        }
    }

    // ==================== USUAL METHODS ===================================

    /**
     * Initialize fragment components and handlers.
     * Sets up async handlers and data manager.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String METHOD_TAG = TAG + " onCreate";

        // Initialize handlers for async operations
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundHandler = new Handler(Looper.getMainLooper());

        // Initialize centralized data manager
        mDataManager = CalendarDataManager.getInstance();

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Fragment created: " + getClass().getSimpleName());
    }

    /**
     * Complete view setup after inflation.
     * Coordinates view finding, RecyclerView setup, and infinite scrolling initialization.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final String mTAG = "onViewCreated: ";

        // Subclasses must implement findViews() to initialize their specific views
        findViews(view);

        // Configure the RecyclerView with appropriate LayoutManager
        setupRecyclerView();

        // Initialize QuattroDue and setup infinite scrolling
        if (getActivity() != null) {
            mQD = QDue.getQuattrodue();
            mDataManager.initialize(mQD);
            setupInfiniteScrolling();
        } else {
            Log.e(TAG, mTAG + "Activity is null, cannot initialize infinite scrolling");
        }

        // Configure the Floating Action Button
        setupFAB();
    }

    /**
     * Setup RecyclerView with support for custom LayoutManagers.
     * <p>
     * This method handles both LinearLayoutManager (for DaysListFragment)
     * and GridLayoutManager (for CalendarFragment). Subclasses can override
     * this method to provide their own LayoutManager setup.
     */
    protected void setupRecyclerView() {
        final String mTAG = "setupRecyclerView: ";

        if (mRecyclerView == null) {
            Log.e(TAG, "RecyclerView is null - subclass must implement findViews()");
            return;
        }

        // Check if LayoutManager already exists
        // Subclasses can override this method for specific LayoutManager types
        RecyclerView.LayoutManager existingLayoutManager = mRecyclerView.getLayoutManager();

        if (existingLayoutManager == null) {
            // Create and set new LinearLayoutManager as default
            mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(mLayoutManager);
            if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Created new LinearLayoutManager");
        } else if (existingLayoutManager instanceof LinearLayoutManager) {
            // Use existing LinearLayoutManager
            mLayoutManager = (LinearLayoutManager) existingLayoutManager;
            if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Using existing LinearLayoutManager");
        } else {
            // For other LayoutManager types (e.g., GridLayoutManager), keep null and use fallback methods
            mLayoutManager = null;
            if (DEBUG_FRAGMENT)
                Log.d(TAG, mTAG + "Using non-LinearLayoutManager, set mLayoutManager to null");
        }

        // Apply common optimizations
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
    }

    /**
     * Configure the common Floating Action Button behavior.
     * Sets up click listener for "go to today" functionality.
     */
    protected void setupFAB() {
        final String mTAG = "setupFAB: ";

        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> scrollToToday());
//            mFabGoToToday.hide(); // Initially hidden
            Log.d(TAG, mTAG + "FAB configured and (not)hidden");
        } else {
            Log.d(TAG, mTAG + "FAB not found in layout");
        }
    }

    /**
     * Setup infinite scrolling with optimized logic.
     * Uses default LayoutManager scrolling behavior.
     */
    protected void setupInfiniteScrolling() {
        setupInfiniteScrolling(true);
    }

    /**
     * Setup infinite scrolling with configurable LayoutManager usage.
     *
     * @param useLayoutManager whether to use LayoutManager for initial scrolling
     *                         or let subclass handle it (e.g., CalendarViewFragment)
     */
    protected void setupInfiniteScrolling(boolean useLayoutManager) {
        final String mTAG = "setupInfiniteScrolling: ";

        if (mQD == null || mDataManager == null) {
            Log.e(TAG, "Components not initialized, cannot setup infinite scrolling");
            return;
        }

        try {
            // Reset all control flags
            resetControlFlags();

            // Initialize cache
            mCurrentDate = mQD.getCursorDate();
            mItemsCache = new ArrayList<>();

            // Pre-load months in cache around current date
            mDataManager.preloadMonthsAround(mCurrentDate, QD_MONTHS_CACHE_SIZE);

            // Generate initial months for display
            for (int i = -QD_MONTHS_CACHE_SIZE; i <= QD_MONTHS_CACHE_SIZE; i++) {
                LocalDate monthDate = mCurrentDate.plusMonths(i);
                addMonthToCache(monthDate);
            }

            // Find today's position in cache
            mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

            // Set central position in cache
            mCurrentCenterPosition = mItemsCache.size() / 2;

            // Setup adapter (implemented by subclasses)
            setupAdapter();

            // Setup optimized scroll listener
            mRecyclerView.addOnScrollListener(new OptimizedInfiniteScrollListener());

            // Scroll to appropriate position if requested
            if (useLayoutManager) {
                scrollToInitialPosition();
            }

            Log.d(TAG, mTAG + "Infinite scroll setup completed: " + mItemsCache.size() +
                    " elements, today at position: " + mTodayPosition);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during infinite scrolling setup: " + e.getMessage());
        }
    }

    /**
     * Optimized scroll listener that prevents adapter modifications during scroll callbacks.
     * <p>
     * This listener implements:
     * - Velocity-based throttling to improve performance
     * - Deferred adapter operations to avoid scroll callback conflicts
     * - Automatic cleanup scheduling when scroll settles
     */
    private class OptimizedInfiniteScrollListener extends RecyclerView.OnScrollListener {

        private static final String mTAG = "OptimizedInfiniteScrollListener: ";

        /**
         * Handle scroll events with velocity calculation and throttling.
         */
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            long currentTime = System.currentTimeMillis();

            // Calculate scroll velocity for performance optimization
            if (mLastScrollTime > 0) {
                long timeDiff = currentTime - mLastScrollTime;
                if (timeDiff > 0) {
                    mScrollVelocity = (int) (Math.abs(dy) / timeDiff * 16); // Normalized to 60fps
                }
            }

            // Throttling to avoid too frequent updates (100ms = ~10fps)
            if (currentTime - mLastScrollTime < 100) return;
            mLastScrollTime = currentTime;

            // Skip updates if scrolling too fast
            if (mScrollVelocity > MAX_SCROLL_VELOCITY) return;

            // Get visible positions safely
            int firstVisible = getFirstVisiblePosition();
            int lastVisible = getLastVisiblePosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Handle loading based on scroll - POST TO NEXT FRAME to avoid scroll callback issues
            mMainHandler.post(() -> handleScrollBasedLoading(firstVisible, lastVisible, dy));

            // Update FAB visibility
            updateFabVisibility(firstVisible, lastVisible);
        }

        /**
         * Handle scroll state changes and trigger cleanup when scroll settles.
         */
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (DEBUG_FRAGMENT) {
                String stateStr = newState == RecyclerView.SCROLL_STATE_IDLE ? "IDLE" :
                        newState == RecyclerView.SCROLL_STATE_DRAGGING ? "DRAGGING" : "SETTLING";
                Log.v(TAG + " onScrollStateChanged", "Scroll state: " + stateStr + ", velocity: " + mScrollVelocity);
            }

            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;

                // Process pending operations after scroll stops
                mMainHandler.postDelayed(() -> {
                    if (mScrollVelocity == 0) {
                        processPendingOperations();
                        scheduleCleanupIfNeeded();
                    }
                }, SCROLL_SETTLE_DELAY);
            }
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
        final String mTAG = "executeTopLoad: ";

        if (!mIsPendingTopLoad.compareAndSet(true, false)) {
            Log.w(TAG, mTAG + "No pending top load - aborting");
            hideTopLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            Log.w(TAG, mTAG + "Cache update in progress - aborting");
            mIsPendingTopLoad.set(true);
            return;
        }

        Log.d(TAG, "Starting multi-month top load execution");

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                Log.e(TAG, mTAG + "Cannot find first month in cache");
                mIsUpdatingCache.set(false);
                hideTopLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths(i);
                Log.d(TAG, mTAG + "Loading previous month " + i + ": " + monthToLoad);

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(0, monthItems);
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w(TAG, mTAG + "No items generated for any previous months");
                mIsUpdatingCache.set(false);
                hideTopLoader();
                return;
            }

            Log.d(TAG, mTAG + "Generated " + allNewItems.size() + " items for " + MONTHS_PER_LOAD + " previous months");

            mMainHandler.post(() -> {
                try {
                    if (mItemsCache == null) {
                        Log.e(TAG, mTAG + "Items cache is null during UI update");
                        return;
                    }

                    mItemsCache.addAll(0, allNewItems);
                    Log.d(TAG, mTAG + "Added items to cache. New size: " + mItemsCache.size());

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition >= 0) {
                        mTodayPosition += allNewItems.size();
                    } else {
                        // Today position was lost - try to find it again
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                        Log.d(TAG, mTAG + "Recomputed today position: " + mTodayPosition);
                    }

                    mCurrentCenterPosition += allNewItems.size();

                    mMainHandler.post(() -> {
                        try {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(0, allNewItems.size());
                                Log.d(TAG, mTAG + "Notified adapter of " + allNewItems.size() + " inserted items");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, mTAG + "Error notifying adapter: " + e.getMessage());
                        }
                    });

                    maintainScrollPosition(allNewItems.size());

                    mMainHandler.postDelayed(() -> {
                        hideTopLoader();
                        Log.d(TAG, mTAG + "Multi-month top load completed successfully");
                        schedulePreloadingCheck();
                    }, 100);

                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error during UI update: " + e.getMessage());
                    hideTopLoader();
                } finally {
                    mIsUpdatingCache.set(false);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error loading previous months: " + e.getMessage());
            mIsUpdatingCache.set(false);
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
        final String mTAG = "executeBottomLoad: ";

        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            Log.w(TAG, "No pending bottom load - aborting");
            hideBottomLoader();
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            Log.w(TAG, "Cache update in progress - aborting");
            mIsPendingBottomLoad.set(true);
            return;
        }

        Log.d(TAG, "Starting multi-month bottom load execution");

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                Log.e(TAG, "Cannot find last month in cache");
                mIsUpdatingCache.set(false);
                hideBottomLoader();
                return;
            }

            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

            for (int i = 1; i <= MONTHS_PER_LOAD; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths(i);
                Log.d(TAG, "Loading next month " + i + ": " + monthToLoad);

                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(monthItems);
                }
            }

            if (allNewItems.isEmpty()) {
                Log.w(TAG, "No items generated for any next months");
                mIsUpdatingCache.set(false);
                hideBottomLoader();
                return;
            }

            Log.d(TAG, "Generated " + allNewItems.size() + " items for " + MONTHS_PER_LOAD + " next months");

            mMainHandler.post(() -> {
                try {
                    if (mItemsCache == null) {
                        Log.e(TAG, "Items cache is null during UI update");
                        return;
                    }

                    int insertPos = mItemsCache.size();
                    mItemsCache.addAll(allNewItems);
                    Log.d(TAG, "Added items to cache. New size: " + mItemsCache.size());

                    // ENHANCED: Better today position tracking
                    if (mTodayPosition < 0) {
                        // Today position not set - try to find it in the expanded cache
                        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                        Log.d(TAG, "Found today position in expanded cache: " + mTodayPosition);
                    }

                    mMainHandler.post(() -> {
                        try {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(insertPos, allNewItems.size());
                                Log.d(TAG, "Notified adapter of " + allNewItems.size() + " inserted items at position " + insertPos);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying adapter: " + e.getMessage());
                        }
                    });

                    mMainHandler.postDelayed(() -> {
                        hideBottomLoader();
                        Log.d(TAG, "Multi-month bottom load completed successfully");
                        schedulePreloadingCheck();
                    }, 100);

                } catch (Exception e) {
                    Log.e(TAG, "Error during UI update: " + e.getMessage());
                    hideBottomLoader();
                } finally {
                    mIsUpdatingCache.set(false);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error loading next months: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideBottomLoader();
        }
    }

    /**
     * NEW: Schedule preloading check to maintain buffer zones
     */
    private void schedulePreloadingCheck() {
        final String mTAG = "schedulePreloadingCheck: ";
        Log.v(TAG, mTAG + "called.");

        // Schedule preloading check after a short delay
        mMainHandler.postDelayed(() -> {
            if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                performPreloadingCheck();
            }
        }, 500); // Check after scrolling settles
    }

    /**
     * NEW: Perform silent preloading to maintain buffer zones
     */
    private void performPreloadingCheck() {
        final String mTAG = "performPreloadingCheck: ";
        Log.v(TAG, mTAG + "called");

        if (mIsUpdatingCache.get()) {
            Log.d(TAG, mTAG + "Cache update in progress - skipping preload check");
            return;
        }

        // Get current scroll position
        int currentPosition = getCurrentVisiblePosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            return;
        }

        int cacheSize = mItemsCache.size();
        int bufferZone = PRELOAD_TRIGGER_WEEKS * 7; // Convert weeks to items

        // Check if we need to preload at the top
        if (currentPosition < bufferZone && !mIsPendingTopLoad.get()) {
            Log.d(TAG, mTAG + "Triggering silent top preload at position: " + currentPosition);
            triggerSilentTopLoad();
        }

        // Check if we need to preload at the bottom
        if (currentPosition > cacheSize - bufferZone && !mIsPendingBottomLoad.get()) {
            Log.d(TAG, mTAG + "Triggering silent bottom preload at position: " + currentPosition);
            triggerSilentBottomLoad();
        }

        // Check if cache is too large and needs cleanup
        if (cacheSize > MAX_CACHED_MONTHS * 35) { // ~35 items per month
            Log.d(TAG, mTAG + "Cache too large (" + cacheSize + "), scheduling cleanup");
            scheduleCleanupIfNeeded();
        }
    }

    /**
     * NEW: Silent top load without visible loader
     */
    private void triggerSilentTopLoad() {
        final String mTAG = "triggerSilentTopLoad: ";

        if (mIsUpdatingCache.get() || mIsPendingTopLoad.get()) {
            return;
        }

        Log.d(TAG, mTAG + "Starting silent top load");
        mIsPendingTopLoad.set(true);

        // Execute without showing loader
        mBackgroundHandler.postDelayed(() -> {
            executeSilentTopLoad();
        }, 100);
    }

    /**
     * NEW: Silent bottom load without visible loader
     */
    private void triggerSilentBottomLoad() {
        final String mTAG = "triggerSilentBottomLoad: ";

        if (mIsUpdatingCache.get() || mIsPendingBottomLoad.get()) {
            return;
        }

        Log.d(TAG, mTAG + "Starting silent bottom load");
        mIsPendingBottomLoad.set(true);

        // Execute without showing loader
        mBackgroundHandler.postDelayed(() -> {
            executeSilentBottomLoad();
        }, 100);
    }

    /**
     * NEW: Execute silent top load (similar to executeTopLoad but without loader UI)
     */
    private void executeSilentTopLoad() {
        final String mTAG = "executeSilentTopLoad: ";

        if (!mIsPendingTopLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            mIsPendingTopLoad.set(true);
            return;
        }

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth == null) {
                mIsUpdatingCache.set(false);
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = firstMonth.minusMonths(i);
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(0, monthItems);
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post(() -> {
                    try {
                        mItemsCache.addAll(0, allNewItems);
                        if (mTodayPosition >= 0) mTodayPosition += allNewItems.size();
                        mCurrentCenterPosition += allNewItems.size();

                        mMainHandler.post(() -> {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(0, allNewItems.size());
                            }
                        });

                        maintainScrollPosition(allNewItems.size());
                        Log.d(TAG, mTAG + "Silent top load completed: " + allNewItems.size() + " items");
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in silent top load: " + e.getMessage());
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * NEW: Execute silent bottom load (similar to executeBottomLoad but without loader UI)
     */
    private void executeSilentBottomLoad() {
        final String mTAG = "executeSilentBottomLoad: ";

        if (!mIsPendingBottomLoad.compareAndSet(true, false)) {
            return;
        }

        if (!mIsUpdatingCache.compareAndSet(false, true)) {
            mIsPendingBottomLoad.set(true);
            return;
        }

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth == null) {
                mIsUpdatingCache.set(false);
                return;
            }

            // Load 2 months silently
            List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                LocalDate monthToLoad = lastMonth.plusMonths(i);
                List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthToLoad);
                if (!monthItems.isEmpty()) {
                    allNewItems.addAll(monthItems);
                }
            }

            if (!allNewItems.isEmpty()) {
                mMainHandler.post(() -> {
                    try {
                        int insertPos = mItemsCache.size();
                        mItemsCache.addAll(allNewItems);

                        if (mTodayPosition < 0) {
                            int todayInNew = SharedViewModels.DataConverter.findTodayPosition(allNewItems);
                            if (todayInNew >= 0) {
                                mTodayPosition = insertPos + todayInNew;
                            }
                        }

                        mMainHandler.post(() -> {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(insertPos, allNewItems.size());
                            }
                        });

                        Log.d(TAG, mTAG + "Silent bottom load completed: " + allNewItems.size() + " items");
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in silent bottom load: " + e.getMessage());
            mIsUpdatingCache.set(false);
        }
    }

    /**
     * Handle loading based on scroll position.
     * Triggers top or bottom load when user scrolls near the edges.
     *
     * @param firstVisible    first visible item position
     * @param lastVisible     last visible item position
     * @param scrollDirection scroll direction (negative = up, positive = down)
     */
    private void handleScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        final String mTAG = "handleScrollBasedLoading: ";

        // Load upward (previous months) when near top
        if (firstVisible <= 10 && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (DEBUG_FRAGMENT)
                Log.d(TAG, mTAG + "Triggering top load at position: " + firstVisible);
            triggerTopLoad();
        }

        // Load downward (next months) when near bottom
        if (lastVisible >= mItemsCache.size() - 10 && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (DEBUG_FRAGMENT)
                Log.d(TAG, mTAG + "Triggering bottom load at position: " + lastVisible);
            triggerBottomLoad();
        }
    }

    /**
     * Trigger top load operation (previous months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger top load with better state management
     */
    protected void triggerTopLoad() {
        final String mTAG = "triggerTopLoad: ";

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w(TAG, mTAG + "Cache update in progress - ignoring trigger");
            return;
        }

        if (mIsPendingTopLoad.get()) {
            Log.w(TAG, mTAG + "Top load already pending - ignoring trigger");
            return;
        }

        if (mShowingTopLoader) {
            Log.w(TAG, mTAG + "Top loader already showing - ignoring trigger");
            return;
        }

        Log.d(TAG, mTAG + "Triggering top load");

        mIsPendingTopLoad.set(true);
        showTopLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed(() -> {
            Log.d(TAG, mTAG + "Executing delayed top load");
            executeTopLoad();
        }, 200); // Increased delay to ensure loader visibility
    }

    /**
     * Trigger bottom load operation (next months).
     * Sets up loading state and schedules execution.
     * FIXED: Trigger bottom load with better state management
     */
    protected void triggerBottomLoad() {
        final String mTAG = "triggerBottomLoad: ";

        // Check if already loading or pending
        if (mIsUpdatingCache.get()) {
            Log.w(TAG, mTAG + "Cache update in progress - ignoring trigger");
            return;
        }

        if (mIsPendingBottomLoad.get()) {
            Log.w(TAG, mTAG + "Bottom load already pending - ignoring trigger");
            return;
        }

        if (mShowingBottomLoader) {
            Log.w(TAG, mTAG + "Bottom loader already showing - ignoring trigger");
            return;
        }

        Log.d(TAG, mTAG + "Triggering bottom load");

        mIsPendingBottomLoad.set(true);
        showBottomLoader();

        // Execute with a small delay to ensure loader is visible
        mBackgroundHandler.postDelayed(() -> {
            Log.d(TAG, mTAG + "Executing delayed bottom load");
            executeBottomLoad();
        }, 200); // Increased delay to ensure loader visibility
    }

    /**
     * Add a month to the cache by generating its view items.
     *
     * @param monthDate the month to add
     */
    private void addMonthToCache(LocalDate monthDate) {
        List<SharedViewModels.ViewItem> monthItems = generateMonthItems(monthDate);
        mItemsCache.addAll(monthItems);
    }

    /**
     * Generate view items for a specific month using the fragment-specific converter.
     *
     * @param monthDate the month to generate items for
     * @return list of view items for the month
     */
    private List<SharedViewModels.ViewItem> generateMonthItems(LocalDate monthDate) {
        List<Day> monthDays = mDataManager.getMonthDays(monthDate);
        return convertMonthData(monthDays, monthDate);
    }

    /**
     * Process pending operations when scroll stops.
     * Executes any pending load operations that were deferred during scrolling.
     */
    protected void processPendingOperations() {
        final String mTAG = "processPendingOperations: ";

        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Processing pending top load");
            executeTopLoad();
        }
        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Processing pending bottom load");
            executeBottomLoad();
        }
    }

    /**
     * Schedule cache cleanup if cache size exceeds limits.
     * Prevents memory issues by cleaning old cache entries when cache grows too large.
     */
    protected void scheduleCleanupIfNeeded() {
        final String mTAG = "scheduleCleanupIfNeeded: ";

        int maxElements = QD_MAX_CACHE_SIZE * 35; // ~35 elements per month
        if (mItemsCache.size() > maxElements) {
            if (DEBUG_FRAGMENT)
                Log.d(TAG, mTAG + "Scheduling cache cleanup, current size: " + mItemsCache.size());

            mBackgroundHandler.postDelayed(() -> {
                if (!mIsUpdatingCache.get() && mScrollVelocity == 0) {
                    cleanupCache();
                }
            }, 1000);
        }
    }

    /**
     * Clean up cache by removing elements far from current position.
     * Works with both LinearLayoutManager and GridLayoutManager.
     */
    private void cleanupCache() {
        final String mTAG = "cleanupCache: ";

        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        mMainHandler.post(() -> {
            try {
                // Get current position safely for any LayoutManager type
                int currentPos = getCurrentVisiblePosition();
                int targetSize = QD_MONTHS_CACHE_SIZE * 35;

                // If we can't determine position, use a safe fallback
                if (currentPos == RecyclerView.NO_POSITION) {
                    currentPos = mItemsCache.size() / 2; // Use middle as fallback
                    if (DEBUG_FRAGMENT) Log.w(TAG, mTAG + "Using fallback position for cleanup");
                }

                // Remove from start if necessary
                while (mItemsCache.size() > targetSize && currentPos > targetSize / 2) {
                    mItemsCache.remove(0);
                    currentPos--;
                    if (mTodayPosition > 0) mTodayPosition--;
                    if (mCurrentCenterPosition > 0) mCurrentCenterPosition--;

                    // POST adapter notification to next frame
                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRemoved(0);
                        }
                    });
                }

                // Remove from end if necessary
                while (mItemsCache.size() > targetSize &&
                        currentPos < mItemsCache.size() - targetSize / 2) {
                    int lastIndex = mItemsCache.size() - 1;
                    mItemsCache.remove(lastIndex);

                    // POST adapter notification to next frame
                    final int finalLastIndex = lastIndex;
                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRemoved(finalLastIndex);
                        }
                    });
                }

                if (DEBUG_FRAGMENT)
                    Log.d(TAG, mTAG + "Cache cleaned, new size: " + mItemsCache.size());
            } finally {
                mIsUpdatingCache.set(false);
            }
        });
    }

    /**
     * Get current visible position safely for any LayoutManager type.
     * Handles both LinearLayoutManager and GridLayoutManager with fallbacks.
     *
     * @return current visible position or NO_POSITION if not determinable
     */
    private int getCurrentVisiblePosition() {
        final String mTAG = "getCurrentVisiblePosition: ";

        if (mRecyclerView == null) return RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;

        try {
            // Handle LinearLayoutManager (DaysListFragment)
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                int firstVisible = linearManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    return firstVisible;
                }
                // Fallback to partially visible
                return linearManager.findFirstVisibleItemPosition();
            }

            // Handle GridLayoutManager (CalendarFragment)
            else if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
                int firstVisible = gridManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    return firstVisible;
                }
                // Fallback to partially visible
                return gridManager.findFirstVisibleItemPosition();
            }

            // For other LayoutManager types, use generic approach
            else {
                // Try to get first visible child
                View firstChild = layoutManager.getChildAt(0);
                if (firstChild != null) {
                    return mRecyclerView.getChildAdapterPosition(firstChild);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error getting current position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

//    /**
//     * Scroll to initial appropriate position.
//     * Works safely with both LinearLayoutManager and GridLayoutManager.
//     */
//    protected void scrollToInitialPosition() {
//        scrollToInitialPositionEnhanced();
//        return;
//
//        final String mTAG = "scrollToInitialPosition: ";
//
//        if (mRecyclerView == null) return;
//
//        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
//        if (layoutManager == null) return;
//
//        try {
//            int targetPosition = mTodayPosition >= 0 ? mTodayPosition : mCurrentCenterPosition;
//
//            if (layoutManager instanceof LinearLayoutManager) {
//                ((LinearLayoutManager) layoutManager).scrollToPosition(targetPosition);
//                if (DEBUG_FRAGMENT)
//                    Log.d(TAG, mTAG+"LinearLayoutManager scrolled to position: " + targetPosition);
//            } else if (layoutManager instanceof GridLayoutManager) {
//                ((GridLayoutManager) layoutManager).scrollToPosition(targetPosition);
//                if (DEBUG_FRAGMENT)
//                    Log.d(TAG, mTAG+"GridLayoutManager scrolled to position: " + targetPosition);
//            } else {
//                // Generic fallback for any other LayoutManager
//                mRecyclerView.scrollToPosition(targetPosition);
//                if (DEBUG_FRAGMENT)
//                    Log.d(TAG, mTAG+"Generic scroll to position: " + targetPosition);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, mTAG+"Error in scrollToInitialPosition: " + e.getMessage());
//
//            // Final fallback attempt
//            try {
//                if (mTodayPosition >= 0) {
//                    mRecyclerView.scrollToPosition(mTodayPosition);
//                } else {
//                    mRecyclerView.scrollToPosition(mCurrentCenterPosition);
//                }
//            } catch (Exception fallbackError) {
//                Log.e(TAG, mTAG+"Fallback scroll also failed: " + fallbackError.getMessage());
//            }
//        }
//    }

    /**
     * ENHANCED: Initial scroll with better timing and fallbacks
     */
    protected void scrollToInitialPosition() {
        final String mTAG = "scrollToInitialPositionEnhanced: ";

        if (mRecyclerView == null) {
            Log.e(TAG, mTAG + "RecyclerView is null");
            return;
        }

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            Log.e(TAG, mTAG + "LayoutManager is null");
            return;
        }

        // CRITICAL: Verify today position is valid before scrolling
        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            try {
                Log.d(TAG, mTAG + "Scrolling to today at position: " + mTodayPosition);

                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(mTodayPosition, 0);
                } else if (layoutManager instanceof GridLayoutManager) {
                    ((GridLayoutManager) layoutManager).scrollToPositionWithOffset(mTodayPosition, 0);
                } else {
                    mRecyclerView.scrollToPosition(mTodayPosition);
                }

                Log.d(TAG, mTAG + "Successfully scrolled to today");

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error scrolling to today: " + e.getMessage());
                // Fallback to center position
                try {
                    mRecyclerView.scrollToPosition(mCurrentCenterPosition);
                } catch (Exception fallbackError) {
                    Log.e(TAG, mTAG + "Fallback scroll also failed: " + fallbackError.getMessage());
                }
            }
        } else {
            Log.w(TAG, mTAG + "Today position invalid (" + mTodayPosition + "), scrolling to center");
            try {
                mRecyclerView.scrollToPosition(mCurrentCenterPosition);
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Center scroll failed: " + e.getMessage());
            }
        }
    }

    /**
     * Scroll to today's date.
     * If today is not in cache, rebuilds cache centered on today.
     */
    public void scrollToToday() {
        final String mTAG = "scrollToToday: ";

        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            mRecyclerView.smoothScrollToPosition(mTodayPosition);
            Log.v(TAG, mTAG + "Scrolling to today, position: " + mTodayPosition);
        } else {
            // Rebuild cache centered on today
            Log.d(TAG, mTAG + "Today not in cache, rebuilding");
            mCurrentDate = LocalDate.now().withDayOfMonth(1);
            setupInfiniteScrolling();
        }
    }

    /**
     * NEW: Load months around today when it's not in cache
     */
    private void loadMonthsAroundToday() {
        final String METHOD_TAG = TAG + " loadMonthsAroundToday";

        if (mIsUpdatingCache.get()) {
            Log.w(METHOD_TAG, "Cache update in progress, cannot load today");
            return;
        }

        // Show loading indicator
        showTodayLoadingIndicator();

        mBackgroundHandler.post(() -> {
            if (!mIsUpdatingCache.compareAndSet(false, true)) {
                hideTodayLoadingIndicator();
                return;
            }

            try {
                LocalDate today = LocalDate.now();
                LocalDate todayMonth = today.withDayOfMonth(1);

                // Determine if we need to add months before or after current cache
                LocalDate firstCachedMonth = findFirstMonthInCache();
                LocalDate lastCachedMonth = findLastMonthInCache();

                if (firstCachedMonth == null || lastCachedMonth == null) {
                    // Cache is empty or corrupted - rebuild around today
                    Log.w(METHOD_TAG, "Cache appears corrupted, rebuilding around today");
                    rebuildCacheAroundToday();
                    return;
                }

                // Calculate what months we need to load
                List<LocalDate> monthsToLoad = new ArrayList<>();

                if (todayMonth.isBefore(firstCachedMonth)) {
                    // Today is before cache - load months from today to cache start
                    LocalDate month = todayMonth;
                    while (month.isBefore(firstCachedMonth)) {
                        monthsToLoad.add(month);
                        month = month.plusMonths(1);
                    }
                    // Also load a few months around today for better UX
                    for (int i = 1; i <= 2; i++) {
                        monthsToLoad.add(0, todayMonth.minusMonths(i));
                    }

                    loadMonthsAtBeginning(monthsToLoad);

                } else if (todayMonth.isAfter(lastCachedMonth)) {
                    // Today is after cache - load months from cache end to today
                    LocalDate month = lastCachedMonth.plusMonths(1);
                    while (!month.isAfter(todayMonth)) {
                        monthsToLoad.add(month);
                        month = month.plusMonths(1);
                    }
                    // Also load a few months around today for better UX
                    for (int i = 1; i <= 2; i++) {
                        monthsToLoad.add(todayMonth.plusMonths(i));
                    }

                    loadMonthsAtEnd(monthsToLoad);

                } else {
                    // Today should be in cache but we couldn't find it
                    // This might be a calculation error - refresh the cache
                    Log.w(METHOD_TAG, "Today should be in cache but not found - refreshing today position");
                    refreshTodayPosition();
                }

            } catch (Exception e) {
                Log.e(METHOD_TAG, "Error loading months around today: " + e.getMessage());
                hideTodayLoadingIndicator();
                mIsUpdatingCache.set(false);
            }
        });
    }

    /**
     * NEW: Load months at the beginning of cache
     */
    private void loadMonthsAtBeginning(List<LocalDate> monthsToLoad) {
        final String mTAG = "loadMonthsAtBeginning: ";
        Log.v(TAG, mTAG + "called");

        List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

        for (LocalDate month : monthsToLoad) {
            List<SharedViewModels.ViewItem> monthItems = generateMonthItems(month);
            allNewItems.addAll(monthItems);
        }

        if (!allNewItems.isEmpty()) {
            mMainHandler.post(() -> {
                try {
                    mItemsCache.addAll(0, allNewItems);

                    // Update positions
                    if (mTodayPosition >= 0) mTodayPosition += allNewItems.size();
                    mCurrentCenterPosition += allNewItems.size();

                    // Find today's new position
                    mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRangeInserted(0, allNewItems.size());
                        }
                    });

                    // Scroll to today
                    scrollToTodayAfterLoad();

                } finally {
                    mIsUpdatingCache.set(false);
                    hideTodayLoadingIndicator();
                }
            });
        } else {
            mIsUpdatingCache.set(false);
            hideTodayLoadingIndicator();
        }
    }

    /**
     * NEW: Load months at the end of cache
     */
    private void loadMonthsAtEnd(List<LocalDate> monthsToLoad) {
        final String mTAG = "loadMonthsAtEnd: ";
        Log.v(TAG, mTAG + "called");

        List<SharedViewModels.ViewItem> allNewItems = new ArrayList<>();

        for (LocalDate month : monthsToLoad) {
            List<SharedViewModels.ViewItem> monthItems = generateMonthItems(month);
            allNewItems.addAll(monthItems);
        }

        if (!allNewItems.isEmpty()) {
            mMainHandler.post(() -> {
                try {
                    int insertPos = mItemsCache.size();
                    mItemsCache.addAll(allNewItems);

                    // Find today's position in the expanded cache
                    mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRangeInserted(insertPos, allNewItems.size());
                        }
                    });

                    // Scroll to today
                    scrollToTodayAfterLoad();

                } finally {
                    mIsUpdatingCache.set(false);
                    hideTodayLoadingIndicator();
                }
            });
        } else {
            mIsUpdatingCache.set(false);
            hideTodayLoadingIndicator();
        }
    }

    /**
     * NEW: Rebuild entire cache around today (fallback option)
     */
    @SuppressLint("NotifyDataSetChanged")
    private void rebuildCacheAroundToday() {
        final String mTAG = "rebuildCacheAroundToday: ";
        Log.v(TAG, mTAG + "called");

        mMainHandler.post(() -> {
            try {
                // Clear current cache
                int oldSize = mItemsCache.size();
                mItemsCache.clear();

                if (getAdapter() != null) {
                    getAdapter().notifyItemRangeRemoved(0, oldSize);
                }

                // Rebuild around today
                LocalDate today = LocalDate.now();
                LocalDate todayMonth = today.withDayOfMonth(1);

                for (int i = -QD_MONTHS_CACHE_SIZE; i <= QD_MONTHS_CACHE_SIZE; i++) {
                    LocalDate monthDate = todayMonth.plusMonths(i);
                    addMonthToCache(monthDate);
                }

                // Find today's position
                mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                mCurrentCenterPosition = mTodayPosition >= 0 ? mTodayPosition : mItemsCache.size() / 2;

                if (getAdapter() != null) {
                    getAdapter().notifyDataSetChanged();
                }

                // Scroll to today
                scrollToTodayAfterLoad();

            } finally {
                mIsUpdatingCache.set(false);
                hideTodayLoadingIndicator();
            }
        });
    }

    /**
     * NEW: Refresh today position in current cache
     */
    private void refreshTodayPosition() {
        final String mTAG = "refreshTodayPosition: ";

        mMainHandler.post(() -> {
            try {
                mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
                Log.d(TAG, mTAG + "Refreshed today position: " + mTodayPosition);

                if (mTodayPosition >= 0) {
                    scrollToTodayAfterLoad();
                } else {
                    Log.w(TAG, mTAG + "Today still not found after refresh");
                }

            } finally {
                mIsUpdatingCache.set(false);
                hideTodayLoadingIndicator();
            }
        });
    }

    /**
     * NEW: Scroll to today after loading is complete
     */
    private void scrollToTodayAfterLoad() {
        final String mTAG = "scrollToTodayAfterLoad: ";

        // Small delay to ensure adapter updates are complete
        mMainHandler.postDelayed(() -> {
            if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
                Log.d(TAG, mTAG + "Scrolling to today at position: " + mTodayPosition);
                mRecyclerView.smoothScrollToPosition(mTodayPosition);
            } else {
                Log.w(TAG, mTAG + "Cannot scroll to today - invalid position: " + mTodayPosition);
            }
        }, 100);
    }

    // ==================== LOADING INDICATOR HELPERS ====================

    /**
     * NEW: Show loading indicator for "go to today" operation
     */
    private void showTodayLoadingIndicator() {
        // This could be a toast, progress dialog, or FAB animation
        // For now, just log - can be enhanced with actual UI
        Log.d(TAG, "Loading months around today...");
    }

    /**
     * NEW: Hide loading indicator for "go to today" operation
     */
    private void hideTodayLoadingIndicator() {
        Log.d(TAG, "Finished loading months around today");
    }

    // === LOADING INDICATOR MANAGEMENT ===

    /**
     * Show top loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showTopLoader with more debugging
     */
    private void showTopLoader() {
        final String mTAG = "showTopLoader: ";

        if (mShowingTopLoader) {
            Log.w(TAG, mTAG + "Top loader already showing - ignoring request");
            return;
        }

        mShowingTopLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.TOP);
        mItemsCache.add(0, loader);

        if (mTodayPosition >= 0) mTodayPosition++;
        mCurrentCenterPosition++;

        Log.w(TAG, mTAG + "Added top loader - cache size now: " + mItemsCache.size());

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getAdapter() != null) {
                getAdapter().notifyItemInserted(0);
                Log.w(TAG, mTAG + "Notified adapter of top loader insertion");
            }
        });
    }

    /**
     * Show bottom loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     * Override showBottomLoader with more debugging
     */
    private void showBottomLoader() {
        final String mTAG = "showBottomLoader: ";

        if (mShowingBottomLoader) {
            Log.w(TAG, mTAG + "Bottom loader already showing - ignoring request");
            return;
        }

        mShowingBottomLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.BOTTOM);
        mItemsCache.add(loader);

        Log.w(TAG, mTAG + "Added bottom loader - cache size now: " + mItemsCache.size());

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getAdapter() != null) {
                getAdapter().notifyItemInserted(mItemsCache.size() - 1);
                Log.w(TAG, mTAG + "Notified adapter of bottom loader insertion");
            }
        });
    }

    /**
     * FIXED: Hide top loader with better error handling
     */
    private void hideTopLoader() {
        final String mTAG = "hideTopLoader: ";
        Log.v(TAG, mTAG + "called.");

        if (!mShowingTopLoader) {
            Log.d(TAG, mTAG + "Top loader not showing - nothing to hide");
            return;
        }

        try {
            for (int i = 0; i < mItemsCache.size(); i++) {
                if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                    SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.TOP) {
                        mItemsCache.remove(i);
                        if (mTodayPosition > i) mTodayPosition--;
                        if (mCurrentCenterPosition > i) mCurrentCenterPosition--;

                        Log.d(TAG, mTAG + "Removed top loader from position " + i);

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post(() -> {
                            try {
                                if (getAdapter() != null) {
                                    getAdapter().notifyItemRemoved(finalI);
                                    Log.d(TAG, mTAG + "Notified adapter of top loader removal");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, mTAG + "Error notifying adapter of loader removal: " + e.getMessage());
                            }
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error hiding top loader: " + e.getMessage());
        } finally {
            mShowingTopLoader = false;
        }
    }

    /**
     * Hide bottom loading indicator.
     * Removes loading item from cache and notifies adapter on next frame.
     */
    private void hideBottomLoader() {
        final String mTAG = "hideBottomLoader: ";

        if (!mShowingBottomLoader) {
            Log.d(TAG, mTAG + "Bottom loader not showing - nothing to hide");
            return;
        }

        Log.d(TAG, mTAG + "Hiding bottom loader");

        try {
            for (int i = mItemsCache.size() - 1; i >= 0; i--) {
                if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                    SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                    if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.BOTTOM) {
                        mItemsCache.remove(i);

                        Log.d(TAG, mTAG + "Removed bottom loader from position " + i);

                        // POST ADAPTER NOTIFICATION TO NEXT FRAME
                        final int finalI = i;
                        mMainHandler.post(() -> {
                            try {
                                if (getAdapter() != null) {
                                    getAdapter().notifyItemRemoved(finalI);
                                    Log.d(TAG, mTAG + "Notified adapter of bottom loader removal");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, mTAG + "Error notifying adapter of loader removal: " + e.getMessage());
                            }
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error hiding bottom loader: " + e.getMessage());
        } finally {
            mShowingBottomLoader = false;
        }
    }

    // === UTILITY METHODS ===

    /**
     * Reset all control flags to initial state.
     * Used during initialization and cleanup.
     */
    private void resetControlFlags() {
        mIsUpdatingCache.set(false);
        mIsPendingTopLoad.set(false);
        mIsPendingBottomLoad.set(false);
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
            SharedViewModels.ViewItem item = mItemsCache.get(i);
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
        final String mTAG = "onStart: ";
        Log.v(TAG, mTAG + "called.");

        if (mQD != null) {
            mQD.updatePreferences(getActivity());
            if (mQD.isRefresh()) {
                mQD.setRefresh(false);
                refreshData();
            }
        }
    }

    /**
     * Handle fragment resume lifecycle.
     * Updates today's position and refreshes adapter.
     */
    @Override
    public void onResume() {
        super.onResume();
        final String mTAG = "onResume: ";
        Log.v(TAG, mTAG + "Fragment resumed: " + getClass().getSimpleName());

        // Update today's position in case date changed
        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);
        if (getAdapter() != null) {
            getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Handle fragment view destruction.
     * Cleans up handlers, cache, and listeners to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final String mTAG = "onDestroyView: ";
        Log.v(TAG, mTAG + "called.");

        // Cancel all pending operations
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }

        // Clean up resources
        if (mItemsCache != null) {
            mItemsCache.clear();
        }
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }

        if (DEBUG_FRAGMENT)
            Log.d(TAG, mTAG + "Fragment view destroyed: " + getClass().getSimpleName());
    }

    /**
     * Refresh data when preferences change.
     * Clears data manager cache and regenerates data.
     */
    public void refreshData() {
        final String mTAG = "refreshData: ";
        Log.v(TAG, mTAG + "called.");

        // Clear data manager cache
        mDataManager.clearCache();

        // Regenerate data
        setupInfiniteScrolling();
    }

    /**
     * Notify updates from external sources.
     * Updates user team and refreshes adapter.
     */
    public void notifyUpdates() {
        final String mTAG = "notifyUpdates: ";
        Log.v(TAG, mTAG + "called.");

        // Update user team if necessary
        onUserTeamChanged();

        if (getAdapter() != null) {
            getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Called when user team changes.
     * Subclasses can override for specific logic.
     */
    protected void onUserTeamChanged() {
        final String mTAG = "onUserTeamChanged: ";
        Log.v(TAG, mTAG + "User team changed");
    }

    // === LAYOUT MANAGER UTILITIES ===

    /**
     * Allow subclasses to set custom LayoutManager.
     * Maintains reference for LinearLayoutManager, sets null for others.
     *
     * @param layoutManager the LayoutManager to set
     */
    protected void setCustomLayoutManager(RecyclerView.LayoutManager layoutManager) {
        final String METHOD_TAG = TAG + " setCustomLayoutManager";

        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(layoutManager);
            if (layoutManager instanceof LinearLayoutManager) {
                mLayoutManager = (LinearLayoutManager) layoutManager;
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Set LinearLayoutManager");
            } else {
                // For GridLayoutManager or others, maintain null reference for compatibility
                mLayoutManager = null;
                if (DEBUG_FRAGMENT)
                    Log.d(METHOD_TAG, "Set non-LinearLayoutManager, mLayoutManager = null");
            }
        }
    }

    /**
     * Maintain scroll position adapting to LayoutManager type.
     * Handles null mLayoutManager gracefully.
     *
     * @param itemsAdded number of items added at the beginning
     */
    private void maintainScrollPosition(int itemsAdded) {
        final String mTAG = "maintainScrollPosition: ";

        if (mRecyclerView == null) return;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        try {
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                linearManager.scrollToPositionWithOffset(itemsAdded, 0);
                if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Maintained LinearLayoutManager position");
            } else if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
                int currentFirst = gridManager.findFirstVisibleItemPosition();
                if (currentFirst >= 0) {
                    gridManager.scrollToPosition(currentFirst + itemsAdded);
                    if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Maintained GridLayoutManager position");
                }
            } else {
                // Generic fallback for other LayoutManager types
                layoutManager.scrollToPosition(itemsAdded);
                if (DEBUG_FRAGMENT) Log.d(TAG, mTAG + "Maintained generic LayoutManager position");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error maintaining scroll position: " + e.getMessage());
        }
    }

    /**
     * Get first visible position regardless of LayoutManager type.
     *
     * @return first visible position or NO_POSITION if not determinable
     */
    protected int getFirstVisiblePosition() {
        final String mTAG = "getFirstVisiblePosition: ";

        if (mRecyclerView == null) return RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;

        try {
            if (layoutManager instanceof LinearLayoutManager) {
                return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            } else if (layoutManager instanceof GridLayoutManager) {
                return ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
            } else {
                // Generic approach for other LayoutManager types
                View firstChild = layoutManager.getChildAt(0);
                if (firstChild != null) {
                    return mRecyclerView.getChildAdapterPosition(firstChild);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error getting first visible position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Get last visible position regardless of LayoutManager type.
     *
     * @return last visible position or NO_POSITION if not determinable
     */
    protected int getLastVisiblePosition() {
        final String mTAG = "getLastVisiblePosition: ";

        if (mRecyclerView == null) return RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;

        try {
            if (layoutManager instanceof LinearLayoutManager) {
                return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof GridLayoutManager) {
                return ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else {
                // Generic approach for other LayoutManager types
                int childCount = layoutManager.getChildCount();
                if (childCount > 0) {
                    View lastChild = layoutManager.getChildAt(childCount - 1);
                    if (lastChild != null) {
                        return mRecyclerView.getChildAdapterPosition(lastChild);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error getting last visible position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Safe scroll to position that works with any LayoutManager.
     *
     * @param position target position to scroll to
     */
    protected void scrollToPositionSafely(int position) {
        final String METHOD_TAG = TAG + " scrollToPositionSafely";

        if (position < 0 || position >= mItemsCache.size() || mRecyclerView == null) return;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        try {
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPosition(position);
                if (DEBUG_FRAGMENT)
                    Log.d(METHOD_TAG, "LinearLayoutManager scrolled to: " + position);
            } else if (layoutManager instanceof GridLayoutManager) {
                ((GridLayoutManager) layoutManager).scrollToPosition(position);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "GridLayoutManager scrolled to: " + position);
            } else {
                // Generic approach
                mRecyclerView.scrollToPosition(position);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Generic scroll to: " + position);
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error scrolling to position: " + e.getMessage());
            // Fallback to generic RecyclerView method
            try {
                mRecyclerView.scrollToPosition(position);
            } catch (Exception fallbackError) {
                Log.e(METHOD_TAG, "Fallback scroll also failed: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * Smooth scroll sicuro che funziona con qualsiasi LayoutManager.
     */
    protected void smoothScrollToPositionSafely(int position) {
        if (position < 0 || position >= mItemsCache.size()) return;

        mRecyclerView.smoothScrollToPosition(position);
    }

    // =======================================================
    // === ABSTRACT METHODS THAT SUBCLASSES MUST IMPLEMENT ===

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
     * Subclasse create a specific Adapter instance
     * wich is given to the RecyclerView with mRecyclerView,setAdapter( adapter )
     */
    protected abstract void setupAdapter();

    /**
     * Get the current adapter.
     */
    protected abstract RecyclerView.Adapter<?> getAdapter();

    /**
     * Update FAB visibility based on the scroll position.
     */
    protected abstract void updateFabVisibility(int firstVisible, int lastVisible);
}