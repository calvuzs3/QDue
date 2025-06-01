package net.calvuz.qdue.quattrodue.utils;

import net.calvuz.qdue.quattrodue.models.ShiftType;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Factory for creating and managing shift types.
 *
 * Supports variable number of shifts per day and provides
 * persistence through SharedPreferences. Includes predefined
 * shift types and utilities for custom shift creation.
 *
 * @author Updated 21/05/2025
 */
public class ShiftTypeFactory {

    private static final String TAG = ShiftTypeFactory.class.getSimpleName();

    // Preferences keys
    private static final String PREFS_NAME = "shift_types_prefs";
    private static final String KEY_SHIFT_COUNT = "shift_count";
    private static final String KEY_SHIFT_NAME_PREFIX = "shift_name_";
    private static final String KEY_SHIFT_DESC_PREFIX = "shift_desc_";
    private static final String KEY_SHIFT_START_HOUR_PREFIX = "shift_start_hour_";
    private static final String KEY_SHIFT_START_MIN_PREFIX = "shift_start_min_";
    private static final String KEY_SHIFT_DURATION_HOURS_PREFIX = "shift_dur_hours_";
    private static final String KEY_SHIFT_DURATION_MINS_PREFIX = "shift_dur_mins_";
    private static final String KEY_SHIFT_COLOR_PREFIX = "shift_color_";

    // Default values
    private static final int DEFAULT_SHIFT_COUNT = 3;

    // Cache for shift types
    private static Map<String, ShiftType> shiftTypeCache = new HashMap<>();

    // Predefined colors for shifts
    private static final int COLOR_MORNING = Color.parseColor("#B3E5FC");  // Light Blue
    private static final int COLOR_AFTERNOON = Color.parseColor("#FFE0B2"); // Light Orange
    private static final int COLOR_NIGHT = Color.parseColor("#E1BEE7");    // Light Purple
    private static final int COLOR_CUSTOM1 = Color.parseColor("#C8E6C9");  // Light Green
    private static final int COLOR_CUSTOM2 = Color.parseColor("#FFCDD2");  // Light Red

    // Standard shift type definitions
    public static final ShiftType MORNING = new ShiftType(
            "Morning",
            "Morning shift (5-13)",
            5, 0,
            8, 0,
            COLOR_MORNING);

    public static final ShiftType AFTERNOON = new ShiftType(
            "Afternoon",
            "Afternoon shift (13-21)",
            13, 0,
            8, 0,
            COLOR_AFTERNOON);

    public static final ShiftType NIGHT = new ShiftType(
            "Night",
            "Night shift (21-5)",
            21, 0,
            8, 0,
            COLOR_NIGHT);

    public static final ShiftType CUSTOM1 = new ShiftType(
            "Custom1",
            "Custom shift 1",
            0, 0,
            0, 0,
            COLOR_CUSTOM1);

    public static final ShiftType CUSTOM2 = new ShiftType(
            "Custom2",
            "Custom shift 2",
            0, 0,
            0, 0,
            COLOR_CUSTOM2);

    // Initialize cache with predefined types
    static {
        shiftTypeCache.put("MORNING", MORNING);
        shiftTypeCache.put("AFTERNOON", AFTERNOON);
        shiftTypeCache.put("NIGHT", NIGHT);
        shiftTypeCache.put("CUSTOM1", CUSTOM1);
        shiftTypeCache.put("CUSTOM2", CUSTOM2);
    }

    // Prevent instantiation
    private ShiftTypeFactory() {}

