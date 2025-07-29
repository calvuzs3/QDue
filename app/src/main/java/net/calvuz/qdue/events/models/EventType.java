package net.calvuz.qdue.events.models;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import android.graphics.Color;

import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

/**
 * Enhanced event types including production stops
 * Names revised for consistency with industrial naming conventions
 */
public enum EventType {
    // General events
    GENERAL(R.string.event_action_general, Color.GRAY, "📅"),
    MEETING(R.string.event_action_meeting, Color.BLUE, "🤝"),
    TRAINING( R.string.event_action_training, Color.GREEN, "📚"),

    // Production events
    MAINTENANCE(R.string.event_action_maintenance, Color.LTGRAY, "🔧"),
    EMERGENCY(R.string.event_action_emergency, Color.RED, "🚨"),

    // Production stops (revised names for consistency)
    STOP_PLANNED(R.string.event_action_planned_stop, Color.parseColor("#FF6B35"), "⏸️"),
    STOP_UNPLANNED(R.string.event_action_unplanned_stop, Color.parseColor("#D32F2F"), "\uD83D\uDEA8"),
    STOP_SHORTAGE_ORDERS(R.string.event_type_stop_shortage_orders, Color.parseColor("#FF6B35"), "📦❌"),
    STOP_SHORTAGE_RAW_MATERIALS(R.string.event_type_stop_shortage_raw_materials, Color.parseColor("#FF8C42"), "📦"),

    // Safety and compliance
    SAFETY_DRILL(R.string.event_type_saefty_drill, Color.parseColor("#96CEB4"), "🛡️"),
    AUDIT(R.string.event_type_audit, Color.parseColor("#FFEAA7"), "📋"),

    // ==================== WORK EVENTS (Shift-Related) ====================

    OVERTIME(R.string.event_action_overtime, Color.parseColor("#7B1FA2"), "⏰", false, true),
    SHIFT_CHANGE(R.string.event_type_shift_change, Color.parseColor("#4ECDC4"), "🔄"),
    SHIFT_SWAP(R.string.event_action_shift_swap, Color.parseColor("#607D8B"), "🔄", false, true),
    COMPENSATION(R.string.event_action_compensation, Color.parseColor("#795548"), "⚖️", false, true),

    // ==================== USER ABSENCE EVENTS (from TurnException) ====================

    VACATION(R.string.event_action_vacation, Color.parseColor("#4CAF50"), "🏖️", true, true),
    SICK_LEAVE(R.string.event_action_sick_leave, Color.parseColor("#F44336"), "🏥", true, false),
    PERSONAL_LEAVE(R.string.event_action_personal_leave, Color.parseColor("#2196F3"), "📄", true, true),
    SPECIAL_LEAVE(R.string.event_action_special_leave, Color.parseColor("#9C27B0"), "♿", true, false),
    SYNDICATE_LEAVE(R.string.event_action_syndicate_leave, Color.parseColor("#FF9800"), "🏛️", true, true),

    // Custom/imported events
    IMPORTED(R.string.event_type_imported, Color.parseColor("#DDA0DD"), "📥"),
    OTHER(R.string.event_type_other, Color.parseColor("#DDA0DD"), "📥");

    @StringRes
    private final int displayName;
    
    private final int color;
    private final String emoji;

    private final boolean affectsWorkSchedule;
    private final boolean requiresApproval;     // business logic integration

