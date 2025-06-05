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

    /**
     * MINIMAL TEST: Replace onCreate() temporarily with this
     */
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // MINIMAL TEST: Skip all complex setup
//        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        // MINIMAL SETUP: Just toolbar
//        if (binding.toolbar != null) {
//            setSupportActionBar(binding.toolbar);
//            Log.d(TAG, "MINIMAL TEST: Toolbar set as ActionBar");
//
//            // Force visibility
//            binding.toolbar.setVisibility(View.VISIBLE);
//            Log.d(TAG, "MINIMAL TEST: Toolbar visibility set to VISIBLE");
//
//            // Check if ActionBar exists
//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setDisplayShowTitleEnabled(true);
//                getSupportActionBar().setTitle("MINIMAL TEST");
//                Log.d(TAG, "MINIMAL TEST: ActionBar configured");
//            } else {
//                Log.e(TAG, "MINIMAL TEST: getSupportActionBar() returned NULL!");
//            }
//        } else {
//            Log.e(TAG, "MINIMAL TEST: binding.toolbar is NULL!");
//        }
//    }
//    /**
//     * CRITICAL TEST: Replace entire onCreate() with this to test if binding is the problem
//     */
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        Log.d(TAG, "=== BYPASS BINDING TEST ===");
//
//        // TEST 1: Create toolbar programmatically (no XML, no binding)
//        LinearLayout rootLayout = new LinearLayout(this);
//        rootLayout.setOrientation(LinearLayout.VERTICAL);
//        rootLayout.setBackgroundColor(Color.RED);
//
//        // Create toolbar in code
//        androidx.appcompat.widget.Toolbar toolbar = new androidx.appcompat.widget.Toolbar(this);
//        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                (int)(56 * getResources().getDisplayMetrics().density)
//        ));
//        toolbar.setBackgroundColor(Color.GREEN);
//        toolbar.setTitle("PROGRAMMATIC TOOLBAR");
//        toolbar.setTitleTextColor(Color.BLACK);
//        toolbar.setElevation(8f);
//
//        // Create test content
//        TextView testContent = new TextView(this);
//        testContent.setLayoutParams(new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT
//        ));
//        testContent.setText("PROGRAMMATIC CONTENT");
//        testContent.setBackgroundColor(Color.BLUE);
//        testContent.setTextColor(Color.WHITE);
//        testContent.setTextSize(24f);
//        testContent.setGravity(android.view.Gravity.CENTER);
//
//        // Add to layout
//        rootLayout.addView(toolbar);
//        rootLayout.addView(testContent);
//
//        // Set as content view (NO BINDING)
//        setContentView(rootLayout);
//
//        // Try to set as ActionBar
//        setSupportActionBar(toolbar);
//
//        Log.d(TAG, "Programmatic toolbar created");
//        Log.d(TAG, "Toolbar height: " + toolbar.getHeight());
//        Log.d(TAG, "ActionBar: " + (getSupportActionBar() != null ? "EXISTS" : "NULL"));
//
//        // Post-layout check
//        toolbar.post(() -> {
//            Log.d(TAG, "POST-LAYOUT Toolbar height: " + toolbar.getHeight());
//            Log.d(TAG, "POST-LAYOUT Toolbar visibility: " + toolbar.getVisibility());
//            int[] location = new int[2];
//            toolbar.getLocationOnScreen(location);
//            Log.d(TAG, "POST-LAYOUT Toolbar position: [" + location[0] + ", " + location[1] + "]");
//        });
//    }

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

//        setupToolbar();
        setupNavigationSafely();

        // Call this in onCreate() after setContentView(binding.getRoot());

//        debugToolbarVisibility();    // 1. Debug
//        debugToolbarOverlap();
//        setupToolbar();             // 2. Setup (ora con fix)
//        forceToolbarLayout();       // 3. Force layout
//        setupNavigationSafely();    // 4. Navigation
//
//        cleanupDebugStyling();


        // Configure toolbar (always exists now, may be hidden)
//        setupToolbar();

        // Setup navigation with safe error handling
//        setupNavigationSafely();

        // REMOVED: setupFAB() - FAB is handled by fragments

        // Track current fragment
