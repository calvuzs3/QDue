package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a shift type with schedule, duration and associated color.
 *
 * Defines the template for shifts including start time, duration, and visual appearance.
 * Immutable value object that can be reused across multiple shifts.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public class ShiftType {

    public static final String TAG = ShiftType.class.getSimpleName();

    // Core properties
    private final String name;             // Shift name (e.g., "Morning")
    private final String description;      // Description (e.g., "Morning shift 6-14")
    private final LocalTime startTime;     // Start time
    private final Duration duration;       // Shift duration
    private final @ColorInt int color;     // Associated color for UI

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Complete constructor with all parameters.
     *
     * @param name Name of the shift
     * @param description Description of the shift
     * @param startHour Start hour (0-23)
     * @param startMinute Start minute (0-59)
     * @param durationHours Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @param color Associated color (ARGB format)
     */
    public ShiftType(String name, String description, int startHour, int startMinute,
                     int durationHours, int durationMinutes, @ColorInt int color) {
        this.name = name;
        this.description = description;
        this.startTime = LocalTime.of(startHour, startMinute);
        this.duration = Duration.ofHours(durationHours).plusMinutes(durationMinutes);
        this.color = color;
    }

    /**
     * @return Shift name
     */
    public String getName() {
        return name;
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
        return duration.toMinutesPart();
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

    @Override
    public int hashCode() {
        return Objects.hash(name, description, startTime, duration, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ShiftType other = (ShiftType) obj;
        return Objects.equals(name, other.name) &&
                Objects.equals(description, other.description) &&
                Objects.equals(startTime, other.startTime) &&
                Objects.equals(duration, other.duration) &&
                color == other.color;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + name + " " + getFormattedStartTime() + "-" + getFormattedEndTime() + "}";
    }
}