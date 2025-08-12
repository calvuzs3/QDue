package net.calvuz.qdue.core.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.models.Team;
import net.calvuz.qdue.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.services.models.OperationResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * WorkScheduleService - Service interface for work schedule operations.
 *
 * <p>Provides comprehensive work schedule management functionality for the QuattroDue
 * pattern system. This interface maintains backward compatibility with existing UI code
 * while supporting both legacy and clean architecture implementations.</p>
 *
 * <h3>Architecture Support:</h3>
 * <ul>
 *   <li><strong>Legacy Implementation</strong>: Uses existing QuattroDue singleton</li>
 *   <li><strong>Clean Architecture</strong>: Uses WorkScheduleServiceAdapter with domain models</li>
 *   <li><strong>Backward Compatibility</strong>: Same interface for both implementations</li>
 * </ul>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Schedule Generation</strong>: Calculate work schedules for any date range</li>
 *   <li><strong>Team Management</strong>: Handle team assignments and rotations</li>
 *   <li><strong>Shift Types</strong>: Manage shift type definitions and configurations</li>
 *   <li><strong>Pattern Calculation</strong>: Apply QuattroDue 4-2 pattern calculations</li>
 *   <li><strong>Calendar Integration</strong>: Provide data for calendar views</li>
 * </ul>
 *
 * <h3>Pattern Details:</h3>
 * <ul>
 *   <li><strong>QuattroDue Pattern</strong>: 4 consecutive work days, 2 rest days</li>
 *   <li><strong>18-Day Cycle</strong>: Complete rotation cycle for all teams</li>
 *   <li><strong>3 Shifts per Day</strong>: Morning, Afternoon, Night shifts</li>
 *   <li><strong>9 Teams</strong>: Teams A through I in rotation</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Compatible
 * @since Database Version 6
 */
public interface WorkScheduleService {

    // ==================== SCHEDULE GENERATION ====================

    /**
     * Get work schedule for a specific date.
     * Returns complete Day object with all shift information.
     *
     * @param date Target date
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return Day object with schedule information, or null if no schedule
     */
    @Nullable
    Day getWorkScheduleForDate(@NonNull LocalDate date, @Nullable Long userId);

    /**
     * Get work schedule for a date range.
     * Returns map of dates to Day objects with schedule information.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return Map of dates to Day objects with schedule information
     */
    @NonNull
    Map<LocalDate, Day> getWorkScheduleForDateRange(@NonNull LocalDate startDate,
                                                    @NonNull LocalDate endDate,
                                                    @Nullable Long userId);

    /**
     * Get work schedule for a complete month.
     * Convenience method for calendar integration.
     *
     * @param month Target month
     * @param userId Optional user ID for team filtering (null for all teams)
     * @return Map of dates to Day objects for the month
     */
    @NonNull
    Map<LocalDate, Day> getWorkScheduleForMonth(@NonNull YearMonth month, @Nullable Long userId);

    /**
     * Generate work schedule events for date range.
     * Returns volatile WorkScheduleEvent objects for integration with events system.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param userId Optional user ID for team filtering
     * @return List of WorkScheduleEvent objects
     */
    @NonNull
    List<WorkScheduleEvent> generateWorkScheduleEvents(@NonNull LocalDate startDate,
                                                       @NonNull LocalDate endDate,
                                                       @Nullable Long userId);

    // ==================== TEAM MANAGEMENT ====================

    /**
     * Get all available complete teams.
     * Returns Teams composed of HalfTeam pairs.
     *
     * @return List of all Team objects
     */
    @NonNull
    List<Team> getAllTeams();

    /**
     * Get all available half-teams (for compatibility).
     *
     * @return List of all HalfTeam objects
     */
    @NonNull
    List<HalfTeam> getAllHalfTeams();

    /**
     * Get team for specific user.
     *
     * @param userId User ID
     * @return Team object, or null if not found
     */
    @Nullable
    Team getTeamForUser(@NonNull Long userId);

