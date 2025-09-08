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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class UserTeamAssignmentUseCases {

    // TAG
    private static final String TAG = "UserTeamAssignmentUseCases";

    // Executor
    private final ExecutorService mExecutorService;

    // ==================== DEPENDENCIES ====================

    private final UserTeamAssignmentRepository mAssignmentRepository;
    private final QDueUserRepository mQDueUserRepository;
    private final TeamRepository mTeamRepository;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param qDueUserRepository Repository for QDueUser operations
     */
    public UserTeamAssignmentUseCases(
            @NonNull QDueUserRepository qDueUserRepository,
            @NonNull UserTeamAssignmentRepository userTeamAssignmentRepository,
            @NonNull TeamRepository teamRepository
    ) {
        // Repositories
        this.mQDueUserRepository = qDueUserRepository;
        this.mAssignmentRepository = userTeamAssignmentRepository;
        this.mTeamRepository = teamRepository;

        // Threads
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    /**
     * Get user team assignment use case.
     */
    public class GetUserTeamAssignmentUseCase {
        // TAG
        private static final String TAG = "getUserTeamAssignmentUseCase";

        /**
         * Get user team assignment.
         *
         * @param assignmentID Assignment ID to get
         * @return CompletableFuture with user team assignment
         */
        public CompletableFuture<OperationResult<UserTeamAssignment>> execute(
                @NonNull String assignmentID
        ) {
            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Getting user team assignment: " + assignmentID );

                    OperationResult<UserTeamAssignment> result = mAssignmentRepository.getAssignmentById( assignmentID ).join();
                    if (result.isSuccess() && result.hasData())  {
                        assert result.getData() != null;
                        return OperationResult.success( result.getData(), OperationResult.OperationType.READ );
                    } else {
                        throw new IllegalStateException( "Failed to get user team assignment: " + result.getErrorMessage() );
                    }
                } catch (Exception e) {
                    Log.e( TAG, "Error getting user team assignment", e );
                    return OperationResult.failure( "Failed to get user team assignment: " + e.getMessage(),
                            OperationResult.OperationType.READ );
                }
            }, mExecutorService );
        }
    }

    /**
     * Get user team assignment for date use case.
     */
    public class GetUserTeamAssignmentForDateUseCase {
        //TAG
        private static final String TAG = "getTeamForUserAssignmentInDateUseCase";

        /**
         * Get user team assignment for date.
         *
         * @param userID User ID to get assignment for
         * @param date   Date to get assignment for
         * @return CompletableFuture with user team assignment
         */
        public CompletableFuture<OperationResult<UserTeamAssignment>> execute(
                @NonNull String userID,
                @NonNull LocalDate date
        ) {
            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Getting user team assignment for user: " + userID + " on date: " + date );

                    OperationResult<List<UserTeamAssignment>> result = mAssignmentRepository.getCurrentUserAssignments( userID, date ).join();

                    if (result.isSuccess()) {
                        List<UserTeamAssignment> assignments = result.getData();
                        if (!assignments.isEmpty()) {
                            return OperationResult.success( assignments.get( 0 ), OperationResult.OperationType.READ );
                        } else {
                            throw new IllegalStateException( "No assignments found for user: " + userID + " on date: " + date );
                        }
                    } else {
                        throw new IllegalStateException( "Failed to get user team assignment: " + result.getErrorMessage() );
                    }
                } catch (Exception e) {
                    Log.e( TAG, "Error getting user team assignment", e );
                    return OperationResult.failure( "Failed to get user team assignment: " + e.getMessage(),
                            OperationResult.OperationType.READ );
                }
            }, mExecutorService );
        }
    }

    /**
     * Create user-team assignment with full validation.
     */
    public class CreateUserTeamAssignmentUseCase {
        //TAG
        private static final String TAG = "CreateUserTeamAssignmentUseCase";

        /**
         * Create user-team assignment with full validation.
         *
         * @param userID           User ID to assign
         * @param teamID           Team ID to assign to
         * @param startDate        Assignment start date
         * @param endDate          Assignment end date (null for permanent)
         * @param title            Assignment title (null for default)
         * @param assignedByUserID User creating the assignment
         * @return CompletableFuture with created assignment
         */
        public CompletableFuture<OperationResult<UserTeamAssignment>> execute(
                @NonNull String userID,
                @NonNull String teamID,
                @NonNull LocalDate startDate,
                @Nullable LocalDate endDate,
                @Nullable String title,
                @Nullable String assignedByUserID) {
            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Creating assignment: user=" + userID + ", team=" + teamID + ", start=" + startDate );

                    // 1. Validate user exists
                    QDueUser user = mQDueUserRepository.readUser( userID ).get().getData();
                    if (user == null) {
                        return OperationResult.failure( "User not found: " + userID,
                                OperationResult.OperationType.CREATE );
                    }

                    // 2. Validate team exists
                    Team team = mTeamRepository.getTeamById( teamID ).get();
                    if (team == null) {
                        return OperationResult.failure( "Team not found: " + teamID,
                                OperationResult.OperationType.CREATE );
                    }

                    // 3. Create assignment with computed status
                    UserTeamAssignment assignment = UserTeamAssignment.builder()
                            .userID( userID )
                            .teamID( teamID )
                            .startDate( startDate )
                            .endDate( endDate )
                            .title( title != null ? title : user.getDisplayName() + " → " + team.getName() )
                            .notes( "CreateUserTeamAssignmentUseCase" )
                            .assignedByUserID( assignedByUserID == null ? userID : assignedByUserID )
                            .computeStatus()
                            .build();

                    // 4. Create assignment through repository (includes validation)
                    Log.d( TAG, "✅ Assignment created successfully" );
                    return mAssignmentRepository.createAssignment( assignment ).get();
                } catch (Exception e) {
                    Log.e( TAG, "Error creating assignment", e );
                    return OperationResult.failure( "Failed to create assignment: " + e.getMessage(),
                            OperationResult.OperationType.CREATE );
                }
            }, mExecutorService );
        }
    }

    public class UpdateUserTeamAssignmentUseCase {
        // TAG
        private static final String TAG = "UpdateUserTeamAssignmentUseCase";

        /**
         * Update assignment with validation.
         *
         * @param assignmentId     Assignment ID to update
         * @param newStartDate     New start date (optional)
         * @param newEndDate       New end date (optional)
         * @param newTitle         New title (optional)
         * @param modifiedByUserId User making the change
         * @return CompletableFuture with updated assignment
         */
        public CompletableFuture<OperationResult<UserTeamAssignment>> execute(
                @NonNull String assignmentId,
                @Nullable LocalDate newStartDate,
                @Nullable LocalDate newEndDate,
                @Nullable String newTitle,
                @NonNull String modifiedByUserId) {

            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Updating assignment: " + assignmentId );

                    // 1. Get existing assignment
                    UserTeamAssignment existing = mAssignmentRepository.getAssignmentById( assignmentId ).get().getData();
                    if (existing == null) {
                        return OperationResult.failure( "Assignment not found: " + assignmentId,
                                OperationResult.OperationType.READ );
                    }

                    // 2. Build updated assignment
                    UserTeamAssignment.Builder builder = UserTeamAssignment.builder()
                            .copyFrom( existing )
                            .lastModifiedByUserId( modifiedByUserId )
                            .updatedAt( System.currentTimeMillis() );

                    if (newStartDate != null) {
                        builder.startDate( newStartDate );
                    }
                    if (newEndDate != null) {
                        builder.endDate( newEndDate );
                    }
                    if (newTitle != null) {
                        builder.title( newTitle );
                    }

                    UserTeamAssignment updatedAssignment = builder
                            .computeStatus()
                            .build();

                    // 3. Update through repository
                    return mAssignmentRepository.updateAssignment( updatedAssignment ).get();
                } catch (Exception e) {
                    Log.e( TAG, "Error updating assignment", e );
                    return OperationResult.failure( "Failed to update assignment: " + e.getMessage(),
                            OperationResult.OperationType.UPDATE );
                }
            }, mExecutorService );
        }
    }

    public class TransferToTeamUserTeamAssignmentUseCase {
        // TAG
        private static final String TAG = "TransferToTeamUserTeamAssignmentUseCase";

        /**
         * Transfer user from one team to another.
         * Handles ending current assignment and creating new one.
         *
         * @param userId           User to transfer
         * @param fromTeamId       Current team ID
         * @param toTeamId         New team ID
         * @param transferDate     Date of transfer
         * @param transferByUserId User performing transfer
         * @return CompletableFuture with transfer result
         */
        public CompletableFuture<OperationResult<TeamTransferResult>> execute(
                @NonNull String userId,
                @NonNull String fromTeamId,
                @NonNull String toTeamId,
                @NonNull LocalDate transferDate,
                @Nullable String transferByUserId) {

            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Transferring user " + userId + " from team " + fromTeamId + " to team " + toTeamId );

                    // 1. Get current assignment
                    UserTeamAssignment currentAssignment = mAssignmentRepository
                            .getUserTeamAssignment( userId, fromTeamId, transferDate.minusDays( 1 ) )
                            .get().getData();

                    if (currentAssignment == null) {
                        return OperationResult.failure( "No current assignment found for user in source team",
                                OperationResult.OperationType.READ );
                    }

                    // 2. End current assignment
                    LocalDate endDate = transferDate.minusDays( 1 );
                    UserTeamAssignment endedAssignment = UserTeamAssignment.builder()
                            .copyFrom( currentAssignment )
                            .endDate( endDate )
                            .lastModifiedByUserId( transferByUserId )
                            .computeStatus()
                            .build();

                    OperationResult<UserTeamAssignment> endResult = mAssignmentRepository.updateAssignment( endedAssignment ).get();
                    if (!endResult.isSuccess()) {
                        return OperationResult.failure( "Failed to end current assignment: " + endResult.getErrorMessage(),
                                OperationResult.OperationType.UPDATE );
                    }

                    // 3. Create new assignment
                    OperationResult<UserTeamAssignment> createResult = getCreateUserTeamAssignmentUseCase().execute(
                            userId, toTeamId, transferDate, null,
                            "Transfer from " + fromTeamId, transferByUserId
                    ).get();

                    if (!createResult.isSuccess()) {
                        return OperationResult.failure( "Failed to create new assignment: " + createResult.getErrorMessage(),
                                OperationResult.OperationType.CREATE );
                    }

                    // 4. Return transfer result
                    TeamTransferResult transferResult = new TeamTransferResult(
                            endResult.getData(), createResult.getData(), transferDate );

                    Log.d( TAG, "✅ User transfer completed successfully" );
                    return OperationResult.success( transferResult,
                            OperationResult.OperationType.UPDATE );
                } catch (Exception e) {
                    Log.e( TAG, "Error transferring user to team", e );
                    return OperationResult.failure( "Failed to transfer user: " + e.getMessage(),
                            OperationResult.OperationType.UPDATE );
                }
            }, mExecutorService );
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
    }

    public class GetTeamMembersUseCase {
        // TAG
        private static final String TAG = "GetTeamMembersUseCase";

        /**
         * Get complete team member information with user details.
         *
         * @param teamID Team ID to get members for
         * @param date   Date to check assignments for
         * @return CompletableFuture with team member details
         */
        public CompletableFuture<OperationResult<TeamMembersResult>> execute(
                @NonNull String teamID,
                @NonNull LocalDate date) {

            return CompletableFuture.supplyAsync( () -> {
                try {
                    Log.d( TAG, "Getting team members with details for team: " + teamID + " on date: " + date );

                    // 1. Get team info
                    Team team = mTeamRepository.getTeamById( teamID ).get();
                    if (team == null) {
                        return OperationResult.failure( "Team not found: " + teamID,
                                OperationResult.OperationType.READ );
                    }

                    // 2. Get current assignments
                    List<UserTeamAssignment> assignments = mAssignmentRepository
                            .getCurrentTeamMembers( teamID, date ).get().getData();

                    if (assignments == null) {
                        assignments = new ArrayList<>();
                    }

                    // 3. Get user details for all assignments
                    List<TeamMemberDetail> memberDetails = new ArrayList<>();
                    for (UserTeamAssignment assignment : assignments) {
                        QDueUser user = mQDueUserRepository.readUser( assignment.getUserID() ).get().getData();
                        if (user != null) {
                            memberDetails.add( new TeamMemberDetail( assignment, user ) );
                        }
                    }

                    // 4. Build result
                    TeamMembersResult result = new TeamMembersResult( team, memberDetails, date );

                    Log.d( TAG, "✅ Found " + memberDetails.size() + " team members for team: " + teamID );
                    return OperationResult.success( result, OperationResult.OperationType.READ );
                } catch (Exception e) {
                    Log.e( TAG, "Error getting team members with details", e );
                    return OperationResult.failure( "Failed to get team members: " + e.getMessage(),
                            OperationResult.OperationType.READ );
                }
            }, mExecutorService );
        }

        // ==================== INNER CLASSES ====================

        /**
         * Team member detail container.
         */
        public record TeamMemberDetail(UserTeamAssignment assignment, QDueUser user) {

            public String getDisplayName() {
                return user.getDisplayName();
            }

            public boolean isPermanentMember() {
                return assignment.getEndDate() == null;
            }

            @NonNull
            @Override
            public String toString() {
                return "TeamMemberDetail{" +
                        "user=" + user.getNickname() +
                        ", assignmentId=" + assignment.getId() +
                        ", permanent=" + isPermanentMember() +
                        '}';
            }
        }

        /**
         * Team members result container.
         */
        public record TeamMembersResult(Team team, List<TeamMemberDetail> members,
                                        LocalDate queryDate) {

            public int getMemberCount() {
                return members.size();
            }

            public long getPermanentMemberCount() {
                return members.stream().filter( TeamMemberDetail::isPermanentMember ).count();
            }

            public List<String> getMemberNames() {
                return members.stream()
                        .map( TeamMemberDetail::getDisplayName )
                        .collect( Collectors.toList() );
            }

            @NonNull
            @Override
            public String toString() {
                return "TeamMembersResult{" +
                        "team=" + team.getName() +
                        ", memberCount=" + getMemberCount() +
                        ", queryDate=" + queryDate +
                        '}';
            }
        }
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

    // ==================== USE CASE INSTANCE PROVIDERS ====================

    @NonNull
    public GetUserTeamAssignmentUseCase getGetUserTeamAssignmentUseCase() {
        return new GetUserTeamAssignmentUseCase();
    }

    @NonNull
    public GetUserTeamAssignmentForDateUseCase getGetUserTeamAssignmentForDateUseCase() {
        return new GetUserTeamAssignmentForDateUseCase();
    }

    @NonNull
    public CreateUserTeamAssignmentUseCase getCreateUserTeamAssignmentUseCase() {
        return new CreateUserTeamAssignmentUseCase();
    }

    @NonNull
    public UpdateUserTeamAssignmentUseCase getUpdateUserTeamAssignmentUseCase() {
        return new UpdateUserTeamAssignmentUseCase();
    }

    @NonNull
    public TransferToTeamUserTeamAssignmentUseCase getTransferToTeamUserTeamAssignmentUseCase() {
        return new TransferToTeamUserTeamAssignmentUseCase();
    }

    // ==================== CLEANUP ====================

    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}
