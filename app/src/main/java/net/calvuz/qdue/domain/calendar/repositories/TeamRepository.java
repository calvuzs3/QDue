package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.Team;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TeamRepository - Domain interface for team management and user-team relationships.
 *
 * <p>Defines business-oriented operations for managing teams and their assignments
 * to users within the work scheduling system. Focuses on team-centric operations
 * and user membership management.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Team Management</strong>: CRUD operations for team entities</li>
 *   <li><strong>User Assignment</strong>: Managing user-team relationships</li>
 *   <li><strong>Team Lookup</strong>: Finding teams by various criteria</li>
 *   <li><strong>Membership Queries</strong>: User membership and team composition</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><strong>Active Teams</strong>: Only active teams participate in scheduling</li>
 *   <li><strong>User Assignment</strong>: Users can belong to multiple teams</li>
 *   <li><strong>Team Uniqueness</strong>: Team names must be unique within system</li>
 *   <li><strong>Soft Deletion</strong>: Teams are deactivated rather than deleted</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Domain Interface
 * @since Clean Architecture Phase 2
 */
public interface TeamRepository {

    // ==================== TEAM CRUD OPERATIONS ====================

    /**
     * Get team by unique identifier.
     *
     * @param teamId Team unique identifier
     * @return CompletableFuture with Team or null if not found
     */
    @NonNull
    CompletableFuture<Team> getTeamById(@NonNull String teamId);

    /**
     * Get team by name.
     *
     * @param teamName Team name (unique identifier)
     * @return CompletableFuture with Team or null if not found
     */
    @NonNull
    CompletableFuture<Team> getTeamByName(@NonNull String teamName);

    /**
     * Get all active teams in the system.
     *
     * @return CompletableFuture with List of active Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getAllActiveTeams();

    /**
     * For convention let's assume an non negative offset to be a quattrodue valid team
     * Get teams configured for QuattroDue pattern (with qdue_offset >= 0).
     *
     * @return CompletableFuture with teams that have qdue_offset defined
     */
    @NonNull
    CompletableFuture<OperationResult<List<Team>>> getTeamsForQuattroDue();

    /**
     * Get all teams (active and inactive).
     *
     * @return CompletableFuture with List of all Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getAllTeams();

    /**
     * Save team (create or update).
     *
     * @param team Team to save
     * @return CompletableFuture with saved Team
     */
    @NonNull
    CompletableFuture<Team> saveTeam(@NonNull Team team);

    /**
     * Delete team (soft delete - marks as inactive).
     *
     * @param teamId Team ID to delete
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> deleteTeam(@NonNull Team team);

    // ==================== USER-TEAM ASSIGNMENT OPERATIONS ====================

    /**
     * Get primary team for user.
     *
     * <p>Returns the primary team assignment for a user. If user belongs to
     * multiple teams, this returns the team marked as primary or the most
     * recently assigned team.</p>
     *
     * @param userId User ID
     * @return CompletableFuture with Team or null if user has no team assignment
     */
    @NonNull
    CompletableFuture<Team> getTeamForUser(@NonNull String userId);

    /**
     * Get all teams for user.
     *
     * <p>Returns all teams that a user is assigned to. Users can belong to
     * multiple teams for scheduling flexibility.</p>
     *
     * @param userId User ID
     * @return CompletableFuture with List of Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getTeamsForUser(@NonNull String userId);

    /**
     * Get all users in team.
     *
     * @param teamId Team ID
     * @return CompletableFuture with List of user IDs
     */
    @NonNull
    CompletableFuture<List<String>> getUsersInTeam(@NonNull String teamId);

    /**
     * Get all users in team by team name.
     *
     * @param teamName Team name
     * @return CompletableFuture with List of user IDs
     */
    @NonNull
    CompletableFuture<List<String>> getUsersInTeamByName(@NonNull String teamName);

    /**
     * Assign user to team.
     *
     * @param userId    User ID to assign
     * @param teamId    Team ID to assign to
     * @param isPrimary Whether this is the user's primary team assignment
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> assignUserToTeam(@NonNull String userId, @NonNull String teamId, boolean isPrimary);

    /**
     * Remove user from team.
     *
     * @param userId User ID to remove
     * @param teamId Team ID to remove from
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> removeUserFromTeam(@NonNull String userId, @NonNull String teamId);

    /**
     * Set user's primary team.
     *
     * @param userId User ID
     * @param teamId Team ID to set as primary
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> setPrimaryTeamForUser(@NonNull String userId, @NonNull String teamId);

    // ==================== BUSINESS QUERY OPERATIONS ====================

    /**
     * Check if user is member of team.
     *
     * @param userId User ID to check
     * @param teamId Team ID to check
     * @return CompletableFuture with boolean indicating membership
     */
    @NonNull
    CompletableFuture<Boolean> isUserInTeam(@NonNull String userId, @NonNull String teamId);

