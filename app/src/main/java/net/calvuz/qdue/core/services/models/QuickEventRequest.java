package net.calvuz.qdue.core.services.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * STEP 1: QuickEventRequest DTO
 * <p>
 * Data Transfer Object for quick event creation requests.
 * Separates UI concerns from business logic by providing a clean
 * interface between UI components and the EventsService.
 * <p>
 * Features:
 * - Builder pattern for easy construction
 * - Validation-ready structure
 * - Immutable after construction
 * - Template metadata support
 * - Custom properties support
 */
public class QuickEventRequest {

    // ==================== CORE PROPERTIES ====================

    private final String templateId;
    private final ToolbarAction sourceAction;
    private final EventType eventType;
    private final LocalDate date;
    private final Long userId;

    // ==================== EVENT PROPERTIES ====================

    private final String displayName;
    private final String description;
    private final EventPriority priority;
    private final boolean allDay;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String location;

    // ==================== METADATA ====================

    private final Map<String, String> customProperties;
    private final long createdAt;

    // ==================== CONSTRUCTOR ====================

    private QuickEventRequest(Builder builder) {
        this.templateId = builder.templateId;
        this.sourceAction = builder.sourceAction;
        this.eventType = builder.eventType;
        this.date = builder.date;
        this.userId = builder.userId;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.priority = builder.priority;
        this.allDay = builder.allDay;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.location = builder.location;
        this.customProperties = new HashMap<>(builder.customProperties);
        this.createdAt = System.currentTimeMillis();
    }

    // ==================== GETTERS ====================

    public String getTemplateId() {
        return templateId;
    }

    public ToolbarAction getSourceAction() {
        return sourceAction;
    }

    public EventType getEventType() {
        return eventType;
    }

    public LocalDate getDate() {
        return date;
    }

    @Nullable
    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public EventPriority getPriority() {
        return priority;
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

    public Map<String, String> getCustomProperties() {
        return new HashMap<>(customProperties);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this is a timed event (has specific start/end times)
     */
    public boolean isTimedEvent() {
        return !allDay && startTime != null && endTime != null;
    }

    /**
     * Get custom property value
     */
    @Nullable
    public String getCustomProperty(String key) {
        return customProperties.get(key);
    }

    /**
     * Check if request has custom property
     */
    public boolean hasCustomProperty(String key) {
        return customProperties.containsKey(key);
    }

    // ==================== BUILDER PATTERN ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String templateId;
        private ToolbarAction sourceAction;
        private EventType eventType;
        private LocalDate date;
        private Long userId;
        private String displayName;
        private String description;
        private EventPriority priority = EventPriority.NORMAL;
        private boolean allDay = true;
        private LocalTime startTime;
        private LocalTime endTime;
        private String location;
        private Map<String, String> customProperties = new HashMap<>();

        public Builder templateId(@NonNull String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder sourceAction(@NonNull ToolbarAction sourceAction) {
            this.sourceAction = sourceAction;
            return this;
        }

        public Builder eventType(@NonNull EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder date(@NonNull LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder userId(@Nullable Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder displayName(@NonNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder priority(@NonNull EventPriority priority) {
            this.priority = priority;
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

        public Builder customProperty(@NonNull String key, @NonNull String value) {
            this.customProperties.put(key, value);
            return this;
        }

        public Builder customProperties(@NonNull Map<String, String> customProperties) {
            this.customProperties.putAll(customProperties);
            return this;
        }

        public QuickEventRequest build() {
            // Basic validation
            if (templateId == null || templateId.trim().isEmpty()) {
                throw new IllegalArgumentException("Template ID is required");
            }
            if (sourceAction == null) {
                throw new IllegalArgumentException("Source action is required");
            }
            if (eventType == null) {
                throw new IllegalArgumentException("Event type is required");
            }
            if (date == null) {
                throw new IllegalArgumentException("Date is required");
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                throw new IllegalArgumentException("Display name is required");
            }

            // Timing validation
            if (!allDay && (startTime == null || endTime == null)) {
                throw new IllegalArgumentException("Start and end times are required for timed events");
            }
            if (!allDay && startTime != null && endTime != null && !startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            return new QuickEventRequest(this);
        }
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return "QuickEventRequest{" +
                "templateId='" + templateId + '\'' +
                ", sourceAction=" + sourceAction +
                ", eventType=" + eventType +
                ", date=" + date +
                ", displayName='" + displayName + '\'' +
                ", allDay=" + allDay +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuickEventRequest that = (QuickEventRequest) o;

        if (allDay != that.allDay) return false;
        if (createdAt != that.createdAt) return false;
        if (templateId != null ? !templateId.equals(that.templateId) : that.templateId != null)
            return false;
        if (sourceAction != that.sourceAction) return false;
        if (eventType != that.eventType) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null)
            return false;
        if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null)
            return false;
        return endTime != null ? endTime.equals(that.endTime) : that.endTime == null;
    }

    @Override
    public int hashCode() {
        int result = templateId != null ? templateId.hashCode() : 0;
        result = 31 * result + (sourceAction != null ? sourceAction.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (allDay ? 1 : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        return result;
    }
}