package net.calvuz.qdue.ui.common.di;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.common.interfaces.MultiLevelBackHandler;
import net.calvuz.qdue.ui.common.interfaces.SelectionModeHandler;
import net.calvuz.qdue.ui.common.interfaces.UnsavedChangesHandler;
import net.calvuz.qdue.utils.Log; /**
 * Builder for configuring back handlers for a specific component.
 *
 * This builder provides a fluent API for setting up back handlers
 * with proper configuration and automatic registration, following
 * the project's established patterns.
 */
public class ComponentBackBuilder {
    private final BackHandlingService mService;
    private final Object mComponent;
    private int mPriority = 0;
    private String mDescription;

    /**
     * Package-private constructor
     */
    ComponentBackBuilder(@NonNull BackHandlingService service, @NonNull Object component) {
        this.mService = service;
        this.mComponent = component;
        this.mDescription = component.getClass().getSimpleName() + " back handler";
    }

    /**
     * Set the priority for this back handler
     *
     * Standard priorities (following project conventions):
     * - 100: Critical (unsaved changes, data loss prevention)
     * - 50: UI state (selection modes, search, filters)
     * - 10: Activity navigation (default activity behavior)
     * - 0: Default behavior
     *
     * @param priority Priority level (higher = called first)
     * @return This builder for chaining
     */
    @NonNull
    public ComponentBackBuilder withPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    /**
     * Set a custom description for debugging
     *
     * @param description Human-readable description
     * @return This builder for chaining
     */
    @NonNull
    public ComponentBackBuilder withDescription(@NonNull String description) {
        this.mDescription = description;
        return this;
    }

    /**
     * Register a simple back press handler
     *
     * @param handler The handler to register
     */
    public void register(@NonNull BackPressHandler handler) {
        EnhancedBackHandler enhanced = new EnhancedBackHandler(handler, mPriority, mDescription);
        mService.registerComponent(mComponent, enhanced);

        Log.d("ComponentBackBuilder", String.format("Registered handler: %s (priority: %d)",
                mDescription, mPriority));
    }

    /**
     * Register an unsaved changes handler
     *
     * @param handler The unsaved changes handler to register
     */
    public void registerUnsavedChanges(@NonNull UnsavedChangesHandler handler) {
        EnhancedUnsavedChangesHandler enhanced = new EnhancedUnsavedChangesHandler(handler, mPriority, mDescription);
        mService.registerComponent(mComponent, enhanced);

        Log.d("ComponentBackBuilder", String.format("Registered unsaved changes handler: %s (priority: %d)",
                mDescription, mPriority));
    }

    /**
     * Register a selection mode handler with standard priority
     *
     * @param handler The selection mode handler to register
     */
    public void registerSelectionMode(@NonNull SelectionModeHandler handler) {
        withPriority(50); // Standard priority for UI state
        withDescription(mComponent.getClass().getSimpleName() + " selection mode");
        register(handler);
    }

    // ==================== INTERNAL WRAPPER CLASSES ====================

    /**
     * Enhanced wrapper that adds priority and description to basic handlers
     */
    private static class EnhancedBackHandler implements MultiLevelBackHandler {
        private final BackPressHandler mHandler;
        private final int mPriority;
        private final String mDescription;

        EnhancedBackHandler(BackPressHandler handler, int priority, String description) {
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
     * Enhanced wrapper for unsaved changes handlers
     */
    private static class EnhancedUnsavedChangesHandler extends EnhancedBackHandler
            implements UnsavedChangesHandler {
        private final UnsavedChangesHandler mUnsavedHandler;

        EnhancedUnsavedChangesHandler(UnsavedChangesHandler handler,
                                      int priority, String description) {
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
