package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * UserScheduleAssignment - Domain model for user-to-team schedule assignments.
 *
 * <p>Represents the assignment of a user to a specific team and recurrence pattern,
 * enabling multi-user schedule management and team-based scheduling. Supports
 * time-bounded assignments and team changes over time.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>User-Team Mapping</strong>: Links users to work teams</li>
 *   <li><strong>Time Boundaries</strong>: Support for start/end dates of assignments</li>
 *   <li><strong>Recurrence Integration</strong>: Links to RecurrenceRule for schedule patterns</li>
 *   <li><strong>Priority System</strong>: Handle conflicting assignments</li>
 *   <li><strong>Multi-Team Support</strong>: Users can be assigned to multiple teams over time</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.1.0 - i18n Integration
 * @since Clean Architecture Phase 2
 */
public class UserScheduleAssignment extends LocalizableDomainModel {

    private static final String TAG = "UserScheduleAssignment";

    private static final String LOCALIZATION_SCOPE = "user_assignment";

    // ==================== ENUMS ====================

    /**
     * Priority level for resolving conflicting assignments.
     */
    public enum Priority {
        LOW( 1, "low_priority" ),
        NORMAL( 5, "normal_priority" ),
        HIGH( 8, "high_priority" ),
        OVERRIDE( 10, "override_priority" );

        private final int level;
        private final String displayNameKey;

        Priority(int level, String displayNameKey) {
            this.level = level;
            this.displayNameKey = displayNameKey;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayNameKey() {
            return displayNameKey;
        }
    }

    /**
     * Status of the assignment.
     */
    public enum Status {
        ACTIVE( "active", "currently_active_assignment" ),
        PENDING( "pending", "future_assignment_not_yet_active" ),
        EXPIRED( "expired", "past_assignment_no_longer_active" ),
        SUSPENDED( "suspended", "temporarily_suspended" ),
        CANCELLED( "cancelled", "cancelled_assignment" );

        private final String displayNameKey;
        private final String descriptionKey;

        Status(String displayNameKey, String descriptionKey) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
        }

        public String getDisplayNameKey() {
            return displayNameKey;
        }

        public String getDescriptionKey() {
            return descriptionKey;
        }
    }

    // ==================== IDENTIFICATION ====================

    private final String id;
    private final String title;
    private final String description;
    private final String notes;

    // ==================== CORE ASSIGNMENT DATA ====================

    private final Long userId;              // User being assigned
    private final String userName;          // Display name for UI
    private final String teamId;            // Team assignment
    private final String teamName;          // Team display name
    private final String recurrenceRuleId;  // Schedule pattern

    // ==================== TIME BOUNDARIES ====================

    private final LocalDate startDate;     // Assignment start (inclusive)
    private final LocalDate endDate;       // Assignment end (inclusive, null = permanent)
    private final boolean isPermanent;     // True if no end date

    // ==================== PRIORITY AND STATUS ====================

    private final Priority priority;
    private final Status status;           // COMPUTED - never set manually

    // ==================== METADATA ====================

    private final String assignedByUserId; // Who created this assignment
    private final String assignedByUserName;
    private final String departmentId;     // Optional department grouping
    private final String departmentName;
    private final String roleId;          // Optional role within team
    private final String roleName;

    // ==================== SYSTEM DATA ====================

    /**
     * Administrative enable/disable flag.
     * <p>
     * - true: Assignment is enabled and should be processed
     * - false: Assignment is disabled (soft delete, administrative suspension)
     * <p>
     * This is independent of dates - a future assignment can be active=true
     */
    private final boolean active;
    private final long createdAt;
    private final long updatedAt;
    private final Long createdByUserId;
    private final Long lastModifiedByUserId;

    // ==================== CONSTRUCTOR ====================

