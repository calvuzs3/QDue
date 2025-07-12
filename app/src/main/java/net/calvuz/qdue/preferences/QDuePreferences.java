package net.calvuz.qdue.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Modern QDue preferences management
 *
 * Integrates with existing WelcomeActivity preferences while providing modern API.
 * This class acts as a bridge between legacy preferences and new QDue architecture.
 *
 * Features:
 * - Compatible with existing WelcomeActivity and WelcomeInterface
 * - View mode preferences (Calendar vs DaysList)
 * - Seamless integration with QDueMainActivity navigation
 * - Maintains backward compatibility with existing preference keys
 *
 * @author QDue Development Team
 * @version 2.0 (Compatible with WelcomeActivity)
 * @since 2025
 */
public class QDuePreferences {

    // TAG for logging
    private static final String TAG = "QDuePreferences";

    // ==================== VIEW MODE PREFERENCES ====================

    /**
     * Get the user's preferred default view mode
     * Uses the same preference key as WelcomeActivity for compatibility
     * @param context Application context
     * @return View mode string (VIEW_MODE_CALENDAR or VIEW_MODE_DAYSLIST)
     */
    public static String getDefaultViewMode(Context context) {
        // Use the same preference system as WelcomeActivity
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR);
    }

    /**
     * Set the user's preferred default view mode
     * Compatible with WelcomeActivity preference system
     * @param context Application context
     * @param viewMode View mode string (VIEW_MODE_CALENDAR or VIEW_MODE_DAYSLIST)
     */
    public static void setDefaultViewMode(Context context, String viewMode) {
        // Validate input
        if (!QDue.Settings.VIEW_MODE_CALENDAR.equals(viewMode) &&
                !QDue.Settings.VIEW_MODE_DAYSLIST.equals(viewMode)) {
            Log.w(TAG, "Invalid view mode: " + viewMode + ". Using default.");
            viewMode = QDue.Settings.VIEW_MODE_CALENDAR;
        }

        // Use the same preference system as WelcomeActivity
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(QDue.Settings.QD_KEY_VIEW_MODE, viewMode).apply();

        Log.d(TAG, "Default view mode set to: " + viewMode);
    }

    // ==================== WELCOME FLOW INTEGRATION ====================

    /**
     * Check if the welcome flow has been completed
     * Uses the same preference key as WelcomeActivity
     * @param context Application context
     * @return true if welcome completed, false otherwise
     */
    public static boolean isWelcomeCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(QDue.Settings.QD_KEY_WELCOME_COMPLETED, false);
    }

    /**
     * Mark the welcome flow as completed
     * This method is primarily used by WelcomeActivity, but can be called elsewhere if needed
     * @param context Application context
     */
    public static void setWelcomeCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(QDue.Settings.QD_KEY_WELCOME_COMPLETED, true).apply();
        Log.d(TAG, "Welcome flow marked as completed");
    }

    // ==================== TEAM PREFERENCES (COMPATIBLE) ====================

    /**
     * Get the selected team preference
     * Compatible with WelcomeActivity team selection
     * @param context Application context
     * @return Selected team ID or default (1)
     */
    public static int getSelectedTeam(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
    }

    /**
     * Set the selected team preference
     * Compatible with WelcomeActivity team selection
     * @param context Application context
     * @param teamId Team identifier (integer)
     */
    public static void setSelectedTeam(Context context, int teamId) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(QDue.Settings.QD_KEY_SELECTED_TEAM, teamId).apply();
        Log.d(TAG, "Selected team set to: " + teamId);
    }

    // ==================== DYNAMIC COLORS PREFERENCES ====================

    /**
     * Check if dynamic colors are enabled
     * Compatible with WelcomeActivity dynamic colors setting
     * @param context Application context
     * @return true if dynamic colors enabled, false otherwise
     */
    public static boolean isDynamicColorsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, true);
    }

    /**
     * Set dynamic colors preference
     * Compatible with WelcomeActivity dynamic colors setting
     * @param context Application context
     * @param enabled true to enable dynamic colors, false to disable
     */
    public static void setDynamicColorsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, enabled).apply();
        Log.d(TAG, "Dynamic colors set to: " + enabled);
    }

    // ==================== FIRST TIME SETUP DETECTION ====================

    /**
     * Check if this is the first time the app is launched
     * This integrates with WelcomeActivity to determine if setup is needed
     * @param context Application context
     * @return true if this appears to be the first app launch
     */
    public static boolean isFirstTimeLaunch(Context context) {
        // Check if welcome is completed (primary indicator)
        boolean welcomeCompleted = isWelcomeCompleted(context);

        // Check if view mode has been explicitly set (secondary indicator)
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        boolean viewModeSet = prefs.contains(QDue.Settings.QD_KEY_VIEW_MODE);

        // Consider it first time if welcome not completed
        boolean isFirstTime = !welcomeCompleted;

        Log.d(TAG, "First time launch check: " + isFirstTime +
                " (welcome: " + welcomeCompleted + ", viewMode: " + viewModeSet + ")");

        return isFirstTime;
    }

    /**
     * Initialize default preferences for cases where WelcomeActivity was skipped
     * This provides fallback defaults if user somehow bypasses welcome flow
     * @param context Application context
     */
    public static void initializeDefaultsIfNeeded(Context context) {
        Log.d(TAG, "Checking if defaults need initialization");

        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);

        // Only set defaults if they don't exist (respect WelcomeActivity choices)
        SharedPreferences.Editor editor = prefs.edit();
        boolean needsCommit = false;

        if (!prefs.contains(QDue.Settings.QD_KEY_VIEW_MODE)) {
            editor.putString(QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR);
            needsCommit = true;
            Log.d(TAG, "Set default view mode: calendar");
        }

        if (!prefs.contains(QDue.Settings.QD_KEY_DYNAMIC_COLORS)) {
            editor.putBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, true);
            needsCommit = true;
            Log.d(TAG, "Set default dynamic colors: enabled");
        }

        if (!prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM)) {
            editor.putInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
            needsCommit = true;
            Log.d(TAG, "Set default team: 1");
        }

        if (needsCommit) {
            editor.apply();
            Log.d(TAG, "Default preferences initialized");
        } else {
            Log.d(TAG, "No defaults needed - preferences already set");
        }
    }

    // ==================== NAVIGATION HELPER METHODS ====================

    /**
     * Get the navigation destination ID for the default view mode
     * This is used by QDueMainActivity to set the correct start destination
     * @param context Application context
     * @return Navigation destination ID (R.id.nav_calendar or R.id.nav_dayslist)
     */
    public static int getDefaultNavigationDestination(Context context) {
        String viewMode = getDefaultViewMode(context);

        // Note: These IDs must match your actual navigation graph
        if (QDue.Settings.VIEW_MODE_DAYSLIST.equals(viewMode)) {
            return net.calvuz.qdue.R.id.nav_dayslist;
        } else {
            return net.calvuz.qdue.R.id.nav_calendar;
        }
    }

    /**
     * Check if the user should see WelcomeActivity
     * This method helps QDueMainActivity decide whether to redirect to welcome
     * @param context Application context
     * @return true if user should be redirected to welcome
     */
    public static boolean shouldShowWelcome(Context context) {
        return !isWelcomeCompleted(context);
    }

    // ==================== WELCOME INTERFACE BRIDGE ====================

    /**
     * Bridge method to work with WelcomeInterface
     * Updates view mode preference in a way compatible with WelcomeActivity
     * @param context Application context
     * @param viewMode View mode from WelcomeInterface
     */
    public static void updateFromWelcomeInterface(Context context, String viewMode) {
        // Normalize the view mode value
        String normalizedViewMode;
        if ("dayslist".equals(viewMode) || QDue.Settings.VIEW_MODE_DAYSLIST.equals(viewMode)) {
            normalizedViewMode = QDue.Settings.VIEW_MODE_DAYSLIST;
        } else {
            normalizedViewMode = QDue.Settings.VIEW_MODE_CALENDAR;
        }

        setDefaultViewMode(context, normalizedViewMode);
        Log.d(TAG, "Updated from WelcomeInterface: " + normalizedViewMode);
    }

    // ==================== DEBUGGING AND UTILITIES ====================

    /**
     * Log all current QDue preferences for debugging
     * @param context Application context
     */
    public static void logAllPreferences(Context context) {
        if (!QDue.Debug.DEBUG_ACTIVITY) return;

        Log.d(TAG, "=== QDue Preferences Debug Info ===");
        Log.d(TAG, "View Mode: " + getDefaultViewMode(context));
        Log.d(TAG, "Welcome Completed: " + isWelcomeCompleted(context));
        Log.d(TAG, "Selected Team: " + getSelectedTeam(context));
        Log.d(TAG, "Dynamic Colors: " + isDynamicColorsEnabled(context));
        Log.d(TAG, "First Time Launch: " + isFirstTimeLaunch(context));
        Log.d(TAG, "Should Show Welcome: " + shouldShowWelcome(context));
        Log.d(TAG, "=== End QDue Preferences ===");
    }

    /**
     * Get SharedPreferences instance (for backward compatibility)
     * @param context Application context
     * @return SharedPreferences instance using QDue preference name
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Reset all QDue preferences to defaults
     * WARNING: This will erase all user preferences including welcome completion!
     * @param context Application context
     */
    public static void resetAllPreferences(Context context) {
        Log.w(TAG, "Resetting all QDue preferences to defaults");

        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Remove all QDue-specific preferences
        editor.remove(QDue.Settings.QD_KEY_VIEW_MODE);
        editor.remove(QDue.Settings.QD_KEY_WELCOME_COMPLETED);
        editor.remove(QDue.Settings.QD_KEY_SELECTED_TEAM);
        editor.remove(QDue.Settings.QD_KEY_DYNAMIC_COLORS);

        editor.apply();

        Log.d(TAG, "All QDue preferences reset - user will see welcome again");
    }
}