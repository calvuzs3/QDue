package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WorkScheduleEvent - Domain model for volatile work schedule events with multi-team support.
 *
 * <p>This is a clean architecture domain model representing a single work schedule event
 * generated from pattern calculations. These events are volatile (not persisted) and
 * generated on-demand for calendar display and integration with full localization support.
 * Now supports multiple teams for complex schedule patterns.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Volatile</strong>: Generated on-demand, not stored in database</li>
 *   <li><strong>Pattern-Based</strong>: Derived from work schedule pattern calculations</li>
 *   <li><strong>Calendar Integration</strong>: Designed for seamless calendar display</li>
 *   <li><strong>Multi-Team Support</strong>: Contains multiple team assignments per event</li>
 *   <li><strong>Domain Model</strong>: No external dependencies, pure business model</li>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state</li>
 *   <li><strong>Localization Support</strong>: Full i18n support for all display text</li>
 * </ul>
 *
 * <h3>Event Types:</h3>
 * <ul>
 *   <li><strong>SHIFT_EVENT</strong>: Regular work shift event</li>
 *   <li><strong>BREAK_EVENT</strong>: Break or rest period event</li>
 *   <li><strong>OVERTIME_EVENT</strong>: Overtime work event</li>
 *   <li><strong>SPECIAL_EVENT</strong>: Special or custom event</li>
 * </ul>
 *
 * <h3>Multi-Team Support:</h3>
 * <ul>
 *   <li><strong>4-2 Pattern</strong>: Typically 2 teams per event</li>
 *   <li><strong>3-2 Pattern</strong>: Can have different team configurations</li>
 *   <li><strong>Custom Patterns</strong>: Flexible team assignment support</li>
 *   <li><strong>User Relevance</strong>: User is relevant if in ANY assigned team</li>
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
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // Create multi-team work schedule event
 * List<Team> teams = Arrays.asList(teamA, teamB);
 * WorkScheduleEvent event = WorkScheduleEvent.builder(LocalDate.now())
 *     .setWorkScheduleShift(morningShift)
 *     .setTeams(teams)
 *     .setShift(morningShiftType)
 *     .setUserId(123L)
 *     .setEventType(EventType.SHIFT_EVENT)
 *     .localizer(domainLocalizer)
 *     .build();
 *
 * // Get localized display
 * String displayName = event.getLocalizedDisplayName();
 * String teamSummary = event.getLocalizedTeamAssignments();
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.1.0 - Multi-Team Support Implementation
 * @since Clean Architecture Phase 2
 */
public class WorkScheduleEvent extends LocalizableDomainModel {

    private static final String LOCALIZATION_SCOPE = "work_event";

    // ==================== EVENT TYPE ENUM ====================

    /**
     * Enumeration of work schedule event types with localization keys.
     */
    public enum EventType {
        SHIFT_EVENT("shift_event", "regular_work_shift_event"),
        BREAK_EVENT("break_event", "break_or_rest_period_event"),
        OVERTIME_EVENT("overtime_event", "overtime_work_event"),
        SPECIAL_EVENT("special_event", "special_or_custom_event");

        private final String displayNameKey;
        private final String descriptionKey;

