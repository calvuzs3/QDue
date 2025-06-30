package net.calvuz.qdue.events.models;

import android.graphics.Color;

/**
 * Enhanced event types including production stops
 * Names revised for consistency with industrial naming conventions
 */
public enum EventType {
    // General events
    GENERAL("Generale", Color.GRAY, "📅"),
    MEETING("Riunione", Color.BLUE, "🤝"),
    TRAINING("Formazione", Color.GREEN, "📚"),
    HOLIDAY("Festività", Color.MAGENTA, "🎉"),

    // Production events
    MAINTENANCE("Manutenzione", Color.LTGRAY, "🔧"),
    EMERGENCY("Emergenza", Color.RED, "🚨"),

    // Production stops (revised names for consistency)
    STOP_PLANNED("Fermata Programmata", Color.parseColor("#FF6B35"), "⏸️"),
    STOP_ORDERS("Fermata Carenza Ordini", Color.parseColor("#FF8C42"), "📦"),
    STOP_CASSA("Fermata Cassa Integrazione", Color.parseColor("#FFA62B"), "💼"),
    STOP_UNPLANNED( "Non programmata", Color.parseColor("#D32F2F"), "\uD83D\uDEA8"),
    STOP_SHORTAGE( "Carenza Ordini", Color.parseColor("#FF6B35"), "📦❌"),

    // Shift-related events
    SHIFT_CHANGE("Cambio Turno", Color.parseColor("#4ECDC4"), "🔄"),
    OVERTIME("Straordinario", Color.parseColor("#7B1FA2"), "⏰"),

    // Safety and compliance
    SAFETY_DRILL("Prova Sicurezza", Color.parseColor("#96CEB4"), "🛡️"),
    AUDIT("Audit", Color.parseColor("#FFEAA7"), "📋"),

    // Custom/imported events
    IMPORTED("Importato", Color.parseColor("#DDA0DD"), "📥"),
    OTHER("Altro", Color.parseColor("#DDA0DD"), "📥");

    private final String displayName;
    private final int color;
    private final String emoji;

    EventType(String displayName, int color, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
    }

    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }
    public String getEmoji() { return emoji; }

    /**
     * Get text color based on background color for readability
     */
    public int getTextColor() {
        // Calculate luminance and return black or white text
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Check if this is a production stop event
     */
    public boolean isProductionStop() {
        return this == STOP_PLANNED || this == STOP_ORDERS || this == STOP_CASSA || this == STOP_UNPLANNED || this == STOP_SHORTAGE;
    }

    public boolean isProductionEvent() {
        return this == MAINTENANCE || this == EMERGENCY;
    }

    public boolean isShiftRelatedEvent() {
        return this == SHIFT_CHANGE || this == OVERTIME;
    }
}