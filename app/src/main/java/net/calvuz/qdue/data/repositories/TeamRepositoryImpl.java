package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
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
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 7
 */
public class TeamRepositoryImpl implements TeamRepository {

    private static final String TAG = "TeamRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarDatabase mDatabase;
    private final TeamDao mTeamDao;
    private final CoreBackupManager mBackupManager;
    private final LocaleManager mLocaleManager;
    private final ExecutorService mExecutorService;

    // ==================== USER-TEAM ASSIGNMENT STORAGE ====================
    // Note: In production, these should be replaced with proper database entities
    private final Map<Long, String> mUserPrimaryTeamAssignments = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> mUserTeamAssignments = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> mTeamUserAssignments = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context Application context for resource access
     * @param database CalendarDatabase instance for data operations
     * @param backupManager CoreBackupManager for automatic backup integration
     */
    public TeamRepositoryImpl(@NonNull Context context,
                              @NonNull CalendarDatabase database,
                              @NonNull CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mTeamDao = database.teamDao();
        this.mBackupManager = backupManager;
        this.mLocaleManager = new LocaleManager(mContext);
        this.mExecutorService = Executors.newFixedThreadPool(3);

        // Initialize with standard QuattroDue teams
        initializeStandardTeamsIfNeeded();

        Log.d(TAG, "TeamRepositoryImpl initialized via dependency injection");
    }

    // ==================== TEAM CRUD OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamById(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting team by ID: " + teamId);

                // Use team name as ID for domain model
                TeamEntity entity = mTeamDao.getActiveTeamByName(teamId);
                if (entity == null) {
                    Log.w(TAG, "Team not found with ID: " + teamId);
                    return null;
                }

