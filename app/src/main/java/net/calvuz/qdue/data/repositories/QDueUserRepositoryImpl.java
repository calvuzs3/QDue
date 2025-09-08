package net.calvuz.qdue.data.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.QDueUserDao;
import net.calvuz.qdue.data.entities.QDueUserEntity;
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
     * @param database for QDueUserDao access
     */
    public QDueUserRepositoryImpl(@NonNull CalendarDatabase database) {
        this.mQDueUserDao = database.qDueUserDao();
        this.mExecutorService = Executors.newSingleThreadExecutor();

        // Initialize standard recurrence rules if needed
//        initializeDefaultUserIfNeeded();

        Log.d( TAG, "QDueUserRepositoryImpl initialized" );
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<QDueUser>> createUser(@NonNull QDueUser qDueUser) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Convert domain model to entity
                QDueUserEntity entity = QDueUserEntity.fromDomainModel( qDueUser );

                long result = mQDueUserDao.insertUser( entity );

                Log.w( TAG, "createUser() entity{" + entity + "} result{" + result + "}" );

                QDueUser domainUser = entity.toDomainModel();
                Log.w( TAG, "createUser() domainUser{" + domainUser + "}" );

                if (result == 1) {
                    return OperationResult.success(
                            entity.toDomainModel(),
                            "User created successfully with ID: " + entity.getId(), // entity.toDomainModel().getId(),
                            OperationResult.OperationType.CREATE
                    );
                } else {
                    return OperationResult.failure(
                            "Failed to create user",
                            OperationResult.OperationType.CREATE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception creating QDueUser", e );
                return OperationResult.failure(
                        "Database error creating user: " + qDueUser,
                        OperationResult.OperationType.CREATE
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> readUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                QDueUserEntity entity = mQDueUserDao.getUserById( userId );

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    return OperationResult.success( user, OperationResult.OperationType.READ );
                } else {
                    return OperationResult.failure(
                            "User not found with ID: " + userId,
                            OperationResult.OperationType.READ
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception getting QDueUser by ID: " + userId, e );
                return OperationResult.failure(
                        "Database error retrieving user: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> updateUser(@NonNull QDueUser qDueUser) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Convert domain model to entity
                QDueUserEntity entity = QDueUserEntity.fromDomainModel( qDueUser );

                // Update entity
                int rowsUpdated = mQDueUserDao.updateUser( entity );

                if (rowsUpdated > 0) {
                    return OperationResult.success(
                            qDueUser,
                            "User updated successfully",
                            OperationResult.OperationType.UPDATE
                    );
                } else {
                    return OperationResult.failure(
                            "User not found or no changes made: ID " + qDueUser.getId(),
                            OperationResult.OperationType.UPDATE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception updating QDueUser", e );
                return OperationResult.failure(
                        "Database error updating user: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> deleteUser(@NonNull QDueUser user) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int rowsDeleted = mQDueUserDao.deleteUserById( user.getId() );

                if (rowsDeleted > 0) {
                    return OperationResult.success(
                            true,
                            "User deleted successfully",
                            OperationResult.OperationType.DELETE
                    );
                } else {
                    return OperationResult.failure(
                            "User not found with ID: " + user.getId(),
                            OperationResult.OperationType.DELETE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "Exception deleting QDueUser ID: " + user.getId(), e );
                return OperationResult.failure(
                        "Database error deleting user: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService );
    }

    // ==================== BULK OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllUsers() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int rowsDeleted = mQDueUserDao.deleteAllUsers();

                return OperationResult.success(
                        rowsDeleted,
                        "Deleted " + rowsDeleted + " users",
                        OperationResult.OperationType.DELETE
                );
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception deleting all QDueUsers", e );
                return OperationResult.failure(
                        "Database error deleting all users: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService );
    }

    // ==================== BUSINESS QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<QDueUser>> readUserByEmail(@NonNull String email) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                QDueUserEntity entity = mQDueUserDao.getUserByEmail( email );

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    return OperationResult.success( user, OperationResult.OperationType.READ );
                } else {
                    return OperationResult.failure(
                            "User not found with email: " + email,
                            OperationResult.OperationType.READ
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception getting QDueUser by email: " + email, e );
                return OperationResult.failure(
                        "Database error retrieving user by email: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<QDueUser>>> getAllUsers() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<QDueUserEntity> entities = mQDueUserDao.getAllUsers();
                List<QDueUser> users = entities.stream()
                        .map( QDueUserEntity::toDomainModel )
                        .collect( Collectors.toList() );

                return OperationResult.success( users, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception getting all QDueUsers", e );
                return OperationResult.failure(
                        "Database error retrieving all users",
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<QDueUser>> getPrimaryUser() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                QDueUserEntity entity = mQDueUserDao.getPrimaryUser();

                if (entity != null) {
                    QDueUser user = entity.toDomainModel();
                    return OperationResult.success( user, OperationResult.OperationType.READ );
                } else {
                    return OperationResult.failure(
                            "No primary user found",
                            OperationResult.OperationType.READ
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception getting primary QDueUser", e );
                return OperationResult.failure(
                        "Database error retrieving primary user: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== EXISTENCE CHECKS ====================

    @Override
    public CompletableFuture<OperationResult<Boolean>> userExists(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = mQDueUserDao.getUserCountById( userId );
                boolean exists = count > 0;
                return OperationResult.success( exists, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception checking user existence for ID: " + userId, e );
                return OperationResult.failure(
                        "Database error checking user existence: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> userExistsByEmail(@NonNull String email) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = mQDueUserDao.getUserCountByEmail( email );
                boolean exists = count > 0;
                return OperationResult.success( exists, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception checking user existence for email: " + email, e );
                return OperationResult.failure(
                        "Database error checking user existence by email: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getUsersCount() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = mQDueUserDao.getTotalUsersCount();
                return OperationResult.success( count, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "❌ Exception getting users count", e );
                return OperationResult.failure(
                        "Database error getting users count: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== CLEANUP ====================

    /**
     * Shutdown executor service for proper resource cleanup.
     * Should be called when repository is no longer needed.
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d( TAG, "ExecutorService shutdown completed" );
        }
    }
}