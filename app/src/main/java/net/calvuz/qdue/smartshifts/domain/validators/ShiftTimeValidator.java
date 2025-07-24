package net.calvuz.qdue.smartshifts.domain.validators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.domain.models.ShiftInfo;
import net.calvuz.qdue.smartshifts.utils.DateTimeHelper;
import net.calvuz.qdue.smartshifts.utils.ShiftValidationHelper;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationResult;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationError;
import net.calvuz.qdue.smartshifts.utils.validators.MultiValidationResult;
import net.calvuz.qdue.smartshifts.utils.validators.ValidationCheck;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Enhanced shift time validator for domain-specific shift validations.
 * <p>
 * Handles validation of shifts where:
 * - ShiftInfo contains the shift type name (reference to ShiftType)
 * - ShiftType entity contains the actual startTime and endTime
 * - Validation requires lookup between ShiftInfo and ShiftType map
 * <p>
 * Key responsibilities:
 * - Validate ShiftType time configurations
 * - Check conflicts between multiple shifts using ShiftType lookup
 * - Validate shift sequences and breaks
 * - Integration with DateTimeHelper for accurate time calculations
 * - Provide ValidationResult with error codes for i18n support
 *
 * @author SmartShifts Team
 * @since Phase 4 - Enhanced Validation Architecture
 */
@Singleton
public class ShiftTimeValidator {

    private static final String TAG = "ShiftTimeValidator";

    // Constants for validation
    private static final int MIN_SHIFT_DURATION_MINUTES = 60; // 1 hour
    private static final int MAX_SHIFT_DURATION_MINUTES = 16 * 60; // 16 hours
    private static final int MIN_BREAK_BETWEEN_SHIFTS_MINUTES = 30; // 30 minutes minimum break

    // Private constructor to prevent instantiation - all methods are static
//    private ShiftTimeValidator() {
//        throw new UnsupportedOperationException("ShiftTimeValidator is a utility class and cannot be instantiated");
//    }
    @Inject
    public ShiftTimeValidator() {
    }

    // ============================================
    // SHIFT TYPE TIME VALIDATION
    // ============================================

    /**
     * Validate ShiftType time configuration
     */
    @NonNull
    public static ValidationResult validateShiftType(@NonNull ShiftType shiftType) {
        String shiftName = shiftType.getName();
        String startTime = shiftType.getStartTime();
        String endTime = shiftType.getEndTime();
        String description = shiftType.getDescription();
        String colorHex = shiftType.getColorHex();

        // Delegate to ShiftValidationHelper for comprehensive validation
        return ShiftValidationHelper.validateShiftConfiguration(shiftName, startTime, endTime, description, colorHex);
    }

    /**
     * Validate multiple ShiftType entities for conflicts
     */
    @NonNull
    public static MultiValidationResult validateShiftTypes(@NonNull List<ShiftType> shiftTypes) {
        return ShiftValidationHelper.validateMultipleShiftConfigurations(shiftTypes);
    }

    // ============================================
    // SHIFT INFO VALIDATION WITH SHIFT TYPE LOOKUP
    // ============================================

    /**
     * Validate ShiftInfo against ShiftType map
     */
    @NonNull
    public static ValidationResult validateShiftInfo(@NonNull ShiftInfo shiftInfo,
                                                     @NonNull Map<String, ShiftType> shiftTypeMap) {
        // Skip validation for rest periods
        if (shiftInfo.isRestPeriod()) {
            return ValidationResult.success("Rest period - no time validation needed");
        }

        // Get ShiftType for this ShiftInfo
        String shiftTypeName = shiftInfo.getShiftType();
        ShiftType shiftType = shiftTypeMap.get(shiftTypeName);

        if (shiftType == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND, shiftTypeName);
        }

