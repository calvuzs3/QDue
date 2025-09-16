package net.calvuz.qdue.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.dao.LocalEventDao;
import net.calvuz.qdue.data.entities.LocalEventEntity;
import net.calvuz.qdue.domain.calendar.repositories.LocalEventsRepository;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.enums.EventType;
import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * LocalEvent Repository Implementation
 *
 * <p>Thread-safe implementation of LocalEventsRepository that provides data access
 * operations for LocalEvent entities. Uses Room database through LocalEventDao
 * with proper error handling, validation, and asynchronous execution.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Thread Safety</strong>: All operations executed on background threads</li>
 *   <li><strong>Error Handling</strong>: Comprehensive exception handling with logging</li>
 *   <li><strong>Data Mapping</strong>: Entity-Domain model conversion</li>
 *   <li><strong>Validation</strong>: Input validation and business rule enforcement</li>
 *   <li><strong>Performance</strong>: Optimized queries and batch operations</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <p>This implementation bridges the domain layer with the data layer, converting
 * between LocalEvent domain models and LocalEventEntity data models while
 * maintaining clean separation of concerns.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @see LocalEventsRepository
 * @see net.calvuz.qdue.data.dao.LocalEventDao
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsRepositoryImpl
        implements LocalEventsRepository
{

    private static final String TAG = "LocalEventsRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final LocalEventDao mLocalEventDao;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param localEventDao DAO for LocalEvent database operations
     */
    public LocalEventsRepositoryImpl(@NonNull LocalEventDao localEventDao) {
        this.mLocalEventDao = localEventDao;
        this.mExecutorService = Executors.newFixedThreadPool( 4 );

        Log.d( TAG, "LocalEventsRepositoryImpl initialized" );
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> createLocalEvent(@NonNull LocalEvent event) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Creating LocalEvent: " + event.getTitle() );

                // Validate event
                OperationResult<Void> validationResult = validateLocalEvent( event );
                if (!validationResult.isSuccess()) {
                    return OperationResult.failure(
                            "Validation failed: " + validationResult.getFirstError(),
                            OperationResult.OperationType.CREATE
                    );
                }

                // Convert to entity and insert
                LocalEventEntity entity = LocalEventEntity.fromDomainModel( event );
                long insertResult = mLocalEventDao.insertLocalEvent( entity );

                if (insertResult > 0) {
                    Log.d( TAG, "LocalEvent created successfully with ID: " + event.getId() );
                    return OperationResult.success(
                            event,
                            "Event created successfully",
                            OperationResult.OperationType.CREATE
                    );
                } else {
                    Log.e( TAG, "Failed to insert LocalEvent" );
                    return OperationResult.failure(
                            "Failed to create event in database",
                            OperationResult.OperationType.CREATE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error creating LocalEvent", e );
                return OperationResult.failure(
                        "Create operation failed: " + e.getMessage(),
                        OperationResult.OperationType.CREATE
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> updateLocalEvent(@NonNull LocalEvent event) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating LocalEvent: " + event.getId() );

                // Validate event
                OperationResult<Void> validationResult = validateLocalEvent( event );
                if (!validationResult.isSuccess()) {
                    return OperationResult.failure(
                            "Validation failed: " + validationResult.getFirstError(),
                            OperationResult.OperationType.UPDATE
                    );
                }

                // Check if event exists
                if (mLocalEventDao.getEventById( event.getId() ) == null) {
                    return OperationResult.failure(
                            "Event not found with ID: " + event.getId(),
                            OperationResult.OperationType.UPDATE
                    );
                }

                // Convert to entity and update
                LocalEventEntity entity = LocalEventEntity.fromDomainModel( event );
                int updateResult = mLocalEventDao.updateLocalEvent( entity );

                if (updateResult > 0) {
                    Log.d( TAG, "LocalEvent updated successfully: " + event.getId() );
                    return OperationResult.success(
                            event,
                            "Event updated successfully",
                            OperationResult.OperationType.UPDATE
                    );
                } else {
                    Log.w( TAG, "No rows affected during update: " + event.getId() );
                    return OperationResult.failure(
                            "Event not found or no changes made",
                            OperationResult.OperationType.UPDATE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error updating LocalEvent: " + event.getId(), e );
                return OperationResult.failure(
                        "Update operation failed: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> deleteLocalEvent(@NonNull String eventId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deleting LocalEvent: " + eventId );

                if (eventId.trim().isEmpty()) {
                    return OperationResult.failure(
                            "Event ID cannot be null or empty",
                            OperationResult.OperationType.DELETE
                    );
                }

                int deleteResult = mLocalEventDao.deleteEventById( eventId );

                if (deleteResult > 0) {
                    Log.d( TAG, "LocalEvent deleted successfully: " + eventId );
                    return OperationResult.success(
                            true,
                            "Event deleted successfully",
                            OperationResult.OperationType.DELETE
                    );
                } else {
                    Log.w( TAG, "No event found to delete: " + eventId );
                    return OperationResult.failure(
                            "Event not found with ID: " + eventId,
                            OperationResult.OperationType.DELETE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error deleting LocalEvent: " + eventId, e );
                return OperationResult.failure(
                        "Delete operation failed: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> deleteLocalEvents(@NonNull List<String> eventIds) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deleting " + eventIds.size() + " LocalEvents" );

                if (eventIds.isEmpty()) {
                    return OperationResult.success(
                            0,
                            "No events to delete",
                            OperationResult.OperationType.DELETE
                    );
                }

                int totalDeleted = 0;
                List<String> failedDeletes = new ArrayList<>();

                for (String eventId : eventIds) {
                    try {
                        int result = mLocalEventDao.deleteEventById( eventId );
                        if (result > 0) {
                            totalDeleted++;
                        } else {
                            failedDeletes.add( eventId );
                        }
                    } catch (Exception e) {
                        Log.w( TAG, "Failed to delete event: " + eventId, e );
                        failedDeletes.add( eventId );
                    }
                }

                if (failedDeletes.isEmpty()) {
                    Log.d( TAG, "Successfully deleted " + totalDeleted + " events" );
                    return OperationResult.success(
                            totalDeleted,
                            "All events deleted successfully",
                            OperationResult.OperationType.DELETE
                    );
                } else {
                    Log.w( TAG,
                           "Partial deletion: " + totalDeleted + " deleted, " + failedDeletes.size() + " failed" );
                    return OperationResult.success(
                            totalDeleted,
                            "Partial deletion: " + failedDeletes.size() + " events failed",
                            OperationResult.OperationType.DELETE
                    );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error in batch delete operation", e );
                return OperationResult.failure(
                        "Batch delete operation failed: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> deleteAllLocalEvents() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Deleting all LocalEvents" );

                int deleteResult = mLocalEventDao.deleteAllLocalEvents();

                Log.d( TAG, "Deleted " + deleteResult + " LocalEvents" );
                return OperationResult.success(
                        deleteResult,
                        "All events deleted successfully",
                        OperationResult.OperationType.DELETE
                );
            } catch (Exception e) {
                Log.e( TAG, "Error deleting all LocalEvents", e );
                return OperationResult.failure(
                        "Delete all operation failed: " + e.getMessage(),
                        OperationResult.OperationType.DELETE
                );
            }
        }, mExecutorService );
    }

    // ==================== RETRIEVAL OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalEvent>> getLocalEventById(@NonNull String eventId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvent by ID: " + eventId );

                if (eventId.trim().isEmpty()) {
                    return OperationResult.failure(
                            "Event ID cannot be null or empty",
                            OperationResult.OperationType.READ
                    );
                }

                LocalEventEntity eventEntity = mLocalEventDao.getEventById( eventId );
                if (eventEntity == null) {
                    Log.d( TAG, "LocalEvent not found: " + eventId );
                    return OperationResult.failure(
                            "Event not found with ID: " + eventId,
                            OperationResult.OperationType.READ
                    );
                }

                LocalEvent event = eventEntity.toDomainModel();

                Log.d( TAG, "LocalEvent found: " + eventId );
                return OperationResult.success(
                        event,
                        "Event retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting LocalEvent by ID: " + eventId, e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getAllLocalEvents() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting all LocalEvents" );

                List<LocalEventEntity> eventsEntities = mLocalEventDao.getAllEvents();
                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Retrieved " + events.size() + " LocalEvents" );
                return OperationResult.success(
                        events,
                        "Events retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting all LocalEvents", e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsForDate(@NonNull LocalDateTime date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvents for date: " + date.toLocalDate() );

                LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = date.toLocalDate().atTime( 23, 59, 59 );

                List<LocalEventEntity> eventsEntities = mLocalEventDao.getEventsForDate( startOfDay,
                                                                                         endOfDay );

                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG,
                       "Retrieved " + events.size() + " events for date: " + date.toLocalDate() );
                return OperationResult.success(
                        events,
                        "Events for date retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events for date: " + date.toLocalDate(), e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsForDateRange(
            @NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG,
                       "Getting LocalEvents for " +
                               "date range: " + startDate.toLocalDate() +
                               " to " + endDate.toLocalDate() );

                if (startDate.isAfter( endDate )) {
                    return OperationResult.failure(
                            "Start date cannot be after end date",
                            OperationResult.OperationType.READ
                    );
                }

                List<LocalEventEntity> eventsEntities = mLocalEventDao.getEventsForDateRange( startDate,
                                                                                endDate );
                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Retrieved " + events.size() + " events for date range" );
                return OperationResult.success(
                        events,
                        "Events for date range retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events for date range", e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getUpcomingLocalEvents(
            @NonNull LocalDateTime currentTime, int limit) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG,
                       "Getting upcoming LocalEvents from: " + currentTime + " (limit: " + limit + ")" );

                if (limit <= 0) {
                    return OperationResult.failure(
                            "Limit must be greater than 0",
                            OperationResult.OperationType.READ
                    );
                }

                List<LocalEventEntity> eventsEntities = mLocalEventDao.getUpcomingEvents( currentTime, limit );
                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Retrieved " + events.size() + " upcoming events" );
                return OperationResult.success(
                        events,
                        "Upcoming events retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting upcoming events", e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== SEARCH AND FILTERING ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> searchLocalEvents(@NonNull String query) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Searching LocalEvents with query: " + query );

                if (query.trim().isEmpty()) {
                    return OperationResult.failure(
                            "Search query cannot be null or empty",
                            OperationResult.OperationType.READ
                    );
                }

                List<LocalEventEntity> eventsEntities = mLocalEventDao.searchEvents( query.trim() );
                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Search returned " + events.size() + " events" );
                return OperationResult.success(
                        events,
                        "Search completed successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error searching events with query: " + query, e );
                return OperationResult.failure(
                        "Search operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByType(@NonNull EventType eventType) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvents by type: " + eventType );

                List<LocalEventEntity> eventsEntities = mLocalEventDao.getEventsByType( eventType );
                List<LocalEvent> events = eventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Retrieved " + events.size() + " events of type: " + eventType );
                return OperationResult.success(
                        events,
                        "Events by type retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events by type: " + eventType, e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByPriority(@NonNull Priority priority) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvents by priority: " + priority );

                // Since the DAO doesn't have getEventsByPriority, we'll get all and filter
                List<LocalEventEntity> allEventsEntities = mLocalEventDao.getAllEvents();
                List<LocalEvent> filteredEvents = allEventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .filter( event -> priority.equals( event.getPriority() ) )
                        .collect( Collectors.toList() );


                Log.d( TAG,
                       "Retrieved " + filteredEvents.size() + " events with priority: " + priority );
                return OperationResult.success(
                        filteredEvents,
                        "Events by priority retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events by priority: " + priority, e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> getLocalEventsByCalendarId(@NonNull String calendarId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvents by calendar ID: " + calendarId );

                if ( calendarId.trim().isEmpty()) {
                    return OperationResult.failure(
                            "Calendar ID cannot be null or empty",
                            OperationResult.OperationType.READ
                    );
                }

                // Since the DAO doesn't have getEventsByCalendarId, we'll get all and filter
                List<LocalEventEntity> allEventsEntities = mLocalEventDao.getAllEvents();
                List<LocalEvent> filteredEvents = allEventsEntities.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .filter( event -> calendarId.equals( event.getCalendarId() ) )
                        .collect( Collectors.toList() );

                Log.d( TAG,
                       "Retrieved " + filteredEvents.size() + " events for calendar: " + calendarId );
                return OperationResult.success(
                        filteredEvents,
                        "Events by calendar ID retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events by calendar ID: " + calendarId, e );
                return OperationResult.failure(
                        "Read operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== STATISTICS AND ANALYTICS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getLocalEventsCount() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting LocalEvents count" );

                int count = mLocalEventDao.getTotalEventCount();

                Log.d( TAG, "Total LocalEvents count: " + count );
                return OperationResult.success(
                        count,
                        "Event count retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting events count", e );
                return OperationResult.failure(
                        "Count operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getUpcomingLocalEventsCount(@NonNull LocalDateTime currentTime) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Getting upcoming LocalEvents count from: " + currentTime );

                int count = mLocalEventDao.getUpcomingEventsCount( currentTime );

                Log.d( TAG, "Upcoming LocalEvents count: " + count );
                return OperationResult.success(
                        count,
                        "Upcoming events count retrieved successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error getting upcoming events count", e );
                return OperationResult.failure(
                        "Count operation failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> localEventExists(@NonNull String eventId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Checking if LocalEvent exists: " + eventId );

                if ( eventId.trim().isEmpty()) {
                    return OperationResult.failure(
                            "Event ID cannot be null or empty",
                            OperationResult.OperationType.READ
                    );
                }

                LocalEventEntity eventEntity = mLocalEventDao.getEventById( eventId );

                boolean exists = eventEntity != null;

                Log.d( TAG, "LocalEvent exists: " + exists + " (ID: " + eventId + ")" );
                return OperationResult.success(
                        exists,
                        "Existence check completed successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error checking event existence: " + eventId, e );
                return OperationResult.failure(
                        "Existence check failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== BATCH OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> createLocalEvents(@NonNull List<LocalEvent> events) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Creating " + events.size() + " LocalEvents in batch" );

                if (events.isEmpty()) {
                    return OperationResult.success(
                            new ArrayList<>(),
                            "No events to create",
                            OperationResult.OperationType.CREATE
                    );
                }

                List<LocalEvent> createdEvents = new ArrayList<>();
                List<String> failedEvents = new ArrayList<>();

                for (LocalEvent event : events) {
                    try {
                        // Validate event
                        OperationResult<Void> validationResult = validateLocalEvent( event );
                        if (!validationResult.isSuccess()) {
                            failedEvents.add( event.getTitle() + " (validation failed)" );
                            continue;
                        }

                        // Convert to entity and insert
                        LocalEventEntity entity = LocalEventEntity.fromDomainModel( event );
                        long insertResult = mLocalEventDao.insertLocalEvent( entity );

                        if (insertResult > 0) {
                            createdEvents.add( event );
                        } else {
                            failedEvents.add( event.getTitle() + " (insert failed)" );
                        }
                    } catch (Exception e) {
                        Log.w( TAG, "Failed to create event: " + event.getTitle(), e );
                        failedEvents.add(
                                event.getTitle() + " (exception: " + e.getMessage() + ")" );
                    }
                }

                String message = "Created " + createdEvents.size() + " events";
                if (!failedEvents.isEmpty()) {
                    message += ", " + failedEvents.size() + " failed";
                }

                Log.d( TAG, "Batch create completed: " + message );
                return OperationResult.success(
                        createdEvents,
                        message,
                        OperationResult.OperationType.CREATE
                );
            } catch (Exception e) {
                Log.e( TAG, "Error in batch create operation", e );
                return OperationResult.failure(
                        "Batch create operation failed: " + e.getMessage(),
                        OperationResult.OperationType.CREATE
                );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> updateLocalEvents(@NonNull List<LocalEvent> events) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating " + events.size() + " LocalEvents in batch" );

                if (events.isEmpty()) {
                    return OperationResult.success(
                            new ArrayList<>(),
                            "No events to update",
                            OperationResult.OperationType.UPDATE
                    );
                }

                List<LocalEvent> updatedEvents = new ArrayList<>();
                List<String> failedEvents = new ArrayList<>();

                for (LocalEvent event : events) {
                    try {
                        // Validate event
                        OperationResult<Void> validationResult = validateLocalEvent( event );
                        if (!validationResult.isSuccess()) {
                            failedEvents.add( event.getTitle() + " (validation failed)" );
                            continue;
                        }

                        // Convert to entity and update
                        LocalEventEntity entity = LocalEventEntity.fromDomainModel( event );
                        int updateResult = mLocalEventDao.updateLocalEvent( entity );

                        if (updateResult > 0) {
                            updatedEvents.add( event );
                        } else {
                            failedEvents.add( event.getTitle() + " (not found or no changes)" );
                        }
                    } catch (Exception e) {
                        Log.w( TAG, "Failed to update event: " + event.getTitle(), e );
                        failedEvents.add(
                                event.getTitle() + " (exception: " + e.getMessage() + ")" );
                    }
                }

                String message = "Updated " + updatedEvents.size() + " events";
                if (!failedEvents.isEmpty()) {
                    message += ", " + failedEvents.size() + " failed";
                }

                Log.d( TAG, "Batch update completed: " + message );
                return OperationResult.success(
                        updatedEvents,
                        message,
                        OperationResult.OperationType.UPDATE
                );
            } catch (Exception e) {
                Log.e( TAG, "Error in batch update operation", e );
                return OperationResult.failure(
                        "Batch update operation failed: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE
                );
            }
        }, mExecutorService );
    }

    // ==================== VALIDATION OPERATIONS ====================

    @Override
    @NonNull
    public OperationResult<Void> validateLocalEvent(@NonNull LocalEvent event) {
        try {
            List<String> errors = new ArrayList<>();

            // Basic validation
            if (event.getId().trim().isEmpty()) {
                errors.add( "Event ID cannot be null or empty" );
            }

            if (event.getTitle().trim().isEmpty()) {
                errors.add( "Event title cannot be null or empty" );
            } else if (event.getTitle().length() > 255) {
                errors.add( "Event title cannot exceed 255 characters" );
            }

            if (event.getCalendarId().trim().isEmpty()) {
                errors.add( "Calendar ID cannot be null or empty" );
            }

            // Time validation
                if (event.getStartTime().isAfter( event.getEndTime() )) {
                    errors.add( "Start time cannot be after end time" );
                }

                if (event.getStartTime().equals( event.getEndTime() ) && !event.isAllDay()) {
                    errors.add(
                            "Start time and end time cannot be the same for non-all-day events" );
                }

            // Optional field validation
            if (event.getDescription() != null && event.getDescription().length() > 1000) {
                errors.add( "Event description cannot exceed 1000 characters" );
            }

            if (event.getLocation() != null && event.getLocation().length() > 255) {
                errors.add( "Event location cannot exceed 255 characters" );
            }

            if (errors.isEmpty()) {
                return OperationResult.success( "Validation passed",
                                                OperationResult.OperationType.VALIDATION );
            } else {
                return OperationResult.failure( errors, OperationResult.OperationType.VALIDATION );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error during event validation", e );
            return OperationResult.failure(
                    "Validation failed: " + e.getMessage(),
                    OperationResult.OperationType.VALIDATION
            );
        }
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<LocalEvent>>> findConflictingEvents(
            @NonNull LocalDateTime startTime, @NonNull LocalDateTime endTime, @Nullable String excludeEventId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Finding conflicting events from " + startTime + " to " + endTime );

                if (startTime.isAfter( endTime )) {
                    return OperationResult.failure(
                            "Start time cannot be after end time",
                            OperationResult.OperationType.READ
                    );
                }

                // Get events in the time range
                List<LocalEventEntity> eventsEntitiesInRange = mLocalEventDao.getEventsForDateRange( startTime,
                                                                                       endTime );

                // Filter out the excluded event and find actual conflicts
                List<LocalEvent> conflictingEvents = eventsEntitiesInRange.stream()
                        .map( LocalEventEntity::toDomainModel )
                        .filter( event -> {
                            // Exclude the specified event
                            if (excludeEventId != null && excludeEventId.equals( event.getId() )) {
                                return false;
                            }

                            // Check for time overlap
                            return timeRangesOverlap(
                                    startTime, endTime,
                                    event.getStartTime(), event.getEndTime()
                            );
                        } )
                        .collect( Collectors.toList() );

                Log.d( TAG, "Found " + conflictingEvents.size() + " conflicting events" );
                return OperationResult.success(
                        conflictingEvents,
                        "Conflict check completed successfully",
                        OperationResult.OperationType.READ
                );
            } catch (Exception e) {
                Log.e( TAG, "Error finding conflicting events", e );
                return OperationResult.failure(
                        "Conflict check failed: " + e.getMessage(),
                        OperationResult.OperationType.READ
                );
            }
        }, mExecutorService );
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check if two time ranges overlap.
     */
    private boolean timeRangesOverlap(
            LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2
    ) {
        return start1.isBefore( end2 ) && start2.isBefore( end1 );
    }

    /**
     * Cleanup resources.
     */
    public void shutdown() {
        Log.d( TAG, "Shutting down LocalEventsRepositoryImpl" );
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
}