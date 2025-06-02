package net.calvuz.qdue;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.utils.ThemeManager;

import java.util.Locale;


public class QDue extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static Locale locale;

    private static QuattroDue quattrodue;



    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        locale = getSystemLocale();

        // 1. Init ShiftTypeFactory

        // 2. Init QuattroDue
        quattrodue = QuattroDue.getInstance(this);

        // Inizializza il tema dell'applicazione
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else{
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

}


