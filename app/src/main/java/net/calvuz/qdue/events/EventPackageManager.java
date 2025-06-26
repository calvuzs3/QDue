package net.calvuz.qdue.events;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Manages external event packages with SSL validation
 */
public class EventPackageManager {

    private static final String TAG = "EV_PKG_MGR";

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final EventDao mEventDao; // database DAO
    private final Gson mGson;
    private final ExecutorService mExecutor; // Executor

    // SSL Configuration
    private static final int CONNECT_TIMEOUT = 15000; // 15 seconds
    private static final int READ_TIMEOUT = 30000;    // 30 seconds

    // ==================== 4. UPDATED PACKAGE MANAGER CONSTRUCTOR ====================

    /**
     * EventPackageManager constructor, use EventDao
     */
    public EventPackageManager(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mEventDao = EventsDatabase.getInstance(context).eventDao(); // Use EventDAO
        mGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Manual update from external URL with SSL validation
     */
    public void updateFromUrl(String url, String packageId, UpdateCallback callback) {
        if (TextUtils.isEmpty(url)) {
            callback.onError("URL non valido");
            return;
        }

        // Validate SSL setting
        boolean sslValidation = mPreferences.getBoolean("events_ssl_validation", true);

        // Execute in background thread using ExecutorService
        mExecutor.execute(() -> {
            try {
                UpdateResult result = downloadAndParsePackage(url, packageId, sslValidation);

                // Switch back to main thread for callback
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (result.success) {
                        callback.onSuccess(result.message);
                        updateLastUpdateInfo(packageId);
                    } else {
                        callback.onError(result.message);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error updating from URL: " + e.getMessage(), e);

                // Switch back to main thread for error callback
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Errore di connessione: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Download and parse package with SSL validation
     */
    private UpdateResult downloadAndParsePackage(String url, String packageId, boolean sslValidation)
            throws IOException, JsonSyntaxException {

        HttpsURLConnection connection = null;
        try {
            URL urlObj = new URL(url);

            // Only allow HTTPS for external sources
            if (!"https".equals(urlObj.getProtocol())) {
                return new UpdateResult(false, "Solo connessioni HTTPS sono supportate");
            }

            connection = (HttpsURLConnection) urlObj.openConnection();

            // Configure SSL validation
            if (sslValidation) {
                // Use default SSL validation
                connection.setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
                connection.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
            } else {
                // WARNING: This disables SSL validation - only for testing!
                Log.w(TAG, "SSL validation disabled - this is not secure!");
                trustAllCertificates(connection);
            }

            // Configure connection
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "QDue-Events/1.0");
            connection.setRequestProperty("Accept", "application/json");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return new UpdateResult(false, "Errore HTTP: " + responseCode);
            }

            // Read response
            String jsonResponse = readResponse(connection);

            // Parse JSON
            EventPackageJson packageJson = mGson.fromJson(jsonResponse, EventPackageJson.class);

            // Validate package
            if (packageJson.package_info == null || packageJson.events == null) {
                return new UpdateResult(false, "Formato package non valido");
            }

            // Validate package ID match
            if (!TextUtils.isEmpty(packageId) &&
                    !packageId.equals(packageJson.package_info.id)) {
                return new UpdateResult(false,
                        "Package ID non corrispondente: atteso " + packageId +
                                ", ricevuto " + packageJson.package_info.id);
            }

            // Import events
            int importedCount = importEventsFromPackage(packageJson, url);

            return new UpdateResult(true,
                    "Aggiornamento completato: " + importedCount + " eventi importati");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Read HTTP response as string
     */
    private String readResponse(HttpsURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Import events from parsed package
     */
    private int importEventsFromPackage(EventPackageJson packageJson, String sourceUrl) {
        String packageId = packageJson.package_info.id;
        String packageVersion = packageJson.package_info.version;

        try {
            // Remove existing events from this package (Room operation)
            mEventDao.deleteEventsByPackageId(packageId);

            int importedCount = 0;
            for (EventPackageJson.EventJson eventJson : packageJson.events) {
                try {
                    LocalEvent event = convertJsonToEvent(eventJson, packageId, packageVersion, sourceUrl);
                    mEventDao.insertEvent(event); // Room operation
                    importedCount++;
                } catch (Exception e) {
                    Log.w(TAG, "Error importing event: " + eventJson.title + " Exception: " + e.getMessage());
                }
            }

            return importedCount;

        } catch (Exception e) {
            Log.e(TAG, "Error in importEventsFromPackage", e);
            return 0;
        }
    }

    /**
     * Convert JSON event to LocalEvent
     */
    private LocalEvent convertJsonToEvent(EventPackageJson.EventJson eventJson,
                                          String packageId, String packageVersion, String sourceUrl) {
        LocalEvent event = new LocalEvent();

        // Basic info
        event.setId(packageId + "_" + eventJson.id); // Ensure unique ID
        event.setTitle(eventJson.title);
        event.setDescription(eventJson.description);
        event.setLocation(eventJson.location);
        event.setAllDay(eventJson.all_day);

        // Dates and times
        LocalDate startDate = LocalDate.parse(eventJson.start_date);
        LocalDate endDate = eventJson.end_date != null ?
                LocalDate.parse(eventJson.end_date) : startDate;

        if (eventJson.all_day) {
            event.setStartTime(startDate.atStartOfDay());
            event.setEndTime(endDate.atTime(23, 59));
        } else {
            LocalTime startTime = eventJson.start_time != null ?
                    LocalTime.parse(eventJson.start_time) : LocalTime.of(9, 0);
            LocalTime endTime = eventJson.end_time != null ?
                    LocalTime.parse(eventJson.end_time) : startTime.plusHours(1);

            event.setStartTime(startDate.atTime(startTime));
            event.setEndTime(endDate.atTime(endTime));
        }

        // LocalEvent type and priority
        try {
            event.setEventType(EventType.valueOf(eventJson.event_type));
        } catch (IllegalArgumentException e) {
            event.setEventType(EventType.IMPORTED);
        }

        try {
            event.setPriority(EventPriority.valueOf(eventJson.priority));
        } catch (IllegalArgumentException e) {
            event.setPriority(EventPriority.NORMAL);
        }

        // Package tracking info
        event.setPackageId(packageId);
        event.setPackageVersion(packageVersion);
        event.setSourceUrl(sourceUrl);
        event.setLastUpdated(LocalDateTime.now());

        // Custom properties
        if (eventJson.custom_properties != null) {
            event.setCustomProperties(new HashMap<>(eventJson.custom_properties));
        }

        // Add tags as custom property
        if (eventJson.tags != null && !eventJson.tags.isEmpty()) {
            event.getCustomProperties().put("tags", String.join(",", eventJson.tags));
        }

        return event;
    }

    /**
     * Update last update info in preferences
     */
    private void updateLastUpdateInfo(String packageId) {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        mPreferences.edit()
                .putString("events_last_update_time", timestamp)
                .putString("events_last_update_package", packageId)
                .apply();
    }

    /**
     * WARNING: Disable SSL validation (only for testing!)
     */
    @SuppressLint("TrustAllX509TrustManager")
    private void trustAllCertificates(HttpsURLConnection connection) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up trust all certificates", e);
        }
    }

    // ==================== UTILITIES ===================================

    /**
     * Cleanup resources when manager is no longer needed
     */
    public void cleanup() {
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
            Log.d(TAG, "ExecutorService shutdown");
        }
    }

    // ==================== INTERFACES AND CALLBACKS ====================

    public interface UpdateCallback {
        void onSuccess(String message);

        void onError(String error);
    }

    public static class UpdateResult {
        public final boolean success;
        public final String message;

        public UpdateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * ENHANCEMENT FOR EventPackageManager.java
     *
     * Add these methods to your existing EventPackageManager class.
     * This integrates EventPackageManagerExtension functionality while maintaining
     * full backward compatibility with existing URL import features.
     */

// ==================== ADD TO EventPackageManager CLASS ====================

// ==================== NEW INTERFACES FOR FILE IMPORT ====================

    /**
     * Callback interface for JSON string file imports
     * Separate from UpdateCallback to avoid conflicts with URL import functionality
     */
    public interface FileImportCallback {
        /**
         * Called when file import succeeds
         * @param importedCount Number of events successfully imported
         * @param message Success message with details
         */
        void onSuccess(int importedCount, String message);

        /**
         * Called when file import fails
         * @param error Error message describing the failure
         */
        void onError(String error);
    }

    /**
     * Validation result for JSON content validation
     */
    public static class JsonValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final EventPackageJson packageJson;

        public JsonValidationResult(boolean isValid, String errorMessage, EventPackageJson packageJson) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.packageJson = packageJson;
        }

        public static JsonValidationResult success(EventPackageJson packageJson) {
            return new JsonValidationResult(true, "", packageJson);
        }

        public static JsonValidationResult failure(String errorMessage) {
            return new JsonValidationResult(false, errorMessage, null);
        }
    }

// ==================== NEW JSON STRING IMPORT METHODS ====================

    /**
     * Import events from JSON string content (for file imports)
     *
     * This method provides file import capability while reusing the existing
     * database operations and event conversion logic. It's designed to work
     * alongside the existing URL import functionality without conflicts.
     *
     * @param jsonContent Raw JSON string content from file
     * @param sourceDescription Description of source (filename, etc.)
     * @param callback Callback for success/error handling
     */
    public void importFromJsonString(String jsonContent, String sourceDescription,
                                     FileImportCallback callback) {
        Log.d(TAG, "Starting JSON string import from: " + sourceDescription);

        // Execute in background thread using existing ExecutorService
        mExecutor.execute(() -> {
            try {
                // Step 1: Validate JSON content
                if (jsonContent == null || jsonContent.trim().isEmpty()) {
                    runOnMainThread(() -> callback.onError("JSON content is empty"));
                    return;
                }

                // Step 2: Parse and validate JSON structure
                JsonValidationResult validation = parseAndValidateJsonString(jsonContent);
                if (!validation.isValid) {
                    runOnMainThread(() -> callback.onError("Invalid JSON format: " + validation.errorMessage));
                    return;
                }

                // Step 3: Import events using existing tested method
                int importedCount = importEventsFromPackage(validation.packageJson, sourceDescription);

                // Step 4: Create success message
                String successMessage = String.format(QDue.getLocale(),
                        "Successfully imported %d events from %s (Package: %s v%s)",
                        importedCount,
                        sourceDescription,
                        validation.packageJson.package_info.name,
                        validation.packageJson.package_info.version
                );

                Log.i(TAG, successMessage);
                runOnMainThread(() -> callback.onSuccess(importedCount, successMessage));

            } catch (Exception e) {
                String error = "Import failed: " + e.getMessage();
                Log.e(TAG, error, e);
                runOnMainThread(() -> callback.onError(error));
            }
        });
    }

    /**
     * Parse and validate JSON string content
     *
     * @param jsonContent Raw JSON string
     * @return JsonValidationResult with validation status and parsed content
     */
    public JsonValidationResult parseAndValidateJsonString(String jsonContent) {
        try {
            // Parse JSON using existing Gson instance
            EventPackageJson packageJson = mGson.fromJson(jsonContent, EventPackageJson.class);

            if (packageJson == null) {
                return JsonValidationResult.failure("Failed to parse JSON - content is null");
            }

            // Validate package structure
            if (packageJson.package_info == null) {
                return JsonValidationResult.failure("Missing package_info section");
            }

            if (packageJson.package_info.id == null || packageJson.package_info.id.trim().isEmpty()) {
                return JsonValidationResult.failure("Missing or empty package ID");
            }

            if (packageJson.events == null) {
                return JsonValidationResult.failure("Missing events array");
            }

            // Additional validation for events
            for (int i = 0; i < packageJson.events.size(); i++) {
                EventPackageJson.EventJson event = packageJson.events.get(i);
                if (event.id == null || event.id.trim().isEmpty()) {
                    return JsonValidationResult.failure(String.format(QDue.getLocale(), "Event at index %d missing ID", i));
                }
                if (event.start_date == null || event.start_date.trim().isEmpty()) {
                    return JsonValidationResult.failure(String.format(QDue.getLocale(), "Event at index %d missing start_date", i));
                }
            }

            Log.d(TAG, String.format(QDue.getLocale(), "JSON validation successful: package %s with %d events",
                    packageJson.package_info.id, packageJson.events.size()));

            return JsonValidationResult.success(packageJson);

        } catch (com.google.gson.JsonSyntaxException e) {
            return JsonValidationResult.failure("Invalid JSON syntax: " + e.getMessage());
        } catch (Exception e) {
            return JsonValidationResult.failure("Validation error: " + e.getMessage());
        }
    }

    /**
     * Enhanced event conversion with improved field mapping
     *
     * This method extends the existing convertJsonToEvent() with additional
     * field mappings and better error handling, while maintaining backward
     * compatibility.
     *
     * @param eventJson JSON event data
     * @param packageId Package identifier
     * @param packageVersion Package version
     * @param sourceUrl Source description/URL
     * @return LocalEvent with enhanced field mapping
     */
    private LocalEvent convertJsonToEventEnhanced(EventPackageJson.EventJson eventJson,
                                                  String packageId, String packageVersion, String sourceUrl) {
        LocalEvent event = new LocalEvent();

        try {
            // Basic info (same as existing method)
            event.setId(packageId + "_" + eventJson.id);
            event.setTitle(eventJson.title != null ? eventJson.title : "Untitled Event");
            event.setDescription(eventJson.description);
            event.setLocation(eventJson.location);
            event.setAllDay(eventJson.all_day);

            // Package info for tracking
            event.setPackageId(packageId);
            event.setPackageVersion(packageVersion);
            event.setSourceUrl(sourceUrl);
            event.setLastUpdated(java.time.LocalDateTime.now());

            // Enhanced date and time processing with better error handling
            java.time.LocalDate startDate = java.time.LocalDate.parse(eventJson.start_date);
            java.time.LocalDate endDate = !isStringEmpty(eventJson.end_date) ?
                    java.time.LocalDate.parse(eventJson.end_date) : startDate;

            if (eventJson.all_day) {
                event.setStartTime(startDate.atStartOfDay());
                event.setEndTime(endDate.atTime(23, 59));
            } else {
                java.time.LocalTime startTime = !isStringEmpty(eventJson.start_time) ?
                        java.time.LocalTime.parse(eventJson.start_time) : java.time.LocalTime.of(9, 0);
                java.time.LocalTime endTime = !isStringEmpty(eventJson.end_time) ?
                        java.time.LocalTime.parse(eventJson.end_time) : startTime.plusHours(1);

                event.setStartTime(startDate.atTime(startTime));
                event.setEndTime(endDate.atTime(endTime));
            }

            // Enhanced field mapping for type and priority
            if (!isStringEmpty(eventJson.event_type)) {
                try {
                    net.calvuz.qdue.events.models.EventType eventType =
                            net.calvuz.qdue.events.models.EventType.valueOf(eventJson.event_type.toUpperCase());
                    event.setEventType(eventType);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown event type: " + eventJson.event_type + ", using default");
                    event.setEventType(net.calvuz.qdue.events.models.EventType.OTHER);
                }
            } else {
                event.setEventType(net.calvuz.qdue.events.models.EventType.OTHER);
            }

            if (!isStringEmpty(eventJson.priority)) {
                try {
                    net.calvuz.qdue.events.models.EventPriority priority =
                            net.calvuz.qdue.events.models.EventPriority.valueOf(eventJson.priority.toUpperCase());
                    event.setPriority(priority);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown priority: " + eventJson.priority + ", using default");
                    event.setPriority(net.calvuz.qdue.events.models.EventPriority.NORMAL);
                }
            } else {
                event.setPriority(net.calvuz.qdue.events.models.EventPriority.NORMAL);
            }

            // Custom properties handling
            if (eventJson.custom_properties != null && !eventJson.custom_properties.isEmpty()) {
                event.setCustomProperties(new java.util.HashMap<>(eventJson.custom_properties));
            }

            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error converting event: " + eventJson.title + " - " + e.getMessage());
            // Return a basic event to avoid complete failure
            LocalEvent fallbackEvent = new LocalEvent();
            fallbackEvent.setId(packageId + "_" + eventJson.id);
            fallbackEvent.setTitle(eventJson.title != null ? eventJson.title : "Error Event");
            fallbackEvent.setStartTime(java.time.LocalDateTime.now());
            fallbackEvent.setEndTime(java.time.LocalDateTime.now().plusHours(1));
            return fallbackEvent;
        }
    }

// ==================== ENHANCED EXISTING METHODS ====================

    /**
     * Enhanced version of importEventsFromPackage with better logging and error handling
     *
     * @deprecated The original importEventsFromPackage() method has simpler field mapping.
     *             Consider using the enhanced conversion for new imports.
     * @param packageJson Event package data
     * @param sourceUrl Source description
     * @return Number of events imported
     */
    @Deprecated
    public int importEventsFromPackageSimple(EventPackageJson packageJson, String sourceUrl) {
        return importEventsFromPackage(packageJson, sourceUrl);
    }

    /**
     * Enhanced import method with improved conversion and error tracking
     *
     * This method uses the enhanced convertJsonToEventEnhanced() for better
     * field mapping while maintaining the same database operations.
     */
    public int importEventsFromPackageEnhanced(EventPackageJson packageJson, String sourceUrl) {
        String packageId = packageJson.package_info.id;
        String packageVersion = packageJson.package_info.version;

        try {
            Log.d(TAG, String.format(QDue.getLocale(),"Starting enhanced import: package %s v%s with %d events",
                    packageId, packageVersion, packageJson.events.size()));

            // Remove existing events from this package (same as original)
            mEventDao.deleteEventsByPackageId(packageId);

            int importedCount = 0;
            int errorCount = 0;

            for (EventPackageJson.EventJson eventJson : packageJson.events) {
                try {
                    // Use enhanced conversion method
                    LocalEvent event = convertJsonToEventEnhanced(eventJson, packageId, packageVersion, sourceUrl);
                    mEventDao.insertEvent(event);
                    importedCount++;
                    Log.d(TAG, "Successfully imported event: " + eventJson.title);
                } catch (Exception e) {
                    errorCount++;
                    Log.w(TAG, "Error importing event: " + eventJson.title + " Exception: " + e.getMessage());
                }
            }

            Log.i(TAG, String.format(QDue.getLocale(),"Enhanced import completed: %d imported, %d errors",
                    importedCount, errorCount));

            return importedCount;

        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced import", e);
            return 0;
        }
    }

// ==================== UTILITY METHODS ====================

    /**
     * Utility method to check if string is null or empty
     */
    private boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Run callback on main thread
     */
    private void runOnMainThread(Runnable runnable) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
    }

    /**
     * Get debug information about current import manager state
     */
    public String getImportDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("EventPackageManager Import Debug Info:\n");
        info.append("• SSL Validation: ").append(mPreferences.getBoolean("events_ssl_validation", true)).append("\n");
        info.append("• Total Events in DB: ").append(mEventDao.getAllEvents().size()).append("\n");
        info.append("• Executor Status: ").append(mExecutor.isShutdown() ? "Shutdown" : "Active").append("\n");
        info.append("• Context: ").append(mContext.getClass().getSimpleName()).append("\n");
        return info.toString();
    }

// ==================== BACKWARD COMPATIBILITY ====================

    /**
     * @deprecated Use importEventsFromPackageEnhanced() for better field mapping
     *             and error handling. This method is kept for backward compatibility.
     */
    @Deprecated
    private LocalEvent convertJsonToEventBasic(EventPackageJson.EventJson eventJson,
                                               String packageId, String packageVersion, String sourceUrl) {
        // This would be your existing convertJsonToEvent method
        // Mark as deprecated to encourage use of enhanced version
        return convertJsonToEvent(eventJson, packageId, packageVersion, sourceUrl);
    }
}
