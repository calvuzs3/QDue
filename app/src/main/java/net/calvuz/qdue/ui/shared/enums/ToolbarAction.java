
// ToolbarAction.java
package net.calvuz.qdue.ui.shared.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import net.calvuz.qdue.R;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Enumeration of available toolbar actions for quick day operations
 * Each action includes icon and label resources for UI display
 */
public enum ToolbarAction {
    STRAORDINARIO(R.drawable.ic_rounded_engineering_24, R.string.action_straordinario, "straordinario"),
    FERIE(R.drawable.ic_rounded_beach_access_24, R.string.action_ferie, "ferie"),
    MALATTIA(R.drawable.ic_rounded_local_hospital_24, R.string.action_malattia, "malattia"),
    LEGGE_104(R.drawable.ic_rounded_accessible_24, R.string.action_legge_104, "legge_104"),
    PERMESSO(R.drawable.ic_rounded_schedule_24, R.string.action_permesso, "permesso"),
    PERMESSO_SINDACALE(R.drawable.ic_rounded_clock_loader_40_24, R.string.action_permesso_sindacale, "permesso_sindacale"),
    ADD_EVENT(R.drawable.ic_rounded_add_24, R.string.action_add_event, "add_event"),
    VIEW_EVENTS(R.drawable.ic_rounded_event_list_24, R.string.action_view_events, "view_events");

    @DrawableRes
    private final int iconRes;

    @StringRes
    private final int labelRes;

    private final String eventType;

    ToolbarAction(@DrawableRes int iconRes, @StringRes int labelRes, String eventType) {
        this.iconRes = iconRes;
        this.labelRes = labelRes;
        this.eventType = eventType;
    }

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

    /**
     * Get actions for quick event creation (ferie, malattia, etc.)
     */
    public static ToolbarAction[] getQuickEventActions() {
        return new ToolbarAction[]{FERIE, MALATTIA, LEGGE_104, PERMESSO};
    }

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
