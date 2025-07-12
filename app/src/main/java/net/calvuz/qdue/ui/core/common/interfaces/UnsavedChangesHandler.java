package net.calvuz.qdue.ui.core.common.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Enhanced interface for components that can have unsaved changes.
 *
 * This interface extends BackPressHandler to provide sophisticated handling
 * of unsaved changes with user confirmation dialogs.
 *
 * Components implementing this interface can automatically integrate with
 * the back handling system to prevent data loss.
 */
public interface UnsavedChangesHandler extends BackPressHandler {

    /**
     * Check if component has unsaved changes
     *
     * @return true if there are unsaved changes that would be lost on navigation
     */
    boolean hasUnsavedChanges();

    /**
     * Handle unsaved changes with user confirmation
     *
     * This method should present appropriate UI (dialog, snackbar, etc.) to let
     * the user decide how to proceed with unsaved changes.
     *
     * @param onProceed Callback to execute if user wants to proceed (discard changes)
     * @param onCancel Callback to execute if user wants to cancel navigation
     */
    void handleUnsavedChanges(@NonNull Runnable onProceed, @NonNull Runnable onCancel);

    /**
     * Get a user-friendly description of what changes would be lost
     *
     * @return Description for user dialogs, or null for default message
     */
    @Nullable
    default String getUnsavedChangesDescription() {
        return "You have unsaved changes that will be lost.";
    }

    /**
     * Default implementation that delegates to unsaved changes handling
     *
     * Components can override this if they need more complex logic.
     */
    @Override
    default boolean onBackPressed() {
        if (hasUnsavedChanges()) {
            handleUnsavedChanges(
                    this::proceedWithBackNavigation,
                    this::cancelBackNavigation
            );
            return true; // Always handle if there are unsaved changes
        }
        return false; // No unsaved changes, let others handle
    }

    /**
     * Called when user confirms they want to proceed despite unsaved changes
     *
     * Default implementation does nothing. Override to implement custom logic
     * like clearing unsaved state, notifying other components, etc.
     */
    default void proceedWithBackNavigation() {
        // Default: do nothing - let the system handle navigation
    }

    /**
     * Called when user cancels navigation due to unsaved changes
     *
     * Default implementation does nothing. Override to implement custom logic
     * like focusing on unsaved fields, showing save options, etc.
     */
    default void cancelBackNavigation() {
        // Default: do nothing - stay on current screen
    }
}