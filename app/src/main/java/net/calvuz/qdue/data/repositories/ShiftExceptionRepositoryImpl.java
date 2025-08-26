package net.calvuz.qdue.data.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.ShiftExceptionDao;
import net.calvuz.qdue.data.entities.ShiftExceptionEntity;
import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.repositories.ShiftExceptionRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ShiftExceptionRepositoryImpl - Clean Architecture bridge for shift exception operations.
 *
 * <p>Provides asynchronous shift exception management functionality following clean architecture
 * principles with consistent error handling using OperationResult pattern.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Entity Conversion</strong>: Uses entity's built-in conversion methods</li>
 *   <li><strong>OperationResult Pattern</strong>: Consistent error handling</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture for all operations</li>
 *   <li><strong>Proper Resource Management</strong>: Closeable ExecutorService</li>
 *   <li><strong>Comprehensive Logging</strong>: Structured error logging</li>
 * </ul>
 *
 * <h3>Business Operations:</h3>
 * <ul>
 *   <li>Exception CRUD operations</li>
 *   <li>Date-based queries for schedule generation</li>
 *   <li>User-specific exception management</li>
 *   <li>Approval workflow operations</li>
 *   <li>Type-based exception filtering</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.1.0 - Clean Architecture Implementation
 * @since Clean Architecture Phase 2
 */
public class ShiftExceptionRepositoryImpl implements ShiftExceptionRepository {

