package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.UserTeamAssignmentEntity;
import net.calvuz.qdue.domain.common.enums.Status;

import java.time.LocalDate;
import java.util.List;

/**
 * UserTeamAssignmentDao - Room Data Access Object for UserTeamAssignment operations
 *
 * <p>Provides comprehensive database access methods for user-team assignment management
 * following clean architecture patterns and optimized for calendar scheduling operations.</p>
 *
 * <h3>Query Categories:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Basic create, read, update, delete operations</li>
 *   <li><strong>User Queries</strong>: Find assignments by user ID with various filters</li>
 *   <li><strong>Team Queries</strong>: Find assignments by team ID with date/status filters</li>
 *   <li><strong>Date Range Queries</strong>: Time-based assignment lookups for calendar</li>
 *   <li><strong>Status Queries</strong>: Active/inactive and computed status filtering</li>
 *   <li><strong>Management Queries</strong>: Administrative operations and reporting</li>
 * </ul>
 *
 * <h3>Performance Optimizations:</h3>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: All queries use database indexes for fast execution</li>
 *   <li><strong>Efficient Filtering</strong>: Combined WHERE clauses minimize result sets</li>
 *   <li><strong>Date Optimizations</strong>: Smart date range queries for calendar operations</li>
 *   <li><strong>Batch Operations</strong>: Support for bulk inserts and updates</li>
 * </ul>
 */
@Dao
public interface UserTeamAssignmentDao {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Insert new user team assignment.
     *
     * @param assignment Assignment to insert
     * @return Row ID of inserted assignment
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(@NonNull UserTeamAssignmentEntity assignment);

    /**
     * Insert multiple assignments in single transaction.
     *
     * @param assignments List of assignments to insert
     * @return Array of row IDs for inserted assignments
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(@NonNull List<UserTeamAssignmentEntity> assignments);

    /**
     * Update existing assignment.
     *
     * @param assignment Assignment to update (ID must exist)
     * @return Number of rows updated (should be 1)
     */
    @Update
    int update(@NonNull UserTeamAssignmentEntity assignment);

    /**
     * Update multiple assignments in single transaction.
     *
     * @param assignments List of assignments to update
     * @return Number of rows updated
     */
    @Update
    int updateAll(@NonNull List<UserTeamAssignmentEntity> assignments);

    /**
     * Delete assignment.
     *
     * @param assignment Assignment to delete
     * @return Number of rows deleted (should be 1)
     */
    @Delete
    int delete(@NonNull UserTeamAssignmentEntity assignment);

    /**
     * Delete assignment by ID.
     *
     * @param assignmentId Assignment ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM user_team_assignments WHERE id = :assignmentId")
    int deleteById(@NonNull String assignmentId);

    /**
     * Soft delete assignment (set active = false).
     *
     * @param assignmentId Assignment ID to deactivate
     * @param lastModifiedByUserId User performing the deactivation
     * @return Number of rows updated
     */
    @Query("UPDATE user_team_assignments SET active = 0, " +
            "updated_at = :timestamp, last_modified_by_user_id = :lastModifiedByUserId " +
            "WHERE id = :assignmentId")
    int softDelete(@NonNull String assignmentId,
                   @Nullable String lastModifiedByUserId,
                   long timestamp);

    // ==================== BASIC RETRIEVAL ====================

    /**
     * Get assignment by ID.
     *
     * @param assignmentId Assignment ID to find
     * @return Assignment entity or null if not found
     */
    @Query("SELECT * FROM user_team_assignments WHERE id = :assignmentId")
    @Nullable
    UserTeamAssignmentEntity getById(@NonNull String assignmentId);

    /**
     * Get multiple assignments by IDs.
     *
     * @param assignmentIds List of assignment IDs to find
     * @return List of found assignments (may be less than requested)
     */
    @Query("SELECT * FROM user_team_assignments WHERE id IN (:assignmentIds)")
    @NonNull
    List<UserTeamAssignmentEntity> getByIds(@NonNull List<String> assignmentIds);

    /**
     * Get all assignments.
     *
     * @return List of all assignments
     */
    @Query("SELECT * FROM user_team_assignments ORDER BY created_at DESC")
    @NonNull
    List<UserTeamAssignmentEntity> getAll();

