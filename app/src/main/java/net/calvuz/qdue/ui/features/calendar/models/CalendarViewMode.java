package net.calvuz.qdue.ui.features.calendar.models;

/**
 * CalendarViewMode - Available view modes for calendar.
 */
public enum CalendarViewMode {
    MONTH("month"),
    WEEK("week"),
    DAY("day");

    private final String value;

    CalendarViewMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CalendarViewMode fromString(String value) {
        for (CalendarViewMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return MONTH; // Default
    }
}
