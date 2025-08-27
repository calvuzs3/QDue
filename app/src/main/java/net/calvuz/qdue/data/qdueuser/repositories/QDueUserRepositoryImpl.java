package net.calvuz.qdue.data.qdueuser.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.qdueuser.dao.QDueUserDao;
import net.calvuz.qdue.data.qdueuser.entities.QDueUserEntity;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * QDueUserRepositoryImpl - Clean Architecture Repository Implementation
 *
 * <p>Data layer implementation of QDueUserRepository providing database operations
 * through Room DAO while maintaining clean architecture principles.</p>
 *
 * <h3>Implementation Features:</h3>
 * <ul>
 *   <li><strong>Async Operations</strong>: All database operations run on background threads</li>
 *   <li><strong>Entity Conversion</strong>: Automatic conversion between domain models and entities</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error handling with OperationResult pattern</li>
 *   <li><strong>Resource Management</strong>: Proper ExecutorService lifecycle management</li>
 * </ul>
 *
 * <h3>Database Integration:</h3>
 * <ul>
 *   <li><strong>DAO Delegation</strong>: Direct delegation to QDueUserDao for database operations</li>
 *   <li><strong>Transaction Safety</strong>: Safe handling of database transactions</li>
 *   <li><strong>Null Safety</strong>: Proper null handling and validation</li>
 *   <li><strong>Performance</strong>: Optimized queries and efficient data conversion</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Repository Implementation
 * @since Clean Architecture Phase 3
 */
public class QDueUserRepositoryImpl implements QDueUserRepository {

