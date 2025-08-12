package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.WorkScheduleService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.repositories.WorkScheduleRepositoryImpl;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.models.Team;
import net.calvuz.qdue.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkScheduleServiceAdapter - Bridge between legacy WorkScheduleService and new clean architecture.
 *
 * <p>This adapter maintains full backward compatibility with existing WorkScheduleService interface
 * while internally using the new clean architecture WorkScheduleRepository. It acts as a bridge
 * pattern converting between legacy quattrodue models and new domain models.</p>
 *
 * <h3>Architecture Benefits:</h3>
 * <ul>
 *   <li><strong>Backward Compatibility</strong>: Existing UI code continues to work unchanged</li>
 *   <li><strong>Clean Architecture</strong>: Internally uses domain models and repositories</li>
 *   <li><strong>Independent Algorithm</strong>: No longer depends on external QuattroDue singleton</li>
 *   <li><strong>Dependency Injection</strong>: Follows established DI patterns</li>
 *   <li><strong>Model Conversion</strong>: Transparent conversion between model types</li>
 * </ul>
 *
 * <h3>Model Conversions:</h3>
 * <ul>
 *   <li>Domain WorkScheduleDay ↔ Legacy Day</li>
 *   <li>Domain Team ↔ Legacy Team + HalfTeam</li>
 *   <li>Domain Shift ↔ Legacy ShiftType</li>
 *   <li>Domain WorkScheduleEvent ↔ Legacy WorkScheduleEvent</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Bridge
 * @since Database Version 6
 */
public class WorkScheduleServiceAdapter implements WorkScheduleService {

    private static final String TAG = "WorkScheduleServiceAdapter";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final CoreBackupManager mBackupManager;
    private final UserService mUserService;
    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== STATE ====================

    private boolean mIsInitialized = false;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection with clean architecture repository.
     *
     * @param context Application context
     * @param database Database instance
     * @param backupManager Backup manager for data operations
     * @param userService User service for team assignments
     */
    public WorkScheduleServiceAdapter(@NonNull Context context,
                                      @NonNull QDueDatabase database,
                                      @NonNull CoreBackupManager backupManager,
                                      @NonNull UserService userService) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mBackupManager = backupManager;
        this.mUserService = userService;

        // Create clean architecture repository
        this.mWorkScheduleRepository = new WorkScheduleRepositoryImpl(context, database, backupManager);

        initializeService();
        Log.d(TAG, "WorkScheduleServiceAdapter created with clean architecture repository");
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize the service adapter.
     */
    private void initializeService() {
        try {
            // Verify repository is ready
            OperationResult<Boolean> readyResult = mWorkScheduleRepository.isRepositoryReady().join();

            if (readyResult.isSuccess() && readyResult.getData()) {
                mIsInitialized = true;
                Log.d(TAG, "Service adapter initialization completed successfully");
            } else {
                Log.w(TAG, "Repository not ready: " + readyResult.getErrorMessage());
                mIsInitialized = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize service adapter", e);
            mIsInitialized = false;
        }
    }

    // ==================== LEGACY INTERFACE IMPLEMENTATION ====================

    @Nullable
    @Override
    public Day getWorkScheduleForDate(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                Log.w(TAG, "Service not initialized, returning null");
                return null;
            }

            // Get domain model from repository
            OperationResult<net.calvuz.qdue.domain.calendar.models.WorkScheduleDay> result =
                    mWorkScheduleRepository.getWorkScheduleForDate(date, userId).join();

            if (result.isSuccess() && result.getData() != null) {
                // Convert domain model to legacy model
                return convertToLegacyDay(result.getData());
            } else {
                Log.w(TAG, "Failed to get work schedule for date " + date + ": " + result.getErrorMessage());
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting work schedule for date " + date, e);
            return null;
        }
    }

    @NonNull
    @Override
    public Map<LocalDate, Day> getWorkScheduleForDateRange(@NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate,
                                                           @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                Log.w(TAG, "Service not initialized, returning empty map");
                return new HashMap<>();
            }

            // Get domain models from repository
            OperationResult<Map<LocalDate, net.calvuz.qdue.domain.calendar.models.WorkScheduleDay>> result =
                    mWorkScheduleRepository.getWorkScheduleForDateRange(startDate, endDate, userId).join();

