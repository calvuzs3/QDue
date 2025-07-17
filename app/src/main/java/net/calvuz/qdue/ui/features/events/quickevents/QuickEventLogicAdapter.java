package net.calvuz.qdue.ui.features.events.quickevents;

import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.actions.EventAction;
import net.calvuz.qdue.events.actions.EventActionManager;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MIGRATED: QuickEventLogicAdapter with EventAction Integration
 *
 * This migrated version leverages the new EventAction system while maintaining
 * backward compatibility with existing ToolbarAction-based functionality.
 *
 * Key Migration Benefits:
 * - Enhanced business logic through EventAction business rules
 * - Improved metadata accuracy and consistency
 * - Better validation and constraint checking
 * - Cleaner separation between UI and business concerns
 * - Forward compatibility with EventAction-based systems
 *
 * Migration Strategy:
 * - Primary logic now uses EventAction and EventActionManager
 * - ToolbarAction support maintained through bridge pattern
 * - Enhanced metadata generation with EventAction context
 * - Improved validation using EventAction business rules
 * - Seamless integration with existing quick event systems
 */
public class QuickEventLogicAdapter {

    private static final String TAG = "QuickEventLogicAdapter";

    // ==================== ENHANCED METADATA CONSTANTS ====================

    // Core metadata keys (maintained for backward compatibility)
    private static final String PROP_QUICK_CREATED = "quick_created";
    private static final String PROP_TOOLBAR_SOURCE = "toolbar_source";
    private static final String PROP_USER_MANAGED = "user_managed";
    private static final String PROP_AFFECTS_WORK = "affects_work_schedule";
    private static final String PROP_APPROVAL_REQUIRED = "approval_required";
    private static final String PROP_EVENT_CATEGORY = "event_category";
    private static final String PROP_CREATION_TIMESTAMP = "creation_timestamp";
    private static final String PROP_CREATOR_TYPE = "creator_type";

    // Enhanced EventAction metadata
    private static final String PROP_EVENT_ACTION = "event_action";
    private static final String PROP_ACTION_CATEGORY = "action_category";
    private static final String PROP_ACTION_VERSION = "action_version";
    private static final String PROP_BUSINESS_RULES_APPLIED = "business_rules_applied";
    private static final String PROP_CONSTRAINTS_VALIDATED = "constraints_validated";

    // EventAction-specific metadata
    private static final String PROP_ABSENCE_TYPE = "absence_type";
    private static final String PROP_OVERTIME_TYPE = "overtime_type";
    private static final String PROP_SHIFT_EXTENSION = "shift_extension";
    private static final String PROP_MEDICAL_CERT = "medical_certificate";
    private static final String PROP_PROTECTED_LEAVE = "protected_leave";
    private static final String PROP_LAW_REFERENCE = "law_reference";
    private static final String PROP_LEAVE_CATEGORY = "leave_category";

    // Enhanced business logic metadata
    private static final String PROP_COST_IMPACT_FACTOR = "cost_impact_factor";
    private static final String PROP_REQUIRES_DOCUMENTATION = "requires_documentation";
    private static final String PROP_AFFECTS_OVERTIME_CALC = "affects_overtime_calculation";
    private static final String PROP_MANAGER_APPROVAL_REQUIRED = "manager_approval_required";
    private static final String PROP_MINIMUM_ADVANCE_NOTICE = "minimum_advance_notice_days";
    private static final String PROP_MAXIMUM_DURATION = "maximum_duration_days";

    // ==================== ENHANCED CORE ADAPTER METHODS ====================

