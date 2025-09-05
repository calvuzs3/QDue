package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.common.enums.Status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * UserTeamAssignmentRepository - Clean Architecture Domain Repository Interface
 *
 * <p>Domain layer repository interface for UserTeamAssignment entity following clean architecture principles.
 * Provides abstraction for user-team assignment persistence operations with consistent error handling using OperationResult pattern.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Domain Layer</strong>: Interface only, no implementation details</li>
 *   <li><strong>Dependency Direction</strong>: Data layer depends on this interface</li>
 *   <li><strong>Business Focused</strong>: Operations reflect business needs, not database structure</li>
 *   <li><strong>Consistent Results</strong>: All operations return OperationResult for uniform error handling</li>
 * </ul>
 *
 * <h3>Repository Operations Categories:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Create, read, update, delete with validation</li>
 *   <li><strong>User Management</strong>: Find assignments by user with date filtering</li>
 *   <li><strong>Team Management</strong>: Find assignments by team with member counting</li>
 *   <li><strong>Calendar Operations</strong>: Date-range based queries for scheduling</li>
 *   <li><strong>Business Logic</strong>: Assignment validation, conflict detection, status management</li>
 * </ul>
 *
 * <h3>Implementation Notes:</h3>
 * <ul>
 *   <li>All operations are asynchronous using CompletableFuture</li>
 *   <li>String IDs used for distributed system compatibility</li>
 *   <li>LocalDate used for precise date-based assignment management</li>
 *   <li>Status enum validation and computation at domain level</li>
 * </ul>
 */
public interface UserTeamAssignmentRepository {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create new user team assignment with validation.
     * Validates business rules including date conflicts and user/team existence.
     *
     * @param assignment Assignment to create (ID will be generated if null)
     * @return CompletableFuture with created assignment including generated ID
     */
    CompletableFuture<OperationResult<UserTeamAssignment>> createAssignment(@NonNull UserTeamAssignment assignment);

    /**
     * Create multiple assignments in single transaction.
     * Validates each assignment and ensures consistency across the batch.
     *
     * @param assignments List of assignments to create
     * @return CompletableFuture with list of created assignments
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> createAssignments(@NonNull List<UserTeamAssignment> assignments);

    /**
     * Get assignment by ID.
     *
     * @param assignmentId Assignment ID to find
     * @return CompletableFuture with found assignment or failure if not exists
     */
    CompletableFuture<OperationResult<UserTeamAssignment>> getAssignmentById(@NonNull String assignmentId);

    /**
     * Get multiple assignments by IDs.
     *
     * @param assignmentIds List of assignment IDs to find
     * @return CompletableFuture with found assignments (may be partial list)
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByIds(@NonNull List<String> assignmentIds);

    /**
     * Update existing assignment with validation.
     * Validates business rules and ensures data consistency.
     *
     * @param assignment Assignment with updated data (must have valid ID)
     * @return CompletableFuture with updated assignment
     */
    CompletableFuture<OperationResult<UserTeamAssignment>> updateAssignment(@NonNull UserTeamAssignment assignment);

    /**
     * Delete assignment by ID (hard delete).
     * Use with caution - consider soft delete for audit trails.
     *
     * @param assignmentId Assignment ID to delete
     * @return CompletableFuture with success/failure result
     */
    CompletableFuture<OperationResult<Boolean>> deleteAssignment(@NonNull String assignmentId);

    /**
     * Soft delete assignment (deactivate).
     * Preserves assignment for audit while marking as inactive.
     *
     * @param assignmentId        Assignment ID to deactivate
     * @param deactivatedByUserId User performing the deactivation
     * @return CompletableFuture with updated assignment
     */
    CompletableFuture<OperationResult<UserTeamAssignment>> deactivateAssignment(@NonNull String assignmentId,
                                                                                @Nullable String deactivatedByUserId);

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Get all assignments with optional filtering.
     *
     * @param activeOnly If true, return only active assignments
     * @return CompletableFuture with list of assignments
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAllAssignments(boolean activeOnly);

    /**
     * Count total assignments.
     *
     * @param activeOnly If true, count only active assignments
     * @return CompletableFuture with assignment count
     */
    CompletableFuture<OperationResult<Integer>> getAssignmentCount(boolean activeOnly);

    // ==================== USER-BASED QUERIES ====================

