package net.calvuz.qdue.quattrodue.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.calvuz.qdue.quattrodue.models.HalfTeam;

/**
 * Factory for creating and managing teams (HalfTeam).
 *
 * Implements a cache system for predefined teams (A-I) and provides
 * utilities for team creation, validation and retrieval.
 *
 * @author Updated 21/05/2025
 */
public final class HalfTeamFactory {

    private static final String TAG = "HalfTeamFactory";

    // Team cache by name
    private static final Map<String, HalfTeam> teamCache = new HashMap<>();

    // Prevent instantiation
    private HalfTeamFactory() {}

    // Static initialization of predefined teams
    static {
        // Initialize base teams A to I
        for (char c = 'A'; c <= 'I'; c++) {
            String name = String.valueOf(c);
            teamCache.put(name, new HalfTeam(name));
        }
    }

    /**
     * Gets a team by name. If team exists in cache, returns existing instance,
     * otherwise creates a new one.
     *
     * @param name Team name
     * @return Corresponding HalfTeam, or null if name is null
     */
    public static HalfTeam getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Convert name to uppercase for consistency
        String upperName = name.toUpperCase();

        // Search in cache
        HalfTeam team = teamCache.get(upperName);

        // If not found, create new instance and add to cache
        if (team == null) {
            team = new HalfTeam(upperName);
            teamCache.put(upperName, team);
        }

        return team;
    }

    /**
     * Gets a team by character.
     *
     * @param c Character identifier of the team
     * @return Corresponding HalfTeam
     */
    public static HalfTeam getByChar(char c) {
        return getByName(String.valueOf(c));
    }

    /**
     * Creates a new custom team.
     * This team is NOT added to the cache.
     *
     * @param name Team name
     * @param description Team description (currently unused)
     * @return New HalfTeam
     */
    public static HalfTeam createCustom(String name, String description) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        return new HalfTeam(name);
    }

    /**
     * Gets all predefined teams (A-I).
     *
     * @return List of predefined teams
     */
    public static List<HalfTeam> getAllTeams() {
        return new ArrayList<>(teamCache.values());
    }

    /**
     * Gets all teams with names in the specified array.
     *
     * @param names Array of team names
     * @return List of corresponding teams
     */
    public static List<HalfTeam> getTeamsByNames(String[] names) {
        List<HalfTeam> teams = new ArrayList<>();

        if (names == null || names.length == 0) {
            return teams;
        }

        for (String name : names) {
            HalfTeam team = getByName(name);
            if (team != null) {
                teams.add(team);
            }
        }

        return teams;
    }

    /**
     * Gets all teams with identifiers in the specified character array.
     *
     * @param chars Array of team identifier characters
     * @return List of corresponding teams
     */
    public static List<HalfTeam> getTeamsByChars(char[] chars) {
        List<HalfTeam> teams = new ArrayList<>();

        if (chars == null || chars.length == 0) {
            return teams;
        }

        for (char c : chars) {
            HalfTeam team = getByChar(c);
            if (team != null) {
                teams.add(team);
            }
        }

        return teams;
    }

    /**
     * Validates if a team is valid (has non-null and well-formed name).
     *
     * @param team Team to validate
     * @return true if team is valid
     */
    public static boolean isValidTeam(HalfTeam team) {
        return team != null && team.getName() != null && !team.getName().isEmpty();
    }
}