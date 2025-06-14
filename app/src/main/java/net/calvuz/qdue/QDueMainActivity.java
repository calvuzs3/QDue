package net.calvuz.qdue;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.ui.events.EventsActivity;
import net.calvuz.qdue.ui.proto.CalendarDataManagerEnhanced;
import net.calvuz.qdue.ui.proto.MigrationHelper;
import net.calvuz.qdue.ui.settings.QDueSettingsActivity;
import net.calvuz.qdue.ui.shared.BaseActivity;
import net.calvuz.qdue.user.ui.UserProfileActivity;
import net.calvuz.qdue.utils.Log;

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
public class QDueMainActivity extends BaseActivity {
    //TAG
    private static final String TAG = "QDueMainActivity";

    // UI
    protected ActivityQdueMainBinding binding;

    // FAB Management
    private FloatingActionButton fabGoToToday;

    // Navigation State
    private NavigationMode currentNavigationMode;
    private int currentDestination = R.id.nav_calendar; // Default to calendar

    // Toolbar
    private MaterialToolbar toolbar;

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

    private CalendarDataManagerEnhanced enhancedDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String mTAG = "onCreate: ";
        Log.v(TAG, mTAG + "called.");

        // Initialize enhanced data manager if virtual scrolling is enabled
        if (MigrationHelper.shouldUseVirtualScrolling()) {
            enhancedDataManager = CalendarDataManagerEnhanced.getEnhancedInstance();
            Log.d(TAG, "Virtual scrolling enabled for this session");
        } else {
            Log.d(TAG, "Using legacy scrolling for this session");
        }

        // Log device capabilities for monitoring
        logDeviceCapabilities();

