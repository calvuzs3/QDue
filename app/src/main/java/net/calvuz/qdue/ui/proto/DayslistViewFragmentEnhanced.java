package net.calvuz.qdue.ui.proto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.dayslist.SimpleEnhancedDaysListAdapter;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DayslistViewFragment with virtual scrolling integration
 * Minimal changes to existing code while gaining virtual scrolling benefits
 */
public class DayslistViewFragmentEnhanced extends EnhancedBaseFragmentBridge {

    private final String TAG = "DayslistViewFragmentEnhanced";

    // Keep existing adapter reference for compatibility
    private SimpleEnhancedDaysListAdapter mLegacyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        return root;
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        // Return bridge adapter if virtual scrolling is enabled, otherwise legacy
        return adapterBridge != null ? adapterBridge : mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        if (adapter instanceof SimpleEnhancedDaysListAdapter) {
            this.mLegacyAdapter = (SimpleEnhancedDaysListAdapter) adapter;
        }
        // Bridge adapter is handled by parent class
    }

    @Override
    protected void findViews(View view) {
        mRecyclerView = view.findViewById(R.id.rv_dayslist);
        mFabGoToToday = view.findViewById(R.id.fab_go_to_today);
    }

    /**
     * Return 1 column for list-like appearance
     */
    @Override
    protected int getGridColumnCount() {
        return 1; // Single column for list behavior
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return SharedViewModels.DataConverter.convertForDaysList(days, monthDate);
    }

    /**
     * Legacy adapter setup for fallback
     */
    protected void setupLegacyAdapter() {
        Log.v(TAG, "setupLegacyAdapter: called.");

        mLegacyAdapter = new SimpleEnhancedDaysListAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam(),
                3
        );
        mRecyclerView.setAdapter(mLegacyAdapter);

        // Aggiornare con eventi se disponibili
        if (mLegacyAdapter != null) {
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            if (!eventsCache.isEmpty()) {
                Log.d(TAG, "Found existing events cache with " + eventsCache.size() + " dates");
                mLegacyAdapter.updateEventsData(eventsCache);
            }
        }
    }

    @Override
    protected void setupAdapter() {
        // Try virtual scrolling first, fallback to legacy
        try {
            super.setupAdapter(); // This will use bridge adapter
            Log.d(TAG, "Virtual scrolling adapter initialized successfully");
        } catch (Exception e) {
            Log.w(TAG, "Virtual scrolling failed, falling back to legacy: " + e.getMessage());
            setupLegacyAdapter();
        }
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        Log.v(TAG, "updateFabVisibility: called.");
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
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
        return 3; // Show 3 shifts for days list
    }

    /**
     * Enhanced method to get current visible month for virtual scrolling
     */
    @Override
    protected LocalDate getCurrentVisibleMonth() {
        if (mRecyclerView == null || mGridLayoutManager == null) {
            return LocalDate.now().withDayOfMonth(1);
        }

        try {
            // Get first visible position
            int firstVisiblePosition = mGridLayoutManager.findFirstVisibleItemPosition();
            if (firstVisiblePosition >= 0 && firstVisiblePosition < mItemsCache.size()) {

                SharedViewModels.ViewItem item = mItemsCache.get(firstVisiblePosition);
                if (item instanceof SharedViewModels.DayItem) {
                    return ((SharedViewModels.DayItem) item).monthDate;
                } else if (item instanceof SharedViewModels.MonthHeader) {
                    return ((SharedViewModels.MonthHeader) item).monthDate;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining visible month: " + e.getMessage());
        }

        return LocalDate.now().withDayOfMonth(1);
    }

    @Override
    protected void notifyEventsDataChanged() {
        Log.d(TAG, "notifyEventsDataChanged: events updated in BaseFragment");

        if (mLegacyAdapter != null) {
            // Ottenere eventi dalla cache BaseFragment
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            Log.d(TAG, "Passing " + eventsCache.size() + " dates with events to DaysListAdapter");

            // Passare eventi all'adapter
            mLegacyAdapter.updateEventsData(eventsCache);
        } else {
            Log.w(TAG, "mLegacyAdapter is null in notifyEventsDataChanged");
        }

        super.notifyEventsDataChanged();
    }
}
