package net.calvuz.qdue.core.domain.events.actions;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.domain.events.models.EventPriority;

import java.text.MessageFormat;

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
        if (reason.contains(getString(QDue.getContext(), R.string.event_conflict_reason_contains_you_cant)) ||
                reason.contains(getString(QDue.getContext(), R.string.event_conflict_reason_contains_exclude)) ||
                reason.contains(getString(QDue.getContext(), R.string.event_conflict_reason_contains_interrupt))) {
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
        if (conflictingAction == null)
            return getString(QDue.getContext(), R.string.event_conflict_tip_verify_manually);

        switch (conflictingAction.getCategory()) {
            case ABSENCE:
                return getString(QDue.getContext(), R.string.event_conflict_tip_choose_another_date_or_change_absence);
            case WORK_ADJUSTMENT:
                return getString(QDue.getContext(), R.string.event_conflict_tip_coordinate_work_adjustment_or_choose_another_date);
            case PRODUCTION:
                return getString(QDue.getContext(), R.string.event_conflict_tip_reprogram_production);
            case DEVELOPMENT:
                return getString(QDue.getContext(), R.string.event_conflict_tip_move_training_or_meeting);
            default:
                return getString(QDue.getContext(), R.string.event_conflict_tip_resolve_manually);
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
            case CRITICAL:
                return "🔴";
            case HIGH:
                return "🟠";
            case MEDIUM:
                return "🟡";
            default:
                return "ℹ️";
        }
    }

    /**
     * Get formatted display string for UI.
     */
    public String getDisplayString() {
        return MessageFormat.format("{0} {1} ({2})\n{3}",
                getDisplayIcon(),
                conflictingEvent.getTitle(),
                conflictingAction.getDisplayName(),
                reason);
    }

    // ==================== ENUM FOR SEVERITY ====================

    public enum ConflictSeverity {
        CRITICAL(R.string.event_conflict_severity_critical),
        HIGH(R.string.event_conflict_severity_high),
        MEDIUM(R.string.event_conflict_severity_medium),
        LOW(R.string.event_conflict_severity_low);

        @StringRes
        private final int displayName;

        ConflictSeverity(@StringRes int displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return getString(QDue.getContext(), displayName);
        }
    }
}