package net.calvuz.qdue.ui.shared;

/**
 * Interface for Fragment to Activity communication.
 *
 * This interface defines the contract for fragments to communicate with their host activity.
 * It follows Android best practices for fragment-activity communication using callbacks.
 *
 * Usage:
 * 1. Activity implements this interface
 * 2. Fragment gets reference to activity via this interface
 * 3. Fragment calls methods when it needs to communicate
 *
 * Benefits:
 * - Type-safe communication
 * - Loose coupling between fragment and activity
 * - Easy to test and maintain
 * - Clear contract definition
 *
 * @author Updated with English comments and best practices
 * @version 2.0
 * @since 2025
 */
public interface FragmentCommunicationInterface {

    /**
     * Called when fragment needs to update the activity title.
     * This is commonly used when navigating between different fragments
     * that should show different titles in the toolbar.
     *
     * @param title The new title to display in the toolbar
     */
//    void onFragmentTitleChanged(String title);

    /**
     * Called when fragment needs to send a status message to the activity.
     * Useful for showing user feedback, error messages, or notifications
     * that should be handled at the activity level (e.g., Snackbar, Toast).
     *
     * @param message The status message to display
     * @param isError Whether this is an error message (affects styling)
     */
//    void onFragmentStatusMessage(String message, boolean isError);

    /**
     * Called when fragment needs to update the toolbar menu.
     * Different fragments might need different menu items or states.
     *
     * @param menuId Resource ID of the menu to inflate, or 0 to clear menu
     */
//    void onFragmentMenuChanged(int menuId);

    /**
     * Called when fragment needs to show/hide the toolbar.
     * Some fragments might want to hide the toolbar for full-screen experience.
     *
     * @param visible Whether the toolbar should be visible
     */
//    void onFragmentToolbarVisibilityChanged(boolean visible);

    /**
     * Called when fragment needs to navigate to another destination.
     * This allows the activity to handle navigation logic centrally.
     *
     * @param destinationId Navigation destination ID
     * @param data Optional data bundle to pass to destination
     */
    void onFragmentNavigationRequested(int destinationId, android.os.Bundle data);

    /**
     * Called when fragment has completed a significant operation.
     * This can trigger activity-level actions like updating other fragments,
     * refreshing data, or showing completion messages.
     *
     * @param operationType Type of operation completed (e.g., "data_refresh", "save_complete")
     * @param success Whether the operation was successful
     * @param resultData Optional result data from the operation
     */
//    void onFragmentOperationComplete(String operationType, boolean success, android.os.Bundle resultData);

    /**
     * Generic method for custom communication needs.
     * When specific methods above don't fit your use case,
     * this provides a flexible way to send custom messages.
     *
     * @param action Action identifier (e.g., "update_fab", "show_loading")
     * @param data Data bundle containing parameters for the action
     */
//    void onFragmentCustomAction(String action, android.os.Bundle data);
}