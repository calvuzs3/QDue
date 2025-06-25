package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.user.data.database.UserDatabase;
import net.calvuz.qdue.utils.Log;

import java.util.Locale;

public class QDue extends Application {

    // TAG
    static String TAG = "QDUE";

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static Locale locale;

    private static QuattroDue quattrodue;

    @Override
    public void onCreate() {
        super.onCreate();

        // MEMBERS
        context = this;
        locale = getSystemLocale();
        Log.d(TAG, "=== SystemLocale initialized");

        // 0. Enable Material You Dynamic Colors (Android 12+)
        enableDynamicColors();
        Log.d(TAG, "=== enableDynamicColors initialized");

        // 1. Initialize ShiftTypeFactory
//        Log.d(TAG, "=== ShiftTypeFactory initialized: " + ShiftTypeFactory.isInitialized());

        // 2. Initialize QuattroDue
        quattrodue = QuattroDue.getInstance(this);
        Log.d(TAG, "=== QuattroDue initialized");

        // 3. Initialize User database
        initializeUserDatabase();
        Log.d(TAG, "=== InitializeUserDatabase initialized");
    }

    /* ===== GETTERS ===== */

    public static Context getContext() {
        return context;
    }

    public static Locale getLocale() {
        return locale;
    }

    public static QuattroDue getQuattrodue() {
        return quattrodue;
    }

    /* ===== PRIVATES ===== */

    @SuppressLint("ObsoleteSdkInt")
    private static Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
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

    private void initializeUserDatabase() {
        try {
            // Initialize user database in background
            new Thread(() -> {
                UserDatabase.getInstance(this);
                Log.i(TAG, "User database initialized");
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing user database: " + e.getMessage());
        }
    }
}