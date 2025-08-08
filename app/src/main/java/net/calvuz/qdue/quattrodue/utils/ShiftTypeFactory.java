package net.calvuz.qdue.quattrodue.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.ui.core.common.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.calvuz.qdue.quattrodue.models.ShiftType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Dynamic factory for creating and managing shift types with UUID support.
 * <p>
 * Enhanced factory supporting variable number of shifts with external JSON API loading
 * and unique identifier management. Members access cached elements generated during
 * setup phase rather than static predefined elements.
 * <p>
 * Features:
 * - UUID-based shift type identification
 * - Backward compatibility with existing data
 * - Automatic ID generation for legacy data
 * - Multiple lookup methods (by ID, name, index)
 * - Local persistence and remote configuration capabilities
 * - Dependency injection compliant
 *
 * @author Updated 08/08/2025 - UUID Support Added
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
    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_DESCRIPTION = "description";
    private static final String JSON_KEY_START_HOUR = "startHour";
    private static final String JSON_KEY_START_MINUTE = "startMinute";
    private static final String JSON_KEY_DURATION_HOURS = "durationHours";
    private static final String JSON_KEY_DURATION_MINUTES = "durationMinutes";
    private static final String JSON_KEY_COLOR = "color";

    // Enhanced caching system with UUID support
    private static final Map<String, ShiftType> idCache = new ConcurrentHashMap<>();         // ID -> ShiftType
    private static final Map<String, ShiftType> nameCache = new ConcurrentHashMap<>();       // Name -> ShiftType
    private static final Map<Integer, ShiftType> indexCache = new ConcurrentHashMap<>();     // Index -> ShiftType
    private static final Map<String, Integer> idToIndexMap = new ConcurrentHashMap<>();      // ID -> Index

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

    // Prevent instantiation - Static factory pattern
    private ShiftTypeFactory() {}

    /**
     * Initializes the factory with a specific number of shifts.
     * Creates default shift types if no external source is configured.
     * <p>
     * This method maintains backward compatibility while providing UUID support.
     *
     * @param context Application context
     * @param shiftCount Number of shifts to create (1-8)
     * @return CompletableFuture that completes when initialization is done
     */
    public static CompletableFuture<Boolean> initialize(@NonNull Context context, int shiftCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                clearAllCaches();
                currentShiftCount = Math.max(1, Math.min(shiftCount, 8)); // Limit 1-8 shifts

                Log.d(TAG, "ShiftTypeFactory initialization started with " + currentShiftCount + " shifts");

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
    public static CompletableFuture<Boolean> initializeFromApi(@NonNull Context context, @NonNull String apiEndpoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                lastApiEndpoint = apiEndpoint;

                Log.i(TAG, "Initializing from API: " + apiEndpoint);

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
     * Gets a shift type by UUID from the cache.
     *
     * @param id Shift UUID
     * @return ShiftType or null if not found
     */
    @Nullable
    public static ShiftType getShiftTypeById(@NonNull String id) {
        ensureInitialized();
        return idCache.get(id);
    }

    /**
     * Gets a shift type by index from the cache (backward compatibility).
     *
     * @param index Shift index (0-based)
     * @return ShiftType or null if not found
     */
    @Nullable
    public static ShiftType getShiftType(int index) {
        ensureInitialized();
        return indexCache.get(index);
    }

    /**
     * Gets a shift type by name from the cache (backward compatibility).
     *
     * @param name Shift name
     * @return ShiftType or null if not found
     */
    @Nullable
    public static ShiftType getShiftType(@NonNull String name) {
        ensureInitialized();
        return nameCache.get(name);
    }

    /**
     * Gets the index of a shift type by its UUID.
     *
     * @param id Shift UUID
     * @return Index or -1 if not found
     */
    public static int getIndexById(@NonNull String id) {
        ensureInitialized();
        return idToIndexMap.getOrDefault(id, -1);
    }

    /**
     * Gets all shift types from the cache.
     *
     * @return List of all configured shift types in index order
     */
    @NonNull
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
     * Gets all shift type IDs in index order.
     *
     * @return List of all shift type UUIDs
     */
    @NonNull
    public static List<String> getAllShiftTypeIds() {
        ensureInitialized();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < currentShiftCount; i++) {
            ShiftType shift = indexCache.get(i);
            if (shift != null) {
                result.add(shift.getId());
            }
        }
        return result;
    }

    /**
     * Creates a mapping of shift type IDs to names for UI purposes.
     *
     * @return Map of ID -> Name
     */
    @NonNull
    public static Map<String, String> getIdToNameMapping() {
        ensureInitialized();
        Map<String, String> mapping = new HashMap<>();
        for (ShiftType shiftType : idCache.values()) {
            mapping.put(shiftType.getId(), shiftType.getName());
        }
        return mapping;
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
     * Checks if the factory is properly initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Adds a new shift type to the cache.
     * <p>
     * The shift type will be assigned the next available index and
     * added to all cache maps.
     *
     * @param shiftType Shift type to add (must have valid UUID)
     * @return Index assigned to the shift type, or -1 if failed
     */
    public static int addShiftType(@NonNull ShiftType shiftType) {
        if (shiftType == null || shiftType.getId() == null) {
            Log.w(TAG, "Cannot add null shift type or shift type without ID");
            return -1;
        }

        // Check for duplicate IDs
        if (idCache.containsKey(shiftType.getId())) {
            Log.w(TAG, "Shift type with ID " + shiftType.getId() + " already exists");
            return getIndexById(shiftType.getId());
        }

        int index = currentShiftCount;
        addToAllCaches(shiftType, index);
        currentShiftCount++;

        Log.d(TAG, "Added shift type: " + shiftType.getName() + " with ID " + shiftType.getId() + " at index " + index);
        return index;
    }

    /**
     * Updates an existing shift type in the cache.
     *
     * @param id New shift type UUID
     * @param shiftType New shift type data
     * @return true if update was successful
     */
    public static boolean updateShiftTypeById(@NonNull String id, @NonNull ShiftType shiftType) {
        Integer index = idToIndexMap.get(id);
        if (index == null) {
            Log.w(TAG, "Cannot update shift type with unknown ID: " + id);
            return false;
        }

        return updateShiftType(index, shiftType);
    }

    /**
     * Updates an existing shift type in the cache by index.
     *
     * @param index Index to update
     * @param shiftType New shift type
     * @return true if update was successful
     */
    public static boolean updateShiftType(int index, @NonNull ShiftType shiftType) {
        if (index < 0 || index >= currentShiftCount || shiftType == null) {
            Log.w(TAG, "Invalid index " + index + " for update or null shift type");
            return false;
        }

        // Remove old entry from all caches
        ShiftType oldShift = indexCache.get(index);
        if (oldShift != null) {
            removeFromAllCaches(oldShift);
        }

        // Add new entry
        addToAllCaches(shiftType, index);

        Log.d(TAG, "Updated shift type at index " + index + ": " + shiftType.getName() + " (ID: " + shiftType.getId() + ")");
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
    public static int createAndAddShiftType(@NonNull String name, @NonNull String description,
                                            int startHour, int startMinute,
                                            int durationHours, int durationMinutes,
                                            int color) {
        ShiftType shiftType = new ShiftType(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
        return addShiftType(shiftType);
    }

    /**
     * Refreshes shift types from the last used API endpoint.
     *
     * @param context Application context
     * @return CompletableFuture with success status
     */
    public static CompletableFuture<Boolean> refreshFromApi(@NonNull Context context) {
        if (lastApiEndpoint == null) {
            Log.w(TAG, "No API endpoint configured for refresh");
            return CompletableFuture.completedFuture(false);
        }
        return initializeFromApi(context, lastApiEndpoint);
    }

    /**
     * Saves current shift types to local storage for persistence.
     * <p>
     * Includes UUID information for future compatibility.
     *
     * @param context Application context
     * @return true if save was successful
     */
    public static boolean saveToLocalStorage(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save metadata
            editor.putInt(KEY_SHIFT_COUNT, currentShiftCount);
            editor.putLong(KEY_LAST_API_SYNC, System.currentTimeMillis());
            editor.putInt(KEY_CACHE_VERSION, 2); // Incremented for UUID support

            // Save each shift type with UUID
            for (int i = 0; i < currentShiftCount; i++) {
                ShiftType shift = indexCache.get(i);
                if (shift != null) {
                    String prefix = "shift_" + i + "_";
                    editor.putString(prefix + "id", shift.getId());
                    editor.putString(prefix + "name", shift.getName());
                    editor.putString(prefix + "description", shift.getDescription());
                    editor.putInt(prefix + "start_hour", shift.getStartHour());
                    editor.putInt(prefix + "start_minute", shift.getStartMinute());
                    editor.putInt(prefix + "duration_hours", shift.getDurationHours());
                    editor.putInt(prefix + "duration_minutes", shift.getDurationMinutes());
                    editor.putInt(prefix + "color", shift.getColor());
                    editor.putBoolean(prefix + "rest_type", shift.isRestType());
                }
            }

            editor.apply();
            Log.d(TAG, "Saved " + currentShiftCount + " shift types to local storage with UUIDs");
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
     * Clears all cache maps.
     */
    private static void clearAllCaches() {
        idCache.clear();
        nameCache.clear();
        indexCache.clear();
        idToIndexMap.clear();
        Log.d(TAG, "All caches cleared");
    }

    /**
     * Adds a shift type to all cache maps.
     *
     * @param shiftType ShiftType to add
     * @param index Index position
     */
    private static void addToAllCaches(@NonNull ShiftType shiftType, int index) {
        idCache.put(shiftType.getId(), shiftType);
        nameCache.put(shiftType.getName(), shiftType);
        indexCache.put(index, shiftType);
        idToIndexMap.put(shiftType.getId(), index);
    }

    /**
     * Removes a shift type from all cache maps.
     *
     * @param shiftType ShiftType to remove
     */
    private static void removeFromAllCaches(@NonNull ShiftType shiftType) {
        idCache.remove(shiftType.getId());
        nameCache.remove(shiftType.getName());
        idToIndexMap.remove(shiftType.getId());
        // Note: indexCache entry will be overwritten, not removed
    }

    /**
     * Creates default shift types for fallback scenarios.
     */
    private static void createDefaultShifts() {
        clearAllCaches();

        String[] defaultNames = {"Morning", "Afternoon", "Night"};
        String[] defaultDescriptions = {
                "Morning shift 06:00-14:00",
                "Afternoon shift 14:00-22:00",
                "Night shift 22:00-06:00"
        };
        int[] defaultStartHours = {6, 14, 22};

        for (int i = 0; i < currentShiftCount && i < defaultNames.length; i++) {
            ShiftType shift = new ShiftType(
                    defaultNames[i],
                    defaultDescriptions[i],
                    defaultStartHours[i],
                    0,
                    8,
                    0,
                    DEFAULT_COLORS[i % DEFAULT_COLORS.length]
            );

            addToAllCaches(shift, i);
        }

        Log.d(TAG, "Created " + currentShiftCount + " default shifts with UUIDs");
    }

    /**
     * Loads shift types from local storage with UUID migration support.
     *
     * @param context Application context
     * @return true if loading was successful
     */
    private static boolean loadFromLocalStorage(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            currentShiftCount = prefs.getInt(KEY_SHIFT_COUNT, 0);
            if (currentShiftCount == 0) {
                Log.d(TAG, "No shift types found in local storage");
                return false;
            }

            clearAllCaches();

            int cacheVersion = prefs.getInt(KEY_CACHE_VERSION, 1);
            boolean needsMigration = cacheVersion < 2;

            // Load each shift type
            for (int i = 0; i < currentShiftCount; i++) {
                String prefix = "shift_" + i + "_";
                String name = prefs.getString(prefix + "name", null);

                if (name == null) continue;

                // Get ID (with migration support)
                String id = prefs.getString(prefix + "id", null);
                // If no ID exists (old format), one will be generated by ShiftType constructor

                String description = prefs.getString(prefix + "description", "");
                int startHour = prefs.getInt(prefix + "start_hour", 8);
                int startMinute = prefs.getInt(prefix + "start_minute", 0);
                int durationHours = prefs.getInt(prefix + "duration_hours", 8);
                int durationMinutes = prefs.getInt(prefix + "duration_minutes", 0);
                int color = prefs.getInt(prefix + "color", DEFAULT_COLORS[i % DEFAULT_COLORS.length]);
                boolean restType = prefs.getBoolean(prefix + "rest_type", false);

                ShiftType shift = new ShiftType(id, name, description, startHour, startMinute,
                        durationHours, durationMinutes, color);
                shift.setRestType(restType);

                addToAllCaches(shift, i);

                if (needsMigration && id == null) {
                    Log.d(TAG, "Generated UUID for legacy shift: " + shift.getName() + " -> " + shift.getId());
                }
            }

            // Save with UUIDs if migration occurred
            if (needsMigration) {
                Log.i(TAG, "Migrating local storage to UUID format");
                saveToLocalStorage(context);
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
    @Nullable
    private static String fetchFromApi(@NonNull String apiEndpoint) {
        try {
            Request request = new Request.Builder()
                    .url(apiEndpoint)
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "QDue-ShiftTypeFactory/1.0")
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
     * Parses JSON response and populates the cache with UUID support.
     *
     * @param jsonResponse JSON string from API
     * @return true if parsing was successful
     */
    private static boolean parseJsonShifts(@NonNull String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray shiftsArray = root.getJSONArray(JSON_KEY_SHIFTS);

            clearAllCaches();
            currentShiftCount = shiftsArray.length();

            for (int i = 0; i < shiftsArray.length(); i++) {
                JSONObject shiftJson = shiftsArray.getJSONObject(i);

                // Get ID (optional in JSON, will be generated if missing)
                String id = shiftJson.optString(JSON_KEY_ID, null);

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

                ShiftType shift = new ShiftType(id, name, description, startHour, startMinute,
                        durationHours, durationMinutes, color);

                addToAllCaches(shift, i);

                if (id == null) {
                    Log.d(TAG, "Generated UUID for API shift: " + shift.getName() + " -> " + shift.getId());
                }
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
    public static void loadFromApiAsync(@NonNull Context context, @NonNull String apiEndpoint, @NonNull LoadCallback callback) {
        initializeFromApi(context, apiEndpoint).thenAccept(success -> {
            if (success) {
                callback.onSuccess(currentShiftCount);
            } else {
                callback.onError("Failed to load from API");
            }
        });
    }
}