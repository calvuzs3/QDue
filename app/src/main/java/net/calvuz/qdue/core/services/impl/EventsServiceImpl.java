package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.models.QuickEventRequest;
import net.calvuz.qdue.events.actions.ConflictAnalysis;
import net.calvuz.qdue.events.actions.EventAction;
import net.calvuz.qdue.events.actions.EventActionManager;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.metadata.EventMetadataManager;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.quickevents.QuickEventLogicAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: EventsServiceImpl - Dependency Injection Compliant
 * <p>
 * Centralized implementation for all Event operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 * - Google Calendar-like event management
 * <p>
 * Fully compliant with EventsService interface contract.
 */
public class EventsServiceImpl implements EventsService {

    private static final String TAG = "EventsServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final EventDao mEventDao;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection
     *
     * @param context       Application context
     * @param database      QDue database instance
     * @param backupManager Core backup manager instance
     */
    public EventsServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mEventDao = database.eventDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(4);

        Log.d(TAG, "EventsServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public EventsServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

// ==================== CORE CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> createEvent(LocalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate event input
                OperationResult<Void> validation = validateEvent(event);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Generate ID if not provided
                if (event.getId() == null || event.getId().trim().isEmpty()) {
                    event.setId(UUID.randomUUID().toString());
                }

                // Set creation/update timestamps
                event.setLastUpdated(LocalDateTime.now());

                // Insert event
                long result = mEventDao.insertEvent(event);
                if (result == 0) {
                    return OperationResult.failure("Failed to create event in database",
                            OperationResult.OperationType.CREATE);
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "create");

                Log.d(TAG, "Event created successfully: " + event.getTitle());
                return OperationResult.success(event, "Event created successfully",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> createEvents(List<LocalEvent> events) {
        // Make parameter effectively final for lambda
        final List<LocalEvent> finalEvents = events != null ? events : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalEvents.isEmpty()) {
                    return OperationResult.failure("No events provided for creation",
                            OperationResult.OperationType.BULK_CREATE);
                }

                List<String> validationErrors = new ArrayList<>();
                List<LocalEvent> validEvents = new ArrayList<>();

                // Validate all events first
                for (int i = 0; i < finalEvents.size(); i++) {
                    LocalEvent event = finalEvents.get(i);
                    OperationResult<Void> validation = validateEvent(event);

                    if (validation.isFailure()) {
                        validationErrors.add("Event " + (i + 1) + ": " + validation.getFirstError());
                    } else {
                        // Generate ID if not provided
                        if (event.getId() == null || event.getId().trim().isEmpty()) {
                            event.setId(UUID.randomUUID().toString());
                        }
                        event.setLastUpdated(LocalDateTime.now());
                        validEvents.add(event);
                    }
                }

                if (!validationErrors.isEmpty()) {
                    return OperationResult.failure(validationErrors, OperationResult.OperationType.BULK_CREATE);
                }

                // Insert all valid events
                long[] results = mEventDao.insertEvents(validEvents);
                if (results.length != validEvents.size()) {
                    return OperationResult.failure("Failed to create all events in database",
                            OperationResult.OperationType.BULK_CREATE);
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "bulk_create");

                Log.d(TAG, "Created " + validEvents.size() + " events successfully");
                return OperationResult.success(validEvents.size(),
                        "Created " + validEvents.size() + " events successfully",
                        OperationResult.OperationType.BULK_CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> updateEvent(LocalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate event input
                OperationResult<Void> validation = validateEvent(event);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check if event exists
                LocalEvent existingEvent = mEventDao.getEventById(event.getId());
                if (existingEvent == null) {
                    return OperationResult.failure("Event not found: " + event.getId(),
                            OperationResult.OperationType.UPDATE);
                }

                // Set update timestamp
                event.setLastUpdated(LocalDateTime.now());

                // Update event
                int result = mEventDao.updateEvent(event);
                if (result == 0) {
                    return OperationResult.failure("Failed to update event in database",
                            OperationResult.OperationType.UPDATE);
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "update");

                Log.d(TAG, "Event updated successfully: " + event.getTitle());
                return OperationResult.success(event, "Event updated successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> deleteEvent(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventId == null || eventId.trim().isEmpty()) {
                    return OperationResult.failure("Event ID cannot be null or empty",
                            OperationResult.OperationType.DELETE);
                }

                // Get event before deletion for logging
                LocalEvent event = mEventDao.getEventById(eventId);
                if (event == null) {
                    return OperationResult.failure("Event not found: " + eventId,
                            OperationResult.OperationType.DELETE);
                }

                String eventTitle = event.getTitle();

                // Delete event
                int deletedRows = mEventDao.deleteEventById(eventId);
                if (deletedRows == 0) {
                    return OperationResult.failure("Failed to delete event from database",
                            OperationResult.OperationType.DELETE);
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "delete");

                Log.d(TAG, "Event deleted successfully: " + eventTitle);
                return OperationResult.success(true, "Event deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteEvents(List<String> eventIds) {
        // Make parameter effectively final for lambda
        final List<String> finalEventIds = eventIds != null ? eventIds : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalEventIds.isEmpty()) {
                    return OperationResult.failure("No event IDs provided for deletion",
                            OperationResult.OperationType.BULK_DELETE);
                }

                int deletedCount = 0;
                List<String> errors = new ArrayList<>();

                for (String eventId : finalEventIds) {
                    try {
                        LocalEvent event = mEventDao.getEventById(eventId);
                        if (event != null) {
                            int deleted = mEventDao.deleteEventById(eventId);
                            if (deleted > 0) {
                                deletedCount++;
                            } else {
                                errors.add("Failed to delete event: " + eventId);
                            }
                        } else {
                            errors.add("Event not found: " + eventId);
                        }
                    } catch (Exception e) {
                        errors.add("Failed to delete event " + eventId + ": " + e.getMessage());
                    }
                }

                // Trigger automatic backup
                if (deletedCount > 0) {
                    mBackupManager.performAutoBackup("events", "bulk_delete");
                }

                if (!errors.isEmpty() && deletedCount == 0) {
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_DELETE);
                } else if (!errors.isEmpty()) {
                    Log.w(TAG, "Bulk delete completed with " + deletedCount + " deletions and " + errors.size() + " errors");
                    return OperationResult.success(deletedCount,
                            "Partially deleted " + deletedCount + " events (" + errors.size() + " failed)",
                            OperationResult.OperationType.BULK_DELETE);
                } else {
                    Log.d(TAG, "Bulk deleted " + deletedCount + " events successfully");
                    return OperationResult.success(deletedCount,
                            "Deleted " + deletedCount + " events successfully",
                            OperationResult.OperationType.BULK_DELETE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get count before deletion
                int count = mEventDao.getEventsCount();

                if (count == 0) {
                    return OperationResult.success(0, "No events to delete",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete all events
                int result = mEventDao.deleteAllLocalEvents();
                if (result == 0 && count > 0) {
                    return OperationResult.failure("Failed to delete all events from database",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "delete_all");

                Log.d(TAG, "Deleted all " + count + " events successfully");
                return OperationResult.success(count, "Deleted all " + count + " events",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

// ==================== QUERY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LocalEvent> events = mEventDao.getAllEvents();
                return OperationResult.success(events,
                        "Found " + events.size() + " events",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> getEventById(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventId == null || eventId.trim().isEmpty()) {
                    return OperationResult.failure("Event ID cannot be null or empty",
                            OperationResult.OperationType.READ);
                }

                LocalEvent event = mEventDao.getEventById(eventId);
                if (event != null) {
                    return OperationResult.success(event, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Event not found: " + eventId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get event by ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDate(LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (date == null) {
                    return OperationResult.failure("Date cannot be null",
                            OperationResult.OperationType.SEARCH);
                }

                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                List<LocalEvent> events = mEventDao.getEventsForDate(startOfDay, endOfDay);

                return OperationResult.success(events,
                        "Found " + events.size() + " events for " + date,
                        OperationResult.OperationType.SEARCH);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events for date: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.SEARCH);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsForDateRange(LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (startDate == null || endDate == null) {
                    return OperationResult.failure("Start and end dates cannot be null",
                            OperationResult.OperationType.SEARCH);
                }

                if (startDate.isAfter(endDate)) {
                    return OperationResult.failure("Start date cannot be after end date",
                            OperationResult.OperationType.SEARCH);
                }

                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(23, 59, 59);
                List<LocalEvent> events = mEventDao.getEventsForDateRange(start, end);

                return OperationResult.success(events,
                        "Found " + events.size() + " events from " + startDate + " to " + endDate,
                        OperationResult.OperationType.SEARCH);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events in range: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.SEARCH);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsByType(EventType eventType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventType == null) {
                    return OperationResult.failure("Event type cannot be null",
                            OperationResult.OperationType.SEARCH);
                }

                List<LocalEvent> events = mEventDao.getEventsByType(eventType);
                return OperationResult.success(events,
                        "Found " + events.size() + " events of type " + eventType.getDisplayName(),
                        OperationResult.OperationType.SEARCH);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events by type: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.SEARCH);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<SuggestedTimeSlot>>> getSuggestedTimeSlots(ToolbarAction action, LocalDate date, Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (action == null || date == null) {
                    return OperationResult.failure("Action and date are required", OperationResult.OperationType.READ);
                }

                List<LocalEvent> existingEvents = mEventDao.getEventsForDate(date.atStartOfDay(), date.atTime(23, 59, 59));
                List<SuggestedTimeSlot> suggestions = new ArrayList<>();

                // Default working hours slots
                LocalTime[] startTimes = {LocalTime.of(5, 0), LocalTime.of(13, 0), LocalTime.of(21, 0)};
                int defaultDuration = 480; // 8 hours

                for (LocalTime startTime : startTimes) {
                    LocalTime endTime = startTime.plusMinutes(defaultDuration);
                    LocalDateTime slotStart = date.atTime(startTime);
                    LocalDateTime slotEnd = date.atTime(endTime);

                    boolean isAvailable = existingEvents.stream()
                            .noneMatch(event -> event.getStartTime().isBefore(slotEnd) && event.getEndTime().isAfter(slotStart));

                    int priority = isAvailable ? (startTime.equals(LocalTime.of(9, 0)) ? 3 : 2) : 1;

                    suggestions.add(new SuggestedTimeSlot(
                            startTime, endTime,
                            startTime + " - " + endTime,
                            priority, isAvailable
                    ));
                }

                suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

                return OperationResult.success(suggestions,
                        "Found " + suggestions.size() + " suggested time slots",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get time slots: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> searchEvents(String searchTerm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (searchTerm == null || searchTerm.trim().isEmpty()) {
                    return OperationResult.failure("Search term cannot be null or empty",
                            OperationResult.OperationType.SEARCH);
                }

                final String normalizedSearchTerm = "%" + searchTerm.trim() + "%";
                List<LocalEvent> events = mEventDao.searchEvents(normalizedSearchTerm);

                return OperationResult.success(events,
                        "Found " + events.size() + " events matching '" + searchTerm + "'",
                        OperationResult.OperationType.SEARCH);
            } catch (Exception e) {
                Log.e(TAG, "Failed to search events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.SEARCH);
            }
        }, mExecutorService);
    }

// ==================== BULK OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> duplicateEvent(String eventId, LocalDate newDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventId == null || eventId.trim().isEmpty()) {
                    return OperationResult.failure("Event ID cannot be null or empty",
                            OperationResult.OperationType.DUPLICATE);
                }
                if (newDate == null) {
                    return OperationResult.failure("New date cannot be null",
                            OperationResult.OperationType.DUPLICATE);
                }

                // Get original event
                LocalEvent originalEvent = mEventDao.getEventById(eventId);
                if (originalEvent == null) {
                    return OperationResult.failure("Event not found: " + eventId,
                            OperationResult.OperationType.DUPLICATE);
                }

                // Create duplicate
                LocalEvent duplicate = createDuplicateEvent(originalEvent, newDate);

                // Validate duplicate
                OperationResult<Void> validation = validateEvent(duplicate);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.DUPLICATE);
                }

                // Insert duplicate
                mEventDao.insertEvent(duplicate);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "duplicate");

                Log.d(TAG, "Event duplicated successfully: " + duplicate.getTitle());
                return OperationResult.success(duplicate, "Event duplicated successfully",
                        OperationResult.OperationType.DUPLICATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to duplicate event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DUPLICATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<ImportResult>> importEvents(List<LocalEvent> events, boolean replaceAll) {
        // Make parameters effectively final for lambda
        final List<LocalEvent> finalEvents = events != null ? events : new ArrayList<>();
        final boolean finalReplaceAll = replaceAll;

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalEvents.isEmpty()) {
                    return OperationResult.failure("No events provided for import",
                            OperationResult.OperationType.IMPORT);
                }

                int totalEvents = finalEvents.size();
                int importedEvents = 0;
                int skippedEvents = 0;
                int errorEvents = 0;
                List<String> errors = new ArrayList<>();

                // Replace all existing events if requested
                if (finalReplaceAll) {
                    mEventDao.deleteAllLocalEvents();
                    Log.d(TAG, "Cleared all existing events for import");
                }

                // Process each event
                for (int i = 0; i < finalEvents.size(); i++) {
                    LocalEvent event = finalEvents.get(i);
                    try {
                        // Validate event
                        OperationResult<Void> validation = validateEvent(event);
                        if (validation.isFailure()) {
                            errors.add("Event " + (i + 1) + " '" + event.getTitle() + "': " + validation.getFirstError());
                            errorEvents++;
                            continue;
                        }

                        // Skip if event exists and not replacing all
                        if (!finalReplaceAll && mEventDao.getEventById(event.getId()) != null) {
                            skippedEvents++;
                            continue;
                        }

                        // Set import timestamp
                        event.setLastUpdated(LocalDateTime.now());

                        // Insert event
                        mEventDao.insertEvent(event);
                        importedEvents++;

                    } catch (Exception e) {
                        errors.add("Event " + (i + 1) + " '" + event.getTitle() + "': " + e.getMessage());
                        errorEvents++;
                    }
                }

                // Trigger automatic backup
                mBackupManager.performAutoBackup("events", "import");

                // Create import result
                ImportResult result = new ImportResult(totalEvents, importedEvents, skippedEvents, errorEvents, errors);

                Log.d(TAG, String.format(QDue.getLocale(),
                        "Import completed: %d imported, %d skipped, %d errors",
                        importedEvents, skippedEvents, errorEvents));

                if (errorEvents > 0 && importedEvents == 0) {
                    return OperationResult.failure(errors, OperationResult.OperationType.IMPORT);
                } else if (errorEvents > 0) {
                    return OperationResult.success(result,
                            "Partial import: " + importedEvents + " imported, " + errorEvents + " failed",
                            OperationResult.OperationType.IMPORT);
                } else {
                    return OperationResult.success(result,
                            "Successfully imported " + importedEvents + " events",
                            OperationResult.OperationType.IMPORT);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to import events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.IMPORT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> exportEvents(LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LocalEvent> events;

                if (startDate != null && endDate != null) {
                    if (startDate.isAfter(endDate)) {
                        return OperationResult.failure("Start date cannot be after end date",
                                OperationResult.OperationType.EXPORT);
                    }

                    LocalDateTime start = startDate.atStartOfDay();
                    LocalDateTime end = endDate.atTime(23, 59, 59);
                    events = mEventDao.getEventsForDateRange(start, end);
                } else {
                    events = mEventDao.getAllEvents();
                }

                String message = startDate != null && endDate != null ?
                        "Exported " + events.size() + " events from " + startDate + " to " + endDate :
                        "Exported " + events.size() + " events";

                Log.d(TAG, message);
                return OperationResult.success(events, message, OperationResult.OperationType.EXPORT);

            } catch (Exception e) {
                Log.e(TAG, "Failed to export events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.EXPORT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getConflictingEvents(LocalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (event == null) {
                    return OperationResult.failure("Event cannot be null",
                            OperationResult.OperationType.VALIDATION);
                }

                if (event.getStartTime() == null || event.getEndTime() == null) {
                    return OperationResult.success(new ArrayList<>(),
                            "No conflicts possible without start/end times",
                            OperationResult.OperationType.VALIDATION);
                }

                // Get overlapping events in the same time range
                List<LocalEvent> overlappingEvents = mEventDao.getEventsForDateRange(
                        event.getStartTime(), event.getEndTime());

                // Remove the event itself from conflicts (for updates)
                if (event.getId() != null) {
                    overlappingEvents.removeIf(e -> e.getId().equals(event.getId()));
                }

                return OperationResult.success(overlappingEvents,
                        "Found " + overlappingEvents.size() + " potentially conflicting events",
                        OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get conflicting events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getQuickEventConflicts(QuickEventRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (request == null || request.getDate() == null) {
                    return OperationResult.failure("Request and date are required", OperationResult.OperationType.READ);
                }

                LocalDateTime startDateTime = request.isAllDay() ?
                        request.getDate().atStartOfDay() :
                        request.getDate().atTime(request.getStartTime());

                LocalDateTime endDateTime = request.isAllDay() ?
                        request.getDate().atTime(23, 59, 59) :
                        request.getDate().atTime(request.getEndTime());

                List<LocalEvent> conflictingEvents = mEventDao.getEventsForDateRange(startDateTime, endDateTime)
                        .stream()
                        .filter(event -> event.getStartTime().isBefore(endDateTime) && event.getEndTime().isAfter(startDateTime))
                        .collect(java.util.stream.Collectors.toList());

                return OperationResult.success(conflictingEvents,
                        "Found " + conflictingEvents.size() + " conflicting events",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get conflicts: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

// ==================== STATISTICS AND INFO ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getEventsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mEventDao.getEventsCount();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events count: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Map<EventType, Integer>>> getEventsCountByType() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LocalEvent> allEvents = mEventDao.getAllEvents();
                Map<EventType, Integer> counts = new HashMap<>();

                for (LocalEvent event : allEvents) {
                    EventType type = event.getEventType();
                    counts.put(type, counts.getOrDefault(type, 0) + 1);
                }

                return OperationResult.success(counts,
                        "Calculated event counts for " + counts.size() + " event types",
                        OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events count by type: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> getNextUpcomingEvent(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (limit <= 0) {
                    return OperationResult.failure("Limit must be greater than 0",
                            OperationResult.OperationType.READ);
                }

                LocalDateTime now = LocalDateTime.now();
                List<LocalEvent> upcomingEvents = mEventDao.getUpcomingEvents(now, limit);

                if (upcomingEvents.isEmpty()) {
                    return OperationResult.failure("No upcoming events found",
                            OperationResult.OperationType.READ);
                } else {
                    return OperationResult.success(upcomingEvents.get(0),
                            "Found next upcoming event",
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get next upcoming event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingEvents(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (limit <= 0) {
                    return OperationResult.failure("Limit must be greater than 0", OperationResult.OperationType.READ);
                }

                LocalDateTime now = LocalDateTime.now();
                List<LocalEvent> upcomingEvents = mEventDao.getUpcomingEvents(now, limit);

                return OperationResult.success(upcomingEvents,
                        "Found " + upcomingEvents.size() + " upcoming events",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get upcoming events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public OperationResult<QuickEventRequest> getDefaultQuickEventRequest(ToolbarAction action, LocalDate date, Long userId) {
        try {
            if (action == null || date == null) {
                return OperationResult.failure("Action and date are required", OperationResult.OperationType.READ);
            }

            EventType eventType = action.getMappedEventType();
            if (eventType == null) {
                return OperationResult.failure("Action has no mapped event type", OperationResult.OperationType.READ);
            }

            QuickEventRequest request = QuickEventRequest.builder()
                    .templateId("template_" + action.name().toLowerCase())
                    .sourceAction(action)
                    .eventType(eventType)
                    .date(date)
                    .userId(userId)
                    .displayName(action.getEventDisplayName())
                    .description("Created via quick event")
                    .priority(eventType.getDefaultPriority())
                    .allDay(getDefaultAllDayForEventType(eventType))
                    .startTime(getDefaultStartTimeForEventType(eventType))
                    .endTime(getDefaultEndTimeForEventType(eventType))
                    .customProperty("default_template", "true")
                    .build();

            return OperationResult.success(request, "Default request created", OperationResult.OperationType.READ);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create default request: " + e.getMessage(), e);
            return OperationResult.failure(e, OperationResult.OperationType.READ);
        }
    }

    @Override
    public CompletableFuture<OperationResult<QuickEventStatistics>> getQuickEventStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(1);
                LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

                List<LocalEvent> quickEvents = mEventDao.getEventsForDateRange(start, end)
                        .stream()
                        .filter(event -> "true".equals(event.getCustomProperty("quick_event")))
                        .filter(event -> userId == null ||
                                userId.toString().equals(event.getCustomProperty("creator_user_id")))
                        //.filter(event -> userId == null || userId.equals(event.getUserId()))
                        .collect(java.util.stream.Collectors.toList());

                Map<ToolbarAction, Integer> eventsByAction = quickEvents.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                event -> ToolbarAction.valueOf(event.getCustomProperty("source_action")),
                                java.util.stream.Collectors.summingInt(e -> 1)
                        ));

                Map<EventType, Integer> eventsByType = quickEvents.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                LocalEvent::getEventType,
                                java.util.stream.Collectors.summingInt(e -> 1)
                        ));

                Map<LocalDate, Integer> eventsByDate = quickEvents.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                event -> event.getStartTime().toLocalDate(),
                                java.util.stream.Collectors.summingInt(e -> 1)
                        ));

                LocalDate mostActiveDate = eventsByDate.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                ToolbarAction mostUsedAction = eventsByAction.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
                double avgEventsPerDay = daysBetween > 0 ? (double) quickEvents.size() / daysBetween : 0;

                QuickEventStatistics stats = new QuickEventStatistics(
                        quickEvents.size(), eventsByAction, eventsByType, eventsByDate,
                        mostActiveDate, mostUsedAction, avgEventsPerDay
                );

                return OperationResult.success(stats, "Statistics calculated", OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get statistics: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Map<ToolbarAction, Integer>>> getMostUsedQuickEventTypes(Long userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LocalEvent> quickEvents = mEventDao.getAllEvents()
                        .stream()
                        .filter(event -> "true".equals(event.getCustomProperty("quick_event")))
                        // TODO: getUserId() ?
                        //.filter(event -> userId == null || userId.equals(event.getUserId()))
                        .collect(java.util.stream.Collectors.toList());

                Map<ToolbarAction, Integer> actionCounts = quickEvents.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                event -> ToolbarAction.valueOf(event.getCustomProperty("source_action")),
                                java.util.stream.Collectors.summingInt(e -> 1)
                        ))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<ToolbarAction, Integer>comparingByValue().reversed())
                        .limit(limit)
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                java.util.LinkedHashMap::new
                        ));

                return OperationResult.success(actionCounts,
                        "Found " + actionCounts.size() + " most used types",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get most used types: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> eventExists(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventId == null || eventId.trim().isEmpty()) {
                    return OperationResult.failure("Event ID cannot be null or empty",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mEventDao.getEventById(eventId) != null;
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check event existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

// ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateEvent(LocalEvent event) {
        List<String> errors = new ArrayList<>();

        // Basic null check
        if (event == null) {
            errors.add("Event cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // ID validation
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            // ID can be generated automatically, so this is just a warning for existing events
            // Don't add to errors for create operations
        } else if (event.getId().length() > 255) {
            errors.add("Event ID cannot exceed 255 characters");
        }

        // Title validation
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            errors.add("Event title is required");
        } else if (event.getTitle().length() > 100) {
            errors.add("Event title cannot exceed 100 characters");
        }

        // Time validation
        if (event.getStartTime() == null) {
            errors.add("Event start time is required");
        }
        if (event.getEndTime() == null) {
            errors.add("Event end time is required");
        }

        // Start/End time logic validation
        if (event.getStartTime() != null && event.getEndTime() != null) {
            if (event.getStartTime().isAfter(event.getEndTime())) {
                errors.add("Start time cannot be after end time");
            }
            if (event.getStartTime().isEqual(event.getEndTime()) && !event.isAllDay()) {
                errors.add("Start time cannot be equal to end time for non-all-day events");
            }
        }

        // Description validation (optional but limited)
        if (event.getDescription() != null && event.getDescription().length() > 1000) {
            errors.add("Event description cannot exceed 1000 characters");
        }

        // Location validation (optional but limited)
        if (event.getLocation() != null && event.getLocation().length() > 255) {
            errors.add("Event location cannot exceed 255 characters");
        }

        // Event type validation
        if (event.getEventType() == null) {
            errors.add("Event type is required");
        }

        // Priority validation
        if (event.getPriority() == null) {
            errors.add("Event priority is required");
        }

        // Package validation (if external event)
        if (event.getPackageId() != null && event.getPackageId().length() > 100) {
            errors.add("Package ID cannot exceed 100 characters");
        }
        if (event.getSourceUrl() != null && event.getSourceUrl().length() > 500) {
            errors.add("Source URL cannot exceed 500 characters");
        }
        if (event.getPackageVersion() != null && event.getPackageVersion().length() > 50) {
            errors.add("Package version cannot exceed 50 characters");
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    // ==================== 🆕 NEW: QUICK EVENT OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> createQuickEvent(QuickEventRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating quick event: " + request.getDisplayName() + " for " + request.getDate());

                // ✅ Validate request
                OperationResult<Void> validation = validateQuickEventRequest(request);
                if (!validation.isSuccess()) {
                    Log.w(TAG, "Quick event request validation failed: " + validation.getFormattedErrorMessage());
                    return OperationResult.failure(validation.getFormattedErrorMessage(), OperationResult.OperationType.CREATE);
                }

                // ✅ Additional Conflict validation
                EventAction eventAction = ToolbarActionBridge.mapToEventAction(request.getSourceAction());
                ConflictAnalysis conflicts = EventActionManager.analyzeConflicts(
                        eventAction, request.getDate(), request.getUserId(), mEventDao); // ← CORREZIONE: usa mEventDao

                if (conflicts.hasConflicts()) {
                    String message = conflicts.getConflictSummary();
                    return OperationResult.failure(message, OperationResult.OperationType.CREATE);
                }



                // ✅ Create LocalEvent from request
                LocalEvent event = createEventFromRequest(request);

                // ✅ Additional business validation
                OperationResult<Void> eventValidation = validateEvent(event);
                if (!eventValidation.isSuccess()) {
                    Log.w(TAG, "Event validation failed: " + eventValidation.getFormattedErrorMessage());
                    return OperationResult.failure(eventValidation.getFormattedErrorMessage(), OperationResult.OperationType.CREATE);
                }

                // ✅ Save to database
                Long eventId = mEventDao.insertEvent(event);
                event.setId(String.valueOf(eventId));

                // ✅ Trigger automatic backup
                triggerBackupAfterCreation(event);

                Log.d(TAG, "Quick event created successfully: " + event.getId());
                return OperationResult.success(event, "Quick event created successfully", OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create quick event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> createQuickEvents(List<QuickEventRequest> requests) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating " + requests.size() + " quick events");

                if (requests.isEmpty()) {
                    return OperationResult.success(new ArrayList<>(), "No events to create", OperationResult.OperationType.BULK_CREATE);
                }

                // ✅ Validate all requests
                OperationResult<QuickEventValidationResult> validation = validateQuickEventRequests(requests);
                if (!validation.isSuccess()) {
                    Log.w(TAG, "Quick events validation failed: " + validation.getFormattedErrorMessage());
                    return OperationResult.failure(validation.getFormattedErrorMessage(), OperationResult.OperationType.BULK_CREATE);
                }

                QuickEventValidationResult validationResult = validation.getData();
                if (!validationResult.isValid()) {
                    String errorMessage = "Validation failed for " + validationResult.getInvalidRequests() + " requests";
                    Log.w(TAG, errorMessage);
                    return OperationResult.failure(errorMessage, OperationResult.OperationType.BULK_CREATE);
                }


                // TODO: add conflict validations
                // ✅ Additional Conflict validation


                // ✅ Create events from requests
                List<LocalEvent> createdEvents = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                for (QuickEventRequest request : requests) {
                    try {
                        LocalEvent event = createEventFromRequest(request);
                        long eventId = mEventDao.insertEvent(event);
                        event.setId(String.valueOf(eventId));
                        createdEvents.add(event);

                        Log.v(TAG, "Created quick event: " + event.getId());

                    } catch (Exception e) {
                        String error = "Failed to create event for " + request.getDate() + ": " + e.getMessage();
                        errors.add(error);
                        Log.e(TAG, error, e);
                    }
                }

                // ✅ Trigger backup for all created events
                if (!createdEvents.isEmpty()) {
                    triggerBackupAfterBulkCreation(createdEvents);
                }

                if (errors.isEmpty()) {
                    Log.d(TAG, "All " + createdEvents.size() + " quick events created successfully");
                    return OperationResult.success(createdEvents, "Created " + createdEvents.size() + " quick events", OperationResult.OperationType.BULK_CREATE);
                } else {
                    String message = "Created " + createdEvents.size() + " events, " + errors.size() + " failed";
                    Log.w(TAG, message);
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to create quick events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<LocalEvent>>> createQuickEventsForDates(QuickEventRequest baseRequest, List<LocalDate> dates) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating quick events for " + dates.size() + " dates using template: " + baseRequest.getTemplateId());

                if (dates.isEmpty()) {
                    return OperationResult.success(new ArrayList<>(), "No dates provided", OperationResult.OperationType.BULK_CREATE);
                }

                // TODO: update as above
                // ✅ Create requests for each date
                List<QuickEventRequest> requests = new ArrayList<>();
                for (LocalDate date : dates) {
                    QuickEventRequest request = QuickEventRequest.builder()
                            .templateId(baseRequest.getTemplateId())
                            .sourceAction(baseRequest.getSourceAction())
                            .eventType(baseRequest.getEventType())
                            .date(date)
                            .userId(baseRequest.getUserId())
                            .displayName(baseRequest.getDisplayName())
                            .description(baseRequest.getDescription())
                            .priority(baseRequest.getPriority())
                            .allDay(baseRequest.isAllDay())
                            .startTime(baseRequest.getStartTime())
                            .endTime(baseRequest.getEndTime())
                            .location(baseRequest.getLocation())
                            .customProperties(baseRequest.getCustomProperties())
                            .build();
                    requests.add(request);
                }

                // ✅ Use existing bulk creation method
                return createQuickEvents(requests).join();

            } catch (Exception e) {
                Log.e(TAG, "Failed to create quick events for dates: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    // ==================== 🆕 NEW: QUICK EVENT VALIDATION ====================

    @Override
    public OperationResult<Void> validateQuickEventRequest(QuickEventRequest request) {
        List<String> errors = new ArrayList<>();

        try {
            // ✅ Basic validation
            if (request == null) {
                errors.add("Request cannot be null");
                return OperationResult.validationFailure(errors);
            }

            if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
                errors.add("Template ID is required");
            }

            if (request.getSourceAction() == null) {
                errors.add("Source action is required");
            }

            if (request.getEventType() == null) {
                errors.add("Event type is required");
            }

            if (request.getDate() == null) {
                errors.add("Date is required");
            }

            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                errors.add("Display name is required");
            }

            // ✅ Date validation
            if (request.getDate() != null) {
                LocalDate today = LocalDate.now();
                if (request.getDate().isBefore(today)) {
                    errors.add("Cannot create events in the past");
                }

                // Check if date is too far in the future (optional business rule)
                LocalDate maxFutureDate = today.plusYears(2);
                if (request.getDate().isAfter(maxFutureDate)) {
                    errors.add("Cannot create events more than 2 years in the future");
                }
            }

            // ✅ Timing validation
            if (!request.isAllDay()) {
                if (request.getStartTime() == null || request.getEndTime() == null) {
                    errors.add("Start and end times are required for timed events");
                } else if (!request.getStartTime().isBefore(request.getEndTime())) {
                    errors.add("Start time must be before end time");
                } else {
                    // Check for reasonable duration
                    long durationMinutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
                    if (durationMinutes < 5) {
                        errors.add("Event duration must be at least 5 minutes");
                    } else if (durationMinutes > 1440) { // 24 hours
                        errors.add("Event duration cannot exceed 24 hours");
                    }
                }
            }

            // ✅ EventType-specific validation
            if (request.getEventType() != null) {
                if (!isEventTypeValidForQuickCreation(request.getEventType())) {
                    errors.add("Event type '" + request.getEventType().getDisplayName() + "' is not supported for quick creation");
                }

                // Validate event type constraints
                if (!validateEventTypeConstraints(request)) {
                    errors.add("Event configuration violates constraints for type: " + request.getEventType().getDisplayName());
                }
            }

            // ✅ User validation
            if (request.getUserId() != null && request.getUserId() < 0) {
                errors.add("Invalid user ID");
            }

            // ✅ Custom properties validation
            Map<String, String> customProps = request.getCustomProperties();
            if (customProps != null && customProps.size() > 50) {
                errors.add("Too many custom properties (max 50)");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during quick event request validation: " + e.getMessage(), e);
            errors.add("Validation error: " + e.getMessage());
        }

        if (errors.isEmpty()) {
            return OperationResult.validationSuccess();
        } else {
            return OperationResult.validationFailure(errors);
        }
    }

    @Override
    public OperationResult<QuickEventValidationResult> validateQuickEventRequests(List<QuickEventRequest> requests) {
        try {
            Map<QuickEventRequest, List<String>> requestErrors = new HashMap<>();
            List<String> globalErrors = new ArrayList<>();
            int validRequests = 0;
            int invalidRequests = 0;

            if (requests == null || requests.isEmpty()) {
                globalErrors.add("No requests provided");
                return OperationResult.failure("No requests to validate", OperationResult.OperationType.VALIDATION);
            }

            // ✅ Validate each request
            for (QuickEventRequest request : requests) {
                OperationResult<Void> validation = validateQuickEventRequest(request);
                if (validation.isSuccess()) {
                    validRequests++;
                } else {
                    invalidRequests++;
                    requestErrors.put(request, validation.getErrors());
                }
            }

            // ✅ Check for duplicate dates (business rule)
            Map<LocalDate, Integer> dateCount = new HashMap<>();
            for (QuickEventRequest request : requests) {
                if (request.getDate() != null) {
                    dateCount.put(request.getDate(), dateCount.getOrDefault(request.getDate(), 0) + 1);
                }
            }

            for (Map.Entry<LocalDate, Integer> entry : dateCount.entrySet()) {
                if (entry.getValue() > 1) {
                    globalErrors.add("Duplicate events for date: " + entry.getKey() + " (count: " + entry.getValue() + ")");
                }
            }

            QuickEventValidationResult result = new QuickEventValidationResult(
                    requests.size(), validRequests, invalidRequests, globalErrors, requestErrors);

            if (result.isValid() && !result.hasErrors()) {
                return OperationResult.success(result, "All requests are valid", OperationResult.OperationType.VALIDATION);
            } else {
                String message = "Validation completed: " + validRequests + " valid, " + invalidRequests + " invalid";
                return OperationResult.failure( message, OperationResult.OperationType.VALIDATION);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during bulk validation: " + e.getMessage(), e);
            return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
        }
    }

    // ==================== 🆕 NEW: QUICK EVENT AVAILABILITY ====================

    @Override
    public CompletableFuture<OperationResult<Boolean>> canCreateQuickEvent(ToolbarAction action, LocalDate date, Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.v(TAG, "Checking if quick event can be created: " + action + " for " + date);

                // ✅ Basic checks
                if (action == null || date == null) {
                    return OperationResult.failure("Action and date are required", OperationResult.OperationType.VALIDATION);
                }

                if (date.isBefore(LocalDate.now())) {
                    return OperationResult.failure("Cannot create events in the past", OperationResult.OperationType.VALIDATION);
                }

                if (!action.isEventCreationAction()) {
                    return OperationResult.failure("Action is not an event creation action", OperationResult.OperationType.VALIDATION);
                }

                // ✅ Check if event type is supported
                EventType eventType = action.getMappedEventType();
                if (eventType == null || !isEventTypeValidForQuickCreation(eventType)) {
                    return OperationResult.failure("Event type not supported for quick creation", OperationResult.OperationType.VALIDATION);
                }

                // ✅ Check for existing events on the same date (business rule)
                List<LocalEvent> existingEvents = mEventDao.getEventsForDate(date.atStartOfDay(), date.atTime(23, 59, 59));
                if (hasConflictingEvents(action, existingEvents)) {
                    return OperationResult.failure("Conflicting events exist for this date", OperationResult.OperationType.VALIDATION);
                }

                // ✅ Additional business rules can be added here
                // For example: check user permissions, quotas, etc.

                return OperationResult.success(true, "Quick event can be created", OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Error checking quick event availability: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Map<LocalDate, Boolean>>> canCreateQuickEventsForDates(ToolbarAction action, List<LocalDate> dates, Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.v(TAG, "Checking quick event availability for " + dates.size() + " dates");

                Map<LocalDate, Boolean> availabilityMap = new HashMap<>();

                if (dates.isEmpty()) {
                    return OperationResult.success(availabilityMap, "No dates to check", OperationResult.OperationType.VALIDATION);
                }

                // ✅ Check each date
                for (LocalDate date : dates) {
                    OperationResult<Boolean> result = canCreateQuickEvent(action, date, userId).join();
                    availabilityMap.put(date, result.isSuccess() && result.getData());
                }

                long availableCount = availabilityMap.values().stream().mapToLong(b -> b ? 1 : 0).sum();
                String message = availableCount + " out of " + dates.size() + " dates are available";

                return OperationResult.success(availabilityMap, message, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Error checking bulk availability: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== 🆕 NEW: HELPER METHODS ====================

    private boolean getDefaultAllDayForEventType(EventType eventType) {
        return eventType == EventType.STOP_PLANNED || eventType == EventType.STOP_UNPLANNED;
    }

    private LocalTime getDefaultStartTimeForEventType(EventType eventType) {
        return eventType == EventType.MEETING ? LocalTime.of(9, 0) : LocalTime.of(8, 0);
    }

    private LocalTime getDefaultEndTimeForEventType(EventType eventType) {
        return eventType == EventType.MEETING ? LocalTime.of(10, 0) : LocalTime.of(17, 0);
    }

    /**
     * Create LocalEvent from QuickEventRequest
     */
    private LocalEvent createEventFromRequest(QuickEventRequest request) {
        // Sostituire logica con:
        EventAction eventAction = ToolbarActionBridge.mapToEventAction(request.getSourceAction());
        LocalEvent event = QuickEventLogicAdapter.createEventFromEventAction(eventAction, request.getDate(), request.getUserId());

        // Aggiornare con dati da request
        if (!request.getDisplayName().isEmpty()) {
            event.setTitle(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        // Inizializzare metadata tracking
        event = EventMetadataManager.initializeEditSessionMetadata(event, request.getUserId());

        return event;

//        LocalEvent event = new LocalEvent();
//
//        // ✅ Basic properties
//        event.setId(UUID.randomUUID().toString());
//        event.setTitle(request.getDisplayName());
//        event.setDescription(request.getDescription());
//        event.setEventType(request.getEventType());
//        event.setPriority(request.getPriority());
//        event.setLocation(request.getLocation());
//        event.setAllDay(request.isAllDay());
//
//        // ✅ Timing
//        if (request.isAllDay()) {
//            event.setStartTime(request.getDate().atStartOfDay());
//            event.setEndTime(request.getDate().atTime(23, 59, 59));
//        } else {
//            event.setStartTime(request.getDate().atTime(request.getStartTime()));
//            event.setEndTime(request.getDate().atTime(request.getEndTime()));
//        }
//
//        // ✅ Metadata
//        event.setLastUpdated(LocalDateTime.now());
//
//        // ✅ Custom properties with template metadata
//        Map<String, String> customProperties = new HashMap<>(request.getCustomProperties());
//        customProperties.put("template_id", request.getTemplateId());
//        customProperties.put("source_action", request.getSourceAction().name());
//        customProperties.put("quick_event", "true");
//        customProperties.put("created_at", String.valueOf(request.getCreatedAt()));
//        event.setCustomProperties(customProperties);
//
//        return event;
    }

    /**
     * Check if event type is valid for quick creation
     */
    private boolean isEventTypeValidForQuickCreation(EventType eventType) {
        // ✅ Business rule: only certain event types support quick creation
        switch (eventType) {
            // ✅ EXISTING: General events
            case GENERAL:
            case MEETING:
            case TRAINING:
                return true;

            // ✅ EXISTING: Production events
            case STOP_PLANNED:
            case STOP_UNPLANNED:
            case MAINTENANCE:
            case EMERGENCY:
                return true;

            // ✅ NEW: User absence events (from ToolbarAction quick events)
            case VACATION:          // VACATION
            case SICK_LEAVE:        // SICK_LEAVE
            case PERSONAL_LEAVE:    // PERSONAL_LEAVE
            case SPECIAL_LEAVE:     // SPECIAL_LEAVE
            case SYNDICATE_LEAVE:   // SYNDICATE_LEAVE
                return true;

            // ✅ NEW: Work events
            case OVERTIME:          // OVERTIME
            case COMPENSATION:      // RECUPERO
            case SHIFT_SWAP:        // CAMBIO_TURNO
                return true;

            // ✅ BLOCKED: These types require special handling
            case HOLIDAY:           // System holidays
            case IMPORTED:          // Imported events
            case OTHER:             // Undefined events
                return false;

            default:
                // ✅ Log unknown types for debugging
                Log.w(TAG, "Unknown event type for quick creation check: " + eventType);
                return false;
        }
    }

    /**
     * Validate event type specific constraints
     */
    private boolean validateEventTypeConstraints(QuickEventRequest request) {
        EventType eventType = request.getEventType();

        // ✅ Add specific validation rules for each event type
        switch (eventType) {
            // Production events - should be all-day
            case STOP_PLANNED:
            case STOP_UNPLANNED:
            case MAINTENANCE:
                return request.isAllDay();

            // Work events - should have specific times
            case MEETING:
            case TRAINING:
            case OVERTIME:

                // TODO: implement an overtime specific interfaces
                // Can be either all-day or partial day
                if (request.isAllDay()) {
                    return true;
                } else {
                    // If timed, must have valid start/end times
                    return request.getStartTime() != null &&
                            request.getEndTime() != null &&
                            request.getStartTime().isBefore(request.getEndTime());
                }

                // ✅ NEW: Straordinario deve avere orari specifici
//                return !request.isAllDay() &&
//                        request.getStartTime() != null &&
//                        request.getEndTime() != null;

            // User absence events - typically all-day but can be partial
            case VACATION:           // ✅ NEW: Ferie
            case SICK_LEAVE:         // ✅ NEW: Malattia
            case PERSONAL_LEAVE:     // ✅ NEW: Permesso
            case SPECIAL_LEAVE:      // ✅ NEW: Legge 104
            case SYNDICATE_LEAVE:    // ✅ NEW: Permesso sindacale
                // Can be either all-day or partial day
                if (request.isAllDay()) {
                    return true;
                } else {
                    // If timed, must have valid start/end times
                    return request.getStartTime() != null &&
                            request.getEndTime() != null &&
                            request.getStartTime().isBefore(request.getEndTime());
                }

                // Flexible events
            case GENERAL:
            case COMPENSATION:
            case SHIFT_SWAP:
                return true;

            default:
                Log.w(TAG, "Unknown event type in constraint validation: " + eventType);
                return true;
        }
    }

    /**
     * Check if there are conflicting events for the action
     */
    private boolean hasConflictingEvents(ToolbarAction action, List<LocalEvent> existingEvents) {
        // ✅ Business rules for conflicts per action type
        switch (action) {
            // User absence events - typically one per day of same type
            case VACATION:
                return existingEvents.stream()
                        .anyMatch(event -> event.getEventType() == EventType.VACATION);

            case SICK_LEAVE:
                return existingEvents.stream()
                        .anyMatch(event -> event.getEventType() == EventType.SICK_LEAVE);

            case PERSONAL_LEAVE:
                return existingEvents.stream()
                        .anyMatch(event -> event.getEventType() == EventType.PERSONAL_LEAVE);

            case SPECIAL_LEAVE:
                return existingEvents.stream()
                        .anyMatch(event -> event.getEventType() == EventType.SPECIAL_LEAVE);

            case SYNDICATE_LEAVE:
                return existingEvents.stream()
                        .anyMatch(event -> event.getEventType() == EventType.SYNDICATE_LEAVE);

            // Work events - can have multiple per day
            case OVERTIME:
                // Check for overlapping overtime on same day
                return existingEvents.stream()
                        .filter(event -> event.getEventType() == EventType.OVERTIME)
                        .anyMatch(event -> !event.isAllDay()); // If existing overtime is timed, might conflict

            // General actions - no specific conflicts
            case ADD_EVENT:
            case VIEW_EVENTS:
                return false;

            default:
                Log.w(TAG, "Unknown action in conflict check: " + action);
                return false;
        }
    }

    /**
     * Trigger backup after quick event creation
     */
    private void triggerBackupAfterCreation(LocalEvent event) {
        if (mBackupManager != null) {
            try {
                mBackupManager.performAutoBackup("create_quick", event.getTitle());
                Log.v(TAG, "Backup triggered for quick event: " + event.getId());
            } catch (Exception e) {
                Log.w(TAG, "Failed to trigger backup after quick event creation: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Trigger backup after bulk creation
     */
    private void triggerBackupAfterBulkCreation(List<LocalEvent> events) {
        if (mBackupManager != null && !events.isEmpty()) {
            try {
                mBackupManager.performAutoBackup("create_quick_bulk", events.size() + " events");
                Log.v(TAG, "Backup triggered for " + events.size() + " quick events");
            } catch (Exception e) {
                Log.w(TAG, "Failed to trigger backup after bulk creation: " + e.getMessage(), e);
            }
        }
    }
    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Create a duplicate of an existing event for a new date
     */
    private LocalEvent createDuplicateEvent(LocalEvent original, LocalDate newDate) {
        LocalEvent duplicate = new LocalEvent();

        // Generate new ID for duplicate
        duplicate.setId(UUID.randomUUID().toString());

        // Copy basic properties
        duplicate.setTitle(original.getTitle() + " (Copy)");
        duplicate.setDescription(original.getDescription());
        duplicate.setEventType(original.getEventType());
        duplicate.setPriority(original.getPriority());
        duplicate.setAllDay(original.isAllDay());
        duplicate.setLocation(original.getLocation());

        // Set new date/time
        if (original.isAllDay()) {
            duplicate.setStartTime(newDate.atStartOfDay());
            duplicate.setEndTime(newDate.atTime(23, 59));
        } else {
            LocalDateTime originalStart = original.getStartTime();
            LocalDateTime originalEnd = original.getEndTime();

            duplicate.setStartTime(newDate.atTime(originalStart.toLocalTime()));
            duplicate.setEndTime(newDate.atTime(originalEnd.toLocalTime()));
        }

        // Copy custom properties if they exist
        if (original.getCustomProperties() != null) {
            duplicate.setCustomProperties(new HashMap<>(original.getCustomProperties()));
        }

        // Clear package-specific properties (this is a local duplicate)
        duplicate.setPackageId(null);
        duplicate.setSourceUrl(null);
        duplicate.setPackageVersion(null);

        // Set creation timestamp
        duplicate.setLastUpdated(LocalDateTime.now());

        return duplicate;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when service is no longer needed
     * Should be called from DI container or application lifecycle
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "EventsServiceImpl executor service shutdown");
        }
    }
}
