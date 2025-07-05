package net.calvuz.qdue.core.backup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.calvuz.qdue.core.backup.models.EntityBackupPackage;
import net.calvuz.qdue.core.backup.models.FullApplicationBackup;
import net.calvuz.qdue.core.backup.services.DatabaseBackupService;
import net.calvuz.qdue.core.backup.services.PreferencesBackupService;
import net.calvuz.qdue.services.models.OperationResult;
import net.calvuz.qdue.utils.Log;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * STEP 1: Core Backup Manager - Unified Backup System for ALL Entities
 *
 * Extends the existing events-only backup system to handle:
 * - Complete database backup (all entities)
 * - SharedPreferences backup
 * - Application-wide backup coordination
 * - Unified restore operations
 * - Cross-entity backup consistency
 *
 * This replaces the existing BackupManager and provides a foundation
 * for the complete Core Backup System.
 */
public class CoreBackupManager {

    private static final String TAG = "CoreBackupManager";

    // Backup configuration
    private static final String BACKUP_DIR_NAME = "qdue_core_backup";
    private static final String BACKUP_FILE_PREFIX = "qdue_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_BACKUP_FILES = 10; // Increased from 5 for core backups

    // Preferences keys
    private static final String PREF_AUTO_BACKUP_ENABLED = "core_auto_backup_enabled";
    private static final String PREF_LAST_BACKUP_TIME = "core_last_backup_time";
    private static final String PREF_BACKUP_COUNT = "core_backup_count";
    private static final String PREF_BACKUP_ON_CREATE = "core_backup_on_create";
    private static final String PREF_BACKUP_ON_UPDATE = "core_backup_on_update";
    private static final String PREF_BACKUP_ON_DELETE = "core_backup_on_delete";
    private static final String PREF_BACKUP_ON_IMPORT = "core_backup_on_import";

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final DatabaseBackupService mDatabaseBackupService;
    private final PreferencesBackupService mPreferencesBackupService;
    private final ExecutorService mExecutorService;
    private final Gson mGson;
    private final File mBackupDirectory;

    private static volatile CoreBackupManager INSTANCE;

    // ==================== CONSTRUCTOR AND SINGLETON ====================

    private CoreBackupManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.mDatabaseBackupService = new DatabaseBackupService(mContext);
        this.mPreferencesBackupService = new PreferencesBackupService(mContext);
        this.mExecutorService = Executors.newFixedThreadPool(2);
        this.mGson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        // Initialize backup directory
        this.mBackupDirectory = new File(mContext.getFilesDir(), BACKUP_DIR_NAME);
        if (!mBackupDirectory.exists()) {
            boolean created = mBackupDirectory.mkdirs();
            Log.d(TAG, "Backup directory created: " + created);
        }

