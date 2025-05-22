package net.calvuz.qdue.quattrodue;

import android.content.Context;
import android.content.res.Resources;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Month;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.utils.HalfTeamFactory;
import net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory;
import net.calvuz.qdue.utils.Log;

/**
 * Classe principale dell'applicazione QuattroDue.
 * Implementa il pattern Singleton e gestisce lo stato dell'applicazione.
 * Corregge il calcolo dei giorni per l'applicazione dello schema.
 */
public class QuattroDue {

    private static final String TAG = "QuattroDue";
    private static final boolean LOG_ENABLED = true;

    // Costanti
    private static final int CAL_NR_MONTHS = 3;

    // Schema di rotazione dei turni (fisso)
    private static final char[][][] SCHEME = new char[][][]{
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'B'}, {'C', 'D'}, {'E', 'F'}, {'G', 'H', 'I'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'A', 'H'}, {'D', 'I'}, {'G', 'F'}, {'E', 'C', 'B'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'H'}, {'E', 'I'}, {'G', 'B'}, {'A', 'D', 'F'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'C', 'D'}, {'E', 'F'}, {'A', 'B'}, {'G', 'H', 'I'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'D', 'I'}, {'G', 'F'}, {'A', 'H'}, {'E', 'C', 'B'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'I'}, {'G', 'B'}, {'C', 'H'}, {'A', 'D', 'F'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'E', 'F'}, {'A', 'B'}, {'C', 'D'}, {'G', 'H', 'I'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'F'}, {'A', 'H'}, {'D', 'I'}, {'E', 'C', 'B'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}},
            {{'G', 'B'}, {'C', 'H'}, {'E', 'I'}, {'A', 'D', 'F'}}
    };

    // Numero di giorni nella ripetizione del ciclo
    private static final int NUMERO_RIPETIZIONE = SCHEME.length;

    // Numero di turni per giorno e parametri delle squadre
    private static final int NUMERO_TURNI_AL_GIORNO = 3;
    private static final int NUMERO_SEMISQUADRE = 9;

    // Data di inizio dello schema
    private static final int SCHEME_START_DAY = 7;
    private static final int SCHEME_START_MONTH = 11;
    private static final int SCHEME_START_YEAR = 2018;

    // Lista di tutte le squadre disponibili
    public static final List<HalfTeam> HALFTEAM_ALL = initializeAllTeams();

    // Istanza singleton
    private static QuattroDue instance;

    // Contesto dell'applicazione
    private final Context context;

    // Dati di stato
    private List<Day> schemeDaysList;
    private List<Month> months;
    private List<ShiftType> shiftTypes;
    private LocalDate schemeDate;
    private LocalDate cursorDate;
    private HalfTeam userHalfTeam;

    // Flag per le funzionalità
    private boolean showCalendars;
    private boolean showStops;
    private boolean refresh = false;

    /**
     * Inizializza la lista di tutte le squadre disponibili.
     *
     * @return Lista delle squadre
     */
    private static List<HalfTeam> initializeAllTeams() {
        List<HalfTeam> teams = new ArrayList<>(NUMERO_SEMISQUADRE);
        teams.add(new HalfTeam("A", "Squadra A"));
        teams.add(new HalfTeam("B", "Squadra B"));
        teams.add(new HalfTeam("C", "Squadra C"));
        teams.add(new HalfTeam("D", "Squadra D"));
        teams.add(new HalfTeam("E", "Squadra E"));
        teams.add(new HalfTeam("F", "Squadra F"));
        teams.add(new HalfTeam("G", "Squadra G"));
        teams.add(new HalfTeam("H", "Squadra H"));
        teams.add(new HalfTeam("I", "Squadra I"));
        return teams;
    }

    /**
     * Costruttore privato (pattern Singleton).
     *
     * @param context Contesto dell'applicazione
     */
    private QuattroDue(Context context) {
        this.context = context.getApplicationContext();
        init(context);
    }

    /**
     * Ottiene l'istanza singleton dell'applicazione.
     *
     * @param context Contesto
     * @return Istanza singleton
     */
    public static synchronized QuattroDue getInstance(Context context) {
        if (instance == null) {
            instance = new QuattroDue(context);
        }
        return instance;
    }

    /**
     * Verifica se è necessario un refresh.
     *
     * @return true se è necessario un refresh, false altrimenti
     */
    public boolean isRefresh() {
        return refresh;
    }

    /**
     * Imposta il flag di refresh a true.
     */
    public void setRefresh() {
        setRefresh(true);
    }

    /**
     * Imposta il flag di refresh.
     *
     * @param refresh Valore del flag
     */
    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    /**
     * Inizializza i dati dell'applicazione.
     *
     * @param context Contesto dell'applicazione
     */
    private void init(Context context) {
        final String fTAG = TAG + " init";
        if (LOG_ENABLED) Log.v(fTAG, "start");

        // Inizializzazione delle liste
        schemeDaysList = new ArrayList<>();
        months = new ArrayList<>();

        // Imposta la data cursore al primo del mese corrente
        LocalDate today = LocalDate.now();
        cursorDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
        if (LOG_ENABLED) Log.d(fTAG, "CursorDate " + cursorDate);

        // Imposta la data di inizio schema dalle preferenze o dai valori predefiniti
        if (context == null) {
            schemeDate = LocalDate.of(SCHEME_START_YEAR, SCHEME_START_MONTH, SCHEME_START_DAY);
        } else {
            schemeDate = LocalDate.of(
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_YEAR, SCHEME_START_YEAR),
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_MONTH, SCHEME_START_MONTH),
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_DAY, SCHEME_START_DAY));
        }
        if (LOG_ENABLED) Log.d(fTAG, "SchemeDate " + schemeDate);

        // Imposta la squadra dell'utente (default: A)
        setUserHalfTeam(new HalfTeam("A"));

        // Carica le preferenze
        loadPreferences(context);

        // 1. Inizializza i tipi di turno
        fetchShiftTypes();

        // 2. Genera lo schema base
        fetchSchemeList();

        // 3. Genera i mesi
        fetchMonths(context);

        if (LOG_ENABLED) Log.d(fTAG, "Inizializzazione completata");
    }

    /**
     * Carica le preferenze dell'utente.
     *
     * @param context Contesto dell'applicazione
     */
    private void loadPreferences(Context context) {
        if (context == null) return;

        // Carica le preferenze per le funzionalità
        showCalendars = Preferences.getSharedPreference(context,
                Preferences.KEY_SHOW_CALENDARS, Preferences.VALUE_SHOW_CALENDARS);
        showStops = Preferences.getSharedPreference(context,
                Preferences.KEY_SHOW_STOPS, Preferences.VALUE_SHOW_STOPS);

        // Carica la preferenza del team utente
        String userTeamPref = Preferences.getSharedPreference(context, Preferences.KEY_USER_HALFTEAM, "0");
        try {
            int teamIndex = Integer.parseInt(userTeamPref);
            Resources res = context.getResources();
            String[] halfTeamValues = res.getStringArray(R.array.pref_entries_user_halfteam);
            if (teamIndex >= 0 && teamIndex < halfTeamValues.length) {
                userHalfTeam = new HalfTeam(halfTeamValues[teamIndex]);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Errore nel parsing dell'indice del team: " + e.getMessage());
        }
    }

    /**
     * Genera i mesi visualizzati.
     *
     * @param context Contesto dell'applicazione
     */
    private void fetchMonths(Context context) {
        final String fTAG = TAG + ": fetchMonths";

        try {
            // Genera 3 mesi: precedente-corrente-successivo
            for (int i = 0; i < CAL_NR_MONTHS; i++) {
                // Parte dalla data cursore (oggi durante l'inizializzazione)
                LocalDate monthDate = cursorDate.plusMonths(i - 1);
                if (LOG_ENABLED) {
                    Log.d(fTAG, "Generazione mese per data: " + monthDate +
                            " (data schema: " + schemeDate + ")");
                }

                // Crea il mese e configura i giorni
                Month month = new Month(monthDate);
                month.setDaysList(getShifts(monthDate));

                // Configura altre funzionalità del mese
                if (showStops) {
                    month.setStops();
                }

                // Aggiunge il mese alla lista
                months.add(month);
                if (LOG_ENABLED) Log.d(fTAG, "Mese aggiunto: " + month);
            }
        } catch (Exception e) {
            Log.e(fTAG, "Errore durante la generazione dei mesi: " + e.getMessage());
        }
    }

    /**
     * Inizializza i tipi di turno standard.
     */
    private void fetchShiftTypes() {
        if (LOG_ENABLED) Log.d(TAG, "fetchShiftTypes");

        shiftTypes = new ArrayList<>(3);
        shiftTypes.add(ShiftTypeFactory.MORNING);
        shiftTypes.add(ShiftTypeFactory.AFTERNOON);
        shiftTypes.add(ShiftTypeFactory.NIGHT);
    }

    /**
     * Genera lo schema base dei turni.
     * Crea la lista schemeDaysList con i 18 giorni dello schema ripetuto.
     */
    private void fetchSchemeList() {
        final String fTAG = TAG + ":fetchSchemeList";
        if (LOG_ENABLED) Log.d(fTAG, "start");

        try {
            // Genera i giorni dello schema
            for (int i = 0; i < NUMERO_RIPETIZIONE; i++) {
                // Crea un nuovo giorno
                LocalDate dayDate = schemeDate.plusDays(i);
                Day day = new Day(dayDate);

                // Configura i turni del giorno secondo lo schema
                for (int shiftIndex = 0; shiftIndex < NUMERO_TURNI_AL_GIORNO; shiftIndex++) {
                    // Crea un nuovo turno
                    Shift shift = new Shift(shiftTypes.get(shiftIndex));

                    // Aggiunge le squadre al turno secondo lo schema
                    for (int teamIndex = 0; teamIndex < SCHEME[i][shiftIndex].length; teamIndex++) {
                        char teamName = SCHEME[i][shiftIndex][teamIndex];
                        HalfTeam halfTeam = HalfTeamFactory.getByName(String.valueOf(teamName));
                        shift.addTeam(halfTeam);
                    }

                    // Aggiunge il turno al giorno
                    day.addShift(shift);
                }

                // Aggiunge il giorno allo schema
                schemeDaysList.add(day);

                // Log delle squadre assegnate
                if (LOG_ENABLED) {
                    StringBuilder sb = new StringBuilder("Day[" + (i + 1) + "]{");
                    for (Shift shift : day.getShifts()) {
                        sb.append(shift.toString());
                        for (HalfTeam halfTeam : shift.getHalfTeams()) {
                            sb.append("[").append(halfTeam.getName()).append("]");
                        }
                    }
                    sb.append("}");
                    Log.d(fTAG, sb.toString());
                }
            }

            if (LOG_ENABLED) Log.d(fTAG, "Dimensione schema: " + schemeDaysList.size());
        } catch (Exception e) {
            Log.e(fTAG, "Errore durante la generazione dello schema: " + e.getMessage());
        }
    }

    /**
     * Genera la lista di giorni per un mese specifico, applicando lo schema di rotazione.
     * Corregge il calcolo della differenza di giorni tra la data di inizio schema e la data richiesta.
     *
     * @param ldreq Data di riferimento (mese)
     * @return Lista dei giorni configurati
     */
    private List<Day> getShifts(LocalDate ldreq) {
        final String fTAG = TAG + ":getShifts";
        if (LOG_ENABLED) Log.d(fTAG, "start");

        // Crea una nuova lista per i giorni
        List<Day> result = new ArrayList<>();

        // Verifica che lo schema sia stato inizializzato
        if (schemeDaysList == null || schemeDaysList.isEmpty()) {
            if (LOG_ENABLED) Log.d(fTAG, "schemeDaysList vuota o null");
            return result;
        }

        // Verifica che la data sia valida
        if (ldreq == null) {
            if (LOG_ENABLED) Log.d(fTAG, "Data richiesta null");
            return result;
        }

        try {
            // Imposta la data al primo giorno del mese
            LocalDate mRequestedStartDate = LocalDate.of(ldreq.getYear(), ldreq.getMonth(), 1);
            if (LOG_ENABLED) Log.d(fTAG, "mRequestedStartDate=" + mRequestedStartDate);

            // Calcola i giorni tra la data di inizio schema e il primo giorno del mese
            // Utilizziamo ChronoUnit.DAYS per ottenere il numero esatto di giorni
            long difference = ChronoUnit.DAYS.between(schemeDate, mRequestedStartDate);
            if (LOG_ENABLED) Log.d(fTAG, "difference=" + difference);

            // Calcola il punto di partenza nello schema
            int startoffset = (int) (difference % NUMERO_RIPETIZIONE);
            // Gestisce i valori negativi correttamente
            if (startoffset < 0) {
                startoffset = NUMERO_RIPETIZIONE + startoffset;
            }
            if (LOG_ENABLED) Log.d(fTAG, "startoffset=" + startoffset);

            // Ottiene il numero di giorni nel mese
            int maxdays = mRequestedStartDate.lengthOfMonth();
            if (LOG_ENABLED) Log.d(fTAG, "maxdays=" + maxdays);

            // Genera i giorni del mese
            LocalDate currentDate = mRequestedStartDate;

            for (int i = 0; i < maxdays; i++) {
                // Clona un giorno dallo schema
                Day templateDay = schemeDaysList.get(startoffset);
                Day day = templateDay.clone();

                // Imposta la data corretta
                day.setLocalDate(currentDate);

                // Aggiunge il giorno alla lista
                result.add(day);

                // Passa al giorno e schema successivi
                currentDate = currentDate.plusDays(1);
                startoffset = (startoffset + 1) % NUMERO_RIPETIZIONE;
            }

            if (LOG_ENABLED) Log.d(fTAG, "Generati " + result.size() + " giorni per il mese");
        } catch (Exception e) {
            Log.e(fTAG, "Errore durante la generazione dei giorni: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Muove la visualizzazione al mese successivo.
     */
    public void moveForward() {
        if (LOG_ENABLED) Log.d(TAG, "moveForward");

        try {
            // Aggiorna la data cursore
            cursorDate = cursorDate.plusMonths(1);

            // Rimuove il primo mese
            months.remove(0);

            // Crea il nuovo mese
            LocalDate newMonthDate = cursorDate.plusMonths(1);
            Month newMonth = new Month(newMonthDate);
            newMonth.setDaysList(getShifts(newMonthDate));

            // Configura le funzionalità aggiuntive
            if (showStops) {
                newMonth.setStops();
            }

            // Aggiunge il nuovo mese alla lista
            months.add(newMonth);

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese: " + newMonth);
        } catch (Exception e) {
            Log.e(TAG, "Errore durante l'avanzamento al mese successivo: " + e.getMessage());
        }
    }

    /**
     * Muove la visualizzazione al mese precedente.
     */
    public void moveBackward() {
        if (LOG_ENABLED) Log.d(TAG, "moveBackward");

        try {
            // Aggiorna la data cursore
            cursorDate = cursorDate.minusMonths(1);

            // Rimuove l'ultimo mese
            months.remove(2);

            // Crea il nuovo mese
            LocalDate newMonthDate = cursorDate.minusMonths(1);
            Month newMonth = new Month(newMonthDate);
            newMonth.setDaysList(getShifts(newMonthDate));

            // Configura le funzionalità aggiuntive
            if (showStops) {
                newMonth.setStops();
            }

            // Aggiunge il nuovo mese all'inizio della lista
            months.add(0, newMonth);

            if (LOG_ENABLED) Log.d(TAG, "Aggiunto mese: " + newMonth);
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il ritorno al mese precedente: " + e.getMessage());
        }
    }

    /**
     * Restituisce il mese corrente.
     *
     * @return Mese corrente
     */
    public Month getMonth() {
        return getMonth(1);
    }

    /**
     * Restituisce un mese specifico dalla lista.
     *
     * @param position Posizione nella lista (0=precedente, 1=corrente, 2=successivo)
     * @return Mese richiesto
     */
    private Month getMonth(int position) {
        if (position >= 0 && position < months.size()) {
            return months.get(position);
        }
        return null;
    }

    /**
     * Restituisce la data cursore.
     *
     * @return Data cursore
     */
    public LocalDate getCursorDate() {
        return cursorDate;
    }

    /**
     * Restituisce la squadra dell'utente.
     *
     * @return Squadra dell'utente
     */
    public HalfTeam getUserHalfTeam() {
        if (userHalfTeam == null) {
            userHalfTeam = new HalfTeam("A");
        }
        return userHalfTeam;
    }

    /**
     * Imposta la squadra dell'utente.
     *
     * @param halfTeam Squadra dell'utente
     */
    private void setUserHalfTeam(HalfTeam halfTeam) {
        // Verifica la validità e la necessità di aggiornamento
        if (halfTeam == null) return;
        if (userHalfTeam != null && userHalfTeam.isSameTeam(halfTeam)) return;

        // Imposta la nuova squadra
        userHalfTeam = halfTeam;
    }

    /**
     * Aggiorna le preferenze dell'applicazione.
     *
     * @param context Contesto dell'applicazione
     */
    public void updatePreferences(Context context) {
        final String fTAG = TAG + "updatePreferences: ";

        try {
            // Carica le preferenze
            boolean newShowCalendars = Preferences.getSharedPreference(context,
                    Preferences.KEY_SHOW_CALENDARS, Preferences.VALUE_SHOW_CALENDARS);
            boolean newShowStops = Preferences.getSharedPreference(context,
                    Preferences.KEY_SHOW_STOPS, Preferences.VALUE_SHOW_STOPS);

            // Verifica se sono cambiate
            if (showCalendars != newShowCalendars) {
                showCalendars = newShowCalendars;
                setRefresh(true);
            }

            if (showStops != newShowStops) {
                showStops = newShowStops;
                setRefresh(true);
            }

            // Aggiorna la squadra dell'utente
            String[] halfTeamEntries = context.getResources().getStringArray(R.array.pref_entries_user_halfteam);

            if (halfTeamEntries.length > 0) {
                String userTeamPref = Preferences.getSharedPreference(context, Preferences.KEY_USER_HALFTEAM, "0");
                try {
                    int teamIndex = Integer.parseInt(userTeamPref);
                    if (teamIndex >= 0 && teamIndex < halfTeamEntries.length) {
                        HalfTeam newUserHalfTeam = new HalfTeam(halfTeamEntries[teamIndex]);
                        if (userHalfTeam == null || !userHalfTeam.isSameTeam(newUserHalfTeam)) {
                            userHalfTeam = newUserHalfTeam;
                            setRefresh(true);
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e(fTAG, "Errore nel parsing dell'indice del team: " + e.getMessage());
                }
            } else {
                if (LOG_ENABLED) Log.e(fTAG, "Array di squadre vuoto");
            }

            // Aggiorna i mesi se necessario
            if (isRefresh()) {
                months.clear();
                fetchMonths(context);
                setRefresh(false);
            }
        } catch (Exception e) {
            Log.e(fTAG, "Errore durante l'aggiornamento delle preferenze: " + e.getMessage());
        }
    }

    /**
     * Aggiorna completamente i dati dell'applicazione.
     *
     * @param context Contesto dell'applicazione
     */
    public void refresh(Context context) {
        try {
            // Pulisce i dati esistenti
            schemeDaysList.clear();
            months.clear();

            // Reinizializza
            init(context);
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il refresh: " + e.getMessage());
        }
    }

    /**
     * Trova il prossimo turno dell'utente.
     *
     * @return Data del prossimo turno, o null se non trovato
     */
    public LocalDate findNextUserShift() {
        if (userHalfTeam == null || months == null || months.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();

        try {
            // Cerca nei mesi disponibili
            for (Month month : months) {
                for (Day day : month.getDaysList()) {
                    // Consideriamo solo i giorni futuri
                    if (day.getDate().isBefore(today)) {
                        continue;
                    }

                    // Cerchiamo il team nei turni del giorno
                    int shiftIndex = day.getInWichTeamIsHalfTeam(userHalfTeam);
                    if (shiftIndex >= 0) {
                        return day.getDate();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante la ricerca del prossimo turno: " + e.getMessage());
        }

        return null; // Nessun turno trovato
    }

    /**
     * Imposta la visualizzazione dei calendari.
     *
     * @param showCalendars true per mostrare i calendari, false altrimenti
     */
    public void setShowCalendars(boolean showCalendars) {
        this.showCalendars = showCalendars;
    }

    /**
     * Imposta la visualizzazione delle fermate.
     *
     * @param showStops true per mostrare le fermate, false altrimenti
     */
    public void setShowStops(boolean showStops) {
        this.showStops = showStops;
    }


    /**
     * Genera la lista di giorni per un mese specifico.
     * Metodo pubblico per supportare il calendario con scrolling infinito.
     *
     * @param monthDate Data del mese richiesto
     * @return Lista dei giorni configurati per il mese
     */
    public List<Day> getShiftsForMonth(LocalDate monthDate) {
        if (monthDate == null) {
            return new ArrayList<>();
        }

        return getShifts(monthDate);
    }

    /**
     * Verifica se una data è il giorno corrente.
     * Metodo di utilità per il calendario.
     *
     * @param date Data da verificare
     * @return true se è oggi, false altrimenti
     */
    public boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * Imposta il flag "oggi" per un giorno specifico.
     * Aggiorna automaticamente i giorni nella cache.
     *
     * @param day Giorno da aggiornare
     */
    private void updateTodayFlag(Day day) {
        if (day != null && day.getDate() != null) {
            day.setIsToday(isToday(day.getDate()));
        }
    }
}
