//package net.calvuz.qdue.smartshifts.utils;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
///**
// * Utility class for input validation
// */
//@Singleton
//public class ValidationHelper {
//
//    @Inject
//    public ValidationHelper() {}
//
//    /**
//     * Validate cycle length
//     */
//    public boolean isValidCycleLength(int cycleLength) {
//        return cycleLength >= 1 && cycleLength <= 365;
//    }
//
//    /**
//     * Validate pattern name
//     */
//    public boolean isValidPatternName(String name) {
//        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100;
//    }
//
//    /**
//     * Validate contact name
//     */
//    public boolean isValidContactName(String name) {
//        return name != null && name.trim().length() >= 1 && name.trim().length() <= 50;
//    }
//
//    /**
//     * Validate user ID format
//     */
//    public boolean isValidUserId(String userId) {
//        return userId != null && userId.trim().length() > 0;
//    }
//
//    /**
//     * Validate UUID format
//     */
//    public boolean isValidUUID(String uuid) {
//        if (uuid == null) return false;
//        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
//    }
//}
//
//// =====================================================================
////
package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helper class for validation operations in SmartShifts.
 *
 * Provides utility methods for:
 * - Settings validation
 * - Time and date validation
 * - String format validation
 * - Numeric range validation
 * - Business logic validation
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class ValidationHelper {

    private static final String TAG = "ValidationHelper";

    // Validation patterns
    private static final Pattern TIME_PATTERN = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );

    // Private constructor to prevent instantiation
    private ValidationHelper() {
        throw new UnsupportedOperationException("ValidationHelper is a utility class and cannot be instantiated");
    }

    // ============================================
    // SETTINGS VALIDATION
    // ============================================

    /**
     * Validate theme setting value
     */
    public static boolean isValidTheme(@Nullable String theme) {
        if (theme == null) return false;

        return theme.equals("system") ||
                theme.equals("light") ||
                theme.equals("dark") ||
                theme.equals("auto");
    }

    /**
     * Validate week start day setting
     */
    public static boolean isValidWeekStartDay(@Nullable String weekStart) {
        if (weekStart == null) return false;
        return weekStart.equals("monday") ||
                weekStart.equals("sunday") ||
                weekStart.equals("saturday");
    }

    /**
     * Validate reminder time in minutes
     */
    public static boolean isValidReminderTime(@Nullable String reminderTime) {
        if (reminderTime == null) return false;

        try {
            int minutes = Integer.parseInt(reminderTime);
            return minutes >= 0 && minutes <= 1440; // 0 to 24 hours
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate data retention days
     */
    public static boolean isValidRetentionDays(@Nullable String retentionDays) {
        if (retentionDays == null) return false;

        try {
            int days = Integer.parseInt(retentionDays);
            return days >= 1 && days <= 3650; // 1 day to 10 years
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate calendar view type
     */
    public static boolean isValidCalendarViewType(@Nullable String viewType) {
        if (viewType == null) return false;

        return viewType.equals("month") ||
                viewType.equals("week") ||
                viewType.equals("day") ||
                viewType.equals("agenda");
    }

    /**
     * Validate backup frequency
     */
    public static boolean isValidBackupFrequency(@Nullable String frequency) {
        if (frequency == null) return false;

        return frequency.equals("daily") ||
                frequency.equals("weekly") ||
                frequency.equals("monthly") ||
                frequency.equals("never");
    }

    /**
     * Validate notification priority
     */
    public static boolean isValidNotificationPriority(@Nullable String priority) {
        if (priority == null) return false;

        return priority.equals("low") ||
                priority.equals("normal") ||
                priority.equals("high") ||
                priority.equals("max");
    }

    // ============================================
    // TIME AND DATE VALIDATION
    // ============================================

    /**
     * Validate time format (HH:mm)
     */
    public static boolean isValidTimeFormat(@Nullable String timeString) {
        if (timeString == null) return false;

        if (!TIME_PATTERN.matcher(timeString).matches()) {
            return false;
        }

        try {
            DateTimeHelper.parseTime(timeString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validate date format (yyyy-MM-dd)
     */
    public static boolean isValidDateFormat(@Nullable String dateString) {
        if (dateString == null) return false;

        try {
            DateTimeHelper.parseDate(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validate time range (start before end)
     */
    public static boolean isValidTimeRange(@Nullable String startTime, @Nullable String endTime) {
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            return false;
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);

            // Allow night shifts that cross midnight
            return !start.equals(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validate shift duration in hours
     */
    public static boolean isValidShiftDuration(int durationHours) {
        return durationHours >= 1 && durationHours <= 24;
    }

    /**
     * Check if time is within working hours (configurable)
     */
    public static boolean isWorkingHours(@NonNull String timeString, @NonNull String startWorkTime, @NonNull String endWorkTime) {
        if (!isValidTimeFormat(timeString) || !isValidTimeFormat(startWorkTime) || !isValidTimeFormat(endWorkTime)) {
            return false;
        }

        try {
            LocalTime time = DateTimeHelper.parseTime(timeString);
            LocalTime startWork = DateTimeHelper.parseTime(startWorkTime);
            LocalTime endWork = DateTimeHelper.parseTime(endWorkTime);

            if (startWork.isBefore(endWork)) {
                // Same day working hours
                return !time.isBefore(startWork) && !time.isAfter(endWork);
            } else {
                // Night shift crossing midnight
                return !time.isBefore(startWork) || !time.isAfter(endWork);
            }
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // ============================================
    // STRING FORMAT VALIDATION
    // ============================================

    /**
     * Validate email format
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (StringHelper.isEmpty(email)) return false;

        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(@Nullable String phone) {
        if (StringHelper.isEmpty(phone)) return false;

        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Validate pattern name
     */
    public static boolean isValidPatternName(@Nullable String name) {
        if (StringHelper.isEmpty(name)) return false;

        String trimmed = name.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 50;
    }

    /**
     * Validate contact name
     */
    public static boolean isValidContactName(@Nullable String name) {
        if (StringHelper.isEmpty(name)) return false;

        String trimmed = name.trim();
        return trimmed.length() >= 1 && trimmed.length() <= 100;
    }

    /**
     * Validate file name
     */
    public static boolean isValidFileName(@Nullable String fileName) {
        if (StringHelper.isEmpty(fileName)) return false;

        String trimmed = fileName.trim();

        // Check length
        if (trimmed.length() < 1 || trimmed.length() > 255) {
            return false;
        }

        // Check for invalid characters
        return !trimmed.matches(".*[<>:\"/\\\\|?*].*");
    }

    // ============================================
    // NUMERIC RANGE VALIDATION
    // ============================================

    /**
     * Validate integer within range
     */
    public static boolean isValidIntegerRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validate double within range
     */
    public static boolean isValidDoubleRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Validate percentage (0-100)
     */
    public static boolean isValidPercentage(double percentage) {
        return isValidDoubleRange(percentage, 0.0, 100.0);
    }

    /**
     * Validate cycle length for shift patterns
     */
    public static boolean isValidCycleLength(int cycleLength) {
        return isValidIntegerRange(cycleLength, 1, 365); // 1 day to 1 year
    }

    /**
     * Validate team count
     */
    public static boolean isValidTeamCount(int teamCount) {
        return isValidIntegerRange(teamCount, 1, 20); // 1 to 20 teams
    }

    /**
     * Validate offset days for team assignments
     */
    public static boolean isValidOffsetDays(int offsetDays, int cycleLength) {
        return isValidIntegerRange(offsetDays, 0, cycleLength - 1);
    }

    // ============================================
    // BUSINESS LOGIC VALIDATION
    // ============================================

    /**
     * Validate shift pattern sequence
     */
    public static ValidationResult validateShiftPatternSequence(@Nullable String patternJson) {
        if (StringHelper.isEmpty(patternJson)) {
            return new ValidationResult(false, "Pattern JSON cannot be empty");
        }

        try {
            // Basic JSON validation
            if (!JsonHelper.isValidJson(patternJson)) {
                return new ValidationResult(false, "Invalid JSON format");
            }

            // TODO: Add specific pattern validation logic
            // - Check if pattern has valid cycle length
            // - Validate shift type references
            // - Check for continuous cycle compliance

            return new ValidationResult(true, "Pattern validation passed");

        } catch (Exception e) {
            return new ValidationResult(false, "Pattern validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate export configuration
     */
    public static ValidationResult validateExportConfiguration(@NonNull String format, @Nullable String destination) {
        if (StringHelper.isEmpty(format)) {
            return new ValidationResult(false, "Export format cannot be empty");
        }

        if (!isValidExportFormat(format)) {
            return new ValidationResult(false, "Unsupported export format: " + format);
        }

        if (StringHelper.isEmpty(destination)) {
            return new ValidationResult(false, "Export destination cannot be empty");
        }

        return new ValidationResult(true, "Export configuration is valid");
    }

    /**
     * Validate import file
     */
    public static ValidationResult validateImportFile(@Nullable java.io.File file) {
        if (file == null) {
            return new ValidationResult(false, "Import file cannot be null");
        }

        if (!file.exists()) {
            return new ValidationResult(false, "Import file does not exist");
        }

        if (!file.canRead()) {
            return new ValidationResult(false, "Cannot read import file");
        }

        if (file.length() == 0) {
            return new ValidationResult(false, "Import file is empty");
        }

        // Check file size (max 100MB)
        long maxSize = 100 * 1024 * 1024;
        if (file.length() > maxSize) {
            return new ValidationResult(false,
                    String.format("Import file too large: %s (max: %s)",
                            StringHelper.formatFileSize(file.length()),
                            StringHelper.formatFileSize(maxSize)));
        }

        return new ValidationResult(true, "Import file is valid");
    }

    /**
     * Validate backup configuration
     */
    public static ValidationResult validateBackupConfiguration(@NonNull String location, int retentionDays) {
        if (StringHelper.isEmpty(location)) {
            return new ValidationResult(false, "Backup location cannot be empty");
        }

        if (!isValidRetentionDays(String.valueOf(retentionDays))) {
            return new ValidationResult(false, "Invalid retention days: " + retentionDays);
        }

        // Check if location is writable
        java.io.File backupDir = new java.io.File(location);
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return new ValidationResult(false, "Cannot create backup directory: " + location);
            }
        }

        if (!backupDir.canWrite()) {
            return new ValidationResult(false, "Cannot write to backup directory: " + location);
        }

        return new ValidationResult(true, "Backup configuration is valid");
    }

    /**
     * Validate team contact information
     */
    public static ValidationResult validateTeamContact(@Nullable String name, @Nullable String phone, @Nullable String email) {
        if (!isValidContactName(name)) {
            return new ValidationResult(false, "Invalid contact name");
        }

        // Phone and email are optional, but if provided must be valid
        if (StringHelper.isNotEmpty(phone) && !isValidPhoneNumber(phone)) {
            return new ValidationResult(false, "Invalid phone number format");
        }

        if (StringHelper.isNotEmpty(email) && !isValidEmail(email)) {
            return new ValidationResult(false, "Invalid email format");
        }

        return new ValidationResult(true, "Contact information is valid");
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Check if export format is supported
     */
    private static boolean isValidExportFormat(@NonNull String format) {
        return format.equals("json") ||
                format.equals("csv") ||
                format.equals("xml") ||
                format.equals("ics") ||
                format.equals("xlsx");
    }

    /**
     * Validate password strength (for future security features)
     */
    public static ValidationResult validatePasswordStrength(@Nullable String password) {
        if (StringHelper.isEmpty(password)) {
            return new ValidationResult(false, "Password cannot be empty");
        }

        String trimmed = password.trim();

        if (trimmed.length() < 8) {
            return new ValidationResult(false, "Password must be at least 8 characters long");
        }

        if (trimmed.length() > 128) {
            return new ValidationResult(false, "Password must be less than 128 characters long");
        }

        boolean hasLower = trimmed.matches(".*[a-z].*");
        boolean hasUpper = trimmed.matches(".*[A-Z].*");
        boolean hasDigit = trimmed.matches(".*\\d.*");
        boolean hasSpecial = trimmed.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        int strength = 0;
        if (hasLower) strength++;
        if (hasUpper) strength++;
        if (hasDigit) strength++;
        if (hasSpecial) strength++;

        if (strength < 3) {
            return new ValidationResult(false, "Password must contain at least 3 of: lowercase, uppercase, digits, special characters");
        }

        return new ValidationResult(true, "Password strength is adequate");
    }

    /**
     * Validate URL format (for cloud storage URLs)
     */
    public static boolean isValidUrl(@Nullable String url) {
        if (StringHelper.isEmpty(url)) return false;

        try {
            new java.net.URL(url.trim());
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * Validate JSON Web Token format (for future API integration)
     */
    public static boolean isValidJwtToken(@Nullable String token) {
        if (StringHelper.isEmpty(token)) return false;

        String trimmed = token.trim();

        // JWT should have 3 parts separated by dots
        String[] parts = trimmed.split("\\.");
        if (parts.length != 3) return false;

        // Each part should be base64 encoded (basic check)
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate cron expression (for future scheduling features)
     */
    public static boolean isValidCronExpression(@Nullable String cronExpression) {
        if (StringHelper.isEmpty(cronExpression)) return false;

        String trimmed = cronExpression.trim();

        // Basic validation: 5 or 6 space-separated fields
        String[] fields = trimmed.split("\\s+");
        return fields.length == 5 || fields.length == 6;
    }

    // ============================================
    // VALIDATION RESULT CLASS
    // ============================================

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String field;

        public ValidationResult(boolean valid, String message) {
            this(valid, message, null);
        }

        public ValidationResult(boolean valid, String message, String field) {
            this.valid = valid;
            this.message = message;
            this.field = field;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getField() {
            return field;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s', field='%s'}",
                    valid, message, field);
        }
    }

    // ============================================
    // BULK VALIDATION METHODS
    // ============================================

    /**
     * Validate multiple fields and return all errors
     */
    public static MultiValidationResult validateMultiple(ValidationCheck... checks) {
        MultiValidationResult result = new MultiValidationResult();

        for (ValidationCheck check : checks) {
            ValidationResult fieldResult = check.validate();
            if (!fieldResult.isValid()) {
                result.addError(fieldResult);
            }
        }

        return result;
    }

    /**
     * Interface for validation checks
     */
    public interface ValidationCheck {
        ValidationResult validate();
    }

    /**
     * Multiple validation result container
     */
    public static class MultiValidationResult {
        private final java.util.List<ValidationResult> errors = new java.util.ArrayList<>();

        public void addError(ValidationResult error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<ValidationResult> getErrors() {
            return new java.util.ArrayList<>(errors);
        }

        public String getErrorSummary() {
            if (errors.isEmpty()) return "No errors";

            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                if (i > 0) summary.append("; ");
                summary.append(errors.get(i).getMessage());
            }

            return summary.toString();
        }

        public int getErrorCount() {
            return errors.size();
        }
    }

    // ============================================
    // COMMON VALIDATION PATTERNS
    // ============================================

    /**
     * Create validation check for required string field
     */
    public static ValidationCheck requiredString(String fieldName, String value) {
        return () -> {
            if (StringHelper.isEmpty(value)) {
                return new ValidationResult(false, fieldName + " è obbligatorio", fieldName);
            }
            return new ValidationResult(true, "OK", fieldName);
        };
    }

    /**
     * Create validation check for string length
     */
    public static ValidationCheck stringLength(String fieldName, String value, int minLength, int maxLength) {
        return () -> {
            if (StringHelper.isEmpty(value)) {
                return new ValidationResult(true, "OK", fieldName); // Let required check handle empty
            }

            int length = value.trim().length();
            if (length < minLength || length > maxLength) {
                return new ValidationResult(false,
                        String.format(Locale.getDefault(),"%s deve essere tra %d e %d caratteri", fieldName, minLength, maxLength),
                        fieldName);
            }

            return new ValidationResult(true, "OK", fieldName);
        };
    }

    /**
     * Create validation check for numeric range
     */
    public static ValidationCheck numericRange(String fieldName, String value, int min, int max) {
        return () -> {
            if (StringHelper.isEmpty(value)) {
                return new ValidationResult(true, "OK", fieldName); // Let required check handle empty
            }

            try {
                int intValue = Integer.parseInt(value.trim());
                if (!isValidIntegerRange(intValue, min, max)) {
                    return new ValidationResult(false,
                            String.format("%s deve essere tra %d e %d", fieldName, min, max),
                            fieldName);
                }
                return new ValidationResult(true, "OK", fieldName);
            } catch (NumberFormatException e) {
                return new ValidationResult(false, fieldName + " deve essere un numero valido", fieldName);
            }
        };
    }
}