        Log.d(TAG, "CoreBackupManager initialized - Directory: " + mBackupDirectory.getAbsolutePath());
    }

    /**
     * Get singleton instance
     */
    public static CoreBackupManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CoreBackupManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CoreBackupManager(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== FULL APPLICATION BACKUP ====================

    /**
     * Perform complete application backup including all entities and preferences
     */
    public CompletableFuture<OperationResult<String>> performFullApplicationBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting full application backup...");

                // Create backup package
                FullApplicationBackup backupPackage = createFullBackupPackage();

                // Generate backup filename
                String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
                String filename = BACKUP_FILE_PREFIX + "full_" + timestamp + BACKUP_FILE_EXTENSION;
                File backupFile = new File(mBackupDirectory, filename);

                // Write backup to file
                try (FileWriter writer = new FileWriter(backupFile)) {
                    mGson.toJson(backupPackage, writer);
                }

                // Update preferences
                updateBackupPreferences();

                // Perform rotation
                performBackupRotation();

                Log.d(TAG, "Full application backup completed: " + filename);
                return OperationResult.success(filename,
                        "Full application backup completed successfully",
                        OperationResult.OperationType.BACKUP);

            } catch (Exception e) {
                Log.e(TAG, "Failed to perform full application backup", e);
                return OperationResult.failure(e, OperationResult.OperationType.BACKUP);
            }
        }, mExecutorService);
    }

    /**
     * Perform entity-specific backup
     */
    public CompletableFuture<OperationResult<String>> performEntityBackup(String entityType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting " + entityType + " backup...");

                // Create entity-specific backup
                EntityBackupPackage entityBackup = mDatabaseBackupService.createEntityBackup(entityType);

                // Generate backup filename
                String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
                String filename = BACKUP_FILE_PREFIX + entityType + "_" + timestamp + BACKUP_FILE_EXTENSION;
                File backupFile = new File(mBackupDirectory, filename);

                // Write backup to file
                try (FileWriter writer = new FileWriter(backupFile)) {
                    mGson.toJson(entityBackup, writer);
                }

                Log.d(TAG, entityType + " backup completed: " + filename);
                return OperationResult.success(filename,
                        entityType + " backup completed successfully",
                        OperationResult.OperationType.BACKUP);

            } catch (Exception e) {
                Log.e(TAG, "Failed to perform " + entityType + " backup", e);
                return OperationResult.failure(e, OperationResult.OperationType.BACKUP);
            }
        }, mExecutorService);
    }

    /**
     * Automatic backup triggered by entity changes
     */
    public CompletableFuture<OperationResult<String>> performAutoBackup(String entityType, String operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if auto backup is enabled
                if (!isAutoBackupEnabled()) {
                    return OperationResult.success("Auto backup disabled",
                            OperationResult.OperationType.BACKUP);
                }

                // Check if backup is enabled for this operation type
                if (!isBackupEnabledForOperation(operation)) {
                    return OperationResult.success("Auto backup disabled for " + operation,
                            OperationResult.OperationType.BACKUP);
                }

                Log.d(TAG, "Auto backup triggered: " + entityType + " - " + operation);

                // For critical operations, perform full backup
                if (isCriticalOperation(operation)) {
                    return performFullApplicationBackup().get();
                } else {
                    return performEntityBackup(entityType).get();
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to perform auto backup", e);
                return OperationResult.failure(e, OperationResult.OperationType.BACKUP);
            }
        }, mExecutorService);
    }

    // ==================== RESTORE OPERATIONS ====================

    /**
     * Get available backup files
     */
    public List<BackupFileInfo> getAvailableBackups() {
        List<BackupFileInfo> backups = new ArrayList<>();

        if (!mBackupDirectory.exists()) {
            return backups;
        }

        File[] files = mBackupDirectory.listFiles((dir, name) ->
                name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

        if (files != null) {
            for (File file : files) {
                try {
                    BackupFileInfo info = parseBackupFile(file);
                    if (info != null) {
                        backups.add(info);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse backup file: " + file.getName(), e);
                }
            }
        }

        // Sort by creation time (newest first)
        backups.sort((a, b) -> Long.compare(b.creationTime, a.creationTime));

        return backups;
    }

    /**
     * Restore from backup file
     */
    public CompletableFuture<OperationResult<RestoreResult>> restoreFromBackup(String filename, boolean replaceAll) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting restore from: " + filename);

                File backupFile = new File(mBackupDirectory, filename);
                if (!backupFile.exists()) {
                    return OperationResult.failure("Backup file not found: " + filename,
                            OperationResult.OperationType.RESTORE);
                }

                // Determine backup type and restore accordingly
                BackupFileInfo info = parseBackupFile(backupFile);
                if (info == null) {
                    return OperationResult.failure("Invalid backup file format",
                            OperationResult.OperationType.RESTORE);
                }

                RestoreResult result;
                if (info.backupType.equals("full")) {
                    result = restoreFullApplicationBackup(backupFile, replaceAll);
                } else {
                    result = restoreEntityBackup(backupFile, info.entityType, replaceAll);
                }

                Log.d(TAG, "Restore completed successfully");
                return OperationResult.success(result, "Restore completed successfully",
                        OperationResult.OperationType.RESTORE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to restore from backup", e);
                return OperationResult.failure(e, OperationResult.OperationType.RESTORE);
            }
        }, mExecutorService);
    }

    // ==================== BACKUP CONFIGURATION ====================

    /**
     * Enable/disable auto backup
     */
    public void setAutoBackupEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled).apply();
        Log.d(TAG, "Auto backup " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if auto backup is enabled
     */
    public boolean isAutoBackupEnabled() {
        return mPreferences.getBoolean(PREF_AUTO_BACKUP_ENABLED, true);
    }

    /**
     * Configure backup triggers for different operations
     */
    public void setBackupTriggers(boolean onCreate, boolean onUpdate, boolean onDelete, boolean onImport) {
        mPreferences.edit()
                .putBoolean(PREF_BACKUP_ON_CREATE, onCreate)
                .putBoolean(PREF_BACKUP_ON_UPDATE, onUpdate)
                .putBoolean(PREF_BACKUP_ON_DELETE, onDelete)
                .putBoolean(PREF_BACKUP_ON_IMPORT, onImport)
                .apply();
        Log.d(TAG, "Backup triggers updated");
    }

    /**
     * Get backup status information
     */
    public BackupStatus getBackupStatus() {
        String lastBackupTime = mPreferences.getString(PREF_LAST_BACKUP_TIME, "Never");
        int backupCount = mPreferences.getInt(PREF_BACKUP_COUNT, 0);
        List<BackupFileInfo> availableBackups = getAvailableBackups();

        return new BackupStatus(
                isAutoBackupEnabled(),
                lastBackupTime,
                backupCount,
                availableBackups.size(),
                mBackupDirectory.getAbsolutePath(),
                calculateTotalBackupSize()
        );
    }

    /**
     * Clean old backup files
     */
    public CompletableFuture<OperationResult<Integer>> cleanOldBackups() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<BackupFileInfo> backups = getAvailableBackups();
                int deletedCount = 0;

                // Keep only recent backups beyond the limit
                if (backups.size() > MAX_BACKUP_FILES) {
                    for (int i = MAX_BACKUP_FILES; i < backups.size(); i++) {
                        File file = new File(mBackupDirectory, backups.get(i).filename);
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }

                Log.d(TAG, "Cleaned " + deletedCount + " old backup files");
                return OperationResult.success(deletedCount,
                        "Cleaned " + deletedCount + " old backup files",
                        OperationResult.OperationType.CLEANUP);

            } catch (Exception e) {
                Log.e(TAG, "Failed to clean old backups", e);
                return OperationResult.failure(e, OperationResult.OperationType.CLEANUP);
            }
        }, mExecutorService);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Create full application backup package
     */
    private FullApplicationBackup createFullBackupPackage() {
        FullApplicationBackup backup = new FullApplicationBackup();
        backup.version = "1.0";
        backup.timestamp = LocalDateTime.now().toString();
        backup.appVersion = getAppVersion();

        // Backup all database entities
        backup.eventsBackup = mDatabaseBackupService.createEntityBackup("events");
        backup.usersBackup = mDatabaseBackupService.createEntityBackup("users");
        backup.establishmentsBackup = mDatabaseBackupService.createEntityBackup("establishments");
        backup.macroDepartmentsBackup = mDatabaseBackupService.createEntityBackup("macro_departments");
        backup.subDepartmentsBackup = mDatabaseBackupService.createEntityBackup("sub_departments");

        // Backup preferences
        backup.preferencesBackup = mPreferencesBackupService.createPreferencesBackup();

        return backup;
    }

    /**
     * Restore full application backup
     */
    private RestoreResult restoreFullApplicationBackup(File backupFile, boolean replaceAll) throws Exception {
        // Parse backup file
        FullApplicationBackup backup = mGson.fromJson(
                new java.io.FileReader(backupFile), FullApplicationBackup.class);

        RestoreResult result = new RestoreResult();

        // Restore database entities
        if (backup.eventsBackup != null) {
            result.eventsRestored = mDatabaseBackupService.restoreEntityBackup(backup.eventsBackup, replaceAll);
        }
        if (backup.usersBackup != null) {
            result.usersRestored = mDatabaseBackupService.restoreEntityBackup(backup.usersBackup, replaceAll);
        }
        if (backup.establishmentsBackup != null) {
            result.establishmentsRestored = mDatabaseBackupService.restoreEntityBackup(backup.establishmentsBackup, replaceAll);
        }
        if (backup.macroDepartmentsBackup != null) {
            result.macroDepartmentsRestored = mDatabaseBackupService.restoreEntityBackup(backup.macroDepartmentsBackup, replaceAll);
        }
        if (backup.subDepartmentsBackup != null) {
            result.subDepartmentsRestored = mDatabaseBackupService.restoreEntityBackup(backup.subDepartmentsBackup, replaceAll);
        }

        // Restore preferences
        if (backup.preferencesBackup != null) {
            result.preferencesRestored = mPreferencesBackupService.restorePreferencesBackup(backup.preferencesBackup);
        }

        return result;
    }

    /**
     * Restore entity-specific backup
     */
    private RestoreResult restoreEntityBackup(File backupFile, String entityType, boolean replaceAll) throws Exception {
        EntityBackupPackage backup = mGson.fromJson(
                new java.io.FileReader(backupFile), EntityBackupPackage.class);

        RestoreResult result = new RestoreResult();
        int restored = mDatabaseBackupService.restoreEntityBackup(backup, replaceAll);

        // Set appropriate field based on entity type
        switch (entityType) {
            case "events":
                result.eventsRestored = restored;
                break;
            case "users":
                result.usersRestored = restored;
                break;
            case "establishments":
                result.establishmentsRestored = restored;
                break;
            case "macro_departments":
                result.macroDepartmentsRestored = restored;
                break;
            case "sub_departments":
                result.subDepartmentsRestored = restored;
                break;
        }

        return result;
    }

    /**
     * Parse backup file to extract information
     */
    private BackupFileInfo parseBackupFile(File file) {
        String filename = file.getName();

        // Parse filename pattern: qdue_backup_{type}_{timestamp}.json
        if (!filename.startsWith(BACKUP_FILE_PREFIX) || !filename.endsWith(BACKUP_FILE_EXTENSION)) {
            return null;
        }

        String nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
        String[] parts = nameWithoutExtension.split("_");

        if (parts.length < 3) {
            return null;
        }

        String backupType = parts[2]; // "full", "events", "users", etc.
        String entityType = backupType.equals("full") ? null : backupType;

        return new BackupFileInfo(
                filename,
                backupType,
                entityType,
                file.lastModified(),
                file.length()
        );
    }

    /**
     * Check if backup is enabled for specific operation
     */
    private boolean isBackupEnabledForOperation(String operation) {
        switch (operation) {
            case "create":
                return mPreferences.getBoolean(PREF_BACKUP_ON_CREATE, true);
            case "update":
                return mPreferences.getBoolean(PREF_BACKUP_ON_UPDATE, true);
            case "delete":
                return mPreferences.getBoolean(PREF_BACKUP_ON_DELETE, true);
            case "import":
                return mPreferences.getBoolean(PREF_BACKUP_ON_IMPORT, true);
            default:
                return true;
        }
    }

    /**
     * Check if operation is critical (requires full backup)
     */
    private boolean isCriticalOperation(String operation) {
        return operation.equals("import") || operation.equals("bulk_delete") || operation.equals("clear_all");
    }

    /**
     * Update backup preferences after successful backup
     */
    private void updateBackupPreferences() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        int currentCount = mPreferences.getInt(PREF_BACKUP_COUNT, 0);

        mPreferences.edit()
                .putString(PREF_LAST_BACKUP_TIME, currentTime)
                .putInt(PREF_BACKUP_COUNT, currentCount + 1)
                .apply();
    }

    /**
     * Perform backup file rotation
     */
    private void performBackupRotation() {
        List<BackupFileInfo> backups = getAvailableBackups();

        if (backups.size() > MAX_BACKUP_FILES) {
            // Delete oldest backups
            for (int i = MAX_BACKUP_FILES; i < backups.size(); i++) {
                File oldFile = new File(mBackupDirectory, backups.get(i).filename);
                boolean deleted = oldFile.delete();
                Log.d(TAG, "Backup rotation: deleted " + backups.get(i).filename + " = " + deleted);
            }
        }
    }

    /**
     * Calculate total backup size
     */
    private long calculateTotalBackupSize() {
        long totalSize = 0;

        if (mBackupDirectory.exists()) {
            File[] files = mBackupDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize;
    }

    /**
     * Get app version
     */
    private String getAppVersion() {
        try {
            return mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Backup file information
     */
    public static class BackupFileInfo {
        public final String filename;
        public final String backupType;
        public final String entityType;
        public final long creationTime;
        public final long fileSize;

        public BackupFileInfo(String filename, String backupType, String entityType,
                              long creationTime, long fileSize) {
            this.filename = filename;
            this.backupType = backupType;
            this.entityType = entityType;
            this.creationTime = creationTime;
            this.fileSize = fileSize;
        }
    }

    /**
     * Backup status information
     */
    public static class BackupStatus {
        public final boolean autoBackupEnabled;
        public final String lastBackupTime;
        public final int totalBackupCount;
        public final int availableBackupCount;
        public final String backupDirectory;
        public final long totalBackupSize;

        public BackupStatus(boolean autoBackupEnabled, String lastBackupTime, int totalBackupCount,
                            int availableBackupCount, String backupDirectory, long totalBackupSize) {
            this.autoBackupEnabled = autoBackupEnabled;
            this.lastBackupTime = lastBackupTime;
            this.totalBackupCount = totalBackupCount;
            this.availableBackupCount = availableBackupCount;
            this.backupDirectory = backupDirectory;
            this.totalBackupSize = totalBackupSize;
        }
    }

    /**
     * Restore operation result
     */
    public static class RestoreResult {
        public int eventsRestored = 0;
        public int usersRestored = 0;
        public int establishmentsRestored = 0;
        public int macroDepartmentsRestored = 0;
        public int subDepartmentsRestored = 0;
        public int preferencesRestored = 0;

        public int getTotalRestored() {
            return eventsRestored + usersRestored + establishmentsRestored +
                    macroDepartmentsRestored + subDepartmentsRestored + preferencesRestored;
        }
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}