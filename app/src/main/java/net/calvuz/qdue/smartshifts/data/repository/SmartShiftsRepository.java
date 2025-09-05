package net.calvuz.qdue.smartshifts.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.dao.TeamContactDao;
import net.calvuz.qdue.smartshifts.data.dao.UserShiftAssignmentDao;
import net.calvuz.qdue.smartshifts.data.dao.SmartShiftEventDao;
import net.calvuz.qdue.smartshifts.data.entities.ShiftPattern;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.data.entities.TeamContact;
import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;
import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;
import net.calvuz.qdue.smartshifts.domain.generators.ShiftGeneratorEngine;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Main repository for SmartShifts system
 * Coordinates between patterns, assignments, and events
 */
@Singleton
public class SmartShiftsRepository {

    private static final String TAG = "SmartShiftsRepository";

    private final ShiftPatternDao patternDao;
    private final ShiftTypeDao shiftTypeDao;
    private final TeamContactDao teamContactDao;
    private final UserShiftAssignmentDao assignmentDao;
    private final SmartShiftEventDao eventDao;
    private final ShiftGeneratorEngine generator;
    private final Context context;

    @Inject
    public SmartShiftsRepository(
            ShiftPatternDao patternDao,
            ShiftTypeDao shiftTypeDao,
            TeamContactDao teamContactDao,
            UserShiftAssignmentDao assignmentDao,
            SmartShiftEventDao eventDao,
            ShiftGeneratorEngine generator,
            Context context
    ) {
        this.patternDao = patternDao;
        this.shiftTypeDao = shiftTypeDao;
        this.teamContactDao = teamContactDao;
        this.assignmentDao = assignmentDao;
        this.eventDao = eventDao;
        this.generator = generator;
        this.context = context;
    }

    /**
     * Get shifts for user in specific month
     */
    public LiveData<List<SmartShiftEvent>> getUserShiftsForMonth(String userId, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        return eventDao.getEventsForUserInPeriod(
                userId,
                monthStart.toString(),
                monthEnd.toString()
        );
    }

    /**
     * Get user's active assignment
     */
    public LiveData<UserShiftAssignment> getUserActiveAssignment(String userId) {
        return assignmentDao.getActiveAssignmentForUserLive(userId);
    }

    /**
     * Check if user has active assignment
     */
    public LiveData<Boolean> hasActiveAssignment(String userId) {
        return assignmentDao.hasActiveAssignmentLive(userId);
    }

