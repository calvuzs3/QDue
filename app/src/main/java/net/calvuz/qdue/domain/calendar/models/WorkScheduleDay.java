package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * WorkScheduleDay - Domain model representing a calendar day with work shifts.
 *
 * <p>This is a clean architecture domain model independent of external QuattroDue dependencies.
 * Represents a single day in the work schedule system with its associated shifts and team assignments.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state management</li>
 *   <li><strong>Shift Management</strong>: Contains multiple work shifts for the day</li>
 *   <li><strong>Team Tracking</strong>: Tracks both working and off-duty teams</li>
 *   <li><strong>Date Handling</strong>: Proper LocalDate integration with timezone safety</li>
 *   <li><strong>Cloneable Support</strong>: For template-based day generation</li>
 * </ul>
 *
 * <h3>Usage in Clean Architecture:</h3>
 * <ul>
 *   <li><strong>Domain Layer</strong>: Pure business model without external dependencies</li>
 *   <li><strong>Repository Layer</strong>: Used for data transfer between layers</li>
 *   <li><strong>Use Case Layer</strong>: Business logic operations on work schedule days</li>
 *   <li><strong>Presentation Layer</strong>: Calendar display and user interaction</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
public class WorkScheduleDay implements Cloneable {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");

    // ==================== CORE DATA ====================

