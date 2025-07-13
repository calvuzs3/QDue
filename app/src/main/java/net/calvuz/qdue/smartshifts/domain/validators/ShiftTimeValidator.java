package net.calvuz.qdue.smartshifts.domain.validators;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Validator for shift time conflicts and coverage
 */
@Singleton
public class ShiftTimeValidator {

    @Inject
    public ShiftTimeValidator() {}

    /**
     * Validate time format (HH:mm)
     */
    public boolean isValidTimeFormat(String time) {
        if (time == null || time.length() != 5) return false;

        try {
            String[] parts = time.split(":");
            if (parts.length != 2) return false;

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if start time is before end time
     */
    public boolean isValidTimeRange(String startTime, String endTime) {
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            return false;
        }

        int startMinutes = timeToMinutes(startTime);
        int endMinutes = timeToMinutes(endTime);

        // Allow night shifts that span midnight
        if (startMinutes > endMinutes) {
            // This is a night shift (e.g., 22:00 to 06:00)
            return true;
        }

        return startMinutes < endMinutes;
    }

    /**
     * Convert time string to minutes since midnight
     */
    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }

    /**
     * Check for time overlaps between shifts
     * PLACEHOLDER for future implementation
     */
    public boolean hasTimeOverlap(String start1, String end1, String start2, String end2) {
        // TODO: Implement overlap detection logic
        return false;
    }

    /**
     * Calculate shift duration in minutes
     */
    public int calculateShiftDuration(String startTime, String endTime) {
        if (!isValidTimeRange(startTime, endTime)) {
            return 0;
        }

        int startMinutes = timeToMinutes(startTime);
        int endMinutes = timeToMinutes(endTime);

        if (startMinutes > endMinutes) {
            // Night shift spanning midnight
            return (24 * 60) - startMinutes + endMinutes;
        }

        return endMinutes - startMinutes;
    }
}