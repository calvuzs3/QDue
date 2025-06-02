package net.calvuz.qdue.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * General-purpose utility library containing static helper functions and members.
 *
 * This utility class provides common functionality that is application-independent
 * and can be reused across different parts of the codebase. The class focuses on
 * Android system interactions, package management, and preference handling.
 *
 * Features:
 * - Intent availability checking for safe intent launching
 * - Application version information retrieval with caching
 * - SharedPreferences wrapper for consistent preference access
 * - Package manager utilities for app metadata
 *
 * All methods are static and thread-safe. The class cannot be instantiated
 * as it serves purely as a utility container.
 *
 * Usage Examples:
 * <pre>
 * // Check if an email intent can be handled
 * Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
 * if (Library.isIntentAvailable(context, emailIntent)) {
 *     startActivity(emailIntent);
 * }
 *
 * // Get application version
 * String version = Library.getVersionCode(context);
 *
 * // Get default shared preferences
 * SharedPreferences prefs = Library.getSharedPreferences(context);
 * </pre>
 *
 * @author luke (original implementation)
 * @author Updated with English comments and JavaDoc
 * @version 2.0
 * @since 2019-01-04 (original)
 * @since 2025 (updated documentation)
 */
public final class Library {

    // ==================== CONSTANTS ====================

    /** Tag for logging purposes */
    private static final String TAG = "Library";

    // ==================== CACHED VALUES ====================

    /**
     * Cached version code to avoid repeated PackageManager queries.
     * Initialized lazily on first access and then reused for subsequent calls.
     */
    private static String sVersionCode = null;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class that should only contain static methods.
     */
    private Library() {
        throw new AssertionError("Library utility class should not be instantiated");
    }

    // ==================== INTENT UTILITIES ====================

    /**
     * Checks whether the specified intent action can be handled by any installed application.
     *
     * This method queries the PackageManager for installed packages that can respond
     * to the given intent. It's useful for preventing crashes when launching intents
     * that might not have handlers available on the device.
     *
     * Common use cases:
     * - Checking if email clients are available before sending emails
     * - Verifying map apps exist before showing directions
     * - Ensuring camera apps are present before capturing photos
     *
     * @param context The application context for accessing PackageManager
     * @param intent  The Intent to check for availability (action must be set)
     * @return true if at least one application can handle the intent, false otherwise
     *
     * @throws IllegalArgumentException if context is null
     * @see PackageManager#queryIntentActivities(Intent, int)
     *
     * @example
     * <pre>
     * Intent shareIntent = new Intent(Intent.ACTION_SEND);
     * shareIntent.setType("text/plain");
     * if (Library.isIntentAvailable(context, shareIntent)) {
     *     startActivity(Intent.createChooser(shareIntent, "Share via"));
     * } else {
     *     // Show alternative or error message
     * }
     * </pre>
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return !list.isEmpty();
        }

        return false;
    }

    // ==================== VERSION MANAGEMENT ====================

    /**
     * Retrieves the application version name with caching for performance.
     *
     * This method fetches the version name from the application's PackageInfo
     * and caches it for subsequent calls to avoid repeated PackageManager queries.
     * The version information is useful for debugging, crash reporting, and
     * feature compatibility checks.
     *
     * The version code is cached statically, so it will persist across multiple
     * calls but will be cleared if the application process is restarted.
     *
     * @param context Application context for accessing PackageManager
     * @return The application version name (e.g., "2.1.0"), or null if not found
     *
     * @throws IllegalArgumentException if context is null
     *
     * @example
     * <pre>
     * String version = Library.getVersionCode(context);
     * if (version != null) {
     *     Log.d("AppInfo", "Running version: " + version);
     *     // Include in crash reports or about dialog
     * }
     * </pre>
     *
     * @implNote The method uses lazy initialization and caches the result.
     *           The cache is cleared only when the application process restarts.
     */
    public static String getVersionCode(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        if (sVersionCode == null) {
            try {
                sVersionCode = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Unable to find package information
                Log.e(TAG, "Unable to find package [" + context.getPackageName() + "]");
                // Leave sVersionCode as null to indicate failure
            }
        }
        return sVersionCode;
    }

    // ==================== SHARED PREFERENCES UTILITIES ====================

    /**
     * Provides access to the default SharedPreferences instance.
     *
     * This is a convenience wrapper around PreferenceManager.getDefaultSharedPreferences()
     * that provides a consistent way to access the application's default preferences
     * throughout the codebase.
     *
     * The default SharedPreferences are typically used for:
     * - User settings and configuration
     * - Application state persistence
     * - Feature flags and toggles
     * - User preferences from PreferenceActivity/PreferenceFragment
     *
     * @param context Application context for preference access
     * @return SharedPreferences instance for the default preferences file
     *
     * @throws IllegalArgumentException if context is null
     *
     * @example
     * <pre>
     * SharedPreferences prefs = Library.getSharedPreferences(context);
     *
     * // Read a preference value
     * boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
     *
     * // Write a preference value
     * prefs.edit()
     *      .putString("user_name", userName)
     *      .apply();
     * </pre>
     *
     * @see PreferenceManager#getDefaultSharedPreferences(Context)
     * @apiNote This method returns the same SharedPreferences instance that is used
     *          by Android's preference framework (PreferenceActivity, etc.)
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    // ==================== ADDITIONAL UTILITY METHODS ====================

    /**
     * Clears the cached version code, forcing a fresh lookup on next access.
     *
     * This method is useful in testing scenarios or when the application
     * version might change during runtime (though this is rare in normal usage).
     *
     * @apiNote This method is primarily intended for testing purposes.
     *          In normal application usage, version caching is beneficial for performance.
     */
    public static void clearVersionCache() {
        sVersionCode = null;
    }

    /**
     * Gets the cached version code without triggering a fresh lookup.
     *
     * This method returns the currently cached version string, or null if
     * no version has been cached yet. Unlike getVersionCode(), this method
     * will not attempt to fetch the version if it hasn't been loaded.
     *
     * @return The cached version code, or null if not yet loaded
     *
     * @see #getVersionCode(Context)
     */
    public static String getCachedVersionCode() {
        return sVersionCode;
    }

    /**
     * Checks if the version code has been cached.
     *
     * @return true if version code is cached, false otherwise
     */
    public static boolean isVersionCached() {
        return sVersionCode != null;
    }
}