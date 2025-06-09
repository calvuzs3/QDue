package net.calvuz.qdue.events;

import net.calvuz.qdue.events.models.LocalEvent;

/**
 * EventDao interface
 * TODO: Define this interface based on your actual database structure
 */
public interface EventDao {
    void deleteEventsByPackageId(String packageId);
    void insertEvent(LocalEvent localEvent);
    void deleteAllLocalEvents();
}