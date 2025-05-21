package net.calvuz.qdue.quattrodue;


/**
 * Constants
 *
 * Created by luke on 16/09/17.
 */

public final class Costants {

    // Number of months in memory
    public final static int CAL_NR_MONTHS             = 3;

    // Which  day is Sunday
    public final static int CAL_SUNDAY                  = 7;

    // Initial date of the scheme
    public final static int SCHEME_START_DAY = 7;
    public final static int SCHEME_START_MONTH = 11;
    public final static int SCHEME_START_YEAR = 2018;

    // SCheme
    public final static char[][][] SCHEME = new char[][][]{
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

    // QD
    public final static class QD {

        // Calendar Permission
        // Dont need this anymore
//        public static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 10101;

        // Shared Preferences
//        public static final String PREFNAME_CALENDARCDG     = "qd";



        // Definizione dei turni
        public final static int NUMERO_TURNI_AL_GIORNO      = 3;
        public final static int NUMERO_SEMISQUADRE          = 9;
        public final static int NUMERO_SEMISQUADRE_TURNO    = 2;

        // La ripetizione nello schema 4-2 si ripete ogni 18 giorni
        public final static int NUMERO_RIPETIZIONE          = 18;


    }
}
