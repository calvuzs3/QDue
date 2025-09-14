package net.calvuz.qdue;

import static android.view.View.VISIBLE;

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
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.features.events.presentation.EventsActivity;
import net.calvuz.qdue.ui.features.events.interfaces.EventsRefreshInterface;
import net.calvuz.qdue.ui.features.settings.SettingsLauncher;
import net.calvuz.qdue.ui.proto.CalendarDataManagerEnhanced;
import net.calvuz.qdue.ui.proto.MigrationHelper;
import net.calvuz.qdue.ui.features.settings.presentation.QDueSettingsActivity;
import net.calvuz.qdue.ui.core.common.enums.NavigationMode;
import net.calvuz.qdue.ui.features.welcome.presentation.WelcomeActivity;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.user.ui.UserProfileLauncher;

import java.text.MessageFormat;
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

    // Enhanced activity launcher
    private ActivityResultLauncher<Intent> mEventsActivityLauncher;

    // ======================= METHODS ============================

    private CalendarDataManagerEnhanced enhancedDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v( TAG, "onCreate: called." );

        // Check if user needs to see welcome before setting up main activity
        if (shouldRedirectToWelcome()) {
            redirectToWelcome();
            return;
        }

        // Setup view mode preferences before calling super
        setupDefaultViewModePreferences();

        // Call existing onCreate
        super.onCreate( savedInstanceState );

        // Initialize enhanced data manager if virtual scrolling is enabled
        if (MigrationHelper.shouldUseVirtualScrolling()) {
            enhancedDataManager = CalendarDataManagerEnhanced.getEnhancedInstance();
            Log.d( TAG, "=== Virtual scrolling enabled for this session" );
        } else {
            Log.d( TAG, "=== Legacy scrolling enabled for this session" );
        }

        // Log device capabilities for monitoring
        logDeviceCapabilities();

        // Use binding
        binding = ActivityQdueMainBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );

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

    // ==================== PREFERENCES METHODS ====================

    /**
     * Check if user should be redirected to WelcomeActivity
     *
     * @return true if welcome should be shown
     */
    private boolean shouldRedirectToWelcome() {
        boolean shouldShow = QDuePreferences.shouldShowWelcome( this );
        Log.d( TAG, "Should redirect to welcome: " + shouldShow );
        return shouldShow;
    }

    /**
     * Redirect user to WelcomeActivity for initial setup
     */
    private void redirectToWelcome() {
        Log.d( TAG, "Redirecting to WelcomeActivity for first-time setup" );

        Intent welcomeIntent = new Intent( this, WelcomeActivity.class );
        startActivity( welcomeIntent );
        finish(); // Close main activity
    }

    /**
     * Setup default view mode preferences and ensure navigation graph compatibility
     * This method prepares the navigation system based on user preferences
     */
    private void setupDefaultViewModePreferences() {
        try {
            // Get the preferred start destination
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination( this );
            String viewMode = QDuePreferences.getDefaultViewMode( this );

            Log.d( TAG, "User preferred view mode: " + viewMode );
            Log.d( TAG, "Target navigation destination: " + preferredDestination );

            // Store for later use in navigation setup
            currentDestination = preferredDestination;
        } catch (Exception e) {
            Log.e( TAG, "Error setting up view mode preferences", e );
            // Fallback to calendar view
            currentDestination = R.id.nav_calendar;
            QDuePreferences.setDefaultViewMode( this, QDue.Settings.VIEW_MODE_CALENDAR );
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
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination( this );
            int currentDestination = navController.getCurrentDestination().getId();

            // Only auto-navigate if we're on a main view (not detail/settings/etc)
            if (isMainNavigationDestination( currentDestination ) &&
                    currentDestination != preferredDestination) {

                Log.d( TAG, "Navigation state mismatch - auto-correcting to preferred view" );
                navController.navigate( preferredDestination );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error verifying navigation state", e );
        }
    }

    /**
     * Check if the given destination ID is a main navigation destination
     *
     * @param destinationId Navigation destination ID to check
     * @return true if it's a main destination (calendar or dayslist)
     */
    private boolean isMainNavigationDestination(int destinationId) {
        return destinationId == R.id.nav_calendar || destinationId == R.id.nav_dayslist || destinationId == R.id.nav_swipe_calendar;
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

            Log.d( TAG, "Device info - Max Memory: " + maxMemory + "MB, API Level: " + apiLevel );

            // Send to analytics if you have it
            // Analytics.setProperty("max_memory_mb", String.valueOf(maxMemory));
            // Analytics.setProperty("api_level", String.valueOf(apiLevel));

        } catch (Exception e) {
            Log.w( TAG, "Error logging device capabilities: " + e.getMessage() );
        }
    }

    /**
     * Detect which navigation components are available in current layout.
     * This allows the same activity to work with different layout configurations.
     */
    @Override
    protected void detectNavigationComponents() {
        // Always present in all layouts
        drawerLayout = binding.drawerLayout;

        // Detect available navigation components
        navigationRail = findViewById( R.id.navigation_rail );
        drawerNavigation = findViewById( R.id.nav_drawer_secondary );
        sidebarNavigation = findViewById( R.id.sidebar_navigation );
        fabGoToToday = findViewById( R.id.fab_go_to_today );
        // Toolbar
        MaterialToolbar toolbar = findViewById( R.id.toolbar );

        // NEW: Extended drawer for phone portrait
        NavigationView extendedDrawer = findViewById( R.id.nav_drawer_extended );
        if (extendedDrawer != null) {
            drawerNavigation = extendedDrawer; // Use extended drawer as primary drawer
        }

        // Determine current navigation mode based on available components
        currentNavigationMode = determineNavigationMode();

        // LOGGING
        Log.v( TAG, "Navigation mode: " + currentNavigationMode );
        Log.v( TAG, "Available components:" );
        Log.v( TAG, "  - Toolbar: " + (toolbar != null) );
//        Log.v(TAG, "  - BottomNavigation: " + (bottomNavigation != null));
        Log.v( TAG, "  - NavigationRail: " + (navigationRail != null) );
        Log.v( TAG, "  - DrawerNavigation: " + (drawerNavigation != null) );
        Log.v( TAG, "  - ExtendedDrawer: " + (extendedDrawer != null) );
        Log.v( TAG, "  - SidebarNavigation: " + (sidebarNavigation != null) );
        Log.v( TAG, "  - FAB: " + (fabGoToToday != null) );
    }

    /**
     * Determine navigation mode based on available components and screen configuration.
     */
    private NavigationMode determineNavigationMode() {
        Configuration config = getResources().getConfiguration();
        int screenWidthDp = config.screenWidthDp;
        int smallestScreenWidthDp = config.smallestScreenWidthDp;
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (navigationRail != null && drawerLayout != null && !isLandscape) {
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
        } catch (Exception e) {
            Log.e( TAG, "setupNavigationSafely: Error setting up navigation: " + e.getMessage() );
        }
    }

    /**
     * NUOVO: Setup AppBar with safe binding access
     */
    private void setupAppBarSafe() {
        try {
            AppBarLayout appBarLayout = getAppBarLayout();
            MaterialToolbar toolbar = getToolbar();

            if (toolbar != null) {
                // Set toolbar as ActionBar
                setSupportActionBar( toolbar );

                // Configure ActionBar
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled( true );
                    getSupportActionBar().setTitle( getString( R.string.app_name ) );

                    // Setup menu button in order to open sidebar
//                    getSupportActionBar().setDisplayHomeAsUpEnabled(shouldShowMenuButton());
                    getSupportActionBar().setHomeAsUpIndicator( R.drawable.ic_menu );

                    Log.d( TAG, "getSupportActionBar != null" );
                }

                // Configure AppBar visibility
                if (appBarLayout != null) {
                    boolean shouldShow = shouldShowAppBar();
                    appBarLayout.setVisibility( shouldShow ? VISIBLE : View.GONE );

                    Log.d( TAG, "AppBar visibility: " + (shouldShow ? "VISIBLE" : "GONE") );
                }

                Log.d( TAG, "Toolbar configured successfully" );
            } else {
                Log.w( TAG, "Toolbar not found in current layout" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error setting up AppBar: " + e.getMessage() );
        }
    }

    /**
     * Setup NavController with fallback error handling.
     */
    @Override
    protected void setupNavController() {
        try {
            // Find NavHostFragment directly
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById( R.id.nav_host_fragment_content_main );

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d( TAG, "NavController found via NavHostFragment" );
            } else {
                // Method 2: Fallback with Navigation.findNavController
                navController = Navigation.findNavController( this, R.id.nav_host_fragment_content_main );
                Log.d( TAG, "NavController found via Navigation.findNavController" );
            }

            // Configure start destination based on user preference
            configureNavigationStartDestination();
        } catch (IllegalStateException e) {
            Log.e( TAG, "Error setting up NavController: " + e.getMessage() );

            // Retry after small delay
            findViewById( R.id.nav_host_fragment_content_main ).post( () -> {
                try {
                    navController = Navigation.findNavController( this, R.id.nav_host_fragment_content_main );
                    configureNavigationStartDestination();
                    Log.d( TAG, "NavController found on second attempt" );
                } catch (Exception retryException) {
                    Log.e( TAG, "Failed to find NavController on retry: " + retryException.getMessage() );
                }
            } );
        }
    }

    /**
     * Configure the navigation graph start destination based on user preferences
     * This modifies the navigation graph dynamically
     */
    private void configureNavigationStartDestination() {
        if (navController == null) {
            Log.w( TAG, "NavController is NULL, cannot configure start destination" );
            return;
        }

        try {
            // Get user's preferred view mode
            String preferredViewMode = QDuePreferences.getDefaultViewMode( this );

            // Get the current navigation graph
            androidx.navigation.NavGraph navGraph = navController.getNavInflater()
                    .inflate( R.navigation.mobile_navigation );

            // Determine start destination based on preference
            int startDestinationId;
            if (QDue.Settings.VIEW_MODE_DAYSLIST.equals( preferredViewMode )) {
                startDestinationId = R.id.nav_dayslist;
                Log.d( TAG, "Setting start destination to DaysList" );
            } else if (QDue.Settings.VIEW_MODE_CALENDAR.equals( preferredViewMode )) {
                startDestinationId = R.id.nav_calendar;
                Log.d( TAG, "Setting start destination to Calendar" );
            } else {
                startDestinationId = R.id.nav_swipe_calendar;
                Log.d( TAG, "Setting start destination to SwipeCalendar" );
            }

            // Apply the start destination to the graph
            navGraph.setStartDestination( startDestinationId );
            navController.setGraph( navGraph );

            // Update current destination tracking
            currentDestination = startDestinationId;

            Log.d( TAG, "Navigation graph configured with current destination: " + currentDestination );
        } catch (Exception e) {
            // Let the navigation graph use its default start destination
            Log.e( TAG, "Error configuring navigation start destination", e );
        }
    }

    // ==================== HANDLE VIEW MODE CHANGES FROM SETTINGS ====================

    /**
     * Handle navigation when user changes view mode from settings
     * Call this method when user changes view mode in settings
     *
     * @param newViewMode The new view mode selected by user
     */
    public void onViewModeChanged(String newViewMode) {
        Log.d( TAG, "View mode changed to: " + newViewMode );

        try {
            // Navigate to the selected view
            int destinationId;
            if (QDue.Settings.VIEW_MODE_DAYSLIST.equals( newViewMode )) {
                destinationId = R.id.nav_dayslist;
            } else if (QDue.Settings.VIEW_MODE_CALENDAR.equals( newViewMode )) {
                destinationId = R.id.nav_calendar;
            } else {
                destinationId = R.id.nav_swipe_calendar;
            }

            // Navigate to the destination if different from current
            if (navController != null && navController.getCurrentDestination().getId() != destinationId) {
                Log.d( TAG, "Navigate to destination: " + destinationId );
                navController.navigate( destinationId );
            }

            // Update current destination tracking
            currentDestination = destinationId;
        } catch (Exception e) {
            Log.e( TAG, "Error handling view mode change", e );
        }
    }

    // ==================== HANDLE ACTIVITY RESULTS ====================

    /**
     * MODIFY your existing onActivityResult (if present) or add this method
     * Handle returning from settings or welcome activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult( requestCode, resultCode, data );

        // Check if returning from settings
        if (requestCode == QDue.SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            handleReturnFromSettings();
        }
    }

    /**
     * Handle returning from settings activity
     * Refresh navigation if view mode preferences changed
     */
    private void handleReturnFromSettings() {
        try {
            // Get current preference
            String currentViewMode = QDuePreferences.getDefaultViewMode( this );
            int preferredDestination = QDuePreferences.getDefaultNavigationDestination( this );

            Log.d( TAG, "handleReturnFromSettings: Current view mode: " + currentViewMode );
            Log.d( TAG, "handleReturnFromSettings: Preferred destination: " + preferredDestination );

            // Check if we need to navigate to different view
            if (navController != null) {
                int currentFragmentId = navController.getCurrentDestination().getId();
                if (currentFragmentId != preferredDestination) {
                    Log.d( TAG, "handleReturnFromSettings: Navigating from " + currentFragmentId + " to " + preferredDestination );
                    navController.navigate( preferredDestination );
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "handleReturnFromSettings: Error handling return from settings", e );
        }
    }

    /// /////////

    /**
     * Enhanced activity result launcher setup
     */
    private void setupActivityLaunchers() {
        Log.d( TAG, "Initializing enhanced activity result launchers" );

        mEventsActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleEventsActivityResult
        );
    }

    /**
     * Enhanced EventsActivity result handler
     */
    private void handleEventsActivityResult(androidx.activity.result.ActivityResult result) {
        Log.d( TAG, "Result received: " + result.getResultCode() );

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            boolean eventsChanged = data.getBooleanExtra( EventsActivity.EXTRA_EVENTS_CHANGED, false );

            if (eventsChanged) {
                int eventCount = data.getIntExtra( EventsActivity.EXTRA_EVENTS_COUNT, 0 );
                String changeType = data.getStringExtra( EventsActivity.EXTRA_CHANGE_TYPE );

                Log.d( TAG, MessageFormat.format(
                        "Events changed: {0} ({1} events)", changeType, eventCount ) );

                // Enhanced refresh with both registered and discovered fragments
                refreshEventsDisplayEnhanced( changeType, eventCount );
            } else {
                Log.d( TAG, "EventsActivity returned but no events changed" );
            }
        } else {
            Log.d( TAG, "EventsActivity cancelled or no data returned" );
        }
    }

    /**
     * API: Enhanced refresh that combines registered fragments and discovery
     */
    public void refreshEventsDisplayEnhanced(String changeType, int eventCount) {
        Log.d( TAG, MessageFormat.format( "Refreshing events display: {0} ({1} events)",
                changeType, eventCount ) );

        // Get all available fragments using both methods
        List<EventsRefreshInterface> allFragments = getAllEventsRefreshFragments();

        Log.d( TAG, MessageFormat.format(
                "Found {0} total fragments for refresh", allFragments.size() ) );

        // Refresh fragments with detailed logging
        int activeCount = 0;
        int inactiveCount = 0;
        int errorCount = 0;

        for (EventsRefreshInterface fragment : allFragments) {
            try {
                if (fragment.isFragmentActive()) {
                    // Fragment is active - refresh immediately
                    fragment.onEventsChanged( changeType, eventCount );
                    activeCount++;
                    Log.d( TAG, MessageFormat.format( "Refreshed active fragment: {0}",
                            fragment.getFragmentDescription() ) );
                } else {
                    // Fragment is inactive - will refresh when visible
                    inactiveCount++;
                    Log.d( TAG, MessageFormat.format( "Skipped inactive fragment: {0}",
                            fragment.getFragmentDescription() ) );
                }
            } catch (Exception e) {
                errorCount++;
                Log.e( TAG, MessageFormat.format( "Error refreshing fragment {0}",
                        fragment.getFragmentDescription() ), e );
            }
        }

        Log.d( TAG, MessageFormat.format( "Refresh summary: {0} active, {1} inactive, {2} errors",
                activeCount, inactiveCount, errorCount ) );

        // Force refresh all if no active fragments and change is significant
        if (activeCount == 0 && inactiveCount > 0 && isSignificantChange( changeType, eventCount )) {
            Log.w( TAG, "No active fragments, forcing refresh of all fragments" );
            forceRefreshAllEventFragments( allFragments );
        }
    }

    /**
     * Get all fragments using both registered list and discovery
     */
    private List<EventsRefreshInterface> getAllEventsRefreshFragments() {
        Set<EventsRefreshInterface> allFragments;

        // Add registered fragments
        synchronized (mRegisteredEventsFragments) {
            allFragments = new HashSet<>( mRegisteredEventsFragments );
            Log.d( TAG, MessageFormat.format( "Added {0} registered fragments", mRegisteredEventsFragments.size() ) );
        }

        // Add discovered fragments (fallback method)
        List<EventsRefreshInterface> discoveredFragments = getEventsRefreshFragmentsByDiscovery();
        allFragments.addAll( discoveredFragments );
        Log.d( TAG, MessageFormat.format( "Added {0} discovered fragments", discoveredFragments.size() ) );

        return new ArrayList<>( allFragments );
    }

    /**
     * Original discovery method (enhanced for better detection)
     */
    private List<EventsRefreshInterface> getEventsRefreshFragmentsByDiscovery() {
        List<EventsRefreshInterface> eventFragments = new ArrayList<>();

        try {
            // Get all fragments from fragment manager
            List<Fragment> allFragments = getSupportFragmentManager().getFragments();
            Log.d( TAG, MessageFormat.format( "Discovered {0} total fragments", allFragments.size() ) );

            // Filter for fragments that implement EventsRefreshInterface
            for (Fragment fragment : allFragments) {
                if (fragment instanceof EventsRefreshInterface) {
                    eventFragments.add( (EventsRefreshInterface) fragment );
                    Log.d( TAG, MessageFormat.format( "Found EventsRefreshInterface fragment: {0}",
                            fragment.getClass().getSimpleName() ) );
                }
            }

            // Also check nested fragments (NavHostFragment children)
            for (Fragment fragment : allFragments) {
                if (fragment.getChildFragmentManager() != null) {
                    List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
                    Log.d( TAG, MessageFormat.format( "Checking {0} child fragments of {1}",
                            childFragments.size(), fragment.getClass().getSimpleName() ) );

                    for (Fragment childFragment : childFragments) {
                        if (childFragment instanceof EventsRefreshInterface) {
                            eventFragments.add( (EventsRefreshInterface) childFragment );
                            Log.d( TAG, MessageFormat.format( "Found nested EventsRefreshInterface fragment: {0}",
                                    childFragment.getClass().getSimpleName() ) );
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "Error discovering EventsRefreshInterface fragments", e );
        }

        return eventFragments;
    }

    /**
     * Check if a change is significant enough to force refresh
     */
    private boolean isSignificantChange(String changeType, int eventCount) {
        if (changeType == null) return false;

        // Import/delete operations are always significant
        if (EventsActivity.CHANGE_TYPE_IMPORT.equals( changeType ) ||
                EventsActivity.CHANGE_TYPE_DELETE.equals( changeType )) {
            return true;
        }

        // Large numbers of events are significant
        return eventCount > 5;
    }

    /**
     * Enhanced force refresh with better error handling
     */
    private void forceRefreshAllEventFragments(List<EventsRefreshInterface> eventFragments) {
        int successCount = 0;
        int errorCount = 0;

        for (EventsRefreshInterface fragment : eventFragments) {
            try {
                fragment.onForceEventsRefresh();
                successCount++;
            } catch (Exception e) {
                errorCount++;
            }
        }

        Log.d( TAG, MessageFormat.format(
                "Force refresh completed: {0} success, {1} errors",
                successCount, errorCount ) );
    }

    /**
     * Open EventsActivity using the activity launcher
     * This replaces your existing startActivity call
     */
    private void openEventsActivity() {
        Log.d( TAG, "Opening EventsActivity with enhanced result launcher" );

        try {
            Intent intent = new Intent( this, EventsActivity.class );
            mEventsActivityLauncher.launch( intent );
        } catch (Exception e) {
            Log.e( TAG, "Error opening EventsActivity", e );
            Library.showError( this, getString( R.string.settings_events_not_available ) );
        }
    }

    private void openUserProfileActivity() {
        Log.d( TAG, "Opening UserProfileActivity" );

        try {
            if (UserProfileLauncher.isAvailable()) {
                UserProfileLauncher.launch( this );
            } else {
                Library.showError( this, getString( R.string.user_profile_not_available ) );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error opening UserProfileActivity", e );
            Library.showError( this, getString( R.string.user_profile_not_available ) );
        }
    }

    private void openSettingsActivity() {
        Log.d( TAG, "Opening SettingsActivity" );

        try {
            if (SettingsLauncher.isAvailable()) {
                SettingsLauncher.launch( this );
            } else {
                Library.showError( this, getString( R.string.settings_not_available ) );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error opening SettingsActivity", e );
            Library.showError( this, getString( R.string.settings_not_available ) );
        }
    }

    /**
     * Unified navigation item selection handler for all navigation modes.
     */
    @Override
    protected boolean handleNavigationItemSelected(int itemId) {
        if (navController == null) {
            Log.e( TAG, "NavController is NULL" );
            return false;
        }

        try {
            currentDestination = itemId;

            if (itemId == R.id.nav_calendar) {
                navController.navigate( R.id.nav_calendar );
                return true;
            } else if (itemId == R.id.nav_dayslist) {
                navController.navigate( R.id.nav_dayslist );
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                openUserProfileActivity();
                return true;
            } else if (itemId == R.id.nav_events) {
                openEventsActivity();
                return true;
            } else if (itemId == R.id.nav_settings) {
                openSettingsActivity();
                return true;
            } else if (itemId == R.id.nav_about) {
                navController.navigate( R.id.nav_about );
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e( TAG, "Error during navigation", e );
            return false;
        }
    }

    /**
     * Enhanced drawer item selection handler for extended menu (phone portrait).
     */
    @Override
    protected boolean handleExtendedDrawerItemSelected(int itemId) {
        String message = "Handling extended drawer item ({0})";
        try {
            if (itemId == R.id.nav_user_profile) {
                Log.d( TAG, MessageFormat.format( message, R.string.nav_user_profile ) );
                openUserProfileActivity();
                return true;
            } else if (itemId == R.id.nav_events) {
                Log.d( TAG, MessageFormat.format( message, R.string.nav_events ) );
                openEventsActivity();
                return true;
            } else if (itemId == R.id.nav_settings) {
                Log.d( TAG, MessageFormat.format( message, R.string.nav_settings ) );
                Intent settingsIntent = new Intent( this, QDueSettingsActivity.class );
                startActivity( settingsIntent );
                return true;
            } else if (itemId == R.id.nav_about) {
                Log.d( TAG, MessageFormat.format( message, R.string.nav_header_about ) );
                if (navController != null) {
                    navController.navigate( R.id.nav_about );
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e( TAG, "Error handling extended drawer item", e );
            return false;
        }
    }

    // ========== COMMUNICATION INTERFACE IMPLEMENTATION ==========

    @Override
    public void onFragmentNavigationRequested(int destinationId, Bundle data) {
        try {
            if (navController != null) {
                if (data != null) {
                    navController.navigate( destinationId, data );
                } else {
                    navController.navigate( destinationId );
                }
                Log.d( TAG, "Navigation to: " + destinationId );
            } else {
                throw new UnsupportedOperationException( "NavController is NULL" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error during navigation", e );
        }
    }

    @Override
    public void onFragmentTitleChanged(String title) {
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle( title );
                Log.v( TAG, "Updated toolbar title to: " + title );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error updating toolbar title", e );
        }
    }

    @Override
    public void onFragmentStatusMessage(String message, boolean isError) {
        try {
            if (isError) {
                Library.showToast( this, message, Toast.LENGTH_LONG );
            } else {
                Snackbar.make( binding.getRoot(), message, Snackbar.LENGTH_SHORT ).show();
            }
            Log.d( TAG, "Displayed status message: " + message );
        } catch (Exception e) {
            Log.e( TAG, "Error showing status message", e );
        }
    }

    @Override
    public void onFragmentMenuChanged(int menuId) {
        // Invalidate options menu to trigger onCreateOptionsMenu with new menu
        try {
            invalidateOptionsMenu();
            Log.d( TAG, "Invalidated options menu for menuId: " + menuId );
        } catch (Exception e) {
            Log.e( TAG, "Error changing fragment menu", e );
        }
    }

    @Override
    public void onFragmentToolbarVisibilityChanged(boolean visible) {
        try {
            com.google.android.material.appbar.AppBarLayout appBarLayout = getAppBarLayout();

            if (appBarLayout != null && shouldShowAppBar()) {
                appBarLayout.setVisibility( visible ? VISIBLE : View.GONE );
                Log.d( TAG, "Set toolbar visibility: " + visible );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error changing toolbar visibility", e );
        }
    }

    @Override
    public void onFragmentOperationComplete(String operationType, boolean success, Bundle
            resultData) {
        try {
            Log.d( TAG, "Operation completed: " + operationType + ", success: " + success );

            switch (operationType) {
                case "data_refresh":
                    if (success) {
                        notifyUpdates();
                    }
                    break;

                case "scroll_to_today":
                    if (success) {
                        onFragmentStatusMessage( "Navigated to today", false );
                    }
                    break;

                default:
                    // Handle other operation types as needed
                    break;
            }
        } catch (Exception e) {
            Log.e( TAG, "Error handling operation completion", e );
        }
    }

    @Override
    public void onFragmentCustomAction(String action, Bundle data) {
        try {
            switch (action) {
                case "update_toolbar_title":
                    if (data != null && data.containsKey( "title" )) {
                        String title = data.getString( "title" );
                        onFragmentTitleChanged( title );
                    }
                    break;

                case "show_loading":
                    // Handle loading indicator requests
                    boolean show = data != null && data.getBoolean( "show", false );
                    showLoadingIndicator( show );
                    break;

                case "update_fab_visibility":
                    // Handle FAB visibility updates if needed
                    boolean visible = data != null && data.getBoolean( "visible", true );
                    handleFabVisibility( visible );
                    break;

                default:
                    Log.d( TAG, "Unhandled custom action: " + action );
                    break;
            }
        } catch (Exception e) {
            Log.e( TAG, "Error handling custom action '" + action + "': " + e.getMessage() );
        }
    }

    /**
     * Show/hide loading indicator for long operations.
     */
    private void showLoadingIndicator(boolean show) {
        Log.v( TAG, "Showing Loading Indicator" );

        try {
            // Example implementation - adjust based on your actual UI
            View loadingOverlay = findViewById( R.id.loading_overlay );
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility( show ? VISIBLE : View.GONE );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error controlling loading indicator: " + e.getMessage() );
        }
    }

    /**
     * Handle FAB visibility if activity needs to coordinate with fragment FABs.
     */
    private void handleFabVisibility(boolean visible) {
        // Implementation depends on whether activity has its own FABs to coordinate
        // For now, this is mainly handled by individual fragments

        fabGoToToday.setVisibility( visible ? View.VISIBLE : View.GONE );
        Log.d( TAG, MessageFormat.format( "Handled FAB visibility ({0})", visible ) );
    }

    // ==================== 1. FIX BINDING APPBAR ====================

    /**
     * Safe access to AppBar components that may not exist in all layouts
     */
    private com.google.android.material.appbar.AppBarLayout getAppBarLayout() {
        // Try direct binding first
        if (binding != null) {
            try {
                // AppBar could be in binding if moved back to fragments
                return binding.getRoot()
                        .findViewById( R.id.appbar );
            } catch (Exception e) {
                Log.e( TAG, "AppBar not in binding, try activity findViewById" );
            }
        }

        // Fallback to activity findViewById (for AppBar in activity layout)
        return findViewById( R.id.appbar );
    }

    /**
     * Safe access to Toolbar
     */
    private com.google.android.material.appbar.MaterialToolbar getToolbar() {
        // Try direct binding first
        if (binding != null) {
            try {
                return binding.getRoot()
                        .findViewById( R.id.toolbar );
            } catch (Exception e) {
                Log.e( TAG, "Toolbar not in binding, try activity findViewById" );
            }
        }

        // Fallback to activity findViewById
        return findViewById( R.id.toolbar );
    }

    /**
     * Check if AppBar should be visible based on orientation and layout
     */
    private boolean shouldShowAppBar() {
        Configuration config = getResources().getConfiguration();
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Show AppBar only in portrait mode
        Log.d( TAG, "Should show AppBar: " + !isLandscape );
        return !isLandscape;
    }

    // ==================== 3. AGGIUNGERE MENU HANDLING ====================

    /**
     * AGGIUNGERE: Menu creation and handling
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            boolean shouldShowMenu = shouldShowAppBar() && getToolbar() != null;
            boolean hasExtendedDrawer = drawerLayout != null && drawerNavigation != null;

            if (shouldShowMenu) {
                // Show toolbar menu only if we don't have extended drawer
                getMenuInflater().inflate( R.menu.menu_main, menu );
                Log.d( TAG, "Menu created for mode without drawer" );
                return true;
            }

            // No menu when drawer is available (drawer replaces toolbar menu)
            Log.d( TAG, "Menu not created - drawer handles extended options" );
            return super.onCreateOptionsMenu( menu );
        } catch (Exception e) {
            Log.e( TAG, "Error creating menu", e );
            return super.onCreateOptionsMenu( menu );
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final String message = "Options Item Selected ({0}) ";

        try {
            int id = item.getItemId();

            // Handle home/up button (menu button)
            if (id == android.R.id.home) {
                Log.d( TAG, MessageFormat.format( message, "Home" ) );
                toggleSidebar();
                return true;
            }

            // Handle other menu items
            if (id == R.id.action_settings) {
                Log.d( TAG, MessageFormat.format( message, "Settings" ) );
                openSettingsActivity();
                return true;
            }

            if (id == R.id.action_about) {
                Log.d( TAG, MessageFormat.format( message, "About" ) );
                if (navController != null) {
                    navController.navigate( R.id.nav_about );
                }
                return true;
            }


            if (id == R.id.action_user_profile) {
                openUserProfileActivity();
            }
        } catch (Exception e) {
            Log.e( TAG, "Error handling menu selection", e );
        }

        return super.onOptionsItemSelected( item );
    }

    /**
     * Drawer toggle
     */
    private void toggleSidebar() {
        try {
            if (drawerLayout != null && drawerNavigation != null) {
                // CORREZIONE: Apri/chiudi effettivamente il drawer
                if (drawerLayout.isDrawerOpen( drawerNavigation )) {
                    drawerLayout.closeDrawer( drawerNavigation );
                } else {
                    drawerLayout.openDrawer( drawerNavigation );
                }
                return;
            }

            // Without Drawer
            throw new UnsupportedOperationException( "Error toggling sidebar" );
        } catch (Exception e) {
            Log.e( TAG, "Error toggling sidebar", e );
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