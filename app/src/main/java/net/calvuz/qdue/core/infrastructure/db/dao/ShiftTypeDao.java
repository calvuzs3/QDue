
package net.calvuz.qdue.core.infrastructure.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity;

import java.util.List;

/**
 * DAO for ShiftType operations.
 *
 * <p>Provides database access methods for shift type management including
 * CRUD operations, querying, and batch operations. Supports both predefined
 * and user-defined shift types with proper validation and constraints.</p>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
@Dao
public interface ShiftTypeDao {

    // ==================== QUERY OPERATIONS ====================

    /**
     * Gets all active shift types ordered by name.
     *
     * @return list of active shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_active = 1 ORDER BY name ASC")
    List<ShiftTypeEntity> getAllActiveShiftTypes();

    /**
     * Gets all shift types (including inactive) ordered by name.
     *
     * @return list of all shift types
     */
    @Query("SELECT * FROM shift_types ORDER BY name ASC")
    List<ShiftTypeEntity> getAllShiftTypes();

    /**
     * Gets a shift type by its ID.
     *
     * @param id shift type ID
     * @return shift type entity or null if not found
     */
    @Query("SELECT * FROM shift_types WHERE id = :id")
    ShiftTypeEntity getShiftTypeById(long id);

    /**
     * Gets a shift type by its name.
     *
     * @param name shift type name (case insensitive)
     * @return shift type entity or null if not found
     */
    @Query("SELECT * FROM shift_types WHERE name = :name LIMIT 1")
    ShiftTypeEntity getShiftTypeByName(String name);

    /**
     * Gets all predefined (system) shift types.
     *
     * @return list of predefined shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_user_defined = 0 AND is_active = 1 ORDER BY name ASC")
    List<ShiftTypeEntity> getPredefinedShiftTypes();

    /**
     * Gets all user-defined shift types.
     *
     * @return list of user-defined shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_user_defined = 1 AND is_active = 1 ORDER BY name ASC")
    List<ShiftTypeEntity> getUserDefinedShiftTypes();

    /**
     * Gets all rest period shift types.
     *
     * @return list of rest period shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_rest_period = 1 AND is_active = 1 ORDER BY name ASC")
    List<ShiftTypeEntity> getRestPeriodShiftTypes();

    /**
     * Gets all work shift types (non-rest periods).
     *
     * @return list of work shift types
     */
    @Query("SELECT * FROM shift_types WHERE is_rest_period = 0 AND is_active = 1 ORDER BY name ASC")
    List<ShiftTypeEntity> getWorkShiftTypes();

    // ==================== INSERT OPERATIONS ====================

    /**
     * Inserts a new shift type.
     *
     * @param shiftType shift type to insert
     * @return ID of the inserted shift type
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertShiftType(ShiftTypeEntity shiftType);

    /**
     * Inserts multiple shift types in a batch.
     *
     * @param shiftTypes list of shift types to insert
     * @return list of IDs of inserted shift types
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertShiftTypes(List<ShiftTypeEntity> shiftTypes);

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Updates an existing shift type.
     *
     * @param shiftType shift type to update
     */
    @Update
    void updateShiftType(ShiftTypeEntity shiftType);

    /**
     * Updates multiple shift types in a batch.
     *
     * @param shiftTypes list of shift types to update
     */
    @Update
    void updateShiftTypes(List<ShiftTypeEntity> shiftTypes);

    /**
     * Deactivates a shift type (soft delete).
     *
     * @param id shift type ID to deactivate
     */
    @Query("UPDATE shift_types SET is_active = 0, updated_at = datetime('now', 'localtime') WHERE id = :id")
    void deactivateShiftType(long id);

    /**
     * Reactivates a previously deactivated shift type.
     *
     * @param id shift type ID to reactivate
     */
    @Query("UPDATE shift_types SET is_active = 1, updated_at = datetime('now', 'localtime') WHERE id = :id")
    void reactivateShiftType(long id);

    // ==================== DELETE OPERATIONS ====================

    /**
     * Permanently deletes a shift type.
     * WARNING: This should only be used for cleanup operations.
     * Use deactivateShiftType() for normal operations.
     *
     * @param shiftType shift type to delete
     */
    @Delete
    void deleteShiftType(ShiftTypeEntity shiftType);

    /**
     * Permanently deletes a shift type by ID.
     * WARNING: This should only be used for cleanup operations.
     * Use deactivateShiftType() for normal operations.
     *
     * @param id shift type ID to delete
     */
    @Query("DELETE FROM shift_types WHERE id = :id")
    void deleteShiftTypeById(long id);

    /**
     * Permanently deletes all shift types.
     * WARNING: This should only be used for testing or reset operations.
     *
     */
    @Query("DELETE FROM shift_types")
    void deleteAllShiftTypes();

    // ==================== VALIDATION AND COUNT OPERATIONS ====================

    /**
     * Checks if a shift type with the given name exists.
     *
     * @param name shift type name to check
     * @return true if exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM shift_types WHERE name = :name)")
    boolean existsByName(String name);

    /**
     * Checks if a shift type with the given name exists (excluding a specific ID).
     * Useful for update operations to avoid self-conflict.
     *
     * @param name shift type name to check
     * @param excludeId ID to exclude from the check
     * @return true if exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM shift_types WHERE name = :name AND id != :excludeId)")
    boolean existsByNameExcludingId(String name, long excludeId);

    /**
     * Gets the total count of active shift types.
     *
     * @return count of active shift types
     */
    @Query("SELECT COUNT(*) FROM shift_types WHERE is_active = 1")
    int getActiveShiftTypeCount();

    /**
     * Gets the count of user-defined shift types.
     *
     * @return count of user-defined shift types
     */
    @Query("SELECT COUNT(*) FROM shift_types WHERE is_user_defined = 1 AND is_active = 1")
    int getUserDefinedShiftTypeCount();

    /**
     * Gets the count of predefined shift types.
     *
     * @return count of predefined shift types
     */
    @Query("SELECT COUNT(*) FROM shift_types WHERE is_user_defined = 0 AND is_active = 1")
    int getPredefinedShiftTypeCount();
}