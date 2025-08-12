package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * WorkScheduleShift - Domain model representing a work shift within a day.
 *
 * <p>This is a clean architecture domain model representing a single work shift
 * independent of external dependencies. Contains shift timing information
 * and team assignments for the shift period using domain Shift for type definitions.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Shift Timing</strong>: Start and end times for the shift</li>
 *   <li><strong>Team Management</strong>: Teams assigned to work this shift</li>
 *   <li><strong>Shift Types</strong>: Uses domain Shift.ShiftType for categorization</li>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state</li>
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
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation with Shift domain
 * @since Database Version 6
 */
public class WorkScheduleShift implements Cloneable {

    // ==================== CORE DATA ====================

    private final Shift shift;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<Team> teams;
    private final String description;

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a work schedule shift with basic information.
     *
     * @param shift Type of shift using domain Shift.ShiftType
     * @param startTime Start time of the shift
     * @param endTime End time of the shift
     */
    public WorkScheduleShift(@NonNull Shift shift,
                             @NonNull LocalTime startTime,
                             @NonNull LocalTime endTime) {
        this.shift = Objects.requireNonNull( shift, "Shift type cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.teams = new ArrayList<>();
        this.description = "";
    }

    /**
     * Creates a work schedule shift with complete information.
     *
     * @param shift Type of shift
     * @param startTime Start time of the shift
     * @param endTime End time of the shift
     * @param teams List of teams assigned to this shift
     * @param description Optional description of the shift
     */
    public WorkScheduleShift(@NonNull Shift shift,
                             @NonNull LocalTime startTime,
                             @NonNull LocalTime endTime,
                             @NonNull List<Team> teams,
                             @Nullable String description) {
        this.shift = Objects.requireNonNull( shift, "Shift type cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.teams = new ArrayList<>(Objects.requireNonNull(teams, "Teams cannot be null"));
        this.description = description != null ? description : "";
    }

    // ==================== GETTERS ====================

    /**
     * Get the shift type.
     *
     * @return Shift.ShiftType enum value
     */
    @NonNull
    public Shift getShift() {
        return shift;
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
     * Get teams assigned to this shift.
     *
     * @return Immutable list of teams
     */
    @NonNull
    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * Get team names assigned to this shift.
     *
     * @return Immutable list of team names
     */
    @NonNull
    public List<String> getTeamNames() {
        return teams.stream()
                .map(Team::getName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
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
     * Get the number of teams assigned to this shift.
     *
     * @return Number of teams
     */
    public int getTeamCount() {
        return teams.size();
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this shift has any teams assigned.
     *
     * @return true if shift has one or more teams
     */
    public boolean hasTeams() {
        return !teams.isEmpty();
    }

    /**
     * Check if a specific team is assigned to this shift.
     *
     * @param team Team to check
     * @return true if team is assigned to this shift
     */
    public boolean hasTeam(@NonNull Team team) {
        Objects.requireNonNull(team, "Team cannot be null");
        return teams.contains(team);
    }

    /**
     * Check if a specific team is assigned to this shift by name.
     *
     * @param teamName Name of the team to check
     * @return true if team is assigned to this shift
     */
    public boolean hasTeamWithName(@NonNull String teamName) {
        Objects.requireNonNull(teamName, "Team name cannot be null");
        return teams.stream()
                .anyMatch(team -> team.hasName(teamName));
    }

    /**
     * Check if this shift spans midnight (night shift).
     *
     * @return true if shift crosses midnight
     */
    public boolean spansMidnight() {
        return endTime.isBefore(startTime);
    }

    /**
     * Get shift duration in minutes.
     * Handles shifts that span midnight correctly.
     *
     * @return Duration in minutes
     */
    public long getDurationMinutes() {
        if (spansMidnight()) {
            // Night shift spanning midnight
            long minutesToMidnight = 24 * 60 - (startTime.getHour() * 60L + startTime.getMinute());
            long minutesFromMidnight = endTime.getHour() * 60L + endTime.getMinute();
            return minutesToMidnight + minutesFromMidnight;
        } else {
            // Normal shift within same day
            return (endTime.getHour() * 60L + endTime.getMinute()) -
                    (startTime.getHour() * 60L + startTime.getMinute());
        }
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        return String.format("%02d:%02d-%02d:%02d",
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute());
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
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Get team names as concatenated string (alias for compatibility).
     *
     * @return String with all team names
     */
    @NonNull
    public String getTeamNamesAsString() {
        return getTeamsAsString();
    }

    /**
     * Get teams as comma-separated string.
     *
     * @return String with team names separated by commas
     */
    @NonNull
    public String getTeamNamesCommaSeparated() {
        return teams.stream()
                .map(Team::getName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
                .stream()
                .reduce((a, b) -> a + ", " + b)
                .orElse("")
                .toString();
    }

    /**
     * Get shift display name for UI.
     *
     * @return Display name with type and time
     */
    @NonNull
    public String getDisplayName() {
        return shift.getDisplayName() + " (" + getTimeRange() + ")";
    }

    /**
     * Get short shift identifier.
     *
     * @return Short code for compact display
     */
    @NonNull
    public String getShortIdentifier() {
        return shift.getShortName();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing WorkScheduleShift instances.
     *
     * @param shift Type of shift to build
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull Shift shift) {
        return new Builder(shift);
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
    public static class Builder {
        private final Shift shift;
        private LocalTime startTime;
        private LocalTime endTime;
        private final List<Team> teams = new ArrayList<>();
        private String description = "";

        private Builder(@NonNull Shift shift) {
            this.shift = Objects.requireNonNull( shift, "Shift type cannot be null");
        }

        private Builder(@NonNull WorkScheduleShift existingShift) {
            this.shift = existingShift.shift;
            this.startTime = existingShift.startTime;
            this.endTime = existingShift.endTime;
            this.teams.addAll(existingShift.teams);
            this.description = existingShift.description;
        }

        /**
         * Set shift start time.
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
         * Set shift end time.
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
         * @param teamName Name of the team to add
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
         * @param teamNames Names of teams to add
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
         * @param description Description of the shift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description != null ? description : "";
            return this;
        }

        /**
         * Build the WorkScheduleShift instance.
         *
         * @return New WorkScheduleShift instance
         * @throws IllegalStateException if required fields are not set
         */
        @NonNull
        public WorkScheduleShift build() {
            if (startTime == null) {
                throw new IllegalStateException("Start time must be set");
            }
            if (endTime == null) {
                throw new IllegalStateException("End time must be set");
            }

            return new WorkScheduleShift( shift, startTime, endTime, teams, description);
        }
    }

    // ==================== CLONEABLE SUPPORT ====================

    /**
     * Create a deep clone of this WorkScheduleShift.
     *
     * @return Cloned WorkScheduleShift instance
     */
    @Override
    @NonNull
    public WorkScheduleShift clone() {
        try {
            return new WorkScheduleShift( shift, startTime, endTime, teams, description);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone WorkScheduleShift", e);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkScheduleShift that = (WorkScheduleShift) o;
        return shift == that.shift &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(teams, that.teams) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash( shift, startTime, endTime, teams, description);
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleShift{" +
                "type=" + shift +
                ", time=" + getTimeRange() +
                ", teams=" + teams.size() +
                ", description='" + description + '\'' +
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
                "shift=" + shift +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDurationMinutes() + "min" +
                ", teams=" + teams +
                ", description='" + description + '\'' +
                ", spansMidnight=" + spansMidnight() +
                '}';
    }
}