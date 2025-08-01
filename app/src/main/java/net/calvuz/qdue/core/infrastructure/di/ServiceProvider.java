package net.calvuz.qdue.core.infrastructure.di;

import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.ShiftTypeService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.OrganizationService;
import net.calvuz.qdue.core.infrastructure.backup.CoreBackupManager;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;

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
     * Get ShiftTypeService instance
     */
    ShiftTypeService getShiftTypeService();

    /**
     * Get WorkScheduleService instance
     */
    WorkScheduleService getWorkScheduleService();

    /**
     * Get CoreBackupManager instance
     */
    CoreBackupManager getCoreBackupManager();

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


