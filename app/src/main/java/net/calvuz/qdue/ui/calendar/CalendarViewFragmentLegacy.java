package net.calvuz.qdue.ui.calendar;

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
 * Enhanced CalendarViewFragment with virtual scrolling integration
 * Maintains calendar grid layout while gaining virtual scrolling performance
 *
 * // ==================== CORREZIONI CALENDARVIEWFRAGMENTLEGACY ====================
 *
 * CalendarViewFragmentLegacy Enhanced - Complete Events Integration
 *
 * CORREZIONI PRINCIPALI:
 * 1. Override metodi EventsRefreshInterface mancanti
 * 2. Implementazione corretta adapter events update
 * 3. Enhanced logging e thread safety
 * 4. Integration con CalendarAdapterLegacy
 */

public class CalendarViewFragmentLegacy extends BaseFragmentLegacy {

    private static final String TAG = "CalendarLgsFrg";

    // Keep existing adapter reference for compatibility
    private CalendarAdapterLegacy mLegacyAdapter;

    // ==================== EXISTING CODE REMAINS UNCHANGED ====================

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

    @Override
    protected int getGridColumnCount() {
        return 7; // Seven columns for calendar days (Sun-Sat)
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // For calendar, convert including empty cells to complete the grid
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    @Override
    protected BaseAdapterLegacy getFragmentAdapter() {
        Log.d(TAG, "getFragmentAdapter: Legacy (CalendarAdapterLegacy)");
        return mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapterLegacy adapter) {
        if (adapter instanceof CalendarAdapterLegacy) {
            this.mLegacyAdapter = (CalendarAdapterLegacy) adapter;
        }
    }

    @Override
    protected void setupAdapter() {
        setupLegacyAdapter();
        Log.d(TAG, "setupAdapter: ✅ Legacy adapter setup completed");
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

    // ==================== CORREZIONE 1: ENHANCED ADAPTER SETUP ====================

    /**
     * Enhanced legacy adapter setup with events integration
     */
    protected void setupLegacyAdapter() {
        final String mTAG = "setupLegacyAdapter: ";
        Log.v(TAG, mTAG + "called.");

        mLegacyAdapter = new CalendarAdapterLegacy(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam()
        );
        mRecyclerView.setAdapter(mLegacyAdapter);

        // ENHANCED: Update with events if available
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
     * OVERRIDE: Enhanced implementation for Calendar specific behavior
     */
    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.d(TAG, String.format(QDue.getLocale(),
                "CalendarFragment: Events changed %s (%d events)", changeType, eventCount));

        // Call parent implementation for base functionality
        super.onEventsChanged(changeType, eventCount);

        // Additional Calendar-specific logic
        updateAdapterWithEventsDelayed();
    }

    /**
     * OVERRIDE: Enhanced implementation for Calendar specific behavior
     */
    @Override
    public void onForceEventsRefresh() {
        Log.d(TAG, "CalendarFragment: Force refresh requested");

        // Call parent implementation for base functionality
        super.onForceEventsRefresh();

        // Additional Calendar-specific logic
        updateAdapterWithEventsDelayed();
    }

    /**
     * OVERRIDE: Fragment-specific activity check
     */
    @Override
    public boolean isFragmentActive() {
        // Enhanced check for Calendar fragment
        boolean baseActive = super.isFragmentActive();
        boolean isInCalendarView = isCurrentlyInCalendarView();

        boolean isActive = baseActive && isInCalendarView;
        Log.v(TAG, String.format("Fragment activity check - base: %s, inCalendarView: %s, result: %s",
                baseActive, isInCalendarView, isActive));

        return isActive;
    }

    /**
     * OVERRIDE: Fragment description
     */
    @Override
    public String getFragmentDescription() {
        return "CalendarViewFragment (Legacy)";
    }

    // ==================== CORREZIONE 3: ENHANCED EVENTS DATA NOTIFICATION ====================

    /**
     * OVERRIDE: Enhanced events data notification with Calendar specifics
     */
    @Override
    protected void notifyEventsDataChanged() {
        final String mTAG = "notifyEventsDataChanged: ";
        Log.d(TAG, mTAG + "called for CalendarFragment");

        try {
            // Call parent implementation
            super.notifyEventsDataChanged();

            // Additional Calendar-specific notification
            updateAdapterWithEvents();

            // FIXED: Also trigger adapter's own notification
            if (mLegacyAdapter != null) {
                mMainHandler.post(() -> {
                    try {
                        mLegacyAdapter.notifyEventsDataChanged();
                        Log.d(TAG, mTAG + "✅ Legacy adapter notified of events changes");
                    } catch (Exception e) {
                        Log.e(TAG, mTAG + "Error notifying legacy adapter: " + e.getMessage());
                    }
                });
            }

            Log.d(TAG, mTAG + "✅ Calendar events notification completed");
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error in Calendar events notification: " + e.getMessage());
        }
    }

    /**
     * OVERRIDE: Enhanced events data refreshed hook
     */
    @Override
    protected void onEventsDataRefreshed() {
        final String mTAG = "onEventsDataRefreshed: ";
        Log.v(TAG, mTAG + "Events data refreshed for CalendarFragment");

        // Additional Calendar-specific refresh logic
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
     * Check if fragment is currently in Calendar view (vs DaysList view)
     */
    private boolean isCurrentlyInCalendarView() {
        try {
            if (getActivity() instanceof QDueMainActivity) {
                // You might need to add a method to check current navigation destination
                // For now, assume we're in the right view if fragment is attached
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking current view: " + e.getMessage());
        }
        return false;
    }

    // ==================== EXISTING METHODS ENHANCED ====================

    /**
     * Enhanced day click handling with events consideration
     */
    protected void onDayClicked(LocalDate date, Day dayData) {
        Log.d(TAG, "Day clicked: " + date);

        // Check if day has events
        List<LocalEvent> eventsForDay = getEventsForDate(date);
        if (!eventsForDay.isEmpty()) {
            Log.d(TAG, "Day has " + eventsForDay.size() + " events");
            // Could show events dialog or navigate to day detail
        }

        // Add your calendar-specific day click logic here
        // Example: Show day detail dialog
        // showDayDetailDialog(date, dayData, eventsForDay);
    }

    /**
     * Enhanced month change handling with events loading
     */
    protected void onMonthChanged(LocalDate newMonth) {
        Log.d(TAG, "Month changed to: " + newMonth);

        // Load events for new month if needed
        loadEventsForMonth(newMonth);

        // Update any calendar-specific UI elements
        if (getActivity() != null) {
            // getActivity().setTitle(newMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
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
     * Debug method specific to Calendar fragment
     */
    public void debugCalendarEventsIntegration() {
        Log.d(TAG, "=== CALENDAR EVENTS INTEGRATION DEBUG ===");

        // Call parent debug
        debugEventsIntegration();

        // Calendar-specific debug
        Log.d(TAG, "Legacy Adapter: " + (mLegacyAdapter != null ? "initialized" : "null"));
        Log.d(TAG, "RecyclerView: " + (mRecyclerView != null ? "initialized" : "null"));
        Log.d(TAG, "Items Cache Size: " + (mItemsCache != null ? mItemsCache.size() : "null"));
        Log.d(TAG, "Grid Columns: " + getGridColumnCount());
        Log.d(TAG, "In Calendar View: " + isCurrentlyInCalendarView());

        if (mLegacyAdapter != null) {
            try {
                // Check adapter state
                Log.d(TAG, "Adapter Events Integration: active");
                Log.d(TAG, "Adapter Item Count: " + mLegacyAdapter.getItemCount());
            } catch (Exception e) {
                Log.e(TAG, "Error accessing adapter debug info: " + e.getMessage());
            }
        }

        Log.d(TAG, "=== END CALENDAR DEBUG ===");
    }

    /**
     * Debug method to force events reload specifically for Calendar
     */
    public void debugForceEventsReload() {
        Log.d(TAG, "=== DEBUG FORCE EVENTS RELOAD (CALENDAR) ===");

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
                if (mLegacyAdapter != null) {
                    mLegacyAdapter.notifyEventsDataChanged();
                }
                Log.d(TAG, "Force reload completed - adapter updated");
            }, 1000);
        }

        Log.d(TAG, "=== END DEBUG FORCE RELOAD (CALENDAR) ===");
    }
}