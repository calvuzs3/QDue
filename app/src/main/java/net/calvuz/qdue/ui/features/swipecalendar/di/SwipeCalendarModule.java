package net.calvuz.qdue.ui.features.swipecalendar.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.WorkScheduleService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.features.swipecalendar.adapters.MonthPagerAdapter;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * SwipeCalendarModule - Dependency injection module for SwipeCalendar feature.
 *
 * <p>Provides centralized dependency management for all SwipeCalendar-related components
 * following the established DI patterns in the application. Handles creation and
 * lifecycle management of adapters, state managers, and data loaders.</p>
 *
 * <h3>Module Features:</h3>
 * <ul>
 *   <li><strong>Lazy Initialization</strong>: Components created on-demand for performance</li>
 *   <li><strong>Lifecycle Management</strong>: Proper cleanup and resource disposal</li>
 *   <li><strong>Thread Safety</strong>: Safe component creation and access</li>
 *   <li><strong>Service Integration</strong>: Seamless integration with existing service layer</li>
 * </ul>
 *
 * <h3>Provided Components:</h3>
 * <ul>
 *   <li>MonthPagerAdapter with integrated data loading</li>
 *   <li>SwipeCalendarStateManager for position persistence</li>
 *   <li>DataLoader implementation for events and work schedule</li>
 *   <li>Background ExecutorService for data loading operations</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class SwipeCalendarModule {

    private static final String TAG = "SwipeCalendarModule";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final WorkScheduleService mWorkScheduleService;

    // ==================== CACHED INSTANCES ====================

    private SwipeCalendarStateManager mStateManager;
    private MonthPagerAdapter mPagerAdapter;
    private DataLoaderImpl mDataLoader;
    private ExecutorService mBackgroundExecutor;

    // Configuration
    private Long mCurrentUserId;
    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates SwipeCalendarModule with required dependencies.
     *
     * @param context             Application context
     * @param eventsService       Service for events data operations
     * @param userService         Service for user data operations
     * @param workScheduleService Service for work schedule operations
     */
    public SwipeCalendarModule(@NonNull Context context,
                               @NonNull EventsService eventsService,
                               @NonNull UserService userService,
                               @NonNull WorkScheduleService workScheduleService) {
        this.mContext = context.getApplicationContext();
        this.mEventsService = eventsService;
        this.mUserService = userService;
        this.mWorkScheduleService = workScheduleService;

        Log.d( TAG, "SwipeCalendarModule created" );
    }

    // ==================== COMPONENT PROVIDERS ====================

    /**
     * Provides SwipeCalendarStateManager instance.
     * Manages calendar position state persistence.
     *
     * @return State manager instance
     */
    @NonNull
    public synchronized SwipeCalendarStateManager provideStateManager() {
        if ( mIsDestroyed ) {
            throw new IllegalStateException( "Module has been destroyed" );
        }

        if ( mStateManager == null ) {
            mStateManager = new SwipeCalendarStateManager( mContext );
            Log.d( TAG, "Created SwipeCalendarStateManager" );
        }

        return mStateManager;
    }

    /**
     * Provides MonthPagerAdapter instance.
     * Manages ViewPager2 month navigation with data integration.
     *
     * @return Pager adapter instance
     */
    @NonNull
    public synchronized MonthPagerAdapter providePagerAdapter() {
        if ( mIsDestroyed ) {
            throw new IllegalStateException( "Module has been destroyed" );
        }

        if ( mPagerAdapter == null ) {
            DataLoaderImpl dataLoader = provideDataLoader();
            mPagerAdapter = new MonthPagerAdapter( mContext, dataLoader );
            Log.d( TAG, "Created MonthPagerAdapter" );
        }

        return mPagerAdapter;
    }

    /**
     * Provides DataLoader implementation.
     * Handles loading of events and work schedule data.
     *
     * @return Data loader instance
     */
    @NonNull
    private synchronized DataLoaderImpl provideDataLoader() {
        if ( mDataLoader == null ) {
            ExecutorService executor = provideBackgroundExecutor();
            mDataLoader = new DataLoaderImpl( executor );
            Log.d( TAG, "Created DataLoader" );
        }

        return mDataLoader;
    }

    /**
     * Provides background ExecutorService for data operations.
     *
     * @return Background executor service
     */
    @NonNull
    private synchronized ExecutorService provideBackgroundExecutor() {
        if ( mBackgroundExecutor == null ) {
            mBackgroundExecutor = Executors.newFixedThreadPool( 2 );
            Log.d( TAG, "Created background ExecutorService" );
        }

        return mBackgroundExecutor;
    }

    // ==================== CONFIGURATION ====================

    /**
     * Set current user ID for data operations.
     *
     * @param userId User ID, or null for default user
     */
    public synchronized void setCurrentUserId(@Nullable Long userId) {
        this.mCurrentUserId = userId;

        // Update data loader if it exists
        if ( mDataLoader != null ) {
            mDataLoader.setCurrentUserId( userId );
        }

        Log.d( TAG, "Current user ID set to: " + userId );
    }

    /**
     * Check if all dependencies are ready.
     *
     * @return true if module is ready for use
     */
    public boolean areDependenciesReady() {
        return !mIsDestroyed &&
                mEventsService != null &&
                mUserService != null &&
                mWorkScheduleService != null;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Called when hosting fragment goes to background.
     * Marks state as inactive to trigger proper restoration logic.
     */
    public void onFragmentPause() {
        if ( mStateManager != null ) {
            mStateManager.markSessionInactive();
        }
        Log.d( TAG, "Fragment paused, session marked inactive" );
    }

    /**
     * Called when hosting fragment is destroyed.
     * Cleanup all resources and cached instances.
     */
    public synchronized void onDestroy() {
        if ( mIsDestroyed ) {
            return;
        }

        Log.d( TAG, "Destroying SwipeCalendarModule" );

        // Cleanup pager adapter
        if ( mPagerAdapter != null ) {
            mPagerAdapter.cleanup();
            mPagerAdapter = null;
        }

        // Cleanup data loader
        if ( mDataLoader != null ) {
            mDataLoader.cleanup();
            mDataLoader = null;
        }

        // Shutdown background executor
        if ( mBackgroundExecutor != null ) {
            mBackgroundExecutor.shutdown();
            mBackgroundExecutor = null;
        }

        // Clear state manager reference
        mStateManager = null;

        mIsDestroyed = true;
        Log.d( TAG, "SwipeCalendarModule destroyed" );
    }

    // ==================== DATA LOADER IMPLEMENTATION ====================

    /**
     * Corrected DataLoaderImpl class with proper async handling
     * Fixes the OperationResult<List<LocalEvent>> to Map<LocalDate, List<LocalEvent>> conversion
     */
    private class DataLoaderImpl implements MonthPagerAdapter.DataLoader {

        private final ExecutorService mExecutor;
        private Long mUserId;

        public DataLoaderImpl(@NonNull ExecutorService executor) {
            this.mExecutor = executor;
            this.mUserId = mCurrentUserId;
        }

        @Override
        public void loadEventsForMonth(@NonNull YearMonth month,
                                       @NonNull MonthPagerAdapter.DataCallback<Map<LocalDate, List<LocalEvent>>> callback) {
            mExecutor.execute( () -> {
                try {
                    // Calculate date range for the month
                    LocalDate startDate = month.atDay( 1 );
                    LocalDate endDate = month.atEndOfMonth();

                    // CORRECTION: Handle CompletableFuture<OperationResult<List<LocalEvent>>>
                    mEventsService.getEventsForDateRange( startDate, endDate )
                            .thenAccept( operationResult -> {
                                if ( operationResult.isSuccess() && operationResult.getData() != null ) {
                                    try {
                                        // Convert List<LocalEvent> to Map<LocalDate, List<LocalEvent>>
                                        Map<LocalDate, List<LocalEvent>> eventsMap = convertEventsToMap( operationResult.getData() );

                                        callback.onSuccess( eventsMap );
                                        Log.v( TAG, "Loaded events for month: " + month + ", count: " + eventsMap.size() );

                                    } catch (Exception e) {
                                        Log.e( TAG, "Failed to convert events to map for month: " + month, e );
                                        callback.onError( e );
                                    }
                                } else {
                                    Exception error = new RuntimeException( "Failed to load events: " + operationResult.getErrorMessage() );
                                    Log.e( TAG, "Service returned error for month: " + month + " - " + operationResult.getErrorMessage() );
                                    callback.onError( error );
                                }
                            } )
                            .exceptionally( throwable -> {
                                Log.e( TAG, "Exception in async events loading for month: " + month, throwable );
                                callback.onError( new RuntimeException( "Async loading failed", throwable ) );
                                return null;
                            } );

                } catch (Exception e) {
                    Log.e( TAG, "Failed to initiate events loading for month: " + month, e );
                    callback.onError( e );
                }
            } );
        }

        @Override
        public void loadWorkScheduleForMonth(@NonNull YearMonth month,
                                             @NonNull MonthPagerAdapter.DataCallback<Map<LocalDate, Day>> callback) {
            mExecutor.execute( () -> {
                try {
                    // Calculate date range for the month
                    LocalDate startDate = month.atDay( 1 );
                    LocalDate endDate = month.atEndOfMonth();

                    // Load work schedule from service (assuming this method is synchronous)
                    Map<LocalDate, Day> workScheduleMap = mWorkScheduleService.getWorkScheduleForDateRange(
                            startDate, endDate, mUserId );

                    callback.onSuccess( workScheduleMap );
                    Log.v( TAG, "Loaded work schedule for month: " + month + ", count: " + workScheduleMap.size() );

                } catch (Exception e) {
                    Log.e( TAG, "Failed to load work schedule for month: " + month, e );
                    callback.onError( e );
                }
            } );
        }


        /**
         * Convert List<LocalEvent> to Map<LocalDate, List<LocalEvent>> grouped by event date
         *
         * @param events List of events to convert
         * @return Map with dates as keys and lists of events as values
         */
        private Map<LocalDate, List<LocalEvent>> convertEventsToMap(@NonNull List<LocalEvent> events) {
            Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();

            for (LocalEvent event : events) {
                if ( event != null ) {
                    LocalDate eventDate = getEventDate( event );

                    // Add event to the map
                    eventsMap.computeIfAbsent( eventDate, k -> new ArrayList<>() ).add( event );
                }
            }

            return eventsMap;
        }

        /**
         * Helper method per ottenere la data di un evento gestendo diversi tipi
         */
        private LocalDate getEventDate(@NonNull LocalEvent event) {
            try {
                // Priorità: data specifica per eventi all-day
                if ( event.isAllDay() && event.getDate() != null ) {
                    return event.getDate();
                }

                // Altrimenti usa startTime
                if ( event.getStartTime() != null ) {
                    return event.getStartTime().toLocalDate();
                }

                // Fallback: se l'evento ha endTime ma non startTime
                if ( event.getEndTime() != null ) {
                    return event.getEndTime().toLocalDate();
                }

                // Ultimo fallback (non dovrebbe mai succedere)
                Log.w( "SwipeCalendarModule", "Event has no valid date, using today: " + event.getTitle() );
                return LocalDate.now();

            } catch (Exception e) {
                Log.e( "SwipeCalendarModule", "Error getting event date for: " + event.getTitle(), e );
                return LocalDate.now();
            }
        }

        /**
         * Update user ID for data operations.
         */
        public void setCurrentUserId(@Nullable Long userId) {
            this.mUserId = userId;
        }

        /**
         * Cleanup data loader resources.
         */
        public void cleanup() {
            // DataLoader doesn't own the executor, so no cleanup needed
            Log.d( TAG, "DataLoader cleaned up" );
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get module information for debugging.
     *
     * @return Human readable module status
     */
    @NonNull
    public String getModuleInfo() {
        StringBuilder info = new StringBuilder();
        info.append( "SwipeCalendarModule Status:\n" );
        info.append( "- Destroyed: " ).append( mIsDestroyed ).append( "\n" );
        info.append( "- Dependencies Ready: " ).append( areDependenciesReady() ).append( "\n" );
        info.append( "- Current User ID: " ).append( mCurrentUserId ).append( "\n" );
        info.append( "- State Manager: " ).append( mStateManager != null ? "Created" : "Not Created" ).append( "\n" );
        info.append( "- Pager Adapter: " ).append( mPagerAdapter != null ? "Created" : "Not Created" ).append( "\n" );
        info.append( "- Background Executor: " ).append( mBackgroundExecutor != null ? "Created" : "Not Created" );

        return info.toString();
    }
}