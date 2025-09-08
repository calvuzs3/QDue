package net.calvuz.qdue.domain.qdueuser.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * QDueUser - Simplified User Domain Model for Google Calendar-like Management
 *
 * <p>Clean architecture domain model representing a simplified user with minimal required data.
 * Designed for streamlined onboarding and user management with optional fields defaulting to empty strings.</p>
 *
 * <h3>Domain Model Features:</h3>
 * <ul>
 *   <li><strong>Minimal Design</strong>: Only essential fields (id, nickname, email)</li>
 *   <li><strong>Optional Fields</strong>: All fields optional in onboarding, defaults to empty strings</li>
 *   <li><strong>Auto-increment ID</strong>: Numeric ID starting from 1L</li>
 *   <li><strong>Email Validation</strong>: Built-in email format validation when present</li>
 *   <li><strong>Immutable Values</strong>: Thread-safe with proper equals/hashCode</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li>ID is UUID</li>
 *   <li>Nickname can be empty string (user choice in onboarding)</li>
 *   <li>Email can be empty string but must be valid format if provided</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Simplified User Model
 * @since Clean Architecture Phase 3
 */
public class QDueUser {

    private static final String TAG = "QDueUser";

    // ==================== DOMAIN PROPERTIES ====================

    private final String id;
    private final String nickname;
    private final String email;

    // ==================== METADATA ====================

    private final long createdAt;
    private final long updatedAt;

    // ==================== EMAIL VALIDATION ====================

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor for existing user (with ID from database).
     * Validation is in the build()
     *
     * @param builder Builder object for user initialization
     */
    public QDueUser(@NonNull Builder builder) {
        this.id = builder.id;
        this.nickname = builder.nickname;
        this.email = builder.email;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getNickname() {
        return nickname;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public Long getCreatedAt() {
        return createdAt;
    }

    @NonNull
    public Long getUpdatedAt() {
        return updatedAt;
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if user has a valid nickname (not empty).
     *
     * @return true if nickname is not empty
     */
    public boolean hasNickname() {
        return !nickname.isEmpty();
    }

    /**
     * Check if user has a valid email (not empty).
     *
     * @return true if email is not empty
     */
    public boolean hasEmail() {
        return !email.isEmpty();
    }

    /**
     * Validate email format if email is provided.
     *
     * @return true if email is empty or valid format
     */
    public boolean isEmailValid() {
        if (email.isEmpty()) {
            return true; // Empty email is valid (optional field)
        }
        return EMAIL_PATTERN.matcher( email ).matches();
    }

    /**
     * Get display name for UI (nickname if available, otherwise "User" + ID).
     *
     * @return Display name for UI
     */
    @NonNull
    public String getDisplayName() {
        if (hasNickname()) {
            return nickname;
        }
        return "User" + (id != null ? " " + id : "");
    }

    // ==================== BUILDER ====================

    @NonNull
    public static QDueUser.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id = UUID.randomUUID().toString();
        private String nickname = "";
        private String email = "";
        private long createdAt = System.currentTimeMillis();
        private long updatedAt = System.currentTimeMillis();

        @NonNull
        public QDueUser.Builder copyFrom(@NonNull QDueUser source) {
            this.id = source.id;
            this.nickname = source.nickname;
            this.email = source.email;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;
            return this;
        }

        @NonNull
        public QDueUser.Builder id(@Nullable String id) {
            if (id != null) this.id = id;
            return this;
        }

        @NonNull
        public QDueUser.Builder nickname(@Nullable String nickname) {
            if (nickname != null) this.nickname = nickname;
            return this;
        }

        @NonNull
        public QDueUser.Builder email(@Nullable String email) {
            if (email != null) this.email = email;
            return this;
        }

        @NonNull
        public QDueUser.Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NonNull
        public QDueUser.Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @NonNull
        public QDueUser build() {
            assert id != null;
            assert nickname != null;
            assert email != null;
            return new QDueUser( this );
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        QDueUser qDueUser = (QDueUser) obj;
        return Objects.equals( id, qDueUser.id ) &&
                Objects.equals( nickname, qDueUser.nickname ) &&
                Objects.equals( email, qDueUser.email );
    }

    @Override
    public int hashCode() {
        return Objects.hash( id, nickname, email );
    }

    @NonNull
    @Override
    public String toString() {
        return "QDueUser{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    // ==================== DEBUG METHODS ====================

    @NonNull
    public String toDetailedString() {
        return "QDueUser{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + LocalDateTime.ofInstant( Instant.ofEpochMilli( createdAt ), ZoneId.systemDefault() ) +
                ", updatedAt=" + LocalDateTime.ofInstant( Instant.ofEpochMilli( updatedAt ), ZoneId.systemDefault() ) +
                '}';
    }
}