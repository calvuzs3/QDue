package net.calvuz.qdue.ui.events.interfaces;

/**
 * Interface for back press handling
 * Remains in UI package as it's navigation-specific
 */
public interface BackPressHandler {
    /**
     * Handle back press event
     * @return true if handled, false if can proceed with default behavior
     */
    boolean onBackPressed();
}
