package net.calvuz.qdue.core.domain.events.data.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.calvuz.qdue.core.domain.events.models.EventType;
import net.calvuz.qdue.core.domain.events.models.EventPriority;
import net.calvuz.qdue.core.domain.events.models.TurnException;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Room TypeConverters for event-specific data types.
 * Handles conversion between Java objects and SQLite-compatible types.
 */
public class EventsTypeConverters {

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

    // ==================== EventType Converters ====================

    /**
     * Convert EventType enum to String for database storage.
     * @param eventType EventType enum value
     * @return String representation or null
     */
    @TypeConverter
    public static String fromEventType(EventType eventType) {
        return eventType != null ? eventType.name() : null;
    }

    /**
     * Convert String back to EventType enum from database.
     * @param eventTypeString String representation
     * @return EventType enum or default value
     */
    @TypeConverter
    public static EventType toEventType(String eventTypeString) {
        if (eventTypeString == null) {
            return EventType.GENERAL; // Default fallback
        }

        try {
            return EventType.valueOf(eventTypeString);
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            return EventType.GENERAL;
        }
    }

    // ==================== EventPriority Converters ====================

    /**
     * Convert EventPriority enum to String for database storage.
     * @param priority EventPriority enum value
     * @return String representation or null
     */
    @TypeConverter
    public static String fromEventPriority(EventPriority priority) {
        return priority != null ? priority.name() : null;
    }

    /**
     * Convert String back to EventPriority enum from database.
     * @param priorityString String representation
     * @return EventPriority enum or default value
     */
    @TypeConverter
    public static EventPriority toEventPriority(String priorityString) {
        if (priorityString == null) {
            return EventPriority.NORMAL; // Default fallback
        }

        try {
            return EventPriority.valueOf(priorityString);
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            return EventPriority.NORMAL;
        }
    }

    // ==================== Map<String, String> Converters ====================

    /**
     * Convert Map<String, String> to JSON string for database storage.
     * Used for custom properties in events.
     * @param stringMap Map to convert
     * @return JSON string representation or null
     */
    @TypeConverter
    public static String fromStringMap(Map<String, String> stringMap) {
        if (stringMap == null || stringMap.isEmpty()) {
            return null;
        }
        return gson.toJson(stringMap);
    }

    /**
     * Convert JSON string back to Map<String, String> from database.
     * @param stringMapString JSON string representation
     * @return Map<String, String> or empty map
     */
    @TypeConverter
    public static Map<String, String> toStringMap(String stringMapString) {
        if (stringMapString == null || stringMapString.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }

        try {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> result = gson.fromJson(stringMapString, type);
            return result != null ? result : new java.util.HashMap<>();
        } catch (Exception e) {
            // Handle malformed JSON gracefully
            return new java.util.HashMap<>();
        }
    }

    // ==================== Boolean Converters (if needed) ====================

    /**
     * Convert Boolean to Integer for database storage.
     * SQLite doesn't have native boolean support.
     * @param value Boolean value
     * @return 1 for true, 0 for false, null for null
     */
    @TypeConverter
    public static Integer fromBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    /**
     * Convert Integer back to Boolean from database.
     * @param value Integer value from database
     * @return Boolean value or null
     */
    @TypeConverter
    public static Boolean toBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value != 0;
    }

    // ==================== EXCEPTIONS data converters ========================

    @TypeConverter
    public String fromExceptionType(TurnException.ExceptionType exceptionType) {
        return exceptionType != null ? exceptionType.name() : null;
    }

    @TypeConverter
    public TurnException.ExceptionType toExceptionType(String exceptionType) {
        try {
            return exceptionType != null ? TurnException.ExceptionType.valueOf(exceptionType) : null;
        } catch (IllegalArgumentException e) {
            return TurnException.ExceptionType.OTHER;
        }
    }

    // TurnException.ExceptionStatus converters
    @TypeConverter
    public String fromExceptionStatus(TurnException.ExceptionStatus status) {
        return status != null ? status.name() : null;
    }

    @TypeConverter
    public TurnException.ExceptionStatus toExceptionStatus(String status) {
        try {
            return status != null ? TurnException.ExceptionStatus.valueOf(status) : null;
        } catch (IllegalArgumentException e) {
            return TurnException.ExceptionStatus.ACTIVE;
        }
    }
}