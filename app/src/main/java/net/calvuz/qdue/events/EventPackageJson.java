package net.calvuz.qdue.events;

/**
 * STEP 2: JSON Package Format and SSL Validation System
 * <p>
 * Simple JSON format for event packages with SSL validation
 * and manual update functionality
 */

// ==================== 1. JSON PACKAGE FORMAT ====================

/**
 * JSON Schema for event packages
 * <p>
 * Example package structure:
 * {
 *   "package_info": {
 *     "id": "company_events_2025",
 *     "name": "Eventi Aziendali 2025",
 *     "version": "1.2.0",
 *     "description": "Pacchetto eventi aziendali aggiornato",
 *     "created_date": "2025-01-15T10:30:00Z",
 *     "valid_from": "2025-01-01",
 *     "valid_to": "2025-12-31",
 *     "author": "HR Department",
 *     "contact_email": "hr@company.com"
 *   },
 *   "events": [
 *     {
 *       "id": "evt_001",
 *       "title": "Fermata Programmata Linea A",
 *       "description": "Manutenzione ordinaria programmata",
 *       "start_date": "2025-03-15",
 *       "end_date": "2025-03-17",
 *       "start_time": "08:00",
 *       "end_time": "17:00",
 *       "all_day": false,
 *       "event_type": "STOP_PLANNED",
 *       "priority": "HIGH",
 *       "location": "Stabilimento Nord - Linea A",
 *       "tags": ["manutenzione", "linea_a", "programmata"],
 *       "custom_properties": {
 *         "department": "Produzione",
 *         "affected_teams": ["A", "B"],
 *         "cost_center": "PROD_001"
 *       }
 *     }
 *   ]
 * }
 */

import java.util.List;
import java.util.Map;

/**
 * Data classes for JSON package parsing
 */
public class EventPackageJson {
    public PackageInfo package_info;
    public List<EventJson> events;

    public PackageInfo getPackageInfo() {
        return package_info;
    }

    public List<EventJson> getEvents() {
        return events;
    }

    public static class PackageInfo {

        public String id;
        public String name;
        public String version;
        public String description;
        public String created_date;
        public String valid_from;
        public String valid_to;
        public String author;
        public String contact_email;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getAuthor() {
            return author;
        }

        public String getCreatedAt() {
            return created_date;
        }
    }

    public static class EventJson {
        public String id;
        public String title;
        public String description;
        public String start_date;
        public String end_date;
        public String start_time;
        public String end_time;
        public boolean all_day = false;
        public String event_type = "GENERAL";
        public String priority = "NORMAL";
        public String location;
        public List<String> tags;
        public Map<String, String> custom_properties;
    }
}