    /**
     * ENHANCED: Create LocalEvent from EventAction with comprehensive business logic.
     * This is the primary method that leverages EventAction business rules.
     *
     * @param action EventAction that defines the business logic
     * @param date Date for the event
     * @param userId User ID creating the event (optional)
     * @return Configured LocalEvent with enhanced metadata and validation
     */
    public static LocalEvent createEventFromEventAction(EventAction action, LocalDate date, Long userId) {
        Log.d(TAG, "Creating event from EventAction: " + action + " for date: " + date);

        if (action == null || date == null) {
            throw new IllegalArgumentException("EventAction and date cannot be null");
        }

        try {
            // Use EventActionManager for business logic and validation
            LocalEvent event = EventActionManager.createEventFromAction(action, date, userId);

            // Apply enhanced quick event metadata
            applyEnhancedQuickEventMetadata(event, action, userId);

            // Apply EventAction-specific business logic
            applyEventActionSpecificLogic(event, action, userId);

            Log.d(TAG, "Enhanced event created from EventAction " + action + ": " + event.getId());
            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error creating event from EventAction: " + e.getMessage());
            throw new RuntimeException("Failed to create event from EventAction", e);
        }
    }

    /**
     * ENHANCED: Create LocalEvent from ToolbarAction with EventAction bridge.
     * Maintains backward compatibility while leveraging EventAction improvements.
     *
     * @param action ToolbarAction (for backward compatibility)
     * @param date Date for the event
     * @param userId User ID creating the event (optional)
     * @return Configured LocalEvent with EventAction-enhanced logic
     */
    public static LocalEvent createEventFromAction(ToolbarAction action, LocalDate date, Long userId) {
        Log.d(TAG, "Creating event from ToolbarAction: " + action + " (with EventAction bridge)");

        if (action == null || date == null) {
            throw new IllegalArgumentException("ToolbarAction and date cannot be null");
        }

        try {
            // Convert ToolbarAction to EventAction using bridge
            EventAction eventAction = ToolbarActionBridge.mapToEventAction(action);

            // Create event using EventAction logic
            LocalEvent event = createEventFromEventAction(eventAction, date, userId);

            // Preserve ToolbarAction metadata for backward compatibility
            preserveToolbarActionMetadata(event, action);

            Log.d(TAG, "Event created from ToolbarAction " + action + " via EventAction " + eventAction + ": " + event.getId());
            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error creating event from ToolbarAction: " + e.getMessage());
            throw new RuntimeException("Failed to create event from ToolbarAction", e);
        }
    }

