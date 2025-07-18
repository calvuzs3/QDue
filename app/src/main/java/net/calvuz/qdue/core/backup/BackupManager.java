package net.calvuz.qdue.core.backup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * STEP 3: Automatic Backup Manager for Events System
 * <p>
 * Manages automatic backup of events to local storage with rotation.
 * Features:
 * - Automatic backup on every events modification
 * - Maintains up to 5 backup files with rotation
 * - Consistent with EventPackageJson format
 * - No user intervention required
 * - Background operation with minimal performance impact
 */
public class BackupManager {

    private static final String TAG = "EV_BACKUP_MGR";

    // Configuration constants
    private static final String BACKUP_DIR_NAME = "events_backup";
    private static final String BACKUP_FILE_PREFIX = "events_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final int MAX_BACKUP_FILES = 5;

    // Preferences keys
    private static final String PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
    private static final String PREF_LAST_BACKUP_TIME = "last_backup_time";
    private static final String PREF_TOTAL_BACKUPS = "total_backups_created";

    // Context and dependencies
    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final Gson mGson;
    private final File mBackupDirectory;

    // Backup callbacks
    public interface BackupCallback {
        void onBackupComplete(BackupResult result);
        void onBackupError(String error, Exception exception);
    }

    /**
     * Result class for backup operations
     */
    public static class BackupResult {
        public final boolean success;
        public final String backupFilePath;
        public final long backupSizeBytes;
        public final int eventsCount;
        public final String timestamp;

        public BackupResult(boolean success, String backupFilePath, long backupSizeBytes,
                            int eventsCount, String timestamp) {
            this.success = success;
            this.backupFilePath = backupFilePath;
            this.backupSizeBytes = backupSizeBytes;
            this.eventsCount = eventsCount;
            this.timestamp = timestamp;
        }

        public static BackupResult success(String filePath, long size, int count, String timestamp) {
            return new BackupResult(true, filePath, size, count, timestamp);
        }

        public static BackupResult failure() {
            return new BackupResult(false, null, 0, 0, null);
        }
    }

    /**
     * Constructor
     */
    public BackupManager(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mGson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        // Create backup directory
        mBackupDirectory = createBackupDirectory();

        Log.d(TAG, "BackupManager initialized. Backup directory: " +
                (mBackupDirectory != null ? mBackupDirectory.getAbsolutePath() : "null"));
    }

    /**
     * Create backup directory in app's private storage
     */
    private File createBackupDirectory() {
        File backupDir = new File(mContext.getFilesDir(), BACKUP_DIR_NAME);

        if (!backupDir.exists()) {
            boolean created = backupDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create backup directory: " + backupDir.getAbsolutePath());
                return null;
            }
            Log.d(TAG, "Created backup directory: " + backupDir.getAbsolutePath());
        }

