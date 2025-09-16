package net.calvuz.qdue.domain.calendar.enums;

import android.graphics.Color;

import net.calvuz.qdue.domain.common.enums.Priority;

/**
 * Enhanced event types including production stops
 * Names revised for consistency with industrial naming conventions
 */
public enum EventType
{
    // General events
    GENERAL("Generale", Color.GRAY, "📅"),
    MEETING("Riunione", Color.BLUE, "🤝"),
    TRAINING("Formazione", Color.GREEN, "📚"),
    HOLIDAY("Festività", Color.MAGENTA, "🎉"),

    // Production events
    MAINTENANCE("Manutenzione", Color.LTGRAY, "🔧"),
    EMERGENCY("Emergenza", Color.RED, "🚨"),

    // Production stops (revised names for consistency)
    STOP_PLANNED("Fermata Programmata", Color.parseColor("#FF6B35"), "⏸️"),
    STOP_ORDERS("Fermata Carenza Ordini", Color.parseColor("#FF8C42"), "📦"),
    STOP_CASSA("Fermata Cassa Integrazione", Color.parseColor("#FFA62B"), "💼"),
    STOP_UNPLANNED("Non programmata", Color.parseColor("#D32F2F"), "\uD83D\uDEA8"),
    STOP_SHORTAGE("Carenza Ordini", Color.parseColor("#FF6B35"), "📦❌"),

    // Safety and compliance
    SAFETY_DRILL("Prova Sicurezza", Color.parseColor("#96CEB4"), "🛡️"),
    AUDIT("Audit", Color.parseColor("#FFEAA7"), "📋"),

    // ==================== WORK EVENTS (Shift-Related) ====================

    // ✅ DOPO (corretto - include parametri business logic):
    OVERTIME("Straordinario", Color.parseColor("#7B1FA2"), "⏰", false, true),

    // Shift-related events (Turn Exceptions)
    SHIFT_CHANGE("Cambio Turno", Color.parseColor("#4ECDC4"), "🔄"),
    SHIFT_SWAP("Scambio Turno", Color.parseColor("#607D8B"), "🔄", false, true),
    COMPENSATION("Recupero", Color.parseColor("#795548"), "⚖️", false, true),

    // ==================== NEW: USER ABSENCE EVENTS (from TurnException) ====================

    // ✅ NEW: TurnException.VACATION → EventType.VACATION
    VACATION("Ferie", Color.parseColor("#4CAF50"), "🏖️", true, true),

    // ✅ NEW: TurnException.SICK_LEAVE → EventType.SICK_LEAVE
    SICK_LEAVE("Malattia", Color.parseColor("#F44336"), "🏥", true, false),

    // ✅ NEW: TurnException.PERMIT → EventType.PERSONAL_LEAVE
    PERSONAL_LEAVE("Permesso", Color.parseColor("#2196F3"), "📄", true, true),

    // ✅ NEW: TurnException.PERMIT_104 → EventType.SPECIAL_LEAVE
    SPECIAL_LEAVE("Legge 104", Color.parseColor("#9C27B0"), "♿", true, false),

    // ✅ NEW: TurnException.PERMIT_SYNDICATE → EventType.SYNDICATE_LEAVE
    SYNDICATE_LEAVE("Permesso Sindacale", Color.parseColor("#FF9800"), "🏛️", true, true),

    // Custom/imported events
    IMPORTED("Importato", Color.parseColor("#DDA0DD"), "📥"),
    OTHER("Altro", Color.parseColor("#DDA0DD"), "📥");

    private final String displayName;
    private final int color;
    private final String emoji;

    private final boolean affectsWorkSchedule;  // ✅ NEW: from TurnException.affectsWorkSchedule()
    private final boolean requiresApproval;     // ✅ NEW: business logic integration

    /**
     * Enhanced constructor with TurnException business logic
     */
    EventType(String displayName, int color, String emoji, boolean affectsWorkSchedule, boolean requiresApproval) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.affectsWorkSchedule = affectsWorkSchedule;
        this.requiresApproval = requiresApproval;
    }

    EventType(String displayName, int color, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
        this.affectsWorkSchedule = false;
        this.requiresApproval = false;
    }

    public String getDisplayName() {
        return displayName;
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
        return isWorkAbsence() || this == HOLIDAY;
    }

    /**
     * ✅ NEW: Get default priority for this event type
     * Based on business impact and urgency
     */
    public Priority getDefaultPriority() {
        switch (this) {
            case EMERGENCY:
            case STOP_UNPLANNED:
            case SICK_LEAVE:
            case SPECIAL_LEAVE:
                return Priority.HIGH;

            case COMPENSATION:
            case SHIFT_SWAP:
            case TRAINING:
                return Priority.LOW;

            case VACATION:
            case PERSONAL_LEAVE:
            case SYNDICATE_LEAVE:
            case OVERTIME:
                //return Priority.NORMAL;
            default:
                return Priority.NORMAL;
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
                return "PERMIT";
            case SPECIAL_LEAVE:
                return "PERMIT_104";
            case SYNDICATE_LEAVE:
                return "PERMIT_SYNDICATE";
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
                return displayName;
        }
    }

    // ==================== EXISTING CATEGORIZATION METHODS (Enhanced) ====================

    /**
     * Check if this is a production stop event
     */
    public boolean isProductionStop() {
        return this == STOP_PLANNED || this == STOP_ORDERS || this == STOP_CASSA || this == STOP_UNPLANNED || this == STOP_SHORTAGE;
    }

    public boolean isProductionEvent() {
        return this == MAINTENANCE || this == EMERGENCY;
    }

    public boolean isShiftRelatedEvent() {
        return this == SHIFT_CHANGE || this == OVERTIME;
    }
}