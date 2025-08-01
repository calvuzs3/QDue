package net.calvuz.qdue.core.domain.quattrodue.utils;

import net.calvuz.qdue.core.domain.quattrodue.models.HalfTeam;
import net.calvuz.qdue.core.domain.quattrodue.models.LegacyShift;
import net.calvuz.qdue.core.domain.quattrodue.models.LegacyShiftType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating shifts (LegacyShift objects).
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
     * @param shiftIndex LegacyShift index (0-based) from ShiftTypeFactory
     * @param date LegacyShift date
     * @return New LegacyShift, or null if shift type not found
     */
    public static LegacyShift createShift(int shiftIndex, LocalDate date) {
        LegacyShiftType legacyShiftType = ShiftTypeFactory.getShiftType(shiftIndex);

        if (legacyShiftType == null) {
            Log.w(TAG, "No shift type found at index " + shiftIndex);
            return null;
        }

        return new LegacyShift( legacyShiftType, date);
    }

    /**
     * Creates a shift using a named shift type from the dynamic factory.
     *
     * @param shiftName Name of the shift type
     * @param date LegacyShift date
     * @return New LegacyShift, or null if shift type not found
     */
    public static LegacyShift createShift(String shiftName, LocalDate date) {
        LegacyShiftType legacyShiftType = ShiftTypeFactory.getShiftType(shiftName);

        if (legacyShiftType == null) {
            Log.w(TAG, "No shift type found with name: " + shiftName);
            return null;
        }

        return new LegacyShift( legacyShiftType, date);
    }

    /**
     * Creates a standard shift by legacy index (for backward compatibility).
     *
     * @deprecated Use createShift(int, LocalDate) instead
     * @param type Legacy shift type (1=first, 2=second, 3=third shift)
     * @param date LegacyShift date
     * @return New LegacyShift
     * @throws IllegalArgumentException if shift type is invalid or not available
     */
    @Deprecated
    public static LegacyShift createStandardShift(int type, LocalDate date) {
        if (type < 1) {
            throw new IllegalArgumentException("LegacyShift type must be >= 1, got: " + type);
        }

        // Convert 1-based to 0-based index
        int shiftIndex = type - 1;
        int availableShifts = ShiftTypeFactory.getShiftCount();

        if (shiftIndex >= availableShifts) {
            throw new IllegalArgumentException(
                    "LegacyShift type " + type + " not available. Only " + availableShifts + " shifts configured."
            );
        }

        LegacyShift legacyShift = createShift(shiftIndex, date);
        if (legacyShift == null) {
            throw new IllegalArgumentException("Failed to create legacyShift for type: " + type);
        }

        return legacyShift;
    }

    /**
     * Creates all configured shifts for a specific date.
     * Uses all shift types available in the ShiftTypeFactory.
     *
     * @param date Date for the shifts
     * @return List of shifts (size depends on factory configuration)
     */
    public static List<LegacyShift> createDailyShifts(LocalDate date) {
        int shiftCount = ShiftTypeFactory.getShiftCount();
        List<LegacyShift> legacyShifts = new ArrayList<>(shiftCount);

        for (int i = 0; i < shiftCount; i++) {
            LegacyShift legacyShift = createShift(i, date);
            if (legacyShift != null) {
                legacyShifts.add( legacyShift );
            } else {
                Log.w(TAG, "Skipped null legacyShift at index " + i + " for date " + date);
            }
        }

        if (legacyShifts.isEmpty()) {
            Log.e(TAG, "No legacyShifts created for date " + date + ". ShiftTypeFactory not initialized?");
        }

        return legacyShifts;
    }

    /**
     * Creates all shifts with specific names for a date.
     *
     * @param date Date for the shifts
     * @param shiftNames Array of shift names to create
     * @return List of created shifts (may be smaller than input if some names not found)
     */
    public static List<LegacyShift> createNamedShifts(LocalDate date, String... shiftNames) {
        List<LegacyShift> legacyShifts = new ArrayList<>(shiftNames.length);

        for (String shiftName : shiftNames) {
            LegacyShift legacyShift = createShift(shiftName, date);
            if (legacyShift != null) {
                legacyShifts.add( legacyShift );
            } else {
                Log.w(TAG, "LegacyShift not found: " + shiftName);
            }
        }

        return legacyShifts;
    }

    /**
     * Creates a custom shift with specified parameters.
     *
     * @param legacyShiftType Type of shift (from ShiftTypeFactory or custom)
     * @param date LegacyShift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New LegacyShift
     */
    public static LegacyShift createCustomShift(LegacyShiftType legacyShiftType, LocalDate date,
                                                boolean isStop, List<HalfTeam> teams) {
        if (legacyShiftType == null) {
            Log.e(TAG, "Cannot create legacyShift with null LegacyShiftType");
            return null;
        }

        LegacyShift legacyShift = new LegacyShift( legacyShiftType, date);
        legacyShift.setStop(isStop);

        if (teams != null) {
            for (HalfTeam team : teams) {
                legacyShift.addTeam(team);
            }
        }

        return legacyShift;
    }

    /**
     * Creates a custom shift with teams, using a shift type from the factory.
     *
     * @param shiftIndex Index of shift type in factory
     * @param date LegacyShift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New LegacyShift, or null if shift type not found
     */
    public static LegacyShift createCustomShift(int shiftIndex, LocalDate date,
                                                boolean isStop, List<HalfTeam> teams) {
        LegacyShiftType legacyShiftType = ShiftTypeFactory.getShiftType(shiftIndex);
        return createCustomShift( legacyShiftType, date, isStop, teams);
    }

    /**
     * Creates a custom shift with teams, using a named shift type from the factory.
     *
     * @param shiftName Name of shift type in factory
     * @param date LegacyShift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New LegacyShift, or null if shift type not found
     */
    public static LegacyShift createCustomShift(String shiftName, LocalDate date,
                                                boolean isStop, List<HalfTeam> teams) {
        LegacyShiftType legacyShiftType = ShiftTypeFactory.getShiftType(shiftName);
        return createCustomShift( legacyShiftType, date, isStop, teams);
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

        List<LegacyShiftType> legacyShiftTypes = ShiftTypeFactory.getAllShiftTypes();
        for (int i = 0; i < legacyShiftTypes.size(); i++) {
            LegacyShiftType shift = legacyShiftTypes.get(i);
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
        private LegacyShiftType customLegacyShiftType;
        private LocalDate date;
        private boolean isStop = false;
        private List<HalfTeam> teams = new ArrayList<>();

        /**
         * Sets shift type by factory index.
         */
        public Builder withShiftIndex(int index) {
            this.shiftIndex = index;
            this.shiftName = null;
            this.customLegacyShiftType = null;
            return this;
        }

        /**
         * Sets shift type by name from factory.
         */
        public Builder withShiftName(String name) {
            this.shiftName = name;
            this.shiftIndex = -1;
            this.customLegacyShiftType = null;
            return this;
        }

        /**
         * Sets custom shift type (not from factory).
         */
        public Builder withCustomShiftType(LegacyShiftType legacyShiftType) {
            this.customLegacyShiftType = legacyShiftType;
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
         * @return Created LegacyShift, or null if configuration is invalid
         */
        public LegacyShift build() {
            if (date == null) {
                Log.e(TAG, "Cannot build shift without date");
                return null;
            }

            LegacyShiftType legacyShiftType = null;

            // Determine shift type from configuration
            if (customLegacyShiftType != null) {
                legacyShiftType = customLegacyShiftType;
            } else if (shiftName != null) {
                legacyShiftType = ShiftTypeFactory.getShiftType(shiftName);
            } else if (shiftIndex >= 0) {
                legacyShiftType = ShiftTypeFactory.getShiftType(shiftIndex);
            }

            if (legacyShiftType == null) {
                Log.e(TAG, "Cannot determine shift type for builder");
                return null;
            }

            return createCustomShift( legacyShiftType, date, isStop, teams);
        }
    }
}