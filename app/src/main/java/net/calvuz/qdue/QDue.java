package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlerFactory;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlingModule;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Main QDue Application class with Hilt support
 * Now supports both existing QDue functionality and new SmartShifts
 */
//@HiltAndroidApp  // Hilt Support for Dependency Injection
public class QDue extends Application {

    // TAG
    static String TAG = "QDUE";

    // CONSTANTS
    public static final String QDUE_RRULE_SCHEME_START_DATE =
            LocalDate.of( 2018, 11, 7 ).toString(); // First day morning team A
    public static final String QDUE_RRULE_DAILY_START_DATE =
            LocalDate.of( 2025, 8, 11 ).toString(); // Monday
    public static final String QDUE_RRULE_SCHEME_START_ASSIGNMENT_DATE =
            LocalDate.of( 2025, 8, 8 ).toString();
    public static final int SETTINGS_REQUEST_CODE = 1001;
    public static final int WELCOME_REQUEST_CODE = 1002;

    @SuppressLint ("StaticFieldLeak")
    private static Context INSTANCE;

    private static Locale locale;

    private static QuattroDue quattrodue;

    @Override
    public void onCreate() {
        super.onCreate();

        if (INSTANCE == null) {
            INSTANCE = this;
        }

        // Initialize default preferences including User ID
        initializeDefaultPreferences();

        // Locale
        locale = new Locale( Locale.US.getLanguage() ); //getSystemLocale();
        Log.d( TAG, "=== SystemLocale initialized" );

        // Enable Material You Dynamic Colors (Android 12+)
//        enableDynamicColors();
//        Log.d( TAG, "=== DynamicColors initialized" );

        // Initialize unified database
        QDueDatabase.getInstance( this );
        Log.d( TAG, "=== QDueDatabase initialized" );

        // Initialize QuattroDue
//        quattrodue = QuattroDue.getInstance( this );
//        Log.d( TAG, "=== QuattroDue initialized" );

        // 🆕 Initialize back handling services early
//        BackHandlingModule.initialize( this );
//        Log.d( TAG, "=== BackHandling services initialized" );

        // ✅ AGGIUNTO: Initialize SmartShifts database
        //initializeSmartShifts();
    }

    /**
     * Initialize default preferences for single-user app.
     * Ensures User ID and other critical preferences are available from app start.
     */
    private void initializeDefaultPreferences() {
        try {
            // Initialize all default preferences including User ID = 1L
            QDuePreferences.initializeDefaultsIfNeeded( this );

            // Migrate legacy team preferences if needed
            QDuePreferences.migrateTeamPreferencesIfNeeded( this );

            // Log current state for debugging
            QDuePreferences.logAllPreferences( this );

            // Colors
            if (QDuePreferences.isDynamicColorsEnabled( this )) {
                enableDynamicColors();
            }
            ;

            Log.d( TAG, "=== Preferences initialized" );
        } catch (Exception e) {
            Log.e( "QDueApplication", "Error initializing preferences", e );

            // Fallback: force reset to defaults
            try {
                QDuePreferences.resetUserIdToDefault( this );
                QDuePreferences.setSelectedTeamName( this, "A" );
                Log.w( TAG, "!!! Applied emergency preference defaults" );
            } catch (Exception fallbackError) {
                Log.e( TAG, "Critical: Cannot initialize preferences", fallbackError );
            }
        }
    }
    /* ===== GETTERS ===== */

    // Application Context
    public static Context getContext() {
        return INSTANCE;
    }

    // Locale
    public static Locale getLocale() {
        return locale;
    }

    // QuattroDue Engine
    public static QuattroDue getQuattrodue() {
        return quattrodue;
    }

    /// ////////////////////////// SMARTSHIFTS STARTS HERE //////////////////////////////////

    /**
     * Initialize SmartShifts components
     * Runs in background to avoid blocking UI
     */
//    private void initializeSmartShifts() {
//        SmartShiftsDatabase.databaseWriteExecutor.execute( () -> {
//            try {
//                SmartShiftsDatabase database = SmartShiftsDatabase.getDatabase( this );
//                DatabaseInitializer.initializeWithLocalizedStrings( database, this );
//                android.util.Log.d( TAG, "=== SmartShifts database initialized successfully" );
//            } catch (Exception e) {
//                Log.e( TAG, "Error initializing SmartShifts database", e );
//            }
//        } );
//    }
    @Override
    public void onTerminate() {
        super.onTerminate();

        // === EXISTING QDUE CLEANUP ===
        // (Mantieni tutto il cleanup QDue esistente qui)

        // === SMARTSHIFTS CLEANUP ===
//        try {
//            SmartShiftsDatabase.closeDatabase();
//            android.util.Log.d( TAG, "=== SmartShifts resources cleaned up" );
//        } catch (Exception e) {
//            android.util.Log.e( TAG, "Error cleaning up SmartShifts resources", e );
//        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

//        // Cleanup SmartShifts cache in low memory situations
//        try {
//            SmartShiftsDatabase database = SmartShiftsDatabase.getDatabase( this );
//            // Could add cache clearing logic here if needed
//        } catch (Exception e) {
//            Log.w( TAG, "Could not clear SmartShifts cache", e );
//        }
    }

