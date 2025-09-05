package net.calvuz.qdue.core.services.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 1: Standardized Operation Result for Service Layer
 * <p>
 * Provides consistent return type for all service operations with:
 * - Success/failure status
 * - Data payload (nullable)
 * - Error messages and details
 * - Operation metadata
 * <p>
 * Used across all service implementations to ensure uniform error handling
 * and result processing throughout the application.
 */
public class OperationResult<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final List<String> errors;
    private final OperationType operationType;
    private final long timestamp;

    // ==================== CONSTRUCTORS ====================

    private OperationResult(boolean success, @Nullable T data, @NonNull String message,
                            @NonNull List<String> errors, @NonNull OperationType operationType) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errors = new ArrayList<>(errors);
        this.operationType = operationType;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== SUCCESS FACTORY METHODS ====================

    /**
     * Create successful result with data
     */
    public static <T> OperationResult<T> success(@NonNull T data, @NonNull String message,
                                                 @NonNull OperationType operationType) {
        return new OperationResult<>(true, data, message, new ArrayList<>(), operationType);
    }

    /**
     * Create successful result without data
     */
    public static <T> OperationResult<T> success(@NonNull String message,
                                                 @NonNull OperationType operationType) {
        return new OperationResult<>(true, null, message, new ArrayList<>(), operationType);
    }

    /**
     * Create successful result with default message
     */
    public static <T> OperationResult<T> success(@NonNull T data, @NonNull OperationType operationType) {
        return success(data, "Operation completed successfully", operationType);
    }

    // ==================== FAILURE FACTORY METHODS ====================

    /**
     * Create failed result with single error
     */
    public static <T> OperationResult<T> failure(@NonNull String error,
                                                 @NonNull OperationType operationType) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new OperationResult<>(false, null, "Operation failed", errors, operationType);
    }

    /**
     * Create failed result with multiple errors
     */
    public static <T> OperationResult<T> failure(@NonNull List<String> errors,
                                                 @NonNull OperationType operationType) {
        return new OperationResult<>(false, null, "Operation failed", errors, operationType);
    }

    /**
     * Create failed result with exception
     */
    public static <T> OperationResult<T> failure(@NonNull Exception exception,
                                                 @NonNull OperationType operationType) {
        List<String> errors = new ArrayList<>();
        errors.add(exception.getMessage() != null ? exception.getMessage() :
                exception.getClass().getSimpleName());
        return new OperationResult<>(false, null, "Operation failed due to exception",
                errors, operationType);
    }

    /**
     * Create failed result with custom message and error
     */
    public static <T> OperationResult<T> failure(@NonNull String message, @NonNull String error,
                                                 @NonNull OperationType operationType) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new OperationResult<>(false, null, message, errors, operationType);
    }

    // ====================  CHAINING UTILITY ====================

    /**
     * Chain result with another result
*/
    public OperationResult<T> or(@NonNull OperationResult<T> other) {
        if (this.success) {
            return this;
        } else {
            return other;
        }
    }

    // ==================== VALIDATION FACTORY METHODS ====================

    /**
     * Create validation failure result
     */
    public static <T> OperationResult<T> validationFailure(@NonNull List<String> validationErrors) {
        return new OperationResult<>(false, null, "Validation failed",
                validationErrors, OperationType.VALIDATION);
    }

    /**
     * Create validation success result
     */
    public static <T> OperationResult<T> validationSuccess() {
        return new OperationResult<>(true, null, "Validation passed",
                new ArrayList<>(), OperationType.VALIDATION);
    }

    // ==================== GETTERS ====================

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    @NonNull
    public OperationType getOperationType() {
        return operationType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if result has data
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Check if result has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Get first error message
     */
    @Nullable
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * Get formatted error message for UI display
     */
    @NonNull
    public String getFormattedErrorMessage() {
        if (errors.isEmpty()) {
            return message;
        }
        if (errors.size() == 1) {
            return errors.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(message).append(":\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append("• ").append(errors.get(i));
            if (i < errors.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get simple error message (alias for getFormattedErrorMessage for API consistency)
     */
    @NonNull
    public String getErrorMessage() {
        return getFormattedErrorMessage();
    }

    // ==================== OPERATION TYPE ENUM ====================

    /**
     * Types of operations for categorization and logging
     */
    public enum OperationType {
        // CRUD operations
        CREATE("Create"),
        READ("Read"),
        UPDATE("Update"),
        DELETE("Delete"),

        // Bulk operations
        IMPORT("Import"),
        EXPORT("Export"),
        BULK_DELETE("Bulk Delete"),
        BULK_CREATE("Bulk Create"),

        // Special operations
        DEACTIVATE("Deactivate"),
        DUPLICATE("Duplicate"),
        VALIDATION("Validation"),
        SEARCH("Search"),
        COUNT( "Count"),
        REFRESH("Refresh"),

        // Backup operations
        BACKUP("Backup"),
        RESTORE("Restore"),

        // System operations
        INITIALIZATION("Initialization"),
        CLEANUP("Cleanup"),
        SYSTEM( "System");

        private final String displayName;

        OperationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return "OperationResult{" +
                "success=" + success +
                ", data=" + data +
                ", message='" + message + '\'' +
                ", errors=" + errors +
                ", operationType=" + operationType +
                ", timestamp=" + timestamp +
                '}';
    }
}