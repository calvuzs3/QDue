package net.calvuz.qdue.ui.features.events.di;

import android.content.Context;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.adapters.EventsAdapter;
import net.calvuz.qdue.ui.features.events.components.imports.FileImportHelper;
import net.calvuz.qdue.ui.features.events.quickevents.QuickEventTemplate;

import java.util.Map;

/**
 * Dependency Injection Module for Events Feature
 * <p>
 * Provides centralized dependency management for all events-related components:
 * - EventsActivity and fragments
 * - EventsAdapter and ViewHolders
 * - Quick Events system
 * - File import/export helpers
 * - Events-specific services and DAOs
 * <p>
 * Features:
 * - Lazy initialization for performance
 * - Singleton pattern for shared components
 * - Thread-safe dependency provision
 * - Easy testing with mock overrides
 * <p>
 * Usage:
 * EventsModule module = new EventsModule(context, serviceProvider);
 * EventsAdapter adapter = module.provideEventsAdapter();
 * FileImportHelper helper = module.provideFileImportHelper();
 */
public class EventsModule {

    private static final String TAG = "EventsModule";

    // ==================== CORE DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final CoreBackupManager mBackupManager;
    private final QDueDatabase mDatabase;
    private final EventDao mEventDao;

    // ==================== CACHED INSTANCES ====================

    // Adapters (activity-scoped)
    private EventsAdapter mEventsAdapter;

    // Helpers (activity-scoped)
    private FileImportHelper mFileImportHelper;

    // Quick Events (singleton)
    private static Map<ToolbarAction, QuickEventTemplate> sQuickEventTemplates;
    private static final Object sQuickEventLock = new Object();

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor - Initialize with core dependencies
     *
     * @param context       Application context
     * @param eventsService Events service instance
     * @param userService   User service instance
     * @param backupManager Backup manager instance
     */
    public EventsModule(Context context,
                        EventsService eventsService,
                        UserService userService,
                        CoreBackupManager backupManager) {
        mContext = context.getApplicationContext();
        mEventsService = eventsService;
        mUserService = userService;
        mBackupManager = backupManager;
        mDatabase = QDueDatabase.getInstance(mContext);
        mEventDao = mDatabase.eventDao();

        Log.d(TAG, "EventsModule initialized");
    }

    // ==================== ADAPTER PROVIDERS ====================

    /**
     * Provide EventsAdapter instance (activity-scoped)
     * Creates new instance per activity to avoid memory leaks
     *
     * @return EventsAdapter configured with dependencies
     */
    public EventsAdapter provideEventsAdapter() {
        // Always create new adapter for each activity
        mEventsAdapter = new EventsAdapter(
                mContext,
                mEventsService,
                mBackupManager
        );

        Log.d(TAG, "Created new EventsAdapter instance");
        return mEventsAdapter;
    }

    /**
     * Get current adapter instance (if exists)
     *
     * @return Current adapter or null if not created
     */
    public EventsAdapter getCurrentEventsAdapter() {
        return mEventsAdapter;
    }

    // ==================== COMPONENT PROVIDERS ====================

    /**
     * Provide FileImportHelper instance (activity-scoped)
     *
     * @return FileImportHelper configured with dependencies
     */
    public FileImportHelper provideFileImportHelper() {
        if (mFileImportHelper == null) {
            mFileImportHelper = new FileImportHelper(
                    mContext,
                    mEventsService,
                    mBackupManager
            );
            Log.d(TAG, "Created FileImportHelper instance");
        }
        return mFileImportHelper;
    }

    // ==================== QUICK EVENTS PROVIDERS ====================

    /**
     * Provide Quick Event Templates (singleton)
     * Thread-safe singleton initialization
     *
     * @return Map of ToolbarAction to QuickEventTemplate
     */
    public Map<ToolbarAction, QuickEventTemplate> provideQuickEventTemplates() {
        if (sQuickEventTemplates == null) {
            synchronized (sQuickEventLock) {
                if (sQuickEventTemplates == null) {
                    sQuickEventTemplates = createQuickEventTemplates();
                    Log.d(TAG, "Created Quick Event Templates singleton");
                }
            }
        }
        return sQuickEventTemplates;
    }

    /**
     * Create Quick Event Templates map
     *
     * @return Configured templates map
     */
    private Map<ToolbarAction, QuickEventTemplate> createQuickEventTemplates() {
        return QuickEventTemplate.getAllTemplates();
    }

    // ==================== SERVICE PROVIDERS ====================

    /**
     * Provide EventsService instance
     *
     * @return EventsService configured instance
     */
    public EventsService provideEventsService() {
        return mEventsService;
    }

    /**
     * Provide UserService instance
     *
     * @return UserService configured instance
     */
    public UserService provideUserService() {
        return mUserService;
    }

    /**
     * Provide CoreBackupManager instance
     *
     * @return CoreBackupManager configured instance
     */
    public CoreBackupManager provideBackupManager() {
        return mBackupManager;
    }

    /**
     * Provide EventDao instance
     *
     * @return EventDao from database
     */
    public EventDao provideEventDao() {
        return mEventDao;
    }

    // ==================== TESTING SUPPORT ====================

    /**
     * Clear cached instances for testing
     * Should only be used in test environments
     */
    public void clearCacheForTesting() {
        mEventsAdapter = null;
        mFileImportHelper = null;

        synchronized (sQuickEventLock) {
            sQuickEventTemplates = null;
        }

        Log.w(TAG, "Cleared all cached instances for testing");
    }

    /**
     * Check if all dependencies are properly initialized
     *
     * @return true if all core dependencies are available
     */
    public boolean areDependenciesReady() {
        return mContext != null &&
                mEventsService != null &&
                mUserService != null &&
                mBackupManager != null &&
                mDatabase != null &&
                mEventDao != null;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when activity is destroyed
     * Call this from Activity.onDestroy()
     */
    public void onDestroy() {
        // Clean up adapter
        if (mEventsAdapter != null) {
            mEventsAdapter.onDestroy();
            mEventsAdapter = null;
        }

        // Clean up file import helper
        if (mFileImportHelper != null) {
            mFileImportHelper.clearPendingCallbacks();
            mFileImportHelper = null;
        }

        Log.d(TAG, "EventsModule cleaned up");
    }

    // ==================== BUILDER PATTERN (OPTIONAL) ====================

    /**
     * Builder for EventsModule construction
     * Provides fluent API for optional dependencies
     */
    public static class Builder {
        private Context context;
        private EventsService eventsService;
        private UserService userService;
        private CoreBackupManager backupManager;

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setEventsService(EventsService eventsService) {
            this.eventsService = eventsService;
            return this;
        }

        public Builder setUserService(UserService userService) {
            this.userService = userService;
            return this;
        }

        public Builder setBackupManager(CoreBackupManager backupManager) {
            this.backupManager = backupManager;
            return this;
        }

        public EventsModule build() {
            if (context == null || eventsService == null ||
                    userService == null || backupManager == null) {
                throw new IllegalStateException(
                        "All dependencies must be provided to EventsModule.Builder");
            }

            return new EventsModule(context, eventsService, userService, backupManager);
        }
    }
}