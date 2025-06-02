package net.calvuz.qdue.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

import net.calvuz.qdue.R;

/**
 * Utility class for retrieving colors and attributes from the current theme.
 *
 * This class provides a comprehensive set of methods to access theme colors in a consistent way.
 * It supports both Material Design standard colors and application-specific custom colors.
 * The utility handles theme attribute resolution and provides convenience methods for
 * commonly used colors throughout the application.
 *
 * Features:
 * - Material Design 3 color system support
 * - Custom application color retrieval
 * - Dynamic color switching based on theme mode
 * - Shift-specific color management
 * - Theme attribute resolution utilities
 *
 * Color Categories:
 * - Material Design Standard Colors: Primary, Secondary, Surface, etc.
 * - Application-Specific Colors: Today highlighting, user shifts, etc.
 * - Dynamic Colors: Theme-aware colors that change with light/dark mode
 * - Shift Colors: Work shift specific colors for UI highlighting
 *
 * Usage:
 * <pre>
 * int primaryColor = ThemeUtils.getMaterialPrimaryColor(context);
 * int todayColor = ThemeUtils.getTodayBackgroundColor(context);
 * int dynamicSurface = ThemeUtils.getDynamicSurfaceColor(context);
 * </pre>
 *
 * @author Updated with English comments and JavaDoc
 * @version 2.0
 * @since 2025
 */
public class ThemeUtils {

    // ==================== PRIVATE CONSTRUCTOR ====================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private ThemeUtils() {
        throw new AssertionError("ThemeUtils should not be instantiated");
    }

