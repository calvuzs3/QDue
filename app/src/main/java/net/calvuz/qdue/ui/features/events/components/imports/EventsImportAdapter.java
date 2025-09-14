package net.calvuz.qdue.ui.features.events.components.imports;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.domain.calendar.events.imports.EventsImportManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Events Import Adapter - Updated to use EventPackageManager instead of Extension
 * <p>
 * This adapter now uses the enhanced EventPackageManager.importFromJsonString() method
 * which provides proper database integration while maintaining 100% compatibility
 * with existing code and interfaces.
 * <p>
 * Key Changes:
 * - Uses EventPackageManager.importFromJsonString() instead of Extension
 * - Proper database saving through tested EventPackageManager methods
 * - Maintains all existing validation and error handling
 * - Compatible with existing EventsActivity integration
 * - Enhanced field mapping through EventPackageManager enhanced methods
 * <p>
 * Usage (unchanged):
 * EventsImportAdapter adapter = new EventsImportAdapter(context);
 * adapter.importFromSAFFile(uri, options, callback);
 */
public class EventsImportAdapter {
    private static final String TAG = "EventsImportAdapter";

    private final Context mContext;
    private final EventsImportManager mImportManager;
    private final EventPackageManager mPackageManager;

    /**
     * Constructor
     */
    public EventsImportAdapter(@NonNull Context context) {
        this.mContext = context;
        this.mImportManager = new EventsImportManager(context);
        this.mPackageManager = new EventPackageManager(context);
    }

    /**
     * Import events from SAF-selected file URI
     * <p>
     * This method bridges SAF file access with the enhanced EventPackageManager,
     * providing proper database integration and the same interface as before.
     *
     * @param fileUri URI from SAF file picker
     * @param options Import options (same as EventsImportManager)
     * @param callback Import callback (same as EventsImportManager)
     */
    public void importFromSAFFile(@NonNull Uri fileUri,
                                  @NonNull EventsImportManager.ImportOptions options,
                                  @NonNull EventsImportManager.ImportCallback callback) {

        Log.d(TAG, "Starting SAF-based import from URI: " + fileUri);

        try {
            // Step 1: Validate file accessibility
            if (!isFileAccessible(fileUri)) {
                callback.onError("File is not accessible or readable", null);
                return;
            }

            // Step 2: Detect and validate file format
            EventsImportManager.FileFormatInfo formatInfo = detectSAFFileFormat(fileUri);
            if (!formatInfo.supported) {
                callback.onError("Unsupported file format: " + formatInfo.description, null);
                return;
            }

            // Step 3: Read file content using SAF
            String jsonContent = readSAFFileContent(fileUri);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                callback.onError("File is empty or could not be read", null);
                return;
            }

            // Step 4: Use EventPackageManager for JSON string import (NEW APPROACH)
            mPackageManager.importFromJsonString(
                    jsonContent,
                    getFileDisplayName(fileUri),
                    new EventPackageManager.FileImportCallback() {
                        @Override
                        public void onSuccess(int importedCount, String message) {
                            // Convert EventPackageManager callback to EventsImportManager callback
                            // FIXED: Correct ImportResult constructor parameter order
                            EventsImportManager.ImportResult result = new EventsImportManager.ImportResult(
                                    true,                           // success
                                    message,                        // message
                                    importedCount,                  // totalEvents
                                    importedCount,                  // importedEvents
                                    0,                              // skippedEvents
                                    0,                              // errorEvents
                                    new java.util.ArrayList<>(),   // warnings
                                    new java.util.ArrayList<>(),   // errors
                                    new java.util.ArrayList<>(),   // importedEventsList (List<EventEntityGoogle>)
                                    null                            // validationResult
                            );
                            callback.onComplete(result);
                            Log.d(TAG, "Successfully imported via EventPackageManager: " + importedCount + " events");
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error, null);
                            Log.e(TAG, "Import failed via EventPackageManager: " + error);
                        }
                    }
            );

