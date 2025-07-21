package net.calvuz.qdue.quattrodue.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.calvuz.qdue.quattrodue.models.ShiftType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Dynamic factory for creating and managing shift types.
 * <p>
 * Supports variable number of shifts with external JSON API loading.
 * Members access cached elements generated during setup phase rather
 * than static predefined elements. Provides both local persistence
 * and remote configuration capabilities.
 *
 * @author Updated 21/05/2025
 */
public class ShiftTypeFactory {

    private static final String TAG = ShiftTypeFactory.class.getSimpleName();

    // Preferences for local persistence
    private static final String PREFS_NAME = "dynamic_shift_types";
    private static final String KEY_SHIFT_COUNT = "shift_count";
    private static final String KEY_LAST_API_SYNC = "last_api_sync";
    private static final String KEY_CACHE_VERSION = "cache_version";

    // JSON keys for API response parsing
    private static final String JSON_KEY_SHIFTS = "shifts";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_DESCRIPTION = "description";
    private static final String JSON_KEY_START_HOUR = "startHour";
    private static final String JSON_KEY_START_MINUTE = "startMinute";
    private static final String JSON_KEY_DURATION_HOURS = "durationHours";
    private static final String JSON_KEY_DURATION_MINUTES = "durationMinutes";
    private static final String JSON_KEY_COLOR = "color";
    private static final String JSON_KEY_ID = "id";

    // Dynamic cache for shift types (thread-safe)
    private static final Map<String, ShiftType> dynamicCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ShiftType> indexCache = new ConcurrentHashMap<>();

    public static boolean isInitialized() {
        return initialized;
    }

    // Factory state
    private static volatile boolean initialized = false;
    private static volatile int currentShiftCount = 0;
    private static String lastApiEndpoint = null;

    // HTTP client for API calls
    private static OkHttpClient httpClient = new OkHttpClient();

    // Default colors for fallback
    private static final int[] DEFAULT_COLORS = {
            Color.parseColor("#B3E5FC"),  // Light Blue
            Color.parseColor("#FFE0B2"),  // Light Orange
            Color.parseColor("#E1BEE7"),  // Light Purple
            Color.parseColor("#C8E6C9"),  // Light Green
            Color.parseColor("#FFCDD2"),  // Light Red
            Color.parseColor("#F0F4C3"),  // Light Yellow
            Color.parseColor("#D1C4E9"),  // Light Indigo
            Color.parseColor("#FFCCBC")   // Light Deep Orange
    };

    // Prevent instantiation
    private ShiftTypeFactory() {}

