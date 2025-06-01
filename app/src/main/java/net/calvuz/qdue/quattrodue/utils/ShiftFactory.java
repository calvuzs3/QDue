package net.calvuz.qdue.quattrodue.utils;

import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating shifts (Shift objects).
 *
 * Provides convenient methods for creating standard shifts
 * (morning, afternoon, night) and custom shifts with teams.
 * Supports both single shift and daily shift set creation.
 *
 * @author Updated 21/05/2025
 */
public final class ShiftFactory {

    private static final String TAG = ShiftFactory.class.getSimpleName();

    // Prevent instantiation
    private ShiftFactory() {}

    /**
     * Creates a standard shift (morning, afternoon or night) for a specific date.
     *
     * @param type Shift type (1=morning, 2=afternoon, 3=night)
     * @param date Shift date
     * @return New Shift
     * @throws IllegalArgumentException if shift type is invalid
     */
    public static Shift createStandardShift(int type, LocalDate date) {
        ShiftType shiftType;

        switch (type) {
            case 1:
                shiftType = ShiftTypeFactory.MORNING;
                break;
            case 2:
                shiftType = ShiftTypeFactory.AFTERNOON;
                break;
            case 3:
                shiftType = ShiftTypeFactory.NIGHT;
                break;
            default:
                throw new IllegalArgumentException("Invalid shift type: " + type);
        }

        return new Shift(shiftType, date);
    }

    /**
     * Creates all three standard shifts for a specific date.
     *
     * @param date Date for the shifts
     * @return List of 3 shifts (morning, afternoon, night)
     */
    public static List<Shift> createDailyShifts(LocalDate date) {
        List<Shift> shifts = new ArrayList<>(3);
        shifts.add(createStandardShift(1, date));
        shifts.add(createStandardShift(2, date));
        shifts.add(createStandardShift(3, date));
        return shifts;
    }

    /**
     * Creates a custom shift with specified parameters.
     *
     * @param shiftType Type of shift
     * @param date Shift date
     * @param isStop Indicates if shift is during plant stop
     * @param teams Teams assigned to the shift
     * @return New Shift
     */
    public static Shift createCustomShift(ShiftType shiftType, LocalDate date,
                                          boolean isStop, List<HalfTeam> teams) {
        Shift shift = new Shift(shiftType, date);
        shift.setStop(isStop);

        if (teams != null) {
            for (HalfTeam team : teams) {
                shift.addTeam(team);
            }
        }

        return shift;
    }
}