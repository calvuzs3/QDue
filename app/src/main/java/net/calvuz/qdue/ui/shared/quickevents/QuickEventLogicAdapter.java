package net.calvuz.qdue.ui.shared.quickevents;

import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * QuickEventLogicAdapter - Business Logic Bridge per Eventi Utente
 *
 * STRATEGIA:
 * - Bridge pattern tra ToolbarAction e LocalEvent creation
 * - Applica business logic derivata da TurnException senza dipendenza diretta
 * - Gestisce creazione eventi user-managed con logica intelligente
 * - Future-proof: quando TurnException sarà rimosso, questo adapter rimane
 *
 * RESPONSABILITÀ:
 * - Applicare business rules per eventi utente (assenze, straordinari)
 * - Configurare customProperties con metadata appropriati
 * - Gestire validazione e constraints specifici per tipo evento
 * - Fornire defaults intelligenti basati su EventType
 */
public class QuickEventLogicAdapter {

    private static final String TAG = "QuickEventLogicAdapter";

    // ==================== BUSINESS RULES CONSTANTS ====================

    // Metadata keys for customProperties
    private static final String PROP_QUICK_CREATED = "quick_created";
    private static final String PROP_TOOLBAR_SOURCE = "toolbar_source";
    private static final String PROP_USER_MANAGED = "user_managed";
    private static final String PROP_AFFECTS_WORK = "affects_work_schedule";
    private static final String PROP_APPROVAL_REQUIRED = "approval_required";
    private static final String PROP_EVENT_CATEGORY = "event_category";
    private static final String PROP_CREATION_TIMESTAMP = "creation_timestamp";
    private static final String PROP_CREATOR_TYPE = "creator_type";

    // Event-specific metadata
    private static final String PROP_ABSENCE_TYPE = "absence_type";
    private static final String PROP_OVERTIME_TYPE = "overtime_type";
    private static final String PROP_SHIFT_EXTENSION = "shift_extension";
    private static final String PROP_MEDICAL_CERT = "medical_certificate";
    private static final String PROP_PROTECTED_LEAVE = "protected_leave";
    private static final String PROP_LAW_REFERENCE = "law_reference";
    private static final String PROP_LEAVE_CATEGORY = "leave_category";

    // ==================== CORE ADAPTER METHODS ====================

    /**
     * Create LocalEvent from ToolbarAction with intelligent defaults and business logic
     * Main entry point for quick event creation
     *
     * @param action ToolbarAction that triggered creation
     * @param date Date for the event
     * @param userId User ID creating the event (optional, can be null)
     * @return Configured LocalEvent ready for database save
     */
    public static LocalEvent createEventFromAction(ToolbarAction action, LocalDate date, Long userId) {
        Log.d(TAG, "Creating event from action: " + action + " for date: " + date);

        // Validate inputs
        if (action == null || date == null) {
            throw new IllegalArgumentException("Action and date cannot be null");
        }

        EventType eventType = action.getMappedEventType();
        if (eventType == null) {
            throw new IllegalArgumentException("ToolbarAction must have mapped EventType: " + action);
        }

        // Create base LocalEvent
        LocalEvent event = new LocalEvent();
        event.setId(generateEventId(action, date));

        // Apply basic configuration
        applyBasicConfiguration(event, action, eventType, date);

        // Apply business logic based on event type
        applyBusinessLogic(event, action, eventType, userId);

        // Apply custom properties with metadata
        applyCustomProperties(event, action, eventType, userId);

        // Validate final configuration
        validateEventConfiguration(event, action);

        Log.d(TAG, "Event created successfully: " + event.getTitle() + " (" + event.getId() + ")");
        return event;
    }

    /**
     * Update existing LocalEvent with user event business logic
     * Used for editing events created via quick actions
     */
    public static void updateEventWithUserLogic(LocalEvent event, ToolbarAction originalAction, Long userId) {
        if (event == null || originalAction == null) return;

        EventType eventType = originalAction.getMappedEventType();
        if (eventType == null) return;

        // Re-apply business logic (useful after event editing)
        applyBusinessLogic(event, originalAction, eventType, userId);

        // Update modification timestamp
        event.setLastUpdated(LocalDateTime.now());

        // Update custom properties
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        props.put("last_modified", LocalDateTime.now().toString());
        props.put("modified_by_logic", "user_event_adapter");

        event.setCustomProperties(props);
    }

