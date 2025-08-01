package net.calvuz.qdue.core.infrastructure.services;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleTemplate;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleType;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ SERVICE INTERFACE: Work Schedule Service following existing architecture patterns
 *
 * <p>Service interface for work schedule generation and management using consistent
 * async patterns with OperationResult for structured error handling.</p>
 *
 * <p>Key architectural features:</p>
 * <ul>
 *   <li>All operations return CompletableFuture&lt;OperationResult&lt;T&gt;&gt;</li>
 *   <li>Consistent error handling and logging</li>
 *   <li>Health monitoring and lifecycle management</li>
 *   <li>Integration with existing ServiceManager pattern</li>
 *   <li>Provider pattern for extensible schedule generation</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 5
 */
public interface WorkScheduleService {

    // ==================== SCHEDULE GENERATION OPERATIONS ====================

    /**
     * Generates work schedule events for a date range using the appropriate provider.
     *
     * @param startDate    Start date (inclusive)
     * @param endDate      End date (inclusive)
     * @param scheduleType Type of schedule to generate
     * @param userTeam     User's team identifier (optional, for filtering)
     * @return CompletableFuture with list of work schedule events
     */
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateSchedule(
            LocalDate startDate, LocalDate endDate, WorkScheduleType scheduleType, String userTeam);

    /**
     * Generates work schedule events for a single month.
     *
     * @param yearMonth    Year and month (e.g., LocalDate.of(2025, 7, 1))
     * @param scheduleType Type of schedule to generate
     * @param userTeam     User's team identifier (optional, for filtering)
     * @return CompletableFuture with list of work schedule events for the month
     */
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateMonthlySchedule(
            LocalDate yearMonth, WorkScheduleType scheduleType, String userTeam);

    /**
     * Generates work schedule events for a single date.
     *
     * @param date         Target date
     * @param scheduleType Type of schedule to generate
     * @param userTeam     User's team identifier (optional, for filtering)
     * @return CompletableFuture with list of work schedule events for the day
     */
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateDailySchedule(
            LocalDate date, WorkScheduleType scheduleType, String userTeam);

    /**
     * Generates work schedule events using a custom template.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @param template  Custom work schedule template
     * @param userTeam  User's team identifier (optional, for filtering)
     * @return CompletableFuture with list of work schedule events
     */
    CompletableFuture<OperationResult<List<WorkScheduleEvent>>> generateScheduleWithTemplate(
            LocalDate startDate, LocalDate endDate, WorkScheduleTemplate template, String userTeam);

    // ==================== TEMPLATE MANAGEMENT OPERATIONS ====================

    /**
     * Gets all available work schedule templates.
     *
     * @return CompletableFuture with list of available templates
     */
    CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getAvailableTemplates();

    /**
     * Gets predefined work schedule templates.
     *
     * @return CompletableFuture with list of predefined templates
     */
    CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getPredefinedTemplates();

    /**
     * Gets user-defined custom work schedule templates.
     *
     * @return CompletableFuture with list of custom templates
     */
    CompletableFuture<OperationResult<List<WorkScheduleTemplate>>> getCustomTemplates();

    /**
     * Gets a work schedule template by its ID.
     *
     * @param templateId Template ID
     * @return CompletableFuture with the template if found
     */
    CompletableFuture<OperationResult<WorkScheduleTemplate>> getTemplateById(Long templateId);

    /**
     * Creates a new custom work schedule template.
     *
     * @param template Template to create
     * @return CompletableFuture with created template
     */
    CompletableFuture<OperationResult<WorkScheduleTemplate>> createCustomTemplate(WorkScheduleTemplate template);

    /**
     * Updates an existing custom work schedule template.
     *
     * @param template Template to update
     * @return CompletableFuture with updated template
     */
    CompletableFuture<OperationResult<WorkScheduleTemplate>> updateCustomTemplate(WorkScheduleTemplate template);

    /**
     * Deactivates a custom work schedule template.
     *
     * @param templateId Template ID to deactivate
     * @return CompletableFuture with success/failure result
     */
    CompletableFuture<OperationResult<Void>> deactivateCustomTemplate(Long templateId);

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validates a work schedule template.
     *
     * @param template Template to validate
     * @return CompletableFuture with validation result
     */
    CompletableFuture<OperationResult<WorkScheduleValidationResult>> validateTemplate(WorkScheduleTemplate template);

