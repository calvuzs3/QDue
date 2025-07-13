package net.calvuz.qdue.smartshifts.domain.generators;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.calvuz.qdue.smartshifts.domain.models.RecurrenceRule;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Parser for recurrence rule JSON to domain objects
 * Handles conversion between JSON patterns and business logic objects
 */
@Singleton
public class RecurrenceRuleParser {

    private final Gson gson;

    @Inject
    public RecurrenceRuleParser() {
        this.gson = new Gson();
    }

    /**
     * Parse JSON recurrence rule to domain object
     */
    public RecurrenceRule parseRecurrenceRule(String recurrenceRuleJson) {
        try {
            JsonObject jsonObject = gson.fromJson(recurrenceRuleJson, JsonObject.class);

            String patternType = jsonObject.get("pattern_type").getAsString();
            int cycleLength = jsonObject.get("cycle_length").getAsInt();
            boolean isContinuous = jsonObject.get("continuous_cycle_compliant").getAsBoolean();

            List<ShiftSequence> sequences = parseShiftSequences(
                    jsonObject.getAsJsonArray("shifts_sequence")
            );

            return new RecurrenceRule(patternType, cycleLength, isContinuous, sequences);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid recurrence rule JSON: " + e.getMessage());
        }
    }

    /**
     * Parse shift sequences from JSON array
     */
    private List<ShiftSequence> parseShiftSequences(JsonArray sequencesArray) {
        List<ShiftSequence> sequences = new ArrayList<>();

        for (JsonElement element : sequencesArray) {
            JsonObject sequenceObj = element.getAsJsonObject();

            List<Integer> days = new ArrayList<>();
            JsonArray daysArray = sequenceObj.getAsJsonArray("days");
            for (JsonElement dayElement : daysArray) {
                days.add(dayElement.getAsInt());
            }

            String shiftType = sequenceObj.get("shift_type").getAsString();

            sequences.add(new ShiftSequence(days, shiftType));
        }

        return sequences;
    }

    /**
     * Generate JSON from recurrence rule object
     */
    public String generateRecurrenceRuleJson(RecurrenceRule rule) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("pattern_type", rule.getPatternType());
        jsonObject.addProperty("cycle_length", rule.getCycleLength());
        jsonObject.addProperty("continuous_cycle_compliant", rule.isContinuousCycle());

        JsonArray sequencesArray = new JsonArray();
        for (ShiftSequence sequence : rule.getShiftSequences()) {
            JsonObject sequenceObj = new JsonObject();

            JsonArray daysArray = new JsonArray();
            for (Integer day : sequence.getDays()) {
                daysArray.add(day);
            }

            sequenceObj.add("days", daysArray);
            sequenceObj.addProperty("shift_type", sequence.getShiftType());
            sequencesArray.add(sequenceObj);
        }

        jsonObject.add("shifts_sequence", sequencesArray);

        // Add team generation info for continuous cycles
        if (rule.isContinuousCycle()) {
            JsonObject teamGeneration = new JsonObject();
            teamGeneration.addProperty("max_teams", calculateMaxTeamsForCycle(rule.getCycleLength()));
            teamGeneration.addProperty("offset_pattern", "sequential");
            teamGeneration.addProperty("coverage_24h", true);
            jsonObject.add("team_generation", teamGeneration);
        }

        return gson.toJson(jsonObject);
    }

    /**
     * Calculate max teams for cycle length
     */
    private int calculateMaxTeamsForCycle(int cycleLength) {
        if (cycleLength == 18) return 9;
        if (cycleLength == 15) return 5;
        if (cycleLength == 7) return 1;
        return Math.max(1, cycleLength / 2);
    }

    // ===== INNER CLASSES =====

    /**
     * Represents a sequence of days with same shift type
     */
    public static class ShiftSequence {
        private final List<Integer> days;
        private final String shiftType;

        public ShiftSequence(List<Integer> days, String shiftType) {
            this.days = days;
            this.shiftType = shiftType;
        }

        public List<Integer> getDays() { return days; }
        public String getShiftType() { return shiftType; }
    }
}
