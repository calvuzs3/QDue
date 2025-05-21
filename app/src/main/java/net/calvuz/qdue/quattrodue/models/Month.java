package net.calvuz.qdue.quattrodue.models;

import androidx.annotation.NonNull;

import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Rappresenta un mese del calendario con i suoi giorni e fermate programmate.
 *
 * @author Luke (originale)
 * @author Aggiornato 21/05/2025
 */
public class Month {

    /* TAG */
    private static final String TAG = Month.class.getSimpleName();

    /* Logging configuration */
    private static final boolean LOG_ENABLED = true;
    private static final boolean LOG_EVENTS = true;
    private static final boolean LOG_STOPS = true;

    /* Proprietà del mese */
    private final LocalDate firstDayOfMonth;
    private boolean isCurrent;
    private List<Day> daysList;

    /**
     * Lista statica delle fermate programmate.
     * NOTA: In un'implementazione ideale, queste informazioni dovrebbero essere caricate
     * da un database o da un file di configurazione.
     */
    private static final List<Stop> STOPS = new ArrayList<>();

    static {
        // 2018
        STOPS.add(new Stop(2018, 8, 11, 3, 2018, 8, 20, 1));
        STOPS.add(new Stop(2018, 12, 21, 3, 2018, 12, 32, 1));
        // 2019
        STOPS.add(new Stop(2019, 1, 1, 1, 2019, 1, 3, 1));
        STOPS.add(new Stop(2019, 4, 19, 3, 2019, 4, 23, 1));
        STOPS.add(new Stop(2019, 6, 20, 3, 2019, 6, 25, 1));
        STOPS.add(new Stop(2019, 8, 13, 3, 2019, 8, 21, 1));
        STOPS.add(new Stop(2019, 12, 24, 3, 2019, 12, 27, 1));
        STOPS.add(new Stop(2019, 12, 31, 3, 2019, 12, 32, 1));
        // Aggiungi qui le fermate per gli anni successivi
    }

    /**
     * Costruttore che inizializza un mese a partire da una data.
     * La data viene impostata al primo giorno del mese.
     *
     * @param date Data di riferimento per il mese
     */
    public Month(LocalDate date) {
        // Impostiamo sempre la data al primo giorno del mese
        this.firstDayOfMonth = date.withDayOfMonth(1);
        this.daysList = new ArrayList<>();

        int daysInMonth = YearMonth.of(date.getYear(), date.getMonth()).lengthOfMonth();

        if (LOG_ENABLED) {
            Log.d(TAG, firstDayOfMonth + " (giorni nel mese: " + daysInMonth + ")");
        }

        // Verifica se è il mese corrente
        LocalDate today = LocalDate.now();
        setIsCurrent(today.getYear() == date.getYear() &&
                today.getMonth() == date.getMonth());
    }

    /**
     * Verifica se questo mese è il mese corrente.
     *
     * @return true se è il mese corrente, false altrimenti
     */
    public boolean isCurrent() {
        return isCurrent;
    }

    /**
     * Imposta se questo mese è il mese corrente.
     *
     * @param isCurrent true se è il mese corrente, false altrimenti
     */
    private void setIsCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;

