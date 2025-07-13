package net.calvuz.qdue.smartshifts.ui.calendar.models;

import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;

import java.time.LocalDate;

/**
 * Model representing a day in the calendar view
 */
public class CalendarDay {

    private final LocalDate date;
    private final int dayOfMonth;
    private final boolean isCurrentMonth;
    private final boolean isToday;
    private final SmartShiftEvent shift;

    public CalendarDay(LocalDate date, int dayOfMonth, boolean isCurrentMonth,
                       boolean isToday, SmartShiftEvent shift) {
        this.date = date;
        this.dayOfMonth = dayOfMonth;
        this.isCurrentMonth = isCurrentMonth;
        this.isToday = isToday;
        this.shift = shift;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public int getDayOfMonth() { return dayOfMonth; }
    public boolean isCurrentMonth() { return isCurrentMonth; }
    public boolean isToday() { return isToday; }
    public SmartShiftEvent getShift() { return shift; }

    /**
     * Check if day has a shift
     */
    public boolean hasShift() {
        return shift != null;
    }

    /**
     * Get shift type ID if available
     */
    public String getShiftTypeId() {
        return shift != null ? shift.shiftTypeId : null;
    }

    /**
     * Check if this is a working day
     */
    public boolean isWorkingDay() {
        return hasShift() && !"rest".equals(getShiftTypeId());
    }

    /**
     * Get shift start time
     */
    public String getShiftStartTime() {
        return shift != null ? shift.startTime : null;
    }

    /**
     * Get shift end time
     */
    public String getShiftEndTime() {
        return shift != null ? shift.endTime : null;
    }

    @Override
    public String toString() {
        return "CalendarDay{" +
                "date=" + date +
                ", dayOfMonth=" + dayOfMonth +
                ", isCurrentMonth=" + isCurrentMonth +
                ", isToday=" + isToday +
                ", hasShift=" + hasShift() +
                '}';
    }
}
