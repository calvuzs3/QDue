package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.ShiftException;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ShiftExceptionRepository - Clean Architecture repository interface for shift exception persistence.
 *
 * <p>Defines business-oriented operations for managing schedule exceptions with focus on user
 * workflows and approval processes. All operations return {@code OperationResult<T>} for
 * consistent error handling and use {@code CompletableFuture} for asynchronous execution.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Exception Management</strong>: CRUD operations for shift exceptions</li>
 *   <li><strong>Date-Based Queries</strong>: Efficient queries for schedule generation</li>
 *   <li><strong>User-Specific Operations</strong>: User-focused exception management</li>
 *   <li><strong>Approval Workflow</strong>: Support for exception approval processes</li>
 *   <li><strong>Type-Based Filtering</strong>: Exception type specific operations</li>
 * </ul>
 *
 * <h3>Business Use Cases:</h3>
 * <ul>
 *   <li><strong>Schedule Generation</strong>: Apply exceptions during schedule calculation</li>
 *   <li><strong>User Interface</strong>: Display user's exceptions and status</li>
 *   <li><strong>Approval Management</strong>: Supervisor approval workflows</li>
 *   <li><strong>Reporting</strong>: Exception statistics and analysis</li>
 *   <li><strong>Calendar Integration</strong>: Exception-aware calendar views</li>
 * </ul>
 *
 * <h3>Error Handling:</h3>
 * <p>All methods return {@code CompletableFuture<OperationResult<T>>} providing:</p>
 * <ul>
 *   <li><strong>Success/Failure Status</strong>: Clear operation outcome</li>
 *   <li><strong>Data Results</strong>: Retrieved data on success</li>
 *   <li><strong>Error Messages</strong>: Localized error descriptions</li>
 *   <li><strong>Exception Details</strong>: Full exception information for debugging</li>
 *   <li><strong>Operation Type</strong>: CREATE, READ, UPDATE, DELETE for audit</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.1.0 - OperationResult Pattern Implementation
 * @since Clean Architecture Phase 2
 */
public interface ShiftExceptionRepository {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Get shift exception by unique identifier.
     *
     * <p>Retrieves a single shift exception by its ID. Returns a failure result
     * if the exception is not found or an error occurs during retrieval.</p>
     *
     * @param exceptionId Unique exception identifier
     * @return CompletableFuture with OperationResult containing ShiftException or error
     */
    @NonNull
    CompletableFuture<OperationResult<ShiftException>> getShiftExceptionById(@NonNull String exceptionId);

    /**
     * Save a shift exception to persistent storage.
     *
     * <p>Creates a new exception or updates an existing one based on the ID.
     * Validates business rules and updates timestamps automatically.</p>
     *
     * @param shiftException ShiftException domain model to save
     * @return CompletableFuture with OperationResult containing saved ShiftException
     */
    @NonNull
    CompletableFuture<OperationResult<ShiftException>> saveShiftException(@NonNull ShiftException shiftException);

