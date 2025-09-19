package net.calvuz.qdue.domain.calendar.usecases;

import android.util.Log;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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

    // ==================== CLASSES ====================

    /**
     * GenerateUserScheduleForMonth - Individual User Schedule Generation for a Month
     */
    public class GenerateUserScheduleForMonth {
        /**
         * Execute use case for complete month.
         *
         * @param userId User ID for schedule generation
         * @param month  Target month
         * @return CompletableFuture with monthly schedule map
         */
        @NonNull
        public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> execute(
                @NonNull String userId,
                @NonNull YearMonth month
        ) {
            LocalDate startDate = month.atDay( 1 );
            LocalDate endDate = month.atEndOfMonth();

            return getGenerateUserScheduleForDateRange().execute( userId, startDate, endDate );
        }
    }

    /**
     * GenerateUserScheduleForDateRange - Individual User Schedule Generation for a Date Range
     */
    public class GenerateUserScheduleForDateRange {
        /**
         * Execute use case for date range.
         *
         * @param userId    User ID for schedule generation
         * @param startDate Start date (inclusive)
         * @param endDate   End date (inclusive)
         * @return CompletableFuture with Map of dates to WorkScheduleDay
         */
        @NonNull
        public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> execute(
                @NonNull String userId,
                @NonNull LocalDate startDate,
                @NonNull LocalDate endDate
        ) {
            // Validate date range
            if (startDate.isAfter( endDate )) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure( "Start date cannot be after end date",
                                OperationResult.OperationType.VALIDATION ) );
            }

            // Check for reasonable date range (business rule)
            long daysDifference = ChronoUnit.DAYS.between( startDate, endDate );
            if (daysDifference > 365) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure( "Date range cannot exceed 365 days",
                                OperationResult.OperationType.VALIDATION ) );
            }

            return mWorkScheduleRepository.getUserWorkScheduleForDateRange( startDate, endDate, userId )
                    .thenApply( result -> {
                        if (result.isSuccess()) {
                            Map<LocalDate, WorkScheduleDay> scheduleMap = result.getData();

                            // Apply business rules to each schedule day
                            if (scheduleMap != null) {
                                scheduleMap.replaceAll( (date, schedule) ->
                                        applyUserSpecificBusinessRules( schedule, userId ) );
                            }

                            return OperationResult.success( scheduleMap,
                                    OperationResult.OperationType.READ );
                        } else {
                            Log.e( TAG, "Failed to get user schedule range: " + result.getErrorMessage(), null );
                            return result;
                        }
                    } )
                    .exceptionally( throwable -> {
                        Log.e( TAG, "Exception in user schedule range generation: " + throwable.getMessage(), null );
                        return OperationResult.failure( "Failed to generate user schedule range: " + throwable.getMessage(),
                                OperationResult.OperationType.READ );
                    } );
        }
    }


    private class GenerateUserScheduleForDate {

        /**
         * Execute use case for specific date.
         *
         * @param userId User ID for schedule generation
         * @param date   Target date
         * @return CompletableFuture with user's WorkScheduleDay
         */
        @NonNull
        public CompletableFuture<OperationResult<WorkScheduleDay>> execute(
                @NonNull String userId, @NonNull LocalDate date) {

            return mWorkScheduleRepository.getWorkScheduleForDate( date, userId )
                    .thenApply( result -> {
                        if (result.isSuccess()) {
                            WorkScheduleDay schedule = result.getData();

                            // Apply user-specific business rules
                            if (schedule != null) {
                                schedule = applyUserSpecificBusinessRules( schedule, userId );
                            }

                            Log.v( TAG, MessageFormat.format( "Generated user schedule for {0} on {1}",
                                    userId, date ) );
                            return OperationResult.success( schedule,
                                    OperationResult.OperationType.READ );
                        } else {
                            Log.e( TAG, "Failed to get user schedule: " + result.getErrorMessage(), null );
                            return result;
                        }
                    } )
                    .exceptionally( throwable -> {
                        Log.e( TAG, "Exception in user schedule generation: " + throwable.getMessage(), null );
                        return OperationResult.failure( "Failed to generate user schedule: " + throwable.getMessage(),
                                OperationResult.OperationType.READ );
                    } );
        }
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Apply user-specific business rules to schedule.
     *
     * @param schedule WorkScheduleDay to process
     * @param userId   User ID for context
     * @return Processed WorkScheduleDay
     */
    @NonNull
    private WorkScheduleDay applyUserSpecificBusinessRules(@NonNull WorkScheduleDay schedule,
                                                           @NonNull String userId) {
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
            Log.e( TAG, "Error applying user business rules for user " + userId, e );
            return schedule; // Return original schedule on error
        }
    }

    // ==================== USE CASES ====================

    public GenerateUserScheduleForMonth getGenerateUserScheduleForMonth() {
        return new GenerateUserScheduleForMonth();
    }

    public GenerateUserScheduleForDateRange getGenerateUserScheduleForDateRange() {
        return new GenerateUserScheduleForDateRange();
    }

    private GenerateUserScheduleForDate getGenerateUserScheduleForDate() {
        return new GenerateUserScheduleForDate();
    }
}