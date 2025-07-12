package net.calvuz.qdue.ui.core.architecture.services;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.interfaces.MultiLevelBackHandler;
import net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler;

/**
 * Convenient builder for creating back handlers with DI.
 *
 * This builder provides a fluent API for creating and configuring
 * back handlers while maintaining dependency injection compatibility.
 */
public class BackHandlerBuilder {
    private final BackHandlingService mService;
    private final Object mComponent;
    private int mPriority = 0;
    private String mDescription;

    public BackHandlerBuilder(@NonNull BackHandlingService service, @NonNull Object component) {
        this.mService = service;
        this.mComponent = component;
        this.mDescription = component.getClass().getSimpleName();
    }

    /**
     * Set the priority for the back handler
     */
    @NonNull
    public BackHandlerBuilder withPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    /**
     * Set a custom description for the back handler
     */
    @NonNull
    public BackHandlerBuilder withDescription(@NonNull String description) {
        this.mDescription = description;
        return this;
    }

    /**
     * Register a simple back handler
     */
    public void register(@NonNull BackPressHandler handler) {
        mService.registerComponent(mComponent, new EnhancedBackHandler(handler, mPriority, mDescription));
    }

    /**
     * Register an unsaved changes handler
     */
    public void registerUnsavedChanges(@NonNull UnsavedChangesHandler handler) {
        mService.registerComponent(mComponent, new EnhancedUnsavedChangesHandler(handler, mPriority, mDescription));
    }

    /**
     * Enhanced wrapper that adds priority and description
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
    private static class EnhancedUnsavedChangesHandler extends EnhancedBackHandler implements UnsavedChangesHandler {
        private final UnsavedChangesHandler mUnsavedHandler;

        EnhancedUnsavedChangesHandler(UnsavedChangesHandler handler, int priority, String description) {
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