        return backupDir;
    }

    /**
     * Check if automatic backup is enabled
     */
    public boolean isAutoBackupEnabled() {
        return mPreferences.getBoolean(PREF_AUTO_BACKUP_ENABLED, true); // Default enabled
    }

    /**
     * Enable or disable automatic backup
     */
    public void setAutoBackupEnabled(boolean enabled) {
        mPreferences.edit()
                .putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled)
                .apply();

        Log.d(TAG, "Auto backup " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Perform automatic backup of current events
     * Called whenever events are modified (create, update, delete, import)
     */
    public void performAutoBackup(List<LocalEvent> events) {
        if (!isAutoBackupEnabled()) {
            Log.d(TAG, "Auto backup disabled, skipping");
            return;
        }

        if (mBackupDirectory == null) {
            Log.w(TAG, "Backup directory not available, skipping auto backup");
            return;
        }

        // Perform backup in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                BackupResult result = createBackup(events);
                if (result.success) {
                    updateBackupPreferences(result);
                    rotateBackupFiles();
                    Log.i(TAG, "Auto backup completed successfully: " + result.backupFilePath);
                } else {
                    Log.w(TAG, "Auto backup failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during auto backup", e);
            }
        }).start();
    }

    /**
     * Perform manual backup with callback
     */
    public void performManualBackup(List<LocalEvent> events, BackupCallback callback) {
        if (mBackupDirectory == null) {
            callback.onBackupError("Backup directory not available", null);
            return;
        }

        // Perform backup in background thread
        new Thread(() -> {
            try {
                BackupResult result = createBackup(events);
                if (result.success) {
                    updateBackupPreferences(result);
                    rotateBackupFiles();
                    callback.onBackupComplete(result);
                } else {
                    callback.onBackupError("Backup creation failed", null);
                }
            } catch (Exception e) {
                callback.onBackupError("Backup error: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Create backup file from events list
     */
    private BackupResult createBackup(List<LocalEvent> events) throws IOException {
        // Generate timestamp for unique filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
        File backupFile = new File(mBackupDirectory, filename);

        // Create EventPackageJson structure for backup
        EventPackageJson packageJson = createPackageFromEvents(events, timestamp);

        // Write JSON to file
        try (FileWriter writer = new FileWriter(backupFile)) {
            mGson.toJson(packageJson, writer);
        }

        long fileSize = backupFile.length();

        Log.d(TAG, String.format("Backup created: %s (%d bytes, %d events)",
                filename, fileSize, events.size()));

        return BackupResult.success(backupFile.getAbsolutePath(), fileSize,
                events.size(), timestamp);
    }

    /**
     * Create EventPackageJson structure from events list for backup
     */
    private EventPackageJson createPackageFromEvents(List<LocalEvent> events, String timestamp) {
        EventPackageJson packageJson = new EventPackageJson();

        // Create package info
        packageJson.package_info = new EventPackageJson.PackageInfo();
        packageJson.package_info.id = "local_backup_" + timestamp;
        packageJson.package_info.name = "Local Events Backup";
        packageJson.package_info.version = "1.0.0";
        packageJson.package_info.description = "Automatic backup of local events";
        packageJson.package_info.created_date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        packageJson.package_info.author = "Q-DUE Events System";
        packageJson.package_info.contact_email = "";

        // Convert events to JSON format
        packageJson.events = new ArrayList<>();
        for (LocalEvent event : events) {
            EventPackageJson.EventJson eventJson = convertLocalEventToJson(event);
            packageJson.events.add(eventJson);
        }

        return packageJson;
    }

    /**
     * Convert LocalEvent to EventPackageJson.EventJson
     */
    private EventPackageJson.EventJson convertLocalEventToJson(LocalEvent event) {
        EventPackageJson.EventJson eventJson = new EventPackageJson.EventJson();

        eventJson.id = event.getId();
        eventJson.title = event.getTitle();
        eventJson.description = event.getDescription();
        eventJson.location = event.getLocation();
        eventJson.all_day = event.isAllDay();

        // Format dates and times
        if (event.getStartTime() != null) {
            eventJson.start_date = event.getStartTime().toLocalDate().toString();
            if (!event.isAllDay() && event.getStartTime().toLocalTime() != null) {
                eventJson.start_time = event.getStartTime().toLocalTime().toString();
            }
        }

        if (event.getEndTime() != null) {
            eventJson.end_date = event.getEndTime().toLocalDate().toString();
            if (!event.isAllDay() && event.getEndTime().toLocalTime() != null) {
                eventJson.end_time = event.getEndTime().toLocalTime().toString();
            }
        }

        // Event type and priority
        if (event.getEventType() != null) {
            eventJson.event_type = event.getEventType().name();
        }
        if (event.getPriority() != null) {
            eventJson.priority = event.getPriority().name();
        }

        // Custom properties
        if (event.getCustomProperties() != null) {
            eventJson.custom_properties = event.getCustomProperties();
        }

        return eventJson;
    }

    /**
     * Update backup-related preferences
     */
    private void updateBackupPreferences(BackupResult result) {
        int totalBackups = mPreferences.getInt(PREF_TOTAL_BACKUPS, 0) + 1;

        mPreferences.edit()
                .putString(PREF_LAST_BACKUP_TIME, result.timestamp)
                .putInt(PREF_TOTAL_BACKUPS, totalBackups)
                .apply();
    }

    /**
     * Rotate backup files - keep only the most recent MAX_BACKUP_FILES
     */
    private void rotateBackupFiles() {
        if (mBackupDirectory == null || !mBackupDirectory.exists()) {
            return;
        }

        File[] backupFiles = mBackupDirectory.listFiles((dir, name) ->
                name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

        if (backupFiles == null || backupFiles.length <= MAX_BACKUP_FILES) {
            return; // No rotation needed
        }

        // Sort files by last modified time (newest first)
        Arrays.sort(backupFiles, Comparator.comparing(File::lastModified).reversed());

        // Delete older files beyond the limit
        for (int i = MAX_BACKUP_FILES; i < backupFiles.length; i++) {
            File fileToDelete = backupFiles[i];
            boolean deleted = fileToDelete.delete();

            Log.d(TAG, "Rotated backup file: " + fileToDelete.getName() +
                    (deleted ? " (deleted)" : " (delete failed)"));
        }
    }

    /**
     * Get list of available backup files
     */
    public List<BackupInfo> getAvailableBackups() {
        List<BackupInfo> backups = new ArrayList<>();

        if (mBackupDirectory == null || !mBackupDirectory.exists()) {
            return backups;
        }

        File[] backupFiles = mBackupDirectory.listFiles((dir, name) ->
                name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

        if (backupFiles != null) {
            // Sort by last modified time (newest first)
            Arrays.sort(backupFiles, Comparator.comparing(File::lastModified).reversed());

            for (File file : backupFiles) {
                BackupInfo info = new BackupInfo(
                        file.getName(),
                        file.getAbsolutePath(),
                        file.length(),
                        file.lastModified()
                );
                backups.add(info);
            }
        }

        return backups;
    }

    /**
     * Get backup statistics
     */
    public BackupStats getBackupStats() {
        String lastBackupTime = mPreferences.getString(PREF_LAST_BACKUP_TIME, null);
        int totalBackups = mPreferences.getInt(PREF_TOTAL_BACKUPS, 0);
        boolean autoBackupEnabled = isAutoBackupEnabled();
        List<BackupInfo> availableBackups = getAvailableBackups();

        return new BackupStats(lastBackupTime, totalBackups, autoBackupEnabled,
                availableBackups.size());
    }

    /**
     * Clean all backup files (for testing or manual cleanup)
     */
    public void cleanAllBackups() {
        if (mBackupDirectory == null || !mBackupDirectory.exists()) {
            return;
        }

        File[] backupFiles = mBackupDirectory.listFiles();
        if (backupFiles != null) {
            for (File file : backupFiles) {
                boolean deleted = file.delete();
                Log.d(TAG, "Cleaned backup file: " + file.getName() +
                        (deleted ? " (deleted)" : " (delete failed)"));
            }
        }

        // Reset preferences
        mPreferences.edit()
                .remove(PREF_LAST_BACKUP_TIME)
                .putInt(PREF_TOTAL_BACKUPS, 0)
                .apply();
    }

    /**
     * Backup information class
     */
    public static class BackupInfo {
        public final String filename;
        public final String fullPath;
        public final long sizeBytes;
        public final long createdTime;

        public BackupInfo(String filename, String fullPath, long sizeBytes, long createdTime) {
            this.filename = filename;
            this.fullPath = fullPath;
            this.sizeBytes = sizeBytes;
            this.createdTime = createdTime;
        }

        public String getFormattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            } else if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
            }
        }

        public String getFormattedDate() {
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(
                    createdTime / 1000, 0, java.time.ZoneOffset.systemDefault().getRules()
                            .getOffset(java.time.Instant.ofEpochMilli(createdTime)));
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    /**
     * Backup statistics class
     */
    public static class BackupStats {
        public final String lastBackupTime;
        public final int totalBackups;
        public final boolean autoBackupEnabled;
        public final int availableBackups;

        public BackupStats(String lastBackupTime, int totalBackups,
                           boolean autoBackupEnabled, int availableBackups) {
            this.lastBackupTime = lastBackupTime;
            this.totalBackups = totalBackups;
            this.autoBackupEnabled = autoBackupEnabled;
            this.availableBackups = availableBackups;
        }
    }
}