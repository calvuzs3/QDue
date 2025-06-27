package net.calvuz.qdue.ui.proto;

import net.calvuz.qdue.events.models.TurnException;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine responsible for merging base shift patterns with user exceptions.
 * <p>
 * Core component of the Virtual Scrolling system that handles:
 * - Merging 4-2 pattern with individual exceptions
 * - Reversible exception handling
 * - Performance-optimized merging for virtual scrolling
 * - Support for different exception types
 * <p>
 * Integration Points:
 * - CalendarDataManagerEnhanced calls this during data loading
 * - VirtualCalendarDataManager uses this for real-time merging
 * - Pattern changes are applied without affecting base pattern
 */
public class PatternMergeEngine {

    private static final String TAG = "PatternMergeEngine";
    private static final boolean LOG_ENABLED = true;

    /**
     * Merge base pattern days with user exceptions
     * <p>
     * @param baseDays Original pattern days from QuattroDue
     * @param exceptions List of exceptions for the same period
     * @param userHalfTeam User's team for filtering relevant changes
     * @return Modified days with exceptions applied
     */
    public static List<Day> mergePatternWithExceptions(List<Day> baseDays,
                                                       List<TurnException> exceptions,
                                                       HalfTeam userHalfTeam) {
        if (baseDays == null || baseDays.isEmpty()) {
            return new ArrayList<>();
        }

        if (exceptions == null || exceptions.isEmpty()) {
            return new ArrayList<>(baseDays); // Return copy of original
        }

        if (LOG_ENABLED) {
            Log.d(TAG, "Merging " + baseDays.size() + " days with " + exceptions.size() +
                    " exceptions for team " + userHalfTeam.getName());
        }

        // Create exception lookup map for performance
        Map<LocalDate, TurnException> exceptionMap = createExceptionMap(exceptions);

        // Process each day
        List<Day> mergedDays = new ArrayList<>();
        for (Day baseDay : baseDays) {
            Day mergedDay = applyExceptionToDay(baseDay, exceptionMap.get(baseDay.getLocalDate()), userHalfTeam);
            mergedDays.add(mergedDay);
        }

        if (LOG_ENABLED) {
            Log.d(TAG, "Merge completed: " + mergedDays.size() + " days processed");
        }

        return mergedDays;
    }

    /**
     * Apply single exception to a day
     * <p>
     * @param baseDay Original day from pattern
     * @param exception Exception to apply (can be null)
     * @param userHalfTeam User's team
     * @return Modified day with exception applied
     */
    private static Day applyExceptionToDay(Day baseDay, TurnException exception, HalfTeam userHalfTeam) {
        if (exception == null || !exception.isActive()) {
            return cloneDay(baseDay); // No exception, return copy of original
        }

        if (LOG_ENABLED) {
            Log.v(TAG, "Applying exception " + exception.getExceptionType() +
                    " to date " + baseDay.getLocalDate());
        }

        Day modifiedDay = cloneDay(baseDay);

        // Apply exception based on type
        switch (exception.getExceptionType()) {
            case VACATION:
            case SICK_LEAVE:
            case PERSONAL_LEAVE:
            case PERMIT_104:
                // Remove user from all shifts (no work)
                removeUserFromAllShifts(modifiedDay, userHalfTeam);
                break;

            case OVERTIME:
            case TRAINING:
            case EMERGENCY:
                // Modify shift assignment if replacement specified
                modifyUserShiftAssignment(modifiedDay, userHalfTeam, exception);
                break;

            case SHIFT_SWAP:
                // Handle shift swap logic
                handleShiftSwap(modifiedDay, userHalfTeam, exception);
                break;

            case COMPENSATION:
                // Add extra shift or modify existing
                handleCompensation(modifiedDay, userHalfTeam, exception);
                break;

            case OTHER:
                // Custom handling based on replacement shift type
                handleCustomException(modifiedDay, userHalfTeam, exception);
                break;

            default:
                Log.w(TAG, "Unknown exception type: " + exception.getExceptionType());
        }

        return modifiedDay;
    }

    /**
     * Remove user from all shifts on this day
     */
    private static void removeUserFromAllShifts(Day day, HalfTeam userHalfTeam) {
        for (Shift shift : day.getShifts()) {
            shift.getHalfTeams().removeIf(team -> team.isSameTeamAs(userHalfTeam));
        }

        // Add user to off-work teams if not already there
        if (!day.getOffWorkHalfTeams().contains(userHalfTeam)) {
            day.getOffWorkHalfTeams().add(userHalfTeam);
        }

        if (LOG_ENABLED) {
            Log.v(TAG, "Removed team " + userHalfTeam.getName() + " from all shifts on " + day.getLocalDate());
        }
    }

    /**
     * Modify user's shift assignment based on exception
     */
    private static void modifyUserShiftAssignment(Day day, HalfTeam userHalfTeam, TurnException exception) {
        String replacementShiftType = exception.getReplacementShiftType();

        if (replacementShiftType == null || replacementShiftType.trim().isEmpty()) {
            // No replacement specified, treat as no-work day
            removeUserFromAllShifts(day, userHalfTeam);
            return;
        }

        // First remove user from current shifts
        for (Shift shift : day.getShifts()) {
            shift.getHalfTeams().removeIf(team -> team.isSameTeamAs(userHalfTeam));
        }

        // Then add to the specified shift type
        for (Shift shift : day.getShifts()) {
            if (shift.getShiftType().getName().equals(replacementShiftType)) {
                shift.getHalfTeams().add(userHalfTeam);
                // Remove from off-work teams
                day.getOffWorkHalfTeams().removeIf(team -> team.isSameTeamAs(userHalfTeam));

                if (LOG_ENABLED) {
                    Log.v(TAG, "Moved team " + userHalfTeam.getName() +
                            " to " + replacementShiftType + " shift on " + day.getLocalDate());
                }
                return;
            }
        }

        Log.w(TAG, "Could not find shift type " + replacementShiftType + " for exception");
    }

