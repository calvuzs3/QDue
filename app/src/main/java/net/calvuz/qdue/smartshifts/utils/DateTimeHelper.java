package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for date and time operations in SmartShifts.
 *
 * Provides utility methods for:
 * - Date and time parsing and formatting
 * - Shift time calculations
 * - Working hours validation
 * - Duration and interval calculations
 * - Timezone handling
 * - Calendar utilities
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class DateTimeHelper {

    private static final String TAG = "DateTimeHelper";

    // Standard formatters
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Private constructor to prevent instantiation
    private DateTimeHelper() {
        throw new UnsupportedOperationException("DateTimeHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // PARSING METHODS
    // ============================================

    /**
     * Parse time string to LocalTime (HH:mm format)
     * Required by ContinuousCycleValidator
     */
    @NonNull
    public static LocalTime parseTime(@NonNull String timeString) {
        if (StringHelper.isEmpty(timeString)) {
            throw new IllegalArgumentException("Time string cannot be empty");
        }

        try {
            return LocalTime.parse(timeString.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + timeString + " (expected HH:mm)", e);
        }
    }

    /**
     * Parse date string to LocalDate (yyyy-MM-dd format)
     * Required by ContinuousCycleValidator
     */
    @NonNull
    public static LocalDate parseDate(@NonNull String dateString) {
        if (StringHelper.isEmpty(dateString)) {
            throw new IllegalArgumentException("Date string cannot be empty");
        }

        try {
            return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString + " (expected yyyy-MM-dd)", e);
        }
    }

    /**
     * Parse datetime string to LocalDateTime
     */
    @NonNull
    public static LocalDateTime parseDateTime(@NonNull String dateTimeString) {
        if (StringHelper.isEmpty(dateTimeString)) {
            throw new IllegalArgumentException("DateTime string cannot be empty");
        }

        try {
            return LocalDateTime.parse(dateTimeString.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid datetime format: " + dateTimeString + " (expected yyyy-MM-ddTHH:mm:ss)", e);
        }
    }

    /**
     * Safe time parsing with fallback
     */
    @Nullable
    public static LocalTime parseTimeSafe(@Nullable String timeString) {
        try {
            return parseTime(timeString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Safe date parsing with fallback
     */
    @Nullable
    public static LocalDate parseDateSafe(@Nullable String dateString) {
        try {
            return parseDate(dateString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ============================================
    // FORMATTING METHODS
    // ============================================

    /**
     * Format time to string (HH:mm)
     */
    @NonNull
    public static String formatTime(@NonNull LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    /**
     * Format date to string (yyyy-MM-dd)
     */
    @NonNull
    public static String formatDate(@NonNull LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Format datetime to string
     */
    @NonNull
    public static String formatDateTime(@NonNull LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Format time for display (HH:mm)
     */
    @NonNull
    public static String formatTimeForDisplay(@NonNull LocalTime time) {
        return time.format(DISPLAY_TIME_FORMATTER);
    }

    public static String formatTimestamp(@NonNull long millis, @NonNull String format) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Format date for display (dd/MM/yyyy)
     */
    @NonNull
    public static String formatDateForDisplay(@NonNull LocalDate date) {
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    /**
     * Format date with custom pattern
     */
    @NonNull
    public static String formatDate(@NonNull LocalDate date, @NonNull String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return date.format(formatter);
    }

    /**
     * Format time with custom pattern
     */
    @NonNull
    public static String formatTime(@NonNull LocalTime time, @NonNull String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return time.format(formatter);
    }

    // ============================================
    // SHIFT TIME CALCULATIONS
    // ============================================

    /**
     * Calculate shift duration in minutes
     * Handles night shifts that cross midnight
     */
    public static int calculateShiftDurationMinutes(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        if (endTime.isAfter(startTime)) {
            // Same day shift
            return (int) Duration.between(startTime, endTime).toMinutes();
        } else if (endTime.isBefore(startTime)) {
            // Night shift crossing midnight
            Duration toMidnight = Duration.between(startTime, LocalTime.MIDNIGHT);
            Duration fromMidnight = Duration.between(LocalTime.MIDNIGHT, endTime);
            return (int) (toMidnight.toMinutes() + fromMidnight.toMinutes());
        } else {
            // Start and end are the same - could be 24h shift or 0h shift
            return 0; // Let caller decide if this is valid
        }
    }

    /**
     * Calculate shift duration in hours
     */
    public static double calculateShiftDurationHours(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        return calculateShiftDurationMinutes(startTime, endTime) / 60.0;
    }

    /**
     * Check if shift crosses midnight
     */
    public static boolean isNightShift(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        return endTime.isBefore(startTime) || endTime.equals(startTime);
    }

    /**
     * Get shift end time on correct date (handles night shifts)
     */
    @NonNull
    public static LocalDateTime getShiftEndDateTime(@NonNull LocalDate shiftDate,
                                                    @NonNull LocalTime startTime,
                                                    @NonNull LocalTime endTime) {
        if (isNightShift(startTime, endTime)) {
            // Night shift - end time is next day
            return LocalDateTime.of(shiftDate.plusDays(1), endTime);
        } else {
            // Day shift - end time is same day
            return LocalDateTime.of(shiftDate, endTime);
        }
    }

    /**
     * Calculate total minutes in a day covered by shift
     */
    public static int calculateCoverageMinutes(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        int duration = calculateShiftDurationMinutes(startTime, endTime);

        // For validation, ensure we don't exceed 24 hours
        return Math.min(duration, 24 * 60);
    }

    // ============================================
    // TIME RANGE VALIDATION
    // ============================================

    /**
     * Check if time falls within a range (handles night shifts)
     */
    public static boolean isTimeInRange(@NonNull LocalTime time,
                                        @NonNull LocalTime rangeStart,
                                        @NonNull LocalTime rangeEnd) {
        if (rangeEnd.isAfter(rangeStart)) {
            // Same day range
            return !time.isBefore(rangeStart) && !time.isAfter(rangeEnd);
        } else {
            // Night range crossing midnight
            return !time.isBefore(rangeStart) || !time.isAfter(rangeEnd);
        }
    }

    /**
     * Check if two time ranges overlap
     */
    public static boolean doTimeRangesOverlap(@NonNull LocalTime start1, @NonNull LocalTime end1,
                                              @NonNull LocalTime start2, @NonNull LocalTime end2) {
        // Convert to minutes for easier calculation
        int start1Min = start1.getHour() * 60 + start1.getMinute();
        int end1Min = end1.getHour() * 60 + end1.getMinute();
        int start2Min = start2.getHour() * 60 + start2.getMinute();
        int end2Min = end2.getHour() * 60 + end2.getMinute();

        // Handle night shifts
        if (end1Min <= start1Min) end1Min += 24 * 60;
        if (end2Min <= start2Min) end2Min += 24 * 60;

        // Check overlap
        return start1Min < end2Min && start2Min < end1Min;
    }

    /**
     * Find gap between two consecutive shifts
     */
    public static int calculateGapMinutes(@NonNull LocalTime firstEnd, @NonNull LocalTime secondStart) {
        if (secondStart.isAfter(firstEnd)) {
            // Same day - simple calculation
            return (int) Duration.between(firstEnd, secondStart).toMinutes();
        } else {
            // Gap crosses midnight
            Duration toMidnight = Duration.between(firstEnd, LocalTime.MIDNIGHT);
            Duration fromMidnight = Duration.between(LocalTime.MIDNIGHT, secondStart);
            return (int) (toMidnight.toMinutes() + fromMidnight.toMinutes());
        }
    }

    // ============================================
    // CALENDAR UTILITIES
    // ============================================

    /**
     * Get current date
     */
    @NonNull
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * Get current time
     */
    @NonNull
    public static LocalTime getCurrentTime() {
        return LocalTime.now();
    }

    /**
     * Get current datetime
     */
    @NonNull
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    /**
     * Get first day of week based on preference
     */
    @NonNull
    public static LocalDate getFirstDayOfWeek(@NonNull LocalDate date, @NonNull String weekStartPreference) {
        DayOfWeek startDay;
        switch (weekStartPreference.toLowerCase()) {
            case "sunday":
                startDay = DayOfWeek.SUNDAY;
                break;
            case "saturday":
                startDay = DayOfWeek.SATURDAY;
                break;
            case "monday":
            default:
                startDay = DayOfWeek.MONDAY;
                break;
        }

        return date.with(TemporalAdjusters.previousOrSame(startDay));
    }

    /**
     * Get last day of week based on preference
     */
    @NonNull
    public static LocalDate getLastDayOfWeek(@NonNull LocalDate date, @NonNull String weekStartPreference) {
        return getFirstDayOfWeek(date, weekStartPreference).plusDays(6);
    }

    /**
     * Get first day of month
     */
    @NonNull
    public static LocalDate getFirstDayOfMonth(@NonNull LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Get last day of month
     */
    @NonNull
    public static LocalDate getLastDayOfMonth(@NonNull LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Generate date range between two dates
     */
    @NonNull
    public static List<LocalDate> getDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * Calculate days between two dates
     */
    public static long daysBetween(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calculate working days between two dates (excluding weekends)
     */
    public static long workingDaysBetween(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        long totalDays = daysBetween(startDate, endDate);
        long workingDays = 0;

        for (long i = 0; i <= totalDays; i++) {
            LocalDate current = startDate.plusDays(i);
            if (!isWeekend(current)) {
                workingDays++;
            }
        }

        return workingDays;
    }

    /**
     * Check if date is weekend
     */
    public static boolean isWeekend(@NonNull LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if date is weekday
     */
    public static boolean isWeekday(@NonNull LocalDate date) {
        return !isWeekend(date);
    }

    // ============================================
    // TIMEZONE UTILITIES
    // ============================================

    /**
     * Get current timezone
     */
    @NonNull
    public static ZoneId getCurrentTimeZone() {
        return ZoneId.systemDefault();
    }

    /**
     * Convert local datetime to zoned datetime
     */
    @NonNull
    public static ZonedDateTime toZonedDateTime(@NonNull LocalDateTime localDateTime, @NonNull ZoneId zoneId) {
        return localDateTime.atZone(zoneId);
    }

    /**
     * Convert to current timezone
     */
    @NonNull
    public static ZonedDateTime toCurrentTimeZone(@NonNull LocalDateTime localDateTime) {
        return toZonedDateTime(localDateTime, getCurrentTimeZone());
    }

    // ============================================
    // DURATION UTILITIES
    // ============================================

    /**
     * Format duration to human readable string
     */
    @NonNull
    public static String formatDuration(@NonNull Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }

    /**
     * Format duration in minutes to human readable string
     */
    @NonNull
    public static String formatDurationMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }

    /**
     * Parse duration string (e.g., "2h 30m" or "150m")
     */
    public static int parseDurationToMinutes(@NonNull String durationString) {
        if (StringHelper.isEmpty(durationString)) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }

        String trimmed = durationString.trim().toLowerCase();
        int totalMinutes = 0;

        // Handle "Xh Ym" format
        if (trimmed.contains("h") && trimmed.contains("m")) {
            String[] parts = trimmed.split("\\s+");
            for (String part : parts) {
                if (part.endsWith("h")) {
                    int hours = Integer.parseInt(part.substring(0, part.length() - 1));
                    totalMinutes += hours * 60;
                } else if (part.endsWith("m")) {
                    int minutes = Integer.parseInt(part.substring(0, part.length() - 1));
                    totalMinutes += minutes;
                }
            }
        }
        // Handle "Xh" format
        else if (trimmed.endsWith("h")) {
            int hours = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
            totalMinutes = hours * 60;
        }
        // Handle "Xm" format
        else if (trimmed.endsWith("m")) {
            totalMinutes = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
        }
        // Handle plain number (assume minutes)
        else {
            totalMinutes = Integer.parseInt(trimmed);
        }

        return totalMinutes;
    }

    // ============================================
    // VALIDATION HELPERS
    // ============================================

    /**
     * Validate that time is within business hours
     */
    public static boolean isWithinBusinessHours(@NonNull LocalTime time,
                                                @NonNull LocalTime businessStart,
                                                @NonNull LocalTime businessEnd) {
        return isTimeInRange(time, businessStart, businessEnd);
    }

    /**
     * Validate minimum time between shifts
     */
    public static boolean hasMinimumRestPeriod(@NonNull LocalTime firstShiftEnd,
                                               @NonNull LocalTime secondShiftStart,
                                               int minimumRestMinutes) {
        int gapMinutes = calculateGapMinutes(firstShiftEnd, secondShiftStart);
        return gapMinutes >= minimumRestMinutes;
    }

    /**
     * Validate maximum shift duration
     */
    public static boolean isWithinMaxDuration(@NonNull LocalTime startTime,
                                              @NonNull LocalTime endTime,
                                              int maxDurationMinutes) {
        int duration = calculateShiftDurationMinutes(startTime, endTime);
        return duration <= maxDurationMinutes;
    }

    // ============================================
    // SPECIALIZED METHODS FOR CONTINUOUS CYCLE VALIDATOR
    // ============================================

    /**
     * Convert time to minutes since midnight (for timeline array)
     * Specialized for ContinuousCycleValidator
     */
    public static int timeToMinutesSinceMidnight(@NonNull LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    /**
     * Convert minutes since midnight to LocalTime
     * Specialized for ContinuousCycleValidator
     */
    @NonNull
    public static LocalTime minutesSinceMidnightToTime(int minutes) {
        int hours = (minutes / 60) % 24;
        int mins = minutes % 60;
        return LocalTime.of(hours, mins);
    }

    /**
     * Create time range for continuous cycle analysis
     * Handles night shifts properly for timeline coverage
     */
    @NonNull
    public static TimeRange createTimeRange(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        int startMinutes = timeToMinutesSinceMidnight(startTime);
        int endMinutes = timeToMinutesSinceMidnight(endTime);

        // Handle night shifts
        if (endMinutes <= startMinutes && !endTime.equals(startTime)) {
            endMinutes += 24 * 60; // Add 24 hours for next day
        }

        return new TimeRange(startMinutes, endMinutes);
    }

    /**
     * Time range class for continuous cycle validation
     */
    public static class TimeRange {
        public final int startMinutes;
        public final int endMinutes;

        public TimeRange(int startMinutes, int endMinutes) {
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }

        public int getDurationMinutes() {
            return endMinutes - startMinutes;
        }

        public boolean contains(int minutes) {
            return minutes >= startMinutes && minutes < endMinutes;
        }

        public boolean overlaps(TimeRange other) {
            return startMinutes < other.endMinutes && other.startMinutes < endMinutes;
        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "TimeRange{%02d:%02d-%02d:%02d (%d-%d)}",
                    startMinutes / 60, startMinutes % 60,
                    endMinutes / 60, endMinutes % 60,
                    startMinutes, endMinutes);
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get readable day name in Italian
     */
    @NonNull
    public static String getDayNameItalian(@NonNull DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Lunedì";
            case TUESDAY: return "Martedì";
            case WEDNESDAY: return "Mercoledì";
            case THURSDAY: return "Giovedì";
            case FRIDAY: return "Venerdì";
            case SATURDAY: return "Sabato";
            case SUNDAY: return "Domenica";
            default: return dayOfWeek.name();
        }
    }

    /**
     * Get readable month name in Italian
     */
    @NonNull
    public static String getMonthNameItalian(int month) {
        String[] months = {
                "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
                "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
        };

        if (month >= 1 && month <= 12) {
            return months[month - 1];
        }

        return "Mese " + month;
    }

    /**
     * Check if year is leap year
     */
    public static boolean isLeapYear(int year) {
        return LocalDate.of(year, 1, 1).isLeapYear();
    }

    /**
     * Get days in month
     */
    public static int getDaysInMonth(int year, int month) {
        return LocalDate.of(year, month, 1).lengthOfMonth();
    }

    /**
     * Get week number of year
     */
    public static int getWeekOfYear(@NonNull LocalDate date) {
        return date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
    }
}