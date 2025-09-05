package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.builders.LocalizableBuilder;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.models.LocalizableDomainModel;

import java.util.Objects;
import java.util.UUID;

/**
 * Team - Domain model representing a work team in the schedule system.
 *
 * <p>This is a clean architecture domain model representing a work team entity.
 * Teams are fundamental business concepts that can be assigned to shifts and
 * have users as members. The model is designed to be simple but extensible
 * for future enhancements with full localization support.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutable Design</strong>: Thread-safe and predictable state</li>
 *   <li><strong>Business Identity</strong>: Proper entity semantics with ID and name</li>
 *   <li><strong>Extensible</strong>: Designed for future property additions</li>
 *   <li><strong>Value Equality</strong>: Teams equal if they have same ID</li>
 *   <li><strong>Display Support</strong>: Localized methods for UI presentation</li>
 *   <li><strong>Localization Support</strong>: Full i18n support for all display text</li>
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
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // Create localized teams
 * Team teamA = Team.builder("A")
 *     .displayName("Team Alpha")
 *     .description("Primary morning shift team")
 *     .teamType(TeamType.STANDARD)
 *     .localizer(domainLocalizer)
 *     .build();
 *
 * // Get localized display
 * String localizedStatus = teamA.getStatusDisplayName();
 * String localizedSummary = teamA.getLocalizedSummary();
 * }
 * </pre>
 *
 * <h3>Future Extensions:</h3>
 * <p>This model can be easily extended to include:</p>
 * <ul>
 *   <li>Team member lists</li>
 *   <li>Team capabilities/skills</li>
 *   <li>Team scheduling preferences</li>
 *   <li>Team performance metrics</li>
 *   <li>Team contact information</li>
 * </ul>
 */
public class Team extends LocalizableDomainModel {

    private static final String LOCALIZATION_SCOPE = "team";

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
        super( builder.mLocalizer, LOCALIZATION_SCOPE );

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

    // ==================== LOCALIZED DISPLAY METHODS ====================

    /**
     * Get localized team type display name.
     */
    @NonNull
    public String getTypeDisplayName() {
        if (teamType != null) {
            return localize( "type." + teamType.name().toLowerCase(),
                    teamType.name(), // fallback
                    teamType.name() );
        }
        return localize( "type.unspecified", "Unspecified", "Unspecified" );
    }

    /**
     * Get localized team type description.
     */
    @NonNull
    public String getTypeDescription() {
        if (teamType != null) {
            return localize( "type." + teamType.name().toLowerCase() + ".description",
                    teamType.getDescriptionKey(), // fallback
                    teamType.name() );
        }
        return localize( "type.unspecified.description", "No specific type", "No specific type" );
    }

    /**
     * Get localized status display name.
     */
    @NonNull
    public String getStatusDisplayName() {
        if (active) {
            return localize( "status.active", "Active", "Active" );
        } else {
            return localize( "status.inactive", "Inactive", "Inactive" );
        }
    }

