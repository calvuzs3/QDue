package net.calvuz.qdue.data.qdueuser.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.qdueuser.entities.QDueUserEntity;

import java.util.List;

/**
 * QDueUserDao - Room Database Access Object for QDueUser Operations
 *
 * <p>Data access layer for QDueUser entity providing direct database operations following Room patterns.
 * Implements efficient queries optimized for the simplified user management requirements.</p>
 *
 * <h3>Query Optimization:</h3>
 * <ul>
 *   <li><strong>Primary Queries</strong>: ID-based lookups with primary key efficiency</li>
 *   <li><strong>Email Lookups</strong>: Direct email matching for authentication</li>
 *   <li><strong>Existence Checks</strong>: COUNT queries for efficient existence validation</li>
 *   <li><strong>Statistics</strong>: Aggregate queries for user counts and profile completion</li>
 * </ul>
 *
 * <h3>Business Support:</h3>
 * <ul>
 *   <li><strong>Auto-increment ID</strong>: Starting from 1L for user identification</li>
 *   <li><strong>Profile Completion</strong>: Queries to support onboarding flow</li>
 *   <li><strong>Conflict Resolution</strong>: REPLACE strategy for updates</li>
 *   <li><strong>Batch Operations</strong>: Efficient multi-user operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture DAO
 * @since Clean Architecture Phase 3
 */
@Dao
public interface QDueUserDao {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Insert new QDueUser entity.
     * ID will be auto-generated starting from 1L.
     *
     * @param qDueUserEntity Entity to insert
     * @return Generated ID of inserted user
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(@NonNull QDueUserEntity qDueUserEntity);

    /**
     * Insert multiple QDueUser entities.
     *
     * @param qDueUserEntities List of entities to insert
     * @return Array of generated IDs
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long[] insertUsers(@NonNull List<QDueUserEntity> qDueUserEntities);

    /**
     * Update existing QDueUser entity.
     *
     * @param qDueUserEntity Entity to update (must have valid ID)
     * @return Number of rows updated (1 if successful)
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateUser(@NonNull QDueUserEntity qDueUserEntity);

    /**
     * Delete QDueUser entity.
     *
     * @param qDueUserEntity Entity to delete
     * @return Number of rows deleted (1 if successful)
     */
    @Delete
    int deleteUser(@NonNull QDueUserEntity qDueUserEntity);

    /**
     * Delete QDueUser by ID.
     *
     * @param userId ID of user to delete
     * @return Number of rows deleted (1 if successful)
     */
    @Query("DELETE FROM qdueuser WHERE id = :userId")
    int deleteUserById(@NonNull Long userId);

    /**
     * Delete all QDueUser entities.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM qdueuser")
    int deleteAllUsers();

    // ==================== RETRIEVAL QUERIES ====================

    /**
     * Get QDueUser by ID.
     *
     * @param userId User ID to find
     * @return QDueUserEntity if found, null otherwise
     */
    @Query("SELECT * FROM qdueuser WHERE id = :userId LIMIT 1")
    QDueUserEntity getUserById(@NonNull Long userId);

    /**
     * Get QDueUser by email address.
     *
     * @param email Email to search for
     * @return QDueUserEntity if found, null otherwise
     */
    @Query("SELECT * FROM qdueuser WHERE TRIM(LOWER(email)) = TRIM(LOWER(:email)) LIMIT 1")
    QDueUserEntity getUserByEmail(@NonNull String email);

    /**
     * Get all QDueUser entities ordered by ID.
     *
     * @return List of all QDueUserEntity objects
     */
    @Query("SELECT * FROM qdueuser ORDER BY id ASC")
    List<QDueUserEntity> getAllUsers();

    /**
     * Get primary user (first user created, typically ID = 1).
     *
     * @return First QDueUserEntity by ID
     */
    @Query("SELECT * FROM qdueuser ORDER BY id ASC LIMIT 1")
    QDueUserEntity getPrimaryUser();

    /**
     * Get users with specific ID range.
     *
     * @param startId Start ID (inclusive)
     * @param endId   End ID (inclusive)
     * @return List of users in ID range
     */
    @Query("SELECT * FROM qdueuser WHERE id BETWEEN :startId AND :endId ORDER BY id ASC")
    List<QDueUserEntity> getUsersInRange(@NonNull Long startId, @NonNull Long endId);

    // ==================== EXISTENCE CHECKS ====================

    /**
     * Check if user exists by ID.
     *
     * @param userId User ID to check
     * @return Count of users with ID (1 if exists, 0 otherwise)
     */
    @Query("SELECT COUNT(*) FROM qdueuser WHERE id = :userId")
    int getUserCountById(@NonNull Long userId);

    /**
     * Check if user exists by email.
     *
     * @param email Email to check
     * @return Count of users with email (1 if exists, 0 otherwise)
     */
    @Query("SELECT COUNT(*) FROM qdueuser WHERE TRIM(LOWER(email)) = TRIM(LOWER(:email))")
    int getUserCountByEmail(@NonNull String email);

    // ==================== STATISTICS QUERIES ====================

    /**
     * Get total count of QDueUser entities.
     *
     * @return Total user count
     */
    @Query("SELECT COUNT(*) FROM qdueuser")
    int getTotalUsersCount();

    /**
     * Get count of users with complete profiles (both nickname and email not empty).
     *
     * @return Count of users with complete profiles
     */
    @Query("SELECT COUNT(*) FROM qdueuser WHERE TRIM(nickname) != '' AND TRIM(email) != ''")
    int getCompleteProfilesCount();

    /**
     * Get count of users with empty nicknames.
     *
     * @return Count of users without nicknames
     */
    @Query("SELECT COUNT(*) FROM qdueuser WHERE TRIM(nickname) = ''")
    int getUsersWithoutNickname();

    /**
     * Get count of users with empty emails.
     *
     * @return Count of users without emails
     */
    @Query("SELECT COUNT(*) FROM qdueuser WHERE TRIM(email) = ''")
    int getUsersWithoutEmail();

    // ==================== PROFILE COMPLETION QUERIES ====================

    /**
     * Get users with incomplete profiles (missing nickname or email).
     *
     * @return List of users needing profile completion
     */
    @Query("SELECT * FROM qdueuser WHERE TRIM(nickname) = '' OR TRIM(email) = '' ORDER BY id ASC")
    List<QDueUserEntity> getIncompleteProfiles();

    /**
     * Get users with only nickname (no email).
     *
     * @return List of users with nickname but no email
     */
    @Query("SELECT * FROM qdueuser WHERE TRIM(nickname) != '' AND TRIM(email) = '' ORDER BY id ASC")
    List<QDueUserEntity> getUsersWithNicknameOnly();

    /**
     * Get users with only email (no nickname).
     *
     * @return List of users with email but no nickname
     */
    @Query("SELECT * FROM qdueuser WHERE TRIM(email) != '' AND TRIM(nickname) = '' ORDER BY id ASC")
    List<QDueUserEntity> getUsersWithEmailOnly();

    // ==================== MAINTENANCE QUERIES ====================

    /**
     * Get the highest user ID (for debugging/maintenance).
     *
     * @return Highest ID in table, or 0 if no users
     */
    @Query("SELECT COALESCE(MAX(id), 0) FROM qdueuser")
    long getMaxUserId();

    /**
     * Get the lowest user ID (should be 1 if users exist).
     *
     * @return Lowest ID in table, or 0 if no users
     */
    @Query("SELECT COALESCE(MIN(id), 0) FROM qdueuser")
    long getMinUserId();
}