    // ==================== CONFIGURATION METHODS ====================

    /**
     * Apply basic event configuration (title, type, priority, timing)
     */
    private static void applyBasicConfiguration(LocalEvent event, ToolbarAction action,
                                                EventType eventType, LocalDate date) {
        // Basic properties
        event.setTitle(eventType.getDisplayName());
        event.setDescription(eventType.getDescription());
        event.setEventType(eventType);
        event.setPriority(eventType.getDefaultPriority());

        // Timing configuration
        boolean allDay = eventType.isDefaultAllDay();
        event.setAllDay(allDay);

        if (allDay) {
            event.setStartTime(date.atStartOfDay());
            event.setEndTime(date.atTime(23, 59, 59));
        } else {
            LocalTime startTime = eventType.getDefaultStartTime();
            LocalTime endTime = eventType.getDefaultEndTime();
            event.setStartTime(date.atTime(startTime));
            event.setEndTime(date.atTime(endTime));
        }

        // Timestamps
        event.setLastUpdated(LocalDateTime.now());
    }

    /**
     * Apply business logic based on event type and user context
     */
    private static void applyBusinessLogic(LocalEvent event, ToolbarAction action,
                                           EventType eventType, Long userId) {

        // Work absence logic
        if (eventType.isWorkAbsence()) {
            applyWorkAbsenceLogic(event, eventType);
        }

        // Overtime/work event logic
        if (eventType == EventType.OVERTIME) {
            applyOvertimeLogic(event);
        }

        // Special leave logic
        if (eventType == EventType.SPECIAL_LEAVE) {
            applySpecialLeaveLogic(event);
        }

        // Sick leave logic
        if (eventType == EventType.SICK_LEAVE) {
            applySickLeaveLogic(event);
        }

        // Approval workflow logic
        if (eventType.requiresApproval()) {
            applyApprovalLogic(event, eventType);
        }

        // User-specific logic
        if (userId != null) {
            applyUserSpecificLogic(event, userId, eventType);
        }
    }

    /**
     * Apply work absence specific logic (vacation, personal leave, etc.)
     */
    private static void applyWorkAbsenceLogic(LocalEvent event, EventType eventType) {
        // Work absences are always all-day by default
        if (!event.isAllDay()) {
            Log.w(TAG, "Work absence should typically be all-day: " + eventType);
        }

        // Set high priority for certain absences
        if (eventType == EventType.SICK_LEAVE || eventType == EventType.SPECIAL_LEAVE) {
            event.setPriority(EventPriority.HIGH);
        }

        // Set location to null (not applicable for absences)
        event.setLocation(null);
    }

    /**
     * Apply overtime specific logic
     */
    private static void applyOvertimeLogic(LocalEvent event) {
        // Overtime should not be all-day
        if (event.isAllDay()) {
            Log.w(TAG, "Overtime should be timed event, not all-day");
            event.setAllDay(false);

            // Set default overtime hours (6:00-14:00)
            LocalDate date = event.getStartTime().toLocalDate();
            event.setStartTime(date.atTime(6, 0));
            event.setEndTime(date.atTime(14, 0));
        }

        // Set high priority for overtime
        event.setPriority(EventPriority.HIGH);
    }

    /**
     * Apply special leave (Legge 104) specific logic
     */
    private static void applySpecialLeaveLogic(LocalEvent event) {
        // Special leave has high priority and no approval required
        event.setPriority(EventPriority.HIGH);
    }

    /**
     * Apply sick leave specific logic
     */
    private static void applySickLeaveLogic(LocalEvent event) {
        // Sick leave has high priority
        event.setPriority(EventPriority.HIGH);

        // Set urgent if created on same day (emergency sick leave)
        LocalDate today = LocalDate.now();
        LocalDate eventDate = event.getStartTime().toLocalDate();
        if (eventDate.equals(today) || eventDate.equals(today.plusDays(1))) {
            event.setPriority(EventPriority.URGENT);
        }
    }