    /**
     * Set team for user.
     *
     * @param userId User ID
     * @param team Team to assign
     * @return true if assignment was successful
     */
    boolean setTeamForUser(@NonNull Long userId, @NonNull Team team);

    /**
     * Get teams working on specific date.
     *
     * @param date Target date
     * @return List of teams working on that date
     */
    @NonNull
    List<Team> getTeamsWorkingOnDate(@NonNull LocalDate date);

    /**
     * Get half-teams working on specific date (compatibility method).
     *
     * @param date Target date
     * @return List of half-teams working on that date
     */
    @NonNull
    List<HalfTeam> getHalfTeamsWorkingOnDate(@NonNull LocalDate date);

    // ==================== SHIFT TYPE MANAGEMENT ====================

    /**
     * Get all available shift types.
     *
     * @return List of all ShiftType objects
     */
    @NonNull
    List<ShiftType> getAllShiftTypes();

    /**
     * Get shift type by ID.
     *
     * @param shiftTypeId Shift type ID
     * @return ShiftType object, or null if not found
     */
    @Nullable
    ShiftType getShiftTypeById(@NonNull Long shiftTypeId);

    /**
     * Get shift type by name.
     *
     * @param name Shift type name
     * @return ShiftType object, or null if not found
     */
    @Nullable
    ShiftType getShiftTypeByName(@NonNull String name);

    /**
     * Get shifts for specific date.
     *
     * @param date Target date
     * @return List of Shift objects for that date
     */
    @NonNull
    List<Shift> getShiftsForDate(@NonNull LocalDate date);

    // ==================== SCHEDULE CONFIGURATION ====================

    /**
     * Get current scheme start date.
     * This is the reference date used for pattern calculations.
     *
     * @return Scheme start date
     */
    @NonNull
    LocalDate getSchemeStartDate();

    /**
     * Update scheme start date.
     * This affects all schedule calculations going forward.
     *
     * @param newStartDate New scheme start date
     * @return Operation result
     */
    @NonNull
    OperationResult<Void> updateSchemeStartDate(@NonNull LocalDate newStartDate);

    /**
     * Get current schedule configuration.
     * Returns configuration parameters for the work schedule system.
     *
     * @return Map of configuration key-value pairs
     */
    @NonNull
    Map<String, Object> getScheduleConfiguration();

    /**
     * Update schedule configuration.
     *
     * @param configuration Configuration parameters to update
     * @return Operation result
     */
    @NonNull
    OperationResult<Void> updateScheduleConfiguration(@NonNull Map<String, Object> configuration);

    // ==================== PATTERN CALCULATIONS ====================

    /**
     * Calculate which day in the 18-day cycle a date falls on.
     *
     * @param date Target date
     * @return Day in cycle (0-17), or -1 if calculation fails
     */
    int getDayInCycle(@NonNull LocalDate date);

    /**
     * Calculate how many days between scheme start and target date.
     *
     * @param date Target date
     * @return Number of days from scheme start
     */
    long getDaysFromSchemeStart(@NonNull LocalDate date);

    /**
     * Check if a date is a working day for any team.
     *
     * @param date Target date
     * @return true if any team is working
     */
    boolean isWorkingDay(@NonNull LocalDate date);

    /**
     * Check if a date is a working day for specific team.
     *
     * @param date Target date
     * @param team Team to check
     * @return true if team is working
     */
    boolean isWorkingDayForTeam(@NonNull LocalDate date, @NonNull Team team);

