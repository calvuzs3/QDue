package net.calvuz.qdue.core.backup.models;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;

import java.util.List;
import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 *
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