    /**
     * Assign pattern to user and generate events
     */
    public CompletableFuture<Boolean> assignPatternToUser(
            String userId,
            String patternId,
            LocalDate startDate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Deactivate previous assignments
                assignmentDao.deactivateUserAssignments(userId);

                // Create new assignment
                UserShiftAssignment assignment = new UserShiftAssignment();
                assignment.id = UUID.randomUUID().toString();
                assignment.userId = userId;
                assignment.shiftPatternId = patternId;
                assignment.startDate = startDate.toString();
                assignment.cycleOffsetDays = 0;
                assignment.teamName = context.getString(R.string.team_personal_label);
                assignment.teamColorHex = "#2196F3";
                assignment.isActive = true;
                assignment.assignedAt = System.currentTimeMillis();

                assignmentDao.insert(assignment);

                // Generate events for next 12 months
                LocalDate endDate = startDate.plusMonths(12);
                List<SmartShiftEvent> events = generator.generateShiftsForPeriod(
                        userId, patternId, startDate, endDate, 0
                );

                eventDao.insertAll(events);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error assigning pattern to user", e);
                return false;
            }
        });
    }

    /**
     * Regenerate events for user (when pattern changes)
     */
    public CompletableFuture<Boolean> regenerateUserEvents(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserShiftAssignment assignment = assignmentDao.getActiveAssignmentForUser(userId);
                if (assignment == null) return false;

                // Delete existing future events
                LocalDate today = LocalDate.now();
                LocalDate futureEnd = today.plusMonths(12);
                eventDao.deleteEventsInDateRange(userId, today.toString(), futureEnd.toString());

                // Generate new events
                List<SmartShiftEvent> events = generator.generateShiftsForPeriod(
                        userId,
                        assignment.shiftPatternId,
                        LocalDate.parse(assignment.startDate),
                        futureEnd,
                        assignment.cycleOffsetDays
                );

                eventDao.insertAll(events);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error regenerating user events", e);
                return false;
            }
        });
    }

    /**
     * Get events for date range
     */
    public List<SmartShiftEvent> getEventsForDateRange(LocalDate startDate, LocalDate endDate, String userId) {
        return eventDao.getEventsForUserInPeriodSync(userId, startDate.toString(), endDate.toString());
    }

    // ========== SYNCHRONOUS METHODS FOR EXPORT/IMPORT ==========

    /**
     * Get all shift types synchronously
     * Used for export/import operations that run on background thread
     */
    public List<ShiftType> getAllShiftTypes() {
        try {
            return shiftTypeDao.getAllActiveShiftTypesSync();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all shift types", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get all shift patterns synchronously
     * Used for export/import operations that run on background thread
     */
    public List<ShiftPattern> getAllShiftPatterns() {
        try {
            return patternDao.getAllActivePatternsSync();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all shift patterns", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get all user assignments synchronously
     * Used for export/import operations that run on background thread
     * <p>
     * ⚠️ IMPORTANT: This method should only be called from background threads
     * (e.g., within CompletableFuture.supplyAsync or similar async contexts)
     */
    public List<UserShiftAssignment> getAllUserAssignments() {
        try {
            return assignmentDao.getAllActiveAssignmentsSync();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all user assignments", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get all team contacts for user synchronously
     * Used for export/import operations that run on background thread
     */
    public List<TeamContact> getAllTeamContacts(String userId) {
        try {
            return teamContactDao.getContactsForUserSync(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting team contacts for user: " + userId, e);
            return List.of(); // Return empty list on error
        }
    }

    // ========== ASYNCHRONOUS METHODS FOR UI OPERATIONS ==========

    /**
     * Get all shift types asynchronously
     * Preferred method for UI operations
     */
    public CompletableFuture<List<ShiftType>> getAllShiftTypesAsync() {
        return CompletableFuture.supplyAsync(this::getAllShiftTypes);
    }

    /**
     * Get all shift patterns asynchronously
     * Preferred method for UI operations
     */
    public CompletableFuture<List<ShiftPattern>> getAllShiftPatternsAsync() {
        return CompletableFuture.supplyAsync(this::getAllShiftPatterns);
    }

    /**
     * Get all user assignments asynchronously
     * Preferred method for UI operations
     */
    public CompletableFuture<List<UserShiftAssignment>> getAllUserAssignmentsAsync() {
        return CompletableFuture.supplyAsync(this::getAllUserAssignments);
    }

    /**
     * Get all team contacts for user asynchronously
     * Preferred method for UI operations
     */
    public CompletableFuture<List<TeamContact>> getAllTeamContactsAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> getAllTeamContacts(userId));
    }

    // ========== BULK OPERATIONS FOR EXPORT/IMPORT ==========

    /**
     * Get all data for export as a single async operation
     * This is more efficient than multiple individual calls
     */
    public CompletableFuture<SmartShiftsExportData> getAllDataForExport() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SmartShiftsExportData exportData = new SmartShiftsExportData();
                exportData.shiftTypes = getAllShiftTypes();
                exportData.shiftPatterns = getAllShiftPatterns();
                exportData.userAssignments = getAllUserAssignments();
                // Note: Team contacts require userID, so not included in bulk export
                return exportData;
            } catch (Exception e) {
                Log.e(TAG, "Error getting all data for export", e);
                return new SmartShiftsExportData(); // Return empty data on error
            }
        });
    }

    /**
     * Data class for bulk export operations
     */
    public static class SmartShiftsExportData {
        public List<ShiftType> shiftTypes = List.of();
        public List<ShiftPattern> shiftPatterns = List.of();
        public List<UserShiftAssignment> userAssignments = List.of();
    }
}