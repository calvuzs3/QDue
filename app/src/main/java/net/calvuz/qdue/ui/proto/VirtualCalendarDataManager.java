package net.calvuz.qdue.ui.proto;

import android.os.Handler;
import android.os.Looper;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROTOTYPE: Virtual Calendar with Skeleton Loading
 * Manages virtual calendar data with intelligent windowing
 * Only keeps essential data in memory, loads on-demand
 * This prototype implements:
 * 1. Virtual scrolling with limited memory footprint (3 months max)
 * 2. Skeleton UI for immediate responsiveness
 * 3. Progressive data loading without blocking UI
 * 4. Smart prefetching based on scroll behavior
 */
public class VirtualCalendarDataManager {
    private static final String TAG = "VirtualCalendarDataManager";

    // Virtual scrolling constants
    private static final int VIEWPORT_MONTHS = 3;           // Max months in memory
    private static final int PREFETCH_TRIGGER_WEEKS = 2;    // When to start prefetching
    private static final int MAX_CONCURRENT_LOADS = 2;      // Prevent memory overload

    // Data state management
    private final Map<LocalDate, DataState> monthDataStates = new ConcurrentHashMap<>();
    private final LRUCache<LocalDate, List<Day>> monthCache = new LRUCache<>(VIEWPORT_MONTHS);
    private final AtomicInteger activePrefetchTasks = new AtomicInteger(0);

    // Async loading infrastructure
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Data availability states for progressive UI updates
     */
    public enum DataState {
        NOT_REQUESTED,    // No data request made yet
        LOADING,          // Currently fetching data
        AVAILABLE,        // Data ready for display
        ERROR,            // Failed to load data
        EXPIRED           // Data needs refresh
    }

    /**
     * Callback interface for data availability updates
     */
    public interface DataAvailabilityCallback {
        void onDataStateChanged(LocalDate month, DataState state, List<Day> data);
        void onLoadingProgress(LocalDate month, int progressPercent);
    }

    private DataAvailabilityCallback callback;

    // ==================== 2. VIRTUAL VIEWPORT MANAGEMENT ====================

    /**
     * Request data for viewport range with intelligent prefetching
     * @param centerMonth The currently visible month
     * @param scrollDirection Direction of scroll movement
     * @param scrollVelocity Scroll speed for prefetch calculation
     */
    public void requestViewportData(LocalDate centerMonth, ScrollDirection scrollDirection, int scrollVelocity) {
        // Calculate viewport range
        List<LocalDate> viewportMonths = calculateViewportRange(centerMonth, scrollDirection, scrollVelocity);

        // Request data for each month in viewport
        for (LocalDate month : viewportMonths) {
            requestMonthDataIfNeeded(month, getPriorityForMonth(month, centerMonth));
        }

        // Cleanup old data outside viewport
        cleanupOutOfViewportData(viewportMonths);
    }

    /**
     * Calculate which months should be in viewport based on scroll behavior
     */
    private List<LocalDate> calculateViewportRange(LocalDate centerMonth, ScrollDirection direction, int velocity) {
        List<LocalDate> months = new ArrayList<>();

        // Always include center month
        months.add(centerMonth);

        // Add previous month
        months.add(centerMonth.minusMonths(1));

        // Add next month
        months.add(centerMonth.plusMonths(1));

        // Smart prefetching based on scroll direction and velocity
        if (velocity > 15) { // Fast scrolling
            if (direction == ScrollDirection.FORWARD) {
                months.add(centerMonth.plusMonths(2)); // Prefetch ahead
            } else if (direction == ScrollDirection.BACKWARD) {
                months.add(centerMonth.minusMonths(2)); // Prefetch behind
            }
        }

        return months;
    }

    // ==================== 3. ASYNC DATA LOADING ====================

    /**
     * Request month data with priority-based loading
     */
    private void requestMonthDataIfNeeded(LocalDate month, LoadPriority priority) {
        DataState currentState = monthDataStates.getOrDefault(month, DataState.NOT_REQUESTED);

        // Skip if already loading or available
        if (currentState == DataState.LOADING || currentState == DataState.AVAILABLE) {
            return;
        }

        // Check concurrent load limit
        if (activePrefetchTasks.get() >= MAX_CONCURRENT_LOADS && priority == LoadPriority.LOW) {
            Log.d(TAG, "Skipping low priority load for " + month + " due to load limit");
            return;
        }

        // Start loading
        monthDataStates.put(month, DataState.LOADING);
        activePrefetchTasks.incrementAndGet();

        // Notify UI of loading state
        if (callback != null) {
            mainHandler.post(() -> callback.onDataStateChanged(month, DataState.LOADING, null));
        }

        // Load data asynchronously
        CompletableFuture.supplyAsync(() -> loadMonthDataBackground(month), backgroundExecutor)
                .thenAccept(data -> handleDataLoadComplete(month, data))
                .exceptionally(throwable -> {
                    handleDataLoadError(month, throwable);
                    return null;
                });
    }

