package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * WorkScheduleEvent - Volatile work schedule event for calendar integration.
 *
 * <p>Represents a single work schedule event generated from the Quattrodue pattern
 * calculations. These events are volatile (not persisted) and generated on-demand
 * for calendar display and integration with the events system.</p>
 *
 * <h3>Event Characteristics:</h3>
 * <ul>
 *   <li><strong>Volatile</strong>: Generated on-demand, not stored in database</li>
 *   <li><strong>Pattern-Based</strong>: Derived from Quattrodue 4-2 pattern calculations</li>
 *   <li><strong>Calendar Integration</strong>: Designed for seamless calendar display</li>
 *   <li><strong>Team-Specific</strong>: Contains team assignment and shift information</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li>WorkScheduleRepository generates these events for date ranges</li>
 *   <li>MonthCalendarFragment displays these in calendar grid</li>
 *   <li>EventsService can merge with persistent events</li>
 *   <li>CalendarDataProvider combines with LocalEvents for unified view</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class WorkScheduleEvent {

    // ==================== IDENTIFICATION ====================

    private String id;                           // Unique ID for this event instance
    private String sourceId;                     // Source identifier (pattern + date + shift)

    // ==================== TEMPORAL INFORMATION ====================

    private LocalDate date;                     // Event date
    private LocalTime startTime;                // Shift start time
    private LocalTime endTime;                  // Shift end time
    private boolean crossesMidnight;            // True if shift crosses midnight

    // ==================== WORK SCHEDULE DATA ====================

    private Shift shift;                        // Associated shift from Day calculation
    private Team team;                          // Team assigned to this shift
    private ShiftType shiftType;                // Type of shift (Morning, Afternoon, Night)

    // ==================== DISPLAY INFORMATION ====================

    private String title;                       // Display title for calendar
    private String description;                 // Detailed description
    private String colorHex;                    // Color for calendar display

    // ==================== USER CONTEXT ====================

    private Long userId;                        // Optional user ID for filtering
    private boolean isUserRelevant;             // True if event is relevant to user's team

    // ==================== PATTERN METADATA ====================

    private int dayInCycle;                     // Day position in 18-day cycle (0-17)
    private long daysFromSchemeStart;           // Days from scheme reference start
    private String patternName;                 // Pattern name (e.g., "Quattrodue 4-2")

    // ==================== GENERATION METADATA ====================

    private final long generatedTimestamp;      // When this event was generated
    private String generatedBy;                 // Service/component that generated event
    private boolean isVolatile;                 // Always true for WorkScheduleEvent

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for framework use.
     */
    public WorkScheduleEvent() {
        this.id = UUID.randomUUID().toString();
        this.generatedTimestamp = System.currentTimeMillis();
        this.isVolatile = true;
        this.crossesMidnight = false;
        this.isUserRelevant = false;
    }

    /**
     * Constructor with basic work schedule information.
     *
     * @param date Event date
     * @param shift Associated shift data
     */
    public WorkScheduleEvent(@NonNull LocalDate date, @NonNull Shift shift) {
        this();
        this.date = date;
        setShift(shift);
    }

    /**
     * Constructor with complete information.
     *
     * @param date Event date
     * @param shift Associated shift data
     * @param team Team assignment
     * @param userId Optional user ID for relevance
     */
    public WorkScheduleEvent(@NonNull LocalDate date, @NonNull Shift shift,
                             @Nullable Team team, @Nullable Long userId) {
        this(date, shift);
        this.team = team;
        this.userId = userId;
        updateUserRelevance();
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getSourceId() {
        return sourceId;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    @Nullable
    public LocalTime getStartTime() {
        return startTime;
    }

    @Nullable
    public LocalTime getEndTime() {
        return endTime;
    }

    public boolean crossesMidnight() {
        return crossesMidnight;
    }

    @Nullable
    public Shift getShift() {
        return shift;
    }

    @Nullable
    public Team getTeam() {
        return team;
    }

    @Nullable
    public ShiftType getShiftType() {
        return shiftType;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getColorHex() {
        return colorHex;
    }

    @Nullable
    public Long getUserId() {
        return userId;
    }

    public boolean isUserRelevant() {
        return isUserRelevant;
    }

    public int getDayInCycle() {
        return dayInCycle;
    }

    public long getDaysFromSchemeStart() {
        return daysFromSchemeStart;
    }

    @Nullable
    public String getPatternName() {
        return patternName;
    }

    public long getGeneratedTimestamp() {
        return generatedTimestamp;
    }

    @Nullable
    public String getGeneratedBy() {
        return generatedBy;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    // ==================== SETTERS ====================

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setSourceId(@Nullable String sourceId) {
        this.sourceId = sourceId;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
        updateSourceId();
    }

    public void setStartTime(@Nullable LocalTime startTime) {
        this.startTime = startTime;
        updateCrossesMidnight();
    }

    public void setEndTime(@Nullable LocalTime endTime) {
        this.endTime = endTime;
        updateCrossesMidnight();
    }

    public void setShift(@Nullable Shift shift) {
        this.shift = shift;

        if (shift != null) {
            // Extract information from shift
            this.shiftType = shift.getShiftType();

            // Create Team from LegacyShift's HalfTeams
            this.team = Team.fromShift(shift);

            // Extract timing if available
            if (shiftType != null) {
                this.startTime = shiftType.getStartTime();
                this.endTime = shiftType.getEndTime();
                this.colorHex = String.valueOf(shiftType.getColor() );
                updateCrossesMidnight();
            }

            // Update display information
            updateDisplayInformation();
        }

        updateSourceId();
        updateUserRelevance();
    }

    public void setTeam(@Nullable Team team) {
        this.team = team;
        updateDisplayInformation();
        updateUserRelevance();
        updateSourceId();
    }

    public void setShiftType(@Nullable ShiftType shiftType) {
        this.shiftType = shiftType;

        if (shiftType != null) {
            this.startTime = shiftType.getStartTime();
            this.endTime = shiftType.getEndTime();
            this.colorHex = String.valueOf(shiftType.getColor() );
            updateCrossesMidnight();
        }

        updateDisplayInformation();
        updateSourceId();
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setColorHex(@Nullable String colorHex) {
        this.colorHex = colorHex;
    }

    public void setUserId(@Nullable Long userId) {
        this.userId = userId;
        updateUserRelevance();
    }

    public void setDayInCycle(int dayInCycle) {
        this.dayInCycle = dayInCycle;
        updateSourceId();
    }

    public void setDaysFromSchemeStart(long daysFromSchemeStart) {
        this.daysFromSchemeStart = daysFromSchemeStart;
    }

    public void setPatternName(@Nullable String patternName) {
        this.patternName = patternName;
    }

    public void setGeneratedBy(@Nullable String generatedBy) {
        this.generatedBy = generatedBy;
    }

    // ==================== COMPUTED PROPERTIES ====================

    /**
     * Get full start date/time combining date and start time.
     *
     * @return LocalDateTime for event start, null if no start time
     */
    @Nullable
    public LocalDateTime getStartDateTime() {
        if (date != null && startTime != null) {
            return LocalDateTime.of(date, startTime);
        }
        return null;
    }

    /**
     * Get full end date/time, handling midnight crossover.
     *
     * @return LocalDateTime for event end, null if no end time
     */
    @Nullable
    public LocalDateTime getEndDateTime() {
        if (date != null && endTime != null) {
            LocalDate endDate = crossesMidnight ? date.plusDays(1) : date;
            return LocalDateTime.of(endDate, endTime);
        }
        return null;
    }

    /**
     * Get duration in minutes.
     *
     * @return Duration in minutes, 0 if timing not available
     */
    public long getDurationMinutes() {
        LocalDateTime start = getStartDateTime();
        LocalDateTime end = getEndDateTime();

        if (start != null && end != null) {
            return java.time.Duration.between(start, end).toMinutes();
        }

        return 0;
    }

    /**
     * Check if this event has timing information.
     *
     * @return true if start and end times are available
     */
    public boolean hasTimingInfo() {
        return startTime != null && endTime != null;
    }

    /**
     * Check if this event has team assignment.
     *
     * @return true if team is assigned
     */
    public boolean hasTeamAssignment() {
        return team != null;
    }

    /**
     * Check if this event has shift type information.
     *
     * @return true if shift type is available
     */
    public boolean hasShiftType() {
        return shiftType != null;
    }

    // ==================== COMPATIBILITY METHODS ====================

    /**
     * Get HalfTeams from assigned Team for backward compatibility.
     *
     * @return Set of HalfTeams, empty if no team assigned
     */
    @NonNull
    public Set<HalfTeam> getHalfTeams() {
        if (team != null) {
            return team.getHalfTeams();
        }
        return new HashSet<>();
    }

    /**
     * Get primary HalfTeam from assigned Team.
     *
     * @return Primary HalfTeam, or null if no team assigned
     */
    @Nullable
    public HalfTeam getPrimaryHalfTeam() {
        return team != null ? team.getPrimaryHalfTeam() : null;
    }

    /**
     * Get secondary HalfTeam from assigned Team.
     *
     * @return Secondary HalfTeam, or null if no team assigned
     */
    @Nullable
    public HalfTeam getSecondaryHalfTeam() {
        return team != null ? team.getSecondaryHalfTeam() : null;
    }

    /**
     * Check if this event's team contains a specific HalfTeam.
     *
     * @param halfTeam HalfTeam to check
     * @return true if team contains the HalfTeam
     */
    public boolean containsHalfTeam(@Nullable HalfTeam halfTeam) {
        return team != null && team.containsHalfTeam(halfTeam);
    }

    /**
     * Set team from individual HalfTeams (compatibility method).
     *
     * @param primaryHalfTeam First HalfTeam
     * @param secondaryHalfTeam Second HalfTeam
     */
    public void setTeamFromHalfTeams(@Nullable HalfTeam primaryHalfTeam, @Nullable HalfTeam secondaryHalfTeam) {
        if (primaryHalfTeam != null && secondaryHalfTeam != null) {
            try {
                this.team = new Team(primaryHalfTeam, secondaryHalfTeam);
                updateDisplayInformation();
                updateUserRelevance();
                updateSourceId();
            } catch (Exception e) {
                // Invalid team composition, keep team as null
                this.team = null;
            }
        } else {
            this.team = null;
        }
    }

    /**
     * Set team from Set&lt;HalfTeam&gt; (compatibility method).
     *
     * @param halfTeams Set of HalfTeams (must contain exactly 2)
     */
    public void setTeamFromHalfTeamSet(@Nullable Set<HalfTeam> halfTeams) {
        if (halfTeams != null && halfTeams.size() == 2) {
            try {
                this.team = new Team(halfTeams);
                updateDisplayInformation();
                updateUserRelevance();
                updateSourceId();
            } catch (Exception e) {
                // Invalid team composition
                this.team = null;
            }
        } else {
            this.team = null;
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Update crosses midnight flag based on start/end times.
     */
    private void updateCrossesMidnight() {
        if (startTime != null && endTime != null) {
            crossesMidnight = endTime.isBefore(startTime);
        } else {
            crossesMidnight = false;
        }
    }

    /**
     * Update display information based on available data.
     */
    private void updateDisplayInformation() {
        StringBuilder titleBuilder = new StringBuilder();
        StringBuilder descBuilder = new StringBuilder();

        // Build title
        if (shiftType != null) {
            titleBuilder.append(shiftType.getName());
        } else {
            titleBuilder.append("Work Shift");
        }

        if (team != null) {
            titleBuilder.append(" - Team ").append(team.getTeamName());
        }

        // Build description
        if (hasTimingInfo()) {
            descBuilder.append("Time: ")
                    .append(startTime.toString())
                    .append(" - ")
                    .append(endTime.toString());

            if (crossesMidnight) {
                descBuilder.append(" (+1 day)");
            }
        }

        if (team != null && descBuilder.length() > 0) {
            descBuilder.append("\n");
        }

        if (team != null) {
            descBuilder.append("Team: ").append(team.getTeamName());
        }

        this.title = titleBuilder.toString();
        this.description = descBuilder.length() > 0 ? descBuilder.toString() : null;
    }

    /**
     * Update user relevance based on team assignment and user context.
     */
    private void updateUserRelevance() {
        // For now, simple logic - can be enhanced with user service integration
        isUserRelevant = (userId != null && team != null);
    }

    /**
     * Update source ID for tracking and deduplication.
     */
    private void updateSourceId() {
        if (date != null) {
            StringBuilder sourceBuilder = new StringBuilder();
            sourceBuilder.append("WS_").append(date.toString());

            if (team != null) {
                sourceBuilder.append("_").append(team.getTeamName());
            }

            if (shiftType != null) {
                sourceBuilder.append("_").append(shiftType.getName().replaceAll("\\s+", ""));
            }

            sourceBuilder.append("_").append(dayInCycle);

            this.sourceId = sourceBuilder.toString();
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        WorkScheduleEvent that = (WorkScheduleEvent) obj;
        return Objects.equals(sourceId, that.sourceId) ||
                (Objects.equals(date, that.date) &&
                        Objects.equals(team, that.team) &&
                        Objects.equals(shiftType, that.shiftType));
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId != null ? sourceId :
                Objects.hash(date, team, shiftType));
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleEvent{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", title='" + title + '\'' +
                ", team=" + (team != null ? team.getTeamName() : "null") +
                ", shiftType=" + (shiftType != null ? shiftType.getName() : "null") +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", dayInCycle=" + dayInCycle +
                ", isUserRelevant=" + isUserRelevant +
                '}';
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Builder for creating WorkScheduleEvent instances.
     */
    public static class Builder {
        private final WorkScheduleEvent event;

        public Builder() {
            this.event = new WorkScheduleEvent();
        }

        public Builder(@NonNull LocalDate date) {
            this();
            this.event.setDate(date);
        }

        public Builder setShift(@Nullable Shift shift) {
            event.setShift(shift);
            return this;
        }

        public Builder setTeam(@Nullable Team team) {
            event.setTeam(team);
            return this;
        }

        /**
         * Set team from HalfTeams (compatibility method).
         */
        public Builder setTeamFromHalfTeams(@Nullable HalfTeam primaryHalfTeam, @Nullable HalfTeam secondaryHalfTeam) {
            event.setTeamFromHalfTeams(primaryHalfTeam, secondaryHalfTeam);
            return this;
        }

        /**
         * Set team from HalfTeam set (compatibility method).
         */
        public Builder setTeamFromHalfTeamSet(@Nullable Set<HalfTeam> halfTeams) {
            event.setTeamFromHalfTeamSet(halfTeams);
            return this;
        }

        public Builder setShiftType(@Nullable ShiftType shiftType) {
            event.setShiftType(shiftType);
            return this;
        }

        public Builder setUserId(@Nullable Long userId) {
            event.setUserId(userId);
            return this;
        }

        public Builder setTiming(@Nullable LocalTime startTime, @Nullable LocalTime endTime) {
            event.setStartTime(startTime);
            event.setEndTime(endTime);
            return this;
        }

        public Builder setDayInCycle(int dayInCycle) {
            event.setDayInCycle(dayInCycle);
            return this;
        }

        public Builder setDaysFromSchemeStart(long daysFromSchemeStart) {
            event.setDaysFromSchemeStart(daysFromSchemeStart);
            return this;
        }

        public Builder setPatternName(@Nullable String patternName) {
            event.setPatternName(patternName);
            return this;
        }

        public Builder setGeneratedBy(@Nullable String generatedBy) {
            event.setGeneratedBy(generatedBy);
            return this;
        }

        public Builder setColorHex(@Nullable String colorHex) {
            event.setColorHex(colorHex);
            return this;
        }

        @NonNull
        public WorkScheduleEvent build() {
            // Final validation and setup
            event.updateDisplayInformation();
            event.updateSourceId();
            event.updateUserRelevance();
            return event;
        }
    }
}