package net.calvuz.qdue.data.di;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.engines.ExceptionResolver;
import net.calvuz.qdue.domain.calendar.engines.RecurrenceCalculator;
import net.calvuz.qdue.domain.calendar.engines.SchedulingEngine;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftExceptionRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.ApplyShiftExceptionsUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateTeamScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GetScheduleStatsUseCase;
import net.calvuz.qdue.domain.calendar.usecases.UseCaseFactory;

/**
 * CalendarServiceProvider - Domain-Specific Dependency Injection Interface
 *
 * <p>Specialized service provider for calendar domain components, complementing the core
 * ServiceProvider for legacy services. Provides clean separation between core services
 * (.core.services) and domain-specific calendar services (.domain.calendar) following
 * clean architecture principles.</p>
 *
 * <h3>Architectural Positioning:</h3>
 * <ul>
 *   <li><strong>Data Layer DI</strong>: Located in .data.di package for repository orchestration</li>
 *   <li><strong>Domain Service Provider</strong>: Complements .core.di.ServiceProvider for domain needs</li>
 *   <li><strong>Clean Architecture</strong>: Bridges data implementations with domain abstractions</li>
 *   <li><strong>Legacy Separation</strong>: Keeps new calendar services separate from core legacy services</li>
 * </ul>
 *
 * <h3>Service Categories:</h3>
 * <ul>
 *   <li><strong>Domain Repositories</strong>: Data access layer with clean architecture compliance</li>
 *   <li><strong>Domain Engines</strong>: Pure algorithm implementations for business logic</li>
 *   <li><strong>Use Cases</strong>: Business operation orchestrators</li>
 *   <li><strong>Factory Services</strong>: Component creation and configuration</li>
 * </ul>
 *
 * <h3>Integration Strategy:</h3>
 * <ul>
 *   <li><strong>CalendarService Integration</strong>: Used by CalendarService to replace massive constructor injection</li>
 *   <li><strong>ServiceProvider Bridge</strong>: Works alongside core ServiceProvider, not replacing it</li>
 *   <li><strong>Repository Orchestration</strong>: Coordinates specialized repositories for complex operations</li>
 *   <li><strong>Domain Engine Access</strong>: Provides configured domain engines with localization</li>
 * </ul>
 *
 * <h3>Lifecycle Management:</h3>
 * <ul>
 *   <li><strong>Lazy Initialization</strong>: Components created only when first accessed</li>
 *   <li><strong>Singleton Pattern</strong>: Single instance per component type within calendar domain</li>
 *   <li><strong>Thread Safety</strong>: Safe for concurrent access across calendar operations</li>
 *   <li><strong>Resource Cleanup</strong>: Proper disposal and resource management</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Domain-Specific DI for Calendar Services
 * @since Clean Architecture Phase 3
 */
public interface CalendarServiceProvider {

    // ==================== DOMAIN REPOSITORIES ====================

    /**
     * Get RecurrenceRuleRepository for Google Calendar-style recurrence pattern management.
     *
     * <p>Provides CRUD operations for recurrence rules supporting DAILY, WEEKLY, MONTHLY,
     * and QUATTRODUE_CYCLE patterns with JSON-based rule storage and caching.</p>
     *
     * @return RecurrenceRuleRepository instance with database persistence
     */
    @NonNull
    RecurrenceRuleRepository getRecurrenceRuleRepository();

    /**
     * Get ShiftExceptionRepository for comprehensive shift exception management.
     *
     * <p>Handles all types of schedule exceptions including absences, shift changes,
     * time reductions, and approval workflows with priority-based conflict resolution.</p>
     *
     * @return ShiftExceptionRepository instance with localized workflow support
     */
    @NonNull
    ShiftExceptionRepository getShiftExceptionRepository();

    /**
     * Get TeamRepository for team management and user-team assignments.
     *
     * <p>Manages team definitions, user memberships, and team-based scheduling
     * operations with support for QuattroDue standard teams (A through I).</p>
     *
     * @return TeamRepository instance with localized team descriptions
     */
    @NonNull
    TeamRepository getTeamRepository();

    /**
     * Get ShiftRepository for shift template and timing management.
     *
     * <p>Manages shift type definitions, timing templates, and shift configuration
     * with support for standard shift patterns (Morning, Afternoon, Night).</p>
     *
     * @return ShiftRepository instance with localized shift descriptions
     */
    @NonNull
    ShiftRepository getShiftRepository();

    /**
     * Get UserScheduleAssignmentRepository for user-team assignment management.
     *
     * <p>Handles user-to-team schedule assignments, temporary transfers, assignment
     * conflicts, and priority-based resolution with time boundary support.</p>
     *
     * @return UserScheduleAssignmentRepository instance with localized assignment status
     */
    @NonNull
    UserScheduleAssignmentRepository getUserScheduleAssignmentRepository();

    /**
     * Get WorkScheduleRepository for high-level work schedule orchestration.
     *
     * <p>Primary repository that orchestrates all other repositories and domain engines
     * to provide complete work schedule generation with exception handling.</p>
     *
     * @return WorkScheduleRepository instance with full orchestration capabilities
     */
    @NonNull
    WorkScheduleRepository getWorkScheduleRepository();

