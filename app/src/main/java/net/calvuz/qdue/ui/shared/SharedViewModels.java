package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.QDue.Debug.DEBUG_SHARED_VIEW_MODELS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.utils.Log;

/**
 * Unified data models shared between fragments.
 * <p>
 * This class provides a common data structure and conversion utilities
 * for both DayslistViewFragment and CalendarViewFragment, ensuring
 * consistent data handling across different UI representations.
 * <p>
 * Key components:
 * - ViewItem: Base class for all adapter items with type identification
 * - MonthHeader: Represents month title headers in the list/grid
 * - DayItem: Represents individual days with shift data
 * - EmptyItem: Placeholder items for calendar grid alignment
 * - LoadingItem: Loading indicators for infinite scroll
 * - DataConverter: Utility methods for converting raw data to view items
 *
 * @author calvuzs3
 */
public class SharedViewModels {

    // TAG
    private static final String TAG = "SharedViewModels";

    /**
     * Base class for all view items displayed in the RecyclerView adapter.
     * <p>
     * Provides type identification system to allow the adapter to handle
     * different types of content (headers, days, loading indicators, etc.)
     * in a unified manner.
     */
    public static abstract class ViewItem {

        /**
         * Enumeration of all possible view item types.
         * Used by adapters to determine how to display each item.
         */
        public enum Type {
            /**
             * Month header with title
             */
            HEADER,
            /**
             * Day with shift data
             */
            DAY,
            /**
             * Empty placeholder cell
             */
            EMPTY,
            /**
             * Loading indicator
             */
            LOADING
        }

        /**
         * Get the type of this view item.
         * Subclasses must implement this to identify their type.
         *
         * @return the type of this view item
         */
        public abstract Type getType();
    }

    /**
     * Represents a month header in the list/grid.
     * <p>
     * Contains the formatted month title and the normalized month date.
     * Handles automatic formatting based on current year for better UX.
     */
    public static class MonthHeader extends ViewItem {

        /**
         * Formatted title string for display
         */
        public final String title;

        /**
         * Normalized month date (always first day of month)
         */
        public final LocalDate monthDate;

        /**
         * Create a month header for the specified date.
         *
         * @param monthDate the month date (will be normalized to first day)
         */
        public MonthHeader(LocalDate monthDate) {
            final String mTAG = "MonthHeader: ";

            // Always normalize to first day of month
            this.monthDate = monthDate.withDayOfMonth(1);
            this.title = formatTitle(monthDate);

            if (DEBUG_SHARED_VIEW_MODELS)
                Log.d(TAG, mTAG + "Created header for: " + this.monthDate + " with title: " + this.title);
        }

        /**
         * Get the view item type.
         *
         * @return HEADER type
         */
        @Override
        public Type getType() {
            return Type.HEADER;
        }

        /**
         * Format the month title based on current year.
         * <p>
         * If the month is in the current year, shows only month name.
         * If different year, shows "Month Year" format.
         * Uses application locale for proper localization.
         *
         * @param date the date to format
         * @return formatted title string
         */
        private String formatTitle(LocalDate date) {
            final String mTAG = "formatTitle: ";

            try {
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = date.getYear() == today.getYear() ?
                        DateTimeFormatter.ofPattern("MMMM", QDue.getLocale()) :
                        DateTimeFormatter.ofPattern("MMMM yyyy", QDue.getLocale());

                String formattedTitle = date.format(formatter);

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Formatted " + date + " to: " + formattedTitle);

                return formattedTitle;
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error formatting date title: " + e.getMessage());
                // Fallback to simple string representation
                return date.getMonth().toString() + " " + date.getYear();
            }
        }
    }

    /**
     * Represents a single day with its shift data.
     * <p>
     * Contains the Day model object and additional metadata
     * for proper display and positioning within the month context.
     */
    public static class DayItem extends ViewItem {

        /**
         * The Day model containing shift information
         */
        public final Day day;

        /**
         * Normalized month date for this day
         */
        public final LocalDate monthDate;

        /**
         * Create a day item for the specified day and month context.
         *
         * @param day       the Day model with shift data
         * @param monthDate the month this day belongs to
         */
        public DayItem(Day day, LocalDate monthDate) {
            final String mTAG = "DayItem: ";

            this.day = day;
            this.monthDate = monthDate.withDayOfMonth(1);

            if (DEBUG_SHARED_VIEW_MODELS)
                Log.d(TAG, mTAG + "Created day item for: " + (day != null ? day.getDate() : "null"));
        }

        /**
         * Get the view item type.
         *
         * @return DAY type
         */
        @Override
        public Type getType() {
            return Type.DAY;
        }

        /**
         * Check if this day is today.
         *
         * @return true if this day represents today's date
         */
        public boolean isToday() {
            return day != null && day.getIsToday();
        }

        /**
         * Check if this day is a Sunday.
         * <p>
         * Uses ISO-8601 standard where Sunday = 7.
         *
         * @return true if this day is a Sunday
         */
        public boolean isSunday() {
            return day != null && day.getDayOfWeek() == 7;
        }
    }