        EventType(String displayNameKey, String descriptionKey) {
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

    // ==================== IDENTIFICATION ====================

    private final String id;                           // Unique ID for this event instance
    private final String sourceId;                     // Source identifier (pattern + date + shifts + teams)

    // ==================== TEMPORAL INFORMATION ====================

    private final LocalDate date;                     // Event date
    private final LocalTime startTime;                // Shift start time
    private final LocalTime endTime;                  // Shift end time
    private final boolean crossesMidnight;            // True if shift crosses midnight

    // ==================== WORK SCHEDULE DATA ====================

    private final WorkScheduleShift workScheduleShift;            // Associated shift from Day calculation
    private final List<Team> teams;                  // Teams assigned to this shift (immutable)
    private final Shift shift;                        // Type of shift (Morning, Afternoon, Night)
    private final EventType eventType;               // Type of event

    // ==================== DISPLAY INFORMATION ====================

    private final String title;                       // Display title for calendar
    private final String description;                 // Detailed description
    private final String colorHex;                    // Color for calendar display

    // ==================== USER CONTEXT ====================

    private final String userId;                        // Optional user ID for filtering
    private final boolean isUserRelevant;             // True if event is relevant to user's team

    // ==================== PATTERN METADATA ====================

    private final int dayInCycle;                     // Day position in cycle (0-based)
    private final long daysFromSchemeStart;           // Days from scheme reference start
    private final String patternName;                 // Pattern name (e.g., "QuattroDue 4-2")

    // ==================== GENERATION METADATA ====================

    private final long generatedTimestamp;            // When this event was generated
    private final String generatedBy;                 // Service/component that generated event
    private final boolean isVolatile;                 // Always true for WorkScheduleEvent

    // ==================== CONSTRUCTORS ====================

    /**
     * Private constructor for builder pattern with localization support.
     */
    private WorkScheduleEvent(@NonNull Builder builder) {
        super(builder.mLocalizer, LOCALIZATION_SCOPE);

        // Identification
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.sourceId = generateSourceId(builder.date, builder.teams, builder.shift, builder.dayInCycle);

        // Temporal information
        this.date = Objects.requireNonNull(builder.date, "Date cannot be null");
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.crossesMidnight = calculateCrossesMidnight(builder.startTime, builder.endTime);

        // Work schedule data
        this.workScheduleShift = builder.workScheduleShift;
        this.teams = builder.teams != null ?
                Collections.unmodifiableList(new ArrayList<>(builder.teams)) :
                Collections.emptyList();
        this.shift = builder.shift;
        this.eventType = builder.eventType != null ? builder.eventType : EventType.SHIFT_EVENT;

        // Display information
        this.title = builder.title != null ? builder.title : generateDefaultTitle();
        this.description = builder.description != null ? builder.description : generateDefaultDescription();
        this.colorHex = builder.colorHex != null ? builder.colorHex : getDefaultColor();

        // User context
        this.userId = builder.userId;
        this.isUserRelevant = calculateUserRelevance(builder.userId, this.teams);

        // Pattern metadata
        this.dayInCycle = builder.dayInCycle;
        this.daysFromSchemeStart = builder.daysFromSchemeStart;
        this.patternName = builder.patternName != null ? builder.patternName : "QuattroDue 4-2";

        // Generation metadata
        this.generatedTimestamp = builder.generatedTimestamp > 0 ? builder.generatedTimestamp : System.currentTimeMillis();
        this.generatedBy = builder.generatedBy;
        this.isVolatile = true; // Always true for WorkScheduleEvent
    }

    // ==================== GETTERS ====================

    @NonNull public String getId() { return id; }
    @NonNull public String getSourceId() { return sourceId; }
    @NonNull public LocalDate getDate() { return date; }
    @Nullable public LocalTime getStartTime() { return startTime; }
    @Nullable public LocalTime getEndTime() { return endTime; }
    public boolean crossesMidnight() { return crossesMidnight; }
    @Nullable public WorkScheduleShift getWorkScheduleShift() { return workScheduleShift; }
    @NonNull public List<Team> getTeams() { return teams; }
    @Nullable public Shift getShift() { return shift; }
    @NonNull public EventType getEventType() { return eventType; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getDescription() { return description; }
    @NonNull public String getColorHex() { return colorHex; }
    @Nullable public String getUserId() { return userId; }
    public boolean isUserRelevant() { return isUserRelevant; }
    public int getDayInCycle() { return dayInCycle; }
    public long getDaysFromSchemeStart() { return daysFromSchemeStart; }
    @NonNull public String getPatternName() { return patternName; }
    public long getGeneratedTimestamp() { return generatedTimestamp; }
    @Nullable public String getGeneratedBy() { return generatedBy; }
    public boolean isVolatile() { return isVolatile; }

    // ==================== TEAM-RELATED GETTERS ====================

    /**
     * Get the primary team (first in the list).
     * Useful for backward compatibility or when you need a single team reference.
     *
     * @return Primary team, null if no teams assigned
     */
    @Nullable
    public Team getPrimaryTeam() {
        return teams.isEmpty() ? null : teams.get(0);
    }

    /**
     * Get count of assigned teams.
     *
     * @return Number of teams assigned to this event
     */
    public int getTeamCount() {
        return teams.size();
    }

    /**
     * Get team names as a list.
     *
     * @return List of team names
     */
    @NonNull
    public List<String> getTeamNames() {
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get team names as comma-separated string.
     *
     * @return Comma-separated team names (e.g., "Team A, Team B")
     */
    @NonNull
    public String getTeamNamesString() {
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if specific team is assigned to this event.
     *
     * @param team Team to check
     * @return true if team is assigned
     */
    public boolean hasTeam(@NonNull Team team) {
        return teams.contains(team);
    }

    /**
     * Check if user is assigned to any of the teams.
     *
     * @param userId User ID to check
     * @return true if user is in any assigned team
     */
    public boolean isUserInTeams(String userId) {
        // This would need integration with user service to check team membership
        // For now, using simple logic based on current user context
        return isUserRelevant && this.userId != null && this.userId == userId;
    }

    // ==================== COMPUTED PROPERTIES ====================

    /**
     * Get full start date/time combining date and start time.
     *
     * @return LocalDateTime for event start, null if no start time
     */
    @Nullable
    public LocalDateTime getStartDateTime() {
        if (startTime != null) {
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
        if (endTime != null) {
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
     * Check if this event has team assignments.
     *
     * @return true if one or more teams are assigned
     */
    public boolean hasTeamAssignments() {
        return !teams.isEmpty();
    }

    /**
     * Check if this event has multiple team assignments.
     *
     * @return true if more than one team is assigned
     */
    public boolean hasMultipleTeams() {
        return teams.size() > 1;
    }

    /**
     * Check if this event has shift type information.
     *
     * @return true if shift type is available
     */
    public boolean hasShiftType() {
        return shift != null;
    }

    /**
     * Check if this is a shift event.
     *
     * @return true if event type is SHIFT_EVENT
     */
    public boolean isShiftEvent() {
        return eventType == EventType.SHIFT_EVENT;
    }

    /**
     * Check if this is a break event.
     *
     * @return true if event type is BREAK_EVENT
     */
    public boolean isBreakEvent() {
        return eventType == EventType.BREAK_EVENT;
    }

    /**
     * Check if this is an overtime event.
     *
     * @return true if event type is OVERTIME_EVENT
     */
    public boolean isOvertimeEvent() {
        return eventType == EventType.OVERTIME_EVENT;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time range for display.
     *
     * @return Time range string (e.g., "06:00-14:00")
     */
    @NonNull
    public String getTimeRange() {
        if (hasTimingInfo()) {
            return String.format("%02d:%02d-%02d:%02d",
                    startTime.getHour(), startTime.getMinute(),
                    endTime.getHour(), endTime.getMinute());
        }
        return "";
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized event type display name.
     */
    @NonNull
    public String getTypeDisplayName() {
        return localize("type." + eventType.name().toLowerCase(),
                eventType.name(), // fallback
                eventType.name());
    }

    /**
     * Get localized event type description.
     */
    @NonNull
    public String getTypeDescription() {
        return localize("type." + eventType.name().toLowerCase() + ".description",
                eventType.getDescriptionKey(), // fallback
                eventType.name());
    }

    /**
     * Get localized display name for UI.
     *
     * @return Localized display name with type and time
     */
    @NonNull
    public String getLocalizedDisplayName() {
        StringBuilder sb = new StringBuilder();

        // Use shift's localized display name if available
        if (shift != null) {
            if (shift.hasLocalizationSupport()) {
                sb.append(shift.getDisplayName());
            } else {
                sb.append(shift.getName());
            }
        } else {
            sb.append(getTypeDisplayName());
        }

        // Add time range if available
        String timeRange = getLocalizedTimeRange();
        if (!timeRange.isEmpty()) {
            sb.append(" (").append(timeRange).append(")");
        }

        return sb.toString();
    }

    /**
     * Get localized time range with proper formatting.
     *
     * @return Localized time range
     */
    @NonNull
    public String getLocalizedTimeRange() {
        if (!hasTimingInfo()) {
            return localize("time.no_timing", "No timing", "No timing");
        }

        String timeRange = getTimeRange();

        if (crossesMidnight) {
            String midnightIndicator = localize("time.midnight_indicator", "+1", "+1");
            return timeRange + " " + midnightIndicator;
        }

        return timeRange;
    }

    /**
     * Get localized duration description.
     *
     * @return Localized duration (e.g., "8 hours 30 minutes")
     */
    @NonNull
    public String getLocalizedDuration() {
        if (!hasTimingInfo()) {
            return localize("duration.unknown", "Unknown duration", "Unknown duration");
        }

        long totalMinutes = getDurationMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            if (minutes == 1) {
                return localize("duration.one_minute", "1 minute", "1 minute");
            } else {
                return localize("duration.minutes", "{0} minutes", "{0} minutes", minutes);
            }
        } else if (minutes == 0) {
            if (hours == 1) {
                return localize("duration.one_hour", "1 hour", "1 hour");
            } else {
                return localize("duration.hours", "{0} hours", "{0} hours", hours);
            }
        } else {
            return localize("duration.hours_minutes", "{0}h {1}m", "{0}h {1}m", hours, minutes);
        }
    }

    /**
     * Get localized team assignments description.
     *
     * @return Localized team assignments
     */
    @NonNull
    public String getLocalizedTeamAssignments() {
        if (!hasTeamAssignments()) {
            return localize("teams.no_assignment", "No team assignment", "No team assignment");
        }

        if (teams.size() == 1) {
            String teamLabel = localize("teams.single_label", "Team", "Team");
            Team team = teams.get(0);
            String teamName = team.hasLocalizationSupport() ? team.getDisplayName() : team.getName();
            return teamLabel + ": " + teamName;
        } else {
            String teamsLabel = localize("teams.multiple_label", "Teams", "Teams");
            List<String> teamNames = teams.stream()
                    .map(team -> team.hasLocalizationSupport() ? team.getDisplayName() : team.getName())
                    .collect(Collectors.toList());

            String separator = localize("format.list_separator", ", ", ", ");
            String lastSeparator = localize("format.list_last_separator", " and ", " and ");

            if (teamNames.size() == 2) {
                return teamsLabel + ": " + teamNames.get(0) + lastSeparator + teamNames.get(1);
            } else {
                String joined = String.join(separator, teamNames.subList(0, teamNames.size() - 1));
                return teamsLabel + ": " + joined + lastSeparator + teamNames.get(teamNames.size() - 1);
            }
        }
    }

    /**
     * Get localized summary for the event.
     *
     * @return Localized event summary
     */
    @NonNull
    public String getLocalizedSummary() {
        StringBuilder summary = new StringBuilder();

        // Event name and time
        summary.append(getLocalizedDisplayName());

        // Team assignments if available
        if (hasTeamAssignments()) {
            String separator = localize("format.summary_separator", " - ", " - ");
            summary.append(separator);

            if (teams.size() == 1) {
                Team team = teams.get(0);
                String teamName = team.hasLocalizationSupport() ? team.getDisplayName() : team.getName();
                summary.append(teamName);
            } else {
                String teamCount = localize("format.team_count", "{0} teams", "{0} teams", teams.size());
                summary.append(teamCount);
            }
        }

        // Event type if not standard shift
        if (eventType != EventType.SHIFT_EVENT) {
            String typeFormat = localize("format.type_brackets", " [{0}]", " [{0}]");
            summary.append(String.format(typeFormat.replace("{0}", "%s"), getTypeDisplayName()));
        }

        return summary.toString();
    }

    /**
     * Get localized detailed summary for the event.
     *
     * @return Localized detailed summary
     */
    @NonNull
    public String getLocalizedDetailedSummary() {
        StringBuilder summary = new StringBuilder();

        // Basic summary
        summary.append(getLocalizedSummary()).append("\n");

        // Duration if available
        if (hasTimingInfo()) {
            String durationLabel = localize("label.duration", "Duration", "Duration");
            summary.append(durationLabel).append(": ").append(getLocalizedDuration()).append("\n");
        }

        // Team assignments detail
        if (hasTeamAssignments()) {
            summary.append(getLocalizedTeamAssignments()).append("\n");
        }

        // Pattern information
        String patternLabel = localize("label.pattern", "Pattern", "Pattern");
        summary.append(patternLabel).append(": ").append(patternName);

        String dayLabel = localize("label.day_in_cycle", " (Day {0})", " (Day {0})");
        summary.append(String.format(dayLabel.replace("{0}", "%d"), dayInCycle + 1)); // +1 for 1-based display

        return summary.toString().trim();
    }

    /**
     * Get localized event card information for detailed display.
     *
     * @return Localized event card content
     */
    @NonNull
    public String getLocalizedEventCard() {
        StringBuilder card = new StringBuilder();

        // Title
        card.append(getLocalizedDisplayName()).append("\n");

        // Type
        String typeLabel = localize("label.type", "Type", "Type");
        card.append(typeLabel).append(": ").append(getTypeDisplayName()).append("\n");

        // Timing
        if (hasTimingInfo()) {
            String timeLabel = localize("label.time", "Time", "Time");
            card.append(timeLabel).append(": ").append(getLocalizedTimeRange()).append("\n");

            String durationLabel = localize("label.duration", "Duration", "Duration");
            card.append(durationLabel).append(": ").append(getLocalizedDuration()).append("\n");
        }

        // Teams
        if (hasTeamAssignments()) {
            card.append(getLocalizedTeamAssignments()).append("\n");
        }

        // Pattern
        String patternLabel = localize("label.pattern", "Pattern", "Pattern");
        card.append(patternLabel).append(": ").append(patternName);

        return card.toString().trim();
    }

    // ==================== NON-LOCALIZED DISPLAY METHODS (Legacy Support) ====================

    /**
     * Get display name for UI (non-localized version for backward compatibility).
     * Consider using getLocalizedDisplayName() for new code.
     *
     * @return Display name with type and time
     */
    @NonNull
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();

        if (shift != null) {
            sb.append(shift.getDisplayName());
        } else {
            sb.append("Work Event");
        }

        String timeRange = getTimeRange();
        if (!timeRange.isEmpty()) {
            sb.append(" (").append(timeRange).append(")");
        }

        return sb.toString();
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a shift event with multiple teams and localization support.
     *
     * @param date Event date
     * @param shift Work shift
     * @param teams List of team assignments
     * @param localizer Optional domain localizer for i18n
     * @return Shift event
     */
    @NonNull
    public static WorkScheduleEvent createShiftEvent(@NonNull LocalDate date, @NonNull Shift shift,
                                                     @NonNull List<Team> teams, @Nullable DomainLocalizer localizer) {
        return builder(date)
                .setShift(shift)
                .setTeams(teams)
                .setEventType(EventType.SHIFT_EVENT)
                .localizer(localizer)
                .build();
    }

    /**
     * Create a shift event with single team and localization support.
     *
     * @param date Event date
     * @param shift Work shift
     * @param team Team assignment
     * @param localizer Optional domain localizer for i18n
     * @return Shift event
     */
    @NonNull
    public static WorkScheduleEvent createShiftEvent(@NonNull LocalDate date, @NonNull Shift shift,
                                                     @NonNull Team team, @Nullable DomainLocalizer localizer) {
        return createShiftEvent(date, shift, Collections.singletonList(team), localizer);
    }

    /**
     * Create a break event with localization support.
     *
     * @param date Event date
     * @param startTime Break start time
     * @param endTime Break end time
     * @param localizer Optional domain localizer for i18n
     * @return Break event
     */
    @NonNull
    public static WorkScheduleEvent createBreakEvent(@NonNull LocalDate date, @NonNull LocalTime startTime,
                                                     @NonNull LocalTime endTime, @Nullable DomainLocalizer localizer) {
        return builder(date)
                .setTiming(startTime, endTime)
                .setEventType(EventType.BREAK_EVENT)
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
    public WorkScheduleEvent withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder(this.date)
                .copyFrom(this)
                .localizer(localizer)
                .build();
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
     * Builder class for creating WorkScheduleEvent instances with localization support.
     */
    public static class Builder extends LocalizableBuilder<WorkScheduleEvent, Builder> {
        private String id;
        private final LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean crossesMidnight;
        private WorkScheduleShift workScheduleShift;
        private List<Team> teams;
        private Shift shift;
        private EventType eventType;
        private String title;
        private String description;
        private String colorHex;
        private String userId;
        private int dayInCycle;
        private long daysFromSchemeStart;
        private String patternName;
        private long generatedTimestamp;
        private String generatedBy;

        private Builder(@NonNull LocalDate date) {
            this.date = Objects.requireNonNull(date, "Date cannot be null");
            this.teams = new ArrayList<>();
        }

        private Builder(@NonNull WorkScheduleEvent existingEvent) {
            this.id = existingEvent.id;
            this.date = existingEvent.date;
            this.startTime = existingEvent.startTime;
            this.endTime = existingEvent.endTime;
            this.crossesMidnight = existingEvent.crossesMidnight;
            this.workScheduleShift = existingEvent.workScheduleShift;
            this.teams = new ArrayList<>(existingEvent.teams);
            this.shift = existingEvent.shift;
            this.eventType = existingEvent.eventType;
            this.title = existingEvent.title;
            this.description = existingEvent.description;
            this.colorHex = existingEvent.colorHex;
            this.userId = existingEvent.userId;
            this.dayInCycle = existingEvent.dayInCycle;
            this.daysFromSchemeStart = existingEvent.daysFromSchemeStart;
            this.patternName = existingEvent.patternName;
            this.generatedTimestamp = existingEvent.generatedTimestamp;
            this.generatedBy = existingEvent.generatedBy;
        }

        /**
         * Copy data from existing WorkScheduleEvent (for withLocalizer implementation).
         */
        @NonNull
        public Builder copyFrom(@NonNull WorkScheduleEvent source) {
            this.id = source.id;
            this.startTime = source.startTime;
            this.endTime = source.endTime;
            this.crossesMidnight = source.crossesMidnight;
            this.workScheduleShift = source.workScheduleShift;
            this.teams = new ArrayList<>(source.teams);
            this.shift = source.shift;
            this.eventType = source.eventType;
            this.title = source.title;
            this.description = source.description;
            this.colorHex = source.colorHex;
            this.userId = source.userId;
            this.dayInCycle = source.dayInCycle;
            this.daysFromSchemeStart = source.daysFromSchemeStart;
            this.patternName = source.patternName;
            this.generatedTimestamp = source.generatedTimestamp;
            this.generatedBy = source.generatedBy;

            return copyLocalizableFrom(source);
        }

        @NonNull public Builder setId(@Nullable String id) { this.id = id; return this; }
        @NonNull public Builder setWorkScheduleShift(@Nullable WorkScheduleShift shift) { this.workScheduleShift = shift; return this; }
        @NonNull public Builder setEventType(@Nullable EventType eventType) { this.eventType = eventType; return this; }
        @NonNull public Builder setUserId(@Nullable String userId) { this.userId = userId; return this; }
        @NonNull public Builder setTitle(@Nullable String title) { this.title = title; return this; }
        @NonNull public Builder setDescription(@Nullable String description) { this.description = description; return this; }
        @NonNull public Builder setColorHex(@Nullable String colorHex) { this.colorHex = colorHex; return this; }
        @NonNull public Builder setDayInCycle(int dayInCycle) { this.dayInCycle = dayInCycle; return this; }
        @NonNull public Builder setDaysFromSchemeStart(long daysFromSchemeStart) { this.daysFromSchemeStart = daysFromSchemeStart; return this; }
        @NonNull public Builder setPatternName(@Nullable String patternName) { this.patternName = patternName; return this; }
        @NonNull public Builder setGeneratedBy(@Nullable String generatedBy) { this.generatedBy = generatedBy; return this; }

        /**
         * Set the shift and automatically extract timing information.
         *
         * @param shift Work shift
         * @return Builder instance for chaining
         */
        @NonNull public Builder setShift(@Nullable Shift shift) {
            this.shift = shift;
            if (shift != null) {
                setTiming(shift.getStartTime(), shift.getEndTime());
            }
            return this;
        }

        /**
         * Set teams list (replaces existing teams).
         *
         * @param teams List of teams to assign
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setTeams(@Nullable List<Team> teams) {
            this.teams.clear();
            if (teams != null) {
                this.teams.addAll(teams);
            }
            return this;
        }

        /**
         * Add a single team to the assignments.
         *
         * @param team Team to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeam(@NonNull Team team) {
            if (!this.teams.contains(team)) {
                this.teams.add(team);
            }
            return this;
        }

        /**
         * Add multiple teams to the assignments.
         *
         * @param teams Teams to add
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder addTeams(@NonNull List<Team> teams) {
            for (Team team : teams) {
                addTeam(team);
            }
            return this;
        }

        /**
         * Clear all team assignments.
         *
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder clearTeams() {
            this.teams.clear();
            return this;
        }

        /**
         * Set timing information.
         *
         * @param startTime Start time of the shift
         * @param endTime End time of the shift
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder setTiming(@Nullable LocalTime startTime, @Nullable LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            if (startTime != null && endTime != null) {
                this.crossesMidnight = calculateCrossesMidnight(startTime, endTime);
            }
            return this;
        }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        /**
         * Build the WorkScheduleEvent instance.
         *
         * @return New WorkScheduleEvent instance
         */
        @Override
        @NonNull
        public WorkScheduleEvent build() {
            return new WorkScheduleEvent(this);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate source ID for tracking and deduplication.
     */
    @NonNull
    private static String generateSourceId(@NonNull LocalDate date, @Nullable List<Team> teams,
                                           @Nullable Shift shift, int dayInCycle) {
        StringBuilder sourceBuilder = new StringBuilder();
        sourceBuilder.append("WE_").append(date.toString());

        if (teams != null && !teams.isEmpty()) {
            sourceBuilder.append("_T[");
            String teamNames = teams.stream()
                    .map(Team::getName)
                    .sorted()
                    .collect(Collectors.joining(","));
            sourceBuilder.append(teamNames);
            sourceBuilder.append("]");
        }

        if (shift != null) {
            sourceBuilder.append("_").append(shift.getName());
        }

        sourceBuilder.append("_").append(dayInCycle);

        return sourceBuilder.toString();
    }

    /**
     * Calculate if shift crosses midnight.
     */
    private static boolean calculateCrossesMidnight(@Nullable LocalTime startTime, @Nullable LocalTime endTime) {
        return startTime != null && endTime != null && endTime.isBefore(startTime);
    }

    /**
     * Calculate user relevance based on team assignments and user context.
     * User is relevant if they are in ANY of the assigned teams.
     */
    private static boolean calculateUserRelevance(@Nullable String userId, @NonNull List<Team> teams) {
        // Simple logic - can be enhanced with user service integration
        // User is relevant if userID is provided and there are team assignments
        return userId != null && !teams.isEmpty();
    }

    /**
     * Generate default title based on available data.
     */
    @NonNull
    private String generateDefaultTitle() {
        if (shift != null) {
            return shift.getName();
        } else if (eventType != null) {
            return eventType.name();
        }
        return "Work Event";
    }

    /**
     * Generate default description based on available data.
     */
    @NonNull
    private String generateDefaultDescription() {
        StringBuilder desc = new StringBuilder();

        if (hasTimingInfo()) {
            desc.append("Time: ").append(getTimeRange());
            if (crossesMidnight) {
                desc.append(" (+1 day)");
            }
        }

        if (hasTeamAssignments()) {
            if (desc.length() > 0) desc.append("\n");
            if (teams.size() == 1) {
                desc.append("Team: ").append(teams.get(0).getName());
            } else {
                desc.append("Teams: ").append(getTeamNamesString());
            }
        }

        return desc.toString();
    }

    /**
     * Get default color based on event type and shift.
     */
    @NonNull
    private String getDefaultColor() {
        if (shift != null && shift.getColorHex() != null) {
            return shift.getColorHex();
        }

        switch (eventType) {
            case BREAK_EVENT: return "#FFC107"; // Amber
            case OVERTIME_EVENT: return "#F44336"; // Red
            case SPECIAL_EVENT: return "#9C27B0"; // Purple
            default: return "#2196F3"; // Blue
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
                        Objects.equals(teams, that.teams) &&
                        Objects.equals(shift, that.shift) &&
                        dayInCycle == that.dayInCycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, date, teams, shift, dayInCycle);
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkScheduleEvent{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", title='" + title + '\'' +
                ", teams=" + getTeamNamesString() +
                ", shift=" + (shift != null ? shift.getName() : "null") +
                ", eventType=" + eventType +
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
                ", teams=" + teams +
                ", teamCount=" + getTeamCount() +
                ", shift=" + shift +
                ", eventType=" + eventType +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", userID=" + userId +
                ", isUserRelevant=" + isUserRelevant +
                ", dayInCycle=" + dayInCycle +
                ", daysFromSchemeStart=" + daysFromSchemeStart +
                ", patternName='" + patternName + '\'' +
                ", generatedTimestamp=" + generatedTimestamp +
                ", generatedBy='" + generatedBy + '\'' +
                ", isVolatile=" + isVolatile +
                ", hasLocalizationSupport=" + hasLocalizationSupport() +
                '}';
    }
}