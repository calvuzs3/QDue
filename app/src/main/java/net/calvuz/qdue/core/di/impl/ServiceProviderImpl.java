package net.calvuz.qdue.core.di.impl;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.OrganizationService;
import net.calvuz.qdue.core.services.impl.CalendarServiceImpl;
import net.calvuz.qdue.core.services.impl.EventsServiceImpl;
import net.calvuz.qdue.core.services.impl.UserServiceImpl;
import net.calvuz.qdue.core.services.impl.OrganizationServiceImpl;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.di.CalendarServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * ServiceProviderImpl - Updated with CalendarServiceProvider Integration
 *
 * <p>Core ServiceProvider implementation that orchestrates both legacy services
 * and modern calendar domain services through proper dependency injection hierarchy.
 * Provides centralized service management with lazy initialization and proper lifecycle.</p>
 *
 * <h3>Service Architecture:</h3>
 * <ul>
 *   <li><strong>Core Services</strong>: EventsService, UserService, OrganizationService</li>
 *   <li><strong>Calendar Service</strong>: Application service using CalendarServiceProvider</li>
 *   <li><strong>Calendar Domain</strong>: Specialized calendar services via CalendarServiceProvider</li>
 *   <li><strong>Infrastructure</strong>: Database, backup, locale management</li>
 * </ul>
 *
 * <h3>Dependency Hierarchy:</h3>
 * <pre>
 * ServiceProvider (this)
 *   ├── Core Services (Events, User, Organization)
 *   ├── CalendarServiceProvider
 *   │   ├── Domain Repositories
 *   │   ├── Domain Engines
 *   │   └── Use Cases
 *   └── CalendarService (uses CalendarServiceProvider)
 * </pre>
 *
 * @author QDue Development Team
 * @version 3.0.0 - CalendarServiceProvider Integration
 * @since Clean Architecture Phase 3
 */
public class ServiceProviderImpl implements ServiceProvider {

    private static final String TAG = "ServiceProviderImpl";

    // ==================== SINGLETON MANAGEMENT ====================

    private static volatile ServiceProviderImpl INSTANCE;

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private QDueDatabase mDatabase;
    private CalendarDatabase mCalendarDatabase;

    // ==================== CORE SERVICES ====================

    private volatile CalendarService mCalendarService;
    private volatile EventsService mEventsService;
    private volatile UserService mUserService;
    private volatile OrganizationService mOrganizationService;
    private volatile CoreBackupManager mCoreBackupManager;

    // ==================== CALENDAR DOMAIN SERVICES ====================

    private volatile CalendarServiceProvider mCalendarServiceProvider;

    // ==================== STATE TRACKING ====================

    private volatile boolean mServicesInitialized = false;
    private volatile boolean mServicesShutdown = false;

    // ==================== SYNCHRONIZATION LOCKS ====================

    private final Object mInitializationLock = new Object();
    private final Object mCalendarServiceLock = new Object();
    private final Object mCalendarServiceProviderLock = new Object();
    private final Object mEventsServiceLock = new Object();
    private final Object mUserServiceLock = new Object();
    private final Object mOrganizationServiceLock = new Object();
    private final Object mBackupManagerLock = new Object();

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor for singleton pattern.
     */
    public ServiceProviderImpl(Context context) {
        mContext = context.getApplicationContext();
        Log.d(TAG, "ServiceProvider created with CalendarServiceProvider integration");
    }