        // NORMAL: Use binding
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Detect and setup navigation components
        detectNavigationComponents();
        setupNavigationSafely();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup enhanced data manager
        if (enhancedDataManager != null) {
            enhancedDataManager.shutdown();
        }
    }

    /**
     * Log device capabilities for analytics
     */
    private void logDeviceCapabilities() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
            int apiLevel = android.os.Build.VERSION.SDK_INT;

            Log.d(TAG, "Device info - Max Memory: " + maxMemory + "MB, API Level: " + apiLevel);

            // Send to analytics if you have it
            // Analytics.setProperty("max_memory_mb", String.valueOf(maxMemory));
            // Analytics.setProperty("api_level", String.valueOf(apiLevel));

        } catch (Exception e) {
            Log.w(TAG, "Error logging device capabilities: " + e.getMessage());
        }
    }

    /**
     * Detect which navigation components are available in current layout.
     * This allows the same activity to work with different layout configurations.
     */
    @Override
    protected void detectNavigationComponents() {
        final String mTAG = "detectNavigationComponents: ";

        // Always present in all layouts
        drawerLayout = binding.drawerLayout;

        // Detect available navigation components
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationRail = findViewById(R.id.navigation_rail);
        drawerNavigation = findViewById(R.id.nav_drawer_secondary);
        sidebarNavigation = findViewById(R.id.sidebar_navigation);
        fabGoToToday = findViewById(R.id.fab_go_to_today);
        toolbar = findViewById(R.id.toolbar);

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
        Log.v(TAG, mTAG + "  - Toolbar: " + (toolbar != null));
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

            // Setup FAB based on integration mode - fragments should do it
//            setupFAB();

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
            AppBarLayout appBarLayout = getAppBarLayout();
            MaterialToolbar toolbar = getToolbar();

            if (toolbar != null) {
                // Set toolbar as ActionBar
                setSupportActionBar(toolbar);

                // Configure ActionBar
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    getSupportActionBar().setTitle(getString(R.string.app_name));

                    // IMPORTANTE: Setup menu button per aprire sidebar
//                    getSupportActionBar().setDisplayHomeAsUpEnabled(shouldShowMenuButton());
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

                    Log.d(TAG, mTAG + "getSupportActionBar != null");
                }

                // Configure AppBar visibility
                if (appBarLayout != null) {
                    boolean shouldShow = shouldShowAppBar();
                    appBarLayout.setVisibility(shouldShow ? View.VISIBLE : View.GONE);

                    Log.d(TAG, mTAG + "AppBar visibility: " + (shouldShow ? "VISIBLE" : "GONE"));
                }

                Log.d(TAG, mTAG + "Toolbar configured successfully");

                // Debug: Verifica che il menu sia inflated
                Log.d(TAG, mTAG + "Toolbar menu items count: " + toolbar.getMenu().size());

            } else {
                Log.d(TAG, mTAG + "Toolbar not found in current layout");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error setting up AppBar: " + e.getMessage());
        }
    }

    /**
     * Setup NavController with fallback error handling.
     */
    @Override
    protected void setupNavController() {
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
     * Unified navigation item selection handler for all navigation modes.
     */
    @Override
    protected boolean handleNavigationItemSelected(int itemId) {
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
            } else if (itemId == R.id.nav_events) {
                // Use Intent for settings to maintain existing behavior
                Intent eventsIntent = new Intent(this, EventsActivity.class);
                startActivity(eventsIntent);
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Use Intent for settings to maintain existing behavior
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            } else if (itemId == R.id.nav_about) {
                navController.navigate(R.id.nav_about);
                return true;
            } /*else if (itemId == R.id.nav_user_profile) {
                Log.d(TAG, "nav_user_profile");
                startActivity(new Intent(this, UserProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }*/

            return false;
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error during navigation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Enhanced drawer item selection handler for extended menu (phone portrait).
     */
    @Override
    protected boolean handleExtendedDrawerItemSelected(int itemId) {
        final String mTAG = "handleExtendedDrawerItemSelected: ";
        Log.v(TAG, mTAG + "called with itemId: " + itemId);

        try {
            /*if (itemId == R.id.nav_user_profile) {
                Log.d(TAG, mTAG + "UserProfile");
                Intent userIntent = new Intent(this, UserProfileActivity.class);
                startActivity(userIntent);
                return true;

            } else if (itemId == R.id.nav_eventi) {
                // TODO: Replace with your actual Eventi Activity
                Intent eventiIntent = new Intent(this, EventiActivity.class);
                startActivity(eventiIntent);
                return true;

            } else*/
            if (itemId == R.id.nav_settings) {
                Log.d(TAG, mTAG + "Settings");
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            } else if (itemId == R.id.nav_about) {
                Log.d(TAG, mTAG + "About");
                if (navController != null) {
                    navController.navigate(R.id.nav_about);
                }
                return true;
            } else if (itemId == R.id.nav_events) {
                // Use Intent for settings to maintain existing behavior
                Intent eventsIntent = new Intent(this, EventsActivity.class);
                startActivity(eventsIntent);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error handling extended drawer item: " + e.getMessage());
            return false;
        }
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
    public void onFragmentOperationComplete(String operationType, boolean success, Bundle
            resultData) {
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
            View loadingOverlay = findViewById(R.id.loading_overlay);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
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
        Log.d(TAG, "shouldShowAppBar: " + !isLandscape);
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

//            if (shouldShowMenu && !hasExtendedDrawer) {
            if (shouldShowMenu) {
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
                Log.d(TAG, mTAG + "Home selected");
                toggleSidebar();
                return true;
            }
/*
            // Handle home/up button (menu button)
            if (id == android.R.id.home) {
                Log.d(TAG, mTAG + "UserProfile selected");
                if (navController != null) {
                    navController.navigate(R.id.nav_user_profile);
                }
                return true;
            }*/

            // Handle other menu items
            if (id == R.id.action_settings) {
                Log.d(TAG, mTAG + "Settings selected");
                Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }

            if (id == R.id.action_about) {
                Log.d(TAG, mTAG + "About selected");
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
     * Drawer toggle
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
     * Show alternative menu for portrait mode
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
            // showQuickOptionsDialog();

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
     * Get reference to FAB for fragment control (when not integrated).
     */
    public FloatingActionButton getFabGoToToday() {
        return fabGoToToday;
    }
}