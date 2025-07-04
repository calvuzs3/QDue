package net.calvuz.qdue.ui.shared;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.shared.interfaces.EventsPreviewInterface;
import net.calvuz.qdue.ui.shared.quickevents.QuickEventConfirmationDialog;
import net.calvuz.qdue.ui.shared.quickevents.QuickEventTemplate;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Base fragment with long-click and selection support
 * Enhanced with Quick Events creation system
 * Intermediate layer between BaseFragmentLegacy and specific implementations
 *
 * RECENT ENHANCEMENTS:
 * ✅ Quick Events Integration with QuickEventTemplate and UserEventLogicAdapter
 * ✅ Enhanced error handling and validation
 * ✅ Business logic delegation to adapter classes
 * ✅ User feedback improvements
 * ✅ Template-based event creation
 */
public abstract class BaseClickFragmentLegacy extends BaseFragmentLegacy implements DayLongClickListener {

    private static final String TAG = "BaseClickFragment";

    // Bottom Selection Toolbar
    protected BottomSelectionToolbar mBottomToolbar;

    // Views
    protected CoordinatorLayout mCoordinatorLayout; // to attach the bottom toolbar

    // Selection mode support
    protected boolean mHasSelectionHiddenFab = false;
    protected MenuItem mSelectAllMenuItem;
    protected MenuItem mClearSelectionMenuItem;
    protected MenuItem mExitSelectionMenuItem;

    // Events Preview Integration
    protected EventsPreviewManager mEventsPreviewManager;
    protected boolean mEventsPreviewEnabled = true;

    // ==================== NEW: Quick Events Integration ====================

    // Quick Events Templates Cache
    private Map<ToolbarAction, QuickEventTemplate> mQuickEventTemplates;
    private boolean mQuickEventsInitialized = false;

    // User context for event creation
    private Long mCurrentUserId = null; // Will be set by subclass or user session

    // ==================== Abstract Methods ====================

    // Abstract methods for subclasses to implement
    protected abstract BaseClickAdapterLegacy getClickAdapter();

    protected abstract String getFragmentName();

    /**
     * @return CoordinatorLayout to attach the bottom toolbar
     */
    protected ViewGroup getToolbarContainer() {
        return mCoordinatorLayout;
    }

    // ===========================================
    // Lifecycle
    // ===========================================