    /// ////////////////////////// SMARTSHIFTS ENDS HERE //////////////////////////////////

    /**
     * Settings - app wide
     */
    public static class Settings {

        // Animation constants
        public static final long QD_WELCOME_LOGO_ANIMATION_DURATION = 1500; // 3 seconds
        public static final long QD_WELCOME_DISPLAY_DURATION = 2500; // 2 seconds after animation

        // Constants for configuration
        public static final String QD_PREF_NAME = "qdue_prefs";

        public static final String QD_KEY_QDUEUSER_NICKNAME = getContext().getString( R.string.qd_preference_qdueuser_nickname );
        public static final String QD_KEY_QDUEUSER_EMAIL = getContext().getString( R.string.qd_preference_qdueuser_email );
        public static final String QD_KEY_QDUEUSER_ONBOARDING_COMPLETED = getContext().getString( R.string.qd_preference_qdueuser_onboarding_completed );

        public static final String QD_KEY_USER_ID = getContext().getString( R.string.qd_preference_user_id );
        public static final String QD_KEY_WELCOME_COMPLETED = getContext().getString( R.string.qd_preference_welcome_completed );
        public static final String QD_KEY_SELECTED_TEAM = getContext().getString( R.string.qd_preference_selected_team );
        public static final String QD_KEY_SELECTED_TEAM_NAME = getContext().getString( R.string.qd_preference_selected_team_name );
        public static final String QD_KEY_VIEW_MODE = getContext().getString( R.string.qd_preference_view_mode );
        public static final String QD_KEY_DYNAMIC_COLORS = getContext().getString( R.string.qd_preference_dynamic_colors_enabled );

        // View mode enum
        public enum ViewMode {
            CALENDAR( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[0] ), //"calendar"),
            DAYS( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[1] ), //"dayslist");
            SWIPE_CALENDAR( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[2] ); //"swipe_calendar"),

            private final String name;

            ViewMode(String name) {
                this.name = name;
            }

            public String getName() {
                return this.name;
            }
        }

        // View mode constants
        public static final String VIEW_MODE_CALENDAR = ViewMode.CALENDAR.getName();
        public static final String VIEW_MODE_DAYSLIST = ViewMode.DAYS.getName();
        public static final String VIEW_MODE_SWIPE_CALENDAR = ViewMode.SWIPE_CALENDAR.getName();
    }

    public static final class Debug {
        public static final boolean DEBUG_ACTIVITY = false;
        public static final boolean DEBUG_FRAGMENT = false;
        public static final boolean DEBUG_ADAPTER = false;
        public static final boolean DEBUG_COLORS = false;
        public static final boolean DEBUG_SHARED_VIEW_MODELS = false;
        // Base
        public static final boolean DEBUG_BASEFRAGMENT = false;
        public static final boolean DEBUG_BASEADAPTER = false;
    }

    public static final class VirtualScrollingSettings {

        // HelperMethod
        public static final boolean isVirtualScrollingEnabled = false;

        // EnhancedBaseFragmentBridge
        public static final boolean ENABLE_VIRTUAL_SCROLLING = false;

        // AdapterBridge // EnhancedBaseFragmentBridge // CalendarViewFragmentEnhanced
        public static final boolean USE_VIRTUAL_SCROLLING = false;

        // Exception storage settings
        public static final boolean USE_EXCEPTION_STORAGE = false;
        public static final boolean ENABLE_PATTERN_OVERRIDE = false;
        public static final boolean ENABLE_EXCEPTION_MERGE = false;
    }

    /**
     * Enable Material You dynamic colors with fallback support
     */
    private void enableDynamicColors() {
        try {
            // Check if Material You is available (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Enable dynamic colors for all activities
                DynamicColors.applyToActivitiesIfAvailable( this );
                Log.i( TAG, "=== Material You dynamic colors enabled" );
            } else {
                Log.i( TAG, "=== Using fallback theme (Android < 12)" );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error enabling dynamic colors", e );
            // Gracefully fallback to static theme
        }
    }

    /**
     * 🆕 Get BackHandlingService for the entire app
     * <p>
     * Usage anywhere in the app:
     * BackHandlingService service = QDue.getBackHandlingService();
     */
    public static BackHandlingService getBackHandlingService() {
        return BackHandlingModule.getBackHandlingService( INSTANCE );
    }

    /**
     * 🆕 Get BackHandlerFactory for the entire app
     * <p>
     * Usage anywhere in the app:
     * BackHandlerFactory factory = QDue.getBackHandlerFactory();
     */
    public static BackHandlerFactory getBackHandlerFactory() {
        return BackHandlingModule.getBackHandlerFactory( INSTANCE );
    }
}