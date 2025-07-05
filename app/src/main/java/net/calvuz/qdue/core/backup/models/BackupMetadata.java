package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP METADATA ====================

/**
 * Backup metadata and statistics
 */
public class BackupMetadata {
    public String deviceInfo;
    public String androidVersion;
    public String appVersion;
    public String backupType;
    public long backupSize;
    public String compressionType;

    // Entity counts
    public int totalEvents;
    public int totalUsers;
    public int totalEstablishments;
    public int totalMacroDepartments;
    public int totalSubDepartments;
    public int totalPreferences;

    // Backup integrity
    public String checksum;
    public boolean isValid;

    // Performance metrics
    public long backupDurationMs;
    public String backupTrigger; // "manual", "auto", "import", etc.

    public BackupMetadata() {
        this.deviceInfo = android.os.Build.MODEL + " (" + android.os.Build.MANUFACTURER + ")";
        this.androidVersion = android.os.Build.VERSION.RELEASE;
        this.isValid = true;
    }
}
