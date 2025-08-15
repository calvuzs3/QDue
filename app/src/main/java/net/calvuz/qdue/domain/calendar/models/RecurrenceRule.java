package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * RecurrenceRule - Domain model for Google Calendar-style recurrence patterns.
 *
 * <p>This domain model represents RRULE-compatible recurrence patterns for work schedules.
 * Supports standard RFC 5545 RRULE syntax while providing easy-to-use builder pattern
 * for common work schedule scenarios.</p>
 *
 * <h3>Supported Frequencies:</h3>
 * <ul>
 *   <li><strong>DAILY</strong>: Every N days</li>
 *   <li><strong>WEEKLY</strong>: Every N weeks on specific days</li>
 *   <li><strong>MONTHLY</strong>: Every N months on specific dates</li>
 *   <li><strong>QUATTRODUE_CYCLE</strong>: Custom 42-day QuattroDue pattern</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Every weekday (Monday to Friday)
 * RecurrenceRule weekdays = RecurrenceRule.builder()
 *     .frequency(Frequency.WEEKLY)
 *     .interval(1)
 *     .byDay(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
 *            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
 *     .build();
 *
 * // QuattroDue 4-2 cycle pattern
 * RecurrenceRule quattroDue = RecurrenceRule.builder()
 *     .frequency(Frequency.QUATTRODUE_CYCLE)
 *     .interval(1)
 *     .cycleLength(42)
 *     .startDate(LocalDate.of(2024, 1, 1))
 *     .build();
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Engine Implementation
 * @since Clean Architecture Phase 2
 */
public class RecurrenceRule extends LocalizableDomainModel {

    private static final String TAG = "RecurrenceRule";

    private static final String LOCALIZATION_SCOPE = "recurrence";

    // ==================== ENUMS ====================

    /**
     * Recurrence frequency types based on RFC 5545 RRULE standard.
     */
    public enum Frequency {
        DAILY,              // Every N days
        WEEKLY,             // Every N weeks on specific days
        MONTHLY,            // Every N months
        YEARLY,             // Every N years (future use)
        QUATTRODUE_CYCLE    // Custom QuattroDue 42-day cycle
    }

    /**
     * End condition for recurrence patterns.
     */
    public enum EndType {
        NEVER,              // No end date
        COUNT,              // End after N occurrences
        UNTIL_DATE          // End on specific date
    }

    public enum WeekStart {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    // ==================== IDENTIFICATION ====================

    private final String id;
    private final String name;
    private final String description;

    // ==================== CORE RECURRENCE DATA ====================

    private final Frequency frequency;
    private final int interval;            // Repeat every N frequency units
    private final LocalDate startDate;    // Pattern start date
    private final EndType endType;
    private final LocalDate endDate;      // End date (if EndType.UNTIL_DATE)
    private final Integer count;          // Number of occurrences (if EndType.COUNT)

    // ==================== WEEKLY PATTERN DATA ====================

    private final List<DayOfWeek> byDay;  // Days of week (for WEEKLY)
    private final WeekStart weekStart;    // Week start day (default: MONDAY)

    // ==================== MONTHLY PATTERN DATA ====================

    private final List<Integer> byMonthDay;  // Days of month (1-31)
    private final List<Integer> byMonth;     // Months (1-12)

    // ==================== QUATTRODUE PATTERN DATA ====================

    private final Integer cycleLength;      // Cycle length in days (42 for QuattroDue)
    private final Integer workDays;         // Work days in cycle (28 for QuattroDue)
    private final Integer restDays;         // Rest days in cycle (14 for QuattroDue)

    // ==================== METADATA ====================

    private final boolean active;
    private final long createdAt;
    private final long updatedAt;

    // ==================== CONSTRUCTOR ====================

    private RecurrenceRule(@NonNull Builder builder) {
        super(builder.mLocalizer, LOCALIZATION_SCOPE);

        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.description = builder.description;
        this.frequency = Objects.requireNonNull( builder.frequency, "Frequency cannot be null" );
        this.interval = Math.max( 1, builder.interval );
        this.startDate = Objects.requireNonNull( builder.startDate, "Start date cannot be null" );
        this.endType = builder.endType != null ? builder.endType : EndType.NEVER;
        this.endDate = builder.endDate;
        this.count = builder.count;

        // Weekly pattern
        this.byDay = builder.byDay != null ? new ArrayList<>( builder.byDay ) : new ArrayList<>();
        this.weekStart = builder.weekStart != null ? builder.weekStart : WeekStart.MONDAY;

        // Monthly pattern
        this.byMonthDay = builder.byMonthDay != null ? new ArrayList<>( builder.byMonthDay ) : new ArrayList<>();
        this.byMonth = builder.byMonth != null ? new ArrayList<>( builder.byMonth ) : new ArrayList<>();

        // QuattroDue pattern
        this.cycleLength = builder.cycleLength;
        this.workDays = builder.workDays;
        this.restDays = builder.restDays;

        // Metadata
        this.active = builder.active;
        this.createdAt = builder.createdAt > 0 ? builder.createdAt : System.currentTimeMillis();
        this.updatedAt = builder.updatedAt > 0 ? builder.updatedAt : System.currentTimeMillis();

        validateRule();
    }

    // ==================== VALIDATION ====================

    /**
     * Validate recurrence rule consistency.
     */
    private void validateRule() {
        switch (frequency) {
            case WEEKLY:
                if (byDay.isEmpty()) {
                    throw new IllegalArgumentException( "WEEKLY frequency requires at least one day of week" );
                }
                break;

            case MONTHLY:
                if (byMonthDay.isEmpty() && byMonth.isEmpty()) {
                    throw new IllegalArgumentException( "MONTHLY frequency requires month days or months" );
                }
                break;

            case QUATTRODUE_CYCLE:
                if (cycleLength == null || cycleLength <= 0) {
                    throw new IllegalArgumentException( "QUATTRODUE_CYCLE requires valid cycle length" );
                }
                break;
        }

        if (endType == EndType.UNTIL_DATE && endDate == null) {
            throw new IllegalArgumentException( "UNTIL_DATE end type requires end date" );
        }

        if (endType == EndType.COUNT && (count == null || count <= 0)) {
            throw new IllegalArgumentException( "COUNT end type requires positive count" );
        }

        if (endDate != null && endDate.isBefore( startDate )) {
            throw new IllegalArgumentException( "End date cannot be before start date" );
        }
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public Frequency getFrequency() {
        return frequency;
    }

    public int getInterval() {
        return interval;
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    @NonNull
    public EndType getEndType() {
        return endType;
    }

    @Nullable
    public LocalDate getEndDate() {
        return endDate;
    }

    @Nullable
    public Integer getCount() {
        return count;
    }

    @NonNull
    public List<DayOfWeek> getByDay() {
        return Collections.unmodifiableList( byDay );
    }

    @NonNull
    public WeekStart getWeekStart() { return weekStart; }

    @NonNull
    public List<Integer> getByMonthDay() {
        return Collections.unmodifiableList( byMonthDay );
    }

    @NonNull
    public List<Integer> getByMonth() {
        return Collections.unmodifiableList( byMonth );
    }

    @Nullable
    public Integer getCycleLength() {
        return cycleLength;
    }

    @Nullable
    public Integer getWorkDays() {
        return workDays;
    }

    @Nullable
    public Integer getRestDays() {
        return restDays;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Check if this rule applies to a specific date.
     */
    public boolean appliesTo(@NonNull LocalDate date) {
        if (date.isBefore( startDate )) {
            return false;
        }

        if (endType == EndType.UNTIL_DATE && endDate != null && date.isAfter( endDate )) {
            return false;
        }

        return matchesPattern( date );
    }

    /**
     * Check if date matches the recurrence pattern.
     */
    private boolean matchesPattern(@NonNull LocalDate date) {
        switch (frequency) {
            case DAILY:
                return matchesDailyPattern( date );
            case WEEKLY:
                return matchesWeeklyPattern( date );
            case MONTHLY:
                return matchesMonthlyPattern( date );
            case QUATTRODUE_CYCLE:
                return matchesQuattroDuePattern( date );
            default:
                return false;
        }
    }

    private boolean matchesDailyPattern(@NonNull LocalDate date) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between( startDate, date );
        return daysBetween >= 0 && daysBetween % interval == 0;
    }

    private boolean matchesWeeklyPattern(@NonNull LocalDate date) {
        if (!byDay.contains( date.getDayOfWeek() )) {
            return false;
        }

        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between( startDate, date );
        return weeksBetween >= 0 && weeksBetween % interval == 0;
    }

    private boolean matchesMonthlyPattern(@NonNull LocalDate date) {
        if (!byMonthDay.isEmpty() && !byMonthDay.contains( date.getDayOfMonth() )) {
            return false;
        }

        if (!byMonth.isEmpty() && !byMonth.contains( date.getMonthValue() )) {
            return false;
        }

        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between( startDate, date );
        return monthsBetween >= 0 && monthsBetween % interval == 0;
    }

    private boolean matchesQuattroDuePattern(@NonNull LocalDate date) {
        if (cycleLength == null) {
            return false;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between( startDate, date );
        if (daysBetween < 0) {
            return false;
        }

        // QuattroDue cycle logic would go here
        // For now, simple modulo check
        return daysBetween % cycleLength < (workDays != null ? workDays : 28);
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get localized display name for UI.
     * Falls back to name property or generates from frequency if name is null.
     */
    @NonNull
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return localize("frequency." + frequency.name().toLowerCase(),
                frequency.name(), // fallback
                frequency.name());
    }

    /**
     * Get localized description for UI.
     * Falls back to description property or generates from frequency if description is null.
     */
    @NonNull
    public String getDisplayDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return localize("frequency." + frequency.name().toLowerCase() + ".description",
                "Every " + interval + " " + frequency.name().toLowerCase(), // fallback
                interval, frequency.name().toLowerCase());

    }

    /**
     * Get localized end condition description.
     */
    @NonNull
    public String getEndConditionDescription() {
        switch (endType) {
            case NEVER:
                return localize("end.never", "Never");
            case COUNT:
                return localize("end.count", "After {0} occurrences", count);
            case UNTIL_DATE:
                return localize("end.until", "Until {0}", endDate);
            default:
                return localizeOrKey("end." + endType.name().toLowerCase());
        }
    }

    @NonNull
    public String toRRuleString() {
        StringBuilder rrule = new StringBuilder( "RRULE:" );

        // Frequency
        rrule.append( "FREQ=" ).append( frequency.name() );

        // Interval
        if (interval > 1) {
            rrule.append( ";INTERVAL=" ).append( interval );
        }

        // By day (weekly)
        if (!byDay.isEmpty()) {
            rrule.append( ";BYDAY=" );
            for (int i = 0; i < byDay.size(); i++) {
                if (i > 0) rrule.append( "," );
                rrule.append( dayOfWeekToRRule( byDay.get( i ) ) );
            }
        }

        // By month day
        if (!byMonthDay.isEmpty()) {
            rrule.append( ";BYMONTHDAY=" );
            for (int i = 0; i < byMonthDay.size(); i++) {
                if (i > 0) rrule.append( "," );
                rrule.append( byMonthDay.get( i ) );
            }
        }

        // End condition
        switch (endType) {
            case COUNT:
                if (count != null) {
                    rrule.append( ";COUNT=" ).append( count );
                }
                break;
            case UNTIL_DATE:
                if (endDate != null) {
                    rrule.append( ";UNTIL=" ).append( endDate.toString().replace( "-", "" ) );
                }
                break;
        }

        return rrule.toString();
    }

    private String dayOfWeekToRRule(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "MO";
            case TUESDAY:
                return "TU";
            case WEDNESDAY:
                return "WE";
            case THURSDAY:
                return "TH";
            case FRIDAY:
                return "FR";
            case SATURDAY:
                return "SA";
            case SUNDAY:
                return "SU";
            default:
                return "";
        }
    }

    // ==================== FACTORY METHODS ====================

    @NonNull
    public static RecurrenceRule createQuattroDueCycle(@NonNull LocalDate startDate,
                                                       @Nullable DomainLocalizer localizer) {
        return builder()
                .frequency(Frequency.QUATTRODUE_CYCLE)
                .startDate(startDate)
                .cycleLength(18)
                .workDays(4)
                .restDays(2)
                .localizer(localizer)
                .build();
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Create a copy of this object with localizer injected.
     * Useful for adding localization to existing instances.
     *
     * @param localizer DomainLocalizer to inject
     * @return New instance with localizer support
     */
    @Override
    @NonNull
    public RecurrenceRule withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder()
                .copyFrom(this)
                .localizer(localizer)
                .build();
    }

    // ==================== BUILDER ====================

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends LocalizableBuilder<RecurrenceRule, Builder> {

        private String id;
        private String name;
        private String description;
        private Frequency frequency;
        private int interval = 1;
        private LocalDate startDate;
        private EndType endType = EndType.NEVER;
        private LocalDate endDate;
        private Integer count;
        private List<DayOfWeek> byDay;
        private WeekStart weekStart = WeekStart.MONDAY;
        private List<Integer> byMonthDay;
        private List<Integer> byMonth;
        private Integer cycleLength;
        private Integer workDays;
        private Integer restDays;
        private boolean active = true;
        private long createdAt;
        private long updatedAt;

        @NonNull
        public Builder copyFrom(@NonNull RecurrenceRule source) {
            this.id = source.id;
            this.name = source.name;
            this.description = source.description;
            this.frequency = source.frequency;
            this.interval = source.interval;
            this.startDate = source.startDate;
            this.endType = source.endType;
            this.endDate = source.endDate;
            this.count = source.count;
            this.byDay = new ArrayList<>( source.byDay );
            this.weekStart = source.weekStart;
            this.byMonthDay = new ArrayList<>( source.byMonthDay );
            this.byMonth = new ArrayList<>( source.byMonth );
            this.cycleLength = source.cycleLength;
            this.workDays = source.workDays;
            this.restDays = source.restDays;
            this.active = source.active;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;

            return copyLocalizableFrom(source);
        }

        @NonNull
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public Builder frequency(@NonNull Frequency frequency) {
            this.frequency = frequency;
            return this;
        }

        @NonNull
        public Builder interval(int interval) {
            this.interval = interval;
            return this;
        }

        @NonNull
        public Builder startDate(@NonNull LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        @NonNull
        public Builder endType(@NonNull EndType endType) {
            this.endType = endType;
            return this;
        }

        @NonNull
        public Builder endDate(@Nullable LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        @NonNull
        public Builder count(@Nullable Integer count) {
            this.count = count;
            return this;
        }

        @NonNull
        public Builder byDay(@NonNull DayOfWeek... days) {
            this.byDay = new ArrayList<>( Arrays.asList( days ) );
            return this;
        }

        @NonNull
        public Builder byDay(@NonNull List<DayOfWeek> days) {
            this.byDay = new ArrayList<>( days );
            return this;
        }

        @NonNull
        public Builder weekStart(@NonNull WeekStart weekStart) {
            this.weekStart = weekStart;
            return this;
        }

        @NonNull
        public Builder byMonthDay(@NonNull Integer... days) {
            this.byMonthDay = new ArrayList<>( Arrays.asList( days ) );
            return this;
        }

        @NonNull
        public Builder byMonth(@NonNull Integer... months) {
            this.byMonth = new ArrayList<>( Arrays.asList( months ) );
            return this;
        }

        @NonNull
        public Builder cycleLength(@Nullable Integer cycleLength) {
            this.cycleLength = cycleLength;
            return this;
        }

        @NonNull
        public Builder workDays(@Nullable Integer workDays) {
            this.workDays = workDays;
            return this;
        }

        @NonNull
        public Builder restDays(@Nullable Integer restDays) {
            this.restDays = restDays;
            return this;
        }

        @NonNull
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        @NonNull
        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NonNull
        public Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        @Override
        @NonNull
        public RecurrenceRule build() {
            return new RecurrenceRule(this);
        }
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurrenceRule that = (RecurrenceRule) o;
        return Objects.equals( id, that.id );
    }

    @Override
    public int hashCode() {
        return Objects.hash( id );
    }

    @Override
    @NonNull
    public String toString() {
        return "RecurrenceRule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", frequency=" + frequency +
                ", interval=" + interval +
                ", startDate=" + startDate +
                ", endType=" + endType +
                '}';
    }
}