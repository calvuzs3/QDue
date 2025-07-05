package net.calvuz.qdue.services.impl;

import android.content.Context;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.backup.BackupIntegration;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.services.EventsService;
import net.calvuz.qdue.services.models.OperationResult;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * STEP 1: Centralized Implementation of EventsService
 *
 * This implementation replaces direct DAO access from UI components,
 * ensuring ALL event operations go through this service for:
 * - Consistent backup integration
 * - Centralized validation
 * - Unified error handling
 * - Background operations
 *
 * Integration Points:
 * 1. EventsActivity - Replace direct EventDao calls with this service
 * 2. BaseClickFragmentLegacy - Replace business logic with service calls
 * 3. All other UI components - Use this service only
 */
public class EventsServiceImpl implements EventsService {

    private static final String TAG = "EventsServiceImpl";

    private final Context mContext;
    private final EventDao mEventDao;
    private final BackupIntegration mBackupIntegration;
    private final ExecutorService mExecutorService;
    private static volatile EventsServiceImpl INSTANCE;

    // ==================== CONSTRUCTOR AND SINGLETON ====================

    private EventsServiceImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mEventDao = QDueDatabase.getInstance(context).eventDao();
        this.mBackupIntegration = BackupIntegration.getInstance(context);
        this.mExecutorService = Executors.newFixedThreadPool(4);

