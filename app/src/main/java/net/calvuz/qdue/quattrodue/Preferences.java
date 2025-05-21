package net.calvuz.qdue.quattrodue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.calvuz.qdue.R;
import net.calvuz.qdue.utils.Library;

/**
 * Application worldwide preferences
 * <p>
 * Created by luke on 04/01/19.
 */

public class Preferences {

    // Shared Preferences Keys
    public static final String KEY_SCHEME_START_DAY = "preference_scheme_start_day";
    public static final String KEY_SCHEME_START_MONTH = "preference_scheme_start_month";
    public static final String KEY_SCHEME_START_YEAR = "preference_scheme_start_year";

    public static final String KEY_USER_HALFTEAM = "preference_user_halfteam";
    public static final String KEY_SHOW_CALENDARS = "preference_show_calendars";
    public static final String KEY_SHOW_STOPS = "preference_show_stops";

    public static final String KEY_VERSION_CODENAME = "preference_version_codename";

    // Shared Preferences Values
    public static final boolean VALUE_SHOW_CALENDARS = true;
    public static final boolean VALUE_SHOW_STOPS = true;

    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.root_preferences, false);
    }

    public static boolean getSharedPreference(Context context, String key, boolean value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        return sp.getBoolean(key, value);
    }

    public static int getSharedPreference(Context context, String key, int value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        return sp.getInt(key, value);
    }

    public static String getSharedPreference(Context context, String key, String value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        return sp.getString(key, value);
    }

    public static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        sp.edit().putBoolean(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        sp.edit().putInt(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences sp = Library.getSharedPreferences(context);
        sp.edit().putString(key, value).apply();
    }
}
