package net.calvuz.qdue.ui.shared.interfaces;

import android.view.View;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;

import java.time.LocalDate;

/**
 * Interface for handling day long-click events and toolbar actions
 * Used across all calendar views (DaysList, CalendarView, etc.)
 */
public interface DayLongClickListener {

    /**
     * Called when a day item is long-clicked
     * @param day The Day object
     * @param date The LocalDate
     * @param itemView The clicked view for positioning
     * @param position The adapter position
     */
    void onDayLongClick(Day day, LocalDate date, View itemView, int position);

    /**
     * Called when a toolbar action is selected
     * @param action The selected action
     * @param day The Day object
     * @param date The LocalDate
     */
    void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date);

    /**
     * Called when selection mode changes
     * @param isSelectionMode Whether we're in selection mode
     * @param selectedCount Number of selected items
     */
    void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);

    /**
     * Called when day selection changes in multi-select mode
     * @param day The Day object
     * @param date The LocalDate
     * @param isSelected Whether the day is now selected
     */
    void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected);
}
