package net.calvuz.qdue.data.export;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * STEP 3: Export Manager for Events System
 * <p>
 * Handles export of events to JSON files in EventPackageJson format.
 * Features:
 * - Export all events or filtered selection
 * - Compatible with EventPackageJson format for import
 * - Support for both file system and content URI exports
 * - Customizable package metadata
 * - Progress reporting for large exports
 */
public class LocalEventsExportManager
{

    private static final String TAG = "EXPORT";

    // Context and dependencies
    private final Context mContext;
    private final Gson mGson;

    // Export callbacks
    public interface ExportCallback {
        void onExportComplete(ExportResult result);
        void onExportProgress(int processed, int total, String currentEvent);
        void onExportError(String error, Exception exception);
    }

    /**
     * Export options configuration
     */
    public static class ExportOptions {
        public String packageId = "exported_events";
        public String packageName = "Exported Events";
        public String packageVersion = "1.0.0";
        public String packageDescription = "Events exported from Q-DUE";
        public String authorName = "Q-DUE Events System";
        public String contactEmail = "";
        public boolean includeCustomProperties = true;
        public boolean reportProgress = true;
        public boolean prettyPrint = true;

        public static ExportOptions createDefault() {
            ExportOptions options = new ExportOptions();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            options.packageId = "qdue_export_" + timestamp;
            options.packageName = "Q-DUE Events Export " + timestamp;
            return options;
        }

        public static ExportOptions createBackup() {
            ExportOptions options = createDefault();
            options.packageName = "Q-DUE Events Backup";
            options.packageDescription = "Complete backup of Q-DUE events";
            return options;
        }
    }

    /**
     * Export result
     */
    public static class ExportResult {
        public final boolean success;
        public final String exportPath;
        public final int exportedEvents;
        public final long fileSizeBytes;
        public final String exportTime;
        public final List<String> warnings;

