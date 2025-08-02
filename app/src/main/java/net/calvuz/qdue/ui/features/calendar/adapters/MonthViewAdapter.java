package net.calvuz.qdue.ui.features.calendar.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.calendar.components.CalendarDayViewHolder;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarDataProvider;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarEventListener;
import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * MonthViewAdapter - RecyclerView.Adapter for monthly calendar grid.
 *
 * <p>Displays a month as a 7x6 grid (42 cells) representing weeks and days.
 * Includes previous/next month overflow days for complete weekly view.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Fixed 42-item grid for consistent UI layout</li>
 *   <li>Work schedule integration with shift color backgrounds</li>
 *   <li>Event indicators with EventType icons</li>
 *   <li>Loading states and skeleton views for individual days</li>
 *   <li>Click and long-click handling for day interactions</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class MonthViewAdapter extends RecyclerView.Adapter<CalendarDayViewHolder> {

    private static final String TAG = "MonthViewAdapter";

    // Grid configuration
    private static final int GRID_SIZE = 42; // 6 weeks × 7 days = 42 cells
    private static final int DAYS_PER_WEEK = 7;

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarDataProvider mDataProvider;
    private CalendarEventListener mEventListener;

    // ==================== DATA ====================

    private YearMonth mCurrentMonth;
    private final List<CalendarDay> mCalendarDays;
    private final List<LocalDate> mGridDates; // All 42 dates in grid

    // ==================== STATE ====================

    private boolean mIsDestroyed = false;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates MonthViewAdapter for specific month.
     *
     * @param context Android context
     * @param month Month to display
     * @param dataProvider Provider for calendar data
     * @param eventListener Listener for day events (can be null)
     */
    public MonthViewAdapter(@NonNull Context context,
                            @NonNull YearMonth month,
                            @NonNull CalendarDataProvider dataProvider,
                            @Nullable CalendarEventListener eventListener) {
        this.mContext = context.getApplicationContext();
        this.mCurrentMonth = month;
        this.mDataProvider = dataProvider;
        this.mEventListener = eventListener;

        this.mCalendarDays = new ArrayList<>(GRID_SIZE);
        this.mGridDates = new ArrayList<>(GRID_SIZE);

        // Enable stable IDs for better performance
        setHasStableIds(true);

        // Initialize grid dates
        calculateGridDates();

        // Initialize with placeholder calendar days
        initializePlaceholderDays();

        Log.d(TAG, "MonthViewAdapter created for " + month);
    }

    // ==================== RECYCLERVIEW.ADAPTER METHODS ====================

    @NonNull
    @Override
    public CalendarDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);

        return new CalendarDayViewHolder(view, mEventListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarDayViewHolder holder, int position) {
        if (mIsDestroyed || position >= mCalendarDays.size()) return;

        CalendarDay calendarDay = mCalendarDays.get(position);
        LocalDate date = mGridDates.get(position);

        // Determine if day belongs to current month
        boolean isCurrentMonth = date.getMonth().equals(mCurrentMonth.getMonth()) &&
                date.getYear() == mCurrentMonth.getYear();

        holder.bindCalendarDay(calendarDay, isCurrentMonth);
    }

    @Override
    public int getItemCount() {
        return GRID_SIZE;
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < mGridDates.size()) {
            return mGridDates.get(position).toEpochDay();
        }
        return RecyclerView.NO_ID;
    }

    // ==================== INITIALIZATION METHODS ====================

    /**
     * Calculate all 42 dates for the grid (including overflow from adjacent months).
     */
    private void calculateGridDates() {
        mGridDates.clear();

        // First day of the month
        LocalDate firstDayOfMonth = mCurrentMonth.atDay(1);

        // Find the Monday of the week containing the first day
        LocalDate startDate = firstDayOfMonth.minusDays(
                (firstDayOfMonth.getDayOfWeek().getValue() - 1) % DAYS_PER_WEEK
        );

        // Add 42 consecutive dates starting from startDate
        for (int i = 0; i < GRID_SIZE; i++) {
            mGridDates.add(startDate.plusDays(i));
        }

        Log.d(TAG, "Grid dates calculated: " + startDate + " to " + startDate.plusDays(GRID_SIZE - 1));
    }

    /**
     * Initialize placeholder calendar days for immediate display.
     */
    private void initializePlaceholderDays() {
        mCalendarDays.clear();

        for (LocalDate date : mGridDates) {
            boolean isCurrentMonth = date.getMonth().equals(mCurrentMonth.getMonth()) &&
                    date.getYear() == mCurrentMonth.getYear();

            CalendarDay placeholderDay = new CalendarDay.Builder(date)
                    .isCurrentMonth(isCurrentMonth)
                    .isLoading(true)
                    .build();

            mCalendarDays.add(placeholderDay);
        }

        Log.d(TAG, "Placeholder days initialized: " + mCalendarDays.size());
    }

    // ==================== UPDATE METHODS ====================

    /**
     * Update adapter to display different month.
     *
     * @param month New month to display
     */
    public void updateMonth(@NonNull YearMonth month) {
        if (month.equals(mCurrentMonth)) return;

        mCurrentMonth = month;

        // Recalculate grid dates for new month
        calculateGridDates();

        // Reset to placeholder days
        initializePlaceholderDays();

        // Notify adapter of complete data change
        notifyDataSetChanged();

        Log.d(TAG, "Month updated to: " + month);
    }

    /**
     * Update calendar days with loaded data.
     *
     * @param calendarDays New calendar days data
     */
    public void updateCalendarDays(@NonNull List<CalendarDay> calendarDays) {
        if (mIsDestroyed) return;

        // Create a map for efficient lookup
        java.util.Map<LocalDate, CalendarDay> dayMap = new java.util.HashMap<>();
        for (CalendarDay day : calendarDays) {
            dayMap.put(day.getDate(), day);
        }

        // Update calendar days in grid order
        for (int i = 0; i < mGridDates.size(); i++) {
            LocalDate date = mGridDates.get(i);
            CalendarDay newDay = dayMap.get(date);

            if (newDay != null) {
                // Replace with loaded data
                if (i < mCalendarDays.size()) {
                    mCalendarDays.set(i, newDay);
                } else {
                    mCalendarDays.add(newDay);
                }
            } else {
                // Create placeholder for missing data
                boolean isCurrentMonth = date.getMonth().equals(mCurrentMonth.getMonth()) &&
                        date.getYear() == mCurrentMonth.getYear();

                CalendarDay placeholderDay = new CalendarDay.Builder(date)
                        .isCurrentMonth(isCurrentMonth)
                        .hasError(true)
                        .build();

                if (i < mCalendarDays.size()) {
                    mCalendarDays.set(i, placeholderDay);
                } else {
                    mCalendarDays.add(placeholderDay);
                }
            }
        }

        // Notify adapter of data change
        notifyDataSetChanged();

        Log.d(TAG, "Calendar days updated: " + calendarDays.size() + " days received");
    }

    /**
     * Update specific day with new data.
     *
     * @param date Date to update
     * @param calendarDay New calendar day data
     */
    public void updateDay(@NonNull LocalDate date, @NonNull CalendarDay calendarDay) {
        if (mIsDestroyed) return;

        int position = mGridDates.indexOf(date);
        if (position >= 0 && position < mCalendarDays.size()) {
            mCalendarDays.set(position, calendarDay);
            notifyItemChanged(position);

            Log.v(TAG, "Day updated: " + date + " at position " + position);
        }
    }

    // ==================== LISTENER MANAGEMENT ====================

    /**
     * Update event listener for all view holders.
     *
     * @param eventListener New event listener
     */
    public void setEventListener(@Nullable CalendarEventListener eventListener) {
        mEventListener = eventListener;

        // Update all currently bound view holders would require tracking them
        // For simplicity, we'll let the change take effect on next bind

        Log.d(TAG, "Event listener updated");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get calendar day for specific position.
     *
     * @param position Position in grid
     * @return CalendarDay or null if invalid position
     */
    @Nullable
    public CalendarDay getCalendarDay(int position) {
        if (position >= 0 && position < mCalendarDays.size()) {
            return mCalendarDays.get(position);
        }
        return null;
    }

    /**
     * Get date for specific position.
     *
     * @param position Position in grid
     * @return LocalDate or null if invalid position
     */
    @Nullable
    public LocalDate getDate(int position) {
        if (position >= 0 && position < mGridDates.size()) {
            return mGridDates.get(position);
        }
        return null;
    }

    /**
     * Find position for specific date.
     *
     * @param date Date to find
     * @return Position or -1 if not found
     */
    public int findPositionForDate(@NonNull LocalDate date) {
        return mGridDates.indexOf(date);
    }

    /**
     * Check if date is part of current month.
     *
     * @param date Date to check
     * @return true if date belongs to current month
     */
    public boolean isCurrentMonthDate(@NonNull LocalDate date) {
        return date.getMonth().equals(mCurrentMonth.getMonth()) &&
                date.getYear() == mCurrentMonth.getYear();
    }

    /**
     * Get all dates belonging to current month.
     *
     * @return List of current month dates
     */
    @NonNull
    public List<LocalDate> getCurrentMonthDates() {
        List<LocalDate> currentMonthDates = new ArrayList<>();

        for (LocalDate date : mGridDates) {
            if (isCurrentMonthDate(date)) {
                currentMonthDates.add(date);
            }
        }

        return currentMonthDates;
    }

    /**
     * Get current month being displayed.
     *
     * @return Current YearMonth
     */
    @NonNull
    public YearMonth getCurrentMonth() {
        return mCurrentMonth;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Cleanup adapter and release resources.
     * Should be called when adapter is no longer needed.
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up MonthViewAdapter");

        mIsDestroyed = true;

        // Clear data lists
        mCalendarDays.clear();
        mGridDates.clear();

        // Clear listener
        mEventListener = null;

        Log.d(TAG, "MonthViewAdapter cleanup completed");
    }

    // ==================== DEBUGGING METHODS ====================

    /**
     * Get adapter status for debugging.
     *
     * @return Status string with current state
     */
    public String getAdapterStatus() {
        return "MonthViewAdapter{" +
                "month=" + mCurrentMonth +
                ", daysLoaded=" + mCalendarDays.size() +
                ", gridDates=" + mGridDates.size() +
                ", destroyed=" + mIsDestroyed +
                ", hasEventListener=" + (mEventListener != null) +
                '}';
    }

    /**
     * Log current state for debugging.
     */
    public void logCurrentState() {
        Log.d(TAG, "Current adapter state:");
        Log.d(TAG, "- Month: " + mCurrentMonth);
        Log.d(TAG, "- Calendar days: " + mCalendarDays.size());
        Log.d(TAG, "- Grid dates: " + mGridDates.size());
        Log.d(TAG, "- Current month dates: " + getCurrentMonthDates().size());
        Log.d(TAG, "- Is destroyed: " + mIsDestroyed);
    }
}