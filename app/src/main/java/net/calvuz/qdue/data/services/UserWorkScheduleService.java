package net.calvuz.qdue.data.services;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface UserWorkScheduleService
{
    /**
     * Generate a work schedule for a user for a given month.
     *
     * @param userID UserID
     * @param month  Month
     * @return CompletableFuture with OperationResult<Map<LocalDate, WorkScheduleDay>>
     */
    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> generateWorkScheduleForUser(
            @NonNull String userID,
            @NonNull YearMonth month
    );

    CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> generateWorkScheduleForUser(
            @NonNull String userID,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate
    );

    /**
     * Generate a work schedule for a user for a given date.
     *
     * @param userID UserID
     * @param date   Date
     * @return CompletableFuture with OperationResult<WorkScheduleDay>
     */
    CompletableFuture<OperationResult<WorkScheduleDay>> generateUserScheduleForDate(
            @NonNull String userID,
            @NonNull LocalDate date
    );

    /**
     * Get service status.
     *
     * @return Service Status
     */
    CompletableFuture<OperationResult<String>> getServiceStatus();
}
