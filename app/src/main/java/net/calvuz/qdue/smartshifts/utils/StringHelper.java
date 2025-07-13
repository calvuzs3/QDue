package net.calvuz.qdue.smartshifts.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for string operations
 */
@Singleton
public class StringHelper {

    @Inject
    public StringHelper() {}

    /**
     * Check if string is null or empty
     */
    public boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Capitalize first letter
     */
    public String capitalize(String str) {
        if (isEmpty(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Truncate string to max length with ellipsis
     */
    public String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Format time duration in minutes to readable string
     */
    public String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + "h";
        }

        return hours + "h " + remainingMinutes + "min";
    }

    /**
     * Join strings with separator
     */
    public String join(String separator, String... strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (!isEmpty(strings[i])) {
                if (builder.length() > 0) {
                    builder.append(separator);
                }
                builder.append(strings[i]);
            }
        }

        return builder.toString();
    }
}
