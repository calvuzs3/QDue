package net.calvuz.qdue.core.common.listeners;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.services.models.OperationResult;

/**
 * Interface for communication between EventsActivity and EventsListFragment
 *
 * Handles callbacks for file operations, backup/restore, and data updates
 */
public interface EventsOperationListener {

//    // ==================== IMPORT OPERATIONS ====================
//
//    /**
//     * Called when import operation completes successfully
//     * @param eventsCount Number of events imported
//     * @param packageName Name of imported package
//     * @param hasWarnings Whether import had warnings
//     */
//    void onImportComplete(int eventsCount, String packageName, boolean hasWarnings);
//
//    /**
//     * Called when import operation fails
//     * @param error Error message
//     * @param canRetry Whether operation can be retried
//     */
//    void onImportError(String error, boolean canRetry);
//
//    /**
//     * Called during import progress
//     * @param processed Number of events processed
//     * @param total Total events to process
//     * @param currentEvent Name of current event being processed
//     */
//    void onImportProgress(int processed, int total, String currentEvent);
//
//    // ==================== EXPORT OPERATIONS ====================
//
//    /**
//     * Called when export operation completes successfully
//     * @param eventsCount Number of events exported
//     * @param filename Name of exported file
//     * @param fileSize Size of exported file in bytes
//     */
//    void onExportComplete(int eventsCount, String filename, long fileSize);
//
//    /**
//     * Called when export operation fails
//     * @param error Error message
//     */
//    void onExportError(String error);
//
//    /**
//     * Called during export progress
//     * @param processed Number of events processed
//     * @param total Total events to process
//     */
//    void onExportProgress(int processed, int total);
//
//    // ==================== BACKUP/RESTORE OPERATIONS ====================
//
//    /**
//     * Called when backup operation completes
//     * @param success Whether backup was successful
//     * @param eventsCount Number of events backed up
//     * @param backupPath Path to backup file
//     */
//    void onBackupComplete(boolean success, int eventsCount, String backupPath);
//
//    /**
//     * Called when restore operation completes
//     * @param success Whether restore was successful
//     * @param restoredEvents Number of events restored
//     * @param replacedAll Whether all events were replaced
//     */
//    void onRestoreComplete(boolean success, int restoredEvents, boolean replacedAll);

    // ==================== DATA OPERATIONS ====================

    /**
     * Called when all events are cleared
     * @param clearedCount Number of events that were cleared
     * @param backupCreated Whether backup was created before clearing
     */
    void onEventsCleared(int clearedCount, boolean backupCreated);

    /**
     * Called when new event is created
     * @param event The newly created event
     */
    void onEventCreated(LocalEvent event);

    /**
     * Called when event is updated
     * @param result Operation result with updated event
     */
    void onEventUpdated(OperationResult<LocalEvent> result);

    /**
     * Called when event is deleted
     * @param result Operation result with deletion details
     */
    void onEventDeleted(OperationResult<String> result);

    /**
     * Called when events are imported
     * @param result Operation result with import details
     */
    void onEventsImported(OperationResult<Integer> result);

    /**
     * Called when events list should be refreshed
     * @param reason Reason for refresh
     */
    void onRefreshRequired(String reason);

    // ==================== EVENT OPERATIONS DELEGATION ====================

    /**
     * Delete specific event with confirmation and undo
     * @param event Event to delete
     * @param listener Callback for deletion status
     */
    void triggerEventDeletion(LocalEvent event, EventDeletionListener listener);

    /**
     * Edit existing event
     * @param event Event to edit
     */
    void triggerEventEdit(LocalEvent event);

    /**
     * Duplicate existing event
     * @param event Event to duplicate
     */
    void triggerEventDuplicate(LocalEvent event);

    /**
     * Share event via system share intent
     * @param event Event to share
     */
    void triggerEventShare(LocalEvent event);

    /**
     * Add event to system calendar
     * @param event Event to add to calendar
     */
    void triggerAddToCalendar(LocalEvent event);


//    // ==================== UI OPERATIONS ====================
//
//    /**
//     * Called when a long-running operation starts
//     * @param operationType Type of operation starting
//     * @param message Message to show to user
//     */
//    void onOperationStarted(String operationType, String message);
//
//    /**
//     * Called when a long-running operation ends
//     * @param operationType Type of operation that ended
//     */
//    void onOperationEnded(String operationType);
//
//    /**
//     * Request to show retry option to user
//     * @param message Error message
//     * @param retryAction Action to execute on retry
//     */
//    void showRetryOption(String message, Runnable retryAction);
//
//    /**
//     * Request to show undo option to user
//     * @param message Message describing what can be undone
//     * @param undoAction Action to execute on undo
//     * @param timeoutSeconds How long undo option should be available
//     */
//    void showUndoOption(String message, Runnable undoAction, int timeoutSeconds);
}