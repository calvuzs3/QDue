package net.calvuz.qdue.ui.features.swipecalendar.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.ui.features.swipecalendar.viewmodels.CalendarSharedViewModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * CalendarSharedViewModelModule - Dependency Injection Module for Calendar Shared ViewModel
 *
 * <p>Provides centralized dependency management for CalendarSharedViewModel following clean
 * architecture principles and established DI patterns. Integrates with existing CalendarServiceProvider
 * and creates Activity-scoped ViewModel instances for state sharing between fragments.</p>
 *
 * <h3>Managed Dependencies:</h3>
 * <ul>
 *   <li><strong>Core Services</strong>: LocalEventsService, QDueUserService, UserWorkScheduleService</li>
 *   <li><strong>Domain Repositories</strong>: WorkScheduleRepository for schedule data access</li>
 *   <li><strong>Use Cases</strong>: GenerateUserScheduleUseCase for business logic</li>
 *   <li><strong>Infrastructure</strong>: LocaleManager for i18n support</li>
 *   <li><strong>ViewModel Factory</strong>: Custom ViewModelProvider.Factory for dependency injection</li>
 * </ul>
 *
 * <h3>Architecture Pattern:</h3>
 * <ul>
 *   <li><strong>Manual DI</strong>: Constructor-based dependency injection</li>
 *   <li><strong>Service Locator</strong>: Integration with existing CalendarServiceProvider</li>
 *   <li><strong>ViewModel Factory</strong>: Custom factory for ViewModel dependency injection</li>
 *   <li><strong>Activity Scope</strong>: ViewModel shared across fragments within same Activity</li>
 *   <li><strong>Clean Separation</strong>: Domain layer isolated from UI concerns</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * // In Activity.onCreate():
 * CalendarSharedViewModelModule module = new CalendarSharedViewModelModule(this, calendarServiceProvider);
 * ViewModelProvider.Factory factory = module.getViewModelFactory();
 * mSharedViewModel = new ViewModelProvider(this, factory).get(CalendarSharedViewModel.class);
 *
 * // In Fragment.onViewCreated():
 * mSharedViewModel = new ViewModelProvider(requireActivity()).get(CalendarSharedViewModel.class);
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since SharedViewModel Implementation
 */
public class CalendarSharedViewModelModule {

    private static final String TAG = "CalendarSharedViewModelModule";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarServiceProvider mCalendarServiceProvider;

    // ==================== LAZY-LOADED SERVICES ====================

    private LocalEventsService mLocalEventsService;
    private QDueUserService mQDueUserService;
    private UserWorkScheduleService mUserWorkScheduleService;
    private WorkScheduleRepository mWorkScheduleRepository;
    private GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;
    private LocaleManager mLocaleManager;

    // ==================== VIEW MODEL FACTORY ====================

    private ViewModelProvider.Factory mViewModelFactory;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new CalendarSharedViewModelModule with dependency injection.
     *
     * @param context                Application or Activity context
     * @param calendarServiceProvider Calendar service provider for dependency resolution
     */
    public CalendarSharedViewModelModule(@NonNull Context context,
                                         @NonNull CalendarServiceProvider calendarServiceProvider) {
        this.mContext = context.getApplicationContext();
        this.mCalendarServiceProvider = calendarServiceProvider;

        Log.d(TAG, "CalendarSharedViewModelModule initialized");
    }

    // ==================== SERVICE ACCESSORS ====================

    /**
     * Get LocalEventsService with lazy initialization.
     *
     * @return LocalEventsService instance
     */
    @NonNull
    public LocalEventsService getLocalEventsService() {
        if (mLocalEventsService == null) {
            mLocalEventsService = mCalendarServiceProvider.getLocalEventsService();
            Log.v(TAG, "LocalEventsService initialized");
        }
        return mLocalEventsService;
    }

    /**
     * Get QDueUserService with lazy initialization.
     *
     * @return QDueUserService instance
     */
    @NonNull
    public QDueUserService getQDueUserService() {
        if (mQDueUserService == null) {
            mQDueUserService = mCalendarServiceProvider.getQDueUserService();
            Log.v(TAG, "QDueUserService initialized");
        }
        return mQDueUserService;
    }

    /**
     * Get UserWorkScheduleService with lazy initialization.
     *
     * @return UserWorkScheduleService instance
     */
    @NonNull
    public UserWorkScheduleService getUserWorkScheduleService() {
        if (mUserWorkScheduleService == null) {
            mUserWorkScheduleService = mCalendarServiceProvider.getUserWorkScheduleService();
            Log.v(TAG, "UserWorkScheduleService initialized");
        }
        return mUserWorkScheduleService;
    }

