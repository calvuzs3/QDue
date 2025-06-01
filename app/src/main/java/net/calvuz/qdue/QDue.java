package net.calvuz.qdue;

import android.app.Application;
import android.content.Context;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.utils.ThemeManager;

import java.util.Locale;

public class QDue extends Application {

    private static Context context;
    private static Locale locale;
    private static QuattroDue mQD;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        locale = Locale.ITALIAN;
        mQD = QuattroDue.getInstance(this);

        // Inizializza il tema dell'applicazione
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
    }

    public static Context getContext() {
        return context;
    }

    /* ===== LOCALE ===== */

    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale locale) {
        QDue.locale = locale;
    }

    public static class Debug {
        public static boolean DEBUG_ACTIVITY = true;
        public static boolean DEBUG_COLORS = false;

    }

}


