package net.calvuz.qdue.smartshifts.utils;

import android.graphics.Color;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for color operations
 */
@Singleton
public class ColorHelper {

    @Inject
    public ColorHelper() {}

    /**
     * Parse hex color string to integer
     */
    public int parseColor(String hexColor) {
        try {
            return Color.parseColor(hexColor);
        } catch (IllegalArgumentException e) {
            return Color.GRAY; // Default fallback
        }
    }

    /**
     * Convert color integer to hex string
     */
    public String colorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * Check if color is dark (for text contrast)
     */
    public boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    /**
     * Get contrasting text color (black or white)
     */
    public int getContrastingTextColor(int backgroundColor) {
        return isDarkColor(backgroundColor) ? Color.WHITE : Color.BLACK;
    }

    /**
     * Generate color with alpha
     */
    public int addAlpha(int color, float alpha) {
        int alphaInt = Math.round(alpha * 255);
        return Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color));
    }
}
