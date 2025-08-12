package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * WorkScheduleEvent - Domain model for volatile work schedule events.
 *
 * <p>This is a clean architecture domain model representing a single work schedule event
 * generated from pattern calculations. These events are volatile (not persisted) and
 * generated on-demand for calendar display and integration.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Volatile</strong>: Generated on-demand, not stored in database</li>
 *   <li><strong>Pattern-Based</strong>: Derived from work schedule pattern calculations</li>
 *   <li><strong>Calendar Integration</strong>: Designed for seamless calendar display</li>
 *   <li><strong>Team-Specific</strong>: Contains team assignment and workScheduleShift information</li>
 *   <li><strong>Domain Model</strong>: No external dependencies, pure business model</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li>WorkScheduleRepository generates these events for date ranges</li>
 *   <li>SwipeCalendarFragment displays these in calendar grid</li>
 *   <li>EventsService can merge with persistent events</li>
 *   <li>CalendarDataProvider combines with LocalEvents for unified view</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
public class WorkScheduleEvent {

    // ==================== IDENTIFICATION ====================

    private String id;                           // Unique ID for this event instance
    private String sourceId;                     // Source identifier (pattern + date + workScheduleShift)

    // ==================== TEMPORAL INFORMATION ====================

    private LocalDate date;                     // Event date
    private LocalTime startTime;                // Shift start time
    private LocalTime endTime;                  // Shift end time
    private boolean crossesMidnight;            // True if workScheduleShift crosses midnight

    // ==================== WORK SCHEDULE DATA ====================

    private WorkScheduleShift workScheduleShift;            // Associated workScheduleShift from Day calculation
    private Team team;                          // Team assigned to this workScheduleShift
    private Shift shift; // Type of workScheduleShift (Morning, Afternoon, Night)

    // ==================== DISPLAY INFORMATION ====================

    private String title;                       // Display title for calendar
    private String description;                 // Detailed description
    private String colorHex;                    // Color for calendar display

    // ==================== USER CONTEXT ====================

    private Long userId;                        // Optional user ID for filtering
    private boolean isUserRelevant;             // True if event is relevant to user's team

    // ==================== PATTERN METADATA ====================

