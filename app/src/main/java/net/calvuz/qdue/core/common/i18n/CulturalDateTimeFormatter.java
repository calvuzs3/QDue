package net.calvuz.qdue.core.common.i18n;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CulturalDateTimeFormatter - Culturally Appropriate Date and Time Formatting.
 *
 * <p>Provides comprehensive date and time formatting that adapts to cultural preferences
 * for each supported locale. Includes special formatting for work schedule contexts
 * and quattrodue system terminology.</p>
 *
 * <h3>Cultural Adaptations by Language:</h3>
 * <ul>
 *   <li><strong>Italian (IT)</strong>: dd/MM/yyyy, 24-hour format, European conventions</li>
 *   <li><strong>English (US)</strong>: MM/dd/yyyy, 12-hour format with AM/PM</li>
 *   <li><strong>English (UK)</strong>: dd/MM/yyyy, 24-hour format</li>
 *   <li><strong>German (DE)</strong>: dd.MM.yyyy, 24-hour format, German conventions</li>
 *   <li><strong>French (FR)</strong>: dd/MM/yyyy, 24-hour format, French conventions</li>
 * </ul>
 *
 * <h3>Formatting Categories:</h3>
 * <ul>
 *   <li>Date formats (short, medium, long, full)</li>
 *   <li>Time formats (with cultural 12/24 hour preferences)</li>
 *   <li>DateTime combinations</li>
 *   <li>Relative time expressions (today, tomorrow, etc.)</li>
 *   <li>Work schedule specific formatting</li>
 *   <li>Duration and time range formatting</li>
 * </ul>
 */
public class CulturalDateTimeFormatter {

    private static final String TAG = "CulturalDateTimeFormatter";

    // Dependencies
    private final Context mContext;
    private final LocaleManager mLocaleManager;

    // Cached formatters for performance
    private final Map<String, DateTimeFormatter> mFormatterCache = new HashMap<>();

    // Current locale state
    private Locale mCurrentLocale;
    private String mCurrentLanguage;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor with dependency injection.
     *
     * @param context Application context for accessing resources
     * @param localeManager LocaleManager for current locale information
     */
    public CulturalDateTimeFormatter(@NonNull Context context, @NonNull LocaleManager localeManager) {
        this.mContext = context.getApplicationContext();
        this.mLocaleManager = localeManager;

        updateLocaleState();

        Log.d(TAG, "CulturalDateTimeFormatter initialized for locale: " + mCurrentLocale);
    }

    // ==================== LOCALE MANAGEMENT ====================

    /**
     * Update internal locale state. Call this when locale changes.
     */
    public void updateLocaleState() {
        mCurrentLocale = mLocaleManager.getCurrentLocale();
        mCurrentLanguage = mLocaleManager.getCurrentLanguageCode();

        // Clear formatter cache when locale changes
        mFormatterCache.clear();

        Log.d(TAG, "Locale state updated: " + mCurrentLocale);
    }

    // ==================== DATE FORMATTING ====================

    /**
     * Format date in culturally appropriate short format.
     * <p>
     * Examples:
     * - Italian: 15/03/2024
     * - English (US): 03/15/2024
     * - English (UK): 15/03/2024
     * - German: 15.03.2024
     *
     * @param date Date to format
     * @return Formatted date string
     */
    @NonNull
    public String formatDateShort(@NonNull LocalDate date) {
        String pattern = getDatePattern(DateFormatStyle.SHORT);
        DateTimeFormatter formatter = getCachedFormatter("date_short", pattern);
        return date.format(formatter);
    }

    /**
     * Format date in medium format with abbreviated month.
     * <p>
     * Examples:
     * - Italian: 15 mar 2024
     * - English: Mar 15, 2024
     * - German: 15. Mär. 2024
     *
     * @param date Date to format
     * @return Formatted date string
     */
    @NonNull
    public String formatDateMedium(@NonNull LocalDate date) {
        String pattern = getDatePattern(DateFormatStyle.MEDIUM);
        DateTimeFormatter formatter = getCachedFormatter("date_medium", pattern);
        return date.format(formatter);
    }