    /**
     * Initializes the factory with a specific number of shifts.
     * Creates default shift types if no external source is configured.
     *
     * @param context Application context
     * @param shiftCount Number of shifts to create
     * @return CompletableFuture that completes when initialization is done
     */
    public static CompletableFuture<Boolean> initialize(Context context, int shiftCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                clearCache();
                currentShiftCount = Math.max(1, Math.min(shiftCount, 8)); // Limit 1-8 shifts

                // Try to load from local storage first
                if (loadFromLocalStorage(context)) {
                    initialized = true;
                    Log.d(TAG, "Initialized from local storage with " + currentShiftCount + " shifts");
                    return true;
                }

                // Create default shifts if no local data
                createDefaultShifts();
                saveToLocalStorage(context);
                initialized = true;

                Log.d(TAG, "Initialized with default shifts: " + currentShiftCount);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error during initialization: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Initializes the factory by loading shift types from external JSON API.
     *
     * @param context Application context
     * @param apiEndpoint URL to fetch shift types JSON
     * @return CompletableFuture with success status
     */
    public static CompletableFuture<Boolean> initializeFromApi(Context context, String apiEndpoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                lastApiEndpoint = apiEndpoint;

                // Try API first
                String jsonResponse = fetchFromApi(apiEndpoint);
                if (jsonResponse != null && parseJsonShifts(jsonResponse)) {
                    saveToLocalStorage(context);
                    initialized = true;
                    Log.d(TAG, "Initialized from API with " + currentShiftCount + " shifts");
                    return true;
                }

                // Fallback to local storage
                if (loadFromLocalStorage(context)) {
                    initialized = true;
                    Log.d(TAG, "API failed, using cached data");
                    return true;
                }

                // Last resort: default shifts
                currentShiftCount = 3;
                createDefaultShifts();
                initialized = true;
                Log.w(TAG, "API and cache failed, using defaults");
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error initializing from API: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Refreshes shift types from the last used API endpoint.
     *
     * @param context Application context
     * @return CompletableFuture with success status
     */
    public static CompletableFuture<Boolean> refreshFromApi(Context context) {
        if (lastApiEndpoint == null) {
            return CompletableFuture.completedFuture(false);
        }
        return initializeFromApi(context, lastApiEndpoint);
    }

    /**
     * Gets a shift type by index from the dynamic cache.
     *
     * @param index Shift index (0-based)
     * @return ShiftType or null if not found
     */
    public static ShiftType getShiftType(int index) {
        ensureInitialized();
        return indexCache.get(index);
    }

    /**
     * Gets a shift type by name from the dynamic cache.
     *
     * @param name Shift name
     * @return ShiftType or null if not found
     */
    public static ShiftType getShiftType(String name) {
        ensureInitialized();
        return dynamicCache.get(name);
    }

    /**
     * Gets all shift types from the dynamic cache.
     *
     * @return List of all configured shift types
     */
    public static List<ShiftType> getAllShiftTypes() {
        ensureInitialized();
        List<ShiftType> result = new ArrayList<>();
        for (int i = 0; i < currentShiftCount; i++) {
            ShiftType shift = indexCache.get(i);
            if (shift != null) {
                result.add(shift);
            }
        }
        return result;
    }

    /**
     * Gets the current number of configured shifts.
     *
     * @return Number of shifts
     */
    public static int getShiftCount() {
        ensureInitialized();
        return currentShiftCount;
    }

    /**
     * Adds a new shift type to the dynamic cache.
     *
     * @param shiftType Shift type to add
     * @return Index assigned to the shift type
     */
    public static int addShiftType(ShiftType shiftType) {
        if (shiftType == null) return -1;

        int index = currentShiftCount;
        dynamicCache.put(shiftType.getName(), shiftType);
        indexCache.put(index, shiftType);
        currentShiftCount++;

        Log.d(TAG, "Added shift type: " + shiftType.getName() + " at index " + index);
        return index;
    }

    /**
     * Updates an existing shift type in the cache.
     *
     * @param index Index to update
     * @param shiftType New shift type
     * @return true if update was successful
     */
    public static boolean updateShiftType(int index, ShiftType shiftType) {
        if (index < 0 || index >= currentShiftCount || shiftType == null) {
            return false;
        }

        // Remove old entry from name cache
        ShiftType oldShift = indexCache.get(index);
        if (oldShift != null) {
            dynamicCache.remove(oldShift.getName());
        }

        // Add new entry
        dynamicCache.put(shiftType.getName(), shiftType);
        indexCache.put(index, shiftType);

        Log.d(TAG, "Updated shift type at index " + index + ": " + shiftType.getName());
        return true;
    }

    /**
     * Creates a custom shift type and adds it to the cache.
     *
     * @param name Shift name
     * @param description Shift description
     * @param startHour Start hour (0-23)
     * @param startMinute Start minute (0-59)
     * @param durationHours Duration in hours
     * @param durationMinutes Additional duration in minutes
     * @param color Shift color (ARGB)
     * @return Index of the created shift type
     */
    public static int createAndAddShiftType(String name, String description,
                                            int startHour, int startMinute,
                                            int durationHours, int durationMinutes,
                                            int color) {
        ShiftType shiftType = new ShiftType(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
        return addShiftType(shiftType);
    }

    /**
     * Saves current shift types to local storage for persistence.
     *
     * @param context Application context
     * @return true if save was successful
     */
    public static boolean saveToLocalStorage(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save metadata
            editor.putInt(KEY_SHIFT_COUNT, currentShiftCount);
            editor.putLong(KEY_LAST_API_SYNC, System.currentTimeMillis());
            editor.putInt(KEY_CACHE_VERSION, 1);

            // Save each shift type
            for (int i = 0; i < currentShiftCount; i++) {
                ShiftType shift = indexCache.get(i);
                if (shift != null) {
                    String prefix = "shift_" + i + "_";
                    editor.putString(prefix + "name", shift.getName());
                    editor.putString(prefix + "description", shift.getDescription());
                    editor.putInt(prefix + "start_hour", shift.getStartHour());
                    editor.putInt(prefix + "start_minute", shift.getStartMinute());
                    editor.putInt(prefix + "duration_hours", shift.getDurationHours());
                    editor.putInt(prefix + "duration_minutes", shift.getDurationMinutes());
                    editor.putInt(prefix + "color", shift.getColor());
                }
            }

            editor.apply();
            Log.d(TAG, "Saved " + currentShiftCount + " shift types to local storage");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error saving to local storage: " + e.getMessage());
            return false;
        }
    }

    // === PRIVATE METHODS ===

    /**
     * Ensures the factory is initialized with default values if needed.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            Log.w(TAG, "Factory not initialized, creating defaults");
            currentShiftCount = 3;
            createDefaultShifts();
            initialized = true;
        }
    }

    /**
     * Clears both dynamic caches.
     */
    private static void clearCache() {
        dynamicCache.clear();
        indexCache.clear();
        Log.d(TAG, "Cache cleared");
    }

    /**
     * Creates default shift types for fallback scenarios.
     */
    private static void createDefaultShifts() {
        clearCache();

        String[] defaultNames = {"Morning", "Afternoon", "Night"};
        int[] defaultStartHours = {5, 13, 21};

        for (int i = 0; i < currentShiftCount && i < defaultNames.length; i++) {
            ShiftType shift = new ShiftType(
                    defaultNames[i],
                    defaultNames[i],
                    defaultStartHours[i],
                    0,
                    8,
                    0,
                    DEFAULT_COLORS[i % DEFAULT_COLORS.length]
            );

            dynamicCache.put(shift.getName(), shift);
            indexCache.put(i, shift);
        }

        Log.d(TAG, "Created " + currentShiftCount + " default shifts");
    }

    /**
     * Loads shift types from local storage.
     *
     * @param context Application context
     * @return true if loading was successful
     */
    private static boolean loadFromLocalStorage(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            currentShiftCount = prefs.getInt(KEY_SHIFT_COUNT, 0);
            if (currentShiftCount == 0) {
                return false;
            }

            clearCache();

            // Load each shift type
            for (int i = 0; i < currentShiftCount; i++) {
                String prefix = "shift_" + i + "_";
                String name = prefs.getString(prefix + "name", null);

                if (name == null) continue;

                String description = prefs.getString(prefix + "description", "");
                int startHour = prefs.getInt(prefix + "start_hour", 8);
                int startMinute = prefs.getInt(prefix + "start_minute", 0);
                int durationHours = prefs.getInt(prefix + "duration_hours", 8);
                int durationMinutes = prefs.getInt(prefix + "duration_minutes", 0);
                int color = prefs.getInt(prefix + "color", DEFAULT_COLORS[i % DEFAULT_COLORS.length]);

                ShiftType shift = new ShiftType(name, description, startHour, startMinute,
                        durationHours, durationMinutes, color);
                dynamicCache.put(name, shift);
                indexCache.put(i, shift);
            }

            Log.d(TAG, "Loaded " + currentShiftCount + " shifts from local storage");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error loading from local storage: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches JSON data from the API endpoint.
     *
     * @param apiEndpoint API URL
     * @return JSON response string or null if failed
     */
    private static String fetchFromApi(String apiEndpoint) {
        try {
            Request request = new Request.Builder()
                    .url(apiEndpoint)
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "Successfully fetched data from API");
                    return jsonData;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching from API: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parses JSON response and populates the cache.
     *
     * @param jsonResponse JSON string from API
     * @return true if parsing was successful
     */
    private static boolean parseJsonShifts(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray shiftsArray = root.getJSONArray(JSON_KEY_SHIFTS);

            clearCache();
            currentShiftCount = shiftsArray.length();

            for (int i = 0; i < shiftsArray.length(); i++) {
                JSONObject shiftJson = shiftsArray.getJSONObject(i);

                String name = shiftJson.getString(JSON_KEY_NAME);
                String description = shiftJson.optString(JSON_KEY_DESCRIPTION, "");
                int startHour = shiftJson.getInt(JSON_KEY_START_HOUR);
                int startMinute = shiftJson.optInt(JSON_KEY_START_MINUTE, 0);
                int durationHours = shiftJson.getInt(JSON_KEY_DURATION_HOURS);
                int durationMinutes = shiftJson.optInt(JSON_KEY_DURATION_MINUTES, 0);

                // Parse color (can be hex string or integer)
                int color = DEFAULT_COLORS[i % DEFAULT_COLORS.length]; // Default fallback
                if (shiftJson.has(JSON_KEY_COLOR)) {
                    try {
                        String colorStr = shiftJson.getString(JSON_KEY_COLOR);
                        if (colorStr.startsWith("#")) {
                            color = Color.parseColor(colorStr);
                        } else {
                            color = shiftJson.getInt(JSON_KEY_COLOR);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Invalid color for shift " + name + ", using default");
                    }
                }

                ShiftType shift = new ShiftType(name, description, startHour, startMinute,
                        durationHours, durationMinutes, color);
                dynamicCache.put(name, shift);
                indexCache.put(i, shift);
            }

            Log.d(TAG, "Parsed " + currentShiftCount + " shifts from JSON");
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Interface for handling API loading results.
     */
    public interface LoadCallback {
        void onSuccess(int shiftCount);
        void onError(String error);
    }

    /**
     * Async helper for loading with callback.
     *
     * @param context Application context
     * @param apiEndpoint API URL
     * @param callback Result callback
     */
    public static void loadFromApiAsync(Context context, String apiEndpoint, LoadCallback callback) {
        initializeFromApi(context, apiEndpoint).thenAccept(success -> {
            if (success) {
                callback.onSuccess(currentShiftCount);
            } else {
                callback.onError("Failed to load from API");
            }
        });
    }
}