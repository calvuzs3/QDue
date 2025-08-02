package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CalendarDataProvider - Interface for providing calendar data.
 *
 * <p>Abstracts data loading and management for calendar components.
 * Combines work schedule data with events to provide unified calendar information.</p>
 */
public interface CalendarDataProvider {

    /**
     * Load calendar data for a specific month.
     *
     * @param yearMonth Month to load data for
     * @return CompletableFuture with calendar days for the month
     */
    @NonNull
    CompletableFuture<List<CalendarDay>> loadMonthData(@NonNull YearMonth yearMonth);

    /**
     * Load calendar data for a specific date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CompletableFuture with calendar days for the date range
     */
    @NonNull
    CompletableFuture<List<CalendarDay>> loadDateRangeData(@NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Load calendar data for a specific date.
     *
     * @param date Date to load data for
     * @return CompletableFuture with calendar day data
     */
    @NonNull
    CompletableFuture<CalendarDay> loadDayData(@NonNull LocalDate date);

    /**
     * Get current loading state for UI feedback.
     *
     * @return Current loading state
     */
    @NonNull
    CalendarLoadingState getLoadingState();

    /**
     * Set current user for data filtering.
     *
     * @param userId User ID to filter by (null for current user)
     */
    void setCurrentUser(@Nullable Long userId);

    /**
     * Refresh data for specific date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     */
    void refreshData(@NonNull LocalDate startDate, @NonNull LocalDate endDate);

    /**
     * Clear cached data and force reload.
     */
    void clearCache();

    /**
     * Destroy provider and cleanup resources.
     */
    void destroy();
}