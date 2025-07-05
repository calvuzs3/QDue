package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP HISTORY ====================

/**
 * Backup history entry for tracking and management
 */
public class BackupHistoryEntry {
    public String backupId;
    public String filename;
    public String backupType;
    public long timestamp;
    public long fileSize;

    public BackupTrigger trigger;
    public BackupStatistics statistics;
    public String deviceInfo;
    public String appVersion;

    public boolean isValid;
    public String validationMessage;
    public long lastValidationTime;

    public BackupHistoryEntry(String filename, String backupType) {
        this.backupId = java.util.UUID.randomUUID().toString();
        this.filename = filename;
        this.backupType = backupType;
        this.timestamp = System.currentTimeMillis();
        this.isValid = true;
    }

    public enum BackupTrigger {
        MANUAL("Manual backup"),
        AUTO_CREATE("Auto backup - Create"),
        AUTO_UPDATE("Auto backup - Update"),
        AUTO_DELETE("Auto backup - Delete"),
        AUTO_IMPORT("Auto backup - Import"),
        SCHEDULED("Scheduled backup"),
        SYSTEM("System backup");

        private final String description;

        BackupTrigger(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}