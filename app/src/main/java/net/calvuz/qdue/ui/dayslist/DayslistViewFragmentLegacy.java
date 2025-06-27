package net.calvuz.qdue.ui.dayslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseAdapterLegacy;
import net.calvuz.qdue.ui.shared.BaseFragmentLegacy;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DayslistViewFragment with virtual scrolling integration
 * Minimal changes to existing code while gaining virtual scrolling benefits
 * ✅
 * ❌
 *
 * ==================== CORREZIONI DAYSLISTVIEWFRAGMENTLEGACY ====================
 *
 * DayslistViewFragmentLegacy Enhanced - Complete Events Integration
 *
 * CORREZIONI PRINCIPALI:
 * 1. Override metodi EventsRefreshInterface
 * 2. Implementazione corretta adapter events update
 * 3. Enhanced logging e debug
 * 4. Thread-safe operations
 */

public class DayslistViewFragmentLegacy extends BaseFragmentLegacy {

    private final String TAG = "DayslistLgsFrg";

    // Keep existing adapter reference for compatibility
    private DaysListAdapterLegacy mLegacyAdapter;

    // ==================== EXISTING CODE REMAINS UNCHANGED ====================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dayslist_view, container, false);
    }

    @Override
    protected BaseAdapterLegacy getFragmentAdapter() {
        return mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapterLegacy adapter) {
        if (adapter instanceof DaysListAdapterLegacy) {
            this.mLegacyAdapter = (DaysListAdapterLegacy) adapter;
        }
    }

    @Override
    protected void findViews(View view) {
        mRecyclerView = view.findViewById(R.id.rv_dayslist);
        mFabGoToToday = view.findViewById(R.id.fab_go_to_today);
    }

    @Override
    protected int getGridColumnCount() {
        return 1; // Single column for list behavior
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return SharedViewModels.DataConverter.convertForDaysList(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        setupLegacyAdapter();
        Log.d(TAG, "setupAdapter: ✅ Legacy adapter setup completed");
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

    // ==================== CORREZIONE 1: ENHANCED ADAPTER SETUP ====================

    /**
     * Enhanced legacy adapter setup with events integration
     */
    protected void setupLegacyAdapter() {
        final String mTAG = "setupLegacyAdapter: ";

        mLegacyAdapter = new DaysListAdapterLegacy(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam(),
                3
        );
        mRecyclerView.setAdapter(mLegacyAdapter);

        // ENHANCED: Update with events if available with better error handling
        updateAdapterWithEvents();

        Log.d(TAG, mTAG + "✅ Legacy adapter setup completed with events integration");
    }

    /**
     * Update adapter with current events data
     */
    private void updateAdapterWithEvents() {
        final String mTAG = "updateAdapterWithEvents: ";

        if (mLegacyAdapter == null) {
            Log.w(TAG, mTAG + "Adapter is null, cannot update with events");
            return;
        }

        try {
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            if (!eventsCache.isEmpty()) {
                Log.d(TAG, mTAG + "Found existing events cache with " + eventsCache.size() + " dates");
                mLegacyAdapter.updateEventsData(eventsCache);
                Log.d(TAG, mTAG + "✅ Adapter updated with events data");
            } else {
                Log.d(TAG, mTAG + "No events cache available yet");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error updating adapter with events: " + e.getMessage());
        }
    }

    // ==================== CORREZIONE 2: COMPLETE EVENTS REFRESH IMPLEMENTATION ====================

    /**
     * OVERRIDE: Enhanced implementation for DaysList specific behavior
     */
    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.d(TAG, String.format(QDue.getLocale(),
                "DaysListFragment: Events changed %s (%d events)", changeType, eventCount));

        // Call parent implementation for base functionality
        super.onEventsChanged(changeType, eventCount);

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

    /**
     * OVERRIDE: Enhanced implementation for DaysList specific behavior
     */
    @Override
    public void onForceEventsRefresh() {
        Log.d(TAG, "DaysListFragment: Force refresh requested");

        // Call parent implementation for base functionality
        super.onForceEventsRefresh();

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

    /**
     * OVERRIDE: Fragment-specific activity check
     */
    @Override
    public boolean isFragmentActive() {
        // Enhanced check for DaysList fragment
        boolean baseActive = super.isFragmentActive();
        boolean isInDaysListView = isCurrentlyInDaysListView();

        boolean isActive = baseActive && isInDaysListView;
        Log.v(TAG, String.format("Fragment activity check - base: %s, inDaysListView: %s, result: %s",
                baseActive, isInDaysListView, isActive));

        return isActive;
    }

    /**
     * OVERRIDE: Fragment description
     */
    @Override
    public String getFragmentDescription() {
        return "DaysListViewFragment (Legacy)";
    }

    // ==================== CORREZIONE 3: ENHANCED EVENTS DATA NOTIFICATION ====================

    /**
     * OVERRIDE: Enhanced events data notification with DaysList specifics
     */
    @Override
    protected void notifyEventsDataChanged() {
        final String mTAG = "notifyEventsDataChanged: ";
        Log.d(TAG, mTAG + "called for DaysListFragment");

        try {
            // Call parent implementation
            super.notifyEventsDataChanged();

            // Additional DaysList-specific notification
            updateAdapterWithEvents();

            Log.d(TAG, mTAG + "✅ DaysList events notification completed");
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in DaysList events notification: " + e.getMessage());
        }
    }

    /**
     * OVERRIDE: Enhanced events data refreshed hook
     */
    @Override
    protected void onEventsDataRefreshed() {
        final String mTAG = "onEventsDataRefreshed: ";
        Log.v(TAG, mTAG + "Events data refreshed for DaysListFragment");

        // Additional DaysList-specific refresh logic
        updateAdapterWithEventsDelayed();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Update adapter with events using delayed execution for thread safety
     */
    private void updateAdapterWithEventsDelayed() {
        final String mTAG = "updateAdapterWithEventsDelayed: ";

        if (mMainHandler != null) {
            mMainHandler.postDelayed(() -> {
                updateAdapterWithEvents();
                Log.v(TAG, mTAG + "Delayed adapter update completed");
            }, 100); // Small delay to ensure data is ready
        } else {
            // Fallback to immediate update
            updateAdapterWithEvents();
            Log.v(TAG, mTAG + "Immediate adapter update completed");
        }
    }

    /**
     * Check if fragment is currently in DaysList view (vs Calendar view)
     */
    private boolean isCurrentlyInDaysListView() {
        try {
            if (getActivity() instanceof QDueMainActivity) {
                QDueMainActivity mainActivity = (QDueMainActivity) getActivity();
                // You might need to add a method to check current navigation destination
                // For now, assume we're in the right view if fragment is attached
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking current view: " + e.getMessage());
        }
        return false;
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Public method for external events data updates
     * Used by MainActivity or other components
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        final String mTAG = "updateEventsData: ";
        Log.d(TAG, mTAG + "External events data update requested");

        if (eventsMap != null && !eventsMap.isEmpty()) {
            // Update internal cache
            updateEventsCache(eventsMap);

            // Update adapter
            updateAdapterWithEvents();

            Log.d(TAG, mTAG + "✅ External events data update completed");
        } else {
            Log.w(TAG, mTAG + "Empty or null events map provided");
        }
    }

    /**
     * Public method to force refresh events
     * Used by MainActivity when events change
     */
    public void refreshEvents() {
        Log.d(TAG, "refreshEvents: External refresh request");
        onForceEventsRefresh();
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method specific to DaysList fragment
     */
    public void debugDaysListEventsIntegration() {
        Log.d(TAG, "=== DAYSLIST EVENTS INTEGRATION DEBUG ===");

        // Call parent debug
        debugEventsIntegration();

        // DaysList-specific debug
        Log.d(TAG, "Legacy Adapter: " + (mLegacyAdapter != null ? "initialized" : "null"));
        Log.d(TAG, "RecyclerView: " + (mRecyclerView != null ? "initialized" : "null"));
        Log.d(TAG, "Items Cache Size: " + (mItemsCache != null ? mItemsCache.size() : "null"));
        Log.d(TAG, "In DaysList View: " + isCurrentlyInDaysListView());

        if (mLegacyAdapter != null) {
            try {
                // If adapter has debug method, call it
                Log.d(TAG, "Adapter Events Integration: active");
            } catch (Exception e) {
                Log.e(TAG, "Error accessing adapter debug info: " + e.getMessage());
            }
        }

        Log.d(TAG, "=== END DAYSLIST DEBUG ===");
    }

    /**
     * Debug method to force events reload specifically for DaysList
     */
    public void debugForceEventsReload() {
        Log.d(TAG, "=== DEBUG FORCE EVENTS RELOAD (DAYSLIST) ===");

        // Clear cache
        if (mEventsCache != null) {
            mEventsCache.clear();
            Log.d(TAG, "Cleared events cache");
        }

        // Force reload
        loadEventsForCurrentPeriod();

        // Schedule adapter update
        if (mMainHandler != null) {
            mMainHandler.postDelayed(() -> {
                updateAdapterWithEvents();
                Log.d(TAG, "Force reload completed - adapter updated");
            }, 1000);
        }

        Log.d(TAG, "=== END DEBUG FORCE RELOAD (DAYSLIST) ===");
    }
}