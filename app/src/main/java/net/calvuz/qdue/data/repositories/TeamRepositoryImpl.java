package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.TeamDao;
import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * TeamRepositoryImpl - Clean Architecture Data Layer Implementation
 *
 * <p>Repository implementation focused on team management and user-team relationships
 * following clean architecture principles. Provides data persistence and business logic
 * for team operations and user assignments within the work scheduling system.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Team CRUD Operations</strong>: Create, read, update, delete teams</li>
 *   <li><strong>User-Team Assignments</strong>: Manage user membership in teams</li>
 *   <li><strong>Domain Model Conversion</strong>: Convert between TeamEntity and Team domain models</li>
 *   <li><strong>Data Persistence</strong>: Interface with CalendarDatabase and TeamDao</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture-based background processing</li>
 * </ul>
 *
 * <h3>Design Patterns:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong>: Clean separation between domain and data</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI following project standards</li>
 *   <li><strong>Async Processing</strong>: Background thread execution with ExecutorService</li>
 *   <li><strong>Caching Strategy</strong>: In-memory cache for frequently accessed data</li>
 * </ul>
 *
 * <h3>User-Team Relationship Model:</h3>
 * <p>Note: This implementation uses a simple mapping approach. For production use,
 * consider implementing a dedicated UserTeamAssignmentEntity and corresponding DAO
 * for more sophisticated relationship management.</p>
 */
public class TeamRepositoryImpl implements TeamRepository {

    private static final String TAG = "TeamRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final TeamDao mTeamDao;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;

    // ==================== USER-TEAM ASSIGNMENT STORAGE ====================

    // Note: In production, these should be replaced with proper database entities
    private final Map<String, String> mUserPrimaryTeamAssignments = new ConcurrentHashMap<>();
    private final Map<String, List<String>> mUserTeamAssignments = new ConcurrentHashMap<>();
    private final Map<String, List<String>> mTeamUserAssignments = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context       Application context for resource access
     * @param database      CalendarDatabase instance for data operations
     * @param backupManager CoreBackupManager for automatic backup integration
     */
    public TeamRepositoryImpl(@NonNull Context context,
                              @NonNull CalendarDatabase database,
                              @NonNull CoreBackupManager backupManager) {
        this.mTeamDao = database.teamDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool( 3 );

        // TODO: Initialize with standard QuattroDue teams? Deprecated
        //initializeStandardTeamsIfNeeded();

        Log.d( TAG, "TeamRepositoryImpl initialized" );
    }

    // ==================== TEAM CRUD OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamById(@NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting team by ID: " + teamId );

                // Use team name as ID for domain model
                TeamEntity entity = mTeamDao.getActiveTeamByName( teamId );
                if (entity == null) {
                    Log.w( TAG, "Team not found with ID: " + teamId );
                    return null;
                }

