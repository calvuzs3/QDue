package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserTeamAssignmentRepository;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ManageUserTeamAssignmentsUseCase - Business Logic for User-Team Assignment Management
 *
 * <p>Orchestrates complex user-team assignment operations including validation, conflict resolution,
 * and multi-repository coordination following clean architecture principles.</p>
 *
 * <h3>Business Operations:</h3>
 * <ul>
 *   <li><strong>Assignment Creation</strong>: Create assignments with full validation</li>
 *   <li><strong>Bulk Operations</strong>: Handle multiple assignments in single transaction</li>
 *   <li><strong>Conflict Resolution</strong>: Detect and resolve assignment conflicts</li>
 *   <li><strong>Status Management</strong>: Automatic status computation and updates</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Use Case
 * @since Clean Architecture Phase 3
 */
public class ManageUserTeamAssignmentsUseCase {

    private static final String TAG = "ManageUserTeamAssignmentsUseCase";

    // ==================== DEPENDENCIES ====================

    private final UserTeamAssignmentRepository mAssignmentRepository;
    private final QDueUserRepository qDueUserRepository;
    private final TeamRepository mTeamRepository;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    public ManageUserTeamAssignmentsUseCase(@NonNull UserTeamAssignmentRepository assignmentRepository,
                                            @NonNull QDueUserRepository userRepository,
                                            @NonNull TeamRepository teamRepository) {
        this.mAssignmentRepository = assignmentRepository;
        this.qDueUserRepository = userRepository;
        this.mTeamRepository = teamRepository;
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    // ==================== ASSIGNMENT CREATION ====================

    /**
     * Create user-team assignment with full validation.
     *
     * @param userId User ID to assign
     * @param teamId Team ID to assign to
     * @param startDate Assignment start date
     * @param endDate Assignment end date (null for permanent)
     * @param title Optional assignment title
     * @param assignedByUserId User creating the assignment
     * @return CompletableFuture with created assignment
     */
    public CompletableFuture<OperationResult<UserTeamAssignment>> createAssignment(
            @NonNull String userId,
            @NonNull String teamId,
            @NonNull LocalDate startDate,
            @Nullable LocalDate endDate,
            @Nullable String title,
            @Nullable String assignedByUserId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating assignment: user=" + userId + ", team=" + teamId + ", start=" + startDate);

                // 1. Validate user exists
                QDueUser user = qDueUserRepository.getUserById(userId).get().getData();
                if (user == null) {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.CREATE);
                }

                // 2. Validate team exists
                Team team = mTeamRepository.getTeamById(teamId).get();
                if (team == null) {
                    return OperationResult.failure("Team not found: " + teamId,
                            OperationResult.OperationType.CREATE);
                }

                // 3. Create assignment with computed status
                UserTeamAssignment assignment = UserTeamAssignment.builder()
                        .userID(userId)
                        .teamID(teamId)
                        .startDate(startDate)
                        .endDate(endDate)
                        .title(title != null ? title : "Assignment: " + user.getDisplayName() + " → " + team.getName())
                        .assignedByUserID(assignedByUserId)
                        .computeStatus()
                        .build();

                // 4. Create assignment through repository (includes validation)
                return mAssignmentRepository.createAssignment(assignment).get();

            } catch (Exception e) {
                Log.e(TAG, "Error creating assignment", e);
                return OperationResult.failure("Failed to create assignment: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    /**
     * Create multiple assignments in single transaction.
     *
     * @param assignmentRequests List of assignment creation requests
     * @return CompletableFuture with created assignments
     */
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> createBulkAssignments(
            @NonNull List<AssignmentCreationRequest> assignmentRequests) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating " + assignmentRequests.size() + " assignments in bulk");

                // 1. Validate all users and teams exist
                for (AssignmentCreationRequest request : assignmentRequests) {
                    QDueUser user = qDueUserRepository.getUserById(request.userId).get().getData();
                    if (user == null) {
                        return OperationResult.failure("User not found: " + request.userId,
                                OperationResult.OperationType.READ);
                    }

                    Team team = mTeamRepository.getTeamById(request.teamId).get();
                    if (team == null) {
                        return OperationResult.failure("Team not found: " + request.teamId,
                                OperationResult.OperationType.READ);
                    }
                }

                // 2. Build assignments
                List<UserTeamAssignment> assignments = assignmentRequests.stream()
                        .map(request -> UserTeamAssignment.builder()
                                .userID(request.userId)
                                .teamID(request.teamId)
                                .startDate(request.startDate)
                                .endDate(request.endDate)
                                .title(request.title)
                                .assignedByUserID(request.assignedByUserId)
                                .computeStatus()
                                .build())
                        .collect(Collectors.toList());

                // 3. Create all assignments
                return mAssignmentRepository.createAssignments(assignments).get();

            } catch (Exception e) {
                Log.e(TAG, "Error creating bulk assignments", e);
                return OperationResult.failure("Failed to create bulk assignments: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    // ==================== ASSIGNMENT MANAGEMENT ====================

    /**
     * Update assignment with validation.
     *
     * @param assignmentId Assignment ID to update
     * @param newStartDate New start date (optional)
     * @param newEndDate New end date (optional)
     * @param newTitle New title (optional)
     * @param modifiedByUserId User making the change
     * @return CompletableFuture with updated assignment
     */
    public CompletableFuture<OperationResult<UserTeamAssignment>> updateAssignment(
            @NonNull String assignmentId,
            @Nullable LocalDate newStartDate,
            @Nullable LocalDate newEndDate,
            @Nullable String newTitle,
            @Nullable String modifiedByUserId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Updating assignment: " + assignmentId);

                // 1. Get existing assignment
                UserTeamAssignment existing = mAssignmentRepository.getAssignmentById(assignmentId).get().getData();
                if (existing == null) {
                    return OperationResult.failure("Assignment not found: " + assignmentId,
                            OperationResult.OperationType.READ);
                }

                // 2. Build updated assignment
                UserTeamAssignment.Builder builder = UserTeamAssignment.builder()
                        .copyFrom(existing)
                        .lastModifiedByUserId(modifiedByUserId)
                        .updatedAt(System.currentTimeMillis());

                if (newStartDate != null) {
                    builder.startDate(newStartDate);
                }
                if (newEndDate != null) {
                    builder.endDate(newEndDate);
                }
                if (newTitle != null) {
                    builder.title(newTitle);
                }

                UserTeamAssignment updatedAssignment = builder
                        .computeStatus()
                        .build();

                // 3. Update through repository
                return mAssignmentRepository.updateAssignment(updatedAssignment).get();

            } catch (Exception e) {
                Log.e(TAG, "Error updating assignment", e);
                return OperationResult.failure("Failed to update assignment: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    /**
     * Transfer user from one team to another.
     * Handles ending current assignment and creating new one.
     *
     * @param userId User to transfer
     * @param fromTeamId Current team ID
     * @param toTeamId New team ID
     * @param transferDate Date of transfer
     * @param transferByUserId User performing transfer
     * @return CompletableFuture with transfer result
     */
    public CompletableFuture<OperationResult<TeamTransferResult>> transferUserToTeam(
            @NonNull String userId,
            @NonNull String fromTeamId,
            @NonNull String toTeamId,
            @NonNull LocalDate transferDate,
            @Nullable String transferByUserId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Transferring user " + userId + " from team " + fromTeamId + " to team " + toTeamId);

                // 1. Get current assignment
                UserTeamAssignment currentAssignment = mAssignmentRepository
                        .getUserTeamAssignment(userId, fromTeamId, transferDate.minusDays(1))
                        .get().getData();

                if (currentAssignment == null) {
                    return OperationResult.failure("No current assignment found for user in source team",
                            OperationResult.OperationType.READ);
                }

                // 2. End current assignment
                LocalDate endDate = transferDate.minusDays(1);
                UserTeamAssignment endedAssignment = UserTeamAssignment.builder()
                        .copyFrom(currentAssignment)
                        .endDate(endDate)
                        .lastModifiedByUserId(transferByUserId)
                        .computeStatus()
                        .build();

                OperationResult<UserTeamAssignment> endResult = mAssignmentRepository.updateAssignment(endedAssignment).get();
                if (!endResult.isSuccess()) {
                    return OperationResult.failure("Failed to end current assignment: " + endResult.getErrorMessage(),
                            OperationResult.OperationType.UPDATE);
                }

                // 3. Create new assignment
                OperationResult<UserTeamAssignment> createResult = createAssignment(
                        userId, toTeamId, transferDate, null,
                        "Transfer from " + fromTeamId, transferByUserId
                ).get();

                if (!createResult.isSuccess()) {
                    return OperationResult.failure("Failed to create new assignment: " + createResult.getErrorMessage(),
                            OperationResult.OperationType.CREATE);
                }

                // 4. Return transfer result
                TeamTransferResult transferResult = new TeamTransferResult(
                        endResult.getData(), createResult.getData(), transferDate);

                Log.d(TAG, "✅ User transfer completed successfully");
                return OperationResult.success(transferResult,
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Error transferring user to team", e);
                return OperationResult.failure("Failed to transfer user: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    // ==================== INNER CLASSES ====================

    /**
     * Assignment creation request container.
     */
    public static class AssignmentCreationRequest {
        public final String userId;
        public final String teamId;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final String title;
        public final String assignedByUserId;

        public AssignmentCreationRequest(@NonNull String userId,
                                         @NonNull String teamId,
                                         @NonNull LocalDate startDate,
                                         @Nullable LocalDate endDate,
                                         @Nullable String title,
                                         @Nullable String assignedByUserId) {
            this.userId = userId;
            this.teamId = teamId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.title = title;
            this.assignedByUserId = assignedByUserId;
        }
    }

    /**
     * Team transfer result container.
     */
    public static class TeamTransferResult {
        public final UserTeamAssignment endedAssignment;
        public final UserTeamAssignment newAssignment;
        public final LocalDate transferDate;

        public TeamTransferResult(@NonNull UserTeamAssignment endedAssignment,
                                  @NonNull UserTeamAssignment newAssignment,
                                  @NonNull LocalDate transferDate) {
            this.endedAssignment = endedAssignment;
            this.newAssignment = newAssignment;
            this.transferDate = transferDate;
        }

        @NonNull
        @Override
        public String toString() {
            return "TeamTransferResult{" +
                    "user=" + newAssignment.getUserID() +
                    ", fromTeam=" + endedAssignment.getTeamID() +
                    ", toTeam=" + newAssignment.getTeamID() +
                    ", transferDate=" + transferDate +
                    '}';
        }
    }

    // ==================== CLEANUP ====================

    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}