    /**
     * Get all assignments for specific user.
     *
     * @param userId     User ID to find assignments for
     * @param activeOnly If true, return only active assignments
     * @return CompletableFuture with list of user's assignments
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByUserId(@NonNull String userId,
                                                                                        boolean activeOnly);

    /**
     * Get current assignments for user (active on specific date).
     * Critical for determining user's current team assignments.
     *
     * @param userId User ID to check
     * @param date   Date to check assignments for
     * @return CompletableFuture with assignments active on specified date
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getCurrentUserAssignments(@NonNull String userId,
                                                                                           @NonNull LocalDate date);

    /**
     * Get user's assignments within date range.
     * Essential for calendar and schedule generation.
     *
     * @param userId    User ID to find assignments for
     * @param startDate Start date of range (inclusive)
     * @param endDate   End date of range (inclusive)
     * @return CompletableFuture with assignments overlapping date range
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getUserAssignmentsInDateRange(
            @NonNull String userId,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate);

    /**
     * Check if user is assigned to any team on specific date.
     * Quick validation for scheduling operations.
     *
     * @param userId User ID to check
     * @param date   Date to check assignment for
     * @return CompletableFuture with true if user has active assignment
     */
    CompletableFuture<OperationResult<Boolean>> isUserAssignedOnDate(@NonNull String userId,
                                                                     @NonNull LocalDate date);

    // ==================== TEAM-BASED QUERIES ====================

    /**
     * Get all assignments for specific team.
     *
     * @param teamId     Team ID to find assignments for
     * @param activeOnly If true, return only active assignments
     * @return CompletableFuture with list of team assignments
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByTeamId(@NonNull String teamId,
                                                                                        boolean activeOnly);

    /**
     * Get current team members (active on specific date).
     * Essential for team roster and capacity planning.
     *
     * @param teamId Team ID to get members for
     * @param date   Date to check assignments for
     * @return CompletableFuture with assignments active on specified date
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getCurrentTeamMembers(@NonNull String teamId,
                                                                                       @NonNull LocalDate date);

    /**
     * Get team assignments within date range.
     * Critical for team schedule planning and resource allocation.
     *
     * @param teamId    Team ID to find assignments for
     * @param startDate Start date of range (inclusive)
     * @param endDate   End date of range (inclusive)
     * @return CompletableFuture with assignments overlapping date range
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getTeamAssignmentsInDateRange(
            @NonNull String teamId,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate);

    /**
     * Count team members on specific date.
     * Quick team capacity check for scheduling decisions.
     *
     * @param teamId Team ID to count members for
     * @param date   Date to check assignments for
     * @return CompletableFuture with number of active team members
     */
    CompletableFuture<OperationResult<Integer>> countTeamMembersOnDate(@NonNull String teamId,
                                                                       @NonNull LocalDate date);

    // ==================== USER-TEAM RELATIONSHIP QUERIES ====================

    /**
     * Check if user is assigned to specific team on date.
     * Precise assignment validation for business logic.
     *
     * @param userId User ID to check
     * @param teamId Team ID to check
     * @param date   Date to check assignment for
     * @return CompletableFuture with assignment if exists, null otherwise
     */
    CompletableFuture<OperationResult<UserTeamAssignment>> getUserTeamAssignment(@NonNull String userId,
                                                                                 @NonNull String teamId,
                                                                                 @NonNull LocalDate date);

    /**
     * Get assignment history between user and team.
     * Useful for audit trails and relationship analysis.
     *
     * @param userId User ID
     * @param teamId Team ID
     * @return CompletableFuture with all assignments between user and team
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentHistory(@NonNull String userId,
                                                                                      @NonNull String teamId);

    /**
     * Validate if assignment would create conflicts.
     * Business rule validation before creating/updating assignments.
     *
     * @param userId              User ID to check
     * @param teamId              Team ID to check
     * @param startDate           Assignment start date
     * @param endDate             Assignment end date (null for permanent)
     * @param excludeAssignmentId Assignment ID to exclude from conflict check (for updates)
     * @return CompletableFuture with validation result and conflict details
     */
    CompletableFuture<OperationResult<AssignmentValidationResult>> validateAssignment(@NonNull String userId,
                                                                                      @NonNull String teamId,
                                                                                      @NonNull LocalDate startDate,
                                                                                      @Nullable LocalDate endDate,
                                                                                      @Nullable String excludeAssignmentId);

    // ==================== STATUS AND DATE-BASED QUERIES ====================

