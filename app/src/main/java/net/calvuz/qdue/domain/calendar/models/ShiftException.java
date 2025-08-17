package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * ShiftException - Domain model for work schedule exceptions and modifications.
 *
 * <p>Represents any deviation from the standard work schedule pattern, including absences,
 * shift changes, time reductions, and special arrangements. Supports complex scenarios
 * like shift swaps between colleagues, company-mandated changes, and partial time modifications.</p>
 *
 * <h3>Exception Types Supported:</h3>
 * <ul>
 *   <li><strong>ABSENCE_VACATION</strong>: Planned vacation days</li>
 *   <li><strong>ABSENCE_SICK</strong>: Sick leave</li>
 *   <li><strong>ABSENCE_SPECIAL</strong>: Special leave (personal, family, etc.)</li>
 *   <li><strong>CHANGE_COMPANY</strong>: Company-mandated shift changes</li>
 *   <li><strong>CHANGE_SWAP</strong>: Voluntary shift swaps between colleagues</li>
 *   <li><strong>CHANGE_SPECIAL</strong>: Special event coverage</li>
 *   <li><strong>REDUCTION_PERSONAL</strong>: Personal time reduction requests</li>
 *   <li><strong>REDUCTION_ROL</strong>: ROL (Riduzione Orario Lavoro) reductions</li>
 *   <li><strong>REDUCTION_UNION</strong>: Union-related time off</li>
 *   <li><strong>CUSTOM</strong>: Custom exceptions for special cases</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.1.0 - i18n Integration
 * @since Clean Architecture Phase 2
 */
public class ShiftException extends LocalizableDomainModel {

    private static final String TAG = "ShiftException";

    private static final String LOCALIZATION_SCOPE = "shift_exception";

    // ==================== ENUMS ====================

    /**
     * Type of shift exception defining behavior and business rules.
     */
    public enum ExceptionType {
        // Full day absences
        ABSENCE_VACATION("vacation", "planned_vacation_holiday", true, false),
        ABSENCE_SICK("sick_leave", "medical_absence", true, false),
        ABSENCE_SPECIAL("special_leave", "personal_family_emergency", true, true),

        // Shift changes and swaps
        CHANGE_COMPANY("company_change", "company_mandated_shift_change", false, true),
        CHANGE_SWAP("shift_swap", "voluntary_swap_colleague", false, true),
        CHANGE_SPECIAL("special_coverage", "event_emergency_coverage", false, true),

        // Time reductions
        REDUCTION_PERSONAL("personal_reduction", "personal_time_reduction", false, true),
        REDUCTION_ROL("rol_reduction", "riduzione_orario_lavoro", false, false),
        REDUCTION_UNION("union_time", "union_syndical_activities", false, false),

        // Custom
        CUSTOM("custom_exception", "custom_business_rule", false, true);

        private final String displayNameKey;
        private final String descriptionKey;
        private final boolean isFullDayDefault;
        private final boolean requiresApprovalDefault;

        ExceptionType(String displayNameKey, String descriptionKey, boolean isFullDayDefault, boolean requiresApprovalDefault) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
            this.isFullDayDefault = isFullDayDefault;
            this.requiresApprovalDefault = requiresApprovalDefault;
        }

