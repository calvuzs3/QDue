package net.calvuz.qdue.ui.features.events.local.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.data.dao.LocalEventDao;
import net.calvuz.qdue.data.repositories.LocalEventsRepositoryImpl;
import net.calvuz.qdue.data.services.LocalEventsService;
import net.calvuz.qdue.data.services.LocalEventsFileService;
import net.calvuz.qdue.data.services.impl.LocalEventsServiceImpl;
import net.calvuz.qdue.domain.calendar.repositories.LocalEventsRepository;
import net.calvuz.qdue.domain.calendar.usecases.LocalEventsUseCases;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * LocalEvents Dependency Injection Module
 *
 * <p>Comprehensive dependency injection module for all LocalEvents MVVM components
 * following clean architecture principles and established DI patterns in QDue.
 * This module provides centralized dependency management while integrating with
 * the existing CalendarServiceProvider infrastructure.</p>
 *
 * <h3>Managed Dependencies:</h3>
 * <ul>
 *   <li><strong>Data Layer</strong>: LocalEventsRepository and DAO integration</li>
 *   <li><strong>Domain Layer</strong>: LocalEventsUseCases business logic</li>
 *   <li><strong>Service Layer</strong>: LocalEventsService and FileService</li>
 *   <li><strong>Presentation Layer</strong>: ViewModels for MVVM architecture</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <ul>
 *   <li><strong>CalendarServiceProvider</strong>: Integrates with existing service provider</li>
 *   <li><strong>Database Access</strong>: Uses existing QDueDatabase infrastructure</li>
 *   <li><strong>Manual DI</strong>: Constructor-based dependency injection</li>
 *   <li><strong>Lazy Loading</strong>: Components created on-demand</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>
 * // In Activity onCreate():
 * LocalEventsModule module = new LocalEventsModule(this, calendarServiceProvider);
 *
 * // Get ViewModels:
 * LocalEventsViewModel eventsViewModel = module.getLocalEventsViewModel();
 * LocalEventsFileOperationsViewModel fileOpsViewModel = module.getFileOperationsViewModel();
 *
 * // Initialize ViewModels:
 * eventsViewModel.initialize();
 * fileOpsViewModel.initialize();
 * </pre>
 *
 * <h3>Service Integration:</h3>
 * <p>LocalEventsService will be registered with CalendarServiceProvider to make it
 * available throughout the application architecture.</p>
 *
 * @see net.calvuz.qdue.data.di.CalendarServiceProvider
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsModule {

    private static final String TAG = "LocalEventsModule";

    // ==================== CORE DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarDatabase mDatabase;

    // ==================== LAZY LOADED COMPONENTS ====================

    // Data Layer
    private volatile LocalEventDao mLocalEventDao;
    private volatile LocalEventsRepository mLocalEventsRepository;

    // Domain Layer
    private volatile LocalEventsUseCases mLocalEventsUseCases;

    // Service Layer
    private volatile LocalEventsService mLocalEventsService;
    private volatile LocalEventsFileService mLocalEventsFileService;

    // Presentation Layer
    private volatile LocalEventsViewModel mLocalEventsViewModel;
    private volatile LocalEventsFileOperationsViewModel mFileOperationsViewModel;

    // ==================== THREAD SAFETY ====================

    private final Object mViewModelsLock = new Object();

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection module.
     *
     * @param context Application context
     * @param database Existing calendar database
     */
    public LocalEventsModule(@NonNull Context context,
                             @NonNull CalendarDatabase database,
                             @NonNull LocalEventsRepository repository,
                             @NonNull LocalEventsUseCases useCases,
                             @NonNull LocalEventsService service,
                             @NonNull LocalEventsFileService fileService
    ) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mLocalEventsRepository = repository;
        this.mLocalEventsUseCases = useCases;
        this.mLocalEventsService = service;
        this.mLocalEventsFileService = fileService;

        this.initializeServices();
        this.initializeViewModels();

        Log.d(TAG, "LocalEventsModule initialized");
    }

    // ==================== DATA LAYER PROVIDERS ====================

    /**
     * Get LocalEventDao instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventDao getLocalEventDao() {
        if (mLocalEventDao == null) {
            mLocalEventDao = mDatabase.localEventDao();
        }
        return mLocalEventDao;
    }

    /**
     * Get LocalEventsRepository instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsRepository getLocalEventRepository() {
        return mLocalEventsRepository;
    }

    // ==================== DOMAIN LAYER PROVIDERS ====================

    /**
     * Get LocalEventsUseCases instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsUseCases getLocalEventsUseCases() {
        return mLocalEventsUseCases;
    }

    // ==================== SERVICE LAYER PROVIDERS ====================

    /**
     * Get LocalEventsService instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsService getLocalEventsService() {
        return mLocalEventsService;
    }

    /**
     * Get LocalEventsFileService instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsFileService getLocalEventsFileService() {
        return mLocalEventsFileService;
    }

    // ==================== PRESENTATION LAYER PROVIDERS ====================

    /**
     * Get LocalEventsViewModel instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsViewModel getLocalEventsViewModel() {
        if (mLocalEventsViewModel == null) {
            synchronized (mViewModelsLock) {
                if (mLocalEventsViewModel == null) {
                    mLocalEventsViewModel = new LocalEventsViewModel(getLocalEventsService());
                    Log.d(TAG, "LocalEventsViewModel created");
                }
            }
        }
        return mLocalEventsViewModel;
    }

    /**
     * Get LocalEventsFileOperationsViewModel instance.
     * Thread-safe singleton within module scope.
     */
    @NonNull
    public LocalEventsFileOperationsViewModel getFileOperationsViewModel() {
        if (mFileOperationsViewModel == null) {
            synchronized (mViewModelsLock) {
                if (mFileOperationsViewModel == null) {
                    mFileOperationsViewModel = new LocalEventsFileOperationsViewModel(
                            getLocalEventsFileService(),
                            getLocalEventsService()
                    );
                    Log.d(TAG, "LocalEventsFileOperationsViewModel created");
                }
            }
        }
        return mFileOperationsViewModel;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Initialize all services.
     * Should be called during Activity onCreate or similar lifecycle event.
     */
    @NonNull
    public LocalEventsModule initializeServices() {
        Log.d(TAG, "Initializing LocalEvents services");

        try {
            // Initialize core service
            LocalEventsService eventsService = getLocalEventsService();
            eventsService.initialize()
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            Log.d(TAG, "LocalEventsService initialized successfully");
                        } else {
                            Log.e(TAG, "Failed to initialize LocalEventsService: " + result.getFirstError());
                        }
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Error initializing LocalEventsService", throwable);
                        return null;
                    });

            // Initialize file service when available
            try {
                LocalEventsFileService fileService = getLocalEventsFileService();
                fileService.initialize()
                        .thenAccept(result -> {
                            if (result.isSuccess()) {
                                Log.d(TAG, "LocalEventsFileService initialized successfully");
                            } else {
                                Log.e(TAG, "Failed to initialize LocalEventsFileService: " + result.getFirstError());
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Error initializing LocalEventsFileService", throwable);
                            return null;
                        });
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "LocalEventsFileService not yet implemented, skipping initialization");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during service initialization", e);
        }

        return this;
    }

    /**
     * Initialize ViewModels.
     * Should be called after service initialization.
     */
    @NonNull
    public LocalEventsModule initializeViewModels() {
        Log.d(TAG, "Initializing LocalEvents ViewModels");

        try {
            // Initialize main events ViewModel
            LocalEventsViewModel eventsViewModel = getLocalEventsViewModel();
            eventsViewModel.initialize();

            // Initialize file operations ViewModel when available
            try {
                LocalEventsFileOperationsViewModel fileOpsViewModel = getFileOperationsViewModel();
                fileOpsViewModel.initialize();
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "LocalEventsFileOperationsViewModel not available due to missing file service");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel initialization", e);
        }

        return this;
    }

    /**
     * Cleanup all components.
     * Should be called during Activity onDestroy or similar lifecycle event.
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up LocalEvents module");

        try {
            // Cleanup ViewModels
            if (mLocalEventsViewModel != null) {
                mLocalEventsViewModel.onDestroy();
                mLocalEventsViewModel = null;
            }

            if (mFileOperationsViewModel != null) {
                mFileOperationsViewModel.onDestroy();
                mFileOperationsViewModel = null;
            }

            // Shutdown services
            if (mLocalEventsService != null) {
                mLocalEventsService.shutdown();
                mLocalEventsService = null;
            }

            if (mLocalEventsFileService != null) {
                mLocalEventsFileService.shutdown();
                mLocalEventsFileService = null;
            }

            // Cleanup repository if needed
            if (mLocalEventsRepository instanceof LocalEventsRepositoryImpl) {
                ((LocalEventsRepositoryImpl) mLocalEventsRepository).shutdown();
            }

            // Clear references
            mLocalEventsUseCases = null;
            mLocalEventsRepository = null;
            mLocalEventDao = null;

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }

        Log.d(TAG, "LocalEvents module cleanup completed");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if all core components are ready.
     */
    public boolean isReady() {
        try {
            return getLocalEventsService().isReady();
        } catch (Exception e) {
            Log.w(TAG, "Error checking readiness", e);
            return false;
        }
    }

    /**
     * Check if file operations are available.
     */
    public boolean isFileOperationsAvailable() {
        try {
            return getLocalEventsFileService().isReady();
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking file operations availability", e);
            return false;
        }
    }

    /**
     * Get module status information.
     */
    @NonNull
    public ModuleStatus getModuleStatus() {
        boolean servicesReady = false;
        boolean fileServicesReady = false;
        boolean viewModelsInitialized = false;

        try {
            servicesReady = getLocalEventsService().isReady();
        } catch (Exception ignored) {}

        try {
            fileServicesReady = getLocalEventsFileService().isReady();
        } catch (Exception ignored) {}

        viewModelsInitialized = (mLocalEventsViewModel != null && mLocalEventsViewModel.isInitialized());

        return new ModuleStatus(servicesReady, fileServicesReady, viewModelsInitialized);
    }

    // ==================== INTEGRATION HELPERS ====================

    /**
     * Register LocalEventsService with CalendarServiceProvider.
     * This allows the service to be available throughout the application.
     */
    public void registerWithCalendarServiceProvider() {
        try {
            LocalEventsService eventsService = getLocalEventsService();

            // Note: This would require extending CalendarServiceProvider interface
            // to include LocalEventsService. For now, we just log the intention.
            Log.d(TAG, "LocalEventsService ready for registration with CalendarServiceProvider");

            // TODO: Implement actual registration when CalendarServiceProvider supports it
            // mCalendarServiceProvider.registerLocalEventsService(eventsService);

        } catch (Exception e) {
            Log.e(TAG, "Error registering with CalendarServiceProvider", e);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Get debug information about module state.
     */
    @NonNull
    public String getDebugInfo() {
        ModuleStatus status = getModuleStatus();

        return String.format(
                "LocalEventsModule{servicesReady=%s, fileServicesReady=%s, " +
                        "viewModelsInitialized=%s, components=[dao=%s, repo=%s, useCases=%s, " +
                        "service=%s, fileService=%s, eventsVM=%s, fileOpsVM=%s]}",
                status.servicesReady, status.fileServicesReady, status.viewModelsInitialized,
                mLocalEventDao != null, mLocalEventsRepository != null, mLocalEventsUseCases != null,
                mLocalEventsService != null, mLocalEventsFileService != null,
                mLocalEventsViewModel != null, mFileOperationsViewModel != null
        );
    }

    // ==================== INNER CLASSES ====================

    /**
     * Module status information.
     */
    public static class ModuleStatus {
        public final boolean servicesReady;
        public final boolean fileServicesReady;
        public final boolean viewModelsInitialized;

        public ModuleStatus(boolean servicesReady, boolean fileServicesReady, boolean viewModelsInitialized) {
            this.servicesReady = servicesReady;
            this.fileServicesReady = fileServicesReady;
            this.viewModelsInitialized = viewModelsInitialized;
        }

        public boolean isFullyReady() {
            return servicesReady && viewModelsInitialized;
        }

        public boolean isFullyReadyWithFileOps() {
            return servicesReady && fileServicesReady && viewModelsInitialized;
        }

        @Override
        public String toString() {
            return String.format("ModuleStatus{services=%s, fileServices=%s, viewModels=%s}",
                                 servicesReady, fileServicesReady, viewModelsInitialized);
        }
    }
}