package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.domain.models.ShiftInfo;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationResult;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationError;
import net.calvuz.qdue.smartshifts.utils.validators.MultiValidationResult;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationCheck;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Validation helper specifically for shift domain operations.
 * <p>
 * Handles validation of:
 * - User-created shift configurations
 * - Shift patterns and cycles
 * - Team configurations
 * - Work-life balance assessment
 * - Fatigue risk analysis
 * - Continuous operation validation
 * <p>
 * All validation methods return ValidationResult with error codes for i18n support.
 * Internal messages are in English for logging purposes.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class ShiftValidationHelper {

    private static final String TAG = "ShiftValidationHelper";

    // Constants for validation limits
    private static final int MAX_SHIFT_TYPES = 4;
    private static final int MIN_SHIFT_DURATION_MINUTES = 60; // 1 hour
    private static final int MAX_SHIFT_DURATION_MINUTES = 12 * 60; // 12 hours
    private static final int MAX_SHIFT_DESCRIPTION_LENGTH = 200;
    private static final int MAX_CONSECUTIVE_WORK_DAYS = 7;
    private static final double MAX_WORK_RATIO = 0.8; // 80%
    private static final double MAX_HOURS_PER_WORK_DAY = 10.0;
    private static final double MAX_NIGHT_SHIFT_RATIO = 0.5; // 50%
    private static final double MAX_LONG_SHIFT_RATIO = 0.3; // 30%
    private static final int MIN_TEAM_COUNT = 1;
    private static final int MAX_TEAM_COUNT = 20;

    // Private constructor to prevent instantiation
    private ShiftValidationHelper() {
        throw new UnsupportedOperationException("ShiftValidationHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // SHIFT CONFIGURATION VALIDATION
    // ============================================

    /**
     * Validate user-created shift configuration
     * Handles up to 4 shift types, each with custom duration and times
     */
    @NonNull
    public static ValidationResult validateShiftConfiguration(
            @NonNull String shiftName,
            @NonNull String startTime,
            @NonNull String endTime,
            @Nullable String description,
            @Nullable String colorHex
    ) {
        // Validate shift name using existing pattern validation
        if (!ValidationHelper.isValidPatternName(shiftName)) {
            return ValidationResult.error(ValidationError.SHIFT_INVALID_NAME, "shiftName");
        }

        // Validate time formats
        if (!ValidationHelper.isValidTimeFormat(startTime)) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT, "startTime");
        }

        if (!ValidationHelper.isValidTimeFormat(endTime)) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT, "endTime");
        }

        // Validate time range
        if (!ValidationHelper.isValidTimeRange(startTime, endTime)) {
            return ValidationResult.error(ValidationError.TIME_RANGE_IDENTICAL, "timeRange");
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);

            // Validate shift duration
            int durationMinutes = DateTimeHelper.calculateShiftDurationMinutes(start, end);
            if (durationMinutes < MIN_SHIFT_DURATION_MINUTES) {
                return ValidationResult.error(ValidationError.SHIFT_DURATION_TOO_SHORT, durationMinutes, MIN_SHIFT_DURATION_MINUTES);
            }
            if (durationMinutes > MAX_SHIFT_DURATION_MINUTES) {
                return ValidationResult.error(ValidationError.SHIFT_DURATION_TOO_LONG, durationMinutes, MAX_SHIFT_DURATION_MINUTES);
            }

            // Validate optional description length
            if (StringHelper.isNotEmpty(description) && description.trim().length() > MAX_SHIFT_DESCRIPTION_LENGTH) {
                return ValidationResult.error(ValidationError.SHIFT_DESCRIPTION_TOO_LONG, "description", MAX_SHIFT_DESCRIPTION_LENGTH);
            }

            // Validate optional color hex format
            if (StringHelper.isNotEmpty(colorHex)) {
                try {
                    ColorHelper.parseColor(colorHex);
                } catch (Exception e) {
                    return ValidationResult.error(ValidationError.SHIFT_INVALID_COLOR, "colorHex");
                }
            }

            return ValidationResult.success("Shift configuration validation passed");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SHIFT_CONFIGURATION_INVALID, e.getMessage());
        }
    }

    /**
     * Validate that shift types don't overlap in time coverage
     */
    @NonNull
    public static ValidationResult validateShiftTypesNoOverlap(@NonNull List<ShiftType> shiftTypes) {
        if (shiftTypes.size() > MAX_SHIFT_TYPES) {
            return ValidationResult.error(ValidationError.SHIFT_TOO_MANY_TYPES, shiftTypes.size(), MAX_SHIFT_TYPES);
        }

        // Check for time overlaps between shift types
        for (int i = 0; i < shiftTypes.size(); i++) {
            for (int j = i + 1; j < shiftTypes.size(); j++) {
                ShiftType shift1 = shiftTypes.get(i);
                ShiftType shift2 = shiftTypes.get(j);

                try {
                    LocalTime start1 = DateTimeHelper.parseTime(shift1.getStartTime());
                    LocalTime end1 = DateTimeHelper.parseTime(shift1.getEndTime());
                    LocalTime start2 = DateTimeHelper.parseTime(shift2.getStartTime());
                    LocalTime end2 = DateTimeHelper.parseTime(shift2.getEndTime());

                    if (DateTimeHelper.doTimeRangesOverlap(start1, end1, start2, end2)) {
                        return ValidationResult.error(ValidationError.SHIFT_TIME_OVERLAP,
                                shift1.getName(), shift2.getName());
                    }
                } catch (Exception e) {
                    return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                            "Error validating shift time overlap: " + e.getMessage());
                }
            }
        }

        return ValidationResult.success("No shift time overlaps detected");
    }

    // ============================================
    // SHIFT INFO UTILITY METHODS
    // ============================================

    /**
     * Get start time from ShiftInfo using shift type lookup
     */
    @Nullable
    public static String getStartTime(@NonNull ShiftInfo shiftInfo, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return null;
        }

        ShiftType shiftType = shiftTypeMap.get(shiftInfo.getShiftType());
        return shiftType != null ? shiftType.getStartTime() : null;
    }

    /**
     * Get end time from ShiftInfo using shift type lookup
     */
    @Nullable
    public static String getEndTime(@NonNull ShiftInfo shiftInfo, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return null;
        }

        ShiftType shiftType = shiftTypeMap.get(shiftInfo.getShiftType());
        return shiftType != null ? shiftType.getEndTime() : null;
    }

    // ============================================
    // CONTINUOUS CYCLE VALIDATION
    // ============================================

    /**
     * Validate that pattern provides continuous cycle coverage
     * Ensures no gaps in 24-hour coverage when teams rotate
     */
    @NonNull
    public static ValidationResult validateContinuousCycleStructure(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount
    ) {
        if (patternDays.isEmpty()) {
            return ValidationResult.error(ValidationError.PATTERN_EMPTY);
        }

        if (teamCount < MIN_TEAM_COUNT || teamCount > MAX_TEAM_COUNT) {
            return ValidationResult.error(ValidationError.TEAM_COUNT_TOO_LOW, teamCount, MIN_TEAM_COUNT, MAX_TEAM_COUNT);
        }

        // Create 24-hour timeline (1440 minutes)
        boolean[] timeline = new boolean[1440];

        try {
            // Check coverage for each day in pattern
            for (ShiftInfo shiftInfo : patternDays) {
                if (!shiftInfo.isRestPeriod()) {
                    String startTimeStr = getStartTime(shiftInfo, shiftTypeMap);
                    String endTimeStr = getEndTime(shiftInfo, shiftTypeMap);

                    if (startTimeStr == null || endTimeStr == null) {
                        return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                                shiftInfo.getShiftType());
                    }

                    LocalTime startTime = DateTimeHelper.parseTime(startTimeStr);
                    LocalTime endTime = DateTimeHelper.parseTime(endTimeStr);

                    // Mark timeline coverage
                    DateTimeHelper.TimeRange range = DateTimeHelper.createTimeRange(startTime, endTime);
                    for (int minute = range.startMinutes; minute < range.endMinutes; minute++) {
                        int timelineIndex = minute % 1440; // Handle day overflow
                        timeline[timelineIndex] = true;
                    }
                }
            }

            // Check for gaps in coverage
            int gapStart = -1;
            for (int minute = 0; minute < 1440; minute++) {
                if (!timeline[minute]) {
                    if (gapStart == -1) {
                        gapStart = minute;
                    }
                } else {
                    if (gapStart != -1) {
                        // Found end of gap
                        LocalTime gapStartTime = DateTimeHelper.minutesSinceMidnightToTime(gapStart);
                        LocalTime gapEndTime = DateTimeHelper.minutesSinceMidnightToTime(minute);
                        return ValidationResult.error(ValidationError.PATTERN_COVERAGE_GAP,
                                DateTimeHelper.formatTime(gapStartTime),
                                DateTimeHelper.formatTime(gapEndTime));
                    }
                }
            }

            // Check if gap extends to end of day
            if (gapStart != -1) {
                LocalTime gapStartTime = DateTimeHelper.minutesSinceMidnightToTime(gapStart);
                return ValidationResult.error(ValidationError.PATTERN_COVERAGE_GAP,
                        DateTimeHelper.formatTime(gapStartTime), "24:00");
            }

            return ValidationResult.success("Continuous 24h coverage confirmed");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.PATTERN_INVALID_CYCLE_LENGTH,
                    "Error validating continuous coverage: " + e.getMessage());
        }
    }

    // ============================================
    // TEAM CONFIGURATION VALIDATION
    // ============================================

    /**
     * Get recommended team count based on pattern characteristics
     */
    public static int getRecommendedTeamCount(@NonNull List<ShiftInfo> patternDays, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (patternDays.isEmpty()) return 1;

        int cycleLength = patternDays.size();
        int workingDays = 0;

        // Calculate working days
        for (ShiftInfo shiftInfo : patternDays) {
            if (!shiftInfo.isRestPeriod()) {
                workingDays++;
            }
        }

        // Recommend based on cycle characteristics
        if (cycleLength <= 7) {
            return 1; // Weekly patterns typically need 1 team
        } else if (cycleLength <= 14) {
            return Math.max(2, workingDays / 7); // Bi-weekly patterns
        } else {
            return Math.max(2, cycleLength / 7); // Longer cycles
        }
    }

    /**
     * Validate team configuration against pattern requirements
     */
    @NonNull
    public static ValidationResult validateTeamConfiguration(
            int teamCount,
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (teamCount < MIN_TEAM_COUNT) {
            return ValidationResult.error(ValidationError.TEAM_COUNT_TOO_LOW, teamCount, MIN_TEAM_COUNT);
        }

        if (teamCount > MAX_TEAM_COUNT) {
            return ValidationResult.error(ValidationError.TEAM_COUNT_TOO_HIGH, teamCount, MAX_TEAM_COUNT);
        }

        int cycleLength = patternDays.size();
        int recommendedTeams = getRecommendedTeamCount(patternDays, shiftTypeMap);

        // Check if team count is reasonable for cycle length
        if (teamCount > cycleLength) {
            return ValidationResult.error(ValidationError.TEAM_COUNT_INAPPROPRIATE,
                    teamCount, cycleLength);
        }

        // Warning if significantly different from recommendation
        if (teamCount < recommendedTeams / 2) {
            return ValidationResult.warning(ValidationError.TEAM_COUNT_INAPPROPRIATE,
                    teamCount, recommendedTeams, "insufficient");
        }

        if (teamCount > recommendedTeams * 2) {
            return ValidationResult.warning(ValidationError.TEAM_COUNT_INAPPROPRIATE,
                    teamCount, recommendedTeams, "excessive");
        }

        return ValidationResult.success("Team configuration is appropriate");
    }

    /**
     * Validate team offset configuration
     */
    @NonNull
    public static ValidationResult validateTeamOffsets(
            @NonNull List<Integer> teamOffsets,
            int cycleLength
    ) {
        if (teamOffsets.isEmpty()) {
            return ValidationResult.error(ValidationError.TEAM_NO_OFFSETS_DEFINED);
        }

        Set<Integer> uniqueOffsets = new HashSet<>();

        for (int i = 0; i < teamOffsets.size(); i++) {
            int offset = teamOffsets.get(i);

            // Validate offset range
            if (!ValidationHelper.isValidOffsetDays(offset, cycleLength)) {
                return ValidationResult.error(ValidationError.TEAM_INVALID_OFFSET,
                        i + 1, offset, 0, cycleLength - 1);
            }

            // Check for duplicate offsets
            if (uniqueOffsets.contains(offset)) {
                return ValidationResult.error(ValidationError.TEAM_DUPLICATE_OFFSET, offset);
            }
            uniqueOffsets.add(offset);
        }

        return ValidationResult.success("Team offsets are valid");
    }

    // ============================================
    // WORK-LIFE BALANCE VALIDATION
    // ============================================

    /**
     * Validate work-life balance characteristics of pattern
     */
    @NonNull
    public static ValidationResult isValidWorkLifeBalance(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (patternDays.isEmpty()) {
            return ValidationResult.error(ValidationError.PATTERN_EMPTY);
        }

        int workingDays = 0;
        int restDays = 0;
        int consecutiveWorkDays = 0;
        int maxConsecutiveWork = 0;
        double totalWorkingHours = 0;

        // Analyze pattern characteristics
        for (ShiftInfo shiftInfo : patternDays) {
            if (shiftInfo.isRestPeriod()) {
                restDays++;
                consecutiveWorkDays = 0;
            } else {
                workingDays++;
                consecutiveWorkDays++;
                maxConsecutiveWork = Math.max(maxConsecutiveWork, consecutiveWorkDays);

                // Calculate working hours
                String startTimeStr = getStartTime(shiftInfo, shiftTypeMap);
                String endTimeStr = getEndTime(shiftInfo, shiftTypeMap);

                if (startTimeStr != null && endTimeStr != null) {
                    try {
                        LocalTime startTime = DateTimeHelper.parseTime(startTimeStr);
                        LocalTime endTime = DateTimeHelper.parseTime(endTimeStr);
                        totalWorkingHours += DateTimeHelper.calculateShiftDurationHours(startTime, endTime);
                    } catch (Exception e) {
                        // Skip invalid times
                    }
                }
            }
        }

        // Validate work-life balance criteria
        double workRatio = (double) workingDays / patternDays.size();
        double avgHoursPerWorkDay = workingDays > 0 ? totalWorkingHours / workingDays : 0;

        // Check for concerning patterns
        if (maxConsecutiveWork > MAX_CONSECUTIVE_WORK_DAYS) {
            return ValidationResult.error(ValidationError.PATTERN_EXCESSIVE_CONSECUTIVE_WORK,
                    maxConsecutiveWork, MAX_CONSECUTIVE_WORK_DAYS);
        }

        if (workRatio > MAX_WORK_RATIO) {
            return ValidationResult.error(ValidationError.PATTERN_HIGH_WORK_RATIO,
                    workRatio * 100, MAX_WORK_RATIO * 100);
        }

        if (avgHoursPerWorkDay > MAX_HOURS_PER_WORK_DAY) {
            return ValidationResult.error(ValidationError.PATTERN_EXCESSIVE_DAILY_HOURS,
                    avgHoursPerWorkDay, MAX_HOURS_PER_WORK_DAY);
        }

        if (restDays == 0) {
            return ValidationResult.error(ValidationError.PATTERN_NO_REST_DAYS);
        }

        // Warnings for suboptimal but acceptable patterns
        if (maxConsecutiveWork > 5) {
            return ValidationResult.warning(ValidationError.PATTERN_EXCESSIVE_CONSECUTIVE_WORK,
                    maxConsecutiveWork, "fatigue_risk");
        }

        if (avgHoursPerWorkDay > 8) {
            return ValidationResult.warning(ValidationError.PATTERN_EXCESSIVE_DAILY_HOURS,
                    avgHoursPerWorkDay, "demanding");
        }

        return ValidationResult.success("Work-life balance is acceptable");
    }

    // ============================================
    // FATIGUE RISK VALIDATION
    // ============================================

    /**
     * Validate fatigue risk based on shift characteristics
     */
    @NonNull
    public static ValidationResult validateFatigueRisk(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (patternDays.isEmpty()) {
            return ValidationResult.error(ValidationError.PATTERN_EMPTY);
        }

        int nightShifts = 0;
        int earlyShifts = 0; // Starting before 06:00
        int lateShifts = 0;  // Ending after 22:00
        int longShifts = 0;  // More than 8 hours

        for (ShiftInfo shiftInfo : patternDays) {
            if (!shiftInfo.isRestPeriod()) {
                String startTimeStr = getStartTime(shiftInfo, shiftTypeMap);
                String endTimeStr = getEndTime(shiftInfo, shiftTypeMap);

                if (startTimeStr != null && endTimeStr != null) {
                    try {
                        LocalTime startTime = DateTimeHelper.parseTime(startTimeStr);
                        LocalTime endTime = DateTimeHelper.parseTime(endTimeStr);

                        // Check shift characteristics
                        if (DateTimeHelper.isNightShift(startTime, endTime)) {
                            nightShifts++;
                        }

                        if (startTime.isBefore(LocalTime.of(6, 0))) {
                            earlyShifts++;
                        }

                        if (endTime.isAfter(LocalTime.of(22, 0)) && !DateTimeHelper.isNightShift(startTime, endTime)) {
                            lateShifts++;
                        }

                        double duration = DateTimeHelper.calculateShiftDurationHours(startTime, endTime);
                        if (duration > 8) {
                            longShifts++;
                        }

                    } catch (Exception e) {
                        // Skip invalid times
                    }
                }
            }
        }

        // Assess fatigue risk
        int totalWorkingDays = (int) patternDays.stream().filter(s -> !s.isRestPeriod()).count();

        if (totalWorkingDays == 0) {
            return ValidationResult.success("No fatigue risk - rest only pattern");
        }

        double nightShiftRatio = (double) nightShifts / totalWorkingDays;
        double longShiftRatio = (double) longShifts / totalWorkingDays;

        // High risk conditions
        if (nightShiftRatio > MAX_NIGHT_SHIFT_RATIO) {
            return ValidationResult.error(ValidationError.FATIGUE_HIGH_NIGHT_SHIFT_RATIO,
                    nightShiftRatio * 100, MAX_NIGHT_SHIFT_RATIO * 100);
        }

        if (longShiftRatio > MAX_LONG_SHIFT_RATIO) {
            return ValidationResult.error(ValidationError.FATIGUE_HIGH_LONG_SHIFT_RATIO,
                    longShiftRatio * 100, MAX_LONG_SHIFT_RATIO * 100);
        }

        // Medium risk warnings
        if (nightShiftRatio > 0.3) {
            return ValidationResult.warning(ValidationError.FATIGUE_HIGH_NIGHT_SHIFT_RATIO,
                    nightShiftRatio * 100, "moderate_fatigue");
        }

        if (earlyShifts + lateShifts > totalWorkingDays * 0.6) {
            return ValidationResult.warning(ValidationError.FATIGUE_EXCESSIVE_DISRUPTIVE_SHIFTS,
                    "early_late_shifts", "sleep_disruption");
        }

        return ValidationResult.success("Fatigue risk is under control");
    }

    // ============================================
    // PATTERN EFFICIENCY AND SUITABILITY
    // ============================================

    /**
     * Calculate pattern efficiency score (0-100)
     */
    public static int calculatePatternEfficiency(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount
    ) {
        if (patternDays.isEmpty()) return 0;

        int score = 100;

        // Factor 1: Coverage efficiency (0-30 points)
        ValidationResult coverageResult = validateContinuousCycleStructure(patternDays, shiftTypeMap, teamCount);
        if (coverageResult.isInvalid()) {
            score -= 30;
        }

        // Factor 2: Work-life balance (0-25 points)
        ValidationResult balanceResult = isValidWorkLifeBalance(patternDays, shiftTypeMap);
        if (balanceResult.isInvalid()) {
            score -= 25;
        } else if (balanceResult.isWarning()) {
            score -= 10;
        }

        // Factor 3: Fatigue risk (0-25 points)
        ValidationResult fatigueResult = validateFatigueRisk(patternDays, shiftTypeMap);
        if (fatigueResult.isInvalid()) {
            score -= 25;
        } else if (fatigueResult.isWarning()) {
            score -= 10;
        }

        // Factor 4: Team configuration appropriateness (0-20 points)
        ValidationResult teamResult = validateTeamConfiguration(teamCount, patternDays, shiftTypeMap);
        if (teamResult.isInvalid()) {
            score -= 20;
        } else if (teamResult.isWarning()) {
            score -= 10;
        }

        return Math.max(0, score);
    }

    /**
     * Check if pattern is suitable for continuous operation
     */
    public static boolean isContinuousOperationSuitable(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount
    ) {
        // Must have valid continuous coverage
        ValidationResult coverageResult = validateContinuousCycleStructure(patternDays, shiftTypeMap, teamCount);
        if (coverageResult.isInvalid()) {
            return false;
        }

        // Must have acceptable work-life balance
        ValidationResult balanceResult = isValidWorkLifeBalance(patternDays, shiftTypeMap);
        if (balanceResult.isInvalid()) {
            return false;
        }

        // Must have controlled fatigue risk
        ValidationResult fatigueResult = validateFatigueRisk(patternDays, shiftTypeMap);
        if (fatigueResult.isInvalid()) {
            return false;
        }

        // Must have appropriate team count
        ValidationResult teamResult = validateTeamConfiguration(teamCount, patternDays, shiftTypeMap);
        if (teamResult.isInvalid()) {
            return false;
        }

        return true;
    }

    // ============================================
    // VALIDATION CHECK FACTORY METHODS
    // ============================================

    /**
     * Create validation check for shift name
     */
    @NonNull
    public static ValidationCheck shiftNameCheck(@NonNull String shiftName) {
        return ValidationCheck.requiredString("shiftName", shiftName)
                .and(ValidationCheck.stringLength("shiftName", shiftName, 2, 50));
    }

    /**
     * Create validation check for shift time format
     */
    @NonNull
    public static ValidationCheck shiftTimeCheck(@NonNull String fieldName, @NonNull String timeString) {
        return ValidationCheck.requiredString(fieldName, timeString)
                .and(ValidationCheck.timeFormat(fieldName, timeString));
    }

    /**
     * Create validation check for shift duration
     */
    @NonNull
    public static ValidationCheck shiftDurationCheck(@NonNull String startTime, @NonNull String endTime) {
        return () -> {
            try {
                LocalTime start = DateTimeHelper.parseTime(startTime);
                LocalTime end = DateTimeHelper.parseTime(endTime);
                int duration = DateTimeHelper.calculateShiftDurationMinutes(start, end);

                if (duration < MIN_SHIFT_DURATION_MINUTES) {
                    return ValidationResult.shiftDurationTooShort();
                }
                if (duration > MAX_SHIFT_DURATION_MINUTES) {
                    return ValidationResult.shiftDurationTooLong();
                }

                return ValidationResult.success();
            } catch (Exception e) {
                return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT, "duration");
            }
        };
    }

    /**
     * Create validation check for complete shift configuration
     */
    @NonNull
    public static ValidationCheck completeShiftConfigurationCheck(
            @NonNull String shiftName,
            @NonNull String startTime,
            @NonNull String endTime,
            @Nullable String description,
            @Nullable String colorHex
    ) {
        return () -> validateShiftConfiguration(shiftName, startTime, endTime, description, colorHex);
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Calculate total coverage hours for a pattern
     */
    public static double calculateTotalCoverageHours(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        double totalHours = 0.0;

        for (ShiftInfo shiftInfo : patternDays) {
            if (!shiftInfo.isRestPeriod()) {
                String startTime = getStartTime(shiftInfo, shiftTypeMap);
                String endTime = getEndTime(shiftInfo, shiftTypeMap);

                if (startTime != null && endTime != null) {
                    try {
                        LocalTime start = DateTimeHelper.parseTime(startTime);
                        LocalTime end = DateTimeHelper.parseTime(endTime);
                        totalHours += DateTimeHelper.calculateShiftDurationHours(start, end);
                    } catch (Exception e) {
                        // Skip invalid times
                    }
                }
            }
        }

        return totalHours;
    }

    /**
     * Get coverage percentage for 24-hour operation
     */
    public static double getCoveragePercentage(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (patternDays.isEmpty()) return 0.0;

        double totalCoverageHours = calculateTotalCoverageHours(patternDays, shiftTypeMap);
        double totalPossibleHours = patternDays.size() * 24.0;

        return (totalCoverageHours / totalPossibleHours) * 100.0;
    }

    /**
     * Validate workload balance for teams
     */
    @NonNull
    public static ValidationResult validateWorkloadBalance(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount
    ) {
        if (patternDays.isEmpty()) {
            return ValidationResult.error(ValidationError.PATTERN_EMPTY);
        }

        if (teamCount < 1) {
            return ValidationResult.error(ValidationError.TEAM_COUNT_TOO_LOW, teamCount, 1);
        }

        double totalHours = calculateTotalCoverageHours(patternDays, shiftTypeMap);
        double expectedHoursPerTeam = totalHours / teamCount;

        // Reasonable workload checks
        if (expectedHoursPerTeam < 35) { // Less than 35 hours per cycle might be too little
            return ValidationResult.warning(ValidationError.TEAM_WORKLOAD_UNBALANCED,
                    expectedHoursPerTeam, "low_workload");
        }

        if (expectedHoursPerTeam > 60) { // More than 60 hours per cycle might be too much
            return ValidationResult.error(ValidationError.TEAM_WORKLOAD_EXCESSIVE,
                    expectedHoursPerTeam, 60);
        }

        return ValidationResult.success("Workload is balanced across teams");
    }

    /**
     * Validate minimum rest period between shifts
     */
    @NonNull
    public static ValidationResult validateMinimumRestPeriod(
            @NonNull ShiftInfo previousShift,
            @NonNull ShiftInfo currentShift,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int minimumRestHours
    ) {
        // Skip validation if either shift is a rest period
        if (previousShift.isRestPeriod() || currentShift.isRestPeriod()) {
            return ValidationResult.success("Rest period present - no minimum rest validation needed");
        }

        String prevEnd = getEndTime(previousShift, shiftTypeMap);
        String currStart = getStartTime(currentShift, shiftTypeMap);

        if (prevEnd == null || currStart == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Shift times not found for rest period calculation");
        }

        try {
            LocalTime previousEndTime = DateTimeHelper.parseTime(prevEnd);
            LocalTime currentStartTime = DateTimeHelper.parseTime(currStart);

            int restMinutes = DateTimeHelper.calculateGapMinutes(previousEndTime, currentStartTime);
            int minimumRestMinutes = minimumRestHours * 60;

            if (restMinutes < minimumRestMinutes) {
                return ValidationResult.error(ValidationError.FATIGUE_INSUFFICIENT_REST_PERIOD,
                        restMinutes, minimumRestMinutes);
            }

            return ValidationResult.success("Adequate rest period between shifts");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                    "Error calculating rest period: " + e.getMessage());
        }
    }

    /**
     * Validate daily shift conflicts within a single day
     */
    @NonNull
    public static ValidationResult validateDailyShiftConflicts(
            @NonNull List<ShiftInfo> dayShifts,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (dayShifts.size() <= 1) {
            return ValidationResult.success("No conflicts possible with single or no shifts");
        }

        // Check for overlaps between shifts in the same day
        for (int i = 0; i < dayShifts.size(); i++) {
            for (int j = i + 1; j < dayShifts.size(); j++) {
                ShiftInfo shift1 = dayShifts.get(i);
                ShiftInfo shift2 = dayShifts.get(j);

                // Skip rest periods
                if (shift1.isRestPeriod() || shift2.isRestPeriod()) {
                    continue;
                }

                String start1 = getStartTime(shift1, shiftTypeMap);
                String end1 = getEndTime(shift1, shiftTypeMap);
                String start2 = getStartTime(shift2, shiftTypeMap);
                String end2 = getEndTime(shift2, shiftTypeMap);

                if (start1 != null && end1 != null && start2 != null && end2 != null) {
                    try {
                        LocalTime startTime1 = DateTimeHelper.parseTime(start1);
                        LocalTime endTime1 = DateTimeHelper.parseTime(end1);
                        LocalTime startTime2 = DateTimeHelper.parseTime(start2);
                        LocalTime endTime2 = DateTimeHelper.parseTime(end2);

                        if (DateTimeHelper.doTimeRangesOverlap(startTime1, endTime1, startTime2, endTime2)) {
                            return ValidationResult.error(ValidationError.SHIFT_TIME_OVERLAP,
                                    shift1.getShiftType(), shift2.getShiftType());
                        }
                    } catch (Exception e) {
                        return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                                "Error validating daily conflicts: " + e.getMessage());
                    }
                }
            }
        }

        return ValidationResult.success("No daily shift conflicts detected");
    }

    // ============================================
    // INTEGRATION VALIDATION METHODS
    // ============================================

    /**
     * Validate integration with helper classes
     */
    @NonNull
    public static MultiValidationResult validateHelperIntegration() {
        MultiValidationResult result = new MultiValidationResult();

        // Test DateTimeHelper integration
        result.addResult(validateDateTimeHelperIntegration());

        // Test ValidationHelper integration
        result.addResult(validateValidationHelperIntegration());

        // Test StringHelper integration
        result.addResult(validateStringHelperIntegration());

        // Test ColorHelper integration
        result.addResult(validateColorHelperIntegration());

        return result;
    }

    /**
     * Validate DateTimeHelper integration
     */
    @NonNull
    private static ValidationResult validateDateTimeHelperIntegration() {
        try {
            // Test basic parsing
            LocalTime testTime = DateTimeHelper.parseTime("12:30");
            String formatted = DateTimeHelper.formatTime(testTime);

            // Test duration calculation
            int duration = DateTimeHelper.calculateShiftDurationMinutes(
                    LocalTime.of(8, 0), LocalTime.of(16, 0));

            if (duration != 480) { // 8 hours = 480 minutes
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Duration calculation failed");
            }

            // Test night shift detection
            boolean isNight = DateTimeHelper.isNightShift(LocalTime.of(22, 0), LocalTime.of(6, 0));
            if (!isNight) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Night shift detection failed");
            }

            // Test time range creation
            DateTimeHelper.TimeRange range = DateTimeHelper.createTimeRange(
                    LocalTime.of(8, 0), LocalTime.of(16, 0));
            if (range.getDurationMinutes() != 480) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "TimeRange creation failed");
            }

            return ValidationResult.success("DateTimeHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR, e.getMessage());
        }
    }

    /**
     * Validate ValidationHelper integration
     */
    @NonNull
    private static ValidationResult validateValidationHelperIntegration() {
        try {
            // Test basic validations
            if (!ValidationHelper.isValidTimeFormat("12:30")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "Time format validation failed");
            }

            if (!ValidationHelper.isValidEmail("test@example.com")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "Email validation failed");
            }

            if (!ValidationHelper.isValidPatternName("Test Pattern")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "Pattern name validation failed");
            }

            return ValidationResult.success("ValidationHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR, e.getMessage());
        }
    }

    /**
     * Validate StringHelper integration
     */
    @NonNull
    private static ValidationResult validateStringHelperIntegration() {
        try {
            // Test basic string operations
            if (!StringHelper.isEmpty(null) || !StringHelper.isEmpty("")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "isEmpty validation failed");
            }

            if (StringHelper.isNotEmpty(null) || StringHelper.isNotEmpty("")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "isNotEmpty validation failed");
            }

            // Test file size formatting
            String formatted = StringHelper.formatFileSize(1024);
            if (!formatted.contains("KB")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "File size formatting failed");
            }

            return ValidationResult.success("StringHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR, e.getMessage());
        }
    }

    /**
     * Validate ColorHelper integration
     */
    @NonNull
    private static ValidationResult validateColorHelperIntegration() {
        try {
            // Test color parsing
            int color = ColorHelper.parseColor("#FF0000");
            if (color == 0) {
                return ValidationResult.error(ValidationError.SHIFT_INVALID_COLOR,
                        "Color parsing failed");
            }

            // Test color to hex conversion
            String hex = ColorHelper.colorToHex(color);
            if (StringHelper.isEmpty(hex)) {
                return ValidationResult.error(ValidationError.SHIFT_INVALID_COLOR,
                        "Color to hex conversion failed");
            }

            return ValidationResult.success("ColorHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SHIFT_INVALID_COLOR, e.getMessage());
        }
    }

    // ============================================
    // PATTERN ANALYSIS METHODS
    // ============================================

    /**
     * Analyze pattern and provide comprehensive validation report
     */
    @NonNull
    public static MultiValidationResult analyzePattern(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount
    ) {
        MultiValidationResult analysis = new MultiValidationResult();

        // 1. Basic pattern validation
        if (patternDays.isEmpty()) {
            analysis.addError(ValidationError.PATTERN_EMPTY);
            return analysis; // Cannot continue analysis
        }

        // 2. Continuous coverage validation
        analysis.addResult(validateContinuousCycleStructure(patternDays, shiftTypeMap, teamCount));

        // 3. Work-life balance validation
        analysis.addResult(isValidWorkLifeBalance(patternDays, shiftTypeMap));

        // 4. Fatigue risk assessment
        analysis.addResult(validateFatigueRisk(patternDays, shiftTypeMap));

        // 5. Team configuration validation
        analysis.addResult(validateTeamConfiguration(teamCount, patternDays, shiftTypeMap));

        // 6. Workload balance validation
        analysis.addResult(validateWorkloadBalance(patternDays, shiftTypeMap, teamCount));

        // 7. Add efficiency score as info
        int efficiency = calculatePatternEfficiency(patternDays, shiftTypeMap, teamCount);
        if (efficiency < 70) {
            analysis.addWarning(ValidationError.PATTERN_LOW_EFFICIENCY, efficiency);
        }

        return analysis;
    }

    /**
     * Quick validation for basic pattern requirements
     */
    @NonNull
    public static ValidationResult quickValidatePattern(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap
    ) {
        if (patternDays.isEmpty()) {
            return ValidationResult.error(ValidationError.PATTERN_EMPTY);
        }

        // Check for basic shift type references
        for (ShiftInfo shiftInfo : patternDays) {
            if (!shiftInfo.isRestPeriod()) {
                if (!shiftTypeMap.containsKey(shiftInfo.getShiftType())) {
                    return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                            shiftInfo.getShiftType());
                }
            }
        }

        // Check for at least some rest periods
        boolean hasRest = patternDays.stream().anyMatch(ShiftInfo::isRestPeriod);
        if (!hasRest) {
            return ValidationResult.warning(ValidationError.PATTERN_NO_REST_DAYS);
        }

        return ValidationResult.success("Basic pattern validation passed");
    }

    // ============================================
    // BULK VALIDATION METHODS
    // ============================================

    /**
     * Validate multiple shift configurations at once
     */
    @NonNull
    public static MultiValidationResult validateMultipleShiftConfigurations(
            @NonNull List<ShiftType> shiftTypes
    ) {
        MultiValidationResult result = new MultiValidationResult();

        // Check individual shift configurations
        for (ShiftType shiftType : shiftTypes) {
            ValidationResult shiftResult = validateShiftConfiguration(
                    shiftType.getName(),
                    shiftType.getStartTime(),
                    shiftType.getEndTime(),
                    shiftType.getDescription(),
                    shiftType.getColorHex()
            );
            result.addResult(shiftResult);
        }

        // Check for overlaps between shift types
        ValidationResult overlapResult = validateShiftTypesNoOverlap(shiftTypes);
        result.addResult(overlapResult);

        return result;
    }

    /**
     * Validate pattern with detailed reporting
     */
    @NonNull
    public static MultiValidationResult validatePatternDetailed(
            @NonNull List<ShiftInfo> patternDays,
            @NonNull Map<String, ShiftType> shiftTypeMap,
            int teamCount,
            @NonNull List<Integer> teamOffsets
    ) {
        MultiValidationResult result = new MultiValidationResult();

        // 1. Pattern analysis
        MultiValidationResult patternAnalysis = analyzePattern(patternDays, shiftTypeMap, teamCount);
        result.addResults(patternAnalysis.getAllResults());

        // 2. Team offset validation
        ValidationResult offsetResult = validateTeamOffsets(teamOffsets, patternDays.size());
        result.addResult(offsetResult);

        // 3. Continuous operation suitability
        boolean isSuitable = isContinuousOperationSuitable(patternDays, shiftTypeMap, teamCount);
        if (!isSuitable) {
            result.addWarning(ValidationError.PATTERN_NOT_CONTINUOUS_SUITABLE);
        }

        return result;
    }
}