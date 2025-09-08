package net.calvuz.qdue.core.di.impl;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.common.i18n.impl.DomainLocalizerImpl;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.QDueUserService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.OrganizationService;
import net.calvuz.qdue.core.services.impl.CalendarServiceImpl;
import net.calvuz.qdue.core.services.impl.EventsServiceImpl;
import net.calvuz.qdue.core.services.impl.QDueUserServiceImpl;
import net.calvuz.qdue.core.services.impl.UserServiceImpl;
import net.calvuz.qdue.core.services.impl.OrganizationServiceImpl;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.di.CalendarServiceProviderImpl;
import net.calvuz.qdue.data.repositories.QDueUserRepositoryImpl;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.domain.qdueuser.usecases.QDueUserUseCases;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;

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
 *   └── CalendarService (uses CalendarServiceProvider)
 *      └── CalendarServiceProvider
 *      |   ├── Domain Repositories
 *      |   ├── Domain Engines
 *      |   └── Use Cases
 *      └── QDueUserService
 *          └── Use Cases
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

    private volatile QDueUserService mQDueUserService;
    private volatile LocaleManager mLocaleManager;
    private volatile DomainLocalizer mDomainLocalizer;
    private volatile CalendarService mCalendarService;
    private volatile EventsService mEventsService;
    private volatile UserService mUserService;
    private volatile OrganizationService mOrganizationService;
    private volatile CoreBackupManager mCoreBackupManager;

    // ==================== STATE TRACKING ====================

    private volatile boolean mServicesInitialized = false;
    private volatile boolean mServicesShutdown = false;

    // ==================== SYNCHRONIZATION LOCKS ====================

    private final Object mInitializationLock = new Object();
    private final Object mQDueUserServiceLock = new Object();
    private final Object mLocaleManagerLock = new Object();
    private final Object mDomainLocalizerLock = new Object();
    private final Object mCalendarServiceLock = new Object();
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
        Log.i( TAG, "ServiceProvider created" );
    }

    /**
     * Get singleton instance with thread-safe initialization.
     */
    @NonNull
    public static ServiceProviderImpl getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceProviderImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceProviderImpl( context );
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
                Log.d( TAG, "Services already initialized or shutdown, skipping" );
                return;
            }

            try {
                Log.d( TAG, "Initializing services" );

                // Initialize database
                mDatabase = QDueDatabase.getInstance( mContext );
                Log.d( TAG, "Database initialized successfully" );

                // Initialize calendar database
                mCalendarDatabase = CalendarDatabase.getInstance( mContext );
                Log.d( TAG, "CalendarDatabase initialized successfully" );

                // Services will be lazy-initialized when first accessed
                // This ensures proper dependency order
                mServicesInitialized = true;
            } catch (Exception e) {
                mServicesInitialized = false;
                Log.e( TAG, "Error initializing services", e );
            }
        }
    }

    @Override
    public void shutdownServices() {
        synchronized (mInitializationLock) {
            if (mServicesShutdown) {
                Log.d( TAG, "Services already shutdown" );
                return;
            }

            Log.d( TAG, "Shutting down services..." );

            try {
                // Shutdown services in reverse dependency order

                if (mCalendarService != null) {
                    mCalendarService.cleanup();
                    mCalendarService = null;
                    Log.i( TAG, "CalendarService shutdown" );
                }

                if (mCoreBackupManager != null) {
                    mCoreBackupManager = null;
                    Log.i( TAG, "CoreBackupManager shutdown" );
                }

                if (mOrganizationService != null) {
                    mOrganizationService = null;
                    Log.i( TAG, "OrganizationService shutdown" );
                }

                if (mUserService != null) {
                    mUserService = null;
                    Log.i( TAG, "UserService shutdown" );
                }

                if (mEventsService != null) {
                    mEventsService = null;
                    Log.i( TAG, "EventsService shutdown" );
                }

                // QDueUser last service (lowest level)
                if (mQDueUserService != null) {
                    mQDueUserService = null;
                    Log.i( TAG, "QDueUserService shutdown" );
                }

                if (mLocaleManager != null) {
                    mLocaleManager = null;
                    Log.i( TAG, "LocaleManager shutdown" );
                }

                if (mDomainLocalizer != null) {
                    mDomainLocalizer = null;
                    Log.i( TAG, "DomainLocalizer shutdown" );
                }

                // DATABASES LAST
                if (mDatabase != null) {
                    mDatabase = null;
                    Log.i( TAG, "Database shutdown" );
                }
                if (mCalendarDatabase != null) {
                    mCalendarDatabase = null;
                    Log.i( TAG, "CalendarDatabase shutdown" );
                }

                mServicesShutdown = true;
                Log.i( TAG, "== Services shutdown completed" );
            } catch (Exception e) {
                Log.e( TAG, "Error during service shutdown", e );
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

    // ==================== INFRASTRUCTURE HELPERS ====================

    @NonNull
    private LocaleManager getLocaleManager() {
        if (mLocaleManager == null) {
            synchronized (mLocaleManagerLock) {
                if (mLocaleManager == null) {
                    Log.d( TAG, "Creating LocaleManager instance" );
                    mLocaleManager = new LocaleManager( mContext );
                }
            }
        }
        return mLocaleManager;
    }

    @NonNull
    private DomainLocalizer getDomainLocalizer() {
        if (mDomainLocalizer == null) {
            synchronized (mDomainLocalizerLock) {
                if (mDomainLocalizer == null) {
                    Log.d( TAG, "Creating DomainLocalizer instance" );
                    mDomainLocalizer = new DomainLocalizerImpl( mContext, getLocaleManager() );
                }
            }
        }
        return mDomainLocalizer;
    }



    // ==================== CALENDAR SERVICES ====================

    @Override
    @NonNull
    public CalendarService getCalendarService() {
        if (mCalendarService == null) {
            synchronized (mCalendarServiceLock) {
                if (mCalendarService == null) {
                    try {
                        ensureInitialized();
                        Log.d( TAG, "Creating CalendarService instance (with CalendarServiceProvider Injected)" );

                        // CalendarService needs CalendarServiceProvider
                        CalendarServiceProvider calendarServiceProvider = CalendarServiceProviderImpl.getInstance(
                                mContext,
                                getCalendarDatabase(),
                                getCoreBackupManager() );

                        mCalendarService = new CalendarServiceImpl(
                                mContext,
                                calendarServiceProvider  // ← Dependency injection
                        );

                        mCalendarService.initialize();
                    } catch (Exception e) {
                        Log.e( TAG, "Failed to initialize CalendarService", e );
                        throw new RuntimeException( "CalendarService initialization failed", e );
                    }
                }
            }
        }
        return mCalendarService;
    }

    @Override
    @NonNull
    public WorkScheduleRepository getWorkScheduleService() {
        // Delegate to CalendarServiceProvider instead of creating directly
        return getCalendarServiceProvider().getWorkScheduleRepository();
    }

    @Override
    @NonNull
    public CalendarServiceProvider getCalendarServiceProvider() {
        // Delegate to CalendarServiceProvider instead of creating directly
        return getCalendarService().getCalendarServiceProvider();
    }

    // ==================== NEW SERVICE PROVIDER METHOD ====================

    @Override
    public QDueUserService getQDueUserService() {
        if (mQDueUserService == null) {
            synchronized (mQDueUserServiceLock) {
                if (mQDueUserService == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating QDueUserService instance" );

                    // Create QDueUserRepository
                    QDueUserRepository qDueUserRepository = new QDueUserRepositoryImpl( getCalendarDatabase() );

                    // Create QDueUserUseCases
                    QDueUserUseCases qDueUserUseCases = new QDueUserUseCases( qDueUserRepository );

                    // Create QDueUserService
                    mQDueUserService = new QDueUserServiceImpl(
                            qDueUserUseCases
                    );
                }
            }
        }
        return mQDueUserService;
    }

    // ==================== CORE SERVICES ====================

    @Override
    @NonNull
    public EventsService getEventsService() {
        if (mEventsService == null) {
            synchronized (mEventsServiceLock) {
                if (mEventsService == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating EventsService instance" );
                    mEventsService = new EventsServiceImpl(
                            mContext,
                            getDatabase(),
                            getCoreBackupManager() );
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
                    Log.d( TAG, "Creating UserService instance" );
                    mUserService = new UserServiceImpl(
                            mContext,
                            getDatabase(),
                            getCoreBackupManager() );
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
                    Log.d( TAG, "Creating OrganizationService instance" );
                    mOrganizationService = new OrganizationServiceImpl(
                            mContext,
                            getDatabase(),
                            getCoreBackupManager() );
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
                    Log.d( TAG, "Creating CoreBackupManager instance" );
                    mCoreBackupManager = new CoreBackupManager(
                            getContext(),
                            getDatabase(),
                            getCalendarDatabase() );
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
     * Ensure services (databases) are initialized before use.
     */
    private void ensureInitialized() {
        if (!mServicesInitialized) {
            synchronized (mInitializationLock) {
                if (!mServicesInitialized) {
                    // Initialize database if not already done
                    if (mDatabase == null) {
                        mDatabase = QDueDatabase.getInstance( mContext );
                    }
                    if (mCalendarDatabase == null) {
                        mCalendarDatabase = CalendarDatabase.getInstance( mContext );
                    }
                    mServicesInitialized = true;
                }
            }
        }

        if (mServicesShutdown) {
            throw new IllegalStateException( "Services (databases) have been shutdown and cannot be used" );
        }
    }

    // ==================== DEBUGGING AND MONITORING ====================

    /**
     * Reset singleton instance (for testing purposes only).
     */
    public static void resetInstanceForTesting() {
        synchronized (ServiceProviderImpl.class) {
            if (INSTANCE != null) {
                try {
                    INSTANCE.shutdownServices();
                } catch (Exception e) {
                    Log.w( TAG, "Error during test reset", e );
                }
                INSTANCE = null;
            }
        }
    }

    /**
     * Get comprehensive service health status for debugging.
     *
     * @return ServiceHealthStatus with detailed service information
     */
    @NonNull
    public ServiceHealthStatus getServiceHealthStatus() {
        mCalendarService.getCalendarServiceProvider();
        return new ServiceHealthStatus(
                mServicesInitialized,
                mServicesShutdown,
                mDatabase != null,
                mEventsService != null,
                mUserService != null,
                mOrganizationService != null,
                mCoreBackupManager != null,
                mCalendarService != null,
                mQDueUserService != null
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
            boolean qdueUserServiceCreated
    ) {

        /**
         * Check if all services are in healthy state.
         *
         * @return true if services are healthy and ready
         */
        public boolean isHealthy() {
            return initialized && !shutdown && databaseReady &&
                    eventsServiceCreated && userServiceCreated &&
                    organizationServiceCreated && backupManagerCreated &&
                    calendarServiceCreated && qdueUserServiceCreated;
        }

        /**
         * Get detailed status summary.
         *
         * @return Human-readable status summary
         */
        @NonNull
        public String getDetailedSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append( "ServiceProvider Health Summary:\n" );
            summary.append( "├── Core: " ).append( isCorpReady() ? "✅ Ready" : "❌ Not Ready" ).append( "\n" );
            summary.append( "└── Services: " ).append( areServicesCreated() ? "✅ Created" : "❌ Missing" ).append( "\n" );

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
            return MessageFormat.format(
                    "ServiceHealth(core={0}, services={1})",
                    isCorpReady() ? "OK" : "FAIL",
                    areServicesCreated() ? "OK" : "PARTIAL"
            );
        }
    }
}