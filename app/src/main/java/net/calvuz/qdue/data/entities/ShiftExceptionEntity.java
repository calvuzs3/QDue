package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.core.db.converters.QDueTypeConverters;
import net.calvuz.qdue.domain.calendar.models.ShiftException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * ShiftExceptionEntity - Room database entity for work schedule exceptions.
 *
 * <p>Stores all types of schedule exceptions including absences, shift changes,
 * time reductions, and approval workflow data. Optimized for high-frequency
 * queries by user, date, and status.</p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li><strong>Primary Key</strong>: UUID-based ID for global uniqueness</li>
 *   <li><strong>User-Date Index</strong>: Composite index for user schedule queries</li>
 *   <li><strong>Status Index</strong>: Fast queries for approval workflow</li>
 *   <li><strong>Priority Index</strong>: Conflict resolution ordering</li>
 *   <li><strong>JSON Metadata</strong>: Flexible key-value storage for extensions</li>
 * </ul>
 *
 * <h3>Query Optimizations:</h3>
 * <ul>
 *   <li>Primary index: (user_id, target_date, priority) for user schedule generation</li>
 *   <li>Status index: (status, requires_approval, target_date) for approval workflows</li>
 *   <li>Date range index: (target_date, active) for calendar views</li>
 *   <li>Swap index: (swap_with_user_id, target_date) for bilateral queries</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Engine Database
 * @since Clean Architecture Phase 2
 */
@Entity(
        tableName = "shift_exceptions",
        indices = {
                @Index(value = {"user_id", "target_date", "priority"}, name = "idx_exception_user_date_priority"),
                @Index(value = {"status", "requires_approval", "target_date"}, name = "idx_exception_status_approval"),
                @Index(value = {"target_date", "active"}, name = "idx_exception_date_active"),
                @Index(value = {"swap_with_user_id", "target_date"}, name = "idx_exception_swap_user"),
                @Index(value = {"exception_type", "target_date"}, name = "idx_exception_type_date"),
                @Index(value = {"user_id", "status"}, name = "idx_exception_user_status")
        }
)
public class ShiftExceptionEntity {

    // ==================== PRIMARY KEY ====================

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    // ==================== IDENTIFICATION ====================

    @Nullable
    @ColumnInfo(name = "title")
    private String title;

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    @Nullable
    @ColumnInfo(name = "notes")
    private String notes;

    // ==================== CORE EXCEPTION DATA ====================

    @NonNull
    @ColumnInfo(name = "exception_type")
    private String exceptionType; // ABSENCE_VACATION, CHANGE_SWAP, etc.

    @NonNull
    @ColumnInfo(name = "user_id")
    private Long userId;

    @NonNull
    @ColumnInfo(name = "target_date")
    private String targetDate; // ISO format: yyyy-MM-dd

    @ColumnInfo(name = "is_full_day", defaultValue = "0")
    private boolean isFullDay;

    // ==================== TIMING MODIFICATIONS ====================

    @Nullable
    @ColumnInfo(name = "original_shift_id")
    private String originalShiftId;

    @Nullable
    @ColumnInfo(name = "new_shift_id")
    private String newShiftId;

    @Nullable
    @ColumnInfo(name = "new_start_time")
    private String newStartTime; // ISO format: HH:mm

    @Nullable
    @ColumnInfo(name = "new_end_time")
    private String newEndTime; // ISO format: HH:mm

    @Nullable
    @ColumnInfo(name = "duration_minutes")
    private Integer durationMinutes;

    // ==================== SWAP AND COLLABORATION DATA ====================

    @Nullable
    @ColumnInfo(name = "swap_with_user_id")
    private Long swapWithUserId;

    @Nullable
    @ColumnInfo(name = "swap_with_user_name")
    private String swapWithUserName;

    @Nullable
    @ColumnInfo(name = "replacement_user_id")
    private Long replacementUserId;

    @Nullable
    @ColumnInfo(name = "replacement_user_name")
    private String replacementUserName;

    // ==================== APPROVAL WORKFLOW ====================

