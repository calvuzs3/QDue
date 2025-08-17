package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.data.entities.TeamMinimalEntity;

import java.util.List;

/**
 * TeamDao - Data Access Object for Team operations.
 *
 * <p>Provides database access methods for TeamEntity operations using Room persistence library.
 * Includes comprehensive CRUD operations, query methods, and batch operations optimized
 * for work schedule management use cases.</p>
 *
 * <h3>Operation Categories:</h3>
 * <ul>
 *   <li><strong>Basic CRUD</strong>: Insert, update, delete, and select operations</li>
 *   <li><strong>Query Methods</strong>: Find teams by various criteria</li>
 *   <li><strong>Batch Operations</strong>: Efficient multi-team operations</li>
 *   <li><strong>Status Management</strong>: Active/inactive team filtering</li>
 *   <li><strong>Utility Methods</strong>: Count, existence checks, cleanup</li>
 * </ul>
 *
 * <h3>Performance Features:</h3>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Leverages database indexes for fast lookups</li>
 *   <li><strong>Batch Operations</strong>: Efficient multiple-record processing</li>
 *   <li><strong>Conflict Resolution</strong>: Smart handling of duplicate entries</li>
 *   <li><strong>Soft Deletes</strong>: Mark teams inactive instead of hard deletion</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Initial Implementation
 * @since Clean architecture
 */
@Dao
public interface TeamDao {

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Insert a new team.
     *
     * @param team Team to insert
     * @return Row ID of inserted team
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertTeam(@NonNull TeamEntity team);

    /**
     * Insert multiple teams in a single transaction.
     * More efficient than multiple single inserts.
     *
     * @param teams Teams to insert
     * @return Array of row IDs for inserted teams
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long[] insertTeams(@NonNull TeamEntity... teams);

    /**
     * Insert multiple teams from list.
     *
     * @param teams List of teams to insert
     * @return List of row IDs for inserted teams
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertTeams(@NonNull List<TeamEntity> teams);

    /**
     * Insert team with replace strategy for duplicates.
     * Useful when re-importing teams or handling conflicts.
     *
     * @param team Team to insert or replace
     * @return Row ID of inserted/updated team
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrReplaceTeam(@NonNull TeamEntity team);

    /**
     * Update existing team.
     *
     * @param team Team to update (must have valid ID)
     * @return Number of rows affected (should be 1 if successful)
     */
    @Update
    int updateTeam(@NonNull TeamEntity team);

    /**
     * Update multiple teams in a single transaction.
     *
     * @param teams Teams to update
     * @return Number of rows affected
     */
    @Update
    int updateTeams(@NonNull TeamEntity... teams);

    /**
     * Delete team from database.
     * Note: Consider using markAsInactive() for soft delete instead.
     *
     * @param team Team to delete
     * @return Number of rows affected (should be 1 if successful)
     */
    @Delete
    int deleteTeam(@NonNull TeamEntity team);

    /**
     * Delete multiple teams.
     *
     * @param teams Teams to delete
     * @return Number of rows affected
     */
    @Delete
    int deleteTeams(@NonNull TeamEntity... teams);

    // ==================== QUERY METHODS ====================

    /**
     * Get all teams ordered by sort order and name.
     *
     * @return List of all teams
     */
    @Query("SELECT * FROM teams ORDER BY sort_order ASC, name ASC")
    List<TeamEntity> getAllTeams();

    /**
     * Get all active teams only.
     * Most common query for operational use.
     *
     * @return List of active teams
     */
    @Query("SELECT * FROM teams WHERE active = 1 ORDER BY sort_order ASC, name ASC")
    List<TeamEntity> getActiveTeams();

    /**
     * Get all inactive teams.
     * Useful for administration and historical views.
     *
     * @return List of inactive teams
     */
    @Query("SELECT * FROM teams WHERE active = 0 ORDER BY name ASC")
    List<TeamEntity> getInactiveTeams();

    /**
     * Find team by ID.
     *
     * @param id Team ID
     * @return Team entity or null if not found
     */
    @Query("SELECT * FROM teams WHERE id = :id")
    @Nullable
    TeamEntity getTeamById(long id);

