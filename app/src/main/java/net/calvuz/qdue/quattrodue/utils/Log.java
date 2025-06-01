package net.calvuz.qdue.quattrodue.utils;

/**
 * Wrapper class for Android Log.
 *
 * Enhanced version that maintains backward compatibility with original.
 * Provides centralized logging control and optional redirection to
 * warn level to avoid verbose ART messages.
 *
 * @author luke (original)
 * @author Updated 21/05/2025
 */
public final class Log {

    // Default TAG
    private final static String TAG = "QD LOG";

    // Debug enabled - default true for backward compatibility
    private final static boolean QD_DEBUG = true;

    // Redirect debug and info to warn to avoid annoying ART messages
    private final static boolean QD_DEBUG_TO_WARN = false;

    /**
     * Log with VERBOSE level.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
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
     * Log with DEBUG level, using default tag.
     *
     * @param msg Message to log
     */
    public static void d(String msg) {
        if (QD_DEBUG) {
            d(TAG, msg);
        }
    }

    /**
     * Log with DEBUG level.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
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
     * Log with INFO level.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
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
     * Log with WARNING level.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
     */
    public static void w(String tag, String msg) {
        if (QD_DEBUG) {
            android.util.Log.w(tag, msg);
        }
    }

    /**
     * Log with ERROR level.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
     */
    public static void e(String tag, String msg) {
        if (QD_DEBUG) {
            android.util.Log.e(tag, msg);
        }
    }

    /**
     * Log with DEBUG level including exception.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
     * @param t Exception to log
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
     * Log with ERROR level including exception.
     *
     * @param tag Tag to identify log source
     * @param msg Message to log
     * @param t Exception to log
     */
    public static void e(String tag, String msg, Throwable t) {
        if (QD_DEBUG) {
            android.util.Log.e(tag, msg, t);
        }
    }
}