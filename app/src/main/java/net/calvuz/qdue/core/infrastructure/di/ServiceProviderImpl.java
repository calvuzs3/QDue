package net.calvuz.qdue.core.infrastructure.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.ShiftTypeService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.OrganizationService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.core.infrastructure.services.impl.EventsServiceImpl;
import net.calvuz.qdue.core.infrastructure.services.impl.ShiftTypeServiceImpl;
import net.calvuz.qdue.core.infrastructure.services.impl.UserServiceImpl;
import net.calvuz.qdue.core.infrastructure.services.impl.OrganizationServiceImpl;
import net.calvuz.qdue.core.infrastructure.backup.CoreBackupManager;
import net.calvuz.qdue.core.infrastructure.db.QDueDatabase;
import net.calvuz.qdue.core.infrastructure.services.impl.WorkScheduleServiceImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * ServiceProvider implementation for dependency injection
 * <p>
 * Provides centralized service management with lazy initialization
 * and proper lifecycle management.
 * <p>
 * Features:
 * - Lazy service initialization
 * - Thread-safe singleton pattern
 * - Proper error handling and logging
 * - Service health monitoring
 * - Graceful shutdown handling
 */
public class ServiceProviderImpl implements ServiceProvider {

    private static final String TAG = "ServiceProviderImpl";

    // Singleton instance
    private static volatile ServiceProviderImpl INSTANCE;

    // Application context
    private final Context mContext;

    // Core services - lazy initialized
    private volatile EventsService mEventsService;
    private volatile UserService mUserService;
    private volatile OrganizationService mOrganizationService;
    private volatile ShiftTypeService mShiftTypeService;
    private volatile WorkScheduleService mWorkScheduleService;
    private volatile CoreBackupManager mCoreBackupManager;

    // Service dependencies
    private QDueDatabase mDatabase;

    // State tracking
    private volatile boolean mServicesInitialized = false;
    private volatile boolean mServicesShutdown = false;

    // Synchronization objects
    private final Object mEventsServiceLock = new Object();
    private final Object mUserServiceLock = new Object();
    private final Object mOrganizationServiceLock = new Object();
    private final Object mShiftTypeServiceLock = new Object();
    private final Object mWorkScheduleServiceLock = new Object();
    private final Object mBackupManagerLock = new Object();
    private final Object mInitializationLock = new Object();

    /**
     * Private constructor for singleton pattern
     */
    private ServiceProviderImpl(Context context) {
        mContext = context.getApplicationContext();
        Log.d(TAG, "ServiceProvider created");
    }

    /**
     * Get singleton instance
     */
    public static ServiceProviderImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceProviderImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceProviderImpl(context);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void initializeServices() {
        synchronized (mInitializationLock) {
            if (mServicesInitialized || mServicesShutdown) {
                Log.d(TAG, "Services already initialized or shutdown, skipping");
                return;
            }

            try {
                Log.d(TAG, "Initializing services...");

                // Initialize database first
                mDatabase = QDueDatabase.getInstance(mContext);

                // Services will be lazy-initialized when first accessed
                mServicesInitialized = true;

                Log.d(TAG, "✅ Services initialization completed");

            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing services: " + e.getMessage());
                throw new RuntimeException("Service initialization failed", e);
            }
        }
    }

    @Override
    public EventsService getEventsService() {
        if (mEventsService == null) {
            synchronized (mEventsServiceLock) {
                if (mEventsService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating EventsService instance");
                    mEventsService = new EventsServiceImpl(mContext, mDatabase);
                }
            }
        }
        return mEventsService;
    }

    @Override
    public UserService getUserService() {
        if (mUserService == null) {
            synchronized (mUserServiceLock) {
                if (mUserService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating UserService instance");
                    mUserService = new UserServiceImpl(mContext, mDatabase);
                }
            }
        }
        return mUserService;
    }

    @Override
    public OrganizationService getOrganizationService() {
        if (mOrganizationService == null) {
            synchronized (mOrganizationServiceLock) {
                if (mOrganizationService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating OrganizationService instance");
                    mOrganizationService = new OrganizationServiceImpl(mContext, mDatabase);
                }
            }
        }
        return mOrganizationService;
    }