    /**
     * Enhanced onPause with events preview cleanup
     */
    @Override
    public void onPause() {
        super.onPause();

        // Force exit selection on pause per UX
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
        }

        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cleanup delegato all'adapter
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            adapter.onDestroy();
        }

        // Events Preview Click
        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.onDestroy();
            mEventsPreviewManager = null;
        }

        // ✅ NEW: Quick Events cleanup
        cleanupQuickEventsSystem();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Initialize bottom toolbar first
        setupBottomToolbar();

        // ✅ NEW: Initialize Quick Events system
        initializeQuickEventsSystem();

        // Super call
        super.onViewCreated(view, savedInstanceState);

        // Setup regular click listener for events preview
        setupAdapterRegularClickListener();

        // Setup long-click listener in adapter if available
        setupAdapterLongClickListener();

        // Initialize events preview after base setup
        initializeEventsPreview();

        // DEBUG
        debugSelectionState();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up bottom toolbar
        if (mBottomToolbar != null) {
            mBottomToolbar.destroy();
            mBottomToolbar = null;
        }
    }


    /**
     * ✅ REFACTORED: Back press - semplificato
     */
    public boolean onBackPressed() {
        if (isSelectionMode()) {
            Log.d(TAG, getFragmentName() + ": Back press in selection mode");

            BaseClickAdapterLegacy adapter = getClickAdapter();
            if (adapter != null) {
                // ✅ Un solo punto di exit
                adapter.setSelectionMode(false);
            } else {
                exitSelectionMode(); // ✅ Fallback
            }

            return true; // Consumed
        }

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            return adapter.onBackPressed();
        }

        return false; // Not consumed
    }


    // ==================== NEW: Quick Events System Initialization ====================

    /**
     * ✅ NEW: Initialize Quick Events system
     * Sets up templates and validates configuration
     */
    private void initializeQuickEventsSystem() {
        try {
            Log.d(TAG, "Initializing Quick Events system...");

            // Load all quick event templates
            mQuickEventTemplates = QuickEventTemplate.getAllTemplates();

            // Validate templates
            if (QuickEventTemplate.validateAllTemplates()) {
                mQuickEventsInitialized = true;
                Log.d(TAG, "✅ Quick Events system initialized successfully with " +
                        mQuickEventTemplates.size() + " templates");
            } else {
                Log.e(TAG, "❌ Quick Events template validation failed");
                mQuickEventsInitialized = false;
            }

            // Initialize user context (can be overridden by subclasses)
            initializeUserContext();

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize Quick Events system: " + e.getMessage());
            mQuickEventsInitialized = false;
        }
    }

    /**
     * ✅ NEW: Initialize user context for event creation
     * Subclasses can override to provide specific user context
     */
    protected void initializeUserContext() {
        // Default implementation - get user from session/preferences
        // Subclasses should override with specific user management logic
        mCurrentUserId = getCurrentUserId();

        Log.d(TAG, "User context initialized: userId=" + mCurrentUserId);
    }

    /**
     * ✅ NEW: Get current user ID
     * Subclasses should override with actual user management integration
     */
    protected Long getCurrentUserId() {
        // TODO: Integrate with actual user session management
        // For now, return a default user ID or null
        return null; // Will be handled gracefully by UserEventLogicAdapter
    }

    /**
     * ✅ NEW: Cleanup Quick Events system
     */
    private void cleanupQuickEventsSystem() {
        if (mQuickEventTemplates != null) {
            mQuickEventTemplates.clear();
            mQuickEventTemplates = null;
        }
        mQuickEventsInitialized = false;
        Log.d(TAG, "Quick Events system cleaned up");
    }

    // ===========================================
    // Setup Methods (Enhanced)
    // ===========================================


    /**
     * Initialize bottom selection toolbar
     */
    private void setupBottomToolbar() {
        if (getContext() == null) return;

        mBottomToolbar = new BottomSelectionToolbar(getContext());
        Log.d(TAG, "setupBottomToolbar: ✅ Bottom toolbar initialized");
    }

    /**
     * Setup regular click listener for events preview
     */
    protected void setupAdapterRegularClickListener() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            // Setup regular click listener for events preview
            adapter.setRegularClickListener(new BaseClickAdapterLegacy.DayRegularClickListener() {
                @Override
                public void onDayRegularClick(Day day, LocalDate date, View itemView, int position) {
                    handleDayRegularClick(day, date, itemView, position);
                }
            });

            Log.d(TAG, getFragmentName() + ": Regular click listener setup in adapter");
        } else {
            Log.e(TAG, "setupAdapterRegularClickListener: ❌ Adapter is null, cannot setup regular click listener");
        }
    }

    /**
     * Setup long-click listener in adapter
     */
    protected void setupAdapterLongClickListener() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            adapter.setLongClickListener(this);
            Log.d(TAG, "setupAdapterLongClickListener: ✅ Long-click listener setup in adapter");
        } else {
            Log.e(TAG, "setupAdapterLongClickListener: ❌ Adapter is null, cannot setup long-click listener ");
        }
    }

    // ===========================================
    // DayLongClickListener Implementation
    // ===========================================

    @Override
    public void onDayLongClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, "onDayLongClick: Day long-clicked: " + date);

        // ❌ remove: enterSelectionMode()
        // ✅ ONLY feedback UI non-state
        updateSelectionUI(); // Update UI, no state change
    }

    @Override
    public void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date) {
        Log.d(TAG, "onToolbarActionSelected: Toolbar action selected: " + action + " for date: " + date);

        switch (action) {
            case ADD_EVENT:
                openEventEditor(date);
                break;

            case VIEW_EVENTS:
                showEventsDialog(date);
                break;

            // ✅ ENHANCED: Quick Event Actions with proper integration
            case FERIE:
            case MALATTIA:
            case LEGGE_104:
            case PERMESSO:
            case PERMESSO_SINDACALE:
            case STRAORDINARIO:
                // These are handled by adapter, but we can show confirmation
                handleQuickEventCreation(action, date);                break;

            default:
                Log.w(TAG, "Unhandled toolbar action: " + action);
        }

        // Exit selection mode after action
        exitSelectionMode();
    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        Log.d(TAG, getFragmentName() + ": Selection mode changed: " + isSelectionMode +
                ", count: " + selectedCount);

        // ✅ SOLO UI UPDATES
        // selectedCount is still 0.. ( && selectedCount > 0 )
        if (isSelectionMode) {
            enterSelectionMode();
            Log.d(TAG, "UI updated: (true) " + selectedCount);
        } else {
            exitSelectionMode();
            Log.d(TAG, "UI updated: (false) " + selectedCount);
        }

//
//        // 🔧 FIX: Store previous mode before updating
//        boolean previousMode = mIsSelectionMode;
//        mIsSelectionMode = isSelectionMode;

//        if (isSelectionMode && selectedCount > 0) {
//            // Enter/update selection mode
//            if (!mIsSelectionMode) {
//                enterSelectionMode();
//            }
//            updateBottomToolbar();
//        } else {
//            // Exit selection mode
//            exitSelectionMode();
//        }
//    }

//        if (isSelectionMode && !previousMode) {
//            // Entering selection mode
//            Log.d(TAG, getFragmentName() + ": Entering selection mode");
//            enterSelectionMode();
//        } else if (!isSelectionMode && previousMode) {
//            // Exiting selection mode
//            Log.d(TAG, getFragmentName() + ": Exiting selection mode");
//            exitSelectionMode();
//        }
//
//        updateSelectionUI();
//        updateActionBarTitle(selectedCount);
//
//        // 🔧 FIX: Special handling for auto-exit (now previousMode is correctly defined)
//        if (!isSelectionMode && previousMode && selectedCount == 0) {
//            Log.d(TAG, getFragmentName() + ": Auto-exit from selection mode detected (no items selected)");
//            // Additional cleanup or notifications can be added here
//            onAutoExitSelectionMode();
//        }
    }

    /**
     * 🔧 NEW: Called when selection mode is auto-exited due to no items selected
     * Subclasses can override for specific behavior
     */
    protected void onAutoExitSelectionMode() {
        Log.d(TAG, getFragmentName() + ": Auto-exit selection mode completed");

        // Default implementation - subclasses can override for specific behavior
        // For example: show toast, update status, etc.

        // Optional: Show subtle feedback that selection mode was exited
        if (getContext() != null) {
            // Uncomment if you want user feedback:
            // Toast.makeText(getContext(), "Selezione annullata", Toast.LENGTH_SHORT).show();
        }
    }

