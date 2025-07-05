package net.calvuz.qdue.ui.shared;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.shared.interfaces.EventsPreviewInterface;
import net.calvuz.qdue.utils.Log;
import java.time.LocalDate;
import java.util.List;

/**
 * Base implementation for events preview functionality
 * Provides common logic for both DaysList and CalendarView implementations
 */
public abstract class BaseEventsPreview implements EventsPreviewInterface {

    protected static final String TAG = "BaseEventsPreview";

    protected final Context mContext;
    protected EventsPreviewListener mListener;
    protected boolean mIsShowing = false;
    protected LocalDate mCurrentDate;
    protected List<LocalEvent> mCurrentEvents;
    protected View mCurrentAnchorView;

    public BaseEventsPreview(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public void setEventsPreviewListener(EventsPreviewListener listener) {
        mListener = listener;
    }

    @Override
    public boolean isEventsPreviewShowing() {
        return mIsShowing;
    }

    @Override
    public void showEventsPreview(LocalDate date, List<LocalEvent> events, View anchorView) {
        Log.d(TAG, "showEventsPreview: " + date + " with " + events.size() + " events");

        // Validate input
        if (date == null || events == null || anchorView == null) {
            Log.w(TAG, "Invalid parameters for showEventsPreview");
            return;
        }

        // Hide current preview if showing
        if (mIsShowing) {
            hideEventsPreview();
        }

        // Store current state
        mCurrentDate = date;
        mCurrentEvents = events;
        mCurrentAnchorView = anchorView;
        mIsShowing = true;

        // Delegate to specific implementation
        showEventsPreviewImpl(date, events, anchorView);

        // Notify listener
        if (mListener != null) {
            mListener.onEventsPreviewShown(date, events.size());
        }
    }

    @Override
    public void hideEventsPreview() {
        if (!mIsShowing) {
            return;
        }

        Log.d(TAG, "hideEventsPreview: " + mCurrentDate);

        LocalDate dateForCallback = mCurrentDate;

        // Delegate to specific implementation
        hideEventsPreviewImpl();

        // Clear state
        mIsShowing = false;
        mCurrentDate = null;
        mCurrentEvents = null;
        mCurrentAnchorView = null;

        // Notify listener
        if (mListener != null) {
            mListener.onEventsPreviewHidden(dateForCallback);
        }
    }

    @Override
    public void onEventQuickAction(EventQuickAction action, LocalEvent event, LocalDate date) {
        Log.d(TAG, "onEventQuickAction: " + action + " for event: " + event.getTitle());

        // Delegate to listener
        if (mListener != null) {
            mListener.onEventQuickAction(action, event, date);
        }

        // Handle built-in actions if needed
        handleBuiltInQuickAction(action, event, date);
    }

    @Override
    public void onEventsGeneralAction(EventGeneralAction action, LocalDate date) {
        Log.d(TAG, "onEventsGeneralAction: " + action + " for date: " + date);

        // Delegate to listener
        if (mListener != null) {
            mListener.onEventsGeneralAction(action, date);
        }

        // Handle built-in actions if needed
        handleBuiltInGeneralAction(action, date);
    }

    // ===========================================
    // Abstract Methods for Subclasses
    // ===========================================

    /**
     * Specific implementation for showing events preview
     * Subclasses implement view-specific logic (expandable card vs bottom sheet)
     */
    protected abstract void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, View anchorView);

    /**
     * Specific implementation for hiding events preview
     * Subclasses implement view-specific logic
     */
    protected abstract void hideEventsPreviewImpl();

    /**
     * Get view type identifier for logging and debugging
     */
    protected abstract String getViewType();

    // ===========================================
    // Common Helper Methods
    // ===========================================

    /**
     * Handle built-in quick actions that don't require external handling
     */
    protected void handleBuiltInQuickAction(EventQuickAction action, LocalEvent event, LocalDate date) {
        switch (action) {
            case TOGGLE_COMPLETE:
                // Handle completion toggle locally if needed
                Log.d(TAG, "Toggling completion for event: " + event.getTitle());
                break;
            case DUPLICATE:
                // Handle duplication locally if needed
                Log.d(TAG, "Duplicating event: " + event.getTitle());
                break;
            default:
                // Other actions (EDIT, DELETE) are handled by listener
                break;
        }
    }

    /**
     * Handle built-in general actions that don't require external handling
     */
    protected void handleBuiltInGeneralAction(EventGeneralAction action, LocalDate date) {
        switch (action) {
            case REFRESH_EVENTS:
                Log.d(TAG, "Refreshing events for date: " + date);
                // Could trigger local refresh if needed
                break;
            default:
                // Other actions are handled by listener
                break;
        }
    }

    /**
     * Utility method to format event count
     */
    protected String formatEventCount(int count) {
        if (count == 0) return "Nessun evento";
        if (count == 1) return "1 evento";
        return count + " eventi";
    }

    /**
     * Validate if event can be edited
     *
     * @param event The event to validate
     * @return true if event can be edited
     */
    protected boolean canEditEvent(LocalEvent event) {
        if (event == null) {
            Log.w(TAG, "Cannot edit null event");
            return false;
        }

        if (event.getId() == null || event.getId().trim().isEmpty()) {
            Log.w(TAG, "Cannot edit event with null/empty ID");
            return false;
        }

        // Additional validation rules can be added here
        // e.g., check if event is read-only, check permissions, etc.

        return true;
    }

    /**
     * Utility method to check if an event can be deleted
     */
    protected boolean canDeleteEvent(LocalEvent event) {
        // Add business logic here
        return event != null;
    }

    // ===========================================
    // Lifecycle Methods
    // ===========================================

    /**
     * Called when the parent view is paused
     * Subclasses can override for cleanup
     */
    public void onPause() {
        if (mIsShowing) {
            hideEventsPreview();
        }
    }

    /**
     * Called when the parent view is destroyed
     * Subclasses can override for cleanup
     */
    public void onDestroy() {
        hideEventsPreview();
        mListener = null;
    }

    // ===========================================
    // Debug Methods
    // ===========================================

    /**
     * Debug current state
     */
    public void debugState() {
        Log.d(TAG, "=== " + getViewType().toUpperCase() + " EVENTS PREVIEW DEBUG ===");
        Log.d(TAG, "Is Showing: " + mIsShowing);
        Log.d(TAG, "Current Date: " + mCurrentDate);
        Log.d(TAG, "Events Count: " + (mCurrentEvents != null ? mCurrentEvents.size() : "null"));
        Log.d(TAG, "Anchor View: " + (mCurrentAnchorView != null ? mCurrentAnchorView.getClass().getSimpleName() : "null"));
        Log.d(TAG, "Listener: " + (mListener != null ? "SET" : "NULL"));
        Log.d(TAG, "=== END " + getViewType().toUpperCase() + " DEBUG ===");
    }
}
