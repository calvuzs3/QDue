package net.calvuz.qdue.ui.core.architecture.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler;
import net.calvuz.qdue.ui.core.common.interfaces.MultiLevelBackHandler;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized back handling service with hybrid weak/strong reference management.
 * <p>
 * <p>This service manages back press handling across the entire application using a
 * dual-reference strategy to prevent premature garbage collection while maintaining
 * memory safety.
 * <p>
 * <p><strong>Architecture:</strong>
 * <ul>
 *   <li>Primary storage: WeakReference system for memory safety</li>
 *   <li>Secondary cache: Strong references to prevent premature GC</li>
 *   <li>Priority-based handler execution</li>
 *   <li>Automatic lifecycle-aware cleanup</li>
 * </ul>
 * <p>
 * <p><strong>Usage Pattern:</strong>
 * <pre>
 * // Get service via DI module
 * BackHandlingService service = BackHandlingModule.getBackHandlingService(context);
 * <p>
 * // Register component with handler
 * service.registerComponent(fragment, backHandler);
 * <p>
 * // Handle back press
 * boolean handled = service.handleBackPress(fragment);
 * </pre>
 * <p>
 * <p><strong>Problem Solved:</strong> Fragments registered through Activities were
 * experiencing premature garbage collection of WeakReferences, causing valid handlers
 * to be ignored. The strong reference cache ensures handlers remain available while
 * components are active.
 */
public class BackHandlingServiceImpl implements BackHandlingService {
    private static final String TAG = "BackHandlingService";

    // ==================== CORE DEPENDENCIES ====================

    private boolean mBackHandlingEnabled = true;

    // ==================== DUAL REFERENCE SYSTEM ====================

    /**
     * Primary storage with weak references for memory safety
     */
    private final Map<Object, HandlerEntry> mRegisteredHandlers = new ConcurrentHashMap<>();

    /**
     * Secondary cache with strong references to prevent premature GC.
     * Critical for fragment lifecycle management.
     */
    private final Map<Object, BackPressHandler> mStrongReferenceCache = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates service instance for dependency injection module.
     * Package-private - only called by BackHandlingModule.
     * <p>
     * @param context Application context for service lifecycle
     */
    public BackHandlingServiceImpl(@NonNull Context context) {
    }

    /**
     * Creates service instance for dependency injection module.
     * Package-private - only called by BackHandlingModule.
     */
    public BackHandlingServiceImpl() {}

    // ==================== CORE SERVICE METHODS ====================

