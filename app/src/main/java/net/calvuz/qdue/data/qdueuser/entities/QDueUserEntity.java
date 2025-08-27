package net.calvuz.qdue.data.qdueuser.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.qdueuser.models.QDueUser;

import java.util.Objects;

/**
 * QDueUserEntity - Room Database Entity for Simplified User Management
 *
 * <p>Data layer entity representing QDueUser in the database following Room persistence patterns.
 * Provides conversion methods between domain models and database entities for clean architecture separation.</p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li><strong>Table Name</strong>: "qdueuser" for clear identification</li>
 *   <li><strong>Primary Key</strong>: Auto-increment Long ID starting from 1</li>
 *   <li><strong>Columns</strong>: id, nickname, email (all simple types)</li>
 *   <li><strong>Constraints</strong>: Non-null columns with empty string defaults</li>
 * </ul>
 *
 * <h3>Clean Architecture Bridge:</h3>
 * <ul>
 *   <li><strong>Entity Conversion</strong>: toDomainModel() and fromDomainModel() methods</li>
 *   <li><strong>Data Layer Isolation</strong>: Room annotations isolated from domain</li>
 *   <li><strong>Type Safety</strong>: Proper null handling and validation</li>
 *   <li><strong>Database Optimization</strong>: Efficient column types and indexing</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Data Entity
 * @since Clean Architecture Phase 3
 */
@Entity(tableName = "qdueuser")
public class QDueUserEntity {

    // ==================== DATABASE COLUMNS ====================

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private Long mId;

    @ColumnInfo(name = "nickname")
    @NonNull
    private String mNickname;

    @ColumnInfo(name = "email")
    @NonNull
    private String mEmail;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public QDueUserEntity() {
        this.mNickname = "";
        this.mEmail = "";
    }

    /**
     * Constructor with all fields.
     *
     * @param id       User ID (null for new entities)
     * @param nickname User nickname
     * @param email    User email
     */
    public QDueUserEntity(@Nullable Long id, @NonNull String nickname, @NonNull String email) {
        this.mId = id;
        this.mNickname = nickname;
        this.mEmail = email;
    }

    /**
     * Constructor for new entity (without ID).
     *
     * @param nickname User nickname
     * @param email    User email
     */
    public QDueUserEntity(@NonNull String nickname, @NonNull String email) {
        this(null, nickname, email);
    }

    // ==================== GETTERS AND SETTERS ====================

    @Nullable
    public Long getId() {
        return mId;
    }

    public void setId(@Nullable Long id) {
        this.mId = id;
    }

    @NonNull
    public String getNickname() {
        return mNickname;
    }

    public void setNickname(@NonNull String nickname) {
        this.mNickname = nickname;
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    public void setEmail(@NonNull String email) {
        this.mEmail = email;
    }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert database entity to domain model.
     * Creates QDueUser from database representation.
     *
     * @return QDueUser domain model
     */
    @NonNull
    public QDueUser toDomainModel() {
        return new QDueUser(mId, mNickname, mEmail);
    }

    /**
     * Convert domain model to database entity.
     * Creates QDueUserEntity from domain representation.
     *
     * @param qDueUser Domain model to convert
     * @return QDueUserEntity for database storage
     */
    @NonNull
    public static QDueUserEntity fromDomainModel(@NonNull QDueUser qDueUser) {
        return new QDueUserEntity(
                qDueUser.getId(),
                qDueUser.getNickname(),
                qDueUser.getEmail()
        );
    }

    /**
     * Update this entity with data from domain model.
     * Useful for update operations while preserving entity instance.
     *
     * @param qDueUser Domain model with updated data
     */
    public void updateFromDomainModel(@NonNull QDueUser qDueUser) {
        // Note: ID is not updated to preserve database integrity
        this.mNickname = qDueUser.getNickname();
        this.mEmail = qDueUser.getEmail();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if entity represents a valid user (has ID).
     *
     * @return true if entity has ID (persisted)
     */
    public boolean isPersisted() {
        return mId != null && mId > 0;
    }

    /**
     * Check if user has nickname.
     *
     * @return true if nickname is not empty
     */
    public boolean hasNickname() {
        return !mNickname.trim().isEmpty();
    }

    /**
     * Check if user has email.
     *
     * @return true if email is not empty
     */
    public boolean hasEmail() {
        return !mEmail.trim().isEmpty();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        QDueUserEntity that = (QDueUserEntity) obj;
        return Objects.equals(mId, that.mId) &&
                Objects.equals(mNickname, that.mNickname) &&
                Objects.equals(mEmail, that.mEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mNickname, mEmail);
    }

    @NonNull
    @Override
    public String toString() {
        return "QDueUserEntity{" +
                "id=" + mId +
                ", nickname='" + mNickname + '\'' +
                ", email='" + mEmail + '\'' +
                ", persisted=" + isPersisted() +
                '}';
    }
}