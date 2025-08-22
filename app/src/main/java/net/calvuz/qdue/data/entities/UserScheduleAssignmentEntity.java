package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;

import java.time.LocalDate;

/**
 * UserScheduleAssignmentEntity - Room database entity for user-to-team schedule assignments.
 *
 * <p>Stores the mapping between users and their team assignments with time boundaries,
 * enabling multi-user schedule management and team-based scheduling. Optimized for
 * fast user schedule lookups and team management queries.</p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li><strong>Primary Key</strong>: UUID-based ID for global uniqueness</li>
 *   <li><strong>User-Date Index</strong>: Fast user assignment lookups by date</li>
 *   <li><strong>Team Index</strong>: Team membership and roster queries</li>
 *   <li><strong>Priority Index</strong>: Conflict resolution for overlapping assignments</li>
 *   <li><strong>Status Index</strong>: Assignment lifecycle management</li>
 * </ul>
 *
 * <h3>Query Optimizations:</h3>
 * <ul>
 *   <li>Primary index: (user_id, start_date, end_date, priority) for user schedule queries</li>
 *   <li>Team index: (team_id, status, start_date) for team roster management</li>
 *   <li>Active assignments: (status, start_date, end_date) for current assignments</li>
 *   <li>Priority conflicts: (user_id, priority, status) for conflict resolution</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Engine Database
 * @since Clean Architecture Phase 2
 */
@Entity(
        tableName = "user_schedule_assignments",
        indices = {
                @Index(value = {"user_id", "start_date", "end_date", "priority"}, name = "idx_assignment_user_date_priority"),
                @Index(value = {"team_id", "status", "start_date"}, name = "idx_assignment_team_status"),
                @Index(value = {"status", "start_date", "end_date"}, name = "idx_assignment_status_dates"),
                @Index(value = {"user_id", "priority", "status"}, name = "idx_assignment_user_priority"),
                @Index(value = {"recurrence_rule_id", "active"}, name = "idx_assignment_recurrence"),
                @Index(value = {"assigned_by_user_id", "created_at"}, name = "idx_assignment_created_by")
        }
)
public class UserScheduleAssignmentEntity {

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

    // ==================== CORE ASSIGNMENT DATA ====================

    @NonNull
    @ColumnInfo(name = "user_id")
    private Long userId;

    @Nullable
    @ColumnInfo(name = "user_name")
    private String userName;

    @NonNull
    @ColumnInfo(name = "team_id")
    private String teamId;

    @Nullable
    @ColumnInfo(name = "team_name")
    private String teamName;

    @NonNull
    @ColumnInfo(name = "recurrence_rule_id")
    private String recurrenceRuleId;

    // ==================== TIME BOUNDARIES ====================

    @NonNull
    @ColumnInfo(name = "start_date")
    private String startDate; // ISO format: yyyy-MM-dd

    @Nullable
    @ColumnInfo(name = "end_date")
    private String endDate; // ISO format: yyyy-MM-dd (null = permanent)

    @ColumnInfo(name = "is_permanent", defaultValue = "0")
    private boolean isPermanent;

    // ==================== PRIORITY AND STATUS ====================

    @NonNull
    @ColumnInfo(name = "priority", defaultValue = "NORMAL")
    private String priority; // LOW, NORMAL, HIGH, OVERRIDE

    @NonNull
    @ColumnInfo(name = "status", defaultValue = "ACTIVE")
    private String status; // ACTIVE, PENDING, EXPIRED, SUSPENDED, CANCELLED

    // ==================== METADATA ====================

    @Nullable
    @ColumnInfo(name = "assigned_by_user_id")
    private String assignedByUserId;

    @Nullable
    @ColumnInfo(name = "assigned_by_user_name")
    private String assignedByUserName;

    @Nullable
    @ColumnInfo(name = "department_id")
    private String departmentId;

    @Nullable
    @ColumnInfo(name = "department_name")
    private String departmentName;

    @Nullable
    @ColumnInfo(name = "role_id")
    private String roleId;

