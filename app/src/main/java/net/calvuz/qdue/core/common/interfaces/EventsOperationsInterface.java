package net.calvuz.qdue.core.common.interfaces;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.common.listeners.EventDeletionListener;

/**
 * Interface for event-specific operations (CRUD)
 * Extends the existing pattern with event management operations
 */
public interface EventsOperationsInterface {

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
     * Edit existing event from the list contextual menu
     * @param event Event to edit
     */
    void triggerEventEditFromList(LocalEvent event);

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

    /**
     * Create new event
     */
    void triggerCreateNewEvent();
}

