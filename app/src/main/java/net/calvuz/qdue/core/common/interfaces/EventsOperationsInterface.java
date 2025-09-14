package net.calvuz.qdue.core.common.interfaces;

import net.calvuz.qdue.domain.calendar.events.models.EventEntityGoogle;
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
    void triggerEventDeletion(EventEntityGoogle event, EventDeletionListener listener);

    /**
     * Edit existing event
     * @param event Event to edit
     */
    void triggerEventEdit(EventEntityGoogle event);

    /**
     * Edit existing event from the list contextual menu
     * @param event Event to edit
     */
    void triggerEventEditFromList(EventEntityGoogle event);

    /**
     * Duplicate existing event
     * @param event Event to duplicate
     */
    void triggerEventDuplicate(EventEntityGoogle event);

    /**
     * Share event via system share intent
     * @param event Event to share
     */
    void triggerEventShare(EventEntityGoogle event);

    /**
     * Add event to system calendar
     * @param event Event to add to calendar
     */
    void triggerAddToCalendar(EventEntityGoogle event);

    /**
     * Create new event
     */
    void triggerCreateNewEvent();
}

