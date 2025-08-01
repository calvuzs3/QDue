package net.calvuz.qdue.core.domain.quattrodue.models;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ✅ DOMAIN MODEL: Work Shift Assignment
 *
 * <p>Represents the assignment of a specific shift type to one or more teams
 * within a work schedule pattern. This is the most granular unit of work
 * scheduling, defining exactly which teams work which shifts.</p>
 *
 * <p>Assignment characteristics:</p>
 * <ul>
 *   <li>Links a shift type to specific teams</li>
 *   <li>Supports multiple teams per assignment</li>
 *   <li>Handles special cases (overtime, modifications)</li>
 *   <li>Validates for conflicts and consistency</li>
 *   <li>Serializable for database storage</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class WorkShiftAssignment {

    // ==================== CORE ASSIGNMENT DATA ====================

    private Long id;                              // Database primary key
    private ShiftType shiftType;                  // Type of shift being assigned
    private List<String> teams;                   // Teams assigned to this shift

    // ==================== ASSIGNMENT METADATA ====================

    private boolean isOvertime;                   // Flag for overtime assignments
    private boolean isMandatory;                  // Flag for mandatory shifts
    private boolean isTemporary;                  // Flag for temporary assignments
    private String assignmentNotes;               // Optional notes for this assignment

    // ==================== MODIFICATION TRACKING ====================

    private boolean isModified;                   // Flag for modified assignments
    private String modificationReason;            // Reason for modification
    private Long originalAssignmentId;            // Reference to original if modified

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for framework use.
     */
    public WorkShiftAssignment() {
        this.teams = new ArrayList<>();
        this.isOvertime = false;
        this.isMandatory = false;
        this.isTemporary = false;
        this.isModified = false;
    }

    /**
     * Constructor for creating a basic shift assignment.
     *
     * @param shiftType Type of shift
     * @param teams Teams assigned to this shift
     */
    public WorkShiftAssignment(ShiftType shiftType, List<String> teams) {
        this();
        this.shiftType = shiftType;
        setTeams(teams);
    }

    /**
     * Constructor for creating a complete shift assignment.
     *
     * @param shiftType Type of shift
     * @param teams Teams assigned to this shift
     * @param isOvertime Whether this is overtime
     * @param isMandatory Whether this is mandatory
     */
    public WorkShiftAssignment(ShiftType shiftType, List<String> teams,
                               boolean isOvertime, boolean isMandatory) {
        this(shiftType, teams);
        this.isOvertime = isOvertime;
        this.isMandatory = isMandatory;
    }

    // ==================== TEAM MANAGEMENT ====================

    /**
     * Adds a team to this assignment.
     *
     * @param teamName Team name to add
     * @return true if team was added (not already present)
     */
    public boolean addTeam(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return false;
        }

        String normalizedTeam = teamName.trim();
        if (teams.contains(normalizedTeam)) {
            return false;
        }

        teams.add(normalizedTeam);
        return true;
    }

    /**
     * Removes a team from this assignment.
     *
     * @param teamName Team name to remove
     * @return true if team was removed
     */
    public boolean removeTeam(String teamName) {
        if (teamName == null) {
            return false;
        }

        return teams.remove(teamName.trim());
    }

    /**
     * Checks if this assignment includes a specific team.
     *
     * @param teamName Team name to check
     * @return true if team is assigned
     */
    public boolean hasTeam(String teamName) {
        if (teamName == null || teams == null) {
            return false;
        }

        return teams.stream()
                .anyMatch(team -> team.equalsIgnoreCase(teamName.trim()));
    }

    /**
     * Clears all team assignments.
     */
    public void clearTeams() {
        if (teams != null) {
            teams.clear();
        }
    }

    /**
     * Sets the teams for this assignment, replacing any existing ones.
     *
     * @param teams New team list
     */
    public void setTeams(List<String> teams) {
        this.teams = new ArrayList<>();
        if (teams != null) {
            for (String team : teams) {
                if (team != null && !team.trim().isEmpty()) {
                    String normalizedTeam = team.trim();
                    if (!this.teams.contains(normalizedTeam)) {
                        this.teams.add(normalizedTeam);
                    }
                }
            }
        }
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Validates this assignment for correctness.
     *
     * @return true if assignment is valid
     */
    public boolean isValid() {
        // Must have a shift type
        if (shiftType == null) {
            return false;
        }

        // Must have at least one team (unless it's a rest period)
        if (teams == null || teams.isEmpty()) {
            return shiftType.isRestPeriod();
        }

        // All team names must be non-empty
        for (String team : teams) {
            if (team == null || team.trim().isEmpty()) {
                return false;
            }
        }

        // No duplicate teams
        Set<String> uniqueTeams = new HashSet<>(teams);
        return uniqueTeams.size() == teams.size();
    }

    /**
     * Checks if this assignment conflicts with another assignment.
     * Conflicts occur when the same team is assigned to overlapping shifts.
     *
     * @param other Other assignment to check against
     * @return true if assignments conflict
     */
    public boolean conflictsWith(WorkShiftAssignment other) {
        if (other == null || this.shiftType == null || other.shiftType == null) {
            return false;
        }

        // Check if any teams overlap
        if (!hasCommonTeams(other)) {
            return false;
        }

        // Check if shift times overlap
        return shiftsOverlap(this.shiftType, other.shiftType);
    }

    /**
     * Checks if this assignment has any teams in common with another.
     *
     * @param other Other assignment to check
     * @return true if there are common teams
     */
    public boolean hasCommonTeams(WorkShiftAssignment other) {
        if (other == null || this.teams == null || other.teams == null) {
            return false;
        }

        return this.teams.stream()
                .anyMatch( other::hasTeam );
    }

    /**
     * Checks if two shift types have overlapping time periods.
     *
     * @param shift1 First shift type
     * @param shift2 Second shift type
     * @return true if shifts overlap in time
     */
    private boolean shiftsOverlap(ShiftType shift1, ShiftType shift2) {
        if (shift1.isRestPeriod() || shift2.isRestPeriod()) {
            return false;
        }

        LocalTime start1 = shift1.getStartTime();
        LocalTime end1 = shift1.getEndTime();
        LocalTime start2 = shift2.getStartTime();
        LocalTime end2 = shift2.getEndTime();

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        // Handle shifts that cross midnight
        if (shift1.crossesMidnight() || shift2.crossesMidnight()) {
            // Complex midnight crossing logic would go here
            // For now, assume they don't overlap if either crosses midnight
            return false;
        }

        // Standard overlap check
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Gets the estimated work hours for this assignment.
     *
     * @return Work hours, or 0 if not calculable
     */
    public double getWorkHours() {
        if (shiftType == null || shiftType.isRestPeriod()) {
            return 0.0;
        }

        LocalTime start = shiftType.getStartTime();
        LocalTime end = shiftType.getEndTime();

        if (start == null || end == null) {
            return 0.0;
        }

        long minutes;
        if (shiftType.crossesMidnight()) {
            // Calculate across midnight boundary
            minutes = java.time.Duration.between(start, LocalTime.MIDNIGHT).toMinutes() +
                    java.time.Duration.between(LocalTime.MIDNIGHT, end).toMinutes();
        } else {
            minutes = java.time.Duration.between(start, end).toMinutes();
        }

        return minutes / 60.0;
    }

    /**
     * Creates a copy of this assignment.
     *
     * @return New assignment instance with copied data
     */
    public WorkShiftAssignment createCopy() {
        WorkShiftAssignment copy = new WorkShiftAssignment();
        copy.shiftType = this.shiftType; // ShiftType is immutable reference
        copy.teams = new ArrayList<>(this.teams);
        copy.isOvertime = this.isOvertime;
        copy.isMandatory = this.isMandatory;
        copy.isTemporary = this.isTemporary;
        copy.assignmentNotes = this.assignmentNotes;
        copy.isModified = this.isModified;
        copy.modificationReason = this.modificationReason;
        copy.originalAssignmentId = this.originalAssignmentId;

        return copy;
    }

    /**
     * Creates a summary string for this assignment.
     *
     * @return Assignment summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        if (shiftType != null) {
            summary.append(shiftType.getName());

            if (!shiftType.isRestPeriod()) {
                summary.append(" (")
                        .append(shiftType.getStartTime())
                        .append("-")
                        .append(shiftType.getEndTime())
                        .append(")");

                if (shiftType.crossesMidnight()) {
                    summary.append("+1");
                }
            }
        }

        if (teams != null && !teams.isEmpty()) {
            summary.append(" → Teams: ").append(String.join(", ", teams));
        }

        List<String> flags = new ArrayList<>();
        if (isOvertime) flags.add("OT");
        if (isMandatory) flags.add("REQ");
        if (isTemporary) flags.add("TEMP");
        if (isModified) flags.add("MOD");

        if (!flags.isEmpty()) {
            summary.append(" [").append(String.join("|", flags)).append("]");
        }

        return summary.toString();
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftType shiftType) { this.shiftType = shiftType; }

    public List<String> getTeams() { return new ArrayList<>(teams); }

    public boolean isOvertime() { return isOvertime; }
    public void setOvertime(boolean overtime) { this.isOvertime = overtime; }

    public boolean isMandatory() { return isMandatory; }
    public void setMandatory(boolean mandatory) { this.isMandatory = mandatory; }

    public boolean isTemporary() { return isTemporary; }
    public void setTemporary(boolean temporary) { this.isTemporary = temporary; }

    public String getAssignmentNotes() { return assignmentNotes; }
    public void setAssignmentNotes(String assignmentNotes) { this.assignmentNotes = assignmentNotes; }

    public boolean isModified() { return isModified; }
    public void setModified(boolean modified) { this.isModified = modified; }

    public String getModificationReason() { return modificationReason; }
    public void setModificationReason(String modificationReason) {
        this.modificationReason = modificationReason;
        if (modificationReason != null && !modificationReason.trim().isEmpty()) {
            this.isModified = true;
        }
    }

    public Long getOriginalAssignmentId() { return originalAssignmentId; }
    public void setOriginalAssignmentId(Long originalAssignmentId) {
        this.originalAssignmentId = originalAssignmentId;
        if (originalAssignmentId != null) {
            this.isModified = true;
        }
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Gets the number of teams assigned.
     *
     * @return Team count
     */
    public int getTeamCount() {
        return teams != null ? teams.size() : 0;
    }

    /**
     * Checks if this is a work assignment (not rest period).
     *
     * @return true if this is work
     */
    public boolean isWorkAssignment() {
        return shiftType != null && !shiftType.isRestPeriod();
    }

    /**
     * Checks if this assignment has any special flags.
     *
     * @return true if has overtime, mandatory, temporary, or modified flags
     */
    public boolean hasSpecialFlags() {
        return isOvertime || isMandatory || isTemporary || isModified;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkShiftAssignment that = (WorkShiftAssignment) o;

        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }

        return Objects.equals(shiftType, that.shiftType) &&
                Objects.equals(teams, that.teams) &&
                isOvertime == that.isOvertime &&
                isMandatory == that.isMandatory;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(shiftType, teams, isOvertime, isMandatory);
    }

    @NonNull
    @Override
    public String toString() {
        return "WorkShiftAssignment{" +
                "id=" + id +
                ", shiftType=" + (shiftType != null ? shiftType.getName() : "null") +
                ", teamsCount=" + (teams != null ? teams.size() : 0) +
                ", isOvertime=" + isOvertime +
                ", isMandatory=" + isMandatory +
                ", isTemporary=" + isTemporary +
                ", isModified=" + isModified +
                '}';
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Builder for creating WorkShiftAssignment instances.
     */
    public static class Builder {
        private final WorkShiftAssignment assignment;

        public Builder() {
            this.assignment = new WorkShiftAssignment();
        }

        public Builder shiftType(ShiftType shiftType) {
            assignment.setShiftType(shiftType);
            return this;
        }

        public Builder teams(List<String> teams) {
            assignment.setTeams(teams);
            return this;
        }

        public Builder addTeam(String team) {
            assignment.addTeam(team);
            return this;
        }

        public Builder overtime(boolean isOvertime) {
            assignment.setOvertime(isOvertime);
            return this;
        }

        public Builder mandatory(boolean isMandatory) {
            assignment.setMandatory(isMandatory);
            return this;
        }

        public Builder temporary(boolean isTemporary) {
            assignment.setTemporary(isTemporary);
            return this;
        }

        public Builder notes(String notes) {
            assignment.setAssignmentNotes(notes);
            return this;
        }

        public Builder modificationReason(String reason) {
            assignment.setModificationReason(reason);
            return this;
        }

        public Builder originalId(Long originalId) {
            assignment.setOriginalAssignmentId(originalId);
            return this;
        }

        public WorkShiftAssignment build() {
            return assignment;
        }
    }
}