package net.calvuz.qdue.core.domain.quattrodue;

import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_LOG_ENABLED;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_SHIFTS_PER_DAY;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_TEAMS;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_SCHEME;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_SCHEME_START_DAY;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_SCHEME_START_MONTH;
import static net.calvuz.qdue.core.domain.quattrodue.Costants.QD_SCHEME_START_YEAR;
import static net.calvuz.qdue.core.domain.quattrodue.Preferences.VALUE_SHOW_CALENDARS;
import static net.calvuz.qdue.core.domain.quattrodue.Preferences.VALUE_SHOW_STOPS;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.domain.quattrodue.models.Day;
import net.calvuz.qdue.core.domain.quattrodue.models.HalfTeam;
import net.calvuz.qdue.core.domain.quattrodue.models.LegacyShift;
import net.calvuz.qdue.core.domain.quattrodue.models.LegacyShiftType;
import net.calvuz.qdue.core.domain.quattrodue.utils.HalfTeamFactory;
import net.calvuz.qdue.core.domain.quattrodue.utils.ShiftTypeFactory;
import net.calvuz.qdue.core.domain.quattrodue.utils.Log;

/**
 * QuattroDue.
 * Pattern Singleton.
 * Calculate the scheme of shifts
 */
public class QuattroDue {

    // TAG
    private static final String TAG = QuattroDue.class.getSimpleName();

    // Last Update - it checks for systems changes
    private LocalDate lastKnownDate;
    private long lastUpdateTime;

    // All teams
    public static final List<HalfTeam> HALFTEAM_ALL = initializeAllTeams();

    // instance
    private static QuattroDue instance;

    // Private members
    private List<Day> schemeDaysList;
    private List<LegacyShiftType> legacyShiftTypes;
    private LocalDate schemeDate;
    private LocalDate cursorDate;
    private HalfTeam userHalfTeam;

    // Private flags
    private boolean showCalendars;
    private boolean showStops;
    private boolean refresh = false;

    /* ===== FUNCTIONAL ===== */

    /**
     * Private Constructor (pattern Singleton).
     *
     * @param context context
     */
    private QuattroDue(Context context) {
        init(context);
    }

    /**
     * Get an instance
     *
     * @param context context
     * @return QuattroDue instance
     */
    public static synchronized QuattroDue getInstance(Context context) {
        if (instance == null) {
            instance = new QuattroDue(context);
        }
        return instance;
    }

    /* ===== PUBLIC ===== */

    /**
     * Get the cursor date
     *
     * @return cursor date
     */
    public LocalDate getCursorDate() {
        return cursorDate;
    }

    /**
     * Check if a refresh is needed
     */
    public boolean isRefresh() {
        return refresh;
    }

