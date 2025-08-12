package net.calvuz.qdue.data.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WorkScheduleRepositoryImpl - Bridge Implementation using QuattroDue Engine
 *
 * <p>This implementation uses the proven QuattroDue calculation engine as a bridge
 * while exposing clean architecture domain models. This approach ensures:</p>
 * <ul>
 *   <li><strong>Reliable Calculations</strong>: Uses the tested QuattroDue algorithm</li>
 *   <li><strong>Clean Architecture</strong>: Exposes domain models (Team, Shift, WorkScheduleDay)</li>
 *   <li><strong>Zero Risk Migration</strong>: No breaking changes to existing logic</li>
 *   <li><strong>Performance Optimization</strong>: Caching and async operations</li>
 *   <li><strong>Future Flexibility</strong>: Easy to replace bridge with native implementation</li>
 * </ul>
 *
 * <h3>Bridge Pattern Benefits:</h3>
 * <ul>
 *   <li>Maintains compatibility with existing QuattroDue logic</li>
 *   <li>Provides clean domain model interface for new code</li>
 *   <li>Allows gradual migration to pure clean architecture</li>
 *   <li>Reduces risk of calculation errors during refactoring</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.1.0 - Bridge Implementation
 * @since Clean Architecture Implementation
 */
public class WorkScheduleRepositoryImpl implements WorkScheduleRepository {

    private static final String TAG = "WorkScheduleRepositoryImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;
    private final LocaleManager mLocaleManager;

    // ==================== QUATTRODUE BRIDGE ====================

    private final QuattroDue mQuattroDue;

    // ==================== CACHING ====================

    private final Map<LocalDate, WorkScheduleDay> mScheduleCache = new ConcurrentHashMap<>();
    private final Map<String, Team> mTeamCache = new ConcurrentHashMap<>();
    private final Map<String, Shift> mShiftCache = new ConcurrentHashMap<>();

    // ==================== TEAM MAPPING ====================

    // Map HalfTeam names to domain Team objects
    private final Map<String, Team> mHalfTeamToTeamMap = new ConcurrentHashMap<>();
    private List<Team> mAllTeams;
    private List<Shift> mAllShifts;

    // ==================== STATE ====================

    private boolean mServiceReady = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection with QuattroDue bridge.
     *
     * @param context Application context
     * @param database QDue database instance
     * @param backupManager Core backup manager instance
     */
    public WorkScheduleRepositoryImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(4);
        this.mLocaleManager = new LocaleManager(mContext);

        // Initialize QuattroDue bridge
        this.mQuattroDue = QuattroDue.getInstance(mContext);

        // Initialize mappings
        initializeBridgeMappings();