                Team domainTeam = convertToDomainModel(entity);
                Log.d(TAG, "Successfully retrieved team: " + teamId);
                return domainTeam;

            } catch (Exception e) {
                Log.e(TAG, "Error getting team by ID: " + teamId, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamByName(@NonNull String teamName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting team by name: " + teamName);

                TeamEntity entity = mTeamDao.getActiveTeamByName(teamName);
                if (entity == null) {
                    Log.w(TAG, "Team not found with name: " + teamName);
                    return null;
                }

                Team domainTeam = convertToDomainModel(entity);
                Log.d(TAG, "Successfully retrieved team by name: " + teamName);
                return domainTeam;

            } catch (Exception e) {
                Log.e(TAG, "Error getting team by name: " + teamName, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAllActiveTeams() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all active teams");

                List<TeamEntity> entities = mTeamDao.getActiveTeams();
                List<Team> domainTeams = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainTeams.size() + " active teams");
                return domainTeams;

            } catch (Exception e) {
                Log.e(TAG, "Error getting active teams", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAllTeams() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all teams");

                List<TeamEntity> entities = mTeamDao.getAllTeams();
                List<Team> domainTeams = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Successfully retrieved " + domainTeams.size() + " teams");
                return domainTeams;

            } catch (Exception e) {
                Log.e(TAG, "Error getting all teams", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Team> saveTeam(@NonNull Team team) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Saving team: " + team.getDisplayName());

                // Convert to entity
                TeamEntity entity = convertToEntity(team);
                if (entity == null) {
                    throw new IllegalArgumentException("Cannot convert team to entity");
                }

                // Check if team exists by name
                boolean isUpdate = mTeamDao.activeTeamExistsByName(team.getName());
                if (isUpdate) {
                    // Get existing entity to preserve ID
                    TeamEntity existingEntity = mTeamDao.getActiveTeamByName(team.getName());
                    if (existingEntity != null) {
                        entity.setId(existingEntity.getId());
                        entity.setCreatedAt(existingEntity.getCreatedAt());
                    }
                    mTeamDao.updateTeam(entity);
                    Log.d(TAG, "Updated existing team: " + team.getName());
                } else {
                    mTeamDao.insertTeam(entity);
                    Log.d(TAG, "Inserted new team: " + team.getName());
                }

                // Trigger auto backup
                mBackupManager.performAutoBackup("teams", isUpdate ? "update" : "create");

                Team savedTeam = convertToDomainModel(entity);
                Log.d(TAG, "Successfully saved team: " + team.getDisplayName());
                return savedTeam;

            } catch (Exception e) {
                Log.e(TAG, "Error saving team: " + team.getDisplayName(), e);
                throw new RuntimeException("Failed to save team", e);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> deleteTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting team: " + teamId);

                // Prevent deletion of standard QuattroDue teams
                if (isStandardQuattroDueTeam(teamId)) {
                    Log.w(TAG, "Cannot delete standard QuattroDue team: " + teamId);
                    return false;
                }

                // Remove all user assignments for this team
                removeAllUsersFromTeam(teamId);

                // Soft delete by marking inactive
                long timestamp = System.currentTimeMillis();
                int affectedRows = mTeamDao.markTeamAsInactiveByName(teamId, timestamp);

                if (affectedRows > 0) {
                    mBackupManager.performAutoBackup("teams", "delete");
                    Log.d(TAG, "Successfully deleted team: " + teamId);
                    return true;
                } else {
                    Log.w(TAG, "No team found to delete: " + teamId);
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error deleting team: " + teamId, e);
                return false;
            }
        }, mExecutorService);
    }

    // ==================== USER-TEAM ASSIGNMENT OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Team> getTeamForUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting primary team for user: " + userId);

                String primaryTeamId = mUserPrimaryTeamAssignments.get(userId);
                if (primaryTeamId == null) {
                    // Fallback to first assigned team
                    List<String> userTeams = mUserTeamAssignments.get(userId);
                    if (userTeams != null && !userTeams.isEmpty()) {
                        primaryTeamId = userTeams.get(0);
                    }
                }

                if (primaryTeamId != null) {
                    return getTeamById(primaryTeamId).get();
                }

                Log.d(TAG, "No team found for user: " + userId);
                return null;

            } catch (Exception e) {
                Log.e(TAG, "Error getting team for user: " + userId, e);
                return null;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsForUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting teams for user: " + userId);

                List<String> teamIds = mUserTeamAssignments.get(userId);
                if (teamIds == null || teamIds.isEmpty()) {
                    return new ArrayList<>();
                }

                List<Team> teams = new ArrayList<>();
                for (String teamId : teamIds) {
                    Team team = getTeamById(teamId).get();
                    if (team != null) {
                        teams.add(team);
                    }
                }

                Log.d(TAG, "Found " + teams.size() + " teams for user: " + userId);
                return teams;

            } catch (Exception e) {
                Log.e(TAG, "Error getting teams for user: " + userId, e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Long>> getUsersInTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Long> users = mTeamUserAssignments.get(teamId);
                return users != null ? new ArrayList<>(users) : new ArrayList<>();
            } catch (Exception e) {
                Log.e(TAG, "Error getting users in team: " + teamId, e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Long>> getUsersInTeamByName(@NonNull String teamName) {
        return getUsersInTeam(teamName); // Team name is used as ID
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> assignUserToTeam(@NonNull Long userId, @NonNull String teamId, boolean isPrimary) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Assigning user " + userId + " to team " + teamId + " (primary: " + isPrimary + ")");

                // Verify team exists
                Team team = getTeamById(teamId).get();
                if (team == null || !team.isActive()) {
                    Log.w(TAG, "Cannot assign to inactive or non-existent team: " + teamId);
                    return false;
                }

                // Add to user's team list
                List<String> userTeams = mUserTeamAssignments.computeIfAbsent(userId, k -> new ArrayList<>());
                if (!userTeams.contains(teamId)) {
                    userTeams.add(teamId);
                }

                // Add to team's user list
                List<Long> teamUsers = mTeamUserAssignments.computeIfAbsent(teamId, k -> new ArrayList<>());
                if (!teamUsers.contains(userId)) {
                    teamUsers.add(userId);
                }

                // Set as primary if requested or if it's the user's first team
                if (isPrimary || mUserPrimaryTeamAssignments.get(userId) == null) {
                    mUserPrimaryTeamAssignments.put(userId, teamId);
                }

                mBackupManager.performAutoBackup("teams", "user_assignment");
                Log.d(TAG, "Successfully assigned user " + userId + " to team " + teamId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error assigning user to team", e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> removeUserFromTeam(@NonNull Long userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Removing user " + userId + " from team " + teamId);

                // Remove from user's team list
                List<String> userTeams = mUserTeamAssignments.get(userId);
                if (userTeams != null) {
                    userTeams.remove(teamId);
                    if (userTeams.isEmpty()) {
                        mUserTeamAssignments.remove(userId);
                    }
                }

                // Remove from team's user list
                List<Long> teamUsers = mTeamUserAssignments.get(teamId);
                if (teamUsers != null) {
                    teamUsers.remove(userId);
                    if (teamUsers.isEmpty()) {
                        mTeamUserAssignments.remove(teamId);
                    }
                }

                // Update primary team if necessary
                String primaryTeam = mUserPrimaryTeamAssignments.get(userId);
                if (teamId.equals(primaryTeam)) {
                    // Set new primary team to first remaining team, or remove if none
                    if (userTeams != null && !userTeams.isEmpty()) {
                        mUserPrimaryTeamAssignments.put(userId, userTeams.get(0));
                    } else {
                        mUserPrimaryTeamAssignments.remove(userId);
                    }
                }

                mBackupManager.performAutoBackup("teams", "user_removal");
                Log.d(TAG, "Successfully removed user " + userId + " from team " + teamId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error removing user from team", e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> setPrimaryTeamForUser(@NonNull Long userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify user is assigned to this team
                List<String> userTeams = mUserTeamAssignments.get(userId);
                if (userTeams == null || !userTeams.contains(teamId)) {
                    Log.w(TAG, "Cannot set primary team - user " + userId + " not assigned to team " + teamId);
                    return false;
                }

                mUserPrimaryTeamAssignments.put(userId, teamId);
                mBackupManager.performAutoBackup("teams", "primary_assignment");
                Log.d(TAG, "Set primary team for user " + userId + " to " + teamId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error setting primary team", e);
                return false;
            }
        }, mExecutorService);
    }

    // ==================== BUSINESS QUERY OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<Boolean> isUserInTeam(@NonNull Long userId, @NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> userTeams = mUserTeamAssignments.get(userId);
                return userTeams != null && userTeams.contains(teamId);
            } catch (Exception e) {
                Log.e(TAG, "Error checking user team membership", e);
                return false;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> isUserInTeamByName(@NonNull Long userId, @NonNull String teamName) {
        return isUserInTeam(userId, teamName); // Team name is used as ID
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsByNamePattern(@NonNull String namePattern) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Searching teams by name pattern: " + namePattern);

                // Convert pattern to SQL LIKE pattern
                String sqlPattern = namePattern.replace("*", "%").replace("?", "_");
                if (!sqlPattern.contains("%")) {
                    sqlPattern = "%" + sqlPattern + "%";
                }

                List<TeamEntity> entities = mTeamDao.findTeamsByNamePattern(sqlPattern);
                List<Team> domainTeams = entities.stream()
                        .map(this::convertToDomainModel)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                Log.d(TAG, "Found " + domainTeams.size() + " teams matching pattern: " + namePattern);
                return domainTeams;

            } catch (Exception e) {
                Log.e(TAG, "Error searching teams by name pattern", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsForShifts(@NonNull List<String> shiftIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This would require shift-team assignment logic
                // For now, return all active teams as placeholder
                Log.d(TAG, "Getting teams for shifts (placeholder implementation)");
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e(TAG, "Error getting teams for shifts", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> getActiveTeamCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mTeamDao.getActiveTeamCount();
            } catch (Exception e) {
                Log.e(TAG, "Error getting active team count", e);
                return 0;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> getUserCountInTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Long> users = mTeamUserAssignments.get(teamId);
                return users != null ? users.size() : 0;
            } catch (Exception e) {
                Log.e(TAG, "Error getting user count in team", e);
                return 0;
            }
        }, mExecutorService);
    }

    // ==================== ADVANCED TEAM OPERATIONS ====================

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getAvailableTeamsForScheduling() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Return all active teams as available for scheduling
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e(TAG, "Error getting available teams for scheduling", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsWithMinimumUsers(int minUserCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Team> allTeams = getAllActiveTeams().get();
                List<Team> eligibleTeams = new ArrayList<>();

                for (Team team : allTeams) {
                    int userCount = getUserCountInTeam(team.getId()).get();
                    if (userCount >= minUserCount) {
                        eligibleTeams.add(team);
                    }
                }

                return eligibleTeams;
            } catch (Exception e) {
                Log.e(TAG, "Error getting teams with minimum users", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getEligibleTeamsForUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For now, return all active teams as eligible
                // Business logic can be added here for eligibility rules
                return getAllActiveTeams().get();
            } catch (Exception e) {
                Log.e(TAG, "Error getting eligible teams for user", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> bulkAssignUsersToTeam(@NonNull List<Long> userIds, @NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int successCount = 0;
                for (Long userId : userIds) {
                    boolean success = assignUserToTeam(userId, teamId, false).get();
                    if (success) {
                        successCount++;
                    }
                }
                return successCount;
            } catch (Exception e) {
                Log.e(TAG, "Error bulk assigning users to team", e);
                return 0;
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<Integer> transferUsersBetweenTeams(@NonNull List<Long> userIds,
                                                                @NonNull String fromTeamId,
                                                                @NonNull String toTeamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int successCount = 0;
                for (Long userId : userIds) {
                    boolean removed = removeUserFromTeam(userId, fromTeamId).get();
                    if (removed) {
                        boolean assigned = assignUserToTeam(userId, toTeamId, false).get();
                        if (assigned) {
                            successCount++;
                        }
                    }
                }
                return successCount;
            } catch (Exception e) {
                Log.e(TAG, "Error transferring users between teams", e);
                return 0;
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS AND REPORTING ====================

    @NonNull
    @Override
    public CompletableFuture<TeamStatistics> getTeamStatistics() {
        return CompletableFuture.supplyAsync(() -> {
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
                    int userCount = mTeamUserAssignments.get(teamId).size();
                    if (userCount > maxUsers) {
                        maxUsers = userCount;
                        largestTeam = getTeamById(teamId).get();
                    }
                    if (userCount < minUsers) {
                        minUsers = userCount;
                        smallestTeam = getTeamById(teamId).get();
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
                Log.e(TAG, "Error getting team statistics", e);
                return new TeamStatistics(0, 0, 0, 0, 0.0, null, null);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<Team>> getTeamsOrderedByUserCount(boolean ascending) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Team> allTeams = getAllActiveTeams().get();

                // Create team-usercount map
                Map<Team, Integer> teamUserCounts = new HashMap<>();
                for (Team team : allTeams) {
                    int userCount = getUserCountInTeam(team.getId()).get();
                    teamUserCounts.put(team, userCount);
                }

                // Sort teams by user count
                allTeams.sort((t1, t2) -> {
                    int count1 = teamUserCounts.get(t1);
                    int count2 = teamUserCounts.get(t2);
                    return ascending ? Integer.compare(count1, count2) : Integer.compare(count2, count1);
                });

                return allTeams;

            } catch (Exception e) {
                Log.e(TAG, "Error getting teams ordered by user count", e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert TeamEntity to domain Team model.
     */
    @Nullable
    private Team convertToDomainModel(@NonNull TeamEntity entity) {
        try {
            Team.Builder builder = Team.builder(entity.getName())
                    .name(entity.getName())
                    .displayName(entity.getEffectiveDisplayName())
                    .active(entity.isActive());

            if (entity.hasDescription()) {
                builder.description(entity.getDescription());
            }

            // Determine team type for QuattroDue teams
            if (isStandardQuattroDueTeam(entity.getName())) {
                builder.teamType(Team.TeamType.QUATTRODUE);
            } else {
                builder.teamType(Team.TeamType.STANDARD);
            }

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Error converting entity to domain model: " + entity.getName(), e);
            return null;
        }
    }

    /**
     * Convert domain Team to TeamEntity.
     */
    @Nullable
    private TeamEntity convertToEntity(@NonNull Team team) {
        try {
            TeamEntity entity = new TeamEntity(team.getName());
            entity.setDisplayName(team.getDisplayName());
            entity.setDescription(team.getDescription());
            entity.setActive(team.isActive());

            return entity;

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain model to entity: " + team.getName(), e);
            return null;
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Initialize standard QuattroDue teams if they don't exist.
     */
    private void initializeStandardTeamsIfNeeded() {
        CompletableFuture.runAsync(() -> {
            try {
                String[] standardTeams = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

                for (int i = 0; i < standardTeams.length; i++) {
                    String teamName = standardTeams[i];
                    if (!mTeamDao.activeTeamExistsByName(teamName)) {
                        String localizedDisplayName = mLocaleManager.getTeamDisplayName(mContext, teamName);
                        String localizedDescription = mLocaleManager.getTeamDescriptionTemplate(mContext) + " " + teamName;

                        TeamEntity entity = new TeamEntity(teamName);
                        entity.setDisplayName(localizedDisplayName != null ? localizedDisplayName : "Team " + teamName);
                        entity.setDescription(localizedDescription);

                        mTeamDao.insertTeam(entity);
                        Log.d(TAG, "Initialized standard QuattroDue team: " + teamName);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing standard teams", e);
            }
        }, mExecutorService);
    }

    /**
     * Check if team is a standard QuattroDue team.
     */
    private boolean isStandardQuattroDueTeam(@NonNull String teamName) {
        String[] standardTeams = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        for (String standardTeam : standardTeams) {
            if (standardTeam.equals(teamName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all users from a team (used during team deletion).
     */
    private void removeAllUsersFromTeam(@NonNull String teamId) {
        List<Long> users = mTeamUserAssignments.remove(teamId);
        if (users != null) {
            for (Long userId : users) {
                List<String> userTeams = mUserTeamAssignments.get(userId);
                if (userTeams != null) {
                    userTeams.remove(teamId);
                    if (userTeams.isEmpty()) {
                        mUserTeamAssignments.remove(userId);
                    }
                }

                // Remove primary assignment if necessary
                if (teamId.equals(mUserPrimaryTeamAssignments.get(userId))) {
                    if (userTeams != null && !userTeams.isEmpty()) {
                        mUserPrimaryTeamAssignments.put(userId, userTeams.get(0));
                    } else {
                        mUserPrimaryTeamAssignments.remove(userId);
                    }
                }
            }
        }
    }
}