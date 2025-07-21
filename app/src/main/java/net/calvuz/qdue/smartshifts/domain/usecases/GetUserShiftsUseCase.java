// ===== PACKAGE: net.calvuz.qdue.smartshifts.ui.usecases =====

package net.calvuz.qdue.smartshifts.domain.usecases;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;
import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;
import net.calvuz.qdue.smartshifts.data.repository.SmartShiftsRepository;
import net.calvuz.qdue.smartshifts.data.repository.UserAssignmentRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Use case for retrieving user shift data
 * Encapsulates business logic for fetching and presenting shift information
 */
public class GetUserShiftsUseCase {

    private final SmartShiftsRepository shiftsRepository;
    private final UserAssignmentRepository assignmentRepository;

    @Inject
    public GetUserShiftsUseCase(
            SmartShiftsRepository shiftsRepository,
            UserAssignmentRepository assignmentRepository
    ) {
        this.shiftsRepository = shiftsRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Get user shifts for current month
     */
    public LiveData<List<SmartShiftEvent>> getCurrentMonthShifts(String userId) {
        LocalDate now = LocalDate.now();
        return shiftsRepository.getUserShiftsForMonth(userId, now.getYear(), now.getMonthValue());
    }

    /**
     * Get user shifts for specific month
     */
    public LiveData<List<SmartShiftEvent>> getShiftsForMonth(String userId, YearMonth yearMonth) {
        return shiftsRepository.getUserShiftsForMonth(
                userId,
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );
    }

    /**
     * Get user's active assignment info
     */
    public LiveData<UserShiftAssignment> getUserActiveAssignment(String userId) {
        return shiftsRepository.getUserActiveAssignment(userId);
    }

    /**
     * Check if user has any active assignment
     */
    public LiveData<Boolean> hasActiveAssignment(String userId) {
        return shiftsRepository.hasActiveAssignment(userId);
    }

    /**
     * Regenerate shifts for user (after pattern change)
     */
    public CompletableFuture<Boolean> regenerateUserShifts(String userId) {
        return shiftsRepository.regenerateUserEvents(userId);
    }

    /**
     * Get shift statistics for user
     */
    public CompletableFuture<ShiftStatistics> getShiftStatistics(String userId, YearMonth yearMonth) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implement statistics calculation
            // For now, return placeholder data
            ShiftStatistics stats = new ShiftStatistics();
            stats.totalShifts = 0;
            stats.workingDays = 0;
            stats.restDays = 0;
            stats.totalHours = 0;
            return stats;
        });
    }

    // ===== RESULT CLASSES =====

    public static class ShiftStatistics {
        public int totalShifts;
        public int workingDays;
        public int restDays;
        public int totalHours;
        public String mostFrequentShiftType;
    }
}

// =====================================================================

