package net.calvuz.qdue.core.services;

import net.calvuz.qdue.core.services.models.QuickEventRequest;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * STEP 1: EventsService Interface - Enhanced with Quick Events
 * <p>
 * Extended EventsService interface with quick event creation capabilities.
 * Maintains all existing functionality while adding new quick event methods
 * that integrate seamlessly with the DI architecture.
 * <p>
 * ✅ ALL methods return OperationResult<T> for consistent error handling
 * ✅ Uniform async pattern with CompletableFuture<OperationResult<T>>
 * ✅ Complete separation from implementation details
 * ✅ Ready for dependency injection and testing
 * ✅ NEW: Quick event creation and validation methods
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
     * Get events for date range
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get upcoming events
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEvents(int limit);

    /**
     * Get next upcoming event
     */
    CompletableFuture<OperationResult<LocalEvent>> getNextUpcomingEvent(int limit);

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

    // ==================== 🆕 NEW: QUICK EVENT OPERATIONS ====================

    /**
     * Create quick event from request with automatic validation and backup
     *
     * @param request QuickEventRequest containing all event data
     * @return CompletableFuture with OperationResult containing created LocalEvent
     */
    CompletableFuture<OperationResult<LocalEvent>> createQuickEvent(QuickEventRequest request);

    /**
     * Create multiple quick events from requests with automatic validation and backup
     *
     * @param requests List of QuickEventRequest objects
     * @return CompletableFuture with OperationResult containing list of created LocalEvents
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> createQuickEvents(List<QuickEventRequest> requests);

    /**
     * Create quick events for multiple dates using the same template
     *
     * @param baseRequest Base request template
     * @param dates       List of dates to create events for
     * @return CompletableFuture with OperationResult containing list of created LocalEvents
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> createQuickEventsForDates(QuickEventRequest baseRequest, List<LocalDate> dates);

    // ==================== 🆕 NEW: QUICK EVENT VALIDATION ====================

    /**
     * Validate quick event request before creation
     *
     * @param request QuickEventRequest to validate
     * @return OperationResult with validation results
     */
    OperationResult<Void> validateQuickEventRequest(QuickEventRequest request);

    /**
     * Validate multiple quick event requests
     *
     * @param requests List of QuickEventRequest objects to validate
     * @return OperationResult with validation results and error details
     */
    OperationResult<QuickEventValidationResult> validateQuickEventRequests(List<QuickEventRequest> requests);

    // ==================== 🆕 NEW: QUICK EVENT AVAILABILITY ====================

    /**
     * Check if quick event can be created for specific date and action
     *
     * @param action ToolbarAction representing the quick event type
     * @param date   Date to check availability for
     * @param userId User ID (can be null)
     * @return CompletableFuture with OperationResult indicating availability
     */
    CompletableFuture<OperationResult<Boolean>> canCreateQuickEvent(ToolbarAction action, LocalDate date, Long userId);

    /**
     * Check availability for multiple dates
     *
     * @param action ToolbarAction representing the quick event type
     * @param dates  List of dates to check
     * @param userId User ID (can be null)
     * @return CompletableFuture with OperationResult containing availability map
     */
    CompletableFuture<OperationResult<java.util.Map<LocalDate, Boolean>>> canCreateQuickEventsForDates(ToolbarAction action, List<LocalDate> dates, Long userId);

    // ==================== 🆕 NEW: QUICK EVENT BUSINESS LOGIC ====================

    /**
     * Get default quick event configuration for a toolbar action
     *
     * @param action ToolbarAction to get configuration for
     * @param date   Date context for the event
     * @param userId User ID (can be null)
     * @return OperationResult with QuickEventRequest template
     */
    OperationResult<QuickEventRequest> getDefaultQuickEventRequest(ToolbarAction action, LocalDate date, Long userId);

    /**
     * Check for conflicts with existing events when creating quick event
     *
     * @param request QuickEventRequest to check for conflicts
     * @return CompletableFuture with OperationResult containing list of conflicting events
     */
    CompletableFuture<OperationResult<List<LocalEvent>>> getQuickEventConflicts(QuickEventRequest request);

    /**
     * Get suggested time slots for quick event creation
     *
     * @param action ToolbarAction representing the quick event type
     * @param date   Date to get suggestions for
     * @param userId User ID (can be null)
     * @return CompletableFuture with OperationResult containing list of suggested time slots
     */
    CompletableFuture<OperationResult<List<SuggestedTimeSlot>>> getSuggestedTimeSlots(ToolbarAction action, LocalDate date, Long userId);

    // ==================== 🆕 NEW: QUICK EVENT STATISTICS ====================

    /**
     * Get quick event statistics for a user
     *
     * @param userId    User ID (can be null for all users)
     * @param startDate Start date for statistics (can be null for all time)
     * @param endDate   End date for statistics (can be null for all time)
     * @return CompletableFuture with OperationResult containing statistics
     */
    CompletableFuture<OperationResult<QuickEventStatistics>> getQuickEventStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get most used quick event types
     *
     * @param userId User ID (can be null for all users)
     * @param limit  Maximum number of results
     * @return CompletableFuture with OperationResult containing map of actions to usage count
     */
    CompletableFuture<OperationResult<java.util.Map<ToolbarAction, Integer>>> getMostUsedQuickEventTypes(Long userId, int limit);

    // ==================== 🆕 NEW: INNER CLASSES FOR QUICK EVENTS ====================

    /**
     * Result of quick event validation with detailed error information
     */
    record QuickEventValidationResult(int totalRequests, int validRequests, int invalidRequests,
                                      List<String> validationErrors,
                                      java.util.Map<QuickEventRequest, List<String>> requestErrors) {

        public boolean isValid() {
            return invalidRequests == 0;
        }

        public boolean hasErrors() {
            return !validationErrors.isEmpty();
        }
        public int getTotalRequests() { return totalRequests; }
        public int getValidRequests() { return validRequests; }
        public int getInvalidRequests() { return invalidRequests; }
    }

    /**
     * Suggested time slot for quick event creation
     *
     * @param priority Higher priority = better suggestion
     */
    record SuggestedTimeSlot(java.time.LocalTime startTime, java.time.LocalTime endTime,
                             String description, int priority, boolean isAvailable) {

        public long getDurationMinutes() {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Statistics for quick event usage
     */
    record QuickEventStatistics(int totalQuickEvents,
                                java.util.Map<ToolbarAction, Integer> eventsByAction,
                                java.util.Map<EventType, Integer> eventsByType,
                                java.util.Map<LocalDate, Integer> eventsByDate,
                                LocalDate mostActiveDate, ToolbarAction mostUsedAction,
                                double averageEventsPerDay) {

        public boolean hasData() {
            return totalQuickEvents > 0;
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Import operation result
     */
    record ImportResult(int totalEvents, int importedEvents, int skippedEvents, int errorEvents,
                        List<String> errors) {
    }
}