    private static final String TAG = "QDueUserRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final QDueUserDao mQDueUserDao;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param qDueUserDao DAO for database operations
     */
    public QDueUserRepositoryImpl(@NonNull QDueUserDao qDueUserDao) {
        this.mQDueUserDao = qDueUserDao;
        this.mExecutorService = Executors.newFixedThreadPool(3);
        Log.d(TAG, "QDueUserRepositoryImpl initialized with DAO dependency");
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<QDueUser>> createUser(@NonNull QDueUser qDueUser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating QDueUser: " + qDueUser );

                // Convert domain model to entity
                QDueUserEntity entity = QDueUserEntity.fromDomainModel(qDueUser);

                // Insert entity and get generated ID
                long generatedId = mQDueUserDao.insertUser(entity);

                if (generatedId > 0) {
                    // Create domain model with generated ID
                    QDueUser createdUser = new QDueUser(generatedId, qDueUser.getNickname(), qDueUser.getEmail());

                    Log.d(TAG, "✅ Successfully created QDueUser with ID: " + generatedId);
                    return OperationResult.success(
                            createdUser,
                            "User created successfully with ID: " + generatedId,
                            OperationResult.OperationType.CREATE
                    );
                } else {
                    Log.e(TAG, "❌ Failed to create QDueUser - no ID generated");
                    return OperationResult.failure(
                            "Failed to create user - no ID generated",
                            OperationResult.OperationType.CREATE
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception creating QDueUser", e);
                return OperationResult.failure(
                        "Database error creating user: " + e.getMessage(),
                        OperationResult.OperationType.CREATE
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> getUserById(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting QDueUser by ID: " + userId);

                QDueUserEntity entity = mQDueUserDao.getUserById(userId);

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    Log.d(TAG, "✅ Found QDueUser: " + user.getDisplayName());
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    Log.w(TAG, "⚠️ QDueUser not found with ID: " + userId);
                    return OperationResult.failure(
                            "User not found with ID: " + userId,
                            OperationResult.OperationType.READ
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting QDueUser by ID: " + userId, e);
                return OperationResult.failure(
                        "Database error retrieving user: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> updateUser(@NonNull QDueUser qDueUser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (qDueUser.getId() == null) {
                    return OperationResult.failure(
                            "Cannot update user without ID",
                            OperationResult.OperationType.UPDATE
                    );
                }

                Log.d(TAG, "Updating QDueUser ID: " + qDueUser.getId());

                // Convert domain model to entity
                QDueUserEntity entity = QDueUserEntity.fromDomainModel(qDueUser);

                // Update entity
                int rowsUpdated = mQDueUserDao.updateUser(entity);

                if (rowsUpdated > 0) {
                    Log.d(TAG, "✅ Successfully updated QDueUser ID: " + qDueUser.getId());
                    return OperationResult.success(
                            qDueUser,
                            "User updated successfully",
                            OperationResult.OperationType.UPDATE
                    );
                } else {
                    Log.w(TAG, "⚠️ No rows updated for QDueUser ID: " + qDueUser.getId());
                    return OperationResult.failure(
                            "User not found or no changes made: ID " + qDueUser.getId(),
                            OperationResult.OperationType.UPDATE
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception updating QDueUser", e);
                return OperationResult.failure(
                        "Database error updating user: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<String>> deleteUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting QDueUser ID: " + userId);

                int rowsDeleted = mQDueUserDao.deleteUserById(userId);

                if (rowsDeleted > 0) {
                    Log.d(TAG, "✅ Successfully deleted QDueUser ID: " + userId);
                    return OperationResult.success(
                            "User deleted successfully",
                            OperationResult.OperationType.DELETE
                    );
                } else {
                    Log.w(TAG, "⚠️ No user found to delete with ID: " + userId);
                    return OperationResult.failure(
                            "User not found with ID: " + userId,
                            OperationResult.OperationType.DELETE
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception deleting QDueUser ID: " + userId, e);
                return OperationResult.failure(
                        "Database error deleting user: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting all QDueUsers");

                int rowsDeleted = mQDueUserDao.deleteAllUsers();

                Log.d(TAG, "✅ Deleted " + rowsDeleted + " QDueUsers");
                return OperationResult.success(
                        rowsDeleted,
                        "Deleted " + rowsDeleted + " users",
                        OperationResult.OperationType.DELETE
                );

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception deleting all QDueUsers", e);
                return OperationResult.failure(
                        "Database error deleting all users: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService);
    }

    // ==================== BUSINESS QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<QDueUser>> getUserByEmail(@NonNull String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting QDueUser by email: " + email);

                QDueUserEntity entity = mQDueUserDao.getUserByEmail(email);

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    Log.d(TAG, "✅ Found QDueUser by email: " + user.getDisplayName());
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    Log.w(TAG, "⚠️ QDueUser not found with email: " + email);
                    return OperationResult.failure(
                            "User not found with email: " + email,
                            OperationResult.OperationType.READ
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting QDueUser by email: " + email, e);
                return OperationResult.failure(
                        "Database error retrieving user by email: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<QDueUser>>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all QDueUsers");

                List<QDueUserEntity> entities = mQDueUserDao.getAllUsers();
                List<QDueUser> users = entities.stream()
                        .map(QDueUserEntity::toDomainModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "✅ Retrieved " + users.size() + " QDueUsers");
                return OperationResult.success(users, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting all QDueUsers", e);
                return OperationResult.failure(
                        "Database error retrieving all users: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> getPrimaryUser() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting primary QDueUser");

                QDueUserEntity entity = mQDueUserDao.getPrimaryUser();

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    Log.d(TAG, "✅ Found primary QDueUser: " + user.getDisplayName());
                    return OperationResult.success(user, OperationResult.OperationType.READ);
                } else {
                    Log.w(TAG, "⚠️ No primary QDueUser found");
                    return OperationResult.failure(
                            "No primary user found",
                            OperationResult.OperationType.READ
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting primary QDueUser", e);
                return OperationResult.failure(
                        "Database error retrieving primary user: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    // ==================== EXISTENCE CHECKS ====================

    @Override
    public CompletableFuture<OperationResult<Boolean>> userExists(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mQDueUserDao.getUserCountById(userId);
                boolean exists = count > 0;
                Log.d(TAG, "User exists check for ID " + userId + ": " + exists);
                return OperationResult.success(exists, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception checking user existence for ID: " + userId, e);
                return OperationResult.failure(
                        "Database error checking user existence: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> userExistsByEmail(@NonNull String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mQDueUserDao.getUserCountByEmail(email);
                boolean exists = count > 0;
                Log.d(TAG, "User exists check for email " + email + ": " + exists);
                return OperationResult.success(exists, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception checking user existence for email: " + email, e);
                return OperationResult.failure(
                        "Database error checking user existence by email: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getUsersCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mQDueUserDao.getTotalUsersCount();
                Log.d(TAG, "Total users count: " + count);
                return OperationResult.success(count, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting users count", e);
                return OperationResult.failure(
                        "Database error getting users count: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> getCompleteProfilesCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mQDueUserDao.getCompleteProfilesCount();
                Log.d(TAG, "Complete profiles count: " + count);
                return OperationResult.success(count, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting complete profiles count", e);
                return OperationResult.failure(
                        "Database error getting complete profiles count: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<QDueUser>>> getIncompleteProfiles() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting incomplete profiles");

                List<QDueUserEntity> entities = mQDueUserDao.getIncompleteProfiles();
                List<QDueUser> users = entities.stream()
                        .map(QDueUserEntity::toDomainModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "✅ Retrieved " + users.size() + " incomplete profiles");
                return OperationResult.success(users, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception getting incomplete profiles", e);
                return OperationResult.failure(
                        "Database error retrieving incomplete profiles: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService);
    }

    // ==================== CLEANUP ====================

    /**
     * Shutdown executor service for proper resource cleanup.
     * Should be called when repository is no longer needed.
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown completed");
        }
    }
}