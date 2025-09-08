package net.calvuz.qdue.ui.features.swipecalendar.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.core.services.QDueUserService;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.PatternAssignmentWizardLauncher;
import net.calvuz.qdue.ui.features.settings.SettingsLauncher;
import net.calvuz.qdue.ui.features.welcome.presentation.WelcomeActivity;

import java.time.LocalDate;

/**
 * SwipeCalendarActivity - Simplified Calendar Activity for Development and Debugging
 *
 * <p>Dedicated activity for hosting SwipeCalendarFragment in isolation, designed specifically
 * for debugging and development purposes. Provides a clean, distraction-free environment
 * to test calendar functionality without the complexity of QDueMainActivity.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Simplified Hosting</strong>: Single-purpose container for SwipeCalendarFragment</li>
 *   <li><strong>Debug-Friendly</strong>: Minimal dependencies and clear log messages</li>
 *   <li><strong>Clean Architecture</strong>: Full dependency injection compliance</li>
 *   <li><strong>Internationalization</strong>: Complete i18n support via LocaleManager</li>
 *   <li><strong>Development Focus</strong>: Easy analysis of calendar-specific logs and behavior</li>
 * </ul>
 *
 * <h3>Dependency Injection Architecture:</h3>
 * <ul>
 *   <li><strong>ServiceProvider Integration</strong>: Uses ServiceProviderImpl for core services</li>
 *   <li><strong>Injectable Implementation</strong>: Follows established DI patterns</li>
 *   <li><strong>LocaleManager Support</strong>: Full internationalization capabilities</li>
 *   <li><strong>Fragment Dependencies</strong>: Properly injected into SwipeCalendarFragment</li>
 * </ul>
 *
 * <h3>Intent Parameters:</h3>
 * <ul>
 *   <li><strong>EXTRA_INITIAL_DATE</strong>: Optional initial date (ISO format)</li>
 *   <li><strong>EXTRA_USER_ID</strong>: Optional user ID for data filtering</li>
 * </ul>
 *
 * <h3>Development Usage:</h3>
 * <pre>
 * // Launch with specific date:
 * Intent intent = SwipeCalendarActivity.createIntent(context, LocalDate.now());
 * startActivity(intent);
 *
 * // Launch with user context:
 * Intent intent = SwipeCalendarActivity.createIntent(context, null, userID);
 * startActivity(intent);
 * </pre>
 */
public class SwipeCalendarActivity extends AppCompatActivity implements Injectable
{
    private static final String TAG = "SwipeCalendarActivity";

    // ==================== INTENT EXTRAS ====================

    /**
     * Extra key for initial date parameter (ISO date string format)
     */
    public static final String EXTRA_INITIAL_DATE = "initial_date";

    /**
     * Extra key for user ID parameter (long value)
     */
    public static final String EXTRA_USER_ID = "user_id";

    // ==================== DEPENDENCIES ====================

    private ServiceProvider mServiceProvider;
    private LocaleManager mLocaleManager;

    // ==================== UI COMPONENTS ====================

    private Toolbar mToolbar;
    private SwipeCalendarFragment mCalendarFragment;

    // ==================== CONFIGURATION ====================

    private LocalDate mInitialDate;
    private String mUserId;

    // ==================== STATIC FACTORY METHODS ====================

    /**
     * Create intent for launching SwipeCalendarActivity.
     *
     * @param context Application context
     * @return Intent for launching activity
     */
    @NonNull
    public static Intent createIntent(@NonNull android.content.Context context) {
        return new Intent(context, SwipeCalendarActivity.class);
    }

    /**
     * Create intent for launching SwipeCalendarActivity with initial date.
     *
     * @param context Application context
     * @param initialDate Initial date to display (nullable)
     * @return Intent for launching activity
     */
    @NonNull
    public static Intent createIntent(@NonNull android.content.Context context, @Nullable LocalDate initialDate) {
        Intent intent = new Intent(context, SwipeCalendarActivity.class);
        if (initialDate != null) {
            intent.putExtra(EXTRA_INITIAL_DATE, initialDate.toString());
        }
        return intent;
    }

    /**
     * Create intent for launching SwipeCalendarActivity with full parameters.
     *
     * @param context Application context
     * @param initialDate Initial date to display (nullable)
     * @param userId User ID for data filtering (nullable)
     * @return Intent for launching activity
     */
    @NonNull
    public static Intent createIntent(@NonNull android.content.Context context,
                                      @Nullable LocalDate initialDate,
                                      @Nullable Long userId) {
        Intent intent = createIntent(context, initialDate);
        if (userId != null) {
            intent.putExtra(EXTRA_USER_ID, userId);
        }
        return intent;
    }

    // ==================== ACTIVITY LIFECYCLE ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "SwipeCalendarActivity onCreate()");

        // Check if user needs to see welcome before setting up main activity
        if (shouldRedirectToWelcome()) {
            redirectToWelcome();
            return;
        }

        // Set content view
        setContentView(R.layout.activity_swipe_calendar);

