package net.calvuz.qdue.user.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.models.UserWithOrganization;

import java.util.List;

/**
 * DAO for User operations.
 * Handles all user-related data persistence including authentication data.
 */
@Dao
public interface UserDao {

    // Basic CRUD operations
    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(long id);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE google_id = :googleId LIMIT 1")
    User getUserByGoogleId(String googleId);

    @Query("SELECT * FROM users WHERE employee_id = :employeeId LIMIT 1")
    User getUserByEmployeeId(String employeeId);

    @Query("SELECT * FROM users WHERE is_active = 1 LIMIT 1")
    User getActiveUser();

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    List<User> getAllUsers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteUserById(long id);

    @Query("DELETE FROM users")
    void deleteAllUsers();

    // Authentication queries
    @Query("SELECT * FROM users WHERE auth_provider = :provider")
    List<User> getUsersByAuthProvider(String provider);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE google_id = :googleId)")
    boolean existsByGoogleId(String googleId);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean existsByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE employee_id = :employeeId)")
    boolean existsByEmployeeId(String employeeId);

    // Profile and status queries
    @Query("SELECT * FROM users WHERE profile_completed = 0")
    List<User> getIncompleteProfiles();

    @Query("SELECT * FROM users WHERE is_active = 1")
    List<User> getActiveUsers();

    @Query("UPDATE users SET last_login = :date, updated_at = :date WHERE id = :userId")
    void updateLastLogin(long userId, java.time.LocalDateTime date);

    @Query("UPDATE users SET profile_completed = 1, updated_at = :date WHERE id = :userId")
    void markProfileCompleted(long userId, java.time.LocalDateTime date);

    @Query("UPDATE users SET is_active = :isActive, updated_at = :date WHERE id = :userId")
    void updateUserStatus(long userId, boolean isActive, java.time.LocalDateTime date);

    // Organizational queries
    @Query("SELECT * FROM users WHERE establishment_id = :establishmentId")
    List<User> getUsersByEstablishment(long establishmentId);

    @Query("SELECT * FROM users WHERE macro_department_id = :macroDepartmentId")
    List<User> getUsersByMacroDepartment(long macroDepartmentId);

    @Query("SELECT * FROM users WHERE sub_department_id = :subDepartmentId")
    List<User> getUsersBySubDepartment(long subDepartmentId);

    @Query("SELECT * FROM users WHERE team_name = :teamName")
    List<User> getUsersByTeam(String teamName);

    // Complex queries with joins - returns detailed user info
    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    UserWithOrganization getUserWithOrganization(long userId);

    @Transaction
    @Query("SELECT * FROM users WHERE is_active = 1 LIMIT 1")
    UserWithOrganization getActiveUserWithOrganization();

    // Statistics and reporting
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    @Query("SELECT COUNT(*) FROM users WHERE is_active = 1")
    int getActiveUserCount();

    @Query("SELECT COUNT(*) FROM users WHERE profile_completed = 1")
    int getCompletedProfileCount();

    @Query("SELECT COUNT(*) FROM users WHERE auth_provider = 'google'")
    int getGoogleAuthUserCount();

    // Data cleanup and maintenance
    @Query("DELETE FROM users WHERE is_active = 0 AND updated_at < :cutoffDate")
    void deleteInactiveUsers(java.time.LocalDateTime cutoffDate);

    @Query("UPDATE users SET profile_image_url = NULL, profile_image_source = 'none', updated_at = :date WHERE profile_image_source = 'local'")
    void clearLocalProfileImages(java.time.LocalDateTime date);

    // Search and filtering
    @Query("SELECT * FROM users WHERE " +
            "(first_name LIKE '%' || :query || '%' OR " +
            "last_name LIKE '%' || :query || '%' OR " +
            "nickname LIKE '%' || :query || '%' OR " +
            "email LIKE '%' || :query || '%' OR " +
            "employee_id LIKE '%' || :query || '%') " +
            "AND is_active = 1 " +
            "ORDER BY first_name ASC, last_name ASC")
    List<User> searchActiveUsers(String query);

    @Query("SELECT DISTINCT job_title FROM users WHERE job_title IS NOT NULL AND job_title != '' ORDER BY job_title ASC")
    List<String> getAllJobTitles();

    @Query("SELECT DISTINCT job_level FROM users WHERE job_level IS NOT NULL AND job_level != '' ORDER BY job_level ASC")
    List<String> getAllJobLevels();

    @Query("SELECT DISTINCT team_name FROM users WHERE team_name IS NOT NULL AND team_name != '' ORDER BY team_name ASC")
    List<String> getAllTeamNames();
}