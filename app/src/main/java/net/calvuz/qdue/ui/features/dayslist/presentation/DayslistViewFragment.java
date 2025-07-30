package net.calvuz.qdue.ui.features.dayslist.presentation;

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
import net.calvuz.qdue.ui.features.dayslist.adapters.DaysListAdapter;
import net.calvuz.qdue.ui.features.dayslist.components.DaysListEventsPreview;
import net.calvuz.qdue.ui.core.components.widgets.EventsPreviewManager;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DayslistViewFragment with virtual scrolling integration
 * Minimal changes to existing code while gaining virtual scrolling benefits
 * ✅
 * ❌
 * <p>
 * DayslistViewFragment Enhanced - Complete Events Integration
 * <p>
 * CORREZIONI PRINCIPALI:
 * 1. Override metodi EventsRefreshInterface
 * 2. Implementazione corretta adapter events update
 * 3. Enhanced logging e debug
 * 4. Thread-safe operations
 */

public class DayslistViewFragment extends BaseInteractiveFragment {

    @Override
    protected BaseInteractiveAdapter getClickAdapter() {
        return mLegacyAdapter; // Updated reference
    }

    @Override
    protected String getFragmentName() {
        return "DaysListViewFragment";
    }

    // TAG
    private final String TAG = "DaysList";

    // Adapter
    private DaysListAdapter mLegacyAdapter;

    // ==================== ABSTRACT METHOD IMPLEMENTATION ====================

    // ===========================================
    // Fragment Lifecycle
    // ===========================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate( R.layout.fragment_dayslist_view, container, false );
    }

    @Override
    public void onPause() {
        super.onPause();

        // Exit selection mode when fragment is paused to avoid confusion
        if (isSelectionMode()) {
            Log.i( TAG, "✅ Exiting selection mode due to fragment pause" );
            exitSelectionMode();
        }
    }

    // ===========================================
    //  Methods
    // ===========================================

    @Override
    protected void findViews(View rootView) {
        mCoordinatorLayout = rootView.findViewById( R.id.coordinator_layout_dayslist );

        if (mCoordinatorLayout == null) {
            Log.e( TAG, "CoordinatorLayout not found - bottom toolbar may not position correctly" );
        }
        mRecyclerView = rootView.findViewById( R.id.rv_dayslist );
        mFabGoToToday = rootView.findViewById( R.id.fab_go_to_today );
    }

    @Override
    protected int getGridColumnCount() {
        return 1; // Single column for list behavior
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return SharedViewModels.DataConverter.convertForDaysList( days, monthDate );
    }

    // ==================== ADAPTER SETUP ====================

    @Override
    protected void setupAdapter() {
        setupLegacyAdapter();
        Log.i( TAG, "✅ Adapter setup completed" );
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        return mLegacyAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        if (adapter instanceof DaysListAdapter) {
            this.mLegacyAdapter = (DaysListAdapter) adapter;
        }
    }

    /**
     * Enhanced legacy adapter setup with events integration
     */
    protected void setupLegacyAdapter() {
        mLegacyAdapter = new DaysListAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam(),
                3
        );
        mRecyclerView.setAdapter( mLegacyAdapter );

        // Update with events if available with better error handling
        updateAdapterWithEvents();

        Log.i( TAG, "✅ Legacy adapter setup completed" );
    }

    // ==================== EVENTS INTEGRATION ====================

    /**
     * Update adapter with current events data
     */
    private void updateAdapterWithEvents() {
        if (mLegacyAdapter == null) {
            Log.w( TAG, "Adapter is null, cannot update with events" );
            return;
        }

        try {
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            if (!eventsCache.isEmpty()) {
                mLegacyAdapter.updateEventsData( eventsCache );
                Log.i( TAG, "✅ Adapter updated with events data" );
            } else {
                Log.i( TAG, "No events cache available yet" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error updating adapter with events: " + e.getMessage() );
        }
    }

    // ==================== FAB VISIBILITY ====================

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        // Don't update FAB visibility if in selection mode
        if (isSelectionMode()) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        // Animate FAB visibility changes
        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            toggleFabVisibility( mFabGoToToday );
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            toggleFabVisibility( mFabGoToToday );
        }
    }

    // ==================== FRAGMENT ACTIVITY CHECK ====================

    @Override
    public String getFragmentDescription() {
        return "DaysListViewFragment (Legacy)";
    }

    // ===========================================
    // Override BaseInteractiveFragment Methods for DaysList Specifics
    // ===========================================

    /**
     * Open event editor for a specific date
     *
     * @param date The date for which to open the editor
     */
    @Override
    protected void onOpenEventEditor(LocalDate date) {
        Log.d( TAG, "✅ Opening event editor for date: " + date );

        // TODO: Implement specific navigation for DaysList
        // For now, delegate to base implementation
        super.onOpenEventEditor( date );

        // DaysList-specific event editor handling
        // Example: Pass additional context like selected shift, etc.
    }

    /**
     * Show events dialog for a specific date
     *
     * @param date   The date for which to show the events dialog
     * @param events The list of events to display
     */
    @Override
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        Log.d( TAG, "✅ Showing events dialog for date: " + date );

        // DaysList-specific events dialog
        if (events != null && !events.isEmpty()) {
            showDaysListEventsDialog( date, events );
        } else {
            super.onShowEventsDialog( date, events );
        }
    }

    /**
     * Create a quick event for a specific date
     *
     * @param action The type of quick event to create
     * @param date   The date for which to create the quick event
     */
    @Override
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d( TAG, "✅ Quick event created: " + action + " for date: " + date );

        // Refresh data to show the new event