// ==================== CORE THEME ATTRIBUTE RESOLUTION ====================

    /**
     * Retrieves a color from the current theme attributes.
     *
     * This is the foundation method that resolves theme attributes to actual color values.
     * All other color methods in this class ultimately use this method for theme resolution.
     *
     * @param context Application context for theme access
     * @param attrRes Theme attribute resource (e.g., R.attr.colorPrimary)
     * @return Resolved color value
     */
    @ColorInt
    public static int getThemeColor(Context context, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    // ==================== MATERIAL DESIGN STANDARD COLORS ====================
    // Note: These may not respect custom overrides if Material Design takes precedence

    /**
     * Gets the primary color from the Material Design theme.
     *
     * @param context Application context
     * @return Primary color from Material theme
     */
    @ColorInt
    public static int getMaterialPrimaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
    }

    /**
     * Gets the on-primary color from the Material Design theme.
     * This is the color used for content (text, icons) displayed on primary color backgrounds.
     *
     * @param context Application context
     * @return On-primary color from Material theme
     */
    @ColorInt
    public static int getMaterialOnPrimaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnPrimary);
    }

    /**
     * Gets the error color from the Material Design theme.
     * Used for error states, warnings, and destructive actions.
     *
     * @param context Application context
     * @return Error color from Material theme
     */
    @ColorInt
    public static int getMaterialErrorColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorError);
    }

    /**
     * Gets the secondary color from the Material Design theme.
     * Used for accent elements and secondary actions.
     *
     * @param context Application context
     * @return Secondary color from Material theme
     */
    @ColorInt
    public static int getMaterialSecondaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorSecondary);
    }

    /**
     * Gets the primary container color from the Material Design theme.
     * Used for less prominent elements that need primary color association.
     *
     * @param context Application context
     * @return Primary container color from Material theme
     */
    @ColorInt
    public static int getMaterialPrimaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorPrimaryContainer);
    }

    /**
     * Gets the on-primary container color from the Material Design theme.
     * This is the color used for content displayed on primary container backgrounds.
     *
     * @param context Application context
     * @return On-primary container color from Material theme
     */
    @ColorInt
    public static int getMaterialOnPrimaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer);
    }

    /**
     * Gets the tertiary container color from the Material Design theme.
     * Often used for highlighting current/today elements.
     *
     * @param context Application context
     * @return Tertiary container color from Material theme
     */
    @ColorInt
    public static int getMaterialTertiaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorTertiaryContainer);
    }

    /**
     * Gets the outline color from the Material Design theme.
     * Used for borders, dividers, and outline elements.
     *
     * @param context Application context
     * @return Outline color from Material theme
     */
    @ColorInt
    public static int getMaterialOutlineColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOutline);
    }

    /**
     * Gets the surface color from the Material Design theme.
     * Used for card backgrounds and elevated surfaces.
     *
     * @param context Application context
     * @return Surface color from Material theme
     */
    @ColorInt
    public static int getMaterialSurfaceColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorSurface);
    }

    /**
     * Gets the on-surface color from the Material Design theme.
     * This is the color used for content displayed on surface backgrounds.
     *
     * @param context Application context
     * @return On-surface color from Material theme
     */
    @ColorInt
    public static int getMaterialOnSurfaceColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnSurface);
    }

    // ==================== APPLICATION-SPECIFIC COLORS ====================

    /**
     * Gets the background color for highlighting today's date.
     *
     * @param context Application context
     * @return Today background color
     */
    @ColorInt
    public static int getTodayBackgroundColor(Context context) {
        return getThemeColor(context, R.attr.colorTodayBackground);
    }

    /**
     * Gets the background color for highlighting user's shifts.
     *
     * @param context Application context
     * @return User shift background color
     */
    @ColorInt
    public static int getUserShiftBackgroundColor(Context context) {
        return context.getColor(R.color.primary_light);
    }

    /**
     * Gets the text color for user's shifts.
     *
     * @param context Application context
     * @return User shift text color
     */
    @ColorInt
    public static int getUserShiftTextColor(Context context) {
        return context.getColor(R.color.on_primary);
    }

    /**
     * Gets the background color for Sunday highlighting.
     *
     * @param context Application context
     * @return Sunday background color
     */
    @ColorInt
    public static int getSundayBackgroundColor(Context context) {
        return getThemeColor(context, R.attr.colorSundayBackground);
    }

    /**
     * Gets the text color for Sunday highlighting.
     *
     * @param context Application context
     * @return Sunday text color
     */
    @ColorInt
    public static int getSundayTextColor(Context context) {
        return getThemeColor(context, R.attr.colorOnSundayBackground);
    }

    /**
     * Gets the normal background color (delegates to dynamic surface).
     *
     * @param context Application context
     * @return Normal background color
     */
    @ColorInt
    public static int getNormalBackgroundColor(Context context) {
        return getDynamicSurfaceColor(context);
    }

    /**
     * Gets the text color for normal backgrounds (delegates to dynamic text color).
     *
     * @param context Application context
     * @return Normal background text color
     */
    @ColorInt
    public static int getOnNormalBackgroundColor(Context context) {
        return getDynamicOnNormalBackgroundColor(context);
    }

    /**
     * Gets the user shift color from theme attributes.
     *
     * @param context Application context
     * @return User shift color
     */
    @ColorInt
    public static int getUserShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorUserShift);
    }

    // ==================== SHIFT-SPECIFIC COLORS ====================

    /**
     * Gets the color for morning shifts.
     *
     * @param context Application context
     * @return Morning shift color
     */
    @ColorInt
    public static int getShiftMorningColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftMorning);
    }

    /**
     * Gets the color for afternoon shifts.
     *
     * @param context Application context
     * @return Afternoon shift color
     */
    @ColorInt
    public static int getShiftAfternoonColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftAfternoon);
    }

    /**
     * Gets the color for night shifts.
     *
     * @param context Application context
     * @return Night shift color
     */
    @ColorInt
    public static int getShiftNightColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftNight);
    }

    /**
     * Gets the color for the first shift of the day.
     *
     * @param context Application context
     * @return First shift color
     */
    @ColorInt
    public static int getFirstShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorFirstShift);
    }

    /**
     * Gets the color for the second shift of the day.
     *
     * @param context Application context
     * @return Second shift color
     */
    @ColorInt
    public static int getSecondShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorSecondShift);
    }

    /**
     * Gets the color for the third shift of the day.
     *
     * @param context Application context
     * @return Third shift color
     */
    @ColorInt
    public static int getThirdShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorThirdShift);
    }

    /**
     * Gets the color for the fourth shift of the day.
     * Currently defaults to second shift color.
     *
     * @param context Application context
     * @return Fourth shift color
     */
    @ColorInt
    public static int getFourthShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorSecondShift);
    }

    /**
     * Gets the color for the fifth shift of the day.
     * Currently defaults to third shift color.
     *
     * @param context Application context
     * @return Fifth shift color
     */
    @ColorInt
    public static int getFifthShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorThirdShift);
    }

    // ==================== DYNAMIC COLORS (Light/Dark Theme Aware) ====================
    // These colors automatically change based on the current theme mode

    /**
     * Gets the surface color that adapts to the current theme mode.
     * Returns dark surface color in dark mode, light surface color in light mode.
     *
     * @param context Application context
     * @return Theme-adaptive surface color
     */
    @ColorInt
    public static int getDynamicSurfaceColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.background_dark);
        } else {
            return context.getColor(R.color.background);
        }
    }

    /**
     * Gets the on-surface text color that adapts to the current theme mode.
     * Returns light text color in dark mode, dark text color in light mode.
     *
     * @param context Application context
     * @return Theme-adaptive on-surface color
     */
    @ColorInt
    public static int getDynamicOnSurfaceColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.on_background_dark);
        } else {
            return context.getColor(R.color.on_background);
        }
    }

    /**
     * Gets the primary color that adapts to the current theme mode.
     * Returns dark theme primary in dark mode, regular primary in light mode.
     *
     * @param context Application context
     * @return Theme-adaptive primary color
     */
    @ColorInt
    public static int getDynamicPrimaryColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.primary_dark_theme);
        } else {
            return context.getColor(R.color.primary);
        }
    }

    /**
     * Gets the normal background text color that adapts to the current theme mode.
     * This is an alias for getDynamicOnSurfaceColor() for semantic clarity.
     *
     * @param context Application context
     * @return Theme-adaptive normal background text color
     */
    @ColorInt
    public static int getDynamicOnNormalBackgroundColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.on_background_dark);
        } else {
            return context.getColor(R.color.on_background);
        }
    }
}