
// ToolbarAction.java
package net.calvuz.qdue.ui.shared;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import net.calvuz.qdue.R;

/**
 * Enumeration of available toolbar actions for quick day operations
 * Each action includes icon and label resources for UI display
 */
public enum ToolbarAction {
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
}