        // Initialize dependency injection
        initializeDependencyInjection();

        // Check for Default User
        checkDefaultUser();

        // Parse intent parameters
        parseIntentParameters();

        // Initialize UI components
        initializeUIComponents();

        // Setup fragment
        setupCalendarFragment(savedInstanceState);

        Log.d(TAG, "SwipeCalendarActivity initialization completed successfully");
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * Initialize dependency injection system.
     */
    private void initializeDependencyInjection() {
        Log.d(TAG, "Initializing dependency injection");

        try {
            // Get ServiceProvider instance
            DependencyInjector.inject( this, this );

            // Initialize services if needed
            if (!mServiceProvider.areServicesReady()) {
                mServiceProvider.initializeServices();
                Log.d(TAG, "Services initialized successfully");
            }

            // Initialize LocaleManager for internationalization
            mLocaleManager = new LocaleManager(getApplicationContext());

            Log.i(TAG, "Dependency injection completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize dependency injection", e);
            throw new RuntimeException("Critical error: Dependency injection failed", e);
        }
    }

    @Override
    public void inject(ServiceProvider serviceProvider) {
        // Implementation of Injectable interface
        this.mServiceProvider = serviceProvider;
    }

    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null &&
                mServiceProvider.areServicesReady() &&
                mLocaleManager != null;
    }

    /**
     * Get ServiceProvider instance for fragment injection.
     *
     * @return ServiceProvider instance
     */
    @NonNull
    public ServiceProvider getServiceProvider() {
        if (mServiceProvider == null) {
            throw new IllegalStateException("ServiceProvider not initialized");
        }
        return mServiceProvider;
    }

    // ==================== USER MANAGEMENT ====================

    private void checkDefaultUser() {

        QDueUserService userService = mServiceProvider.getQDueUserService();
        QDueUser user = userService.getPrimaryUser().join().getData();
        if (user == null) {
            // create a new default user (no welcome needed)
            QDueUser newUser = userService.createUser( "User","" )
                    .thenApply( result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            return result.getData();
                        } else {
                            throw new RuntimeException( "Failed to create default user" );
                        }
                    }).join();
            Log.i(TAG, "Created default user: " + newUser.getId());
        }
    }
    // ==================== MAIN ACTIVITY LOGIC ====================

    /**
     * Check if user should be redirected to WelcomeActivity
     *
     * @return true if welcome should be shown
     */
    private boolean shouldRedirectToWelcome() {
        boolean shouldShow = QDuePreferences.shouldShowWelcome( this );
        Log.d( TAG, "--> Should redirect to welcome: " + shouldShow );
        return shouldShow;
    }

    /**
     * Redirect user to WelcomeActivity for initial setup
     */
    private void redirectToWelcome() {
        Log.d( TAG, "----> Redirecting to WelcomeActivity" );

        Intent welcomeIntent = new Intent( this, WelcomeActivity.class );
        startActivity( welcomeIntent );
        finish(); // Close main activity
    }

    // ==================== LOCALE INTEGRATION ====================

    // Note: LocaleManager handles locale configuration automatically.
    // The Activity will use the current application locale set by LocaleManager.
    // No manual context updates needed - Android handles this through Configuration.

    // ==================== INTENT PARSING ====================

    /**
     * Parse intent parameters for initial configuration.
     */
    private void parseIntentParameters() {
        Intent intent = getIntent();

        // Parse initial date
        String dateString = intent.getStringExtra(EXTRA_INITIAL_DATE);
        if (dateString != null && !dateString.trim().isEmpty()) {
            try {
                mInitialDate = LocalDate.parse(dateString);
                Log.d(TAG, "Parsed initial date: " + mInitialDate);
            } catch (Exception e) {
                Log.w(TAG, "Invalid initial date format: " + dateString + ", using today", e);
                mInitialDate = null;
            }
        }

        // Parse user ID
        if (intent.hasExtra(EXTRA_USER_ID)) {
            String userId = intent.getStringExtra(EXTRA_USER_ID);
            if (!userId.isEmpty()) {
                mUserId = userId;
                Log.d(TAG, "Parsed user ID: " + mUserId);
            }
        }

        if (mInitialDate == null && mUserId == null) {
            Log.d(TAG, "No specific parameters provided, using defaults");
        }
    }

    // ==================== UI INITIALIZATION ====================

    /**
     * Initialize UI components and setup toolbar.
     */
    private void initializeUIComponents() {
        Log.d(TAG, "Initializing UI components");

        // Setup toolbar
        mToolbar = findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);

            // Configure toolbar for debug mode
            if (getSupportActionBar() != null) {
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);

                // Set localized title
                String title = getLocalizedString("debug_swipe_calendar_title", "Swipe Calendar Debug");
                getSupportActionBar().setTitle(title);
            }

            Log.d(TAG, "Toolbar configured successfully");
        } else {
            Log.w(TAG, "Toolbar not found in layout - continuing without toolbar");
        }
    }

    // ==================== FRAGMENT MANAGEMENT ====================

    /**
     * Setup SwipeCalendarFragment with proper dependency injection.
     *
     * @param savedInstanceState Saved instance state for fragment restoration
     */
    private void setupCalendarFragment(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Setting up SwipeCalendarFragment");

        if (savedInstanceState == null) {
            // Create new fragment instance
            mCalendarFragment = SwipeCalendarFragment.newInstance(mInitialDate, mUserId);

            // Add fragment to container
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.calendar_container, mCalendarFragment, "swipe_calendar")
                    .commit();

            Log.d(TAG, "SwipeCalendarFragment created and added to container");

        } else {
            // Restore existing fragment
            mCalendarFragment = (SwipeCalendarFragment) getSupportFragmentManager()
                    .findFragmentByTag("swipe_calendar");

            Log.d(TAG, "SwipeCalendarFragment restored from saved state");
        }

        if (mCalendarFragment == null) {
            Log.e(TAG, "Failed to setup SwipeCalendarFragment");
            throw new RuntimeException("Critical error: SwipeCalendarFragment setup failed");
        }
    }

    // ==================== OPTIONS MENU ====================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Handle toolbar back button
        if (itemId == android.R.id.home) {
            Log.d(TAG, "Back button pressed - finishing debug activity");
            finish();
            return true;
        }

        // Handle Settings
        if ( itemId == R.id.action_settings ) {
            if (SettingsLauncher.isAvailable()) {
                Log.d(TAG, "Settings button pressed - launching SettingsActivity");
                SettingsLauncher.launch( this );
            }
            return true;
        }

        // Handle Assignment
        if ( itemId == R.id.action_assignment ) {
            if (PatternAssignmentWizardLauncher.isAvailable()) {
                Log.d(TAG, "Assignment button pressed - launching AssignmentActivity");
                PatternAssignmentWizardLauncher.launch( this );

            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== RESULT HANDLING ====================

    /**
     * Handle results from launched activities, specifically assignment wizard.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if result is from assignment wizard
        if (PatternAssignmentWizardLauncher.isAssignmentUpdated(requestCode, resultCode)) {
            Log.d(TAG, "Assignment was updated - refreshing calendar data");

            // 🔄 Refresh calendar data
            refreshCalendarData();

            // Optional: Show confirmation message
            showAssignmentUpdateConfirmation();
        } else if (requestCode == PatternAssignmentWizardLauncher.REQUEST_FIRST_ASSIGNMENT ||
                requestCode == PatternAssignmentWizardLauncher.REQUEST_CHANGE_ASSIGNMENT) {
            // Assignment wizard was cancelled
            Log.d(TAG, "Assignment wizard was cancelled");
        }
    }

    /**
     * Refresh calendar data after assignment changes.
     * This method should reload data from your calendar service.
     */
    private void refreshCalendarData() {
        try {
            // 🔄 Delega al fragment per il refresh
            if (mCalendarFragment != null) {
                mCalendarFragment.refreshData();
            } else {
                // Trova il fragment se la referenza non è disponibile
                mCalendarFragment = (SwipeCalendarFragment) getSupportFragmentManager()
                        .findFragmentByTag("swipe_calendar");
                if (mCalendarFragment != null) {
                    mCalendarFragment.refreshData();
                }
            }

            Log.d(TAG, "Calendar data refresh completed");

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing calendar data", e);
        }
    }

    /**
     * Show confirmation that assignment was updated.
     * Optional user feedback method.
     */
    private void showAssignmentUpdateConfirmation() {
        // Mostra un Toast di conferma
        Toast.makeText( this, "Assignment updated successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Assignment update confirmation shown");
    }

    // ==================== INTERNATIONALIZATION HELPERS ====================

    /**
     * Get localized string using LocaleManager with fallback.
     *
     * @param key Resource key
     * @param fallback Fallback text if resource not found
     * @return Localized string
     */
    private String getLocalizedString(String key, String fallback) {
        if (mLocaleManager != null) {
            return LocaleManager.getLocalizedString(this, key, fallback);
        }
        return fallback;
    }

    // ==================== PUBLIC DEBUG METHODS ====================

    /**
     * Navigate calendar to specific date (for debugging purposes).
     *
     * @param targetDate Target date to navigate to
     */
    public void navigateToDate(@NonNull LocalDate targetDate) {
        if (mCalendarFragment != null) {
            Log.d(TAG, "Debug navigation to date: " + targetDate);
            // Note: This would require adding a public method to SwipeCalendarFragment
            // For now, we log the intent
        } else {
            Log.w(TAG, "Cannot navigate - fragment not ready");
        }
    }

    /**
     * Get current fragment for debugging purposes.
     *
     * @return Current SwipeCalendarFragment instance or null
     */
    @Nullable
    public SwipeCalendarFragment getCurrentFragment() {
        return mCalendarFragment;
    }

    /**
     * Check if debug activity is properly initialized.
     *
     * @return true if all components are ready
     */
    public boolean isDebugActivityReady() {
        return areDependenciesReady() &&
                mCalendarFragment != null &&
                mToolbar != null;
    }
}