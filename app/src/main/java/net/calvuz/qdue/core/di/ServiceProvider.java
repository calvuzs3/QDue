package net.calvuz.qdue.core.di;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
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



    /// Get QDueUserService for simplified user management.
    ///
    /// Provides simplified user management operations following clean architecture
    /// principles with minimal onboarding and optional profile fields.
    /// #### Service Features:
    ///
    ///     - **Minimal Onboarding**: Single-step user creation
    ///     - **Optional Fields**: Nickname and email with empty defaults
    ///     - **Clean Architecture**: Domain-driven design with use cases
    ///     - **Async Operations**: CompletableFuture for all operations
    ///
    /// #### Use Cases:
    ///
    ///     - **New Applications**: Simple user management needs
    ///     - **Onboarding Flows**: Minimal friction user creation
    ///     - **Calendar Integration**: User identification for calendar features
    ///     - **Profile Management**: Basic profile completion workflows
    ///
    ///
    /// @return QDueUserService instance with full dependency injection
    QDueUserService getQDueUserService();

    /// Get CalendarService instance for work schedule and calendar operations.
    ///
    /// Provides comprehensive calendar functionality including:
    ///
    ///     - Work schedule generation for users and teams
    ///     - Calendar event creation from schedules
    ///     - Team management and assignments
    ///     - Shift template management
    ///     - Exception handling for schedule modifications
    ///
    /// The service uses clean domain models with full localization support
    /// and follows the OperationResult pattern for consistent error handling.
    ///
    /// @return CalendarService instance
    /// @throws RuntimeException if service initialization fails
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
     * Get LocaleManager instance
     */
    LocaleManager getLocaleManager();

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


