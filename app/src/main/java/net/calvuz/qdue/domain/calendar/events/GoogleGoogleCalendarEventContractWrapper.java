package net.calvuz.qdue.domain.calendar.events;

import net.calvuz.qdue.domain.events.enums.EventPriority;
import net.calvuz.qdue.domain.events.enums.EventSource;
import net.calvuz.qdue.domain.events.enums.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for Google Calendar events
 * Will be implemented when Google Calendar integration is added
 */
public class GoogleGoogleCalendarEventContractWrapper
        implements GoogleCalendarEventContract
{
    private final com.google.api.services.calendar.model.Event googleEvent;
    private final String calendarId;

    public GoogleGoogleCalendarEventContractWrapper(com.google.api.services.calendar.model.Event googleEvent,
                                                    String calendarId) {
        this.googleEvent = googleEvent;
        this.calendarId = calendarId;
    }

    @Override
    public String getId() {
        return "google_" + googleEvent.getId();
    }

    @Override
    public String getTitle() {
        return googleEvent.getSummary();
    }

    @Override
    public String getDescription() {
        return googleEvent.getDescription();
    }

    @Override
    public LocalDateTime getStartTime() {
        // Convert Google DateTime to LocalDateTime
        if (googleEvent.getStart().getDateTime() != null) {
            return Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }

    @Override
    public LocalDateTime getEndTime() {
        // Convert Google DateTime to LocalDateTime
        if (googleEvent.getEnd().getDateTime() != null) {
            return Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }

    @Override
    public LocalDate getDate() {
        LocalDateTime start = getStartTime();
        return start != null ? start.toLocalDate() : LocalDate.now();
    }

    @Override
    public EventType getEventType() {
        // Map Google event to our types based on title/description keywords
        String title = getTitle();
        String desc = getDescription();
        String combined = (title + " " + (desc != null ? desc : "")).toLowerCase();

        if (combined.contains("riunione") || combined.contains("meeting")) return EventType.MEETING;
        if (combined.contains("formazione") || combined.contains("training")) return EventType.TRAINING;
        if (combined.contains("manutenzione")) return EventType.MAINTENANCE;
        if (combined.contains("fermata")) {
            if (combined.contains("cassa")) return EventType.STOP_CASSA;
            if (combined.contains("ordini")) return EventType.STOP_ORDERS;
            return EventType.STOP_PLANNED;
        }

        return EventType.IMPORTED;
    }

    @Override
    public EventPriority getPriority() {
        // Default to normal, could be enhanced with Google event metadata
        return EventPriority.NORMAL;
    }

    @Override
    public EventSource getSource() {
        return EventSource.GOOGLE_CALENDAR;
    }

    @Override
    public boolean isAllDay() {
        return googleEvent.getStart().getDate() != null;
    }

    @Override
    public String getLocation() {
        return googleEvent.getLocation();
    }

    @Override
    public Map<String, String> getCustomProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("google_calendar_id", calendarId);
        props.put("google_event_id", googleEvent.getId());
        return props;
    }

    /**
     * Get the original Google event
     */
    public com.google.api.services.calendar.model.Event getGoogleEvent() {
        return googleEvent;
    }

    /**
     * Get the calendar ID this event belongs to
     */
    public String getCalendarId() {
        return calendarId;
    }
}
