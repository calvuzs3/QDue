package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.YearMonth; /**
 * CalendarNavigationListener - Interface for handling calendar navigation events.
 *
 * <p>Handles navigation between different time periods and view modes in the calendar.</p>
 */
public interface CalendarNavigationListener {

    /**
     * Called when user navigates to a different month.
     *
     * @param yearMonth The new month being displayed
     */
    void onMonthChanged(@NonNull YearMonth yearMonth);

    /**
     * Called when user requests to change view mode.
     *
     * @param viewMode The requested view mode (month/week/day)
     */
    void onViewModeChangeRequested(@NonNull String viewMode);

    /**
     * Called when user wants to navigate to today's date.
     */
    void onTodayRequested();

    /**
     * Called when user wants to navigate to a specific date.
     *
     * @param date The target date
     */
    void onDateNavigationRequested(@NonNull LocalDate date);
}
