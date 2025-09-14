package net.calvuz.qdue.core.common.interfaces;

import net.calvuz.qdue.domain.calendar.events.models.EventEntityGoogle;
import net.calvuz.qdue.core.common.listeners.EventDeletionListener;

public interface EventsDatabaseOperationsInterface {

    /**
     * Delete specific event by ID with undo functionality
     * @param event Event to delete
     * @param listener Callback for deletion status
     */
    void triggerEventDeletion(EventEntityGoogle event, EventDeletionListener listener);

    /**
     * Delete All Events from DB
     */
    void triggerDeleteAllEvents();
}
