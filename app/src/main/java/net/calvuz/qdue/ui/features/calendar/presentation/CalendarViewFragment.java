package net.calvuz.qdue.ui.features.calendar.presentation;

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
import net.calvuz.qdue.ui.core.architecture.base.BaseAdapter;
import net.calvuz.qdue.ui.core.architecture.base.BaseInteractiveAdapter;
import net.calvuz.qdue.ui.core.architecture.base.BaseInteractiveFragment;
import net.calvuz.qdue.ui.core.components.widgets.EventsPreviewManager;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.features.calendar.adapters.CalendarAdapter;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CalendarViewFragment - Complete Click System Integration
 * <p>
 * ENHANCED FEATURES:
 * - Extends BaseInteractiveFragment for long-click support
 * - Implements regular click for events preview bottom sheet
 * - Full integration with events system
 * - Thread-safe operations
 */
public class CalendarViewFragment extends BaseInteractiveFragment {

    @Override
    protected BaseInteractiveAdapter getClickAdapter() {
        return mLegacyAdapter; // Updated reference
    }

    @Override
    protected String getFragmentName() {
        return "CalendarViewFragment";
    }

    // TAG
    private static final String TAG = "Calendar";

    // Keep existing adapter reference for compatibility
    private CalendarAdapter mLegacyAdapter;

