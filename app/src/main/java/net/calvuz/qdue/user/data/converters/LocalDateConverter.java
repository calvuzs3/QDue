package net.calvuz.qdue.user.data.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Room type converter for LocalDate.
 */
public class LocalDateConverter {

    private static final Gson gson = new Gson();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ==================== LocalDateTime Converters ====================

    /**
     * Convert LocalDateTime to String for database storage.
     * @param dateTime LocalDateTime to convert
     * @return ISO formatted string or null
     */
    @TypeConverter
    public static String fromLocalDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    /**
     * Convert String back to LocalDateTime from database.
     * @param dateTimeString ISO formatted string
     * @return LocalDateTime object or null
     */
    @TypeConverter
    public static LocalDateTime toLocalDateTime(String dateTimeString) {
        return dateTimeString != null ? LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER) : null;
    }


    // ==================== LocalDate Converters ====================

    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    @TypeConverter
    public static LocalDate toLocalDate(String dateString) {
        return dateString != null ? LocalDate.parse(dateString) : null;
    }


    @TypeConverter
    public static LocalDate fromString(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

}
