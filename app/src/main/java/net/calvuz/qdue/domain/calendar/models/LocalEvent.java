package net.calvuz.qdue.domain.calendar.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.domain.calendar.enums.EventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class LocalEvent
{

    @NonNull
    private String id;
    @NonNull
    private String calendarId;
    @NonNull
    private String title;
    @Nullable
    private String description;
    @NonNull
    private LocalDateTime startTime;
    @NonNull
    private LocalDateTime endTime;
    @NonNull
    private EventType eventType;
    @NonNull
    private Priority priority;
    @NonNull
    Boolean allDay;
    @Nullable
    private String location;
    @Nullable
    private Map<String, String> customProperties;
    @Nullable
    private String sourceUrl;      // Original URL source
    private long createdAt;
    private long updatedAt;

//    private com.google.api.services.calendar.model.Event myEvent;

    // ==================== CONSTRUCTOR ====================

    private LocalEvent(
            Builder builder
    ) {
        this.id = builder.getId();
        this.calendarId = builder.getCalendarId();
        this.title = builder.getTitle();
        this.description = builder.getDescription();
        this.startTime = builder.getStartTime();
        this.endTime = builder.getEndTime();
        this.eventType = builder.getEventType();
        this.priority = builder.getPriority();
        this.allDay = builder.isAllDay();
        this.location = builder.getLocation();
        this.customProperties = builder.getCustomProperties();
        this.sourceUrl = builder.getSourceUrl();
        this.createdAt = builder.getCreatedAt();
        this.updatedAt = builder.getUpdatedAt();
    }

    private LocalEvent(
            @NonNull String id,
            @NonNull String calendarId,
            @NonNull String title,
            @Nullable String description,
            @NonNull LocalDateTime startTime,
            @NonNull LocalDateTime endTime,
            @NonNull EventType eventType,
            @NonNull Priority priority,
            boolean allDay,
            @Nullable String location,
            @Nullable Map<String, String> customProperties,
            @Nullable String sourceUrl,      // Original URL source
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.calendarId = calendarId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventType = eventType;
        this.priority = priority;
        this.allDay = allDay;
        this.location = location;
        this.customProperties = customProperties;
        this.sourceUrl = sourceUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ==================== GETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getCalendarId() {
        return calendarId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @NonNull
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @NonNull
    public EventType getEventType() {
        return eventType;
    }

    @NonNull
    public Priority getPriority() {
        return priority;
    }

    public boolean isAllDay() {
        return allDay;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    @Nullable
    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    @Nullable
    public String getCustomProperty(String key) {
        return customProperties != null ? customProperties.get( key ) : null;
    }

    @Nullable
    public String getSourceUrl() {
        return sourceUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // ==================== SETTERS ====================

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setCalendarId(@NonNull String calendarId) {
        this.calendarId = calendarId;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setStartTime(@NonNull LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(@NonNull LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setEventType(@NonNull EventType eventType) {
        this.eventType = eventType;
    }

    public void setPriority(@NonNull Priority priority) {
        this.priority = priority;
    }

    public void setAllDay(@NonNull Boolean allDay) {
        this.allDay = allDay;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    public void setCustomProperties(@Nullable Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    public void setSourceUrl(@Nullable String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing LocalEvent instances.
     *
     * @return Builder instance
     */
    @NonNull
    public static LocalEvent.Builder builder() {
        return new LocalEvent.Builder();
    }

    @NonNull
    public Builder toBuilder() {
        return new Builder().copyFrom( this );
    }

    /**
     * Builder class for creating Shift instances with localization support.
     */
    public static class Builder
    {
        @NonNull
        private String id;
        @NonNull
        private String calendarId;
        @NonNull
        private String title;
        private String description;
        @NonNull
        private LocalDateTime startTime;
        @NonNull
        private LocalDateTime endTime;
        @NonNull
        private EventType eventType;
        @NonNull
        private Priority priority;
        private boolean allDay;
        private String location;
        private Map<String, String> customProperties;
        private String sourceUrl;      // Original URL source
        private long createdAt;
        private long updatedAt;

        public Builder() {
            this.id = UUID.randomUUID().toString();
            this.calendarId = "qdue";
            this.title = "Untitled";
            this.description = null;
            this.startTime = LocalDateTime.now();
            this.endTime = LocalDateTime.now().plusHours( 1 );
            this.eventType = EventType.GENERAL;
            this.priority = Priority.NORMAL;
            this.allDay = true;
            this.location = null;
            this.customProperties = null;
            this.sourceUrl = null;
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = System.currentTimeMillis();
        }

        /**
         * Copy data from existing LocalEvent.
         */
        @NonNull
        public LocalEvent.Builder copyFrom(@NonNull LocalEvent source) {
            this.id = source.getId();
            this.calendarId = source.getCalendarId();
            this.title = source.getTitle();
            this.description = source.getDescription();
            this.startTime = source.getStartTime();
            this.endTime = source.getEndTime();
            this.eventType = source.getEventType();
            this.priority = source.getPriority();
            this.allDay = source.isAllDay();
            this.location = source.getLocation();
            this.customProperties = source.getCustomProperties();
            this.sourceUrl = source.getSourceUrl();
            this.createdAt = source.getCreatedAt();
            this.updatedAt = source.getUpdatedAt();

            return this;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getCalendarId() {
            return calendarId;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        @NonNull
        public LocalDateTime getStartTime() {
            return startTime;
        }

        @NonNull
        public LocalDateTime getEndTime() {
            return endTime;
        }

        @NonNull
        public EventType getEventType() {
            return eventType;
        }

        @NonNull
        public Priority getPriority() {
            return priority;
        }

        public boolean isAllDay() {
            return allDay;
        }

        public String getLocation() {
            return location;
        }

        public Map<String, String> getCustomProperties() {
            return customProperties;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        @NonNull
        public LocalEvent.Builder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public LocalEvent.Builder calendarId(@NonNull String calendarId) {
            this.calendarId = calendarId;
            return this;
        }

        @NonNull
        public LocalEvent.Builder title(@NonNull String title) {
            this.title = title;
            return this;
        }

        @NonNull
        public LocalEvent.Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public LocalEvent.Builder startTime(@NonNull LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        @NonNull
        public LocalEvent.Builder endTime(@Nullable LocalDateTime endTime) {
            if (endTime == null) {
                this.allDay = true;
                this.endTime = this.startTime.plusHours( 1 );
            } else {
                this.allDay = false;
                this.endTime = endTime;
            }
            return this;
        }

        @NonNull
        public LocalEvent.Builder eventType(@NonNull EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        @NonNull
        public LocalEvent.Builder priority(@NonNull Priority priority) {
            this.priority = priority;
            return this;
        }

        @NonNull
        public LocalEvent.Builder allDay(boolean allDay) {
            this.allDay = allDay;
            return this;
        }

        @NonNull
        public LocalEvent.Builder location(@Nullable String location) {
            this.location = location;
            return this;
        }

        @NonNull
        public LocalEvent.Builder customProperties(@Nullable Map<String, String> customProperties) {
            this.customProperties = customProperties;
            return this;
        }

        @NonNull
        public LocalEvent.Builder sourceUrl(@Nullable String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        @NonNull
        public LocalEvent.Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NonNull
        public LocalEvent.Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @NonNull
        public LocalEvent build() {
            return new LocalEvent( this );
        }
    }
}
