package net.calvuz.qdue.domain.events.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.domain.events.enums.EventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class LocalEvent
{

    @NonNull
    private final String id;
    @NonNull
    private final String calendarId;
    @NonNull
    private final String title;
    @Nullable
    private final String description;
    @NonNull
    private final LocalDateTime startTime;
    @NonNull
    final LocalDateTime endTime;
    @NonNull
    final EventType eventType;
    @NonNull
    final Priority priority;
    @NonNull
    Boolean allDay = true;
    @Nullable
    private final String location;
    @Nullable
    private final Map<String, String> customProperties;
    @Nullable
    private final String sourceUrl;      // Original URL source
    private final long createdAt;
    private final long updatedAt;

    // ==================== CONSTRUCTOR ====================

    public LocalEvent(Builder builder) {
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

    @NonNull
    public String getID() {
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

    // ==================== BUILDER PATTERN ====================

    /**
     * Create a builder for constructing EventEntityGoogle instances.
     *
     * @return Builder instance
     */
    @NonNull
    public static LocalEvent.Builder builder() {
        return new LocalEvent.Builder();
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
         * Copy data from existing EventEntityGoogle.
         */
        @NonNull
        public LocalEvent.Builder copyFrom(@NonNull LocalEvent source) {
            this.id = source.getID();
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
