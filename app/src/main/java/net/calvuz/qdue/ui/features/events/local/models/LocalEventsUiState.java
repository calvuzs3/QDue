package net.calvuz.qdue.ui.features.events.local.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LocalEvents UI State Models
 *
 * <p>Immutable state classes for representing UI state in the LocalEvents MVVM architecture.
 * These classes provide type-safe, immutable state management that can be observed by UI
 * components and updated by ViewModels through proper state management patterns.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Immutability</strong>: All state classes are immutable for thread safety</li>
 *   <li><strong>Builder Pattern</strong>: Easy construction and modification of state</li>
 *   <li><strong>Type Safety</strong>: Strongly typed state properties</li>
 *   <li><strong>Defensive Copying</strong>: Collections are copied to prevent external modification</li>
 * </ul>
 *
 * <h3>State Classes:</h3>
 * <ul>
 *   <li>{@link LocalEventsListState} - Main events list UI state</li>
 *   <li>{@link EventSelectionState} - Selection mode state</li>
 *   <li>{@link EventFilterState} - Filtering and search state</li>
 *   <li>{@link FileOperationState} - File operations progress state</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public final class LocalEventsUiState {

    // Private constructor to prevent instantiation
    private LocalEventsUiState() {}

    // ==================== MAIN EVENTS LIST STATE ====================

    /**
     * Immutable state for the main events list view.
     */
    public static final class LocalEventsListState {
        private final List<LocalEvent> events;
        private final List<LocalEvent> filteredEvents;
        private final int totalEventsCount;
        private final boolean hasEvents;
        private final boolean isLoading;
        private final String errorMessage;
        private final long lastUpdated;

        private LocalEventsListState(@NonNull Builder builder) {
            this.events = new ArrayList<>(builder.events);
            this.filteredEvents = new ArrayList<>(builder.filteredEvents);
            this.totalEventsCount = builder.totalEventsCount;
            this.hasEvents = builder.hasEvents;
            this.isLoading = builder.isLoading;
            this.errorMessage = builder.errorMessage;
            this.lastUpdated = builder.lastUpdated;
        }

        // Getters
        @NonNull
        public List<LocalEvent> getEvents() { return new ArrayList<>(events); }

        @NonNull
        public List<LocalEvent> getFilteredEvents() { return new ArrayList<>(filteredEvents); }

        public int getTotalEventsCount() { return totalEventsCount; }

        public boolean hasEvents() { return hasEvents; }

        public boolean isLoading() { return isLoading; }

        @Nullable
        public String getErrorMessage() { return errorMessage; }

        public long getLastUpdated() { return lastUpdated; }

        public boolean hasError() { return errorMessage != null; }

        public boolean isEmpty() { return events.isEmpty(); }

        public boolean isFilterActive() { return events.size() != filteredEvents.size(); }

        // Builder method
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        @NonNull
        public Builder toBuilder() {
            return new Builder(this);
        }

        @Override
        public String toString() {
            return String.format("LocalEventsListState{events=%d, filtered=%d, hasEvents=%s, loading=%s, error=%s}",
                                 events.size(), filteredEvents.size(), hasEvents, isLoading, hasError());
        }

        public static final class Builder {
            private List<LocalEvent> events = new ArrayList<>();
            private List<LocalEvent> filteredEvents = new ArrayList<>();
            private int totalEventsCount = 0;
            private boolean hasEvents = false;
            private boolean isLoading = false;
            private String errorMessage = null;
            private long lastUpdated = System.currentTimeMillis();

            private Builder() {}

            private Builder(@NonNull LocalEventsListState state) {
                this.events = new ArrayList<>(state.events);
                this.filteredEvents = new ArrayList<>(state.filteredEvents);
                this.totalEventsCount = state.totalEventsCount;
                this.hasEvents = state.hasEvents;
                this.isLoading = state.isLoading;
                this.errorMessage = state.errorMessage;
                this.lastUpdated = state.lastUpdated;
            }

            @NonNull
            public Builder events(@NonNull List<LocalEvent> events) {
                this.events = new ArrayList<>(events);
                this.totalEventsCount = events.size();
                this.hasEvents = !events.isEmpty();
                if (filteredEvents.isEmpty()) {
                    this.filteredEvents = new ArrayList<>(events);
                }
                return this;
            }

            @NonNull
            public Builder filteredEvents(@NonNull List<LocalEvent> filteredEvents) {
                this.filteredEvents = new ArrayList<>(filteredEvents);
                return this;
            }

            @NonNull
            public Builder totalEventsCount(int count) {
                this.totalEventsCount = count;
                this.hasEvents = count > 0;
                return this;
            }

            @NonNull
            public Builder hasEvents(boolean hasEvents) {
                this.hasEvents = hasEvents;
                return this;
            }

            @NonNull
            public Builder isLoading(boolean isLoading) {
                this.isLoading = isLoading;
                return this;
            }

            @NonNull
            public Builder errorMessage(@Nullable String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            @NonNull
            public Builder clearError() {
                this.errorMessage = null;
                return this;
            }

            @NonNull
            public Builder updateTimestamp() {
                this.lastUpdated = System.currentTimeMillis();
                return this;
            }

            @NonNull
            public LocalEventsListState build() {
                return new LocalEventsListState(this);
            }
        }
    }

    // ==================== EVENT SELECTION STATE ====================

    /**
     * Immutable state for event selection mode.
     */
    public static final class EventSelectionState {
        private final boolean selectionMode;
        private final Set<String> selectedEventIds;
        private final int selectedCount;
        private final boolean hasSelection;
        private final boolean allSelected;

        private EventSelectionState(@NonNull Builder builder) {
            this.selectionMode = builder.selectionMode;
            this.selectedEventIds = new HashSet<>(builder.selectedEventIds);
            this.selectedCount = selectedEventIds.size();
            this.hasSelection = selectedCount > 0;
            this.allSelected = builder.allSelected;
        }

        // Getters
        public boolean isSelectionMode() { return selectionMode; }

        @NonNull
        public Set<String> getSelectedEventIds() { return new HashSet<>(selectedEventIds); }

        public int getSelectedCount() { return selectedCount; }

        public boolean hasSelection() { return hasSelection; }

        public boolean isAllSelected() { return allSelected; }

        public boolean isEventSelected(@NonNull String eventId) {
            return selectedEventIds.contains(eventId);
        }

        // Builder method
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        @NonNull
        public Builder toBuilder() {
            return new Builder(this);
        }

        @Override
        public String toString() {
            return String.format("EventSelectionState{mode=%s, selected=%d, hasSelection=%s, allSelected=%s}",
                                 selectionMode, selectedCount, hasSelection, allSelected);
        }

        public static final class Builder {
            private boolean selectionMode = false;
            private Set<String> selectedEventIds = new HashSet<>();
            private boolean allSelected = false;

            private Builder() {}

            private Builder(@NonNull EventSelectionState state) {
                this.selectionMode = state.selectionMode;
                this.selectedEventIds = new HashSet<>(state.selectedEventIds);
                this.allSelected = state.allSelected;
            }

            @NonNull
            public Builder selectionMode(boolean selectionMode) {
                this.selectionMode = selectionMode;
                if (!selectionMode) {
                    this.selectedEventIds.clear();
                    this.allSelected = false;
                }
                return this;
            }

            @NonNull
            public Builder selectedEventIds(@NonNull Set<String> selectedEventIds) {
                this.selectedEventIds = new HashSet<>(selectedEventIds);
                return this;
            }

            @NonNull
            public Builder addSelectedEvent(@NonNull String eventId) {
                this.selectedEventIds.add(eventId);
                return this;
            }

            @NonNull
            public Builder removeSelectedEvent(@NonNull String eventId) {
                this.selectedEventIds.remove(eventId);
                this.allSelected = false;
                return this;
            }

            @NonNull
            public Builder clearSelection() {
                this.selectedEventIds.clear();
                this.allSelected = false;
                return this;
            }

            @NonNull
            public Builder allSelected(boolean allSelected) {
                this.allSelected = allSelected;
                return this;
            }

            @NonNull
            public EventSelectionState build() {
                return new EventSelectionState(this);
            }
        }
    }

    // ==================== EVENT FILTER STATE ====================

    /**
     * Immutable state for event filtering and search.
     */
    public static final class EventFilterState {
        private final String searchQuery;
        private final EventType filterEventType;
        private final Priority filterPriority;
        private final String filterCalendarId;
        private final Boolean filterAllDay;
        private final LocalDateTime filterDateFrom;
        private final LocalDateTime filterDateTo;
        private final boolean hasActiveFilter;
        private final boolean hasActiveSearch;

        private EventFilterState(@NonNull Builder builder) {
            this.searchQuery = builder.searchQuery;
            this.filterEventType = builder.filterEventType;
            this.filterPriority = builder.filterPriority;
            this.filterCalendarId = builder.filterCalendarId;
            this.filterAllDay = builder.filterAllDay;
            this.filterDateFrom = builder.filterDateFrom;
            this.filterDateTo = builder.filterDateTo;
            this.hasActiveSearch = searchQuery != null && !searchQuery.trim().isEmpty();
            this.hasActiveFilter = filterEventType != null || filterPriority != null ||
                    filterCalendarId != null || filterAllDay != null ||
                    filterDateFrom != null || filterDateTo != null;
        }

        // Getters
        @NonNull
        public String getSearchQuery() { return searchQuery != null ? searchQuery : ""; }

        @Nullable
        public EventType getFilterEventType() { return filterEventType; }

        @Nullable
        public Priority getFilterPriority() { return filterPriority; }

        @Nullable
        public String getFilterCalendarId() { return filterCalendarId; }

        @Nullable
        public Boolean getFilterAllDay() { return filterAllDay; }

        @Nullable
        public LocalDateTime getFilterDateFrom() { return filterDateFrom; }

        @Nullable
        public LocalDateTime getFilterDateTo() { return filterDateTo; }

        public boolean hasActiveFilter() { return hasActiveFilter; }

        public boolean hasActiveSearch() { return hasActiveSearch; }

        public boolean hasAnyFilter() { return hasActiveFilter || hasActiveSearch; }

        public boolean isEmpty() { return !hasAnyFilter(); }

        // Builder method
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        @NonNull
        public Builder toBuilder() {
            return new Builder(this);
        }

        @Override
        public String toString() {
            return String.format("EventFilterState{search='%s', type=%s, priority=%s, calendar=%s, " +
                                         "allDay=%s, dateRange=%s-%s, hasFilter=%s, hasSearch=%s}",
                                 searchQuery, filterEventType, filterPriority, filterCalendarId,
                                 filterAllDay, filterDateFrom, filterDateTo, hasActiveFilter, hasActiveSearch);
        }

        public static final class Builder {
            private String searchQuery = "";
            private EventType filterEventType = null;
            private Priority filterPriority = null;
            private String filterCalendarId = null;
            private Boolean filterAllDay = null;
            private LocalDateTime filterDateFrom = null;
            private LocalDateTime filterDateTo = null;

            private Builder() {}

            private Builder(@NonNull EventFilterState state) {
                this.searchQuery = state.searchQuery;
                this.filterEventType = state.filterEventType;
                this.filterPriority = state.filterPriority;
                this.filterCalendarId = state.filterCalendarId;
                this.filterAllDay = state.filterAllDay;
                this.filterDateFrom = state.filterDateFrom;
                this.filterDateTo = state.filterDateTo;
            }

            @NonNull
            public Builder searchQuery(@Nullable String searchQuery) {
                this.searchQuery = searchQuery != null ? searchQuery : "";
                return this;
            }

            @NonNull
            public Builder filterEventType(@Nullable EventType filterEventType) {
                this.filterEventType = filterEventType;
                return this;
            }

            @NonNull
            public Builder filterPriority(@Nullable Priority filterPriority) {
                this.filterPriority = filterPriority;
                return this;
            }

            @NonNull
            public Builder filterCalendarId(@Nullable String filterCalendarId) {
                this.filterCalendarId = filterCalendarId;
                return this;
            }

            @NonNull
            public Builder filterAllDay(@Nullable Boolean filterAllDay) {
                this.filterAllDay = filterAllDay;
                return this;
            }

            @NonNull
            public Builder filterDateRange(@Nullable LocalDateTime from, @Nullable LocalDateTime to) {
                this.filterDateFrom = from;
                this.filterDateTo = to;
                return this;
            }

            @NonNull
            public Builder clearFilters() {
                this.filterEventType = null;
                this.filterPriority = null;
                this.filterCalendarId = null;
                this.filterAllDay = null;
                this.filterDateFrom = null;
                this.filterDateTo = null;
                return this;
            }

            @NonNull
            public Builder clearSearch() {
                this.searchQuery = "";
                return this;
            }

            @NonNull
            public Builder clearAll() {
                clearFilters();
                clearSearch();
                return this;
            }

            @NonNull
            public EventFilterState build() {
                return new EventFilterState(this);
            }
        }
    }

    // ==================== FILE OPERATION STATE ====================

    /**
     * Immutable state for file operations progress.
     */
    public static final class FileOperationState {
        private final OperationType operationType;
        private final boolean isActive;
        private final int processedItems;
        private final int totalItems;
        private final String currentItem;
        private final String statusMessage;
        private final String errorMessage;
        private final long startTime;
        private final long estimatedTimeRemaining;

        private FileOperationState(@NonNull Builder builder) {
            this.operationType = builder.operationType;
            this.isActive = builder.isActive;
            this.processedItems = builder.processedItems;
            this.totalItems = builder.totalItems;
            this.currentItem = builder.currentItem;
            this.statusMessage = builder.statusMessage;
            this.errorMessage = builder.errorMessage;
            this.startTime = builder.startTime;
            this.estimatedTimeRemaining = builder.estimatedTimeRemaining;
        }

        // Getters
        @NonNull
        public OperationType getOperationType() { return operationType; }

        public boolean isActive() { return isActive; }

        public int getProcessedItems() { return processedItems; }

        public int getTotalItems() { return totalItems; }

        @NonNull
        public String getCurrentItem() { return currentItem != null ? currentItem : ""; }

        @NonNull
        public String getStatusMessage() { return statusMessage != null ? statusMessage : ""; }

        @Nullable
        public String getErrorMessage() { return errorMessage; }

        public long getStartTime() { return startTime; }

        public long getEstimatedTimeRemaining() { return estimatedTimeRemaining; }

        public boolean hasError() { return errorMessage != null; }

        public int getPercentage() {
            if (totalItems <= 0) return 0;
            return Math.round((processedItems * 100.0f) / totalItems);
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        public boolean isCompleted() {
            return !isActive && processedItems >= totalItems && totalItems > 0;
        }

        // Builder method
        @NonNull
        public static Builder builder(@NonNull OperationType operationType) {
            return new Builder(operationType);
        }

        @NonNull
        public Builder toBuilder() {
            return new Builder(this);
        }

        @Override
        public String toString() {
            return String.format("FileOperationState{type=%s, active=%s, progress=%d/%d (%d%%), " +
                                         "currentItem='%s', hasError=%s}",
                                 operationType, isActive, processedItems, totalItems, getPercentage(),
                                 currentItem, hasError());
        }

        public enum OperationType {
            IMPORT, EXPORT, VALIDATION, NONE
        }

        public static final class Builder {
            private final OperationType operationType;
            private boolean isActive = false;
            private int processedItems = 0;
            private int totalItems = 0;
            private String currentItem = "";
            private String statusMessage = "";
            private String errorMessage = null;
            private long startTime = System.currentTimeMillis();
            private long estimatedTimeRemaining = 0;

            private Builder(@NonNull OperationType operationType) {
                this.operationType = operationType;
            }

            private Builder(@NonNull FileOperationState state) {
                this.operationType = state.operationType;
                this.isActive = state.isActive;
                this.processedItems = state.processedItems;
                this.totalItems = state.totalItems;
                this.currentItem = state.currentItem;
                this.statusMessage = state.statusMessage;
                this.errorMessage = state.errorMessage;
                this.startTime = state.startTime;
                this.estimatedTimeRemaining = state.estimatedTimeRemaining;
            }

            @NonNull
            public Builder isActive(boolean isActive) {
                this.isActive = isActive;
                if (isActive && startTime == 0) {
                    this.startTime = System.currentTimeMillis();
                }
                return this;
            }

            @NonNull
            public Builder progress(int processedItems, int totalItems) {
                this.processedItems = Math.max(0, processedItems);
                this.totalItems = Math.max(0, totalItems);
                return this;
            }

            @NonNull
            public Builder currentItem(@Nullable String currentItem) {
                this.currentItem = currentItem != null ? currentItem : "";
                return this;
            }

            @NonNull
            public Builder statusMessage(@Nullable String statusMessage) {
                this.statusMessage = statusMessage != null ? statusMessage : "";
                return this;
            }

            @NonNull
            public Builder errorMessage(@Nullable String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            @NonNull
            public Builder clearError() {
                this.errorMessage = null;
                return this;
            }

            @NonNull
            public Builder estimatedTimeRemaining(long estimatedTimeRemaining) {
                this.estimatedTimeRemaining = Math.max(0, estimatedTimeRemaining);
                return this;
            }

            @NonNull
            public Builder startOperation() {
                this.isActive = true;
                this.startTime = System.currentTimeMillis();
                this.errorMessage = null;
                return this;
            }

            @NonNull
            public Builder completeOperation() {
                this.isActive = false;
                this.estimatedTimeRemaining = 0;
                return this;
            }

            @NonNull
            public Builder resetOperation() {
                this.isActive = false;
                this.processedItems = 0;
                this.totalItems = 0;
                this.currentItem = "";
                this.statusMessage = "";
                this.errorMessage = null;
                this.startTime = System.currentTimeMillis();
                this.estimatedTimeRemaining = 0;
                return this;
            }

            @NonNull
            public FileOperationState build() {
                return new FileOperationState(this);
            }
        }
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create empty events list state.
     */
    @NonNull
    public static LocalEventsListState emptyEventsListState() {
        return LocalEventsListState.builder().build();
    }

    /**
     * Create loading events list state.
     */
    @NonNull
    public static LocalEventsListState loadingEventsListState() {
        return LocalEventsListState.builder()
                .isLoading(true)
                .build();
    }

    /**
     * Create error events list state.
     */
    @NonNull
    public static LocalEventsListState errorEventsListState(@NonNull String errorMessage) {
        return LocalEventsListState.builder()
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create empty selection state.
     */
    @NonNull
    public static EventSelectionState emptySelectionState() {
        return EventSelectionState.builder().build();
    }

    /**
     * Create empty filter state.
     */
    @NonNull
    public static EventFilterState emptyFilterState() {
        return EventFilterState.builder().build();
    }

    /**
     * Create inactive file operation state.
     */
    @NonNull
    public static FileOperationState inactiveFileOperationState() {
        return FileOperationState.builder(FileOperationState.OperationType.NONE).build();
    }

    // ==================== UTILITY CLASSES ====================

    /**
     * Utility class for state operations.
     */
    public static final class StateUtils {

        private StateUtils() {}

        /**
         * Check if events list state represents a successful load with data.
         */
        public static boolean isSuccessfulLoadWithData(@NonNull LocalEventsListState state) {
            return !state.isLoading() && !state.hasError() && state.hasEvents();
        }

        /**
         * Check if events list state represents an empty result.
         */
        public static boolean isEmptyResult(@NonNull LocalEventsListState state) {
            return !state.isLoading() && !state.hasError() && !state.hasEvents();
        }

        /**
         * Check if selection state allows bulk operations.
         */
        public static boolean canPerformBulkOperations(@NonNull EventSelectionState state) {
            return state.isSelectionMode() && state.hasSelection();
        }

        /**
         * Check if filter state has meaningful filters applied.
         */
        public static boolean hasMeaningfulFilters(@NonNull EventFilterState state) {
            return state.hasActiveFilter() || (state.hasActiveSearch() && state.getSearchQuery().length() >= 2);
        }

        /**
         * Check if file operation state indicates active operation.
         */
        public static boolean isFileOperationInProgress(@NonNull FileOperationState state) {
            return state.isActive() && state.getOperationType() != FileOperationState.OperationType.NONE;
        }
    }
}