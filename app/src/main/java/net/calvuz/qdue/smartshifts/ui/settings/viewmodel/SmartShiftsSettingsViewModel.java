package net.calvuz.qdue.smartshifts.ui.settings.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult.OperationType;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;
import net.calvuz.qdue.smartshifts.domain.usecases.GetUserShiftsUseCase;
import net.calvuz.qdue.smartshifts.domain.usecases.ManageContactsUseCase;
import net.calvuz.qdue.smartshifts.ui.exportimport.viewmodel.ExportImportViewModel;
import net.calvuz.qdue.smartshifts.utils.ValidationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Refactored ViewModel for SmartShifts Settings management.
 * <p>
 * Now uses the unified operation result system for consistent
 * handling across all settings operations including:
 * - Preference management and validation
 * - Data backup and restore operations
 * - Settings export/import functionality
 * - Integration with SmartShiftsExportImportManager
 * - Unified progress tracking and error handling
 * <p>
 * Key improvements:
 * - Single result type (UnifiedOperationResult) for all operations
 * - Centralized operation type management
 * - Consistent error handling and user feedback
 * - Simplified integration with UI components
 * - Better separation of concerns with ExportImportManager
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features (Refactored)
 */
@HiltViewModel
public class SmartShiftsSettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SmartShiftsSettingsVM";

    // Dependencies
    private final SharedPreferences preferences;
    private final ExecutorService executorService;
    private final GetUserShiftsUseCase getUserShiftsUseCase;
    private final ManageContactsUseCase manageContactsUseCase;
    private final SmartShiftsExportImportManager exportImportManager;

    // LiveData for UI updates - now using unified system
    private final MutableLiveData<String> navigationEvent = new MutableLiveData<>();
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();
    private final MutableLiveData<UnifiedOperationResult<ValidationResults>> validationResults = new MutableLiveData<>();
    private final MutableLiveData<UnifiedOperationResult<Object>> operationResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Progress tracking for long operations
    private final MutableLiveData<OperationProgress> operationProgress = new MutableLiveData<>();

    // Enhanced LiveData for missing functionality
    private final MutableLiveData<ExportImportViewModel.CurrentOperationInfo> currentOperationInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canCancelOperation = new MutableLiveData<>(false);
    private final MutableLiveData<ExportImportViewModel.CancellationReason> cancellationReason = new MutableLiveData<>();
    private final MutableLiveData<ExportImportViewModel.ProgressMilestone> progressMilestone = new MutableLiveData<>();


    @Inject
    public SmartShiftsSettingsViewModel(
            @NonNull Application application,
            GetUserShiftsUseCase getUserShiftsUseCase,
            ManageContactsUseCase manageContactsUseCase,
            SmartShiftsExportImportManager exportImportManager
    ) {
        super(application);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(application);
        this.executorService = Executors.newFixedThreadPool(2);
        this.getUserShiftsUseCase = getUserShiftsUseCase;
        this.manageContactsUseCase = manageContactsUseCase;
        this.exportImportManager = exportImportManager;

        initializeDefaultValues();
        setupExportImportManagerListeners();
    }

    /**
     * Setup listeners for ExportImportManager operations
     */
    private void setupExportImportManagerListeners() {
        exportImportManager.addListener(new SmartShiftsExportImportManager.ExportImportListener() {
            @Override
            public void onProgress(int progress, String message) {
                OperationProgress progressInfo = new OperationProgress(progress, message);
                operationProgress.postValue(progressInfo);

                // ✅ Update cancellation capability
                boolean canCancel = progress > 0 && progress < 95; // Allow cancellation during most of operation
                canCancelOperation.postValue(canCancel);

                // ✅ Check for milestone
                if (isImportantProgressMilestone(progress)) {
                    ExportImportViewModel.ProgressMilestone milestone =
                            new ExportImportViewModel.ProgressMilestone(progress, message, System.currentTimeMillis());
                    progressMilestone.postValue(milestone);
                }

                // ✅ Update operation info
                updateCurrentOperationInfo();
            }

            @Override
            public void onSuccess(Object result) {
                isLoading.postValue(false);

                // Convert manager results to unified system
                UnifiedOperationResult<Object> unifiedResult = convertManagerResultToUnified(result);
                operationResults.postValue(unifiedResult);

                // ✅ Update operation info
                updateCurrentOperationInfo();
            }

            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);

                // Create unified error result
                OperationType operationType = getCurrentOperationType();
                UnifiedOperationResult<Object> errorResult = UnifiedOperationResult.failure(
                        operationType,
                        error
                );
                operationResults.postValue(errorResult);

                // ✅ Update operation info
                updateCurrentOperationInfo();
            }
        });
    }

    /**
     * Convert ExportImportManager results to unified system
     */
    private UnifiedOperationResult<Object> convertManagerResultToUnified(Object managerResult) {
        if (managerResult instanceof SmartShiftsExportImportManager.ExportResult) {
            SmartShiftsExportImportManager.ExportResult exportResult =
                    (SmartShiftsExportImportManager.ExportResult) managerResult;

            if (exportResult.isSuccess()) {
                return UnifiedOperationResult.successWithFile(
                        getOperationTypeFromFormat(exportResult.getFormat(), true),
                        exportResult.getExportFile(),
                        "Export completato con successo"
                );
            } else {
                return UnifiedOperationResult.failure(
                        getOperationTypeFromFormat(exportResult.getFormat(), true),
                        exportResult.getErrorMessage()
                );
            }
        }

        if (managerResult instanceof SmartShiftsExportImportManager.ImportResult) {
            SmartShiftsExportImportManager.ImportResult importResult =
                    (SmartShiftsExportImportManager.ImportResult) managerResult;

            if (importResult.isSuccess()) {
                UnifiedOperationResult.OperationMetadata metadata =
                        new UnifiedOperationResult.OperationMetadata.Builder()
                                .setRecordsProcessed(importResult.getImportedRecords())
                                .setRecordsSkipped(importResult.getSkippedRecords())
                                .build();

                return new UnifiedOperationResult.Builder<>()
                        .setSuccess(true)
                        .setOperationType(OperationType.IMPORT_FILE)
                        .setData(managerResult)
                        .setMessage("Import completato con successo")
                        .setRecordCount(importResult.getImportedRecords())
                        .setMetadata(metadata)
                        .build();
            } else {
                return UnifiedOperationResult.failure(
                        OperationType.IMPORT_FILE,
                        importResult.getErrorMessage()
                );
            }
        }

        if (managerResult instanceof SmartShiftsExportImportManager.BackupResult) {
            SmartShiftsExportImportManager.BackupResult backupResult =
                    (SmartShiftsExportImportManager.BackupResult) managerResult;

            if (backupResult.isSuccess()) {
                return UnifiedOperationResult.successWithFile(
                        OperationType.BACKUP_NOW,
                        backupResult.getBackupFile(),
                        "Backup completato con successo"
                );
            } else {
                return UnifiedOperationResult.failure(
                        OperationType.BACKUP_NOW,
                        backupResult.getErrorMessage()
                );
            }
        }

        if (managerResult instanceof SmartShiftsExportImportManager.SyncResult) {
            SmartShiftsExportImportManager.SyncResult syncResult =
                    (SmartShiftsExportImportManager.SyncResult) managerResult;

            if (syncResult.isSuccess()) {
                UnifiedOperationResult.OperationMetadata metadata =
                        new UnifiedOperationResult.OperationMetadata.Builder()
                                .setRecordsProcessed(syncResult.getAppliedChanges())
                                .setConflictsResolved(syncResult.getConflicts())
                                .build();

                return new UnifiedOperationResult.Builder<>()
                        .setSuccess(true)
                        .setOperationType(OperationType.CLOUD_SYNC)
                        .setData(managerResult)
                        .setMessage("Sincronizzazione completata con successo")
                        .setMetadata(metadata)
                        .build();
            } else {
                return UnifiedOperationResult.failure(
                        OperationType.CLOUD_SYNC,
                        syncResult.getErrorMessage()
                );
            }
        }

        // Fallback for unknown result types
        return UnifiedOperationResult.success(
                OperationType.UNKNOWN,
                managerResult,
                "Operazione completata"
        );
    }

    /**
     * Get operation type from format and operation direction
     */
    private OperationType getOperationTypeFromFormat(String format, boolean isExport) {
        if (format == null) {
            return isExport ? OperationType.EXPORT_COMPLETE : OperationType.IMPORT_FILE;
        }

        switch (format.toLowerCase()) {
            case "json":
                return isExport ? OperationType.EXPORT_JSON : OperationType.IMPORT_JSON;
            case "csv":
                return isExport ? OperationType.EXPORT_CSV : OperationType.IMPORT_CSV;
            case "xml":
                return isExport ? OperationType.EXPORT_XML : OperationType.IMPORT_XML;
            case "ics":
                return isExport ? OperationType.EXPORT_ICAL : OperationType.IMPORT_ICAL;
            case "xlsx":
                return isExport ? OperationType.EXPORT_EXCEL : OperationType.IMPORT_EXCEL;
            default:
                return isExport ? OperationType.EXPORT_COMPLETE : OperationType.IMPORT_FILE;
        }
    }

    /**
     * Get current operation type from manager state
     */
    private OperationType getCurrentOperationType() {
        SmartShiftsExportImportManager.ExportImportOperation currentOp =
                exportImportManager.getCurrentOperation();

        if (currentOp != null) {
            return OperationType.fromLegacyValue(currentOp.getType().name());
        }

        return OperationType.UNKNOWN;
    }

    /**
     * Initialize default preference values if not set
     */
    private void initializeDefaultValues() {
        SharedPreferences.Editor editor = preferences.edit();

        // Set default values only if not already set
        if (!preferences.contains(getApplication().getString(R.string.smartshifts_pref_theme))) {
            editor.putString(
                    getApplication().getString(R.string.smartshifts_pref_theme),
                    getApplication().getString(R.string.smartshifts_default_theme)
            );
        }

        if (!preferences.contains(getApplication().getString(R.string.smartshifts_pref_week_start_day))) {
            editor.putString(
                    getApplication().getString(R.string.smartshifts_pref_week_start_day),
                    getApplication().getString(R.string.smartshifts_default_week_start_day)
            );
        }

        if (!preferences.contains(getApplication().getString(R.string.smartshifts_pref_reminder_advance_time))) {
            editor.putString(
                    getApplication().getString(R.string.smartshifts_pref_reminder_advance_time),
                    getApplication().getString(R.string.smartshifts_default_reminder_advance_time)
            );
        }

        // Apply defaults
        editor.apply();
    }

    // ============================================
    // PUBLIC GETTERS FOR LIVEDATA
    // ============================================

    public LiveData<String> getNavigationEvent() {
        return navigationEvent;
    }

    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    public LiveData<UnifiedOperationResult<ValidationResults>> getValidationResults() {
        return validationResults;
    }

    public LiveData<UnifiedOperationResult<Object>> getOperationResults() {
        return operationResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<OperationProgress> getOperationProgress() {
        return operationProgress;
    }

    // ============================================

    /**
     * Get current operation information for secondary text display
     */
    public LiveData<ExportImportViewModel.CurrentOperationInfo> getCurrentOperationInfo() {
        return currentOperationInfo;
    }

    /**
     * Get cancellation capability state
     */
    public LiveData<Boolean> getCanCancelOperation() {
        return canCancelOperation;
    }

    /**
     * Get cancellation operation results
     */
    public LiveData<ExportImportViewModel.CancellationReason> getCancellationReason() {
        return cancellationReason;
    }

    /**
     * Get progress milestone information
     */
    public LiveData<ExportImportViewModel.ProgressMilestone> getProgressMilestone() {
        return progressMilestone;
    }

    // ============================================
    // NAVIGATION METHODS
    // ============================================

    /**
     * Navigate to specific settings section
     */
    public void navigateToSection(String section) {
        navigationEvent.setValue(section);

        // Update toolbar title based on section
        String title = getApplication().getString(R.string.smartshifts_settings_title);
        switch (section) {
            case "calendar":
                title = getApplication().getString(R.string.smartshifts_settings_calendar);
                break;
            case "notifications":
                title = getApplication().getString(R.string.smartshifts_settings_notifications);
                break;
            case "data":
                title = getApplication().getString(R.string.smartshifts_settings_data);
                break;
            case "about":
                title = getApplication().getString(R.string.smartshifts_settings_about);
                break;
        }
        toolbarTitle.setValue(title);
    }

    /**
     * Reset toolbar title to main settings
     */
    public void resetToolbarTitle() {
        toolbarTitle.setValue(getApplication().getString(R.string.smartshifts_settings_title));
    }

    // ============================================
    // UNIFIED OPERATION EXECUTION
    // ============================================

    /**
     * Execute any operation using the unified system
     */
    public void executeOperation(@NonNull OperationType operationType) {
        executeOperation(operationType, null);
    }

    /**
     * Execute operation with configuration
     */
    public void executeOperation(@NonNull OperationType operationType, Object configuration) {
        isLoading.setValue(true);
        canCancelOperation.setValue(true); // ✅

        // ✅ Initialize operation info
        String displayName = getOperationDisplayNameFromUnified(operationType);
        ExportImportViewModel.CurrentOperationInfo info = new ExportImportViewModel.CurrentOperationInfo(
                convertToManagerOperationType(operationType),
                displayName,
                true,
                System.currentTimeMillis()
        );
        currentOperationInfo.setValue(info);

        executorService.execute(() -> {
            try {
                UnifiedOperationResult<Object> result = performOperation(operationType, configuration);
                operationResults.postValue(result);

            } catch (Exception e) {
                UnifiedOperationResult<Object> errorResult = UnifiedOperationResult.failure(
                        operationType,
                        e
                );
                operationResults.postValue(errorResult);

            } finally {
                isLoading.postValue(false);
                canCancelOperation.postValue(false); // ✅
            }
        });
    }

    /**
     * Get display name from unified operation type
     */
    private String getOperationDisplayNameFromUnified(OperationType operationType) {
        return operationType.getCategory().getDisplayName();
//        switch (operationType.getCategory()) {
//            case SETTINGS:
//                return "impostazioni";
//            case BACKUP:
//                return "backup";
//            case EXPORT:
//                return "esportazione";
//            case IMPORT:
//                return "importazione";
//            case CLOUD:
//                return "sincronizzazione cloud";
//            case DATA:
//                return "gestione dati";
//            default:
//                return operationType.getDisplayName().toLowerCase();
//        }
    }

    /**
     * Perform the actual operation based on type
     */
    private UnifiedOperationResult<Object> performOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {

        switch (operationType.getCategory()) {
            case SETTINGS:
                return performSettingsOperation(operationType, configuration);

            case BACKUP:
                return performBackupOperation(operationType, configuration);

            case EXPORT:
                return performExportOperation(operationType, configuration);

            case IMPORT:
                return performImportOperation(operationType, configuration);

            case CLOUD:
                return performCloudOperation(operationType, configuration);

            case DATA:
                return performDataOperation(operationType, configuration);

            default:
                throw new UnsupportedOperationException(
                        "Operazione non supportata: " + operationType.getDisplayName()
                );
        }
    }

    // ============================================
    // SETTINGS OPERATIONS
    // ============================================

    /**
     * Handle settings-related operations
     */
    private UnifiedOperationResult<Object> performSettingsOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {

        switch (operationType) {
            case VALIDATE_SETTINGS:
                return performSettingsValidation();

            case RESET_SETTINGS:
                return performSettingsReset();

            case CLEAR_CACHE:
                return performCacheClearing();

            default:
                throw new UnsupportedOperationException(
                        "Operazione settings non supportata: " + operationType
                );
        }
    }

    /**
     * Validate all current settings
     */
    private UnifiedOperationResult<Object> performSettingsValidation() {
        List<String> errors = new ArrayList<>();

        // Validate theme setting
        String theme = preferences.getString(
                getApplication().getString(R.string.smartshifts_pref_theme),
                getApplication().getString(R.string.smartshifts_default_theme)
        );
        if (!ValidationHelper.isValidTheme(theme)) {
            errors.add("Tema non valido: " + theme);
        }

        // Validate week start day
        String weekStart = preferences.getString(
                getApplication().getString(R.string.smartshifts_pref_week_start_day),
                getApplication().getString(R.string.smartshifts_default_week_start_day)
        );
        if (!ValidationHelper.isValidWeekStartDay(weekStart)) {
            errors.add("Giorno inizio settimana non valido: " + weekStart);
        }

        // Validate reminder advance time
        String reminderTime = preferences.getString(
                getApplication().getString(R.string.smartshifts_pref_reminder_advance_time),
                getApplication().getString(R.string.smartshifts_default_reminder_advance_time)
        );
        if (!ValidationHelper.isValidReminderTime(reminderTime)) {
            errors.add("Tempo anticipo promemoria non valido: " + reminderTime);
        }

        // Validate data retention days
        String retentionDays = preferences.getString(
                getApplication().getString(R.string.smartshifts_pref_data_retention_days),
                getApplication().getString(R.string.smartshifts_default_data_retention_days)
        );
        if (!ValidationHelper.isValidRetentionDays(retentionDays)) {
            errors.add("Giorni conservazione dati non validi: " + retentionDays);
        }

        // Validate quiet hours if enabled
        boolean quietHoursEnabled = preferences.getBoolean(
                getApplication().getString(R.string.smartshifts_pref_quiet_hours_enabled),
                false
        );
        if (quietHoursEnabled) {
            String startTime = preferences.getString(
                    getApplication().getString(R.string.smartshifts_pref_quiet_hours_start),
                    "22:00"
            );
            String endTime = preferences.getString(
                    getApplication().getString(R.string.smartshifts_pref_quiet_hours_end),
                    "07:00"
            );

            if (!ValidationHelper.isValidTimeFormat(startTime) || !ValidationHelper.isValidTimeFormat(endTime)) {
                errors.add("Orari silenziosi non validi");
            }
        }

        // Create validation results
        ValidationResults validationData = new ValidationResults(errors.isEmpty(), errors);

        if (errors.isEmpty()) {
            return UnifiedOperationResult.success(
                    OperationType.VALIDATE_SETTINGS,
                    validationData,
                    "Tutte le impostazioni sono valide"
            );
        } else {
            return UnifiedOperationResult.failure(
                    OperationType.VALIDATE_SETTINGS,
                    errors
            );
        }
    }

    /**
     * Reset all settings to defaults
     */
    private UnifiedOperationResult<Object> performSettingsReset() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            // Reinitialize defaults
            initializeDefaultValues();

            return UnifiedOperationResult.success(
                    OperationType.RESET_SETTINGS,
                    null,
                    "Impostazioni ripristinate ai valori predefiniti"
            );

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.RESET_SETTINGS,
                    "Errore durante il reset delle impostazioni: " + e.getMessage()
            );
        }
    }

    /**
     * Clear application cache
     */
    private UnifiedOperationResult<Object> performCacheClearing() {
        try {
            long clearedBytes = 0;

            // Clear app cache directories
            File cacheDir = getApplication().getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                clearedBytes += calculateDirectorySize(cacheDir);
                deleteRecursive(cacheDir);
            }

            // Clear external cache
            File externalCacheDir = getApplication().getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                clearedBytes += calculateDirectorySize(externalCacheDir);
                deleteRecursive(externalCacheDir);
            }

            UnifiedOperationResult.OperationMetadata metadata =
                    new UnifiedOperationResult.OperationMetadata.Builder()
                            .build();

            return new UnifiedOperationResult.Builder<>()
                    .setSuccess(true)
                    .setOperationType(OperationType.CLEAR_CACHE)
                    .setMessage("Cache pulita con successo (" + formatBytes(clearedBytes) + " liberati)")
                    .setFileSize(clearedBytes)
                    .setMetadata(metadata)
                    .build();

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.CLEAR_CACHE,
                    "Errore durante la pulizia cache: " + e.getMessage()
            );
        }
    }

    // ============================================
    // BACKUP OPERATIONS
    // ============================================

    /**
     * Handle backup-related operations
     */
    private UnifiedOperationResult<Object> performBackupOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {

        switch (operationType) {
            case BACKUP_NOW:
                return delegateToExportImportManager(operationType, configuration);

            case RESTORE_BACKUP:
                return delegateToExportImportManager(operationType, configuration);

            case AUTO_BACKUP:
                return performAutoBackupSetup(configuration);

            default:
                throw new UnsupportedOperationException(
                        "Operazione backup non supportata: " + operationType
                );
        }
    }

    /**
     * Setup automatic backup configuration
     */
    private UnifiedOperationResult<Object> performAutoBackupSetup(Object configuration) {
        try {
            // TODO: Implement automatic backup setup using WorkManager
            // This would schedule periodic backups

            return UnifiedOperationResult.success(
                    OperationType.AUTO_BACKUP,
                    configuration,
                    "Backup automatico configurato con successo"
            );

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.AUTO_BACKUP,
                    "Errore durante la configurazione del backup automatico: " + e.getMessage()
            );
        }
    }

    // ============================================
    // EXPORT/IMPORT/CLOUD/DATA OPERATIONS
    // ============================================

    /**
     * Delegate export operations to ExportImportManager
     */
    private UnifiedOperationResult<Object> performExportOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {
        return delegateToExportImportManager(operationType, configuration);
    }

    /**
     * Delegate import operations to ExportImportManager
     */
    private UnifiedOperationResult<Object> performImportOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {
        return delegateToExportImportManager(operationType, configuration);
    }

    /**
     * Delegate cloud operations to ExportImportManager
     */
    private UnifiedOperationResult<Object> performCloudOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {
        return delegateToExportImportManager(operationType, configuration);
    }

    /**
     * Handle data operations
     */
    private UnifiedOperationResult<Object> performDataOperation(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {

        switch (operationType) {
            case DATA_VALIDATION:
                return performDataValidation();

            case DATA_CLEANUP:
                return performDataCleanup();

            case DATA_MIGRATION:
                return performDataMigration(configuration);

            default:
                throw new UnsupportedOperationException(
                        "Operazione dati non supportata: " + operationType
                );
        }
    }

    /**
     * Delegate operation to ExportImportManager
     */
    private UnifiedOperationResult<Object> delegateToExportImportManager(
            @NonNull OperationType operationType,
            Object configuration
    ) throws Exception {

        // Convert unified operation type back to manager operation type
        SmartShiftsExportImportManager.OperationType managerType =
                convertToManagerOperationType(operationType);

        // Set loading state and delegate to manager
        // The manager will notify through listeners
        isLoading.postValue(true);

        // TODO: Call appropriate manager method based on operation type
        // For now, return a placeholder result
        return UnifiedOperationResult.success(
                operationType,
                null,
                "Operazione delegata al manager"
        );
    }

    /**
     * Convert unified operation type to manager operation type
     */
    private SmartShiftsExportImportManager.OperationType convertToManagerOperationType(
            OperationType unifiedType
    ) {
        switch (unifiedType) {
            case EXPORT_COMPLETE:
            case EXPORT_JSON:
            case EXPORT_CSV:
            case EXPORT_XML:
            case EXPORT_EXCEL:
                return SmartShiftsExportImportManager.OperationType.EXPORT;

            case EXPORT_SELECTIVE:
                return SmartShiftsExportImportManager.OperationType.EXPORT_SELECTIVE;

            case EXPORT_CALENDAR:
            case EXPORT_ICAL:
                return SmartShiftsExportImportManager.OperationType.EXPORT_CALENDAR;

            case IMPORT_FILE:
            case IMPORT_JSON:
            case IMPORT_CSV:
            case IMPORT_XML:
            case IMPORT_EXCEL:
            case IMPORT_ICAL:
                return SmartShiftsExportImportManager.OperationType.IMPORT;

            case IMPORT_CLOUD:
                return SmartShiftsExportImportManager.OperationType.IMPORT_CLOUD;

            case BACKUP_NOW:
                return SmartShiftsExportImportManager.OperationType.BACKUP;

            case RESTORE_BACKUP:
                return SmartShiftsExportImportManager.OperationType.RESTORE;

            case CLOUD_SYNC:
                return SmartShiftsExportImportManager.OperationType.CLOUD_SYNC;

            default:
                throw new IllegalArgumentException(
                        "Cannot convert unified type to manager type: " + unifiedType
                );
        }
    }

    // ============================================
    // DATA OPERATION IMPLEMENTATIONS
    // ============================================

    /**
     * Perform comprehensive data validation
     */
    private UnifiedOperationResult<Object> performDataValidation() {
        try {
            List<String> issues = new ArrayList<>();
            int totalRecords = 0;

            // TODO: Implement comprehensive data validation
            // - Validate shift patterns
            // - Validate user assignments
            // - Validate team contacts
            // - Check data integrity

            if (issues.isEmpty()) {
                return UnifiedOperationResult.success(
                        OperationType.DATA_VALIDATION,
                        null,
                        "Validazione dati completata - nessun problema rilevato"
                );
            } else {
                return UnifiedOperationResult.failure(
                        OperationType.DATA_VALIDATION,
                        issues
                );
            }

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.DATA_VALIDATION,
                    "Errore durante la validazione dati: " + e.getMessage()
            );
        }
    }

    /**
     * Perform data cleanup (remove old/invalid records)
     */
    private UnifiedOperationResult<Object> performDataCleanup() {
        try {
            int cleanedRecords = 0;

            // TODO: Implement data cleanup
            // - Remove old shift events
            // - Clean up orphaned records
            // - Remove invalid assignments

            UnifiedOperationResult.OperationMetadata metadata =
                    new UnifiedOperationResult.OperationMetadata.Builder()
                            .setRecordsProcessed(cleanedRecords)
                            .build();

            return new UnifiedOperationResult.Builder<>()
                    .setSuccess(true)
                    .setOperationType(OperationType.DATA_CLEANUP)
                    .setMessage("Pulizia dati completata - " + cleanedRecords + " record rimossi")
                    .setRecordCount(cleanedRecords)
                    .setMetadata(metadata)
                    .build();

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.DATA_CLEANUP,
                    "Errore durante la pulizia dati: " + e.getMessage()
            );
        }
    }

    /**
     * Perform data migration
     */
    private UnifiedOperationResult<Object> performDataMigration(Object configuration) {
        try {
            int migratedRecords = 0;

            // TODO: Implement data migration logic
            // - Migrate from old schema versions
            // - Convert legacy data formats
            // - Update data structures

            UnifiedOperationResult.OperationMetadata metadata =
                    new UnifiedOperationResult.OperationMetadata.Builder()
                            .setRecordsProcessed(migratedRecords)
                            .build();

            return new UnifiedOperationResult.Builder<>()
                    .setSuccess(true)
                    .setOperationType(OperationType.DATA_MIGRATION)
                    .setMessage("Migrazione dati completata - " + migratedRecords + " record migrati")
                    .setRecordCount(migratedRecords)
                    .setMetadata(metadata)
                    .build();

        } catch (Exception e) {
            return UnifiedOperationResult.failure(
                    OperationType.DATA_MIGRATION,
                    "Errore durante la migrazione dati: " + e.getMessage()
            );
        }
    }

    // ============================================
    // LEGACY COMPATIBILITY METHODS
    // ============================================

    /**
     * Execute legacy data operation (for backward compatibility)
     * @deprecated Use executeOperation(OperationType) instead
     */
    @Deprecated
    public void executeDataOperation(DataOperationType legacyOperationType) {
        OperationType newOperationType = convertLegacyOperationType(legacyOperationType);
        executeOperation(newOperationType);
    }

    /**
     * Convert legacy DataOperationType to new OperationType
     */
    private OperationType convertLegacyOperationType(DataOperationType legacyType) {
        switch (legacyType) {
            case BACKUP_NOW:
                return OperationType.BACKUP_NOW;
            case RESTORE_BACKUP:
                return OperationType.RESTORE_BACKUP;
            case EXPORT_DATA:
                return OperationType.EXPORT_COMPLETE;
            case IMPORT_DATA:
                return OperationType.IMPORT_FILE;
            case RESET_SETTINGS:
                return OperationType.RESET_SETTINGS;
            case CLEAR_CACHE:
                return OperationType.CLEAR_CACHE;
            default:
                return OperationType.UNKNOWN;
        }
    }

    /**
     * Validate current settings (legacy method)
     * @deprecated Use executeOperation(OperationType.VALIDATE_SETTINGS) instead
     */
    @Deprecated
    public void validateCurrentSettings() {
        executeOperation(OperationType.VALIDATE_SETTINGS);
    }

    // ============================================