        if (isCurrent && daysList != null && !daysList.isEmpty()) {
            // Se è il mese corrente, impostiamo il giorno corrente
            LocalDate today = LocalDate.now();
            setToday(today.getDayOfMonth());

            if (LOG_ENABLED) {
                Log.d(TAG, "setIsCurrent: " + today.getDayOfMonth());
            }
        }
    }

    /**
     * Imposta il giorno corrente nel mese.
     *
     * @param dayOfMonth Giorno del mese (1-31)
     */
    public void setToday(int dayOfMonth) {
        if (daysList != null && dayOfMonth > 0 && dayOfMonth <= daysList.size()) {
            daysList.get(dayOfMonth - 1).setIsToday(true);

            if (LOG_ENABLED) {
                Log.d(TAG, "setToday: " + dayOfMonth);
            }
        }
    }

    /**
     * Restituisce la lista dei giorni del mese.
     *
     * @return Lista immutabile dei giorni
     */
    public List<Day> getDaysList() {
        return daysList != null ? Collections.unmodifiableList(daysList) : new ArrayList<>();
    }

    /**
     * Imposta la lista dei giorni del mese.
     *
     * @param daysList Lista dei giorni
     */
    public void setDaysList(List<Day> daysList) {
        this.daysList = new ArrayList<>(daysList);

        if (LOG_ENABLED) {
            Log.d(TAG, "setDaysList: lista giorni impostata");
        }

        // Se è il mese corrente, impostiamo il giorno corrente
        if (isCurrent()) {
            setToday(LocalDate.now().getDayOfMonth());
        }
    }

    /**
     * Configura le fermate per questo mese.
     * Carica le fermate predefinite e le applica ai giorni del mese.
     */
    public void setStops() {
        processStops();
    }

    /**
     * Elabora le fermate programmate per questo mese.
     * Questo metodo recupera le fermate pertinenti e le applica ai giorni del mese.
     */
    private void processStops() {
        if (LOG_STOPS) {
            Log.v(TAG, "processStops: inizio elaborazione fermate");
        }

        // Recupera le fermate per il mese corrente
        List<Stop> monthStops = getStopsForMonth(firstDayOfMonth.getYear(),
                firstDayOfMonth.getMonthValue());

        for (Stop stop : monthStops) {
            if (LOG_STOPS) {
                Log.d(TAG, "processStops: " + stop);
            }

            try {
                applyStop(stop);
            } catch (Exception e) {
                Log.e(TAG, "Errore durante l'applicazione della fermata: " + e.getMessage());
            }
        }

        if (LOG_STOPS) {
            Log.v(TAG, "processStops: fine elaborazione fermate");
        }
    }

    /**
     * Applica una fermata ai giorni del mese.
     *
     * @param stop Fermata da applicare
     */
    private void applyStop(Stop stop) {
        // Conta i turni
        int shiftCounter = stop.shift;
        // Conta i giorni
        int dayCounter = stop.day;

        // Continua finché non raggiungiamo il giorno finale o la fine del mese
        while (dayCounter < stop.endday && dayCounter <= daysList.size()) {
            // Ottieni il giorno
            Day day = daysList.get(dayCounter - 1);

            if (day != null) {
                if (LOG_STOPS) {
                    Log.d(TAG, "applyStop: " + day);
                }

                // Imposta i turni come fermate
                while (shiftCounter < 4) {
                    day.setStop(shiftCounter);
                    shiftCounter++;
                }

                // Resetta il contatore dei turni per il giorno successivo
                shiftCounter = 1;
            }

            dayCounter++;
        }
    }

    /**
     * Genera e inizializza la lista dei giorni del mese.
     * Questo metodo può essere chiamato dopo aver creato l'oggetto Month
     * per popolare la lista dei giorni.
     */
    public void generateDays() {
        int daysInMonth = YearMonth.of(firstDayOfMonth.getYear(), firstDayOfMonth.getMonth()).lengthOfMonth();
        daysList = new ArrayList<>(daysInMonth);

        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = LocalDate.of(firstDayOfMonth.getYear(), firstDayOfMonth.getMonth(), i);
            daysList.add(new Day(date));
        }

        // Se è il mese corrente, impostiamo il giorno corrente
        if (isCurrent()) {
            setToday(LocalDate.now().getDayOfMonth());
        }
    }

    /**
     * Recupera le fermate programmate per un determinato mese.
     *
     * @param year  Anno
     * @param month Mese (1-12)
     * @return Lista delle fermate per il mese specificato
     */
    private List<Stop> getStopsForMonth(int year, int month) {
        if (LOG_STOPS) {
            Log.d(TAG, "getStopsForMonth(" + year + ", " + month + ")");
        }

        // Filtra le fermate per l'anno e il mese specificati
        return STOPS.stream()
                .filter(stop -> stop.year == year && stop.month == month)
                .collect(Collectors.toList());
    }

    /**
     * Restituisce il primo giorno del mese.
     *
     * @return Data del primo giorno del mese
     */
    public LocalDate getFirstDayOfMonth() {
        return firstDayOfMonth;
    }

    /**
     * Restituisce l'anno del mese.
     *
     * @return Anno
     */
    public int getYear() {
        return firstDayOfMonth.getYear();
    }

    /**
     * Restituisce il numero del mese (1-12).
     *
     * @return Numero del mese
     */
    public int getMonthValue() {
        return firstDayOfMonth.getMonthValue();
    }

    /**
     * Restituisce il nome del mese.
     *
     * @return Nome del mese
     */
    public String getMonthName() {
        return firstDayOfMonth.getMonth().toString();
    }

    /**
     * Restituisce una rappresentazione testuale del mese (es. "Maggio 2025").
     *
     * @return Stringa rappresentativa del mese
     */
    public String getTitle() {
        return firstDayOfMonth.getMonth().toString() + " " + firstDayOfMonth.getYear();
    }

    @Override
    public String toString() {
        return TAG + "{" + firstDayOfMonth + ", giorni: " +
                (daysList != null ? daysList.size() : 0) + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Month other = (Month) obj;
        return Objects.equals(firstDayOfMonth, other.firstDayOfMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstDayOfMonth);
    }
}