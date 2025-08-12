package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

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
 *   <li><strong>Display Support</strong>: Methods for UI presentation</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * {@code
 * // Create teams
 * Team teamA = Team.builder("A").build();
 * Team teamAB = Team.builder("AB")
 *     .displayName("Team Alpha-Beta")
 *     .description("Primary morning shift team")
 *     .build();
 *
 * // Use in collections
 * List<Team> workingTeams = Arrays.asList(teamA, teamAB);
 *
 * // Business logic
 * boolean sameTeam = teamA.equals(teamAB);
 * String display = teamA.getDisplayName();
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
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
public class Team {

    // ==================== CORE PROPERTIES ====================

    private final String id;
    private final String name;
    private final String displayName;
    private final String description;
    private final boolean active;

    // ==================== CACHED VALUES ====================

    private final int hashCodeCache;

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a Team with basic information.
     *
     * @param id Unique team identifier
     * @param name Team name (typically used in schedules)
     */
    public Team(@NonNull String id, @NonNull String name) {
        this(id, name, name, "", true);
    }

    /**
     * Creates a Team with complete information.
     *
     * @param id Unique team identifier
     * @param name Team name (typically used in schedules)
     * @param displayName Human-readable display name
     * @param description Team description
     * @param active Whether team is currently active
     */
    public Team(@NonNull String id,
                @NonNull String name,
                @Nullable String displayName,
                @Nullable String description,
                boolean active) {
        this.id = Objects.requireNonNull(id, "Team ID cannot be null").trim();
        this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
        this.displayName = displayName != null && !displayName.trim().isEmpty() ?
                displayName.trim() : this.name;
        this.description = description != null ? description.trim() : "";
        this.active = active;

        // Validation
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("Team ID cannot be empty");
        }
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }

        // Cache hash code
        this.hashCodeCache = Objects.hash(this.id);
    }

    // ==================== GETTERS ====================

    /**
     * Get the unique team identifier.
     *
     * @return Team ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Get the team name (used in schedules and business logic).
     *
     * @return Team name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Get the display name for UI presentation.
     *
     * @return Human-readable team name
     */
    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the team description.
     *
     * @return Team description or empty string
     */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Check if team is currently active.
     *
     * @return true if team is active
     */
    public boolean isActive() {
        return active;
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
        return this.id.equals(other.id);
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
        return this.name.equalsIgnoreCase(teamName.trim());
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
        return this.id.equals(teamId.trim());
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get short identifier for compact display.
     *
     * @return Team name (short form)
     */
    @NonNull
    public String getShortName() {
        return name;
    }

    /**
     * Get full name with description if available.
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
     * Get summary information about the team.
     *
     * @return Team summary
     */
    @NonNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Team ").append(displayName);

        if (!name.equals(displayName)) {
            summary.append(" (").append(name).append(")");
        }

        if (!active) {
            summary.append(" [INACTIVE]");
        }

        return summary.toString();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing Team instances.
     *
     * @param id Team ID (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull String id) {
        return new Builder(id);
    }

    /**
     * Create a builder for constructing Team instances with name.
     *
     * @param id Team ID (required)
     * @param name Team name (required)
     * @return Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull String id, @NonNull String name) {
        return new Builder(id, name);
    }

    /**
     * Create a builder from existing Team.
     *
     * @return Builder instance with current team data
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class for creating Team instances.
     */
    public static class Builder {
        private final String id;
        private String name;
        private String displayName;
        private String description = "";
        private boolean active = true;

        private Builder(@NonNull String id) {
            this.id = Objects.requireNonNull(id, "Team ID cannot be null").trim();
            this.name = this.id; // Default name to ID
        }

        private Builder(@NonNull String id, @NonNull String name) {
            this.id = Objects.requireNonNull(id, "Team ID cannot be null").trim();
            this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
        }

        private Builder(@NonNull Team existingTeam) {
            this.id = existingTeam.id;
            this.name = existingTeam.name;
            this.displayName = existingTeam.displayName;
            this.description = existingTeam.description;
            this.active = existingTeam.active;
        }

        /**
         * Set team name.
         *
         * @param name Team name
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name, "Team name cannot be null").trim();
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
            return new Team(id, name, displayName, description, active);
        }
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a team from name (ID and name will be the same).
     *
     * @param name Team name
     * @return Team instance
     */
    @NonNull
    public static Team fromName(@NonNull String name) {
        Objects.requireNonNull(name, "Team name cannot be null");
        String cleanName = name.trim();
        return new Team(cleanName, cleanName);
    }

    /**
     * Create a team from ID and name.
     *
     * @param id Team ID
     * @param name Team name
     * @return Team instance
     */
    @NonNull
    public static Team fromIdAndName(@NonNull String id, @NonNull String name) {
        return new Team(id, name);
    }

    /**
     * Create teams from an array of names.
     * Convenient for creating multiple teams at once.
     *
     * @param names Array of team names
     * @return Array of Team instances
     */
    @NonNull
    public static Team[] fromNames(@NonNull String... names) {
        Objects.requireNonNull(names, "Team names array cannot be null");

        Team[] teams = new Team[names.length];
        for (int i = 0; i < names.length; i++) {
            teams[i] = fromName(names[i]);
        }
        return teams;
    }

    // ==================== STANDARD TEAMS FACTORY ====================

    /**
     * Utility class for creating standard QuattroDue teams.
     */
    public static class Standard {

        /**
         * Create standard QuattroDue teams (A, B, C, D, E, F, G, H, I).
         *
         * @return Array of standard teams
         */
        @NonNull
        public static Team[] createQuattroDueTeams() {
            return fromNames("A", "B", "C", "D", "E", "F", "G", "H", "I");
        }

        /**
         * Get team A.
         *
         * @return Team A
         */
        @NonNull
        public static Team teamA() {
            return fromName("A");
        }

        /**
         * Get team B.
         *
         * @return Team B
         */
        @NonNull
        public static Team teamB() {
            return fromName("B");
        }

        /**
         * Get team C.
         *
         * @return Team C
         */
        @NonNull
        public static Team teamC() {
            return fromName("C");
        }

        // Additional standard teams can be added here as needed
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Team team = (Team) obj;
        return Objects.equals(id, team.id);
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
                ", active=" + active +
                '}';
    }

    // ==================== CONVERSION UTILITIES ====================

    /**
     * Utility methods for team conversion and manipulation.
     */
    public static class Utils {

        /**
         * Convert team names to Team objects.
         *
         * @param teamNames Array of team names
         * @return Array of Team objects
         */
        @NonNull
        public static Team[] fromNameArray(@NonNull String[] teamNames) {
            return fromNames(teamNames);
        }

        /**
         * Extract team names from Team objects.
         *
         * @param teams Array of Team objects
         * @return Array of team names
         */
        @NonNull
        public static String[] toNameArray(@NonNull Team[] teams) {
            Objects.requireNonNull(teams, "Teams array cannot be null");

            String[] names = new String[teams.length];
            for (int i = 0; i < teams.length; i++) {
                names[i] = teams[i] != null ? teams[i].getName() : "";
            }
            return names;
        }

        /**
         * Find team by ID in array.
         *
         * @param teams Array of teams to search
         * @param id Team ID to find
         * @return Team with matching ID, or null if not found
         */
        @Nullable
        public static Team findById(@NonNull Team[] teams, @NonNull String id) {
            Objects.requireNonNull(teams, "Teams array cannot be null");
            Objects.requireNonNull(id, "Team ID cannot be null");

            for (Team team : teams) {
                if (team != null && team.hasId(id)) {
                    return team;
                }
            }
            return null;
        }

        /**
         * Find team by name in array.
         *
         * @param teams Array of teams to search
         * @param name Team name to find
         * @return Team with matching name, or null if not found
         */
        @Nullable
        public static Team findByName(@NonNull Team[] teams, @NonNull String name) {
            Objects.requireNonNull(teams, "Teams array cannot be null");
            Objects.requireNonNull(name, "Team name cannot be null");

            for (Team team : teams) {
                if (team != null && team.hasName(name)) {
                    return team;
                }
            }
            return null;
        }
    }
}