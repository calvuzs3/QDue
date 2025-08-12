package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

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
@Entity(
        tableName = "teams",
        indices = {
                @Index(value = "name", unique = true),  // Unique team names
                @Index(value = "active"),               // Query by active status
                @Index(value = {"name", "active"})     // Composite index for common queries
        }
)
public class TeamEntity {

    // ==================== DATABASE COLUMNS ====================

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @Nullable
    @ColumnInfo(name = "display_name")
    private String displayName;

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "active", defaultValue = "1")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // Future extensions - ready for additional properties
    @Nullable
    @ColumnInfo(name = "color_hex")
    private String colorHex;

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    private int sortOrder;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor required by Room.
     */
    public TeamEntity() {
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * Constructor with basic team information.
     *
     * @param name Team name (required)
     */
    public TeamEntity(@NonNull String name) {
        this();
        this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
        this.displayName = this.name; // Default display name to name
    }

    /**
     * Constructor with complete team information.
     *
     * @param name Team name (required)
     * @param displayName Display name for UI
     * @param description Team description
     * @param active Whether team is active
     */
    public TeamEntity(@NonNull String name,
                      @Nullable String displayName,
                      @Nullable String description,
                      boolean active) {
        this();
        this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
        this.displayName = displayName != null && !displayName.trim().isEmpty() ?
                displayName.trim() : this.name;
        this.description = description != null ? description.trim() : null;
        this.active = active;
    }

    // ==================== GETTERS AND SETTERS ====================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
        this.updatedAt = System.currentTimeMillis();
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName != null && !displayName.trim().isEmpty() ?
                displayName.trim() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description != null && !description.trim().isEmpty() ?
                description.trim() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(@Nullable String colorHex) {
        this.colorHex = colorHex;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Get effective display name.
     * Returns display name if set, otherwise returns name.
     *
     * @return Display name or name
     */
    @NonNull
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.isEmpty() ? displayName : name;
    }

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
    public boolean hasCustomDisplayName() {
        return displayName != null && !displayName.equals(name);
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
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Team name cannot be null or empty");
        }

        if (name.length() > 50) { // Reasonable limit for team names
            throw new IllegalStateException("Team name cannot exceed 50 characters");
        }

        if (displayName != null && displayName.length() > 100) {
            throw new IllegalStateException("Display name cannot exceed 100 characters");
        }

        if (description != null && description.length() > 500) {
            throw new IllegalStateException("Description cannot exceed 500 characters");
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
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Restore team to active status.
     */
    public void markAsActive() {
        this.active = true;
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TeamEntity that = (TeamEntity) obj;

        // For entities with auto-generated IDs, use ID for equality when available
        if (this.id != 0 && that.id != 0) {
            return this.id == that.id;
        }

        // Otherwise use business key (name)
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        // Use ID if available, otherwise use name
        return id != 0 ? Long.hashCode(id) : Objects.hash(name);
    }

    @Override
    @NonNull
    public String toString() {
        return "TeamEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
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
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", colorHex='" + colorHex + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create TeamEntity from name only.
     *
     * @param name Team name
     * @return New TeamEntity instance
     */
    @NonNull
    public static TeamEntity fromName(@NonNull String name) {
        return new TeamEntity(name);
    }

    /**
     * Create standard QuattroDue teams.
     * Utility method for initial database setup.
     *
     * @return Array of standard team entities
     */
    @NonNull
    public static TeamEntity[] createStandardQuattroDueTeams() {
        String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        TeamEntity[] teams = new TeamEntity[teamNames.length];

        for (int i = 0; i < teamNames.length; i++) {
            teams[i] = new TeamEntity(
                    teamNames[i],
                    "Team " + teamNames[i],
                    "Standard QuattroDue team " + teamNames[i],
                    true
            );
            teams[i].setSortOrder(i); // Preserve alphabetical order
        }

        return teams;
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Utility methods for working with TeamEntity instances.
     */
    public static class Utils {

        /**
         * Check if entity represents the same team as another (by name).
         *
         * @param entity1 First team entity
         * @param entity2 Second team entity
         * @return true if teams have same name
         */
        public static boolean isSameTeam(@Nullable TeamEntity entity1, @Nullable TeamEntity entity2) {
            if (entity1 == null || entity2 == null) {
                return false;
            }
            return Objects.equals(entity1.getName(), entity2.getName());
        }

        /**
         * Find team entity by name in array.
         *
         * @param entities Array of team entities
         * @param name Team name to find
         * @return TeamEntity with matching name, or null if not found
         */
        @Nullable
        public static TeamEntity findByName(@NonNull TeamEntity[] entities, @NonNull String name) {
            Objects.requireNonNull(entities, "Entities array cannot be null");
            Objects.requireNonNull(name, "Team name cannot be null");

            for (TeamEntity entity : entities) {
                if (entity != null && name.equalsIgnoreCase(entity.getName())) {
                    return entity;
                }
            }
            return null;
        }
    }
}