    private UserScheduleAssignment(@NonNull Builder builder) {
        super( builder.mLocalizer, LOCALIZATION_SCOPE );

        // Identification
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.title = builder.title;
        this.description = builder.description;
        this.notes = builder.notes;

        // Core data
        this.userId = Objects.requireNonNull( builder.userId, "User ID cannot be null" );
        this.userName = builder.userName;
        this.teamId = Objects.requireNonNull( builder.teamId, "Team ID cannot be null" );
        this.teamName = builder.teamName;
        this.recurrenceRuleId = Objects.requireNonNull( builder.recurrenceRuleId, "Recurrence rule ID cannot be null" );

        // Time boundaries
        this.startDate = Objects.requireNonNull( builder.startDate, "Start date cannot be null" );
        this.endDate = builder.endDate;
        this.isPermanent = builder.endDate == null;

        // Status - Auto-correct status based on dates and business rules
        this.status = computeStatus( builder.startDate, builder.endDate, builder.active );

        // Priority
        this.priority = builder.priority != null ? builder.priority : Priority.NORMAL;

        // Metadata
        this.assignedByUserId = builder.assignedByUserId;
        this.assignedByUserName = builder.assignedByUserName;
        this.departmentId = builder.departmentId;
        this.departmentName = builder.departmentName;
        this.roleId = builder.roleId;
        this.roleName = builder.roleName;

        // System
        this.active = builder.active;
        this.createdAt = builder.createdAt > 0 ? builder.createdAt : System.currentTimeMillis();
        this.updatedAt = builder.updatedAt > 0 ? builder.updatedAt : System.currentTimeMillis();
        this.createdByUserId = builder.createdByUserId;
        this.lastModifiedByUserId = builder.lastModifiedByUserId;

        validateAssignment();
    }

    // ==================== VALIDATION ====================

