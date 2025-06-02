
package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.QDue.Debug.DEBUG_FRAGMENT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * Fragment per la visualizzazione calendario dei turni dell'utente.
 * Utilizza l'adapter base unificato specializzato per la vista calendario.
 */
public class CalendarViewFragment extends BaseFragment {

    private static final String TAG = "CalendarViewFragment";

    private CalendarAdapter mAdapter;
private GridLayoutManager mGridLayoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    protected void findViews(View rootView) {
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
    }

    @Override
    protected void setupRecyclerView() {
        if (mRecyclerView == null) {
            Log.e(TAG, "mRecyclerView is null");
            return;
        }

        // For calendar, use GridLayoutManager with 7 columns
        mGridLayoutManager = new GridLayoutManager(getContext(), 7);
        // Configure span size for headers
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < mItemsCache.size()) {
                    SharedViewModels.ViewItem item = mItemsCache.get(position);
                    if (item instanceof SharedViewModels.MonthHeader) {
                        return 7; // Header occupies full width
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
     * Handles loading for calendar grid.
     * FIXED: Operations are posted to next frame.
     */
    private void handleGridScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        // For 7x6 grid, calculate how many "rows" we are from start/end
        int firstRow = firstVisible / 7;
        int lastRow = lastVisible / 7;
        int totalRows = (mItemsCache.size() + 6) / 7; // Round up

        // Load upward (first 2 rows visible)
        if (firstRow <= 2 && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (DEBUG_FRAGMENT) Log.d(TAG, "Triggering top load at row: " + firstRow);

            // POST TO NEXT FRAME TO AVOID SCROLL CALLBACK ISSUES
            mMainHandler.post(() -> triggerTopLoad());
        }

        // Load downward (last 2 rows visible)
        if (lastRow >= totalRows - 3 && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (DEBUG_FRAGMENT) Log.d(TAG, "Triggering bottom load at row: " + lastRow);

            // POST TO NEXT FRAME TO AVOID SCROLL CALLBACK ISSUES
            mMainHandler.post(() -> triggerBottomLoad());
        }
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
}