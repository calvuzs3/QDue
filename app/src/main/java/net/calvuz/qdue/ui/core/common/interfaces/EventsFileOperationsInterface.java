package net.calvuz.qdue.ui.core.common.interfaces;

import android.net.Uri;

import net.calvuz.qdue.domain.calendar.events.models.EventEntityGoogle;

import java.util.List;
import java.util.Set;

public interface EventsFileOperationsInterface {

    /**
     * Trigger import events from FILE
     */
    public void triggerImportEventsFromFile();

    /**
     * Trigger import events from URL
     */
    public void triggerImportEventsFromUrl();

    /**
     * Trigger export events to file
     */
    public void triggerExportEventsToFile(Uri fileUri);

    /**
     * Trigger export SELECTED events to file
     * @param selectedEventIds Set of event IDs to export
     * @param selectedEvents List of selected EventEntityGoogle objects for export
     */
    public void triggerExportSelectedEventsToFile(Uri fileUri, Set<String> selectedEventIds, List<EventEntityGoogle> selectedEvents);
}
