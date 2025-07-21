package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Helper class for JSON operations in SmartShifts.
 *
 * Provides utility methods for:
 * - JSON serialization and deserialization
 * - File I/O operations with JSON
 * - Pretty printing JSON
 * - Error handling for JSON operations
 * - Type-safe conversions
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class JsonHelper {

    private static final String TAG = "JsonHelper";

    // Gson instances
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();

    private static final Gson GSON_PRETTY = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .setPrettyPrinting()
            .create();

    // Private constructor to prevent instantiation
    private JsonHelper() {
        throw new UnsupportedOperationException("JsonHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // BASIC JSON OPERATIONS
    // ============================================

    /**
     * Convert object to JSON string
     */
    @NonNull
    public static String toJson(@NonNull Object object) {
        try {
            return GSON.toJson(object);
        } catch (Exception e) {
            throw new JsonOperationException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Convert object to pretty-printed JSON string
     */
    @NonNull
    public static String toJsonPretty(@NonNull Object object) {
        try {
            return GSON_PRETTY.toJson(object);
        } catch (Exception e) {
            throw new JsonOperationException("Failed to serialize object to pretty JSON", e);
        }
    }

    /**
     * Convert JSON string to object of specified class
     */
    @NonNull
    public static <T> T fromJson(@NonNull String json, @NonNull Class<T> classOfT) {
        try {
            T result = GSON.fromJson(json, classOfT);
            if (result == null) {
                throw new JsonOperationException("JSON deserialization resulted in null object");
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new JsonOperationException("Invalid JSON syntax", e);
        } catch (Exception e) {
            throw new JsonOperationException("Failed to deserialize JSON to " + classOfT.getSimpleName(), e);
        }
    }

    /**
     * Convert JSON string to object of specified type
     */
    @NonNull
    public static <T> T fromJson(@NonNull String json, @NonNull Type typeOfT) {
        try {
            T result = GSON.fromJson(json, typeOfT);
            if (result == null) {
                throw new JsonOperationException("JSON deserialization resulted in null object");
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new JsonOperationException("Invalid JSON syntax", e);
        } catch (Exception e) {
            throw new JsonOperationException("Failed to deserialize JSON to specified type", e);
        }
    }

    /**
     * Safe JSON parsing with fallback
     */
    @Nullable
    public static <T> T fromJsonSafe(@NonNull String json, @NonNull Class<T> classOfT) {
        try {
            return fromJson(json, classOfT);
        } catch (JsonOperationException e) {
            return null;
        }
    }

    /**
     * Safe JSON parsing with fallback value
     */
    @NonNull
    public static <T> T fromJsonSafe(@NonNull String json, @NonNull Class<T> classOfT, @NonNull T fallback) {
        try {
            return fromJson(json, classOfT);
        } catch (JsonOperationException e) {
            return fallback;
        }
    }

    // ============================================
    // FILE I/O OPERATIONS
    // ============================================

    /**
     * Write JSON string to file
     */
    public static void writeJsonToFile(@NonNull String json, @NonNull File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        } catch (IOException e) {
            throw new IOException("Failed to write JSON to file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Write object as JSON to file
     */
    public static void writeObjectToFile(@NonNull Object object, @NonNull File file) throws IOException {
        String json = toJsonPretty(object);
        writeJsonToFile(json, file);
    }

    /**
     * Read JSON string from file
     */
    @NonNull
    public static String readJsonFromFile(@NonNull File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IOException("Cannot read file: " + file.getAbsolutePath());
        }

        StringBuilder content = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charsRead);
            }
        } catch (IOException e) {
            throw new IOException("Failed to read JSON from file: " + file.getAbsolutePath(), e);
        }

        return content.toString();
    }

    /**
     * Read object from JSON file
     */
    @NonNull
    public static <T> T readObjectFromFile(@NonNull File file, @NonNull Class<T> classOfT) throws IOException {
        String json = readJsonFromFile(file);
        return fromJson(json, classOfT);
    }

    /**
     * Read object from JSON file with type
     */
    @NonNull
    public static <T> T readObjectFromFile(@NonNull File file, @NonNull Type typeOfT) throws IOException {
        String json = readJsonFromFile(file);
        return fromJson(json, typeOfT);
    }

    /**
     * Safe read object from file with fallback
     */
    @Nullable
    public static <T> T readObjectFromFileSafe(@NonNull File file, @NonNull Class<T> classOfT) {
        try {
            return readObjectFromFile(file, classOfT);
        } catch (IOException | JsonOperationException e) {
            return null;
        }
    }

    // ============================================
    // COLLECTION OPERATIONS
    // ============================================

    /**
     * Convert JSON string to List of objects
     */
    @NonNull
    public static <T> List<T> fromJsonToList(@NonNull String json, @NonNull Class<T> classOfT) {
        Type listType = TypeToken.getParameterized(List.class, classOfT).getType();
        return fromJson(json, listType);
    }

    /**
     * Convert JSON string to Map
     */
    @NonNull
    public static Map<String, Object> fromJsonToMap(@NonNull String json) {
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        return fromJson(json, mapType);
    }

    /**
     * Convert JSON string to typed Map
     */
    @NonNull
    public static <T> Map<String, T> fromJsonToMap(@NonNull String json, @NonNull Class<T> valueClass) {
        Type mapType = TypeToken.getParameterized(Map.class, String.class, valueClass).getType();
        return fromJson(json, mapType);
    }

    /**
     * Read List from JSON file
     */
    @NonNull
    public static <T> List<T> readListFromFile(@NonNull File file, @NonNull Class<T> classOfT) throws IOException {
        String json = readJsonFromFile(file);
        return fromJsonToList(json, classOfT);
    }

    /**
     * Write List to JSON file
     */
    public static void writeListToFile(@NonNull List<?> list, @NonNull File file) throws IOException {
        writeObjectToFile(list, file);
    }

    // ============================================
    // VALIDATION OPERATIONS
    // ============================================

    /**
     * Check if string is valid JSON
     */
    public static boolean isValidJson(@NonNull String json) {
        try {
            GSON.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Validate JSON structure against expected fields
     */
    public static ValidationResult validateJsonStructure(@NonNull String json, @NonNull String[] requiredFields) {
        try {
            Map<String, Object> jsonMap = fromJsonToMap(json);

            for (String field : requiredFields) {
                if (!jsonMap.containsKey(field)) {
                    return new ValidationResult(false, "Missing required field: " + field);
                }
            }

            return new ValidationResult(true, "JSON structure is valid");

        } catch (JsonOperationException e) {
            return new ValidationResult(false, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Get JSON size in bytes
     */
    public static long getJsonSizeBytes(@NonNull String json) {
        return json.getBytes().length;
    }

    /**
     * Get formatted JSON size
     */
    @NonNull
    public static String getFormattedJsonSize(@NonNull String json) {
        long bytes = getJsonSizeBytes(json);
        return StringHelper.formatFileSize(bytes);
    }

    // ============================================
    // TRANSFORMATION OPERATIONS
    // ============================================

    /**
     * Minify JSON (remove formatting)
     */
    @NonNull
    public static String minifyJson(@NonNull String json) {
        try {
            Object obj = GSON.fromJson(json, Object.class);
            return GSON.toJson(obj);
        } catch (JsonSyntaxException e) {
            throw new JsonOperationException("Cannot minify invalid JSON", e);
        }
    }

    /**
     * Prettify JSON (add formatting)
     */
    @NonNull
    public static String prettifyJson(@NonNull String json) {
        try {
            Object obj = GSON.fromJson(json, Object.class);
            return GSON_PRETTY.toJson(obj);
        } catch (JsonSyntaxException e) {
            throw new JsonOperationException("Cannot prettify invalid JSON", e);
        }
    }

    /**
     * Deep clone object using JSON serialization
     */
    @NonNull
    public static <T> T deepClone(@NonNull T object, @NonNull Class<T> classOfT) {
        String json = toJson(object);
        return fromJson(json, classOfT);
    }

    /**
     * Merge two JSON objects (second overrides first)
     */
    @NonNull
    public static String mergeJsonObjects(@NonNull String json1, @NonNull String json2) {
        Map<String, Object> map1 = fromJsonToMap(json1);
        Map<String, Object> map2 = fromJsonToMap(json2);

        map1.putAll(map2); // map2 values override map1

        return toJson(map1);
    }

    // ============================================
    // BACKUP/EXPORT SPECIFIC OPERATIONS
    // ============================================

    /**
     * Create backup metadata JSON
     */
    @NonNull
    public static String createBackupMetadata(@NonNull String appVersion, @NonNull String dataVersion) {
        BackupMetadata metadata = new BackupMetadata();
        metadata.appVersion = appVersion;
        metadata.dataVersion = dataVersion;
        metadata.timestamp = System.currentTimeMillis();
        metadata.format = "SmartShifts-JSON-v1.0";

        return toJsonPretty(metadata);
    }

    /**
     * Validate backup JSON structure
     */
    public static ValidationResult validateBackupJson(@NonNull String json) {
        String[] requiredFields = {"metadata", "shiftTypes", "shiftPatterns", "userAssignments"};
        return validateJsonStructure(json, requiredFields);
    }

    /**
     * Extract metadata from backup JSON
     */
    @Nullable
    public static BackupMetadata extractBackupMetadata(@NonNull String backupJson) {
        try {
            Map<String, Object> root = fromJsonToMap(backupJson);
            Object metadataObj = root.get("metadata");

            if (metadataObj != null) {
                String metadataJson = toJson(metadataObj);
                return fromJson(metadataJson, BackupMetadata.class);
            }

            return null;
        } catch (JsonOperationException e) {
            return null;
        }
    }

    // ============================================
    // INNER CLASSES
    // ============================================

    /**
     * JSON operation exception
     */
    public static class JsonOperationException extends RuntimeException {
        public JsonOperationException(String message) {
            super(message);
        }

        public JsonOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JSON validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * Backup metadata structure
     */
    public static class BackupMetadata {
        public String appVersion;
        public String dataVersion;
        public long timestamp;
        public String format;
        public String description;

        public BackupMetadata() {
            // Default constructor for JSON deserialization
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get JSON type name
     */
    @NonNull
    public static String getJsonTypeName(@NonNull Object object) {
        if (object == null) return "null";

        if (object instanceof String) return "string";
        if (object instanceof Number) return "number";
        if (object instanceof Boolean) return "boolean";
        if (object instanceof List) return "array";
        if (object instanceof Map) return "object";

        return "unknown";
    }

    /**
     * Count JSON elements (for arrays and objects)
     */
    public static int countJsonElements(@NonNull String json) {
        try {
            Object obj = GSON.fromJson(json, Object.class);

            if (obj instanceof List) {
                return ((List<?>) obj).size();
            } else if (obj instanceof Map) {
                return ((Map<?, ?>) obj).size();
            } else {
                return 1; // Single value
            }
        } catch (JsonSyntaxException e) {
            return 0;
        }
    }

    /**
     * Extract specific field from JSON
     */
    @Nullable
    public static Object extractJsonField(@NonNull String json, @NonNull String fieldName) {
        try {
            Map<String, Object> map = fromJsonToMap(json);
            return map.get(fieldName);
        } catch (JsonOperationException e) {
            return null;
        }
    }

    /**
     * Extract string field with fallback
     */
    @NonNull
    public static String extractStringField(@NonNull String json, @NonNull String fieldName, @NonNull String fallback) {
        Object value = extractJsonField(json, fieldName);
        return value instanceof String ? (String) value : fallback;
    }

    /**
     * Extract number field with fallback
     */
    public static double extractNumberField(@NonNull String json, @NonNull String fieldName, double fallback) {
        Object value = extractJsonField(json, fieldName);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    /**
     * Extract boolean field with fallback
     */
    public static boolean extractBooleanField(@NonNull String json, @NonNull String fieldName, boolean fallback) {
        Object value = extractJsonField(json, fieldName);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    /**
     * Check if JSON contains specific field
     */
    public static boolean hasJsonField(@NonNull String json, @NonNull String fieldName) {
        try {
            Map<String, Object> map = fromJsonToMap(json);
            return map.containsKey(fieldName);
        } catch (JsonOperationException e) {
            return false;
        }
    }

    /**
     * Remove field from JSON object
     */
    @NonNull
    public static String removeJsonField(@NonNull String json, @NonNull String fieldName) {
        try {
            Map<String, Object> map = fromJsonToMap(json);
            map.remove(fieldName);
            return toJson(map);
        } catch (JsonOperationException e) {
            return json; // Return original if operation fails
        }
    }

    /**
     * Add or update field in JSON object
     */
    @NonNull
    public static String setJsonField(@NonNull String json, @NonNull String fieldName, @NonNull Object value) {
        try {
            Map<String, Object> map = fromJsonToMap(json);
            map.put(fieldName, value);
            return toJson(map);
        } catch (JsonOperationException e) {
            return json; // Return original if operation fails
        }
    }

    // ============================================
    // STREAMING AND LARGE FILE OPERATIONS
    // ============================================

    /**
     * Check if file is too large for memory operation
     */
    public static boolean isFileTooLarge(@NonNull File file, long maxSizeBytes) {
        return file.length() > maxSizeBytes;
    }

    /**
     * Get recommended max file size for JSON operations (50MB)
     */
    public static long getRecommendedMaxFileSize() {
        return 50 * 1024 * 1024; // 50MB
    }

    /**
     * Validate file size before JSON operation
     */
    public static ValidationResult validateFileSize(@NonNull File file) {
        long maxSize = getRecommendedMaxFileSize();

        if (!file.exists()) {
            return new ValidationResult(false, "File does not exist");
        }

        if (file.length() == 0) {
            return new ValidationResult(false, "File is empty");
        }

        if (isFileTooLarge(file, maxSize)) {
            return new ValidationResult(false,
                    String.format("File too large: %s (max: %s)",
                            StringHelper.formatFileSize(file.length()),
                            StringHelper.formatFileSize(maxSize)));
        }

        return new ValidationResult(true, "File size is acceptable");
    }

    // ============================================
    // PERFORMANCE HELPERS
    // ============================================

    /**
     * Benchmark JSON operation
     */
    public static long benchmarkJsonOperation(@NonNull Runnable operation) {
        long startTime = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get JSON complexity score (nested objects/arrays count)
     */
    public static int getJsonComplexityScore(@NonNull String json) {
        try {
            return calculateComplexity(GSON.fromJson(json, Object.class));
        } catch (JsonSyntaxException e) {
            return -1; // Invalid JSON
        }
    }

    private static int calculateComplexity(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            int complexity = map.size();
            for (Object value : map.values()) {
                complexity += calculateComplexity(value);
            }
            return complexity;
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            int complexity = list.size();
            for (Object item : list) {
                complexity += calculateComplexity(item);
            }
            return complexity;
        } else {
            return 1; // Primitive value
        }
    }

    /**
     * Estimate JSON processing time based on size and complexity
     */
    public static long estimateProcessingTimeMs(@NonNull String json) {
        long size = getJsonSizeBytes(json);
        int complexity = getJsonComplexityScore(json);

        // Simple heuristic: base time + size factor + complexity factor
        long baseTime = 10; // 10ms base
        long sizeFactor = size / 1024; // 1ms per KB
        long complexityFactor = complexity / 10; // 1ms per 10 complexity points

        return baseTime + sizeFactor + complexityFactor;
    }
}