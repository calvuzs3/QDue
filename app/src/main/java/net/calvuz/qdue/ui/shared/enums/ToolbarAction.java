
// ToolbarAction.java
package net.calvuz.qdue.ui.shared.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.EventType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Enumeration of available toolbar actions for quick day operations
 * Each action includes icon and label resources for UI display
 */
public enum ToolbarAction {

    // ==================== USER ABSENCE ACTIONS (Work Schedule Impact) ====================

    /**
     * ✅ ENHANCED: Ferie - Maps to EventType.VACATION
     * User vacation days, typically requires approval
     */
    FERIE(
            R.drawable.ic_rounded_beach_access_24,
            R.string.action_ferie,
            "ferie",
            EventType.VACATION,
            "Crea evento ferie per la data selezionata"
    ),

    /**
     * ✅ ENHANCED: Malattia - Maps to EventType.SICK_LEAVE
     * Sick leave, high priority, usually no approval required
     */
    MALATTIA(
            R.drawable.ic_rounded_local_hospital_24,
            R.string.action_malattia,
            "malattia",
            EventType.SICK_LEAVE,
            "Crea evento malattia per la data selezionata"
    ),

    /**
     * ✅ ENHANCED: Legge 104 - Maps to EventType.SPECIAL_LEAVE
     * Special protected leave, no approval required
     */
    LEGGE_104(
            R.drawable.ic_rounded_accessible_24,
            R.string.action_legge_104,
            "legge_104",
            EventType.SPECIAL_LEAVE,
            "Crea permesso Legge 104 per la data selezionata"
    ),

    /**
     * ✅ ENHANCED: Permesso - Maps to EventType.PERSONAL_LEAVE
     * Personal leave, typically requires approval
     */
    PERMESSO(
            R.drawable.ic_rounded_schedule_24,
            R.string.action_permesso,
            "permesso",
            EventType.PERSONAL_LEAVE,
            "Crea permesso personale per la data selezionata"
    ),

    /**
     * ✅ ENHANCED: Permesso Sindacale - Maps to EventType.SYNDICATE_LEAVE
     * Union/syndicate leave, typically requires approval
     */
    PERMESSO_SINDACALE(
            R.drawable.ic_rounded_clock_loader_40_24,
            R.string.action_permesso_sindacale,
            "permesso_sindacale",
            EventType.SYNDICATE_LEAVE,
            "Crea permesso sindacale per la data selezionata"
    ),

    // ==================== WORK EVENTS (Shift-Related) ====================

    /**
     * ✅ NEW: Straordinario - Maps to EventType.OVERTIME
     * Overtime work, scheduled extra shift
     */
    STRAORDINARIO(
            R.drawable.ic_rounded_engineering_24,
            R.string.action_straordinario,
            "straordinario",
            EventType.OVERTIME,
            "Crea turno straordinario per la data selezionata"
    ),

    // ==================== GENERAL ACTIONS (Non-EventType) ====================

    /**
     * ✅ ENHANCED: Add Event - Opens full event editor
     * General event creation, maps to EventType.GENERAL by default
     */
    ADD_EVENT(
            R.drawable.ic_rounded_add_24,
            R.string.action_add_event,
            "add_event",
            EventType.GENERAL,
            "Apri editor eventi per creare un nuovo evento"
    ),

    /**
     * ✅ ENHANCED: View Events - Shows events list/dialog
     * Navigation action, no direct EventType mapping
     */
    VIEW_EVENTS(
            R.drawable.ic_rounded_event_list_24,
            R.string.action_view_events,
            "view_events",
            null, // No direct EventType mapping for navigation actions
            "Visualizza tutti gli eventi per la data selezionata"
    );

