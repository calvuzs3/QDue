package net.calvuz.qdue.core.domain.quattrodue.models;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.util.Objects;

/**
 * <p>Domain model representing a shift type definition for work schedule templates.
 * Updated to follow the same patterns as other domain models in the application.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Supports shifts that cross midnight (e.g., night shifts 21:00-05:00)</li>
 *   <li>Break time support for extended shifts</li>
 *   <li>Visual customization via hex color codes</li>
 *   <li>Distinction between predefined and user-defined types</li>
 *   <li>Soft delete capability via active/inactive status</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 2.0
 * @since Database Version 5
 */
public class ShiftType {

    private Long id;
    private String name;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private String colorHex;
    private boolean isRestPeriod;
    private boolean hasBreakTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
    private boolean isUserDefined;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;

    // Default constructor
    public ShiftType() {
        this.isActive = true;
        this.isUserDefined = false;
        this.isRestPeriod = false;
        this.hasBreakTime = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Constructor for work shifts
    public ShiftType(String name, String description, LocalTime startTime,
                     LocalTime endTime, String colorHex) {
        this();
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorHex = colorHex;
    }

    // Constructor for rest periods
    public ShiftType(String name, String description, String colorHex, boolean isRestPeriod) {
        this();
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
        this.isRestPeriod = isRestPeriod;
    }

    // Constructor with break time
    public ShiftType(String name, String description, LocalTime startTime, LocalTime endTime,
                     String colorHex, LocalTime breakStartTime, LocalTime breakEndTime) {
        this(name, description, startTime, endTime, colorHex);
        this.hasBreakTime = true;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
    }

    /**
     * Checks if this shift crosses midnight (ends on the next day).
     *
     * @return true if end time is before start time, indicating midnight crossing
     */
    public boolean crossesMidnight() {
        if (isRestPeriod || startTime == null || endTime == null) {
            return false;
        }
        return endTime.isBefore(startTime);
    }

    /**
     * Calculates the duration of the shift in minutes.
     * Handles midnight-crossing shifts correctly.
     *
     * @return duration in minutes, 0 for rest periods
     */
    public int getDurationMinutes() {
        if (isRestPeriod || startTime == null || endTime == null) {
            return 0;
        }

        if (crossesMidnight()) {
            // Calculate duration across midnight
            int minutesToMidnight = (24 * 60) - (startTime.getHour() * 60 + startTime.getMinute());
            int minutesFromMidnight = endTime.getHour() * 60 + endTime.getMinute();
            return minutesToMidnight + minutesFromMidnight;
        } else {
            // Regular same-day calculation
            return (endTime.getHour() * 60 + endTime.getMinute()) -
                    (startTime.getHour() * 60 + startTime.getMinute());
        }
    }

    /**
     * Gets the effective work duration excluding break time.
     *
     * @return work duration in minutes, excluding breaks
     */
    public int getWorkDurationMinutes() {
        int totalDuration = getDurationMinutes();
        if (hasBreakTime && breakStartTime != null && breakEndTime != null) {
            int breakDuration = (breakEndTime.getHour() * 60 + breakEndTime.getMinute()) -
                    (breakStartTime.getHour() * 60 + breakStartTime.getMinute());
            return Math.max(0, totalDuration - breakDuration);
        }
        return totalDuration;
    }

    /**
     * Gets work hours in decimal format (e.g., 8.5 for 8 hours 30 minutes).
     *
     * @return work hours as decimal
     */
    public double getWorkHours() {
        return getWorkDurationMinutes() / 60.0;
    }

    /**
     * Validates the shift type configuration.
     *
     * @return true if the shift type is valid
     */
    public boolean isValid() {
        // Name is required
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Color is required
        if (!isValidHexColor( colorHex )) {
            return false;
        }

        // Rest periods don't need time validation
        if (isRestPeriod) {
            return true;
        }

        // Work periods need start and end times
        if (startTime == null || endTime == null) {
            return false;
        }

        // Break time validation if present
        if (hasBreakTime) {
            if (breakStartTime == null || breakEndTime == null) {
                return false;
            }

            // Break must be within shift hours (simplified check for non-midnight crossing)
            return crossesMidnight() ||
                    (!breakStartTime.isBefore( startTime ) && !breakEndTime.isAfter( endTime ));
        }

        return true;
    }

    /**
     * Validates hex color format.
     *
     * @param color hex color string
     * @return true if valid hex color format
     */
    private boolean isValidHexColor(String color) {
        if (color == null) return false;
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    /**
     * Updates the timestamp when the shift type is modified.
     */
    public void markAsUpdated() {
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        markAsUpdated();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        markAsUpdated();
    }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        markAsUpdated();
    }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        markAsUpdated();
    }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
        markAsUpdated();
    }

    public boolean isRestPeriod() { return isRestPeriod; }
    public void setRestPeriod(boolean restPeriod) {
        isRestPeriod = restPeriod;
        markAsUpdated();
    }

    public boolean isHasBreakTime() { return hasBreakTime; }
    public void setHasBreakTime(boolean hasBreakTime) {
        this.hasBreakTime = hasBreakTime;
        markAsUpdated();
    }

    public LocalTime getBreakStartTime() { return breakStartTime; }
    public void setBreakStartTime(LocalTime breakStartTime) {
        this.breakStartTime = breakStartTime;
        markAsUpdated();
    }

    public LocalTime getBreakEndTime() { return breakEndTime; }
    public void setBreakEndTime(LocalTime breakEndTime) {
        this.breakEndTime = breakEndTime;
        markAsUpdated();
    }

    public boolean isUserDefined() { return isUserDefined; }
    public void setUserDefined(boolean userDefined) {
        isUserDefined = userDefined;
        markAsUpdated();
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        markAsUpdated();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftType shiftType = (ShiftType) o;
        return Objects.equals(id, shiftType.id) && Objects.equals(name, shiftType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ShiftType{")
                .append("id=").append(id)
                .append(", name='").append(name).append('\'')
                .append(", isRestPeriod=").append(isRestPeriod);

        if (!isRestPeriod && startTime != null && endTime != null) {
            sb.append(", time=").append(startTime).append("-").append(endTime);
            if (crossesMidnight()) {
                sb.append(" (crosses midnight)");
            }
        }

        sb.append(", color=").append(colorHex)
                .append(", userDefined=").append(isUserDefined)
                .append(", active=").append(isActive)
                .append('}');

        return sb.toString();
    }
}