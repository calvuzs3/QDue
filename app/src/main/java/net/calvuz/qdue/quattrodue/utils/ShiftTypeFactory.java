package net.calvuz.qdue.quattrodue.utils;

import net.calvuz.qdue.quattrodue.models.ShiftType;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Factory per creare e gestire i tipi di turno.
 * Supporta un numero variabile di turni al giorno.
 */
public class ShiftTypeFactory {

    // TAG
    private static final String TAG = ShiftTypeFactory.class.getSimpleName();

    // Chiavi per le preferenze
    private static final String PREFS_NAME = "shift_types_prefs";
    private static final String KEY_SHIFT_COUNT = "shift_count";
    private static final String KEY_SHIFT_NAME_PREFIX = "shift_name_";
    private static final String KEY_SHIFT_DESC_PREFIX = "shift_desc_";
    private static final String KEY_SHIFT_START_HOUR_PREFIX = "shift_start_hour_";
    private static final String KEY_SHIFT_START_MIN_PREFIX = "shift_start_min_";
    private static final String KEY_SHIFT_DURATION_HOURS_PREFIX = "shift_dur_hours_";
    private static final String KEY_SHIFT_DURATION_MINS_PREFIX = "shift_dur_mins_";
    private static final String KEY_SHIFT_COLOR_PREFIX = "shift_color_";

    // Valori predefiniti
    private static final int DEFAULT_SHIFT_COUNT = 3;

    // Cache di tipi di turno
    private static Map<String, ShiftType> shiftTypeCache = new HashMap<>();

    // Colori predefiniti per i turni
    private static final int COLOR_MORNING = Color.parseColor("#B3E5FC");  // Light Blue
    private static final int COLOR_AFTERNOON = Color.parseColor("#FFE0B2"); // Light Orange
    private static final int COLOR_NIGHT = Color.parseColor("#E1BEE7");    // Light Purple
    private static final int COLOR_CUSTOM1 = Color.parseColor("#C8E6C9");  // Light Green
    private static final int COLOR_CUSTOM2 = Color.parseColor("#FFCDD2");  // Light Red

    // Definizione dei turni standard
    public static final ShiftType MORNING = new ShiftType(
            "Mattino",
            "Turno del mattino (5-13)",
            5, 0,
            8, 0,
            COLOR_MORNING);

    public static final ShiftType AFTERNOON = new ShiftType(
            "Pomeriggio",
            "Turno del pomeriggio (13-21)",
            13, 0,
            8, 0,
            COLOR_AFTERNOON);

    public static final ShiftType NIGHT = new ShiftType(
            "Notte",
            "Turno notturno (21-5)",
            21, 0,
            8, 0,
            COLOR_NIGHT);

    public static final ShiftType CUSTOM1 = new ShiftType(
            "Personalizzato1",
            "Turno personalizzato 1",
            0, 0,
            0, 0,
            COLOR_CUSTOM1);

    public static final ShiftType CUSTOM2 = new ShiftType(
            "Personalizzato2",
            "Turno personalizzato 2",
            0, 0,
            0, 0,
            COLOR_CUSTOM2);

    // Inizializza la cache con i tipi predefiniti
    static {
        shiftTypeCache.put("MORNING", MORNING);
        shiftTypeCache.put("AFTERNOON", AFTERNOON);
        shiftTypeCache.put("NIGHT", NIGHT);
        shiftTypeCache.put("CUSTOM1", CUSTOM1);
        shiftTypeCache.put("CUSTOM2", CUSTOM2);
    }

    /**
     * Non permettere l'istanziazione
     */
    private ShiftTypeFactory() {}

