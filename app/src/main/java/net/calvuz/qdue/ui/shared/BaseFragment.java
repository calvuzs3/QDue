package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.QDue.Debug.DEBUG_FRAGMENT;
import static net.calvuz.qdue.quattrodue.Costants.QD_MAX_CACHE_SIZE;
import static net.calvuz.qdue.quattrodue.Costants.QD_MONTHS_CACHE_SIZE;

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
 *
 * This class provides:
 * - Infinite scrolling with intelligent caching
 * - Unified data management through CalendarDataManager
 * - Layout manager agnostic operations (works with LinearLayoutManager and GridLayoutManager)
 * - Thread-safe operations with proper scroll callback handling
 * - Memory management with automatic cache cleanup
 *
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

    /** Core QuattroDue instance for shift data */
    protected QuattroDue mQD;

    /** Centralized data manager for calendar operations */
    protected CalendarDataManager mDataManager;

    /** Main RecyclerView component */
    protected RecyclerView mRecyclerView;

    /** LinearLayoutManager reference (null for GridLayoutManager) */
    protected LinearLayoutManager mLayoutManager;

    /** Floating Action Button for "go to today" functionality */
    protected FloatingActionButton mFabGoToToday;

    // === INFINITE SCROLLING CACHE ===

    /** Cache of view items for infinite scrolling */
    protected List<SharedViewModels.ViewItem> mItemsCache;

    /** Current center position in cache */
    protected int mCurrentCenterPosition;

    /** Current date cursor */
    protected LocalDate mCurrentDate;

    // === CONCURRENCY CONTROL ===

    /** Atomic flag to prevent concurrent cache updates */
    protected final AtomicBoolean mIsUpdatingCache = new AtomicBoolean(false);

    /** Atomic flag for pending top load operations */
    protected final AtomicBoolean mIsPendingTopLoad = new AtomicBoolean(false);

    /** Atomic flag for pending bottom load operations */
    protected final AtomicBoolean mIsPendingBottomLoad = new AtomicBoolean(false);

    // === ASYNC OPERATION HANDLERS ===

    /** Main thread handler for UI operations */
    protected Handler mMainHandler;

    /** Background handler for data operations */
    protected Handler mBackgroundHandler;

    // === POSITION TRACKING ===

    /** Position of today in the cache (-1 if not found) */
    protected int mTodayPosition = -1;

    // === LOADING INDICATORS ===

    /** Flag indicating top loader is currently showing */
    protected boolean mShowingTopLoader = false;

    /** Flag indicating bottom loader is currently showing */
    protected boolean mShowingBottomLoader = false;

    // === SCROLL VELOCITY CONTROL ===

    /** Last scroll timestamp for throttling */
    protected long mLastScrollTime = 0;

    /** Current scroll velocity for performance optimization */
    protected int mScrollVelocity = 0;

    /** Maximum scroll velocity before ignoring updates */
    protected static final int MAX_SCROLL_VELOCITY = 25;

    /** Delay before processing operations after scroll settles */
    protected static final long SCROLL_SETTLE_DELAY = 150;

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

        final String METHOD_TAG = TAG + " onViewCreated";

        // Subclasses must implement findViews() to initialize their specific views
        findViews(view);

        // Configure the RecyclerView with appropriate LayoutManager
        setupRecyclerView();

        // Configure the Floating Action Button
        setupFAB();

        // Initialize QuattroDue and setup infinite scrolling
        if (getActivity() != null) {
            mQD = QDue.getQuattrodue();
            mDataManager.initialize(mQD);
            setupInfiniteScrolling();
        } else {
            Log.e(METHOD_TAG, "Activity is null, cannot initialize infinite scrolling");
        }
    }

    /**
     * Setup RecyclerView with support for custom LayoutManagers.
     *
     * This method handles both LinearLayoutManager (for DaysListFragment)
     * and GridLayoutManager (for CalendarFragment). Subclasses can override
     * this method to provide their own LayoutManager setup.
     */
    protected void setupRecyclerView() {
        final String METHOD_TAG = TAG + " setupRecyclerView";

        if (mRecyclerView == null) {
            Log.e(METHOD_TAG, "RecyclerView is null - subclass must implement findViews()");
            return;
        }

        // Check if LayoutManager already exists
        // Subclasses can override this method for specific LayoutManager types
        RecyclerView.LayoutManager existingLayoutManager = mRecyclerView.getLayoutManager();

        if (existingLayoutManager == null) {
            // Create and set new LinearLayoutManager as default
            mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(mLayoutManager);
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Created new LinearLayoutManager");
        } else if (existingLayoutManager instanceof LinearLayoutManager) {
            // Use existing LinearLayoutManager
            mLayoutManager = (LinearLayoutManager) existingLayoutManager;
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Using existing LinearLayoutManager");
        } else {
            // For other LayoutManager types (e.g., GridLayoutManager), keep null and use fallback methods
            mLayoutManager = null;
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Using non-LinearLayoutManager, set mLayoutManager to null");
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
        final String METHOD_TAG = TAG + " setupFAB";

        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> scrollToToday());
            mFabGoToToday.hide(); // Initially hidden
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "FAB configured and hidden");
        } else {
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "FAB not found in layout");
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
     *                        or let subclass handle it (e.g., CalendarViewFragment)
     */
    protected void setupInfiniteScrolling(boolean useLayoutManager) {
        final String METHOD_TAG = TAG + " setupInfiniteScrolling";

        if (mQD == null || mDataManager == null) {
            Log.e(METHOD_TAG, "Components not initialized, cannot setup infinite scrolling");
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

            Log.d(METHOD_TAG, "Infinite scroll setup completed: " + mItemsCache.size() +
                    " elements, today at position: " + mTodayPosition);

        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error during infinite scrolling setup: " + e.getMessage());
        }
    }

    /**
     * Optimized scroll listener that prevents adapter modifications during scroll callbacks.
     *
     * This listener implements:
     * - Velocity-based throttling to improve performance
     * - Deferred adapter operations to avoid scroll callback conflicts
     * - Automatic cleanup scheduling when scroll settles
     */
    private class OptimizedInfiniteScrollListener extends RecyclerView.OnScrollListener {

        private static final String LISTENER_TAG = TAG + " OptimizedInfiniteScrollListener";

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
                Log.v(LISTENER_TAG + " onScrollStateChanged", "Scroll state: " + stateStr + ", velocity: " + mScrollVelocity);
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
     *
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     */
    protected void executeTopLoad() {
        final String METHOD_TAG = TAG + " executeTopLoad";

        if (!mIsPendingTopLoad.compareAndSet(true, false)) return;
        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        try {
            LocalDate firstMonth = findFirstMonthInCache();
            if (firstMonth != null) {
                LocalDate prevMonth = firstMonth.minusMonths(1);

                // POST TO NEXT FRAME - CRITICAL FIX for scroll callback conflicts
                mMainHandler.post(() -> {
                    try {
                        List<SharedViewModels.ViewItem> newItems = generateMonthItems(prevMonth);
                        mItemsCache.addAll(0, newItems);

                        // Update positions
                        if (mTodayPosition >= 0) mTodayPosition += newItems.size();
                        mCurrentCenterPosition += newItems.size();

                        // POST ADAPTER NOTIFICATIONS TO NEXT FRAME
                        mMainHandler.post(() -> {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(0, newItems.size());
                            }
                        });

                        // Maintain scroll position - adapted for different LayoutManager types
                        maintainScrollPosition(newItems.size());

                        hideTopLoader();

                        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Added previous month: " + prevMonth);
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
                hideTopLoader();
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error loading previous month: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideTopLoader();
        }
    }

    /**
     * Execute bottom load operation (loading next months).
     *
     * This method is thread-safe and posts all adapter operations to the next frame
     * to avoid conflicts with scroll callbacks.
     */
    protected void executeBottomLoad() {
        final String METHOD_TAG = TAG + " executeBottomLoad";

        if (!mIsPendingBottomLoad.compareAndSet(true, false)) return;
        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        try {
            LocalDate lastMonth = findLastMonthInCache();
            if (lastMonth != null) {
                LocalDate nextMonth = lastMonth.plusMonths(1);

                // POST TO NEXT FRAME - CRITICAL FIX for scroll callback conflicts
                mMainHandler.post(() -> {
                    try {
                        List<SharedViewModels.ViewItem> newItems = generateMonthItems(nextMonth);
                        int insertPos = mItemsCache.size();
                        mItemsCache.addAll(newItems);

                        // Check if today is in new elements
                        if (mTodayPosition < 0) {
                            int todayInNew = SharedViewModels.DataConverter.findTodayPosition(newItems);
                            if (todayInNew >= 0) {
                                mTodayPosition = insertPos + todayInNew;
                            }
                        }

                        // POST ADAPTER NOTIFICATIONS TO NEXT FRAME
                        mMainHandler.post(() -> {
                            if (getAdapter() != null) {
                                getAdapter().notifyItemRangeInserted(insertPos, newItems.size());
                            }
                        });

                        hideBottomLoader();

                        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Added next month: " + nextMonth);
                    } finally {
                        mIsUpdatingCache.set(false);
                    }
                });
            } else {
                mIsUpdatingCache.set(false);
                hideBottomLoader();
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error loading next month: " + e.getMessage());
            mIsUpdatingCache.set(false);
            hideBottomLoader();
        }
    }

    /**
     * Handle loading based on scroll position.
     * Triggers top or bottom load when user scrolls near the edges.
     *
     * @param firstVisible first visible item position
     * @param lastVisible last visible item position
     * @param scrollDirection scroll direction (negative = up, positive = down)
     */
    private void handleScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        final String METHOD_TAG = TAG + " handleScrollBasedLoading";

        // Load upward (previous months) when near top
        if (firstVisible <= 10 && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Triggering top load at position: " + firstVisible);
            triggerTopLoad();
        }

        // Load downward (next months) when near bottom
        if (lastVisible >= mItemsCache.size() - 10 && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Triggering bottom load at position: " + lastVisible);
            triggerBottomLoad();
        }
    }

    /**
     * Trigger top load operation (previous months).
     * Sets up loading state and schedules execution.
     */
    protected void triggerTopLoad() {
        final String METHOD_TAG = TAG + " triggerTopLoad";

        mIsPendingTopLoad.set(true);
        showTopLoader();
        mBackgroundHandler.postDelayed(this::executeTopLoad, 100);

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Top load triggered");
    }

    /**
     * Trigger bottom load operation (next months).
     * Sets up loading state and schedules execution.
     */
    protected void triggerBottomLoad() {
        final String METHOD_TAG = TAG + " triggerBottomLoad";

        mIsPendingBottomLoad.set(true);
        showBottomLoader();
        mBackgroundHandler.postDelayed(this::executeBottomLoad, 100);

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Bottom load triggered");
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
        final String METHOD_TAG = TAG + " processPendingOperations";

        if (mIsPendingTopLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Processing pending top load");
            executeTopLoad();
        }
        if (mIsPendingBottomLoad.get() && !mIsUpdatingCache.get()) {
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Processing pending bottom load");
            executeBottomLoad();
        }
    }

    /**
     * Schedule cache cleanup if cache size exceeds limits.
     * Prevents memory issues by cleaning old cache entries when cache grows too large.
     */
    protected void scheduleCleanupIfNeeded() {
        final String METHOD_TAG = TAG + " scheduleCleanupIfNeeded";

        int maxElements = QD_MAX_CACHE_SIZE * 35; // ~35 elements per month
        if (mItemsCache.size() > maxElements) {
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Scheduling cache cleanup, current size: " + mItemsCache.size());

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
        final String METHOD_TAG = TAG + " cleanupCache";

        if (!mIsUpdatingCache.compareAndSet(false, true)) return;

        mMainHandler.post(() -> {
            try {
                // Get current position safely for any LayoutManager type
                int currentPos = getCurrentVisiblePosition();
                int targetSize = QD_MONTHS_CACHE_SIZE * 35;

                // If we can't determine position, use a safe fallback
                if (currentPos == RecyclerView.NO_POSITION) {
                    currentPos = mItemsCache.size() / 2; // Use middle as fallback
                    if (DEBUG_FRAGMENT) Log.w(METHOD_TAG, "Using fallback position for cleanup");
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

                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Cache cleaned, new size: " + mItemsCache.size());
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
        final String METHOD_TAG = TAG + " getCurrentVisiblePosition";

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
            Log.e(METHOD_TAG, "Error getting current position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Scroll to initial appropriate position.
     * Works safely with both LinearLayoutManager and GridLayoutManager.
     */
    protected void scrollToInitialPosition() {
        final String METHOD_TAG = TAG + " scrollToInitialPosition";

        if (mRecyclerView == null) return;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        try {
            int targetPosition = mTodayPosition >= 0 ? mTodayPosition : mCurrentCenterPosition;

            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPosition(targetPosition);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "LinearLayoutManager scrolled to position: " + targetPosition);
            } else if (layoutManager instanceof GridLayoutManager) {
                ((GridLayoutManager) layoutManager).scrollToPosition(targetPosition);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "GridLayoutManager scrolled to position: " + targetPosition);
            } else {
                // Generic fallback for any other LayoutManager
                mRecyclerView.scrollToPosition(targetPosition);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Generic scroll to position: " + targetPosition);
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error in scrollToInitialPosition: " + e.getMessage());

            // Final fallback attempt
            try {
                if (mTodayPosition >= 0) {
                    mRecyclerView.scrollToPosition(mTodayPosition);
                } else {
                    mRecyclerView.scrollToPosition(mCurrentCenterPosition);
                }
            } catch (Exception fallbackError) {
                Log.e(METHOD_TAG, "Fallback scroll also failed: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * Scroll to today's date.
     * If today is not in cache, rebuilds cache centered on today.
     */
    protected void scrollToToday() {
        final String METHOD_TAG = TAG + " scrollToToday";

        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            mRecyclerView.smoothScrollToPosition(mTodayPosition);
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Scrolling to today, position: " + mTodayPosition);
        } else {
            // Rebuild cache centered on today
            if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Today not in cache, rebuilding");
            mCurrentDate = LocalDate.now().withDayOfMonth(1);
            setupInfiniteScrolling();
        }
    }

    // === LOADING INDICATOR MANAGEMENT ===

    /**
     * Show top loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     */
    private void showTopLoader() {
        final String METHOD_TAG = TAG + " showTopLoader";

        if (mShowingTopLoader) return;
        mShowingTopLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.TOP);
        mItemsCache.add(0, loader);

        if (mTodayPosition >= 0) mTodayPosition++;
        mCurrentCenterPosition++;

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getAdapter() != null) {
                getAdapter().notifyItemInserted(0);
            }
        });

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Top loader shown");
    }

    /**
     * Show bottom loading indicator.
     * Adds loading item to cache and notifies adapter on next frame.
     */
    private void showBottomLoader() {
        final String METHOD_TAG = TAG + " showBottomLoader";

        if (mShowingBottomLoader) return;
        mShowingBottomLoader = true;

        SharedViewModels.LoadingItem loader = new SharedViewModels.LoadingItem(
                SharedViewModels.LoadingItem.LoadingType.BOTTOM);
        mItemsCache.add(loader);

        // POST ADAPTER NOTIFICATION TO NEXT FRAME
        mMainHandler.post(() -> {
            if (getAdapter() != null) {
                getAdapter().notifyItemInserted(mItemsCache.size() - 1);
            }
        });

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Bottom loader shown");
    }

    private void hideTopLoader() {
        if (!mShowingTopLoader) return;

        for (int i = 0; i < mItemsCache.size(); i++) {
            if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.TOP) {
                    mItemsCache.remove(i);
                    if (mTodayPosition > i) mTodayPosition--;
                    if (mCurrentCenterPosition > i) mCurrentCenterPosition--;

                    // POST ADAPTER NOTIFICATION TO NEXT FRAME
                    final int finalI = i;
                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRemoved(finalI);
                        }
                    });
                    break;
                }
            }
        }
        mShowingTopLoader = false;
    }

    /**
     * Hide bottom loading indicator.
     * Removes loading item from cache and notifies adapter on next frame.
     */
    private void hideBottomLoader() {
        final String METHOD_TAG = TAG + " hideBottomLoader";

        if (!mShowingBottomLoader) return;

        for (int i = mItemsCache.size() - 1; i >= 0; i--) {
            if (mItemsCache.get(i) instanceof SharedViewModels.LoadingItem) {
                SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) mItemsCache.get(i);
                if (loading.loadingType == SharedViewModels.LoadingItem.LoadingType.BOTTOM) {
                    mItemsCache.remove(i);

                    // POST ADAPTER NOTIFICATION TO NEXT FRAME
                    final int finalI = i;
                    mMainHandler.post(() -> {
                        if (getAdapter() != null) {
                            getAdapter().notifyItemRemoved(finalI);
                        }
                    });
                    break;
                }
            }
        }
        mShowingBottomLoader = false;

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Bottom loader hidden");
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
        final String METHOD_TAG = TAG + " onStart";

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Fragment started: " + getClass().getSimpleName());

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
        final String METHOD_TAG = TAG + " onResume";

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Fragment resumed: " + getClass().getSimpleName());

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
        final String METHOD_TAG = TAG + " onDestroyView";

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

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Fragment view destroyed: " + getClass().getSimpleName());
    }

    /**
     * Refresh data when preferences change.
     * Clears data manager cache and regenerates data.
     */
    public void refreshData() {
        final String METHOD_TAG = TAG + " refreshData";

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Data refresh requested");

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
        final String METHOD_TAG = TAG + " notifyUpdates";

        if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "External updates notified");

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
        final String METHOD_TAG = TAG + " onUserTeamChanged";
        Log.d(METHOD_TAG, "User team changed");
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
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Set non-LinearLayoutManager, mLayoutManager = null");
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
        final String METHOD_TAG = TAG + " maintainScrollPosition";

        if (mRecyclerView == null) return;

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        try {
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                linearManager.scrollToPositionWithOffset(itemsAdded, 0);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Maintained LinearLayoutManager position");
            } else if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
                int currentFirst = gridManager.findFirstVisibleItemPosition();
                if (currentFirst >= 0) {
                    gridManager.scrollToPosition(currentFirst + itemsAdded);
                    if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Maintained GridLayoutManager position");
                }
            } else {
                // Generic fallback for other LayoutManager types
                layoutManager.scrollToPosition(itemsAdded);
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "Maintained generic LayoutManager position");
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error maintaining scroll position: " + e.getMessage());
        }
    }


    /**
     * Get first visible position regardless of LayoutManager type.
     *
     * @return first visible position or NO_POSITION if not determinable
     */
    protected int getFirstVisiblePosition() {
        final String METHOD_TAG = TAG + " getFirstVisiblePosition";

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
            Log.e(METHOD_TAG, "Error getting first visible position: " + e.getMessage());
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Get last visible position regardless of LayoutManager type.
     *
     * @return last visible position or NO_POSITION if not determinable
     */
    protected int getLastVisiblePosition() {
        final String METHOD_TAG = TAG + " getLastVisiblePosition";

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
            Log.e(METHOD_TAG, "Error getting last visible position: " + e.getMessage());
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
                if (DEBUG_FRAGMENT) Log.d(METHOD_TAG, "LinearLayoutManager scrolled to: " + position);
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