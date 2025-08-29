package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.RecurrenceRuleEntity;

import java.util.List;

/**
 * RecurrenceRuleDao - Data Access Object for recurrence patterns.
 *
 * <p>Optimized for pattern lookup, frequency filtering, and date range queries.
 * Supports both simple CRUD operations and complex pattern matching queries.</p>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Calendar Engine Database
 * @since Clean architecture
 */
@Dao
public interface RecurrenceRuleDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRecurrenceRule(@NonNull RecurrenceRuleEntity rule);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertRecurrenceRules(@NonNull List<RecurrenceRuleEntity> rules);

    @Update
    int updateRecurrenceRule(@NonNull RecurrenceRuleEntity rule);

    @Delete
    int deleteRecurrenceRule(@NonNull RecurrenceRuleEntity rule);

    @Query("DELETE FROM recurrence_rules WHERE id = :ruleId")
    int deleteRecurrenceRuleById(@NonNull String ruleId);

    @Query("DELETE FROM recurrence_rules WHERE active = 0 AND updated_at < :cutoffTime")
    int deleteInactiveRulesOlderThan(long cutoffTime);

    // ==================== SINGLE RECORD QUERIES ====================

    @Query("SELECT * FROM recurrence_rules WHERE id = :ruleId AND active = 1 LIMIT 1")
    @Nullable
    RecurrenceRuleEntity getRecurrenceRuleById(@NonNull String ruleId);

    @Query("SELECT * FROM recurrence_rules WHERE name = :name AND active = 1 LIMIT 1")
    @Nullable
    RecurrenceRuleEntity getRecurrenceRuleByName(@NonNull String name);

    // ==================== LIST QUERIES ====================

    @Query("SELECT * FROM recurrence_rules ORDER BY created_at DESC")
    @NonNull
    List<RecurrenceRuleEntity> getAllRecurrenceRules();

    @Query("SELECT * FROM recurrence_rules WHERE active = 1 ORDER BY created_at DESC")
    @NonNull
    List<RecurrenceRuleEntity> getActiveUserRecurrenceRules();

    @Query("SELECT * FROM recurrence_rules WHERE active = 1 ORDER BY created_at DESC")
    @NonNull
    List<RecurrenceRuleEntity> getAllActiveRecurrenceRules();

    @Query("SELECT * FROM recurrence_rules WHERE frequency = :frequency AND active = 1 ORDER BY start_date")
    @NonNull
    List<RecurrenceRuleEntity> getRecurrenceRulesByFrequency(@NonNull String frequency);

    @Query("SELECT * FROM recurrence_rules WHERE frequency = 'QUATTRODUE_CYCLE' AND active = 1 ORDER BY start_date")
    @NonNull
    List<RecurrenceRuleEntity> getQuattroDueRecurrenceRules();

    @Query("SELECT * FROM recurrence_rules WHERE start_date <= :date AND (end_date IS NULL OR end_date >= :date) AND active = 1")
    @NonNull
    List<RecurrenceRuleEntity> getActiveRecurrenceRulesForDate(@NonNull String date);

    @Query("SELECT * FROM recurrence_rules WHERE start_date >= :startDate AND start_date <= :endDate AND active = 1 ORDER BY start_date")
    @NonNull
    List<RecurrenceRuleEntity> getRecurrenceRulesInDateRange(@NonNull String startDate, @NonNull String endDate);

    // ==================== BUSINESS QUERIES ====================

    @Query("SELECT COUNT(*) FROM recurrence_rules WHERE active = 1")
    int getActiveRecurrenceRuleCount();

    @Query("SELECT COUNT(*) FROM recurrence_rules WHERE frequency = :frequency AND active = 1")
    int getRecurrenceRuleCountByFrequency(@NonNull String frequency);

    @Query("UPDATE recurrence_rules SET active = 0, updated_at = :timestamp WHERE id = :ruleId")
    int deactivateRecurrenceRule(@NonNull String ruleId, long timestamp);

    @Query("UPDATE recurrence_rules SET updated_at = :timestamp WHERE id = :ruleId")
    int updateTimestamp(@NonNull String ruleId, long timestamp);

    // ==================== NEW METHODS FOR BACKUP SUPPORT ====================

     /**
      * ✅ NEW: Get all recurrence rules with status filter
      */
     @Query("SELECT * FROM recurrence_rules WHERE active = :active ORDER BY created_at DESC")
     @NonNull
     List<RecurrenceRuleEntity> getAllRecurrenceRulesByStatus(boolean active);

     /**
      * ✅ NEW: Get total count for statistics
      */
     @Query("SELECT COUNT(*) FROM recurrence_rules")
     int getTotalRecurrenceRuleCount();

    // ==================== STATISTICS QUERIES ====================

    @Query("""
        SELECT 
            COUNT(*) as total_rules,
            SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_rules,
            SUM(CASE WHEN frequency = 'QUATTRODUE_CYCLE' THEN 1 ELSE 0 END) as quattrodue_rules,
            SUM(CASE WHEN frequency = 'WEEKLY' THEN 1 ELSE 0 END) as weekly_rules,
            SUM(CASE WHEN frequency = 'DAILY' THEN 1 ELSE 0 END) as daily_rules
        FROM recurrence_rules
        """)
    @Nullable
    RecurrenceRuleStatistics getRecurrenceRuleStatistics();

    /**
     * Statistics container for recurrence rules.
     */
    class RecurrenceRuleStatistics {
        public int total_rules;
        public int active_rules;
        public int quattrodue_rules;
        public int weekly_rules;
        public int daily_rules;
    }
}
