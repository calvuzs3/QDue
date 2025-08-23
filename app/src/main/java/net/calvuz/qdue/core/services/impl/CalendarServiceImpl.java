package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftExceptionRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateTeamScheduleUseCase;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CalendarServiceImpl - Clean Architecture Application Service Implementation
 *
 * <p>Application Service that orchestrates domain use cases and repositories through
 * CalendarServiceProvider dependency injection. Acts as a facade for calendar domain
 * operations, providing business-level APIs while delegating to domain layer components.</p>
 *
 * <h3>Clean Architecture Position:</h3>
 * <ul>
 *   <li><strong>Layer</strong>: Application Service (between UI and Domain)</li>
 *   <li><strong>Responsibility</strong>: Business workflow orchestration</li>
 *   <li><strong>Dependencies</strong>: Domain layer (via CalendarServiceProvider)</li>
 *   <li><strong>Clients</strong>: UI layer (via ServiceProvider.getCalendarService())</li>
 * </ul>
 *
 * <h3>Architecture Benefits:</h3>
 * <ul>
 *   <li><strong>Clean Separation</strong>: UI doesn't know about domain repositories</li>
 *   <li><strong>Business Logic</strong>: Orchestrates complex multi-repository operations</li>
 *   <li><strong>Stable Interface</strong>: UI-friendly API that hides domain complexity</li>
 *   <li><strong>Transaction Boundaries</strong>: Coordinates multi-step business operations</li>
 * </ul>
 *
 * <h3>Dependency Flow:</h3>
 * <pre>
 * UI Layer → CalendarService → CalendarServiceProvider → Domain Repositories/Use Cases
 * </pre>
 *
 * <h3>Multi-Team Support:</h3>
 * <ul>
 *   <li><strong>WorkScheduleEvent</strong>: Fully supports List&lt;Team&gt; assignments</li>
 *   <li><strong>Team Coordination</strong>: Handles complex team scheduling scenarios</li>
 *   <li><strong>User Relevance</strong>: Correctly calculates user relevance across multiple teams</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 3.0.0 - Clean Architecture Application Service with Multi-Team Support
 * @since Clean Architecture Phase 3
 */
public class CalendarServiceImpl implements CalendarService {

    private static final String TAG = "CalendarServiceImpl";

    // ==================== DEPENDENCIES (CLEAN ARCHITECTURE) ====================

    private final Context mContext;
    private final CalendarServiceProvider mCalendarServiceProvider;

    // ==================== THREADING AND PERFORMANCE ====================

    private final ExecutorService mExecutorService;
    private final Map<String, Object> mCache;

    // ==================== STATE MANAGEMENT ====================

