package net.calvuz.qdue.domain.calendar.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RecurrenceRuleExtensions - Extensions for Custom Pattern Support
 *
 * <p>Provides extensions to the existing RecurrenceRule domain model to support
 * user-defined custom work patterns that don't fit standard RRULE frequencies.
 * This includes pattern day sequences, shift assignments, and conversion utilities.</p>
 *
 * <h3>Custom Pattern Storage:</h3>
 * <ul>
 *   <li><strong>Pattern Data</strong>: JSON storage of PatternDay sequences</li>
 *   <li><strong>Metadata</strong>: Pattern statistics and validation info</li>
 *   <li><strong>Backward Compatibility</strong>: Works with existing RecurrenceRule</li>
 *   <li><strong>Type Safety</strong>: Strongly typed conversion methods</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Create custom pattern from PatternDays
 * List&lt;PatternDay&gt; patternDays = createPatternDays();
 * RecurrenceRule customRule = RecurrenceRuleExtensions.createCustomPattern(
 *     "My Work Pattern", patternDays, LocalDate.now());
 *
 * // Extract pattern back to PatternDays
 * List&lt;PatternDay&gt; extracted = RecurrenceRuleExtensions.extractPatternDays(customRule);
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Custom Pattern Support
 * @since Clean Architecture Phase 2
 */
public class RecurrenceRuleExtensions {

    private static final String TAG = "RecurrenceRuleExtensions";

    // ==================== CONSTANTS ====================

    /**
     * New frequency type for custom patterns.
     * Added as extension to existing RecurrenceRule.Frequency enum.
     */
    public static final String CUSTOM_PATTERN_FREQUENCY = "CUSTOM_PATTERN";

    // JSON keys for custom pattern data storage
    private static final String JSON_KEY_PATTERN_DAYS = "pattern_days";
    private static final String JSON_KEY_PATTERN_METADATA = "metadata";
    private static final String JSON_KEY_VERSION = "version";
    private static final String JSON_KEY_WORK_DAYS_COUNT = "work_days_count";
    private static final String JSON_KEY_REST_DAYS_COUNT = "rest_days_count";
    private static final String JSON_KEY_SHIFT_TYPES = "shift_types";

    private static final String CURRENT_VERSION = "1.0";

    private static final Gson GSON = new Gson();

    // ==================== PATTERN CREATION ====================

    /**
     * Create a RecurrenceRule for custom user pattern.
     *
     * @param patternName User-friendly name for the pattern
     * @param patternDays List of pattern days defining the sequence
     * @param startDate Pattern start date
     * @return RecurrenceRule configured for custom pattern
     */
    @NonNull
    public static RecurrenceRule createCustomPattern(@NonNull String patternName,
                                                     @NonNull List<PatternDay> patternDays,
                                                     @NonNull LocalDate startDate) {
        return createCustomPattern(patternName, null, patternDays, startDate);
    }

    /**
     * Create a RecurrenceRule for custom user pattern with description.
     *
     * @param patternName User-friendly name for the pattern
     * @param description Optional pattern description
     * @param patternDays List of pattern days defining the sequence
     * @param startDate Pattern start date
     * @return RecurrenceRule configured for custom pattern
     */
    @NonNull
    public static RecurrenceRule createCustomPattern(@NonNull String patternName,
                                                     @Nullable String description,
                                                     @NonNull List<PatternDay> patternDays,
                                                     @NonNull LocalDate startDate) {
        try {
            if (patternDays.isEmpty()) {
                throw new IllegalArgumentException("Pattern days cannot be empty");
            }

            // Generate pattern data JSON
            String patternDataJson = encodePatternDaysToJson(patternDays);

            // Calculate pattern statistics
            PatternStatistics stats = calculatePatternStatistics(patternDays);

            // We'll use QUATTRODUE_CYCLE as the base frequency and store custom data
            // This allows backward compatibility with existing RecurrenceRule structure
            RecurrenceRule.Builder builder = RecurrenceRule.builder()
                    .id("custom_pattern_" + UUID.randomUUID().toString())
                    .name(patternName)
                    .description(description != null ? description : generatePatternDescription(stats))
                    .frequency(RecurrenceRule.Frequency.QUATTRODUE_CYCLE) // Base frequency
                    .interval(1)
                    .startDate(startDate)
                    .endType(RecurrenceRule.EndType.NEVER)
                    .cycleLength(patternDays.size())
                    .workDays(stats.workDays)
                    .restDays(stats.restDays)
                    .active(true);

            RecurrenceRule rule = builder.build();

            // Store custom pattern data using metadata approach
            // This would require extending RecurrenceRule to have a metadata/notes field
            // For now, we'll store it in the description field with a special format
            String enhancedDescription = createEnhancedDescription(description, patternDataJson, stats);

            // Rebuild with enhanced description containing pattern data
            return builder.description(enhancedDescription).build();

        } catch (Exception e) {
            Log.e(TAG, "Error creating custom pattern", e);
            throw new RuntimeException("Failed to create custom pattern: " + e.getMessage(), e);
        }
    }

    // ==================== PATTERN EXTRACTION ====================

