package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.UserTeamAssignmentDao;
import net.calvuz.qdue.data.entities.UserTeamAssignmentEntity;
import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.calendar.repositories.UserTeamAssignmentRepository;
import net.calvuz.qdue.domain.common.enums.Status;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * UserTeamAssignmentRepositoryImpl - Clean Architecture Repository Implementation
 *
 * <p>Data layer implementation of UserTeamAssignmentRepository providing database operations
 * through Room DAO while maintaining clean architecture principles and business rule validation.</p>
 *
 * <h3>Implementation Features:</h3>
 * <ul>
 *   <li><strong>Async Operations</strong>: All database operations run on background threads</li>
 *   <li><strong>Entity Conversion</strong>: Automatic conversion between domain models and entities</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error handling with OperationResult pattern</li>
 *   <li><strong>Business Logic</strong>: Assignment validation and conflict detection</li>
 *   <li><strong>Resource Management</strong>: Proper ExecutorService lifecycle management</li>
 * </ul>
 *
 * <h3>Database Integration:</h3>
 * <ul>
 *   <li><strong>DAO Delegation</strong>: Direct delegation to UserTeamAssignmentDao for database operations</li>
 *   <li><strong>Transaction Safety</strong>: Safe handling of database transactions and batch operations</li>
 *   <li><strong>Null Safety</strong>: Proper null handling and validation throughout</li>
 *   <li><strong>Performance</strong>: Optimized queries and efficient data conversion</li>
 * </ul>
 *
 * <h3>Business Logic Implementation:</h3>
 * <ul>
 *   <li><strong>Assignment Validation</strong>: Conflict detection and date validation</li>
 *   <li><strong>Status Management</strong>: Automatic status computation and updates</li>
 *   <li><strong>Audit Trail</strong>: Tracking of creation and modification metadata</li>
 *   <li><strong>Soft Delete</strong>: Deactivation instead of hard deletion for data integrity</li>
 * </ul>
 */
public class UserTeamAssignmentRepositoryImpl implements UserTeamAssignmentRepository {

