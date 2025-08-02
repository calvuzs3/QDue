package net.calvuz.qdue.ui.features.calendar.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate; /**
 * CalendarQuickActionListener - Interface for quick actions in calendar.
 *
 * <p>Handles quick actions like creating events, setting reminders, etc.</p>
 */
public interface CalendarQuickActionListener {

    /**
     * Called when user wants to create a quick event.
     *
     * @param date Date for the event
     * @param eventType Suggested event type
     */
    void onQuickEventRequested(@NonNull LocalDate date, @Nullable String eventType);

    /**
     * Called when user wants to set a quick reminder.
     *
     * @param date Date for the reminder
     */
    void onQuickReminderRequested(@NonNull LocalDate date);

    /**
     * Called when user wants to mark a day as special.
     *
     * @param date Date to mark
     * @param markType Type of marking (holiday, important, etc.)
     */
    void onQuickMarkRequested(@NonNull LocalDate date, @NonNull String markType);
}
