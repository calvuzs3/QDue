package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;

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

/**
 * GetTeamMembersUseCase - Retrieve team membership information
 */
public class GetTeamMembersUseCase {

    private static final String TAG = "GetTeamMembersUseCase";

    private final UserTeamAssignmentRepository mAssignmentRepository;
    private final QDueUserRepository mQDueUserRepository;
    private final TeamRepository mTeamRepository;
    private final ExecutorService mExecutorService;

    public GetTeamMembersUseCase(@NonNull UserTeamAssignmentRepository assignmentRepository,
                                 @NonNull QDueUserRepository userRepository,
                                 @NonNull TeamRepository teamRepository) {
        this.mAssignmentRepository = assignmentRepository;
        this.mQDueUserRepository = userRepository;
        this.mTeamRepository = teamRepository;
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Get complete team member information with user details.
     *
     * @param teamID Team ID to get members for
     * @param date Date to check assignments for
     * @return CompletableFuture with team member details
     */
    public CompletableFuture<OperationResult<TeamMembersResult>> getTeamMembersWithDetails(
            @NonNull String teamID,
            @NonNull LocalDate date) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting team members with details for team: " + teamID + " on date: " + date);

                // 1. Get team info
                Team team = mTeamRepository.getTeamById(teamID).get();
                if (team == null) {
                    return OperationResult.failure("Team not found: " + teamID,
                            OperationResult.OperationType.READ);
                }

                // 2. Get current assignments
                List<UserTeamAssignment> assignments = mAssignmentRepository
                        .getCurrentTeamMembers(teamID, date).get().getData();

                if (assignments == null) {
                    assignments = new ArrayList<>();
                }

                // 3. Get user details for all assignments
                List<TeamMemberDetail> memberDetails = new ArrayList<>();
                for (UserTeamAssignment assignment : assignments) {
                    QDueUser user = mQDueUserRepository.readUser(assignment.getUserID()).get().getData();
                    if (user != null) {
                        memberDetails.add(new TeamMemberDetail(assignment, user));
                    }
                }

                // 4. Build result
                TeamMembersResult result = new TeamMembersResult(team, memberDetails, date);

                Log.d(TAG, "✅ Found " + memberDetails.size() + " team members for team: " + teamID);
                return OperationResult.success(result, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting team members with details", e);
                return OperationResult.failure("Failed to get team members: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
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

    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}
