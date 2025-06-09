package net.calvuz.qdue.events;

import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventSource;
import net.calvuz.qdue.events.models.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Main interface for all calendar events
 * Supports local events, Google Calendar events, and external sources
 */
public interface CalendarEvent {
    String getId();
    String getTitle();
    String getDescription();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    LocalDate getDate();
    EventType getEventType();
    EventPriority getPriority();
    EventSource getSource();
    boolean isAllDay();
    String getLocation();
    Map<String, String> getCustomProperties();
}