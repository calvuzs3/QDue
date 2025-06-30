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
import net.calvuz.qdue.ui.shared.BaseClickAdapterLegacy;
import net.calvuz.qdue.ui.shared.BaseClickFragmentLegacy;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.ui.shared.ToolbarAction;
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
 * DayslistViewFragmentLegacy Enhanced - Complete Events Integration
 *
 * CORREZIONI PRINCIPALI:
 * 1. Override metodi EventsRefreshInterface
 * 2. Implementazione corretta adapter events update
 * 3. Enhanced logging e debug
 * 4. Thread-safe operations
 */

public class DayslistViewFragmentLegacy extends BaseClickFragmentLegacy {

    private final String TAG = "DayslistLgsFrg";

    // Keep existing adapter reference for compatibility
    private DaysListAdapterLegacy mLegacyAdapter;

    // ===========================================
    // Abstract Methods Implementation (BaseClickFragmentLegacy)
    // ===========================================

    @Override
    protected BaseClickAdapterLegacy getClickAdapter() {
        return mLegacyAdapter;
    }

    @Override
    protected String getFragmentName() {
        return "DaysListViewFragment";
    }

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

        // Don't update FAB visibility if in selection mode
        if (mIsSelectionMode) return;

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
        mLegacyAdapter = new DaysListAdapterLegacy(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam(),
                3
        );
        mRecyclerView.setAdapter(mLegacyAdapter);

        // Update with events if available with better error handling
        updateAdapterWithEvents();

        Log.d(TAG, "setupLegacyAdapter: ✅ Legacy adapter setup completed");
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

    // ==================== FRAGMENT ACTIVITY CHECK ====================


    @Override
    public String getFragmentDescription() {
        return "DaysListViewFragment (Legacy with BaseClickFragment)";
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

    // ==================== CLICK SUPPORT ====================

    // ===========================================
    // Override BaseClickFragmentLegacy Methods for DaysList Specifics
    // ===========================================

    @Override
    protected void onOpenEventEditor(LocalDate date) {
        Log.d(TAG, "DaysList: Opening event editor for date: " + date);

        // TODO: Implement specific navigation for DaysList
        // For now, delegate to base implementation
        super.onOpenEventEditor(date);

        // DaysList-specific event editor handling
        // Example: Pass additional context like selected shift, etc.
    }

    @Override
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        Log.d(TAG, "DaysList: Showing events dialog for date: " + date);

        // DaysList-specific events dialog
        if (events != null && !events.isEmpty()) {
            // TODO: Create DaysListEventsDialog with specific features
            showDaysListEventsDialog(date, events);
        } else {
            super.onShowEventsDialog(date, events);
        }
    }

