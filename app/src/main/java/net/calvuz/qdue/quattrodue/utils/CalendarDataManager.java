
package net.calvuz.qdue.quattrodue.utils;

// ==================== GESTORE DATI CENTRALIZZATO ====================

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.Costants;

/**
 * Gestore centralizzato per i dati del calendario.
 * Elimina la duplicazione tra DayslistViewFragment e CalendarViewFragment.
 */
public class CalendarDataManager {

    private static final String TAG = "CalendarDataManager";
    private static final boolean LOG_ENABLED = true;

    private static volatile CalendarDataManager instance;
    private final Map<String, MonthCache> monthsCache = new ConcurrentHashMap<>();
    private QuattroDue quattroDue;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    // Configurazione cache
    private static final int MAX_CACHED_MONTHS = 24;
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minuti

    private CalendarDataManager() {}

    public static CalendarDataManager getInstance() {
        if (instance == null) {
            synchronized (CalendarDataManager.class) {
                if (instance == null) {
                    instance = new CalendarDataManager();
                }
            }
        }
        Log.d(TAG, "getInstancee()");

        return instance;
    }

    public void initialize(QuattroDue qd) {
        this.quattroDue = qd;
        if (LOG_ENABLED) Log.d(TAG, "CalendarDataManager inizializzato");
    }

    /**
     * METODO PRINCIPALE: Ottiene i giorni per un mese con cache intelligente.
     */
    public List<Day> getMonthDays(LocalDate monthDate) {
        if (quattroDue == null) return new ArrayList<>();

        LocalDate normalizedDate = monthDate.withDayOfMonth(1);
        String cacheKey = getCacheKey(normalizedDate);

        // Controlla cache
        MonthCache cached = monthsCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            if (LOG_ENABLED) Log.v(TAG, "Cache hit: " + normalizedDate);
            List<Day> days = cached.getDaysCopy();
            updateTodayFlags(days);
            return days;
        }

        // Cache miss - genera dati
        if (LOG_ENABLED) Log.d(TAG, "Cache miss: " + normalizedDate);

        try {
            List<Day> days = quattroDue.getShiftsForMonth(normalizedDate);
            updateTodayFlags(days);

            // Salva in cache
            monthsCache.put(cacheKey, new MonthCache(days));
            cleanupCacheIfNeeded();

            return new ArrayList<>(days);
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore generazione dati: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Pre-carica mesi intorno a una data per migliorare prestazioni.
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
     * Trova la posizione di oggi.
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

    public void clearCache() {
        monthsCache.clear();
        if (LOG_ENABLED) Log.d(TAG, "Cache pulita");
    }

    public void onUserTeamChanged() {
        // Non serve ricaricare i dati base, solo aggiornare la UI
        if (LOG_ENABLED) Log.d(TAG, "Team utente cambiato");
    }

    // === METODI PRIVATI ===

    private String getCacheKey(LocalDate date) {
        return date.getYear() + "-" + date.getMonthValue();
    }

    private void updateTodayFlags(List<Day> days) {
        LocalDate today = LocalDate.now();
        for (Day day : days) {
            day.setIsToday(day.getDate().equals(today));
        }
    }

    private void cleanupCacheIfNeeded() {
        if (monthsCache.size() <= MAX_CACHED_MONTHS) return;

        // Rimuovi entry scadute
        monthsCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Se ancora troppo piena, rimuovi le più vecchie
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

    // === CLASSI INTERNE ===

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