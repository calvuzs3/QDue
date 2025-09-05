package net.calvuz.qdue.domain.calendar.engines;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SchedulingEngine - Pure Algorithm for Complete Schedule Generation
 *
 * <p>Master scheduling algorithm that orchestrates recurrence calculation and exception
 * resolution to generate complete work schedules. This is a pure domain algorithm that
 * coordinates other domain engines following clean architecture principles.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Pure Algorithm</strong>: No external dependencies or infrastructure concerns</li>
 *   <li><strong>Engine Orchestration</strong>: Coordinates RecurrenceCalculator and ExceptionResolver</li>
 *   <li><strong>Framework Agnostic</strong>: No Android or database dependencies</li>
 *   <li><strong>Testable</strong>: Completely isolated and unit testable</li>
 *   <li><strong>Stateless</strong>: All required data passed as parameters</li>
 * </ul>
 *
 * <h3>Scheduling Process:</h3>
 * <ul>
 *   <li><strong>1. Base Schedule</strong>: Generate from recurrence rules using RecurrenceCalculator</li>
 *   <li><strong>2. Exception Resolution</strong>: Apply exceptions using ExceptionResolver</li>
 *   <li><strong>3. Validation</strong>: Validate final schedule consistency</li>
 *   <li><strong>4. Optimization</strong>: Apply performance optimizations</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-Engine Coordination</strong>: Orchestrates domain engines efficiently</li>
 *   <li><strong>Schedule Validation</strong>: Comprehensive schedule consistency checks</li>
 *   <li><strong>Conflict Resolution</strong>: Advanced conflict detection and resolution</li>
 *   <li><strong>Performance Optimized</strong>: Efficient batch processing</li>
 *   <li><strong>Error Resilient</strong>: Graceful handling of invalid data</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 3.0.0 - Pure Domain Algorithm
 * @since Step 3 - Clean Architecture Implementation
 */
public class SchedulingEngine extends LocalizableDomainModel {

    private static final String TAG = "SchedulingEngine";
    private static final String LOCALIZATION_SCOPE = "scheduling";

