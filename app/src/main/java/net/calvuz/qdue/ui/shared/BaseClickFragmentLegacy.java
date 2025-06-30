package net.calvuz.qdue.ui.shared;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.utils.Log;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base fragment with long-click and selection support
 * Intermediate layer between BaseFragmentLegacy and specific implementations
 */
public abstract class BaseClickFragmentLegacy extends BaseFragmentLegacy implements DayLongClickListener {

    private static final String TAG = "BaseClickFragment";

    // Selection mode support
    protected boolean mIsSelectionMode = false;
    protected boolean previousMode = false;

    protected MenuItem mSelectAllMenuItem;
    protected MenuItem mClearSelectionMenuItem;
    protected MenuItem mExitSelectionMenuItem;

    // Abstract methods for subclasses to implement
    protected abstract BaseClickAdapterLegacy getClickAdapter();

    protected abstract String getFragmentName();

    // ===========================================
    // DayLongClickListener Implementation
    // ===========================================

    @Override
    public void onDayLongClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, getFragmentName() + ": Day long-clicked: " + date);

        // Show selection mode UI if needed
        if (!mIsSelectionMode) {
            enterSelectionMode();
        }

        // Update FAB or other UI elements based on selection
        updateSelectionUI();

        // Provide haptic feedback
        if (itemView != null) {
            itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    @Override
    public void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date) {
        Log.d(TAG, getFragmentName() + ": Toolbar action selected: " + action + " for date: " + date);

        switch (action) {
            case ADD_EVENT:
                openEventEditor(date);
                break;

            case VIEW_EVENTS:
                showEventsDialog(date);
                break;

            case FERIE:
            case MALATTIA:
            case LEGGE_104:
            case PERMESSO:
                // These are handled by adapter, but we can show confirmation
                showQuickEventConfirmation(action, date);
                break;

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

        // 🔧 FIX: Store previous mode before updating
        boolean previousMode = mIsSelectionMode;
        mIsSelectionMode = isSelectionMode;

        if (isSelectionMode && !previousMode) {
            // Entering selection mode
            Log.d(TAG, getFragmentName() + ": Entering selection mode");
            enterSelectionMode();
        } else if (!isSelectionMode && previousMode) {
            // Exiting selection mode
            Log.d(TAG, getFragmentName() + ": Exiting selection mode");
            exitSelectionMode();
        }

        updateSelectionUI();
        updateActionBarTitle(selectedCount);

        // 🔧 FIX: Special handling for auto-exit (now previousMode is correctly defined)
        if (!isSelectionMode && previousMode && selectedCount == 0) {
            Log.d(TAG, getFragmentName() + ": Auto-exit from selection mode detected (no items selected)");
            // Additional cleanup or notifications can be added here
            onAutoExitSelectionMode();
        }
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

    @Override
    public void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected) {
        Log.d(TAG, getFragmentName() + ": Day selection changed: " + date + " -> " + isSelected);

        // Update UI based on selection changes
        updateSelectionUI();

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            int selectedCount = adapter.getSelectedCount();
            updateActionBarTitle(selectedCount);

            // 🔧 NEW: Log selection count for debugging
            Log.d(TAG, getFragmentName() + ": Current selection count: " + selectedCount);
        }
    }

    // ===========================================
    // Selection Mode UI Management
    // ===========================================

    /**
     * Enter selection mode - update UI accordingly
     */
    protected void enterSelectionMode() {
        if (mIsSelectionMode) return;

        mIsSelectionMode = true;

        // Update action bar/toolbar if available
        updateActionBarForSelection(true);

        // Hide FAB during selection
        hideFabForSelection();

        // Enable selection mode in adapter
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null && !adapter.isSelectionMode()) {
            adapter.setSelectionMode(true);
        }

        Log.d(TAG, getFragmentName() + ": Entered selection mode");
    }

    /**
     * Exit selection mode - restore normal UI
     */
    protected void exitSelectionMode() {
        if (!mIsSelectionMode) return;

        mIsSelectionMode = false;

        // Clear adapter selection
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            adapter.exitSelectionMode();
        }

        // Update action bar/toolbar
        updateActionBarForSelection(false);

        // Show FAB again
        showFabAfterSelection();

        Log.d(TAG, getFragmentName() + ": Exited selection mode");
    }

    /**
     * Hide FAB during selection mode
     */
    protected void hideFabForSelection() {
        if (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(200)
                    .withEndAction(() -> mFabGoToToday.setVisibility(View.GONE))
                    .start();
        }
    }

    /**
     * Show FAB after exiting selection mode
     */
    protected void showFabAfterSelection() {
        if (mFabGoToToday != null) {
            mFabGoToToday.setVisibility(View.VISIBLE);
            mFabGoToToday.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }

    /**
     * Update action bar for selection mode
     */
    protected void updateActionBarForSelection(boolean isSelectionMode) {
        if (getActivity() instanceof QDueMainActivity) {
            QDueMainActivity mainActivity = (QDueMainActivity) getActivity();

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
     * Setup action bar for selection mode
     */
    protected void setupSelectionActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific selection action bar setup
        // For now, just update the title
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            updateActionBarTitle(adapter.getSelectedCount());
        }
    }

    /**
     * Restore normal action bar
     */
    protected void restoreNormalActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific action bar restoration
        // For now, just reset the title
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(getFragmentName());
        }
    }

    /**
     * Update action bar title with selection count
     */
    protected void updateActionBarTitle(int selectedCount) {
        if (mIsSelectionMode && getActivity() != null) {
            String title = selectedCount > 0 ?
                    String.format(QDue.getLocale(), "%d giorni selezionati", selectedCount) :
                    "Seleziona giorni";

            // Update action bar title
            if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(title);
            }

            Log.d(TAG, "Updated action bar title: " + title);
        }
    }

    /**
     * Update UI elements based on current selection state
     */
    protected void updateSelectionUI() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        boolean hasSelection = adapter != null && adapter.getSelectedCount() > 0;

        Log.v(TAG, getFragmentName() + ": updateSelectionUI - hasSelection: " + hasSelection +
                ", selectionMode: " + mIsSelectionMode);

        // Enable/disable menu items based on selection
        if (mSelectAllMenuItem != null) {
            mSelectAllMenuItem.setEnabled(mIsSelectionMode);
        }

        if (mClearSelectionMenuItem != null) {
            mClearSelectionMenuItem.setEnabled(mIsSelectionMode && hasSelection);
        }

        if (mExitSelectionMenuItem != null) {
            mExitSelectionMenuItem.setEnabled(mIsSelectionMode);
        }

        // Update other UI elements as needed
        updateSelectionDependentUI(hasSelection);

        // 🔧 NEW: Update action bar title when selection changes
        if (adapter != null) {
            updateActionBarTitle(adapter.getSelectedCount());
        }
    }

    /**
     * Update UI elements that depend on selection state
     * Subclasses can override for specific UI updates
     */
    protected void updateSelectionDependentUI(boolean hasSelection) {
        // Default implementation - subclasses can override
    }


