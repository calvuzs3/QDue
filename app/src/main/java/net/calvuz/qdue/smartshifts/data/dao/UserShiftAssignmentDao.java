package net.calvuz.qdue.smartshifts.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;

import java.util.List;

/**
 * Data Access Object for UserShiftAssignment entity
 * Handles user assignments to shift patterns with team information
 */
@Dao
public interface UserShiftAssignmentDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a new user assignment
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserShiftAssignment assignment);

    /**
     * Insert multiple assignments (for team setup)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserShiftAssignment> assignments);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing assignment
     */
    @Update
    void update(UserShiftAssignment assignment);

    /**
     * Deactivate all assignments for a user (when changing pattern)
     */
    @Query("UPDATE user_shift_assignments SET is_active = 0 WHERE user_id = :userId")
    void deactivateUserAssignments(String userId);

    /**
     * Update assignment start date
     */
    @Query("UPDATE user_shift_assignments SET start_date = :startDate WHERE id = :id")
    void updateStartDate(String id, String startDate);

    /**
     * Update team information
     */
    @Query("UPDATE user_shift_assignments SET team_name = :teamName, team_color_hex = :teamColorHex WHERE id = :id")
    void updateTeamInfo(String id, String teamName, String teamColorHex);

    // ===== DELETE OPERATIONS =====

    /**
     * Hard delete an assignment
     */
    @Delete
    void delete(UserShiftAssignment assignment);

    /**
     * Delete all assignments for a user
     */
    @Query("DELETE FROM user_shift_assignments WHERE user_id = :userId")
    void deleteAllUserAssignments(String userId);

    /**
     * Delete assignments for a specific pattern
     */
    @Query("DELETE FROM user_shift_assignments WHERE shift_pattern_id = :patternId")
    void deleteAssignmentsForPattern(String patternId);

    // ===== QUERY OPERATIONS =====

    /**
     * Get active assignment for a user
     */
    @Query("SELECT * FROM user_shift_assignments WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    UserShiftAssignment getActiveAssignmentForUser(String userId);

    /**
     * Get active assignment for a user (LiveData)
     */
    @Query("SELECT * FROM user_shift_assignments WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    LiveData<UserShiftAssignment> getActiveAssignmentForUserLive(String userId);

    /**
     * Get all assignments for a user (including inactive)
     */
    @Query("SELECT * FROM user_shift_assignments WHERE user_id = :userId ORDER BY assigned_at DESC")
    LiveData<List<UserShiftAssignment>> getAllAssignmentsForUser(String userId);

    /**
     * Get all assignments for a pattern
     */
    @Query("SELECT * FROM user_shift_assignments WHERE shift_pattern_id = :patternId AND is_active = 1")
    List<UserShiftAssignment> getAssignmentsForPattern(String patternId);

    /**
     * Get all active assignments
     */
    @Query("SELECT * FROM user_shift_assignments WHERE is_active = 1 ORDER BY assigned_at DESC")
    LiveData<List<UserShiftAssignment>> getAllActiveAssignments();

    /**
     * Check if user has any active assignment
     */
    @Query("SELECT COUNT(*) FROM user_shift_assignments WHERE user_id = :userId AND is_active = 1")
    int hasActiveAssignment(String userId);

    /**
     * Check if user has any active assignment (LiveData)
     */
    @Query("SELECT COUNT(*) > 0 FROM user_shift_assignments WHERE user_id = :userId AND is_active = 1")
    LiveData<Boolean> hasActiveAssignmentLive(String userId);

    /**
     * Get assignments by team name pattern
     */
    @Query("SELECT * FROM user_shift_assignments WHERE team_name LIKE :teamNamePattern AND is_active = 1")
    List<UserShiftAssignment> getAssignmentsByTeamPattern(String teamNamePattern);

    /**
     * Get assignment by ID
     */
    @Query("SELECT * FROM user_shift_assignments WHERE id = :id")
    UserShiftAssignment getAssignmentById(String id);
}

// =====================================================================

