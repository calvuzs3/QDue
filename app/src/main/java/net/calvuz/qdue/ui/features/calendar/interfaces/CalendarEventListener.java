package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;

import java.time.LocalDate;
import java.util.List;

/**
 * CalendarEventListener - Interface for handling calendar user interactions.
 *
 * <p>Handles all user interactions within the calendar view including day clicks,
 * event actions, and quick event creation.</p>
 */
public interface CalendarEventListener {

    /**
     * Called when user clicks on a day in the calendar.
     *
     * @param date The clicked date
     * @param calendarDay Calendar day data with events and shifts
     */
    void onDayClick(@NonNull LocalDate date, @NonNull CalendarDay calendarDay);

    /**
     * Called when user long-clicks on a day for quick actions.
     *
     * @param date The long-clicked date
     * @param calendarDay Calendar day data with events and shifts
     */
    void onDayLongClick(@NonNull LocalDate date, @NonNull CalendarDay calendarDay);

    /**
     * Called when user clicks on a specific event within a day.
     *
     * @param event The clicked event
     * @param date The date of the event
     */
    void onEventClick(@NonNull LocalEvent event, @NonNull LocalDate date);

    /**
     * Called when user wants to create a new event on a specific date.
     *
     * @param date The date for the new event
     */
    void onCreateEvent(@NonNull LocalDate date);

    /**
     * Called when user wants to view shift details for a specific date.
     *
     * @param date The date of the shift
     * @param workScheduleEvents List of work schedule events for the date
     */
    void onShiftDetailsRequested(@NonNull LocalDate date, @NonNull List<WorkScheduleEvent> workScheduleEvents);
}

