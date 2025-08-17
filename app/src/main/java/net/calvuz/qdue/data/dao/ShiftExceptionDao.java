package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.ShiftExceptionEntity;

import java.util.List;

/**
 * ShiftExceptionDao - Data Access Object for schedule exceptions.
 *
 * <p>Highly optimized for user schedule queries, approval workflows, and conflict resolution.
 * Includes specialized methods for different exception types and business scenarios.</p>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Initial Implementation
 * @since Clean architecture
 */
@Dao
public interface ShiftExceptionDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    long insertShiftException(@NonNull ShiftExceptionEntity exception);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertShiftExceptions(@NonNull List<ShiftExceptionEntity> exceptions);

    @Update
    int updateShiftException(@NonNull ShiftExceptionEntity exception);

    @Delete
    int deleteShiftException(@NonNull ShiftExceptionEntity exception);

    @Query ("DELETE FROM shift_exceptions WHERE id = :exceptionId")
    int deleteShiftExceptionById(@NonNull String exceptionId);

    @Query("DELETE FROM shift_exceptions WHERE active = 0 AND updated_at < :cutoffTime")
    int deleteInactiveExceptionsOlderThan(long cutoffTime);

    // ==================== SINGLE RECORD QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE id = :exceptionId LIMIT 1")
    @Nullable
    ShiftExceptionEntity getShiftExceptionById(@NonNull String exceptionId);

    // ==================== USER-CENTRIC QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE user_id = :userId AND active = 1 ORDER BY target_date DESC, priority DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsByUserId(@NonNull Long userId);

    @Query("SELECT * FROM shift_exceptions WHERE user_id = :userId AND target_date = :date AND active = 1 ORDER BY priority DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsForUserAndDate(@NonNull Long userId, @NonNull String date);

    @Query("SELECT * FROM shift_exceptions WHERE user_id = :userId AND target_date >= :startDate AND target_date <= :endDate AND active = 1 ORDER BY target_date, priority DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsForUserInDateRange(@NonNull Long userId, @NonNull String startDate, @NonNull String endDate);

    @Query("SELECT * FROM shift_exceptions WHERE user_id = :userId AND status = :status AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsByUserAndStatus(@NonNull Long userId, @NonNull String status);

    // ==================== DATE-BASED QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE target_date = :date AND active = 1 ORDER BY priority DESC, user_id")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsForDate(@NonNull String date);

    @Query("SELECT * FROM shift_exceptions WHERE target_date >= :startDate AND target_date <= :endDate AND active = 1 ORDER BY target_date, priority DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsInDateRange(@NonNull String startDate, @NonNull String endDate);

    // ==================== TYPE-BASED QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE exception_type = :exceptionType AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsByType(@NonNull String exceptionType);

    @Query("SELECT * FROM shift_exceptions WHERE exception_type LIKE :typePrefix AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsByTypePrefix(@NonNull String typePrefix);

    // Absence types
    @Query("SELECT * FROM shift_exceptions WHERE exception_type LIKE 'ABSENCE_%' AND user_id = :userId AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getUserAbsences(@NonNull Long userId);

    // Shift changes
    @Query("SELECT * FROM shift_exceptions WHERE exception_type LIKE 'CHANGE_%' AND user_id = :userId AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getUserShiftChanges(@NonNull Long userId);

    // Time reductions
    @Query("SELECT * FROM shift_exceptions WHERE exception_type LIKE 'REDUCTION_%' AND user_id = :userId AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getUserTimeReductions(@NonNull Long userId);

    // ==================== APPROVAL WORKFLOW QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE status = 'PENDING' AND requires_approval = 1 AND active = 1 ORDER BY target_date, priority DESC")
    @NonNull
    List<ShiftExceptionEntity> getPendingApprovalsOrderedByDateAndPriority();

    @Query("SELECT * FROM shift_exceptions WHERE status = :status AND requires_approval = 1 AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsByApprovalStatus(@NonNull String status);

    @Query("SELECT * FROM shift_exceptions WHERE approved_by_user_id = :approverId AND active = 1 ORDER BY approved_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftExceptionsApprovedBy(@NonNull Long approverId);

    // ==================== COLLABORATION QUERIES ====================

    @Query("SELECT * FROM shift_exceptions WHERE swap_with_user_id = :userId AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftSwapsInvolvingUser(@NonNull Long userId);

    @Query("SELECT * FROM shift_exceptions WHERE (user_id = :userId1 AND swap_with_user_id = :userId2) OR (user_id = :userId2 AND swap_with_user_id = :userId1) AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getShiftSwapsBetweenUsers(@NonNull Long userId1, @NonNull Long userId2);

    @Query("SELECT * FROM shift_exceptions WHERE replacement_user_id = :userId AND active = 1 ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getReplacementAssignmentsForUser(@NonNull Long userId);

    // ==================== CONFLICT DETECTION QUERIES ====================

    @Query("""
        SELECT * FROM shift_exceptions 
        WHERE user_id = :userId 
        AND target_date = :date 
        AND active = 1 
        AND (status = 'APPROVED' OR (status = 'DRAFT' AND requires_approval = 0))
        ORDER BY priority DESC
        """)
    @NonNull
    List<ShiftExceptionEntity> getEffectiveExceptionsForUserAndDate(@NonNull Long userId, @NonNull String date);

    @Query("""
        SELECT * FROM shift_exceptions 
        WHERE target_date = :date 
        AND active = 1 
        AND (status = 'APPROVED' OR (status = 'DRAFT' AND requires_approval = 0))
        ORDER BY priority DESC, user_id
        """)
    @NonNull
    List<ShiftExceptionEntity> getEffectiveExceptionsForDate(@NonNull String date);

    // ==================== BUSINESS UPDATE OPERATIONS ====================

    @Query("UPDATE shift_exceptions SET status = :newStatus, updated_at = :timestamp WHERE id = :exceptionId")
    int updateExceptionStatus(@NonNull String exceptionId, @NonNull String newStatus, long timestamp);

    @Query("UPDATE shift_exceptions SET status = 'APPROVED', approved_by_user_id = :approverId, approved_by_user_name = :approverName, approved_date = :approvedDate, updated_at = :timestamp WHERE id = :exceptionId")
    int approveException(@NonNull String exceptionId, @NonNull Long approverId, @NonNull String approverName, @NonNull String approvedDate, long timestamp);

    @Query("UPDATE shift_exceptions SET status = 'REJECTED', rejection_reason = :reason, updated_at = :timestamp WHERE id = :exceptionId")
    int rejectException(@NonNull String exceptionId, @NonNull String reason, long timestamp);

    @Query("UPDATE shift_exceptions SET active = 0, updated_at = :timestamp WHERE id = :exceptionId")
    int deactivateException(@NonNull String exceptionId, long timestamp);

    // ==================== BACKUP OPERATIONS ====================

    /**
     * Get ALL shift exceptions for complete backup
     * Essential for full database backup operations
     */
    @Query("SELECT * FROM shift_exceptions ORDER BY target_date DESC, created_at DESC")
    @NonNull
    List<ShiftExceptionEntity> getAllShiftExceptions();

    /**
     * Get all shift exceptions with status filter for backup operations
     *
     * @param active Status filter (true for active, false for inactive)
     */
    @Query("SELECT * FROM shift_exceptions WHERE active = :active ORDER BY target_date DESC")
    @NonNull
    List<ShiftExceptionEntity> getAllShiftExceptionsByStatus(boolean active);

    /**
     * Get total count of all shift exceptions (for backup statistics)
     */
    @Query("SELECT COUNT(*) FROM shift_exceptions")
    int getTotalShiftExceptionCount();

    // ==================== STATISTICS QUERIES ====================

    @Query("SELECT COUNT(*) FROM shift_exceptions WHERE user_id = :userId AND active = 1")
    int getExceptionCountForUser(@NonNull Long userId);

    @Query("SELECT COUNT(*) FROM shift_exceptions WHERE target_date = :date AND active = 1")
    int getExceptionCountForDate(@NonNull String date);

    @Query("SELECT COUNT(*) FROM shift_exceptions WHERE status = 'PENDING' AND requires_approval = 1 AND active = 1")
    int getPendingApprovalCount();

    @Query("""
        SELECT 
            COUNT(*) as total_exceptions,
            SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_exceptions,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_exceptions,
            SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved_exceptions,
            SUM(CASE WHEN exception_type LIKE 'ABSENCE_%' THEN 1 ELSE 0 END) as absences,
            SUM(CASE WHEN exception_type LIKE 'CHANGE_%' THEN 1 ELSE 0 END) as changes,
            SUM(CASE WHEN exception_type LIKE 'REDUCTION_%' THEN 1 ELSE 0 END) as reductions
        FROM shift_exceptions
        WHERE user_id = :userId
        """)
    @Nullable
    UserExceptionStatistics getUserExceptionStatistics(@NonNull Long userId);

    /**
     * Statistics container for user exceptions.
     */
    class UserExceptionStatistics {
        public int total_exceptions;
        public int active_exceptions;
        public int pending_exceptions;
        public int approved_exceptions;
        public int absences;
        public int changes;
        public int reductions;
    }
}
