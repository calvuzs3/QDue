package net.calvuz.qdue.quattrodue;

import android.content.Context;
import android.content.SharedPreferences;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Library;

import java.time.LocalDate;

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


    /**
     * Get current scheme start date as LocalDate
     *
     * @param context Context
     * @return Scheme start date
     */
    public static LocalDate getSchemeStartDate(Context context) {
        int day = getSharedPreference(context, KEY_SCHEME_START_DAY, Costants.QD_SCHEME_START_DAY);
        int month = getSharedPreference(context, KEY_SCHEME_START_MONTH, Costants.QD_SCHEME_START_MONTH);
        int year = getSharedPreference(context, KEY_SCHEME_START_YEAR, Costants.QD_SCHEME_START_YEAR);

        return LocalDate.of(year, month, day);
    }

    /**
     * Set scheme start date from LocalDate
     *
     * @param context Context
     * @param date New scheme start date
     */
    public static void setSchemeStartDate(Context context, LocalDate date) {
        setSharedPreference(context, KEY_SCHEME_START_DAY, date.getDayOfMonth());
        setSharedPreference(context, KEY_SCHEME_START_MONTH, date.getMonthValue());
        setSharedPreference(context, KEY_SCHEME_START_YEAR, date.getYear());
    }

    /**
     * Check if scheme start date has changed since last check
     *
     * @param context Context
     * @param lastKnownDate Last known scheme date
     * @return true if date has changed
     */
    public static boolean hasSchemeStartDateChanged(Context context, LocalDate lastKnownDate) {
        LocalDate currentDate = getSchemeStartDate(context);
        return !currentDate.equals(lastKnownDate);
    }
}
