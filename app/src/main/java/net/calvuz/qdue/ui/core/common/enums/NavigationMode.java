package net.calvuz.qdue.ui.core.common.enums;


/**
 * Enum to track current navigation mode for proper handling
 */
public enum NavigationMode {
    PHONE_PORTRAIT,      // BottomNavigation + FAB separato
    TABLET_PORTRAIT,     // NavigationRail + FAB integrato
    LANDSCAPE_SMALL,     // NavigationRail espanso + Extended FAB
    LANDSCAPE_LARGE      // NavigationRail + Drawer
}