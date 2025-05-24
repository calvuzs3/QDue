package net.calvuz.qdue.quattrodue;

import android.content.Context;
import android.content.SharedPreferences;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.utils.Library;

/**
 * Application worldwide preferences
 * <p>
 * Created by luke on 04/01/19.
 */

public class Preferences {

    // Shared Preferences Keys
//    public static final String KEY_SCHEME_START_DAY = "preference_scheme_start_day";
    public static final String KEY_SCHEME_START_DAY = QDue.getContext().getString(R.string.qd_preference_scheme_start_day);
    public static final String KEY_SCHEME_START_MONTH = QDue.getContext().getString(R.string.qd_preference_scheme_start_month);
    public static final String KEY_SCHEME_START_YEAR = QDue.getContext().getString(R.string.qd_preference_scheme_start_year);

    public static final String KEY_USER_TEAM = QDue.getContext().getString(R.string.qd_preference_user_team);
    public static final String KEY_SHOW_CALENDARS = QDue.getContext().getString(R.string.qd_preference_show_stops);
    public static final String KEY_SHOW_STOPS = QDue.getContext().getString(R.string.qd_preference_show_stops);

    // Shared Preferences
    public static final boolean VALUE_SHOW_CALENDARS = true;
    public static final boolean VALUE_SHOW_STOPS = true;

    // Metodi
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