    /**
     * Find team by name (case-insensitive).
     *
     * @param name Team name to find
     * @return Team entity or null if not found
     */
    @Query("SELECT * FROM teams WHERE LOWER(name) = LOWER(:name)")
    @Nullable
    TeamEntity getTeamByName(@NonNull String name);

    /**
     * Find team by name among active teams only.
     *
     * @param name Team name to find
     * @return Active team entity or null if not found
     */
    @Query("SELECT * FROM teams WHERE LOWER(name) = LOWER(:name) AND active = 1")
    @Nullable
    TeamEntity getActiveTeamByName(@NonNull String name);

    /**
     * Find teams by name pattern (LIKE search).
     *
     * @param namePattern Pattern to search (use % for wildcards)
     * @return List of matching teams
     */
    @Query("SELECT * FROM teams WHERE name LIKE :namePattern ORDER BY name ASC")
    List<TeamEntity> findTeamsByNamePattern(@NonNull String namePattern);

    /**
     * Get teams by multiple names.
     * Useful for batch lookups.
     *
     * @param names List of team names
     * @return List of matching teams
     */
    @Query("SELECT * FROM teams WHERE name IN (:names) ORDER BY sort_order ASC, name ASC")
    List<TeamEntity> getTeamsByNames(@NonNull List<String> names);

    /**
     * Get teams by multiple IDs.
     *
     * @param ids List of team IDs
     * @return List of matching teams
     */
    @Query("SELECT * FROM teams WHERE id IN (:ids) ORDER BY sort_order ASC, name ASC")
    List<TeamEntity> getTeamsByIds(@NonNull List<Long> ids);

    // ==================== STATUS MANAGEMENT ====================

    /**
     * Mark team as inactive by ID (soft delete).
     *
     * @param teamId Team ID to deactivate
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET active = 0, updated_at = :timestamp WHERE id = :teamId")
    int markTeamAsInactive(long teamId, long timestamp);

    /**
     * Mark team as active by ID.
     *
     * @param teamId Team ID to activate
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET active = 1, updated_at = :timestamp WHERE id = :teamId")
    int markTeamAsActive(long teamId, long timestamp);

    /**
     * Mark team as inactive by name.
     *
     * @param name Team name to deactivate
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET active = 0, updated_at = :timestamp WHERE LOWER(name) = LOWER(:name)")
    int markTeamAsInactiveByName(@NonNull String name, long timestamp);

    /**
     * Mark all teams as active.
     * Useful for bulk operations.
     *
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET active = 1, updated_at = :timestamp")
    int markAllTeamsAsActive(long timestamp);

    /**
     * Mark all teams as inactive.
     * Use with caution - for major reorganizations only.
     *
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET active = 0, updated_at = :timestamp")
    int markAllTeamsAsInactive(long timestamp);

    // ==================== UTILITY METHODS ====================

    /**
     * Count total teams.
     *
     * @return Total number of teams
     */
    @Query("SELECT COUNT(*) FROM teams")
    int getTeamCount();


    /**
     * Count active teams.
     *
     * @return Number of active teams
     */
    @Query("SELECT COUNT(*) FROM teams WHERE active = 1")
    int getActiveTeamCount();

    /**
     * Count inactive teams.
     *
     * @return Number of inactive teams
     */
    @Query("SELECT COUNT(*) FROM teams WHERE active = 0")
    int getInactiveTeamCount();

    /**
     * Check if team exists by name.
     *
     * @param name Team name to check
     * @return true if team exists
     */
    @Query("SELECT COUNT(*) > 0 FROM teams WHERE LOWER(name) = LOWER(:name)")
    boolean teamExistsByName(@NonNull String name);

    /**
     * Check if active team exists by name.
     *
     * @param name Team name to check
     * @return true if active team exists
     */
    @Query("SELECT COUNT(*) > 0 FROM teams WHERE LOWER(name) = LOWER(:name) AND active = 1")
    boolean activeTeamExistsByName(@NonNull String name);

    /**
     * Check if team exists by ID.
     *
     * @param id Team ID to check
     * @return true if team exists
     */
    @Query("SELECT COUNT(*) > 0 FROM teams WHERE id = :id")
    boolean teamExistsById(long id);

