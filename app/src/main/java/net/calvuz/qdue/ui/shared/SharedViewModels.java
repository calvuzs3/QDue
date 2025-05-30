// ==================== MODELLI DATI UNIFICATI ====================

package net.calvuz.qdue.ui.shared;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.quattrodue.models.Day;

/**
 * Modelli dati condivisi tra i due fragment.
 */
public class SharedViewModels {

    // Elemento base per adapter
    public static abstract class ViewItem {
        public enum Type { HEADER, DAY, EMPTY, LOADING }
        public abstract Type getType();
    }

    // Header del mese
    public static class MonthHeader extends ViewItem {
        public final String title;
        public final LocalDate monthDate;

        public MonthHeader(LocalDate monthDate) {
            this.monthDate = monthDate.withDayOfMonth(1);
            this.title = formatTitle(monthDate);
        }

        @Override public Type getType() { return Type.HEADER; }

        private String formatTitle(LocalDate date) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = date.getYear() == today.getYear() ?
                    DateTimeFormatter.ofPattern("MMMM", QDue.getLocale()) :
                    DateTimeFormatter.ofPattern("MMMM yyyy", QDue.getLocale());
            return date.format(formatter);
        }
    }

    // Giorno con dati
    public static class DayItem extends ViewItem {
        public final Day day;
        public final LocalDate monthDate;

        public DayItem(Day day, LocalDate monthDate) {
            this.day = day;
            this.monthDate = monthDate.withDayOfMonth(1);
        }

        @Override public Type getType() { return Type.DAY; }

        public boolean isToday() { return day.getIsToday(); }
        public boolean isSunday() { return day.getDayOfWeek() == 7; }
    }

    // Cella vuota (per calendario)
    public static class EmptyItem extends ViewItem {
        @Override public Type getType() { return Type.EMPTY; }
    }

    // Indicatore caricamento
    public static class LoadingItem extends ViewItem {
        public enum LoadingType { TOP, BOTTOM }

        public final LoadingType loadingType;
        public final String message;

        public LoadingItem(LoadingType type) {
            this.loadingType = type;
            this.message = type == LoadingType.TOP ?
                    "Caricamento precedenti..." : "Caricamento successivi...";
        }

        @Override public Type getType() { return Type.LOADING; }
    }

    // Convertitore dati
    public static class DataConverter {

        /**
         * Converte per lista giorni (DayslistViewFragment).
         */
        public static List<ViewItem> convertForDaysList(List<Day> days, LocalDate monthDate) {
            List<ViewItem> items = new ArrayList<>();
            items.add(new MonthHeader(monthDate));
            for (Day day : days) {
                items.add(new DayItem(day, monthDate));
            }
            return items;
        }

        /**
         * Converte per calendario (CalendarViewFragment).
         */
        public static List<ViewItem> convertForCalendar(List<Day> days, LocalDate monthDate) {
            List<ViewItem> items = new ArrayList<>();

            if (days.isEmpty()) {
                // Mese senza dati - crea struttura base
                LocalDate firstDay = monthDate.withDayOfMonth(1);
                addEmptyCellsForWeekStart(items, firstDay);

                int daysInMonth = firstDay.lengthOfMonth();
                for (int i = 1; i <= daysInMonth; i++) {
                    Day emptyDay = new Day(firstDay.withDayOfMonth(i));
                    items.add(new DayItem(emptyDay, monthDate));
                }
            } else {
                // Mese con dati
                addEmptyCellsForWeekStart(items, days.get(0).getDate());
                for (Day day : days) {
                    items.add(new DayItem(day, monthDate));
                }
            }

            // Completa la griglia (multipli di 7)
            while (items.size() % 7 != 0) {
                items.add(new EmptyItem());
            }

            return items;
        }

        private static void addEmptyCellsForWeekStart(List<ViewItem> items, LocalDate firstDay) {
            int dayOfWeek = firstDay.getDayOfWeek().getValue();
            int emptyCells = (dayOfWeek == 7) ? 0 : dayOfWeek;
            for (int i = 0; i < emptyCells; i++) {
                items.add(new EmptyItem());
            }
        }

        public static int findTodayPosition(List<ViewItem> items) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof DayItem) {
                    DayItem dayItem = (DayItem) items.get(i);
                    if (dayItem.day.getDate().equals(today)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}