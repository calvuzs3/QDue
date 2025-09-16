package net.calvuz.qdue.data.services.impl;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.export.LocalEventsExportManager;
import net.calvuz.qdue.data.services.LocalEventsFileService;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.events.imports.EventsImportManager;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.ui.features.events.components.imports.FileAccessAdapter;
import net.calvuz.qdue.ui.features.events.components.imports.EventsImportAdapter;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;

/**
 * LocalEvents File Service Implementation (Updated)
 *
 * <p>Updated implementation of LocalEventsFileService that handles all file-based
 * operations including import, export, and validation. This service integrates with
 * existing QDue file handling infrastructure while providing LocalEvents-specific
 * functionality through the MVVM architecture.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Import Operations</strong>: JSON and .qdue file import with validation</li>
 *   <li><strong>Export Operations</strong>: Structured export with progress tracking</li>
 *   <li><strong>File Validation</strong>: Schema validation before import operations</li>
 *   <li><strong>Progress Tracking</strong>: Real-time progress callbacks for long operations</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error reporting and recovery</li>
 *   <li><strong>Thread Safety</strong>: All operations executed on background threads</li>
 * </ul>
 *
 * <h3>Updates:</h3>
 * <ul>
 *   <li><strong>JsonSchemaValidator 2.0</strong>: Compatible with updated ValidationResult API</li>
 *   <li><strong>Enhanced Validation</strong>: Direct JSON string validation support</li>
 *   <li><strong>Improved Error Handling</strong>: Better error messages and recovery</li>
 *   <li><strong>Performance Optimizations</strong>: Streamlined validation and import processes</li>
 * </ul>
 *
 * <h3>Integration:</h3>
 * <p>This service leverages existing QDue infrastructure:</p>
 * <ul>
 *   <li>{@link net.calvuz.qdue.ui.features.events.components.imports.FileAccessAdapter}</li>
 *   <li>{@link net.calvuz.qdue.ui.features.events.components.imports.EventsImportAdapter}</li>
 *   <li>{@link net.calvuz.qdue.core.backup.ExportManager}</li>
 *   <li>{@link net.calvuz.qdue.events.validation.JsonSchemaValidator}</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Updated for JsonSchemaValidator 2.0 compatibility
 * @see net.calvuz.qdue.data.services.LocalEventsFileService
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsFileServiceImpl implements LocalEventsFileService {

    private static final String TAG = "LocalEventsFileServiceImpl";
    private static final String SERVICE_VERSION = "2.0.0";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final LocalEventsService mLocalEventsService;
    private final ExecutorService mExecutorService;

    // ==================== FILE HANDLING COMPONENTS ====================

    private final FileAccessAdapter mFileAccessAdapter;
    private final EventsImportAdapter mEventsImportAdapter;
    private final LocalEventsExportManager mExportManager;

    // ==================== STATE MANAGEMENT ====================

    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mShutdown = new AtomicBoolean(false);
    private final AtomicLong mLastOperationTime = new AtomicLong(0);
    private final AtomicInteger mActiveOperations = new AtomicInteger(0);

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param context            Application context
     * @param localEventsService Service for LocalEvent operations
     */
    public LocalEventsFileServiceImpl(
            @NonNull Context context,
            @NonNull LocalEventsService localEventsService
    ) {
        this.mContext = context.getApplicationContext();
        this.mLocalEventsService = localEventsService;
        this.mExecutorService = Executors.newFixedThreadPool(2);

        // Initialize file handling components
        this.mFileAccessAdapter = new FileAccessAdapter(context);
        this.mEventsImportAdapter = new EventsImportAdapter(context);
        this.mExportManager = new LocalEventsExportManager(context);

        Log.d(TAG, "LocalEventsFileServiceImpl created with JsonSchemaValidator 2.0 support");
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Initializing LocalEventsFileService");

                if (mShutdown.get()) {
                    return OperationResult.failure(
                            "Cannot initialize shutdown service",
                            OperationResult.OperationType.INITIALIZATION
                    );
                }

                if (mInitialized.get()) {
                    Log.d(TAG, "File service already initialized");
                    return OperationResult.success(
                            null,
                            "File service already initialized",
                            OperationResult.OperationType.INITIALIZATION
                    );
                }

                // Verify dependencies are ready
                if (!mLocalEventsService.isReady()) {
                    return OperationResult.failure(
                            "LocalEventsService not ready",
                            OperationResult.OperationType.INITIALIZATION
                    );
                }

                updateLastOperationTime();
                mInitialized.set(true);

                Log.d(TAG, "LocalEventsFileService initialized successfully");
                return OperationResult.success(
                        null,
                        "File service initialized successfully",
                        OperationResult.OperationType.INITIALIZATION
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize LocalEventsFileService", e);
                return OperationResult.failure(
                        "Initialization failed: " + e.getMessage(),
                        OperationResult.OperationType.INITIALIZATION
                );
            }
        }, mExecutorService);
    }

    @Override
    public boolean isReady() {
        return mInitialized.get() && !mShutdown.get() && mLocalEventsService.isReady();
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down LocalEventsFileService");

        mShutdown.set(true);
        mInitialized.set(false);

        if (mFileAccessAdapter != null) {
            mFileAccessAdapter.clearPendingCallback();
        }

        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }

        Log.d(TAG, "LocalEventsFileService shutdown completed");
    }

    // ==================== IMPORT OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ImportResult>> importEventsFromFile(@NonNull Uri fileUri) {
        ImportOptions defaultOptions = new ImportOptions();
        return importEventsFromFile(fileUri, defaultOptions);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ImportResult>> importEventsFromFile(
            @NonNull Uri fileUri, @NonNull ImportOptions options) {
        return importEventsFromFileWithProgress(fileUri, options, null);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ImportResult>> importEventsFromFileWithProgress(
            @NonNull Uri fileUri, @NonNull ImportOptions options, @Nullable ProgressCallback progressCallback) {

        if (!ensureServiceReady()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Service not ready",
                                            OperationResult.OperationType.IMPORT)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            mActiveOperations.incrementAndGet();
            updateLastOperationTime();

            try {
                Log.d(TAG, "Starting import from file: " + fileUri);

                // Validate file type
                if (!mFileAccessAdapter.isSupportedFile(fileUri)) {
                    return OperationResult.failure(
                            "Unsupported file type. " + getSupportedFileTypesDescription(),
                            OperationResult.OperationType.IMPORT
                    );
                }

                // Progress callback wrapper
                EventsImportManager.ImportCallback importCallback = new EventsImportManager.ImportCallback() {
                    @Override
                    public void onValidationComplete(JsonSchemaValidator.ValidationResult validationResult) {
                        Log.d(TAG, "Validation completed: " + validationResult.isValid());
                    }

                    @Override
                    public void onProgress(int processed, int total, String currentEvent) {
                        if (progressCallback != null) {
                            progressCallback.onProgress(processed, total, currentEvent);
                        }
                    }

                    @Override
                    public void onComplete(EventsImportManager.ImportResult result) {
                        Log.d(TAG, "Import completed: " + result.success);
                    }

                    @Override
                    public void onError(String error, Exception exception) {
                        Log.e(TAG, "Import error: " + error, exception);
                    }
                };

                // Convert ImportOptions to EventsImportManager.ImportOptions
                EventsImportManager.ImportOptions importManagerOptions = convertImportOptions(options);

                // Perform import using EventsImportAdapter
                CompletableFuture<EventsImportManager.ImportResult> importFuture =
                        performImportOperation(fileUri, importManagerOptions, importCallback);

                EventsImportManager.ImportResult importResult = importFuture.join();

                // Convert result
                ImportResult result = convertImportResult(importResult, startTime);

                if (result.success) {
                    Log.d(TAG, "Import successful: " + result.getSummary());
                    return OperationResult.success(result, "Import completed successfully",
                                                   OperationResult.OperationType.IMPORT);
                } else {
                    Log.w(TAG, "Import failed: " + result.getSummary());
                    return OperationResult.failure(
                            "Import failed: " + String.join(", ", result.errors),
                            OperationResult.OperationType.IMPORT);
                }
            } catch (Exception e) {
                Log.e(TAG, "Import operation failed", e);
                ImportResult errorResult = new ImportResult(false, 0, 0, 0, 0,
                                                            new ArrayList<>(),
                                                            List.of(e.getMessage()),
                                                            System.currentTimeMillis() - startTime);
                return OperationResult.failure("Import failed: " + e.getMessage(),
                                               OperationResult.OperationType.IMPORT);
            } finally {
                mActiveOperations.decrementAndGet();
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ValidationResult>> validateFileForImport(@NonNull Uri fileUri) {
        if (!ensureServiceReady()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Service not ready",
                                            OperationResult.OperationType.VALIDATION)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            mActiveOperations.incrementAndGet();
            updateLastOperationTime();

            try {
                Log.d(TAG, "Validating file for import: " + fileUri);

                // Check file type first
                if (!mFileAccessAdapter.isSupportedFile(fileUri)) {
                    ValidationResult result = new ValidationResult(false, 0,
                                                                   new ArrayList<>(), List.of(
                            "Unsupported file type"));
                    return OperationResult.success(result, "File validation completed",
                                                   OperationResult.OperationType.VALIDATION);
                }

                // Perform validation using updated JsonSchemaValidator
                CompletableFuture<JsonSchemaValidator.ValidationResult> validationFuture =
                        performValidationOperation(fileUri);

                JsonSchemaValidator.ValidationResult schemaValidation = validationFuture.join();

                // Convert validation result using updated API
                ValidationResult result = convertValidationResult(schemaValidation);

                Log.d(TAG, "File validation completed: " + result.isValid);
                return OperationResult.success(result, "File validation completed",
                                               OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "File validation failed", e);
                ValidationResult errorResult = new ValidationResult(false, 0,
                                                                    new ArrayList<>(), List.of(
                        "Validation failed: " + e.getMessage()));
                return OperationResult.failure("Validation failed: " + e.getMessage(),
                                               OperationResult.OperationType.VALIDATION);
            } finally {
                mActiveOperations.decrementAndGet();
            }
        }, mExecutorService);
    }

    // ==================== EXPORT OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ExportResult>> exportAllEventsToFile(@NonNull Uri fileUri) {
        return mLocalEventsService.getAllEvents()
                .thenCompose(eventsResult -> {
                    if (eventsResult.isSuccess()) {
                        List<LocalEvent> events = eventsResult.getData();
                        ExportOptions defaultOptions = new ExportOptions();
                        return exportEventsToFile(fileUri, events, defaultOptions);
                    } else {
                        return CompletableFuture.completedFuture(
                                OperationResult.failure(
                                        "Failed to get events: " + eventsResult.getFirstError(),
                                        OperationResult.OperationType.EXPORT)
                        );
                    }
                });
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ExportResult>> exportEventsToFile(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events) {
        ExportOptions defaultOptions = new ExportOptions();
        return exportEventsToFile(fileUri, events, defaultOptions);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ExportResult>> exportEventsToFile(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events, @NonNull ExportOptions options) {
        return exportEventsToFileWithProgress(fileUri, events, options, null);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ExportResult>> exportEventsToFileWithProgress(
            @NonNull Uri fileUri, @NonNull List<LocalEvent> events, @NonNull ExportOptions options,
            @Nullable ProgressCallback progressCallback
    ) {

        if (!ensureServiceReady()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Service not ready",
                                            OperationResult.OperationType.EXPORT)
            );
        }

        if (events.isEmpty()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("No events to export",
                                            OperationResult.OperationType.EXPORT)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            mActiveOperations.incrementAndGet();
            updateLastOperationTime();

            try {
                Log.d(TAG,
                      "Starting export to file: " + fileUri + " (" + events.size() + " events)");

                // Convert ExportOptions to LocalEventsExportManager.ExportOptions
                LocalEventsExportManager.ExportOptions exportManagerOptions = convertExportOptions(options);

                // Progress callback wrapper
                LocalEventsExportManager.ExportCallback exportCallback = new LocalEventsExportManager.ExportCallback() {
                    @Override
                    public void onExportComplete(LocalEventsExportManager.ExportResult result) {
                        Log.d(TAG, "Export completed: " + result.success);
                    }

                    @Override
                    public void onExportProgress(int processed, int total, String currentEvent) {
                        if (progressCallback != null) {
                            progressCallback.onProgress(processed, total, currentEvent);
                        }
                    }

                    @Override
                    public void onExportError(String error, Exception exception) {
                        Log.e(TAG, "Export error: " + error, exception);
                    }
                };

                // Perform export using LocalEventsExportManager
                CompletableFuture<LocalEventsExportManager.ExportResult> exportFuture =
                        performExportOperation(events, fileUri, exportManagerOptions,
                                               exportCallback);

                LocalEventsExportManager.ExportResult exportManagerResult = exportFuture.join();

                // Convert result
                ExportResult result = convertExportResult(exportManagerResult, fileUri.toString(),
                                                          startTime);

                if (result.success) {
                    Log.d(TAG, "Export successful: " + result.getSummary());
                    return OperationResult.success(result, "Export completed successfully",
                                                   OperationResult.OperationType.EXPORT);
                } else {
                    Log.w(TAG, "Export failed");
                    return OperationResult.failure("Export failed",
                                                   OperationResult.OperationType.EXPORT);
                }
            } catch (Exception e) {
                Log.e(TAG, "Export operation failed", e);
                ExportResult errorResult = new ExportResult(false, 0, fileUri.toString(),
                                                            0, List.of(
                        "Export failed: " + e.getMessage()),
                                                            System.currentTimeMillis() - startTime);
                return OperationResult.failure("Export failed: " + e.getMessage(),
                                               OperationResult.OperationType.EXPORT);
            } finally {
                mActiveOperations.decrementAndGet();
            }
        }, mExecutorService);
    }

    // ==================== FILE INFORMATION ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<FileInfo>> getFileInfo(@NonNull Uri fileUri) {
        if (!ensureServiceReady()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Service not ready",
                                            OperationResult.OperationType.READ)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            updateLastOperationTime();

            try {
                Log.d(TAG, "Getting file info for: " + fileUri);

                String fileDisplayInfo = mFileAccessAdapter.getFileDisplayInfo(fileUri);
                boolean isSupported = mFileAccessAdapter.isSupportedFile(fileUri);

                // Extract basic file information
                String fileName = fileUri.getLastPathSegment();
                String mimeType = mContext.getContentResolver().getType(fileUri);

                // Get file size (simplified approach)
                long fileSize = 0;
                try {
                    fileSize = mFileAccessAdapter.getFileSize(fileUri);
                } catch (Exception e) {
                    Log.w(TAG, "Could not get file size: " + e.getMessage());
                }

                FileInfo fileInfo = new FileInfo(fileName, mimeType, fileSize, isSupported,
                                                 fileDisplayInfo);

                Log.d(TAG, "File info retrieved: " + fileInfo.displayInfo);
                return OperationResult.success(fileInfo, "File info retrieved successfully",
                                               OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Error getting file info", e);
                return OperationResult.failure("Error getting file info: " + e.getMessage(),
                                               OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> isFileSupported(@NonNull Uri fileUri) {
        return CompletableFuture.supplyAsync(() -> {
            updateLastOperationTime();

            try {
                boolean isSupported = mFileAccessAdapter.isSupportedFile(fileUri);
                return OperationResult.success(isSupported, "File support check completed",
                                               OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Error checking file support", e);
                return OperationResult.failure("Error checking file support: " + e.getMessage(),
                                               OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public String getSupportedFileTypesDescription() {
        return FileAccessAdapter.getSupportedFileTypesDescription();
    }

    // ==================== SERVICE STATE ====================

    @Override
    @NonNull
    public FileServiceStatus getServiceStatus() {
        return new FileServiceStatus(
                mInitialized.get(),
                isReady(),
                SERVICE_VERSION,
                mLastOperationTime.get(),
                mActiveOperations.get()
        );
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private boolean ensureServiceReady() {
        if (!isReady()) {
            Log.w(TAG, "File service operation attempted when not ready");
            return false;
        }
        return true;
    }

    private void updateLastOperationTime() {
        mLastOperationTime.set(System.currentTimeMillis());
    }

    // ==================== CONVERSION METHODS (UPDATED) ====================

    private EventsImportManager.ImportOptions convertImportOptions(ImportOptions options) {
        EventsImportManager.ImportOptions importOptions = new EventsImportManager.ImportOptions();
        importOptions.validateBeforeImport = options.validateBeforeImport;
        importOptions.reportProgress = options.reportProgress;
        importOptions.preserveExistingEvents = options.preserveExistingEvents;
        // importOptions.allowPartialImport = options.allowPartialImport;

        // Convert conflict resolution
        switch (options.conflictResolution) {
            case SKIP_DUPLICATE:
                importOptions.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.SKIP_DUPLICATE;
                break;
            case REPLACE_EXISTING:
                importOptions.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.REPLACE_EXISTING;
                break;
            case RENAME_DUPLICATE:
                importOptions.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.RENAME_DUPLICATE;
                break;
        }

        return importOptions;
    }

    private LocalEventsExportManager.ExportOptions convertExportOptions(ExportOptions options) {
        LocalEventsExportManager.ExportOptions exportOptions = new LocalEventsExportManager.ExportOptions();
        exportOptions.packageName = options.packageName;
        exportOptions.packageDescription = options.packageDescription;
        exportOptions.authorName = options.authorName;
        exportOptions.includeCustomProperties = options.includeCustomProperties;
        exportOptions.reportProgress = options.reportProgress;
        return exportOptions;
    }

    private ImportResult convertImportResult(EventsImportManager.ImportResult source, long startTime) {
        long operationTime = System.currentTimeMillis() - startTime;
        return new ImportResult(
                source.success,
                source.totalEvents,
                source.importedEvents,
                source.skippedEvents,
                source.errorEvents,
                source.warnings,
                source.errors,
                operationTime
        );
    }

    private ExportResult convertExportResult(LocalEventsExportManager.ExportResult source, String filePath, long startTime) {
        long operationTime = System.currentTimeMillis() - startTime;
        return new ExportResult(
                source.success,
                source.exportedEvents,
                filePath,
                source.fileSizeBytes,
                source.warnings,
                operationTime
        );
    }

    /**
     * UPDATED: Convert JsonSchemaValidator.ValidationResult to LocalEventsFileService.ValidationResult
     * using the new ValidationResult API with getter methods.
     */
    private ValidationResult convertValidationResult(JsonSchemaValidator.ValidationResult source) {
        return new ValidationResult(
                source.isValid(),           // Use getter method instead of field access
                source.getEventCount(),     // Use getter method instead of field access
                source.getWarnings(),       // Use getter method instead of field access
                source.getErrors()          // Use getter method instead of field access
        );
    }

    // ==================== OPERATION EXECUTION METHODS (UPDATED) ====================

    private CompletableFuture<EventsImportManager.ImportResult> performImportOperation(
            Uri fileUri, EventsImportManager.ImportOptions options, EventsImportManager.ImportCallback callback) {

        CompletableFuture<EventsImportManager.ImportResult> future = new CompletableFuture<>();

        mEventsImportAdapter.importFromSAFFile(fileUri, options,
                                               new EventsImportManager.ImportCallback() {
                                                   @Override
                                                   public void onValidationComplete(JsonSchemaValidator.ValidationResult validationResult) {
                                                       callback.onValidationComplete(validationResult);
                                                   }

                                                   @Override
                                                   public void onProgress(int processed, int total, String currentEvent) {
                                                       callback.onProgress(processed, total, currentEvent);
                                                   }

                                                   @Override
                                                   public void onComplete(EventsImportManager.ImportResult result) {
                                                       callback.onComplete(result);
                                                       future.complete(result);
                                                   }

                                                   @Override
                                                   public void onError(String error, Exception exception) {
                                                       callback.onError(error, exception);
                                                       future.completeExceptionally(
                                                               exception != null ? exception : new RuntimeException(error));
                                                   }
                                               });

        return future;
    }

    private CompletableFuture<LocalEventsExportManager.ExportResult> performExportOperation(
            List<LocalEvent> events, Uri fileUri, LocalEventsExportManager.ExportOptions options, LocalEventsExportManager.ExportCallback callback) {

        CompletableFuture<LocalEventsExportManager.ExportResult> future = new CompletableFuture<>();

        mExportManager.exportToUri(events, fileUri, options, new LocalEventsExportManager.ExportCallback() {
            @Override
            public void onExportComplete(LocalEventsExportManager.ExportResult result) {
                callback.onExportComplete(result);
                future.complete(result);
            }

            @Override
            public void onExportProgress(int processed, int total, String currentEvent) {
                callback.onExportProgress(processed, total, currentEvent);
            }

            @Override
            public void onExportError(String error, Exception exception) {
                callback.onExportError(error, exception);
                future.completeExceptionally(
                        exception != null ? exception : new RuntimeException(error));
            }
        });

        return future;
    }

    /**
     * UPDATED: Perform validation operation using the new JsonSchemaValidator API.
     * Now reads file content and uses JsonSchemaValidator.validateEventPackage(String).
     */
    private CompletableFuture<JsonSchemaValidator.ValidationResult> performValidationOperation(Uri fileUri) {
        CompletableFuture<JsonSchemaValidator.ValidationResult> future = new CompletableFuture<>();

        try {
            // Read file content
            String fileContent = readFileContent(fileUri);

            if (fileContent == null || fileContent.trim().isEmpty()) {
                JsonSchemaValidator.ValidationResult errorResult =
                        JsonSchemaValidator.ValidationResult.parseError("File is empty or unreadable");
                future.complete(errorResult);
                return future;
            }

            // Use the new JsonSchemaValidator API with string input
            JsonSchemaValidator.ValidationResult result =
                    JsonSchemaValidator.validateEventPackage(fileContent);

            future.complete(result);

        } catch (Exception e) {
            Log.e(TAG, "Validation operation failed", e);
            JsonSchemaValidator.ValidationResult errorResult =
                    JsonSchemaValidator.ValidationResult.parseError("Validation failed: " + e.getMessage());
            future.complete(errorResult);
        }

        return future;
    }

    /**
     * Read file content from URI.
     * Helper method for validation operation.
     */
    private String readFileContent(@NonNull Uri fileUri) throws IOException {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = mContext.getContentResolver().openInputStream(fileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    // ==================== DEBUG METHODS ====================

    public String getDebugInfo() {
        return String.format(
                "LocalEventsFileServiceImpl{initialized=%s, ready=%s, shutdown=%s, " +
                        "lastOperation=%d, activeOps=%d, version='%s'}",
                mInitialized.get(), isReady(), mShutdown.get(),
                mLastOperationTime.get(), mActiveOperations.get(), SERVICE_VERSION
        );
    }
}