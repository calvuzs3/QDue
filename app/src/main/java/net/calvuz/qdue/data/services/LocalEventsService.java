package net.calvuz.qdue.data.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LocalEvents Service Interface
 *
 * <p>Comprehensive service interface for LocalEvent operations that replaces the
 * existing EventsService. This service provides high-level business operations
 * for LocalEvent management, integrating with CalendarServiceProvider and
 * following clean architecture principles.</p>
 *
 * <h3>Service Features:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Complete create, read, update, delete support</li>
 *   <li><strong>Business Logic</strong>: Validation, conflict resolution, event management</li>
 *   <li><strong>Calendar Integration</strong>: Monthly views, date ranges, upcoming events</li>
 *   <li><strong>Search & Filtering</strong>: Advanced query and filtering capabilities</li>
 *   <li><strong>Batch Operations</strong>: Efficient bulk operations for multiple events</li>
 *   <li><strong>Analytics</strong>: Statistics and summary information</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <p>This service is designed to be provided by CalendarServiceProvider and used
 * by ViewModels in the MVVM architecture. It orchestrates use cases while
 * providing a clean service interface for UI components.</p>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // Service injection in ViewModel
 * public class LocalEventsViewModel extends BaseViewModel {
 *     private final LocalEventsService mEventsService;
 *
 *     public LocalEventsViewModel(LocalEventsService eventsService) {
 *         this.mEventsService = eventsService;
 *     }
 *
 *     public void loadEvents() {
 *         mEventsService.getAllEvents()
 *             .thenAccept(result -> updateEventsState(result));
 *     }
 * }
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @see net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases
 * @see net.calvuz.qdue.data.di.CalendarServiceProvider
 * @since LocalEvents MVVM Implementation
 */
public interface LocalEventsService
{

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Initialize the service and its dependencies.
     *
     * @return CompletableFuture with OperationResult indicating initialization success
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> initialize();

    /**
     * Check if service is ready for operations.
     *
     * @return true if service is initialized and ready
     */
    boolean isReady();

    /**
     * Shutdown service and cleanup resources.
     */
    void shutdown();

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create new LocalEvent with business validation.
     *
     * @param event Event to create (must not be null)
     * @return CompletableFuture with OperationResult containing created event
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> createEvent(@NonNull LocalEvent event);

    /**
     * Create new LocalEvent with optional conflict checking.
     *
     * @param event          Event to create
     * @param checkConflicts Whether to check for time conflicts
     * @return CompletableFuture with OperationResult containing created event
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> createEventWithConflictCheck(
            @NonNull LocalEvent event, boolean checkConflicts);

    /**
     * Update existing LocalEvent with validation.
     *
     * @param event Event to update (must have valid ID)
     * @return CompletableFuture with OperationResult containing updated event
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> updateEvent(@NonNull LocalEvent event);

    /**
     * Update event with optional conflict checking.
     *
     * @param event          Event to update
     * @param checkConflicts Whether to check for time conflicts
     * @return CompletableFuture with OperationResult containing updated event
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> updateEventWithConflictCheck(
            @NonNull LocalEvent event, boolean checkConflicts);

    /**
     * Delete LocalEvent by ID.
     *
     * @param eventId ID of event to delete
     * @return CompletableFuture with OperationResult indicating success/failure
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> deleteEvent(@NonNull String eventId);

    /**
     * Delete multiple events by IDs.
     *
     * @param eventIds List of event IDs to delete
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> deleteEvents(@NonNull List<String> eventIds);

    /**
     * Delete all events with confirmation.
     *
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> deleteAllEvents();

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Get LocalEvent by ID.
     *
     * @param eventId ID of event to retrieve
     * @return CompletableFuture with OperationResult containing event or error
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> getEventById(@NonNull String eventId);

    /**
     * Get all LocalEvents ordered by start time.
     *
     * @return CompletableFuture with OperationResult containing list of all events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getAllEvents();

    /**
     * Get LocalEvents for specific date.
     *
     * @param date Target date
     * @return CompletableFuture with OperationResult containing events for date
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDate(@NonNull LocalDateTime date);

    /**
     * Get LocalEvents for date range.
     *
     * @param startDate Range start date (inclusive)
     * @param endDate   Range end date (inclusive)
     * @return CompletableFuture with OperationResult containing events in range
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDateRange(
            @NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate);

    /**
     * Get LocalEvents for entire month.
     *
     * @param yearMonth Target month
     * @return CompletableFuture with OperationResult containing events for month
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForMonth(@NonNull YearMonth yearMonth);

    /**
     * Get upcoming LocalEvents from current time.
     *
     * @param limit Maximum number of events to return
     * @return CompletableFuture with OperationResult containing upcoming events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEvents(int limit);

    /**
     * Get upcoming LocalEvents from specific time.
     *
     * @param fromTime Time to search from
     * @param limit    Maximum number of events to return
     * @return CompletableFuture with OperationResult containing upcoming events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEventsFromTime(
            @NonNull LocalDateTime fromTime, int limit);

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Search LocalEvents by query string.
     *
     * @param query Search query (title and description)
     * @return CompletableFuture with OperationResult containing matching events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> searchEvents(@NonNull String query);

    /**
     * Get LocalEvents by type.
     *
     * @param eventType Type of events to retrieve
     * @return CompletableFuture with OperationResult containing events of specified type
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByType(@NonNull EventType eventType);

    /**
     * Get LocalEvents by priority.
     *
     * @param priority Priority level to filter by
     * @return CompletableFuture with OperationResult containing events with specified priority
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByPriority(@NonNull Priority priority);

    /**
     * Get LocalEvents by calendar ID.
     *
     * @param calendarId Calendar ID to filter by
     * @return CompletableFuture with OperationResult containing events from specified calendar
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByCalendarId(@NonNull String calendarId);

    // ==================== BATCH OPERATIONS ====================

    /**
     * Create multiple LocalEvents in batch operation.
     *
     * @param events List of events to create
     * @return CompletableFuture with OperationResult containing list of created events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> createEvents(@NonNull List<LocalEvent> events);

    /**
     * Update multiple LocalEvents in batch operation.
     *
     * @param events List of events to update
     * @return CompletableFuture with OperationResult containing list of updated events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> updateEvents(@NonNull List<LocalEvent> events);

    // ==================== VALIDATION AND BUSINESS LOGIC ====================

    /**
     * Validate LocalEvent data without persisting.
     *
     * @param event Event to validate
     * @return OperationResult indicating validation success/failure with error details
     */
    @NonNull
    OperationResult<Void> validateEvent(@NonNull LocalEvent event);

