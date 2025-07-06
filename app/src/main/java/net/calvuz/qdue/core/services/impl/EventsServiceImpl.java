package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 *
 * Centralized implementation for all Event operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 * - Google Calendar-like event management
 *
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
     * @param context Application context
     * @param database QDue database instance
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
    public CompletableFuture<OperationResult<List<LocalEvent>>> getEventsInRange(LocalDate startDate, LocalDate endDate) {
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
                List<LocalEvent> events = mEventDao.getEventsInDateRange(start, end);

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
                    events = mEventDao.getEventsInDateRange(start, end);
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
                List<LocalEvent> overlappingEvents = mEventDao.getEventsInDateRange(
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
