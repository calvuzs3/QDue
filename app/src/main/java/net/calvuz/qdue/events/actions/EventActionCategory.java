package net.calvuz.qdue.events.actions;

/**
 * Categories for grouping related EventActions
 */
public enum EventActionCategory {
    ABSENCE("Assenze"),
    WORK_ADJUSTMENT("Aggiustamenti Turno"),
    PRODUCTION("Produzione"),
    DEVELOPMENT("Sviluppo"),
    GENERAL("Generale");

    private final String displayName;

    EventActionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
