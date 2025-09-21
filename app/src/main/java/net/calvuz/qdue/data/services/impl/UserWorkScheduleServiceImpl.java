package net.calvuz.qdue.data.services.impl;

import androidx.annotation.NonNull;

import net.calvuz.qdue.data.services.UserWorkScheduleService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.usecases.GenerateUserScheduleUseCase;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserWorkScheduleServiceImpl implements UserWorkScheduleService
{
    private final static String TAG = "UserWorkScheduleServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;

    // ==================== PERFORMANCE AND CACHING ====================

    private final ExecutorService mExecutorService;
    //private final ConcurrentHashMap<String, Object> mCache;

    // ==================== STATE MANAGEMENT ====================

    private final boolean mIsInitialized;
    private volatile boolean mIsShutdown = false;

    // ==================== CONSTRUCTOR ====================

    public UserWorkScheduleServiceImpl(
            @NonNull GenerateUserScheduleUseCase mGenerateUserScheduleUseCase
    ) {
        this.mGenerateUserScheduleUseCase = mGenerateUserScheduleUseCase;

        // Initialize performance components
        this.mExecutorService = Executors.newFixedThreadPool( 3 );

        // Mark as initialized
        this.mIsInitialized = true;

        Log.d( TAG, "UserWorkScheduleService initialized" );
    }

    // ==================== IMPLEMENTATIONS ====================

    /**
     * Wrapper..
     * Generate a work schedule for a user for a given month.
     *
     * @param userID UserID
     * @param month  Month
     * @return CompletableFuture with OperationResult<Map<LocalDate, WorkScheduleDay>>
     */
    @Override
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> generateWorkScheduleForUser(
            @NonNull String userID,
            @NonNull YearMonth month
    ) {
        return mGenerateUserScheduleUseCase.getGenerateUserScheduleForMonth()
                .execute( userID, month );
    }

    /**
     * Wrapper..
     * Generate a work schedule for a user for a given date range.
     *
     * @param userID    UserID
     * @param startDate starting date (included)
     * @param endDate   ending date (included)
     * @return CompletableFuture with OperationResult<Map<LocalDate, WorkScheduleDay>>
     */
    @Override
    public CompletableFuture<OperationResult<Map<LocalDate, WorkScheduleDay>>> generateWorkScheduleForUser(
            @NonNull String userID,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate
    ) {
        return mGenerateUserScheduleUseCase.getGenerateUserScheduleForDateRange()
                .execute( userID, startDate, endDate );
    }

    /**
     * Wrapper..
     * Generate a work schedule for a user for a given date.
     *
     * @param userID UserID
     */
    @Override
    public CompletableFuture<OperationResult<WorkScheduleDay>> generateUserScheduleForDate(
            @NonNull String userID,
            @NonNull LocalDate date
    ) {
        return mGenerateUserScheduleUseCase.getGenerateUserScheduleForDate()
                .execute( userID, date );
    }

    // ==================== MAINTENANCE OPERATIONS ====================

    /**
     * Get service status.
     *
     * @return Service Status
     */
    @Override
    public CompletableFuture<OperationResult<String>> getServiceStatus() {
        return CompletableFuture.supplyAsync( () -> {
            try {
                String status = "QDueUserService Status:\n" +
                        "- Initialized: " + mIsInitialized + "\n" +
                        "- Shutdown: " + mIsShutdown + "\n" +
                        //"- Cache size: " + mCache.size() + "\n" +
                        "- Executor shutdown: " + mExecutorService.isShutdown() + "\n";

                return OperationResult.success( status, OperationResult.OperationType.SYSTEM );
            } catch (Exception e) {
                Log.e( TAG, "❌ Failed to get service status", e );
                return OperationResult.failure(
                        "Failed to get service status",
                        OperationResult.OperationType.SYSTEM
                );
            }
        } );
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Shutdown service and cleanup resources.
     */
    public void shutdown() {
        Log.d( TAG, "Shutting down UserWorkScheduleService" );

        mIsShutdown = true;
        //mCache.clear();

        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }

        Log.d( TAG, "✅ UserWorkScheduleService shutdown completed" );
    }
}
