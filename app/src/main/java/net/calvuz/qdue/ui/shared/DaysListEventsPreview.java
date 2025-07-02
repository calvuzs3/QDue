package net.calvuz.qdue.ui.shared;

import android.content.Context;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

// Placeholder classes for Phase 2 & 3
// DaysListEventsPreview.java (to be implemented in Phase 2)
class DaysListEventsPreview extends BaseEventsPreview {
    public DaysListEventsPreview(Context context) {
        super(context);
    }

    @Override
    protected void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, android.view.View anchorView) {
        // TODO: Implement in Phase 2
        Log.d(TAG, "DaysListEventsPreview.showEventsPreviewImpl - TODO: Phase 2");
    }

    @Override
    protected void hideEventsPreviewImpl() {
        // TODO: Implement in Phase 2
        Log.d(TAG, "DaysListEventsPreview.hideEventsPreviewImpl - TODO: Phase 2");
    }

    @Override
    protected String getViewType() {
        return "DaysList";
    }
}

// CalendarEventsPreview.java (to be implemented in Phase 3)
class CalendarEventsPreview extends BaseEventsPreview {
    public CalendarEventsPreview(Context context) {
        super(context);
    }

    @Override
    protected void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, android.view.View anchorView) {
        // TODO: Implement in Phase 3
        Log.d(TAG, "CalendarEventsPreview.showEventsPreviewImpl - TODO: Phase 3");
    }

    @Override
    protected void hideEventsPreviewImpl() {
        // TODO: Implement in Phase 3
        Log.d(TAG, "CalendarEventsPreview.hideEventsPreviewImpl - TODO: Phase 3");
    }

    @Override
    protected String getViewType() {
        return "CalendarView";
    }
}