    /**
     * Ottiene il numero di turni configurati.
     *
     * @param context Contesto dell'applicazione
     * @return Numero di turni
     */
    public static int getShiftCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SHIFT_COUNT, DEFAULT_SHIFT_COUNT);
    }

    /**
     * Imposta il numero di turni.
     *
     * @param context Contesto dell'applicazione
     * @param count Numero di turni
     */
    public static void setShiftCount(Context context, int count) {
        if (count < 1) count = 1; // Almeno un turno
        if (count > 5) count = 5; // Massimo 5 turni

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_SHIFT_COUNT, count);
        editor.apply();
    }

    /**
     * Salva un tipo di turno nelle preferenze.
     *
     * @param context Contesto dell'applicazione
     * @param index Indice del turno (0-based)
     * @param shiftType Tipo di turno da salvare
     */
    public static void saveShiftType(Context context, int index, ShiftType shiftType) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        editor.putString(KEY_SHIFT_NAME_PREFIX + index, shiftType.getName());
        editor.putString(KEY_SHIFT_DESC_PREFIX + index, shiftType.getDescription());
        editor.putInt(KEY_SHIFT_START_HOUR_PREFIX + index, shiftType.getStartHour());
        editor.putInt(KEY_SHIFT_START_MIN_PREFIX + index, shiftType.getStartMinute());
        editor.putInt(KEY_SHIFT_DURATION_HOURS_PREFIX + index, shiftType.getDurationHours());
        editor.putInt(KEY_SHIFT_DURATION_MINS_PREFIX + index, shiftType.getDurationMinutes());
        editor.putInt(KEY_SHIFT_COLOR_PREFIX + index, shiftType.getColor());

        editor.apply();
    }

    /**
     * Carica un tipo di turno dalle preferenze.
     *
     * @param context Contesto dell'applicazione
     * @param index Indice del turno (0-based)
     * @return Tipo di turno caricato
     */
    public static ShiftType loadShiftType(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Valori predefiniti in base all'indice
        String defaultName = getDefaultName(index);
        String defaultDesc = getDefaultDescription(index);
        int defaultStartHour = getDefaultStartHour(index);  // ✓ FIX
        int defaultColor = getDefaultColor(index);

        // Carica i valori dalle preferenze
        String name = prefs.getString(KEY_SHIFT_NAME_PREFIX + index, defaultName);
        String desc = prefs.getString(KEY_SHIFT_DESC_PREFIX + index, defaultDesc);
        int startHour = prefs.getInt(KEY_SHIFT_START_HOUR_PREFIX + index, defaultStartHour);
        int startMin = prefs.getInt(KEY_SHIFT_START_MIN_PREFIX + index, 0);
        int durationHours = prefs.getInt(KEY_SHIFT_DURATION_HOURS_PREFIX + index, 8);
        int durationMins = prefs.getInt(KEY_SHIFT_DURATION_MINS_PREFIX + index, 0);
        int color = prefs.getInt(KEY_SHIFT_COLOR_PREFIX + index, defaultColor);

        return new ShiftType(name, desc, startHour, startMin, durationHours, durationMins, color);
    }

    /**
     * Restituisce l'ora di inizio predefinita per un turno.
     */
    private static int getDefaultStartHour(int index) {
        switch (index) {
            case 0: return 5;   // Mattino
            case 1: return 13;  // Pomeriggio
            case 2: return 21;  // Notte
            default: return 5;  // Default per turni aggiuntivi
        }
    }

    /**
     * Restituisce il nome predefinito per un turno.
     */
    private static String getDefaultName(int index) {
        switch (index) {
            case 0: return "Mattino";
            case 1: return "Pomeriggio";
            case 2: return "Notte";
            default: return "Turno " + (index + 1);
        }
    }

    /**
     * Restituisce la descrizione predefinita per un turno.
     */
    private static String getDefaultDescription(int index) {
        switch (index) {
            case 0: return "Turno del mattino (5-13)";
            case 1: return "Turno del pomeriggio (13-21)";
            case 2: return "Turno notturno (21-5)";
            default: return "Descrizione turno " + (index + 1);
        }
    }

    /**
     * Restituisce un colore predefinito in base all'indice.
     *
     * @param index Indice del turno
     * @return Colore predefinito
     */
    private static int getDefaultColor(int index) {
        switch (index) {
            case 0: return COLOR_MORNING;
            case 1: return COLOR_AFTERNOON;
            case 2: return COLOR_NIGHT;
            case 3: return COLOR_CUSTOM1;
            case 4: return COLOR_CUSTOM2;
            default: return Color.GRAY;
        }
    }

    /**
     * Carica tutti i tipi di turno configurati.
     *
     * @param context Contesto dell'applicazione
     * @return Lista dei tipi di turno
     */
    public static List<ShiftType> loadAllShiftTypes(Context context) {
        int count = getShiftCount(context);
        List<ShiftType> shiftTypes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            shiftTypes.add(loadShiftType(context, i));
        }

        return shiftTypes;
    }

    /**
     * Reimposta tutti i tipi di turno ai valori predefiniti.
     *
     * @param context Contesto dell'applicazione
     */
    public static void resetToDefaults(Context context) {
        setShiftCount(context, DEFAULT_SHIFT_COUNT);

        // Imposta i turni predefiniti
        saveShiftType(context, 0, MORNING);
        saveShiftType(context, 1, AFTERNOON);
        saveShiftType(context, 2, NIGHT);
    }

    /**
     * Crea un nuovo tipo di turno personalizzato.
     *
     * @param name Nome del turno
     * @param description Descrizione del turno
     * @param startHour Ora di inizio
     * @param startMinute Minuto di inizio
     * @param durationHours Durata in ore
     * @param durationMinutes Durata in minuti aggiuntivi
     * @param color Colore del turno
     * @return Nuovo tipo di turno
     */
    public static ShiftType createCustom(String name, String description,
                                         int startHour, int startMinute,
                                         int durationHours, int durationMinutes,
                                         int color) {
        return new ShiftType(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
    }

    /**
     * Crea un nuovo tipo di turno personalizzato con colore predefinito.
     *
     * @param name Nome del turno
     * @param description Descrizione del turno
     * @param startHour Ora di inizio
     * @param startMinute Minuto di inizio
     * @param durationHours Durata in ore
     * @param durationMinutes Durata in minuti aggiuntivi
     * @return Nuovo tipo di turno
     */
    public static ShiftType createCustom(String name, String description,
                                         int startHour, int startMinute,
                                         int durationHours, int durationMinutes) {
        // Assegna un colore in base al nome (per consistenza)
        int color;
        switch (name.toLowerCase()) {
            case "mattino": color = COLOR_MORNING; break;
            case "pomeriggio": color = COLOR_AFTERNOON; break;
            case "notte": color = COLOR_NIGHT; break;
            default: color = Color.GRAY;
        }

        return createCustom(name, description, startHour, startMinute,
                durationHours, durationMinutes, color);
    }

    /**
     * Ottiene un tipo di turno standard.
     *
     * @param index Indice del turno (1-based)
     * @return Tipo di turno standard
     */
    public static ShiftType getStandardShiftType(int index) {
        switch (index) {
            case 1: return MORNING;
            case 2: return AFTERNOON;
            case 3: return NIGHT;
            case 4: return CUSTOM1;
            case 5: return CUSTOM2;
            default: throw new IllegalArgumentException("Indice turno non valido: " + index);
        }
    }
}
