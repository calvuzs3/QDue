package net.calvuz.qdue.smartshifts.data.database;

import android.content.Context;
import android.util.Log;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Database initializer for populating predefined data
 * Handles initial setup of shift types and patterns
 */
public class DatabaseInitializer {

    private static final String TAG = "DatabaseInitializer";

    /**
     * Populate database with initial predefined data
     */
    public static void populateInitialData(SmartShiftsDatabase database) {
        try {
            Log.d(TAG, "Starting database initialization...");

            // Check if data already exists
            int shiftTypesCount = database.shiftTypeDao().getActiveShiftTypesCount();
            int patternsCount = database.shiftPatternDao().getActivePatternsCount();

            if (shiftTypesCount == 0) {
                Log.d(TAG, "Inserting predefined shift types...");
                insertPredefinedShiftTypes(database);
            }

            if (patternsCount == 0) {
                Log.d(TAG, "Inserting predefined shift patterns...");
                insertPredefinedPatterns(database);
            }

            Log.d(TAG, "Database initialization completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error during database initialization", e);
        }
    }

    /**
     * Insert predefined shift types
     */
    private static void insertPredefinedShiftTypes(SmartShiftsDatabase database) {
        List<ShiftType> predefinedTypes = new ArrayList<>();

        // Morning shift
        predefinedTypes.add(new ShiftType(
                "morning",
                "Mattina", // Will be replaced by localized string in production
                "06:00",
                "14:00",
                "#4CAF50", // Green
                "ic_morning",
                true,
                1,
                false
        ));

        // Afternoon shift
        predefinedTypes.add(new ShiftType(
                "afternoon",
                "Pomeriggio",
                "14:00",
                "22:00",
                "#FF9800", // Orange
                "ic_afternoon",
                true,
                2,
                false
        ));

        // Night shift
        predefinedTypes.add(new ShiftType(
                "night",
                "Notte",
                "22:00",
                "06:00",
                "#3F51B5", // Indigo
                "ic_night",
                true,
                3,
                false
        ));

        // Rest period
        predefinedTypes.add(new ShiftType(
                "rest",
                "Riposo",
                "00:00",
                "23:59",
                "#9E9E9E", // Grey
                "ic_rest",
                false,
                4,
                false
        ));

        database.shiftTypeDao().insertAll(predefinedTypes);
        Log.d(TAG, "Inserted " + predefinedTypes.size() + " predefined shift types");
    }

    /**
     * Insert predefined shift patterns
     */
    private static void insertPredefinedPatterns(SmartShiftsDatabase database) {
        List<ShiftPattern> predefinedPatterns = new ArrayList<>();

        // Continuous Cycle 4-2 (18 days)
        predefinedPatterns.add(new ShiftPattern(
                "continuous_4_2",
                "Ciclo Continuo 4-2", // Will be replaced by localized string in production
                "4 mattine, 2 riposi, 4 notti, 2 riposi, 4 pomeriggi, 2 riposi",
                18,
                generateContinuous42Json(),
                true,
                true,
                null
        ));

        // Continuous Cycle 3-2 (15 days)
        predefinedPatterns.add(new ShiftPattern(
                "continuous_3_2",
                "Ciclo Continuo 3-2",
                "3 mattine, 2 riposi, 3 notti, 2 riposi, 3 pomeriggi, 2 riposi",
                15,
                generateContinuous32Json(),
                true,
                true,
                null
        ));

        // Weekly Standard 5-2 (7 days)
        predefinedPatterns.add(new ShiftPattern(
                "weekly_5_2",
                "Settimana Standard 5-2",
                "5 giorni lavorativi, 2 giorni riposo (lunedì-venerdì)",
                7,
                generateWeekly52Json(),
                false,
                true,
                null
        ));

        // Weekly Standard 6-1 (7 days)
        predefinedPatterns.add(new ShiftPattern(
                "weekly_6_1",
                "Settimana Standard 6-1",
                "6 giorni lavorativi, 1 giorno riposo",
                7,
                generateWeekly61Json(),
                false,
                true,
                null
        ));

        database.shiftPatternDao().insertAll(predefinedPatterns);
        Log.d(TAG, "Inserted " + predefinedPatterns.size() + " predefined shift patterns");
    }

