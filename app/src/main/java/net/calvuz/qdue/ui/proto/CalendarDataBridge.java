package net.calvuz.qdue.ui.proto;

/**
 * INTEGRATION STEP 1: Bridge Classes for Prototype Integration
 * <p>
 * These classes bridge your existing code with the new virtual scrolling prototype
 * allowing gradual migration without breaking existing functionality
 */

// ==================== 1. DATA BRIDGE FOR EXISTING CALENDAR MANAGER ====================

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.utils.CalendarDataManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Bridge between existing CalendarDataManager and new VirtualCalendarDataManager
 * Allows gradual migration while maintaining existing API compatibility
 */
public class CalendarDataBridge {

    private static final String TAG = "CalendarDataBridge";

    private final CalendarDataManager existingManager;
    protected final VirtualCalendarDataManager virtualManager;

    public CalendarDataBridge() {
        // Initialize both managers
        this.existingManager = CalendarDataManager.getInstance();
        this.virtualManager = new VirtualCalendarDataManager();

        Log.d(TAG, "Bridge initialized between existing and virtual data managers");
    }

    /**
     * Enhanced data loading that uses virtual manager but falls back to existing
     */
    public CompletableFuture<List<Day>> getMonthDataAsync(LocalDate month) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First try the new virtual manager approach
                List<Day> virtualData = virtualManager.getMonthData(month);
                if (virtualData != null && !virtualData.isEmpty()) {
                    Log.d(TAG, "Data served from virtual manager for " + month);
                    return virtualData;
                }

                // Fallback to existing manager
                List<Day> existingData = existingManager.getMonthDays(month);
                Log.d(TAG, "Data served from existing manager for " + month);
                return existingData;

            } catch (Exception e) {
                Log.e(TAG, "Error loading data for " + month + ": " + e.getMessage());
                // Return empty list as fallback
                return existingManager.getMonthDays(month);
            }
        });
    }

    /**
     * Set callback for data availability updates
     */
    public void setDataAvailabilityCallback(VirtualCalendarDataManager.DataAvailabilityCallback callback) {
        virtualManager.setDataAvailabilityCallback(callback);
    }

    /**
     * Request data for viewport - delegates to virtual manager
     */
    public void requestViewportData(LocalDate centerMonth,
                                    VirtualCalendarDataManager.ScrollDirection direction,
                                    int velocity) {
        virtualManager.requestViewportData(centerMonth, direction, velocity);
    }

    /**
     * Get current data state for a month
     */
    public VirtualCalendarDataManager.DataState getDataState(LocalDate month) {
        return virtualManager.getDataState(month);
    }

    /**
     * Force refresh specific month
     */
    public void refreshMonth(LocalDate month) {
        // Clear from both managers
//        existingManager.clearMonth(month);
        existingManager.cleanupCacheIfNeeded(); // it was private..
        virtualManager.refreshMonth(month);
    }

    /**
     * Clear all cached data
     */
    public void clearAllCache() {
        existingManager.clearCache();
        // Virtual manager cleanup happens automatically
    }

    /**
     * Shutdown resources
     */
    public void shutdown() {
        virtualManager.shutdown();
    }
}
