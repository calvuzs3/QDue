package net.calvuz.qdue.ui.features.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * CalendarDay - Data model representing a single day in the calendar.
 *
 * <p>Combines work schedule information with events to provide a unified
 * view of what happens on a specific day. Includes visual information
 * for UI rendering such as colors and indicators.</p>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarDay {

    private final LocalDate date;
    private final boolean isToday;
    private final boolean isCurrentMonth;
    private final boolean isWeekend;

    // Work schedule information
    private final List<WorkScheduleEvent> workScheduleEvents;
    private final ShiftTypeEntity effectiveShift; // Effective shift after applying events
    private final boolean hasShiftModification;

    // Events information
    private final List<LocalEvent> events;
    private final int eventCount;
    private final boolean hasHighPriorityEvents;

    // Visual properties
    private final int backgroundColor;
    private final int textColor;
    private final boolean hasIndicators;

    // Loading state
    private final boolean isLoading;
    private final boolean hasError;

    // ==================== CONSTRUCTOR ====================

    private CalendarDay(@NonNull Builder builder) {
        this.date = builder.date;
        this.isToday = LocalDate.now().equals(builder.date);
        this.isCurrentMonth = builder.isCurrentMonth;
        this.isWeekend = isWeekend(builder.date);

        this.workScheduleEvents = Collections.unmodifiableList(builder.workScheduleEvents);
        this.effectiveShift = builder.effectiveShift;
        this.hasShiftModification = builder.hasShiftModification;

        this.events = Collections.unmodifiableList(builder.events);
        this.eventCount = builder.events.size();
        this.hasHighPriorityEvents = builder.hasHighPriorityEvents;

        this.backgroundColor = builder.backgroundColor;
        this.textColor = builder.textColor;
        this.hasIndicators = builder.hasIndicators;

        this.isLoading = builder.isLoading;
        this.hasError = builder.hasError;
    }

    // ==================== GETTERS ====================

    @NonNull
    public LocalDate getDate() { return date; }

    public boolean isToday() { return isToday; }

    public boolean isCurrentMonth() { return isCurrentMonth; }

    public boolean isWeekend() { return isWeekend; }

    @NonNull
    public List<WorkScheduleEvent> getWorkScheduleEvents() { return workScheduleEvents; }

    @Nullable
    public ShiftTypeEntity getEffectiveShift() { return effectiveShift; }

    public boolean hasShiftModification() { return hasShiftModification; }

    @NonNull
    public List<LocalEvent> getEvents() { return events; }

    public int getEventCount() { return eventCount; }

    public boolean hasHighPriorityEvents() { return hasHighPriorityEvents; }

    public int getBackgroundColor() { return backgroundColor; }

    public int getTextColor() { return textColor; }

    public boolean hasIndicators() { return hasIndicators; }

    public boolean isLoading() { return isLoading; }

    public boolean hasError() { return hasError; }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this day has any work schedule.
     */
    public boolean hasWorkSchedule() {
        return effectiveShift != null || !workScheduleEvents.isEmpty();
    }

    /**
     * Check if this day has any events.
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * Get display text for the day (day of month).
     */
    public String getDisplayText() {
        return String.valueOf(date.getDayOfMonth());
    }

    /**
     * Check if day is weekend.
     */
    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; // Saturday = 6, Sunday = 7
    }

    // ==================== BUILDER PATTERN ====================

    public static class Builder {
        private LocalDate date;
        private boolean isCurrentMonth = true;
        private List<WorkScheduleEvent> workScheduleEvents = new ArrayList<>();
        private ShiftTypeEntity effectiveShift;
        private boolean hasShiftModification = false;
        private List<LocalEvent> events = new ArrayList<>();
        private boolean hasHighPriorityEvents = false;
        private int backgroundColor = 0;
        private int textColor = 0;
        private boolean hasIndicators = false;
        private boolean isLoading = false;
        private boolean hasError = false;

        public Builder(@NonNull LocalDate date) {
            this.date = date;
        }

        public Builder isCurrentMonth(boolean isCurrentMonth) {
            this.isCurrentMonth = isCurrentMonth;
            return this;
        }

        public Builder workScheduleEvents(@NonNull List<WorkScheduleEvent> events) {
            this.workScheduleEvents = new ArrayList<>(events);
            return this;
        }

        public Builder effectiveShift(@Nullable ShiftTypeEntity shift) {
            this.effectiveShift = shift;
            return this;
        }

        public Builder hasShiftModification(boolean hasModification) {
            this.hasShiftModification = hasModification;
            return this;
        }

        public Builder events(@NonNull List<LocalEvent> events) {
            this.events = new ArrayList<>(events);
            return this;
        }

        public Builder hasHighPriorityEvents(boolean hasHighPriority) {
            this.hasHighPriorityEvents = hasHighPriority;
            return this;
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder hasIndicators(boolean hasIndicators) {
            this.hasIndicators = hasIndicators;
            return this;
        }

        public Builder isLoading(boolean isLoading) {
            this.isLoading = isLoading;
            return this;
        }

        public Builder hasError(boolean hasError) {
            this.hasError = hasError;
            return this;
        }

        public CalendarDay build() {
            return new CalendarDay(this);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarDay that = (CalendarDay) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @NonNull
    @Override
    public String toString() {
        return "CalendarDay{" +
                "date=" + date +
                ", eventCount=" + eventCount +
                ", hasWorkSchedule=" + hasWorkSchedule() +
                ", isToday=" + isToday +
                '}';
    }
}

