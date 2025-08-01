
package net.calvuz.qdue.core.infrastructure.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Database entity representing a shift type definition.
 *
 * <p>Stores shift type configurations in the database with proper indexing
 * for performance and uniqueness constraints for data integrity.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Unique shift type names within the same user context</li>
 *   <li>Optimized queries via name and active status indices</li>
 *   <li>Audit trail with creation and update timestamps</li>
 *   <li>Support for both predefined and user-defined shift types</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
@Entity(tableName = "shift_types",
        indices = {
                @Index(value = "name", unique = true),
                @Index(value = "is_active"),
                @Index(value = "is_user_defined")
        })
public class ShiftTypeEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "start_time")
    private LocalTime startTime;

    @ColumnInfo(name = "end_time")
    private LocalTime endTime;

    @ColumnInfo(name = "color_hex")
    private String colorHex;

    @ColumnInfo(name = "is_rest_period", defaultValue = "0")
    private boolean isRestPeriod;

    @ColumnInfo(name = "has_break_time", defaultValue = "0")
    private boolean hasBreakTime;

    @ColumnInfo(name = "break_start_time")
    private LocalTime breakStartTime;

    @ColumnInfo(name = "break_end_time")
    private LocalTime breakEndTime;

    @ColumnInfo(name = "is_user_defined", defaultValue = "0")
    private boolean isUserDefined;

    @ColumnInfo(name = "is_active", defaultValue = "1")
    private boolean isActive;

    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public ShiftTypeEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isUserDefined = false;
        this.isRestPeriod = false;
        this.hasBreakTime = false;
    }

    // Constructor for work shifts
    public ShiftTypeEntity(String name, String description, LocalTime startTime,
                           LocalTime endTime, String colorHex) {
        this();
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorHex = colorHex;
    }

    // Constructor for rest periods
    public ShiftTypeEntity(String name, String description, String colorHex, boolean isRestPeriod) {
        this();
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
        this.isRestPeriod = isRestPeriod;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRestPeriod() { return isRestPeriod; }
    public void setRestPeriod(boolean restPeriod) {
        isRestPeriod = restPeriod;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isHasBreakTime() { return hasBreakTime; }
    public void setHasBreakTime(boolean hasBreakTime) {
        this.hasBreakTime = hasBreakTime;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getBreakStartTime() { return breakStartTime; }
    public void setBreakStartTime(LocalTime breakStartTime) {
        this.breakStartTime = breakStartTime;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getBreakEndTime() { return breakEndTime; }
    public void setBreakEndTime(LocalTime breakEndTime) {
        this.breakEndTime = breakEndTime;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isUserDefined() { return isUserDefined; }
    public void setUserDefined(boolean userDefined) {
        isUserDefined = userDefined;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}