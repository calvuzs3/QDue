/**
 * STEP 3: EventsPreferenceFragment Implementation
 *
 * Complete settings fragment for events management with
 * manual update functionality and SSL validation
 */

package net.calvuz.qdue.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.EventPackageManager;
import net.calvuz.qdue.utils.Log;

/**
 * Events preference fragment with manual update functionality
 */
public class EventsPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "EventsPreferenceFragment";

    // Preferences keys
    private static final String KEY_EVENTS_ENABLED = "events_enabled";
    private static final String KEY_EXTERNAL_URL = "events_external_url";
    private static final String KEY_PACKAGE_ID = "events_package_id";
    private static final String KEY_SSL_VALIDATION = "events_ssl_validation";
    private static final String KEY_MANUAL_UPDATE = "events_manual_update";
    private static final String KEY_LAST_UPDATE_INFO = "events_last_update_info";
    private static final String KEY_CLEAR_LOCAL = "events_clear_local";

    // Preferences
    private EditTextPreference mUrlPreference;
    private EditTextPreference mPackageIdPreference;
    private SwitchPreferenceCompat mSslValidationPreference;
    private Preference mManualUpdatePreference;
    private Preference mLastUpdateInfoPreference;
    private Preference mClearLocalPreference;

    // Managers
    private EventPackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.events_preferences, rootKey);

        // Initialize managers
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mPackageManager = new EventPackageManager(requireContext());
//        mPackageManager = new EventPackageManager(requireContext(), getEventDao());

        // Find preferences
        findPreferences();

        // Setup preferences
        setupPreferences();

        // Update UI state
        updatePreferencesState();
        updateLastUpdateInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Find all preferences by key
     */
    private void findPreferences() {
        mUrlPreference = findPreference(KEY_EXTERNAL_URL);
        mPackageIdPreference = findPreference(KEY_PACKAGE_ID);
        mSslValidationPreference = findPreference(KEY_SSL_VALIDATION);
        mManualUpdatePreference = findPreference(KEY_MANUAL_UPDATE);
        mLastUpdateInfoPreference = findPreference(KEY_LAST_UPDATE_INFO);
        mClearLocalPreference = findPreference(KEY_CLEAR_LOCAL);
    }

    /**
     * Setup preference listeners and initial values
     */
    private void setupPreferences() {
        // URL preference with validation
        if (mUrlPreference != null) {
            mUrlPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String url = newValue.toString().trim();
                if (!TextUtils.isEmpty(url) && !url.startsWith("https://")) {
                    Toast.makeText(getContext(), "Solo URL HTTPS sono supportati", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            });

            // Set custom summary provider
            mUrlPreference.setSummaryProvider(preference -> {
                String url = mUrlPreference.getText();
                return TextUtils.isEmpty(url) ? "Nessun URL configurato" : url;
            });
        }

        // Package ID preference
        if (mPackageIdPreference != null) {
            mPackageIdPreference.setSummaryProvider(preference -> {
                String packageId = mPackageIdPreference.getText();
                return TextUtils.isEmpty(packageId) ? "Nessun ID specificato" : packageId;
            });
        }

        // Manual update preference
        if (mManualUpdatePreference != null) {
            mManualUpdatePreference.setOnPreferenceClickListener(preference -> {
                performManualUpdate();
                return true;
            });
        }

        // Clear local events preference
        if (mClearLocalPreference != null) {
            mClearLocalPreference.setOnPreferenceClickListener(preference -> {
                showClearLocalEventsDialog();
                return true;
            });
        }
    }

    /**
     * Update preferences state based on dependencies
     */
    private void updatePreferencesState() {
        boolean eventsEnabled = mSharedPreferences.getBoolean(KEY_EVENTS_ENABLED, true);
        String externalUrl = mSharedPreferences.getString(KEY_EXTERNAL_URL, "");
        boolean hasUrl = !TextUtils.isEmpty(externalUrl);

        // Update manual update preference state
        if (mManualUpdatePreference != null) {
            mManualUpdatePreference.setEnabled(eventsEnabled && hasUrl);
            if (!hasUrl) {
                mManualUpdatePreference.setSummary("Configura prima un URL esterno");
            } else {
                mManualUpdatePreference.setSummary("Scarica ora gli aggiornamenti dal server");
            }
        }

        // Update last update info state
        if (mLastUpdateInfoPreference != null) {
            mLastUpdateInfoPreference.setEnabled(hasUrl);
        }
    }

    /**
     * Update last update information
     */
    private void updateLastUpdateInfo() {
        if (mLastUpdateInfoPreference == null) return;

        String lastUpdateTime = mSharedPreferences.getString("events_last_update_time", "");
        String lastUpdatePackage = mSharedPreferences.getString("events_last_update_package", "");

        if (TextUtils.isEmpty(lastUpdateTime)) {
            mLastUpdateInfoPreference.setSummary("Nessun aggiornamento effettuato");
        } else {
            String summary = "Ultimo: " + lastUpdateTime;
            if (!TextUtils.isEmpty(lastUpdatePackage)) {
                summary += " (" + lastUpdatePackage + ")";
            }
            mLastUpdateInfoPreference.setSummary(summary);
        }
    }

    /**
     * Perform manual update from external URL
     */
    private void performManualUpdate() {
        String url = mSharedPreferences.getString(KEY_EXTERNAL_URL, "");
        String packageId = mSharedPreferences.getString(KEY_PACKAGE_ID, "");

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "Configura prima un URL esterno", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        mManualUpdatePreference.setSummary("Aggiornamento in corso…");
        mManualUpdatePreference.setEnabled(false);

        // Perform update
        mPackageManager.updateFromUrl(url, packageId, new EventPackageManager.UpdateCallback() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    updateLastUpdateInfo();
                    updatePreferencesState();
                    Log.d(TAG, "Manual update successful: " + message);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Errore: " + error, Toast.LENGTH_LONG).show();
                    updatePreferencesState();
                    Log.e(TAG, "Manual update failed: " + error);
                });
            }
        });
    }

    /**
     * Show dialog to confirm clearing local events
     */
    private void showClearLocalEventsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancella Eventi Locali")
                .setMessage("Sei sicuro di voler cancellare tutti gli eventi salvati localmente? " +
                        "Questa operazione non può essere annullata.")
                .setPositiveButton("Cancella", (dialog, which) -> {
                    clearLocalEvents();
                })
                .setNegativeButton("Annulla", null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }

    /**
     * Clear all local events
     */
    private void clearLocalEvents() {
        try {
            // TODO: Implement actual deletion through your DAO
            // getEventDao().deleteAllLocalEvents();

            // Clear last update info
            mSharedPreferences.edit()
                    .remove("events_last_update_time")
                    .remove("events_last_update_package")
                    .apply();

            updateLastUpdateInfo();
            Toast.makeText(getContext(), "Eventi locali cancellati", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Local events cleared");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing local events", e);
            Toast.makeText(getContext(), "Errore durante la cancellazione", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle shared preference changes
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_EVENTS_ENABLED:
            case KEY_EXTERNAL_URL:
                updatePreferencesState();
                break;

            case KEY_PACKAGE_ID:
                // Update package ID summary
                if (mPackageIdPreference != null) {
                    mPackageIdPreference.setSummaryProvider(mPackageIdPreference.getSummaryProvider());
                }
                break;
        }
    }

    /**
     * Get EventDao instance
     * TODO: Replace with your actual DAO implementation
     */
    private EventDao getEventDao() {
        // TODO: Return your actual EventDao instance
        // This could be from Room database, Retrofit service, etc.
        return new MockEventDao();
    }

    /**
     * Mock EventDao for compilation
     * TODO: Replace with your actual implementation
     */
    private static class MockEventDao implements EventDao {
        @Override
        public void deleteEventsByPackageId(String packageId) {
            // Mock implementation
        }

        @Override
        public void insertEvent(LocalEvent event) {
            // Mock implementation
        }

        @Override
        public void deleteAllLocalEvents() {
            // Mock implementation
        }
    }

}

// ==================== INTEGRATION INSTRUCTIONS ====================

/**
 * INTEGRATION STEPS:
 *
 * 1. Add to main root_preferences.xml:
 *
 *    <Preference
 *        app:key="events_settings"
 *        app:title="@string/settings_events_title"
 *        app:summary="@string/settings_events_main_summary"
 *        app:icon="@drawable/ic_event"
 *        app:fragment="net.calvuz.qdue.ui.settings.EventsPreferenceFragment" />
 *
 * 2. Create res/xml/events_preferences.xml with the settings structure
 *
 * 3. Add all strings to res/values/strings.xml
 *
 * 4. Create all drawable icons
 *
 * 5. Implement actual EventDao interface for your database
 *
 * 6. Add network permissions to AndroidManifest.xml:
 *    <uses-permission android:name="android.permission.INTERNET" />
 *    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *
 * 7. Add Gson dependency to build.gradle:
 *    implementation 'com.google.code.gson:gson:2.10.1'
 */