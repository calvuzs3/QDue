package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.core.services.models.OperationResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WorkScheduleRepository - Clean Architecture repository interface for work schedule operations.
 *
 * <p>Provides comprehensive asynchronous work schedule management functionality following
 * clean architecture principles. This repository is independent of external frameworks
 * and focuses purely on business operations for the work schedule domain.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Schedule Generation</strong>: Calculate work schedules for any date range</li>
 *   <li><strong>Team Management</strong>: Handle team assignments and rotations</li>
 *   <li><strong>Shift Type Management</strong>: Manage shift type definitions from database</li>
 *   <li><strong>Pattern Calculation</strong>: Apply work pattern calculations (4-2 cycle)</li>
 *   <li><strong>Calendar Integration</strong>: Provide data for calendar views</li>
 * </ul>
 *
 * <h3>Domain Model Usage:</h3>
 * <ul>
 *   <li><strong>WorkScheduleDay</strong>: Represents a complete day with shifts</li>
 *   <li><strong>WorkScheduleShift</strong>: Individual shift within a day</li>
 *   <li><strong>Team</strong>: Work team assignments</li>
 *   <li><strong>WorkScheduleEvent</strong>: Volatile events for calendar integration</li>
 *   <li><strong>ShiftType</strong>: Persistent shift type definitions</li>
 * </ul>
 *
 * <h3>Async Operations:</h3>
 * <ul>
 *   <li>All operations return {@code CompletableFuture<OperationResult<T>>}</li>
 *   <li>Background thread execution for database operations</li>
 *   <li>Consistent error handling with {@code OperationResult} pattern</li>
 *   <li>Thread-safe caching mechanisms</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation
 * @since Database Version 6
 */
public interface WorkScheduleRepository {

    // ==================== SCHEDULE GENERATION ====================

    /**
     * Get work schedule for a specific date.
     * Returns complete WorkScheduleDay object with all shift information.
     *
     * @param date Target date
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return CompletableFuture with WorkScheduleDay object wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<WorkScheduleDay>> getWorkScheduleForDate(@NonNull LocalDate date,
                                                                               @Nullable String userId);

    /**
     * Get work schedule for a date range.
     * Returns map of dates to WorkScheduleDay objects with schedule information.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return CompletableFuture with Map of dates to WorkScheduleDay objects wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable String userId);

    @NonNull
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getUserWorkScheduleForDateRange(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable String userId);

    @NonNull
    CompletableFuture<OperationResult<WorkScheduleDay>> getUserWorkScheduleForDate(
            @NonNull LocalDate date, @Nullable String userId);

    /**
     * Get work schedule for a complete month.
     * Convenience method for calendar integration.
     *
     * @param month Target month
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return CompletableFuture with Map of dates to WorkScheduleDay objects for the month wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getWorkScheduleForMonth(
            @NonNull YearMonth month, @Nullable String userId);

    /**
     * Generate work schedule events for date range.
     * Returns volatile WorkScheduleEvent objects for integration with events system.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param userId Optional user ID for team filtering
     * @return CompletableFuture with List of WorkScheduleEvent objects wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateWorkScheduleEvents(
            @NonNull LocalDate startDate, @NonNull LocalDate endDate, @Nullable String userId);

    // ==================== TEAM MANAGEMENT ====================

    /**
     * Get all available teams.
     * Returns domain Team objects.
     *
     * @return CompletableFuture with List of all Team objects wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<Team>>> getAllTeams();

    /**
     * Get team for specific user.
     *
     * @param userId User ID
     * @return CompletableFuture with Team object wrapped in OperationResult (null data if not found)
     */
    @NonNull
    CompletableFuture<OperationResult<Team>> getTeamForUser(@NonNull String userId);

    /**
     * Set team for user.
     *
     * @param userId User ID
     * @param team Team to assign
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> setTeamForUser(@NonNull String userId, @NonNull Team team);

    /**
     * Get teams working on specific date.
     *
     * @param date Target date
     * @return CompletableFuture with List of teams working on that date wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<Team>>> getTeamsWorkingOnDate(@NonNull LocalDate date);

    // ==================== SHIFT TYPE MANAGEMENT ====================

    /**
     * Get all available shifts from database.
     * Shifts are now persistent entities stored in database.
     *
     * @return CompletableFuture with List of all Shift objects wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<Shift>>> getAllShifts();

    /**
     * Get shift by ID.
     *
     * @param shiftId Shift ID
     * @return CompletableFuture with Shift object wrapped in OperationResult (null data if not found)
     */
    @NonNull
    CompletableFuture<OperationResult<Shift>> getShiftById(@NonNull String shiftId);

    /**
     * Get shift by name.
     *
     * @param name Shift name
     * @return CompletableFuture with Shift object wrapped in OperationResult (null data if not found)
     */
    @NonNull
    CompletableFuture<OperationResult<Shift>> getShiftByName(@NonNull String name);

