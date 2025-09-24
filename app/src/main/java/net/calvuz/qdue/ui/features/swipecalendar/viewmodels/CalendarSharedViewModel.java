package net.calvuz.qdue.ui.features.swipecalendar.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CalendarSharedViewModel - MVVM Shared ViewModel for Calendar Features
 *
 * <p>Centralized ViewModel managing shared state between MonthCalendarFragment and
 * DayViewFragment following Google Calendar-like data model with reactive LiveData observables.
 * Provides bidirectional synchronization to ensure consistent state across navigation.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Shared State Management</strong>: Current month, selected date, view mode</li>
 *   <li><strong>Bidirectional Sync</strong>: MonthView ↔ DayView state synchronization</li>
 *   <li><strong>Event Data Coordination</strong>: Centralized event loading and caching</li>
 *   <li><strong>View Mode Support</strong>: MONTH and DAY view coordination</li>
 *   <li><strong>Live Data Integration</strong>: Reactive UI updates with observers</li>
 *   <li><strong>Work Schedule Integration</strong>: Combined events and work schedule data</li>
 * </ul>
 *
 * <h3>State Synchronization:</h3>
 * <ul>
 *   <li><strong>Navigation Sync</strong>: Date changes in DayView update MonthView header</li>
 *   <li><strong>Position Sync</strong>: ViewPager position synchronization on return</li>
 *   <li><strong>Header Updates</strong>: Real-time header text updates based on current date</li>
 *   <li><strong>Month Transitions</strong>: Automatic month navigation when day crosses boundaries</li>
 * </ul>
 *
 * <h3>Dependency Injection Architecture:</h3>
 * <ul>
 *   <li><strong>Service Integration</strong>: LocalEventsService, QDueUserService, UserWorkScheduleService injection</li>
 *   <li><strong>Repository Pattern</strong>: WorkScheduleRepository for schedule data</li>
 *   <li><strong>Use Case Layer</strong>: GenerateUserScheduleUseCase for business logic</li>
 *   <li><strong>User Management</strong>: Dynamic QDueUser loading and caching</li>
 *   <li><strong>Clean Architecture</strong>: Separation of concerns with reactive patterns</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - MVVM Foundation with State Sync
 * @since MVVM Migration Phase
 */
public class CalendarSharedViewModel extends ViewModel
{

    private static final String TAG = "CalendarSharedViewModel";

    // ==================== VIEW MODE ENUM ====================

    /**
     * Calendar view mode enumeration for fragment coordination
     */
    public enum ViewMode
    {
        MONTH,  // Month view with month grid
        DAY     // Day view with hourly timeline
    }

    /**
     * Loading state enumeration for UI state management
     */
    public enum LoadingState
    {
        IDLE,       // No operations in progress
        LOADING,    // Data loading in progress
        SUCCESS,    // Data loaded successfully
        ERROR       // Error occurred during loading
    }

    /**
     * Navigation event for fragment coordination
     */
    public static class NavigationEvent
    {
        public final ViewMode targetViewMode;
        public final LocalDate targetDate;
        public final boolean shouldAnimate;

        public NavigationEvent(ViewMode targetViewMode, LocalDate targetDate, boolean shouldAnimate) {
            this.targetViewMode = targetViewMode;
            this.targetDate = targetDate;
            this.shouldAnimate = shouldAnimate;
        }
    }

    // ==================== DEPENDENCIES ====================

    private final LocalEventsService mEventsService;
    private final QDueUserService mUserService;
    private final UserWorkScheduleService mUserWorkScheduleService;

    // ==================== CORE STATE LIVE DATA ====================

    // Current state
    private final MutableLiveData<YearMonth> mCurrentMonth = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> mSelectedDate = new MutableLiveData<>();
    private final MutableLiveData<ViewMode> mViewMode = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCurrentPosition = new MutableLiveData<>();

    // Loading and error states
    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();
    private final MutableLiveData<String> mErrorMessage = new MutableLiveData<>();

    // Navigation events
    private final MutableLiveData<NavigationEvent> mNavigationEvent = new MutableLiveData<>();

    // ==================== DATA LIVE DATA ====================

    // Month data - maps LocalDate to event/schedule lists
    private final MutableLiveData<Map<LocalDate, List<LocalEvent>>> mMonthEvents = new MutableLiveData<>();
    private final MutableLiveData<Map<LocalDate, List<WorkScheduleDay>>> mMonthWorkSchedule = new MutableLiveData<>();

