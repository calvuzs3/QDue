package net.calvuz.qdue;

import static net.calvuz.qdue.QDue.Debug.DEBUG_ACTIVITY;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.ui.dayslist.DayslistViewFragment;
import net.calvuz.qdue.ui.shared.FragmentCommunicationInterface;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.TimeChangeReceiver;

/**
 * Main Activity with responsive toolbar visibility.
 * - Portrait: Toolbar visible + BottomNavigation
 * - Landscape: Toolbar hidden + Sidebar Navigation
 * <p>
 * Key improvement: Toolbar always exists in binding, just hidden in landscape
 */
public class QDueMainActivity extends AppCompatActivity
        implements
        FragmentCommunicationInterface,
        TimeChangeReceiver.TimeChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QDueMainActivity";

    // UI related
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityQdueMainBinding binding;
    private NavController navController;

    // Navigation components (one will be null based on orientation)
    private BottomNavigationView bottomNavigation;
    private NavigationView sidebarNavigation;

    // Current fragment reference
//    private Fragment mCurrentFragment = null;

    // Time Change Receiver
    private TimeChangeReceiver mTimeChangeReceiver;
    private boolean mReceiverRegistered = false;

    // Shared Preferences
    private SharedPreferences sharedPreferences;

    // ======================= METHODS ============================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, String.valueOf(QDue.getContext() == getApplicationContext()));

        // Initialize time change receiver
        mTimeChangeReceiver = new TimeChangeReceiver(this);

        // Register preference listener
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // NORMAL: Use binding
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation
        setupNavigation();
    }

    /**
     * Setup navigation safely with error handling.
     */
    private void setupNavigation() {
        try {
            // Method 1: Find NavHostFragment directly
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                if (DEBUG_ACTIVITY) Log.d(TAG, "NavController found via NavHostFragment");
            } else {
                // Method 2: Fallback with Navigation.findNavController
                navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if (DEBUG_ACTIVITY)
                    Log.d(TAG, "NavController found via Navigation.findNavController");
            }

            // Setup navigation components
            setupNavigationComponents();

        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting up NavController: " + e.getMessage());

            // Retry after small delay
            findViewById(R.id.nav_host_fragment_content_main).post(() -> {
                try {
                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    setupNavigationComponents();
                    if (DEBUG_ACTIVITY) Log.d(TAG, "NavController found on second attempt");
                } catch (Exception retryException) {
                    Log.e(TAG, "Failed to find NavController on retry: " + retryException.getMessage());
                }
            });
        }
    }

    /**
     * Setup bottom navigation (portrait mode).
     */
    private void setupBottomNavigation() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Configuring Bottom Navigation");

        try {
            NavigationUI.setupWithNavController(bottomNavigation, navController);

            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                try {
                    if (id == R.id.nav_settings) {
                        navController.navigate(R.id.nav_settings);
                        return true;
                    } else if (id == R.id.nav_about) {
                        navController.navigate(R.id.nav_about);
                        return true;
                    }

                    return NavigationUI.onNavDestinationSelected(item, navController);
                } catch (Exception e) {
                    Log.e(TAG, "Error during bottom navigation: " + e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation: " + e.getMessage());
        }
    }

    /**
     * Setup sidebar navigation (landscape mode).
     */
    private void setupSidebarNavigation() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Configuring Sidebar Navigation");

        try {
            NavigationUI.setupWithNavController(sidebarNavigation, navController);

            sidebarNavigation.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                try {
                    if (id == R.id.nav_settings) {
                        try {
                            navController.navigate(R.id.nav_settings);
                        } catch (Exception e) {
                            // Usa Intent diretto
                            Intent intent = new Intent(this, QDueSettingsActivity.class);
                            startActivity(intent);
                        }
                        return true;
                    } else if (id == R.id.nav_about) {
                        navController.navigate(R.id.nav_about);
                        return true;
                    }

                    return NavigationUI.onNavDestinationSelected(item, navController);
                } catch (Exception e) {
                    Log.e(TAG, "Error during sidebar navigation: " + e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up sidebar navigation: " + e.getMessage());
        }
    }

    /**
     * Notify fragments to update data.
     */
    private void notifyUpdates() {
        if (DEBUG_ACTIVITY) Log.d(TAG, "notifyUpdates");

        try {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (fragment != null) {
                if (fragment instanceof NavHostFragment) {
                    Fragment childFragment = ((NavHostFragment) fragment).getChildFragmentManager().getPrimaryNavigationFragment();

                    if (childFragment instanceof DayslistViewFragment) {
                        ((DayslistViewFragment) childFragment).notifyUpdates();
                    }
                }
            } else {
                Log.e(TAG, "Fragment not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during notifyUpdates: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onResume");
        registerTimeChangeReceiver();
        notifyUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onPause");
        unregisterTimeChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG_ACTIVITY) Log.d(TAG, "onDestroy");
        unregisterTimeChangeReceiver();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTimeChangeReceiver() {
        if (!mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                IntentFilter filter = TimeChangeReceiver.createCriticalIntentFilter();
                registerReceiver(mTimeChangeReceiver, filter);
                mReceiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Error registering TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    private void unregisterTimeChangeReceiver() {
        if (mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                unregisterReceiver(mTimeChangeReceiver);
                mReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    // TimeChangeListener implementation
    @Override
    public void onTimeChanged() {
        Log.d(TAG, "onTimeChanged - updating interface");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "System time updated", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDateChanged() {
        Log.d(TAG, "onDateChanged - updating interface");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "System date updated", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onTimezoneChanged() {
        Log.d(TAG, "onTimezoneChanged - updating interface");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "Timezone updated", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * FIXED: Configure ActionBar only when toolbar is visible
     */
    private void setupNavigationComponents() {
        if (navController == null) {
            Log.e(TAG, "NavController is null, cannot configure navigation");
            return;
        }

        // Find navigation components (only one will exist based on layout)
        bottomNavigation = findViewById(R.id.bottom_navigation);
        sidebarNavigation = findViewById(R.id.sidebar_navigation);

        // Determine current orientation
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (DEBUG_ACTIVITY) {
            Log.d(TAG, "Orientation: " + (isLandscape ? "Landscape" : "Portrait"));
            Log.d(TAG, "Bottom Navigation available: " + (bottomNavigation != null));
            Log.d(TAG, "Sidebar Navigation available: " + (sidebarNavigation != null));
        }

        // Setup navigation based on available components
        if (bottomNavigation != null) {
            setupBottomNavigation();
        }

        if (sidebarNavigation != null) {
            setupSidebarNavigation();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Preference changed: " + key);
    }

    // ========== COMMUNICATION INTERFACE IMPLEMENTATION ==========

    @Override
    public void onFragmentNavigationRequested(int destinationId, Bundle data) {
        final String mTAG = "onFragmentNavigationRequested";

        try {
            if (navController != null) {
                if (data != null) {
                    navController.navigate(destinationId, data);
                } else {
                    navController.navigate(destinationId);
                }
                if (DEBUG_ACTIVITY) Log.d(TAG, mTAG + " Navigation to: " + destinationId);
            } else {
                Log.e(TAG, mTAG + " NavController is null, cannot navigate");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + " Error during navigation: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentTitleChanged(String title) {
        final String METHOD_TAG = TAG + " onFragmentTitleChanged";

        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Updated toolbar title to: " + title);
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error updating toolbar title: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentStatusMessage(String message, boolean isError) {
        final String METHOD_TAG = TAG + " onFragmentStatusMessage";

        try {
            if (isError) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } else {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
            if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Displayed status message: " + message);
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error showing status message: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentMenuChanged(int menuId) {
        final String METHOD_TAG = TAG + " onFragmentMenuChanged";

        // Invalidate options menu to trigger onCreateOptionsMenu with new menu
        try {
            invalidateOptionsMenu();
            if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Invalidated options menu for menuId: " + menuId);
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error changing fragment menu: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentToolbarVisibilityChanged(boolean visible) {
        final String METHOD_TAG = TAG + " onFragmentToolbarVisibilityChanged";

        try {
            // Handle toolbar visibility for different orientations
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (!isLandscape && binding.appBarLayout != null) {
                // Only affect toolbar in portrait mode
                binding.appBarLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Set toolbar visibility: " + visible);
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error changing toolbar visibility: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentOperationComplete(String operationType, boolean success, Bundle resultData) {
        final String METHOD_TAG = TAG + " onFragmentOperationComplete";

        try {
            if (DEBUG_ACTIVITY) {
                Log.d(METHOD_TAG, "Operation completed: " + operationType + ", success: " + success);
            }

            switch (operationType) {
                case "data_refresh":
                    if (success) {
                        notifyUpdates();
                    }
                    break;

                case "scroll_to_today":
                    if (success) {
                        onFragmentStatusMessage("Navigated to today", false);
                    }
                    break;

                default:
                    // Handle other operation types as needed
                    break;
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error handling operation completion: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentCustomAction(String action, Bundle data) {
        final String METHOD_TAG = TAG + " onFragmentCustomAction";

        try {
            switch (action) {
                case "update_toolbar_title":
                    if (data != null && data.containsKey("title")) {
                        String title = data.getString("title");
                        onFragmentTitleChanged(title);
                    }
                    break;

                case "show_loading":
                    // Handle loading indicator requests
                    boolean show = data != null && data.getBoolean("show", false);
                    showLoadingIndicator(show);
                    break;

                case "update_fab_visibility":
                    // Handle FAB visibility updates if needed
                    boolean visible = data != null && data.getBoolean("visible", true);
                    handleFabVisibility(visible);
                    break;

                default:
                    if (DEBUG_ACTIVITY) {
                        Log.d(METHOD_TAG, "Unhandled custom action: " + action);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(METHOD_TAG, "Error handling custom action '" + action + "': " + e.getMessage());
        }
    }

    /**
     * Show/hide loading indicator for long operations.
     */
    private void showLoadingIndicator(boolean show) {
        // Implementation depends on your loading indicator setup
        // This could be a progress bar in the binding, or a custom loading overlay
        try {
            // Example implementation - adjust based on your actual UI
            View loadingOverlay = findViewById(R.id.loading_overlay);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error controlling loading indicator: " + e.getMessage());
        }
    }

    /**
     * Handle FAB visibility if activity needs to coordinate with fragment FABs.
     */
    private void handleFabVisibility(boolean visible) {
        // Implementation depends on whether activity has its own FABs to coordinate
        // For now, this is mainly handled by individual fragments
        if (DEBUG_ACTIVITY) {
            Log.d(TAG, "FAB visibility coordination: " + visible);
        }
    }
}