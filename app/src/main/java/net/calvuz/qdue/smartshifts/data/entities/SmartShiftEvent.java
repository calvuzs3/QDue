package net.calvuz.qdue.smartshifts.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing generated shift events for specific dates
 * These are the actual work shifts calculated from patterns
 */
@Entity(
        tableName = "smart_shift_events",
        foreignKeys = {
                @ForeignKey(
                        entity = ShiftPattern.class,
                        parentColumns = "id",
                        childColumns = "shift_pattern_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ShiftType.class,
                        parentColumns = "id",
                        childColumns = "shift_type_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"user_id", "event_date"}),
                @Index(value = {"shift_pattern_id", "event_date"}),
                @Index(value = {"master_event_id"}),
                @Index(value = {"event_type", "status"})
        }
)
public class SmartShiftEvent {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;                    // UUID generated

    @NonNull
    @ColumnInfo(name = "event_type")
    public String eventType;             // "master", "instance", "exception"

    @Nullable
    @ColumnInfo(name = "master_event_id")
    public String masterEventId;         // FK for derived events

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "shift_pattern_id")
    public String shiftPatternId;

    @NonNull
    @ColumnInfo(name = "shift_type_id")
    public String shiftTypeId;           // FK to shift_types

    @NonNull
    @ColumnInfo(name = "event_date")
    public String eventDate;             // "2025-07-15" (ISO date format)

    @NonNull
    @ColumnInfo(name = "start_time")
    public String startTime;             // "06:00" (HH:mm format)

    @NonNull
    @ColumnInfo(name = "end_time")
    public String endTime;               // "14:00" (HH:mm format)

    @ColumnInfo(name = "cycle_day_number")
    public int cycleDayNumber;           // day in cycle (1-18 for 4-2)

    @NonNull
    @ColumnInfo(name = "status")
    public String status;                // "active", "modified", "deleted"

    @Nullable
    @ColumnInfo(name = "exception_reason")
    public String exceptionReason;       // reason for modification

    @ColumnInfo(name = "generated_at")
    public long generatedAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    // Default constructor for Room
    public SmartShiftEvent() {
    }

    // Constructor for creating events
    public SmartShiftEvent(@NonNull String id, @NonNull String eventType,
                           @NonNull String userId, @NonNull String shiftPatternId,
                           @NonNull String shiftTypeId, @NonNull String eventDate,
                           @NonNull String startTime, @NonNull String endTime,
                           int cycleDayNumber, @NonNull String status) {
        this.id = id;
        this.eventType = eventType;
        this.userId = userId;
        this.shiftPatternId = shiftPatternId;
        this.shiftTypeId = shiftTypeId;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cycleDayNumber = cycleDayNumber;
        this.status = status;
        long currentTime = System.currentTimeMillis();
        this.generatedAt = currentTime;
        this.updatedAt = currentTime;
    }

    /**
     * Check if this is a master recurring event
     */
    public boolean isMasterEvent() {
        return "master".equals(eventType);
    }

    /**
     * Check if this is an exception to the pattern
     */
    public boolean isException() {
        return "exception".equals(eventType);
    }

    /**
     * Check if this event is active
     */
    public boolean isActive() {
        return "active".equals(status);
    }

    /**
     * Get full datetime string for start
     */
    public String getStartDateTime() {
        return eventDate + "T" + startTime + ":00";
    }

    /**
     * Get full datetime string for end
     */
    public String getEndDateTime() {
        return eventDate + "T" + endTime + ":00";
    }

    /**
     * Get event date as a string
     *
     * @return String representation of date
     */
    public String getEventDate() {
        return eventDate;
    }

    @Override
    public String toString() {
        return "SmartShiftEvent{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
