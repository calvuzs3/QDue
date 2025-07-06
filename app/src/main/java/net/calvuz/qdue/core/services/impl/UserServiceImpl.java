package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.dao.UserDao;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: User Service Implementation with Dependency Injection
 *
 * Centralized implementation for all User operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 *
 * Fully compliant with UserService interface contract.
 */
public class UserServiceImpl implements UserService {

    private static final String TAG = "UserServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final UserDao mUserDao;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection
     *
     * @param context Application context
     * @param database QDue database instance
     * @param backupManager Core backup manager instance
     */
    public UserServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mUserDao = database.userDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "UserServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public UserServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<User>> createUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate user input
                OperationResult<Void> validation = validateUser(user);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for email duplicates
                if (user.getEmail() != null && mUserDao.existsByEmail(user.getEmail())) {
                    return OperationResult.failure("User with email already exists: " + user.getEmail(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for employee ID duplicates
                if (user.getEmployeeId() != null && mUserDao.existsByEmployeeId(user.getEmployeeId())) {
                    return OperationResult.failure("User with employee ID already exists: " + user.getEmployeeId(),
                            OperationResult.OperationType.CREATE);
                }

                // Set creation timestamps
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                // Insert user and get generated ID
                long userId = mUserDao.insertUser(user);
                user.setId(userId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("users", "create");

                Log.d(TAG, "User created successfully: " + user.getEmail());
                return OperationResult.success(user, "User created successfully",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> updateUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate user input
                OperationResult<Void> validation = validateUser(user);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check if user exists
                User existingUser = mUserDao.getUserById(user.getId());
                if (existingUser == null) {
                    return OperationResult.failure("User not found: " + user.getId(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check email uniqueness (excluding current user)
                if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
                    if (mUserDao.existsByEmail(user.getEmail())) {
                        return OperationResult.failure("Email already in use: " + user.getEmail(),
                                OperationResult.OperationType.UPDATE);
                    }
                }

                // Check employee ID uniqueness (excluding current user)
                if (user.getEmployeeId() != null && !user.getEmployeeId().equals(existingUser.getEmployeeId())) {
                    if (mUserDao.existsByEmployeeId(user.getEmployeeId())) {
                        return OperationResult.failure("Employee ID already in use: " + user.getEmployeeId(),
                                OperationResult.OperationType.UPDATE);
                    }
                }

                // Set update timestamp
                user.setUpdatedAt(LocalDateTime.now());

                // Update user
                mUserDao.updateUser(user);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("users", "update");

                Log.d(TAG, "User updated successfully: " + user.getEmail());
                return OperationResult.success(user, "User updated successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<String>> deleteUser(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get user before deletion for logging
                User user = mUserDao.getUserById(userId);
                if (user == null) {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.DELETE);
                }

                String userEmail = user.getEmail();

                // Delete user
                mUserDao.deleteUserById(userId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("users", "delete");

                Log.d(TAG, "User deleted successfully: " + userEmail);
                return OperationResult.success(userEmail, "User deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get count before deletion
                List<User> users = mUserDao.getAllUsers();
                int count = users.size();

                if (count == 0) {
                    return OperationResult.success(0, "No users to delete",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete all users
                mUserDao.deleteAllUsers();

                // Trigger automatic backup
                mBackupManager.performAutoBackup("users", "bulk_delete");

                Log.d(TAG, "Deleted all " + count + " users successfully");
                return OperationResult.success(count, "Deleted all " + count + " users",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all users: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    // ==================== QUERY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<User>> getUserById(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = mUserDao.getUserById(userId);
                if (user != null) {
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get user by ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> getUserByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = mUserDao.getUserByEmail(email);
                if (user != null) {
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("User not found with email: " + email,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get user by email: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> getUserByGoogleId(String googleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = mUserDao.getUserByGoogleId(googleId);
                if (user != null) {
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("User not found with Google ID: " + googleId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get user by Google ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> getUserByEmployeeId(String employeeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = mUserDao.getUserByEmployeeId(employeeId);
                if (user != null) {
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("User not found with employee ID: " + employeeId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get user by employee ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> getActiveUser() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User activeUser = mUserDao.getActiveUser();
                if (activeUser != null) {
                    return OperationResult.success(activeUser, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("No active user found",
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get active user", e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<User>>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<User> users = mUserDao.getAllUsers();
                return OperationResult.success(users, OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all users: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== AUTHENTICATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<User>> authenticateUser(String email, String provider) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Input validation
                if (email == null || email.trim().isEmpty()) {
                    return OperationResult.failure("Email is required for authentication",
                            OperationResult.OperationType.READ);
                }
                if (provider == null || provider.trim().isEmpty()) {
                    return OperationResult.failure("Authentication provider is required",
                            OperationResult.OperationType.READ);
                }

                // Find user by email
                User user = mUserDao.getUserByEmail(email);
                if (user == null) {
                    return OperationResult.failure("User not found: " + email,
                            OperationResult.OperationType.READ);
                }

                // Check if provider matches
                if (!provider.equals(user.getAuthProvider())) {
                    return OperationResult.failure("Authentication provider mismatch. Expected: " +
                                    user.getAuthProvider() + ", provided: " + provider,
                            OperationResult.OperationType.READ);
                }

                // Update last login time and set as active
                user.setLastLogin(LocalDateTime.now());
                user.setActive(true);
                user.setUpdatedAt(LocalDateTime.now());
                mUserDao.updateUser(user);

                // Trigger backup for user activity update
                mBackupManager.performAutoBackup("users", "update");

                Log.d(TAG, "User authenticated successfully: " + email);
                return OperationResult.success(user, "User authenticated successfully",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to authenticate user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<User>> registerUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Additional registration-specific validation
                if (user.getAuthProvider() == null || user.getAuthProvider().trim().isEmpty()) {
                    return OperationResult.failure("Authentication provider is required for registration",
                            OperationResult.OperationType.CREATE);
                }

                // Set registration-specific defaults
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user.setActive(true);

                // Use create user method which handles all validation and duplicate checks
                OperationResult<User> result = createUser(user).get();

                if (result.isSuccess()) {
                    Log.d(TAG, "User registered successfully: " + user.getEmail());
                    return OperationResult.success(result.getData(), "User registered successfully",
                            OperationResult.OperationType.CREATE);
                } else {
                    return result;
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to register user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Void>> logoutUser(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get user
                User user = mUserDao.getUserById(userId);
                if (user == null) {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.UPDATE);
                }

                // Set user as inactive
                user.setActive(false);
                user.setUpdatedAt(LocalDateTime.now());
                mUserDao.updateUser(user);

                // Trigger backup for user state change
                mBackupManager.performAutoBackup("users", "update");

                Log.d(TAG, "User logged out successfully: " + user.getEmail());
                return OperationResult.success("User logged out successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to logout user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Void>> setActiveUser(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, deactivate all users
                List<User> allUsers = mUserDao.getAllUsers();
                for (User user : allUsers) {
                    if (user.isActive()) {
                        user.setActive(false);
                        user.setUpdatedAt(LocalDateTime.now());
                        mUserDao.updateUser(user);
                    }
                }

                // Then activate target user
                User targetUser = mUserDao.getUserById(userId);
                if (targetUser == null) {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.UPDATE);
                }

                targetUser.setActive(true);
                targetUser.setLastLogin(LocalDateTime.now());
                targetUser.setUpdatedAt(LocalDateTime.now());
                mUserDao.updateUser(targetUser);

                // Trigger backup for user state changes
                mBackupManager.performAutoBackup("users", "update");

                Log.d(TAG, "Set active user: " + targetUser.getEmail());
                return OperationResult.success("Active user set successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to set active user: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    // ==================== PROFILE OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<User>> updateProfile(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if profile is now complete
                boolean isComplete = isProfileComplete(user);
                user.setProfileCompleted(isComplete);

                // Use standard update user method
                OperationResult<User> result = updateUser(user).get();

                if (result.isSuccess()) {
                    Log.d(TAG, "Profile updated successfully: " + user.getEmail() +
                            " (complete: " + isComplete + ")");
                    return OperationResult.success(result.getData(),
                            "Profile updated successfully" + (isComplete ? " and marked as complete" : ""),
                            OperationResult.OperationType.UPDATE);
                } else {
                    return result;
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to update profile: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<User>>> getIncompleteProfiles() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<User> incompleteUsers = mUserDao.getIncompleteProfiles();
                return OperationResult.success(incompleteUsers,
                        "Found " + incompleteUsers.size() + " incomplete profiles",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get incomplete profiles: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Void>> completeProfile(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = mUserDao.getUserById(userId);
                if (user == null) {
                    return OperationResult.failure("User not found: " + userId,
                            OperationResult.OperationType.UPDATE);
                }

                // Mark profile as completed
                user.setProfileCompleted(true);
                user.setUpdatedAt(LocalDateTime.now());
                mUserDao.updateUser(user);

                // Trigger backup
                mBackupManager.performAutoBackup("users", "update");

                Log.d(TAG, "Profile completed for user: " + user.getEmail());
                return OperationResult.success("Profile marked as complete",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to complete profile: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateUser(User user) {
        List<String> errors = new ArrayList<>();

        // Basic null check
        if (user == null) {
            errors.add("User cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // Email validation
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!isValidEmail(user.getEmail())) {
            errors.add("Invalid email format");
        }

        // Name validation
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            errors.add("First name is required");
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            errors.add("Last name is required");
        }

        // Name length validation
        if (user.getFirstName() != null && user.getFirstName().length() > 50) {
            errors.add("First name cannot exceed 50 characters");
        }
        if (user.getLastName() != null && user.getLastName().length() > 50) {
            errors.add("Last name cannot exceed 50 characters");
        }

        // Employee ID validation (if provided)
        if (user.getEmployeeId() != null && user.getEmployeeId().trim().isEmpty()) {
            errors.add("Employee ID cannot be empty if provided");
        }
        if (user.getEmployeeId() != null && user.getEmployeeId().length() > 20) {
            errors.add("Employee ID cannot exceed 20 characters");
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> userExists(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (email == null || email.trim().isEmpty()) {
                    return OperationResult.failure("Email is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mUserDao.existsByEmail(email);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check user existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> employeeIdExists(String employeeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (employeeId == null || employeeId.trim().isEmpty()) {
                    return OperationResult.failure("Employee ID is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mUserDao.existsByEmployeeId(employeeId);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check employee ID existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getUsersCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mUserDao.getAllUsers().size();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get users count", e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<User>>> getUsersByAuthProvider(String provider) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (provider == null || provider.trim().isEmpty()) {
                    return OperationResult.failure("Authentication provider is required",
                            OperationResult.OperationType.READ);
                }

                List<User> users = mUserDao.getUsersByAuthProvider(provider);
                return OperationResult.success(users,
                        "Found " + users.size() + " users with provider: " + provider,
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get users by auth provider: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate email format using regex
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Check if user profile is complete based on required fields
     */
    private boolean isProfileComplete(User user) {
        return user.getFirstName() != null && !user.getFirstName().trim().isEmpty() &&
                user.getLastName() != null && !user.getLastName().trim().isEmpty() &&
                user.getEmail() != null && !user.getEmail().trim().isEmpty();
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when service is no longer needed
     * Should be called from DI container or application lifecycle
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "UserServiceImpl executor service shutdown");
        }
    }
}