    /**
     * Get WorkScheduleRepository with lazy initialization.
     *
     * @return WorkScheduleRepository instance
     */
    @NonNull
    public WorkScheduleRepository getWorkScheduleRepository() {
        if (mWorkScheduleRepository == null) {
            mWorkScheduleRepository = mCalendarServiceProvider.getWorkScheduleRepository();
            Log.v(TAG, "WorkScheduleRepository initialized");
        }
        return mWorkScheduleRepository;
    }

    /**
     * Get GenerateUserScheduleUseCase with lazy initialization.
     *
     * @return GenerateUserScheduleUseCase instance
     */
    @NonNull
    public GenerateUserScheduleUseCase getGenerateUserScheduleUseCase() {
        if (mGenerateUserScheduleUseCase == null) {
            mGenerateUserScheduleUseCase = mCalendarServiceProvider.getGenerateUserScheduleUseCase();
            Log.v(TAG, "GenerateUserScheduleUseCase initialized");
        }
        return mGenerateUserScheduleUseCase;
    }

    /**
     * Get LocaleManager with lazy initialization.
     *
     * @return LocaleManager instance
     */
    @NonNull
    public LocaleManager getLocaleManager() {
        if (mLocaleManager == null) {
            mLocaleManager = mCalendarServiceProvider.getLocaleManager();
            Log.v(TAG, "LocaleManager initialized");
        }
        return mLocaleManager;
    }

    // ==================== VIEW MODEL FACTORY ====================

    /**
     * Get ViewModelProvider.Factory for creating CalendarSharedViewModel instances.
     * The factory handles dependency injection for the ViewModel.
     *
     * @return ViewModelProvider.Factory instance
     */
    @NonNull
    public ViewModelProvider.Factory getViewModelFactory() {
        if (mViewModelFactory == null) {
            mViewModelFactory = new CalendarSharedViewModelFactory(
                    getLocalEventsService(),
                    getQDueUserService(),
                    getUserWorkScheduleService(),
                    getWorkScheduleRepository(),
                    getGenerateUserScheduleUseCase(),
                    getLocaleManager()
            );
            Log.d(TAG, "ViewModelFactory created");
        }
        return mViewModelFactory;
    }

    // ==================== VIEW MODEL FACTORY IMPLEMENTATION ====================

    /**
     * Custom ViewModelProvider.Factory for CalendarSharedViewModel dependency injection.
     */
    public static class CalendarSharedViewModelFactory implements ViewModelProvider.Factory {

        private final LocalEventsService mEventsService;
        private final QDueUserService mUserService;
        private final UserWorkScheduleService mUserWorkScheduleService;
        private final WorkScheduleRepository mWorkScheduleRepository;
        private final GenerateUserScheduleUseCase mUserScheduleUseCase;
        private final LocaleManager mLocaleManager;

        /**
         * Create factory with injected dependencies.
         */
        public CalendarSharedViewModelFactory(@NonNull LocalEventsService eventsService,
                                              @NonNull QDueUserService userService,
                                              @NonNull UserWorkScheduleService userWorkScheduleService,
                                              @NonNull WorkScheduleRepository workScheduleRepository,
                                              @NonNull GenerateUserScheduleUseCase userScheduleUseCase,
                                              @NonNull LocaleManager localeManager) {
            this.mEventsService = eventsService;
            this.mUserService = userService;
            this.mUserWorkScheduleService = userWorkScheduleService;
            this.mWorkScheduleRepository = workScheduleRepository;
            this.mUserScheduleUseCase = userScheduleUseCase;
            this.mLocaleManager = localeManager;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(CalendarSharedViewModel.class)) {
                return (T) new CalendarSharedViewModel(
                        mEventsService,
                        mUserService,
                        mUserWorkScheduleService,
                        mWorkScheduleRepository,
                        mUserScheduleUseCase,
                        mLocaleManager
                );
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }

    // ==================== LIFECYCLE ====================

    /**
     * Cleanup resources when module is no longer needed.
     * Call this from Activity.onDestroy() if needed.
     */
    public void cleanup() {
        // Clear references to allow garbage collection
        mLocalEventsService = null;
        mQDueUserService = null;
        mUserWorkScheduleService = null;
        mWorkScheduleRepository = null;
        mGenerateUserScheduleUseCase = null;
        mLocaleManager = null;
        mViewModelFactory = null;

        Log.d(TAG, "CalendarSharedViewModelModule cleaned up");
    }

    // ==================== VALIDATION ====================

    /**
     * Check if all required dependencies are available.
     *
     * @return true if all dependencies are ready
     */
    public boolean areDependenciesReady() {
        try {
            // Test service availability by lazy loading
            getLocalEventsService();
            getQDueUserService();
            getUserWorkScheduleService();
            getWorkScheduleRepository();
            getGenerateUserScheduleUseCase();
            getLocaleManager();

            Log.d(TAG, "All dependencies are ready");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Dependency validation failed", e);
            return false;
        }
    }
}