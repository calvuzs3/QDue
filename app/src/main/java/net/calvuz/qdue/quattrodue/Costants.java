package net.calvuz.qdue.quattrodue;


/**
 * Constants
 *
 * Created by luke on 16/09/17.
 */

public final class Costants {

    // QuattroDue Log (module scope)
    public static final boolean QD_LOG_ENABLED = false;

    // Definizione dei turni
    public final static int QD_SHIFTS = 4;
    public final static int QD_SHIFTS_PER_DAY = 3;
    public final static int QD_TEAMS = 9;
    public final static int QD_TEAMS_PER_SHIFT = 2;

    // Numero di mesi da mantenere in cache (prima e dopo il mese corrente)
    public static final int QD_MONTHS_CACHE_SIZE = 6;

    // Numero massimo di mesi in memoria per evitare memory leak
    public static final int QD_MAX_CACHE_SIZE = 24;

    // Initial date of the scheme
    public final static int QD_SCHEME_START_DAY = 7;
    public final static int QD_SCHEME_START_MONTH = 11;
    public final static int QD_SCHEME_START_YEAR = 2018;

    // SCheme
    public final static char[][][] QD_SCHEME = new char[][][]{
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
}
