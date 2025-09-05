package net.calvuz.qdue.data.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.data.entities.QDueUserEntity;

import java.util.List;

/// QDueUserDao - Room Database Access Object for QDueUser Operations
///
/// Data access layer for QDueUser entity providing direct database operations following Room patterns.
/// Implements efficient queries optimized for the simplified user management requirements.
@Dao
public interface QDueUserDao {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Insert new QDueUser entity.
     * ID will be auto-generated starting from 1L.
     *
     * @param qDueUserEntity Entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(@NonNull QDueUserEntity qDueUserEntity);

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
    @Query("DELETE FROM user WHERE id = :userId")
    int deleteUserById(@NonNull String userId);

    /**
     * Delete all QDueUser entities.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM user")
    int deleteAllUsers();

    // ==================== RETRIEVAL QUERIES ====================

    /**
     * Get QDueUser by ID.
     *
     * @param userId User ID to find
     * @return QDueUserEntity if found, null otherwise
     */
    @Query("SELECT * FROM user WHERE id = :userId LIMIT 1")
    QDueUserEntity getUserById(@NonNull String userId);

    /**
     * Get QDueUser by email address.
     *
     * @param email Email to search for
     * @return QDueUserEntity if found, null otherwise
     */
    @Query("SELECT * FROM user WHERE TRIM(LOWER(email)) = TRIM(LOWER(:email)) LIMIT 1")
    QDueUserEntity getUserByEmail(@NonNull String email);

    /**
     * Get all QDueUser entities ordered by ID.
     *
     * @return List of all QDueUserEntity objects
     */
    @Query("SELECT * FROM user ORDER BY id ASC")
    List<QDueUserEntity> getAllUsers();

    /**
     * Get primary user (first user created, typically ID = 1).
     *
     * @return First QDueUserEntity by ID
     */
    @Query("SELECT * FROM user ORDER BY nickname ASC, id ASC LIMIT 1")
    QDueUserEntity getPrimaryUser();

    // ==================== EXISTENCE CHECKS ====================

    /**
     * Check if user exists by ID.
     *
     * @param userId User ID to check
     * @return Count of users with ID (1 if exists, 0 otherwise)
     */
    @Query("SELECT COUNT(*) FROM user WHERE id = :userId")
    int getUserCountById(@NonNull String userId);

    /**
     * Check if user exists by email.
     *
     * @param email Email to check
     * @return Count of users with email (1 if exists, 0 otherwise)
     */
    @Query("SELECT COUNT(*) FROM user WHERE TRIM(LOWER(email)) = TRIM(LOWER(:email))")
    int getUserCountByEmail(@NonNull String email);

    // ==================== STATISTICS QUERIES ====================

    /**
     * Get total count of QDueUser entities.
     *
     * @return Total user count
     */
    @Query("SELECT COUNT(*) FROM user")
    int getTotalUsersCount();

}