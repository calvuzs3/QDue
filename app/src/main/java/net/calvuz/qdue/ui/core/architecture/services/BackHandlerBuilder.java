package net.calvuz.qdue.ui.core.architecture.services;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.interfaces.MultiLevelBackHandler;

/**
 * Simple builder for direct back handler creation with fluent API.
 * <p>
 * <p>This builder provides immediate, lightweight back handler registration
 * without the complexity of the factory system. Ideal for simple components,
 * testing scenarios, and direct service integration.
 * <p>
 * <p><strong>Design Purpose:</strong>
 * <ul>
 *   <li>Direct service access without factory layer</li>
 *   <li>Minimal API surface for simple use cases</li>
 *   <li>Testing-friendly with easy mocking</li>
 *   <li>Self-contained handler wrappers</li>
 * </ul>
 * <p>
 * <p><strong>Usage Pattern:</strong>
 * <pre>
 * // Direct registration with existing service
 * BackHandlingService service = BackHandlingModule.getBackHandlingService(context);
 * <p>
 * new BackHandlerBuilder(service, this)
 *     .withPriority(100)
 *     .withDescription("Fragment selection mode")
 *     .register(() -> exitSelectionMode());
 * <p>
 * // Or for unsaved changes
 * new BackHandlerBuilder(service, this)
 *     .registerUnsavedChanges(new UnsavedChangesHandler() {
 *         // Implementation
 *     });
 * </pre>
 * <p>
 * <p><strong>When to Use:</strong>
 * <ul>
 *   <li>Simple components without complex DI requirements</li>
 *   <li>Testing scenarios requiring direct control</li>
 *   <li>Legacy components needing quick integration</li>
 *   <li>Prototype development and experimentation</li>
 * </ul>
 * <p>
 * <p><strong>vs BackHandlerFactory:</strong> This builder is simpler but less
 * feature-rich than BackHandlerFactory. Use BackHandlerFactory for complex
 * applications with advanced DI requirements.
 */
public class BackHandlerBuilder {

    // ==================== BUILDER STATE ====================

    private final BackHandlingService mService;
    private final Object mComponent;
    private int mPriority = 0;
    private String mDescription;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates builder for component with direct service access.
     *<p>
     * @param service Back handling service instance
     * @param component Component that will handle back presses
     */
    public BackHandlerBuilder(@NonNull BackHandlingService service, @NonNull Object component) {
        this.mService = service;
        this.mComponent = component;
        this.mDescription = component.getClass().getSimpleName();
    }

    // ==================== FLUENT CONFIGURATION ====================

    /**
     * Sets priority for back handler execution order.
     * Higher priority handlers execute first.
     * <p>
     * @param priority Handler priority (default: 0)
     * @return This builder for method chaining
     */
    @NonNull
    public BackHandlerBuilder withPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    /**
     * Sets custom description for debugging and logging.
     * <p>
     * @param description Human-readable handler description
     * @return This builder for method chaining
     */
    @NonNull
    public BackHandlerBuilder withDescription(@NonNull String description) {
        this.mDescription = description;
        return this;
    }

    // ==================== REGISTRATION METHODS ====================

    /**
     * Registers simple back press handler.
     * <p>
     * @param handler Back press handler implementation
     */
    public void register(@NonNull BackPressHandler handler) {
        mService.registerComponent(mComponent, new BackHandler(handler, mPriority, mDescription));
    }

    /**
     * Registers unsaved changes handler with confirmation dialog support.
     * <p>
     * @param handler Unsaved changes handler implementation
     */
    public void registerUnsavedChanges(@NonNull net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler handler) {
        mService.registerComponent(mComponent, new UnsavedChangesHandler(handler, mPriority, mDescription));
    }

    // ==================== ENHANCED WRAPPER CLASSES ====================

    /**
     * Enhanced wrapper that adds priority and description to simple handlers.
     * <p>
     * <p>This wrapper transforms basic BackPressHandler implementations into
     * MultiLevelBackHandler instances with priority support and debugging
     * information.
     */
    private static class BackHandler implements MultiLevelBackHandler {
        private final BackPressHandler mHandler;
        private final int mPriority;
        private final String mDescription;

        BackHandler(BackPressHandler handler, int priority, String description) {
            this.mHandler = handler;
            this.mPriority = priority;
            this.mDescription = description;
        }

        @Override
        public boolean onBackPressed() {
            return mHandler.onBackPressed();
        }

        @Override
        public int getBackHandlingPriority() {
            return mPriority;
        }

        @Override
        public String getBackHandlingDescription() {
            return mDescription;
        }
    }

    /**
     * Enhanced wrapper for unsaved changes handlers with full interface support.
     * <p>
     * <p>This wrapper extends BackHandler to provide complete
     * UnsavedChangesHandler interface implementation while maintaining
     * priority and description support.
     */
    private static class UnsavedChangesHandler extends BackHandler implements net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler {
        private final net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler mUnsavedHandler;

        UnsavedChangesHandler(net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler handler, int priority, String description) {
            super(handler, priority, description);
            this.mUnsavedHandler = handler;
        }

        @Override
        public boolean hasUnsavedChanges() {
            return mUnsavedHandler.hasUnsavedChanges();
        }

        @Override
        public void handleUnsavedChanges(@NonNull Runnable onProceed, @NonNull Runnable onCancel) {
            mUnsavedHandler.handleUnsavedChanges(onProceed, onCancel);
        }

        @Override
        public String getUnsavedChangesDescription() {
            return mUnsavedHandler.getUnsavedChangesDescription();
        }
    }
}