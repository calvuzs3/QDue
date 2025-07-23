// SmartShiftsErrorHandler.java - Sistema Error Handling Unificato (usando stringhe esistenti)
package net.calvuz.qdue.smartshifts.domain.common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult.OperationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Sistema Error Handling Unificato per SmartShifts
 * <p>
 * Estende UnifiedOperationResult esistente per fornire:
 * - Tracking errori a livello applicazione
 * - Messaggi localizzati da smartshifts_strings.xml
 * - Categorizzazione e severità errori
 * - Suggerimenti recovery automatici
 * - Integrazione seamless con ExportImportManager
 * <p>
 * Utilizza le stringhe esistenti per coerenza con il sistema SmartShifts.
 */
@Singleton
public class SmartShiftsErrorHandler {

    private static final String TAG = "SmartShiftsErrorHandler";

    // Dependencies
    private final Context context;

    // Error tracking
    private final Map<String, ErrorLog> errorHistory = new ConcurrentHashMap<>();
    private final MutableLiveData<ErrorState> currentErrorState = new MutableLiveData<>();
    private final MutableLiveData<List<ErrorSummary>> recentErrors = new MutableLiveData<>();

    // Configuration
    private final int MAX_ERROR_HISTORY = 50;

    @Inject
    public SmartShiftsErrorHandler(@ApplicationContext Context context) {
        this.context = context;
        initializeErrorState();
    }

    // ============================================
    // PUBLIC API - ERROR HANDLING
    // ============================================

    /**
     * Handle error from UnifiedOperationResult using existing strings
     */
    public <T> SmartShiftsError handleOperationError(@NonNull UnifiedOperationResult<T> result) {
        if (result.isSuccess()) {
            return null;
        }

        SmartShiftsError error = SmartShiftsError.fromOperationResult(result, context);
        logError(error);
        updateErrorState(error);

        return error;
    }

    /**
     * Handle raw exception with localized messages
     */
    public SmartShiftsError handleException(
            @NonNull Exception exception,
            @NonNull OperationType operationType,
            @Nullable String userContext) {

        SmartShiftsError error = SmartShiftsError.fromException(
                exception, operationType, userContext, context);

        logError(error);
        updateErrorState(error);

        return error;
    }

    /**
     * Handle custom error with severity
     */
    public SmartShiftsError handleCustomError(
            @NonNull String errorMessage,
            @NonNull ErrorSeverity severity,
            @NonNull OperationType operationType,
            @Nullable String userContext) {

        SmartShiftsError error = SmartShiftsError.custom(
                errorMessage, severity, operationType, userContext, context);

        logError(error);
        updateErrorState(error);

        return error;
    }

    /**
     * Clear current error state
     */
    public void clearCurrentError() {
        currentErrorState.postValue(ErrorState.noError());
    }

    /**
     * Get current error state for UI observation
     */
    public LiveData<ErrorState> getCurrentErrorState() {
        return currentErrorState;
    }

    /**
     * Get recent errors for debugging/settings
     */
    public LiveData<List<ErrorSummary>> getRecentErrors() {
        return recentErrors;
    }

    // ============================================
    // ERROR SEVERITY & CATEGORY (usando stringhe esistenti)
    // ============================================

    public enum ErrorSeverity {
        INFO(1, R.string.smartshifts_severity_info),
        WARNING(2, R.string.smartshifts_severity_warning),
        ERROR(3, R.string.smartshifts_severity_error),
        CRITICAL(4, R.string.smartshifts_severity_critical);

        private final int level;
        private final int displayNameResId;

        ErrorSeverity(int level, int displayNameResId) {
            this.level = level;
            this.displayNameResId = displayNameResId;
        }

        public int getLevel() { return level; }
        public String getDisplayName(Context context) {
            return context.getString(displayNameResId);
        }

        public boolean isMoreSevereThan(ErrorSeverity other) {
            return this.level > other.level;
        }
    }