    private static final String TAG = "UserTeamAssignmentRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final UserTeamAssignmentDao mAssignmentDao;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection following clean architecture principles.
     *
     * @param context       Application context
     * @param assignmentDao UserTeamAssignmentDao for database operations
     */
    public UserTeamAssignmentRepositoryImpl(@NonNull Context context,
                                            @NonNull UserTeamAssignmentDao assignmentDao) {
        this.mAssignmentDao = assignmentDao;
        this.mExecutorService = Executors.newSingleThreadExecutor(); // .newFixedThreadPool(1, r -> {
//            Thread thread = new Thread(r, "UserTeamAssignmentRepo-Thread");
//            thread.setDaemon(true);
//            return thread;
//        });

        Log.d( TAG, "UserTeamAssignmentRepositoryImpl initialized with clean architecture support" );
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<UserTeamAssignment>> createAssignment(@NonNull UserTeamAssignment assignment) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Creating assignment: " + assignment.getId() );

                // Validate assignment before creation
                OperationResult<AssignmentValidationResult> validation = performAssignmentValidation(
                        assignment.getUserID(),
                        assignment.getTeamID(),
                        assignment.getStartDate(),
                        assignment.getEndDate(),
                        null // No exclusion for new assignment
                );

                if (!validation.isSuccess() || !validation.getData().isValid()) {
                    String message = validation.isSuccess() ?
                            validation.getData().validationMessage() :
                            validation.getErrorMessage();
                    return OperationResult.failure( "Assignment validation failed: " + message,
                            OperationResult.OperationType.CREATE );
                }

                // Compute status if not set
                UserTeamAssignment assignmentToCreate = assignment.getStatus() != null ?
                        assignment :
                        UserTeamAssignment.builder()
                                .copyFrom( assignment )
                                .computeStatus()
                                .build();

                // Convert to entity and insert
                UserTeamAssignmentEntity entity = UserTeamAssignmentEntity.fromDomainModel( assignmentToCreate );
                long rowId = mAssignmentDao.insert( entity );

                if (rowId > 0) {
                    UserTeamAssignment createdAssignment = entity.toDomainModel();
                    Log.d( TAG, "✅ Assignment created successfully: " + createdAssignment.getId() );
                    return OperationResult.success( createdAssignment,
                            OperationResult.OperationType.CREATE );
                } else {
                    return OperationResult.failure( "Failed to insert assignment into database",
                            OperationResult.OperationType.CREATE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error creating assignment", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.CREATE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> createAssignments(@NonNull List<UserTeamAssignment> assignments) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Creating " + assignments.size() + " assignments in batch" );

                // Validate all assignments first
                for (UserTeamAssignment assignment : assignments) {
                    OperationResult<AssignmentValidationResult> validation = performAssignmentValidation(
                            assignment.getUserID(),
                            assignment.getTeamID(),
                            assignment.getStartDate(),
                            assignment.getEndDate(),
                            null
                    );

                    if (!validation.isSuccess() || !validation.getData().isValid()) {
                        String message = validation.isSuccess() ?
                                validation.getData().validationMessage() :
                                validation.getErrorMessage();
                        return OperationResult.failure( "Assignment validation failed for " + assignment.getId() + ": " + message,
                                OperationResult.OperationType.CREATE );
                    }
                }

                // Convert all to entities with computed status
                List<UserTeamAssignmentEntity> entities = assignments.stream()
                        .map( assignment -> {
                            UserTeamAssignment assignmentToCreate = assignment.getStatus() != null ?
                                    assignment :
                                    UserTeamAssignment.builder()
                                            .copyFrom( assignment )
                                            .computeStatus()
                                            .build();
                            return UserTeamAssignmentEntity.fromDomainModel( assignmentToCreate );
                        } )
                        .collect( Collectors.toList() );

                // Insert all in single transaction
                long[] rowIds = mAssignmentDao.insertAll( entities );

                if (rowIds.length == entities.size()) {
                    List<UserTeamAssignment> createdAssignments = entities.stream()
                            .map( UserTeamAssignmentEntity::toDomainModel )
                            .collect( Collectors.toList() );

                    Log.d( TAG, "✅ Created " + createdAssignments.size() + " assignments successfully" );
                    return OperationResult.success( createdAssignments,
                            OperationResult.OperationType.CREATE );
                } else {
                    return OperationResult.failure( "Failed to insert all assignments. Expected: " +
                                    entities.size() + ", Inserted: " + rowIds.length,
                            OperationResult.OperationType.CREATE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error creating assignments batch", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.CREATE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<UserTeamAssignment>> getAssignmentById(@NonNull String assignmentId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignment by ID: " + assignmentId );

                UserTeamAssignmentEntity entity = mAssignmentDao.getById( assignmentId );
                if (entity != null) {
                    UserTeamAssignment assignment = entity.toDomainModel();
                    Log.d( TAG, "✅ Found assignment: " + assignment.getId() );
                    return OperationResult.success( assignment,
                            OperationResult.OperationType.READ );
                } else {
                    Log.w( TAG, "Assignment not found: " + assignmentId );
                    return OperationResult.failure( "Assignment not found: " + assignmentId,
                            OperationResult.OperationType.READ );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignment by ID: " + assignmentId, e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByIds(@NonNull List<String> assignmentIds) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting " + assignmentIds.size() + " assignments by IDs" );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getByIds( assignmentIds );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments out of " + assignmentIds.size() + " requested" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments by IDs", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<UserTeamAssignment>> updateAssignment(@NonNull UserTeamAssignment assignment) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating assignment: " + assignment.getId() );

                // Validate assignment with exclusion of current assignment
                OperationResult<AssignmentValidationResult> validation = performAssignmentValidation(
                        assignment.getUserID(),
                        assignment.getTeamID(),
                        assignment.getStartDate(),
                        assignment.getEndDate(),
                        assignment.getId() // Exclude current assignment from conflict check
                );

                if (!validation.isSuccess() || !validation.getData().isValid()) {
                    String message = validation.isSuccess() ?
                            validation.getData().validationMessage() :
                            validation.getErrorMessage();
                    return OperationResult.failure( "Assignment validation failed: " + message ,
                            OperationResult.OperationType.UPDATE );
                }

                // Ensure status is computed
                UserTeamAssignment assignmentToUpdate = assignment.getStatus() != null ?
                        assignment :
                        UserTeamAssignment.builder()
                                .copyFrom( assignment )
                                .computeStatus()
                                .updatedAt( System.currentTimeMillis() )
                                .build();

                // Update entity
                UserTeamAssignmentEntity entity = UserTeamAssignmentEntity.fromDomainModel( assignmentToUpdate );
                int updatedRows = mAssignmentDao.update( entity );

                if (updatedRows > 0) {
                    UserTeamAssignment updatedAssignment = entity.toDomainModel();
                    Log.d( TAG, "✅ Assignment updated successfully: " + updatedAssignment.getId() );
                    return OperationResult.success( updatedAssignment,
                            OperationResult.OperationType.UPDATE );
                } else {
                    return OperationResult.failure( "Assignment not found for update: " + assignment.getId() ,
                            OperationResult.OperationType.UPDATE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error updating assignment: " + assignment.getId(), e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> deleteAssignment(@NonNull String assignmentId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deleting assignment: " + assignmentId );

                int deletedRows = mAssignmentDao.deleteById( assignmentId );

                if (deletedRows > 0) {
                    Log.d( TAG, "✅ Assignment deleted successfully: " + assignmentId );
                    return OperationResult.success( true ,
                            OperationResult.OperationType.DELETE );
                } else {
                    Log.w( TAG, "Assignment not found for deletion: " + assignmentId );
                    return OperationResult.failure( "Assignment not found: " + assignmentId ,
                            OperationResult.OperationType.DELETE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error deleting assignment: " + assignmentId, e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.DELETE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<UserTeamAssignment>> deactivateAssignment(@NonNull String assignmentId,
                                                                                       @Nullable String deactivatedByUserId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deactivating assignment: " + assignmentId );

                long timestamp = System.currentTimeMillis();
                int updatedRows = mAssignmentDao.softDelete( assignmentId, deactivatedByUserId, timestamp );

                if (updatedRows > 0) {
                    // Get updated assignment
                    UserTeamAssignmentEntity entity = mAssignmentDao.getById( assignmentId );
                    if (entity != null) {
                        UserTeamAssignment deactivatedAssignment = entity.toDomainModel();
                        Log.d( TAG, "✅ Assignment deactivated successfully: " + assignmentId );
                        return OperationResult.success( deactivatedAssignment ,
                                OperationResult.OperationType.DEACTIVATE );
                    } else {
                        return OperationResult.failure( "Assignment not found after deactivation: " + assignmentId ,
                                OperationResult.OperationType.DEACTIVATE );
                    }
                } else {
                    Log.w( TAG, "Assignment not found for deactivation: " + assignmentId );
                    return OperationResult.failure( "Assignment not found: " + assignmentId ,
                            OperationResult.OperationType.DEACTIVATE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error deactivating assignment: " + assignmentId, e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.DEACTIVATE );
            }
        }, mExecutorService );
    }

    // ==================== RETRIEVAL OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAllAssignments(boolean activeOnly) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting all assignments (activeOnly: " + activeOnly + ")" );

                List<UserTeamAssignmentEntity> entities = activeOnly ?
                        mAssignmentDao.getAllActive() :
                        mAssignmentDao.getAll();

                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments" );
                return OperationResult.success( assignments,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting all assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> getAssignmentCount(boolean activeOnly) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = activeOnly ?
                        mAssignmentDao.getActiveCount() :
                        mAssignmentDao.getCount();

                Log.d( TAG, "✅ Assignment count (activeOnly: " + activeOnly + "): " + count );
                return OperationResult.success( count,
                        OperationResult.OperationType.COUNT );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignment count", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.COUNT );
            }
        }, mExecutorService );
    }

    // ==================== USER-BASED QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByUserId(@NonNull String userId,
                                                                                               boolean activeOnly) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments for user: " + userId + " (activeOnly: " + activeOnly + ")" );

