// ==================== PHASE 1: CORE BUSINESS LOGIC EXTRACTION ====================

package net.calvuz.qdue.events.actions;

import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.EventPriority;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * EventAction - Pure Business Logic for Event Actions
 *
 * This enum contains only business logic and domain rules, completely separated
 * from UI concerns. It represents the core business actions that can be performed
 * on events, regardless of how they are triggered (toolbar, API, automation, etc.).
 *
 * Migration Strategy:
 * - Phase 1: Extract business logic from ToolbarAction
 * - Phase 2: Create mapping layer for backward compatibility
 * - Phase 3: Migrate consumers to use EventAction directly
 * - Phase 4: Deprecate ToolbarAction business methods
 */
public enum EventAction {

    // ==================== ABSENCE ACTIONS ====================

    VACATION("Ferie", EventType.VACATION, EventPriority.NORMAL, true, true, true, false),
    SICK_LEAVE("Malattia", EventType.SICK_LEAVE, EventPriority.HIGH, true, false, true, true),
    PERSONAL_LEAVE("Permesso Personale", EventType.PERSONAL_LEAVE, EventPriority.NORMAL, true, true, true, false),
    SPECIAL_LEAVE("Permesso Legge 104", EventType.SPECIAL_LEAVE, EventPriority.NORMAL, true, true, true, false),
    SYNDICATE_LEAVE("Permesso Sindacale", EventType.SYNDICATE_LEAVE, EventPriority.NORMAL, true, true, true, false),

    // ==================== WORK ADJUSTMENT ACTIONS ====================

    OVERTIME("Straordinario", EventType.OVERTIME, EventPriority.NORMAL, false, true, false, false),
    SHIFT_SWAP("Cambio Turno", EventType.SHIFT_SWAP, EventPriority.NORMAL, false, true, false, false),
    COMPENSATION("Recupero", EventType.COMPENSATION, EventPriority.NORMAL, false, true, false, false),

    // ==================== PRODUCTION ACTIONS ====================

    PLANNED_STOP("Fermata Pianificata", EventType.STOP_PLANNED, EventPriority.HIGH, false, false, false, false),
    UNPLANNED_STOP("Fermata Non Pianificata", EventType.STOP_UNPLANNED, EventPriority.URGENT, false, false, false, true),
    MAINTENANCE("Manutenzione", EventType.MAINTENANCE, EventPriority.HIGH, false, false, false, false),
    EMERGENCY("Emergenza", EventType.EMERGENCY, EventPriority.URGENT, false, false, true, true),

    // ==================== DEVELOPMENT ACTIONS ====================

    TRAINING("Formazione", EventType.TRAINING, EventPriority.NORMAL, false, true, false, false),
    MEETING("Riunione", EventType.MEETING, EventPriority.NORMAL, false, false, false, false),

    // ==================== GENERAL ACTIONS ====================

    GENERAL("Evento Generale", EventType.GENERAL, EventPriority.NORMAL, false, false, false, false);

    // ==================== BUSINESS PROPERTIES ====================

    private final String displayName;
    private final EventType mappedEventType;
    private final EventPriority defaultPriority;
    private final boolean defaultAllDay;
    private final boolean requiresApproval;
    private final boolean affectsWorkSchedule;
    private final boolean isUrgentByNature;

    // ==================== CONSTRUCTOR ====================

    EventAction(String displayName, EventType mappedEventType, EventPriority defaultPriority,
                boolean defaultAllDay, boolean requiresApproval, boolean affectsWorkSchedule,
                boolean isUrgentByNature) {
        this.displayName = displayName;
        this.mappedEventType = mappedEventType;
        this.defaultPriority = defaultPriority;
        this.defaultAllDay = defaultAllDay;
        this.requiresApproval = requiresApproval;
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.isUrgentByNature = isUrgentByNature;
    }

    // ==================== CORE BUSINESS METHODS ====================

    /**
     * Get the EventType that should be created when this action is performed.
     */
    public EventType getMappedEventType() {
        return mappedEventType;
    }

    /**
     * Get the default priority for events created by this action.
     */
    public EventPriority getDefaultPriority() {
        return defaultPriority;
    }

    /**
     * Get the display name for this action.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if events created by this action should be all-day by default.
     */
    public boolean isDefaultAllDay() {
        return defaultAllDay;
    }

    /**
     * Check if this action requires approval workflow.
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /**
     * Check if this action affects work schedule and requires turn management.
     */
    public boolean affectsWorkSchedule() {
        return affectsWorkSchedule;
    }

    /**
     * Check if this action is urgent by nature (emergency situations).
     */
    public boolean isUrgentByNature() {
        return isUrgentByNature;
    }

    // ==================== ADVANCED BUSINESS LOGIC ====================

