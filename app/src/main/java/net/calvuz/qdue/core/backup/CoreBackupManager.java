package net.calvuz.qdue.core.backup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.calvuz.qdue.core.backup.models.EntityBackupPackage;
import net.calvuz.qdue.core.backup.models.FullApplicationBackup;
import net.calvuz.qdue.core.backup.models.PreferencesBackupPackage;
import net.calvuz.qdue.core.backup.services.CalendarDatabaseBackupService;
import net.calvuz.qdue.core.backup.services.DatabaseBackupService;
import net.calvuz.qdue.core.backup.services.PreferencesBackupService;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EXTENDED: CoreBackupManager - Unified Backup System with Calendar Support
 *
 * <p>Enhanced version of the CoreBackupManager supporting both QDueDatabase and CalendarDatabase
 * following clean architecture principles with dependency injection compliance.</p>
 *
 * <h3>Enhanced Features:</h3>
 * <ul>
 *   <li><strong>Dual Database Support</strong>: Handles both QDue and Calendar databases</li>
 *   <li><strong>Calendar Auto Backup</strong>: Automatic backup triggers for calendar operations</li>
 *   <li><strong>Unified Backup Format</strong>: Single backup file with both database contents</li>
 *   <li><strong>Migration Ready</strong>: Prepares for QDueDatabase → CalendarDatabase transition</li>
 *   <li><strong>Backward Compatibility</strong>: Maintains all existing QDueDatabase functionality</li>
 * </ul>
 *
 * <h3>Calendar Entities Supported:</h3>
 * <ul>
 *   <li>ShiftEntity - Shift type templates</li>
 *   <li>TeamEntity - Work team definitions</li>
 *   <li>RecurrenceRuleEntity - RRULE patterns</li>
 *   <li>ShiftExceptionEntity - Schedule exceptions</li>
 *   <li>UserScheduleAssignmentEntity - User-team assignments</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Calendar Integration & Clean Architecture
 * @since Clean Architecture Phase 2
 */
public class CoreBackupManager {

    private static final String TAG = "CoreBackupManager";

    // Backup configuration
    private static final String BACKUP_DIR_NAME = "qdue_unified_backup";
    private static final String BACKUP_FILE_PREFIX = "qdue_backup_";
    private static final String CALENDAR_BACKUP_PREFIX = "calendar_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_BACKUP_FILES = 10;
    private static final int MAX_ENTITY_BACKUP_FILES = 5;

    // Preferences keys for unified backup system
    private static final String PREF_AUTO_BACKUP_ENABLED = "unified_auto_backup_enabled";
    private static final String PREF_LAST_BACKUP_TIME = "unified_last_backup_time";
    private static final String PREF_BACKUP_COUNT = "unified_backup_count";
    private static final String PREF_BACKUP_ON_CREATE = "unified_backup_on_create";
    private static final String PREF_BACKUP_ON_UPDATE = "unified_backup_on_update";
    private static final String PREF_BACKUP_ON_DELETE = "unified_backup_on_delete";
    private static final String PREF_BACKUP_ON_IMPORT = "unified_backup_on_import";

    // Calendar-specific preferences
    private static final String PREF_CALENDAR_AUTO_BACKUP_ENABLED = "calendar_auto_backup_enabled";
    private static final String PREF_CALENDAR_LAST_BACKUP_TIME = "calendar_last_backup_time";
    private static final String PREF_CALENDAR_BACKUP_COUNT = "calendar_backup_count";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final CalendarDatabase mCalendarDatabase;
    private final SharedPreferences mPreferences;
    private final DatabaseBackupService mDatabaseBackupService;
    private final CalendarDatabaseBackupService mCalendarDatabaseBackupService;
    private final PreferencesBackupService mPreferencesBackupService;
    private final ExecutorService mExecutorService;
    private final Gson mGson;
    private final File mBackupDirectory;

    // ==================== CONSTRUCTORS FOR DEPENDENCY INJECTION ====================

