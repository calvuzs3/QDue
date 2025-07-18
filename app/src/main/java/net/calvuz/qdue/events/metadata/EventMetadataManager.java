package net.calvuz.qdue.events.metadata;

import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.actions.EventAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.enums.ToolbarActionBridge;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MIGRATED: Advanced Metadata System for Event Edit Tracking with EventAction Integration
 *
 * This migrated version integrates with the new EventAction system while maintaining
 * backward compatibility with existing ToolbarAction-based metadata. Key improvements:
 *
 * - Enhanced EventAction-based business logic analysis
 * - Backward compatibility with existing ToolbarAction metadata
 * - Advanced turn impact analysis using EventAction capabilities
 * - Improved business rule enforcement and validation
 * - Seamless integration with EventActionManager
 *
 * Migration Benefits:
 * - Pure business logic separation from UI concerns
 * - Enhanced metadata accuracy through EventAction business rules
 * - Better turn impact analysis using EventAction categories
 * - Improved validation and constraint checking
 */
public class EventMetadataManager {

    private static final String TAG = "EventEditMetadata";

    // ==================== ORIGINAL VALUES PRESERVATION ====================

    // Core original metadata (con prefisso original_)
    private static final String ORIGINAL_TEMPLATE_ID = "original_template_id";
    private static final String ORIGINAL_QUICK_EVENT = "original_quick_event";
    private static final String ORIGINAL_UI_CREATED = "original_ui_created";
    private static final String ORIGINAL_SOURCE_ACTION = "original_source_action";
    private static final String ORIGINAL_EVENT_ACTION = "original_event_action"; // NEW: EventAction tracking
    private static final String ORIGINAL_EVENT_TYPE = "original_event_type";
    private static final String ORIGINAL_PRIORITY = "original_priority";
    private static final String ORIGINAL_ALL_DAY = "original_all_day";
    private static final String ORIGINAL_START_TIME = "original_start_time";
    private static final String ORIGINAL_END_TIME = "original_end_time";
    private static final String ORIGINAL_TITLE = "original_title";
    private static final String ORIGINAL_DESCRIPTION = "original_description";
    private static final String ORIGINAL_LOCATION = "original_location";

    // ==================== EDIT SESSION TRACKING ====================

    private static final String EDIT_SESSION_ID = "edit_session_id";
    private static final String EDIT_COUNT = "edit_count";
    private static final String FIRST_EDIT_TIMESTAMP = "first_edit_timestamp";
    private static final String LAST_EDIT_TIMESTAMP = "last_edit_timestamp";
    private static final String EDITED_BY_USER = "edited_by_user";
    private static final String EDIT_SOURCE = "edit_source";
    private static final String EDIT_DEVICE_INFO = "edit_device_info";
    private static final String EDIT_APP_VERSION = "edit_app_version";

    // ==================== ENHANCED EVENTACTION INTEGRATION ====================

    private static final String CURRENT_EVENT_ACTION = "current_event_action";
    private static final String EVENT_ACTION_CHANGED = "event_action_changed";
    private static final String ACTION_CATEGORY_CHANGE = "action_category_change";
    private static final String BUSINESS_LOGIC_VIOLATIONS = "business_logic_violations";
    private static final String ACTION_CONSTRAINTS_CHECKED = "action_constraints_checked";

    // ==================== CHANGE DETECTION ====================

    private static final String FIELDS_CHANGED = "fields_changed";
    private static final String CHANGE_SEVERITY = "change_severity";
    private static final String CHANGE_IMPACT_SCORE = "change_impact_score";
    private static final String VALIDATION_ERRORS = "validation_errors";
    private static final String AUTO_CORRECTIONS = "auto_corrections";
    private static final String USER_OVERRIDES = "user_overrides";

    // ==================== ENHANCED TURN EXCEPTION INTEGRATION ====================

    // EventAction-based turn impact tracking
    private static final String AFFECTS_SHIFT_SCHEDULE = "affects_shift_schedule";
    private static final String ORIGINAL_TURN_IMPACT = "original_turn_impact";
    private static final String CURRENT_TURN_IMPACT = "current_turn_impact";
    private static final String TURN_CHANGE_TYPE = "turn_change_type";
    private static final String REQUIRES_APPROVAL_CHANGE = "requires_approval_change";
    private static final String SHIFT_COVERAGE_NEEDED = "shift_coverage_needed";
    private static final String WORK_SCHEDULE_CONFLICT = "work_schedule_conflict";

    // Enhanced exception status tracking
    private static final String EXCEPTION_STATUS_CHANGE = "exception_status_change";
    private static final String APPROVAL_WORKFLOW_TRIGGERED = "approval_workflow_triggered";
    private static final String ORIGINAL_EXCEPTION_TYPE = "original_exception_type";
    private static final String CURRENT_EXCEPTION_TYPE = "current_exception_type";
    private static final String SHIFT_TYPE_CHANGE = "shift_type_change";
    private static final String ACTION_BUSINESS_RULES_APPLIED = "action_business_rules_applied";

    // ==================== ENHANCED BUSINESS IMPACT ANALYSIS ====================

    private static final String BUSINESS_IMPACT_SCORE = "business_impact_score";
    private static final String PRODUCTION_IMPACT = "production_impact";
    private static final String COST_IMPLICATION = "cost_implication";
    private static final String RESOURCE_IMPACT = "resource_impact";
    private static final String TIMELINE_IMPACT = "timeline_impact";
    private static final String ACTION_COST_FACTOR = "action_cost_factor"; // NEW: from EventAction.getCostImpactFactor()

    // ==================== PERFORMANCE TRACKING ====================

    private static final String EDIT_DURATION = "edit_duration_seconds";
    private static final String SAVE_ATTEMPTS = "save_attempts";
    private static final String FORM_INTERACTIONS = "form_interactions";
    private static final String UI_PERFORMANCE_SCORE = "ui_performance_score";

    // ==================== ENHANCED ENUMS WITH EVENTACTION INTEGRATION ====================

    public enum ChangeSeverity {
        MINOR("minor", 1),
        MODERATE("moderate", 2),
        MAJOR("major", 3),
        CRITICAL("critical", 4);

        private final String value;
        private final int score;

        ChangeSeverity(String value, int score) {
            this.value = value;
            this.score = score;
        }

        public String getValue() { return value; }
        public int getScore() { return score; }
    }

    public enum TurnChangeType {
        NONE("none", 0),
        MINOR("minor", 1),
        MAJOR("major", 3),
        CRITICAL("critical", 5);

        private final String value;
        private final int impact;

        TurnChangeType(String value, int impact) {
            this.value = value;
            this.impact = impact;
        }

        public String getValue() { return value; }
        public int getImpact() { return impact; }
    }

    public enum ProductionImpact {
        NONE("none", 0),
        LOW("low", 1),
        MEDIUM("medium", 3),
        HIGH("high", 5);

        private final String value;
        private final int score;

        ProductionImpact(String value, int score) {
            this.value = value;
            this.score = score;
        }

        public String getValue() { return value; }
        public int getScore() { return score; }
    }

    // ==================== ENHANCED CORE METADATA MANAGEMENT ====================

    /**
     * Enhanced initialization with EventAction integration.
     * Automatically detects and preserves EventAction-based metadata.
     */
    public static LocalEvent initializeEditSessionMetadata(LocalEvent event, Long userId) {
        if (event == null) {
            Log.e(TAG, "Cannot initialize metadata for null event");
            return null;
        }

        try {
            Map<String, String> props = event.getCustomProperties();
            if (props == null) props = new HashMap<>();

            // Enhanced: Preserve original values with EventAction detection
            preserveOriginalValuesWithEventAction(props, event);

            // Initialize edit session
            initializeEditSession(props, userId);

            // Enhanced: Set up EventAction-based turn impact tracking
            initializeEventActionTurnTracking(props, event);

            event.setCustomProperties(props);
            Log.d(TAG, "Enhanced edit session metadata initialized for event: " + event.getId());

            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error initializing enhanced edit session metadata: " + e.getMessage());
            return event;
        }
    }

