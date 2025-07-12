package net.calvuz.qdue.ui.architecture.base;

import android.annotation.SuppressLint;
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
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigationrail.NavigationRailView;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.events.interfaces.EventsRefreshInterface;
import net.calvuz.qdue.ui.shared.interfaces.FragmentCommunicationInterface;
import net.calvuz.qdue.ui.shared.interfaces.NotifyUpdatesInterface;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.TimeChangeReceiver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base Activity for all QDue activities.
 * implements:
 * - FragmentCommunicationInterface
 * - TimeChangeReceiver.TimeChangeListener
 * - SharedPreferences.OnSharedPreferenceChangeListener
 */
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

    // Fragment registration system
    protected final Set<EventsRefreshInterface> mRegisteredEventsFragments =
            Collections.synchronizedSet(new HashSet<>());

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

    // ===========================================================

    /**
     * Register a fragment for events refresh notifications
     * Called by fragments in their onResume()
     */
    protected void registerEventsRefreshFragment(EventsRefreshInterface fragment) {
        if (fragment != null) {
            mRegisteredEventsFragments.add(fragment);
            Log.d(TAG, String.format(QDue.getLocale(), "Registered fragment: %s (Total: %d)",
                    fragment.getFragmentDescription(), mRegisteredEventsFragments.size()));
        }
    }

    /**
     * Unregister a fragment from events refresh notifications
     * Called by fragments in their onPause()
     */
    protected void unregisterEventsRefreshFragment(EventsRefreshInterface fragment) {
        if (fragment != null) {
            boolean removed = mRegisteredEventsFragments.remove(fragment);
            Log.d(TAG, String.format(QDue.getLocale(), "Unregistered fragment: %s (Removed: %s, Total: %d)",
                    fragment.getFragmentDescription(), removed, mRegisteredEventsFragments.size()));
        }
    }

    /**
     * Clear all registered fragments (on activity destroy)
     */
    private void clearAllRegisteredFragments() {
        Log.d(TAG, String.format(QDue.getLocale(), "Clearing %d registered fragments", mRegisteredEventsFragments.size()));
        mRegisteredEventsFragments.clear();
    }

    /**
     * Initialize TimeChangeReceiver and SharedPreferences.
     *
     * @param savedInstanceState bundle
     */
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
     * Register TimeChangeReceiver
     * (#notify updates on resume).
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume: called.");

        registerTimeChangeReceiver();

        // Try not to call it
//        notifyUpdates();
    }

    /**
     * Unregister TimeChangeReceiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause: called.");

        unregisterTimeChangeReceiver();
    }

    /**
     * Unregister TimeChangeReceiver
     * Unregister SharedPreferences
     * Unregister registered fragments
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy: called.");

        unregisterTimeChangeReceiver();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        // Clear registered fragments
        clearAllRegisteredFragments();
    }

    // ===========================================================

    /**
     * Setup Navigation Controller with fallback error handling.
     */
    protected void setupPhonePortraitNavigation() {
        Log.v(TAG, "setupPhonePortraitNavigation: called.");

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
        Log.v(TAG, "setupTabletPortraitNavigation: called.");

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
        Log.v(TAG, "setupLandscapeSmallNavigation: called.");

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

    // ===========================================================

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

                    Log.d(TAG, mTAG + "✅ Fragment notified for changes");
                } else {

                    Log.e(TAG, mTAG + "❌ Fragment not found");
                }
            }
        } catch (Exception e) {

            Log.e(TAG, mTAG + "❌ Error during notifyUpdates: " + e.getMessage());
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
            Log.e(TAG, mTAG + "❌ Error controlling loading indicator: " + e.getMessage());
        }
    }

    // ===========================================================

    /**
     * Register TimeChangeReceiver
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTimeChangeReceiver() {
        if (!receiverRegistered && timeChangeReceiver != null) {
            try {
                IntentFilter filter = TimeChangeReceiver.createCriticalIntentFilter();
                registerReceiver(timeChangeReceiver, filter);
                receiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "❌ registerTimeChangeReceiver: Error registering TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    /**
     * Unregister TimeChangeReceiver
     */
    private void unregisterTimeChangeReceiver() {
        if (receiverRegistered && timeChangeReceiver != null) {
            try {
                unregisterReceiver(timeChangeReceiver);
                receiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "❌ unregisterTimeChangeReceiver: Error unregistering TimeChangeReceiver: " + e.getMessage());
            }
        }
    }

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onTimeChanged() {
        Log.d(TAG, "onTimeChanged: ✅");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "System time updated", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onDateChanged() {
        Log.d(TAG, "onDateChanged: ✅");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "System date updated", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onTimezoneChanged() {
        Log.d(TAG, "onTimezoneChanged: ✅");
        runOnUiThread(() -> {
            notifyUpdates();
            Toast.makeText(this, "Timezone updated", Toast.LENGTH_SHORT).show();
        });
    }

    // ===========================================================

    /**
     * Handle configuration changes (orientation, screen size).
     *
     * @param newConfig New configuration
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final String mTAG = "onConfigurationChanged: ";
        Log.v(TAG, mTAG + "called.");

        Log.d(TAG, mTAG + "onConfigurationChanged: ✅ orientation: " + (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? "Landscape" : "Portrait"));

        // Note: Navigation components will be automatically reconfigured
        // when the activity recreates with the new layout
    }

    // ===========================================================

    /**
     * SharedPreferences.OnSharedPreferenceChangeListener implementation
     *
     * @param sharedPreferences SharedPreferences
     * @param key               Paramenter
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String
            key) {
        Log.d(TAG, "onSharedPreferenceChanged: ✅  " + key);
    }
}