    /**
     * Format date in long format with full month name.
     * <p>
     * Examples:
     * - Italian: 15 marzo 2024
     * - English: March 15, 2024
     * - German: 15. März 2024
     *
     * @param date Date to format
     * @return Formatted date string
     */
    @NonNull
    public String formatDateLong(@NonNull LocalDate date) {
        String pattern = getDatePattern(DateFormatStyle.LONG);
        DateTimeFormatter formatter = getCachedFormatter("date_long", pattern);
        return date.format(formatter);
    }

    /**
     * Format date with day of week.
     * <p>
     * Examples:
     * - Italian: venerdì 15 marzo 2024
     * - English: Friday, March 15, 2024
     * - German: Freitag, 15. März 2024
     *
     * @param date Date to format
     * @return Formatted date string with day of week
     */
    @NonNull
    public String formatDateWithDayOfWeek(@NonNull LocalDate date) {
        String pattern = getDateWithDayPattern();
        DateTimeFormatter formatter = getCachedFormatter("date_with_day", pattern);
        return date.format(formatter);
    }

    // ==================== TIME FORMATTING ====================

    /**
     * Format time according to cultural preferences (12/24 hour).
     * <p>
     * Examples:
     * - Italian: 14:30
     * - English (US): 2:30 PM
     * - English (UK): 14:30
     * - German: 14:30
     *
     * @param time Time to format
     * @return Formatted time string
     */
    @NonNull
    public String formatTime(@NonNull LocalTime time) {
        String pattern = getTimePattern();
        DateTimeFormatter formatter = getCachedFormatter("time", pattern);
        return time.format(formatter);
    }

    /**
     * Format time with seconds.
     *
     * @param time Time to format
     * @return Formatted time string with seconds
     */
    @NonNull
    public String formatTimeWithSeconds(@NonNull LocalTime time) {
        String pattern = getTimeWithSecondsPattern();
        DateTimeFormatter formatter = getCachedFormatter("time_seconds", pattern);
        return time.format(formatter);
    }

    // ==================== DATETIME FORMATTING ====================

    /**
     * Format date and time together.
     * <p>
     * Examples:
     * - Italian: 15/03/2024 14:30
     * - English (US): 03/15/2024 2:30 PM
     * - German: 15.03.2024 14:30
     *
     * @param dateTime DateTime to format
     * @return Formatted datetime string
     */
    @NonNull
    public String formatDateTime(@NonNull LocalDateTime dateTime) {
        String datePattern = getDatePattern(DateFormatStyle.SHORT);
        String timePattern = getTimePattern();
        String pattern = datePattern + " " + timePattern;

        DateTimeFormatter formatter = getCachedFormatter("datetime", pattern);
        return dateTime.format(formatter);
    }

    /**
     * Format datetime in medium format.
     *
     * @param dateTime DateTime to format
     * @return Formatted datetime string
     */
    @NonNull
    public String formatDateTimeMedium(@NonNull LocalDateTime dateTime) {
        String datePattern = getDatePattern(DateFormatStyle.MEDIUM);
        String timePattern = getTimePattern();
        String pattern = datePattern + " " + timePattern;

        DateTimeFormatter formatter = getCachedFormatter("datetime_medium", pattern);
        return dateTime.format(formatter);
    }

    // ==================== RELATIVE TIME FORMATTING ====================

    /**
     * Format date relative to today (today, tomorrow, yesterday, or date).
     *
     * @param date Date to format
     * @return Relative date string
     */
    @NonNull
    public String formatRelativeDate(@NonNull LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysDiff = ChronoUnit.DAYS.between(today, date);

        if (daysDiff == 0) {
            return mContext.getString(R.string.today);
        } else if (daysDiff == 1) {
            return mContext.getString(R.string.tomorrow);
        } else if (daysDiff == -1) {
            return mContext.getString(R.string.yesterday);
        } else {
            return formatDateShort(date);
        }
    }

