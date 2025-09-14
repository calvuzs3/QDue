package net.calvuz.qdue.domain.events.local.usecases;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.events.local.repository.LocalEventRepository;
import net.calvuz.qdue.domain.events.models.LocalEvent;
import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LocalEvents Use Cases Collection
 *
 * <p>Comprehensive collection of business use cases for LocalEvent operations.
 * This class provides high-level business operations that orchestrate repository
 * calls while enforcing business rules and validation logic.</p>
 *
 * <h3>Available Use Cases:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Create, read, update, delete events</li>
 *   <li><strong>Business Logic</strong>: Validation, conflict resolution, event management</li>
 *   <li><strong>Query Operations</strong>: Search, filter, date-based queries</li>
 *   <li><strong>Calendar Integration</strong>: Monthly views, date ranges, upcoming events</li>
 * </ul>
 *
 * <h3>Business Rules Enforced:</h3>
 * <ul>
 *   <li>Event validation before persistence</li>
 *   <li>Conflict detection and resolution</li>
 *   <li>Proper error handling and logging</li>
 *   <li>Data consistency and integrity</li>
 * </ul>
 *
 * @see net.calvuz.qdue.domain.events.local.repository.LocalEventRepository
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsUseCases {

    private static final String TAG = "LocalEventsUseCases";

    // ==================== DEPENDENCIES ====================

    private final LocalEventRepository mRepository;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param repository LocalEvent repository for data operations
     */
    public LocalEventsUseCases(@NonNull LocalEventRepository repository) {
        this.mRepository = repository;
        Log.d(TAG, "LocalEventsUseCases initialized");
    }

    // ==================== CREATE OPERATIONS ====================

    /**
     * Create new LocalEvent with business validation.
     *
     * @param event Event to create
     * @return CompletableFuture with OperationResult containing created event
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> createEvent(@NonNull LocalEvent event) {
        Log.d(TAG, "Creating event: " + event.getTitle());

        return mRepository.validateLocalEvent(event).isSuccess() ?
                mRepository.createLocalEvent(event) :
                CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Event validation failed",
                                OperationResult.OperationType.CREATE
                        )
                );
    }

    /**
     * Create new LocalEvent with conflict checking.
     *
     * @param event Event to create
     * @param checkConflicts Whether to check for time conflicts
     * @return CompletableFuture with OperationResult containing created event
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> createEventWithConflictCheck(
            @NonNull LocalEvent event, boolean checkConflicts) {
        Log.d(TAG, "Creating event with conflict check: " + event.getTitle());

        if (!checkConflicts) {
            return createEvent(event);
        }

        return mRepository.findConflictingEvents(
                event.getStartTime(), event.getEndTime(), null
        ).thenCompose(conflictResult -> {
            if (!conflictResult.isSuccess()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Failed to check conflicts: " + conflictResult.getFirstError(),
                                OperationResult.OperationType.CREATE
                        )
                );
            }

            List<LocalEvent> conflicts = conflictResult.getData();
            if (!conflicts.isEmpty()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Event conflicts with " + conflicts.size() + " existing events",
                                OperationResult.OperationType.CREATE
                        )
                );
            }

            return createEvent(event);
        });
    }

    /**
     * Create multiple events in batch operation.
     *
     * @param events List of events to create
     * @return CompletableFuture with OperationResult containing created events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> createEvents(@NonNull List<LocalEvent> events) {
        Log.d(TAG, "Creating " + events.size() + " events in batch");
        return mRepository.createLocalEvents(events);
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get event by ID.
     *
     * @param eventId ID of event to retrieve
     * @return CompletableFuture with OperationResult containing event or error
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> getEventById(@NonNull String eventId) {
        Log.d(TAG, "Getting event by ID: " + eventId);
        return mRepository.getLocalEventById(eventId);
    }

    /**
     * Get all events ordered by start time.
     *
     * @return CompletableFuture with OperationResult containing all events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getAllEvents() {
        Log.d(TAG, "Getting all events");
        return mRepository.getAllLocalEvents();
    }

    /**
     * Get events for specific date.
     *
     * @param date Target date
     * @return CompletableFuture with OperationResult containing events for date
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDate(@NonNull LocalDateTime date) {
        Log.d(TAG, "Getting events for date: " + date.toLocalDate());
        return mRepository.getLocalEventsForDate(date);
    }

    /**
     * Get events for date range.
     *
     * @param startDate Range start date
     * @param endDate Range end date
     * @return CompletableFuture with OperationResult containing events in range
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDateRange(
            @NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate) {
        Log.d(TAG, "Getting events for date range: " + startDate.toLocalDate() + " to " + endDate.toLocalDate());
        return mRepository.getLocalEventsForDateRange(startDate, endDate);
    }

    /**
     * Get events for entire month.
     *
     * @param yearMonth Target month
     * @return CompletableFuture with OperationResult containing events for month
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForMonth(@NonNull YearMonth yearMonth) {
        Log.d(TAG, "Getting events for month: " + yearMonth);

        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        return getEventsForDateRange(startOfMonth, endOfMonth);
    }

    /**
     * Get upcoming events from current time.
     *
     * @param limit Maximum number of events to return
     * @return CompletableFuture with OperationResult containing upcoming events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEvents(int limit) {
        Log.d(TAG, "Getting upcoming events (limit: " + limit + ")");
        return mRepository.getUpcomingLocalEvents(LocalDateTime.now(), limit);
    }

    /**
     * Get upcoming events from specific time.
     *
     * @param fromTime Time to search from
     * @param limit Maximum number of events to return
     * @return CompletableFuture with OperationResult containing upcoming events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEventsFromTime(
            @NonNull LocalDateTime fromTime, int limit) {
        Log.d(TAG, "Getting upcoming events from: " + fromTime + " (limit: " + limit + ")");
        return mRepository.getUpcomingLocalEvents(fromTime, limit);
    }

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Update existing event with validation.
     *
     * @param event Event to update
     * @return CompletableFuture with OperationResult containing updated event
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> updateEvent(@NonNull LocalEvent event) {
        Log.d(TAG, "Updating event: " + event.getID());

        // Validate event first
        OperationResult<Void> validationResult = mRepository.validateLocalEvent(event);
        if (!validationResult.isSuccess()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure(
                            "Event validation failed: " + validationResult.getFirstError(),
                            OperationResult.OperationType.UPDATE
                    )
            );
        }

        return mRepository.updateLocalEvent(event);
    }

    /**
     * Update event with conflict checking.
     *
     * @param event Event to update
     * @param checkConflicts Whether to check for time conflicts
     * @return CompletableFuture with OperationResult containing updated event
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> updateEventWithConflictCheck(
            @NonNull LocalEvent event, boolean checkConflicts) {
        Log.d(TAG, "Updating event with conflict check: " + event.getID());

        if (!checkConflicts) {
            return updateEvent(event);
        }

        return mRepository.findConflictingEvents(
                event.getStartTime(), event.getEndTime(), event.getID()
        ).thenCompose(conflictResult -> {
            if (!conflictResult.isSuccess()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Failed to check conflicts: " + conflictResult.getFirstError(),
                                OperationResult.OperationType.UPDATE
                        )
                );
            }

            List<LocalEvent> conflicts = conflictResult.getData();
            if (!conflicts.isEmpty()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Event conflicts with " + conflicts.size() + " existing events",
                                OperationResult.OperationType.UPDATE
                        )
                );
            }

            return updateEvent(event);
        });
    }

    /**
     * Update multiple events in batch operation.
     *
     * @param events List of events to update
     * @return CompletableFuture with OperationResult containing updated events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> updateEvents(@NonNull List<LocalEvent> events) {
        Log.d(TAG, "Updating " + events.size() + " events in batch");
        return mRepository.updateLocalEvents(events);
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete event by ID.
     *
     * @param eventId ID of event to delete
     * @return CompletableFuture with OperationResult indicating success/failure
     */
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> deleteEvent(@NonNull String eventId) {
        Log.d(TAG, "Deleting event: " + eventId);
        return mRepository.deleteLocalEvent(eventId);
    }

    /**
     * Delete multiple events by IDs.
     *
     * @param eventIds List of event IDs to delete
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    public CompletableFuture<OperationResult<Integer>> deleteEvents(@NonNull List<String> eventIds) {
        Log.d(TAG, "Deleting " + eventIds.size() + " events");
        return mRepository.deleteLocalEvents(eventIds);
    }

    /**
     * Delete all events with confirmation.
     *
     * @param confirmationToken Security token to confirm deletion
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    public CompletableFuture<OperationResult<Integer>> deleteAllEvents(@NonNull String confirmationToken) {
        Log.d(TAG, "Deleting all events with confirmation");

        // Simple confirmation token check
        if (!"CONFIRM_DELETE_ALL".equals(confirmationToken)) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure(
                            "Invalid confirmation token for delete all operation",
                            OperationResult.OperationType.DELETE
                    )
            );
        }

        return mRepository.deleteAllLocalEvents();
    }

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Search events by query string.
     *
     * @param query Search query
     * @return CompletableFuture with OperationResult containing matching events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> searchEvents(@NonNull String query) {
        Log.d(TAG, "Searching events with query: " + query);

        if (query.trim().length() < 2) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure(
                            "Search query must be at least 2 characters long",
                            OperationResult.OperationType.READ
                    )
            );
        }

        return mRepository.searchLocalEvents(query);
    }

    /**
     * Get events by type.
     *
     * @param eventType Type of events to retrieve
     * @return CompletableFuture with OperationResult containing events of specified type
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByType(@NonNull EventType eventType) {
        Log.d(TAG, "Getting events by type: " + eventType);
        return mRepository.getLocalEventsByType(eventType);
    }

    /**
     * Get events by priority.
     *
     * @param priority Priority level to filter by
     * @return CompletableFuture with OperationResult containing events with specified priority
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByPriority(@NonNull Priority priority) {
        Log.d(TAG, "Getting events by priority: " + priority);
        return mRepository.getLocalEventsByPriority(priority);
    }

    /**
     * Get events by calendar ID.
     *
     * @param calendarId Calendar ID to filter by
     * @return CompletableFuture with OperationResult containing events from specified calendar
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByCalendarId(@NonNull String calendarId) {
        Log.d(TAG, "Getting events by calendar ID: " + calendarId);
        return mRepository.getLocalEventsByCalendarId(calendarId);
    }

    // ==================== STATISTICS AND ANALYTICS ====================

    /**
     * Get total count of events.
     *
     * @return CompletableFuture with OperationResult containing total event count
     */
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getEventsCount() {
        Log.d(TAG, "Getting events count");
        return mRepository.getLocalEventsCount();
    }

    /**
     * Get count of upcoming events.
     *
     * @return CompletableFuture with OperationResult containing upcoming events count
     */
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getUpcomingEventsCount() {
        Log.d(TAG, "Getting upcoming events count");
        return mRepository.getUpcomingLocalEventsCount(LocalDateTime.now());
    }

    /**
     * Check if event exists by ID.
     *
     * @param eventId ID to check
     * @return CompletableFuture with OperationResult indicating existence
     */
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> eventExists(@NonNull String eventId) {
        Log.d(TAG, "Checking if event exists: " + eventId);
        return mRepository.localEventExists(eventId);
    }

    // ==================== BUSINESS LOGIC OPERATIONS ====================

    /**
     * Find conflicting events for time range.
     *
     * @param startTime Conflict check start time
     * @param endTime Conflict check end time
     * @param excludeEventId Optional event ID to exclude from conflict check
     * @return CompletableFuture with OperationResult containing list of conflicting events
     */
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> findConflictingEvents(
            @NonNull LocalDateTime startTime, @NonNull LocalDateTime endTime, @Nullable String excludeEventId) {
        Log.d(TAG, "Finding conflicting events from " + startTime + " to " + endTime);
        return mRepository.findConflictingEvents(startTime, endTime, excludeEventId);
    }

    /**
     * Validate event without persisting.
     *
     * @param event Event to validate
     * @return OperationResult indicating validation success/failure
     */
    @NonNull
    public OperationResult<Void> validateEvent(@NonNull LocalEvent event) {
        Log.d(TAG, "Validating event: " + event.getTitle());
        return mRepository.validateLocalEvent(event);
    }

    /**
     * Get events summary for calendar view.
     *
     * @param yearMonth Target month
     * @return CompletableFuture with OperationResult containing events summary
     */
    @NonNull
    public CompletableFuture<OperationResult<LocalEventsSummary>> getEventsummaryForMonth(@NonNull YearMonth yearMonth) {
        Log.d(TAG, "Getting events summary for month: " + yearMonth);

        return getEventsForMonth(yearMonth).thenApply(result -> {
            if (!result.isSuccess()) {
                return OperationResult.failure(
                        "Failed to get events for month: " + result.getFirstError(),
                        OperationResult.OperationType.READ
                );
            }

            List<LocalEvent> events = result.getData();
            LocalEventsSummary summary = new LocalEventsSummary(yearMonth, events);

            return OperationResult.success(
                    summary,
                    "Events summary retrieved successfully",
                    OperationResult.OperationType.READ
            );
        });
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Summary of events for a specific month.
     */
    public static class LocalEventsSummary {
        private final YearMonth month;
        private final List<LocalEvent> events;
        private final int totalEvents;
        private final int allDayEvents;
        private final int timedEvents;

        public LocalEventsSummary(@NonNull YearMonth month, @NonNull List<LocalEvent> events) {
            this.month = month;
            this.events = events;
            this.totalEvents = events.size();
            this.allDayEvents = (int) events.stream().filter(LocalEvent::isAllDay).count();
            this.timedEvents = totalEvents - allDayEvents;
        }

        @NonNull
        public YearMonth getMonth() { return month; }

        @NonNull
        public List<LocalEvent> getEvents() { return events; }

        public int getTotalEvents() { return totalEvents; }

        public int getAllDayEvents() { return allDayEvents; }

        public int getTimedEvents() { return timedEvents; }

        public boolean hasEvents() { return totalEvents > 0; }
    }
}