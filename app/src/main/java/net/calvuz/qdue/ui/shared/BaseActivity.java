package net.calvuz.qdue.ui.shared;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigationrail.NavigationRailView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.TimeChangeReceiver;

import java.util.List;

public abstract class BaseActivity extends AppCompatActivity implements
        FragmentCommunicationInterface,
        TimeChangeReceiver.TimeChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // TAG
    private static final String TAG = "BaseActivity";

    // Time Change Receiver
    protected TimeChangeReceiver timeChangeReceiver;
    protected boolean receiverRegistered = false;

    // Shared Preferences
    protected SharedPreferences sharedPreferences;

    // Navigation Components (only one will be active based on layout)
    protected BottomNavigationView bottomNavigation;
    protected NavigationRailView navigationRail;
    protected NavigationView drawerNavigation;
    protected DrawerLayout drawerLayout;
    protected NavigationView sidebarNavigation;
    protected NavController navController;


    /**
     * Detect which navigation components are available in current layout.
     * This allows the same activity to work with different layout configurations.
     */
    protected abstract void detectNavigationComponents();

    /**
     * Unified navigation item selection handler for all navigation modes.
     */
    protected abstract boolean handleNavigationItemSelected(int itemId);

    /**
     * Enhanced drawer item selection handler for extended menu (phone portrait).
     */
    protected abstract boolean handleExtendedDrawerItemSelected(int itemId);

    /**
     * Setup NavController with fallback error handling.
     */
    protected abstract void setupNavController();




    /**
     * Enum to track current navigation mode for proper handling
     */
    protected enum NavigationMode {
        PHONE_PORTRAIT,      // BottomNavigation + FAB separato
        TABLET_PORTRAIT,     // NavigationRail + FAB integrato
        LANDSCAPE_SMALL,     // NavigationRail espanso + Extended FAB
        LANDSCAPE_LARGE      // NavigationRail + Drawer
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String mTAG = "onCreate: ";
        Log.v(TAG, mTAG + "called.");

        // Initialize time change receiver
        timeChangeReceiver = new TimeChangeReceiver(this);

        // Register preference listener
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    /**
     * Setup Phone Portrait navigation (BottomNavigation + separate FAB).
     */
    protected void setupPhonePortraitNavigation() {
        final String mTAG = "setupPhonePortraitNavigation: ";
        Log.v(TAG, mTAG + "called.");

        // Setup BottomNavigation for primary navigation
        if (bottomNavigation != null && navController != null) {
            NavigationUI.setupWithNavController(bottomNavigation, navController);

            bottomNavigation.setOnItemSelectedListener(item -> {
                return handleNavigationItemSelected(item.getItemId());
            });
        }

        // Setup DrawerNavigation for extended menu
        if (drawerNavigation != null) {
            drawerNavigation.setNavigationItemSelectedListener(item -> {
                boolean handled = handleExtendedDrawerItemSelected(item.getItemId());
                if (handled && drawerLayout != null) {
                    drawerLayout.closeDrawer(drawerNavigation);
                }
                return handled;
            });

            // Enable drawer
            if (drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        }

    }

    /**
     * Setup Tablet Portrait navigation (NavigationRail + integrated FAB).
     */
    protected void setupTabletPortraitNavigation() {
        final String mTAG = "setupTabletPortraitNavigation: ";
        Log.v(TAG, mTAG + "called.");

        if (navigationRail != null && navController != null) {
            NavigationUI.setupWithNavController(navigationRail, navController);

            navigationRail.setOnItemSelectedListener(item -> {
                return handleNavigationItemSelected(item.getItemId());
            });

            // Disable drawer for tablet portrait
            if (drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }

    /**
     * Setup Landscape Small navigation (NavigationRail expanded + Extended FAB).
     */
    protected void setupLandscapeSmallNavigation() {
        final String mTAG = "setupLandscapeSmallNavigation: ";
        Log.v(TAG, mTAG + "called.");

        if (navigationRail != null && navController != null) {
            NavigationUI.setupWithNavController(navigationRail, navController);

            navigationRail.setOnItemSelectedListener(item -> {
                return handleNavigationItemSelected(item.getItemId());
            });

            // Disable drawer for small landscape
            if (drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }

    /**
     * Setup Landscape Large navigation (NavigationRail + secondary Drawer).
     */
    protected void setupLandscapeLargeNavigation() {
        final String mTAG = "setupLandscapeLargeNavigation: ";
        Log.v(TAG, mTAG + "called.");

        // Setup primary NavigationRail
        if (navigationRail != null && navController != null) {
            NavigationUI.setupWithNavController(navigationRail, navController);

            navigationRail.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_drawer_toggle) {
                    // Open secondary drawer
                    if (drawerLayout != null && drawerNavigation != null) {
                        drawerLayout.open();
                    }
                    return true;
                } else {
                    return handleNavigationItemSelected(item.getItemId());
                }
            });
        }

        // Setup secondary drawer
        if (drawerNavigation != null && navController != null) {
            NavigationUI.setupWithNavController(drawerNavigation, navController);

            drawerNavigation.setNavigationItemSelectedListener(item -> {
                boolean handled = handleNavigationItemSelected(item.getItemId());
                if (handled && drawerLayout != null) {
                    drawerLayout.close(); // Close drawer after navigation
                }
                return handled;
            });

            // Enable drawer for large landscape
            if (drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        }
    }

    /**
     * Handle configuration changes (orientation, screen size).
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final String mTAG = "onConfigurationChanged: ";
        Log.v(TAG, mTAG + "called.");

        Log.d(TAG, mTAG + "Configuration changed - orientation: " + (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? "Landscape" : "Portrait"));

        // Note: Navigation components will be automatically reconfigured
        // when the activity recreates with the new layout
    }

    /**
     * Notify fragments to update data.
     */
    public void notifyUpdates() {
        final String mTAG = "notifyUpdates: ";
        Log.v(TAG, mTAG + "called.");

        try {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {

                // Check for fragment VISIBILITY
                if (fragment instanceof NotifyUpdatesInterface && fragment.isVisible()) {
                    ((NotifyUpdatesInterface) fragment).notifyUpdates();

                    Log.i(TAG, mTAG + "Fragment notified for changes");
                } else {

                    Log.e(TAG, mTAG + "Fragment not found");
                }
            }
        } catch (Exception e) {

            Log.e(TAG, mTAG + "Error during notifyUpdates: " + e.getMessage());
        }
    }

    /**
     * Show/hide loading indicator for long operations.
     */
    private void showLoadingIndicator(boolean show) {
        // Implementation depends on your loading indicator setup
        // This could be a progress bar in the binding, or a custom loading overlay
        final String mTAG = "showLoadingIndicator: ";
        Log.v(TAG, mTAG + "called.");

        try {
            // Example implementation - adjust based on your actual UI
            View loadingOverlay = findViewById(R.id.loading_overlay);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error controlling loading indicator: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume: called.");

        registerTimeChangeReceiver();
        notifyUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause: called.");

        unregisterTimeChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy: called.");

        unregisterTimeChangeReceiver();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTimeChangeReceiver() {
        if (!receiverRegistered && timeChangeReceiver != null) {
            try {
                IntentFilter filter = TimeChangeReceiver.createCriticalIntentFilter();
                registerReceiver(timeChangeReceiver, filter);
                receiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "registerTimeChangeReceiver: Error registering TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    private void unregisterTimeChangeReceiver() {
        if (receiverRegistered && timeChangeReceiver != null) {
            try {
                unregisterReceiver(timeChangeReceiver);
                receiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "unregisterTimeChangeReceiver: Error unregistering TimeChangeReceiver: " + e.getMessage());
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String
            key) {
        Log.d(TAG, "onSharedPreferenceChanged: " + key);
    }
}