    // ==================== DOMAIN ENGINES ====================

    /**
     * Get RecurrenceCalculator for RRULE processing and date generation.
     *
     * <p>Advanced recurrence calculation engine supporting Google Calendar-compatible
     * RRULE patterns with QuattroDue-specific extensions and optimized date generation.</p>
     *
     * @return RecurrenceCalculator instance with localization support
     */
    @NonNull
    RecurrenceCalculator getRecurrenceCalculator();

    /**
     * Get ExceptionResolver for priority-based shift exception resolution.
     *
     * <p>Sophisticated exception resolution algorithm for handling conflicts, overrides,
     * and modifications to work schedules with comprehensive business rules.</p>
     *
     * @return ExceptionResolver instance with conflict resolution capabilities
     */
    @NonNull
    ExceptionResolver getExceptionResolver();

    /**
     * Get SchedulingEngine for complete schedule generation orchestration.
     *
     * <p>Master scheduling algorithm that coordinates recurrence calculation and exception
     * resolution to generate complete work schedules with validation and optimization.</p>
     *
     * @return SchedulingEngine instance with full orchestration support
     */
    @NonNull
    SchedulingEngine getSchedulingEngine();

    // ==================== USE CASES ====================

    /**
     * Get GenerateUserScheduleUseCase for individual user schedule operations.
     *
     * <p>Comprehensive user schedule generation with recurrence rules, exception handling,
     * team assignments, and business rule validation for single-user operations.</p>
     *
     * @return GenerateUserScheduleUseCase instance
     */
    @NonNull
    GenerateUserScheduleUseCase getGenerateUserScheduleUseCase();

    /**
     * Get GenerateTeamScheduleUseCase for team coordination operations.
     *
     * <p>Team-wide schedule generation with coverage analysis, resource optimization,
     * conflict detection, and multi-team coordination capabilities.</p>
     *
     * @return GenerateTeamScheduleUseCase instance
     */
    @NonNull
    GenerateTeamScheduleUseCase getGenerateTeamScheduleUseCase();

    /**
     * Get ApplyShiftExceptionsUseCase for exception handling workflow.
     *
     * <p>Comprehensive shift exception processing with approval workflows, conflict
     * resolution, and business rule validation for exception management.</p>
     *
     * @return ApplyShiftExceptionsUseCase instance
     */
    @NonNull
    ApplyShiftExceptionsUseCase getApplyShiftExceptionsUseCase();

    /**
     * Get GetScheduleStatsUseCase for analytics and reporting.
     *
     * <p>Comprehensive schedule analytics, statistics generation, validation
     * capabilities, and reporting for management and planning purposes.</p>
     *
     * @return GetScheduleStatsUseCase instance
     */
    @NonNull
    GetScheduleStatsUseCase getGetScheduleStatsUseCase();

    /**
     * Get UseCaseFactory for convenient access to all use cases.
     *
     * <p>Factory providing configured use case instances with proper repository
     * dependencies and consistent configuration across all use cases.</p>
     *
     * @return UseCaseFactory instance with all use cases configured
     */
    @NonNull
    UseCaseFactory getUseCaseFactory();

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Initialize all calendar domain services and infrastructure.
     *
     * <p>Performs initial setup of repositories, domain engines, and use cases
     * with proper dependency resolution and configuration.</p>
     */
    void initializeCalendarServices();

    /**
     * Check if all calendar services are properly initialized and ready for use.
     *
     * <p>Verifies that all dependencies are available and services are in a usable state.</p>
     *
     * @return true if all calendar services are ready, false otherwise
     */
    boolean areCalendarServicesReady();

    /**
     * Shutdown all calendar services and cleanup resources.
     *
     * <p>Performs graceful shutdown of all calendar services, closes connections,
     * and releases resources.</p>
     */
    void shutdownCalendarServices();

    /**
     * Get detailed information about calendar service status and health.
     *
     * <p>Provides comprehensive status information for monitoring, debugging,
     * and health checks specific to calendar domain services.</p>
     *
     * @return CalendarServiceStatus with detailed service information
     */
    @NonNull
    CalendarServiceStatus getCalendarServiceStatus();

    // ==================== SERVICE STATUS ====================

    /**
         * CalendarServiceStatus - Comprehensive calendar service health information.
         */
        record CalendarServiceStatus(boolean servicesReady, boolean repositoriesInitialized,
                                     boolean enginesInitialized, boolean useCasesInitialized,
                                     long initializationTime, int activeRepositories, int activeEngines,
                                     int activeUseCases, String statusMessage) {

        @NonNull
            @Override
            public String toString() {
                return "CalendarServiceStatus{" +
                        "ready=" + servicesReady +
                        ", repos=" + repositoriesInitialized +
                        ", engines=" + enginesInitialized +
                        ", useCases=" + useCasesInitialized +
                        ", activeRepos=" + activeRepositories +
                        ", activeEngines=" + activeEngines +
                        ", activeUseCases=" + activeUseCases +
                        ", message='" + statusMessage + '\'' +
                        '}';
            }
        }
}