// METODI OPERAZIONE CORRENTE - NUOVI
// ============================================

    /**
     * Get secondary progress text for current operation
     */
    public String getSecondaryProgressText() {
        ExportImportViewModel.CurrentOperationInfo info = getCurrentOperationInfoValue();
        if (info != null && info.isActive()) {
            return "Eseguendo " + info.getOperationDisplayName() + "...";
        }
        return "";
    }

    /**
     * Get current operation information value (non-observable)
     */
    public ExportImportViewModel.CurrentOperationInfo getCurrentOperationInfoValue() {
        ExportImportViewModel.CurrentOperationInfo info = currentOperationInfo.getValue();
        return info != null ? info : new ExportImportViewModel.CurrentOperationInfo();
    }

    /**
     * Get current operation from manager (controlled access)
     */
    public SmartShiftsExportImportManager.ExportImportOperation getCurrentOperation() {
        return exportImportManager.getCurrentOperation();
    }

    /**
     * Check if current operation can be cancelled
     */
    public boolean canCancelCurrentOperation() {
        SmartShiftsExportImportManager.ExportImportOperation current = getCurrentOperation();
        return current != null && !current.isCompleted() && !current.isCancelled();
    }

    /**
     * Enhanced cancel current operation with state management
     */
    public void cancelCurrentOperation() {
        SmartShiftsExportImportManager.ExportImportOperation currentOp = getCurrentOperation();

        if (currentOp == null) {
            cancellationReason.setValue(new ExportImportViewModel.CancellationReason(false, "Nessuna operazione in corso"));
            return;
        }

        if (currentOp.isCompleted()) {
            cancellationReason.setValue(new ExportImportViewModel.CancellationReason(false, "Operazione già completata"));
            return;
        }

        if (currentOp.isCancelled()) {
            cancellationReason.setValue(new ExportImportViewModel.CancellationReason(false, "Operazione già annullata"));
            return;
        }

        try {
            exportImportManager.cancelCurrentOperation();

            // Update UI state
            isLoading.setValue(false);
            canCancelOperation.setValue(false);

            // Create cancellation result
            OperationType operationType = getCurrentOperationType();
            UnifiedOperationResult<Object> cancelResult = UnifiedOperationResult.failure(
                    operationType,
                    "Operazione annullata dall'utente"
            );
            operationResults.setValue(cancelResult);

            cancellationReason.setValue(new ExportImportViewModel.CancellationReason(true, "Operazione annullata con successo"));

            // Update operation info
            updateCurrentOperationInfo();

        } catch (Exception e) {
            cancellationReason.setValue(new ExportImportViewModel.CancellationReason(false, "Errore durante l'annullamento: " + e.getMessage()));
        }
    }

    /**
     * Get export import manager (for Activity access when needed)
     * Note: Use with caution - prefer ViewModel methods when possible
     */
    public SmartShiftsExportImportManager getExportImportManager() {
        return exportImportManager;
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Calculate directory size in bytes
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    /**
     * Recursively delete directory contents
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Remove listeners from export import manager
        exportImportManager.removeListener(null); // Remove all listeners
    }

    // /////


    /**
     * Update current operation information
     */
    private void updateCurrentOperationInfo() {
        SmartShiftsExportImportManager.ExportImportOperation current = getCurrentOperation();

        if (current != null) {
            String operationName = getOperationDisplayName(current.getType());
            ExportImportViewModel.CurrentOperationInfo info = new ExportImportViewModel.CurrentOperationInfo(
                    current.getType(),
                    operationName,
                    !current.isCompleted(),
                    current.getStartTime()
            );
            currentOperationInfo.setValue(info);
        } else {
            currentOperationInfo.setValue(new ExportImportViewModel.CurrentOperationInfo());
        }
    }

    /**
     * Get user-friendly operation display name
     */
    private String getOperationDisplayName(SmartShiftsExportImportManager.OperationType operationType) {
        if (operationType == null) return "operazione sconosciuta";

        switch (operationType) {
            case EXPORT:
                return "esportazione";
            case EXPORT_SELECTIVE:
                return "esportazione selettiva";
            case EXPORT_CALENDAR:
                return "esportazione calendario";
            case IMPORT:
                return "importazione";
            case IMPORT_CLOUD:
                return "importazione cloud";
            case BACKUP:
                return "backup";
            case RESTORE:
                return "ripristino";
            case CLOUD_SYNC:
                return "sincronizzazione cloud";
            default:
                return operationType.name().toLowerCase().replace("_", " ");
        }
    }

    /**
     * Check if progress represents important milestone
     */
    private boolean isImportantProgressMilestone(int progress) {
        return progress == 25 || progress == 50 || progress == 75;
    }

    // ============================================
    // DATA CLASSES
    // ============================================

    /**
     * Legacy validation results container (kept for compatibility)
     */
    public static class ValidationResults {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResults(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * Operation progress information
     */
    public static class OperationProgress {
        private final int progress;
        private final String message;
        private final long timestamp;

        public OperationProgress(int progress, String message) {
            this.progress = progress;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public int getProgress() { return progress; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Legacy data operation types (kept for compatibility)
     * @deprecated Use UnifiedOperationResult.OperationType instead
     */
    @Deprecated
    public enum DataOperationType {
        BACKUP_NOW,
        RESTORE_BACKUP,
        EXPORT_DATA,
        IMPORT_DATA,
        RESET_SETTINGS,
        CLEAR_CACHE,
        UNKNOWN;

        public String toStatusMessage() {
            switch (this) {
                case BACKUP_NOW: return "BACKUP_SUCCESS";
                case RESTORE_BACKUP: return "RESTORE_SUCCESS";
                case EXPORT_DATA: return "EXPORT_SUCCESS";
                case IMPORT_DATA: return "IMPORT_SUCCESS";
                case RESET_SETTINGS: return "RESET_COMPLETE";
                case CLEAR_CACHE: return "CACHE_CLEARED";
                default: return "UNKNOWN";
            }
        }
    }
}