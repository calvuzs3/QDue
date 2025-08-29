package net.calvuz.qdue.ui.features.settings.presentation;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Settings Activity with improved error handling and transaction safety.
 */
public class QDueSettingsActivity extends AppCompatActivity implements SettingsFragment.SettingsNavigationCallback{

    private static final String TAG = "QDueSettingsActivity";

    // ==================== FRAGMENT TAGS ====================

    private static final String FRAGMENT_TAG_SETTINGS = "fragment_settings";
    private static final String FRAGMENT_TAG_CUSTOM_PATTERNS = "fragment_custom_patterns";

    // ==================== STATE TRACKING ====================

    private boolean isNavigatingToSubFragment = false;

    // ==================== LIFECYCLE ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_qdue_settings);

            // Setup floating toolbar (NO ActionBar)
            setupFloatingToolbar();

            // Load main settings fragment or restore state
            if (savedInstanceState == null) {
                loadSettingsFragment();
            } else {
                handleActivityRecreation();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset navigation state
        isNavigatingToSubFragment = false;

        // Refresh current fragment if it's SettingsFragment
        refreshCurrentFragment();
    }

    @Override
    public void onBackPressed() {
        if (handleBackNavigation()) {
            return; // Back navigation was handled
        }

        // Default back behavior
        super.onBackPressed();
    }

    // ==================== TOOLBAR SETUP ====================

