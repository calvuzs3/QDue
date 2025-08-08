package net.calvuz.qdue.quattrodue.models;

import static android.os.Build.VERSION.SDK_INT;

import android.graphics.Color;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a shift type with schedule, duration and associated color.
 * <p>
 * Defines the template for shifts including start time, duration, and visual appearance.
 * Immutable value object that can be reused across multiple shifts.
 * <p>
 * Enhanced with UUID support for unique identification while maintaining
 * full backward compatibility with existing code.
 *
 * @author Luke (original)
 * @author Updated 08/08/2025 - UUID Support Added
 */
public class ShiftType {

    public static final String TAG = ShiftType.class.getSimpleName();

    // Core properties
    private final String id;               // Unique identifier (UUID)
    private final String name;             // Shift name (e.g., "Morning")
    private final String description;      // Description (e.g., "Morning shift 6-14")
    private final LocalTime startTime;     // Start time
    private final Duration duration;       // Shift duration
    private final @ColorInt int color;     // Associated color for UI
    private boolean restType;              // Is a rest day

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Complete constructor with ID parameter - Primary constructor.
     *
     * @param id              Unique identifier (UUID string)
     * @param name            Name of the shift
     * @param description     Description of the shift
     * @param startHour       Start hour (0-23)
     * @param startMinute     Start minute (0-59)
     * @param durationHours   Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @param color           Associated color (ARGB format)
     */
    public ShiftType(@Nullable String id, String name, String description, int startHour, int startMinute,
                     int durationHours, int durationMinutes, @ColorInt int color) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : generateUUID();
        this.name = name;
        this.description = description;
        this.startTime = LocalTime.of(startHour, startMinute);
        this.duration = Duration.ofHours(durationHours).plusMinutes(durationMinutes);
        this.color = color;
        this.restType = false;
    }

    /**
     * Backward-compatible constructor without ID - Automatically generates UUID.
     * <p>
     * This constructor maintains full compatibility with existing code
     * while providing automatic UUID generation for new instances.
     *
     * @param name            Name of the shift
     * @param description     Description of the shift
     * @param startHour       Start hour (0-23)
     * @param startMinute     Start minute (0-59)
     * @param durationHours   Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @param color           Associated color (ARGB format)
     */
    public ShiftType(String name, String description, int startHour, int startMinute,
                     int durationHours, int durationMinutes, @ColorInt int color) {
        this(null, name, description, startHour, startMinute, durationHours, durationMinutes, color);
    }

    /**
     * Copy constructor with new ID generation.
     * <p>
     * Creates a new ShiftType based on an existing one but with a new UUID.
     * Useful for cloning operations.
     *
     * @param original The original ShiftType to copy
     */
    public ShiftType(@NonNull ShiftType original) {
        this(null, original.name, original.description,
                original.getStartHour(), original.getStartMinute(),
                original.getDurationHours(), original.getDurationMinutes(),
                original.color);
    }

    /**
     * Copy constructor with specific ID.
     * <p>
     * Creates a new ShiftType based on an existing one but with a specified ID.
     * Useful for deserialization or data migration operations.
     *
     * @param original The original ShiftType to copy
     * @param newId    The new ID to assign
     */
    public ShiftType(@NonNull ShiftType original, @Nullable String newId) {
        this(newId, original.name, original.description,
                original.getStartHour(), original.getStartMinute(),
                original.getDurationHours(), original.getDurationMinutes(),
                original.color);
    }

    /**
     * Generates a new UUID string.
     * <p>
     * Creates a universally unique identifier for shift type instances.
     * Uses UUID.randomUUID() for maximum uniqueness across distributed systems.
     *
     * @return A new UUID string
     */
    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return Unique identifier of this shift type
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * @return Shift name
     */
    public String getName() {
        return name;
    }

    /**
     * @return First char of Shift name
     */
    public String getShortName() {
        return name.substring(0, 1).toUpperCase();
    }

    /**
     * @return Shift description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Start hour (0-23)
     */
    public int getStartHour() {
        return startTime.getHour();
    }

    /**
     * @return Start minute (0-59)
     */
    public int getStartMinute() {
        return startTime.getMinute();
    }

    /**
     * @return Start time as LocalTime object
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * @return Shift duration in hours
     */
    public int getDurationHours() {
        return (int) duration.toHours();
    }

    /**
     * @return Additional minutes of duration (0-59)
     */
    public int getDurationMinutes() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            return duration.toMinutesPart();
        }
        return 0;
    }

    /**
     * @return Total duration as Duration object
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * @return Associated color in ARGB format
     */
    public @ColorInt int getColor() {
        return color;
    }

    /**
     * Calculates the end time of the shift.
     *
     * @return End time
     */
    public LocalTime getEndTime() {
        return startTime.plus(duration);
    }

    /**
     * Checks if a given time is within the shift.
     *
     * @param time Time to verify
     * @return true if time is within the shift
     */
    public boolean includes(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(getEndTime());
    }

    /**
     * @return Formatted start time (e.g., "06:00")
     */
    public String getFormattedStartTime() {
        return startTime.format(TIME_FORMATTER);
    }

    /**
     * @return Formatted end time (e.g., "14:00")
     */
    public String getFormattedEndTime() {
        return getEndTime().format(TIME_FORMATTER);
    }

    /**
     * @return Formatted time range (e.g., "06:00 - 14:00")
     */
    public String getTimeRange() {
        return getFormattedStartTime() + " - " + getFormattedEndTime();
    }

    /**
     * Calculate optimal text color for readability against background.
     *
     * @return Text color (black or white) for optimal contrast
     */
    public int getTextColor() {
        return calculateTextColorForBackground(this.color);
    }

    /**
     * Calculate optimal text color for readability.
     * <p>
     * Uses luminance calculation to determine whether black or white text
     * provides better contrast against the background color.
     *
     * @param backgroundColor The background color to calculate against
     * @return Color.BLACK or Color.WHITE for optimal readability
     */
    private static int calculateTextColorForBackground(int backgroundColor) {
        // Calculate luminance of background color
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        // Calculate relative luminance (sRGB)
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        // Return black for light backgrounds, white for dark backgrounds
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Creates a new ShiftType with updated properties while preserving ID.
     * <p>
     * Builder-like method for creating modified versions while maintaining identity.
     *
     * @param newName        New name (null to keep current)
     * @param newDescription New description (null to keep current)
     * @param newColor       New color (null to keep current)
     * @return New ShiftType instance with preserved ID
     */
    public ShiftType withUpdatedProperties(@Nullable String newName,
                                           @Nullable String newDescription,
                                           @Nullable Integer newColor) {
        return new ShiftType(
                this.id,  // Preserve ID
                newName != null ? newName : this.name,
                newDescription != null ? newDescription : this.description,
                this.getStartHour(),
                this.getStartMinute(),
                this.getDurationHours(),
                this.getDurationMinutes(),
                newColor != null ? newColor : this.color
        );
    }

    /**
     * Checks if this ShiftType has the same schedule as another.
     * <p>
     * Compares start time and duration only, ignoring ID, name, description, and color.
     *
     * @param other Other ShiftType to compare
     * @return true if schedules match
     */
    public boolean hasSameSchedule(@NonNull ShiftType other) {
        return Objects.equals(this.startTime, other.startTime) &&
                Objects.equals(this.duration, other.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, startTime, duration, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ShiftType other = (ShiftType) obj;
        return Objects.equals(id, other.id) &&
                Objects.equals(name, other.name) &&
                Objects.equals(description, other.description) &&
                Objects.equals(startTime, other.startTime) &&
                Objects.equals(duration, other.duration) &&
                color == other.color;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{id=" + id + ", name=" + name + " " +
                getFormattedStartTime() + "-" + getFormattedEndTime() + "}";
    }

    public boolean isRestType() {
        return restType;
    }

    public void setRestType(boolean restType) {
        this.restType = restType;
    }
}