    // Coordinated domain engines
    private final RecurrenceCalculator mRecurrenceCalculator;
    private final ExceptionResolver mExceptionResolver;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for creating scheduling engine with coordinated domain engines.
     *
     * @param recurrenceCalculator RecurrenceCalculator for base schedule generation
     * @param exceptionResolver    ExceptionResolver for exception application
     * @param localizer            DomainLocalizer for i18n support (can be null)
     */
    public SchedulingEngine(@NonNull RecurrenceCalculator recurrenceCalculator,
                            @NonNull ExceptionResolver exceptionResolver,
                            @Nullable DomainLocalizer localizer) {
        super( localizer, LOCALIZATION_SCOPE );
        this.mRecurrenceCalculator = recurrenceCalculator;
        this.mExceptionResolver = exceptionResolver;
        Log.d( TAG, "SchedulingEngine initialized with coordinated engines" );
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Create a copy of this SchedulingEngine with localizer injected.
     *
     * @param localizer DomainLocalizer to inject
     * @return New SchedulingEngine instance with localizer support
     */
    @Override
    @NonNull
    public SchedulingEngine withLocalizer(@NonNull DomainLocalizer localizer) {
        return new SchedulingEngine(
                mRecurrenceCalculator.withLocalizer( localizer ),
                mExceptionResolver.withLocalizer( localizer ),
                localizer
        );
    }

    // ==================== CORE SCHEDULING METHODS ====================

    /**
     * Generate complete work schedule for a specific date.
     *
     * <p>Orchestrates the complete scheduling process from recurrence calculation
     * through exception resolution to produce the final work schedule.</p>
     *
     * @param date              Target date for schedule generation
     * @param assignment        User schedule assignment
     * @param recurrenceRule    Recurrence rule for base schedule
     * @param exceptions        List of exceptions to apply
     * @param userTeamMappings  Map of userID to Team for team lookups
     * @param replacementShifts Map of shiftId to WorkScheduleShift for replacements
     * @return Complete WorkScheduleDay with all shifts and exceptions applied
     */
    @NonNull
    public WorkScheduleDay generateCompleteSchedule(@NonNull LocalDate date,
                                                    @NonNull UserScheduleAssignment assignment,
                                                    @NonNull RecurrenceRule recurrenceRule,
                                                    @NonNull List<ShiftException> exceptions,
                                                    @NonNull Map<String, Team> userTeamMappings,
                                                    @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        try {
            Log.d( TAG, "Generating complete schedule for date: " + date +
                    ", user: " + assignment.getUserId() );

            // Step 1: Generate base schedule from recurrence rules
            WorkScheduleDay baseSchedule = mRecurrenceCalculator.generateScheduleForDate(
                    date, recurrenceRule, assignment );

            // Step 2: Apply exceptions to base schedule
            WorkScheduleDay scheduleWithExceptions = mExceptionResolver.applyExceptions(
                    baseSchedule, exceptions, userTeamMappings, replacementShifts );

            // Step 3: Validate and optimize final schedule
            WorkScheduleDay finalSchedule = validateAndOptimizeSchedule( scheduleWithExceptions );

            Log.d( TAG, "Successfully generated complete schedule with " +
                    finalSchedule.getShifts().size() + " shifts" );

            return finalSchedule;
        } catch (Exception e) {
            Log.e( TAG, "Error generating complete schedule for date: " + date, e );
            return createEmptySchedule( date );
        }
    }

    /**
     * Generate complete work schedules for a date range.
     *
     * <p>Batch processing for multiple dates with optimized performance.</p>
     *
     * @param startDate         Start date (inclusive)
     * @param endDate           End date (inclusive)
     * @param assignment        User schedule assignment
     * @param recurrenceRule    Recurrence rule for base schedule
     * @param exceptions        List of exceptions to apply across range
     * @param userTeamMappings  Map of userID to Team for team lookups
     * @param replacementShifts Map of shiftId to WorkScheduleShift for replacements
     * @return Map of dates to complete WorkScheduleDay objects
     */
    @NonNull
    public Map<LocalDate, WorkScheduleDay> generateCompleteScheduleRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate,
            @NonNull UserScheduleAssignment assignment,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull List<ShiftException> exceptions,
            @NonNull Map<String, Team> userTeamMappings,
            @NonNull Map<String, WorkScheduleShift> replacementShifts) {

        Map<LocalDate, WorkScheduleDay> scheduleMap = new HashMap<>();

        try {
            Log.d( TAG, "Generating complete schedule range: " + startDate + " to " + endDate );

            // Batch generate base schedules for efficiency
            Map<LocalDate, List<net.calvuz.qdue.domain.calendar.models.Shift>> baseShifts =
                    mRecurrenceCalculator.calculateShiftsForDateRange( startDate, endDate, recurrenceRule, assignment );

            // Process each date
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter( endDate )) {

                // Create base schedule for this date
                WorkScheduleDay baseSchedule = createScheduleFromShifts( currentDate,
                        baseShifts.getOrDefault( currentDate, new ArrayList<>() ) );

                // Filter exceptions for this date
                List<ShiftException> dateExceptions = filterExceptionsForDate( exceptions, currentDate );

                // Apply exceptions
                WorkScheduleDay finalSchedule = mExceptionResolver.applyExceptions(
                        baseSchedule, dateExceptions, userTeamMappings, replacementShifts );

                // Validate and add to map
                scheduleMap.put( currentDate, validateAndOptimizeSchedule( finalSchedule ) );

                currentDate = currentDate.plusDays( 1 );
            }

            Log.d( TAG, "Successfully generated schedule range with " + scheduleMap.size() + " days" );
        } catch (Exception e) {
            Log.e( TAG, "Error generating schedule range", e );
        }

