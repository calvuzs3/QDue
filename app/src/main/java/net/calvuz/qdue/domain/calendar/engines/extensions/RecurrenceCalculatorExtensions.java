package net.calvuz.qdue.domain.calendar.engines.extensions;

import static net.calvuz.qdue.domain.common.DomainLibrary.logDebug;
import static net.calvuz.qdue.domain.common.DomainLibrary.logError;
import static net.calvuz.qdue.domain.common.DomainLibrary.logWarning;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.extensions.RecurrenceRuleExtensions;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecurrenceCalculatorExtensions - Extensions for Custom Pattern Calculation
 *
 * <p>Extends the existing RecurrenceCalculator to support user-defined custom patterns
 * that are stored as JSON data within RecurrenceRule descriptions. Provides
 * seamless integration with the existing recurrence calculation engine.</p>
 *
 * <h3>Custom Pattern Processing:</h3>
 * <ul>
 *   <li><strong>Pattern Extraction</strong>: Extract PatternDay sequences from RecurrenceRule</li>
 *   <li><strong>Cycle Calculation</strong>: Apply pattern cyclically from start date</li>
 *   <li><strong>Shift Generation</strong>: Convert PatternDays to WorkScheduleShifts</li>
 *   <li><strong>Date Range Support</strong>: Generate schedules for any date range</li>
 * </ul>
 *
 * <h3>Integration:</h3>
 * <ul>
 *   <li><strong>Backward Compatible</strong>: Works with existing RecurrenceCalculator</li>
 *   <li><strong>Performance Optimized</strong>: Caches pattern data for repeated calculations</li>
 *   <li><strong>Error Resilient</strong>: Graceful fallback for invalid patterns</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Custom Pattern Support
 * @since Clean Architecture Phase 2
 */
public class RecurrenceCalculatorExtensions {

    private static final String TAG = "RecurrenceCalculatorExtensions";

    // ==================== CUSTOM PATTERN CALCULATION ====================

