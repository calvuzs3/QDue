package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;
import java.util.Objects;

/**
 * Represents a work team (or half-team) of workers.
 * <p>
 * Each HalfTeam is identified by a unique name (typically a single character).
 * Supports cloning and comparison operations for team management.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public class HalfTeam implements Cloneable, Comparable<HalfTeam> {

    public static final String TAG = HalfTeam.class.getSimpleName();

    // Core properties
    private final String name;

    /**
     * Creates a new half-team with the specified name.
     *
     * @param name Team identifier (typically a single character)
     */
    public HalfTeam(String name) {
        this.name = name;
    }

    /**
     * @return The team name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this team matches another team by name.
     * Utility method compatible with original version.
     *
     * @param other Other team to compare
     * @return true if teams have the same name
     */
    public boolean isSameTeamAs(HalfTeam other) {
        if (other == null) return false;
        return Objects.equals(other.getName(), this.name);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + this.name + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        HalfTeam other = (HalfTeam) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public HalfTeam clone() throws CloneNotSupportedException {
        // No additional logic needed as name is immutable
        // and we don't store other mutable state
        return (HalfTeam) super.clone();
    }

    /**
     * Compares this team with another for sorting.
     * Sorting is based on team name.
     *
     * @param other Other team to compare
     * @return negative if this team precedes other,
     *         positive if this team follows other,
     *         zero if teams are equal
     */
    @Override
    public int compareTo(HalfTeam other) {
        return this.name.compareTo(other.name);
    }

    public String getShortName() {
        return getName();
    }
}