    /**
     * Check for conflicting events in time range.
     *
     * @param startTime      Conflict check start time
     * @param endTime        Conflict check end time
     * @param excludeEventId Optional event ID to exclude from conflict check
     * @return CompletableFuture with OperationResult containing list of conflicting events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> findConflictingEvents(
            @NonNull LocalDateTime startTime, @NonNull LocalDateTime endTime, @Nullable String excludeEventId);

    /**
     * Check if LocalEvent exists by ID.
     *
     * @param eventId ID to check
     * @return CompletableFuture with OperationResult indicating existence
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> eventExists(@NonNull String eventId);

    // ==================== STATISTICS AND ANALYTICS ====================

    /**
     * Get total count of LocalEvents.
     *
     * @return CompletableFuture with OperationResult containing total event count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getEventsCount();

    /**
     * Get count of upcoming LocalEvents.
     *
     * @return CompletableFuture with OperationResult containing upcoming events count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getUpcomingEventsCount();

    /**
     * Get events summary for calendar view.
     *
     * @param yearMonth Target month
     * @return CompletableFuture with OperationResult containing events summary
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEventsUseCases.LocalEventsSummary>> getEventsSummaryForMonth(
            @NonNull YearMonth yearMonth
    );

    // ==================== CALENDAR INTEGRATION ====================

    /**
     * Refresh events cache and notify observers.
     *
     * @return CompletableFuture with OperationResult indicating refresh success
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> refreshEvents();

    /**
     * Get events that need user attention (conflicts, missing info, etc.).
     *
     * @return CompletableFuture with OperationResult containing events needing attention
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsNeedingAttention();

    // ==================== SERVICE STATE QUERIES ====================

    /**
     * Get service status information.
     *
     * @return ServiceStatus with current service state
     */
    @NonNull
    ServiceStatus getServiceStatus();

    // ==================== INNER CLASSES ====================

    /**
         * Service status information.
         */
        record ServiceStatus(boolean initialized, boolean ready, String version, long lastOperationTime,
                             int totalEvents)
        {

            @Override
            public String toString() {
                return String.format( QDue.getLocale(),
                                      "ServiceStatus{initialized=%s, ready=%s, version='%s', " +
                                              "lastOperationTime=%d, totalEvents=%d}",
                                      initialized, ready, version, lastOperationTime, totalEvents );
            }
        }
}