package net.calvuz.qdue.services;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.services.models.OperationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * STEP 1: Centralized Service Interface for ALL Event Operations
 *
 * Ensures consistent backup, validation, and business logic for events.
 * All UI components (Activities, Fragments) must use this service instead
 * of direct DAO access to guarantee consistent behavior.
 *
 * Features:
 * - Automatic backup integration
 * - Consistent validation rules
 * - Centralized business logic
 * - Background operations with CompletableFuture
 * - Standardized error handling
 */
public interface EventsService {

    // ==================== CORE CRUD OPERATIONS ====================

    /**
     * Create new event with automatic backup
     * @param event Event to create
     * @return Operation result with created event
     */
    CompletableFuture<OperationResult<LocalEvent>> createEvent(LocalEvent event);

    /**
     * Create multiple events in batch with automatic backup
     * @param events List of events to create
     * @return Operation result with created events count
     */
    CompletableFuture<OperationResult<Integer>> createEvents(List<LocalEvent> events);

    /**
     * Update existing event with automatic backup
     * @param event Event to update
     * @return Operation result with updated event
     */
    CompletableFuture<OperationResult<LocalEvent>> updateEvent(LocalEvent event);

    /**
     * Delete event by ID with automatic backup
     * @param eventId ID of event to delete
     * @return Operation result with deleted event title
     */
    CompletableFuture<OperationResult<String>> deleteEvent(String eventId);

    /**
     * Delete multiple events by IDs with automatic backup
     * @param eventIds List of event IDs to delete
     * @return Operation result with deleted events count
     */
    CompletableFuture<OperationResult<Integer>> deleteEvents(List<String> eventIds);

    /**
     * Delete all events with automatic backup
     * @return Operation result with deleted events count
     */
    CompletableFuture<OperationResult<Integer>> deleteAllEvents();

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get all events
     * @return All events in database
     */
    CompletableFuture<List<LocalEvent>> getAllEvents();

    /**
     * Get event by ID
     * @param eventId Event ID
     * @return Event if found, null otherwise
     */
    CompletableFuture<LocalEvent> getEventById(String eventId);

    /**
     * Get events for specific date
     * @param date Target date
     * @return Events occurring on specified date
     */
    CompletableFuture<List<LocalEvent>> getEventsForDate(LocalDate date);

    /**
     * Get events in date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Events in specified range
     */
    CompletableFuture<List<LocalEvent>> getEventsInRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get events by type
     * @param eventType Event type filter
     * @return Events of specified type
     */
    CompletableFuture<List<LocalEvent>> getEventsByType(EventType eventType);

    /**
     * Search events by title/description
     * @param searchTerm Search term
     * @return Events matching search term
     */
    CompletableFuture<List<LocalEvent>> searchEvents(String searchTerm);

    // ==================== BULK OPERATIONS ====================

    /**
     * Duplicate event to new date
     * @param eventId Source event ID
     * @param newDate Target date for duplicate
     * @return Operation result with duplicated event
     */
    CompletableFuture<OperationResult<LocalEvent>> duplicateEvent(String eventId, LocalDate newDate);

    /**
     * Import events from external source
     * @param events Events to import
     * @param replaceAll Whether to replace all existing events
     * @return Operation result with import statistics
     */
    CompletableFuture<OperationResult<ImportResult>> importEvents(List<LocalEvent> events, boolean replaceAll);

    /**
     * Export events to external format
     * @param startDate Start date for export (null for all)
     * @param endDate End date for export (null for all)
     * @return Operation result with exported events
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> exportEvents(LocalDate startDate, LocalDate endDate);

    // ==================== STATISTICS AND INFO ====================

    /**
     * Get events count
     * @return Total number of events
     */
    CompletableFuture<Integer> getEventsCount();

    /**
     * Get events count by type
     * @return Map of event type to count
     */
    CompletableFuture<java.util.Map<EventType, Integer>> getEventsCountByType();

    /**
     * Get next upcoming event
     * @param limit Maximum number of events to return
     * @return Next event after current time, null if none
     */
    CompletableFuture<LocalEvent> getNextUpcomingEvent(int limit);

    /**
     * Check if event exists
     * @param eventId Event ID to check
     * @return True if event exists
     */
    CompletableFuture<Boolean> eventExists(String eventId);

    // ==================== VALIDATION ====================

    /**
     * Validate event data
     * @param event Event to validate
     * @return Validation result with errors if any
     */
    OperationResult<Void> validateEvent(LocalEvent event);

    /**
     * Check for event conflicts (overlapping events)
     * @param event Event to check
     * @return List of conflicting events
     */
    CompletableFuture<List<LocalEvent>> getConflictingEvents(LocalEvent event);

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