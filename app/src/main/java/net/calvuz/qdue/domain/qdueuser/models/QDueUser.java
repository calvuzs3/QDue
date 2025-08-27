package net.calvuz.qdue.domain.qdueuser.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
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
 *   <li>ID is auto-generated, starting from 1L</li>
 *   <li>Nickname can be empty string (user choice in onboarding)</li>
 *   <li>Email can be empty string but must be valid format if provided</li>
 *   <li>No complex profile data or authentication providers</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Simplified User Model
 * @since Clean Architecture Phase 3
 */
public class QDueUser {

    private static final String TAG = "QDueUser";

    // ==================== EMAIL VALIDATION ====================

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // ==================== DOMAIN PROPERTIES ====================

    private final Long mId;
    private final String mNickname;
    private final String mEmail;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor for existing user (with ID from database).
     *
     * @param id       User ID (auto-generated)
     * @param nickname User nickname (can be empty)
     * @param email    User email (can be empty, must be valid if present)
     */
    public QDueUser(Long id, @NonNull String nickname, @NonNull String email) {
        this.mId = id;
        this.mNickname = nickname;
        this.mEmail = email;
    }

    /**
     * Constructor for new user (without ID for creation).
     *
     * @param nickname User nickname (can be empty)
     * @param email    User email (can be empty, must be valid if present)
     */
    public QDueUser(@NonNull String nickname, @NonNull String email) {
        this(null, nickname, email);
    }

    /**
     * Constructor for onboarding with empty defaults.
     * Creates user with empty nickname and email for minimal onboarding.
     */
    public QDueUser() {
        this("", "");
    }

    // ==================== GETTERS ====================

    @Nullable
    public Long getId() {
        return mId;
    }

    @NonNull
    public String getNickname() {
        return mNickname;
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if user has a valid nickname (not empty).
     *
     * @return true if nickname is not empty
     */
    public boolean hasNickname() {
        return !mNickname.isEmpty();
    }

    /**
     * Check if user has a valid email (not empty).
     *
     * @return true if email is not empty
     */
    public boolean hasEmail() {
        return !mEmail.isEmpty();
    }

    /**
     * Validate email format if email is provided.
     *
     * @return true if email is empty or valid format
     */
    public boolean isEmailValid() {
        if (mEmail.isEmpty()) {
            return true; // Empty email is valid (optional field)
        }
        return EMAIL_PATTERN.matcher(mEmail).matches();
    }

    /**
     * Check if user profile is complete (has both nickname and email).
     *
     * @return true if both nickname and email are provided
     */
    public boolean isProfileComplete() {
        return hasNickname() && hasEmail() && isEmailValid();
    }

    /**
     * Get display name for UI (nickname if available, otherwise "User" + ID).
     *
     * @return Display name for UI
     */
    @NonNull
    public String getDisplayName() {
        if (hasNickname()) {
            return mNickname;
        }
        return "User" + (mId != null ? " " + mId : "");
    }

    // ==================== BUILDER PATTERN FOR UPDATES ====================

    /**
     * Create builder for updating user properties.
     *
     * @return QDueUserBuilder for fluent updates
     */
    public QDueUserBuilder toBuilder() {
        return new QDueUserBuilder()
                .setId(mId)
                .setNickname(mNickname)
                .setEmail(mEmail);
    }

    /**
     * Builder pattern for creating/updating QDueUser instances.
     */
    public static class QDueUserBuilder {
        private Long mId;
        private String mNickname = "";
        private String mEmail = "";

        public QDueUserBuilder setId(Long id) {
            this.mId = id;
            return this;
        }

        public QDueUserBuilder setNickname(@NonNull String nickname) {
            this.mNickname = nickname.trim();
            return this;
        }

        public QDueUserBuilder setEmail(@NonNull String email) {
            if (!email.trim().isEmpty() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                return this;
            }
            this.mEmail = email.trim();
            return this;
        }

        public QDueUser build() {
            return new QDueUser(mId, mNickname, mEmail);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        QDueUser qDueUser = (QDueUser) obj;
        return Objects.equals(mId, qDueUser.mId) &&
                Objects.equals(mNickname, qDueUser.mNickname) &&
                Objects.equals(mEmail, qDueUser.mEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mNickname, mEmail);
    }

    @NonNull
    @Override
    public String toString() {
        return "QDueUser{" +
                "id=" + mId +
                ", nickname='" + mNickname + '\'' +
                ", email='" + mEmail + '\'' +
                ", profileComplete=" + isProfileComplete() +
                '}';
    }
}