package net.calvuz.qdue.events.models;

/**
 * Sources for calendar events
 */
public enum EventSource {
    LOCAL("Locale", "Events created locally in the app"),
    GOOGLE_CALENDAR("Google Calendar", "Events from Google Calendar API"),
    EXTERNAL_URL("URL Esterno", "Events from external URL packages"),
    COMPANY_FEED("Feed Aziendale", "Events from company RSS/API feeds");

    private final String displayName;
    private final String description;

    EventSource(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}