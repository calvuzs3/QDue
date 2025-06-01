package net.calvuz.qdue.quattrodue.utils;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;

/**
 * Utility class for managing shift schedule patterns.
 *
 * Implements the logic for applying the fixed shift rotation scheme.
 * Contains the 18-day rotation pattern and provides methods for
 * generating days, finding team schedules, and calculating rotations.
 *
 * @author Updated 21/05/2025
 */
public final class SchemeManager {

    private static final String TAG = "SchemeManager";

    // Constants for the scheme
    private static final int SHIFTS_PER_DAY = 3;

    // Fixed rotation scheme (each row is a day, each column is a shift)
    // For each element, the character list represents teams assigned to that shift
    private static final char[][][] SCHEME = new char[][][]{
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}}
    };

    // Number of days in the rotation cycle
    private static final int CYCLE_LENGTH = SCHEME.length;

    // Reference start date for the scheme
    private static LocalDate referenceStartDate = LocalDate.of(2018, 11, 7);

    // Prevent instantiation
    private SchemeManager() {}

    /**
     * Sets the reference start date for the scheme.
     *
     * @param date Scheme start date
     */
    public static void setReferenceStartDate(LocalDate date) {
        if (date != null) {
            referenceStartDate = date;
        }
    }

    /**
     * Gets the reference start date for the scheme.
     *
     * @return Scheme start date
     */
    public static LocalDate getReferenceStartDate() {
        return referenceStartDate;
    }

    /**
     * Generates the base cycle days of the scheme.
     * These days serve as templates for generating actual days.
     *
     * @param shiftTypes List of shift types
     * @return List of scheme days
     */
    public static List<Day> generateCycleDays(List<ShiftType> shiftTypes) {
        List<Day> schemeDays = new ArrayList<>(CYCLE_LENGTH);

        try {
            // Generate cycle days
            for (int dayIndex = 0; dayIndex < CYCLE_LENGTH; dayIndex++) {
                // Create a reference day
                LocalDate dayDate = referenceStartDate.plusDays(dayIndex);
                Day day = new Day(dayDate);

                // Configure shifts according to scheme
                for (int shiftIndex = 0; shiftIndex < SHIFTS_PER_DAY; shiftIndex++) {
                    // Create a new shift
                    Shift shift = new Shift(shiftTypes.get(shiftIndex));

                    // Add teams according to scheme
                    for (char teamChar : SCHEME[dayIndex][shiftIndex]) {
                        HalfTeam team = HalfTeamFactory.getByChar(teamChar);
                        shift.addTeam(team);
                    }

                    // Add shift to day
                    day.addShift(shift);
                }

                // Add day to list
                schemeDays.add(day);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating cycle days: " + e.getMessage());
        }

        return schemeDays;
    }

    /**
     * Generates days for a specific month applying the rotating scheme.
     *
     * @param targetDate Target month date
     * @param cycleDays Template cycle days
     * @return List of configured days for the month
     */
    public static List<Day> generateDaysForMonth(LocalDate targetDate, List<Day> cycleDays) {
        List<Day> monthDays = new ArrayList<>();

        if (targetDate == null || cycleDays == null || cycleDays.isEmpty()) {
            return monthDays;
        }

        try {
            // Set date to first day of month
            LocalDate firstDayOfMonth = LocalDate.of(targetDate.getYear(), targetDate.getMonth(), 1);

            // Calculate days between reference date and first day of month
            int daysBetween = (int) Period.between(referenceStartDate, firstDayOfMonth).toTotalMonths() * 30 +
                    Period.between(referenceStartDate, firstDayOfMonth).getDays();

            // Calculate starting index in cycle
            int startIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

            // Get number of days in month
            int daysInMonth = targetDate.lengthOfMonth();

            // Generate month days
            LocalDate currentDate = firstDayOfMonth;

            for (int i = 0; i < daysInMonth; i++) {
                int cycleIndex = (startIndex + i) % CYCLE_LENGTH;

                // Clone day from cycle
                Day day = cycleDays.get(cycleIndex).clone();

                // Set correct date
                day.setLocalDate(currentDate);

                // Add day to list
                monthDays.add(day);

                // Move to next day
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating month days: " + e.getMessage());
        }

        return monthDays;
    }

    /**
     * Checks if a day has a specific team on shift.
     *
     * @param day Day to check
     * @param team Team to search for
     * @return Shift index (0-based) if team is on shift, -1 otherwise
     */
    public static int findTeamShiftIndex(Day day, HalfTeam team) {
        if (day == null || team == null) {
            return -1;
        }

        List<Shift> shifts = day.getShifts();
        if (shifts == null || shifts.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).containsHalfTeam(team)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Finds the next date when a team is on shift, starting from a specified date.
     *
     * @param startDate Search start date
     * @param team Team to search for
     * @param maxDaysToCheck Maximum number of days to check
     * @return Date of next shift, or null if not found within limit
     */
    public static LocalDate findNextShiftDate(LocalDate startDate, HalfTeam team, int maxDaysToCheck) {
        if (startDate == null || team == null || maxDaysToCheck <= 0) {
            return null;
        }

        try {
            // List of template cycle days
            List<ShiftType> shiftTypes = new ArrayList<>();
            for (int i = 0; i < SHIFTS_PER_DAY; i++) {
                shiftTypes.add(ShiftTypeFactory.createCustom(String.valueOf(i + 1),
                        "Shift " + (i + 1), 5 + i*8, 0, 8, 0));
            }

            List<Day> cycleDays = generateCycleDays(shiftTypes);

            // Search in future days
            LocalDate currentDate = startDate;

            for (int i = 0; i < maxDaysToCheck; i++) {
                // Calculate days between reference date and current date
                int daysBetween = (int) Period.between(referenceStartDate, currentDate).toTotalMonths() * 30 +
                        Period.between(referenceStartDate, currentDate).getDays();

                // Calculate cycle index
                int cycleIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

                // Check if team is on shift this day
                Day day = cycleDays.get(cycleIndex);
                if (findTeamShiftIndex(day, team) >= 0) {
                    return currentDate;
                }

                // Move to next day
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching next shift: " + e.getMessage());
        }

        return null; // Not found within limit
    }

    /**
     * Gets all teams working on a specific date.
     *
     * @param date Date of interest
     * @return List of teams on shift
     */
    public static List<HalfTeam> getTeamsWorkingOnDate(LocalDate date) {
        List<HalfTeam> workingTeams = new ArrayList<>();

        if (date == null) {
            return workingTeams;
        }

        try {
            // Calculate days between reference date and specified date
            int daysBetween = (int) Period.between(referenceStartDate, date).toTotalMonths() * 30 +
                    Period.between(referenceStartDate, date).getDays();

            // Calculate cycle index
            int cycleIndex = Math.floorMod(daysBetween, CYCLE_LENGTH);

            // Get teams on shift for each shift type
            for (int shiftIndex = 0; shiftIndex < SHIFTS_PER_DAY; shiftIndex++) {
                char[] teamChars = SCHEME[cycleIndex][shiftIndex];
                for (char teamChar : teamChars) {
                    HalfTeam team = HalfTeamFactory.getByChar(teamChar);
                    workingTeams.add(team);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting working teams: " + e.getMessage());
        }

        return workingTeams;
    }

    /**
     * Gets all teams that are off work on a specific date.
     *
     * @param date Date of interest
     * @return List of teams off work
     */
    public static List<HalfTeam> getTeamsOffWorkOnDate(LocalDate date) {
        // List of all teams
        List<HalfTeam> allTeams = HalfTeamFactory.getAllTeams();

        // List of working teams
        List<HalfTeam> workingTeams = getTeamsWorkingOnDate(date);

        // Create copy of all teams
        List<HalfTeam> offWorkTeams = new ArrayList<>(allTeams);

        // Remove working teams
        offWorkTeams.removeAll(workingTeams);

        return offWorkTeams;
    }
}