                List<UserTeamAssignmentEntity> entities = activeOnly ?
                        mAssignmentDao.getActiveByUserId( userId ) :
                        mAssignmentDao.getByUserId( userId );

                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments for user: " + userId );
                return OperationResult.success( assignments,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments for user: " + userId, e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getCurrentUserAssignments(@NonNull String userId,
                                                                                                  @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting current assignments for user: " + userId + " on date: " + date );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getCurrentAssignmentsByUserId( userId, date );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " current assignments for user: " + userId );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting current assignments for user: " + userId, e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getUserAssignmentsInDateRange(@NonNull String userId,
                                                                                                      @NonNull LocalDate startDate,
                                                                                                      @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments for user: " + userId + " in range: " + startDate + " to " + endDate );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getByUserIdAndDateRange( userId, startDate, endDate );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments for user in date range" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting user assignments in date range", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> isUserAssignedOnDate(@NonNull String userId,
                                                                            @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getCurrentAssignmentsByUserId( userId, date );
                boolean isAssigned = !entities.isEmpty();

                Log.d( TAG, "✅ User " + userId + " assignment check on " + date + ": " + isAssigned );
                return OperationResult.success( isAssigned,
                        OperationResult.OperationType.SEARCH );
            } catch (Exception e) {
                Log.e( TAG, "Error checking user assignment on date", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.SEARCH );
            }
        }, mExecutorService );
    }

    // ==================== TEAM-BASED QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByTeamId(@NonNull String teamId,
                                                                                               boolean activeOnly) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments for team: " + teamId + " (activeOnly: " + activeOnly + ")" );

                List<UserTeamAssignmentEntity> entities = activeOnly ?
                        mAssignmentDao.getActiveByTeamId( teamId ) :
                        mAssignmentDao.getByTeamId( teamId );

                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments for team: " + teamId );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments for team: " + teamId, e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getCurrentTeamMembers(@NonNull String teamId,
                                                                                              @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting current team members for team: " + teamId + " on date: " + date );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getCurrentTeamMembers( teamId, date );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " current members for team: " + teamId );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting current team members: " + teamId, e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getTeamAssignmentsInDateRange(@NonNull String teamId,
                                                                                                      @NonNull LocalDate startDate,
                                                                                                      @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments for team: " + teamId + " in range: " + startDate + " to " + endDate );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getByTeamIdAndDateRange( teamId, startDate, endDate );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments for team in date range" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting team assignments in date range", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> countTeamMembersOnDate(@NonNull String teamId,
                                                                              @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = mAssignmentDao.countTeamMembersOnDate( teamId, date );
                Log.d( TAG, "✅ Team " + teamId + " member count on " + date + ": " + count );
                return OperationResult.success( count ,
                        OperationResult.OperationType.COUNT );
            } catch (Exception e) {
                Log.e( TAG, "Error counting team members on date", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.COUNT );
            }
        }, mExecutorService );
    }

    // ==================== USER-TEAM RELATIONSHIP QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<UserTeamAssignment>> getUserTeamAssignment(@NonNull String userId,
                                                                                        @NonNull String teamId,
                                                                                        @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignment for user: " + userId + ", team: " + teamId + ", date: " + date );

                UserTeamAssignmentEntity entity = mAssignmentDao.getAssignment( userId, teamId, date );
                if (entity != null) {
                    UserTeamAssignment assignment = entity.toDomainModel();
                    Log.d( TAG, "✅ Found assignment: " + assignment.getId() );
                    return OperationResult.success( assignment,
                            OperationResult.OperationType.READ );
                } else {
                    Log.d( TAG, "No assignment found for user-team-date combination" );
                    return OperationResult.failure( "No assignment found",
                            OperationResult.OperationType.READ );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error getting user-team assignment", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentHistory(@NonNull String userId,
                                                                                             @NonNull String teamId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignment history for user: " + userId + ", team: " + teamId );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getAssignmentHistory( userId, teamId );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments in history" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignment history", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<AssignmentValidationResult>> validateAssignment(@NonNull String userId,
                                                                                             @NonNull String teamId,
                                                                                             @NonNull LocalDate startDate,
                                                                                             @Nullable LocalDate endDate,
                                                                                             @Nullable String excludeAssignmentId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                OperationResult<AssignmentValidationResult> result = performAssignmentValidation(
                        userId, teamId, startDate, endDate, excludeAssignmentId );
                return result;
            } catch (Exception e) {
                Log.e( TAG, "Error validating assignment", e );
                return OperationResult.failure( "Validation error: " + e.getMessage() ,
                        OperationResult.OperationType.VALIDATION );
            }
        }, mExecutorService );
    }

    // ==================== STATUS AND DATE-BASED QUERIES ====================

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsByStatus(@NonNull Status status,
                                                                                               boolean activeOnly) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments by status: " + status + " (activeOnly: " + activeOnly + ")" );

                List<UserTeamAssignmentEntity> entities = activeOnly ?
                        mAssignmentDao.getByStatusAndActive( status, true ) :
                        mAssignmentDao.getByStatus( status );

                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments with status: " + status );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments by status", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsInDateRange(@NonNull LocalDate startDate,
                                                                                                  @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting assignments in date range: " + startDate + " to " + endDate );

                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getAssignmentsInDateRange( startDate, endDate );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments in date range" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments in date range", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsStartingOn(@NonNull LocalDate startDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getAssignmentsStartingOn( startDate );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments starting on " + startDate );
                return OperationResult.success( assignments,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments starting on date", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsEndingOn(@NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getAssignmentsEndingOn( endDate );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments ending on " + endDate );
                return OperationResult.success( assignments,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments ending on date", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getPermanentAssignments() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getPermanentAssignments();
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " permanent assignments" );
                return OperationResult.success( assignments,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting permanent assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== MANAGEMENT OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getAssignmentsCreatedBy(@NonNull String createdByUserId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getAssignmentsCreatedBy( createdByUserId );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments created by " + createdByUserId );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting assignments created by user", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<List<UserTeamAssignment>>> getModifiedAssignments(long fromTimestamp,
                                                                                               long toTimestamp) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<UserTeamAssignmentEntity> entities = mAssignmentDao.getModifiedBetween( fromTimestamp, toTimestamp );
                List<UserTeamAssignment> assignments = entities.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "✅ Found " + assignments.size() + " assignments modified between timestamps" );
                return OperationResult.success( assignments ,
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting modified assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deactivateUserAssignments(@NonNull String userId,
                                                                                 @Nullable String deactivatedByUserId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deactivating all assignments for user: " + userId );

                long timestamp = System.currentTimeMillis();
                int deactivatedCount = mAssignmentDao.deactivateAssignmentsForUser( userId, timestamp, deactivatedByUserId );

                Log.d( TAG, "✅ Deactivated " + deactivatedCount + " assignments for user: " + userId );
                return OperationResult.success( deactivatedCount ,
                        OperationResult.OperationType.DEACTIVATE );
            } catch (Exception e) {
                Log.e( TAG, "Error deactivating user assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.DEACTIVATE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deactivateTeamAssignments(@NonNull String teamId,
                                                                                 @Nullable String deactivatedByUserId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deactivating all assignments for team: " + teamId );

                long timestamp = System.currentTimeMillis();
                int deactivatedCount = mAssignmentDao.deactivateAssignmentsForTeam( teamId, timestamp, deactivatedByUserId );

                Log.d( TAG, "✅ Deactivated " + deactivatedCount + " assignments for team: " + teamId );
                return OperationResult.success( deactivatedCount ,
                        OperationResult.OperationType.DEACTIVATE );
            } catch (Exception e) {
                Log.e( TAG, "Error deactivating team assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage(),
                        OperationResult.OperationType.DEACTIVATE );
            }
        }, mExecutorService );
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> updateAssignmentStatus(@NonNull List<String> assignmentIds,
                                                                              @NonNull Status status,
                                                                              @Nullable String modifiedByUserId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating status for " + assignmentIds.size() + " assignments to: " + status );

                long timestamp = System.currentTimeMillis();
                int updatedCount = mAssignmentDao.updateStatusForAssignments( assignmentIds, status, timestamp, modifiedByUserId );

                Log.d( TAG, "✅ Updated status for " + updatedCount + " assignments" );
                return OperationResult.success( updatedCount,
                        OperationResult.OperationType.UPDATE );
            } catch (Exception e) {
                Log.e( TAG, "Error updating assignment status", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    // ==================== CLEANUP OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> cleanupInactiveAssignments(long beforeTimestamp) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Cleaning up inactive assignments before timestamp: " + beforeTimestamp );

                int deletedCount = mAssignmentDao.deleteInactiveAssignmentsBefore( beforeTimestamp );

                Log.d( TAG, "✅ Cleaned up " + deletedCount + " inactive assignments" );
                return OperationResult.success( deletedCount ,
                        OperationResult.OperationType.CLEANUP );
            } catch (Exception e) {
                Log.e( TAG, "Error cleaning up inactive assignments", e );
                return OperationResult.failure( "Database error: " + e.getMessage() ,
                        OperationResult.OperationType.CLEANUP );
            }
        }, mExecutorService );
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Perform assignment validation logic.
     * Checks for conflicts, date validity, and business rules.
     */
    private OperationResult<AssignmentValidationResult> performAssignmentValidation(@NonNull String userId,
                                                                                    @NonNull String teamId,
                                                                                    @NonNull LocalDate startDate,
                                                                                    @Nullable LocalDate endDate,
                                                                                    @Nullable String excludeAssignmentId) {
        try {
            // Validate date range
            if (endDate != null && endDate.isBefore( startDate )) {
                return OperationResult.success( new AssignmentValidationResult(
                                false,
                                "End date cannot be before start date",
                                null ),
                        OperationResult.OperationType.VALIDATION );
            }

            // Check for overlapping assignments for the user
            LocalDate checkEndDate = endDate != null ? endDate : startDate.plusYears( 100 ); // Far future for permanent assignments
            List<UserTeamAssignmentEntity> userAssignments = mAssignmentDao.getByUserIdAndDateRange( userId, startDate, checkEndDate );

            // Filter out the assignment being updated (if any)
            if (excludeAssignmentId != null) {
                userAssignments = userAssignments.stream()
                        .filter( assignment -> !excludeAssignmentId.equals( assignment.getId() ) )
                        .collect( Collectors.toList() );
            }

            if (!userAssignments.isEmpty()) {
                List<UserTeamAssignment> conflictingAssignments = userAssignments.stream()
                        .map( UserTeamAssignmentEntity::toDomainModel )
                        .collect( Collectors.toList() );

                return OperationResult.success( new AssignmentValidationResult(
                                false,
                                "User " + userId + " already has " + conflictingAssignments.size() +
                                        " conflicting assignment(s) in the specified date range",
                                conflictingAssignments ),
                        OperationResult.OperationType.VALIDATION
                );
            }

            // Validation passed
            return OperationResult.success( new AssignmentValidationResult(
                            true,
                            "Assignment validation passed",
                            null ),
                    OperationResult.OperationType.VALIDATION );
        } catch (Exception e) {
            Log.e( TAG, "Error during assignment validation", e );
            return OperationResult.failure( "Validation error: " + e.getMessage(),
                    OperationResult.OperationType.VALIDATION );
        }
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Shutdown the repository and clean up resources.
     * Should be called when the repository is no longer needed.
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d( TAG, "UserTeamAssignmentRepositoryImpl shutdown completed" );
        }
    }
}