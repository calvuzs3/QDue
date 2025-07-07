package net.calvuz.qdue.core.ui.interfaces;


/**
 * Interface for selection mode handling across different components.
 *
 * Many components (lists, grids, etc.) have selection modes that should
 * be exited on back press. This interface provides a standard way to handle this.
 */
public interface SelectionModeHandler extends BackPressHandler {

    /**
     * Check if component is currently in selection mode
     *
     * @return true if in selection mode
     */
    boolean isInSelectionMode();

    /**
     * Exit selection mode
     *
     * @return true if selection mode was exited, false if not in selection mode
     */
    boolean exitSelectionMode();

    /**
     * Get the number of selected items
     *
     * @return number of selected items, or 0 if not in selection mode
     */
    default int getSelectedItemCount() {
        return 0;
    }

    /**
     * Default implementation that exits selection mode on back press
     */
    @Override
    default boolean onBackPressed() {
        if (isInSelectionMode()) {
            return exitSelectionMode();
        }
        return false;
    }
}
