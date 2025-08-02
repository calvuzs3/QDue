package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;

import java.time.YearMonth;
import java.util.List;

/**
 * CalendarDataLoadingListener - Interface for calendar data loading callbacks.
 *
 * <p>Provides callbacks for data loading states to update UI accordingly.</p>
 */
public interface CalendarDataLoadingListener {

    /**
     * Called when data loading starts.
     *
     * @param yearMonth Month being loaded
     */
    void onLoadingStarted(@NonNull YearMonth yearMonth);

    /**
     * Called when data loading completes successfully.
     *
     * @param yearMonth Month that was loaded
     * @param calendarDays Loaded calendar days
     */
    void onLoadingCompleted(@NonNull YearMonth yearMonth, @NonNull List<CalendarDay> calendarDays);

    /**
     * Called when data loading fails.
     *
     * @param yearMonth Month that failed to load
     * @param error Error that occurred
     */
    void onLoadingFailed(@NonNull YearMonth yearMonth, @NonNull Throwable error);

    /**
     * Called when loading progress updates.
     *
     * @param yearMonth Month being loaded
     * @param progress Progress percentage (0-100)
     */
    void onLoadingProgress(@NonNull YearMonth yearMonth, int progress);
}

