package net.calvuz.qdue.core.db.converters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Type Converters for Calendar Engine Database
 *
 * <p>Provides comprehensive type conversion support for the new calendar entities,
 * including JSON serialization/deserialization, Java 8 time types, and enum conversions.
 * All converters include robust error handling and fallback mechanisms.</p>
 *
 * <h3>Supported Conversions:</h3>
 * <ul>
 *   <li><strong>Java 8 Time</strong>: LocalDate, LocalTime with ISO formatting</li>
 *   <li><strong>JSON Collections</strong>: List&lt;DayOfWeek&gt;, List&lt;Integer&gt;, Map&lt;String,String&gt;</li>
 *   <li><strong>Enums</strong>: All calendar domain enums with fallback handling</li>
 *   <li><strong>Complex Types</strong>: Nullable support with proper null handling</li>
 * </ul>
 *
 * <h3>Error Handling Strategy:</h3>
 * <ul>
 *   <li><strong>Graceful Degradation</strong>: Invalid data returns defaults rather than crashing</li>
 *   <li><strong>Logging</strong>: All conversion errors are logged for debugging</li>
 *   <li><strong>Null Safety</strong>: Proper handling of null values in all converters</li>
 *   <li><strong>Version Compatibility</strong>: Forward/backward compatible JSON serialization</li>
 * </ul>
 */
public class CalendarTypeConverters {

    private static final String TAG = "CalendarTypeConverters";
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

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

    /**
     * Convert LocalDate to ISO string for database storage.
     */
    @TypeConverter
    @Nullable
    public static String fromLocalDate(@Nullable LocalDate date) {
        return date != null ? date.toString() : null;
    }

