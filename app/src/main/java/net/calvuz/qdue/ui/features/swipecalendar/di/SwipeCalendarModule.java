package net.calvuz.qdue.ui.features.swipecalendar.di;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.UseCaseFactory;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.features.swipecalendar.adapters.MonthPagerAdapter;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SwipeCalendarModule - Clean Architecture Dependency Injection Module
 *
 * <p>Provides centralized dependency management for all SwipeCalendar-related components
 * following clean architecture principles and established DI patterns. Integrates seamlessly
 * with async repository layer and use case architecture.</p>
 *
 * <h3>Clean Architecture Integration:</h3>
 * <ul>
 *   <li><strong>Repository Layer</strong>: Uses WorkScheduleRepository for data access</li>
 *   <li><strong>Use Case Layer</strong>: Delegates business logic to GenerateUserScheduleUseCase</li>
 *   <li><strong>Async Operations</strong>: All data operations use CompletableFuture pattern</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI with proper lifecycle</li>
 * </ul>
 *
 * <h3>Module Features:</h3>
 * <ul>
 *   <li><strong>Single User Application</strong>: Optimized for single user workflow</li>
 *   <li><strong>Default User Support</strong>: Automatic default user management</li>
 *   <li><strong>Lazy Initialization</strong>: Components created on-demand for performance</li>
 *   <li><strong>Lifecycle Management</strong>: Proper cleanup and resource disposal</li>
 *   <li><strong>Thread Safety</strong>: Safe component creation and access</li>
 *   <li><strong>Service Integration</strong>: Seamless integration with existing service layer</li>
 *   <li><strong>Error Handling</strong>: Consistent error handling across all data operations</li>
 * </ul>
 *
 * <h3>Provided Components:</h3>
 * <ul>
 *   <li>MonthPagerAdapter with async data loading via use cases</li>
 *   <li>SwipeCalendarStateManager for position persistence</li>
 *   <li>AsyncDataLoader implementation for events and work schedule</li>
 *   <li>GenerateUserScheduleUseCase for work schedule operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.1.0 - Single User Implementation
 * @since Database Version 6
 */
public class SwipeCalendarModule {

    private static final String TAG = "SwipeCalendarModule";

    // ==================== SINGLE USER CONFIGURATION ====================

    /**
     * Default user ID for single user application.
     * Uses 1L as conventional default user identifier.
     */
    private static final Long DEFAULT_USER_ID = 1L;

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== CLEAN ARCHITECTURE COMPONENTS ====================

    private UseCaseFactory mUseCaseFactory;
    private GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;

    // ==================== CACHED INSTANCES ====================

    private SwipeCalendarStateManager mStateManager;
    private MonthPagerAdapter mPagerAdapter;
    private AsyncDataLoader mDataLoader;

    // ==================== CONFIGURATION ====================

    private Long mCurrentUserId = DEFAULT_USER_ID;
    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates SwipeCalendarModule with required dependencies for clean architecture.
     *
     * @param context                Application context
     * @param eventsService          Service for events data operations
     * @param userService            Service for user data operations
     * @param workScheduleRepository Repository for work schedule operations
     */
    public SwipeCalendarModule(@NonNull Context context,
                               @NonNull EventsService eventsService,
                               @NonNull UserService userService,
                               @NonNull WorkScheduleRepository workScheduleRepository) {

        // Validate context for proper theming capabilities
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Warning: Context is not an Activity - theme resolution may fail");
        }

        this.mContext = context;
        this.mEventsService = eventsService;
        this.mUserService = userService;
        this.mWorkScheduleRepository = workScheduleRepository;

        // Initialize clean architecture components
        initializeCleanArchitecture();