        // Validate the ShiftType itself
        return validateShiftType(shiftType);
    }

    /**
     * Validate list of ShiftInfo against ShiftType map
     */
    @NonNull
    public static MultiValidationResult validateShiftInfoList(@NonNull List<ShiftInfo> shiftInfoList,
                                                              @NonNull Map<String, ShiftType> shiftTypeMap) {
        MultiValidationResult result = new MultiValidationResult();

        for (int i = 0; i < shiftInfoList.size(); i++) {
            ShiftInfo shiftInfo = shiftInfoList.get(i);
            ValidationResult shiftValidation = validateShiftInfo(shiftInfo, shiftTypeMap);

            if (shiftValidation.isInvalid()) {
                result.addResult(shiftValidation.withFieldName("shift_" + (i + 1)));
            }
        }

        if (result.isEmpty()) {
            result.addResult(ValidationResult.success("All shifts validated successfully"));
        }

        return result;
    }

    // ============================================
    // SHIFT CONFLICTS AND OVERLAPS
    // ============================================

    /**
     * Check for time conflicts between two ShiftInfo objects
     */
    @NonNull
    public static ValidationResult validateNoShiftConflict(@NonNull ShiftInfo shift1, @NonNull ShiftInfo shift2,
                                                           @NonNull Map<String, ShiftType> shiftTypeMap) {
        // Skip validation if either shift is a rest period
        if (shift1.isRestPeriod() || shift2.isRestPeriod()) {
            return ValidationResult.success("Rest period present - no conflict possible");
        }

        // Get start and end times using ShiftValidationHelper
        String start1 = ShiftValidationHelper.getStartTime(shift1, shiftTypeMap);
        String end1 = ShiftValidationHelper.getEndTime(shift1, shiftTypeMap);
        String start2 = ShiftValidationHelper.getStartTime(shift2, shiftTypeMap);
        String end2 = ShiftValidationHelper.getEndTime(shift2, shiftTypeMap);

        // Validate that times exist
        if (start1 == null || end1 == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Times not found for shift type: " + shift1.getShiftType());
        }

        if (start2 == null || end2 == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Times not found for shift type: " + shift2.getShiftType());
        }

        // Use DateTimeHelper for overlap detection
        try {
            LocalTime startTime1 = DateTimeHelper.parseTime(start1);
            LocalTime endTime1 = DateTimeHelper.parseTime(end1);
            LocalTime startTime2 = DateTimeHelper.parseTime(start2);
            LocalTime endTime2 = DateTimeHelper.parseTime(end2);

            boolean hasOverlap = DateTimeHelper.doTimeRangesOverlap(startTime1, endTime1, startTime2, endTime2);

            if (hasOverlap) {
                return ValidationResult.error(ValidationError.SHIFT_TIME_OVERLAP,
                        shift1.getShiftType(), shift2.getShiftType());
            }

            return ValidationResult.success("No time conflict between shifts");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                    "Error parsing shift times: " + e.getMessage());
        }
    }

