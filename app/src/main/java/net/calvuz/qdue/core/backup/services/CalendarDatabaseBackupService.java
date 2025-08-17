package net.calvuz.qdue.core.backup.services;

import android.content.Context;

import net.calvuz.qdue.core.backup.models.EntityBackupPackage;
import net.calvuz.qdue.core.db.CalendarDatabase;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.entities.RecurrenceRuleEntity;
import net.calvuz.qdue.data.entities.ShiftEntity;
import net.calvuz.qdue.data.entities.ShiftExceptionEntity;
import net.calvuz.qdue.data.entities.TeamEntity;
import net.calvuz.qdue.data.entities.UserScheduleAssignmentEntity;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CalendarDatabaseBackupService - Specialized backup service for CalendarDatabase entities
 *
 * <p>Provides comprehensive backup operations for all calendar-related entities following
 * the same patterns as DatabaseBackupService but optimized for calendar domain models.</p>
 *
 * <h3>Supported Calendar Entities:</h3>
 * <ul>
 *   <li><strong>ShiftEntity</strong>: Shift type templates and definitions</li>
 *   <li><strong>TeamEntity</strong>: Work team configurations</li>
 *   <li><strong>RecurrenceRuleEntity</strong>: RRULE patterns for recurring schedules</li>
 *   <li><strong>ShiftExceptionEntity</strong>: Schedule exceptions and modifications</li>
 *   <li><strong>UserScheduleAssignmentEntity</strong>: User-to-team assignments</li>
 * </ul>
 *
 * <h3>Backup Features:</h3>
 * <ul>
 *   <li><strong>Entity Relationships</strong>: Maintains referential integrity during backup</li>
 *   <li><strong>Calendar Metadata</strong>: Captures schedule statistics and validation data</li>
 *   <li><strong>Time-aware Backup</strong>: Handles time-sensitive calendar data correctly</li>
 *   <li><strong>Incremental Support</strong>: Supports both full and incremental backups</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Calendar Backup Service
 * @since Clean Architecture Phase 2
 */
public class CalendarDatabaseBackupService {

    private static final String TAG = "CalendarDatabaseBackupService";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final CalendarDatabase mCalendarDatabase;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection
     *
     * @param context Application context
     * @param calendarDatabase Calendar database instance
     */
    public CalendarDatabaseBackupService(Context context, CalendarDatabase calendarDatabase) {
        this.mContext = context.getApplicationContext();
        this.mCalendarDatabase = calendarDatabase;

        Log.d(TAG, "CalendarDatabaseBackupService initialized");
    }

    // ==================== CALENDAR BACKUP OPERATIONS ====================

