package net.calvuz.qdue.core.db.converters;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.events.enums.EventPriority;
import net.calvuz.qdue.domain.calendar.events.models.TurnException;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Unified Room TypeConverters for Q-DUE Database.
 * Handles conversion between Java objects and SQLite-compatible types for all entities.
 * <p>
 * Combines converters from:
 * - EventsTypeConverters (events)
 * - LocalDateConverter (users)
 * - New TurnException converters
 */
public class QDueTypeConverters {

    private static final String TAG = "QDueTypeConverters";

    private static final Gson gson = new Gson();


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

    // ==================== TurnException.ExceptionType Converters ====================

    /**
     * Convert TurnException.ExceptionType enum to String for database storage.
     * @param exceptionType ExceptionType enum value
     * @return String representation or null
     */
    @TypeConverter
    public static String fromExceptionType(TurnException.ExceptionType exceptionType) {
        return exceptionType != null ? exceptionType.name() : null;
    }

    /**
     * Convert String back to TurnException.ExceptionType enum from database.
     * @param exceptionTypeString String representation
     * @return ExceptionType enum or default value
     */
    @TypeConverter
    public static TurnException.ExceptionType toExceptionType(String exceptionTypeString) {
        if (exceptionTypeString == null) {
            return TurnException.ExceptionType.OTHER; // Default fallback
        }

        try {
            return TurnException.ExceptionType.valueOf(exceptionTypeString);
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            return TurnException.ExceptionType.OTHER;
        }
    }

    // ==================== TurnException.ExceptionStatus Converters ====================

    /**
     * Convert TurnException.ExceptionStatus enum to String for database storage.
     * @param status ExceptionStatus enum value
     * @return String representation or null
     */
    @TypeConverter
    public static String fromExceptionStatus(TurnException.ExceptionStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * Convert String back to TurnException.ExceptionStatus enum from database.
     * @param statusString String representation
     * @return ExceptionStatus enum or default value
     */
    @TypeConverter
    public static TurnException.ExceptionStatus toExceptionStatus(String statusString) {
        if (statusString == null) {
            return TurnException.ExceptionStatus.ACTIVE; // Default fallback
        }

        try {
            return TurnException.ExceptionStatus.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            return TurnException.ExceptionStatus.ACTIVE;
        }
    }

    // ==================== Map<String, String> Converters ====================


    // ==================== Boolean Converters ====================

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
}