            Log.d(TAG, "Successfully delegated SAF import to EventPackageManager");

        } catch (Exception e) {
            Log.e(TAG, "Error in SAF import process", e);
            callback.onError("Import failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if file URI is accessible via SAF
     */
    private boolean isFileAccessible(@NonNull Uri fileUri) {
        try (InputStream inputStream = mContext.getContentResolver().openInputStream(fileUri)) {
            return inputStream != null;
        } catch (Exception e) {
            Log.w(TAG, "File not accessible: " + fileUri, e);
            return false;
        }
    }

    /**
     * Detect file format for SAF-selected files
     * <p>
     * Uses the existing EventsImportManager.FileFormatInfo structure
     * with direct constructor calls (no factory methods needed)
     */
    private EventsImportManager.FileFormatInfo detectSAFFileFormat(@NonNull Uri fileUri) {
        try {
            String filename = getFileDisplayName(fileUri);
            String mimeType = mContext.getContentResolver().getType(fileUri);

            Log.d(TAG, String.format("Detecting format - filename: %s, mimeType: %s", filename, mimeType));

            // Check by filename extension
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".json")) {
                    return new EventsImportManager.FileFormatInfo(
                            true, "JSON", "JSON Event Package");
                } else if (lowerFilename.endsWith(".qdue")) {
                    return new EventsImportManager.FileFormatInfo(
                            true, "QDUE", "QDue Event Package");
                }
            }

            // Check by MIME type
            if (mimeType != null) {
                if (mimeType.equals("application/json") || mimeType.equals("text/plain")) {
                    return new EventsImportManager.FileFormatInfo(
                            true, "JSON", "JSON Event Package");
                }
            }

            // Try content-based detection (read first few characters)
            try (InputStream inputStream = mContext.getContentResolver().openInputStream(fileUri);
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

                char[] buffer = new char[100];
                int bytesRead = reader.read(buffer);
                if (bytesRead > 0) {
                    String content = new String(buffer, 0, bytesRead).trim();
                    if (content.startsWith("{") || content.startsWith("[")) {
                        return new EventsImportManager.FileFormatInfo(
                                true, "JSON", "JSON Event Package (detected)");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error in content-based detection", e);
            }

            // Unsupported format
            return new EventsImportManager.FileFormatInfo(
                    false, "Unknown", "Unknown or unsupported file format");

        } catch (Exception e) {
            Log.e(TAG, "Error detecting file format", e);
            return new EventsImportManager.FileFormatInfo(
                    false, "Error", "Error detecting file format: " + e.getMessage());
        }
    }

    /**
     * Read complete file content from SAF URI
     */
    private String readSAFFileContent(@NonNull Uri fileUri) throws Exception {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = mContext.getContentResolver().openInputStream(fileUri);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        Log.d(TAG, String.format("Read %d characters from SAF file", content.length()));
        return content.toString();
    }

    /**
     * Get display name for SAF file
     */
    private String getFileDisplayName(@NonNull Uri fileUri) {
        String filename = null;

        try (android.database.Cursor cursor = mContext.getContentResolver().query(
                fileUri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    filename = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading filename from URI", e);
        }

        return filename != null ? filename : "Selected File";
    }

    /**
     * Get the underlying EventsImportManager instance
     * Allows access to all existing functionality if needed
     */
    @NonNull
    public EventsImportManager getImportManager() {
        return mImportManager;
    }

    /**
     * Get the underlying EventPackageManager instance
     * Provides access to the enhanced import functionality
     */
    @NonNull
    public EventPackageManager getPackageManager() {
        return mPackageManager;
    }

    /**
     * Create default import options (delegates to EventsImportManager)
     */
    @NonNull
    public static EventsImportManager.ImportOptions createDefaultOptions() {
        return EventsImportManager.createDefaultOptions();
    }

    /**
     * Create permissive import options (delegates to EventsImportManager)
     */
    @NonNull
    public static EventsImportManager.ImportOptions createPermissiveOptions() {
        return EventsImportManager.createPermissiveOptions();
    }

    /**
     * Validate file before import using EventPackageManager
     * <p>
     * This provides validation using the enhanced EventPackageManager methods
     * instead of the previous approach.
     */
    public void validateSAFFile(@NonNull Uri fileUri,
                                @NonNull ValidationCallback callback) {
        try {
            String jsonContent = readSAFFileContent(fileUri);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                callback.onValidationError("File is empty or could not be read",
                        new Exception("Empty file content"));
                return;
            }

            // Use EventPackageManager's validation capabilities
            new Thread(() -> {
                try {
                    // Test parsing with EventPackageManager's enhanced validation
                    EventPackageManager.JsonValidationResult result =
                            mPackageManager.parseAndValidateJsonString(jsonContent);

                    if (result.isValid) {
                        // Create compatible validation result
                        JsonSchemaValidator.ValidationResult validationResult =
                                new JsonSchemaValidator.ValidationResult(
                                        true, "", new java.util.ArrayList<>(), new java.util.ArrayList<>());

                        callback.onValidationComplete(validationResult, result.packageJson);
                    } else {
                        callback.onValidationError("Validation failed: " + result.errorMessage,
                                new Exception(result.errorMessage));
                    }

                } catch (Exception e) {
                    callback.onValidationError("Validation failed: " + e.getMessage(), e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error validating SAF file", e);
            callback.onValidationError("Error reading file for validation: " + e.getMessage(), e);
        }
    }

    /**
     * Validation callback interface
     */
    public interface ValidationCallback {
        void onValidationComplete(
                JsonSchemaValidator.ValidationResult result,
                EventPackageJson packageJson);
        void onValidationError(String error, Exception exception);
    }

    /**
     * Import from URL (delegates to EventsImportManager)
     * <p>
     * This method continues to use EventsImportManager for URL imports
     * while file imports use the enhanced EventPackageManager approach.
     */
    public void importFromUrl(@NonNull String url,
                              @NonNull EventsImportManager.ImportOptions options,
                              @NonNull EventsImportManager.ImportCallback callback) {

        Log.d(TAG, "Delegating URL import to EventsImportManager: " + url);
        // TODO: implementare
        //mImportManager.importFromUrl(url, "",options, callback,null);
    }

    /**
     * Get debug information for troubleshooting
     */
    @NonNull
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("EventsImportAdapter Debug Info:\n");
        info.append("• SAF File Support: Available\n");
        info.append("• EventPackageManager: ").append(mPackageManager != null ? "Available" : "NULL").append("\n");
        info.append("• EventsImportManager: ").append(mImportManager != null ? "Available" : "NULL").append("\n");

        if (mPackageManager != null) {
            info.append("\n").append(mPackageManager.getImportDebugInfo());
        }

        return info.toString();
    }
}