    /**
     * Check if a date is a working day for specific half-team (compatibility).
     *
     * @param date Target date
     * @param halfTeam HalfTeam to check
     * @return true if half-team is working
     */
    boolean isWorkingDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam);

    /**
     * Check if a date is a rest day for specific team.
     *
     * @param date Target date
     * @param team Team to check
     * @return true if team is resting
     */
    boolean isRestDayForTeam(@NonNull LocalDate date, @NonNull Team team);

    /**
     * Check if a date is a rest day for specific half-team (compatibility).
     *
     * @param date Target date
     * @param halfTeam HalfTeam to check
     * @return true if half-team is resting
     */
    boolean isRestDayForHalfTeam(@NonNull LocalDate date, @NonNull HalfTeam halfTeam);

    // ==================== CALENDAR INTEGRATION ====================

    /**
     * Check if a Day object has work schedule data.
     * Utility method for calendar display logic.
     *
     * @param day Day to check
     * @return true if day has schedule data
     */
    boolean hasWorkSchedule(@Nullable Day day);

    /**
     * Get display color for work schedule on specific date.
     * Returns appropriate color for calendar display.
     *
     * @param date Target date
     * @param userId Optional user ID for team-specific coloring
     * @return Color hex string, or null if no schedule
     */
    @Nullable
    String getWorkScheduleColor(@NonNull LocalDate date, @Nullable Long userId);

    /**
     * Get work schedule summary for date.
     * Returns human-readable summary string.
     *
     * @param date Target date
     * @param userId Optional user ID for personalized summary
     * @return Summary string, or null if no schedule
     */
    @Nullable
    String getWorkScheduleSummary(@NonNull LocalDate date, @Nullable Long userId);

    // ==================== SERVICE STATUS ====================

    /**
     * Check if service is ready for operations.
     *
     * @return true if service is ready
     */
    boolean isServiceReady();

    /**
     * Refresh work schedule data.
     * Forces recalculation of all cached schedule information.
     *
     * @return Operation result
     */
    @NonNull
    OperationResult<Void> refreshWorkScheduleData();

    /**
     * Clear work schedule cache.
     * Clears all cached schedule calculations.
     *
     * @return Operation result
     */
    @NonNull
    OperationResult<Void> clearCache();

    /**
     * Get service status information.
     * Returns diagnostic information about service state.
     *
     * @return Map of status information
     */
    @NonNull
    Map<String, Object> getServiceStatus();

    // ==================== VALIDATION ====================

    /**
     * Validate work schedule configuration.
     * Checks if current configuration is valid and complete.
     *
     * @return Operation result containing validation details
     */
    @NonNull
    OperationResult<Map<String, Object>> validateConfiguration();

    // ==================== TEAM UTILITIES ====================

    /**
     * Find team by name.
     * Convenience method for team lookup.
     *
     * @param teamName Name of the team to find
     * @return Team object, or null if not found
     */
    @Nullable
    Team findTeamByName(@NonNull String teamName);

    /**
     * Find team by ID.
     * Convenience method for team lookup.
     *
     * @param teamId ID of the team to find
     * @return Team object, or null if not found
     */
    @Nullable
    Team findTeamById(@NonNull String teamId);

    /**
     * Create standard teams for the work schedule system.
     * This creates the default team structure (A, B, C, D, E, F, G, H, I).
     *
     * @return List of created teams
     */
    @NonNull
    List<Team> createStandardTeams();

    // ==================== ADVANCED PATTERN OPERATIONS ====================

    /**
     * Get next working day for specific team.
     *
     * @param team Team to check
     * @param fromDate Starting date (exclusive)
     * @return Next working date, or null if none found in reasonable range
     */
    @Nullable
    LocalDate getNextWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate);

    /**
     * Get previous working day for specific team.
     *
     * @param team Team to check
     * @param fromDate Starting date (exclusive)
     * @return Previous working date, or null if none found in reasonable range
     */
    @Nullable
    LocalDate getPreviousWorkingDay(@NonNull Team team, @NonNull LocalDate fromDate);

    /**
     * Get working days count for team in date range.
     *
     * @param team Team to check
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Working days count
     */
    int getWorkingDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Get rest days count for team in date range.
     *
     * @param team Team to check
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Rest days count
     */
    int getRestDaysCount(@NonNull Team team, @NonNull LocalDate startDate, @NonNull LocalDate endDate);
}