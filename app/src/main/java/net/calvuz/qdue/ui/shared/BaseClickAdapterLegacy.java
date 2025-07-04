package net.calvuz.qdue.ui.shared;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.shared.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
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

    // NEW: Regular click support
    protected DayRegularClickListener mRegularClickListener;

    // Long-click and selection support
    protected DayLongClickListener mLongClickListener; // Fragment callback

    // Selection Mode
    protected boolean mIsSelectionMode = false;
    protected Set<LocalDate> mSelectedDates = new HashSet<>();

    //protected FloatingDayToolbar mFloatingToolbar;

    /**
     * Handle toolbar action execution - subclasses should implement
     *
     * @param action The action to be executed
     * @param day    The day associated with the action
     * @param date   The date associated with the action
     */
    protected abstract void handleToolbarAction(ToolbarAction action, Day day, LocalDate date);

    public BaseClickAdapterLegacy(Context context, List<SharedViewModels.ViewItem> items,
                                  HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);

        // Initialize floating toolbar
        //mFloatingToolbar = new FloatingDayToolbar(context);

        Log.d(TAG, "BaseClickAdapterLegacy: ✅ initialized with long-click support");
    }

    /**
     * Set the regular click listener for events preview
     */
    public void setRegularClickListener(DayRegularClickListener listener) {
        mRegularClickListener = listener;
        Log.d(TAG, "Regular click listener set: " + (listener != null ? "active" : "null"));
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

        // NEW: Setup regular click listener
        longClickHolder.setRegularClickListener(mRegularClickListener);

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
//        mFloatingToolbar.show(itemView, day, date, this);

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

        // Provide haptic feedback
        if (itemView != null) {
            itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    @Override
    public void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date) {
        Log.d(TAG, "onToolbarActionSelected: " + action + " for date: " + date);

        // Forward to fragment listener
        if (mLongClickListener != null) {
            mLongClickListener.onToolbarActionSelected(action, day, date);
        }

        // Action is handled by fragment, selection mode will be exited there
    }
//        // Hide toolbar
//        mFloatingToolbar.hide();
//
//        // Handle action
//        handleToolbarAction(action, day, date);
//
//        // Notify fragment/activity
//        if (mLongClickListener != null) {
//            mLongClickListener.onToolbarActionSelected(action, day, date);
//        }
//
//        // Exit selection mode after action
//        setSelectionMode(false);
//    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        Log.d(TAG, "onSelectionModeChanged: " + isSelectionMode + ", count: " + selectedCount);

        mIsSelectionMode = isSelectionMode;

        // If exiting selection mode, clear selections
        if (!isSelectionMode) {
            clearSelections();
        }

        // Notify fragment
        if (mLongClickListener != null) {
            mLongClickListener.onSelectionModeChanged(isSelectionMode, selectedCount);
        }

        // Refresh UI
//        notifyDataSetChanged();
    }

    @Override
    public void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected) {
        Log.d(TAG, "onDaySelectionChanged: " + date + ", selected: " + isSelected);

        // Update internal selection state
        toggleDaySelection(date, isSelected); // try

        // Forward to fragment listener
        if (mLongClickListener != null) {
            mLongClickListener.onDaySelectionChanged(day, date, isSelected);
        }
        // Note: checkAndExitSelectionIfEmpty() is called by toggleDaySelection()
    }

    // ===========================================
    // Selection Management
    // ===========================================

    /**
     * Toggle selection for a specific date
     */
    public void toggleDateSelection(LocalDate date) {
        if (mSelectedDates.contains(date)) {
            mSelectedDates.remove(date);
        } else {
            mSelectedDates.add(date);
        }

        // Get day object for callback
        Day day = findDayForDate(date);

        // Notify about selection change
        boolean isSelected = mSelectedDates.contains(date);
        onDaySelectionChanged(day, date, isSelected);

        // Update UI for this specific item
        updateItemSelection(date, isSelected);

        Log.d(TAG, "Date " + date + " selection toggled to: " + isSelected +
                ", total selected: " + mSelectedDates.size());
    }

    /**
     * Set selection mode state
     */
    public void setSelectionMode(boolean isSelectionMode) {
        if (mIsSelectionMode == isSelectionMode) {
            Log.v(TAG, "Selection mode already " + (isSelectionMode ? "enabled" : "disabled"));
            return;
        }

        Log.d(TAG, "Selection mode changing: " + mIsSelectionMode + " -> " + isSelectionMode);

        boolean wasSelectionMode = mIsSelectionMode;
        mIsSelectionMode = isSelectionMode;

        // If exiting selection mode, clear all selections
        if (!isSelectionMode && wasSelectionMode) {
            clearSelections();
        }

        // Notify about mode change
        onSelectionModeChanged(isSelectionMode, mSelectedDates.size());

        // Update all visible ViewHolders
        notifyDataSetChanged();

        Log.d(TAG, "Selection mode changed to: " + isSelectionMode);
    }

    /**
     * Clear all selections
     */
    public void clearSelections() {
        if (mSelectedDates.isEmpty()) return;

        Log.d(TAG, "Clearing " + mSelectedDates.size() + " selections");

        // Notify about each deselection
        for (LocalDate date : mSelectedDates) {
            Day day = findDayForDate(date);
            onDaySelectionChanged(day, date, false);
        }

        mSelectedDates.clear();

        // Update UI
        notifyDataSetChanged();

        // Auto-exit selection mode if no items selected
        if (mIsSelectionMode && mSelectedDates.isEmpty()) {
            setSelectionMode(false);
        }
    }
