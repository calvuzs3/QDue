package net.calvuz.qdue.smartshifts.domain.generators;

import android.content.Context;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;
import net.calvuz.qdue.smartshifts.domain.models.RecurrenceRule;
import net.calvuz.qdue.smartshifts.domain.models.ShiftInfo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Core engine for generating shift events from patterns
 * Handles complex recurrence rules and team offset calculations
 */
@Singleton
public class ShiftGeneratorEngine {

    private final ShiftTypeDao shiftTypeDao;
    private final ShiftPatternDao patternDao;
    private final RecurrenceRuleParser ruleParser;
    private final Context context;

    @Inject
    public ShiftGeneratorEngine(
            ShiftTypeDao shiftTypeDao,
            ShiftPatternDao patternDao,
            RecurrenceRuleParser ruleParser,
            Context context
    ) {
        this.shiftTypeDao = shiftTypeDao;
        this.patternDao = patternDao;
        this.ruleParser = ruleParser;
        this.context = context;
    }

    /**
     * Generate shift events for user in specific period
     */
    public List<SmartShiftEvent> generateShiftsForPeriod(
            String userId,
            String shiftPatternId,
            LocalDate startDate,
            LocalDate endDate,
            int cycleOffsetDays
    ) {

        ShiftPattern pattern = patternDao.getPatternById(shiftPatternId);
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern not found: " + shiftPatternId);
        }

        RecurrenceRule rule = ruleParser.parseRecurrenceRule(pattern.recurrenceRuleJson);
        List<SmartShiftEvent> events = new ArrayList<>();

        // Calculate cycle start date (reference point)
        LocalDate cycleStart = calculateCycleStart(pattern, startDate);

        // Generate events for each day in the period
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            // Calculate which day in the cycle this represents
            long daysSinceCycleStart = ChronoUnit.DAYS.between(cycleStart, date);
            long adjustedDays = daysSinceCycleStart - cycleOffsetDays;
            int cycleDayNumber = (int) ((adjustedDays % rule.getCycleLength()) + rule.getCycleLength()) % rule.getCycleLength() + 1;

            // Get shift info for this cycle day
            ShiftInfo shiftInfo = rule.getShiftForDay(cycleDayNumber);