    /**
     * Get assignments by status.
     *
     * @param status     Status to filter by
     * @param activeOnly If true, include only active assignments
     * @return CompletableFuture with assignments matching status
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByStatus(@NonNull Status status,
                                                                                        boolean activeOnly);

    /**
     * Get assignments active within date range.
     * Primary method for calendar and schedule generation.
     *
     * @param startDate Start date of range (inclusive)
     * @param endDate   End date of range (inclusive)
     * @return CompletableFuture with assignments overlapping date range
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsInDateRange(@NonNull LocalDate startDate,
                                                                                           @NonNull LocalDate endDate);

    /**
     * Get assignments starting on specific date.
     *
     * @param startDate Date to find assignments starting on
     * @return CompletableFuture with assignments starting on specified date
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsStartingOn(@NonNull LocalDate startDate);

    /**
     * Get assignments ending on specific date.
     *
     * @param endDate Date to find assignments ending on
     * @return CompletableFuture with assignments ending on specified date
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsEndingOn(@NonNull LocalDate endDate);

    /**
     * Get permanent assignments (no end date).
     *
     * @return CompletableFuture with permanent assignments
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getPermanentAssignments();

    // ==================== MANAGEMENT OPERATIONS ====================

    /**
     * Get assignments created by specific user.
     * Administrative audit and tracking.
     *
     * @param createdByUserId User ID who created assignments
     * @return CompletableFuture with assignments created by user
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsCreatedBy(@NonNull String createdByUserId);

    /**
     * Get recently modified assignments.
     * Change tracking and audit support.
     *
     * @param fromTimestamp Start timestamp (inclusive)
     * @param toTimestamp   End timestamp (inclusive)
     * @return CompletableFuture with assignments modified within time range
     */
    CompletableFuture<OperationResult<List<UserTeamAssignment>>> getModifiedAssignments(long fromTimestamp,
                                                                                        long toTimestamp);

    /**
     * Bulk deactivate assignments for user.
     * Administrative operation for user management.
     *
     * @param userId              User ID to deactivate assignments for
     * @param deactivatedByUserId User performing the deactivation
     * @return CompletableFuture with number of assignments deactivated
     */
    CompletableFuture<OperationResult<Integer>> deactivateUserAssignments(@NonNull String userId,
                                                                          @Nullable String deactivatedByUserId);

    /**
     * Bulk deactivate assignments for team.
     * Administrative operation for team management.
     *
     * @param teamId              Team ID to deactivate assignments for
     * @param deactivatedByUserId User performing the deactivation
     * @return CompletableFuture with number of assignments deactivated
     */
    CompletableFuture<OperationResult<Integer>> deactivateTeamAssignments(@NonNull String teamId,
                                                                          @Nullable String deactivatedByUserId);

    /**
     * Update assignment status in bulk.
     * Administrative status management.
     *
     * @param assignmentIds    List of assignment IDs to update
     * @param status           New status to set
     * @param modifiedByUserId User performing the update
     * @return CompletableFuture with number of assignments updated
     */
    CompletableFuture<OperationResult<Integer>> updateAssignmentStatus(@NonNull List<String> assignmentIds,
                                                                       @NonNull Status status,
                                                                       @Nullable String modifiedByUserId);

    // ==================== CLEANUP OPERATIONS ====================

    /**
     * Delete inactive assignments older than specified timestamp.
     * Administrative cleanup operation.
     *
     * @param beforeTimestamp Delete assignments created before this timestamp
     * @return CompletableFuture with number of assignments deleted
     */
    CompletableFuture<OperationResult<Integer>> cleanupInactiveAssignments(long beforeTimestamp);

    // ==================== INNER CLASSES ====================

    /**
     * Assignment validation result container.
     * Provides detailed feedback for assignment conflict checking.
     */
    class AssignmentValidationResult {
        public boolean isValid=false;
        public String validationMessage;
        public List<UserTeamAssignment> conflictingAssignments;

        public AssignmentValidationResult(
            boolean isValid,
            String validationMessage,
            List<UserTeamAssignment> conflictingAssignments)
        {
            this.isValid = isValid;
            this.validationMessage = validationMessage;
            this.conflictingAssignments = conflictingAssignments;
        }
        public boolean isValid() { return isValid; }
        public String validationMessage() { return validationMessage; }
        public List<UserTeamAssignment> conflictingAssignments() { return conflictingAssignments; }
        public boolean hasConflicts() { return !conflictingAssignments.isEmpty(); }

        @NonNull
        @Override
        public String toString() {
            return "AssignmentValidationResult{" +
                    "valid=" + isValid +
                    ", message='" + validationMessage + '\'' +
                    ", conflicts=" + conflictingAssignments.size() +
                    '}';
        }
    }
}