    /**
     * Validate assignment consistency.
     */
    private void validateAssignment() {
        // Basic date validation
        if (endDate != null && endDate.isBefore( startDate )) {
            throw new IllegalArgumentException( "End date cannot be before start date" );
        }

        // Business rule: assignments cannot be too far in the past or future
        LocalDate today = LocalDate.now();
        LocalDate maxPast = today.minusYears( 50 );
        LocalDate maxFuture = today.plusYears( 50 );

        if (startDate.isBefore( maxPast )) {
            throw new IllegalArgumentException( "Start date cannot be more than 50 years in the past: " + startDate );
        }

        if (startDate.isAfter( maxFuture )) {
            throw new IllegalArgumentException( "Start date cannot be more than 50 years in the future: " + startDate );
        }

        // Log status computation for debugging
        Log.d( TAG, "Assignment validation passed - ID: " + id +
                ", Status: " + status + ", Active: " + active +
                ", Start: " + startDate + ", End: " + endDate );
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
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
    public String getNotes() {
        return notes;
    }

    @NonNull
    public Long getUserId() {
        return userId;
    }

    @Nullable
    public String getUserName() {
        return userName;
    }

    @NonNull
    public String getTeamId() {
        return teamId;
    }

    @Nullable
    public String getTeamName() {
        return teamName;
    }

    @NonNull
    public String getRecurrenceRuleId() {
        return recurrenceRuleId;
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    @Nullable
    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    @NonNull
    public Priority getPriority() {
        return priority;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    @Nullable
    public String getAssignedByUserName() {
        return assignedByUserName;
    }

    @Nullable
    public String getDepartmentId() {
        return departmentId;
    }

    @Nullable
    public String getDepartmentName() {
        return departmentName;
    }

    @Nullable
    public String getRoleId() {
        return roleId;
    }

    @Nullable
    public String getRoleName() {
        return roleName;
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

    @Nullable
    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    @Nullable
    public Long getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    // ==================== BUSINESS STATUS COMPUTATION ====================

    /**
     * Compute business status based on dates and administrative state.
     * This is the single source of truth for status logic.
     *
     * @param startDate Assignment start date
     * @param endDate   Assignment end date (nullable)
     * @param active    Administrative flag
     * @return Computed status that maintains domain consistency
     */
    @NonNull
    private Status computeStatus(@NonNull LocalDate startDate,
                                 @Nullable LocalDate endDate,
                                 boolean active) {
        // Administrative override: if not active, it's cancelled
        if (!active) {
            Log.d( TAG, "Assignment administratively disabled - status: CANCELLED" );
            return Status.CANCELLED;
        }

        LocalDate today = LocalDate.now();

        // Date-based status computation
        if (startDate.isAfter( today )) {
            Log.d( TAG, "Future assignment starting " + startDate + " - status: PENDING" );
            return Status.PENDING;  // Future assignment
        } else if (endDate != null && endDate.isBefore( today )) {
            Log.d( TAG, "Past assignment ended " + endDate + " - status: EXPIRED" );
            return Status.EXPIRED;  // Past assignment
        } else {
            Log.d( TAG, "Current assignment - status: ACTIVE" );
            return Status.ACTIVE;   // Current assignment
        }
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Check if assignment applies to a specific date.
     */
    public boolean appliesTo(@NonNull LocalDate date) {
        // Administrative check: assignment must be enabled
        if (!active) {
            Log.d( TAG, "Assignment is administratively disabled" );
            return false;
        }

        // Date range check (independent of status)
        if (date.isBefore( startDate )) {
            Log.d( TAG, MessageFormat.format( "Date ({0}) is before start date ({1})", date, startDate ) );
            return false;
        }

        if (!isPermanent && endDate != null && date.isAfter( endDate )) {
            Log.d( TAG, MessageFormat.format( "Date ({0}) is after end date ({1})", date, endDate ) );
            return false;
        }

        // Assignment applies to this date
        return true;
    }

    /**
     * Check if assignment is currently active based on dates.
     */
    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        return appliesTo( today );
    }

    /**
     * Check if assignment is in the future.
     */
    public boolean isFuture() {
        LocalDate today = LocalDate.now();
        return startDate.isAfter( today );
    }

    /**
     * Check if assignment has expired.
     */
    public boolean isExpired() {
        if (isPermanent) {
            return false;
        }

        LocalDate today = LocalDate.now();
        return endDate != null && endDate.isBefore( today );
    }

    /**
     * Get the effective status based on current date.
     */
    @NonNull
    public Status getEffectiveStatus() {
        return status; // Already computed correctly in constructor
    }

    /**
     * Enhanced method: Check if assignment is administratively enabled.
     */
    public boolean isAdministrativelyEnabled() {
        return active;
    }

    /**
     * Enhanced method: Check if assignment is in a "processable" state.
     * Combines administrative and business checks.
     */
    public boolean isProcessable() {
        return active && (status == Status.ACTIVE || status == Status.PENDING);
    }

    /**
     * Enhanced method: Check if assignment should be included in future planning.
     */
    public boolean isValidForPlanning(@NonNull LocalDate planningDate) {
        return active && appliesTo(planningDate);
    }

    /**
     * Get assignment duration in days.
     */
    public long getDurationDays() {
        if (isPermanent) {
            return -1; // Indicates permanent
        }

        if (endDate == null) {
            return -1;
        }

        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 because both dates are inclusive
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized priority display name.
     */
    @NonNull
    public String getPriorityDisplayName() {
        return localize( "priority." + priority.name().toLowerCase(), priority.name() );
    }

    /**
     * Get localized status display name.
     */
    @NonNull
    public String getStatusDisplayName() {
        return localize( "status." + status.name().toLowerCase(), status.name() );
    }

    /**
     * Get localized status description.
     */
    @NonNull
    public String getStatusDescription() {
        return localize( "status." + status.name().toLowerCase() + ".description", status.getDescriptionKey() );
    }

    /**
     * Get display title for UI with localization support.
     */
    @NonNull
    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }

        String teamDisplay = teamName != null ? teamName : teamId;
        String userDisplay = userName != null ? userName : String.valueOf( userId );

        return localize( "display.title", userDisplay + " - " + teamDisplay, userDisplay, teamDisplay );
    }

    /**
     * Get localized display period.
     */
    @NonNull
    public String getDisplayPeriod() {
        if (isPermanent) {
            return localize( "period.permanent", "From " + startDate, startDate );
        } else {
            return localize( "period.temporary", startDate + " to " + endDate, startDate, endDate );
        }
    }

    /**
     * Get localized assignment type description.
     */
    @NonNull
    public String getAssignmentTypeDescription() {
        if (isPermanent) {
            return localize( "type.permanent", "Permanent Assignment" );
        } else {
            long days = getDurationDays();
            if (days <= 7) {
                return localize( "type.short_term", "Short-term Assignment", days );
            } else if (days <= 30) {
                return localize( "type.medium_term", "Medium-term Assignment", days );
            } else {
                return localize( "type.long_term", "Long-term Assignment", days );
            }
        }
    }

    /**
     * Get localized effective status message.
     */
    @NonNull
    public String getEffectiveStatusMessage() {
        Status effectiveStatus = getEffectiveStatus();

        if (effectiveStatus == Status.ACTIVE && isPermanent) {
            return localize( "effective_status.active_permanent", "Active (Permanent)" );
        } else if (effectiveStatus == Status.ACTIVE && !isPermanent) {
            return localize( "effective_status.active_temporary", "Active (Until " + endDate + ")", endDate );
        } else if (effectiveStatus == Status.PENDING) {
            return localize( "effective_status.pending_start", "Starts " + startDate, startDate );
        } else if (effectiveStatus == Status.EXPIRED) {
            return localize( "effective_status.expired_on", "Expired " + endDate, endDate );
        }

        return localize( "status." + effectiveStatus.name().toLowerCase(), effectiveStatus.name() );
    }

    /**
     * Get localized duration description.
     */
    @NonNull
    public String getDurationDescription() {
        if (isPermanent) {
            return localize( "duration.permanent", "Permanent" );
        }

        long days = getDurationDays();
        if (days < 0) {
            return localize( "duration.unspecified", "Unspecified" );
        } else if (days == 1) {
            return localize( "duration.one_day", "1 Day" );
        } else if (days <= 7) {
            return localize( "duration.days", days + " Days", days );
        } else if (days <= 31) {
            long weeks = days / 7;
            long remainingDays = days % 7;
            if (remainingDays == 0) {
                return localize( "duration.weeks", weeks + " Weeks", weeks );
            } else {
                return localize( "duration.weeks_days", weeks + " Weeks, " + remainingDays + " Days", weeks, remainingDays );
            }
        } else {
            long months = days / 30;
            long remainingDays = days % 30;
            if (remainingDays == 0) {
                return localize( "duration.months", months + " Months", months );
            } else {
                return localize( "duration.months_days", months + " Months, " + remainingDays + " Days", months, remainingDays );
            }
        }
    }

    /**
     * Get localized assignment context information.
     */
    @NonNull
    public String getAssignmentContext() {
        if (assignedByUserName != null) {
            return localize( "context.assigned_by", "Assigned by " + assignedByUserName, assignedByUserName );
        } else if (departmentName != null && roleName != null) {
            return localize( "context.department_role", departmentName + " - " + roleName, departmentName, roleName );
        } else if (departmentName != null) {
            return localize( "context.department_only", "Department: " + departmentName, departmentName );
        } else if (roleName != null) {
            return localize( "context.role_only", "Role: " + roleName, roleName );
        } else {
            return localize( "context.standard", "Standard Assignment" );
        }
    }

    // ==================== FACTORY METHODS ====================
    /**
     * Create pattern assignment with correct status computation.
     * Status will be automatically determined based on start date.
     */
    @NonNull
    public static UserScheduleAssignment createPatternAssignment(@NonNull Long userId,
                                                                 @NonNull String teamId,
                                                                 @NonNull String recurrenceRuleId,
                                                                 @NonNull LocalDate startDate,
                                                                 @NonNull String patternName) {
        return builder()
                .userId(userId)
                .teamId(teamId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(startDate)
                .assignedByUserName(patternName)
                .priority(Priority.NORMAL)
                .active(true)  // Administratively enabled
                // DON'T set status - it will be computed automatically
                .build();
    }

    /**
     * Create temporary pattern assignment with end date.
     */
    @NonNull
    public static UserScheduleAssignment createPatternAssignment(@NonNull Long userId,
                                                                 @NonNull String teamId,
                                                                 @NonNull String recurrenceRuleId,
                                                                 @NonNull LocalDate startDate,
                                                                 @Nullable LocalDate endDate,
                                                                 @NonNull String patternName) {
        return builder()
                .userId(userId)
                .teamId(teamId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(startDate)
                .endDate(endDate)
                .assignedByUserName(patternName)
                .priority(Priority.NORMAL)
                .active(true)  // Administratively enabled
                // DON'T set status - it will be computed automatically
                .build();
    }

    /**
     * Create standard permanent team assignment.
     */
    @NonNull
    public static UserScheduleAssignment createPermanentAssignment(@NonNull Long userId, @NonNull String teamId,
                                                                   @NonNull String recurrenceRuleId, @NonNull LocalDate startDate) {
        return createPermanentAssignment(userId, teamId, recurrenceRuleId, startDate, null);
    }

    /**
     * Create standard permanent team assignment with localization support.
     */
    @NonNull
    public static UserScheduleAssignment createPermanentAssignment(@NonNull Long userId, @NonNull String teamId,
                                                                   @NonNull String recurrenceRuleId, @NonNull LocalDate startDate,
                                                                   @Nullable DomainLocalizer localizer) {
        return builder()
                .userId(userId)
                .teamId(teamId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(startDate)
                .priority(Priority.NORMAL)
                .active(true)
                .localizer(localizer)
                .build();
    }

    /**
     * Create temporary team assignment with end date.
     */
    @NonNull
    public static UserScheduleAssignment createTemporaryAssignment(@NonNull Long userId, @NonNull String teamId,
                                                                   @NonNull String recurrenceRuleId, @NonNull LocalDate startDate,
                                                                   @NonNull LocalDate endDate) {
        return createTemporaryAssignment(userId, teamId, recurrenceRuleId, startDate, endDate, null);
    }

    /**
     * Create temporary team assignment with end date and localization support.
     */
    @NonNull
    public static UserScheduleAssignment createTemporaryAssignment(@NonNull Long userId, @NonNull String teamId,
                                                                   @NonNull String recurrenceRuleId, @NonNull LocalDate startDate,
                                                                   @NonNull LocalDate endDate, @Nullable DomainLocalizer localizer) {
        return builder()
                .userId(userId)
                .teamId(teamId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(startDate)
                .endDate(endDate)
                .priority(Priority.HIGH) // Temporary assignments usually override standard ones
                .active(true)
                .localizer(localizer)
                .build();
    }

    /**
     * Create team transfer assignment (replaces previous assignment).
     */
    @NonNull
    public static UserScheduleAssignment createTeamTransfer(@NonNull Long userId, @NonNull String newTeamId,
                                                            @NonNull String recurrenceRuleId, @NonNull LocalDate transferDate,
                                                            @Nullable String assignedByUserId) {
        return createTeamTransfer(userId, newTeamId, recurrenceRuleId, transferDate, assignedByUserId, null);
    }

    /**
     * Create team transfer assignment with localization support.
     */
    @NonNull
    public static UserScheduleAssignment createTeamTransfer(@NonNull Long userId, @NonNull String newTeamId,
                                                            @NonNull String recurrenceRuleId, @NonNull LocalDate transferDate,
                                                            @Nullable String assignedByUserId, @Nullable DomainLocalizer localizer) {
        return builder()
                .userId(userId)
                .teamId(newTeamId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(transferDate)
                .priority(Priority.OVERRIDE) // Team transfers override existing assignments
                .active(true)
                .assignedByUserId(assignedByUserId)
                .localizer(localizer)
                .build();
    }

    /**
     * Create disabled assignment (for soft delete scenarios).
     */
    @NonNull
    public static UserScheduleAssignment createDisabledAssignment(@NonNull UserScheduleAssignment source) {
        return builder()
                .copyFrom(source)
                .active(false)  // Administratively disabled
                // Status will be automatically computed as CANCELLED
                .build();
    }
    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    @Override
    @NonNull
    public UserScheduleAssignment withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder()
                .copyFrom( this )
                .localizer( localizer )
                .build();
    }

    // ==================== BUILDER ====================

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends LocalizableBuilder<UserScheduleAssignment, Builder> {
        private String id;
        private String title;
        private String description;
        private String notes;
        private Long userId;
        private String userName;
        private String teamId;
        private String teamName;
        private String recurrenceRuleId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Priority priority;
        private Status status;
        private String assignedByUserId;
        private String assignedByUserName;
        private String departmentId;
        private String departmentName;
        private String roleId;
        private String roleName;
        private boolean active = true;
        private long createdAt;
        private long updatedAt;
        private Long createdByUserId;
        private Long lastModifiedByUserId;

        @NonNull
        public Builder copyFrom(@NonNull UserScheduleAssignment source) {
            this.id = source.id;
            this.title = source.title;
            this.description = source.description;
            this.notes = source.notes;
            this.userId = source.userId;
            this.userName = source.userName;
            this.teamId = source.teamId;
            this.teamName = source.teamName;
            this.recurrenceRuleId = source.recurrenceRuleId;
            this.startDate = source.startDate;
            this.endDate = source.endDate;
            this.priority = source.priority;
            this.status = source.status;
            this.assignedByUserId = source.assignedByUserId;
            this.assignedByUserName = source.assignedByUserName;
            this.departmentId = source.departmentId;
            this.departmentName = source.departmentName;
            this.roleId = source.roleId;
            this.roleName = source.roleName;
            this.active = source.active;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;
            this.createdByUserId = source.createdByUserId;
            this.lastModifiedByUserId = source.lastModifiedByUserId;

            return copyLocalizableFrom( source );
        }

        @NonNull
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public Builder notes(@Nullable String notes) {
            this.notes = notes;
            return this;
        }

        @NonNull
        public Builder userId(@NonNull Long userId) {
            this.userId = userId;
            return this;
        }

        @NonNull
        public Builder userName(@Nullable String userName) {
            this.userName = userName;
            return this;
        }

        @NonNull
        public Builder teamId(@NonNull String teamId) {
            this.teamId = teamId;
            return this;
        }

        @NonNull
        public Builder teamName(@Nullable String teamName) {
            this.teamName = teamName;
            return this;
        }

        @NonNull
        public Builder recurrenceRuleId(@NonNull String recurrenceRuleId) {
            this.recurrenceRuleId = recurrenceRuleId;
            return this;
        }

        @NonNull
        public Builder startDate(@NonNull LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        @NonNull
        public Builder endDate(@Nullable LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        @NonNull
        public Builder priority(@Nullable Priority priority) {
            this.priority = priority;
            return this;
        }

        @NonNull
        public Builder assignedByUserId(@Nullable String assignedByUserId) {
            this.assignedByUserId = assignedByUserId;
            return this;
        }

        @NonNull
        public Builder assignedByUserName(@Nullable String assignedByUserName) {
            this.assignedByUserName = assignedByUserName;
            return this;
        }

        @NonNull
        public Builder departmentId(@Nullable String departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        @NonNull
        public Builder departmentName(@Nullable String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        @NonNull
        public Builder roleId(@Nullable String roleId) {
            this.roleId = roleId;
            return this;
        }

        @NonNull
        public Builder roleName(@Nullable String roleName) {
            this.roleName = roleName;
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

        @NonNull
        public Builder createdByUserId(@Nullable Long createdByUserId) {
            this.createdByUserId = createdByUserId;
            return this;
        }

        @NonNull
        public Builder lastModifiedByUserId(@Nullable Long lastModifiedByUserId) {
            this.lastModifiedByUserId = lastModifiedByUserId;
            return this;
        }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        @Override
        @NonNull
        public UserScheduleAssignment build() {
            return new UserScheduleAssignment( this );
        }
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserScheduleAssignment that = (UserScheduleAssignment) o;
        return Objects.equals( id, that.id );
    }

    @Override
    public int hashCode() {
        return Objects.hash( id );
    }

    @Override
    @NonNull
    public String toString() {
        return "UserScheduleAssignment{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", teamId='" + teamId + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}