    /**
     * Create or update shift in database.
     *
     * @param shift Shift to save
     * @return CompletableFuture with saved Shift wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Shift>> saveShift(@NonNull Shift shift);

    /**
     * Delete shift from database.
     *
     * @param shiftId ID of shift to delete
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Object>> deleteShift(@NonNull String shiftId);

    /**
     * Get work schedule shifts for specific date.
     *
     * @param date Target date
     * @return CompletableFuture with List of WorkScheduleShift objects for that date wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<WorkScheduleShift>>> getShiftsForDate(@NonNull LocalDate date);

    // ==================== SCHEDULE CONFIGURATION ====================

    /**
     * Get current scheme start date.
     * This is the reference date used for pattern calculations.
     *
     * @return CompletableFuture with scheme start date wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<LocalDate>> getSchemeStartDate();

    /**
     * Update scheme start date.
     * This affects all schedule calculations going forward.
     *
     * @param newStartDate New scheme start date
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> updateSchemeStartDate(@NonNull LocalDate newStartDate);

    /**
     * Get current schedule configuration.
     * Returns configuration parameters for the work schedule system.
     *
     * @return CompletableFuture with Map of configuration key-value pairs wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Map<String, Object>>> getScheduleConfiguration();

    /**
     * Update schedule configuration.
     *
     * @param configuration Configuration parameters to update
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> updateScheduleConfiguration(@NonNull Map<String, Object> configuration);

    // ==================== PATTERN CALCULATIONS ====================

    /**
     * Calculate which day in the pattern cycle a date falls on.
     * For QuattroDue pattern, this returns position in 18-day cycle.
     *
     * @param date Target date
     * @return CompletableFuture with day in cycle (0-based) wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getDayInCycle(@NonNull LocalDate date);

    /**
     * Check if a date is a working day for any team.
     *
     * @param date Target date
     * @return CompletableFuture with boolean result wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> isWorkingDay(@NonNull LocalDate date);

    /**
     * Check if a date is a working day for specific team.
     *
     * @param date Target date
     * @param team Team to check
     * @return CompletableFuture with boolean result wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team);

    /**
     * Check if a date is a rest day for specific team.
     *
     * @param date Target date
     * @param team Team to check
     * @return CompletableFuture with boolean result wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team);

    // ==================== CALENDAR INTEGRATION ====================

    /**
     * Check if a WorkScheduleDay object has work schedule data.
     * Utility method for calendar display logic.
     *
     * @param day WorkScheduleDay to check
     * @return CompletableFuture with boolean result wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> hasWorkSchedule(@Nullable WorkScheduleDay day);

    /**
     * Get display color for work schedule on specific date.
     * Returns appropriate color for calendar display.
     *
     * @param date Target date
     * @param userId Optional user ID for team-specific coloring
     * @return CompletableFuture with color hex string wrapped in OperationResult (null data if no schedule)
     */
    @NonNull
    CompletableFuture<OperationResult<String>> getWorkScheduleColor(@NonNull LocalDate date, @Nullable String userId);

    /**
     * Get work schedule summary for date.
     * Returns human-readable summary string.
     *
     * @param date Target date
     * @param userId Optional user ID for personalized summary
     * @return CompletableFuture with summary string wrapped in OperationResult (null data if no schedule)
     */
    @NonNull
    CompletableFuture<OperationResult<String>> getWorkScheduleSummary(@NonNull LocalDate date, @Nullable String userId);

    // ==================== DATA MANAGEMENT ====================

    /**
     * Refresh work schedule data.
     * Forces recalculation of all cached schedule information.
     *
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> refreshWorkScheduleData();

    /**
     * Clear work schedule cache.
     * Clears all cached schedule calculations.
     *
     * @return CompletableFuture with operation result
     */
    @NonNull
    CompletableFuture<OperationResult<Void>> clearCache();

    /**
     * Get service status information.
     * Returns diagnostic information about repository state.
     *
     * @return CompletableFuture with Map of status information wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Map<String, Object>>> getServiceStatus();

    // ==================== VALIDATION ====================

    /**
     * Validate work schedule configuration.
     * Checks if current configuration is valid and complete.
     *
     * @return CompletableFuture with operation result containing validation details
     */
    @NonNull
    CompletableFuture<OperationResult<Map<String, Object>>> validateConfiguration();

    /**
     * Check if repository is ready for operations.
     *
     * @return CompletableFuture with boolean result wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> isRepositoryReady();

    // ==================== TEAM UTILITIES ====================

    /**
     * Find team by name.
     * Convenience method for team lookup.
     *
     * @param teamName Name of the team to find
     * @return CompletableFuture with Team object wrapped in OperationResult (null data if not found)
     */
    @NonNull
    CompletableFuture<OperationResult<Team>> findTeamByName(@NonNull String teamName);

    /**
     * Find team by ID.
     * Convenience method for team lookup.
     *
     * @param teamId ID of the team to find
     * @return CompletableFuture with Team object wrapped in OperationResult (null data if not found)
     */
    @NonNull
    CompletableFuture<OperationResult<Team>> findTeamById(@NonNull String teamId);

    /**
     * Create standard teams for the work schedule system.
     * This creates the default team structure (A, B, C, D, E, F, G, H, I).
     *
     * @return CompletableFuture with List of created teams wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<List<Team>>> createStandardTeams();

    // ==================== ADVANCED PATTERN OPERATIONS ====================

    /**
     * Get next working day for specific team.
     *
     * @param team Team to check
     * @param fromDate Starting date (exclusive)
     * @return CompletableFuture with next working date wrapped in OperationResult (null if none found in reasonable range)
     */
    @NonNull
    CompletableFuture<OperationResult<LocalDate>> getNextWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate);

    /**
     * Get previous working day for specific team.
     *
     * @param team Team to check
     * @param fromDate Starting date (exclusive)
     * @return CompletableFuture with previous working date wrapped in OperationResult (null if none found in reasonable range)
     */
    @NonNull
    CompletableFuture<OperationResult<LocalDate>> getPreviousWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate);

    /**
     * Get working days count for team in date range.
     *
     * @param team Team to check
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with working days count wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getWorkingDaysCount(@NonNull Team team,
                                                                    @NonNull LocalDate startDate,
                                                                    @NonNull LocalDate endDate);

    /**
     * Get rest days count for team in date range.
     *
     * @param team Team to check
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with rest days count wrapped in OperationResult
     */
    @NonNull
    CompletableFuture<OperationResult<Integer>> getRestDaysCount(@NonNull Team team,
                                                                 @NonNull LocalDate startDate,
                                                                 @NonNull LocalDate endDate);
}