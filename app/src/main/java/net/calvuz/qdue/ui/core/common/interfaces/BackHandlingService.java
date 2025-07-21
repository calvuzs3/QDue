package net.calvuz.qdue.ui.core.common.interfaces;

import androidx.annotation.NonNull;

/**
 * Injectable service interface for centralized back handling management.
 * <p>
 * This interface allows for dependency injection of back handling services
 * while maintaining loose coupling between components.
 */
public interface BackHandlingService {

    /**
     * Handle back press for a specific component
     *
     * @param component The component requesting back handling
     * @return true if handled, false otherwise
     */
    boolean handleBackPress(@NonNull Object component);

    /**
     * Register a component for back handling
     *
     * @param component The component to register
     * @param handler The handler for the component
     */
    void registerComponent(@NonNull Object component, @NonNull BackPressHandler handler);

    /**
     * Unregister a component
     *
     * @param component The component to unregister
     */
    void unregisterComponent(@NonNull Object component);

    /**
     * Check if a component has unsaved changes
     *
     * @param component The component to check
     * @return true if component has unsaved changes
     */
    boolean hasUnsavedChanges(@NonNull Object component);

    /**
     * Enable or disable back handling for debugging
     *
     * @param enabled true to enable, false to disable
     */
    void setBackHandlingEnabled(boolean enabled);
}