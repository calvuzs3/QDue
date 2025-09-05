package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.enums.Status;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class UserTeamAssignment extends LocalizableDomainModel {

    private static final String TAG = "UserTeamAssignment";

    private static final String LOCALIZATION_SCOPE = "user_team_assignment";

    // ==================== IDENTIFICATION ====================

    private final String id;
    private final String title;
    private final String description;
    private final String notes;

    // ==================== CORE ASSIGNMENT DATA ====================

    private final String userID;                    // User being assigned
    private final String teamID;                    // Team assignment

    // ==================== TIME BOUNDARIES ====================

    private final LocalDate startDate;              // Assignment start (inclusive)
    private final LocalDate endDate;                // Assignment end (inclusive, null = permanent)
    //private final boolean isPermanent;            // True if no end date

    // ==================== PRIORITY AND STATUS ====================

    private final Status status;                    // COMPUTED - never set manually

    // ==================== METADATA ====================

    private final String assignedByUserId;          // Who created this assignment

    // ==================== SYSTEM DATA ====================

    private final long createdAt;
    private final long updatedAt;
    private final String createdByUserId;
    private final String lastModifiedByUserId;

    /**
     * Administrative enable/disable flag.
     * <p>
     * - true: Assignment is enabled and should be processed
     * - false: Assignment is disabled (soft delete, administrative suspension)
     * <p>
     * This is independent of dates - a future assignment can be active=true
     */
    private final boolean active;

    // ==================== CONSTRUCTOR ====================

    private UserTeamAssignment(@NonNull Builder builder) {
        super( builder.mLocalizer, LOCALIZATION_SCOPE );

        // Identification
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.title = builder.title;
        this.description = builder.description;
        this.notes = builder.notes;
        this.userID = Objects.requireNonNull( builder.userID, "User ID cannot be null" );
        this.teamID = Objects.requireNonNull( builder.teamID, "Team ID cannot be null" );
        this.startDate = Objects.requireNonNull( builder.startDate, "Start date cannot be null" );
        this.endDate = builder.endDate;
        this.status = builder.status;
        this.assignedByUserId = builder.assignedByUserID;
        this.active = builder.active;
        this.createdAt = builder.createdAt > 0 ? builder.createdAt : System.currentTimeMillis();
        this.updatedAt = builder.updatedAt > 0 ? builder.updatedAt : System.currentTimeMillis();
        this.createdByUserId = builder.createdByUserId;
        this.lastModifiedByUserId = builder.lastModifiedByUserId;
    }

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Check if this instance has localization support.
     *
     * @return true if localizer is available
     */
    @Override
    public boolean hasLocalizationSupport() {
        return super.hasLocalizationSupport();
    }

    /**
     * Create a copy of this object with localizer injected.
     * Useful for adding localization to existing instances.
     *
     * @param localizer DomainLocalizer to inject
     * @return New instance with localizer support
     */
    @NonNull
    @Override
    public UserTeamAssignment withLocalizer(@NonNull DomainLocalizer localizer) {

        return builder()
                .copyFrom( this )
                .localizer( localizer )
                .build();
    }

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getNotes() {
        return notes;
    }

    public String getUserID() {
        return userID;
    }

    public String getTeamID() {
        return teamID;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Status getStatus() {
        return status;
    }

    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    public boolean isActive() {
        return active;
    }

    // ==================== BUILDER ====================

    @NonNull
    public static UserTeamAssignment.Builder builder() {
        return new UserTeamAssignment.Builder();
    }

    public static class Builder extends LocalizableBuilder<UserTeamAssignment, UserTeamAssignment.Builder> {
        private String id;
        private String title;
        private String description;
        private String notes;
        private String userID;
        private String teamID;
        private LocalDate startDate;
        private LocalDate endDate;
        private Status status;
        private String assignedByUserID;
        private boolean active = true;
        private long createdAt;
        private long updatedAt;
        private String createdByUserId;
        private String lastModifiedByUserId;

        @NonNull
        public UserTeamAssignment.Builder copyFrom(@NonNull UserTeamAssignment source) {
            this.id = source.id;
            this.title = source.title;
            this.description = source.description;
            this.notes = source.notes;
            this.userID = source.userID;
            this.teamID = source.teamID;
            this.startDate = source.startDate;
            this.endDate = source.endDate;
            this.status = source.status;
            this.assignedByUserID = source.assignedByUserId;
            this.active = source.active;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;
            this.createdByUserId = source.createdByUserId;
            this.lastModifiedByUserId = source.lastModifiedByUserId;

            return copyLocalizableFrom( source );
        }

        @NonNull
        public UserTeamAssignment.Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder notes(@Nullable String notes) {
            this.notes = notes;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder userID(@NonNull String userId) {
            this.userID = userId;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder teamID(@NonNull String teamId) {
            this.teamID = teamId;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder startDate(@NonNull LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder endDate(@Nullable LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder assignedByUserID(@Nullable String assignedByUserId) {
            this.assignedByUserID = assignedByUserId;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder status(@NonNull Status status) {
            this.status = status;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder active(boolean active) {
            this.active = active;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder createdByUserId(@Nullable String createdByUserId) {
            this.createdByUserId = createdByUserId;
            return this;
        }

        @NonNull
        public UserTeamAssignment.Builder lastModifiedByUserId(@Nullable String lastModifiedByUserId) {
            this.lastModifiedByUserId = lastModifiedByUserId;
            return this;
        }

        public UserTeamAssignment.Builder computeStatus() {
            this.status = Status.computeStatus(this.startDate, this.endDate, this.active);
            return this;
        }

        @Override
        @NonNull
        protected UserTeamAssignment.Builder self() {
            return this;
        }

        @Override
        @NonNull
        public UserTeamAssignment build() {
            return new UserTeamAssignment( this );
        }
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTeamAssignment that = (UserTeamAssignment) o;
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
                ", title='" + title + '\'' +
                ", userID=" + userID +
                ", teamID='" + teamID + '\'' +
                '}';
    }

    @NonNull
    public String toDetailedString() {
        return "UserScheduleAssignment{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", notes='" + notes + '\'' +
                ", userID=" + userID +
                ", teamID='" + teamID + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", assignedByUserID='" + assignedByUserId + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdByUserId=" + createdByUserId +
                ", lastModifiedByUserId=" + lastModifiedByUserId +
                '}';
    }
}
