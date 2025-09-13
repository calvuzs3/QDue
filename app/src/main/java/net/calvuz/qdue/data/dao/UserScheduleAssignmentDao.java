package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.UserScheduleAssignmentEntity;

import java.util.List;

/**
 * UserScheduleAssignmentDao - Data Access Object for user-team assignments.
 *
 * <p>Optimized for user schedule generation, team roster management, and assignment
 * conflict resolution. Includes specialized queries for multi-user scheduling scenarios.</p>
 */
@Dao
public interface UserScheduleAssignmentDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    long insertUserScheduleAssignment(@NonNull UserScheduleAssignmentEntity assignment);

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertUserScheduleAssignments(@NonNull List<UserScheduleAssignmentEntity> assignments);

    @Update
    int updateUserScheduleAssignment(@NonNull UserScheduleAssignmentEntity assignment);

    @Delete
    int deleteUserScheduleAssignment(@NonNull UserScheduleAssignmentEntity assignment);

    @Query ("DELETE FROM user_schedule_assignments")
    int deleteAllUserScheduleAssignments();

    @Query ("DELETE FROM user_schedule_assignments WHERE id = :assignmentId")
    int deleteUserScheduleAssignmentById(@NonNull String assignmentId);

    @Query ("DELETE FROM user_schedule_assignments WHERE active = 0 AND updated_at < :cutoffTime")
    int deleteInactiveAssignmentsOlderThan(long cutoffTime);

    // ==================== SINGLE RECORD QUERIES ====================

    @Query ("SELECT * FROM user_schedule_assignments WHERE id = :assignmentId LIMIT 1")
    @Nullable
    UserScheduleAssignmentEntity getUserScheduleAssignmentById(@NonNull String assignmentId);

    // ==================== USER-CENTRIC QUERIES ====================

    @Query ("SELECT * FROM user_schedule_assignments WHERE user_id = :userId AND active = 1 ORDER BY priority DESC, start_date DESC")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsByUserId(@NonNull String userId);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND active = 1 
            AND (end_date IS NULL OR end_date >= date('now'))
            ORDER BY priority DESC, start_date DESC
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getUserActiveAssignments(@NonNull String userId);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND (status = 'ACTIVE' OR status = 'PENDING') 
            AND start_date <= :date 
            AND (is_permanent = 1 OR end_date >= :date) 
            AND active = 1 
            ORDER BY priority DESC 
            LIMIT 1
            """)
    @Nullable
    UserScheduleAssignmentEntity getActiveAssignmentForUserOnDate(@NonNull String userId, @NonNull String date);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND start_date <= :endDate  
            AND (is_permanent = 1 OR end_date >= :startDate) 
            AND active = 1 
            ORDER BY priority DESC, start_date
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsForUserInDateRange(@NonNull String userId, @NonNull String startDate, @NonNull String endDate);

    @Query ("SELECT * FROM user_schedule_assignments WHERE user_id = :userId AND status = :status AND active = 1 ORDER BY start_date DESC")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsByUserAndStatus(@NonNull String userId, @NonNull String status);

    // ==================== TEAM-CENTRIC QUERIES ====================

    @Query ("SELECT * FROM user_schedule_assignments WHERE team_id = :teamId AND active = 1 ORDER BY start_date DESC")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsByTeamId(@NonNull String teamId);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE team_id = :teamId 
            AND status = 'ACTIVE' 
            AND start_date <= :date 
            AND (is_permanent = 1 OR end_date >= :date)
            AND active = 1 
            ORDER BY user_id
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getActiveTeamMembersOnDate(@NonNull String teamId, @NonNull String date);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE team_id = :teamId 
            AND start_date <= :endDate 
            AND (is_permanent = 1 OR end_date >= :startDate)
            AND active = 1 
            ORDER BY start_date, user_id
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getTeamAssignmentsInDateRange(@NonNull String teamId, @NonNull String startDate, @NonNull String endDate);

    // ==================== DATE-BASED QUERIES ====================

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE status = 'ACTIVE' 
            AND start_date <= :date 
            AND (is_permanent = 1 OR end_date >= :date)
            AND active = 1 
            ORDER BY team_id, user_id
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getAllActiveAssignmentsOnDate(@NonNull String date);

    @Query ("SELECT * FROM user_schedule_assignments WHERE start_date = :date AND active = 1 ORDER BY team_id, user_id")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsStartingOnDate(@NonNull String date);

    @Query ("SELECT * FROM user_schedule_assignments WHERE end_date = :date AND active = 1 ORDER BY team_id, user_id")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsEndingOnDate(@NonNull String date);

    // ==================== CONFLICT DETECTION QUERIES ====================

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND start_date <= :endDate 
            AND (is_permanent = 1 OR end_date >= :startDate)
            AND active = 1 
            AND id != :excludeAssignmentId
            ORDER BY priority DESC
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> findConflictingAssignments(@NonNull String userId, @NonNull String startDate, @NonNull String endDate, @NonNull String excludeAssignmentId);

    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND start_date <= :endDate 
            AND (is_permanent = 1 OR end_date >= :startDate)
            AND status = 'ACTIVE'
            AND active = 1 
            ORDER BY priority DESC
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getActiveConflictingAssignments(@NonNull String userId, @NonNull String startDate, @NonNull String endDate);

    // ==================== RECURRENCE RULE QUERIES ====================

    @Query ("SELECT * FROM user_schedule_assignments WHERE recurrence_rule_id = :ruleId AND active = 1 ORDER BY start_date")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsByRecurrenceRule(@NonNull String ruleId);

    @Query ("SELECT COUNT(*) FROM user_schedule_assignments WHERE recurrence_rule_id = :ruleId AND active = 1")
    int getAssignmentCountForRecurrenceRule(@NonNull String ruleId);

    // ==================== BUSINESS UPDATE OPERATIONS ====================

    @Query ("UPDATE user_schedule_assignments SET status = :newStatus, updated_at = :timestamp WHERE id = :assignmentId")
    int updateAssignmentStatus(@NonNull String assignmentId, @NonNull String newStatus, long timestamp);

    @Query ("UPDATE user_schedule_assignments SET team_id = :newTeamId, team_name = :newTeamName, updated_at = :timestamp WHERE id = :assignmentId")
    int transferUserToTeam(@NonNull String assignmentId, @NonNull String newTeamId, @NonNull String newTeamName, long timestamp);

    @Query ("UPDATE user_schedule_assignments SET end_date = :endDate, is_permanent = 0, updated_at = :timestamp WHERE id = :assignmentId")
    int setAssignmentEndDate(@NonNull String assignmentId, @NonNull String endDate, long timestamp);

    @Query ("UPDATE user_schedule_assignments SET active = 0, updated_at = :timestamp WHERE id = :assignmentId")
    int deactivateAssignment(@NonNull String assignmentId, long timestamp);

    // ==================== OTHER QUERIES ====================

    /**
     * Get all active assignments in date range (more efficient than current implementation).
     */
    @Query ("""
            SELECT * FROM user_schedule_assignments 
            WHERE status = 'ACTIVE' 
            AND start_date <= :endDate 
            AND (is_permanent = 1 OR end_date >= :startDate) 
            AND active = 1 
            ORDER BY team_id, user_id
            """)
    @NonNull
    List<UserScheduleAssignmentEntity> getActiveAssignmentsInDateRange(
            @NonNull String startDate, @NonNull String endDate);

    /**
     * Get all active user IDs (more efficient than current implementation).
     */
    @Query ("""
            SELECT DISTINCT user_id FROM user_schedule_assignments 
            WHERE status = 'ACTIVE' 
            AND start_date <= date('now') 
            AND (is_permanent = 1 OR end_date >= date('now')) 
            AND active = 1
            """)
    @NonNull
    List<String> getAllActiveUserIds();

    /**
     * Alternative method using specific date.
     */
    @Query ("""
            SELECT DISTINCT user_id FROM user_schedule_assignments 
            WHERE status = 'ACTIVE' 
            AND start_date <= :date 
            AND (is_permanent = 1 OR end_date >= :date)  
            AND active = 1
            """)
    @NonNull
    List<String> getAllActiveUserIdsOnDate(@NonNull String date);

    /**
     * Get active assignment count for user (for performance).
     */
    @Query ("""
            SELECT COUNT(*) FROM user_schedule_assignments 
            WHERE user_id = :userId 
            AND status = 'ACTIVE'  
            AND start_date <= date('now') 
            AND (is_permanent = 1 OR end_date >= date('now')) 
            AND active = 1
            """)
    int getActiveAssignmentCountForUser(@NonNull String userId);

    /**
     * Get active assignment count for team (for performance).
     */
    @Query ("""
            SELECT COUNT(*) FROM user_schedule_assignments 
            WHERE team_id = :teamId 
            AND status = 'ACTIVE' 
            AND start_date <= date('now') 
            AND (is_permanent = 1 OR end_date >= date('now')) 
            AND active = 1
            """)
    int getActiveAssignmentCountForTeam(@NonNull String teamId);

    /**
     * Get ALL user schedule assignments for complete backup
     * Essential for full database backup operations
     */
    @Query ("SELECT * FROM user_schedule_assignments ORDER BY start_date DESC, created_at DESC")
    @NonNull
    List<UserScheduleAssignmentEntity> getAllUserScheduleAssignments();

    /**
     * Get assignments in date range for backup and reporting
     * Useful for selective backup operations
     *
     * @param startDate Start date for date range
     * @param endDate   End date for date range
     */
    @Query ("SELECT * FROM user_schedule_assignments WHERE " +
            "(start_date <= :endDate) AND " +
            "(end_date IS NULL OR end_date >= :startDate) " +
            "ORDER BY start_date, created_at")
    @NonNull
    List<UserScheduleAssignmentEntity> getAssignmentsInDateRange(@NonNull String startDate, @NonNull String endDate);

    /**
     * Get all assignments with status filter for backup operations
     *
     * @param active Status filter (true for active, false for inactive)
     */
    @Query ("SELECT * FROM user_schedule_assignments WHERE active = :active ORDER BY start_date DESC")
    @NonNull
    List<UserScheduleAssignmentEntity> getAllAssignmentsByStatus(boolean active);

    /**
     * Get total count of all assignments (for backup statistics)
     */
    @Query ("SELECT COUNT(*) FROM user_schedule_assignments")
    int getTotalAssignmentCount();

    // ==================== STATISTICS QUERIES ====================

    @Query ("SELECT COUNT(*) FROM user_schedule_assignments WHERE user_id = :userId AND active = 1")
    int getAssignmentCountForUser(@NonNull String userId);

    @Query ("SELECT COUNT(*) FROM user_schedule_assignments WHERE team_id = :teamId AND active = 1")
    int getAssignmentCountForTeam(@NonNull String teamId);

    @Query ("SELECT COUNT(DISTINCT user_id) FROM user_schedule_assignments WHERE team_id = :teamId AND status = 'ACTIVE' AND active = 1")
    int getActiveUserCountForTeam(@NonNull String teamId);

    @Query ("""
            SELECT 
                COUNT(*) as total_assignments,
                SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_assignments, 
                SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as current_assignments, 
                SUM(CASE WHEN is_permanent = 1 THEN 1 ELSE 0 END) as permanent_assignments, 
                COUNT(DISTINCT user_id) as unique_users, 
                COUNT(DISTINCT team_id) as unique_teams 
            FROM user_schedule_assignments
            """)
    @Nullable
    AssignmentStatistics getAssignmentStatistics();

    /**
     * Statistics container for assignments.
     */
    class AssignmentStatistics {
        public int total_assignments;
        public int active_assignments;
        public int current_assignments;
        public int permanent_assignments;
        public int unique_users;
        public int unique_teams;
    }
}
