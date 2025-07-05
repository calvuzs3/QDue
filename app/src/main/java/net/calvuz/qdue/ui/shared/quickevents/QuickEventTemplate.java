package net.calvuz.qdue.ui.shared.quickevents;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * QuickEventTemplate - Simplified template system with QuickEventLogicAdapter integration
 *
 * EVOLUTION STRATEGY:
 * - Drastically simplified thanks to QuickEventLogicAdapter
 * - Now focuses only on template structure and validation
 * - Business logic completely delegated to QuickEventLogicAdapter
 * - Maintains template concept for UI consistency and future extensions
 *
 * RESPONSABILITÀ RIDOTTE:
 * - Template structure definition and validation
 * - Preview/display logic for UI
 * - Factory pattern for template creation
 * - Integration bridge to QuickEventLogicAdapter
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

    public boolean isValid() {
        return isValid;
    }

    public final boolean isValid;

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
        this.isValid = validateTemplate();

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

    // ==================== TEMPLATE USAGE ====================

    /**
     * ✅ MAIN METHOD: Create LocalEvent from this template
     * Delegates all business logic to QuickEventLogicAdapter
     */
    public LocalEvent createEvent(LocalDate date, Long userId) {
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
        LocalEvent event = QuickEventLogicAdapter.createEventFromAction(sourceAction, date, userId);

        // Add template tracking to metadata
        addTemplateMetadata(event);

        Log.d(TAG, "Event created from template " + templateId + ": " + event.getId());
        return event;
    }

    /**
     * Create LocalEvent with default user (when userId not available)
     */
    public LocalEvent createEvent(LocalDate date) {
        return createEvent(date, null);
    }

    /**
     * Add template-specific metadata to created event
     */
    private void addTemplateMetadata(LocalEvent event) {
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        props.put("template_id", templateId);
        props.put("template_source", "quick_event_template");
        props.put("template_version", "2.0"); // Version after QuickEventLogicAdapter integration

        event.setCustomProperties(props);
    }

    // ==================== TEMPLATE PREVIEW/DISPLAY ====================

    /**
     * ✅ NEW: Get template preview for UI display
     * Shows what the event would look like without creating it
     */
    public EventPreview getPreview(LocalDate date) {
        if (eventType == null) return null;

        return new EventPreview(
                displayName,
                description,
                eventType.getEmoji(),
                eventType.getColor(),
                eventType.isDefaultAllDay(),
                eventType.isDefaultAllDay() ? null : eventType.getDefaultStartTime(),
                eventType.isDefaultAllDay() ? null : eventType.getDefaultEndTime(),
                eventType.getDefaultPriority(),
                eventType.affectsWorkSchedule(),
                eventType.requiresApproval()
        );
    }

    /**
     * ✅ NEW: Check if template can be used on specific date
     * Delegates validation to QuickEventLogicAdapter
     */
    public boolean canUseOnDate(LocalDate date, Long userId) {
        return isValid && QuickEventLogicAdapter.canCreateEventOnDate(sourceAction, date, userId);
    }

    /**
     * ✅ NEW: Get recommended title for specific date
     * Delegates to QuickEventLogicAdapter for context-aware titles
     */
    public String getRecommendedTitle(LocalDate date) {
        return QuickEventLogicAdapter.getRecommendedTitle(sourceAction, date);
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

    /**
     * Check if creates all-day events by default (delegated to EventType)
     */
    public boolean isDefaultAllDay() {
        return eventType != null && eventType.isDefaultAllDay();
    }

    /**
     * Get default priority (delegated to EventType)
     */
    public EventPriority getDefaultPriority() {
        return eventType != null ? eventType.getDefaultPriority() : EventPriority.NORMAL;
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

    /**
     * Generate unique template ID
     */
    private static String generateTemplateId(ToolbarAction action) {
        return "template_" + action.name().toLowerCase();
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDisplayName() {
        return eventType.getDisplayName();
    }

    // ==================== UTILITY CLASSES ====================

    /**
     * ✅ NEW: Event preview data class for UI display
     * Shows what the event would look like without creating it
     */
    public static class EventPreview {
        public final String title;
        public final String description;
        public final String emoji;
        public final int color;
        public final boolean allDay;
        public final LocalTime startTime; // null if allDay
        public final LocalTime endTime;   // null if allDay
        public final EventPriority priority;
        public final boolean affectsWork;
        public final boolean needsApproval;

        public EventPreview(String title, String description, String emoji, int color,
                            boolean allDay, LocalTime startTime, LocalTime endTime,
                            EventPriority priority, boolean affectsWork, boolean needsApproval) {
            this.title = title;
            this.description = description;
            this.emoji = emoji;
            this.color = color;
            this.allDay = allDay;
            this.startTime = startTime;
            this.endTime = endTime;
            this.priority = priority;
            this.affectsWork = affectsWork;
            this.needsApproval = needsApproval;
        }

        /**
         * Get formatted time string for display
         */
        public String getTimeString() {
            if (allDay) return "Tutto il giorno";
            if (startTime == null) return "";
            if (endTime == null) return startTime.toString();
            return startTime + " - " + endTime;
        }

        /**
         * Get summary for UI tooltips
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(title);
            if (allDay) {
                sb.append(" (tutto il giorno)");
            } else {
                sb.append(" (").append(getTimeString()).append(")");
            }
            if (needsApproval) {
                sb.append(" - Richiede approvazione");
            }
            if (affectsWork) {
                sb.append(" - Assenza dal lavoro");
            }
            return sb.toString();
        }
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

    @Override
    public String toString() {
        return "QuickEventTemplate{" +
                "id='" + templateId + '\'' +
                ", action=" + sourceAction +
                ", eventType=" + eventType +
                ", displayName='" + displayName + '\'' +
                ", valid=" + isValid +
                '}';
    }
}

// ==================== TEMPLATE FACTORY (Simplified) ====================

/**
 * ✅ SIMPLIFIED: Factory for QuickEventTemplate creation
 * Much simpler now that business logic is in QuickEventLogicAdapter
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
            Log.d(TAG, "Template cache initialized with " + templateCache.size() + " templates");
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
     * Check if template exists for action
     */
    public static boolean hasTemplate(ToolbarAction action) {
        return getTemplate(action) != null;
    }
}