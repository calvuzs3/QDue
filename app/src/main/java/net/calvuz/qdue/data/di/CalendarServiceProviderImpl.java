package net.calvuz.qdue.data.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.data.repositories.RecurrenceRuleRepositoryImpl;
import net.calvuz.qdue.data.repositories.ShiftExceptionRepositoryImpl;
import net.calvuz.qdue.data.repositories.ShiftRepositoryImpl;
import net.calvuz.qdue.data.repositories.TeamRepositoryImpl;
import net.calvuz.qdue.data.repositories.UserScheduleAssignmentRepositoryImpl;
import net.calvuz.qdue.data.repositories.WorkScheduleRepositoryImpl;
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
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateTeamScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GetScheduleStatsUseCase;
import net.calvuz.qdue.domain.calendar.usecases.UseCaseFactory;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.core.common.i18n.impl.DomainLocalizerImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * CalendarServiceProviderImpl - Updated with ServiceProvider Integration
 *
 * <p>Complete implementation of CalendarServiceProvider that integrates with core ServiceProvider
 * for infrastructure dependencies. Provides comprehensive dependency injection for calendar domain
 * services while leveraging core infrastructure from ServiceProvider.</p>
 *
 * <h3>Integration Strategy:</h3>
 * <ul>
 *   <li><strong>Infrastructure Delegation</strong>: Database, Context, BackupManager from ServiceProvider</li>
 *   <li><strong>Domain Ownership</strong>: Full ownership of calendar domain components</li>
 *   <li><strong>Clean Separation</strong>: Domain services isolated from core services</li>
 *   <li><strong>Dependency Bridge</strong>: Bridges core infrastructure with domain needs</li>
 * </ul>
 *
 * <h3>Dependency Flow:</h3>
 * <pre>
 * ServiceProvider
 *   ├── Infrastructure (DB, Context, Backup)
 *   └── CalendarServiceProvider (this)
 *       ├── Domain Repositories
 *       ├── Domain Engines
 *       ├── Use Cases
 *       └── WorkScheduleRepository
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.0.0 - ServiceProvider Integration
 * @since Clean Architecture Phase 3
 */
public class CalendarServiceProviderImpl implements CalendarServiceProvider {

    private static final String TAG = "CalendarServiceProviderImpl";

    // ==================== SINGLETON MANAGEMENT ====================

