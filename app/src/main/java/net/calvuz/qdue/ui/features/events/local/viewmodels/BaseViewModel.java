package net.calvuz.qdue.ui.features.events.local.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base ViewModel Class
 *
 * <p>Abstract base class for all ViewModels in the LocalEvents MVVM architecture.
 * Provides common functionality including state management, event notification,
 * and lifecycle management using observable pattern with custom listeners.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>State Management</strong>: Observable state changes with listener pattern</li>
 *   <li><strong>Event System</strong>: One-time events for navigation and UI actions</li>
 *   <li><strong>Error Handling</strong>: Centralized error state management</li>
 *   <li><strong>Loading States</strong>: Built-in loading state tracking</li>
 *   <li><strong>Thread Safety</strong>: Concurrent collections for multi-threaded access</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>
 * public class MyViewModel extends BaseViewModel {
 *     private final MyService mService;
 *
 *     public MyViewModel(MyService service) {
 *         this.mService = service;
 *     }
 *
 *     public void loadData() {
 *         setLoading("data", true);
 *         mService.getData()
 *             .thenAccept(result -> {
 *                 setLoading("data", false);
 *                 if (result.isSuccess()) {
 *                     notifyStateChange("data", result.getData());
 *                 } else {
 *                     notifyError("data", result.getErrorMessage());
 *                 }
 *             });
 *     }
 * }
 *
 * // In Activity/Fragment:
 * viewModel.addStateChangeListener("data", (key, value) -> updateUI(value));
 * viewModel.addEventListener(event -> handleNavigationEvent(event));
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public abstract class BaseViewModel {

    private static final String TAG = "BaseViewModel";

    // ==================== STATE MANAGEMENT ====================

    private final Map<String, Object> mState = new ConcurrentHashMap<>();
    private final Map<String, List<StateChangeListener>> mStateListeners = new ConcurrentHashMap<>();
    private final List<EventListener> mEventListeners = new CopyOnWriteArrayList<>();

    // ==================== LOADING STATE ====================

    private final Map<String, Boolean> mLoadingStates = new ConcurrentHashMap<>();
    private final List<LoadingStateListener> mLoadingListeners = new CopyOnWriteArrayList<>();

    // ==================== ERROR STATE ====================

    private final Map<String, String> mErrorStates = new ConcurrentHashMap<>();
    private final List<ErrorStateListener> mErrorListeners = new CopyOnWriteArrayList<>();

    // ==================== LIFECYCLE STATE ====================

    private boolean mInitialized = false;
    private boolean mDestroyed = false;

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Initialize ViewModel. Called when ViewModel is first created.
     * Subclasses should override to perform initialization.
     */
    public void initialize() {
        if (!mInitialized) {
            Log.d(TAG, "Initializing ViewModel: " + getClass().getSimpleName());
            mInitialized = true;
            onInitialize();
        }
    }

    /**
     * Called when ViewModel is being destroyed.
     * Subclasses should override to perform cleanup.
     */
    public void onDestroy() {
        if (!mDestroyed) {
            Log.d(TAG, "Destroying ViewModel: " + getClass().getSimpleName());
            mDestroyed = true;

            // Clear all listeners
            mStateListeners.clear();
            mEventListeners.clear();
            mLoadingListeners.clear();
            mErrorListeners.clear();

            // Clear state
            mState.clear();
            mLoadingStates.clear();
            mErrorStates.clear();

            onCleanup();
        }
    }

    /**
     * Check if ViewModel is initialized.
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Check if ViewModel is destroyed.
     */
    public boolean isDestroyed() {
        return mDestroyed;
    }

    // ==================== ABSTRACT METHODS ====================

    /**
     * Called during initialization. Subclasses should implement initialization logic here.
     */
    protected abstract void onInitialize();

    /**
     * Called during destruction. Subclasses should implement cleanup logic here.
     */
    protected abstract void onCleanup();

    // ==================== STATE MANAGEMENT ====================

    /**
     * Get current value for state key.
     *
     * @param key State key
     * @return Current value or null if not set
     */
    @Nullable
    public Object getState(@NonNull String key) {
        ensureNotDestroyed();
        return mState.get(key);
    }

    /**
     * Get current value for state key with type casting.
     *
     * @param key State key
     * @param clazz Expected value type
     * @return Current value cast to expected type or null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getState(@NonNull String key, @NonNull Class<T> clazz) {
        Object value = getState(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Set state value and notify listeners.
     *
     * @param key State key
     * @param value New value
     */
    protected void setState(@NonNull String key, @Nullable Object value) {
        ensureNotDestroyed();

        Object oldValue = mState.put(key, value);
        if (!objectEquals(oldValue, value)) {
            notifyStateChange(key, value);
        }
    }

    /**
     * Update state value and notify listeners.
     * Alias for setState for better readability.
     *
     * @param key State key
     * @param value New value
     */
    protected void notifyStateChange(@NonNull String key, @Nullable Object value) {
        ensureNotDestroyed();
        setState(key, value);

        List<StateChangeListener> listeners = mStateListeners.get(key);
        if (listeners != null) {
            for (StateChangeListener listener : listeners) {
                try {
                    listener.onStateChanged(key, value);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying state change listener for key: " + key, e);
                }
            }
        }
    }

    /**
     * Add listener for specific state key changes.
     *
     * @param key State key to listen for
     * @param listener Listener to add
     */
    public void addStateChangeListener(@NonNull String key, @NonNull StateChangeListener listener) {
        ensureNotDestroyed();
        mStateListeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove listener for specific state key.
     *
     * @param key State key
     * @param listener Listener to remove
     */
    public void removeStateChangeListener(@NonNull String key, @NonNull StateChangeListener listener) {
        List<StateChangeListener> listeners = mStateListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                mStateListeners.remove(key);
            }
        }
    }

    // ==================== EVENT MANAGEMENT ====================

    /**
     * Emit one-time event to all listeners.
     *
     * @param event Event to emit
     */
    protected void emitEvent(@NonNull ViewModelEvent event) {
        ensureNotDestroyed();

        Log.d(TAG, "Emitting event: " + event.getType());
        for (EventListener listener : mEventListeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying event listener for event: " + event.getType(), e);
            }
        }
    }

    /**
     * Add event listener.
     *
     * @param listener Listener to add
     */
    public void addEventListener(@NonNull EventListener listener) {
        ensureNotDestroyed();
        mEventListeners.add(listener);
    }

    /**
     * Remove event listener.
     *
     * @param listener Listener to remove
     */
    public void removeEventListener(@NonNull EventListener listener) {
        mEventListeners.remove(listener);
    }

    // ==================== LOADING STATE MANAGEMENT ====================

    /**
     * Set loading state for specific operation.
     *
     * @param operation Operation identifier
     * @param loading Loading state
     */
    protected void setLoading(@NonNull String operation, boolean loading) {
        ensureNotDestroyed();

        Boolean oldState = mLoadingStates.put(operation, loading);
        if (oldState == null || oldState != loading) {
            for (LoadingStateListener listener : mLoadingListeners) {
                try {
                    listener.onLoadingStateChanged(operation, loading);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying loading state listener for operation: " + operation, e);
                }
            }
        }
    }

    /**
     * Check if specific operation is loading.
     *
     * @param operation Operation identifier
     * @return true if operation is loading
     */
    public boolean isLoading(@NonNull String operation) {
        return mLoadingStates.getOrDefault(operation, false);
    }

    /**
     * Check if any operation is loading.
     *
     * @return true if any operation is loading
     */
    public boolean isAnyLoading() {
        return mLoadingStates.values().stream().anyMatch(Boolean::booleanValue);
    }

    /**
     * Add loading state listener.
     *
     * @param listener Listener to add
     */
    public void addLoadingStateListener(@NonNull LoadingStateListener listener) {
        ensureNotDestroyed();
        mLoadingListeners.add(listener);
    }

    /**
     * Remove loading state listener.
     *
     * @param listener Listener to remove
     */
    public void removeLoadingStateListener(@NonNull LoadingStateListener listener) {
        mLoadingListeners.remove(listener);
    }

    // ==================== ERROR STATE MANAGEMENT ====================

    /**
     * Set error state for specific operation.
     *
     * @param operation Operation identifier
     * @param error Error message or null to clear
     */
    protected void setError(@NonNull String operation, @Nullable String error) {
        ensureNotDestroyed();

        if (error != null) {
            mErrorStates.put(operation, error);
        } else {
            mErrorStates.remove(operation);
        }

        for (ErrorStateListener listener : mErrorListeners) {
            try {
                listener.onErrorStateChanged(operation, error);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying error state listener for operation: " + operation, e);
            }
        }
    }

    /**
     * Notify error without storing state.
     *
     * @param operation Operation identifier
     * @param error Error message
     */
    protected void notifyError(@NonNull String operation, @NonNull String error) {
        ensureNotDestroyed();

        for (ErrorStateListener listener : mErrorListeners) {
            try {
                listener.onErrorStateChanged(operation, error);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying error state listener for operation: " + operation, e);
            }
        }
    }

    /**
     * Get current error for operation.
     *
     * @param operation Operation identifier
     * @return Current error message or null
     */
    @Nullable
    public String getError(@NonNull String operation) {
        return mErrorStates.get(operation);
    }

    /**
     * Check if operation has error.
     *
     * @param operation Operation identifier
     * @return true if operation has error
     */
    public boolean hasError(@NonNull String operation) {
        return mErrorStates.containsKey(operation);
    }

    /**
     * Clear error for operation.
     *
     * @param operation Operation identifier
     */
    public void clearError(@NonNull String operation) {
        setError(operation, null);
    }

    /**
     * Clear all errors.
     */
    public void clearAllErrors() {
        List<String> operations = new ArrayList<>(mErrorStates.keySet());
        for (String operation : operations) {
            clearError(operation);
        }
    }

    /**
     * Add error state listener.
     *
     * @param listener Listener to add
     */
    public void addErrorStateListener(@NonNull ErrorStateListener listener) {
        ensureNotDestroyed();
        mErrorListeners.add(listener);
    }

    /**
     * Remove error state listener.
     *
     * @param listener Listener to remove
     */
    public void removeErrorStateListener(@NonNull ErrorStateListener listener) {
        mErrorListeners.remove(listener);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Ensure ViewModel is not destroyed.
     */
    private void ensureNotDestroyed() {
        if (mDestroyed) {
            throw new IllegalStateException("ViewModel is destroyed and cannot be used");
        }
    }

    /**
     * Compare objects for equality handling nulls.
     */
    private boolean objectEquals(@Nullable Object a, @Nullable Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Get debug information about ViewModel state.
     */
    public String getDebugInfo() {
        return String.format(
                "%s{initialized=%s, destroyed=%s, stateKeys=%d, " +
                        "loadingOperations=%d, errorOperations=%d, listeners=%d}",
                getClass().getSimpleName(), mInitialized, mDestroyed,
                mState.size(), mLoadingStates.size(), mErrorStates.size(),
                mStateListeners.size() + mEventListeners.size() +
                        mLoadingListeners.size() + mErrorListeners.size()
        );
    }

    // ==================== INTERFACES ====================

    /**
     * Listener for state changes.
     */
    public interface StateChangeListener {
        /**
         * Called when state changes.
         *
         * @param key State key that changed
         * @param newValue New value (may be null)
         */
        void onStateChanged(@NonNull String key, @Nullable Object newValue);
    }

    /**
     * Listener for one-time events.
     */
    public interface EventListener {
        /**
         * Called when event is emitted.
         *
         * @param event Event that was emitted
         */
        void onEvent(@NonNull ViewModelEvent event);
    }

    /**
     * Listener for loading state changes.
     */
    public interface LoadingStateListener {
        /**
         * Called when loading state changes.
         *
         * @param operation Operation identifier
         * @param loading New loading state
         */
        void onLoadingStateChanged(@NonNull String operation, boolean loading);
    }

    /**
     * Listener for error state changes.
     */
    public interface ErrorStateListener {
        /**
         * Called when error state changes.
         *
         * @param operation Operation identifier
         * @param error Error message or null if cleared
         */
        void onErrorStateChanged(@NonNull String operation, @Nullable String error);
    }

    // ==================== EVENT CLASSES ====================

    /**
     * Base class for ViewModel events.
     */
    public static abstract class ViewModelEvent {
        private final String type;
        private final long timestamp;

        protected ViewModelEvent(@NonNull String type) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        @NonNull
        public String getType() { return type; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("%s{type='%s', timestamp=%d}",
                                 getClass().getSimpleName(), type, timestamp);
        }
    }

    /**
     * Navigation event for triggering navigation actions.
     */
    public static class NavigationEvent extends ViewModelEvent {
        private final String destination;
        private final Map<String, Object> arguments;

        public NavigationEvent(@NonNull String destination) {
            this(destination, new HashMap<>());
        }

        public NavigationEvent(@NonNull String destination, @NonNull Map<String, Object> arguments) {
            super("NAVIGATION");
            this.destination = destination;
            this.arguments = new HashMap<>(arguments);
        }

        @NonNull
        public String getDestination() { return destination; }
        @NonNull
        public Map<String, Object> getArguments() { return new HashMap<>(arguments); }

        public void addArgument(@NonNull String key, @Nullable Object value) {
            arguments.put(key, value);
        }

        @Nullable
        public Object getArgument(@NonNull String key) {
            return arguments.get(key);
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getArgument(@NonNull String key, @NonNull Class<T> clazz) {
            Object value = arguments.get(key);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
    }

    /**
     * UI action event for triggering UI-specific actions.
     */
    public static class UIActionEvent extends ViewModelEvent {
        private final String action;
        private final Map<String, Object> data;

        public UIActionEvent(@NonNull String action) {
            this(action, new HashMap<>());
        }

        public UIActionEvent(@NonNull String action, @NonNull Map<String, Object> data) {
            super("UI_ACTION");
            this.action = action;
            this.data = new HashMap<>(data);
        }

        @NonNull
        public String getAction() { return action; }
        @NonNull
        public Map<String, Object> getData() { return new HashMap<>(data); }

        @Nullable
        public Object getData(@NonNull String key) {
            return data.get(key);
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getData(@NonNull String key, @NonNull Class<T> clazz) {
            Object value = data.get(key);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
    }
}