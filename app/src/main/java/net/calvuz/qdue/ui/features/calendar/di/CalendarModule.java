package net.calvuz.qdue.ui.features.calendar.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.ui.features.calendar.adapters.CalendarPagerAdapter;
import net.calvuz.qdue.ui.features.calendar.adapters.MonthViewAdapter;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarDataProvider;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarEventListener;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarNavigationListener;
import net.calvuz.qdue.ui.features.calendar.providers.CalendarDataProviderImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CalendarModule - Dependency injection module for calendar feature.
 *
 * <p>Provides centralized dependency management for all calendar-related components
 * following the established DI patterns in the application. Handles creation and
 * lifecycle management of calendar adapters, data providers, and services.</p>
 *
 * <p>Module Features:</p>
 * <ul>
 *   <li>Lazy initialization of expensive components</li>
 *   <li>Proper lifecycle management with cleanup</li>
 *   <li>Thread-safe component creation</li>
 *   <li>Integration with existing service layer</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarModule {

    private static final String TAG = "CalendarModule";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final EventsService mEventsService;
    private final UserService mUserService;
    private final WorkScheduleService mWorkScheduleService;

    // ==================== CACHED INSTANCES ====================

    private CalendarDataProvider mCalendarDataProvider;
    private CalendarPagerAdapter mCalendarPagerAdapter;
    private ExecutorService mExecutorService;

    // Listeners (weak references handled by components)
    private CalendarEventListener mCalendarEventListener;
    private CalendarNavigationListener mCalendarNavigationListener;

    // Configuration
    private Long mCurrentUserId;
    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates CalendarModule with required dependencies.
     *
     * @param context Application context
     * @param eventsService Events service for event data
     * @param userService User service for user context
     * @param workScheduleService Work schedule service for shift data
     */
    public CalendarModule(@NonNull Context context,
                          @NonNull EventsService eventsService,
                          @NonNull UserService userService,
                          @NonNull WorkScheduleService workScheduleService) {
        this.mContext = context.getApplicationContext();
        this.mEventsService = eventsService;
        this.mUserService = userService;
        this.mWorkScheduleService = workScheduleService;

        Log.d(TAG, "CalendarModule created");
    }

    // ==================== PROVIDER METHODS ====================

    /**
     * Provides CalendarDataProvider instance with lazy initialization.
     *
     * @return Configured CalendarDataProvider
     */
    @NonNull
    public synchronized CalendarDataProvider provideCalendarDataProvider() {
        if (mCalendarDataProvider == null && !mIsDestroyed) {
            mCalendarDataProvider = new CalendarDataProviderImpl(
                    mContext,
                    mEventsService,
                    mUserService,
                    mWorkScheduleService,
                    getExecutorService()
            );

            if (mCurrentUserId != null) {
                mCalendarDataProvider.setCurrentUser(mCurrentUserId);
            }

            Log.d(TAG, "CalendarDataProvider created");
        }
        return mCalendarDataProvider;
    }

    /**
     * Provides CalendarPagerAdapter for ViewPager2 with month navigation.
     *
     * @param eventListener Listener for calendar events
     * @param navigationListener Listener for navigation events
     * @return Configured CalendarPagerAdapter
     */
    @NonNull
    public synchronized CalendarPagerAdapter provideCalendarPagerAdapter(
            @Nullable CalendarEventListener eventListener,
            @Nullable CalendarNavigationListener navigationListener) {

        if (mCalendarPagerAdapter == null && !mIsDestroyed) {
            mCalendarEventListener = eventListener;
            mCalendarNavigationListener = navigationListener;

            mCalendarPagerAdapter = new CalendarPagerAdapter(
                    provideCalendarDataProvider(),
                    mCalendarEventListener,
                    mCalendarNavigationListener
            );

            Log.d(TAG, "CalendarPagerAdapter created");
        }
        return mCalendarPagerAdapter;
    }

    /**
     * Provides MonthViewAdapter for individual month display.
     *
     * @param yearMonth Month to display
     * @param eventListener Listener for day click events
     * @return Configured MonthViewAdapter
     */
    @NonNull
    public MonthViewAdapter provideMonthViewAdapter(@NonNull YearMonth yearMonth,
                                                    @Nullable CalendarEventListener eventListener) {
        return new MonthViewAdapter(
                mContext,
                yearMonth,
                provideCalendarDataProvider(),
                eventListener
        );
    }

    /**
     * Provides ExecutorService for background operations.
     *
     * @return Shared ExecutorService instance
     */
    @NonNull
    private synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null && !mIsDestroyed) {
            mExecutorService = Executors.newFixedThreadPool(3, r -> {
                Thread thread = new Thread(r, "CalendarModule-Worker");
                thread.setDaemon(true);
                return thread;
            });

            Log.d(TAG, "ExecutorService created");
        }
        return mExecutorService;
    }

    // ==================== CONFIGURATION METHODS ====================

    /**
     * Set current user for data filtering.
     *
     * @param userId User ID to filter data by (null for current user)
     */
    public void setCurrentUser(@Nullable Long userId) {
        mCurrentUserId = userId;

        if (mCalendarDataProvider != null) {
            mCalendarDataProvider.setCurrentUser(userId);
        }

        Log.d(TAG, "Current user set to: " + userId);
    }

    /**
     * Update event listener for existing adapter.
     *
     * @param eventListener New event listener
     */
    public void updateEventListener(@Nullable CalendarEventListener eventListener) {
        mCalendarEventListener = eventListener;

        if (mCalendarPagerAdapter != null) {
            mCalendarPagerAdapter.updateEventListener(eventListener);
        }
    }

    /**
     * Update navigation listener for existing adapter.
     *
     * @param navigationListener New navigation listener
     */
    public void updateNavigationListener(@Nullable CalendarNavigationListener navigationListener) {
        mCalendarNavigationListener = navigationListener;

        if (mCalendarPagerAdapter != null) {
            mCalendarPagerAdapter.updateNavigationListener(navigationListener);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Refresh calendar data for specific date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     */
    public void refreshCalendarData(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        if (mCalendarDataProvider != null && !mIsDestroyed) {
            mCalendarDataProvider.refreshData(startDate, endDate);
            Log.d(TAG, "Calendar data refresh requested for " + startDate + " to " + endDate);
        }
    }

    /**
     * Refresh calendar data for specific month.
     *
     * @param yearMonth Month to refresh
     */
    public void refreshCalendarData(@NonNull YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        refreshCalendarData(startDate, endDate);
    }

    /**
     * Clear cached data and force reload.
     */
    public void clearCache() {
        if (mCalendarDataProvider != null && !mIsDestroyed) {
            mCalendarDataProvider.clearCache();
            Log.d(TAG, "Calendar cache cleared");
        }
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Check if all dependencies are ready.
     *
     * @return true if module is properly initialized
     */
    public boolean areDependenciesReady() {
        return mContext != null &&
                mEventsService != null &&
                mUserService != null &&
                mWorkScheduleService != null &&
                !mIsDestroyed;
    }

    /**
     * Cleanup all resources and stop background operations.
     * Should be called when the module is no longer needed.
     */
    public void onDestroy() {
        Log.d(TAG, "Destroying CalendarModule");

        mIsDestroyed = true;

        // Cleanup data provider
        if (mCalendarDataProvider != null) {
            mCalendarDataProvider.destroy();
            mCalendarDataProvider = null;
        }

        // Cleanup adapter
        if (mCalendarPagerAdapter != null) {
            mCalendarPagerAdapter.onDestroy();
            mCalendarPagerAdapter = null;
        }

        // Shutdown executor service
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }

        // Clear listeners
        mCalendarEventListener = null;
        mCalendarNavigationListener = null;

        Log.d(TAG, "CalendarModule destroyed");
    }

    // ==================== DEBUGGING METHODS ====================

    /**
     * Get module status for debugging.
     *
     * @return Status string with component states
     */
    public String getModuleStatus() {
        return "CalendarModule{" +
                "destroyed=" + mIsDestroyed +
                ", dataProvider=" + (mCalendarDataProvider != null) +
                ", pagerAdapter=" + (mCalendarPagerAdapter != null) +
                ", executorService=" + (mExecutorService != null && !mExecutorService.isShutdown()) +
                ", currentUserId=" + mCurrentUserId +
                '}';
    }
}