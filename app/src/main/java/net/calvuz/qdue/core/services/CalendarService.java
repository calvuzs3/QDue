package net.calvuz.qdue.core.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleEvent;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.ShiftException;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * CalendarService - Core Service Interface for QuattroDue Calendar Operations
 *
 * <p>Service interface for calendar and work schedule operations following the
 * QDue ServiceProvider architecture. Provides operations for work schedule
 * management using clean domain models with localization support.</p>
 *
 * <h3>Service Responsibilities:</h3>
 * <ul>
 *   <li><strong>Work Schedule Generation</strong>: Generate schedules for users and teams</li>
 *   <li><strong>Calendar Events</strong>: Create volatile calendar events from schedules</li>
 *   <li><strong>Team Management</strong>: Team assignments and lookups</li>
 *   <li><strong>Shift Templates</strong>: Manage shift type definitions</li>
 *   <li><strong>Exception Handling</strong>: Process shift exceptions and modifications</li>
 *   <li><strong>User Assignments</strong>: Manage user-to-team schedule assignments</li>
 * </ul>
 *
 * <h3>Integration with QDue Architecture:</h3>
 * <ul>
 *   <li><strong>ServiceProvider Pattern</strong>: Registered in ServiceProviderImpl</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture for all database operations</li>
 *   <li><strong>OperationResult Pattern</strong>: Consistent error handling pattern</li>
 *   <li><strong>Domain Models</strong>: Uses clean domain models from calendar package</li>
 *   <li><strong>Localization Support</strong>: Integrates with LocaleManager and DomainLocalizer</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Via ServiceProvider
 * CalendarService calendarService = serviceProvider.getCalendarService();
 *
 * // Get user schedule for today
 * CompletableFuture&lt;OperationResult&lt;WorkScheduleDay&gt;&gt; result =
 *     calendarService.getUserScheduleForDate(userID, LocalDate.now());
 * </pre>
 */
public interface CalendarService {

    @NonNull
    CalendarServiceProvider getCalendarServiceProvider();

    // ==================== RECURRENCE RULE OPERATIONS ====================

    /**
     * Get all available recurrence rules.
     *
     * @return CompletableFuture with List of RecurrenceRule objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<RecurrenceRule>>> getAllRecurrenceRules();

    // ==================== WORK SCHEDULE OPERATIONS ====================

    /**
     * Get work schedule for specific user on specific date.
     *
     * @param userId User ID for schedule lookup
     * @param date Target date for schedule generation
     * @return CompletableFuture with user's WorkScheduleDay or error
     */
    @NonNull
    CompletableFuture<OperationResult<WorkScheduleDay>> getUserScheduleForDate(
            @NonNull String userId, @NonNull LocalDate date);

    /**
     * Get work schedule for specific user over date range.
     *
     * @param userId User ID for schedule lookup
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with Map of dates to WorkScheduleDay objects
     */
    @NonNull
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getUserScheduleForDateRange(
            @NonNull String userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Get work schedule for specific user for complete month.
     *
     * @param userId User ID for schedule lookup
     * @param month Target month (year and month)
     * @return CompletableFuture with monthly schedule map
     */
    @NonNull
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> getUserScheduleForMonth(
            @NonNull String userId, @NonNull YearMonth month);

    /**
     * Get team schedule for specific date.
     *
     * @param date Target date for schedule generation
     * @param teamId Optional team ID for filtering (null for all teams)
     * @return CompletableFuture with team WorkScheduleDay
     */
    @NonNull
    CompletableFuture<OperationResult<WorkScheduleDay>> getTeamScheduleForDate(
            @NonNull LocalDate date, @Nullable String teamId);

    // ==================== CALENDAR EVENTS OPERATIONS ====================

    /**
     * Generate volatile calendar events for user in date range.
     *
     * @param userId User ID for event generation
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with List of WorkScheduleEvent objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> getCalendarEventsForUser(
            @NonNull String userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Generate volatile calendar events for team in date range.
     *
     * @param teamId Team ID for event generation
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with List of WorkScheduleEvent objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> getCalendarEventsForTeam(
            @NonNull String teamId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    // ==================== TEAM MANAGEMENT ====================

    /**
     * Get all available teams.
     *
     * @return CompletableFuture with List of Team objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<Team>>> getAllTeams();

    /**
     * Get team by ID.
     *
     * @param teamId Team ID to look up
     * @return CompletableFuture with Team object or null if not found
     */
    @NonNull
    CompletableFuture<OperationResult<Team>> getTeamById(@NonNull String teamId);

    /**
     * Get team assignment for specific user on specific date.
     *
     * @param userId User ID for team lookup
     * @param date Target date for assignment lookup
     * @return CompletableFuture with Team assignment or null if not assigned
     */
    @NonNull
    CompletableFuture<OperationResult<Team>> getTeamForUser(@NonNull String userId, @NonNull LocalDate date);

    // ==================== SHIFT TEMPLATES MANAGEMENT ====================

    /**
     * Get all available shift templates.
     *
     * @return CompletableFuture with List of Shift templates
     */
    @NonNull
    CompletableFuture<OperationResult<List<Shift>>> getAllShiftTemplates();

    /**
     * Get shift template by ID.
     *
     * @param shiftId Shift ID to look up
     * @return CompletableFuture with Shift template or null if not found
     */
    @NonNull
    CompletableFuture<OperationResult<Shift>> getShiftTemplateById(@NonNull String shiftId);

    // ==================== USER ASSIGNMENT MANAGEMENT ====================

    /**
     * Get user schedule assignment for specific date.
     *
     * @param userId User ID for assignment lookup
     * @param date Target date for assignment lookup
     * @return CompletableFuture with UserScheduleAssignment or null if not assigned
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> getUserAssignmentForDate(
            @NonNull String userId, @NonNull LocalDate date);

    /**
     * Get all users with active schedule assignments.
     *
     * @return CompletableFuture with List of active user IDs
     */
    @NonNull
    CompletableFuture<OperationResult<List<String>>> getActiveUsers();

    // ==================== EXCEPTION MANAGEMENT ====================

    /**
     * Get shift exceptions for user on specific date.
     *
     * @param userId User ID for exception lookup
     * @param date Target date for exception lookup
     * @return CompletableFuture with List of ShiftException objects
     */
    @NonNull
    CompletableFuture<OperationResult<List<ShiftException>>> getShiftExceptionsForUser(
            @NonNull String userId, @NonNull LocalDate date);

    /**
     * Create a new shift exception.
     *
     * @param shiftException ShiftException to create
     * @return CompletableFuture with created ShiftException
     */
    @NonNull
    CompletableFuture<OperationResult<ShiftException>> createShiftException(
            @NonNull ShiftException shiftException);

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Clear schedule cache for performance optimization.
     */
    void clearScheduleCache();

    /**
     * Clear cache for specific date range.
     *
     * @param startDate Start date for cache clearing
     * @param endDate End date for cache clearing
     */
    void clearCacheForDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate);

    // ==================== SERVICE LIFECYCLE ====================

    /**
     * Initialize service resources and connections.
     */
    void initialize();

    /**
     * Check if service is ready for operations.
     *
     * @return true if service is ready, false otherwise
     */
    boolean isReady();

    /**
     * Cleanup service resources.
     */
    void cleanup();
}