    private int dayInCycle;                     // Day position in cycle (0-based)
    private long daysFromSchemeStart;           // Days from scheme reference start
    private String patternName;                 // Pattern name (e.g., "QuattroDue 4-2")

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
        this.patternName = "QuattroDue 4-2";
    }

    /**
     * Constructor with basic work schedule information.
     *
     * @param date Event date
     * @param workScheduleShift Associated workScheduleShift data
     */
    public WorkScheduleEvent(@NonNull LocalDate date, @NonNull WorkScheduleShift workScheduleShift) {
        this();
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        setWorkScheduleShift( workScheduleShift );
    }

    /**
     * Constructor with complete information.
     *
     * @param date Event date
     * @param workScheduleShift Associated workScheduleShift data
     * @param team Team assignment
     * @param userId Optional user ID for relevance
     */
    public WorkScheduleEvent(@NonNull LocalDate date, @NonNull WorkScheduleShift workScheduleShift,
                             @Nullable Team team, @Nullable Long userId) {
        this(date, workScheduleShift );
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
    public WorkScheduleShift getWorkScheduleShift() {
        return workScheduleShift;
    }

    @Nullable
    public Team getTeam() {
        return team;
    }

    @Nullable
    public Shift getShift() {
        return shift;
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
        this.id = Objects.requireNonNull(id, "ID cannot be null");
    }

    public void setSourceId(@Nullable String sourceId) {
        this.sourceId = sourceId;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
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

    public void setWorkScheduleShift(@Nullable WorkScheduleShift workScheduleShift) {
        this.workScheduleShift = workScheduleShift;

        if (workScheduleShift != null) {
            // Extract information from workScheduleShift
            this.shift = workScheduleShift.getShift();

            // Extract timing if available
            this.startTime = workScheduleShift.getStartTime();
            this.endTime = workScheduleShift.getEndTime();
            updateCrossesMidnight();

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

    public void setShift(@Nullable Shift shift) {
        this.shift = shift;

        if (shift != null) {
            // Note: ShiftType timing will be handled by database ShiftType entities
            updateDisplayInformation();
        }

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
     * Check if this event has workScheduleShift type information.
     *
     * @return true if workScheduleShift type is available
     */
    public boolean hasShiftType() {
        return shift != null;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return String.format("%02d:%02d-%02d:%02d",
                    startTime.getHour(), startTime.getMinute(),
                    endTime.getHour(), endTime.getMinute());
        }
        return "";
    }

    /**
     * Get display name for UI.
     *
     * @return Display name with type and time
     */
    @NonNull
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();

        if (shift != null) {
            sb.append( shift.getDisplayName());
        } else {
            sb.append("Work Shift");
        }

        String timeRange = getTimeRange();
        if (!timeRange.isEmpty()) {
            sb.append(" (").append(timeRange).append(")");
        }

        return sb.toString();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing WorkScheduleEvent instances.
     *
     * @param date Event date (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull LocalDate date) {
        return new Builder(date);
    }

    /**
     * Create a builder from existing WorkScheduleEvent.
     *
     * @return Builder instance with current event data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class for creating WorkScheduleEvent instances.
     */
    public static class Builder {
        private final WorkScheduleEvent event;

        private Builder(@NonNull LocalDate date) {
            this.event = new WorkScheduleEvent();
            this.event.setDate(date);
        }

        private Builder(@NonNull WorkScheduleEvent existingEvent) {
            this.event = new WorkScheduleEvent();
            this.event.date = existingEvent.date;
            this.event.workScheduleShift = existingEvent.workScheduleShift;
            this.event.team = existingEvent.team;
            this.event.shift = existingEvent.shift;
            this.event.startTime = existingEvent.startTime;
            this.event.endTime = existingEvent.endTime;
            this.event.userId = existingEvent.userId;
            this.event.dayInCycle = existingEvent.dayInCycle;
            this.event.daysFromSchemeStart = existingEvent.daysFromSchemeStart;
            this.event.patternName = existingEvent.patternName;
            this.event.colorHex = existingEvent.colorHex;
            this.event.title = existingEvent.title;
            this.event.description = existingEvent.description;
            this.event.generatedBy = existingEvent.generatedBy;
        }

        /**
         * Set work schedule workScheduleShift.
         *
         * @param shift WorkScheduleShift to associate
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setShift(@Nullable WorkScheduleShift shift) {
            event.setWorkScheduleShift(shift);
            return this;
        }

        /**
         * Set team assignment.
         *
         * @param team Team to assign
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setTeam(@Nullable Team team) {
            event.setTeam(team);
            return this;
        }

        /**
         * Set workScheduleShift type.
         *
         * @param shift Type of workScheduleShift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setShiftType(@Nullable Shift shift) {
            event.setShift(shift);
            return this;
        }

        /**
         * Set user context.
         *
         * @param userId User ID for relevance checking
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setUserId(@Nullable Long userId) {
            event.setUserId(userId);
            return this;
        }

        /**
         * Set timing information.
         *
         * @param startTime Start time of the workScheduleShift
         * @param endTime End time of the workScheduleShift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setTiming(@Nullable LocalTime startTime, @Nullable LocalTime endTime) {
            event.setStartTime(startTime);
            event.setEndTime(endTime);
            return this;
        }

        /**
         * Set pattern metadata.
         *
         * @param dayInCycle Day position in pattern cycle
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setDayInCycle(int dayInCycle) {
            event.setDayInCycle(dayInCycle);
            return this;
        }

        /**
         * Set days from scheme start.
         *
         * @param daysFromSchemeStart Days from pattern reference start
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setDaysFromSchemeStart(long daysFromSchemeStart) {
            event.setDaysFromSchemeStart(daysFromSchemeStart);
            return this;
        }

        /**
         * Set pattern name.
         *
         * @param patternName Name of the work pattern
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setPatternName(@Nullable String patternName) {
            event.setPatternName(patternName);
            return this;
        }

        /**
         * Set generation source.
         *
         * @param generatedBy Component that generated this event
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setGeneratedBy(@Nullable String generatedBy) {
            event.setGeneratedBy(generatedBy);
            return this;
        }

        /**
         * Set display color.
         *
         * @param colorHex Hex color code for calendar display
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setColorHex(@Nullable String colorHex) {
            event.setColorHex(colorHex);
            return this;
        }

        /**
         * Build the WorkScheduleEvent instance.
         *
         * @return New WorkScheduleEvent instance
         */
        @NonNull
        public WorkScheduleEvent build() {
            // Final validation and setup
            event.updateDisplayInformation();
            event.updateSourceId();
            event.updateUserRelevance();
            return event;
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
        if (shift != null) {
            titleBuilder.append( shift.getDisplayName());
        } else {
            titleBuilder.append("Work Shift");
        }

        if (team != null) {
            titleBuilder.append(" - Team ").append(team.getName());
        }

        // Build description
        if (hasTimingInfo()) {
            descBuilder.append("Time: ").append(getTimeRange());

            if (crossesMidnight) {
                descBuilder.append(" (+1 day)");
            }
        }

        if (team != null && descBuilder.length() > 0) {
            descBuilder.append("\n");
        }

        if (team != null) {
            descBuilder.append("Team: ").append(team.getDisplayName());
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
                sourceBuilder.append("_").append(team.getName());
            }

            if (shift != null) {
                sourceBuilder.append("_").append( shift.getName());
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
                        Objects.equals( shift, that.shift ));
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId != null ? sourceId :
                Objects.hash(date, team, shift ));
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleEvent{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", title='" + title + '\'' +
                ", team=" + (team != null ? team.getName() : "null") +
                ", shift=" + (shift != null ? shift.getName() : "null") +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", dayInCycle=" + dayInCycle +
                ", isUserRelevant=" + isUserRelevant +
                '}';
    }

    /**
     * Get detailed string representation for debugging.
     *
     * @return Detailed string with all event information
     */
    @NonNull
    public String toDetailedString() {
        return "WorkScheduleEvent{" +
                "id='" + id + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", date=" + date +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", crossesMidnight=" + crossesMidnight +
                ", workScheduleShift=" + workScheduleShift +
                ", team=" + team +
                ", shift=" + shift +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", userId=" + userId +
                ", isUserRelevant=" + isUserRelevant +
                ", dayInCycle=" + dayInCycle +
                ", daysFromSchemeStart=" + daysFromSchemeStart +
                ", patternName='" + patternName + '\'' +
                ", generatedTimestamp=" + generatedTimestamp +
                ", generatedBy='" + generatedBy + '\'' +
                ", isVolatile=" + isVolatile +
                '}';
    }
}