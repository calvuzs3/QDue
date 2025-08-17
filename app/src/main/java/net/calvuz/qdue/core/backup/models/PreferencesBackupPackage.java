package net.calvuz.qdue.core.backup.models;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * PreferencesBackupPackage - Data structure for application preferences backup
 *
 * <p>Contains all SharedPreferences data in a format suitable for JSON serialization
 * and cross-platform backup/restore operations.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Type Safety</strong>: Handles all standard preference types</li>
 *   <li><strong>JSON Compatible</strong>: All data types can be serialized to JSON</li>
 *   <li><strong>Version Tracking</strong>: Supports backup format versioning</li>
 *   <li><strong>Metadata Support</strong>: Additional backup information</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Preferences Backup Package
 * @since Clean Architecture Phase 2
 */
public class PreferencesBackupPackage {

    // ==================== BACKUP METADATA ====================

    /**
     * Backup format version for compatibility checking
     */
    public String version;

    /**
     * Timestamp when backup was created
     */
    public String timestamp;

    // ==================== PREFERENCES DATA ====================

    /**
     * All application preferences as key-value pairs
     * Contains Boolean, String, Integer, Long, Float values
     */
    public Map<String, Object> preferences;

    /**
     * Total number of preferences in backup
     */
    public int preferenceCount;

    // ==================== ADDITIONAL METADATA ====================

    /**
     * Additional metadata about the backup
     */
    public Map<String, Object> metadata;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor - initializes empty backup package
     */
    public PreferencesBackupPackage() {
        this.preferences = new HashMap<>();
        this.metadata = new HashMap<>();
        this.preferenceCount = 0;
        this.version = "1.0";
    }

    /**
     * Constructor with preferences data
     *
     * @param preferences Map of preference key-value pairs
     */
    public PreferencesBackupPackage(Map<String, Object> preferences) {
        this.preferences = preferences != null ? new HashMap<>(preferences) : new HashMap<>();
        this.metadata = new HashMap<>();
        this.preferenceCount = this.preferences.size();
        this.version = "1.0";
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get the number of preferences in this backup
     *
     * @return count of preferences
     */
    public int getPreferenceCount() {
        return preferenceCount;
    }

    /**
     * Update preference count to match actual preferences size
     */
    public void updatePreferenceCount() {
        this.preferenceCount = preferences != null ? preferences.size() : 0;
    }

    /**
     * Check if backup contains any preferences
     *
     * @return true if backup has preferences
     */
    public boolean hasPreferences() {
        return preferences != null && !preferences.isEmpty();
    }

    /**
     * Get specific preference value by key
     *
     * @param key preference key
     * @return preference value or null if not found
     */
    public Object getPreference(String key) {
        return preferences != null ? preferences.get(key) : null;
    }

    /**
     * Add or update a preference in the backup
     *
     * @param key preference key
     * @param value preference value
     */
    public void putPreference(String key, Object value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
        updatePreferenceCount();
    }

    /**
     * Remove a preference from the backup
     *
     * @param key preference key to remove
     * @return the previous value associated with key, or null
     */
    public Object removePreference(String key) {
        if (preferences == null) {
            return null;
        }
        Object removed = preferences.remove(key);
        updatePreferenceCount();
        return removed;
    }

    /**
     * Get a summary of the backup contents
     *
     * @return formatted summary string
     */
    public String getBackupSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("PreferencesBackup v").append(version);
        summary.append(" (").append(timestamp).append(")\n");
        summary.append("Preferences: ").append(getPreferenceCount());

        if (metadata != null && !metadata.isEmpty()) {
            summary.append("\nMetadata: ").append(metadata.size()).append(" items");
        }

        return summary.toString();
    }

    /**
     * Validate backup package integrity
     *
     * @return true if backup is valid
     */
    public boolean isValid() {
        // Check basic structure
        if (version == null || timestamp == null) {
            return false;
        }

        // Check preferences consistency
        if (preferences == null) {
            return preferenceCount == 0;
        }

        // Check count matches actual size
        return preferenceCount == preferences.size();
    }

    /**
     * Get backup statistics for monitoring
     *
     * @return map containing backup statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("version", version);
        stats.put("timestamp", timestamp);
        stats.put("preferenceCount", preferenceCount);
        stats.put("hasMetadata", metadata != null && !metadata.isEmpty());
        stats.put("valid", isValid());

        // Count by preference type if preferences exist
        if (preferences != null && !preferences.isEmpty()) {
            Map<String, Integer> typeCounts = new HashMap<>();

            for (Object value : preferences.values()) {
                String type = value != null ? value.getClass().getSimpleName() : "null";
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }

            stats.put("typeBreakdown", typeCounts);
        }

        return stats;
    }

    // ==================== OBJECT METHODS ====================

    @NonNull
    @Override
    public String toString() {
        return "PreferencesBackupPackage{" +
                "version='" + version + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", preferenceCount=" + preferenceCount +
                ", hasPreferences=" + hasPreferences() +
                ", valid=" + isValid() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PreferencesBackupPackage that = (PreferencesBackupPackage) o;

        if (preferenceCount != that.preferenceCount) return false;
        if (!Objects.equals( version, that.version )) return false;
        if (!Objects.equals( timestamp, that.timestamp )) return false;
        return Objects.equals( preferences, that.preferences );
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (preferences != null ? preferences.hashCode() : 0);
        result = 31 * result + preferenceCount;
        return result;
    }
}