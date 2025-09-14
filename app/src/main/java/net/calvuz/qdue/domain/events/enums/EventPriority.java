package net.calvuz.qdue.domain.events.enums;

import android.graphics.Color;

public enum EventPriority {
    LOW("Bassa", Color.GREEN),
    NORMAL("Normale", Color.GRAY),
    HIGH("Alta", Color.DKGRAY),
    URGENT("Urgente", Color.RED);

    private final String displayName;
    private final int color;

    EventPriority(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }

    public boolean isHigh() {
        return this.equals( EventPriority.HIGH ) || this.equals( EventPriority.URGENT );
    }
}