package net.calvuz.qdue.domain.events.local.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.events.models.LocalEvent;
import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EventEntityGoogle Repository Interface
 *
 * <p>Provides comprehensive repository interface for EventEntityGoogle data operations
 * following the repository pattern and clean architecture principles. This interface
 * defines all data access operations for EventEntityGoogle entities with consistent
 * OperationResult return types and asynchronous CompletableFuture execution.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Full create, read, update, delete support</li>
 *   <li><strong>Query Operations</strong>: Advanced filtering and search capabilities</li>
 *   <li><strong>Calendar Integration</strong>: Date-based and range-based queries</li>
 *   <li><strong>Async Operations</strong>: All operations return CompletableFuture</li>
 *   <li><strong>Consistent Results</strong>: OperationResult pattern throughout</li>
 * </ul>
 *
 * <h3>Implementation Notes:</h3>
 * <p>Implementations should provide thread-safe operations, proper error handling,
 * and integration with the existing database layer. All operations should be
 * optimized for performance while maintaining data consistency.</p>
 *
 * @see net.calvuz.qdue.domain.events.models.LocalEvent
 * @see net.calvuz.qdue.core.services.models.OperationResult
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public interface LocalEventRepository {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create new EventEntityGoogle in repository.
     *
     * @param event EventEntityGoogle to create (must not be null)
     * @return CompletableFuture with OperationResult containing created event with assigned ID
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> createLocalEvent(@NonNull LocalEvent event);

    /**
     * Update existing EventEntityGoogle in repository.
     *
     * @param event EventEntityGoogle to update (must have valid ID)
     * @return CompletableFuture with OperationResult containing updated event
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> updateLocalEvent(@NonNull LocalEvent event);

    /**
     * Delete EventEntityGoogle by ID.
     *
     * @param eventId ID of event to delete
     * @return CompletableFuture with OperationResult indicating success/failure
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> deleteLocalEvent(@NonNull String eventId);

    /**
     * Delete multiple LocalEvents by IDs.
     *
     * @param eventIds List of event IDs to delete
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> deleteLocalEvents(@NonNull List<String> eventIds);

    /**
     * Delete all LocalEvents from repository.
     *
     * @return CompletableFuture with OperationResult containing number of deleted events
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> deleteAllLocalEvents();

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Get EventEntityGoogle by ID.
     *
     * @param eventId ID of event to retrieve
     * @return CompletableFuture with OperationResult containing event or null if not found
     */
    @NonNull
    CompletableFuture<OperationResult<LocalEvent>> getLocalEventById(@NonNull String eventId);

    /**
     * Get all LocalEvents ordered by start time.
     *
     * @return CompletableFuture with OperationResult containing list of all events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getAllLocalEvents();

    /**
     * Get LocalEvents for specific date (both all-day and timed events).
     *
     * @param date Target date for events
     * @return CompletableFuture with OperationResult containing events for the date
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsForDate(@NonNull LocalDateTime date);

    /**
     * Get LocalEvents within date range.
     *
     * @param startDate Range start date (inclusive)
     * @param endDate Range end date (inclusive)
     * @return CompletableFuture with OperationResult containing events in range
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsForDateRange(
            @NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate);

    /**
     * Get upcoming LocalEvents from current time.
     *
     * @param currentTime Current timestamp
     * @param limit Maximum number of events to return
     * @return CompletableFuture with OperationResult containing upcoming events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingLocalEvents(
            @NonNull LocalDateTime currentTime, int limit);

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Search LocalEvents by title or description.
     *
     * @param query Search query string
     * @return CompletableFuture with OperationResult containing matching events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> searchLocalEvents(@NonNull String query);

    /**
     * Get LocalEvents by type.
     *
     * @param eventType Type of events to retrieve
     * @return CompletableFuture with OperationResult containing events of specified type
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByType(@NonNull EventType eventType);

    /**
     * Get LocalEvents by priority.
     *
     * @param priority Priority level to filter by
     * @return CompletableFuture with OperationResult containing events with specified priority
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByPriority(@NonNull Priority priority);

    /**
     * Get LocalEvents by calendar ID.
     *
     * @param calendarId Calendar ID to filter by
     * @return CompletableFuture with OperationResult containing events from specified calendar
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByCalendarId(@NonNull String calendarId);

    // ==================== STATISTICS AND ANALYTICS ====================

    /**
     * Get total count of LocalEvents.
     *
     * @return CompletableFuture with OperationResult containing total event count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getLocalEventsCount();

    /**
     * Get count of upcoming LocalEvents.
     *
     * @param currentTime Current timestamp
     * @return CompletableFuture with OperationResult containing upcoming events count
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getUpcomingLocalEventsCount(@NonNull LocalDateTime currentTime);

    /**
     * Check if EventEntityGoogle exists by ID.
     *
     * @param eventId ID to check
     * @return CompletableFuture with OperationResult indicating existence
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> localEventExists(@NonNull String eventId);

    // ==================== BATCH OPERATIONS ====================

    /**
     * Create multiple LocalEvents in batch operation.
     *
     * @param events List of events to create
     * @return CompletableFuture with OperationResult containing list of created events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> createLocalEvents(@NonNull List<LocalEvent> events);

    /**
     * Update multiple LocalEvents in batch operation.
     *
     * @param events List of events to update
     * @return CompletableFuture with OperationResult containing list of updated events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> updateLocalEvents(@NonNull List<LocalEvent> events);

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validate EventEntityGoogle data without persisting.
     *
     * @param event Event to validate
     * @return OperationResult indicating validation success/failure with error details
     */
    @NonNull
    OperationResult<Void> validateLocalEvent(@NonNull LocalEvent event);

    /**
     * Check for conflicting events in time range.
     *
     * @param startTime Conflict check start time
     * @param endTime Conflict check end time
     * @param excludeEventId Optional event ID to exclude from conflict check
     * @return CompletableFuture with OperationResult containing list of conflicting events
     */
    @NonNull
    CompletableFuture<OperationResult<List<LocalEvent>>> findConflictingEvents(
            @NonNull LocalDateTime startTime, @NonNull LocalDateTime endTime, @Nullable String excludeEventId);
}