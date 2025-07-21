package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 * <p>
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP CONFIGURATION ====================

/**
 * Backup configuration settings
 */
public class BackupConfiguration {
    public boolean autoBackupEnabled;
    public int maxBackupFiles;
    public String backupDirectory;

    // Trigger settings
    public boolean backupOnCreate;
    public boolean backupOnUpdate;
    public boolean backupOnDelete;
    public boolean backupOnImport;

    // Entity-specific settings
    public boolean includeEvents;
    public boolean includeUsers;
    public boolean includeOrganizations;
    public boolean includePreferences;

    // Compression and encryption
    public boolean compressionEnabled;
    public String compressionType;
    public boolean encryptionEnabled;
    public String encryptionAlgorithm;

    public BackupConfiguration() {
        // Default settings
        this.autoBackupEnabled = true;
        this.maxBackupFiles = 10;
        this.backupDirectory = "qdue_core_backup";

        // Default triggers
        this.backupOnCreate = true;
        this.backupOnUpdate = true;
        this.backupOnDelete = true;
        this.backupOnImport = true;

        // Default entities
        this.includeEvents = true;
        this.includeUsers = true;
        this.includeOrganizations = true;
        this.includePreferences = true;

        // Default compression/encryption
        this.compressionEnabled = false;
        this.compressionType = "none";
        this.encryptionEnabled = false;
        this.encryptionAlgorithm = "none";
    }
}