//        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
//            mCurrentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
//            if (DEBUG_ACTIVITY)
//                Log.d(TAG, "Fragment changed: " + (mCurrentFragment != null ? mCurrentFragment.getClass().getSimpleName() : "null"));
//        });
    }

    /**
     * Setup navigation safely with error handling.
     */
    private void setupNavigationSafely() {
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

//    /**
//     * Update toolbar title safely.
//     */
//    private void updateToolbarTitle() {
//        // Only update title if ActionBar exists and is visible
//        if (getSupportActionBar() != null && toolbar.getVisibility() == View.VISIBLE) {
////            getSupportActionBar().setTitle(R.string.app_name);
//        }
//    }

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
// CRITICAL FIXES for QDueMainActivity.java menu navigation

//    /**
//     * FIXED: Setup toolbar with proper orientation handling.
//     * Now toolbar exists in binding and is properly configured.
//     */
//    private void setupToolbar() {
//        final String METHOD_TAG = TAG + " setupToolbar";
//
//        try {
//            // FIXED: Toolbar now exists in binding
//            if (binding.toolbar != null) {
//                setSupportActionBar(binding.toolbar);
//
//                // Check orientation and adjust visibility
//                boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
//
//                if (isLandscape) {
//                    // Hide entire AppBar in landscape
//                    if (binding.appBarLayout != null) {
//                        binding.appBarLayout.setVisibility(View.GONE);
//                    }
//                    if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "AppBar hidden for landscape mode");
//                } else {
//                    // Show AppBar in portrait
//                    if (binding.appBarLayout != null) {
//                        binding.appBarLayout.setVisibility(View.VISIBLE);
//                    }
//                    if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "AppBar visible for portrait mode");
//                }
//            } else {
//                Log.e(METHOD_TAG, "Toolbar not found in binding!");
//            }
//
//        } catch (Exception e) {
//            Log.e(METHOD_TAG, "Error configuring toolbar: " + e.getMessage());
//        }
//    }

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

//        // FIXED: Configure ActionBar only when toolbar is visible
//        if (!isLandscape && binding.toolbar != null &&
//                binding.toolbar.getVisibility() == View.VISIBLE) {
//
//            mAppBarConfiguration = new AppBarConfiguration.Builder(
//                    R.id.nav_dayslist, R.id.nav_calendar)
//                    .build();
//
//            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//            if (DEBUG_ACTIVITY) Log.d(TAG, "ActionBar configured for portrait mode");
//        } else {
//            if (DEBUG_ACTIVITY) Log.d(TAG, "Skipping ActionBar setup for landscape mode");
//        }
    }

//    /**
//     * FIXED: Create options menu with null-safe checks
//     */
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        final String METHOD_TAG = TAG + " onCreateOptionsMenu";
//
//        try {
//            // FIXED: Show menu only in portrait when toolbar is visible
//            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
//
//            // NULL-SAFE: Check if binding and toolbar exist
//            boolean toolbarVisible = binding != null &&
//                    binding.toolbar != null &&
//                    binding.toolbar.getVisibility() == View.VISIBLE;
//
//            if (isPortrait && toolbarVisible && sidebarNavigation == null) {
//                getMenuInflater().inflate(R.menu.toolbar_menu, menu);
//                if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Menu inflated for portrait mode");
//                return true;
//            }
//
//            if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Menu not created - conditions not met");
//            return super.onCreateOptionsMenu(menu);
//
//        } catch (Exception e) {
//            Log.e(METHOD_TAG, "Error creating options menu: " + e.getMessage());
//            return super.onCreateOptionsMenu(menu);
//        }
//    }

//    /**
//     * FIXED: Handle menu item selection with better error handling.
//     */
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        final String METHOD_TAG = TAG + " onOptionsItemSelected";
//
//        if (navController == null) {
//            Log.e(METHOD_TAG, "NavController is null, cannot handle menu selection");
//            return super.onOptionsItemSelected(item);
//        }
//
//        int mID = item.getItemId();
//
//        try {
//            if (mID == R.id.action_settings) {
//                Log.d(METHOD_TAG, "Navigating to Settings");
//                navController.navigate(R.id.nav_settings);
//                return true;
//            }
//            if (mID == R.id.action_about) {
//                Log.d(METHOD_TAG, "Navigating to About");
//                navController.navigate(R.id.nav_about);
//                return true;
//            }
//        } catch (Exception e) {
//            Log.e(METHOD_TAG, "Error during menu navigation: " + e.getMessage());
//
//            // FALLBACK: Try using Intent for Settings
//            if (mID == R.id.action_settings) {
//                try {
//                    Intent settingsIntent = new Intent(this, QDueSettingsActivity.class);
//                    startActivity(settingsIntent);
//                    return true;
//                } catch (Exception intentError) {
//                    Log.e(METHOD_TAG, "Fallback Intent also failed: " + intentError.getMessage());
//                }
//            }
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

//    /**
//     * FIXED: Support navigate up with null-safe checks
//     */
//    @Override
//    public boolean onSupportNavigateUp() {
//        final String METHOD_TAG = TAG + " onSupportNavigateUp";
//
//        try {
//            // Only handle navigation up if we have ActionBar visible (portrait mode)
//            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
//
//            // NULL-SAFE: Check if binding and toolbar exist
//            boolean toolbarVisible = binding != null &&
//                    binding.toolbar != null &&
//                    binding.toolbar.getVisibility() == View.VISIBLE;
//
//            if (navController != null && mAppBarConfiguration != null &&
//                    getSupportActionBar() != null && isPortrait && toolbarVisible) {
//
//                if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Handling navigation up");
//                return NavigationUI.navigateUp(navController, mAppBarConfiguration)
//                        || super.onSupportNavigateUp();
//            }
//
//            if (DEBUG_ACTIVITY) Log.d(METHOD_TAG, "Navigation up not handled - conditions not met");
//            return super.onSupportNavigateUp();
//
//        } catch (Exception e) {
//            Log.e(METHOD_TAG, "Error in onSupportNavigateUp: " + e.getMessage());
//            return super.onSupportNavigateUp();
//        }
//    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Preference changed: " + key);
    }

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
            Log.e(TAG, mTAG+ " Error during navigation: " + e.getMessage());
        }
    }
