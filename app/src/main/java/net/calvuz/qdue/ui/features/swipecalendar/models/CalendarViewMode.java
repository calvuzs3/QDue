package net.calvuz.qdue.ui.features.swipecalendar.models;

import androidx.annotation.NonNull;

/**
 * CalendarViewMode - Enumeration for Calendar View Modes
 *
 * <p>Defines the available viewing modes for the SwipeCalendar feature
 * following MVVM architecture patterns and state management best practices.</p>
 *
 * <h3>View Modes:</h3>
 * <ul>
 *   <li><strong>MONTH</strong>: Monthly calendar view with month-to-month navigation</li>
 *   <li><strong>DAY</strong>: Daily calendar view with day-to-day navigation</li>
 * </ul>
 *
 * <h3>MVVM Integration:</h3>
 * <ul>
 *   <li><strong>Observable State</strong>: Used in SharedCalendarViewModel LiveData</li>
 *   <li><strong>UI Binding</strong>: Direct binding to Fragment visibility states</li>
 *   <li><strong>Navigation Logic</strong>: Determines active Fragment in SwipeCalendarActivity</li>
 *   <li><strong>State Persistence</strong>: Survives configuration changes via ViewModel</li>
 * </ul>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // In SharedCalendarViewModel
 * private MutableLiveData&lt;CalendarViewMode&gt; mViewMode = new MutableLiveData&lt;&gt;(CalendarViewMode.MONTH);
 *
 * // In SwipeCalendarActivity
 * viewModel.getViewMode().observe(this, mode -&gt; {
 *     switch (mode) {
 *         case MONTH:
 *             showMonthFragment();
 *             break;
 *         case DAY:
 *             showDayFragment();
 *             break;
 *     }
 * });
 *
 * // Toggle between modes
 * viewModel.toggleViewMode();
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - MVVM Architecture Implementation
 * @since MVVM Migration Phase
 */
public enum CalendarViewMode {

    /**
     * Monthly calendar view mode.
     *
     * <p>Displays calendar in month-by-month format with horizontal swipe navigation
     * between months. Shows events and work schedule in monthly grid layout.</p>
     *
     * <h4>Features:</h4>
     * <ul>
     *   <li>Month-to-month ViewPager2 navigation</li>
     *   <li>Monthly grid with date cells</li>
     *   <li>Event and work schedule indicators</li>
     *   <li>Range: January 1900 to December 2100</li>
     * </ul>
     */
    MONTH("month"),

    /**
     * Daily calendar view mode.
     *
     * <p>Displays calendar in day-by-day format with horizontal swipe navigation
     * between days. Shows detailed events and work schedule for single day.</p>
     *
     * <h4>Features:</h4>
     * <ul>
     *   <li>Day-to-day ViewPager2 navigation</li>
     *   <li>Detailed daily view layout</li>
     *   <li>Full event details and timing</li>
     *   <li>Work schedule shift information</li>
     * </ul>
     */
    DAY("day");

    // ==================== ENUM PROPERTIES ====================

    /**
     * String identifier for the view mode.
     * Used for logging, persistence, and debugging.
     */
    @NonNull
    private final String mIdentifier;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor for enum values.
     *
     * @param identifier String identifier for the mode
     */
    CalendarViewMode(@NonNull String identifier) {
        this.mIdentifier = identifier;
    }

    // ==================== GETTERS ====================

    /**
     * Get string identifier for this view mode.
     *
     * @return String identifier (e.g., "month", "day")
     */
    @NonNull
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * Get display name for this view mode.
     * Suitable for UI labels and user-facing text.
     *
     * @return Capitalized display name
     */
    @NonNull
    public String getDisplayName() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this is the MONTH view mode.
     *
     * @return true if this is MONTH mode
     */
    public boolean isMonth() {
        return this == MONTH;
    }

    /**
     * Check if this is the DAY view mode.
     *
     * @return true if this is DAY mode
     */
    public boolean isDay() {
        return this == DAY;
    }

    /**
     * Get the opposite view mode.
     * Useful for toggle operations.
     *
     * @return DAY if this is MONTH, MONTH if this is DAY
     */
    @NonNull
    public CalendarViewMode getOpposite() {
        return this == MONTH ? DAY : MONTH;
    }

    // ==================== STATIC UTILITY METHODS ====================

    /**
     * Parse CalendarViewMode from string identifier.
     *
     * @param identifier String identifier ("month" or "day")
     * @return Corresponding CalendarViewMode or MONTH as default
     */
    @NonNull
    public static CalendarViewMode fromIdentifier(@NonNull String identifier) {
        for (CalendarViewMode mode : values()) {
            if (mode.getIdentifier().equalsIgnoreCase(identifier)) {
                return mode;
            }
        }
        return MONTH; // Default fallback
    }

    /**
     * Parse CalendarViewMode from enum name.
     *
     * @param name Enum name ("MONTH" or "DAY")
     * @return Corresponding CalendarViewMode or MONTH as default
     */
    @NonNull
    public static CalendarViewMode fromName(@NonNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MONTH; // Default fallback
        }
    }

    // ==================== STRING REPRESENTATION ====================

    /**
     * String representation of the view mode.
     *
     * @return String identifier for logging and debugging
     */
    @Override
    @NonNull
    public String toString() {
        return mIdentifier;
    }
}