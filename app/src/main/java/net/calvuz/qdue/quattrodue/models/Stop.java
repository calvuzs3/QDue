package net.calvuz.qdue.quattrodue.models;
import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Rappresenta una fermata degli impianti in un periodo specifico.
 * Una fermata è definita da una data di inizio, una data di fine,
 * e dai turni coinvolti.
 *
 * @author Luke (originale)
 * @author Aggiornato 21/05/2025
 */
public final class Stop {

    /* TAG */
    private static final String TAG = Stop.class.getSimpleName();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* Proprietà della fermata */
    // Data di inizio
    public final int year;
    public final int month;
    public final int day;
    public final int shift;

    // Data di fine
    public final int endyear;
    public final int endmonth;
    public final int endday;
    public final int endshift;

    // Date come oggetti LocalDate per facilitare i calcoli
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Crea una nuova fermata con le date di inizio e fine specificate.
     *
     * @param year Anno di inizio
     * @param month Mese di inizio (1-12)
     * @param day Giorno di inizio (1-31)
     * @param shift Turno di inizio (1-3)
     * @param endyear Anno di fine
     * @param endmonth Mese di fine (1-12)
     * @param endday Giorno di fine (1-31)
     * @param endshift Turno di fine (1-3)
     */
    public Stop(int year, int month, int day, int shift,
                int endyear, int endmonth, int endday, int endshift) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.shift = shift;
        this.endyear = endyear;
        this.endmonth = endmonth;
        this.endday = endday;
        this.endshift = endshift;

        // Creiamo oggetti LocalDate per facilitare i calcoli
        this.startDate = LocalDate.of(year, month, day);

        // Gestiamo il caso del giorno 32 (usato come workaround nella vecchia implementazione)
        int adjustedEndDay = endday;
        if (adjustedEndDay > 31) {
            // Aggiungiamo un mese e impostiamo il giorno a 1
            LocalDate nextMonth = LocalDate.of(endyear, endmonth, 1).plusMonths(1);
            this.endDate = nextMonth;
        } else {
            this.endDate = LocalDate.of(endyear, endmonth, adjustedEndDay);
        }
    }

    /**
     * Alternativo costruttore che utilizza oggetti LocalDate.
     *
     * @param startDate Data di inizio
     * @param startShift Turno di inizio (1-3)
     * @param endDate Data di fine
     * @param endShift Turno di fine (1-3)
     */
    public Stop(LocalDate startDate, int startShift, LocalDate endDate, int endShift) {
        this.year = startDate.getYear();
        this.month = startDate.getMonthValue();
        this.day = startDate.getDayOfMonth();
        this.shift = startShift;

        this.endyear = endDate.getYear();
        this.endmonth = endDate.getMonthValue();
        this.endday = endDate.getDayOfMonth();
        this.endshift = endShift;

        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Verifica se una data specifica è compresa nella fermata.
     *
     * @param date Data da verificare
     * @return true se la data è compresa nella fermata, false altrimenti
     */
    public boolean includes(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Verifica se questa fermata si sovrappone con un'altra.
     *
     * @param other Altra fermata da verificare
     * @return true se c'è sovrapposizione, false altrimenti
     */
    public boolean overlaps(Stop other) {
        return includes(other.startDate) || includes(other.endDate) ||
                other.includes(startDate) || other.includes(endDate);
    }

    /**
     * Calcola la durata della fermata in giorni.
     *
     * @return Numero di giorni della fermata
     */
    public int getDurationInDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }

    /**
     * Restituisce la data di inizio come oggetto LocalDate.
     *
     * @return Data di inizio
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Restituisce la data di fine come oggetto LocalDate.
     *
     * @return Data di fine
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "{" +
                startDate.format(DATE_FORMATTER) + "(" + shift + ")" +
                " - " +
                endDate.format(DATE_FORMATTER) + "(" + endshift + ")" +
                "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Stop other = (Stop) obj;
        return year == other.year &&
                month == other.month &&
                day == other.day &&
                shift == other.shift &&
                endyear == other.endyear &&
                endmonth == other.endmonth &&
                endday == other.endday &&
                endshift == other.endshift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, shift, endyear, endmonth, endday, endshift);
    }
}