package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Team - Domain model representing a work team in the schedule system.
 *
 * <p>This is a clean architecture domain model representing a work team entity.
 * Teams are fundamental business concepts that can be assigned to shifts and
 * have users as members. The model is designed to be simple but extensible
 * for future enhancements.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state</li>
 *   <li><strong>Business Identity</strong>: Proper entity semantics with ID and name</li>
 *   <li><strong>Extensible</strong>: Designed for future property additions</li>
 *   <li><strong>Value Equality</strong>: Teams equal if they have same ID</li>
 * </ul>
 *
 * <h3>Team Types:</h3>
 * <ul>
 *   <li><strong>STANDARD</strong>: Regular work teams (A, B, C, etc.)</li>
 *   <li><strong>QUATTRODUE</strong>: QuattroDue cycle teams</li>
 *   <li><strong>EMERGENCY</strong>: Emergency response teams</li>
 *   <li><strong>MANAGEMENT</strong>: Management and supervisory teams</li>
 *   <li><strong>SUPPORT</strong>: Support and auxiliary teams</li>
 * </ul>
 */
public class Team {

    // ==================== TEAM TYPE ENUM ====================

    /**
     * Enumeration of team types in the work schedule system with localization keys.
     */
    public enum TeamType {
        STANDARD( "standard", "standard_work_teams" ),
        QUATTRODUE( "quattrodue", "quattrodue_cycle_teams" ),
        EMERGENCY( "emergency", "emergency_response_teams" ),
        MANAGEMENT( "management", "management_and_supervisory_teams" ),
        SUPPORT( "support", "support_and_auxiliary_teams" );

        private final String displayNameKey;
        private final String descriptionKey;

        TeamType(String displayNameKey, String descriptionKey) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
        }

        /**
         * Get localization key for display name.
         *
         * @return Localization key for display name
         */
        @NonNull
        public String getDisplayNameKey() {
            return displayNameKey;
        }

