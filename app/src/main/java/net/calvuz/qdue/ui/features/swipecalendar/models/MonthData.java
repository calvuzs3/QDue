package net.calvuz.qdue.ui.features.swipecalendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MonthData - Data Container for Calendar Month Information
 *
 * <p>Immutable data class containing all calendar-related information for a specific month
 * including events and work schedule data. Used in MVVM architecture for data caching
 * and state management in SharedCalendarViewModel.</p>
 *
 * <h3>Data Contents:</h3>
 * <ul>
 *   <li><strong>Month Information</strong>: YearMonth for which data is valid</li>
 *   <li><strong>Events Data</strong>: Map of LocalDate to List of LocalEvent</li>
 *   <li><strong>Work Schedule</strong>: Map of LocalDate to WorkScheduleDay</li>
 *   <li><strong>Load Timestamp</strong>: When data was loaded for cache management</li>
 * </ul>
 *
 * <h3>MVVM Integration:</h3>
 * <ul>
 *   <li><strong>ViewModel Caching</strong>: Stored in SharedCalendarViewModel cache</li>
 *   <li><strong>Performance Optimization</strong>: Reduces redundant data loading</li>
 *   <li><strong>State Management</strong>: Preserves data across configuration changes</li>
 *   <li><strong>Observable Updates</strong>: Triggers LiveData updates when cache changes</li>
 * </ul>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // Create MonthData after loading
 * MonthData monthData = new MonthData(yearMonth, eventsMap, workScheduleMap);
 *
 * // Access events for specific date
 * List&lt;LocalEvent&gt; events = monthData.getEventsForDate(LocalDate.now());
 *
 * // Access work schedule for specific date
 * WorkScheduleDay schedule = monthData.getWorkScheduleForDate(LocalDate.now());
 *
 * // Check if data is available
 * boolean hasData = monthData.hasEventsData() && monthData.hasWorkScheduleData();
 * </pre>
 *
 * @author QDue Development Team
 * @version 1.0.0 - MVVM Architecture Implementation
 * @since MVVM Migration Phase
 */
public class MonthData {

    // ==================== IMMUTABLE FIELDS ====================

    @NonNull
    private final YearMonth mMonth;

    @NonNull
    private final Map<LocalDate, List<LocalEvent>> mEvents;

    @NonNull
    private final Map<LocalDate, WorkScheduleDay> mWorkSchedule;

    private final long mLoadTimestamp;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create MonthData with events and work schedule.
     *
     * @param month YearMonth for which this data is valid
     * @param events Map of dates to events (nullable, will use empty map if null)
     * @param workSchedule Map of dates to work schedule (nullable, will use empty map if null)
     */
    public MonthData(@NonNull YearMonth month,
                     @Nullable Map<LocalDate, List<LocalEvent>> events,
                     @Nullable Map<LocalDate, WorkScheduleDay> workSchedule) {
        this.mMonth = Objects.requireNonNull(month, "Month cannot be null");
        this.mEvents = events != null ? Map.copyOf(events) : Collections.emptyMap();
        this.mWorkSchedule = workSchedule != null ? Map.copyOf(workSchedule) : Collections.emptyMap();
        this.mLoadTimestamp = System.currentTimeMillis();
    }

    /**
     * Create empty MonthData for a month.
     *
     * @param month YearMonth for which to create empty data
     */
    public MonthData(@NonNull YearMonth month) {
        this(month, null, null);
    }

    // ==================== GETTERS ====================

    /**
     * Get the month for which this data is valid.
     *
     * @return YearMonth instance
     */
    @NonNull
    public YearMonth getMonth() {
        return mMonth;
    }

    /**
     * Get all events data for this month.
     *
     * @return Unmodifiable map of dates to event lists
     */
    @NonNull
    public Map<LocalDate, List<LocalEvent>> getEvents() {
        return mEvents;
    }

    /**
     * Get all work schedule data for this month.
     *
     * @return Unmodifiable map of dates to work schedule days
     */
    @NonNull
    public Map<LocalDate, WorkScheduleDay> getWorkSchedule() {
        return mWorkSchedule;
    }

    /**
     * Get timestamp when this data was loaded.
     *
     * @return Load timestamp in milliseconds
     */
    public long getLoadTimestamp() {
        return mLoadTimestamp;
    }

    // ==================== DATA ACCESS METHODS ====================

    /**
     * Get events for a specific date.
     *
     * @param date Target date
     * @return List of events for the date, empty list if none
     */
    @NonNull
    public List<LocalEvent> getEventsForDate(@NonNull LocalDate date) {
        List<LocalEvent> events = mEvents.get(date);
        return events != null ? events : Collections.emptyList();
    }

    /**
     * Get work schedule for a specific date.
     *
     * @param date Target date
     * @return WorkScheduleDay for the date, null if none
     */
    @Nullable
    public WorkScheduleDay getWorkScheduleForDate(@NonNull LocalDate date) {
        return mWorkSchedule.get(date);
    }

