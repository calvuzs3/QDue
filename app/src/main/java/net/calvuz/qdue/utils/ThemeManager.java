package net.calvuz.qdue.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;

import net.calvuz.qdue.R;

/**
 * Gestore per i temi dell'applicazione.
 * Supporta tema chiaro/scuro e preparazione per tema dinamico (Material You).
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_DYNAMIC_COLORS = "dynamic_colors";

    // Modalità tema
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static ThemeManager instance;
    private Context context;
    private SharedPreferences preferences;

    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    /**
     * Applica il tema salvato nelle preferenze.
     */
    public void applyTheme() {
        int themeMode = getThemeMode();

        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        // Applica i colori dinamici se supportati e abilitati
        if (isDynamicColorsSupported() && isDynamicColorsEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable((Application) context.getApplicationContext());
        }
    }

    /**
     * Imposta la modalità tema.
     */
    public void setThemeMode(int themeMode) {
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme();
    }

    /**
     * Ottiene la modalità tema corrente.
     */
    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    /**
     * Verifica se il dispositivo è in modalità scura.
     */
    public boolean isDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) || (getThemeMode() == THEME_DARK);
    }

    /**
     * Verifica se i colori dinamici sono supportati (Android 12+).
     */
    public boolean isDynamicColorsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * Verifica se i colori dinamici sono abilitati.
     */
    public boolean isDynamicColorsEnabled() {
        return preferences.getBoolean(KEY_DYNAMIC_COLORS, true);
    }

    /**
     * Abilita/disabilita i colori dinamici.
     */
    public void setDynamicColorsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply();
    }

    /**
     * Ottiene il colore per un turno specifico.
     * In futuro può essere integrato con i colori dinamici.
     */
    public int getShiftColor(int shiftType) {
        switch (shiftType) {
            case 1: // Mattino
                return context.getColor(R.color.shift_morning);
            case 2: // Pomeriggio
                return context.getColor(R.color.shift_afternoon);
            case 3: // Notte
                return context.getColor(R.color.shift_night);
            default:
                return context.getColor(R.color.primary);
        }
    }

    /**
     * Restituisce il nome della modalità tema corrente.
     */
    public String getThemeModeName() {
        switch (getThemeMode()) {
            case THEME_LIGHT:
                return "Chiaro";
            case THEME_DARK:
                return "Scuro";
            case THEME_SYSTEM:
            default:
                return "Sistema";
        }
    }

    /**
     * Prepara l'applicazione per i colori dinamici futuri.
     * Questo metodo può essere espanso quando si implementerà Material You.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void prepareDynamicColors() {
        if (isDynamicColorsSupported() && isDynamicColorsEnabled()) {
            // Qui si possono aggiungere personalizzazioni specifiche per Material You
            Log.d(TAG, "Preparing dynamic colors for Material You");
        }
    }
}