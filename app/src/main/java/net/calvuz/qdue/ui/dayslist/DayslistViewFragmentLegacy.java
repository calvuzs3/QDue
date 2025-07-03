package net.calvuz.qdue.ui.dayslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseAdapterLegacy;
import net.calvuz.qdue.ui.shared.BaseClickAdapterLegacy;
import net.calvuz.qdue.ui.shared.BaseClickFragmentLegacy;
import net.calvuz.qdue.ui.shared.DaysListEventsPreview;
import net.calvuz.qdue.ui.shared.EventsPreviewManager;
import net.calvuz.qdue.ui.shared.interfaces.EventsPreviewInterface;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DayslistViewFragment with virtual scrolling integration
 * Minimal changes to existing code while gaining virtual scrolling benefits
 * ✅
 * ❌
 * <p>
 * DayslistViewFragmentLegacy Enhanced - Complete Events Integration
 * <p>
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

    // ==================== ABSTRACT METHOD IMPLEMENTATION ====================

    @Override
    protected BaseClickAdapterLegacy getClickAdapter() {
        return mLegacyAdapter;
    }

    @Override
    protected String getFragmentName() {
        return "DaysListViewFragment";
    }

    // ==================== EXISTING FRAGMENT IMPLEMENTATION ====================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dayslist_view, container, false);
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

    // ==================== ADAPTER SETUP ====================

    @Override
    protected void setupAdapter() {
        setupLegacyAdapter();
        Log.d(TAG, "setupAdapter: ✅ Legacy adapter setup completed");
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

    // ==================== FAB VISIBILITY ====================

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

    // ==================== FRAGMENT ACTIVITY CHECK ====================

    @Override
    public String getFragmentDescription() {
        return "DaysListViewFragment (Legacy)";
    }

    // ==================== CLICK SUPPORT ====================

    // ===========================================
    // Override BaseClickFragmentLegacy Methods for DaysList Specifics
    // ===========================================

    /**
     * Open event editor for a specific date
     * @param date The date for which to open the editor
     */
    @Override
    protected void onOpenEventEditor(LocalDate date) {
        Log.d(TAG, "DaysList: Opening event editor for date: " + date);

        // TODO: Implement specific navigation for DaysList
        // For now, delegate to base implementation
        super.onOpenEventEditor(date);

        // DaysList-specific event editor handling
        // Example: Pass additional context like selected shift, etc.
    }

    /**
     * Show events dialog for a specific date
     * @param date The date for which to show the events dialog
     * @param events The list of events to display
     */
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

    /**
     * Create a quick event for a specific date
     * @param action The type of quick event to create
     * @param date The date for which to create the quick event
     */
    @Override
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "DaysList: Quick event created: " + action + " for date: " + date);

        // DaysList-specific post-creation handling
//        updateAdapterWithEventsDelayed();
    }

    /**
     * Update UI based on selection state
     * @param hasSelection Whether selection mode is active
     */
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
    // Menu Integration
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
        return super.onOptionsItemSelected(item);

        // Handle DaysList-specific menu items
        // TODO: Add DaysList-specific menu handling
    }

    // ===========================================
    // Preview Integration
    // ===========================================

    /**
     * events preview setup
     */
    @Override
    protected EventsPreviewManager.ViewType getEventsPreviewViewType() {

        return EventsPreviewManager.ViewType.DAYS_LIST;
    }

    // Add method to handle back press for expansion
    @Override
    public boolean onBackPressed() {
        // First check if any card is expanded
        if (mEventsPreviewManager != null) {
            if (mEventsPreviewManager.getCurrentImplementation() instanceof DaysListEventsPreview daysListPreview) {
                if (daysListPreview.hasExpandedCard()) {
                    daysListPreview.collapseAll();
                    return true; // Consumed
                }
            }
        }

        // Then delegate to parent
        return super.onBackPressed();
    }


    /**
     * Setup Phase 3 specific integration
     */
    private void setupPhase3Integration() {
        Log.d(TAG, "Setting up Phase 3 DaysList expansion integration");

        // Ensure RecyclerView settings support smooth expansion
        if (mRecyclerView != null) {
            // Disable item animator to prevent conflicts with expansion animations
            mRecyclerView.setItemAnimator(null);

            // Ensure nested scrolling is handled correctly
            mRecyclerView.setNestedScrollingEnabled(true);

            // Add scroll listener to handle expansion during scroll
            mRecyclerView.addOnScrollListener(new ExpansionScrollListener());
        }

        // NEW: Configure compact mode for DaysList
        if (mEventsPreviewManager != null) {
            EventsPreviewInterface impl = mEventsPreviewManager.getCurrentImplementation();
            if (impl instanceof DaysListEventsPreview daysListPreview) {
                daysListPreview.setUseCompactMode(true); // Enable compact mode
                Log.d(TAG, "Compact mode enabled for DaysList expansion");
            }
        }

        Log.d(TAG, "Phase 3 integration setup completed");
    }

    // ===========================================
    // Lifecycle Enhanced
    // ===========================================

    /**
     * Phase 3 setup
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Phase 3 specific setup
        setupPhase3Integration();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensure selection state consistency
        if (mLegacyAdapter != null && mIsSelectionMode != mLegacyAdapter.isSelectionMode()) {
            Log.w(TAG, "Selection mode inconsistency detected, syncing...");
            mIsSelectionMode = mLegacyAdapter.isSelectionMode();
            updateSelectionUI();
        }

        testEventsPreview();
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

    // Nel fragment, temporaneamente:
    public void testEventsPreview() {
        debugClickHandling();

        // Test click normale su un giorno con eventi
        LocalDate testDate = LocalDate.now();
        List<LocalEvent> testEvents = getEventsForDate(testDate);
        // Click normale dovrebbe mostrare "TODO: Phase 2" nei log
    }


    // Add method to test expansion functionality
    public void testExpansionFunctionality() {
        Log.d(TAG, "=== TESTING EXPANSION FUNCTIONALITY ===");

        // Test expansion on today
        LocalDate today = LocalDate.now();
        List<LocalEvent> todayEvents = getEventsForDate(today);

        Log.d(TAG, "Testing expansion for today: " + today);
        Log.d(TAG, "Today has " + todayEvents.size() + " events");

        if (mEventsPreviewManager != null) {
            // Find a view for today (simplified)
            View anchorView = mRecyclerView != null ? mRecyclerView : getView();
            mEventsPreviewManager.showEventsPreview(today, todayEvents, anchorView);

            Log.d(TAG, "Expansion test triggered");

            // Auto-collapse after 5 seconds
            if (mMainHandler != null) {
                mMainHandler.postDelayed(() -> {
                    mEventsPreviewManager.hideEventsPreview();
                    Log.d(TAG, "Expansion test auto-collapsed");
                }, 5000);
            }
        }

        Log.d(TAG, "=== END EXPANSION TEST ===");
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

    public void debugClickHandling() {

        Log.d(TAG, "=== DEBUG CLICK HANDLING ===");
    }


    // ===========================================
    // Expansion Support
    // ===========================================

    /**
     * Scroll listener to handle expansion during scroll
     */
    private class ExpansionScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            // Collapse expanded cards when user starts scrolling
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                if (mEventsPreviewManager != null && mEventsPreviewManager.isEventsPreviewShowing()) {
                    if (mEventsPreviewManager.getCurrentImplementation() instanceof DaysListEventsPreview daysListPreview) {
                        if (daysListPreview.hasExpandedCard()) {
                            Log.d(TAG, "Collapsing expanded card due to scroll start");
                            daysListPreview.collapseAll();
                        }
                    }
                }
            }
        }
    }
}