    /**
     * Checks if a specific schedule type is supported.
     *
     * @param scheduleType Schedule type to check
     * @return CompletableFuture with support status
     */
    CompletableFuture<OperationResult<Boolean>> isScheduleTypeSupported(WorkScheduleType scheduleType);

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Gets information about the next scheduled shift for a team.
     *
     * @param team         Team identifier
     * @param fromDate     Date to start searching from
     * @param scheduleType Schedule type to use
     * @return CompletableFuture with next shift information
     */
    CompletableFuture<OperationResult<WorkScheduleEvent>> getNextScheduledShift(
            String team, LocalDate fromDate, WorkScheduleType scheduleType);

    /**
     * Gets teams that are working on a specific date.
     *
     * @param date         Target date
     * @param scheduleType Schedule type to use
     * @return CompletableFuture with list of working teams
     */
    CompletableFuture<OperationResult<List<String>>> getWorkingTeams(LocalDate date, WorkScheduleType scheduleType);

    /**
     * Gets teams that are off work on a specific date.
     *
     * @param date         Target date
     * @param scheduleType Schedule type to use
     * @return CompletableFuture with list of teams off work
     */
    CompletableFuture<OperationResult<List<String>>> getTeamsOffWork(LocalDate date, WorkScheduleType scheduleType);

    // ==================== STATISTICS OPERATIONS ====================

    /**
     * Gets work schedule statistics for a date range.
     *
     * @param startDate    Start date
     * @param endDate      End date
     * @param scheduleType Schedule type
     * @return CompletableFuture with schedule statistics
     */
    CompletableFuture<OperationResult<WorkScheduleStatistics>> getScheduleStatistics(
            LocalDate startDate, LocalDate endDate, WorkScheduleType scheduleType);

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Initializes the work schedule service and its providers.
     *
     * @return CompletableFuture with initialization result
     */
    CompletableFuture<OperationResult<Void>> initializeService();

    /**
     * Shuts down the service and releases resources.
     */
    void shutdown();

    // ==================== RESULT CLASSES ====================

    /**
     * Validation result for work schedule templates.
     */
    record WorkScheduleValidationResult(boolean valid, List<String> errorMessages,
                                        List<String> warningMessages) {
        public WorkScheduleValidationResult(boolean valid, List<String> errorMessages, List<String> warningMessages) {
            this.valid = valid;
            this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
            this.warningMessages = warningMessages != null ? warningMessages : new ArrayList<>();
        }

        public boolean hasWarnings() {
            return !warningMessages.isEmpty();
        }

        public String getFormattedErrorMessage() {
            return errorMessages.isEmpty() ? "" : String.join( ", ", errorMessages );
        }

        public String getFormattedWarningMessage() {
            return warningMessages.isEmpty() ? "" : String.join( ", ", warningMessages );
        }
    }

    /**
     * Statistics for work schedule analysis.
     */
    class WorkScheduleStatistics {
        private final int totalEvents;
        private final int workEvents;
        private final int restEvents;
        private final Map<String, Integer> eventsByTeam;
        private final Map<String, Integer> eventsByShiftType;
        private final double averageWorkHoursPerDay;
        private final long collectionTime;

        public WorkScheduleStatistics(int totalEvents, int workEvents, int restEvents,
                                      Map<String, Integer> eventsByTeam, Map<String, Integer> eventsByShiftType,
                                      double averageWorkHoursPerDay) {
            this.totalEvents = totalEvents;
            this.workEvents = workEvents;
            this.restEvents = restEvents;
            this.eventsByTeam = eventsByTeam != null ? eventsByTeam : new HashMap<>();
            this.eventsByShiftType = eventsByShiftType != null ? eventsByShiftType : new HashMap<>();
            this.averageWorkHoursPerDay = averageWorkHoursPerDay;
            this.collectionTime = System.currentTimeMillis();
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public int getWorkEvents() {
            return workEvents;
        }

        public int getRestEvents() {
            return restEvents;
        }

        public Map<String, Integer> getEventsByTeam() {
            return eventsByTeam;
        }

        public Map<String, Integer> getEventsByShiftType() {
            return eventsByShiftType;
        }

        public double getAverageWorkHoursPerDay() {
            return averageWorkHoursPerDay;
        }

        public long getCollectionTime() {
            return collectionTime;
        }

        public String getSummary() {
            return "Work Schedule Statistics: " + totalEvents + " total events (" +
                    workEvents + " work, " + restEvents + " rest), " +
                    String.format( QDue.getLocale(), "%.1f", averageWorkHoursPerDay ) + " avg hours/day";
        }
    }
}
