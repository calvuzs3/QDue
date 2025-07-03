package net.calvuz.qdue.ui.shared;

import android.content.Context;
import android.view.View;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * CalendarEventsPreview - Updated implementation using Bottom Sheet
 *
 * This class now uses CalendarEventsBottomSheet for a better user experience
 * on small calendar cells (48x48dp).
 */
public class CalendarEventsPreview extends BaseEventsPreview {

    private static final String TAG = "CalendarEventsPreview";

    // Delegate to bottom sheet implementation
    private CalendarEventsBottomSheet mBottomSheetImpl;

    public CalendarEventsPreview(Context context) {
        super(context);

        // Initialize bottom sheet implementation
        mBottomSheetImpl = new CalendarEventsBottomSheet(context);

        // Set up listener forwarding
        setupListenerForwarding();

        Log.d(TAG, "CalendarEventsPreview initialized with bottom sheet");
    }

    // ==================== LISTENER FORWARDING ====================

    /**
     * Setup listener forwarding to delegate to bottom sheet
     */
    private void setupListenerForwarding() {
        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.setEventsPreviewListener(new EventsPreviewListener() {
                @Override
                public void onEventQuickAction(EventQuickAction action, LocalEvent event, LocalDate date) {
                    // Forward to our listener
                    if (mListener != null) {
                        mListener.onEventQuickAction(action, event, date);
                    }
                }

                @Override
                public void onEventsGeneralAction(EventGeneralAction action, LocalDate date) {
                    // Forward to our listener
                    if (mListener != null) {
                        mListener.onEventsGeneralAction(action, date);
                    }
                }

                @Override
                public void onEventsPreviewShown(LocalDate date, int eventCount) {
                    // Forward to our listener
                    if (mListener != null) {
                        mListener.onEventsPreviewShown(date, eventCount);
                    }
                }

                @Override
                public void onEventsPreviewHidden(LocalDate date) {
                    // Forward to our listener
                    if (mListener != null) {
                        mListener.onEventsPreviewHidden(date);
                    }
                }
            });
        }
    }

    // ==================== BASE EVENTS PREVIEW IMPLEMENTATION ====================

    @Override
    protected void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, View anchorView) {
        Log.d(TAG, "Delegating to bottom sheet: showEventsPreview for " + date);

        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.showEventsPreview(date, events, anchorView);
        } else {
            Log.e(TAG, "Bottom sheet implementation is null");
        }
    }

    @Override
    protected void hideEventsPreviewImpl() {
        Log.d(TAG, "Delegating to bottom sheet: hideEventsPreview");

        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.hideEventsPreview();
        }
    }

    @Override
    protected String getViewType() {
        return "CalendarView";
    }

    // ==================== ADDITIONAL METHODS ====================

    @Override
    public void setEventsPreviewListener(EventsPreviewListener listener) {
        // Store listener and update forwarding
        super.setEventsPreviewListener(listener);
        setupListenerForwarding();
    }

    @Override
    public boolean isEventsPreviewShowing() {
        return mBottomSheetImpl != null && mBottomSheetImpl.isEventsPreviewShowing();
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    public void onPause() {
        super.onPause();

        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.onDestroy();
            mBottomSheetImpl = null;
        }
    }

    // ==================== DEBUG METHODS ====================

    @Override
    public void debugState() {
        super.debugState();

        Log.d(TAG, "=== CALENDAR EVENTS PREVIEW DEBUG ===");
        Log.d(TAG, "Bottom Sheet Implementation: " + (mBottomSheetImpl != null ? "active" : "null"));

        if (mBottomSheetImpl != null) {
            mBottomSheetImpl.debugState();
        }

        Log.d(TAG, "=== END CALENDAR EVENTS PREVIEW DEBUG ===");
    }
}