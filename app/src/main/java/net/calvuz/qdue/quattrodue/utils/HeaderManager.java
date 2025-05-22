package net.calvuz.qdue.quattrodue.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import net.calvuz.qdue.utils.Log;

/**
 * Utility class per gestire gli header mensili nell'adapter infinito.
 * Ottimizza il calcolo delle posizioni degli header per migliorare le prestazioni.
 */
public class HeaderManager {

    private static final String TAG = "HeaderManager";
    private static final boolean LOG_ENABLED = true;

    // Cache per le posizioni degli header calcolate
    private static final Map<String, Integer> headerPositionCache = new HashMap<>();

    // Data di riferimento per i calcoli
    private final LocalDate referenceDate;
    private final int initialPosition;

    /**
     * Costruttore.
     *
     * @param referenceDate Data di riferimento
     * @param initialPosition Posizione iniziale virtuale
     */
    public HeaderManager(LocalDate referenceDate, int initialPosition) {
        this.referenceDate = referenceDate;
        this.initialPosition = initialPosition;
    }

    /**
     * Determina se una posizione corrisponde a un header.
     *
     * @param position Posizione nell'adapter
     * @return true se è un header, false altrimenti
     */
    public boolean isHeaderPosition(int position) {
        if (position == 0) return true; // Prima posizione è sempre un header

        try {
            LocalDate currentDate = getDateForAdapterPosition(position);
            LocalDate previousDate = getDateForAdapterPosition(position - 1);

            if (currentDate == null || previousDate == null) {
                return false;
            }

            // È un header se siamo nel primo giorno di un nuovo mese
            return currentDate.getDayOfMonth() == 1 &&
                    !currentDate.getMonth().equals(previousDate.getMonth());
        } catch (Exception e) {
            if (LOG_ENABLED) {
                Log.e(TAG, "Errore nel determinare se la posizione " + position + " è un header: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Calcola la data per una posizione dell'adapter, considerando gli header.
     *
     * @param adapterPosition Posizione nell'adapter
     * @return Data corrispondente
     */
    public LocalDate getDateForAdapterPosition(int adapterPosition) {
        try {
            // Stima il numero di header prima di questa posizione
            int estimatedHeaders = Math.max(0, adapterPosition / 31);

            // Calcola la posizione del giorno effettiva
            int dayPosition = adapterPosition - estimatedHeaders;

            // Calcola l'offset dalla data di riferimento
            int offset = dayPosition - initialPosition;

            return referenceDate.plusDays(offset);
        } catch (Exception e) {
            if (LOG_ENABLED) {
                Log.e(TAG, "Errore nel calcolo della data per posizione " + adapterPosition + ": " + e.getMessage());
            }
            return referenceDate;
        }
    }

    /**
     * Calcola la posizione dell'adapter per una data specifica.
     *
     * @param date Data target
     * @return Posizione nell'adapter
     */
    public int getAdapterPositionForDate(LocalDate date) {
        try {
            // Calcola i giorni dalla data di riferimento
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, date);
            int dayPosition = initialPosition + (int) daysDifference;

            // Calcola quanti header ci sono prima di questa data
            int headersBefore = calculateHeadersBefore(date);

            return dayPosition + headersBefore;
        } catch (Exception e) {
            if (LOG_ENABLED) {
                Log.e(TAG, "Errore nel calcolo della posizione per data " + date + ": " + e.getMessage());
            }
            return initialPosition;
        }
    }

    /**
     * Calcola il numero di header prima di una data specifica.
     *
     * @param targetDate Data target
     * @return Numero di header
     */
    private int calculateHeadersBefore(LocalDate targetDate) {
        try {
            // Calcola il numero di mesi tra la data di riferimento e la target
            YearMonth refMonth = YearMonth.from(referenceDate);
            YearMonth targetMonth = YearMonth.from(targetDate);

            long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(refMonth, targetMonth);

            // Ogni mese ha un header, quindi il numero di header è circa uguale al numero di mesi
            return Math.abs((int) monthsBetween);
        } catch (Exception e) {
            if (LOG_ENABLED) {
                Log.e(TAG, "Errore nel calcolo degli header prima di " + targetDate + ": " + e.getMessage());
            }
            return 0;
        }
    }

    /**
     * Ottiene la data del mese per un header alla posizione specificata.
     *
     * @param headerPosition Posizione dell'header
     * @return Primo giorno del mese dell'header
     */
    public LocalDate getMonthForHeaderPosition(int headerPosition) {
        LocalDate date = getDateForAdapterPosition(headerPosition);
        if (date != null) {
            return date.withDayOfMonth(1);
        }
        return referenceDate.withDayOfMonth(1);
    }

    /**
     * Pulisce la cache delle posizioni degli header.
     */
    public static void clearCache() {
        headerPositionCache.clear();
        if (LOG_ENABLED) {
            Log.d(TAG, "Cache degli header pulita");
        }
    }

    /**
     * Ottiene statistiche sulla cache per il debug.
     *
     * @return Stringa con le statistiche
     */
    public static String getCacheStats() {
        return "Header cache size: " + headerPositionCache.size();
    }
}