//        loadDaysListData();
        // DaysList-specific post-creation handling
//        updateAdapterWithEventsDelayed();
    }

    // ===========================================
    // DaysList Specific Methods
    // ===========================================

    /**
     * Show DaysList-specific events dialog
     */
    private void showDaysListEventsDialog(LocalDate date, List<LocalEvent> events) {
        Log.v( TAG, "✅ Showing DaysList events dialog with " + events.size() + " events for " + date );

        // TODO: Implement DaysListEventsDialogFragment
        // For now, use base implementation
        showEventsListDialog( date, events );
    }

    /**
     * Update adapter with events using delayed execution
     */
    private void updateAdapterWithEventsDelayed() {
        if (mMainHandler != null) {
            mMainHandler.postDelayed( () -> {
                updateAdapterWithEvents();
                Log.i( TAG, "✅ Delayed adapter events update completed" );
            }, 300 );
        }
    }

    // ===========================================
    // Events Refresh Integration
    // ===========================================

    @Override
    public void onEventsChanged(String changeType, int eventCount) {
        Log.d( TAG, String.format( QDue.getLocale(),
                "✅ Events changed %s (%d events)", changeType, eventCount ) );

        // Call parent implementation for base functionality
        super.onEventsChanged( changeType, eventCount );

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

    @Override
    public void onForceEventsRefresh() {
        Log.d( TAG, "✅ Force refresh requested" );

        // Call parent implementation for base functionality
        super.onForceEventsRefresh();

        // Additional DaysList-specific logic
        updateAdapterWithEventsDelayed();
    }

    @Override
    public boolean isFragmentActive() {
        boolean baseActive = super.isFragmentActive();
        boolean isInDaysListView = isCurrentlyInDaysListView();

        boolean isActive = baseActive && isInDaysListView;

        Log.d( TAG, String.format( QDue.getLocale(),
                "✅ Fragment activity check - base: %s, inDaysListView: %s, result: %s",
                baseActive, isInDaysListView, isActive ) );

        return isActive;
    }

    @Override
    protected void onEventsDataRefreshed() {
        Log.d( TAG, "✅ Events data refreshed" );

        // Additional DaysList-specific refresh logic
        updateAdapterWithEventsDelayed();
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * HELPER: Check if fragment is currently in DaysList view (vs Calendar view)
     */
    private boolean isCurrentlyInDaysListView() {
        try {
            if (getActivity() instanceof QDueMainActivity) {
                // You might need to add a method to check current navigation destination
                // For now, assume we're in the right view if fragment is attached
                return true;
            }
        } catch (Exception e) {
            Log.e( TAG, "Error checking current view: " + e.getMessage() );
        }
        return false;
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
//        return super.onBackPressed();
        return false;
    }
}