package net.calvuz.qdue.domain.calendar.usecases;

import android.util.Log;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GenerateUserScheduleUseCase - Individual User Schedule Generation
 *
 * <p>Comprehensive use case for generating individual user work schedules with
 * advanced recurrence rules, exception handling, team assignments, and business
 * rule validation. Integrates with clean architecture principles.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>User-Centric</strong>: Personalized schedule generation</li>
 *   <li><strong>Business Rules</strong>: User-specific validation and constraints</li>
 *   <li><strong>Performance Optimized</strong>: Efficient date range processing</li>
 *   <li><strong>Error Resilient</strong>: Comprehensive validation and error handling</li>
 * </ul>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Single Responsibility</strong>: User schedule generation only</li>
 *   <li><strong>Repository Pattern</strong>: Uses WorkScheduleRepository interface</li>
 *   <li><strong>Domain Logic</strong>: Business rules encapsulated in use case</li>
 *   <li><strong>Async Operations</strong>: All methods return CompletableFuture</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation
 * @since Clean Architecture Migration
 */
public class GenerateUserScheduleUseCase {

    private static final String TAG = "GenerateUserScheduleUseCase";

    // Dependencies
    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param workScheduleRepository Repository for work schedule operations
     */
    public GenerateUserScheduleUseCase(@NonNull WorkScheduleRepository workScheduleRepository) {
        this.mWorkScheduleRepository = workScheduleRepository;
    }

    // ==================== CORE OPERATIONS ====================

    /**
     * Execute use case for specific date.
     *
     * @param userId User ID for schedule generation
     * @param date Target date
     * @return CompletableFuture with user's WorkScheduleDay
     */
    @NonNull
    public CompletableFuture<OperationResult<WorkScheduleDay>> executeForDate(
            @NonNull Long userId, @NonNull LocalDate date) {

        Log.d(TAG,"Executing user schedule generation for userId: " + userId + ", date: " + date);

        return mWorkScheduleRepository.getWorkScheduleForDate(date, userId)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        WorkScheduleDay schedule = result.getData();

                        // Apply user-specific business rules
                        if (schedule != null) {
                            schedule = applyUserSpecificBusinessRules(schedule, userId);
                        }

                        Log.d(TAG,"Generated user schedule for " + userId + " on " + date);
                        return OperationResult.success(schedule, OperationResult.OperationType.READ);
                    } else {
                        Log.e(TAG,"Failed to get user schedule: " + result.getErrorMessage(), null);
                        return result;
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG,"Exception in user schedule generation: " + throwable.getMessage(), null);
                    return OperationResult.failure("Failed to generate user schedule: " + throwable.getMessage(), OperationResult.OperationType.READ);
                });
    }

    /**
     * Execute use case for date range.
     *
     * @param userId User ID for schedule generation
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with Map of dates to WorkScheduleDay
     */
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> executeForDateRange(
            @NonNull Long userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {

        // Validate date range
        if (startDate.isAfter(endDate)) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Start date cannot be after end date",
                            OperationResult.OperationType.VALIDATION));
        }

        // Check for reasonable date range (business rule)
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysDifference > 365) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Date range cannot exceed 365 days",
                            OperationResult.OperationType.VALIDATION));
        }

        Log.d(TAG,"Executing user schedule range generation for userId: " + userId +
                ", range: " + startDate + " to " + endDate);

        return mWorkScheduleRepository.getWorkScheduleForDateRange(startDate, endDate, userId)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        Map<LocalDate, WorkScheduleDay> scheduleMap = result.getData();

                        // Apply business rules to each schedule day
                        if (scheduleMap != null) {
                            scheduleMap.replaceAll((date, schedule) ->
                                    applyUserSpecificBusinessRules(schedule, userId));
                        }

                        Log.d(TAG,"Generated user schedule range for " + userId +
                                ": " + startDate + " to " + endDate +
                                " (" + (scheduleMap != null ? scheduleMap.size() : 0) + " days)");

                        return OperationResult.success(scheduleMap, OperationResult.OperationType.READ);
                    } else {
                        Log.e(TAG,"Failed to get user schedule range: " + result.getErrorMessage(), null);
                        return result;
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG,"Exception in user schedule range generation: " + throwable.getMessage(), null);
                    return OperationResult.failure("Failed to generate user schedule range: " + throwable.getMessage(), OperationResult.OperationType.READ);
                });
    }

    /**
     * Execute use case for complete month.
     *
     * @param userId User ID for schedule generation
     * @param month Target month
     * @return CompletableFuture with monthly schedule map
     */
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> executeForMonth(
            @NonNull Long userId, @NonNull YearMonth month) {

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        Log.d(TAG,"Executing user schedule month generation for userId: " + userId +
                ", month: " + month);

        return executeForDateRange(userId, startDate, endDate);
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Apply user-specific business rules to schedule.
     *
     * @param schedule WorkScheduleDay to process
     * @param userId User ID for context
     * @return Processed WorkScheduleDay
     */
    private WorkScheduleDay applyUserSpecificBusinessRules(@NonNull WorkScheduleDay schedule,
                                                           @NonNull Long userId) {
        try {
            // Apply user-specific business logic
            // Examples:
            // - Check overtime limits
            // - Apply user preferences
            // - Validate consecutive working days
            // - Check certification requirements
            // - Apply seniority rules

            // For now, return schedule as-is
            // Real implementation would include:
            // - User preference validation
            // - Overtime limit checking
            // - Certification validation
            // - Union rule compliance

            return schedule;

        } catch (Exception e) {
            Log.e(TAG,"Error applying user business rules for user " + userId, e);
            return schedule; // Return original schedule on error
        }
    }
}