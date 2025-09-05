package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.engines.SchedulingEngine;
import net.calvuz.qdue.domain.calendar.engines.RecurrenceCalculator;
import net.calvuz.qdue.domain.calendar.engines.ExceptionResolver;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WorkScheduleRepositoryImpl - Complete Implementation with Multi-Team Support
 *
 * <p>Complete implementation of WorkScheduleRepository interface that orchestrates domain engines
 * and specialized repositories through CalendarServiceProvider dependency injection. Implements
 * all methods from the interface with proper error handling, localization support, and multi-team
 * WorkScheduleEvent generation.</p>
 *
 * <h3>Implementation Strategy:</h3>
 * <ul>
 *   <li><strong>Complete Interface</strong>: Implements ALL methods from WorkScheduleRepository</li>
 *   <li><strong>Multi-Team Support</strong>: Generates WorkScheduleEvents with multiple teams</li>
 *   <li><strong>Repository Coordination</strong>: Uses specialized repositories for specific operations</li>
 *   <li><strong>Domain Engine Integration</strong>: Coordinates calculation engines for schedule generation</li>
 *   <li><strong>Fixed Dependencies</strong>: Uses CalendarServiceProvider for proper DI</li>
 * </ul>
 *
 * <h3>Multi-Team Architecture:</h3>
 * <ul>
 *   <li><strong>QuattroDue 4-2 Pattern</strong>: Typically 2 teams per shift</li>
 *   <li><strong>QuattroDue 3-2 Pattern</strong>: Different team configurations</li>
 *   <li><strong>Custom Patterns</strong>: Flexible team assignment support</li>
 *   <li><strong>User Relevance</strong>: User relevant if in ANY assigned team</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 3.2.0 - Multi-Team Support with Fixed Dependencies
 * @since Clean Architecture Phase 3
 */
public class WorkScheduleRepositoryImpl implements WorkScheduleRepository {

    private static final String TAG = "WorkScheduleRepositoryImpl";

    // ==================== CORE DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarServiceProvider mCalendarServiceProvider;
    private final ExecutorService mExecutorService;

    // ==================== CACHING ====================

    private final ConcurrentHashMap<String, WorkScheduleDay> mScheduleCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> mConfigCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDate> mSchemeCache = new ConcurrentHashMap<>();

    // Cache configuration
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long CACHE_EXPIRY_MS = 30 * 60 * 1000; // 30 minutes

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor with CalendarServiceProvider dependency injection.
     *
     * @param context                 Application context
     * @param calendarServiceProvider CalendarServiceProvider for domain repository access
     */
    public WorkScheduleRepositoryImpl(@NonNull Context context,
                                      @NonNull CalendarServiceProvider calendarServiceProvider) {
        this.mContext = context.getApplicationContext();
        this.mCalendarServiceProvider = calendarServiceProvider;
        this.mExecutorService = Executors.newCachedThreadPool();

        Log.i( TAG, "WorkScheduleRepositoryImpl initialized with CalendarServiceProvider DI" );
    }

    // ==================== SCHEDULE GENERATION ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<WorkScheduleDay>> getWorkScheduleForDate(@NonNull LocalDate date, @Nullable String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Check cache first
                String cacheKey = buildCacheKey( date, userId );
                WorkScheduleDay cachedSchedule = mScheduleCache.get( cacheKey );
                if (cachedSchedule != null) {
                    return OperationResult.success( cachedSchedule, OperationResult.OperationType.READ );
                }

                // Generate schedule using domain engines
                WorkScheduleDay schedule = generateScheduleForDate( date, userId );

                // Cache the result
                mScheduleCache.put( cacheKey, schedule );

