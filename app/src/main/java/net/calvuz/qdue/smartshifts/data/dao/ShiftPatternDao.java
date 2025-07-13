package net.calvuz.qdue.smartshifts.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;

import java.util.List;

/**
 * Data Access Object for ShiftPattern entity
 * Handles CRUD operations for shift patterns (Continuous 4-2, Weekly 5-2, Custom patterns, etc.)
 */
@Dao
public interface ShiftPatternDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a new shift pattern
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShiftPattern shiftPattern);

    /**
     * Insert multiple shift patterns (for initial setup)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShiftPattern> shiftPatterns);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing shift pattern
     */
    @Update
    void update(ShiftPattern shiftPattern);

    /**
     * Update pattern details
     */
    @Query("UPDATE shift_patterns SET name = :name, description = :description, " +
            "recurrence_rule_json = :recurrenceRuleJson, is_continuous_cycle = :isContinuousCycle, " +
            "updated_at = :updatedAt WHERE id = :id")
    void updatePatternDetails(String id, String name, String description,
                              String recurrenceRuleJson, boolean isContinuousCycle, long updatedAt);

    /**
     * Soft delete - mark as inactive
     */
    @Query("UPDATE shift_patterns SET is_active = 0, updated_at = :updatedAt WHERE id = :id")
    void softDelete(String id, long updatedAt);

    /**
     * Reactivate a soft-deleted pattern
     */
    @Query("UPDATE shift_patterns SET is_active = 1, updated_at = :updatedAt WHERE id = :id")
    void reactivate(String id, long updatedAt);

    // ===== DELETE OPERATIONS =====

    /**
     * Hard delete a shift pattern (use with caution)
     */
    @Delete
    void delete(ShiftPattern shiftPattern);

    /**
     * Delete all custom patterns for a specific user
     */
    @Query("DELETE FROM shift_patterns WHERE created_by_user_id = :userId AND is_predefined = 0")
    void deleteCustomPatternsForUser(String userId);

    // ===== QUERY OPERATIONS =====

    /**
     * Get all active shift patterns
     */
    @Query("SELECT * FROM shift_patterns WHERE is_active = 1 ORDER BY is_predefined DESC, name ASC")
    LiveData<List<ShiftPattern>> getAllActivePatterns();

    /**
     * Get all active patterns (synchronous)
     */
    @Query("SELECT * FROM shift_patterns WHERE is_active = 1 ORDER BY is_predefined DESC, name ASC")
    List<ShiftPattern> getAllActivePatternsSync();

    /**
     * Get shift pattern by ID
     */
    @Query("SELECT * FROM shift_patterns WHERE id = :id")
    ShiftPattern getPatternById(String id);

    /**
     * Get shift pattern by ID (LiveData)
     */
    @Query("SELECT * FROM shift_patterns WHERE id = :id")
    LiveData<ShiftPattern> getPatternByIdLive(String id);

    /**
     * Get all predefined patterns
     */
    @Query("SELECT * FROM shift_patterns WHERE is_predefined = 1 AND is_active = 1 ORDER BY name ASC")
    LiveData<List<ShiftPattern>> getPredefinedPatterns();

    /**
     * Get all predefined patterns (synchronous)
     */
    @Query("SELECT * FROM shift_patterns WHERE is_predefined = 1 AND is_active = 1 ORDER BY name ASC")
    List<ShiftPattern> getPredefinedPatternsSync();

    /**
     * Get custom patterns for a specific user
     */
    @Query("SELECT * FROM shift_patterns WHERE created_by_user_id = :userId AND is_predefined = 0 AND is_active = 1 ORDER BY created_at DESC")
    LiveData<List<ShiftPattern>> getCustomPatternsForUser(String userId);

    /**
     * Get continuous cycle compatible patterns
     */
    @Query("SELECT * FROM shift_patterns WHERE is_continuous_cycle = 1 AND is_active = 1 ORDER BY is_predefined DESC, name ASC")
    List<ShiftPattern> getContinuousCyclePatterns();

    /**
     * Check if pattern name already exists for user
     */
    @Query("SELECT COUNT(*) FROM shift_patterns WHERE name = :name AND created_by_user_id = :userId AND is_active = 1 AND id != :excludeId")
    int countPatternsByNameForUser(String name, String userId, String excludeId);

    /**
     * Check if any patterns exist
     */
    @Query("SELECT COUNT(*) FROM shift_patterns WHERE is_active = 1")
    int getActivePatternsCount();

    /**
     * Get patterns with specific cycle length
     */
    @Query("SELECT * FROM shift_patterns WHERE cycle_length_days = :cycleDays AND is_active = 1")
    List<ShiftPattern> getPatternsByCycleLength(int cycleDays);
}

// =====================================================================