//        BaseClickAdapterLegacy adapter = getClickAdapter();
//        if (adapter != null) {
//            int selectedCount = adapter.getSelectedCount();
//            updateActionBarTitle(selectedCount);
//
//            // 🔧 NEW: Log selection count for debugging
//            Log.d(TAG, getFragmentName() + ": Current selection count: " + selectedCount);
//        }
//    }


    // ==================== NEW: Quick Event Creation Logic ====================

    /**
     * ✅ ENHANCED: Handle quick event creation with confirmation dialog
     * Main entry point for toolbar action → confirmation dialog → actual event creation
     */
    private void handleQuickEventCreation(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "handleQuickEventCreation: Creating quick event " + action + " for date " + date);

        // Validate Quick Events system initialization
        if (!mQuickEventsInitialized) {
            Log.e(TAG, "❌ Quick Events system not initialized");
            showQuickEventError("Sistema eventi rapidi non disponibile", action, date);
            return;
        }

        // Get template for this action
        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) {
            Log.e(TAG, "❌ No template found for action: " + action);
            showQuickEventError("Template non trovato per " + action.getEventDisplayName(), action, date);
            return;
        }

        // Validate template can be used on this date
        if (!template.canUseOnDate(date, mCurrentUserId)) {
            Log.w(TAG, "⚠️ Template cannot be used on date: " + date + " for action: " + action);
            String eventName = template.getDisplayName();
            showQuickEventError("Impossibile creare '" + eventName + "' per " + date, action, date);
            return;
        }

        // ✅ NEW: Show confirmation dialog instead of creating event directly
        showQuickEventConfirmationDialog(action, date, template);
    }

    /**
     * ✅ NEW: Show confirmation dialog for quick event creation
     * Provides preview and editing capabilities before final creation
     */
    private void showQuickEventConfirmationDialog(ToolbarAction action, LocalDate date, QuickEventTemplate template) {
        Log.d(TAG, "showQuickEventConfirmationDialog: Showing dialog for " + action + " on " + date);

        try {
            // Create and show confirmation dialog with event creation listener
            QuickEventConfirmationDialog.showConfirmationDialog(
                    getContext(),
                    action,
                    date,
                    mCurrentUserId,
                    template,
                    new QuickEventConfirmationDialogListener()
            );

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to show confirmation dialog: " + e.getMessage());
            showQuickEventError("Errore nell'apertura del dialog: " + e.getMessage(), action, date);
        }
    }

    /**
     * ✅ NEW: Event creation listener for confirmation dialog callbacks
     * Handles results from the confirmation dialog
     */
    private class QuickEventConfirmationDialogListener implements QuickEventConfirmationDialog.EventCreationListener {

        @Override
        public void onEventCreated(LocalEvent createdEvent, ToolbarAction sourceAction, LocalDate date) {
            Log.d(TAG, "onEventCreated: Event created successfully via dialog: " + createdEvent.getTitle());

            // Trigger fragment-level event handling
            onQuickEventCreated(sourceAction, date);

            // Analytics or additional tracking can be added here
            trackQuickEventCreation(sourceAction, date, true);
        }


        @Override
        public void onEventCreationCancelled(ToolbarAction sourceAction, LocalDate date) {
            Log.d(TAG, "onEventCreationCancelled: User cancelled event creation for " + sourceAction + " on " + date);

            // Optional: Analytics tracking for cancellations
            trackQuickEventCreation(sourceAction, date, false);
        }

        @Override
        public void onEventCreationFailed(ToolbarAction sourceAction, LocalDate date, String errorMessage) {
            Log.e(TAG, "onEventCreationFailed: " + errorMessage + " for " + sourceAction + " on " + date);

            // Show error to user (confirmation dialog already shows Toast, but we can add more here)
            onQuickEventCreationError(sourceAction, date, errorMessage);
        }

        @Override
        public void onEventsRefreshNeeded() {
            Log.d(TAG, "onEventsRefreshNeeded: Triggering events refresh");

            // Trigger events refresh in the fragment
            onForceEventsRefresh();
        }
    }

    /**
     * ✅ NEW: Handle quick event creation error
     * Called when event creation fails for any reason
     */
    protected void onQuickEventCreationError(ToolbarAction action, LocalDate date, String errorMessage) {
        Log.e(TAG, "Quick event creation error: " + errorMessage + " for " + action + " on " + date);

        // Subclasses can override for specific error handling
        // Default implementation: just log (error Toast already shown by dialog)
    }

    /**
     * ✅ NEW: Track quick event creation for analytics
     * Optional analytics tracking for usage patterns
     */
    protected void trackQuickEventCreation(ToolbarAction action, LocalDate date, boolean success) {
        // Subclasses can override for analytics integration
        // Default implementation: just log
        Log.d(TAG, "Quick event tracking: " + action + " for " + date + " success=" + success);
    }

    /**
     * ✅ DEPRECATED: Old method - now redirects to new confirmation dialog system
     * Keeping for backward compatibility
     */
    @Deprecated
    protected boolean saveQuickEvent(LocalEvent event, ToolbarAction action, LocalDate date) {
        Log.w(TAG, "saveQuickEvent: This method is deprecated. Use QuickEventConfirmationDialog instead.");

        // This method is now handled by QuickEventConfirmationDialog
        // Keeping minimal implementation for backward compatibility
        return false;
    }

    /**
     * ✅ DEPRECATED: Old method - now handled by confirmation dialog
     * Keeping for backward compatibility
     */
    @Deprecated
    private void showQuickEventSuccess(LocalEvent event, ToolbarAction action, LocalDate date) {
        Log.w(TAG, "showQuickEventSuccess: This method is deprecated. Use QuickEventConfirmationDialog instead.");

        // Success feedback is now handled by QuickEventConfirmationDialog
    }

    /**
     * ✅ NEW: Show error message for quick event creation
     */
    private void showQuickEventError(String errorMessage, ToolbarAction action, LocalDate date) {
        if (getContext() == null) return;

        String message = "❌ " + errorMessage;
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        Log.e(TAG, "Quick event error shown: " + errorMessage + " for action " + action + " on " + date);
    }

    /**
     * ✅ NEW: Format date for user display
     * Can be overridden for localized formatting
     */
    protected String formatDateForUser(LocalDate date) {
        // Simple formatting - can be enhanced with proper localization
        return date.toString(); // TODO: Use DateTimeFormatter with locale
    }


    // ==================== ENHANCED: Event Creation Methods ====================

    /**
     * ✅ ENHANCED: Show quick event confirmation with template preview
     * Replaces the simple Toast implementation
     */
    protected void showQuickEventConfirmation(ToolbarAction action, LocalDate date) {
        // This method is now handled by handleQuickEventCreation
        // Keeping for backward compatibility
        handleQuickEventCreation(action, date);
        //onQuickEventCreated(action, date);
    }




    // ===========================================
    // Selection Mode UI Management
    // ===========================================

    /**
     * Proxy for ADAPTER: Enter selection mode (update UI accordingly?)
     */
    protected void enterSelectionMode() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null && !adapter.isSelectionMode()) {
            adapter.setSelectionMode(true);
        }

        hideFabForSelection();
        showBottomToolbar();
        updateActionBarForSelection(isSelectionMode());
        updateBottomToolbar();
        // TODO: update fab visibility
        //toggleFabVisibility(mFabGoToToday);
        updateActionBarTitle(getCurrentSelectionCount());

        Log.d(TAG, "enterSelectionMode: Entered selection mode");
    }


    /**
     * ✅ REFACTORED: Exit selection mode - eliminato clear diretto
     */
    protected void exitSelectionMode() {
        if (isSelectionMode()) return; // false is ok(already updated)

        // ✅ Update action bar/toolbar
        updateActionBarForSelection(false);

        // ✅ Show FAB again
        showFabAfterSelection();

        // ✅ Hide bottom toolbar
        hideBottomToolbar();

        // ✅ SOLO setSelectionMode - il clear è gestito dall'adapter
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false); // ✅ Questo gestirà automaticamente il clear se necessario
        }

        Log.d(TAG, "exitSelectionMode: Exited selection mode");
    }