    /**
     * Enhanced constructor for dependency injection with Calendar support
     *
     * @param context Application context
     * @param database QDue database instance
     * @param calendarDatabase Calendar database instance
     */
    public CoreBackupManager(Context context, QDueDatabase database, CalendarDatabase calendarDatabase) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mCalendarDatabase = calendarDatabase;
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.mDatabaseBackupService = new DatabaseBackupService(mContext, database);
        this.mCalendarDatabaseBackupService = new CalendarDatabaseBackupService(mContext, calendarDatabase);
        this.mPreferencesBackupService = new PreferencesBackupService(mContext);
        this.mExecutorService = Executors.newFixedThreadPool(4); // Increased for calendar operations
        this.mGson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        // Initialize backup directory
        this.mBackupDirectory = new File(mContext.getFilesDir(), BACKUP_DIR_NAME);
        if (!mBackupDirectory.exists()) {
            boolean created = mBackupDirectory.mkdirs();
            Log.d(TAG, "Unified backup directory created: " + created);
        }

        Log.d(TAG, "CoreBackupManager initialized with Calendar support via dependency injection");
    }

    // ==================== CALENDAR AUTO BACKUP INTEGRATION ====================

    /**
     * ✅ NEW: Calendar auto backup trigger for CREATE operations
     * Used by calendar repositories when creating entities
     */
    public CompletableFuture<OperationResult<Void>> triggerCalendarAutoBackupOnCreate() {
        return triggerCalendarAutoBackup("CREATE");
    }

    /**
     * ✅ NEW: Calendar auto backup trigger for UPDATE operations
     * Used by calendar repositories when updating entities
     */
    public CompletableFuture<OperationResult<Void>> triggerCalendarAutoBackupOnUpdate() {
        return triggerCalendarAutoBackup("UPDATE");
    }

    /**
     * ✅ NEW: Calendar auto backup trigger for DELETE operations
     * Used by calendar repositories when deleting entities
     */
    public CompletableFuture<OperationResult<Void>> triggerCalendarAutoBackupOnDelete() {
        return triggerCalendarAutoBackup("DELETE");
    }

    /**
     * ✅ NEW: Calendar auto backup trigger for IMPORT operations
     * Used by calendar services when importing data
     */
    public CompletableFuture<OperationResult<Void>> triggerCalendarAutoBackupOnImport() {
        return triggerCalendarAutoBackup("IMPORT");
    }

    /**
     * ✅ NEW: Generic calendar auto backup method
     *
     * @param operation Type of operation triggering backup
     * @return CompletableFuture with operation result
     */
    private CompletableFuture<OperationResult<Void>> triggerCalendarAutoBackup(String operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isCalendarAutoBackupEnabled()) {
                    Log.d(TAG, "Calendar auto backup disabled, skipping backup for: " + operation);
                    return OperationResult.success("Calendar auto backup disabled, skipping backup for: " + operation,
                            OperationResult.OperationType.BACKUP);
                }

                Log.d(TAG, "Triggering calendar auto backup for operation: " + operation);

                if (mCalendarDatabase == null) {
                    Log.w(TAG, "Calendar database not available for backup");
                    return OperationResult.failure("Calendar database not initialized",
                            OperationResult.OperationType.BACKUP);
                }

                // Create calendar-specific backup
                OperationResult<String> backupResult = createCalendarBackup();

                if (backupResult.isSuccess()) {
                    updateCalendarBackupTimestamp();
                    Log.d(TAG, "Calendar auto backup completed successfully for: " + operation);
                    return OperationResult.success("Calendar auto backup completed successfully for: " + operation,
                            OperationResult.OperationType.BACKUP);
                } else {
                    Log.e(TAG, "Calendar auto backup failed for: " + operation + " - " + backupResult.getErrors());
                    return OperationResult.failure(backupResult.getErrors(),
                            OperationResult.OperationType.BACKUP);
                }

            } catch (Exception e) {
                Log.e(TAG, "Calendar auto backup exception for: " + operation, e);
                return OperationResult.failure("Calendar backup failed: " + e.getMessage(),
                        OperationResult.OperationType.BACKUP);
            }
        }, mExecutorService);
    }

    // ==================== UNIFIED AUTO BACKUP (EXISTING + ENHANCED) ====================

    /**
     * ✅ ENHANCED: Standard auto backup method for all service layers
     * Now includes both QDue and Calendar databases
     */
    public CompletableFuture<OperationResult<Void>> triggerAutoBackupOnCreate() {
        return triggerAutoBackup("CREATE");
    }

    /**
     * ✅ ENHANCED: Auto backup for UPDATE operations
     * Backs up both databases when Calendar is available
     */
    public CompletableFuture<OperationResult<Void>> triggerAutoBackupOnUpdate() {
        return triggerAutoBackup("UPDATE");
    }

    /**
     * ✅ ENHANCED: Auto backup for DELETE operations
     * Backs up both databases when Calendar is available
     */
    public CompletableFuture<OperationResult<Void>> triggerAutoBackupOnDelete() {
        return triggerAutoBackup("DELETE");
    }

    /**
     * ✅ ENHANCED: Auto backup for IMPORT operations
     * Backs up both databases when Calendar is available
     */
    public CompletableFuture<OperationResult<Void>> triggerAutoBackupOnImport() {
        return triggerAutoBackup("IMPORT");
    }

    /**
     * ✅ ENHANCED: Generic auto backup method supporting both databases
     */
    private CompletableFuture<OperationResult<Void>> triggerAutoBackup(String operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isAutoBackupEnabled()) {
                    Log.d(TAG, "Auto backup disabled, skipping backup for: " + operation);
                    return OperationResult.success("Auto backup disabled, skipping backup for: " + operation,
                            OperationResult.OperationType.BACKUP);
                }

                Log.d(TAG, "Triggering unified auto backup for operation: " + operation);

                // Create unified backup (both databases)
                OperationResult<String> backupResult = createFullApplicationBackup();

                if (backupResult.isSuccess()) {
                    updateBackupTimestamp();
                    cleanupOldBackupFiles();
                    Log.d(TAG, "Unified auto backup completed successfully for: " + operation);
                    return OperationResult.success("Unified auto backup completed successfully for: " + operation,
                            OperationResult.OperationType.BACKUP);
                } else {
                    Log.e(TAG, "Unified auto backup failed for: " + operation + " - " + backupResult.getErrors());
                    return OperationResult.failure(backupResult.getErrors(),
                            OperationResult.OperationType.BACKUP);
                }

            } catch (Exception e) {
                Log.e(TAG, "Unified auto backup exception for: " + operation, e);
                return OperationResult.failure("Unified backup failed: " + e.getMessage(),
                        OperationResult.OperationType.BACKUP);
            }
        }, mExecutorService);
    }

    // ==================== CALENDAR BACKUP OPERATIONS ====================

    /**
     * ✅ NEW: Create backup of Calendar database only
     */
    public OperationResult<String> createCalendarBackup() {
        try {
            if (mCalendarDatabase == null) {
                return OperationResult.failure("Calendar database not available",
                        OperationResult.OperationType.BACKUP);
            }

            Log.d(TAG, "Creating calendar-only backup");

            // Generate calendar backup using service
            OperationResult<EntityBackupPackage> calendarBackup =
                    mCalendarDatabaseBackupService.generateCalendarBackup();

            if (calendarBackup.isFailure()) {
                return OperationResult.failure(calendarBackup.getErrors(),
                        OperationResult.OperationType.BACKUP);
            }

            // Create backup file with timestamp
            String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
            String filename = CALENDAR_BACKUP_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
            File backupFile = new File(mBackupDirectory, filename);

            try (FileWriter writer = new FileWriter(backupFile)) {
                mGson.toJson(calendarBackup.getData(), writer);
            }

            Log.d(TAG, "Calendar backup created successfully: " + filename);
            return OperationResult.success(backupFile.getAbsolutePath(), OperationResult.OperationType.BACKUP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create calendar backup", e);
            return OperationResult.failure("Calendar backup failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    /**
     * ✅ ENHANCED: Create full application backup including both databases
     */
    public OperationResult<String> createFullApplicationBackup() {
        try {
            Log.d(TAG, "Creating full application backup (QDue + Calendar)");

            FullApplicationBackup fullBackup = new FullApplicationBackup();
            fullBackup.version = "2.0";
            fullBackup.timestamp = LocalDateTime.now().toString();
            fullBackup.appVersion = getAppVersion();

            // Backup QDue database using NEW methods
            OperationResult<EntityBackupPackage> qDueEventsBackup =
                    mDatabaseBackupService.generateEventsBackup();
            if (qDueEventsBackup.isSuccess()) {
                fullBackup.eventsBackup = qDueEventsBackup.getData();
            }

            OperationResult<EntityBackupPackage> qDueUsersBackup =
                    mDatabaseBackupService.generateUsersBackup();
            if (qDueUsersBackup.isSuccess()) {
                fullBackup.usersBackup = qDueUsersBackup.getData();
            }

            OperationResult<EntityBackupPackage> qDueEstablishmentsBackup =
                    mDatabaseBackupService.generateEstablishmentsBackup();
            if (qDueEstablishmentsBackup.isSuccess()) {
                fullBackup.establishmentsBackup = qDueEstablishmentsBackup.getData();
            }

            OperationResult<EntityBackupPackage> qDueMacroBackup =
                    mDatabaseBackupService.generateMacroDepartmentsBackup();
            if (qDueMacroBackup.isSuccess()) {
                fullBackup.macroDepartmentsBackup = qDueMacroBackup.getData();
            }

            OperationResult<EntityBackupPackage> qDueSubBackup =
                    mDatabaseBackupService.generateSubDepartmentsBackup();
            if (qDueSubBackup.isSuccess()) {
                fullBackup.subDepartmentsBackup = qDueSubBackup.getData();
            }

            // NEW: Backup Calendar database if available
            if (mCalendarDatabase != null) {
                OperationResult<EntityBackupPackage> calendarBackup =
                        mCalendarDatabaseBackupService.generateCalendarBackup();
                if (calendarBackup.isSuccess()) {
                    fullBackup.calendarBackup = calendarBackup.getData();
                }
            }

            // Backup preferences using NEW method
            OperationResult<PreferencesBackupPackage> preferencesResult =
                    mPreferencesBackupService.generatePreferencesBackup();
            if (preferencesResult.isSuccess()) {
                fullBackup.preferencesBackup = preferencesResult.getData();
            }

            // Write to file
            String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
            String filename = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
            File backupFile = new File(mBackupDirectory, filename);

            try (FileWriter writer = new FileWriter(backupFile)) {
                mGson.toJson(fullBackup, writer);
            }

            Log.d(TAG, "Full application backup created successfully: " + filename);
            return OperationResult.success(backupFile.getAbsolutePath(), OperationResult.OperationType.BACKUP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create full application backup", e);
            return OperationResult.failure("Full backup failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    /**
     * ✅ BACKWARD COMPATIBILITY: Legacy auto backup method
     * Used by existing services (EventsService, UserService, etc.)
     *
     * @param entityType Type of entity being backed up
     * @param operation Operation being performed (create, update, delete, etc.)
     */
    public void performAutoBackup(String entityType, String operation) {
        try {
            Log.d(TAG, "Legacy auto backup triggered for " + entityType + " operation: " + operation);

            // Map operation to new trigger methods
            switch (operation.toLowerCase()) {
                case "create":
                case "bulk_create":
                case "import":
                    triggerAutoBackupOnCreate();
                    break;

                case "update":
                case "bulk_update":
                    triggerAutoBackupOnUpdate();
                    break;

                case "delete":
                case "bulk_delete":
                    triggerAutoBackupOnDelete();
                    break;

                default:
                    // Default to create for unknown operations
                    Log.w(TAG, "Unknown backup operation: " + operation + ", defaulting to create trigger");
                    triggerAutoBackupOnCreate();
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Legacy auto backup failed for " + entityType + " " + operation, e);
            // Don't throw exception to maintain backward compatibility
        }
    }

    /**
     * ✅ BACKWARD COMPATIBILITY: Legacy full application backup method
     * Used by existing services (OrganizationService, etc.)
     * Wraps the new createFullApplicationBackup() method for backward compatibility
     */
    public void performFullApplicationBackup() {
        try {
            Log.d(TAG, "Legacy full application backup triggered");

            OperationResult<String> result = createFullApplicationBackup();

            if (result.isSuccess()) {
                Log.d(TAG, "Legacy full application backup completed successfully: " + result.getData());
            } else {
                Log.e(TAG, "Legacy full application backup failed: " + result.getFormattedErrorMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Legacy full application backup exception", e);
            // Don't throw exception to maintain backward compatibility
        }
    }

    // ==================== CALENDAR BACKUP SETTINGS ====================

    /**
     * ✅ NEW: Check if calendar auto backup is enabled
     */
    public boolean isCalendarAutoBackupEnabled() {
        return mPreferences.getBoolean(PREF_CALENDAR_AUTO_BACKUP_ENABLED, true);
    }

    /**
     * ✅ NEW: Enable/disable calendar auto backup
     */
    public void setCalendarAutoBackupEnabled(boolean enabled) {
        mPreferences.edit()
                .putBoolean(PREF_CALENDAR_AUTO_BACKUP_ENABLED, enabled)
                .apply();
        Log.d(TAG, "Calendar auto backup " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * ✅ NEW: Get last calendar backup timestamp
     */
    public String getLastCalendarBackupTime() {
        return mPreferences.getString(PREF_CALENDAR_LAST_BACKUP_TIME, "Never");
    }

    /**
     * ✅ NEW: Update calendar backup timestamp
     */
    private void updateCalendarBackupTimestamp() {
        String timestamp = LocalDateTime.now().toString();
        int currentCount = mPreferences.getInt(PREF_CALENDAR_BACKUP_COUNT, 0);

        mPreferences.edit()
                .putString(PREF_CALENDAR_LAST_BACKUP_TIME, timestamp)
                .putInt(PREF_CALENDAR_BACKUP_COUNT, currentCount + 1)
                .apply();
    }

    // ==================== EXISTING BACKUP SETTINGS (ENHANCED) ====================

    /**
     * ✅ EXISTING: Check if auto backup is enabled
     */
    public boolean isAutoBackupEnabled() {
        return mPreferences.getBoolean(PREF_AUTO_BACKUP_ENABLED, true);
    }

    /**
     * ✅ EXISTING: Enable/disable auto backup
     */
    public void setAutoBackupEnabled(boolean enabled) {
        mPreferences.edit()
                .putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled)
                .apply();
        Log.d(TAG, "Auto backup " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * ✅ EXISTING: Get last backup timestamp
     */
    public String getLastBackupTime() {
        return mPreferences.getString(PREF_LAST_BACKUP_TIME, "Never");
    }

    /**
     * ✅ EXISTING: Update backup timestamp
     */
    private void updateBackupTimestamp() {
        String timestamp = LocalDateTime.now().toString();
        int currentCount = mPreferences.getInt(PREF_BACKUP_COUNT, 0);

        mPreferences.edit()
                .putString(PREF_LAST_BACKUP_TIME, timestamp)
                .putInt(PREF_BACKUP_COUNT, currentCount + 1)
                .apply();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get application version for backup metadata
     */
    private String getAppVersion() {
        try {
            return mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * ✅ ENHANCED: Cleanup old backup files (both QDue and Calendar)
     */
    private void cleanupOldBackupFiles() {
        try {
            File[] backupFiles = mBackupDirectory.listFiles((dir, name) ->
                    name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

            File[] calendarFiles = mBackupDirectory.listFiles((dir, name) ->
                    name.startsWith(CALENDAR_BACKUP_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

            // Cleanup QDue backup files
            if (backupFiles != null && backupFiles.length > MAX_BACKUP_FILES) {
                java.util.Arrays.sort(backupFiles, (a, b) ->
                        Long.compare(a.lastModified(), b.lastModified()));

                for (int i = 0; i < backupFiles.length - MAX_BACKUP_FILES; i++) {
                    if (backupFiles[i].delete()) {
                        Log.d(TAG, "Deleted old backup file: " + backupFiles[i].getName());
                    }
                }
            }

            // Cleanup Calendar backup files
            if (calendarFiles != null && calendarFiles.length > MAX_BACKUP_FILES) {
                java.util.Arrays.sort(calendarFiles, (a, b) ->
                        Long.compare(a.lastModified(), b.lastModified()));

                for (int i = 0; i < calendarFiles.length - MAX_BACKUP_FILES; i++) {
                    if (calendarFiles[i].delete()) {
                        Log.d(TAG, "Deleted old calendar backup file: " + calendarFiles[i].getName());
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup old backup files", e);
        }
    }

    /**
     * ✅ ENHANCED: Get backup statistics including calendar data
     */
    public Map<String, Object> getBackupStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // QDue backup stats
        stats.put("autoBackupEnabled", isAutoBackupEnabled());
        stats.put("lastBackupTime", getLastBackupTime());
        stats.put("totalBackups", mPreferences.getInt(PREF_BACKUP_COUNT, 0));

        // Calendar backup stats
        stats.put("calendarAutoBackupEnabled", isCalendarAutoBackupEnabled());
        stats.put("lastCalendarBackupTime", getLastCalendarBackupTime());
        stats.put("totalCalendarBackups", mPreferences.getInt(PREF_CALENDAR_BACKUP_COUNT, 0));

        // File system stats
        File[] backupFiles = mBackupDirectory.listFiles((dir, name) ->
                name.endsWith(BACKUP_FILE_EXTENSION));
        stats.put("backupFilesCount", backupFiles != null ? backupFiles.length : 0);

        return stats;
    }

    /**
     * ✅ NEW: Check if calendar functionality is available
     */
    public boolean isCalendarSupported() {
        return mCalendarDatabase != null;
    }

    /**
     * Get comprehensive backup status including calendar
     */
    public BackupStatus getBackupStatus() {
        BackupStatus status = new BackupStatus();

        // Basic QDue backup status
        status.autoBackupEnabled = isAutoBackupEnabled();
        status.lastBackupTime = getLastBackupTime();
        status.totalBackups = mPreferences.getInt(PREF_BACKUP_COUNT, 0);

        // Calendar backup status
        status.calendarAutoBackupEnabled = isCalendarAutoBackupEnabled();
        status.calendarSupported = isCalendarSupported();
        status.lastCalendarBackupTime = getLastCalendarBackupTime();
        status.totalCalendarBackups = mPreferences.getInt(PREF_CALENDAR_BACKUP_COUNT, 0);

        // File system status
        File[] backupFiles = mBackupDirectory.listFiles((dir, name) ->
                name.endsWith(BACKUP_FILE_EXTENSION));
        status.backupFilesCount = backupFiles != null ? backupFiles.length : 0;

        // Directory health check
        status.backupDirectoryExists = mBackupDirectory.exists() && mBackupDirectory.isDirectory();
        status.canWriteToBackupDirectory = mBackupDirectory.canWrite();

        return status;
    }

    /**
     * Shutdown backup manager and cleanup resources
     */
    public void shutdown() {
        try {
            mExecutorService.shutdown();
            Log.d(TAG, "CoreBackupManager shutdown completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during CoreBackupManager shutdown", e);
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * ✅ ENHANCED: BackupStatus - Enhanced backup status with calendar support
     *
     * <p>Comprehensive backup status information including both QDue and Calendar databases,
     * file system health, and backup configuration details.</p>
     */
    public static class BackupStatus {

        // ==================== BASIC BACKUP STATUS ====================

        /**
         * Whether automatic backup is enabled for QDue database
         */
        public boolean autoBackupEnabled;

        /**
         * Last backup timestamp for QDue database
         */
        public String lastBackupTime;

        /**
         * Total number of QDue backups performed
         */
        public int totalBackups;

        // ==================== CALENDAR BACKUP STATUS (NEW) ====================

        /**
         * ✅ NEW: Whether automatic calendar backup is enabled
         */
        public boolean calendarAutoBackupEnabled;

        /**
         * ✅ NEW: Whether calendar database support is available
         */
        public boolean calendarSupported;

        /**
         * ✅ NEW: Last calendar backup timestamp
         */
        public String lastCalendarBackupTime;

        /**
         * ✅ NEW: Total number of calendar backups performed
         */
        public int totalCalendarBackups;

        // ==================== FILE SYSTEM STATUS ====================

        /**
         * Number of backup files currently stored
         */
        public int backupFilesCount;

        /**
         * Whether backup directory exists and is accessible
         */
        public boolean backupDirectoryExists;

        /**
         * Whether app can write to backup directory
         */
        public boolean canWriteToBackupDirectory;

        // ==================== CONSTRUCTORS ====================

        /**
         * Default constructor
         */
        public BackupStatus() {
            this.autoBackupEnabled = false;
            this.lastBackupTime = "Never";
            this.totalBackups = 0;
            this.calendarAutoBackupEnabled = false;
            this.calendarSupported = false;
            this.lastCalendarBackupTime = "Never";
            this.totalCalendarBackups = 0;
            this.backupFilesCount = 0;
            this.backupDirectoryExists = false;
            this.canWriteToBackupDirectory = false;
        }

        // ==================== STATUS METHODS ====================

        /**
         * ✅ NEW: Check if backup system is fully operational
         */
        public boolean isBackupSystemHealthy() {
            return backupDirectoryExists &&
                    canWriteToBackupDirectory &&
                    (autoBackupEnabled || calendarAutoBackupEnabled);
        }

        /**
         * ✅ NEW: Check if calendar backup is functional
         */
        public boolean isCalendarBackupHealthy() {
            return calendarSupported &&
                    calendarAutoBackupEnabled &&
                    backupDirectoryExists &&
                    canWriteToBackupDirectory;
        }

        /**
         * ✅ NEW: Get total backup count (QDue + Calendar)
         */
        public int getTotalAllBackups() {
            return totalBackups + totalCalendarBackups;
        }

        /**
         * ✅ NEW: Get most recent backup time across all databases
         */
        public String getMostRecentBackupTime() {
            if ("Never".equals(lastBackupTime) && "Never".equals(lastCalendarBackupTime)) {
                return "Never";
            }

            if ("Never".equals(lastBackupTime)) {
                return lastCalendarBackupTime;
            }

            if ("Never".equals(lastCalendarBackupTime)) {
                return lastBackupTime;
            }

            // Both have values - return the more recent one (simplified comparison)
            return lastBackupTime; // Could be enhanced with actual date comparison
        }

        /**
         * ✅ ENHANCED: Get comprehensive status summary
         */
        public String getStatusSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Backup System Status:\n");

            // System health
            summary.append("- System Health: ").append(isBackupSystemHealthy() ? "Healthy" : "Issues Detected").append("\n");
            summary.append("- Directory: ").append(backupDirectoryExists ? "OK" : "Missing").append("\n");
            summary.append("- Write Access: ").append(canWriteToBackupDirectory ? "OK" : "Denied").append("\n");

            // QDue backup status
            summary.append("- QDue Auto Backup: ").append(autoBackupEnabled ? "Enabled" : "Disabled").append("\n");
            summary.append("- QDue Last Backup: ").append(lastBackupTime).append("\n");
            summary.append("- QDue Total Backups: ").append(totalBackups).append("\n");

            // Calendar backup status
            summary.append("- Calendar Support: ").append(calendarSupported ? "Available" : "Not Available").append("\n");
            if (calendarSupported) {
                summary.append("- Calendar Auto Backup: ").append(calendarAutoBackupEnabled ? "Enabled" : "Disabled").append("\n");
                summary.append("- Calendar Last Backup: ").append(lastCalendarBackupTime).append("\n");
                summary.append("- Calendar Total Backups: ").append(totalCalendarBackups).append("\n");
            }

            // File statistics
            summary.append("- Total Backup Files: ").append(backupFilesCount).append("\n");
            summary.append("- Combined Backups: ").append(getTotalAllBackups()).append("\n");

            return summary.toString();
        }

        /**
         * Get backup configuration summary
         */
        public String getConfigurationSummary() {
            StringBuilder config = new StringBuilder();
            config.append("Backup Configuration:\n");
            config.append("- QDue Auto Backup: ").append(autoBackupEnabled ? "ON" : "OFF").append("\n");

            if (calendarSupported) {
                config.append("- Calendar Auto Backup: ").append(calendarAutoBackupEnabled ? "ON" : "OFF").append("\n");
                config.append("- Calendar Support: Available\n");
            } else {
                config.append("- Calendar Support: Not Available\n");
            }

            return config.toString();
        }

        // ==================== OBJECT METHODS ====================

        @NonNull
        @Override
        public String toString() {
            return "BackupStatus{" +
                    "autoBackupEnabled=" + autoBackupEnabled +
                    ", lastBackupTime='" + lastBackupTime + '\'' +
                    ", totalBackups=" + totalBackups +
                    ", calendarAutoBackupEnabled=" + calendarAutoBackupEnabled +
                    ", calendarSupported=" + calendarSupported +
                    ", lastCalendarBackupTime='" + lastCalendarBackupTime + '\'' +
                    ", totalCalendarBackups=" + totalCalendarBackups +
                    ", backupFilesCount=" + backupFilesCount +
                    ", systemHealthy=" + isBackupSystemHealthy() +
                    '}';
        }
    }
}