    /**
     * Gets the number of configured shifts.
     *
     * @param context Application context
     * @return Number of shifts
     */
    public static int getShiftCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SHIFT_COUNT, DEFAULT_SHIFT_COUNT);
    }

    /**
     * Sets the number of shifts.
     *
     * @param context Application context
     * @param count Number of shifts (1-5)
     */
    public static void setShiftCount(Context context, int count) {
        if (count < 1) count = 1; // At least one shift
        if (count > 5) count = 5; // Maximum 5 shifts

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_SHIFT_COUNT, count);
        editor.apply();
    }

    /**
     * Saves a shift type to preferences.
     *
     * @param context Application context
     * @param index Shift index (0-based)
     * @param shiftType Shift type to save
     */
    public static void saveShiftType(Context context, int index, ShiftType shiftType) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        editor.putString(KEY_SHIFT_NAME_PREFIX + index, shiftType.getName());
        editor.putString(KEY_SHIFT_DESC_PREFIX + index, shiftType.getDescription());
        editor.putInt(KEY_SHIFT_START_HOUR_PREFIX + index, shiftType.getStartHour());
        editor.putInt(KEY_SHIFT_START_MIN_PREFIX + index, shiftType.getStartMinute());
        editor.putInt(KEY_SHIFT_DURATION_HOURS_PREFIX + index, shiftType.getDurationHours());
        editor.putInt(KEY_SHIFT_DURATION_MINS_PREFIX + index, shiftType.getDurationMinutes());
        editor.putInt(KEY_SHIFT_COLOR_PREFIX + index, shiftType.getColor());

        editor.apply();
    }

    /**
     * Loads a shift type from preferences.
     *
     * @param context Application context
     * @param index Shift index (0-based)
     * @return Loaded shift type
     */
    public static ShiftType loadShiftType(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Default values based on index
        String defaultName = getDefaultName(index);
        String defaultDesc = getDefaultDescription(index);
        int defaultStartHour = getDefaultStartHour(index);
        int defaultColor = getDefaultColor(index);

        // Load values from preferences
        String name = prefs.getString(KEY_SHIFT_NAME_PREFIX + index, defaultName);
        String desc = prefs.getString(KEY_SHIFT_DESC_PREFIX + index, defaultDesc);
        int startHour = prefs.getInt(KEY_SHIFT_START_HOUR_PREFIX + index, defaultStartHour);
        int startMin = prefs.getInt(KEY_SHIFT_START_MIN_PREFIX + index, 0);
        int durationHours = prefs.getInt(KEY_SHIFT_DURATION_HOURS_PREFIX + index, 8);
        int durationMins = prefs.getInt(KEY_SHIFT_DURATION_MINS_PREFIX + index, 0);
        int color = prefs.getInt(KEY_SHIFT_COLOR_PREFIX + index, defaultColor);

        return new ShiftType(name, desc, startHour, startMin, durationHours, durationMins, color);
    }

    /**
     * Returns default start hour for a shift.
     */
    private static int getDefaultStartHour(int index) {
        switch (index) {
            case 0: return 5;   // Morning
            case 1: return 13;  // Afternoon
            case 2: return 21;  // Night
            default: return 5;  // Default for additional shifts
        }
    }

    /**
     * Returns default name for a shift.
     */
    private static String getDefaultName(int index) {
        switch (index) {
            case 0: return "Morning";
            case 1: return "Afternoon";
            case 2: return "Night";
            default: return "Shift " + (index + 1);
        }
    }

    /**
     * Returns default description for a shift.
     */
    private static String getDefaultDescription(int index) {
        switch (index) {
            case 0: return "Morning shift (5-13)";
            case 1: return "Afternoon shift (13-21)";
            case 2: return "Night shift (21-5)";
            default: return "Description shift " + (index + 1);
        }
    }

    /**
     * Returns a default color based on index.
     *
     * @param index Shift index
     * @return Default color
     */
    private static int getDefaultColor(int index) {
        switch (index) {
            case 0: return COLOR_MORNING;
            case 1: return COLOR_AFTERNOON;
            case 2: return COLOR_NIGHT;
            case 3: return COLOR_CUSTOM1;
            case 4: return COLOR_CUSTOM2;
            default: return Color.GRAY;
        }
    }

    /**
     * Loads all configured shift types.
     *
     * @param context Application context
     * @return List of shift types
     */
    public static List<ShiftType> loadAllShiftTypes(Context context) {
        int count = getShiftCount(context);
        List<ShiftType> shiftTypes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            shiftTypes.add(loadShiftType(context, i));
        }

        return shiftTypes;
    }

    /**
     * Resets all shift types to default values.
     *
     * @param context Application context
     */
    public static void resetToDefaults(Context context) {
        setShiftCount(context, DEFAULT_SHIFT_COUNT);

        // Set default shifts
        saveShiftType(context, 0, MORNING);
        saveShiftType(context, 1, AFTERNOON);
        saveShiftType(context, 2, NIGHT);
    }

    /**
     * Creates a new custom shift type.
     *
     * @param name Shift name
     * @param description Shift description
     * @param startHour Start hour
     * @param startMinute Start minute
     * @param durationHours Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @param color Shift color
     * @return New shift type
     */
    public static ShiftType createCustom(String name, String description,
                                         int startHour, int startMinute,
                                         int durationHours, int durationMinutes,
                                         int color) {
        return new ShiftType(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
    }

    /**
     * Creates a new custom shift type with default color.
     *
     * @param name Shift name
     * @param description Shift description
     * @param startHour Start hour
     * @param startMinute Start minute
     * @param durationHours Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @return New shift type
     */
    public static ShiftType createCustom(String name, String description,
                                         int startHour, int startMinute,
                                         int durationHours, int durationMinutes) {
        // Assign color based on name for consistency
        int color;
        switch (name.toLowerCase()) {
            case "morning": color = COLOR_MORNING; break;
            case "afternoon": color = COLOR_AFTERNOON; break;
            case "night": color = COLOR_NIGHT; break;
            default: color = Color.GRAY;
        }

        return createCustom(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
    }

    /**
     * Gets a standard shift type.
     *
     * @param index Shift index (1-based)
     * @return Standard shift type
     * @throws IllegalArgumentException if index is invalid
     */
    public static ShiftType getStandardShiftType(int index) {
        switch (index) {
            case 1: return MORNING;
            case 2: return AFTERNOON;
            case 3: return NIGHT;
            case 4: return CUSTOM1;
            case 5: return CUSTOM2;
            default: throw new IllegalArgumentException("Invalid shift index: " + index);
        }
    }
}