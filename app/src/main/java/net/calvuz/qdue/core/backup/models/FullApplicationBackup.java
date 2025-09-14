package net.calvuz.qdue.core.backup.models;

import java.util.Map;

/**
 * EXTENDED: FullApplicationBackup - Complete application backup with Calendar support
 *
 * <p>Enhanced version of FullApplicationBackup supporting both legacy QDueDatabase entities
 * and new CalendarDatabase entities in a unified backup format.</p>
 *
 * <h3>Enhanced Features:</h3>
 * <ul>
 *   <li><strong>Dual Database Support</strong>: Backs up both QDue and Calendar databases</li>
 *   <li><strong>Migration Ready</strong>: Supports transition from QDue to Calendar database</li>
 *   <li><strong>Backward Compatibility</strong>: Maintains compatibility with existing backups</li>
 *   <li><strong>Version Tracking</strong>: Enhanced versioning for different backup formats</li>
 * </ul>
 *
 * <h3>Backup Structure:</h3>
 * <pre>
 * FullApplicationBackup
 * ├── QDue Database Entities (Legacy)
 * │   ├── eventsBackup
 * │   ├── usersBackup
 * │   ├── establishmentsBackup
 * │   ├── macroDepartmentsBackup
 * │   └── subDepartmentsBackup
 * ├── Calendar Database Entities (NEW)
 * │   └── calendarBackup
 * ├── Application Preferences
 * │   └── preferencesBackup
 * └── Metadata
 *     └── metadata (enhanced with calendar info)
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Calendar Integration Support
 * @since Clean Architecture Phase 2
 */
public class FullApplicationBackup {

    // ==================== BACKUP METADATA ====================

    /**
     * Backup format version - determines backup structure
     * Version 1.0: QDue database only
     * Version 2.0: QDue + Calendar databases (current)
     */
    public String version;

    /**
     * Backup creation timestamp
     */
    public String timestamp;

    /**
     * Application version when backup was created
     */
    public String appVersion;

    // ==================== QDUE DATABASE ENTITIES (LEGACY) ====================

    /**
     * Legacy events backup from QDueDatabase
     * Contains EventEntityGoogle entities and related data
     */
    public EntityBackupPackage eventsBackup;

    /**
     * Users backup from QDueDatabase
     * Contains User entities and profile data
     */
    public EntityBackupPackage usersBackup;

    /**
     * Establishments backup from QDueDatabase
     * Contains organizational structure data
     */
    public EntityBackupPackage establishmentsBackup;

    /**
     * Macro departments backup from QDueDatabase
     * Contains department hierarchy data
     */
    public EntityBackupPackage macroDepartmentsBackup;

    /**
     * Sub departments backup from QDueDatabase
     * Contains detailed department structure
     */
    public EntityBackupPackage subDepartmentsBackup;

    // ==================== CALENDAR DATABASE ENTITIES (NEW) ====================

    /**
     * ✅ NEW: Calendar backup from CalendarDatabase
     * Contains all calendar-related entities:
     * - ShiftEntity (shift type templates)
     * - TeamEntity (work team definitions)
     * - RecurrenceRuleEntity (RRULE patterns)
     * - ShiftExceptionEntity (schedule exceptions)
     * - UserScheduleAssignmentEntity (user-team assignments)
     */
    public EntityBackupPackage calendarBackup;

    // ==================== APPLICATION PREFERENCES ====================

    /**
     * Application preferences and settings backup
     * Contains user preferences, app configuration, etc.
     */
    public PreferencesBackupPackage preferencesBackup;

    // ==================== BACKUP METADATA ====================

    /**
     * Enhanced backup metadata with calendar information
     */
    public BackupMetadata metadata;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor - initializes enhanced metadata
     */
    public FullApplicationBackup() {
        this.metadata = new BackupMetadata();
        this.version = "2.0"; // Default to current version with calendar support
    }

    /**
     * Constructor with version specification
     *
     * @param version Backup format version
     */
    public FullApplicationBackup(String version) {
        this.metadata = new BackupMetadata();
        this.version = version;
    }

    // ==================== ENHANCED METADATA CLASS ====================

    /**
     * Enhanced BackupMetadata with calendar-specific information
     */
    public static class BackupMetadata {

        // ==================== BASIC METADATA ====================

        /**
         * Total number of entities backed up across all databases
         */
        public int totalEntities;

        /**
         * Size of backup data in bytes
         */
        public long backupSizeBytes;

        /**
         * Backup creation duration in milliseconds
         */
        public long backupDurationMs;

        // ==================== DATABASE-SPECIFIC METADATA ====================

        /**
         * QDue database backup statistics
         */
        public DatabaseBackupStats qDueDatabaseStats;

        /**
         * ✅ NEW: Calendar database backup statistics
         */
        public DatabaseBackupStats calendarDatabaseStats;

        // ==================== BACKUP VALIDATION ====================

        /**
         * Backup validation checksum for integrity verification
         */
        public String validationChecksum;

        /**
         * ✅ NEW: Calendar backup validation status
         */
        public boolean calendarBackupValid;

        /**
         * QDue backup validation status
         */
        public boolean qDueBackupValid;

        // ==================== COMPATIBILITY INFORMATION ====================

        /**
         * ✅ NEW: Indicates if backup contains calendar data
         */
        public boolean hasCalendarData;

        /**
         * Indicates if backup contains legacy QDue data
         */
        public boolean hasQDueData;

        /**
         * ✅ NEW: Calendar database version when backup was created
         */
        public int calendarDatabaseVersion;

        /**
         * QDue database version when backup was created
         */
        public int qDueDatabaseVersion;

