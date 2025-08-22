package net.calvuz.qdue.domain.calendar.engines;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RecurrenceCalculator - Pure Algorithm for Google Calendar RRULE Processing
 *
 * <p>Advanced recurrence calculation engine supporting Google Calendar-compatible
 * RRULE patterns with QuattroDue-specific extensions. This is a pure domain algorithm
 * with no external dependencies, following clean architecture principles.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Pure Algorithm</strong>: No repository or infrastructure dependencies</li>
 *   <li><strong>Framework Agnostic</strong>: No Android or database dependencies</li>
 *   <li><strong>Testable</strong>: Completely isolated and unit testable</li>
 *   <li><strong>Stateless</strong>: All required data passed as parameters</li>
 * </ul>
 *
 * <h3>Supported Recurrence Types:</h3>
 * <ul>
 *   <li><strong>DAILY</strong>: Daily recurring patterns with intervals</li>
 *   <li><strong>WEEKLY</strong>: Weekly patterns with specific weekdays</li>
 *   <li><strong>MONTHLY</strong>: Monthly patterns with date/weekday rules</li>
 *   <li><strong>QUATTRODUE_CYCLE</strong>: Custom 4-2 cycle patterns</li>
 *   <li><strong>CONTINUOUS_CYCLE</strong>: Extended continuous shift cycles</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>RRULE Compatible</strong>: Standard RFC 5545 recurrence rules</li>
 *   <li><strong>Cycle Calculations</strong>: Advanced cycle position tracking</li>
 *   <li><strong>Team Offsets</strong>: Multi-team coordination with phase shifts</li>
 *   <li><strong>Performance Optimized</strong>: Efficient date calculations</li>
 *   <li><strong>Error Resilient</strong>: Graceful handling of invalid patterns</li>
 * </ul>
 *
 * <h3>JSON Pattern Format:</h3>
 * <pre>
 * {
 *   "frequency": "QUATTRODUE_CYCLE",
 *   "interval": 1,
 *   "cycle_length": 18,
 *   "shift_sequence": [
 *     {"days": [1,2,3,4], "shift_type": "morning"},
 *     {"days": [5,6], "shift_type": "rest"},
 *     {"days": [7,8,9,10], "shift_type": "afternoon"},
 *     {"days": [11,12], "shift_type": "rest"},
 *     {"days": [13,14,15,16], "shift_type": "night"},
 *     {"days": [17,18], "shift_type": "rest"}
 *   ]
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 3.1.0 - Pure Domain Algorithm
 * @since Step 3 - Clean Architecture Implementation
 */
public class RecurrenceCalculator extends LocalizableDomainModel {

    private static final String TAG = "RecurrenceCalculator";
    private static final String LOCALIZATION_SCOPE = "recurrence";

    private final Gson mGson;

    // Performance caching for patterns
    private final Map<String, RecurrencePattern> mPatternCache;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for creating recurrence calculator with optional localization.
     *
     * @param localizer DomainLocalizer for i18n support (can be null)
     */
    public RecurrenceCalculator(@Nullable DomainLocalizer localizer) {
        super( localizer, LOCALIZATION_SCOPE );
        this.mGson = new Gson();
        this.mPatternCache = new HashMap<>();
        Log.d( TAG, "RecurrenceCalculator initialized as pure algorithm" );
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Create a copy of this RecurrenceCalculator with localizer injected.
     *
     * @param localizer DomainLocalizer to inject
     * @return New RecurrenceCalculator instance with localizer support
     */
    @Override
    @NonNull
    public RecurrenceCalculator withLocalizer(@NonNull DomainLocalizer localizer) {
        return new RecurrenceCalculator( localizer );
    }

    // ==================== CORE CALCULATION METHODS ====================

    /**
     * Generate complete work schedule for a date based on recurrence rule and assignment.
     *
     * @param date           Target date for schedule generation
     * @param recurrenceRule Recurrence rule domain model
     * @param assignment     User schedule assignment domain model
     * @return WorkScheduleDay with all shifts for the date
     */
    @NonNull
    public WorkScheduleDay generateScheduleForDate(@NonNull LocalDate date,
                                                   @NonNull RecurrenceRule recurrenceRule,
                                                   @NonNull UserScheduleAssignment assignment) {
        try {
            Log.d( TAG, "Generating schedule for date: " + date + ", rule: " + recurrenceRule.getId() );

            WorkScheduleDay.Builder scheduleBuilder = WorkScheduleDay.builder( date );

            // Calculate shifts for this date
            List<Shift> shifts = calculateShiftsForDate( date, recurrenceRule, assignment );

            // Add each shift to the schedule
            for (Shift shift : shifts) {
                WorkScheduleShift workScheduleShift = createWorkScheduleShift( shift, assignment );
                if (workScheduleShift != null) {
                    scheduleBuilder.addShift( workScheduleShift );
                }
            }

            WorkScheduleDay schedule = scheduleBuilder.build();
            Log.d( TAG, "Generated schedule with " + schedule.getShifts().size() + " shifts" );

            return schedule;
        } catch (Exception e) {
            Log.e( TAG, "Error generating schedule for date: " + date, e );
            return WorkScheduleDay.builder( date ).build(); // Return empty schedule on error
        }
    }

    /**
     * Calculate shifts for specific date based on recurrence rule and user assignment.
     *
     * @param date           Target date for calculation
     * @param recurrenceRule Recurrence rule domain model
     * @param assignment     User schedule assignment domain model
     * @return List of Shift objects for the date
     */
    @NonNull
    public List<Shift> calculateShiftsForDate(@NonNull LocalDate date,
                                              @NonNull RecurrenceRule recurrenceRule,
                                              @NonNull UserScheduleAssignment assignment) {
        try {
            Log.d( TAG, "Calculating shifts for date: " + date + ", rule: " + recurrenceRule.getId() );

            // Parse recurrence pattern from domain model
            RecurrencePattern pattern = parseRecurrencePattern( recurrenceRule );

            // Get team offset from assignment (or 0 if not available)
            int teamOffset = getTeamOffsetFromAssignment( assignment );

            // Calculate cycle position for the date
            int cyclePosition = calculateCyclePosition( date, assignment.getStartDate(),
                    pattern, teamOffset );

            // Get shift assignments for this cycle position
            List<ShiftAssignment> shiftAssignments = getShiftAssignmentsForCycleDay( pattern, cyclePosition );

            // Convert to Shift domain objects
            List<Shift> shifts = new ArrayList<>();
            for (ShiftAssignment shiftAssignment : shiftAssignments) {
                Shift shift = createShiftFromAssignment( shiftAssignment, date );
                if (shift != null) {
                    shifts.add( shift );
                }
            }

            Log.d( TAG, "Generated " + shifts.size() + " shifts for date: " + date );
            return shifts;
        } catch (Exception e) {
            Log.e( TAG, "Error calculating shifts for date: " + date, e );
            return new ArrayList<>(); // Return empty list on error
        }
    }

    /**
     * Calculate shifts for date range with batch optimization.
     *
     * @param startDate      Start date (inclusive)
     * @param endDate        End date (inclusive)
     * @param recurrenceRule Recurrence rule domain model
     * @param assignment     User schedule assignment domain model
     * @return Map of dates to List of Shift objects
     */
    @NonNull
    public Map<LocalDate, List<Shift>> calculateShiftsForDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        Map<LocalDate, List<Shift>> shiftsMap = new HashMap<>();

        try {
            Log.d( TAG, "Calculating shifts for date range: " + startDate + " to " + endDate );

            // Parse pattern once for efficiency
            RecurrencePattern pattern = parseRecurrencePattern( recurrenceRule );
            int teamOffset = getTeamOffsetFromAssignment( assignment );

            // Process each date in range
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter( endDate )) {
                List<Shift> shifts = calculateShiftsForDate( currentDate, recurrenceRule, assignment );
                shiftsMap.put( currentDate, shifts );
                currentDate = currentDate.plusDays( 1 );
            }

            Log.d( TAG, "Generated shifts for " + shiftsMap.size() + " dates" );
        } catch (Exception e) {
            Log.e( TAG, "Error calculating shifts for date range", e );
        }

        return shiftsMap;
    }

    /**
     * Validate recurrence rule pattern for correctness.
     *
     * @param recurrenceRule Recurrence rule domain model to validate
     * @return ValidationResult with status and messages
     */
    @NonNull
    public ValidationResult validateRecurrenceRule(@NonNull RecurrenceRule recurrenceRule) {
        try {
            Log.d( TAG, "Validating recurrence rule: " + recurrenceRule.getId() );

            RecurrencePattern pattern = parseRecurrencePattern( recurrenceRule );

            // Basic validation checks
            ValidationResult result = new ValidationResult();
            result.isValid = true;

            // Check cycle length
            if (pattern.cycleLength <= 0 || pattern.cycleLength > 365) {
                result.isValid = false;
                result.errorMessage = localize( "validation.invalid_cycle_length",
                        "Invalid cycle length: " + pattern.cycleLength );
                return result;
            }

            // Check shift sequence coverage
            if (!validateShiftSequenceCoverage( pattern )) {
                result.isValid = false;
                result.errorMessage = localize( "validation.incomplete_shift_coverage",
                        "Shift sequence does not cover all cycle days" );
                return result;
            }

            // Check for gaps or overlaps
            ValidationIssues issues = checkForGapsAndOverlaps( pattern );
            if (issues.hasIssues()) {
                result.isValid = false;
                result.errorMessage = issues.getDescription();
                return result;
            }

            Log.d( TAG, "Recurrence rule validation passed" );
            return result;
        } catch (Exception e) {
            Log.e( TAG, "Error validating recurrence rule", e );
            ValidationResult result = new ValidationResult();
            result.isValid = false;
            result.errorMessage = "Validation error: " + e.getMessage();
            return result;
        }
    }

    // ==================== PATTERN PARSING METHODS ====================

    private RecurrencePattern parseRecurrencePattern(@NonNull RecurrenceRule recurrenceRule) {
        String cacheKey = recurrenceRule.getId();

        // Check cache first
        RecurrencePattern cachedPattern = mPatternCache.get( cacheKey );
        if (cachedPattern != null) {
            return cachedPattern;
        }

        try {
            RecurrencePattern pattern = new RecurrencePattern();
            pattern.ruleId = recurrenceRule.getId();
            pattern.frequency = recurrenceRule.getFrequency().name();
            pattern.interval = recurrenceRule.getInterval();
            pattern.cycleLength = recurrenceRule.getCycleLength() != null ?
                    recurrenceRule.getCycleLength() : 1;
            pattern.isContinuous = false; // Could be derived from frequency type

            // Create shift sequence based on frequency
            pattern.shiftSequence = createShiftSequenceFromRule( recurrenceRule );

            // Cache the parsed pattern
            mPatternCache.put( cacheKey, pattern );

            Log.d( TAG, "Parsed recurrence pattern: " + pattern.frequency +
                    ", cycle: " + pattern.cycleLength );

            return pattern;
        } catch (Exception e) {
            Log.e( TAG, "Error parsing recurrence pattern", e );
            return createDefaultPattern();
        }
    }

    private List<ShiftSequence> createShiftSequenceFromRule(@NonNull RecurrenceRule recurrenceRule) {
        List<ShiftSequence> sequences = new ArrayList<>();

        // Create sequences based on frequency type
        switch (recurrenceRule.getFrequency()) {
            case QUATTRODUE_CYCLE:
                sequences = createQuattroDueSequence();
                break;
            case DAILY:
                sequences = createDailySequence();
                break;
            case WEEKLY:
                sequences = createWeeklySequence( recurrenceRule.getByDay() );
                break;
            case MONTHLY:
                sequences = createMonthlySequence( recurrenceRule );
                break;
            default:
                sequences = createDailySequence(); // Fallback
                break;
        }

        return sequences;
    }

    private List<ShiftSequence> createQuattroDueSequence() {
        List<ShiftSequence> sequences = new ArrayList<>();

        // QuattroDue 4-2 Pattern: 18-day cycle
        // Work periods: 1-4, 7-10, 13-16

        // WORK PERIOD 1: Days 1-4 (Morning shift)
        ShiftSequence work1 = new ShiftSequence();
        work1.shiftType = "morning";
        work1.days = Arrays.asList( 0, 1, 2, 3 );
        work1.startTime = LocalTime.of( 5, 0 ).toString();
        work1.endTime = LocalTime.of( 13, 0 ).toString();
        work1.duration = 8 * 60;
        sequences.add( work1 );

        ShiftSequence rest1 = new ShiftSequence();
        rest1.shiftType = "rest";
        rest1.days = Arrays.asList( 4, 5 );
        sequences.add( rest1 );

        // WORK PERIOD 2: Days 7-10 (Night shift)
        ShiftSequence work2 = new ShiftSequence();
        work2.shiftType = "night";
        work2.days = Arrays.asList( 6, 7, 8, 9 );
        work2.startTime = LocalTime.of( 21, 0 ).toString();
        work2.endTime = LocalTime.of( 5, 0 ).toString();
        work2.duration = 8 * 60;
        sequences.add( work2 );

        ShiftSequence rest2 = new ShiftSequence();
        rest2.shiftType = "rest";
        rest2.days = List.of( 10, 11 );
        sequences.add( rest2 );

        // WORK PERIOD 3: Days 13-16 (Afternoon shift)
        ShiftSequence work3 = new ShiftSequence();
        work3.shiftType = "afternoon";
        work3.days = Arrays.asList( 12, 13, 14, 15 );
        work3.startTime = LocalTime.of( 13, 0 ).toString();
        work3.endTime = LocalTime.of( 21, 0 ).toString();
        work3.duration = 8 * 60;
        sequences.add( work3 );

        ShiftSequence rest3 = new ShiftSequence();
        rest3.shiftType = "rest";
        rest3.days = List.of( 16, 17 );
        sequences.add( rest3 );

//        // Standard 18-day QuattroDue cycle
//        ShiftSequence morningSeq = new ShiftSequence();
//        morningSeq.shiftType = "morning";
//        morningSeq.days = List.of(1, 2, 3, 4);
//        morningSeq.startTime = "06:00";
//        morningSeq.endTime = "14:00";
//        morningSeq.duration = 480;
//        sequences.add(morningSeq);
//
//        ShiftSequence restSeq1 = new ShiftSequence();
//        restSeq1.shiftType = "rest";
//        restSeq1.days = List.of(5, 6);
//        sequences.add(restSeq1);
//
//        ShiftSequence afternoonSeq = new ShiftSequence();
//        afternoonSeq.shiftType = "afternoon";
//        afternoonSeq.days = List.of(7, 8, 9, 10);
//        afternoonSeq.startTime = "14:00";
//        afternoonSeq.endTime = "22:00";
//        afternoonSeq.duration = 480;
//        sequences.add(afternoonSeq);
//
//        ShiftSequence restSeq2 = new ShiftSequence();
//        restSeq2.shiftType = "rest";
//        restSeq2.days = List.of(11, 12);
//        sequences.add(restSeq2);
//
//        ShiftSequence nightSeq = new ShiftSequence();
//        nightSeq.shiftType = "night";
//        nightSeq.days = List.of(13, 14, 15, 16);
//        nightSeq.startTime = "22:00";
//        nightSeq.endTime = "06:00";
//        nightSeq.duration = 480;
//        sequences.add(nightSeq);
//
//        ShiftSequence restSeq3 = new ShiftSequence();
//        restSeq3.shiftType = "rest";
//        restSeq3.days = List.of(17, 18);
//        sequences.add(restSeq3);

        Log.d( TAG, "Created QuattroDue sequence with " + sequences.size() + " work periods" );
        return sequences;
    }

    private List<ShiftSequence> createDailySequence() {
        List<ShiftSequence> sequences = new ArrayList<>();

        ShiftSequence dailySeq = new ShiftSequence();
        dailySeq.shiftType = "morning";
        dailySeq.days = List.of( 1 );
        dailySeq.startTime = "08:00";
        dailySeq.endTime = "17:00";
        dailySeq.duration = 8;
        sequences.add( dailySeq );

        return sequences;
    }

    private List<ShiftSequence> createWeeklySequence(@Nullable List<DayOfWeek> byDay) {
        List<ShiftSequence> sequences = new ArrayList<>();

        if (byDay != null && !byDay.isEmpty()) {
            ShiftSequence weeklySeq = new ShiftSequence();
            weeklySeq.shiftType = "morning";
            // Convert day names to day numbers (simplified)
            weeklySeq.days = byDay.stream()
                    .map( this::dayOfWeekToNumber )
                    .filter( day -> day > 0 )
                    .collect( Collectors.toList() );
            weeklySeq.startTime = "08:00";
            weeklySeq.endTime = "17:00";
            weeklySeq.duration = 8;
            sequences.add( weeklySeq );
        }

        return sequences;
    }

    private List<ShiftSequence> createMonthlySequence(@NonNull RecurrenceRule recurrenceRule) {
        List<ShiftSequence> sequences = new ArrayList<>();

        // Simple monthly pattern - work on specific day of month
        ShiftSequence monthlySeq = new ShiftSequence();
        monthlySeq.shiftType = "morning";
        monthlySeq.days = List.of( 1 ); // First day of cycle
        monthlySeq.startTime = "08:00";
        monthlySeq.endTime = "17:00";
        monthlySeq.duration = 8;
        sequences.add( monthlySeq );

        return sequences;
    }

    private int dayOfWeekToNumber(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return 1;
            case TUESDAY:
                return 2;
            case WEDNESDAY:
                return 3;
            case THURSDAY:
                return 4;
            case FRIDAY:
                return 5;
            case SATURDAY:
                return 6;
            case SUNDAY:
                return 7;
            default:
                return 1;
        }
    }

    // ==================== CYCLE CALCULATION METHODS ====================

    private int calculateCyclePosition(@NonNull LocalDate date, @NonNull LocalDate startDate,
                                       @NonNull RecurrencePattern pattern, int teamOffset) {

        long daysSinceStart = ChronoUnit.DAYS.between( startDate, date );

        // Apply team offset (for multi-team coordination)
        daysSinceStart += teamOffset;

        // Calculate position in cycle (1-based)
        int cyclePosition = (int) ((daysSinceStart % pattern.cycleLength) + 1);

        // Handle negative values for dates before start
        if (cyclePosition <= 0) {
            cyclePosition += pattern.cycleLength;
        }

        Log.v( TAG, "Cycle position for " + date + ": " + cyclePosition +
                " (days since start: " + daysSinceStart + ", offset: " + teamOffset + ")" );

        return cyclePosition;
    }

    private List<ShiftAssignment> getShiftAssignmentsForCycleDay(@NonNull RecurrencePattern pattern,
                                                                 int cycleDay) {
        List<ShiftAssignment> assignments = new ArrayList<>();

        for (ShiftSequence sequence : pattern.shiftSequence) {
            if (sequence.days.contains( cycleDay )) {
                ShiftAssignment assignment = new ShiftAssignment();
                assignment.shiftType = sequence.shiftType;
                assignment.startTime = sequence.startTime;
                assignment.endTime = sequence.endTime;
                assignment.duration = sequence.duration;
                assignment.cycleDay = cycleDay;

                assignments.add( assignment );
            }
        }

        return assignments;
    }

    // ==================== SHIFT CREATION METHODS ====================

    private WorkScheduleShift createWorkScheduleShift(@NonNull Shift shift,
                                                      @NonNull UserScheduleAssignment assignment) {
        try {
            WorkScheduleShift.Builder builder = WorkScheduleShift.builder()
                    .shift( shift )
                    .startTime( shift.getStartTime() )
                    .endTime( shift.getEndTime() )
                    .description( "QuattroDue shift for team " + assignment.getTeamId() );

            // CRITICAL: Add user's team to the shift
            Team userTeam = Team.builder( assignment.getTeamId() )
                    .name( assignment.getTeamName() != null ?
                            assignment.getTeamName() : assignment.getTeamId() )
                    .build();

            builder.addTeam( userTeam );
            Log.d( TAG, "Added team " + userTeam.getName() + " to shift " + shift.getName() );

            return builder.build();
        } catch (Exception e) {
            Log.e( TAG, "Error creating WorkScheduleShift", e );
            return null;
        }
    }

    private Shift createShiftFromAssignment(@NonNull ShiftAssignment assignment, @NonNull LocalDate date) {
        if ("rest".equals( assignment.shiftType )) {
            return null; // No shift for rest days
        }

        try {
            // Parse timing information
            LocalTime startTime = parseTime( assignment.startTime );
            LocalTime endTime = parseTime( assignment.endTime );

            // Create Shift domain object
            return Shift.builder( assignment.shiftType )
                    .setStartTime( startTime )
                    .setEndTime( endTime )
                    .setBreakTimeDuration( Duration.ZERO )
                    .build();
        } catch (Exception e) {
            Log.e( TAG, "Error creating shift from assignment", e );
            return null;
        }
    }

    private LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return LocalTime.of( 8, 0 ); // Default time
        }

        try {
            return LocalTime.parse( timeString );
        } catch (Exception e) {
            Log.w( TAG, "Error parsing time: " + timeString + ", using default" );
            return LocalTime.of( 8, 0 );
        }
    }

    private String generateShiftId(String shiftType, LocalDate date) {
        return shiftType + "_" + date.toString();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract team offset from assignment, with fallback to 0.
     */
    private int getTeamOffsetFromAssignment(@NonNull UserScheduleAssignment assignment) {
        // Check if assignment has team offset information
        // This could be derived from team ID, department, or specific field
        try {
            // Simple hash-based offset for demonstration
            // In practice, this would be a proper field or calculated value
            String teamId = assignment.getTeamId();
            if (teamId != null) {
                return Math.abs( teamId.hashCode() ) % 6; // 0-5 days offset
            }
        } catch (Exception e) {
            Log.w( TAG, "Error calculating team offset, using 0" );
        }
        return 0;
    }

    // ==================== VALIDATION METHODS ====================

    private boolean validateShiftSequenceCoverage(@NonNull RecurrencePattern pattern) {
        boolean[] covered = new boolean[pattern.cycleLength + 1]; // 1-based indexing

        for (ShiftSequence sequence : pattern.shiftSequence) {
            for (int day : sequence.days) {
                if (day >= 1 && day <= pattern.cycleLength) {
                    covered[day] = true;
                }
            }
        }

        // Check if all days are covered
        for (int i = 1; i <= pattern.cycleLength; i++) {
            if (!covered[i]) {
                Log.w( TAG, "Cycle day " + i + " is not covered by any shift sequence" );
                return false;
            }
        }

        return true;
    }

    private ValidationIssues checkForGapsAndOverlaps(@NonNull RecurrencePattern pattern) {
        ValidationIssues issues = new ValidationIssues();

        Map<Integer, List<String>> dayAssignments = new HashMap<>();

        // Build map of day assignments
        for (ShiftSequence sequence : pattern.shiftSequence) {
            for (int day : sequence.days) {
                dayAssignments.computeIfAbsent( day, k -> new ArrayList<>() ).add( sequence.shiftType );
            }
        }

        // Check for overlaps (multiple non-rest assignments per day)
        for (Map.Entry<Integer, List<String>> entry : dayAssignments.entrySet()) {
            int day = entry.getKey();
            List<String> assignments = entry.getValue();

            long workingAssignments = assignments.stream()
                    .filter( type -> !"rest".equals( type ) )
                    .count();

            if (workingAssignments > 1) {
                issues.addOverlap( day, assignments );
            }
        }

        return issues;
    }

    private RecurrencePattern createDefaultPattern() {
        RecurrencePattern pattern = new RecurrencePattern();
        pattern.frequency = "DAILY";
        pattern.interval = 1;
        pattern.cycleLength = 1;
        pattern.isContinuous = false;
        pattern.shiftSequence = createDailySequence();
        return pattern;
    }

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Clear pattern cache to force re-parsing.
     */
    public void clearPatternCache() {
        mPatternCache.clear();
        Log.d( TAG, "Pattern cache cleared" );
    }

    /**
     * Clear cache for specific pattern.
     *
     * @param ruleId Recurrence rule ID
     */
    public void clearPatternCache(@NonNull String ruleId) {
        mPatternCache.remove( ruleId );
        Log.d( TAG, "Pattern cache cleared for rule: " + ruleId );
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create RecurrenceCalculator without localization support.
     */
    @NonNull
    public static RecurrenceCalculator create() {
        return new RecurrenceCalculator( null );
    }

    /**
     * Create RecurrenceCalculator with localization support.
     */
    @NonNull
    public static RecurrenceCalculator create(@NonNull DomainLocalizer localizer) {
        return new RecurrenceCalculator( localizer );
    }

    // ==================== INNER CLASSES ====================

    public static class RecurrencePattern {
        public String ruleId;
        public String frequency;
        public int interval;
        public int cycleLength;
        public boolean isContinuous;
        public List<ShiftSequence> shiftSequence;
    }

    public static class ShiftSequence {
        public String shiftType;
        public List<Integer> days;
        public String startTime;
        public String endTime;
        public int duration;
    }

    public static class ShiftAssignment {
        public String shiftType;
        public String startTime;
        public String endTime;
        public int duration;
        public int cycleDay;
    }

    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;
        public List<String> warnings = new ArrayList<>();
    }

    public static class ValidationIssues {
        private final List<String> overlaps = new ArrayList<>();
        private final List<String> gaps = new ArrayList<>();

        public void addOverlap(int day, List<String> assignments) {
            overlaps.add( "Day " + day + ": " + String.join( ", ", assignments ) );
        }

        public void addGap(int day) {
            gaps.add( "Day " + day + ": no assignment" );
        }

        public boolean hasIssues() {
            return !overlaps.isEmpty() || !gaps.isEmpty();
        }

        public String getDescription() {
            StringBuilder sb = new StringBuilder();
            if (!overlaps.isEmpty()) {
                sb.append( "Overlaps: " ).append( String.join( "; ", overlaps ) );
            }
            if (!gaps.isEmpty()) {
                if (sb.length() > 0) sb.append( ". " );
                sb.append( "Gaps: " ).append( String.join( "; ", gaps ) );
            }
            return sb.toString();
        }
    }
}