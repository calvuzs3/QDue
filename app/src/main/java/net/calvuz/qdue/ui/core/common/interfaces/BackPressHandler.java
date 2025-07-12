package net.calvuz.qdue.ui.core.common.interfaces;

/**
 * Unified back press handler interface for Activities, Fragments, and any component
 * that needs custom back button behavior.
 *
 * This interface provides a consistent way to handle back presses across the entire
 * application while maintaining dependency injection compliance.
 *
 * Features:
 * - Single interface for all components (Activity, Fragment, Adapter, etc.)
 * - DI-compliant with clear responsibilities
 * - Testable and mockable
 * - Consistent behavior across the app
 *
 * Usage:
 * - Activities: Implement for custom navigation logic
 * - Fragments: Implement for selection modes, unsaved changes, etc.
 * - Custom components: Implement for component-specific back handling
 */
public interface BackPressHandler {
    /**
     * Handle back press event
     *
     * @return true if the back press was handled and should not be processed further,
     *         false if the back press should be passed to the next handler or default behavior
     */
    boolean onBackPressed();
}