    private static volatile CalendarServiceProviderImpl INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Get singleton instance with ServiceProvider integration.
     *
     * //@param coreServiceProvider Core ServiceProvider for infrastructure dependencies
     * @return CalendarServiceProviderImpl singleton instance
     */
    @NonNull
    public static CalendarServiceProviderImpl getInstance(//@NonNull ServiceProvider coreServiceProvider,
                                                          @NonNull Context context,
                                                          @NonNull CalendarDatabase database,
                                                          @NonNull CoreBackupManager backupManager) {
        if (INSTANCE == null) {
            synchronized (INSTANCE_LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new CalendarServiceProviderImpl(//coreServiceProvider,
                            context,
                            database,
                            backupManager
                    );
                }
            }
        }
        return INSTANCE;
    }

    // ==================== CORE DEPENDENCIES ====================

    //private final ServiceProvider mCoreServiceProvider;
    private final Context mContext;
    private final CalendarDatabase mDatabase;
    private final CoreBackupManager mCoreBackupManager;

    // ==================== REPOSITORY INSTANCES ====================

    private volatile RecurrenceRuleRepository mRecurrenceRuleRepository;
    private volatile ShiftExceptionRepository mShiftExceptionRepository;
    private volatile TeamRepository mTeamRepository;
    private volatile ShiftRepository mShiftRepository;
    private volatile UserScheduleAssignmentRepository mUserScheduleAssignmentRepository;
    private volatile WorkScheduleRepository mWorkScheduleRepository;

    // ==================== DOMAIN ENGINE INSTANCES ====================

    private volatile RecurrenceCalculator mRecurrenceCalculator;
    private volatile ExceptionResolver mExceptionResolver;
    private volatile SchedulingEngine mSchedulingEngine;

    // ==================== USE CASE INSTANCES ====================

    private volatile CreatePatternAssignmentUseCase mCreatePatternAssignmentUseCase;
    private volatile GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;
    private volatile GenerateTeamScheduleUseCase mGenerateTeamScheduleUseCase;
    private volatile ApplyShiftExceptionsUseCase mApplyShiftExceptionsUseCase;
    private volatile GetScheduleStatsUseCase mGetScheduleStatsUseCase;
    private volatile UseCaseFactory mUseCaseFactory;

    // ==================== INFRASTRUCTURE INSTANCES ====================

    private volatile DomainLocalizer mDomainLocalizer;
    private volatile LocaleManager mLocaleManager;

    // ==================== SYNCHRONIZATION LOCKS ====================

    private final Object mInitializationLock = new Object();
    private final Object mRecurrenceRepositoryLock = new Object();
    private final Object mShiftExceptionRepositoryLock = new Object();
    private final Object mTeamRepositoryLock = new Object();
    private final Object mShiftRepositoryLock = new Object();
    private final Object mUserAssignmentRepositoryLock = new Object();
    private final Object mWorkScheduleRepositoryLock = new Object();
    private final Object mRecurrenceCalculatorLock = new Object();
    private final Object mExceptionResolverLock = new Object();
    private final Object mSchedulingEngineLock = new Object();
    private final Object mUserScheduleUseCaseLock = new Object();
    private final Object mTeamScheduleUseCaseLock = new Object();
    private final Object mShiftExceptionsUseCaseLock = new Object();
    private final Object mScheduleStatsUseCaseLock = new Object();
    private final Object mUseCaseFactoryLock = new Object();

    // ==================== STATE TRACKING ====================

    private volatile boolean mCalendarServicesInitialized = false;
    private volatile boolean mCalendarServicesShutdown = false;
    private volatile long mInitializationTime = 0;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor for singleton pattern with ServiceProvider integration.
     *
     * //@param coreServiceProvider Core ServiceProvider for infrastructure dependencies
     */
    private CalendarServiceProviderImpl(//@NonNull ServiceProvider coreServiceProvider,
                                        @NonNull Context context,
                                        @NonNull CalendarDatabase database,
                                        @NonNull CoreBackupManager backupManager) {
        //this.mCoreServiceProvider = coreServiceProvider;
        this.mContext = context;
        this.mDatabase = database;
        this.mCoreBackupManager = backupManager;
        //this.mCoreBackupManager = coreServiceProvider.getCoreBackupManager();
        Log.i(TAG, "CalendarServiceProviderImpl created with ServiceProvider integration");
    }

    // ==================== INFRASTRUCTURE HELPERS ====================

    /**
     * Get Context through ServiceProvider integration.
     */
    @NonNull
    private Context getContext() {
        return mContext;
        // Access package-private method from ServiceProviderImpl
//        if (mCoreServiceProvider instanceof ServiceProviderImpl) {
//            return ((ServiceProviderImpl) mCoreServiceProvider).getContext();
//        }
//        throw new IllegalStateException("ServiceProvider must be ServiceProviderImpl for infrastructure access");
    }

    /**
     * Get CalendarDatabase through ServiceProvider integration.
     */
    @NonNull
    private CalendarDatabase getDatabase() {
        return mDatabase;
        // Access package-private method from ServiceProviderImpl
//        if (mCoreServiceProvider instanceof ServiceProviderImpl) {
//            return ((ServiceProviderImpl) mCoreServiceProvider).getDatabase();
//        }
//        throw new IllegalStateException("ServiceProvider must be ServiceProviderImpl for database access");
    }

    /**
     * Get LocaleManager through ServiceProvider.
     */
    @NonNull
    private LocaleManager getLocaleManager() {
        if (mLocaleManager == null) {
            synchronized (this) {
                if (mLocaleManager == null) {
                    mLocaleManager = new LocaleManager(getContext());
                }
            }
        }
        return mLocaleManager;
    }

    /**
     * Get DomainLocalizer through LocaleManager bridge.
     */
    @NonNull
    private DomainLocalizer getDomainLocalizer() {
        if (mDomainLocalizer == null) {
            synchronized (this) {
                if (mDomainLocalizer == null) {
                    mDomainLocalizer = new DomainLocalizerImpl(getContext(), getLocaleManager());
                }
            }
        }
        return mDomainLocalizer;
    }

    /**
     * Get CoreBackupManager through ServiceProvider.
     */
    @NonNull
    private CoreBackupManager getCoreBackupManager() {
        return mCoreBackupManager;
//        return mCoreServiceProvider.getCoreBackupManager();
    }

    // ==================== DOMAIN REPOSITORIES ====================

    @Override
    @NonNull
    public RecurrenceRuleRepository getRecurrenceRuleRepository() {
        if (mRecurrenceRuleRepository == null) {
            synchronized (mRecurrenceRepositoryLock) {
                if (mRecurrenceRuleRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating RecurrenceRuleRepository instance");
                    mRecurrenceRuleRepository = new RecurrenceRuleRepositoryImpl(
                            getContext(),
                            getDatabase(),
                            getCoreBackupManager()
                            //getLocaleManager()
                    );
                }
            }
        }
        return mRecurrenceRuleRepository;
    }

    @Override
    @NonNull
    public ShiftExceptionRepository getShiftExceptionRepository() {
        if (mShiftExceptionRepository == null) {
            synchronized (mShiftExceptionRepositoryLock) {
                if (mShiftExceptionRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating ShiftExceptionRepository instance with i18n workflow support");
                    mShiftExceptionRepository = new ShiftExceptionRepositoryImpl(
                            getDatabase()
                            //getDomainLocalizer()
                    );
                }
            }
        }
        return mShiftExceptionRepository;
    }

    @Override
    @NonNull
    public TeamRepository getTeamRepository() {
        if (mTeamRepository == null) {
            synchronized (mTeamRepositoryLock) {
                if (mTeamRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating TeamRepository instance with i18n team messaging");
                    mTeamRepository = new TeamRepositoryImpl(
                            getContext(),
                            getDatabase(),
                            getCoreBackupManager()
                            //getLocaleManager(),
                            //getDomainLocalizer()
                    );
                }
            }
        }
        return mTeamRepository;
    }

    @Override
    @NonNull
    public ShiftRepository getShiftRepository() {
        if (mShiftRepository == null) {
            synchronized (mShiftRepositoryLock) {
                if (mShiftRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating ShiftRepository instance with i18n shift descriptions");
                    mShiftRepository = new ShiftRepositoryImpl(
                            getContext(),
                            getDatabase(),
                            getCoreBackupManager()
                            //getLocaleManager(),
                            //getDomainLocalizer()
                    );
                }
            }
        }
        return mShiftRepository;
    }

    @Override
    @NonNull
    public UserScheduleAssignmentRepository getUserScheduleAssignmentRepository() {
        if (mUserScheduleAssignmentRepository == null) {
            synchronized (mUserAssignmentRepositoryLock) {
                if (mUserScheduleAssignmentRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating UserScheduleAssignmentRepository instance with i18n assignment messaging");
                    mUserScheduleAssignmentRepository = new UserScheduleAssignmentRepositoryImpl(
                            getDatabase()
                            //getDomainLocalizer()
                    );
                }
            }
        }
        return mUserScheduleAssignmentRepository;
    }

    @Override
    @NonNull
    public WorkScheduleRepository getWorkScheduleRepository() {
        if (mWorkScheduleRepository == null) {
            synchronized (mWorkScheduleRepositoryLock) {
                if (mWorkScheduleRepository == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating WorkScheduleRepository instance with CalendarServiceProvider DI");

                    // WorkScheduleRepository uses this CalendarServiceProvider for all dependencies
                    mWorkScheduleRepository = new WorkScheduleRepositoryImpl(
                            getContext(),
                            this // Pass this CalendarServiceProvider
                    );
                }
            }
        }
        return mWorkScheduleRepository;
    }

    // ==================== DOMAIN ENGINES ====================

    @Override
    @NonNull
    public RecurrenceCalculator getRecurrenceCalculator() {
        if (mRecurrenceCalculator == null) {
            synchronized (mRecurrenceCalculatorLock) {
                if (mRecurrenceCalculator == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating RecurrenceCalculator instance with localization support");
                    mRecurrenceCalculator = new RecurrenceCalculator(getDomainLocalizer());
                }
            }
        }
        return mRecurrenceCalculator;
    }

    @Override
    @NonNull
    public ExceptionResolver getExceptionResolver() {
        if (mExceptionResolver == null) {
            synchronized (mExceptionResolverLock) {
                if (mExceptionResolver == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating ExceptionResolver instance with localization support");
                    mExceptionResolver = new ExceptionResolver(getDomainLocalizer());
                }
            }
        }
        return mExceptionResolver;
    }

    @Override
    @NonNull
    public SchedulingEngine getSchedulingEngine() {
        if (mSchedulingEngine == null) {
            synchronized (mSchedulingEngineLock) {
                if (mSchedulingEngine == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating SchedulingEngine instance with coordinated domain engines");
                    mSchedulingEngine = new SchedulingEngine(
                            getRecurrenceCalculator(),
                            getExceptionResolver(),
                            getDomainLocalizer()
                    );
                }
            }
        }
        return mSchedulingEngine;
    }

    // ==================== USE CASES ====================

    @NonNull
    @Override
    public CreatePatternAssignmentUseCase getCreatePatternAssignmentUseCase() {
        if (mCreatePatternAssignmentUseCase == null) {
            mCreatePatternAssignmentUseCase = new CreatePatternAssignmentUseCase(
                    getTeamRepository(),
                    getRecurrenceRuleRepository(),
                    getUserScheduleAssignmentRepository()
            );
        }
        return mCreatePatternAssignmentUseCase;
    }

    @Override
    @NonNull
    public GenerateUserScheduleUseCase getGenerateUserScheduleUseCase() {
        if (mGenerateUserScheduleUseCase == null) {
            synchronized (mUserScheduleUseCaseLock) {
                if (mGenerateUserScheduleUseCase == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating GenerateUserScheduleUseCase instance");
                    mGenerateUserScheduleUseCase = new GenerateUserScheduleUseCase(
                            getWorkScheduleRepository()
                    );
                }
            }
        }
        return mGenerateUserScheduleUseCase;
    }

    @Override
    @NonNull
    public GenerateTeamScheduleUseCase getGenerateTeamScheduleUseCase() {
        if (mGenerateTeamScheduleUseCase == null) {
            synchronized (mTeamScheduleUseCaseLock) {
                if (mGenerateTeamScheduleUseCase == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating GenerateTeamScheduleUseCase instance");
                    mGenerateTeamScheduleUseCase = new GenerateTeamScheduleUseCase(
                            getWorkScheduleRepository()
                    );
                }
            }
        }
        return mGenerateTeamScheduleUseCase;
    }

    @Override
    @NonNull
    public ApplyShiftExceptionsUseCase getApplyShiftExceptionsUseCase() {
        if (mApplyShiftExceptionsUseCase == null) {
            synchronized (mShiftExceptionsUseCaseLock) {
                if (mApplyShiftExceptionsUseCase == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating ApplyShiftExceptionsUseCase instance");
                    mApplyShiftExceptionsUseCase = new ApplyShiftExceptionsUseCase(
                            getWorkScheduleRepository()
                    );
                }
            }
        }
        return mApplyShiftExceptionsUseCase;
    }

    @Override
    @NonNull
    public GetScheduleStatsUseCase getGetScheduleStatsUseCase() {
        if (mGetScheduleStatsUseCase == null) {
            synchronized (mScheduleStatsUseCaseLock) {
                if (mGetScheduleStatsUseCase == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating GetScheduleStatsUseCase instance");
                    mGetScheduleStatsUseCase = new GetScheduleStatsUseCase(
                            getWorkScheduleRepository()
                    );
                }
            }
        }
        return mGetScheduleStatsUseCase;
    }

    @Override
    @NonNull
    public UseCaseFactory getUseCaseFactory() {
        if (mUseCaseFactory == null) {
            synchronized (mUseCaseFactoryLock) {
                if (mUseCaseFactory == null) {
                    ensureInitialized();
                    Log.d(TAG, "Creating UseCaseFactory instance with all use cases");
                    mUseCaseFactory = new UseCaseFactory(getWorkScheduleRepository());
                }
            }
        }
        return mUseCaseFactory;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    public void initializeCalendarServices() {
        synchronized (mInitializationLock) {
            if (mCalendarServicesInitialized) {
                Log.d(TAG, "Calendar services already initialized");
                return;
            }

            Log.i(TAG, "Initializing calendar services...");
            long startTime = System.currentTimeMillis();

            try {
                // Verify infrastructure is available
                if (mDatabase == null) {
                    throw new RuntimeException("Database not available in CalendarServiceProvider");
                }

                if (mContext == null) {
                    throw new RuntimeException("Context not available in CalendarServiceProvider");
                }

                if (mCoreBackupManager == null) {
                    throw new RuntimeException("CoreBackupManager not available in CalendarServiceProvider");
                }

                // Mark as initialized
                mCalendarServicesInitialized = true;
                mInitializationTime = System.currentTimeMillis() - startTime;

                Log.i(TAG, "Calendar services initialized successfully in " + mInitializationTime + "ms");

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize calendar services", e);
                mCalendarServicesInitialized = false;
                throw new RuntimeException("Calendar service initialization failed", e);
            }
        }
    }

    @Override
    public boolean areCalendarServicesReady() {
        return mCalendarServicesInitialized && !mCalendarServicesShutdown &&
                mDatabase != null && mContext != null && mCoreBackupManager != null;
    }

    @Override
    public void shutdownCalendarServices() {
        synchronized (mInitializationLock) {
            if (mCalendarServicesShutdown) {
                Log.d(TAG, "Calendar services already shutdown");
                return;
            }

            Log.i(TAG, "Shutting down calendar services...");

            try {
                // Shutdown use cases
                mUseCaseFactory = null;
                mGetScheduleStatsUseCase = null;
                mApplyShiftExceptionsUseCase = null;
                mGenerateTeamScheduleUseCase = null;
                mGenerateUserScheduleUseCase = null;

                // Shutdown domain engines
                mSchedulingEngine = null;
                mExceptionResolver = null;
                mRecurrenceCalculator = null;

                // Shutdown repositories
                if (mWorkScheduleRepository != null && mWorkScheduleRepository instanceof WorkScheduleRepositoryImpl) {
                    ((WorkScheduleRepositoryImpl) mWorkScheduleRepository).cleanup();
                }
                mWorkScheduleRepository = null;
                mUserScheduleAssignmentRepository = null;
                mShiftRepository = null;
                mTeamRepository = null;
                mShiftExceptionRepository = null;
                mRecurrenceRuleRepository = null;

                // Shutdown infrastructure
                mDomainLocalizer = null;
                mLocaleManager = null;

                mCalendarServicesShutdown = true;
                Log.i(TAG, "Calendar services shutdown completed");

            } catch (Exception e) {
                Log.e(TAG, "Error during calendar service shutdown", e);
            }
        }
    }

    @Override
    @NonNull
    public CalendarServiceStatus getCalendarServiceStatus() {
        return new CalendarServiceStatus(
                areCalendarServicesReady(),
                countActiveRepositories() > 0,
                countActiveEngines() > 0,
                countActiveUseCases() > 0,
                mInitializationTime,
                countActiveRepositories(),
                countActiveEngines(),
                countActiveUseCases(),
                mCalendarServicesInitialized ? "Calendar services ready with ServiceProvider integration" :
                        "Calendar services not initialized"
        );
    }

    // ==================== HELPER METHODS ====================

    /**
     * Ensure calendar services are initialized.
     */
    private void ensureInitialized() {
        if (!mCalendarServicesInitialized) {
            initializeCalendarServices();
        }
    }

    /**
     * Count active repositories for status reporting.
     */
    private int countActiveRepositories() {
        int count = 0;
        if (mRecurrenceRuleRepository != null) count++;
        if (mShiftExceptionRepository != null) count++;
        if (mTeamRepository != null) count++;
        if (mShiftRepository != null) count++;
        if (mUserScheduleAssignmentRepository != null) count++;
        if (mWorkScheduleRepository != null) count++;
        return count;
    }

    /**
     * Count active engines for status reporting.
     */
    private int countActiveEngines() {
        int count = 0;
        if (mRecurrenceCalculator != null) count++;
        if (mExceptionResolver != null) count++;
        if (mSchedulingEngine != null) count++;
        return count;
    }

    /**
     * Count active use cases for status reporting.
     */
    private int countActiveUseCases() {
        int count = 0;
        if (mGenerateUserScheduleUseCase != null) count++;
        if (mGenerateTeamScheduleUseCase != null) count++;
        if (mApplyShiftExceptionsUseCase != null) count++;
        if (mGetScheduleStatsUseCase != null) count++;
        if (mUseCaseFactory != null) count++;
        return count;
    }

    /**
     * Reset singleton instance (for testing purposes).
     */
    public static void resetInstance() {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE != null) {
                INSTANCE.shutdownCalendarServices();
                INSTANCE = null;
            }
        }
    }
}