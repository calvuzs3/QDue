package net.calvuz.qdue.smartshifts.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing user assignment to a specific shift pattern
 * Links users to their active shift pattern with start date and team info
 */
@Entity(
        tableName = "user_shift_assignments",
        foreignKeys = {
                @ForeignKey(
                        entity = ShiftPattern.class,
                        parentColumns = "id",
                        childColumns = "shift_pattern_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"user_id", "is_active"}),
                @Index(value = {"shift_pattern_id"}),
                @Index(value = {"start_date"})
        }
)
public class UserShiftAssignment {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;                    // UUID generated

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;                // User identifier

    @NonNull
    @ColumnInfo(name = "shift_pattern_id")
    public String shiftPatternId;        // FK to shift_patterns

    @NonNull
    @ColumnInfo(name = "start_date")
    public String startDate;             // "2025-01-15" - cycle start date

    @ColumnInfo(name = "cycle_offset_days")
    public int cycleOffsetDays;          // offset from team A (0 for single user)

    @Nullable
    @ColumnInfo(name = "team_name")
    public String teamName;              // "Team A", "Personal", etc.

    @Nullable
    @ColumnInfo(name = "team_color_hex")
    public String teamColorHex;          // team color

    @ColumnInfo(name = "is_active")
    public boolean isActive;             // only one active assignment per user

    @ColumnInfo(name = "assigned_at")
    public long assignedAt;

    // Default constructor for Room
    public UserShiftAssignment() {}

    // Constructor for creating assignments
    public UserShiftAssignment(@NonNull String id, @NonNull String userId,
                               @NonNull String shiftPatternId, @NonNull String startDate,
                               int cycleOffsetDays, @Nullable String teamName,
                               @Nullable String teamColorHex) {
        this.id = id;
        this.userId = userId;
        this.shiftPatternId = shiftPatternId;
        this.startDate = startDate;
        this.cycleOffsetDays = cycleOffsetDays;
        this.teamName = teamName;
        this.teamColorHex = teamColorHex;
        this.isActive = true;
        this.assignedAt = System.currentTimeMillis();
    }

    /**
     * Check if this is a personal (single user) assignment
     */
    public boolean isPersonalAssignment() {
        return cycleOffsetDays == 0 && (teamName == null || teamName.contains("Personal"));
    }

    /**
     * Check if this is a team assignment
     */
    public boolean isTeamAssignment() {
        return !isPersonalAssignment();
    }

    @Override
    public String toString() {
        return "UserShiftAssignment{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", shiftPatternId='" + shiftPatternId + '\'' +
                ", startDate='" + startDate + '\'' +
                ", teamName='" + teamName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

// =====================================================================

