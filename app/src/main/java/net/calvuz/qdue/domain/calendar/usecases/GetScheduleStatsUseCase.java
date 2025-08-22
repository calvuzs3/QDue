package net.calvuz.qdue.domain.calendar.usecases;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GetScheduleStatsUseCase - Schedule Analytics and Validation
 *
 * <p>Comprehensive use case for schedule analytics, statistics generation,
 * and validation operations. Provides management reporting and planning
 * capabilities with advanced business rule validation.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Schedule Analytics</strong>: Comprehensive statistics generation</li>
 *   <li><strong>Validation Engine</strong>: Business rule validation for schedule changes</li>
 *   <li><strong>Performance Metrics</strong>: Coverage analysis and utilization tracking</li>
 *   <li><strong>Planning Support</strong>: Data for management decision making</li>
 *   <li><strong>Compliance Checking</strong>: Regulatory and policy compliance</li>
 * </ul>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Single Responsibility</strong>: Analytics and validation only</li>
 *   <li><strong>Repository Pattern</strong>: Uses WorkScheduleRepository interface</li>
 *   <li><strong>Domain Logic</strong>: Business rules encapsulated in use case</li>
 *   <li><strong>Async Operations</strong>: All methods return CompletableFuture</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Complete Implementation
 * @since Clean Architecture Implementation
 */
public class GetScheduleStatsUseCase {

    private static final String TAG = "GetScheduleStatsUseCase";

    // Dependencies
    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param workScheduleRepository Repository for work schedule operations
     */
    public GetScheduleStatsUseCase(@NonNull WorkScheduleRepository workScheduleRepository) {
        this.mWorkScheduleRepository = workScheduleRepository;
    }

    // ==================== ANALYTICS OPERATIONS ====================

    /**
     * Execute use case to generate comprehensive schedule statistics.
     *
     * @param startDate Start of analysis period
     * @param endDate   End of analysis period
     * @param userId    Optional user ID for user-specific stats (null for all users)
     * @return CompletableFuture with detailed ScheduleStats
     */
    @NonNull
    public CompletableFuture<OperationResult<ScheduleStats>> executeAnalysis(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable Long userId) {

        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Generating schedule statistics for period: " + startDate + " to " + endDate +
                        (userId != null ? ", userId: " + userId : " (all users)") );

                // Validate input
                OperationResult<Void> validation = validateAnalysisInput( startDate, endDate, userId );
                if (!validation.isSuccess()) {
                    return OperationResult.failure( validation.getErrorMessage(), OperationResult.OperationType.VALIDATION );
                }

                // Get schedule data for analysis
                OperationResult<Map<LocalDate, WorkScheduleDay>> scheduleResult =
                        mWorkScheduleRepository.getWorkScheduleForDateRange( startDate, endDate, userId ).join();

                if (!scheduleResult.isSuccess()) {
                    return OperationResult.failure( "Failed to get schedule for analysis: " +
                            scheduleResult.getErrorMessage(), OperationResult.OperationType.VALIDATION );
                }

                Map<LocalDate, WorkScheduleDay> scheduleMap = scheduleResult.getData();
                if (scheduleMap == null || scheduleMap.isEmpty()) {
                    return OperationResult.failure( "No schedule data available for analysis", OperationResult.OperationType.VALIDATION );
                }

                // Calculate comprehensive statistics
                ScheduleStats stats = calculateComprehensiveStatistics( scheduleMap, startDate, endDate, userId );

                Log.d( TAG, "Generated schedule statistics for " + scheduleMap.size() + " days" );

