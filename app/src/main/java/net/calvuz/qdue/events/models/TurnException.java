package net.calvuz.qdue.events.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.user.data.entities.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing exceptions to the standard 4-2 shift pattern.
 * Stores user-specific modifications like vacation days, overtime, sick leave, etc.
 * <p>
 * Integration with Virtual Scrolling:
 * - Exceptions are merged with base pattern during data loading
 * - Supports reversible changes (delete exception = restore original pattern)
 * - Optimized indexes for fast month-based queries
 */
@Entity(
        tableName = "turn_exceptions",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"user_id", "date"}, unique = true), // One exception per user per date
                @Index(value = {"date", "exception_type"}), // Fast filtering by type
                @Index(value = {"date"}), // Calendar month queries
                @Index(value = {"user_id"}), // User-specific queries
                @Index(value = {"created_at"}) // Recent changes tracking
        }
)
public class TurnException {

    // Primary key
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    // User reference (FK to User table)
    @ColumnInfo(name = "user_id")
    private long userId;

    // Date of the exception
    @NonNull
    @ColumnInfo(name = "date")
    private LocalDate date;

    // Type of exception
    @NonNull
    @ColumnInfo(name = "exception_type")
    private ExceptionType exceptionType;

    // Original shift type (for restoration)
    @ColumnInfo(name = "original_shift_type")
    private String originalShiftType;

    // Optional replacement shift type
    @ColumnInfo(name = "replacement_shift_type")
    private String replacementShiftType;

    // User notes
    @ColumnInfo(name = "notes")
    private String notes;

    // Timestamps
    @NonNull
    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnInfo(name = "modified_at")
    private LocalDateTime modifiedAt;

    // Status for future features
    @ColumnInfo(name = "status", defaultValue = "ACTIVE")
    private ExceptionStatus status;

    /**
     * Exception types supported by the system
     */
    public enum ExceptionType {
        VACATION("Ferie"),
        SICK_LEAVE("Malattia"),
        OVERTIME("Straordinario"),
        PERMIT("Permesso"),
        PERMIT_104("Permesso 104"),
        PERMIT_SYNDICATE("Permesso sindacale"),
        TRAINING("Formazione"),
        PERSONAL_LEAVE("Permesso personale"),
        COMPENSATION("Recupero"),
        SHIFT_SWAP("Cambio turno"),
        EMERGENCY("Emergenza"),
        OTHER("Altro");

        private final String displayName;

        ExceptionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Exception status for workflow management
     */
    public enum ExceptionStatus {
        ACTIVE("Attiva"),
        PENDING("In attesa"),
        APPROVED("Approvata"),
        REJECTED("Rifiutata"),
        CANCELLED("Cancellata");

        private final String displayName;

        ExceptionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room
     */
    public TurnException() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = ExceptionStatus.ACTIVE;
    }

    /**
     * Create new exception with required fields
     */
    public TurnException(long userId, @NonNull LocalDate date,
                         @NonNull ExceptionType exceptionType, String originalShiftType) {
        this();
        this.userId = userId;
        this.date = date;
        this.exceptionType = exceptionType;
        this.originalShiftType = originalShiftType;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this exception can be reversed (has original shift type)
     */
    public boolean isReversible() {
        return originalShiftType != null && !originalShiftType.trim().isEmpty();
    }

    /**
     * Check if this exception is currently active
     */
    public boolean isActive() {
        return status == ExceptionStatus.ACTIVE;
    }

    /**
     * Check if this exception affects work schedule
     */
    public boolean affectsWorkSchedule() {
        return exceptionType == ExceptionType.VACATION ||
                exceptionType == ExceptionType.SICK_LEAVE ||
                exceptionType == ExceptionType.PERSONAL_LEAVE ||
                exceptionType == ExceptionType.PERMIT_104 ||
                exceptionType == ExceptionType.PERMIT ||
                exceptionType == ExceptionType.PERMIT_SYNDICATE;
    }

    /**
     * Get effective shift type (replacement or null if no work)
     */
    public String getEffectiveShiftType() {
        if (affectsWorkSchedule()) {
            return null; // No work on vacation/sick days
        }
        return replacementShiftType != null ? replacementShiftType : originalShiftType;
    }

    /**
     * Update modification timestamp
     */
    public void markModified() {
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * Create a copy for editing
     */
    public TurnException copy() {
        TurnException copy = new TurnException();
        copy.id = UUID.randomUUID().toString(); // New ID for copy
        copy.userId = this.userId;
        copy.date = this.date;
        copy.exceptionType = this.exceptionType;
        copy.originalShiftType = this.originalShiftType;
        copy.replacementShiftType = this.replacementShiftType;
        copy.notes = this.notes;
        copy.createdAt = LocalDateTime.now();
        copy.status = this.status;
        return copy;
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
    }

    @NonNull
    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(@NonNull ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getOriginalShiftType() {
        return originalShiftType;
    }

    public void setOriginalShiftType(String originalShiftType) {
        this.originalShiftType = originalShiftType;
    }

    public String getReplacementShiftType() {
        return replacementShiftType;
    }

    public void setReplacementShiftType(String replacementShiftType) {
        this.replacementShiftType = replacementShiftType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @NonNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public ExceptionStatus getStatus() {
        return status;
    }

    public void setStatus(ExceptionStatus status) {
        this.status = status;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TurnException that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    @Override
    public String toString() {
        return "TurnException{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", date=" + date +
                ", exceptionType=" + exceptionType +
                ", originalShiftType='" + originalShiftType + '\'' +
                ", replacementShiftType='" + replacementShiftType + '\'' +
                ", status=" + status +
                '}';
    }
}