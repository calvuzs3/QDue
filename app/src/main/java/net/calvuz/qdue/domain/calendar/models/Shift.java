package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Shift - Domain model representing a shift type with schedule, duration and display properties.
 *
 * <p>This is a clean architecture domain model representing shift type definitions independent
 * of external frameworks. Defines templates for shifts including timing, duration, breaks,
 * and visual appearance properties for calendar integration with full localization support.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe value object pattern</li>
 *   <li><strong>UUID Identification</strong>: Unique identifier for global tracking</li>
 *   <li><strong>Timing Management</strong>: Start/end times with midnight crossing support</li>
 *   <li><strong>Break Management</strong>: Optional break time with inclusion flexibility</li>
 *   <li><strong>Visual Properties</strong>: Color management for calendar display</li>
 *   <li><strong>Localization Support</strong>: Full i18n support for all display text</li>
 *   <li><strong>Domain-Driven</strong>: No external dependencies, pure business model</li>
 * </ul>
 *
 * <h3>Shift Types:</h3>
 * <ul>
 *   <li><strong>CYCLE_42</strong>: QuattroDue 4-2 cycle shift</li>
 *   <li><strong>DAILY</strong>: Standard daily shift</li>
 *   <li><strong>MORNING</strong>: Morning shift (typically 06:00-14:00)</li>
 *   <li><strong>AFTERNOON</strong>: Afternoon shift (typically 14:00-22:00)</li>
 *   <li><strong>NIGHT</strong>: Night shift (typically 22:00-06:00)</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // Create localized morning shift
 * Shift morningShift = Shift.builder("Morning Shift")
 *     .setStartTime(LocalTime.of(6, 0))
 *     .setEndTime(LocalTime.of(14, 0))
 *     .setShiftType(ShiftType.MORNING)
 *     .setColorHex("#4CAF50")
 *     .setHasBreakTime(true)
 *     .setBreakTimeDuration(Duration.ofMinutes(30))
 *     .localizer(domainLocalizer)
 *     .build();
 *
 * // Get localized display
 * String localizedName = morningShift.getTypeDisplayName();
 * String localizedSummary = morningShift.getLocalizedSummary();
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Localization Implementation
 * @since Clean Architecture Phase 2
 */
public class Shift extends LocalizableDomainModel {

    public static final String TAG = Shift.class.getSimpleName();

    private static final String LOCALIZATION_SCOPE = "shift";

    // ==================== SHIFT TYPE ENUM ====================

    /**
     * Enumeration of shift types in the work schedule system with localization keys.
     */
    public enum ShiftType {
        CYCLE_42("cycle_42", "c42"),
        DAILY("daily", "d"),
        MORNING("morning", "m"),
        AFTERNOON("afternoon", "a"),
        NIGHT("night", "n");

        private final String displayNameKey;
        private final String shortCodeKey;

        ShiftType(String displayNameKey, String shortCodeKey) {
            this.displayNameKey = displayNameKey;
            this.shortCodeKey = shortCodeKey;
        }

        /**
         * Get localization key for display name.
         *
         * @return Localization key for display name
         */
        @NonNull
        public String getDisplayNameKey() {
            return displayNameKey;
        }

        /**
         * Get localization key for short code.
         *
         * @return Localization key for short code
         */
        @NonNull
        public String getShortCodeKey() {
            return shortCodeKey;
        }
    }

    // ==================== CORE PROPERTIES ====================

    private final String id;                    // Unique identifier (UUID)
    private final String name;                  // Shift name (e.g., "Morning")
    private final String description;           // Description (e.g., "Morning shift 6-14")
    private final ShiftType shiftType;          // Type classification
    private final LocalTime startTime;          // Start time
    private final LocalTime endTime;            // End time
    private final boolean crossesMidnight;      // True if shift crosses midnight
    private final String colorHex;              // Associated color (hex format)
    private final boolean isUserRelevant;       // True if relevant to current user
    private final boolean hasBreakTime;         // True if shift includes break
    private final boolean isBreakTimeIncluded;  // True if break is included in work time
    private final Duration breakTimeDuration;   // Duration of break time

