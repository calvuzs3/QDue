package net.calvuz.qdue.ui.features.calendar.models;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CalendarRange - Represents a date range for calendar operations.
 */
public class CalendarRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public CalendarRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    public LocalDate getStartDate() { return startDate; }

    @NonNull
    public LocalDate getEndDate() { return endDate; }

    public boolean contains(@NonNull LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public long getDayCount() {
        return startDate.until(endDate).getDays() + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarRange that = (CalendarRange) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @NonNull
    @Override
    public String toString() {
        return "CalendarRange{" + startDate + " to " + endDate + '}';
    }
}
