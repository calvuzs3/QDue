package net.calvuz.qdue.core.services;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.services.models.OperationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REFACTORED: EventsService Interface - Dependency Injection Compliant
 *
 * ✅ ALL methods return OperationResult<T> for consistent error handling
 * ✅ Uniform async pattern with CompletableFuture<OperationResult<T>>
 * ✅ Complete separation from implementation details
 * ✅ Ready for dependency injection and testing
 */
public interface EventsService {

    // ==================== CORE CRUD OPERATIONS ====================

    /**
     * Create new event with automatic backup
     */
    CompletableFuture<OperationResult<LocalEvent>> createEvent(LocalEvent event);

    /**
     * Create multiple events in batch with automatic backup
     */
    CompletableFuture<OperationResult<Integer>> createEvents(List<LocalEvent> events);

    /**
     * Update existing event with automatic backup
     */
    CompletableFuture<OperationResult<LocalEvent>> updateEvent(LocalEvent event);

    /**
     * Delete event by ID with automatic backup
     */
    CompletableFuture<OperationResult<Boolean>> deleteEvent(String eventId);

    /**
     * Delete multiple events by IDs with automatic backup
     */
    CompletableFuture<OperationResult<Integer>> deleteEvents(List<String> eventIds);

    /**
     * Delete all events with automatic backup
     */
    CompletableFuture<OperationResult<Integer>> deleteAllEvents();

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get all events
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getAllEvents();

    /**
     * Get event by ID
     */
    CompletableFuture<OperationResult<LocalEvent>> getEventById(String eventId);

    /**
     * Get events for specific date
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDate(LocalDate date);

    /**
     * Get events in date range
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsInRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get events by type
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByType(EventType eventType);

    /**
     * Search events by title/description
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> searchEvents(String searchTerm);

    // ==================== BULK OPERATIONS ====================

    /**
     * Duplicate event to new date
     */
    CompletableFuture<OperationResult<LocalEvent>> duplicateEvent(String eventId, LocalDate newDate);

    /**
     * Import events from external source
     */
    CompletableFuture<OperationResult<ImportResult>> importEvents(List<LocalEvent> events, boolean replaceAll);

    /**
     * Export events to external format
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> exportEvents(LocalDate startDate, LocalDate endDate);

    // ==================== STATISTICS AND INFO ====================

    /**
     * Get events count
     */
    CompletableFuture<OperationResult<Integer>> getEventsCount();

    /**
     * Get events count by type
     */
    CompletableFuture<OperationResult<java.util.Map<EventType, Integer>>> getEventsCountByType();

    /**
     * Get next upcoming event
     */
    CompletableFuture<OperationResult<LocalEvent>> getNextUpcomingEvent(int limit);

    /**
     * Check if event exists
     */
    CompletableFuture<OperationResult<Boolean>> eventExists(String eventId);

    // ==================== VALIDATION ====================

    /**
     * Validate event data
     */
    OperationResult<Void> validateEvent(LocalEvent event);

    /**
     * Check for event conflicts (overlapping events)
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getConflictingEvents(LocalEvent event);

    // ==================== INNER CLASSES ====================

    /**
     * Import operation result
     */
    class ImportResult {
        public final int totalEvents;
        public final int importedEvents;
        public final int skippedEvents;
        public final int errorEvents;
        public final List<String> errors;

        public ImportResult(int totalEvents, int importedEvents, int skippedEvents,
                            int errorEvents, List<String> errors) {
            this.totalEvents = totalEvents;
            this.importedEvents = importedEvents;
            this.skippedEvents = skippedEvents;
            this.errorEvents = errorEvents;
            this.errors = errors;
        }
    }
}