    /**
     * Handle shift swap between users
     */
    private static void handleShiftSwap(Day day, HalfTeam userHalfTeam, TurnException exception) {
        // For now, treat as simple shift change
        // Future enhancement: implement proper swap logic with other user
        modifyUserShiftAssignment(day, userHalfTeam, exception);
    }

    /**
     * Handle compensation time
     */
    private static void handleCompensation(Day day, HalfTeam userHalfTeam, TurnException exception) {
        String replacementShiftType = exception.getReplacementShiftType();

        if (replacementShiftType != null && !replacementShiftType.trim().isEmpty()) {
            // Add to specified shift (in addition to existing)
            modifyUserShiftAssignment(day, userHalfTeam, exception);
        } else {
            // No specific shift, might be time off
            removeUserFromAllShifts(day, userHalfTeam);
        }
    }

    /**
     * Handle custom exceptions
     */
    private static void handleCustomException(Day day, HalfTeam userHalfTeam, TurnException exception) {
        // Default to modifying shift assignment
        modifyUserShiftAssignment(day, userHalfTeam, exception);
    }

    /**
     * Create a lookup map for fast exception access
     */
    private static Map<LocalDate, TurnException> createExceptionMap(List<TurnException> exceptions) {
        Map<LocalDate, TurnException> map = new HashMap<>();
        for (TurnException exception : exceptions) {
            map.put(exception.getDate(), exception);
        }
        return map;
    }

    /**
     * Clone a day to avoid modifying the original
     */
    private static Day cloneDay(Day original) {
        try {
            return original.clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Failed to clone day: " + e.getMessage());
            // Fallback: create new day with same date
            return new Day(original.getLocalDate());
        }
    }

    /**
     * Get the original shift type for a user on a specific day
     * Used when creating exceptions to store the original pattern
     */
    public static String getOriginalShiftType(Day baseDay, HalfTeam userHalfTeam) {
        if (baseDay == null || userHalfTeam == null) {
            return null;
        }

        // Find which shift the user is assigned to
        for (Shift shift : baseDay.getShifts()) {
            for (HalfTeam team : shift.getHalfTeams()) {
                if (team.isSameTeamAs(userHalfTeam)) {
                    return shift.getShiftType().getName();
                }
            }
        }

        // User is not working (off day)
        return "OFF";
    }

    /**
     * Validate that an exception can be applied to a day
     */
    public static boolean validateException(Day baseDay, TurnException exception, HalfTeam userHalfTeam) {
        if (baseDay == null || exception == null || userHalfTeam == null) {
            return false;
        }

        // Check date matches
        if (!baseDay.getLocalDate().equals(exception.getDate())) {
            Log.w(TAG, "Exception date mismatch: day=" + baseDay.getLocalDate() +
                    ", exception=" + exception.getDate());
            return false;
        }

        // Additional validations can be added here
        return true;
    }

    /**
     * Performance utility: batch merge multiple months
     */
    public static Map<LocalDate, List<Day>> batchMergeMonths(
            Map<LocalDate, List<Day>> monthlyBaseDays,
            Map<LocalDate, List<TurnException>> monthlyExceptions,
            HalfTeam userHalfTeam) {

        Map<LocalDate, List<Day>> mergedMonths = new HashMap<>();

        for (Map.Entry<LocalDate, List<Day>> entry : monthlyBaseDays.entrySet()) {
            LocalDate month = entry.getKey();
            List<Day> baseDays = entry.getValue();
            List<TurnException> exceptions = monthlyExceptions.get(month);

            List<Day> mergedDays = mergePatternWithExceptions(baseDays, exceptions, userHalfTeam);
            mergedMonths.put(month, mergedDays);
        }

        return mergedMonths;
    }

    /**
     * Utility to check if any exceptions exist for a date range
     */
    public static boolean hasExceptionsInRange(List<TurnException> exceptions,
                                               LocalDate startDate, LocalDate endDate) {
        if (exceptions == null || exceptions.isEmpty()) {
            return false;
        }

        return exceptions.stream()
                .anyMatch(e -> !e.getDate().isBefore(startDate) &&
                        !e.getDate().isAfter(endDate) &&
                        e.isActive());
    }

    /**
     * Get summary of applied exceptions for debugging
     */
    public static String getMergeSummary(List<Day> originalDays, List<Day> mergedDays,
                                         List<TurnException> exceptions) {
        if (!LOG_ENABLED) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Merge Summary:\n");
        summary.append("- Original days: ").append(originalDays.size()).append("\n");
        summary.append("- Merged days: ").append(mergedDays.size()).append("\n");
        summary.append("- Exceptions applied: ").append(exceptions.size()).append("\n");

        // Count exceptions by type
        Map<TurnException.ExceptionType, Integer> typeCount = new HashMap<>();
        for (TurnException exception : exceptions) {
            typeCount.merge(exception.getExceptionType(), 1, Integer::sum);
        }

        if (!typeCount.isEmpty()) {
            summary.append("- Exception breakdown:\n");
            for (Map.Entry<TurnException.ExceptionType, Integer> entry : typeCount.entrySet()) {
                summary.append("  * ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return summary.toString();
    }
}