    /**
     * ENHANCED: Check if an EventAction can be performed on a specific date.
     * Uses EventAction business rules for accurate validation.
     */
    public static boolean canCreateEventActionOnDate(EventAction action, LocalDate date, Long userId) {
        if (action == null || date == null) {
            return false;
        }

        try {
            return EventActionManager.canPerformAction(action, date, userId);
        } catch (Exception e) {
            Log.e(TAG, "Error checking EventAction availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * ENHANCED: Check if a ToolbarAction can be performed (backward compatibility).
     * Delegates to EventAction validation through bridge.
     */
    public static boolean canCreateEventOnDate(ToolbarAction action, LocalDate date, Long userId) {
        if (action == null || date == null) {
            return false;
        }

        try {
            EventAction eventAction = ToolbarActionBridge.mapToEventAction(action);
            return canCreateEventActionOnDate(eventAction, date, userId);
        } catch (Exception e) {
            Log.e(TAG, "Error checking ToolbarAction availability: " + e.getMessage());
            return false;
        }
    }

    // ==================== ENHANCED METADATA APPLICATION ====================

    /**
     * Apply enhanced quick event metadata with EventAction context.
     */
    private static void applyEnhancedQuickEventMetadata(LocalEvent event, EventAction action, Long userId) {
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        // Core quick event metadata (backward compatibility)
        props.put(PROP_QUICK_CREATED, "true");
        props.put(PROP_USER_MANAGED, "true");
        props.put(PROP_CREATION_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        props.put(PROP_CREATOR_TYPE, "quick_event_logic_adapter");

        // Enhanced EventAction metadata
        props.put(PROP_EVENT_ACTION, action.name());
        props.put(PROP_ACTION_CATEGORY, action.getCategory().name()); // getDisplayname(); // .name());
        props.put(PROP_ACTION_VERSION, "2.0"); // EventAction-based version
        props.put(PROP_BUSINESS_RULES_APPLIED, "true");
        props.put(PROP_CONSTRAINTS_VALIDATED, "true");

        // EventAction business rule metadata
        props.put(PROP_AFFECTS_WORK, String.valueOf(action.affectsWorkSchedule()));
        props.put(PROP_APPROVAL_REQUIRED, String.valueOf(action.requiresApproval()));
        props.put(PROP_COST_IMPACT_FACTOR, String.valueOf(action.getCostImpactFactor()));
        props.put(PROP_REQUIRES_DOCUMENTATION, String.valueOf(action.requiresDocumentation()));
        props.put(PROP_AFFECTS_OVERTIME_CALC, String.valueOf(action.affectsOvertimeCalculation()));
        props.put(PROP_MANAGER_APPROVAL_REQUIRED, String.valueOf(action.requiresManagerApproval()));
        props.put(PROP_MINIMUM_ADVANCE_NOTICE, String.valueOf(action.getMinimumAdvanceNoticeDays()));
        props.put(PROP_MAXIMUM_DURATION, String.valueOf(action.getMaximumDurationDays()));

        // User context
        if (userId != null) {
            props.put("created_by_user", String.valueOf(userId));
        }

        event.setCustomProperties(props);
        Log.d(TAG, "Enhanced quick event metadata applied for EventAction: " + action);
    }

    /**
     * Apply EventAction-specific business logic and metadata.
     */
    private static void applyEventActionSpecificLogic(LocalEvent event, EventAction action, Long userId) {
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        // Apply category-specific logic
        switch (action.getCategory()) {
            case ABSENCE:
                applyAbsenceLogic(event, action, props);
                break;
            case WORK_ADJUSTMENT:
                applyWorkAdjustmentLogic(event, action, props);
                break;
            case PRODUCTION:
                applyProductionLogic(event, action, props);
                break;
            case DEVELOPMENT:
                applyDevelopmentLogic(event, action, props);
                break;
            case GENERAL:
                applyGeneralLogic(event, action, props);
                break;
        }

        // Apply urgency logic
        if (action.isUrgentByNature()) {
            applyUrgencyLogic(event, action);
        }

        // Apply approval workflow logic
        if (action.requiresApproval()) {
            applyApprovalLogic(event, action);
        }

        // Apply user-specific logic
        applyUserSpecificLogic(event, userId, action);

        event.setCustomProperties(props);
        Log.d(TAG, "EventAction-specific logic applied for: " + action);
    }

    /**
     * Apply absence-specific business logic.
     */
    private static void applyAbsenceLogic(LocalEvent event, EventAction action, Map<String, String> props) {
        props.put(PROP_EVENT_CATEGORY, "absence");

        switch (action) {
            case VACATION:
                props.put(PROP_ABSENCE_TYPE, "vacation");
                props.put(PROP_LEAVE_CATEGORY, "planned_leave");
                break;

            case SICK_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "sick");
                props.put(PROP_MEDICAL_CERT, "may_be_required");
                props.put(PROP_PROTECTED_LEAVE, "true");
                break;

            case PERSONAL_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "personal");
                props.put(PROP_LEAVE_CATEGORY, "personal_leave");
                break;

            case SPECIAL_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "special");
                props.put(PROP_LAW_REFERENCE, "law_104");
                props.put(PROP_PROTECTED_LEAVE, "true");
                break;

            case SYNDICATE_LEAVE:
                props.put(PROP_ABSENCE_TYPE, "syndicate");
                props.put(PROP_LEAVE_CATEGORY, "union_leave");
                props.put(PROP_PROTECTED_LEAVE, "true");
                break;
        }

        Log.d(TAG, "Absence logic applied for: " + action);
    }

    /**
     * Apply work adjustment specific business logic.
     */
    private static void applyWorkAdjustmentLogic(LocalEvent event, EventAction action, Map<String, String> props) {
        props.put(PROP_EVENT_CATEGORY, "work_adjustment");

        switch (action) {
            case OVERTIME:
                props.put(PROP_OVERTIME_TYPE, "standard_overtime");
                props.put(PROP_SHIFT_EXTENSION, "true");

                // Adjust default timing for overtime
                if (event.getStartTime() != null) {
                    LocalTime startTime = event.getStartTime().toLocalTime();
                    if (startTime.isBefore(LocalTime.of(17, 0))) {
                        // If before 5 PM, assume after-hours overtime
                        event.setStartTime(event.getStartTime().with(LocalTime.of(17, 0)));
                        event.setEndTime(event.getStartTime().plusHours(3)); // 3 hours overtime
                    }
                }
                break;

            case SHIFT_SWAP:
                props.put(PROP_OVERTIME_TYPE, "shift_swap");
                props.put(PROP_SHIFT_EXTENSION, "false");
                break;

            case COMPENSATION:
                props.put(PROP_OVERTIME_TYPE, "compensation");
                props.put(PROP_SHIFT_EXTENSION, "false");
                break;
        }

        Log.d(TAG, "Work adjustment logic applied for: " + action);
    }

    /**
     * Apply production-specific business logic.
     */
    private static void applyProductionLogic(LocalEvent event, EventAction action, Map<String, String> props) {
        props.put(PROP_EVENT_CATEGORY, "production");

        // Production events typically need location
        if (event.getLocation() == null || event.getLocation().isEmpty()) {
            event.setLocation("Area Produzione");
        }

        switch (action) {
            case PLANNED_STOP:
                props.put("stop_type", "planned");
                props.put("production_impact", "scheduled");
                break;

            case UNPLANNED_STOP:
                props.put("stop_type", "unplanned");
                props.put("production_impact", "immediate");
                // Unplanned stops are urgent
                event.setPriority(EventPriority.URGENT);
                break;

            case MAINTENANCE:
                props.put("maintenance_type", "scheduled");
                props.put("production_impact", "planned");
                break;

            case EMERGENCY:
                props.put("emergency_type", "production");
                props.put("production_impact", "critical");
                event.setPriority(EventPriority.URGENT);
                break;
        }

        Log.d(TAG, "Production logic applied for: " + action);
    }

    /**
     * Apply development-specific business logic.
     */
    private static void applyDevelopmentLogic(LocalEvent event, EventAction action, Map<String, String> props) {
        props.put(PROP_EVENT_CATEGORY, "development");

        switch (action) {
            case TRAINING:
                props.put("training_type", "professional_development");
                props.put("affects_productivity", "temporary");

                // Training typically during business hours
                if (!event.isAllDay() && event.getStartTime() != null) {
                    LocalTime startTime = event.getStartTime().toLocalTime();
                    if (startTime.isBefore(LocalTime.of(8, 0)) || startTime.isAfter(LocalTime.of(18, 0))) {
                        // Adjust to business hours
                        event.setStartTime(event.getStartTime().with(LocalTime.of(9, 0)));
                        event.setEndTime(event.getStartTime().plusHours(8)); // Full day training
                    }
                }
                break;

            case MEETING:
                props.put("meeting_type", "business");
                props.put("affects_productivity", "minimal");
                break;
        }

        Log.d(TAG, "Development logic applied for: " + action);
    }

    /**
     * Apply general event logic.
     */
    private static void applyGeneralLogic(LocalEvent event, EventAction action, Map<String, String> props) {
        props.put(PROP_EVENT_CATEGORY, "general");
        props.put("event_nature", "general_purpose");

        Log.d(TAG, "General logic applied for: " + action);
    }

    /**
     * Apply urgency-specific logic for time-sensitive events.
     */
    private static void applyUrgencyLogic(LocalEvent event, EventAction action) {
        // Emergency events happening today or tomorrow get urgent priority
        LocalDate today = LocalDate.now();
        LocalDate eventDate = event.getStartTime().toLocalDate();

        if (eventDate.equals(today) || eventDate.equals(today.plusDays(1))) {
            event.setPriority(EventPriority.URGENT);

            // Add urgency indicator to description
            String description = event.getDescription();
            if (description == null) description = "";
            if (!description.contains("URGENTE")) {
                event.setDescription("URGENTE: " + description);
            }
        }

        Log.d(TAG, "Urgency logic applied for: " + action);
    }

    /**
     * Apply approval workflow logic for events requiring authorization.
     */
    private static void applyApprovalLogic(LocalEvent event, EventAction action) {
        // Events requiring approval get normal priority unless overridden
        if (event.getPriority() == null) {
            event.setPriority(EventPriority.NORMAL);
        }

        // Add approval note to description
        String currentDesc = event.getDescription();
        if (currentDesc == null) currentDesc = "";

        if (!currentDesc.contains("approvazione")) {
            String approvalNote = action.requiresManagerApproval() ?
                    " (Richiede approvazione manager)" :
                    " (Richiede approvazione)";
            event.setDescription(currentDesc + approvalNote);
        }

        Log.d(TAG, "Approval logic applied for: " + action);
    }

    /**
     * Apply user-specific business logic.
     */
    private static void applyUserSpecificLogic(LocalEvent event, Long userId, EventAction action) {
        // Add user context to metadata
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        if (userId != null) {
            props.put("created_by_user", String.valueOf(userId));
            props.put("user_action_performed", action.name());

            // User-specific business rules could be added here
            // For example, checking user permissions, quotas, etc.
        }

        event.setCustomProperties(props);
        Log.d(TAG, "User-specific logic applied for user: " + userId + ", action: " + action);
    }

    /**
     * Preserve ToolbarAction metadata for backward compatibility.
     */
    private static void preserveToolbarActionMetadata(LocalEvent event, ToolbarAction action) {
        Map<String, String> props = event.getCustomProperties();
        if (props == null) props = new HashMap<>();

        // Preserve original ToolbarAction information
        props.put(PROP_TOOLBAR_SOURCE, action.name());
        props.put("original_ui_action", action.name());
        props.put("migration_version", "toolbar_to_eventaction_v1");

        event.setCustomProperties(props);
        Log.d(TAG, "ToolbarAction metadata preserved for backward compatibility: " + action);
    }

    // ==================== ENHANCED VALIDATION & UTILITY ====================

    /**
     * Enhanced validation using EventAction business rules.
     */
    private static void validateEventConfiguration(LocalEvent event, EventAction action) {
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

        // Enhanced: EventAction-specific validation
        if (action != null) {
            // Validate EventType consistency
            if (event.getEventType() != action.getMappedEventType()) {
                Log.w(TAG, "Event type " + event.getEventType() + " does not match EventAction " + action + " expected type " + action.getMappedEventType());
            }

            // Validate all-day setting consistency
            if (event.isAllDay() != action.isDefaultAllDay()) {
                Log.w(TAG, "All-day setting may not be optimal for EventAction: " + action);
            }

            // Validate date constraints
            if (event.getStartTime() != null) {
                LocalDate eventDate = event.getStartTime().toLocalDate();
                if (!action.canPerformOnDate(eventDate)) {
                    throw new IllegalStateException("EventAction " + action + " cannot be performed on date " + eventDate);
                }
            }

            // Validate duration constraints
            if (event.getStartTime() != null && event.getEndTime() != null) {
                long durationDays = java.time.temporal.ChronoUnit.DAYS.between(
                        event.getStartTime().toLocalDate(),
                        event.getEndTime().toLocalDate()
                ) + 1;

                if (durationDays > action.getMaximumDurationDays()) {
                    throw new IllegalStateException("Event duration " + durationDays + " days exceeds maximum " + action.getMaximumDurationDays() + " for action " + action);
                }
            }
        }

        Log.d(TAG, "Enhanced event configuration validation completed for EventAction: " + action);
    }

    /**
     * Generate enhanced event ID with EventAction context.
     */
    private static String generateEventId(EventAction action, LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String actionStr = action.name().toLowerCase();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        return String.format("eventaction_%s_%s_%s", actionStr, dateStr, randomSuffix);
    }

    // ==================== ENHANCED QUERY & ANALYSIS METHODS ====================

    /**
     * Enhanced check if LocalEvent was created via EventAction.
     */
    public static boolean isEventActionCreatedEvent(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;

        // Check for EventAction metadata
        String eventAction = event.getCustomProperties().get(PROP_EVENT_ACTION);
        if (eventAction != null) return true;

        // Check for business rules applied flag
        return "true".equals(event.getCustomProperties().get(PROP_BUSINESS_RULES_APPLIED));
    }

    /**
     * Enhanced check if LocalEvent was created via quick action (backward compatibility).
     */
    public static boolean isQuickCreatedEvent(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;
        return "true".equals(event.getCustomProperties().get(PROP_QUICK_CREATED));
    }

    /**
     * Get EventAction that created this event.
     */
    public static EventAction getCreatorEventAction(LocalEvent event) {
        if (event.getCustomProperties() == null) return null;

        String actionName = event.getCustomProperties().get(PROP_EVENT_ACTION);
        if (actionName == null) return null;

        try {
            return EventAction.valueOf(actionName);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid EventAction name in metadata: " + actionName);
            return null;
        }
    }

    /**
     * Get ToolbarAction that created this event (backward compatibility).
     */
    public static ToolbarAction getCreatorToolbarAction(LocalEvent event) {
        if (event.getCustomProperties() == null) return null;

        String actionName = event.getCustomProperties().get(PROP_TOOLBAR_SOURCE);
        if (actionName == null) return null;

        try {
            return ToolbarAction.valueOf(actionName);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid ToolbarAction name in metadata: " + actionName);
            return null;
        }
    }

    /**
     * Enhanced check if event affects work schedule using EventAction metadata.
     */
    public static boolean affectsWorkSchedule(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;

        // Try EventAction-based metadata first
        String affectsWork = event.getCustomProperties().get(PROP_AFFECTS_WORK);
        if (affectsWork != null) {
            return "true".equals(affectsWork);
        }

        // Fallback to EventAction derivation
        EventAction action = getCreatorEventAction(event);
        if (action != null) {
            return action.affectsWorkSchedule();
        }

        // Final fallback to EventType analysis
        return affectsWorkScheduleLegacy(event.getEventType());
    }

    /**
     * Enhanced check if event requires approval using EventAction metadata.
     */
    public static boolean requiresApproval(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;

        // Try EventAction-based metadata first
        String requiresApproval = event.getCustomProperties().get(PROP_APPROVAL_REQUIRED);
        if (requiresApproval != null) {
            return "true".equals(requiresApproval);
        }

        // Fallback to EventAction derivation
        EventAction action = getCreatorEventAction(event);
        if (action != null) {
            return action.requiresApproval();
        }

        return false;
    }

    /**
     * Get EventAction cost impact factor from metadata.
     */
    public static double getCostImpactFactor(LocalEvent event) {
        if (event.getCustomProperties() == null) return 0.0;

        String costFactor = event.getCustomProperties().get(PROP_COST_IMPACT_FACTOR);
        if (costFactor != null) {
            try {
                return Double.parseDouble(costFactor);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid cost impact factor in metadata: " + costFactor);
            }
        }

        // Fallback to EventAction derivation
        EventAction action = getCreatorEventAction(event);
        if (action != null) {
            return action.getCostImpactFactor();
        }

        return 0.0;
    }

    /**
     * Get comprehensive EventAction metadata summary.
     */
    public static String getEventActionMetadataSummary(LocalEvent event) {
        if (event.getCustomProperties() == null) return "No EventAction metadata";

        Map<String, String> props = event.getCustomProperties();
        StringBuilder summary = new StringBuilder();

        summary.append("EventAction Metadata Summary:\n");
        summary.append("- EventAction: ").append(props.get(PROP_EVENT_ACTION)).append("\n");
        summary.append("- Action Category: ").append(props.get(PROP_ACTION_CATEGORY)).append("\n");
        summary.append("- Business Rules Applied: ").append(props.get(PROP_BUSINESS_RULES_APPLIED)).append("\n");
        summary.append("- Affects Work Schedule: ").append(props.get(PROP_AFFECTS_WORK)).append("\n");
        summary.append("- Requires Approval: ").append(props.get(PROP_APPROVAL_REQUIRED)).append("\n");
        summary.append("- Cost Impact Factor: ").append(props.get(PROP_COST_IMPACT_FACTOR)).append("\n");
        summary.append("- Requires Documentation: ").append(props.get(PROP_REQUIRES_DOCUMENTATION)).append("\n");
        summary.append("- Manager Approval Required: ").append(props.get(PROP_MANAGER_APPROVAL_REQUIRED)).append("\n");
        summary.append("- Min Advance Notice: ").append(props.get(PROP_MINIMUM_ADVANCE_NOTICE)).append(" days\n");
        summary.append("- Max Duration: ").append(props.get(PROP_MAXIMUM_DURATION)).append(" days\n");

        // Backward compatibility info
        summary.append("- Original ToolbarAction: ").append(props.get(PROP_TOOLBAR_SOURCE)).append("\n");
        summary.append("- Action Version: ").append(props.get(PROP_ACTION_VERSION)).append("\n");

        return summary.toString();
    }

    /**
     * Get recommended title for EventAction and date.
     *
     * @param action EventAction for the event
     * @param date Date of the event
     * @return Recommended title string
     */
    public static String getRecommendedTitle(EventAction action, LocalDate date) {
        if (action == null) return "Evento";

        String baseTitle = action.getDisplayName();

        // Add date context for some actions
        switch (action) {
            case VACATION:
                return "Ferie - " + date.format(DateTimeFormatter.ofPattern("dd/MM"));
            case SICK_LEAVE:
                return "Malattia - " + date.format(DateTimeFormatter.ofPattern("dd/MM"));
            case OVERTIME:
                return "Straordinario - " + date.format(DateTimeFormatter.ofPattern("dd/MM"));
            case EMERGENCY:
                return "URGENTE: " + baseTitle;
            case PLANNED_STOP:
            case UNPLANNED_STOP:
            case MAINTENANCE:
                return baseTitle + " - " + date.format(DateTimeFormatter.ofPattern("dd/MM"));
            default:
                return baseTitle;
        }
    }

    /**
     * Get recommended title for ToolbarAction (backward compatibility).
     */
    public static String getRecommendedTitle(ToolbarAction action, LocalDate date) {
        EventAction eventAction = ToolbarActionBridge.mapToEventAction(action);
        return getRecommendedTitle(eventAction, date);
    }

    // ==================== LEGACY COMPATIBILITY METHODS ====================

    /**
     * Legacy work schedule check for backward compatibility.
     */
    private static boolean affectsWorkScheduleLegacy(EventType eventType) {
        if (eventType == null) return false;

        switch (eventType) {
            case VACATION:
            case SICK_LEAVE:
            case PERSONAL_LEAVE:
            case SPECIAL_LEAVE:
            case SYNDICATE_LEAVE:
            case OVERTIME:
            case TRAINING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Legacy event creation method for backward compatibility.
     * @deprecated Use createEventFromEventAction or createEventFromAction instead
     */
    @Deprecated
    public static LocalEvent createEventFromActionLegacy(ToolbarAction action, LocalDate date, Long userId) {
        Log.w(TAG, "Using deprecated legacy event creation method. Consider migrating to EventAction-based methods.");
        return createEventFromAction(action, date, userId);
    }
}