    // ==================== CACHED VALUES ====================

    private final int hashCodeCache;            // Cached hash code for performance
    private final Duration totalDuration;       // Total shift duration (cached)
    private final Duration workDuration;        // Actual work duration (cached)

    // ==================== CONSTRUCTORS ====================

    /**
     * Private constructor for builder pattern with localization support.
     */
    private Shift(@NonNull Builder builder) {
        super(builder.mLocalizer, LOCALIZATION_SCOPE);

        // Validation
        Objects.requireNonNull(builder.name, "Shift name cannot be null");
        Objects.requireNonNull(builder.startTime, "Start time cannot be null");
        Objects.requireNonNull(builder.endTime, "End time cannot be null");

        if (builder.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Shift name cannot be empty");
        }

        // Core properties
        this.id = (builder.id != null && !builder.id.trim().isEmpty()) ? builder.id.trim() : generateUUID();
        this.name = builder.name.trim();
        this.description = builder.description != null ? builder.description.trim() : "";
        this.shiftType = builder.shiftType;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.colorHex = builder.colorHex != null ? builder.colorHex.trim() : "#2196F3"; // Default blue
        this.isUserRelevant = builder.isUserRelevant;
        this.hasBreakTime = builder.hasBreakTime;
        this.isBreakTimeIncluded = builder.isBreakTimeIncluded;
        this.breakTimeDuration = builder.breakTimeDuration != null ? builder.breakTimeDuration : Duration.ZERO;

        // Calculate derived properties
        this.crossesMidnight = endTime.isBefore(startTime);
        this.totalDuration = calculateTotalDuration();
        this.workDuration = calculateWorkDuration();

        // Cache hash code
        this.hashCodeCache = Objects.hash(this.id);
    }

    // ==================== GETTERS ====================

    /**
     * Get the unique shift identifier.
     *
     * @return Shift ID (UUID)
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the shift name.
     *
     * @return Shift name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Get the shift description.
     *
     * @return Shift description or empty string
     */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Get the shift type classification.
     *
     * @return ShiftType or null if not classified
     */
    @Nullable
    public ShiftType getShiftType() {
        return shiftType;
    }

    /**
     * Get the start time of the shift.
     *
     * @return LocalTime representing shift start
     */
    @NonNull
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Get the end time of the shift.
     *
     * @return LocalTime representing shift end
     */
    @NonNull
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Check if shift crosses midnight.
     *
     * @return true if end time is before start time
     */
    public boolean crossesMidnight() {
        return crossesMidnight;
    }

    /**
     * Get the color in hex format.
     *
     * @return Color hex string (e.g., "#FF5722")
     */
    @NonNull
    public String getColorHex() {
        return colorHex;
    }

    /**
     * Check if shift is relevant to current user.
     *
     * @return true if user-relevant
     */
    public boolean isUserRelevant() {
        return isUserRelevant;
    }

    /**
     * Check if shift includes break time.
     *
     * @return true if break time is included
     */
    public boolean hasBreakTime() {
        return hasBreakTime;
    }

    /**
     * Check if break time is included in total work duration.
     *
     * @return true if break is included in work time calculation
     */
    public boolean isBreakTimeIncluded() {
        return isBreakTimeIncluded;
    }

    /**
     * Get the duration of break time.
     *
     * @return Break duration (Duration.ZERO if no break)
     */
    @NonNull
    public Duration getBreakTimeDuration() {
        return breakTimeDuration;
    }

    /**
     * Get total shift duration (including break if any).
     *
     * @return Total duration from start to end
     */
    @NonNull
    public Duration getTotalDuration() {
        return totalDuration;
    }

    /**
     * Get actual work duration (excluding break if not included).
     *
     * @return Effective work duration
     */
    @NonNull
    public Duration getWorkDuration() {
        return workDuration;
    }