    /**
     * Set refresh flag.
     *
     * @param refresh refresh yes or no
     */
    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }


    /**
     * Create a list of days for a specified month using the Scheme
     * It calculate the difference between the beginning of the scheme
     * and the requested date
     *
     * @param date requested date (month)
     * @return Days list
     */
    private List<Day> getShifts(@NonNull LocalDate date) {

        // TAG
        final String TAG = QuattroDue.TAG + ":getShifts";
        Log.v(TAG, "start");

        // new List
        List<Day> result = new ArrayList<>();

        // Check the Scheme
        if (schemeDaysList == null || schemeDaysList.isEmpty()) {
            Log.e(TAG, "schemeDaysList empty or null");
            return result;
        }

        try {
            // Set the date to the first day of month
            LocalDate mRequestedStartDate = LocalDate.of(date.getYear(), date.getMonth(), 1);
            Log.v(TAG, "date request to the fist of month: " + mRequestedStartDate);

            // Calculate the days between Scheme Initial date
            // and the first day of month
            // Lets use ChronoUnit.DAYS to get an exact number of days
            long difference = ChronoUnit.DAYS.between(schemeDate, mRequestedStartDate);
            Log.v(TAG, "days difference: " + difference);

            // Calculate offset
            int startoffset = (int) (difference % QD_SCHEME.length);

            // negative values..
            if (startoffset < 0) {
                startoffset = QD_SCHEME.length + startoffset;
            }
            if (QD_LOG_ENABLED) Log.d(TAG, "startoffset=" + startoffset);

            // get month's days
            int maxdays = mRequestedStartDate.lengthOfMonth();
            Log.v(TAG, "number of days in month: " + maxdays);

            // create Days
            LocalDate currentDate = mRequestedStartDate;

            for (int i = 0; i < maxdays; i++) {
                // Clone a Day of the Scheme
                Day templateDay = schemeDaysList.get(startoffset);
                Day day = templateDay.clone();

                // Set correct date
                day.setLocalDate(currentDate);

                // Add Day to the list
                result.add(day);

                // Next
                currentDate = currentDate.plusDays(1);
                startoffset = (startoffset + 1) % QD_SCHEME.length;
            }

            Log.v(TAG, "Days created for the month: " + result.size());
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Update Preferences
     *
     * @param context context
     */
    public void updatePreferences(@NonNull Context context) {

        // TAG
        final String TAG = QuattroDue.TAG + "updatePreferences()";

        try {
            // LoadPreferences
            boolean newShowCalendars = Preferences.getSharedPreference(context,
                    Preferences.KEY_SHOW_CALENDARS, VALUE_SHOW_CALENDARS);
            boolean newShowStops = Preferences.getSharedPreference(context,
                    Preferences.KEY_SHOW_STOPS, VALUE_SHOW_STOPS);

            // Check for changes in Preferences
            if (showCalendars != newShowCalendars) {
                showCalendars = newShowCalendars;
                setRefresh(true);
            }

            if (showStops != newShowStops) {
                showStops = newShowStops;
                setRefresh(true);
            }

            // NEW: Check for scheme date changes
            LocalDate newSchemeDate = Preferences.getSchemeStartDate(context);
            if (!newSchemeDate.equals(this.schemeDate)) {
                Log.v(TAG, "Scheme date changed - regenerating");
                regenerateSchemeWithNewDate(context);
            }

            // Update user (half)tean if needed
            // It is already set in the init
            // TODO re-evaluate this
            String[] halfTeamEntries = context.getResources().getStringArray(R.array.pref_entries_user_team);

            if (halfTeamEntries.length > 0) {
                String userTeamPref = Preferences.getSharedPreference(context, Preferences.KEY_USER_TEAM, "0");
                try {
                    int teamIndex = Integer.parseInt(userTeamPref);
                    if (teamIndex >= 0 && teamIndex < halfTeamEntries.length) {
                        HalfTeam newUserHalfTeam = new HalfTeam(halfTeamEntries[teamIndex]);
                        if (userHalfTeam == null || !userHalfTeam.isSameTeamAs(newUserHalfTeam)) {
                            userHalfTeam = newUserHalfTeam;
                            setRefresh(true);
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "empty teams array");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    /**
     * Total Update
     *
     * @param context context
     */
    public void refresh(@NonNull Context context) {

        // TAG
        final String TAG = QuattroDue.TAG + "refresh()";
        Log.v(TAG, "start");

        try {
            // Clear
            schemeDaysList.clear();
//            months.clear();

            // Init
            init(context);

            // Update the time tracking
            updateTimeTracking();

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        Log.v(TAG, "completed");
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
     * Update internal timestamps after a refresh.
     */
    private void updateTimeTracking() {
        lastKnownDate = LocalDate.now();
        lastUpdateTime = System.currentTimeMillis();

        Log.d(TAG, "Time tracking updated: " + lastKnownDate);
    }

    /* ===== PRIVATE ===== */

    /**
     * Init (half)teams
     *
     * @return (half)teams
     */
    private static List<HalfTeam> initializeAllTeams() {
        List<HalfTeam> teams = new ArrayList<>(QD_TEAMS);
        teams.add(new HalfTeam("A"));
        teams.add(new HalfTeam("B"));
        teams.add(new HalfTeam("C"));
        teams.add(new HalfTeam("D"));
        teams.add(new HalfTeam("E"));
        teams.add(new HalfTeam("F"));
        teams.add(new HalfTeam("G"));
        teams.add(new HalfTeam("H"));
        teams.add(new HalfTeam("I"));
        return teams;
    }

    /**
     * Get User HalfTeam
     *
     * @return user halfteam
     */
    public HalfTeam getUserHalfTeam() {

        // TAG
        final String TAG = QuattroDue.TAG + " getUserHalfTeam()";

        if (userHalfTeam == null) {
            Log.d(TAG, "userHalfTeam is null - revert to the default");
            userHalfTeam = new HalfTeam("A");
        }
        return userHalfTeam;
    }

    /**
     * Set user halfteam
     *
     * @param halfTeam user halfteam
     */
    private void setUserHalfTeam(@NonNull HalfTeam halfTeam) {

        // TAG
        final String TAG = QuattroDue.TAG + " setUserHalfTeam()";
        Log.v(TAG, "start");

        if (userHalfTeam != null && userHalfTeam.equals(halfTeam)) {
            Log.d(TAG, "Change required but equals to the previous");
            return;
        }

        // Set user (half)team
        userHalfTeam = halfTeam;

        Log.v(TAG, "completed");
    }

    /**
     * Load LegacyShift Types
     */
    private void fetchShiftTypes() {

        // TAG
        final String TAG = QuattroDue.TAG + " fetchShiftTypes()";
        Log.v(TAG, "start");

        legacyShiftTypes = ShiftTypeFactory.getAllShiftTypes();

//        legacyShiftTypes = new ArrayList<>(3);
//        legacyShiftTypes.add(ShiftTypeFactory.MORNING);
//        legacyShiftTypes.add(ShiftTypeFactory.AFTERNOON);
//        legacyShiftTypes.add(ShiftTypeFactory.NIGHT);

        Log.v(TAG, "completed");
    }

    /**
     * Create the shifts scheme
     * A list with 18 days
     */
    private void fetchSchemeList() {

        // TAG
        final String TAG = QuattroDue.TAG + ":fetchSchemeList";
        Log.v(TAG, "start");

        try {
            // Create Days
            for (int i = 0; i < QD_SCHEME.length; i++) {
                // Create a new Day
                LocalDate dayDate = schemeDate.plusDays(i);
                Day day = new Day(dayDate);

                // Setup shifts for the day
                for (int shiftIndex = 0; shiftIndex < QD_SHIFTS_PER_DAY; shiftIndex++) {
                    // Create a new LegacyShift
                    LegacyShift legacyShift = new LegacyShift( legacyShiftTypes.get(shiftIndex));

                    // Add (half)teams
                    for (int teamIndex = 0; teamIndex < QD_SCHEME[i][shiftIndex].length; teamIndex++) {
                        char teamName = QD_SCHEME[i][shiftIndex][teamIndex];
                        HalfTeam halfTeam = HalfTeamFactory.getByName(String.valueOf(teamName));
                        legacyShift.addTeam(halfTeam);
                    }

                    // Add LegacyShift to Day
                    day.addShift( legacyShift );
                }

                // Add Day to Scheme
                schemeDaysList.add(day);

                // Verbose Log
                StringBuilder sb = new StringBuilder("Day[" + (i + 1) + "]{");
                for (LegacyShift legacyShift : day.getShifts()) {
                    sb.append( legacyShift.toString());
                    for (HalfTeam halfTeam : legacyShift.getHalfTeams()) {
                        sb.append("[").append(halfTeam.getName()).append("]");
                    }
                }
                sb.append("}");
                Log.v(TAG, sb.toString());
            }

            Log.d(TAG, "Scheme days size: " + schemeDaysList.size());
        } catch (Exception e) {

            Log.e(TAG, "Error: " + e.getMessage());
        }

        Log.v(TAG, "completed");
    }

    /**
     * Load User Preferences
     * It also sets the user (half)team
     *
     * @param context context
     */
    private void loadPreferences(Context context) {

        // TAG
        final String TAG = QuattroDue.TAG + " loadPreferences()";
        Log.v(TAG, "start");

        ///  Che if Context is null
        if (context == null) return;

        // Load preferences CORE
        // sets to defaults if there are not any
        showCalendars = Preferences.getSharedPreference(context,
                Preferences.KEY_SHOW_CALENDARS, VALUE_SHOW_CALENDARS);
        showStops = Preferences.getSharedPreference(context,
                Preferences.KEY_SHOW_STOPS, VALUE_SHOW_STOPS);

        // Load preferences USER
        String userTeamPref = Preferences.getSharedPreference(context, Preferences.KEY_USER_TEAM, "0");
        try {
            int teamIndex = Integer.parseInt(userTeamPref);
            Resources res = context.getResources();
            String[] halfTeamValues = res.getStringArray(R.array.pref_entries_user_team);
            if (teamIndex >= 0 && teamIndex < halfTeamValues.length) {
                // preference
                setUserHalfTeam(new HalfTeam(halfTeamValues[teamIndex]));
                Log.v(QuattroDue.TAG, "User Team: " + userHalfTeam.getName());
            }
        } catch (NumberFormatException e) {
            // default
            setUserHalfTeam(new HalfTeam("A"));
            Log.e(QuattroDue.TAG, "Error in parsing user team index: " + e.getMessage());
        }

        Log.v(TAG, "completed");
    }

    /**
     * Initial setup
     *
     * @param context context
     */
    private void init(Context context) {

        // TAG
        final String TAG = QuattroDue.TAG + " init()";
        Log.v(TAG, "start");

        // Setup Lists
        schemeDaysList = new ArrayList<>();
//        months = new ArrayList<>();

        // Imposta la data cursore al primo del mese corrente
        LocalDate today = LocalDate.now();
        cursorDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
        Log.v(TAG, "CursorDate " + cursorDate);

        // Set Scheme initial date from preferences (or defaults)
        if (context == null) {
            Log.v(TAG, "context is null - set schemeDate from defaults");
            schemeDate = LocalDate.of(QD_SCHEME_START_YEAR, QD_SCHEME_START_MONTH, QD_SCHEME_START_DAY);
        } else {
            Log.v(TAG, "set schemeDate from preferences()");

            schemeDate = LocalDate.of(
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_YEAR, QD_SCHEME_START_YEAR),
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_MONTH, QD_SCHEME_START_MONTH),
                    Preferences.getSharedPreference(context, Preferences.KEY_SCHEME_START_DAY, QD_SCHEME_START_DAY));
            Log.v(TAG, "SchemeDate " + schemeDate);
        }

        // Load preferences
        Log.v(TAG, "call loadPreferences()");
        loadPreferences(context);

        // Setup Shifts
        Log.v(TAG, "call fetchShiftTypes()");
        fetchShiftTypes();

        // Setup Scheme
        Log.v(TAG, "call fetchSchemeList()");
        fetchSchemeList();

        // Setup Months
        Log.v(TAG, "call fetchMonths()");
//        fetchMonths(context);

        Log.v(TAG, "completed");

    }

    /**
     * Force regeneration of the entire scheme with new start date
     * Called when scheme start date is changed via preferences
     *
     * @param context Context
     * @return true if regeneration successful
     */
    public boolean regenerateSchemeWithNewDate(Context context) {
        final String TAG = QuattroDue.TAG + ":regenerateSchemeWithNewDate";
        Log.v(TAG, "start");

        try {
            // Get new scheme date from preferences
            LocalDate newSchemeDate = Preferences.getSchemeStartDate(context);
            Log.v(TAG, "New scheme date: " + newSchemeDate);

            // Update internal scheme date
            this.schemeDate = newSchemeDate;

            // Clear existing scheme data
            if (schemeDaysList != null) {
                schemeDaysList.clear();
            }

            // Regenerate scheme with new date
            fetchSchemeList();

            // Force refresh flag
            setRefresh(true);

            Log.v(TAG, "Scheme successfully regenerated with new date");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error regenerating scheme: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update scheme date and regenerate if changed
     *
     * @param context Context
     */
    public void updateSchemeDate(Context context) {
        final String TAG = QuattroDue.TAG + ":updateSchemeDate";

        LocalDate newSchemeDate = Preferences.getSchemeStartDate(context);

        if (!newSchemeDate.equals(this.schemeDate)) {
            Log.v(TAG, "Scheme date changed from " + this.schemeDate + " to " + newSchemeDate);
            regenerateSchemeWithNewDate(context);
        }
    }

    /**
     * Get current scheme start date
     *
     * @return Current scheme start date
     */
    public LocalDate getSchemeDate() {
        return schemeDate;
    }

}