//    /**
//     * Exit selection mode - restore normal UI
//     */
//    protected void exitSelectionMode() {
//        // Exit if in selection mode
//        if (!isSelectionMode()) return;
//
//        // Update action bar/toolbar
//        updateActionBarForSelection(false);
//
//        // Show FAB again
//        showFabAfterSelection();
//
//        // Hide bottom toolbar
//        hideBottomToolbar();
//
//        // Disable selection mode in adapter
//        BaseClickAdapterLegacy adapter = getClickAdapter();
//        if (adapter != null && adapter.isSelectionMode()) {
//            adapter.setSelectionMode(false);
//            adapter.clearSelections();
//        }
//
//        Log.d(TAG, "exitSelectionMode: Exited selection mode");
//    }

    /**
     * Show bottom toolbar with current selection
     */
    private void showBottomToolbar() {
        if (mBottomToolbar == null) {
            Log.e(TAG, "showBottomToolbar: ❌ Bottom toolbar is null");
            return;
        }

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter == null) return;

        ViewGroup container = getToolbarContainer();
        if (container == null) {
            Log.e(TAG, "showBottomToolbar: ❌ No toolbar container available");
            return;
        }

        Set<LocalDate> selectedDates = adapter.getSelectedDates();
        mBottomToolbar.show(container, selectedDates, this);
    }

    /**
     * Hide bottom toolbar
     */
    private void hideBottomToolbar() {
        if (mBottomToolbar != null) {
            mBottomToolbar.hide();
        } else {
            Log.e(TAG, "hideBottomToolbar: ❌ Bottom toolbar is null");
        }
    }

    /**
     * Hide FAB during selection mode
     */
    protected void hideFabForSelection() {

        if (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mHasSelectionHiddenFab = true;
            toggleFabVisibility(mFabGoToToday);
            mFabGoToToday.hide();
        }
    }

    /**
     * Show FAB after exiting selection mode
     */
    protected void showFabAfterSelection() {
        if (mFabGoToToday != null && mHasSelectionHiddenFab) {
            mHasSelectionHiddenFab = false;
            toggleFabVisibility(mFabGoToToday);
            mFabGoToToday.show();
        }
    }

    /**
     * Setup action bar for selection mode
     * mock default implementation
     */
    protected void setupSelectionActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific selection action bar setup
        // For now, just update the title

        updateActionBarTitle(getCurrentSelectionCount());

    }

    /**
     * Restore normal action bar
     * mock default implementation
     */
    protected void restoreNormalActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific action bar restoration
        // For now, just reset the title

        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(getFragmentName());
        }
    }

    // ===========================================
    // UI Update Methods
    // ===========================================

    /**
     * Update UI elements based on current selection state
     */
    protected void updateSelectionUI() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter == null) return;

        int selectedCount = adapter.getSelectedCount();

        // Update toolbar
        updateBottomToolbar();

        // Update action bar title
        updateActionBarTitle(selectedCount);

        // Update any other UI elements
        updateToolbarMenuItems();
    }

    /**
     * Update action bar title with selection count
     */
    protected void updateActionBarTitle(int selectedCount) {
        if (isSelectionMode() && getActivity() != null) {
            String title = selectedCount > 0 ?
                    String.format(QDue.getLocale(), "%d selezionati", selectedCount) :
                    "Seleziona";

            // Update action bar title
            if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(title);
            }

            Log.d(TAG, "Updated action bar title: " + title);
        }
    }

    /**
     * Update action bar for selection mode
     */
    protected void updateActionBarForSelection(boolean isSelectionMode) {
        if (getActivity() instanceof QDueMainActivity mainActivity) {

            // Custom action bar if needed
            if (isSelectionMode) {
                // Show selection action bar
                setupSelectionActionBar(mainActivity);
                Log.d(TAG, "Showing selection action bar");
            } else {
                // Restore normal action bar
                restoreNormalActionBar(mainActivity);
                Log.d(TAG, "Restoring normal action bar");
            }
        }
    }

    /**
     * Update toolbar menu items
     */
    protected void updateToolbarMenuItems() {
        // Update menu items based on selection state
        if (mSelectAllMenuItem != null) {
            mSelectAllMenuItem.setVisible(isSelectionMode());
        }
        if (mClearSelectionMenuItem != null) {
            mClearSelectionMenuItem.setVisible(isSelectionMode());
        }
        if (mExitSelectionMenuItem != null) {
            mExitSelectionMenuItem.setVisible(isSelectionMode());
        }
    }

    /**
     * Update bottom toolbar with current selection
     */
    private void updateBottomToolbar() {
        if (mBottomToolbar == null || !mBottomToolbar.isVisible()) return;

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            Set<LocalDate> selectedDates = adapter.getSelectedDates();
            mBottomToolbar.updateSelection(selectedDates);
        }
    }


    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * Check if currently in selection mode with items selected
     */
    public boolean hasActiveSelection() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return isSelectionMode() && adapter != null && adapter.getSelectedCount() > 0;
    }

    /**
     * Get current selection count safely
     */
    public int getCurrentSelectionCount() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }

    // ===========================================
    // Event Handling Methods
    // ===========================================

    /**
     * Show events dialog for the specified date
     */
    protected void showEventsDialog(LocalDate date) {
        // Get events for this date
        List<LocalEvent> events = getEventsForDate(date);
        onShowEventsDialog(date, events);
    }