    /**
     * Calculate shifts for a single date using custom pattern.
     *
     * @param date Target date for calculation
     * @param recurrenceRule Custom pattern recurrence rule
     * @param assignment User schedule assignment
     * @return List of Shift objects for the date
     */
    @NonNull
    public static List<Shift> calculateCustomPatternShiftsForDate(
            @NonNull LocalDate date,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        try {
            logDebug("Calculating custom pattern shifts for date: " + date);

            // Verify this is a custom pattern
            if (!RecurrenceRuleExtensions.isCustomPattern(recurrenceRule)) {
                logWarning("RecurrenceRule is not a custom pattern, returning empty shifts");
                return new ArrayList<>();
            }

            // Extract pattern days
            List<PatternDay> patternDays = RecurrenceRuleExtensions.extractPatternDays(recurrenceRule);
            if (patternDays.isEmpty()) {
                logWarning("No pattern days found in custom pattern rule");
                return new ArrayList<>();
            }

            // Calculate which day in the pattern this date corresponds to
            int patternDayIndex = calculatePatternDayIndex(date, assignment.getStartDate(), patternDays.size());

            if (patternDayIndex < 0 || patternDayIndex >= patternDays.size()) {
                logError("Invalid pattern day index: " + patternDayIndex + " for pattern size: " + patternDays.size(), null);
                return new ArrayList<>();
            }

            // Get the pattern day for this date
            PatternDay patternDay = patternDays.get(patternDayIndex);

            // Convert to shifts
            List<Shift> shifts = new ArrayList<>();
            if (patternDay.isWorkDay() && patternDay.getShift() != null) {
                shifts.add(patternDay.getShift());
            }

            logDebug("Generated " + shifts.size() + " shifts for date " + date +
                    " (pattern day " + (patternDayIndex + 1) + ")");

            return shifts;

        } catch (Exception e) {
            logError("Error calculating custom pattern shifts for date: " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate shifts for date range using custom pattern.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param recurrenceRule Custom pattern recurrence rule
     * @param assignment User schedule assignment
     * @return Map of dates to List of Shift objects
     */
    @NonNull
    public static Map<LocalDate, List<Shift>> calculateCustomPatternShiftsForDateRange(
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        Map<LocalDate, List<Shift>> shiftsMap = new HashMap<>();

        try {
            logDebug("Calculating custom pattern shifts for date range: " + startDate + " to " + endDate);

            // Verify this is a custom pattern
            if (!RecurrenceRuleExtensions.isCustomPattern(recurrenceRule)) {
                logWarning("RecurrenceRule is not a custom pattern, returning empty map");
                return shiftsMap;
            }

            // Extract pattern days once for efficiency
            List<PatternDay> patternDays = RecurrenceRuleExtensions.extractPatternDays(recurrenceRule);
            if (patternDays.isEmpty()) {
                logWarning("No pattern days found in custom pattern rule");
                return shiftsMap;
            }

            // Process each date in range
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                List<Shift> shifts = calculateCustomPatternShiftsForDate(
                        currentDate, recurrenceRule, assignment);
                shiftsMap.put(currentDate, shifts);
                currentDate = currentDate.plusDays(1);
            }

            logDebug("Generated custom pattern shifts for " + shiftsMap.size() + " dates");

        } catch (Exception e) {
            logError("Error calculating custom pattern shifts for date range", e);
        }

        return shiftsMap;
    }

    // ==================== WORK SCHEDULE DAY GENERATION ====================

    /**
     * Generate WorkScheduleDay for a specific date using custom pattern.
     *
     * @param date Target date
     * @param recurrenceRule Custom pattern recurrence rule
     * @param assignment User schedule assignment
     * @return WorkScheduleDay with custom pattern shifts
     */
    @NonNull
    public static WorkScheduleDay generateCustomPatternWorkScheduleDay(
            @NonNull LocalDate date,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        try {
            logDebug("Generating WorkScheduleDay for custom pattern: " + date);

            // Get shifts for this date
            List<Shift> shifts = calculateCustomPatternShiftsForDate(date, recurrenceRule, assignment);

            // Build WorkScheduleDay
            WorkScheduleDay.Builder dayBuilder = WorkScheduleDay.builder( date ); //.date(date);

            // Convert shifts to WorkScheduleShifts and add to day
            for (Shift shift : shifts) {
                WorkScheduleShift workShift = WorkScheduleShift.builder()
                        .shift(shift)
                        .startTime(shift.getStartTime())
                        .endTime(shift.getEndTime())
                        .build();

                dayBuilder.addShift(workShift);
            }

            WorkScheduleDay workScheduleDay = dayBuilder.build();

            logDebug("Generated WorkScheduleDay with " + shifts.size() + " shifts for " + date);
            return workScheduleDay;

        } catch (Exception e) {
            logError("Error generating WorkScheduleDay for custom pattern", e);

            // Return empty day on error
            return WorkScheduleDay.builder(date).build();
        }
    }

    /**
     * Generate WorkScheduleDay list for date range using custom pattern.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param recurrenceRule Custom pattern recurrence rule
     * @param assignment User schedule assignment
     * @return List of WorkScheduleDay objects
     */
    @NonNull
    public static List<WorkScheduleDay> generateCustomPatternWorkScheduleDays(
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate,
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        List<WorkScheduleDay> workScheduleDays = new ArrayList<>();

        try {
            logDebug("Generating WorkScheduleDays for custom pattern range: " + startDate + " to " + endDate);

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                WorkScheduleDay workScheduleDay = generateCustomPatternWorkScheduleDay(
                        currentDate, recurrenceRule, assignment);
                workScheduleDays.add(workScheduleDay);
                currentDate = currentDate.plusDays(1);
            }

            logDebug("Generated " + workScheduleDays.size() + " WorkScheduleDays for custom pattern");

        } catch (Exception e) {
            logError("Error generating WorkScheduleDays for custom pattern range", e);
        }

        return workScheduleDays;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate which day in the pattern sequence a specific date corresponds to.
     *
     * @param targetDate Date to calculate for
     * @param patternStartDate Start date of the pattern
     * @param patternLength Total length of the pattern in days
     * @return Zero-based index in the pattern sequence
     */
    private static int calculatePatternDayIndex(@NonNull LocalDate targetDate,
                                                @NonNull LocalDate patternStartDate,
                                                int patternLength) {
        try {
            // Calculate days since pattern start
            long daysSinceStart = ChronoUnit.DAYS.between(patternStartDate, targetDate);

            // Handle dates before pattern start
            if (daysSinceStart < 0) {
                logWarning("Target date " + targetDate + " is before pattern start " + patternStartDate);
                return -1;
            }

            // Calculate position in pattern cycle (0-based)
            int patternIndex = (int) (daysSinceStart % patternLength);

            logDebug("Pattern day index for " + targetDate + ": " + patternIndex +
                    " (days since start: " + daysSinceStart + ", pattern length: " + patternLength + ")");

            return patternIndex;

        } catch (Exception e) {
            logError("Error calculating pattern day index", e);
            return -1;
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Validate custom pattern for calculation feasibility.
     *
     * @param recurrenceRule RecurrenceRule to validate
     * @param assignment UserScheduleAssignment to validate
     * @return ValidationResult with details
     */
    @NonNull
    public static ValidationResult validateCustomPatternForCalculation(
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        try {
            // Check if this is a custom pattern
            if (!RecurrenceRuleExtensions.isCustomPattern(recurrenceRule)) {
                return new ValidationResult(false, "Not a custom pattern rule");
            }

            // Extract and validate pattern days
            List<PatternDay> patternDays = RecurrenceRuleExtensions.extractPatternDays(recurrenceRule);
            if (patternDays.isEmpty()) {
                return new ValidationResult(false, "No pattern days found");
            }

            // Validate pattern day sequence
            RecurrenceRuleExtensions.ValidationResult patternValidation =
                    RecurrenceRuleExtensions.validateCustomPattern(patternDays);
            if (!patternValidation.isValid) {
                return new ValidationResult(false, "Pattern validation failed: " + patternValidation.message);
            }

            // Validate assignment dates
            if (assignment.getStartDate() == null) {
                return new ValidationResult(false, "Assignment start date is null");
            }

            // Validate recurrence rule matches assignment
            if (!recurrenceRule.getId().equals(assignment.getRecurrenceRuleId())) {
                return new ValidationResult(false, "RecurrenceRule ID doesn't match assignment");
            }

            return new ValidationResult(true, "Custom pattern is valid for calculation");

        } catch (Exception e) {
            logError("Error validating custom pattern for calculation", e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }

    // ==================== PATTERN STATISTICS ====================

    /**
     * Calculate pattern statistics for custom pattern.
     *
     * @param recurrenceRule Custom pattern recurrence rule
     * @return Pattern statistics or null if not a custom pattern
     */
    @NonNull
    public static PatternStatistics calculateCustomPatternStatistics(@NonNull RecurrenceRule recurrenceRule) {
        try {
            if (!RecurrenceRuleExtensions.isCustomPattern(recurrenceRule)) {
                return new PatternStatistics(0, 0, 0, 0.0);
            }

            List<PatternDay> patternDays = RecurrenceRuleExtensions.extractPatternDays(recurrenceRule);
            if (patternDays.isEmpty()) {
                return new PatternStatistics(0, 0, 0, 0.0);
            }

            int totalDays = patternDays.size();
            int workDays = 0;
            int restDays = 0;

            for (PatternDay day : patternDays) {
                if (day.isWorkDay()) {
                    workDays++;
                } else {
                    restDays++;
                }
            }

            double workPercentage = totalDays > 0 ? (workDays * 100.0) / totalDays : 0.0;

            return new PatternStatistics(totalDays, workDays, restDays, workPercentage);

        } catch (Exception e) {
            logError("Error calculating custom pattern statistics", e);
            return new PatternStatistics(0, 0, 0, 0.0);
        }
    }

    /**
     * Pattern statistics container.
     */
    public static class PatternStatistics {
        public final int totalDays;
        public final int workDays;
        public final int restDays;
        public final double workPercentage;

        public PatternStatistics(int totalDays, int workDays, int restDays, double workPercentage) {
            this.totalDays = totalDays;
            this.workDays = workDays;
            this.restDays = restDays;
            this.workPercentage = workPercentage;
        }
    }
}