    private static final String TAG = "ShiftExceptionRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final ShiftExceptionDao mShiftExceptionDao;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param database Calendar database instance
     */
    public ShiftExceptionRepositoryImpl(@NonNull CalendarDatabase database) {
        this.mShiftExceptionDao = database.shiftExceptionDao();
        this.mExecutorService = Executors.newFixedThreadPool(
                2,
                r -> {
                    Thread thread = new Thread( r, "ShiftException-DB-Thread" );
                    thread.setDaemon( true ); // Avoid blocking JVM shutdown
                    return thread;
                }
        );
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ShiftException>> getShiftExceptionById(@NonNull String exceptionId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                ShiftExceptionEntity entity = mShiftExceptionDao.getShiftExceptionById( exceptionId );

                if (entity != null) {
                    ShiftException domain = entity.toDomainModel(); // ✅ Use entity's conversion method
                    Log.d( TAG, "Successfully retrieved shift exception: " + exceptionId );
                    return OperationResult.success( domain, OperationResult.OperationType.READ );
                } else {
                    String message = "Shift exception not found: " + exceptionId;
                    Log.w( TAG, message );
                    return OperationResult.failure( message, OperationResult.OperationType.READ );
                }
            } catch (Exception e) {
                String error = "Error getting shift exception by ID: " + exceptionId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ShiftException>> saveShiftException(@NonNull ShiftException shiftException) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // ✅ Use entity's conversion method
                ShiftExceptionEntity entity = ShiftExceptionEntity.fromDomainModel( shiftException );
                entity.updateTimestamp(); // Update timestamp before saving

                long result = mShiftExceptionDao.insertShiftException( entity );

                if (result > 0) {
                    Log.d( TAG, "Successfully saved shift exception: " + shiftException.getId() );
                    return OperationResult.success( shiftException, OperationResult.OperationType.CREATE );
                } else {
                    String error = "Failed to save shift exception: " + shiftException.getId();
                    Log.e( TAG, error );
                    return OperationResult.failure( error, OperationResult.OperationType.CREATE );
                }
            } catch (Exception e) {
                String error = "Error saving shift exception: " + shiftException.getId();
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.CREATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> deleteShiftException(@NonNull String exceptionId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int deletedRows = mShiftExceptionDao.deleteShiftExceptionById( exceptionId );
                boolean success = deletedRows > 0;

                if (success) {
                    Log.d( TAG, "Successfully deleted shift exception: " + exceptionId );
                    return OperationResult.success( true, OperationResult.OperationType.DELETE );
                } else {
                    String message = "Shift exception not found for deletion: " + exceptionId;
                    Log.w( TAG, message );
                    return OperationResult.failure( message, OperationResult.OperationType.DELETE );
                }
            } catch (Exception e) {
                String error = "Error deleting shift exception: " + exceptionId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.DELETE );
            }
        }, mExecutorService );
    }

    // ==================== QUERY OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getEffectiveExceptionsForUserOnDate(
            @NonNull Long userId, @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getEffectiveExceptionsForUserAndDate( userId, date.toString() );

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.v( TAG, "Retrieved " + domain.size() + " effective exceptions for user " + userId );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting effective exceptions for user " + userId + " on date " + date;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsForUserInDateRange(
            @NonNull Long userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getShiftExceptionsForUserInDateRange(
                                userId, startDate.toString(), endDate.toString() );

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.v( TAG, MessageFormat.format( "Retrieved {0} exceptions for user {1} in date range ({2}-{3})",
                        domain.size(), userId, startDate.toString(), endDate.toString() ) );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = MessageFormat.format( "Error getting exceptions for user {0} in date range ({1}-{2})",
                        userId, startDate.toString(), endDate.toString() );
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getEffectiveExceptionsForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting effective exceptions for date: " + date );

                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getEffectiveExceptionsForDate( date.toString() );

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.d( TAG, "Retrieved " + domain.size() + " effective exceptions for date " + date );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting effective exceptions for date: " + date;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsInDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting exceptions in date range: " + startDate + " to " + endDate );

                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getShiftExceptionsInDateRange(
                                startDate.toString(), endDate.toString() );

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.d( TAG, "Retrieved " + domain.size() + " exceptions in date range" );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting exceptions in date range: " + startDate + " to " + endDate;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsByTypeForUser(
            @NonNull Long userId, @NonNull ShiftException.ExceptionType exceptionType) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting exceptions by type " + exceptionType + " for user " + userId );

                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getShiftExceptionsByTypePrefix( exceptionType.name() );

                // Filter by user ID
                List<ShiftExceptionEntity> userEntities = entities.stream()
                        .filter( entity -> entity.getUserId().equals( userId ) )
                        .collect( Collectors.toList() );

                List<ShiftException> domain = convertEntitiesToDomain( userEntities );

                Log.d( TAG, "Retrieved " + domain.size() + " exceptions of type " + exceptionType + " for user " + userId );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting exceptions by type " + exceptionType + " for user " + userId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getPendingApprovalsOrderedByPriority() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting pending approvals ordered by priority" );

                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getPendingApprovalsOrderedByDateAndPriority();

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.d( TAG, "Retrieved " + domain.size() + " pending approvals" );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting pending approvals";
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getExceptionsByApprovalStatus(
            @NonNull ShiftException.ApprovalStatus status) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting exceptions by approval status: " + status );

                List<ShiftExceptionEntity> entities =
                        mShiftExceptionDao.getShiftExceptionsByApprovalStatus( status.name() );

                List<ShiftException> domain = convertEntitiesToDomain( entities );

                Log.d( TAG, "Retrieved " + domain.size() + " exceptions with status " + status );
                return OperationResult.success( domain, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting exceptions by approval status: " + status;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== UPDATE OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> updateExceptionStatus(
            @NonNull String exceptionId, @NonNull ShiftException.ApprovalStatus newStatus) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating exception status for " + exceptionId + " to " + newStatus );

                int updatedRows = mShiftExceptionDao.updateExceptionStatus(
                        exceptionId, newStatus.name(), System.currentTimeMillis() );

                boolean success = updatedRows > 0;

                if (success) {
                    Log.d( TAG, "Successfully updated exception status: " + exceptionId );
                    return OperationResult.success( true, OperationResult.OperationType.UPDATE );
                } else {
                    String message = "Exception not found for status update: " + exceptionId;
                    Log.w( TAG, message );
                    return OperationResult.failure( message, OperationResult.OperationType.UPDATE );
                }
            } catch (Exception e) {
                String error = "Error updating exception status for " + exceptionId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> approveException(
            @NonNull String exceptionId, @NonNull Long approverId,
            @NonNull String approverName, @NonNull LocalDate approvedDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Approving exception " + exceptionId + " by user " + approverId );

                int updatedRows = mShiftExceptionDao.approveException(
                        exceptionId, approverId, approverName,
                        approvedDate.toString(), System.currentTimeMillis() );

                boolean success = updatedRows > 0;

                if (success) {
                    Log.d( TAG, "Successfully approved exception: " + exceptionId );
                    return OperationResult.success( true, OperationResult.OperationType.UPDATE );
                } else {
                    String message = "Exception not found for approval: " + exceptionId;
                    Log.w( TAG, message );
                    return OperationResult.failure( message, OperationResult.OperationType.UPDATE );
                }
            } catch (Exception e) {
                String error = "Error approving exception " + exceptionId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> rejectException(
            @NonNull String exceptionId, @NonNull String rejectionReason) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Rejecting exception " + exceptionId + " with reason: " + rejectionReason );

                int updatedRows = mShiftExceptionDao.rejectException(
                        exceptionId, rejectionReason, System.currentTimeMillis() );

                boolean success = updatedRows > 0;

                if (success) {
                    Log.d( TAG, "Successfully rejected exception: " + exceptionId );
                    return OperationResult.success( true, OperationResult.OperationType.UPDATE );
                } else {
                    String message = "Exception not found for rejection: " + exceptionId;
                    Log.w( TAG, message );
                    return OperationResult.failure( message, OperationResult.OperationType.UPDATE );
                }
            } catch (Exception e) {
                String error = "Error rejecting exception " + exceptionId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    // ==================== STATISTICS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getExceptionCountForUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting exception count for user: " + userId );

                int count = mShiftExceptionDao.getExceptionCountForUser( userId );

                Log.d( TAG, "User " + userId + " has " + count + " exceptions" );
                return OperationResult.success( count, OperationResult.OperationType.READ );
            } catch (Exception e) {
                String error = "Error getting exception count for user " + userId;
                Log.e( TAG, error, e );
                return OperationResult.failure( error, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Convert list of entities to domain models.
     * ✅ Uses entity's built-in conversion method
     */
    @NonNull
    private List<ShiftException> convertEntitiesToDomain(@NonNull List<ShiftExceptionEntity> entities) {
        return entities.stream()
                .map( ShiftExceptionEntity::toDomainModel )
                .collect( Collectors.toList() );
    }

    // ==================== RESOURCE MANAGEMENT ====================

    /**
     * Shutdown the executor service.
     * Should be called when the repository is no longer needed.
     */
    public void shutdown() {
        Log.d( TAG, "Shutting down ShiftExceptionRepository executor service" );
        mExecutorService.shutdown();
    }
}