package net.calvuz.qdue.core.services;

import android.content.Context;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.core.services.impl.EstablishmentServiceImpl;
import net.calvuz.qdue.core.services.impl.EventsServiceImpl;
import net.calvuz.qdue.core.services.impl.MacroDepartmentServiceImpl;
import net.calvuz.qdue.core.services.impl.OrganizationServiceImpl;
import net.calvuz.qdue.core.services.impl.SubDepartmentServiceImpl;
import net.calvuz.qdue.core.services.impl.UserServiceImpl;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REFACTORED: ServiceManager - Dependency Injection Compliant
 * <p>
 * ✅ Public constructor for ServiceProvider injection
 * ✅ Removed singleton pattern
 * ✅ Uses public constructors for all service implementations
 * ✅ Proper dependency management with QDueDatabase
 * ✅ Thread-safe operations without static instances
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";

    private final Context mContext;
    private final QDueDatabase mDatabase;

    // Service instances
    private EventsServiceImpl mEventsService;
    private UserServiceImpl mUserService;
    private EstablishmentServiceImpl mEstablishmentService;
    private MacroDepartmentServiceImpl mMacroDepartmentService;
    private SubDepartmentServiceImpl mSubDepartmentService;
    private OrganizationServiceImpl mOrganizationService;
    private CoreBackupManager mCoreBackupManager;

    private boolean mServicesInitialized = false;

    // ==================== ✅ REFACTORED: PUBLIC CONSTRUCTOR ====================

    /**
     * ✅ Public constructor for dependency injection
     * No more singleton pattern - ServiceProvider manages instance
     */
    public ServiceManager(Context context, QDueDatabase database) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;

        initializeServices();
        Log.d(TAG, "ServiceManager initialized via dependency injection");
    }

    // ==================== SERVICE INITIALIZATION ====================

    /**
     * ✅ REFACTORED: Initialize all services using public constructors
     */
    private void initializeServices() {
        Log.d(TAG, "Initializing all application services...");

        try {
            // Initialize backup manager first (other services depend on it)
            mCoreBackupManager = new CoreBackupManager(mContext, mDatabase);

            // Initialize core services
            mEventsService = new EventsServiceImpl(mContext, mDatabase);
            mUserService = new UserServiceImpl(mContext, mDatabase);

            // Initialize organization services
            mEstablishmentService = new EstablishmentServiceImpl(mContext, mDatabase);
            mMacroDepartmentService = new MacroDepartmentServiceImpl(mContext, mDatabase);
            mSubDepartmentService = new SubDepartmentServiceImpl(mContext, mDatabase);

            // Initialize composite service (depends on individual services)
            mOrganizationService = new OrganizationServiceImpl(mContext, mDatabase);

            mServicesInitialized = true;
            Log.d(TAG, "✅ All services initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize services", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }

    // ==================== SERVICE ACCESS METHODS ====================

    public EventsService getEventsService() {
        return mEventsService;
    }

    public UserService getUserService() {
        return mUserService;
    }

    public EstablishmentService getEstablishmentService() {
        return mEstablishmentService;
    }

    public MacroDepartmentService getMacroDepartmentService() {
        return mMacroDepartmentService;
    }

    public SubDepartmentService getSubDepartmentService() {
        return mSubDepartmentService;
    }

    public OrganizationService getOrganizationService() {
        return mOrganizationService;
    }

    public CoreBackupManager getCoreBackupManager() {
        return mCoreBackupManager;
    }

    // ==================== SERVICE HEALTH AND STATUS ====================

    public boolean areAllServicesInitialized() {
        return mServicesInitialized &&
                mEventsService != null &&
                mUserService != null &&
                mEstablishmentService != null &&
                mMacroDepartmentService != null &&
                mSubDepartmentService != null &&
                mOrganizationService != null &&
                mCoreBackupManager != null;
    }

    public ServiceStatus getServiceStatus() {
        ServiceStatus status = new ServiceStatus();

        status.eventsServiceInitialized = (mEventsService != null);
        status.userServiceInitialized = (mUserService != null);
        status.establishmentServiceInitialized = (mEstablishmentService != null);
        status.macroDepartmentServiceInitialized = (mMacroDepartmentService != null);
        status.subDepartmentServiceInitialized = (mSubDepartmentService != null);
        status.organizationServiceInitialized = (mOrganizationService != null);
        status.coreBackupManagerInitialized = (mCoreBackupManager != null);

        status.allServicesReady = areAllServicesInitialized();
        status.initializationTime = System.currentTimeMillis();

        return status;
    }

    /**
     * ✅ REFACTORED: Health check with OperationResult pattern
     */
    public CompletableFuture<OperationResult<ServiceHealthCheck>> performHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            ServiceHealthCheck healthCheck = new ServiceHealthCheck();

            try {
                // Check Events Service
                healthCheck.eventsServiceHealthy = checkEventsServiceHealth();

                // Check User Service
                healthCheck.userServiceHealthy = checkUserServiceHealth();

                // Check Organization Services
                healthCheck.organizationServicesHealthy = checkOrganizationServicesHealth();

                // Check Backup Manager
                healthCheck.backupManagerHealthy = checkBackupManagerHealth();

                // Overall health
                healthCheck.overallHealthy = healthCheck.eventsServiceHealthy &&
                        healthCheck.userServiceHealthy &&
                        healthCheck.organizationServicesHealthy &&
                        healthCheck.backupManagerHealthy;

                healthCheck.checkTime = System.currentTimeMillis();

                Log.d(TAG, "Service health check completed - Overall healthy: " + healthCheck.overallHealthy);

                return OperationResult.success(healthCheck, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Service health check failed", e);
                healthCheck.overallHealthy = false;
                healthCheck.errorMessage = e.getMessage();
                return OperationResult.failure("Health check failed: " + e.getMessage(), OperationResult.OperationType.VALIDATION);
            }
        });
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * ✅ REFACTORED: Application-wide backup with OperationResult
     */
    public CompletableFuture<OperationResult<String>> triggerApplicationWideBackup() {
        Log.d(TAG, "Triggering application-wide backup...");

        if (mCoreBackupManager != null) {
            return mCoreBackupManager.performFullApplicationBackup()
                    .thenApply(result -> {
                        if (result.isSuccess()) {
                            Log.d(TAG, "Application-wide backup completed successfully");
                        } else {
                            Log.e(TAG, "Application-wide backup failed: " + result.getFormattedErrorMessage());
                        }
                        return result;
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Application-wide backup exception", throwable);
                        return OperationResult.failure("Backup failed: " + throwable.getMessage(), OperationResult.OperationType.BACKUP);
                    });
        } else {
            return CompletableFuture.completedFuture(
                    OperationResult.failure("Backup manager not initialized", OperationResult.OperationType.BACKUP)
            );
        }
    }

    /**
     * ✅ REFACTORED: Statistics collection with OperationResult
     */
    public CompletableFuture<OperationResult<ApplicationStatistics>> getApplicationStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            ApplicationStatistics stats = new ApplicationStatistics();

            try {
                // Events statistics
                if (mEventsService != null) {
                    OperationResult<Integer> eventsCountResult = mEventsService.getEventsCount().get();
                    if (eventsCountResult.isSuccess()) {
                        stats.totalEvents = eventsCountResult.getData();
                    }

                    OperationResult<Map<EventType, Integer>> eventsByTypeResult = mEventsService.getEventsCountByType().get();
                    if (eventsByTypeResult.isSuccess()) {
                        stats.eventsByType = eventsByTypeResult.getData();
                    }
                }

                // User statistics
                if (mUserService != null) {
                    OperationResult<Integer> usersCountResult = mUserService.getUsersCount().get();
                    if (usersCountResult.isSuccess()) {
                        stats.totalUsers = usersCountResult.getData();
                    }
                }

                // Organization statistics
                if (mEstablishmentService != null) {
                    OperationResult<Integer> establishmentsCountResult = mEstablishmentService.getEstablishmentsCount().get();
                    if (establishmentsCountResult.isSuccess()) {
                        stats.totalEstablishments = establishmentsCountResult.getData();
                    }
                }
                if (mMacroDepartmentService != null) {
                    OperationResult<Integer> macroDeptsCountResult = mMacroDepartmentService.getMacroDepartmentsCount().get();
                    if (macroDeptsCountResult.isSuccess()) {
                        stats.totalMacroDepartments = macroDeptsCountResult.getData();
                    }
                }
                if (mSubDepartmentService != null) {
                    OperationResult<Integer> subDeptsCountResult = mSubDepartmentService.getSubDepartmentsCount().get();
                    if (subDeptsCountResult.isSuccess()) {
                        stats.totalSubDepartments = subDeptsCountResult.getData();
                    }
                }

                // Backup statistics
                if (mCoreBackupManager != null) {
                    stats.backupStatus = mCoreBackupManager.getBackupStatus();
                }

                stats.totalEntities = stats.totalEvents + stats.totalUsers + stats.totalEstablishments +
                        stats.totalMacroDepartments + stats.totalSubDepartments;

                Log.d(TAG, "Application statistics collected: " + stats.totalEntities + " total entities");

                return OperationResult.success(stats, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to collect application statistics", e);
                stats.errorMessage = e.getMessage();
                return OperationResult.failure("Statistics collection failed: " + e.getMessage(), OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    public void shutdown() {
        Log.d(TAG, "Shutting down all services...");

        try {
            if (mEventsService != null) {
                mEventsService.shutdown();
            }
            if (mUserService != null) {
                mUserService.shutdown();
            }
            if (mEstablishmentService != null) {
                mEstablishmentService.shutdown();
            }
            if (mMacroDepartmentService != null) {
                mMacroDepartmentService.shutdown();
            }
            if (mSubDepartmentService != null) {
                mSubDepartmentService.shutdown();
            }
            if (mOrganizationService != null) {
                mOrganizationService.shutdown();
            }
            if (mCoreBackupManager != null) {
                mCoreBackupManager.shutdown();
            }

            mServicesInitialized = false;
            Log.d(TAG, "All services shut down successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error during service shutdown", e);
        }
    }

    /**
     * ✅ REFACTORED: Restart with proper reinitialization
     */
    public CompletableFuture<OperationResult<Void>> restart() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Restarting all services...");

                shutdown();

                // Clear instances to force reinitialization
                mEventsService = null;
                mUserService = null;
                mEstablishmentService = null;
                mMacroDepartmentService = null;
                mSubDepartmentService = null;
                mOrganizationService = null;
                mCoreBackupManager = null;

                // Reinitialize
                initializeServices();

                Log.d(TAG, "All services restarted successfully");
                return OperationResult.success("Restart Success", OperationResult.OperationType.INITIALIZATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to restart services", e);
                return OperationResult.failure("Restart failed: " + e.getMessage(), OperationResult.OperationType.INITIALIZATION);
            }
        });
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * ✅ REFACTORED: Health checks with OperationResult pattern
     */
    private boolean checkEventsServiceHealth() {
        try {
            if (mEventsService == null) return false;
            OperationResult<Integer> result = mEventsService.getEventsCount().get();
            return result.isSuccess() && result.getData() >= 0;
        } catch (Exception e) {
            Log.w(TAG, "Events service health check failed", e);
            return false;
        }
    }

    private boolean checkUserServiceHealth() {
        try {
            if (mUserService == null) return false;
            OperationResult<Integer> result = mUserService.getUsersCount().get();
            return result.isSuccess() && result.getData() >= 0;
        } catch (Exception e) {
            Log.w(TAG, "User service health check failed", e);
            return false;
        }
    }

    private boolean checkOrganizationServicesHealth() {
        try {
            if (mEstablishmentService == null || mMacroDepartmentService == null ||
                    mSubDepartmentService == null || mOrganizationService == null) {
                return false;
            }

            OperationResult<Integer> result = mEstablishmentService.getEstablishmentsCount().get();
            return result.isSuccess() && result.getData() >= 0;
        } catch (Exception e) {
            Log.w(TAG, "Organization services health check failed", e);
            return false;
        }
    }

    private boolean checkBackupManagerHealth() {
        try {
            return mCoreBackupManager != null &&
                    mCoreBackupManager.getBackupStatus() != null;
        } catch (Exception e) {
            Log.w(TAG, "Backup manager health check failed", e);
            return false;
        }
    }

    // ==================== INNER CLASSES ====================

    public static class ServiceStatus {
        public boolean eventsServiceInitialized;
        public boolean userServiceInitialized;
        public boolean establishmentServiceInitialized;
        public boolean macroDepartmentServiceInitialized;
        public boolean subDepartmentServiceInitialized;
        public boolean organizationServiceInitialized;
        public boolean coreBackupManagerInitialized;
        public boolean allServicesReady;
        public long initializationTime;

        public String getSummary() {
            int initializedCount = 0;
            if (eventsServiceInitialized) initializedCount++;
            if (userServiceInitialized) initializedCount++;
            if (establishmentServiceInitialized) initializedCount++;
            if (macroDepartmentServiceInitialized) initializedCount++;
            if (subDepartmentServiceInitialized) initializedCount++;
            if (organizationServiceInitialized) initializedCount++;
            if (coreBackupManagerInitialized) initializedCount++;

            return initializedCount + "/7 services initialized";
        }
    }

    public static class ServiceHealthCheck {
        public boolean eventsServiceHealthy;
        public boolean userServiceHealthy;
        public boolean organizationServicesHealthy;
        public boolean backupManagerHealthy;
        public boolean overallHealthy;
        public long checkTime;
        public String errorMessage;

        public String getHealthSummary() {
            if (overallHealthy) {
                return "All services are healthy";
            } else {
                return "Service health issues detected" +
                        (errorMessage != null ? ": " + errorMessage : "");
            }
        }
    }

    public static class ApplicationStatistics {
        public int totalEvents;
        public int totalUsers;
        public int totalEstablishments;
        public int totalMacroDepartments;
        public int totalSubDepartments;
        public int totalEntities;

        public Map<EventType, Integer> eventsByType;
        public CoreBackupManager.BackupStatus backupStatus;

        public String errorMessage;
        public long collectionTime;

        public ApplicationStatistics() {
            this.collectionTime = System.currentTimeMillis();
        }

        public String getSummary() {
            if (errorMessage != null) {
                return "Statistics collection failed: " + errorMessage;
            }
            return "Total entities: " + totalEntities +
                    " (Events: " + totalEvents +
                    ", Users: " + totalUsers +
                    ", Organizations: " + (totalEstablishments + totalMacroDepartments + totalSubDepartments) + ")";
        }
    }
}