package net.calvuz.qdue.data.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.UserScheduleAssignmentDao;
import net.calvuz.qdue.data.entities.UserScheduleAssignmentEntity;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.common.enums.Status;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * UserScheduleAssignmentRepositoryImpl - Clean Architecture bridge for user assignment operations.
 *
 * <p>Provides asynchronous user schedule assignment management functionality following clean
 * architecture principles with consistent error handling using OperationResult pattern.</p>
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
 *   <li>User-team assignment CRUD operations</li>
 *   <li>Date-based assignment queries for schedule generation</li>
 *   <li>Team roster and membership management</li>
 *   <li>Priority-based conflict resolution</li>
 *   <li>Assignment lifecycle management</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.1.0 - Clean Architecture Implementation
 * @since Clean Architecture Phase 2
 */
public class UserScheduleAssignmentRepositoryImpl implements UserScheduleAssignmentRepository {

    private static final String TAG = "UserScheduleAssignmentRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final UserScheduleAssignmentDao mUserScheduleAssignmentDao;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param database Calendar database instance
     */
    public UserScheduleAssignmentRepositoryImpl(@NonNull CalendarDatabase database) {
        this.mUserScheduleAssignmentDao = database.userScheduleAssignmentDao();
        this.mExecutorService = Executors.newFixedThreadPool(
                2,
                r -> {
                    Thread thread = new Thread(r, "UserAssignment-DB-Thread");
                    thread.setDaemon(true); // Avoid blocking JVM shutdown
                    return thread;
                }
        );
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> getUserScheduleAssignmentById(@NonNull String assignmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting user schedule assignment by ID: " + assignmentId);

                UserScheduleAssignmentEntity entity = mUserScheduleAssignmentDao.getUserScheduleAssignmentById(assignmentId);

                if (entity != null) {
                    UserScheduleAssignment domain = entity.toDomainModel(); // ✅ Use entity's conversion method
                    Log.d(TAG, "Successfully retrieved user assignment: " + assignmentId);
                    return OperationResult.success(domain, OperationResult.OperationType.READ);
                } else {
                    String message = "User schedule assignment not found: " + assignmentId;
                    Log.w(TAG, message);
                    return OperationResult.failure(message, OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                String error = "Error getting user schedule assignment by ID: " + assignmentId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> insertUserScheduleAssignment(@NonNull UserScheduleAssignment assignment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Saving user schedule assignment: " + assignment.getId());

                // ✅ Use entity's conversion method
                UserScheduleAssignmentEntity entity = UserScheduleAssignmentEntity.fromDomainModel(assignment);
                entity.updateTimestamp(); // Update timestamp before saving

                long result = mUserScheduleAssignmentDao.insertUserScheduleAssignment(entity);

                if (result > 0) {
                    Log.d(TAG, "Successfully saved user assignment: " + assignment.getId());
                    return OperationResult.success(assignment, OperationResult.OperationType.CREATE);
                } else {
                    String error = "Failed to save user schedule assignment: " + assignment.getId();
                    Log.e(TAG, error);
                    return OperationResult.failure(error, OperationResult.OperationType.CREATE);
                }

            } catch (Exception e) {
                String error = "Error saving user schedule assignment: " + assignment.getId();
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> deleteUserScheduleAssignment(@NonNull String assignmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting user schedule assignment: " + assignmentId);

                int deletedRows = mUserScheduleAssignmentDao.deleteUserScheduleAssignmentById(assignmentId);
                boolean success = deletedRows > 0;

                if (success) {
                    Log.d(TAG, "Successfully deleted user assignment: " + assignmentId);
                    return OperationResult.success(true, OperationResult.OperationType.DELETE);
                } else {
                    String message = "User assignment not found for deletion: " + assignmentId;
                    Log.w(TAG, message);
                    return OperationResult.failure(message, OperationResult.OperationType.DELETE);
                }

            } catch (Exception e) {
                String error = "Error deleting user schedule assignment: " + assignmentId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> updateUserScheduleAssignment(@NonNull UserScheduleAssignment assignment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d( TAG, "Updating user schedule assignment: " + assignment.getId() );

                // ✅ Use entity's conversion method
                UserScheduleAssignmentEntity entity = UserScheduleAssignmentEntity.fromDomainModel( assignment );
                entity.updateTimestamp(); // Update timestamp before saving

                long result = mUserScheduleAssignmentDao.updateUserScheduleAssignment(entity);
                if (result > 0) {
                    Log.d(TAG, "Successfully updated user assignment: " + assignment.getId());
                    return OperationResult.success(assignment, OperationResult.OperationType.UPDATE);
                } else {
                    String error = "Failed to update user schedule assignment: " + assignment.getId();
                    Log.e(TAG, error);
                    return OperationResult.failure(error, OperationResult.OperationType.UPDATE);
                }

            } catch (Exception e) {
                String error = "Error updating user schedule assignment: " + assignment.getId();
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
            }

    // ==================== USER-SPECIFIC QUERIES ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getUserActiveAssignments(@NonNull String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting active assignments for user: " + userId);

                // Get assignments that are not expired or cancelled
                List<UserScheduleAssignmentEntity> entities = mUserScheduleAssignmentDao.getUserActiveAssignments(userId);
                List<UserScheduleAssignment> activeAssignments = new ArrayList<>();

                for (UserScheduleAssignmentEntity entity : entities) {
                    UserScheduleAssignment domainAssignment = entity.toDomainModel(); //convertToDomainModel(entity);
                    if (domainAssignment.isActive()) {
                        // Include ACTIVE and PENDING assignments (not EXPIRED or CANCELLED)
                        Status status = domainAssignment.getStatus();
                        if (status == Status.ACTIVE ||
                                status == Status.PENDING) {
                            activeAssignments.add(domainAssignment);
                        }
                    }
                }

                Log.d(TAG, "Found " + activeAssignments.size() + " active assignments for user");
                return OperationResult.success(activeAssignments, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting active assignments for user: " + userId, e);
                return OperationResult.failure("Failed to load active assignments: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> getActiveAssignmentForUser(
            @NonNull String userId, @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserScheduleAssignmentEntity entity =
                        mUserScheduleAssignmentDao.getActiveAssignmentForUserOnDate(userId, date.toString());

                if (entity != null) {
                    // User has an assignment on this date
                    UserScheduleAssignment domain = entity.toDomainModel();
                    Log.v(TAG, "Found active assignment for user " + userId + ": " + entity.getId());
                    return OperationResult.success(domain,
                            OperationResult.OperationType.READ);
                } else {
                    // User has no active assignment on this date (free day?)
                    Log.v(TAG, "No active assignment found for user " + userId + " on date " + date); // Debug level since this is a common case
                    return OperationResult.success("No active assignment found",
                            OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting active assignment for user " + userId + " on date " + date, e);
                return OperationResult.failure("Error getting active assignment for user",
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsForUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAssignmentsByUserAndStatus(userId, "ACTIVE");

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(entities);

                Log.v(TAG, "Retrieved " + domain.size() + " active assignments for user " + userId);
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting active assignments for user " + userId, e);
                return OperationResult.failure("Error getting active assignments for user",
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsForUserInDateRange(
            @NonNull String userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAssignmentsForUserInDateRange(
                                userId, startDate.toString(), endDate.toString());

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(entities);

                Log.v(TAG, "Retrieved " + domain.size() + " assignments for user " + userId + " in date range");
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting assignments for user " + userId + " in date range", e);
                return OperationResult.failure("Error getting assignments for user in date range",
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== DATE-BASED QUERIES ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting active assignments for date: " + date);

                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAllActiveAssignmentsOnDate(date.toString());

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(entities);

                Log.d(TAG, "Retrieved " + domain.size() + " active assignments for date " + date);
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting active assignments for date: " + date;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getActiveAssignmentsInDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting active assignments in date range: " + startDate + " to " + endDate);

                // Since there's no direct method in DAO, we need to iterate through dates
                // or use a different approach. For now, let's use a simple query that gets
                // all active assignments and filter them by date range logic
                List<UserScheduleAssignmentEntity> allActiveEntities =
                        mUserScheduleAssignmentDao.getAllActiveAssignmentsOnDate(startDate.toString());

                // Filter entities that are active within the date range
                List<UserScheduleAssignmentEntity> filteredEntities = allActiveEntities.stream()
                        .filter(entity -> {
                            LocalDate entityStart = LocalDate.parse(entity.getStartDate());
                            LocalDate entityEnd = entity.getEndDate() != null ?
                                    LocalDate.parse(entity.getEndDate()) : LocalDate.MAX;

                            // Check if entity date range overlaps with requested range
                            return !entityStart.isAfter(endDate) && !entityEnd.isBefore(startDate);
                        })
                        .collect(Collectors.toList());

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(filteredEntities);

                Log.d(TAG, "Retrieved " + domain.size() + " active assignments in date range");
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting active assignments in date range: " + startDate + " to " + endDate;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== TEAM-BASED QUERIES ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsByTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting assignments by team: " + teamId);

                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAssignmentsByTeamId(teamId);

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(entities);

                Log.d(TAG, "Retrieved " + domain.size() + " assignments for team " + teamId);
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting assignments by team: " + teamId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getAssignmentsByRecurrenceRule(
            @NonNull String recurrenceRuleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting assignments by recurrence rule: " + recurrenceRuleId);

                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAssignmentsByRecurrenceRule(recurrenceRuleId);

                List<UserScheduleAssignment> domain = convertEntitiesToDomain(entities);

                Log.d(TAG, "Retrieved " + domain.size() + " assignments for recurrence rule " + recurrenceRuleId);
                return OperationResult.success(domain, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting assignments by recurrence rule: " + recurrenceRuleId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<String>>> getAllActiveUserIds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting all active user IDs");

                // Since there's no direct method in DAO, we get all active assignments 
                // for today and extract unique user IDs
                String today = LocalDate.now().toString();
                List<UserScheduleAssignmentEntity> entities =
                        mUserScheduleAssignmentDao.getAllActiveAssignmentsOnDate(today);

                List<String> userIds = entities.stream()
                        .map(UserScheduleAssignmentEntity::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + userIds.size() + " active user IDs");
                return OperationResult.success(userIds, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting all active user IDs";
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== UPDATE OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> updateAssignmentStatus(
            @NonNull String assignmentId, @NonNull Status newStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Updating assignment status for " + assignmentId + " to " + newStatus);

                int updatedRows = mUserScheduleAssignmentDao.updateAssignmentStatus(
                        assignmentId, newStatus.name(), System.currentTimeMillis());

                boolean success = updatedRows > 0;

                if (success) {
                    Log.d(TAG, "Successfully updated assignment status: " + assignmentId);
                    return OperationResult.success(true, OperationResult.OperationType.UPDATE);
                } else {
                    String message = "Assignment not found for status update: " + assignmentId;
                    Log.w(TAG, message);
                    return OperationResult.failure(message, OperationResult.OperationType.UPDATE);
                }

            } catch (Exception e) {
                String error = "Error updating assignment status for " + assignmentId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> endAssignment(@NonNull String assignmentId, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Ending assignment " + assignmentId + " on date " + endDate);

                int updatedRows = mUserScheduleAssignmentDao.setAssignmentEndDate(
                        assignmentId, endDate.toString(), System.currentTimeMillis());

                boolean success = updatedRows > 0;

                if (success) {
                    Log.d(TAG, "Successfully ended assignment: " + assignmentId);
                    return OperationResult.success(true, OperationResult.OperationType.UPDATE);
                } else {
                    String message = "Assignment not found for ending: " + assignmentId;
                    Log.w(TAG, message);
                    return OperationResult.failure(message, OperationResult.OperationType.UPDATE);
                }

            } catch (Exception e) {
                String error = "Error ending assignment " + assignmentId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION AND CHECKS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> hasActiveAssignments(@NonNull String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Checking active assignments for user: " + userId);

                int count = mUserScheduleAssignmentDao.getAssignmentCountForUser(userId);
                boolean hasAssignments = count > 0;

                Log.d(TAG, "User " + userId + " has " + count + " assignments");
                return OperationResult.success(hasAssignments, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error checking active assignments for user " + userId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> teamHasActiveAssignments(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Checking active assignments for team: " + teamId);

                int count = mUserScheduleAssignmentDao.getAssignmentCountForTeam(teamId);
                boolean hasAssignments = count > 0;

                Log.d(TAG, "Team " + teamId + " has " + count + " assignments");
                return OperationResult.success(hasAssignments, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error checking active assignments for team " + teamId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS AND REPORTING ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getAssignmentCountForTeam(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting assignment count for team: " + teamId);

                int count = mUserScheduleAssignmentDao.getAssignmentCountForTeam(teamId);

                Log.d(TAG, "Team " + teamId + " has " + count + " total assignments");
                return OperationResult.success(count, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error getting assignment count for team " + teamId;
                Log.e(TAG, error, e);
                return OperationResult.failure(error,OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Convert list of entities to domain models.
     * ✅ Uses entity's built-in conversion method
     */
    @NonNull
    private List<UserScheduleAssignment> convertEntitiesToDomain(@NonNull List<UserScheduleAssignmentEntity> entities) {
        return entities.stream()
                .map(UserScheduleAssignmentEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    // ==================== RESOURCE MANAGEMENT ====================

    /**
     * Shutdown the executor service.
     * Should be called when the repository is no longer needed.
     */
    public void shutdown() {
        Log.d(TAG, "Shutting down UserScheduleAssignmentRepository executor service");
        mExecutorService.shutdown();
    }
}