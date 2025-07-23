package net.calvuz.qdue.smartshifts.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing different types of work shifts
 * Examples: Morning, Afternoon, Night, Rest
 */
@Entity(
        tableName = "shift_types",
        indices = {
                @Index(value = {"is_active", "sort_order"}),
                @Index(value = {"is_custom"})
        }
)
public class ShiftType {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;                    // "morning", "afternoon", "night", "rest"

    @NonNull
    @ColumnInfo(name = "name")
    public String name;                  // Localized name from strings.xml

    @NonNull
    @ColumnInfo(name = "start_time")
    public String startTime;             // "06:00" (HH:mm format)

    @NonNull
    @ColumnInfo(name = "end_time")
    public String endTime;               // "14:00" (HH:mm format)

    @NonNull
    @ColumnInfo(name = "color_hex")
    public String colorHex;              // "#4CAF50" 

    @NonNull
    @ColumnInfo(name = "icon_name")
    public String iconName;              // "ic_morning", "ic_afternoon", etc.

    @ColumnInfo(name = "is_working_shift")
    public boolean isWorkingShift;       // false only for "Rest"

    @ColumnInfo(name = "sort_order")
    public int sortOrder;                // Display order

    @ColumnInfo(name = "is_custom")
    public boolean isCustom;             // true if added by user

    @ColumnInfo(name = "is_active")
    public boolean isActive;             // soft delete flag

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    // Default constructor for Room
    public ShiftType() {}

    // Constructor for creating predefined shift types
    public ShiftType(@NonNull String id, @NonNull String name, @NonNull String startTime,
                     @NonNull String endTime, @NonNull String colorHex, @NonNull String iconName,
                     boolean isWorkingShift, int sortOrder, boolean isCustom) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorHex = colorHex;
        this.iconName = iconName;
        this.isWorkingShift = isWorkingShift;
        this.sortOrder = sortOrder;
        this.isCustom = isCustom;
        this.isActive = true;
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    /**
     * Check if this shift spans midnight (night shift)
     */
    public boolean spansNextDay() {
        try {
            int start = Integer.parseInt(startTime.replace(":", ""));
            int end = Integer.parseInt(endTime.replace(":", ""));
            return start > end; // e.g., 22:00 > 06:00
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get duration in minutes
     */
    public int getDurationMinutes() {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

            if (spansNextDay()) {
                endMinutes += 24 * 60; // Add 24 hours
            }

            return endMinutes - startMinutes;
        } catch (Exception e) {
            return 0;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ShiftType{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", isWorkingShift=" + isWorkingShift +
                '}';
    }


    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return name;
    }
    @NonNull
    public String getStartTime() {
        return startTime;
    }

    @NonNull
    public String getEndTime() {
        return endTime;
    }

    @NonNull
    public String getColorHex() {
        return colorHex;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setStartTime(@NonNull String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(@NonNull String endTime) {
        this.endTime = endTime;
    }

}

// =====================================================================

