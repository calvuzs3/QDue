// ==================== PHASE 4: BACKWARD COMPATIBILITY BRIDGE ====================

package net.calvuz.qdue.ui.core.common.enums;

import net.calvuz.qdue.events.actions.EventAction;
import net.calvuz.qdue.events.models.EventType;

/**
 * ToolbarActionBridge - Backward Compatibility Bridge
 *
 * This class provides backward compatibility while migrating from ToolbarAction
 * to EventAction. It maintains the existing API while delegating business logic
 * to the new EventAction system.
 *
 * @deprecated Use EventAction directly for business logic
 */
@Deprecated
public class ToolbarActionBridge {

    /**
     * Map old ToolbarAction values to new EventAction values.
     */
    public static EventAction mapToEventAction(ToolbarAction toolbarAction) {
        switch (toolbarAction) {
            case FERIE:
                return EventAction.VACATION;
            case MALATTIA:
                return EventAction.SICK_LEAVE;
            case LEGGE_104:
                return EventAction.SPECIAL_LEAVE;
            case PERMESSO:
                return EventAction.PERSONAL_LEAVE;
            case PERMESSO_SINDACALE:
                return EventAction.SYNDICATE_LEAVE;
            case STRAORDINARIO:
                return EventAction.OVERTIME;
            default:
                return EventAction.GENERAL;
        }
    }

    /**
     * Get EventType using the new system.
     */
    public static EventType getMappedEventType(ToolbarAction toolbarAction) {
        return mapToEventAction(toolbarAction).getMappedEventType();
    }

    /**
     * Check if affects work schedule using the new system.
     */
    public static boolean affectsWorkSchedule(ToolbarAction toolbarAction) {
        return mapToEventAction(toolbarAction).affectsWorkSchedule();
    }

    /**
     * Check if requires approval using the new system.
     */
    public static boolean requiresApproval(ToolbarAction toolbarAction) {
        return mapToEventAction(toolbarAction).requiresApproval();
    }
}