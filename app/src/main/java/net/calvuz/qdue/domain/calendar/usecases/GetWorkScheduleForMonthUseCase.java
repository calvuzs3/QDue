package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GetWorkScheduleForMonthUseCase - Get work schedule for entire month
 */
public class GetWorkScheduleForMonthUseCase {

    private static final String TAG = "GetWorkScheduleForMonthUseCase";

    private final WorkScheduleRepository workScheduleRepository;

    public GetWorkScheduleForMonthUseCase(@NonNull WorkScheduleRepository workScheduleRepository) {
        this.workScheduleRepository = workScheduleRepository;
    }

    /**
     * ✅ FIXED: Execute method with correct return types
     */
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> execute(@NonNull YearMonth month) {

        // ✅ FIXED: Validate input and return early if invalid
        OperationResult<Void> validationResult = validateInput(month);
        if (!validationResult.isSuccess()) {
            return CompletableFuture.completedFuture(
                    OperationResult.failure(validationResult.getErrorMessage(), OperationResult.OperationType.READ)
            );
        }

        Log.d(TAG, "Executing GetWorkScheduleForMonth for: " + month);

        // ✅ FIXED: Repository method returns CompletableFuture<OperationResult<Map<LocalDate, Day>>>
        return workScheduleRepository.getWorkScheduleForMonth(month, null)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        Log.d(TAG, "✅ Successfully retrieved schedule for " + month +
                                " (" + result.getData().size() + " days)");
                        return result;
                    } else {
                        Log.w(TAG, "❌ Failed to get schedule for " + month + ": " + result.getErrorMessage());
                        return result;
                    }
                })
                .exceptionally(throwable -> {
                    String error = "Unexpected error getting work schedule for " + month + ": " + throwable.getMessage();
                    Log.e(TAG, error, throwable);
                    return OperationResult.failure(error, OperationResult.OperationType.READ);
                });
    }

    /**
     * ✅ FIXED: Business validation with correct OperationResult construction
     */
    private OperationResult<Void> validateInput(@NonNull YearMonth month) {

        YearMonth now = YearMonth.now();

        // Rule 1: Month cannot be more than 5 years in the future
        YearMonth maxFuture = now.plusYears(5);
        if (month.isAfter(maxFuture)) {
            return OperationResult.failure("Month cannot be more than 5 years in the future",
                    OperationResult.OperationType.VALIDATION);
        }

        // Rule 2: Month cannot be more than 2 years in the past
        YearMonth maxPast = now.minusYears(2);
        if (month.isBefore(maxPast)) {
            return OperationResult.failure("Month cannot be more than 2 years in the past",
                    OperationResult.OperationType.VALIDATION);
        }

        // ✅ FIXED: Correct success construction for Void type
        return OperationResult.success("Validation passed", OperationResult.OperationType.VALIDATION);
    }
}

