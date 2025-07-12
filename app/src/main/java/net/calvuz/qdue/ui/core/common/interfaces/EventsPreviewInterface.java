package net.calvuz.qdue.ui.core.common.interfaces;

import net.calvuz.qdue.events.models.LocalEvent;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface for handling events preview in different views
 * Adaptive strategy: different implementations for DaysList vs CalendarView
 */
public interface EventsPreviewInterface {

    /**
     * Show events preview for a specific date
     * @param date The date to show events for
     * @param events List of events for that date
     * @param anchorView View that was clicked (for positioning)
     */
    void showEventsPreview(LocalDate date, List<LocalEvent> events, android.view.View anchorView);

    /**
     * Hide currently showing events preview
     */
    void hideEventsPreview();

    /**
     * Check if events preview is currently showing
     * @return true if preview is visible
     */
    boolean isEventsPreviewShowing();

    /**
     * Handle quick action on an event
     * @param action The action to perform
     * @param event The event to act on
     * @param date The date context
     */
    void onEventQuickAction(EventQuickAction action, LocalEvent event, LocalDate date);

    /**
     * Handle general actions (add event, navigate to events activity)
     * @param action The action to perform
     * @param date The date context
     */
    void onEventsGeneralAction(EventGeneralAction action, LocalDate date);

    /**
     * Set callback listener for events preview actions
     * @param listener The callback listener
     */
    void setEventsPreviewListener(EventsPreviewListener listener);

    /**
     * Quick actions that can be performed on individual events
     */
    enum EventQuickAction {
        VIEW_DETAIL, EDIT, DELETE, DUPLICATE, TOGGLE_COMPLETE
    }

    /**
     * General actions for events management
     */
    enum EventGeneralAction {
        ADD_EVENT, NAVIGATE_TO_EVENTS_ACTIVITY, REFRESH_EVENTS
    }

    /**
     * Callback interface for events preview actions
     */
    interface EventsPreviewListener {
        /**
         * Called when an event quick action is performed
         */
        void onEventQuickAction(EventQuickAction action, LocalEvent event, LocalDate date);

        /**
         * Called when a general events action is performed
         */
        void onEventsGeneralAction(EventGeneralAction action, LocalDate date);

        /**
         * Called when events preview is shown
         */
        void onEventsPreviewShown(LocalDate date, int eventCount);

        /**
         * Called when events preview is hidden
         */
        void onEventsPreviewHidden(LocalDate date);
    }
}
