package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.calendar.repositories.UserTeamAssignmentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ValidateAssignmentUseCase - Assignment validation business logic
 */
public class ValidateAssignmentUseCase
{
    private final UserTeamAssignmentRepository mAssignmentRepository;

    public ValidateAssignmentUseCase(
            @NonNull UserTeamAssignmentRepository assignmentRepository
    ) {
        this.mAssignmentRepository = assignmentRepository;
    }

    /**
     * Validate assignment with detailed conflict analysis.
     *
     * @param userId              User ID to validate
     * @param teamId              Team ID to validate
     * @param startDate           Assignment start date
     * @param endDate             Assignment end date
     * @param excludeAssignmentId Assignment to exclude from validation
     * @return CompletableFuture with detailed validation result
     */
    public CompletableFuture<OperationResult<DetailedValidationResult>> execute(
            @NonNull String userId,
            @NonNull String teamId,
            @NonNull LocalDate startDate,
            @Nullable LocalDate endDate,
            @Nullable String excludeAssignmentId
    ) {

        return mAssignmentRepository.validateAssignment(
                        userId, teamId, startDate, endDate, excludeAssignmentId )
                .thenApply( result -> {
                    if (!result.isSuccess()) {
                        return OperationResult.failure( result.getErrorMessage(),
                                                        OperationResult.OperationType.VALIDATION );
                    }

                    DetailedValidationResult detailedResult = getDetailedValidationResult(
                            startDate, endDate, result );

                    return OperationResult.success( detailedResult,
                                                    OperationResult.OperationType.VALIDATION );
                } );
    }

    @NonNull
    private static DetailedValidationResult getDetailedValidationResult(
            @NonNull LocalDate startDate,
            @Nullable LocalDate endDate,
            @NonNull OperationResult<UserTeamAssignmentRepository.AssignmentValidationResult> result
    ) {
        UserTeamAssignmentRepository.AssignmentValidationResult validationResult = result.getData();
        assert validationResult != null;

        return new DetailedValidationResult(
                validationResult.isValid,
                validationResult.validationMessage,
                validationResult.conflictingAssignments,
                startDate,
                endDate
        );
    }

    // ==================== INNER CLASSES ====================

    /**
     * Detailed validation result with additional analysis.
     */
    public record DetailedValidationResult(
            boolean isValid,
            String message,
            List<UserTeamAssignment> conflicts,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate
    )
    {

        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }

        public int getConflictCount() {
            return conflicts.size();
        }

        public List<String> getConflictSummary() {
            return conflicts.stream()
                    .map( assignment -> "Assignment " + assignment.getId() +
                            " from " + assignment.getStartDate() +
                            " to " + (assignment.getEndDate() != null ? assignment.getEndDate() : "permanent") )
                    .collect( Collectors.toList() );
        }

        @NonNull
        @Override
        public String toString() {
            return "DetailedValidationResult{" +
                    "valid=" + isValid +
                    ", conflicts=" + getConflictCount() +
                    ", requestedRange=" + requestedStartDate +
                    " to " + (requestedEndDate != null ? requestedEndDate : "permanent") +
                    '}';
        }
    }
}