package net.calvuz.qdue.events.actions;

import androidx.annotation.StringRes;

import net.calvuz.qdue.R;

/**
 * Categories for grouping related EventActions
 */
public enum EventActionCategory {
    ABSENCE(R.string.event_action_category_absence),
    WORK_ADJUSTMENT(R.string.event_action_category_work_adjustment),
    PRODUCTION(R.string.event_action_category_production),
    DEVELOPMENT(R.string.event_action_category_development),
    GENERAL(R.string.event_action_category_general);

    @StringRes
    private final int displayName;

    EventActionCategory(@StringRes int displayName) {
        this.displayName = displayName;
    }

    @StringRes
    public int getDisplayName() {
        return displayName;
    }
}