    /**
     * Background data loading with progress updates
     */
    private List<Day> loadMonthDataBackground(LocalDate month) {
        try {
            // Simulate progressive loading with progress updates
            List<Day> monthDays = new ArrayList<>();
            int totalDays = month.lengthOfMonth();

            for (int day = 1; day <= totalDays; day++) {
                // Simulate processing time for shift data
                Thread.sleep(10); // Realistic API delay simulation

                LocalDate dayDate = month.withDayOfMonth(day);
                Day dayData = createDayWithShiftData(dayDate); // Your existing logic
                monthDays.add(dayData);

                // Report progress
                final int progress = (day * 100) / totalDays;
                if (callback != null && day % 5 == 0) { // Update every 5 days
                    mainHandler.post(() -> callback.onLoadingProgress(month, progress));
                }
            }

            return monthDays;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data loading interrupted", e);
        }
    }

    /**
     * Handle successful data load completion
     */
    private void handleDataLoadComplete(LocalDate month, List<Day> data) {
        activePrefetchTasks.decrementAndGet();
        monthDataStates.put(month, DataState.AVAILABLE);
        monthCache.put(month, data);

        Log.d(TAG, "Data loaded successfully for " + month + " (" + data.size() + " days)");

        // Notify UI of data availability
        if (callback != null) {
            mainHandler.post(() -> callback.onDataStateChanged(month, DataState.AVAILABLE, data));
        }
    }

    /**
     * Handle data loading errors
     */
    private void handleDataLoadError(LocalDate month, Throwable error) {
        activePrefetchTasks.decrementAndGet();
        monthDataStates.put(month, DataState.ERROR);

        Log.e(TAG, "Failed to load data for " + month + ": " + error.getMessage());

        if (callback != null) {
            mainHandler.post(() -> callback.onDataStateChanged(month, DataState.ERROR, null));
        }
    }

    // ==================== 4. MEMORY MANAGEMENT ====================

    /**
     * Cleanup data outside current viewport to free memory
     */
    private void cleanupOutOfViewportData(List<LocalDate> currentViewport) {
        Set<LocalDate> viewportSet = new HashSet<>(currentViewport);

        // Remove cache entries not in viewport
        monthCache.evictNotInSet(viewportSet);

        // Cancel pending loads for months far from viewport
        monthDataStates.entrySet().removeIf(entry -> {
            LocalDate month = entry.getKey();
            DataState state = entry.getValue();

            if (!viewportSet.contains(month) && state == DataState.LOADING) {
                // Cancel if more than 2 months away from viewport
                boolean shouldCancel = currentViewport.stream()
                        .noneMatch(viewportMonth ->
                                Math.abs(ChronoUnit.MONTHS.between(month, viewportMonth)) <= 2);

                if (shouldCancel) {
                    Log.d(TAG, "Cancelling load for distant month: " + month);
                    return true;
                }
            }

            return false;
        });
    }

    // ==================== 5. PUBLIC API ====================

    /**
     * Get data for a specific month if available
     */
    public List<Day> getMonthData(LocalDate month) {
        return monthCache.get(month);
    }

    /**
     * Get current data state for a month
     */
    public DataState getDataState(LocalDate month) {
        return monthDataStates.getOrDefault(month, DataState.NOT_REQUESTED);
    }

    /**
     * Set callback for data availability updates
     */
    public void setDataAvailabilityCallback(DataAvailabilityCallback callback) {
        this.callback = callback;
    }

    /**
     * Force refresh data for a specific month
     */
    public void refreshMonth(LocalDate month) {
        monthDataStates.put(month, DataState.NOT_REQUESTED);
        monthCache.remove(month);
        requestMonthDataIfNeeded(month, LoadPriority.HIGH);
    }

    // ==================== 6. HELPER ENUMS & CLASSES ====================

    public enum ScrollDirection {
        FORWARD, BACKWARD, STATIONARY
    }

    private enum LoadPriority {
        HIGH,    // Currently visible
        MEDIUM,  // Adjacent to visible
        LOW      // Prefetch
    }

    private LoadPriority getPriorityForMonth(LocalDate month, LocalDate centerMonth) {
        long monthsAway = Math.abs(ChronoUnit.MONTHS.between(month, centerMonth));

        if (monthsAway == 0) return LoadPriority.HIGH;
        if (monthsAway == 1) return LoadPriority.MEDIUM;
        return LoadPriority.LOW;
    }

    // ==================== 7. LRU CACHE IMPLEMENTATION ====================

    /**
     * Simple LRU cache for month data
     */
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(maxSize + 1, 1.0f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }

        public void evictNotInSet(Set<K> keepSet) {
            entrySet().removeIf(entry -> !keepSet.contains(entry.getKey()));
        }
    }

    // ==================== 8. UTILITY METHODS ====================

    /**
     * Create Day object with shift data (placeholder for your existing logic)
     */
    private Day createDayWithShiftData(LocalDate date) {
        // TODO: Replace with your actual Day creation logic
        // This is just a placeholder for the prototype
        return new Day(date  /*, QDue.getQuattrodue().getUserHalfTeam() /* your shift data logic here */);
    }

    /**
     * Clean shutdown of background tasks
     */
    public void shutdown() {
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}