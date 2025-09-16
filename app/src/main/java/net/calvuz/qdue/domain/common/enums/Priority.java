package net.calvuz.qdue.domain.common.enums;

import net.calvuz.qdue.core.common.utils.ColorUtils;

/**
 * Priority level.
 */
public enum Priority
{
    LOW( 1, "low_priority", ColorUtils.getMaterialColors().get( "low_priority" ) ),
    NORMAL( 3, "normal_priority", ColorUtils.getMaterialColors().get( "medium_priority" ) ),
    HIGH( 5, "high_priority", ColorUtils.getMaterialColors().get( "high_priority" ) ),
    OVERRIDE( 10, "override_priority", ColorUtils.getMaterialColors().get( "override_priority" ) );

    private final int level;
    private final String displayNameKey;
    private final String colorHex;

    Priority(int level, String displayNameKey, String colorHex) {
        this.level = level;
        this.displayNameKey = displayNameKey;
        this.colorHex = colorHex;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getColorHex() {
        return colorHex;
    }
}