package net.calvuz.qdue.core.backup.models;


/**
 * STEP 1: Core Backup System Models
 *
 * Provides data structures for the unified backup system that handles
 * all entities in the QDue application, extending beyond just events.
 */

// ==================== BACKUP PROGRESS TRACKING ====================

/**
 * Backup/restore progress information
 */
public class BackupProgress {
    public String operationType; // "backup", "restore", "validation"
    public String currentStep;
    public int totalSteps;
    public int completedSteps;
    public int percentage;

    public String currentEntity;
    public int entityProgress;
    public int totalEntities;

    public long startTime;
    public long estimatedEndTime;
    public String statusMessage;

    public BackupProgress(String operationType, int totalSteps) {
        this.operationType = operationType;
        this.totalSteps = totalSteps;
        this.completedSteps = 0;
        this.percentage = 0;
        this.startTime = System.currentTimeMillis();
        this.statusMessage = "Starting " + operationType + "...";
    }

    public void updateProgress(int completedSteps, String currentStep) {
        this.completedSteps = completedSteps;
        this.currentStep = currentStep;
        this.percentage = totalSteps > 0 ? (completedSteps * 100) / totalSteps : 0;
        this.statusMessage = currentStep;

        // Estimate completion time
        if (completedSteps > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            long estimatedTotal = (elapsed * totalSteps) / completedSteps;
            this.estimatedEndTime = startTime + estimatedTotal;
        }
    }

    public void updateEntityProgress(String entityName, int entityProgress, int totalEntities) {
        this.currentEntity = entityName;
        this.entityProgress = entityProgress;
        this.totalEntities = totalEntities;
    }

    public boolean isCompleted() {
        return completedSteps >= totalSteps;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getEstimatedRemainingTime() {
        if (estimatedEndTime > 0) {
            return Math.max(0, estimatedEndTime - System.currentTimeMillis());
        }
        return 0;
    }
}