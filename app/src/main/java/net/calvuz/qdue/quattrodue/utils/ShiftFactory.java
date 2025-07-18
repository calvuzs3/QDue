package net.calvuz.qdue.quattrodue.utils;

import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating shifts (Shift objects).
 * <p>
 * Updated to work with the new dynamic ShiftTypeFactory.
 * No longer relies on static shift types but accesses the
 * dynamically configured shift types from the factory cache.
 *
 * @author Updated 21/05/2025
 */
public final class ShiftFactory {

    private static final String TAG = ShiftFactory.class.getSimpleName();

    // Prevent instantiation
    private ShiftFactory() {}

    /**
     * Creates a shift using a shift type from the dynamic factory.
     *
     * @param shiftIndex Shift index (0-based) from ShiftTypeFactory
     * @param date Shift date
     * @return New Shift, or null if shift type not found
     */
    public static Shift createShift(int shiftIndex, LocalDate date) {
        ShiftType shiftType = ShiftTypeFactory.getShiftType(shiftIndex);

        if (shiftType == null) {
            Log.w(TAG, "No shift type found at index " + shiftIndex);
            return null;
        }

        return new Shift(shiftType, date);
    }

    /**
     * Creates a shift using a named shift type from the dynamic factory.
     *
     * @param shiftName Name of the shift type
     * @param date Shift date
     * @return New Shift, or null if shift type not found
     */
    public static Shift createShift(String shiftName, LocalDate date) {
        ShiftType shiftType = ShiftTypeFactory.getShiftType(shiftName);

        if (shiftType == null) {
            Log.w(TAG, "No shift type found with name: " + shiftName);
            return null;
        }

        return new Shift(shiftType, date);
    }

    /**
     * Creates a standard shift by legacy index (for backward compatibility).
     *
     * @deprecated Use createShift(int, LocalDate) instead
     * @param type Legacy shift type (1=first, 2=second, 3=third shift)
     * @param date Shift date
     * @return New Shift
     * @throws IllegalArgumentException if shift type is invalid or not available
     */
    @Deprecated
    public static Shift createStandardShift(int type, LocalDate date) {
        if (type < 1) {
            throw new IllegalArgumentException("Shift type must be >= 1, got: " + type);
        }

        // Convert 1-based to 0-based index
        int shiftIndex = type - 1;
        int availableShifts = ShiftTypeFactory.getShiftCount();

        if (shiftIndex >= availableShifts) {
            throw new IllegalArgumentException(
                    "Shift type " + type + " not available. Only " + availableShifts + " shifts configured."
            );
        }

        Shift shift = createShift(shiftIndex, date);
        if (shift == null) {
            throw new IllegalArgumentException("Failed to create shift for type: " + type);
        }

        return shift;
    }

    /**
     * Creates all configured shifts for a specific date.
     * Uses all shift types available in the ShiftTypeFactory.
     *
     * @param date Date for the shifts
     * @return List of shifts (size depends on factory configuration)
     */
    public static List<Shift> createDailyShifts(LocalDate date) {
        int shiftCount = ShiftTypeFactory.getShiftCount();
        List<Shift> shifts = new ArrayList<>(shiftCount);

        for (int i = 0; i < shiftCount; i++) {
            Shift shift = createShift(i, date);
            if (shift != null) {
                shifts.add(shift);
            } else {
                Log.w(TAG, "Skipped null shift at index " + i + " for date " + date);
            }
        }

        if (shifts.isEmpty()) {
            Log.e(TAG, "No shifts created for date " + date + ". ShiftTypeFactory not initialized?");
        }

        return shifts;
    }

    /**
     * Creates all shifts with specific names for a date.
     *
     * @param date Date for the shifts
     * @param shiftNames Array of shift names to create
     * @return List of created shifts (may be smaller than input if some names not found)
     */
    public static List<Shift> createNamedShifts(LocalDate date, String... shiftNames) {
        List<Shift> shifts = new ArrayList<>(shiftNames.length);

        for (String shiftName : shiftNames) {
            Shift shift = createShift(shiftName, date);
            if (shift != null) {
                shifts.add(shift);
            } else {
                Log.w(TAG, "Shift not found: " + shiftName);
            }
        }

        return shifts;
    }

    /**
     * Creates a custom shift with specified parameters.
     *
     * @param shiftType Type of shift (from ShiftTypeFactory or custom)
     * @param date Shift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New Shift
     */
    public static Shift createCustomShift(ShiftType shiftType, LocalDate date,
                                          boolean isStop, List<HalfTeam> teams) {
        if (shiftType == null) {
            Log.e(TAG, "Cannot create shift with null ShiftType");
            return null;
        }

        Shift shift = new Shift(shiftType, date);
        shift.setStop(isStop);

        if (teams != null) {
            for (HalfTeam team : teams) {
                shift.addTeam(team);
            }
        }

        return shift;
    }

