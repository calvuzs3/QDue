package net.calvuz.qdue.ui.shared;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
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
     * Enhanced MaterialDayViewHolder with regular click support
     * Aggiornare questa classe in DaysListAdapterLegacy.java
     */
    public class BaseMaterialDayViewHolder extends DayViewHolder implements BaseClickAdapterLegacy.LongClickCapable {
        final String mTAG = "BaseMaterialDayViewHolder: ";

        // Existing fields
        public TextView eventsIndicator;

        // Selection support fields
        private boolean mIsSelectionMode = false;
        private boolean mIsSelected = false;
        private DayLongClickListener mLongClickListener;

        // 🔧 NEW: Regular click support
        private DayRegularClickListener mRegularClickListener;

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

            Log.v(TAG, mTAG + "initialized");
        }

        // ===========================================
        // Enhanced LongClick Listener Setup
        // ===========================================

        /**
         * Setup long click listener for toolbar activation
         */
        private void setupLongClickListener() {
            itemView.setOnLongClickListener(v -> {
                Log.d(TAG, mTAG + "Long click detected!");

                if (mLongClickListener != null && mCurrentDay != null) {
                    // Provide haptic feedback
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

                    // Trigger callback
                    mLongClickListener.onDayLongClick(mCurrentDay, mCurrentDate, itemView, mCurrentPosition);

                    Log.d(TAG, mTAG + "Long click callback triggered for date: " + mCurrentDate);
                    return true;
                } else {
                    Log.w(TAG, mTAG + "Long click ignored - listener: " +
                            (mLongClickListener != null ? "OK" : "NULL") +
                            ", day: " + (mCurrentDay != null ? "OK" : "NULL"));
                }
                return false;
            });

            Log.d(TAG, mTAG + "Long click listener setup completed");
        }

        // ===========================================
        // Enhanced Click Listener Setup
        // ===========================================

        /**
         * Enhanced setup regular click listener for both selection and normal mode
         */
        private void setupClickListener() {
            itemView.setOnClickListener(v -> {
                Log.d(TAG, mTAG + "Click detected - selection mode: " + mIsSelectionMode);

                if (mIsSelectionMode) {
                    // Selection mode: toggle selection
                    handleSelectionModeClick();
                } else {
                    // Normal mode: show events preview or handle as regular click
                    handleRegularModeClick();
                }
            });

            Log.d(TAG, mTAG + "Enhanced click listener setup completed");
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

                Log.d(TAG, mTAG + "Selection toggled for date: " + mCurrentDate + ", selected: " + mIsSelected);
            } else {
                Log.w(TAG, mTAG + "Selection mode click ignored - missing data or listener");
            }
        }

        /**
         * Handle click in normal mode (events preview)
         */
        private void handleRegularModeClick() {
            if (mRegularClickListener != null && mCurrentDay != null) {
                // Trigger regular click callback for events preview
                mRegularClickListener.onDayRegularClick(mCurrentDay, mCurrentDate, itemView, mCurrentPosition);

                Log.d(TAG, mTAG + "Regular click triggered for date: " + mCurrentDate);
            } else {
                Log.v(TAG, mTAG + "Regular click ignored - listener: " +
                        (mRegularClickListener != null ? "OK" : "NULL") +
                        ", day: " + (mCurrentDay != null ? "OK" : "NULL"));
            }
        }

        // ===========================================
        // LongClickCapable Interface Implementation
        // ===========================================

        @Override
        public void bindDayData(Day day, LocalDate date, int position, DayLongClickListener listener) {
            Log.d(TAG, mTAG + "bindDayData called for date: " + date);

            // Store day data for callbacks
            mCurrentDay = day;
            mCurrentDate = date;
            mCurrentPosition = position;
            mLongClickListener = listener;

            // Debug verification
            Log.d(TAG, mTAG + "Data bound - Day: " + (day != null ? "OK" : "NULL") +
                    ", Listener: " + (listener != null ? "OK" : "NULL") +
                    ", Date: " + date);

            // Update visual state
            updateSelectionVisual();
        }

        @Override
        public void setRegularClickListener(DayRegularClickListener listener) {
            mRegularClickListener = listener;
            Log.d(TAG, mTAG + "Regular click listener set: " + (listener != null ? "OK" : "NULL"));
        }

        @Override
        public void setSelectionMode(boolean isSelectionMode) {
            Log.d(TAG, mTAG + "setSelectionMode: " + isSelectionMode + " (was: " + mIsSelectionMode + ")");

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
            Log.d(TAG, mTAG + "setSelected: " + isSelected + " (was: " + mIsSelected + ")");

            mIsSelected = isSelected;

            // Update MaterialCardView checked state
            if (itemView instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) itemView;
                cardView.setChecked(isSelected);
                Log.d(TAG, mTAG + "MaterialCardView.setChecked(" + isSelected + ")");
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
            if (itemView instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) itemView;

                Log.v(TAG, mTAG + "updateSelectionVisual - SelectionMode: " + mIsSelectionMode +
                        ", Selected: " + mIsSelected);

                // Enable/disable checkable state based on selection mode
                cardView.setCheckable(mIsSelectionMode);
                cardView.setChecked(mIsSelected);

                // Visual feedback based on mode
                if (mIsSelectionMode) {
                    // Selection mode: elevation and selection styling
                    cardView.setCardElevation(mIsSelected ? 8f : 4f);

                    // Optional: Add selection mode visual cues
                    //updateSelectionModeVisuals(cardView);

                    Log.v(TAG, mTAG + "Selection mode elevation set: " + (mIsSelected ? "8f" : "4f"));
                } else {
                    // Normal mode: standard elevation with subtle hover effect
                    cardView.setCardElevation(2f);

                    // Optional: Add clickable visual cues for events preview
                    //updateNormalModeVisuals(cardView);

                    Log.v(TAG, mTAG + "Normal mode elevation set: 2f");
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