    /**
     * Check if user is member of team by team name.
     *
     * @param userId   User ID to check
     * @param teamName Team name to check
     * @return CompletableFuture with boolean indicating membership
     */
    @NonNull
    CompletableFuture<Boolean> isUserInTeamByName(@NonNull String userId, @NonNull String teamName);

    /**
     * Get teams by name pattern.
     *
     * @param namePattern Team name pattern (supports wildcards)
     * @return CompletableFuture with List of matching Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getTeamsByNamePattern(@NonNull String namePattern);

    /**
     * Find teams working on specific shifts.
     *
     * @param shiftIds List of shift IDs
     * @return CompletableFuture with List of Team objects assigned to those shifts
     */
    @NonNull
    CompletableFuture<List<Team>> getTeamsForShifts(@NonNull List<String> shiftIds);

    /**
     * Get count of active teams.
     *
     * @return CompletableFuture with count of active teams
     */
    @NonNull
    CompletableFuture<Integer> getActiveTeamCount();

    /**
     * Get count of users in team.
     *
     * @param teamId Team ID
     * @return CompletableFuture with count of users in team
     */
    @NonNull
    CompletableFuture<Integer> getUserCountInTeam(@NonNull String teamId);

    // ==================== ADVANCED TEAM OPERATIONS ====================

    /**
     * Find available teams for scheduling.
     *
     * <p>Returns teams that are active and available for new shift assignments.
     * Excludes teams that are at capacity or marked as unavailable.</p>
     *
     * @return CompletableFuture with List of available Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getAvailableTeamsForScheduling();

    /**
     * Get teams with minimum user count.
     *
     * @param minUserCount Minimum number of users required
     * @return CompletableFuture with List of Team objects meeting criteria
     */
    @NonNull
    CompletableFuture<List<Team>> getTeamsWithMinimumUsers(int minUserCount);

    /**
     * Get teams that user can be assigned to.
     *
     * <p>Returns teams that a specific user is eligible to join based on
     * business rules, capacity, and current assignments.</p>
     *
     * @param userId User ID to check eligibility for
     * @return CompletableFuture with List of eligible Team objects
     */
    @NonNull
    CompletableFuture<List<Team>> getEligibleTeamsForUser(@NonNull String userId);

    /**
     * Bulk assign users to team.
     *
     * @param userIds List of user IDs to assign
     * @param teamId  Team ID to assign to
     * @return CompletableFuture with count of successful assignments
     */
    @NonNull
    CompletableFuture<Integer> bulkAssignUsersToTeam(@NonNull List<String> userIds, @NonNull String teamId);

    /**
     * Transfer users between teams.
     *
     * @param userIds    List of user IDs to transfer
     * @param fromTeamId Source team ID
     * @param toTeamId   Destination team ID
     * @return CompletableFuture with count of successful transfers
     */
    @NonNull
    CompletableFuture<Integer> transferUsersBetweenTeams(@NonNull List<String> userIds,
                                                         @NonNull String fromTeamId,
                                                         @NonNull String toTeamId);

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get team assignment statistics.
     *
     * @return CompletableFuture with TeamStatistics object
     */
    @NonNull
    CompletableFuture<TeamStatistics> getTeamStatistics();

    /**
     * Get teams ordered by user count.
     *
     * @param ascending True for ascending order, false for descending
     * @return CompletableFuture with List of Team objects ordered by user count
     */
    @NonNull
    CompletableFuture<List<Team>> getTeamsOrderedByUserCount(boolean ascending);

    // ==================== INNER CLASSES ====================

    /**
     * Team statistics data class.
     */
    class TeamStatistics {
        public final int totalTeams;
        public final int activeTeams;
        public final int totalUsers;
        public final int usersWithTeamAssignment;
        public final double averageUsersPerTeam;
        public final Team largestTeam;
        public final Team smallestTeam;

        public TeamStatistics(int totalTeams, int activeTeams, int totalUsers,
                              int usersWithTeamAssignment, double averageUsersPerTeam,
                              @Nullable Team largestTeam, @Nullable Team smallestTeam) {
            this.totalTeams = totalTeams;
            this.activeTeams = activeTeams;
            this.totalUsers = totalUsers;
            this.usersWithTeamAssignment = usersWithTeamAssignment;
            this.averageUsersPerTeam = averageUsersPerTeam;
            this.largestTeam = largestTeam;
            this.smallestTeam = smallestTeam;
        }
    }
}