        Log.d(TAG, "SwipeCalendarModule created with clean architecture support (single user: " + mCurrentUserId + ")");
    }

    // ==================== CLEAN ARCHITECTURE INITIALIZATION ====================

    /**
     * Initialize use case infrastructure and clean architecture components.
     * Sets up the dependency chain: Repository → Use Cases → Module.
     */
    private void initializeCleanArchitecture() {
        try {
            // Create use case factory with repository dependency
            this.mUseCaseFactory = new UseCaseFactory(mWorkScheduleRepository);

            // Get user schedule use case instance for work schedule operations
            this.mGenerateUserScheduleUseCase = mUseCaseFactory.getUserScheduleUseCase();

            Log.d(TAG, "✅ Clean architecture components initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize clean architecture components", e);
            throw new RuntimeException("Clean architecture initialization failed", e);
        }
    }

    // ==================== USE CASE PROVIDERS ====================

    /**
     * Get user schedule use case for work schedule operations.
     * Provides access to business logic layer for calendar work schedule operations.
     *
     * @return GenerateUserScheduleUseCase instance
     * @throws IllegalStateException if module has been destroyed
     */
    @NonNull
    public GenerateUserScheduleUseCase getUserScheduleUseCase() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Module has been destroyed");
        }
        return mGenerateUserScheduleUseCase;
    }

    /**
     * Get use case factory for additional use case creation.
     *
     * @return UseCaseFactory instance
     * @throws IllegalStateException if module has been destroyed
     */
    @NonNull
    public UseCaseFactory getUseCaseFactory() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Module has been destroyed");
        }
        return mUseCaseFactory;
    }

    // ==================== COMPONENT PROVIDERS ====================

    /**
     * Provides SwipeCalendarStateManager instance.
     * Manages calendar position state persistence.
     *
     * @return State manager instance
     * @throws IllegalStateException if module has been destroyed
     */
    @NonNull
    public synchronized SwipeCalendarStateManager provideStateManager() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Module has been destroyed");
        }

        if (mStateManager == null) {
            mStateManager = new SwipeCalendarStateManager(mContext);
            Log.d(TAG, "Created SwipeCalendarStateManager");
        }

        return mStateManager;
    }

    /**
     * Provides MonthPagerAdapter instance with clean architecture data loading.
     * Manages ViewPager2 month navigation with async use case integration.
     *
     * @return Pager adapter instance
     * @throws IllegalStateException if module has been destroyed
     */
    @NonNull
    public synchronized MonthPagerAdapter providePagerAdapter() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Module has been destroyed");
        }

        if (mPagerAdapter == null) {
            AsyncDataLoader dataLoader = provideDataLoader();
            mPagerAdapter = new MonthPagerAdapter(mContext, dataLoader);
            Log.d(TAG, "Created MonthPagerAdapter with clean architecture data loading");
        }

        return mPagerAdapter;
    }

    /**
     * Provides AsyncDataLoader implementation.
     * Handles loading of events and work schedule data using clean architecture patterns.
     *
     * @return Data loader instance
     */
    @NonNull
    private synchronized AsyncDataLoader provideDataLoader() {
        if (mDataLoader == null) {
            mDataLoader = new AsyncDataLoader();
            Log.d(TAG, "Created AsyncDataLoader");
        }

        return mDataLoader;
    }

    // ==================== ASYNC DATA LOADER IMPLEMENTATION ====================

    /**
     * AsyncDataLoader - Clean Architecture Data Loading Implementation
     *
     * <p>Handles all data loading operations using clean architecture patterns:
     * - Events: Direct service calls (maintained for compatibility)
     * - Work Schedule: Use case delegation for business logic separation
     * - Async operations: CompletableFuture with proper error handling
     * - Thread safety: All operations handle concurrency properly
     * - Single User: Optimized for single user application workflow</p>
     */
    private class AsyncDataLoader implements MonthPagerAdapter.DataLoader {

        /**
         * Load events for specified month using EventsService.
         * Maintains direct service usage for events as they don't require complex business logic.
         *
         * @param month Target month
         * @param callback Callback for async result delivery
         */
        @Override
        public void loadEventsForMonth(@NonNull YearMonth month,
                                       @NonNull MonthPagerAdapter.DataCallback<Map<LocalDate, List<LocalEvent>>> callback) {

            Log.d(TAG, "Loading events for month: " + month);

            try {
                LocalDate startDate = month.atDay(1);
                LocalDate endDate = month.atEndOfMonth();

                mEventsService.getEventsForDateRange(startDate, endDate)
                        .thenAccept(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                try {
                                    // Group events by date for calendar display
                                    Map<LocalDate, List<LocalEvent>> eventsMap = groupEventsByDate(result.getData());

                                    callback.onSuccess(eventsMap);
                                    Log.d(TAG, "✅ Events loaded successfully for " + month + " (" + eventsMap.size() + " dates)");

                                } catch (Exception e) {
                                    Log.e(TAG, "❌ Failed to process events for " + month, e);
                                    callback.onError(e);
                                }
                            } else {
                                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error";
                                Exception error = new RuntimeException("Failed to load events: " + errorMsg);
                                Log.w(TAG, "❌ Events service returned error for " + month + ": " + errorMsg);
                                callback.onError(error);
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ Exception in async events loading for " + month, throwable);
                            callback.onError(new RuntimeException("Async events loading failed", throwable));
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to initiate events loading for " + month, e);
                callback.onError(e);
            }
        }

        /**
         * Load work schedule for specified month using GenerateUserScheduleUseCase.
         * Delegates to GenerateUserScheduleUseCase for proper business logic separation.
         * Uses default user ID for single user application workflow.
         *
         * @param month Target month
         * @param callback Callback for async result delivery
         */
        @Override
        public void loadWorkScheduleForMonth(@NonNull YearMonth month,
                                             @NonNull MonthPagerAdapter.DataCallback<Map<LocalDate, WorkScheduleDay>> callback) {

            Log.d(TAG, "Loading work schedule for month: " + month + " (userId: " + mCurrentUserId + ")");

            try {
                // Use GenerateUserScheduleUseCase.executeForMonth with current user ID
                mGenerateUserScheduleUseCase.executeForMonth(mCurrentUserId, month)
                        .thenAccept(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                callback.onSuccess(result.getData());
                                Log.d(TAG, "✅ Work schedule loaded successfully via GenerateUserScheduleUseCase for " + month +
                                        " (user: " + mCurrentUserId + ", " + result.getData().size() + " days)");
                            } else {
                                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error";
                                Exception error = new RuntimeException("Failed to load work schedule: " + errorMsg);
                                Log.w(TAG, "❌ GenerateUserScheduleUseCase returned error for " + month + ": " + errorMsg);
                                callback.onError(error);
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ Exception in async work schedule loading for " + month, throwable);
                            callback.onError(new RuntimeException("Async work schedule loading failed", throwable));
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to initiate work schedule loading for " + month, e);
                callback.onError(e);
            }
        }

        /**
         * Helper method to group events by date for calendar display.
         * Handles different event types (all-day, timed) and extracts appropriate dates.
         *
         * @param events List of events to group
         * @return Map with dates as keys and lists of events as values
         */
        @NonNull
        private Map<LocalDate, List<LocalEvent>> groupEventsByDate(@NonNull List<LocalEvent> events) {
            Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();

            for (LocalEvent event : events) {
                if (event != null) {
                    try {
                        LocalDate eventDate = extractEventDate(event);
                        eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);

                    } catch (Exception e) {
                        Log.w(TAG, "Failed to extract date from event: " + event.getTitle(), e);
                        // Skip malformed events rather than failing entire operation
                    }
                }
            }

            return eventsMap;
        }

        /**
         * Extract date from LocalEvent handling different event types properly.
         * Supports all-day events, timed events, and provides fallback logic.
         *
         * @param event Event to extract date from
         * @return LocalDate for the event
         */
        @NonNull
        private LocalDate extractEventDate(@NonNull LocalEvent event) {
            try {
                // Priority 1: All-day events should use the specific date
                if (event.isAllDay() && event.getDate() != null) {
                    return event.getDate();
                }

                // Priority 2: Timed events should use start time date
                if (event.getStartTime() != null) {
                    return event.getStartTime().toLocalDate();
                }

                // Priority 3: If no start time, try end time
                if (event.getEndTime() != null) {
                    return event.getEndTime().toLocalDate();
                }

                // Priority 4: If all-day but no specific date, try date field anyway
                if (event.getDate() != null) {
                    return event.getDate();
                }

                // Last resort: Use today (this should rarely happen)
                Log.w(TAG, "Event has no valid date, using today: " + event.getTitle());
                return LocalDate.now();

            } catch (Exception e) {
                Log.e(TAG, "Error extracting event date for: " + event.getTitle(), e);
                return LocalDate.now();
            }
        }
    }

    // ==================== SINGLE USER CONFIGURATION ====================

    /**
     * Set current user ID for data operations.
     * For single user applications, this typically remains the default user.
     *
     * @param userId User ID, or null to use default user
     */
    public synchronized void setCurrentUserId(@Nullable Long userId) {
        this.mCurrentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Log.d(TAG, "Current user ID updated to: " + this.mCurrentUserId);
    }

    /**
     * Get current user ID being used for data operations.
     *
     * @return Current user ID (always non-null, defaults to DEFAULT_USER_ID)
     */
    @NonNull
    public Long getCurrentUserId() {
        return mCurrentUserId;
    }

    /**
     * Get default user ID for single user application.
     *
     * @return Default user ID constant
     */
    @NonNull
    public static Long getDefaultUserId() {
        return DEFAULT_USER_ID;
    }

    /**
     * Reset to default user configuration.
     * Useful for debugging or application reset scenarios.
     */
    public synchronized void resetToDefaultUser() {
        setCurrentUserId(DEFAULT_USER_ID);
        Log.d(TAG, "Reset to default user: " + DEFAULT_USER_ID);
    }

    // ==================== DEPENDENCY VALIDATION ====================

    /**
     * Check if all dependencies are ready for operation.
     * Validates that all required services and clean architecture components are available.
     *
     * @return true if module is ready for use
     */
    public boolean areDependenciesReady() {
        return !mIsDestroyed &&
                mEventsService != null &&
                mUserService != null &&
                mWorkScheduleRepository != null &&
                mUseCaseFactory != null &&
                mGenerateUserScheduleUseCase != null;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Called when hosting fragment goes to background.
     * Marks state as inactive to trigger proper restoration logic on resume.
     */
    public void onFragmentPause() {
        if (mStateManager != null) {
            mStateManager.markSessionInactive();
        }
        Log.d(TAG, "Fragment paused, session marked inactive");
    }

    /**
     * Called when hosting fragment resumes from background.
     * Can be used to refresh data or update state as needed.
     */
    public void onFragmentResume() {
        Log.d(TAG, "Fragment resumed");
        // Future: Add any necessary resume logic here
    }

    /**
     * Called when hosting fragment is destroyed.
     * Cleanup all resources and cached instances to prevent memory leaks.
     */
    public synchronized void onDestroy() {
        if (mIsDestroyed) {
            return;
        }

        Log.d(TAG, "Destroying SwipeCalendarModule");

        try {
            // Cleanup pager adapter
            if (mPagerAdapter != null) {
                mPagerAdapter.cleanup();
                mPagerAdapter = null;
            }

            // Clear data loader reference
            mDataLoader = null;

            // Cleanup use case infrastructure
            if (mUseCaseFactory != null) {
                mUseCaseFactory.cleanup();
                mUseCaseFactory = null;
            }

            // Clear use case references
            mGenerateUserScheduleUseCase = null;

            // Clear state manager reference
            mStateManager = null;

            // Reset to default user configuration
            mCurrentUserId = DEFAULT_USER_ID;

            mIsDestroyed = true;
            Log.d(TAG, "✅ SwipeCalendarModule destroyed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during SwipeCalendarModule destruction", e);
            mIsDestroyed = true; // Mark as destroyed even if cleanup failed
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get comprehensive module information for debugging and monitoring.
     * Provides detailed status of all module components and dependencies.
     *
     * @return Human readable module status and configuration
     */
    @NonNull
    public String getModuleInfo() {
        StringBuilder info = new StringBuilder();
        info.append("SwipeCalendarModule Status (Single User):\n");
        info.append("──────────────────────────────────────────\n");
        info.append("• Destroyed: ").append(mIsDestroyed).append("\n");
        info.append("• Dependencies Ready: ").append(areDependenciesReady()).append("\n");
        info.append("• Current User ID: ").append(mCurrentUserId).append("\n");
        info.append("• Default User ID: ").append(DEFAULT_USER_ID).append("\n");
        info.append("\nComponents:\n");
        info.append("• State Manager: ").append(mStateManager != null ? "✅ Created" : "❌ Not Created").append("\n");
        info.append("• Pager Adapter: ").append(mPagerAdapter != null ? "✅ Created" : "❌ Not Created").append("\n");
        info.append("• Data Loader: ").append(mDataLoader != null ? "✅ Created" : "❌ Not Created").append("\n");
        info.append("\nClean Architecture:\n");
        info.append("• Use Case Factory: ").append(mUseCaseFactory != null ? "✅ Ready" : "❌ Not Ready").append("\n");
        info.append("• GenerateUserScheduleUseCase: ").append(mGenerateUserScheduleUseCase != null ? "✅ Ready" : "❌ Not Ready").append("\n");
        info.append("\nServices:\n");
        info.append("• Events Service: ").append(mEventsService != null ? "✅ Available" : "❌ Unavailable").append("\n");
        info.append("• User Service: ").append(mUserService != null ? "✅ Available" : "❌ Unavailable").append("\n");
        info.append("• WorkSchedule Repository: ").append(mWorkScheduleRepository != null ? "✅ Available" : "❌ Unavailable");

        return info.toString();
    }

    /**
     * Validate module state and dependencies.
     * Useful for debugging and ensuring proper module configuration.
     *
     * @return Map containing validation results
     */
    @NonNull
    public Map<String, Object> validateModuleState() {
        Map<String, Object> validation = new HashMap<>();

        validation.put("module_destroyed", mIsDestroyed);
        validation.put("dependencies_ready", areDependenciesReady());
        validation.put("events_service_available", mEventsService != null);
        validation.put("user_service_available", mUserService != null);
        validation.put("work_schedule_repository_available", mWorkScheduleRepository != null);
        validation.put("use_case_factory_ready", mUseCaseFactory != null);
        validation.put("generate_user_schedule_use_case_ready", mGenerateUserScheduleUseCase != null);
        validation.put("state_manager_created", mStateManager != null);
        validation.put("pager_adapter_created", mPagerAdapter != null);
        validation.put("data_loader_created", mDataLoader != null);
        validation.put("current_user_id", mCurrentUserId);
        validation.put("default_user_id", DEFAULT_USER_ID);

        return validation;
    }
}