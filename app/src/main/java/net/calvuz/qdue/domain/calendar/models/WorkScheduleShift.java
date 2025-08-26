package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * WorkScheduleShift - Domain model representing a work shift within a day.
 *
 * <p>This is a clean architecture domain model representing a single work shift
 * independent of external dependencies. Contains shift timing information
 * and team assignments for the shift period using domain Shift for type definitions
 * with full localization support for calendar display and user interaction.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Shift Timing</strong>: Start and end times for the shift</li>
 *   <li><strong>Team Management</strong>: Teams assigned to work this shift</li>
 *   <li><strong>Shift Templates</strong>: Uses domain Shift for categorization</li>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state</li>
 *   <li><strong>Builder Pattern</strong>: Fluent API for construction</li>
 *   <li><strong>Localization Support</strong>: Full i18n support for all display text</li>
 *   <li><strong>Clean Architecture</strong>: No external dependencies</li>
 * </ul>
 *
 * <h3>Shift Types:</h3>
 * <ul>
 *   <li><strong>MORNING</strong>: Early shift (typically 06:00-14:00)</li>
 *   <li><strong>AFTERNOON</strong>: Middle shift (typically 14:00-22:00)</li>
 *   <li><strong>NIGHT</strong>: Night shift (typically 22:00-06:00)</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // Create morning shift with teams and localization
 * WorkScheduleShift morningShift = WorkScheduleShift.builder()
 *     .shift(Shift.createMorningShift(localizer))
 *     .startTime(LocalTime.of(6, 0))
 *     .endTime(LocalTime.of(14, 0))
 *     .addTeam(teamA)
 *     .addTeam(teamB)
 *     .description("Morning shift with teams A and B")
 *     .localizer(domainLocalizer)
 *     .build();
 *
 * // Get localized display
 * String localizedSummary = morningShift.getLocalizedSummary();
 * String teamStatus = morningShift.getLocalizedTeamStatus();
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation with Localization
 * @since Database Version 6
 */
public class WorkScheduleShift extends LocalizableDomainModel implements Cloneable {

    private static final String LOCALIZATION_SCOPE = "work_shift";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ==================== CORE DATA ====================

    private final Shift shift;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<Team> teams;
    private final String description;

    // ==================== CACHED VALUES ====================

    private final boolean crossesMidnight;
    private final Duration duration;

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a work schedule shift with basic information.
     *
     * @param shift Template shift definition
     * @param startTime Start time of the shift
     * @param endTime End time of the shift
     */
    public WorkScheduleShift(@NonNull Shift shift,
                             @NonNull LocalTime startTime,
                             @NonNull LocalTime endTime) {
        this(shift, startTime, endTime, new ArrayList<>(), null, null);
    }

    /**
     * Creates a work schedule shift with complete information.
     *
     * @param shift Template shift definition
     * @param startTime Start time of the shift
     * @param endTime End time of the shift
     * @param teams Teams assigned to this shift
     * @param description Optional description
     */
    public WorkScheduleShift(@NonNull Shift shift,
                             @NonNull LocalTime startTime,
                             @NonNull LocalTime endTime,
                             @NonNull List<Team> teams,
                             @Nullable String description) {
        this(shift, startTime, endTime, teams, description, null);
    }

