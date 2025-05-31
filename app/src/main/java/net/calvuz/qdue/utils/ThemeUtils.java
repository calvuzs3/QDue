package net.calvuz.qdue.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

import net.calvuz.qdue.R;

/**
 * Utility per ottenere colori e attributi del tema corrente.
 */
public class ThemeUtils {

    /**
     * Ottiene un colore dall'attributo del tema corrente.
     *
     * @param context Contesto
     * @param attrRes Risorsa dell'attributo (es. R.attr.colorPrimary)
     * @return Colore risolto
     */
    @ColorInt
    public static int getThemeColor(Context context, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    /**
     * COLORI MATERIAL DESIGN STANDARD
     * Questi potrebbero non rispettare i tuoi override se Material Design ha precedenza
     */
    /**
     * Ottiene il colore primario del tema.
     */
    @ColorInt
    public static int getMaterialPrimaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
    }

    /**
     * Ottiene il colore on-primary del tema.
     */
    @ColorInt
    public static int getMaterialOnPrimaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnPrimary);
    }

        /**
     * Ottiene il colore di errore del tema.
     */
    @ColorInt
    public static int getMaterialErrorColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorError);
    }

    /**
     * Ottiene il colore secondary del tema.
     */
    @ColorInt
    public static int getMaterialSecondaryColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorSecondary);
    }

    /**
     * Ottiene il colore primary container del tema.
     */
    @ColorInt
    public static int getMaterialPrimaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorPrimaryContainer);
    }

    /**
     * Ottiene il colore on-primary container del tema.
     */
    @ColorInt
    public static int getMaterialOnPrimaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer);
    }

    /**
     * Ottiene il colore tertiary container del tema (per oggi).
     */
    @ColorInt
    public static int getMaterialTertiaryContainerColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorTertiaryContainer);
    }

    /**
     * Ottiene il colore outline del tema.
     */
    @ColorInt
    public static int getMaterialOutlineColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOutline);
    }


    @ColorInt
    public static int getMaterialSurfaceColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorSurface);
    }

    @ColorInt
    public static int getMaterialOnSurfaceColor(Context context) {
        return getThemeColor(context, com.google.android.material.R.attr.colorOnSurface);
    }




    /**
     * COLORI SPECIFICI DELL'APP
     */

    @ColorInt
    public static int getTodayBackgroundColor(Context context) {
        return getThemeColor(context, R.attr.colorTodayBackground);
    }

    @ColorInt
    public static int getUserShiftBackgroundColor(Context context) {
        return context.getColor(R.color.primary_light);
    }

    @ColorInt
    public static int getUserShiftTextColor(Context context) {
        return context.getColor(R.color.on_primary);
    }
    @ColorInt
    public static int getSundayBackgroundColor(Context context) {
        return getThemeColor(context, R.attr.colorSundayBackground);
    }
    @ColorInt
    public static int getSundayTextColor(Context context) {
        return getThemeColor(context, R.attr.colorOnSundayBackground);
    }

    @ColorInt
    public static int getNormalBackgroundColor(Context context) {
        return getDynamicSurfaceColor(context);
    }

    @ColorInt
    public static int getOnNormalBackgroundColor(Context context) {
        return getDynamicOnNormalBackgroundColor(context);
    }

    @ColorInt
    public static int getUserShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorUserShift);
    }


    @ColorInt
    public static int getShiftMorningColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftMorning);
    }

    @ColorInt
    public static int getShiftAfternoonColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftAfternoon);
    }

    @ColorInt
    public static int getShiftNightColor(Context context) {
        return getThemeColor(context, R.attr.colorShiftNight);
    }

    @ColorInt
    public static int getFirstShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorFirstShift);
    }

    @ColorInt
    public static int getSecondShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorSecondShift);
    }

    @ColorInt
    public static int getThirdShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorThirdShift);
    }

    @ColorInt
    public static int getFourthShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorSecondShift);
    }

    @ColorInt
    public static int getFifthShiftColor(Context context) {
        return getThemeColor(context, R.attr.colorThirdShift);
    }

    /**
     * COLORI DINAMICI (per theme chiaro/scuro)
     * Questi cambiano automaticamente in base al tema
     */
    @ColorInt
    public static int getDynamicSurfaceColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.background_dark);
        } else {
            return context.getColor(R.color.background);
        }
    }

    @ColorInt
    public static int getDynamicOnSurfaceColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.on_background_dark);
        } else {
            return context.getColor(R.color.on_background);
        }
    }

    @ColorInt
    public static int getDynamicPrimaryColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.primary_dark_theme);
        } else {
            return context.getColor(R.color.primary);
        }
    }

    @ColorInt
    public static int getDynamicOnNormalBackgroundColor(Context context) {
        ThemeManager themeManager = ThemeManager.getInstance(context);
        if (themeManager.isDarkMode()) {
            return context.getColor(R.color.on_background_dark);
        } else {
            return context.getColor(R.color.on_background);
        }
    }
}