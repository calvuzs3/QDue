package net.calvuz.qdue.ui.shared;

import android.content.Context;
import android.view.View;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.utils.Log;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base adapter with long-click and selection support
 * Intermediate layer between BaseAdapterLegacy and specific implementations
 */
public abstract class BaseClickAdapterLegacy extends BaseAdapterLegacy implements DayLongClickListener {

    private static final String TAG = "BaseClickAdapter";

    // Long-click and selection support
    protected DayLongClickListener mLongClickListener; // Fragment callback
    protected boolean mIsSelectionMode = false;
    protected Set<LocalDate> mSelectedDates = new HashSet<>();
    protected FloatingDayToolbar mFloatingToolbar;

    public BaseClickAdapterLegacy(Context context, List<SharedViewModels.ViewItem> items,
                                  HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);

        // Initialize floating toolbar
        mFloatingToolbar = new FloatingDayToolbar(context);

        Log.d(TAG, "BaseClickAdapterLegacy: ✅ initialized with long-click support");
    }

    /**
     * Set the fragment/activity callback listener
     */
    public void setLongClickListener(DayLongClickListener listener) {
        mLongClickListener = listener;
    }

    /**
     * Setup long-click support for any DayViewHolder
     * Should be called by subclass in bindDay method
     */
    protected void setupLongClickSupport(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        if (dayItem.day == null || !(holder instanceof LongClickCapable)) return;

        LongClickCapable longClickHolder = (LongClickCapable) holder;

        // Bind day data to holder for callbacks
        longClickHolder.bindDayData(dayItem.day, dayItem.day.getLocalDate(), position, this);

        // Update selection mode state
        longClickHolder.setSelectionMode(mIsSelectionMode);

        // Update selection state if this date is selected
        LocalDate date = dayItem.day.getLocalDate();
        boolean isSelected = mSelectedDates.contains(date);
        longClickHolder.setSelected(isSelected);

        Log.v(TAG, "Long-click support setup for date: " + date +
                ", selectionMode: " + mIsSelectionMode +
                ", selected: " + isSelected);
    }

    // ===========================================
    // DayLongClickListener Implementation
    // ===========================================

    @Override
    public void onDayLongClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, "onDayLongClick: " + date);

        // Show floating toolbar
        mFloatingToolbar.show(itemView, day, date, this);

        // Enter selection mode if not already
        if (!mIsSelectionMode) {
            setSelectionMode(true);
        }

        // Select this day
        toggleDaySelection(date, true);

        // Notify fragment/activity
        if (mLongClickListener != null) {
            mLongClickListener.onDayLongClick(day, date, itemView, position);
        }
    }

    @Override
    public void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date) {
        Log.d(TAG, "onToolbarActionSelected: " + action + " for date: " + date);

        // Hide toolbar
        mFloatingToolbar.hide();

        // Handle action
        handleToolbarAction(action, day, date);

        // Notify fragment/activity
        if (mLongClickListener != null) {
            mLongClickListener.onToolbarActionSelected(action, day, date);
        }

        // Exit selection mode after action
        setSelectionMode(false);
    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        Log.d(TAG, "onSelectionModeChanged: " + isSelectionMode + ", count: " + selectedCount);

        // Notify fragment/activity
        if (mLongClickListener != null) {
            mLongClickListener.onSelectionModeChanged(isSelectionMode, selectedCount);
        }
    }

    @Override
    public void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected) {
        Log.d(TAG, "onDaySelectionChanged: " + date + ", selected: " + isSelected);

        // Update internal selection state
        toggleDaySelection(date, isSelected);

        // Notify fragment/activity
        if (mLongClickListener != null) {
            mLongClickListener.onDaySelectionChanged(day, date, isSelected);
        }

        // Note: checkAndExitSelectionIfEmpty() is called by toggleDaySelection()
    }

    // ===========================================
    // Selection Management
    // ===========================================

    /**
     * Set selection mode state
     */
    public void setSelectionMode(boolean isSelectionMode) {
        if (mIsSelectionMode == isSelectionMode) {
            Log.v(TAG, "Selection mode already " + (isSelectionMode ? "enabled" : "disabled"));
            return;
        }

        Log.d(TAG, "Selection mode changing: " + mIsSelectionMode + " -> " + isSelectionMode);

        mIsSelectionMode = isSelectionMode;

        // Clear selections when exiting selection mode
        if (!isSelectionMode) {
            if (!mSelectedDates.isEmpty()) {
                Log.d(TAG, "Clearing " + mSelectedDates.size() + " selections on mode exit");
                mSelectedDates.clear();
            }
            mFloatingToolbar.hide();
        }

        // Update all visible ViewHolders
        notifyDataSetChanged();

        // Notify callback
        onSelectionModeChanged(isSelectionMode, mSelectedDates.size());

        Log.d(TAG, "Selection mode changed to: " + isSelectionMode);
    }

    /**
     * Toggle selection state for a specific date
     */
    protected void toggleDaySelection(LocalDate date, boolean isSelected) {
        boolean wasSelected = mSelectedDates.contains(date);

        if (isSelected) {
            mSelectedDates.add(date);
            Log.d(TAG, "Day selected: " + date + " (total selected: " + mSelectedDates.size() + ")");
        } else {
            mSelectedDates.remove(date);
            Log.d(TAG, "Day deselected: " + date + " (total selected: " + mSelectedDates.size() + ")");
        }

        // Find and update the corresponding ViewHolder
        updateViewHolderSelection(date, isSelected);

        // 🔧 NEW: Auto-exit selection mode if no items selected
        checkAndExitSelectionIfEmpty();
    }

    /**
     * 🔧 NEW: Check if selection is empty and auto-exit if needed
     */
    private void checkAndExitSelectionIfEmpty() {
        if (mIsSelectionMode && mSelectedDates.isEmpty()) {
            Log.d(TAG, "No items selected - auto-exiting selection mode");

            // 🔧 FIX: Use Android's main thread posting instead of mMainHandler
            // Exit selection mode with a small delay to allow for smooth UI transition
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                if (mSelectedDates.isEmpty()) { // Double-check in case something was selected in the meantime
                    setSelectionMode(false);
                    Log.d(TAG, "Auto-exit completed - selection mode disabled");
                }
            }, 500); // Small delay for smooth UX
        }
    }

    /**
     * Update ViewHolder selection state without full refresh
     */
    protected void updateViewHolderSelection(LocalDate date, boolean isSelected) {
        // Find the position of this date in the adapter
        for (int i = 0; i < getItemCount(); i++) {
            SharedViewModels.ViewItem item = mItems.get(i);
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day != null && dayItem.day.getLocalDate().equals(date)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    /**
     * Get currently selected dates
     */
    public Set<LocalDate> getSelectedDates() {
        return new HashSet<>(mSelectedDates);
    }

    /**
     * Check if we're in selection mode
     */
    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    /**
     * Get selected dates count
     */
    public int getSelectedCount() {
        return mSelectedDates.size();
    }

    // ===========================================
    // Abstract Methods for Subclasses
    // ===========================================

    /**
     * Handle toolbar action execution - subclasses should implement
     */
    protected abstract void handleToolbarAction(ToolbarAction action, Day day, LocalDate date);

    // ===========================================
    // Public API Methods
    // ===========================================

    /**
     * Public method to exit selection mode
     */
    public void exitSelectionMode() {
        setSelectionMode(false);
    }

    /**
     * Public method to clear all selections
     */
    public void clearSelections() {
        Log.d(TAG, "Clearing all selections (" + mSelectedDates.size() + " items)");

        mSelectedDates.clear();
        notifyDataSetChanged();

        // Notify selection changed with 0 count
        onSelectionModeChanged(mIsSelectionMode, 0);

        // 🔧 NEW: Auto-exit after clearing
        checkAndExitSelectionIfEmpty();
    }

    /**
     * Public method to select all visible days
     */
    public void selectAllDays() {
        for (SharedViewModels.ViewItem item : mItems) {
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day != null) {
                    mSelectedDates.add(dayItem.day.getLocalDate());
                }
            }
        }
        notifyDataSetChanged();
        onSelectionModeChanged(mIsSelectionMode, mSelectedDates.size());
    }

    /**
     * 🔧 NEW: Manual deselect all with auto-exit
     * This method can be called from UI or programmatically
     */
    public void deselectAll() {
        Log.d(TAG, "Deselecting all items manually");

        if (!mSelectedDates.isEmpty()) {
            clearSelections(); // This will trigger auto-exit
        } else if (mIsSelectionMode) {
            // Already empty but still in selection mode - force exit
            setSelectionMode(false);
        }
    }

    /**
     * Handle back press - exit selection mode if active
     */
    public boolean onBackPressed() {
        if (mIsSelectionMode) {
            setSelectionMode(false);
            return true; // Consumed
        }
        return false; // Not consumed
    }

    /**
     * Cleanup resources when adapter is destroyed
     */
    public void onDestroy() {
        if (mFloatingToolbar != null) {
            mFloatingToolbar.hide();
        }
        mSelectedDates.clear();
        mLongClickListener = null;
    }

    // ===========================================
    // Interface for ViewHolder Long-Click Support
    // ===========================================

    /**
     * Interface that ViewHolders should implement to support long-click
     */
    public interface LongClickCapable {
        void bindDayData(Day day, LocalDate date, int position, DayLongClickListener listener);
        void setSelectionMode(boolean isSelectionMode);
        void setSelected(boolean isSelected);
        boolean isSelected();
    }
}