        /**
         * Get localization key for description.
         *
         * @return Localization key for description
         */
        @NonNull
        public String getDescriptionKey() {
            return descriptionKey;
        }
    }

    // ==================== CORE PROPERTIES ====================

    private final String id;
    private final String name;
    private final String displayName;
    private final String description;
    private final String colorHex;
    private final int qdueOffset;

    private final TeamType teamType;

    // ==================== METADATA ======================

    private final boolean active;
    private final long createdAt;
    private final long updatedAt;

    // ==================== CACHED VALUES ====================

    private final int hashCodeCache;

    // ==================== CONSTRUCTORS ====================

    /**
     * Private constructor for builder pattern with localization support.
     */
    private Team(@NonNull Builder builder) {

        // Validation
        Objects.requireNonNull( builder.id, "Team ID cannot be null" );
        Objects.requireNonNull( builder.name, "Team name cannot be null" );

        String cleanId = builder.id.trim();
        String cleanName = builder.name.trim();

        if (cleanId.isEmpty()) {
            throw new IllegalArgumentException( "Team ID cannot be empty" );
        }
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException( "Team name cannot be empty" );
        }

        // Core properties
        this.id = cleanId;
        this.name = cleanName;
        this.displayName = builder.displayName != null && !builder.displayName.trim().isEmpty() ?
                builder.displayName.trim() : this.name;
        this.description = builder.description != null ? builder.description.trim() : "";
        this.colorHex = builder.colorHex != null ? builder.colorHex.trim() : null;
        this.qdueOffset = builder.qdueOffset;

        this.teamType = builder.teamType;

        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;

        // Cache hash code
        this.hashCodeCache = Objects.hash( this.id );
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getColorHex() {
        return colorHex;
    }

    public int getQdueOffset() {
        return qdueOffset;
    }

    @NonNull
    public TeamType getTeamType() {
        return teamType;
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

    public int getHashCodeCache() {
        return hashCodeCache;
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Check if this team matches another team by ID.
     *
     * @param other Other team to compare
     * @return true if teams have the same ID
     */
    public boolean isSameTeam(@Nullable Team other) {
        if (other == null) {
            return false;
        }
        return this.id.equals( other.id );
    }

    /**
     * Check if this team has the specified name.
     *
     * @param teamName Name to check (case-insensitive)
     * @return true if team name matches
     */
    public boolean hasName(@Nullable String teamName) {
        if (teamName == null) {
            return false;
        }
        return this.name.equalsIgnoreCase( teamName.trim() );
    }

    /**
     * Check if this team has the specified ID.
     *
     * @param teamId ID to check
     * @return true if team ID matches
     */
    public boolean hasId(@Nullable String teamId) {
        if (teamId == null) {
            return false;
        }
        return this.id.equals( teamId.trim() );
    }

    /**
     * Check if this is a QuattroDue team.
     *
     * @return true if team type is QUATTRODUE
     */
    public boolean isQuattroDueTeam() {
        return teamType == TeamType.QUATTRODUE;
    }

    /**
     * Check if this is a management team.
     *
     * @return true if team type is MANAGEMENT
     */
    public boolean isManagementTeam() {
        return teamType == TeamType.MANAGEMENT;
    }

    /**
     * Check if this is an emergency team.
     *
     * @return true if team type is EMERGENCY
     */
    public boolean isEmergencyTeam() {
        return teamType == TeamType.EMERGENCY;
    }

    // ==================== INFO ====================

    /**
     * Get summary information about the team.
     *
     * @return Team summary
     */
    @NonNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append( "Team " ).append( displayName );

        if (!name.equals( displayName )) {
            summary.append( " (" ).append( name ).append( ")" );
        }

        if (teamType != null) {
            summary.append( " [" ).append( teamType.name() ).append( "]" );
        }

        if (!active) {
            summary.append( " [INACTIVE]" );
        }

        return summary.toString();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing Team instances.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder for constructing Team instances.
     *
     * @param id Team ID (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull String id) {
        return new Builder( id );
    }

    /**
     * Create a builder for constructing Team instances with name.
     *
     * @param id   Team ID (required)
     * @param name Team name (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull String id, @NonNull String name) {
        return new Builder( id, name );
    }

    /**
     * Builder class for creating Team instances with localization support.
     */
    public static class Builder {
        private String id;
        private String name;
        private String displayName;
        private String description = "";
        private String colorHex = "#4CAF50";
        private int qdueOffset = 0;
        private TeamType teamType = TeamType.STANDARD;
        private boolean active = true;
        private long createdAt = System.currentTimeMillis();
        private long updatedAt = System.currentTimeMillis();

        private Builder() {
            this.id = UUID.randomUUID().toString();
            this.name = this.id; // Default name to ID
        }

        private Builder(@NonNull String id) {
            this.id = Objects.requireNonNull( id, "Team ID cannot be null" ).trim();
            this.name = this.id; // Default name to ID
        }

        private Builder(@NonNull String id, @NonNull String name) {
            this.id = Objects.requireNonNull( id, "Team ID cannot be null" ).trim();
            this.name = Objects.requireNonNull( name, "Team name cannot be null" ).trim();
        }

        private Builder(@NonNull Team existingTeam) {
            this.id = existingTeam.id;
            this.name = existingTeam.name;
            this.displayName = existingTeam.displayName;
            this.description = existingTeam.description;
            this.colorHex = existingTeam.colorHex;
            this.qdueOffset = existingTeam.qdueOffset;
            this.teamType = existingTeam.teamType;
            this.active = existingTeam.active;
            this.createdAt = existingTeam.createdAt;
            this.updatedAt = existingTeam.updatedAt;
        }

        /**
         * Copy data from existing Team (for withLocalizer implementation).
         */
        @NonNull
        public Builder copyFrom(@NonNull Team source) {
            this.name = source.name;
            this.displayName = source.displayName;
            this.description = source.description;
            this.colorHex = source.colorHex;
            this.qdueOffset = source.qdueOffset;
            this.teamType = source.teamType;
            this.active = source.active;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;

            return this;
        }

        // ================== GETTERS ====================

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getColorHex() {
            return colorHex;
        }

        public int getQdueOffset() {
            return qdueOffset;
        }

        public TeamType getTeamType() {
            return teamType;
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

        // ================== SETTERS ====================

        /**
         * Set id
         *
         * @param id Team ID
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder id(@NonNull String id) {
            this.id = Objects.requireNonNull( id, "Team ID cannot be null" ).trim();
            return this;
        }

        /**
         * Set team name.
         *
         * @param name Team name
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull( name, "Team name cannot be null" ).trim();
            if (this.displayName == null)
                this.displayName = this.name;
            return this;
        }

        /**
         * Set display name.
         *
         * @param displayName Display name for UI
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder displayName(@Nullable String displayName) {
            if (displayName != null)
                this.displayName = displayName;
            return this;
        }

        /**
         * Set team description.
         *
         * @param description Team description
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description != null ? description : "";
            return this;
        }

        /**
         * Set team color hex code.
         *
         * @param colorHex Team color hex code
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder colorHex(@Nullable String colorHex) {
            // HARDCODED DEFAULT COLOR GREEN if null
            if (colorHex != null) {
                this.colorHex = colorHex;
            }
            return this;
        }

        /**
         * Set QuattroDue pattern offset in days.
         * Determines this team's position in the 18-day QuattroDue cycle.
         * Range: 0-17 (0 = base pattern, others are phase shifts)
         *
         * @param qdueOffset QuattroDue pattern offset in days
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder qdueOffset(int qdueOffset) {
            this.qdueOffset = qdueOffset;
            return this;
        }

        /**
         * Set team type classification.
         *
         * @param teamType Type classification
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder teamType(@Nullable TeamType teamType) {
            this.teamType = teamType;
            return this;
        }

        /**
         * Set team active status.
         *
         * @param active Whether team is active
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Set team creation timestamp.
         *
         * @param createdAt Team creation timestamp
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Set team update timestamp.
         *
         * @param updatedAt Team update timestamp
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Mark team as inactive.
         *
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder inactive() {
            this.active = false;
            return this;
        }

        /**
         * Build the Team instance.
         *
         * @return New Team instance
         * @throws IllegalArgumentException if required fields are not valid
         */
        @NonNull
        public Team build() {
            return new Team( this );
        }
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a standard QuattroDue team with localization support.
     *
     * @param teamLetter Team letter (A, B, C, etc.)
     * @return QuattroDue team
     */
    @NonNull
    public static Team createQuattroDueTeam(@NonNull String teamLetter, @Nullable String colorHex, int qdueOffset) {
        return builder( teamLetter )
                .displayName( teamLetter )
                .teamType( TeamType.QUATTRODUE )
                .description( "QuattroDue Team" )
                .colorHex( colorHex )
                .qdueOffset( qdueOffset )
                .build();
    }

    /**
     * Create a management team with localization support.
     *
     * @param teamName  Team name
     * @return Management team
     */
    @NonNull
    public static Team createManagementTeam(@NonNull String teamName, @NonNull String colorHex) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .description("Management Team")
                .colorHex( colorHex )
                .teamType( TeamType.MANAGEMENT )
                .build();
    }

    /**
     * Create an emergency team with localization support.
     *
     * @param teamName  Team name
     * @return Emergency team
     */
    @NonNull
    public static Team createEmergencyTeam(@NonNull String teamName, @NonNull String colorHex) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .description("Emergency Team")
                .colorHex( colorHex )
                .teamType( TeamType.EMERGENCY )
                .build();
    }

    /**
     * Create a support team with localization support.
     *
     * @param teamName  Team name
     * @return Support team
     */
    @NonNull
    public static Team createSupportTeam(@NonNull String teamName, @NonNull String colorHex) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .description("Support Team")
                .colorHex( colorHex )
                .teamType( TeamType.SUPPORT )
                .build();
    }

    // ==================== FACTORY METHODS (Legacy Support) ====================

    /**
     * Create a team from name (ID and name will be the same).
     *
     * @param name Team name
     * @return Team instance
     */
    @NonNull
    public static Team fromName(@NonNull String name) {
        Objects.requireNonNull( name, "Team name cannot be null" );
        String cleanName = name.trim();
        return builder( cleanName ).name( cleanName ).build();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Team team = (Team) obj;
        return Objects.equals( id, team.id );
    }

    @Override
    public int hashCode() {
        return hashCodeCache;
    }

    @Override
    @NonNull
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", teamType=" + teamType +
                ", active=" + active +
                '}';
    }

    /**
     * Get detailed string representation for debugging.
     *
     * @return Detailed string with all team information
     */
    @NonNull
    public String toDetailedString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", teamType=" + teamType +
                ", active=" + active +
                ", hashCodeCache=" + hashCodeCache +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}