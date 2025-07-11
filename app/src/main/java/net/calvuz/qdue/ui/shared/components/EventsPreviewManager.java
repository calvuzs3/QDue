package net.calvuz.qdue.ui.shared.components;

import android.content.Context;
import androidx.annotation.NonNull;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.shared.base.BaseEventsPreview;
import net.calvuz.qdue.ui.shared.interfaces.EventsPreviewInterface;
import net.calvuz.qdue.utils.Log;
import java.time.LocalDate;
import java.util.List;

/**
 * Manager class that handles adaptive strategy for events preview
 * Delegates to appropriate implementation based on view type
 */
public class EventsPreviewManager implements EventsPreviewInterface {

    private static final String TAG = "EventsPreviewManager";

    private final Context mContext;
    private EventsPreviewInterface mCurrentImplementation;
    private EventsPreviewListener mListener;
    private ViewType mCurrentViewType;

    public enum ViewType {
        DAYS_LIST, CALENDAR_VIEW
    }

    public EventsPreviewManager(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Set the current view type and initialize appropriate implementation
     */
    public void setViewType(ViewType viewType) {
        if (mCurrentViewType == viewType) {
            return; // No change needed
        }

        Log.d(TAG, "Switching view type: " + mCurrentViewType + " -> " + viewType);

        // Hide current preview if showing
        if (mCurrentImplementation != null && mCurrentImplementation.isEventsPreviewShowing()) {
            mCurrentImplementation.hideEventsPreview();
        }

        // Create new implementation
        switch (viewType) {
            case DAYS_LIST:
                mCurrentImplementation = new DaysListEventsPreview(mContext);
                break;
            case CALENDAR_VIEW:
                mCurrentImplementation = new CalendarEventsPreview(mContext);
                break;
            default:
                Log.w(TAG, "Unknown view type: " + viewType);
                return;
        }

        // Set listener on new implementation
        if (mListener != null) {
            mCurrentImplementation.setEventsPreviewListener(mListener);
        }

        mCurrentViewType = viewType;
        Log.d(TAG, "View type switched to: " + viewType);
    }

    @Override
    public void showEventsPreview(LocalDate date, List<LocalEvent> events, android.view.View anchorView) {
        if (mCurrentImplementation != null) {
            mCurrentImplementation.showEventsPreview(date, events, anchorView);
        } else {
            Log.w(TAG, "No implementation set for current view type");
        }
    }

    @Override
    public void hideEventsPreview() {
        if (mCurrentImplementation != null) {
            mCurrentImplementation.hideEventsPreview();
        }
    }

    @Override
    public boolean isEventsPreviewShowing() {
        return mCurrentImplementation != null && mCurrentImplementation.isEventsPreviewShowing();
    }

    @Override
    public void onEventQuickAction(EventQuickAction action, LocalEvent event, LocalDate date) {
        if (mCurrentImplementation != null) {
            mCurrentImplementation.onEventQuickAction(action, event, date);
        }
    }

    @Override
    public void onEventsGeneralAction(EventGeneralAction action, LocalDate date) {
        if (mCurrentImplementation != null) {
            mCurrentImplementation.onEventsGeneralAction(action, date);
        }
    }

    @Override
    public void setEventsPreviewListener(EventsPreviewListener listener) {
        mListener = listener;
        if (mCurrentImplementation != null) {
            mCurrentImplementation.setEventsPreviewListener(listener);
        }
    }

    /**
     * Get current view type
     */
    public ViewType getCurrentViewType() {
        return mCurrentViewType;
    }

    /**
     * Lifecycle management
     */
    public void onPause() {
        if (mCurrentImplementation instanceof BaseEventsPreview) {
            ((BaseEventsPreview) mCurrentImplementation).onPause();
        }
    }

    public void onDestroy() {
        if (mCurrentImplementation instanceof BaseEventsPreview) {
            ((BaseEventsPreview) mCurrentImplementation).onDestroy();
        }
        mCurrentImplementation = null;
        mListener = null;
    }

    /**
     * Get current implementation (for back press handling)
     */
    public EventsPreviewInterface getCurrentImplementation() {
        return mCurrentImplementation;
    }

    /**
     * Debug current state
     */
    public void debugState() {
        Log.d(TAG, "=== EVENTS PREVIEW MANAGER DEBUG (Phase 3) ===");
        Log.d(TAG, "Current View Type: " + mCurrentViewType);
        Log.d(TAG, "Implementation: " + (mCurrentImplementation != null ?
                mCurrentImplementation.getClass().getSimpleName() : "null"));
        Log.d(TAG, "Listener: " + (mListener != null ? "SET" : "NULL"));
        Log.d(TAG, "Currently Showing: " + isEventsPreviewShowing());

        if (mCurrentImplementation instanceof BaseEventsPreview) {
            ((BaseEventsPreview) mCurrentImplementation).debugState();
        }

        // Phase 3 specific debugging
        if (mCurrentImplementation instanceof DaysListEventsPreview daysListPreview) {
            Log.d(TAG, "DaysList Expanded Cards: " + (daysListPreview.hasExpandedCard() ? "YES" : "NO"));
            Log.d(TAG, "DaysList Expanded Date: " + daysListPreview.getCurrentlyExpandedDate());
        }

        Log.d(TAG, "=== END MANAGER DEBUG (Phase 3) ===");
    }

}
