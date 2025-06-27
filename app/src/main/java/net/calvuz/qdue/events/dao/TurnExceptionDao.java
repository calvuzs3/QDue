package net.calvuz.qdue.events.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.calvuz.qdue.events.models.TurnException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for TurnException operations.
 * Provides CRUD operations and specialized queries for virtual scrolling integration.
 * <p>
 * Key Features:
 * - Month-based queries for virtual scrolling
 * - User-specific exception management
 * - Bulk operations for performance
 * - Pattern restoration support
 */
@Dao
public interface TurnExceptionDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Insert a new turn exception
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertException(TurnException exception);

    /**
     * Insert multiple exceptions (batch operation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertExceptions(List<TurnException> exceptions);

    /**
     * Update an existing exception
     */
    @Update
    void updateException(TurnException exception);

    /**
     * Update multiple exceptions
     */
    @Update
    void updateExceptions(List<TurnException> exceptions);

    /**
     * Delete an exception (restores original pattern)
     */
    @Delete
    void deleteException(TurnException exception);

    /**
     * Delete multiple exceptions
     */
    @Delete
    void deleteExceptions(List<TurnException> exceptions);

    // ==================== VIRTUAL SCROLLING QUERIES ====================

    /**
     * Get all exceptions for a specific month and user (virtual scrolling)
     * Most important query for virtual scrolling performance
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :monthStart " +
            "AND date < :monthEnd " +
            "AND status = 'ACTIVE' " +
            "ORDER BY date ASC")
    List<TurnException> getExceptionsForUserMonth(long userId, LocalDate monthStart, LocalDate monthEnd);

    /**
     * Get exceptions for multiple months (viewport loading)
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :startDate " +
            "AND date <= :endDate " +
            "AND status = 'ACTIVE' " +
            "ORDER BY date ASC")
    List<TurnException> getExceptionsForUserDateRange(long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Check if user has exception on specific date
     */
    @Query("SELECT EXISTS(SELECT 1 FROM turn_exceptions " +
            "WHERE user_id = :userId AND date = :date AND status = 'ACTIVE')")
    boolean hasExceptionOnDate(long userId, LocalDate date);

    /**
     * Get specific exception for user and date
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId AND date = :date AND status = 'ACTIVE' " +
            "LIMIT 1")
    TurnException getExceptionForUserDate(long userId, LocalDate date);

    // ==================== MANAGEMENT QUERIES ====================

    /**
     * Get all exceptions for a user
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "ORDER BY date DESC")
    List<TurnException> getAllExceptionsForUser(long userId);

    /**
     * Get exceptions by type for a user
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND exception_type = :exceptionType " +
            "AND status = 'ACTIVE' " +
            "ORDER BY date DESC")
    List<TurnException> getExceptionsByType(long userId, TurnException.ExceptionType exceptionType);

    /**
     * Get upcoming exceptions for a user
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :fromDate " +
            "AND status = 'ACTIVE' " +
            "ORDER BY date ASC " +
            "LIMIT :limit")
    List<TurnException> getUpcomingExceptions(long userId, LocalDate fromDate, int limit);

    /**
     * Get recent exceptions for a user
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND created_at >= :since " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit")
    List<TurnException> getRecentExceptions(long userId, LocalDateTime since, int limit);

    // ==================== ANALYTICS QUERIES ====================

    /**
     * Count exceptions by type for a user in a date range
     */
    @Query("SELECT exception_type, COUNT(*) as count " +
            "FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :startDate " +
            "AND date <= :endDate " +
            "AND status = 'ACTIVE' " +
            "GROUP BY exception_type")
    List<ExceptionTypeCount> getExceptionCountsByType(long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get total exception count for user
     */
    @Query("SELECT COUNT(*) FROM turn_exceptions " +
            "WHERE user_id = :userId AND status = 'ACTIVE'")
    int getTotalExceptionCount(long userId);

    /**
     * Get exception count for specific month
     */
    @Query("SELECT COUNT(*) FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :monthStart " +
            "AND date < :monthEnd " +
            "AND status = 'ACTIVE'")
    int getMonthExceptionCount(long userId, LocalDate monthStart, LocalDate monthEnd);

    // ==================== BULK OPERATIONS ====================

    /**
     * Delete all exceptions for a user in a date range
     */
    @Query("DELETE FROM turn_exceptions " +
            "WHERE user_id = :userId " +
            "AND date >= :startDate " +
            "AND date <= :endDate")
    void deleteExceptionsInRange(long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Delete all exceptions for a user
     */
    @Query("DELETE FROM turn_exceptions WHERE user_id = :userId")
    void deleteAllExceptionsForUser(long userId);

    /**
     * Soft delete exceptions (mark as cancelled)
     */
    @Query("UPDATE turn_exceptions " +
            "SET status = 'CANCELLED', modified_at = :modifiedAt " +
            "WHERE user_id = :userId " +
            "AND date >= :startDate " +
            "AND date <= :endDate")
    void cancelExceptionsInRange(long userId, LocalDate startDate, LocalDate endDate, LocalDateTime modifiedAt);

    // ==================== MAINTENANCE QUERIES ====================

    /**
     * Get exceptions that need cleanup (old cancelled exceptions)
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE status = 'CANCELLED' " +
            "AND modified_at < :cutoffDate " +
            "ORDER BY modified_at ASC")
    List<TurnException> getExpiredCancelledExceptions(LocalDateTime cutoffDate);

    /**
     * Get orphaned exceptions (users that don't exist)
     */
    @Query("SELECT * FROM turn_exceptions " +
            "WHERE user_id NOT IN (SELECT id FROM users)")
    List<TurnException> getOrphanedExceptions();

    /**
     * Database statistics for monitoring
     */
    @Query("SELECT " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active, " +
            "COUNT(CASE WHEN date >= :today THEN 1 END) as future, " +
            "MAX(created_at) as latest_created " +
            "FROM turn_exceptions")
    ExceptionStatistics getExceptionStatistics(LocalDate today);

    // ==================== TRANSACTION OPERATIONS ====================

    /**
     * Replace exception (delete old, insert new) - atomic operation
     * Useful for editing existing exceptions
     */
    @Transaction
    default void replaceException(TurnException oldException, TurnException newException) {
        deleteException(oldException);
        insertException(newException);
    }

    /**
     * Bulk replace exceptions for a date range
     */
    @Transaction
    default void replaceExceptionsInRange(long userId, LocalDate startDate, LocalDate endDate,
                                          List<TurnException> newExceptions) {
        deleteExceptionsInRange(userId, startDate, endDate);
        if (!newExceptions.isEmpty()) {
            insertExceptions(newExceptions);
        }
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Result class for exception type counts
     */
    class ExceptionTypeCount {
        public TurnException.ExceptionType exception_type;
        public int count;
    }

    /**
     * Result class for database statistics
     */
    class ExceptionStatistics {
        public int total;
        public int active;
        public int future;
        public LocalDateTime latest_created;
    }
}