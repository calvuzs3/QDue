package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * UserScheduleAssignmentRepository - Clean Architecture repository interface for user assignment persistence.
 *
 * <p>Defines business-oriented operations for managing user-to-team schedule assignments with
 * focus on team coordination, time boundaries, and multi-user scheduling. All operations
 * return {@code OperationResult<T>} for consistent error handling and use {@code CompletableFuture}
 * for asynchronous execution.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Assignment Management</strong>: CRUD operations for user-team assignments</li>
 *   <li><strong>Date-Based Queries</strong>: Efficient queries for schedule generation</li>
 *   <li><strong>Team Coordination</strong>: Team roster and membership management</li>
 *   <li><strong>Time Boundaries</strong>: Support for temporary and permanent assignments</li>
 *   <li><strong>Priority Resolution</strong>: Handle conflicting assignments with priorities</li>
 * </ul>
 *
 * <h3>Business Use Cases:</h3>
 * <ul>
 *   <li><strong>Schedule Generation</strong>: Determine user's team for specific dates</li>
 *   <li><strong>Team Management</strong>: Manage team rosters and assignments</li>
 *   <li><strong>User Interface</strong>: Display user's current and future assignments</li>
 *   <li><strong>Transfer Management</strong>: Handle team transfers and role changes</li>
 *   <li><strong>Reporting</strong>: Assignment analytics and team composition</li>
 * </ul>
 *
 * <h3>Assignment Types Supported:</h3>
 * <ul>
 *   <li><strong>Permanent Assignments</strong>: No end date, indefinite duration</li>
 *   <li><strong>Temporary Assignments</strong>: Fixed start and end dates</li>
 *   <li><strong>Team Transfers</strong>: High-priority assignments that override existing ones</li>
 *   <li><strong>Role-Based Assignments</strong>: Assignments with specific roles within teams</li>
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
public interface UserScheduleAssignmentRepository {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Get user schedule assignment by unique identifier.
     *
     * <p>Retrieves a single assignment by its ID. Returns a failure result
     * if the assignment is not found or an error occurs during retrieval.</p>
     *
     * @param assignmentId Unique assignment identifier
     * @return CompletableFuture with OperationResult containing UserScheduleAssignment or error
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> getUserScheduleAssignmentById(@NonNull String assignmentId);

