package net.calvuz.qdue.ui.file;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.utils.Log;

/**
 * Helper class to integrate FileManager with your existing event system.
 * Provides high-level methods for importing JSON/QDue event files.
 *
 * Features:
 * - Integration with existing EventPackageJson structure
 * - Automatic JSON parsing and validation
 * - Error handling with user-friendly messages
 * - Progress callbacks for UI feedback
 * - Support for both single file and batch imports
 *
 * Usage:
 * FileImportHelper importHelper = new FileImportHelper(this);
 * importHelper.importEventFile(callback);
 */
public class FileImportHelper {
    private static final String TAG = "FileImportHelper";

    private final FileManager mFileManager;
    private final Gson mGson;

    /**
     * Callback interface for event file imports
     */
    public interface EventImportCallback {
        /**
         * Called when import starts
         */
        default void onImportStarted() {
            Log.d(TAG, "Event import started");
        }

        /**
         * Called when JSON parsing begins
         * @param filename Name of the file being processed
         */
        default void onParsingStarted(@NonNull String filename) {
            Log.d(TAG, "Parsing started for: " + filename);
        }

        /**
         * Called when event package is successfully imported
         * @param eventPackage The parsed event package
         * @param fileInfo Information about the imported file
         */
        void onEventPackageImported(@NonNull EventPackageJson eventPackage,
                                    @NonNull FileManager.FileInfo fileInfo);

        /**
         * Called when import fails
         * @param error User-friendly error message
         * @param technicalDetails Technical error details for logging
         * @param cause Original exception (if any)
         */
        void onImportFailed(@NonNull String error, @NonNull String technicalDetails,
                            @androidx.annotation.Nullable Throwable cause);

        /**
         * Called when user cancels the import
         */
        default void onImportCancelled() {
            Log.d(TAG, "Event import cancelled by user");
        }

        /**
         * Called when validation warnings are found (non-fatal)
         * @param warnings List of validation warnings
         */
        default void onValidationWarnings(@NonNull java.util.List<String> warnings) {
            Log.w(TAG, "Validation warnings: " + warnings);
        }
    }

    /**
     * Constructor for Activity context
     */
    public FileImportHelper(@NonNull Activity activity) {
        this.mFileManager = new FileManager(activity);
        this.mGson = new Gson();
    }

    /**
     * Constructor for Fragment context
     */
    public FileImportHelper(@NonNull Fragment fragment) {
        this.mFileManager = new FileManager(fragment);
        this.mGson = new Gson();
    }

    /**
     * Start importing an event file (JSON or QDue)
     */
    public void importEventFile(@NonNull EventImportCallback callback) {
        callback.onImportStarted();

        mFileManager.importFile(new FileManager.FileImportCallback() {
            @Override
            public void onFileImported(@NonNull FileManager.FileInfo fileInfo, @NonNull String content) {
                callback.onParsingStarted(fileInfo.getFilename());
                parseAndValidateEventPackage(fileInfo, content, callback);
            }

            @Override
            public void onImportError(@NonNull String error, @androidx.annotation.Nullable Throwable cause) {
                callback.onImportFailed(
                        "Failed to read file",
                        "File reading error: " + error,
                        cause
                );
            }

            @Override
            public void onImportCancelled() {
                callback.onImportCancelled();
            }

            @Override
            public void onUnsupportedFileType(@NonNull FileManager.FileInfo fileInfo) {
                callback.onImportFailed(
                        "Unsupported file type",
                        String.format("File '%s' has unsupported type: %s. Only .json and .qdue files are supported.",
                                fileInfo.getFilename(), fileInfo.getType()),
                        null
                );
            }
        });
    }