    private final LocalDate date;
    private final boolean isToday;
    private final List<WorkScheduleShift> shifts;
    private final List<Team> offWorkTeams;  // Teams that are off work on this day

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a new WorkScheduleDay with the specified date.
     *
     * @param date The date of this work schedule day
     */
    public WorkScheduleDay(@NonNull LocalDate date) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.isToday = date.equals(LocalDate.now());
        this.shifts = new ArrayList<>();
        this.offWorkTeams = new ArrayList<>();
    }

    /**
     * Creates a new WorkScheduleDay with complete data.
     *
     * @param date The date of this work schedule day
     * @param shifts List of work shifts for this day
     * @param offWorkTeams List of teams that are off work
     */
    public WorkScheduleDay(@NonNull LocalDate date,
                           @NonNull List<WorkScheduleShift> shifts,
                           @NonNull List<Team> offWorkTeams) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.isToday = date.equals(LocalDate.now());
        this.shifts = new ArrayList<>(Objects.requireNonNull(shifts, "Shifts cannot be null"));
        this.offWorkTeams = new ArrayList<>(Objects.requireNonNull(offWorkTeams, "Off work teams cannot be null"));
    }

    /**
     * Private constructor for cloning.
     */
    private WorkScheduleDay(@NonNull LocalDate date,
                            boolean isToday,
                            @NonNull List<WorkScheduleShift> shifts,
                            @NonNull List<Team> offWorkTeams) {
        this.date = date;
        this.isToday = isToday;
        this.shifts = new ArrayList<>(shifts);
        this.offWorkTeams = new ArrayList<>(offWorkTeams);
    }

    // ==================== GETTERS ====================

    /**
     * Get the date of this work schedule day.
     *
     * @return LocalDate representing this day
     */
    @NonNull
    public LocalDate getDate() {
        return date;
    }

    /**
     * Check if this day is today.
     *
     * @return true if this day represents today's date
     */
    public boolean isToday() {
        return isToday;
    }

    /**
     * Get all work shifts for this day.
     *
     * @return Immutable list of work shifts
     */
    @NonNull
    public List<WorkScheduleShift> getShifts() {
        return Collections.unmodifiableList(shifts);
    }

    /**
     * Get teams that are off work on this day.
     *
     * @return Immutable list of off-work teams
     */
    @NonNull
    public List<Team> getOffWorkTeams() {
        return Collections.unmodifiableList(offWorkTeams);
    }

    /**
     * Get the number of work shifts on this day.
     *
     * @return Number of shifts
     */
    public int getShiftCount() {
        return shifts.size();
    }

    /**
     * Get a specific shift by index.
     *
     * @param index Shift index (0-based)
     * @return WorkScheduleShift at the specified index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @NonNull
    public WorkScheduleShift getShift(int index) {
        return shifts.get(index);
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this day has any work shifts.
     *
     * @return true if day has one or more shifts
     */
    public boolean hasShifts() {
        return !shifts.isEmpty();
    }

    /**
     * Check if a specific team is working on this day.
     *
     * @param team Team to check
     * @return true if team is working in any shift
     */
    public boolean isTeamWorking(@NonNull Team team) {
        Objects.requireNonNull(team, "Team cannot be null");

        return shifts.stream()
                .anyMatch(shift -> shift.getTeams().contains(team));
    }

    /**
     * Check if a specific team is working on this day by team name.
     *
     * @param teamName Name of the team to check
     * @return true if team is working in any shift
     */
    public boolean isTeamWorking(@NonNull String teamName) {
        Objects.requireNonNull(teamName, "Team name cannot be null");

        return shifts.stream()
                .anyMatch(shift -> shift.hasTeamWithName(teamName));
    }

    /**
     * Check if a specific team is off work on this day.
     *
     * @param team Team to check
     * @return true if team is off work
     */
    public boolean isTeamOffWork(@NonNull Team team) {
        Objects.requireNonNull(team, "Team cannot be null");
        return offWorkTeams.contains(team);
    }

    /**
     * Check if a specific team is off work on this day by team name.
     *
     * @param teamName Name of the team to check
     * @return true if team is off work
     */
    public boolean isTeamOffWork(@NonNull String teamName) {
        Objects.requireNonNull(teamName, "Team name cannot be null");
        return offWorkTeams.stream()
                .anyMatch(team -> team.hasName(teamName));
    }

    /**
     * Find which shift index a team is working in.
     *
     * @param team Team to find
     * @return Shift index (0-based), or -1 if team is not working
     */
    public int findTeamShiftIndex(@NonNull Team team) {
        Objects.requireNonNull(team, "Team cannot be null");

        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).getTeams().contains(team)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find which shift index a team is working in by team name.
     *
     * @param teamName Name of the team to find
     * @return Shift index (0-based), or -1 if team is not working
     */
    public int findTeamShiftIndex(@NonNull String teamName) {
        Objects.requireNonNull(teamName, "Team name cannot be null");

        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).hasTeamWithName(teamName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get teams working in a specific shift.
     *
     * @param shiftIndex Index of the shift (0-based)
     * @return List of teams in the shift
     * @throws IndexOutOfBoundsException if shift index is invalid
     */
    @NonNull
    public List<Team> getTeamsInShift(int shiftIndex) {
        return shifts.get(shiftIndex).getTeams();
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get localized day of week name.
     *
     * @return Localized day name (e.g., "Monday", "Tuesday")
     */
    @NonNull
    public String getDayOfWeekName() {
        return date.format(DAY_FORMATTER);
    }

    /**
     * Get day of month as integer.
     *
     * @return Day of month (1-31)
     */
    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    /**
     * Get off-work teams as concatenated string.
     *
     * @return String with all off-work team names
     */
    @NonNull
    public String getOffWorkTeamsAsString() {
        return offWorkTeams.stream()
                .map(Team::getName)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Get teams in specific shift as string.
     *
     * @param shiftIndex Index of the shift
     * @return String with team names for the shift
     */
    @NonNull
    public String getTeamsInShiftAsString(int shiftIndex) {
        if (shiftIndex < 0 || shiftIndex >= shifts.size()) {
            return "";
        }
        return shifts.get(shiftIndex).getTeamsAsString();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing WorkScheduleDay instances.
     *
     * @param date The date for the new day
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull LocalDate date) {
        return new Builder(date);
    }

    /**
     * Create a builder from existing WorkScheduleDay.
     *
     * @return Builder instance with current day data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class for creating WorkScheduleDay instances.
     */
    public static class Builder {
        private final LocalDate date;
        private final List<WorkScheduleShift> shifts = new ArrayList<>();
        private final List<Team> offWorkTeams = new ArrayList<>();

        private Builder(@NonNull LocalDate date) {
            this.date = Objects.requireNonNull(date, "Date cannot be null");
        }

        private Builder(@NonNull WorkScheduleDay existingDay) {
            this.date = existingDay.date;
            this.shifts.addAll(existingDay.shifts);
            this.offWorkTeams.addAll(existingDay.offWorkTeams);
        }

        /**
         * Add a work shift to this day.
         *
         * @param shift Work shift to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addShift(@NonNull WorkScheduleShift shift) {
            this.shifts.add(Objects.requireNonNull(shift, "Shift cannot be null"));
            return this;
        }

        /**
         * Add multiple work shifts to this day.
         *
         * @param shifts Work shifts to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addShifts(@NonNull List<WorkScheduleShift> shifts) {
            Objects.requireNonNull(shifts, "Shifts cannot be null");
            this.shifts.addAll(shifts);
            return this;
        }

        /**
         * Add a team that is off work on this day.
         *
         * @param team Team that is off work
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addOffWorkTeam(@NonNull Team team) {
            this.offWorkTeams.add(Objects.requireNonNull(team, "Team cannot be null"));
            return this;
        }

        /**
         * Add a team that is off work on this day by name.
         *
         * @param teamName Name of the team that is off work
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addOffWorkTeam(@NonNull String teamName) {
            Objects.requireNonNull(teamName, "Team name cannot be null");
            this.offWorkTeams.add(Team.fromName(teamName));
            return this;
        }

        /**
         * Add multiple teams that are off work on this day.
         *
         * @param teams Teams that are off work
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addOffWorkTeams(@NonNull List<Team> teams) {
            Objects.requireNonNull(teams, "Teams cannot be null");
            this.offWorkTeams.addAll(teams);
            return this;
        }

        /**
         * Add multiple teams that are off work on this day by names.
         *
         * @param teamNames Names of teams that are off work
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addOffWorkTeamNames(@NonNull List<String> teamNames) {
            Objects.requireNonNull(teamNames, "Team names cannot be null");
            teamNames.forEach(name -> {
                if (name != null && !name.trim().isEmpty()) {
                    this.offWorkTeams.add(Team.fromName(name.trim()));
                }
            });
            return this;
        }

        /**
         * Build the WorkScheduleDay instance.
         *
         * @return New WorkScheduleDay instance
         */
        @NonNull
        public WorkScheduleDay build() {
            return new WorkScheduleDay(date, shifts, offWorkTeams);
        }
    }

    // ==================== CLONEABLE SUPPORT ====================

    /**
     * Create a deep clone of this WorkScheduleDay.
     * Useful for template-based day generation.
     *
     * @return Cloned WorkScheduleDay instance
     */
    @Override
    @NonNull
    public WorkScheduleDay clone() {
        try {
            // Clone shifts deeply
            List<WorkScheduleShift> clonedShifts = new ArrayList<>();
            for (WorkScheduleShift shift : shifts) {
                clonedShifts.add(shift.clone());
            }

            // Clone off-work teams (Team objects are immutable, so shallow copy is fine)
            List<Team> clonedOffWorkTeams = new ArrayList<>(offWorkTeams);

            return new WorkScheduleDay(date, isToday, clonedShifts, clonedOffWorkTeams);

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone WorkScheduleDay", e);
        }
    }

    /**
     * Create a clone with a different date.
     * Useful for template-based generation where shifts remain the same but date changes.
     *
     * @param newDate New date for the cloned day
     * @return Cloned WorkScheduleDay with new date
     */
    @NonNull
    public WorkScheduleDay cloneWithDate(@NonNull LocalDate newDate) {
        Objects.requireNonNull(newDate, "New date cannot be null");

        try {
            // Clone shifts deeply
            List<WorkScheduleShift> clonedShifts = new ArrayList<>();
            for (WorkScheduleShift shift : shifts) {
                clonedShifts.add(shift.clone());
            }

            // Clone off-work teams
            List<Team> clonedOffWorkTeams = new ArrayList<>(offWorkTeams);

            // Create new instance with new date and recalculated isToday
            return new WorkScheduleDay(newDate, clonedShifts, clonedOffWorkTeams);

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone WorkScheduleDay with new date", e);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkScheduleDay that = (WorkScheduleDay) o;
        return isToday == that.isToday &&
                Objects.equals(date, that.date) &&
                Objects.equals(shifts, that.shifts) &&
                Objects.equals(offWorkTeams, that.offWorkTeams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, isToday, shifts, offWorkTeams);
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleDay{" +
                "date=" + date +
                ", isToday=" + isToday +
                ", shiftsCount=" + shifts.size() +
                ", offWorkTeamsCount=" + offWorkTeams.size() +
                '}';
    }

    /**
     * Get detailed string representation for debugging.
     *
     * @return Detailed string with all day information
     */
    @NonNull
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WorkScheduleDay{")
                .append("date=").append(date)
                .append(", dayOfWeek=").append(getDayOfWeekName())
                .append(", isToday=").append(isToday)
                .append(", shifts=[");

        for (int i = 0; i < shifts.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(shifts.get(i).toString());
        }

        sb.append("], offWorkTeams=").append(offWorkTeams).append('}');
        return sb.toString();
    }
}