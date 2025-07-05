package net.calvuz.qdue.core.backup.models;

import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== PREFERENCES BACKUP PACKAGE ====================

/**
 * SharedPreferences backup package
 */
public class PreferencesBackupPackage {
    public String version;
    public String timestamp;
    public int preferencesCount;

    // All SharedPreferences data
    public Map<String, Object> preferences;

    // Preference categories for organized restore
    public Map<String, Map<String, Object>> categorizedPreferences;

    public PreferencesBackupPackage() {}

    public PreferencesBackupPackage(String version, Map<String, Object> preferences) {
        this.version = version;
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.preferences = preferences;
        this.preferencesCount = preferences != null ? preferences.size() : 0;
    }
}
