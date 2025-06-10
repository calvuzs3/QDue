package net.calvuz.qdue.quattrodue.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;

/**
 * Centralized data manager for calendar operations.
 *
 * Eliminates duplication between DayslistViewFragment and CalendarViewFragment
 * by providing unified data access with intelligent caching and preloading.
 * Thread-safe implementation with performance optimizations.
 *
 * It uniquely make use of quattroDue.getShiftsForMonth(normalizedDate);
 *
 * @author Updated 21/05/2025
 */
public class CalendarDataManager {

    private static final String TAG = "CalendarDataManager";
    private static final boolean LOG_ENABLED = true;

    private static volatile CalendarDataManager instance;
    private final Map<String, MonthCache> monthsCache = new ConcurrentHashMap<>();
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    // Cache configuration
    private static final int MAX_CACHED_MONTHS = 24;
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    // Singleton
    private CalendarDataManager() {}

    /**
     * Gets the singleton instance.
     *
     * @return CalendarDataManager instance
     */
    public static CalendarDataManager getInstance() {
        if (instance == null) {
            synchronized (CalendarDataManager.class) {
                if (instance == null) {
                    instance = new CalendarDataManager();
                }
            }
            instance.initialize();
        }
        return instance;
    }

    /**
     * Initializes the data manager with QuattroDue instance.
     */
    private void initialize() {
        if (LOG_ENABLED) Log.d(TAG, "CalendarDataManager initialized");
    }

    /**
     * MAIN METHOD: Gets days for a month with intelligent caching.
     *
     * @param monthDate Month date
     * @return List of days for the month
     */
    public List<Day> getMonthDays(LocalDate monthDate) {
        if (QDue.getQuattrodue() == null) return new ArrayList<>();

        LocalDate normalizedDate = monthDate.withDayOfMonth(1);
        String cacheKey = getCacheKey(normalizedDate);

        // Check cache
        MonthCache cached = monthsCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            if (LOG_ENABLED) Log.v(TAG, "Cache hit: " + normalizedDate);
            List<Day> days = cached.getDaysCopy();
            updateTodayFlags(days);
            return days;
        }

        // Cache miss - generate data
        if (LOG_ENABLED) Log.d(TAG, "Cache miss: " + normalizedDate);

        try {
            List<Day> days = QDue.getQuattrodue().getShiftsForMonth(normalizedDate);
            updateTodayFlags(days);

            // Save to cache
            monthsCache.put(cacheKey, new MonthCache(days));
            cleanupCacheIfNeeded();

            return new ArrayList<>(days);
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Error generating data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Pre-loads months around a date to improve performance.
     *
     * @param centerDate Center date
     * @param radius Number of months to load in each direction
     */
    public void preloadMonthsAround(LocalDate centerDate, int radius) {
        if (!isUpdating.compareAndSet(false, true)) return;

        try {
            for (int i = -radius; i <= radius; i++) {
                LocalDate monthToLoad = centerDate.plusMonths(i);
                String cacheKey = getCacheKey(monthToLoad.withDayOfMonth(1));
                if (!monthsCache.containsKey(cacheKey)) {
                    getMonthDays(monthToLoad);
                }
            }
        } finally {
            isUpdating.set(false);
        }
    }

    /**
     * Finds today's position in the data.
     *
     * @return TodayPosition object with month and day index
     */
    public TodayPosition findTodayPosition() {
        LocalDate today = LocalDate.now();
        LocalDate todayMonth = today.withDayOfMonth(1);

        List<Day> monthDays = getMonthDays(todayMonth);

        for (int i = 0; i < monthDays.size(); i++) {
            if (monthDays.get(i).getDate().equals(today)) {
                return new TodayPosition(todayMonth, i);
            }
        }

        return new TodayPosition(todayMonth, -1);
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        monthsCache.clear();
        if (LOG_ENABLED) Log.d(TAG, "Cache cleared");
    }

    /**
     * Called when user team changes.
     */
    public void onUserTeamChanged() {
        // No need to reload base data, just update UI
        if (LOG_ENABLED) Log.d(TAG, "User team changed");
    }

    // === PRIVATE METHODS ===

    /**
     * Generates cache key for a date.
     */
    private String getCacheKey(LocalDate date) {
        return date.getYear() + "-" + date.getMonthValue();
    }

    /**
     * Updates today flags for a list of days.
     */
    private void updateTodayFlags(List<Day> days) {
        LocalDate today = LocalDate.now();
        for (Day day : days) {
            day.setIsToday(day.getDate().equals(today));
        }
    }

    /**
     * Cleans up cache if it exceeds maximum size.
     */
    private void cleanupCacheIfNeeded() {
        if (monthsCache.size() <= MAX_CACHED_MONTHS) return;

        // Remove expired entries
        monthsCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // If still too full, remove oldest entries
        if (monthsCache.size() > MAX_CACHED_MONTHS) {
            List<String> keys = new ArrayList<>(monthsCache.keySet());
            keys.sort((a, b) -> Long.compare(
                    monthsCache.get(a).timestamp,
                    monthsCache.get(b).timestamp
            ));

            int toRemove = monthsCache.size() - MAX_CACHED_MONTHS + 2;
            for (int i = 0; i < toRemove && i < keys.size(); i++) {
                monthsCache.remove(keys.get(i));
            }
        }
    }

    // === INNER CLASSES ===

    /**
     * Cache entry for a month's data.
     */
    private static class MonthCache {
        final List<Day> days;
        final long timestamp;

        MonthCache(List<Day> days) {
            this.days = new ArrayList<>(days);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }

        List<Day> getDaysCopy() {
            return new ArrayList<>(days);
        }
    }

    /**
     * Result object for today position search.
     */
    public static class TodayPosition {
        public final LocalDate monthDate;
        public final int dayIndex;
        public final boolean found;

        TodayPosition(LocalDate monthDate, int dayIndex) {
            this.monthDate = monthDate;
            this.dayIndex = dayIndex;
            this.found = dayIndex >= 0;
        }
    }
}