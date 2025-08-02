package net.calvuz.qdue.ui.features.calendar.presentation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.core.infrastructure.di.Injectable;
import net.calvuz.qdue.core.infrastructure.di.ServiceProvider;
import net.calvuz.qdue.core.infrastructure.di.ServiceProviderImpl;
import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.ui.core.architecture.services.BackHandlingServiceImpl;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlerFactory;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlingModule;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.features.calendar.di.CalendarModule;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;

/**
 * CalendarActivity - Main activity for calendar view functionality.
 *
 * <p>Provides Google Calendar-like interface with:</p>
 * <ul>
 *   <li>Monthly calendar view with swipe navigation</li>
 *   <li>Shift schedule integration with color coding</li>
 *   <li>Event overlay indicators</li>
 *   <li>Quick event creation and management</li>
 *   <li>Loading states for async data operations</li>
 * </ul>
 *
 * <p>Architecture Features:</p>
 * <ul>
 *   <li>Dependency injection compliance via Injectable interface</li>
 *   <li>Service-based data management</li>
 *   <li>Back handling service integration</li>
 *   <li>Modular design with CalendarModule for feature DI</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarActivity extends AppCompatActivity implements
        Injectable, BackPressHandler {

    private static final String TAG = "CalendarActivity";

    // Intent extras
    public static final String EXTRA_INITIAL_DATE = "initial_date";
    public static final String EXTRA_VIEW_MODE = "view_mode";
    public static final String EXTRA_USER_ID = "user_id";

    // View modes
    public enum ViewMode {
        MONTH, WEEK, DAY
    }

    // ==================== DEPENDENCIES (DI) ====================

    private ServiceProvider mServiceProvider;
    private EventsService mEventsService;
    private UserService mUserService;
    private WorkScheduleService mWorkScheduleService;

    // Feature module
    private CalendarModule mCalendarModule;

    // Back handling
    private BackHandlingService mBackHandlingService;
    private BackHandlingModule mBackHandlingModule;

    // ==================== UI COMPONENTS ====================

    private MaterialToolbar mToolbar;
    private NavController mNavController;

    // Initial parameters
    private LocalDate mInitialDate;
    private ViewMode mViewMode = ViewMode.MONTH;
    private Long mUserId;

    // ==================== STATIC FACTORY METHODS ====================

    /**
     * Create intent to launch CalendarActivity.
     *
     * @param context Context for intent creation
     * @return Intent configured for CalendarActivity
     */
    public static Intent createIntent(@NonNull Context context) {
        return createIntent(context, LocalDate.now(), ViewMode.MONTH, null);
    }

    /**
     * Create intent to launch CalendarActivity with specific date.
     *
     * @param context Context for intent creation
     * @param initialDate Date to display initially
     * @return Intent configured for CalendarActivity
     */
    public static Intent createIntent(@NonNull Context context, @NonNull LocalDate initialDate) {
        return createIntent(context, initialDate, ViewMode.MONTH, null);
    }

    /**
     * Create intent to launch CalendarActivity with full configuration.
     *
     * @param context Context for intent creation
     * @param initialDate Date to display initially
     * @param viewMode Initial view mode (month/week/day)
     * @param userId User ID for schedule filtering (null for current user)
     * @return Intent configured for CalendarActivity
     */
    public static Intent createIntent(@NonNull Context context, @NonNull LocalDate initialDate,
                                      @NonNull ViewMode viewMode, @Nullable Long userId) {
        Intent intent = new Intent(context, CalendarActivity.class);
        intent.putExtra(EXTRA_INITIAL_DATE, initialDate.toString());
        intent.putExtra(EXTRA_VIEW_MODE, viewMode.name());
        if (userId != null) {
            intent.putExtra(EXTRA_USER_ID, userId);
        }
        return intent;
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Creating CalendarActivity");

        // Extract intent parameters
        extractIntentParameters();

        // Initialize dependency injection
        initializeDependencyInjection();

        // Set up UI
        setContentView(R.layout.activity_calendar);
        initializeViews();
        setupToolbar();
        setupNavigation();

        // Initialize feature module
        initializeCalendarModule();

        // Initialize back handling
        initializeBackHandling();

        Log.d(TAG, "CalendarActivity created successfully");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying CalendarActivity");

        // Cleanup feature module
        cleanupCalendarModule();

        // Cleanup back handling
        cleanupBackHandling();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Use back handling service
        if (mBackHandlingService != null && mBackHandlingService.handleBackPress(this)) {
            Log.d(TAG, "Back press handled by service");
            return;
        }

        // Default back handling
        super.onBackPressed();
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * Initialize dependency injection system.
     */
    private void initializeDependencyInjection() {
        try {
            mServiceProvider = ServiceProviderImpl.getInstance(this);
            mServiceProvider.initializeServices();

            // Inject dependencies
            inject(mServiceProvider);

            if (!areDependenciesReady()) {
                throw new IllegalStateException("Calendar dependencies not ready");
            }

            Log.d(TAG, "Dependency injection initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize dependency injection", e);
            finish();
        }
    }

    @Override
    public void inject(ServiceProvider serviceProvider) {
        mEventsService = serviceProvider.getEventsService();
        mUserService = serviceProvider.getUserService();
        mWorkScheduleService = serviceProvider.getWorkScheduleService();

        Log.d(TAG, "Services injected successfully");
    }

    @Override
    public boolean areDependenciesReady() {
        return mEventsService != null &&
                mUserService != null &&
                mWorkScheduleService != null;
    }

    // ==================== INITIALIZATION METHODS ====================

    /**
     * Extract parameters from intent.
     */
    private void extractIntentParameters() {
        Intent intent = getIntent();

        // Initial date
        String dateStr = intent.getStringExtra(EXTRA_INITIAL_DATE);
        if (dateStr != null) {
            try {
                mInitialDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                Log.w(TAG, "Invalid initial date: " + dateStr);
                mInitialDate = LocalDate.now();
            }
        } else {
            mInitialDate = LocalDate.now();
        }

        // View mode
        String viewModeStr = intent.getStringExtra(EXTRA_VIEW_MODE);
        if (viewModeStr != null) {
            try {
                mViewMode = ViewMode.valueOf(viewModeStr);
            } catch (Exception e) {
                Log.w(TAG, "Invalid view mode: " + viewModeStr);
                mViewMode = ViewMode.MONTH;
            }
        }

        // User ID
        if (intent.hasExtra(EXTRA_USER_ID)) {
            mUserId = intent.getLongExtra(EXTRA_USER_ID, -1L);
            if (mUserId == -1L) {
                mUserId = null;
            }
        }

        Log.d(TAG, "Intent parameters: date=" + mInitialDate +
                ", viewMode=" + mViewMode + ", userId=" + mUserId);
    }

    /**
     * Initialize UI views.
     */
    private void initializeViews() {
        mToolbar = findViewById(R.id.toolbar);
    }

    /**
     * Setup toolbar with calendar-specific features.
     */
    private void setupToolbar() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.calendar_title);
            }
        }
    }

    /**
     * Setup navigation with calendar fragment.
     */
    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                mNavController = navHostFragment.getNavController();

                // Pass initial parameters to fragment
                Bundle args = new Bundle();
                args.putString(CalendarViewFragment.ARG_INITIAL_DATE, mInitialDate.toString());
                args.putString(CalendarViewFragment.ARG_VIEW_MODE, mViewMode.name());
                if (mUserId != null) {
                    args.putLong(CalendarViewFragment.ARG_USER_ID, mUserId);
                }

                // Navigate to calendar fragment with args
                mNavController.navigate(R.id.calendarViewFragment, args);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation", e);
        }
    }

    /**
     * Initialize calendar feature module.
     */
    private void initializeCalendarModule() {
        try {
            mCalendarModule = new CalendarModule(
                    this,
                    mEventsService,
                    mUserService,
                    mWorkScheduleService
            );

            if (!mCalendarModule.areDependenciesReady()) {
                throw new IllegalStateException("Calendar module dependencies not ready");
            }

            Log.d(TAG, "Calendar module initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize calendar module", e);
        }
    }

    /**
     * Initialize back handling service.
     */
    private void initializeBackHandling() {
        try {
            mBackHandlingModule = new BackHandlingModule(this);
            mBackHandlingService = mBackHandlingModule.provideBackHandlingService();

            // Register this activity as back handler
            BackHandlerFactory.create(this)
                    .withPriority(BackHandlerFactory.Priority.ACTIVITY)
                    .withDescription("CalendarActivity")
                    .register(mBackHandlingService);

            Log.d(TAG, "Back handling initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing back handling", e);
        }
    }

    // ==================== CLEANUP METHODS ====================

    /**
     * Cleanup calendar module.
     */
    private void cleanupCalendarModule() {
        if (mCalendarModule != null) {
            mCalendarModule.onDestroy();
            mCalendarModule = null;
        }
    }

    /**
     * Cleanup back handling.
     */
    private void cleanupBackHandling() {
        if (mBackHandlingModule != null) {
            mBackHandlingModule.onDestroy();
            mBackHandlingModule = null;
        }
        mBackHandlingService = null;
    }

    // ==================== MENU HANDLING ====================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ==================== BACK PRESS HANDLER ====================

    @Override
    public boolean onBackPressed() {
        // Let fragments handle back press first
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof BackPressHandler) {
            BackPressHandler handler = (BackPressHandler) currentFragment;
            if (handler.onBackPressed()) {
                return true; // Handled by fragment
            }
        }

        // Default behavior - finish activity
        finish();
        return true;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get current fragment from navigation.
     */
    @Nullable
    private Fragment getCurrentFragment() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                return navHostFragment.getChildFragmentManager().getFragments().get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current fragment", e);
        }
        return null;
    }

    /**
     * Get calendar module for fragment access.
     */
    @Nullable
    public CalendarModule getCalendarModule() {
        return mCalendarModule;
    }

    /**
     * Check if calendar module is ready.
     */
    public boolean isCalendarModuleReady() {
        return mCalendarModule != null && mCalendarModule.areDependenciesReady();
    }
}