package net.calvuz.qdue.ui.features.events.quickevents;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.events.actions.EventAction;
import net.calvuz.qdue.domain.calendar.events.metadata.EventMetadataManager;
import net.calvuz.qdue.domain.calendar.events.models.EventEntityGoogle;
import net.calvuz.qdue.domain.events.enums.EventType;
import net.calvuz.qdue.domain.events.enums.EventPriority;
import net.calvuz.qdue.core.services.models.QuickEventRequest;
import net.calvuz.qdue.core.services.models.EventPreview;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * QuickEventTemplate - Simplified template system with QuickEventLogicAdapter integration
 * <p>
 * EVOLUTION STRATEGY:
 * - Drastically simplified thanks to QuickEventLogicAdapter
 * - Now focuses only on template structure and validation
 * - Business logic completely delegated to QuickEventLogicAdapter
 * - Maintains template concept for UI consistency and future extensions
 * <p>
 * REFACTORED VERSION:
 * - ❌ REMOVED: All business logic (validation, event creation)
 * - ❌ REMOVED: Database operations
 * - ✅ KEPT: UI template structure and preview generation
 * - ✅ KEPT: Factory pattern for template creation
 * - ✅ NEW: Integration with service layer via DTOs
 * - ✅ NEW: Clean separation between UI and business logic
 * <p>
 * RESPONSIBILITIES (UI Only):
 * - Template metadata management
 * - UI preview generation
 * - Request DTO creation for service layer
 * - Display configuration
 */
public class QuickEventTemplate {

    private static final String TAG = "QuickEventTemplate";

    // ==================== CORE TEMPLATE PROPERTIES ====================

    // Template identifier
    public final String templateId;

    // Associated toolbar action
    public final ToolbarAction sourceAction;

    // Mapped event type (from ToolbarAction)
    public final EventType eventType;

    // Template metadata
    public final String displayName;
    public final String description;
       public final boolean isValid;

    // ==================== UI DISPLAY PROPERTIES ====================

    private final String iconResource;
    private final int colorResource;
    private final EventPriority defaultPriority;
    private final boolean defaultAllDay;
    private final LocalTime defaultStartTime;
    private final LocalTime defaultEndTime;

    // ==================== CONSTRUCTOR ====================

    /**
     * Simplified constructor - most properties derived from EventType
     */
    private QuickEventTemplate(String templateId, ToolbarAction sourceAction) {
        this.templateId = templateId;
        this.sourceAction = sourceAction;
        this.eventType = sourceAction.getMappedEventType();

        // Derive properties from EventType (via ToolbarAction)
        this.displayName = sourceAction.getEventDisplayName();
        this.description = sourceAction.getDescription();
        this.isValid = validateTemplateStructure();  // validateTemplate()

        // ✅ UI display configuration
        this.iconResource = getIconForEventType(eventType);
        this.colorResource = getColorForEventType(eventType);
        this.defaultPriority = getDefaultPriorityForEventType(eventType);
        this.defaultAllDay = getDefaultAllDayForEventType(eventType);
        this.defaultStartTime = getDefaultStartTimeForEventType(eventType);
        this.defaultEndTime = getDefaultEndTimeForEventType(eventType);

        Log.v(TAG, "Template created: " + templateId + " (" + displayName + ")");
    }

    // ==================== FACTORY METHODS ====================

    /**
     * ✅ SIMPLIFIED: Create template from ToolbarAction
     * Much simpler now that business logic is in QuickEventLogicAdapter
     */
    public static QuickEventTemplate fromToolbarAction(ToolbarAction action) {
        if (action == null) {
            throw new IllegalArgumentException("ToolbarAction cannot be null");
        }

        if (action.getMappedEventType() == null) {
            throw new IllegalArgumentException("ToolbarAction must have mapped EventType: " + action);
        }

        // Generate template ID
        String templateId = generateTemplateId(action);

        return new QuickEventTemplate(templateId, action);
    }