    @NonNull
    @ColumnInfo(name = "status", defaultValue = "DRAFT")
    private String status; // DRAFT, PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED

    @ColumnInfo(name = "requires_approval", defaultValue = "0")
    private boolean requiresApproval;

    @Nullable
    @ColumnInfo(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Nullable
    @ColumnInfo(name = "approved_by_user_name")
    private String approvedByUserName;

    @Nullable
    @ColumnInfo(name = "approved_date")
    private String approvedDate; // ISO format: yyyy-MM-dd

    @Nullable
    @ColumnInfo(name = "rejection_reason")
    private String rejectionReason;

    // ==================== PRIORITY AND METADATA ====================

    @NonNull
    @ColumnInfo(name = "priority", defaultValue = "NORMAL")
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @ColumnInfo(name = "is_recurring", defaultValue = "0")
    private boolean isRecurring;

    @Nullable
    @ColumnInfo(name = "recurrence_rule_id")
    private String recurrenceRuleId;

    @Nullable
    @ColumnInfo(name = "metadata_json")
    private String metadataJson; // JSON key-value pairs

    // ==================== SYSTEM DATA ====================

    @ColumnInfo(name = "active", defaultValue = "1")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @Nullable
    @ColumnInfo(name = "created_by_user_id")
    private Long createdByUserId;

    @Nullable
    @ColumnInfo(name = "last_modified_by_user_id")
    private Long lastModifiedByUserId;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public ShiftExceptionEntity() {
        // Room requires a no-argument constructor
    }

    /**
     * Constructor with required fields.
     */
    public ShiftExceptionEntity(@NonNull String id, @NonNull String exceptionType,
                                @NonNull Long userId, @NonNull String targetDate) {
        this.id = id;
        this.exceptionType = exceptionType;
        this.userId = userId;
        this.targetDate = targetDate;
        this.isFullDay = false;
        this.status = "DRAFT";
        this.requiresApproval = false;
        this.priority = "NORMAL";
        this.isRecurring = false;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @Nullable
    public String getTitle() { return title; }
    public void setTitle(@Nullable String title) { this.title = title; }

    @Nullable
    public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    @Nullable
    public String getNotes() { return notes; }
    public void setNotes(@Nullable String notes) { this.notes = notes; }

    @NonNull
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(@NonNull String exceptionType) { this.exceptionType = exceptionType; }

    @NonNull
    public Long getUserId() { return userId; }
    public void setUserId(@NonNull Long userId) { this.userId = userId; }

    @NonNull
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(@NonNull String targetDate) { this.targetDate = targetDate; }

    public boolean isFullDay() { return isFullDay; }
    public void setFullDay(boolean fullDay) { isFullDay = fullDay; }

    @Nullable
    public String getOriginalShiftId() { return originalShiftId; }
    public void setOriginalShiftId(@Nullable String originalShiftId) { this.originalShiftId = originalShiftId; }

    @Nullable
    public String getNewShiftId() { return newShiftId; }
    public void setNewShiftId(@Nullable String newShiftId) { this.newShiftId = newShiftId; }

    @Nullable
    public String getNewStartTime() { return newStartTime; }
    public void setNewStartTime(@Nullable String newStartTime) { this.newStartTime = newStartTime; }

    @Nullable
    public String getNewEndTime() { return newEndTime; }
    public void setNewEndTime(@Nullable String newEndTime) { this.newEndTime = newEndTime; }

    @Nullable
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(@Nullable Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    @Nullable
    public Long getSwapWithUserId() { return swapWithUserId; }
    public void setSwapWithUserId(@Nullable Long swapWithUserId) { this.swapWithUserId = swapWithUserId; }

    @Nullable
    public String getSwapWithUserName() { return swapWithUserName; }
    public void setSwapWithUserName(@Nullable String swapWithUserName) { this.swapWithUserName = swapWithUserName; }

    @Nullable
    public Long getReplacementUserId() { return replacementUserId; }
    public void setReplacementUserId(@Nullable Long replacementUserId) { this.replacementUserId = replacementUserId; }

    @Nullable
    public String getReplacementUserName() { return replacementUserName; }
    public void setReplacementUserName(@Nullable String replacementUserName) { this.replacementUserName = replacementUserName; }

    @NonNull
    public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    @Nullable
    public Long getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(@Nullable Long approvedByUserId) { this.approvedByUserId = approvedByUserId; }

    @Nullable
    public String getApprovedByUserName() { return approvedByUserName; }
    public void setApprovedByUserName(@Nullable String approvedByUserName) { this.approvedByUserName = approvedByUserName; }

    @Nullable
    public String getApprovedDate() { return approvedDate; }
    public void setApprovedDate(@Nullable String approvedDate) { this.approvedDate = approvedDate; }

    @Nullable
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(@Nullable String rejectionReason) { this.rejectionReason = rejectionReason; }

    @NonNull
    public String getPriority() { return priority; }
    public void setPriority(@NonNull String priority) { this.priority = priority; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    @Nullable
    public String getRecurrenceRuleId() { return recurrenceRuleId; }
    public void setRecurrenceRuleId(@Nullable String recurrenceRuleId) { this.recurrenceRuleId = recurrenceRuleId; }

    @Nullable
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(@Nullable String metadataJson) { this.metadataJson = metadataJson; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Nullable
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(@Nullable Long createdByUserId) { this.createdByUserId = createdByUserId; }

    @Nullable
    public Long getLastModifiedByUserId() { return lastModifiedByUserId; }
    public void setLastModifiedByUserId(@Nullable Long lastModifiedByUserId) { this.lastModifiedByUserId = lastModifiedByUserId; }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert to domain model.
     */
    @NonNull
    public ShiftException toDomainModel() {
        ShiftException.Builder builder = ShiftException.builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .notes(this.notes)
                .type(ShiftException.ExceptionType.valueOf(this.exceptionType))
                .userId(this.userId)
                .targetDate(LocalDate.parse(this.targetDate))
                .isFullDay(this.isFullDay)
                .status(ShiftException.ApprovalStatus.valueOf(this.status))
                .requiresApproval(this.requiresApproval)
                .priority(ShiftException.Priority.valueOf(this.priority))
                .isRecurring(this.isRecurring)
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);

        // Optional shift references
        if (this.originalShiftId != null) {
            builder.originalShiftId(this.originalShiftId);
        }
        if (this.newShiftId != null) {
            builder.newShiftId(this.newShiftId);
        }

        // Optional timing
        if (this.newStartTime != null) {
            builder.newStartTime(LocalTime.parse(this.newStartTime));
        }
        if (this.newEndTime != null) {
            builder.newEndTime(LocalTime.parse(this.newEndTime));
        }
        if (this.durationMinutes != null) {
            builder.durationMinutes(this.durationMinutes);
        }

        // Optional collaboration
        if (this.swapWithUserId != null) {
            builder.swapWithUserId(this.swapWithUserId);
        }
        if (this.swapWithUserName != null) {
            builder.swapWithUserName(this.swapWithUserName);
        }
        if (this.replacementUserId != null) {
            builder.replacementUserId(this.replacementUserId);
        }
        if (this.replacementUserName != null) {
            builder.replacementUserName(this.replacementUserName);
        }

        // Optional approval data
        if (this.approvedByUserId != null) {
            builder.approvedByUserId(this.approvedByUserId);
        }
        if (this.approvedByUserName != null) {
            builder.approvedByUserName(this.approvedByUserName);
        }
        if (this.approvedDate != null) {
            builder.approvedDate(LocalDate.parse(this.approvedDate));
        }
        if (this.rejectionReason != null) {
            builder.rejectionReason(this.rejectionReason);
        }

        // Optional recurrence
        if (this.recurrenceRuleId != null) {
            builder.recurrenceRuleId(this.recurrenceRuleId);
        }

        // Optional metadata (JSON deserialization)
        if (this.metadataJson != null) {
            Map<String, String> metadata = QDueTypeConverters.toStringMap(this.metadataJson);
            builder.metadata(metadata);
        }

        // Optional system data
        if (this.createdByUserId != null) {
            builder.createdByUserId(this.createdByUserId);
        }
        if (this.lastModifiedByUserId != null) {
            builder.lastModifiedByUserId(this.lastModifiedByUserId);
        }

        return builder.build();
    }

    /**
     * Create entity from domain model.
     */
    @NonNull
    public static ShiftExceptionEntity fromDomainModel(@NonNull ShiftException domainModel) {
        ShiftExceptionEntity entity = new ShiftExceptionEntity();

        // Basic fields
        entity.setId(domainModel.getId());
        entity.setTitle(domainModel.getTitle());
        entity.setDescription(domainModel.getDescription());
        entity.setNotes(domainModel.getNotes());
        entity.setExceptionType(domainModel.getType().name());
        entity.setUserId(domainModel.getUserId());
        entity.setTargetDate(domainModel.getTargetDate().toString());
        entity.setFullDay(domainModel.isFullDay());
        entity.setStatus(domainModel.getStatus().name());
        entity.setRequiresApproval(domainModel.requiresApproval());
        entity.setPriority(domainModel.getPriority().name());
        entity.setRecurring(domainModel.isRecurring());
        entity.setActive(domainModel.isActive());
        entity.setCreatedAt(domainModel.getCreatedAt());
        entity.setUpdatedAt(domainModel.getUpdatedAt());

        // Optional fields
        entity.setOriginalShiftId(domainModel.getOriginalShiftId());
        entity.setNewShiftId(domainModel.getNewShiftId());

        if (domainModel.getNewStartTime() != null) {
            entity.setNewStartTime(domainModel.getNewStartTime().toString());
        }
        if (domainModel.getNewEndTime() != null) {
            entity.setNewEndTime(domainModel.getNewEndTime().toString());
        }
        entity.setDurationMinutes(domainModel.getDurationMinutes());

        // Collaboration
        entity.setSwapWithUserId(domainModel.getSwapWithUserId());
        entity.setSwapWithUserName(domainModel.getSwapWithUserName());
        entity.setReplacementUserId(domainModel.getReplacementUserId());
        entity.setReplacementUserName(domainModel.getReplacementUserName());

        // Approval
        entity.setApprovedByUserId(domainModel.getApprovedByUserId());
        entity.setApprovedByUserName(domainModel.getApprovedByUserName());
        if (domainModel.getApprovedDate() != null) {
            entity.setApprovedDate(domainModel.getApprovedDate().toString());
        }
        entity.setRejectionReason(domainModel.getRejectionReason());

        // Recurrence
        entity.setRecurrenceRuleId(domainModel.getRecurrenceRuleId());

        // Metadata (JSON serialization)
        if (!domainModel.getMetadata().isEmpty()) {
            entity.setMetadataJson(QDueTypeConverters.fromStringMap(domainModel.getMetadata()));
        }

        // System data
        entity.setCreatedByUserId(domainModel.getCreatedByUserId());
        entity.setLastModifiedByUserId(domainModel.getLastModifiedByUserId());

        return entity;
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Update timestamp to current time.
     */
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Check if this is an absence type exception.
     */
    public boolean isAbsence() {
        return exceptionType.startsWith("ABSENCE_");
    }

    /**
     * Check if this is a shift change type exception.
     */
    public boolean isShiftChange() {
        return exceptionType.startsWith("CHANGE_");
    }

    /**
     * Check if this is a time reduction type exception.
     */
    public boolean isTimeReduction() {
        return exceptionType.startsWith("REDUCTION_");
    }

    /**
     * Check if exception is currently effective.
     */
    public boolean isEffective() {
        return "APPROVED".equals(status) || (!requiresApproval && "DRAFT".equals(status));
    }

    /**
     * Check if exception involves another user.
     */
    public boolean involvesOtherUser() {
        return swapWithUserId != null || replacementUserId != null;
    }

    // ==================== TOSTRING ====================

    @Override
    @NonNull
    public String toString() {
        return "ShiftExceptionEntity{" +
                "id='" + id + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", userId=" + userId +
                ", targetDate='" + targetDate + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", active=" + active +
                '}';
    }
}