package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

/**
 * ShiftEntity - Database entity for shift types persistence.
 *
 * <p>Represents a shift type stored in the database with complete configuration
 * including timing, breaks, and display properties. This entity stores what
 * previously was represented by ShiftType but now as persistent data.</p>
 *
 * <h3>Database Features:</h3>
 * <ul>
 *   <li><strong>Room Entity</strong>: Full Room database integration</li>
 *   <li><strong>Indexed Queries</strong>: Optimized lookups by name and type</li>
 *   <li><strong>Soft Delete</strong>: Active/inactive status instead of deletion</li>
 *   <li><strong>Audit Fields</strong>: Creation/update timestamps</li>
 *   <li><strong>Validation Support</strong>: Database constraints</li>
 * </ul>
 *
 * <h3>Shift Types:</h3>
 * <ul>
 *   <li><strong>MORNING</strong>: Early shift (typically 06:00-14:00)</li>
 *   <li><strong>AFTERNOON</strong>: Middle shift (typically 14:00-22:00)</li>
 *   <li><strong>NIGHT</strong>: Night shift (typically 22:00-06:00)</li>
 *   <li><strong>CUSTOM</strong>: User-defined shifts</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 7
 */
@Entity(
        tableName = "shifts",
        indices = {
                @Index(value = {"name"}, unique = true),
                @Index(value = {"shift_type"}),
                @Index(value = {"active"})
        }
)
public class ShiftEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    @NonNull
    @ColumnInfo(name = "shift_type")
    private String shiftType; // MORNING, AFTERNOON, NIGHT, CUSTOM

    @NonNull
    @ColumnInfo(name = "start_time")
    private String startTime; // Stored as "HH:mm" format

    @NonNull
    @ColumnInfo(name = "end_time")
    private String endTime; // Stored as "HH:mm" format

    @ColumnInfo(name = "crosses_midnight")
    private boolean crossesMidnight;

    @Nullable
    @ColumnInfo(name = "color_hex")
    private String colorHex;

    @ColumnInfo(name = "is_user_relevant")
    private boolean isUserRelevant;

    @ColumnInfo(name = "has_break_time")
    private boolean hasBreakTime;

    @ColumnInfo(name = "is_break_time_included")
    private boolean isBreakTimeIncluded;

    @ColumnInfo(name = "break_time_duration_minutes")
    private long breakTimeDurationMinutes;

    @ColumnInfo(name = "display_order")
    private int displayOrder;

    @ColumnInfo(name = "active")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public ShiftEntity() {
        this.id = UUID.randomUUID().toString();
        this.name = "New Shift";
        this.shiftType = ShiftTypes.CUSTOM;
        this.startTime = "08:00";
        this.endTime = "17:00";
        this.active = true;
        this.crossesMidnight = false;
        this.isUserRelevant = false;
        this.hasBreakTime = true;
        this.isBreakTimeIncluded = false;
        this.breakTimeDurationMinutes = 60;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.displayOrder = 0;
    }

    /**
     * Constructor with basic information.
     *
     * @param id Unique identifier
     * @param name Shift name
     * @param shiftType Shift type (MORNING, AFTERNOON, NIGHT, CUSTOM)
     * @param startTime Start time in "HH:mm" format
     * @param endTime End time in "HH:mm" format
     */
    public ShiftEntity(@NonNull String id, @NonNull String name, @NonNull String shiftType,
                       @NonNull String startTime, @NonNull String endTime) {
        this();
        this.id = id;
        this.name = name;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.crossesMidnight = calculateCrossesMidnight(startTime, endTime);
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NonNull
    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(@NonNull String shiftType) {
        this.shiftType = shiftType;
    }

    @NonNull
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(@NonNull String startTime) {
        this.startTime = startTime;
        this.crossesMidnight = calculateCrossesMidnight(startTime, this.endTime);
        this.updatedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(@NonNull String endTime) {
        this.endTime = endTime;
        this.crossesMidnight = calculateCrossesMidnight(this.startTime, endTime);
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isCrossesMidnight() {
        return crossesMidnight;
    }

    public void setCrossesMidnight(boolean crossesMidnight) {
        this.crossesMidnight = crossesMidnight;
    }

    @Nullable
    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(@Nullable String colorHex) {
        this.colorHex = colorHex;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isUserRelevant() {
        return isUserRelevant;
    }

    public void setUserRelevant(boolean userRelevant) {
        isUserRelevant = userRelevant;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isHasBreakTime() {
        return hasBreakTime;
    }

    public void setHasBreakTime(boolean hasBreakTime) {
        this.hasBreakTime = hasBreakTime;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isBreakTimeIncluded() {
        return isBreakTimeIncluded;
    }

    public void setBreakTimeIncluded(boolean breakTimeIncluded) {
        isBreakTimeIncluded = breakTimeIncluded;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getBreakTimeDurationMinutes() {
        return breakTimeDurationMinutes;
    }

    public void setBreakTimeDurationMinutes(long breakTimeDurationMinutes) {
        this.breakTimeDurationMinutes = breakTimeDurationMinutes;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculate if shift crosses midnight based on start and end times.
     *
     * @param startTimeStr Start time in "HH:mm" format
     * @param endTimeStr End time in "HH:mm" format
     * @return true if shift crosses midnight
     */
    private boolean calculateCrossesMidnight(String startTimeStr, String endTimeStr) {
        try {
            LocalTime start = LocalTime.parse(startTimeStr);
            LocalTime end = LocalTime.parse(endTimeStr);
            return end.isBefore(start);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get start time as LocalTime object.
     *
     * @return LocalTime or null if parsing fails
     */
    @Nullable
    public LocalTime getStartTimeAsLocalTime() {
        try {
            return LocalTime.parse(startTime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get end time as LocalTime object.
     *
     * @return LocalTime or null if parsing fails
     */
    @Nullable
    public LocalTime getEndTimeAsLocalTime() {
        try {
            return LocalTime.parse(endTime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get break duration as Duration object.
     *
     * @return Duration of break time
     */
    @NonNull
    public Duration getBreakTimeDuration() {
        return Duration.ofMinutes(breakTimeDurationMinutes);
    }

    /**
     * Set break duration from Duration object.
     *
     * @param duration Break time duration
     */
    public void setBreakTimeDuration(@NonNull Duration duration) {
        this.breakTimeDurationMinutes = duration.toMinutes();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Calculate total shift duration in minutes.
     *
     * @return Total duration in minutes
     */
    public long getTotalDurationMinutes() {
        LocalTime start = getStartTimeAsLocalTime();
        LocalTime end = getEndTimeAsLocalTime();

        if (start == null || end == null) {
            return 0;
        }

        if (crossesMidnight) {
            // For shifts crossing midnight
            long minutesToMidnight = 24 * 60 - (start.getHour() * 60L + start.getMinute());
            long minutesFromMidnight = end.getHour() * 60L + end.getMinute();
            return minutesToMidnight + minutesFromMidnight;
        } else {
            // For normal shifts within same day
            return (end.getHour() * 60L + end.getMinute()) - (start.getHour() * 60L + start.getMinute());
        }
    }

    /**
     * Calculate work duration in minutes (excluding break if not included).
     *
     * @return Work duration in minutes
     */
    public long getWorkDurationMinutes() {
        long totalDuration = getTotalDurationMinutes();

        if (hasBreakTime && !isBreakTimeIncluded) {
            return totalDuration - breakTimeDurationMinutes;
        }

        return totalDuration;
    }

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        return startTime + "-" + endTime;
    }

    /**
     * Get display name with type and time.
     *
     * @return Display name for UI
     */
    @NonNull
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);

        String timeRange = getTimeRange();
        if (!timeRange.isEmpty()) {
            sb.append(" (").append(timeRange).append(")");
        }

        return sb.toString();
    }

    /**
     * Update the updated timestamp.
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ShiftEntity that = (ShiftEntity) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return "ShiftEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", shiftType='" + shiftType + '\'' +
                ", timeRange='" + getTimeRange() + '\'' +
                ", active=" + active +
                '}';
    }

    // ==================== CONSTANTS ====================

    /**
     * Standard shift type constants.
     */
    public static class ShiftTypes {
        public static final String MORNING = "MORNING";
        public static final String AFTERNOON = "AFTERNOON";
        public static final String NIGHT = "NIGHT";
        public static final String CUSTOM = "CUSTOM";
    }

    /**
     * Standard shift configuration constants.
     */
    public static class StandardShifts {
        public static final String MORNING_SHIFT_ID = "shift_morning";
        public static final String AFTERNOON_SHIFT_ID = "shift_afternoon";
        public static final String NIGHT_SHIFT_ID = "shift_night";
    }
}