        Log.d(TAG, "WorkScheduleRepositoryImpl initialized with QuattroDue bridge");
    }

    /**
     * Alternative constructor with automatic backup manager.
     */
    public WorkScheduleRepositoryImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== BRIDGE INITIALIZATION ====================

    /**
     * Initialize mappings between QuattroDue models and domain models.
     */
    private void initializeBridgeMappings() {
        try {
            // Initialize team mappings
            initializeTeamMappings();

            // Initialize shift mappings
            initializeShiftMappings();

            mServiceReady = true;
            Log.d(TAG, "Bridge mappings initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize bridge mappings", e);
            mServiceReady = false;
        }
    }

    /**
     * Create mappings between HalfTeam and domain Team objects.
     */
    private void initializeTeamMappings() {
        mAllTeams = new ArrayList<>();
        String[] teamNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

        for (String teamName : teamNames) {
            // Create domain Team object
            Team domainTeam = Team.builder(teamName)
                    .name(teamName)
                    .displayName(mLocaleManager.getTeamDisplayName(teamName))
                    .description(mLocaleManager.getTeamDescriptionTemplate() + " " + teamName)
                    .active(true)
                    .build();

            // Add to collections
            mAllTeams.add(domainTeam);
            mHalfTeamToTeamMap.put(teamName, domainTeam);
            mTeamCache.put(teamName, domainTeam);
            mTeamCache.put(domainTeam.getId(), domainTeam);
        }

        Log.d(TAG, "Initialized " + mAllTeams.size() + " team mappings");
    }

    /**
     * Create mappings for shift types.
     */
    private void initializeShiftMappings() {
        mAllShifts = new ArrayList<>();

        // Create domain Shift objects with localized names
        Shift morningShift = Shift.builder(mLocaleManager.getShiftName("MORNING"))
                .setShiftType(Shift.ShiftType.CYCLE_42)
                .setStartTime(6, 0)
                .setEndTime(14, 0)
                .setColorHex("#4CAF50")
                .setDescription(mLocaleManager.getShiftDescription("MORNING"))
                .setHasBreakTime(true)
                .setBreakTimeDuration(30)
                .build();

        Shift afternoonShift = Shift.builder(mLocaleManager.getShiftName("AFTERNOON"))
                .setShiftType(Shift.ShiftType.CYCLE_42)
                .setStartTime(14, 0)
                .setEndTime(22, 0)
                .setColorHex("#FF9800")
                .setDescription(mLocaleManager.getShiftDescription("AFTERNOON"))
                .setHasBreakTime(true)
                .setBreakTimeDuration(30)
                .build();

        Shift nightShift = Shift.builder(mLocaleManager.getShiftName("NIGHT"))
                .setShiftType(Shift.ShiftType.CYCLE_42)
                .setStartTime(22, 0)
                .setEndTime(6, 0)
                .setColorHex("#3F51B5")
                .setDescription(mLocaleManager.getShiftDescription("NIGHT"))
                .setHasBreakTime(true)
                .setBreakTimeDuration(45)
                .build();

        mAllShifts.add(morningShift);
        mAllShifts.add(afternoonShift);
        mAllShifts.add(nightShift);

        // Cache shifts
        for (Shift shift : mAllShifts) {
            mShiftCache.put(shift.getId(), shift);
            mShiftCache.put(shift.getName(), shift);
        }

        Log.d(TAG, "Initialized " + mAllShifts.size() + " shift mappings");
    }

    // ==================== BRIDGE CONVERSION METHODS ====================

    /**
     * Convert QuattroDue Day to domain WorkScheduleDay.
     *
     * @param quattroduDay QuattroDue Day object
     * @return Domain WorkScheduleDay object
     */
    private WorkScheduleDay convertDayToWorkScheduleDay(Day quattroduDay) {
        try {
            if (quattroduDay == null) {
                return null;
            }

            WorkScheduleDay.Builder builder = WorkScheduleDay.builder(quattroduDay.getLocalDate());

            // Convert each shift from QuattroDue to domain model
            List<net.calvuz.qdue.quattrodue.models.Shift> quattroduShifts = quattroduDay.getShifts();

            for (int i = 0; i < quattroduShifts.size() && i < mAllShifts.size(); i++) {
                net.calvuz.qdue.quattrodue.models.Shift quattroduShift = quattroduShifts.get(i);
                Shift domainShiftTemplate = mAllShifts.get(i);

                // Create WorkScheduleShift
                WorkScheduleShift.Builder shiftBuilder = WorkScheduleShift.builder(domainShiftTemplate)
                        .startTime(domainShiftTemplate.getStartTime())
                        .endTime(domainShiftTemplate.getEndTime())
                        .description(domainShiftTemplate.getDescription());

                // Add teams from QuattroDue shift
                Set<HalfTeam> halfTeams = quattroduShift.getHalfTeams();
                for (HalfTeam halfTeam : halfTeams) {
                    Team domainTeam = mHalfTeamToTeamMap.get(halfTeam.getName());
                    if (domainTeam != null) {
                        shiftBuilder.addTeam(domainTeam);
                    }
                }

                builder.addShift(shiftBuilder.build());
            }

            // Add off-work teams
            List<String> allTeamNames = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I");
            for (String teamName : allTeamNames) {
                boolean isWorking = quattroduShifts.stream()
                        .flatMap(shift -> shift.getHalfTeams().stream())
                        .anyMatch(halfTeam -> halfTeam.getName().equals(teamName));

                if (!isWorking) {
                    Team offTeam = mHalfTeamToTeamMap.get(teamName);
                    if (offTeam != null) {
                        builder.addOffWorkTeam(offTeam);
                    }
                }
            }

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Error converting QuattroDue Day to WorkScheduleDay", e);
            return WorkScheduleDay.builder(quattroduDay.getLocalDate()).build();
        }
    }

    /**
     * Convert HalfTeam to domain Team.
     *
     * @param halfTeam QuattroDue HalfTeam
     * @return Domain Team object
     */
    private Team convertHalfTeamToTeam(HalfTeam halfTeam) {
        if (halfTeam == null) {
            return null;
        }
        return mHalfTeamToTeamMap.get(halfTeam.getName());
    }

    // ==================== SCHEDULE GENERATION (BRIDGE METHODS) ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<WorkScheduleDay>> getWorkScheduleForDate(@NonNull LocalDate date, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting work schedule for date: " + date + " (using QuattroDue bridge)");

                // Check cache first
                WorkScheduleDay cachedDay = mScheduleCache.get(date);
                if (cachedDay != null) {
                    Log.d(TAG, "✅ Cache hit for date: " + date);
                    return OperationResult.success(cachedDay, OperationResult.OperationType.READ);
                }

                // Use QuattroDue to calculate the day
                Day quattroduDay = mQuattroDue.getDayByDate(date);
                if (quattroduDay == null) {
                    Log.w(TAG, "QuattroDue returned null for date: " + date);
                    return OperationResult.failure("No schedule available for date", OperationResult.OperationType.READ);
                }

                // Convert to domain model
                WorkScheduleDay workScheduleDay = convertDayToWorkScheduleDay(quattroduDay);

                // Cache the result
                if (workScheduleDay != null) {
                    mScheduleCache.put(date, workScheduleDay);
                }

                Log.d(TAG, "✅ Successfully calculated work schedule for: " + date +
                        " (shifts: " + (workScheduleDay != null ? workScheduleDay.getShiftCount() : 0) + ")");

                return OperationResult.success(workScheduleDay, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Failed to get work schedule for date " + date + ": " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting work schedule for date range: " + startDate + " to " + endDate + " (using QuattroDue bridge)");

                Map<LocalDate, WorkScheduleDay> scheduleMap = new ConcurrentHashMap<>();
                LocalDate currentDate = startDate;

                while (!currentDate.isAfter(endDate)) {
                    // Use QuattroDue for each day
                    Day quattroduDay = mQuattroDue.getDayByDate(currentDate);
                    if (quattroduDay != null) {
                        WorkScheduleDay workScheduleDay = convertDayToWorkScheduleDay(quattroduDay);
                        if (workScheduleDay != null) {
                            scheduleMap.put(currentDate, workScheduleDay);
                            mScheduleCache.put(currentDate, workScheduleDay); // Cache each day
                        }
                    }
                    currentDate = currentDate.plusDays(1);
                }

                Log.d(TAG, "✅ Successfully calculated work schedule for range (" + scheduleMap.size() + " days)");
                return OperationResult.success(scheduleMap, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Failed to get work schedule for date range " + startDate + " to " + endDate + ": " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForMonth(
            @NonNull YearMonth month, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting work schedule for month: " + month + " (using QuattroDue bridge)");

                // Use QuattroDue's optimized month calculation
                LocalDate firstDayOfMonth = month.atDay(1);
                List<Day> quattroduDays = mQuattroDue.getShiftsForMonth(firstDayOfMonth);

                Map<LocalDate, WorkScheduleDay> scheduleMap = new ConcurrentHashMap<>();

                for (Day quattroduDay : quattroduDays) {
                    WorkScheduleDay workScheduleDay = convertDayToWorkScheduleDay(quattroduDay);
                    if (workScheduleDay != null) {
                        scheduleMap.put(quattroduDay.getLocalDate(), workScheduleDay);
                        mScheduleCache.put(quattroduDay.getLocalDate(), workScheduleDay);
                    }
                }

                Log.d(TAG, "✅ Successfully retrieved schedule for month " + month + " (" + scheduleMap.size() + " days)");
                return OperationResult.success(scheduleMap, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Failed to get work schedule for month " + month + ": " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== PATTERN CALCULATIONS (BRIDGE) ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Integer>> getDayInCycle(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use QuattroDue's proven calculation
                int dayInCycle = mQuattroDue.getDayInCycle(date);
                if (dayInCycle >= 0) {
                    return OperationResult.success(dayInCycle, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Failed to calculate day in cycle", OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                String error = "Error calculating day in cycle: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Long>> getDaysFromSchemeStart(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate schemeStartDate = mQuattroDue.getSchemeDate();
                if (schemeStartDate == null) {
                    return OperationResult.failure("Scheme start date not configured", OperationResult.OperationType.READ);
                }

                long days = java.time.temporal.ChronoUnit.DAYS.between(schemeStartDate, date);
                return OperationResult.success(days, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error calculating days from scheme start: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> isWorkingDay(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use QuattroDue's proven method
                boolean hasWorkSchedule = mQuattroDue.hasWorkScheduleOnDate(date);
                return OperationResult.success(hasWorkSchedule, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error checking if working day: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the day from QuattroDue
                Day quattroduDay = mQuattroDue.getDayByDate(date);
                if (quattroduDay == null) {
                    return OperationResult.failure("Failed to get day information", OperationResult.OperationType.READ);
                }

                // Check if team is working on this day
                boolean isWorking = quattroduDay.getShifts().stream()
                        .flatMap(shift -> shift.getHalfTeams().stream())
                        .anyMatch(halfTeam -> halfTeam.getName().equals(team.getName()));

                return OperationResult.success(isWorking, OperationResult.OperationType.READ);

            } catch (Exception e) {
                String error = "Error checking if working day for team: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OperationResult<Boolean> workingResult = isWorkingDayForTeam(date, team).join();
                if (workingResult.isSuccess()) {
                    boolean isRest = !workingResult.getData();
                    return OperationResult.success(isRest, OperationResult.OperationType.READ);
                } else {
                    return workingResult;
                }
            } catch (Exception e) {
                String error = "Error checking if rest day for team: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== TEAM MANAGEMENT ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<Team>>> getAllTeams() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return OperationResult.success(new ArrayList<>(mAllTeams), OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get all teams: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Team>> findTeamByName(@NonNull String teamName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Team team = mTeamCache.get(teamName);
                return OperationResult.success(team, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to find team by name: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Team>> findTeamById(@NonNull String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Team team = mTeamCache.get(teamId);
                return OperationResult.success(team, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to find team by ID: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<Team>>> createStandardTeams() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return OperationResult.success(new ArrayList<>(mAllTeams), OperationResult.OperationType.CREATE);
            } catch (Exception e) {
                String error = "Failed to create standard teams: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    // ==================== SHIFT MANAGEMENT ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<Shift>>> getAllShifts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return OperationResult.success(new ArrayList<>(mAllShifts), OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get all shifts: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Shift>> getShiftById(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Shift shift = mShiftCache.get(shiftId);
                return OperationResult.success(shift, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get shift by ID: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Shift>> getShiftByName(@NonNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Shift shift = mShiftCache.get(name);
                return OperationResult.success(shift, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get shift by name: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== CONFIGURATION MANAGEMENT ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<LocalDate>> getSchemeStartDate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate schemeDate = mQuattroDue.getSchemeDate();
                return OperationResult.success(schemeDate, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get scheme start date: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> updateSchemeStartDate(@NonNull LocalDate newStartDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Updating scheme start date to: " + newStartDate);

                // Update QuattroDue's scheme date
                mQuattroDue.setSchemeDate(mContext, newStartDate);

                // Clear cache since calculations will change
                mScheduleCache.clear();

                Log.d(TAG, "✅ Successfully updated scheme start date");
                return OperationResult.success("Scheme start date updated", OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                String error = "Failed to update scheme start date: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Map<String, Object>>> getScheduleConfiguration() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get configuration from QuattroDue
                Map<String, Object> config = mQuattroDue.getCycleInfo();
                return OperationResult.success(config, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get schedule configuration: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> updateScheduleConfiguration(@NonNull Map<String, Object> configuration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Clear cache when configuration changes
                mScheduleCache.clear();
                return OperationResult.success("Configuration updated", OperationResult.OperationType.UPDATE);
            } catch (Exception e) {
                String error = "Failed to update configuration: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> isRepositoryReady() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean ready = mServiceReady && mQuattroDue != null;
                return OperationResult.success(ready, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to check repository readiness: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Map<String, Object>>> validateConfiguration() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> validationResults = new HashMap<>();

                // Validate QuattroDue readiness
                validationResults.put("quattrodue_ready", mQuattroDue != null);
                validationResults.put("service_ready", mServiceReady);
                validationResults.put("teams_mapped", mAllTeams != null && mAllTeams.size() == 9);
                validationResults.put("shifts_mapped", mAllShifts != null && mAllShifts.size() == 3);
                validationResults.put("cache_size", mScheduleCache.size());
                validationResults.put("database_connected", mDatabase != null);

                // Get QuattroDue validation info
                if (mQuattroDue != null) {
                    Map<String, Object> quattroduInfo = mQuattroDue.getCycleInfo();
                    validationResults.putAll(quattroduInfo);
                }

                return OperationResult.success(validationResults, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                String error = "Failed to validate configuration: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== DATA MANAGEMENT ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> clearCache() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                mScheduleCache.clear();
                Log.d(TAG, "✅ Successfully cleared cache");
                return OperationResult.success("Cache cleared", OperationResult.OperationType.DELETE);
            } catch (Exception e) {
                String error = "Failed to clear cache: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> refreshWorkScheduleData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Clear cache
                clearCache().join();

                // Refresh QuattroDue
                mQuattroDue.refresh(mContext);

                // Reinitialize bridge mappings
                initializeBridgeMappings();

                Log.d(TAG, "✅ Successfully refreshed work schedule data");
                return OperationResult.success("Work schedule data refreshed", OperationResult.OperationType.REFRESH);

            } catch (Exception e) {
                String error = "Failed to refresh work schedule data: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.REFRESH);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Map<String, Object>>> getServiceStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> status = new HashMap<>();
                status.put("service_ready", mServiceReady);
                status.put("cache_size", mScheduleCache.size());
                status.put("quattrodue_bridge", mQuattroDue != null);
                status.put("teams_count", mAllTeams != null ? mAllTeams.size() : 0);
                status.put("shifts_count", mAllShifts != null ? mAllShifts.size() : 0);

                if (mQuattroDue != null) {
                    status.put("scheme_start_date", mQuattroDue.getSchemeDate() != null ? mQuattroDue.getSchemeDate().toString() : null);
                    status.putAll(mQuattroDue.getCycleInfo());
                }

                return OperationResult.success(status, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get service status: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== ADDITIONAL BRIDGE METHODS ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<Team>>> getTeamsWorkingOnDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use QuattroDue to get the day
                Day quattroduDay = mQuattroDue.getDayByDate(date);
                if (quattroduDay == null) {
                    return OperationResult.success(new ArrayList<>(), OperationResult.OperationType.READ);
                }

                List<Team> workingTeams = new ArrayList<>();

                // Convert HalfTeams to domain Teams
                quattroduDay.getShifts().stream()
                        .flatMap(shift -> shift.getHalfTeams().stream())
                        .forEach(halfTeam -> {
                            Team domainTeam = convertHalfTeamToTeam(halfTeam);
                            if (domainTeam != null && !workingTeams.contains(domainTeam)) {
                                workingTeams.add(domainTeam);
                            }
                        });

                return OperationResult.success(workingTeams, OperationResult.OperationType.READ);
            } catch (Exception e) {
                String error = "Failed to get teams working on date: " + e.getMessage();
                Log.e(TAG, error, e);
                return OperationResult.failure(error, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== PLACEHOLDER IMPLEMENTATIONS ====================
    // These methods maintain the interface contract but are not yet fully implemented

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateWorkScheduleEvents(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement using QuattroDue bridge
            return OperationResult.success(new ArrayList<>(), OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Team>> getTeamForUser(@NonNull Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement user team lookup from database
            return OperationResult.success(null, OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> setTeamForUser(@NonNull Long userId, @NonNull Team team) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement user team assignment to database
            return OperationResult.success("Team assigned", OperationResult.OperationType.UPDATE);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Shift>> saveShift(@NonNull Shift shift) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement shift persistence to database
            return OperationResult.success(shift, OperationResult.OperationType.CREATE);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Void>> deleteShift(@NonNull String shiftId) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement shift deletion from database
            return OperationResult.success("Shift deleted", OperationResult.OperationType.DELETE);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleShift>>> getShiftsForDate(@NonNull LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement getting WorkScheduleShifts for specific date
            return OperationResult.success(new ArrayList<>(), OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> hasWorkSchedule(@Nullable WorkScheduleDay day) {
        return CompletableFuture.supplyAsync(() ->
                OperationResult.success(day != null && day.hasShifts(), OperationResult.OperationType.READ), mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<String>> getWorkScheduleColor(@NonNull LocalDate date, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() ->
                OperationResult.success("#2196F3", OperationResult.OperationType.READ), mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<String>> getWorkScheduleSummary(@NonNull LocalDate date, @Nullable Long userId) {
        return CompletableFuture.supplyAsync(() ->
                OperationResult.success("Work schedule for " + date, OperationResult.OperationType.READ), mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<LocalDate>> getNextWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement using QuattroDue bridge
            return OperationResult.success(null, OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<LocalDate>> getPreviousWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement using QuattroDue bridge
            return OperationResult.success(null, OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Integer>> getWorkingDaysCount(@NonNull Team team,
                                                                           @NonNull LocalDate startDate,
                                                                           @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement using QuattroDue bridge
            return OperationResult.success(0, OperationResult.OperationType.READ);
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Integer>> getRestDaysCount(@NonNull Team team,
                                                                        @NonNull LocalDate startDate,
                                                                        @NonNull LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement using QuattroDue bridge
            return OperationResult.success(0, OperationResult.OperationType.READ);
        }, mExecutorService);
    }
}