    // ==================== EXISTING FRAGMENT IMPLEMENTATION ====================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);

    }

    @Override
    protected void findViews(View rootView) {
        mCoordinatorLayout = rootView.findViewById(R.id.coordinator_layout_calendar);

        if (mCoordinatorLayout == null) {
            Log.e(TAG, "CoordinatorLayout not found - bottom toolbar may not position correctly");
        }

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

    // ==================== CLICK ADAPTER INTEGRATION ====================

    @Override
    protected EventsPreviewManager.ViewType getEventsPreviewViewType() {
        return EventsPreviewManager.ViewType.CALENDAR_VIEW;
    }

    // ==================== ADAPTER SETUP ====================

    @Override
    protected void setupAdapter() {
        setupLegacyAdapter();
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        return mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        if (adapter instanceof CalendarAdapter) {
            this.mLegacyAdapter = (CalendarAdapter) adapter;
        }
    }

    /**
     * Enhanced legacy adapter setup with full click integration
     */
    protected void setupLegacyAdapter() {
        // Create adapter with click support
        mLegacyAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam()
        );

        // Set adapter to RecyclerView
        mRecyclerView.setAdapter(mLegacyAdapter);

        // Update with events if available
        updateAdapterWithEvents();

        Log.v(TAG, "✅ Legacy adapter setup completed");
    }

    // ==================== FAB VISIBILITY ====================

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

    // ==================== EVENTS INTEGRATION ====================

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
                mLegacyAdapter.updateEventsData(eventsCache);
                Log.i(TAG, mTAG + "✅ Adapter updated with events data");
            } else {
                Log.d(TAG, mTAG + "❌ No events cache available yet");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error updating adapter with events: " + e.getMessage());
        }
    }

    // ==================== EVENTS REFRESH IMPLEMENTATION ====================

    /**
     * OVERRIDE: Enhanced implementation for Calendar specific behavior
     */
    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.i(TAG, String.format(QDue.getLocale(),
                "CalendarFragment: Events changed %s (%d events)", changeType, eventCount));

        // Call parent implementation for base functionality
        super.onEventsChanged(changeType, eventCount);

        // Additional Calendar-specific logic
    }

    /**
     * OVERRIDE: Enhanced implementation for Calendar specific behavior
     */
    @Override
    public void onForceEventsRefresh() {
        Log.v(TAG, "CalendarFragment: Force refresh requested");

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
        Log.v(TAG, String.format(QDue.getLocale(), "Fragment activity check - base: %s, inCalendarView: %s, result: %s",
                baseActive, isInCalendarView, isActive));

        return isActive;
    }

    /**
     * OVERRIDE: Fragment description
     */
    @Override
    public String getFragmentDescription() {
        return "CalendarViewFragment (Legacy with Click Integration)";
    }

    // ==================== ENHANCED EVENTS DATA NOTIFICATION ====================

    /**
     * OVERRIDE: Enhanced events data notification with Calendar specifics
     */
    @Override
    protected void notifyEventsDataChanged() {
        final String mTAG = "notifyEventsDataChanged: ";
        Log.v(TAG, mTAG + "called");

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
                        Log.v(TAG, mTAG + "✅ Legacy adapter notified of events changes");
                    } catch (Exception e) {
                        Log.e(TAG, mTAG + "Error notifying legacy adapter: " + e.getMessage());
                    }
                });
            }

            Log.v(TAG, mTAG + "✅ Calendar events notification completed");
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
        Log.v(TAG, mTAG + "called.");

        // Additional Calendar-specific refresh logic
        updateAdapterWithEventsDelayed();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check if fragment is currently in Calendar view (vs DaysList view)
     */
    private boolean isCurrentlyInCalendarView() {
        try {
            if (getActivity() instanceof QDueMainActivity) {
                // For now, assume we're in the right view if fragment is attached
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking current view: " + e.getMessage());
        }
        return false;
    }

    // ==================== CLICK SUPPORT ====================

    // ===========================================
    // Override BaseInteractiveFragment Methods for DaysList Specifics
    // ===========================================

    @Override
    protected void onOpenEventEditor(LocalDate date) {
        Log.d(TAG, "Calendar: Opening event editor for date: " + date);

        // For now, delegate to base implementation
        super.onOpenEventEditor(date);

        // DaysList-specific event editor handling
        // Example: Pass additional context like selected shift, etc.
    }

    @Override
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        Log.d(TAG, "Calendar: Showing events dialog for date: " + date);

        // DaysList-specific events dialog
        if (events != null && !events.isEmpty()) {
            showCalendarEventsDialog(date, events);
        } else {
            super.onShowEventsDialog(date, events);
        }
    }

    @Override
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "Calendar: Quick event created: " + action + " for date: " + date);

        // DaysList-specific post-creation handling
        updateAdapterWithEventsDelayed();
    }

    // ===========================================
    // Calendar Specific Methods
    // ===========================================

    /**
     * Show DaysList-specific events dialog
     */
    private void showCalendarEventsDialog(LocalDate date, List<LocalEvent> events) {
        Log.d(TAG, "Showing Calendar events dialog with " + events.size() + " events for " + date);

        // For now, use base implementation
        showEventsListDialog(date, events);
    }

    /**
     * Update adapter with events using delayed execution
     */
    private void updateAdapterWithEventsDelayed() {
        if (mMainHandler != null) {
            mMainHandler.postDelayed(() -> {
                updateAdapterWithEvents();
                Log.d(TAG, "Delayed adapter events update completed");
            }, 300);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method specific to Calendar fragment
     */
    public void debugCalendarEventsIntegration() {
        Log.d(TAG, "=== CALENDAR CLICK INTEGRATION DEBUG ===");

        // Call parent debug
        debugEventsIntegration();

        // Calendar-specific debug
        Log.d(TAG, "Legacy Adapter: " + (mLegacyAdapter != null ? "initialized" : "null"));
        Log.d(TAG, "RecyclerView: " + (mRecyclerView != null ? "initialized" : "null"));
        Log.d(TAG, "Items Cache Size: " + (mItemsCache != null ? mItemsCache.size() : "null"));
        Log.d(TAG, "Grid Columns: " + getGridColumnCount());
        Log.d(TAG, "In Calendar View: " + isCurrentlyInCalendarView());
        Log.d(TAG, "Events Preview Manager: " + (mEventsPreviewManager != null ? "active" : "null"));

        if (mLegacyAdapter != null) {
            try {
                // Check adapter state
                Log.d(TAG, "Adapter Item Count: " + mLegacyAdapter.getItemCount());
                Log.d(TAG, "Adapter Selection Mode: " + mLegacyAdapter.isSelectionMode());
                Log.d(TAG, "Adapter Selected Count: " + mLegacyAdapter.getSelectedCount());
            } catch (Exception e) {
                Log.e(TAG, "Error accessing adapter debug info: " + e.getMessage());
            }
        }

        // Events preview debug
        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.debugState();
        }

        Log.d(TAG, "=== END CALENDAR CLICK DEBUG ===");
    }

    /**
     * Debug method to test bottom sheet functionality
     */
    public void debugTestBottomSheet() {
        Log.d(TAG, "=== DEBUG TEST BOTTOM SHEET ===");

        LocalDate testDate = LocalDate.now();
        List<LocalEvent> testEvents = getEventsForDate(testDate);

        Log.d(TAG, "Testing bottom sheet for today: " + testDate);
        Log.d(TAG, "Test events count: " + testEvents.size());

        if (mEventsPreviewManager != null) {
            View anchorView = mRecyclerView != null ? mRecyclerView : getView();
            mEventsPreviewManager.showEventsPreview(testDate, testEvents, anchorView);
            Log.d(TAG, "Bottom sheet show requested");
        } else {
            Log.e(TAG, "Events preview manager is null - cannot test");
        }

        Log.d(TAG, "=== END DEBUG TEST BOTTOM SHEET ===");
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