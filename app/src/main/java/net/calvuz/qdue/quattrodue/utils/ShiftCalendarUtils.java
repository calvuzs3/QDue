package net.calvuz.qdue.quattrodue.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.Month;

/**
 * Classe di utilità per lavorare con date, turni e squadre.
 */
public final class ShiftCalendarUtils {

    // TAG
    private static final String TAG = "ShiftCalendarUtils";

    // Non permettere istanziazione
    private ShiftCalendarUtils() {}

    /**
     * Formattatori per date
     */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    /**
     * Crea un mese con tutti i suoi giorni.
     *
     * @param year Anno
     * @param month Mese (1-12)
     * @return Oggetto Month completo
     */
    public static Month createMonth(int year, int month) {
        LocalDate date = LocalDate.of(year, month, 1);
        Month monthObj = new Month(date);

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        List<Day> days = new ArrayList<>(daysInMonth);

        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new Day(LocalDate.of(year, month, i)));
        }

        monthObj.setDaysList(days);

        // Configura le fermate
        monthObj.setStops();

        return monthObj;
    }

    /**
     * Ottiene il mese corrente.
     *
     * @return Oggetto Month per il mese corrente
     */
    public static Month getCurrentMonth() {
        LocalDate today = LocalDate.now();
        return createMonth(today.getYear(), today.getMonthValue());
    }

    /**
     * Ottiene il mese successivo a quello specificato.
     *
     * @param current Mese corrente
     * @return Mese successivo
     */
    public static Month getNextMonth(Month current) {
        LocalDate date = current.getFirstDayOfMonth().plusMonths(1);
        return createMonth(date.getYear(), date.getMonthValue());
    }

    /**
     * Ottiene il mese precedente a quello specificato.
     *
     * @param current Mese corrente
     * @return Mese precedente
     */
    public static Month getPreviousMonth(Month current) {
        LocalDate date = current.getFirstDayOfMonth().minusMonths(1);
        return createMonth(date.getYear(), date.getMonthValue());
    }

    /**
     * Verifica se una data è un weekend (sabato o domenica).
     *
     * @param date Data da verificare
     * @return true se è weekend, false altrimenti
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Ottiene il nome del giorno della settimana in italiano.
     *
     * @param date Data
     * @return Nome del giorno
     */
    public static String getDayOfWeekName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
    }

    /**
     * Ottiene il nome breve del giorno della settimana in italiano.
     *
     * @param date Data
     * @return Nome breve del giorno
     */
    public static String getShortDayOfWeekName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ITALIAN);
    }

    /**
     * Trova la prossima data in cui un determinato giorno della settimana si verifica.
     *
     * @param from Data di partenza
     * @param dayOfWeek Giorno della settimana da trovare
     * @return Data del prossimo giorno della settimana specificato
     */
    public static LocalDate getNextDayOfWeek(LocalDate from, DayOfWeek dayOfWeek) {
        return from.with(TemporalAdjusters.next(dayOfWeek));
    }

    /**
     * Ottiene una lista di tutte le date in cui un determinato giorno della settimana
     * si verifica in un mese specifico.
     *
     * @param year Anno
     * @param month Mese (1-12)
     * @param dayOfWeek Giorno della settimana
     * @return Lista di date
     */
    public static List<LocalDate> getDatesOfDayInMonth(int year, int month, DayOfWeek dayOfWeek) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate firstDayOfWeek = date.with(TemporalAdjusters.firstInMonth(dayOfWeek));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        LocalDate current = firstDayOfWeek;
        while (!current.isAfter(lastDayOfMonth)) {
            dates.add(current);
            current = current.plusWeeks(1);
        }

        return dates;
    }
}

