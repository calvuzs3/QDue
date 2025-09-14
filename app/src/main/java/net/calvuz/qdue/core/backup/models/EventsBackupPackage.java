package net.calvuz.qdue.core.backup.models;

import net.calvuz.qdue.domain.calendar.events.models.EventEntityGoogle;

import java.util.List;
import java.util.Map;

/**
 * STEP 1: Core Backup System Models
 * <p>
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== SPECIALIZED ENTITY BACKUPS ====================

/**
 * Events-specific backup with additional calendar features
 */
public class EventsBackupPackage extends EntityBackupPackage {
    public String calendarVersion;
    public List<EventEntityGoogle> events;
    public EventsBackupMetadata eventsMetadata;

    public EventsBackupPackage(List<EventEntityGoogle> events) {
        super("events", "1.0", events);
        this.events = events;
        this.calendarVersion = "1.0";
        this.eventsMetadata = new EventsBackupMetadata(events);
    }

    public static class EventsBackupMetadata {
        public int totalEvents;
        public String dateRange;
        public Map<String, Integer> eventsByType;
        public Map<String, Integer> eventsByPriority;
        public int allDayEvents;
        public int timedEvents;

        public EventsBackupMetadata(List<EventEntityGoogle> events) {
            if (events != null && !events.isEmpty()) {
                this.totalEvents = events.size();

                // Calculate date range
                EventEntityGoogle earliest = events.stream()
                        .min((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
                        .orElse(null);
                EventEntityGoogle latest = events.stream()
                        .max((e1, e2) -> e1.getEndTime().compareTo(e2.getEndTime()))
                        .orElse(null);

                if (earliest != null && latest != null) {
                    this.dateRange = earliest.getStartTime().toLocalDate().toString() +
                            " to " + latest.getEndTime().toLocalDate().toString();
                }

                // Count by type
                this.eventsByType = new java.util.HashMap<>();
                this.eventsByPriority = new java.util.HashMap<>();

                for (EventEntityGoogle event : events) {
                    // Count by type
                    String type = event.getEventType() != null ? event.getEventType().toString() : "UNKNOWN";
                    eventsByType.put(type, eventsByType.getOrDefault(type, 0) + 1);

                    // Count by priority
                    String priority = event.getPriority() != null ? event.getPriority().toString() : "UNKNOWN";
                    eventsByPriority.put(priority, eventsByPriority.getOrDefault(priority, 0) + 1);

                    // Count all-day vs timed
                    if (event.isAllDay()) {
                        allDayEvents++;
                    } else {
                        timedEvents++;
                    }
                }
            }
        }
    }
}