    /**
     * Get all active assignments.
     *
     * @return List of active assignments
     */
    @Query("SELECT * FROM user_team_assignments WHERE active = 1 ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getAllActive();

    /**
     * Count total assignments.
     *
     * @return Total number of assignments
     */
    @Query("SELECT COUNT(*) FROM user_team_assignments")
    int getCount();

    /**
     * Count active assignments.
     *
     * @return Number of active assignments
     */
    @Query("SELECT COUNT(*) FROM user_team_assignments WHERE active = 1")
    int getActiveCount();

    // ==================== USER-BASED QUERIES ====================

    /**
     * Get all assignments for specific user.
     *
     * @param userId User ID to find assignments for
     * @return List of user's assignments ordered by start date
     */
    @Query("SELECT * FROM user_team_assignments WHERE user_id = :userId " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByUserId(@NonNull String userId);

    /**
     * Get active assignments for specific user.
     *
     * @param userId User ID to find assignments for
     * @return List of user's active assignments
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE user_id = :userId AND active = 1 " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getActiveByUserId(@NonNull String userId);

    /**
     * Get current assignments for user (active on specific date).
     *
     * @param userId User ID to check
     * @param date Date to check assignments for
     * @return List of assignments active on the specified date
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE user_id = :userId AND active = 1 " +
            "AND start_date <= :date " +
            "AND (end_date IS NULL OR end_date >= :date)")
    @NonNull
    List<UserTeamAssignmentEntity> getCurrentAssignmentsByUserId(@NonNull String userId,
                                                                 @NonNull LocalDate date);

    /**
     * Get assignments for user within date range.
     *
     * @param userId User ID to find assignments for
     * @param startDate Start date of range (inclusive)
     * @param endDate End date of range (inclusive)
     * @return List of assignments overlapping with date range
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE user_id = :userId AND active = 1 " +
            "AND start_date <= :endDate " +
            "AND (end_date IS NULL OR end_date >= :startDate) " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByUserIdAndDateRange(@NonNull String userId,
                                                           @NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate);

    // ==================== TEAM-BASED QUERIES ====================

    /**
     * Get all assignments for specific team.
     *
     * @param teamId Team ID to find assignments for
     * @return List of team assignments ordered by start date
     */
    @Query("SELECT * FROM user_team_assignments WHERE team_id = :teamId " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByTeamId(@NonNull String teamId);

    /**
     * Get active assignments for specific team.
     *
     * @param teamId Team ID to find assignments for
     * @return List of active team assignments
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE team_id = :teamId AND active = 1 " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getActiveByTeamId(@NonNull String teamId);

    /**
     * Get current team members (active on specific date).
     *
     * @param teamId Team ID to get members for
     * @param date Date to check assignments for
     * @return List of assignments active on the specified date
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE team_id = :teamId AND active = 1 " +
            "AND start_date <= :date " +
            "AND (end_date IS NULL OR end_date >= :date)")
    @NonNull
    List<UserTeamAssignmentEntity> getCurrentTeamMembers(@NonNull String teamId,
                                                         @NonNull LocalDate date);

    /**
     * Get team assignments within date range.
     *
     * @param teamId Team ID to find assignments for
     * @param startDate Start date of range (inclusive)
     * @param endDate End date of range (inclusive)
     * @return List of assignments overlapping with date range
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE team_id = :teamId AND active = 1 " +
            "AND start_date <= :endDate " +
            "AND (end_date IS NULL OR end_date >= :startDate) " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByTeamIdAndDateRange(@NonNull String teamId,
                                                           @NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate);

    /**
     * Count active team members on specific date.
     *
     * @param teamId Team ID to count members for
     * @param date Date to check assignments for
     * @return Number of active team members
     */
    @Query("SELECT COUNT(*) FROM user_team_assignments " +
            "WHERE team_id = :teamId AND active = 1 " +
            "AND start_date <= :date " +
            "AND (end_date IS NULL OR end_date >= :date)")
    int countTeamMembersOnDate(@NonNull String teamId, @NonNull LocalDate date);

    // ==================== USER-TEAM RELATIONSHIP QUERIES ====================

    /**
     * Check if user is assigned to specific team on date.
     *
     * @param userId User ID to check
     * @param teamId Team ID to check
     * @param date Date to check assignment for
     * @return Assignment if exists and active, null otherwise
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE user_id = :userId AND team_id = :teamId AND active = 1 " +
            "AND start_date <= :date " +
            "AND (end_date IS NULL OR end_date >= :date) " +
            "LIMIT 1")
    @Nullable
    UserTeamAssignmentEntity getAssignment(@NonNull String userId,
                                           @NonNull String teamId,
                                           @NonNull LocalDate date);

    /**
     * Get all assignments between specific user and team.
     *
     * @param userId User ID
     * @param teamId Team ID
     * @return List of all assignments between user and team
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE user_id = :userId AND team_id = :teamId " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getAssignmentHistory(@NonNull String userId,
                                                        @NonNull String teamId);

    // ==================== STATUS-BASED QUERIES ====================

    /**
     * Get assignments by status.
     *
     * @param status Status to filter by
     * @return List of assignments with specified status
     */
    @Query("SELECT * FROM user_team_assignments WHERE status = :status " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByStatus(@NonNull Status status);

    /**
     * Get assignments by status and active flag.
     *
     * @param status Status to filter by
     * @param active Active flag to filter by
     * @return List of assignments matching criteria
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE status = :status AND active = :active " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getByStatusAndActive(@NonNull Status status, boolean active);

    // ==================== DATE-BASED QUERIES ====================

    /**
     * Get all assignments starting on specific date.
     *
     * @param startDate Date to find assignments starting on
     * @return List of assignments starting on specified date
     */
    @Query("SELECT * FROM user_team_assignments WHERE start_date = :startDate " +
            "ORDER BY created_at ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getAssignmentsStartingOn(@NonNull LocalDate startDate);

    /**
     * Get all assignments ending on specific date.
     *
     * @param endDate Date to find assignments ending on
     * @return List of assignments ending on specified date
     */
    @Query("SELECT * FROM user_team_assignments WHERE end_date = :endDate " +
            "ORDER BY created_at ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getAssignmentsEndingOn(@NonNull LocalDate endDate);

    /**
     * Get assignments active within date range (for calendar display).
     *
     * @param startDate Start date of range (inclusive)
     * @param endDate End date of range (inclusive)
     * @return List of assignments overlapping with date range
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE active = 1 " +
            "AND start_date <= :endDate " +
            "AND (end_date IS NULL OR end_date >= :startDate) " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getAssignmentsInDateRange(@NonNull LocalDate startDate,
                                                             @NonNull LocalDate endDate);

    /**
     * Get permanent assignments (no end date).
     *
     * @return List of permanent assignments
     */
    @Query("SELECT * FROM user_team_assignments WHERE end_date IS NULL AND active = 1 " +
            "ORDER BY start_date ASC")
    @NonNull
    List<UserTeamAssignmentEntity> getPermanentAssignments();

    // ==================== MANAGEMENT AND ADMIN QUERIES ====================

    /**
     * Get assignments created by specific user.
     *
     * @param createdByUserId User ID who created assignments
     * @return List of assignments created by user
     */
    @Query("SELECT * FROM user_team_assignments WHERE assigned_by_user_id = :createdByUserId " +
            "ORDER BY created_at DESC")
    @NonNull
    List<UserTeamAssignmentEntity> getAssignmentsCreatedBy(@NonNull String createdByUserId);

    /**
     * Get assignments modified within time range.
     *
     * @param fromTimestamp Start timestamp (inclusive)
     * @param toTimestamp End timestamp (inclusive)
     * @return List of assignments modified within time range
     */
    @Query("SELECT * FROM user_team_assignments " +
            "WHERE updated_at BETWEEN :fromTimestamp AND :toTimestamp " +
            "ORDER BY updated_at DESC")
    @NonNull
    List<UserTeamAssignmentEntity> getModifiedBetween(long fromTimestamp, long toTimestamp);

    /**
     * Get recently created assignments.
     *
     * @param limit Maximum number of assignments to return
     * @return List of most recently created assignments
     */
    @Query("SELECT * FROM user_team_assignments " +
            "ORDER BY created_at DESC LIMIT :limit")
    @NonNull
    List<UserTeamAssignmentEntity> getRecentlyCreated(int limit);

    // ==================== BATCH UPDATE OPERATIONS ====================

    /**
     * Update assignment status for multiple assignments.
     *
     * @param assignmentIds List of assignment IDs to update
     * @param status New status to set
     * @param timestamp Current timestamp for updated_at
     * @param lastModifiedByUserId User performing the update
     * @return Number of assignments updated
     */
    @Query("UPDATE user_team_assignments " +
            "SET status = :status, updated_at = :timestamp, " +
            "last_modified_by_user_id = :lastModifiedByUserId " +
            "WHERE id IN (:assignmentIds)")
    int updateStatusForAssignments(@NonNull List<String> assignmentIds,
                                   @NonNull Status status,
                                   long timestamp,
                                   @Nullable String lastModifiedByUserId);

    /**
     * Deactivate assignments for specific user.
     *
     * @param userId User ID to deactivate assignments for
     * @param timestamp Current timestamp for updated_at
     * @param lastModifiedByUserId User performing the deactivation
     * @return Number of assignments deactivated
     */
    @Query("UPDATE user_team_assignments " +
            "SET active = 0, updated_at = :timestamp, " +
            "last_modified_by_user_id = :lastModifiedByUserId " +
            "WHERE user_id = :userId AND active = 1")
    int deactivateAssignmentsForUser(@NonNull String userId,
                                     long timestamp,
                                     @Nullable String lastModifiedByUserId);

    /**
     * Deactivate assignments for specific team.
     *
     * @param teamId Team ID to deactivate assignments for
     * @param timestamp Current timestamp for updated_at
     * @param lastModifiedByUserId User performing the deactivation
     * @return Number of assignments deactivated
     */
    @Query("UPDATE user_team_assignments " +
            "SET active = 0, updated_at = :timestamp, " +
            "last_modified_by_user_id = :lastModifiedByUserId " +
            "WHERE team_id = :teamId AND active = 1")
    int deactivateAssignmentsForTeam(@NonNull String teamId,
                                     long timestamp,
                                     @Nullable String lastModifiedByUserId);

    // ==================== CLEANUP OPERATIONS ====================

    /**
     * Delete all inactive assignments older than specified timestamp.
     *
     * @param beforeTimestamp Delete assignments created before this timestamp
     * @return Number of assignments deleted
     */
    @Query("DELETE FROM user_team_assignments " +
            "WHERE active = 0 AND created_at < :beforeTimestamp")
    int deleteInactiveAssignmentsBefore(long beforeTimestamp);

    /**
     * Delete all assignments (for testing/reset purposes).
     *
     * @return Number of assignments deleted
     */
    @Query("DELETE FROM user_team_assignments")
    int deleteAll();
}