    /**
     * Save a user schedule assignment to persistent storage.
     *
     * <p>Creates a new assignment or updates an existing one based on the ID.
     * Validates business rules and updates timestamps automatically.</p>
     *
     * @param assignment UserScheduleAssignment domain model to save
     * @return CompletableFuture with OperationResult containing saved UserScheduleAssignment
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> insertUserScheduleAssignment(@NonNull UserScheduleAssignment assignment);

    /**
     * Delete user schedule assignment by identifier.
     *
     * <p>Permanently removes an assignment from storage. Returns a failure
     * result if the assignment is not found.</p>
     *
     * @param assignmentId Assignment ID to delete
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> deleteUserScheduleAssignment(@NonNull String assignmentId);

    /**
     * Update user schedule assignment.
     *
     * <p>Updates an existing assignment with new data. Returns a failure
     * result if the assignment is not found or an error occurs during update.</p>
     *
     * @param assignment Updated UserScheduleAssignment domain model
     * @return CompletableFuture with OperationResult containing updated UserScheduleAssignment
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> updateUserScheduleAssignment(@NonNull UserScheduleAssignment assignment);

    // ==================== USER-SPECIFIC QUERIES ====================


    /**
     * Get active assignments for specific user.
     * Returns assignments that are currently active or will be active in future.
     *
     * @param userId User ID to get assignments for
     * @return CompletableFuture with OperationResult containing active assignments
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getUserActiveAssignments(@NonNull Long userId);

    /**
     * Get active assignment for user on specific date.
     *
     * <p>Returns the highest-priority active assignment for a user on the specified date.
     * Used during schedule generation to determine user's team assignment.
     * Returns null if no active assignment found.</p>
     *
     * @param userId User identifier
     * @param date Target date for assignment lookup
     * @return CompletableFuture with OperationResult containing UserScheduleAssignment or null
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> getActiveAssignmentForUser(
            @NonNull Long userId, @NonNull LocalDate date);

    /**
     * Get all active assignments for user (all time periods).
     *
     * <p>Retrieves all assignments with ACTIVE status for a user, regardless of dates.
     * Useful for user profile displays and assignment management interfaces.</p>
     *
     * @param userId User identifier
     * @return CompletableFuture with OperationResult containing List of active UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsForUser(@NonNull Long userId);

    /**
     * Get assignments for user in date range.
     *
     * <p>Retrieves all assignments (regardless of status) for a user within
     * the specified date range. Useful for historical analysis and reporting.</p>
     *
     * @param userId User identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with OperationResult containing List of UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsForUserInDateRange(
            @NonNull Long userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    // ==================== DATE-BASED QUERIES ====================

    /**
     * Get all active assignments for specific date across all users.
     *
     * <p>Returns all assignments that are active on the specified date.
     * Used for schedule generation and team roster displays.</p>
     *
     * @param date Target date for assignments
     * @return CompletableFuture with OperationResult containing List of active UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsForDate(@NonNull LocalDate date);

    /**
     * Get all active assignments in date range across all users.
     *
     * <p>Retrieves all active assignments within the specified date range.
     * Useful for bulk schedule operations and analytics.</p>
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with OperationResult containing List of UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsInDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    // ==================== TEAM-BASED QUERIES ====================

    /**
     * Get all assignments by team ID.
     *
     * <p>Retrieves all assignments (past, current, and future) for a specific team.
     * Useful for team management and roster planning.</p>
     *
     * @param teamId Team identifier
     * @return CompletableFuture with OperationResult containing List of UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsByTeam(@NonNull String teamId);

    /**
     * Get assignments by recurrence rule.
     *
     * <p>Retrieves all assignments linked to a specific recurrence rule.
     * Useful for recurrence pattern management and bulk operations.</p>
     *
     * @param recurrenceRuleId Recurrence rule identifier
     * @return CompletableFuture with OperationResult containing List of UserScheduleAssignment objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsByRecurrenceRule(
            @NonNull String recurrenceRuleId);

    /**
     * Get all active user IDs across all assignments.
     *
     * <p>Returns a list of all user IDs that have at least one active assignment.
     * Useful for system administration and user management.</p>
     *
     * @return CompletableFuture with OperationResult containing List of active user IDs
     */
    @NonNull
    CompletableFuture<OperationResult<List<Long>>> getAllActiveUserIds();

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Update assignment status.
     *
     * <p>Changes the status of an assignment and updates the timestamp.
     * Used for assignment lifecycle management.</p>
     *
     * @param assignmentId Assignment identifier
     * @param newStatus New assignment status
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> updateAssignmentStatus(
            @NonNull String assignmentId, @NonNull UserScheduleAssignment.Status newStatus);

    /**
     * End assignment on specific date.
     *
     * <p>Sets an end date for a permanent assignment, effectively terminating it.
     * Commonly used for team transfers and role changes.</p>
     *
     * @param assignmentId Assignment identifier
     * @param endDate End date to set
     * @return CompletableFuture with OperationResult containing success boolean
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> endAssignment(@NonNull String assignmentId, @NonNull LocalDate endDate);

    // ==================== VALIDATION AND CHECKS ====================

    /**
     * Check if user has any active assignments.
     *
     * <p>Quick check to determine if a user is currently assigned to any teams.
     * Used for user validation and business rule enforcement.</p>
     *
     * @param userId User identifier
     * @return CompletableFuture with OperationResult containing boolean indicating active assignments
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> hasActiveAssignments(@NonNull Long userId);

    /**
     * Check if team has any active assignments.
     *
     * <p>Quick check to determine if a team currently has any assigned users.
     * Used for team validation and deletion rules.</p>
     *
     * @param teamId Team identifier
     * @return CompletableFuture with OperationResult containing boolean indicating active assignments
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> teamHasActiveAssignments(@NonNull String teamId);

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get total assignment count for team.
     *
     * <p>Returns the total number of assignments (all statuses) for a team.
     * Useful for team analytics and capacity planning.</p>
     *
     * @param teamId Team identifier
     * @return CompletableFuture with OperationResult containing assignment count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getAssignmentCountForTeam(@NonNull String teamId);

    // ==================== FUTURE EXTENSIONS ====================

    // Note: Additional methods for future functionality:
    // - getConflictingAssignments() - Find overlapping assignments for conflict resolution
    // - getAssignmentsByPriority() - Priority-based filtering
    // - getAssignmentHistory() - Historical assignment tracking
    // - bulkCreateAssignments() - Batch assignment operations
    // - getTeamCapacity() - Team size and capacity metrics
    // - getAssignmentsByDepartment() - Department-based grouping
}