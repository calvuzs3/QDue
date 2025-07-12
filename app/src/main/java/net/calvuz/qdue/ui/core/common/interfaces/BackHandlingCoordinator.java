package net.calvuz.qdue.ui.core.common.interfaces;

import androidx.annotation.NonNull;

/**
 * Interface for components that need to communicate with back handlers.
 *
 * This interface allows for loose coupling between components and their
 * back handling logic, supporting dependency injection patterns.
 */
public interface BackHandlingCoordinator {

    /**
     * Register a back handler with optional priority
     *
     * @param handler The handler to register
     * @param priority Higher priority handlers are called first (default: 0)
     */
    void registerBackHandler(@NonNull BackPressHandler handler, int priority);

    /**
     * Register a back handler with default priority
     *
     * @param handler The handler to register
     */
    default void registerBackHandler(@NonNull BackPressHandler handler) {
        registerBackHandler(handler, 0);
    }

    /**
     * Unregister a back handler
     *
     * @param handler The handler to unregister
     */
    void unregisterBackHandler(@NonNull BackPressHandler handler);

    /**
     * Clear all registered handlers
     */
    void clearBackHandlers();

    /**
     * Check if any registered handler can handle back press
     *
     * @return true if any handler would handle the back press
     */
    boolean canHandleBackPress();
}