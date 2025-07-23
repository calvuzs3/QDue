package net.calvuz.qdue.smartshifts.domain.models;

/**
 * Domain model representing shift information for a specific day
 */
public class  ShiftInfo {

    private final String shiftType;
    private final int cycleDay;

    public ShiftInfo(String shiftType, int cycleDay) {
        this.shiftType = shiftType;
        this.cycleDay = cycleDay;
    }

    public String getShiftType() { return shiftType; }
    public int getCycleDay() { return cycleDay; }

    /**
     * Check if this is a working shift (not rest)
     */
    public boolean isWorkingShift() {
        return !shiftType.equals("rest");
    }

    /**
     * Check if this is a rest period
     */
    public boolean isRestPeriod() {
        return shiftType.equals("rest");
    }

    @Override
    public String toString() {
        return "ShiftInfo{" +
                "shiftType='" + shiftType + '\'' +
                ", cycleDay=" + cycleDay +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShiftInfo shiftInfo = (ShiftInfo) o;
        return cycleDay == shiftInfo.cycleDay && shiftType.equals(shiftInfo.shiftType);
    }

    @Override
    public int hashCode() {
        return shiftType.hashCode() * 31 + cycleDay;
    }
}
