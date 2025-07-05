package net.calvuz.qdue.core.backup.models;

import java.util.List;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP COMPARISON ====================

/**
 * Backup comparison result for analyzing differences between backups
 */
public class BackupComparisonResult {
    public String backup1Filename;
    public String backup2Filename;
    public long comparisonTimestamp;

    public EntityComparison eventsComparison;
    public EntityComparison usersComparison;
    public EntityComparison organizationComparison;
    public PreferencesComparison preferencesComparison;

    public ComparisonSummary summary;

    public BackupComparisonResult(String backup1, String backup2) {
        this.backup1Filename = backup1;
        this.backup2Filename = backup2;
        this.comparisonTimestamp = System.currentTimeMillis();
        this.summary = new ComparisonSummary();
    }

    public static class EntityComparison {
        public int totalInBackup1;
        public int totalInBackup2;
        public int commonEntities;
        public int onlyInBackup1;
        public int onlyInBackup2;
        public int modifiedEntities;

        public List<String> addedIds;
        public List<String> removedIds;
        public List<String> modifiedIds;

        public EntityComparison() {
            this.addedIds = new java.util.ArrayList<>();
            this.removedIds = new java.util.ArrayList<>();
            this.modifiedIds = new java.util.ArrayList<>();
        }
    }

    public static class PreferencesComparison {
        public int totalInBackup1;
        public int totalInBackup2;
        public int commonPreferences;
        public int onlyInBackup1;
        public int onlyInBackup2;
        public int modifiedPreferences;

        public List<String> addedKeys;
        public List<String> removedKeys;
        public List<String> modifiedKeys;

        public PreferencesComparison() {
            this.addedKeys = new java.util.ArrayList<>();
            this.removedKeys = new java.util.ArrayList<>();
            this.modifiedKeys = new java.util.ArrayList<>();
        }
    }

    public static class ComparisonSummary {
        public boolean areIdentical;
        public int totalDifferences;
        public String overallChangeType; // "MAJOR", "MINOR", "IDENTICAL"
        public List<String> significantChanges;

        public ComparisonSummary() {
            this.significantChanges = new java.util.ArrayList<>();
        }
    }
}