    /**
     * Generate JSON for Continuous 4-2 pattern
     */
    private static String generateContinuous42Json() {
        return "{\n" +
                "  \"pattern_type\": \"custom_cycle\",\n" +
                "  \"cycle_length\": 18,\n" +
                "  \"continuous_cycle_compliant\": true,\n" +
                "  \"shifts_sequence\": [\n" +
                "    {\"days\": [1,2,3,4], \"shift_type\": \"morning\"},\n" +
                "    {\"days\": [5,6], \"shift_type\": \"rest\"},\n" +
                "    {\"days\": [7,8,9,10], \"shift_type\": \"night\"},\n" +
                "    {\"days\": [11,12], \"shift_type\": \"rest\"},\n" +
                "    {\"days\": [13,14,15,16], \"shift_type\": \"afternoon\"},\n" +
                "    {\"days\": [17,18], \"shift_type\": \"rest\"}\n" +
                "  ],\n" +
                "  \"team_generation\": {\n" +
                "    \"max_teams\": 9,\n" +
                "    \"offset_pattern\": \"sequential\",\n" +
                "    \"coverage_24h\": true\n" +
                "  }\n" +
                "}";
    }

    /**
     * Generate JSON for Continuous 3-2 pattern
     */
    private static String generateContinuous32Json() {
        return "{\n" +
                "  \"pattern_type\": \"custom_cycle\",\n" +
                "  \"cycle_length\": 15,\n" +
                "  \"continuous_cycle_compliant\": true,\n" +
                "  \"shifts_sequence\": [\n" +
                "    {\"days\": [1,2,3], \"shift_type\": \"morning\"},\n" +
                "    {\"days\": [4,5], \"shift_type\": \"rest\"},\n" +
                "    {\"days\": [6,7,8], \"shift_type\": \"night\"},\n" +
                "    {\"days\": [9,10], \"shift_type\": \"rest\"},\n" +
                "    {\"days\": [11,12,13], \"shift_type\": \"afternoon\"},\n" +
                "    {\"days\": [14,15], \"shift_type\": \"rest\"}\n" +
                "  ],\n" +
                "  \"team_generation\": {\n" +
                "    \"max_teams\": 5,\n" +
                "    \"offset_pattern\": \"sequential\",\n" +
                "    \"coverage_24h\": true\n" +
                "  }\n" +
                "}";
    }

    /**
     * Generate JSON for Weekly 5-2 pattern
     */
    private static String generateWeekly52Json() {
        return "{\n" +
                "  \"pattern_type\": \"weekly\",\n" +
                "  \"cycle_length\": 7,\n" +
                "  \"continuous_cycle_compliant\": false,\n" +
                "  \"shifts_sequence\": [\n" +
                "    {\"days\": [1,2,3,4,5], \"shift_type\": \"morning\"},\n" +
                "    {\"days\": [6,7], \"shift_type\": \"rest\"}\n" +
                "  ],\n" +
                "  \"team_generation\": {\n" +
                "    \"max_teams\": 1,\n" +
                "    \"offset_pattern\": \"none\",\n" +
                "    \"coverage_24h\": false\n" +
                "  }\n" +
                "}";
    }

    /**
     * Generate JSON for Weekly 6-1 pattern
     */
    private static String generateWeekly61Json() {
        return "{\n" +
                "  \"pattern_type\": \"weekly\",\n" +
                "  \"cycle_length\": 7,\n" +
                "  \"continuous_cycle_compliant\": false,\n" +
                "  \"shifts_sequence\": [\n" +
                "    {\"days\": [1,2,3,4,5,6], \"shift_type\": \"morning\"},\n" +
                "    {\"days\": [7], \"shift_type\": \"rest\"}\n" +
                "  ],\n" +
                "  \"team_generation\": {\n" +
                "    \"max_teams\": 1,\n" +
                "    \"offset_pattern\": \"none\",\n" +
                "    \"coverage_24h\": false\n" +
                "  }\n" +
                "}";
    }

    /**
     * Verify data integrity after initialization
     */
    public static void verifyDataIntegrity(SmartShiftsDatabase database) {
        try {
            // Check minimum required data exists
            int shiftTypesCount = database.shiftTypeDao().getActiveShiftTypesCount();
            int patternsCount = database.shiftPatternDao().getActivePatternsCount();

            if (shiftTypesCount < 4) {
                Log.w(TAG, "Warning: Expected at least 4 shift types, found: " + shiftTypesCount);
                // Attempt to re-initialize if data is missing
                insertPredefinedShiftTypes(database);
            }

            if (patternsCount < 4) {
                Log.w(TAG, "Warning: Expected at least 4 patterns, found: " + patternsCount);
                // Attempt to re-initialize if data is missing
                insertPredefinedPatterns(database);
            }

            Log.d(TAG, "Data integrity verification completed. ShiftTypes: " +
                    shiftTypesCount + ", Patterns: " + patternsCount);

        } catch (Exception e) {
            Log.e(TAG, "Error during data integrity verification", e);
        }
    }

    /**
     * Create shift type with localized name
     * This method should be used when Context is available for string resolution
     */
    public static ShiftType createLocalizedShiftType(Context context, String id, int nameResId,
                                                     String startTime, String endTime, String colorHex,
                                                     String iconName, boolean isWorkingShift, int sortOrder) {
        return new ShiftType(
                id,
                context.getString(nameResId), // Resolve localized string
                startTime,
                endTime,
                colorHex,
                iconName,
                isWorkingShift,
                sortOrder,
                false // not custom
        );
    }

