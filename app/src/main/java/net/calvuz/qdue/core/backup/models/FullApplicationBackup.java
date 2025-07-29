package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 * <p>
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== FULL APPLICATION BACKUP ====================

/**
 * Complete application backup containing all entities and preferences
 */
public class FullApplicationBackup {
    public String version;
    public String timestamp;
    public String appVersion;

    // Database entities
    public EntityBackupPackage eventsBackup;
    public EntityBackupPackage usersBackup;
    public EntityBackupPackage establishmentsBackup;
    public EntityBackupPackage macroDepartmentsBackup;
    public EntityBackupPackage subDepartmentsBackup;

    // Application preferences
    public PreferencesBackupPackage preferencesBackup;

    // Metadata
    public BackupMetadata metadata;

    public FullApplicationBackup() {
        this.metadata = new BackupMetadata();
    }
}

