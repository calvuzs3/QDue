package net.calvuz.qdue.ui.proto;

import static net.calvuz.qdue.QDue.VirtualScrollingSettings.ENABLE_VIRTUAL_SCROLLING;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.features.calendar.CalendarAdapter;
import net.calvuz.qdue.ui.architecture.base.BaseAdapter;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * Enhanced CalendarViewFragment with virtual scrolling integration
 * Maintains calendar grid layout while gaining virtual scrolling performance
 */
public class CalendarViewFragmentEnhanced extends EnhancedBaseFragmentBridge {

    private static final String TAG = "F-Calendar";

    // Keep existing adapter reference for compatibility
    private CalendarAdapter mLegacyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    protected void findViews(View rootView) {
        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
    }

    /**
     * Return 7 columns for calendar grid layout
     */
    @Override
    protected int getGridColumnCount() {
        return 7; // Seven columns for calendar days (Sun-Sat)
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // For calendar, convert including empty cells to complete the grid
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    /**
     * Legacy adapter setup for fallback
     */
    protected void setupLegacyAdapter() {
        Log.v(TAG, "setupLegacyAdapter: called.");

        mLegacyAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam()
        );
        mRecyclerView.setAdapter(mLegacyAdapter);
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        if (ENABLE_VIRTUAL_SCROLLING && adapterBridge != null) {
            Log.d(TAG, "getFragmentAdapter: ✅ AdapterBridge (virtual mode)");
            return adapterBridge;
        }

        Log.d(TAG, "getFragmentAdapter: Legacy (CalendarAdapter)");
        return mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        if (ENABLE_VIRTUAL_SCROLLING && adapter instanceof AdapterBridge) {
            // ✅ Virtual mode: store bridge adapter reference
            // Non serve mLegacyAdapter in virtual mode
            Log.d(TAG, "setFragmentAdapter: ✅ Virtual adapter bridge set");
            return;
        }

        if (adapter instanceof CalendarAdapter) {
            this.mLegacyAdapter = (CalendarAdapter) adapter;
            Log.d(TAG, "setFragmentAdapter: Legacy CalendarAdapter set");
        }
    }

    @Override
    protected void setupAdapter() {
        final String mTAG = "setupAdapter: ";
        Log.d(TAG, mTAG + "called.");

        if (ENABLE_VIRTUAL_SCROLLING) {
            // ✅ ORDER: Create adapter first
            adapterBridge = new AdapterBridge(
                    getContext(),
                    mItemsCache,
                    getHalfTeam(),
                    getShiftsToShow()
            );

            // ✅ ORDER: Attach to RecyclerView immediately
            if (mRecyclerView != null) {
                mRecyclerView.setAdapter(adapterBridge);
                Log.d(TAG, mTAG + "✅ Bridge adapter attached");
            }

            // ✅ ORDER: Set fragment adapter reference
            setFragmentAdapter(adapterBridge);

            // ✅ ORDER: Load data last
            loadInitialVirtualData();

            Log.d(TAG, mTAG + "Bridge adapter setup completed");
            return;
        }

        setupLegacyAdapter();
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            // For grid calendar, calculate if today is visible considering 7-column layout
            int firstVisibleRow = firstVisible / 7;
            int lastVisibleRow = lastVisible / 7;
            int todayRow = mTodayPosition / 7;

            showFab = !(firstVisibleRow <= todayRow && lastVisibleRow >= todayRow);
        }

        // Animate FAB visibility changes
        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.setVisibility(View.VISIBLE);
            mFabGoToToday.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(150)
                    .withEndAction(() -> mFabGoToToday.setVisibility(View.GONE)).start();
        }
    }

    // ==================== BRIDGE INTERFACE IMPLEMENTATIONS ====================

    @Override
    protected HalfTeam getHalfTeam() {
        return QDue.getQuattrodue().getUserHalfTeam();
    }

    @Override
    protected int getShiftsToShow() {
        return 1; // (was 7) Show all shifts for calendar view
    }

    /**
     * Enhanced method to get current visible month for virtual scrolling
     * Calendar-specific logic for 7-column grid
     */
    @Override
    protected LocalDate getCurrentVisibleMonth() {
        if (mRecyclerView == null || mGridLayoutManager == null) {
            return LocalDate.now().withDayOfMonth(1);
        }

        try {
            // For calendar grid, look at middle visible position for better accuracy
            int firstVisible = mGridLayoutManager.findFirstVisibleItemPosition();
            int lastVisible = mGridLayoutManager.findLastVisibleItemPosition();

            if (firstVisible >= 0 && lastVisible >= 0) {
                int middlePosition = (firstVisible + lastVisible) / 2;

                if (middlePosition < mItemsCache.size()) {
                    SharedViewModels.ViewItem item = mItemsCache.get(middlePosition);

                    if (item instanceof SharedViewModels.DayItem) {
                        return ((SharedViewModels.DayItem) item).monthDate;
                    } else if (item instanceof SharedViewModels.MonthHeader) {
                        return ((SharedViewModels.MonthHeader) item).monthDate;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining visible month: " + e.getMessage());
        }

        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Calendar-specific day click handling
     */
    protected void onDayClicked(LocalDate date, Day dayData) {
        Log.d(TAG, "Day clicked: " + date);

        // Add your calendar-specific day click logic here
        // For example, show day details, edit shifts, etc.

        // Example: Show day detail dialog
        // showDayDetailDialog(date, dayData);
    }

    /**
     * Calendar-specific month header updates
     */
    protected void onMonthChanged(LocalDate newMonth) {
        Log.d(TAG, "Month changed to: " + newMonth);

        // Update any calendar-specific UI elements
        // For example, update toolbar title, mini calendar, etc.

        // Example: Update activity title
        if (getActivity() != null) {
            // getActivity().setTitle(newMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
    }

    /**
     * Override da BaseFragment - chiamato quando eventi cambiano nel database
     */
    @Override
    public void notifyEventsDataChanged() {
        final String mTAG = "notifyEventsDataChanged: ";
        Log.v(TAG, mTAG + "called");

        if (ENABLE_VIRTUAL_SCROLLING && adapterBridge != null) {
            // ✅ VIRTUAL: Notify bridge adapter about events data change
            notifyVirtualEventsDataChanged();
            return;
        }

        // ✅ LEGACY: Notify legacy adapter
        if (mLegacyAdapter != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mLegacyAdapter.notifyEventsDataChanged();
                    Log.d(TAG, mTAG + "Legacy adapter notified");
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error notifying legacy adapter: " + e.getMessage());
                }
            });
        }

        Log.v(TAG, mTAG + "completed");
    }

    /**
     * Notify virtual events data changed
     */
    private void notifyVirtualEventsDataChanged() {
        final String mTAG = "notifyVirtualEventsDataChanged: ";
        Log.v(TAG, mTAG + "called");

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (adapterBridge != null) {
                    // ✅ Method 1: Force adapter refresh
//                    adapterBridge.notifyDataSetChanged();

                    // ✅ Method 2: Request fresh data
                        adapterBridge.requestEventsRefresh();

                    // ✅ Method 3: Reload virtual data
//                    loadInitialVirtualData();

                    Log.d(TAG, mTAG + "Bridge adapter notified and refreshed");
                }
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error notifying bridge adapter: " + e.getMessage());
            }
        });

        Log.v(TAG, mTAG + "completed");
    }
}