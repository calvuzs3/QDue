package net.calvuz.qdue.ui.common.di;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.common.interfaces.BackHandlingService;
import net.calvuz.qdue.utils.Log; /**
 * Factory for creating back handlers with fluent API and custom DI support.
 *
 * This factory provides a convenient way to create and register back handlers
 * while maintaining clean dependency injection patterns using the project's
 * custom ServiceLocator approach.
 *
 * Features:
 * - Fluent API for handler creation
 * - Automatic priority and description management
 * - Type-safe handler registration
 * - Built-in common handler patterns
 * - Follows project's singleton pattern
 *
 * Usage:
 * BackHandlerFactory factory = BackHandlingModule.getBackHandlerFactory(context);
 * factory.forComponent(this)
 *        .withPriority(100)
 *        .registerUnsavedChanges(myHandler);
 */
public class BackHandlerFactory {
    private static final String TAG = "BackHandlerFactory";

    private final BackHandlingService mBackService;

    /**
     * Package-private constructor (called by BackHandlingModule)
     *
     * @param backService The back handling service
     */
    BackHandlerFactory(@NonNull BackHandlingService backService) {
        this.mBackService = backService;
        Log.d(TAG, "BackHandlerFactory created with service: " + backService.getClass().getSimpleName());
    }

    /**
     * Create a builder for a specific component
     *
     * @param component The component that will handle back presses
     * @return Builder for configuring the back handler
     */
    @NonNull
    public ComponentBackBuilder forComponent(@NonNull Object component) {
        return new ComponentBackBuilder(mBackService, component);
    }

    /**
     * Create a simple back handler that always handles back presses
     *
     * @param component The component
     * @param action Action to execute on back press
     */
    public void createSimpleHandler(@NonNull Object component, @NonNull Runnable action) {
        forComponent(component).register(() -> {
            action.run();
            return true;
        });
    }

    /**
     * Create a conditional back handler
     *
     * @param component The component
     * @param condition Condition to check before handling
     * @param action Action to execute if condition is true
     */
    public void createConditionalHandler(@NonNull Object component,
                                         @NonNull java.util.function.BooleanSupplier condition,
                                         @NonNull Runnable action) {
        forComponent(component).register(() -> {
            if (condition.getAsBoolean()) {
                action.run();
                return true;
            }
            return false;
        });
    }

    /**
     * Get the underlying back handling service
     *
     * @return The back handling service
     */
    @NonNull
    public BackHandlingService getBackService() {
        return mBackService;
    }
}