    /**
     * Delete shift exception by identifier.
     *
     * <p>Permanently removes a shift exception from storage. Returns a failure
     * result if the exception is not found.</p>
     *
     * @param exceptionId Exception ID to delete
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> deleteShiftException(@NonNull String exceptionId);

    // ==================== DATE-BASED QUERIES ====================

    /**
     * Get effective exceptions for user on specific date.
     *
     * <p>Returns only exceptions that are currently effective (approved or
     * draft without approval requirement) for the specified user and date.
     * Used during schedule generation to apply exceptions.</p>
     *
     * @param userId User identifier
     * @param date   Target date for exceptions
     * @return CompletableFuture with OperationResult containing List of effective ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getEffectiveExceptionsForUserOnDate(
            @NonNull String userId, @NonNull LocalDate date);

    /**
     * Get all exceptions for user in date range.
     *
     * <p>Retrieves all exceptions (regardless of status) for a user within
     * the specified date range. Useful for user interface displays and reporting.</p>
     *
     * @param userId    User identifier
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return CompletableFuture with OperationResult containing List of ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsForUserInDateRange(
            @NonNull String userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Get all effective exceptions for specific date across all users.
     *
     * <p>Returns effective exceptions for all users on the specified date.
     * Used for schedule generation and conflict detection across teams.</p>
     *
     * @param date Target date for exceptions
     * @return CompletableFuture with OperationResult containing List of effective ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getEffectiveExceptionsForDate(@NonNull LocalDate date);

    /**
     * Get all exceptions in date range across all users.
     *
     * <p>Retrieves all exceptions (regardless of status) within the specified
     * date range for all users. Useful for management reporting and analysis.</p>
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return CompletableFuture with OperationResult containing List of ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsInDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    // ==================== TYPE-BASED QUERIES ====================

    /**
     * Get exceptions by type for specific user.
     *
     * <p>Retrieves all exceptions of a specific type for a user. Useful for
     * analyzing patterns in vacation requests, sick leave, etc.</p>
     *
     * @param userId        User identifier
     * @param exceptionType Type of exception to retrieve
     * @return CompletableFuture with OperationResult containing List of ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsByTypeForUser(
            @NonNull String userId, @NonNull ShiftException.ExceptionType exceptionType);

    /**
     * Get exceptions by approval status.
     *
     * <p>Retrieves all exceptions with a specific approval status across all users.
     * Useful for approval workflow management and status reporting.</p>
     *
     * @param status Approval status to filter by
     * @return CompletableFuture with OperationResult containing List of ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsByApprovalStatus(
            @NonNull ShiftException.ApprovalStatus status);

    // ==================== APPROVAL WORKFLOW ====================

    /**
     * Get pending exceptions requiring approval, ordered by priority and date.
     *
     * <p>Returns exceptions in PENDING status ordered by priority (highest first)
     * and target date (earliest first). Used for supervisor approval interfaces.</p>
     *
     * @return CompletableFuture with OperationResult containing List of pending ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getPendingApprovalsOrderedByPriority();

    /**
     * Update exception approval status.
     *
     * <p>Changes the approval status of an exception and updates the timestamp.
     * Used for workflow state transitions.</p>
     *
     * @param exceptionId Exception identifier
     * @param newStatus   New approval status
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> updateExceptionStatus(
            @NonNull String exceptionId, @NonNull ShiftException.ApprovalStatus newStatus);

    /**
     * Approve exception with approver details.
     *
     * <p>Approves an exception by setting status to APPROVED and recording
     * approver information and approval date.</p>
     *
     * @param exceptionId  Exception identifier
     * @param approverId   Approver user identifier
     * @param approverName Approver display name
     * @param approvedDate Date of approval
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> approveException(
            @NonNull String exceptionId, @NonNull Long approverId,
            @NonNull String approverName, @NonNull LocalDate approvedDate);

    /**
     * Reject exception with reason.
     *
     * <p>Rejects an exception by setting status to REJECTED and recording
     * the rejection reason for user feedback.</p>
     *
     * @param exceptionId     Exception identifier
     * @param rejectionReason Reason for rejection (user-facing message)
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> rejectException(
            @NonNull String exceptionId, @NonNull String rejectionReason);

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get total exception count for specific user.
     *
     * <p>Returns the total number of exceptions (all statuses) for a user.
     * Useful for user profile displays and quota management.</p>
     *
     * @param userId User identifier
     * @return CompletableFuture with OperationResult containing exception count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getExceptionCountForUser(@NonNull String userId);

    // ==================== FUTURE EXTENSIONS ====================

    // Note: Additional methods for future functionality:
    // - getRecurringExceptions() - For recurring exception support
    // - getExceptionsByPriority() - Priority-based filtering
    // - getExceptionsRequiringAction() - Action items for supervisors
    // - getExceptionMetrics() - Statistical analysis
    // - bulkUpdateExceptions() - Batch operations
}