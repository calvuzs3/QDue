package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.ui.dayslist.DayslistViewFragment;
import net.calvuz.qdue.ui.settings.QDueSettingsActivity;
import net.calvuz.qdue.ui.shared.FragmentCommunicationInterface;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.TimeChangeReceiver;

/**
 * Enhanced Main Activity with hybrid NavigationRail system.
 * <p>
 * Navigation Strategy:
 * - Phone Portrait: BottomNavigation + FAB separato
 * - Tablet Portrait: NavigationRail + FAB integrato
 * - Landscape Small: NavigationRail espanso + Extended FAB
 * - Landscape Large: NavigationRail + Drawer secondario
 * <p>
 * Features:
 * - Dynamic navigation component detection
 * - Seamless orientation change handling
 * - Material 3 compliant navigation patterns
 * - Unified FAB management across all configurations
 */
public class QDueMainActivity extends AppCompatActivity
        implements
        FragmentCommunicationInterface,
        TimeChangeReceiver.TimeChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QDueMainActivity";

    // UI related
    private AppBarConfiguration appBarConfiguration;
    private ActivityQdueMainBinding binding;
    private NavController navController;

    // Navigation Components (only one will be active based on layout)
    private BottomNavigationView bottomNavigation;
    private NavigationRailView navigationRail;
    private NavigationView drawerNavigation;
    private DrawerLayout drawerLayout;
    private NavigationView sidebarNavigation;

    // FAB Management
    private FloatingActionButton fabGoToToday;
    private boolean isFabIntegrated = false; // Whether FAB is integrated in NavigationRail

    // Navigation State
    private NavigationMode currentNavigationMode;
    private int currentDestination = R.id.nav_calendar; // Default to calendar

    // Time Change Receiver
    private TimeChangeReceiver timeChangeReceiver;
    private boolean receiverRegistered = false;

    // Shared Preferences
    private SharedPreferences sharedPreferences;

    /**
     * Enum to track current navigation mode for proper handling
     */
    private enum NavigationMode {
        PHONE_PORTRAIT,      // BottomNavigation + FAB separato
        TABLET_PORTRAIT,     // NavigationRail + FAB integrato
        LANDSCAPE_SMALL,     // NavigationRail espanso + Extended FAB
        LANDSCAPE_LARGE      // NavigationRail + Drawer
    }

    // ======================= METHODS ============================

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

        // NORMAL: Use binding
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Detect and setup navigation components
        detectNavigationComponents();
        setupNavigationSafely();
    }

    /**
     * Detect which navigation components are available in current layout.
     * This allows the same activity to work with different layout configurations.
     */
    private void detectNavigationComponents() {
        final String mTAG = "detectNavigationComponents: ";

        // Always present in all layouts
        drawerLayout = binding.drawerLayout;

        // Detect available navigation components
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationRail = findViewById(R.id.navigation_rail);
        drawerNavigation = findViewById(R.id.nav_drawer_secondary);
        sidebarNavigation = findViewById(R.id.sidebar_navigation);
        fabGoToToday = findViewById(R.id.fab_go_to_today);

        // NEW: Extended drawer for phone portrait
        NavigationView extendedDrawer = findViewById(R.id.nav_drawer_extended);
        if (extendedDrawer != null) {
            drawerNavigation = extendedDrawer; // Use extended drawer as primary drawer
        }

        // Determine current navigation mode based on available components
        currentNavigationMode = determineNavigationMode();

        // LOGGING
        Log.v(TAG, mTAG + "Navigation mode: " + currentNavigationMode);
        Log.v(TAG, mTAG + "Available components:");
        Log.v(TAG, mTAG + "  - BottomNavigation: " + (bottomNavigation != null));
        Log.v(TAG, mTAG + "  - NavigationRail: " + (navigationRail != null));
        Log.v(TAG, mTAG + "  - DrawerNavigation: " + (drawerNavigation != null));
        Log.v(TAG, mTAG + "  - ExtendedDrawer: " + (extendedDrawer != null));
        Log.v(TAG, mTAG + "  - SidebarNavigation: " + (sidebarNavigation != null));
        Log.v(TAG, mTAG + "  - FAB: " + (fabGoToToday != null));
    }

    /**
     * Determine navigation mode based on available components and screen configuration.
     */
    private NavigationMode determineNavigationMode() {
        Configuration config = getResources().getConfiguration();
        int screenWidthDp = config.screenWidthDp;
        int smallestScreenWidthDp = config.smallestScreenWidthDp;
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (bottomNavigation != null && drawerLayout != null && !isLandscape) {
            // Phone portrait with BottomNavigation + DrawerLayout
            return NavigationMode.PHONE_PORTRAIT;
        } else if (bottomNavigation != null) {
            // Fallback phone portrait
            return NavigationMode.PHONE_PORTRAIT;
        } else if (navigationRail != null && drawerLayout != null && !isLandscape) {
            // Tablet portrait with NavigationRail + DrawerLayout
            return NavigationMode.TABLET_PORTRAIT;
        } else if (sidebarNavigation != null) {
            // Landscape with fixed sidebar
            if (isLandscape && screenWidthDp >= 840) {
                return NavigationMode.LANDSCAPE_LARGE;
            } else {
                return NavigationMode.LANDSCAPE_SMALL;
            }
        } else if (navigationRail != null) {
            // NavigationRail without drawer (landscape small)
            return NavigationMode.LANDSCAPE_SMALL;
        } else {
            // Fallback
            return NavigationMode.PHONE_PORTRAIT;
        }
    }

    /**
     * Setup navigation safely with error handling and mode-specific configuration.
     */
    private void setupNavigationSafely() {
        try {
            // Setup NavController first
            setupNavController();

            // Setup AppBar and Toolbar
            setupAppBarSafe();

            // Configure navigation based on current mode
            switch (currentNavigationMode) {
                case PHONE_PORTRAIT:
                    setupPhonePortraitNavigation();
                    break;
                case TABLET_PORTRAIT:
                    setupTabletPortraitNavigation();
                    break;
                case LANDSCAPE_SMALL:
                    setupLandscapeSmallNavigation();
                    break;
                case LANDSCAPE_LARGE:
                    setupLandscapeLargeNavigation();
                    break;
            }

            // Setup FAB based on integration mode
            setupFAB();

        } catch (Exception e) {
            Log.e(TAG, "setupNavigationSafely: Error setting up navigation: " + e.getMessage());
        }
    }

    /**
     * NUOVO: Setup AppBar with safe binding access
     */
    private void setupAppBarSafe() {
        final String mTAG = "setupAppBarSafe: ";
        Log.v(TAG, mTAG + "called.");

        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = getToolbar();
            com.google.android.material.appbar.AppBarLayout appBarLayout = getAppBarLayout();

            if (toolbar != null) {
                // Set toolbar as ActionBar
                setSupportActionBar(toolbar);

                // Configure ActionBar
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    getSupportActionBar().setTitle(getString(R.string.app_name));

                    // IMPORTANTE: Setup menu button per aprire sidebar
                    getSupportActionBar().setDisplayHomeAsUpEnabled(shouldShowMenuButton());
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
                }

                // Configure AppBar visibility
                if (appBarLayout != null) {
                    boolean shouldShow = shouldShowAppBar();
                    appBarLayout.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
                    Log.d(TAG, mTAG + "AppBar visibility: " + (shouldShow ? "VISIBLE" : "GONE"));
                }

                Log.d(TAG, mTAG + "Toolbar configured successfully");
            } else {
                Log.d(TAG, mTAG + "Toolbar not found in current layout");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error setting up AppBar: " + e.getMessage());
        }
    }

    /**
     * Check if menu button should be shown
     */
    private boolean shouldShowMenuButton() {
        Configuration config = getResources().getConfiguration();
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Show menu button in portrait when we have sidebar available
        // or when we want to toggle sidebar visibility
//        return !isLandscape && (findViewById(R.id.sidebar_navigation) != null ||
//                currentNavigationMode == NavigationMode.PHONE_PORTRAIT);

        // Show menu button when we have a drawer available
        return drawerLayout != null && drawerNavigation != null && !isLandscape;
    }

    /**
     * Setup NavController with fallback error handling.
     */
    private void setupNavController() {
        final String mTAG = "setupNavController: ";
        Log.v(TAG, mTAG + "called.");

        try {
            // Method 1: Find NavHostFragment directly
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d(TAG, mTAG + "NavController found via NavHostFragment");
            } else {
                // Method 2: Fallback with Navigation.findNavController
                navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                Log.d(TAG, mTAG + "NavController found via Navigation.findNavController");
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting up NavController: " + e.getMessage());

            // Retry after small delay
            findViewById(R.id.nav_host_fragment_content_main).post(() -> {
                try {
                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    Log.d(TAG, mTAG + "NavController found on second attempt");
                } catch (Exception retryException) {
                    Log.e(TAG, mTAG + "Failed to find NavController on retry: " + retryException.getMessage());
                }
            });
        }
    }

    /**
     * Setup Phone Portrait navigation (BottomNavigation + separate FAB).
     */
    private void setupPhonePortraitNavigation() {
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


//        isFabIntegrated = false; // FAB is separate in phone portrait
        isFabIntegrated = true; // FAB is separate in phone portrait
    }

    /**
     * Setup Tablet Portrait navigation (NavigationRail + integrated FAB).
     */
    private void setupTabletPortraitNavigation() {
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

        isFabIntegrated = true; // FAB is integrated in NavigationRail
    }

    /**
     * Setup Landscape Small navigation (NavigationRail expanded + Extended FAB).
     */
    private void setupLandscapeSmallNavigation() {
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

        isFabIntegrated = true; // Extended FAB is integrated in NavigationRail
    }

    /**
     * Setup Landscape Large navigation (NavigationRail + secondary Drawer).
     */
    private void setupLandscapeLargeNavigation() {
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

        isFabIntegrated = true; // FAB is integrated in NavigationRail
    }

    /**
     * Unified navigation item selection handler for all navigation modes.
     */
    private boolean handleNavigationItemSelected(int itemId) {
        final String mTAG = "handleNavigationItemSelected: ";
        Log.v(TAG, mTAG + "called.");

        if (navController == null) {
            Log.e(TAG, mTAG + "NavController is null, cannot handle navigation");
            return false;
        }

        try {
            currentDestination = itemId;

            if (itemId == R.id.nav_calendar) {
                navController.navigate(R.id.nav_calendar);
                return true;
            } else if (itemId == R.id.nav_dayslist) {
                navController.navigate(R.id.nav_dayslist);
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Use Intent for settings to maintain existing behavior
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            } else if (itemId == R.id.nav_about) {
                navController.navigate(R.id.nav_about);
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                // Navigate to user profile if implemented
                // navController.navigate(R.id.nav_user_profile);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during navigation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Enhanced drawer item selection handler for extended menu (phone portrait).
     */
    private boolean handleExtendedDrawerItemSelected(int itemId) {
        final String mTAG = "handleExtendedDrawerItemSelected: ";
        Log.v(TAG, mTAG + "called with itemId: " + itemId);

        try {
            if (itemId == R.id.nav_user_profile) {
                // TODO: Replace with your actual User Profile Activity
//                Intent userIntent = new Intent(this, UserProfileActivity.class);
//                startActivity(userIntent);
                return true;

            } else if (itemId == R.id.nav_eventi) {
                // TODO: Replace with your actual Eventi Activity
//                Intent eventiIntent = new Intent(this, EventiActivity.class);
//                startActivity(eventiIntent);
                return true;

            } else if (itemId == R.id.nav_settings) {
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            } else if (itemId == R.id.nav_about) {
                if (navController != null) {
                    navController.navigate(R.id.nav_about);
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error handling extended drawer item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Setup FAB based on integration mode and current navigation configuration.
     */
    private void setupFAB() {
        final String mTAG = "setupFAB: ";
        Log.v(TAG, mTAG + "called.");

        if (fabGoToToday == null) {
            Log.d(TAG, mTAG + "FAB not found in current layout");
            return;
        }

        // Configure FAB click listener
        fabGoToToday.setOnClickListener(v -> {
            try {
                // Communicate with current fragment to scroll to today
                Fragment currentFragment = getCurrentFragment();
                if (currentFragment != null) {
                    if (currentFragment instanceof DayslistViewFragment) {
                        ((DayslistViewFragment) currentFragment).scrollToToday();
                    } else if (currentFragment instanceof net.calvuz.qdue.ui.calendar.CalendarViewFragment) {
                        ((net.calvuz.qdue.ui.calendar.CalendarViewFragment) currentFragment).scrollToToday();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error handling FAB click: " + e.getMessage());
            }
        });

        // Configure FAB visibility based on integration mode
        if (isFabIntegrated) {
            // FAB is integrated in NavigationRail - always visible
            fabGoToToday.show();
            Log.d(TAG, mTAG + "FAB configured as integrated (always visible)");
        } else {
            // FAB is separate - managed by fragments
            fabGoToToday.hide(); // Start hidden, fragments will control visibility
            Log.d(TAG, mTAG + "FAB configured as separate (fragment-controlled)");
        }
    }

    /**
     * Get current fragment from NavHostFragment.
     */
    private Fragment getCurrentFragment() {
        final String mTAG = "getCurrentFragment: ";
        Log.v(TAG, mTAG + "called.");

        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                return navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error getting current fragment: " + e.getMessage());
        }
        return null;
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
    private void notifyUpdates() {
        final String mTAG = "notifyUpdates: ";
        Log.v(TAG, mTAG + "called.");

        try {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (fragment != null) {
                if (fragment instanceof NavHostFragment) {
                    Fragment childFragment = fragment.getChildFragmentManager().getPrimaryNavigationFragment();

                    if (childFragment instanceof DayslistViewFragment) {
                        ((DayslistViewFragment) childFragment).notifyUpdates();
                    }
                }
            } else {
                Log.e(TAG, mTAG + "Fragment not found");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during notifyUpdates: " + e.getMessage());
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.d(TAG, "onSharedPreferenceChanged: " + key);
    }

    // ========== COMMUNICATION INTERFACE IMPLEMENTATION ==========

    @Override
    public void onFragmentNavigationRequested(int destinationId, Bundle data) {
        final String mTAG = "onFragmentNavigationRequested: ";
        Log.v(TAG, mTAG + "called.");

        try {
            if (navController != null) {
                if (data != null) {
                    navController.navigate(destinationId, data);
                } else {
                    navController.navigate(destinationId);
                }
                Log.d(TAG, mTAG + " Navigation to: " + destinationId);
            } else {
                Log.e(TAG, mTAG + " NavController is null, cannot navigate");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + " Error during navigation: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentTitleChanged(String title) {
        final String mTAG = "onFragmentTitleChanged: ";
        Log.v(TAG, mTAG + "called.");

        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                Log.v(TAG, mTAG + "Updated toolbar title to: " + title);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error updating toolbar title: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentStatusMessage(String message, boolean isError) {
        final String mTAG = "onFragmentStatusMessage: ";
        Log.v(TAG, mTAG + "called.");

        try {
            if (isError) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } else {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
            Log.d(TAG, mTAG + "Displayed status message: " + message);
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error showing status message: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentMenuChanged(int menuId) {
        final String mTAG = "onFragmentMenuChanged: ";
        Log.v(TAG, mTAG + "called.");

        // Invalidate options menu to trigger onCreateOptionsMenu with new menu
        try {
            invalidateOptionsMenu();
            Log.d(TAG, mTAG + "Invalidated options menu for menuId: " + menuId);
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error changing fragment menu: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentToolbarVisibilityChanged(boolean visible) {
        final String mTAG = "onFragmentToolbarVisibilityChanged: ";
        Log.v(TAG, mTAG + "called.");

        try {
            com.google.android.material.appbar.AppBarLayout appBarLayout = getAppBarLayout();

            if (appBarLayout != null && shouldShowAppBar()) {
                appBarLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
                Log.d(TAG, mTAG + "Set toolbar visibility: " + visible);
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error changing toolbar visibility: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentOperationComplete(String operationType, boolean success, Bundle resultData) {
        final String mTAG = "onFragmentOperationComplete: ";
        Log.v(TAG, mTAG + "called.");

        try {
            Log.d(TAG, mTAG + "Operation completed: " + operationType + ", success: " + success);

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
            Log.e(TAG, mTAG + "Error handling operation completion: " + e.getMessage());
        }
    }

    @Override
    public void onFragmentCustomAction(String action, Bundle data) {
        final String mTAG = "onFragmentCustomAction: ";
        Log.v(TAG, mTAG + "called.");

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
                    Log.d(TAG, mTAG + "Unhandled custom action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error handling custom action '" + action + "': " + e.getMessage());
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
//            View loadingOverlay = findViewById(R.id.loading_overlay);
//            if (loadingOverlay != null) {
//                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
//            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error controlling loading indicator: " + e.getMessage());
        }
    }

    /**
     * Handle FAB visibility if activity needs to coordinate with fragment FABs.
     */
    private void handleFabVisibility(boolean visible) {
        // Implementation depends on whether activity has its own FABs to coordinate
        // For now, this is mainly handled by individual fragments
        final String mTAG = "handleFabVisibility: ";
        Log.v(TAG, mTAG + "called.");

        Log.d(TAG, mTAG + "FAB visibility coordination: " + visible);
    }

    // ==================== 1. FIX BINDING APPBAR ====================

/**
 * AGGIUNGERE: Safe AppBar access methods
 * Aggiungi questi metodi alla classe QDueMainActivity
 */

    /**
     * Safe access to AppBar components that may not exist in all layouts
     */
    private com.google.android.material.appbar.AppBarLayout getAppBarLayout() {
        // Try direct binding first
        if (binding != null) {
            try {
                // AppBar could be in binding if moved back to fragments
                return (com.google.android.material.appbar.AppBarLayout) binding.getRoot()
                        .findViewById(R.id.appbar);
            } catch (Exception e) {
                // AppBar not in binding, try activity findViewById
            }
        }

        // Fallback to activity findViewById (for AppBar in activity layout)
        return findViewById(R.id.appbar);
    }

    /**
     * Safe access to Toolbar
     */
    private com.google.android.material.appbar.MaterialToolbar getToolbar() {
        // Try direct binding first
        if (binding != null) {
            try {
                return (com.google.android.material.appbar.MaterialToolbar) binding.getRoot()
                        .findViewById(R.id.toolbar);
            } catch (Exception e) {
                // Toolbar not in binding, try activity findViewById
            }
        }

        // Fallback to activity findViewById
        return findViewById(R.id.toolbar);
    }

    /**
     * Check if AppBar should be visible based on orientation and layout
     */
    private boolean shouldShowAppBar() {
        Configuration config = getResources().getConfiguration();
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Show AppBar only in portrait mode
        return !isLandscape;
    }

    // ==================== 3. AGGIUNGERE MENU HANDLING ====================

    /**
     * AGGIUNGERE: Menu creation and handling
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final String mTAG = "onCreateOptionsMenu: ";

        try {
            boolean shouldShowMenu = shouldShowAppBar() && getToolbar() != null;
            boolean hasExtendedDrawer = drawerLayout != null && drawerNavigation != null;

            if (shouldShowMenu && !hasExtendedDrawer) {
                // Show toolbar menu only if we don't have extended drawer
                getMenuInflater().inflate(R.menu.toolbar_menu, menu);
                Log.d(TAG, mTAG + "Menu created for mode without drawer");
                return true;
            }

            // No menu when drawer is available (drawer replaces toolbar menu)
            Log.d(TAG, mTAG + "Menu not created - drawer handles extended options");
            return super.onCreateOptionsMenu(menu);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error creating menu: " + e.getMessage());
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final String mTAG = "onOptionsItemSelected: ";

        try {
            int id = item.getItemId();

            // Handle home/up button (menu button)
            if (id == android.R.id.home) {
                Log.d(TAG, mTAG + "Menu button clicked");
                toggleSidebar();
                return true;
            }

            // Handle other menu items
            if (id == R.id.action_settings) {
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }

            if (id == R.id.action_about) {
                if (navController != null) {
                    navController.navigate(R.id.nav_about);
                }
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error handling menu selection: " + e.getMessage());
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * FIX IMMEDIATO: DrawerLayout Error - No Left Drawer
     *
     * Sostituire il metodo toggleSidebar() in QDueMainActivity.java
     */

    /**
     * SOSTITUIRE: toggleSidebar method
     */
    private void toggleSidebar() {
        final String mTAG = "toggleSidebar: ";
        Log.v(TAG, mTAG + "called.");

        try {
            if (drawerLayout != null && drawerNavigation != null) {
                // CORREZIONE: Apri/chiudi effettivamente il drawer
                if (drawerLayout.isDrawerOpen(drawerNavigation)) {
                    drawerLayout.closeDrawer(drawerNavigation);
                } else {
                    drawerLayout.openDrawer(drawerNavigation);
                }
                return;
            }

            // Fallback se non c'è drawer
            showPortraitMenu();

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error toggling sidebar: " + e.getMessage());
        }
    }

    /**
     * NUOVO: Show alternative menu for portrait mode
     */
    private void showPortraitMenu() {
        final String mTAG = "showPortraitMenu: ";

        try {
            // Opzione 1: Mostra messaggio informativo
            onFragmentStatusMessage("Use bottom navigation to switch views", false);

            // Opzione 2: Naviga direttamente ai settings (se preferisci)
            // Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
            // startActivity(settingsIntent);

            // Opzione 3: Mostra dialogo con opzioni rapide (se implementi in futuro)
//             showQuickOptionsDialog();

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error showing portrait menu: " + e.getMessage());
        }
    }

    // === PUBLIC METHODS FOR FRAGMENT INTERACTION ===

    /**
     * Get current navigation mode for fragments that need to adapt their behavior.
     */
    public NavigationMode getCurrentNavigationMode() {
        return currentNavigationMode;
    }

    /**
     * Check if FAB is integrated in NavigationRail or separate.
     */
    public boolean isFabIntegrated() {
        return isFabIntegrated;
    }

    /**
     * Get reference to FAB for fragment control (when not integrated).
     */
    public FloatingActionButton getFabGoToToday() {
        return fabGoToToday;
    }
}