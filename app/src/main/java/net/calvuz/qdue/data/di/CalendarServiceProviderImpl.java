package net.calvuz.qdue.data.di;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.data.dao.UserTeamAssignmentDao;
import net.calvuz.qdue.data.repositories.LocalEventsRepositoryImpl;
import net.calvuz.qdue.data.repositories.QDueUserRepositoryImpl;
import net.calvuz.qdue.data.repositories.RecurrenceRuleRepositoryImpl;
import net.calvuz.qdue.data.repositories.ShiftExceptionRepositoryImpl;
import net.calvuz.qdue.data.repositories.ShiftRepositoryImpl;
import net.calvuz.qdue.data.repositories.TeamRepositoryImpl;
import net.calvuz.qdue.data.repositories.UserScheduleAssignmentRepositoryImpl;
import net.calvuz.qdue.data.repositories.UserTeamAssignmentRepositoryImpl;
import net.calvuz.qdue.data.repositories.WorkScheduleRepositoryImpl;
import net.calvuz.qdue.data.services.LocalEventsFileService;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.services.UserSchedulePatternService;
import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.data.services.impl.LocalEventsFileServiceImpl;
import net.calvuz.qdue.data.services.impl.LocalEventsServiceImpl;
import net.calvuz.qdue.data.services.impl.QDueUserServiceImpl;
import net.calvuz.qdue.data.services.impl.UserSchedulePatternServiceImpl;
import net.calvuz.qdue.data.services.impl.UserWorkScheduleServiceImpl;
import net.calvuz.qdue.domain.calendar.engines.ExceptionResolver;
import net.calvuz.qdue.domain.calendar.engines.RecurrenceCalculator;
import net.calvuz.qdue.domain.calendar.engines.SchedulingEngine;
import net.calvuz.qdue.domain.calendar.repositories.LocalEventsRepository;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftExceptionRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserTeamAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.ApplyShiftExceptionsUseCase;
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateTeamScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.domain.calendar.usecases.GetScheduleStatsUseCase;
import net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases;
import net.calvuz.qdue.domain.calendar.usecases.TeamUseCases;
import net.calvuz.qdue.domain.calendar.usecases.UserTeamAssignmentUseCases;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.core.common.i18n.impl.DomainLocalizerImpl;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.domain.qdueuser.usecases.QDueUserUseCases;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule;

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
 */
public class CalendarServiceProviderImpl implements CalendarServiceProvider
{

    private static final String TAG = "CalendarServiceProviderImpl";

    // ==================== SINGLETON MANAGEMENT ====================

