package net.calvuz.qdue.smartshifts.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helper class for string operations in SmartShifts.
 *
 * Provides utility methods for:
 * - String formatting and manipulation
 * - File size formatting
 * - Preferences conversion
 * - Text validation and sanitization
 * - Localization helpers
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class StringHelper {

    private static final String TAG = "StringHelper";

    // Formatting patterns
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Private constructor to prevent instantiation
    private StringHelper() {
        throw new UnsupportedOperationException("StringHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // BASIC STRING OPERATIONS
    // ============================================

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not null and not empty
     */
    public static boolean isNotEmpty(@Nullable String str) {
        return !isEmpty(str);
    }

    /**
     * Get string value or default if null/empty
     */
    @NonNull
    public static String getOrDefault(@Nullable String str, @NonNull String defaultValue) {
        return isEmpty(str) ? defaultValue : str.trim();
    }

    /**
     * Safely trim string
     */
    @Nullable
    public static String safeTrim(@Nullable String str) {
        return str != null ? str.trim() : null;
    }

    /**
     * Capitalize first letter of string
     */
    @NonNull
    public static String capitalize(@NonNull String str) {
        if (isEmpty(str)) return str;

        String trimmed = str.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }

        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Capitalize each word in string
     */
    @NonNull
    public static String capitalizeWords(@NonNull String str) {
        if (isEmpty(str)) return str;

        String[] words = str.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            result.append(capitalize(words[i]));
        }

        return result.toString();
    }

    /**
     * Truncate string to max length with ellipsis
     */
    @NonNull
    public static String truncate(@NonNull String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }

        if (maxLength <= 3) {
            return str.substring(0, maxLength);
        }

        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Truncate string to max length with custom suffix
     */
    @NonNull
    public static String truncate(@NonNull String str, int maxLength, @NonNull String suffix) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }

        if (maxLength <= suffix.length()) {
            return str.substring(0, maxLength);
        }

        return str.substring(0, maxLength - suffix.length()) + suffix;
    }

    // ============================================
    // FILE SIZE FORMATTING
    // ============================================

    /**
     * Format file size in human readable format
     */
    @NonNull
    public static String formatFileSize(long bytes) {
        if (bytes < 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return String.format(Locale.getDefault(), "%.0f %s", size, units[unitIndex]);
        } else {
            return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex]);
        }
    }

    /**
     * Format file size with specific precision
     */
    @NonNull
    public static String formatFileSize(long bytes, int decimalPlaces) {
        if (bytes < 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return String.format(Locale.getDefault(), "%.0f %s", size, units[unitIndex]);
        } else {
            String format = "%." + decimalPlaces + "f %s";
            return String.format(Locale.getDefault(), format, size, units[unitIndex]);
        }
    }

    /**
     * Parse file size string to bytes
     */
    public static long parseFileSize(@NonNull String sizeString) {
        if (isEmpty(sizeString)) return 0;

        String cleanSize = sizeString.trim().toUpperCase();

        // Extract number and unit
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(B|KB|MB|GB|TB)?");
        java.util.regex.Matcher matcher = pattern.matcher(cleanSize);

        if (!matcher.matches()) return 0;

        double number = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);

        if (unit == null) unit = "B";

        switch (unit) {
            case "TB": return (long) (number * 1024 * 1024 * 1024 * 1024);
            case "GB": return (long) (number * 1024 * 1024 * 1024);
            case "MB": return (long) (number * 1024 * 1024);
            case "KB": return (long) (number * 1024);
            case "B":
            default:
                return (long) number;
        }
    }

    // ============================================
    // PREFERENCES CONVERSION
    // ============================================

    /**
     * Convert SharedPreferences to Map
     */
    @NonNull
    public static Map<String, Object> preferencesToMap(@NonNull SharedPreferences preferences) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Convert Map to SharedPreferences
     */
    public static void mapToPreferences(@NonNull Map<String, Object> map, @NonNull SharedPreferences.Editor editor) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value != null) {
                // Convert other types to string
                editor.putString(key, value.toString());
            }
        }
    }

    /**
     * Convert preferences to readable map with localized strings
     */
    @NonNull
    public static Map<String, String> preferencesToReadableMap(@NonNull SharedPreferences preferences, @NonNull Context context) {
        Map<String, String> readableMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Try to get localized key name
            String readableKey = getReadablePreferenceKey(key, context);
            String readableValue = getReadablePreferenceValue(key, value, context);

            readableMap.put(readableKey, readableValue);
        }

        return readableMap;
    }

    /**
     * Get readable preference key name
     */
    @NonNull
    private static String getReadablePreferenceKey(@NonNull String key, @NonNull Context context) {
        // Try to find a string resource for the key
        try {
            int resId = context.getResources().getIdentifier(key + "_title", "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        } catch (Exception e) {
            // Ignore and fall back to key
        }

        // Convert key to readable format
        return key.replace("_", " ").replace("smartshifts ", "").trim();
    }

    /**
     * Get readable preference value
     */
    @NonNull
    private static String getReadablePreferenceValue(@NonNull String key, @Nullable Object value, @NonNull Context context) {
        if (value == null) return "Non impostato";

        if (value instanceof Boolean) {
            return (Boolean) value ? "Attivo" : "Disattivo";
        }

        // Try to get localized value for known preference types
        if (key.contains("theme")) {
            return getThemeDisplayValue(value.toString(), context);
        } else if (key.contains("language")) {
            return getLanguageDisplayValue(value.toString(), context);
        } else if (key.contains("week_start")) {
            return getWeekStartDisplayValue(value.toString(), context);
        }

        return value.toString();
    }

    /**
     * Get display value for theme preference
     */
    @NonNull
    private static String getThemeDisplayValue(@NonNull String theme, @NonNull Context context) {
        switch (theme) {
            case "system": return "Sistema";
            case "light": return "Chiaro";
            case "dark": return "Scuro";
            case "auto": return "Automatico";
            default: return theme;
        }
    }

    /**
     * Get display value for language preference
     */
    @NonNull
    private static String getLanguageDisplayValue(@NonNull String language, @NonNull Context context) {
        switch (language) {
            case "system": return "Sistema";
            case "it": return "Italiano";
            case "en": return "English";
            case "fr": return "Français";
            case "de": return "Deutsch";
            case "es": return "Español";
            default: return language;
        }
    }

    /**
     * Get display value for week start preference
     */
    @NonNull
    private static String getWeekStartDisplayValue(@NonNull String weekStart, @NonNull Context context) {
        switch (weekStart) {
            case "monday": return "Lunedì";
            case "sunday": return "Domenica";
            case "saturday": return "Sabato";
            default: return weekStart;
        }
    }

    // ============================================
    // TEXT VALIDATION AND SANITIZATION
    // ============================================

    /**
     * Validate email format
     */
    public static boolean isValidEmail(@Nullable String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(@Nullable String phone) {
        if (isEmpty(phone)) return false;

        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return cleanPhone.matches("^[+]?[0-9]{10,15}$");
    }

    /**
     * Sanitize filename for safe file system usage
     */
    @NonNull
    public static String sanitizeFilename(@NonNull String filename) {
        if (isEmpty(filename)) return "untitled";

        // Replace invalid characters
        String sanitized = filename.replaceAll("[<>:\"/\\\\|?*]", "_");

        // Remove multiple consecutive underscores
        sanitized = sanitized.replaceAll("_+", "_");

        // Trim and limit length
        sanitized = sanitized.trim();
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        // Ensure it doesn't start/end with dots or spaces
        sanitized = sanitized.replaceAll("^[\\s\\.]+|[\\s\\.]+$", "");

        return isEmpty(sanitized) ? "untitled" : sanitized;
    }

    /**
     * Remove special characters from string
     */
    @NonNull
    public static String removeSpecialCharacters(@NonNull String str) {
        if (isEmpty(str)) return str;

        return SPECIAL_CHARS_PATTERN.matcher(str).replaceAll("");
    }

    /**
     * Normalize whitespace (collapse multiple spaces to single space)
     */
    @NonNull
    public static String normalizeWhitespace(@NonNull String str) {
        if (isEmpty(str)) return str;

        return WHITESPACE_PATTERN.matcher(str.trim()).replaceAll(" ");
    }

    /**
     * Clean string for search (lowercase, no special chars, normalized whitespace)
     */
    @NonNull
    public static String cleanForSearch(@NonNull String str) {
        if (isEmpty(str)) return str;

        return normalizeWhitespace(removeSpecialCharacters(str.toLowerCase()));
    }

    // ============================================
    // COMPARISON AND SORTING
    // ============================================

    /**
     * Compare strings ignoring case and locale
     */
    public static int compareIgnoreCase(@Nullable String str1, @Nullable String str2) {
        if (str1 == null && str2 == null) return 0;
        if (str1 == null) return -1;
        if (str2 == null) return 1;

        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY); // Ignore case

        return collator.compare(str1, str2);
    }

    /**
     * Check if string contains substring ignoring case
     */
    public static boolean containsIgnoreCase(@Nullable String str, @Nullable String substring) {
        if (str == null || substring == null) return false;

        return str.toLowerCase().contains(substring.toLowerCase());
    }

    /**
     * Check if string starts with prefix ignoring case
     */
    public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String prefix) {
        if (str == null || prefix == null) return false;

        return str.toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * Check if string ends with suffix ignoring case
     */
    public static boolean endsWithIgnoreCase(@Nullable String str, @Nullable String suffix) {
        if (str == null || suffix == null) return false;

        return str.toLowerCase().endsWith(suffix.toLowerCase());
    }

    // ============================================
    // FORMATTING HELPERS
    // ============================================

    /**
     * Format duration in milliseconds to human readable string
     */
    @NonNull
    public static String formatDuration(long durationMs) {
        if (durationMs < 0) return "0s";

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format(Locale.getDefault(), "%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%ds", seconds);
        }
    }

    /**
     * Format number with thousands separator
     */
    @NonNull
    public static String formatNumber(long number) {
        return String.format(Locale.getDefault(), "%,d", number);
    }

    /**
     * Format percentage with specified decimal places
     */
    @NonNull
    public static String formatPercentage(double percentage, int decimalPlaces) {
        String format = "%." + decimalPlaces + "f%%";
        return String.format(Locale.getDefault(), format, percentage);
    }

    /**
     * Join array of strings with separator
     */
    @NonNull
    public static String join(@NonNull String[] array, @NonNull String separator) {
        if (array.length == 0) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) result.append(separator);
            result.append(array[i]);
        }

        return result.toString();
    }

    /**
     * Join iterable of strings with separator
     */
    @NonNull
    public static String join(@NonNull Iterable<String> iterable, @NonNull String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String item : iterable) {
            if (!first) result.append(separator);
            result.append(item);
            first = false;
        }

        return result.toString();
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Generate random alphanumeric string
     */
    @NonNull
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();
    }

    /**
     * Count occurrences of substring in string
     */
    public static int countOccurrences(@NonNull String str, @NonNull String substring) {
        if (isEmpty(str) || isEmpty(substring)) return 0;

        int count = 0;
        int index = 0;

        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }

    /**
     * Reverse string
     */
    @NonNull
    public static String reverse(@NonNull String str) {
        if (isEmpty(str)) return str;

        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Check if string is numeric
     */
    public static boolean isNumeric(@Nullable String str) {
        if (isEmpty(str)) return false;

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extract numbers from string
     */
    @NonNull
    public static String extractNumbers(@NonNull String str) {
        if (isEmpty(str)) return "";

        return str.replaceAll("[^0-9]", "");
    }

    /**
     * Extract letters from string
     */
    @NonNull
    public static String extractLetters(@NonNull String str) {
        if (isEmpty(str)) return "";

        return str.replaceAll("[^a-zA-Z]", "");
    }

    /**
     * Mask string (show only first and last characters)
     */
    @NonNull
    public static String maskString(@NonNull String str, int visibleChars) {
        if (isEmpty(str) || str.length() <= visibleChars * 2) {
            return str;
        }

        String start = str.substring(0, visibleChars);
        String end = str.substring(str.length() - visibleChars);
        String middle = "*".repeat(str.length() - visibleChars * 2);

        return start + middle + end;
    }
}