package net.calvuz.qdue.smartshifts.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.smartshifts.utils.validation.ValidationResult;
import net.calvuz.qdue.smartshifts.utils.validation.ValidationError;
import net.calvuz.qdue.smartshifts.utils.validation.MultiValidationResult;
import net.calvuz.qdue.smartshifts.utils.validation.ValidationCheck;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Generic validation helper for common validation operations.
 * <p>
 * Handles validation of:
 * - Basic data types (strings, numbers, emails, etc.)
 * - Time and date formats
 * - File operations
 * - Settings and preferences
 * - Generic business rules
 * <p>
 * For shift-specific validations, use ShiftValidationHelper.
 * All validation methods return ValidationResult with error codes for i18n support.
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
     * Validate time range (start before end, or allow night shifts)
     */
    public static boolean isValidTimeRange(@Nullable String startTime, @Nullable String endTime) {
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            return false;
        }

        try {
            LocalTime start = DateTimeHelper.parseTime(startTime);
            LocalTime end = DateTimeHelper.parseTime(endTime);

            // Allow night shifts that cross midnight, but not identical times
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
     * Validate pattern name (used by both generic and shift validations)
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
     * Validate cycle length for patterns
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
    // FILE AND EXPORT VALIDATION
    // ============================================

    /**
     * Validate export configuration
     */
    @NonNull
    public static ValidationResult validateExportConfiguration(@NonNull String format, @Nullable String destination) {
        if (StringHelper.isEmpty(format)) {
            return ValidationResult.error(ValidationError.EXPORT_UNSUPPORTED_FORMAT, "format");
        }

        if (!isValidExportFormat(format)) {
            return ValidationResult.error(ValidationError.EXPORT_UNSUPPORTED_FORMAT, format);
        }

        if (StringHelper.isEmpty(destination)) {
            return ValidationResult.error(ValidationError.EXPORT_INVALID_DESTINATION, "destination");
        }

        return ValidationResult.success("Export configuration is valid");
    }

    /**
     * Validate import file
     */
    @NonNull
    public static ValidationResult validateImportFile(@Nullable java.io.File file) {
        if (file == null) {
            return ValidationResult.error(ValidationError.IMPORT_INVALID_FILE, "file_null");
        }

        if (!file.exists()) {
            return ValidationResult.error(ValidationError.IMPORT_INVALID_FILE, "file_not_exists");
        }

        if (!file.canRead()) {
            return ValidationResult.error(ValidationError.IMPORT_INVALID_FILE, "file_not_readable");
        }

        if (file.length() == 0) {
            return ValidationResult.error(ValidationError.IMPORT_INVALID_FILE, "file_empty");
        }

        // Check file size (max 100MB)
        long maxSize = 100 * 1024 * 1024;
        if (file.length() > maxSize) {
            return ValidationResult.error(ValidationError.GENERIC_FILE_TOO_LARGE,
                    file.length(), maxSize);
        }

        return ValidationResult.success("Import file is valid");
    }

    /**
     * Validate backup configuration
     */
    @NonNull
    public static ValidationResult validateBackupConfiguration(@NonNull String location, int retentionDays) {
        if (StringHelper.isEmpty(location)) {
            return ValidationResult.error(ValidationError.EXPORT_INVALID_DESTINATION, "backup_location");
        }

        if (!isValidRetentionDays(String.valueOf(retentionDays))) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_RETENTION_DAYS, retentionDays);
        }

        // Check if location is writable
        java.io.File backupDir = new java.io.File(location);
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return ValidationResult.error(ValidationError.SYSTEM_FILE_SYSTEM_ERROR,
                        "Cannot create backup directory: " + location);
            }
        }

        if (!backupDir.canWrite()) {
            return ValidationResult.error(ValidationError.SYSTEM_FILE_SYSTEM_ERROR,
                    "Cannot write to backup directory: " + location);
        }

        return ValidationResult.success("Backup configuration is valid");
    }

    /**
     * Validate team contact information
     */
    @NonNull
    public static ValidationResult validateTeamContact(@Nullable String name, @Nullable String phone, @Nullable String email) {
        if (!isValidContactName(name)) {
            return ValidationResult.error(ValidationError.GENERIC_REQUIRED_FIELD_MISSING, "contact_name");
        }

        // Phone and email are optional, but if provided must be valid
        if (StringHelper.isNotEmpty(phone) && !isValidPhoneNumber(phone)) {
            return ValidationResult.error(ValidationError.GENERIC_INVALID_PHONE, "phone");
        }

        if (StringHelper.isNotEmpty(email) && !isValidEmail(email)) {
            return ValidationResult.error(ValidationError.GENERIC_INVALID_EMAIL, "email");
        }

        return ValidationResult.success("Contact information is valid");
    }

    // ============================================
    // SECURITY VALIDATION
    // ============================================

    /**
     * Validate password strength (for future security features)
     */
    @NonNull
    public static ValidationResult validatePasswordStrength(@Nullable String password) {
        if (StringHelper.isEmpty(password)) {
            return ValidationResult.error(ValidationError.GENERIC_REQUIRED_FIELD_MISSING, "password");
        }

        String trimmed = password.trim();

        if (trimmed.length() < 8) {
            return ValidationResult.error(ValidationError.GENERIC_STRING_TOO_SHORT, "password", 8);
        }

        if (trimmed.length() > 128) {
            return ValidationResult.error(ValidationError.GENERIC_STRING_TOO_LONG, "password", 128);
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
            return ValidationResult.error(ValidationError.BUSINESS_RULE_VIOLATION,
                    "Password must contain at least 3 character types");
        }

        return ValidationResult.success("Password strength is adequate");
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

    // ============================================
    // BULK VALIDATION METHODS
    // ============================================

    /**
     * Validate multiple fields and return all errors
     */
    @NonNull
    public static MultiValidationResult validateMultiple(@NonNull ValidationCheck... checks) {
        MultiValidationResult result = new MultiValidationResult();

        for (ValidationCheck check : checks) {
            ValidationResult fieldResult = check.validate();
            result.addResult(fieldResult);
        }

        return result;
    }

    // ============================================
    // VALIDATION CHECK FACTORY METHODS
    // ============================================

    /**
     * Create validation check for required string field
     */
    @NonNull
    public static ValidationCheck requiredString(@NonNull String fieldName, @Nullable String value) {
        return ValidationCheck.requiredString(fieldName, value);
    }

    /**
     * Create validation check for string length
     */
    @NonNull
    public static ValidationCheck stringLength(@NonNull String fieldName, @Nullable String value, int minLength, int maxLength) {
        return ValidationCheck.stringLength(fieldName, value, minLength, maxLength);
    }

    /**
     * Create validation check for numeric range
     */
    @NonNull
    public static ValidationCheck numericRange(@NonNull String fieldName, @Nullable String value, int min, int max) {
        return ValidationCheck.numericRange(fieldName, value, min, max);
    }

    /**
     * Create validation check for email format
     */
    @NonNull
    public static ValidationCheck emailFormat(@NonNull String fieldName, @Nullable String email) {
        return ValidationCheck.emailFormat(fieldName, email);
    }

    /**
     * Create validation check for phone format
     */
    @NonNull
    public static ValidationCheck phoneFormat(@NonNull String fieldName, @Nullable String phone) {
        return ValidationCheck.phoneFormat(fieldName, phone);
    }

    /**
     * Create validation check for time format
     */
    @NonNull
    public static ValidationCheck timeFormat(@NonNull String fieldName, @Nullable String timeString) {
        return ValidationCheck.timeFormat(fieldName, timeString);
    }

    // ============================================
    // SETTINGS VALIDATION WITH ERROR CODES
    // ============================================

    /**
     * Validate theme setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateThemeSetting(@Nullable String theme) {
        if (!isValidTheme(theme)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_THEME, theme);
        }
        return ValidationResult.success("Valid theme setting");
    }

    /**
     * Validate week start setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateWeekStartSetting(@Nullable String weekStart) {
        if (!isValidWeekStartDay(weekStart)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_WEEK_START, weekStart);
        }
        return ValidationResult.success("Valid week start setting");
    }

    /**
     * Validate reminder time setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateReminderTimeSetting(@Nullable String reminderTime) {
        if (!isValidReminderTime(reminderTime)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_REMINDER_TIME, reminderTime);
        }
        return ValidationResult.success("Valid reminder time setting");
    }

    /**
     * Validate retention days setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateRetentionDaysSetting(@Nullable String retentionDays) {
        if (!isValidRetentionDays(retentionDays)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_RETENTION_DAYS, retentionDays);
        }
        return ValidationResult.success("Valid retention days setting");
    }

    /**
     * Validate backup frequency setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateBackupFrequencySetting(@Nullable String frequency) {
        if (!isValidBackupFrequency(frequency)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_BACKUP_FREQUENCY, frequency);
        }
        return ValidationResult.success("Valid backup frequency setting");
    }

    /**
     * Validate notification priority setting with proper error codes
     */
    @NonNull
    public static ValidationResult validateNotificationPrioritySetting(@Nullable String priority) {
        if (!isValidNotificationPriority(priority)) {
            return ValidationResult.error(ValidationError.SETTINGS_INVALID_NOTIFICATION_PRIORITY, priority);
        }
        return ValidationResult.success("Valid notification priority setting");
    }

    // ============================================
    // COMPREHENSIVE VALIDATION METHODS
    // ============================================

    /**
     * Validate complete user profile information
     */
    @NonNull
    public static MultiValidationResult validateUserProfile(
            @Nullable String name,
            @Nullable String email,
            @Nullable String phone
    ) {
        return validateMultiple(
                requiredString("name", name),
                stringLength("name", name, 2, 50),
                emailFormat("email", email),
                phoneFormat("phone", phone)
        );
    }

    /**
     * Validate complete settings configuration
     */
    @NonNull
    public static MultiValidationResult validateSettingsConfiguration(
            @Nullable String theme,
            @Nullable String weekStart,
            @Nullable String reminderTime,
            @Nullable String retentionDays,
            @Nullable String backupFrequency,
            @Nullable String notificationPriority
    ) {
        MultiValidationResult result = new MultiValidationResult();

        result.addResult(validateThemeSetting(theme));
        result.addResult(validateWeekStartSetting(weekStart));
        result.addResult(validateReminderTimeSetting(reminderTime));
        result.addResult(validateRetentionDaysSetting(retentionDays));
        result.addResult(validateBackupFrequencySetting(backupFrequency));
        result.addResult(validateNotificationPrioritySetting(notificationPriority));

        return result;
    }

    // ============================================
    // JSON VALIDATION
    // ============================================

    /**
     * Validate JSON structure with error codes
     */
    @NonNull
    public static ValidationResult validateJsonStructure(@Nullable String json, @NonNull String[] requiredFields) {
        if (StringHelper.isEmpty(json)) {
            return ValidationResult.error(ValidationError.GENERIC_REQUIRED_FIELD_MISSING, "json");
        }

        if (!JsonHelper.isValidJson(json)) {
            return ValidationResult.error(ValidationError.GENERIC_INVALID_JSON, "Invalid JSON format");
        }

        try {
            JsonHelper.ValidationResult structureResult = JsonHelper.validateJsonStructure(json, requiredFields);
            if (!structureResult.isValid()) {
                return ValidationResult.error(ValidationError.IMPORT_INVALID_DATA_STRUCTURE,
                        structureResult.getMessage());
            }

            return ValidationResult.success("JSON structure is valid");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.GENERIC_INVALID_JSON, e.getMessage());
        }
    }

    /**
     * Validate backup JSON structure
     */
    @NonNull
    public static ValidationResult validateBackupJson(@Nullable String json) {
        String[] requiredFields = {"metadata", "shiftTypes", "shiftPatterns", "userAssignments"};
        return validateJsonStructure(json, requiredFields);
    }

    // ============================================
    // SYSTEM INTEGRATION VALIDATION
    // ============================================

    /**
     * Validate system component integrations
     */
    @NonNull
    public static MultiValidationResult validateSystemIntegration() {
        MultiValidationResult result = new MultiValidationResult();

        result.addResult(validateDateTimeHelperIntegration());
        result.addResult(validateJsonHelperIntegration());
        result.addResult(validateStringHelperIntegration());

        return result;
    }

    /**
     * Validate DateTimeHelper integration
     */
    @NonNull
    private static ValidationResult validateDateTimeHelperIntegration() {
        try {
            // Test basic parsing
            LocalTime testTime = DateTimeHelper.parseTime("12:30");
            String formatted = DateTimeHelper.formatTime(testTime);

            if (!"12:30".equals(formatted)) {
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Time formatting failed");
            }

            // Test duration calculation
            int duration = DateTimeHelper.calculateShiftDurationMinutes(
                    LocalTime.of(8, 0), LocalTime.of(16, 0));

            if (duration != 480) { // 8 hours = 480 minutes
                return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR,
                        "Duration calculation failed");
            }

            return ValidationResult.success("DateTimeHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_DATETIME_HELPER_ERROR, e.getMessage());
        }
    }

    /**
     * Validate JsonHelper integration
     */
    @NonNull
    private static ValidationResult validateJsonHelperIntegration() {
        try {
            // Test basic JSON operations
            String testJson = "{\"test\": \"value\", \"number\": 42}";

            if (!JsonHelper.isValidJson(testJson)) {
                return ValidationResult.error(ValidationError.SYSTEM_JSON_HELPER_ERROR,
                        "JSON validation failed");
            }

            java.util.Map<String, Object> map = JsonHelper.fromJsonToMap(testJson);
            if (!"value".equals(map.get("test"))) {
                return ValidationResult.error(ValidationError.SYSTEM_JSON_HELPER_ERROR,
                        "JSON parsing failed");
            }

            return ValidationResult.success("JsonHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_JSON_HELPER_ERROR, e.getMessage());
        }
    }

    /**
     * Validate StringHelper integration
     */
    @NonNull
    private static ValidationResult validateStringHelperIntegration() {
        try {
            // Test basic string operations
            if (!StringHelper.isEmpty(null) || !StringHelper.isEmpty("")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "isEmpty validation failed");
            }

            if (StringHelper.isNotEmpty(null) || StringHelper.isNotEmpty("")) {
                return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR,
                        "isNotEmpty validation failed");
            }

            return ValidationResult.success("StringHelper integration OK");

        } catch (Exception e) {
            return ValidationResult.error(ValidationError.SYSTEM_STRING_HELPER_ERROR, e.getMessage());
        }
    }

    // ============================================
    // UTILITY AND CONVENIENCE METHODS
    // ============================================

    /**
     * Quick validation for basic requirements
     */
    @NonNull
    public static ValidationResult quickValidate(@NonNull String fieldName, @Nullable String value, boolean required) {
        if (required && StringHelper.isEmpty(value)) {
            return ValidationResult.requiredField(fieldName);
        }

        return ValidationResult.success("Quick validation passed");
    }

    /**
     * Validate field with custom condition
     */
    @NonNull
    public static ValidationResult validateCondition(@NonNull String fieldName, boolean condition, @NonNull ValidationError errorCode) {
        if (!condition) {
            return ValidationResult.error(errorCode, fieldName);
        }

        return ValidationResult.success("Condition validation passed");
    }

    /**
     * Create a reusable validation check for common patterns
     */
    @NonNull
    public static ValidationCheck createCommonValidation(@NonNull String fieldName, @Nullable String value, boolean required, int minLength, int maxLength) {
        ValidationCheck baseCheck = required ?
                requiredString(fieldName, value) :
                ValidationCheck.alwaysValid();

        return baseCheck.and(stringLength(fieldName, value, minLength, maxLength));
    }

    // ============================================
    // BACKWARD COMPATIBILITY METHODS
    // ============================================

    /**
     * Legacy method - returns simple boolean for backward compatibility
     * @deprecated Use validatePatternName with ValidationResult instead
     */
    @Deprecated
    public static boolean isValidPatternNameLegacy(@Nullable String name) {
        return isValidPatternName(name);
    }

    /**
     * Legacy method - returns simple boolean for backward compatibility
     * @deprecated Use validateTeamContact with ValidationResult instead
     */
    @Deprecated
    public static boolean isValidContactNameLegacy(@Nullable String name) {
        return isValidContactName(name);
    }

    // ============================================
    // CONSTANTS FOR EXTERNAL USE
    // ============================================

    /** Minimum pattern name length */
    public static final int MIN_PATTERN_NAME_LENGTH = 2;

    /** Maximum pattern name length */
    public static final int MAX_PATTERN_NAME_LENGTH = 50;

    /** Minimum contact name length */
    public static final int MIN_CONTACT_NAME_LENGTH = 1;

    /** Maximum contact name length */
    public static final int MAX_CONTACT_NAME_LENGTH = 100;

    /** Maximum file name length */
    public static final int MAX_FILE_NAME_LENGTH = 255;

    /** Maximum backup retention days */
    public static final int MAX_RETENTION_DAYS = 3650;

    /** Maximum reminder time in minutes (24 hours) */
    public static final int MAX_REMINDER_TIME_MINUTES = 1440;
}