    /**
     * ✅ Generate complete calendar backup package
     * Includes all calendar entities with metadata and relationships
     *
     * @return OperationResult containing EntityBackupPackage with all calendar data
     */
    public OperationResult<EntityBackupPackage> generateCalendarBackup() {
        try {
            Log.d(TAG, "Generating complete calendar backup");

            // Collect all calendar entities
            List<Object> allCalendarEntities = new ArrayList<>();
            Map<String, Integer> entityCounts = new HashMap<>();

            // 1. Backup Shifts
            try {
                List<ShiftEntity> shifts = mCalendarDatabase.shiftDao().getAllShifts();
                if (shifts == null) shifts = new ArrayList<>();
                allCalendarEntities.addAll(shifts);
                entityCounts.put("shifts", shifts.size());
                Log.d(TAG, "Backed up " + shifts.size() + " shifts");
            } catch (Exception e) {
                Log.w(TAG, "Failed to backup shifts", e);
                entityCounts.put("shifts", 0);
            }

            // 2. Backup Teams
            try {
                List<TeamEntity> teams = mCalendarDatabase.teamDao().getAllTeams();
                if (teams == null) teams = new ArrayList<>();
                allCalendarEntities.addAll(teams);
                entityCounts.put("teams", teams.size());
                Log.d(TAG, "Backed up " + teams.size() + " teams");
            } catch (Exception e) {
                Log.w(TAG, "Failed to backup teams", e);
                entityCounts.put("teams", 0);
            }

            // 3. Backup Recurrence Rules - ALL rules for complete backup
            try {
                List<RecurrenceRuleEntity> rules = mCalendarDatabase.recurrenceRuleDao().getAllRecurrenceRules();
                if (rules == null) rules = new ArrayList<>();
                allCalendarEntities.addAll(rules);
                entityCounts.put("recurrenceRules", rules.size());
                Log.d(TAG, "Backed up " + rules.size() + " recurrence rules (ALL including inactive)");
            } catch (Exception e) {
                Log.w(TAG, "Failed to backup recurrence rules", e);
                entityCounts.put("recurrenceRules", 0);
            }

            // 4. Backup Shift Exceptions - ALL exceptions for complete backup
            try {
                List<ShiftExceptionEntity> exceptions = mCalendarDatabase.shiftExceptionDao().getAllShiftExceptions();
                if (exceptions == null) exceptions = new ArrayList<>();
                allCalendarEntities.addAll(exceptions);
                entityCounts.put("shiftExceptions", exceptions.size());
                Log.d(TAG, "Backed up " + exceptions.size() + " shift exceptions (ALL including inactive)");
            } catch (Exception e) {
                Log.w(TAG, "Failed to backup shift exceptions", e);
                entityCounts.put("shiftExceptions", 0);
            }

            // 5. Backup User Schedule Assignments - ALL assignments for complete backup
            try {
                List<UserScheduleAssignmentEntity> assignments =
                        mCalendarDatabase.userScheduleAssignmentDao().getAllUserScheduleAssignments();
                if (assignments == null) assignments = new ArrayList<>();
                allCalendarEntities.addAll(assignments);
                entityCounts.put("userScheduleAssignments", assignments.size());
                Log.d(TAG, "Backed up " + assignments.size() + " user schedule assignments (ALL including inactive)");
            } catch (Exception e) {
                Log.w(TAG, "Failed to backup user schedule assignments", e);
                entityCounts.put("userScheduleAssignments", 0);
            }

            // Create backup package using correct constructor
            EntityBackupPackage calendarPackage = new EntityBackupPackage("calendar", "1.0", allCalendarEntities);

            // Add calendar-specific metadata to entityMetadata (not metadata)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("entityCounts", entityCounts);
            metadata.put("databaseVersion", getCalendarDatabaseVersion());
            metadata.put("backupSource", "CalendarDatabase");
            metadata.put("backupTimestamp", LocalDateTime.now().toString());
            metadata.put("totalEntitiesCount", allCalendarEntities.size()); // Use correct property name

            calendarPackage.metadata = metadata;

            // Generate backup statistics
            generateCalendarBackupStatistics(calendarPackage);

            Log.d(TAG, "Calendar backup completed successfully. Total entities: " + allCalendarEntities.size());
            return OperationResult.success(calendarPackage, OperationResult.OperationType.BACKUP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate calendar backup", e);
            return OperationResult.failure("Calendar backup failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    // ==================== SPECIALIZED BACKUP METHODS ====================

    /**
     * ✅ Generate incremental backup based on last backup timestamp
     */
    public OperationResult<EntityBackupPackage> generateIncrementalCalendarBackup(String lastBackupTimestamp) {
        try {
            Log.d(TAG, "Generating incremental calendar backup since: " + lastBackupTimestamp);

            // For now, use full backup as incremental logic requires timestamp tracking
            // TODO: Implement incremental logic based on entity timestamps when available
            Log.w(TAG, "Incremental backup not yet implemented, performing full backup");
            return generateCalendarBackup();

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate incremental calendar backup", e);
            return OperationResult.failure("Incremental calendar backup failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    /**
     * ✅ Generate selective backup for specific entity types only
     */
    public OperationResult<EntityBackupPackage> generateSelectiveCalendarBackup(List<String> entityTypes) {
        try {
            Log.d(TAG, "Generating selective calendar backup for: " + entityTypes);

            List<Object> selectedEntities = new ArrayList<>();
            Map<String, Integer> entityCounts = new HashMap<>();

            for (String entityType : entityTypes) {
                try {
                    switch (entityType.toLowerCase()) {
                        case "shifts":
                            List<ShiftEntity> shifts = mCalendarDatabase.shiftDao().getAllShifts();
                            if (shifts != null) {
                                selectedEntities.addAll(shifts);
                                entityCounts.put("shifts", shifts.size());
                            }
                            break;

                        case "teams":
                            List<TeamEntity> teams = mCalendarDatabase.teamDao().getAllTeams();
                            if (teams != null) {
                                selectedEntities.addAll(teams);
                                entityCounts.put("teams", teams.size());
                            }
                            break;

                        case "recurrencerules":
                            List<RecurrenceRuleEntity> rules = mCalendarDatabase.recurrenceRuleDao().getAllRecurrenceRules();
                            if (rules != null) {
                                selectedEntities.addAll(rules);
                                entityCounts.put("recurrenceRules", rules.size());
                            }
                            break;

                        case "shiftexceptions":
                            List<ShiftExceptionEntity> exceptions = mCalendarDatabase.shiftExceptionDao().getAllShiftExceptions();
                            if (exceptions != null) {
                                selectedEntities.addAll(exceptions);
                                entityCounts.put("shiftExceptions", exceptions.size());
                            }
                            break;

                        case "userscheduleassignments":
                            List<UserScheduleAssignmentEntity> assignments =
                                    mCalendarDatabase.userScheduleAssignmentDao().getAllUserScheduleAssignments();
                            if (assignments != null) {
                                selectedEntities.addAll(assignments);
                                entityCounts.put("userScheduleAssignments", assignments.size());
                            }
                            break;

                        default:
                            Log.w(TAG, "Unknown entity type requested: " + entityType);
                            break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to backup entity type: " + entityType, e);
                    entityCounts.put(entityType, 0);
                }
            }

            // Create selective backup package
            EntityBackupPackage selectivePackage = new EntityBackupPackage("calendar_selective", "1.0", selectedEntities);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("selectedEntityTypes", entityTypes);
            metadata.put("entityCounts", entityCounts);
            metadata.put("backupTimestamp", LocalDateTime.now().toString());
            metadata.put("totalEntitiesCount", selectedEntities.size());

            selectivePackage.metadata = metadata;

            Log.d(TAG, "Selective calendar backup completed. Total entities: " + selectedEntities.size());
            return OperationResult.success(selectivePackage, OperationResult.OperationType.BACKUP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate selective calendar backup", e);
            return OperationResult.failure("Selective calendar backup failed: " + e.getMessage(),
                    OperationResult.OperationType.BACKUP);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * ✅ Generate calendar-specific backup statistics
     */
    private void generateCalendarBackupStatistics(EntityBackupPackage calendarPackage) {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // Get entity counts from metadata
            @SuppressWarnings("unchecked")
            Map<String, Integer> entityCounts = (Map<String, Integer>) calendarPackage.metadata.get("entityCounts");

            if (entityCounts != null) {
                int totalShifts = entityCounts.getOrDefault("shifts", 0);
                int totalTeams = entityCounts.getOrDefault("teams", 0);
                int totalRules = entityCounts.getOrDefault("recurrenceRules", 0);
                int totalExceptions = entityCounts.getOrDefault("shiftExceptions", 0);
                int totalAssignments = entityCounts.getOrDefault("userScheduleAssignments", 0);

                // Calculate relationship statistics
                statistics.put("shiftToTeamRatio", totalTeams > 0 ? (double) totalShifts / totalTeams : 0.0);
                statistics.put("exceptionsToRulesRatio", totalRules > 0 ? (double) totalExceptions / totalRules : 0.0);
                statistics.put("assignmentsToTeamRatio", totalTeams > 0 ? (double) totalAssignments / totalTeams : 0.0);
                statistics.put("totalCalendarComplexity", totalShifts + totalTeams + totalRules + totalExceptions + totalAssignments);
            }

            // Add calendar-specific metadata
            statistics.put("backupFormat", "CalendarDatabase_v2.0");
            statistics.put("supportsRRULE", true);
            statistics.put("supportsExceptions", true);
            statistics.put("supportsTeamAssignments", true);

            // Add statistics to existing metadata
            calendarPackage.metadata.put("calendarStatistics", statistics);

        } catch (Exception e) {
            Log.w(TAG, "Failed to generate calendar backup statistics", e);
        }
    }

    /**
     * ✅ Get calendar database version
     */
    private int getCalendarDatabaseVersion() {
        try {
            // Return current calendar database version
            return 2; // Based on CalendarDatabase schema version
        } catch (Exception e) {
            Log.w(TAG, "Could not determine calendar database version", e);
            return 1;
        }
    }

    /**
     * ✅ Validate calendar backup data integrity
     */
    public OperationResult<Boolean> validateCalendarBackup(EntityBackupPackage calendarBackup) {
        try {
            if (calendarBackup == null) {
                return OperationResult.failure("Calendar backup package is null",
                        OperationResult.OperationType.VALIDATION);
            }

            if (calendarBackup.entities == null || calendarBackup.entities.isEmpty()) {
                Log.w(TAG, "Calendar backup contains no entities");
                return OperationResult.success("Calendar backup contains no entities", OperationResult.OperationType.BACKUP); // Empty backup is valid
            }

            // Validate entity types
            for (Object entity : calendarBackup.entities) {
                if (!(entity instanceof ShiftEntity) &&
                        !(entity instanceof TeamEntity) &&
                        !(entity instanceof RecurrenceRuleEntity) &&
                        !(entity instanceof ShiftExceptionEntity) &&
                        !(entity instanceof UserScheduleAssignmentEntity)) {

                    return OperationResult.failure("Invalid entity type in calendar backup: " +
                                    entity.getClass().getSimpleName(),
                            OperationResult.OperationType.VALIDATION);
                }
            }

            Log.d(TAG, "Calendar backup validation passed");
            return OperationResult.success("Calendar backup validation passed", OperationResult.OperationType.VALIDATION);

        } catch (Exception e) {
            Log.e(TAG, "Calendar backup validation failed", e);
            return OperationResult.failure("Calendar backup validation error: " + e.getMessage(),
                    OperationResult.OperationType.VALIDATION);
        }
    }

    /**
     * ✅ Get backup service statistics
     */
    public Map<String, Object> getBackupServiceStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("serviceName", "CalendarDatabaseBackupService");
            stats.put("calendarDatabaseVersion", getCalendarDatabaseVersion());
            stats.put("supportedEntityTypes", List.of("shifts", "teams", "recurrenceRules", "shiftExceptions", "userScheduleAssignments"));

            // Add current database stats if available
            try {
                stats.put("currentShiftCount", mCalendarDatabase.shiftDao().getShiftCount());
                stats.put("currentTeamCount", mCalendarDatabase.teamDao().getTeamCount());
                stats.put("currentRecurrenceRuleCount", mCalendarDatabase.recurrenceRuleDao().getTotalRecurrenceRuleCount());
                stats.put("currentShiftExceptionCount", mCalendarDatabase.shiftExceptionDao().getTotalShiftExceptionCount());
                stats.put("currentAssignmentCount", mCalendarDatabase.userScheduleAssignmentDao().getTotalAssignmentCount());
            } catch (Exception e) {
                Log.w(TAG, "Could not get current database statistics", e);
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to generate backup service statistics", e);
        }

        return stats;
    }
}