// ===========================================
// 🔧 NEW: Additional Helper Methods
// ===========================================

    /**
     * Check if currently in selection mode with items selected
     */
    public boolean hasActiveSelection() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return mIsSelectionMode && adapter != null && adapter.getSelectedCount() > 0;
    }

    /**
     * Get current selection count safely
     */
    public int getCurrentSelectionCount() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }


    // ===========================================
    // Action Handling
    // ===========================================

    /**
     * Open event editor for the specified date
     */
    protected void openEventEditor(LocalDate date) {
        Log.d(TAG, "Opening event editor for date: " + date);

        // TODO: Navigate to event editor with pre-filled date
        // Example using Navigation Component or Intent

        // For now, show a toast and delegate to subclass
        showEventEditorPlaceholder(date);
        onOpenEventEditor(date);
    }

    /**
     * Show events dialog for the specified date
     */
    protected void showEventsDialog(LocalDate date) {
        Log.d(TAG, "Showing events dialog for date: " + date);

        // Get events for this date
        Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
        List<LocalEvent> events = eventsCache.get(date);

        if (events == null || events.isEmpty()) {
            // Show "no events" message
            showNoEventsMessage(date);
        } else {
            // Show events list dialog
            showEventsListDialog(date, events);
        }

        // Delegate to subclass for specific handling
        onShowEventsDialog(date, events);
    }

    /**
     * Show confirmation for quick event creation
     */
    protected void showQuickEventConfirmation(ToolbarAction action, LocalDate date) {
        if (getContext() == null) return;

        String eventType = getEventTypeName(action);
        String message = String.format(QDue.getLocale(),
                "Evento '%s' creato per %s", eventType, date);

        android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Quick event confirmation shown: " + eventType + " for " + date);

        // Delegate to subclass
        onQuickEventCreated(action, date);
    }

    /**
     * Get user-friendly event type name
     */
    protected String getEventTypeName(ToolbarAction action) {
        switch (action) {
            case FERIE: return "Ferie";
            case MALATTIA: return "Malattia";
            case LEGGE_104: return "Legge 104";
            case PERMESSO: return "Permesso";
            default: return action.name();
        }
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
    // Abstract/Virtual Methods for Subclasses
    // ===========================================

    /**
     * Called when event editor should be opened
     * Subclasses should override for specific navigation
     */
    protected void onOpenEventEditor(LocalDate date) {
        // Default implementation - subclasses can override
    }

    /**
     * Called when events dialog should be shown
     * Subclasses should override for specific dialog implementation
     */
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        // Default implementation - subclasses can override
    }

    /**
     * Called when a quick event is created
     * Subclasses should override for specific handling
     */
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        // Default implementation - subclasses can override
    }

    // ===========================================
    // Back Press Handling
    // ===========================================

    /**
     * 🔧 NEW: Enhanced back press handling with auto-exit awareness
     */
    public boolean onBackPressed() {
        if (mIsSelectionMode) {
            Log.d(TAG, getFragmentName() + ": Back press in selection mode");

            BaseClickAdapterLegacy adapter = getClickAdapter();
            if (adapter != null && adapter.getSelectedCount() > 0) {
                // Has selections - clear them (this will auto-exit)
                adapter.deselectAll();
                Log.d(TAG, getFragmentName() + ": Cleared selections on back press");
            } else {
                // No selections - just exit mode
                exitSelectionMode();
                Log.d(TAG, getFragmentName() + ": Exited selection mode on back press");
            }
            return true; // Consumed
        }

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            return adapter.onBackPressed();
        }

        return false; // Not consumed
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
            adapter.selectAllDays();
            return true;
        } else if (itemId == R.id.action_clear_selection) {
            adapter.clearSelections();
            return true;
        } else if (itemId == R.id.action_exit_selection) {
            exitSelectionMode();
            return true;
        }
        */

        return false;
    }

    // ===========================================
    // Lifecycle
    // ===========================================

    @Override
    public void onDestroy() {
        super.onDestroy();

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            adapter.onDestroy();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup long-click listener in adapter if available
        setupAdapterLongClickListener();
    }

    /**
     * Setup long-click listener in adapter
     */
    protected void setupAdapterLongClickListener() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            adapter.setLongClickListener(this);
            Log.d(TAG, getFragmentName() + ": Long-click listener setup in adapter");
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
     * Public method to check if in selection mode
     */
    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    /**
     * Public method to get selected count
     */
    public int getSelectedCount() {
        BaseClickAdapterLegacy adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }


    /**
     * 🔧 NEW: Force exit selection mode (public method for external calls)
     */
    public void forceExitSelectionMode() {
        Log.d(TAG, getFragmentName() + ": Force exit selection mode requested");

        if (mIsSelectionMode) {
            BaseClickAdapterLegacy adapter = getClickAdapter();
            if (adapter != null) {
                adapter.deselectAll(); // This will trigger auto-exit
            } else {
                // Fallback: exit directly
                exitSelectionMode();
            }
        }
    }


    // ===========================================
    // Debug Methods
    // ===========================================

    /**
     * Debug selection state
     */
    public void debugSelectionState() {
        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " SELECTION STATE DEBUG ===");
        Log.d(TAG, "Fragment Selection Mode: " + mIsSelectionMode);

        BaseClickAdapterLegacy adapter = getClickAdapter();
        if (adapter != null) {
            Log.d(TAG, "Adapter Selection Mode: " + adapter.isSelectionMode());
            Log.d(TAG, "Selected Count: " + adapter.getSelectedCount());
            Log.d(TAG, "Selected Dates: " + adapter.getSelectedDates());
            Log.d(TAG, "Has Active Selection: " + hasActiveSelection());

            // 🔧 NEW: Check for inconsistencies
            if (mIsSelectionMode != adapter.isSelectionMode()) {
                Log.w(TAG, "⚠️ INCONSISTENCY: Fragment and Adapter selection mode mismatch!");
            }

            if (mIsSelectionMode && adapter.getSelectedCount() == 0) {
                Log.w(TAG, "⚠️ WARNING: In selection mode but no items selected (should auto-exit)");
            }
        } else {
            Log.d(TAG, "Adapter: null");
        }

        Log.d(TAG, "FAB Visible: " + (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE));
        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " SELECTION DEBUG ===");
    }

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
}