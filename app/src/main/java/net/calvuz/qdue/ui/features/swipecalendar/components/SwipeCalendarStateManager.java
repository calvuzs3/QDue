package net.calvuz.qdue.ui.features.swipecalendar.components;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SwipeCalendarStateManager - State persistence for swipe calendar navigation.
 *
 * <p>Manages user's calendar position state using SharedPreferences to provide
 * seamless navigation experience across app sessions. Implements intelligent
 * initial position logic: "today" for first use, saved position for returns.</p>
 *
 * <h3>State Management Strategy:</h3>
 * <ul>
 *   <li><strong>First Launch</strong>: Navigate to current month (today)</li>
 *   <li><strong>Return Session</strong>: Restore last viewed month position</li>
 *   <li><strong>Background Return</strong>: Maintain position if user returns within session</li>
 *   <li><strong>Data Reset</strong>: Clear state and return to today</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <ul>
 *   <li>Atomic operations for concurrent access protection</li>
 *   <li>Synchronized SharedPreferences access</li>
 *   <li>Safe position calculations with bounds checking</li>
 * </ul>
 */
public class SwipeCalendarStateManager {

    private static final String TAG = "SwipeCalendarStateManager";

    // SharedPreferences keys
    private static final String PREF_KEY_CURRENT_POSITION = "swipe_calendar_current_position";
    private static final String PREF_KEY_FIRST_LAUNCH = "swipe_calendar_first_launch";
    private static final String PREF_KEY_LAST_SAVED_MONTH = "swipe_calendar_last_month";
    private static final String PREF_KEY_SESSION_ACTIVE = "swipe_calendar_session_active";

    // Calendar range constants (1900-2100)
    private static final YearMonth BASE_MONTH = YearMonth.of( 1900, 1 );
    private static final YearMonth END_MONTH = YearMonth.of( 2100, 12 );
    private static final int TOTAL_MONTHS = (int) ChronoUnit.MONTHS.between( BASE_MONTH, END_MONTH ) + 1; // 2401 months

    // Dependencies
    private final SharedPreferences mPreferences;

    // State tracking
    private final AtomicBoolean mIsInitialized = new AtomicBoolean( false );
    private volatile int mCurrentPosition = -1;
    private volatile YearMonth mCurrentMonth;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates SwipeCalendarStateManager with dependency injection.
     *
     * @param context Application context for SharedPreferences access
     */
    public SwipeCalendarStateManager(@NonNull Context context) {
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences( context );

        Log.d( TAG, "SwipeCalendarStateManager initialized" );
    }

    // ==================== INITIALIZATION ====================


