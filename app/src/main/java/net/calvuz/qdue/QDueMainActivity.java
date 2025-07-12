package net.calvuz.qdue;

import static net.calvuz.qdue.QDue.SETTINGS_REQUEST_CODE;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.databinding.ActivityQdueMainBinding;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.features.events.presentation.EventsActivity;
import net.calvuz.qdue.ui.features.events.interfaces.EventsRefreshInterface;
import net.calvuz.qdue.ui.proto.CalendarDataManagerEnhanced;
import net.calvuz.qdue.ui.proto.MigrationHelper;
import net.calvuz.qdue.ui.features.settings.QDueSettingsActivity;
import net.calvuz.qdue.ui.core.architecture.base.BaseActivity;
import net.calvuz.qdue.ui.core.common.enums.NavigationMode;
import net.calvuz.qdue.ui.features.welcome.presentation.WelcomeActivity;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // First Timme we launch the activity
    private boolean mIsFirstLaunchApp = true;

    // UI
    protected ActivityQdueMainBinding binding;

    // FAB Management
    private FloatingActionButton fabGoToToday;

    // Navigation State
    private NavigationMode currentNavigationMode;
    private int currentDestination = R.id.nav_calendar; // Default to calendar

    // Toolbar
    private MaterialToolbar toolbar;


    // Enhanced activity launcher
    private ActivityResultLauncher<Intent> mEventsActivityLauncher;

    // ======================= METHODS ============================

    private CalendarDataManagerEnhanced enhancedDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: called.");

        // Check if user needs to see welcome before setting up main activity
        if (shouldRedirectToWelcome()) {
            redirectToWelcome();
            return;
        }

        // Setup view mode preferences before calling super
        setupDefaultViewModePreferences();

        // Call existing onCreate
        super.onCreate(savedInstanceState);

        // Initialize enhanced data manager if virtual scrolling is enabled
        if (MigrationHelper.shouldUseVirtualScrolling()) {
            enhancedDataManager = CalendarDataManagerEnhanced.getEnhancedInstance();
            Log.d(TAG, "=== Virtual scrolling enabled for this session");
        } else {
            Log.d(TAG, "=== Legacy scrolling enabled for this session");
        }

        // Log device capabilities for monitoring
        logDeviceCapabilities();

        // Use binding
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Detect and setup navigation components
        detectNavigationComponents();
        setupNavigationSafely();

        // Setup Events Activity Laucher
        setupActivityLaunchers();
    }

    /**
     * Handle returning from other activities (like WelcomeActivity)
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Check if we need to redirect to welcome (edge case)
        if (shouldRedirectToWelcome()) {
            redirectToWelcome();
            return;
        }

        // TODO: Handle view mode changes from settings onl first time
        // Verify navigation state matches preferences
        verifyNavigationStateMatchesPreferences();
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
     * Handle extras from WelcomeActivity:
     */
    private void handleWelcomeIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("from_welcome", false)) {
            String selectedViewMode = intent.getStringExtra("selected_view_mode");
            Log.d(TAG, "Started from WelcomeActivity with view mode: " + selectedViewMode);

            // Optional: Show welcome completion message
            if (findViewById(android.R.id.content) != null) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Benvenuto in QDue!", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== PREFERENCES METHODS ====================

    /**
     * Check if user should be redirected to WelcomeActivity
     * @return true if welcome should be shown
     */
    private boolean shouldRedirectToWelcome() {
        boolean shouldShow = QDuePreferences.shouldShowWelcome(this);
        Log.d(TAG, "Should redirect to welcome: " + shouldShow);
        return shouldShow;
    }

    /**
     * Redirect user to WelcomeActivity for initial setup
     */
    private void redirectToWelcome() {
        Log.d(TAG, "Redirecting to WelcomeActivity for first-time setup");

        Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish(); // Close main activity

        // Add smooth transition
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Setup default view mode preferences and ensure navigation graph compatibility
     * This method prepares the navigation system based on user preferences
     */
    private void setupDefaultViewModePreferences() {
        final String methodTag = TAG + ".setupDefaultViewModePreferences";
        Log.d(methodTag, "Setting up view mode preferences");

        try {
            // Initialize defaults only if needed (respects WelcomeActivity choices)
            QDuePreferences.initializeDefaultsIfNeeded(this);

            // Log current preferences for debugging
            if (QDue.Debug.DEBUG_ACTIVITY) {
                QDuePreferences.logAllPreferences(this);
            }

            // Get the preferred start destination
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination(this);
            String viewMode = QDuePreferences.getDefaultViewMode(this);

            Log.d(methodTag, "User preferred view mode: " + viewMode);
            Log.d(methodTag, "Target navigation destination: " + preferredDestination);

            // Store for later use in navigation setup
            currentDestination = preferredDestination;

        } catch (Exception e) {
            Log.e(methodTag, "Error setting up view mode preferences", e);
            // Fallback to calendar view
            currentDestination = R.id.nav_calendar;
            QDuePreferences.setDefaultViewMode(this, QDue.Settings.VIEW_MODE_CALENDAR);
        }
    }

    // ==================== USUAL METHODS ====================

    /**
     * Verify that current navigation state matches user preferences
     * This handles cases where preferences might have changed externally
     */
    private void verifyNavigationStateMatchesPreferences() {
        if (navController == null) return;
        if (mIsFirstLaunchApp) {
            mIsFirstLaunchApp = false;
            return;
        }

        try {
            String currentViewMode = QDuePreferences.getDefaultViewMode(this);
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination(this);
            int currentDestination = navController.getCurrentDestination().getId();

            // Only auto-navigate if we're on a main view (not detail/settings/etc)
            if (isMainNavigationDestination(currentDestination) &&
                    currentDestination != preferredDestination) {

                Log.d(TAG, "Navigation state mismatch - auto-correcting to preferred view");
                navController.navigate(preferredDestination);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error verifying navigation state", e);
        }
    }

    /**
     * Check if the given destination ID is a main navigation destination
     * @param destinationId Navigation destination ID to check
     * @return true if it's a main destination (calendar or dayslist)
     */
    private boolean isMainNavigationDestination(int destinationId) {
        return destinationId == R.id.nav_calendar || destinationId == R.id.nav_dayslist;
    }

    /// ////////

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
        Log.v(TAG, mTAG + "called with preferred destination: " + currentDestination);


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

            // Configure start destination based on user preference
            configureNavigationStartDestination();

        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting up NavController: " + e.getMessage());

            // Retry after small delay
            findViewById(R.id.nav_host_fragment_content_main).post(() -> {
                try {
                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    configureNavigationStartDestination();
                    Log.d(TAG, mTAG + "NavController found on second attempt");
                } catch (Exception retryException) {
                    Log.e(TAG, mTAG + "Failed to find NavController on retry: " + retryException.getMessage());
                }
            });
        }
    }


    /**
     * Configure the navigation graph start destination based on user preferences
     * This modifies the navigation graph dynamically
     */
    private void configureNavigationStartDestination() {
        final String mTAG = "configureNavigationStartDestination: ";

        if (navController == null) {
            Log.w(TAG, mTAG
                    + "NavController is null, cannot configure start destination");
            return;
        }

        try {
            // Get user's preferred view mode
            String preferredViewMode = QDuePreferences.getDefaultViewMode(this);

            // Get the current navigation graph
            androidx.navigation.NavGraph navGraph = navController.getNavInflater()
                    .inflate(R.navigation.mobile_navigation);

            // Determine start destination based on preference
            int startDestinationId;
            if (QDue.Settings.VIEW_MODE_DAYSLIST.equals(preferredViewMode)) {
                startDestinationId = R.id.nav_dayslist;
                Log.d(TAG, mTAG + "Setting start destination to DaysList");
            } else {
                startDestinationId = R.id.nav_calendar;
                Log.d(TAG, mTAG + "Setting start destination to Calendar (default)");
            }

            // Apply the start destination to the graph
            navGraph.setStartDestination(startDestinationId);
            navController.setGraph(navGraph);

            // Update current destination tracking
            currentDestination = startDestinationId;

            Log.d(TAG, mTAG + "Navigation graph configured with start destination: " + startDestinationId);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error configuring navigation start destination", e);
            // Let the navigation graph use its default start destination
        }
    }

    // ==================== HANDLE VIEW MODE CHANGES FROM SETTINGS ====================

    /**
     * Handle navigation when user changes view mode from settings
     * Call this method when user changes view mode in settings
     * @param newViewMode The new view mode selected by user
     */
    public void onViewModeChanged(String newViewMode) {
        final String mTAG = "onViewModeChanged: ";
        Log.d(TAG, mTAG + "View mode changed to: " + newViewMode);

        try {
            // Save the new preference
            QDuePreferences.setDefaultViewMode(this, newViewMode);

            // Navigate to the selected view
            int destinationId;
            if (QDue.Settings.VIEW_MODE_DAYSLIST.equals(newViewMode)) {
                destinationId = R.id.nav_dayslist;
            } else {
                destinationId = R.id.nav_calendar;
            }

            // Navigate to the destination if different from current
            if (navController != null && navController.getCurrentDestination().getId() != destinationId) {
                navController.navigate(destinationId);
                Log.d(TAG, mTAG + "Navigated to destination: " + destinationId);
            }

            // Update current destination tracking
            currentDestination = destinationId;

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error handling view mode change", e);
        }
    }

    // ==================== HANDLE ACTIVITY RESULTS ====================

    /**
     * MODIFY your existing onActivityResult (if present) or add this method
     * Handle returning from settings or welcome activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if returning from settings
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            handleReturnFromSettings();
        }
    }

    /**
     * Handle returning from settings activity
     * Refresh navigation if view mode preferences changed
     */
    private void handleReturnFromSettings() {
        final String methodTag = TAG + ".handleReturnFromSettings";
        Log.d(methodTag, "Returned from settings, checking for preference changes");

        try {
            // Get current preference
            String currentViewMode = QDuePreferences.getDefaultViewMode(this);
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination(this);

            Log.d(methodTag, "Current view mode: " + currentViewMode);
            Log.d(methodTag, "Preferred destination: " + preferredDestination);

            // Check if we need to navigate to different view
            if (navController != null) {
                int currentFragmentId = navController.getCurrentDestination().getId();
                if (currentFragmentId != preferredDestination) {
                    Log.d(methodTag, "Navigating from " + currentFragmentId + " to " + preferredDestination);
                    navController.navigate(preferredDestination);
                }
            }

        } catch (Exception e) {
            Log.e(methodTag, "Error handling return from settings", e);
        }
    }

    /// /////////

    /**
     * Enhanced activity result launcher setup
     */
    private void setupActivityLaunchers() {
        Log.d(TAG, "Initializing enhanced activity result launchers");

        mEventsActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleEventsActivityResult(result)
        );
    }

    /**
     * Enhanced EventsActivity result handler
     */
    private void handleEventsActivityResult(androidx.activity.result.ActivityResult result) {
        final String mTAG = "handleEventsActivityResult: ";
        Log.d(TAG, mTAG + "Result received: " + result.getResultCode());

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            boolean eventsChanged = data.getBooleanExtra(EventsActivity.EXTRA_EVENTS_CHANGED, false);

            if (eventsChanged) {
                int eventCount = data.getIntExtra(EventsActivity.EXTRA_EVENTS_COUNT, 0);
                String changeType = data.getStringExtra(EventsActivity.EXTRA_CHANGE_TYPE);

                Log.d(TAG, String.format(QDue.getLocale(),
                        "%sEvents changed: %s (%d events)", mTAG, changeType, eventCount));

                // Enhanced refresh with both registered and discovered fragments
                refreshEventsDisplayEnhanced(changeType, eventCount);
            } else {
                Log.d(TAG, mTAG + "EventsActivity returned but no events changed");
            }
        } else {
            Log.d(TAG, mTAG + "EventsActivity cancelled or no data returned");
        }
    }

    /**
     * API: Enhanced refresh that combines registered fragments and discovery
     */
    public void refreshEventsDisplayEnhanced(String changeType, int eventCount) {
        final String mTAG = "refreshEventsDisplayEnhanced: ";
        Log.d(TAG, String.format(QDue.getLocale(),
                "%sRefreshing events display: %s (%d events)", mTAG, changeType, eventCount));

        // Get all available fragments using both methods
        List<EventsRefreshInterface> allFragments = getAllEventsRefreshFragments();

        Log.d(TAG, String.format(QDue.getLocale(),
                "%sFound %d total fragments for refresh", mTAG, allFragments.size()));

        // Refresh fragments with detailed logging
        int activeCount = 0;
        int inactiveCount = 0;
        int errorCount = 0;

        for (EventsRefreshInterface fragment : allFragments) {
            try {
                if (fragment.isFragmentActive()) {
                    // Fragment is active - refresh immediately
                    fragment.onEventsChanged(changeType, eventCount);
                    activeCount++;
                    Log.d(TAG, String.format("%sRefreshed active fragment: %s",
                            mTAG, fragment.getFragmentDescription()));
                } else {
                    // Fragment is inactive - will refresh when visible
                    inactiveCount++;
                    Log.d(TAG, String.format("%sSkipped inactive fragment: %s",
                            mTAG, fragment.getFragmentDescription()));
                }
            } catch (Exception e) {
                errorCount++;
                Log.e(TAG, String.format("%sError refreshing fragment %s",
                        mTAG, fragment.getFragmentDescription()), e);
            }
        }

        Log.d(TAG, String.format(QDue.getLocale(),
                "%sRefresh summary: %d active, %d inactive, %d errors",
                mTAG, activeCount, inactiveCount, errorCount));

        // Show user feedback
        if (activeCount > 0) {
            showSuccessMessage(String.format(QDue.getLocale(),
                    "Refreshed %d views", activeCount));
        }

        // Force refresh all if no active fragments and change is significant
        if (activeCount == 0 && inactiveCount > 0 && isSignificantChange(changeType, eventCount)) {
            Log.w(TAG, mTAG + "No active fragments, forcing refresh of all fragments");
            forceRefreshAllEventFragments(allFragments);
        }
    }

    /**
     * Get all fragments using both registered list and discovery
     */
    private List<EventsRefreshInterface> getAllEventsRefreshFragments() {
        Set<EventsRefreshInterface> allFragments = new HashSet<>();

        // Add registered fragments
        synchronized (mRegisteredEventsFragments) {
            allFragments.addAll(mRegisteredEventsFragments);
            Log.d(TAG, String.format(QDue.getLocale() ,"Added %d registered fragments", mRegisteredEventsFragments.size()));
        }

        // Add discovered fragments (fallback method)
        List<EventsRefreshInterface> discoveredFragments = getEventsRefreshFragmentsByDiscovery();
        allFragments.addAll(discoveredFragments);
        Log.d(TAG, String.format(QDue.getLocale(), "Added %d discovered fragments", discoveredFragments.size()));

        return new ArrayList<>(allFragments);
    }

    /**
     * Original discovery method (enhanced for better detection)
     */
    private List<EventsRefreshInterface> getEventsRefreshFragmentsByDiscovery() {
        List<EventsRefreshInterface> eventFragments = new ArrayList<>();

        try {
            // Get all fragments from fragment manager
            List<Fragment> allFragments = getSupportFragmentManager().getFragments();
            Log.d(TAG, String.format("Discovered %d total fragments", allFragments.size()));

            // Filter for fragments that implement EventsRefreshInterface
            for (Fragment fragment : allFragments) {
                if (fragment instanceof EventsRefreshInterface) {
                    eventFragments.add((EventsRefreshInterface) fragment);
                    Log.d(TAG, String.format("Found EventsRefreshInterface fragment: %s",
                            fragment.getClass().getSimpleName()));
                }
            }

            // Also check nested fragments (NavHostFragment children)
            for (Fragment fragment : allFragments) {
                if (fragment.getChildFragmentManager() != null) {
                    List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
                    Log.d(TAG, String.format(QDue.getLocale(),"Checking %d child fragments of %s",
                            childFragments.size(), fragment.getClass().getSimpleName()));

                    for (Fragment childFragment : childFragments) {
                        if (childFragment instanceof EventsRefreshInterface) {
                            eventFragments.add((EventsRefreshInterface) childFragment);
                            Log.d(TAG, String.format("Found nested EventsRefreshInterface fragment: %s",
                                    childFragment.getClass().getSimpleName()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error discovering EventsRefreshInterface fragments", e);
        }

        return eventFragments;
    }

    /**
     * Check if a change is significant enough to force refresh
     */
    private boolean isSignificantChange(String changeType, int eventCount) {
        if (changeType == null) return false;

        // Import/delete operations are always significant
        if (EventsActivity.CHANGE_TYPE_IMPORT.equals(changeType) ||
                EventsActivity.CHANGE_TYPE_DELETE.equals(changeType)) {
            return true;
        }

        // Large numbers of events are significant
        return eventCount > 5;
    }

    /**
     * Enhanced force refresh with better error handling
     */
    private void forceRefreshAllEventFragments(List<EventsRefreshInterface> eventFragments) {
        final String mTAG = "forceRefreshAllEventFragments: ";
        Log.d(TAG, mTAG + "Force refreshing " + eventFragments.size() + " event fragments");

        int successCount = 0;
        int errorCount = 0;

        for (EventsRefreshInterface fragment : eventFragments) {
            try {
                fragment.onForceEventsRefresh();
                successCount++;
                Log.d(TAG, String.format("%sForce refreshed fragment: %s",
                        mTAG, fragment.getFragmentDescription()));
            } catch (Exception e) {
                errorCount++;
                Log.e(TAG, String.format("%sError force refreshing fragment: %s",
                        mTAG, fragment.getFragmentDescription()), e);
            }
        }

        Log.d(TAG, String.format(QDue.getLocale(),
                "%sForce refresh completed: %d success, %d errors",
                mTAG, successCount, errorCount));
    }

    /**
     * Open EventsActivity using the activity launcher
     * This replaces your existing startActivity call
     */
    private void openEventsActivity() {
        Log.d(TAG, "Opening EventsActivity with enhanced result launcher");

        try {
            Intent intent = new Intent(this, EventsActivity.class);
            mEventsActivityLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening EventsActivity", e);
            showSuccessMessage("Error opening Events");
        }
    }

    /**
     * Refresh events display when returning from EventsActivity
     * Uses interface-based approach for clean fragment management
     *
     * @param changeType Type of change that occurred
     * @param eventCount Number of events affected
     */
    private void refreshEventsDisplay(String changeType, int eventCount) {
        Log.d(TAG, String.format(QDue.getLocale(), "Refreshing events display: %s (%d events)", changeType, eventCount));

        // Get all fragments that implement EventsRefreshInterface
        List<EventsRefreshInterface> eventFragments = getEventsRefreshFragments();

        Log.d(TAG, String.format(QDue.getLocale(), "Found %d fragments implementing EventsRefreshInterface", eventFragments.size()));

        // Refresh active fragments immediately
        int activeCount = 0;
        int inactiveCount = 0;

        for (EventsRefreshInterface fragment : eventFragments) {
            try {
                if (fragment.isFragmentActive()) {
                    // Fragment is active - refresh immediately
                    fragment.onEventsChanged(changeType, eventCount);
                    activeCount++;
                    Log.d(TAG, String.format("Refreshed active fragment: %s", fragment.getFragmentDescription()));
                } else {
                    // Fragment is inactive - will refresh when it becomes visible
                    inactiveCount++;
                    Log.d(TAG, String.format("Skipped inactive fragment: %s (will refresh on resume)",
                            fragment.getFragmentDescription()));
                }
            } catch (Exception e) {
                Log.e(TAG, String.format("Error refreshing fragment %s", fragment.getFragmentDescription()), e);
            }
        }

        Log.d(TAG, String.format("Refresh summary: %d active, %d inactive fragments", activeCount, inactiveCount));

        // Show user feedback
        Toast.makeText(this, String.format(QDue.getLocale(), "Refreshed %d fragments", activeCount), Toast.LENGTH_SHORT).show();

        // Optional: Force refresh all if no active fragments found
        if (activeCount == 0 && inactiveCount > 0) {
            Log.w(TAG, "No active fragments found, forcing refresh of all fragments");
            forceRefreshAllEventFragments(eventFragments);
        }
    }

    /**
     * Get all fragments that implement EventsRefreshInterface
     * @return List of fragments that can be refreshed when events change
     */
    private List<EventsRefreshInterface> getEventsRefreshFragments() {
        List<EventsRefreshInterface> eventFragments = new ArrayList<>();

        try {
            // Get all fragments from fragment manager
            List<Fragment> allFragments = getSupportFragmentManager().getFragments();

            // Filter for fragments that implement EventsRefreshInterface
            for (Fragment fragment : allFragments) {
                if (fragment instanceof EventsRefreshInterface) {
                    eventFragments.add((EventsRefreshInterface) fragment);
                    Log.d(TAG, String.format("Found EventsRefreshInterface fragment: %s",
                            fragment.getClass().getSimpleName()));
                }
            }

            // Also check nested fragments (if using nested fragment managers)
            for (Fragment fragment : allFragments) {
                if (fragment.getChildFragmentManager() != null) {
                    List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
                    for (Fragment childFragment : childFragments) {
                        if (childFragment instanceof EventsRefreshInterface) {
                            eventFragments.add((EventsRefreshInterface) childFragment);
                            Log.d(TAG, String.format("Found nested EventsRefreshInterface fragment: %s",
                                    childFragment.getClass().getSimpleName()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting EventsRefreshInterface fragments", e);
        }

        return eventFragments;
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
                openEventsActivity();  // Already set for activity result
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
                getMenuInflater().inflate(R.menu.menu_main, menu);
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


    /**
     * Show success message to user
     */
    private void showSuccessMessage(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing success message", e);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method to check events refresh system
     */
    public void debugEventsRefreshSystem() {
        Log.d(TAG, "=== EVENTS REFRESH SYSTEM DEBUG ===");
        Log.d(TAG, "Registered fragments: " + mRegisteredEventsFragments.size());

        synchronized (mRegisteredEventsFragments) {
            for (EventsRefreshInterface fragment : mRegisteredEventsFragments) {
                Log.d(TAG, "  - " + fragment.getFragmentDescription() +
                        " (Active: " + fragment.isFragmentActive() + ")");
            }
        }

        List<EventsRefreshInterface> discovered = getEventsRefreshFragmentsByDiscovery();
        Log.d(TAG, "Discovered fragments: " + discovered.size());

        for (EventsRefreshInterface fragment : discovered) {
            Log.d(TAG, "  - " + fragment.getFragmentDescription() +
                    " (Active: " + fragment.isFragmentActive() + ")");
        }

        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * Debug method to log current navigation and preference state
     * Call this in onCreate or onResume for debugging
     */
    private void logNavigationAndPreferenceState() {
        if (!QDue.Debug.DEBUG_ACTIVITY) return;

        Log.d(TAG, "=== Navigation & Preference State Debug ===");

        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestId = navController.getCurrentDestination().getId();
            Log.d(TAG, "Current navigation destination: " + currentDestId);
            Log.d(TAG, "Target destination: " + currentDestination);
        } else {
            Log.d(TAG, "NavController or current destination is null");
        }

        // Log all preferences
        QDuePreferences.logAllPreferences(this);

        Log.d(TAG, "=== End Navigation & Preference Debug ===");
    }

    private void debugCurrentState() {
        if (!QDue.Debug.DEBUG_ACTIVITY) return;

        Log.d(TAG, "=== DEBUG: Current App State ===");
        Log.d(TAG, "Welcome completed: " + QDuePreferences.isWelcomeCompleted(this));
        Log.d(TAG, "Current view mode: " + QDuePreferences.getDefaultViewMode(this));
        Log.d(TAG, "Should show welcome: " + QDuePreferences.shouldShowWelcome(this));

        if (navController != null && navController.getCurrentDestination() != null) {
            Log.d(TAG, "Current navigation: " + navController.getCurrentDestination().getId());
        }

        Log.d(TAG, "=== END DEBUG ===");
    }
}