    /**
     * Setup floating toolbar without ActionBar
     */
    private void setupFloatingToolbar() {
        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> handleToolbarNavigation());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar: " + e.getMessage());
        }
    }

    /**
     * Handle toolbar navigation click (back button)
     */
    private void handleToolbarNavigation() {
        if (!handleBackNavigation()) {
            finish();
        }
    }

    // ==================== FRAGMENT MANAGEMENT ====================


    /**
     * Load SettingsFragment with comprehensive error handling
     */
    private void loadSettingsFragment() {
        // Check if activity is still valid
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing or destroyed, skipping fragment transaction");
            return;
        }

        try {
            // Create and add fragment safely
            SettingsFragment settingsFragment = new SettingsFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.settings_container, settingsFragment, FRAGMENT_TAG_SETTINGS);

            // Add to back stack for proper navigation
            //transaction.addToBackStack(null); // this is intended for multiple fragments

            // Commit safely
            commitFragmentTransaction(transaction, "SettingsFragment");

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error loading SettingsFragment: " + e.getMessage());
            finish();
        }
    }

    /**
     * Update toolbar title based on current fragment
     */
    private void updateToolbarTitle() {
        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
            if (toolbar != null) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);

                if (currentFragment instanceof CustomPatternPreferencesFragment) {
                    toolbar.setTitle(R.string.pref_custom_patterns_title);
                } else {
                    toolbar.setTitle(R.string.settings_title);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating toolbar title", e);
        }
    }


    // ==================== NAVIGATION CALLBACK IMPLEMENTATION ====================

    /**
     * Navigate to a specific fragment from main settings
     * Implementation of SettingsFragment.SettingsNavigationCallback
     */
    @Override
    public void navigateToFragment(@NonNull Fragment fragment, @NonNull String title) {
        try {
            if (isNavigatingToSubFragment) {
                Log.w(TAG, "Navigation already in progress, ignoring request");
                return;
            }

            isNavigatingToSubFragment = true;

            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing or destroyed, canceling navigation");
                return;
            }

            // Determine fragment tag
            String fragmentTag = getFragmentTag(fragment);

            // Create transaction
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // Enter animation
                    R.anim.slide_out_left,  // Exit animation
                    R.anim.slide_in_left,   // Pop enter animation
                    R.anim.slide_out_right  // Pop exit animation
            );

            transaction.replace(R.id.settings_container, fragment, fragmentTag);
            transaction.addToBackStack(fragmentTag); // Add to back stack for navigation

            // Update toolbar title
            updateToolbarTitle(title);

            // Commit transaction
            commitFragmentTransaction(transaction, "Navigate to " + fragmentTag);

            Log.d(TAG, "Successfully navigated to fragment: " + fragmentTag);

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to fragment", e);
            isNavigatingToSubFragment = false;
        }
    }

    /**
     * Get appropriate fragment tag for a fragment instance
     */
    private String getFragmentTag(@NonNull Fragment fragment) {
        if (fragment instanceof CustomPatternPreferencesFragment) {
            return FRAGMENT_TAG_CUSTOM_PATTERNS;
        }

        // Default tag for unknown fragments
        return fragment.getClass().getSimpleName();
    }

    /**
     * Update toolbar title
     */
    private void updateToolbarTitle(@NonNull String title) {
        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
            if (toolbar != null) {
                toolbar.setTitle(title);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating toolbar title to: " + title, e);
        }
    }

    // ==================== BACK NAVIGATION ====================

    /**
     * Handle back navigation with proper fragment back stack management
     *
     * @return true if back navigation was handled, false if should use default behavior
     */
    private boolean handleBackNavigation() {
        try {
            int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();

            Log.d(TAG, "Back navigation requested. Back stack entries: " + backStackEntryCount);

            if (backStackEntryCount > 0) {
                // Pop from back stack (will trigger fragment transaction)
                getSupportFragmentManager().popBackStack();

                // Update toolbar title after popping
                updateToolbarTitleAfterPop();

                Log.d(TAG, "Popped fragment from back stack");
                return true; // Back navigation handled
            }

            // No fragments in back stack, let activity handle it
            Log.d(TAG, "No fragments in back stack, using default back behavior");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error in back navigation", e);
            return false;
        }
    }

    /**
     * Update toolbar title after popping from back stack
     */
    private void updateToolbarTitleAfterPop() {
        // Use post to ensure fragment transaction is complete
        getSupportFragmentManager().executePendingTransactions();

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);

        if (currentFragment instanceof SettingsFragment) {
            updateToolbarTitle(getString(R.string.settings_title));
        } else if (currentFragment instanceof CustomPatternPreferencesFragment) {
            updateToolbarTitle(getString(R.string.pref_custom_patterns_title));
        }
    }

    // ==================== FRAGMENT TRANSACTION UTILITIES ====================

    /**
     * Safely commit fragment transaction with comprehensive error handling
     */
    private void commitFragmentTransaction(@NonNull FragmentTransaction transaction, @NonNull String operationName) {
        try {
            // Commit safely
            if (!getSupportFragmentManager().isStateSaved()) {
                transaction.commit();
                Log.d(TAG, operationName + " committed successfully");
            } else {
                // Use commitAllowingStateLoss if state is saved
                transaction.commitAllowingStateLoss();
                Log.w(TAG, operationName + " used commitAllowingStateLoss due to saved state");
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in fragment transaction: " + e.getMessage());

            // Try alternative approach
            handleFailedFragmentTransaction(transaction, operationName);

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in fragment transaction: " + e.getMessage(), e);

            // For navigation transactions, reset navigation state
            if (operationName.contains("Navigate")) {
                isNavigatingToSubFragment = false;
            }
        }
    }

    /**
     * Handle failed fragment transaction with retry logic
     */
    private void handleFailedFragmentTransaction(@NonNull FragmentTransaction transaction, @NonNull String operationName) {
        try {
            getSupportFragmentManager().executePendingTransactions();
            transaction.commitAllowingStateLoss();
            Log.d(TAG, "Retry fragment transaction successful for: " + operationName);
        } catch (Exception retryException) {
            Log.e(TAG, "Retry fragment transaction also failed for: " + operationName, retryException);

            // If this is a navigation transaction, reset state
            if (operationName.contains("Navigate")) {
                isNavigatingToSubFragment = false;
            }

            // For critical failures, finish activity
            if (operationName.contains("Main")) {
                finish();
            }
        }
    }

    // ==================== FRAGMENT LIFECYCLE SUPPORT ====================

    /**
     * Refresh current fragment if it supports refresh
     */
    private void refreshCurrentFragment() {
        try {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);

            if (currentFragment instanceof SettingsFragment) {
                ((SettingsFragment) currentFragment).refreshPreferenceValues();
                Log.d(TAG, "Refreshed SettingsFragment values");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing current fragment", e);
        }
    }

    /**
     * Handle activity recreation (configuration change, etc.)
     */
    private void handleActivityRecreation() {
        Log.d(TAG, "Activity recreated - fragments should already exist");

        // Update toolbar title based on current fragment
        updateToolbarTitle();
    }

    // ==================== MENU HANDLING ====================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // No ActionBar menu - handled by toolbar click listener
        return super.onOptionsItemSelected(item);
    }

    // ==================== CLEANUP ====================

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