//    /**
//     * TEMPORARY DEBUG METHOD - Add to QDueMainActivity.java onCreate() after setContentView
//     */
//    private void debugToolbarVisibility() {
//        Log.d(TAG, "=== TOOLBAR DEBUG ===");
//        Log.d(TAG, "binding.toolbar: " + (binding.toolbar != null ? "EXISTS" : "NULL"));
//        Log.d(TAG, "binding.appBarLayout: " + (binding.appBarLayout != null ? "EXISTS" : "NULL"));
//
//        if (binding.toolbar != null) {
//            Log.d(TAG, "toolbar visibility: " + binding.toolbar.getVisibility());
//            Log.d(TAG, "toolbar height: " + binding.toolbar.getHeight());
//        }
//
//        if (binding.appBarLayout != null) {
//            Log.d(TAG, "appBarLayout visibility: " + binding.appBarLayout.getVisibility());
//            Log.d(TAG, "appBarLayout height: " + binding.appBarLayout.getHeight());
//        }
//
//        // Force toolbar to be visible for testing
//        if (binding.appBarLayout != null) {
//            binding.appBarLayout.setVisibility(View.VISIBLE);
//            binding.appBarLayout.setBackgroundColor(Color.RED); // Temporary red background
//        }
//
//        if (binding.toolbar != null) {
//            binding.toolbar.setVisibility(View.VISIBLE);
//            binding.toolbar.setBackgroundColor(Color.BLUE); // Temporary blue background
//        }
//    }
//
//    /**
//     * TEMPORARY: Force layout measurement - Add in onCreate() after setupToolbar()
//     */
//    private void forceToolbarLayout() {
//        if (binding.appBarLayout != null && binding.toolbar != null) {
//            // Force immediate layout
//            binding.appBarLayout.measure(
//                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
//                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//            );
//            binding.appBarLayout.layout(0, 0, binding.appBarLayout.getMeasuredWidth(), binding.appBarLayout.getMeasuredHeight());
//
//            // Post another debug check
//            binding.toolbar.post(() -> {
//                Log.d(TAG, "After force layout - toolbar height: " + binding.toolbar.getHeight());
//                Log.d(TAG, "After force layout - appBar height: " + binding.appBarLayout.getHeight());
//            });
//        }
//    }
//
//    /**
//     * TEMPORARY: DEBUG METHOD: Test if toolbar is hidden by content or z-index
//     * Add this in onCreate() after forceToolbarLayout()
//     */
//    private void debugToolbarOverlap() {
//        Log.d(TAG, "=== OVERLAP DEBUG ===");
//
//        if (binding.toolbar != null && binding.appBarLayout != null) {
//            // Test 1: Make toolbar highly visible with extreme styling
//            binding.toolbar.setBackgroundColor(Color.RED);
//            binding.toolbar.setElevation(100f); // Extreme elevation
//            binding.appBarLayout.setElevation(100f);
//            binding.appBarLayout.setBackgroundColor(Color.YELLOW);
//
//            // Test 2: Add temporary huge padding to content to see if overlap
//            View contentInclude = findViewById(R.id.nav_host_fragment_content_main);
//            if (contentInclude != null) {
//                contentInclude.setPadding(0, 200, 0, 0); // 200px top padding
//                Log.d(TAG, "Added 200px padding to content");
//            }
//
//            // Test 3: Log Z-order
//            Log.d(TAG, "AppBar elevation: " + binding.appBarLayout.getElevation());
//            Log.d(TAG, "Toolbar elevation: " + binding.toolbar.getElevation());
//
//            // Test 4: Check content bounds
//            if (contentInclude != null) {
//                contentInclude.post(() -> {
//                    int[] location = new int[2];
//                    contentInclude.getLocationOnScreen(location);
//                    Log.d(TAG, "Content top position: " + location[1]);
//                    Log.d(TAG, "Content height: " + contentInclude.getHeight());
//                });
//            }
//
//            // Test 5: Check toolbar bounds
//            binding.toolbar.post(() -> {
//                int[] location = new int[2];
//                binding.toolbar.getLocationOnScreen(location);
//                Log.d(TAG, "Toolbar top position: " + location[1]);
//                Log.d(TAG, "Toolbar bottom position: " + (location[1] + binding.toolbar.getHeight()));
//            });
//        }
//    }

}