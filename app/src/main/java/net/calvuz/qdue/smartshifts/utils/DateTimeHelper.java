package net.calvuz.qdue.smartshifts.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for date and time operations
 */
@Singleton
public class DateTimeHelper {

    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

    @Inject
    public DateTimeHelper() {}

    /**
     * Parse date string in ISO format
     */
    public LocalDate parseIsoDate(String dateString) {
        try {
            return LocalDate.parse(dateString, ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Format date for display
     */
    public String formatDisplayDate(LocalDate date) {
        return date.format(DISPLAY_DATE);
    }

    /**
     * Format date for long display
     */
    public String formatLongDate(LocalDate date) {
        return date.format(LONG_DATE);
    }

    /**
     * Get days between two dates
     */
    public long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Check if date is today
     */
    public boolean isToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }

    /**
     * Check if date is in current month
     */
    public boolean isCurrentMonth(LocalDate date) {
        LocalDate now = LocalDate.now();
        return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
    }

    /**
     * Get start of month
     */
    public LocalDate getMonthStart(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Get end of month
     */
    public LocalDate getMonthEnd(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }
}
