package net.calvuz.qdue.core.backup.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.calvuz.qdue.core.backup.models.PreferencesBackupPackage;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * STEP 1: Preferences Backup Service
 * <p>
 * Handles backup and restore operations for SharedPreferences.
 * Provides unified interface for backing up all application preferences
 * with categorization and selective restore capabilities.
 * <p>
 * Note: SharedPreferences are automatically backed up by Android,
 * but this service provides additional control and immediate backup capabilities.
 */
public class PreferencesBackupService {

    private static final String TAG = "PreferencesBackupService";

    // Standard preference categories
    private static final String CATEGORY_BACKUP = "backup";
    private static final String CATEGORY_UI = "ui";
    private static final String CATEGORY_SECURITY = "security";
    private static final String CATEGORY_SYNC = "sync";
    private static final String CATEGORY_NOTIFICATIONS = "notifications";
    private static final String CATEGORY_GENERAL = "general";

    // Sensitive preferences that should not be backed up
    private static final String[] SENSITIVE_KEYS = {
            "auth_token",
            "password",
            "secret",
            "private_key",
            "session_id",
            "device_id"
    };

    private final Context mContext;
    private final SharedPreferences mDefaultPreferences;

    // ==================== CONSTRUCTOR ====================

    public PreferencesBackupService(Context context) {
        this.mContext = context.getApplicationContext();
        this.mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        Log.d(TAG, "PreferencesBackupService initialized");
    }

    // ==================== BACKUP OPERATIONS ====================

    /**
     * Create complete preferences backup
     */
    public PreferencesBackupPackage createPreferencesBackup() {
        Log.d(TAG, "Creating preferences backup");

        try {
            // Get all preferences (excluding sensitive ones)
            Map<String, Object> allPreferences = getAllNonSensitivePreferences();

            // Categorize preferences
            Map<String, Map<String, Object>> categorizedPreferences = categorizePreferences(allPreferences);

            // Create backup package
            PreferencesBackupPackage backup = new PreferencesBackupPackage("1.0", allPreferences);
            backup.categorizedPreferences = categorizedPreferences;

            Log.d(TAG, "Created preferences backup with " + allPreferences.size() + " preferences");
            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create preferences backup", e);
            return new PreferencesBackupPackage("1.0", new HashMap<>());
        }
    }

    /**
     * Create category-specific preferences backup
     */
    public PreferencesBackupPackage createCategoryBackup(String category) {
        Log.d(TAG, "Creating " + category + " preferences backup");

        try {
            Map<String, Object> allPreferences = getAllNonSensitivePreferences();
            Map<String, Object> categoryPreferences = filterPreferencesByCategory(allPreferences, category);

            PreferencesBackupPackage backup = new PreferencesBackupPackage("1.0", categoryPreferences);

            // Add single category
            Map<String, Map<String, Object>> categorized = new HashMap<>();
            categorized.put(category, categoryPreferences);
            backup.categorizedPreferences = categorized;

            Log.d(TAG, "Created " + category + " backup with " + categoryPreferences.size() + " preferences");
            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create " + category + " preferences backup", e);
            return new PreferencesBackupPackage("1.0", new HashMap<>());
        }
    }

    // ==================== RESTORE OPERATIONS ====================

    /**
     * Restore preferences from backup
     */
    public int restorePreferencesBackup(PreferencesBackupPackage backup) {
        if (backup == null || backup.preferences == null) {
            Log.w(TAG, "Cannot restore null preferences backup");
            return 0;
        }

        Log.d(TAG, "Restoring preferences backup (" + backup.preferencesCount + " preferences)");

        try {
            SharedPreferences.Editor editor = mDefaultPreferences.edit();
            int restored = 0;

            for (Map.Entry<String, Object> entry : backup.preferences.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip sensitive keys
                if (isSensitiveKey(key)) {
                    Log.d(TAG, "Skipping sensitive preference: " + key);
                    continue;
                }

                // Apply preference based on type
                if (restorePreferenceValue(editor, key, value)) {
                    restored++;
                }
            }

            // Commit changes
            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Successfully restored " + restored + " preferences");
                return restored;
            } else {
                Log.e(TAG, "Failed to commit preferences changes");
                return 0;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore preferences backup", e);
            return 0;
        }
    }

