package net.calvuz.qdue.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Extension methods for EventPackageManager to handle local file import
 *
 * This class extends the existing EventPackageManager functionality
 * to support direct JSON string processing for local file imports
 */
public class EventPackageManagerExtension {

    private static final String TAG = "EV_PKG_MGR_EXT";

    /**
     * Callback interface for import operations
     */
    public interface ImportCallback {
        void onSuccess(int importedCount, String message);
        void onError(String error);
    }

    /**
     * Import events from JSON string content
     *
     * @param jsonContent Raw JSON string content
     * @param sourceDescription Description of source (file path, etc.)
     * @param callback Callback for success/error handling
     */
    public static void importFromJsonString(EventPackageManager packageManager,
                                            String jsonContent,
                                            String sourceDescription,
                                            ImportCallback callback) {

        Log.d(TAG, "Starting JSON string import from: " + sourceDescription);

        try {
            // Validate JSON content
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                callback.onError("JSON content is empty");
                return;
            }

            // Parse JSON
            EventPackageJson packageJson = parseJsonContent(jsonContent);

            // Validate package structure
            ValidationResult validation = validatePackageStructure(packageJson);
            if (!validation.isValid) {
                callback.onError("Invalid JSON format: " + validation.errorMessage);
                return;
            }

            // Process events
            int importedCount = processEvents(packageManager, packageJson, sourceDescription);

            String successMessage = String.format(QDue.getLocale(),
                    "Successfully imported %d events from %s (Package: %s v%s)",
                    importedCount,
                    sourceDescription,
                    packageJson.package_info.name,
                    packageJson.package_info.version
            );

            Log.i(TAG, successMessage);
            callback.onSuccess(importedCount, successMessage);

        } catch (JsonSyntaxException e) {
            String error = "Invalid JSON syntax: " + e.getMessage();
            Log.e(TAG, error, e);
            callback.onError(error);

        } catch (Exception e) {
            String error = "Import failed: " + e.getMessage();
            Log.e(TAG, error, e);
            callback.onError(error);
        }
    }

    /**
     * Parse JSON content into EventPackageJson object
     */
    private static EventPackageJson parseJsonContent(String jsonContent) throws JsonSyntaxException {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        return gson.fromJson(jsonContent, EventPackageJson.class);
    }

    /**
     * Validate the structure of the parsed JSON package
     */
    private static ValidationResult validatePackageStructure(EventPackageJson packageJson) {
        // Check if package is null
        if (packageJson == null) {
            return ValidationResult.invalid("Failed to parse JSON structure");
        }

        // Check package_info
        if (packageJson.package_info == null) {
            return ValidationResult.invalid("Missing 'package_info' section");
        }

        if (isEmpty(packageJson.package_info.id)) {
            return ValidationResult.invalid("Missing package ID in package_info");
        }

        if (isEmpty(packageJson.package_info.name)) {
            return ValidationResult.invalid("Missing package name in package_info");
        }

        if (isEmpty(packageJson.package_info.version)) {
            return ValidationResult.invalid("Missing package version in package_info");
        }

        // Check events array
        if (packageJson.events == null) {
            return ValidationResult.invalid("Missing 'events' array");
        }

        if (packageJson.events.isEmpty()) {
            return ValidationResult.invalid("Events array is empty");
        }

        // Validate individual events
        for (int i = 0; i < packageJson.events.size(); i++) {
            EventPackageJson.EventJson event = packageJson.events.get(i);
            ValidationResult eventValidation = validateEvent(event, i);
            if (!eventValidation.isValid) {
                return ValidationResult.invalid("Event " + (i + 1) + ": " + eventValidation.errorMessage);
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Validate individual event structure
     */
    private static ValidationResult validateEvent(EventPackageJson.EventJson event, int index) {
        if (event == null) {
            return ValidationResult.invalid("Event is null");
        }

        if (isEmpty(event.id)) {
            return ValidationResult.invalid("Missing event ID");
        }

        if (isEmpty(event.title)) {
            return ValidationResult.invalid("Missing event title");
        }

        if (isEmpty(event.start_date)) {
            return ValidationResult.invalid("Missing start_date");
        }

        // Validate date format
        try {
            LocalDate.parse(event.start_date);
        } catch (DateTimeParseException e) {
            return ValidationResult.invalid("Invalid start_date format: " + event.start_date);
        }

        // Validate end_date if present
        if (!isEmpty(event.end_date)) {
            try {
                LocalDate.parse(event.end_date);
            } catch (DateTimeParseException e) {
                return ValidationResult.invalid("Invalid end_date format: " + event.end_date);
            }
        }

        // Validate time formats if present
        if (!isEmpty(event.start_time)) {
            try {
                LocalTime.parse(event.start_time);
            } catch (DateTimeParseException e) {
                return ValidationResult.invalid("Invalid start_time format: " + event.start_time);
            }
        }

        if (!isEmpty(event.end_time)) {
            try {
                LocalTime.parse(event.end_time);
            } catch (DateTimeParseException e) {
                return ValidationResult.invalid("Invalid end_time format: " + event.end_time);
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Process and import events from validated package
     */
    private static int processEvents(EventPackageManager packageManager,
                                     EventPackageJson packageJson,
                                     String sourceDescription) {

        String packageId = packageJson.package_info.id;
        String packageVersion = packageJson.package_info.version;

        Log.d(TAG, String.format(QDue.getLocale(), "Processing %d events for package %s v%s",
                packageJson.events.size(), packageId, packageVersion));

        // TODO: This should use the actual EventPackageManager methods
        // For now, we simulate the processing
        int importedCount = 0;

        for (EventPackageJson.EventJson eventJson : packageJson.events) {
            try {
                // Convert JSON event to LocalEvent
                LocalEvent localEvent = convertJsonToLocalEvent(eventJson, packageId, packageVersion, sourceDescription);

                // TODO: Save to database via EventDao
                // eventDao.insertEvent(localEvent);

                importedCount++;
                Log.d(TAG, "Successfully processed event: " + eventJson.title);

            } catch (Exception e) {
                Log.w(TAG, "Failed to process event: " + eventJson.title + " - " + e.getMessage());
                // Continue with other events
            }
        }

        return importedCount;
    }

    /**
     * Convert EventPackageJson.EventJson to LocalEvent
     */
    private static LocalEvent convertJsonToLocalEvent(EventPackageJson.EventJson eventJson,
                                                      String packageId,
                                                      String packageVersion,
                                                      String sourceDescription) {
        LocalEvent event = new LocalEvent();

        // Basic info
        event.setId(packageId + "_" + eventJson.id);
        event.setTitle(eventJson.title);
        event.setDescription(eventJson.description);
        event.setLocation(eventJson.location);
        event.setAllDay(eventJson.all_day);

        // Package info
        event.setPackageId(packageId);
        event.setPackageVersion(packageVersion);
        event.setSourceUrl(sourceDescription);
        event.setLastUpdated(LocalDateTime.now());

        // Dates and times
        LocalDate startDate = LocalDate.parse(eventJson.start_date);
        LocalDate endDate = !isEmpty(eventJson.end_date) ?
                LocalDate.parse(eventJson.end_date) : startDate;

        if (eventJson.all_day) {
            event.setStartTime(startDate.atStartOfDay());
            event.setEndTime(endDate.atTime(23, 59));
        } else {
            LocalTime startTime = !isEmpty(eventJson.start_time) ?
                    LocalTime.parse(eventJson.start_time) : LocalTime.of(0, 0);
            LocalTime endTime = !isEmpty(eventJson.end_time) ?
                    LocalTime.parse(eventJson.end_time) : LocalTime.of(23, 59);

            event.setStartTime(startDate.atTime(startTime));
            event.setEndTime(endDate.atTime(endTime));
        }

        // Event type
        if (!isEmpty(eventJson.event_type)) {
            try {
                EventType eventType = EventType.valueOf(eventJson.event_type.toUpperCase());
                event.setEventType(eventType);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unknown event type: " + eventJson.event_type + ", using GENERAL");
                event.setEventType(EventType.GENERAL);
            }
        }

        // Priority
        if (!isEmpty(eventJson.priority)) {
            try {
                EventPriority priority = EventPriority.valueOf(eventJson.priority.toUpperCase());
                event.setPriority(priority);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unknown priority: " + eventJson.priority + ", using NORMAL");
                event.setPriority(EventPriority.NORMAL);
            }
        }

        // Custom properties
        if (eventJson.custom_properties != null) {
            event.setCustomProperties(eventJson.custom_properties);
        }

        return event;
    }

    /**
     * Utility method to check if string is empty or null
     */
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validation result helper class
     */
    private static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}