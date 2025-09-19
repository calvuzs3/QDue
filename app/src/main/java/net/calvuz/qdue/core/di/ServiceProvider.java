package net.calvuz.qdue.core.di;

import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.OrganizationService;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

/**
 * Dependency injection interface for service providers
 */
public interface ServiceProvider {



    /**
     * Get QDueUserService for simplified user management.
     *
     * <p>Provides simplified user management operations following clean architecture
     * principles with minimal onboarding and optional profile fields.</p>
     *
     * <h4>Service Features:</h4>
     * <ul>
     *   <li><strong>Minimal Onboarding</strong>: Single-step user creation</li>
     *   <li><strong>Optional Fields</strong>: Nickname and email with empty defaults</li>
     *   <li><strong>Clean Architecture</strong>: Domain-driven design with use cases</li>
     *   <li><strong>Async Operations</strong>: CompletableFuture for all operations</li>
     * </ul>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li><strong>New Applications</strong>: Simple user management needs</li>
     *   <li><strong>Onboarding Flows</strong>: Minimal friction user creation</li>
     *   <li><strong>Calendar Integration</strong>: User identification for calendar features</li>
     *   <li><strong>Profile Management</strong>: Basic profile completion workflows</li>
     * </ul>
     *
     * @return QDueUserService instance with full dependency injection
     */
    QDueUserService getQDueUserService();

    /**
     * Get CalendarService instance for work schedule and calendar operations.
     *
     * <p>Provides comprehensive calendar functionality including:
     * <ul>
     *   <li>Work schedule generation for users and teams</li>
     *   <li>Calendar event creation from schedules</li>
     *   <li>Team management and assignments</li>
     *   <li>Shift template management</li>
     *   <li>Exception handling for schedule modifications</li>
     * </ul>
     *
     * The service uses clean domain models with full localization support
     * and follows the OperationResult pattern for consistent error handling.
     *
     * @return CalendarService instance
     * @throws RuntimeException if service initialization fails
     */
    CalendarService getCalendarService();

    /**
     * Get EventsService instance
     */
    EventsService getEventsService();

    /**
     * Get UserService instance
     */
    UserService getUserService();

    /**
     * Get OrganizationService instance
     */
    OrganizationService getOrganizationService();

    /**
     * Get CoreBackupManager instance
     */
    CoreBackupManager getCoreBackupManager();

    /**
     * Get CalendarServiceProvider for domain-specific calendar services.
     *
     * <p>Provides access to specialized calendar domain services including:</p>
     * <ul>
     *   <li><strong>Domain Repositories</strong>: TeamRepository, ShiftRepository, etc.</li>
     *   <li><strong>Domain Engines</strong>: RecurrenceCalculator, ExceptionResolver, SchedulingEngine</li>
     *   <li><strong>Use Cases</strong>: GenerateUserScheduleUseCase, GenerateTeamScheduleUseCase, etc.</li>
     * </ul>
     *
     * <p>This provider is used internally by CalendarService and WorkScheduleRepository
     * for complex calendar operations that require multiple specialized repositories.</p>
     *
     * @return CalendarServiceProvider instance with all domain services
     * @throws RuntimeException if calendar services initialization fails
     */
    CalendarServiceProvider getCalendarServiceProvider();


    /**
     * Get WorkScheduleRepository instance.
     *
     * <p>High-level repository for work schedule operations that orchestrates
     * multiple domain repositories through CalendarServiceProvider. This is
     * the primary interface for work schedule generation and management.</p>
     *
     * <p><strong>Note:</strong> This method delegates to CalendarServiceProvider
     * for proper dependency injection and avoids creating WorkScheduleRepository
     * with wrong dependencies.</p>
     *
     * @return WorkScheduleRepository instance from CalendarServiceProvider
     * @throws RuntimeException if WorkScheduleRepository cannot be created
     */
    WorkScheduleRepository getWorkScheduleService();

    /**
     * Initialize all services
     */
    void initializeServices();

    /**
     * Shutdown all services
     */
    void shutdownServices();

    /**
     * Check if services are ready
     */
    boolean areServicesReady();
}


