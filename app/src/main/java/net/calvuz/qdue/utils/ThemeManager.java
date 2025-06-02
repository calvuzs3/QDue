package net.calvuz.qdue.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.R;

/**
 * Application theme manager with support for light/dark themes and Material You dynamic colors.
 * <p>
 * This singleton class manages the application's theme configuration, including:
 * - Light, dark, and system-following theme modes
 * - Dynamic color support for Android 12+ (Material You)
 * - Theme persistence through SharedPreferences
 * - Shift-specific color management
 * <p>
 * The ThemeManager provides a centralized way to handle theme changes throughout
 * the application lifecycle and ensures consistent theming across all components.
 * <p>
 * Usage:
 * <pre>
 * ThemeManager themeManager = ThemeManager.getInstance(context);
 * themeManager.setThemeMode(ThemeManager.THEME_DARK);
 * themeManager.applyTheme();
 * </pre>
 *
 * @author Updated with English comments and JavaDoc
 * @version 2.0
 * @since 2025
 */
public class ThemeManager {

    // ==================== CONSTANTS ====================

    /**
     * Tag for logging purposes
     */
    private static final String TAG = "ThemeManager";

    /**
     * SharedPreferences file name for theme settings
     */
    private static final String PREFS_NAME = "theme_preferences";

    /**
     * SharedPreferences key for theme mode setting
     */
    private static final String KEY_THEME_MODE = "theme_mode";

    /**
     * SharedPreferences key for dynamic colors setting
     */
    private static final String KEY_DYNAMIC_COLORS = "dynamic_colors";

    // ==================== THEME MODE CONSTANTS ====================

    /**
     * Theme mode constant: Follow system theme setting
     */
    public static final int THEME_SYSTEM = 0;

    /**
     * Theme mode constant: Force light theme
     */
    public static final int THEME_LIGHT = 1;

    /**
     * Theme mode constant: Force dark theme
     */
    public static final int THEME_DARK = 2;

    // ==================== SINGLETON INSTANCE ====================

    /**
     * Singleton instance of ThemeManager
     */
    private static ThemeManager instance;

    // ==================== MEMBER VARIABLES ====================

    /**
     * Application context for accessing resources and system services
     */
    private Context context;

    /**
     * SharedPreferences instance for persisting theme settings
     */
    private SharedPreferences preferences;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor for singleton pattern.
     * Initializes the ThemeManager with application context and SharedPreferences.
     *
     * @param context Application context
     */
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ==================== SINGLETON ACCESS ====================

    /**
     * Gets the singleton instance of ThemeManager.
     * Creates a new instance if one doesn't exist.
     *
     * @param context Application context
     * @return ThemeManager singleton instance
     */
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    // ==================== THEME MODE MANAGEMENT ====================

    /**
     * Sets the theme mode and immediately applies it.
     *
     * @param themeMode One of THEME_SYSTEM, THEME_LIGHT, or THEME_DARK
     */
    public void setThemeMode(int themeMode) {
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme();
    }

    /**
     * Gets the currently saved theme mode.
     *
     * @return Current theme mode (THEME_SYSTEM, THEME_LIGHT, or THEME_DARK)
     */
    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    /**
     * Gets a human-readable name for the current theme mode.
     *
     * @return Theme mode name in Italian ("Sistema", "Chiaro", or "Scuro")
     * @deprecated Consider using string resources for localization
     */
    public String getThemeModeName() {
        switch (getThemeMode()) {
            case THEME_LIGHT:
                return "Chiaro";
            case THEME_DARK:
                return "Scuro";
            case THEME_SYSTEM:
            default:
                return "Sistema";
        }
    }

    // ==================== THEME STATE DETECTION ====================

    /**
     * Checks if the device is currently in dark mode.
     * <p>
     * This method considers both the system configuration and the forced theme setting.
     * It returns true if either the system is in dark mode or the user has forced dark theme.
     *
     * @return true if currently in dark mode, false otherwise
     */
    public boolean isDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) || (getThemeMode() == THEME_DARK);
    }

    // ==================== DYNAMIC COLORS MANAGEMENT ====================

    /**
     * Checks if dynamic colors (Material You) are supported on this device.
     * <p>
     * Dynamic colors require Android 12 (API level 31) or higher.
     *
     * @return true if dynamic colors are supported, false otherwise
     */
    public boolean isDynamicColorsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * Checks if dynamic colors are enabled by the user.
     *
     * @return true if dynamic colors are enabled, false otherwise
     */
    public boolean isDynamicColorsEnabled() {
        return preferences.getBoolean(KEY_DYNAMIC_COLORS, true);
    }

    /**
     * Enables or disables dynamic colors.
     *
     * @param enabled true to enable dynamic colors, false to disable
     */
    public void setDynamicColorsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply();
    }

    /**
     * Prepares the application for future dynamic color features.
     * <p>
     * This method can be expanded when implementing more Material You features.
     * Currently logs preparation status for debugging purposes.
     *
     * @apiNote Requires Android 12+ (API level 31)
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void prepareDynamicColors() {
        if (isDynamicColorsSupported() && isDynamicColorsEnabled()) {
            // Here you can add specific customizations for Material You
            Log.d(TAG, "Preparing dynamic colors for Material You");
        }
    }

    // ==================== SHIFT COLOR MANAGEMENT ====================

    /**
     * Gets the color for a specific shift type.
     * <p>
     * This method provides shift-specific colors that can be integrated
     * with dynamic color schemes in the future.
     *
     * @param shiftType Shift type identifier (1=Morning, 2=Afternoon, 3=Night)
     * @return Color resource for the specified shift type
     */
    public int getShiftColor(int shiftType) {
        switch (shiftType) {
            case 1: // Morning shift
                return context.getColor(R.color.shift_morning);
            case 2: // Afternoon shift
                return context.getColor(R.color.shift_afternoon);
            case 3: // Night shift
                return context.getColor(R.color.shift_night);
            default:
                return context.getColor(R.color.primary);
        }
    }

    // ==================== THEME APPLICATION METHODS ====================

    /**
     * Applies the currently saved theme from preferences.
     * <p>
     * This method reads the saved theme mode and applies it using AppCompatDelegate.
     * It also handles dynamic color application for supported devices.
     * Should be called during application startup and when theme settings change.
     */
    public void applyTheme() {
        int themeMode = getThemeMode();

        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        // Apply dynamic colors if supported and enabled
        if (isDynamicColorsSupported() && isDynamicColorsEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable((Application) context.getApplicationContext());
        }
    }
}