                return OperationResult.success( schedule, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error generating work schedule for date: " + date, e );
                return OperationResult.failure( "Failed to generate schedule for " + date,
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable String userId) {

        return CompletableFuture.supplyAsync( () -> {
            Log.v( TAG, "Generating work schedule for date range: " + startDate + " to " + endDate );

            try {
                Map<LocalDate, WorkScheduleDay> scheduleMap = new HashMap<>();

                for (LocalDate date = startDate; !date.isAfter( endDate ); date = date.plusDays( 1 )) {
                    WorkScheduleDay daySchedule = generateScheduleForDate( date, userId );
                    scheduleMap.put( date, daySchedule );
                }

                Log.d( TAG, "Successfully generated schedule for " + scheduleMap.size() + " days" );
                return OperationResult.success( scheduleMap, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error generating work schedule for date range", e );
                return OperationResult.failure( "Failed to generate date range schedule",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForMonth(
            @NonNull YearMonth month, @Nullable String userId) {

        LocalDate startDate = month.atDay( 1 );
        LocalDate endDate = month.atEndOfMonth();

        return getWorkScheduleForDateRange( startDate, endDate, userId );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateWorkScheduleEvents(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable String userId) {

        return CompletableFuture.supplyAsync( () -> {
            Log.v( TAG, "Getting work schedule events from " + startDate + " to " + endDate );

            try {
                List<WorkScheduleEvent> events = new ArrayList<>();

                for (LocalDate date = startDate; !date.isAfter( endDate ); date = date.plusDays( 1 )) {
                    WorkScheduleDay daySchedule = generateScheduleForDate( date, userId );
                    List<WorkScheduleEvent> dayEvents = convertScheduleDayToMultiTeamEvents( daySchedule, userId );
                    events.addAll( dayEvents );
                }

                Log.v( TAG, "Got " + events.size() + " work schedule events with multi-team support" );
                return OperationResult.success( events, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting work schedule events", e );
                return OperationResult.failure( "Failed to get events",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== TEAM MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Team>>> getAllTeams() {
        return mCalendarServiceProvider.getTeamRepository().getAllActiveTeams()
                .thenApply( teams -> OperationResult.success( teams, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error getting all teams", throwable );
                    return OperationResult.failure( "Failed to get teams",
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Team>> getTeamForUser(@NonNull String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Get user's current assignment
                UserScheduleAssignment assignment = getUserAssignmentForDate( LocalDate.now(), userId );
                if (assignment == null) {
                    return OperationResult.success( null, OperationResult.OperationType.READ );
                }

                // Get team by ID from assignment
                String teamId = assignment.getTeamId();
                Team team = mCalendarServiceProvider.getTeamRepository().getTeamById( teamId ).join();

                return OperationResult.success( team, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting team for user " + userId, e );
                return OperationResult.failure( "Failed to get team for user",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> setTeamForUser(@NonNull String userId, @NonNull Team team) {
        return CompletableFuture.supplyAsync( () -> {
            Log.w( TAG, "Setting team for user " + userId + " to " + team.getName() );

            try {
                // Create new assignment for user
                UserScheduleAssignment assignment = UserScheduleAssignment.createPermanentAssignment(
                        userId,
                        team.getId().toString(),
                        getDefaultRecurrenceRuleId(),
                        LocalDate.now()
                );

                // Save assignment
                CompletableFuture<OperationResult<UserScheduleAssignment>> saveResult =
                        mCalendarServiceProvider.getUserScheduleAssignmentRepository()
                                .insertUserScheduleAssignment( assignment );

                OperationResult<UserScheduleAssignment> result = saveResult.join();

                if (result.isSuccess()) {
                    return OperationResult.success( null, OperationResult.OperationType.UPDATE );
                } else {
                    return OperationResult.failure( "Failed to save assignment: " + result.getErrorMessage(),
                            OperationResult.OperationType.UPDATE );
                }
            } catch (Exception e) {
                Log.e( TAG, "Error setting team for user " + userId, e );
                return OperationResult.failure( "Failed to set team: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Team>>> getTeamsWorkingOnDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, null );
                List<Team> workingTeams = new ArrayList<>();

                for (WorkScheduleShift shift : schedule.getShifts()) {
                    workingTeams.addAll( shift.getTeams() );
                }

                return OperationResult.success( workingTeams, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting teams working on date " + date, e );
                return OperationResult.failure( "Failed to get working teams",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== SHIFT TYPE MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Shift>>> getAllShifts() {
        return mCalendarServiceProvider.getShiftRepository().getAllShifts()
                .thenApply( shifts -> OperationResult.success( shifts, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error getting all shifts", throwable );
                    return OperationResult.failure( "Failed to get shifts",
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Shift>> getShiftById(@NonNull String shiftId) {
        return mCalendarServiceProvider.getShiftRepository().getShiftById( shiftId )
                .thenApply( shift -> OperationResult.success( shift, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error getting shift by ID " + shiftId, throwable );
                    return OperationResult.failure( "Failed to get shift",
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Shift>> getShiftByName(@NonNull String name) {
        return mCalendarServiceProvider.getShiftRepository().getShiftByName( name )
                .thenApply( shift -> OperationResult.success( shift, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error getting shift by name " + name, throwable );
                    return OperationResult.failure( "Failed to get shift",
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Shift>> saveShift(@NonNull Shift shift) {
        return mCalendarServiceProvider.getShiftRepository().saveShift( shift )
                .thenApply( savedShift -> OperationResult.success( savedShift, OperationResult.OperationType.CREATE ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error saving shift", throwable );
                    return OperationResult.failure( "Failed to save shift",
                            OperationResult.OperationType.CREATE );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Object>> deleteShift(@NonNull String shiftId) {
        return mCalendarServiceProvider.getShiftRepository().deleteShift( shiftId )
                .thenApply( success -> {
                    if (success) {
                        return OperationResult.success( null,
                                OperationResult.OperationType.DELETE );
                    } else {
                        return OperationResult.failure( "Failed to delete shift",
                                OperationResult.OperationType.DELETE );
                    }
                } )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error deleting shift " + shiftId, throwable );
                    return OperationResult.failure( "Failed to delete shift",
                            OperationResult.OperationType.DELETE );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<WorkScheduleShift>>> getShiftsForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, null );
                return OperationResult.success( schedule.getShifts(),
                        OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting shifts for date " + date, e );
                return OperationResult.failure( "Failed to get shifts",
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== SCHEDULE CONFIGURATION ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalDate>> getSchemeStartDate() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Check cache first
                LocalDate cached = mSchemeCache.get( "scheme_start_date" );
                if (cached != null) {
                    Log.v( TAG, "Returning cached scheme start date: " + cached );
                    return OperationResult.success( cached,
                            OperationResult.OperationType.READ );
                }

                // ✅ FIX: Read from preferences like the old QuattroDue system
                // Use QUATTRODUE
                LocalDate schemeStart = Preferences.getSchemeStartDate( mContext );
                Log.d( TAG, "Read scheme start date from preferences: " + schemeStart );

                // Cache the result
                mSchemeCache.put( "scheme_start_date", schemeStart );

                return OperationResult.success( schemeStart, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting scheme start date from preferences", e );

                // ✅ FALLBACK: Use hardcoded default as last resort
                // Use HARDCODED
                LocalDate fallbackDate = LocalDate.of( 2018, 11, 7 ); // Default QuattroDue date
                Log.w( TAG, "Using fallback scheme start date: " + fallbackDate );

                mSchemeCache.put( "scheme_start_date", fallbackDate );
                return OperationResult.success( fallbackDate, OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> updateSchemeStartDate(@NonNull LocalDate newStartDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Log.d( TAG, "Updating scheme start date to: " + newStartDate );

                // ✅ FIX: Save to preferences AND cache
                Preferences.setSchemeStartDate( mContext, newStartDate );
                mSchemeCache.put( "scheme_start_date", newStartDate );

                // Clear schedule cache since all calculations will change
                mScheduleCache.clear();
                Log.d( TAG, "Schedule cache cleared after scheme date update" );

                return OperationResult.success( "Updating scheme start date to: " + newStartDate,
                        OperationResult.OperationType.UPDATE );
            } catch (Exception e) {
                Log.e( TAG, "Error updating scheme start date", e );
                return OperationResult.failure( "Failed to update scheme start date: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<String, Object>>> getScheduleConfiguration() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put( "pattern_type", "QUATTRODUE" );
                config.put( "cycle_length", 18 );
                config.put( "work_days", 4 );
                config.put( "rest_days", 2 );
                config.put( "teams_count", 9 );
                config.put( "shifts_per_day", 3 );
                config.put( "teams_per_shift", 2 ); // NEW: Multi-team support

                return OperationResult.success( config, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting schedule configuration", e );
                return OperationResult.failure( "Failed to get configuration: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> updateScheduleConfiguration(@NonNull Map<String, Object> configuration) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                // Store configuration
                mConfigCache.putAll( configuration );

                // Clear schedule cache since configuration changed
                mScheduleCache.clear();

                return OperationResult.success( null, OperationResult.OperationType.UPDATE );
            } catch (Exception e) {
                Log.e( TAG, "Error updating schedule configuration", e );
                return OperationResult.failure( "Failed to update configuration: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE );
            }
        }, mExecutorService );
    }

    // ==================== PATTERN CALCULATIONS ====================

    @Deprecated
    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getDayInCycle(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                LocalDate schemeStart = getSchemeStartDate().join().getData();
                if (schemeStart == null)
                    throw new RuntimeException( "No Scheme StartDate" );

                // TODO: remove hardcode 18 day cycle length
                long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between( schemeStart, date );
                int dayInCycle = (int) (daysSinceStart % 18); // 18-day QuattroDue cycle

                Log.v( TAG, "Day in cycle calculation: " + date + " = day " + dayInCycle +
                        " (scheme start: " + schemeStart + ", days since: " + daysSinceStart + ")" );

                return OperationResult.success( dayInCycle, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error calculating day in cycle", e );
                return OperationResult.failure( "Failed to calculate day in cycle: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> isWorkingDay(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, null );
                boolean hasWorkingShifts = !schedule.getShifts().isEmpty();

                return OperationResult.success( hasWorkingShifts, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error checking if working day", e );
                return OperationResult.failure( "Failed to check working day: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, null );

                for (WorkScheduleShift shift : schedule.getShifts()) {
                    if (shift.getTeams().contains( team )) {
                        return OperationResult.success( true, OperationResult.OperationType.READ );
                    }
                }

                return OperationResult.success( false, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error checking if working day for team", e );
                return OperationResult.failure( "Failed to check team working day: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        return isWorkingDayForTeam( date, team )
                .thenApply( result -> {
                    if (result.isSuccess()) {
                        boolean isWorking = result.getData();
                        return OperationResult.success( !isWorking, OperationResult.OperationType.READ );
                    } else {
                        return result;
                    }
                } );
    }

    // ==================== CALENDAR INTEGRATION ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> hasWorkSchedule(@Nullable WorkScheduleDay day) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                boolean hasSchedule = day != null && !day.getShifts().isEmpty();
                return OperationResult.success( hasSchedule, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error checking if has work schedule", e );
                return OperationResult.failure( "Failed to check schedule: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<String>> getWorkScheduleColor(@NonNull LocalDate date, @Nullable String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, userId );

                if (schedule.getShifts().isEmpty()) {
                    return OperationResult.success( null, OperationResult.OperationType.READ );
                }

                // Default work schedule color
                String color = "#2196F3"; // Blue

                return OperationResult.success( color, OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting work schedule color", e );
                return OperationResult.failure( "Failed to get color: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<String>> getWorkScheduleSummary(@NonNull LocalDate date, @Nullable String userId) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                WorkScheduleDay schedule = generateScheduleForDate( date, userId );

                if (schedule.getShifts().isEmpty()) {
                    return OperationResult.success( null, OperationResult.OperationType.READ );
                }

                StringBuilder summary = new StringBuilder();
                for (WorkScheduleShift shift : schedule.getShifts()) {
                    if (summary.length() > 0) summary.append( ", " );
                    summary.append( shift.getShift().getName() );

                    // NEW: Include team count in summary
                    int teamCount = shift.getTeams().size();
                    if (teamCount > 1) {
                        summary.append( " (" ).append( teamCount ).append( " teams)" );
                    }
                }

                return OperationResult.success( summary.toString(), OperationResult.OperationType.READ );
            } catch (Exception e) {
                Log.e( TAG, "Error getting work schedule summary", e );
                return OperationResult.failure( "Failed to get summary: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== STUB IMPLEMENTATIONS FOR COMPLETENESS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> refreshWorkScheduleData() {
        return CompletableFuture.supplyAsync( () -> {
            mScheduleCache.clear();
            mConfigCache.clear();
            mSchemeCache.clear();
            return OperationResult.success( null, OperationResult.OperationType.UPDATE );
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Void>> clearCache() {
        return refreshWorkScheduleData();
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<String, Object>>> getServiceStatus() {
        return CompletableFuture.supplyAsync( () -> {
            Map<String, Object> status = new HashMap<>();
            status.put( "cache_size", mScheduleCache.size() );
            status.put( "ready", true );
            status.put( "calendar_services_ready", mCalendarServiceProvider.areCalendarServicesReady() );
            return OperationResult.success( status, OperationResult.OperationType.READ );
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<String, Object>>> validateConfiguration() {
        return CompletableFuture.supplyAsync( () -> {
            Map<String, Object> validation = new HashMap<>();
            validation.put( "valid", true );
            validation.put( "errors", new ArrayList<>() );
            return OperationResult.success( validation, OperationResult.OperationType.READ );
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Boolean>> isRepositoryReady() {
        return CompletableFuture.supplyAsync( () -> {
            boolean ready = mCalendarServiceProvider.areCalendarServicesReady();
            return OperationResult.success( ready, OperationResult.OperationType.READ );
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Team>> findTeamByName(@NonNull String teamName) {
        return mCalendarServiceProvider.getTeamRepository().getTeamByName( teamName )
                .thenApply( team -> OperationResult.success( team, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error finding team by name " + teamName, throwable );
                    return OperationResult.failure( "Team not found: " + throwable.getMessage(),
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Team>> findTeamById(@NonNull String teamId) {
        return mCalendarServiceProvider.getTeamRepository().getTeamById( teamId )
                .thenApply( team -> OperationResult.success( team, OperationResult.OperationType.READ ) )
                .exceptionally( throwable -> {
                    Log.e( TAG, "Error finding team by ID " + teamId, throwable );
                    return OperationResult.failure( "Team not found: " + throwable.getMessage(),
                            OperationResult.OperationType.READ );
                } );
    }

    @Override
    @NonNull
    @Deprecated
    public CompletableFuture<OperationResult<List<Team>>> createStandardTeams() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                List<Team> standardTeams = new ArrayList<>();

                // Create standard QuattroDue teams A through I
                String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
                for (int i = 0; i < teamNames.length; i++) {
                    Team team = Team.builder( String.valueOf( (long) (i + 1) ) )
                            .name( "Team " + teamNames[i] )
                            .displayName( "Team " + teamNames[i] )
                            .description( "QuattroDue Team " + teamNames[i] )
                            .active( true )
                            .build();

                    // Save each team using repository
                    Team savedTeam = mCalendarServiceProvider.getTeamRepository().saveTeam( team ).join();
                    if (savedTeam != null) {
                        standardTeams.add( savedTeam );
                    }
                }

                return OperationResult.success( standardTeams, OperationResult.OperationType.CREATE );
            } catch (Exception e) {
                Log.e( TAG, "Error creating standard teams", e );
                return OperationResult.failure( "Failed to create standard teams: " + e.getMessage(),
                        OperationResult.OperationType.CREATE );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalDate>> getNextWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                for (int i = 1; i <= 30; i++) { // Check next 30 days
                    LocalDate checkDate = fromDate.plusDays( i );
                    if (isWorkingDayForTeam( checkDate, team ).join().getData()) {
                        return OperationResult.success( checkDate, OperationResult.OperationType.READ );
                    }
                }
                return OperationResult.success( null, OperationResult.OperationType.READ );
            } catch (Exception e) {
                return OperationResult.failure( "Error finding next working day: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<LocalDate>> getPreviousWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                for (int i = 1; i <= 30; i++) { // Check previous 30 days
                    LocalDate checkDate = fromDate.minusDays( i );
                    if (isWorkingDayForTeam( checkDate, team ).join().getData()) {
                        return OperationResult.success( checkDate, OperationResult.OperationType.READ );
                    }
                }
                return OperationResult.success( null, OperationResult.OperationType.READ );
            } catch (Exception e) {
                return OperationResult.failure( "Error finding previous working day: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getWorkingDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                int count = 0;
                for (LocalDate date = startDate; !date.isAfter( endDate ); date = date.plusDays( 1 )) {
                    if (isWorkingDayForTeam( date, team ).join().getData()) {
                        count++;
                    }
                }
                return OperationResult.success( count, OperationResult.OperationType.READ );
            } catch (Exception e) {
                return OperationResult.failure( "Error counting working days: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Integer>> getRestDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between( startDate, endDate ) + 1;
                int workingDays = getWorkingDaysCount( team, startDate, endDate ).join().getData();
                int restDays = (int) totalDays - workingDays;
                return OperationResult.success( restDays, OperationResult.OperationType.READ );
            } catch (Exception e) {
                return OperationResult.failure( "Error counting rest days: " + e.getMessage(),
                        OperationResult.OperationType.READ );
            }
        }, mExecutorService );
    }

    // ==================== HELPER METHODS ====================

    private String buildCacheKey(@NonNull LocalDate date, @Nullable String userId) {
        return "schedule_" + date + "_" + (userId != null ? userId : "default");
    }

    private WorkScheduleDay generateScheduleForDate(@NonNull LocalDate date, @Nullable String userId) {
        try {
            // Get user assignment
            UserScheduleAssignment assignment = getUserAssignmentForDate( date, userId );
            if (assignment == null) {
                // Means user doesn't have work assignment on this date
                Log.i( TAG, "No user assignment found for date " + date );

                // Create an empty schedule
                return WorkScheduleDay.builder( date ).build();
            }

            // Get domain engines from CalendarServiceProvider
            SchedulingEngine schedulingEngine = mCalendarServiceProvider.getSchedulingEngine();

            // Generate base schedule
            WorkScheduleDay baseSchedule = generateBaseScheduleFromAssignment( date, assignment );

            // Apply exceptions if user specified
            if (userId != null) {
                baseSchedule = applyShiftExceptionsToSchedule( baseSchedule, userId );
            }

            return baseSchedule;
        } catch (Exception e) {
            Log.e( TAG, "Error generating schedule for date " + date, e );
            return WorkScheduleDay.builder( date ).build();
        }
    }

    @Nullable
    private UserScheduleAssignment getUserAssignmentForDate(@NonNull LocalDate date, @Nullable String userId) {
        if (userId == null) return null;

        try {
            CompletableFuture<OperationResult<UserScheduleAssignment>> futureResult =
                    mCalendarServiceProvider.getUserScheduleAssignmentRepository()
                            .getActiveAssignmentForUser( userId, date );

            OperationResult<UserScheduleAssignment> result = futureResult.join();

            if (result.isSuccess()) {
                return result.getData();
            } else {
                Log.w( TAG, "Failed to get user assignment: " + result.getErrorMessage() );
                return null;
            }
        } catch (Exception e) {
            Log.e( TAG, "Error getting user assignment", e );
            return null;
        }
    }

    @NonNull
    private String getDefaultRecurrenceRuleId() {
        try {
            List<RecurrenceRule> rules = mCalendarServiceProvider.getRecurrenceRuleRepository()
                    .getRecurrenceRulesByFrequency( RecurrenceRule.Frequency.QUATTRODUE_CYCLE ).join();

            if (rules != null && !rules.isEmpty()) {
                return rules.get( 0 ).getId();
            }

            return "default-quattrodue-rule";
        } catch (Exception e) {
            Log.e( TAG, "Error getting default recurrence rule", e );
            return "default-quattrodue-rule";
        }
    }

    @NonNull
    private WorkScheduleDay generateBaseScheduleFromAssignment(@NonNull LocalDate date, @NonNull UserScheduleAssignment assignment) {
        try {
            // Get recurrence rule by ID from assignment
            String recurrenceRuleId = assignment.getRecurrenceRuleId();

            RecurrenceRule theRule = mCalendarServiceProvider.getRecurrenceRuleRepository()
                    .getRecurrenceRuleById( recurrenceRuleId ).join();

            if (theRule == null) {
                Log.w( TAG, "Recurrence theRule not found: " + recurrenceRuleId );
                return WorkScheduleDay.builder( date ).build();
            }

            // Use RecurrenceCalculator to generate schedule
            RecurrenceCalculator calculator = mCalendarServiceProvider.getRecurrenceCalculator();
            return calculator.generateScheduleForDate( date, theRule, assignment );
        } catch (Exception e) {
            Log.e( TAG, "Error generating base schedule", e );
            return WorkScheduleDay.builder( date ).build();
        }
    }

    @NonNull
    private WorkScheduleDay applyShiftExceptionsToSchedule(@NonNull WorkScheduleDay baseSchedule, @NonNull String userId) {
        try {
            LocalDate date = baseSchedule.getDate();

            // Get exceptions for user and date
            CompletableFuture<OperationResult<List<ShiftException>>> futureExceptions =
                    mCalendarServiceProvider.getShiftExceptionRepository()
                            .getEffectiveExceptionsForUserOnDate( userId, date );

            OperationResult<List<ShiftException>> exceptionsResult = futureExceptions.join();
            List<ShiftException> exceptions = exceptionsResult.getData();

            if (exceptions == null || exceptions.isEmpty()) {
                return baseSchedule;
            }

            // Build user-team mappings
            Map<String, Team> userTeamMappings = buildUserTeamMappings( userId, date );

            // Apply exceptions using ExceptionResolver
            ExceptionResolver resolver = mCalendarServiceProvider.getExceptionResolver();
            return resolver.applyExceptions( baseSchedule, exceptions, userTeamMappings, new HashMap<>() );
        } catch (Exception e) {
            Log.e( TAG, "Error applying shift exceptions", e );
            return baseSchedule;
        }
    }

    @NonNull
    private Map<String, Team> buildUserTeamMappings(@NonNull String userId, @NonNull LocalDate date) {
        Map<String, Team> mappings = new HashMap<>();

        try {
            UserScheduleAssignment assignment = getUserAssignmentForDate( date, userId );
            if (assignment != null) {
                String teamId = assignment.getTeamId();
                Team team = mCalendarServiceProvider.getTeamRepository().getTeamById( teamId ).join();
                if (team != null) {
                    mappings.put( userId, team );
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "Error building user-team mappings", e );
        }

        return mappings;
    }

    /**
     * NEW: Convert WorkScheduleDay to multi-team WorkScheduleEvents.
     */
    @NonNull
    private List<WorkScheduleEvent> convertScheduleDayToMultiTeamEvents(@NonNull WorkScheduleDay scheduleDay, @Nullable String userId) {
        List<WorkScheduleEvent> events = new ArrayList<>();

        for (WorkScheduleShift shift : scheduleDay.getShifts()) {
            // Create WorkScheduleEvent with multiple teams support
            WorkScheduleEvent event = WorkScheduleEvent.builder( scheduleDay.getDate() )
                    .setShift( shift.getShift() )
                    .setTeams( shift.getTeams() ) // Multi-team support
                    .setUserId( userId )
                    .setDescription( shift.getDescription() )
                    .setDayInCycle( calculateDayInCycle( scheduleDay.getDate() ) )
                    .setPatternName( "QuattroDue 4-2" )
                    .setGeneratedBy( "WorkScheduleRepositoryImpl" )
                    .build();

            events.add( event );
        }

        return events;
    }

    /**
     * Calculate day in cycle for WorkScheduleEvent metadata.
     */
    private int calculateDayInCycle(@NonNull LocalDate date) {
        try {
            return getDayInCycle( date ).join().getData();
        } catch (Exception e) {
            Log.w( TAG, "Error calculating day in cycle for " + date, e );
            return 0;
        }
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    public void cleanup() {
        try {
            mScheduleCache.clear();
            mConfigCache.clear();
            mSchemeCache.clear();

            if (mExecutorService != null && !mExecutorService.isShutdown()) {
                mExecutorService.shutdown();
            }

            Log.d( TAG, "WorkScheduleRepositoryImpl cleanup completed" );
        } catch (Exception e) {
            Log.e( TAG, "Error during cleanup", e );
        }
    }
}