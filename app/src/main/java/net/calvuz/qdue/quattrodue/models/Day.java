package net.calvuz.qdue.quattrodue.models;

import static net.calvuz.qdue.quattrodue.QuattroDue.HALFTEAM_ALL;

import androidx.annotation.NonNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.utils.Log;

/**
 * Represents a calendar day with its work shifts.
 * <p>
 * Each day contains multiple shifts and tracks teams that are off work.
 * Supports cloning for template-based generation.
 *
 * @author Luke (original)
 * @author Updated 21/05/2025
 */
public class Day implements Cloneable {

    public final static String TAG = Day.class.getSimpleName();

    // Logging configuration
    private final static boolean LOG_ENABLED = Costants.QD_LOG_ENABLED;
    private final static boolean LOG_SHIFTS = LOG_ENABLED;
    private final static boolean LOG_STOPS = LOG_ENABLED;

    // Day of week formatter
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");

    public LocalDate getLocalDate() {
        return localDate;
    }

    // Core data
    private LocalDate localDate;  // Not final to allow modification
    private boolean isToday;
    private List<Shift> shifts;
    private List<HalfTeam> offWorkHalfTeams;

    /**
     * Creates a new day with the specified date.
     *
     * @param date The date of the day
     */
    public Day(LocalDate date) {
        if (LOG_ENABLED) Log.v(TAG, "New Day {" + date + "}");

        this.localDate = date;
        this.shifts = new ArrayList<>();

        // Set isToday if it's today
        LocalDate today = LocalDate.now();
        this.isToday = date.equals(today);

        if (LOG_ENABLED) Log.d(TAG, date + " - " + this.localDate);

        // Initialize off-work teams with all available teams
        try {
            this.offWorkHalfTeams = new ArrayList<>(HALFTEAM_ALL);
        } catch (Exception e) {
            Log.e(TAG, "HALFTEAM.clone() : " + e.getMessage());
        }
    }

