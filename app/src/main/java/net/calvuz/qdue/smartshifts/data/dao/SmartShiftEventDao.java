package net.calvuz.qdue.smartshifts.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;

import java.util.List;

/**
 * Data Access Object for SmartShiftEvent entity
 * Handles generated shift events for specific dates and users
 */
@Dao
public interface SmartShiftEventDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a new shift event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SmartShiftEvent event);

    /**
     * Insert multiple events (for bulk generation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SmartShiftEvent> events);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing event
     */
    @Update
    void update(SmartShiftEvent event);

    /**
     * Update event status
     */
    @Query("UPDATE smart_shift_events SET status = :status, updated_at = :updatedAt WHERE id = :id")
    void updateEventStatus(String id, String status, long updatedAt);

    /**
     * Update event times
     */
    @Query("UPDATE smart_shift_events SET start_time = :startTime, end_time = :endTime, updated_at = :updatedAt WHERE id = :id")
    void updateEventTimes(String id, String startTime, String endTime, long updatedAt);

    /**
     * Mark event as exception
     */
    @Query("UPDATE smart_shift_events SET event_type = 'exception', exception_reason = :reason, updated_at = :updatedAt WHERE id = :id")
    void markAsException(String id, String reason, long updatedAt);

    // ===== DELETE OPERATIONS =====

    /**
     * Hard delete an event
     */
    @Delete
    void delete(SmartShiftEvent event);

    /**
     * Delete events for a user in date range
     */
    @Query("DELETE FROM smart_shift_events WHERE user_id = :userId AND event_date BETWEEN :startDate AND :endDate")
    void deleteEventsInDateRange(String userId, String startDate, String endDate);

    /**
     * Delete all events for a pattern
     */
    @Query("DELETE FROM smart_shift_events WHERE shift_pattern_id = :patternId")
    void deleteEventsForPattern(String patternId);

    /**
     * Delete all events for a user
     */
    @Query("DELETE FROM smart_shift_events WHERE user_id = :userId")
    void deleteAllUserEvents(String userId);

    // ===== QUERY OPERATIONS =====

    /**
     * Get events for user in date range
     */
    @Query("SELECT * FROM smart_shift_events WHERE user_id = :userId AND event_date BETWEEN :startDate AND :endDate AND status = 'active' ORDER BY event_date ASC, start_time ASC")
    LiveData<List<SmartShiftEvent>> getEventsForUserInPeriod(String userId, String startDate, String endDate);

    /**
     * Get events for user in date range (synchronous)
     */
    @Query("SELECT * FROM smart_shift_events WHERE user_id = :userId AND event_date BETWEEN :startDate AND :endDate AND status = 'active' ORDER BY event_date ASC, start_time ASC")
    List<SmartShiftEvent> getEventsForUserInPeriodSync(String userId, String startDate, String endDate);

    /**
     * Get events for user on specific date
     */
    @Query("SELECT * FROM smart_shift_events WHERE user_id = :userId AND event_date = :date AND status = 'active' ORDER BY start_time ASC")
    LiveData<List<SmartShiftEvent>> getEventsForUserOnDate(String userId, String date);

    /**
     * Get events for user on specific date (synchronous)
     */
    @Query("SELECT * FROM smart_shift_events WHERE user_id = :userId AND event_date = :date AND status = 'active' ORDER BY start_time ASC")
    List<SmartShiftEvent> getEventsForUserOnDateSync(String userId, String date);

    /**
     * Get event by ID
     */
    @Query("SELECT * FROM smart_shift_events WHERE id = :id")
    SmartShiftEvent getEventById(String id);

    /**
     * Get event by ID (LiveData)
     */
    @Query("SELECT * FROM smart_shift_events WHERE id = :id")
    LiveData<SmartShiftEvent> getEventByIdLive(String id);

    /**
     * Get all master events for a pattern
     */
    @Query("SELECT * FROM smart_shift_events WHERE shift_pattern_id = :patternId AND event_type = 'master' AND status = 'active'")
    List<SmartShiftEvent> getMasterEventsForPattern(String patternId);

    /**
     * Get events by master event ID
     */
    @Query("SELECT * FROM smart_shift_events WHERE master_event_id = :masterEventId ORDER BY event_date ASC")
    List<SmartShiftEvent> getEventsByMasterId(String masterEventId);

    /**
     * Get exceptions for user in date range
     */
    @Query("SELECT * FROM smart_shift_events WHERE user_id = :userId AND event_type = 'exception' AND event_date BETWEEN :startDate AND :endDate ORDER BY event_date ASC")
    List<SmartShiftEvent> getExceptionsForUserInPeriod(String userId, String startDate, String endDate);

    /**
     * Count events for user in month
     */
    @Query("SELECT COUNT(*) FROM smart_shift_events WHERE user_id = :userId AND event_date LIKE :monthPattern AND status = 'active'")
    int countEventsForUserInMonth(String userId, String monthPattern);

    /**
     * Get latest event date for user
     */
    @Query("SELECT MAX(event_date) FROM smart_shift_events WHERE user_id = :userId AND status = 'active'")
    String getLatestEventDateForUser(String userId);

    /**
     * Get events count by shift type in period
     */
    @Query("SELECT shift_type_id, COUNT(*) as count FROM smart_shift_events WHERE user_id = :userId AND event_date BETWEEN :startDate AND :endDate AND status = 'active' GROUP BY shift_type_id")
    List<ShiftTypeCount> getShiftTypeCountsForPeriod(String userId, String startDate, String endDate);

    /**
     * Check if events exist for user and pattern
     */
    @Query("SELECT COUNT(*) FROM smart_shift_events WHERE user_id = :userId AND shift_pattern_id = :patternId AND status = 'active'")
    int countEventsForUserAndPattern(String userId, String patternId);

    // Helper class for shift type counts
    class ShiftTypeCount {
        public String shift_type_id;
        public int count;
    }
}

// =====================================================================

