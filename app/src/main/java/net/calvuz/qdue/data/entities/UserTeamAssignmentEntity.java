package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.common.enums.Status;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * UserTeamAssignmentEntity - Room Database Entity for User-Team Assignment Management
 *
 * <p>Data layer entity representing UserTeamAssignment in the database following Room persistence patterns.
 * Provides conversion methods between domain models and database entities for clean architecture separation.</p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li><strong>Table Name</strong>: "user_team_assignments" for clear identification</li>
 *   <li><strong>Primary Key</strong>: String UUID for distributed system compatibility</li>
 *   <li><strong>Indexes</strong>: Optimized for user queries, team queries, and date ranges</li>
 *   <li><strong>Foreign Key References</strong>: String IDs to users and teams</li>
 * </ul>
 *
 * <h3>Performance Indexes:</h3>
 * <ul>
 *   <li><strong>user_id</strong>: Fast lookup of assignments by user</li>
 *   <li><strong>team_id</strong>: Fast lookup of assignments by team</li>
 *   <li><strong>date_range</strong>: Efficient date-based queries</li>
 *   <li><strong>status_active</strong>: Quick filtering by status and active state</li>
 * </ul>
 *
 * <h3>Clean Architecture Bridge:</h3>
 * <ul>
 *   <li><strong>Entity Conversion</strong>: toDomainModel() and fromDomainModel() methods</li>
 *   <li><strong>Data Layer Isolation</strong>: Room annotations isolated from domain</li>
 *   <li><strong>Type Safety</strong>: Enum and LocalDate conversion via QDueTypeConverters</li>
 *   <li><strong>Database Optimization</strong>: Efficient column types and indexing</li>
 * </ul>
 */
@Entity(
        tableName = "user_team_assignments",
        indices = {
                @Index(value = {"user_id"}, name = "idx_user_team_assignments_user_id"),
                @Index(value = {"team_id"}, name = "idx_user_team_assignments_team_id"),
                @Index(value = {"user_id", "team_id"}, name = "idx_user_team_assignments_user_team"),
                @Index(value = {"start_date", "end_date"}, name = "idx_user_team_assignments_date_range"),
                @Index(value = {"status", "active"}, name = "idx_user_team_assignments_status_active"),
                @Index(value = {"assigned_by_user_id"}, name = "idx_user_team_assignments_assigned_by"),
                @Index(value = {"created_at"}, name = "idx_user_team_assignments_created_at")
        }
)
public class UserTeamAssignmentEntity {

    // ==================== DATABASE COLUMNS ====================

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String mId;

    @ColumnInfo(name = "title")
    @Nullable
    private String mTitle;

    @ColumnInfo(name = "description")
    @Nullable
    private String mDescription;

    @ColumnInfo(name = "notes")
    @Nullable
    private String mNotes;

    // ==================== CORE ASSIGNMENT DATA ====================

    /**
     * User ID being assigned to team.
     * String reference to User entity.
     */
    @ColumnInfo(name = "user_id")
    @NonNull
    private String userId;

    /**
     * Team ID for the assignment.
     * String reference to Team entity.
     */
    @ColumnInfo(name = "team_id")
    @NonNull
    private String teamId;

    // ==================== TIME BOUNDARIES ====================

    /**
     * Assignment start date (inclusive).
     * Converted via QDueTypeConverters LocalDate support.
     */
    @ColumnInfo(name = "start_date")
    @NonNull
    private LocalDate startDate;

    /**
     * Assignment end date (inclusive, null = permanent).
     * Converted via QDueTypeConverters LocalDate support.
     */
    @ColumnInfo(name = "end_date")
    @Nullable
    private LocalDate endDate;

    // ==================== STATUS AND METADATA ====================

    /**
     * Assignment status (computed based on dates and active flag).
     * Stored as string via QDueTypeConverters Status enum support.
     */
    @ColumnInfo(name = "status")
    @Nullable
    private Status status;

