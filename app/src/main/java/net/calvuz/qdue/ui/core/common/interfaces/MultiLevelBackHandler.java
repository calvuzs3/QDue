package net.calvuz.qdue.ui.core.common.interfaces;

import androidx.annotation.Nullable;

/**
 * Marker interface for components that support complex back handling scenarios.
 * <p>
 * Components implementing this interface can handle multiple back scenarios
 * in a prioritized manner (e.g., first exit selection mode, then handle unsaved changes).
 */
public interface MultiLevelBackHandler extends BackPressHandler {

    /**
     * Get the priority level for back handling
     * <p>
     * Higher values indicate higher priority. Standard levels:
     * - 100: Critical actions (unsaved changes)
     * - 50: UI state changes (selection modes, search)
     * - 0: Navigation (default)
     *
     * @return priority level
     */
    default int getBackHandlingPriority() {
        return 0;
    }

    /**
     * Get a description of what this handler does
     *
     * @return description for debugging/logging
     */
    @Nullable
    default String getBackHandlingDescription() {
        return getClass().getSimpleName() + " back handler";
    }
}