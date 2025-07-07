package net.calvuz.qdue.ui.events.interfaces;

/**
 * 🆕 NEW: Interface for fragments to communicate back handling state
 */
public interface UnsavedChangesProvider {
    boolean hasUnsavedChanges();
    void handleUnsavedChanges(Runnable onProceed, Runnable onCancel);
}