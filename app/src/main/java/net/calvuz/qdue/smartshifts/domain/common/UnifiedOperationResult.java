package net.calvuz.qdue.smartshifts.domain.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified operation result system for SmartShifts operations.
 * <p>
 * Provides consistent result handling across:
 * - Settings operations (backup, restore, reset, etc.)
 * - Export/Import operations (all formats and types)
 * - Data operations (validation, sync, etc.)
 * <p>
 * This replaces fragmented result classes and provides a single
 * point of truth for operation outcomes.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class UnifiedOperationResult<T> {

    // Core result properties
    private final boolean success;
    private final OperationType operationType;
    private final T data;
    private final String message;
    private final List<String> errors;
    private final long timestamp;

    // Extended properties for complex operations
    private final OperationMetadata metadata;
    private final File resultFile;
    private final int recordCount;
    private final long fileSize;

    // ============================================
    // UNIFIED OPERATION TYPE ENUM
    // ============================================

    /**
     * Unified operation types covering all SmartShifts operations.
     * Replaces fragmented enums across different components.
     */
    public enum OperationType {
        // Settings operations
        VALIDATE_SETTINGS("validate_settings", "Validazione Impostazioni", OperationCategory.SETTINGS),
        RESET_SETTINGS("reset_settings", "Reset Impostazioni", OperationCategory.SETTINGS),
        CLEAR_CACHE("clear_cache", "Pulizia Cache", OperationCategory.SETTINGS),

        // Backup operations
        BACKUP_NOW("backup_now", "Backup Immediato", OperationCategory.BACKUP),
        RESTORE_BACKUP("restore_backup", "Ripristino Backup", OperationCategory.BACKUP),
        AUTO_BACKUP("auto_backup", "Backup Automatico", OperationCategory.BACKUP),

        // Export operations
        EXPORT_COMPLETE("export_complete", "Esportazione Completa", OperationCategory.EXPORT),
        EXPORT_SELECTIVE("export_selective", "Esportazione Selettiva", OperationCategory.EXPORT),
        EXPORT_CALENDAR("export_calendar", "Esportazione Calendario", OperationCategory.EXPORT),
        EXPORT_JSON("export_json", "Esportazione JSON", OperationCategory.EXPORT),
        EXPORT_CSV("export_csv", "Esportazione CSV", OperationCategory.EXPORT),
        EXPORT_XML("export_xml", "Esportazione XML", OperationCategory.EXPORT),
        EXPORT_ICAL("export_ical", "Esportazione iCal", OperationCategory.EXPORT),
        EXPORT_EXCEL("export_excel", "Esportazione Excel", OperationCategory.EXPORT),

        // Import operations
        IMPORT_FILE("import_file", "Importazione File", OperationCategory.IMPORT),
        IMPORT_CLOUD("import_cloud", "Importazione Cloud", OperationCategory.IMPORT),
        IMPORT_JSON("import_json", "Importazione JSON", OperationCategory.IMPORT),
        IMPORT_CSV("import_csv", "Importazione CSV", OperationCategory.IMPORT),
        IMPORT_XML("import_xml", "Importazione XML", OperationCategory.IMPORT),
        IMPORT_ICAL("import_ical", "Importazione iCal", OperationCategory.IMPORT),
        IMPORT_EXCEL("import_excel", "Importazione Excel", OperationCategory.IMPORT),

        // Cloud operations
        CLOUD_SYNC("cloud_sync", "Sincronizzazione Cloud", OperationCategory.CLOUD),
        CLOUD_UPLOAD("cloud_upload", "Upload Cloud", OperationCategory.CLOUD),
        CLOUD_DOWNLOAD("cloud_download", "Download Cloud", OperationCategory.CLOUD),

        // Data operations
        DATA_VALIDATION("data_validation", "Validazione Dati", OperationCategory.DATA),
        DATA_MIGRATION("data_migration", "Migrazione Dati", OperationCategory.DATA),
        DATA_CLEANUP("data_cleanup", "Pulizia Dati", OperationCategory.DATA),

        // Unknown/Error states
        UNKNOWN("unknown", "Operazione Sconosciuta", OperationCategory.UNKNOWN);

        private final String value;
        private final String displayName;
        private final OperationCategory category;

        OperationType(String value, String displayName, OperationCategory category) {
            this.value = value;
            this.displayName = displayName;
            this.category = category;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
        public OperationCategory getCategory() { return category; }

        /**
         * Get operation type from legacy string values
         * for backward compatibility with existing code
         */
        public static OperationType fromLegacyValue(String legacyValue) {
            if (legacyValue == null) return UNKNOWN;

            switch (legacyValue.toUpperCase()) {
                // Legacy DataOperationType mappings
                case "BACKUP_NOW": return BACKUP_NOW;
                case "RESTORE_BACKUP": return RESTORE_BACKUP;
                case "EXPORT_DATA": return EXPORT_COMPLETE;
                case "IMPORT_DATA": return IMPORT_FILE;
                case "RESET_SETTINGS": return RESET_SETTINGS;
                case "CLEAR_CACHE": return CLEAR_CACHE;

                // Legacy ExportImportManager mappings
                case "EXPORT": return EXPORT_COMPLETE;
                case "EXPORT_SELECTIVE": return EXPORT_SELECTIVE;
                case "EXPORT_CALENDAR": return EXPORT_CALENDAR;
                case "IMPORT": return IMPORT_FILE;
                case "IMPORT_CLOUD": return IMPORT_CLOUD;
                case "BACKUP": return BACKUP_NOW;
                case "RESTORE": return RESTORE_BACKUP;
                case "CLOUD_SYNC": return CLOUD_SYNC;

                default: return UNKNOWN;
            }
        }

        /**
         * Check if operation requires user confirmation before execution
         */
        public boolean requiresConfirmation() {
            switch (this) {
                case RESET_SETTINGS:
                case RESTORE_BACKUP:
                case DATA_CLEANUP:
                case CLEAR_CACHE:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Check if operation is potentially destructive
         */
        public boolean isDestructive() {
            switch (this) {
                case RESET_SETTINGS:
                case RESTORE_BACKUP:
                case DATA_CLEANUP:
                case CLEAR_CACHE:
                case DATA_MIGRATION:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Check if operation requires progress tracking
         */
        public boolean requiresProgressTracking() {
            return category == OperationCategory.EXPORT ||
                    category == OperationCategory.IMPORT ||
                    category == OperationCategory.BACKUP ||
                    category == OperationCategory.CLOUD ||
                    this == DATA_MIGRATION;
        }

        /**
         * Get recommended timeout for operation (in milliseconds)
         */
        public long getRecommendedTimeout() {
            switch (category) {
                case EXPORT:
                case IMPORT:
                case BACKUP:
                    return 300_000; // 5 minutes
                case CLOUD:
                    return 600_000; // 10 minutes
                case DATA:
                    return 180_000; // 3 minutes
                case SETTINGS:
                    return 30_000;  // 30 seconds
                default:
                    return 60_000;  // 1 minute
            }
        }
    }

    // ============================================
    // OPERATION CATEGORY ENUM
    // ============================================

    /**
     * High-level categories for operation types
     */
    public enum OperationCategory {
        SETTINGS("Impostazioni"),
        BACKUP("Backup"),
        EXPORT("Esportazione"),
        IMPORT("Importazione"),
        CLOUD("Cloud"),
        DATA("Dati"),
        UNKNOWN("Sconosciuto");

        private final String displayName;

        OperationCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    // ============================================
    // OPERATION METADATA CLASS
    // ============================================

    /**
     * Extended metadata for complex operations
     */
    public static class OperationMetadata {
        private final long duration;
        private final String format;
        private final String version;
        private final int conflictsResolved;
        private final int recordsProcessed;
        private final int recordsSkipped;
        private final String sourcePath;
        private final String destinationPath;

        private OperationMetadata(Builder builder) {
            this.duration = builder.duration;
            this.format = builder.format;
            this.version = builder.version;
            this.conflictsResolved = builder.conflictsResolved;
            this.recordsProcessed = builder.recordsProcessed;
            this.recordsSkipped = builder.recordsSkipped;
            this.sourcePath = builder.sourcePath;
            this.destinationPath = builder.destinationPath;
        }

        // Getters
        public long getDuration() { return duration; }
        public String getFormat() { return format; }
        public String getVersion() { return version; }
        public int getConflictsResolved() { return conflictsResolved; }
        public int getRecordsProcessed() { return recordsProcessed; }
        public int getRecordsSkipped() { return recordsSkipped; }
        public String getSourcePath() { return sourcePath; }
        public String getDestinationPath() { return destinationPath; }

        public static class Builder {
            private long duration = 0;
            private String format;
            private String version;
            private int conflictsResolved = 0;
            private int recordsProcessed = 0;
            private int recordsSkipped = 0;
            private String sourcePath;
            private String destinationPath;

            public Builder setDuration(long duration) {
                this.duration = duration;
                return this;
            }

            public Builder setFormat(String format) {
                this.format = format;
                return this;
            }

            public Builder setVersion(String version) {
                this.version = version;
                return this;
            }

            public Builder setConflictsResolved(int conflictsResolved) {
                this.conflictsResolved = conflictsResolved;
                return this;
            }

            public Builder setRecordsProcessed(int recordsProcessed) {
                this.recordsProcessed = recordsProcessed;
                return this;
            }

            public Builder setRecordsSkipped(int recordsSkipped) {
                this.recordsSkipped = recordsSkipped;
                return this;
            }

            public Builder setSourcePath(String sourcePath) {
                this.sourcePath = sourcePath;
                return this;
            }

            public Builder setDestinationPath(String destinationPath) {
                this.destinationPath = destinationPath;
                return this;
            }

            public OperationMetadata build() {
                return new OperationMetadata(this);
            }
        }
    }

    // ============================================
    // CONSTRUCTORS
    // ============================================

    /**
     * Private constructor - use builders for construction
     */
    private UnifiedOperationResult(
            boolean success,
            OperationType operationType,
            T data,
            String message,
            List<String> errors,
            OperationMetadata metadata,
            File resultFile,
            int recordCount,
            long fileSize
    ) {
        this.success = success;
        this.operationType = operationType;
        this.data = data;
        this.message = message;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.metadata = metadata;
        this.resultFile = resultFile;
        this.recordCount = recordCount;
        this.fileSize = fileSize;
        this.timestamp = System.currentTimeMillis();
    }

    // ============================================
    // BUILDER PATTERN
    // ============================================

    /**
     * Create successful result with data
     */
    public static <T> UnifiedOperationResult<T> success(
            @NonNull OperationType operationType,
            @Nullable T data,
            @Nullable String message
    ) {
        return new Builder<T>()
                .setSuccess(true)
                .setOperationType(operationType)
                .setData(data)
                .setMessage(message)
                .build();
    }

    /**
     * Create successful result with file output
     */
    public static <T> UnifiedOperationResult<T> successWithFile(
            @NonNull OperationType operationType,
            @NonNull File resultFile,
            @Nullable String message
    ) {
        return new Builder<T>()
                .setSuccess(true)
                .setOperationType(operationType)
                .setResultFile(resultFile)
                .setFileSize(resultFile.length())
                .setMessage(message)
                .build();
    }

    /**
     * Create failure result with error message
     */
    public static <T> UnifiedOperationResult<T> failure(
            @NonNull OperationType operationType,
            @NonNull String errorMessage
    ) {
        return new Builder<T>()
                .setSuccess(false)
                .setOperationType(operationType)
                .setMessage(errorMessage)
                .build();
    }

    /**
     * Create failure result with multiple errors
     */
    public static <T> UnifiedOperationResult<T> failure(
            @NonNull OperationType operationType,
            @NonNull List<String> errors
    ) {
        return new Builder<T>()
                .setSuccess(false)
                .setOperationType(operationType)
                .addErrors(errors)
                .build();
    }

    /**
     * Create failure result from exception
     */
    public static <T> UnifiedOperationResult<T> failure(
            @NonNull OperationType operationType,
            @NonNull Exception exception
    ) {
        return new Builder<T>()
                .setSuccess(false)
                .setOperationType(operationType)
                .setMessage("Errore durante " + operationType.getDisplayName() + ": " + exception.getMessage())
                .build();
    }

    // ============================================
    // BUILDER CLASS
    // ============================================

    public static class Builder<T> {
        private boolean success = false;
        private OperationType operationType = OperationType.UNKNOWN;
        private T data;
        private String message;
        private List<String> errors = new ArrayList<>();
        private OperationMetadata metadata;
        private File resultFile;
        private int recordCount = 0;
        private long fileSize = 0;

        public Builder<T> setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> setOperationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder<T> setData(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> addError(String error) {
            if (error != null) {
                this.errors.add(error);
            }
            return this;
        }

        public Builder<T> addErrors(List<String> errors) {
            if (errors != null) {
                this.errors.addAll(errors);
            }
            return this;
        }

        public Builder<T> setMetadata(OperationMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder<T> setResultFile(File resultFile) {
            this.resultFile = resultFile;
            return this;
        }

        public Builder<T> setRecordCount(int recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        public Builder<T> setFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public UnifiedOperationResult<T> build() {
            return new UnifiedOperationResult<>(
                    success, operationType, data, message, errors,
                    metadata, resultFile, recordCount, fileSize
            );
        }
    }

    // ============================================
    // GETTERS
    // ============================================

    public boolean isSuccess() { return success; }
    public boolean isFailure() { return !success; }
    public OperationType getOperationType() { return operationType; }
    @Nullable public T getData() { return data; }
    @Nullable public String getMessage() { return message; }
    @NonNull public List<String> getErrors() { return new ArrayList<>(errors); }
    public long getTimestamp() { return timestamp; }
    @Nullable public OperationMetadata getMetadata() { return metadata; }
    @Nullable public File getResultFile() { return resultFile; }
    public int getRecordCount() { return recordCount; }
    public long getFileSize() { return fileSize; }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get primary error message for display
     */
    @Nullable
    public String getPrimaryError() {
        if (errors.isEmpty()) {
            return message;
        }
        return errors.get(0);
    }

    /**
     * Get formatted error message for UI display
     */
    @NonNull
    public String getFormattedErrorMessage() {
        if (success) {
            return message != null ? message : "Operazione completata con successo";
        }

        if (errors.isEmpty()) {
            return message != null ? message : "Errore sconosciuto";
        }

        if (errors.size() == 1) {
            return errors.get(0);
        }

        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message).append(":\n");
        }
        for (int i = 0; i < errors.size(); i++) {
            sb.append("• ").append(errors.get(i));
            if (i < errors.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get success message for UI display
     */
    @NonNull
    public String getSuccessMessage() {
        if (!success) {
            return "Operazione fallita";
        }

        if (message != null) {
            return message;
        }

        // Generate default success message based on operation type
        return operationType.getDisplayName() + " completata con successo";
    }

    /**
     * Check if operation has file output
     */
    public boolean hasFileOutput() {
        return resultFile != null && resultFile.exists();
    }

    /**
     * Check if operation processed records
     */
    public boolean hasRecordCount() {
        return recordCount > 0;
    }

    /**
     * Get operation duration from metadata
     */
    public long getDuration() {
        return metadata != null ? metadata.getDuration() : 0;
    }

    /**
     * Create legacy-compatible status message for backward compatibility
     */
    @NonNull
    public String toLegacyStatusMessage() {
        switch (operationType) {
            case BACKUP_NOW:
                return success ? "BACKUP_SUCCESS" : "BACKUP_FAILED";
            case RESTORE_BACKUP:
                return success ? "RESTORE_SUCCESS" : "RESTORE_FAILED";
            case EXPORT_COMPLETE:
            case EXPORT_SELECTIVE:
                return success ? "EXPORT_SUCCESS" : "EXPORT_FAILED";
            case IMPORT_FILE:
            case IMPORT_CLOUD:
                return success ? "IMPORT_SUCCESS" : "IMPORT_FAILED";
            case RESET_SETTINGS:
                return success ? "RESET_COMPLETE" : "RESET_FAILED";
            case CLEAR_CACHE:
                return success ? "CACHE_CLEARED" : "CACHE_CLEAR_FAILED";
            default:
                return success ? "SUCCESS" : "FAILED";
        }
    }

    // ============================================
    // TRANSFORMATION METHODS
    // ============================================

    /**
     * Transform this result to a different data type
     */
    public <R> UnifiedOperationResult<R> map(java.util.function.Function<T, R> mapper) {
        if (!success || data == null) {
            return new UnifiedOperationResult<>(
                    success, operationType, null, message, errors,
                    metadata, resultFile, recordCount, fileSize
            );
        }

        try {
            R mappedData = mapper.apply(data);
            return new UnifiedOperationResult<>(
                    success, operationType, mappedData, message, errors,
                    metadata, resultFile, recordCount, fileSize
            );
        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    operationType,
                    "Errore durante la trasformazione: " + e.getMessage()
            );
        }
    }

    /**
     * Create a copy with different operation type (for operation chaining)
     */
    public UnifiedOperationResult<T> withOperationType(OperationType newOperationType) {
        return new UnifiedOperationResult<>(
                success, newOperationType, data, message, errors,
                metadata, resultFile, recordCount, fileSize
        );
    }

    // ============================================
    // OBJECT METHODS
    // ============================================

    @Override
    public String toString() {
        return "UnifiedOperationResult{" +
                "success=" + success +
                ", operationType=" + operationType +
                ", data=" + data +
                ", message='" + message + '\'' +
                ", errors=" + errors.size() +
                ", timestamp=" + timestamp +
                ", hasFile=" + hasFileOutput() +
                ", recordCount=" + recordCount +
                '}';
    }
}