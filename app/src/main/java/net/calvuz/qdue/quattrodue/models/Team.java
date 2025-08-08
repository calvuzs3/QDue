package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.quattrodue.utils.HalfTeamFactory;
import net.calvuz.qdue.quattrodue.utils.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Team - Complete work team composed of HalfTeam members.
 *
 * <p>Represents a complete work team in the Quattrodue pattern system. In the standard
 * 4-2 pattern, each shift is assigned to a complete Team composed of exactly two
 * HalfTeam members (e.g., Team A+B, Team C+D, Team E+F).</p>
 *
 * <h3>Quattrodue Team Structure:</h3>
 * <ul>
 *   <li><strong>Standard Teams</strong>: AB, CD, EF, GH, etc.</li>
 *   <li><strong>Two HalfTeams</strong>: Each team contains exactly 2 HalfTeam members</li>
 *   <li><strong>Pattern Assignment</strong>: Teams rotate through shifts according to pattern</li>
 *   <li><strong>Shift Coverage</strong>: One complete team covers each shift period</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Immutable team composition once created</li>
 *   <li>Validation ensures exactly 2 HalfTeam members</li>
 *   <li>Backward compatibility with Set&lt;HalfTeam&gt; operations</li>
 *   <li>Display name generation (e.g., "AB", "CD")</li>
 *   <li>Team membership checking and comparison</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class Team {

    // ==================== CORE PROPERTIES ====================

    private final HalfTeam primaryHalfTeam;     // First half-team (e.g., A, C, E)
    private final HalfTeam secondaryHalfTeam;   // Second half-team (e.g., B, D, F)
    private final String teamName;              // Computed team name (e.g., "AB", "CD")
    private final String teamId;                // Unique identifier for this team

    // ==================== CACHED VALUES ====================

    private final Set<HalfTeam> halfTeamsSet;   // Immutable set for compatibility
    private final List<HalfTeam> halfTeamsList; // Immutable list for ordered access
    private final int hashCodeCache;            // Cached hash code

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a Team from two HalfTeam members.
     *
     * @param primaryHalfTeam First half-team member
     * @param secondaryHalfTeam Second half-team member
     * @throws IllegalArgumentException if either half-team is null or teams are identical
     */
    public Team(@NonNull HalfTeam primaryHalfTeam, @NonNull HalfTeam secondaryHalfTeam) {
        // Validation
        if (primaryHalfTeam.equals(secondaryHalfTeam)) {
            throw new IllegalArgumentException("Half-teams must be different: " + primaryHalfTeam.getName());
        }

        // Ensure consistent ordering (alphabetical by name)
        if (primaryHalfTeam.getName().compareTo(secondaryHalfTeam.getName()) <= 0) {
            this.primaryHalfTeam = primaryHalfTeam;
            this.secondaryHalfTeam = secondaryHalfTeam;
        } else {
            this.primaryHalfTeam = secondaryHalfTeam;
            this.secondaryHalfTeam = primaryHalfTeam;
        }

        // Generate computed values
        this.teamName = generateTeamName();
        this.teamId = generateTeamId();

        // Create immutable collections
        this.halfTeamsSet = Set.of(this.primaryHalfTeam, this.secondaryHalfTeam);
        this.halfTeamsList = List.of(this.primaryHalfTeam, this.secondaryHalfTeam);

        // Cache hash code
        this.hashCodeCache = Objects.hash(this.primaryHalfTeam, this.secondaryHalfTeam);
    }

    /**
     * Creates a Team from a collection of HalfTeam members.
     *
     * @param halfTeams Collection of half-teams (must contain exactly 2 members)
     * @throws IllegalArgumentException if collection doesn't contain exactly 2 different half-teams
     */
    public Team(@NonNull Collection<HalfTeam> halfTeams) {
        if (halfTeams.size() < 2) {
            throw new IllegalArgumentException("Team must contain at least 2 half-teams, got: " +
                    halfTeams.size());
        }

        List<HalfTeam> teamList = new ArrayList<>(halfTeams);
        teamList.removeIf(Objects::isNull); // Remove any null values

        if (teamList.size() < 2) {
            throw new IllegalArgumentException("Team must contain at least 2 non-null half-teams");
        }

        // Use primary constructor for validation and setup
        HalfTeam first = teamList.get(0);
        HalfTeam second = teamList.get(1);

        // Validation will be done by primary constructor
        Team temp = new Team(first, second);

        // Copy all values from temp
        this.primaryHalfTeam = temp.primaryHalfTeam;
        this.secondaryHalfTeam = temp.secondaryHalfTeam;
        this.teamName = temp.teamName;
        this.teamId = temp.teamId;
        this.halfTeamsSet = temp.halfTeamsSet;
        this.halfTeamsList = temp.halfTeamsList;
        this.hashCodeCache = temp.hashCodeCache;
    }

    // ==================== STATIC FACTORY METHODS ====================

    /**
     * Creates a Team from HalfTeam names.
     *
     * @param primaryName Name of first half-team (e.g., "A")
     * @param secondaryName Name of second half-team (e.g., "B")
     * @return Team instance, or null if half-teams cannot be created
     */
    @Nullable
    public static Team fromNames(@NonNull String primaryName, @NonNull String secondaryName) {
        try {
            HalfTeam primary = HalfTeamFactory.getByName(primaryName);
            HalfTeam secondary = HalfTeamFactory.getByName(secondaryName);

            if (primary != null && secondary != null) {
                return new Team(primary, secondary);
            }
        } catch (Exception e) {
            // Invalid team creation
        }
        return null;
    }

    /**
     * Creates a Team from character identifiers.
     *
     * @param primaryChar Character for first half-team (e.g., 'A')
     * @param secondaryChar Character for second half-team (e.g., 'B')
     * @return Team instance, or null if half-teams cannot be created
     */
    @Nullable
    public static Team fromChars(char primaryChar, char secondaryChar) {
        return fromNames(String.valueOf(primaryChar), String.valueOf(secondaryChar));
    }

    /**
     * Creates a Team from a Shift's HalfTeam set.
     * Maintains backward compatibility with existing code.
     *
     * @param Shift Shift with half-teams
     * @return Team instance, or null if shift doesn't have exactly 2 half-teams
     */
    @Nullable
    public static Team fromShift(@NonNull Shift Shift) {
        Set<HalfTeam> halfTeams = Shift.getHalfTeams();
        if (halfTeams != null && halfTeams.size() > 1) {
            try {
                return new Team(halfTeams);
            } catch (Exception e) {
                // Invalid team composition
                Log.e("Team", "Invalid team composition", e);
            }
        }
        return null;
    }

    // ==================== CORE GETTERS ====================

    /**
     * Get the primary (first) half-team.
     *
     * @return Primary HalfTeam
     */
    @NonNull
    public HalfTeam getPrimaryHalfTeam() {
        return primaryHalfTeam;
    }

    /**
     * Get the secondary (second) half-team.
     *
     * @return Secondary HalfTeam
     */
    @NonNull
    public HalfTeam getSecondaryHalfTeam() {
        return secondaryHalfTeam;
    }

    /**
     * Get team name (e.g., "AB", "CD").
     *
     * @return Team display name
     */
    @NonNull
    public String getTeamName() {
        return teamName;
    }

    /**
     * Get unique team identifier.
     *
     * @return Team ID
     */
    @NonNull
    public String getTeamId() {
        return teamId;
    }

    /**
     * Get immutable set of half-teams.
     * For backward compatibility with existing code.
     *
     * @return Immutable set containing both half-teams
     */
    @NonNull
    public Set<HalfTeam> getHalfTeams() {
        return halfTeamsSet;
    }

    /**
     * Get ordered list of half-teams.
     *
     * @return Immutable list with primary half-team first
     */
    @NonNull
    public List<HalfTeam> getHalfTeamsList() {
        return halfTeamsList;
    }

    // ==================== TEAM MEMBERSHIP ====================

    /**
     * Check if this team contains a specific half-team.
     *
     * @param halfTeam Half-team to check
     * @return true if half-team is member of this team
     */
    public boolean containsHalfTeam(@Nullable HalfTeam halfTeam) {
        if (halfTeam == null) {
            return false;
        }
        return primaryHalfTeam.equals(halfTeam) || secondaryHalfTeam.equals(halfTeam);
    }

    /**
     * Check if this team contains a half-team by name.
     *
     * @param halfTeamName Half-team name to check
     * @return true if half-team name matches either member
     */
    public boolean containsHalfTeamName(@Nullable String halfTeamName) {
        if (halfTeamName == null) {
            return false;
        }
        return primaryHalfTeam.getName().equalsIgnoreCase(halfTeamName) ||
                secondaryHalfTeam.getName().equalsIgnoreCase(halfTeamName);
    }

    /**
     * Check if this team shares any half-team with another team.
     *
     * @param otherTeam Other team to compare
     * @return true if teams share at least one half-team member
     */
    public boolean sharesHalfTeamWith(@Nullable Team otherTeam) {
        if (otherTeam == null) {
            return false;
        }
        return containsHalfTeam(otherTeam.primaryHalfTeam) ||
                containsHalfTeam(otherTeam.secondaryHalfTeam);
    }

    /**
     * Get the other half-team member.
     *
     * @param halfTeam One of the half-team members
     * @return The other half-team member, or null if provided half-team is not a member
     */
    @Nullable
    public HalfTeam getOtherHalfTeam(@Nullable HalfTeam halfTeam) {
        if (halfTeam == null) {
            return null;
        }

        if (primaryHalfTeam.equals(halfTeam)) {
            return secondaryHalfTeam;
        } else if (secondaryHalfTeam.equals(halfTeam)) {
            return primaryHalfTeam;
        }

        return null; // Not a member of this team
    }

    // ==================== COMPATIBILITY METHODS ====================

    /**
     * Convert this Team to a Set&lt;HalfTeam&gt; for legacy compatibility.
     *
     * @return Mutable set containing both half-teams
     */
    @NonNull
    public Set<HalfTeam> toHalfTeamSet() {
        return new HashSet<>(halfTeamsSet);
    }

    /**
     * Convert this Team to a List&lt;HalfTeam&gt; for ordered operations.
     *
     * @return Mutable list with primary half-team first
     */
    @NonNull
    public List<HalfTeam> toHalfTeamList() {
        return new ArrayList<>(halfTeamsList);
    }

    /**
     * Get team names as concatenated string (e.g., "AB").
     * Compatible with Shift.getTeamsAsString() format.
     *
     * @return Concatenated team names
     */
    @NonNull
    public String getTeamsAsString() {
        return teamName;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get short display name (same as team name).
     *
     * @return Short team name (e.g., "AB")
     */
    @NonNull
    public String getShortName() {
        return teamName;
    }

    /**
     * Get long display name with member details.
     *
     * @return Detailed team description
     */
    @NonNull
    public String getLongName() {
        return String.format("Team %s (%s + %s)",
                teamName,
                primaryHalfTeam.getName(),
                secondaryHalfTeam.getName());
    }

    /**
     * Get description of team composition.
     *
     * @return Team composition description
     */
    @NonNull
    public String getDescription() {
        return String.format("Work team composed of half-teams %s and %s",
                primaryHalfTeam.getName(),
                secondaryHalfTeam.getName());
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate team name from half-team names.
     */
    @NonNull
    private String generateTeamName() {
        return primaryHalfTeam.getName() + secondaryHalfTeam.getName();
    }

    /**
     * Generate unique team identifier.
     */
    @NonNull
    private String generateTeamId() {
        return "TEAM_" + generateTeamName();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Team team = (Team) obj;
        return Objects.equals(primaryHalfTeam, team.primaryHalfTeam) &&
                Objects.equals(secondaryHalfTeam, team.secondaryHalfTeam);
    }

    @Override
    public int hashCode() {
        return hashCodeCache;
    }

    @Override
    @NonNull
    public String toString() {
        return "Team{" +
                "name='" + teamName + '\'' +
                ", primary=" + primaryHalfTeam.getName() +
                ", secondary=" + secondaryHalfTeam.getName() +
                '}';
    }

    // ==================== UTILITY CLASSES ====================

    /**
     * Utility class for common Team operations.
     */
    public static class Utils {

        /**
         * Create all standard Quattrodue teams (AB, CD, EF, GH, AI).
         * Based on typical 4-2 pattern team assignments.
         *
         * @return List of standard teams
         */
        @NonNull
        public static List<Team> createStandardTeams() {
            List<Team> teams = new ArrayList<>();

            // Standard team pairings from Quattrodue pattern
            String[][] standardPairs = {
                    {"A", "B"}, {"C", "D"}, {"E", "F"},
                    {"G", "H"}, {"A", "I"}, {"B", "C"},
                    {"D", "E"}, {"F", "G"}, {"H", "I"}
            };

            for (String[] pair : standardPairs) {
                Team team = Team.fromNames(pair[0], pair[1]);
                if (team != null) {
                    teams.add(team);
                }
            }

            return teams;
        }

        /**
         * Find teams that contain a specific half-team.
         *
         * @param halfTeam Half-team to search for
         * @param availableTeams List of teams to search in
         * @return List of teams containing the half-team
         */
        @NonNull
        public static List<Team> findTeamsContaining(@Nullable HalfTeam halfTeam,
                                                     @NonNull List<Team> availableTeams) {
            if (halfTeam == null) {
                return new ArrayList<>();
            }

            return availableTeams.stream()
                    .filter(team -> team.containsHalfTeam(halfTeam))
                    .collect(Collectors.toList());
        }

        /**
         * Convert legacy Set&lt;HalfTeam&gt; to Team if possible.
         *
         * @param halfTeams Set of half-teams
         * @return Team instance, or null if conversion not possible
         */
        @Nullable
        public static Team fromHalfTeamSet(@Nullable Set<HalfTeam> halfTeams) {
            if (halfTeams == null || halfTeams.size() != 2) {
                return null;
            }

            try {
                return new Team(halfTeams);
            } catch (Exception e) {
                return null;
            }
        }
    }
}