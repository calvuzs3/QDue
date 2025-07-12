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
 * Centralized back handling service using project's custom ServiceLocator pattern.
 *
 * This service manages back press handling across the entire application,
 * providing consistent behavior and centralized logic while maintaining
 * loose coupling between components. No external DI libraries required.
 *
 * Features:
 * - Centralized back handling logic
 * - Priority-based handler execution
 * - Weak references to prevent memory leaks
 * - Lifecycle-aware cleanup
 * - Debug and testing support
 * - Custom ServiceLocator pattern compliance
 *
 * Usage with ServiceLocator:
 * BackHandlingService service = BackHandlingModule.getBackHandlingService(context);
 * service.registerComponent(this, myBackHandler);
 */
public class BackHandlingServiceImpl implements BackHandlingService {
    private static final String TAG = "BackHandlingService";

    private final Context mContext;
    private final Map<Object, HandlerEntry> mRegisteredHandlers = new ConcurrentHashMap<>();
    private boolean mBackHandlingEnabled = true;

    // ✅ AGGIUNGERE: Cache con strong references per evitare GC
    private final Map<Object, BackPressHandler> mStrongReferenceCache = new ConcurrentHashMap<>();


    /**
     * Internal entry for registered handlers
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

    /**
     * Constructor for ServiceLocator pattern
     * (ex Package-private for use in BackHandlingModule)
     * Called only by BackHandlingModule
     *
     * @param context Application context
     */
    public BackHandlingServiceImpl(@NonNull Context context) {
        this.mContext = context;
        Log.d(TAG, "BackHandlingService initialized with ServiceLocator pattern");
    }

    @Override
    public boolean handleBackPress(@NonNull Object component) {
        if (!mBackHandlingEnabled) {
            Log.d(TAG, "Back handling disabled, skipping");
            return false;
        }

        // ✅ STEP 1: Prova prima senza cleanup per vedere lo stato reale
        Log.d(TAG, String.format(QDue.getLocale(),
                "Handling back press for %s. Registered: %d, Cached: %d",
                component.getClass().getSimpleName(),
                mRegisteredHandlers.size(),
                mStrongReferenceCache.size()));

        // ✅ STEP 2: Prova strong reference cache PRIMA del cleanup
        BackPressHandler cachedHandler = mStrongReferenceCache.get(component);
        if (cachedHandler != null) {
            Log.d(TAG, "Found handler in strong reference cache, trying it first");
            try {
                boolean handled = cachedHandler.onBackPressed();
                if (handled) {
                    Log.d(TAG, "✅ Back press handled by CACHED handler for " +
                            component.getClass().getSimpleName());
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in cached handler: " + e.getMessage());
            }
        }

        // ✅ STEP 3: Fallback al sistema WeakReference normale
        cleanupInvalidEntries();

        List<HandlerEntry> handlers = getValidHandlersSortedByPriority();

        Log.d(TAG, String.format(QDue.getLocale(),
                "After cleanup: %d valid handlers remaining", handlers.size()));

        // Try each handler in priority order
        for (HandlerEntry entry : handlers) {
            try {
                BackPressHandler handler = entry.getHandler();
                Object handlerComponent = entry.getComponent();

                if (handler != null && handlerComponent != null) {
                    if (handlerComponent == component || isGlobalHandler(entry)) {
                        boolean handled = handler.onBackPressed();

                        if (handled) {
                            Log.d(TAG, String.format(QDue.getLocale(),
                                    "✅ Back press handled by WeakRef handler %s (priority: %d)",
                                    entry.description, entry.priority));
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in back handler: " + entry.description, e);
            }
        }

        Log.d(TAG, "No handler processed the back press");
        return false;
    }

    @Override
    public void registerComponent(@NonNull Object component, @NonNull BackPressHandler handler) {
        int priority = 0;
        String description = component.getClass().getSimpleName();

        // Extract priority and description if available
        if (handler instanceof MultiLevelBackHandler) {
            MultiLevelBackHandler multilevel = (MultiLevelBackHandler) handler;
            priority = multilevel.getBackHandlingPriority();
            String customDesc = multilevel.getBackHandlingDescription();
            if (customDesc != null) {
                description = customDesc;
            }
        }

        HandlerEntry entry = new HandlerEntry(component, handler, priority, description);
        mRegisteredHandlers.put(component, entry);

        // ✅ AGGIUNGERE: Mantieni strong reference per evitare GC
        mStrongReferenceCache.put(component, handler);

        Log.d(TAG, String.format(QDue.getLocale(),
                "Registered back handler: %s (priority: %d)",
                description, priority));
    }

    @Override
    public void unregisterComponent(@NonNull Object component) {
        HandlerEntry removed = mRegisteredHandlers.remove(component);

        // ✅ AGGIUNGERE: Rimuovi dalla cache
        BackPressHandler cachedHandler = mStrongReferenceCache.remove(component);

        if (removed != null) {
            Log.d(TAG, "Unregistered back handler: " + removed.description +
                    " [STRONG REF REMOVED]");
        }
    }

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
        Log.d(TAG, "Back handling " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Get all valid handlers sorted by priority (highest first)
     */
    @NonNull
    private List<HandlerEntry> getValidHandlersSortedByPriority() {
        return mRegisteredHandlers.values().stream()
                .filter(HandlerEntry::isValid)
                .sorted(Comparator.comparingInt((HandlerEntry e) -> e.priority).reversed())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Clean up entries with invalid weak references
     */
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
     * Check if handler entry represents a global handler
     *
     * Global handlers are called for any component's back press.
     * Currently, Activity-level handlers are considered global.
     */
    private boolean isGlobalHandler(@NonNull HandlerEntry entry) {
        Object component = entry.getComponent();
        return component instanceof AppCompatActivity;
    }

    /**
     * Get debug information about registered handlers
     */
    @NonNull
    public String getDebugInfo() {
        cleanupInvalidEntries();

        StringBuilder info = new StringBuilder();
        info.append("=== Back Handling Service Debug Info ===\n");
        info.append("Enabled: ").append(mBackHandlingEnabled).append("\n");
        info.append("Registered handlers: ").append(mRegisteredHandlers.size()).append("\n\n");

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
                    "   Registered: %d ms ago\n",
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
     * Force cleanup of all invalid entries (for testing)
     */
    public void forceCleanup() {
        cleanupInvalidEntries();
        Log.d(TAG, "Forced cleanup completed");
    }

    /**
     * Get the number of registered handlers (for testing)
     */
    public int getRegisteredHandlerCount() {
        cleanupInvalidEntries();
        return mRegisteredHandlers.size();
    }
}