    /**
     * Enhanced change tracking with EventAction business logic validation.
     */
    public static LocalEvent updateChangeTrackingMetadata(LocalEvent event, LocalEvent originalEvent,
                                                          List<String> changedFields, long editDurationSeconds) {
        if (event == null || originalEvent == null) {
            Log.e(TAG, "Cannot update change tracking for null events");
            return event;
        }

        try {
            Map<String, String> props = event.getCustomProperties();
            if (props == null) props = new HashMap<>();

            // Update change tracking
            updateChangeDetection(props, changedFields, editDurationSeconds);

            // Enhanced: Analyze change severity with EventAction context
            ChangeSeverity severity = analyzeChangeSeverityWithEventAction(event, originalEvent, changedFields);
            props.put(CHANGE_SEVERITY, severity.getValue());
            props.put(CHANGE_IMPACT_SCORE, String.valueOf(calculateEnhancedChangeImpactScore(event, originalEvent)));

            // Enhanced: Update EventAction-based turn impact analysis
            updateEventActionTurnImpactAnalysis(props, event, originalEvent);

            // Enhanced: Update business impact assessment with EventAction data
            updateEnhancedBusinessImpactAssessment(props, event, originalEvent);

            // Enhanced: Validate EventAction business rules
            validateEventActionBusinessRules(props, event, originalEvent);

            event.setCustomProperties(props);
            Log.d(TAG, "Enhanced change tracking metadata updated for event: " + event.getId());

            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error updating enhanced change tracking metadata: " + e.getMessage());
            return event;
        }
    }

