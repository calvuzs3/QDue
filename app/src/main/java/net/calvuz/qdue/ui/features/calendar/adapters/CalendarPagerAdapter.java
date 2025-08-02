package net.calvuz.qdue.ui.features.calendar.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarDataProvider;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarEventListener;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarNavigationListener;
import net.calvuz.qdue.ui.features.calendar.components.MonthViewHolder;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.YearMonth;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CalendarPagerAdapter - RecyclerView.Adapter for ViewPager2 month navigation.
 *
 * <p>Provides infinite scrolling through months with efficient memory management.
 * Each page displays a monthly calendar grid with work schedules and events.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Infinite scrolling with intelligent position mapping</li>
 *   <li>Memory efficient with ViewHolder recycling</li>
 *   <li>Cached month data for smooth navigation</li>
 *   <li>Integration with CalendarDataProvider for data loading</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarPagerAdapter extends RecyclerView.Adapter<MonthViewHolder> {

    private static final String TAG = "CalendarPagerAdapter";

    // ViewPager2 configuration for infinite scroll
    private static final int TOTAL_PAGES = 10000; // Large number for "infinite" scroll
    private static final int CENTER_POSITION = TOTAL_PAGES / 2;

    // Base month for position calculations
    private static final YearMonth BASE_MONTH = YearMonth.of(2020, 1);

    // ==================== DEPENDENCIES ====================

    private final CalendarDataProvider mDataProvider;
    private CalendarEventListener mEventListener;
    private CalendarNavigationListener mNavigationListener;

    // ==================== CACHED DATA ====================

    // Cache for month holders to maintain state
    private final ConcurrentMap<Integer, MonthViewHolder> mActiveHolders = new ConcurrentHashMap<>();

    // Track which months have been notified for updates
    private final ConcurrentMap<YearMonth, Boolean> mNotificationTracker = new ConcurrentHashMap<>();

    // ==================== STATE ====================

    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates CalendarPagerAdapter with required dependencies.
     *
     * @param dataProvider Provider for calendar data
     * @param eventListener Listener for calendar events (can be null)
     * @param navigationListener Listener for navigation events (can be null)
     */
    public CalendarPagerAdapter(@NonNull CalendarDataProvider dataProvider,
                                @Nullable CalendarEventListener eventListener,
                                @Nullable CalendarNavigationListener navigationListener) {
        this.mDataProvider = dataProvider;
        this.mEventListener = eventListener;
        this.mNavigationListener = navigationListener;

        // Enable stable IDs for better performance
        setHasStableIds(true);

        Log.d(TAG, "CalendarPagerAdapter created");
    }

    // ==================== RECYCLERVIEW.ADAPTER METHODS ====================

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_month2, parent, false);

        MonthViewHolder holder = new MonthViewHolder(view, mDataProvider, mEventListener);

        Log.v(TAG, "MonthViewHolder created");
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        if (mIsDestroyed) return;

        YearMonth month = getMonthForPosition(position);

        // Store holder reference for updates
        mActiveHolders.put(position, holder);

        // Bind month data
        holder.bindMonth(month);

        Log.v(TAG, "Month bound: " + month + " at position " + position);
    }

    @Override
    public void onViewRecycled(@NonNull MonthViewHolder holder) {
        super.onViewRecycled(holder);

        // Remove from active holders when recycled
        mActiveHolders.values().remove(holder);

        // Cleanup holder
        holder.cleanup();

        Log.v(TAG, "MonthViewHolder recycled");
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }

    @Override
    public long getItemId(int position) {
        // Use month hash as stable ID
        YearMonth month = getMonthForPosition(position);
        return month.hashCode();
    }

    // ==================== POSITION CALCULATION METHODS ====================

    /**
     * Calculate YearMonth for ViewPager2 position.
     *
     * @param position ViewPager2 position
     * @return YearMonth for that position
     */
    @NonNull
    public YearMonth getMonthForPosition(int position) {
        long offset = position - CENTER_POSITION;
        return BASE_MONTH.plusMonths(offset);
    }

    /**
     * Calculate ViewPager2 position for YearMonth.
     *
     * @param month Target month
     * @return Position for ViewPager2
     */
    public int getPositionForMonth(@NonNull YearMonth month) {
        long monthsBetween = BASE_MONTH.until(month, java.time.temporal.ChronoUnit.MONTHS);
        return CENTER_POSITION + (int) monthsBetween;
    }

    // ==================== UPDATE METHODS ====================

    /**
     * Notify that data for a specific month has changed.
     *
     * @param month Month that has new data
     */
    public void notifyMonthDataChanged(@NonNull YearMonth month) {
        if (mIsDestroyed) return;

        // Prevent duplicate notifications
        if (mNotificationTracker.putIfAbsent(month, true) != null) {
            return; // Already notified recently
        }

        // Find and update active holders for this month
        for (ConcurrentMap.Entry<Integer, MonthViewHolder> entry : mActiveHolders.entrySet()) {
            Integer position = entry.getKey();
            MonthViewHolder holder = entry.getValue();

            if (month.equals(getMonthForPosition(position))) {
                holder.refreshData();
                Log.d(TAG, "Refreshed data for month: " + month);
            }
        }

        // Clear notification tracker after a delay to allow for batched updates
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> mNotificationTracker.remove(month), 1000);
    }

    /**
     * Refresh all currently visible months.
     */
    public void refreshAllVisibleMonths() {
        if (mIsDestroyed) return;

        Log.d(TAG, "Refreshing all visible months");

        for (MonthViewHolder holder : mActiveHolders.values()) {
            if (holder != null) {
                holder.refreshData();
            }
        }
    }

    // ==================== LISTENER MANAGEMENT ====================

    /**
     * Update event listener for existing holders.
     *
     * @param eventListener New event listener
     */
    public void updateEventListener(@Nullable CalendarEventListener eventListener) {
        mEventListener = eventListener;

        // Update all active holders
        for (MonthViewHolder holder : mActiveHolders.values()) {
            if (holder != null) {
                holder.setEventListener(eventListener);
            }
        }

        Log.d(TAG, "Event listener updated");
    }

    /**
     * Update navigation listener.
     *
     * @param navigationListener New navigation listener
     */
    public void updateNavigationListener(@Nullable CalendarNavigationListener navigationListener) {
        mNavigationListener = navigationListener;
        Log.d(TAG, "Navigation listener updated");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get current month for center position.
     * Useful for determining which month is currently displayed.
     *
     * @return YearMonth for center position
     */
    @NonNull
    public YearMonth getCenterMonth() {
        return getMonthForPosition(CENTER_POSITION);
    }

    /**
     * Check if adapter contains data for specific month.
     *
     * @param month Month to check
     * @return true if month is within adapter range
     */
    public boolean containsMonth(@NonNull YearMonth month) {
        int position = getPositionForMonth(month);
        return position >= 0 && position < getItemCount();
    }

    /**
     * Get months currently visible in ViewPager2.
     *
     * @return Array of currently visible months (up to 3)
     */
    @NonNull
    public YearMonth[] getVisibleMonths() {
        java.util.List<YearMonth> months = new java.util.ArrayList<>();

        for (Integer position : mActiveHolders.keySet()) {
            months.add(getMonthForPosition(position));
        }

        return months.toArray(new YearMonth[0]);
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Cleanup adapter and all active holders.
     * Should be called when adapter is no longer needed.
     */
    public void onDestroy() {
        Log.d(TAG, "Destroying CalendarPagerAdapter");

        mIsDestroyed = true;

        // Cleanup all active holders
        for (MonthViewHolder holder : mActiveHolders.values()) {
            if (holder != null) {
                holder.cleanup();
            }
        }
        mActiveHolders.clear();

        // Clear notification tracker
        mNotificationTracker.clear();

        // Clear listeners
        mEventListener = null;
        mNavigationListener = null;

        Log.d(TAG, "CalendarPagerAdapter destroyed");
    }

    // ==================== DEBUGGING METHODS ====================

    /**
     * Get adapter status for debugging.
     *
     * @return Status string with active holders and state
     */
    public String getAdapterStatus() {
        return "CalendarPagerAdapter{" +
                "destroyed=" + mIsDestroyed +
                ", activeHolders=" + mActiveHolders.size() +
                ", centerMonth=" + getCenterMonth() +
                ", hasEventListener=" + (mEventListener != null) +
                ", hasNavigationListener=" + (mNavigationListener != null) +
                '}';
    }

    /**
     * Log current state for debugging.
     */
    public void logCurrentState() {
        Log.d(TAG, "Current adapter state:");
        Log.d(TAG, "- Active holders: " + mActiveHolders.size());
        Log.d(TAG, "- Center month: " + getCenterMonth());
        Log.d(TAG, "- Visible months: " + java.util.Arrays.toString(getVisibleMonths()));
        Log.d(TAG, "- Is destroyed: " + mIsDestroyed);
    }
}