    /**
     * Handles back press for specified component with priority-based execution.
     * <p>
     * <p>Execution order:
     * <ol>
     *   <li>Try strong reference cache first (fastest path)</li>
     *   <li>Clean up invalid weak references</li>
     *   <li>Execute handlers by priority (highest first)</li>
     *   <li>Support global handlers (Activity-level)</li>
     * </ol>
     * <p>
     * @param component Component requesting back handling
     * @return true if back press was handled, false to continue with default behavior
     */
    @Override
    public boolean handleBackPress(@NonNull Object component) {
        if (!mBackHandlingEnabled) {
            return false;
        }

        // Fast path: try strong reference cache first
        BackPressHandler cachedHandler = mStrongReferenceCache.get(component);
        if (cachedHandler != null) {
            try {
                boolean handled = cachedHandler.onBackPressed();
                if (handled) {
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in cached handler: " + e.getMessage());
            }
        }

        // Fallback: use weak reference system with cleanup
        cleanupInvalidEntries();
        List<HandlerEntry> handlers = getValidHandlersSortedByPriority();

        for (HandlerEntry entry : handlers) {
            try {
                BackPressHandler handler = entry.getHandler();
                Object handlerComponent = entry.getComponent();

                if (handler != null && handlerComponent != null) {
                    if (handlerComponent == component || isGlobalHandler(entry)) {
                        boolean handled = handler.onBackPressed();
                        if (handled) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in back handler: " + entry.description, e);
            }
        }

        return false;
    }

    /**
     * Registers component with back press handler using dual reference strategy.
     * <p>
     * @param component Component to register (Fragment, Activity, etc.)
     * @param handler Back press handler implementation
     */
    @Override
    public void registerComponent(@NonNull Object component, @NonNull BackPressHandler handler) {
        int priority = 0;
        String description = component.getClass().getSimpleName();

        if (handler instanceof MultiLevelBackHandler) {
            MultiLevelBackHandler multilevel = (MultiLevelBackHandler) handler;
            priority = multilevel.getBackHandlingPriority();
            String customDesc = multilevel.getBackHandlingDescription();
            if (customDesc != null) {
                description = customDesc;
            }
        }

        // Primary registration with weak references
        HandlerEntry entry = new HandlerEntry(component, handler, priority, description);
        mRegisteredHandlers.put(component, entry);

        // Secondary cache with strong reference to prevent GC
        mStrongReferenceCache.put(component, handler);
    }

    /**
     * Unregisters component and cleans up both reference systems.
     * <p>
     * @param component Component to unregister
     */
    @Override
    public void unregisterComponent(@NonNull Object component) {
        HandlerEntry removed = mRegisteredHandlers.remove(component);
        BackPressHandler cachedHandler = mStrongReferenceCache.remove(component);

        if (removed != null && cachedHandler != null) {
            Log.d(TAG, "Unregistered: " + removed.description);
        }
    }

    /**
     * Checks if component has unsaved changes requiring confirmation.
     * <p>
     * @param component Component to check
     * @return true if component has unsaved changes
     */
    @Override
    public boolean hasUnsavedChanges(@NonNull Object component) {
        HandlerEntry entry = mRegisteredHandlers.get(component);
        if (entry != null && entry.isValid()) {
            BackPressHandler handler = entry.getHandler();
            if (handler instanceof UnsavedChangesHandler) {
                return ((UnsavedChangesHandler) handler).hasUnsavedChanges();
            }
        }
        return false;
    }

    @Override
    public void setBackHandlingEnabled(boolean enabled) {
        this.mBackHandlingEnabled = enabled;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    @NonNull
    private List<HandlerEntry> getValidHandlersSortedByPriority() {
        return mRegisteredHandlers.values().stream()
                .filter(HandlerEntry::isValid)
                .sorted(Comparator.comparingInt((HandlerEntry e) -> e.priority).reversed())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void cleanupInvalidEntries() {
        int before = mRegisteredHandlers.size();
        mRegisteredHandlers.entrySet().removeIf(entry -> !entry.getValue().isValid());
        int after = mRegisteredHandlers.size();

        if (before != after) {
            Log.d(TAG, String.format(QDue.getLocale(),
                    "Cleaned up %d invalid handler entries", before - after));
        }
    }

    /**
     * Determines if handler should be called globally for any component.
     * Activity-level handlers are considered global.
     */
    private boolean isGlobalHandler(@NonNull HandlerEntry entry) {
        Object component = entry.getComponent();
        return component instanceof AppCompatActivity;
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Gets comprehensive debug information about service state.
     * Useful for troubleshooting handler registration and execution.
     * <p>
     * @return Formatted debug information string
     */
    @NonNull
    public String debugGetInfo() {
        cleanupInvalidEntries();

        StringBuilder info = new StringBuilder();
        info.append("=== Back Handling Service Debug Info ===\n");
        info.append("Enabled: ").append(mBackHandlingEnabled).append("\n");
        info.append("Weak references: ").append(mRegisteredHandlers.size()).append("\n");
        info.append("Strong references: ").append(mStrongReferenceCache.size()).append("\n\n");

        List<HandlerEntry> handlers = getValidHandlersSortedByPriority();
        for (int i = 0; i < handlers.size(); i++) {
            HandlerEntry entry = handlers.get(i);
            Object component = entry.getComponent();

            info.append(String.format(QDue.getLocale(),
                    "%d. %s (priority: %d)\n",
                    i + 1, entry.description, entry.priority));
            info.append(String.format("   Component: %s\n",
                    component != null ? component.getClass().getSimpleName() : "null"));
            info.append(String.format("   Handler: %s\n",
                    entry.getHandler() != null ? entry.getHandler().getClass().getSimpleName() : "null"));
            info.append(String.format(QDue.getLocale(),
                    "   Age: %d ms\n",
                    System.currentTimeMillis() - entry.registrationTime));

            if (entry.getHandler() instanceof UnsavedChangesHandler) {
                UnsavedChangesHandler uch = (UnsavedChangesHandler) entry.getHandler();
                info.append(String.format("   Has unsaved changes: %s\n", uch.hasUnsavedChanges()));
            }

            info.append("\n");
        }

        return info.toString();
    }

    /**
     * Forces cleanup of all invalid entries.
     * Primarily for testing and diagnostics.
     */
    public void forceCleanup() {
        cleanupInvalidEntries();
    }

    /**
     * Gets count of currently registered handlers.
     * Performs cleanup before counting for accuracy.
     * <p>
     * @return Number of valid registered handlers
     */
    public int getRegisteredHandlerCount() {
        cleanupInvalidEntries();
        return mRegisteredHandlers.size();
    }

    // ==================== INNER CLASSES ====================

    /**
     * Internal storage entry combining weak references with metadata.
     * <p>
     * <p>Uses weak references for automatic memory management while storing
     * essential metadata (priority, description, registration time) for
     * debugging and execution order.
     */
    private static class HandlerEntry {
        final WeakReference<Object> componentRef;
        final WeakReference<BackPressHandler> handlerRef;
        final int priority;
        final String description;
        final long registrationTime;

        HandlerEntry(Object component, BackPressHandler handler, int priority, String description) {
            this.componentRef = new WeakReference<>(component);
            this.handlerRef = new WeakReference<>(handler);
            this.priority = priority;
            this.description = description;
            this.registrationTime = System.currentTimeMillis();
        }

        boolean isValid() {
            return componentRef.get() != null && handlerRef.get() != null;
        }

        BackPressHandler getHandler() {
            return handlerRef.get();
        }

        Object getComponent() {
            return componentRef.get();
        }
    }
}