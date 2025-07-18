package net.calvuz.qdue.ui.features.calendar.di;

import android.content.Context;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.calendar.adapters.CalendarAdapter;
import net.calvuz.qdue.ui.features.calendar.components.CalendarEventsBottomSheet;
import net.calvuz.qdue.quattrodue.models.HalfTeam;

import java.util.List;

/**
 * Dependency Injection Module for Calendar Feature
 * <p>
 * Provides centralized dependency management for Calendar components:
 * - CalendarViewFragment
 * - CalendarAdapter
 * - CalendarEventsBottomSheet
 * - Calendar-specific services and configurations
 * <p>
 * Features:
 * - Activity-scoped adapter instances
 * - 7-column grid layout configuration
 * - Calendar-specific events preview
 * - Optimized for calendar grid display
 * <p>
 * Usage:
 * CalendarModule module = new CalendarModule(context, eventsService, userService);
 * CalendarAdapter adapter = module.provideCalendarAdapter(items, userTeam);
 * CalendarEventsBottomSheet bottomSheet = module.provideEventsBottomSheet();
 */
public class CalendarModule {

    private static final String TAG = "CalendarModule";

    // ==================== CORE DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final QDueDatabase mDatabase;
    private final CalendarDataManager mDataManager;

    // ==================== CACHED INSTANCES ====================

    // Adapter (activity-scoped)
    private CalendarAdapter mCalendarAdapter;

    // Events preview components (fragment-scoped)
    private CalendarEventsBottomSheet mEventsBottomSheet;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor - Initialize with core dependencies
     *
     * @param context Application context
     * @param eventsService Events service instance
     * @param userService User service instance
     */
    public CalendarModule(Context context,
                          EventsService eventsService,
                          UserService userService) {
        mContext = context.getApplicationContext();
        mEventsService = eventsService;
        mUserService = userService;
        mDatabase = QDueDatabase.getInstance(mContext);
        mDataManager = CalendarDataManager.getInstance();

        Log.d(TAG, "CalendarModule initialized");
    }

    // ==================== ADAPTER PROVIDERS ====================

    /**
     * Provide CalendarAdapter instance (activity-scoped)
     * Creates new instance per activity to avoid memory leaks
     *
     * @param context Fragment context
     * @param items List of view items for calendar grid
     * @param userHalfTeam User's half team
     * @return CalendarAdapter configured with dependencies
     */
    public CalendarAdapter provideCalendarAdapter(Context context,
                                                  EventsService eventsService,
                                                  List<SharedViewModels.ViewItem> items,
                                                  HalfTeam userHalfTeam) {
        // Always create new adapter for each activity/fragment
        mCalendarAdapter = new CalendarAdapter(
                context,
                eventsService,
                items,
                userHalfTeam
        );

        Log.d(TAG, "Created new CalendarAdapter instance");
        return mCalendarAdapter;
    }

    /**
     * Get current adapter instance (if exists)
     *
     * @return Current adapter or null if not created
     */
    public CalendarAdapter getCurrentCalendarAdapter() {
        return mCalendarAdapter;
    }

    // ==================== COMPONENT PROVIDERS ====================