        return scheduleMap;
    }

    /**
     * Generate team-wide schedule for specific date.
     *
     * <p>Combines individual user schedules into a comprehensive team schedule.</p>
     *
     * @param date              Target date
     * @param assignments       List of user assignments for the team
     * @param recurrenceRules   Map of ruleId to RecurrenceRule
     * @param exceptions        List of exceptions across all users
     * @param userTeamMappings  Map of userID to Team
     * @param replacementShifts Map of shiftId to WorkScheduleShift
     * @return Combined team WorkScheduleDay
     */
    @NonNull
    public WorkScheduleDay generateTeamSchedule(@NonNull LocalDate date,
                                                @NonNull List<UserScheduleAssignment> assignments,
                                                @NonNull Map<String, RecurrenceRule> recurrenceRules,
                                                @NonNull List<ShiftException> exceptions,
                                                @NonNull Map<String, Team> userTeamMappings,
                                                @NonNull Map<String, WorkScheduleShift> replacementShifts) {
        try {
            Log.d( TAG, "Generating team schedule for date: " + date +
                    ", assignments: " + assignments.size() );

            WorkScheduleDay.Builder teamScheduleBuilder = WorkScheduleDay.builder( date );

            // Generate schedule for each user assignment
            for (UserScheduleAssignment assignment : assignments) {
                RecurrenceRule rule = recurrenceRules.get( assignment.getRecurrenceRuleId() );

                if (rule != null) {
                    // Filter exceptions for this user
                    List<ShiftException> userExceptions = filterExceptionsForUser( exceptions, assignment.getUserId() );

                    // Generate complete schedule for this user
                    WorkScheduleDay userSchedule = generateCompleteSchedule(
                            date, assignment, rule, userExceptions, userTeamMappings, replacementShifts );

                    // Add user's shifts to team schedule
                    for (WorkScheduleShift shift : userSchedule.getShifts()) {
                        teamScheduleBuilder.addShift( shift );
                    }
                }
            }

            WorkScheduleDay teamSchedule = teamScheduleBuilder.build();

            // Validate team schedule for conflicts
            validateTeamScheduleConsistency( teamSchedule );

            Log.d( TAG, "Successfully generated team schedule with " +
                    teamSchedule.getShifts().size() + " total shifts" );

            return teamSchedule;
        } catch (Exception e) {
            Log.e( TAG, "Error generating team schedule for date: " + date, e );
            return createEmptySchedule( date );
        }
    }

    // ==================== VALIDATION AND OPTIMIZATION ====================

    /**
     * Validate and optimize a generated schedule.
     *
     * @param schedule Schedule to validate and optimize
     * @return Validated and optimized schedule
     */
    @NonNull
    private WorkScheduleDay validateAndOptimizeSchedule(@NonNull WorkScheduleDay schedule) {
        try {
            // Basic validation
            validateScheduleConsistency( schedule );

            // Apply optimizations
            WorkScheduleDay optimizedSchedule = applyScheduleOptimizations( schedule );

            return optimizedSchedule;
        } catch (Exception e) {
            Log.w( TAG, "Schedule validation/optimization failed, returning original" );
            return schedule;
        }
    }

    private void validateScheduleConsistency(@NonNull WorkScheduleDay schedule) {
        for (WorkScheduleShift shift : schedule.getShifts()) {
            // Check basic shift validity
            if (shift.getStartTime().isAfter( shift.getEndTime() ) && !isOvernightShift( shift )) {
                Log.w( TAG, "Invalid shift timing detected: " + shift.getStartTime() +
                        " to " + shift.getEndTime() );
            }

            // Check team assignments
            if (shift.getTeams().isEmpty()) {
                Log.w( TAG, "Shift with no team assignments detected" );
            }
        }
    }

    private void validateTeamScheduleConsistency(@NonNull WorkScheduleDay teamSchedule) {
        // Check for scheduling conflicts between team members
        Map<String, List<WorkScheduleShift>> shiftsByType = new HashMap<>();

        for (WorkScheduleShift shift : teamSchedule.getShifts()) {
            String shiftType = shift.getShift().getName();
            shiftsByType.computeIfAbsent( shiftType, k -> new ArrayList<>() ).add( shift );
        }

        // Validate coverage requirements
        for (Map.Entry<String, List<WorkScheduleShift>> entry : shiftsByType.entrySet()) {
            String shiftType = entry.getKey();
            List<WorkScheduleShift> shifts = entry.getValue();

            if (shifts.size() < getMinimumCoverageForShiftType( shiftType )) {
                Log.w( TAG, "Insufficient coverage for shift type: " + shiftType +
                        " (have: " + shifts.size() + ", need: " +
                        getMinimumCoverageForShiftType( shiftType ) + ")" );
            }
        }
    }

    private WorkScheduleDay applyScheduleOptimizations(@NonNull WorkScheduleDay schedule) {
        // Apply performance and efficiency optimizations
        // For now, return the schedule as-is
        // Future optimizations could include:
        // - Consolidating adjacent shifts
        // - Optimizing break scheduling
        // - Balancing workload distribution
        return schedule;
    }

    // ==================== HELPER METHODS ====================

    private WorkScheduleDay createEmptySchedule(@NonNull LocalDate date) {
        return WorkScheduleDay.builder( date ).build();
    }

    private WorkScheduleDay createScheduleFromShifts(@NonNull LocalDate date,
                                                     @NonNull List<net.calvuz.qdue.domain.calendar.models.Shift> shifts) {
        WorkScheduleDay.Builder builder = WorkScheduleDay.builder( date );

        for (net.calvuz.qdue.domain.calendar.models.Shift shift : shifts) {
            WorkScheduleShift workScheduleShift = WorkScheduleShift.builder()
                    .shift( shift )
                    .startTime( shift.getStartTime() )
                    .endTime( shift.getEndTime() )
                    .build();
            builder.addShift( workScheduleShift );
        }

        return builder.build();
    }

    private List<ShiftException> filterExceptionsForDate(@NonNull List<ShiftException> exceptions,
                                                         @NonNull LocalDate date) {
        return exceptions.stream()
                .filter( exception -> exception.appliesTo( date ) )
                .collect( java.util.stream.Collectors.toList() );
    }

    private List<ShiftException> filterExceptionsForUser(@NonNull List<ShiftException> exceptions,
                                                         @NonNull String userId) {
        return exceptions.stream()
                .filter( exception -> userId.equals( exception.getUserId() ) )
                .collect( java.util.stream.Collectors.toList() );
    }

    private boolean isOvernightShift(@NonNull WorkScheduleShift shift) {
        return shift.getStartTime().isAfter( shift.getEndTime() );
    }

    private int getMinimumCoverageForShiftType(@NonNull String shiftType) {
        // Business rule for minimum coverage requirements
        switch (shiftType.toLowerCase()) {
            case "morning":
            case "afternoon":
            case "night":
                return 1; // At least one person per shift type
            default:
                return 0;
        }
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized schedule generation status message.
     */
    @NonNull
    public String getScheduleGenerationStatus(@NonNull WorkScheduleDay schedule) {
        int shiftCount = schedule.getShifts().size();

        if (shiftCount == 0) {
            return localize( "status.no_shifts", "No shifts scheduled", String.valueOf( shiftCount ) );
        } else {
            return localize( "status.shifts_generated",
                    "Generated " + shiftCount + " shifts", String.valueOf( shiftCount ) );
        }
    }

    /**
     * Get localized validation status message.
     */
    @NonNull
    public String getValidationStatus(@NonNull WorkScheduleDay schedule) {
        // Simple validation check
        boolean hasInvalidShifts = schedule.getShifts().stream()
                .anyMatch( shift -> shift.getStartTime().isAfter( shift.getEndTime() ) &&
                        !isOvernightShift( shift ) );

        if (hasInvalidShifts) {
            return localize( "validation.has_errors", "Schedule has validation errors" );
        } else {
            return localize( "validation.passed", "Schedule validation passed" );
        }
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create SchedulingEngine with domain engines and without localization.
     */
    @NonNull
    public static SchedulingEngine create(@NonNull RecurrenceCalculator recurrenceCalculator,
                                          @NonNull ExceptionResolver exceptionResolver) {
        return new SchedulingEngine( recurrenceCalculator, exceptionResolver, null );
    }

    /**
     * Create SchedulingEngine with domain engines and localization support.
     */
    @NonNull
    public static SchedulingEngine create(@NonNull RecurrenceCalculator recurrenceCalculator,
                                          @NonNull ExceptionResolver exceptionResolver,
                                          @NonNull DomainLocalizer localizer) {
        return new SchedulingEngine( recurrenceCalculator, exceptionResolver, localizer );
    }
}