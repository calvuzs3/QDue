package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.List; /**
 * CalendarViewStateListener - Interface for calendar view state changes.
 *
 * <p>Handles view state changes like selection mode, current date highlights, etc.</p>
 */
public interface CalendarViewStateListener {

    /**
     * Called when calendar view state changes.
     *
     * @param currentDate Currently highlighted date
     * @param selectedDates List of selected dates (for multi-selection)
     * @param isSelectionMode Whether selection mode is active
     */
    void onViewStateChanged(@Nullable LocalDate currentDate,
                            @NonNull List<LocalDate> selectedDates,
                            boolean isSelectionMode);

    /**
     * Called when calendar view mode changes.
     *
     * @param viewMode New view mode
     * @param visibleRange Currently visible date range
     */
    void onViewModeChanged(@NonNull String viewMode, @NonNull LocalDate[] visibleRange);
}
