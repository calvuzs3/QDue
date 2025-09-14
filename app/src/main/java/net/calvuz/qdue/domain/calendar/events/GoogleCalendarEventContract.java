package net.calvuz.qdue.domain.calendar.events;

import net.calvuz.qdue.domain.events.enums.EventPriority;
import net.calvuz.qdue.domain.events.enums.EventSource;
import net.calvuz.qdue.domain.events.enums.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Main interface for all calendar events
 * Supports local events, Google Calendar events, and external sources
 */
public interface GoogleCalendarEventContract
{
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