    /**
     * Extract PatternDay list from custom RecurrenceRule.
     *
     * WARNING: Returned PatternDays contain minimal Shift placeholders (ID/name only).
     * Service layer MUST populate full Shift objects via ShiftRepository.getShiftById().
     *
     * @param recurrenceRule RecurrenceRule containing custom pattern data
     * @return List of PatternDays with minimal shift data (NOT complete Shift objects)
     */
    @NonNull
    public static List<PatternDay> extractPatternDays(@NonNull RecurrenceRule recurrenceRule) {
        try {
            if (!isCustomPattern(recurrenceRule)) {
                Log.w(TAG, "RecurrenceRule is not a custom pattern: " + recurrenceRule.getId());
                return new ArrayList<>();
            }

            String description = recurrenceRule.getDescription();
            if (description == null) {
                Log.w(TAG, "No description found in custom pattern rule");
                return new ArrayList<>();
            }

            // Extract JSON data from enhanced description
            String patternDataJson = extractPatternDataFromDescription(description);
            if (patternDataJson == null) {
                Log.w(TAG, "No pattern data found in description");
                return new ArrayList<>();
            }

            // Decode JSON to PatternDay list (with minimal shift placeholders)
            return decodePatternDaysFromJson(patternDataJson);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting pattern days", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if RecurrenceRule represents a custom pattern.
     *
     * @param recurrenceRule RecurrenceRule to check
     * @return true if custom pattern, false otherwise
     */
    public static boolean isCustomPattern(@NonNull RecurrenceRule recurrenceRule) {
        // Check if this is a custom pattern by looking for pattern data in description
        String description = recurrenceRule.getDescription();
        return description != null &&
                description.contains("CUSTOM_PATTERN_DATA:") &&
                recurrenceRule.getFrequency() == RecurrenceRule.Frequency.QUATTRODUE_CYCLE;
    }

    // ==================== JSON ENCODING/DECODING ====================

    /**
     * Encode PatternDay list to JSON string.
     */
    @NonNull
    private static String encodePatternDaysToJson(@NonNull List<PatternDay> patternDays) {
        try {
            List<PatternDayData> patternDayDataList = new ArrayList<>();

            for (PatternDay patternDay : patternDays) {
                PatternDayData data = new PatternDayData();
                data.dayNumber = patternDay.getDayNumber();
                data.isRestDay = patternDay.isRestDay();

                if (patternDay.isWorkDay() && patternDay.getShift() != null) {
                    Shift shift = patternDay.getShift();
                    data.shiftId = shift.getId();
                    data.shiftName = shift.getName();
                    data.shiftStartTime = shift.getStartTime() != null ? shift.getStartTime().toString() : null;
                    data.shiftEndTime = shift.getEndTime() != null ? shift.getEndTime().toString() : null;
                }

                patternDayDataList.add(data);
            }

            Map<String, Object> patternData = new HashMap<>();
            patternData.put(JSON_KEY_VERSION, CURRENT_VERSION);
            patternData.put(JSON_KEY_PATTERN_DAYS, patternDayDataList);

            return GSON.toJson(patternData);

        } catch (Exception e) {
            Log.e(TAG, "Error encoding pattern days to JSON", e);
            throw new RuntimeException("JSON encoding failed", e);
        }
    }

    /**
     * Decode PatternDay list from JSON string.
     * Note: Returns PatternDays with minimal shift data (ID/name only).
     * Service layer must populate full Shift objects via ShiftRepository lookup.
     */
    @NonNull
    private static List<PatternDay> decodePatternDaysFromJson(@NonNull String jsonData) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> patternData = GSON.fromJson(jsonData, mapType);

            if (!patternData.containsKey(JSON_KEY_PATTERN_DAYS)) {
                Log.w(TAG, "No pattern days found in JSON data");
                return new ArrayList<>();
            }

            Type listType = new TypeToken<List<PatternDayData>>(){}.getType();
            String patternDaysJson = GSON.toJson(patternData.get(JSON_KEY_PATTERN_DAYS));
            List<PatternDayData> patternDayDataList = GSON.fromJson(patternDaysJson, listType);

            List<PatternDay> patternDays = new ArrayList<>();
            for (PatternDayData data : patternDayDataList) {
                PatternDay patternDay;

                if (data.isRestDay) {
                    patternDay = new PatternDay(data.dayNumber, null);
                } else {
                    // Create minimal shift placeholder - Service layer must populate via ShiftRepository
                    Shift minimalShift = createMinimalShiftPlaceholder(data);
                    patternDay = new PatternDay(data.dayNumber, minimalShift);
                }

                patternDays.add(patternDay);
            }

            return patternDays;

        } catch (Exception e) {
            Log.e(TAG, "Error decoding pattern days from JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * Create minimal shift placeholder with only ID and name.
     * WARNING: This is NOT a complete Shift object from database.
     * Service layer must replace with actual Shift via ShiftRepository.getShiftById().
     */
    @NonNull
    private static Shift createMinimalShiftPlaceholder(@NonNull PatternDayData data) {
        return Shift.builder(data.shiftId != null ? data.shiftId : "unknown_shift")
//                .id(data.shiftId != null ? data.shiftId : "unknown_shift")
//                .name(data.shiftName != null ? data.shiftName : "Unknown Shift")
                // Note: startTime/endTime NOT set - must be loaded from DB
                .build();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create enhanced description containing pattern data.
     */
    @NonNull
    private static String createEnhancedDescription(@Nullable String userDescription,
                                                    @NonNull String patternDataJson,
                                                    @NonNull PatternStatistics stats) {
        StringBuilder description = new StringBuilder();

        if (userDescription != null && !userDescription.trim().isEmpty()) {
            description.append(userDescription).append("\n\n");
        }

        description.append("Pattern personalizzato: ")
                .append(stats.totalDays).append(" giorni, ")
                .append(stats.workDays).append(" lavoro, ")
                .append(stats.restDays).append(" riposo");

        // Append pattern data with special marker
        description.append("\n\nCUSTOM_PATTERN_DATA:")
                .append(patternDataJson)
                .append(":END_CUSTOM_PATTERN_DATA");

        return description.toString();
    }

    /**
     * Extract pattern data from enhanced description.
     */
    @Nullable
    private static String extractPatternDataFromDescription(@NonNull String description) {
        int startMarker = description.indexOf("CUSTOM_PATTERN_DATA:");
        int endMarker = description.indexOf(":END_CUSTOM_PATTERN_DATA");

        if (startMarker != -1 && endMarker != -1 && endMarker > startMarker) {
            return description.substring(startMarker + "CUSTOM_PATTERN_DATA:".length(), endMarker);
        }

        return null;
    }

    /**
     * Calculate statistics for pattern days.
     */
    @NonNull
    private static PatternStatistics calculatePatternStatistics(@NonNull List<PatternDay> patternDays) {
        int totalDays = patternDays.size();
        int workDays = 0;
        int restDays = 0;
        List<String> shiftTypes = new ArrayList<>();

        for (PatternDay day : patternDays) {
            if (day.isWorkDay()) {
                workDays++;
                if (day.getShift() != null && day.getShift().getName() != null) {
                    String shiftName = day.getShift().getName();
                    if (!shiftTypes.contains(shiftName)) {
                        shiftTypes.add(shiftName);
                    }
                }
            } else {
                restDays++;
            }
        }

        return new PatternStatistics(totalDays, workDays, restDays, shiftTypes);
    }

    /**
     * Generate pattern description from statistics.
     */
    @NonNull
    private static String generatePatternDescription(@NonNull PatternStatistics stats) {
        StringBuilder desc = new StringBuilder();
        desc.append("Schema di ").append(stats.totalDays).append(" giorni");

        if (stats.workDays > 0) {
            desc.append(" con ").append(stats.workDays).append(" giorni di lavoro");
        }

        if (stats.restDays > 0) {
            desc.append(" e ").append(stats.restDays).append(" giorni di riposo");
        }

        if (!stats.shiftTypes.isEmpty()) {
            desc.append(" (turni: ").append(String.join(", ", stats.shiftTypes)).append(")");
        }

        return desc.toString();
    }

    // ==================== DATA CLASSES ====================

    /**
     * Data transfer object for JSON serialization of PatternDay.
     */
    private static class PatternDayData {
        public int dayNumber;
        public boolean isRestDay;
        public String shiftId;
        public String shiftName;
        public String shiftStartTime;
        public String shiftEndTime;
    }

    /**
     * Pattern statistics container.
     */
    private static class PatternStatistics {
        public final int totalDays;
        public final int workDays;
        public final int restDays;
        public final List<String> shiftTypes;

        public PatternStatistics(int totalDays, int workDays, int restDays, List<String> shiftTypes) {
            this.totalDays = totalDays;
            this.workDays = workDays;
            this.restDays = restDays;
            this.shiftTypes = shiftTypes;
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Validate custom pattern for consistency.
     *
     * @param patternDays Pattern days to validate
     * @return Validation result with details
     */
    @NonNull
    public static ValidationResult validateCustomPattern(@NonNull List<PatternDay> patternDays) {
        try {
            if (patternDays.isEmpty()) {
                return new ValidationResult(false, "Pattern cannot be empty");
            }

            if (patternDays.size() > 365) {
                return new ValidationResult(false, "Pattern cannot exceed 365 days");
            }

            // Check day numbering
            for (int i = 0; i < patternDays.size(); i++) {
                PatternDay day = patternDays.get(i);
                if (day.getDayNumber() != i + 1) {
                    return new ValidationResult(false, "Day numbering must be sequential starting from 1");
                }
            }

            // Check for valid shifts
            for (PatternDay day : patternDays) {
                if (day.isWorkDay()) {
                    Shift shift = day.getShift();
                    if (shift == null || shift.getId() == null || shift.getId().trim().isEmpty()) {
                        return new ValidationResult(false, "Work day " + day.getDayNumber() + " has invalid shift");
                    }
                }
            }

            return new ValidationResult(true, "Pattern is valid");

        } catch (Exception e) {
            Log.e(TAG, "Error validating custom pattern", e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
}