//        Log.d(TAG, "Showing events dialog for date: " + date);
//
//        // Get events for this date
//        Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
//        List<LocalEvent> events = eventsCache.get(date);
//
//        if (events == null || events.isEmpty()) {
//            // Show "no events" message
//            showNoEventsMessage(date);
//        } else {
//            // Show events list dialog
//            showEventsListDialog(date, events);
//        }
//
//        // Delegate to subclass for specific handling
//        onShowEventsDialog(date, events);
//    }



    /**
     * Get events for a specific date from cache
     * default implementation -  sub classes should override
     */
    protected List<LocalEvent> getEventsForDate(LocalDate date) {
        Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
        List<LocalEvent> events = eventsCache.get(date);
        return events != null ? events : new ArrayList<>();
    }

    // ===========================================
    // Template Methods for Subclasses
    // ===========================================

    /**
     * Called when event editor should be opened
     * Subclasses should override for specific navigation
     */
    protected void onOpenEventEditor(LocalDate date) {
        Log.d(TAG, "Opening event editor for date: " + date);
        // Default implementation - subclasses can override
    }

    /**
     * Called when events dialog should be shown
     */
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        Log.d(TAG, "Showing events dialog for date: " + date);
        // Default implementation - subclasses can override
    }

      /**
     * ✅ ENHANCED: Called when a quick event is created
     * Now includes actual event creation logic
     */
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "Quick event created: " + action + " for date: " + date);

        // Notify subclasses that an event was created
        // Subclasses can override for specific handling (analytics, notifications, etc.)
    }

    /**
     * ✅ NEW: Called when quick events system needs user context
     * Subclasses can override to provide specific user ID logic
     */
    protected void onQuickEventsUserContextNeeded() {
        Log.d(TAG, "Quick events system requesting user context");

        // Re-initialize user context
        initializeUserContext();
    }

    // ===========================================
    // Menu Handling
    // ===========================================

    /**
     * Handle options menu item selection for selection mode
     */
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter == null) return false;

        // Handle selection mode menu items
        int itemId = item.getItemId();

        // These would be defined in menu XML
        /*
        if (itemId == R.id.action_select_all) {

        for (LocalDate date : getAllAvailableDates()) {
        if (!mSelectedDates.contains(date)) {
            toggleDateSelection(date);
        }
    }

            //adapter.selectAllDays();
            return true;
        } else if (itemId == R.id.action_clear_selection) {
        if (!mSelectedDates.isEmpty()) {
        Set<LocalDate> toDeselect = new HashSet<>(mSelectedDates);
        for (LocalDate date : toDeselect) {
            toggleDateSelection(date);
        }
    }
            //adapter.clearSelections(); // avoid this -> adapter.setSelectionMode(false)
            return true;
        } else if (itemId == R.id.action_exit_selection) {
            exitSelectionMode();
            return true;
        }
        */

        return false;
    }

    // ===========================================
    // Placeholder Methods (to be replaced by actual implementations)
    // ===========================================

    /**
     * Show placeholder for event editor
     */
    protected void showEventEditorPlaceholder(LocalDate date) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Apertura editor eventi per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show no events message
     */
    protected void showNoEventsMessage(LocalDate date) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Nessun evento per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show events list dialog
     */
    protected void showEventsListDialog(LocalDate date, List<LocalEvent> events) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    events.size() + " eventi per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ===========================================
    // Public API Methods
    // ===========================================

    /**
     * Public method to get selected dates
     */
    public Set<LocalDate> getSelectedDates() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedDates() : new java.util.HashSet<>();
    }

    /**
     * Proxy for ADAPTER: check if it's in selection mode
     */
    public boolean isSelectionMode() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null && adapter.isSelectionMode();
    }

    /**
     * Public method to get selected count
     */
    public int getSelectedCount() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }


    /**
     * ✅ REFACTORED: Force exit - eliminato clear diretto
     */
    public void forceExitSelectionMode() {
        Log.d(TAG, getFragmentName() + ": Force exit selection mode requested");

        if (isSelectionMode()) {
            BaseClickAdapterLegacy adapter = getClickAdapter();
            if (adapter != null) {
                adapter.setSelectionMode(false); // ✅ Questo gestirà tutto automaticamente
            } else {
                exitSelectionMode(); // ✅ Fallback
            }
        }
    }