    /**
     * Get maximum sort order value.
     * Useful for adding new teams at the end.
     *
     * @return Maximum sort order, or 0 if no teams exist
     */
    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM teams")
    int getMaxSortOrder();


    /**
     * Get teams by status for backup operations
     *
     * @param active Status to filter by
     * @return List of teams with specified status
     */
    @Query("SELECT * FROM teams WHERE active = :active ORDER BY name")
    @NonNull
    List<TeamEntity> getTeamsByStatus(boolean active);

    /**
     * Get teams created after specific timestamp.
     *
     * @param timestamp Timestamp to filter by
     * @return List of recently created teams
     */
    @Query("SELECT * FROM teams WHERE created_at > :timestamp ORDER BY created_at DESC")
    List<TeamEntity> getTeamsCreatedAfter(long timestamp);

    /**
     * Get teams updated after specific timestamp.
     *
     * @param timestamp Timestamp to filter by
     * @return List of recently updated teams
     */
    @Query("SELECT * FROM teams WHERE updated_at > :timestamp ORDER BY updated_at DESC")
    List<TeamEntity> getTeamsUpdatedAfter(long timestamp);

    // ==================== BATCH OPERATIONS ====================

    /**
     * Update sort order for multiple teams.
     * Useful for reordering teams.
     *
     * @param teamId Team ID
     * @param sortOrder New sort order
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET sort_order = :sortOrder, updated_at = :timestamp WHERE id = :teamId")
    int updateTeamSortOrder(long teamId, int sortOrder, long timestamp);

    /**
     * Bulk update team names.
     * Advanced operation for data migration or corrections.
     *
     * @param oldName Current team name
     * @param newName New team name
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET name = :newName, updated_at = :timestamp WHERE LOWER(name) = LOWER(:oldName)")
    int renameTeam(@NonNull String oldName, @NonNull String newName, long timestamp);

    /**
     * Update display names for all teams that don't have custom display names.
     *
     * @param prefix Prefix to add to team names
     * @return Number of rows affected
     */
    @Query("UPDATE teams SET display_name = :prefix || name, updated_at = :timestamp WHERE display_name IS NULL OR display_name = name")
    int updateDisplayNamesWithPrefix(@NonNull String prefix, long timestamp);

    // ==================== CLEANUP OPERATIONS ====================

    /**
     * Delete all inactive teams permanently.
     * Use with extreme caution - this is irreversible.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM teams WHERE active = 0")
    int deleteAllInactiveTeams();

    /**
     * Delete teams older than specified timestamp.
     *
     * @param timestamp Cutoff timestamp
     * @return Number of rows deleted
     */
    @Query("DELETE FROM teams WHERE created_at < :timestamp")
    int deleteTeamsOlderThan(long timestamp);

    /**
     * Reset all teams to default state.
     * Nuclear option for complete team reset.
     *
     * @return Number of rows affected
     */
    @Query("DELETE FROM teams")
    int deleteAllTeams();

    // ==================== SPECIALIZED QUERIES ====================

    /**
     * Get team names only (for performance when full entities not needed).
     *
     * @return List of team names
     */
    @Query("SELECT name FROM teams WHERE active = 1 ORDER BY sort_order ASC, name ASC")
    List<String> getActiveTeamNames();

    /**
     * Get team IDs and names for dropdown/selection purposes.
     *
     * @return List of teams with minimal data
     */
    @Query("SELECT id, name, display_name FROM teams WHERE active = 1 ORDER BY sort_order ASC, name ASC")
    List<TeamMinimalEntity> getActiveTeamsMinimal();

    /**
     * Get distinct team names (useful for validation).
     *
     * @return List of unique team names
     */
    @Query("SELECT DISTINCT name FROM teams ORDER BY name ASC")
    List<String> getDistinctTeamNames();

    /**
     * Search teams by description.
     *
     * @param searchTerm Search term to find in descriptions
     * @return List of teams with matching descriptions
     */
    @Query("SELECT * FROM teams WHERE description LIKE '%' || :searchTerm || '%' ORDER BY name ASC")
    List<TeamEntity> searchTeamsByDescription(@NonNull String searchTerm);
}