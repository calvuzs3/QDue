package net.calvuz.qdue.events.actions;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

import java.util.List;
import java.util.stream.Collectors;

public class ConflictAnalysis {
    private final List<EventConflict> conflicts;

    public ConflictAnalysis(List<EventConflict> conflicts) {
        this.conflicts = conflicts;
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public List<EventConflict> getConflicts() {
        return conflicts;
    }

    public String getConflictSummary() {
        if (conflicts.isEmpty()) return getString(QDue.getContext(), R.string.event_conflict_analysis_no_conflicts);

        StringBuilder summary = new StringBuilder();
        summary.append(getString(QDue.getContext(), R.string.event_conflict_analysis_conflicts_found));
        for (EventConflict conflict : conflicts) {
            summary.append("⚠️ ").append(conflict.getConflictingEvent().getTitle())
                    .append("\n ➤ ")
                    .append(conflict.getReason()).append("\n\n");
        }
        return summary.toString();
    }

    // ==================== METODI AGGIUNTIVI ====================

    /**
     * Get count of conflicts.
     */
    public int getConflictCount() {
        return conflicts.size();
    }

    /**
     * Get conflicts by severity level.
     */
    public List<EventConflict> getCriticalConflicts() {
        return conflicts.stream()
                .filter(EventConflict::isCritical)
                .collect(Collectors.toList());
    }

    /**
     * Check if there are any critical conflicts.
     */
    public boolean hasCriticalConflicts() {
        return conflicts.stream().anyMatch(EventConflict::isCritical);
    }
}