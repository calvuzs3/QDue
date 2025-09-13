package net.calvuz.qdue.ui.core.common.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * BroadcastReceiver for detecting system time changes and automatically updating the application.
 *
 * <p>This receiver monitors various time-related system broadcasts and ensures that the shift
 * scheduling system remains accurate when the device's date, time, or timezone changes.
 * It provides both automatic QuattroDue updates and optional listener notifications for
 * UI components that need to respond to time changes.
 *
 * <p>Monitored System Events:
 * - {@link Intent#ACTION_TIME_CHANGED}: Manual time adjustments
 * - {@link Intent#ACTION_DATE_CHANGED}: Date changes (including day rollover)
 * - {@link Intent#ACTION_TIMEZONE_CHANGED}: Timezone modifications
 * - {@link Intent#ACTION_TIME_TICK}: Periodic minute updates (optional)
 *
 * <p>Features:
 * - Automatic QuattroDue refresh on critical time changes
 * - Optional listener interface for UI updates
 * - Configurable logging for debugging
 * - Two modes: full monitoring or critical events only
 * - Safe error handling with graceful degradation
 *
 * <p>Usage in Activity:
 * <pre>
 * // Register for critical events only
 * TimeChangeReceiver receiver = new TimeChangeReceiver(this);
 * IntentFilter filter = TimeChangeReceiver.createOnlyCriticalIntentFilter();
 * registerReceiver(receiver, filter);
 *
 * // Don't forget to unregister
 * unregisterReceiver(receiver);
 * </pre>
 *
 * <p>Usage in Manifest (for background operation):
 * <pre>
 * &lt;receiver android:name=".utils.TimeChangeReceiver"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.intent.action.TIME_SET" /&gt;
 *         &lt;action android:name="android.intent.action.DATE_CHANGED" /&gt;
 *         &lt;action android:name="android.intent.action.TIMEZONE_CHANGED" /&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/receiver&gt;
 * </pre>
 *
 * @author QDue Team
 * @version 1.0
 * @since 2025
 */
public class TimeChangeReceiver
        extends BroadcastReceiver
{

    // ==================== CONSTANTS ====================

    /**
     * Tag for logging purposes
     */
    private static final String TAG = "TimeChangeReceiver";

    /**
     * Enable/disable logging for debugging purposes
     */
    private static final boolean LOG_ENABLED = true;

    // ==================== LISTENER INTERFACE ====================

    /**
     * Interface for receiving notifications about time-related system changes.
     *
     * <p>Implement this interface to get callbacks when the system time, date, or timezone
     * changes. This is useful for updating UI components, refreshing displays, or
     * triggering application-specific time-sensitive operations.
     *
     * <p>All methods are called on the main thread, so UI updates can be performed directly.
     * For long-running operations, consider using background threads.
     */
    public interface TimeChangeListener
    {

        /**
         * Called when the system time is manually changed by the user.
         * This typically happens when the user adjusts the time in settings.
         *
         * @implNote This is called in addition to any automatic updates performed by TimeChangeReceiver
         */
        void onTimeChanged();

        /**
         * Called when the system date changes.
         * This includes both manual date changes and natural day rollover at midnight.
         *
         * @implNote This is called in addition to any automatic updates performed by TimeChangeReceiver
         */
        void onDateChanged();

        /**
         * Called when the system timezone is changed.
         * This happens when the user changes timezone settings or when automatic
         * timezone detection updates the current timezone.
         *
         * @implNote This is called in addition to any automatic updates performed by TimeChangeReceiver
         */
        void onTimezoneChanged();
    }

    // ==================== MEMBER VARIABLES ====================

    /**
     * Optional listener for time change notifications
     */
    private TimeChangeListener listener;

    /**
     * Saved date for comparison during time tick
     */
    private LocalDate savedDate = null;
    private boolean savedDateFlag = false; // Flag for future use

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for manifest-declared receivers.
     *
     * <p>This constructor is used when the receiver is declared in the AndroidManifest.xml
     * and instantiated by the Android system. No listener will be available in this case.
     */
    public TimeChangeReceiver() {
        // Empty constructor for manifest registration
    }

    /**
     * Constructor with listener for programmatic registration.
     *
     * <p>Use this constructor when registering the receiver programmatically in code
     * and you want to receive listener callbacks for UI updates.
     *
     * @param listener Callback interface for time change notifications, may be null
     */
    public TimeChangeReceiver(TimeChangeListener listener) {

        this.listener = listener;
    }

    // ==================== BROADCAST RECEIVER IMPLEMENTATION ====================

    /**
     * Handles incoming time-related system broadcasts.
     *
     * <p>This method is called by the Android system when any of the registered
     * time-related intents are broadcast. It dispatches to appropriate handler
     * methods based on the action type.
     *
     * @param context Application context for accessing system services
     * @param intent  Intent containing the broadcast action and any extras
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (LOG_ENABLED) {
            Log.d( TAG, "Received broadcast: " + action );
        }

        switch (action) {
            case Intent.ACTION_TIME_CHANGED:
                handleTimeChanged( context );
                break;
            case Intent.ACTION_DATE_CHANGED:
                handleDateChanged( context );
                break;
            case Intent.ACTION_TIMEZONE_CHANGED:
                handleTimezoneChanged( context );
                break;
            case Intent.ACTION_TIME_TICK:
                // Called every minute - used for periodic checks
                handleTimeTick( context );
                break;
        }
    }

    // ==================== TIME CHANGE HANDLERS ====================

    /**
     * Handles system time changes (manual time adjustments).
     *
     * <p>This method is called when the user manually changes the system time.
     * It triggers a full QuattroDue refresh and notifies any registered listener.
     *
     * @param context Application context for system access
     */
    private void handleTimeChanged(Context context) {

        performUpdates( context );
    }

    /**
     * Handles system date changes (date rollover or manual date changes).
     *
     * <p>This method is called when the system date changes, either through natural
     * day rollover at midnight or manual date adjustment by the user.
     * It triggers a full QuattroDue refresh and notifies any registered listener.
     *
     * @param context Application context for system access
     */
    private void handleDateChanged(Context context) {

        performUpdates( context );
    }

    /**
     * Handles timezone changes (manual or automatic timezone updates).
     *
     * <p>This method is called when the system timezone changes, either through
     * manual user settings or automatic timezone detection.
     * It triggers a full QuattroDue refresh and notifies any registered listener.
     *
     * @param context Application context for system access
     */
    private void handleTimezoneChanged(Context context) {

        performUpdates( context );
    }

    /**
     * Handles periodic time tick events (called every minute).
     *
     * <p>This method provides a lightweight way to detect day changes without
     * constantly polling the system time. It's less invasive than checking
     * every second and is sufficient for shift scheduling purposes.
     *
     * <p>Note: This method only sets a refresh flag rather than performing
     * a full update, as minute ticks are frequent and most don't require
     * any action.
     *
     * @param context Application context for system access
     */
    private void handleTimeTick(Context context) {

        // Check if the day has changed
        // This check is less invasive than checking every second
        LocalDate today = LocalDate.now();

        // Compare to saved date
        // Should save a specific Date to check for special updates..
        if (this.savedDate!=null && !this.savedDate.equals( today )) {
            this.savedDateFlag = true;
        }
    }

    // ==================== QUATTRODUE UPDATE METHODS ====================

    /**
     * Updates data after a time change event.
     *
     * <p>This method performs a complete refresh
     * to ensure all displayed data remains accurate after time changes.
     * It includes null check on listener to prevent crashes.
     *
     * @param context Application context for convenience
     */
    private void performUpdates(@NonNull Context context) {

        // Notify listener if available
        if (listener != null) {
            listener.onTimeChanged();
        }

        if (LOG_ENABLED) {
            Log.d( TAG, "Updates performed after time change" );
        }
    }

    // ==================== INTENT FILTER FACTORY METHODS ====================

    /**
     * Creates an IntentFilter for registering all time-related events.
     *
     * <p>This filter includes all time events including the minute tick (TIME_TICK).
     * Use this when you need comprehensive time monitoring, but be aware that
     * TIME_TICK events occur every minute and may impact battery life.
     *
     * <p>Included Actions:
     * - {@link Intent#ACTION_TIME_CHANGED}
     * - {@link Intent#ACTION_DATE_CHANGED}
     * - {@link Intent#ACTION_TIMEZONE_CHANGED}
     * - {@link Intent#ACTION_TIME_TICK}
     *
     * @return IntentFilter configured for all time events
     * @see #createOnlyCriticalIntentFilter() for a more battery-friendly alternative
     */
    public static IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( Intent.ACTION_TIME_CHANGED );
        filter.addAction( Intent.ACTION_DATE_CHANGED );
        filter.addAction( Intent.ACTION_TIMEZONE_CHANGED );
        filter.addAction( Intent.ACTION_TIME_TICK );
        return filter;
    }

    /**
     * Creates an IntentFilter for critical time events only (excludes TIME_TICK).
     *
     * <p>This filter excludes the minute tick events, making it more battery-friendly
     * while still catching all important time changes. Recommended for most use cases
     * unless you specifically need minute-by-minute updates.
     *
     * <p>Included Actions:
     * - {@link Intent#ACTION_TIME_CHANGED}
     * - {@link Intent#ACTION_DATE_CHANGED}
     * - {@link Intent#ACTION_TIMEZONE_CHANGED}
     *
     * @return IntentFilter configured for critical time events only
     * @see #createIntentFilter() for comprehensive monitoring including minute ticks
     */
    public static IntentFilter createOnlyCriticalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( Intent.ACTION_TIME_CHANGED );
        filter.addAction( Intent.ACTION_DATE_CHANGED );
        filter.addAction( Intent.ACTION_TIMEZONE_CHANGED );
        return filter;
    }

    // ==================== FLAG ====================

    /**
     * Set a date to check for future updates
     *
     * @param savedDate date to compare for special operations
     */
    public void setSavedDate(@NonNull LocalDate savedDate) {
        this.savedDate = Objects.requireNonNull( savedDate, "savedDate cannot be null" );
    }

    /**
     * Check the flag
     *
     * @return true if the savedDate is reached, false otherwise
     */
    public boolean getFlag() {
        return this.savedDateFlag;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Sets or updates the time change listener.
     *
     * <p>This method allows changing the listener after the receiver has been created.
     * Useful when the listener lifecycle differs from the receiver lifecycle.
     *
     * @param listener New listener for time change notifications, may be null to remove listener
     */
    public void setTimeChangeListener(TimeChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Gets the current time change listener.
     *
     * @return Current listener, or null if no listener is set
     */
    public TimeChangeListener getTimeChangeListener() {
        return listener;
    }

    /**
     * Checks if a time change listener is currently set.
     *
     * @return true if a listener is set, false otherwise
     */
    public boolean hasTimeChangeListener() {
        return listener != null;
    }
}