    @SuppressLint ("StaticFieldLeak")
    private static volatile CalendarServiceProviderImpl INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Get singleton instance with ServiceProvider integration.
     * <p>
     * //@param coreServiceProvider Core ServiceProvider for infrastructure dependencies
     *
     * @return CalendarServiceProviderImpl singleton instance
     */
    @NonNull
    public static CalendarServiceProviderImpl getInstance(
            @NonNull Context context,
            @NonNull CalendarDatabase database,
            @NonNull CoreBackupManager backupManager
    ) {
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

    private volatile LocalEventsRepository mLocalEventsRepository;
    private volatile QDueUserRepository mQDueUserRepository;
    private volatile TeamRepository mTeamRepository;
    private volatile ShiftRepository mShiftRepository;
    private volatile UserTeamAssignmentRepository mUserTeamAssignmentRepository;
    private volatile RecurrenceRuleRepository mRecurrenceRuleRepository;
    private volatile ShiftExceptionRepository mShiftExceptionRepository;
    private volatile UserScheduleAssignmentRepository mUserScheduleAssignmentRepository;
    private volatile WorkScheduleRepository mWorkScheduleRepository;

    // ==================== DOMAIN ENGINE INSTANCES ====================

    private volatile RecurrenceCalculator mRecurrenceCalculator;
    private volatile ExceptionResolver mExceptionResolver;
    private volatile SchedulingEngine mSchedulingEngine;

    // ==================== MODULE INSTANCES ====================

    private volatile LocalEventsModule mLocalEventsModule;

    // ==================== SERVICES INSTANCES ====================

    private volatile QDueUserService mQDueUserService;
    private volatile LocalEventsService mLocalEventsService;
    private volatile LocalEventsFileService mLocalEventsFileService;
    private volatile UserWorkScheduleService mUserWorkScheduleService;
    private volatile UserSchedulePatternService mUserSchedulePatternService;

    // ==================== USE CASE INSTANCES ====================

    private volatile LocalEventsUseCases mLocalEventsUseCases;
    private volatile TeamUseCases mTeamUseCases;
    private volatile UserTeamAssignmentUseCases mUserTeamAssignmentUseCases;
    private volatile CreatePatternAssignmentUseCase mCreatePatternAssignmentUseCase;
    private volatile GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;
    private volatile GenerateTeamScheduleUseCase mGenerateTeamScheduleUseCase;
    private volatile ApplyShiftExceptionsUseCase mApplyShiftExceptionsUseCase;
    private volatile GetScheduleStatsUseCase mGetScheduleStatsUseCase;

    // ==================== INFRASTRUCTURE INSTANCES ====================

    private volatile DomainLocalizer mDomainLocalizer;
    private volatile LocaleManager mLocaleManager;

    // ==================== SYNCHRONIZATION LOCKS ====================

    private final Object mInitializationLock = new Object();
    private final Object mLocalEventsRepositoryLock = new Object();
    private final Object mQDueUserRepositoryLock = new Object();
    private final Object mRecurrenceRepositoryLock = new Object();
    private final Object mShiftExceptionRepositoryLock = new Object();
    private final Object mTeamRepositoryLock = new Object();
    private final Object mShiftRepositoryLock = new Object();
    private final Object mUserAssignmentRepositoryLock = new Object();
    private final Object mUserTeamAssignmentRepositoryLock = new Object();
    private final Object mWorkScheduleRepositoryLock = new Object();
    private final Object mLocalEventsModuleLock = new Object();
    private final Object mQDueUserServiceLock = new Object();
    private final Object mLocalEventsServiceLock = new Object();
    private final Object mLocalEventsFileServiceLock = new Object();
    private final Object mUserWorkScheduleServiceLock = new Object();
    private final Object mUserSchedulePatternServiceLock = new Object();
    private final Object mLocalEventsUseCasesLock = new Object();
    private final Object mTeamUseCasesLock = new Object();
    private final Object mCreatePatternAssignmentUseCaseLock = new Object();
    private final Object mRecurrenceCalculatorLock = new Object();
    private final Object mExceptionResolverLock = new Object();
    private final Object mSchedulingEngineLock = new Object();
    private final Object mUserTeamAssignmentUseCasesLock = new Object();
    private final Object mUserScheduleUseCaseLock = new Object();
    private final Object mTeamScheduleUseCaseLock = new Object();
    private final Object mShiftExceptionsUseCaseLock = new Object();
    private final Object mScheduleStatsUseCaseLock = new Object();

    // ==================== STATE TRACKING ====================

    private volatile boolean mCalendarServicesInitialized = false;
    private volatile boolean mCalendarServicesShutdown = false;
    private volatile long mInitializationTime = 0;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor for singleton pattern with ServiceProvider integration.
     * <p>
     * //@param coreServiceProvider Core ServiceProvider for infrastructure dependencies
     */
    private CalendarServiceProviderImpl(
            @NonNull Context context,
            @NonNull CalendarDatabase database,
            @NonNull CoreBackupManager backupManager
    ) {
        this.mContext = context;
        this.mDatabase = database;
        this.mCoreBackupManager = backupManager;
    }

    // ==================== INFRASTRUCTURE HELPERS ====================

    /**
     * Get Context through ServiceProvider integration.
     */
    @NonNull
    private Context getContext() {
        return mContext;
    }

    /**
     * Get CalendarDatabase through ServiceProvider integration.
     */
    @NonNull
    private CalendarDatabase getDatabase() {
        return mDatabase;
    }

    /**
     * Get LocaleManager through ServiceProvider.
     */
    @NonNull
    private LocaleManager getLocaleManager() {
        if (mLocaleManager == null) {
            synchronized (this) {
                if (mLocaleManager == null) {
                    mLocaleManager = new LocaleManager( getContext() );
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
                    mDomainLocalizer = new DomainLocalizerImpl( getContext(), getLocaleManager() );
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
    }

    // ==================== DOMAIN REPOSITORIES ====================

    @Override
    @NonNull
    public LocalEventsRepository getLocalEventsRepository() {
        if (mLocalEventsRepository == null) {
            synchronized (mLocalEventsRepositoryLock) {
                if (mLocalEventsRepository == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating LocalEventsRepository instance" );
                    mLocalEventsRepository = new LocalEventsRepositoryImpl(
                            getDatabase().localEventDao()
                    );
                }
            }
        }
        return mLocalEventsRepository;
    }

    @Override
    @NonNull
    public QDueUserRepository getQDueUserRepository() {
        if (mQDueUserRepository == null) {
            synchronized (mQDueUserRepositoryLock) {
                if (mQDueUserRepository == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating QDueUserRepository instance" );
                    mQDueUserRepository = new QDueUserRepositoryImpl(
                            getDatabase()
                    );
                }
            }
        }
        return mQDueUserRepository;
    }

    @Override
    @NonNull
    public RecurrenceRuleRepository getRecurrenceRuleRepository() {
        if (mRecurrenceRuleRepository == null) {
            synchronized (mRecurrenceRepositoryLock) {
                if (mRecurrenceRuleRepository == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating RecurrenceRuleRepository instance" );
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
                    Log.d( TAG, "Creating ShiftExceptionRepository instance" );
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
                    Log.d( TAG, "Creating TeamRepository instance" );
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
                    Log.d( TAG, "Creating ShiftRepository instance" );
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
                    Log.d( TAG, "Creating UserScheduleAssignmentRepository instance" );
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
                    Log.d( TAG, "Creating WorkScheduleRepository instance" );

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

    /**
     * Get UserTeamAssignmentRepository for user-team assignment management.
     *
     * @return UserTeamAssignmentRepository instance
     */
    @NonNull
    @Override
    public UserTeamAssignmentRepository getUserTeamAssignmentRepository() {
        if (mUserTeamAssignmentRepository == null) {
            synchronized (mUserTeamAssignmentRepositoryLock) {
                if (mUserTeamAssignmentRepository == null) {
                    Log.d( TAG, "Creating UserTeamAssignmentRepository instance" );

                    UserTeamAssignmentDao assignmentDao = mDatabase.userTeamAssignmentDao();
                    mUserTeamAssignmentRepository = new UserTeamAssignmentRepositoryImpl(
                            mContext,
                            assignmentDao
                    );
                }
            }
        }
        return mUserTeamAssignmentRepository;
    }

    // ==================== DOMAIN ENGINES ====================

    @Override
    @NonNull
    public RecurrenceCalculator getRecurrenceCalculator() {
        if (mRecurrenceCalculator == null) {
            synchronized (mRecurrenceCalculatorLock) {
                if (mRecurrenceCalculator == null) {
                    ensureInitialized();
                    Log.d( TAG,
                           "Creating RecurrenceCalculator instance with localization support" );
                    mRecurrenceCalculator = new RecurrenceCalculator( getDomainLocalizer() );
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
                    Log.d( TAG, "Creating ExceptionResolver instance with localization support" );
                    mExceptionResolver = new ExceptionResolver( getDomainLocalizer() );
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
                    Log.d( TAG,
                           "Creating SchedulingEngine instance with coordinated domain engines" );
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

    // ==================== MODULES ====================

    @Override
    @NonNull
    public LocalEventsModule getLocaEventsModule() {
        if (mLocalEventsModule == null) {
            synchronized (mLocalEventsModuleLock) {
                if (mLocalEventsModule == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating LocalEventsModule instance" );
                    mLocalEventsModule = new LocalEventsModule(
                            getContext(),
                            getDatabase(),
                            getLocalEventsRepository(),
                            getLocalEventsUseCases(),
                            getLocalEventsService(),
                            getLocalEventsFileService()
                    );
                }
            }
        }
        return mLocalEventsModule;
    }

    // ======================================== SERVICES ========================================

    @Override
    public QDueUserService getQDueUserService() {
        if (mQDueUserService == null) {
            synchronized (mQDueUserServiceLock) {
                if (mQDueUserService == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating QDueUserService instance" );

                    // Create QDueUserRepository
                    QDueUserRepository qDueUserRepository = new QDueUserRepositoryImpl( getDatabase() );

                    // Create QDueUserUseCases
                    QDueUserUseCases qDueUserUseCases = new QDueUserUseCases( qDueUserRepository );

                    // Create QDueUserService
                    mQDueUserService = new QDueUserServiceImpl( qDueUserUseCases );
                }
            }
        }
        return mQDueUserService;
    }

    @Override
    @NonNull
    public UserWorkScheduleService getUserWorkScheduleService() {
        if (mUserWorkScheduleService == null) {

            synchronized (mUserWorkScheduleServiceLock) {
                if (mUserWorkScheduleService == null) {
                    try {
                        ensureInitialized();
                        Log.d( TAG, "Creating UserWorkScheduleService instance" );

                        WorkScheduleRepository repo = getWorkScheduleRepository();
                        GenerateUserScheduleUseCase use = new GenerateUserScheduleUseCase( repo );

                        mUserWorkScheduleService = new UserWorkScheduleServiceImpl( use );
                    } catch (Exception e) {
                        Log.e( TAG, "Failed to initialize UserWorkScheduleService", e );
                        throw new RuntimeException( "UserWorkScheduleService initialization failed",
                                                    e );
                    }
                }
            }
        }
        return mUserWorkScheduleService;
    }

    @Override
    @NonNull
    public UserSchedulePatternService getUserSchedulePatternService() {
        if (mUserSchedulePatternService == null) {

            synchronized (mUserSchedulePatternServiceLock) {
                if (mUserSchedulePatternService == null) {
                    try {
                        ensureInitialized();
                        Log.d( TAG, "Creating UserSchedulePatternService instance" );

                        mUserSchedulePatternService = new UserSchedulePatternServiceImpl(
                                getContext(),
                                getShiftRepository(),
                                getUserScheduleAssignmentRepository(),
                                getRecurrenceRuleRepository(),
                                getLocaleManager()
                        );
                    } catch (Exception e) {
                        Log.e( TAG, "Failed to initialize UserSchedulePatternService", e );
                        throw new RuntimeException(
                                "UserSchedulePatternService initialization failed", e );
                    }
                }
            }
        }
        return mUserSchedulePatternService;
    }

    /**
     * Get LocalEventsFileService for local event file operations.
     *
     * @return LocalEventsFileService instance
     */
    @NonNull
    public LocalEventsFileService getLocalEventsFileService() {
        if (mLocalEventsFileService == null) {
            synchronized (mLocalEventsFileServiceLock) {
                if (mLocalEventsFileService == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating LocalEventsFileService instance" );
                    mLocalEventsFileService = new LocalEventsFileServiceImpl(
                            getContext(),
                            getLocalEventsService()
                    );
                }
            }
        }
        return mLocalEventsFileService;
    }

    /**
     * Get LocalEventsService for local event operations.
     *
     * @return LocalEventsService instance
     */
    @NonNull
    public LocalEventsService getLocalEventsService() {
        if (mLocalEventsService == null) {
            synchronized (mLocalEventsServiceLock) {
                if (mLocalEventsService == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating LocalEventsService instance" );
                    mLocalEventsService = new LocalEventsServiceImpl(
                            getContext(),
                            getLocalEventsUseCases()
                    );

                    // Initialize the service
                    mLocalEventsService.initialize();
                }
            }
        }
        return mLocalEventsService;
    }

    // ==================== USE CASES ====================

    @NonNull
    @Override
    public LocalEventsUseCases getLocalEventsUseCases() {
        if (mLocalEventsUseCases == null) {
            synchronized (mLocalEventsUseCasesLock) {
                if (mLocalEventsUseCases == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating LocalEventsUseCases instance" );
                    mLocalEventsUseCases = new LocalEventsUseCases(
                            getLocalEventsRepository()
                    );
                }
            }
        }
        return mLocalEventsUseCases;
    }

    @NonNull
    @Override
    public TeamUseCases getTeamUseCases() {
        if (mTeamUseCases == null) {
            synchronized (mTeamUseCasesLock) {
                if (mTeamUseCases == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating TeamUseCases instance" );
                    mTeamUseCases = new TeamUseCases(
                            getTeamRepository()
                            //getDomainLocalizer()
                    );
                }
            }
        }
        return mTeamUseCases;
    }

    @NonNull
    @Override
    public UserTeamAssignmentUseCases getUserTeamAssignmentUseCases() {
        if (mUserTeamAssignmentUseCases == null) {
            synchronized (mUserTeamAssignmentUseCasesLock) {
                if (mUserTeamAssignmentUseCases == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating UserTeamAssignmentUseCases instance" );
                    mUserTeamAssignmentUseCases = new UserTeamAssignmentUseCases(
                            getQDueUserRepository(),
                            getUserTeamAssignmentRepository(),
                            getTeamRepository()
                    );
                }
            }
        }
        return mUserTeamAssignmentUseCases;
    }

    @NonNull
    @Override
    public CreatePatternAssignmentUseCase getCreatePatternAssignmentUseCase() {
        if (mCreatePatternAssignmentUseCase == null) {
            synchronized (mCreatePatternAssignmentUseCaseLock) {
                mCreatePatternAssignmentUseCase = new CreatePatternAssignmentUseCase(
                        getTeamRepository(),
                        getRecurrenceRuleRepository(),
                        getUserScheduleAssignmentRepository()
                );
            }
        }
        return mCreatePatternAssignmentUseCase;
    }

    @Override
    @NonNull
    public GenerateTeamScheduleUseCase getGenerateTeamScheduleUseCase() {
        if (mGenerateTeamScheduleUseCase == null) {
            synchronized (mTeamScheduleUseCaseLock) {
                if (mGenerateTeamScheduleUseCase == null) {
                    ensureInitialized();
                    Log.d( TAG, "Creating GenerateTeamScheduleUseCase instance" );
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
                    Log.d( TAG, "Creating ApplyShiftExceptionsUseCase instance" );
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
                    Log.d( TAG, "Creating GetScheduleStatsUseCase instance" );
                    mGetScheduleStatsUseCase = new GetScheduleStatsUseCase(
                            getWorkScheduleRepository()
                    );
                }
            }
        }
        return mGetScheduleStatsUseCase;
    }

//    @Override
//    @NonNull
//    public UseCaseFactory getUseCaseFactory() {
//        if (mUseCaseFactory == null) {
//            synchronized (mUseCaseFactoryLock) {
//                if (mUseCaseFactory == null) {
//                    ensureInitialized();
//                    Log.d( TAG, "Creating UseCaseFactory instance with all use cases" );
//                    mUseCaseFactory = new UseCaseFactory( getWorkScheduleRepository() );
//                }
//            }
//        }
//        return mUseCaseFactory;
//    }
//
//    /**
//     * Get ManageUserTeamAssignmentsUseCase for assignment creation/management.
//     *
//     * @return ManageUserTeamAssignmentsUseCase instance
//     */
//    @NonNull
//    @Override
//    public ManageUserTeamAssignmentsUseCase getManageUserTeamAssignmentsUseCase() {
//        if (mManageUserTeamAssignmentsUseCase == null) {
//            synchronized (mManageUserTeamAssignmentsUseCaseLock) {
//                if (mManageUserTeamAssignmentsUseCase == null) {
//                    Log.d( TAG, "Creating ManageUserTeamAssignmentsUseCase instance" );
//
//                    mManageUserTeamAssignmentsUseCase = new ManageUserTeamAssignmentsUseCase(
//                            getUserTeamAssignmentRepository(),
//                            getQDueUserRepository(), // Assuming this exists
//                            getTeamRepository()  // Assuming this exists
//                    );
//                }
//            }
//        }
//        return mManageUserTeamAssignmentsUseCase;
//    }
//
//    /**
//     * Get GetTeamMembersUseCase for team member queries.
//     *
//     * @return GetTeamMembersUseCase instance
//     */
//    @NonNull
//    @Override
//    public GetTeamMembersUseCase getGetTeamMembersUseCase() {
//        if (mGetTeamMembersUseCase == null) {
//            synchronized (mGetTeamMembersUseCaseLock) {
//                if (mGetTeamMembersUseCase == null) {
//                    Log.d( TAG, "Creating GetTeamMembersUseCase instance" );
//
//                    mGetTeamMembersUseCase = new GetTeamMembersUseCase(
//                            getUserTeamAssignmentRepository(),
//                            getQDueUserRepository(),
//                            getTeamRepository()
//                    );
//                }
//            }
//        }
//        return mGetTeamMembersUseCase;
//    }
//
//    /**
//     * Get ValidateAssignmentUseCase for assignment validation.
//     *
//     * @return ValidateAssignmentUseCase instance
//     */
//    @NonNull
//    @Override
//    public ValidateAssignmentUseCase getValidateAssignmentUseCase() {
//        if (mValidateAssignmentUseCase == null) {
//            synchronized (mValidateAssignmentUseCaseLock) {
//                if (mValidateAssignmentUseCase == null) {
//                    Log.d( TAG, "Creating ValidateAssignmentUseCase instance" );
//
//                    mValidateAssignmentUseCase = new ValidateAssignmentUseCase(
//                            getUserTeamAssignmentRepository()
//                    );
//                }
//            }
//        }
//        return mValidateAssignmentUseCase;
//    }

    // ==================== USE CASE PROVIDERS ====================

    /**
     * Get GenerateUserScheduleUseCase for individual user schedule operations.
     *
     * <p>Provides comprehensive user schedule generation with recurrence rules,
     * exception handling, team assignments, and business rule validation.</p>
     *
     * @return GenerateUserScheduleUseCase instance (cached)
     */
    @NonNull
    public GenerateUserScheduleUseCase getGenerateUserScheduleUseCase() {
        if (mGenerateUserScheduleUseCase == null) {
            synchronized (mUserScheduleUseCaseLock) {
                if (mGenerateUserScheduleUseCase == null) {
                    mGenerateUserScheduleUseCase = new GenerateUserScheduleUseCase(
                            getWorkScheduleRepository() );
                    Log.d( TAG, "Created GenerateUserScheduleUseCase instance" );
                }
            }
        }
        return mGenerateUserScheduleUseCase;
    }

    /**
     * Get GenerateTeamScheduleUseCase for team coordination operations.
     *
     * <p>Provides team-wide schedule generation with coverage analysis,
     * resource optimization, conflict detection, and multi-team coordination.</p>
     *
     * @return GenerateTeamScheduleUseCase instance (cached)
     */
    @NonNull
    public GenerateTeamScheduleUseCase getTeamScheduleUseCase() {
        if (mGenerateTeamScheduleUseCase == null) {
            synchronized (mTeamScheduleUseCaseLock) {
                if (mGenerateTeamScheduleUseCase == null) {
                    mGenerateTeamScheduleUseCase = new GenerateTeamScheduleUseCase(
                            getWorkScheduleRepository() );
                    Log.d( TAG, "Created GenerateTeamScheduleUseCase instance" );
                }
            }
        }
        return mGenerateTeamScheduleUseCase;
    }

    /**
     * Get ApplyShiftExceptionsUseCase for exception handling workflow.
     *
     * <p>Provides comprehensive shift exception processing with approval
     * workflows, conflict resolution, and business rule validation.</p>
     *
     * @return ApplyShiftExceptionsUseCase instance (cached)
     */
    @NonNull
    public ApplyShiftExceptionsUseCase getShiftExceptionsUseCase() {
        if (mApplyShiftExceptionsUseCase == null) {
            synchronized (mShiftExceptionsUseCaseLock) {
                if (mApplyShiftExceptionsUseCase == null) {
                    mApplyShiftExceptionsUseCase = new ApplyShiftExceptionsUseCase(
                            getWorkScheduleRepository() );
                    Log.d( TAG, "Created ApplyShiftExceptionsUseCase instance" );
                }
            }
        }
        return mApplyShiftExceptionsUseCase;
    }

    /**
     * Get GetScheduleStatsUseCase for analytics and reporting.
     *
     * <p>Provides comprehensive schedule analytics, statistics generation,
     * validation capabilities, and reporting for management and planning.</p>
     *
     * @return GetScheduleStatsUseCase instance (cached)
     */
    @NonNull
    @Override
    public GetScheduleStatsUseCase getScheduleStatsUseCase() {
        if (mGetScheduleStatsUseCase == null) {
            synchronized (mScheduleStatsUseCaseLock) {
                if (mGetScheduleStatsUseCase == null) {
                    mGetScheduleStatsUseCase = new GetScheduleStatsUseCase(
                            getWorkScheduleRepository() );
                    Log.d( TAG, "Created GetScheduleStatsUseCase instance" );
                }
            }
        }
        return mGetScheduleStatsUseCase;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    public void initializeCalendarServices() {
        synchronized (mInitializationLock) {
            if (mCalendarServicesInitialized) {
                Log.d( TAG, "Calendar services already initialized" );
                return;
            }

            Log.d( TAG, "Initializing calendar services..." );
            long startTime = System.currentTimeMillis();

            try {
                // Mark as initialized
                mCalendarServicesInitialized = true;
                mInitializationTime = System.currentTimeMillis() - startTime;

                Log.i( TAG,
                       "Calendar services initialized successfully (" + mInitializationTime + "ms)" );
            } catch (Exception e) {
                Log.e( TAG, "Failed to initialize calendar services", e );
                mCalendarServicesInitialized = false;
                throw new RuntimeException( "Calendar services initialization failed", e );
            }
        }
    }

    @Override
    public boolean areCalendarServicesReady() {
        return mCalendarServicesInitialized && !mCalendarServicesShutdown;
    }

    @Override
    public void shutdownCalendarServices() {
        synchronized (mInitializationLock) {
            if (mCalendarServicesShutdown) {
                Log.w( TAG, "Calendar services already shutdown" );
                return;
            }

            Log.i( TAG, "Shutting down calendar services..." );

            try {
                // Shutdown SERVICES
                if (mLocalEventsService != null)
                    mLocalEventsService.shutdown();
                if (mLocalEventsFileService != null)
                    mLocalEventsFileService.shutdown();
                mLocalEventsService=null;
                mLocalEventsFileService=null;
                mUserWorkScheduleService=null;
                mUserSchedulePatternService=null;

                // Shutdown USECASES
                if (mUserTeamAssignmentUseCases != null)
                    mUserTeamAssignmentUseCases.shutdown();
                mUserTeamAssignmentUseCases = null;
                mGetScheduleStatsUseCase = null;
                mApplyShiftExceptionsUseCase = null;
                mGenerateTeamScheduleUseCase = null;
                mGenerateUserScheduleUseCase = null;

                // Shutdown domain ENGINES
                mSchedulingEngine = null;
                mExceptionResolver = null;
                mRecurrenceCalculator = null;

                // Shutdown REPOSITORIES
                if (mWorkScheduleRepository != null && mWorkScheduleRepository instanceof WorkScheduleRepositoryImpl) {
                    ((WorkScheduleRepositoryImpl) mWorkScheduleRepository).cleanup();
                }
                if (mUserTeamAssignmentRepository != null && mUserTeamAssignmentRepository instanceof UserTeamAssignmentRepositoryImpl) {
                    ((UserTeamAssignmentRepositoryImpl) mUserTeamAssignmentRepository).shutdown();
                }
                if (mQDueUserRepository != null && mQDueUserRepository instanceof QDueUserRepositoryImpl) {
                    ((QDueUserRepositoryImpl) mQDueUserRepository).shutdown();
                }
                mWorkScheduleRepository = null;
                mUserTeamAssignmentRepository = null;
                mQDueUserRepository = null;
                mUserScheduleAssignmentRepository = null;
                mShiftRepository = null;
                mTeamRepository = null;
                mShiftExceptionRepository = null;
                mRecurrenceRuleRepository = null;

                // Shutdown infrastructure
                mDomainLocalizer = null;
                mLocaleManager = null;

                mCalendarServicesShutdown = true;
                Log.i( TAG, "Calendar services shutdown completed" );
            } catch (Exception e) {
                Log.e( TAG, "Error during calendar service shutdown", e );
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
                countActiveServices() > 0,
                countActiveUseCases() > 0,
                mInitializationTime,
                countActiveRepositories(),
                countActiveEngines(),
                countActiveServices(),
                countActiveUseCases(),
                mCalendarServicesInitialized ?
                        "Calendar Services initialized" :
                        "Calendar Services not initialized"
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
        if (mQDueUserRepository != null) count++;
        if (mRecurrenceRuleRepository != null) count++;
        if (mShiftExceptionRepository != null) count++;
        if (mTeamRepository != null) count++;
        if (mShiftRepository != null) count++;
        if (mUserScheduleAssignmentRepository != null) count++;
        if (mWorkScheduleRepository != null) count++;
        if (mUserTeamAssignmentRepository != null) count++;
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

    private int countActiveServices() {
        int count = 0;
        if (mLocalEventsService != null) count++;
        if (mLocalEventsFileService != null) count++;
        if (mUserWorkScheduleService != null) count++;
        if (mUserSchedulePatternService != null) count++;
        return count;
    }

    /**
     * Count active use cases for status reporting.
     */
    private int countActiveUseCases() {
        int count = 0;
        if (mUserTeamAssignmentUseCases != null) count++;
        if (mCreatePatternAssignmentUseCase != null) count++;
        if (mGenerateUserScheduleUseCase != null) count++;
        if (mGenerateTeamScheduleUseCase != null) count++;
        if (mApplyShiftExceptionsUseCase != null) count++;
        if (mGetScheduleStatsUseCase != null) count++;
        return count;
    }
}