    /**
     * Represents an empty placeholder cell.
     * <p>
     * Used in calendar grid layout to fill empty spaces
     * and maintain proper week alignment (7-day grid).
     */
    public static class EmptyItem extends ViewItem {

        /**
         * Get the view item type.
         *
         * @return EMPTY type
         */
        @Override
        public Type getType() {
            return Type.EMPTY;
        }
    }

    /**
     * Represents a loading indicator for infinite scroll.
     * <p>
     * Shows progress indicators when loading additional months
     * during infinite scroll operations.
     */
    public static class LoadingItem extends ViewItem {

        /**
         * Enumeration of loading indicator positions.
         */
        public enum LoadingType {
            /**
             * Loading indicator at top (loading previous months)
             */
            TOP,
            /**
             * Loading indicator at bottom (loading next months)
             */
            BOTTOM
        }

        /**
         * The position type of this loading indicator
         */
        public final LoadingType loadingType;

        /**
         * Localized message to display
         */
        public final String message;

        /**
         * Create a loading indicator of the specified type.
         *
         * @param type the loading position type
         */
        public LoadingItem(LoadingType type) {
            final String mTAG = "LoadingItem: ";

            this.loadingType = type;
            this.message = type == LoadingType.TOP ?
                    "Loading previous..." : "Loading next...";

            if (DEBUG_SHARED_VIEW_MODELS)
                Log.d(TAG, mTAG + "Created " + type + " loading item with message: " + message);
        }

        /**
         * Get the view item type.
         *
         * @return LOADING type
         */
        @Override
        public Type getType() {
            return Type.LOADING;
        }
    }

    /**
     * Utility class for converting raw data to view items.
     * <p>
     * Provides static methods to transform Day objects into properly
     * structured ViewItem lists for different display formats
     * (linear list vs. calendar grid).
     */
    public static class DataConverter {

        // TAG
        private static final String CONVERTER_TAG = TAG + "DataConverter: ";

        /**
         * Convert day data for linear list display (DayslistViewFragment).
         * <p>
         * Creates a simple linear structure with:
         * - Month header at the beginning
         * - All days in chronological order
         *
         * @param days      list of days in the month
         * @param monthDate the month these days belong to
         * @return list of view items for linear display
         */
        public static List<ViewItem> convertForDaysList(List<Day> days, LocalDate monthDate) {
            final String mTAG = CONVERTER_TAG + "convertForDaysList: ";

            List<ViewItem> items = new ArrayList<>();

            try {
                // Add month header
                items.add(new MonthHeader(monthDate));

                // Add all days in order
                for (Day day : days) {
                    items.add(new DayItem(day, monthDate));
                }

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Converted " + days.size() + " days to " + items.size() + " items for month: " + monthDate);

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error converting days list: " + e.getMessage());
            }

            return items;
        }

        /**
         * Convert day data for calendar grid display (CalendarViewFragment).
         * <p>
         * Creates a grid-aligned structure with:
         * - Month header spanning full width
         * - Empty cells to align first day with correct weekday
         * - All days in grid positions
         * - Empty cells to complete final week (multiple of 7)
         *
         * @param days      list of days in the month
         * @param monthDate the month these days belong to
         * @return list of view items for calendar grid display
         */
        public static List<ViewItem> convertForCalendar(List<Day> days, LocalDate monthDate) {
            final String mTAG = CONVERTER_TAG + "convertForCalendar: ";

            List<ViewItem> items = new ArrayList<>();

            try {
                // Add month header (spans full width)
                items.add(new MonthHeader(monthDate));

                if (days.isEmpty()) {
                    // Month without data - create basic structure
                    LocalDate firstDay = monthDate.withDayOfMonth(1);
                    addEmptyCellsForWeekStart(items, firstDay);

                    // Add empty days for the entire month
                    int daysInMonth = firstDay.lengthOfMonth();
                    for (int i = 1; i <= daysInMonth; i++) {
                        Day emptyDay = new Day(firstDay.withDayOfMonth(i));
                        items.add(new DayItem(emptyDay, monthDate));
                    }

                    if (DEBUG_SHARED_VIEW_MODELS)
                        Log.d(TAG, mTAG + "Created empty month structure with " + daysInMonth + " days");
                } else {
                    // Month with data
                    addEmptyCellsForWeekStart(items, days.get(0).getDate());

                    // Add all days with data
                    for (Day day : days) {
                        items.add(new DayItem(day, monthDate));
                    }

                    if (DEBUG_SHARED_VIEW_MODELS) {
                        Log.d(TAG, mTAG + "Added " + days.size() + " days with data");
                    }
                }

                // Complete the grid to multiples of 7 (full weeks)
                while (items.size() % 7 != 0) {
                    items.add(new EmptyItem());
                }

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Final calendar grid: " + items.size() + " items (" + (items.size() / 7) + " weeks)");

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error converting calendar data: " + e.getMessage());
            }

            return items;
        }

