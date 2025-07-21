package net.calvuz.qdue.ui.features.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.settings.presentation.SettingsFragment;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Settings Activity with improved error handling and transaction safety.
 */
public class QDueSettingsActivity extends AppCompatActivity {

    private static final String TAG = "QDueSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_qdue_settings);

            // Setup floating toolbar (NO ActionBar)
            setupFloatingToolbar();

            // Load SettingsFragment safely
            loadSettingsFragment(savedInstanceState);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            finish();
        }
    }

    /**
     * Setup floating toolbar without ActionBar
     */
    private void setupFloatingToolbar() {
        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> finish());
                Log.d(TAG, "Floating toolbar configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar: " + e.getMessage());
        }
    }

    /**
     * Load SettingsFragment with comprehensive error handling
     */
    private void loadSettingsFragment(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(TAG, "Activity recreated - fragment should already exist");
            return;
        }

        try {
            // Check if container exists
            if (findViewById(R.id.settings_container) == null) {
                Log.e(TAG, "Settings container not found in layout");
                return;
            }

            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing or destroyed, skipping fragment transaction");
                return;
            }

            // Create and add fragment safely
            SettingsFragment settingsFragment = new SettingsFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.settings_container, settingsFragment);

            // Add to back stack for proper navigation
            //transaction.addToBackStack(null); // this is intended for multiple fragments

            // Commit safely
            if (!getSupportFragmentManager().isStateSaved()) {
                transaction.commit();
                Log.d(TAG, "SettingsFragment loaded successfully");
            } else {
                // Use commitAllowingStateLoss if state is saved
                transaction.commitAllowingStateLoss();
                Log.w(TAG, "Used commitAllowingStateLoss due to saved state");
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in fragment transaction: " + e.getMessage());

            // Try alternative approach
            try {
                getSupportFragmentManager().executePendingTransactions();
                FragmentTransaction retryTransaction = getSupportFragmentManager().beginTransaction();
                retryTransaction.replace(R.id.settings_container, new SettingsFragment());
                retryTransaction.commitAllowingStateLoss();
                Log.d(TAG, "Retry fragment transaction successful");
            } catch (Exception retryException) {
                Log.e(TAG, "Retry fragment transaction also failed: " + retryException.getMessage());
                finish();
            }

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error loading SettingsFragment: " + e.getMessage());
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // No ActionBar menu - handled by toolbar click listener
        return super.onOptionsItemSelected(item);
    }

    /**
     * Clean up resources
     */
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            Log.d(TAG, "Activity destroyed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
}