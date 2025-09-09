package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.enums.Pattern;
import net.calvuz.qdue.domain.common.enums.Status;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class UserPatternAssignment
{
    // ==================== IDENTIFICATION ====================

    private final String id;
    private final String title;
    private final String description;
    private final String notes;

    // ==================== CORE ASSIGNMENT DATA ====================

    private final String userID;                    // User being assigned
    private final Pattern pattern;                    // Team assignment

    // ==================== TIME BOUNDARIES ====================

    private final LocalDate startDate;              // Assignment start (inclusive)
    private final LocalDate endDate;                // Assignment end (inclusive, null = permanent)

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

    private UserPatternAssignment(
            @NonNull UserPatternAssignment.Builder builder
    ) {
        this.id = builder.getId() != null ? builder.getId() : UUID.randomUUID().toString();
        this.title = builder.getTitle();
        this.description = builder.getDescription();
        this.notes = builder.getNotes();
        this.userID = Objects.requireNonNull( builder.getAssignedByUserID(),
                                              "User ID cannot be null" );
        this.pattern = Objects.requireNonNull( builder.getPattern(), "Pattern cannot be null" );
        this.startDate = Objects.requireNonNull( builder.getStartDate(),
                                                 "Start date cannot be null" );
        this.endDate = builder.getEndDate();
        this.status = builder.getStatus();
        this.assignedByUserId = builder.getAssignedByUserID();
        this.active = builder.isActive();
        this.createdAt = builder.getCreatedAt() > 0 ? builder.getCreatedAt() : System.currentTimeMillis();
        this.updatedAt = builder.getCreatedAt() > 0 ? builder.getUpdatedAt() : System.currentTimeMillis();
        this.createdByUserId = builder.getCreatedByUserId();
        this.lastModifiedByUserId = builder.getLastModifiedByUserId();
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

    public Pattern getPattern() {
        return pattern;
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
    public static UserPatternAssignment.Builder builder() {
        return new UserPatternAssignment.Builder();
    }

    public static class Builder
    {
        private String id;
        private String title;
        private String description;
        private String notes;
        private String userID;
        private Pattern pattern;
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
        public UserPatternAssignment.Builder copyFrom(@NonNull UserPatternAssignment source) {
            this.id = source.getId();
            this.title = source.getTitle();
            this.description = source.getDescription();
            this.notes = source.getNotes();
            this.userID = source.getUserID();
            this.pattern = source.getPattern();
            this.startDate = source.getStartDate();
            this.endDate = source.getEndDate();
            this.status = source.getStatus();
            this.assignedByUserID = source.getAssignedByUserId();
            this.active = source.isActive();
            this.createdAt = source.getCreatedAt();
            this.updatedAt = source.getUpdatedAt();
            this.createdByUserId = source.getCreatedByUserId();
            this.lastModifiedByUserId = source.getLastModifiedByUserId();

            return this;
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

        public Pattern getPattern() {
            return pattern;
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

        public String getAssignedByUserID() {
            return assignedByUserID;
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

        public String getCreatedByUserId() {
            return createdByUserId;
        }

        public String getLastModifiedByUserId() {
            return lastModifiedByUserId;
        }

        // ==================== BUILDER CHAINS METHODS ====================

        @NonNull
        public UserPatternAssignment.Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder notes(@Nullable String notes) {
            this.notes = notes;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder userID(@NonNull String userId) {
            this.userID = userId;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder pattern(@NonNull Pattern pattern) {
            this.pattern = pattern;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder startDate(@NonNull LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder endDate(@Nullable LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder assignedByUserID(@Nullable String assignedByUserId) {
            this.assignedByUserID = assignedByUserId;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder status(@NonNull Status status) {
            this.status = status;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder active(boolean active) {
            this.active = active;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder createdByUserId(@Nullable String createdByUserId) {
            this.createdByUserId = createdByUserId;
            return this;
        }

        @NonNull
        public UserPatternAssignment.Builder lastModifiedByUserId(@Nullable String lastModifiedByUserId) {
            this.lastModifiedByUserId = lastModifiedByUserId;
            return this;
        }

        public UserPatternAssignment.Builder computeStatus() {
            this.status = Status.computeStatus( this.startDate, this.endDate, this.active );
            return this;
        }

        @NonNull
        public UserPatternAssignment build() {
            return new UserPatternAssignment( this );
        }
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPatternAssignment that = (UserPatternAssignment) o;
        return Objects.equals( id, that.id );
    }

    @Override
    public int hashCode(

    ) {
        return Objects.hash( id );
    }

    @Override
    @NonNull
    public String toString(

    ) {
        return "PatternAssignment{" +
                "id='" + getId() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", userID=" + getUserID() +
                ", pattern='" + getPattern().name() + '\'' +
                '}';
    }

}
