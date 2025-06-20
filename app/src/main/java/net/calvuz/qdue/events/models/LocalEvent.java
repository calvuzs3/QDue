package net.calvuz.qdue.events.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.calvuz.qdue.events.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local event implementation with Room Entity annotations.
 * Stores events in app's local database with Google Calendar-like functionality.
 */
@Entity(
        tableName = "events",
        indices = {
                @Index(value = "start_time", name = "index_events_start_time"),
                @Index(value = "package_id", name = "index_events_package_id"),
                @Index(value = "event_type", name = "index_events_event_type"),
                @Index(value = "priority", name = "index_events_priority"),
                @Index(value = {"start_time", "end_time"}, name = "index_events_time_range")
        }
)
public class LocalEvent implements CalendarEvent {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "start_time")
    private LocalDateTime startTime;

    @ColumnInfo(name = "end_time")
    private LocalDateTime endTime;

    @ColumnInfo(name = "event_type")
    private EventType eventType;

    @ColumnInfo(name = "priority")
    private EventPriority priority;

    @ColumnInfo(name = "all_day")
    private boolean allDay;

    @ColumnInfo(name = "location")
    private String location;

    @ColumnInfo(name = "custom_properties")
    private Map<String, String> customProperties;

    // Package source info (for external URL events)
    @ColumnInfo(name = "package_id")
    private String packageId;      // Unique identifier for update tracking

    @ColumnInfo(name = "source_url")
    private String sourceUrl;      // Original URL source

    @ColumnInfo(name = "package_version")
    private String packageVersion; // Version for update checking

    @ColumnInfo(name = "last_updated")
    private LocalDateTime lastUpdated;

    // ==================== CONSTRUCTORS ====================

    public LocalEvent() {
        this.id = UUID.randomUUID().toString();
        this.eventType = EventType.GENERAL;
        this.priority = EventPriority.NORMAL;
        this.customProperties = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalEvent(String title, LocalDate date) {
        this();
        this.title = title;
        this.startTime = date.atStartOfDay();
        this.endTime = date.atTime(23, 59);
        this.allDay = true;
    }

    // ==================== CALENDAR EVENT INTERFACE ====================

    @Override
    public String getId() { return id; }

    @Override
    public String getTitle() { return title; }

    @Override
    public String getDescription() { return description; }

    @Override
    public LocalDateTime getStartTime() { return startTime; }

    @Override
    public LocalDateTime getEndTime() { return endTime; }

    @Override
    public LocalDate getDate() {
        return startTime != null ? startTime.toLocalDate() : LocalDate.now();
    }

    @Override
    public EventType getEventType() { return eventType; }

    @Override
    public EventPriority getPriority() { return priority; }

    @Override
    public EventSource getSource() {
        return packageId != null ? EventSource.EXTERNAL_URL : EventSource.LOCAL;
    }

    @Override
    public boolean isAllDay() { return allDay; }

    @Override
    public String getLocation() { return location; }

    @Override
    public Map<String, String> getCustomProperties() { return customProperties; }

    // ==================== ADDITIONAL GETTERS/SETTERS ====================

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getPackageVersion() { return packageVersion; }
    public void setPackageVersion(String packageVersion) { this.packageVersion = packageVersion; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    /**
     * Check if this event can be updated (came from external source)
     */
    public boolean isUpdatable() {
        return packageId != null && sourceUrl != null;
    }

    /**
     * Get formatted time string for display
     */
    public String getTimeString() {
        if (allDay) return "Tutto il giorno";
        if (startTime == null) return "";
        if (endTime == null) return startTime.toLocalTime().toString();
        return startTime.toLocalTime() + " - " + endTime.toLocalTime();
    }

    // ==================== STANDARD SETTERS ====================

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public void setPriority(EventPriority priority) { this.priority = priority; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public void setLocation(String location) { this.location = location; }
    public void setCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    // ==================== UTILITY METHODS ====================

    public boolean hasTime() {
        return !(startTime == null);
    }

    /**
     * Check if this event overlaps with another time range
     */
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        if (startTime == null || endTime == null || otherStart == null || otherEnd == null) {
            return false;
        }
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    /**
     * Check if this event is happening today
     */
    public boolean isToday() {
        return getDate().equals(LocalDate.now());
    }

    /**
     * Check if this event is in the past
     */
    public boolean isPast() {
        LocalDateTime now = LocalDateTime.now();
        return endTime != null ? endTime.isBefore(now) : getDate().isBefore(LocalDate.now());
    }

    /**
     * Check if this event is upcoming (starts in the future)
     */
    public boolean isUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null ? startTime.isAfter(now) : getDate().isAfter(LocalDate.now());
    }

    /**
     * Check if this event is currently happening
     */
    public boolean isHappening() {
        LocalDateTime now = LocalDateTime.now();
        if (startTime == null || endTime == null) {
            return isToday() && !isPast();
        }
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /**
     * Get duration in minutes (for timed events)
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Get duration in days (for multi-day events)
     */
    public long getDurationDays() {
        if (startTime == null || endTime == null) {
            return 1;
        }
        return java.time.Duration.between(startTime.toLocalDate().atStartOfDay(),
                endTime.toLocalDate().atTime(23, 59, 59)).toDays() + 1;
    }

    /**
     * Create a copy of this event (useful for editing)
     */
    public LocalEvent copy() {
        LocalEvent copy = new LocalEvent();
        copy.setId(UUID.randomUUID().toString()); // New ID for copy
        copy.setTitle(this.title);
        copy.setDescription(this.description);
        copy.setStartTime(this.startTime);
        copy.setEndTime(this.endTime);
        copy.setEventType(this.eventType);
        copy.setPriority(this.priority);
        copy.setAllDay(this.allDay);
        copy.setLocation(this.location);
        copy.setCustomProperties(this.customProperties != null ? new HashMap<>(this.customProperties) : null);
        copy.setPackageId(this.packageId);
        copy.setSourceUrl(this.sourceUrl);
        copy.setPackageVersion(this.packageVersion);
        copy.setLastUpdated(LocalDateTime.now());
        return copy;
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocalEvent that = (LocalEvent) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // ==================== TO STRING ====================

    @Override
    public String toString() {
        return "LocalEvent{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", eventType=" + eventType +
                ", priority=" + priority +
                ", allDay=" + allDay +
                ", packageId='" + packageId + '\'' +
                '}';
    }

    public LocalDate getStartDate() {
        return startTime != null ? startTime.toLocalDate() : LocalDate.now();
    }

    public LocalDate getEndDate() {
        return endTime != null ? endTime.toLocalDate() : LocalDate.now();
    }
}