    /**
     * User ID who created this assignment.
     * String reference to User entity.
     */
    @ColumnInfo(name = "assigned_by_user_id")
    @Nullable
    private String assignedByUserId;

    /**
     * Administrative enable/disable flag.
     * true = enabled, false = disabled/suspended
     */
    @ColumnInfo(name = "active")
    @NonNull
    private Boolean active;

    // ==================== SYSTEM DATA ====================

    @ColumnInfo(name = "created_at")
    @NonNull
    private Long createdAt;

    @ColumnInfo(name = "updated_at")
    @NonNull
    private Long updatedAt;

    @ColumnInfo(name = "created_by_user_id")
    @Nullable
    private String createdByUserId;

    @ColumnInfo(name = "last_modified_by_user_id")
    @Nullable
    private String lastModifiedByUserId;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public UserTeamAssignmentEntity() {
        this.mId = UUID.randomUUID().toString();
        this.active = true;
        this.status = Status.ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Constructor with required fields.
     */
    @Ignore
    public UserTeamAssignmentEntity(@NonNull String userId,
                                    @NonNull String teamId,
                                    @NonNull LocalDate startDate) {
        this();
        this.userId = userId;
        this.teamId = teamId;
        this.startDate = startDate;
    }

    /**
     * Full constructor for complete entity creation.
     */
    @Ignore
    public UserTeamAssignmentEntity(@Nullable String id,
                                    @Nullable String title,
                                    @Nullable String description,
                                    @Nullable String notes,
                                    @NonNull String userId,
                                    @NonNull String teamId,
                                    @NonNull LocalDate startDate,
                                    @Nullable LocalDate endDate,
                                    @Nullable Status status,
                                    @Nullable String assignedByUserId,
                                    @Nullable Boolean active,
                                    @Nullable Long createdAt,
                                    @Nullable Long updatedAt,
                                    @Nullable String createdByUserId,
                                    @Nullable String lastModifiedByUserId) {
        this.mId = id != null ? id : UUID.randomUUID().toString();
        this.mTitle = title;
        this.mDescription = description;
        this.mNotes = notes;
        this.userId = userId;
        this.teamId = teamId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.assignedByUserId = assignedByUserId;
        this.active = active != null ? active : true;
        long currentTime = System.currentTimeMillis();
        this.createdAt = createdAt != null ? createdAt : currentTime;
        this.updatedAt = updatedAt != null ? updatedAt : currentTime;
        this.createdByUserId = createdByUserId;
        this.lastModifiedByUserId = lastModifiedByUserId;
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        this.mId = id;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    public void setTitle(@Nullable String title) {
        this.mTitle = title;
        touch();
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@Nullable String description) {
        this.mDescription = description;
        touch();
    }

    @Nullable
    public String getNotes() {
        return mNotes;
    }

    public void setNotes(@Nullable String notes) {
        this.mNotes = notes;
        touch();
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
        touch();
    }

    @NonNull
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@NonNull String teamId) {
        this.teamId = teamId;
        touch();
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(@NonNull LocalDate startDate) {
        this.startDate = startDate;
        touch();
    }

    @Nullable
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable LocalDate endDate) {
        this.endDate = endDate;
        touch();
    }

    @Nullable
    public Status getStatus() {
        return status;
    }

    public void setStatus(@Nullable Status status) {
        this.status = status;
        touch();
    }

    @Nullable
    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(@Nullable String assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
        touch();
    }

    @NonNull
    public Boolean getActive() {
        return active;
    }

    public void setActive(@NonNull Boolean active) {
        this.active = active;
        touch();
    }

    @NonNull
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull Long createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(@Nullable String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    @Nullable
    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    public void setLastModifiedByUserId(@Nullable String lastModifiedByUserId) {
        this.lastModifiedByUserId = lastModifiedByUserId;
        touch();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Update timestamp when entity is modified.
     */
    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Check if assignment is permanent (no end date).
     */
    public boolean isPermanent() {
        return endDate == null;
    }

    /**
     * Check if assignment is currently active based on dates and flag.
     */
    public boolean isCurrentlyActive() {
        if (!active) {
            return false;
        }

        LocalDate today = LocalDate.now();
        boolean afterStart = !today.isBefore( startDate );
        boolean beforeEnd = endDate == null || !today.isAfter( endDate );

        return afterStart && beforeEnd;
    }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert entity to domain model.
     * Performs clean architecture entity-to-domain conversion.
     *
     * @return UserTeamAssignment domain model
     */
    @NonNull
    public UserTeamAssignment toDomainModel() {
        return UserTeamAssignment.builder()
                .id(mId)
                .title(mTitle)
                .description(mDescription)
                .notes(mNotes)
                .userID( userId )
                .teamID( teamId )
                .startDate( startDate )
                .endDate( endDate )
                .status( status )
                .assignedByUserID( assignedByUserId )
                .active( active )
                .createdAt( createdAt )
                .updatedAt( updatedAt )
                .createdByUserId( createdByUserId )
                .lastModifiedByUserId( lastModifiedByUserId )
                .build();
    }

    /**
     * Create entity from domain model.
     * Performs clean architecture domain-to-entity conversion.
     *
     * @param domainModel UserTeamAssignment domain model
     * @return UserTeamAssignmentEntity database entity
     */
    @NonNull
    public static UserTeamAssignmentEntity fromDomainModel(@NonNull UserTeamAssignment domainModel) {
        return new UserTeamAssignmentEntity(
                domainModel.getId(),
                domainModel.getTitle(),
                domainModel.getDescription(),
                domainModel.getNotes(),
                domainModel.getUserID(),
                domainModel.getTeamID(),
                domainModel.getStartDate(),
                domainModel.getEndDate(),
                domainModel.getStatus(),
                domainModel.getAssignedByUserId(),
                domainModel.isActive(),
                domainModel.getCreatedAt(),
                domainModel.getUpdatedAt(),
                domainModel.getCreatedByUserId(),
                domainModel.getLastModifiedByUserId()
        );
    }

    /**
     * Update entity from domain model (preserving entity metadata).
     *
     * @param domainModel Domain model to update from
     */
    public void updateFromDomainModel(@NonNull UserTeamAssignment domainModel) {
        // Update modifiable fields only
        this.mTitle = domainModel.getTitle();
        this.mDescription = domainModel.getDescription();
        this.mNotes = domainModel.getNotes();
        this.userId = domainModel.getUserID();
        this.teamId = domainModel.getTeamID();
        this.startDate = domainModel.getStartDate();
        this.endDate = domainModel.getEndDate();
        this.status = domainModel.getStatus();
        this.assignedByUserId = domainModel.getAssignedByUserId();
        this.active = domainModel.isActive();
        this.lastModifiedByUserId = domainModel.getLastModifiedByUserId();

        // Always update the timestamp
        touch();
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTeamAssignmentEntity that = (UserTeamAssignmentEntity) o;
        return Objects.equals(mId, that.mId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId);
    }

    // ==================== STRING REPRESENTATION ====================

    @Override
    @NonNull
    public String toString() {
        return "UserTeamAssignmentEntity{" +
                "id='" + mId + '\'' +
                ", userID='" + userId + '\'' +
                ", teamId='" + teamId + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", active=" + active +
                '}';
    }

    @NonNull
    public String toDetailedString() {
        return "UserTeamAssignmentEntity{" +
                "id='" + mId + '\'' +
                ", title='" + mTitle + '\'' +
                ", description='" + mDescription + '\'' +
                ", notes='" + mNotes + '\'' +
                ", userID='" + userId + '\'' +
                ", teamId='" + teamId + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", assignedByUserID='" + assignedByUserId + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdByUserId='" + createdByUserId + '\'' +
                ", lastModifiedByUserId='" + lastModifiedByUserId + '\'' +
                '}';
    }
}