//    /**
//     * Validate daily shifts for conflicts
//     */
//    @NonNull
//    public static MultiValidationResult validateDailyShiftsForConflicts(@NonNull List<ShiftInfo> dailyShifts,
//                                                                        @NonNull Map<String, ShiftType> shiftTypeMap) {
//        return (MultiValidationResult) ShiftValidationHelper.validateDailyShiftConflicts(dailyShifts, shiftTypeMap);
//    }

    // ============================================
    // SHIFT DURATION VALIDATION
    // ============================================

    /**
     * Validate shift duration for ShiftInfo
     */
    @NonNull
    public static ValidationResult validateShiftDuration(@NonNull ShiftInfo shiftInfo,
                                                         @NonNull Map<String, ShiftType> shiftTypeMap) {
        // Skip validation for rest periods
        if (shiftInfo.isRestPeriod()) {
            return ValidationResult.success("Rest period - no duration validation needed");
        }

        // Get start and end times
        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Times not found for shift type: " + shiftInfo.getShiftType());
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);

            int durationMinutes = DateTimeHelper.calculateShiftDurationMinutes(start, end);

            // Validate minimum duration
            if (durationMinutes < MIN_SHIFT_DURATION_MINUTES) {
                return ValidationResult.error(ValidationError.SHIFT_DURATION_TOO_SHORT,
                        durationMinutes, MIN_SHIFT_DURATION_MINUTES);
            }

            // Validate maximum duration
            if (durationMinutes > MAX_SHIFT_DURATION_MINUTES) {
                return ValidationResult.error(ValidationError.SHIFT_DURATION_TOO_LONG,
                        durationMinutes, MAX_SHIFT_DURATION_MINUTES);
            }

            return ValidationResult.success("Valid shift duration: " +
                    DateTimeHelper.formatDurationMinutes(durationMinutes));

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                    "Error parsing shift times: " + e.getMessage());
        }
    }

    // ============================================
    // BREAK AND REST VALIDATION
    // ============================================

    /**
     * Validate minimum break between consecutive ShiftInfo objects
     */
    @NonNull
    public static ValidationResult validateMinimumBreakBetweenShifts(@NonNull ShiftInfo previousShift,
                                                                     @NonNull ShiftInfo nextShift,
                                                                     @NonNull Map<String, ShiftType> shiftTypeMap,
                                                                     int minimumBreakMinutes) {
        return ShiftValidationHelper.validateMinimumRestPeriod(previousShift, nextShift, shiftTypeMap, minimumBreakMinutes / 60);
    }

    /**
     * Validate minimum break with default minimum (30 minutes)
     */
    @NonNull
    public static ValidationResult validateMinimumBreakBetweenShifts(@NonNull ShiftInfo previousShift,
                                                                     @NonNull ShiftInfo nextShift,
                                                                     @NonNull Map<String, ShiftType> shiftTypeMap) {
        return validateMinimumBreakBetweenShifts(previousShift, nextShift, shiftTypeMap, MIN_BREAK_BETWEEN_SHIFTS_MINUTES);
    }

    // ============================================
    // NIGHT SHIFT VALIDATION
    // ============================================

    /**
     * Validate night shift configuration for ShiftInfo
     */
    @NonNull
    public static ValidationResult validateNightShift(@NonNull ShiftInfo shiftInfo,
                                                      @NonNull Map<String, ShiftType> shiftTypeMap) {
        // Skip validation for rest periods
        if (shiftInfo.isRestPeriod()) {
            return ValidationResult.success("Rest period - no night shift validation needed");
        }

        // Get start and end times
        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Times not found for shift type: " + shiftInfo.getShiftType());
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);

            // Check if it's actually a night shift
            boolean isNightShift = DateTimeHelper.isNightShift(start, end);
            if (!isNightShift) {
                return ValidationResult.warning(ValidationError.SHIFT_INVALID_NIGHT_SHIFT,
                        "Shift does not cross midnight - not a night shift");
            }

            // Validate night shift duration (typically should not exceed 12 hours)
            int durationMinutes = DateTimeHelper.calculateShiftDurationMinutes(start, end);
            if (durationMinutes > 12 * 60) {
                return ValidationResult.warning(ValidationError.SHIFT_DURATION_TOO_LONG,
                        "Night shift duration exceeds 12 hours: " + DateTimeHelper.formatDurationMinutes(durationMinutes));
            }

            return ValidationResult.success("Valid night shift configuration");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                    "Error parsing shift times: " + e.getMessage());
        }
    }

    // ============================================
    // SHIFT SEQUENCE VALIDATION
    // ============================================

    /**
     * Validate sequence of shifts for adequate breaks
     */
    @NonNull
    public static MultiValidationResult validateShiftSequence(@NonNull List<ShiftInfo> shiftSequence,
                                                              @NonNull Map<String, ShiftType> shiftTypeMap,
                                                              int minimumBreakMinutes) {
        MultiValidationResult result = new MultiValidationResult();

        if (shiftSequence.size() < 2) {
            result.addResult(ValidationResult.success("Single shift - no sequence validation needed"));
            return result;
        }

        // Validate breaks between consecutive shifts
        for (int i = 0; i < shiftSequence.size() - 1; i++) {
            ShiftInfo currentShift = shiftSequence.get(i);
            ShiftInfo nextShift = shiftSequence.get(i + 1);

            ValidationResult breakValidation = validateMinimumBreakBetweenShifts(
                    currentShift, nextShift, shiftTypeMap, minimumBreakMinutes);

            if (breakValidation.isInvalid()) {
                result.addResult(breakValidation.withFieldName(
                        "break_between_shift_" + (i + 1) + "_and_" + (i + 2)));
            }
        }

        // Validate individual shift durations in sequence
        for (int i = 0; i < shiftSequence.size(); i++) {
            ValidationResult durationValidation = validateShiftDuration(shiftSequence.get(i), shiftTypeMap);
            if (durationValidation.isInvalid()) {
                result.addResult(durationValidation.withFieldName("shift_" + (i + 1) + "_duration"));
            }
        }

        if (result.isEmpty()) {
            result.addResult(ValidationResult.success("Shift sequence validation passed"));
        }

        return result;
    }

    // ============================================
    // WORKING HOURS VALIDATION
    // ============================================

    /**
     * Validate if ShiftInfo falls within business/working hours
     */
    @NonNull
    public static ValidationResult validateWithinWorkingHours(@NonNull ShiftInfo shiftInfo,
                                                              @NonNull Map<String, ShiftType> shiftTypeMap,
                                                              @NonNull String workingHoursStart,
                                                              @NonNull String workingHoursEnd) {
        // Skip validation for rest periods
        if (shiftInfo.isRestPeriod()) {
            return ValidationResult.success("Rest period - no working hours validation needed");
        }

        // Get shift times
        String shiftStart = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String shiftEnd = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (shiftStart == null || shiftEnd == null) {
            return ValidationResult.error(ValidationError.SHIFT_TYPE_NOT_FOUND,
                    "Times not found for shift type: " + shiftInfo.getShiftType());
        }

        try {
            LocalTime shiftStartTime = DateTimeHelper.parseTime(shiftStart);
            LocalTime shiftEndTime = DateTimeHelper.parseTime(shiftEnd);
            LocalTime workStartTime = DateTimeHelper.parseTime(workingHoursStart);
            LocalTime workEndTime = DateTimeHelper.parseTime(workingHoursEnd);

            // Check if shift start is within working hours
            boolean startWithinHours = DateTimeHelper.isTimeInRange(shiftStartTime, workStartTime, workEndTime);

            // Check if shift end is within working hours (for non-night shifts)
            boolean endWithinHours = DateTimeHelper.isTimeInRange(shiftEndTime, workStartTime, workEndTime);

            // For night shifts, we may need different logic
            boolean isNightShift = DateTimeHelper.isNightShift(shiftStartTime, shiftEndTime);

            if (isNightShift) {
                // Night shifts may extend beyond normal working hours - this might be acceptable
                if (!startWithinHours) {
                    return ValidationResult.warning(ValidationError.TIME_OUTSIDE_WORKING_HOURS,
                            "Night shift starts outside working hours");
                }
            } else {
                // Day shifts should be within working hours
                if (!startWithinHours || !endWithinHours) {
                    return ValidationResult.error(ValidationError.TIME_OUTSIDE_WORKING_HOURS,
                            "Shift extends outside working hours");
                }
            }

            return ValidationResult.success("Shift is within acceptable working hours");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.TIME_INVALID_FORMAT,
                    "Error parsing times: " + e.getMessage());
        }
    }

    // ============================================
    // VALIDATION CHECK FACTORY METHODS
    // ============================================

    /**
     * Create validation check for ShiftType
     */
    @NonNull
    public static ValidationCheck createShiftTypeCheck(@NonNull ShiftType shiftType) {
        return () -> validateShiftType(shiftType);
    }

    /**
     * Create validation check for ShiftInfo
     */
    @NonNull
    public static ValidationCheck createShiftInfoCheck(@NonNull ShiftInfo shiftInfo,
                                                       @NonNull Map<String, ShiftType> shiftTypeMap) {
        return () -> validateShiftInfo(shiftInfo, shiftTypeMap);
    }

    /**
     * Create validation check for shift conflict
     */
    @NonNull
    public static ValidationCheck createConflictCheck(@NonNull ShiftInfo shift1, @NonNull ShiftInfo shift2,
                                                      @NonNull Map<String, ShiftType> shiftTypeMap) {
        return () -> validateNoShiftConflict(shift1, shift2, shiftTypeMap);
    }

    /**
     * Create validation check for shift duration
     */
    @NonNull
    public static ValidationCheck createDurationCheck(@NonNull ShiftInfo shiftInfo,
                                                      @NonNull Map<String, ShiftType> shiftTypeMap) {
        return () -> validateShiftDuration(shiftInfo, shiftTypeMap);
    }

    /**
     * Create comprehensive shift validation check
     */
    @NonNull
    public static ValidationCheck createCompleteShiftCheck(@NonNull ShiftInfo shiftInfo,
                                                           @NonNull Map<String, ShiftType> shiftTypeMap) {
        return ValidationCheck.all(
                createShiftInfoCheck(shiftInfo, shiftTypeMap),
                createDurationCheck(shiftInfo, shiftTypeMap)
        );
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get shift duration in human-readable format
     */
    @NonNull
    public static String getShiftDurationDisplay(@NonNull ShiftInfo shiftInfo,
                                                 @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return "Rest period";
        }

        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return "Invalid shift type";
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);
            int durationMinutes = DateTimeHelper.calculateShiftDurationMinutes(start, end);
            return DateTimeHelper.formatDurationMinutes(durationMinutes);
        } catch (Exception e) {
            return "Invalid time format";
        }
    }

    /**
     * Check if ShiftInfo represents a night shift
     */
    public static boolean isNightShift(@NonNull ShiftInfo shiftInfo, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return false;
        }

        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return false;
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);
            return DateTimeHelper.isNightShift(start, end);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get overlap duration between two ShiftInfo objects in minutes
     */
    public static int getOverlapDurationMinutes(@NonNull ShiftInfo shift1, @NonNull ShiftInfo shift2,
                                                @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shift1.isRestPeriod() || shift2.isRestPeriod()) {
            return 0;
        }

        String start1 = ShiftValidationHelper.getStartTime(shift1, shiftTypeMap);
        String end1 = ShiftValidationHelper.getEndTime(shift1, shiftTypeMap);
        String start2 = ShiftValidationHelper.getStartTime(shift2, shiftTypeMap);
        String end2 = ShiftValidationHelper.getEndTime(shift2, shiftTypeMap);

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return 0;
        }

        try {
            LocalTime startTime1 = DateTimeHelper.parseTime(start1);
            LocalTime endTime1 = DateTimeHelper.parseTime(end1);
            LocalTime startTime2 = DateTimeHelper.parseTime(start2);
            LocalTime endTime2 = DateTimeHelper.parseTime(end2);

            if (!DateTimeHelper.doTimeRangesOverlap(startTime1, endTime1, startTime2, endTime2)) {
                return 0;
            }

            // Calculate overlap duration
            int start1Min = DateTimeHelper.timeToMinutesSinceMidnight(startTime1);
            int end1Min = DateTimeHelper.timeToMinutesSinceMidnight(endTime1);
            int start2Min = DateTimeHelper.timeToMinutesSinceMidnight(startTime2);
            int end2Min = DateTimeHelper.timeToMinutesSinceMidnight(endTime2);

            // Handle night shifts
            if (end1Min <= start1Min) end1Min += 24 * 60;
            if (end2Min <= start2Min) end2Min += 24 * 60;

            // Calculate overlap
            int overlapStart = Math.max(start1Min, start2Min);
            int overlapEnd = Math.min(end1Min, end2Min);

            return Math.max(0, overlapEnd - overlapStart);

        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get time range display for ShiftInfo
     */
    @NonNull
    public static String getTimeRangeDisplay(@NonNull ShiftInfo shiftInfo, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return "Rest";
        }

        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return "Invalid shift type: " + shiftInfo.getShiftType();
        }

        return startTime + " - " + endTime;
    }

    // ============================================
    // INTEGRATION METHODS
    // ============================================

    /**
     * Test integration with ShiftValidationHelper
     */
    @NonNull
    public static ValidationResult testShiftValidationHelperIntegration() {
        try {
            // Test basic shift configuration validation
            ValidationResult shiftTest = ShiftValidationHelper.validateShiftConfiguration(
                    "Test Shift", "08:00", "16:00", "Test description", "#FF0000");

            if (shiftTest.isInvalid()) {
                return ValidationResult.error(ValidationError.SYSTEM_DATABASE_ERROR,
                        "ShiftValidationHelper test failed: " + shiftTest.getInternalMessage());
            }

            return ValidationResult.success("ShiftValidationHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_DATABASE_ERROR,
                    "ShiftValidationHelper integration error: " + e.getMessage());
        }
    }

    /**
     * Test integration with DateTimeHelper
     */
    @NonNull
    public static ValidationResult testDateTimeHelperIntegration() {
        try {
            // Test basic time operations
            LocalTime testTime = DateTimeHelper.parseTime("12:30");
            String formatted = DateTimeHelper.formatTime(testTime);

            if (!"12:30".equals(formatted)) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Time formatting test failed");
            }

            // Test duration calculation
            int duration = DateTimeHelper.calculateShiftDurationMinutes(
                    LocalTime.of(8, 0), LocalTime.of(16, 0));

            if (duration != 480) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Duration calculation test failed");
            }

            // Test night shift detection
            boolean isNight = DateTimeHelper.isNightShift(LocalTime.of(22, 0), LocalTime.of(6, 0));
            if (!isNight) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Night shift detection test failed");
            }

            return ValidationResult.success("DateTimeHelper integration test passed");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                    "Integration test failed: " + e.getMessage());
        }
    }

    /**
     * Comprehensive integration test
     */
    @NonNull
    public static MultiValidationResult validateAllIntegrations() {
        MultiValidationResult result = new MultiValidationResult();

        result.addResult(testShiftValidationHelperIntegration());
        result.addResult(testDateTimeHelperIntegration());

        return result;
    }

    // ============================================
    // CONSTANTS FOR EXTERNAL USE
    // ============================================

    /**
     * Minimum shift duration in minutes
     */
    public static final int MIN_SHIFT_DURATION = MIN_SHIFT_DURATION_MINUTES;

    /**
     * Maximum shift duration in minutes
     */
    public static final int MAX_SHIFT_DURATION = MAX_SHIFT_DURATION_MINUTES;

    /**
     * Minimum break between shifts in minutes
     */
    public static final int MIN_BREAK_BETWEEN_SHIFTS = MIN_BREAK_BETWEEN_SHIFTS_MINUTES;

    // ============================================
    // DEPRECATED METHODS (FOR BACKWARD COMPATIBILITY)
    // ============================================

    /**
     * @deprecated Use validateShiftType() instead
     */
    @Deprecated
    public static boolean isValidTimeFormat(@Nullable String time) {
        return DateTimeHelper.parseTimeSafe(time) != null;
    }

    /**
     * @deprecated Use validateNoShiftConflict() instead
     */
    @Deprecated
    public static boolean hasTimeOverlap(@NonNull ShiftInfo shift1, @NonNull ShiftInfo shift2,
                                         @NonNull Map<String, ShiftType> shiftTypeMap) {
        ValidationResult result = validateNoShiftConflict(shift1, shift2, shiftTypeMap);
        return result.isInvalid();
    }

    /**
     * @deprecated Use getShiftDurationDisplay() instead
     */
    @Deprecated
    public static int calculateShiftDuration(@NonNull ShiftInfo shiftInfo, @NonNull Map<String, ShiftType> shiftTypeMap) {
        if (shiftInfo.isRestPeriod()) {
            return 0;
        }

        String startTime = ShiftValidationHelper.getStartTime(shiftInfo, shiftTypeMap);
        String endTime = ShiftValidationHelper.getEndTime(shiftInfo, shiftTypeMap);

        if (startTime == null || endTime == null) {
            return 0;
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);
            return DateTimeHelper.calculateShiftDurationMinutes(start, end);
        } catch (Exception e) {
            return 0;
        }
    }
}