    public enum ErrorCategory {
        DATABASE("Database", R.drawable.ic_rounded_database_24),
        NETWORK("Rete", R.drawable.ic_rounded_network_check_24),
        FILE_SYSTEM("File System", R.drawable.ic_rounded_folder_24),
        VALIDATION("Validazione", R.drawable.ic_rounded_check_circle_24),
        PERMISSION("Permessi", R.drawable.ic_rounded_security_24),
        BUSINESS_LOGIC("Logica Business", R.drawable.ic_rounded_settings_24),
        USER_INPUT("Input Utente", R.drawable.ic_rounded_edit_24),
        EXPORT_IMPORT("Export/Import", R.drawable.ic_rounded_import_export),
        SETTINGS("Impostazioni", R.drawable.ic_rounded_settings_24),
        PATTERN_VALIDATION("Validazione Pattern", R.drawable.ic_rounded_pattern_24),
        SHIFT_GENERATION("Generazione Turni", R.drawable.ic_rounded_calendar_today_24),
        CONTACT_MANAGEMENT("Gestione Contatti", R.drawable.ic_rounded_contacts_24),
        UNKNOWN("Sconosciuto", R.drawable.ic_rounded_error_24);

        private final String displayName;
        private final int iconResId;

        ErrorCategory(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

    // ============================================
    // SMARTSHIFTS ERROR CLASS
    // ============================================

    public static class SmartShiftsError {
        private final String id;
        private final String message;
        private final String userFriendlyMessage;
        private final ErrorSeverity severity;
        private final ErrorCategory category;
        private final OperationType operationType;
        private final String userContext;
        private final long timestamp;
        private final Exception originalException;
        private final List<String> recoverySuggestions;
        private final Map<String, String> metadata;

        private SmartShiftsError(Builder builder) {
            this.id = builder.id;
            this.message = builder.message;
            this.userFriendlyMessage = builder.userFriendlyMessage;
            this.severity = builder.severity;
            this.category = builder.category;
            this.operationType = builder.operationType;
            this.userContext = builder.userContext;
            this.timestamp = System.currentTimeMillis();
            this.originalException = builder.originalException;
            this.recoverySuggestions = new ArrayList<>(builder.recoverySuggestions);
            this.metadata = new HashMap<>(builder.metadata);
        }

        // Factory methods usando stringhe esistenti da smartshifts_strings.xml
        public static SmartShiftsError fromOperationResult(
                UnifiedOperationResult<?> result, Context context) {

            ErrorCategory category = categorizeFromOperationType(result.getOperationType());
            ErrorSeverity severity = determineSeverity(result, category);

            return new Builder()
                    .setId(generateErrorId())
                    .setMessage(result.getFormattedErrorMessage())
                    .setUserFriendlyMessage(createUserFriendlyMessage(result, context))
                    .setSeverity(severity)
                    .setCategory(category)
                    .setOperationType(result.getOperationType())
                    .addRecoverySuggestions(generateRecoverySuggestions(result, context))
                    .build();
        }

        public static SmartShiftsError fromException(
                Exception exception, OperationType operationType,
                String userContext, Context context) {

            ErrorCategory category = categorizeFromException(exception);
            ErrorSeverity severity = determineSeverityFromException(exception);

            return new Builder()
                    .setId(generateErrorId())
                    .setMessage(exception.getMessage())
                    .setUserFriendlyMessage(createUserFriendlyMessage(exception, context))
                    .setSeverity(severity)
                    .setCategory(category)
                    .setOperationType(operationType)
                    .setUserContext(userContext)
                    .setOriginalException(exception)
                    .addRecoverySuggestions(generateRecoverySuggestions(exception, context))
                    .build();
        }

        public static SmartShiftsError custom(
                String message, ErrorSeverity severity, OperationType operationType,
                String userContext, Context context) {

            return new Builder()
                    .setId(generateErrorId())
                    .setMessage(message)
                    .setUserFriendlyMessage(message)
                    .setSeverity(severity)
                    .setCategory(ErrorCategory.UNKNOWN)
                    .setOperationType(operationType)
                    .setUserContext(userContext)
                    .build();
        }

        // ============================================
        // HELPER METHODS (usando stringhe esistenti)
        // ============================================

        private static ErrorCategory categorizeFromOperationType(OperationType operationType) {
            switch (operationType) {
                case EXPORT_JSON:
                case EXPORT_CSV:
                case EXPORT_COMPLETE:
                case EXPORT_SELECTIVE:
                case IMPORT_FILE:
                case IMPORT_CLOUD:
                case BACKUP_NOW:
                case RESTORE_BACKUP:
                    return ErrorCategory.EXPORT_IMPORT;
                case DATABASE_QUERY:
                case DATABASE_UPDATE:
                    return ErrorCategory.DATABASE;
                case PREFERENCES_UPDATE:
                case RESET_SETTINGS:
                case CLEAR_CACHE:
                    return ErrorCategory.SETTINGS;
                case NETWORK_SYNC:
                case CLOUD_UPLOAD:
                case CLOUD_DOWNLOAD:
                    return ErrorCategory.NETWORK;
                case PATTERN_VALIDATION:
                case PATTERN_CREATE:
                    return ErrorCategory.PATTERN_VALIDATION;
                case SHIFT_GENERATION:
                    return ErrorCategory.SHIFT_GENERATION;
                case CONTACT_MANAGEMENT:
                    return ErrorCategory.CONTACT_MANAGEMENT;
                default:
                    return ErrorCategory.UNKNOWN;
            }
        }

        private static ErrorCategory categorizeFromException(Exception exception) {
            String className = exception.getClass().getSimpleName();

            if (className.contains("SQL") || className.contains("Database")) {
                return ErrorCategory.DATABASE;
            } else if (className.contains("Network") || className.contains("Http") ||
                    className.contains("Connect") || className.contains("Socket")) {
                return ErrorCategory.NETWORK;
            } else if (className.contains("File") || className.contains("IO") ||
                    className.contains("Storage")) {
                return ErrorCategory.FILE_SYSTEM;
            } else if (className.contains("Security") || className.contains("Permission")) {
                return ErrorCategory.PERMISSION;
            } else if (className.contains("Validation") || className.contains("Argument")) {
                return ErrorCategory.VALIDATION;
            } else {
                return ErrorCategory.UNKNOWN;
            }
        }

        private static ErrorSeverity determineSeverity(UnifiedOperationResult<?> result, ErrorCategory category) {
            // Database e Permission errors sono critici
            if (category == ErrorCategory.DATABASE || category == ErrorCategory.PERMISSION) {
                return ErrorSeverity.CRITICAL;
            }
            // Network errors sono warning (retryable)
            if (category == ErrorCategory.NETWORK) {
                return ErrorSeverity.WARNING;
            }
            // Validation errors sono error standard
            if (category == ErrorCategory.VALIDATION || category == ErrorCategory.USER_INPUT) {
                return ErrorSeverity.ERROR;
            }
            // Default to ERROR
            return ErrorSeverity.ERROR;
        }

        private static ErrorSeverity determineSeverityFromException(Exception exception) {
            if (exception instanceof SecurityException) {
                return ErrorSeverity.CRITICAL;
            } else if (exception instanceof java.io.IOException ||
                    exception instanceof java.net.SocketException) {
                return ErrorSeverity.WARNING;
            } else if (exception instanceof IllegalArgumentException ||
                    exception instanceof IllegalStateException) {
                return ErrorSeverity.ERROR;
            } else {
                return ErrorSeverity.ERROR;
            }
        }

        // Message creation usando stringhe esistenti da smartshifts_strings.xml
        private static String createUserFriendlyMessage(UnifiedOperationResult<?> result, Context context) {
            OperationType type = result.getOperationType();

            switch (type) {
                case EXPORT_JSON:
                case EXPORT_CSV:
                case EXPORT_COMPLETE:
                    return context.getString(R.string.smartshifts_export_error_message);
                case IMPORT_FILE:
                case IMPORT_CLOUD:
                    return context.getString(R.string.smartshifts_import_error_message);
                case BACKUP_NOW:
                    return context.getString(R.string.smartshifts_backup_error_message);
                case RESTORE_BACKUP:
                    return context.getString(R.string.smartshifts_restore_error_message);
                case DATABASE_QUERY:
                case DATABASE_UPDATE:
                    return context.getString(R.string.error_database_operation);
                case SHIFT_GENERATION:
                    return context.getString(R.string.error_shift_generation);
                case PATTERN_VALIDATION:
                    return context.getString(R.string.error_pattern_not_found);
                case NETWORK_SYNC:
                    return context.getString(R.string.error_network_connection);
                case PREFERENCES_UPDATE:
                    return context.getString(R.string.error_saving_data);
                default:
                    return context.getString(R.string.error_unknown);
            }
        }

        private static String createUserFriendlyMessage(Exception exception, Context context) {
            if (exception instanceof java.io.FileNotFoundException) {
                return context.getString(R.string.error_loading_data);
            } else if (exception instanceof SecurityException) {
                return context.getString(R.string.error_saving_data);
            } else if (exception instanceof java.net.UnknownHostException ||
                    exception instanceof java.net.SocketException) {
                return context.getString(R.string.error_network_connection);
            } else if (exception instanceof IllegalArgumentException) {
                return context.getString(R.string.error_invalid_date);
            } else {
                return context.getString(R.string.error_unknown);
            }
        }

        // Recovery suggestions usando azioni esistenti da smartshifts_strings.xml
        private static List<String> generateRecoverySuggestions(UnifiedOperationResult<?> result, Context context) {
            List<String> suggestions = new ArrayList<>();
            OperationType type = result.getOperationType();

            switch (type) {
                case EXPORT_JSON:
                case EXPORT_CSV:
                case EXPORT_COMPLETE:
                    suggestions.add(context.getString(R.string.action_retry));
                    suggestions.add("Verifica spazio disponibile");
                    break;
                case IMPORT_FILE:
                    suggestions.add("Verifica formato file");
                    suggestions.add(context.getString(R.string.action_retry));
                    break;
                case DATABASE_QUERY:
                case DATABASE_UPDATE:
                    suggestions.add("Riavvia l'applicazione");
                    suggestions.add("Contatta supporto se persiste");
                    break;
                case NETWORK_SYNC:
                    suggestions.add("Verifica connessione internet");
                    suggestions.add("Riprova più tardi");
                    break;
                default:
                    suggestions.add(context.getString(R.string.action_retry));
            }

            return suggestions;
        }

        private static List<String> generateRecoverySuggestions(Exception exception, Context context) {
            List<String> suggestions = new ArrayList<>();

            if (exception instanceof java.io.FileNotFoundException) {
                suggestions.add("Verifica che il file esista");
                suggestions.add("Controlla percorso file");
            } else if (exception instanceof SecurityException) {
                suggestions.add("Verifica permessi applicazione");
                suggestions.add("Controlla impostazioni privacy");
            } else if (exception instanceof java.net.UnknownHostException) {
                suggestions.add("Verifica connessione internet");
                suggestions.add("Riprova più tardi");
            } else {
                suggestions.add(context.getString(R.string.action_retry));
                suggestions.add("Contatta supporto se necessario");
            }

            return suggestions;
        }

        private static String generateErrorId() {
            return "SS_ERR_" + System.currentTimeMillis() + "_" +
                    String.valueOf(System.nanoTime()).substring(8);
        }

        // Getters
        public String getId() { return id; }
        public String getMessage() { return message; }
        public String getUserFriendlyMessage() { return userFriendlyMessage; }
        public ErrorSeverity getSeverity() { return severity; }
        public ErrorCategory getCategory() { return category; }
        public OperationType getOperationType() { return operationType; }
        public String getUserContext() { return userContext; }
        public long getTimestamp() { return timestamp; }
        public Exception getOriginalException() { return originalException; }
        public List<String> getRecoverySuggestions() { return new ArrayList<>(recoverySuggestions); }
        public Map<String, String> getMetadata() { return new HashMap<>(metadata); }

        // Utility methods
        public boolean isCritical() { return severity == ErrorSeverity.CRITICAL; }
        public boolean isRetryable() {
            return category == ErrorCategory.NETWORK ||
                    category == ErrorCategory.FILE_SYSTEM ||
                    category == ErrorCategory.EXPORT_IMPORT;
        }

        // Builder pattern
        public static class Builder {
            private String id;
            private String message;
            private String userFriendlyMessage;
            private ErrorSeverity severity = ErrorSeverity.ERROR;
            private ErrorCategory category = ErrorCategory.UNKNOWN;
            private OperationType operationType = OperationType.UNKNOWN;
            private String userContext;
            private Exception originalException;
            private List<String> recoverySuggestions = new ArrayList<>();
            private Map<String, String> metadata = new HashMap<>();

            public Builder setId(String id) { this.id = id; return this; }
            public Builder setMessage(String message) { this.message = message; return this; }
            public Builder setUserFriendlyMessage(String message) { this.userFriendlyMessage = message; return this; }
            public Builder setSeverity(ErrorSeverity severity) { this.severity = severity; return this; }
            public Builder setCategory(ErrorCategory category) { this.category = category; return this; }
            public Builder setOperationType(OperationType type) { this.operationType = type; return this; }
            public Builder setUserContext(String context) { this.userContext = context; return this; }
            public Builder setOriginalException(Exception ex) { this.originalException = ex; return this; }
            public Builder addRecoverySuggestion(String suggestion) {
                this.recoverySuggestions.add(suggestion); return this;
            }
            public Builder addRecoverySuggestions(List<String> suggestions) {
                this.recoverySuggestions.addAll(suggestions); return this;
            }
            public Builder addMetadata(String key, String value) {
                this.metadata.put(key, value); return this;
            }

            public SmartShiftsError build() { return new SmartShiftsError(this); }
        }
    }

    // ============================================
    // ERROR STATE MANAGEMENT
    // ============================================

    public static class ErrorState {
        private final boolean hasError;
        private final SmartShiftsError currentError;
        private final int totalErrorCount;
        private final long lastErrorTime;

        private ErrorState(boolean hasError, SmartShiftsError currentError,
                           int totalErrorCount, long lastErrorTime) {
            this.hasError = hasError;
            this.currentError = currentError;
            this.totalErrorCount = totalErrorCount;
            this.lastErrorTime = lastErrorTime;
        }

        public static ErrorState noError() {
            return new ErrorState(false, null, 0, 0);
        }

        public static ErrorState withError(SmartShiftsError error, int totalCount) {
            return new ErrorState(true, error, totalCount, System.currentTimeMillis());
        }

        // Getters
        public boolean hasError() { return hasError; }
        public SmartShiftsError getCurrentError() { return currentError; }
        public int getTotalErrorCount() { return totalErrorCount; }
        public long getLastErrorTime() { return lastErrorTime; }
    }

    public static class ErrorSummary {
        private final String errorId;
        private final String message;
        private final ErrorSeverity severity;
        private final ErrorCategory category;
        private final long timestamp;
        private final int occurrenceCount;

        public ErrorSummary(String errorId, String message, ErrorSeverity severity,
                            ErrorCategory category, long timestamp, int occurrenceCount) {
            this.errorId = errorId;
            this.message = message;
            this.severity = severity;
            this.category = category;
            this.timestamp = timestamp;
            this.occurrenceCount = occurrenceCount;
        }

        // Getters
        public String getErrorId() { return errorId; }
        public String getMessage() { return message; }
        public ErrorSeverity getSeverity() { return severity; }
        public ErrorCategory getCategory() { return category; }
        public long getTimestamp() { return timestamp; }
        public int getOccurrenceCount() { return occurrenceCount; }
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private void initializeErrorState() {
        currentErrorState.setValue(ErrorState.noError());
        recentErrors.setValue(new ArrayList<>());
    }

    private void logError(SmartShiftsError error) {
        // Log to Android system using context strings
        Log.e(TAG, String.format(
                "[%s] %s - %s (Context: %s)",
                error.getSeverity().getDisplayName(context),
                error.getCategory().getDisplayName(),
                error.getMessage(),
                error.getUserContext()
        ), error.getOriginalException());

        // Store in error history
        ErrorLog errorLog = new ErrorLog(error);
        errorHistory.put(error.getId(), errorLog);

        // Cleanup old errors if needed
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            cleanupOldErrors();
        }

        // Update recent errors list
        updateRecentErrorsList();
    }

    private void updateErrorState(SmartShiftsError error) {
        ErrorState newState = ErrorState.withError(error, errorHistory.size());
        currentErrorState.postValue(newState);
    }

    private void updateRecentErrorsList() {
        List<ErrorSummary> summaries = new ArrayList<>();

        for (ErrorLog log : errorHistory.values()) {
            SmartShiftsError error = log.getError();
            ErrorSummary summary = new ErrorSummary(
                    error.getId(),
                    error.getUserFriendlyMessage(),
                    error.getSeverity(),
                    error.getCategory(),
                    error.getTimestamp(),
                    log.getOccurrenceCount()
            );
            summaries.add(summary);
        }

        // Sort by timestamp (most recent first)
        summaries.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        recentErrors.postValue(summaries);
    }

    private void cleanupOldErrors() {
        // Keep only the most recent errors
        List<Map.Entry<String, ErrorLog>> sortedEntries = new ArrayList<>(errorHistory.entrySet());
        sortedEntries.sort((a, b) -> Long.compare(
                b.getValue().getError().getTimestamp(),
                a.getValue().getError().getTimestamp()
        ));

        // Remove oldest entries
        for (int i = MAX_ERROR_HISTORY; i < sortedEntries.size(); i++) {
            errorHistory.remove(sortedEntries.get(i).getKey());
        }
    }

    // ============================================
    // ERROR LOG WRAPPER
    // ============================================

    private static class ErrorLog {
        private final SmartShiftsError error;
        private int occurrenceCount = 1;

        public ErrorLog(SmartShiftsError error) {
            this.error = error;
        }

        public SmartShiftsError getError() { return error; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public void incrementOccurrence() { occurrenceCount++; }
    }
}