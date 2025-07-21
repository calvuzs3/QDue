package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.utils.Log;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a work shift on a specific day.
 * <p>
 * A shift is characterized by a shift type and can include multiple teams.
 * Supports plant stops and cloning for template-based generation.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public class Shift implements Cloneable {

    private static final String TAG = Shift.class.getSimpleName();

    // Logging configuration
    private final static boolean LOG_ENABLED = Costants.QD_LOG_ENABLED;

    // Shift properties
    private final ShiftType shiftType;
    private boolean stop; // true if shift is during plant stop
    private final Set<HalfTeam> halfTeams;
    private LocalDate date; // Optional shift date

    /**
     * Creates a new shift with the specified type.
     *
     * @param shiftType Type of shift
     */
    public Shift(ShiftType shiftType) {
        this.shiftType = shiftType;
        this.stop = false;
        this.halfTeams = new HashSet<>();
    }

    /**
     * Creates a new shift with type and date.
     *
     * @param shiftType Type of shift
     * @param date Date of the shift
     */
    public Shift(ShiftType shiftType, LocalDate date) {
        this.shiftType = shiftType;
        this.stop = false;
        this.halfTeams = new HashSet<>();
        this.date = date;
    }

    /**
     * @return true if shift is during plant stop
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * Sets the plant stop status of the shift.
     *
     * @param stop true if shift is during plant stop
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * Adds a team to the shift.
     *
     * @param halfTeam Team to add
     */
    public void addTeam(HalfTeam halfTeam) {
        if (halfTeam == null) {
            if (LOG_ENABLED) Log.w(TAG, "Attempt to add null team");
            return;
        }

        halfTeams.add(halfTeam);
    }

    /**
     * @return Immutable set of teams assigned to the shift
     */
    public Set<HalfTeam> getHalfTeams() {
        return Collections.unmodifiableSet(halfTeams);
    }

    /**
     * Checks if a specific team is assigned to this shift.
     *
     * @param halfTeam Team to check
     * @return true if team is assigned to the shift
     */
    public boolean containsHalfTeam(HalfTeam halfTeam) {
        if (halfTeam == null) return false;

        return halfTeams.stream()
                .anyMatch(ht -> ht.isSameTeamAs(halfTeam));
    }

    /**
     * @return Concatenated string with all team names in the shift
     */
    public String getTeamsAsString() {
        return halfTeams.stream()
                .map(HalfTeam::getName)
                .collect(Collectors.joining());
    }

    /**
     * @return The shift type
     */
    public ShiftType getShiftType() {
        return shiftType;
    }

    /**
     * @return The shift date, or null if not specified
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the shift date.
     *
     * @param date Shift date
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * @return The shift name
     */
    public String getName() {
        return shiftType != null ? shiftType.getName() : "Unknown";
    }

    /**
     * @return Formatted start time of the shift
     */
    public String getStartTime() {
        return shiftType != null ? shiftType.getFormattedStartTime() : "";
    }

    /**
     * @return Formatted end time of the shift
     */
    public String getEndTime() {
        return shiftType != null ? shiftType.getFormattedEndTime() : "";
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(TAG);
        sb.append("{");

        if (shiftType != null) {
            sb.append(shiftType.getName());
        } else {
            sb.append("Unknown");
        }

        if (isStop()) {
            sb.append("-stop");
        }

        if (date != null) {
            sb.append(" ").append(date);
        }

        if (!halfTeams.isEmpty()) {
            sb.append(" HalfTeams: ").append(getTeamsAsString());
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Shift other = (Shift) obj;
        return Objects.equals(shiftType, other.shiftType) &&
                stop == other.stop &&
                Objects.equals(date, other.date) &&
                halfTeams.size() == other.halfTeams.size() &&
                halfTeams.containsAll(other.halfTeams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shiftType, stop, date, halfTeams.size());
    }

    @NonNull
    @Override
    public Shift clone() throws CloneNotSupportedException {
        Shift clone = (Shift) super.clone();

        // Copy stop status
        clone.setStop(this.stop);

        // Copy date
        if (this.date != null) {
            clone.setDate(LocalDate.of(this.date.getYear(),
                    this.date.getMonth(),
                    this.date.getDayOfMonth()));
        }

        // Clone teams
        for (HalfTeam team : this.halfTeams) {
            try {
                clone.addTeam(team.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Error cloning team: " + e.getMessage());
            }
        }

        return clone;
    }

    /**
     * Builder for creating Shift instances fluently.
     */
    public static class Builder {
        private ShiftType shiftType;
        private boolean stop = false;
        private LocalDate date;
        private final Set<HalfTeam> halfTeams = new HashSet<>();

        public Builder withShiftType(ShiftType shiftType) {
            this.shiftType = shiftType;
            return this;
        }

        public Builder withStop(boolean stop) {
            this.stop = stop;
            return this;
        }

        public Builder withDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder addTeam(HalfTeam halfTeam) {
            if (halfTeam != null) {
                this.halfTeams.add(halfTeam);
            }
            return this;
        }

        public Builder addTeams(Set<HalfTeam> teams) {
            if (teams != null) {
                this.halfTeams.addAll(teams);
            }
            return this;
        }

        public Shift build() {
            Shift shift = new Shift(shiftType, date);
            shift.setStop(stop);
            halfTeams.forEach(shift::addTeam);
            return shift;
        }
    }
}