        // ==================== MIGRATION SUPPORT ====================

        /**
         * ✅ NEW: Indicates if this backup can be used for QDue→Calendar migration
         */
        public boolean supportsMigration;

        /**
         * ✅ NEW: Migration compatibility notes
         */
        public String migrationNotes;

        // ==================== CONSTRUCTOR ====================

        public BackupMetadata() {
            this.totalEntities = 0;
            this.backupSizeBytes = 0L;
            this.backupDurationMs = 0L;
            this.qDueDatabaseStats = new DatabaseBackupStats();
            this.calendarDatabaseStats = new DatabaseBackupStats();
            this.validationChecksum = "";
            this.calendarBackupValid = false;
            this.qDueBackupValid = false;
            this.hasCalendarData = false;
            this.hasQDueData = false;
            this.calendarDatabaseVersion = 0;
            this.qDueDatabaseVersion = 0;
            this.supportsMigration = false;
            this.migrationNotes = "";
        }
    }

    // ==================== DATABASE BACKUP STATISTICS ====================

    /**
     * Database-specific backup statistics
     */
    public static class DatabaseBackupStats {

        /**
         * Number of entities backed up from this database
         */
        public int entitiesCount;

        /**
         * Size of this database backup in bytes
         */
        public long databaseBackupSize;

        /**
         * Time taken to backup this database in milliseconds
         */
        public long databaseBackupDuration;

        /**
         * Database backup success status
         */
        public boolean backupSuccessful;

        /**
         * ✅ NEW: Entity type breakdown for detailed statistics
         */
        public Map<String, Integer> entityTypeBreakdown;

        /**
         * Error messages if backup failed
         */
        public String errorMessage;

        public DatabaseBackupStats() {
            this.entitiesCount = 0;
            this.databaseBackupSize = 0L;
            this.databaseBackupDuration = 0L;
            this.backupSuccessful = false;
            this.entityTypeBreakdown = new java.util.HashMap<>();
            this.errorMessage = "";
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * ✅ NEW: Check if backup contains calendar data
     *
     * @return true if calendar backup is present and valid
     */
    public boolean hasValidCalendarData() {
        return calendarBackup != null &&
                calendarBackup.entities != null &&
                !calendarBackup.entities.isEmpty() &&
                metadata != null &&
                metadata.calendarBackupValid;
    }

    /**
     * Check if backup contains QDue data
     *
     * @return true if any QDue backup packages are present
     */
    public boolean hasValidQDueData() {
        return (eventsBackup != null && eventsBackup.entities != null && !eventsBackup.entities.isEmpty()) ||
                (usersBackup != null && usersBackup.entities != null && !usersBackup.entities.isEmpty()) ||
                (establishmentsBackup != null && establishmentsBackup.entities != null && !establishmentsBackup.entities.isEmpty()) ||
                (macroDepartmentsBackup != null && macroDepartmentsBackup.entities != null && !macroDepartmentsBackup.entities.isEmpty()) ||
                (subDepartmentsBackup != null && subDepartmentsBackup.entities != null && !subDepartmentsBackup.entities.isEmpty());
    }

    /**
     * ✅ NEW: Get total number of calendar entities
     *
     * @return count of all calendar entities in backup
     */
    public int getCalendarEntitiesCount() {
        if (calendarBackup == null || calendarBackup.entities == null) {
            return 0;
        }
        return calendarBackup.entities.size();
    }

    /**
     * Get total number of QDue entities
     *
     * @return count of all QDue entities in backup
     */
    public int getQDueEntitiesCount() {
        int count = 0;

        if (eventsBackup != null && eventsBackup.entities != null) {
            count += eventsBackup.entities.size();
        }
        if (usersBackup != null && usersBackup.entities != null) {
            count += usersBackup.entities.size();
        }
        if (establishmentsBackup != null && establishmentsBackup.entities != null) {
            count += establishmentsBackup.entities.size();
        }
        if (macroDepartmentsBackup != null && macroDepartmentsBackup.entities != null) {
            count += macroDepartmentsBackup.entities.size();
        }
        if (subDepartmentsBackup != null && subDepartmentsBackup.entities != null) {
            count += subDepartmentsBackup.entities.size();
        }

        return count;
    }

    /**
     * ✅ NEW: Get total number of all entities (QDue + Calendar)
     *
     * @return total count of all entities in backup
     */
    public int getTotalEntitiesCount() {
        return getQDueEntitiesCount() + getCalendarEntitiesCount();
    }

    /**
     * ✅ NEW: Check if backup is compatible with current application version
     *
     * @param currentVersion Current application version
     * @return true if backup is compatible
     */
    public boolean isCompatible(String currentVersion) {
        // Version 2.0 backups are compatible with calendar-enabled versions
        // Version 1.0 backups are compatible but without calendar data
        return "2.0".equals(version) || "1.0".equals(version);
    }

    /**
     * ✅ NEW: Get backup summary for display purposes
     *
     * @return human-readable backup summary
     */
    public String getBackupSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Backup v").append(version).append(" (").append(timestamp).append(")\n");
        summary.append("QDue entities: ").append(getQDueEntitiesCount()).append("\n");

        if (hasValidCalendarData()) {
            summary.append("Calendar entities: ").append(getCalendarEntitiesCount()).append("\n");
        }

        summary.append("Total: ").append(getTotalEntitiesCount()).append(" entities");

        if (metadata != null && metadata.backupSizeBytes > 0) {
            summary.append(" (").append(formatBytes(metadata.backupSizeBytes)).append(")");
        }

        return summary.toString();
    }

    /**
     * Format bytes for human-readable display
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}