    /**
     * Provide CalendarEventsBottomSheet instance (fragment-scoped)
     *
     * @param context Fragment context
     * @return CalendarEventsBottomSheet configured with dependencies
     */
    public CalendarEventsBottomSheet provideEventsBottomSheet(Context context) {
        if (mEventsBottomSheet == null) {
            mEventsBottomSheet = new CalendarEventsBottomSheet(context);
            Log.d(TAG, "Created CalendarEventsBottomSheet instance");
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

    // ==================== CALENDAR-SPECIFIC CONFIGURATION ====================

    /**
     * Get column count for Calendar layout
     * Always returns 7 for calendar grid (Sunday-Saturday)
     *
     * @return Column count (always 7 for Calendar)
     */
    public int getColumnCount() {
        return 7; // Calendar always uses 7 columns (week days)
    }

    /**
     * Check if calendar should show week numbers
     * Can be configured based on user preferences
     *
     * @return true if week numbers should be shown
     */
    public boolean shouldShowWeekNumbers() {
        // TODO: Get from user preferences or configuration
        return false; // Default: don't show week numbers
    }

    /**
     * Get first day of week for calendar
     * Can be configured based on user locale/preferences
     *
     * @return First day of week (1 = Monday, 7 = Sunday)
     */
    public int getFirstDayOfWeek() {
        // TODO: Get from user preferences or locale
        return 1; // Default: Monday as first day of week
    }

    /**
     * Configure adapter with calendar-specific settings
     * Call this after adapter creation to enable calendar features
     */
    public void configureCalendarFeatures() {
        if (mCalendarAdapter != null && mEventsService != null) {
            // Configure adapter with events service
            mCalendarAdapter.setEventsService(mEventsService);

            // Set calendar-specific configurations
            // e.g., cell height, event indicators, etc.

            Log.d(TAG, "Calendar features configured for CalendarAdapter");
        } else {
            Log.w(TAG, "Cannot configure calendar features - missing dependencies");
        }
    }

    // ==================== TESTING SUPPORT ====================

    /**
     * Clear cached instances for testing
     * Should only be used in test environments
     */
    public void clearCacheForTesting() {
        mCalendarAdapter = null;
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
        if (mCalendarAdapter != null) {
            mCalendarAdapter.onDestroy();
            mCalendarAdapter = null;
        }

        // Clean up events bottom sheet
        if (mEventsBottomSheet != null) {
            mEventsBottomSheet.onDestroy();
            mEventsBottomSheet = null;
        }

        Log.d(TAG, "CalendarModule cleaned up");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create module instance with default configuration
     * Convenience method for common use cases
     *
     * @param context Application context
     * @param eventsService Events service instance
     * @param userService User service instance
     * @return Configured CalendarModule
     */
    public static CalendarModule createDefault(Context context,
                                               EventsService eventsService,
                                               UserService userService) {
        CalendarModule module = new CalendarModule(context, eventsService, userService);

        // Verify dependencies
        if (!module.areDependenciesReady()) {
            throw new IllegalStateException("CalendarModule dependencies not ready");
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
        info.append("CalendarModule Debug Info:\n");
        info.append("- Dependencies ready: ").append(areDependenciesReady()).append("\n");
        info.append("- CalendarAdapter: ").append(mCalendarAdapter != null ? "created" : "null").append("\n");
        info.append("- EventsBottomSheet: ").append(mEventsBottomSheet != null ? "created" : "null").append("\n");
        info.append("- Column count: ").append(getColumnCount()).append("\n");
        info.append("- First day of week: ").append(getFirstDayOfWeek()).append("\n");
        info.append("- Show week numbers: ").append(shouldShowWeekNumbers()).append("\n");

        return info.toString();
    }

    // ==================== CALENDAR LAYOUT HELPERS ====================

    /**
     * Calculate optimal cell height for calendar grid
     * Based on screen size and available space
     *
     * @param availableHeight Available height for calendar grid
     * @param numberOfWeeks Number of weeks to display
     * @return Optimal cell height in pixels
     */
    public int calculateOptimalCellHeight(int availableHeight, int numberOfWeeks) {
        if (numberOfWeeks <= 0) numberOfWeeks = 6; // Default to 6 weeks

        // Reserve space for month header and padding
        int reservedSpace = 120; // dp converted to pixels would be better
        int availableForCells = Math.max(availableHeight - reservedSpace, 200);

        int cellHeight = availableForCells / numberOfWeeks;

        // Ensure minimum and maximum cell heights
        int minCellHeight = 48; // Minimum touch target
        int maxCellHeight = 80; // Maximum for good visual appearance

        cellHeight = Math.max(minCellHeight, Math.min(maxCellHeight, cellHeight));

        Log.d(TAG, "Calculated optimal cell height: " + cellHeight + "px for " + numberOfWeeks + " weeks");
        return cellHeight;
    }

    /**
     * Check if current layout is suitable for calendar view
     * Based on screen orientation and size
     *
     * @return true if layout is suitable for calendar grid
     */
    public boolean isLayoutSuitableForCalendar() {
        // TODO: Check screen size, orientation, density
        // For now, assume calendar is always suitable
        return true;
    }
}