    private volatile boolean mIsInitialized = false;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for clean architecture dependency injection.
     *
     * <p>Receives CalendarServiceProvider which provides access to all domain layer
     * components (repositories, engines, use cases) following clean architecture
     * dependency inversion principle.</p>
     *
     * @param context Application context for system resources
     * @param calendarServiceProvider Domain layer dependency provider
     */
    public CalendarServiceImpl(@NonNull Context context,
                               @NonNull CalendarServiceProvider calendarServiceProvider) {

        this.mContext = context.getApplicationContext();
        this.mCalendarServiceProvider = calendarServiceProvider;

        // Initialize performance infrastructure
        this.mExecutorService = Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r, "CalendarService-" + Thread.currentThread().getId());
            thread.setDaemon(true);
            return thread;
        });

        this.mCache = new ConcurrentHashMap<>();

        Log.i(TAG, "CalendarServiceImpl created with CalendarServiceProvider DI");
    }

    // ==================== SERVICE LIFECYCLE ====================

    @Override
    public void initialize() {
        if (mIsInitialized) {
            Log.w(TAG, "CalendarService already initialized");
            return;
        }

        try {
            Log.d(TAG, "Initializing CalendarService...");

            // Initialize CalendarServiceProvider
            mCalendarServiceProvider.initializeCalendarServices();

            // Verify domain services are ready
            if (!mCalendarServiceProvider.areCalendarServicesReady()) {
                throw new RuntimeException("CalendarServiceProvider initialization failed");
            }

            mIsInitialized = true;
            Log.i(TAG, "CalendarService initialized successfully with domain layer integration");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CalendarService", e);
            mIsInitialized = false;
            throw new RuntimeException("CalendarService initialization failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return mIsInitialized && mCalendarServiceProvider.areCalendarServicesReady();
    }

    @Override
    public void cleanup() {
        Log.d(TAG, "Cleaning up CalendarService resources");

        clearScheduleCache();

        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }

        // Cleanup domain services
        mCalendarServiceProvider.shutdownCalendarServices();

        mIsInitialized = false;
        Log.d(TAG, "CalendarService cleanup completed");
    }

    // ==================== WORK SCHEDULE OPERATIONS ====================

    /**
     * @return 
     */
    @NonNull
    @Override
    public CalendarServiceProvider getCalendarServiceProvider() {
        return mCalendarServiceProvider;
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<WorkScheduleDay>> getUserScheduleForDate(
            @NonNull Long userId, @NonNull LocalDate date) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Getting user schedule for userId: " + userId + ", date: " + date);

                // Check cache first
                String cacheKey = generateUserCacheKey(userId, date);
                Object cachedResult = mCache.get(cacheKey);
                if (cachedResult instanceof WorkScheduleDay) {
                    Log.d(TAG, "Cache hit for user schedule: " + cacheKey);
                    return OperationResult.success((WorkScheduleDay) cachedResult, OperationResult.OperationType.READ);
                }

                // Use GenerateUserScheduleUseCase for complex user schedule logic
                GenerateUserScheduleUseCase generateUserScheduleUseCase =
                        mCalendarServiceProvider.getGenerateUserScheduleUseCase();

                // Generate schedule using domain use case
                OperationResult<WorkScheduleDay> result = generateUserScheduleUseCase
                        .executeForDate( userId, date ).join(); // .generateUserScheduleForDate(userId, date).join();

                if (result.isSuccess() && result.getData() != null) {
                    // Cache the result
                    mCache.put(cacheKey, result.getData());
                    Log.d(TAG, "Generated user schedule with " +
                            result.getData().getShifts().size() + " shifts using domain use case");
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting user schedule for " + userId + " on " + date, e);
                return OperationResult.failure("Failed to get user schedule: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getUserScheduleForDateRange(
            @NonNull Long userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Getting user schedule range for userId: " + userId +
                        ", dates: " + startDate + " to " + endDate);

                // Use GenerateUserScheduleUseCase for date range operations
                GenerateUserScheduleUseCase generateUserScheduleUseCase =
                        mCalendarServiceProvider.getGenerateUserScheduleUseCase();

                OperationResult<Map<LocalDate, WorkScheduleDay>> result = generateUserScheduleUseCase
                        .executeForDateRange( userId, startDate, endDate ).join();
                        //.generateUserScheduleForDateRange(userId, startDate, endDate).join();

                if (result.isSuccess()) {
                    Log.d(TAG, "Generated user schedule range with " +
                            result.getData().size() + " days using domain use case");
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting user schedule range for " + userId, e);
                return OperationResult.failure("Failed to get user schedule range: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getUserScheduleForMonth(
            @NonNull Long userId, @NonNull YearMonth month) {

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return getUserScheduleForDateRange(userId, startDate, endDate);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<WorkScheduleDay>> getTeamScheduleForDate(
            @NonNull LocalDate date, @Nullable String teamId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Getting team schedule for date: " + date + ", teamId: " + teamId);

                // Use GenerateTeamScheduleUseCase for team coordination logic
                GenerateTeamScheduleUseCase generateTeamScheduleUseCase =
                        mCalendarServiceProvider.getGenerateTeamScheduleUseCase();

                OperationResult<WorkScheduleDay> result;
                if (teamId != null) {
                    result = generateTeamScheduleUseCase
                            .executeForDate( date, Integer.valueOf( teamId )).join();
                            //.generateTeamScheduleForDate(teamId, date).join();
                } else {
                    result = generateTeamScheduleUseCase
                            .executeForDate( date, null ).join();
                            //.generateAllTeamsScheduleForDate(date).join();
                }

                if (result.isSuccess()) {
                    Log.d(TAG, "Generated team schedule with " +
                            result.getData().getShifts().size() + " shifts using domain use case");
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting team schedule for " + teamId + " on " + date, e);
                return OperationResult.failure("Failed to get team schedule: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== CALENDAR EVENTS OPERATIONS ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> getCalendarEventsForUser(
            @NonNull Long userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Getting calendar events for user: " + userId +
                        ", dates: " + startDate + " to " + endDate);

                // Use WorkScheduleRepository to generate events with multi-team support
                WorkScheduleRepository workScheduleRepository =
                        mCalendarServiceProvider.getWorkScheduleRepository();

                OperationResult<List<WorkScheduleEvent>> result = workScheduleRepository
                        .generateWorkScheduleEvents(startDate, endDate, userId).join();

                if (result.isSuccess()) {
                    Log.d(TAG, "Generated " + result.getData().size() +
                            " calendar events for user using WorkScheduleRepository");
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting calendar events for user " + userId, e);
                return OperationResult.failure("Failed to get calendar events: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> getCalendarEventsForTeam(
            @NonNull String teamId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Getting calendar events for team: " + teamId +
                        ", dates: " + startDate + " to " + endDate);

                // Use GenerateTeamScheduleUseCase and convert to events
                GenerateTeamScheduleUseCase generateTeamScheduleUseCase =
                        mCalendarServiceProvider.getGenerateTeamScheduleUseCase();

                OperationResult<Map<LocalDate, WorkScheduleDay>> scheduleResult =
                        generateTeamScheduleUseCase.executeForDateRange( startDate, endDate, Integer.valueOf( teamId ) ).join();
                        //.generateTeamScheduleForDateRange(teamId, startDate, endDate).join();

                if (!scheduleResult.isSuccess()) {
                    return OperationResult.failure("Failed to generate team schedule: " +
                            scheduleResult.getErrorMessage(), OperationResult.OperationType.READ);
                }

                // Convert schedules to events
                List<WorkScheduleEvent> events = convertSchedulesToEvents(scheduleResult.getData(), null);

                Log.d(TAG, "Generated " + events.size() +
                        " calendar events for team using domain use case");

                return OperationResult.success(events, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting calendar events for team " + teamId, e);
                return OperationResult.failure("Failed to get team calendar events: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== TEAM MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Team>>> getAllTeams() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use TeamRepository for team management
                TeamRepository teamRepository = mCalendarServiceProvider.getTeamRepository();

                // TeamRepository.getAllActiveTeams() returns CompletableFuture<List<Team>>
                List<Team> teams = teamRepository.getAllActiveTeams().join();

                Log.d(TAG, "Retrieved " + teams.size() + " teams using TeamRepository");
                return OperationResult.success(teams, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting all teams", e);
                return OperationResult.failure("Failed to get teams: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Team>> getTeamById(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Check cache first
                String cacheKey = "team_" + teamId;
                Object cachedTeam = mCache.get(cacheKey);
                if (cachedTeam instanceof Team) {
                    return OperationResult.success((Team) cachedTeam, OperationResult.OperationType.READ);
                }

                // Use TeamRepository
                TeamRepository teamRepository = mCalendarServiceProvider.getTeamRepository();

                // TeamRepository.getTeamById() returns CompletableFuture<Team>
                Team team = teamRepository.getTeamById(teamId).join();

                if (team != null) {
                    mCache.put(cacheKey, team);
                    Log.d(TAG, "Retrieved team " + teamId + " using TeamRepository");
                    return OperationResult.success(team, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.success(null, OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting team by ID: " + teamId, e);
                return OperationResult.failure("Failed to get team: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Team>> getTeamForUser(@NonNull Long userId, @NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use UserScheduleAssignmentRepository to get user's team assignment
                UserScheduleAssignmentRepository assignmentRepository =
                        mCalendarServiceProvider.getUserScheduleAssignmentRepository();

                // Get active assignment for user on date
                OperationResult<UserScheduleAssignment> assignmentResult =
                        assignmentRepository.getActiveAssignmentForUser(userId, date).join();

                if (!assignmentResult.isSuccess() || assignmentResult.getData() == null) {
                    return OperationResult.success(null, OperationResult.OperationType.READ);
                }

                // Get team by ID from assignment
                UserScheduleAssignment assignment = assignmentResult.getData();
                String teamId = assignment.getTeamId();

                TeamRepository teamRepository = mCalendarServiceProvider.getTeamRepository();
                Team team = teamRepository.getTeamById(teamId).join();

                Log.d(TAG, "Retrieved team " + teamId + " for user " + userId + " on " + date);
                return OperationResult.success(team, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting team for user: " + userId + " on " + date, e);
                return OperationResult.failure("Failed to get team for user: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== SHIFT TEMPLATES MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Shift>>> getAllShiftTemplates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use ShiftRepository for shift template management
                ShiftRepository shiftRepository = mCalendarServiceProvider.getShiftRepository();

                // ShiftRepository.getAllShifts() returns CompletableFuture<List<Shift>>
                List<Shift> shifts = shiftRepository.getAllShifts().join();

                Log.d(TAG, "Retrieved " + shifts.size() + " shift templates using ShiftRepository");
                return OperationResult.success(shifts, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting all shift templates", e);
                return OperationResult.failure("Failed to get shift templates: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<Shift>> getShiftTemplateById(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Check cache first
                String cacheKey = "shift_" + shiftId;
                Object cachedShift = mCache.get(cacheKey);
                if (cachedShift instanceof Shift) {
                    return OperationResult.success((Shift) cachedShift, OperationResult.OperationType.READ);
                }

                // Use ShiftRepository
                ShiftRepository shiftRepository = mCalendarServiceProvider.getShiftRepository();

                // ShiftRepository.getShiftById() returns CompletableFuture<Shift>
                Shift shift = shiftRepository.getShiftById(shiftId).join();

                if (shift != null) {
                    mCache.put(cacheKey, shift);
                    Log.d(TAG, "Retrieved shift template " + shiftId + " using ShiftRepository");
                    return OperationResult.success(shift, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.success(null, OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting shift template by ID: " + shiftId, e);
                return OperationResult.failure("Failed to get shift template: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== USER ASSIGNMENT MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> getUserAssignmentForDate(
            @NonNull Long userId, @NonNull LocalDate date) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use UserScheduleAssignmentRepository
                UserScheduleAssignmentRepository assignmentRepository =
                        mCalendarServiceProvider.getUserScheduleAssignmentRepository();

                // UserScheduleAssignmentRepository returns OperationResult<UserScheduleAssignment>
                OperationResult<UserScheduleAssignment> result =
                        assignmentRepository.getActiveAssignmentForUser(userId, date).join();

                if (result.isSuccess()) {
                    Log.d(TAG, "Retrieved user assignment for " + userId + " on " + date);
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting user assignment for " + userId + " on " + date, e);
                return OperationResult.failure("Failed to get user assignment: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<Long>>> getActiveUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use UserScheduleAssignmentRepository to get users with active assignments
                UserScheduleAssignmentRepository assignmentRepository =
                        mCalendarServiceProvider.getUserScheduleAssignmentRepository();

                // Get active assignments and extract user IDs
                OperationResult<List<UserScheduleAssignment>> assignmentsResult =
                        assignmentRepository.getActiveAssignmentsForDate(LocalDate.now()).join();

                if (!assignmentsResult.isSuccess()) {
                    return OperationResult.failure("Failed to get active assignments: " +
                            assignmentsResult.getErrorMessage(), OperationResult.OperationType.READ);
                }

                List<Long> activeUsers = new ArrayList<>();
                for (UserScheduleAssignment assignment : assignmentsResult.getData()) {
                    if (!activeUsers.contains(assignment.getUserId())) {
                        activeUsers.add(assignment.getUserId());
                    }
                }

                Log.d(TAG, "Retrieved " + activeUsers.size() + " active users");
                return OperationResult.success(activeUsers, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error getting active users", e);
                return OperationResult.failure("Failed to get active users: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== EXCEPTION MANAGEMENT ====================

    @Override
    @NonNull
    public CompletableFuture<OperationResult<List<ShiftException>>> getShiftExceptionsForUser(
            @NonNull Long userId, @NonNull LocalDate date) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.READ);
                }

                // Use ShiftExceptionRepository
                ShiftExceptionRepository exceptionRepository =
                        mCalendarServiceProvider.getShiftExceptionRepository();

                // ShiftExceptionRepository returns OperationResult<List<ShiftException>>
                OperationResult<List<ShiftException>> result =
                        exceptionRepository.getEffectiveExceptionsForUserOnDate(userId, date).join();

                if (result.isSuccess()) {
                    Log.d(TAG, "Retrieved " + result.getData().size() +
                            " shift exceptions for user " + userId + " on " + date);
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error getting shift exceptions for user " + userId + " on " + date, e);
                return OperationResult.failure("Failed to get shift exceptions: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    @NonNull
    public CompletableFuture<OperationResult<ShiftException>> createShiftException(
            @NonNull ShiftException shiftException) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isReady()) {
                    return OperationResult.failure("CalendarService not ready",
                            OperationResult.OperationType.CREATE);
                }

                // Use ShiftExceptionRepository
                ShiftExceptionRepository exceptionRepository =
                        mCalendarServiceProvider.getShiftExceptionRepository();

                // ShiftExceptionRepository returns OperationResult<ShiftException>
                OperationResult<ShiftException> result =
                        exceptionRepository.saveShiftException(shiftException).join();

                if (result.isSuccess()) {
                    Log.d(TAG, "Created shift exception: " + shiftException.getId());

                    // Clear relevant cache entries
                    clearCacheForUser(shiftException.getUserId(), shiftException.getTargetDate());
                }

                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error creating shift exception", e);
                return OperationResult.failure("Failed to create shift exception: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    // ==================== CACHE MANAGEMENT ====================

    @Override
    public void clearScheduleCache() {
        mCache.clear();
        Log.d(TAG, "All caches cleared");
    }

    @Override
    public void clearCacheForDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        mCache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            try {
                if (key.contains("_")) {
                    String[] parts = key.split("_");
                    if (parts.length >= 3) {
                        LocalDate keyDate = LocalDate.parse(parts[2]);
                        return !keyDate.isBefore(startDate) && !keyDate.isAfter(endDate);
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
        Log.d(TAG, "Cache cleared for date range: " + startDate + " to " + endDate);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate cache key for user schedule.
     */
    private String generateUserCacheKey(@NonNull Long userId, @NonNull LocalDate date) {
        return "user_" + userId + "_" + date.toString();
    }

    /**
     * Clear cache entries for specific user and date.
     */
    private void clearCacheForUser(@NonNull Long userId, @NonNull LocalDate date) {
        String userCacheKey = generateUserCacheKey(userId, date);
        mCache.remove(userCacheKey);
        Log.v(TAG, "Cleared cache for user " + userId + " on " + date);
    }

    /**
     * Convert schedule map to WorkScheduleEvent list with multi-team support.
     */
    private List<WorkScheduleEvent> convertSchedulesToEvents(
            @NonNull Map<LocalDate, WorkScheduleDay> schedules, @Nullable Long userId) {

        List<WorkScheduleEvent> events = new ArrayList<>();

        for (Map.Entry<LocalDate, WorkScheduleDay> entry : schedules.entrySet()) {
            LocalDate date = entry.getKey();
            WorkScheduleDay schedule = entry.getValue();

            for (net.calvuz.qdue.domain.calendar.models.WorkScheduleShift shift : schedule.getShifts()) {
                // Create WorkScheduleEvent with multi-team support
                WorkScheduleEvent event = WorkScheduleEvent.builder(date)
                        .setShift(shift.getShift())
                        .setTeams(shift.getTeams()) // Multi-team support
                        .setUserId(userId)
                        .setTiming(shift.getStartTime(), shift.getEndTime())
                        .setDescription(shift.getDescription())
                        .setEventType(WorkScheduleEvent.EventType.SHIFT_EVENT)
                        .build();

                events.add(event);
            }
        }

        return events;
    }

    // ==================== PUBLIC ACCESS FOR CONTEXT ====================

    /**
     * Get context for external integrations.
     * Used by CalendarServiceProviderImpl for infrastructure access.
     *
     * @return Application context
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }
}