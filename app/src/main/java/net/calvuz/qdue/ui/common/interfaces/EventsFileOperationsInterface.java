package net.calvuz.qdue.ui.common.interfaces;

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
    public void triggerExportEventsToFile();
}
