package net.calvuz.qdue.ui.core.common.utils;

/**
 * Wrapper class for Log.
 * Questa classe è stata migliorata ma mantiene la retrocompatibilità
 * con la versione originale.
 *
 * Created on 20/09/17.
 * Updated on 21/05/2025.
 *
 * @author calvuzs3
 */
public final class Log {

    // Default TAG
    private final static String TAG = "QD LOG";

    // Debug - default a true per mantenere la retrocompatibilità
    private final static boolean QD_DEBUG = true;

    // Wrap the debug and info to the warn to avoid art annoying messages
    private final static boolean QD_DEBUG_TO_WARN = false;

    /**
     * Log con livello VERBOSE.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     */
    public static void v(String tag, String msg) {
        if (QD_DEBUG) {
            if (QD_DEBUG_TO_WARN) {
                android.util.Log.w(tag, msg);
            } else {
                android.util.Log.v(tag, msg);
            }
        }
    }

    /**
     * Log con livello DEBUG, utilizzando il tag predefinito.
     *
     * @param msg Messaggio da loggare
     */
    public static void d(String msg) {
        if (QD_DEBUG) {
            d(TAG, msg);
        }
    }

    /**
     * Log con livello DEBUG.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     */
    public static void d(String tag, String msg) {
        if (QD_DEBUG) {
            if (QD_DEBUG_TO_WARN) {
                android.util.Log.w(tag, msg);
            } else {
                android.util.Log.d(tag, msg);
            }
        }
    }

    /**
     * Log con livello INFO.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     */
    public static void i(String tag, String msg) {
        if (QD_DEBUG) {
            if (QD_DEBUG_TO_WARN) {
                android.util.Log.w(tag, msg);
            } else {
                android.util.Log.i(tag, msg);
            }
        }
    }

    /**
     * Log con livello WARNING.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     */
    public static void w(String tag, String msg) {
        if (QD_DEBUG) {
            android.util.Log.w(tag, msg);
        }
    }

    /**
     * Log con livello WARNING.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     * @param e Eccezione da loggare
     */
    public static void w(String tag, String msg, Exception e) {
        if (QD_DEBUG) {
            android.util.Log.w(tag, msg, e);
        }
    }

    /**
     * Log con livello ERROR.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     */
    public static void e(String tag, String msg) {
        if (QD_DEBUG) {
            android.util.Log.e(tag, msg);
        }
    }

    /**
     * Log con livello DEBUG che include eccezione.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     * @param t Eccezione da loggare
     */
    public static void d(String tag, String msg, Throwable t) {
        if (QD_DEBUG) {
            if (QD_DEBUG_TO_WARN) {
                android.util.Log.w(tag, msg, t);
            } else {
                android.util.Log.d(tag, msg, t);
            }
        }
    }

    /**
     * Log con livello ERROR che include eccezione.
     *
     * @param tag Tag per identificare la sorgente del log
     * @param msg Messaggio da loggare
     * @param t Eccezione da loggare
     */
    public static void e(String tag, String msg, Throwable t) {
        if (QD_DEBUG) {
            android.util.Log.e(tag, msg, t);
        }
    }
}