    /**
     * Creates a work schedule shift with complete information and localization support.
     *
     * @param shift Template shift definition
     * @param startTime Start time of the shift
     * @param endTime End time of the shift
     * @param teams Teams assigned to this shift
     * @param description Optional description
     * @param localizer Optional domain localizer for i18n
     */
    public WorkScheduleShift(@NonNull Shift shift,
                             @NonNull LocalTime startTime,
                             @NonNull LocalTime endTime,
                             @NonNull List<Team> teams,
                             @Nullable String description,
                             @Nullable DomainLocalizer localizer) {
        super(localizer, LOCALIZATION_SCOPE);

        this.shift = Objects.requireNonNull(shift, "Shift cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.teams = new ArrayList<>(Objects.requireNonNull(teams, "Teams cannot be null"));
        this.description = description != null ? description : "";

        // Calculate cached values
        this.crossesMidnight = endTime.isBefore(startTime);
        this.duration = calculateDuration();
    }

    /**
     * Private constructor for builder pattern.
     */
    private WorkScheduleShift(@NonNull Builder builder) {
        super(builder.mLocalizer, LOCALIZATION_SCOPE);

        this.shift = Objects.requireNonNull(builder.shift, "Shift cannot be null");
        this.startTime = Objects.requireNonNull(builder.startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(builder.endTime, "End time cannot be null");
        this.teams = new ArrayList<>(builder.teams);
        this.description = builder.description != null ? builder.description : "";

        // Calculate cached values
        this.crossesMidnight = endTime.isBefore(startTime);
        this.duration = calculateDuration();
    }

    // ==================== GETTERS ====================

    /**
     * Get the shift template definition.
     *
     * @return Shift template
     */
    @NonNull
    public Shift getShift() {
        return shift;
    }

    /**
     * Get the start time of this shift instance.
     *
     * @return LocalTime representing shift start
     */
    @NonNull
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Get the end time of this shift instance.
     *
     * @return LocalTime representing shift end
     */
    @NonNull
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Get teams assigned to work this shift.
     *
     * @return Immutable list of teams
     */
    @NonNull
    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * Get the shift description.
     *
     * @return Description or empty string
     */
    @NonNull
    public String getDescription() {
        return description;
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
     * Get the duration of this shift.
     *
     * @return Duration of the shift
     */
    @NonNull
    public Duration getDuration() {
        return duration;
    }

    /**
     * Get duration in minutes.
     *
     * @return Duration in minutes
     */
    public long getDurationMinutes() {
        return duration.toMinutes();
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this shift has any assigned teams.
     *
     * @return true if teams are assigned
     */
    public boolean hasTeams() {
        return !teams.isEmpty();
    }

    /**
     * Check if a specific team is assigned to this shift.
     *
     * @param team Team to check
     * @return true if team is assigned
     */
    public boolean hasTeam(@NonNull Team team) {
        Objects.requireNonNull(team, "Team cannot be null");
        return teams.contains(team);
    }

    /**
     * Check if a team with specific name is assigned to this shift.
     *
     * @param teamName Team name to check
     * @return true if team with that name is assigned
     */
    public boolean hasTeamWithId(@NonNull String teamId) {
        Objects.requireNonNull(teamId, "Team name cannot be null");
        return teams.stream()
                .anyMatch(team -> team.hasId(teamId));
    }

    /**
     * Get the number of teams assigned to this shift.
     *
     * @return Number of teams
     */
    public int getTeamCount() {
        return teams.size();
    }

    /**
     * Get team at specific index.
     *
     * @param index Team index (0-based)
     * @return Team at the specified index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @NonNull
    public Team getTeam(int index) {
        return teams.get(index);
    }

    /**
     * Check if a given time falls within this shift.
     *
     * @param time Time to check
     * @return true if time is within shift period
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
     * Check if this shift overlaps with another shift.
     *
     * @param other Other shift to check
     * @return true if shifts overlap in time
     */
    public boolean overlapsWith(@NonNull WorkScheduleShift other) {
        Objects.requireNonNull(other, "Other shift cannot be null");

        return includesTime(other.startTime) ||
                includesTime(other.endTime) ||
                other.includesTime(this.startTime) ||
                other.includesTime(this.endTime);
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        return startTime.format(TIME_FORMATTER) + "-" + endTime.format(TIME_FORMATTER);
    }

    /**
     * Get teams as concatenated string.
     *
     * @return String with all team names
     */
    @NonNull
    public String getTeamsAsString() {
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.joining(""));
    }

    /**
     * Get teams as comma-separated string.
     *
     * @return String with team names separated by commas
     */
    @NonNull
    public String getTeamsAsCommaSeparatedString() {
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Get display name for this shift.
     *
     * @return Display name combining shift name and time range
     */
    @NonNull
    public String getDisplayName() {
        return shift.getName() + " (" + getTimeRange() + ")";
    }

    /**
     * Get summary information for display.
     *
     * @return Summary including shift name, time, and teams
     */
    @NonNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(getDisplayName());

        if (hasTeams()) {
            summary.append(" - Teams: ").append(getTeamsAsCommaSeparatedString());
        }

        if (!description.isEmpty()) {
            summary.append(" - ").append(description);
        }

        return summary.toString();
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized time range display.
     */
    @NonNull
    public String getLocalizedTimeRange() {
        return localize("time.range_format", getTimeRange(), getTimeRange());
    }

    /**
     * Get localized team count description.
     */
    @NonNull
    public String getLocalizedTeamCount() {
        if (teams.isEmpty()) {
            return localize("teams.none", "No teams assigned", "No teams assigned");
        } else if (teams.size() == 1) {
            return localize("teams.single", "1 team", "1 team");
        } else {
            return localize("teams.multiple", "{0} teams", "{0} teams", teams.size());
        }
    }

    /**
     * Get localized team status summary.
     */
    @NonNull
    public String getLocalizedTeamStatus() {
        if (teams.isEmpty()) {
            return localize("teams.none_assigned", "No teams assigned", "No teams assigned");
        }

        String teamsLabel = localize("teams.assigned", "Teams", "Teams");
        return teamsLabel + ": " + getTeamsAsCommaSeparatedString();
    }

    /**
     * Get localized shift summary.
     */
    @NonNull
    public String getLocalizedSummary() {
        StringBuilder summary = new StringBuilder();

        // Shift type display name
        if (shift != null) {
            summary.append(shift.getDisplayTitle());
        } else {
            summary.append(localize("shift.unknown", "Unknown shift", "Unknown shift"));
        }

        // Time range
        String timeLabel = localize("time.period", " ({0})", " ({0})");
        summary.append(String.format(timeLabel.replace("{0}", "%s"), getLocalizedTimeRange()));

        // Team information
        if (hasTeams()) {
            String separator = localize("format.summary_separator", " - ", " - ");
            summary.append(separator).append(getLocalizedTeamStatus());
        }

        // Duration if significant
        long durationMinutes = getDurationMinutes();
        if (durationMinutes > 0) {
            String durationLabel = localize("duration.minutes", " [{0} min]", " [{0} min]");
            summary.append(String.format(durationLabel.replace("{0}", "%d"), durationMinutes));
        }

        return summary.toString();
    }

    /**
     * Get localized detailed information.
     */
    @NonNull
    public String getLocalizedDetailedSummary() {
        StringBuilder summary = new StringBuilder();

        // Basic shift information
        summary.append(getLocalizedSummary()).append("\n");

        // Description if available
        if (!description.isEmpty()) {
            String descLabel = localize("description.label", "Description", "Description");
            summary.append(descLabel).append(": ").append(description).append("\n");
        }

        // Timing details
        String timingLabel = localize("timing.details", "Timing details", "Timing details");
        summary.append(timingLabel).append(":\n");

        String startLabel = localize("time.start", "Start", "Start");
        String endLabel = localize("time.end", "End", "End");
        String durationLabel = localize("duration.label", "Duration", "Duration");

        summary.append("  ").append(startLabel).append(": ").append(startTime).append("\n");
        summary.append("  ").append(endLabel).append(": ").append(endTime).append("\n");
        summary.append("  ").append(durationLabel).append(": ").append(getDurationMinutes()).append(" ");
        summary.append(localize("time.minutes", "minutes", "minutes")).append("\n");

        if (crossesMidnight) {
            String midnightNote = localize("time.crosses_midnight_note",
                    "Note: This shift crosses midnight",
                    "Note: This shift crosses midnight");
            summary.append("  ").append(midnightNote).append("\n");
        }

        return summary.toString().trim();
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a morning shift with default timing.
     *
     * @return Morning WorkScheduleShift
     */
    @NonNull
    public static WorkScheduleShift createMorningShift() {
        return builder()
                .shift(Shift.createMorningShift(null))
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(14, 0))
                .build();
    }

    /**
     * Create an afternoon shift with default timing.
     *
     * @return Afternoon WorkScheduleShift
     */
    @NonNull
    public static WorkScheduleShift createAfternoonShift() {
        return builder()
                .shift(Shift.createAfternoonShift(null))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(22, 0))
                .build();
    }

    /**
     * Create a night shift with default timing.
     *
     * @return Night WorkScheduleShift
     */
    @NonNull
    public static WorkScheduleShift createNightShift() {
        return builder()
                .shift(Shift.createNightShift(null))
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
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
    public WorkScheduleShift withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder()
                .copyFrom(this)
                .localizer(localizer)
                .build();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing WorkScheduleShift instances.
     *
     * @return Builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder from existing WorkScheduleShift.
     *
     * @return Builder instance with current shift data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class for creating WorkScheduleShift instances.
     */
    public static class Builder extends LocalizableBuilder<WorkScheduleShift, Builder> {
        private Shift shift;
        private LocalTime startTime;
        private LocalTime endTime;
        private final List<Team> teams = new ArrayList<>();
        private String description;

        public Builder() {
            // Empty constructor for fresh builds
        }

        private Builder(@NonNull WorkScheduleShift existingShift) {
            this.shift = existingShift.shift;
            this.startTime = existingShift.startTime;
            this.endTime = existingShift.endTime;
            this.teams.addAll(existingShift.teams);
            this.description = existingShift.description;
        }

        /**
         * Copy data from existing WorkScheduleShift.
         */
        @NonNull
        public Builder copyFrom(@NonNull WorkScheduleShift source) {
            this.shift = source.shift;
            this.startTime = source.startTime;
            this.endTime = source.endTime;
            this.teams.clear();
            this.teams.addAll(source.teams);
            this.description = source.description;
            return copyLocalizableFrom(source);
        }

        /**
         * Clear all teams.
         */
        @NonNull
        public Builder clearTeams() {
            this.teams.clear();
            return this;
        }

        /**
         * Set shift template.
         *
         * @param shift Shift template definition
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder shift(@NonNull Shift shift) {
            this.shift = Objects.requireNonNull(shift, "Shift cannot be null");
            return this;
        }

        /**
         * Set start time.
         *
         * @param startTime Start time for the shift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder startTime(@NonNull LocalTime startTime) {
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
        public Builder startTime(int hour, int minute) {
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
        public Builder endTime(@NonNull LocalTime endTime) {
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
        public Builder endTime(int hour, int minute) {
            this.endTime = LocalTime.of(hour, minute);
            return this;
        }

        /**
         * Add a team to this shift.
         *
         * @param team Team to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeam(@NonNull Team team) {
            this.teams.add(Objects.requireNonNull(team, "Team cannot be null"));
            return this;
        }

        /**
         * Add a team to this shift by name.
         *
         * @param teamName Team name to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeam(@NonNull String teamName) {
            Objects.requireNonNull(teamName, "Team name cannot be null");
            this.teams.add(Team.fromName(teamName));
            return this;
        }

        /**
         * Add multiple teams to this shift.
         *
         * @param teams Teams to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeams(@NonNull List<Team> teams) {
            Objects.requireNonNull(teams, "Teams cannot be null");
            this.teams.addAll(teams);
            return this;
        }

        /**
         * Add multiple teams to this shift by names.
         *
         * @param teamNames Team names to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeamNames(@NonNull List<String> teamNames) {
            Objects.requireNonNull(teamNames, "Team names cannot be null");
            teamNames.forEach(name -> {
                if (name != null && !name.trim().isEmpty()) {
                    this.teams.add(Team.fromName(name.trim()));
                }
            });
            return this;
        }

        /**
         * Set shift description.
         *
         * @param description Shift description
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        /**
         * Build the WorkScheduleShift instance.
         *
         * @return New WorkScheduleShift instance
         * @throws IllegalStateException if required fields are not set
         */
        @Override
        @NonNull
        public WorkScheduleShift build() {
            if (shift == null) {
                throw new IllegalStateException("Shift template must be set");
            }
            if (startTime == null) {
                throw new IllegalStateException("Start time must be set");
            }
            if (endTime == null) {
                throw new IllegalStateException("End time must be set");
            }

            return new WorkScheduleShift(this);
        }
    }

    // ==================== CLONEABLE SUPPORT ====================

    /**
     * Create a deep clone of this WorkScheduleShift.
     * Useful for template-based shift generation.
     *
     * @return Cloned WorkScheduleShift instance
     */
    @Override
    @NonNull
    public WorkScheduleShift clone() {
        try {
            // Clone teams (Team objects are immutable, so shallow copy is fine)
            List<Team> clonedTeams = new ArrayList<>(teams);

            return new WorkScheduleShift(shift, startTime, endTime, clonedTeams, description, getLocalizer());

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone WorkScheduleShift", e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate duration from start to end time.
     *
     * @return Duration of the shift
     */
    @NonNull
    private Duration calculateDuration() {
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

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkScheduleShift that = (WorkScheduleShift) o;
        return Objects.equals(shift, that.shift) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(teams, that.teams) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shift, startTime, endTime, teams, description);
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleShift{" +
                "shift=" + shift.getName() +
                ", timeRange=" + getTimeRange() +
                ", teams=" + getTeamsAsString() +
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
        return "WorkScheduleShift{" +
                "shift=" + shift.toDetailedString() +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", crossesMidnight=" + crossesMidnight +
                ", duration=" + duration +
                ", teams=" + teams +
                ", description='" + description + '\'' +
                ", hasLocalizationSupport=" + hasLocalizationSupport() +
                '}';
    }
}