    /**
     * ✅ SIMPLIFIED: Create all available templates
     * Only creates templates for event-creation actions
     */
    public static Map<ToolbarAction, QuickEventTemplate> getAllTemplates() {
        Map<ToolbarAction, QuickEventTemplate> templates = new HashMap<>();

        // Create templates only for event-creation actions
        for (ToolbarAction action : ToolbarAction.getQuickEventActions()) {
            if (action.isEventCreationAction()) {
                try {
                    templates.put(action, fromToolbarAction(action));
                } catch (Exception e) {
                    Log.w(TAG, "Failed to create template for action: " + action + " - " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "Created " + templates.size() + " templates");
        return templates;
    }

    // ==================== 🆕 NEW: SERVICE INTEGRATION METHODS ====================

    /**
     * ✅ NEW: Create QuickEventRequest for service layer
     * This replaces the old createEvent() method
     */
    public QuickEventRequest createEventRequest(LocalDate date, Long userId) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        try {
            return QuickEventRequest.builder()
                    .templateId(templateId)
                    .sourceAction(sourceAction)
                    .eventType(eventType)
                    .date(date)
                    .userId(userId)
                    .displayName(displayName)
                    .description(description)
                    .priority(defaultPriority)
                    .allDay(defaultAllDay)
                    .startTime(defaultAllDay ? null : defaultStartTime)
                    .endTime(defaultAllDay ? null : defaultEndTime)
                    .customProperty("template_id", templateId)
                    .customProperty("ui_created", "true")
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create event request: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create event request", e);
        }
    }

    /**
     * ✅ NEW: Create EventPreview for UI display
     * This replaces the old getPreview() method
     */
    public EventPreview getPreview(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        try {
            return EventPreview.builder()
                    .title(displayName)
                    .description(description)
                    .eventType(eventType)
                    .priority(defaultPriority)
                    .date(date)
                    .allDay(defaultAllDay)
                    .startTime(defaultAllDay ? null : defaultStartTime)
                    .endTime(defaultAllDay ? null : defaultEndTime)
                    .templateId(templateId)
                    .iconResource(iconResource)
                    .colorResource(colorResource)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create preview: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create preview", e);
        }
    }

    // ==================== TEMPLATE USAGE ====================

    /**
     * ✅ MAIN METHOD: Create EventEntityGoogle from this template
     * Delegates all business logic to QuickEventLogicAdapter
     */
    public EventEntityGoogle createEvent(LocalDate date, Long userId) {
        if (!isValid) {
            throw new IllegalStateException("Cannot create event from invalid template: " + templateId);
        }

        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        // Validate business rules
        if (!QuickEventLogicAdapter.canCreateEventOnDate(sourceAction, date, userId)) {
            throw new IllegalArgumentException("Cannot create " + displayName + " on date: " + date);
        }

        // Delegate to QuickEventLogicAdapter for actual creation
        //EventEntityGoogle event = QuickEventLogicAdapter.createEventFromAction(sourceAction, date, userID);
        EventAction eventAction = ToolbarActionBridge.mapToEventAction(sourceAction);
        EventEntityGoogle event = QuickEventLogicAdapter.createEventFromEventAction( eventAction, date, userId);

        // Inizializzare metadata tracking
        event = EventMetadataManager.initializeEditSessionMetadata(event, userId);

        // Add template tracking to metadata
        addTemplateMetadata(event);

        Log.d(TAG, "Event created from template " + templateId + ": " + event.getId());
        return event;
    }

    /**
     * Create EventEntityGoogle with default user (when userID not available)
     */
    public EventEntityGoogle createEvent(LocalDate date) {
        return createEvent(date, null);
    }

    /**
     * Add template-specific metadata to created event
     */
    private void addTemplateMetadata(EventEntityGoogle event) {
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        props.put("template_id", templateId);
        props.put("template_source", "quick_event_template");
        props.put("template_version", "2.0"); // Version after QuickEventLogicAdapter integration

        event.setCustomProperties(props);
    }

    // Aggiungere metodo per titolo raccomandato
    public String getRecommendedTitle(LocalDate date) {
        EventAction eventAction = ToolbarActionBridge.mapToEventAction(sourceAction);
        return QuickEventLogicAdapter.getRecommendedTitle(eventAction, date);
    }

    // ==================== TEMPLATE PREVIEW/DISPLAY ====================

//    /**
//     * ✅ NEW: Get template preview for UI display
//     * Shows what the event would look like without creating it
//     */
//    public EventPreview getPreview(LocalDate date) {
//        if (eventType == null) return null;
//
//        return new EventPreview(
//                displayName,
//                description,
//                eventType.getEmoji(),
//                eventType.getColor(),
//                eventType.isDefaultAllDay(),
//                eventType.isDefaultAllDay() ? null : eventType.getDefaultStartTime(),
//                eventType.isDefaultAllDay() ? null : eventType.getDefaultEndTime(),
//                eventType.getDefaultPriority(),
//                eventType.affectsWorkSchedule(),
//                eventType.requiresApproval()
//        );
//    }

    /**
     * ✅ NEW: Check if template can be used on specific date
     * Delegates validation to QuickEventLogicAdapter
     */
    public boolean canUseOnDate(LocalDate date, Long userId) {
        return isValid && QuickEventLogicAdapter.canCreateEventOnDate(sourceAction, date, userId);
    }

    // ==================== TEMPLATE PROPERTIES (Simplified) ====================

    /**
     * Get display emoji (delegated to EventType)
     */
    public String getEmoji() {
        return eventType != null ? eventType.getEmoji() : "📅";
    }

    /**
     * Get display color (delegated to EventType)
     */
    public int getColor() {
        return eventType != null ? eventType.getColor() : android.graphics.Color.GRAY;
    }

    /**
     * Check if creates work absence (delegated to EventType)
     */
    public boolean createsWorkAbsence() {
        return eventType != null && eventType.isWorkAbsence();
    }

    /**
     * Check if requires approval (delegated to EventType)
     */
    public boolean requiresApproval() {
        return eventType != null && eventType.requiresApproval();
    }

    /**
     * Check if affects work schedule (delegated to EventType)
     */
    public boolean affectsWorkSchedule() {
        return eventType != null && eventType.affectsWorkSchedule();
    }


    // ==================== VALIDATION ====================

    /**
     * ✅ SIMPLIFIED: Template validation
     * Much simpler now that business logic is elsewhere
     */
    private boolean validateTemplate() {
        if (sourceAction == null) {
            Log.e(TAG, "Template validation failed: null sourceAction");
            return false;
        }

        if (eventType == null) {
            Log.e(TAG, "Template validation failed: null eventType for action " + sourceAction);
            return false;
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            Log.e(TAG, "Template validation failed: empty displayName for action " + sourceAction);
            return false;
        }

        // Validate that ToolbarAction can create events
        if (!sourceAction.isEventCreationAction()) {
            Log.e(TAG, "Template validation failed: action is not event creation action " + sourceAction);
            return false;
        }

        return true;
    }

    // ==================== GETTERS ====================

    public String getTemplateId() {
        return templateId;
    }

    public ToolbarAction getSourceAction() {
        return sourceAction;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getIconResource() {
        return iconResource; // delegated to EventType
    }

    public int getColorResource() {
        return colorResource; // delegated to EventType
    }

    public EventPriority getDefaultPriority() {
        return defaultPriority; // delegated to EventType
    }

    public boolean isDefaultAllDay() {
        return defaultAllDay; // delegated to EventType
    }

    public LocalTime getDefaultStartTime() {
        return defaultStartTime; // delegated to EventType
    }

    public LocalTime getDefaultEndTime() {
        return defaultEndTime; // delegated to EventType
    }

//    // ==================== UTILITY CLASSES ====================
//
//    /**
//     * ✅ NEW: Event preview data class for UI display
//     * Shows what the event would look like without creating it
//     */
//    public static class EventPreview {
//        public final String title;
//        public final String description;
//        public final String emoji;
//        public final int color;
//        public final boolean allDay;
//        public final LocalTime startTime; // null if allDay
//        public final LocalTime endTime;   // null if allDay
//        public final EventPriority priority;
//        public final boolean affectsWork;
//        public final boolean needsApproval;
//
//        public EventPreview(String title, String description, String emoji, int color,
//                            boolean allDay, LocalTime startTime, LocalTime endTime,
//                            EventPriority priority, boolean affectsWork, boolean needsApproval) {
//            this.title = title;
//            this.description = description;
//            this.emoji = emoji;
//            this.color = color;
//            this.allDay = allDay;
//            this.startTime = startTime;
//            this.endTime = endTime;
//            this.priority = priority;
//            this.affectsWork = affectsWork;
//            this.needsApproval = needsApproval;
//        }
//
//        /**
//         * Get formatted time string for display
//         */
//        public String getTimeString() {
//            if (allDay) return "Tutto il giorno";
//            if (startTime == null) return "";
//            if (endTime == null) return startTime.toString();
//            return startTime + " - " + endTime;
//        }
//
//        /**
//         * Get summary for UI tooltips
//         */
//        public String getSummary() {
//            StringBuilder sb = new StringBuilder();
//            sb.append(title);
//            if (allDay) {
//                sb.append(" (tutto il giorno)");
//            } else {
//                sb.append(" (").append(getTimeString()).append(")");
//            }
//            if (needsApproval) {
//                sb.append(" - Richiede approvazione");
//            }
//            if (affectsWork) {
//                sb.append(" - Assenza dal lavoro");
//            }
//            return sb.toString();
//        }
//    }


    // ==================== UI HELPER METHODS ====================

    /**
     * ✅ UI-only validation (no business logic)
     */
    private boolean validateTemplateStructure() {
        return templateId != null && !templateId.trim().isEmpty() &&
                sourceAction != null &&
                eventType != null &&
                displayName != null && !displayName.trim().isEmpty();
    }

    /**
     * ✅ UI configuration methods
     */
    private String getIconForEventType(EventType eventType) {
        if (eventType == null) return "ic_event_default";

        switch (eventType) {
            case STOP_PLANNED:
            case STOP_UNPLANNED:
                return "ic_stop";
            case MAINTENANCE:
                return "ic_maintenance";
            case MEETING:
                return "ic_meeting";
            case GENERAL:
            default:
                return "ic_event_general";
        }
    }

    private int getColorForEventType(EventType eventType) {
        if (eventType == null) return 0;

        switch (eventType) {
            case STOP_PLANNED:
                return android.R.color.holo_orange_light;
            case STOP_UNPLANNED:
                return android.R.color.holo_red_light;
            case MAINTENANCE:
                return android.R.color.holo_blue_light;
            case MEETING:
                return android.R.color.holo_green_light;
            case GENERAL:
            default:
                return android.R.color.holo_purple;
        }
    }

    private EventPriority getDefaultPriorityForEventType(EventType eventType) {
        if (eventType == null) return EventPriority.NORMAL;

        switch (eventType) {
            case STOP_UNPLANNED:
                return EventPriority.HIGH;
            case STOP_PLANNED:
            case MAINTENANCE:
                return EventPriority.NORMAL;
            case GENERAL, MEETING:
            default:
                return EventPriority.NORMAL;
        }
    }

    private boolean getDefaultAllDayForEventType(EventType eventType) {
        if (eventType == null) return true;

        switch (eventType) {
            case MEETING:
                return false;
            case GENERAL, STOP_PLANNED, STOP_UNPLANNED, MAINTENANCE:
            default:
                return true;
        }
    }

    private LocalTime getDefaultStartTimeForEventType(EventType eventType) {
        if (eventType == null) return LocalTime.of(8, 0);

        switch (eventType) {
            case MEETING:
                return LocalTime.of(9, 0);
            case STOP_PLANNED:
            case STOP_UNPLANNED:
            case GENERAL, MAINTENANCE:
            default:
                return LocalTime.of(8, 0);
        }
    }

    private LocalTime getDefaultEndTimeForEventType(EventType eventType) {
        if (eventType == null) return LocalTime.of(17, 0);

        switch (eventType) {
            case MEETING:
                return LocalTime.of(10, 0);
            case STOP_PLANNED:
            case STOP_UNPLANNED:
            case GENERAL, MAINTENANCE:
            default:
                return LocalTime.of(17, 0);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Generate unique template ID
     */
    private static String generateTemplateId(ToolbarAction action) {
        return "template_" + action.name().toLowerCase();
    }

    /**
     * Check if template can create events for date (UI validation only)
     */
    public boolean canCreateForDate(LocalDate date) {
        if (date == null) {
            return false;
        }

        // ✅ Basic UI validation only (no business logic)
        return !date.isBefore(LocalDate.now()) && isValid;
    }

    // ==================== DEBUG & DEVELOPMENT ====================

    /**
     * Get debug summary for development/logging
     */
    public String getDebugSummary() {
        return String.format("QuickEventTemplate{id=%s, action=%s, eventType=%s, valid=%s, workAbsence=%s}",
                templateId,
                sourceAction != null ? sourceAction.name() : "NULL",
                eventType != null ? eventType.name() : "NULL",
                isValid,
                createsWorkAbsence());
    }

    /**
     * Validate all templates consistency (for unit tests)
     */
    public static boolean validateAllTemplates() {
        try {
            Map<ToolbarAction, QuickEventTemplate> templates = getAllTemplates();

            for (QuickEventTemplate template : templates.values()) {
                if (!template.isValid) {
                    Log.e(TAG, "Invalid template found: " + template.getDebugSummary());
                    return false;
                }
            }

            // Check that all quick event actions have templates
            for (ToolbarAction action : ToolbarAction.getQuickEventActions()) {
                if (action.isEventCreationAction() && !templates.containsKey(action)) {
                    Log.e(TAG, "Missing template for quick event action: " + action);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Template validation failed: " + e.getMessage());
            return false;
        }
    }

    // ==================== OBJECT METHODS ====================

    @NonNull
    @Override
    public String toString() {
        return "QuickEventTemplate{" +
                "templateId='" + templateId + '\'' +
                ", sourceAction=" + sourceAction +
                ", eventType=" + eventType +
                ", displayName='" + displayName + '\'' +
                ", valid=" + isValid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuickEventTemplate that = (QuickEventTemplate) o;

        if (isValid != that.isValid) return false;
        if (!Objects.equals(templateId, that.templateId))
            return false;
        if (sourceAction != that.sourceAction) return false;
        if (eventType != that.eventType) return false;
        return Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        int result = templateId != null ? templateId.hashCode() : 0;
        result = 31 * result + (sourceAction != null ? sourceAction.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (isValid ? 1 : 0);
        return result;
    }
}



// ==================== TEMPLATE FACTORY (Simplified) ====================

/**
 * ✅ SIMPLIFIED: Factory for QuickEventTemplate creation
 * Much simpler now that business logic is in QuickEventLogicAdapter
 * ✅ SIMPLIFIED: Factory for QuickEventTemplate creation (UI only)
 */
class QuickEventTemplateFactory {

    private static final String TAG = "QuickEventTemplateFactory";

    /**
     * Create template from ToolbarAction with validation
     */
    public static QuickEventTemplate createTemplate(ToolbarAction action) {
        QuickEventTemplate template = QuickEventTemplate.fromToolbarAction(action);

        if (!template.isValid) {
            throw new IllegalStateException("Invalid template created for action: " + action);
        }

        return template;
    }

    /**
     * Get all available templates with caching
     */
    private static Map<ToolbarAction, QuickEventTemplate> templateCache = null;

    public static Map<ToolbarAction, QuickEventTemplate> getAllTemplates() {
        if (templateCache == null) {
            templateCache = QuickEventTemplate.getAllTemplates();
            Log.v(TAG, "Template cache initialized with " + templateCache.size() + " templates");
        }
        return new HashMap<>(templateCache); // Return copy for thread safety
    }

    /**
     * Clear template cache (useful for testing or configuration changes)
     */
    public static void clearCache() {
        templateCache = null;
        Log.d(TAG, "Template cache cleared");
    }

    /**
     * Get template for specific action
     */
    public static QuickEventTemplate getTemplate(ToolbarAction action) {
        Map<ToolbarAction, QuickEventTemplate> templates = getAllTemplates();
        return templates.get(action);
    }

    /**
     * Check if template exists for action (legacy support)
     */
    public static boolean hasTemplate(ToolbarAction action) {
        return hasTemplateSupport(action);
    }

    /**
     * Check if action has template support
     */
    public static boolean hasTemplateSupport(ToolbarAction action) {
        try {
            QuickEventTemplate template = QuickEventTemplate.fromToolbarAction(action);
            return template.isValid();
        } catch (Exception e) {
            return false;
        }
    }
}