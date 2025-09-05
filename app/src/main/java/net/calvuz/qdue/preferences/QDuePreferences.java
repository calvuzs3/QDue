package net.calvuz.qdue.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.UUID;

/**
 * Modern QDue preferences management
 * <p>
 * Integrates with existing WelcomeActivity preferences while providing modern API.
 * This class acts as a bridge between legacy preferences and new QDue architecture.
 * <p>
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

    // ==================== USER ID MANAGEMENT (SINGLE USER APP) ====================

    // Default User ID for single-user application.
    public static final String DEFAULT_USER_ID = UUID.randomUUID().toString();

    /**
     * Get the current user ID.
     * For single-user app, this always returns a valid ID.
     *
     * @param context Application context
     * @return User ID
     */
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(QDue.Settings.QD_KEY_USER_ID, DEFAULT_USER_ID);

        // Safety check: never return 0 or negative IDs
        if (userId.trim().isEmpty()) {
            Log.w(TAG, "Invalid User ID found: " + userId + ". Resetting to default: " + DEFAULT_USER_ID);
            setUserId(context, DEFAULT_USER_ID);
            return DEFAULT_USER_ID;
        }

        return userId;
    }

    /**
     * Set the user ID.
     * For single-user app, this allows customization.
     *
     * @param context Application context
     * @param userId User ID to set
     */
    public static void setUserId(Context context, String userId) {
        // Validate user ID
        if (userId.trim().isEmpty()) {
            Log.w(TAG, "Invalid user ID provided: " + userId + ". Using default: " + DEFAULT_USER_ID);
            userId = DEFAULT_USER_ID;
        }

        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(QDue.Settings.QD_KEY_USER_ID, userId).apply();

        Log.d(TAG, "User ID set to: " + userId);
    }

    /**
     * Check if user ID has been explicitly set.
     * Used during first-time initialization to determine if default should be applied.
     *
     * @param context Application context
     * @return true if user ID preference exists
     */
    public static boolean hasUserIdSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(QDue.Settings.QD_KEY_USER_ID);
    }

    /**
     * Reset user ID to default (1L).
     * Used for troubleshooting or resetting single-user app state.
     *
     * @param context Application context
     */
    public static void resetUserIdToDefault(Context context) {
        setUserId(context, DEFAULT_USER_ID);
        Log.d(TAG, "User ID reset to default: " + DEFAULT_USER_ID);
    }

    // ==================== VIEW MODE PREFERENCES ====================

    /**
     * Get the user's preferred default view mode
     * Uses the same preference key as WelcomeActivity for compatibility
     *
     * @param context Application context
     * @return View mode string (VIEW_MODE_CALENDAR or VIEW_MODE_DAYSLIST)
     */
    public static String getDefaultViewMode(Context context) {
        // Use the same preference system as WelcomeActivity
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        return prefs.getString( QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR );
    }

    /**
     * Set the user's preferred default view mode
     * Compatible with WelcomeActivity preference system
     *
     * @param context  Application context
     * @param viewMode View mode string (VIEW_MODE_CALENDAR or VIEW_MODE_DAYSLIST)
     */
    public static void setDefaultViewMode(Context context, String viewMode) {
        // Validate input
        if (!QDue.Settings.VIEW_MODE_CALENDAR.equals( viewMode ) &&
                !QDue.Settings.VIEW_MODE_DAYSLIST.equals( viewMode ) &&
                !QDue.Settings.VIEW_MODE_SWIPE_CALENDAR.equals( viewMode )) {
            Log.w( TAG, "Invalid view mode: " + viewMode + ". Using default." );
            viewMode = QDue.Settings.VIEW_MODE_CALENDAR;
        }

        // Use the same preference system as WelcomeActivity
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        prefs.edit().putString( QDue.Settings.QD_KEY_VIEW_MODE, viewMode ).apply();

        Log.d( TAG, "Default view mode set to: " + viewMode );
    }

    // ==================== WELCOME FLOW INTEGRATION ====================

    /**
     * Check if the welcome flow has been completed
     * Uses the same preference key as WelcomeActivity
     *
     * @param context Application context
     * @return true if welcome completed, false otherwise
     */
    public static boolean isWelcomeCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        return prefs.getBoolean( QDue.Settings.QD_KEY_WELCOME_COMPLETED, false );
    }

    /**
     * Mark the welcome flow as completed
     * This method is primarily used by WelcomeActivity, but can be called elsewhere if needed
     *
     * @param context Application context
     */
    public static void setWelcomeCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        prefs.edit().putBoolean( QDue.Settings.QD_KEY_WELCOME_COMPLETED, true ).apply();
        Log.d( TAG, "Welcome flow marked as completed" );
    }

    // ==================== LEGACY TEAM PREFERENCES (DEPRECATED) ====================

    /**
     * Get the selected team preference (legacy method).
     *
     * @deprecated Use {@link #getSelectedTeamName(Context)} for modern Team model support.
     * @param context Application context
     * @return Selected team ID or default (1)
     */
    @Deprecated
    public static int getSelectedTeam(Context context) {
           SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);

        // Try to get from modern preference first
        if (prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME)) {
            String teamName = prefs.getString(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME, "A");
            return convertTeamNameToLegacyId(teamName);
        }

        // Fallback to legacy preference
        return prefs.getInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
    }

    /**
     * Set the selected team preference (legacy method).
     *
     * @deprecated Use {@link #setSelectedTeamName(Context, String)} for modern Team model support.
     * @param context Application context
     * @param teamId  Team identifier (integer)
     */
    @Deprecated
    public static void setSelectedTeam(Context context, int teamId) {
        // Convert to team name and use modern method
        String teamName = convertLegacyTeamIdToName(teamId);
        setSelectedTeamName(context, teamName);

        Log.d(TAG, "Legacy setSelectedTeam called with ID " + teamId + ", converted to name: " + teamName);
    }

    // ==================== TEAM PREFERENCES ====================

    /**
     * Get the selected team name (modern approach).
     * Uses team name (A, B, C, etc.) instead of integer ID.
     *
     * @param context Application context
     * @return Selected team name or default ("A")
     */
    public static String getSelectedTeamName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);

        // Check if we have the modern string-based preference
        if (prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME)) {
            return prefs.getString(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME, "A");
        }

        // Fallback: convert legacy int preference to team name
        if (prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM)) {
            int legacyTeamId = prefs.getInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
            String teamName = convertLegacyTeamIdToName(legacyTeamId);

            // Migrate to new format
            setSelectedTeamName(context, teamName);
            Log.d(TAG, "Migrated legacy team ID " + legacyTeamId + " to team name: " + teamName);

            return teamName;
        }

        // Ultimate fallback
        return "A";
    }

    /**
     * Set the selected team name (modern approach).
     * Stores team name (A, B, C, etc.) directly.
     *
     * @param context Application context
     * @param teamName Team name (A, B, C, D, E, F, G, H, I)
     */
    public static void setSelectedTeamName(Context context, String teamName) {
        // Validate team name
        if (!isValidQuattroDueTeamName(teamName)) {
            Log.w(TAG, "Invalid team name: " + teamName + ". Using default 'A'.");
            teamName = "A";
        }

        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save modern preference
        editor.putString(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME, teamName);

        // Also update legacy preference for backward compatibility
        int legacyTeamId = convertTeamNameToLegacyId(teamName);
        editor.putInt(QDue.Settings.QD_KEY_SELECTED_TEAM, legacyTeamId);

        editor.apply();
        Log.d(TAG, "Selected team set to: " + teamName + " (legacy ID: " + legacyTeamId + ")");
    }

    /**
     * Set the selected team using Team object (modern approach).
     *
     * @param context Application context
     * @param team Team object from domain model
     */
    public static void setSelectedTeam(Context context, net.calvuz.qdue.domain.calendar.models.Team team) {
        if (team == null) {
            Log.w(TAG, "Null team provided. Using default 'A'.");
            setSelectedTeamName(context, "A");
            return;
        }

        setSelectedTeamName(context, team.getName());
        Log.d(TAG, "Selected team set from Team object: " + team.getName());
    }

    /**
     * Get the selected team as a Team object (modern approach).
     * This method requires database access, so it's async-friendly.
     *
     * @param context Application context
     * @return Team name that can be used to lookup Team object from repository
     */
    public static String getSelectedTeamNameForRepository(Context context) {
        return getSelectedTeamName(context);
    }

    // ==================== TEAM CONVERSION UTILITIES ====================

    /**
     * Convert legacy team ID to team name.
     * Maps 1=A, 2=B, 3=C, etc.
     *
     * @param teamId Legacy team ID (1-9)
     * @return Team name (A-I)
     */
    private static String convertLegacyTeamIdToName(int teamId) {
        // QuattroDue teams: 1=A, 2=B, 3=C, 4=D, 5=E, 6=F, 7=G, 8=H, 9=I
        switch (teamId) {
            case 1: return "A";
            case 2: return "B";
            case 3: return "C";
            case 4: return "D";
            case 5: return "E";
            case 6: return "F";
            case 7: return "G";
            case 8: return "H";
            case 9: return "I";
            default:
                Log.w(TAG, "Unknown legacy team ID: " + teamId + ". Using 'A'.");
                return "A";
        }
    }

    /**
     * Convert team name to legacy team ID.
     * Maps A=1, B=2, C=3, etc.
     *
     * @param teamName Team name (A-I)
     * @return Legacy team ID (1-9)
     */
    private static int convertTeamNameToLegacyId(String teamName) {
        if (teamName == null) return 1;

        switch (teamName.toUpperCase()) {
            case "A": return 1;
            case "B": return 2;
            case "C": return 3;
            case "D": return 4;
            case "E": return 5;
            case "F": return 6;
            case "G": return 7;
            case "H": return 8;
            case "I": return 9;
            default:
                Log.w(TAG, "Unknown team name: " + teamName + ". Using ID 1.");
                return 1;
        }
    }


    /**
     * Validate if team name is a valid QuattroDue team.
     *
     * @param teamName Team name to validate
     * @return true if valid QuattroDue team name
     */
    private static boolean isValidQuattroDueTeamName(String teamName) {
        if (teamName == null) return false;

        String[] validTeams = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        String upperTeamName = teamName.toUpperCase();

        for (String validTeam : validTeams) {
            if (validTeam.equals(upperTeamName)) {
                return true;
            }
        }

        return false;
    }

    // ==================== MIGRATION HELPERS ====================

    /**
     * Migrate legacy team preferences to modern format.
     * Call this once during app startup to ensure smooth transition.
     *
     * @param context Application context
     */
    public static void migrateTeamPreferencesIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE);

        // If modern preference already exists, no migration needed
        if (prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME)) {
            Log.d(TAG, "Team preferences already migrated - no action needed");
            return;
        }

        // If legacy preference exists, migrate it
        if (prefs.contains(QDue.Settings.QD_KEY_SELECTED_TEAM)) {
            int legacyTeamId = prefs.getInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
            String teamName = convertLegacyTeamIdToName(legacyTeamId);

            // Save modern preference
            prefs.edit()
                    .putString(QDue.Settings.QD_KEY_SELECTED_TEAM_NAME, teamName)
                    .apply();

            Log.d(TAG, "Migrated team preference: ID " + legacyTeamId + " -> Name " + teamName);
        } else {
            // No legacy preference, set default
            setSelectedTeamName(context, "A");
            Log.d(TAG, "No legacy team preference found - set default 'A'");
        }
    }

    // ==================== DYNAMIC COLORS PREFERENCES ====================

    /**
     * Check if dynamic colors are enabled
     * Compatible with WelcomeActivity dynamic colors setting
     *
     * @param context Application context
     * @return true if dynamic colors enabled, false otherwise
     */
    public static boolean isDynamicColorsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        return prefs.getBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, true );
    }

    /**
     * Set dynamic colors preference
     * Compatible with WelcomeActivity dynamic colors setting
     *
     * @param context Application context
     * @param enabled true to enable dynamic colors, false to disable
     */
    public static void setDynamicColorsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        prefs.edit().putBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, enabled ).apply();
        Log.d( TAG, "Dynamic colors set to: " + enabled );
    }

    // ==================== FIRST TIME SETUP DETECTION ====================

    /**
     * Check if this is the first time the app is launched
     * This integrates with WelcomeActivity to determine if setup is needed
     *
     * @param context Application context
     * @return true if this appears to be the first app launch
     */
    public static boolean isFirstTimeLaunch(Context context) {
        // Check if welcome is completed (primary indicator)
        boolean welcomeCompleted = isWelcomeCompleted( context );

        // Check if view mode has been explicitly set (secondary indicator)
        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        boolean viewModeSet = prefs.contains( QDue.Settings.QD_KEY_VIEW_MODE );

        // Consider it first time if welcome not completed
        boolean isFirstTime = !welcomeCompleted;

        Log.d( TAG, "First time launch check: " + isFirstTime +
                " (welcome: " + welcomeCompleted + ", viewMode: " + viewModeSet + ")" );

        return isFirstTime;
    }

    /**
     * Initialize default preferences for cases where WelcomeActivity was skipped
     * This provides fallback defaults if user somehow bypasses welcome flow
     *
     * @param context Application context
     */
    public static void initializeDefaultsIfNeeded(Context context) {
        Log.d( TAG, "Checking if defaults need initialization" );

        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );

        // Only set defaults if they don't exist (respect WelcomeActivity choices)
        SharedPreferences.Editor editor = prefs.edit();
        boolean needsCommit = false;

        if (!prefs.contains( QDue.Settings.QD_KEY_VIEW_MODE )) {
            editor.putString( QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR );
            needsCommit = true;
            Log.d( TAG, "Set default view mode: calendar" );
        }

        if (!prefs.contains( QDue.Settings.QD_KEY_DYNAMIC_COLORS )) {
            editor.putBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, true );
            needsCommit = true;
            Log.d( TAG, "Set default dynamic colors: enabled" );
        }

        if (!prefs.contains( QDue.Settings.QD_KEY_SELECTED_TEAM )) {
            editor.putInt( QDue.Settings.QD_KEY_SELECTED_TEAM, 1 );
            needsCommit = true;
            Log.d( TAG, "Set default team (deprecated): 1" );
        }

        if (!prefs.contains( QDue.Settings.QD_KEY_SELECTED_TEAM_NAME )) {
            editor.putString( QDue.Settings.QD_KEY_SELECTED_TEAM_NAME, "A" );
            needsCommit = true;
            Log.d( TAG, "Set default team name: A" );
        }

        if (needsCommit) {
            editor.apply();
            Log.d( TAG, "Default preferences initialized" );
        } else {
            Log.d( TAG, "No defaults needed - preferences already set" );
        }
    }

    // ==================== NAVIGATION HELPER METHODS ====================

    /**
     * Get the navigation destination ID for the default view mode
     * This is used by QDueMainActivity to set the correct start destination
     *
     * @param context Application context
     * @return Navigation destination ID (R.id.nav_calendar or R.id.nav_dayslist)
     */
    public static int getDefaultNavigationDestination(Context context) {
        String viewMode = getDefaultViewMode( context );

        // Note: These IDs must match your actual navigation graph
        if (QDue.Settings.VIEW_MODE_DAYSLIST.equals( viewMode )) {
            return net.calvuz.qdue.R.id.nav_dayslist;
        } else if (QDue.Settings.VIEW_MODE_CALENDAR.equals( (viewMode) )) {
            return net.calvuz.qdue.R.id.nav_calendar;
        } else if (QDue.Settings.VIEW_MODE_SWIPE_CALENDAR.equals( (viewMode) )) {
            return net.calvuz.qdue.R.id.nav_swipe_calendar;
        } else {
            return R.id.nav_swipe_calendar;
        }
    }

    /**
     * Check if the user should see WelcomeActivity
     * This method helps QDueMainActivity decide whether to redirect to welcome
     *
     * @param context Application context
     * @return true if user should be redirected to welcome
     */
    public static boolean shouldShowWelcome(Context context) {
        return !isWelcomeCompleted( context );
    }

    // ==================== WELCOME INTERFACE BRIDGE ====================

    /**
     * Bridge method to work with WelcomeInterface
     * Updates view mode preference in a way compatible with WelcomeActivity
     *
     * @param context  Application context
     * @param viewMode View mode from WelcomeInterface
     */
    public static void updateFromWelcomeInterface(Context context, String viewMode) {
        // Normalize the view mode value
        String normalizedViewMode;
        if (QDue.Settings.VIEW_MODE_DAYSLIST.equals( viewMode )) {
            normalizedViewMode = QDue.Settings.VIEW_MODE_DAYSLIST;
        } else if (QDue.Settings.VIEW_MODE_SWIPE_CALENDAR.equals( viewMode )) {
            normalizedViewMode = QDue.Settings.VIEW_MODE_SWIPE_CALENDAR;
        } else {
            normalizedViewMode = QDue.Settings.VIEW_MODE_CALENDAR;
        }

        setDefaultViewMode( context, normalizedViewMode );
        Log.d( TAG, "Updated from WelcomeInterface: " + normalizedViewMode );
    }

    // ==================== DEBUGGING AND UTILITIES ====================

    /**
     * Log all current QDue preferences for debugging
     *
     * @param context Application context
     */
    public static void logAllPreferences(Context context) {
        if (!QDue.Debug.DEBUG_ACTIVITY) return;

        Log.d( TAG, "=== QDue Preferences Debug Info ===" );
        Log.d( TAG, "User ID: " + getUserId( context ) );
        Log.d( TAG, "View Mode: " + getDefaultViewMode( context ) );
        Log.d( TAG, "Welcome Completed: " + isWelcomeCompleted( context ) );
        Log.d( TAG, "Selected Team (deprecated): " + getSelectedTeam( context ) );
        Log.d( TAG, "Selected Team Name: " + getSelectedTeamName( context ) );
        Log.d( TAG, "Dynamic Colors: " + isDynamicColorsEnabled( context ) );
        Log.d( TAG, "First Time Launch: " + isFirstTimeLaunch( context ) );
        Log.d( TAG, "Should Show Welcome: " + shouldShowWelcome( context ) );
        Log.d( TAG, "=== End QDue Preferences ===" );
    }

    /**
     * Get SharedPreferences instance (for backward compatibility)
     *
     * @param context Application context
     * @return SharedPreferences instance using QDue preference name
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
    }

    /**
     * Reset all QDue preferences to defaults
     * WARNING: This will erase all user preferences including welcome completion!
     *
     * @param context Application context
     */
    public static void resetAllPreferences(Context context) {
        Log.w( TAG, "Resetting all QDue preferences to defaults" );

        SharedPreferences prefs = context.getSharedPreferences( QDue.Settings.QD_PREF_NAME, Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = prefs.edit();

        // Remove all QDue-specific preferences
        editor.remove( QDue.Settings.QD_KEY_USER_ID );
        editor.remove( QDue.Settings.QD_KEY_VIEW_MODE );
        editor.remove( QDue.Settings.QD_KEY_WELCOME_COMPLETED );
        editor.remove( QDue.Settings.QD_KEY_SELECTED_TEAM );
        editor.remove( QDue.Settings.QD_KEY_SELECTED_TEAM_NAME );
        editor.remove( QDue.Settings.QD_KEY_DYNAMIC_COLORS );

        editor.apply();

        Log.d( TAG, "All QDue preferences reset - user will see welcome again" );
    }
}