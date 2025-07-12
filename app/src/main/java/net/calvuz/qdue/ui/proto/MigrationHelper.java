package net.calvuz.qdue.ui.proto;

import static net.calvuz.qdue.QDue.VirtualScrollingSettings.USE_VIRTUAL_SCROLLING;

import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Utility class to help with gradual migration to virtual scrolling
 */
public class MigrationHelper {

    private static final String TAG = "MigrationHelper";

    /**
     * Feature flag to enable/disable virtual scrolling globally
     */
    public static boolean isVirtualScrollingEnabled() {
        // // Change to false in QDue to disable virtual scrolling globally
        return USE_VIRTUAL_SCROLLING;
    }

    /**
     * Check if device has sufficient resources for virtual scrolling
     */
    public static boolean isDeviceCapable() {
        try {
            // Check available memory, API level, etc.
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long freeMemory = runtime.freeMemory();

            // Require at least 64MB available memory
            boolean hasMemory = (maxMemory - freeMemory) > 64 * 1024 * 1024;

            // Require API 21+ for optimal performance
            boolean hasApiLevel = android.os.Build.VERSION.SDK_INT >= 21;

            Log.d(TAG, "Device capability check - Memory: " + hasMemory + ", API: " + hasApiLevel);
            return hasApiLevel;
//            return hasMemory && hasApiLevel;

        } catch (Exception e) {
            Log.w(TAG, "Error checking device capability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Determine if virtual scrolling should be used for this session
     */
    public static boolean shouldUseVirtualScrolling() {
        return isVirtualScrollingEnabled() && isDeviceCapable();
    }

    /**
     * Log performance metrics for monitoring migration success
     */
    public static void logPerformanceMetric(String metric, long value) {
        Log.d(TAG, "Performance metric - " + metric + ": " + value + "ms");

        // You could send these to analytics service for monitoring
        // Analytics.log("virtual_scrolling_" + metric, value);
    }
}