    @Override
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "DaysList: Quick event created: " + action + " for date: " + date);

        // Refresh events data after quick event creation
        refreshEventsDataDelayed();

        // DaysList-specific post-creation handling
        updateAdapterWithEventsDelayed();
    }

    @Override
    protected void updateSelectionDependentUI(boolean hasSelection) {
        // DaysList-specific UI updates based on selection
        Log.v(TAG, "DaysList: Updating selection-dependent UI, hasSelection: " + hasSelection);

        // Could add DaysList-specific UI updates here
        // For example: show/hide certain buttons, update status bar, etc.
    }

    // ===========================================
    // DaysList Specific Methods
    // ===========================================

    /**
     * Show DaysList-specific events dialog
     */
    private void showDaysListEventsDialog(LocalDate date, List<LocalEvent> events) {
        Log.d(TAG, "Showing DaysList events dialog with " + events.size() + " events for " + date);

        // TODO: Implement DaysListEventsDialogFragment
        // For now, use base implementation
        showEventsListDialog(date, events);
    }

    /**
     * Refresh events data with delay for thread safety
     */
    private void refreshEventsDataDelayed() {
        if (mMainHandler != null) {
            mMainHandler.postDelayed(() -> {
                loadEventsForCurrentPeriod();
                Log.d(TAG, "Delayed events data refresh completed");
            }, 200);
        }
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

    // ===========================================
    // Events Refresh Integration (existing code enhanced)
    // ===========================================

    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.d(TAG, String.format(QDue.getLocale(),
                "DaysListFragment: Events changed %s (%d events)", changeType, eventCount));

        // Call parent implementation for base functionality
        super.onEventsChanged(changeType, eventCount);

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

    @Override
    public void onForceEventsRefresh() {
        Log.d(TAG, "DaysListFragment: Force refresh requested");

        // Call parent implementation for base functionality
        super.onForceEventsRefresh();

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

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

    @Override
    protected void onEventsDataRefreshed() {
        final String mTAG = "onEventsDataRefreshed: ";
        Log.v(TAG, mTAG + "Events data refreshed for DaysListFragment");

        // Additional DaysList-specific refresh logic
        updateAdapterWithEventsDelayed();
    }

    // ===========================================
    // Helper Methods
    // ===========================================

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

    // ===========================================
    // Public API Methods Enhanced
    // ===========================================

    /**
     * Public method for external events data updates
     * Enhanced with selection mode awareness
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
     * Enhanced with selection mode handling
     */
    public void refreshEvents() {
        Log.d(TAG, "refreshEvents: External refresh request");

        // Exit selection mode before refreshing to avoid inconsistencies
        if (mIsSelectionMode) {
            exitSelectionMode();
        }

        onForceEventsRefresh();
    }

    // ===========================================
    // Back Press Handling Enhanced
    // ===========================================

    /**
     * Enhanced back press handling with selection mode priority
     */
    public boolean onBackPressed() {
        // First check if we can handle it in BaseClickFragmentLegacy
        boolean handled = super.onBackPressed();
        if (handled) {
            return true;
        }

        // If not handled by parent, check DaysList-specific handling
        // (currently none, but could be added for future features)

        return false; // Not consumed
    }

    // ===========================================
    // Menu Integration Enhanced
    // ===========================================

    /**
     * Handle options menu creation for selection mode
     */
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Add selection mode menu items if needed
        if (mIsSelectionMode) {
            // TODO: Inflate selection mode menu
            // inflater.inflate(R.menu.menu_dayslist_selection, menu);
            // mSelectAllMenuItem = menu.findItem(R.id.action_select_all);
            // mClearSelectionMenuItem = menu.findItem(R.id.action_clear_selection);
        }
    }

    /**
     * Handle options menu item selection
     */
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        // First try parent handling
        boolean handled = super.onOptionsItemSelected(item);
        if (handled) {
            return true;
        }

        // Handle DaysList-specific menu items
        // TODO: Add DaysList-specific menu handling

        return false;
    }

    // ===========================================
    // Debug Methods Enhanced
    // ===========================================

    /**
     * Debug method specific to DaysList fragment with BaseClickFragment integration
     */
    public void debugDaysListEventsIntegration() {
        Log.d(TAG, "=== DAYSLIST EVENTS INTEGRATION DEBUG (BaseClickFragment) ===");

        // Call parent debug
        debugEventsIntegration();

        // Call BaseClickFragment debug
        debugLongClickIntegration();

        // DaysList-specific debug
        Log.d(TAG, "Legacy Adapter: " + (mLegacyAdapter != null ? "initialized" : "null"));
        Log.d(TAG, "RecyclerView: " + (mRecyclerView != null ? "initialized" : "null"));
        Log.d(TAG, "Items Cache Size: " + (mItemsCache != null ? mItemsCache.size() : "null"));
        Log.d(TAG, "In DaysList View: " + isCurrentlyInDaysListView());
        Log.d(TAG, "Fragment Selection Mode: " + mIsSelectionMode);

        if (mLegacyAdapter != null) {
            Log.d(TAG, "Adapter Selection Mode: " + mLegacyAdapter.isSelectionMode());
            Log.d(TAG, "Adapter Selected Count: " + mLegacyAdapter.getSelectedCount());
            Log.d(TAG, "Adapter Events Integration: active");
        }

        Log.d(TAG, "=== END DAYSLIST DEBUG (BaseClickFragment) ===");
    }

    /**
     * Debug method to force events reload specifically for DaysList with selection awareness
     */
    public void debugForceEventsReload() {
        Log.d(TAG, "=== DEBUG FORCE EVENTS RELOAD (DAYSLIST + BaseClickFragment) ===");

        // Exit selection mode to avoid conflicts
        if (mIsSelectionMode) {
            Log.d(TAG, "Exiting selection mode before reload");
            exitSelectionMode();
        }

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

        Log.d(TAG, "=== END DEBUG FORCE RELOAD (DAYSLIST + BaseClickFragment) ===");
    }

    /**
     * Debug selection state specifically for DaysList
     */
    public void debugDaysListSelectionState() {
        Log.d(TAG, "=== DAYSLIST SELECTION STATE DEBUG ===");

        // Call parent debug
        debugSelectionState();

        // DaysList-specific selection debug
        Log.d(TAG, "Current View Type: DaysList");
        Log.d(TAG, "Events Cache Size: " + (mEventsCache != null ? mEventsCache.size() : "null"));
        Log.d(TAG, "Today Position: " + mTodayPosition);

        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                    (androidx.recyclerview.widget.LinearLayoutManager) mRecyclerView.getLayoutManager();
            Log.d(TAG, "First Visible Position: " + layoutManager.findFirstVisibleItemPosition());
            Log.d(TAG, "Last Visible Position: " + layoutManager.findLastVisibleItemPosition());
        }

        Log.d(TAG, "=== END DAYSLIST SELECTION DEBUG ===");
    }

    // ===========================================
    // Lifecycle Enhanced
    // ===========================================

    @Override
    public void onResume() {
        super.onResume();

        // Ensure selection state consistency
        if (mLegacyAdapter != null && mIsSelectionMode != mLegacyAdapter.isSelectionMode()) {
            Log.w(TAG, "Selection mode inconsistency detected, syncing...");
            mIsSelectionMode = mLegacyAdapter.isSelectionMode();
            updateSelectionUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Exit selection mode when fragment is paused to avoid confusion
        if (mIsSelectionMode) {
            Log.d(TAG, "Exiting selection mode due to fragment pause");
            exitSelectionMode();
        }
    }

    // ===========================================
    // Integration Test Methods
    // ===========================================

    /**
     * Test long-click functionality
     */
    public void testLongClickFunctionality() {
        Log.d(TAG, "=== TESTING LONG-CLICK FUNCTIONALITY ===");

        if (mLegacyAdapter == null) {
            Log.e(TAG, "Cannot test: adapter is null");
            return;
        }

        // Test selection mode toggle
        boolean wasInSelectionMode = mIsSelectionMode;

        if (!wasInSelectionMode) {
            enterSelectionMode();
            Log.d(TAG, "Test: Entered selection mode");
        }

        // Test selection state
        debugSelectionState();

        if (!wasInSelectionMode) {
            exitSelectionMode();
            Log.d(TAG, "Test: Exited selection mode");
        }

        Log.d(TAG, "=== END LONG-CLICK FUNCTIONALITY TEST ===");
    }

    /**
     * Test toolbar integration
     */
    public void testToolbarIntegration() {
        Log.d(TAG, "=== TESTING TOOLBAR INTEGRATION ===");

        // Test each toolbar action
        LocalDate testDate = LocalDate.now();

        for (ToolbarAction action : ToolbarAction.values()) {
            Log.d(TAG, "Testing action: " + action);
            onToolbarActionSelected(action, null, testDate);
        }

        Log.d(TAG, "=== END TOOLBAR INTEGRATION TEST ===");
    }
}