    /**
     * Enhanced constructor with TurnException business logic
     */
    EventType(@StringRes int displayName, int color, String emoji, boolean affectsWorkSchedule, boolean requiresApproval) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.requiresApproval = requiresApproval;
    }

    EventType(@StringRes int displayName, int color, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.affectsWorkSchedule = false;
        this.requiresApproval = false;
    }

    public String getDisplayName() {
        return getString(QDue.getContext(), displayName);
    }

    public int getColor() {
        return color;
    }

    public String getEmoji() {
        return emoji;
    }

    /**
     * Get text color based on background color for readability
     */
    public int getTextColor() {
        // Calculate luminance and return black or white text
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    // ==================== NEW: TurnException BUSINESS LOGIC INTEGRATION ====================

    /**
     * ✅ NEW: Check if this event affects work schedule (from TurnException.affectsWorkSchedule())
     * Events that remove user from normal work shifts
     */
    public boolean affectsWorkSchedule() {
        return affectsWorkSchedule;
    }

    /**
     * ✅ NEW: Check if this event requires approval (business logic)
     * Events that need manager/HR approval before being effective
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /**
     * ✅ NEW: Check if this is a work absence event (user-managed leave)
     * Combines work schedule impact with user leave types
     */
    public boolean isWorkAbsence() {
        return this == VACATION ||
                this == SICK_LEAVE ||
                this == PERSONAL_LEAVE ||
                this == SPECIAL_LEAVE ||
                this == SYNDICATE_LEAVE;
    }

    /**
     * ✅ NEW: Check if this is a user-managed event (created by user via quick actions)
     * Events that users can create themselves via toolbar
     */
    public boolean isUserManaged() {
        return isWorkAbsence() ||
                this == OVERTIME ||
                this == COMPENSATION ||
                this == SHIFT_SWAP;
    }

    /**
     * ✅ NEW: Check if this event should be all-day by default
     * Work absences are typically all-day, work events are timed
     */
    public boolean isDefaultAllDay() {
        return isWorkAbsence();
    }

    /**
     * ✅ NEW: Get default priority for this event type
     * Based on business impact and urgency
     */
    public EventPriority getDefaultPriority() {
        switch (this) {
            case EMERGENCY:
            case STOP_UNPLANNED:
            case SICK_LEAVE:
            case SPECIAL_LEAVE:
                return EventPriority.HIGH;

            case COMPENSATION:
            case SHIFT_SWAP:
            case TRAINING:
                return EventPriority.LOW;

            case VACATION:
            case PERSONAL_LEAVE:
            case SYNDICATE_LEAVE:
            case OVERTIME:
                //return EventPriority.NORMAL;
            default:
                return EventPriority.NORMAL;
        }
    }

    // ==================== NEW: TOOLBAR ACTION MAPPING ====================

    /**
     * ✅ NEW: Get EventType for ToolbarAction (migration from TurnException)
     * Maps toolbar actions to appropriate EventTypes
     */
    public static EventType fromToolbarAction(String toolbarActionName) {
        switch (toolbarActionName.toUpperCase()) {
            case "VACATION":
                return VACATION;
            case "SICK_LEAVE":
                return SICK_LEAVE;
            case "OVERTIME":
                return OVERTIME;
            case "PERSONAL_LEAVE":
                return PERSONAL_LEAVE;
            case "SPECIAL_LEAVE":
                return SPECIAL_LEAVE;
            case "SYNDICATE_LEAVE":
                return SYNDICATE_LEAVE;
            default:
                return GENERAL;
        }
    }

    /**
     * ✅ NEW: Get TurnException.ExceptionType equivalent (for backward compatibility)
     * Used during migration period to maintain logic compatibility
     */
    public String getTurnExceptionTypeName() {
        switch (this) {
            case VACATION:
                return "VACATION";
            case SICK_LEAVE:
                return "SICK_LEAVE";
            case OVERTIME:
                return "OVERTIME";
            case PERSONAL_LEAVE:
                return "PERSONAL_LEAVE";
            case SPECIAL_LEAVE:
                return "SPECIAL_LEAVE";
            case SYNDICATE_LEAVE:
                return "SYNDICATE_LEAVE";
            case TRAINING:
                return "TRAINING";
            case COMPENSATION:
                return "COMPENSATION";
            case SHIFT_SWAP:
                return "SHIFT_SWAP";
            case EMERGENCY:
                return "EMERGENCY";
            default:
                return "OTHER";
        }
    }

    // ==================== NEW: QUICK EVENT TEMPLATE SUPPORT ====================

    /**
     * ✅ NEW: Get custom properties template for this event type
     * Pre-fills customProperties based on event type characteristics
     */
    public java.util.Map<String, String> getDefaultCustomProperties() {
        java.util.Map<String, String> props = new java.util.HashMap<>();

        // Common properties
        props.put("event_type_category", getCategory());
        props.put("affects_work_schedule", String.valueOf(affectsWorkSchedule()));
        props.put("requires_approval", String.valueOf(requiresApproval()));
        props.put("turn_exception_type", getTurnExceptionTypeName());
        props.put("default_all_day", String.valueOf(isDefaultAllDay()));
        props.put("user_managed", String.valueOf(isUserManaged()));

        // Type-specific properties
        if (isWorkAbsence()) {
            props.put("absence_type", name().toLowerCase());
            props.put("leave_category", "user_absence");
        }

        if (this == OVERTIME) {
            props.put("overtime_type", "scheduled");
            props.put("shift_extension", "true");
        }

        if (this == SICK_LEAVE) {
            props.put("medical_certificate", "pending");
        }

        if (this == SPECIAL_LEAVE) {
            props.put("protected_leave", "true");
            props.put("law_reference", "104/92");
        }

        return props;
    }

    /**
     * ✅ NEW: Get event category for grouping
     */
    public String getCategory() {
        if (isWorkAbsence()) return "user_absence";
        if (isProductionStop()) return "production_stop";
        if (isProductionEvent()) return "production";
        if (isShiftRelatedEvent()) return "shift_management";
        return "general";
    }

    // ==================== NEW: TIME DEFAULTS FOR QUICK EVENTS ====================

    /**
     * ✅ NEW: Get default start time for timed events
     * Smart defaults based on event type
     */
    public java.time.LocalTime getDefaultStartTime() {
        switch (this) {
            case OVERTIME:
                return java.time.LocalTime.of(5, 0);  // Early morning shift
            case PERSONAL_LEAVE:
                return java.time.LocalTime.of(12, 0);  // Standard office hours
            case TRAINING:
                return java.time.LocalTime.of(5, 0);  // Training sessions
            case MEETING:
                return java.time.LocalTime.of(10, 0); // Meeting time
            default:
                return java.time.LocalTime.of(5, 0);  // Default work start
        }
    }

    /**
     * ✅ NEW: Get default end time for timed events
     * Smart defaults based on event type and typical duration
     */
    public java.time.LocalTime getDefaultEndTime() {
        switch (this) {
            case OVERTIME:
                return java.time.LocalTime.of(13, 0); // 8-hour shift
            case PERSONAL_LEAVE:
                return java.time.LocalTime.of(13, 0); // Full work day
            case TRAINING:
                return java.time.LocalTime.of(13, 0); // Full day training
            case MEETING:
                return java.time.LocalTime.of(11, 0); // 1-hour meeting
            default:
                return java.time.LocalTime.of(13, 0); // Default work end
        }
    }

    // ==================== VALIDATION & UTILITY ====================

    /**
     * ✅ NEW: Validate if this event type is compatible with specified parameters
     */
    public boolean isValidConfiguration(boolean allDay, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        // All-day events don't need time validation
        if (allDay) return true;

        // Timed events need valid time range
        if (startTime == null || endTime == null) return false;
        if (!startTime.isBefore(endTime)) return false;

        // Work absences should typically be all-day
        if (isWorkAbsence() && !allDay) {
            // Allow partial day leave but warn
            return true; // Could be partial day leave
        }

        return true;
    }

    /**
     * ✅ NEW: Get display description for event type
     * Used in UI for tooltips, help text, etc.
     */
    public String getDescription() {
        switch (this) {
            case VACATION:
                return "Giorno di ferie programmato";
            case SICK_LEAVE:
                return "Assenza per malattia";
            case OVERTIME:
                return "Turno straordinario programmato";
            case PERSONAL_LEAVE:
                return "Permesso personale";
            case SPECIAL_LEAVE:
                return "Permesso ai sensi della Legge 104/92";
            case SYNDICATE_LEAVE:
                return "Permesso per attività sindacale";
            case COMPENSATION:
                return "Recupero ore o giorni";
            case SHIFT_SWAP:
                return "Cambio di turno con collega";
            default:
                return getDisplayName();
        }
    }

    // ==================== EXISTING CATEGORIZATION METHODS (Enhanced) ====================

    /**
     * Check if this is a production stop event
     */
    public boolean isProductionStop() {
        return this == STOP_PLANNED || this == STOP_SHORTAGE_RAW_MATERIALS || this == STOP_UNPLANNED || this == STOP_SHORTAGE_ORDERS;
    }

    public boolean isProductionEvent() {
        return this == MAINTENANCE || this == EMERGENCY;
    }

    public boolean isShiftRelatedEvent() {
        return this == SHIFT_CHANGE || this == OVERTIME;
    }
}