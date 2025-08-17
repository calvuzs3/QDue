package net.calvuz.qdue.core.backup.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.calvuz.qdue.core.backup.models.PreferencesBackupPackage;
import net.calvuz.qdue.core.services.models.OperationResult;
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

    private static final String VERSION = "2.0";

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

    // ==================== CORE BACKUP METHODS ====================

    /**
     * ✅ NEW: Generate preferences backup with OperationResult wrapper
     * Used by CoreBackupManager for unified backup operations
     */
    public OperationResult<PreferencesBackupPackage> generatePreferencesBackup() {
        try {
            Log.d(TAG, "Generating preferences backup with OperationResult");
            PreferencesBackupPackage preferencesBackup = createPreferencesBackup();

            if (preferencesBackup == null) {
                return OperationResult.failure("Failed to create preferences backup package",
                        OperationResult.OperationType.BACKUP);
            }

            Log.d(TAG, "Preferences backup generated successfully: " + preferencesBackup.getPreferenceCount() + " preferences");
            return OperationResult.success(preferencesBackup, OperationResult.OperationType.BACKUP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate preferences backup", e);
            return OperationResult.failure("Preferences backup generation failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    // ==================== BACKUP OPERATIONS ====================

    /**
     * Create complete preferences backup
     */
    public PreferencesBackupPackage createPreferencesBackup() {
        try {
            Log.d(TAG, "Creating preferences backup package");

            // Get all preferences
            Map<String, ?> allPreferences = mDefaultPreferences.getAll();
            Map<String, Object> backupPreferences = new HashMap<>();

            // Convert all preference values to backup-safe formats
            for (Map.Entry<String, ?> entry : allPreferences.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value != null) {
                    // Convert to JSON-safe types
                    if (value instanceof Boolean || value instanceof String ||
                            value instanceof Integer || value instanceof Long ||
                            value instanceof Float) {
                        backupPreferences.put(key, value);
                    } else {
                        // Convert other types to string representation
                        backupPreferences.put(key, value.toString());
                    }
                }
            }

            // Create backup package
            PreferencesBackupPackage backup = new PreferencesBackupPackage();
            backup.preferences = backupPreferences;
            backup.version = VERSION;
            backup.timestamp = java.time.LocalDateTime.now().toString();
            backup.preferenceCount = backupPreferences.size();

            // Add metadata
            backup.metadata = new HashMap<>();
            backup.metadata.put("totalPreferences", backupPreferences.size());
            backup.metadata.put("backupSource", "SharedPreferences");
            backup.metadata.put("backupMethod", "PreferencesBackupService");

            Log.d(TAG, "Preferences backup package created: " + backupPreferences.size() + " preferences");
            return backup;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create preferences backup package", e);
            return null;
        }
    }

    // ==================== RESTORE METHODS ====================

    /**
     * ✅ Restore preferences from backup package
     */
    public OperationResult<Integer> restorePreferencesBackup(PreferencesBackupPackage backup) {
        try {
            if (backup == null || backup.preferences == null) {
                return OperationResult.failure("Invalid preferences backup package",
                        OperationResult.OperationType.RESTORE);
            }

            Log.d(TAG, "Restoring preferences from backup: " + backup.preferences.size() + " preferences");

            SharedPreferences.Editor editor = mDefaultPreferences.edit();
            int restoredCount = 0;

            for (Map.Entry<String, Object> entry : backup.preferences.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                try {
                    // Restore based on value type
                    if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                        restoredCount++;
                    } else if (value instanceof String) {
                        editor.putString(key, (String) value);
                        restoredCount++;
                    } else if (value instanceof Integer) {
                        editor.putInt(key, (Integer) value);
                        restoredCount++;
                    } else if (value instanceof Long) {
                        editor.putLong(key, (Long) value);
                        restoredCount++;
                    } else if (value instanceof Float) {
                        editor.putFloat(key, (Float) value);
                        restoredCount++;
                    } else {
                        // Try to restore as string
                        editor.putString(key, value.toString());
                        restoredCount++;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to restore preference: " + key, e);
                }
            }

            // Apply all changes
            boolean applied = editor.commit();
            if (!applied) {
                return OperationResult.failure("Failed to apply preferences changes to storage",
                        OperationResult.OperationType.RESTORE);
            }

            Log.d(TAG, "Preferences restore completed: " + restoredCount + " preferences restored");
            return OperationResult.success(restoredCount, OperationResult.OperationType.RESTORE);

        } catch (Exception e) {
            Log.e(TAG, "Failed to restore preferences backup", e);
            return OperationResult.failure("Preferences restore failed: " + e.getMessage(),
                    OperationResult.OperationType.RESTORE);
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

        if (backup.getPreferenceCount() != backup.preferences.size()) {
            Log.w(TAG, "Preferences count mismatch: declared=" + backup.getPreferenceCount() +
                    ", actual=" + backup.preferences.size());
            return false;
        }

        Log.d(TAG, "Preferences backup validation passed");
        return true;
    }
}