    /**
     * Apply approval workflow logic
     */
    private static void applyApprovalLogic(LocalEvent event, EventType eventType) {
        // Events requiring approval get normal priority unless overridden
        if (event.getPriority() == null) {
            event.setPriority(EventPriority.NORMAL);
        }

        // Set description to include approval note
        String currentDesc = event.getDescription();
        if (currentDesc == null) currentDesc = "";

        if (!currentDesc.contains("approvazione")) {
            event.setDescription(currentDesc + " (Richiede approvazione)");
        }
    }

    /**
     * Apply user-specific logic
     */
    private static void applyUserSpecificLogic(LocalEvent event, Long userId, EventType eventType) {
        // Add user context to description if needed
        // This is where user-specific business rules would go

        // For now, just ensure user tracking in metadata
        // (handled in applyCustomProperties)
    }

    /**
     * Apply custom properties with comprehensive metadata
     */
    private static void applyCustomProperties(LocalEvent event, ToolbarAction action,
                                              EventType eventType, Long userId) {
        Map<String, String> props = new HashMap<>();

        // Core metadata
        props.put(PROP_QUICK_CREATED, "true");
        props.put(PROP_TOOLBAR_SOURCE, action.name());
        props.put(PROP_USER_MANAGED, String.valueOf(eventType.isUserManaged()));
        props.put(PROP_AFFECTS_WORK, String.valueOf(eventType.affectsWorkSchedule()));
        props.put(PROP_APPROVAL_REQUIRED, String.valueOf(eventType.requiresApproval()));
        props.put(PROP_EVENT_CATEGORY, eventType.getCategory());
        props.put(PROP_CREATION_TIMESTAMP, LocalDateTime.now().toString());
        props.put(PROP_CREATOR_TYPE, "toolbar_quick_action");

        // User tracking
        if (userId != null) {
            props.put("creator_user_id", String.valueOf(userId));
        }

        // Event type specific properties
        switch (eventType) {
            case VACATION:
                props.put(PROP_ABSENCE_TYPE, "vacation");
                props.put(PROP_LEAVE_CATEGORY, "annual_leave");
                break;

            case SICK_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "sick_leave");
                props.put(PROP_MEDICAL_CERT, "pending");
                props.put(PROP_LEAVE_CATEGORY, "medical_leave");
                break;

            case OVERTIME:
                props.put(PROP_OVERTIME_TYPE, "scheduled");
                props.put(PROP_SHIFT_EXTENSION, "true");
                break;

            case PERSONAL_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "personal");
                props.put(PROP_LEAVE_CATEGORY, "personal_leave");
                break;