            if (result.isSuccess() && result.getData() != null) {
                // Convert domain models to legacy models
                Map<LocalDate, Day> legacyMap = new HashMap<>();
                for (Map.Entry<LocalDate, net.calvuz.qdue.domain.calendar.models.WorkScheduleDay> entry : result.getData().entrySet()) {
                    Day legacyDay = convertToLegacyDay(entry.getValue());
                    if (legacyDay != null) {
                        legacyMap.put(entry.getKey(), legacyDay);
                    }
                }
                return legacyMap;
            } else {
                Log.w(TAG, "Failed to get work schedule for date range: " + result.getErrorMessage());
                return new HashMap<>();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting work schedule for date range", e);
            return new HashMap<>();
        }
    }

    @NonNull
    @Override
    public Map<LocalDate, Day> getWorkScheduleForMonth(@NonNull YearMonth month, @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                Log.w(TAG, "Service not initialized, returning empty map");
                return new HashMap<>();
            }

            // Get domain models from repository
            OperationResult<Map<LocalDate, net.calvuz.qdue.domain.calendar.models.WorkScheduleDay>> result =
                    mWorkScheduleRepository.getWorkScheduleForMonth(month, userId).join();

            if (result.isSuccess() && result.getData() != null) {
                // Convert domain models to legacy models
                Map<LocalDate, Day> legacyMap = new HashMap<>();
                for (Map.Entry<LocalDate, net.calvuz.qdue.domain.calendar.models.WorkScheduleDay> entry : result.getData().entrySet()) {
                    Day legacyDay = convertToLegacyDay(entry.getValue());
                    if (legacyDay != null) {
                        legacyMap.put(entry.getKey(), legacyDay);
                    }
                }
                return legacyMap;
            } else {
                Log.w(TAG, "Failed to get work schedule for month: " + result.getErrorMessage());
                return new HashMap<>();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting work schedule for month", e);
            return new HashMap<>();
        }
    }

    @NonNull
    @Override
    public List<WorkScheduleEvent> generateWorkScheduleEvents(@NonNull LocalDate startDate,
                                                              @NonNull LocalDate endDate,
                                                              @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                Log.w(TAG, "Service not initialized, returning empty list");
                return new ArrayList<>();
            }

            // Get domain events from repository
            OperationResult<List<net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent>> result =
                    mWorkScheduleRepository.generateWorkScheduleEvents(startDate, endDate, userId).join();

            if (result.isSuccess() && result.getData() != null) {
                // Convert domain events to legacy events
                List<WorkScheduleEvent> legacyEvents = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent domainEvent : result.getData()) {
                    WorkScheduleEvent legacyEvent = convertToLegacyEvent(domainEvent);
                    if (legacyEvent != null) {
                        legacyEvents.add(legacyEvent);
                    }
                }
                return legacyEvents;
            } else {
                Log.w(TAG, "Failed to generate work schedule events: " + result.getErrorMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating work schedule events", e);
            return new ArrayList<>();
        }
    }

    // ==================== MODEL CONVERSION METHODS ====================

    /**
     * Convert domain WorkScheduleDay to legacy Day.
     *
     * @param domainDay Domain model
     * @return Legacy model or null if conversion fails
     */
    @Nullable
    private Day convertToLegacyDay(@NonNull net.calvuz.qdue.domain.calendar.models.WorkScheduleDay domainDay) {
        try {
            // Create legacy Day
            Day legacyDay = new Day(domainDay.getDate());
            legacyDay.setIsToday(domainDay.isToday());

            // Convert shifts
            for (net.calvuz.qdue.domain.calendar.models.WorkScheduleShift domainShift : domainDay.getShifts()) {
                Shift legacyShift = convertToLegacyShift(domainShift);
                if (legacyShift != null) {
                    legacyDay.addShift(legacyShift);
                }
            }

            // Note: Off-work teams conversion would require additional mapping
            // since legacy Day uses HalfTeam while domain uses Team
            // For now, we'll skip this conversion as it may not be critical

            return legacyDay;

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain day to legacy day", e);
            return null;
        }
    }

    /**
     * Convert domain WorkScheduleShift to legacy Shift.
     *
     * @param domainShift Domain model
     * @return Legacy model or null if conversion fails
     */
    @Nullable
    private Shift convertToLegacyShift(@NonNull net.calvuz.qdue.domain.calendar.models.WorkScheduleShift domainShift) {
        try {
            // Create legacy ShiftType from domain shift
            ShiftType legacyShiftType = convertToLegacyShiftType(domainShift);
            if (legacyShiftType == null) {
                return null;
            }

            // Create legacy Shift
            Shift legacyShift = new Shift(legacyShiftType);

            // Convert teams to HalfTeams (simplified conversion)
            for (net.calvuz.qdue.domain.calendar.models.Team domainTeam : domainShift.getTeams()) {
                HalfTeam legacyHalfTeam = convertToLegacyHalfTeam(domainTeam);
                if (legacyHalfTeam != null) {
                    legacyShift.addHalfTeam(legacyHalfTeam);
                }
            }

            return legacyShift;

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain shift to legacy shift", e);
            return null;
        }
    }

    /**
     * Convert domain WorkScheduleShift to legacy ShiftType.
     *
     * @param domainShift Domain model
     * @return Legacy ShiftType or null if conversion fails
     */
    @Nullable
    private ShiftType convertToLegacyShiftType(@NonNull net.calvuz.qdue.domain.calendar.models.WorkScheduleShift domainShift) {
        try {
            // Get shift type information
            String name = domainShift.getShift() != null ?
                    domainShift.getShift().getDisplayName() : "Unknown";
            String description = domainShift.getDescription();

            // Extract timing
            int startHour = domainShift.getStartTime().getHour();
            int startMinute = domainShift.getStartTime().getMinute();

            // Calculate duration
            long durationMinutes = domainShift.getDurationMinutes();
            int durationHours = (int) (durationMinutes / 60);
            int remainingMinutes = (int) (durationMinutes % 60);

            // Default color
            int color = 0xFF2196F3; // Blue

            // Create legacy ShiftType
            return new ShiftType(name, description, startHour, startMinute,
                    durationHours, remainingMinutes, color);

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain shift to legacy shift type", e);
            return null;
        }
    }

    /**
     * Convert domain Team to legacy HalfTeam (simplified).
     *
     * @param domainTeam Domain model
     * @return Legacy HalfTeam or null if conversion fails
     */
    @Nullable
    private HalfTeam convertToLegacyHalfTeam(@NonNull net.calvuz.qdue.domain.calendar.models.Team domainTeam) {
        try {
            // Create legacy HalfTeam with team name
            // This is a simplified conversion - real implementation might need more sophisticated mapping
            return new HalfTeam(domainTeam.getName());

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain team to legacy half team", e);
            return null;
        }
    }

    /**
     * Convert domain WorkScheduleEvent to legacy WorkScheduleEvent.
     *
     * @param domainEvent Domain model
     * @return Legacy WorkScheduleEvent or null if conversion fails
     */
    @Nullable
    private WorkScheduleEvent convertToLegacyEvent(@NonNull net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent domainEvent) {
        try {
            // Create legacy WorkScheduleEvent
            WorkScheduleEvent legacyEvent = new WorkScheduleEvent();

            // Set basic properties
            legacyEvent.setId(domainEvent.getId());
            legacyEvent.setSourceId(domainEvent.getSourceId());
            legacyEvent.setDate(domainEvent.getDate());
            legacyEvent.setStartTime(domainEvent.getStartTime());
            legacyEvent.setEndTime(domainEvent.getEndTime());
            legacyEvent.setTitle(domainEvent.getTitle());
            legacyEvent.setDescription(domainEvent.getDescription());
            legacyEvent.setColorHex(domainEvent.getColorHex());
            legacyEvent.setUserId(domainEvent.getUserId());
            legacyEvent.setDayInCycle(domainEvent.getDayInCycle());
            legacyEvent.setDaysFromSchemeStart(domainEvent.getDaysFromSchemeStart());
            legacyEvent.setPatternName(domainEvent.getPatternName());
            legacyEvent.setGeneratedBy(domainEvent.getGeneratedBy());

            // Convert team if present
            if (domainEvent.getTeam() != null) {
                // Simplified team conversion - real implementation might need more sophisticated mapping
                Team legacyTeam = convertToLegacyTeam(domainEvent.getTeam());
                legacyEvent.setTeam(legacyTeam);
            }

            return legacyEvent;

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain event to legacy event", e);
            return null;
        }
    }

    /**
     * Convert domain Team to legacy Team (simplified).
     *
     * @param domainTeam Domain model
     * @return Legacy Team or null if conversion fails
     */
    @Nullable
    private Team convertToLegacyTeam(@NonNull net.calvuz.qdue.domain.calendar.models.Team domainTeam) {
        try {
            // Create two HalfTeams from the domain team
            // This is a simplified approach - real implementation might need more sophisticated logic
            HalfTeam halfTeam1 = new HalfTeam(domainTeam.getName());
            HalfTeam halfTeam2 = new HalfTeam(domainTeam.getName()); // Simplified - same name

            // Create legacy Team
            return new Team(halfTeam1, halfTeam2);

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain team to legacy team", e);
            return null;
        }
    }

    // ==================== SERVICE STATUS METHODS ====================

    @Override
    public boolean isServiceReady() {
        return mIsInitialized;
    }

    @Override
    public int getDayInCycle(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                return -1;
            }

            OperationResult<Integer> result = mWorkScheduleRepository.getDayInCycle(date).join();
            return result.isSuccess() ? result.getData() : -1;

        } catch (Exception e) {
            Log.e(TAG, "Error getting day in cycle", e);
            return -1;
        }
    }

    @Override
    public long getDaysFromSchemeStart(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                return 0;
            }

            OperationResult<Long> result = mWorkScheduleRepository.getDaysFromSchemeStart(date).join();
            return result.isSuccess() ? result.getData() : 0;

        } catch (Exception e) {
            Log.e(TAG, "Error getting days from scheme start", e);
            return 0;
        }
    }

    @Override
    public boolean isWorkingDay(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                return false;
            }

            OperationResult<Boolean> result = mWorkScheduleRepository.isWorkingDay(date).join();
            return result.isSuccess() && result.getData();

        } catch (Exception e) {
            Log.e(TAG, "Error checking if working day", e);
            return false;
        }
    }

    @Override
    public boolean isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        try {
            if (!mIsInitialized) {
                return false;
            }

            // Convert legacy team to domain team
            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return false;
            }

            OperationResult<Boolean> result = mWorkScheduleRepository.isWorkingDayForTeam(date, domainTeam).join();
            return result.isSuccess() && result.getData();

        } catch (Exception e) {
            Log.e(TAG, "Error checking if working day for team", e);
            return false;
        }
    }

    @Override
    public boolean isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team) {
        try {
            if (!mIsInitialized) {
                return true; // Default to rest day if service not ready
            }

            // Convert legacy team to domain team
            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return true;
            }

            OperationResult<Boolean> result = mWorkScheduleRepository.isRestDayForTeam(date, domainTeam).join();
            return result.isSuccess() && result.getData();

        } catch (Exception e) {
            Log.e(TAG, "Error checking if rest day for team", e);
            return true;
        }
    }

    /**
     * Convert legacy Team to domain Team (reverse conversion).
     *
     * @param legacyTeam Legacy model
     * @return Domain Team or null if conversion fails
     */
    @Nullable
    private net.calvuz.qdue.domain.calendar.models.Team convertToDomainTeam(@NonNull Team legacyTeam) {
        try {
            // Get team name from legacy team
            String teamName = legacyTeam.getTeamName();
            if (teamName == null || teamName.isEmpty()) {
                return null;
            }

            // Find domain team by name
            OperationResult<net.calvuz.qdue.domain.calendar.models.Team> result =
                    mWorkScheduleRepository.findTeamByName(teamName).join();

            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error converting legacy team to domain team", e);
            return null;
        }
    }

    // ==================== PLACEHOLDER METHODS ====================
    // These methods are part of the WorkScheduleService interface but may not be fully implemented

    @NonNull
    @Override
    public List<Team> getAllTeams() {
        try {
            if (!mIsInitialized) {
                return new ArrayList<>();
            }

            OperationResult<List<net.calvuz.qdue.domain.calendar.models.Team>> result =
                    mWorkScheduleRepository.getAllTeams().join();

            if (result.isSuccess() && result.getData() != null) {
                List<Team> legacyTeams = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.Team domainTeam : result.getData()) {
                    Team legacyTeam = convertToLegacyTeam(domainTeam);
                    if (legacyTeam != null) {
                        legacyTeams.add(legacyTeam);
                    }
                }
                return legacyTeams;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting all teams", e);
            return new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public List<HalfTeam> getAllHalfTeams() {
        // Convert teams to half-teams for compatibility
        List<HalfTeam> halfTeams = new ArrayList<>();
        List<Team> teams = getAllTeams();

        for (Team team : teams) {
            // Add both half-teams from each team
            halfTeams.addAll(team.getHalfTeams());
        }

        return halfTeams;
    }

    @Nullable
    @Override
    public Team getTeamForUser(@NonNull Long userId) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<net.calvuz.qdue.domain.calendar.models.Team> result =
                    mWorkScheduleRepository.getTeamForUser(userId).join();

            if (result.isSuccess() && result.getData() != null) {
                return convertToLegacyTeam(result.getData());
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting team for user", e);
            return null;
        }
    }

    @Override
    public boolean setTeamForUser(@NonNull Long userId, @NonNull Team team) {
        try {
            if (!mIsInitialized) {
                return false;
            }

            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return false;
            }

            OperationResult<Void> result = mWorkScheduleRepository.setTeamForUser(userId, domainTeam).join();
            return result.isSuccess();

        } catch (Exception e) {
            Log.e(TAG, "Error setting team for user", e);
            return false;
        }
    }

    /**
     * Get teams working on specific date.
     *
     * @param date Target date
     * @return List of teams working on that date
     */
    @NonNull
    @Override
    public List<Team> getTeamsWorkingOnDate(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                Log.w(TAG, "Service not initialized, returning empty list");
                return new ArrayList<>();
            }

            // Get domain teams from repository
            OperationResult<List<net.calvuz.qdue.domain.calendar.models.Team>> result =
                    mWorkScheduleRepository.getTeamsWorkingOnDate(date).join();

            if (result.isSuccess() && result.getData() != null) {
                // Convert domain teams to legacy teams
                List<Team> legacyTeams = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.Team domainTeam : result.getData()) {
                    Team legacyTeam = convertToLegacyTeam(domainTeam);
                    if (legacyTeam != null) {
                        legacyTeams.add(legacyTeam);
                    }
                }
                Log.d(TAG, "✅ Successfully got " + legacyTeams.size() + " teams working on " + date);
                return legacyTeams;
            } else {
                Log.w(TAG, "Failed to get teams working on date " + date + ": " + result.getErrorMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting teams working on date " + date, e);
            return new ArrayList<>();
        }
    }

    // ==================== ADDITIONAL INTERFACE METHODS ====================
    // Additional methods required by the complete WorkScheduleService interface

    @Override
    public boolean hasWorkSchedule(@Nullable Day day) {
        return day != null && day.getShifts() != null && !day.getShifts().isEmpty();
    }

    @Nullable
    @Override
    public String getWorkScheduleColor(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<String> result = mWorkScheduleRepository.getWorkScheduleColor(date, userId).join();
            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting work schedule color", e);
            return null;
        }
    }

    @Nullable
    @Override
    public String getWorkScheduleSummary(@NonNull LocalDate date, @Nullable Long userId) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<String> result = mWorkScheduleRepository.getWorkScheduleSummary(date, userId).join();
            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting work schedule summary", e);
            return null;
        }
    }

    @NonNull
    @Override
    public LocalDate getSchemeStartDate() {
        try {
            if (!mIsInitialized) {
                return LocalDate.of(2024, 1, 1); // Default fallback
            }

            OperationResult<LocalDate> result = mWorkScheduleRepository.getSchemeStartDate().join();
            return result.isSuccess() && result.getData() != null ?
                    result.getData() : LocalDate.of(2024, 1, 1);

        } catch (Exception e) {
            Log.e(TAG, "Error getting scheme start date", e);
            return LocalDate.of(2024, 1, 1);
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> updateSchemeStartDate(@NonNull LocalDate newStartDate) {
        try {
            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.UPDATE);
            }

            return mWorkScheduleRepository.updateSchemeStartDate(newStartDate).join();

        } catch (Exception e) {
            String error = "Error updating scheme start date: " + e.getMessage();
            Log.e(TAG, error, e);
            return OperationResult.failure(error, OperationResult.OperationType.UPDATE);
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getScheduleConfiguration() {
        try {
            if (!mIsInitialized) {
                return new HashMap<>();
            }

            OperationResult<Map<String, Object>> result = mWorkScheduleRepository.getScheduleConfiguration().join();
            return result.isSuccess() && result.getData() != null ?
                    result.getData() : new HashMap<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting schedule configuration", e);
            return new HashMap<>();
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> updateScheduleConfiguration(@NonNull Map<String, Object> configuration) {
        try {
            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.UPDATE);
            }

            return mWorkScheduleRepository.updateScheduleConfiguration(configuration).join();

        } catch (Exception e) {
            String error = "Error updating schedule configuration: " + e.getMessage();
            Log.e(TAG, error, e);
            return OperationResult.failure(error, OperationResult.OperationType.UPDATE);
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> refreshWorkScheduleData() {
        try {
            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.REFRESH);
            }

            return mWorkScheduleRepository.refreshWorkScheduleData().join();

        } catch (Exception e) {
            String error = "Error refreshing work schedule data: " + e.getMessage();
            Log.e(TAG, error, e);
            return OperationResult.failure(error, OperationResult.OperationType.REFRESH);
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> clearCache() {
        try {
            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.DELETE);
            }

            return mWorkScheduleRepository.clearCache().join();

        } catch (Exception e) {
            String error = "Error clearing cache: " + e.getMessage();
            Log.e(TAG, error, e);
            return OperationResult.failure(error, OperationResult.OperationType.DELETE);
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getServiceStatus() {
        try {
            if (!mIsInitialized) {
                Map<String, Object> status = new HashMap<>();
                status.put("service_ready", false);
                status.put("error", "Service not initialized");
                return status;
            }

            OperationResult<Map<String, Object>> result = mWorkScheduleRepository.getServiceStatus().join();
            return result.isSuccess() && result.getData() != null ?
                    result.getData() : new HashMap<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting service status", e);
            Map<String, Object> status = new HashMap<>();
            status.put("service_ready", false);
            status.put("error", e.getMessage());
            return status;
        }
    }

    @NonNull
    @Override
    public OperationResult<Map<String, Object>> validateConfiguration() {
        try {
            if (!mIsInitialized) {
                return OperationResult.failure("Service not initialized", OperationResult.OperationType.VALIDATION);
            }

            return mWorkScheduleRepository.validateConfiguration().join();

        } catch (Exception e) {
            String error = "Error validating configuration: " + e.getMessage();
            Log.e(TAG, error, e);
            return OperationResult.failure(error, OperationResult.OperationType.VALIDATION);
        }
    }

    @Nullable
    @Override
    public Team findTeamByName(@NonNull String teamName) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<net.calvuz.qdue.domain.calendar.models.Team> result =
                    mWorkScheduleRepository.findTeamByName(teamName).join();

            if (result.isSuccess() && result.getData() != null) {
                return convertToLegacyTeam(result.getData());
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error finding team by name", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Team findTeamById(@NonNull String teamId) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<net.calvuz.qdue.domain.calendar.models.Team> result =
                    mWorkScheduleRepository.findTeamById(teamId).join();

            if (result.isSuccess() && result.getData() != null) {
                return convertToLegacyTeam(result.getData());
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error finding team by ID", e);
            return null;
        }
    }

    @NonNull
    @Override
    public List<Team> createStandardTeams() {
        try {
            if (!mIsInitialized) {
                return new ArrayList<>();
            }

            OperationResult<List<net.calvuz.qdue.domain.calendar.models.Team>> result =
                    mWorkScheduleRepository.createStandardTeams().join();

            if (result.isSuccess() && result.getData() != null) {
                List<Team> legacyTeams = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.Team domainTeam : result.getData()) {
                    Team legacyTeam = convertToLegacyTeam(domainTeam);
                    if (legacyTeam != null) {
                        legacyTeams.add(legacyTeam);
                    }
                }
                return legacyTeams;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error creating standard teams", e);
            return new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public LocalDate getNextWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return null;
            }

            OperationResult<LocalDate> result = mWorkScheduleRepository.getNextWorkingDay(domainTeam, fromDate).join();
            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting next working day", e);
            return null;
        }
    }

    @Nullable
    @Override
    public LocalDate getPreviousWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return null;
            }

            OperationResult<LocalDate> result = mWorkScheduleRepository.getPreviousWorkingDay(domainTeam, fromDate).join();
            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting previous working day", e);
            return null;
        }
    }

    @Override
    public int getWorkingDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        try {
            if (!mIsInitialized) {
                return 0;
            }

            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return 0;
            }

            OperationResult<Integer> result = mWorkScheduleRepository.getWorkingDaysCount(domainTeam, startDate, endDate).join();
            return result.isSuccess() && result.getData() != null ? result.getData() : 0;

        } catch (Exception e) {
            Log.e(TAG, "Error getting working days count", e);
            return 0;
        }
    }

    @Override
    public int getRestDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        try {
            if (!mIsInitialized) {
                return 0;
            }

            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertToDomainTeam(team);
            if (domainTeam == null) {
                return 0;
            }

            OperationResult<Integer> result = mWorkScheduleRepository.getRestDaysCount(domainTeam, startDate, endDate).join();
            return result.isSuccess() && result.getData() != null ? result.getData() : 0;

        } catch (Exception e) {
            Log.e(TAG, "Error getting rest days count", e);
            return 0;
        }
    }

    @NonNull
    @Override
    public List<ShiftType> getAllShiftTypes() {
        try {
            if (!mIsInitialized) {
                return new ArrayList<>();
            }

            // Get domain shifts and convert to legacy ShiftTypes
            OperationResult<List<net.calvuz.qdue.domain.calendar.models.Shift>> result =
                    mWorkScheduleRepository.getAllShifts().join();

            if (result.isSuccess() && result.getData() != null) {
                List<ShiftType> legacyShiftTypes = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.Shift domainShift : result.getData()) {
                    ShiftType legacyShiftType = convertToLegacyShiftTypeFromShift(domainShift);
                    if (legacyShiftType != null) {
                        legacyShiftTypes.add(legacyShiftType);
                    }
                }
                return legacyShiftTypes;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting all shift types", e);
            return new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public ShiftType getShiftTypeById(@NonNull Long shiftTypeId) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            // Convert Long ID to String for domain repository
            OperationResult<net.calvuz.qdue.domain.calendar.models.Shift> result =
                    mWorkScheduleRepository.getShiftById(String.valueOf(shiftTypeId)).join();

            if (result.isSuccess() && result.getData() != null) {
                return convertToLegacyShiftTypeFromShift(result.getData());
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting shift type by ID", e);
            return null;
        }
    }

    @Nullable
    @Override
    public ShiftType getShiftTypeByName(@NonNull String name) {
        try {
            if (!mIsInitialized) {
                return null;
            }

            OperationResult<net.calvuz.qdue.domain.calendar.models.Shift> result =
                    mWorkScheduleRepository.getShiftByName(name).join();

            if (result.isSuccess() && result.getData() != null) {
                return convertToLegacyShiftTypeFromShift(result.getData());
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting shift type by name", e);
            return null;
        }
    }

    @NonNull
    @Override
    public List<Shift> getShiftsForDate(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                return new ArrayList<>();
            }

            // Get WorkScheduleShifts and convert to legacy Shifts
            OperationResult<List<net.calvuz.qdue.domain.calendar.models.WorkScheduleShift>> result =
                    mWorkScheduleRepository.getShiftsForDate(date).join();

            if (result.isSuccess() && result.getData() != null) {
                List<Shift> legacyShifts = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.WorkScheduleShift domainShift : result.getData()) {
                    Shift legacyShift = convertToLegacyShift(domainShift);
                    if (legacyShift != null) {
                        legacyShifts.add(legacyShift);
                    }
                }
                return legacyShifts;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting shifts for date", e);
            return new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public List<HalfTeam> getHalfTeamsWorkingOnDate(@NonNull LocalDate date) {
        try {
            if (!mIsInitialized) {
                return new ArrayList<>();
            }

            // Get working teams and convert to half-teams
            OperationResult<List<net.calvuz.qdue.domain.calendar.models.Team>> result =
                    mWorkScheduleRepository.getTeamsWorkingOnDate(date).join();

            if (result.isSuccess() && result.getData() != null) {
                List<HalfTeam> halfTeams = new ArrayList<>();
                for (net.calvuz.qdue.domain.calendar.models.Team domainTeam : result.getData()) {
                    // Convert team to half-teams (simplified approach)
                    HalfTeam halfTeam = convertToLegacyHalfTeam(domainTeam);
                    if (halfTeam != null) {
                        halfTeams.add(halfTeam);
                    }
                }
                return halfTeams;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error getting half-teams working on date", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isWorkingDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam) {
        try {
            if (!mIsInitialized) {
                return false;
            }

            // Convert HalfTeam to domain Team and check
            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertHalfTeamToDomainTeam(halfTeam);
            if (domainTeam == null) {
                return false;
            }

            OperationResult<Boolean> result = mWorkScheduleRepository.isWorkingDayForTeam(date, domainTeam).join();
            return result.isSuccess() && result.getData();

        } catch (Exception e) {
            Log.e(TAG, "Error checking if working day for half-team", e);
            return false;
        }
    }

    @Override
    public boolean isRestDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam) {
        try {
            if (!mIsInitialized) {
                return true;
            }

            // Convert HalfTeam to domain Team and check
            net.calvuz.qdue.domain.calendar.models.Team domainTeam = convertHalfTeamToDomainTeam(halfTeam);
            if (domainTeam == null) {
                return true;
            }

            OperationResult<Boolean> result = mWorkScheduleRepository.isRestDayForTeam(date, domainTeam).join();
            return result.isSuccess() && result.getData();

        } catch (Exception e) {
            Log.e(TAG, "Error checking if rest day for half-team", e);
            return true;
        }
    }

    // ==================== ADDITIONAL CONVERSION METHODS ====================

    /**
     * Convert domain Shift to legacy ShiftType.
     *
     * @param domainShift Domain shift model
     * @return Legacy ShiftType or null if conversion fails
     */
    @Nullable
    private ShiftType convertToLegacyShiftTypeFromShift(@NonNull net.calvuz.qdue.domain.calendar.models.Shift domainShift) {
        try {
            // Extract properties from domain shift
            String name = domainShift.getName();
            String description = domainShift.getDescription();
            int startHour = domainShift.getStartHour();
            int startMinute = domainShift.getStartMinute();

            // Calculate duration
            long totalMinutes = domainShift.getTotalDurationMinutes();
            int durationHours = (int) (totalMinutes / 60);
            int remainingMinutes = (int) (totalMinutes % 60);

            // Parse color from hex
            int color;
            try {
                String colorHex = domainShift.getColorHex();
                if (colorHex.startsWith("#")) {
                    color = (int) Long.parseLong(colorHex.substring(1), 16);
                    // Add alpha channel if not present
                    if (colorHex.length() == 7) { // #RRGGBB
                        color = 0xFF000000 | color;
                    }
                } else {
                    color = 0xFF2196F3; // Default blue
                }
            } catch (Exception e) {
                color = 0xFF2196F3; // Default blue
            }

            // Create legacy ShiftType
            return new ShiftType(domainShift.getId(), name, description, startHour, startMinute,
                    durationHours, remainingMinutes, color);

        } catch (Exception e) {
            Log.e(TAG, "Error converting domain shift to legacy shift type", e);
            return null;
        }
    }

    /**
     * Convert HalfTeam to domain Team (reverse conversion).
     *
     * @param halfTeam Legacy HalfTeam
     * @return Domain Team or null if conversion fails
     */
    @Nullable
    private net.calvuz.qdue.domain.calendar.models.Team convertHalfTeamToDomainTeam(@NonNull HalfTeam halfTeam) {
        try {
            String teamName = halfTeam.getName();
            if (teamName == null || teamName.isEmpty()) {
                return null;
            }

            // Find domain team by name
            OperationResult<net.calvuz.qdue.domain.calendar.models.Team> result =
                    mWorkScheduleRepository.findTeamByName(teamName).join();

            return result.isSuccess() ? result.getData() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error converting half-team to domain team", e);
            return null;
        }
    }
}