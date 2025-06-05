package net.calvuz.qdue.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

import static net.calvuz.qdue.QDue.Debug.DEBUG_FRAGMENT;

/**
 * Fragment per la visualizzazione calendario dei turni dell'utente.
 * Utilizza l'adapter base unificato specializzato per la vista calendario.
 */
public class CalendarViewFragment extends BaseFragment {

    // TAG
    private static final String TAG = "CalendarViewFragment";

    // Members
    private CalendarAdapter mAdapter;
    private GridLayoutManager mGridLayoutManager;
    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar - base class doesn't
        setupToolbar(view);
    }

    @Override
    protected void findViews(View rootView) {

        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
        mToolbar = rootView.findViewById(R.id.toolbar);
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // For calendar, convert including empty cells to complete the grid
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        if (DEBUG_FRAGMENT) Log.v(TAG, "setupAdapter");

        mAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                mQD.getUserHalfTeam()
        );
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected RecyclerView.Adapter<?> getAdapter() {
        return mAdapter;
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            // For grid calendar, calculate if today is visible
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

    @Override
    protected void onUserTeamChanged() {
        super.onUserTeamChanged();
        if (mAdapter != null) {
            mAdapter.updateUserTeam(mQD.getUserHalfTeam());
        }
    }

    /**
     * Override infinite scrolling setup for GridLayoutManager.
     */
    @Override
    protected void setupInfiniteScrolling() {
        // Base class uses LinearLayoutManager, but calendar uses GridLayoutManager
        // Re-implement only scroll detection part
        super.setupInfiniteScrolling(false); // Don't use base layout manager

        // Replace listener to handle GridLayoutManager
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
            mRecyclerView.addOnScrollListener(new CalendarScrollListener());
        }
    }

    /**
     * Custom listener for calendar GridLayoutManager.
     * FIXED: Posts adapter changes to next frame to avoid scroll callback conflicts.
     */
    private class CalendarScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - mLastScrollTime < 100) return; // Throttling
            mLastScrollTime = currentTime;

            GridLayoutManager gridManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            if (gridManager == null) return;

            int firstVisible = gridManager.findFirstVisibleItemPosition();
            int lastVisible = gridManager.findLastVisibleItemPosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Handle loading based on scroll for grid
            handleGridScrollBasedLoading(firstVisible, lastVisible, dy);

            // Update FAB visibility
            updateFabVisibility(firstVisible, lastVisible);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;
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
     * Calculates effective grid position excluding headers.
     * Headers span 7 cells but only count as 1 item in calculations.
     */
    private int calculateEffectiveGridPosition(int position) {
        if (position < 0 || position >= mItemsCache.size()) return position;

        int effectivePosition = 0;
        int headerCount = 0;

        for (int i = 0; i <= position && i < mItemsCache.size(); i++) {
            SharedViewModels.ViewItem item = mItemsCache.get(i);
            if (item instanceof SharedViewModels.MonthHeader) {
                headerCount++;
            } else {
                effectivePosition++;
            }
        }

        // Each header takes up space equivalent to 1 row (7 cells) in grid calculations
        return effectivePosition + (headerCount * 7);
    }

    /**
     * Calculates total effective items for proper grid boundary detection.
     */
    private int calculateTotalEffectiveItems() {
        int effectiveItems = 0;
        int headerCount = 0;

        for (SharedViewModels.ViewItem item : mItemsCache) {
            if (item instanceof SharedViewModels.MonthHeader) {
                headerCount++;
            } else {
                effectiveItems++;
            }
        }

        return effectiveItems + (headerCount * 7);
    }

    /**
     * Scroll to initial appropriate position.
     * Protected - GridLayoutManager needs to scroll to initial position by itself.
     */
    @Override
    protected void scrollToInitialPosition() {
        if (mTodayPosition >= 0) {
            mGridLayoutManager.scrollToPosition(mTodayPosition);
        } else {
            mGridLayoutManager.scrollToPosition(mCurrentCenterPosition);
        }
    }

    /**
     * OVERRIDE: Grid-specific initial scroll with better positioning
     */
    protected void scrollToInitialPositionEnhanced() {
        final String METHOD_TAG = TAG + " scrollToInitialPositionEnhanced";

        if (mRecyclerView == null || mGridLayoutManager == null) {
            Log.e(METHOD_TAG, "RecyclerView or GridLayoutManager is null");
            return;
        }

        // CRITICAL: Verify today position is valid before scrolling
        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            try {
                Log.d(METHOD_TAG, "Grid scrolling to today at position: " + mTodayPosition);

                // For grid, use scrollToPositionWithOffset for better control
                // Offset by a few rows to show context around today
                int offset = calculateGridOffset();
                mGridLayoutManager.scrollToPositionWithOffset(mTodayPosition, offset);

                Log.d(METHOD_TAG, "Successfully scrolled to today with offset: " + offset);

            } catch (Exception e) {
                Log.e(METHOD_TAG, "Error scrolling to today: " + e.getMessage());
                // Fallback to simple scroll
                try {
                    mGridLayoutManager.scrollToPosition(mTodayPosition);
                } catch (Exception fallbackError) {
                    Log.e(METHOD_TAG, "Fallback grid scroll also failed: " + fallbackError.getMessage());
                }
            }
        } else {
            Log.w(METHOD_TAG, "Today position invalid (" + mTodayPosition + "), scrolling to center");
            try {
                mGridLayoutManager.scrollToPosition(mCurrentCenterPosition);
            } catch (Exception e) {
                Log.e(METHOD_TAG, "Center grid scroll failed: " + e.getMessage());
            }
        }
    }

    /**
     * NEW: Calculate appropriate offset for grid scroll to show context
     */
    private int calculateGridOffset() {
        // For calendar grid, offset by 2-3 rows (14-21 cells) to show context
        // This ensures today isn't at the very top of the screen
        return -getResources().getDimensionPixelSize(R.dimen.calendar_day_height) * 3;
    }

    /**
     * ENHANCED: Override scroll to today for grid-specific behavior
     */
    @Override
    public void scrollToToday() {
        final String METHOD_TAG = TAG + " scrollToToday";

        // First, try to find today in current cache
        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            // Today is in cache - scroll to it with grid-specific positioning
            Log.d(METHOD_TAG, "Today found in cache at position: " + mTodayPosition);

            if (mGridLayoutManager != null) {
                // For grid, use smooth scroll with offset for better positioning
                mRecyclerView.post(() -> {
                    try {
                        // First scroll near the position
                        int targetPosition = Math.max(0, mTodayPosition - 14); // ~2 weeks before
                        mGridLayoutManager.scrollToPosition(targetPosition);

                        // Then smooth scroll to exact position
                        mRecyclerView.postDelayed(() -> {
                            mRecyclerView.smoothScrollToPosition(mTodayPosition);
                        }, 100);
                    } catch (Exception e) {
                        Log.e(METHOD_TAG, "Error in grid scroll to today: " + e.getMessage());
                        mRecyclerView.smoothScrollToPosition(mTodayPosition);
                    }
                });
            } else {
                mRecyclerView.smoothScrollToPosition(mTodayPosition);
            }
            return;
        }

        // Today is not in cache - use base class smart loading
        super.scrollToToday();
    }

    /**
     * Override setupRecyclerView to fix span size for loading items
     */
    @Override
    protected void setupRecyclerView() {
        if (mRecyclerView == null) {
            Log.e(TAG, "mRecyclerView is null");
            return;
        }

        // For calendar, use GridLayoutManager with 7 columns
        mGridLayoutManager = new GridLayoutManager(getContext(), 7);

        // FIXED: Configure span size for headers AND loading items
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < mItemsCache.size()) {
                    SharedViewModels.ViewItem item = mItemsCache.get(position);
                    if (item instanceof SharedViewModels.MonthHeader) {
                        return 7; // Header occupies full width
                    }
                    // FIX: Loading items should also occupy full width
                    if (item instanceof SharedViewModels.LoadingItem) {
                        return 7; // Loading occupies full width
                    }
                }
                return 1; // Days occupy 1 cell
            }
        });

        mRecyclerView.setLayoutManager(mGridLayoutManager);

        // Base class LinearLayoutManager is not needed
        mLayoutManager = null;

        // Optimizations
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
    }

    /**
     * DIAGNOSTIC: Enhanced debugging for grid scroll issues
     */
    private void debugGridState() {
        if (!DEBUG_FRAGMENT) return;

        if (mGridLayoutManager != null) {
            int firstVisible = mGridLayoutManager.findFirstVisibleItemPosition();
            int lastVisible = mGridLayoutManager.findLastVisibleItemPosition();
            int firstCompletelyVisible = mGridLayoutManager.findFirstCompletelyVisibleItemPosition();
            int lastCompletelyVisible = mGridLayoutManager.findLastCompletelyVisibleItemPosition();

            Log.d(TAG, "Grid State - First: " + firstVisible + ", Last: " + lastVisible);
            Log.d(TAG, "Grid State - FirstComplete: " + firstCompletelyVisible + ", LastComplete: " + lastCompletelyVisible);
            Log.d(TAG, "Grid State - Today position: " + mTodayPosition + ", Cache size: " + mItemsCache.size());

            // Check if today is actually visible
            if (mTodayPosition >= 0) {
                boolean todayVisible = mTodayPosition >= firstVisible && mTodayPosition <= lastVisible;
                Log.d(TAG, "Grid State - Today visible: " + todayVisible);
            }
        }
    }

    /**
     * DIAGNOSTIC: Add method to check for invisible loaders
     */
    private void debugCacheState() {
        if (!DEBUG_FRAGMENT) return;

        int headers = 0, days = 0, empty = 0, loading = 0;

        for (int i = 0; i < mItemsCache.size(); i++) {
            SharedViewModels.ViewItem item = mItemsCache.get(i);
            String type = "UNKNOWN";

            if (item instanceof SharedViewModels.MonthHeader) {
                headers++;
                type = "HEADER";
            } else if (item instanceof SharedViewModels.DayItem) {
                days++;
                type = "DAY";
            } else if (item instanceof SharedViewModels.EmptyItem) {
                empty++;
                type = "EMPTY";
            } else if (item instanceof SharedViewModels.LoadingItem) {
                loading++;
                type = "LOADING";
                SharedViewModels.LoadingItem loadingItem = (SharedViewModels.LoadingItem) item;
                Log.w(TAG, "FOUND LOADING ITEM at position " + i + " type: " + loadingItem.loadingType);
            }

            if (i < 10 || i > mItemsCache.size() - 10) {
                Log.d(TAG, "Position " + i + ": " + type);
            }
        }

        Log.d(TAG, "Cache state - Headers: " + headers + ", Days: " + days + ", Empty: " + empty + ", Loading: " + loading);

        // Check for flags
        Log.d(TAG, "Flags - ShowingTopLoader: " + mShowingTopLoader + ", ShowingBottomLoader: " + mShowingBottomLoader);
        Log.d(TAG, "Flags - PendingTopLoad: " + mIsPendingTopLoad.get() + ", PendingBottomLoad: " + mIsPendingBottomLoad.get());
    }

    /**
     * Override scroll methods to add debugging
     */
    private void handleGridScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        // Add debugging at start
        debugCacheState();

        // Calculate effective grid positions excluding headers
        int effectiveFirst = calculateEffectiveGridPosition(firstVisible);
        int effectiveLast = calculateEffectiveGridPosition(lastVisible);
        int totalEffectiveItems = calculateTotalEffectiveItems();

        // More conservative trigger zones to prevent frequent loading
        int loadTriggerZone = 21; // INCREASED: 3 full weeks to be extra conservative

        // Load upward - only when very close to top
        if (effectiveFirst <= loadTriggerZone && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (DEBUG_FRAGMENT)
                Log.d(TAG, "Triggering top load at effective position: " + effectiveFirst);

            // Delay the trigger slightly to ensure scroll has stabilized
            mMainHandler.postDelayed(() -> {
                // Double-check conditions before triggering
                if (!mIsUpdatingCache.get() && !mIsPendingTopLoad.get()) {
                    triggerTopLoad();
                }
            }, 100); // INCREASED delay
        }

        // Load downward - only when very close to bottom
        if (effectiveLast >= totalEffectiveItems - loadTriggerZone && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (DEBUG_FRAGMENT)
                Log.d(TAG, "Triggering bottom load at effective position: " + effectiveLast);

            // Delay the trigger slightly to ensure scroll has stabilized
            mMainHandler.postDelayed(() -> {
                // Double-check conditions before triggering
                if (!mIsUpdatingCache.get() && !mIsPendingBottomLoad.get()) {
                    triggerBottomLoad();
                }
            }, 100); // INCREASED delay
        }
    }

    /**
     * FIXED: Setup toolbar
     * it has been removed from land/layouts
     */
    private void setupToolbar(View root) {
        final String mTAG = "setupToolbar: ";

        if (mToolbar == null) {
            Log.e(TAG, mTAG + "Toolbar not found in fragment layout");
            return;
        }

        try {
            // Set toolbar as ActionBar for this fragment's activity
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity =
                        (androidx.appcompat.app.AppCompatActivity) getActivity();
                activity.setSupportActionBar(mToolbar);

                // Configure ActionBar
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
                }
            }

            // CRITICAL: Set menu item click listener
            mToolbar.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();
                Log.v(TAG, mTAG + "onMenuItemClickListener() -> ("
                        + id + ") \n"
                        + item.getTitle());
                try {
                    if (item.getTitle() == (String) getResources().getString(R.string.go_to_today))
                        Log.v(TAG, mTAG + "stringhe coincidenti");
                    scrollToToday();
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error: " + e.getMessage());
                }

                try {
                    if (id == R.id.action_about) {
                        navigateTo(R.id.nav_about);
                        return true;
                    }
                    if (id == R.id.action_settings) {
                        navigateTo(R.id.nav_settings);
                        return true;
                    }
                    if (id == R.id.fab_go_to_today) {
                        Log.e(TAG, mTAG + "FAB found as a menu item in setuptoolbar");
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "onMenuItemClick failed: " + e.getMessage());
                }

                Log.v(TAG, mTAG + "onMenuItemClickListener() ->" +
                        " got (" + id + ")" + " expected (" + R.id.fab_go_to_today + ") \n");
                return true;
            });

            // Enable options menu for this fragment
            setHasOptionsMenu(true);

            Log.d(TAG, mTAG + "Fragment toolbar setup complete");

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error setting up fragment toolbar: " + e.getMessage());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuInflater menuiflater = getActivity().getMenuInflater();
        menuiflater.inflate(R.menu.toolbar_menu, menu);
    }
}