    // ==================== COMPUTED PROPERTIES ====================

    /**
     * Get total duration in minutes.
     *
     * @return Total minutes from start to end
     */
    public long getTotalDurationMinutes() {
        return totalDuration.toMinutes();
    }

    /**
     * Get work duration in minutes.
     *
     * @return Work minutes (excluding break if not included)
     */
    public long getWorkDurationMinutes() {
        return workDuration.toMinutes();
    }

    /**
     * Get break duration in minutes.
     *
     * @return Break minutes
     */
    public long getBreakDurationMinutes() {
        return breakTimeDuration.toMinutes();
    }

    /**
     * Get start hour (0-23).
     *
     * @return Start hour
     */
    public int getStartHour() {
        return startTime.getHour();
    }

    /**
     * Get start minute (0-59).
     *
     * @return Start minute
     */
    public int getStartMinute() {
        return startTime.getMinute();
    }

    /**
     * Get end hour (0-23).
     *
     * @return End hour
     */
    public int getEndHour() {
        return endTime.getHour();
    }

    /**
     * Get end minute (0-59).
     *
     * @return End minute
     */
    public int getEndMinute() {
        return endTime.getMinute();
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.format(formatter) + "-" + endTime.format(formatter);
    }

    /**
     * Get formatted start time.
     *
     * @return Formatted start time (e.g., "06:00")
     */
    @NonNull
    public String getFormattedStartTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.format(formatter);
    }

    /**
     * Get formatted end time.
     *
     * @return Formatted end time (e.g., "14:00")
     */
    @NonNull
    public String getFormattedEndTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return endTime.format(formatter);
    }

    /**
     * Get short name for compact display.
     *
     * @return First character of name in uppercase
     */
    @NonNull
    public String getShortName() {
        return name.substring(0, 1).toUpperCase();
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized shift type display name.
     */
    @NonNull
    public String getTypeDisplayName() {
        if (shiftType != null) {
            return localize("type." + shiftType.name().toLowerCase(),
                    shiftType.name(), // fallback
                    shiftType.name());
        }
        return localize("type.custom", name, name);
    }

    /**
     * Get localized shift type short code.
     */
    @NonNull
    public String getTypeShortCode() {
        if (shiftType != null) {
            return localize("code." + shiftType.name().toLowerCase(),
                    shiftType.name().substring(0, 1), // fallback to first letter
                    shiftType.name().substring(0, 1));
        }
        return getShortName();
    }

    /**
     * Get display title for UI with localization support.
     */
    @NonNull
    public String getDisplayTitle() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return getTypeDisplayName();
    }

    /**
     * Get localized display name for UI presentation.
     * Uses shift type localization if available, falls back to name.
     *
     * @return Localized display name with time range
     */
    @NonNull
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();

        if (shiftType != null) {
            sb.append(getTypeDisplayName());
        } else {
            sb.append(name);
        }

        sb.append(" (").append(getTimeRange()).append(")");

        return sb.toString();
    }

    /**
     * Get full name with description if available.
     *
     * @return Full shift description with localization
     */
    @NonNull
    public String getFullName() {
        String displayName = getDisplayName();

        if (description.isEmpty()) {
            return displayName;
        }

        // Localize common descriptions or use as-is
        String localizedDescription = localize("description.custom", description, description);
        return displayName + " - " + localizedDescription;
    }

    /**
     * Get localized summary information about the shift.
     *
     * @return Localized shift summary with timing and break info
     */
    @NonNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        // Use localized display name
        summary.append(getDisplayName());

        if (hasBreakTime) {
            String breakLabel = localize("break.label", "break", "break");
            String minutesLabel = localize("time.minutes_short", "min", "min");

            summary.append(" (").append(breakLabel).append(": ")
                    .append(getBreakDurationMinutes()).append(minutesLabel);

            if (!isBreakTimeIncluded) {
                String unpaidLabel = localize("break.unpaid", "unpaid", "unpaid");
                summary.append(" - ").append(unpaidLabel);
            }
            summary.append(")");
        }

        if (crossesMidnight) {
            String midnightLabel = localize("time.crosses_midnight", "crosses midnight", "crosses midnight");
            summary.append(" [").append(midnightLabel).append("]");
        }

        return summary.toString();
    }

    /**
     * Get localized break information.
     */
    @NonNull
    public String getBreakInfo() {
        if (!hasBreakTime) {
            return localize("break.none", "No break", "No break");
        }

        String minutesLabel = localize("time.minutes", "minutes", "minutes");
        StringBuilder info = new StringBuilder()
                .append(getBreakDurationMinutes())
                .append(" ")
                .append(minutesLabel);

        if (!isBreakTimeIncluded) {
            String unpaidLabel = localize("break.unpaid", "unpaid", "unpaid");
            info.append(" (").append(unpaidLabel).append(")");
        }

        return info.toString();
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if a given time is within the shift period.
     *
     * @param time Time to check
     * @return true if time falls within shift
     */
    public boolean includesTime(@NonNull LocalTime time) {
        Objects.requireNonNull(time, "Time cannot be null");

        if (crossesMidnight) {
            // For shifts crossing midnight (e.g., 22:00-06:00)
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        } else {
            // For normal shifts within same day
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        }
    }

    /**
     * Check if this shift has the same schedule as another.
     *
     * @param other Other shift to compare
     * @return true if schedules match (ignoring other properties)
     */
    public boolean hasSameSchedule(@NonNull Shift other) {
        Objects.requireNonNull(other, "Other shift cannot be null");

        return Objects.equals(this.startTime, other.startTime) &&
                Objects.equals(this.endTime, other.endTime);
    }

    /**
     * Check if this shift overlaps with another shift.
     *
     * @param other Other shift to check
     * @return true if shifts overlap in time
     */
    public boolean overlapsWith(@NonNull Shift other) {
        Objects.requireNonNull(other, "Other shift cannot be null");

        // Handle midnight crossing for both shifts
        // This is a simplified overlap check - can be enhanced for complex cases
        return includesTime(other.startTime) ||
                includesTime(other.endTime) ||
                other.includesTime(this.startTime) ||
                other.includesTime(this.endTime);
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a morning shift with localization support.
     *
     * @param localizer Optional domain localizer for i18n
     * @return Morning shift (05:00-13:00)
     */
    @NonNull
    public static Shift createMorningShift(@Nullable DomainLocalizer localizer) {
        return builder("Morning Shift")
                .setShiftType(ShiftType.MORNING)
                .setStartTime(5, 0)
                .setEndTime(13, 0)
                .setColorHex("#4CAF50")
                .localizer(localizer)
                .build();
    }

    /**
     * Create an afternoon shift with localization support.
     *
     * @param localizer Optional domain localizer for i18n
     * @return Afternoon shift (13:00-21:00)
     */
    @NonNull
    public static Shift createAfternoonShift(@Nullable DomainLocalizer localizer) {
        return builder("Afternoon Shift")
                .setShiftType(ShiftType.AFTERNOON)
                .setStartTime(13, 0)
                .setEndTime(21, 0)
                .setColorHex("#FF9800")
                .localizer(localizer)
                .build();
    }

    /**
     * Create a night shift with localization support.
     *
     * @param localizer Optional domain localizer for i18n
     * @return Night shift (21:00-05:00)
     */
    @NonNull
    public static Shift createNightShift(@Nullable DomainLocalizer localizer) {
        return builder("Night Shift")
                .setShiftType(ShiftType.NIGHT)
                .setStartTime(21, 0)
                .setEndTime(5, 0)
                .setColorHex("#3F51B5")
                .localizer(localizer)
                .build();
    }

    /**
     * Create a QuattroDue cycle shift with localization support.
     *
     * @param localizer Optional domain localizer for i18n
     * @return QuattroDue cycle shift
     */
    @NonNull
    public static Shift createQuattroDueCycleShift(@Nullable DomainLocalizer localizer) {
        return builder("QuattroDue Cycle")
                .setShiftType(ShiftType.CYCLE_42)
                .setStartTime(6, 0)
                .setEndTime(18, 0)
                .setColorHex("#9C27B0")
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
    public Shift withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder(this.name)
                .copyFrom(this)
                .localizer(localizer)
                .build();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing Shift instances.
     *
     * @param name Shift name (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull String name) {
        return new Builder(name);
    }

    /**
     * Create a builder from existing Shift.
     *
     * @return Builder instance with current shift data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class for creating Shift instances with localization support.
     */
    public static class Builder extends LocalizableBuilder<Shift, Builder> {
        private String id;
        private final String name;
        private String description = "";
        private ShiftType shiftType;
        private LocalTime startTime;
        private LocalTime endTime;
        private String colorHex = "#2196F3";
        private boolean isUserRelevant = false;
        private boolean hasBreakTime = false;
        private boolean isBreakTimeIncluded = false;
        private Duration breakTimeDuration = Duration.ZERO;

        public Builder(@NonNull String name) {
            this.name = Objects.requireNonNull(name, "Name cannot be null");
        }

        private Builder(@NonNull Shift existingShift) {
            this.id = existingShift.id;
            this.name = existingShift.name;
            this.description = existingShift.description;
            this.shiftType = existingShift.shiftType;
            this.startTime = existingShift.startTime;
            this.endTime = existingShift.endTime;
            this.colorHex = existingShift.colorHex;
            this.isUserRelevant = existingShift.isUserRelevant;
            this.hasBreakTime = existingShift.hasBreakTime;
            this.isBreakTimeIncluded = existingShift.isBreakTimeIncluded;
            this.breakTimeDuration = existingShift.breakTimeDuration;
        }

        /**
         * Copy data from existing Shift (for withLocalizer implementation).
         */
        @NonNull
        public Builder copyFrom(@NonNull Shift source) {
            this.id = source.id;
            this.description = source.description;
            this.shiftType = source.shiftType;
            this.startTime = source.startTime;
            this.endTime = source.endTime;
            this.colorHex = source.colorHex;
            this.isUserRelevant = source.isUserRelevant;
            this.hasBreakTime = source.hasBreakTime;
            this.isBreakTimeIncluded = source.isBreakTimeIncluded;
            this.breakTimeDuration = source.breakTimeDuration;

            return copyLocalizableFrom(source);
        }

        /**
         * Set shift ID.
         *
         * @param id Unique identifier
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setId(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * Set shift description.
         *
         * @param description Shift description
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            this.description = description != null ? description : "";
            return this;
        }

        /**
         * Set shift type classification.
         *
         * @param shiftType Type classification
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setShiftType(@Nullable ShiftType shiftType) {
            this.shiftType = shiftType;
            return this;
        }

        /**
         * Set start time.
         *
         * @param startTime Start time for the shift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setStartTime(@NonNull LocalTime startTime) {
            this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
            return this;
        }

        /**
         * Set start time using hour and minute.
         *
         * @param hour Start hour (0-23)
         * @param minute Start minute (0-59)
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setStartTime(int hour, int minute) {
            this.startTime = LocalTime.of(hour, minute);
            return this;
        }

        /**
         * Set end time.
         *
         * @param endTime End time for the shift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setEndTime(@NonNull LocalTime endTime) {
            this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
            return this;
        }

        /**
         * Set end time using hour and minute.
         *
         * @param hour End hour (0-23)
         * @param minute End minute (0-59)
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setEndTime(int hour, int minute) {
            this.endTime = LocalTime.of(hour, minute);
            return this;
        }

        /**
         * Set color in hex format.
         *
         * @param colorHex Color hex string (e.g., "#FF5722")
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setColorHex(@Nullable String colorHex) {
            this.colorHex = colorHex != null ? colorHex : "#2196F3";
            return this;
        }

        /**
         * Set user relevance flag.
         *
         * @param isUserRelevant Whether shift is relevant to current user
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setUserRelevant(boolean isUserRelevant) {
            this.isUserRelevant = isUserRelevant;
            return this;
        }

        /**
         * Set break time configuration.
         *
         * @param hasBreakTime Whether shift includes break
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setHasBreakTime(boolean hasBreakTime) {
            this.hasBreakTime = hasBreakTime;
            return this;
        }

        /**
         * Set break time inclusion in work duration.
         *
         * @param isBreakTimeIncluded Whether break is included in work time
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setBreakTimeIncluded(boolean isBreakTimeIncluded) {
            this.isBreakTimeIncluded = isBreakTimeIncluded;
            return this;
        }

        /**
         * Set break duration.
         *
         * @param breakTimeDuration Duration of break time
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setBreakTimeDuration(@Nullable Duration breakTimeDuration) {
            this.breakTimeDuration = breakTimeDuration != null ? breakTimeDuration : Duration.ZERO;
            return this;
        }

        /**
         * Set break duration in minutes.
         *
         * @param minutes Break duration in minutes
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setBreakTimeDuration(long minutes) {
            this.breakTimeDuration = Duration.ofMinutes(minutes);
            return this;
        }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        /**
         * Build the Shift instance.
         *
         * @return New Shift instance
         * @throws IllegalStateException if required fields are not set
         */
        @Override
        @NonNull
        public Shift build() {
            if (startTime == null) {
                throw new IllegalStateException("Start time must be set");
            }
            if (endTime == null) {
                throw new IllegalStateException("End time must be set");
            }

            return new Shift(this);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate a new UUID string.
     *
     * @return New UUID string
     */
    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Calculate total duration from start to end time.
     *
     * @return Total duration including midnight crossing
     */
    private Duration calculateTotalDuration() {
        if (crossesMidnight) {
            // For shifts crossing midnight (e.g., 22:00-06:00)
            Duration toMidnight = Duration.between(startTime, LocalTime.MIDNIGHT);
            Duration fromMidnight = Duration.between(LocalTime.MIDNIGHT, endTime);
            return toMidnight.plus(fromMidnight);
        } else {
            // For normal shifts within same day
            return Duration.between(startTime, endTime);
        }
    }

    /**
     * Calculate actual work duration.
     *
     * @return Work duration excluding break if not included
     */
    private Duration calculateWorkDuration() {
        Duration base = totalDuration;

        if (hasBreakTime && !isBreakTimeIncluded) {
            return base.minus(breakTimeDuration);
        }

        return base;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Shift shift = (Shift) obj;
        return Objects.equals(id, shift.id);
    }

    @Override
    public int hashCode() {
        return hashCodeCache;
    }

    @Override
    @NonNull
    public String toString() {
        return TAG + "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", time=" + getTimeRange() +
                ", type=" + (shiftType != null ? shiftType.name() : "null") +
                ", crossesMidnight=" + crossesMidnight +
                '}';
    }

    /**
     * Get detailed string representation for debugging.
     *
     * @return Detailed string with all shift information
     */
    @NonNull
    public String toDetailedString() {
        return TAG + "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", shiftType=" + shiftType +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", crossesMidnight=" + crossesMidnight +
                ", colorHex='" + colorHex + '\'' +
                ", isUserRelevant=" + isUserRelevant +
                ", hasBreakTime=" + hasBreakTime +
                ", isBreakTimeIncluded=" + isBreakTimeIncluded +
                ", breakTimeDuration=" + breakTimeDuration +
                ", totalDuration=" + totalDuration +
                ", workDuration=" + workDuration +
                ", hasLocalizationSupport=" + hasLocalizationSupport() +
                '}';
    }
}