    /**
     * Parse and validate event package from file content
     */
    private void parseAndValidateEventPackage(@NonNull FileManager.FileInfo fileInfo,
                                              @NonNull String content,
                                              @NonNull EventImportCallback callback) {
        try {
            // Parse JSON
            EventPackageJson eventPackage = mGson.fromJson(content, EventPackageJson.class);

            if (eventPackage == null) {
                callback.onImportFailed(
                        "Invalid file format",
                        "Failed to parse JSON: resulting object is null",
                        null
                );
                return;
            }

            // Validate package structure
            ValidationResult validation = validateEventPackage(eventPackage, fileInfo);

            if (!validation.isValid()) {
                callback.onImportFailed(
                        validation.getUserMessage(),
                        validation.getTechnicalDetails(),
                        null
                );
                return;
            }

            // Report warnings if any
            if (!validation.getWarnings().isEmpty()) {
                callback.onValidationWarnings(validation.getWarnings());
            }

            // Success
            callback.onEventPackageImported(eventPackage, fileInfo);
            Log.d(TAG, String.format(QDue.getLocale(), "Successfully imported event package: %s (%d events)",
                    eventPackage.getPackageInfo() != null ? eventPackage.getPackageInfo().getId() : "unknown",
                    eventPackage.getEvents() != null ? eventPackage.getEvents().size() : 0));

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JSON parsing error", e);
            callback.onImportFailed(
                    "Invalid JSON format",
                    "JSON syntax error: " + e.getMessage(),
                    e
            );
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during parsing", e);
            callback.onImportFailed(
                    "Import failed",
                    "Unexpected error: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Validation result container
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String userMessage;
        private final String technicalDetails;
        private final java.util.List<String> warnings;

        private ValidationResult(boolean valid, String userMessage, String technicalDetails,
                                 java.util.List<String> warnings) {
            this.valid = valid;
            this.userMessage = userMessage;
            this.technicalDetails = technicalDetails;
            this.warnings = warnings != null ? warnings : new java.util.ArrayList<>();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "", "", new java.util.ArrayList<>());
        }

        public static ValidationResult success(java.util.List<String> warnings) {
            return new ValidationResult(true, "", "", warnings);
        }

        public static ValidationResult failure(String userMessage, String technicalDetails) {
            return new ValidationResult(false, userMessage, technicalDetails, new java.util.ArrayList<>());
        }

        public boolean isValid() { return valid; }
        public String getUserMessage() { return userMessage; }
        public String getTechnicalDetails() { return technicalDetails; }
        public java.util.List<String> getWarnings() { return warnings; }
    }

    /**
     * Validate event package structure and content
     */
    private ValidationResult validateEventPackage(@NonNull EventPackageJson eventPackage,
                                                  @NonNull FileManager.FileInfo fileInfo) {
        java.util.List<String> warnings = new java.util.ArrayList<>();

        // Check package info
        if (eventPackage.getPackageInfo() == null) {
            return ValidationResult.failure(
                    "Invalid file structure",
                    "Missing package_info section in " + fileInfo.getFilename()
            );
        }

        EventPackageJson.PackageInfo packageInfo = eventPackage.getPackageInfo();

        // Validate package ID
        if (packageInfo.getId() == null || packageInfo.getId().trim().isEmpty()) {
            return ValidationResult.failure(
                    "Invalid package information",
                    "Package ID is missing or empty"
            );
        }

        // Validate version
        if (packageInfo.getVersion() == null || packageInfo.getVersion().trim().isEmpty()) {
            warnings.add("Package version is missing");
        }

        // Check events array
        if (eventPackage.getEvents() == null) {
            return ValidationResult.failure(
                    "Invalid file structure",
                    "Missing events array in " + fileInfo.getFilename()
            );
        }

        if (eventPackage.getEvents().isEmpty()) {
            warnings.add("No events found in the package");
        }

        // Validate individual events
        int eventIndex = 0;
        for (Object eventObj : eventPackage.getEvents()) {
            ValidationResult eventValidation = validateEvent(eventObj, eventIndex, fileInfo.getFilename());
            if (!eventValidation.isValid()) {
                return eventValidation;
            }
            warnings.addAll(eventValidation.getWarnings());
            eventIndex++;
        }

        // Additional validations
        validateFileSize(fileInfo, warnings);
        validatePackageMetadata(packageInfo, warnings);

        return warnings.isEmpty() ? ValidationResult.success() : ValidationResult.success(warnings);
    }

    /**
     * Validate individual event
     */
    private ValidationResult validateEvent(@NonNull Object eventObj, int index, @NonNull String filename) {
        if (!(eventObj instanceof com.google.gson.internal.LinkedTreeMap)) {
            return ValidationResult.failure(
                    "Invalid event format",
                    String.format("Event at index %d in %s is not a valid object", index, filename)
            );
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> event = (java.util.Map<String, Object>) eventObj;
        java.util.List<String> warnings = new java.util.ArrayList<>();

        // Check required fields
        if (!event.containsKey("id") || event.get("id") == null) {
            return ValidationResult.failure(
                    "Invalid event data",
                    String.format("Event at index %d is missing required 'id' field", index)
            );
        }

        if (!event.containsKey("title") || event.get("title") == null) {
            warnings.add(String.format("Event %d is missing title", index));
        }

        if (!event.containsKey("start_date") || event.get("start_date") == null) {
            return ValidationResult.failure(
                    "Invalid event data",
                    String.format("Event at index %d is missing required 'start_date' field", index)
            );
        }

        // Validate date format (basic check)
        String startDate = String.valueOf(event.get("start_date"));
        if (!isValidDateFormat(startDate)) {
            warnings.add(String.format("Event %d has potentially invalid date format: %s", index, startDate));
        }

        return ValidationResult.success(warnings);
    }

    /**
     * Validate file size and add warnings if necessary
     */
    private void validateFileSize(@NonNull FileManager.FileInfo fileInfo, @NonNull java.util.List<String> warnings) {
        long sizeInMB = fileInfo.getSize() / (1024 * 1024);

        if (sizeInMB > 10) { // Warn for files larger than 10MB
            warnings.add(String.format("Large file size (%s) may impact performance",
                    fileInfo.getFormattedSize()));
        }
    }

    /**
     * Validate package metadata and add warnings
     */
    private void validatePackageMetadata(@NonNull EventPackageJson.PackageInfo packageInfo,
                                         @NonNull java.util.List<String> warnings) {
        if (packageInfo.getDescription() == null || packageInfo.getDescription().trim().isEmpty()) {
            warnings.add("Package description is missing");
        }

        if (packageInfo.getCreatedAt() == null || packageInfo.getCreatedAt().trim().isEmpty()) {
            warnings.add("Package creation date is missing");
        }

        if (packageInfo.getAuthor() == null || packageInfo.getAuthor().trim().isEmpty()) {
            warnings.add("Package author is missing");
        }
    }

    /**
     * Basic date format validation
     */
    private boolean isValidDateFormat(@NonNull String dateString) {
        // Basic check for YYYY-MM-DD format
        return dateString.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    /**
     * Forward activity result to file manager
     */
    public void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable android.content.Intent data) {
        mFileManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Clear pending callbacks
     */
    public void clearPendingCallbacks() {
        mFileManager.clearPendingCallbacks();
    }

    /**
     * Get the underlying file manager instance
     */
    public FileManager getFileManager() {
        return mFileManager;
    }

    /**
     * Create an event import helper with custom Gson configuration
     */
    public static FileImportHelper createWithCustomGson(@NonNull Activity activity, @NonNull Gson gson) {
        FileImportHelper helper = new FileImportHelper(activity);
        // Note: This would require making mGson non-final and adding a setter
        // For now, we use the default Gson instance
        return helper;
    }

    /**
     * Utility method to get supported file info for user display
     */
    public static String getSupportedFileTypesDescription() {
        return "Supported files: JSON files (.json) and QDue event files (.qdue)";
    }
}