    /**
     * Format time until/since a datetime.
     * <p>
     * Examples:
     * - "in 2 hours"
     * - "3 days ago"
     * - "in 1 day 5 hours"
     *
     * @param dateTime Target datetime
     * @return Relative time string
     */
    @NonNull
    public String formatTimeUntil(@NonNull LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long totalMinutes = ChronoUnit.MINUTES.between(now, dateTime);
        boolean future = totalMinutes > 0;
        totalMinutes = Math.abs(totalMinutes);

        String timeString;
        if (totalMinutes < 60) {
            // Less than 1 hour - show minutes
            timeString = mContext.getString(R.string.time_until_minutes, (int) totalMinutes);
        } else if (totalMinutes < 1440) {
            // Less than 1 day - show hours
            int hours = (int) (totalMinutes / 60);
            timeString = mContext.getString(R.string.time_until_hours, hours);
        } else {
            // 1 day or more - show days and hours
            int days = (int) (totalMinutes / 1440);
            int remainingHours = (int) ((totalMinutes % 1440) / 60);

            if (remainingHours == 0) {
                timeString = mContext.getString(R.string.time_until_days, days);
            } else {
                timeString = mContext.getString(R.string.time_until_days_hours, days, remainingHours);
            }
        }

        // Add prefix for future/past
        if (future) {
            return mContext.getString(R.string.time_until_prefix) + " " + timeString;
        } else {
            return timeString + " ago"; // Note: This should be localized in real implementation
        }
    }

    // ==================== WORK SCHEDULE FORMATTING ====================

    /**
     * Format a work shift time range.
     * <p>
     * Examples:
     * - Italian: 08:00 – 16:00
     * - English (US): 8:00 AM – 4:00 PM
     * - German: 08:00 – 16:00
     *
     * @param startTime Shift start time
     * @param endTime Shift end time
     * @return Formatted time range string
     */
    @NonNull
    public String formatShiftTimeRange(@NonNull LocalTime startTime, @NonNull LocalTime endTime) {
        String start = formatTime(startTime);
        String end = formatTime(endTime);

        // Use en-dash (–) for time ranges, which is culturally appropriate
        return start + " – " + end;
    }

    /**
     * Format day of week for work schedule display.
     *
     * @param dayOfWeek Day of week
     * @param style Text style (FULL, SHORT, NARROW)
     * @return Localized day name
     */
    @NonNull
    public String formatDayOfWeek(@NonNull DayOfWeek dayOfWeek, @NonNull TextStyle style) {
        return dayOfWeek.getDisplayName(style, mCurrentLocale);
    }

    /**
     * Format work day status.
     *
     * @param date Date to check
     * @param isWorkDay true if it's a work day
     * @return Formatted work day status
     */
    @NonNull
    public String formatWorkDayStatus(@NonNull LocalDate date, boolean isWorkDay) {
        String dateStr = formatRelativeDate(date);
        String status = mContext.getString(isWorkDay ? R.string.work_day : R.string.rest_day);

        return dateStr + " - " + status;
    }

    // ==================== DURATION FORMATTING ====================

    /**
     * Format duration in hours and minutes.
     * <p>
     * Examples:
     * - "2 hours 30 minutes"
     * - "45 minutes"
     * - "1 hour"
     *
     * @param totalMinutes Total duration in minutes
     * @return Formatted duration string
     */
    @NonNull
    public String formatDuration(long totalMinutes) {
        if (totalMinutes < 60) {
            return mContext.getString(R.string.duration_minutes, (int) totalMinutes);
        } else {
            int hours = (int) (totalMinutes / 60);
            int minutes = (int) (totalMinutes % 60);

            if (minutes == 0) {
                return hours == 1 ?
                        mContext.getString(R.string.duration_hour) :
                        mContext.getString(R.string.duration_hours, hours);
            } else {
                return mContext.getString(R.string.duration_hours_minutes, hours, minutes);
            }
        }
    }