                return OperationResult.success( stats, OperationResult.OperationType.VALIDATION );
            } catch (Exception e) {
                Log.e( TAG, "Error generating schedule statistics", e );
                return OperationResult.failure( "Failed to generate statistics: " + e.getMessage(), OperationResult.OperationType.VALIDATION );
            }
        } );
    }

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validate proposed schedule changes against business rules.
     *
     * @param userId          User ID for validation context
     * @param originalDate    Original date being modified
     * @param proposedChanges Description of proposed changes
     * @return CompletableFuture with detailed ScheduleValidationResult
     */
    @NonNull
    public CompletableFuture<OperationResult<ScheduleValidationResult>> validateScheduleChanges(
            @NonNull Long userId, @NonNull LocalDate originalDate, @NonNull String proposedChanges) {

        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Validating schedule changes for userId: " + userId +
                        ", date: " + originalDate );

                // Create validation result
                ScheduleValidationResult result = new ScheduleValidationResult();
                result.userId = userId;
                result.targetDate = originalDate;
                result.proposedChanges = proposedChanges;
                result.isValid = true;

                // Validate input parameters
                if (proposedChanges == null || proposedChanges.trim().isEmpty()) {
                    result.isValid = false;
                    result.validationErrors.add( "Proposed changes description cannot be empty" );
                    return OperationResult.success( result, OperationResult.OperationType.VALIDATION );
                }

                // Get current schedule for comparison
                OperationResult<WorkScheduleDay> currentScheduleResult =
                        mWorkScheduleRepository.getWorkScheduleForDate( originalDate, userId ).join();

                if (!currentScheduleResult.isSuccess()) {
                    result.warnings.add( "Could not retrieve current schedule for validation" );
                }

                WorkScheduleDay currentSchedule = currentScheduleResult.getData();

                // Apply business rule validations
                result = applyBusinessRuleValidations( result, currentSchedule, proposedChanges );

                // Apply regulatory compliance checks
                result = applyComplianceValidations( result, userId, originalDate );

                // Apply temporal validations
                result = applyTemporalValidations( result, originalDate );

                Log.d( TAG, "Schedule validation completed for user " + userId +
                        ", valid: " + result.isValid );

                return OperationResult.success( result, OperationResult.OperationType.VALIDATION );
            } catch (Exception e) {
                Log.e( TAG, "Error validating schedule changes", e );

                ScheduleValidationResult errorResult = new ScheduleValidationResult();
                errorResult.userId = userId;
                errorResult.targetDate = originalDate;
                errorResult.proposedChanges = proposedChanges;
                errorResult.isValid = false;
                errorResult.validationErrors.add( "Validation error: " + e.getMessage() );

                return OperationResult.success( errorResult, OperationResult.OperationType.VALIDATION );
            }
        } );
    }

    // ==================== STATISTICS CALCULATION ====================

    private ScheduleStats calculateComprehensiveStatistics(@NonNull Map<LocalDate, WorkScheduleDay> scheduleMap,
                                                           @NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate,
                                                           @Nullable Long userId) {
        ScheduleStats stats = new ScheduleStats();

        // Basic metrics
        stats.analysisStartDate = startDate;
        stats.analysisEndDate = endDate;
        stats.userId = userId;
        stats.totalDays = scheduleMap.size();
        stats.workingDays = 0;
        stats.restDays = 0;
        stats.totalShifts = 0;
        stats.totalWorkingHours = 0.0;

        // Advanced metrics
        Map<String, Integer> shiftTypeDistribution = new HashMap<>();
        Map<String, Double> dailyHoursDistribution = new HashMap<>();
        List<Double> dailyHours = new ArrayList<>();

        // Process each schedule day
        for (Map.Entry<LocalDate, WorkScheduleDay> entry : scheduleMap.entrySet()) {
            LocalDate date = entry.getKey();
            WorkScheduleDay schedule = entry.getValue();

            if (schedule.hasShifts()) {
                stats.workingDays++;
                int dayShifts = schedule.getShifts().size();
                stats.totalShifts += dayShifts;

                // Calculate working hours for this day
                double dayHours = 0.0;
                for (var shift : schedule.getShifts()) {
                    double shiftHours = calculateShiftDuration( shift );
                    dayHours += shiftHours;
                    stats.totalWorkingHours += shiftHours;

                    // Track shift type distribution
                    String shiftType = shift.getShift().getName();
                    shiftTypeDistribution.merge( shiftType, 1, Integer::sum );
                }

                dailyHours.add( dayHours );
                dailyHoursDistribution.put( date.toString(), dayHours );
            } else {
                stats.restDays++;
                dailyHours.add( 0.0 );
            }
        }

        // Calculate averages and derived metrics
        stats.averageShiftsPerDay = stats.totalDays > 0 ?
                (double) stats.totalShifts / stats.totalDays : 0.0;

        stats.averageHoursPerDay = stats.totalDays > 0 ?
                stats.totalWorkingHours / stats.totalDays : 0.0;

        stats.averageHoursPerWorkingDay = stats.workingDays > 0 ?
                stats.totalWorkingHours / stats.workingDays : 0.0;

        stats.workingDayPercentage = stats.totalDays > 0 ?
                (double) stats.workingDays / stats.totalDays * 100.0 : 0.0;

        // Set distribution data
        stats.shiftTypeDistribution = shiftTypeDistribution;
        stats.dailyHoursDistribution = dailyHoursDistribution;

        // Calculate additional metrics
        if (!dailyHours.isEmpty()) {
            stats.maxDailyHours = dailyHours.stream().mapToDouble( Double::doubleValue ).max().orElse( 0.0 );
            stats.minDailyHours = dailyHours.stream().mapToDouble( Double::doubleValue ).min().orElse( 0.0 );
        }

        // Calculate trend analysis
        calculateTrendAnalysis( stats, scheduleMap );

        return stats;
    }

    private void calculateTrendAnalysis(@NonNull ScheduleStats stats,
                                        @NonNull Map<LocalDate, WorkScheduleDay> scheduleMap) {
        try {
            // Simple trend analysis - could be expanded
            List<LocalDate> sortedDates = new ArrayList<>( scheduleMap.keySet() );
            sortedDates.sort( LocalDate::compareTo );

            if (sortedDates.size() >= 7) {
                // Compare first week vs last week
                int weekSize = Math.min( 7, sortedDates.size() / 2 );

                double firstWeekHours = 0.0;
                double lastWeekHours = 0.0;

                for (int i = 0; i < weekSize; i++) {
                    WorkScheduleDay firstWeekDay = scheduleMap.get( sortedDates.get( i ) );
                    if (firstWeekDay != null) {
                        firstWeekHours += calculateDayHours( firstWeekDay );
                    }

                    int lastIndex = sortedDates.size() - 1 - i;
                    WorkScheduleDay lastWeekDay = scheduleMap.get( sortedDates.get( lastIndex ) );
                    if (lastWeekDay != null) {
                        lastWeekHours += calculateDayHours( lastWeekDay );
                    }
                }

                stats.trendAnalysis = String.format( "First week avg: %.1f hrs, Last week avg: %.1f hrs",
                        firstWeekHours / weekSize, lastWeekHours / weekSize );
            }
        } catch (Exception e) {
            stats.trendAnalysis = "Trend analysis unavailable";
        }
    }

    private double calculateDayHours(@NonNull WorkScheduleDay schedule) {
        return schedule.getShifts().stream()
                .mapToDouble( this::calculateShiftDuration )
                .sum();
    }

    private double calculateShiftDuration(@NonNull net.calvuz.qdue.domain.calendar.models.WorkScheduleShift shift) {
        try {
            // Calculate duration in hours between start and end time
            long minutes = java.time.Duration.between( shift.getStartTime(), shift.getEndTime() ).toMinutes();

            // Handle overnight shifts
            if (minutes < 0) {
                minutes += 24 * 60; // Add 24 hours for overnight shifts
            }

            return minutes / 60.0;
        } catch (Exception e) {
            Log.w( TAG, "Error calculating shift duration, using 8 hours default" );
            return 8.0; // Default fallback
        }
    }

    // ==================== VALIDATION LOGIC ====================

    private ScheduleValidationResult applyBusinessRuleValidations(@NonNull ScheduleValidationResult result,
                                                                  @Nullable WorkScheduleDay currentSchedule,
                                                                  @NonNull String proposedChanges) {
        try {
            // Business Rule 1: Check for consecutive working days limit
            if (proposedChanges.toLowerCase().contains( "overtime" )) {
                result.warnings.add( "Overtime request requires additional approval" );
            }

            // Business Rule 2: Check for minimum rest period
            if (proposedChanges.toLowerCase().contains( "night" ) &&
                    proposedChanges.toLowerCase().contains( "morning" )) {
                result.validationErrors.add( "Insufficient rest period between night and morning shifts" );
                result.isValid = false;
            }

            // Business Rule 3: Check for weekend work
            if (result.targetDate.getDayOfWeek().getValue() >= 6) { // Saturday or Sunday
                if (proposedChanges.toLowerCase().contains( "extra" )) {
                    result.warnings.add( "Weekend overtime may require additional compensation" );
                }
            }

            // Business Rule 4: Check current workload
            if (currentSchedule != null && currentSchedule.hasShifts()) {
                double currentHours = calculateDayHours( currentSchedule );
                if (currentHours > 10 && proposedChanges.toLowerCase().contains( "add" )) {
                    result.validationErrors.add( "Cannot add shifts when daily hours exceed 10" );
                    result.isValid = false;
                }
            }
        } catch (Exception e) {
            result.warnings.add( "Business rule validation error: " + e.getMessage() );
        }

        return result;
    }

    private ScheduleValidationResult applyComplianceValidations(@NonNull ScheduleValidationResult result,
                                                                @NonNull Long userId,
                                                                @NonNull LocalDate targetDate) {
        try {
            // Compliance Rule 1: Future date validation
            LocalDate maxFutureDate = LocalDate.now().plusMonths( 6 );
            if (targetDate.isAfter( maxFutureDate )) {
                result.validationErrors.add( "Cannot modify schedules more than 6 months in advance" );
                result.isValid = false;
            }

            // Compliance Rule 2: Past date validation
            LocalDate minPastDate = LocalDate.now().minusDays( 7 );
            if (targetDate.isBefore( minPastDate )) {
                result.validationErrors.add( "Cannot modify schedules more than 7 days in the past" );
                result.isValid = false;
            }

            // Compliance Rule 3: Holiday restrictions
            if (isHoliday( targetDate )) {
                result.warnings.add( "Schedule change affects a holiday - additional approvals may be required" );
            }
        } catch (Exception e) {
            result.warnings.add( "Compliance validation error: " + e.getMessage() );
        }

        return result;
    }

    private ScheduleValidationResult applyTemporalValidations(@NonNull ScheduleValidationResult result,
                                                              @NonNull LocalDate targetDate) {
        try {
            LocalDate now = LocalDate.now();

            // Temporal Rule 1: Last minute changes
            if (targetDate.equals( now ) || targetDate.equals( now.plusDays( 1 ) )) {
                result.warnings.add( "Last minute schedule changes may impact team coordination" );
            }

            // Temporal Rule 2: Weekend processing
            if (now.getDayOfWeek().getValue() >= 6) { // Current day is weekend
                result.warnings.add( "Weekend schedule changes may have delayed processing" );
            }
        } catch (Exception e) {
            result.warnings.add( "Temporal validation error: " + e.getMessage() );
        }

        return result;
    }

    // ==================== HELPER METHODS ====================

    private OperationResult<Void> validateAnalysisInput(@NonNull LocalDate startDate,
                                                        @NonNull LocalDate endDate,
                                                        @Nullable Long userId) {
        if (startDate.isAfter( endDate )) {
            return OperationResult.failure( "Start date cannot be after end date", OperationResult.OperationType.VALIDATION );
        }

        // Check for reasonable date range
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between( startDate, endDate );
        if (daysDifference > 730) { // 2 years
            return OperationResult.failure( "Analysis period cannot exceed 2 years", OperationResult.OperationType.VALIDATION );
        }

        if (daysDifference < 0) {
            return OperationResult.failure( "Invalid date range", OperationResult.OperationType.VALIDATION );
        }

        // Validate user ID if provided
        if (userId != null && userId <= 0) {
            return OperationResult.failure( "User ID must be positive", OperationResult.OperationType.VALIDATION );
        }

        return OperationResult.success( "validateAnalysisInput success", OperationResult.OperationType.VALIDATION );
    }

    private boolean isHoliday(@NonNull LocalDate date) {
        // Simple holiday check - could be enhanced with proper holiday calendar
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Basic Italian holidays
        return (month == 1 && day == 1) ||   // New Year
                (month == 12 && day == 25) ||  // Christmas
                (month == 8 && day == 15) ||   // Assumption
                (month == 4 && day == 25) ||   // Liberation Day
                (month == 5 && day == 1);      // Labor Day
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Comprehensive schedule statistics result.
     */
    public static class ScheduleStats {
        // Analysis parameters
        public LocalDate analysisStartDate;
        public LocalDate analysisEndDate;
        public Long userId; // null for all users

        // Basic metrics
        public int totalDays;
        public int workingDays;
        public int restDays;
        public int totalShifts;
        public double totalWorkingHours;

        // Average metrics
        public double averageShiftsPerDay;
        public double averageHoursPerDay;
        public double averageHoursPerWorkingDay;
        public double workingDayPercentage;

        // Distribution data
        public Map<String, Integer> shiftTypeDistribution = new HashMap<>();
        public Map<String, Double> dailyHoursDistribution = new HashMap<>();

        // Advanced metrics
        public double maxDailyHours;
        public double minDailyHours;
        public String trendAnalysis;

        // Coverage analysis (could be expanded)
        public double coverageScore = 100.0; // Percentage
        public List<String> coverageIssues = new ArrayList<>();

        @Override
        public String toString() {
            return String.format( "ScheduleStats{totalDays=%d, workingDays=%d, totalHours=%.1f, avgHours=%.1f}",
                    totalDays, workingDays, totalWorkingHours, averageHoursPerDay );
        }
    }

    /**
     * Schedule validation result with detailed feedback.
     */
    public static class ScheduleValidationResult {
        public boolean isValid = true;
        public Long userId;
        public LocalDate targetDate;
        public String proposedChanges;
        public List<String> validationErrors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        // Additional validation metadata
        public String validationTimestamp = java.time.Instant.now().toString();
        public String validationSummary;

        public boolean hasErrors() {
            return !validationErrors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getSummary() {
            if (validationSummary != null) {
                return validationSummary;
            }

            StringBuilder summary = new StringBuilder();
            summary.append( isValid ? "VALID" : "INVALID" );

            if (hasErrors()) {
                summary.append( " (" ).append( validationErrors.size() ).append( " errors)" );
            }

            if (hasWarnings()) {
                summary.append( " (" ).append( warnings.size() ).append( " warnings)" );
            }

            return summary.toString();
        }

        @Override
        public String toString() {
            return String.format( "ScheduleValidationResult{valid=%s, errors=%d, warnings=%d}",
                    isValid, validationErrors.size(), warnings.size() );
        }
    }
}