    /**
     * Convert ISO string from database to LocalDate.
     */
    @TypeConverter
    @Nullable
    public static LocalDate toLocalDate(@Nullable String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Failed to parse LocalDate: " + dateString, e);
            return null;
        }
    }

    /**
     * Convert LocalTime to ISO string for database storage.
     */
    @TypeConverter
    @Nullable
    public static String fromLocalTime(@Nullable LocalTime time) {
        return time != null ? time.format(DateTimeFormatter.ISO_LOCAL_TIME) : null;
    }

    /**
     * Convert ISO string from database to LocalTime.
     */
    @TypeConverter
    @Nullable
    public static LocalTime toLocalTime(@Nullable String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Failed to parse LocalTime: " + timeString, e);
            return null;
        }
    }

    // ==================== EVENT TYPE CONVERTERS ====================

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

    // ==================== TEAM TYPE CONVERTERS ====================

    /**
     * Convert TeamType enum to String for database storage.
     *
     * @param teamType TeamType enum value
     * @return String representation or null
     */
    @TypeConverter
    @Nullable
    public static String fromTeamType(@Nullable net.calvuz.qdue.domain.calendar.models.Team.TeamType teamType) {
        return teamType != null ? teamType.name() : null;
    }

    /**
     * Convert String back to TeamType enum from database.
     *
     * @param teamTypeString String representation
     * @return TeamType enum or default value (STANDARD)
     */
    @TypeConverter
    @NonNull
    public static net.calvuz.qdue.domain.calendar.models.Team.TeamType toTeamType(@Nullable String teamTypeString) {
        if (teamTypeString == null || teamTypeString.trim().isEmpty()) {
            return net.calvuz.qdue.domain.calendar.models.Team.TeamType.STANDARD; // Default fallback
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.Team.TeamType.valueOf( teamTypeString.trim().toUpperCase() );
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            Log.w( TAG, "Unknown TeamType value: " + teamTypeString + ", using STANDARD as fallback" );
            return net.calvuz.qdue.domain.calendar.models.Team.TeamType.STANDARD;
        }
    }

    // ==================== STATUS ENUM CONVERTERS ====================

    /**
     * Convert Status enum to String for database storage.
     * @param status Status enum value
     * @return String representation or null
     */
    @TypeConverter
    @Nullable
    public static String fromStatus(@Nullable net.calvuz.qdue.domain.common.enums.Status status) {
        return status != null ? status.name() : null;
    }

    /**
     * Convert String back to Status enum from database.
     * @param statusString String representation
     * @return Status enum or default value (ACTIVE)
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.common.enums.Status toStatus(@Nullable String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            return net.calvuz.qdue.domain.common.enums.Status.ACTIVE; // Default fallback
        }

        try {
            return net.calvuz.qdue.domain.common.enums.Status.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle case where enum value doesn't exist (backward compatibility)
            Log.w(TAG, "Unknown Status value: " + statusString + ", using ACTIVE as fallback");
            return net.calvuz.qdue.domain.common.enums.Status.ACTIVE;
        }
    }

    // ==================== PRIORITY ENUM CONVERTERS ====================

    /**
     * Convert UserScheduleAssignment.Priority enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromPriority(@Nullable net.calvuz.qdue.domain.common.enums.Priority priority) {
        return priority != null ? priority.name() : null;
    }

    /**
     * Convert string to UserScheduleAssignment.Priority enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.common.enums.Priority toPriority(@Nullable String priorityString) {
        if (priorityString == null || priorityString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.common.enums.Priority.valueOf( priorityString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid UserScheduleAssignment.Priority: " + priorityString, e );
            return net.calvuz.qdue.domain.common.enums.Priority.NORMAL; // Fallback
        }
    }

    // ==================== DAYOFWEEK LIST CONVERTERS ====================

    /**
     * Convert List&lt;DayOfWeek&gt; to JSON string for database storage.
     */
    @TypeConverter
    @Nullable
    public static String fromDayOfWeekList(@Nullable List<DayOfWeek> dayOfWeekList) {
        if (dayOfWeekList == null || dayOfWeekList.isEmpty()) {
            return null;
        }

        try {
            List<String> dayNames = new ArrayList<>();
            for (DayOfWeek day : dayOfWeekList) {
                dayNames.add( day.name() );
            }
            return GSON.toJson( dayNames );
        } catch (Exception e) {
            Log.e( TAG, "Failed to serialize DayOfWeek list", e );
            return null;
        }
    }

    /**
     * Convert JSON string from database to List&lt;DayOfWeek&gt;.
     */
    @TypeConverter
    @NonNull
    public static List<DayOfWeek> fromDayOfWeekListJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            List<String> dayNames = GSON.fromJson( json, listType );

            List<DayOfWeek> dayOfWeekList = new ArrayList<>();
            for (String dayName : dayNames) {
                try {
                    dayOfWeekList.add( DayOfWeek.valueOf( dayName ) );
                } catch (IllegalArgumentException e) {
                    Log.w( TAG, "Invalid DayOfWeek name: " + dayName );
                    // Skip invalid entries rather than failing completely
                }
            }
            return dayOfWeekList;
        } catch (JsonSyntaxException e) {
            Log.e( TAG, "Failed to deserialize DayOfWeek list from JSON: " + json, e );
            return new ArrayList<>();
        }
    }

    // ==================== INTEGER LIST CONVERTERS ====================

    /**
     * Convert List&lt;Integer&gt; to JSON string for database storage.
     */
    @TypeConverter
    @Nullable
    public static String fromIntegerList(@Nullable List<Integer> integerList) {
        if (integerList == null || integerList.isEmpty()) {
            return null;
        }

        try {
            return GSON.toJson( integerList );
        } catch (Exception e) {
            Log.e( TAG, "Failed to serialize Integer list", e );
            return null;
        }
    }

    /**
     * Convert JSON string from database to List&lt;Integer&gt;.
     */
    @TypeConverter
    @NonNull
    public static List<Integer> fromIntegerListJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<Integer>>() {
            }.getType();
            List<Integer> result = GSON.fromJson( json, listType );
            return result != null ? result : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            Log.e( TAG, "Failed to deserialize Integer list from JSON: " + json, e );
            return new ArrayList<>();
        }
    }

    // ==================== STRING MAP CONVERTERS ====================

    /**
     * Convert Map&lt;String, String&gt; to JSON string for database storage.
     */
    @TypeConverter
    @Nullable
    public static String fromStringMap(@Nullable Map<String, String> stringMap) {
        if (stringMap == null || stringMap.isEmpty()) {
            return null;
        }

        try {
            return GSON.toJson(stringMap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize String map", e);
            return null;
        }
    }

    /**
     * Convert JSON string from database to Map&lt;String, String&gt;.
     */
    @TypeConverter
    @NonNull
    public static Map<String, String> toStringMap(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> result = GSON.fromJson(json, mapType);
            return result != null ? result : new HashMap<>();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Failed to deserialize String map from JSON: " + json, e);
            return new HashMap<>();
        }
    }

    // ==================== RECURRENCE CONVERTERS ====================

    /**
     * Convert RecurrenceRule.Frequency enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromRecurrenceFrequency(@Nullable net.calvuz.qdue.domain.calendar.models.RecurrenceRule.Frequency frequency) {
        return frequency != null ? frequency.name() : null;
    }

    /**
     * Convert string to RecurrenceRule.Frequency enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.calendar.models.RecurrenceRule.Frequency toRecurrenceFrequency(@Nullable String frequencyString) {
        if (frequencyString == null || frequencyString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.RecurrenceRule.Frequency.valueOf( frequencyString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid RecurrenceRule.Frequency: " + frequencyString, e );
            return net.calvuz.qdue.domain.calendar.models.RecurrenceRule.Frequency.DAILY; // Fallback
        }
    }

    /**
     * Convert RecurrenceRule.EndType enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromRecurrenceEndType(@Nullable net.calvuz.qdue.domain.calendar.models.RecurrenceRule.EndType endType) {
        return endType != null ? endType.name() : null;
    }

    /**
     * Convert string to RecurrenceRule.EndType enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.calendar.models.RecurrenceRule.EndType toRecurrenceEndType(@Nullable String endTypeString) {
        if (endTypeString == null || endTypeString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.RecurrenceRule.EndType.valueOf( endTypeString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid RecurrenceRule.EndType: " + endTypeString, e );
            return net.calvuz.qdue.domain.calendar.models.RecurrenceRule.EndType.NEVER; // Fallback
        }
    }

    // ==================== SHIFT EXCEPTIONS CONVERTERS ====================

    /**
     * Convert ShiftException.ExceptionType enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromShiftExceptionType(@Nullable net.calvuz.qdue.domain.calendar.models.ShiftException.ExceptionType exceptionType) {
        return exceptionType != null ? exceptionType.name() : null;
    }

    /**
     * Convert string to ShiftException.ExceptionType enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.calendar.models.ShiftException.ExceptionType toShiftExceptionType(@Nullable String exceptionTypeString) {
        if (exceptionTypeString == null || exceptionTypeString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.ShiftException.ExceptionType.valueOf( exceptionTypeString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid ShiftException.ExceptionType: " + exceptionTypeString, e );
            return net.calvuz.qdue.domain.calendar.models.ShiftException.ExceptionType.CUSTOM; // Fallback
        }
    }

    /**
     * Convert ShiftException.ApprovalStatus enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromShiftExceptionApprovalStatus(@Nullable net.calvuz.qdue.domain.calendar.models.ShiftException.ApprovalStatus approvalStatus) {
        return approvalStatus != null ? approvalStatus.name() : null;
    }

    /**
     * Convert string to ShiftException.ApprovalStatus enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.calendar.models.ShiftException.ApprovalStatus toShiftExceptionApprovalStatus(@Nullable String approvalStatusString) {
        if (approvalStatusString == null || approvalStatusString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.ShiftException.ApprovalStatus.valueOf( approvalStatusString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid ShiftException.ApprovalStatus: " + approvalStatusString, e );
            return net.calvuz.qdue.domain.calendar.models.ShiftException.ApprovalStatus.DRAFT; // Fallback
        }
    }

    /**
     * Convert ShiftException.Priority enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromShiftExceptionPriority(@Nullable net.calvuz.qdue.domain.calendar.models.ShiftException.Priority priority) {
        return priority != null ? priority.name() : null;
    }

    /**
     * Convert string to ShiftException.Priority enum.
     */
    @TypeConverter
    @Nullable
    public static net.calvuz.qdue.domain.calendar.models.ShiftException.Priority toShiftExceptionPriority(@Nullable String priorityString) {
        if (priorityString == null || priorityString.trim().isEmpty()) {
            return null;
        }

        try {
            return net.calvuz.qdue.domain.calendar.models.ShiftException.Priority.valueOf( priorityString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid ShiftException.Priority: " + priorityString, e );
            return net.calvuz.qdue.domain.calendar.models.ShiftException.Priority.NORMAL; // Fallback
        }
    }


    /**
     * Convert DayOfWeek enum to string.
     */
    @TypeConverter
    @Nullable
    public static String fromDayOfWeek(@Nullable DayOfWeek dayOfWeek) {
        return dayOfWeek != null ? dayOfWeek.name() : null;
    }

    /**
     * Convert string to DayOfWeek enum.
     */
    @TypeConverter
    @Nullable
    public static DayOfWeek toDayOfWeek(@Nullable String dayOfWeekString) {
        if (dayOfWeekString == null || dayOfWeekString.trim().isEmpty()) {
            return null;
        }

        try {
            return DayOfWeek.valueOf( dayOfWeekString );
        } catch (IllegalArgumentException e) {
            Log.e( TAG, "Invalid DayOfWeek: " + dayOfWeekString, e );
            return DayOfWeek.MONDAY; // Fallback to Monday
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Test if a JSON string is valid.
     * Utility method for debugging and validation.
     */
    public static boolean isValidJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            GSON.fromJson( json, Object.class );
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Get pretty-printed JSON for debugging.
     * Utility method for development and debugging.
     */
    @NonNull
    public static String toPrettyJson(@Nullable Object object) {
        if (object == null) {
            return "null";
        }

        try {
            return new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson( object );
        } catch (Exception e) {
            Log.e( TAG, "Failed to create pretty JSON", e );
            return object.toString();
        }
    }

    /**
     * Validate and fix JSON before database storage.
     * Ensures JSON is valid and properly formatted.
     */
    @Nullable
    public static String validateAndFixJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            // Parse and re-serialize to ensure valid JSON
            Object parsed = GSON.fromJson( json, Object.class );
            return GSON.toJson( parsed );
        } catch (JsonSyntaxException e) {
            Log.e( TAG, "Invalid JSON detected and removed: " + json, e );
            return null; // Return null for invalid JSON
        }
    }

    /**
     * Get converter version for future migration support.
     */
    @NonNull
    public static String getConverterVersion() {
        return "1.0.0";
    }

    /**
     * Check if all required converters are available.
     * Utility method for testing and validation.
     */
    public static boolean areAllConvertersAvailable() {
        try {
            // Test basic conversions
            LocalDate testDate = LocalDate.now();
            String dateString = fromLocalDate( testDate );
            LocalDate parsedDate = toLocalDate( dateString );

            LocalTime testTime = LocalTime.now();
            String timeString = fromLocalTime( testTime );
            LocalTime parsedTime = toLocalTime( timeString );

            // Test JSON conversions
            List<DayOfWeek> testDays = new ArrayList<>();
            testDays.add( DayOfWeek.MONDAY );
            String daysJson = fromDayOfWeekList( testDays );
            List<DayOfWeek> parsedDays = fromDayOfWeekListJson( daysJson );

            // Basic validation
            return testDate.equals( parsedDate ) &&
                    testTime.equals( parsedTime ) &&
                    testDays.equals( parsedDays );
        } catch (Exception e) {
            Log.e( TAG, "Converter availability check failed", e );
            return false;
        }
    }
}