    // ==================== ENUM PROPERTIES ====================

//    STRAORDINARIO(R.drawable.ic_rounded_engineering_24, R.string.action_straordinario, "straordinario"),
//    FERIE(R.drawable.ic_rounded_beach_access_24, R.string.action_ferie, "ferie"),
//    MALATTIA(R.drawable.ic_rounded_local_hospital_24, R.string.action_malattia, "malattia"),
//    LEGGE_104(R.drawable.ic_rounded_accessible_24, R.string.action_legge_104, "legge_104"),
//    PERMESSO(R.drawable.ic_rounded_schedule_24, R.string.action_permesso, "permesso"),
//    PERMESSO_SINDACALE(R.drawable.ic_rounded_clock_loader_40_24, R.string.action_permesso_sindacale, "permesso_sindacale"),
//    FORMAZIONE(R.drawable.ic_rounded_self_improvement_24, R.string.action_formazione, "formazione"),
//    EMERGENZA(R.drawable.ic_rounded_emergency_home_24, R.string.action_emergenza, "emergenza"),
//    ADD_EVENT(R.drawable.ic_rounded_add_24, R.string.action_add_event, "add_event"),
//    VIEW_EVENTS(R.drawable.ic_rounded_event_list_24, R.string.action_view_events, "view_events");

    @DrawableRes
    private final int iconRes;

    @StringRes
    private final int labelRes;

    private final String eventType; // Legacy field for backward compatibility

    private final EventType mappedEventType; // ✅ NEW: Direct EventType mapping

    private final String description; // ✅ NEW: Action description for accessibility/tooltips


    /**
     * Enhanced constructor with EventType integration
     */
    ToolbarAction(@DrawableRes int iconRes, @StringRes int labelRes, String eventType,
                  EventType mappedEventType, String description) {
        this.iconRes = iconRes;
        this.labelRes = labelRes;
        this.eventType = eventType;
        this.mappedEventType = mappedEventType;
        this.description = description;
    }

    ToolbarAction(@DrawableRes int iconRes, @StringRes int labelRes, String eventType) {
        this.iconRes = iconRes;
        this.labelRes = labelRes;
        this.eventType = eventType;
        this.mappedEventType = null;
        this.description = null;
    }


    // ==================== EXISTING GETTERS (Backward Compatibility) ====================

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    public String getEventType() {
        return eventType;
    }



    // ==================== NEW: EventType INTEGRATION ====================

    /**
     * ✅ NEW: Get mapped EventType for this action
     * Returns the EventType that should be created when this action is triggered
     */
    public EventType getMappedEventType() {
        return mappedEventType;
    }

    /**
     * ✅ NEW: Get action description
     * Used for accessibility, tooltips, and help text
     */
    public String getDescription() {
        return description;
    }

    /**
     * ✅ NEW: Check if this action creates a work absence event
     * Delegates to EventType logic
     */
    public boolean createsWorkAbsence() {
        return mappedEventType != null && mappedEventType.isWorkAbsence();
    }

    /**
     * ✅ NEW: Check if this action creates an event requiring approval
     * Delegates to EventType logic
     */
    public boolean requiresApproval() {
        return mappedEventType != null && mappedEventType.requiresApproval();
    }

    /**
     * ✅ NEW: Check if this action affects work schedule
     * Delegates to EventType logic
     */
    public boolean affectsWorkSchedule() {
        return mappedEventType != null && mappedEventType.affectsWorkSchedule();
    }

    /**
     * ✅ NEW: Check if this action should create all-day events by default
     * Delegates to EventType logic
     */
    public boolean isDefaultAllDay() {
        return mappedEventType != null && mappedEventType.isDefaultAllDay();
    }

    /**
     * ✅ NEW: Get display name from mapped EventType
     * Consistent with EventType.getDisplayName()
     */
    public String getEventDisplayName() {
        return mappedEventType != null ? mappedEventType.getDisplayName() : "Evento";
    }

    /**
     * ✅ NEW: Get event color from mapped EventType
     * For consistent UI theming
     */
    public int getEventColor() {
        return mappedEventType != null ? mappedEventType.getColor() : android.graphics.Color.GRAY;
    }

    /**
     * ✅ NEW: Get event emoji from mapped EventType
     * For visual consistency
     */
    public String getEventEmoji() {
        return mappedEventType != null ? mappedEventType.getEmoji() : "📅";
    }

    // ==================== STATIC UTILITY METHODS ====================

