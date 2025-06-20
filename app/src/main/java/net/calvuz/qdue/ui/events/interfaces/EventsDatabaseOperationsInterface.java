package net.calvuz.qdue.ui.events.interfaces;

import net.calvuz.qdue.events.models.LocalEvent;

public interface EventsDatabaseOperationsInterface {

    /**
     * Delete specific event by ID with undo functionality
     * @param event Event to delete
     * @param listener Callback for deletion status
     */
    void triggerEventDeletion(LocalEvent event, EventDeletionListener listener);

    /**
     * Delete All Events from DB
     */
    void triggerDeleteAllEvents();
}