    @Override
    public ShiftTypeService getShiftTypeService() {
        if (mShiftTypeService == null) {
            synchronized (mShiftTypeServiceLock) {
                if (mShiftTypeService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating ShiftTypeService instance");
                    mShiftTypeService = new ShiftTypeServiceImpl(mContext, mDatabase);
                }
            }
        }
        return mShiftTypeService;
    }


    @Override
    public WorkScheduleService getWorkScheduleService() {
        if (mWorkScheduleService == null) {
            synchronized (mWorkScheduleServiceLock) {
                if (mWorkScheduleService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating WorkScheduleService instance");
                    mWorkScheduleService = new WorkScheduleServiceImpl(
                            mContext,
                            mDatabase,
                            mShiftTypeService);
                }
            }
        }
        return mWorkScheduleService;
    }

    @Override
    public CoreBackupManager getCoreBackupManager() {
        if (mCoreBackupManager == null) {
            synchronized (mBackupManagerLock) {
                if (mCoreBackupManager == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating CoreBackupManager instance");
                    mCoreBackupManager = new CoreBackupManager(mContext, mDatabase);
                }
            }
        }
        return mCoreBackupManager;
    }

    @Override
    public void shutdownServices() {
        synchronized (mInitializationLock) {
            if (mServicesShutdown) {
                Log.d(TAG, "Services already shutdown");
                return;
            }

            Log.d(TAG, "Shutting down services...");

            try {
                // Shutdown services in reverse order of dependency
                if (mCoreBackupManager != null) {
                    // CoreBackupManager shutdown if needed
                    Log.d(TAG, "CoreBackupManager shutdown");
                }

                if (mWorkScheduleService != null) {
                    // WorkScheduleService shutdown if needed
                    Log.d(TAG, "WorkScheduleService shutdown");
                }

                if (mShiftTypeService != null) {
                    // ShiftTypeService shutdown if needed
                    Log.d(TAG, "ShiftTypeService shutdown");
                }

                if (mOrganizationService != null) {
                    // OrganizationService shutdown if needed
                    Log.d(TAG, "OrganizationService shutdown");
                }

                if (mUserService != null) {
                    // UserService shutdown if needed
                    Log.d(TAG, "UserService shutdown");
                }

                if (mEventsService != null) {
                    // EventsService shutdown if needed
                    Log.d(TAG, "EventsService shutdown");
                }

                mServicesShutdown = true;
                Log.d(TAG, "✅ Services shutdown completed");

            } catch (Exception e) {
                Log.e(TAG, "❌ Error during service shutdown: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean areServicesReady() {
        return mServicesInitialized && !mServicesShutdown && mDatabase != null;
    }

    /**
     * Ensure services are initialized before use
     */
    private void ensureInitialized() {
        if (!mServicesInitialized) {
            synchronized (mInitializationLock) {
                if (!mServicesInitialized) {
                    initializeServices();
                }
            }
        }

        if (mServicesShutdown) {
            throw new IllegalStateException("Services have been shutdown and cannot be used");
        }
    }

    /**
     * Get service health status for debugging
     */
    public ServiceHealthStatus getServiceHealthStatus() {
        return new ServiceHealthStatus(
                mServicesInitialized,
                mServicesShutdown,
                mDatabase != null,
                mEventsService != null,
                mUserService != null,
                mOrganizationService != null,
                mShiftTypeService != null,
                mWorkScheduleService != null,
                mCoreBackupManager != null
        );
    }

    /**
         * Service health status data class
         */
        public record ServiceHealthStatus(boolean initialized, boolean shutdown,
                                          boolean databaseReady,
                                          boolean eventsServiceCreated,
                                          boolean userServiceCreated,
                                          boolean organizationServiceCreated,
                                          boolean shiftTypeServiceCreated,
                                          boolean workScheduleServiceCreated,
                                          boolean backupManagerCreated) {

        public boolean isHealthy() {
                return initialized && !shutdown && databaseReady;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format(
                        "ServiceHealth{initialized=%s, shutdown=%s, database=%s, " +
                                "events=%s, user=%s, org=%s, shitfttype =%s, workschedule=%s," +
                                "backup=%s}",
                        initialized, shutdown, databaseReady,
                        eventsServiceCreated, userServiceCreated,
                        organizationServiceCreated, shiftTypeServiceCreated,
                        workScheduleServiceCreated, backupManagerCreated
                );
            }
        }
}
