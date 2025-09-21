package net.calvuz.qdue.ui.features.dayview.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * DayViewDataLoader - Async Multi-Source Data Loading for Day View
 *
 * <p>Handles asynchronous loading of events from multiple sources (LocalEvent, WorkScheduleDay)
 * with aggregation and error handling. Designed with extensible architecture to support
 * additional event types in the future.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-Source Loading</strong>: Parallel loading from LocalEvents and WorkSchedule</li>
 *   <li><strong>Clean Architecture</strong>: Uses use cases and repositories for data access</li>
 *   <li><strong>Error Resilience</strong>: Partial success handling when some sources fail</li>
 *   <li><strong>User Context</strong>: Supports user-specific data filtering</li>
 *   <li><strong>Extensible Design</strong>: Easy addition of new event sources</li>
 * </ul>
 *
 * <h3>Data Sources:</h3>
 * <ul>
 *   <li><strong>LocalEvents</strong>: User-created events via LocalEventsService</li>
 *   <li><strong>WorkSchedule</strong>: Work schedule data via GenerateUserScheduleUseCase</li>
 *   <li><strong>Future Sources</strong>: Extensible architecture for new event types</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>
 * DayViewDataLoader loader = new DayViewDataLoader(services...);
 * loader.loadEventsForDate(LocalDate.now())
 *       .thenAccept(eventsMap -> {
 *           // Handle aggregated events from all sources
 *       })
 *       .exceptionally(throwable -> {
 *           // Handle loading errors
 *       });
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewDataLoader {

    private static final String TAG = "DayViewDataLoader";

    // ==================== EVENT SOURCE KEYS ====================

    public static final String SOURCE_LOCAL_EVENTS = "local_events";
    public static final String SOURCE_WORK_SCHEDULE = "work_schedule";
    // Future sources can be added here:
    // public static final String SOURCE_CALENDAR_EVENTS = "calendar_events";
    // public static final String SOURCE_EXTERNAL_EVENTS = "external_events";

    // ==================== DATA SOURCES ====================

    private final LocalEventsService mLocalEventsService;
    private final UserWorkScheduleService mUserWorkScheduleService;

    // ==================== CONFIGURATION ====================

    private final QDueUser mQDueUser;
    private boolean mLoadWorkSchedule = true;
    private boolean mLoadLocalEvents = true;
    private boolean mAllowPartialFailure = true;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create DayViewDataLoader with required data sources.
     *
     * @param localEventsService        LocalEventsService for local events
     * @param userWorkScheduleService   UserWorkScheduleService for work schedule
     * @param qDueUser                  User context (null for default user)
     */
    public DayViewDataLoader(
            @NonNull LocalEventsService localEventsService,
            @NonNull UserWorkScheduleService userWorkScheduleService,
            @NonNull QDueUser qDueUser
    ) {
        this.mLocalEventsService = localEventsService;
        this.mUserWorkScheduleService = userWorkScheduleService;
        this.mQDueUser = qDueUser;

        Log.d( TAG, "DayViewDataLoader created for user: " +
                qDueUser );
    }

    // ==================== CONFIGURATION METHODS ====================

    /**
     * Configure which data sources to load.
     *
     * @param loadLocalEvents   Load local events
     * @param loadWorkSchedule  Load work schedule
     * @return this loader for method chaining
     */
    @NonNull
    public DayViewDataLoader configure(boolean loadLocalEvents, boolean loadWorkSchedule) {
        this.mLoadLocalEvents = loadLocalEvents;
        this.mLoadWorkSchedule = loadWorkSchedule;

        Log.d(TAG, "Configured data sources - LocalEvents: " + loadLocalEvents +
                ", WorkSchedule: " + loadWorkSchedule);
        return this;
    }

    /**
     * Set whether to allow partial failures.
     * If true, returns data from successful sources even if some sources fail.
     * If false, fails completely if any source fails.
     *
     * @param allowPartialFailure Allow partial failure
     * @return this loader for method chaining
     */
    @NonNull
    public DayViewDataLoader setAllowPartialFailure(boolean allowPartialFailure) {
        this.mAllowPartialFailure = allowPartialFailure;
        Log.d(TAG, "Allow partial failure set to: " + allowPartialFailure);
        return this;
    }

    // ==================== MAIN LOADING METHODS ====================

    /**
     * Load all events for specified date from all configured sources.
     *
     * @param targetDate Date to load events for
     * @return CompletableFuture with map of source -> events list
     */
    @NonNull
    public CompletableFuture<Map<String, List<Object>>> loadEventsForDate(@NonNull LocalDate targetDate) {
        Log.d(TAG, "Loading events for date: " + targetDate);

        List<CompletableFuture<Map.Entry<String, List<Object>>>> loadingTasks = new ArrayList<>();

        // Load local events if configured
        if (mLoadLocalEvents) {
            CompletableFuture<Map.Entry<String, List<Object>>> localEventsTask =
                    loadLocalEventsForDate(targetDate)
                            .thenApply(events -> Map.entry(SOURCE_LOCAL_EVENTS, (List<Object>) new ArrayList<Object>(events)))
                            .exceptionally(throwable -> {
                                Log.e( TAG, "Failed to load local events for date: " + targetDate,
                                       throwable );
                                if (mAllowPartialFailure) {
                                    return Map.entry(SOURCE_LOCAL_EVENTS, new ArrayList<>());
                                } else {
                                    throw new CompletionException( throwable );
                                }
                            });
            loadingTasks.add(localEventsTask);
        }

        // Load work schedule if configured
        if (mLoadWorkSchedule) {
            CompletableFuture<Map.Entry<String, List<Object>>> workScheduleTask =
                    loadWorkScheduleForDate(targetDate)
                            .thenApply(days -> Map.entry(SOURCE_WORK_SCHEDULE, (List<Object>) new ArrayList<Object>(days)))
                            .exceptionally(throwable -> {
                                Log.e( TAG, "Failed to load work schedule for date: " + targetDate,
                                       throwable );
                                if (mAllowPartialFailure) {
                                    return Map.entry(SOURCE_WORK_SCHEDULE, new ArrayList<>());
                                } else {
                                    throw new CompletionException( throwable );
                                }
                            });
            loadingTasks.add(workScheduleTask);
        }

        // Execute all loading tasks in parallel
        return CompletableFuture.allOf(loadingTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, List<Object>> results = new HashMap<>();

                    for (CompletableFuture<Map.Entry<String, List<Object>>> task : loadingTasks) {
                        try {
                            Map.Entry<String, List<Object>> entry = task.join();
                            results.put(entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to get result from loading task", e);
                            if (!mAllowPartialFailure) {
                                throw new CompletionException(e);
                            }
                        }
                    }

                    logLoadingResults(targetDate, results);
                    return results;
                });
    }

    /**
     * Load events for date range from all sources.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return CompletableFuture with map of date -> (source -> events)
     */
    @NonNull
    public CompletableFuture<Map<LocalDate, Map<String, List<Object>>>> loadEventsForDateRange(
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate) {

        Log.d(TAG, "Loading events for date range: " + startDate + " to " + endDate);

        List<CompletableFuture<Map.Entry<LocalDate, Map<String, List<Object>>>>> dateLoadingTasks =
                new ArrayList<>();

        // Load for each date in range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate dateToLoad = currentDate;

            CompletableFuture<Map.Entry<LocalDate, Map<String, List<Object>>>> dateTask =
                    loadEventsForDate(dateToLoad)
                            .thenApply(eventsMap -> Map.entry(dateToLoad, eventsMap));

            dateLoadingTasks.add(dateTask);
            currentDate = currentDate.plusDays(1);
        }

        return CompletableFuture.allOf(dateLoadingTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<LocalDate, Map<String, List<Object>>> results = new HashMap<>();

                    for (CompletableFuture<Map.Entry<LocalDate, Map<String, List<Object>>>> task : dateLoadingTasks) {
                        try {
                            Map.Entry<LocalDate, Map<String, List<Object>>> entry = task.join();
                            results.put(entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to get result from date loading task", e);
                            if (!mAllowPartialFailure) {
                                throw new CompletionException(e);
                            }
                        }
                    }

                    Log.d(TAG, "Loaded events for " + results.size() + " dates in range");
                    return results;
                });
    }

    /**
     * Refresh events for date by clearing cache and reloading.
     *
     * @param targetDate Date to refresh
     * @return CompletableFuture with refreshed events
     */
    @NonNull
    public CompletableFuture<Map<String, List<Object>>> refreshEventsForDate(@NonNull LocalDate targetDate) {
        Log.d(TAG, "Refreshing events for date: " + targetDate);

        // Clear any local caches here if implemented
        // mLocalCache.invalidate(targetDate);

        return loadEventsForDate(targetDate);
    }

    // ==================== INDIVIDUAL SOURCE LOADING METHODS ====================

    /**
     * Load local events for specific date.
     *
     * @param targetDate Target date
     * @return CompletableFuture with local events list
     */
    @NonNull
    private CompletableFuture<List<LocalEvent>> loadLocalEventsForDate(@NonNull LocalDate targetDate) {
        Log.d(TAG, "Loading local events for date: " + targetDate);

        return mLocalEventsService.getEventsForDate( LocalDateTime.from( targetDate.atStartOfDay() ))
                .thenApply(events -> {
                    if (events.isSuccess()) {
                        Log.d( TAG, "Loaded local events for date: " + targetDate );
                        return events.getData();
                    } else return new ArrayList<LocalEvent>();
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to load local events for date: " + targetDate, throwable);
                    throw new CompletionException("Failed to load local events", throwable);
                });
    }

    /**
     * Load work schedule for specific date.
     *
     * @param targetDate Target date
     * @return CompletableFuture with work schedule days list
     */
    @NonNull
    private CompletableFuture<List<WorkScheduleDay>> loadWorkScheduleForDate(
            @NonNull LocalDate targetDate
    ) {
        Log.d(TAG, "Loading work schedule for date: " + targetDate);

        if (mQDueUser == null ) {
            throw new RuntimeException("QDueUser is null");
        }
        String userId = mQDueUser.getId();

        return mUserWorkScheduleService.generateUserScheduleForDate( userId, targetDate ) //mGenerateUserScheduleUseCase.getGenerateUserScheduleForDate().execute( userId, targetDate ) // .generateUserScheduleForDate(userId, targetDate)
                .thenApply(schedule -> {
                    List<WorkScheduleDay> days = new ArrayList<>();

                    if (schedule.isSuccess()) {
                        if (schedule.getData() != null && schedule.getData().getDate().equals( targetDate )) {
                            days.add( schedule.getData() );
                        }

                        Log.d( TAG,
                               "Loaded " + days.size() + " work schedule days for date: " + targetDate );
                    }
                    return days;
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to load work schedule for date: " + targetDate, throwable);
                    throw new CompletionException("Failed to load work schedule", throwable);
                });
    }

    // Future source loading methods can be added here:
    // private CompletableFuture<List<CalendarEvent>> loadCalendarEventsForDate(LocalDate targetDate) { ... }
    // private CompletableFuture<List<ExternalEvent>> loadExternalEventsForDate(LocalDate targetDate) { ... }

    // ==================== UTILITY METHODS ====================

    /**
     * Get aggregated event count for all sources.
     *
     * @param eventsMap Events map from loadEventsForDate
     * @return Total event count
     */
    public int getTotalEventCount(@NonNull Map<String, List<Object>> eventsMap) {
        int totalCount = 0;
        for (List<?> eventsList : eventsMap.values()) {
            totalCount += eventsList.size();
        }
        return totalCount;
    }

    /**
     * Get events of specific type from events map.
     *
     * @param eventsMap Events map from loadEventsForDate
     * @param sourceKey Source key (e.g., SOURCE_LOCAL_EVENTS)
     * @param eventType Expected event type class
     * @return List of events of specified type
     */
    @NonNull
    public <T> List<T> getEventsOfType(@NonNull Map<String, List<Object>> eventsMap,
                                       @NonNull String sourceKey,
                                       @NonNull Class<T> eventType) {
        List<Object> events = eventsMap.get(sourceKey);
        if (events == null) {
            return new ArrayList<>();
        }

        List<T> typedEvents = new ArrayList<>();
        for (Object event : events) {
            if (eventType.isInstance(event)) {
                typedEvents.add(eventType.cast(event));
            }
        }

        return typedEvents;
    }

    /**
     * Check if any source has events for the loaded data.
     *
     * @param eventsMap Events map from loadEventsForDate
     * @return true if any source has events
     */
    public boolean hasAnyEvents(@NonNull Map<String, List<?>> eventsMap) {
        return eventsMap.values().stream()
                .anyMatch(eventsList -> eventsList != null && !eventsList.isEmpty());
    }

    /**
     * Get data source status summary.
     *
     * @param eventsMap Events map from loadEventsForDate
     * @return Status summary string
     */
    @NonNull
    public String getDataSourceSummary(@NonNull Map<String, List<Object>> eventsMap) {
        StringBuilder summary = new StringBuilder();
        summary.append("Data Sources: ");

        for (Map.Entry<String, List<Object>> entry : eventsMap.entrySet()) {
            String source = entry.getKey();
            int count = entry.getValue() != null ? entry.getValue().size() : 0;
            summary.append(source).append("(").append(count).append(") ");
        }

        return summary.toString().trim();
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Log loading results for debugging.
     */
    private void logLoadingResults(@NonNull LocalDate targetDate, @NonNull Map<String, List<Object>> results) {
        int totalEvents = getTotalEventCount(results);
        String summary = getDataSourceSummary(results);

        Log.d(TAG, "Loading completed for date " + targetDate +
                " - Total events: " + totalEvents +
                " - " + summary);
    }
}