    /**
     * Check if this action requires shift coverage.
     */
    public boolean requiresShiftCoverage() {
        switch (this) {
            case VACATION:
            case SICK_LEAVE:
            case PERSONAL_LEAVE:
            case SPECIAL_LEAVE:
            case EMERGENCY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if this action creates a work absence.
     */
    public boolean createsWorkAbsence() {
        switch (this) {
            case VACATION:
            case SICK_LEAVE:
            case PERSONAL_LEAVE:
            case SPECIAL_LEAVE:
            case SYNDICATE_LEAVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if this action affects production schedule.
     */
    public boolean affectsProduction() {
        switch (this) {
            case PLANNED_STOP:
            case UNPLANNED_STOP:
            case MAINTENANCE:
            case EMERGENCY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the category of this action for classification purposes.
     */
    public EventActionCategory getCategory() {
        switch (this) {
            case VACATION:
            case SICK_LEAVE:
            case PERSONAL_LEAVE:
            case SPECIAL_LEAVE:
            case SYNDICATE_LEAVE:
                return EventActionCategory.ABSENCE;
            case OVERTIME:
            case SHIFT_SWAP:
            case COMPENSATION:
                return EventActionCategory.WORK_ADJUSTMENT;
            case PLANNED_STOP:
            case UNPLANNED_STOP:
            case MAINTENANCE:
            case EMERGENCY:
                return EventActionCategory.PRODUCTION;
            case TRAINING:
            case MEETING:
                return EventActionCategory.DEVELOPMENT;
            default:
                return EventActionCategory.GENERAL;
        }
    }

    /**
     * Get the TurnException equivalent type for backward compatibility.
     */
    public String getTurnExceptionTypeName() {
        switch (this) {
            case VACATION: return "VACATION";
            case SICK_LEAVE: return "SICK_LEAVE";
            case OVERTIME: return "OVERTIME";
            case PERSONAL_LEAVE: return "PERMIT";
            case SPECIAL_LEAVE: return "PERMIT_104";
            case SYNDICATE_LEAVE: return "PERMIT_SYNDICATE";
            case TRAINING: return "TRAINING";
            case COMPENSATION: return "COMPENSATION";
            case SHIFT_SWAP: return "SHIFT_SWAP";
            case EMERGENCY: return "EMERGENCY";
            default: return "OTHER";
        }
    }

    /**
     * Get default start time for this action (if not all-day).
     */
    public LocalTime getDefaultStartTime() {
        switch (this) {
            case MEETING:
                return LocalTime.of(9, 0);
            case TRAINING:
                return LocalTime.of(8, 30);
            case OVERTIME:
                return LocalTime.of(17, 0);
            default:
                return LocalTime.of(8, 0);
        }
    }

    /**
     * Get default end time for this action (if not all-day).
     */
    public LocalTime getDefaultEndTime() {
        switch (this) {
            case MEETING:
                return LocalTime.of(10, 0);
            case TRAINING:
                return LocalTime.of(17, 0);
            case OVERTIME:
                return LocalTime.of(20, 0);
            default:
                return LocalTime.of(17, 0);
        }
    }

    // ==================== VALIDATION BUSINESS RULES ====================

    /**
     * Check if this action can be performed on the specified date.
     */
    public boolean canPerformOnDate(LocalDate date) {
        if (date == null) return false;

        LocalDate today = LocalDate.now();

        switch (this) {
            case VACATION:
                // Vacation usually requires advance notice (except emergencies)
                return !date.isBefore(today);
            case SICK_LEAVE:
                // Sick leave can be retroactive up to 3 days
                return !date.isBefore(today.minusDays(3));
            case EMERGENCY:
                // Emergency can be on any date
                return true;
            case PLANNED_STOP:
            case MAINTENANCE:
                // Planned activities should be in the future
                return date.isAfter(today);
            default:
                // Most actions can be performed from today onward
                return !date.isBefore(today);
        }
    }

    /**
     * Check if this action can be performed by the specified user.
     */
    public boolean canPerformByUser(Long userId) {
        // TODO: Implement user-specific business rules
        // For now, allow all users to perform all actions
        return userId != null;
    }

    /**
     * Get minimum advance notice required for this action (in days).
     */
    public int getMinimumAdvanceNoticeDays() {
        switch (this) {
            case VACATION:
                return 0; // 7 -> 1 week notice
            case TRAINING:
                return 0; // 3 -> 3 days notice
            case PLANNED_STOP:
            case MAINTENANCE:
                return 1; // 1 day notice
            case SICK_LEAVE:
            case EMERGENCY:
                return 0; // No advance notice required
            default:
                return 0;
        }
    }

    /**
     * Get maximum duration allowed for this action (in days).
     */
    public int getMaximumDurationDays() {
        switch (this) {
            case VACATION:
                return 30; // Maximum 1 month vacation
            case SICK_LEAVE:
                return 180; // Maximum 6 months sick leave
            case TRAINING:
                return 5; // Maximum 1 work week
            case PERSONAL_LEAVE:
            case SPECIAL_LEAVE:
            case SYNDICATE_LEAVE:
                return 1; // Usually single day
            default:
                return 365; // Default maximum 1 year
        }
    }

    // ==================== BUSINESS RULE COMBINATIONS ====================

    /**
     * Check if this action requires documentation/certificates.
     */
    public boolean requiresDocumentation() {
        switch (this) {
            case SICK_LEAVE:
            case SPECIAL_LEAVE:
                return true;
            case VACATION:
                return getMaximumDurationDays() > 5; // Long vacations need documentation
            default:
                return false;
        }
    }

    /**
     * Check if this action affects overtime calculations.
     */
    public boolean affectsOvertimeCalculation() {
        switch (this) {
            case OVERTIME:
            case COMPENSATION:
                return true;
            case VACATION:
            case SICK_LEAVE:
                return true; // Affects calculation negatively
            default:
                return false;
        }
    }

    /**
     * Check if this action requires manager approval specifically.
     */
    public boolean requiresManagerApproval() {
        switch (this) {
            case VACATION:
            case OVERTIME:
            case TRAINING:
                return true;
            case SPECIAL_LEAVE:
                return true; // HR approval
            default:
                return false;
        }
    }

    /**
     * Get cost impact factor for this action (for budgeting purposes).
     */
    public double getCostImpactFactor() {
        switch (this) {
            case OVERTIME:
                return 1.5; // 150% of normal rate
            case VACATION:
            case SICK_LEAVE:
                return -1.0; // Negative impact (paid leave)
            case TRAINING:
                return 0.5; // Additional cost for training
            case EMERGENCY:
                return 2.0; // High cost impact
            default:
                return 0.0; // No direct cost impact
        }
    }
}

// ==================== PHASE 2: ACTION CATEGORY ENUM ====================

