package net.calvuz.qdue.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * A class which collects static function and members as utilities
 * It is meant to be unrelated from the app
 * <p>
 * Created by luke on 04/01/19.
 */

public final class Library {

    private static final String TAG = "Library";
    private static String sVersionCode = null;

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param intent  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return !list.isEmpty();
        }

        return false;
    }

    /**
     * Intended to save the version code name for to supply the upcoming changes in
     * future version of the app.
     *
     * @param context context
     * @return the version code name
     */
    public static String getVersionCode(Context context) {
        if (sVersionCode == null) {
            try {
                sVersionCode = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Can't find version.
                Log.e(TAG, "Unable to find package [" + context.getPackageName() + "]");
            }
        }
        return sVersionCode;
    }

    /**
     * Wrapper function to return the shared preferences
     *
     * @param context context
     * @return sharedpreferences
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
