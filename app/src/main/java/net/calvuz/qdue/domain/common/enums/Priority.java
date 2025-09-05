package net.calvuz.qdue.domain.common.enums;

/**
 * Priority level.
 */
public enum Priority {
    LOW( 1, "low_priority" ),
    NORMAL( 3, "normal_priority" ),
    HIGH( 5, "high_priority" ),
    OVERRIDE( 10, "override_priority" );

    private final int level;
    private final String displayNameKey;

    Priority(int level, String displayNameKey) {
        this.level = level;
        this.displayNameKey = displayNameKey;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }
}