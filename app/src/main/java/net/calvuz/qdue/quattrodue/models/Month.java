package net.calvuz.qdue.quattrodue.models;

import static net.calvuz.qdue.quattrodue.Costants.QD_LOG_ENABLED;

import net.calvuz.qdue.quattrodue.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a calendar month with its days and scheduled plant stops.
 * <p>
 * Manages a collection of days and applies predefined stop schedules.
 * Contains static stop data that should ideally be loaded from external source.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public class Month {

    private static final String TAG = Month.class.getSimpleName();

    // Month properties
    private final LocalDate firstDayOfMonth;
    private boolean isCurrent;
    private List<Day> daysList;

    /**
     * Static list of scheduled plant stops.
     * NOTE: In an ideal implementation, this data should be loaded
     * from a database or configuration file.
     */
    private static final List<Stop> STOPS = new ArrayList<>();

    // Initialize static stop data
    static {
        // 2018
        STOPS.add(new Stop(2018, 8, 11, 3, 2018, 8, 20, 1));
        STOPS.add(new Stop(2018, 12, 21, 3, 2018, 12, 32, 1));
        // 2019
        STOPS.add(new Stop(2019, 1, 1, 1, 2019, 1, 3, 1));
        STOPS.add(new Stop(2019, 4, 19, 3, 2019, 4, 23, 1));
        STOPS.add(new Stop(2019, 6, 20, 3, 2019, 6, 25, 1));
        STOPS.add(new Stop(2019, 8, 13, 3, 2019, 8, 21, 1));
        STOPS.add(new Stop(2019, 12, 24, 3, 2019, 12, 27, 1));
        STOPS.add(new Stop(2019, 12, 31, 3, 2019, 12, 32, 1));
        // Add more years as needed
    }

    /**
     * Creates a month starting from the given date.
     * The date is always set to the first day of the month.
     *
     * @param date Reference date for the month
     */
    public Month(LocalDate date) {
        // Always set to first day of month
        this.firstDayOfMonth = date.withDayOfMonth(1);
        this.daysList = new ArrayList<>();

        int daysInMonth = YearMonth.of(date.getYear(), date.getMonth()).lengthOfMonth();

        if (QD_LOG_ENABLED) {
            Log.d(TAG, firstDayOfMonth + " (days in month: " + daysInMonth + ")");
        }

        // Check if this is the current month
        LocalDate today = LocalDate.now();
        setIsCurrent(today.getYear() == date.getYear() &&
                today.getMonth() == date.getMonth());
    }

    /**
     * @return true if this is the current month
     */
    public boolean isCurrent() {
        return isCurrent;
    }

    /**
     * Sets whether this is the current month.
     *
     * @param isCurrent true if this is the current month
     */
    private void setIsCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;

        if (isCurrent && daysList != null && !daysList.isEmpty()) {
            // If current month, set today
            LocalDate today = LocalDate.now();
            setToday(today.getDayOfMonth());

            if (QD_LOG_ENABLED) {
                Log.d(TAG, "setIsCurrent: " + today.getDayOfMonth());
            }
        }
    }

    /**
     * Sets the current day in the month.
     *
     * @param dayOfMonth Day of month (1-31)
     */
    public void setToday(int dayOfMonth) {
        if (daysList != null && dayOfMonth > 0 && dayOfMonth <= daysList.size()) {
            daysList.get(dayOfMonth - 1).setIsToday(true);

            if (QD_LOG_ENABLED) {
                Log.d(TAG, "setToday: " + dayOfMonth);
            }
        }
    }

    /**
     * @return Immutable list of days in the month
     */
    public List<Day> getDaysList() {
        return daysList != null ? Collections.unmodifiableList(daysList) : new ArrayList<>();
    }

    /**
     * Sets the list of days for this month.
     *
     * @param daysList List of days
     */
    public void setDaysList(List<Day> daysList) {
        this.daysList = new ArrayList<>(daysList);

        if (QD_LOG_ENABLED) {
            Log.d(TAG, "setDaysList: days list set");
        }

        // If current month, set today
        if (isCurrent()) {
            setToday(LocalDate.now().getDayOfMonth());
        }
    }

    /**
     * Configures stops for this month.
     * Loads predefined stops and applies them to month days.
     */
    public void setStops() {
        processStops();
    }

    /**
     * Processes scheduled stops for this month.
     * Retrieves relevant stops and applies them to days.
     */
    private void processStops() {
        // Get stops for current month
        List<Stop> monthStops = getStopsForMonth(firstDayOfMonth.getYear(),
                firstDayOfMonth.getMonthValue());

        for (Stop stop : monthStops) {
            try {
                applyStop(stop);
            } catch (Exception e) {
                Log.e(TAG, "Error applying stop: " + e.getMessage());
            }
        }
    }

    /**
     * Applies a stop to the days of the month.
     *
     * @param stop Stop to apply
     */
    private void applyStop(Stop stop) {
        // Count shifts
        int shiftCounter = stop.shift;
        // Count days
        int dayCounter = stop.day;

        // Continue until we reach end day or end of month
        while (dayCounter < stop.endday && dayCounter <= daysList.size()) {
            // Get the day
            Day day = daysList.get(dayCounter - 1);

            if (day != null) {
                // Set shifts as stops
                while (shiftCounter < 4) {
                    day.setStop(shiftCounter);
                    shiftCounter++;
                }

                // Reset shift counter for next day
                shiftCounter = 1;
            }

            dayCounter++;
        }
    }

    /**
     * Generates and initializes the list of days in the month.
     * This method can be called after creating the Month object
     * to populate the days list.
     */
    public void generateDays() {
        int daysInMonth = YearMonth.of(firstDayOfMonth.getYear(), firstDayOfMonth.getMonth()).lengthOfMonth();
        daysList = new ArrayList<>(daysInMonth);

        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = LocalDate.of(firstDayOfMonth.getYear(), firstDayOfMonth.getMonth(), i);
            daysList.add(new Day(date));
        }

        // If current month, set today
        if (isCurrent()) {
            setToday(LocalDate.now().getDayOfMonth());
        }
    }

    /**
     * Retrieves scheduled stops for a specific month.
     *
     * @param year  Year
     * @param month Month (1-12)
     * @return List of stops for the specified month
     */
    private List<Stop> getStopsForMonth(int year, int month) {
        // Filter stops for specified year and month
        return STOPS.stream()
                .filter(stop -> stop.year == year && stop.month == month)
                .collect(Collectors.toList());
    }

    /**
     * @return First day of the month
     */
    public LocalDate getFirstDayOfMonth() {
        return firstDayOfMonth;
    }

    /**
     * @return Year of the month
     */
    public int getYear() {
        return firstDayOfMonth.getYear();
    }

    /**
     * @return Month number (1-12)
     */
    public int getMonthValue() {
        return firstDayOfMonth.getMonthValue();
    }

    /**
     * @return Month name
     */
    public String getMonthName() {
        return firstDayOfMonth.getMonth().toString();
    }

    /**
     * @return Text representation of the month (e.g., "May 2025")
     */
    public String getTitle() {
        return firstDayOfMonth.getMonth().toString() + " " + firstDayOfMonth.getYear();
    }

    @Override
    public String toString() {
        return TAG + "{" + firstDayOfMonth + ", days: " +
                (daysList != null ? daysList.size() : 0) + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Month other = (Month) obj;
        return Objects.equals(firstDayOfMonth, other.firstDayOfMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstDayOfMonth);
    }
}