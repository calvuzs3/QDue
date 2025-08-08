package net.calvuz.qdue.core.di;

import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.OrganizationService;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.services.WorkScheduleService;

/**
 * Dependency injection interface for service providers
 */
public interface ServiceProvider {

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

    WorkScheduleService getWorkScheduleService();

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