        private ExportResult(boolean success, String exportPath, int exportedEvents,
                             long fileSizeBytes, String exportTime, List<String> warnings) {
            this.success = success;
            this.exportPath = exportPath;
            this.exportedEvents = exportedEvents;
            this.fileSizeBytes = fileSizeBytes;
            this.exportTime = exportTime;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public static ExportResult success(String path, int count, long size, List<String> warnings) {
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            return new ExportResult(true, path, count, size, timestamp, warnings);
        }

        public static ExportResult failure(String path) {
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            return new ExportResult(false, path, 0, 0, timestamp, null);
        }

        public String getSummary() {
            if (success) {
                return String.format(QDue.getLocale(),"Exported %d events (%s)", exportedEvents, getFormattedSize());
            } else {
                return "Export failed";
            }
        }

        public String getFormattedSize() {
            if (fileSizeBytes < 1024) {
                return fileSizeBytes + " B";
            } else if (fileSizeBytes < 1024 * 1024) {
                return String.format(QDue.getLocale(),"%.1f KB", fileSizeBytes / 1024.0);
            } else {
                return String.format(QDue.getLocale(),"%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
            }
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Constructor
     */
    public LocalEventsExportManager(Context context) {
        mContext = context.getApplicationContext();
        mGson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
    }

    /**
     * Export events to URI (for SAF - Storage Access Framework)
     */
    public void exportToUri(List<LocalEvent> events, Uri destinationUri,
                            ExportOptions options, ExportCallback callback) {
        new Thread(() -> {
            try {
                ExportResult result = performExportToUri(events, destinationUri, options, callback);
                callback.onExportComplete(result);
            } catch (Exception e) {
                Log.e(TAG, "Error exporting to URI: " + destinationUri, e);
                callback.onExportError("Export failed: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Export events to file path
     */
    public void exportToFile(List<LocalEvent> events, String filePath,
                             ExportOptions options, ExportCallback callback) {
        new Thread(() -> {
            try {
                ExportResult result = performExportToFile(events, filePath, options, callback);
                callback.onExportComplete(result);
            } catch (Exception e) {
                Log.e(TAG, "Error exporting to file: " + filePath, e);
                callback.onExportError("Export failed: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Perform export to URI
     */
    private ExportResult performExportToUri(List<LocalEvent> events, Uri destinationUri,
                                            ExportOptions options, ExportCallback callback)
            throws IOException {

        Log.d(TAG, String.format(QDue.getLocale(),"Starting export of %d events to URI: %s",
                events.size(), destinationUri.toString()));

        List<String> warnings = new ArrayList<>();

        // Create EventPackageJson structure
        EventPackageJson packageJson = createPackageFromEvents(events, options, warnings, callback);

        // Write to URI using ContentResolver
        try (OutputStream outputStream = mContext.getContentResolver().openOutputStream(destinationUri);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8")) {

            if (options.prettyPrint) {
                mGson.toJson(packageJson, writer);
            } else {
                Gson compactGson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .create();
                compactGson.toJson(packageJson, writer);
            }

            writer.flush();
        }

        // Get file size (approximate for URI)
        long fileSize = estimateJsonSize(packageJson);

        Log.i(TAG, String.format(QDue.getLocale(),"Export completed to URI: %d events, %d bytes, %d warnings",
                events.size(), fileSize, warnings.size()));

        return ExportResult.success( destinationUri.toString(), events.size(), fileSize, warnings);
    }

    /**
     * Perform export to file path
     */
    private ExportResult performExportToFile(List<LocalEvent> events, String filePath,
                                             ExportOptions options, ExportCallback callback)
            throws IOException {

        Log.d(TAG, String.format(QDue.getLocale(),"Starting export of %d events to file: %s",
                events.size(), filePath));

        List<String> warnings = new ArrayList<>();

        // Create EventPackageJson structure
        EventPackageJson packageJson = createPackageFromEvents(events, options, warnings, callback);

        // Write to file
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
            if (options.prettyPrint) {
                mGson.toJson(packageJson, writer);
            } else {
                Gson compactGson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .create();
                compactGson.toJson(packageJson, writer);
            }
        }

        // Get actual file size
        java.io.File file = new java.io.File(filePath);
        long fileSize = file.length();

        Log.i(TAG, String.format(QDue.getLocale(),"Export completed to file: %d events, %d bytes, %d warnings",
                events.size(), fileSize, warnings.size()));

        return ExportResult.success( filePath, events.size(), fileSize, warnings);
    }

    /**
     * Create EventPackageJson structure from events list
     */
    private EventPackageJson createPackageFromEvents(List<LocalEvent> events,
                                                     ExportOptions options,
                                                     List<String> warnings,
                                                     ExportCallback callback) {
        EventPackageJson packageJson = new EventPackageJson();

        // Create package info
        packageJson.package_info = new EventPackageJson.PackageInfo();
        packageJson.package_info.id = options.packageId;
        packageJson.package_info.name = options.packageName;
        packageJson.package_info.version = options.packageVersion;
        packageJson.package_info.description = options.packageDescription;
        packageJson.package_info.created_date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        packageJson.package_info.author = options.authorName;
        packageJson.package_info.contact_email = options.contactEmail;

        // Set validity dates (current date to 1 year from now)
        LocalDateTime now = LocalDateTime.now();
        packageJson.package_info.valid_from = now.toLocalDate().toString();
        packageJson.package_info.valid_to = now.plusYears(1).toLocalDate().toString();

        // Convert events to JSON format
        packageJson.events = new ArrayList<>();
        int totalEvents = events.size();

        for (int i = 0; i < events.size(); i++) {
            LocalEvent event = events.get(i);

            if (options.reportProgress && callback != null) {
                callback.onExportProgress(i + 1, totalEvents, event.getTitle());
            }

            try {
                EventPackageJson.EventJson eventJson = convertLocalEventToJson(event, options, warnings);
                packageJson.events.add(eventJson);
            } catch (Exception e) {
                String warning = "Failed to export event '" + event.getTitle() + "': " + e.getMessage();
                warnings.add(warning);
                Log.w(TAG, warning + " - " + e.getMessage());
            }
        }

        return packageJson;
    }

    /**
     * Convert LocalEvent to EventPackageJson.EventJson
     */
    private EventPackageJson.EventJson convertLocalEventToJson(LocalEvent event,
                                                               ExportOptions options,
                                                               List<String> warnings) {
        EventPackageJson.EventJson eventJson = new EventPackageJson.EventJson();

        // Basic info
        eventJson.id = event.getId();
        eventJson.title = event.getTitle();
        eventJson.description = event.getDescription();
        eventJson.location = event.getLocation();
        eventJson.all_day = event.isAllDay();

        // Validate required fields
        if (eventJson.id == null || eventJson.id.trim().isEmpty()) {
            warnings.add("Event '" + eventJson.title + "' has no ID - using title as ID");
            eventJson.id = eventJson.title != null ? eventJson.title.replaceAll("[^a-zA-Z0-9_]", "_") : "unknown_event";
        }

        if (eventJson.title == null || eventJson.title.trim().isEmpty()) {
            warnings.add("Event '" + eventJson.id + "' has no title");
            eventJson.title = "Untitled Event";
        }

        // Format dates and times
        if (event.getStartTime() != null) {
            eventJson.start_date = event.getStartTime().toLocalDate().toString();
            if (!event.isAllDay()) {
                eventJson.start_time = event.getStartTime().toLocalTime().toString();
            }
        } else {
            warnings.add("Event '" + eventJson.title + "' has no start time - using current date");
            eventJson.start_date = LocalDateTime.now().toLocalDate().toString();
        }

        if (event.getEndTime() != null) {
            eventJson.end_date = event.getEndTime().toLocalDate().toString();
            if (!event.isAllDay()) {
                eventJson.end_time = event.getEndTime().toLocalTime().toString();
            }
        }

        // Event type and priority
        if (event.getEventType() != null) {
            eventJson.event_type = event.getEventType().name();
        } else {
            eventJson.event_type = "GENERAL";
        }

        if (event.getPriority() != null) {
            eventJson.priority = event.getPriority().name();
        } else {
            eventJson.priority = "NORMAL";
        }

        // Custom properties
        if (options.includeCustomProperties && event.getCustomProperties() != null &&
                !event.getCustomProperties().isEmpty()) {
            eventJson.custom_properties = event.getCustomProperties();
        }

        // Add export metadata to custom properties
        if (eventJson.custom_properties == null) {
            eventJson.custom_properties = new java.util.HashMap<>();
        }
        eventJson.custom_properties.put("exported_from", "qdue_events_system");
        eventJson.custom_properties.put("export_date", LocalDateTime.now().toString());

        return eventJson;
    }

    /**
     * Estimate JSON file size for URI exports
     */
    private long estimateJsonSize(EventPackageJson packageJson) {
        try {
            String jsonString = mGson.toJson(packageJson);
            return jsonString.getBytes("UTF-8").length;
        } catch (Exception e) {
            // Rough estimation if JSON serialization fails
            int eventsCount = packageJson.events != null ? packageJson.events.size() : 0;
            return ((long)eventsCount) * 1000; // Approximately 1KB per event
        }
    }

    /**
     * Generate suggested filename for export
     */
    public static String generateExportFilename(ExportOptions options) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "qdue_events_" + timestamp + ".json";
    }

    /**
     * Generate suggested filename with event count
     */
    public static String generateExportFilename(ExportOptions options, int eventCount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format(QDue.getLocale(),"qdue_events_%s_%devents.json", timestamp, eventCount);
    }

    /**
     * Validate events before export
     */
    public ValidationResult validateEventsForExport(List<LocalEvent> events) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (events == null || events.isEmpty()) {
            errors.add("No events to export");
            return new ValidationResult(false, errors, warnings);
        }

        for (int i = 0; i < events.size(); i++) {
            LocalEvent event = events.get(i);
            String prefix = "Event " + (i + 1) + " (" + event.getTitle() + "): ";

            if (event.getId() == null || event.getId().trim().isEmpty()) {
                warnings.add(prefix + "Missing ID, will generate from title");
            }

            if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
                warnings.add(prefix + "Missing title, will use default");
            }

            if (event.getStartTime() == null) {
                warnings.add(prefix + "Missing start time, will use current date");
            }
        }

        boolean valid = errors.isEmpty();
        Log.d(TAG, String.format(QDue.getLocale(),"Export validation: %s (%d events, %d warnings, %d errors)",
                valid ? "PASSED" : "FAILED", events.size(), warnings.size(), errors.size()));

        return new ValidationResult(valid, errors, warnings);
    }

    /**
     * Validation result for export
     */
    public static class ValidationResult {
        public final boolean valid;
        public final List<String> errors;
        public final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public boolean hasIssues() {
            return !errors.isEmpty() || !warnings.isEmpty();
        }

        public String getSummary() {
            if (valid) {
                return warnings.isEmpty() ?
                        "Ready to export" :
                        String.format(QDue.getLocale(),"Ready to export (%d warnings)", warnings.size());
            } else {
                return String.format(QDue.getLocale(),"Cannot export (%d errors)", errors.size());
            }
        }
    }
}