    /**
     * Restore specific category from backup
     */
    public int restoreCategoryBackup(PreferencesBackupPackage backup, String category) {
        if (backup == null || backup.categorizedPreferences == null) {
            Log.w(TAG, "Cannot restore null categorized preferences backup");
            return 0;
        }

        Map<String, Object> categoryPreferences = backup.categorizedPreferences.get(category);
        if (categoryPreferences == null) {
            Log.w(TAG, "Category " + category + " not found in backup");
            return 0;
        }

        Log.d(TAG, "Restoring " + category + " preferences (" + categoryPreferences.size() + " preferences)");

        try {
            SharedPreferences.Editor editor = mDefaultPreferences.edit();
            int restored = 0;

            for (Map.Entry<String, Object> entry : categoryPreferences.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (restorePreferenceValue(editor, key, value)) {
                    restored++;
                }
            }

            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Successfully restored " + restored + " " + category + " preferences");
                return restored;
            } else {
                Log.e(TAG, "Failed to commit " + category + " preferences changes");
                return 0;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore " + category + " preferences", e);
            return 0;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all application preferences excluding sensitive ones
     */
    private Map<String, Object> getAllNonSensitivePreferences() {
        Map<String, Object> preferences = new HashMap<>();

        // Get default SharedPreferences
        Map<String, ?> defaultPrefs = mDefaultPreferences.getAll();
        for (Map.Entry<String, ?> entry : defaultPrefs.entrySet()) {
            String key = entry.getKey();
            if (!isSensitiveKey(key)) {
                preferences.put(key, entry.getValue());
            }
        }

        // Get app-specific SharedPreferences files
        addAppSpecificPreferences(preferences);

        return preferences;
    }

    /**
     * Add app-specific SharedPreferences files
     */
    private void addAppSpecificPreferences(Map<String, Object> preferences) {
        try {
            // Get app's SharedPreferences directory
            File prefsDir = new File(mContext.getApplicationInfo().dataDir, "shared_prefs");
            if (!prefsDir.exists()) {
                return;
            }

            File[] prefFiles = prefsDir.listFiles((dir, name) -> name.endsWith(".xml"));
            if (prefFiles == null) {
                return;
            }

            for (File prefFile : prefFiles) {
                String prefName = prefFile.getName().replace(".xml", "");

                // Skip default preferences (already processed)
                if (prefName.equals(mContext.getPackageName() + "_preferences")) {
                    continue;
                }

                try {
                    SharedPreferences specificPrefs = mContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                    Map<String, ?> specificPrefsMap = specificPrefs.getAll();

                    for (Map.Entry<String, ?> entry : specificPrefsMap.entrySet()) {
                        String key = prefName + "." + entry.getKey();
                        if (!isSensitiveKey(key)) {
                            preferences.put(key, entry.getValue());
                        }
                    }

                } catch (Exception e) {
                    Log.w(TAG, "Failed to read preferences file: " + prefName, e);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to scan app-specific preferences", e);
        }
    }

    /**
     * Categorize preferences based on key patterns
     */
    private Map<String, Map<String, Object>> categorizePreferences(Map<String, Object> preferences) {
        Map<String, Map<String, Object>> categorized = new HashMap<>();

        // Initialize categories
        categorized.put(CATEGORY_BACKUP, new HashMap<>());
        categorized.put(CATEGORY_UI, new HashMap<>());
        categorized.put(CATEGORY_SECURITY, new HashMap<>());
        categorized.put(CATEGORY_SYNC, new HashMap<>());
        categorized.put(CATEGORY_NOTIFICATIONS, new HashMap<>());
        categorized.put(CATEGORY_GENERAL, new HashMap<>());

        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String category = determineCategory(key);
            categorized.get(category).put(entry.getKey(), entry.getValue());
        }

        return categorized;
    }

    /**
     * Determine category for a preference key
     */
    private String determineCategory(String key) {
        if (key.contains("backup") || key.contains("restore")) {
            return CATEGORY_BACKUP;
        } else if (key.contains("ui") || key.contains("theme") || key.contains("display")) {
            return CATEGORY_UI;
        } else if (key.contains("auth") || key.contains("security") || key.contains("login")) {
            return CATEGORY_SECURITY;
        } else if (key.contains("sync") || key.contains("cloud") || key.contains("server")) {
            return CATEGORY_SYNC;
        } else if (key.contains("notification") || key.contains("alert") || key.contains("sound")) {
            return CATEGORY_NOTIFICATIONS;
        } else {
            return CATEGORY_GENERAL;
        }
    }

    /**
     * Filter preferences by category
     */
    private Map<String, Object> filterPreferencesByCategory(Map<String, Object> preferences, String category) {
        Map<String, Object> filtered = new HashMap<>();

        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (determineCategory(key).equals(category)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    /**
     * Check if key is sensitive and should not be backed up
     */
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restore individual preference value based on type
     */
    private boolean restorePreferenceValue(SharedPreferences.Editor editor, String key, Object value) {
        try {
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> stringSet = (Set<String>) value;
                editor.putStringSet(key, stringSet);
            } else {
                Log.w(TAG, "Unknown preference type for key " + key + ": " + value.getClass());
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore preference " + key, e);
            return false;
        }
    }

    /**
     * Get preferences count
     */
    public int getPreferencesCount() {
        return getAllNonSensitivePreferences().size();
    }

    /**
     * Get category count
     */
    public int getCategoryCount(String category) {
        Map<String, Object> allPreferences = getAllNonSensitivePreferences();
        return filterPreferencesByCategory(allPreferences, category).size();
    }

    /**
     * Get available categories
     */
    public String[] getAvailableCategories() {
        return new String[]{
                CATEGORY_BACKUP,
                CATEGORY_UI,
                CATEGORY_SECURITY,
                CATEGORY_SYNC,
                CATEGORY_NOTIFICATIONS,
                CATEGORY_GENERAL
        };
    }

    /**
     * Validate preferences backup
     */
    public boolean validatePreferencesBackup(PreferencesBackupPackage backup) {
        if (backup == null) {
            Log.w(TAG, "Preferences backup is null");
            return false;
        }

        if (backup.preferences == null) {
            Log.w(TAG, "Preferences map is null");
            return false;
        }

        if (backup.preferencesCount != backup.preferences.size()) {
            Log.w(TAG, "Preferences count mismatch: declared=" + backup.preferencesCount +
                    ", actual=" + backup.preferences.size());
            return false;
        }

        Log.d(TAG, "Preferences backup validation passed");
        return true;
    }
}