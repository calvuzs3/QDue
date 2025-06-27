package net.calvuz.qdue.ui.proto;

/**
 * INTEGRATION STEP 3: CalendarDataManager Integration
 *
 * Enhance existing CalendarDataManager to work with virtual scrolling
 * while maintaining backward compatibility
 */

// ==================== 1. ENHANCED CALENDAR DATA MANAGER ====================

import android.content.Context;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.dao.TurnExceptionDao;
import net.calvuz.qdue.events.models.TurnException;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.utils.Log;

/**
 * Enhanced CalendarDataManager with virtual scrolling support
 * Maintains existing API while adding async capabilities
 */
public class CalendarDataManagerEnhanced extends CalendarDataManager {

    private static final String TAG = "CalendarDataManagerEnhanced";
    private static final boolean LOG_ENABLED = true;

    // EXCEPTIONS storage integration
    private TurnExceptionDao exceptionDao;
    private HalfTeam currentUserTeam;

    // Virtual scrolling components
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);
    private final Map<LocalDate, CompletableFuture<List<Day>>> pendingLoads = new ConcurrentHashMap<>();
    private final Map<LocalDate, DataState> dataStates = new ConcurrentHashMap<>();

    // Callbacks for virtual scrolling
    private VirtualCalendarDataManager.DataAvailabilityCallback dataCallback;

    // Enhanced singleton instance
    private static volatile CalendarDataManagerEnhanced enhancedInstance;

    // Data states for virtual scrolling
    public enum DataState {
        NOT_REQUESTED, LOADING, AVAILABLE, ERROR, EXPIRED
    }

    // Private constructor for singleton
    private CalendarDataManagerEnhanced() {
        super();
    }

    /**
     * Get enhanced singleton instance
     */
    public static CalendarDataManagerEnhanced getEnhancedInstance() {
        if (enhancedInstance == null) {
            synchronized (CalendarDataManagerEnhanced.class) {
                if (enhancedInstance == null) {
                    enhancedInstance = new CalendarDataManagerEnhanced();
                }
            }
            enhancedInstance.initialize();
        }
        return enhancedInstance;
    }

    // Integration with EXCEPTIONS Storage
    private void initializeExceptionStorage() {
        Context context = QDue.getContext(); // Assume this method exists
        QDueDatabase database = QDueDatabase.getInstance(context); // Unified
        this.exceptionDao = database.turnExceptionDao();
        this.currentUserTeam = QDue.getQuattrodue().getUserHalfTeam();
    }

    private long getCurrentUserId() {
        QDueDatabase database = QDueDatabase.getInstance(QDue.getContext());
        User activeUser = database.getCurrentUser();
        return activeUser != null ? activeUser.getId() : 1L; // Default fallback
    }

    // ==================== 2. ASYNC DATA LOADING ====================

    /**
     * Async version of getMonthDays for virtual scrolling
     */
    public CompletableFuture<List<Day>> getMonthDaysAsync(LocalDate monthDate) {
        LocalDate normalizedDate = monthDate.withDayOfMonth(1);

        // Check if already loading
        CompletableFuture<List<Day>> existingLoad = pendingLoads.get(normalizedDate);
        if (existingLoad != null && !existingLoad.isDone()) {
            return existingLoad;
        }

        // Check cache first (from parent class)
        try {
            List<Day> cachedData = super.getMonthDays(normalizedDate);
            if (cachedData != null && !cachedData.isEmpty()) {
                dataStates.put(normalizedDate, DataState.AVAILABLE);
                notifyDataAvailable(normalizedDate, cachedData);
                return CompletableFuture.completedFuture(cachedData);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking cache for " + normalizedDate + ": " + e.getMessage());
        }

        // Start async loading
        CompletableFuture<List<Day>> loadTask = CompletableFuture.supplyAsync(() -> {
            return loadMonthDataWithProgress(normalizedDate);
        }, asyncExecutor);

        // Track the loading task
        pendingLoads.put(normalizedDate, loadTask);
        dataStates.put(normalizedDate, DataState.LOADING);
        notifyDataStateChanged(normalizedDate, DataState.LOADING, null);

        // Handle completion
        loadTask.whenComplete((data, throwable) -> {
            pendingLoads.remove(normalizedDate);

            if (throwable != null) {
                Log.e(TAG, "Error loading month " + normalizedDate + ": " + throwable.getMessage());
                dataStates.put(normalizedDate, DataState.ERROR);
                notifyDataStateChanged(normalizedDate, DataState.ERROR, null);
            } else {
                dataStates.put(normalizedDate, DataState.AVAILABLE);
                notifyDataAvailable(normalizedDate, data);
            }
        });

        return loadTask;
    }

    /**
     * Load month data with progress reporting
     */
    private List<Day> loadMonthDataWithProgress(LocalDate normalizedDate) {
        try {
            if (LOG_ENABLED) {
                Log.d(TAG, "Starting async load for month: " + normalizedDate);
            }

            // Report progress start
            notifyLoadingProgress(normalizedDate, 10);

            // Use existing QuattroDue logic
            QuattroDue quattroDue = QDue.getQuattrodue();
            if (quattroDue == null) {
                throw new RuntimeException("QuattroDue instance not available");
            }

            // Report progress
            notifyLoadingProgress(normalizedDate, 30);

            // Get shifts for the month
            List<Day> monthDays = quattroDue.getShiftsForMonth(normalizedDate);

            // Report progress
            notifyLoadingProgress(normalizedDate, 70);

            // Simulate some processing time for realistic progress
            try {
                Thread.sleep(100); // Minimal delay for smooth progress
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Report completion
            notifyLoadingProgress(normalizedDate, 100);

            if (LOG_ENABLED) {
                Log.d(TAG, "Async load completed for month: " + normalizedDate +
                        " (" + (monthDays != null ? monthDays.size() : 0) + " days)");
            }

            // Apply user EXCEPTIONS to base pattern
            List<TurnException> exceptions = loadExceptionsForMonth(normalizedDate);
            List<Day> mergedDays = PatternMergeEngine.mergePatternWithExceptions(
                    monthDays, exceptions, currentUserTeam);

            return mergedDays;
            // Old  one
            //return monthDays != null ? monthDays : new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error in loadMonthDataWithProgress: " + e.getMessage());
            throw new RuntimeException("Failed to load month data", e);
        }
    }

    /**
     * Load exceptions for a specific month
     * @param monthDate
     * @return exceptions list
     */
    private List<TurnException> loadExceptionsForMonth(LocalDate monthDate) {
        if (exceptionDao == null || currentUserTeam == null) {
            return new ArrayList<>();
        }

        LocalDate monthStart = monthDate.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        // Get user ID (implement getUserId() method)
        long userId = getCurrentUserId();

        return exceptionDao.getExceptionsForUserMonth(userId, monthStart, monthEnd);
    }

    // ==================== 3. VIRTUAL SCROLLING INTEGRATION ====================

    /**
     * Set callback for data availability updates
     */
    public void setDataAvailabilityCallback(VirtualCalendarDataManager.DataAvailabilityCallback callback) {
        this.dataCallback = callback;
    }

    /**
     * Get current data state for a month
     */
    public DataState getDataState(LocalDate monthDate) {
        LocalDate normalizedDate = monthDate.withDayOfMonth(1);
        return dataStates.getOrDefault(normalizedDate, DataState.NOT_REQUESTED);
    }

    /**
     * Request data for viewport with intelligent loading
     */
    public void requestViewportData(LocalDate centerMonth,
                                    VirtualCalendarDataManager.ScrollDirection direction,
                                    int velocity) {

        // Calculate months to load based on direction and velocity
        List<LocalDate> monthsToLoad = calculateMonthsToLoad(centerMonth, direction, velocity);

        // Load each month asynchronously
        for (LocalDate month : monthsToLoad) {
            getMonthDaysAsync(month);
        }
    }

    /**
     * Calculate which months to load based on scroll behavior
     */
    private List<LocalDate> calculateMonthsToLoad(LocalDate centerMonth,
                                                  VirtualCalendarDataManager.ScrollDirection direction,
                                                  int velocity) {
        List<LocalDate> months = new ArrayList<>();

        // Always load center month
        months.add(centerMonth);

        // Load adjacent months
        months.add(centerMonth.minusMonths(1));
        months.add(centerMonth.plusMonths(1));

        // Smart prefetching based on scroll direction and velocity
        if (velocity > 15) { // Fast scrolling
            if (direction == VirtualCalendarDataManager.ScrollDirection.FORWARD) {
                months.add(centerMonth.plusMonths(2));
                if (velocity > 30) {
                    months.add(centerMonth.plusMonths(3));
                }
            } else if (direction == VirtualCalendarDataManager.ScrollDirection.BACKWARD) {
                months.add(centerMonth.minusMonths(2));
                if (velocity > 30) {
                    months.add(centerMonth.minusMonths(3));
                }
            }
        }

        return months;
    }

    /**
     * Force refresh specific month
     */
    public void refreshMonth(LocalDate monthDate) {
        LocalDate normalizedDate = monthDate.withDayOfMonth(1);

        // Clear from cache (parent class)
        clearMonth(normalizedDate);

        // Reset state
        dataStates.put(normalizedDate, DataState.NOT_REQUESTED);

        // Cancel any pending load
        CompletableFuture<List<Day>> pendingLoad = pendingLoads.remove(normalizedDate);
        if (pendingLoad != null && !pendingLoad.isDone()) {
            pendingLoad.cancel(true);
        }

        // Start fresh load
        getMonthDaysAsync(normalizedDate);
    }

    // ==================== 4. NOTIFICATION METHODS ====================

    /**
     * Notify data state changed
     */
    private void notifyDataStateChanged(LocalDate month, DataState state, List<Day> data) {
        if (dataCallback != null) {
            try {
                // Convert internal state to VirtualCalendarDataManager state
                VirtualCalendarDataManager.DataState virtualState = convertToVirtualState(state);
                dataCallback.onDataStateChanged(month, virtualState, data);
            } catch (Exception e) {
                Log.e(TAG, "Error in data state callback: " + e.getMessage());
            }
        }
    }

    /**
     * Notify data available
     */
    private void notifyDataAvailable(LocalDate month, List<Day> data) {
        notifyDataStateChanged(month, DataState.AVAILABLE, data);
    }

    /**
     * Notify loading progress
     */
    private void notifyLoadingProgress(LocalDate month, int progressPercent) {
        if (dataCallback != null) {
            try {
                dataCallback.onLoadingProgress(month, progressPercent);
            } catch (Exception e) {
                Log.e(TAG, "Error in progress callback: " + e.getMessage());
            }
        }
    }

    /**
     * Convert internal DataState to VirtualCalendarDataManager.DataState
     */
    private VirtualCalendarDataManager.DataState convertToVirtualState(DataState internalState) {
        switch (internalState) {
            case NOT_REQUESTED: return VirtualCalendarDataManager.DataState.NOT_REQUESTED;
            case LOADING: return VirtualCalendarDataManager.DataState.LOADING;
            case AVAILABLE: return VirtualCalendarDataManager.DataState.AVAILABLE;
            case ERROR: return VirtualCalendarDataManager.DataState.ERROR;
            case EXPIRED: return VirtualCalendarDataManager.DataState.EXPIRED;
            default: return VirtualCalendarDataManager.DataState.NOT_REQUESTED;
        }
    }

    // ==================== 5. ENHANCED CACHE MANAGEMENT ====================

    /**
     * Clear specific month from cache
     */
    public void clearMonth(LocalDate monthDate) {
        LocalDate normalizedDate = monthDate.withDayOfMonth(1);

        // Clear from parent cache
        super.clearCache(); // You might need to enhance parent to clear specific month

        // Clear from enhanced cache
        dataStates.remove(normalizedDate);

        // Cancel pending loads
        CompletableFuture<List<Day>> pendingLoad = pendingLoads.remove(normalizedDate);
        if (pendingLoad != null && !pendingLoad.isDone()) {
            pendingLoad.cancel(true);
        }
    }

    /**
     * Enhanced cache cleanup with smart retention
     */
    public void cleanupCache(LocalDate currentMonth, int maxMonthsToKeep) {
        try {
            // Keep months within range of current month
            List<LocalDate> monthsToKeep = new ArrayList<>();
            for (int i = -maxMonthsToKeep/2; i <= maxMonthsToKeep/2; i++) {
                monthsToKeep.add(currentMonth.plusMonths(i));
            }

            // Remove data states for months outside range
            dataStates.entrySet().removeIf(entry -> !monthsToKeep.contains(entry.getKey()));

            // Cancel pending loads for months outside range
            pendingLoads.entrySet().removeIf(entry -> {
                if (!monthsToKeep.contains(entry.getKey())) {
                    entry.getValue().cancel(true);
                    return true;
                }
                return false;
            });

            if (LOG_ENABLED) {
                Log.d(TAG, "Cache cleanup completed. Keeping " + monthsToKeep.size() + " months");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during cache cleanup: " + e.getMessage());
        }
    }

    // ==================== 6. PERFORMANCE MONITORING ====================

    /**
     * Get cache statistics for monitoring
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
                dataStates.size(),
                pendingLoads.size(),
                (int) dataStates.values().stream().filter(state -> state == DataState.AVAILABLE).count(),
                (int) dataStates.values().stream().filter(state -> state == DataState.LOADING).count(),
                (int) dataStates.values().stream().filter(state -> state == DataState.ERROR).count()
        );
    }

    /**
     * Cache statistics holder
     */
    public static class CacheStatistics {
        public final int totalMonths;
        public final int pendingLoads;
        public final int availableMonths;
        public final int loadingMonths;
        public final int errorMonths;

        public CacheStatistics(int totalMonths, int pendingLoads, int availableMonths,
                               int loadingMonths, int errorMonths) {
            this.totalMonths = totalMonths;
            this.pendingLoads = pendingLoads;
            this.availableMonths = availableMonths;
            this.loadingMonths = loadingMonths;
            this.errorMonths = errorMonths;
        }

        @Override
        public String toString() {
            return String.format("Cache Stats - Total: %d, Available: %d, Loading: %d, Error: %d, Pending: %d",
                    totalMonths, availableMonths, loadingMonths, errorMonths, pendingLoads);
        }
    }

    // ==================== 7. SHUTDOWN AND CLEANUP ====================

    /**
     * Shutdown async executor and cleanup resources
     */
    public void shutdown() {
        try {
            // Cancel all pending loads
            for (CompletableFuture<List<Day>> future : pendingLoads.values()) {
                future.cancel(true);
            }
            pendingLoads.clear();

            // Shutdown executor
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }

            // Clear data
            dataStates.clear();
            dataCallback = null;

            if (LOG_ENABLED) {
                Log.d(TAG, "Enhanced CalendarDataManager shutdown completed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown: " + e.getMessage());
            asyncExecutor.shutdownNow();
        }
    }

    // ==================== 8. BACKWARD COMPATIBILITY ====================

    /**
     * Override parent getMonthDays to integrate with async loading
     */
    @Override
    public List<Day> getMonthDays(LocalDate monthDate) {
        LocalDate normalizedDate = monthDate.withDayOfMonth(1);

        // Check if data is already available
        DataState state = getDataState(normalizedDate);
        if (state == DataState.AVAILABLE) {
            // Use parent implementation for immediate return
            return super.getMonthDays(normalizedDate);
        }

        // If not available, trigger async load and return parent result as fallback
        getMonthDaysAsync(normalizedDate);
        return super.getMonthDays(normalizedDate);
    }
}