        Log.d(TAG, "EventsServiceImpl initialized");
    }

    /**
     * Get singleton instance of EventsService
     */
    public static EventsServiceImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EventsServiceImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EventsServiceImpl(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== CORE CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> createEvent(LocalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate event
                OperationResult<Void> validation = validateEvent(event);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Set creation timestamp
                event.setLastUpdated(LocalDateTime.now());

                // Insert into database
                mEventDao.insertEvent(event);

                // Get all events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onEventCreated(allEvents, event);

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
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate all events
                List<String> validationErrors = new ArrayList<>();
                for (int i = 0; i < events.size(); i++) {
                    OperationResult<Void> validation = validateEvent(events.get(i));
                    if (validation.isFailure()) {
                        validationErrors.add("Event " + (i + 1) + ": " + validation.getFirstError());
                    }
                }

                if (!validationErrors.isEmpty()) {
                    return OperationResult.failure(validationErrors,
                            OperationResult.OperationType.BULK_CREATE);
                }

                // Set timestamps
                LocalDateTime now = LocalDateTime.now();
                for (LocalEvent event : events) {
                    event.setLastUpdated(now);
                }

                // Insert all events
                mEventDao.insertEvents(events);

                // Get all events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onBulkEventsChanged(allEvents, "bulk_create");

                Log.d(TAG, "Created " + events.size() + " events successfully");
                return OperationResult.success(events.size(),
                        "Created " + events.size() + " events successfully",
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
                // Validate event
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

                // Update in database
                mEventDao.updateEvent(event);

                // Get all events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onEventUpdated(allEvents, event);

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
    public CompletableFuture<OperationResult<String>> deleteEvent(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get event before deletion for backup info
                LocalEvent event = mEventDao.getEventById(eventId);
                if (event == null) {
                    return OperationResult.failure("Event not found: " + eventId,
                            OperationResult.OperationType.DELETE);
                }

                String eventTitle = event.getTitle();

                // Delete from database
                mEventDao.deleteEvent(event);

                // Get remaining events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onEventDeleted(allEvents, eventTitle);

                Log.d(TAG, "Event deleted successfully: " + eventTitle);
                return OperationResult.success(eventTitle, "Event deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete event: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteEvents(List<String> eventIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int deletedCount = 0;
                List<String> errors = new ArrayList<>();

                for (String eventId : eventIds) {
                    try {
                        LocalEvent event = mEventDao.getEventById(eventId);
                        if (event != null) {
                            mEventDao.deleteEvent(event);
                            deletedCount++;
                        } else {
                            errors.add("Event not found: " + eventId);
                        }
                    } catch (Exception e) {
                        errors.add("Failed to delete event " + eventId + ": " + e.getMessage());
                    }
                }

                // Get remaining events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onBulkEventsChanged(allEvents, "bulk_delete");

                if (!errors.isEmpty()) {
                    Log.w(TAG, "Bulk delete completed with errors: " + errors.size());
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_DELETE);
                }

                Log.d(TAG, "Bulk deleted " + deletedCount + " events successfully");
                return OperationResult.success(deletedCount,
                        "Deleted " + deletedCount + " events successfully",
                        OperationResult.OperationType.BULK_DELETE);

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

                // Delete all events
                mEventDao.deleteAllLocalEvents();

                // Get remaining events (should be empty) for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onBulkEventsChanged(allEvents, "clear_all");

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
    public CompletableFuture<List<LocalEvent>> getAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mEventDao.getAllEvents();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all events: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<LocalEvent> getEventById(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mEventDao.getEventById(eventId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get event by ID: " + e.getMessage(), e);
                return null;
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<List<LocalEvent>> getEventsForDate(LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                return mEventDao.getEventsForDate(startOfDay, endOfDay);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events for date: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<List<LocalEvent>> getEventsInRange(LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(23, 59, 59);
                return mEventDao.getEventsInDateRange(start, end);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events in range: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<List<LocalEvent>> getEventsByType(EventType eventType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mEventDao.getEventsByType(eventType);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events by type: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<List<LocalEvent>> searchEvents(String searchTerm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = "%" + searchTerm + "%";
                return mEventDao.searchEvents(query);
            } catch (Exception e) {
                Log.e(TAG, "Failed to search events: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== BULK OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<LocalEvent>> duplicateEvent(String eventId, LocalDate newDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
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

                // Get all events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onEventCreated(allEvents, duplicate);

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
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalEvents = events.size();
                int importedEvents = 0;
                int skippedEvents = 0;
                int errorEvents = 0;
                List<String> errors = new ArrayList<>();

                // Clear all if replace mode
                if (replaceAll) {
                    mEventDao.deleteAllLocalEvents();
                }

                // Import each event
                for (LocalEvent event : events) {
                    try {
                        // Validate event
                        OperationResult<Void> validation = validateEvent(event);
                        if (validation.isFailure()) {
                            errors.add("Event '" + event.getTitle() + "': " + validation.getFirstError());
                            errorEvents++;
                            continue;
                        }

                        // Check for duplicates (by ID)
                        if (!replaceAll && mEventDao.getEventById(event.getId()) != null) {
                            skippedEvents++;
                            continue;
                        }

                        // Set import timestamp
                        event.setLastUpdated(LocalDateTime.now());

                        // Insert event
                        mEventDao.insertEvent(event);
                        importedEvents++;

                    } catch (Exception e) {
                        errors.add("Event '" + event.getTitle() + "': " + e.getMessage());
                        errorEvents++;
                    }
                }

                // Get all events for backup
                List<LocalEvent> allEvents = mEventDao.getAllEvents();

                // Trigger backup
                mBackupIntegration.onEventsImported(allEvents, importedEvents);

                ImportResult result = new ImportResult(totalEvents, importedEvents, skippedEvents, errorEvents, errors);

                Log.d(TAG, String.format(QDue.getLocale(),
                        "Import completed: %d imported, %d skipped, %d errors",
                        importedEvents, skippedEvents, errorEvents));

                if (errorEvents > 0) {
                    return OperationResult.failure(errors, OperationResult.OperationType.IMPORT);
                } else {
                    return OperationResult.success(result, "Import completed successfully",
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
                    // Export date range
                    LocalDateTime start = startDate.atStartOfDay();
                    LocalDateTime end = endDate.atTime(23, 59, 59);
                    events = mEventDao.getEventsInDateRange(start, end);
                } else {
                    // Export all events
                    events = mEventDao.getAllEvents();
                }

                Log.d(TAG, "Exported " + events.size() + " events");
                return OperationResult.success(events, "Exported " + events.size() + " events",
                        OperationResult.OperationType.EXPORT);

            } catch (Exception e) {
                Log.e(TAG, "Failed to export events: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.EXPORT);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS AND INFO ====================

    @Override
    public CompletableFuture<Integer> getEventsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mEventDao.getEventsCount();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events count: " + e.getMessage(), e);
                return 0;
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<Map<EventType, Integer>> getEventsCountByType() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LocalEvent> allEvents = mEventDao.getAllEvents();
                Map<EventType, Integer> counts = new HashMap<>();

                for (LocalEvent event : allEvents) {
                    EventType type = event.getEventType();
                    counts.put(type, counts.getOrDefault(type, 0) + 1);
                }

                return counts;
            } catch (Exception e) {
                Log.e(TAG, "Failed to get events count by type: " + e.getMessage(), e);
                return new HashMap<>();
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<LocalEvent> getNextUpcomingEvent(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<LocalEvent> upcomingEvents = mEventDao.getUpcomingEvents(now, limit);
                return upcomingEvents.isEmpty() ? null : upcomingEvents.get(0);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get next upcoming event: " + e.getMessage(), e);
                return null;
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<Boolean> eventExists(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mEventDao.getEventById(eventId) != null;
            } catch (Exception e) {
                Log.e(TAG, "Failed to check event existence: " + e.getMessage(), e);
                return false;
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateEvent(LocalEvent event) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        if (event == null) {
            errors.add("Event cannot be null");
            return OperationResult.validationFailure(errors);
        }

        if (event.getId() == null || event.getId().trim().isEmpty()) {
            errors.add("Event ID cannot be empty");
        }

        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            errors.add("Event title cannot be empty");
        }

        if (event.getStartTime() == null) {
            errors.add("Event start time cannot be null");
        }

        if (event.getEndTime() == null) {
            errors.add("Event end time cannot be null");
        }

        // Date logic validation
        if (event.getStartTime() != null && event.getEndTime() != null) {
            if (event.getStartTime().isAfter(event.getEndTime())) {
                errors.add("Start time cannot be after end time");
            }
        }

        // Title length validation
        if (event.getTitle() != null && event.getTitle().length() > 100) {
            errors.add("Event title cannot exceed 100 characters");
        }

        // Description length validation
        if (event.getDescription() != null && event.getDescription().length() > 1000) {
            errors.add("Event description cannot exceed 1000 characters");
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    @Override
    public CompletableFuture<List<LocalEvent>> getConflictingEvents(LocalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (event.getStartTime() == null || event.getEndTime() == null) {
                    return new ArrayList<>();
                }

                // Get events in the same time range
                List<LocalEvent> overlappingEvents = mEventDao.getEventsInDateRange(
                        event.getStartTime(), event.getEndTime());

                // Filter out the event itself (for updates)
                overlappingEvents.removeIf(e -> e.getId().equals(event.getId()));

                return overlappingEvents;

            } catch (Exception e) {
                Log.e(TAG, "Failed to get conflicting events: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, mExecutorService);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Create duplicate event with new date
     */
    private LocalEvent createDuplicateEvent(LocalEvent original, LocalDate newDate) {
        LocalEvent duplicate = new LocalEvent();

        // Copy all properties
        duplicate.setTitle(original.getTitle() + " (Copy)");
        duplicate.setDescription(original.getDescription());
        duplicate.setEventType(original.getEventType());
        duplicate.setPriority(original.getPriority());
        duplicate.setAllDay(original.isAllDay());
        duplicate.setLocation(original.getLocation());

        // Set new date while preserving time if not all-day
        if (original.isAllDay()) {
            duplicate.setStartTime(newDate.atStartOfDay());
            duplicate.setEndTime(newDate.atTime(23, 59));
        } else {
            LocalDateTime originalStart = original.getStartTime();
            LocalDateTime originalEnd = original.getEndTime();

            duplicate.setStartTime(newDate.atTime(originalStart.toLocalTime()));
            duplicate.setEndTime(newDate.atTime(originalEnd.toLocalTime()));
        }

        // Copy custom properties
        if (original.getCustomProperties() != null) {
            duplicate.setCustomProperties(new HashMap<>(original.getCustomProperties()));
        }

        // Clear package info (this is a manual duplicate, not from package)
        duplicate.setPackageId(null);
        duplicate.setSourceUrl(null);
        duplicate.setPackageVersion(null);

        return duplicate;
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}