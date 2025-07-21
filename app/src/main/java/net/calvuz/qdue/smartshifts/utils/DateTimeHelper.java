package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for date and time operations in SmartShifts.
 *
 * Provides utility methods for:
 * - Date/time formatting and parsing
 * - Timestamp operations
 * - LocalDate/LocalDateTime conversions
 * - Time zone handling
 * - Duration calculations
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class DateTimeHelper {

    private static final String TAG = "DateTimeHelper";

    // Standard date/time formats
    public static final String FORMAT_DATE_ISO = "yyyy-MM-dd";
    public static final String FORMAT_TIME_24H = "HH:mm";
    public static final String FORMAT_DATETIME_ISO = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String FORMAT_TIMESTAMP = "yyyyMMdd_HHmmss";
    public static final String FORMAT_DISPLAY_DATE = "dd/MM/yyyy";
    public static final String FORMAT_DISPLAY_DATETIME = "dd/MM/yyyy HH:mm";
    public static final String FORMAT_DISPLAY_TIME = "HH:mm";

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_DATE_ISO);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_TIME_24H);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_DATETIME_ISO);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_TIMESTAMP);
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_DISPLAY_DATE);
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_DISPLAY_DATETIME);

    // SimpleDateFormat for legacy compatibility
    private static final SimpleDateFormat LEGACY_DATE_FORMAT = new SimpleDateFormat(FORMAT_DATE_ISO, Locale.getDefault());
    private static final SimpleDateFormat LEGACY_TIMESTAMP_FORMAT = new SimpleDateFormat(FORMAT_TIMESTAMP, Locale.getDefault());

    // Private constructor to prevent instantiation
    private DateTimeHelper() {
        throw new UnsupportedOperationException("DateTimeHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // DATE FORMATTING METHODS
    // ============================================

    /**
     * Format LocalDate to ISO string (yyyy-MM-dd)
     */
    @NonNull
    public static String formatDate(@NonNull LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Format LocalTime to 24h string (HH:mm)
     */
    @NonNull
    public static String formatTime(@NonNull LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    /**
     * Format LocalDateTime to ISO string (yyyy-MM-dd'T'HH:mm:ss)
     */
    @NonNull
    public static String formatDateTime(@NonNull LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Format timestamp to readable date string
     */
    @NonNull
    public static String formatDate(long timestamp) {
        return LEGACY_DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Format timestamp with custom pattern
     */
    @NonNull
    public static String formatTimestamp(long timestamp, @NonNull String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

    /**
     * Format timestamp for file names (yyyyMMdd_HHmmss)
     */
    @NonNull
    public static String formatTimestampForFileName(long timestamp) {
        return LEGACY_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    /**
     * Format LocalDate for display (dd/MM/yyyy)
     */
    @NonNull
    public static String formatDateForDisplay(@NonNull LocalDate date) {
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    /**
     * Format LocalDateTime for display (dd/MM/yyyy HH:mm)
     */
    @NonNull
    public static String formatDateTimeForDisplay(@NonNull LocalDateTime dateTime) {
        return dateTime.format(DISPLAY_DATETIME_FORMATTER);
    }

    // ============================================
    // DATE PARSING METHODS
    // ============================================

    /**
     * Parse ISO date string to LocalDate
     */
    @NonNull
    public static LocalDate parseDate(@NonNull String dateString) throws DateTimeParseException {
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }

    /**
     * Parse time string to LocalTime
     */
    @NonNull
    public static LocalTime parseTime(@NonNull String timeString) throws DateTimeParseException {
        return LocalTime.parse(timeString, TIME_FORMATTER);
    }

    /**
     * Parse ISO datetime string to LocalDateTime
     */
    @NonNull
    public static LocalDateTime parseDateTime(@NonNull String dateTimeString) throws DateTimeParseException {
        return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
    }

    /**
     * Safe date parsing with fallback
     */
    public static LocalDate parseDateSafe(@NonNull String dateString, LocalDate fallback) {
        try {
            return parseDate(dateString);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    /**
     * Safe time parsing with fallback
     */
    public static LocalTime parseTimeSafe(@NonNull String timeString, LocalTime fallback) {
        try {
            return parseTime(timeString);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    // ============================================
    // TIMESTAMP CONVERSIONS
    // ============================================

    /**
     * Convert LocalDate to timestamp (start of day)
     */
    public static long toTimestamp(@NonNull LocalDate date) {
        return date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Convert LocalDateTime to timestamp
     */
    public static long toTimestamp(@NonNull LocalDateTime dateTime) {
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Convert timestamp to LocalDate
     */
    @NonNull
    public static LocalDate fromTimestamp(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Convert timestamp to LocalDateTime
     */
    @NonNull
    public static LocalDateTime fromTimestampToDateTime(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    // ============================================
    // DATE CALCULATIONS
    // ============================================

    /**
     * Get current timestamp
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Get current date
     */
    @NonNull
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * Get current date and time
     */
    @NonNull
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    /**
     * Check if date is today
     */
    public static boolean isToday(@NonNull LocalDate date) {
        return date.equals(LocalDate.now());
    }

    /**
     * Check if date is yesterday
     */
    public static boolean isYesterday(@NonNull LocalDate date) {
        return date.equals(LocalDate.now().minusDays(1));
    }

    /**
     * Check if date is tomorrow
     */
    public static boolean isTomorrow(@NonNull LocalDate date) {
        return date.equals(LocalDate.now().plusDays(1));
    }

    /**
     * Get days between two dates
     */
    public static long daysBetween(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Get months between two dates
     */
    public static long monthsBetween(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate);
    }

    /**
     * Get first day of month
     */
    @NonNull
    public static LocalDate getFirstDayOfMonth(@NonNull LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Get last day of month
     */
    @NonNull
    public static LocalDate getLastDayOfMonth(@NonNull LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * Get first day of week (Monday)
     */
    @NonNull
    public static LocalDate getFirstDayOfWeek(@NonNull LocalDate date) {
        return date.with(java.time.DayOfWeek.MONDAY);
    }

    /**
     * Get last day of week (Sunday)
     */
    @NonNull
    public static LocalDate getLastDayOfWeek(@NonNull LocalDate date) {
        return date.with(java.time.DayOfWeek.SUNDAY);
    }

    // ============================================
    // TIME VALIDATION METHODS
    // ============================================

    /**
     * Validate time format (HH:mm)
     */
    public static boolean isValidTimeFormat(@NonNull String timeString) {
        try {
            parseTime(timeString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validate date format (yyyy-MM-dd)
     */
    public static boolean isValidDateFormat(@NonNull String dateString) {
        try {
            parseDate(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Check if time is within working hours (06:00 - 22:00)
     */
    public static boolean isWorkingHours(@NonNull LocalTime time) {
        LocalTime startWork = LocalTime.of(6, 0);
        LocalTime endWork = LocalTime.of(22, 0);
        return !time.isBefore(startWork) && !time.isAfter(endWork);
    }

    /**
     * Check if time range is valid (start before end)
     */
    public static boolean isValidTimeRange(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        return startTime.isBefore(endTime);
    }

    // ============================================
    // DISPLAY HELPERS
    // ============================================

    /**
     * Get relative date string (Today, Yesterday, Tomorrow, or date)
     */
    @NonNull
    public static String getRelativeDateString(@NonNull LocalDate date) {
        if (isToday(date)) {
            return "Oggi";
        } else if (isYesterday(date)) {
            return "Ieri";
        } else if (isTomorrow(date)) {
            return "Domani";
        } else {
            return formatDateForDisplay(date);
        }
    }

    /**
     * Get time ago string from timestamp
     */
    @NonNull
    public static String getTimeAgoString(long timestamp) {
        long now = getCurrentTimestamp();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days == 1 ? "1 giorno fa" : days + " giorni fa";
        } else if (hours > 0) {
            return hours == 1 ? "1 ora fa" : hours + " ore fa";
        } else if (minutes > 0) {
            return minutes == 1 ? "1 minuto fa" : minutes + " minuti fa";
        } else {
            return "Ora";
        }
    }

    /**
     * Format duration in human readable format
     */
    @NonNull
    public static String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "m";
        } else if (minutes > 0) {
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return seconds + "s";
        }
    }

    // ============================================
    // SHIFT-SPECIFIC HELPERS
    // ============================================

    /**
     * Check if date is weekend (Saturday or Sunday)
     */
    public static boolean isWeekend(@NonNull LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Get week number of year
     */
    public static int getWeekOfYear(@NonNull LocalDate date) {
        return date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
    }

    /**
     * Get day of week as integer (1=Monday, 7=Sunday)
     */
    public static int getDayOfWeekNumber(@NonNull LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /**
     * Check if date falls within date range (inclusive)
     */
    public static boolean isDateInRange(@NonNull LocalDate date, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Calculate shift overlap in minutes
     */
    public static long calculateOverlapMinutes(@NonNull LocalTime start1, @NonNull LocalTime end1,
                                               @NonNull LocalTime start2, @NonNull LocalTime end2) {
        // Handle night shifts that cross midnight
        LocalDateTime base = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

        LocalDateTime dt1Start = base.with(start1);
        LocalDateTime dt1End = end1.isBefore(start1) ? base.plusDays(1).with(end1) : base.with(end1);

        LocalDateTime dt2Start = base.with(start2);
        LocalDateTime dt2End = end2.isBefore(start2) ? base.plusDays(1).with(end2) : base.with(end2);

        LocalDateTime overlapStart = dt1Start.isAfter(dt2Start) ? dt1Start : dt2Start;
        LocalDateTime overlapEnd = dt1End.isBefore(dt2End) ? dt1End : dt2End;

        if (overlapStart.isBefore(overlapEnd)) {
            return java.time.temporal.ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
        } else {
            return 0;
        }
    }
}