            case SPECIAL_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "special_leave");
                props.put(PROP_PROTECTED_LEAVE, "true");
                props.put(PROP_LAW_REFERENCE, "104/92");
                props.put(PROP_LEAVE_CATEGORY, "protected_leave");
                break;

            case SYNDICATE_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "syndicate");
                props.put(PROP_LEAVE_CATEGORY, "union_leave");
                break;
        }

        // Apply additional EventType default properties
        Map<String, String> eventTypeProps = eventType.getDefaultCustomProperties();
        props.putAll(eventTypeProps);

        event.setCustomProperties(props);
    }

    // ==================== VALIDATION & UTILITY ====================

    /**
     * Validate final event configuration
     */
    private static void validateEventConfiguration(LocalEvent event, ToolbarAction action) {
        // Basic validation
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Event title cannot be empty");
        }

        if (event.getEventType() == null) {
            throw new IllegalStateException("Event type must be set");
        }

        if (event.getStartTime() == null) {
            throw new IllegalStateException("Event start time must be set");
        }

        // Time validation for timed events
        if (!event.isAllDay()) {
            if (event.getEndTime() == null) {
                throw new IllegalStateException("Timed events must have end time");
            }

            if (!event.getStartTime().isBefore(event.getEndTime())) {
                throw new IllegalStateException("Start time must be before end time");
            }
        }

        // EventType-specific validation
        EventType eventType = event.getEventType();
        if (!eventType.isValidConfiguration(event.isAllDay(),
                event.getStartTime() != null ? event.getStartTime().toLocalTime() : null,
                event.getEndTime() != null ? event.getEndTime().toLocalTime() : null)) {
            Log.w(TAG, "Event configuration may not be optimal for event type: " + eventType);
        }
    }

    /**
     * Generate unique event ID
     */
    private static String generateEventId(ToolbarAction action, LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String actionStr = action.name().toLowerCase();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        return String.format("quick_%s_%s_%s", actionStr, dateStr, randomSuffix);
    }

    // ==================== QUERY & ANALYSIS METHODS ====================

    /**
     * Check if LocalEvent was created via quick action
     */
    public static boolean isQuickCreatedEvent(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;
        return "true".equals(event.getCustomProperties().get(PROP_QUICK_CREATED));
    }

    /**
     * Get ToolbarAction that created this event (if any)
     */
    public static ToolbarAction getCreatorToolbarAction(LocalEvent event) {
        if (event.getCustomProperties() == null) return null;

        String actionName = event.getCustomProperties().get(PROP_TOOLBAR_SOURCE);
        if (actionName == null) return null;

        try {
            return ToolbarAction.valueOf(actionName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if event affects work schedule (based on stored metadata)
     */
    public static boolean eventAffectsWorkSchedule(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;
        return "true".equals(event.getCustomProperties().get(PROP_AFFECTS_WORK));
    }

    /**
     * Check if event requires approval (based on stored metadata)
     */
    public static boolean eventRequiresApproval(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;
        return "true".equals(event.getCustomProperties().get(PROP_APPROVAL_REQUIRED));
    }

    /**
     * Get event category from metadata
     */
    public static String getEventCategory(LocalEvent event) {
        if (event.getCustomProperties() == null) return "unknown";
        return event.getCustomProperties().getOrDefault(PROP_EVENT_CATEGORY, "unknown");
    }

    // ==================== BUSINESS LOGIC QUERIES ====================

    /**
     * Check if user can create this type of event on specified date
     * Business rules validation
     */
    public static boolean canCreateEventOnDate(ToolbarAction action, LocalDate date, Long userId) {
        if (action == null || date == null) return false;

        // Basic date validation
        if (date.isBefore(LocalDate.now().minusDays(30))) {
            Log.w(TAG, "Cannot create events more than 30 days in the past");
            return false;
        }

        if (date.isAfter(LocalDate.now().plusYears(1))) {
            Log.w(TAG, "Cannot create events more than 1 year in the future");
            return false;
        }

        // Action-specific validation
        EventType eventType = action.getMappedEventType();
        if (eventType == null) return false;

        // Sick leave cannot be created too far in future
        if (eventType == EventType.SICK_LEAVE && date.isAfter(LocalDate.now().plusDays(2))) {
            Log.w(TAG, "Sick leave cannot be created more than 2 days in advance");
            return false;
        }

        return true;
    }

    /**
     * Get recommended title for event based on date and context
     */
    public static String getRecommendedTitle(ToolbarAction action, LocalDate date) {
        EventType eventType = action.getMappedEventType();
        if (eventType == null) return "Evento";

        String baseTitle = eventType.getDisplayName();

        // Add date context for certain events
        if (eventType == EventType.OVERTIME) {
            return baseTitle + " - " + date.getDayOfWeek().toString();
        }

        return baseTitle;
    }

    /**
     * Get debug summary for development/troubleshooting
     */
    public static String getDebugSummary(LocalEvent event) {
        if (event == null) return "NULL EVENT";

        StringBuilder sb = new StringBuilder();
        sb.append("LocalEvent{");
        sb.append("id=").append(event.getId());
        sb.append(", title=").append(event.getTitle());
        sb.append(", type=").append(event.getEventType());
        sb.append(", date=").append(event.getStartTime() != null ? event.getStartTime().toLocalDate() : "NULL");
        sb.append(", allDay=").append(event.isAllDay());
        sb.append(", quickCreated=").append(isQuickCreatedEvent(event));
        sb.append(", createdBy=").append(getCreatorToolbarAction(event));
        sb.append(", category=").append(getEventCategory(event));
        sb.append("}");

        return sb.toString();
    }
}