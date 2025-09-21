package net.calvuz.qdue.ui.features.dayview.components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * DayViewEventOperations - Comprehensive Event Operations for Day View
 *
 * <p>Handles all interactive operations for day view events including CRUD operations,
 * bulk actions, sharing, clipboard operations, and export functionality.
 * Designed with extensible architecture to support multiple event types.</p>
 *
 * <h3>Supported Operations:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Create, read, update, delete events</li>
 *   <li><strong>Bulk Operations</strong>: Multi-select batch operations</li>
 *   <li><strong>Sharing</strong>: Share events as text via Intent</li>
 *   <li><strong>Clipboard</strong>: Copy event details to system clipboard</li>
 *   <li><strong>Export</strong>: Export day events to files (CSV, text)</li>
 *   <li><strong>Duplication</strong>: Copy events to different dates</li>
 * </ul>
 *
 * <h3>Event Type Support:</h3>
 * <ul>
 *   <li><strong>LocalEvent</strong>: Full CRUD operations supported</li>
 *   <li><strong>WorkScheduleDay</strong>: Read-only operations (view, share, export)</li>
 *   <li><strong>Future Types</strong>: Extensible architecture for new event types</li>
 * </ul>
 *
 * <h3>Error Handling:</h3>
 * <p>All operations return CompletableFuture with proper error handling.
 * Bulk operations support partial success scenarios.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewEventOperations
{

    private static final String TAG = "DayViewEventOperations";

    // ==================== OPERATION RESULT TYPES ====================

    /**
     * Result of bulk operation with success/failure details.
     */
    public static class BulkOperationResult
    {
        private final int mTotalRequested;
        private final int mSuccessCount;
        private final int mFailureCount;
        private final List<String> mFailureReasons;

        public BulkOperationResult(
                int totalRequested, int successCount,
                int failureCount, List<String> failureReasons
        ) {
            mTotalRequested = totalRequested;
            mSuccessCount = successCount;
            mFailureCount = failureCount;
            mFailureReasons = new ArrayList<>( failureReasons );
        }

        public int getTotalRequested() {
            return mTotalRequested;
        }

        public int getSuccessCount() {
            return mSuccessCount;
        }

        public int getFailureCount() {
            return mFailureCount;
        }

        public List<String> getFailureReasons() {
            return new ArrayList<>( mFailureReasons );
        }

        public boolean isCompleteSuccess() {
            return mFailureCount == 0;
        }

        public boolean isPartialSuccess() {
            return mSuccessCount > 0 && mFailureCount > 0;
        }

        public boolean isCompleteFailure() {
            return mSuccessCount == 0;
        }
    }

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final LocalEventsService mLocalEventsService;
    private final LocalEventsUseCases mLocalEventsUseCases;
    private final DayViewStateManager mStateManager;

    // ==================== FORMATTING HELPERS ====================

    private final DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern( "MMMM d, yyyy" );
    private final DateTimeFormatter mTimeFormatter = DateTimeFormatter.ofPattern( "HH:mm" );
    private final DateTimeFormatter mFileNameFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd" );

    // ==================== CONSTRUCTOR ====================

    /**
     * Create DayViewEventOperations with required dependencies.
     *
     * @param context             Application context
     * @param localEventsService  Service for local events operations
     * @param localEventsUseCases Use cases for local events business logic
     * @param stateManager        State manager for current day view state
     */
    public DayViewEventOperations(
            @NonNull Context context,
            @NonNull LocalEventsService localEventsService,
            @NonNull LocalEventsUseCases localEventsUseCases,
            @NonNull DayViewStateManager stateManager
    ) {
        this.mContext = context.getApplicationContext();
        this.mLocalEventsService = localEventsService;
        this.mLocalEventsUseCases = localEventsUseCases;
        this.mStateManager = stateManager;

        Log.d( TAG, "DayViewEventOperations initialized" );
    }

    // ==================== SINGLE EVENT OPERATIONS ====================

    /**
     * Create new local event for current date.
     *
     * @param eventTitle       Event title
     * @param eventDescription Event description
     * @param startTime        Event start time (optional)
     * @param endTime          Event end time (optional)
     * @return CompletableFuture with created event
     */
    @NonNull
    public CompletableFuture<LocalEvent> createLocalEvent(
            @NonNull String eventTitle,
            @Nullable String eventDescription,
            @Nullable String startTime,
            @Nullable String endTime
    ) {
        LocalDate currentDate = mStateManager.getCurrentDate();
        Log.d( TAG, "Creating local event for date: " + currentDate + ", title: " + eventTitle );

        LocalDateTime startTimeDate = LocalDateTime.from(
                currentDate.atTime( LocalTime.parse( startTime ) ) );
        LocalDateTime endTimeDate = LocalDateTime.from(
                currentDate.atTime( LocalTime.parse( endTime ) ) );

        LocalEvent newEvent = LocalEvent.builder()
                .title( eventTitle )
                .description( eventDescription )
                .startTime( startTimeDate )
                .endTime( endTimeDate )
                .build();

        return mLocalEventsService.createEvent(
                        newEvent ) //mLocalEventsUseCases.createEvent( newEvent )
                .thenApply( result -> {
                    if (result.isSuccess()) {
                        Log.d( TAG,
                               "Successfully created local event: " + result.getData().getId() );
                        return result.getData();
                    } else {
                        throw new IllegalStateException( result.getMessage() );
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Failed to create local event", throwable );
                    throw new CompletionException(
                            "Failed to create event: " + throwable.getMessage(), throwable );
                } );
    }

    /**
     * Update existing local event.
     *
     * @param eventId          Event ID to update
     * @param eventTitle       Updated title
     * @param eventDescription Updated description
     * @param startTime        Updated start time
     * @param endTime          Updated end time
     * @return CompletableFuture with updated event
     */
    @NonNull
    public CompletableFuture<LocalEvent> updateLocalEvent(
            @NonNull String eventId,
            @NonNull String eventTitle,
            @Nullable String eventDescription,
            @Nullable String startTime,
            @Nullable String endTime
    ) {
        Log.d( TAG, "Updating local event: " + eventId );

        return mLocalEventsService.getEventById( eventId )
                .thenCompose( result -> {
                    if (result.isFailure()) {
                        throw new CompletionException(
                                new IllegalArgumentException( "Event not found: " + eventId ) );
                    }

                    // Get Event data
                    LocalEvent existing = result.getData();

                    // Update Event fields
                    existing.setTitle( eventTitle );
                    existing.setDescription( eventDescription );
                    existing.setStartTime( LocalDateTime.parse( startTime ) );
                    existing.setEndTime( LocalDateTime.parse( endTime ) );
                    existing.setUpdatedAt( System.currentTimeMillis() );

                    // Save Event
                    return mLocalEventsUseCases.updateEvent( existing );
                } )
                .thenApply( updatedEvent -> {
                    if (updatedEvent.isFailure()) {
                        throw new CompletionException(
                                new IllegalArgumentException(
                                        "Failed to update event: " + eventId ) );
                    }

                    Log.d( TAG, "Successfully updated local event: " + eventId );
                    return updatedEvent.getData();
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Failed to update local event: " + eventId, throwable );
                    throw new CompletionException(
                            "Failed to update event: " + throwable.getMessage(), throwable );
                } );
    }

    /**
     * Delete single local event.
     *
     * @param eventId Event ID to delete
     * @return CompletableFuture with deletion success
     */
    @NonNull
    public CompletableFuture<Boolean> deleteLocalEvent(@NonNull String eventId) {
        Log.d( TAG, "Deleting local event: " + eventId );

        return mLocalEventsUseCases.deleteEvent( eventId )
                .thenApply( result -> {
                    if (result.isSuccess()) {
                        Log.d( TAG, "Successfully deleted local event: " + eventId );
                    } else {
                        Log.w( TAG, "Failed to delete local event: " + eventId );
                    }
                    return result.isSuccess();
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error deleting local event: " + eventId, throwable );
                    throw new CompletionException(
                            "Failed to delete event: " + throwable.getMessage(), throwable );
                } );
    }

    /**
     * Duplicate local event to another date.
     *
     * @param eventId    Event ID to duplicate
     * @param targetDate Target date for duplication
     * @return CompletableFuture with duplicated event
     */
    @NonNull
    public CompletableFuture<LocalEvent> duplicateLocalEvent(@NonNull String eventId, @NonNull LocalDate targetDate) {
        Log.d( TAG, "Duplicating local event " + eventId + " to date: " + targetDate );

        return mLocalEventsUseCases.getEventById( eventId )
                .thenCompose( result -> {
                    if (result.isFailure()) {
                        throw new CompletionException(
                                new IllegalArgumentException( "Event not found: " + eventId ) );
                    }

                    // Create duplicate with new date
                    assert result.getData() != null;
                    LocalEvent originalEvent = result.getData();

                    // Opzione 1: Usando Duration (raccomandato)
                    Duration eventDuration = Duration.between( originalEvent.getStartTime(),
                                                               originalEvent.getEndTime() );

                    LocalDateTime newStartTime = LocalDateTime.of( targetDate,
                                                                   originalEvent.getStartTime().toLocalTime() );
                    LocalDateTime newEndTime = newStartTime.plus( eventDuration );

                    LocalEvent duplicateEvent = LocalEvent.builder()
                            .copyFrom( originalEvent )

                            .title( originalEvent.getTitle() + " (Copy)" )
                            .description( originalEvent.getDescription() )
                            .startTime( newStartTime )
                            .endTime( newEndTime )
                            .createdAt( System.currentTimeMillis() )
                            .build();

                    return mLocalEventsUseCases.createEvent( duplicateEvent );
                } )
                .thenApply( result -> {
                    if (result.isFailure()) {
                        throw new CompletionException(
                                new IllegalArgumentException(
                                        "Failed to duplicate event: " + eventId ) );
                    }
                    Log.d( TAG, "Successfully duplicated event to: " + result.getData().getId() );
                    return result.getData();
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Failed to duplicate event: " + eventId, throwable );
                    throw new CompletionException(
                            "Failed to duplicate event: " + throwable.getMessage(), throwable );
                } );
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Delete multiple selected events.
     *
     * @param selectedEventIds Set of event IDs to delete
     * @return CompletableFuture with bulk operation result
     */
    @NonNull
    public CompletableFuture<BulkOperationResult> deleteSelectedEvents(@NonNull Set<String> selectedEventIds) {
        Log.d( TAG, "Deleting " + selectedEventIds.size() + " selected events" );

        if (selectedEventIds.isEmpty()) {
            return CompletableFuture.completedFuture(
                    new BulkOperationResult( 0, 0, 0, new ArrayList<>() ) );
        }

        List<CompletableFuture<Boolean>> deletionTasks = selectedEventIds.stream()
                .map( eventId -> deleteLocalEvent( eventId )
                        .exceptionally( throwable -> {
                            Log.e( TAG, "Failed to delete event in bulk operation: " + eventId,
                                   throwable );
                            return false;
                        } ) )
                .collect( Collectors.toList() );

        return CompletableFuture.allOf( deletionTasks.toArray( new CompletableFuture[0] ) )
                .thenApply( v -> {
                    int successCount = 0;
                    int failureCount = 0;
                    List<String> failureReasons = new ArrayList<>();

                    for (int i = 0; i < deletionTasks.size(); i++) {
                        try {
                            boolean success = deletionTasks.get( i ).join();
                            if (success) {
                                successCount++;
                            } else {
                                failureCount++;
                                failureReasons.add( "Failed to delete event at index " + i );
                            }
                        } catch (Exception e) {
                            failureCount++;
                            failureReasons.add(
                                    "Exception deleting event at index " + i + ": " + e.getMessage() );
                        }
                    }

                    BulkOperationResult result = new BulkOperationResult(
                            selectedEventIds.size(), successCount, failureCount, failureReasons );

                    Log.d( TAG, "Bulk deletion completed - Success: " + successCount +
                            ", Failed: " + failureCount );
                    return result;
                } );
    }

    /**
     * Duplicate multiple events to target date.
     *
     * @param selectedEventIds Set of event IDs to duplicate
     * @param targetDate       Target date for duplication
     * @return CompletableFuture with bulk operation result
     */
    @NonNull
    public CompletableFuture<BulkOperationResult> duplicateSelectedEvents(
            @NonNull Set<String> selectedEventIds,
            @NonNull LocalDate targetDate
    ) {

        Log.d( TAG,
               "Duplicating " + selectedEventIds.size() + " selected events to date: " + targetDate );

        if (selectedEventIds.isEmpty()) {
            return CompletableFuture.completedFuture(
                    new BulkOperationResult( 0, 0, 0, new ArrayList<>() ) );
        }

        List<CompletableFuture<LocalEvent>> duplicationTasks = selectedEventIds.stream()
                .map( eventId -> duplicateLocalEvent( eventId, targetDate )
                        .exceptionally( throwable -> {
                            Log.e( TAG, "Failed to duplicate event in bulk operation: " + eventId,
                                   throwable );
                            return null;
                        } ) )
                .collect( Collectors.toList() );

        return CompletableFuture.allOf( duplicationTasks.toArray( new CompletableFuture[0] ) )
                .thenApply( v -> {
                    int successCount = 0;
                    int failureCount = 0;
                    List<String> failureReasons = new ArrayList<>();

                    for (int i = 0; i < duplicationTasks.size(); i++) {
                        try {
                            LocalEvent result = duplicationTasks.get( i ).join();
                            if (result != null) {
                                successCount++;
                            } else {
                                failureCount++;
                                failureReasons.add( "Failed to duplicate event at index " + i );
                            }
                        } catch (Exception e) {
                            failureCount++;
                            failureReasons.add(
                                    "Exception duplicating event at index " + i + ": " + e.getMessage() );
                        }
                    }

                    BulkOperationResult result = new BulkOperationResult(
                            selectedEventIds.size(), successCount, failureCount, failureReasons );

                    Log.d( TAG, "Bulk duplication completed - Success: " + successCount +
                            ", Failed: " + failureCount );
                    return result;
                } );
    }

    // ==================== SHARING OPERATIONS ====================

    /**
     * Generate shareable text for selected events.
     *
     * @param selectedEventIds Set of event IDs to share
     * @return CompletableFuture with formatted share text
     */
    @NonNull
    public CompletableFuture<String> shareSelectedEvents(@NonNull Set<String> selectedEventIds) {
        Log.d( TAG, "Generating share text for " + selectedEventIds.size() + " selected events" );

        if (selectedEventIds.isEmpty()) {
            return CompletableFuture.completedFuture( "No events selected for sharing." );
        }

        LocalDate currentDate = mStateManager.getCurrentDate();
        StringBuilder shareText = new StringBuilder();
        shareText.append( "Events for " ).append( currentDate.format( mDateFormatter ) ).append(
                ":\n\n" );

        // Get local events
        List<LocalEvent> localEvents = mStateManager.getLocalEvents();
        List<LocalEvent> selectedLocalEvents = localEvents.stream()
                .filter( event -> selectedEventIds.contains( event.getId() ) )
                .collect( Collectors.toList() );

        // Get work schedule days
        List<WorkScheduleDay> workScheduleDays = mStateManager.getWorkScheduleDays();
        List<WorkScheduleDay> selectedWorkDays = workScheduleDays.stream()
                .filter( day -> selectedEventIds.contains( day.getId() ) )
                .collect( Collectors.toList() );

        // Format local events
        if (!selectedLocalEvents.isEmpty()) {
            shareText.append( "Personal Events:\n" );
            for (LocalEvent event : selectedLocalEvents) {
                shareText.append( "• " ).append( formatEventForSharing( event ) ).append( "\n" );
            }
            shareText.append( "\n" );
        }

        // Format work schedule
        if (!selectedWorkDays.isEmpty()) {
            shareText.append( "Work Schedule:\n" );
            for (WorkScheduleDay day : selectedWorkDays) {
                shareText.append( "• " ).append( formatWorkScheduleForSharing( day ) ).append(
                        "\n" );
            }
            shareText.append( "\n" );
        }

        shareText.append( "Generated by QDue Calendar" );

        String finalText = shareText.toString();
        Log.d( TAG, "Generated share text with length: " + finalText.length() );

        return CompletableFuture.completedFuture( finalText );
    }

    /**
     * Copy selected events to system clipboard.
     *
     * @param selectedEventIds Set of event IDs to copy
     * @return CompletableFuture with success result
     */
    @NonNull
    public CompletableFuture<Boolean> copySelectedEventsToClipboard(@NonNull Set<String> selectedEventIds) {
        Log.d( TAG, "Copying " + selectedEventIds.size() + " selected events to clipboard" );

        return shareSelectedEvents( selectedEventIds )
                .thenApply( shareText -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(
                                Context.CLIPBOARD_SERVICE );
                        ClipData clip = ClipData.newPlainText( "QDue Events", shareText );
                        clipboard.setPrimaryClip( clip );

                        Log.d( TAG, "Successfully copied events to clipboard" );
                        return true;
                    } catch (Exception e) {
                        Log.e( TAG, "Failed to copy events to clipboard", e );
                        return false;
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error generating text for clipboard", throwable );
                    return false;
                } );
    }

    // ==================== EXPORT OPERATIONS ====================

    /**
     * Export all events for current day to file.
     *
     * @param targetDate Date to export events for
     * @return CompletableFuture with exported file path
     */
    @NonNull
    public CompletableFuture<String> exportDayEvents(@NonNull LocalDate targetDate) {
        Log.d( TAG, "Exporting events for date: " + targetDate );

        return CompletableFuture.supplyAsync( () -> {
            try {
                File exportFile = createExportFile( targetDate );

                List<LocalEvent> localEvents = mStateManager.getLocalEvents();
                List<WorkScheduleDay> workScheduleDays = mStateManager.getWorkScheduleDays();

                try (FileWriter writer = new FileWriter( exportFile )) {
                    writer.write( "QDue Calendar Export\n" );
                    writer.write( "Date: " + targetDate.format( mDateFormatter ) + "\n" );
                    writer.write( "Export Time: " + java.time.LocalDateTime.now() + "\n\n" );

                    // Export local events
                    if (!localEvents.isEmpty()) {
                        writer.write( "Personal Events (" + localEvents.size() + "):\n" );
                        writer.write( "=".repeat( 40 ) + "\n" );
                        for (LocalEvent event : localEvents) {
                            writer.write( formatEventForExport( event ) + "\n\n" );
                        }
                    }

                    // Export work schedule
                    if (!workScheduleDays.isEmpty()) {
                        writer.write( "Work Schedule (" + workScheduleDays.size() + "):\n" );
                        writer.write( "=".repeat( 40 ) + "\n" );
                        for (WorkScheduleDay day : workScheduleDays) {
                            writer.write( formatWorkScheduleForExport( day ) + "\n\n" );
                        }
                    }

                    if (localEvents.isEmpty() && workScheduleDays.isEmpty()) {
                        writer.write( "No events found for this date.\n" );
                    }
                }

                Log.d( TAG, "Successfully exported events to: " + exportFile.getAbsolutePath() );
                return exportFile.getAbsolutePath();
            } catch (IOException e) {
                Log.e( TAG, "Failed to export events for date: " + targetDate, e );
                throw new CompletionException( "Failed to export events: " + e.getMessage(), e );
            }
        } );
    }

    /**
     * Export selected events to file.
     *
     * @param selectedEventIds Set of event IDs to export
     * @return CompletableFuture with exported file path
     */
    @NonNull
    public CompletableFuture<String> exportSelectedEvents(@NonNull Set<String> selectedEventIds) {
        Log.d( TAG, "Exporting " + selectedEventIds.size() + " selected events" );

        LocalDate currentDate = mStateManager.getCurrentDate();

        return shareSelectedEvents( selectedEventIds )
                .thenApply( shareText -> {
                    try {
                        File exportFile = createExportFile( currentDate, "selected" );

                        try (FileWriter writer = new FileWriter( exportFile )) {
                            writer.write( shareText );
                        }

                        Log.d( TAG,
                               "Successfully exported selected events to: " + exportFile.getAbsolutePath() );
                        return exportFile.getAbsolutePath();
                    } catch (IOException e) {
                        Log.e( TAG, "Failed to export selected events", e );
                        throw new CompletionException(
                                "Failed to export selected events: " + e.getMessage(), e );
                    }
                } );
    }

    // ==================== PRIVATE FORMATTING METHODS ====================

    @NonNull
    private String formatEventForSharing(@NonNull LocalEvent event) {
        StringBuilder formatted = new StringBuilder();
        formatted.append( event.getTitle() );

        if (!event.getStartTime().toString().isEmpty()) {
            formatted.append( " (" ).append( event.getStartTime() );
            if (!event.getEndTime().toString().isEmpty()) {
                formatted.append( " - " ).append( event.getEndTime() );
            }
            formatted.append( ")" );
        }

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            formatted.append( " - " ).append( event.getDescription() );
        }

        return formatted.toString();
    }

    @NonNull
    private String formatWorkScheduleForSharing(@NonNull WorkScheduleDay day) {
        StringBuilder formatted = new StringBuilder();
        formatted.append( "Work Schedule" );

        // Add work schedule specific formatting based on WorkScheduleDay structure
        // This would depend on the actual structure of WorkScheduleDay
        if (day.getShiftCount() > 0) {
            for (int x = 0; x < day.getShiftCount(); x++)
                formatted.append( " - " ).append( day.getShift( x ).getDisplayName() ).append(
                        "\n" );
        }

        return formatted.toString();
    }

    @NonNull
    private String formatEventForExport(@NonNull LocalEvent event) {
        StringBuilder formatted = new StringBuilder();
        formatted.append( "Title: " ).append( event.getTitle() ).append( "\n" );

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            formatted.append( "Description: " ).append( event.getDescription() ).append( "\n" );
        }

        if (!event.getStartTime().toString().isEmpty()) {
            formatted.append( "Start Time: " ).append( event.getStartTime() ).append( "\n" );
        }

        if (!event.getEndTime().toString().isEmpty()) {
            formatted.append( "End Time: " ).append( event.getEndTime() ).append( "\n" );
        }

        formatted.append( "Created: " ).append( new java.util.Date( event.getCreatedAt() ) ).append(
                "\n" );

        if (event.getUpdatedAt() > 0) {
            formatted.append( "Updated: " ).append(
                    new java.util.Date( event.getUpdatedAt() ) ).append( "\n" );
        }

        return formatted.toString();
    }

    @NonNull
    private String formatWorkScheduleForExport(@NonNull WorkScheduleDay day) {
        StringBuilder formatted = new StringBuilder();
        formatted.append( "Work Schedule Day\n" );

        if (day.getShiftCount() > 0) {
            for (int x = 0; x < day.getShiftCount(); x++)
                formatted
                        .append( "Shift: " )
                        .append( day.getShift( x ).getDisplayName() )
                        .append( "\n" );

            // Add more shift details based on actual WorkScheduleDay structure
        }

        return formatted.toString();
    }

    @NonNull
    private File createExportFile(@NonNull LocalDate date) throws IOException {
        return createExportFile( date, null );
    }

    @NonNull
    private File createExportFile(@NonNull LocalDate date, @Nullable String suffix) throws IOException {
        File exportDir = new File( mContext.getExternalFilesDir( null ), "exports" );
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String fileName = "qdue_events_" + date.format( mFileNameFormatter );
        if (suffix != null) {
            fileName += "_" + suffix;
        }
        fileName += ".txt";

        return new File( exportDir, fileName );
    }
}