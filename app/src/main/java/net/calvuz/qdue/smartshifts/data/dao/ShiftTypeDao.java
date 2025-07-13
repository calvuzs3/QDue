package net.calvuz.qdue.smartshifts.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.smartshifts.data.entities.ShiftType;

import java.util.List;

/**
 * Data Access Object for ShiftType entity
 * Handles CRUD operations for shift types (Morning, Afternoon, Night, Rest, etc.)
 */
@Dao
public interface ShiftTypeDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a new shift type
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShiftType shiftType);

    /**
     * Insert multiple shift types (for initial setup)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShiftType> shiftTypes);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing shift type
     */
    @Update
    void update(ShiftType shiftType);

    /**
     * Update shift type name and times
     */
    @Query("UPDATE shift_types SET name = :name, start_time = :startTime, " +
            "end_time = :endTime, updated_at = :updatedAt WHERE id = :id")
    void updateShiftDetails(String id, String name, String startTime, String endTime, long updatedAt);

    /**
     * Soft delete - mark as inactive
     */
    @Query("UPDATE shift_types SET is_active = 0, updated_at = :updatedAt WHERE id = :id")
    void softDelete(String id, long updatedAt);

    /**
     * Reactivate a soft-deleted shift type
     */
    @Query("UPDATE shift_types SET is_active = 1, updated_at = :updatedAt WHERE id = :id")
    void reactivate(String id, long updatedAt);

    // ===== DELETE OPERATIONS =====

    /**
     * Hard delete a shift type (use with caution)
     */
    @Delete
    void delete(ShiftType shiftType);

    /**
     * Delete all custom shift types (keep predefined ones)
     */
    @Query("DELETE FROM shift_types WHERE is_custom = 1")
    void deleteAllCustomShiftTypes();

    // ===== QUERY OPERATIONS =====

    /**
     * Get all active shift types ordered by sort order
     */
    @Query("SELECT * FROM shift_types WHERE is_active = 1 ORDER BY sort_order ASC")
    LiveData<List<ShiftType>> getAllActiveShiftTypes();

    /**
     * Get all active shift types (synchronous)
     */
    @Query("SELECT * FROM shift_types WHERE is_active = 1 ORDER BY sort_order ASC")
    List<ShiftType> getAllActiveShiftTypesSync();

    /**
     * Get shift type by ID
     */
    @Query("SELECT * FROM shift_types WHERE id = :id")
    ShiftType getShiftTypeById(String id);

    /**
     * Get shift type by ID (LiveData)
     */
    @Query("SELECT * FROM shift_types WHERE id = :id")
    LiveData<ShiftType> getShiftTypeByIdLive(String id);

    /**
     * Get all predefined shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_custom = 0 AND is_active = 1 ORDER BY sort_order ASC")
    List<ShiftType> getPredefinedShiftTypes();

    /**
     * Get all custom (user-created) shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_custom = 1 AND is_active = 1 ORDER BY sort_order ASC")
    LiveData<List<ShiftType>> getCustomShiftTypes();

    /**
     * Get only working shift types (exclude Rest)
     */
    @Query("SELECT * FROM shift_types WHERE is_working_shift = 1 AND is_active = 1 ORDER BY sort_order ASC")
    List<ShiftType> getWorkingShiftTypes();

    /**
     * Check if shift type name already exists
     */
    @Query("SELECT COUNT(*) FROM shift_types WHERE name = :name AND is_active = 1 AND id != :excludeId")
    int countShiftTypesByName(String name, String excludeId);

    /**
     * Get next sort order for new shift type
     */
    @Query("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM shift_types")
    int getNextSortOrder();

    /**
     * Check if any shift types exist
     */
    @Query("SELECT COUNT(*) FROM shift_types WHERE is_active = 1")
    int getActiveShiftTypesCount();

    /**
     * Get shift types for time period validation
     */
    @Query("SELECT * FROM shift_types WHERE is_working_shift = 1 AND is_active = 1")
    List<ShiftType> getShiftTypesForValidation();
}

// =====================================================================

