package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.LocalEventEntity;
import net.calvuz.qdue.domain.calendar.enums.EventType;

import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface LocalEventDao
{
    // ==================== CRUD OPERATIONS ====================
    /**
     * Insert new LocalEvent entity.
     */
    @Insert (onConflict = OnConflictStrategy.ABORT)
    long insertLocalEvent(@NonNull LocalEventEntity localEventEntity);

    /**
     * Update existing QDueUser entity.
     * @return Number of rows updated (1 if successful)
     */
    @Update (onConflict = OnConflictStrategy.REPLACE)
    int updateLocalEvent(@NonNull LocalEventEntity localEventEntity);

    /**
     * Delete LocalEvent entity.
     * @return Number of rows deleted (1 if successful)
     */
    @Delete
    int deleteLocalEvent(@NonNull LocalEventEntity localEventEntity);

    /**
     * Delete all LocalEvents entities.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM local_events")
    int deleteAllLocalEvents();

    /**
     * Delete event by ID.
     * @param eventId ID of event to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM local_events WHERE id = :eventId")
    int deleteEventById(String eventId);

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Get event by ID.
     * @param eventId Event ID to search for
     * @return Event if found, null otherwise
     */
    @Query("SELECT * FROM local_events WHERE id = :eventId LIMIT 1")
    LocalEventEntity getEventById(String eventId);

    /**
     * Get all events ordered by start time.
     * @return List of all events
     */
    @Query("SELECT * FROM local_events ORDER BY start_time ASC")
    List<LocalEventEntity> getAllEvents();

    /**
     * Get upcoming events from current time.
     * @param currentTime Current timestamp
     * @return Number of upcoming events
     */
    @Query("SELECT COUNT(*) FROM local_events WHERE start_time >= :currentTime ")
    int getUpcomingEventsCount(LocalDateTime currentTime);

    /**
     * Get total count of events.
     * @return Total number of events in database
     */
    @Query("SELECT COUNT(*) FROM local_events")
    int getTotalEventCount();

    // ==================== CALENDAR-SPECIFIC QUERIES ====================

    /**
     * Get all events for a specific date (both all-day and timed events).
     * @param startOfDay Start of the day (00:00:00)
     * @param endOfDay End of the day (23:59:59)
     * @return List of events for that date
     */
    @Query("SELECT * FROM local_events WHERE " +
            "(DATE(start_time) = DATE(:startOfDay)) OR " +
            "(start_time <= :endOfDay AND end_time >= :startOfDay) " +
            "ORDER BY all_day DESC, start_time ASC")
    List<LocalEventEntity> getEventsForDate(LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * Get events within a date range (for calendar views).
     * @param startDate Range start date
     * @param endDate Range end date
     * @return List of events in the specified range
     */
    @Query("SELECT * FROM local_events WHERE " +
            "start_time <= :endDate AND end_time >= :startDate " +
            "ORDER BY start_time ASC")
    List<LocalEventEntity> getEventsForDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get upcoming events from current time.
     * @param currentTime Current timestamp
     * @param limit Maximum number of events to return
     * @return List of upcoming events
     */
    @Query("SELECT * FROM local_events WHERE start_time >= :currentTime " +
            "ORDER BY start_time ASC LIMIT :limit")
    List<LocalEventEntity> getUpcomingEvents(LocalDateTime currentTime, int limit);

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Search events by title or description.
     * @param query Search query
     * @return List of matching events
     */
    @Query("SELECT * FROM local_events WHERE " +
            "title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' " +
            "ORDER BY start_time ASC")
    List<LocalEventEntity> searchEvents(String query);

    /**
     * Get events by type.
     * @param eventType Type of events to retrieve
     * @return List of events of specified type
     */
    @Query("SELECT * FROM local_events WHERE event_type = :eventType ORDER BY start_time ASC")
    List<LocalEventEntity> getEventsByType(EventType eventType);
}