    /**
     * Creates a custom shift with teams, using a shift type from the factory.
     *
     * @param shiftIndex Index of shift type in factory
     * @param date Shift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New Shift, or null if shift type not found
     */
    public static Shift createCustomShift(int shiftIndex, LocalDate date,
                                          boolean isStop, List<HalfTeam> teams) {
        ShiftType shiftType = ShiftTypeFactory.getShiftType(shiftIndex);
        return createCustomShift(shiftType, date, isStop, teams);
    }

    /**
     * Creates a custom shift with teams, using a named shift type from the factory.
     *
     * @param shiftName Name of shift type in factory
     * @param date Shift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New Shift, or null if shift type not found
     */
    public static Shift createCustomShift(String shiftName, LocalDate date,
                                          boolean isStop, List<HalfTeam> teams) {
        ShiftType shiftType = ShiftTypeFactory.getShiftType(shiftName);
        return createCustomShift(shiftType, date, isStop, teams);
    }

    /**
     * Utility method to check if ShiftTypeFactory is properly initialized.
     *
     * @return true if factory is ready to use
     */
    public static boolean isFactoryReady() {
        return ShiftTypeFactory.isInitialized() && ShiftTypeFactory.getShiftCount() > 0;
    }

    /**
     * Gets information about available shift types for debugging.
     *
     * @return String with shift type information
     */
    public static String getAvailableShiftsInfo() {
        if (!isFactoryReady()) {
            return "ShiftTypeFactory not initialized";
        }

        StringBuilder info = new StringBuilder();
        info.append("Available shifts (").append(ShiftTypeFactory.getShiftCount()).append("):\n");

        List<ShiftType> shiftTypes = ShiftTypeFactory.getAllShiftTypes();
        for (int i = 0; i < shiftTypes.size(); i++) {
            ShiftType shift = shiftTypes.get(i);
            info.append("  [").append(i).append("] ")
                    .append(shift.getName())
                    .append(" (").append(shift.getFormattedStartTime())
                    .append("-").append(shift.getFormattedEndTime()).append(")\n");
        }

        return info.toString();
    }

    /**
     * Builder pattern for creating shifts with fluent interface.
     */
    public static class Builder {
        private int shiftIndex = -1;
        private String shiftName;
        private ShiftType customShiftType;
        private LocalDate date;
        private boolean isStop = false;
        private List<HalfTeam> teams = new ArrayList<>();

        /**
         * Sets shift type by factory index.
         */
        public Builder withShiftIndex(int index) {
            this.shiftIndex = index;
            this.shiftName = null;
            this.customShiftType = null;
            return this;
        }

        /**
         * Sets shift type by name from factory.
         */
        public Builder withShiftName(String name) {
            this.shiftName = name;
            this.shiftIndex = -1;
            this.customShiftType = null;
            return this;
        }

        /**
         * Sets custom shift type (not from factory).
         */
        public Builder withCustomShiftType(ShiftType shiftType) {
            this.customShiftType = shiftType;
            this.shiftIndex = -1;
            this.shiftName = null;
            return this;
        }

        /**
         * Sets the shift date.
         */
        public Builder forDate(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * Marks the shift as during plant stop.
         */
        public Builder asStop() {
            this.isStop = true;
            return this;
        }

        /**
         * Adds a team to the shift.
         */
        public Builder addTeam(HalfTeam team) {
            if (team != null) {
                this.teams.add(team);
            }
            return this;
        }

        /**
         * Adds multiple teams to the shift.
         */
        public Builder addTeams(List<HalfTeam> teams) {
            if (teams != null) {
                this.teams.addAll(teams);
            }
            return this;
        }

        /**
         * Builds the shift with configured parameters.
         *
         * @return Created Shift, or null if configuration is invalid
         */
        public Shift build() {
            if (date == null) {
                Log.e(TAG, "Cannot build shift without date");
                return null;
            }

            ShiftType shiftType = null;

            // Determine shift type from configuration
            if (customShiftType != null) {
                shiftType = customShiftType;
            } else if (shiftName != null) {
                shiftType = ShiftTypeFactory.getShiftType(shiftName);
            } else if (shiftIndex >= 0) {
                shiftType = ShiftTypeFactory.getShiftType(shiftIndex);
            }

            if (shiftType == null) {
                Log.e(TAG, "Cannot determine shift type for builder");
                return null;
            }

            return createCustomShift(shiftType, date, isStop, teams);
        }
    }
}