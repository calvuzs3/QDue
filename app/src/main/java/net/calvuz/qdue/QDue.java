package net.calvuz.qdue;

import android.app.Application;
import android.content.Context;

import net.calvuz.qdue.utils.ThemeManager;

import java.util.Locale;

public class QDue extends Application {

    private static Context context;
    private static Locale locale;

    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale locale) {
        QDue.locale = locale;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        locale = Locale.ITALIAN;

        // Inizializza il tema dell'applicazione
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
    }

    public static Context getContext(){
        return context;
    }
}
