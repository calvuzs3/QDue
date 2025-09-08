package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.qdueuser.models.QDueUser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/// QDueUserEntity - Room Database Entity for Simplified User Management
///
/// Data layer entity representing QDueUser in the database following Room persistence patterns.
/// Provides conversion methods between domain models and database entities for clean architecture separation.
/// ### Database Design:
///
///     - **Table Name**: "qdueuser" for clear identification
///     - **Primary Key**: String ID from UUID
///     - **Columns**: id, nickname, email, created_at, updated_at (all simple types)
///     - **Constraints**: Non-null columns with empty string defaults
///
/// ### Clean Architecture Bridge:
///
///     - **Entity Conversion**: toDomainModel() and fromDomainModel() methods
///     - **Data Layer Isolation**: Room annotations isolated from domain
///     - **Type Safety**: Proper null handling and validation
///     - **Database Optimization**: Efficient column types and indexing
@Entity (tableName = "user")
public class QDueUserEntity {

    // ==================== DATABASE COLUMNS ====================

    @PrimaryKey ()
    @ColumnInfo (name = "id")
    @NonNull
    private String id;

    @ColumnInfo (name = "nickname")
    @NonNull
    private String nickname;

    @ColumnInfo (name = "email")
    @NonNull
    private String email;

    @ColumnInfo (name = "created_at")
    @NonNull
    private Long createdAt;

    @ColumnInfo (name = "updated_at")
    @NonNull
    private Long updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public QDueUserEntity() {
    }

    // ==================== GETTERS AND SETTERS ====================

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

    public void setId(@Nullable String id) {

        this.id = id;
    }

    public void setNickname(@NonNull String nickname) {

        this.nickname = nickname;
    }

    public void setEmail(@NonNull String email) {

        this.email = email;
    }

    public void setCreatedAt(@NonNull Long createdAt) {

        this.createdAt = createdAt;
    }

    public void setUpdatedAt(@NonNull Long updatedAt) {

        this.updatedAt = updatedAt;
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
        return new QDueUser.Builder()
                .id( this.id )
                .nickname( this.nickname )
                .email( this.email )
                .createdAt( this.createdAt )
                .updatedAt( this.updatedAt )
                .build();
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
        QDueUserEntity entity = new QDueUserEntity();

        entity.setId( qDueUser.getId() );
        entity.setNickname( qDueUser.getNickname() );
        entity.setEmail( qDueUser.getEmail() );
        entity.setCreatedAt( qDueUser.getCreatedAt() );
        entity.setUpdatedAt( qDueUser.getUpdatedAt() );

        return entity;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if user has nickname.
     *
     * @return true if nickname is not empty
     */
    public boolean hasNickname() {
        return !nickname.trim().isEmpty();
    }

    /**
     * Check if user has email.
     *
     * @return true if email is not empty
     */
    public boolean hasEmail() {
        return !email.trim().isEmpty();
    }

    /**
     * Check if user profile is complete (has both nickname and email).
     *
     * @return true if both nickname and email are provided
     */
    public boolean isProfileComplete() {
        return hasNickname() && hasEmail();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        QDueUserEntity that = (QDueUserEntity) obj;
        return Objects.equals( id, that.id ) &&
                Objects.equals( nickname, that.nickname ) &&
                Objects.equals( email, that.email );
    }

    @Override
    public int hashCode() {
        return Objects.hash( id, nickname, email );
    }

    @NonNull
    @Override
    public String toString() {
        return "QDueUserEntity{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + LocalDateTime.ofInstant( Instant.ofEpochMilli( createdAt ), ZoneId.systemDefault() ) +
                ", updatedAt=" + LocalDateTime.ofInstant( Instant.ofEpochMilli( updatedAt ), ZoneId.systemDefault() ) +
                ", profileComplete=" + isProfileComplete() +
                '}';
    }
}