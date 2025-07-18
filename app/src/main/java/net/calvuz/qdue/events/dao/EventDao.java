package net.calvuz.qdue.events.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.EventPriority;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EventDao interface for Google Calendar-like event operations.
 * Provides comprehensive CRUD operations and advanced querying capabilities.
 * <p>
 * Features:
 * - Basic CRUD operations
 * - Calendar-specific date range queries
 * - Search and filtering capabilities
 * - Batch operations for performance
 * - Conflict detection for overlapping events
 */
@Dao
public interface EventDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Insert a single event.
     * @param event Event to insert
     * @return Row ID of inserted event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertEvent(LocalEvent event);

    /**
     * Insert multiple events in batch for better performance.
     * @param events List of events to insert
     * @return Array of row IDs for inserted events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertEvents(List<LocalEvent> events);

    /**
     * Update an existing event.
     * @param event Event with updated data
     * @return Number of rows affected (should be 1 if successful)
     */
    @Update
    int updateEvent(LocalEvent event);

    /**
     * Update multiple events in batch.
     * @param events List of events to update
     * @return Number of rows affected
     */
    @Update
    int updateEvents(List<LocalEvent> events);

    /**
     * Delete a specific event.
     * @param event Event to delete
     * @return Number of rows affected (should be 1 if successful)
     */
    @Delete
    int deleteEvent(LocalEvent event);

    /**
     * Delete event by ID.
     * @param eventId ID of event to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM events WHERE id = :eventId")
    int deleteEventById(String eventId);

    /**
     * Delete all local events (non-package events).
     */
    @Query("DELETE FROM events WHERE package_id IS NULL OR package_id = ''")
    int deleteAllLocalEvents();

    /**
     * Delete all events from a specific package.
     * @param packageId Package ID to delete events from
     */
    @Query("DELETE FROM events WHERE package_id = :packageId")
    void deleteEventsByPackageId(String packageId);

    /**
     * Delete all events (complete cleanup).
     */
    @Query("DELETE FROM events")
    int deleteAllEvents();

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Get event by ID.
     * @param eventId Event ID to search for
     * @return Event if found, null otherwise
     */
    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    LocalEvent getEventById(String eventId);

    /**
     * Get all events ordered by start time.
     * @return List of all events
     */
    @Query("SELECT * FROM events ORDER BY start_time ASC")
    List<LocalEvent> getAllEvents();

    /**
     * Check if event exists by ID.
     * @param eventId Event ID to check
     * @return True if exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM events WHERE id = :eventId")
    boolean eventExists(String eventId);

    /**
     * Get upcoming events from current time.
     * @param currentTime Current timestamp
     * @return Number of upcoming events
     */
    @Query("SELECT COUNT(*) FROM events WHERE start_time >= :currentTime ")
    int getUpcomingEventsCount(LocalDateTime currentTime);

    /**
     * Get total count of events.
     * @return Total number of events in database
     */
    @Query("SELECT COUNT(*) FROM events")
    int getEventsCount();

    // ==================== CALENDAR-SPECIFIC QUERIES ====================

    /**
     * Get all events for a specific date (both all-day and timed events).
     * @param startOfDay Start of the day (00:00:00)
     * @param endOfDay End of the day (23:59:59)
     * @return List of events for that date
     */
    @Query("SELECT * FROM events WHERE " +
            "(DATE(start_time) = DATE(:startOfDay)) OR " +
            "(start_time <= :endOfDay AND end_time >= :startOfDay) " +
            "ORDER BY all_day DESC, start_time ASC")
    List<LocalEvent> getEventsForDate(LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * Get events within a date range (for calendar views).
     * @param startDate Range start date
     * @param endDate Range end date
     * @return List of events in the specified range
     */
    @Query("SELECT * FROM events WHERE " +
            "start_time <= :endDate AND end_time >= :startDate " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getEventsForDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get upcoming events from current time.
     * @param currentTime Current timestamp
     * @param limit Maximum number of events to return
     * @return List of upcoming events
     */
    @Query("SELECT * FROM events WHERE start_time >= :currentTime " +
            "ORDER BY start_time ASC LIMIT :limit")
    List<LocalEvent> getUpcomingEvents(LocalDateTime currentTime, int limit);

    /**
     * Get past events before current time.
     * @param currentTime Current timestamp
     * @param limit Maximum number of events to return
     * @return List of past events (most recent first)
     */
    @Query("SELECT * FROM events WHERE end_time < :currentTime " +
            "ORDER BY end_time DESC LIMIT :limit")
    List<LocalEvent> getPastEvents(LocalDateTime currentTime, int limit);

    /**
     * Get events for current week.
     * @param weekStart Start of week
     * @param weekEnd End of week
     * @return List of events for the week
     */
    @Query("SELECT * FROM events WHERE " +
            "start_time <= :weekEnd AND end_time >= :weekStart " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getEventsForWeek(LocalDateTime weekStart, LocalDateTime weekEnd);

    /**
     * Get events for current month.
     * @param monthStart Start of month
     * @param monthEnd End of month
     * @return List of events for the month
     */
    @Query("SELECT * FROM events WHERE " +
            "start_time <= :monthEnd AND end_time >= :monthStart " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getEventsForMonth(LocalDateTime monthStart, LocalDateTime monthEnd);

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Search events by title (case-insensitive).
     * @param titleQuery Search query for title
     * @return List of matching events
     */
    @Query("SELECT * FROM events WHERE title LIKE '%' || :titleQuery || '%' " +
            "ORDER BY start_time ASC")
    List<LocalEvent> searchEventsByTitle(String titleQuery);

    /**
     * Search events by description (case-insensitive).
     * @param descriptionQuery Search query for description
     * @return List of matching events
     */
    @Query("SELECT * FROM events WHERE description LIKE '%' || :descriptionQuery || '%' " +
            "ORDER BY start_time ASC")
    List<LocalEvent> searchEventsByDescription(String descriptionQuery);

    /**
     * Search events by title or description.
     * @param query Search query
     * @return List of matching events
     */
    @Query("SELECT * FROM events WHERE " +
            "title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' " +
            "ORDER BY start_time ASC")
    List<LocalEvent> searchEvents(String query);

    /**
     * Get events by type.
     * @param eventType Type of events to retrieve
     * @return List of events of specified type
     */
    @Query("SELECT * FROM events WHERE event_type = :eventType ORDER BY start_time ASC")
    List<LocalEvent> getEventsByType(EventType eventType);

    /**
     * Get events by priority.
     * @param priority Priority level
     * @return List of events with specified priority
     */
    @Query("SELECT * FROM events WHERE priority = :priority ORDER BY start_time ASC")
    List<LocalEvent> getEventsByPriority(EventPriority priority);

    /**
     * Get events by location.
     * @param location Location to search for
     * @return List of events at specified location
     */
    @Query("SELECT * FROM events WHERE location = :location ORDER BY start_time ASC")
    List<LocalEvent> getEventsByLocation(String location);

    /**
     * Get events from specific package.
     * @param packageId Package ID
     * @return List of events from that package
     */
    @Query("SELECT * FROM events WHERE package_id = :packageId ORDER BY start_time ASC")
    List<LocalEvent> getEventsByPackageId(String packageId);

    /**
     * Get only local events (no package ID).
     * @return List of local events
     */
    @Query("SELECT * FROM events WHERE package_id IS NULL OR package_id = '' " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getLocalEvents();

    // ==================== ADVANCED CALENDAR FEATURES ====================

    /**
     * Get conflicting events (overlapping time ranges).
     * Useful for conflict detection when scheduling new events.
     * @param startTime Start time of new event
     * @param endTime End time of new event
     * @param excludeEventId Event ID to exclude (for updates)
     * @return List of conflicting events
     */
    @Query("SELECT * FROM events WHERE " +
            "id != :excludeEventId AND " +
            "start_time < :endTime AND end_time > :startTime " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getConflictingEvents(LocalDateTime startTime, LocalDateTime endTime, String excludeEventId);

    /**
     * Get all-day events for a specific date.
     * @param date Date to check for all-day events
     * @return List of all-day events
     */
    @Query("SELECT * FROM events WHERE all_day = 1 AND DATE(start_time) = DATE(:date) " +
            "ORDER BY title ASC")
    List<LocalEvent> getAllDayEventsForDate(LocalDateTime date);

    /**
     * Get timed events for a specific date.
     * @param startOfDay Start of the day
     * @param endOfDay End of the day
     * @return List of timed events
     */
    @Query("SELECT * FROM events WHERE all_day = 0 AND " +
            "start_time >= :startOfDay AND start_time <= :endOfDay " +
            "ORDER BY start_time ASC")
    List<LocalEvent> getTimedEventsForDate(LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * Get events that need updates (from external packages).
     * @return List of events that can be updated
     */
    @Query("SELECT * FROM events WHERE package_id IS NOT NULL AND package_id != '' " +
            "AND source_url IS NOT NULL AND source_url != '' " +
            "ORDER BY last_updated ASC")
    List<LocalEvent> getUpdatableEvents();

    // ==================== STATISTICS AND ANALYTICS ====================

    /**
     * Get event count by type.
     * @param eventType Event type to count
     * @return Number of events of that type
     */
    @Query("SELECT COUNT(*) FROM events WHERE event_type = :eventType")
    int getEventCountByType(EventType eventType);

    /**
     * Get event count by priority.
     * @param priority Priority level
     * @return Number of events with that priority
     */
    @Query("SELECT COUNT(*) FROM events WHERE priority = :priority")
    int getEventCountByPriority(EventPriority priority);

    /**
     * Get event count for date range.
     * @param startDate Range start
     * @param endDate Range end
     * @return Number of events in range
     */
    @Query("SELECT COUNT(*) FROM events WHERE " +
            "start_time <= :endDate AND end_time >= :startDate")
    int getEventCountInDateRange(LocalDateTime startDate, LocalDateTime endDate);
}