package net.calvuz.qdue.core.domain.quattrodue.models;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ✅ DOMAIN MODEL: Work Schedule Pattern
 *
 * <p>Represents the shift assignment pattern for a single day within a work
 * schedule cycle. Each pattern contains one or more shift assignments that
 * define which teams work which shifts on that specific day.</p>
 *
 * <p>Pattern characteristics:</p>
 * <ul>
 *   <li>Represents one day in a rotation cycle</li>
 *   <li>Contains multiple shift assignments</li>
 *   <li>Supports different shift types (work, rest, overtime)</li>
 *   <li>Manages team assignments for each shift</li>
 *   <li>Serializable for database storage</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public class WorkSchedulePattern {

    // ==================== CORE PROPERTIES ====================

    private Long id;                                  // Database primary key
    private int dayInCycle;                           // Day position in cycle (0-based)
    private String dayName;                           // Optional day name/label
    private List<WorkShiftAssignment> shiftAssignments; // Shift assignments for this day

    // ==================== METADATA ====================

    private boolean isRestDay;                        // Flag for complete rest days
    private boolean hasOvertimeShifts;                // Flag for overtime shifts
    private String notes;                             // Optional notes for this day
    private int assignedTeamsCount;                   // Cached count of unique teams

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for framework use.
     */
    public WorkSchedulePattern() {
        this.shiftAssignments = new ArrayList<>();
        this.isRestDay = false;
        this.hasOvertimeShifts = false;
    }

    /**
     * Constructor for creating a pattern with day information.
     *
     * @param dayInCycle Day position in cycle (0-based)
     * @param dayName Optional day name
     */
    public WorkSchedulePattern(int dayInCycle, String dayName) {
        this();
        this.dayInCycle = dayInCycle;
        this.dayName = dayName;
    }

    /**
     * Constructor for creating a complete pattern.
     *
     * @param dayInCycle Day position in cycle
     * @param dayName Optional day name
     * @param shiftAssignments List of shift assignments
     */
    public WorkSchedulePattern(int dayInCycle, String dayName, List<WorkShiftAssignment> shiftAssignments) {
        this(dayInCycle, dayName);
        setShiftAssignments(shiftAssignments);
    }

    // ==================== SHIFT ASSIGNMENT MANAGEMENT ====================

    /**
     * Adds a shift assignment to this pattern.
     *
     * @param assignment Shift assignment to add
     */
    public void addShiftAssignment(WorkShiftAssignment assignment) {
        if (assignment == null) {
            return;
        }

        if (shiftAssignments == null) {
            shiftAssignments = new ArrayList<>();
        }

        shiftAssignments.add(assignment);
        updateCachedValues();
    }

    /**
     * Removes a shift assignment from this pattern.
     *
     * @param assignment Shift assignment to remove
     * @return true if assignment was removed
     */
    public boolean removeShiftAssignment(WorkShiftAssignment assignment) {
        if (shiftAssignments == null || assignment == null) {
            return false;
        }

        boolean removed = shiftAssignments.remove(assignment);
        if (removed) {
            updateCachedValues();
        }

        return removed;
    }

    /**
     * Clears all shift assignments.
     */
    public void clearShiftAssignments() {
        if (shiftAssignments != null) {
            shiftAssignments.clear();
            updateCachedValues();
        }
    }

    /**
     * Gets shift assignments for a specific shift type.
     *
     * @param shiftType Shift type to filter by
     * @return List of matching assignments
     */
    public List<WorkShiftAssignment> getAssignmentsForShiftType(ShiftType shiftType) {
        if (shiftAssignments == null || shiftType == null) {
            return new ArrayList<>();
        }

        return shiftAssignments.stream()
                .filter(assignment -> assignment.getShiftType() != null &&
                        assignment.getShiftType().equals(shiftType))
                .collect(Collectors.toList());
    }

    /**
     * Gets shift assignments for a specific team.
     *
     * @param teamName Team name to filter by
     * @return List of matching assignments
     */
    public List<WorkShiftAssignment> getAssignmentsForTeam(String teamName) {
        if (shiftAssignments == null || teamName == null) {
            return new ArrayList<>();
        }

        return shiftAssignments.stream()
                .filter(assignment -> assignment.hasTeam(teamName))
                .collect(Collectors.toList());
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Validates this pattern for correctness.
     *
     * @return true if pattern is valid
     */
    public boolean isValid() {
        // Basic validation
        if (dayInCycle < 0) {
            return false;
        }

        if (shiftAssignments == null) {
            return true; // Empty patterns are valid (rest days)
        }

        // Validate each assignment
        for (WorkShiftAssignment assignment : shiftAssignments) {
            if (assignment == null || !assignment.isValid()) {
                return false;
            }
        }

        // Check for conflicting assignments (same team, overlapping shifts)
        return !hasConflictingAssignments();
    }

    /**
     * Checks if this pattern has conflicting shift assignments.
     *
     * @return true if conflicts exist
     */
    private boolean hasConflictingAssignments() {
        if (shiftAssignments == null || shiftAssignments.size() <= 1) {
            return false;
        }

        for (int i = 0; i < shiftAssignments.size(); i++) {
            for (int j = i + 1; j < shiftAssignments.size(); j++) {
                WorkShiftAssignment assignment1 = shiftAssignments.get(i);
                WorkShiftAssignment assignment2 = shiftAssignments.get(j);

                if (assignment1.conflictsWith(assignment2)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets the count of work shifts (excluding rest periods).
     *
     * @return Number of work shifts
     */
    public int getWorkShiftCount() {
        if (shiftAssignments == null) {
            return 0;
        }

        return (int) shiftAssignments.stream()
                .filter(assignment -> assignment.getShiftType() != null &&
                        !assignment.getShiftType().isRestPeriod())
                .count();
    }

    /**
     * Gets the count of rest periods.
     *
     * @return Number of rest periods
     */
    public int getRestPeriodCount() {
        if (shiftAssignments == null) {
            return 0;
        }

        return (int) shiftAssignments.stream()
                .filter(assignment -> assignment.getShiftType() != null &&
                        assignment.getShiftType().isRestPeriod())
                .count();
    }

    /**
     * Gets all unique teams assigned to shifts in this pattern.
     *
     * @return List of unique team names
     */
    public List<String> getUniqueTeams() {
        if (shiftAssignments == null) {
            return new ArrayList<>();
        }

        return shiftAssignments.stream()
                .flatMap(assignment -> assignment.getTeams().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Checks if this pattern assigns any work to a specific team.
     *
     * @param teamName Team name to check
     * @return true if team has work assignments
     */
    public boolean hasWorkForTeam(String teamName) {
        if (shiftAssignments == null || teamName == null) {
            return false;
        }

        return shiftAssignments.stream()
                .anyMatch(assignment -> assignment.hasTeam(teamName) &&
                        assignment.getShiftType() != null &&
                        !assignment.getShiftType().isRestPeriod());
    }

    /**
     * Creates a copy of this pattern.
     *
     * @return New pattern instance with copied data
     */
    public WorkSchedulePattern createCopy() {
        WorkSchedulePattern copy = new WorkSchedulePattern();
        copy.dayInCycle = this.dayInCycle;
        copy.dayName = this.dayName;
        copy.isRestDay = this.isRestDay;
        copy.hasOvertimeShifts = this.hasOvertimeShifts;
        copy.notes = this.notes;

        // Deep copy shift assignments
        if (this.shiftAssignments != null) {
            copy.shiftAssignments = new ArrayList<>();
            for (WorkShiftAssignment assignment : this.shiftAssignments) {
                if (assignment != null) {
                    copy.shiftAssignments.add(assignment.createCopy());
                }
            }
        }

        copy.updateCachedValues();
        return copy;
    }

    /**
     * Updates cached values after modifications.
     */
    private void updateCachedValues() {
        this.assignedTeamsCount = getUniqueTeams().size();

        // Update rest day flag
        this.isRestDay = (getWorkShiftCount() == 0);

        // Update overtime flag
        this.hasOvertimeShifts = shiftAssignments != null &&
                shiftAssignments.stream()
                        .anyMatch(assignment -> assignment.isOvertime());
    }

    /**
     * Creates a summary string for this pattern.
     *
     * @return Pattern summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        if (dayName != null) {
            summary.append(dayName);
        } else {
            summary.append("Day ").append(dayInCycle + 1);
        }

        if (isRestDay) {
            summary.append(" (Rest Day)");
        } else {
            int workShifts = getWorkShiftCount();
            summary.append(" (").append(workShifts).append(" shift");
            if (workShifts != 1) summary.append("s");

            if (assignedTeamsCount > 0) {
                summary.append(", ").append(assignedTeamsCount).append(" team");
                if (assignedTeamsCount != 1) summary.append("s");
            }

            summary.append(")");
        }

        if (hasOvertimeShifts) {
            summary.append(" [OT]");
        }

        return summary.toString();
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getDayInCycle() { return dayInCycle; }
    public void setDayInCycle(int dayInCycle) { this.dayInCycle = dayInCycle; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public List<WorkShiftAssignment> getShiftAssignments() {
        return shiftAssignments != null ? new ArrayList<>(shiftAssignments) : new ArrayList<>();
    }

    public void setShiftAssignments(List<WorkShiftAssignment> shiftAssignments) {
        this.shiftAssignments = shiftAssignments != null ? new ArrayList<>(shiftAssignments) : new ArrayList<>();
        updateCachedValues();
    }

    public boolean isRestDay() { return isRestDay; }
    public void setRestDay(boolean restDay) {
        this.isRestDay = restDay;
        if (restDay) {
            clearShiftAssignments();
        }
    }

    public boolean hasOvertimeShifts() { return hasOvertimeShifts; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getAssignedTeamsCount() { return assignedTeamsCount; }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkSchedulePattern that = (WorkSchedulePattern) o;

        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }

        return dayInCycle == that.dayInCycle &&
                Objects.equals(dayName, that.dayName) &&
                Objects.equals(shiftAssignments, that.shiftAssignments);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(dayInCycle, dayName, shiftAssignments);
    }

    @NonNull
    @Override
    public String toString() {
        return "WorkSchedulePattern{" +
                "id=" + id +
                ", dayInCycle=" + dayInCycle +
                ", dayName='" + dayName + '\'' +
                ", assignmentsCount=" + (shiftAssignments != null ? shiftAssignments.size() : 0) +
                ", isRestDay=" + isRestDay +
                ", hasOvertimeShifts=" + hasOvertimeShifts +
                '}';
    }
}