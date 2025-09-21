package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * WorkScheduleDay - Domain model representing a calendar day with work workShifts.
 *
 * <p>This is a clean architecture domain model independent of external QuattroDue dependencies.
 * Represents a single day in the work schedule system with its associated workShifts and team assignments
 * with full localization support for calendar display and user interaction.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state management</li>
 *   <li><strong>Shift Management</strong>: Contains multiple work workShifts for the day</li>
 *   <li><strong>Team Tracking</strong>: Tracks both working and off-duty teams</li>
 *   <li><strong>Date Handling</strong>: Proper LocalDate integration with timezone safety</li>
 *   <li><strong>Cloneable Support</strong>: For template-based day generation</li>
 *   <li><strong>Localization Support</strong>: Full i18n support for all display text</li>
 * </ul>
 *
 * <h3>Day Status Types:</h3>
 * <ul>
 *   <li><strong>WORKING_DAY</strong>: Regular working day with workShifts</li>
 *   <li><strong>REST_DAY</strong>: Rest day with no workShifts</li>
 *   <li><strong>HOLIDAY</strong>: Holiday with special arrangements</li>
 *   <li><strong>PARTIAL_DAY</strong>: Partial working day with reduced workShifts</li>
 * </ul>
 *
 * <h3>Usage in Clean Architecture:</h3>
 * <ul>
 *   <li><strong>Domain Layer</strong>: Pure business model without external dependencies</li>
 *   <li><strong>Repository Layer</strong>: Used for data transfer between layers</li>
 *   <li><strong>Use Case Layer</strong>: Business logic operations on work schedule days</li>
 *   <li><strong>Presentation Layer</strong>: Calendar display and user interaction</li>
 * </ul>
 */
