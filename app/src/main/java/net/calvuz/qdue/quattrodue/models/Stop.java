package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a plant stop during a specific period.
 *
 * A stop is defined by start date/shift and end date/shift.
 * Supports date range operations and overlap detection.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public final class Stop {

    private static final String TAG = Stop.class.getSimpleName();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Stop properties - start period
    public final int year;
    public final int month;
    public final int day;
    public final int shift;

    // Stop properties - end period
    public final int endyear;
    public final int endmonth;
    public final int endday;
    public final int endshift;

    // LocalDate objects for easier calculations
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Creates a new stop with specified start and end periods.
     *
     * @param year Start year
     * @param month Start month (1-12)
     * @param day Start day (1-31)
     * @param shift Start shift (1-3)
     * @param endyear End year
     * @param endmonth End month (1-12)
     * @param endday End day (1-31)
     * @param endshift End shift (1-3)
     */
    public Stop(int year, int month, int day, int shift,
                int endyear, int endmonth, int endday, int endshift) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.shift = shift;
        this.endyear = endyear;
        this.endmonth = endmonth;
        this.endday = endday;
        this.endshift = endshift;

        // Create LocalDate objects for easier calculations
        this.startDate = LocalDate.of(year, month, day);

        // Handle day 32 case (used as workaround in old implementation)
        int adjustedEndDay = endday;
        if (adjustedEndDay > 31) {
            // Add one month and set day to 1
            LocalDate nextMonth = LocalDate.of(endyear, endmonth, 1).plusMonths(1);
            this.endDate = nextMonth;
        } else {
            this.endDate = LocalDate.of(endyear, endmonth, adjustedEndDay);
        }
    }

    /**
     * Alternative constructor using LocalDate objects.
     *
     * @param startDate Start date
     * @param startShift Start shift (1-3)
     * @param endDate End date
     * @param endShift End shift (1-3)
     */
    public Stop(LocalDate startDate, int startShift, LocalDate endDate, int endShift) {
        this.year = startDate.getYear();
        this.month = startDate.getMonthValue();
        this.day = startDate.getDayOfMonth();
        this.shift = startShift;

        this.endyear = endDate.getYear();
        this.endmonth = endDate.getMonthValue();
        this.endday = endDate.getDayOfMonth();
        this.endshift = endShift;

        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Checks if a specific date is included in the stop period.
     *
     * @param date Date to verify
     * @return true if date is within the stop period
     */
    public boolean includes(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if this stop overlaps with another stop.
     *
     * @param other Other stop to verify
     * @return true if there's overlap
     */
    public boolean overlaps(Stop other) {
        return includes(other.startDate) || includes(other.endDate) ||
                other.includes(startDate) || other.includes(endDate);
    }

    /**
     * Calculates the duration of the stop in days.
     *
     * @return Number of days in the stop
     */
    public int getDurationInDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }

    /**
     * @return Start date as LocalDate object
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * @return End date as LocalDate object
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" +
                startDate.format(DATE_FORMATTER) + "(" + shift + ")" +
                " - " +
                endDate.format(DATE_FORMATTER) + "(" + endshift + ")" +
                "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Stop other = (Stop) obj;
        return year == other.year &&
                month == other.month &&
                day == other.day &&
                shift == other.shift &&
                endyear == other.endyear &&
                endmonth == other.endmonth &&
                endday == other.endday &&
                endshift == other.endshift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, shift, endyear, endmonth, endday, endshift);
    }
}