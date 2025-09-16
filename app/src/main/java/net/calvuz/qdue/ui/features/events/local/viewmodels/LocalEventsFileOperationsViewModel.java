package net.calvuz.qdue.ui.features.events.local.viewmodels;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.data.services.LocalEventsFileService;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocalEvents File Operations ViewModel
 *
 * <p>Specialized ViewModel for handling file-based operations on LocalEvents including
 * import, export, and file validation. This ViewModel works in coordination with
 * LocalEventsViewModel to provide file operation capabilities without coupling
 * file handling to the main events management logic.</p>
 *
 * <h3>Managed State:</h3>
 * <ul>
 *   <li><strong>importOptions</strong>: Current import configuration options</li>
 *   <li><strong>exportOptions</strong>: Current export configuration options</li>
 *   <li><strong>lastImportResult</strong>: Result of last import operation</li>
 *   <li><strong>lastExportResult</strong>: Result of last export operation</li>
 *   <li><strong>importProgress</strong>: Current import progress information</li>
 *   <li><strong>exportProgress</strong>: Current export progress information</li>
 *   <li><strong>selectedFileInfo</strong>: Information about currently selected file</li>
 * </ul>
 *
 * <h3>Operations:</h3>
 * <ul>
 *   <li>File validation and import with progress tracking</li>
 *   <li>Event export with customizable options</li>
 *   <li>File information retrieval and validation</li>
 *   <li>Progress reporting for long-running operations</li>
 * </ul>
 *
 * <h3>Coordination:</h3>
 * <p>This ViewModel coordinates with LocalEventsViewModel to refresh event data
 * after successful import operations and to get event data for export operations.</p>
 *
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel
 * @see net.calvuz.qdue.data.services.LocalEventsFileService
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsFileOperationsViewModel extends BaseViewModel {

    private static final String TAG = "LocalEventsFileOpsVM";

    // ==================== STATE KEYS ====================

    public static final String STATE_IMPORT_OPTIONS = "importOptions";
    public static final String STATE_EXPORT_OPTIONS = "exportOptions";
    public static final String STATE_LAST_IMPORT_RESULT = "lastImportResult";
    public static final String STATE_LAST_EXPORT_RESULT = "lastExportResult";
    public static final String STATE_IMPORT_PROGRESS = "importProgress";
    public static final String STATE_EXPORT_PROGRESS = "exportProgress";
    public static final String STATE_SELECTED_FILE_INFO = "selectedFileInfo";

    // ==================== OPERATION KEYS ====================

    public static final String OP_VALIDATE_FILE = "validateFile";
    public static final String OP_IMPORT_FILE = "importFile";
    public static final String OP_EXPORT_ALL = "exportAll";
    public static final String OP_EXPORT_SELECTED = "exportSelected";
    public static final String OP_GET_FILE_INFO = "getFileInfo";

    // ==================== DEPENDENCIES ====================

    private final LocalEventsFileService mFileService;
    private final LocalEventsService mEventsService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param fileService Service for file operations
     * @param eventsService Service for events data
     */
    public LocalEventsFileOperationsViewModel(@NonNull LocalEventsFileService fileService,
                                              @NonNull LocalEventsService eventsService) {
        this.mFileService = fileService;
        this.mEventsService = eventsService;
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onInitialize() {
        Log.d(TAG, "Initializing LocalEventsFileOperationsViewModel");

        // Initialize default options
        setState(STATE_IMPORT_OPTIONS, createDefaultImportOptions());
        setState(STATE_EXPORT_OPTIONS, createDefaultExportOptions());
        setState(STATE_IMPORT_PROGRESS, new ProgressInfo());
        setState(STATE_EXPORT_PROGRESS, new ProgressInfo());
    }

    @Override
    protected void onCleanup() {
        Log.d(TAG, "Cleaning up LocalEventsFileOperationsViewModel");
        // Cleanup is handled by base class
    }

    // ==================== PUBLIC API - FILE INFORMATION ====================

    /**
     * Get file information for selected file.
     *
     * @param fileUri URI of file to inspect
     */
    public void getFileInfo(@NonNull Uri fileUri) {
        Log.d(TAG, "Getting file info for: " + fileUri);

        setLoading(OP_GET_FILE_INFO, true);
        clearError(OP_GET_FILE_INFO);

        mFileService.getFileInfo(fileUri)
                .thenAccept(result -> {
                    setLoading(OP_GET_FILE_INFO, false);

                    if (result.isSuccess()) {
                        LocalEventsFileService.FileInfo fileInfo = result.getData();
                        setState(STATE_SELECTED_FILE_INFO, fileInfo);
                        Log.d(TAG, "File info retrieved: " + fileInfo.displayInfo);
                    } else {
                        setError(OP_GET_FILE_INFO, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_GET_FILE_INFO, false);
                    setError(OP_GET_FILE_INFO, "Error getting file info: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Validate file for import without importing.
     *
     * @param fileUri URI of file to validate
     */
    public void validateFileForImport(@NonNull Uri fileUri) {
        Log.d(TAG, "Validating file for import: " + fileUri);

        setLoading(OP_VALIDATE_FILE, true);
        clearError(OP_VALIDATE_FILE);

        mFileService.validateFileForImport(fileUri)
                .thenAccept(result -> {
                    setLoading(OP_VALIDATE_FILE, false);

                    if (result.isSuccess()) {
                        LocalEventsFileService.ValidationResult validation = result.getData();

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("validation", validation);
                        eventData.put("fileUri", fileUri);

                        if (validation.isValid) {
                            emitEvent(new UIActionEvent("FILE_VALIDATION_SUCCESS", eventData));
                        } else {
                            emitEvent(new UIActionEvent("FILE_VALIDATION_FAILED", eventData));
                        }

                        Log.d(TAG, "File validation completed: " + validation.isValid +
                                " (" + validation.eventCount + " events)");
                    } else {
                        setError(OP_VALIDATE_FILE, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_VALIDATE_FILE, false);
                    setError(OP_VALIDATE_FILE, "File validation error: " + throwable.getMessage());
                    return null;
                });
    }

    // ==================== PUBLIC API - IMPORT OPERATIONS ====================

    /**
     * Import events from file with default options.
     *
     * @param fileUri URI of file to import
     */
    public void importEventsFromFile(@NonNull Uri fileUri) {
        LocalEventsFileService.ImportOptions options = getImportOptions();
        importEventsFromFile(fileUri, options);
    }

    /**
     * Import events from file with custom options.
     *
     * @param fileUri URI of file to import
     * @param options Import configuration options
     */
    public void importEventsFromFile(@NonNull Uri fileUri, @NonNull LocalEventsFileService.ImportOptions options) {
        Log.d(TAG, "Importing events from file: " + fileUri);

        setLoading(OP_IMPORT_FILE, true);
        clearError(OP_IMPORT_FILE);
        setState(STATE_IMPORT_PROGRESS, new ProgressInfo());

        LocalEventsFileService.ProgressCallback progressCallback = (processed, total, currentItem) -> {
            ProgressInfo progress = new ProgressInfo(processed, total, currentItem);
            setState(STATE_IMPORT_PROGRESS, progress);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("progress", progress);
            emitEvent(new UIActionEvent("IMPORT_PROGRESS", eventData));
        };

        mFileService.importEventsFromFileWithProgress(fileUri, options, progressCallback)
                .thenAccept(result -> {
                    setLoading(OP_IMPORT_FILE, false);
                    setState(STATE_IMPORT_PROGRESS, new ProgressInfo());

                    if (result.isSuccess()) {
                        LocalEventsFileService.ImportResult importResult = result.getData();
                        setState(STATE_LAST_IMPORT_RESULT, importResult);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("result", importResult);

                        if (importResult.success) {
                            emitEvent(new UIActionEvent("IMPORT_SUCCESS", eventData));

                            // Notify that events data should be refreshed
                            emitEvent(new UIActionEvent("REFRESH_EVENTS", Map.of(
                                    "reason", "import",
                                    "importedCount", importResult.importedEvents
                            )));

                            Log.d(TAG, "Import completed successfully: " + importResult.getSummary());
                        } else {
                            emitEvent(new UIActionEvent("IMPORT_FAILED", eventData));
                        }
                    } else {
                        setError(OP_IMPORT_FILE, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_IMPORT_FILE, false);
                    setState(STATE_IMPORT_PROGRESS, new ProgressInfo());
                    setError(OP_IMPORT_FILE, "Import error: " + throwable.getMessage());
                    return null;
                });
    }

    // ==================== PUBLIC API - EXPORT OPERATIONS ====================

    /**
     * Export all events to file.
     *
     * @param fileUri Target file URI for export
     */
    public void exportAllEventsToFile(@NonNull Uri fileUri) {
        Log.d(TAG, "Exporting all events to file: " + fileUri);

        setLoading(OP_EXPORT_ALL, true);
        clearError(OP_EXPORT_ALL);
        setState(STATE_EXPORT_PROGRESS, new ProgressInfo());

        LocalEventsFileService.ProgressCallback progressCallback = (processed, total, currentItem) -> {
            ProgressInfo progress = new ProgressInfo(processed, total, currentItem);
            setState(STATE_EXPORT_PROGRESS, progress);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("progress", progress);
            emitEvent(new UIActionEvent("EXPORT_PROGRESS", eventData));
        };

        LocalEventsFileService.ExportOptions options = getExportOptions();

        mFileService.exportAllEventsToFile(fileUri)
                .thenAccept(result -> {
                    setLoading(OP_EXPORT_ALL, false);
                    setState(STATE_EXPORT_PROGRESS, new ProgressInfo());

                    if (result.isSuccess()) {
                        LocalEventsFileService.ExportResult exportResult = result.getData();
                        setState(STATE_LAST_EXPORT_RESULT, exportResult);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("result", exportResult);

                        if (exportResult.success) {
                            emitEvent(new UIActionEvent("EXPORT_SUCCESS", eventData));
                            Log.d(TAG, "Export completed successfully: " + exportResult.getSummary());
                        } else {
                            emitEvent(new UIActionEvent("EXPORT_FAILED", eventData));
                        }
                    } else {
                        setError(OP_EXPORT_ALL, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_EXPORT_ALL, false);
                    setState(STATE_EXPORT_PROGRESS, new ProgressInfo());
                    setError(OP_EXPORT_ALL, "Export error: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Export selected events to file.
     *
     * @param fileUri Target file URI for export
     * @param events List of events to export
     */
    public void exportSelectedEventsToFile(@NonNull Uri fileUri, @NonNull List<LocalEvent> events) {
        Log.d(TAG, "Exporting " + events.size() + " selected events to file: " + fileUri);

        if (events.isEmpty()) {
            emitEvent(new UIActionEvent("SHOW_ERROR",
                                        Map.of("message", "No events selected for export")));
            return;
        }

        setLoading(OP_EXPORT_SELECTED, true);
        clearError(OP_EXPORT_SELECTED);
        setState(STATE_EXPORT_PROGRESS, new ProgressInfo());

        LocalEventsFileService.ProgressCallback progressCallback = (processed, total, currentItem) -> {
            ProgressInfo progress = new ProgressInfo(processed, total, currentItem);
            setState(STATE_EXPORT_PROGRESS, progress);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("progress", progress);
            emitEvent(new UIActionEvent("EXPORT_PROGRESS", eventData));
        };

        LocalEventsFileService.ExportOptions options = getExportOptions();

        mFileService.exportEventsToFileWithProgress(fileUri, events, options, progressCallback)
                .thenAccept(result -> {
                    setLoading(OP_EXPORT_SELECTED, false);
                    setState(STATE_EXPORT_PROGRESS, new ProgressInfo());

                    if (result.isSuccess()) {
                        LocalEventsFileService.ExportResult exportResult = result.getData();
                        setState(STATE_LAST_EXPORT_RESULT, exportResult);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("result", exportResult);

                        if (exportResult.success) {
                            emitEvent(new UIActionEvent("EXPORT_SUCCESS", eventData));
                            Log.d(TAG, "Selected events export completed: " + exportResult.getSummary());
                        } else {
                            emitEvent(new UIActionEvent("EXPORT_FAILED", eventData));
                        }
                    } else {
                        setError(OP_EXPORT_SELECTED, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_EXPORT_SELECTED, false);
                    setState(STATE_EXPORT_PROGRESS, new ProgressInfo());
                    setError(OP_EXPORT_SELECTED, "Export error: " + throwable.getMessage());
                    return null;
                });
    }

    // ==================== PUBLIC API - OPTIONS MANAGEMENT ====================

    /**
     * Update import options.
     *
     * @param options New import options
     */
    public void updateImportOptions(@NonNull LocalEventsFileService.ImportOptions options) {
        setState(STATE_IMPORT_OPTIONS, options);
        Log.d(TAG, "Import options updated");
    }

    /**
     * Update export options.
     *
     * @param options New export options
     */
    public void updateExportOptions(@NonNull LocalEventsFileService.ExportOptions options) {
        setState(STATE_EXPORT_OPTIONS, options);
        Log.d(TAG, "Export options updated");
    }

    /**
     * Reset import options to defaults.
     */
    public void resetImportOptions() {
        setState(STATE_IMPORT_OPTIONS, createDefaultImportOptions());
        Log.d(TAG, "Import options reset to defaults");
    }

    /**
     * Reset export options to defaults.
     */
    public void resetExportOptions() {
        setState(STATE_EXPORT_OPTIONS, createDefaultExportOptions());
        Log.d(TAG, "Export options reset to defaults");
    }

    // ==================== PUBLIC API - GETTERS ====================

    /**
     * Get current import options.
     */
    @NonNull
    public LocalEventsFileService.ImportOptions getImportOptions() {
        LocalEventsFileService.ImportOptions options = getState(STATE_IMPORT_OPTIONS,
                                                                LocalEventsFileService.ImportOptions.class);
        return options != null ? options : createDefaultImportOptions();
    }

    /**
     * Get current export options.
     */
    @NonNull
    public LocalEventsFileService.ExportOptions getExportOptions() {
        LocalEventsFileService.ExportOptions options = getState(STATE_EXPORT_OPTIONS,
                                                                LocalEventsFileService.ExportOptions.class);
        return options != null ? options : createDefaultExportOptions();
    }

    /**
     * Get last import result.
     */
    @Nullable
    public LocalEventsFileService.ImportResult getLastImportResult() {
        return getState(STATE_LAST_IMPORT_RESULT, LocalEventsFileService.ImportResult.class);
    }

    /**
     * Get last export result.
     */
    @Nullable
    public LocalEventsFileService.ExportResult getLastExportResult() {
        return getState(STATE_LAST_EXPORT_RESULT, LocalEventsFileService.ExportResult.class);
    }

    /**
     * Get current import progress.
     */
    @NonNull
    public ProgressInfo getImportProgress() {
        ProgressInfo progress = getState(STATE_IMPORT_PROGRESS, ProgressInfo.class);
        return progress != null ? progress : new ProgressInfo();
    }

    /**
     * Get current export progress.
     */
    @NonNull
    public ProgressInfo getExportProgress() {
        ProgressInfo progress = getState(STATE_EXPORT_PROGRESS, ProgressInfo.class);
        return progress != null ? progress : new ProgressInfo();
    }

    /**
     * Get selected file info.
     */
    @Nullable
    public LocalEventsFileService.FileInfo getSelectedFileInfo() {
        return getState(STATE_SELECTED_FILE_INFO, LocalEventsFileService.FileInfo.class);
    }

    /**
     * Get supported file types description.
     */
    @NonNull
    public String getSupportedFileTypesDescription() {
        return mFileService.getSupportedFileTypesDescription();
    }

    // ==================== PUBLIC API - UTILITY METHODS ====================

    /**
     * Check if file is supported for import.
     *
     * @param fileUri URI of file to check
     */
    public void checkFileSupport(@NonNull Uri fileUri) {
        mFileService.isFileSupported(fileUri)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        boolean isSupported = result.getData();

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("fileUri", fileUri);
                        eventData.put("supported", isSupported);

                        if (isSupported) {
                            emitEvent(new UIActionEvent("FILE_SUPPORTED", eventData));
                        } else {
                            emitEvent(new UIActionEvent("FILE_NOT_SUPPORTED", eventData));
                        }
                    }
                })
                .exceptionally(throwable -> {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("fileUri", fileUri);
                    eventData.put("error", throwable.getMessage());
                    emitEvent(new UIActionEvent("FILE_SUPPORT_CHECK_ERROR", eventData));
                    return null;
                });
    }

    /**
     * Clear all operation results and progress.
     */
    public void clearResults() {
        setState(STATE_LAST_IMPORT_RESULT, null);
        setState(STATE_LAST_EXPORT_RESULT, null);
        setState(STATE_IMPORT_PROGRESS, new ProgressInfo());
        setState(STATE_EXPORT_PROGRESS, new ProgressInfo());
        setState(STATE_SELECTED_FILE_INFO, null);
        clearAllErrors();
        Log.d(TAG, "All results and progress cleared");
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Create default import options.
     */
    private LocalEventsFileService.ImportOptions createDefaultImportOptions() {
        LocalEventsFileService.ImportOptions options = new LocalEventsFileService.ImportOptions();
        options.validateBeforeImport = true;
        options.reportProgress = true;
        options.preserveExistingEvents = true;
        options.allowPartialImport = true;
        options.conflictResolution = LocalEventsFileService.ImportOptions.ConflictResolution.SKIP_DUPLICATE;
        return options;
    }

    /**
     * Create default export options.
     */
    private LocalEventsFileService.ExportOptions createDefaultExportOptions() {
        LocalEventsFileService.ExportOptions options = new LocalEventsFileService.ExportOptions();
        options.packageName = "QDue LocalEvents Export";
        options.packageDescription = "LocalEvents export from QDue MVVM";
        options.authorName = "QDue App";
        options.includeCustomProperties = true;
        options.reportProgress = true;
        options.prettyFormat = true;
        return options;
    }

    // ==================== INNER CLASSES ====================

    /**
     * Progress information for long-running operations.
     */
    public static class ProgressInfo {
        private final int processed;
        private final int total;
        private final String currentItem;
        private final boolean active;

        public ProgressInfo() {
            this(0, 0, "");
        }

        public ProgressInfo(int processed, int total, String currentItem) {
            this.processed = processed;
            this.total = total;
            this.currentItem = currentItem != null ? currentItem : "";
            this.active = total > 0;
        }

        public int getProcessed() { return processed; }
        public int getTotal() { return total; }
        public String getCurrentItem() { return currentItem; }
        public boolean isActive() { return active; }

        public int getPercentage() {
            if (total <= 0) return 0;
            return Math.round((processed * 100.0f) / total);
        }

        public String getProgressText() {
            if (!active) return "";
            return String.format("%d/%d (%d%%)", processed, total, getPercentage());
        }

        public String getDetailedProgressText() {
            if (!active) return "";
            String progress = getProgressText();
            if (!currentItem.isEmpty()) {
                progress += " - " + currentItem;
            }
            return progress;
        }

        @Override
        public String toString() {
            return String.format("ProgressInfo{processed=%d, total=%d, currentItem='%s', active=%s}",
                                 processed, total, currentItem, active);
        }
    }
}