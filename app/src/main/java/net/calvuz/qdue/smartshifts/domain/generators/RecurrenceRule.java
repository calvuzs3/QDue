package net.calvuz.qdue.smartshifts.domain.models;

import net.calvuz.qdue.smartshifts.domain.generators.RecurrenceRuleParser.ShiftSequence;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Domain model representing a recurrence rule for shift patterns
 * Encapsulates the business logic for shift scheduling
 */
public class RecurrenceRule {

    private final String patternType;
    private final int cycleLength;
    private final boolean isContinuousCycle;
    private final List<ShiftSequence> shiftSequences;

    public RecurrenceRule(String patternType, int cycleLength, boolean isContinuousCycle,
                          List<ShiftSequence> shiftSequences) {
        this.patternType = patternType;
        this.cycleLength = cycleLength;
        this.isContinuousCycle = isContinuousCycle;
        this.shiftSequences = shiftSequences;
    }

    /**
     * Get shift info for specific day in cycle
     */
    public ShiftInfo getShiftForDay(int cycleDay) {
        // Ensure day is within cycle bounds
        if (cycleDay < 1 || cycleDay > cycleLength) {
            return null;
        }

        // Find which sequence contains this day
        for (ShiftSequence sequence : shiftSequences) {
            if (sequence.getDays().contains(cycleDay)) {
                return new ShiftInfo(sequence.getShiftType(), cycleDay);
            }
        }

        return null; // Rest day or invalid day
    }

    /**
     * Get all working days in cycle
     */
    public List<Integer> getWorkingDays() {
        List<Integer> workingDays = new java.util.ArrayList<>();

        for (ShiftSequence sequence : shiftSequences) {
            if (!sequence.getShiftType().equals("rest")) {
                workingDays.addAll(sequence.getDays());
            }
        }

        return workingDays;
    }

    /**
     * Get all rest days in cycle
     */
    public List<Integer> getRestDays() {
        List<Integer> restDays = new java.util.ArrayList<>();

        for (ShiftSequence sequence : shiftSequences) {
            if (sequence.getShiftType().equals("rest")) {
                restDays.addAll(sequence.getDays());
            }
        }

        return restDays;
    }

    /**
     * Calculate work-rest ratio
     */
    public double getWorkRestRatio() {
        int workingDays = getWorkingDays().size();
        int restDays = getRestDays().size();

        if (restDays == 0) return Double.MAX_VALUE;
        return (double) workingDays / restDays;
    }

    // Getters
    public String getPatternType() { return patternType; }
    public int getCycleLength() { return cycleLength; }
    public boolean isContinuousCycle() { return isContinuousCycle; }
    public List<ShiftSequence> getShiftSequences() { return shiftSequences; }

    @Override
    public String toString() {
        return "RecurrenceRule{" +
                "patternType='" + patternType + '\'' +
                ", cycleLength=" + cycleLength +
                ", isContinuousCycle=" + isContinuousCycle +
                ", sequences=" + shiftSequences.size() +
                '}';
    }
}

// =====================================================================