                Team domainTeam = TeamEntity.toDomainModel( entity );
                Log.d( TAG, "Successfully retrieved team: " + teamId );
                return domainTeam;
            } catch (Exception e) {
                Log.e( TAG, "Error getting team by ID: " + teamId, e );
                return null;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamByName(@NonNull String teamName) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting team by name: " + teamName );

                TeamEntity entity = mTeamDao.getActiveTeamByName( teamName );
                if (entity == null) {
                    Log.w( TAG, "Team not found with name: " + teamName );
                    return null;
                }

                Team domainTeam = TeamEntity.toDomainModel( entity );
                Log.d( TAG, "Successfully retrieved team by name: " + teamName );
                return domainTeam;
            } catch (Exception e) {
                Log.e( TAG, "Error getting team by name: " + teamName, e );
                return null;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAllActiveTeams() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting all active teams" );

                List<TeamEntity> entities = mTeamDao.getActiveTeams();
                List<Team> domainTeams = entities.stream()
                        .map( TeamEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Successfully retrieved " + domainTeams.size() + " active teams" );
                return domainTeams;
            } catch (Exception e) {
                Log.e( TAG, "Error getting active teams", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<Team>>> getTeamsForQuattroDue() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<TeamEntity> teams = mTeamDao.getAllTeams();
                Log.d( TAG, "Found " + teams.size() + " teams" );

                List<Team> domainTeams = teams.stream()
                        .map( TeamEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .filter( team -> team.getTeamType() == Team.TeamType.QUATTRODUE )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Found " + teams.size() + " teams of type " + Team.TeamType.QUATTRODUE.name() );

                return OperationResult.success( domainTeams,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                return OperationResult.failure( "Failed to load QuattroDue teams: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAllTeams() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting all teams" );

                List<TeamEntity> entities = mTeamDao.getAllTeams();
                List<Team> domainTeams = entities.stream()
                        .map( TeamEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Successfully retrieved " + domainTeams.size() + " teams" );
                return domainTeams;
            } catch (Exception e) {
                Log.e( TAG, "Error getting all teams", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Team> saveTeam(@NonNull Team team) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Saving team: " + team);

                // Convert to entity
                TeamEntity entity = TeamEntity.fromDomainModel( team );

                // Check if team exists by name
                boolean isUpdate = mTeamDao.activeTeamExistsByName( team.getName() );
                if (isUpdate) {
                    // Get existing entity to preserve ID
                    TeamEntity existingEntity = mTeamDao.getActiveTeamByName( team.getName() );
                    if (existingEntity != null) {
                        entity.setId( existingEntity.getId() );
                        entity.setCreatedAt( existingEntity.getCreatedAt() );
                    }
                    mTeamDao.updateTeam( entity );
                    Log.d( TAG, "Updated existing team: " + team );
                } else {
                    mTeamDao.insertTeam( entity );
                    Log.d( TAG, "Inserted new team: " + team );
                }

                // Trigger auto backup
                mBackupManager.performAutoBackup( "teams", isUpdate ? "update" : "create" );

                Team savedTeam = TeamEntity.toDomainModel( entity );
                Log.d( TAG, "Successfully saved team: " + team );
                return savedTeam;
            } catch (Exception e) {
                Log.e( TAG, "Error saving team: " + team, e );
                throw new RuntimeException( "Failed to save team", e );
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> deleteTeam(@NonNull Team team) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deleting team: " + team );

                // Prevent deletion of standard QuattroDue teams
                if (team.getTeamType().equals( Team.TeamType.QUATTRODUE )) {
                    Log.w( TAG, "Cannot delete a QuattroDue team: " + team );
                    return false;
                }

                // Remove all user assignments for this team
                removeAllUsersFromTeam( team.getId() );

                // Soft delete by marking inactive
                long timestamp = System.currentTimeMillis();
                int affectedRows = mTeamDao.markTeamAsInactiveByName( team.getId() , timestamp );

                if (affectedRows > 0) {
                    mBackupManager.performAutoBackup( "teams", "delete" );
                    Log.d( TAG, "Successfully deleted team: " + team );
                    return true;
                } else {
                    Log.w( TAG, "No team found to delete: " + team );
                    return false;
                }
            } catch (Exception e) {
                Log.e( TAG, "Error deleting team: " + team, e );
                return false;
            }
        }, mExecutorService );
    }

    // ==================== USER-TEAM ASSIGNMENT OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamForUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting primary team for user: " + userId );

                String primaryTeamId = mUserPrimaryTeamAssignments.get( userId );
                if (primaryTeamId == null) {
                    // Fallback to first assigned team
                    List<String> userTeams = mUserTeamAssignments.get( userId );
                    if (userTeams != null && !userTeams.isEmpty()) {
                        primaryTeamId = userTeams.get( 0 );
                    }
                }

                if (primaryTeamId != null) {
                    return getTeamById( primaryTeamId ).get();
                }

                Log.d( TAG, "No team found for user: " + userId );
                return null;
            } catch (Exception e) {
                Log.e( TAG, "Error getting team for user: " + userId, e );
                return null;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsForUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting teams for user: " + userId );

                List<String> teamIds = mUserTeamAssignments.get( userId );
                if (teamIds == null || teamIds.isEmpty()) {
                    return new ArrayList<>();
                }

                List<Team> teams = new ArrayList<>();
                for (String teamId : teamIds) {
                    Team team = getTeamById( teamId ).get();
                    if (team != null) {
                        teams.add( team );
                    }
                }

                Log.d( TAG, "Found " + teams.size() + " teams for user: " + userId );
                return teams;
            } catch (Exception e) {
                Log.e( TAG, "Error getting teams for user: " + userId, e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getUsersInTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<String> users = mTeamUserAssignments.get( teamId );
                return users != null ? new ArrayList<>( users ) : new ArrayList<>();
            } catch (Exception e) {
                Log.e( TAG, "Error getting users in team: " + teamId, e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getUsersInTeamByName(@NonNull String teamName) {
        return getUsersInTeam( teamName ); // Team name is used as ID
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> assignUserToTeam(@NonNull String userId, @NonNull String teamId, boolean isPrimary) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Assigning user " + userId + " to team " + teamId + " (primary: " + isPrimary + ")" );

                // Verify team exists
                Team team = getTeamById( teamId ).get();
                if (team == null || !team.isActive()) {
                    Log.w( TAG, "Cannot assign to inactive or non-existent team: " + teamId );
                    return false;
                }

                // Add to user's team list
                List<String> userTeams = mUserTeamAssignments.computeIfAbsent( userId, k -> new ArrayList<>() );
                if (!userTeams.contains( teamId )) {
                    userTeams.add( teamId );
                }

                // Add to team's user list
                List<String> teamUsers = mTeamUserAssignments.computeIfAbsent( teamId, k -> new ArrayList<>() );
                if (!teamUsers.contains( userId )) {
                    teamUsers.add( userId );
                }

                // Set as primary if requested or if it's the user's first team
                if (isPrimary || mUserPrimaryTeamAssignments.get( userId ) == null) {
                    mUserPrimaryTeamAssignments.put( userId, teamId );
                }

                mBackupManager.performAutoBackup( "teams", "user_assignment" );
                Log.d( TAG, "Successfully assigned user " + userId + " to team " + teamId );
                return true;
            } catch (Exception e) {
                Log.e( TAG, "Error assigning user to team", e );
                return false;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> removeUserFromTeam(@NonNull String userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Removing user " + userId + " from team " + teamId );

                // Remove from user's team list
                List<String> userTeams = mUserTeamAssignments.get( userId );
                if (userTeams != null) {
                    userTeams.remove( teamId );
                    if (userTeams.isEmpty()) {
                        mUserTeamAssignments.remove( userId );
                    }
                }

                // Remove from team's user list
                List<String> teamUsers = mTeamUserAssignments.get( teamId );
                if (teamUsers != null) {
                    teamUsers.remove( userId );
                    if (teamUsers.isEmpty()) {
                        mTeamUserAssignments.remove( teamId );
                    }
                }

                // Update primary team if necessary
                String primaryTeam = mUserPrimaryTeamAssignments.get( userId );
                if (teamId.equals( primaryTeam )) {
                    // Set new primary team to first remaining team, or remove if none
                    if (userTeams != null && !userTeams.isEmpty()) {
                        mUserPrimaryTeamAssignments.put( userId, userTeams.get( 0 ) );
                    } else {
                        mUserPrimaryTeamAssignments.remove( userId );
                    }
                }

                mBackupManager.performAutoBackup( "teams", "user_removal" );
                Log.d( TAG, "Successfully removed user " + userId + " from team " + teamId );
                return true;
            } catch (Exception e) {
                Log.e( TAG, "Error removing user from team", e );
                return false;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> setPrimaryTeamForUser(@NonNull String userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Verify user is assigned to this team
                List<String> userTeams = mUserTeamAssignments.get( userId );
                if (userTeams == null || !userTeams.contains( teamId )) {
                    Log.w( TAG, "Cannot set primary team - user " + userId + " not assigned to team " + teamId );
                    return false;
                }

                mUserPrimaryTeamAssignments.put( userId, teamId );
                mBackupManager.performAutoBackup( "teams", "primary_assignment" );
                Log.d( TAG, "Set primary team for user " + userId + " to " + teamId );
                return true;
            } catch (Exception e) {
                Log.e( TAG, "Error setting primary team", e );
                return false;
            }
        }, mExecutorService );
    }

    // ==================== BUSINESS QUERY OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Boolean> isUserInTeam(@NonNull String userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<String> userTeams = mUserTeamAssignments.get( userId );
                return userTeams != null && userTeams.contains( teamId );
            } catch (Exception e) {
                Log.e( TAG, "Error checking user team membership", e );
                return false;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> isUserInTeamByName(@NonNull String userId, @NonNull String teamName) {
        return isUserInTeam( userId, teamName ); // Team name is used as ID
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsByNamePattern(@NonNull String namePattern) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Searching teams by name pattern: " + namePattern );

                // Convert pattern to SQL LIKE pattern
                String sqlPattern = namePattern.replace( "*", "%" ).replace( "?", "_" );
                if (!sqlPattern.contains( "%" )) {
                    sqlPattern = "%" + sqlPattern + "%";
                }

                List<TeamEntity> entities = mTeamDao.findTeamsByNamePattern( sqlPattern );
                List<Team> domainTeams = entities.stream()
                        .map( TeamEntity::toDomainModel )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Found " + domainTeams.size() + " teams matching pattern: " + namePattern );
                return domainTeams;
            } catch (Exception e) {
                Log.e( TAG, "Error searching teams by name pattern", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsForShifts(@NonNull List<String> shiftIds) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // This would require shift-team assignment logic
                // For now, return all active teams as placeholder
                Log.d( TAG, "Getting teams for shifts (placeholder implementation)" );
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e( TAG, "Error getting teams for shifts", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> getActiveTeamCount() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                return mTeamDao.getActiveTeamCount();
            } catch (Exception e) {
                Log.e( TAG, "Error getting active team count", e );
                return 0;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> getUserCountInTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<String> users = mTeamUserAssignments.get( teamId );
                return users != null ? users.size() : 0;
            } catch (Exception e) {
                Log.e( TAG, "Error getting user count in team", e );
                return 0;
            }
        }, mExecutorService );
    }

    // ==================== ADVANCED TEAM OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAvailableTeamsForScheduling() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Return all active teams as available for scheduling
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e( TAG, "Error getting available teams for scheduling", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsWithMinimumUsers(int minUserCount) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<Team> allTeams = getAllActiveTeams().get();
                List<Team> eligibleTeams = new ArrayList<>();

                for (Team team : allTeams) {
                    int userCount = getUserCountInTeam( team.getId() ).get();
                    if (userCount >= minUserCount) {
                        eligibleTeams.add( team );
                    }
                }

                return eligibleTeams;
            } catch (Exception e) {
                Log.e( TAG, "Error getting teams with minimum users", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getEligibleTeamsForUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // For now, return all active teams as eligible
                // Business logic can be added here for eligibility rules
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e( TAG, "Error getting eligible teams for user", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> bulkAssignUsersToTeam(@NonNull List<String> userIds, @NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int successCount = 0;
                for (String userId : userIds) {
                    boolean success = assignUserToTeam( userId, teamId, false ).get();
                    if (success) {
                        successCount++;
                    }
                }
                return successCount;
            } catch (Exception e) {
                Log.e( TAG, "Error bulk assigning users to team", e );
                return 0;
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> transferUsersBetweenTeams(@NonNull List<String> userIds,
                                                                @NonNull String fromTeamId,
                                                                @NonNull String toTeamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int successCount = 0;
                for (String userId : userIds) {
                    boolean removed = removeUserFromTeam( userId, fromTeamId ).get();
                    if (removed) {
                        boolean assigned = assignUserToTeam( userId, toTeamId, false ).get();
                        if (assigned) {
                            successCount++;
                        }
                    }
                }
                return successCount;
            } catch (Exception e) {
                Log.e( TAG, "Error transferring users between teams", e );
                return 0;
            }
        }, mExecutorService );
    }

    // ==================== STATISTICS AND REPORTING ====================

    @NonNull
    @Override
    public CompletableFuture<TeamStatistics> getTeamStatistics() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int totalTeams = mTeamDao.getTeamCount();
                int activeTeams = mTeamDao.getActiveTeamCount();

                int totalUsers = mUserTeamAssignments.size();
                int usersWithTeamAssignment = mUserPrimaryTeamAssignments.size();

                double averageUsersPerTeam = activeTeams > 0 ? (double) totalUsers / activeTeams : 0.0;

                // Find largest and smallest teams
                Team largestTeam = null;
                Team smallestTeam = null;
                int maxUsers = 0;
                int minUsers = Integer.MAX_VALUE;

                for (String teamId : mTeamUserAssignments.keySet()) {
                    int userCount = mTeamUserAssignments.get( teamId ).size();
                    if (userCount > maxUsers) {
                        maxUsers = userCount;
                        largestTeam = getTeamById( teamId ).get();
                    }
                    if (userCount < minUsers) {
                        minUsers = userCount;
                        smallestTeam = getTeamById( teamId ).get();
                    }
                }

                return new TeamStatistics(
                        totalTeams,
                        activeTeams,
                        totalUsers,
                        usersWithTeamAssignment,
                        averageUsersPerTeam,
                        largestTeam,
                        smallestTeam
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting team statistics", e );
                return new TeamStatistics( 0, 0, 0, 0, 0.0, null, null );
            }
        }, mExecutorService );
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsOrderedByUserCount(boolean ascending) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<Team> allTeams = getAllActiveTeams().get();

                // Create team-usercount map
                Map<Team, Integer> teamUserCounts = new HashMap<>();
                for (Team team : allTeams) {
                    int userCount = getUserCountInTeam( team.getId() ).get();
                    teamUserCounts.put( team, userCount );
                }

                // Sort teams by user count
                allTeams.sort( (t1, t2) -> {
                    int count1 = teamUserCounts.get( t1 );
                    int count2 = teamUserCounts.get( t2 );
                    return ascending ? Integer.compare( count1, count2 ) : Integer.compare( count2, count1 );
                } );

                return allTeams;
            } catch (Exception e) {
                Log.e( TAG, "Error getting teams ordered by user count", e );
                return new ArrayList<>();
            }
        }, mExecutorService );
    }

    // ==================== HELPER METHODS ====================

    /**
     * Remove all users from a team (used during team deletion).
     */
    private void removeAllUsersFromTeam(@NonNull String teamId) {
        List<String> users = mTeamUserAssignments.remove( teamId );
        if (users != null) {
            for (String userId : users) {
                List<String> userTeams = mUserTeamAssignments.get( userId );
                if (userTeams != null) {
                    userTeams.remove( teamId );
                    if (userTeams.isEmpty()) {
                        mUserTeamAssignments.remove( userId );
                    }
                }

                // Remove primary assignment if necessary
                if (teamId.equals( mUserPrimaryTeamAssignments.get( userId ) )) {
                    if (userTeams != null && !userTeams.isEmpty()) {
                        mUserPrimaryTeamAssignments.put( userId, userTeams.get( 0 ) );
                    } else {
                        mUserPrimaryTeamAssignments.remove( userId );
                    }
                }
            }
        }
    }
}