    /**
     * Create shift pattern with localized name and description
     */
    public static ShiftPattern createLocalizedShiftPattern(Context context, String id,
                                                           int nameResId, int descriptionResId,
                                                           int cycleLengthDays, String recurrenceRuleJson,
                                                           boolean isContinuousCycle) {
        return new ShiftPattern(
                id,
                context.getString(nameResId),        // Resolve localized name
                context.getString(descriptionResId), // Resolve localized description
                cycleLengthDays,
                recurrenceRuleJson,
                isContinuousCycle,
                true, // predefined
                null  // system pattern
        );
    }

    /**
     * Initialize database with localized strings
     * Should be called when Context is available
     */
    public static void initializeWithLocalizedStrings(SmartShiftsDatabase database, Context context) {
        try {
            // Check if localized data needs to be updated
            List<ShiftType> currentTypes = database.shiftTypeDao().getPredefinedShiftTypes();

            if (currentTypes.size() > 0) {
                // Update existing predefined types with localized names
                updateShiftTypesWithLocalizedNames(database, context, currentTypes);
            }

            List<ShiftPattern> currentPatterns = database.shiftPatternDao().getPredefinedPatternsSync();

            if (currentPatterns.size() > 0) {
                // Update existing predefined patterns with localized names/descriptions
                updatePatternsWithLocalizedNames(database, context, currentPatterns);
            }

            Log.d(TAG, "Database updated with localized strings");

        } catch (Exception e) {
            Log.e(TAG, "Error updating database with localized strings", e);
        }
    }

    /**
     * Update shift types with localized names
     */
    private static void updateShiftTypesWithLocalizedNames(SmartShiftsDatabase database,
                                                           Context context, List<ShiftType> shiftTypes) {
        long updateTime = System.currentTimeMillis();

        for (ShiftType shiftType : shiftTypes) {
            String localizedName = getLocalizedShiftTypeName(context, shiftType.id);
            if (localizedName != null && !localizedName.equals(shiftType.name)) {
                database.shiftTypeDao().updateShiftDetails(
                        shiftType.id,
                        localizedName,
                        shiftType.startTime,
                        shiftType.endTime,
                        updateTime
                );
            }
        }
    }

    /**
     * Update patterns with localized names and descriptions
     */
    private static void updatePatternsWithLocalizedNames(SmartShiftsDatabase database,
                                                         Context context, List<ShiftPattern> patterns) {
        long updateTime = System.currentTimeMillis();

        for (ShiftPattern pattern : patterns) {
            String localizedName = getLocalizedPatternName(context, pattern.id);
            String localizedDesc = getLocalizedPatternDescription(context, pattern.id);

            if (localizedName != null || localizedDesc != null) {
                database.shiftPatternDao().updatePatternDetails(
                        pattern.id,
                        localizedName != null ? localizedName : pattern.name,
                        localizedDesc != null ? localizedDesc : pattern.description,
                        pattern.recurrenceRuleJson,
                        pattern.isContinuousCycle,
                        updateTime
                );
            }
        }
    }

    /**
     * Get localized shift type name by ID
     */
    private static String getLocalizedShiftTypeName(Context context, String shiftTypeId) {
        switch (shiftTypeId) {
            case "morning":
                return context.getString(R.string.shift_type_morning);
            case "afternoon":
                return context.getString(R.string.shift_type_afternoon);
            case "night":
                return context.getString(R.string.shift_type_night);
            case "rest":
                return context.getString(R.string.shift_type_rest);
            default:
                return null;
        }
    }

    /**
     * Get localized pattern name by ID
     */
    private static String getLocalizedPatternName(Context context, String patternId) {
        switch (patternId) {
            case "continuous_4_2":
                return context.getString(R.string.pattern_continuous_4_2_name);
            case "continuous_3_2":
                return context.getString(R.string.pattern_continuous_3_2_name);
            case "weekly_5_2":
                return context.getString(R.string.pattern_weekly_5_2_name);
            case "weekly_6_1":
                return context.getString(R.string.pattern_weekly_6_1_name);
            default:
                return null;
        }
    }

    /**
     * Get localized pattern description by ID
     */
    private static String getLocalizedPatternDescription(Context context, String patternId) {
        switch (patternId) {
            case "continuous_4_2":
                return context.getString(R.string.pattern_continuous_4_2_desc);
            case "continuous_3_2":
                return context.getString(R.string.pattern_continuous_3_2_desc);
            case "weekly_5_2":
                return context.getString(R.string.pattern_weekly_5_2_desc);
            case "weekly_6_1":
                return context.getString(R.string.pattern_weekly_6_1_desc);
            default:
                return null;
        }
    }
}