    /**
     * Enhanced finalization with EventAction validation and compliance checking.
     */
    public static LocalEvent finalizeEditSessionMetadata(LocalEvent event, int saveAttempts, long totalEditDuration) {
        if (event == null) {
            Log.e(TAG, "Cannot finalize metadata for null event");
            return null;
        }

        try {
            Map<String, String> props = event.getCustomProperties();
            if (props == null) props = new HashMap<>();

            // Finalize edit session
            props.put(LAST_EDIT_TIMESTAMP, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            props.put(SAVE_ATTEMPTS, String.valueOf(saveAttempts));
            props.put(EDIT_DURATION, String.valueOf(totalEditDuration));

            // Update edit count
            int editCount = getEditCount(props) + 1;
            props.put(EDIT_COUNT, String.valueOf(editCount));

            // Calculate enhanced performance score
            int performanceScore = calculateEnhancedUIPerformanceScore(totalEditDuration, saveAttempts, props);
            props.put(UI_PERFORMANCE_SCORE, String.valueOf(performanceScore));

            // Enhanced: Final EventAction compliance check
            performFinalEventActionComplianceCheck(props, event);

            event.setCustomProperties(props);
            Log.d(TAG, "Enhanced edit session metadata finalized for event: " + event.getId());

            return event;

        } catch (Exception e) {
            Log.e(TAG, "Error finalizing enhanced edit session metadata: " + e.getMessage());
            return event;
        }
    }

    // ==================== ENHANCED ORIGINAL VALUES PRESERVATION ====================

    /**
     * Enhanced preservation with EventAction detection and mapping.
     */
    private static void preserveOriginalValuesWithEventAction(Map<String, String> props, LocalEvent event) {
        // Only preserve original values if this is the first edit
        if (!props.containsKey(ORIGINAL_EVENT_TYPE)) {

            // Preserve existing metadata with EventAction detection
            preserveExistingMetadataWithEventAction(props);

            // Preserve core event properties
            if (event.getEventType() != null) {
                props.put(ORIGINAL_EVENT_TYPE, event.getEventType().name());

                // Enhanced: Detect and preserve EventAction if derivable
                EventAction derivedAction = deriveEventActionFromEvent(event);
                if (derivedAction != null) {
                    props.put(ORIGINAL_EVENT_ACTION, derivedAction.name());
                    Log.d(TAG, "Derived original EventAction: " + derivedAction);
                }
            }

            if (event.getPriority() != null) {
                props.put(ORIGINAL_PRIORITY, event.getPriority().name());
            }
            props.put(ORIGINAL_ALL_DAY, String.valueOf(event.isAllDay()));

            if (event.getStartTime() != null) {
                props.put(ORIGINAL_START_TIME, event.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (event.getEndTime() != null) {
                props.put(ORIGINAL_END_TIME, event.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (event.getTitle() != null) {
                props.put(ORIGINAL_TITLE, event.getTitle());
            }
            if (event.getDescription() != null) {
                props.put(ORIGINAL_DESCRIPTION, event.getDescription());
            }
            if (event.getLocation() != null) {
                props.put(ORIGINAL_LOCATION, event.getLocation());
            }

            Log.d(TAG, "Enhanced original values preserved with EventAction detection");
        }
    }

    /**
     * Enhanced preservation with EventAction and ToolbarAction bridge support.
     */
    private static void preserveExistingMetadataWithEventAction(Map<String, String> props) {
        // Preserve quick event metadata
        String templateId = props.get("template_id");
        if (templateId != null) {
            props.put(ORIGINAL_TEMPLATE_ID, templateId);
        }

        String quickEvent = props.get("quick_event");
        if (quickEvent != null) {
            props.put(ORIGINAL_QUICK_EVENT, quickEvent);
        }

        String uiCreated = props.get("ui_created");
        if (uiCreated != null) {
            props.put(ORIGINAL_UI_CREATED, uiCreated);
        }

        // Enhanced: Handle both ToolbarAction and EventAction sources
        String sourceAction = props.get("source_action");
        if (sourceAction != null) {
            props.put(ORIGINAL_SOURCE_ACTION, sourceAction);

            // Enhanced: Try to convert ToolbarAction to EventAction
            try {
                ToolbarAction toolbarAction = ToolbarAction.valueOf(sourceAction);
                EventAction eventAction = ToolbarActionBridge.mapToEventAction(toolbarAction);
                props.put(ORIGINAL_EVENT_ACTION, eventAction.name());
                Log.d(TAG, "Converted ToolbarAction " + sourceAction + " to EventAction " + eventAction);
            } catch (Exception e) {
                Log.d(TAG, "Could not convert source_action to EventAction: " + sourceAction);
            }
        }

        // Enhanced: Check for direct EventAction metadata
        String eventAction = props.get("event_action");
        if (eventAction != null) {
            props.put(ORIGINAL_EVENT_ACTION, eventAction);
        }

        Log.d(TAG, "Enhanced existing metadata preserved with EventAction bridge support");
    }

    // ==================== ENHANCED EVENTACTION TURN IMPACT ANALYSIS ====================

    /**
     * Enhanced turn impact tracking using EventAction business logic.
     */
    private static void initializeEventActionTurnTracking(Map<String, String> props, LocalEvent event) {
        try {
            // Derive EventAction from event or metadata
            EventAction eventAction = deriveEventActionFromEvent(event);

            if (eventAction != null) {
                // Use EventAction business logic for accurate analysis
                boolean affectsShift = eventAction.affectsWorkSchedule();
                props.put(AFFECTS_SHIFT_SCHEDULE, String.valueOf(affectsShift));

                if (affectsShift) {
                    // Enhanced: Use EventAction for turn exception mapping
                    String exceptionType = eventAction.getTurnExceptionTypeName();
                    props.put(ORIGINAL_EXCEPTION_TYPE, exceptionType);
                    props.put(ORIGINAL_TURN_IMPACT, calculateEventActionTurnImpact(eventAction).getValue());

                    // Enhanced: Use EventAction business rules
                    boolean requiresApproval = eventAction.requiresApproval();
                    props.put(REQUIRES_APPROVAL_CHANGE, String.valueOf(requiresApproval));

                    boolean requiresCoverage = eventAction.requiresShiftCoverage();
                    props.put(SHIFT_COVERAGE_NEEDED, String.valueOf(requiresCoverage));

                    // Enhanced: Set action-specific business rules
                    props.put(ACTION_BUSINESS_RULES_APPLIED, "true");
                }

                Log.d(TAG, "EventAction turn impact tracking initialized - action: " + eventAction + ", affects shift: " + affectsShift);
            } else {
                // Fallback to original logic for backward compatibility
                boolean affectsShift = affectsShiftScheduleLegacy(event.getEventType());
                props.put(AFFECTS_SHIFT_SCHEDULE, String.valueOf(affectsShift));
                Log.d(TAG, "Legacy turn impact tracking initialized - affects shift: " + affectsShift);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing EventAction turn tracking: " + e.getMessage());
            // Fallback to legacy method
            boolean affectsShift = affectsShiftScheduleLegacy(event.getEventType());
            props.put(AFFECTS_SHIFT_SCHEDULE, String.valueOf(affectsShift));
        }
    }

    /**
     * Enhanced turn impact analysis using EventAction business logic.
     */
    private static void updateEventActionTurnImpactAnalysis(Map<String, String> props, LocalEvent current, LocalEvent original) {
        try {
            // Derive EventActions for both current and original events
            EventAction currentAction = deriveEventActionFromEvent(current);
            EventAction originalAction = deriveEventActionFromEvent(original);

            // Track EventAction changes
            if (currentAction != originalAction) {
                props.put(EVENT_ACTION_CHANGED, "true");

                if (currentAction != null) {
                    props.put(CURRENT_EVENT_ACTION, currentAction.name());
                }

                // Enhanced: Analyze EventAction category changes
                if (currentAction != null && originalAction != null) {
                    if (currentAction.getCategory() != originalAction.getCategory()) {
                        props.put(ACTION_CATEGORY_CHANGE,
                                originalAction.getCategory().name() + "->" + currentAction.getCategory().name());
                    }
                }

                // Enhanced: Analyze turn change using EventAction business logic
                TurnChangeType changeType = analyzeEventActionTurnChangeType(originalAction, currentAction);
                props.put(TURN_CHANGE_TYPE, changeType.getValue());

                // Enhanced: Update current exception type using EventAction
                if (currentAction != null) {
                    String currentExceptionType = currentAction.getTurnExceptionTypeName();
                    props.put(CURRENT_EXCEPTION_TYPE, currentExceptionType);
                }

                // Enhanced: Check approval workflow changes using EventAction business logic
                boolean needsApprovalChange = checkEventActionApprovalChange(originalAction, currentAction);
                if (needsApprovalChange) {
                    props.put(APPROVAL_WORKFLOW_TRIGGERED, "true");
                }

                // Enhanced: Analyze shift coverage needs using EventAction
                analyzeEventActionShiftCoverageNeeds(props, currentAction, originalAction);
            }

            // Update current turn impact using EventAction
            if (currentAction != null) {
                TurnChangeType currentImpact = calculateEventActionTurnImpact(currentAction);
                props.put(CURRENT_TURN_IMPACT, currentImpact.getValue());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in EventAction turn impact analysis: " + e.getMessage());
            // Fallback to legacy analysis
            updateLegacyTurnImpactAnalysis(props, current, original);
        }
    }

    // ==================== ENHANCED BUSINESS IMPACT ASSESSMENT ====================

    /**
     * Enhanced business impact assessment using EventAction business logic.
     */
    private static void updateEnhancedBusinessImpactAssessment(Map<String, String> props, LocalEvent current, LocalEvent original) {
        try {
            // Calculate enhanced business impact score using EventAction
            int businessScore = calculateBusinessImpactScore(current, original);
            props.put(BUSINESS_IMPACT_SCORE, String.valueOf(businessScore));

            // Enhanced: Assess production impact using EventAction
            ProductionImpact prodImpact = assessEventActionProductionImpact(current, original);
            props.put(PRODUCTION_IMPACT, prodImpact.getValue());

            // Enhanced: Assess cost implications using EventAction cost factors
            String costImplication = assessEventActionCostImplication(current, original);
            props.put(COST_IMPLICATION, costImplication);

            // Enhanced: Add EventAction cost factors
            EventAction currentAction = deriveEventActionFromEvent(current);
            if (currentAction != null) {
                double costFactor = currentAction.getCostImpactFactor();
                props.put(ACTION_COST_FACTOR, String.valueOf(costFactor));
            }

            // Enhanced: Assess resource impact using EventAction
            String resourceImpact = assessEventActionResourceImpact(current, original);
            props.put(RESOURCE_IMPACT, resourceImpact);

        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced business impact assessment: " + e.getMessage());
            // Fallback to legacy assessment
            updateLegacyBusinessImpactAssessment(props, current, original);
        }
    }

    /**
     * Validate EventAction business rules and constraints.
     */
    private static void validateEventActionBusinessRules(Map<String, String> props, LocalEvent current, LocalEvent original) {
        try {
            EventAction currentAction = deriveEventActionFromEvent(current);
            if (currentAction == null) return;

            List<String> violations = new ArrayList<>();

            // Validate date constraints
            if (current.getStartTime() != null) {
                if (!currentAction.canPerformOnDate(current.getStartTime().toLocalDate())) {
                    violations.add("invalid_date_for_action");
                }
            }

            // Validate duration constraints
            if (current.getStartTime() != null && current.getEndTime() != null) {
                long durationDays = ChronoUnit.DAYS.between(
                        current.getStartTime().toLocalDate(),
                        current.getEndTime().toLocalDate()
                ) + 1;

                if (durationDays > currentAction.getMaximumDurationDays()) {
                    violations.add("duration_exceeds_maximum");
                }
            }

            // Validate advance notice requirements
            if (current.getStartTime() != null) {
                long daysUntilEvent = ChronoUnit.DAYS.between(
                        LocalDateTime.now().toLocalDate(),
                        current.getStartTime().toLocalDate()
                );

                if (daysUntilEvent < currentAction.getMinimumAdvanceNoticeDays()) {
                    violations.add("insufficient_advance_notice");
                }
            }

            if (!violations.isEmpty()) {
                props.put(BUSINESS_LOGIC_VIOLATIONS, String.join(",", violations));
                Log.w(TAG, "EventAction business rule violations detected: " + violations);
            }

            props.put(ACTION_CONSTRAINTS_CHECKED, "true");

        } catch (Exception e) {
            Log.e(TAG, "Error validating EventAction business rules: " + e.getMessage());
        }
    }

    /**
     * Perform final EventAction compliance check.
     */
    private static void performFinalEventActionComplianceCheck(Map<String, String> props, LocalEvent event) {
        try {
            EventAction eventAction = deriveEventActionFromEvent(event);
            if (eventAction == null) return;

            // Check final compliance status
            boolean isCompliant = true;
            List<String> complianceIssues = new ArrayList<>();

            // Check if event still matches EventAction expectations
            if (event.getEventType() != eventAction.getMappedEventType()) {
                isCompliant = false;
                complianceIssues.add("event_type_mismatch");
            }

            // Check if business rules are still satisfied
            String violations = props.get(BUSINESS_LOGIC_VIOLATIONS);
            if (violations != null && !violations.isEmpty()) {
                isCompliant = false;
                complianceIssues.add("business_rule_violations");
            }

            props.put("final_compliance_status", isCompliant ? "compliant" : "non_compliant");
            if (!complianceIssues.isEmpty()) {
                props.put("compliance_issues", String.join(",", complianceIssues));
            }

            Log.d(TAG, "Final EventAction compliance check completed - compliant: " + isCompliant);

        } catch (Exception e) {
            Log.e(TAG, "Error in final EventAction compliance check: " + e.getMessage());
        }
    }

    // ==================== ENHANCED HELPER METHODS ====================

    /**
     * Derive EventAction from LocalEvent using multiple strategies.
     */
    public static EventAction deriveEventActionFromEvent(LocalEvent event) {
        if (event == null) return null;

        try {
            // Strategy 1: Check for direct EventAction metadata
            if (event.getCustomProperties() != null) {
                String eventActionStr = event.getCustomProperties().get("event_action");
                if (eventActionStr != null) {
                    return EventAction.valueOf(eventActionStr);
                }

                // Strategy 2: Convert from ToolbarAction metadata
                String sourceActionStr = event.getCustomProperties().get("source_action");
                if (sourceActionStr != null) {
                    try {
                        ToolbarAction toolbarAction = ToolbarAction.valueOf(sourceActionStr);
                        return ToolbarActionBridge.mapToEventAction(toolbarAction);
                    } catch (Exception e) {
                        Log.d(TAG, "Could not convert ToolbarAction to EventAction: " + sourceActionStr);
                    }
                }
            }

            // Strategy 3: Derive from EventType and title
            return deriveEventActionFromEventTypeAndTitle(event.getEventType(), event.getTitle());

        } catch (Exception e) {
            Log.d(TAG, "Could not derive EventAction from event: " + e.getMessage());
            return null;
        }
    }

    /**
     * Derive EventAction from EventType and title analysis.
     */
    private static EventAction deriveEventActionFromEventTypeAndTitle(EventType eventType, String title) {
        if (eventType == null) return null;

        // Direct EventType to EventAction mapping
        switch (eventType) {
            case VACATION: return EventAction.VACATION;
            case SICK_LEAVE: return EventAction.SICK_LEAVE;
            case PERSONAL_LEAVE: return EventAction.PERSONAL_LEAVE;
            case SPECIAL_LEAVE: return EventAction.SPECIAL_LEAVE;
            case SYNDICATE_LEAVE: return EventAction.SYNDICATE_LEAVE;
            case OVERTIME: return EventAction.OVERTIME;
            case SHIFT_SWAP: return EventAction.SHIFT_SWAP;
            case COMPENSATION: return EventAction.COMPENSATION;
            case STOP_PLANNED: return EventAction.PLANNED_STOP;
            case STOP_UNPLANNED: return EventAction.UNPLANNED_STOP;
            case MAINTENANCE: return EventAction.MAINTENANCE;
            case EMERGENCY: return EventAction.EMERGENCY;
            case TRAINING: return EventAction.TRAINING;
            case MEETING: return EventAction.MEETING;
            default: return EventAction.GENERAL;
        }
    }

    /**
     * Calculate EventAction-based turn impact.
     */
    private static TurnChangeType calculateEventActionTurnImpact(EventAction action) {
        if (action == null) return TurnChangeType.NONE;

        // Use EventAction category for impact calculation
        switch (action.getCategory()) {
            case ABSENCE:
                if (action.isUrgentByNature()) {
                    return TurnChangeType.CRITICAL; // Emergency absences
                } else {
                    return TurnChangeType.MAJOR; // Planned absences
                }
            case WORK_ADJUSTMENT:
                return TurnChangeType.MAJOR; // Overtime, shift swaps
            case PRODUCTION:
                if (action.affectsProduction()) {
                    return TurnChangeType.CRITICAL; // Production stops
                } else {
                    return TurnChangeType.MAJOR; // Maintenance
                }
            case DEVELOPMENT:
                return TurnChangeType.MINOR; // Training, meetings
            default:
                return TurnChangeType.NONE;
        }
    }

    /**
     * Analyze EventAction turn change type.
     */
    private static TurnChangeType analyzeEventActionTurnChangeType(EventAction originalAction, EventAction currentAction) {
        if (originalAction == currentAction) {
            return TurnChangeType.NONE;
        }

        if (originalAction == null || currentAction == null) {
            return TurnChangeType.MAJOR;
        }

        // Check if both actions affect work schedule
        boolean originalAffectsWork = originalAction.affectsWorkSchedule();
        boolean currentAffectsWork = currentAction.affectsWorkSchedule();

        if (originalAffectsWork != currentAffectsWork) {
            return TurnChangeType.CRITICAL; // Work impact change
        }

        if (!originalAffectsWork && !currentAffectsWork) {
            return TurnChangeType.MINOR; // No work impact
        }

        // Both affect work - analyze categories
        if (originalAction.getCategory() != currentAction.getCategory()) {
            return TurnChangeType.CRITICAL; // Different categories
        }

        // Same category - check specific business impact
        if (originalAction.isUrgentByNature() != currentAction.isUrgentByNature()) {
            return TurnChangeType.MAJOR; // Urgency change
        }

        return TurnChangeType.MAJOR; // Same category, different action
    }

    /**
     * Check EventAction approval workflow changes.
     */
    private static boolean checkEventActionApprovalChange(EventAction originalAction, EventAction currentAction) {
        if (originalAction == null || currentAction == null) {
            return currentAction != null && currentAction.requiresApproval();
        }

        return originalAction.requiresApproval() != currentAction.requiresApproval();
    }

    /**
     * Analyze EventAction shift coverage needs.
     */
    private static void analyzeEventActionShiftCoverageNeeds(Map<String, String> props, EventAction currentAction, EventAction originalAction) {
        boolean needsCoverage = false;
        boolean hasConflict = false;

        if (currentAction != null && originalAction != null) {
            // Check if coverage requirements changed
            boolean currentNeedsCoverage = currentAction.requiresShiftCoverage();
            boolean originalNeedsCoverage = originalAction.requiresShiftCoverage();

            needsCoverage = currentNeedsCoverage && !originalNeedsCoverage;

            // Check for work schedule conflicts based on EventAction categories
            hasConflict = detectEventActionWorkScheduleConflict(currentAction, originalAction);
        } else if (currentAction != null) {
            needsCoverage = currentAction.requiresShiftCoverage();
        }

        props.put(SHIFT_COVERAGE_NEEDED, String.valueOf(needsCoverage));
        props.put(WORK_SCHEDULE_CONFLICT, String.valueOf(hasConflict));
    }

    /**
     * Detect work schedule conflicts using EventAction business logic.
     */
    private static boolean detectEventActionWorkScheduleConflict(EventAction currentAction, EventAction originalAction) {
        if (currentAction == null || originalAction == null) return false;

        // Actions in different categories that both affect work schedule create conflicts
        return currentAction.affectsWorkSchedule() &&
                originalAction.affectsWorkSchedule() &&
                currentAction.getCategory() != originalAction.getCategory();
    }

    /**
     * Enhanced change severity analysis with EventAction context.
     */
    private static ChangeSeverity analyzeChangeSeverityWithEventAction(LocalEvent current, LocalEvent original, List<String> changedFields) {
        if (changedFields == null || changedFields.isEmpty()) {
            return ChangeSeverity.MINOR;
        }

        int severityScore = 0;

        // Enhanced: EventAction change analysis
        EventAction currentAction = deriveEventActionFromEvent(current);
        EventAction originalAction = deriveEventActionFromEvent(original);

        if (currentAction != originalAction) {
            severityScore += 4; // EventAction change is significant

            // Critical if category changes
            if (currentAction != null && originalAction != null &&
                    currentAction.getCategory() != originalAction.getCategory()) {
                severityScore += 3;
            }

            // Critical if work schedule impact changes
            if (currentAction != null && originalAction != null &&
                    currentAction.affectsWorkSchedule() != originalAction.affectsWorkSchedule()) {
                severityScore += 3;
            }
        }

        // EventType change analysis (enhanced with EventAction context)
        if (changedFields.contains("eventType")) {
            severityScore += 3;

            // Enhanced: Check EventAction business rule violations
            if (currentAction != null && current.getEventType() != currentAction.getMappedEventType()) {
                severityScore += 2; // Inconsistency between action and type
            }
        }

        // Date/time changes with EventAction context
        if (changedFields.contains("startTime") || changedFields.contains("endTime")) {
            severityScore += 2;

            // Enhanced: Check against EventAction constraints
            if (currentAction != null && current.getStartTime() != null) {
                if (!currentAction.canPerformOnDate(current.getStartTime().toLocalDate())) {
                    severityScore += 2; // Violates action constraints
                }
            }
        }

        // Priority changes with EventAction context
        if (changedFields.contains("priority")) {
            severityScore += 1;

            // Enhanced: Check against EventAction default priority
            if (currentAction != null && current.getPriority() != currentAction.getDefaultPriority()) {
                severityScore += 1; // Deviates from action default
            }
        }

        // All-day toggle
        if (changedFields.contains("allDay")) {
            severityScore += 2;

            // Enhanced: Check against EventAction default
            if (currentAction != null && current.isAllDay() != currentAction.isDefaultAllDay()) {
                severityScore += 1; // Deviates from action default
            }
        }

        // Determine severity
        if (severityScore >= 7) return ChangeSeverity.CRITICAL;
        if (severityScore >= 4) return ChangeSeverity.MAJOR;
        if (severityScore >= 2) return ChangeSeverity.MODERATE;
        return ChangeSeverity.MINOR;
    }

    /**
     * Enhanced change impact score calculation with EventAction factors.
     */
    private static int calculateEnhancedChangeImpactScore(LocalEvent current, LocalEvent original) {
        int score = 0;

        // Enhanced: EventAction impact analysis
        EventAction currentAction = deriveEventActionFromEvent(current);
        EventAction originalAction = deriveEventActionFromEvent(original);

        if (currentAction != originalAction) {
            score += 40; // EventAction change has high impact

            if (currentAction != null && originalAction != null) {
                // Category change impact
                if (currentAction.getCategory() != originalAction.getCategory()) {
                    score += 20;
                }

                // Work schedule impact change
                if (currentAction.affectsWorkSchedule() != originalAction.affectsWorkSchedule()) {
                    score += 15;
                }

                // Approval requirement change
                if (currentAction.requiresApproval() != originalAction.requiresApproval()) {
                    score += 10;
                }

                // Cost factor change
                double costDifference = Math.abs(currentAction.getCostImpactFactor() - originalAction.getCostImpactFactor());
                if (costDifference > 0.5) {
                    score += 10;
                }
            }
        }

        // EventType impact (reduced weight due to EventAction analysis)
        if (current.getEventType() != original.getEventType()) {
            score += 20; // Reduced from 30
        }

        // Time impact with EventAction context
        if (hasTimeChanges(current, original)) {
            score += 15; // Reduced from 20
            if (hasSignificantTimeChanges(current, original)) {
                score += 10;

                // Enhanced: Check if violates EventAction constraints
                if (currentAction != null && violatesEventActionTimeConstraints(current, currentAction)) {
                    score += 5;
                }
            }
        }

        // Priority impact
        if (current.getPriority() != original.getPriority()) {
            score += 8; // Reduced from 10
            if (hasCriticalPriorityChange(current.getPriority(), original.getPriority())) {
                score += 5;
            }
        }

        // All-day toggle impact
        if (current.isAllDay() != original.isAllDay()) {
            score += 10; // Reduced from 15
        }

        return Math.min(score, 100);
    }

    /**
     * Enhanced production impact assessment using EventAction.
     */
    private static ProductionImpact assessEventActionProductionImpact(LocalEvent current, LocalEvent original) {
        EventAction currentAction = deriveEventActionFromEvent(current);
        EventAction originalAction = deriveEventActionFromEvent(original);

        // Enhanced: Use EventAction production impact analysis
        boolean currentAffectsProd = currentAction != null && currentAction.affectsProduction();
        boolean originalAffectsProd = originalAction != null && originalAction.affectsProduction();

        if (currentAffectsProd && originalAffectsProd) {
            // Both affect production - check change significance
            if (hasSignificantTimeChanges(current, original)) {
                return ProductionImpact.HIGH;
            }
            return ProductionImpact.MEDIUM;
        }

        if (currentAffectsProd != originalAffectsProd) {
            // Production status change
            return ProductionImpact.HIGH;
        }

        // Enhanced: Check work schedule impact using EventAction
        boolean currentAffectsWork = currentAction != null && currentAction.affectsWorkSchedule();
        boolean originalAffectsWork = originalAction != null && originalAction.affectsWorkSchedule();

        if (currentAffectsWork || originalAffectsWork) {
            return ProductionImpact.MEDIUM;
        }

        return ProductionImpact.LOW;
    }

    /**
     * Enhanced cost implication assessment using EventAction cost factors.
     */
    private static String assessEventActionCostImplication(LocalEvent current, LocalEvent original) {
        EventAction currentAction = deriveEventActionFromEvent(current);
        EventAction originalAction = deriveEventActionFromEvent(original);

        if (currentAction != null && originalAction != null) {
            double currentCost = currentAction.getCostImpactFactor();
            double originalCost = originalAction.getCostImpactFactor();
            double costDifference = Math.abs(currentCost - originalCost);

            if (costDifference >= 2.0) {
                return "significant";
            } else if (costDifference >= 1.0) {
                return "moderate";
            } else if (costDifference >= 0.5) {
                return "minimal";
            }
        }

        // Fallback to legacy logic
        if (currentAction != null && currentAction.requiresShiftCoverage() &&
                (originalAction == null || !originalAction.requiresShiftCoverage())) {
            return "significant";
        }

        if ((currentAction != null && currentAction == EventAction.OVERTIME) ||
                (originalAction != null && originalAction == EventAction.OVERTIME)) {
            return "moderate";
        }

        return "none";
    }

    /**
     * Enhanced resource impact assessment using EventAction.
     */
    private static String assessEventActionResourceImpact(LocalEvent current, LocalEvent original) {
        EventAction currentAction = deriveEventActionFromEvent(current);
        EventAction originalAction = deriveEventActionFromEvent(original);

        // Enhanced: Use EventAction business logic
        if ((currentAction != null && currentAction.affectsProduction()) ||
                (originalAction != null && originalAction.affectsProduction())) {
            return "high_resource_impact";
        }

        if ((currentAction != null && currentAction.requiresShiftCoverage()) ||
                (originalAction != null && originalAction.requiresShiftCoverage())) {
            return "medium_resource_impact";
        }

        if ((currentAction != null && currentAction.affectsWorkSchedule()) ||
                (originalAction != null && originalAction.affectsWorkSchedule())) {
            return "low_resource_impact";
        }

        return "no_resource_impact";
    }

    /**
     * Enhanced UI performance score calculation.
     */
    private static int calculateEnhancedUIPerformanceScore(long editDurationSeconds, int saveAttempts, Map<String, String> props) {
        int score = 100;

        // Penalize long edit sessions
        if (editDurationSeconds > 300) { // > 5 minutes
            score -= 20;
        } else if (editDurationSeconds > 120) { // > 2 minutes
            score -= 10;
        }

        // Penalize multiple save attempts
        score -= (saveAttempts - 1) * 15;

        // Enhanced: Penalize business rule violations
        String violations = props.get(BUSINESS_LOGIC_VIOLATIONS);
        if (violations != null && !violations.isEmpty()) {
            score -= 10;
        }

        // Enhanced: Bonus for EventAction compliance
        String complianceStatus = props.get("final_compliance_status");
        if ("compliant".equals(complianceStatus)) {
            score += 5;
        }

        return Math.max(score, 0);
    }

    // ==================== VALIDATION HELPER METHODS ====================

    /**
     * Check if event violates EventAction time constraints.
     */
    private static boolean violatesEventActionTimeConstraints(LocalEvent event, EventAction action) {
        if (event.getStartTime() == null || action == null) return false;

        // Check minimum advance notice
        long daysUntilEvent = ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                event.getStartTime().toLocalDate()
        );

        return daysUntilEvent < action.getMinimumAdvanceNoticeDays();
    }

    // ==================== LEGACY COMPATIBILITY METHODS ====================

    /**
     * Legacy shift schedule check for backward compatibility.
     */
    private static boolean affectsShiftScheduleLegacy(EventType eventType) {
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
     * Legacy turn impact analysis for fallback.
     */
    private static void updateLegacyTurnImpactAnalysis(Map<String, String> props, LocalEvent current, LocalEvent original) {
        // Fallback to original implementation
        EventType originalType = original.getEventType();
        EventType currentType = current.getEventType();

        if (originalType != currentType) {
            TurnChangeType changeType = analyzeLegacyTurnChangeType(originalType, currentType);
            props.put(TURN_CHANGE_TYPE, changeType.getValue());
        }
    }

    /**
     * Legacy turn change type analysis.
     */
    private static TurnChangeType analyzeLegacyTurnChangeType(EventType originalType, EventType currentType) {
        if (originalType == currentType) {
            return TurnChangeType.NONE;
        }

        boolean originalAffectsWork = affectsShiftScheduleLegacy(originalType);
        boolean currentAffectsWork = affectsShiftScheduleLegacy(currentType);

        if (originalAffectsWork != currentAffectsWork) {
            return TurnChangeType.CRITICAL;
        }

        if (!originalAffectsWork && !currentAffectsWork) {
            return TurnChangeType.MINOR;
        }

        return TurnChangeType.MAJOR;
    }

    /**
     * Legacy business impact assessment for fallback.
     */
    private static void updateLegacyBusinessImpactAssessment(Map<String, String> props, LocalEvent current, LocalEvent original) {
        int businessScore = calculateBusinessImpactScore(current, original);
        props.put(BUSINESS_IMPACT_SCORE, String.valueOf(businessScore));

        ProductionImpact prodImpact = assessProductionImpact(current, original);
        props.put(PRODUCTION_IMPACT, prodImpact.getValue());

        String costImplication = assessCostImplication(current, original);
        props.put(COST_IMPLICATION, costImplication);

        String resourceImpact = assessResourceImpact(current, original);
        props.put(RESOURCE_IMPACT, resourceImpact);
    }

    // ==================== ENHANCED UTILITY QUERY METHODS ====================

    /**
     * Get original EventAction from metadata.
     */
    public static EventAction getOriginalEventAction(LocalEvent event) {
        if (event.getCustomProperties() == null) return null;

        String originalActionStr = event.getCustomProperties().get(ORIGINAL_EVENT_ACTION);
        if (originalActionStr != null) {
            try {
                return EventAction.valueOf(originalActionStr);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid original event action: " + originalActionStr);
            }
        }
        return null;
    }

    /**
     * Get current EventAction from metadata.
     */
    public static EventAction getCurrentEventAction(LocalEvent event) {
        if (event.getCustomProperties() == null) return null;

        String currentActionStr = event.getCustomProperties().get(CURRENT_EVENT_ACTION);
        if (currentActionStr != null) {
            try {
                return EventAction.valueOf(currentActionStr);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid current event action: " + currentActionStr);
            }
        }
        return deriveEventActionFromEvent(event);
    }

    /**
     * Check if EventAction changed during editing.
     */
    public static boolean hasEventActionChanged(LocalEvent event) {
        if (event.getCustomProperties() == null) return false;
        return "true".equals(event.getCustomProperties().get(EVENT_ACTION_CHANGED));
    }

    /**
     * Enhanced edit session summary with EventAction information.
     */
    public static String getEnhancedEditSessionSummary(LocalEvent event) {
        if (event.getCustomProperties() == null) return "No edit session data";

        Map<String, String> props = event.getCustomProperties();
        StringBuilder summary = new StringBuilder();

        summary.append("Enhanced Edit Session Summary:\n");
        summary.append("- Session ID: ").append(props.get(EDIT_SESSION_ID)).append("\n");
        summary.append("- Edit Count: ").append(props.get(EDIT_COUNT)).append("\n");
        summary.append("- Change Severity: ").append(props.get(CHANGE_SEVERITY)).append("\n");
        summary.append("- Business Impact: ").append(props.get(BUSINESS_IMPACT_SCORE)).append("\n");
        summary.append("- Turn Impact: ").append(props.get(CURRENT_TURN_IMPACT)).append("\n");
        summary.append("- Fields Changed: ").append(props.get(FIELDS_CHANGED)).append("\n");

        // Enhanced: EventAction information
        summary.append("- Original EventAction: ").append(props.get(ORIGINAL_EVENT_ACTION)).append("\n");
        summary.append("- Current EventAction: ").append(props.get(CURRENT_EVENT_ACTION)).append("\n");
        summary.append("- EventAction Changed: ").append(props.get(EVENT_ACTION_CHANGED)).append("\n");
        summary.append("- Action Category Change: ").append(props.get(ACTION_CATEGORY_CHANGE)).append("\n");
        summary.append("- Business Logic Violations: ").append(props.get(BUSINESS_LOGIC_VIOLATIONS)).append("\n");
        summary.append("- Final Compliance: ").append(props.get("final_compliance_status")).append("\n");

        return summary.toString();
    }

    // ==================== ADDITIONAL HELPER METHODS ====================

    /**
     * Get EventAction business rule violations from metadata.
     * Returns a list of business rule violations detected during editing.
     *
     * @param event The event to check
     * @return List of violation strings
     */
    public static List<String> getEventActionViolations(LocalEvent event) {
        List<String> violations = new ArrayList<>();

        if (event == null || event.getCustomProperties() == null) {
            return violations;
        }

        try {
            Map<String, String> props = event.getCustomProperties();
            String violationsStr = props.get(BUSINESS_LOGIC_VIOLATIONS);

            if (violationsStr != null && !violationsStr.isEmpty()) {
                String[] violationArray = violationsStr.split(",");
                for (String violation : violationArray) {
                    String trimmed = violation.trim();
                    if (!trimmed.isEmpty()) {
                        violations.add(trimmed);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting EventAction violations: " + e.getMessage());
        }

        return violations;
    }

    /**
     * Check if event has any business rule violations.
     *
     * @param event The event to check
     * @return true if violations exist, false otherwise
     */
    public static boolean hasBusinessRuleViolations(LocalEvent event) {
        return !getEventActionViolations(event).isEmpty();
    }

    /**
     * Get the edit impact summary as a readable string.
     *
     * @param event The event to analyze
     * @return Formatted summary string
     */
    public static String getEditImpactSummary(LocalEvent event) {
        if (event == null) {
            return "No event data available";
        }

        StringBuilder summary = new StringBuilder();

        try {
            // Basic impact information
            ChangeSeverity severity = getChangeSeverity(event);
            int businessImpact = getBusinessImpactScore(event);
            boolean requiresApproval = requiresApprovalDueToChanges(event);
            boolean wasQuickEvent = wasOriginallyQuickEvent(event);

            summary.append("📊 Edit Impact Summary:\n");
            summary.append("• Severity: ").append(severity.getValue().toUpperCase()).append("\n");
            summary.append("• Business Impact: ").append(businessImpact).append("/10\n");
            summary.append("• Requires Approval: ").append(requiresApproval ? "Yes" : "No").append("\n");
            summary.append("• Originally Quick Event: ").append(wasQuickEvent ? "Yes" : "No").append("\n");

            // EventAction information
            EventAction originalAction = getOriginalEventAction(event);
            EventAction currentAction = getCurrentEventAction(event);

            if (originalAction != null || currentAction != null) {
                summary.append("\n🎯 EventAction Analysis:\n");
                summary.append("• Original: ").append(originalAction != null ? originalAction.getDisplayName() : "None").append("\n");
                summary.append("• Current: ").append(currentAction != null ? currentAction.getDisplayName() : "None").append("\n");
                summary.append("• Changed: ").append(hasEventActionChanged(event) ? "Yes" : "No").append("\n");
            }

            // Violations
            List<String> violations = getEventActionViolations(event);
            if (!violations.isEmpty()) {
                summary.append("\n⚠️ Business Rule Violations:\n");
                for (String violation : violations) {
                    summary.append("• ").append(violation.replace("_", " ").toUpperCase()).append("\n");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating edit impact summary: " + e.getMessage());
            summary.append("Error generating summary: ").append(e.getMessage());
        }

        return summary.toString();
    }

    /**
     * Get a quick status indicator for the edit session.
     *
     * @param event The event to check
     * @return Status string (e.g., "🟢 Low Impact", "🟡 Moderate Impact", "🔴 High Impact")
     */
    public static String getEditStatusIndicator(LocalEvent event) {
        try {
            ChangeSeverity severity = getChangeSeverity(event);
            int businessImpact = getBusinessImpactScore(event);
            boolean requiresApproval = requiresApprovalDueToChanges(event);

            if (severity == ChangeSeverity.CRITICAL || businessImpact >= 8 || requiresApproval) {
                return "🔴 High Impact";
            } else if (severity == ChangeSeverity.MAJOR || businessImpact >= 5) {
                return "🟡 Moderate Impact";
            } else if (severity == ChangeSeverity.MODERATE || businessImpact >= 2) {
                return "🟠 Minor Impact";
            } else {
                return "🟢 Low Impact";
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting edit status indicator: " + e.getMessage());
            return "❓ Unknown Impact";
        }
    }

    // ==================== REMAINING HELPER METHODS ====================
    // (Include all the remaining helper methods from the original implementation)

    private static void initializeEditSession(Map<String, String> props, Long userId) {
        String sessionId = generateEditSessionId();
        props.put(EDIT_SESSION_ID, sessionId);
        props.put(FIRST_EDIT_TIMESTAMP, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.put(EDIT_SOURCE, "manual_edit");

        if (userId != null) {
            props.put(EDITED_BY_USER, String.valueOf(userId));
        }

        props.put(EDIT_DEVICE_INFO, "android");
        props.put(EDIT_APP_VERSION, "1.0");

        Log.d(TAG, "Edit session initialized with ID: " + sessionId);
    }

    private static String generateEditSessionId() {
        return "edit_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int getEditCount(Map<String, String> props) {
        String countStr = props.get(EDIT_COUNT);
        if (countStr != null) {
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid edit count format: " + countStr);
            }
        }
        return 0;
    }

    private static void updateChangeDetection(Map<String, String> props, List<String> changedFields, long editDuration) {
        if (changedFields != null && !changedFields.isEmpty()) {
            props.put(FIELDS_CHANGED, String.join(",", changedFields));
        }

        props.put(LAST_EDIT_TIMESTAMP, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        int interactions = getFormInteractions(props) + (changedFields != null ? changedFields.size() : 0);
        props.put(FORM_INTERACTIONS, String.valueOf(interactions));
    }

    private static int getFormInteractions(Map<String, String> props) {
        String interactionsStr = props.get(FORM_INTERACTIONS);
        if (interactionsStr != null) {
            try {
                return Integer.parseInt(interactionsStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid form interactions format: " + interactionsStr);
            }
        }
        return 0;
    }

    private static boolean hasTimeChanges(LocalEvent current, LocalEvent original) {
        if (current.getStartTime() == null || original.getStartTime() == null) {
            return current.getStartTime() != original.getStartTime();
        }

        if (current.getEndTime() == null || original.getEndTime() == null) {
            return current.getEndTime() != original.getEndTime();
        }

        return !current.getStartTime().equals(original.getStartTime()) ||
                !current.getEndTime().equals(original.getEndTime());
    }

    private static boolean hasSignificantTimeChanges(LocalEvent current, LocalEvent original) {
        if (!hasTimeChanges(current, original)) {
            return false;
        }

        if (current.getStartTime() != null && original.getStartTime() != null) {
            long hoursDiff = Math.abs(ChronoUnit.HOURS.between(original.getStartTime(), current.getStartTime()));
            if (hoursDiff > 1) {
                return true;
            }
        }

        if (current.getEndTime() != null && original.getEndTime() != null) {
            long hoursDiff = Math.abs(ChronoUnit.HOURS.between(original.getEndTime(), current.getEndTime()));
            if (hoursDiff > 1) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasCriticalPriorityChange(EventPriority current, EventPriority original) {
        if (current == null || original == null) return false;
        return current == EventPriority.URGENT || original == EventPriority.URGENT;
    }

    // Legacy methods for compatibility
    private static int calculateBusinessImpactScore(LocalEvent current, LocalEvent original) {
        // Original implementation for backward compatibility
        return 0; // Placeholder - implement original logic
    }

    private static ProductionImpact assessProductionImpact(LocalEvent current, LocalEvent original) {
        // Original implementation for backward compatibility
        return ProductionImpact.LOW; // Placeholder
    }

    private static String assessCostImplication(LocalEvent current, LocalEvent original) {
        // Original implementation for backward compatibility
        return "none"; // Placeholder
    }

    private static String assessResourceImpact(LocalEvent current, LocalEvent original) {
        // Original implementation for backward compatibility
        return "no_resource_impact"; // Placeholder
    }

    // ==================== MISSING UTILITY QUERY METHODS FOR EVENTEDITMETADATAMANAGER ====================

    /**
     * Check if event requires approval due to changes made during editing.
     * Analyzes various change factors to determine if approval workflow should be triggered.
     *
     * @param event The event to check
     * @return true if approval is required due to changes, false otherwise
     */
    public static boolean requiresApprovalDueToChanges(LocalEvent event) {
        if (event == null || event.getCustomProperties() == null) {
            return false;
        }

        try {
            Map<String, String> props = event.getCustomProperties();

            // Check if approval workflow was explicitly triggered
            String approvalTriggered = props.get(APPROVAL_WORKFLOW_TRIGGERED);
            if ("true".equals(approvalTriggered)) {
                return true;
            }

            // Check if EventAction change requires approval
            String eventActionChanged = props.get(EVENT_ACTION_CHANGED);
            if ("true".equals(eventActionChanged)) {
                // Get current EventAction and check if it requires approval
                String currentActionStr = props.get(CURRENT_EVENT_ACTION);
                if (currentActionStr != null) {
                    try {
                        EventAction currentAction = EventAction.valueOf(currentActionStr);
                        if (currentAction.requiresApproval()) {
                            return true;
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Invalid current EventAction in metadata: " + currentActionStr);
                    }
                }
            }

            // Check approval requirement change
            String approvalChange = props.get(REQUIRES_APPROVAL_CHANGE);
            if ("true".equals(approvalChange)) {
                return true;
            }

            // Check for high business impact that might require approval
            int businessImpact = getBusinessImpactScore(event);
            if (businessImpact >= 8) { // High impact threshold
                return true;
            }

            // Check for critical change severity
            ChangeSeverity severity = getChangeSeverity(event);
            if (severity == ChangeSeverity.CRITICAL) {
                return true;
            }

            // Check for work schedule conflict that requires approval
            String workConflict = props.get(WORK_SCHEDULE_CONFLICT);
            if ("true".equals(workConflict)) {
                return true;
            }

            // Check for business logic violations that require approval
            String violations = props.get(BUSINESS_LOGIC_VIOLATIONS);
            if (violations != null && !violations.isEmpty()) {
                String[] violationArray = violations.split(",");
                for (String violation : violationArray) {
                    if (violation.trim().equals("insufficient_advance_notice") ||
                            violation.trim().equals("duration_exceeds_maximum")) {
                        return true; // These violations typically require approval override
                    }
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if approval required due to changes: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get business impact score from metadata (0-10 scale).
     * Returns a comprehensive score based on various business impact factors.
     *
     * @param event The event to check
     * @return Business impact score (0-10), where 10 is maximum impact
     */
    public static int getBusinessImpactScore(LocalEvent event) {
        if (event == null || event.getCustomProperties() == null) {
            return 0;
        }

        try {
            Map<String, String> props = event.getCustomProperties();

            // Try to get stored business impact score first
            String scoreStr = props.get(BUSINESS_IMPACT_SCORE);
            if (scoreStr != null && !scoreStr.isEmpty()) {
                try {
                    return Integer.parseInt(scoreStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid business impact score format: " + scoreStr);
                }
            }

            // Calculate business impact score if not stored
            return calculateBusinessImpactScore(event, props);

        } catch (Exception e) {
            Log.e(TAG, "Error getting business impact score: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calculate business impact score based on event properties and metadata.
     */
    private static int calculateBusinessImpactScore(LocalEvent event, Map<String, String> props) {
        int score = 0;

        try {
            // EventAction impact (0-4 points)
            String currentActionStr = props.get(CURRENT_EVENT_ACTION);
            if (currentActionStr != null) {
                try {
                    EventAction action = EventAction.valueOf(currentActionStr);
                    if (action.affectsProduction()) {
                        score += 3;
                    } else if (action.affectsWorkSchedule()) {
                        score += 2;
                    }

                    if (action.requiresApproval()) {
                        score += 1;
                    }
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Invalid EventAction in metadata: " + currentActionStr);
                }
            }

            // EventType impact (0-3 points)
            if (event.getEventType() != null) {
                switch (event.getEventType()) {
                    case EMERGENCY:
                    case STOP_UNPLANNED:
                        score += 3;
                        break;
                    case MAINTENANCE:
                    case STOP_PLANNED:
                    case OVERTIME:
                        score += 2;
                        break;
                    case VACATION:
                    case SICK_LEAVE:
                    case TRAINING:
                        score += 1;
                        break;
                }
            }

            // Priority impact (0-2 points)
            if (event.getPriority() != null) {
                switch (event.getPriority()) {
                    case URGENT:
                        score += 2;
                        break;
                    case HIGH:
                        score += 1;
                        break;
                }
            }

            // Change severity impact (0-1 point)
            String severityStr = props.get(CHANGE_SEVERITY);
            if ("critical".equals(severityStr)) {
                score += 1;
            }

            return Math.min(score, 10); // Cap at 10

        } catch (Exception e) {
            Log.e(TAG, "Error calculating business impact score: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get change severity level from metadata.
     * Returns the severity of changes made during the edit session.
     *
     * @param event The event to check
     * @return ChangeSeverity enum value
     */
    public static ChangeSeverity getChangeSeverity(LocalEvent event) {
        if (event == null || event.getCustomProperties() == null) {
            return ChangeSeverity.MINOR;
        }

        try {
            Map<String, String> props = event.getCustomProperties();
            String severityStr = props.get(CHANGE_SEVERITY);

            if (severityStr != null && !severityStr.isEmpty()) {
                for (ChangeSeverity severity : ChangeSeverity.values()) {
                    if (severity.getValue().equals(severityStr)) {
                        return severity;
                    }
                }
            }

            // Calculate severity if not stored
            return calculateChangeSeverity(event, props);

        } catch (Exception e) {
            Log.e(TAG, "Error getting change severity: " + e.getMessage());
            return ChangeSeverity.MINOR;
        }
    }

    /**
     * Calculate change severity based on metadata and event properties.
     */
    private static ChangeSeverity calculateChangeSeverity(LocalEvent event, Map<String, String> props) {
        try {
            int severityScore = 0;

            // EventAction change impact
            String eventActionChanged = props.get(EVENT_ACTION_CHANGED);
            if ("true".equals(eventActionChanged)) {
                severityScore += 3;

                // Category change adds more severity
                String categoryChange = props.get(ACTION_CATEGORY_CHANGE);
                if (categoryChange != null && !categoryChange.isEmpty()) {
                    severityScore += 2;
                }
            }

            // Fields changed impact
            String fieldsChanged = props.get(FIELDS_CHANGED);
            if (fieldsChanged != null && !fieldsChanged.isEmpty()) {
                String[] fields = fieldsChanged.split(",");
                severityScore += fields.length / 2; // Each 2 fields add 1 point

                // Critical field changes
                for (String field : fields) {
                    if ("eventType".equals(field.trim()) || "startTime".equals(field.trim()) ||
                            "endTime".equals(field.trim()) || "allDay".equals(field.trim())) {
                        severityScore += 1;
                    }
                }
            }

            // Business logic violations impact
            String violations = props.get(BUSINESS_LOGIC_VIOLATIONS);
            if (violations != null && !violations.isEmpty()) {
                severityScore += 2;
            }

            // Work schedule conflict impact
            String workConflict = props.get(WORK_SCHEDULE_CONFLICT);
            if ("true".equals(workConflict)) {
                severityScore += 2;
            }

            // Turn change type impact
            String turnChangeType = props.get(TURN_CHANGE_TYPE);
            if ("critical".equals(turnChangeType)) {
                severityScore += 3;
            } else if ("major".equals(turnChangeType)) {
                severityScore += 2;
            } else if ("minor".equals(turnChangeType)) {
                severityScore += 1;
            }

            // Determine severity level
            if (severityScore >= 8) return ChangeSeverity.CRITICAL;
            if (severityScore >= 5) return ChangeSeverity.MAJOR;
            if (severityScore >= 2) return ChangeSeverity.MODERATE;
            return ChangeSeverity.MINOR;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating change severity: " + e.getMessage());
            return ChangeSeverity.MINOR;
        }
    }

    /**
     * Check if event was originally created via quick event system.
     * Analyzes metadata to determine if the event originated from quick event creation.
     *
     * @param event The event to check
     * @return true if originally created as quick event, false otherwise
     */
    public static boolean wasOriginallyQuickEvent(LocalEvent event) {
        if (event == null || event.getCustomProperties() == null) {
            return false;
        }

        try {
            Map<String, String> props = event.getCustomProperties();

            // Check original quick event flag
            String originalQuickEvent = props.get(ORIGINAL_QUICK_EVENT);
            if ("true".equals(originalQuickEvent)) {
                return true;
            }

            // Check current quick event flag (for backward compatibility)
            String quickEvent = props.get("quick_event");
            if ("true".equals(quickEvent)) {
                return true;
            }

            // Check for quick_created flag
            String quickCreated = props.get("quick_created");
            if ("true".equals(quickCreated)) {
                return true;
            }

            // Check for original template ID (indicates quick event)
            String originalTemplateId = props.get(ORIGINAL_TEMPLATE_ID);
            if (originalTemplateId != null && !originalTemplateId.isEmpty()) {
                return true;
            }

            // Check for template_id (for backward compatibility)
            String templateId = props.get("template_id");
            if (templateId != null && !templateId.isEmpty()) {
                return true;
            }

            // Check for original source action (indicates toolbar/quick creation)
            String originalSourceAction = props.get(ORIGINAL_SOURCE_ACTION);
            if (originalSourceAction != null && !originalSourceAction.isEmpty()) {
                return true;
            }

            // Check for source_action (for backward compatibility)
            String sourceAction = props.get("source_action");
            if (sourceAction != null && !sourceAction.isEmpty()) {
                return true;
            }

            // Check for original EventAction (indicates EventAction-based creation)
            String originalEventAction = props.get(ORIGINAL_EVENT_ACTION);
            if (originalEventAction != null && !originalEventAction.isEmpty()) {
                return true;
            }

            // Check for event_action (for backward compatibility)
            String eventAction = props.get("event_action");
            if (eventAction != null && !eventAction.isEmpty()) {
                return true;
            }

            // Check for creation timestamp and creator type
            String creatorType = props.get("creator_type");
            if ("quick_event_logic_adapter".equals(creatorType) ||
                    "quick_event_template".equals(creatorType) ||
                    "toolbar_action".equals(creatorType)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if originally quick event: " + e.getMessage());
            return false;
        }
    }


}