package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.view.View;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlerFactory;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlingModule;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.util.Locale;

import dagger.hilt.android.HiltAndroidApp;

import net.calvuz.qdue.smartshifts.data.database.SmartShiftsDatabase;
import net.calvuz.qdue.smartshifts.data.database.DatabaseInitializer;

/**
 * Main QDue Application class with Hilt support
 * Now supports both existing QDue functionality and new SmartShifts
 */
@HiltAndroidApp  // Hilt Support for Dependency Injection
public class QDue extends Application {

    // TAG
    static String TAG = "QDUE";

    // CONSTANTS
    public static final int SETTINGS_REQUEST_CODE = 1001;
    public static final int WELCOME_REQUEST_CODE = 1002;


    @SuppressLint("StaticFieldLeak")
    private static Context INSTANCE;

    private static Locale locale;

    private static QuattroDue quattrodue;

    @Override
    public void onCreate() {
        super.onCreate();

        // INSTANCE reference
        INSTANCE = this;

        // Locale
        locale = getSystemLocale();
        Log.d(TAG, "=== SystemLocale initialized");

        // Enable Material You Dynamic Colors (Android 12+)
        enableDynamicColors();
        Log.d(TAG, "=== DynamicColors initialized");

        // Initialize unified database
        QDueDatabase.getInstance(this);
        Log.d(TAG, "=== QDueDatabase initialized");

        // Initialize QuattroDue
        quattrodue = QuattroDue.getInstance(this);
        Log.d(TAG, "=== QuattroDue initialized");

        // 🆕 Initialize back handling services early
        BackHandlingModule.initialize(this);
        Log.d(TAG, "=== BackHandling services initialized");

        // ✅ AGGIUNTO: Initialize SmartShifts database
        initializeSmartShifts();
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
    private void initializeSmartShifts() {
        SmartShiftsDatabase.databaseWriteExecutor.execute(() -> {
            try {
                SmartShiftsDatabase database = SmartShiftsDatabase.getDatabase(this);
                DatabaseInitializer.initializeWithLocalizedStrings(database, this);
                android.util.Log.d(TAG, "=== SmartShifts database initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SmartShifts database", e);
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // === EXISTING QDUE CLEANUP ===
        // (Mantieni tutto il cleanup QDue esistente qui)

        // === SMARTSHIFTS CLEANUP ===
        try {
            SmartShiftsDatabase.closeDatabase();
            android.util.Log.d(TAG, "=== SmartShifts resources cleaned up");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error cleaning up SmartShifts resources", e);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        // Cleanup SmartShifts cache in low memory situations
        try {
            SmartShiftsDatabase database = SmartShiftsDatabase.getDatabase(this);
            // Could add cache clearing logic here if needed
        } catch (Exception e) {
            Log.w(TAG, "Could not clear SmartShifts cache", e);
        }
    }

    /// ////////////////////////// SMARTSHIFTS ENDS HERE //////////////////////////////////


    @SuppressLint("ObsoleteSdkInt")
    private static Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return INSTANCE.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return INSTANCE.getResources().getConfiguration().locale;
        }
    }

    /**
     * Settings - app wide
     */
    public static class Settings {

        // Constants for configuration
        public static final String QD_PREF_NAME = "qdue_prefs";
        public static final String QD_KEY_WELCOME_COMPLETED = getContext().getString(R.string.qd_preference_welcome_completed);
        public static final String QD_KEY_SELECTED_TEAM = getContext().getString( R.string.qd_preference_selected_team );
        public static final String QD_KEY_VIEW_MODE = getContext().getString(R.string.qd_preference_view_mode);
        public static final String QD_KEY_DYNAMIC_COLORS = getContext().getString(R.string.qd_preference_dynamic_colors_enabled);


        // View mode constants
        public enum ViewMode {
            CALENDAR( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[0] ), //"calendar"),
            DAYS( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[1] ), //"dayslist");
            SWIPE_CALENDAR( getContext().getResources().getStringArray( R.array.qdue_view_mode_values )[2] ); //"swipe_calendar"),

            private final String name;
            ViewMode(String name) { this.name = name; }

            public String getName() { return this.name; }
        }
        public static final String VIEW_MODE_CALENDAR = ViewMode.CALENDAR.getName();
        public static final String VIEW_MODE_DAYSLIST = ViewMode.DAYS.getName();
        public static final String VIEW_MODE_SWIPE_CALENDAR = ViewMode.SWIPE_CALENDAR.getName();



        // Animation constants
        public static final long QD_WELCOME_LOGO_ANIMATION_DURATION = 1500; // 3 seconds
        public static final long QD_WELCOME_DISPLAY_DURATION = 2500; // 2 seconds after animation

    }

    public static final class Debug {
        public static final boolean DEBUG_ACTIVITY = false;
        public static final boolean DEBUG_FRAGMENT = false;
        public static final boolean DEBUG_ADAPTER = false;
        public static final boolean DEBUG_COLORS = false;
        public static final boolean DEBUG_SHARED_VIEW_MODELS = false;
        // Base
        public static final boolean DEBUG_BASEFRAGMENT = true;
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
     * Debug settings for virtual scrolling
     */
    public static class VirtualScrollingDebugSettings {

        // Enable/disable debug logging
        public static final boolean DEBUG_VIRTUAL_SCROLLING = true;

        // Enable/disable performance logging
        public static final boolean DEBUG_PERFORMANCE = true;

        // Enable/disable memory monitoring
        public static final boolean DEBUG_MEMORY = true;

        // Enable/disable cache statistics
        public static final boolean DEBUG_CACHE_STATS = true;

        // Simulate slow network for testing
        public static final boolean SIMULATE_SLOW_LOADING = false;
        public static final int SIMULATED_DELAY_MS = 500;

        /**
         * Log debug message if enabled
         */
        public void logDebug(String tag, String message) {
            if (DEBUG_VIRTUAL_SCROLLING) {
                android.util.Log.d(tag, message);
            }
        }

        /**
         * Log performance metric if enabled
         */
        public void logPerformance(String tag, String metric, long value) {
            if (DEBUG_PERFORMANCE) {
                Log.d(tag, MessageFormat.format("PERF: {0} = {1}ms", metric, value));
            }
        }
    }

    /**
     * Enable Material You dynamic colors with fallback support
     */
    private void enableDynamicColors() {
        try {
            // Check if Material You is available (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Enable dynamic colors for all activities
                DynamicColors.applyToActivitiesIfAvailable(this);
                Log.i(TAG, "=== Material You dynamic colors enabled");
            } else {
                Log.i(TAG, "=== Using fallback purple-blue theme (Android < 12)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling dynamic colors: " + e.getMessage());
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
        return BackHandlingModule.getBackHandlingService(INSTANCE);
    }

    /**
     * 🆕 Get BackHandlerFactory for the entire app
     * <p>
     * Usage anywhere in the app:
     * BackHandlerFactory factory = QDue.getBackHandlerFactory();
     */
    public static BackHandlerFactory getBackHandlerFactory() {
        return BackHandlingModule.getBackHandlerFactory(INSTANCE);
    }

}