package net.calvuz.qdue.ui.events.interfaces;

/**
 * Listener interface for event deletion operations
 * Following the same pattern as existing interfaces
 */
public interface EventDeletionListener {
    /**
     * Called when deletion is requested and confirmed by user
     */
    void onDeletionRequested();

    /**
     * Called when user cancels the deletion
     */
    void onDeletionCancelled();

    /**
     * Called when deletion operation completes
     * @param success Whether deletion was successful
     * @param message Result message
     */
    void onDeletionCompleted(boolean success, String message);
}