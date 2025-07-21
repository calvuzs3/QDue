package net.calvuz.qdue.events.actions;

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
        if (conflicts.isEmpty()) return "Nessun conflitto";

        StringBuilder summary = new StringBuilder();
        summary.append("Conflitti rilevati:\n\n");
        for (EventConflict conflict : conflicts) {
            summary.append("⚠\uFE0F ").append(conflict.getConflictingEvent().getTitle())
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
     * Get short summary for UI display.
     */
    public String getShortSummary() {
        if (conflicts.isEmpty()) return "✅ Nessun conflitto";

        int criticalCount = getCriticalConflicts().size();
        if (criticalCount > 0) {
            return "🔴 " + criticalCount + " conflitti critici, " +
                    (conflicts.size() - criticalCount) + " altri";
        } else {
            return "🟡 " + conflicts.size() + " conflitti rilevati";
        }
    }

    /**
     * Check if there are any critical conflicts.
     */
    public boolean hasCriticalConflicts() {
        return conflicts.stream().anyMatch(EventConflict::isCritical);
    }
}