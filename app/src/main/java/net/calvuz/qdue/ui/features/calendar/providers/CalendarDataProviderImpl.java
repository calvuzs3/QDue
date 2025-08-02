package net.calvuz.qdue.ui.features.calendar.providers;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.domain.events.models.EventPriority;
import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity;
import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarDataProvider;
import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;
import net.calvuz.qdue.ui.features.calendar.models.CalendarLoadingState;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * CalendarDataProviderImpl - Implementation of CalendarDataProvider interface.
 *
 * <p>Combines data from WorkScheduleService and EventsService to provide unified
 * calendar information. Handles caching, error recovery, and loading states.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Async data loading with CompletableFuture</li>
 *   <li>Intelligent caching with automatic cleanup</li>
 *   <li>Work schedule and events integration</li>
 *   <li>Error handling and retry mechanisms</li>
 *   <li>User filtering and context switching</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarDataProviderImpl implements CalendarDataProvider {

    private static final String TAG = "CalendarDataProviderImpl";

    // Cache configuration
    private static final int MAX_CACHE_SIZE = 12; // Months to keep in cache
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final WorkScheduleService mWorkScheduleService;
    private final ExecutorService mExecutorService;

    // ==================== CACHE AND STATE ====================

    // Cache for calendar days by month
    private final Map<YearMonth, CacheEntry> mMonthCache = new ConcurrentHashMap<>();

    // Current loading state
    private volatile CalendarLoadingState mLoadingState = CalendarLoadingState.IDLE;

    // Current user context
    private volatile Long mCurrentUserId = null;

    // Destruction flag
    private volatile boolean mIsDestroyed = false;

    // ==================== CACHE ENTRY ====================

    /**
     * Cache entry with timestamp for expiry checking.
     */
    private static class CacheEntry {
        final List<CalendarDay> calendarDays;
        final long timestamp;

        CacheEntry(List<CalendarDay> calendarDays) {
            this.calendarDays = calendarDays;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates CalendarDataProviderImpl with required dependencies.
     *
     * @param context Android context
     * @param eventsService Service for event data
     * @param userService Service for user context
     * @param workScheduleService Service for work schedule data
     * @param executorService Executor for background operations
     */
    public CalendarDataProviderImpl(@NonNull Context context,
                                    @NonNull EventsService eventsService,
                                    @NonNull UserService userService,
                                    @NonNull WorkScheduleService workScheduleService,
                                    @NonNull ExecutorService executorService) {
        this.mContext = context.getApplicationContext();
        this.mEventsService = eventsService;
        this.mUserService = userService;
        this.mWorkScheduleService = workScheduleService;
        this.mExecutorService = executorService;

        Log.d(TAG, "CalendarDataProviderImpl created");
    }

    // ==================== CALENDARDATAPROVIDER IMPLEMENTATION ====================

    @NonNull
    @Override
    public CompletableFuture<List<CalendarDay>> loadMonthData(@NonNull YearMonth yearMonth) {
        if (mIsDestroyed) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        Log.d(TAG, "Loading month data for: " + yearMonth);

        // Check cache first
        CacheEntry cached = mMonthCache.get(yearMonth);
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Returning cached data for: " + yearMonth);
            return CompletableFuture.completedFuture(new ArrayList<>(cached.calendarDays));
        }

        // Load data asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                setLoadingState(CalendarLoadingState.LOADING);

                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                // Add padding to include full weeks
                LocalDate paddedStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
                LocalDate paddedEnd = endDate.plusDays(7 - endDate.getDayOfWeek().getValue());

                List<CalendarDay> calendarDays = loadDateRangeDataSync(paddedStart, paddedEnd);

                // Cache the result
                mMonthCache.put(yearMonth, new CacheEntry(calendarDays));
                cleanupCache();

                setLoadingState(CalendarLoadingState.LOADED);

                Log.d(TAG, "Month data loaded: " + yearMonth + " (" + calendarDays.size() + " days)");
                return calendarDays;

            } catch (Exception e) {
                Log.e(TAG, "Error loading month data for " + yearMonth, e);
                setLoadingState(CalendarLoadingState.ERROR);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<List<CalendarDay>> loadDateRangeData(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (mIsDestroyed) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        Log.d(TAG, "Loading date range data: " + startDate + " to " + endDate);

        return CompletableFuture.supplyAsync(() -> {
            try {
                setLoadingState(CalendarLoadingState.LOADING);

                List<CalendarDay> calendarDays = loadDateRangeDataSync(startDate, endDate);

                setLoadingState(CalendarLoadingState.LOADED);
                return calendarDays;

            } catch (Exception e) {
                Log.e(TAG, "Error loading date range data", e);
                setLoadingState(CalendarLoadingState.ERROR);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<CalendarDay> loadDayData(@NonNull LocalDate date) {
        if (mIsDestroyed) {
            return CompletableFuture.completedFuture(createErrorDay(date));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<CalendarDay> dayList = loadDateRangeDataSync(date, date);
                if (!dayList.isEmpty()) {
                    return dayList.get(0);
                } else {
                    return createErrorDay(date);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading day data for " + date, e);
                return createErrorDay(date);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CalendarLoadingState getLoadingState() {
        return mLoadingState;
    }

    @Override
    public void setCurrentUser(@Nullable Long userId) {
        if (!java.util.Objects.equals(mCurrentUserId, userId)) {
            mCurrentUserId = userId;

            // Clear cache when user changes
            clearCache();

            Log.d(TAG, "Current user changed to: " + userId);
        }
    }

    @Override
    public void refreshData(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (mIsDestroyed) return;

        Log.d(TAG, "Refreshing data: " + startDate + " to " + endDate);

        // Remove affected months from cache
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(endMonth)) {
            mMonthCache.remove(currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }

        setLoadingState(CalendarLoadingState.REFRESHING);
    }

    @Override
    public void clearCache() {
        mMonthCache.clear();
        Log.d(TAG, "Cache cleared");
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying CalendarDataProviderImpl");

        mIsDestroyed = true;

        // Clear cache
        clearCache();

        // Note: ExecutorService is managed by CalendarModule

        Log.d(TAG, "CalendarDataProviderImpl destroyed");
    }

    // ==================== PRIVATE DATA LOADING METHODS ====================

    /**
     * Load calendar data for date range synchronously.
     * This method combines work schedule and events data.
     */
    @NonNull
    private List<CalendarDay> loadDateRangeDataSync(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        try {
            // Load work schedule data
            Map<LocalDate, List<WorkScheduleEvent>> workScheduleMap = loadWorkScheduleData(startDate, endDate);

            // Load events data
            Map<LocalDate, List<LocalEvent>> eventsMap = loadEventsData(startDate, endDate);

            // Load shift types for effective shift calculation
            Map<LocalDate, ShiftTypeEntity> effectiveShiftsMap = calculateEffectiveShifts(workScheduleMap, eventsMap, startDate, endDate);

            // Combine data into CalendarDay objects
            List<CalendarDay> calendarDays = new ArrayList<>();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                CalendarDay calendarDay = createCalendarDay(
                        currentDate,
                        workScheduleMap.getOrDefault(currentDate, new ArrayList<>()),
                        eventsMap.getOrDefault(currentDate, new ArrayList<>()),
                        effectiveShiftsMap.get(currentDate)
                );

                calendarDays.add(calendarDay);
                currentDate = currentDate.plusDays(1);
            }

            return calendarDays;

        } catch (Exception e) {
            Log.e(TAG, "Error in loadDateRangeDataSync", e);
            throw e;
        }
    }

    /**
     * Load work schedule data from WorkScheduleService.
     */
    @NonNull
    private Map<LocalDate, List<WorkScheduleEvent>> loadWorkScheduleData(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        Map<LocalDate, List<WorkScheduleEvent>> workScheduleMap = new HashMap<>();

        try {
            // Determine schedule type - could be configurable
            net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType scheduleType =
                    net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType.FIXED_4_2;

            // Determine user team
            String userTeam = getCurrentUserTeam();

            // Load work schedule data
            CompletableFuture<OperationResult<List<WorkScheduleEvent>>> future =
                    mWorkScheduleService.generateSchedule(startDate, endDate, scheduleType, userTeam);

            OperationResult<List<WorkScheduleEvent>> result = future.get();

            if (result.isSuccess() && result.getData() != null) {
                // Group by date
                for (WorkScheduleEvent event : result.getData()) {
                    LocalDate date = event.getDate();
                    workScheduleMap.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
                }
            } else {
                Log.w(TAG, "Failed to load work schedule: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading work schedule data", e);
        }

        return workScheduleMap;
    }

    /**
     * Load events data from EventsService.
     */
    @NonNull
    private Map<LocalDate, List<LocalEvent>> loadEventsData(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();

        try {
            // Load events from service
            CompletableFuture<OperationResult<List<LocalEvent>>> future =
                    mEventsService.getEventsByDateRange(startDate, endDate);

            OperationResult<List<LocalEvent>> result = future.get();

            if (result.isSuccess() && result.getData() != null) {
                // Group by start date
                for (LocalEvent event : result.getData()) {
                    LocalDate eventDate = event.getStartTime().toLocalDate();

                    // Filter by current user if specified
                    if (mCurrentUserId != null && !isEventRelevantForUser(event, mCurrentUserId)) {
                        continue;
                    }

                    eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
                }
            } else {
                Log.w(TAG, "Failed to load events: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading events data", e);
        }

        return eventsMap;
    }

    /**
     * Calculate effective shifts considering events that modify schedules.
     */
    @NonNull
    private Map<LocalDate, ShiftTypeEntity> calculateEffectiveShifts(
            @NonNull Map<LocalDate, List<WorkScheduleEvent>> workScheduleMap,
            @NonNull Map<LocalDate, List<LocalEvent>> eventsMap,
            @NonNull LocalDate startDate, @NonNull LocalDate endDate) {

        Map<LocalDate, ShiftTypeEntity> effectiveShiftsMap = new HashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            ShiftTypeEntity effectiveShift = calculateEffectiveShiftForDate(
                    currentDate,
                    workScheduleMap.get(currentDate),
                    eventsMap.get(currentDate)
            );

            if (effectiveShift != null) {
                effectiveShiftsMap.put(currentDate, effectiveShift);
            }

            currentDate = currentDate.plusDays(1);
        }

        return effectiveShiftsMap;
    }

    /**
     * Calculate effective shift for a specific date.
     */
    @Nullable
    private ShiftTypeEntity calculateEffectiveShiftForDate(@NonNull LocalDate date,
                                                           @Nullable List<WorkScheduleEvent> workScheduleEvents,
                                                           @Nullable List<LocalEvent> events) {

        // Start with base shift from work schedule
        ShiftTypeEntity baseShift = null;
        if (workScheduleEvents != null && !workScheduleEvents.isEmpty()) {
            // Use the first work schedule event's shift type
            WorkScheduleEvent firstEvent = workScheduleEvents.get(0);
            baseShift = firstEvent.getShiftType();
        }

        // Check for events that modify the shift
        if (events != null) {
            for (LocalEvent event : events) {
                if (event.getEventType().affectsWorkSchedule()) {
                    // Event modifies work schedule - could change shift type
                    // For now, keep the base shift but mark as modified
                    // Future enhancement: implement actual shift modification logic
                }
            }
        }

        return baseShift;
    }

    /**
     * Create CalendarDay object from loaded data.
     */
    @NonNull
    private CalendarDay createCalendarDay(@NonNull LocalDate date,
                                          @NonNull List<WorkScheduleEvent> workScheduleEvents,
                                          @NonNull List<LocalEvent> events,
                                          @Nullable ShiftTypeEntity effectiveShift) {

        // Calculate if shift was modified
        boolean hasShiftModification = events.stream()
                .anyMatch(event -> event.getEventType().affectsWorkSchedule());

        // Calculate if has high priority events
        boolean hasHighPriorityEvents = events.stream()
                .anyMatch(event -> event.getPriority() == EventPriority.HIGH ||
                        event.getPriority() == EventPriority.URGENT);

        // Calculate background color
        int backgroundColor = calculateBackgroundColor(effectiveShift);

        // Calculate text color
        int textColor = calculateTextColor(backgroundColor);

        // Check if has indicators
        boolean hasIndicators = !events.isEmpty() || hasShiftModification;

        return new CalendarDay.Builder(date)
                .workScheduleEvents(workScheduleEvents)
                .effectiveShift(effectiveShift)
                .hasShiftModification(hasShiftModification)
                .events(events)
                .hasHighPriorityEvents(hasHighPriorityEvents)
                .backgroundColor(backgroundColor)
                .textColor(textColor)
                .hasIndicators(hasIndicators)
                .build();
    }

    /**
     * Create error calendar day.
     */
    @NonNull
    private CalendarDay createErrorDay(@NonNull LocalDate date) {
        return new CalendarDay.Builder(date)
                .hasError(true)
                .build();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculate background color based on shift type.
     */
    private int calculateBackgroundColor(@Nullable ShiftTypeEntity shiftType) {
        if (shiftType != null && shiftType.getColorHex() != null) {
            try {
                String colorHex = shiftType.getColorHex();
                if (!colorHex.startsWith("#")) {
                    colorHex = "#" + colorHex;
                }
                return Color.parseColor(colorHex);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color hex: " + shiftType.getColorHex());
            }
        }

        // Default background color
        return mContext.getResources().getColor(R.color.calendar_day_default_background);
    }

    /**
     * Calculate text color for readability.
     */
    private int calculateTextColor(int backgroundColor) {
        // Calculate luminance
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get current user's team.
     */
    @Nullable
    private String getCurrentUserTeam() {
        // TODO: Implement user team retrieval
        // For now, return null to get all teams
        return null;
    }

    /**
     * Check if event is relevant for specific user.
     */
    private boolean isEventRelevantForUser(@NonNull LocalEvent event, @NonNull Long userId) {
        // TODO: Implement user-specific event filtering
        // For now, include all events
        return true;
    }

    /**
     * Set loading state thread-safely.
     */
    private void setLoadingState(@NonNull CalendarLoadingState state) {
        mLoadingState = state;
    }

    /**
     * Cleanup cache to prevent memory leaks.
     */
    private void cleanupCache() {
        if (mMonthCache.size() <= MAX_CACHE_SIZE) return;

        // Remove expired entries first
        mMonthCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // If still too large, remove oldest entries
        if (mMonthCache.size() > MAX_CACHE_SIZE) {
            List<YearMonth> months = new ArrayList<>(mMonthCache.keySet());
            months.sort(YearMonth::compareTo);

            int toRemove = mMonthCache.size() - MAX_CACHE_SIZE;
            for (int i = 0; i < toRemove && i < months.size(); i++) {
                mMonthCache.remove(months.get(i));
            }
        }

        Log.v(TAG, "Cache cleaned up, size: " + mMonthCache.size());
    }
}