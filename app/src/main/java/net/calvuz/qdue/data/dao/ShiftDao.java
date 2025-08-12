package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.data.entities.ShiftMinimalEntity;

import java.util.List;

/**
 * ShiftDao - Data Access Object for Shift operations.
 *
 * <p>Provides comprehensive database access methods for ShiftEntity operations using Room
 * persistence library. Includes CRUD operations, query methods, and batch operations
 * optimized for calendar-like shift management use cases.</p>
 *
 * <h3>Operation Categories:</h3>
 * <ul>
 *   <li><strong>Basic CRUD</strong>: Insert, update, delete, and select operations</li>
 *   <li><strong>Query Methods</strong>: Find shifts by various criteria</li>
 *   <li><strong>Batch Operations</strong>: Efficient multi-shift operations</li>
 *   <li><strong>Status Management</strong>: Active/inactive shift filtering</li>
 *   <li><strong>Type Management</strong>: Shift type classification queries</li>
 *   <li><strong>Utility Methods</strong>: Count, existence checks, cleanup</li>
 * </ul>
 *
 * <h3>Performance Features:</h3>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Leverages database indexes for fast lookups</li>
 *   <li><strong>Batch Operations</strong>: Efficient multiple-record processing</li>
 *   <li><strong>Conflict Resolution</strong>: Smart handling of duplicate entries</li>
 *   <li><strong>Soft Deletes</strong>: Mark shifts inactive instead of hard deletion</li>
 *   <li><strong>Time-based Queries</strong>: Optimized for calendar operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
@Dao
public interface ShiftDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Insert a new shift.
     *
     * @param shift Shift to insert
     * @return Row ID of inserted shift
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertShift(@NonNull ShiftEntity shift);

    /**
     * Insert multiple shifts in a single transaction.
     * More efficient than multiple single inserts.
     *
     * @param shifts Shifts to insert
     * @return Array of row IDs for inserted shifts
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long[] insertShifts(@NonNull ShiftEntity... shifts);

    /**
     * Insert multiple shifts from list.
     *
     * @param shifts List of shifts to insert
     * @return List of row IDs for inserted shifts
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertShifts(@NonNull List<ShiftEntity> shifts);

    /**
     * Insert shift with replace strategy for duplicates.
     * Useful when re-importing shifts or handling conflicts.
     *
     * @param shift Shift to insert or replace
     * @return Row ID of inserted/updated shift
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrReplaceShift(@NonNull ShiftEntity shift);

    /**
     * Update existing shift.
     *
     * @param shift Shift to update (must have valid ID)
     * @return Number of rows affected (should be 1 if successful)
     */
    @Update
    int updateShift(@NonNull ShiftEntity shift);

    /**
     * Update multiple shifts in a single transaction.
     *
     * @param shifts Shifts to update
     * @return Number of rows affected
     */
    @Update
    int updateShifts(@NonNull ShiftEntity... shifts);

    /**
     * Delete shift from database.
     * Note: Consider using markAsInactive() for soft delete instead.
     *
     * @param shift Shift to delete
     * @return Number of rows affected (should be 1 if successful)
     */
    @Delete
    int deleteShift(@NonNull ShiftEntity shift);

    /**
     * Delete multiple shifts.
     *
     * @param shifts Shifts to delete
     * @return Number of rows affected
     */
    @Delete
    int deleteShifts(@NonNull ShiftEntity... shifts);

    // ==================== QUERY METHODS ====================

    /**
     * Get all shifts ordered by display order and name.
     *
     * @return List of all shifts
     */
    @Query("SELECT * FROM shifts ORDER BY display_order ASC, name ASC")
    List<ShiftEntity> getAllShifts();

    /**
     * Get all active shifts only.
     * Most common query for operational use.
     *
     * @return List of active shifts
     */
    @Query("SELECT * FROM shifts WHERE active = 1 ORDER BY display_order ASC, name ASC")
    List<ShiftEntity> getActiveShifts();

    /**
     * Get all inactive shifts.
     * Useful for administration and historical views.
     *
     * @return List of inactive shifts
     */
    @Query("SELECT * FROM shifts WHERE active = 0 ORDER BY name ASC")
    List<ShiftEntity> getInactiveShifts();

    /**
     * Find shift by ID.
     *
     * @param id Shift ID
     * @return Shift entity or null if not found
     */
    @Query("SELECT * FROM shifts WHERE id = :id")
    @Nullable
    ShiftEntity getShiftById(@NonNull String id);

    /**
     * Find shift by name (case-insensitive).
     *
     * @param name Shift name to find
     * @return Shift entity or null if not found
     */
    @Query("SELECT * FROM shifts WHERE LOWER(name) = LOWER(:name)")
    @Nullable
    ShiftEntity getShiftByName(@NonNull String name);

    /**
     * Find shift by name among active shifts only.
     *
     * @param name Shift name to find
     * @return Active shift entity or null if not found
     */
    @Query("SELECT * FROM shifts WHERE LOWER(name) = LOWER(:name) AND active = 1")
    @Nullable
    ShiftEntity getActiveShiftByName(@NonNull String name);

    /**
     * Find shifts by name pattern (LIKE search).
     *
     * @param namePattern Pattern to search (use % for wildcards)
     * @return List of matching shifts
     */
    @Query("SELECT * FROM shifts WHERE name LIKE :namePattern ORDER BY name ASC")
    List<ShiftEntity> findShiftsByNamePattern(@NonNull String namePattern);

    /**
     * Get shifts by multiple names.
     * Useful for batch lookups.
     *
     * @param names List of shift names
     * @return List of matching shifts
     */
    @Query("SELECT * FROM shifts WHERE name IN (:names) ORDER BY display_order ASC, name ASC")
    List<ShiftEntity> getShiftsByNames(@NonNull List<String> names);

    /**
     * Get shifts by multiple IDs.
     *
     * @param ids List of shift IDs
     * @return List of matching shifts
     */
    @Query("SELECT * FROM shifts WHERE id IN (:ids) ORDER BY display_order ASC, name ASC")
    List<ShiftEntity> getShiftsByIds(@NonNull List<String> ids);

    // ==================== SHIFT TYPE QUERIES ====================

    /**
     * Get shifts by shift type.
     *
     * @param shiftType Shift type (MORNING, AFTERNOON, NIGHT, CUSTOM)
     * @return List of shifts of specified type
     */
    @Query("SELECT * FROM shifts WHERE shift_type = :shiftType AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getShiftsByType(@NonNull String shiftType);

    /**
     * Get morning shifts only.
     *
     * @return List of morning shifts
     */
    @Query("SELECT * FROM shifts WHERE shift_type = 'MORNING' AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getMorningShifts();

    /**
     * Get afternoon shifts only.
     *
     * @return List of afternoon shifts
     */
    @Query("SELECT * FROM shifts WHERE shift_type = 'AFTERNOON' AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getAfternoonShifts();

    /**
     * Get night shifts only.
     *
     * @return List of night shifts
     */
    @Query("SELECT * FROM shifts WHERE shift_type = 'NIGHT' AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getNightShifts();

    /**
     * Get custom user-defined shifts.
     *
     * @return List of custom shifts
     */
    @Query("SELECT * FROM shifts WHERE shift_type = 'CUSTOM' AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getCustomShifts();

    // ==================== TIME-BASED QUERIES ====================

    /**
     * Get shifts that cross midnight.
     *
     * @return List of shifts crossing midnight
     */
    @Query("SELECT * FROM shifts WHERE crosses_midnight = 1 AND active = 1 ORDER BY name ASC")
    List<ShiftEntity> getShiftsCrossingMidnight();

    /**
     * Get shifts with break time.
     *
     * @return List of shifts that include breaks
     */
    @Query("SELECT * FROM shifts WHERE has_break_time = 1 AND active = 1 ORDER BY name ASC")
    List<ShiftEntity> getShiftsWithBreaks();

    /**
     * Get shifts by time range (start time).
     *
     * @param startTime Start time in "HH:mm" format
     * @return List of shifts starting at specified time
     */
    @Query("SELECT * FROM shifts WHERE start_time = :startTime AND active = 1 ORDER BY name ASC")
    List<ShiftEntity> getShiftsByStartTime(@NonNull String startTime);

    /**
     * Get shifts by duration range.
     *
     * @param minDurationMinutes Minimum duration in minutes
     * @param maxDurationMinutes Maximum duration in minutes
     * @return List of shifts within duration range
     */
    @Query("SELECT * FROM shifts WHERE active = 1 AND " +
            "(CASE WHEN crosses_midnight = 1 " +
            "THEN (24 * 60) - (CAST(substr(start_time, 1, 2) AS INTEGER) * 60 + CAST(substr(start_time, 4, 2) AS INTEGER)) + " +
            "(CAST(substr(end_time, 1, 2) AS INTEGER) * 60 + CAST(substr(end_time, 4, 2) AS INTEGER)) " +
            "ELSE (CAST(substr(end_time, 1, 2) AS INTEGER) * 60 + CAST(substr(end_time, 4, 2) AS INTEGER)) - " +
            "(CAST(substr(start_time, 1, 2) AS INTEGER) * 60 + CAST(substr(start_time, 4, 2) AS INTEGER)) END) " +
            "BETWEEN :minDurationMinutes AND :maxDurationMinutes " +
            "ORDER BY name ASC")
    List<ShiftEntity> getShiftsByDurationRange(long minDurationMinutes, long maxDurationMinutes);

    // ==================== USER RELEVANCE QUERIES ====================

    /**
     * Get user-relevant shifts only.
     *
     * @return List of user-relevant shifts
     */
    @Query("SELECT * FROM shifts WHERE is_user_relevant = 1 AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getUserRelevantShifts();

    /**
     * Get shifts not relevant to user.
     *
     * @return List of non-user-relevant shifts
     */
    @Query("SELECT * FROM shifts WHERE is_user_relevant = 0 AND active = 1 ORDER BY display_order ASC")
    List<ShiftEntity> getNonUserRelevantShifts();

    // ==================== STATUS MANAGEMENT ====================

    /**
     * Mark shift as inactive by ID (soft delete).
     *
     * @param shiftId Shift ID to deactivate
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET active = 0, updated_at = :timestamp WHERE id = :shiftId")
    int markShiftAsInactive(@NonNull String shiftId, long timestamp);

    /**
     * Mark shift as active by ID.
     *
     * @param shiftId Shift ID to activate
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET active = 1, updated_at = :timestamp WHERE id = :shiftId")
    int markShiftAsActive(@NonNull String shiftId, long timestamp);

    /**
     * Mark shift as inactive by name.
     *
     * @param name Shift name to deactivate
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET active = 0, updated_at = :timestamp WHERE LOWER(name) = LOWER(:name)")
    int markShiftAsInactiveByName(@NonNull String name, long timestamp);

    /**
     * Mark all shifts as active.
     * Useful for bulk operations.
     *
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET active = 1, updated_at = :timestamp")
    int markAllShiftsAsActive(long timestamp);

    /**
     * Mark all shifts as inactive.
     * Use with caution - for major reorganizations only.
     *
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET active = 0, updated_at = :timestamp")
    int markAllShiftsAsInactive(long timestamp);

    /**
     * Toggle user relevance for shift.
     *
     * @param shiftId Shift ID to toggle
     * @param isUserRelevant New user relevance status
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET is_user_relevant = :isUserRelevant, updated_at = :timestamp WHERE id = :shiftId")
    int updateShiftUserRelevance(@NonNull String shiftId, boolean isUserRelevant, long timestamp);

    // ==================== UTILITY METHODS ====================

    /**
     * Count total shifts.
     *
     * @return Total number of shifts
     */
    @Query("SELECT COUNT(*) FROM shifts")
    int getShiftCount();

    /**
     * Count active shifts.
     *
     * @return Number of active shifts
     */
    @Query("SELECT COUNT(*) FROM shifts WHERE active = 1")
    int getActiveShiftCount();

    /**
     * Count inactive shifts.
     *
     * @return Number of inactive shifts
     */
    @Query("SELECT COUNT(*) FROM shifts WHERE active = 0")
    int getInactiveShiftCount();

    /**
     * Count shifts by type.
     *
     * @param shiftType Shift type to count
     * @return Number of shifts of specified type
     */
    @Query("SELECT COUNT(*) FROM shifts WHERE shift_type = :shiftType AND active = 1")
    int getShiftCountByType(@NonNull String shiftType);

    /**
     * Check if shift exists by name.
     *
     * @param name Shift name to check
     * @return true if shift exists
     */
    @Query("SELECT COUNT(*) > 0 FROM shifts WHERE LOWER(name) = LOWER(:name)")
    boolean shiftExistsByName(@NonNull String name);

    /**
     * Check if active shift exists by name.
     *
     * @param name Shift name to check
     * @return true if active shift exists
     */
    @Query("SELECT COUNT(*) > 0 FROM shifts WHERE LOWER(name) = LOWER(:name) AND active = 1")
    boolean activeShiftExistsByName(@NonNull String name);

    /**
     * Check if shift exists by ID.
     *
     * @param id Shift ID to check
     * @return true if shift exists
     */
    @Query("SELECT COUNT(*) > 0 FROM shifts WHERE id = :id")
    boolean shiftExistsById(@NonNull String id);

    /**
     * Get maximum display order value.
     * Useful for adding new shifts at the end.
     *
     * @return Maximum display order, or 0 if no shifts exist
     */
    @Query("SELECT COALESCE(MAX(display_order), 0) FROM shifts")
    int getMaxDisplayOrder();

    /**
     * Get shifts created after specific timestamp.
     *
     * @param timestamp Timestamp to filter by
     * @return List of recently created shifts
     */
    @Query("SELECT * FROM shifts WHERE created_at > :timestamp ORDER BY created_at DESC")
    List<ShiftEntity> getShiftsCreatedAfter(long timestamp);

    /**
     * Get shifts updated after specific timestamp.
     *
     * @param timestamp Timestamp to filter by
     * @return List of recently updated shifts
     */
    @Query("SELECT * FROM shifts WHERE updated_at > :timestamp ORDER BY updated_at DESC")
    List<ShiftEntity> getShiftsUpdatedAfter(long timestamp);

    // ==================== BATCH OPERATIONS ====================

    /**
     * Update display order for multiple shifts.
     * Useful for reordering shifts.
     *
     * @param shiftId Shift ID
     * @param displayOrder New display order
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET display_order = :displayOrder, updated_at = :timestamp WHERE id = :shiftId")
    int updateShiftDisplayOrder(@NonNull String shiftId, int displayOrder, long timestamp);

    /**
     * Bulk update shift names.
     * Advanced operation for data migration or corrections.
     *
     * @param oldName Current shift name
     * @param newName New shift name
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET name = :newName, updated_at = :timestamp WHERE LOWER(name) = LOWER(:oldName)")
    int renameShift(@NonNull String oldName, @NonNull String newName, long timestamp);

    /**
     * Update color for all shifts of a specific type.
     *
     * @param shiftType Shift type to update
     * @param colorHex New color in hex format
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET color_hex = :colorHex, updated_at = :timestamp WHERE shift_type = :shiftType")
    int updateShiftTypeColor(@NonNull String shiftType, @NonNull String colorHex, long timestamp);

    /**
     * Bulk update break time settings.
     *
     * @param hasBreakTime Whether shifts should have break time
     * @param isBreakTimeIncluded Whether break time is included in work duration
     * @param breakDurationMinutes Break duration in minutes
     * @return Number of rows affected
     */
    @Query("UPDATE shifts SET has_break_time = :hasBreakTime, " +
            "is_break_time_included = :isBreakTimeIncluded, " +
            "break_time_duration_minutes = :breakDurationMinutes, " +
            "updated_at = :timestamp " +
            "WHERE active = 1")
    int updateAllShiftsBreakSettings(boolean hasBreakTime, boolean isBreakTimeIncluded,
                                     long breakDurationMinutes, long timestamp);

    // ==================== CLEANUP OPERATIONS ====================

    /**
     * Delete all inactive shifts permanently.
     * Use with extreme caution - this is irreversible.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM shifts WHERE active = 0")
    int deleteAllInactiveShifts();

    /**
     * Delete shifts older than specified timestamp.
     *
     * @param timestamp Cutoff timestamp
     * @return Number of rows deleted
     */
    @Query("DELETE FROM shifts WHERE created_at < :timestamp")
    int deleteShiftsOlderThan(long timestamp);

    /**
     * Reset all shifts to default state.
     * Nuclear option for complete shift reset.
     *
     * @return Number of rows affected
     */
    @Query("DELETE FROM shifts")
    int deleteAllShifts();

    // ==================== SPECIALIZED QUERIES ====================

    /**
     * Get shift names only (for performance when full entities not needed).
     *
     * @return List of shift names
     */
    @Query("SELECT name FROM shifts WHERE active = 1 ORDER BY display_order ASC, name ASC")
    List<String> getActiveShiftNames();

    /**
     * Get active shifts for dropdown/selection purposes.
     *
     * @return List of active shifts optimized for UI selection
     */
    @Query("SELECT * FROM shifts WHERE active = 1 ORDER BY display_order ASC, name ASC")
    List<ShiftEntity> getActiveShiftsForSelection();

    /**
     * Get shift IDs and names for dropdown/selection purposes.
     * Returns minimal shift data optimized for UI components.
     *
     * @return List of shifts with minimal data
     */
    @Query("SELECT id, name, description, display_order FROM shifts WHERE active = 1 ORDER BY display_order ASC, name ASC")
    List<ShiftMinimalEntity> getActiveShiftsMinimal();

    /**
     * Get distinct shift types.
     *
     * @return List of unique shift types
     */
    @Query("SELECT DISTINCT shift_type FROM shifts WHERE active = 1 ORDER BY shift_type ASC")
    List<String> getDistinctShiftTypes();

    /**
     * Search shifts by description.
     *
     * @param searchTerm Search term to find in descriptions
     * @return List of shifts with matching descriptions
     */
    @Query("SELECT * FROM shifts WHERE description LIKE '%' || :searchTerm || '%' AND active = 1 ORDER BY name ASC")
    List<ShiftEntity> searchShiftsByDescription(@NonNull String searchTerm);

    /**
     * Get shift statistics summary.
     *
     * @return Statistics about shifts in database
     */
    @Query("SELECT " +
            "COUNT(*) as total_shifts, " +
            "SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_shifts, " +
            "SUM(CASE WHEN shift_type = 'MORNING' AND active = 1 THEN 1 ELSE 0 END) as morning_shifts, " +
            "SUM(CASE WHEN shift_type = 'AFTERNOON' AND active = 1 THEN 1 ELSE 0 END) as afternoon_shifts, " +
            "SUM(CASE WHEN shift_type = 'NIGHT' AND active = 1 THEN 1 ELSE 0 END) as night_shifts, " +
            "SUM(CASE WHEN shift_type = 'CUSTOM' AND active = 1 THEN 1 ELSE 0 END) as custom_shifts, " +
            "SUM(CASE WHEN has_break_time = 1 AND active = 1 THEN 1 ELSE 0 END) as shifts_with_breaks " +
            "FROM shifts")
    ShiftStatistics getShiftStatistics();

    /**
     * Statistics container for shift data.
     */
    class ShiftStatistics {
        public int total_shifts;
        public int active_shifts;
        public int morning_shifts;
        public int afternoon_shifts;
        public int night_shifts;
        public int custom_shifts;
        public int shifts_with_breaks;
    }
}