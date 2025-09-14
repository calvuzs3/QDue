package net.calvuz.qdue.data.events.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.domain.common.enums.Priority;
import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.events.models.LocalEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity (
        tableName = "local_events",
        indices = {
                @Index (value = {"event_type"}),
                @Index (value = {"event_priority"})
        }
)
public class LocalEventEntity
{
    @PrimaryKey
    @NonNull
    @ColumnInfo (name = "id")
    private String id;

    @NonNull
    @ColumnInfo (name = "calendar_id")
    private String calendarId;

    @NonNull
    @ColumnInfo (name = "title")
    private String title;

    @Nullable
    @ColumnInfo (name = "description")
    private String description;

    @NonNull
    @ColumnInfo (name = "start_time")
    private LocalDateTime startTime;

    @NonNull
    @ColumnInfo (name = "end_time")
    private LocalDateTime endTime;

    @NonNull
    @ColumnInfo (name = "event_type")
    private EventType eventType;

    @NonNull
    @ColumnInfo (name = "event_priority")
    private Priority eventPriority;

    @NonNull
    @ColumnInfo (name = "all_day")
    Boolean allDay = true;

    @Nullable
    @ColumnInfo (name = "location")
    private String location;

    @Nullable
    @ColumnInfo (name = "custom_properties")
    private Map<String, String> customProperties;

    @Nullable
    @ColumnInfo (name = "source_url")
    private String sourceUrl;      // Original URL source

    @NonNull
    @ColumnInfo (name = "created_at")
    private Long createdAt;

    @NonNull
    @ColumnInfo (name = "updated_at")
    private Long updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for Room.
     */
    public LocalEventEntity() {
        this.id = UUID.randomUUID().toString();
        this.calendarId = "qdue";
        this.title = "Untitled";
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now().plusHours( 1 );
        this.allDay = true;
        this.eventType = EventType.GENERAL;
        this.eventPriority = Priority.NORMAL;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public LocalEventEntity(
            @NonNull String id,
            @NonNull String calendarId,
            @NonNull String title,
            @Nullable String description,
            @NonNull LocalDateTime startTime,
            @NonNull LocalDateTime endTime,
            @NonNull EventType eventType,
            @NonNull Priority eventPriority,
            @NonNull Boolean allDay,
            @Nullable String location,
            @Nullable Map<String, String> customProperties,
            @Nullable String sourceUrl,
            @NonNull Long createdAt,
            @NonNull Long updatedAt
    ) {
        this();
        this.id = id;
        this.calendarId = calendarId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventType = eventType;
        this.eventPriority = eventPriority;
        this.allDay = allDay;
        this.location = location;
        this.customProperties = customProperties;
        this.sourceUrl = sourceUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ==================== GETTERS AND SETTERS ====================

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(@NonNull String calendarId) {
        this.calendarId = calendarId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NonNull
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(@NonNull LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @NonNull
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(@NonNull LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @NonNull
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(@NonNull EventType eventType) {
        this.eventType = eventType;
    }

    @NonNull
    public Priority getEventPriority() {
        return eventPriority;
    }

    public void setEventPriority(@NonNull Priority priority) {
        this.eventPriority = priority;
    }

    @NonNull
    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(@NonNull Boolean allDay) {
        this.allDay = allDay;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    @Nullable
    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(@Nullable Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    @Nullable
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(@Nullable String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @NonNull
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== DOMAIN MODEL CONVERSION ====================

    /**
     * Convert database entity to domain model.
     * Creates EventEntityGoogle from database representation.
     *
     * @return EventEntityGoogle domain model
     */
    @NonNull
    public LocalEvent toDomainModel() {
        return LocalEvent.builder()
                .id( this.getId() )
                .calendarId( this.getCalendarId() )
                .title( this.getTitle() )
                .description( this.getDescription() )
                .startTime( this.getStartTime() )
                .endTime( this.getEndTime() )
                .eventType( this.getEventType() )
                .priority( this.getEventPriority() )
                .allDay( this.getAllDay() )
                .location( this.getLocation() )
                .customProperties( this.getCustomProperties() )
                .sourceUrl( this.getSourceUrl() )
                .createdAt( this.getCreatedAt() )
                .updatedAt( this.getUpdatedAt() )
                .build();
    }

    /**
     * Convert domain model to database entity.
     * Creates LocalEventEntity from domain representation.
     *
     * @return LocalEventEntity for database storage
     */
    @NonNull
    public static LocalEventEntity fromDomainModel(@NonNull LocalEvent localEvent) {

        return new LocalEventEntity(
                localEvent.getID(),
                localEvent.getCalendarId(),
                localEvent.getTitle(),
                localEvent.getDescription(),
                localEvent.getStartTime(),
                localEvent.getEndTime(),
                localEvent.getEventType(),
                localEvent.getPriority(),
                localEvent.isAllDay(),
                localEvent.getLocation(),
                localEvent.getCustomProperties(),
                localEvent.getSourceUrl(),
                localEvent.getCreatedAt(),
                localEvent.getUpdatedAt()
        );
    }
}