        public String getDisplayNameKey() { return displayNameKey; }
        public String getDescriptionKey() { return descriptionKey; }
        public boolean isFullDayDefault() { return isFullDayDefault; }
        public boolean requiresApprovalDefault() { return requiresApprovalDefault; }
    }

    /**
     * Status of the exception in approval workflow.
     */
    public enum ApprovalStatus {
        DRAFT("draft", "being_created_not_submitted"),
        PENDING("pending", "submitted_awaiting_approval"),
        APPROVED("approved", "approved_and_active"),
        REJECTED("rejected", "rejected_by_supervisor"),
        CANCELLED("cancelled", "cancelled_by_requester"),
        EXPIRED("expired", "expired_without_approval");

        private final String displayNameKey;
        private final String descriptionKey;

        ApprovalStatus(String displayNameKey, String descriptionKey) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
        }

        public String getDisplayNameKey() { return displayNameKey; }
        public String getDescriptionKey() { return descriptionKey; }
    }

    /**
     * Priority level for conflicting exceptions.
     */
    public enum Priority {
        LOW(1, "low_priority"),
        NORMAL(5, "normal_priority"),
        HIGH(8, "high_priority"),
        URGENT(10, "urgent_priority");

        private final int level;
        private final String displayNameKey;

        Priority(int level, String displayNameKey) {
            this.level = level;
            this.displayNameKey = displayNameKey;
        }

        public int getLevel() { return level; }
        public String getDisplayNameKey() { return displayNameKey; }
    }

    // ==================== IDENTIFICATION ====================

    private final String id;
    private final String title;
    private final String description;
    private final String notes;

    // ==================== CORE EXCEPTION DATA ====================

    private final ExceptionType type;
    private final Long userId;                 // Primary user affected
    private final LocalDate targetDate;        // Date of the exception
    private final boolean isFullDay;           // Full day exception vs partial

    // ==================== TIMING MODIFICATIONS ====================

    private final String originalShiftId;      // Original shift being modified
    private final String newShiftId;           // New shift (for swaps/changes)
    private final LocalTime newStartTime;      // Modified start time
    private final LocalTime newEndTime;        // Modified end time
    private final Integer durationMinutes;     // Total exception duration

    // ==================== SWAP AND COLLABORATION DATA ====================

    private final Long swapWithUserId;         // User swapping with (for CHANGE_SWAP)
    private final String swapWithUserName;     // Display name for UI
    private final Long replacementUserId;      // Replacement user (for coverage)
    private final String replacementUserName;  // Display name for UI

    // ==================== APPROVAL WORKFLOW ====================

    private final ApprovalStatus status;
    private final boolean requiresApproval;
    private final Long approvedByUserId;
    private final String approvedByUserName;
    private final LocalDate approvedDate;
    private final String rejectionReason;

    // ==================== PRIORITY AND METADATA ====================

    private final Priority priority;
    private final boolean isRecurring;         // Future: recurring exceptions
    private final String recurrenceRuleId;     // Link to RecurrenceRule if recurring
    private final Map<String, String> metadata; // Flexible key-value metadata

    // ==================== SYSTEM DATA ====================

    private final boolean active;
    private final long createdAt;
    private final long updatedAt;
    private final Long createdByUserId;
    private final Long lastModifiedByUserId;

    // ==================== CONSTRUCTOR ====================

    private ShiftException(@NonNull Builder builder) {
        super(builder.mLocalizer, LOCALIZATION_SCOPE);

        // Identification
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.title = builder.title;
        this.description = builder.description;
        this.notes = builder.notes;

        // Core data
        this.type = Objects.requireNonNull(builder.type, "Exception type cannot be null");
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.targetDate = Objects.requireNonNull(builder.targetDate, "Target date cannot be null");
        this.isFullDay = builder.isFullDay != null ? builder.isFullDay : type.isFullDayDefault();

        // Timing
        this.originalShiftId = builder.originalShiftId;
        this.newShiftId = builder.newShiftId;
        this.newStartTime = builder.newStartTime;
        this.newEndTime = builder.newEndTime;
        this.durationMinutes = builder.durationMinutes;

        // Collaboration
        this.swapWithUserId = builder.swapWithUserId;
        this.swapWithUserName = builder.swapWithUserName;
        this.replacementUserId = builder.replacementUserId;
        this.replacementUserName = builder.replacementUserName;

        // Approval
        this.status = builder.status != null ? builder.status : ApprovalStatus.DRAFT;
        this.requiresApproval = builder.requiresApproval != null ? builder.requiresApproval : type.requiresApprovalDefault();
        this.approvedByUserId = builder.approvedByUserId;
        this.approvedByUserName = builder.approvedByUserName;
        this.approvedDate = builder.approvedDate;
        this.rejectionReason = builder.rejectionReason;

        // Priority and metadata
        this.priority = builder.priority != null ? builder.priority : Priority.NORMAL;
        this.isRecurring = builder.isRecurring;
        this.recurrenceRuleId = builder.recurrenceRuleId;
        this.metadata = builder.metadata != null ? new HashMap<>(builder.metadata) : new HashMap<>();

        // System
        this.active = builder.active;
        this.createdAt = builder.createdAt > 0 ? builder.createdAt : System.currentTimeMillis();
        this.updatedAt = builder.updatedAt > 0 ? builder.updatedAt : System.currentTimeMillis();
        this.createdByUserId = builder.createdByUserId;
        this.lastModifiedByUserId = builder.lastModifiedByUserId;

        validateException();
    }

    // ==================== VALIDATION ====================

    /**
     * Validate exception consistency and business rules.
     */
    private void validateException() {
        // Validate timing for partial day exceptions
        if (!isFullDay) {
            if (newStartTime != null && newEndTime != null) {
                if (newEndTime.isBefore(newStartTime) && !crossesMidnight()) {
                    throw new IllegalArgumentException("End time cannot be before start time for same-day exception");
                }
            }
        }

        // Validate swap data
        if (type == ExceptionType.CHANGE_SWAP && swapWithUserId == null) {
            throw new IllegalArgumentException("CHANGE_SWAP type requires swapWithUserId");
        }

        // Validate approval consistency
        if (status == ApprovalStatus.APPROVED) {
            if (requiresApproval && approvedByUserId == null) {
                throw new IllegalArgumentException("Approved exceptions requiring approval must have approvedByUserId");
            }
        }

        // Validate rejection reason
        if (status == ApprovalStatus.REJECTED && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
            throw new IllegalArgumentException("Rejected exceptions must have rejection reason");
        }

        // Validate recurring data
        if (isRecurring && recurrenceRuleId == null) {
            throw new IllegalArgumentException("Recurring exceptions must have recurrenceRuleId");
        }
    }

    // ==================== GETTERS ====================

    @NonNull public String getId() { return id; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getDescription() { return description; }
    @Nullable public String getNotes() { return notes; }
    @NonNull public ExceptionType getType() { return type; }
    @NonNull public Long getUserId() { return userId; }
    @NonNull public LocalDate getTargetDate() { return targetDate; }
    public boolean isFullDay() { return isFullDay; }
    @Nullable public String getOriginalShiftId() { return originalShiftId; }
    @Nullable public String getNewShiftId() { return newShiftId; }
    @Nullable public LocalTime getNewStartTime() { return newStartTime; }
    @Nullable public LocalTime getNewEndTime() { return newEndTime; }
    @Nullable public Integer getDurationMinutes() { return durationMinutes; }
    @Nullable public Long getSwapWithUserId() { return swapWithUserId; }
    @Nullable public String getSwapWithUserName() { return swapWithUserName; }
    @Nullable public Long getReplacementUserId() { return replacementUserId; }
    @Nullable public String getReplacementUserName() { return replacementUserName; }
    @NonNull public ApprovalStatus getStatus() { return status; }
    public boolean requiresApproval() { return requiresApproval; }
    @Nullable public Long getApprovedByUserId() { return approvedByUserId; }
    @Nullable public String getApprovedByUserName() { return approvedByUserName; }
    @Nullable public LocalDate getApprovedDate() { return approvedDate; }
    @Nullable public String getRejectionReason() { return rejectionReason; }
    @NonNull public Priority getPriority() { return priority; }
    public boolean isRecurring() { return isRecurring; }
    @Nullable public String getRecurrenceRuleId() { return recurrenceRuleId; }
    @NonNull public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    @Nullable public Long getCreatedByUserId() { return createdByUserId; }
    @Nullable public Long getLastModifiedByUserId() { return lastModifiedByUserId; }

    // ==================== BUSINESS METHODS ====================

    /**
     * Check if this exception applies to the target date.
     */
    public boolean appliesTo(@NonNull LocalDate date) {
        return active && targetDate.equals(date) && isEffective();
    }

    /**
     * Check if exception is currently effective.
     */
    public boolean isEffective() {
        return status == ApprovalStatus.APPROVED || (!requiresApproval && status == ApprovalStatus.DRAFT);
    }

    /**
     * Check if exception is pending approval.
     */
    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    /**
     * Check if exception crosses midnight.
     */
    public boolean crossesMidnight() {
        return newStartTime != null && newEndTime != null && newEndTime.isBefore(newStartTime);
    }

    /**
     * Get calculated duration in minutes.
     */
    public int getCalculatedDurationMinutes() {
        if (durationMinutes != null) {
            return durationMinutes;
        }

        if (isFullDay) {
            return 24 * 60; // Full day = 24 hours
        }

        if (newStartTime != null && newEndTime != null) {
            if (crossesMidnight()) {
                // Calculate across midnight
                int minutesToMidnight = (24 * 60) - (newStartTime.getHour() * 60 + newStartTime.getMinute());
                int minutesFromMidnight = newEndTime.getHour() * 60 + newEndTime.getMinute();
                return minutesToMidnight + minutesFromMidnight;
            } else {
                // Same day calculation
                int startMinutes = newStartTime.getHour() * 60 + newStartTime.getMinute();
                int endMinutes = newEndTime.getHour() * 60 + newEndTime.getMinute();
                return endMinutes - startMinutes;
            }
        }

        return 0;
    }

    /**
     * Check if this is an absence type exception.
     */
    public boolean isAbsence() {
        return type == ExceptionType.ABSENCE_VACATION ||
                type == ExceptionType.ABSENCE_SICK ||
                type == ExceptionType.ABSENCE_SPECIAL;
    }

    /**
     * Check if this is a shift change type exception.
     */
    public boolean isShiftChange() {
        return type == ExceptionType.CHANGE_COMPANY ||
                type == ExceptionType.CHANGE_SWAP ||
                type == ExceptionType.CHANGE_SPECIAL;
    }

    /**
     * Check if this is a time reduction type exception.
     */
    public boolean isTimeReduction() {
        return type == ExceptionType.REDUCTION_PERSONAL ||
                type == ExceptionType.REDUCTION_ROL ||
                type == ExceptionType.REDUCTION_UNION;
    }

    /**
     * Check if exception involves another user (swap or replacement).
     */
    public boolean involvesOtherUser() {
        return swapWithUserId != null || replacementUserId != null;
    }

    /**
     * Get metadata value by key.
     */
    @Nullable
    public String getMetadata(@NonNull String key) {
        return metadata.get(key);
    }

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized type display name.
     */
    @NonNull
    public String getTypeDisplayName() {
        return localize("type." + type.name().toLowerCase(), type.name());
    }

    /**
     * Get localized type description.
     */
    @NonNull
    public String getTypeDescription() {
        return localize("type." + type.name().toLowerCase() + ".description", type.getDescriptionKey());
    }

    /**
     * Get display title for UI with localization support.
     */
    @NonNull
    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return getTypeDisplayName();
    }

    /**
     * Get localized status display name.
     */
    @NonNull
    public String getStatusDisplayName() {
        return localize("status." + status.name().toLowerCase(), status.name());
    }

    /**
     * Get localized status description.
     */
    @NonNull
    public String getStatusDescription() {
        return localize("status." + status.name().toLowerCase() + ".description", status.getDescriptionKey());
    }

    /**
     * Get localized priority display name.
     */
    @NonNull
    public String getPriorityDisplayName() {
        return localize("priority." + priority.name().toLowerCase(), priority.name());
    }

    /**
     * Get localized duration description.
     */
    @NonNull
    public String getDurationDescription() {
        if (isFullDay) {
            return localize("duration.full_day", "Full Day");
        }

        int minutes = getCalculatedDurationMinutes();
        if (minutes <= 0) {
            return localize("duration.unspecified", "Unspecified");
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return localize("duration.hours_minutes", hours + "h " + mins + "m", hours, mins);
        } else if (hours > 0) {
            return localize("duration.hours_only", hours + "h", hours);
        } else {
            return localize("duration.minutes_only", mins + "m", mins);
        }
    }

    /**
     * Get localized approval status message.
     */
    @NonNull
    public String getApprovalStatusMessage() {
        if (status == ApprovalStatus.APPROVED && approvedByUserName != null) {
            return localize("approval.approved_by", "Approved by " + approvedByUserName, approvedByUserName);
        } else if (status == ApprovalStatus.REJECTED && rejectionReason != null) {
            return localize("approval.rejected_reason", "Rejected: " + rejectionReason, rejectionReason);
        } else if (status == ApprovalStatus.PENDING) {
            return localize("approval.pending_review", "Pending Review");
        }
        return getStatusDisplayName();
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create vacation absence exception.
     */
    @NonNull
    public static ShiftException createVacation(@NonNull Long userId, @NonNull LocalDate date) {
        return createVacation(userId, date, null);
    }

    /**
     * Create vacation absence exception with localization support.
     */
    @NonNull
    public static ShiftException createVacation(@NonNull Long userId, @NonNull LocalDate date,
                                                @Nullable DomainLocalizer localizer) {
        return builder()
                .type(ExceptionType.ABSENCE_VACATION)
                .userId(userId)
                .targetDate(date)
                .isFullDay(true)
                .localizer(localizer)
                .build();
    }

    /**
     * Create sick leave exception.
     */
    @NonNull
    public static ShiftException createSickLeave(@NonNull Long userId, @NonNull LocalDate date) {
        return createSickLeave(userId, date, null);
    }

    /**
     * Create sick leave exception with localization support.
     */
    @NonNull
    public static ShiftException createSickLeave(@NonNull Long userId, @NonNull LocalDate date,
                                                 @Nullable DomainLocalizer localizer) {
        return builder()
                .type(ExceptionType.ABSENCE_SICK)
                .userId(userId)
                .targetDate(date)
                .isFullDay(true)
                .requiresApproval(false)  // Usually no approval needed for sick leave
                .localizer(localizer)
                .build();
    }

    /**
     * Create shift swap between users.
     */
    @NonNull
    public static ShiftException createShiftSwap(@NonNull Long userId, @NonNull Long swapWithUserId,
                                                 @NonNull LocalDate date, @NonNull String originalShiftId,
                                                 @NonNull String newShiftId) {
        return createShiftSwap(userId, swapWithUserId, date, originalShiftId, newShiftId, null);
    }

    /**
     * Create shift swap between users with localization support.
     */
    @NonNull
    public static ShiftException createShiftSwap(@NonNull Long userId, @NonNull Long swapWithUserId,
                                                 @NonNull LocalDate date, @NonNull String originalShiftId,
                                                 @NonNull String newShiftId, @Nullable DomainLocalizer localizer) {
        return builder()
                .type(ExceptionType.CHANGE_SWAP)
                .userId(userId)
                .targetDate(date)
                .originalShiftId(originalShiftId)
                .newShiftId(newShiftId)
                .swapWithUserId(swapWithUserId)
                .isFullDay(false)
                .requiresApproval(true)
                .localizer(localizer)
                .build();
    }

    /**
     * Create time reduction exception.
     */
    @NonNull
    public static ShiftException createTimeReduction(@NonNull Long userId, @NonNull LocalDate date,
                                                     @NonNull LocalTime newStartTime, @NonNull LocalTime newEndTime,
                                                     @NonNull ExceptionType reductionType) {
        return createTimeReduction(userId, date, newStartTime, newEndTime, reductionType, null);
    }

    /**
     * Create time reduction exception with localization support.
     */
    @NonNull
    public static ShiftException createTimeReduction(@NonNull Long userId, @NonNull LocalDate date,
                                                     @NonNull LocalTime newStartTime, @NonNull LocalTime newEndTime,
                                                     @NonNull ExceptionType reductionType, @Nullable DomainLocalizer localizer) {
        if (!reductionType.name().startsWith("REDUCTION_")) {
            throw new IllegalArgumentException("reductionType must be a REDUCTION_ type");
        }

        return builder()
                .type(reductionType)
                .userId(userId)
                .targetDate(date)
                .newStartTime(newStartTime)
                .newEndTime(newEndTime)
                .isFullDay(false)
                .localizer(localizer)
                .build();
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    @Override
    @NonNull
    public ShiftException withLocalizer(@NonNull DomainLocalizer localizer) {
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

    public static class Builder extends LocalizableBuilder<ShiftException, Builder> {
        private String id;
        private String title;
        private String description;
        private String notes;
        private ExceptionType type;
        private Long userId;
        private LocalDate targetDate;
        private Boolean isFullDay;
        private String originalShiftId;
        private String newShiftId;
        private LocalTime newStartTime;
        private LocalTime newEndTime;
        private Integer durationMinutes;
        private Long swapWithUserId;
        private String swapWithUserName;
        private Long replacementUserId;
        private String replacementUserName;
        private ApprovalStatus status;
        private Boolean requiresApproval;
        private Long approvedByUserId;
        private String approvedByUserName;
        private LocalDate approvedDate;
        private String rejectionReason;
        private Priority priority;
        private boolean isRecurring;
        private String recurrenceRuleId;
        private Map<String, String> metadata;
        private boolean active = true;
        private long createdAt;
        private long updatedAt;
        private Long createdByUserId;
        private Long lastModifiedByUserId;

        @NonNull
        public Builder copyFrom(@NonNull ShiftException source) {
            this.id = source.id;
            this.title = source.title;
            this.description = source.description;
            this.notes = source.notes;
            this.type = source.type;
            this.userId = source.userId;
            this.targetDate = source.targetDate;
            this.isFullDay = source.isFullDay;
            this.originalShiftId = source.originalShiftId;
            this.newShiftId = source.newShiftId;
            this.newStartTime = source.newStartTime;
            this.newEndTime = source.newEndTime;
            this.durationMinutes = source.durationMinutes;
            this.swapWithUserId = source.swapWithUserId;
            this.swapWithUserName = source.swapWithUserName;
            this.replacementUserId = source.replacementUserId;
            this.replacementUserName = source.replacementUserName;
            this.status = source.status;
            this.requiresApproval = source.requiresApproval;
            this.approvedByUserId = source.approvedByUserId;
            this.approvedByUserName = source.approvedByUserName;
            this.approvedDate = source.approvedDate;
            this.rejectionReason = source.rejectionReason;
            this.priority = source.priority;
            this.isRecurring = source.isRecurring;
            this.recurrenceRuleId = source.recurrenceRuleId;
            this.metadata = source.metadata != null ? new HashMap<>(source.metadata) : null;
            this.active = source.active;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;
            this.createdByUserId = source.createdByUserId;
            this.lastModifiedByUserId = source.lastModifiedByUserId;

            return copyLocalizableFrom(source);
        }

        // Builder methods for all fields...
        @NonNull public Builder id(@Nullable String id) { this.id = id; return this; }
        @NonNull public Builder title(@Nullable String title) { this.title = title; return this; }
        @NonNull public Builder description(@Nullable String description) { this.description = description; return this; }
        @NonNull public Builder notes(@Nullable String notes) { this.notes = notes; return this; }
        @NonNull public Builder type(@NonNull ExceptionType type) { this.type = type; return this; }
        @NonNull public Builder userId(@NonNull Long userId) { this.userId = userId; return this; }
        @NonNull public Builder targetDate(@NonNull LocalDate targetDate) { this.targetDate = targetDate; return this; }
        @NonNull public Builder isFullDay(@Nullable Boolean isFullDay) { this.isFullDay = isFullDay; return this; }
        @NonNull public Builder originalShiftId(@Nullable String originalShiftId) { this.originalShiftId = originalShiftId; return this; }
        @NonNull public Builder newShiftId(@Nullable String newShiftId) { this.newShiftId = newShiftId; return this; }
        @NonNull public Builder newStartTime(@Nullable LocalTime newStartTime) { this.newStartTime = newStartTime; return this; }
        @NonNull public Builder newEndTime(@Nullable LocalTime newEndTime) { this.newEndTime = newEndTime; return this; }
        @NonNull public Builder durationMinutes(@Nullable Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        @NonNull public Builder swapWithUserId(@Nullable Long swapWithUserId) { this.swapWithUserId = swapWithUserId; return this; }
        @NonNull public Builder swapWithUserName(@Nullable String swapWithUserName) { this.swapWithUserName = swapWithUserName; return this; }
        @NonNull public Builder replacementUserId(@Nullable Long replacementUserId) { this.replacementUserId = replacementUserId; return this; }
        @NonNull public Builder replacementUserName(@Nullable String replacementUserName) { this.replacementUserName = replacementUserName; return this; }
        @NonNull public Builder status(@Nullable ApprovalStatus status) { this.status = status; return this; }
        @NonNull public Builder requiresApproval(@Nullable Boolean requiresApproval) { this.requiresApproval = requiresApproval; return this; }
        @NonNull public Builder approvedByUserId(@Nullable Long approvedByUserId) { this.approvedByUserId = approvedByUserId; return this; }
        @NonNull public Builder approvedByUserName(@Nullable String approvedByUserName) { this.approvedByUserName = approvedByUserName; return this; }
        @NonNull public Builder approvedDate(@Nullable LocalDate approvedDate) { this.approvedDate = approvedDate; return this; }
        @NonNull public Builder rejectionReason(@Nullable String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        @NonNull public Builder priority(@Nullable Priority priority) { this.priority = priority; return this; }
        @NonNull public Builder isRecurring(boolean isRecurring) { this.isRecurring = isRecurring; return this; }
        @NonNull public Builder recurrenceRuleId(@Nullable String recurrenceRuleId) { this.recurrenceRuleId = recurrenceRuleId; return this; }
        @NonNull public Builder metadata(@Nullable Map<String, String> metadata) { this.metadata = metadata; return this; }
        @NonNull public Builder active(boolean active) { this.active = active; return this; }
        @NonNull public Builder createdAt(long createdAt) { this.createdAt = createdAt; return this; }
        @NonNull public Builder updatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }
        @NonNull public Builder createdByUserId(@Nullable Long createdByUserId) { this.createdByUserId = createdByUserId; return this; }
        @NonNull public Builder lastModifiedByUserId(@Nullable Long lastModifiedByUserId) { this.lastModifiedByUserId = lastModifiedByUserId; return this; }

        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        @Override
        @NonNull
        public ShiftException build() {
            return new ShiftException(this);
        }
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftException that = (ShiftException) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    @NonNull
    public String toString() {
        return "ShiftException{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", userId=" + userId +
                ", targetDate=" + targetDate +
                ", status=" + status +
                ", isFullDay=" + isFullDay +
                '}';
    }
}