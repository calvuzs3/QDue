package net.calvuz.qdue.core.backup.models;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP STATISTICS ====================

/**
 * Comprehensive backup statistics
 */
public class BackupStatistics {
    public String backupId;
    public String backupType;
    public long timestamp;

    // Size information
    public long totalSize;
    public long compressedSize;
    public double compressionRatio;

    // Entity counts
    public EntityCounts entityCounts;

    // Performance metrics
    public long backupDuration;
    public double backupSpeed; // MB/s
    public long peakMemoryUsage;

    // Quality metrics
    public int validationErrors;
    public int validationWarnings;
    public double dataIntegrityScore;

    public BackupStatistics() {
        this.entityCounts = new EntityCounts();
        this.timestamp = System.currentTimeMillis();
    }

    public static class EntityCounts {
        public int events;
        public int users;
        public int establishments;
        public int macroDepartments;
        public int subDepartments;
        public int preferences;

        public int getTotal() {
            return events + users + establishments + macroDepartments + subDepartments + preferences;
        }
    }
}
