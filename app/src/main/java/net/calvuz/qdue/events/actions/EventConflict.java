package net.calvuz.qdue.events.actions;

import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.models.EventPriority;

public class EventConflict {
    private final LocalEvent conflictingEvent;
    private final EventAction conflictingAction;
    private final String reason;

    public EventConflict(LocalEvent event, EventAction action, String reason) {
        this.conflictingEvent = event;
        this.conflictingAction = action;
        this.reason = reason;
    }

    public LocalEvent getConflictingEvent() {
        return conflictingEvent;
    }

    public EventAction getConflictingAction() {
        return conflictingAction;
    }

    public String getReason() {
        return reason;
    }

    // ==================== METODI AGGIUNTIVI ====================

    /**
     * Check if this is a critical conflict that blocks creation.
     */
    public boolean isCritical() {
        // Critical conflicts based on action combinations
        if (conflictingAction == null) return false;

        // Emergency conflicts are always critical
        if (conflictingAction == EventAction.EMERGENCY) {
            return true;
        }

        // High priority events create critical conflicts
        if (conflictingEvent.getPriority() == EventPriority.URGENT) {
            return true;
        }

        // Absence conflicts are usually critical
        if (conflictingAction.getCategory() == EventActionCategory.ABSENCE) {
            return true;
        }

        // Business rule violations are critical
        if (reason.contains("Non puoi") || reason.contains("escludono") || reason.contains("interrompe")) {
            return true;
        }

        return false;
    }

    /**
     * Get conflict severity level.
     */
    public ConflictSeverity getSeverity() {
        if (isCritical()) {
            return ConflictSeverity.CRITICAL;
        }

        // High severity for work-related conflicts
        if (conflictingAction.affectsWorkSchedule()) {
            return ConflictSeverity.HIGH;
        }

        // Medium for other conflicts
        return ConflictSeverity.MEDIUM;
    }

    /**
     * Get suggested resolution for this conflict.
     */
    public String getSuggestedResolution() {
        if (conflictingAction == null) return "Verifica manualmente";

        switch (conflictingAction.getCategory()) {
            case ABSENCE:
                return "Scegli una data diversa o modifica l'assenza esistente";
            case WORK_ADJUSTMENT:
                return "Coordina gli aggiustamenti di turno o scegli un'altra data";
            case PRODUCTION:
                return "Riprogramma una delle attività produttive";
            case DEVELOPMENT:
                return "Sposta la formazione/riunione in un altro momento";
            default:
                return "Risolvi il conflitto manualmente";
        }
    }

    /**
     * Check if this conflict can be automatically resolved.
     */
    public boolean canAutoResolve() {
        // Only non-critical conflicts with low business impact can be auto-resolved
        return !isCritical() &&
                getSeverity() == ConflictSeverity.MEDIUM &&
                conflictingAction.getCategory() == EventActionCategory.DEVELOPMENT;
    }

    /**
     * Get display icon for UI.
     */
    public String getDisplayIcon() {
        switch (getSeverity()) {
            case CRITICAL: return "🔴";
            case HIGH: return "🟠";
            case MEDIUM: return "🟡";
            default: return "ℹ️";
        }
    }

    /**
     * Get formatted display string for UI.
     */
    public String getDisplayString() {
        return getDisplayIcon() + " " +
                conflictingEvent.getTitle() +
                " (" + conflictingAction.getDisplayName() + ")\n" +
                reason;
    }

    // ==================== ENUM FOR SEVERITY ====================

    public enum ConflictSeverity {
        CRITICAL("Critico"),
        HIGH("Alto"),
        MEDIUM("Medio"),
        LOW("Basso");

        private final String displayName;

        ConflictSeverity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}