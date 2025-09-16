package net.calvuz.qdue.data.services;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LocalEvents File Operations Service Interface
 *
 * <p>Dedicated service interface for file-based operations on LocalEvents including
 * import, export, and validation. This service handles complex file operations
 * that were previously embedded in the Activity, providing a clean separation
 * of concerns in the MVVM architecture.</p>
 *
 * <h3>Service Features:</h3>
 * <ul>
 *   <li><strong>Import Operations</strong>: File import with validation and conflict resolution</li>
 *   <li><strong>Export Operations</strong>: Single and batch export with formatting options</li>
 *   <li><strong>File Validation</strong>: Pre-import validation and schema checking</li>
 *   <li><strong>Progress Tracking</strong>: Real-time progress reporting for long operations</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error reporting and recovery</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <p>This service works alongside LocalEventsService to provide file operations
 * without coupling file handling to core business logic. It's designed to be
 * used by dedicated ViewModels for file operations.</p>
 *
 * <h3>Supported File Operations:</h3>
 * <ul>
 *   <li>JSON format import/export (.qdue files)</li>
 *   <li>Batch operations for multiple events</li>
 *   <li>Validation before import</li>
 *   <li>Conflict resolution strategies</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public interface LocalEventsFileService {

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Initialize the file service and its dependencies.
     *
     * @return CompletableFuture with OperationResult indicating initialization success
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> initialize();

    /**
     * Check if service is ready for file operations.
     *
     * @return true if service is initialized and ready
     */
    boolean isReady();

    /**
     * Shutdown service and cleanup resources.
     */
    void shutdown();

    // ==================== IMPORT OPERATIONS ====================

    /**
     * Import events from file URI with default options.
     *
     * @param fileUri URI of file to import
     * @return CompletableFuture with ImportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ImportResult>> importEventsFromFile(@NonNull Uri fileUri);

    /**
     * Import events from file URI with custom options.
     *
     * @param fileUri URI of file to import
     * @param options Import configuration options
     * @return CompletableFuture with ImportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ImportResult>> importEventsFromFile(
            @NonNull Uri fileUri, @NonNull ImportOptions options);

    /**
     * Import events from file URI with progress callback.
     *
     * @param fileUri URI of file to import
     * @param options Import configuration options
     * @param progressCallback Callback for progress updates
     * @return CompletableFuture with ImportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ImportResult>> importEventsFromFileWithProgress(
            @NonNull Uri fileUri, @NonNull ImportOptions options, @Nullable ProgressCallback progressCallback);

    /**
     * Validate file before import without importing.
     *
     * @param fileUri URI of file to validate
     * @return CompletableFuture with ValidationResult containing validation details
     */
    @NonNull
    CompletableFuture<OperationResult<ValidationResult>> validateFileForImport(@NonNull Uri fileUri);

    // ==================== EXPORT OPERATIONS ====================

    /**
     * Export all events to file URI.
     *
     * @param fileUri Target file URI for export
     * @return CompletableFuture with ExportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ExportResult>> exportAllEventsToFile(@NonNull Uri fileUri);

    /**
     * Export selected events to file URI.
     *
     * @param fileUri Target file URI for export
     * @param events List of events to export
     * @return CompletableFuture with ExportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ExportResult>> exportEventsToFile(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events);

    /**
     * Export events to file URI with custom options.
     *
     * @param fileUri Target file URI for export
     * @param events List of events to export
     * @param options Export configuration options
     * @return CompletableFuture with ExportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ExportResult>> exportEventsToFile(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events, @NonNull ExportOptions options);

    /**
     * Export events to file URI with progress callback.
     *
     * @param fileUri Target file URI for export
     * @param events List of events to export
     * @param options Export configuration options
     * @param progressCallback Callback for progress updates
     * @return CompletableFuture with ExportResult containing operation details
     */
    @NonNull
    CompletableFuture<OperationResult<ExportResult>> exportEventsToFileWithProgress(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events, @NonNull ExportOptions options,
            @Nullable ProgressCallback progressCallback);

    // ==================== FILE INFORMATION ====================

    /**
     * Get file information and metadata.
     *
     * @param fileUri URI of file to inspect
     * @return CompletableFuture with FileInfo containing file details
     */
    @NonNull
    CompletableFuture<OperationResult<FileInfo>> getFileInfo(@NonNull Uri fileUri);

    /**
     * Check if file is supported for import.
     *
     * @param fileUri URI of file to check
     * @return CompletableFuture with boolean indicating support
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> isFileSupported(@NonNull Uri fileUri);

    /**
     * Get supported file types description.
     *
     * @return String describing supported file types
     */
    @NonNull
    String getSupportedFileTypesDescription();

    // ==================== SERVICE STATE ====================

    /**
     * Get file service status information.
     *
     * @return FileServiceStatus with current service state
     */
    @NonNull
    FileServiceStatus getServiceStatus();

    // ==================== INNER CLASSES AND INTERFACES ====================

    /**
     * Import configuration options.
     */
    class ImportOptions {
        public boolean validateBeforeImport = true;
        public boolean reportProgress = true;
        public boolean preserveExistingEvents = true;
        public boolean allowPartialImport = true;
        public ConflictResolution conflictResolution = ConflictResolution.SKIP_DUPLICATE;

        public enum ConflictResolution {
            SKIP_DUPLICATE,
            REPLACE_EXISTING,
            RENAME_DUPLICATE
        }
    }

    /**
     * Export configuration options.
     */
    class ExportOptions {
        public String packageName = "QDue Events Export";
        public String packageDescription = "LocalEvents export from QDue";
        public String authorName = "QDue App";
        public boolean includeCustomProperties = true;
        public boolean reportProgress = true;
        public boolean prettyFormat = true;
    }

    /**
     * Import operation result.
     */
    class ImportResult {
        public final boolean success;
        public final int totalEvents;
        public final int importedEvents;
        public final int skippedEvents;
        public final int failedEvents;
        public final List<String> warnings;
        public final List<String> errors;
        public final long operationTimeMs;

        public ImportResult(boolean success, int totalEvents, int importedEvents,
                            int skippedEvents, int failedEvents, List<String> warnings,
                            List<String> errors, long operationTimeMs) {
            this.success = success;
            this.totalEvents = totalEvents;
            this.importedEvents = importedEvents;
            this.skippedEvents = skippedEvents;
            this.failedEvents = failedEvents;
            this.warnings = warnings;
            this.errors = errors;
            this.operationTimeMs = operationTimeMs;
        }

        public boolean hasIssues() {
            return !warnings.isEmpty() || !errors.isEmpty();
        }

        public String getSummary() {
            return String.format("Imported %d/%d events (skipped: %d, failed: %d)",
                                 importedEvents, totalEvents, skippedEvents, failedEvents);
        }
    }

    /**
     * Export operation result.
     */
    class ExportResult {
        public final boolean success;
        public final int exportedEvents;
        public final String filePath;
        public final long fileSizeBytes;
        public final List<String> warnings;
        public final long operationTimeMs;

        public ExportResult(boolean success, int exportedEvents, String filePath,
                            long fileSizeBytes, List<String> warnings, long operationTimeMs) {
            this.success = success;
            this.exportedEvents = exportedEvents;
            this.filePath = filePath;
            this.fileSizeBytes = fileSizeBytes;
            this.warnings = warnings;
            this.operationTimeMs = operationTimeMs;
        }

        public String getSummary() {
            return String.format("Exported %d events to %s (%.1f KB)",
                                 exportedEvents, filePath, fileSizeBytes / 1024.0);
        }
    }

    /**
     * File validation result.
     */
    class ValidationResult {
        public final boolean isValid;
        public final int eventCount;
        public final List<String> warnings;
        public final List<String> errors;

        public ValidationResult(boolean isValid, int eventCount,
                                List<String> warnings, List<String> errors) {
            this.isValid = isValid;
            this.eventCount = eventCount;
            this.warnings = warnings;
            this.errors = errors;
        }

        public boolean hasIssues() {
            return !warnings.isEmpty() || !errors.isEmpty();
        }
    }

    /**
     * File information.
     */
    class FileInfo {
        public final String fileName;
        public final String mimeType;
        public final long sizeBytes;
        public final boolean isSupported;
        public final String displayInfo;

        public FileInfo(String fileName, String mimeType, long sizeBytes,
                        boolean isSupported, String displayInfo) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.isSupported = isSupported;
            this.displayInfo = displayInfo;
        }
    }

    /**
     * Progress callback for long-running operations.
     */
    interface ProgressCallback {
        /**
         * Called when operation progress is updated.
         *
         * @param processed Number of items processed
         * @param total Total number of items
         * @param currentItem Current item being processed
         */
        void onProgress(int processed, int total, String currentItem);
    }

    /**
     * File service status information.
     */
    class FileServiceStatus {
        private final boolean initialized;
        private final boolean ready;
        private final String version;
        private final long lastOperationTime;
        private final int activeOperations;

        public FileServiceStatus(boolean initialized, boolean ready, String version,
                                 long lastOperationTime, int activeOperations) {
            this.initialized = initialized;
            this.ready = ready;
            this.version = version;
            this.lastOperationTime = lastOperationTime;
            this.activeOperations = activeOperations;
        }

        public boolean isInitialized() { return initialized; }
        public boolean isReady() { return ready; }
        public String getVersion() { return version; }
        public long getLastOperationTime() { return lastOperationTime; }
        public int getActiveOperations() { return activeOperations; }

        @Override
        public String toString() {
            return String.format("FileServiceStatus{initialized=%s, ready=%s, version='%s', " +
                                         "lastOperationTime=%d, activeOperations=%d}",
                                 initialized, ready, version, lastOperationTime, activeOperations);
        }
    }
}