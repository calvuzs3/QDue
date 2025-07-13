package net.calvuz.qdue.smartshifts.data.database;

import android.content.Context;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Room TypeConverters for complex data types
 * Handles JSON serialization/deserialization
 */
public class SmartShiftsConverters {

    private static final Gson gson = new Gson();

    // ===== STRING LIST CONVERTERS =====

    @TypeConverter
    public static String fromStringList(List<String> value) {
        if (value == null) {
            return null;
        }
        return gson.toJson(value);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    // ===== MAP CONVERTERS =====

    @TypeConverter
    public static String fromStringMap(Map<String, String> value) {
        if (value == null) {
            return null;
        }
        return gson.toJson(value);
    }

    @TypeConverter
    public static Map<String, String> toStringMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(value, mapType);
    }

    // ===== OBJECT MAP CONVERTERS =====

    @TypeConverter
    public static String fromObjectMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return gson.toJson(value);
    }

    @TypeConverter
    public static Map<String, Object> toObjectMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(value, mapType);
    }
}

// =====================================================================

