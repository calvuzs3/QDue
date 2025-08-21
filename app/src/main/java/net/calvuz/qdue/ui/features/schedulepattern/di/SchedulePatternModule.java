package net.calvuz.qdue.ui.features.schedulepattern.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.di.CalendarServiceProviderImpl;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.ui.features.schedulepattern.services.UserSchedulePatternService;
import net.calvuz.qdue.ui.features.schedulepattern.services.impl.UserSchedulePatternServiceImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * SchedulePatternModule - Dependency Injection Module for Schedule Pattern Feature
 *
 * <p>Provides centralized dependency management for all Schedule Pattern creation components
 * following clean architecture principles and established DI patterns.</p>
 *
 * <h3>Managed Dependencies:</h3>
 * <ul>
 *   <li><strong>Core Services</strong>: CalendarService integration</li>
 *   <li><strong>Domain Repositories</strong>: Shift, Assignment, RecurrenceRule repositories</li>
 *   <li><strong>Feature Services</strong>: UserSchedulePatternService for business logic</li>
 *   <li><strong>Infrastructure</strong>: LocaleManager for i18n support</li>
 * </ul>
 *
 * <h3>Architecture Pattern:</h3>
 * <ul>
 *   <li><strong>Manual DI</strong>: Constructor-based dependency injection</li>
 *   <li><strong>Service Locator</strong>: Integration with existing ServiceProvider</li>
 *   <li><strong>Lazy Loading</strong>: Services created on-demand</li>
 *   <li><strong>Clean Separation</strong>: Domain layer isolated from UI concerns</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * // In Activity.inject() method:
 * SchedulePatternModule module = new SchedulePatternModule(this, serviceProvider);
 * mShiftRepository = module.getShiftRepository();
 * mPatternService = module.getUserSchedulePatternService();
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class SchedulePatternModule {

    private static final String TAG = "SchedulePatternModule";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final ServiceProvider mServiceProvider;
    private final CalendarServiceProvider mCalendarServiceProvider;

    // ==================== LAZY-LOADED SERVICES ====================

    private ShiftRepository mShiftRepository;
    private UserScheduleAssignmentRepository mUserScheduleAssignmentRepository;
    private RecurrenceRuleRepository mRecurrenceRuleRepository;
    private UserSchedulePatternService mUserSchedulePatternService;
    private LocaleManager mLocaleManager;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new SchedulePatternModule with dependency injection.
     *
     * @param context Application or Activity context
     * @param serviceProvider Core service provider
     */
    public SchedulePatternModule(@NonNull Context context, @NonNull ServiceProvider serviceProvider) {
        this.mContext = context.getApplicationContext();
        this.mServiceProvider = serviceProvider;

        // Get calendar service provider from core service provider
        CalendarService calendarService = serviceProvider.getCalendarService();
        CalendarServiceProvider calSerPro = serviceProvider.getCalendarServiceProvider();

//        if (calendarService instanceof CalendarServiceProvider) {
//            this.mCalendarServiceProvider = (CalendarServiceProvider) calendarService;
//        } else {
//            throw new IllegalStateException("*** CalendarService must implement CalendarServiceProvider");
//        }
        if (calSerPro instanceof CalendarServiceProvider) {
            this.mCalendarServiceProvider = (CalendarServiceProvider) calSerPro;
        } else {
            throw new IllegalStateException("*** CalendarService must implement CalendarServiceProvider");
        }

        Log.i(TAG, "SchedulePatternModule initialized successfully");
    }

    // ==================== REPOSITORY PROVIDERS ====================

    /**
     * Get ShiftRepository for shift template operations.
     *
     * @return ShiftRepository instance
     */
    @NonNull
    public ShiftRepository getShiftRepository() {
        if (mShiftRepository == null) {
            mShiftRepository = mCalendarServiceProvider.getShiftRepository();
            Log.d(TAG, "ShiftRepository created");
        }
        return mShiftRepository;
    }

    /**
     * Get UserScheduleAssignmentRepository for assignment management.
     *
     * @return UserScheduleAssignmentRepository instance
     */
    @NonNull
    public UserScheduleAssignmentRepository getUserScheduleAssignmentRepository() {
        if (mUserScheduleAssignmentRepository == null) {
            mUserScheduleAssignmentRepository = mCalendarServiceProvider.getUserScheduleAssignmentRepository();
            Log.d(TAG, "UserScheduleAssignmentRepository created");
        }
        return mUserScheduleAssignmentRepository;
    }

    /**
     * Get RecurrenceRuleRepository for pattern persistence.
     *
     * @return RecurrenceRuleRepository instance
     */
    @NonNull
    public RecurrenceRuleRepository getRecurrenceRuleRepository() {
        if (mRecurrenceRuleRepository == null) {
            mRecurrenceRuleRepository = mCalendarServiceProvider.getRecurrenceRuleRepository();
            Log.d(TAG, "RecurrenceRuleRepository created");
        }
        return mRecurrenceRuleRepository;
    }

    // ==================== FEATURE SERVICE PROVIDERS ====================

    /**
     * Get UserSchedulePatternService for pattern creation business logic.
     *
     * @return UserSchedulePatternService instance
     */
    @NonNull
    public UserSchedulePatternService getUserSchedulePatternService() {
        if (mUserSchedulePatternService == null) {
            mUserSchedulePatternService = new UserSchedulePatternServiceImpl(
                    mContext,
                    getShiftRepository(),
                    getUserScheduleAssignmentRepository(),
                    getRecurrenceRuleRepository(),
                    getLocaleManager()
            );
            Log.d(TAG, "UserSchedulePatternService created");
        }
        return mUserSchedulePatternService;
    }

    // ==================== INFRASTRUCTURE PROVIDERS ====================

    /**
     * Get LocaleManager for internationalization support.
     *
     * @return LocaleManager instance
     */
    @NonNull
    public LocaleManager getLocaleManager() {
        if (mLocaleManager == null) {
            mLocaleManager = new LocaleManager(mContext);
            Log.d(TAG, "LocaleManager created");
        }
        return mLocaleManager;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a new pattern creation session with all required dependencies.
     *
     * @return PatternCreationSession with injected dependencies
     */
    @NonNull
    public PatternCreationSession createPatternCreationSession() {
        return new PatternCreationSession(
                getUserSchedulePatternService(),
                getShiftRepository(),
                getLocaleManager()
        );
    }

    // ==================== VALIDATION ====================

    /**
     * Validate that all required dependencies are available.
     *
     * @return true if all dependencies ready, false otherwise
     */
    public boolean validateDependencies() {
        try {
            // Check core service provider
            if (mServiceProvider == null) {
                Log.e(TAG, "ServiceProvider is null");
                return false;
            }

            // Check calendar service provider
            if (mCalendarServiceProvider == null) {
                Log.e(TAG, "CalendarServiceProvider is null");
                return false;
            }

            // Verify repositories can be created
            ShiftRepository shiftRepo = getShiftRepository();
            if (shiftRepo == null) {
                Log.e(TAG, "ShiftRepository cannot be created");
                return false;
            }

            Log.d(TAG, "All dependencies validated successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error validating dependencies", e);
            return false;
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * PatternCreationSession - Session object for pattern creation workflow.
     *
     * <p>Encapsulates all dependencies needed for a complete pattern creation session,
     * providing a clean API for the UI layer.</p>
     */
    public static class PatternCreationSession {

        private final UserSchedulePatternService mPatternService;
        private final ShiftRepository mShiftRepository;
        private final LocaleManager mLocaleManager;

        public PatternCreationSession(@NonNull UserSchedulePatternService patternService,
                                      @NonNull ShiftRepository shiftRepository,
                                      @NonNull LocaleManager localeManager) {
            this.mPatternService = patternService;
            this.mShiftRepository = shiftRepository;
            this.mLocaleManager = localeManager;
        }

        @NonNull
        public UserSchedulePatternService getPatternService() {
            return mPatternService;
        }

        @NonNull
        public ShiftRepository getShiftRepository() {
            return mShiftRepository;
        }

        @NonNull
        public LocaleManager getLocaleManager() {
            return mLocaleManager;
        }
    }
}