    @Nullable
    @ColumnInfo(name = "role_name")
    private String roleName;

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
    public UserScheduleAssignmentEntity() {
        // Room requires a no-argument constructor
    }

    /**
     * Constructor with required fields.
     */
    public UserScheduleAssignmentEntity(@NonNull String id, @NonNull Long userId,
                                        @NonNull String teamId, @NonNull String recurrenceRuleId,
                                        @NonNull String startDate) {
        this.id = id;
        this.userId = userId;
        this.teamId = teamId;
        this.recurrenceRuleId = recurrenceRuleId;
        this.startDate = startDate;
        this.isPermanent = true; // Default to permanent if no end date
        this.priority = "NORMAL";
        this.status = "ACTIVE";
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
    public Long getUserId() { return userId; }
    public void setUserId(@NonNull Long userId) { this.userId = userId; }

    @Nullable
    public String getUserName() { return userName; }
    public void setUserName(@Nullable String userName) { this.userName = userName; }

    @NonNull
    public String getTeamId() { return teamId; }
    public void setTeamId(@NonNull String teamId) { this.teamId = teamId; }

    @Nullable
    public String getTeamName() { return teamName; }
    public void setTeamName(@Nullable String teamName) { this.teamName = teamName; }

    @NonNull
    public String getRecurrenceRuleId() { return recurrenceRuleId; }
    public void setRecurrenceRuleId(@NonNull String recurrenceRuleId) { this.recurrenceRuleId = recurrenceRuleId; }

    @NonNull
    public String getStartDate() { return startDate; }
    public void setStartDate(@NonNull String startDate) { this.startDate = startDate; }

    @Nullable
    public String getEndDate() { return endDate; }
    public void setEndDate(@Nullable String endDate) { this.endDate = endDate; }

    public boolean isPermanent() { return isPermanent; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }

    @NonNull
    public String getPriority() { return priority; }
    public void setPriority(@NonNull String priority) { this.priority = priority; }

    @NonNull
    public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    @Nullable
    public String getAssignedByUserId() { return assignedByUserId; }
    public void setAssignedByUserId(@Nullable String assignedByUserId) { this.assignedByUserId = assignedByUserId; }

    @Nullable
    public String getAssignedByUserName() { return assignedByUserName; }
    public void setAssignedByUserName(@Nullable String assignedByUserName) { this.assignedByUserName = assignedByUserName; }

    @Nullable
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(@Nullable String departmentId) { this.departmentId = departmentId; }

    @Nullable
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(@Nullable String departmentName) { this.departmentName = departmentName; }

    @Nullable
    public String getRoleId() { return roleId; }
    public void setRoleId(@Nullable String roleId) { this.roleId = roleId; }

    @Nullable
    public String getRoleName() { return roleName; }
    public void setRoleName(@Nullable String roleName) { this.roleName = roleName; }

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
    public UserScheduleAssignment toDomainModel() {
        UserScheduleAssignment.Builder builder = UserScheduleAssignment.builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .notes(this.notes)
                .userId(this.userId)
                .userName(this.userName)
                .teamId(this.teamId)
                .teamName(this.teamName)
                .recurrenceRuleId(this.recurrenceRuleId)
                .startDate(LocalDate.parse(this.startDate))
                .priority(UserScheduleAssignment.Priority.valueOf(this.priority))
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);

        // Optional end date
        if (this.endDate != null) {
            builder.endDate(LocalDate.parse(this.endDate));
        }

        // Optional metadata
        builder.assignedByUserId(this.assignedByUserId)
                .assignedByUserName(this.assignedByUserName)
                .departmentId(this.departmentId)
                .departmentName(this.departmentName)
                .roleId(this.roleId)
                .roleName(this.roleName);

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
    public static UserScheduleAssignmentEntity fromDomainModel(@NonNull UserScheduleAssignment domainModel) {
        UserScheduleAssignmentEntity entity = new UserScheduleAssignmentEntity();

        // Basic fields
        entity.setId(domainModel.getId());
        entity.setTitle(domainModel.getTitle());
        entity.setDescription(domainModel.getDescription());
        entity.setNotes(domainModel.getNotes());
        entity.setUserId(domainModel.getUserId());
        entity.setUserName(domainModel.getUserName());
        entity.setTeamId(domainModel.getTeamId());
        entity.setTeamName(domainModel.getTeamName());
        entity.setRecurrenceRuleId(domainModel.getRecurrenceRuleId());
        entity.setStartDate(domainModel.getStartDate().toString());
        entity.setPermanent(domainModel.isPermanent());
        entity.setPriority(domainModel.getPriority().name());
        entity.setStatus(domainModel.getStatus().name());
        entity.setActive(domainModel.isActive());
        entity.setCreatedAt(domainModel.getCreatedAt());
        entity.setUpdatedAt(domainModel.getUpdatedAt());

        // Optional end date
        if (domainModel.getEndDate() != null) {
            entity.setEndDate(domainModel.getEndDate().toString());
        }

        // Metadata
        entity.setAssignedByUserId(domainModel.getAssignedByUserId());
        entity.setAssignedByUserName(domainModel.getAssignedByUserName());
        entity.setDepartmentId(domainModel.getDepartmentId());
        entity.setDepartmentName(domainModel.getDepartmentName());
        entity.setRoleId(domainModel.getRoleId());
        entity.setRoleName(domainModel.getRoleName());

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
     * Check if assignment is currently active based on dates and status.
     */
    public boolean isCurrentlyActive() {
        if (!active || !"ACTIVE".equals(status)) {
            return false;
        }

        String today = LocalDate.now().toString();

        // Check if started
        if (startDate.compareTo(today) > 0) {
            return false; // Future assignment
        }

        // Check if ended (if not permanent)
        if (!isPermanent && endDate != null && endDate.compareTo(today) < 0) {
            return false; // Past assignment
        }

        return true;
    }

    /**
     * Check if assignment is in the future.
     */
    public boolean isFuture() {
        String today = LocalDate.now().toString();
        return startDate.compareTo(today) > 0;
    }

    /**
     * Check if assignment has expired.
     */
    public boolean isExpired() {
        if (isPermanent) {
            return false;
        }

        String today = LocalDate.now().toString();
        return endDate != null && endDate.compareTo(today) < 0;
    }

    /**
     * Get assignment duration in days.
     * Returns -1 for permanent assignments.
     */
    public long getDurationDays() {
        if (isPermanent || endDate == null) {
            return -1; // Permanent
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1; // +1 because both dates are inclusive
    }

    /**
     * Check if this assignment conflicts with another by date range.
     */
    public boolean conflictsWith(@NonNull UserScheduleAssignmentEntity other) {
        // Must be same user and both active
        if (!this.userId.equals(other.userId) || !this.active || !other.active) {
            return false;
        }

        // Date range overlap check
        LocalDate thisStart = LocalDate.parse(this.startDate);
        LocalDate thisEnd = this.endDate != null ? LocalDate.parse(this.endDate) : LocalDate.MAX;

        LocalDate otherStart = LocalDate.parse(other.startDate);
        LocalDate otherEnd = other.endDate != null ? LocalDate.parse(other.endDate) : LocalDate.MAX;

        // Check for overlap: (thisStart <= otherEnd) && (thisEnd >= otherStart)
        return !thisStart.isAfter(otherEnd) && !thisEnd.isBefore(otherStart);
    }

    /**
     * Get priority level as integer for comparison.
     */
    public int getPriorityLevel() {
        switch (priority) {
            case "LOW": return 1;
            case "NORMAL": return 5;
            case "HIGH": return 8;
            case "OVERRIDE": return 10;
            default: return 5; // Default to NORMAL
        }
    }

    // ==================== TOSTRING ====================

    @Override
    @NonNull
    public String toString() {
        return "UserScheduleAssignmentEntity{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", teamId='" + teamId + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", permanent=" + isPermanent +
                ", active=" + active +
                '}';
    }
}