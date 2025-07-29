package net.calvuz.qdue.events.models;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

/**
 * Sources for calendar events
 */
public enum EventSource {
    LOCAL(R.string.event_source_local, R.string.event_source_desc_local),
    GOOGLE_CALENDAR(R.string.event_source_google_calendar, R.string.event_source_desc_google_calendar),
    EXTERNAL_URL(R.string.event_source_external_url, R.string.event_source_desc_external_url),
    COMPANY_FEED(R.string.event_source_company_feed, R.string.event_source_desc_company_feed);

    @StringRes
    private final int displayName;
    @StringRes
    private final int description;

    EventSource(@StringRes int displayName, @StringRes int description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return getString(QDue.getContext(), displayName); }
    public String getDescription() { return getString(QDue.getContext(), description); }
}