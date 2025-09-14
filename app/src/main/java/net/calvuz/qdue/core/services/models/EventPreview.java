package net.calvuz.qdue.core.services.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.events.enums.EventPriority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * STEP 1: EventPreview DTO
 * <p>
 * Data Transfer Object for event preview display in UI.
 * Provides a clean interface for showing event preview information
 * without exposing complex business logic or database entities.
 * <p>
 * Features:
 * - Immutable after construction
 * - UI-friendly display methods
 * - Formatted text generation
 * - Builder pattern for easy construction
 * - Validation for UI consistency
 */
public class EventPreview {

    // ==================== CORE PROPERTIES ====================

    private final String title;
    private final String description;
    private final EventType eventType;
    private final EventPriority priority;
    private final LocalDate date;

    // ==================== TIMING PROPERTIES ====================

    private final boolean allDay;
    private final LocalTime startTime;
    private final LocalTime endTime;

    // ==================== DISPLAY PROPERTIES ====================

    private final String location;
    private final String templateId;
    private final String iconResource;
    private final int colorResource;

    // ==================== CONSTRUCTOR ====================

    private EventPreview(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.eventType = builder.eventType;
        this.priority = builder.priority;
        this.date = builder.date;
        this.allDay = builder.allDay;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.location = builder.location;
        this.templateId = builder.templateId;
        this.iconResource = builder.iconResource;
        this.colorResource = builder.colorResource;
    }

    // ==================== GETTERS ====================

    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isAllDay() {
        return allDay;
    }

    @Nullable
    public LocalTime getStartTime() {
        return startTime;
    }

    @Nullable
    public LocalTime getEndTime() {
        return endTime;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public String getTemplateId() {
        return templateId;
    }

    @Nullable
    public String getIconResource() {
        return iconResource;
    }

    public int getColorResource() {
        return colorResource;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Get formatted time string for display
     */
    public String getFormattedTimeString() {
        if (allDay) {
            return "Tutto il giorno";
        }

        if (startTime != null && endTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return startTime.format(formatter) + " - " + endTime.format(formatter);
        }

        return "Orario non specificato";
    }

    /**
     * Get formatted date string for display
     */
    public String getFormattedDateString() {
        if (date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            return date.format(formatter);
        }
        return "Data non specificata";
    }

    /**
     * Get full formatted datetime string
     */
    public String getFormattedDateTimeString() {
        String dateStr = getFormattedDateString();
        String timeStr = getFormattedTimeString();
        return dateStr + " • " + timeStr;
    }

    /**
     * Get event type display name
     */
    public String getEventTypeDisplayName() {
        return eventType != null ? eventType.getDisplayName() : "Generale";
    }

    /**
     * Get priority display name
     */
    public String getPriorityDisplayName() {
        if (priority == null) {
            return "Normale";
        }
        switch (priority) {
            case LOW:
                return "Bassa";
            case HIGH:
                return "Alta";
            case URGENT:
                return "Urgente";
            case NORMAL:
            default:
                return "Normale";
        }
    }

    /**
     * Get summary text for preview
     */
    public String getSummaryText() {
        StringBuilder summary = new StringBuilder();
        summary.append(title);

        if (description != null && !description.trim().isEmpty()) {
            summary.append(" - ").append(description);
        }

        return summary.toString();
    }

    /**
     * Get detailed preview text
     */
    public String getDetailedPreviewText() {
        StringBuilder details = new StringBuilder();

        details.append("📅 ").append(getFormattedDateString()).append("\n");
        details.append("⏰ ").append(getFormattedTimeString()).append("\n");
        details.append("📂 ").append(getEventTypeDisplayName()).append("\n");
        details.append("⭐ ").append(getPriorityDisplayName());

        if (location != null && !location.trim().isEmpty()) {
            details.append("\n📍 ").append(location);
        }

        if (description != null && !description.trim().isEmpty()) {
            details.append("\n📝 ").append(description);
        }

        return details.toString();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this is a timed event
     */
    public boolean isTimedEvent() {
        return !allDay && startTime != null && endTime != null;
    }

    /**
     * Check if event has location
     */
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }

    /**
     * Check if event has description
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Get duration in minutes (for timed events)
     */
    public long getDurationMinutes() {
        if (isTimedEvent()) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }

    /**
     * Get formatted duration string
     */
    public String getFormattedDurationString() {
        if (allDay) {
            return "Tutto il giorno";
        }

        long minutes = getDurationMinutes();
        if (minutes == 0) {
            return "Durata non specificata";
        }

        if (minutes < 60) {
            return minutes + " minuti";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " ore";
            } else {
                return hours + " ore e " + remainingMinutes + " minuti";
            }
        }
    }

    // ==================== BUILDER PATTERN ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private EventType eventType;
        private EventPriority priority = EventPriority.NORMAL;
        private LocalDate date;
        private boolean allDay = true;
        private LocalTime startTime;
        private LocalTime endTime;
        private String location;
        private String templateId;
        private String iconResource;
        private int colorResource = 0;

        public Builder title(@NonNull String title) {
            this.title = title;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder eventType(@NonNull EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder priority(@NonNull EventPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder date(@NonNull LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder allDay(boolean allDay) {
            this.allDay = allDay;
            return this;
        }

        public Builder startTime(@Nullable LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(@Nullable LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder location(@Nullable String location) {
            this.location = location;
            return this;
        }

        public Builder templateId(@NonNull String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder iconResource(@Nullable String iconResource) {
            this.iconResource = iconResource;
            return this;
        }

        public Builder colorResource(int colorResource) {
            this.colorResource = colorResource;
            return this;
        }

        public EventPreview build() {
            // Basic validation
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (eventType == null) {
                throw new IllegalArgumentException("Event type is required");
            }
            if (date == null) {
                throw new IllegalArgumentException("Date is required");
            }
            if (templateId == null || templateId.trim().isEmpty()) {
                throw new IllegalArgumentException("Template ID is required");
            }

            // Timing validation
            if (!allDay && (startTime == null || endTime == null)) {
                throw new IllegalArgumentException("Start and end times are required for timed events");
            }
            if (!allDay && startTime != null && endTime != null && !startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            return new EventPreview(this);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return "EventPreview{" +
                "title='" + title + '\'' +
                ", eventType=" + eventType +
                ", date=" + date +
                ", allDay=" + allDay +
                ", templateId='" + templateId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventPreview that = (EventPreview) o;

        if (allDay != that.allDay) return false;
        if (colorResource != that.colorResource) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (eventType != that.eventType) return false;
        if (priority != that.priority) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null)
            return false;
        if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
        return templateId != null ? templateId.equals(that.templateId) : that.templateId == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (priority != null ? priority.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (allDay ? 1 : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (templateId != null ? templateId.hashCode() : 0);
        result = 31 * result + colorResource;
        return result;
    }
}