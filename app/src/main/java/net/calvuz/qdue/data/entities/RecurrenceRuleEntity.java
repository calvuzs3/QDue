package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.core.db.converters.CalendarTypeConverters;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * RecurrenceRuleEntity - Room database entity for recurrence patterns.
 *
 * <p>Stores Google Calendar-style RRULE patterns in the database with full support for
 * QuattroDue cycles, weekly patterns, and complex recurrence rules. Designed for
 * optimal query performance and storage efficiency.</p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li><strong>Primary Key</strong>: UUID-based ID for global uniqueness</li>
 *   <li><strong>Indexes</strong>: Optimized for frequency, start_date, and active status queries</li>
 *   <li><strong>JSON Storage</strong>: Complex arrays stored as JSON for flexibility</li>
 *   <li><strong>Type Converters</strong>: Automatic conversion between domain and database types</li>
 * </ul>
 *
 * <h3>Performance Optimizations:</h3>
 * <ul>
 *   <li>Composite index on (frequency, active, start_date) for common queries</li>
 *   <li>Separate index on start_date for date range queries</li>
 *   <li>JSON storage for arrays minimizes table complexity</li>
 *   <li>Timestamp fields for efficient sorting and filtering</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Engine Database
 * @since Clean Architecture Phase 2
 */
@Entity(
        tableName = "recurrence_rules",
        indices = {
                @Index(value = {"frequency", "active", "start_date"}, name = "idx_recurrence_frequency_active_start"),
                @Index(value = {"start_date", "end_date"}, name = "idx_recurrence_date_range"),
                @Index(value = {"active", "created_at"}, name = "idx_recurrence_active_created"),
                @Index(value = {"frequency"}, name = "idx_recurrence_frequency")
        }
)
public class RecurrenceRuleEntity {

    // ==================== PRIMARY KEY ====================

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    // ==================== IDENTIFICATION ====================

    @Nullable
    @ColumnInfo(name = "name")
    private String name;

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    // ==================== CORE RECURRENCE DATA ====================

    @NonNull
    @ColumnInfo(name = "frequency")
    private String frequency; // DAILY, WEEKLY, MONTHLY, YEARLY, QUATTRODUE_CYCLE

    @ColumnInfo(name = "interval_value", defaultValue = "1")
    private int intervalValue;

    @NonNull
    @ColumnInfo(name = "start_date")
    private String startDate; // ISO format: yyyy-MM-dd

    @NonNull
    @ColumnInfo(name = "end_type", defaultValue = "NEVER")
    private String endType; // NEVER, COUNT, UNTIL_DATE

    @Nullable
    @ColumnInfo(name = "end_date")
    private String endDate; // ISO format: yyyy-MM-dd (nullable)

    @Nullable
    @ColumnInfo(name = "occurrence_count")
    private Integer count;

    // ==================== WEEKLY PATTERN DATA ====================

    @Nullable
    @ColumnInfo(name = "by_day_json")
    private String byDayJson; // JSON array: ["MONDAY","TUESDAY","WEDNESDAY"]

    @NonNull
    @ColumnInfo(name = "week_start", defaultValue = "MONDAY")
    private String weekStart; // MONDAY, TUESDAY, etc.

    // ==================== MONTHLY PATTERN DATA ====================

    @Nullable
    @ColumnInfo(name = "by_month_day_json")
    private String byMonthDayJson; // JSON array: [1,15,31]

    @Nullable
    @ColumnInfo(name = "by_month_json")
    private String byMonthJson; // JSON array: [1,6,12]

    // ==================== QUATTRODUE PATTERN DATA ====================

    @Nullable
    @ColumnInfo(name = "cycle_length")
    private Integer cycleLength; // 42 for QuattroDue

    @Nullable
    @ColumnInfo(name = "work_days")
    private Integer workDays; // 28 for QuattroDue

    @Nullable
    @ColumnInfo(name = "rest_days")
    private Integer restDays; // 14 for QuattroDue

    // ==================== METADATA ====================

    @ColumnInfo(name = "active", defaultValue = "1")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public RecurrenceRuleEntity() {
        // Room requires a no-argument constructor
    }

