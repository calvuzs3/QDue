package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 * <p>
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== RESTORE CONFIGURATION ====================

/**
 * Restore operation configuration
 */
public class RestoreConfiguration {
    public boolean replaceAll;
    public boolean mergeData;
    public boolean createBackupBeforeRestore;

    // Entity-specific restore settings
    public boolean restoreEvents;
    public boolean restoreUsers;
    public boolean restoreOrganizations;
    public boolean restorePreferences;

    // Conflict resolution
    public ConflictResolution conflictResolution;
    public boolean skipInvalidData;
    public boolean validateDataIntegrity;

    public RestoreConfiguration() {
        // Default settings
        this.replaceAll = false;
        this.mergeData = true;
        this.createBackupBeforeRestore = true;

        // Default entities
        this.restoreEvents = true;
        this.restoreUsers = true;
        this.restoreOrganizations = true;
        this.restorePreferences = false; // Safer default

        // Default conflict resolution
        this.conflictResolution = ConflictResolution.KEEP_NEWER;
        this.skipInvalidData = true;
        this.validateDataIntegrity = true;
    }

    public enum ConflictResolution {
        KEEP_EXISTING,  // Keep existing data, skip conflicting imports
        KEEP_NEWER,     // Keep data with newer timestamp
        REPLACE_ALL,    // Replace existing with imported data
        MERGE_FIELDS    // Merge non-conflicting fields
    }
}