    /**
     * ✅ NEW: Check if this is a navigation action (doesn't create events)
     * Used to differentiate between event-creating and navigation actions
     */
    public boolean isNavigationAction() {
        return this == ADD_EVENT || this == VIEW_EVENTS;
    }

    /**
     * ✅ NEW: Check if this is an event creation action
     * Opposite of navigation action
     */
    public boolean isEventCreationAction() {
        return !isNavigationAction() && mappedEventType != null;
    }

    /**
     * ✅ ENHANCED: Get actions for quick event creation (user-managed events)
     * Filters to show only actions that create user-managed events
     */
    public static ToolbarAction[] getQuickEventActions() {
        return new ToolbarAction[]{
                FERIE,
                MALATTIA,
                LEGGE_104,
                PERMESSO,
                PERMESSO_SINDACALE,
                STRAORDINARIO // ✅ NEW: Added STRAORDINARIO
        };
    }

    /**
     * ✅ NEW: Get actions that create work absence events
     * Only absence-related actions
     */
    public static ToolbarAction[] getWorkAbsenceActions() {
        return new ToolbarAction[]{
                FERIE,
                MALATTIA,
                LEGGE_104,
                PERMESSO,
                PERMESSO_SINDACALE
        };
    }

    /**
     * ✅ NEW: Get actions that create work events (not absences)
     * Work-positive actions like overtime
     */
    public static ToolbarAction[] getWorkEventActions() {
        return new ToolbarAction[]{
                STRAORDINARIO
        };
    }

    /**
     * ✅ NEW: Get actions that require approval
     * Business logic filtering
     */
    public static ToolbarAction[] getApprovalRequiredActions() {
        java.util.List<ToolbarAction> approvalActions = new java.util.ArrayList<>();
        for (ToolbarAction action : getQuickEventActions()) {
            if (action.requiresApproval()) {
                approvalActions.add(action);
            }
        }
        return approvalActions.toArray(new ToolbarAction[0]);
    }









    /**
     * Get user-friendly event icon
     */
    public static int getEventIcon(ToolbarAction action) {
        return action.iconRes;
    }

    /**
     * Get user-friendly event type name
     */
    public static String getEventTypeName(ToolbarAction action) {
        return switch (action) {
            case STRAORDINARIO -> "Straordinario";
            case FERIE -> "Ferie";
            case MALATTIA -> "Malattia";
            case LEGGE_104 -> "Legge 104";
            case PERMESSO -> "Permesso";
            case PERMESSO_SINDACALE -> "Permesso sindacale";
            case ADD_EVENT -> "Aggiungi Evento";
            case VIEW_EVENTS -> "Visualizza Eventi";
            default -> action.name();
        };
    }

//    /**
//     * Get actions for quick event creation (ferie, malattia, etc.)
//     */
//    public static ToolbarAction[] getQuickEventActions() {
//        return new ToolbarAction[]{FERIE, MALATTIA, LEGGE_104, PERMESSO};
//    }

    /**
     * Get all available actions
     */
    public static ToolbarAction[] getAllActions() {
        return values();
    }

    /**
     * Get default actions
     */
    public static List<ToolbarAction> getDefaultActions() {
        return java.util.Arrays.asList(
                ToolbarAction.STRAORDINARIO,
                ToolbarAction.FERIE,
                ToolbarAction.MALATTIA,
                ToolbarAction.LEGGE_104,
                ToolbarAction.ADD_EVENT
        );
    }

    /**
     * Get actions based on selected dates
     */
    public static List<ToolbarAction> getActionsForDates(Set<LocalDate> selectedDates) {
        if (selectedDates == null || selectedDates.isEmpty()) {
            return getDefaultActions();
        }

        if (selectedDates.size() == 1) {
            // Single date - all actions available
            return getDefaultActions();

        } else {
            // Multiple dates - only bulk actions
            return java.util.Arrays.asList(
                    ToolbarAction.FERIE,
                    ToolbarAction.MALATTIA,
                    ToolbarAction.ADD_EVENT
            );
        }
    }
}