    /**
     * Get singleton instance with thread-safe initialization.
     */
    @NonNull
    public static ServiceProviderImpl getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceProviderImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceProviderImpl(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== SERVICE LIFECYCLE ====================

    @Override
    public void initializeServices() {
        synchronized (mInitializationLock) {
            if (mServicesInitialized || mServicesShutdown) {
                Log.d(TAG, "Services already initialized or shutdown, skipping");
                return;
            }

            try {
                Log.i(TAG, "== Initializing services with CalendarServiceProvider integration...");

                // Initialize database
                mDatabase = QDueDatabase.getInstance(mContext);
                Log.i(TAG, "Database initialized successfully");

                // Initialize calendar database
                mCalendarDatabase = CalendarDatabase.getInstance( mContext );
                Log.i(TAG, "CalendarDatabase initialized successfully");

                // Services will be lazy-initialized when first accessed
                // This ensures proper dependency order
                mServicesInitialized = true;

                Log.i(TAG, "== Services initialization completed");
            } catch (Exception e) {
                mServicesInitialized = false;
                Log.e(TAG, "Error initializing services", e);
            }
        }
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
                // Shutdown services in reverse dependency order

                // 1. CalendarService first (highest level)
                if (mCalendarService != null) {
                    mCalendarService.cleanup();
                    mCalendarService = null;
                    Log.i(TAG, "CalendarService shutdown");
                }

                // 2. CalendarServiceProvider (domain services)
                if (mCalendarServiceProvider != null) {
                    mCalendarServiceProvider.shutdownCalendarServices();
                    mCalendarServiceProvider = null;
                    Log.i(TAG, "CalendarServiceProvider shutdown");
                }

                // 3. Core services
                if (mCoreBackupManager != null) {
                    mCoreBackupManager = null;
                    Log.i(TAG, "CoreBackupManager shutdown");
                }

                if (mOrganizationService != null) {
                    mOrganizationService = null;
                    Log.i(TAG, "OrganizationService shutdown");
                }

                if (mUserService != null) {
                    mUserService = null;
                    Log.i(TAG, "UserService shutdown");
                }

                if (mEventsService != null) {
                    mEventsService = null;
                    Log.i(TAG, "EventsService shutdown");
                }

                mServicesShutdown = true;
                Log.i(TAG, "== Services shutdown completed");

            } catch (Exception e) {
                Log.e(TAG, "Error during service shutdown", e);
            }
        }
    }

    @Override
    public boolean areServicesReady() {
        return mServicesInitialized &&
                !mServicesShutdown &&
                mDatabase != null &&
                mCalendarDatabase != null;
    }

    // ==================== CALENDAR SERVICES ====================

    @Override
    @NonNull
    public CalendarServiceProvider getCalendarServiceProvider() {
        if (mCalendarServiceProvider == null) {
            synchronized (mCalendarServiceProviderLock) {
                if (mCalendarServiceProvider == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating CalendarServiceProvider instance");

                    // CalendarServiceProvider needs this ServiceProvider for infrastructure
                    mCalendarServiceProvider = CalendarServiceProviderImpl.getInstance(
                            //this,
                            mContext,
                            getCalendarDatabase(),
                            getCoreBackupManager());

                    // Initialize calendar services
                    mCalendarServiceProvider.initializeCalendarServices();

                    Log.d(TAG, "CalendarServiceProvider created and initialized");
                }
            }
        }
        return mCalendarServiceProvider;
    }

    @Override
    @NonNull
    public CalendarService getCalendarService() {
        if (mCalendarService == null) {
            synchronized (mCalendarServiceLock) {
                if (mCalendarService == null) {
                    try {
                        Log.d(TAG, "Creating CalendarService instance with CalendarServiceProvider DI");

                        // UPDATED: CalendarService now uses CalendarServiceProvider
                        CalendarServiceProvider calendarServiceProvider = getCalendarServiceProvider();

                        mCalendarService = new CalendarServiceImpl(
                                mContext,
                                calendarServiceProvider  // ← Dependency injection
                        );

                        mCalendarService.initialize();
                        Log.i(TAG, "CalendarService initialized successfully with domain integration");

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to initialize CalendarService", e);
                        throw new RuntimeException("CalendarService initialization failed", e);
                    }
                }
            }
        }
        return mCalendarService;
    }

    @Override
    @NonNull
    public WorkScheduleRepository getWorkScheduleService() {
        // UPDATED: Delegate to CalendarServiceProvider instead of creating directly
        return getCalendarServiceProvider().getWorkScheduleRepository();
    }

    // ==================== CORE SERVICES ====================

    @Override
    @NonNull
    public EventsService getEventsService() {
        if (mEventsService == null) {
            synchronized (mEventsServiceLock) {
                if (mEventsService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating EventsService instance");
                    mEventsService = new EventsServiceImpl(mContext, getDatabase());
                }
            }
        }
        return mEventsService;
    }

    @Override
    @NonNull
    public UserService getUserService() {
        if (mUserService == null) {
            synchronized (mUserServiceLock) {
                if (mUserService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating UserService instance");
                    mUserService = new UserServiceImpl(mContext, getDatabase());
                }
            }
        }
        return mUserService;
    }

    @Override
    @NonNull
    public OrganizationService getOrganizationService() {
        if (mOrganizationService == null) {
            synchronized (mOrganizationServiceLock) {
                if (mOrganizationService == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating OrganizationService instance");
                    mOrganizationService = new OrganizationServiceImpl(mContext, getDatabase());
                }
            }
        }
        return mOrganizationService;
    }

    @Override
    @NonNull
    public CoreBackupManager getCoreBackupManager() {
        if (mCoreBackupManager == null) {
            synchronized (mBackupManagerLock) {
                if (mCoreBackupManager == null) {
                    Log.d(TAG, "Creating CoreBackupManager instance");
                    mCoreBackupManager = new CoreBackupManager(
                            getContext(),
                            getDatabase(),
                            getCalendarDatabase());
                }
            }
        }
        return mCoreBackupManager;
    }

    // ==================== INFRASTRUCTURE ACCESS ====================

    /**
     * Get QDueDatabase instance for CalendarServiceProviderImpl.
     * Package-private access for CalendarServiceProviderImpl integration.
     *
     * @return QDueDatabase instance
     */
    @NonNull
    QDueDatabase getDatabase() {
        ensureInitialized();
        return mDatabase;
    }

    /**
     * Get CalendarDatabase instance for CalendarServiceProviderImpl.
     * Package-private access for CalendarServiceProviderImpl integration.
     *
     * @return CalendarDatabase instance
     */
    @NonNull
    CalendarDatabase getCalendarDatabase() {
        ensureInitialized();
        return mCalendarDatabase;
    }

    /**
     * Get Context for CalendarServiceProviderImpl.
     * Package-private access for CalendarServiceProviderImpl integration.
     *
     * @return Application context
     */
    @NonNull
    Context getContext() {
        return mContext;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Ensure services are initialized before use.
     */
    private void ensureInitialized() {
        if (!mServicesInitialized) {
            synchronized (mInitializationLock) {
                if (!mServicesInitialized) {
                    // Initialize database if not already done
                    if (mDatabase == null) {
                        mDatabase = QDueDatabase.getInstance(mContext);
                    }
                    if (mCalendarDatabase == null) {
                        mCalendarDatabase = CalendarDatabase.getInstance( mContext );
                    }
                    if (mCoreBackupManager == null) {
                        //
                    }
                    mServicesInitialized = true;
                }
            }
        }

        if (mServicesShutdown) {
            throw new IllegalStateException("Services have been shutdown and cannot be used");
        }
    }

    // ==================== DEBUGGING AND MONITORING ====================

    /**
     * Get comprehensive service health status for debugging.
     *
     * @return ServiceHealthStatus with detailed service information
     */
    @NonNull
    public ServiceHealthStatus getServiceHealthStatus() {
        CalendarServiceProvider.CalendarServiceStatus calendarStatus = null;
        if (mCalendarServiceProvider != null) {
            calendarStatus = mCalendarServiceProvider.getCalendarServiceStatus();
        }

        return new ServiceHealthStatus(
                mServicesInitialized,
                mServicesShutdown,
                mDatabase != null,
                mEventsService != null,
                mUserService != null,
                mOrganizationService != null,
                mCoreBackupManager != null,
                mCalendarService != null,
                mCalendarServiceProvider != null,
                calendarStatus
        );
    }

    /**
     * ServiceHealthStatus - Comprehensive service health information.
     */
    public record ServiceHealthStatus(
            boolean initialized,
            boolean shutdown,
            boolean databaseReady,
            boolean eventsServiceCreated,
            boolean userServiceCreated,
            boolean organizationServiceCreated,
            boolean backupManagerCreated,
            boolean calendarServiceCreated,
            boolean calendarServiceProviderCreated,
            CalendarServiceProvider.CalendarServiceStatus calendarServiceStatus) {

        /**
         * Check if all services are in healthy state.
         *
         * @return true if services are healthy and ready
         */
        public boolean isHealthy() {
            return initialized && !shutdown && databaseReady &&
                    (calendarServiceStatus == null || calendarServiceStatus.servicesReady());
        }

        /**
         * Get detailed status summary.
         *
         * @return Human-readable status summary
         */
        @NonNull
        public String getDetailedSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("ServiceProvider Health Summary:\n");
            summary.append("├── Core: ").append(isCorpReady() ? "✅ Ready" : "❌ Not Ready").append("\n");
            summary.append("├── Services: ").append(areServicesCreated() ? "✅ Created" : "❌ Missing").append("\n");
            summary.append("└── Calendar Domain: ");

            if (calendarServiceProviderCreated && calendarServiceStatus != null) {
                summary.append(calendarServiceStatus.servicesReady() ? "✅ Ready" : "❌ Not Ready");
                summary.append("\n    ").append(calendarServiceStatus.toString());
            } else {
                summary.append("❌ Not Initialized");
            }

            return summary.toString();
        }

        private boolean isCorpReady() {
            return initialized && !shutdown && databaseReady;
        }

        private boolean areServicesCreated() {
            return eventsServiceCreated && userServiceCreated &&
                    organizationServiceCreated && backupManagerCreated;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(
                    "ServiceHealth{core=%s, services=%s, calendar=%s, calendarProvider=%s}",
                    isCorpReady() ? "OK" : "FAIL",
                    areServicesCreated() ? "OK" : "PARTIAL",
                    calendarServiceCreated ? "OK" : "NULL",
                    calendarServiceProviderCreated ? "OK" : "NULL"
            );
        }
    }

    /**
     * Reset singleton instance (for testing purposes only).
     */
    public static void resetInstanceForTesting() {
        synchronized (ServiceProviderImpl.class) {
            if (INSTANCE != null) {
                try {
                    INSTANCE.shutdownServices();
                } catch (Exception e) {
                    Log.w(TAG, "Error during test reset", e);
                }
                INSTANCE = null;
            }
        }
    }
}