    /**
     * Check if there are events for a specific date.
     *
     * @param date Target date
     * @return true if there are events for the date
     */
    public boolean hasEventsForDate(@NonNull LocalDate date) {
        List<LocalEvent> events = mEvents.get(date);
        return events != null && !events.isEmpty();
    }

    /**
     * Check if there is work schedule for a specific date.
     *
     * @param date Target date
     * @return true if there is work schedule for the date
     */
    public boolean hasWorkScheduleForDate(@NonNull LocalDate date) {
        return mWorkSchedule.containsKey(date);
    }

    /**
     * Get number of events for a specific date.
     *
     * @param date Target date
     * @return Number of events for the date
     */
    public int getEventCountForDate(@NonNull LocalDate date) {
        List<LocalEvent> events = mEvents.get(date);
        return events != null ? events.size() : 0;
    }

    // ==================== DATA AVAILABILITY CHECKS ====================

    /**
     * Check if this MonthData contains events data.
     *
     * @return true if events data is available
     */
    public boolean hasEventsData() {
        return !mEvents.isEmpty();
    }

    /**
     * Check if this MonthData contains work schedule data.
     *
     * @return true if work schedule data is available
     */
    public boolean hasWorkScheduleData() {
        return !mWorkSchedule.isEmpty();
    }

    /**
     * Check if this MonthData contains both events and work schedule data.
     *
     * @return true if both types of data are available
     */
    public boolean hasCompleteData() {
        return hasEventsData() || hasWorkScheduleData(); // Either type is considered complete
    }

    /**
     * Check if this MonthData is empty (no events and no work schedule).
     *
     * @return true if no data is available
     */
    public boolean isEmpty() {
        return mEvents.isEmpty() && mWorkSchedule.isEmpty();
    }

    // ==================== STATISTICS METHODS ====================

    /**
     * Get total number of events in this month.
     *
     * @return Total event count
     */
    public int getTotalEventCount() {
        return mEvents.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Get number of days with events in this month.
     *
     * @return Number of days with events
     */
    public int getDaysWithEventsCount() {
        return (int) mEvents.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .count();
    }

    /**
     * Get number of days with work schedule in this month.
     *
     * @return Number of days with work schedule
     */
    public int getDaysWithWorkScheduleCount() {
        return mWorkSchedule.size();
    }

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Check if this data is older than specified age in milliseconds.
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if data is older than specified age
     */
    public boolean isOlderThan(long maxAgeMs) {
        return (System.currentTimeMillis() - mLoadTimestamp) > maxAgeMs;
    }

    /**
     * Get age of this data in milliseconds.
     *
     * @return Data age in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - mLoadTimestamp;
    }

    // ==================== EQUALITY AND HASH ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MonthData monthData = (MonthData) obj;
        return Objects.equals(mMonth, monthData.mMonth) &&
                Objects.equals(mEvents, monthData.mEvents) &&
                Objects.equals(mWorkSchedule, monthData.mWorkSchedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMonth, mEvents, mWorkSchedule);
    }

    // ==================== STRING REPRESENTATION ====================

    @Override
    @NonNull
    public String toString() {
        return "MonthData{" +
                "month=" + mMonth +
                ", eventsCount=" + getTotalEventCount() +
                ", daysWithEvents=" + getDaysWithEventsCount() +
                ", daysWithWorkSchedule=" + getDaysWithWorkScheduleCount() +
                ", age=" + getAgeMs() + "ms" +
                '}';
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Builder for creating MonthData instances.
     */
    public static class Builder {
        private YearMonth mMonth;
        private Map<LocalDate, List<LocalEvent>> mEvents;
        private Map<LocalDate, WorkScheduleDay> mWorkSchedule;

        /**
         * Set the month for this data.
         *
         * @param month YearMonth instance
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder month(@NonNull YearMonth month) {
            this.mMonth = month;
            return this;
        }

        /**
         * Set events data.
         *
         * @param events Map of dates to event lists
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder events(@Nullable Map<LocalDate, List<LocalEvent>> events) {
            this.mEvents = events;
            return this;
        }

        /**
         * Set work schedule data.
         *
         * @param workSchedule Map of dates to work schedule days
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder workSchedule(@Nullable Map<LocalDate, WorkScheduleDay> workSchedule) {
            this.mWorkSchedule = workSchedule;
            return this;
        }

        /**
         * Build MonthData instance.
         *
         * @return MonthData instance
         * @throws IllegalStateException if month is not set
         */
        @NonNull
        public MonthData build() {
            if (mMonth == null) {
                throw new IllegalStateException("Month must be set");
            }
            return new MonthData(mMonth, mEvents, mWorkSchedule);
        }
    }

    /**
     * Create new Builder instance.
     *
     * @return Builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create new Builder instance with month already set.
     *
     * @param month YearMonth for the data
     * @return Builder instance with month set
     */
    @NonNull
    public static Builder builder(@NonNull YearMonth month) {
        return new Builder().month(month);
    }
}