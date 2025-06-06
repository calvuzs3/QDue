package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory;
import net.calvuz.qdue.user.data.database.UserDatabase;
import net.calvuz.qdue.utils.Log;

import java.util.Locale;

public class QDue extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static Locale locale;

    private static QuattroDue quattrodue;

    @Override
    public void onCreate() {
        super.onCreate();

        // TAG
        String TAG ="QDue";

        // MEMBERS
        context = this;
        locale = getSystemLocale();

        // 0. Enable Material You Dynamic Colors (Android 12+)
        enableDynamicColors();

        // 1. Initialize ShiftTypeFactory
        Log.v(TAG, "ShiftTypeFactory initialized: " + ShiftTypeFactory.isInitialized());

        // 2. Initialize QuattroDue
        quattrodue = QuattroDue.getInstance(this);

        // 3. Initialize User database
        initializeUserDatabase();
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

    public static class Debug {
        public static boolean DEBUG_ACTIVITY = true;
        public static boolean DEBUG_FRAGMENT = true;
        public static boolean DEBUG_COLORS = false;
        public static boolean DEBUG_SHARED_VIEW_MODELS = false;

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
                Log.d("QDue", "Material You dynamic colors enabled");
            } else {
                Log.d("QDue", "Using fallback purple-blue theme (Android < 12)");
            }
        } catch (Exception e) {
            Log.e("QDue", "Error enabling dynamic colors: " + e.getMessage());
            // Gracefully fallback to static theme
        }
    }

    private void initializeUserDatabase() {
        try {
            // Initialize user database in background
            new Thread(() -> {
                UserDatabase.getInstance(this);
                Log.v("QDue", "User database initialized");
            }).start();
        } catch (Exception e) {
            Log.e("QDue", "Error initializing user database: " + e.getMessage());
        }
    }
}