    /**
     * Get display title for UI with localization support.
     */
    @NonNull
    public String getDisplayTitle() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return name;
    }

    /**
     * Get localized full name with description if available.
     *
     * @return Localized full team description
     */
    @NonNull
    public String getLocalizedFullName() {
        String title = getDisplayTitle();

        if (description.isEmpty()) {
            return title;
        }

        // Localize common descriptions or use as-is
        String localizedDescription = localize( "description.custom", description, description );
        String separator = localize( "format.name_description_separator", " - ", " - " );
        return title + separator + localizedDescription;
    }

    /**
     * Get localized summary information about the team.
     *
     * @return Localized team summary
     */
    @NonNull
    public String getLocalizedSummary() {
        StringBuilder summary = new StringBuilder();

        // Team label with name
        String teamLabel = localize( "label.team", "Team", "Team" );
        summary.append( teamLabel ).append( " " ).append( getDisplayTitle() );

        // Add ID if different from display name
        if (!name.equals( displayName )) {
            String idTemplate = localize( "format.id_parentheses", " ({0})", " ({0})" );
            summary.append( String.format( idTemplate.replace( "{0}", "%s" ), name ) );
        }

        // Add type if specified
        if (teamType != null) {
            String typeTemplate = localize( "format.type_brackets", " [{0}]", " [{0}]" );
            summary.append( String.format( typeTemplate.replace( "{0}", "%s" ), getTypeDisplayName() ) );
        }

        // Add status if inactive
        if (!active) {
            String inactiveLabel = localize( "status.inactive_suffix", " [INACTIVE]", " [INACTIVE]" );
            summary.append( inactiveLabel );
        }

        return summary.toString();
    }

    /**
     * Get localized team card information for detailed display.
     *
     * @return Localized team card content
     */
    @NonNull
    public String getLocalizedTeamCard() {
        StringBuilder card = new StringBuilder();

        // Title
        card.append( getDisplayTitle() ).append( "\n" );

        // ID if different
        if (!name.equals( displayName )) {
            String idLabel = localize( "label.id", "ID", "ID" );
            card.append( idLabel ).append( ": " ).append( name ).append( "\n" );
        }

        // Type
        if (teamType != null) {
            String typeLabel = localize( "label.type", "Type", "Type" );
            card.append( typeLabel ).append( ": " ).append( getTypeDisplayName() ).append( "\n" );
        }

        // Status
        String statusLabel = localize( "label.status", "Status", "Status" );
        card.append( statusLabel ).append( ": " ).append( getStatusDisplayName() ).append( "\n" );

        // Description
        if (!description.isEmpty()) {
            String descLabel = localize( "label.description", "Description", "Description" );
            card.append( descLabel ).append( ": " ).append( description );
        }

        return card.toString().trim();
    }

    // ==================== NON-LOCALIZED DISPLAY METHODS (Legacy Support) ====================

    /**
     * Get full name with description if available (non-localized version for backward compatibility).
     * Consider using getLocalizedFullName() for new code.
     *
     * @return Full team description
     */
    @NonNull
    public String getFullName() {
        if (description.isEmpty()) {
            return displayName;
        }
        return displayName + " - " + description;
    }

    /**
     * Get summary information about the team (non-localized version).
     * Consider using getLocalizedSummary() for new code.
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

    // ==================== LOCALIZABLE IMPLEMENTATION ====================

    /**
     * Create a copy of this object with localizer injected.
     * Useful for adding localization to existing instances.
     *
     * @param localizer DomainLocalizer to inject
     * @return New instance with localizer support
     */
    @Override
    @NonNull
    public Team withLocalizer(@NonNull DomainLocalizer localizer) {
        return builder( this.id )
                .copyFrom( this )
                .localizer( localizer )
                .build();
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
     * Create a builder from existing Team.
     *
     * @return Builder instance with current team data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder( this );
    }

    /**
     * Builder class for creating Team instances with localization support.
     */
    public static class Builder extends LocalizableBuilder<Team, Builder> {
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

            return copyLocalizableFrom( source );
        }

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
         * Set localizer for localization support.
         *
         * @return Builder instance for chaining
         */
        @Override
        @NonNull
        protected Builder self() {
            return this;
        }

        /**
         * Build the Team instance.
         *
         * @return New Team instance
         * @throws IllegalArgumentException if required fields are not valid
         */
        @Override
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
     * @param localizer  Optional domain localizer for i18n
     * @return QuattroDue team
     */
    @NonNull
    public static Team createQuattroDueTeam(@NonNull String teamLetter, @Nullable String colorHex, int qdueOffset, @Nullable DomainLocalizer localizer) {
        return builder( teamLetter )
                .displayName( "Team " + teamLetter )
                .teamType( TeamType.QUATTRODUE )
                .description( "QuattroDue cycle team " + teamLetter )
                .colorHex( colorHex )
                .qdueOffset( qdueOffset )
                .localizer( localizer )
                .build();
    }

    /**
     * Create a management team with localization support.
     *
     * @param teamName  Team name
     * @param localizer Optional domain localizer for i18n
     * @return Management team
     */
    @NonNull
    public static Team createManagementTeam(@NonNull String teamName, @NonNull String colorHex, @Nullable DomainLocalizer localizer) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .colorHex( colorHex )
                .teamType( TeamType.MANAGEMENT )
                .localizer( localizer )
                .build();
    }

    /**
     * Create an emergency team with localization support.
     *
     * @param teamName  Team name
     * @param localizer Optional domain localizer for i18n
     * @return Emergency team
     */
    @NonNull
    public static Team createEmergencyTeam(@NonNull String teamName, @NonNull String colorHex, @Nullable DomainLocalizer localizer) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .colorHex( colorHex )
                .teamType( TeamType.EMERGENCY )
                .localizer( localizer )
                .build();
    }

    /**
     * Create a support team with localization support.
     *
     * @param teamName  Team name
     * @param localizer Optional domain localizer for i18n
     * @return Support team
     */
    @NonNull
    public static Team createSupportTeam(@NonNull String teamName, @NonNull String colorHex, @Nullable DomainLocalizer localizer) {
        return builder( teamName )
                .name( teamName )
                .displayName( teamName )
                .colorHex( colorHex )
                .teamType( TeamType.SUPPORT )
                .localizer( localizer )
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
                ", hasLocalizationSupport=" + hasLocalizationSupport() +
                ", hashCodeCache=" + hashCodeCache +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}