    /**
     * Finds the shift index where a specific team is working.
     *
     * @param halfTeam The team to search for
     * @return The shift index or -1 if team is not working
     */
    public int getInWichTeamIsHalfTeam(HalfTeam halfTeam) {
        if (shifts == null || shifts.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < shifts.size(); i++) {
            for (HalfTeam ht : shifts.get(i).getHalfTeams()) {
                if (ht.isSameTeamAs(halfTeam)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Returns all shifts of the day.
     *
     * @return Immutable list of shifts
     */
    public List<Shift> getShifts() {
        return shifts != null ? Collections.unmodifiableList(shifts) : new ArrayList<>();
    }

    /**
     * Sets the shifts for this day by cloning each provided shift.
     *
     * @param shifts The shifts to copy
     */
    private void setShifts(@NonNull List<Shift> shifts) {
        this.shifts = new ArrayList<>();

        for (Shift shift : shifts) {
            try {
                addShift(shift.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "setShifts: " + e.getMessage());
            }
        }
    }

    /**
     * Adds a new shift to the day.
     *
     * @param shift The shift to add
     */
    public void addShift(Shift shift) {
        if (shifts == null) {
            shifts = new ArrayList<>();
        }

        shifts.add(shift);
        if (LOG_SHIFTS) Log.d(TAG, "addShift: " + shift);

        // Remove shift teams from off-work teams list
        if (offWorkHalfTeams != null && !offWorkHalfTeams.isEmpty()) {
            Iterator<HalfTeam> itr = offWorkHalfTeams.iterator();

            while (itr.hasNext()) {
                HalfTeam offWorkTeam = itr.next();

                for (HalfTeam shiftTeam : shift.getHalfTeams()) {
                    if (shiftTeam.isSameTeamAs(offWorkTeam)) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
    }

    public List<HalfTeam> getOffWorkHalfTeams() {
        return offWorkHalfTeams;
    }

    /**
     * Sets off-work teams by cloning each provided team.
     *
     * @param offWorkHalfTeams List of off-work teams
     */
    private void setOffWorkHalfTeams(@NonNull List<HalfTeam> offWorkHalfTeams) {
        this.offWorkHalfTeams = new ArrayList<>();

        for (HalfTeam team : offWorkHalfTeams) {
            try {
                addOffWorkHalfTeam(team.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "setOffWorkHalfTeam: " + e.getMessage());
            }
        }
    }

    /**
     * Adds a team to the off-work teams list.
     *
     * @param halfTeam The team to add
     */
    private void addOffWorkHalfTeam(HalfTeam halfTeam) {
        if (this.offWorkHalfTeams == null) {
            this.offWorkHalfTeams = new ArrayList<>();
        }
        this.offWorkHalfTeams.add(halfTeam);
    }

    /**
     * @return Day of month (1-31)
     */
    public int getDayOfMonth() {
        return localDate.getDayOfMonth();
    }

    /**
     * @return Day of week (1-7, where 1 is Monday and 7 is Sunday)
     */
    public int getDayOfWeek() {
        return localDate.getDayOfWeek().getValue();
    }


    /**
     * Returns the localized day of week name using QDue's configured locale.
     *
     * @return Localized day of week name in the app's configured locale
     */
    public String getDayOfWeekAsString() {
        try {
            // Use QDue's configured locale instead of system default
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE", QDue.getLocale());
            return localDate.format(dayFormatter);
        } catch (Exception e) {
            // Fallback to system locale if QDue locale is not available
            Log.w(TAG, "Failed to use QDue locale for day formatting, falling back to system locale: " + e.getMessage());
            DateTimeFormatter fallbackFormatter = DateTimeFormatter.ofPattern("EEEE");
            return localDate.format(fallbackFormatter);
        }
    }

    /**
     * @return String with all off-work team names
     */
    public String getOffWorkHalfTeamsAsString() {
        if (offWorkHalfTeams == null || offWorkHalfTeams.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (HalfTeam halfTeam : offWorkHalfTeams) {
            result.append(halfTeam.getName());
        }
        return result.toString();
    }

    /**
     * Returns teams for a specific shift as string.
     *
     * @param position The shift index
     * @return String with team names for the shift
     */
    public String getTeamsAsString(int position) {
        if (shifts == null || shifts.size() <= position) {
            return "";
        }

        return shifts.get(position).getTeamsAsString();
    }

    /**
     * @return true if this day is today
     */
    public boolean getIsToday() {
        return this.isToday;
    }

    /**
     * Sets whether this day is today.
     *
     * @param isToday true if this is today
     */
    public void setIsToday(boolean isToday) {
        this.isToday = isToday;
    }

    /**
     * Sets the date of this day.
     *
     * @param date The new date
     */
    public void setLocalDate(@NonNull LocalDate date) {
        this.localDate = date;
        if (LOG_ENABLED) Log.d(TAG, "setLocalDate: " + date);
    }

    /**
     * @return The date of this day
     */
    public LocalDate getDate() {
        return this.localDate;
    }

    /**
     * @return true if day has events (currently always false)
     */
    public boolean hasEvents() {
        return false;
    }

    /**
     * Marks a shift as a plant stop.
     *
     * @param shiftIndex The shift index (1-based)
     */
    public void setStop(int shiftIndex) {
        if (shifts != null && shifts.size() >= shiftIndex && shiftIndex > 0) {
            shifts.get(shiftIndex - 1).setStop(true);
            if (LOG_STOPS) Log.v(TAG, "setStop: " + shiftIndex);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" + this.localDate + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Day other = (Day) obj;
        return Objects.equals(localDate, other.localDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDate);
    }

    @NonNull
    @Override
    public Day clone() throws CloneNotSupportedException {
        Day cloned = (Day) super.clone();

        // Reset isToday to false for clone
        cloned.setIsToday(false);

        // Clone the date
        cloned.localDate = localDate;

        // Clone off-work teams
        if (this.offWorkHalfTeams != null) {
            cloned.setOffWorkHalfTeams(this.offWorkHalfTeams);
        }

        // Clone shifts
        if (this.shifts != null) {
            cloned.setShifts(this.shifts);
        }

        return cloned;
    }
}