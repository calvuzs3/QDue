package net.calvuz.qdue.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TeamMinimalEntity - Minimal team data for UI selections and dropdowns.
 *
 * <p>This class contains only the essential team information needed
 * for UI components like dropdowns, spinners, and selection lists.
 * Used to optimize database queries when full TeamEntity data is not needed.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // In DAO queries
 * @Query("SELECT id, name, display_name FROM teams WHERE active = 1")
 * List<TeamMinimalEntity> getActiveTeamsMinimal();
 *
 * // In UI adapters
 * TeamMinimalEntity team = getActiveTeamsMinimal().get(0);
 * String displayText = team.getDisplayText();
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
public record TeamMinimalEntity(long id, @NonNull String name, @Nullable String displayName) {

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for minimal team data.
     *
     * @param id          Team database ID
     * @param name        Team name (e.g., "A", "B", "C")
     * @param displayName Team display name (e.g., "Team A", "Team Alpha")
     */
    public TeamMinimalEntity {
    }

    // ==================== GETTERS ====================

    // ==================== UTILITY METHODS ====================

    /**
     * Get effective display name.
     * Returns display name if available, otherwise returns name.
     *
     * @return Display name or name if display name is null/empty
     */
    @NonNull
    public String getEffectiveDisplayName() {
        return (displayName != null && !displayName.trim().isEmpty()) ? displayName : name;
    }

    /**
     * Get display text for UI components.
     * Optimized for dropdowns and selection lists.
     *
     * @return Formatted display text
     */
    @NonNull
    public String getDisplayText() {
        return getEffectiveDisplayName();
    }

    /**
     * Get short display text (typically just the team name).
     * Useful for compact UI elements.
     *
     * @return Short team identifier
     */
    @NonNull
    public String getShortDisplayText() {
        return name;
    }

    /**
     * Check if this team matches a given name (case-insensitive).
     *
     * @param teamName Name to check
     * @return true if team name matches
     */
    public boolean hasName(@Nullable String teamName) {
        if (teamName == null) {
            return false;
        }
        return this.name.equalsIgnoreCase( teamName.trim() );
    }

    /**
     * Check if this team has a specific ID.
     *
     * @param teamId ID to check
     * @return true if team ID matches
     */
    public boolean hasId(long teamId) {
        return this.id == teamId;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TeamMinimalEntity that = (TeamMinimalEntity) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode( id );
    }

    @Override
    @NonNull
    public String toString() {
        return "TeamMinimalEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Convert to summary string for logging/debugging.
     *
     * @return Summary string
     */
    @NonNull
    public String toSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append( "Team " ).append( getEffectiveDisplayName() );

        if (!name.equals( getEffectiveDisplayName() )) {
            summary.append( " (" ).append( name ).append( ")" );
        }

        return summary.toString();
    }

    /**
     * Check if this team represents the same team as another TeamMinimalEntity.
     *
     * @param other Other team to compare
     * @return true if teams have same ID
     */
    public boolean isSameTeam(@Nullable TeamMinimalEntity other) {
        if (other == null) {
            return false;
        }
        return this.id == other.id;
    }

    // ==================== UTILITY FACTORY METHODS ====================

    /**
     * Create TeamMinimalEntity from basic data.
     *
     * @param id   Team ID
     * @param name Team name
     * @return TeamMinimalEntity instance
     */
    @NonNull
    public static TeamMinimalEntity create(long id, @NonNull String name) {
        return new TeamMinimalEntity( id, name, null );
    }

    /**
     * Create TeamMinimalEntity with display name.
     *
     * @param id          Team ID
     * @param name        Team name
     * @param displayName Team display name
     * @return TeamMinimalEntity instance
     */
    @NonNull
    public static TeamMinimalEntity createWithDisplayName(long id, @NonNull String name, @Nullable String displayName) {
        return new TeamMinimalEntity( id, name, displayName );
    }

    /**
     * Create standard QuattroDue team.
     *
     * @param id         Team ID
     * @param teamLetter Team letter (A, B, C, etc.)
     * @return TeamMinimalEntity instance for standard team
     */
    @NonNull
    public static TeamMinimalEntity createStandardTeam(long id, @NonNull String teamLetter) {
        return new TeamMinimalEntity( id, teamLetter, "Team " + teamLetter );
    }
}