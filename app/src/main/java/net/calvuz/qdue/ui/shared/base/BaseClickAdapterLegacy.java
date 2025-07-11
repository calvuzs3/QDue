package net.calvuz.qdue.ui.shared.base;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.QDue;
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
public abstract class BaseClickAdapterLegacy extends BaseAdapterLegacy implements
        DayLongClickListener,
        BaseClickFragmentLegacy.Refreshable {

    // TAG
    private static final String TAG = "BaseClickAdapter";

    // Auto exit selection mode delay ( < animation)
    public static final int AUTO_EXIT_SELECTION_MODE_DELAY = 800;

    // NEW: Regular click support
    protected DayRegularClickListener mRegularClickListener;

    // Long-click and selection support
    protected DayLongClickListener mLongClickListener; // Fragment callback

    // Selection Mode
    protected boolean mIsSelectionMode = false;
    protected Set<LocalDate> mSelectedDates = new HashSet<>();

    // ✅ NEW: Loop prevention flag
    private boolean mUpdatingSelectionInternally = false;

    // ✅ NEW: Handler per auto-exit con cleanup
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Runnable mAutoExitRunnable;

    /**
     * ✅ NEW: Multiple selection enabled flag
     * true = multiple selection (default behavior)
     * false = single selection (max 1 item)
     */
    private boolean mMultipleSelectionEnabled = false;

    /**
     * ✅ NEW: Currently selected item in single selection mode
     * Used for quick access and smooth transitions
     */
    private LocalDate mSingleSelectedDate = null;

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
        // TODO: find the correct way to implement RegularClickListener
        longClickHolder.setRegularClickListener(mRegularClickListener);

        // Update selection mode state
        longClickHolder.setSelectionMode(mIsSelectionMode);

        // Update selection mode state
        longClickHolder.setSelectionMode(mIsSelectionMode);

        // Update selection state if this date is selected
        LocalDate date = dayItem.day.getLocalDate();
        boolean isSelected = mSelectedDates.contains(date);
        longClickHolder.setSelected(isSelected);

        Log.v(TAG, "setupLongClickSupport: " + date +
                " selectionMode=" + mIsSelectionMode +
                " selected=" + isSelected);
    }

    // ==================== ✅ UNICO PUNTO DI MODIFICA CACHE ====================

    /**
     * ✅ METODO CENTRALE - UNICO PUNTO dove mSelectedDates viene modificato
     * Tutti gli altri metodi DEVONO chiamare questo per modificare la selezione
     */
    private void updateSelectionSet(SelectionOperation operation, LocalDate date) {
        if (mUpdatingSelectionInternally) {
            Log.v(TAG, "updateSelectionSet: Skip - already updating internally");
            return;
        }

        mUpdatingSelectionInternally = true;

        try {
            boolean changed = false;
            int previousSize = mSelectedDates.size();

            // ✅ Capture previous selection for selective updates
            Set<LocalDate> previousSelection = new HashSet<>(mSelectedDates);

            switch (operation) {
                case CLEAR_ALL:
                    if (!mSelectedDates.isEmpty()) {
                        Log.d(TAG, "updateSelectionSet: CLEAR_ALL - clearing " + mSelectedDates.size() + " items");
                        mSelectedDates.clear();
                        mSingleSelectedDate = null; // ✅ Clear single selection tracking
                        changed = true;
                    }
                    break;

                case ADD_SINGLE:
                    if (mMultipleSelectionEnabled) {
                        // ✅ MULTIPLE MODE: Add to set if not present
                        if (!mSelectedDates.contains(date)) {
                            Log.d(TAG, "updateSelectionSet: ADD_SINGLE (multiple) - adding " + date);
                            mSelectedDates.add(date);
                            changed = true;
                        }
                    } else {
                        // ✅ SINGLE MODE: Replace current selection
                        LocalDate previousSingle = mSingleSelectedDate;
                        if (!date.equals(previousSingle)) {
                            Log.d(TAG, "updateSelectionSet: ADD_SINGLE (single) - replacing " +
                                    previousSingle + " with " + date);

                            mSelectedDates.clear();
                            mSelectedDates.add(date);
                            mSingleSelectedDate = date;

                            // Update previous selection for UI update
                            if (previousSingle != null) {
                                previousSelection.clear();
                                previousSelection.add(previousSingle);
                            }

                            changed = true;
                        }
                    }
                    break;

                case REMOVE_SINGLE:
                    if (date != null && mSelectedDates.contains(date)) {
                        Log.d(TAG, "updateSelectionSet: REMOVE_SINGLE - removing " + date);
                        mSelectedDates.remove(date);

                        // ✅ Update single selection tracking
                        if (date.equals(mSingleSelectedDate)) {
                            mSingleSelectedDate = null;
                        }

                        changed = true;
                    }
                    break;

                case ADD_ALL_VISIBLE:
                    if (mMultipleSelectionEnabled && mItems != null) {
                        Log.d(TAG, "updateSelectionSet: ADD_ALL_VISIBLE");
                        for (SharedViewModels.ViewItem item : mItems) {
                            if (item instanceof SharedViewModels.DayItem) {
                                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                                if (dayItem.day != null) {
                                    LocalDate itemDate = dayItem.day.getLocalDate();
                                    if (!mSelectedDates.contains(itemDate)) {
                                        mSelectedDates.add(itemDate);
                                        changed = true;
                                    }
                                }
                            }
                        }
                        // ✅ Clear single selection tracking in multiple mode
                        mSingleSelectedDate = null;
                    } else if (!mMultipleSelectionEnabled) {
                        Log.w(TAG, "updateSelectionSet: ADD_ALL_VISIBLE ignored in single selection mode");
                    }
                    break;
            }

            if (changed) {
                int newSize = mSelectedDates.size();
                Log.d(TAG, "updateSelectionSet: Selection changed " + previousSize + " → " + newSize);

                // ✅ SELECTIVE UI UPDATE instead of notifyDataSetChanged()
                performOptimalUIUpdate(operation, date, previousSelection);

                // ✅ Notify callback
                onSelectionModeChanged(mIsSelectionMode, newSize);

                // ✅ Auto-exit check
                if (mIsSelectionMode && newSize == 0) {
                    scheduleAutoExit();
                }
            } else {
                Log.v(TAG, "updateSelectionSet: No change needed for " + operation);
            }

        } finally {
            mUpdatingSelectionInternally = false;
        }
    }

    /**
     * ✅ Enum per operazioni supportate
     */
    private enum SelectionOperation {
        CLEAR_ALL,
        ADD_SINGLE,
        REMOVE_SINGLE,
        ADD_ALL_VISIBLE,
        MODE_CHANGE_ONLY
    }

    // ==================== ✅ NEW: TRANSITION HELPERS ====================

    /**
     * ✅ NEW: Handle transition from multiple to single selection
     */
    private void transitionToSingleSelection() {
        if (mSelectedDates.size() <= 1) {
            // 0 or 1 items selected - no change needed
            if (mSelectedDates.size() == 1) {
                mSingleSelectedDate = mSelectedDates.iterator().next();
                Log.d(TAG, "transitionToSingleSelection: Kept single item: " + mSingleSelectedDate);
            } else {
                mSingleSelectedDate = null;
                Log.d(TAG, "transitionToSingleSelection: No items selected");
            }
            return;
        }

        // Multiple items selected - keep only the first one (or most recent)
        LocalDate itemToKeep = mSelectedDates.iterator().next(); // Get first item

        Log.d(TAG, "transitionToSingleSelection: Multiple items selected, keeping " + itemToKeep +
                " and clearing " + (mSelectedDates.size() - 1) + " others");

        // Store items to clear for UI update
        Set<LocalDate> itemsToClear = new HashSet<>(mSelectedDates);
        itemsToClear.remove(itemToKeep);

        // Update selection
        mSelectedDates.clear();
        mSelectedDates.add(itemToKeep);
        mSingleSelectedDate = itemToKeep;

        // Update UI for cleared items
        if (!itemsToClear.isEmpty()) {
            updateMultipleItems(itemsToClear, "transitionToSingle");
        }

        // Notify change
        onSelectionModeChanged(mIsSelectionMode, 1);
    }

    // ==================== ✅ REFACTORED PUBLIC API WITH SELECTIVE UPDATES ====================

    /**
     * ✅ REFACTORED: Clear all selections - ora usa metodo centrale
     */
    public void clearSelections() {
        Log.d(TAG, "clearSelections: Requested");
        updateSelectionSet(SelectionOperation.CLEAR_ALL, null);
    }

    /**
     * ✅ REFACTORED: Toggle single day selection - ora usa metodo centrale
     */
    protected void toggleDaySelection(LocalDate date, boolean isSelected) {
        Log.d(TAG, "toggleDaySelection: " + date + " → " + isSelected);

        if (isSelected) {
            updateSelectionSet(SelectionOperation.ADD_SINGLE, date);
        } else {
            updateSelectionSet(SelectionOperation.REMOVE_SINGLE, date);
        }
    }

    /**
     * ✅ REFACTORED: Select all days - ora usa metodo centrale
     */
    public void selectAllDays() {
        if (!mMultipleSelectionEnabled) {
            Log.w(TAG, "selectAllDays: Ignored in single selection mode");
            return;
        }

        Log.d(TAG, "selectAllDays: Requested (multiple mode)");
        updateSelectionSet(SelectionOperation.ADD_ALL_VISIBLE, null);
    }

    /**
     * ✅ REFACTORED: Deselect all - ora usa metodo centrale
     */
    public void deselectAll() {
        Log.d(TAG, "deselectAll: Requested");

        if (!mSelectedDates.isEmpty()) {
            updateSelectionSet(SelectionOperation.CLEAR_ALL, null);
        } else if (mIsSelectionMode) {
            // ✅ Già vuoto ma ancora in selection mode - force exit senza clear
            setSelectionModeInternal(false);
        }
    }


    /**
     * ✅ NEW: Select specific date (works in both modes)
     */
    public void selectDate(LocalDate date) {
        if (date != null) {
            Log.d(TAG, "selectDate: " + date + " (mode: " +
                    (mMultipleSelectionEnabled ? "MULTIPLE" : "SINGLE") + ")");

            if (!mIsSelectionMode) {
                setSelectionMode(true);
            }

            updateSelectionSet(SelectionOperation.ADD_SINGLE, date);
        }
    }


    /**
     * ✅ NEW: Toggle date selection with mode awareness
     */
    public void toggleDateSelection(LocalDate date) {
        if (date == null) return;

        if (mSelectedDates.contains(date)) {
            // Deselect
            updateSelectionSet(SelectionOperation.REMOVE_SINGLE, date);
        } else {
            // Select
            updateSelectionSet(SelectionOperation.ADD_SINGLE, date);
        }
    }

    @Override
    public void refreshData() {
        Log.d(TAG, "refreshData: ✅ Force refreshing adapter data");

        try {
            // ✅ Clear any internal selection state if needed
            if (mIsSelectionMode && mSelectedDates.isEmpty()) {
                Log.d(TAG, "refreshData: Cleaning up empty selection mode");
                setSelectionMode(false);
            }

            // ✅ Notify adapter of data changes
            notifyDataSetChanged();

            // ✅ If this adapter has a fragment reference, trigger its refresh
            if (mLongClickListener instanceof BaseClickFragmentLegacy) {
                BaseClickFragmentLegacy fragment = (BaseClickFragmentLegacy) mLongClickListener;

                // Use main thread to safely trigger fragment refresh
                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        try {
                            // Trigger fragment-specific refresh methods
//                            fragment.triggerFragmentDataReload();
                        } catch (Exception e) {
                            Log.e(TAG, "Error triggering fragment reload: " + e.getMessage(), e);
                        }
                    });
                }
            }

            Log.d(TAG, "refreshData: ✅ Adapter refresh completed");

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing adapter data: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NEW: Force update specific date (useful for external state changes)
     */
    public void refreshDateSelection(LocalDate date) {
        if (date != null) {
            updateSingleItem(date, "refreshDateSelection");
        }
    }

    /**
     * ✅ NEW: Force update multiple dates
     */
    public void refreshDatesSelection(Set<LocalDate> dates) {
        if (dates != null && !dates.isEmpty()) {
            updateMultipleItems(dates, "refreshDatesSelection");
        }
    }

    /**
     * ✅ NEW: Get compact selection state for debugging
     * ✅ ENHANCED: Get selection summary with mode info
     */
    public String getSelectionStateSummary() {
        return String.format(QDue.getLocale(),
                "SelectionState{mode=%s, count=%d, type=%s, single=%s}",
                mIsSelectionMode ? "ON" : "OFF",
                mSelectedDates.size(),
                mMultipleSelectionEnabled ? "MULTIPLE" : "SINGLE",
                mSingleSelectedDate != null ? mSingleSelectedDate.toString() : "null");
    }

    /**
     * ✅ NEW: Check if date can be selected (considering mode and current state)
     */
    public boolean canSelectDate(LocalDate date) {
        if (date == null) return false;

        if (mMultipleSelectionEnabled) {
            // Multiple mode: can always select if not already selected
            return !mSelectedDates.contains(date);
        } else {
            // Single mode: can select if different from current
            return !date.equals(mSingleSelectedDate);
        }
    }

    /**
     * ✅ NEW: Get max selection count based on mode
     */
    public int getMaxSelectionCount() {
        return mMultipleSelectionEnabled ? Integer.MAX_VALUE : 1;
    }

    /**
     * ✅ NEW: Check if selection is at maximum capacity
     */
    public boolean isSelectionAtMaxCapacity() {
        return !mMultipleSelectionEnabled && mSelectedDates.size() >= 1;
    }

    /**
     * ✅ NEW: Enable/disable multiple selection
     * @param enabled true for multiple selection, false for single selection
     */
    public void setMultipleSelectionEnabled(boolean enabled) {
        if (mMultipleSelectionEnabled == enabled) {
            Log.v(TAG, "setMultipleSelectionEnabled: Already " + enabled);
            return;
        }

        Log.d(TAG, "setMultipleSelectionEnabled: " + mMultipleSelectionEnabled + " → " + enabled);

        boolean wasMultiple = mMultipleSelectionEnabled;
        mMultipleSelectionEnabled = enabled;

        // Handle transition from multiple to single
        if (wasMultiple && !enabled) {
            transitionToSingleSelection();
        }
        // Transition from single to multiple doesn't need special handling

        Log.d(TAG, "setMultipleSelectionEnabled: Selection mode changed to " +
                (enabled ? "MULTIPLE" : "SINGLE"));
    }

    /**
     * ✅ NEW: Check if multiple selection is enabled
     */
    public boolean isMultipleSelectionEnabled() {
        return mMultipleSelectionEnabled;
    }

    /**
     * ✅ NEW: Get currently selected item in single selection mode
     */
    public LocalDate getSingleSelectedDate() {
        return mSingleSelectedDate;
    }

    // ==================== ✅ SELECTION MODE MANAGEMENT REFACTORED ====================

    /**
     * ✅ REFACTORED: Set selection mode - eliminato clear diretto
     */
    public void setSelectionMode(boolean isSelectionMode) {
        setSelectionModeInternal(isSelectionMode);
    }

    /**
     * ✅ NEW: Internal selection mode setter senza loop
     * ✅ ENHANCED: Selection mode with selective updates
     */
    private void setSelectionModeInternal(boolean isSelectionMode) {
        if (mIsSelectionMode == isSelectionMode) {
            Log.v(TAG, "setSelectionModeInternal: Already " + isSelectionMode);
            return;
        }

        Log.d(TAG, "setSelectionModeInternal: " + mIsSelectionMode + " → " + isSelectionMode);

        boolean wasSelectionMode = mIsSelectionMode;
        mIsSelectionMode = isSelectionMode;

        if (!isSelectionMode && wasSelectionMode && !mSelectedDates.isEmpty()) {
            // ✅ Exiting with selections - clear via central method (selective update)
            Log.d(TAG, "setSelectionModeInternal: Exiting with selections - clearing");
            updateSelectionSet(SelectionOperation.CLEAR_ALL, null);
        } else {
            // ✅ Mode change without selection change - need full UI update for visual mode
            Log.d(TAG, "setSelectionModeInternal: Mode change only - full UI update");
            performOptimalUIUpdate(SelectionOperation.MODE_CHANGE_ONLY, null, null);
            onSelectionModeChanged(isSelectionMode, mSelectedDates.size());
        }

        if (isSelectionMode) {
            cancelAutoExit();
        }
    }
    // ==================== ✅ INDIVIDUAL ITEM UPDATE METHODS ====================

    /**
     * ✅ NEW: Update individual ViewHolder selection state (called from ViewHolder)
     * Used when ViewHolder needs to update its own state without going through central method
     */
    public void updateViewHolderSelectionState(LocalDate date, boolean isSelected, String reason) {
        Log.v(TAG, "updateViewHolderSelectionState: " + date + " → " + isSelected + " (" + reason + ")");
        updateSingleItem(date, "ViewHolder: " + reason);
    }

    /**
     * ✅ ENHANCED: Existing method now uses selective update
     */
    protected void updateViewHolderSelection(LocalDate date, boolean isSelected) {
        updateSingleItem(date, "updateViewHolderSelection");
    }

    // ==================== ✅ HELPER METHODS PER SELECTIVE UPDATE ====================

    /**
     * ✅ NEW: Update single item by date
     */
    private void updateSingleItem(LocalDate date, String reason) {
        int position = findPositionForDate(date);
        if (position >= 0) {
            Log.v(TAG, "updateSingleItem: " + date + " at position " + position + " (" + reason + ")");
            notifyItemChanged(position);
        } else {
            Log.w(TAG, "updateSingleItem: Date " + date + " not found in adapter");
        }
    }

    /**
     * ✅ NEW: Update multiple items by date set
     */
    private void updateMultipleItems(Set<LocalDate> dates, String reason) {
        if (dates == null || dates.isEmpty()) return;

        Log.d(TAG, "updateMultipleItems: Updating " + dates.size() + " items (" + reason + ")");

        for (LocalDate date : dates) {
            int position = findPositionForDate(date);
            if (position >= 0) {
                Log.v(TAG, "updateMultipleItems: " + date + " at position " + position);
                notifyItemChanged(position);
            }
        }
    }

    /**
     * ✅ NEW: Selective vs Full update decision
     * ✅ ENHANCED: UI update with single selection awareness
     */
    private void performOptimalUIUpdate(SelectionOperation operation, LocalDate affectedDate,
                                        Set<LocalDate> previousSelection) {

        switch (operation) {
            case ADD_SINGLE:
                if (!mMultipleSelectionEnabled && previousSelection != null && !previousSelection.isEmpty()) {
                    // ✅ SINGLE MODE: Update both old and new selection
                    Set<LocalDate> datesToUpdate = new HashSet<>(previousSelection);
                    if (affectedDate != null) {
                        datesToUpdate.add(affectedDate);
                    }
                    updateMultipleItems(datesToUpdate, "SINGLE_REPLACE");
                } else {
                    // ✅ MULTIPLE MODE or no previous selection: update only affected item
                    if (affectedDate != null) {
                        updateSingleItem(affectedDate, operation.name());
                    }
                }
                break;

            case REMOVE_SINGLE:
                // ✅ Same logic for both modes
                if (affectedDate != null) {
                    updateSingleItem(affectedDate, operation.name());
                }
                break;

            case CLEAR_ALL:
                // ✅ Clear all - update only previously selected items
                if (previousSelection != null && !previousSelection.isEmpty()) {
                    updateMultipleItems(previousSelection, "CLEAR_ALL");
                } else {
                    Log.v(TAG, "performOptimalUIUpdate: CLEAR_ALL with no previous selection");
                }
                break;

            case ADD_ALL_VISIBLE:
                // ✅ Select all - only available in multiple mode
                if (mMultipleSelectionEnabled) {
                    Log.d(TAG, "performOptimalUIUpdate: ADD_ALL_VISIBLE - full update");
                    notifyDataSetChanged();
                } else {
                    Log.w(TAG, "performOptimalUIUpdate: ADD_ALL_VISIBLE ignored in single mode");
                }
                break;

            case MODE_CHANGE_ONLY:
                // ✅ Mode change without selection change
                Log.d(TAG, "performOptimalUIUpdate: MODE_CHANGE_ONLY - full update");
                notifyDataSetChanged();
                break;
        }
    }

    // ==================== ✅ AUTO-EXIT MANAGEMENT MIGLIORATO ====================

    /**
     * ✅ NEW: Schedule auto-exit con handler cleanup
     */
    private void scheduleAutoExit() {
        // ✅ Cancel previous auto-exit
        cancelAutoExit();

        if (mIsSelectionMode && mSelectedDates.isEmpty()) {
            Log.d(TAG, "scheduleAutoExit: Scheduling auto-exit in 800ms");

            mAutoExitRunnable = () -> {
                if (mIsSelectionMode && mSelectedDates.isEmpty()) {
                    Log.d(TAG, "scheduleAutoExit: Executing auto-exit");
                    setSelectionModeInternal(false);
                } else {
                    Log.d(TAG, "scheduleAutoExit: Conditions changed - skipping auto-exit");
                }
                mAutoExitRunnable = null;
            };

            mMainHandler.postDelayed(mAutoExitRunnable, AUTO_EXIT_SELECTION_MODE_DELAY); // Manteniamo delay richiesto
        }
    }

    /**
     * ✅ NEW: Cancel auto-exit scheduled
     */
    private void cancelAutoExit() {
        if (mAutoExitRunnable != null) {
            Log.v(TAG, "cancelAutoExit: Cancelling scheduled auto-exit");
            mMainHandler.removeCallbacks(mAutoExitRunnable);
            mAutoExitRunnable = null;
        }
    }

    // ==================== ✅ CALLBACK METHODS LOOP-SAFE ====================

    @Override
    public void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected) {
        if (mUpdatingSelectionInternally) {
            Log.v(TAG, "onDaySelectionChanged: Skip - updating internally");
            return; // ✅ Prevent callback loop
        }

        Log.d(TAG, "onDaySelectionChanged: " + date + " → " + isSelected);

        // ✅ Update state via central method
        toggleDaySelection(date, isSelected);

        // ✅ Forward to fragment listener (outside internal update)
        if (mLongClickListener != null) {
            mLongClickListener.onDaySelectionChanged(day, date, isSelected);
        }
    }

    // ==================== ✅ CLEANUP MIGLIORATO ====================

    /**
     * ✅ REFACTORED: Cleanup con handler cleanup
     */
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up");

        // ✅ Cancel scheduled auto-exit
        cancelAutoExit();

        // ✅ Clear selections via central method
        if (!mSelectedDates.isEmpty()) {
            updateSelectionSet(SelectionOperation.CLEAR_ALL, null);
        }

        // ✅ Clear references
        mRegularClickListener = null;
        mLongClickListener = null;
        mMainHandler = null;

        // ✅ Cleanup other resources
    }

    // ==================== ✅ METODI READONLY (non modificano state) ====================

    /**
     * ✅ Get selected dates (defensive copy)
     */
    public Set<LocalDate> getSelectedDates() {
        return new HashSet<>(mSelectedDates);
    }

    /**
     * ✅ Get selection count
     */
    public int getSelectedCount() {
        return mSelectedDates.size();
    }

    /**
     * ✅ Check selection mode
     */
    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    /**
     * ✅ Check if date is selected
     */
    public boolean isDateSelected(LocalDate date) {
        return mSelectedDates.contains(date);
    }

    // ==================== ✅ PUBLIC API METHODS ====================

    /**
     * ✅ Public method to exit selection mode
     */
    public void exitSelectionMode() {
        setSelectionMode(false);
    }

    /**
     * ✅ Handle back press
     */
    public boolean onBackPressed() {
        if (mIsSelectionMode) {
            Log.d(TAG, "onBackPressed: Exiting selection mode");
            setSelectionMode(false);
            return true;
        }
        return false;
    }

    // ==================== ✅ LOGGING OPTIMIZATION ====================

    /**
     * ✅ NEW: Concise selection state logging
     * ✅ ENHANCED: Enhanced logging with mode info
     */
    private void logSelectionSummary(String context) {
        Log.d(TAG, String.format(QDue.getLocale(),
                "%s: mode=%s, count=%d, type=%s, dates=%s",
                context,
                mIsSelectionMode ? "ON" : "OFF",
                mSelectedDates.size(),
                mMultipleSelectionEnabled ? "MULTI" : "SINGLE",
                mSelectedDates.size() <= 3 ? mSelectedDates.toString() :
                        "[" + mSelectedDates.size() + " dates]"
        ));
    }

    // Existing methods remain unchanged...
    // (setupLongClickSupport, clearSelections, deselectAll, etc.)




/// ///////////////////////////////////////////////////////////////////////////////////

    // ===========================================
    // DayLongClickListener Implementation
    // ===========================================
    @Override
    public void onDayLongClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, "onDayLongClick: " + date + " (mode: " +
                (mMultipleSelectionEnabled ? "MULTIPLE" : "SINGLE") + ")");

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
    }

    // ===========================================
    // Selection Management
    // ===========================================



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