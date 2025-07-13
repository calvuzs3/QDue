package net.calvuz.qdue.smartshifts.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.smartshifts.data.dao.UserShiftAssignmentDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.SmartShiftEventDao;
import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;
import net.calvuz.qdue.smartshifts.domain.generators.ShiftGeneratorEngine;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for managing user shift assignments
 */
@Singleton
public class UserAssignmentRepository {

    private final UserShiftAssignmentDao assignmentDao;
    private final ShiftPatternDao patternDao;
    private final SmartShiftEventDao eventDao;
    private final ShiftGeneratorEngine generator;

    @Inject
    public UserAssignmentRepository(
            UserShiftAssignmentDao assignmentDao,
            ShiftPatternDao patternDao,
            SmartShiftEventDao eventDao,
            ShiftGeneratorEngine generator
    ) {
        this.assignmentDao = assignmentDao;
        this.patternDao = patternDao;
        this.eventDao = eventDao;
        this.generator = generator;
    }

    /**
     * Get user's active assignment
     */
    public LiveData<UserShiftAssignment> getActiveAssignmentForUser(String userId) {
        return assignmentDao.getActiveAssignmentForUserLive(userId);
    }

    /**
     * Get all assignments for user
     */
    public LiveData<List<UserShiftAssignment>> getAllAssignmentsForUser(String userId) {
        return assignmentDao.getAllAssignmentsForUser(userId);
    }

    /**
     * Change user's pattern
     */
    public CompletableFuture<Boolean> changeUserPattern(
            String userId,
            String newPatternId,
            LocalDate newStartDate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Deactivate current assignment
                assignmentDao.deactivateUserAssignments(userId);

                // Delete future events
                LocalDate today = LocalDate.now();
                eventDao.deleteEventsInDateRange(
                        userId,
                        today.toString(),
                        today.plusMonths(12).toString()
                );

                // Create new assignment (handled by SmartShiftsRepository)
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Update assignment start date
     */
    public CompletableFuture<Boolean> updateStartDate(String assignmentId, LocalDate newStartDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                assignmentDao.updateStartDate(assignmentId, newStartDate.toString());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Check if user has any assignment
     */
    public LiveData<Boolean> hasActiveAssignment(String userId) {
        return assignmentDao.hasActiveAssignmentLive(userId);
    }
}

// =====================================================================

