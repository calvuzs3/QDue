package net.calvuz.qdue.smartshifts.domain.usecases;

import android.content.Context;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.repository.UserAssignmentRepository;
import net.calvuz.qdue.smartshifts.data.repository.SmartShiftsRepository;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Use case for assigning patterns to users
 */
public class AssignPatternUseCase {

    private final UserAssignmentRepository assignmentRepository;
    private final SmartShiftsRepository shiftsRepository;
    private final Context context;

    @Inject
    public AssignPatternUseCase(
            UserAssignmentRepository assignmentRepository,
            SmartShiftsRepository shiftsRepository,
            Context context
    ) {
        this.assignmentRepository = assignmentRepository;
        this.shiftsRepository = shiftsRepository;
        this.context = context;
    }

    /**
     * Assign pattern to user for first time
     */
    public CompletableFuture<AssignmentResult> assignPatternToUser(
            String userId,
            String patternId,
            LocalDate startDate
    ) {
        return shiftsRepository.assignPatternToUser(userId, patternId, startDate)
                .thenApply(success -> {
                    AssignmentResult result = new AssignmentResult();
                    result.success = success;
                    if (success) {
                        result.message = context.getString(
                                R.string.success_assignment_created
                        );
                    } else {
                        result.message = context.getString(
                                R.string.error_saving_data
                        );
                    }
                    return result;
                });
    }

    /**
     * Change user's current pattern
     */
    public CompletableFuture<AssignmentResult> changeUserPattern(
            String userId,
            String newPatternId,
            LocalDate newStartDate
    ) {
        return assignmentRepository.changeUserPattern(userId, newPatternId, newStartDate)
                .thenCompose(success -> {
                    if (success) {
                        return assignPatternToUser(userId, newPatternId, newStartDate);
                    } else {
                        AssignmentResult result = new AssignmentResult();
                        result.success = false;
                        result.message = context.getString(
                                R.string.error_saving_data
                        );
                        return CompletableFuture.completedFuture(result);
                    }
                });
    }

    /**
     * Update assignment start date
     */
    public CompletableFuture<Boolean> updateStartDate(String assignmentId, LocalDate newStartDate) {
        return assignmentRepository.updateStartDate(assignmentId, newStartDate);
    }

    // ===== RESULT CLASSES =====

    public static class AssignmentResult {
        public boolean success;
        public String message;
        public String assignmentId;
    }
}