        /**
         * Add empty cells at the beginning to align the first day with correct weekday.
         * <p>
         *     TODO: change from mon to sun
         * Uses ISO-8601 standard where Monday = 1, Sunday = 7.
         * For calendar display, we need to align the first day of the month
         * with its correct weekday position.
         *
         * @param items    the list to add empty cells to
         * @param firstDay the first day of the month
         */
        private static void addEmptyCellsForWeekStart(List<ViewItem> items, LocalDate firstDay) {
            final String mTAG = CONVERTER_TAG + "addEmptyCellsForWeekStart: ";

            try {
                int dayOfWeek = firstDay.getDayOfWeek().getValue();

                // Calculate empty cells needed
                // Sunday (7) needs 0 empty cells, Monday (1) needs 1, etc.
                // TODO: do dayOfWeek - 1
                int emptyCells = (dayOfWeek == 7) ? 0 : dayOfWeek;

                for (int i = 0; i < emptyCells; i++) {
                    items.add(new EmptyItem());
                }

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Added " + emptyCells + " empty cells for first day: " + firstDay + " (weekday: " + dayOfWeek + ")");

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error adding empty cells for week start: " + e.getMessage());
            }
        }

        /**
         * Find the position of today's date in the view items list.
         * <p>
         * Searches through all DayItem instances to find the one
         * that represents today's date.
         *
         * @param items list of view items to search
         * @return index of today's position, or -1 if not found
         */
        public static int findTodayPosition(List<ViewItem> items) {
            final String mTAG = CONVERTER_TAG + "findTodayPosition: ";

            if (items == null || items.isEmpty()) {
                if (DEBUG_SHARED_VIEW_MODELS) Log.d(TAG, mTAG + "Items list is null or empty");
                return -1;
            }

            try {
                LocalDate today = LocalDate.now();

                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof DayItem dayItem) {
                        if (dayItem.day != null && dayItem.day.getDate().equals(today)) {
                            if (DEBUG_SHARED_VIEW_MODELS) {
                                Log.d(TAG, mTAG + "Found today at position: " + i);
                            }
                            return i;
                        }
                    }
                }

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Today not found in " + items.size() + " items");

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error finding today position: " + e.getMessage());
            }

            return -1;
        }

        /**
         * Count the number of day items in the list.
         * <p>
         * Utility method for DEBUG_SHARED_VIEW_MODELS
         * counting and validation.
         *
         * @param items list of view items
         * @return number of DayItem instances
         */
        public static int countDayItems(List<ViewItem> items) {
            final String mTAG = CONVERTER_TAG + "countDayItems: ";

            if (items == null) return 0;

            int count = 0;
            try {
                for (ViewItem item : items) {
                    if (item instanceof DayItem) {
                        count++;
                    }
                }

                if (DEBUG_SHARED_VIEW_MODELS)
                    Log.d(TAG, mTAG + "Counted " + count + " day items out of " + items.size() + " total items");

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error counting day items: " + e.getMessage());
            }

            return count;
        }

        /**
         * Get statistics about the view items list.
         * <p>
         * Utility method for DEBUG_SHARED_VIEW_MODELS
         * for statistical analysis and monitoring.
         *
         * @param items list of view items to analyze
         * @return formatted statistics string
         */
        public static String getItemStatistics(List<ViewItem> items) {
            final String mTAG = CONVERTER_TAG + "getItemStatistics: ";

            if (items == null) {
                return "No items (null list)";
            }

            try {
                int headers = 0, days = 0, empty = 0, loading = 0;

                for (ViewItem item : items) {
                    switch (item.getType()) {
                        case HEADER:
                            headers++;
                            break;
                        case DAY:
                            days++;
                            break;
                        case EMPTY:
                            empty++;
                            break;
                        case LOADING:
                            loading++;
                            break;
                    }
                }

                String stats = String.format("Total: %d (Headers: %d, Days: %d, Empty: %d, Loading: %d)",
                        items.size(), headers, days, empty, loading);

                if (DEBUG_SHARED_VIEW_MODELS) Log.d(TAG, mTAG + "Generated statistics: " + stats);

                return stats;

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error generating statistics: " + e.getMessage());
                return "Error generating statistics";
            }
        }

        /**
         * Validate the structure of a calendar grid.
         * <p>
         * Checks if the items form a valid calendar grid structure
         * with proper week alignment.
         *
         * @param items list of view items to validate
         * @return true if structure is valid for calendar display
         */
        public static boolean validateCalendarStructure(List<ViewItem> items) {
            final String mTAG = CONVERTER_TAG + "validateCalendarStructure: ";

            if (items == null || items.isEmpty()) {
                Log.w(TAG, mTAG + "Cannot validate null or empty items list");
                return false;
            }

            try {
                // Should start with a header
                if (!(items.get(0) instanceof MonthHeader)) {
                    Log.w(TAG, mTAG + "Calendar should start with MonthHeader");
                    return false;
                }

                // Total items should be multiple of 7 plus 1 (for header)
                if ((items.size() - 1) % 7 != 0) {
                    Log.w(TAG, mTAG + "Calendar grid not aligned to weeks: " + items.size() + " items");
                    return false;
                }

                if (DEBUG_SHARED_VIEW_MODELS) Log.d(TAG, mTAG + "Calendar structure is valid");

                return true;

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Error validating calendar structure: " + e.getMessage());
                return false;
            }
        }
    }
}