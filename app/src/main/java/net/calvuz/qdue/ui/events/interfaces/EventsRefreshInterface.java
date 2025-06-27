package net.calvuz.qdue.ui.events.interfaces;

/**
 * Interface for fragments that display events and need to be refreshed
 * when events data changes in the database.
 *
 * Fragments implementing this interface will receive notifications when:
 * - Events are imported from files
 * - Events are created, modified, or deleted
 * - Any other event-related data changes occur
 *
 * The interface provides both immediate refresh for active fragments
 * and lazy refresh for inactive fragments.
 */
public interface EventsRefreshInterface {

    /**
     * Called when events data has changed and the fragment should refresh
     * its display immediately.
     *
     * This method is called for active/visible fragments to provide
     * immediate visual feedback to the user.
     *
     * @param changeType Type of change (import, delete, create, modify)
     * @param eventCount Number of events affected by the change
     */
    void onEventsChanged(String changeType, int eventCount);

    /**
     * Called when the fragment should perform a complete refresh of events data.
     *
     * This method is called when:
     * - Fragment becomes visible after being inactive
     * - A full refresh is needed (e.g., after major data changes)
     * - Manual refresh is triggered
     */
    void onForceEventsRefresh();

    /**
     * Check if this fragment is currently active and should receive immediate updates.
     *
     * @return true if fragment is visible and should be refreshed immediately,
     *         false if fragment is inactive and should use lazy loading
     */
    boolean isFragmentActive();

    /**
     * Get a description of what type of events this fragment displays.
     * Used for logging and debugging purposes.
     *
     * @return String description (e.g., "Calendar View", "Events List", "Days List")
     */
    String getFragmentDescription();
}