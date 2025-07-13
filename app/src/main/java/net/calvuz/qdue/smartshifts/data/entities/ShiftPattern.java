package net.calvuz.qdue.smartshifts.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing recurring shift patterns
 * Examples: Continuous 4-2, Weekly 5-2, Custom patterns
 */
@Entity(
        tableName = "shift_patterns",
        indices = {
                @Index(value = {"is_predefined", "is_active"}),
                @Index(value = {"created_by_user_id"}),
                @Index(value = {"is_continuous_cycle"})
        }
)
public class ShiftPattern {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;                    // UUID generated

    @NonNull
    @ColumnInfo(name = "name")
    public String name;                  // "Continuous Cycle 4-2", "Custom User Pattern"

    @Nullable
    @ColumnInfo(name = "description")
    public String description;           // Detailed description

    @ColumnInfo(name = "cycle_length_days")
    public int cycleLengthDays;          // 18, 15, 7, etc.

    @NonNull
    @ColumnInfo(name = "recurrence_rule_json")
    public String recurrenceRuleJson;    // JSON with recurrence schema

    @ColumnInfo(name = "is_continuous_cycle")
    public boolean isContinuousCycle;    // continuous cycle validation result

    @ColumnInfo(name = "is_predefined")
    public boolean isPredefined;         // true for system patterns

    @ColumnInfo(name = "is_active")
    public boolean isActive;             // soft delete flag

    @Nullable
    @ColumnInfo(name = "created_by_user_id")
    public String createdByUserId;       // null for predefined patterns

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    // Default constructor for Room
    public ShiftPattern() {}

    // Constructor for creating patterns
    public ShiftPattern(@NonNull String id, @NonNull String name, @Nullable String description,
                        int cycleLengthDays, @NonNull String recurrenceRuleJson,
                        boolean isContinuousCycle, boolean isPredefined,
                        @Nullable String createdByUserId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cycleLengthDays = cycleLengthDays;
        this.recurrenceRuleJson = recurrenceRuleJson;
        this.isContinuousCycle = isContinuousCycle;
        this.isPredefined = isPredefined;
        this.createdByUserId = createdByUserId;
        this.isActive = true;
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    /**
     * Check if this is a custom user-created pattern
     */
    public boolean isCustomPattern() {
        return !isPredefined && createdByUserId != null;
    }

    /**
     * Check if pattern supports multiple teams
     */
    public boolean supportsMultipleTeams() {
        return isContinuousCycle && cycleLengthDays >= 7;
    }

    @Override
    public String toString() {
        return "ShiftPattern{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cycleLengthDays=" + cycleLengthDays +
                ", isContinuousCycle=" + isContinuousCycle +
                ", isPredefined=" + isPredefined +
                '}';
    }
}

// =====================================================================

