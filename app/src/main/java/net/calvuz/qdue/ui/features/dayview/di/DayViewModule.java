package net.calvuz.qdue.ui.features.dayview.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.features.dayview.components.DayViewStateManager;
import net.calvuz.qdue.ui.features.dayview.components.DayViewDataLoader;
import net.calvuz.qdue.ui.features.dayview.components.DayViewEventOperations;
import net.calvuz.qdue.ui.features.dayview.adapters.DayViewEventsAdapter;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * DayViewModule - Clean Architecture Dependency Injection Module
 *
 * <p>Provides centralized dependency management for all DayView-related components
 * following clean architecture principles and established DI patterns.
 * Manages display and interaction with daily calendar events including LocalEvent
 * and WorkScheduleDay with extensibility for future event types.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-Event Support</strong>: LocalEvent, WorkScheduleDay, extensible architecture</li>
 *   <li><strong>CRUD Operations</strong>: Create, read, update, delete events with validation</li>
 *   <li><strong>Bulk Operations</strong>: Multi-selection support for batch actions</li>
 *   <li><strong>Interactive Operations</strong>: Share, copy to clipboard, duplicate events</li>
 *   <li><strong>Real-time Updates</strong>: Reactive data loading when changes occur</li>
 * </ul>
 *
 * <h3>Clean Architecture Integration:</h3>
 * <ul>
 *   <li><strong>Repository Layer</strong>: Uses LocalEventsRepository and WorkScheduleRepository</li>
 *   <li><strong>Use Case Layer</strong>: Delegates business logic to specialized use cases</li>
 *   <li><strong>Async Operations</strong>: All data operations use CompletableFuture pattern</li>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI with proper lifecycle</li>
 * </ul>
 *
 * <h3>Provided Components:</h3>
 * <ul>
 *   <li>DayViewStateManager for comprehensive state management</li>
 *   <li>DayViewDataLoader for async multi-source data loading</li>
 *   <li>DayViewEventOperations for CRUD and interactive operations</li>
 *   <li>DayViewEventsAdapter for unified event display and interaction</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewModule
{

    private static final String TAG = "DayViewModule";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarServiceProvider mCaendarServiceProvider;
    private final LocalEventsService mLocalEventsService;
    private final UserWorkScheduleService mUserWorkScheduleService;
    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== CLEAN ARCHITECTURE COMPONENTS ====================

    private final GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;
    private final LocalEventsUseCases mLocalEventsUseCases;

    // ==================== CACHED INSTANCES ====================

    private DayViewStateManager mStateManager;
    private DayViewDataLoader mDataLoader;
    private DayViewEventOperations mEventOperations;
    private DayViewEventsAdapter mEventsAdapter;

    // ==================== CONFIGURATION ====================

    private QDueUser mQDueUser;
    private LocalDate mTargetDate;
    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates DayViewModule with required dependencies for clean architecture.
     *
     * @param context                 Context for resource access and theming
     * @param calendarServiceProvider CalendarServiceProvider instance for domain services
     * @throws IllegalArgumentException if context is not an Activity or required services unavailable
     */
    public DayViewModule(
            @NonNull Context context,
            @NonNull CalendarServiceProvider calendarServiceProvider
    ) {
        // Validate context for proper theming capabilities
        if (!(context instanceof android.app.Activity)) {
            Log.w( TAG, "Context is not an Activity - some theming features may be limited" );
        }

        this.mContext = context; //context.getApplicationContext();
        this.mCaendarServiceProvider = calendarServiceProvider;

        QDueUserService mQDueUserService = calendarServiceProvider.getQDueUserService();
        this.mQDueUser = mQDueUserService.getPrimaryUser().join().getData();
        if (mQDueUser == null) {
            throw new RuntimeException( "Primary User is null" );
        }

        // Initialize core services
        this.mLocalEventsService = calendarServiceProvider.getLocalEventsService();
        this.mUserWorkScheduleService = calendarServiceProvider.getUserWorkScheduleService();
        this.mWorkScheduleRepository = calendarServiceProvider.getWorkScheduleRepository();

        // Initialize use cases for clean architecture
        this.mGenerateUserScheduleUseCase = calendarServiceProvider.getGenerateUserScheduleUseCase();
        this.mLocalEventsUseCases = calendarServiceProvider.getLocalEventsUseCases();

        Log.d( TAG, "DayViewModule initialized with clean architecture dependencies" );
    }

    // ==================== CONFIGURATION ====================

    /**
     * Configure module for specific date and user context.
     *
     * @param targetDate Target date for day view display
     * @param qDueUser   User context for data filtering (null for default user)
     * @return this module for method chaining
     */
    @NonNull
    public DayViewModule configure(@NonNull LocalDate targetDate, @Nullable QDueUser qDueUser) {
        if (mIsDestroyed) {
            throw new IllegalStateException( "Module has been destroyed" );
        }

        this.mTargetDate = targetDate;
        if (qDueUser != null) {
            this.mQDueUser = qDueUser;
        }

        Log.d( TAG, "DayViewModule configured for date: " + targetDate +
                ", user: " + (qDueUser != null ? qDueUser.getId() : "default") );

        return this;
    }

    // ==================== CORE COMPONENT PROVIDERS ====================

    /**
     * Provides DayViewStateManager instance with comprehensive state management.
     * Manages selected date, events, UI states, and multi-selection for bulk operations.
     *
     * @return State manager instance
     * @throws IllegalStateException if module has been destroyed or not configured
     */
    @NonNull
    public synchronized DayViewStateManager provideStateManager() {
        validateConfiguration();

        if (mStateManager == null) {
            mStateManager = new DayViewStateManager( mContext, mTargetDate );
            Log.d( TAG, "Created DayViewStateManager for date: " + mTargetDate );
        }

        return mStateManager;
    }

    /**
     * Provides DayViewDataLoader instance for async multi-source data loading.
     * Handles loading LocalEvents and WorkScheduleDay data with extensible architecture.
     *
     * @return Data loader instance
     * @throws IllegalStateException if module has been destroyed or not configured
     */
    @NonNull
    public synchronized DayViewDataLoader provideDataLoader() {
        validateConfiguration();

        if (mDataLoader == null) {
            mDataLoader = new DayViewDataLoader(
                    mLocalEventsService,
                    mUserWorkScheduleService,
                    mQDueUser
            );
            Log.d( TAG, "Created DayViewDataLoader with multi-source support" );
        }

        return mDataLoader;
    }

    /**
     * Provides DayViewEventOperations instance for comprehensive event management.
     * Handles CRUD operations, bulk actions, sharing, and clipboard operations.
     *
     * @return Event operations instance
     * @throws IllegalStateException if module has been destroyed or not configured
     */
    @NonNull
    public synchronized DayViewEventOperations provideEventOperations() {
        validateConfiguration();

        if (mEventOperations == null) {
            mEventOperations = new DayViewEventOperations(
                    mContext,
                    mLocalEventsService,
                    mLocalEventsUseCases,
                    provideStateManager()
            );
            Log.d( TAG, "Created DayViewEventOperations with full CRUD support" );
        }

        return mEventOperations;
    }

    /**
     * Provides DayViewEventsAdapter instance for unified event display.
     * Supports LocalEvent, WorkScheduleDay, and extensible event types with interactions.
     *
     * @return Events adapter instance
     * @throws IllegalStateException if module has been destroyed or not configured
     */
    @NonNull
    public synchronized DayViewEventsAdapter provideEventsAdapter( Context context) {
        validateConfiguration();

        // Use the correct context
        if (mEventsAdapter == null) {
            mEventsAdapter = new DayViewEventsAdapter(
                    context,
                    provideStateManager(),
                    provideEventOperations()
            );
            Log.d( TAG, "Created DayViewEventsAdapter with multi-type event support" );
        }

        return mEventsAdapter;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Load all events for the configured date from all available sources.
     *
     * @return CompletableFuture with aggregated events from all sources
     * @throws IllegalStateException if module has been destroyed or not configured
     */
    @NonNull
    public CompletableFuture<Void> loadDayEvents() {
        validateConfiguration();

        return provideDataLoader().loadEventsForDate( mTargetDate )
                .thenAccept( events -> {
                    provideStateManager().updateEvents( events );
                    Log.d( TAG, "Loaded " + events.size() + " events for date: " + mTargetDate );
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error loading events for date: " + mTargetDate, throwable );
                    provideStateManager().setError(
                            "Failed to load events: " + throwable.getMessage() );
                    return null;
                } );
    }

    /**
     * Refresh events data from all sources and update UI.
     *
     * @return CompletableFuture for refresh operation
     */
    @NonNull
    public CompletableFuture<Void> refreshEvents() {
        validateConfiguration();

        provideStateManager().setLoading( true );
        return loadDayEvents()
                .whenComplete( (result, throwable) -> provideStateManager().setLoading( false ) );
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up module resources and cached instances.
     * Should be called when the associated fragment/activity is destroyed.
     */
    public void destroy() {
        Log.d( TAG, "Destroying DayViewModule" );

        mIsDestroyed = true;

        // Clean up adapter
        if (mEventsAdapter != null) {
            mEventsAdapter.cleanup();
            mEventsAdapter = null;
        }

        // Clean up state manager
        if (mStateManager != null) {
            mStateManager.cleanup();
            mStateManager = null;
        }

        // Clean up other components
        mDataLoader = null;
        mEventOperations = null;

        Log.d( TAG, "DayViewModule destroyed" );
    }

    /**
     * Check if module is ready for use.
     *
     * @return true if module is configured and not destroyed
     */
    public boolean isReady() {
        return !mIsDestroyed && mTargetDate != null;
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Validate that module is properly configured before use.
     *
     * @throws IllegalStateException if module is destroyed or not configured
     */
    private void validateConfiguration() {
        if (mIsDestroyed) {
            throw new IllegalStateException( "Module has been destroyed" );
        }
        if (mTargetDate == null) {
            throw new IllegalStateException( "Module not configured - call configure() first" );
        }
    }
}