package net.calvuz.qdue.core.di;

/**
 * Injectable activity interface for dependency injection
 */
public interface Injectable {

    /**
     * Inject dependencies into this component
     */
    void inject(ServiceProvider serviceProvider);

    /**
     * Check if dependencies are injected and ready
     */
    boolean areDependenciesReady();
}
