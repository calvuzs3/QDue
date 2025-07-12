package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.ui.common.di.BackHandlerFactory;
import net.calvuz.qdue.ui.common.di.BackHandlingModule;
import net.calvuz.qdue.ui.common.interfaces.BackHandlingService;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.utils.Log;

import java.util.Locale;

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

        // Initialize QuattroDue
        quattrodue = QuattroDue.getInstance(this);
        Log.d(TAG, "=== QuattroDue initialized");

        // Initialize ShiftTypeFactory
        //Log.d(TAG, "=== ShiftTypeFactory initialized: " + ShiftTypeFactory.isInitialized());

        // Initialize unified database
        QDueDatabase.getInstance(this);
        Log.d(TAG, "=== QDueDatabase initialized");

        // 🆕 Initialize back handling services early
        BackHandlingModule.initialize(this);
        Log.d(TAG, "=== BackHandling services initialized");
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

    /* ===== PRIVATES ===== */

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
        public static final String QD_KEY_WELCOME_COMPLETED = "qdue_welcome_completed";
        public static final String QD_KEY_SELECTED_TEAM = "qdue_selected_team";
        public static final String QD_KEY_VIEW_MODE = "qdue_view_mode";
        public static final String QD_KEY_DYNAMIC_COLORS = "qdue_dynamic_colors_enabled";


        // View mode constants
        public static final String VIEW_MODE_CALENDAR = "calendar";
        public static final String VIEW_MODE_DAYSLIST = "dayslist";

        // Animation constants
        public static final long QD_WELCOME_LOGO_ANIMATION_DURATION = 3000; // 3 seconds
        public static final long QD_WELCOME_DISPLAY_DURATION = 2000; // 2 seconds after animation

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
    public class VirtualScrollingDebugSettings {

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
                android.util.Log.d(tag, "PERF: " + metric + " = " + value + "ms");
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
                Log.i(TAG, "Material You dynamic colors enabled");
            } else {
                Log.i(TAG, "Using fallback purple-blue theme (Android < 12)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling dynamic colors: " + e.getMessage());
            // Gracefully fallback to static theme
        }
    }

    /**
     * 🆕 Get BackHandlingService for the entire app
     *
     * Usage anywhere in the app:
     * BackHandlingService service = QDue.getBackHandlingService();
     */
    public static BackHandlingService getBackHandlingService() {
        return BackHandlingModule.getBackHandlingService(INSTANCE);
    }

    /**
     * 🆕 Get BackHandlerFactory for the entire app
     *
     * Usage anywhere in the app:
     * BackHandlerFactory factory = QDue.getBackHandlerFactory();
     */
    public static BackHandlerFactory getBackHandlerFactory() {
        return BackHandlingModule.getBackHandlerFactory(INSTANCE);
    }

}