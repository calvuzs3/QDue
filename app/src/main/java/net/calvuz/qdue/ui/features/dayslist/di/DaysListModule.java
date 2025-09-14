package net.calvuz.qdue.ui.features.dayslist.di;

import android.content.Context;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.dayslist.adapters.DaysListAdapter;
import net.calvuz.qdue.ui.features.dayslist.components.DaysListEventsPreview;
import net.calvuz.qdue.ui.features.dayslist.components.DaysListEventsBottomSheet;

import java.util.List;

/**
 * Dependency Injection Module for DaysList Feature
 * <p>
 * Provides centralized dependency management for DaysList components:
 * - DayslistViewFragment
 * - DaysListAdapter
 * - DaysListEventsPreview components
 * - Days list specific services
 * <p>
 * Features:
 * - Activity-scoped adapter instances
 * - Shared events preview components
 * - Calendar data manager integration
 * - Clean separation from other features
 * <p>
 * Usage:
 * DaysListModule module = new DaysListModule(context, eventsService, userService);
 * DaysListAdapter adapter = module.provideDaysListAdapter(listener);
 * DaysListEventsPreview preview = module.provideEventsPreview();
 */
public class DaysListModule {

    private static final String TAG = "DaysListModule";

    // ==================== CORE DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final QDueDatabase mDatabase;
    private final CalendarDataManager mDataManager;

    // ==================== CACHED INSTANCES ====================

    // Adapter (activity-scoped)
    private DaysListAdapter mDaysListAdapter;

    // Events preview components (fragment-scoped)
    private DaysListEventsPreview mEventsPreview;
    private DaysListEventsBottomSheet mEventsBottomSheet;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor - Initialize with core dependencies
     *
     * @param context Application context
     * @param eventsService Events service instance
     * @param userService User service instance
     */
    public DaysListModule(Context context,
                          EventsService eventsService,
                          UserService userService) {
        mContext = context.getApplicationContext();
        mEventsService = eventsService;
        mUserService = userService;
        mDatabase = QDueDatabase.getInstance(mContext);
        mDataManager = CalendarDataManager.getInstance();

        Log.d(TAG, "DaysListModule initialized");
    }

    // ==================== ADAPTER PROVIDERS ====================

    /**
     * Provide DaysListAdapter instance (activity-scoped)
     * Creates new instance per activity to avoid memory leaks
     *
     * @param context Fragment context
     * @param items List of view items
     * @param userHalfTeam User's half team
     * @param numShifts Number of shifts per day
     * @return DaysListAdapter configured with dependencies
     */
    public DaysListAdapter provideDaysListAdapter(Context context,
                                                  List<SharedViewModels.ViewItem> items,
                                                  HalfTeam userHalfTeam,
                                                  int numShifts) {
        // Always create new adapter for each activity/fragment
        mDaysListAdapter = new DaysListAdapter(
                context,
                items,
                userHalfTeam,
                numShifts
        );

        Log.d(TAG, "Created new DaysListAdapter instance");
        return mDaysListAdapter;
    }

    /**
     * Get current adapter instance (if exists)
     *
     * @return Current adapter or null if not created
     */
    public DaysListAdapter getCurrentDaysListAdapter() {
        return mDaysListAdapter;
    }

    // ==================== COMPONENT PROVIDERS ====================

    /**
     * Provide DaysListEventsPreview instance (fragment-scoped)
     *
     * @param context Fragment context
     * @return DaysListEventsPreview configured with dependencies
     */
    public DaysListEventsPreview provideEventsPreview(Context context) {
        if (mEventsPreview == null) {
            mEventsPreview = new DaysListEventsPreview(context);
            Log.d(TAG, "Created DaysListEventsPreview instance");
        }
        return mEventsPreview;
    }

    /**
     * Provide DaysListEventsBottomSheet instance (fragment-scoped)
     *
     * @param context Fragment context
     * @return DaysListEventsBottomSheet configured with dependencies
     */
    public DaysListEventsBottomSheet provideEventsBottomSheet(Context context) {
        if (mEventsBottomSheet == null) {
            mEventsBottomSheet = new DaysListEventsBottomSheet(context);
            Log.d(TAG, "Created DaysListEventsBottomSheet instance");
        }
        return mEventsBottomSheet;
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
     * Provide CalendarDataManager instance
     *
     * @return CalendarDataManager singleton instance
     */
    public CalendarDataManager provideCalendarDataManager() {
        return mDataManager;
    }

    /**
     * Provide QDueDatabase instance
     *
     * @return QDueDatabase singleton instance
     */
    public QDueDatabase provideDatabase() {
        return mDatabase;
    }

    // ==================== TESTING SUPPORT ====================

    /**
     * Clear cached instances for testing
     * Should only be used in test environments
     */
    public void clearCacheForTesting() {
        mDaysListAdapter = null;
        mEventsPreview = null;
        mEventsBottomSheet = null;

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
                mDatabase != null &&
                mDataManager != null;
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when fragment is destroyed
     * Call this from Fragment.onDestroy()
     */
    public void onDestroy() {
        // Clean up adapter
        if (mDaysListAdapter != null) {
            mDaysListAdapter.onDestroy();
            mDaysListAdapter = null;
        }

        // Clean up events preview components
        if (mEventsPreview != null) {
            mEventsPreview.onDestroy();
            mEventsPreview = null;
        }

        if (mEventsBottomSheet != null) {
            mEventsBottomSheet.onDestroy();
            mEventsBottomSheet = null;
        }

        Log.d(TAG, "DaysListModule cleaned up");
    }

    // ==================== CONFIGURATION ====================

    /**
     * Configure adapter with events integration
     * Call this after adapter creation to enable events features
     */
    public void configureEventsIntegration() {
        if (mDaysListAdapter != null && mEventsService != null) {
            // Configure adapter with events service
//            mDaysListAdapter.setEventsService(mEventsService);

            Log.d(TAG, "Events integration configured for DaysListAdapter");
        } else {
            Log.w(TAG, "Cannot configure events integration - missing dependencies");
        }
    }

    /**
     * Get column count for DaysList layout
     * Always returns 1 for single-column list behavior
     *
     * @return Column count (always 1 for DaysList)
     */
    public int getColumnCount() {
        return 1; // DaysList always uses single column
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create module instance with default configuration
     * Convenience method for common use cases
     *
     * @param context Application context
     * @param eventsService Events service instance
     * @param userService User service instance
     * @return Configured DaysListModule
     */
    public static DaysListModule createDefault(Context context,
                                               EventsService eventsService,
                                               UserService userService) {
        DaysListModule module = new DaysListModule(context, eventsService, userService);

        // Verify dependencies
        if (!module.areDependenciesReady()) {
            throw new IllegalStateException("DaysListModule dependencies not ready");
        }

        return module;
    }

    /**
     * Get debug information about module state
     *
     * @return String with module state information
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("DaysListModule Debug Info:\n");
        info.append("- Dependencies ready: ").append(areDependenciesReady()).append("\n");
        info.append("- DaysListAdapter: ").append(mDaysListAdapter != null ? "created" : "null").append("\n");
        info.append("- EventsPreview: ").append(mEventsPreview != null ? "created" : "null").append("\n");
        info.append("- EventsBottomSheet: ").append(mEventsBottomSheet != null ? "created" : "null").append("\n");
        info.append("- Column count: ").append(getColumnCount()).append("\n");

        return info.toString();
    }
}