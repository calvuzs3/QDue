package net.calvuz.qdue.ui.features.events.local.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.domain.events.models.LocalEvent;
import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LocalEvents ViewModel
 *
 * <p>Main ViewModel for LocalEvents management that orchestrates business operations
 * through LocalEventsService while managing UI state. This ViewModel handles the
 * primary events list, filtering, searching, and coordination with other ViewModels.</p>
 *
 * <h3>Managed State:</h3>
 * <ul>
 *   <li><strong>events</strong>: List of all LocalEvents</li>
 *   <li><strong>filteredEvents</strong>: Currently filtered/searched events</li>
 *   <li><strong>selectedEvents</strong>: Set of selected event IDs for batch operations</li>
 *   <li><strong>currentFilter</strong>: Active filter configuration</li>
 *   <li><strong>searchQuery</strong>: Current search query</li>
 *   <li><strong>eventsCount</strong>: Total number of events</li>
 *   <li><strong>hasEvents</strong>: Boolean indicating if any events exist</li>
 * </ul>
 *
 * <h3>Operations:</h3>
 * <ul>
 *   <li>Load and refresh events</li>
 *   <li>Create, update, delete events</li>
 *   <li>Search and filter events</li>
 *   <li>Batch operations on selected events</li>
 *   <li>Calendar view data preparation</li>
 * </ul>
 *
 * <h3>Navigation Events:</h3>
 * <ul>
 *   <li>Event detail navigation</li>
 *   <li>Event creation navigation</li>
 *   <li>Event editing navigation</li>
 * </ul>
 *
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel
 * @see net.calvuz.qdue.data.services.LocalEventsService
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsViewModel extends BaseViewModel {

    private static final String TAG = "LocalEventsViewModel";

    // ==================== STATE KEYS ====================

    public static final String STATE_EVENTS = "events";
    public static final String STATE_FILTERED_EVENTS = "filteredEvents";
    public static final String STATE_SELECTED_EVENTS = "selectedEvents";
    public static final String STATE_CURRENT_FILTER = "currentFilter";
    public static final String STATE_SEARCH_QUERY = "searchQuery";
    public static final String STATE_EVENTS_COUNT = "eventsCount";
    public static final String STATE_HAS_EVENTS = "hasEvents";
    public static final String STATE_SELECTION_MODE = "selectionMode";

    // ==================== OPERATION KEYS ====================

    public static final String OP_LOAD_EVENTS = "loadEvents";
    public static final String OP_CREATE_EVENT = "createEvent";
    public static final String OP_UPDATE_EVENT = "updateEvent";
    public static final String OP_DELETE_EVENT = "deleteEvent";
    public static final String OP_DELETE_EVENTS = "deleteEvents";
    public static final String OP_DELETE_ALL = "deleteAll";
    public static final String OP_SEARCH = "search";
    public static final String OP_FILTER = "filter";

    // ==================== DEPENDENCIES ====================

    private final LocalEventsService mLocalEventsService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param localEventsService Service for LocalEvent operations
     */
    public LocalEventsViewModel(@NonNull LocalEventsService localEventsService) {
        this.mLocalEventsService = localEventsService;
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onInitialize() {
        Log.d(TAG, "Initializing LocalEventsViewModel");

        // Initialize state
        setState(STATE_EVENTS, new ArrayList<LocalEvent>());
        setState(STATE_FILTERED_EVENTS, new ArrayList<LocalEvent>());
        setState(STATE_SELECTED_EVENTS, new HashSet<String>());
        setState(STATE_CURRENT_FILTER, new EventFilter());
        setState(STATE_SEARCH_QUERY, "");
        setState(STATE_EVENTS_COUNT, 0);
        setState(STATE_HAS_EVENTS, false);
        setState(STATE_SELECTION_MODE, false);

        // Load initial data
        loadEvents();
    }

    @Override
    protected void onCleanup() {
        Log.d(TAG, "Cleaning up LocalEventsViewModel");
        // Cleanup is handled by base class
    }

    // ==================== PUBLIC API - DATA LOADING ====================

    /**
     * Load all events from service.
     */
    public void loadEvents() {
        Log.d(TAG, "Loading all events");

        setLoading(OP_LOAD_EVENTS, true);
        clearError(OP_LOAD_EVENTS);

        mLocalEventsService.getAllEvents()
                .thenAccept(result -> {
                    setLoading(OP_LOAD_EVENTS, false);

                    if (result.isSuccess()) {
                        List<LocalEvent> events = result.getData();
                        updateEventsState(events);
                        Log.d(TAG, "Loaded " + events.size() + " events");
                    } else {
                        setError(OP_LOAD_EVENTS, result.getFirstError());
                        Log.e(TAG, "Failed to load events: " + result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_LOAD_EVENTS, false);
                    String error = "Error loading events: " + throwable.getMessage();
                    setError(OP_LOAD_EVENTS, error);
                    Log.e(TAG, error, throwable);
                    return null;
                });
    }

    /**
     * Refresh events data.
     */
    public void refreshEvents() {
        Log.d(TAG, "Refreshing events");
        loadEvents();
    }

    /**
     * Load events for specific date.
     *
     * @param date Target date
     */
    public void loadEventsForDate(@NonNull LocalDateTime date) {
        Log.d(TAG, "Loading events for date: " + date.toLocalDate());

        setLoading(OP_LOAD_EVENTS, true);
        clearError(OP_LOAD_EVENTS);

        mLocalEventsService.getEventsForDate(date)
                .thenAccept(result -> {
                    setLoading(OP_LOAD_EVENTS, false);

                    if (result.isSuccess()) {
                        List<LocalEvent> events = result.getData();
                        updateEventsState(events);
                        Log.d(TAG, "Loaded " + events.size() + " events for date");
                    } else {
                        setError(OP_LOAD_EVENTS, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_LOAD_EVENTS, false);
                    setError(OP_LOAD_EVENTS, "Error loading events for date: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Load events for date range.
     *
     * @param startDate Range start date
     * @param endDate Range end date
     */
    public void loadEventsForDateRange(@NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate) {
        Log.d(TAG, "Loading events for date range: " + startDate.toLocalDate() + " to " + endDate.toLocalDate());

        setLoading(OP_LOAD_EVENTS, true);
        clearError(OP_LOAD_EVENTS);

        mLocalEventsService.getEventsForDateRange(startDate, endDate)
                .thenAccept(result -> {
                    setLoading(OP_LOAD_EVENTS, false);

                    if (result.isSuccess()) {
                        List<LocalEvent> events = result.getData();
                        updateEventsState(events);
                        Log.d(TAG, "Loaded " + events.size() + " events for date range");
                    } else {
                        setError(OP_LOAD_EVENTS, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_LOAD_EVENTS, false);
                    setError(OP_LOAD_EVENTS, "Error loading events for date range: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Load events for specific month.
     *
     * @param yearMonth Target month
     */
    public void loadEventsForMonth(@NonNull YearMonth yearMonth) {
        Log.d(TAG, "Loading events for month: " + yearMonth);

        setLoading(OP_LOAD_EVENTS, true);
        clearError(OP_LOAD_EVENTS);

        mLocalEventsService.getEventsForMonth(yearMonth)
                .thenAccept(result -> {
                    setLoading(OP_LOAD_EVENTS, false);

                    if (result.isSuccess()) {
                        List<LocalEvent> events = result.getData();
                        updateEventsState(events);
                        Log.d(TAG, "Loaded " + events.size() + " events for month");
                    } else {
                        setError(OP_LOAD_EVENTS, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_LOAD_EVENTS, false);
                    setError(OP_LOAD_EVENTS, "Error loading events for month: " + throwable.getMessage());
                    return null;
                });
    }

    // ==================== PUBLIC API - CRUD OPERATIONS ====================

    /**
     * Create new event.
     *
     * @param event Event to create
     */
    public void createEvent(@NonNull LocalEvent event) {
        Log.d(TAG, "Creating event: " + event.getTitle());

        setLoading(OP_CREATE_EVENT, true);
        clearError(OP_CREATE_EVENT);

        mLocalEventsService.createEvent(event)
                .thenAccept(result -> {
                    setLoading(OP_CREATE_EVENT, false);

                    if (result.isSuccess()) {
                        LocalEvent createdEvent = result.getData();
                        addEventToState(createdEvent);
                        emitEvent(new UIActionEvent("SHOW_SUCCESS",
                                                    Map.of("message", "Event created: " + createdEvent.getTitle())));
                        Log.d(TAG, "Event created successfully: " + createdEvent.getID());
                    } else {
                        setError(OP_CREATE_EVENT, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_CREATE_EVENT, false);
                    setError(OP_CREATE_EVENT, "Error creating event: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Update existing event.
     *
     * @param event Event to update
     */
    public void updateEvent(@NonNull LocalEvent event) {
        Log.d(TAG, "Updating event: " + event.getID());

        setLoading(OP_UPDATE_EVENT, true);
        clearError(OP_UPDATE_EVENT);

        mLocalEventsService.updateEvent(event)
                .thenAccept(result -> {
                    setLoading(OP_UPDATE_EVENT, false);

                    if (result.isSuccess()) {
                        LocalEvent updatedEvent = result.getData();
                        updateEventInState(updatedEvent);
                        emitEvent(new UIActionEvent("SHOW_SUCCESS",
                                                    Map.of("message", "Event updated: " + updatedEvent.getTitle())));
                        Log.d(TAG, "Event updated successfully: " + updatedEvent.getID());
                    } else {
                        setError(OP_UPDATE_EVENT, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_UPDATE_EVENT, false);
                    setError(OP_UPDATE_EVENT, "Error updating event: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Delete event by ID.
     *
     * @param eventId ID of event to delete
     */
    public void deleteEvent(@NonNull String eventId) {
        Log.d(TAG, "Deleting event: " + eventId);

        setLoading(OP_DELETE_EVENT, true);
        clearError(OP_DELETE_EVENT);

        mLocalEventsService.deleteEvent(eventId)
                .thenAccept(result -> {
                    setLoading(OP_DELETE_EVENT, false);

                    if (result.isSuccess()) {
                        removeEventFromState(eventId);
                        removeFromSelection(eventId);
                        emitEvent(new UIActionEvent("SHOW_SUCCESS",
                                                    Map.of("message", "Event deleted")));
                        Log.d(TAG, "Event deleted successfully: " + eventId);
                    } else {
                        setError(OP_DELETE_EVENT, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_DELETE_EVENT, false);
                    setError(OP_DELETE_EVENT, "Error deleting event: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Delete selected events.
     */
    public void deleteSelectedEvents() {
        Set<String> selectedIds = getSelectedEventIds();
        if (selectedIds.isEmpty()) {
            emitEvent(new UIActionEvent("SHOW_ERROR",
                                        Map.of("message", "No events selected for deletion")));
            return;
        }

        Log.d(TAG, "Deleting " + selectedIds.size() + " selected events");

        setLoading(OP_DELETE_EVENTS, true);
        clearError(OP_DELETE_EVENTS);

        mLocalEventsService.deleteEvents(new ArrayList<>(selectedIds))
                .thenAccept(result -> {
                    setLoading(OP_DELETE_EVENTS, false);

                    if (result.isSuccess()) {
                        int deletedCount = result.getData();

                        // Remove deleted events from state
                        for (String eventId : selectedIds) {
                            removeEventFromState(eventId);
                        }

                        clearSelection();
                        emitEvent(new UIActionEvent("SHOW_SUCCESS",
                                                    Map.of("message", "Deleted " + deletedCount + " events")));
                        Log.d(TAG, "Deleted " + deletedCount + " events successfully");
                    } else {
                        setError(OP_DELETE_EVENTS, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_DELETE_EVENTS, false);
                    setError(OP_DELETE_EVENTS, "Error deleting events: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Delete all events.
     */
    public void deleteAllEvents() {
        Log.d(TAG, "Deleting all events");

        setLoading(OP_DELETE_ALL, true);
        clearError(OP_DELETE_ALL);

        mLocalEventsService.deleteAllEvents()
                .thenAccept(result -> {
                    setLoading(OP_DELETE_ALL, false);

                    if (result.isSuccess()) {
                        int deletedCount = result.getData();
                        updateEventsState(new ArrayList<>());
                        clearSelection();
                        emitEvent(new UIActionEvent("SHOW_SUCCESS",
                                                    Map.of("message", "Deleted all " + deletedCount + " events")));
                        Log.d(TAG, "Deleted all " + deletedCount + " events successfully");
                    } else {
                        setError(OP_DELETE_ALL, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_DELETE_ALL, false);
                    setError(OP_DELETE_ALL, "Error deleting all events: " + throwable.getMessage());
                    return null;
                });
    }

    // ==================== PUBLIC API - SEARCH AND FILTERING ====================

    /**
     * Search events by query.
     *
     * @param query Search query
     */
    public void searchEvents(@NonNull String query) {
        Log.d(TAG, "Searching events with query: " + query);

        setState(STATE_SEARCH_QUERY, query);

        if (query.trim().isEmpty()) {
            // Clear search - show all events with current filter
            applyCurrentFilter();
            return;
        }

        if (query.trim().length() < 2) {
            setError(OP_SEARCH, "Search query must be at least 2 characters");
            return;
        }

        setLoading(OP_SEARCH, true);
        clearError(OP_SEARCH);

        mLocalEventsService.searchEvents(query.trim())
                .thenAccept(result -> {
                    setLoading(OP_SEARCH, false);

                    if (result.isSuccess()) {
                        List<LocalEvent> searchResults = result.getData();
                        setState(STATE_FILTERED_EVENTS, searchResults);
                        Log.d(TAG, "Search returned " + searchResults.size() + " events");
                    } else {
                        setError(OP_SEARCH, result.getFirstError());
                    }
                })
                .exceptionally(throwable -> {
                    setLoading(OP_SEARCH, false);
                    setError(OP_SEARCH, "Search error: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Apply event filter.
     *
     * @param filter Filter to apply
     */
    public void applyFilter(@NonNull EventFilter filter) {
        Log.d(TAG, "Applying filter: " + filter);

        setState(STATE_CURRENT_FILTER, filter);
        setState(STATE_SEARCH_QUERY, ""); // Clear search when filtering

        setLoading(OP_FILTER, true);
        clearError(OP_FILTER);

        // Get all events and apply filter locally for now
        // In the future, this could be optimized with server-side filtering
        List<LocalEvent> allEvents = getAllEvents();
        List<LocalEvent> filteredEvents = filterEventsLocally(allEvents, filter);

        setState(STATE_FILTERED_EVENTS, filteredEvents);
        setLoading(OP_FILTER, false);

        Log.d(TAG, "Filter applied: " + filteredEvents.size() + " events match");
    }

    /**
     * Clear current filter and search.
     */
    public void clearFilter() {
        Log.d(TAG, "Clearing filter and search");

        setState(STATE_CURRENT_FILTER, new EventFilter());
        setState(STATE_SEARCH_QUERY, "");
        setState(STATE_FILTERED_EVENTS, getAllEvents());
    }

    // ==================== PUBLIC API - SELECTION MANAGEMENT ====================

    /**
     * Toggle selection mode.
     */
    public void toggleSelectionMode() {
        boolean currentMode = getState(STATE_SELECTION_MODE, Boolean.class);
        boolean newMode = !currentMode;

        setState(STATE_SELECTION_MODE, newMode);

        if (!newMode) {
            clearSelection();
        }

        Log.d(TAG, "Selection mode: " + newMode);
    }

    /**
     * Toggle event selection.
     *
     * @param eventId Event ID to toggle
     */
    public void toggleEventSelection(@NonNull String eventId) {
        Set<String> selected = getSelectedEventIds();
        Set<String> newSelected = new HashSet<>(selected);

        if (newSelected.contains(eventId)) {
            newSelected.remove(eventId);
        } else {
            newSelected.add(eventId);
        }

        setState(STATE_SELECTED_EVENTS, newSelected);
        Log.d(TAG, "Event selection toggled: " + eventId + ", total selected: " + newSelected.size());
    }

    /**
     * Select all visible events.
     */
    public void selectAllEvents() {
        List<LocalEvent> filteredEvents = getFilteredEvents();
        Set<String> allIds = filteredEvents.stream()
                .map(LocalEvent::getID)
                .collect(Collectors.toSet());

        setState(STATE_SELECTED_EVENTS, allIds);
        Log.d(TAG, "Selected all " + allIds.size() + " visible events");
    }

    /**
     * Clear all selections.
     */
    public void clearSelection() {
        setState(STATE_SELECTED_EVENTS, new HashSet<String>());
        Log.d(TAG, "Cleared all selections");
    }

    // ==================== PUBLIC API - NAVIGATION ====================

    /**
     * Navigate to event detail.
     *
     * @param eventId Event ID to view
     */
    public void navigateToEventDetail(@NonNull String eventId) {
        Log.d(TAG, "Navigating to event detail: " + eventId);

        Map<String, Object> args = new HashMap<>();
        args.put("eventId", eventId);
        emitEvent(new NavigationEvent("event_detail", args));
    }

    /**
     * Navigate to event creation.
     */
    public void navigateToCreateEvent() {
        navigateToCreateEvent(null);
    }

    /**
     * Navigate to event creation with target date.
     *
     * @param targetDate Optional target date for new event
     */
    public void navigateToCreateEvent(@Nullable LocalDateTime targetDate) {
        Log.d(TAG, "Navigating to create event" + (targetDate != null ? " for date: " + targetDate.toLocalDate() : ""));

        Map<String, Object> args = new HashMap<>();
        if (targetDate != null) {
            args.put("targetDate", targetDate);
        }
        emitEvent(new NavigationEvent("create_event", args));
    }

    /**
     * Navigate to event editing.
     *
     * @param eventId Event ID to edit
     */
    public void navigateToEditEvent(@NonNull String eventId) {
        Log.d(TAG, "Navigating to edit event: " + eventId);

        Map<String, Object> args = new HashMap<>();
        args.put("eventId", eventId);
        emitEvent(new NavigationEvent("edit_event", args));
    }

    // ==================== PUBLIC API - GETTERS ====================

    /**
     * Get all events.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<LocalEvent> getAllEvents() {
        List<LocalEvent> events = getState(STATE_EVENTS, List.class);
        return events != null ? events : new ArrayList<>();
    }

    /**
     * Get filtered events (current view).
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<LocalEvent> getFilteredEvents() {
        List<LocalEvent> events = getState(STATE_FILTERED_EVENTS, List.class);
        return events != null ? events : new ArrayList<>();
    }

    /**
     * Get selected event IDs.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public Set<String> getSelectedEventIds() {
        Set<String> selected = getState(STATE_SELECTED_EVENTS, Set.class);
        return selected != null ? selected : new HashSet<>();
    }

    /**
     * Get selected events.
     */
    @NonNull
    public List<LocalEvent> getSelectedEvents() {
        Set<String> selectedIds = getSelectedEventIds();
        return getAllEvents().stream()
                .filter(event -> selectedIds.contains(event.getID()))
                .collect(Collectors.toList());
    }

    /**
     * Get current filter.
     */
    @NonNull
    public EventFilter getCurrentFilter() {
        EventFilter filter = getState(STATE_CURRENT_FILTER, EventFilter.class);
        return filter != null ? filter : new EventFilter();
    }

    /**
     * Get current search query.
     */
    @NonNull
    public String getSearchQuery() {
        String query = getState(STATE_SEARCH_QUERY, String.class);
        return query != null ? query : "";
    }

    /**
     * Get events count.
     */
    public int getEventsCount() {
        Integer count = getState(STATE_EVENTS_COUNT, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Check if has any events.
     */
    public boolean hasEvents() {
        Boolean hasEvents = getState(STATE_HAS_EVENTS, Boolean.class);
        return hasEvents != null ? hasEvents : false;
    }

    /**
     * Check if in selection mode.
     */
    public boolean isSelectionMode() {
        Boolean selectionMode = getState(STATE_SELECTION_MODE, Boolean.class);
        return selectionMode != null ? selectionMode : false;
    }

    /**
     * Check if event is selected.
     */
    public boolean isEventSelected(@NonNull String eventId) {
        return getSelectedEventIds().contains(eventId);
    }

    /**
     * Get selected events count.
     */
    public int getSelectedEventsCount() {
        return getSelectedEventIds().size();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Update events state with new list.
     */
    private void updateEventsState(@NonNull List<LocalEvent> events) {
        setState(STATE_EVENTS, new ArrayList<>(events));
        setState(STATE_EVENTS_COUNT, events.size());
        setState(STATE_HAS_EVENTS, !events.isEmpty());

        // Update filtered events if no active filter/search
        if (getCurrentFilter().isEmpty() && getSearchQuery().isEmpty()) {
            setState(STATE_FILTERED_EVENTS, new ArrayList<>(events));
        } else {
            applyCurrentFilter();
        }
    }

    /**
     * Add event to state.
     */
    private void addEventToState(@NonNull LocalEvent event) {
        List<LocalEvent> currentEvents = new ArrayList<>(getAllEvents());
        currentEvents.add(event);
        updateEventsState(currentEvents);
    }

    /**
     * Update event in state.
     */
    private void updateEventInState(@NonNull LocalEvent event) {
        List<LocalEvent> currentEvents = new ArrayList<>(getAllEvents());
        for (int i = 0; i < currentEvents.size(); i++) {
            if (event.getID().equals(currentEvents.get(i).getID())) {
                currentEvents.set(i, event);
                break;
            }
        }
        updateEventsState(currentEvents);
    }

    /**
     * Remove event from state.
     */
    private void removeEventFromState(@NonNull String eventId) {
        List<LocalEvent> currentEvents = new ArrayList<>(getAllEvents());
        currentEvents.removeIf(event -> eventId.equals(event.getID()));
        updateEventsState(currentEvents);
    }

    /**
     * Remove event from selection.
     */
    private void removeFromSelection(@NonNull String eventId) {
        Set<String> currentSelection = new HashSet<>(getSelectedEventIds());
        currentSelection.remove(eventId);
        setState(STATE_SELECTED_EVENTS, currentSelection);
    }

    /**
     * Apply current filter to all events.
     */
    private void applyCurrentFilter() {
        EventFilter filter = getCurrentFilter();
        List<LocalEvent> allEvents = getAllEvents();
        List<LocalEvent> filteredEvents = filterEventsLocally(allEvents, filter);
        setState(STATE_FILTERED_EVENTS, filteredEvents);
    }

    /**
     * Filter events locally.
     */
    private List<LocalEvent> filterEventsLocally(@NonNull List<LocalEvent> events, @NonNull EventFilter filter) {
        return events.stream()
                .filter(event -> filter.matches(event))
                .collect(Collectors.toList());
    }

    // ==================== INNER CLASSES ====================

    /**
     * Event filter configuration.
     */
    public static class EventFilter {
        private EventType eventType;
        private Priority priority;
        private String calendarId;
        private Boolean allDay;
        private LocalDateTime startDateFrom;
        private LocalDateTime startDateTo;

        public EventFilter() {
            // Default filter matches all events
        }

        public boolean isEmpty() {
            return eventType == null && priority == null && calendarId == null &&
                    allDay == null && startDateFrom == null && startDateTo == null;
        }

        public boolean matches(@NonNull LocalEvent event) {
            if (eventType != null && !eventType.equals(event.getEventType())) {
                return false;
            }

            if (priority != null && !priority.equals(event.getPriority())) {
                return false;
            }

            if (calendarId != null && !calendarId.equals(event.getCalendarId())) {
                return false;
            }

            if (allDay != null && !allDay.equals(event.isAllDay())) {
                return false;
            }

            if (startDateFrom != null && event.getStartTime().isBefore(startDateFrom)) {
                return false;
            }

            if (startDateTo != null && event.getStartTime().isAfter(startDateTo)) {
                return false;
            }

            return true;
        }

        // Getters and setters
        public EventType getEventType() { return eventType; }
        public EventFilter setEventType(EventType eventType) { this.eventType = eventType; return this; }

        public Priority getPriority() { return priority; }
        public EventFilter setPriority(Priority priority) { this.priority = priority; return this; }

        public String getCalendarId() { return calendarId; }
        public EventFilter setCalendarId(String calendarId) { this.calendarId = calendarId; return this; }

        public Boolean getAllDay() { return allDay; }
        public EventFilter setAllDay(Boolean allDay) { this.allDay = allDay; return this; }

        public LocalDateTime getStartDateFrom() { return startDateFrom; }
        public EventFilter setStartDateFrom(LocalDateTime startDateFrom) { this.startDateFrom = startDateFrom; return this; }

        public LocalDateTime getStartDateTo() { return startDateTo; }
        public EventFilter setStartDateTo(LocalDateTime startDateTo) { this.startDateTo = startDateTo; return this; }

        @Override
        public String toString() {
            return String.format("EventFilter{type=%s, priority=%s, calendar=%s, allDay=%s, dateRange=%s-%s}",
                                 eventType, priority, calendarId, allDay, startDateFrom, startDateTo);
        }
    }
}