    /**
     * Initialize state manager and determine initial position.
     * Call this method when the calendar fragment is created.
     *
     * @return Initial adapter position for ViewPager2
     */
    public synchronized int initializeAndGetInitialPosition() {
        if (mIsInitialized.get()) {
            Log.w(TAG, "StateManager already initialized, returning current position");
            return mCurrentPosition;
        }

        try {
            boolean isFirstLaunch = mPreferences.getBoolean(PREF_KEY_FIRST_LAUNCH, true);
            boolean isSessionActive = mPreferences.getBoolean(PREF_KEY_SESSION_ACTIVE, false);

            YearMonth targetMonth;

            if (isFirstLaunch) {
                // ✅ ALWAYS go to today on first launch
                targetMonth = YearMonth.now();
                Log.d(TAG, "First launch detected, navigating to current month: " + targetMonth);

                // Mark as no longer first launch
                mPreferences.edit()
                        .putBoolean(PREF_KEY_FIRST_LAUNCH, false)
                        .putBoolean(PREF_KEY_SESSION_ACTIVE, true)
                        .apply();
            } else if (isSessionActive) {
                // Returning within session - restore saved position
                int savedPosition = mPreferences.getInt(PREF_KEY_CURRENT_POSITION, -1);
                String savedMonthStr = mPreferences.getString(PREF_KEY_LAST_SAVED_MONTH, null);

                if (savedPosition >= 0 && savedPosition < TOTAL_MONTHS && savedMonthStr != null) {
                    try {
                        targetMonth = YearMonth.parse(savedMonthStr);

                        // ✅ RELAXED VALIDATION: Only reject obviously wrong dates (pre-1950 or post-2050)
                        if (targetMonth.getYear() < 1950 || targetMonth.getYear() > 2050) {
                            Log.w(TAG, "Saved month outside reasonable range: " + targetMonth + ", using today instead");
                            targetMonth = YearMonth.now();
                        } else {
                            Log.d(TAG, "Session active, restoring saved position: " + targetMonth);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to parse saved month, fallback to today", e);
                        targetMonth = YearMonth.now();
                    }
                } else {
                    Log.w(TAG, "Invalid saved state, fallback to today");
                    targetMonth = YearMonth.now();
                }
            } else {
                // ✅ NEW session - ALWAYS go to today (not 1900!)
                targetMonth = YearMonth.now();
                Log.d(TAG, "New session, navigating to current month: " + targetMonth);

                // Mark session as active
                mPreferences.edit()
                        .putBoolean(PREF_KEY_SESSION_ACTIVE, true)
                        .apply();
            }

            // Convert target month to position
            mCurrentPosition = getPositionForMonth(targetMonth);
            mCurrentMonth = targetMonth;

            // ✅ BASIC VALIDATION: Only check bounds, not date reasonableness
            if (mCurrentPosition < 0 || mCurrentPosition >= TOTAL_MONTHS) {
                Log.w(TAG, "Position out of bounds: " + mCurrentPosition + ", fallback to today");
                YearMonth today = YearMonth.now();
                mCurrentPosition = getPositionForMonth(today);
                mCurrentMonth = today;
            }

            // Save initial state
            saveCurrentState();

            mIsInitialized.set(true);
            Log.d(TAG, "Initialization complete. Position: " + mCurrentPosition + ", Month: " + mCurrentMonth);

            return mCurrentPosition;
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization, fallback to today", e);

            // ✅ ROBUST FALLBACK: Always fallback to today
            YearMonth today = YearMonth.now();
            mCurrentPosition = getPositionForMonth(today);
            mCurrentMonth = today;

            saveCurrentState();
            mIsInitialized.set(true);

            return mCurrentPosition;
        }
    }

    // ==================== POSITION MANAGEMENT ====================

    /**
     * Update current position and save state.
     * Call this when user swipes to a different month.
     *
     * @param position New adapter position
     */
    public synchronized void updatePosition(int position) {
        if (position < 0 || position >= TOTAL_MONTHS) {
            Log.w( TAG, "Invalid position: " + position + ", ignoring update" );
            return;
        }

        mCurrentPosition = position;
        mCurrentMonth = getMonthForPosition( position );

        saveCurrentState();

        Log.v(TAG, "Position updated to: " + position + " (" + mCurrentMonth + ")");
    }

    /**
     * Get current adapter position.
     *
     * @return Current position, or -1 if not initialized
     */
    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    /**
     * Get current month.
     *
     * @return Current YearMonth, or null if not initialized
     */
    public YearMonth getCurrentMonth() {
        return mCurrentMonth;
    }

    /**
     * Get position for "today" (current month).
     *
     * @return Adapter position for current month
     */
    public int getTodayPosition() {
        return getPositionForMonth( YearMonth.now() );
    }

    /**
     * Navigate to today's month and update state.
     *
     * @return Position for today's month
     */
    public synchronized int navigateToToday() {
        YearMonth today = YearMonth.now();
        int todayPosition = getPositionForMonth( today );

        updatePosition( todayPosition );

        Log.d( TAG, "Navigated to today: " + today + " (position " + todayPosition + ")" );
        return todayPosition;
    }

    // ==================== POSITION CONVERSION ====================

    /**
     * Convert YearMonth to adapter position.
     *
     * @param month Target month
     * @return Adapter position (0-based)
     */
    public static int getPositionForMonth(@NonNull YearMonth month) {
        long months = ChronoUnit.MONTHS.between( BASE_MONTH, month );
        return (int) months;
    }

    /**
     * Convert adapter position to YearMonth.
     *
     * @param position Adapter position
     * @return YearMonth object
     */
    @NonNull
    public static YearMonth getMonthForPosition(int position) {
        return BASE_MONTH.plusMonths( position );
    }

    /**
     * Check if position is within valid range.
     *
     * @param position Position to check
     * @return true if position is valid
     */
    public static boolean isValidPosition(int position) {
        return position >= 0 && position < TOTAL_MONTHS;
    }

    /**
     * Get total number of months in range.
     *
     * @return Total months (2401)
     */
    public static int getTotalMonths() {
        return TOTAL_MONTHS;
    }

    // ==================== STATE PERSISTENCE ====================

    /**
     * Save current state to SharedPreferences.
     */
    private void saveCurrentState() {
        try {
            mPreferences.edit()
                    .putInt( PREF_KEY_CURRENT_POSITION, mCurrentPosition )
                    .putString( PREF_KEY_LAST_SAVED_MONTH, mCurrentMonth.toString() )
                    .putBoolean( PREF_KEY_SESSION_ACTIVE, true )
                    .apply();

            Log.v( TAG, "State saved: position=" + mCurrentPosition + ", month=" + mCurrentMonth );
        } catch (Exception e) {
            Log.e( TAG, "Failed to save state", e );
        }
    }

    /**
     * Mark session as inactive (call when app goes to background).
     */
    public void markSessionInactive() {
        try {
            mPreferences.edit()
                    .putBoolean( PREF_KEY_SESSION_ACTIVE, false )
                    .apply();

            Log.d( TAG, "Session marked as inactive" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to mark session inactive", e );
        }
    }

    /**
     * Clear all saved state (reset to first launch).
     */
    public synchronized void clearState() {
        try {
            mPreferences.edit()
                    .remove( PREF_KEY_CURRENT_POSITION )
                    .remove( PREF_KEY_LAST_SAVED_MONTH )
                    .putBoolean( PREF_KEY_FIRST_LAUNCH, true )
                    .putBoolean( PREF_KEY_SESSION_ACTIVE, false )
                    .apply();

            mCurrentPosition = -1;
            mCurrentMonth = null;
            mIsInitialized.set( false );

            Log.d( TAG, "State cleared, reset to first launch" );
        } catch (Exception e) {
            Log.e( TAG, "Failed to clear state", e );
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if manager is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return mIsInitialized.get();
    }

    /**
     * Get range information for debugging.
     *
     * @return Human readable range description
     */
    @NonNull
    public String getRangeInfo() {
        return String.format( QDue.getLocale(), "Range: %s to %s (%d months), Current: %s (pos %d)",
                BASE_MONTH, END_MONTH, TOTAL_MONTHS, mCurrentMonth, mCurrentPosition );
    }

    // ============ ADD THESE NEW METHODS AT THE END OF THE CLASS ============

    /**
     * Force reset to today's month and clear all saved state.
     * Useful for debugging or when state becomes corrupted.
     *
     * @return Position for today's month
     */
    public synchronized int forceResetToToday() {
        Log.d(TAG, "Force resetting to today");

        // Clear all saved preferences
        mPreferences.edit()
                .remove(PREF_KEY_CURRENT_POSITION)
                .remove(PREF_KEY_LAST_SAVED_MONTH)
                .putBoolean(PREF_KEY_FIRST_LAUNCH, true)
                .putBoolean(PREF_KEY_SESSION_ACTIVE, false)
                .apply();

        // Reset to today
        YearMonth today = YearMonth.now();
        mCurrentPosition = getPositionForMonth(today);
        mCurrentMonth = today;

        // Save new state
        saveCurrentState();

        Log.d(TAG, "Force reset complete. Position: " + mCurrentPosition + ", Month: " + mCurrentMonth);
        return mCurrentPosition;
    }

}