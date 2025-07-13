package net.calvuz.qdue.smartshifts.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.UserShiftAssignmentDao;
import net.calvuz.qdue.smartshifts.data.dao.SmartShiftEventDao;
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

    private final ShiftPatternDao patternDao;
    private final UserShiftAssignmentDao assignmentDao;
    private final SmartShiftEventDao eventDao;
    private final ShiftGeneratorEngine generator;
    private final Context context;

    @Inject
    public SmartShiftsRepository(
            ShiftPatternDao patternDao,
            UserShiftAssignmentDao assignmentDao,
            SmartShiftEventDao eventDao,
            ShiftGeneratorEngine generator,
            Context context
    ) {
        this.patternDao = patternDao;
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
                e.printStackTrace();
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
                e.printStackTrace();
                return false;
            }
        });
    }
}

// =====================================================================

