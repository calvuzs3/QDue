package net.calvuz.qdue;

import static net.calvuz.qdue.QDue.Debug.DEBUG_ACTIVITY;
import static net.calvuz.qdue.QDue.Debug.DEBUG_COLORS;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
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
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.utils.ThemeManager;
import net.calvuz.qdue.utils.ThemeUtils;
import net.calvuz.qdue.utils.TimeChangeReceiver;

/**
 * Main Activity with responsive toolbar visibility.
 * - Portrait: Toolbar visible + BottomNavigation
 * - Landscape: Toolbar hidden + Sidebar Navigation
 *
 * Key improvement: Toolbar always exists in binding, just hidden in landscape
 */
public class QDueMainActivity extends AppCompatActivity
        implements
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
    private Fragment mCurrentFragment = null;

    // Time Change Receiver
    private TimeChangeReceiver mTimeChangeReceiver;
    private boolean mReceiverRegistered = false;

    // Shared Preferences
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Test theme colors if needed
        if (DEBUG_COLORS) testThemeColors();

        Log.d(TAG, String.valueOf(QDue.getContext() == getApplicationContext()));

        // Initialize time change receiver
        mTimeChangeReceiver = new TimeChangeReceiver(this);

        // Register preference listener
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Initialize binding and layout
        binding = ActivityQdueMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configure toolbar (always exists now, may be hidden)
        setupToolbar();

        // Setup navigation with safe error handling
        setupNavigationSafely();

        // Track current fragment
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            mCurrentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (DEBUG_ACTIVITY)
                Log.d(TAG, "Fragment changed: " + (mCurrentFragment != null ? mCurrentFragment.getClass().getSimpleName() : "null"));
        });
    }

    /**
     * Setup toolbar with orientation-aware visibility.
     * Toolbar exists in both orientations but is hidden in landscape.
     */
    private void setupToolbar() {
        try {
            // Toolbar always exists in binding now
            setSupportActionBar(binding.toolbar);

            // Check orientation and adjust visibility
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (isLandscape) {
                // Hide toolbar and AppBar in landscape
                binding.toolbar.setVisibility(View.GONE);
                if (binding.appBarLayout != null) {
                    binding.appBarLayout.setVisibility(View.GONE);
                }
                if (DEBUG_ACTIVITY) Log.d(TAG, "Toolbar hidden for landscape mode");
            } else {
                // Show toolbar in portrait
                binding.toolbar.setVisibility(View.VISIBLE);
                if (binding.appBarLayout != null) {
                    binding.appBarLayout.setVisibility(View.VISIBLE);
                }
                if (DEBUG_ACTIVITY) Log.d(TAG, "Toolbar visible for portrait mode");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error configuring toolbar: " + e.getMessage());
        }
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
                if (DEBUG_ACTIVITY) Log.d(TAG, "NavController found via Navigation.findNavController");
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
     * Setup navigation components based on current orientation.
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

        // Configure ActionBar only in portrait mode (when toolbar is visible)
        if (!isLandscape && binding.toolbar.getVisibility() == View.VISIBLE) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_dayslist, R.id.nav_calendar)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            if (DEBUG_ACTIVITY) Log.d(TAG, "ActionBar configured for portrait mode");
        } else {
            if (DEBUG_ACTIVITY) Log.d(TAG, "Skipping ActionBar setup for landscape mode");
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
                        navController.navigate(R.id.nav_settings);
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
     * Update toolbar title safely.
     */
    private void updateToolbarTitle() {
        // Only update title if ActionBar exists and is visible
        if (getSupportActionBar() != null && binding.toolbar.getVisibility() == View.VISIBLE) {
            getSupportActionBar().setTitle(R.string.app_name);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show menu for portrait mode (when there's no sidebar)
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (sidebarNavigation == null && isPortrait) {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (navController == null) {
            return super.onOptionsItemSelected(item);
        }

        int mID = item.getItemId();

        try {
            if (mID == R.id.action_settings) {
                navController.navigate(R.id.nav_settings);
                return true;
            }
            if (mID == R.id.action_about) {
                navController.navigate(R.id.nav_about);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during menu navigation: " + e.getMessage());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Only handle navigation up if we have ActionBar visible (portrait mode)
        if (navController != null && mAppBarConfiguration != null &&
                getSupportActionBar() != null && binding.toolbar.getVisibility() == View.VISIBLE) {
            return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                    || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }

    // ... (rest of the existing methods remain unchanged)

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

    private void testThemeColors() {
        // Test dynamic colors
        int surfaceColor = ThemeUtils.getDynamicSurfaceColor(this);
        int onSurfaceColor = ThemeUtils.getDynamicOnSurfaceColor(this);
        int primaryColor = ThemeUtils.getDynamicPrimaryColor(this);

        // Test app-specific colors
        int todayBg = ThemeUtils.getTodayBackgroundColor(this);
        int userShiftBg = ThemeUtils.getUserShiftBackgroundColor(this);
        int sundayText = ThemeUtils.getSundayTextColor(this);

        Log.d("ThemeTest", "=== DYNAMIC COLORS ===");
        Log.d("ThemeTest", "Surface: " + Integer.toHexString(surfaceColor));
        Log.d("ThemeTest", "OnSurface: " + Integer.toHexString(onSurfaceColor));
        Log.d("ThemeTest", "Primary: " + Integer.toHexString(primaryColor));

        Log.d("ThemeTest", "=== APP COLORS ===");
        Log.d("ThemeTest", "Today BG: " + Integer.toHexString(todayBg));
        Log.d("ThemeTest", "User Shift BG: " + Integer.toHexString(userShiftBg));
        Log.d("ThemeTest", "Sunday Text: " + Integer.toHexString(sundayText));

        // Test dark mode detection
        ThemeManager themeManager = ThemeManager.getInstance(this);
        Log.d("ThemeTest", "Is Dark Mode: " + themeManager.isDarkMode());

        // Compare with Material Design
        int materialSurface = ThemeUtils.getMaterialSurfaceColor(this);
        int yourSurface = getColor(R.color.surface);

        Log.d("ThemeTest", "=== COMPARISON ===");
        Log.d("ThemeTest", "Material Surface: " + Integer.toHexString(materialSurface));
        Log.d("ThemeTest", "Your Surface: " + Integer.toHexString(yourSurface));

        if (materialSurface != yourSurface) {
            Log.d("ThemeTest", "✅ Your colors are different from Material Design (correct!)");
        } else {
            Log.w("ThemeTest", "⚠️ Colors are the same as Material Design");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (DEBUG_ACTIVITY) Log.d(TAG, "Preference changed: " + key);
    }
}