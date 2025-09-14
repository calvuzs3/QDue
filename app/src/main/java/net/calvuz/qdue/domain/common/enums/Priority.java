package net.calvuz.qdue.domain.common.enums;

import net.calvuz.qdue.core.common.utils.ColorUtils;

/**
 * Priority level.
 */
public enum Priority
{
    LOW( 1, "low_priority", ColorUtils.getMaterialColors().get( "light_green_300" ) ),
    NORMAL( 3, "normal_priority", ColorUtils.getMaterialColors().get( "green_300" ) ),
    HIGH( 5, "high_priority", ColorUtils.getMaterialColors().get( "amber_300" ) ),
    URGENT( 7, "urgent_priority", ColorUtils.getMaterialColors().get( "orange_300" ) ),
    OVERRIDE( 10, "override_priority", ColorUtils.getMaterialColors().get( "red_300" ) );

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

    public String getDisplayName() {
        return displayNameKey;
    }

    public String getColor() {
        return colorHex;
    }

    public boolean isOverride() {
        return this.equals( Priority.OVERRIDE );
    }

    public boolean isHigh() {
        return this.equals( Priority.HIGH ) || this.equals( Priority.OVERRIDE );
    }

    public boolean isNormal() {
        return this.equals( Priority.NORMAL );
    }

    public boolean isLow() {
        return this.equals( Priority.LOW );
    }
}