            if (shiftInfo != null && !shiftInfo.getShiftType().equals("rest")) {
                SmartShiftEvent event = createShiftEvent(
                        userId,
                        pattern,
                        shiftInfo,
                        date,
                        cycleDayNumber
                );
                events.add(event);
            }
        }

        return events;
    }

    /**
     * Calculate cycle start date based on pattern and reference date
     */
    private LocalDate calculateCycleStart(ShiftPattern pattern, LocalDate referenceDate) {
        // For predefined patterns, use a fixed reference date
        if (pattern.isPredefined) {
            return LocalDate.of(2025, 1, 6); // Monday, January 6, 2025
        }

        // For custom patterns, align to nearest cycle boundary
        return referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1);
    }

    /**
     * Create shift event from shift info
     */
    private SmartShiftEvent createShiftEvent(
            String userId,
            ShiftPattern pattern,
            ShiftInfo shiftInfo,
            LocalDate date,
            int cycleDayNumber
    ) {

        ShiftType shiftType = shiftTypeDao.getShiftTypeById(shiftInfo.getShiftType());
        if (shiftType == null) {
            throw new IllegalArgumentException("Shift type not found: " + shiftInfo.getShiftType());
        }

        SmartShiftEvent event = new SmartShiftEvent();
        event.id = UUID.randomUUID().toString();
        event.eventType = "instance";
        event.userId = userId;
        event.shiftPatternId = pattern.id;
        event.shiftTypeId = shiftType.id;
        event.eventDate = date.toString();
        event.startTime = shiftType.startTime;
        event.endTime = shiftType.endTime;
        event.cycleDayNumber = cycleDayNumber;
        event.status = "active";
        event.generatedAt = System.currentTimeMillis();
        event.updatedAt = System.currentTimeMillis();

        return event;
    }

    /**
     * Generate team assignments for continuous cycle patterns
     */
    public List<TeamAssignmentResult> generateTeamAssignments(
            String basePatternId,
            LocalDate startDate,
            int numberOfTeams
    ) {

        ShiftPattern basePattern = patternDao.getPatternById(basePatternId);
        if (basePattern == null || !basePattern.isContinuousCycle) {
            throw new IllegalArgumentException("Pattern not suitable for team generation");
        }

        List<TeamAssignmentResult> assignments = new ArrayList<>();

        for (int teamIndex = 0; teamIndex < numberOfTeams; teamIndex++) {
            int offsetDays = calculateTeamOffset(teamIndex, basePattern.cycleLengthDays);

            TeamAssignmentResult assignment = new TeamAssignmentResult();
            assignment.teamName = context.getString(
                    R.string.team_name_format,
                    String.valueOf((char)('A' + teamIndex))
            );
            assignment.offsetDays = offsetDays;
            assignment.teamColorHex = generateTeamColor(teamIndex);

            assignments.add(assignment);
        }

        return assignments;
    }

    /**
     * Calculate team offset based on cycle length
     */
    private int calculateTeamOffset(int teamIndex, int cycleLength) {
        // For 18-day cycle: 0, 2, 4, 6, 8, 10, 12, 14, 16
        // For 15-day cycle: 0, 3, 6, 9, 12
        if (cycleLength == 18) {
            return teamIndex * 2;
        } else if (cycleLength == 15) {
            return teamIndex * 3;
        } else {
            return teamIndex; // Default increment
        }
    }

    /**
     * Generate color for team based on index
     */
    private String generateTeamColor(int teamIndex) {
        String[] colors = {
                "#4CAF50", // Green
                "#2196F3", // Blue
                "#FF9800", // Orange
                "#9C27B0", // Purple
                "#F44336", // Red
                "#00BCD4", // Cyan
                "#FFEB3B", // Yellow
                "#795548", // Brown
                "#607D8B"  // Blue Grey
        };

        return colors[teamIndex % colors.length];
    }

    /**
     * Validate pattern for shift generation
     */
    public PatternValidationResult validatePattern(String recurrenceRuleJson) {
        try {
            RecurrenceRule rule = ruleParser.parseRecurrenceRule(recurrenceRuleJson);

            PatternValidationResult result = new PatternValidationResult();
            result.isValid = true;
            result.isContinuousCycle = validateContinuousCycle(rule);
            result.maxTeams = calculateMaxTeams(rule);
            result.workingDaysPerCycle = calculateWorkingDays(rule);

            return result;
        } catch (Exception e) {
            PatternValidationResult result = new PatternValidationResult();
            result.isValid = false;
            result.errorMessage = e.getMessage();
            return result;
        }
    }

    /**
     * Validate if pattern supports continuous cycle
     * PLACEHOLDER - will be implemented in future phase
     */
    private boolean validateContinuousCycle(RecurrenceRule rule) {
        // TODO: Implement continuous cycle validation logic
        // For now, return true for predefined patterns
        return rule.getCycleLength() >= 7;
    }

    /**
     * Calculate maximum teams supported by pattern
     */
    private int calculateMaxTeams(RecurrenceRule rule) {
        int cycleLength = rule.getCycleLength();

        if (cycleLength == 18) return 9;
        if (cycleLength == 15) return 5;
        if (cycleLength == 7) return 1;

        return Math.max(1, cycleLength / 2);
    }

    /**
     * Calculate working days per cycle
     */
    private int calculateWorkingDays(RecurrenceRule rule) {
        int workingDays = 0;

        for (int day = 1; day <= rule.getCycleLength(); day++) {
            ShiftInfo shiftInfo = rule.getShiftForDay(day);
            if (shiftInfo != null && !shiftInfo.getShiftType().equals("rest")) {
                workingDays++;
            }
        }

        return workingDays;
    }

    // ===== RESULT CLASSES =====

    /**
     * Result class for team assignment generation
     */
    public static class TeamAssignmentResult {
        public String teamName;
        public int offsetDays;
        public String teamColorHex;
    }

    /**
     * Result class for pattern validation
     */
    public static class PatternValidationResult {
        public boolean isValid;
        public boolean isContinuousCycle;
        public int maxTeams;
        public int workingDaysPerCycle;
        public String errorMessage;
    }
}

// =====================================================================

