package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.Objects;
import java.util.UUID;

/**
 * TeamEntity - RoomDB entity for persisting work teams.
 *
 * <p>Data layer entity that represents work teams in the database. This entity
 * is responsible for data persistence and mapping to/from domain models.
 * Follows RoomDB patterns and conventions used throughout the QDue application.</p>
 *
 * <h3>Database Features:</h3>
 * <ul>
 *   <li><strong>Primary Key</strong>: Auto-generated ID for database identity</li>
 *   <li><strong>Unique Constraint</strong>: Team name must be unique</li>
 *   <li><strong>Indexes</strong>: Optimized queries on name and active status</li>
 *   <li><strong>Null Safety</strong>: Proper null handling for optional fields</li>
 *   <li><strong>Extensible</strong>: Ready for additional team properties</li>
 * </ul>
 *
 * <h3>Clean Architecture Role:</h3>
 * <ul>
 *   <li><strong>Data Layer</strong>: Belongs to data persistence layer</li>
 *   <li><strong>Repository Mapping</strong>: Converted to domain Team objects</li>
 *   <li><strong>Database Abstraction</strong>: Hides persistence details from domain</li>
 *   <li><strong>Framework Dependent</strong>: Uses RoomDB annotations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Database Version 6
 */
@Entity (
        tableName = "teams",
        indices = {
                @Index (value = {"name"}, unique = true, name = "ix_teams_name"),
                @Index (value = {"active"}, name = "ix_teams_active"),
                @Index (value = {"name", "active"}, name = "ix_teams_name_active"),
                @Index (value = {"qdue_offset"}, name = "idx_teams_qdue_offset") // NEW INDEX
        }
)

public class TeamEntity {

    // ==================== DATABASE COLUMNS ====================

    @NonNull
    @PrimaryKey
    @ColumnInfo (name = "id")
    private String id;

    @NonNull
    @ColumnInfo (name = "name")
    private String name;

    @Nullable
    @ColumnInfo (name = "display_name")
    private String displayName;

    @Nullable
    @ColumnInfo (name = "description")
    private String description;

    @Nullable
    @ColumnInfo (name = "color_hex")
    private String colorHex;

    @NonNull
    @ColumnInfo (name = "qdue_offset")
    private Integer qdueOffset;

    @NonNull
    @ColumnInfo (name = "team_type")
    private Team.TeamType teamType;

    @ColumnInfo (name = "active", defaultValue = "1")
    private boolean active;

    @ColumnInfo (name = "created_at")
    private long createdAt;

    @ColumnInfo (name = "updated_at")
    private long updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor with basic team information.
     */
    public TeamEntity() {
        this.id = UUID.randomUUID().toString();
        this.name = this.id;
        this.colorHex = "#";
        this.qdueOffset = 0;
        this.teamType = Team.TeamType.STANDARD;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
        touch();
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = Objects.requireNonNull( name, "Team name cannot be null" ).trim();
        touch();
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName != null && !displayName.trim().isEmpty() ?
                displayName.trim() : null;
        touch();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description != null && !description.trim().isEmpty() ?
                description.trim() : null;
        touch();
    }

    @Nullable
    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(@Nullable String colorHex) {
        this.colorHex = colorHex;
        touch();
    }

    public int getQdueOffset() {
        return qdueOffset;
    }

    public void setQdueOffset(int qdueOffset) {
        this.qdueOffset = qdueOffset;
        touch();
    }

    @NonNull
    public Team.TeamType getTeamType() {
        return teamType;
    }

    public void setTeamType(@NonNull Team.TeamType teamType) {
        this.teamType = teamType;
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        touch();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Check if team has description.
     *
     * @return true if description is not null and not empty
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Check if team has custom display name.
     *
     * @return true if display name differs from name
     */
    public boolean hasDisplayName() {
        return displayName != null && !displayName.equals( name );
    }

    /**
     * Update the updated timestamp.
     * Call this when making changes to track modification time.
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== VALIDATION ====================

    /**
     * Validate entity data before database operations.
     *
     * @return true if entity is valid
     * @throws IllegalStateException if entity is invalid
     */
    public boolean validate() {
        if (name.trim().isEmpty()) {
            throw new IllegalStateException( "Team name cannot be null or empty" );
        }

        if (name.length() > 50) { // Reasonable limit for team names
            throw new IllegalStateException( "Team name cannot exceed 50 characters" );
        }

        if (displayName != null && displayName.length() > 100) {
            throw new IllegalStateException( "Display name cannot exceed 100 characters" );
        }

        if (description != null && description.length() > 500) {
            throw new IllegalStateException( "Description cannot exceed 500 characters" );
        }

        return true;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Mark team as inactive instead of deleting.
     * Soft delete approach to preserve historical data.
     */
    public void markAsInactive() {
        this.active = false;
        touch();
    }

    /**
     * Restore team to active status.
     */
    public void markAsActive() {
        this.active = true;
        touch();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TeamEntity that = (TeamEntity) obj;

        // Check for name equality
        if (this.name.equals( that.getName() ))
            return true;

        // Check for ID equality
        return this.id.equals( that.id );
    }

    @Override
    public int hashCode() {
        // Use ID if available, otherwise use name
        return Objects.hash( id, name );
    }

    @Override
    @NonNull
    public String toString() {
        return "TeamEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", qdueOffset=" + qdueOffset +
                '}';
    }

    /**
     * Get detailed string representation for debugging.
     *
     * @return Detailed string with all entity information
     */
    @NonNull
    public String toDetailedString() {
        return "TeamEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", qdueOffset=" + qdueOffset +
                ", teamType=" + teamType.name() +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Convert to domain model.
     * This method creates a Team domain object from the entity.
     */
    @NonNull
    public static Team toDomainModel(@NonNull TeamEntity entity) {
        try {
            Team.Builder builder = Team.builder()
                    .id( entity.getId() )
                    .name( entity.getName() )
                    .displayName( entity.getDisplayName() )
                    .colorHex( entity.getColorHex() )
                    .teamType( entity.getTeamType() )
                    .description( entity.getDescription() )
                    .active( entity.isActive() )
                    .qdueOffset( entity.getQdueOffset() )
                    .createdAt( entity.getCreatedAt() )
                    .updatedAt( entity.getUpdatedAt() );

            return builder.build();
        } catch (Exception e) {
            Log.e( "TeamEntity", "Failed to convert to domain model", e );
            throw new RuntimeException( "Failed to convert to domain model" );
        }
    }

    /**
     * Convert from Domain model.
     * This method creates a TeamEntity entity from the domain object.
     */
    @NonNull
    public static TeamEntity fromDomainModel(@NonNull Team team) {
        try {
            TeamEntity entity = new TeamEntity();
            entity.setId( team.getId() );
            entity.setName( team.getName() );
            entity.setDisplayName( team.getDisplayName() );
            entity.setDescription( team.getDescription() );
            entity.setActive( team.isActive() );
            entity.setColorHex( team.getColorHex() );
            entity.setQdueOffset( team.getQdueOffset() );
            entity.setTeamType( team.getTeamType() );
            entity.setCreatedAt( team.getCreatedAt() );
            entity.setUpdatedAt( team.getUpdatedAt() );
            return entity;
        } catch (Exception e) {
            Log.e( "TeamEntity", "Failed to convert from domain model", e );
            throw new RuntimeException( "Failed to convert from domain model" );
        }
    }
}