//    /**
//     * Public method to clear all selections
//     */
//    public void clearSelections() {
//        Log.d(TAG, "Clearing all selections (" + mSelectedDates.size() + " items)");
//
//        mSelectedDates.clear();
//        notifyDataSetChanged();
//
//        // Notify selection changed with 0 count
//        onSelectionModeChanged(mIsSelectionMode, 0);
//
//        // 🔧 NEW: Auto-exit after clearing
//        checkAndExitSelectionIfEmpty();
//    }

    /**
     * Select all available days
     */
    public void selectAllDays() {
        if (mItems == null) return;

        Log.d(TAG, "Selecting all days");

        // Add all day dates to selection
        for (SharedViewModels.ViewItem item : mItems) {
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day != null) {
                    LocalDate date = dayItem.day.getLocalDate();
                    if (!mSelectedDates.contains(date)) {
                        mSelectedDates.add(date);
                        onDaySelectionChanged(dayItem.day, date, true);
                    }
                }
            }
        }

        // Update UI
        notifyDataSetChanged();

        Log.d(TAG, "Selected " + mSelectedDates.size() + " days");
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
     * Update UI for a specific item's selection state
     */
    private void updateItemSelection(LocalDate date, boolean isSelected) {
        // Find the position of this date in the adapter
        int position = findPositionForDate(date);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    /**
     * Find position in adapter for a specific date
     */
    public int findPositionForDate(LocalDate date) {
        if (mItems == null) return -1;

        for (int i = 0; i < mItems.size(); i++) {
            SharedViewModels.ViewItem item = mItems.get(i);
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day != null && dayItem.day.getLocalDate().equals(date)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Find Day object for a specific date
     */
    private Day findDayForDate(LocalDate date) {
        if (mItems == null) return null;

        for (SharedViewModels.ViewItem item : mItems) {
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day != null && dayItem.day.getLocalDate().equals(date)) {
                    return dayItem.day;
                }
            }
        }
        return null;
    }

    // ===========================================
    // Public API Methods
    // ===========================================

    /**
     * Get currently selected dates
     */
    public Set<LocalDate> getSelectedDates() {
        return new HashSet<>(mSelectedDates);
    }

    /**
     * Get count of selected dates
     */
    public int getSelectedCount() {
        return mSelectedDates.size();
    }

    /**
     * Check if we're in selection mode
     */
    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    /**
     * Check if a specific date is selected
     */
    public boolean isDateSelected(LocalDate date) {
        return mSelectedDates.contains(date);
    }

    // ===========================================
    // Back Press Handling
    // ===========================================

    /**
     * Handle back press - exit selection mode if active
     */
    public boolean onBackPressed() {
        if (mIsSelectionMode) {
            Log.d(TAG, "Back press in selection mode - exiting");
            setSelectionMode(false);
            return true; // Consumed
        }
        return false; // Not consumed
    }

    // ===========================================
    // Lifecycle and Cleanup
    // ===========================================

    /**
     * Cleanup resources
     */
    public void onDestroy() {
        // Clear selections
        mSelectedDates.clear();

        // Clear listener
        mLongClickListener = null;

        // FloatingDayToolbar cleanup removed since it's no longer used
        // if (mFloatingToolbar != null) {
        //     mFloatingToolbar.hide();
        //     mFloatingToolbar = null;
        // }

        Log.d(TAG, "BaseClickAdapter destroyed");
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
            Log.d(TAG, "checkAndExitSelectionIfEmpty: if reaches 0, auto-exiting selection mode with delay of 800");

            // 🔧 FIX: Use Android's main thread posting instead of mMainHandler
            // Exit selection mode with a small delay to allow for smooth UI transition
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                if (mIsSelectionMode && mSelectedDates.isEmpty()) { // Double-check in case something was selected in the meantime
                    setSelectionMode(false);

                    // Forward to fragment listener
                    if (mLongClickListener != null) {
                        mLongClickListener.onSelectionModeChanged(false, 0);
                    }

                    Log.d(TAG, "checkAndExitSelectionIfEmpty: Auto-exit completed");
                } else {
                    Log.d(TAG, "checkAndExitSelectionIfEmpty: Auto-exit NOT completed");
                }
            }, 800); // Small delay for smooth UX
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

        // 🔧 NEW: Regular click support for events preview
        void setRegularClickListener(DayRegularClickListener listener);
    }

    /**
     * Interface for handling regular clicks (non-selection mode)
     */
    public interface DayRegularClickListener {
        /**
         * Called when a day is clicked in normal mode (not selection mode)
         */
        void onDayRegularClick(Day day, LocalDate date, View itemView, int position);
    }

    // ===========================================
    // ViewHolder Click and Long-Click Support
    // ===========================================

    /**
     * Enhanced DayslistDayViewHolder with regular click support
     * Aggiornare questa classe in DaysListAdapterLegacy.java
     */
    public class BaseMaterialDayViewHolder extends DayViewHolder implements BaseClickAdapterLegacy.LongClickCapable {
        // Existing fields
        public TextView eventsIndicator;

        // 🔧 NEW: Regular click support (calendar view  overrides it)
        private DayRegularClickListener mRegularClickListener;

        // Selection support fields
        private boolean mIsSelectionMode = false;
        private boolean mIsSelected = false;
        private DayLongClickListener mLongClickListener;

        // View holder data
        private Day mCurrentDay;
        private LocalDate mCurrentDate;
        private int mCurrentPosition;

        public BaseMaterialDayViewHolder(@NonNull MaterialCardView itemView) {
            super(itemView);

            // Initialize existing components
            eventsIndicator = itemView.findViewById(R.id.tv_events_indicator);
            if (eventsIndicator != null) {
                eventsIndicator.setVisibility(View.GONE);
            }

            // Setup listeners immediately in constructor
            setupLongClickListener();
            setupClickListener(); // Enhanced to handle both modes
        }

        // ===========================================
        // Click and  LongClick Listener Setup
        // ===========================================

        /**
         * Enhanced setup regular click listener for both selection and normal mode
         */
        private void setupClickListener() {
            itemView.setOnClickListener(v -> {
                Log.d(TAG, "Click detected - selection mode: " + mIsSelectionMode);

                if (mIsSelectionMode) {
                    // Selection mode: toggle selection
                    handleSelectionModeClick();
                } else {
                    // Normal mode: show events preview
                    handleRegularModeClick();
                }
            });
        }

        /**
         * Setup long click listener for toolbar activation
         */
        private void setupLongClickListener() {
            itemView.setOnLongClickListener(v -> {
                Log.d(TAG, "Long click detected!");

                if (mLongClickListener != null && mCurrentDay != null) {
                    // Provide haptic feedback
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

                    // Trigger callback
                    mLongClickListener.onDayLongClick(mCurrentDay, mCurrentDate, itemView, mCurrentPosition);

                    Log.d(TAG, "Long click callback triggered for date: " + mCurrentDate);
                    return true;
                } else {
                    Log.w(TAG, "Long click ignored - listener: " +
                            (mLongClickListener != null ? "OK" : "NULL") +
                            ", day: " + (mCurrentDay != null ? "OK" : "NULL"));
                }
                return false;
            });
        }

        /**
         * Handle click in selection mode
         */
        private void handleSelectionModeClick() {
            if (mLongClickListener != null && mCurrentDay != null) {
                // Toggle selection
                setSelected(!mIsSelected);

                // Notify listener
                mLongClickListener.onDaySelectionChanged(mCurrentDay, mCurrentDate, mIsSelected);

                Log.d(TAG, "Selection toggled for date: " + mCurrentDate + ", selected: " + mIsSelected);
            } else {
                Log.w(TAG, "Selection mode click ignored - missing data or listener");
            }
        }

        /**
         * Handle click in normal mode (events preview)
         */
        private void handleRegularModeClick() {
            if (mRegularClickListener != null && mCurrentDay != null) {
                // Trigger regular click callback for events preview
                mRegularClickListener.onDayRegularClick(mCurrentDay, mCurrentDate, itemView, mCurrentPosition);

                Log.d(TAG, "Regular click triggered for date: " + mCurrentDate);
            } else {
                Log.v(TAG, "Regular click ignored - listener: " +
                        (mRegularClickListener != null ? "OK" : "NULL") +
                        ", day: " + (mCurrentDay != null ? "OK" : "NULL"));
            }
        }

        // ===========================================
        // LongClickCapable Interface Implementation
        // ===========================================

        @Override
        public void bindDayData(Day day, LocalDate date, int position, DayLongClickListener listener) {
            Log.d(TAG, "bindDayData called for date: " + date);

            // Store day data for callbacks
            mCurrentDay = day;
            mCurrentDate = date;
            mCurrentPosition = position;
            mLongClickListener = listener;

            // Update visual state
            updateSelectionVisual();
        }

        @Override
        public void setRegularClickListener(DayRegularClickListener listener) {
            mRegularClickListener = listener;
        }

        @Override
        public void setSelectionMode(boolean isSelectionMode) {
            mIsSelectionMode = isSelectionMode;

            // Update visual state
            updateSelectionVisual();

            // Reset selection if exiting selection mode
            if (!isSelectionMode) {
                setSelected(false);
            }
        }

        @Override
        public void setSelected(boolean isSelected) {
            mIsSelected = isSelected;

            // Update MaterialCardView checked state
            if (itemView instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) itemView;
                cardView.setChecked(isSelected);
            }

            updateSelectionVisual();
        }

        @Override
        public boolean isSelected() {
            return mIsSelected;
        }

        // ===========================================
        // Visual State Management (Enhanced)
        // ===========================================

        /**
         * Enhanced update UI based on selection state and click mode
         */
        private void updateSelectionVisual() {
            if (itemView instanceof MaterialCardView cardView) {

                // Enable/disable checkable state based on selection mode
                cardView.setCheckable(mIsSelectionMode);
                cardView.setChecked(mIsSelected);

                // Visual feedback based on mode
                if (mIsSelectionMode) {
                    // Selection mode: elevation and selection styling
                    cardView.setCardElevation(mIsSelected ? 8f : 4f);

                    // Optional: Add selection mode visual cues
                    //updateSelectionModeVisuals(cardView);
                } else {
                    // Normal mode: standard elevation with subtle hover effect
                    cardView.setCardElevation(2f);

                    // Optional: Add clickable visual cues for events preview
                    //updateNormalModeVisuals(cardView);
                }
            }
        }

        /**
         * 🔧 DEBUG: Metodo per verificare lo stato interno
         */
        public void debugState() {
            Log.d(TAG, "=== VIEWHOLDER DEBUG STATE ===");
            Log.d(TAG, "Current Day: " + (mCurrentDay != null ? mCurrentDay.getLocalDate() : "NULL"));
            Log.d(TAG, "Current Date: " + mCurrentDate);
            Log.d(TAG, "Current Position: " + mCurrentPosition);
            Log.d(TAG, "Listener: " + (mLongClickListener != null ? "OK" : "NULL"));
            Log.d(TAG, "Selection Mode: " + mIsSelectionMode);
            Log.d(TAG, "Is Selected: " + mIsSelected);
            Log.d(TAG, "ItemView: " + itemView.getClass().getSimpleName());

            if (itemView instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) itemView;
                Log.d(TAG, "CardView Checkable: " + cardView.isCheckable());
                Log.d(TAG, "CardView Checked: " + cardView.isChecked());
                Log.d(TAG, "CardView Elevation: " + cardView.getCardElevation());
            }

            // Test listener presence
            Log.d(TAG, "Has OnLongClickListener: " + (itemView.hasOnClickListeners()));
            Log.d(TAG, "=== END VIEWHOLDER DEBUG ===");
        }

    }
}