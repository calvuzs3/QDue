package net.calvuz.qdue.events.models;

import net.calvuz.qdue.events.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local event implementation
 * Stores events in app's local database
 */
public class LocalEvent implements CalendarEvent {
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventType eventType;
    private EventPriority priority;
    private boolean allDay;
    private String location;
    private Map<String, String> customProperties;

    // Package source info (for external URL events)
    private String packageId;      // Unique identifier for update tracking
    private String sourceUrl;      // Original URL source
    private String packageVersion; // Version for update checking
    private LocalDateTime lastUpdated;

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

    // Implement CalendarEvent interface
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

    // Additional getters/setters for local event specific fields
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

    // Standard setters
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

    public boolean hasTime() {
        return !(startTime == null);
    }
}