    // Selected day data - derived from month data and selected date
    private final MediatorLiveData<List<LocalEvent>> mSelectedDayEvents = new MediatorLiveData<>();
    private final MediatorLiveData<List<WorkScheduleDay>> mSelectedDayWorkSchedule = new MediatorLiveData<>();

    // User data
    private final MutableLiveData<QDueUser> mCurrentUser = new MutableLiveData<>();

    // ==================== CACHING ====================

    // Cached data to reduce repeated queries
    private final Map<YearMonth, Map<LocalDate, List<LocalEvent>>> mEventsCache = new ConcurrentHashMap<>();
    private final Map<YearMonth, Map<LocalDate, List<WorkScheduleDay>>> mWorkScheduleCache = new ConcurrentHashMap<>();

    // User cache
    private QDueUser mQDueUser;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create CalendarSharedViewModel with dependency injection.
     *
     * @param eventsService           Service for events data operations
     * @param userService             Service for user data operations
     * @param userWorkScheduleService Service for work schedule operations
     * @param workScheduleRepository  Repository for work schedule operations
     * @param userScheduleUseCase     Use case for work schedule business logic
     * @param localeManager           Locale manager for internationalization
     */
    public CalendarSharedViewModel(
            @NonNull LocalEventsService eventsService,
            @NonNull QDueUserService userService,
            @NonNull UserWorkScheduleService userWorkScheduleService,
            @NonNull WorkScheduleRepository workScheduleRepository,
            @NonNull GenerateUserScheduleUseCase userScheduleUseCase,
            @NonNull LocaleManager localeManager
    ) {

        this.mEventsService = eventsService;
        this.mUserService = userService;
        this.mUserWorkScheduleService = userWorkScheduleService;

        // Initialize default state
        initializeDefaultState();

        // Setup derived live data observers
        setupDerivedLiveData();

        // Load current user
        loadCurrentUser();

        Log.d( TAG, "CalendarSharedViewModel initialized with MVVM architecture and state sync" );
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize default state values for calendar display.
     */
    private void initializeDefaultState() {
        // Set initial state
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        // ✅ FIXED: Calculate correct position for current month
        int currentPosition = SwipeCalendarStateManager.getPositionForMonth( currentMonth);

        mCurrentMonth.setValue( currentMonth );
        mSelectedDate.setValue( today );
        mViewMode.setValue( ViewMode.MONTH );
        mLoadingState.setValue( LoadingState.IDLE );
        mCurrentPosition.setValue(currentPosition); // ✅ CORRECTED: Use calculated position

        // Initialize empty data containers
        mMonthEvents.setValue( new HashMap<>() );
        mMonthWorkSchedule.setValue( new HashMap<>() );

        Log.d( TAG, "Default state initialized - Month: " + currentMonth + ", Selected: " + today );
    }

    /**
     * Setup derived LiveData that depends on multiple sources.
     */
    private void setupDerivedLiveData() {
        // Selected day events - derived from month events and selected date
        mSelectedDayEvents.addSource( mMonthEvents, this::updateSelectedDayEvents );
        mSelectedDayEvents.addSource( mSelectedDate,
                                      date -> updateSelectedDayEvents( mMonthEvents.getValue() ) );

        // Selected day work schedule - derived from month schedule and selected date
        mSelectedDayWorkSchedule.addSource( mMonthWorkSchedule,
                                            this::updateSelectedDayWorkSchedule );
        mSelectedDayWorkSchedule.addSource( mSelectedDate, date -> updateSelectedDayWorkSchedule(
                mMonthWorkSchedule.getValue() ) );

        Log.d( TAG, "Derived LiveData observers configured" );
    }

    /**
     * Load current user from QDueUserService.
     */
    private void loadCurrentUser() {
        mUserService.getPrimaryUser()
                .thenAccept( result -> {
                    if (result.isSuccess()) {
                        mQDueUser = result.getData();
                        mCurrentUser.postValue( mQDueUser );
                        Log.d( TAG, "Current user loaded: " + mQDueUser.getNickname() );
                    } else {
                        Log.w( TAG, "Failed to load current user: " + result.getErrorMessage() );
                        mErrorMessage.postValue( "Failed to load user data" );
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error loading current user", throwable );
                    mErrorMessage.postValue( "Error loading user data: " + throwable.getMessage() );
                    return null;
                } );
    }

    // ==================== CORE STATE ACCESSORS ====================

    /**
     * Get current month LiveData.
     */
    @NonNull
    public LiveData<YearMonth> getCurrentMonth() {
        return mCurrentMonth;
    }

    /**
     * Get selected date LiveData.
     */
    @NonNull
    public LiveData<LocalDate> getSelectedDate() {
        return mSelectedDate;
    }

    /**
     * Get current view mode LiveData.
     */
    @NonNull
    public LiveData<ViewMode> getViewMode() {
        return mViewMode;
    }

    /**
     * Get current position LiveData for ViewPager synchronization.
     */
    @NonNull
    public LiveData<Integer> getCurrentPosition() {
        return mCurrentPosition;
    }

    /**
     * Get loading state LiveData.
     */
    @NonNull
    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    /**
     * Get error message LiveData.
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Get navigation event LiveData.
     */
    @NonNull
    public LiveData<NavigationEvent> getNavigationEvent() {
        return mNavigationEvent;
    }

    // ==================== DATA ACCESSORS ====================

    /**
     * Get month events LiveData.
     */
    @NonNull
    public LiveData<Map<LocalDate, List<LocalEvent>>> getMonthEvents() {
        return mMonthEvents;
    }

    /**
     * Get month work schedule LiveData.
     */
    @NonNull
    public LiveData<Map<LocalDate, List<WorkScheduleDay>>> getMonthWorkSchedule() {
        return mMonthWorkSchedule;
    }

    /**
     * Get selected day events LiveData.
     */
    @NonNull
    public LiveData<List<LocalEvent>> getSelectedDayEvents() {
        return mSelectedDayEvents;
    }

    /**
     * Get selected day work schedule LiveData.
     */
    @NonNull
    public LiveData<List<WorkScheduleDay>> getSelectedDayWorkSchedule() {
        return mSelectedDayWorkSchedule;
    }

    /**
     * Get current user LiveData.
     */
    @NonNull
    public LiveData<QDueUser> getCurrentUser() {
        return mCurrentUser;
    }

    // ==================== STATE MUTATION METHODS ====================

    /**
     * Navigate to specific month and update state.
     * Call this from MonthCalendarFragment when user swipes.
     *
     * @param month    Target month
     * @param position ViewPager position
     */
    public void navigateToMonth(@NonNull YearMonth month, int position) {
        YearMonth currentMonth = mCurrentMonth.getValue();

        // ✅ VALIDATION: Ensure position matches month
        int expectedPosition = SwipeCalendarStateManager.getPositionForMonth(month);
        if (position != expectedPosition) {
            Log.w(TAG, "Position mismatch for month " + month + ": provided=" + position + ", expected=" + expectedPosition + ". Using expected.");
            position = expectedPosition;
        }

       if (currentMonth == null || !currentMonth.equals( month )) {
            mCurrentMonth.setValue( month );
            mCurrentPosition.setValue( position );

            // If selected date is not in the new month, update it to first day of month
            LocalDate selectedDate = mSelectedDate.getValue();
            if (selectedDate == null || !YearMonth.from( selectedDate ).equals( month )) {
                mSelectedDate.setValue( month.atDay( 1 ) );
            }

            Log.d( TAG, "Navigated to month: " + month + " at position: " + position );
            loadMonthData( month );
        }
    }

    /**
     * Navigate to specific date and update state.
     * Call this from DayViewFragment when user changes date.
     *
     * @param date Target date
     */
    public void navigateToDate(@NonNull LocalDate date) {
        LocalDate currentSelectedDate = mSelectedDate.getValue();
        if (currentSelectedDate == null || !currentSelectedDate.equals( date )) {
            YearMonth targetMonth = YearMonth.from( date );
            YearMonth currentMonth = mCurrentMonth.getValue();

            // Update selected date
            mSelectedDate.setValue( date );

            // Check if we need to change months
            if (currentMonth == null || !currentMonth.equals( targetMonth )) {
                mCurrentMonth.setValue( targetMonth );
                loadMonthData( targetMonth );

                // Trigger navigation event for MonthCalendarFragment to update ViewPager
                mNavigationEvent.setValue( new NavigationEvent( ViewMode.MONTH, date, false ) );

                Log.d( TAG, "Date navigation triggered month change: " + targetMonth );
            }

            Log.d( TAG, "Navigated to date: " + date );
        }
    }

    /**
     * Set current view mode.
     *
     * @param viewMode Target view mode
     */
    public void setViewMode(@NonNull ViewMode viewMode) {
        ViewMode currentMode = mViewMode.getValue();
        if (currentMode == null || !currentMode.equals( viewMode )) {
            mViewMode.setValue( viewMode );
            Log.d( TAG, "View mode changed to: " + viewMode );
        }
    }

    /**
     * Update current position for ViewPager synchronization.
     *
     * @param position Current ViewPager position
     */
    public void updatePosition(int position) {
        Integer currentPosition = mCurrentPosition.getValue();
        if (currentPosition == null || currentPosition != position) {
            mCurrentPosition.setValue( position );
            Log.v( TAG, "Position updated to: " + position );
        }
    }

    // ==================== DATA LOADING ====================

    /**
     * Load data for specific month.
     *
     * @param month Target month
     */
    public void loadMonthData(@NonNull YearMonth month) {
        Log.d( TAG, "Loading data for month: " + month );

        // Check cache first
        if (mEventsCache.containsKey( month ) && mWorkScheduleCache.containsKey( month )) {
            Map<LocalDate, List<LocalEvent>> cachedEvents = mEventsCache.get( month );
            Map<LocalDate, List<WorkScheduleDay>> cachedSchedule = mWorkScheduleCache.get( month );

            mMonthEvents.setValue( cachedEvents );
            mMonthWorkSchedule.setValue( cachedSchedule );

            Log.d( TAG, "Month data loaded from cache: " + month );
            return;
        }

        // Load from services
        mLoadingState.setValue( LoadingState.LOADING );

        LocalDate startDate = month.atDay( 1 );
        LocalDate endDate = month.atEndOfMonth();

        CompletableFuture<Map<LocalDate, List<LocalEvent>>> eventsFuture = loadEventsForDateRange(
                startDate, endDate );
        CompletableFuture<Map<LocalDate, List<WorkScheduleDay>>> scheduleFuture = loadWorkScheduleForDateRange(
                startDate, endDate );

        CompletableFuture.allOf( eventsFuture, scheduleFuture )
                .thenRun( () -> {
                    try {
                        Map<LocalDate, List<LocalEvent>> events = eventsFuture.get();
                        Map<LocalDate, List<WorkScheduleDay>> schedule = scheduleFuture.get();

                        // Cache results
                        mEventsCache.put( month, events );
                        mWorkScheduleCache.put( month, schedule );

                        // Update LiveData
                        mMonthEvents.postValue( events );
                        mMonthWorkSchedule.postValue( schedule );

                        mLoadingState.postValue( LoadingState.SUCCESS );
                        Log.d( TAG, "Month data loaded successfully: " + month );
                    } catch (Exception e) {
                        Log.e( TAG, "Error processing month data", e );
                        mLoadingState.postValue( LoadingState.ERROR );
                        mErrorMessage.postValue( "Failed to load month data: " + e.getMessage() );
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error loading month data", throwable );
                    mLoadingState.postValue( LoadingState.ERROR );
                    mErrorMessage.postValue(
                            "Failed to load month data: " + throwable.getMessage() );
                    return null;
                } );
    }

    /**
     * Refresh data for current month.
     */
    public void refreshCurrentMonth() {
        YearMonth currentMonth = mCurrentMonth.getValue();
        if (currentMonth != null) {
            // Clear cache for current month
            mEventsCache.remove( currentMonth );
            mWorkScheduleCache.remove( currentMonth );

            // Reload data
            loadMonthData( currentMonth );
            Log.d( TAG, "Refreshing current month data: " + currentMonth );
        }
    }

    // ==================== PRIVATE DATA LOADING METHODS ====================

    /**
     * Load events for date range using LocalEventsService.
     */
    private CompletableFuture<Map<LocalDate, List<LocalEvent>>> loadEventsForDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (mQDueUser == null) {
            Log.w( TAG, "Cannot load events - QDueUser is null" );
            return CompletableFuture.completedFuture( new HashMap<>() );
        }

        //return mEventsService.getAllEventsForUser(mQDueUser.getId())
        return mEventsService.getEventsForDateRange( startDate.atStartOfDay(),
                                                     endDate.atTime( 23, 59, 59 ) )
                .thenApply( result -> {
                    if (result.isSuccess()) {
//                        List<LocalEvent> allEvents = result.getData();
                        List<LocalEvent> filteredEvents = result.getData(); //filterEventsByDateRange(allEvents, startDate, endDate);

                        Map<LocalDate, List<LocalEvent>> eventsByDate = new HashMap<>();

                        // Group events by date
                        if (filteredEvents != null && !filteredEvents.isEmpty()) {
                            for (LocalEvent event : filteredEvents) {
                                LocalDate eventDate = event.getStartTime().toLocalDate();
                                if (eventDate != null) {
                                    eventsByDate.computeIfAbsent( eventDate,
                                                                  k -> new ArrayList<>() ).add(
                                            event );
                                }
                            }
                        }

                        Log.v( TAG,
                               "Events loaded for user " + mQDueUser.getId() + " range " + startDate + " to " + endDate + ": " + filteredEvents.size() + " events" );
                        return eventsByDate;
                    } else {
                        Log.w( TAG, "Failed to load events: " + result.getErrorMessage() );
                        return new HashMap<LocalDate, List<LocalEvent>>();
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error loading events for date range", throwable );
                    return new HashMap<>();
                } );
    }

    /**
     * Filter events by date range.
     */
    private List<LocalEvent> filterEventsByDateRange(List<LocalEvent> allEvents, LocalDate startDate, LocalDate endDate) {
        List<LocalEvent> filteredEvents = new ArrayList<>();

        for (LocalEvent event : allEvents) {
            LocalDate eventDate = event.getStartTime().toLocalDate();
            if (eventDate != null &&
                    !eventDate.isBefore( startDate ) &&
                    !eventDate.isAfter( endDate )) {
                filteredEvents.add( event );
            }
        }

        Log.v( TAG,
               "Filtered " + allEvents.size() + " events to " + filteredEvents.size() + " events for range " + startDate + " to " + endDate );
        return filteredEvents;
    }

    /**
     * Load work schedule for date range using UserWorkScheduleService.
     */
    private CompletableFuture<Map<LocalDate, List<WorkScheduleDay>>> loadWorkScheduleForDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (mQDueUser == null) {
            Log.w( TAG, "Cannot load work schedule - QDueUser is null" );
            return CompletableFuture.completedFuture( new HashMap<>() );
        }

        return mUserWorkScheduleService.generateWorkScheduleForUser( mQDueUser.getId(), startDate,
                                                                     endDate )
                .thenApply( result -> {
                    if (result.isSuccess()) {

                        Map<LocalDate, WorkScheduleDay> sched = result.getData();

//                        List<WorkScheduleDay> schedules = result.getData();

                        Map<LocalDate, List<WorkScheduleDay>> schedulesByDate = new HashMap<>();

                        if (sched != null) {
                            List<WorkScheduleDay> schedules = new ArrayList<>( sched.values() );

                            // Group schedules by date
                            if (!schedules.isEmpty()) {
                                for (WorkScheduleDay schedule : schedules) {
                                    LocalDate scheduleDate = schedule.getDate();
                                    schedulesByDate.computeIfAbsent( scheduleDate,
                                                                     k -> new ArrayList<>() ).add(
                                            schedule );
                                }
                            }
                        }

                        Log.v( TAG,
                               "Work schedules loaded for user " + mQDueUser.getId() + " range " + startDate + " to " + endDate + ": " + sched.size() + " schedules" );
                        return schedulesByDate;
                    } else {
                        Log.w( TAG, "Failed to load work schedules: " + result.getErrorMessage() );
                        return new HashMap<LocalDate, List<WorkScheduleDay>>();
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error loading work schedules for date range", throwable );
                    return new HashMap<>();
                } );
    }

    // ==================== DERIVED DATA UPDATE METHODS ====================

    /**
     * Update selected day events based on current events and selected date.
     */
    private void updateSelectedDayEvents(@Nullable Map<LocalDate, List<LocalEvent>> monthEvents) {
        LocalDate selectedDate = mSelectedDate.getValue();
        if (selectedDate != null && monthEvents != null) {
            List<LocalEvent> dayEvents = monthEvents.getOrDefault( selectedDate,
                                                                   new ArrayList<>() );
            mSelectedDayEvents.setValue( dayEvents );
            Log.v( TAG,
                   "Selected day events updated: " + dayEvents.size() + " events for " + selectedDate );
        }
    }

    /**
     * Update selected day work schedule based on current schedule and selected date.
     */
    private void updateSelectedDayWorkSchedule(@Nullable Map<LocalDate, List<WorkScheduleDay>> monthSchedule) {
        LocalDate selectedDate = mSelectedDate.getValue();
        if (selectedDate != null && monthSchedule != null) {
            List<WorkScheduleDay> daySchedule = monthSchedule.getOrDefault( selectedDate,
                                                                            new ArrayList<>() );
            mSelectedDayWorkSchedule.setValue( daySchedule );
            Log.v( TAG,
                   "Selected day work schedule updated: " + daySchedule.size() + " schedules for " + selectedDate );
        }
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onCleared() {
        super.onCleared();

        // Clear caches to free memory
        mEventsCache.clear();
        mWorkScheduleCache.clear();

        Log.d( TAG, "CalendarSharedViewModel cleared and caches freed" );
    }
}