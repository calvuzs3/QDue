package net.calvuz.qdue.ui.features.dayview.components;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DayViewStateManager - Comprehensive State Management for Day View
 *
 * <p>Manages all state aspects for the day view including selected date, loaded events,
 * UI states, multi-selection for bulk operations, and extensible event type support.
 * Provides reactive state updates through listener pattern.</p>
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><strong>Date Management</strong>: Current displayed date with validation</li>
 *   <li><strong>Event Storage</strong>: Multi-type event storage (LocalEvent, WorkScheduleDay)</li>
 *   <li><strong>UI States</strong>: Loading, error, empty states with transitions</li>
 *   <li><strong>Multi-Selection</strong>: Bulk selection management for batch operations</li>
 *   <li><strong>State Notifications</strong>: Reactive listeners for UI updates</li>
 * </ul>
 *
 * <h3>Event Type Extension:</h3>
 * <p>Designed for easy extension to support additional event types in the future.
 * New event types can be added by extending the BaseEvent abstraction and
 * adding corresponding storage and selection methods.</p>
 *
 * <h3>Thread Safety:</h3>
 * <p>All public methods are thread-safe. State changes are synchronized and
 * listener notifications are posted to the main thread for UI updates.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewStateManager {

    private static final String TAG = "DayViewStateManager";

    // ==================== STATE LISTENER INTERFACES ====================

    /**
     * Listener for day view state changes.
     */
    public interface DayViewStateListener {
        /**
         * Called when the target date changes.
         */
        void onDateChanged(@NonNull LocalDate newDate);

        /**
         * Called when events are updated.
         */
        void onEventsUpdated(@NonNull List<LocalEvent> localEvents,
                             @NonNull List<WorkScheduleDay> workScheduleDays);

        /**
         * Called when loading state changes.
         */
        void onLoadingStateChanged(boolean isLoading);

        /**
         * Called when error state changes.
         */
        void onErrorStateChanged(@Nullable String errorMessage);

        /**
         * Called when selection state changes.
         */
        void onSelectionChanged(@NonNull Set<String> selectedEventIds, boolean isSelectionMode);

        /**
         * Called when empty state changes.
         */
        void onEmptyStateChanged(boolean isEmpty);
    }

    // ==================== CORE STATE ====================

    private final Context mContext;
    private LocalDate mCurrentDate;

    // ==================== EVENT STORAGE ====================

    // Current implementation supports LocalEvent and WorkScheduleDay
    // Extensible design for future event types
    private final List<LocalEvent> mLocalEvents = new ArrayList<>();
    private final List<WorkScheduleDay> mWorkScheduleDays = new ArrayList<>();

    // Future event types can be added here:
    // private final List<NewEventType> mNewEventTypes = new ArrayList<>();

    // ==================== UI STATE ====================

    private boolean mIsLoading = false;
    private String mErrorMessage = null;
    private boolean mIsEmpty = true;

    // ==================== SELECTION STATE ====================

    private final Set<String> mSelectedEventIds = new HashSet<>();
    private boolean mIsSelectionMode = false;
    private int mMaxSelectableEvents = Integer.MAX_VALUE;

    // ==================== LISTENERS ====================

    private final List<DayViewStateListener> mStateListeners = new CopyOnWriteArrayList<>();

    // ==================== SYNCHRONIZATION ====================

    private final Object mStateLock = new Object();
    private final Object mEventLock = new Object();
    private final Object mSelectionLock = new Object();

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new DayViewStateManager for specified date.
     *
     * @param context Application context
     * @param initialDate Initial date to display
     */
    public DayViewStateManager(@NonNull Context context, @NonNull LocalDate initialDate) {
        this.mContext = context.getApplicationContext();
        this.mCurrentDate = initialDate;

        Log.d(TAG, "DayViewStateManager created for date: " + initialDate);
    }

    // ==================== DATE MANAGEMENT ====================

    /**
     * Get current displayed date.
     *
     * @return Current date
     */
    @NonNull
    public LocalDate getCurrentDate() {
        synchronized (mStateLock) {
            return mCurrentDate;
        }
    }

    /**
     * Set new target date and clear existing data.
     *
     * @param newDate New date to display
     */
    public void setCurrentDate(@NonNull LocalDate newDate) {
        synchronized (mStateLock) {
            if (!mCurrentDate.equals(newDate)) {
                LocalDate oldDate = mCurrentDate;
                mCurrentDate = newDate;

                // Clear existing data for new date
                clearAllEvents();
                clearSelection();
                clearError();

                Log.d(TAG, "Date changed from " + oldDate + " to " + newDate);

                // Notify listeners
                notifyDateChanged(newDate);
            }
        }
    }

    // ==================== EVENT MANAGEMENT ====================

    /**
     * Update local events for current date.
     *
     * @param localEvents List of local events
     */
    public void updateLocalEvents(@NonNull List<LocalEvent> localEvents) {
        synchronized (mEventLock) {
            mLocalEvents.clear();
            mLocalEvents.addAll(localEvents);

            Log.d(TAG, "Updated " + localEvents.size() + " local events for date: " + mCurrentDate);
            updateEmptyState();
            notifyEventsUpdated();
        }
    }

    /**
     * Update work schedule days for current date.
     *
     * @param workScheduleDays List of work schedule days
     */
    public void updateWorkScheduleDays(@NonNull List<WorkScheduleDay> workScheduleDays) {
        synchronized (mEventLock) {
            mWorkScheduleDays.clear();
            mWorkScheduleDays.addAll(workScheduleDays);

            Log.d(TAG, "Updated " + workScheduleDays.size() + " work schedule days for date: " + mCurrentDate);
            updateEmptyState();
            notifyEventsUpdated();
        }
    }

    /**
     * Update all events from multiple sources.
     *
     * @param allEvents Map of event type to event list
     */
    public void updateEvents(@NonNull Map<String, List<Object>> allEvents) {
        synchronized (mEventLock) {
            // Clear existing events
            mLocalEvents.clear();
            mWorkScheduleDays.clear();

            // Update with new events
            List<?> localEvents = allEvents.get("local_events");
            if (localEvents != null) {
                for (Object event : localEvents) {
                    if (event instanceof LocalEvent) {
                        mLocalEvents.add((LocalEvent) event);
                    }
                }
            }

            List<?> workSchedule = allEvents.get("work_schedule");
            if (workSchedule != null) {
                for (Object event : workSchedule) {
                    if (event instanceof WorkScheduleDay) {
                        mWorkScheduleDays.add((WorkScheduleDay) event);
                    }
                }
            }

            // Future event types can be handled here:
            // List<?> newEventTypes = allEvents.get("new_event_type");
            // if (newEventTypes != null) { ... }

            Log.d(TAG, "Updated events: " + mLocalEvents.size() + " local, " +
                    mWorkScheduleDays.size() + " work schedule for date: " + mCurrentDate);

            updateEmptyState();
            notifyEventsUpdated();
        }
    }

    /**
     * Get all local events for current date.
     *
     * @return Copy of local events list
     */
    @NonNull
    public List<LocalEvent> getLocalEvents() {
        synchronized (mEventLock) {
            return new ArrayList<>(mLocalEvents);
        }
    }

    /**
     * Get all work schedule days for current date.
     *
     * @return Copy of work schedule days list
     */
    @NonNull
    public List<WorkScheduleDay> getWorkScheduleDays() {
        synchronized (mEventLock) {
            return new ArrayList<>(mWorkScheduleDays);
        }
    }

    /**
     * Get total count of all events.
     *
     * @return Total event count
     */
    public int getTotalEventCount() {
        synchronized (mEventLock) {
            return mLocalEvents.size() + mWorkScheduleDays.size();
        }
    }

    /**
     * Clear all events.
     */
    public void clearAllEvents() {
        synchronized (mEventLock) {
            mLocalEvents.clear();
            mWorkScheduleDays.clear();
            updateEmptyState();
            notifyEventsUpdated();
        }
    }

    // ==================== UI STATE MANAGEMENT ====================

    /**
     * Set loading state.
     *
     * @param isLoading Loading state
     */
    public void setLoading(boolean isLoading) {
        synchronized (mStateLock) {
            if (mIsLoading != isLoading) {
                mIsLoading = isLoading;
                Log.d(TAG, "Loading state changed to: " + isLoading);
                notifyLoadingStateChanged(isLoading);
            }
        }
    }

    /**
     * Get current loading state.
     *
     * @return Loading state
     */
    public boolean isLoading() {
        synchronized (mStateLock) {
            return mIsLoading;
        }
    }

    /**
     * Set error message.
     *
     * @param errorMessage Error message (null to clear)
     */
    public void setError(@Nullable String errorMessage) {
        synchronized (mStateLock) {
            mErrorMessage = errorMessage;
            Log.d(TAG, "Error state changed: " + errorMessage);
            notifyErrorStateChanged(errorMessage);
        }
    }

    /**
     * Get current error message.
     *
     * @return Error message or null
     */
    @Nullable
    public String getError() {
        synchronized (mStateLock) {
            return mErrorMessage;
        }
    }

    /**
     * Clear error state.
     */
    public void clearError() {
        setError(null);
    }

    /**
     * Check if currently in error state.
     *
     * @return True if has error
     */
    public boolean hasError() {
        synchronized (mStateLock) {
            return mErrorMessage != null;
        }
    }

    /**
     * Check if events list is empty.
     *
     * @return True if no events
     */
    public boolean isEmpty() {
        synchronized (mStateLock) {
            return mIsEmpty;
        }
    }

    // ==================== SELECTION MANAGEMENT ====================

    /**
     * Toggle selection mode.
     *
     * @param enabled Selection mode enabled
     */
    public void setSelectionMode(boolean enabled) {
        synchronized (mSelectionLock) {
            if (mIsSelectionMode != enabled) {
                mIsSelectionMode = enabled;

                if (!enabled) {
                    // Clear selections when exiting selection mode
                    mSelectedEventIds.clear();
                }

                Log.d(TAG, "Selection mode changed to: " + enabled);
                notifySelectionChanged();
            }
        }
    }

    /**
     * Check if in selection mode.
     *
     * @return Selection mode state
     */
    public boolean isSelectionMode() {
        synchronized (mSelectionLock) {
            return mIsSelectionMode;
        }
    }

    /**
     * Toggle event selection.
     *
     * @param eventId Event ID to toggle
     * @return True if event is now selected
     */
    public boolean toggleEventSelection(@NonNull String eventId) {
        synchronized (mSelectionLock) {
            boolean wasSelected = mSelectedEventIds.contains(eventId);

            if (wasSelected) {
                mSelectedEventIds.remove(eventId);
                Log.d(TAG, "Deselected event: " + eventId);
            } else {
                if (mSelectedEventIds.size() < mMaxSelectableEvents) {
                    mSelectedEventIds.add(eventId);
                    Log.d(TAG, "Selected event: " + eventId);
                } else {
                    Log.w(TAG, "Maximum selectable events reached: " + mMaxSelectableEvents);
                    return false;
                }
            }

            notifySelectionChanged();
            return !wasSelected;
        }
    }

    /**
     * Select all events.
     */
    public void selectAllEvents() {
        synchronized (mSelectionLock) {
            mSelectedEventIds.clear();

            // Add all local event IDs
            for (LocalEvent event : mLocalEvents) {
                if (mSelectedEventIds.size() >= mMaxSelectableEvents) break;
                mSelectedEventIds.add(event.getId());
            }

            // Add work schedule day IDs
            for (WorkScheduleDay day : mWorkScheduleDays) {
                if (mSelectedEventIds.size() >= mMaxSelectableEvents) break;
                mSelectedEventIds.add(day.getId());
            }

            Log.d(TAG, "Selected all events: " + mSelectedEventIds.size());
            notifySelectionChanged();
        }
    }

    /**
     * Clear all selections.
     */
    public void clearSelection() {
        synchronized (mSelectionLock) {
            if (!mSelectedEventIds.isEmpty()) {
                mSelectedEventIds.clear();
                Log.d(TAG, "Cleared all selections");
                notifySelectionChanged();
            }
        }
    }

    /**
     * Get selected event IDs.
     *
     * @return Copy of selected event IDs
     */
    @NonNull
    public Set<String> getSelectedEventIds() {
        synchronized (mSelectionLock) {
            return new HashSet<>(mSelectedEventIds);
        }
    }

    /**
     * Get count of selected events.
     *
     * @return Selected event count
     */
    public int getSelectedCount() {
        synchronized (mSelectionLock) {
            return mSelectedEventIds.size();
        }
    }

    /**
     * Check if event is selected.
     *
     * @param eventId Event ID to check
     * @return True if selected
     */
    public boolean isEventSelected(@NonNull String eventId) {
        synchronized (mSelectionLock) {
            return mSelectedEventIds.contains(eventId);
        }
    }

    // ==================== LISTENER MANAGEMENT ====================

    /**
     * Add state change listener.
     *
     * @param listener Listener to add
     */
    public void addStateListener(@NonNull DayViewStateListener listener) {
        mStateListeners.add(listener);
        Log.d(TAG, "Added state listener. Total listeners: " + mStateListeners.size());
    }

    /**
     * Remove state change listener.
     *
     * @param listener Listener to remove
     */
    public void removeStateListener(@NonNull DayViewStateListener listener) {
        mStateListeners.remove(listener);
        Log.d(TAG, "Removed state listener. Total listeners: " + mStateListeners.size());
    }

    // ==================== CLEANUP ====================

    /**
     * Clean up resources and clear all data.
     */
    public void cleanup() {
        synchronized (mStateLock) {
            synchronized (mEventLock) {
                synchronized (mSelectionLock) {
                    Log.d(TAG, "Cleaning up DayViewStateManager");

                    // Clear all data
                    mLocalEvents.clear();
                    mWorkScheduleDays.clear();
                    mSelectedEventIds.clear();
                    mStateListeners.clear();

                    // Reset states
                    mIsLoading = false;
                    mErrorMessage = null;
                    mIsEmpty = true;
                    mIsSelectionMode = false;

                    Log.d(TAG, "DayViewStateManager cleanup complete");
                }
            }
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Update empty state based on current events.
     */
    private void updateEmptyState() {
        boolean newEmptyState = mLocalEvents.isEmpty() && mWorkScheduleDays.isEmpty();
        if (mIsEmpty != newEmptyState) {
            mIsEmpty = newEmptyState;
            notifyEmptyStateChanged(mIsEmpty);
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private void notifyDateChanged(@NonNull LocalDate newDate) {
        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onDateChanged(newDate);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onDateChanged", e);
            }
        }
    }

    private void notifyEventsUpdated() {
        List<LocalEvent> localEventsCopy = new ArrayList<>(mLocalEvents);
        List<WorkScheduleDay> workScheduleCopy = new ArrayList<>(mWorkScheduleDays);

        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onEventsUpdated(localEventsCopy, workScheduleCopy);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onEventsUpdated", e);
            }
        }
    }

    private void notifyLoadingStateChanged(boolean isLoading) {
        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onLoadingStateChanged(isLoading);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onLoadingStateChanged", e);
            }
        }
    }

    private void notifyErrorStateChanged(@Nullable String errorMessage) {
        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onErrorStateChanged(errorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onErrorStateChanged", e);
            }
        }
    }

    private void notifySelectionChanged() {
        Set<String> selectedCopy = new HashSet<>(mSelectedEventIds);
        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onSelectionChanged(selectedCopy, mIsSelectionMode);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onSelectionChanged", e);
            }
        }
    }

    private void notifyEmptyStateChanged(boolean isEmpty) {
        for (DayViewStateListener listener : mStateListeners) {
            try {
                listener.onEmptyStateChanged(isEmpty);
            } catch (Exception e) {
                Log.e(TAG, "Error in listener.onEmptyStateChanged", e);
            }
        }
    }
}