    // ==================== PATTERN GENERATORS ====================

    /**
     * Get date pattern for the specified style.
     */
    private String getDatePattern(DateFormatStyle style) {
        switch (mCurrentLanguage) {
            case LocaleManager.LANGUAGE_ENGLISH:
                if ("US".equals(mCurrentLocale.getCountry())) {
                    // US English
                    switch (style) {
                        case SHORT: return "MM/dd/yyyy";
                        case MEDIUM: return "MMM d, yyyy";
                        case LONG: return "MMMM d, yyyy";
                        default: return "MM/dd/yyyy";
                    }
                } else {
                    // UK English and others
                    switch (style) {
                        case SHORT: return "dd/MM/yyyy";
                        case MEDIUM: return "d MMM yyyy";
                        case LONG: return "d MMMM yyyy";
                        default: return "dd/MM/yyyy";
                    }
                }
            case LocaleManager.LANGUAGE_GERMAN:
                switch (style) {
                    case SHORT: return "dd.MM.yyyy";
                    case MEDIUM: return "d. MMM. yyyy";
                    case LONG: return "d. MMMM yyyy";
                    default: return "dd.MM.yyyy";
                }
            case LocaleManager.LANGUAGE_FRENCH:
            case LocaleManager.LANGUAGE_ITALIAN:
            default:
                switch (style) {
                    case SHORT: return "dd/MM/yyyy";
                    case MEDIUM: return "d MMM yyyy";
                    case LONG: return "d MMMM yyyy";
                    default: return "dd/MM/yyyy";
                }
        }
    }

    /**
     * Get date pattern with day of week.
     */
    private String getDateWithDayPattern() {
        switch (mCurrentLanguage) {
            case LocaleManager.LANGUAGE_ENGLISH:
                return "EEEE, MMMM d, yyyy";
            case LocaleManager.LANGUAGE_GERMAN:
                return "EEEE, d. MMMM yyyy";
            case LocaleManager.LANGUAGE_FRENCH:
            case LocaleManager.LANGUAGE_ITALIAN:
            default:
                return "EEEE d MMMM yyyy";
        }
    }

    /**
     * Get time pattern based on cultural preferences.
     */
    private String getTimePattern() {
        if (mLocaleManager.is24HourFormat()) {
            return "HH:mm";
        } else {
            return "h:mm a";
        }
    }

    /**
     * Get time pattern with seconds.
     */
    private String getTimeWithSecondsPattern() {
        if (mLocaleManager.is24HourFormat()) {
            return "HH:mm:ss";
        } else {
            return "h:mm:ss a";
        }
    }

    // ==================== FORMATTER CACHING ====================

    /**
     * Get cached formatter or create new one.
     */
    private DateTimeFormatter getCachedFormatter(String key, String pattern) {
        String cacheKey = key + "_" + mCurrentLocale.toString();
        DateTimeFormatter formatter = mFormatterCache.get(cacheKey);

        if (formatter == null) {
            formatter = DateTimeFormatter.ofPattern(pattern, mCurrentLocale);
            mFormatterCache.put(cacheKey, formatter);
        }

        return formatter;
    }

    // ==================== ENUMS AND HELPERS ====================

    /**
     * Date format styles.
     */
    public enum DateFormatStyle {
        SHORT,   // 15/03/2024
        MEDIUM,  // 15 mar 2024
        LONG,    // 15 marzo 2024
        FULL     // venerdì 15 marzo 2024
    }

    // ==================== SYSTEM INTEGRATION ====================

    /**
     * Check if system is using 24-hour format (for Android system integration).
     *
     * @return true if system uses 24-hour format
     */
    public boolean isSystem24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }

    /**
     * Get system date format string from Android.
     *
     * @return System date format string
     */
    @Nullable
    public String getSystemDateFormat() {
        return DateFormat.getDateFormat(mContext).toString();
    }
}