public class WorkScheduleDay
        implements Cloneable
{

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern( "EEEE" );

    // ==================== DAY STATUS ENUM ====================

    /**
     * Enumeration of day status types with localization keys.
     */
    public enum DayStatus
    {
        WORKING_DAY( "working_day", "regular_working_day_with_shifts" ),
        REST_DAY( "rest_day", "rest_day_with_no_shifts" ),
        HOLIDAY( "holiday", "holiday_with_special_arrangements" ),
        PARTIAL_DAY( "partial_day", "partial_working_day_with_reduced_shifts" );

        private final String displayNameKey;
        private final String descriptionKey;

        DayStatus(String displayNameKey, String descriptionKey) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
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
         * Get localization key for description.
         *
         * @return Localization key for description
         */
        @NonNull
        public String getDescriptionKey() {
            return descriptionKey;
        }
    }

    // ==================== CORE DATA ====================

    private final LocalDate date;
    private final boolean isToday;
    private final DayStatus dayStatus;
    private final List<WorkScheduleShift> workShifts;
    private final List<Team> offWorkTeams;  // Teams that are off work on this day

    // ==================== CONSTRUCTORS ====================

    /**
     * Private constructor for builder pattern.
     */
    private WorkScheduleDay(@NonNull Builder builder) {

        this.date = Objects.requireNonNull( builder.date, "Date cannot be null" );
        this.isToday = date.equals( LocalDate.now() );
        this.dayStatus = builder.dayStatus != null ? builder.dayStatus : determineDayStatus(
                builder.shifts );
        this.workShifts = new ArrayList<>( builder.shifts );
        this.offWorkTeams = new ArrayList<>( builder.offWorkTeams );
    }

    /**
     * Private constructor for cloning.
     */
    private WorkScheduleDay(
            @NonNull LocalDate date,
            boolean isToday,
            @Nullable DayStatus dayStatus,
            @NonNull List<WorkScheduleShift> workShifts,
            @NonNull List<Team> offWorkTeams
    ) {

        this.date = date;
        this.isToday = isToday;
        this.dayStatus = dayStatus != null ? dayStatus : determineDayStatus( workShifts );
        this.workShifts = new ArrayList<>( workShifts );
        this.offWorkTeams = new ArrayList<>( offWorkTeams );
    }

    // ==================== GETTERS ====================

    /**
     * Wrapper for missing ID
     * @return hash in place of an id
     */
    @NonNull
    public String getId() {
        return hashCode() + "";
    }

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
     * Get the day status classification.
     *
     * @return DayStatus classification
     */
    @NonNull
    public DayStatus getDayStatus() {
        return dayStatus;
    }

    /**
     * Get all work workShifts for this day.
     *
     * @return Immutable list of work workShifts
     */
    @NonNull
    public List<WorkScheduleShift> getWorkShifts() {
        return Collections.unmodifiableList( workShifts );
    }

    /**
     * Wrapper..
     */
    @NonNull
    public List<WorkScheduleShift> getShifts() {
        return this.getWorkShifts();
    }

    /**
     * Get teams that are off work on this day.
     *
     * @return Immutable list of off-work teams
     */
    @NonNull
    public List<Team> getOffWorkTeams() {
        return Collections.unmodifiableList( offWorkTeams );
    }

    /**
     * Get the number of work workShifts on this day.
     *
     * @return Number of workShifts
     */
    public int getShiftCount() {
        return workShifts.size();
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
        return workShifts.get( index );
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this day has any work workShifts.
     *
     * @return true if day has one or more workShifts
     */
    public boolean hasShifts() {
        return !workShifts.isEmpty();
    }

    /**
     * Check if this is a working day.
     *
     * @return true if day status is WORKING_DAY or PARTIAL_DAY
     */
    public boolean isWorkingDay() {
        return dayStatus == DayStatus.WORKING_DAY || dayStatus == DayStatus.PARTIAL_DAY;
    }

    /**
     * Check if this is a rest day.
     *
     * @return true if day status is REST_DAY
     */
    public boolean isRestDay() {
        return dayStatus == DayStatus.REST_DAY;
    }

    /**
     * Check if this is a holiday.
     *
     * @return true if day status is HOLIDAY
     */
    public boolean isHoliday() {
        return dayStatus == DayStatus.HOLIDAY;
    }

    /**
     * Check if this is yesterday.
     *
     * @return true if this day is yesterday
     */
    public boolean isYesterday() {
        return date.equals( LocalDate.now().minusDays( 1 ) );
    }

    /**
     * Check if this is tomorrow.
     *
     * @return true if this day is tomorrow
     */
    public boolean isTomorrow() {
        return date.equals( LocalDate.now().plusDays( 1 ) );
    }

    /**
     * Check if a specific team is working on this day.
     *
     * @param team Team to check
     * @return true if team is working in any shift
     */
    public boolean isTeamWorking(@NonNull Team team) {
        Objects.requireNonNull( team, "Team cannot be null" );

        return workShifts.stream()
                .anyMatch( shift -> shift.getTeams().contains( team ) );
    }

    /**
     * Check if a specific team is working on this day by team name.
     *
     * @param teamID Name of the team to check
     * @return true if team is working in any shift
     */
    public boolean isTeamWorking(@NonNull String teamID) {
        Objects.requireNonNull( teamID, "Team ID cannot be null" );

        return workShifts.stream()
                .anyMatch( shift -> shift.hasTeamWithId( teamID ) );
    }

    /**
     * Check if a specific team is off work on this day.
     *
     * @param team Team to check
     * @return true if team is off work
     */
    public boolean isTeamOffWork(@NonNull Team team) {
        Objects.requireNonNull( team, "Team cannot be null" );
        return offWorkTeams.contains( team );
    }

    /**
     * Check if a specific team is off work on this day by team name.
     *
     * @param teamName Name of the team to check
     * @return true if team is off work
     */
    public boolean isTeamOffWork(@NonNull String teamName) {
        Objects.requireNonNull( teamName, "Team name cannot be null" );
        return offWorkTeams.stream()
                .anyMatch( team -> team.hasName( teamName ) );
    }

    /**
     * Find which shift index a team is working in.
     *
     * @param team Team to find
     * @return Shift index (0-based), or -1 if team is not working
     */
    public int findTeamShiftIndex(@NonNull Team team) {
        Objects.requireNonNull( team, "Team cannot be null" );

        for (int i = 0; i < workShifts.size(); i++) {
            if (workShifts.get( i ).getTeams().contains( team )) {
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
        Objects.requireNonNull( teamName, "Team name cannot be null" );

        for (int i = 0; i < workShifts.size(); i++) {
            if (workShifts.get( i ).hasTeamWithId( teamName )) {
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
        return workShifts.get( shiftIndex ).getTeams();
    }

    // ==================== DISPLAY METHODS ====================

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
                .map( Team::getName )
                .collect( StringBuilder::new, StringBuilder::append, StringBuilder::append )
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
        if (shiftIndex < 0 || shiftIndex >= workShifts.size()) {
            return "";
        }
        return workShifts.get( shiftIndex ).getTeamsAsString();
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get localized day of week name with special handling for today/yesterday/tomorrow.
     *
     * @return Localized day name (e.g., "Today", "Yesterday", "Monday")
     */
    @NonNull
    public String getDayName() {
        if (isToday()) {
            return "Today";
        }
        return date.format( DAY_FORMATTER );
    }

    /**
     * Get team status summary.
     *
     * @return Team status
     */
    @NonNull
    public String getTeamStatus() {
        StringBuilder status = new StringBuilder();

        // Working teams
        List<String> workingTeamNames = getWorkingTeamNames();
        if (!workingTeamNames.isEmpty()) {
            status.append( "Teams working: " );
            status.append( String.join( ", ", workingTeamNames ) );
        }

        // Off-work teams
        if (!offWorkTeams.isEmpty()) {
            if (status.length() > 0) {
                status.append( " | " );
            }
            status.append( "Teams off: " );
            status.append( String.join( ", ", offWorkTeams.stream().map( Team::getName ).collect(
                    Collectors.toList() ) ) );
        }

        return status.length() > 0 ? status.toString() : "No team information";
    }

    /**
     * Get localized summary for the day.
     *
     * @return Localized day summary
     */
    @NonNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        // Day name and date
        summary.append( getDayName() );

        // Add date if not today/yesterday/tomorrow
        if (!isToday() && !isYesterday() && !isTomorrow()) {
            summary.append( MessageFormat.format( "({0})", date.toString() ) );
        }

        // Status if not standard working day
        if (!dayStatus.equals( DayStatus.WORKING_DAY )) {
            summary.append( MessageFormat.format( " [{0}]", this.dayStatus ) );
        }

        // Shift count
        summary.append( " - " )
                .append( getShiftCount() )
                .append( " shifts" );
        summary.append( "\n" );

        return summary.toString();
    }

    /**
     * Get localized detailed summary for the day.
     *
     * @return Localized detailed summary
     */
    @NonNull
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();

        // Basic summary
        summary.append( getSummary() );

        // Team status if available
        String teamStatus = getTeamStatus();
        if (!teamStatus.equals( "No team information" )) {
            summary.append( teamStatus ).append( "\n" );
        }

        return summary.toString();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get list of working team names.
     *
     * @return List of team names that are working
     */
    @NonNull
    private List<String> getWorkingTeamNames() {
        return workShifts.stream()
                .flatMap( shift -> shift.getTeams().stream() )
                .map( Team::getName )
                .distinct()
                .sorted()
                .collect( Collectors.toList() );
    }

    /**
     * Determine day status based on workShifts.
     *
     * @param shifts List of workShifts for the day
     * @return Appropriate DayStatus
     */
    @NonNull
    private DayStatus determineDayStatus(@NonNull List<WorkScheduleShift> shifts) {
        if (shifts.isEmpty()) {
            return DayStatus.REST_DAY;
        } else {
            // Could implement more sophisticated logic here
            return DayStatus.WORKING_DAY;
        }
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing WorkScheduleDay instances.
     *
     * @param date The date for the new day
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(
            @NonNull LocalDate date
    ) {
        return new Builder( date );
    }

    /**
     * Builder class for creating WorkScheduleDay instances with localization support.
     */
    public static class Builder
    {
        private LocalDate date;
        private DayStatus dayStatus;
        private final List<WorkScheduleShift> shifts = new ArrayList<>();
        private final List<Team> offWorkTeams = new ArrayList<>();

        public Builder() {
        }

        public Builder(@NonNull LocalDate date) {
            this.date = Objects.requireNonNull( date, "Date cannot be null" );
        }

        /**
         * Copy data from existing WorkScheduleDay (for withLocalizer implementation).
         */
        @NonNull
        public Builder copyFrom(@NonNull WorkScheduleDay source) {
            this.dayStatus = source.dayStatus;
            this.shifts.clear();
            this.shifts.addAll( source.workShifts );
            this.offWorkTeams.clear();
            this.offWorkTeams.addAll( source.offWorkTeams );

            return this;
        }

        // ==================== GETTERS ====================

        public LocalDate getDate() {
            return date;
        }

        public DayStatus getDayStatus() {
            return dayStatus;
        }

        public List<WorkScheduleShift> getShifts() {
            return shifts;
        }

        public List<Team> getOffWorkTeams() {
            return offWorkTeams;
        }

        // ==================== SETTERS ====================

        /**
         * Set the date for the day.
         *
         * @param date
         * @return Builder instance for chaining
         */
        public Builder date(@NonNull LocalDate date) {
            this.date = Objects.requireNonNull( date, "Date cannot be null" );
            return this;
        }

        /**
         * Set day status.
         *
         * @param dayStatus Day status classification
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder dayStatus(@Nullable DayStatus dayStatus) {
            this.dayStatus = dayStatus;
            return this;
        }

        /**
         * Add a work shift to this day.
         *
         * @param shift Work shift to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addShift(@NonNull WorkScheduleShift shift) {
            this.shifts.add( Objects.requireNonNull( shift, "Shift cannot be null" ) );
            return this;
        }

        /**
         * Add multiple work workShifts to this day.
         *
         * @param shifts Work workShifts to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addShifts(@NonNull List<WorkScheduleShift> shifts) {
            Objects.requireNonNull( shifts, "Shifts cannot be null" );
            this.shifts.addAll( shifts );
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
            this.offWorkTeams.add( Objects.requireNonNull( team, "Team cannot be null" ) );
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
            Objects.requireNonNull( teamName, "Team name cannot be null" );
            this.offWorkTeams.add( Team.fromName( teamName ) );
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
            Objects.requireNonNull( teams, "Teams cannot be null" );
            this.offWorkTeams.addAll( teams );
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
            Objects.requireNonNull( teamNames, "Team names cannot be null" );
            teamNames.forEach( name -> {
                if (name != null && !name.trim().isEmpty()) {
                    this.offWorkTeams.add( Team.fromName( name.trim() ) );
                }
            } );
            return this;
        }

        // ==================== BUILDER ====================

        /**
         * Build the WorkScheduleDay instance.
         *
         * @return New WorkScheduleDay instance
         */
        @NonNull
        public WorkScheduleDay build() {
            return new WorkScheduleDay( this );
        }
    }

    // ==================== CLONEABLE SUPPORT ====================

    /**
     * Create a deep clone of this WorkScheduleDay with localization preservation.
     * Useful for template-based day generation.
     *
     * @return Cloned WorkScheduleDay instance
     */
    @Override
    @NonNull
    public WorkScheduleDay clone() {
        try {
            // Clone workShifts deeply
            List<WorkScheduleShift> clonedShifts = new ArrayList<>();
            for (WorkScheduleShift shift : workShifts) {
                clonedShifts.add( shift.clone() );
            }

            // Clone off-work teams (Team objects are immutable, so shallow copy is fine)
            List<Team> clonedOffWorkTeams = new ArrayList<>( offWorkTeams );

            return new WorkScheduleDay( date, isToday, dayStatus, clonedShifts, clonedOffWorkTeams );
        } catch (Exception e) {
            throw new RuntimeException( "Failed to clone WorkScheduleDay", e );
        }
    }

    /**
     * Create a clone with a different date and localization preservation.
     * Useful for template-based generation where workShifts remain the same but date changes.
     *
     * @param newDate New date for the cloned day
     * @return Cloned WorkScheduleDay with new date
     */
    @NonNull
    public WorkScheduleDay cloneWithDate(@NonNull LocalDate newDate) {
        Objects.requireNonNull( newDate, "New date cannot be null" );

        try {
            // Clone workShifts deeply
            List<WorkScheduleShift> clonedShifts = new ArrayList<>();
            for (WorkScheduleShift shift : workShifts) {
                clonedShifts.add( shift.clone() );
            }

            // Clone off-work teams
            List<Team> clonedOffWorkTeams = new ArrayList<>( offWorkTeams );

            // Create new instance with new date and recalculated isToday
            return new WorkScheduleDay( newDate, newDate.equals( LocalDate.now() ), dayStatus,
                                        clonedShifts, clonedOffWorkTeams);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to clone WorkScheduleDay with new date", e );
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkScheduleDay that = (WorkScheduleDay) o;
        return isToday == that.isToday &&
                Objects.equals( date, that.date ) &&
                Objects.equals( dayStatus, that.dayStatus ) &&
                Objects.equals( workShifts, that.workShifts ) &&
                Objects.equals( offWorkTeams, that.offWorkTeams );
    }

    @Override
    public int hashCode() {
        return Objects.hash( date, isToday, dayStatus, workShifts, offWorkTeams );
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleDay{" +
                "date=" + date +
                ", isToday=" + isToday +
                ", dayStatus=" + dayStatus +
                ", shiftsCount=" + workShifts.size() +
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
        sb.append( "WorkScheduleDay{" )
                .append( "date=" ).append( date )
                .append( ", dayOfWeek=" ).append( date.format( DAY_FORMATTER ) )
                .append( ", isToday=" ).append( isToday )
                .append( ", dayStatus=" ).append( dayStatus )
                .append( ", workShifts=[" );

        for (int i = 0; i < workShifts.size(); i++) {
            if (i > 0) sb.append( ", " );
            sb.append( workShifts.get( i ).toString() );
        }

        sb.append( "], offWorkTeams=" ).append( offWorkTeams )
                .append( '}' );
        return sb.toString();
    }
}