//    /**
//     * 🔧 NEW: Force exit selection mode (public method for external calls)
//     */
//    public void forceExitSelectionMode() {
//        Log.d(TAG, getFragmentName() + ": Force exit selection mode requested");
//
//        if (isSelectionMode()) {
//            BaseClickAdapterLegacy adapter = getClickAdapter();
//            if (adapter != null) {
//                adapter.deselectAll(); // This will trigger auto-exit
//            } else {
//                // Fallback: exit directly
//                exitSelectionMode();
//            }
//        }
//    }


    /**
     * ✅ NEW: Get quick event template for action
     */
    public QuickEventTemplate getQuickEventTemplate(ToolbarAction action) {
        return mQuickEventTemplates != null ? mQuickEventTemplates.get(action) : null;
    }

    /**
     * ✅ NEW: Check if quick events system is available
     */
    public boolean isQuickEventsAvailable() {
        return mQuickEventsInitialized && mQuickEventTemplates != null;
    }

    /**
     * ✅ NEW: Get preview for quick event creation
     */
    public QuickEventTemplate.EventPreview getQuickEventPreview(ToolbarAction action, LocalDate date) {
        QuickEventTemplate template = getQuickEventTemplate(action);
        return template != null ? template.getPreview(date) : null;
    }



    /// ////////////////////////////////////////////////////////////////////////////////////

    // ===========================================
    // NUOVO: Events Preview Integration
    // ===========================================

    /**
     * Initialize events preview manager in onCreate or onViewCreated
     */
    protected void initializeEventsPreview() {
        mEventsPreviewManager = new EventsPreviewManager(requireContext());
        mEventsPreviewManager.setEventsPreviewListener(new EventsPreviewListenerImpl());

        // Set view type based on fragment type
        EventsPreviewManager.ViewType viewType = getEventsPreviewViewType();
        mEventsPreviewManager.setViewType(viewType);

        Log.d(TAG, getFragmentName() + ": Events preview initialized with type: " + viewType);
    }

    /**
     * Abstract method for subclasses to specify their view type
     */
    protected abstract EventsPreviewManager.ViewType getEventsPreviewViewType();

    /**
     * Handle regular click on day (non-selection mode)
     */
    protected void handleDayRegularClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, getFragmentName() + ": Regular click on date: " + date);

        if (!mEventsPreviewEnabled) {
            Log.v(TAG, "Events preview disabled, ignoring click");
            return;
        }

        // Get events for this date
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            // No events - show "add event" option or ignore
            handleNoEventsClick(date, itemView);
        } else {
            // Has events - show preview
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.showEventsPreview(date, events, itemView);
            }
        }
    }

    /**
     * Handle click on date with no events
     */
    protected void handleNoEventsClick(LocalDate date, View itemView) {
        Log.d(TAG, getFragmentName() + ": Click on date with no events: " + date);

        // TODO: offer to add event
//        showAddEventOption(date);
    }

    /**
     * Show option to add event for date with no events
     */
    protected void showAddEventOption(LocalDate date) {
        if (getContext() != null) {
            // Simple toast for now - can be enhanced with quick add dialog
            String message = "Nessun evento per " + date + ". Tocca a lungo per aggiungere.";
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

// ===========================================
// Events Preview Listener Implementation
// ===========================================

    /**
     * Implementation of EventsPreviewListener for handling events preview callbacks
     */
    private class EventsPreviewListenerImpl implements EventsPreviewInterface.EventsPreviewListener {

        @Override
        public void onEventQuickAction(EventsPreviewInterface.EventQuickAction action,
                                       LocalEvent event, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Event quick action: " + action + " for event: " + event.getTitle());

            switch (action) {
                case EDIT:
                    openEventEditor(event, date);
                    break;
                case DELETE:
                    confirmDeleteEvent(event, date);
                    break;
                case DUPLICATE:
                    duplicateEvent(event, date);
                    break;
                case TOGGLE_COMPLETE:
                    toggleEventComplete(event, date);
                    break;
            }

            // Hide preview after action
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.hideEventsPreview();
            }
        }

        @Override
        public void onEventsGeneralAction(EventsPreviewInterface.EventGeneralAction action, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Events general action: " + action + " for date: " + date);

            switch (action) {
                case ADD_EVENT:
                    openEventEditor(date);
                    break;
                case NAVIGATE_TO_EVENTS_ACTIVITY:
                    navigateToEventsActivity(date);
                    break;
                case REFRESH_EVENTS:
                    onForceEventsRefresh();
                    break;
            }

            // Hide preview after action
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.hideEventsPreview();
            }
        }

        @Override
        public void onEventsPreviewShown(LocalDate date, int eventCount) {
            Log.d(TAG, getFragmentName() + ": Events preview shown for " + date + " with " + eventCount + " events");

            // Optional: Analytics, state tracking, etc.
        }

        @Override
        public void onEventsPreviewHidden(LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Events preview hidden for " + date);

            // Optional: Cleanup, state tracking, etc.
        }
    }

    // ===========================================
    // Event Action Handlers
    // ===========================================

    /**
     * Open event editor for existing event
     */
    protected void openEventEditor(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Opening event editor for event: " + event.getTitle());

        // TODO: Implement navigation to event editor with event data
        // For now, delegate to base implementation
        onOpenEventEditor(date);
    }

    /**
     * Open event editor for new event on date
     */
    protected void openEventEditor(LocalDate date) {
        Log.d(TAG, "Opening event editor for new event on date: " + date);

        // TODO: Navigate to event editor with pre-filled date
        // Example using Navigation Component or Intent

        // For now, show a toast and delegate to subclass
        showEventEditorPlaceholder(date);

        // Delegate to base implementation
        onOpenEventEditor(date);
    }

    /**
     * Confirm and delete event
     */
    protected void confirmDeleteEvent(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Confirming delete for event: " + event.getTitle());

        if (getContext() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Elimina evento")
                .setMessage("Vuoi eliminare l'evento \"" + event.getTitle() + "\"?")
                .setPositiveButton("Elimina", (dialog, which) -> deleteEvent(event, date))
                .setNegativeButton("Annulla", null)
                .show();
    }

    /**
     * Delete event
     */
    protected void deleteEvent(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Deleting event: " + event.getTitle());

        // TODO: Implement actual event deletion
        // For now, show placeholder
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Evento \"" + event.getTitle() + "\" eliminato",
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        // Refresh events
        onForceEventsRefresh();
    }

    /**
     * Duplicate event
     */
    protected void duplicateEvent(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Duplicating event: " + event.getTitle());

        // TODO: Implement event duplication
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Evento \"" + event.getTitle() + "\" duplicato",
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        // Refresh events
        onForceEventsRefresh();
    }

    /**
     * Toggle event completion status
     */
    protected void toggleEventComplete(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Toggling completion for event: " + event.getTitle());

        // TODO: Implement completion toggle
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Stato evento \"" + event.getTitle() + "\" cambiato",
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        // Refresh events
        onForceEventsRefresh();
    }

    /**
     * Navigate to events activity
     */
    protected void navigateToEventsActivity(LocalDate date) {
        Log.d(TAG, "Navigating to events activity for date: " + date);

        // TODO: Implement navigation to events activity
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Apertura eventi per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

// ===========================================
// Public API Methods
// ===========================================

    /**
     * Enable/disable events preview
     */
    public void setEventsPreviewEnabled(boolean enabled) {
        mEventsPreviewEnabled = enabled;

        if (!enabled && mEventsPreviewManager != null) {
            mEventsPreviewManager.hideEventsPreview();
        }
    }

    /**
     * Check if events preview is enabled
     */
    public boolean isEventsPreviewEnabled() {
        return mEventsPreviewEnabled;
    }

    /**
     * Force hide events preview
     */
    public void hideEventsPreview() {
        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.hideEventsPreview();
        }
    }

    /**
     * Check if events preview is currently showing
     */
    public boolean isEventsPreviewShowing() {
        return mEventsPreviewManager != null && mEventsPreviewManager.isEventsPreviewShowing();
    }


    // ==================== NEW: Bulk Quick Events Creation ====================


    /**
     * ✅ ENHANCED: Handle bulk quick event creation with confirmation
     * Now shows preview and confirmation for bulk operations
     */
    public void handleBulkQuickEventCreation(ToolbarAction action, Set<LocalDate> selectedDates) {
        if (selectedDates == null || selectedDates.isEmpty()) {
            Log.w(TAG, "No dates selected for bulk event creation");
            return;
        }

        Log.d(TAG, "handleBulkQuickEventCreation: Creating " + action + " for " + selectedDates.size() + " dates");

        if (!mQuickEventsInitialized) {
            showQuickEventError("Sistema eventi rapidi non disponibile", action, null);
            return;
        }

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) {
            showQuickEventError("Template non trovato per " + action.getEventDisplayName(), action, null);
            return;
        }

        // ✅ ENHANCED: Show bulk confirmation dialog with better preview
        showBulkEventConfirmationDialog(action, selectedDates, template);
    }

    /**
     * ✅ ENHANCED: Show confirmation dialog for bulk event creation with validation preview
     */
    private void showBulkEventConfirmationDialog(ToolbarAction action, Set<LocalDate> selectedDates, QuickEventTemplate template) {
        if (getContext() == null) return;

        String eventName = template.getDisplayName();
        int totalDates = selectedDates.size();
        int validDates = getValidDatesCount(action, selectedDates);
        int invalidDates = totalDates - validDates;

        // Build confirmation message with validation info
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("Creare evento '%s' per:", eventName));
        messageBuilder.append("\n\n");
        messageBuilder.append("✅ ").append(validDates).append(" date valide");

        if (invalidDates > 0) {
            messageBuilder.append("\n❌ ").append(invalidDates).append(" date non disponibili");
            messageBuilder.append("\n\nGli eventi saranno creati solo per le date valide.");
        }

        String title = String.format("Conferma %d eventi", validDates);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(messageBuilder.toString())
                .setPositiveButton("Crea", (dialog, which) -> {
                    if (validDates > 0) {
                        executeBulkEventCreation(action, selectedDates, template);
                    }
                })
                .setNegativeButton("Annulla", (dialog, which) -> {
                    trackQuickEventCreation(action, null, false);
                })
                .setIcon(template.getEventType().getEmoji().codePointAt(0)) // Use emoji as icon
                .show();
    }

    /**
     * ✅ ENHANCED: Execute bulk event creation with proper database integration
     * Now uses real EventDao for persistence
     */
    private void executeBulkEventCreation(ToolbarAction action, Set<LocalDate> selectedDates, QuickEventTemplate template) {
        Log.d(TAG, "executeBulkEventCreation: Creating events for " + selectedDates.size() + " dates");

        // Get database access
        EventDao eventDao = QDueDatabase.getInstance(getContext()).eventDao();

        // Execute in background thread
        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<LocalEvent> createdEvents = new ArrayList<>();

            for (LocalDate date : selectedDates) {
                try {
                    if (template.canUseOnDate(date, mCurrentUserId)) {
                        LocalEvent event = template.createEvent(date, mCurrentUserId);

                        if (event != null) {
                            // Save to database using EventDao
                            long rowId = eventDao.insertEvent(event);

                            if (rowId > 0) {
                                successCount++;
                                createdEvents.add(event);
                                Log.d(TAG, "✅ Bulk event created: " + event.getTitle() + " for " + date);
                            } else {
                                errorCount++;
                                errors.add("Errore salvataggio per " + date);
                            }
                        } else {
                            errorCount++;
                            errors.add("Errore creazione per " + date);
                        }
                    } else {
                        errorCount++;
                        errors.add("Non disponibile per " + date);
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Errore per " + date + ": " + e.getMessage());
                    Log.e(TAG, "Error creating bulk event for date " + date, e);
                }
            }

            final int finalSuccessCount = successCount;
            final int finalErrorCount = errorCount;
            final List<String> finalErrors = errors;

            // Switch back to main thread for UI updates
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Show results
                    showBulkEventResults(action, finalSuccessCount, finalErrorCount, errors);

                    // Refresh events if any were created successfully
                    if (finalSuccessCount > 0) {
                        onForceEventsRefresh();
                        exitSelectionMode(); // Exit selection mode after successful bulk creation

                        // Track successful bulk creation
                        trackQuickEventCreation(action, null, true);
                    } else {
                        // Track failed bulk creation
                        trackQuickEventCreation(action, null, false);
                    }
                });
            }
        });
    }

    /**
     * ✅ NEW: Show results of bulk event creation
     */
    private void showBulkEventResults(ToolbarAction action, int successCount, int errorCount, List<String> errors) {
        if (getContext() == null) return;

        String eventName = action.getEventDisplayName();

        if (errorCount == 0) {
            // All successful
            String message = String.format(QDue.getLocale(),
                    "✅ %d eventi '%s' creati con successo", successCount, eventName);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        } else if (successCount == 0) {
            // All failed
            String message = "❌ Nessun evento creato. Errori riscontrati.";
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        } else {
            // Mixed results
            String message = String.format(QDue.getLocale(),
                    "⚠️ %d eventi creati, %d errori", successCount, errorCount);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, String.format(QDue.getLocale(), "Bulk event results: %d success, %d errors for action %s",
                successCount, errorCount, action));
    }


    // ==================== NEW: Quick Events Validation ====================

    /**
     * ✅ NEW: Validate quick event creation before showing UI
     * Can be used by toolbars/adapters to enable/disable actions
     */
    public boolean canCreateQuickEvent(ToolbarAction action, LocalDate date) {
        if (!mQuickEventsInitialized) return false;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return false;

        return template.canUseOnDate(date, mCurrentUserId);
    }

    /**
     * ✅ NEW: Validate bulk quick event creation
     */
    public boolean canCreateBulkQuickEvents(ToolbarAction action, Set<LocalDate> dates) {
        if (!mQuickEventsInitialized || dates == null || dates.isEmpty()) return false;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return false;

        // Check if at least one date is valid
        return dates.stream().anyMatch(date -> template.canUseOnDate(date, mCurrentUserId));
    }

    /**
     * ✅ NEW: Get count of valid dates for bulk creation
     */
    public int getValidDatesCount(ToolbarAction action, Set<LocalDate> dates) {
        if (!mQuickEventsInitialized || dates == null) return 0;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return 0;

        return (int) dates.stream()
                .filter(date -> template.canUseOnDate(date, mCurrentUserId))
                .count();
    }




    // ===========================================
    // Debug Methods Enhanced
    // ===========================================

    /**
     * Debug long-click integration
     */
    public void debugLongClickIntegration() {
        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " LONG-CLICK INTEGRATION DEBUG ===");

        debugSelectionState();

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            // Call adapter debug if available
            try {
                java.lang.reflect.Method debugMethod = adapter.getClass().getMethod("debugSelectionState");
                debugMethod.invoke(adapter);
            } catch (Exception e) {
                Log.d(TAG, "Adapter debug method not available: " + e.getMessage());
            }
        }

        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " LONG-CLICK DEBUG ===");
    }

    /**
     * Enhanced debug with events preview state
     */
    public void debugSelectionState() {
        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " SELECTION STATE DEBUG ===");
        Log.d(TAG, "Fragment Selection Mode: " + isSelectionMode());

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            Log.d(TAG, "Adapter Selection Mode: " + adapter.isSelectionMode());
            Log.d(TAG, "Selected Count: " + adapter.getSelectedCount());
            Log.d(TAG, "Selected Dates: " + adapter.getSelectedDates());
            Log.d(TAG, "Has Active Selection: " + hasActiveSelection());

            // 🔧 NEW: Check for inconsistencies
            if (isSelectionMode() != adapter.isSelectionMode()) {
                Log.w(TAG, "⚠️ INCONSISTENCY: Fragment and Adapter selection mode mismatch!");
            }

            if (isSelectionMode() && adapter.getSelectedCount() == 0) {
                Log.w(TAG, "⚠️ WARNING: In selection mode but no items selected (should auto-exit)");
            }
        } else {
            Log.d(TAG, "Adapter: null");
        }

        Log.d(TAG, "FAB Visible: " + (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE));
        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " SELECTION DEBUG ===");

        //super.debugSelectionState();

        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " EVENTS PREVIEW DEBUG ===");
        Log.d(TAG, "Events Preview Enabled: " + mEventsPreviewEnabled);

        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.debugState();
        } else {
            Log.d(TAG, "Events Preview Manager: null");
        }

        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " EVENTS PREVIEW DEBUG ===");
    }


    /**
     * ✅ NEW: Debug quick events system specifically
     */
    public void debugQuickEventsSystem() {
        Log.d(TAG, "=== QUICK EVENTS SYSTEM DEBUG ===");
        Log.d(TAG, "System Initialized: " + mQuickEventsInitialized);
        Log.d(TAG, "Templates Count: " + (mQuickEventTemplates != null ? mQuickEventTemplates.size() : 0));
        Log.d(TAG, "User Context: " + mCurrentUserId);

        if (mQuickEventTemplates != null) {
            Log.d(TAG, "Available Templates:");
            for (ToolbarAction action : mQuickEventTemplates.keySet()) {
                QuickEventTemplate template = mQuickEventTemplates.get(action);
                Log.d(TAG, "  " + action + " -> " + (template != null ? template.getDisplayName() : "NULL"));
            }
        }

        // Test validation for today
        LocalDate today = LocalDate.now();
        Log.d(TAG, "Validation test for today (" + today + "):");
        if (mQuickEventTemplates != null) {
            for (ToolbarAction action : ToolbarAction.getQuickEventActions()) {
                boolean canCreate = canCreateQuickEvent(action, today);
                Log.d(TAG, "  " + action + ": " + (canCreate ? "✅" : "❌"));
            }
        }

        Log.d(TAG, "=== END QUICK EVENTS DEBUG ===");
    }
}