    /**
     * Constructor with required fields.
     */
    public RecurrenceRuleEntity(@NonNull String id, @NonNull String frequency,
                                int intervalValue, @NonNull String startDate,
                                @NonNull String endType) {
        this.id = id;
        this.frequency = frequency;
        this.intervalValue = intervalValue;
        this.startDate = startDate;
        this.endType = endType;
        this.weekStart = "MONDAY";
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @Nullable
    public String getName() { return name; }
    public void setName(@Nullable String name) { this.name = name; }

    @Nullable
    public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    @NonNull
    public String getFrequency() { return frequency; }
    public void setFrequency(@NonNull String frequency) { this.frequency = frequency; }

    public int getIntervalValue() { return intervalValue; }
    public void setIntervalValue(int intervalValue) { this.intervalValue = intervalValue; }

    @NonNull
    public String getStartDate() { return startDate; }
    public void setStartDate(@NonNull String startDate) { this.startDate = startDate; }

    @NonNull
    public String getEndType() { return endType; }
    public void setEndType(@NonNull String endType) { this.endType = endType; }

    @Nullable
    public String getEndDate() { return endDate; }
    public void setEndDate(@Nullable String endDate) { this.endDate = endDate; }

    @Nullable
    public Integer getCount() { return count; }
    public void setCount(@Nullable Integer count) { this.count = count; }

    @Nullable
    public String getByDayJson() { return byDayJson; }
    public void setByDayJson(@Nullable String byDayJson) { this.byDayJson = byDayJson; }

    @NonNull
    public String getWeekStart() { return weekStart; }
    public void setWeekStart(@NonNull String weekStart) { this.weekStart = weekStart; }

    @Nullable
    public String getByMonthDayJson() { return byMonthDayJson; }
    public void setByMonthDayJson(@Nullable String byMonthDayJson) { this.byMonthDayJson = byMonthDayJson; }

    @Nullable
    public String getByMonthJson() { return byMonthJson; }
    public void setByMonthJson(@Nullable String byMonthJson) { this.byMonthJson = byMonthJson; }

    @Nullable
    public Integer getCycleLength() { return cycleLength; }
    public void setCycleLength(@Nullable Integer cycleLength) { this.cycleLength = cycleLength; }

    @Nullable
    public Integer getWorkDays() { return workDays; }
    public void setWorkDays(@Nullable Integer workDays) { this.workDays = workDays; }

    @Nullable
    public Integer getRestDays() { return restDays; }
    public void setRestDays(@Nullable Integer restDays) { this.restDays = restDays; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // ==================== BUSINESS METHODS ====================

    /**
     * Convert to domain model.
     * This method creates a RecurrenceRule domain object from the entity.
     */
    @NonNull
    public RecurrenceRule toDomainModel() {
        RecurrenceRule.Builder builder = RecurrenceRule.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .frequency(RecurrenceRule.Frequency.valueOf(this.frequency))
                .interval(this.intervalValue)
                .startDate(LocalDate.parse(this.startDate))
                .endType(RecurrenceRule.EndType.valueOf(this.endType))
                // Week start
                .weekStart( RecurrenceRule.WeekStart.valueOf(this.weekStart))
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);

        // End date
        if (this.endDate != null) {
            builder.endDate(LocalDate.parse(this.endDate));
        }

        // Count
        if (this.count != null) {
            builder.count(this.count);
        }

        // By day (JSON deserialization handled by converter)
        if (this.byDayJson != null) {
            List<DayOfWeek> byDay = CalendarTypeConverters.fromDayOfWeekListJson(this.byDayJson);
            builder.byDay(byDay);
        }

        // By month day (JSON deserialization)
        if (this.byMonthDayJson != null) {
            List<Integer> byMonthDay = CalendarTypeConverters.fromIntegerListJson(this.byMonthDayJson);
            builder.byMonthDay(byMonthDay.toArray(new Integer[0]));
        }

        // By month (JSON deserialization)
        if (this.byMonthJson != null) {
            List<Integer> byMonth = CalendarTypeConverters.fromIntegerListJson(this.byMonthJson);
            builder.byMonth(byMonth.toArray(new Integer[0]));
        }

        // QuattroDue cycle data
        if (this.cycleLength != null) {
            builder.cycleLength(this.cycleLength);
        }
        if (this.workDays != null) {
            builder.workDays(this.workDays);
        }
        if (this.restDays != null) {
            builder.restDays(this.restDays);
        }

        return builder.build();
    }

    /**
     * Create entity from domain model.
     * This static method converts a RecurrenceRule domain object to an entity.
     */
    @NonNull
    public static RecurrenceRuleEntity fromDomainModel(@NonNull RecurrenceRule domainModel) {
        RecurrenceRuleEntity entity = new RecurrenceRuleEntity();

        // Basic fields
        entity.setId(domainModel.getId());
        entity.setName(domainModel.getName());
        entity.setDescription(domainModel.getDescription());
        entity.setFrequency(domainModel.getFrequency().name());
        entity.setIntervalValue(domainModel.getInterval());
        entity.setStartDate(domainModel.getStartDate().toString());
        entity.setEndType(domainModel.getEndType().name());
        entity.setActive(domainModel.isActive());
        entity.setCreatedAt(domainModel.getCreatedAt());
        entity.setUpdatedAt(domainModel.getUpdatedAt());

        // Optional fields
        if (domainModel.getEndDate() != null) {
            entity.setEndDate(domainModel.getEndDate().toString());
        }
        if (domainModel.getCount() != null) {
            entity.setCount(domainModel.getCount());
        }

        // Week start
        entity.setWeekStart(domainModel.getWeekStart().name());

        // By day (JSON serialization)
        if (!domainModel.getByDay().isEmpty()) {
            entity.setByDayJson( CalendarTypeConverters.fromDayOfWeekList(domainModel.getByDay()));
        }

        // By month day (JSON serialization)
        if (!domainModel.getByMonthDay().isEmpty()) {
            entity.setByMonthDayJson( CalendarTypeConverters.fromIntegerList(domainModel.getByMonthDay()));
        }

        // By month (JSON serialization)
        if (!domainModel.getByMonth().isEmpty()) {
            entity.setByMonthJson( CalendarTypeConverters.fromIntegerList(domainModel.getByMonth()));
        }

        // QuattroDue cycle data
        entity.setCycleLength(domainModel.getCycleLength());
        entity.setWorkDays(domainModel.getWorkDays());
        entity.setRestDays(domainModel.getRestDays());

        return entity;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Update timestamp to current time.
     */
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Check if this is a QuattroDue cycle rule.
     */
    public boolean isQuattroDueCycle() {
        return "QUATTRODUE_CYCLE".equals(this.frequency);
    }

    /**
     * Check if this is a weekly rule with specific days.
     */
    public boolean isWeeklyWithDays() {
        return "WEEKLY".equals(this.frequency) && this.byDayJson != null;
    }

    /**
     * Check if this rule has an end condition.
     */
    public boolean hasEndCondition() {
        return !"NEVER".equals(this.endType);
    }

    // ==================== TOSTRING ====================

    @Override
    @NonNull
    public String toString() {
        return "RecurrenceRuleEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", frequency='" + frequency + '\'' +
                ", interval=" + intervalValue +
                ", startDate='" + startDate + '\'' +
                ", endType='" + endType + '\'' +
                ", active=" + active +
                '}';
    }
}