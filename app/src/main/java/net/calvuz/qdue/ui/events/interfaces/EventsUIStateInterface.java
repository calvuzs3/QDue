package net.calvuz.qdue.ui.events.interfaces;

/**
 * Interface for communication between EventsListFragment and EventsActivity
 * regarding UI state changes, specifically FAB visibility control
 *
 * @author calvuzs3
 */
public interface EventsUIStateInterface {

    /**
     * Called by EventsListFragment to notify EventsActivity about events list state
     * Used for controlling FAB visibility based on events availability
     *
     * @param hasEvents true if there are events in the list, false if empty
     */
    void onEventsListStateChanged(boolean hasEvents);

}