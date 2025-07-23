package net.calvuz.qdue.smartshifts.utils.validation;

/**
 * Enumeration of validation error codes for internationalization support.
 * <p>
 * This enum provides error codes that can be resolved to localized messages
 * in the UI layer, while keeping internal validation logic language-independent.
 * <p>
 * Naming convention: CATEGORY_SPECIFIC_ERROR
 * Categories: GENERIC, SHIFT, TEAM, PATTERN, TIME, etc.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public enum ValidationError {

    // ============================================
    // GENERIC VALIDATION ERRORS
    // ============================================

    /** Required field is missing or empty */
    GENERIC_REQUIRED_FIELD_MISSING,

    /** Invalid format (generic) */
    GENERIC_INVALID_FORMAT,

    /** Value is out of acceptable range */
    GENERIC_OUT_OF_RANGE,

    /** String length exceeds maximum allowed */
    GENERIC_STRING_TOO_LONG,

    /** String length below minimum required */
    GENERIC_STRING_TOO_SHORT,

    /** File not found or inaccessible */
    GENERIC_FILE_NOT_FOUND,

    /** File size exceeds limit */
    GENERIC_FILE_TOO_LARGE,

    /** Invalid email format */
    GENERIC_INVALID_EMAIL,

    /** Invalid phone number format */
    GENERIC_INVALID_PHONE,

    /** Invalid URL format */
    GENERIC_INVALID_URL,

    /** JSON parsing or validation error */
    GENERIC_INVALID_JSON,

    // ============================================
    // TIME AND DATE VALIDATION ERRORS
    // ============================================

    /** Invalid time format (should be HH:mm) */
    TIME_INVALID_FORMAT,

    /** Invalid date format (should be yyyy-MM-dd) */
    TIME_INVALID_DATE_FORMAT,

    /** Start time equals end time */
    TIME_RANGE_IDENTICAL,

    /** Time range is invalid */
    TIME_RANGE_INVALID,

    /** Duration is too short */
    TIME_DURATION_TOO_SHORT,

    /** Duration is too long */
    TIME_DURATION_TOO_LONG,

    /** Time is outside working hours */
    TIME_OUTSIDE_WORKING_HOURS,

    // ============================================
    // SHIFT VALIDATION ERRORS
    // ============================================

    /** Shift name is invalid */
    SHIFT_INVALID_NAME,

    /** Shift duration is below minimum (1 hour) */
    SHIFT_DURATION_TOO_SHORT,

    /** Shift duration exceeds maximum (12 hours) */
    SHIFT_DURATION_TOO_LONG,

    /** Shift description is too long */
    SHIFT_DESCRIPTION_TOO_LONG,

    /** Shift color format is invalid */
    SHIFT_INVALID_COLOR,

    /** Two or more shifts have overlapping time ranges */
    SHIFT_TIME_OVERLAP,

    /** Maximum number of shift types exceeded (4) */
    SHIFT_TOO_MANY_TYPES,

    /** Shift type not found in configuration */
    SHIFT_TYPE_NOT_FOUND,

    /** Night shift validation failed */
    SHIFT_INVALID_NIGHT_SHIFT,

    /** Shift configuration is inconsistent */
    SHIFT_CONFIGURATION_INVALID,

    // ============================================
    // PATTERN VALIDATION ERRORS
    // ============================================

    /** Pattern has no days defined */
    PATTERN_EMPTY,

    /** Pattern cycle length is invalid */
    PATTERN_INVALID_CYCLE_LENGTH,

    /** Pattern has gaps in 24-hour coverage */
    PATTERN_COVERAGE_GAP,

    /** Pattern has no rest days */
    PATTERN_NO_REST_DAYS,

    /** Too many consecutive working days */
    PATTERN_EXCESSIVE_CONSECUTIVE_WORK,

    /** Work-to-rest ratio is too high */
    PATTERN_HIGH_WORK_RATIO,

    /** Average working hours per day too high */
    PATTERN_EXCESSIVE_DAILY_HOURS,

    /** Pattern is unsuitable for continuous operation */
    PATTERN_NOT_CONTINUOUS_SUITABLE,

    /** Pattern efficiency score is too low */
    PATTERN_LOW_EFFICIENCY,

    // ============================================
    // TEAM VALIDATION ERRORS
    // ============================================

    /** Team count is below minimum */
    TEAM_COUNT_TOO_LOW,

    /** Team count exceeds maximum */
    TEAM_COUNT_TOO_HIGH,

    /** Team count inappropriate for pattern */
    TEAM_COUNT_INAPPROPRIATE,

    /** Team offset is out of valid range */
    TEAM_INVALID_OFFSET,

    /** Duplicate team offsets detected */
    TEAM_DUPLICATE_OFFSET,

    /** No team offsets defined */
    TEAM_NO_OFFSETS_DEFINED,

    /** Team workload is unbalanced */
    TEAM_WORKLOAD_UNBALANCED,

    /** Team workload is excessive */
    TEAM_WORKLOAD_EXCESSIVE,

    // ============================================
    // FATIGUE AND SAFETY ERRORS
    // ============================================

    /** High percentage of night shifts detected */
    FATIGUE_HIGH_NIGHT_SHIFT_RATIO,

    /** High percentage of long shifts detected */
    FATIGUE_HIGH_LONG_SHIFT_RATIO,

    /** Too many early or late shifts */
    FATIGUE_EXCESSIVE_DISRUPTIVE_SHIFTS,

    /** Insufficient rest period between shifts */
    FATIGUE_INSUFFICIENT_REST_PERIOD,

    /** Overall fatigue risk is high */
    FATIGUE_HIGH_RISK,

    // ============================================
    // EXPORT/IMPORT VALIDATION ERRORS
    // ============================================

    /** Export format not supported */
    EXPORT_UNSUPPORTED_FORMAT,

    /** Export destination invalid */
    EXPORT_INVALID_DESTINATION,

    /** Import file is corrupted or invalid */
    IMPORT_INVALID_FILE,

    /** Import file format not recognized */
    IMPORT_UNSUPPORTED_FORMAT,

    /** Import data structure validation failed */
    IMPORT_INVALID_DATA_STRUCTURE,

    // ============================================
    // SYSTEM INTEGRATION ERRORS
    // ============================================

    /** DateTimeHelper integration test failed */
    SYSTEM_DATETIME_HELPER_ERROR,

    /** JsonHelper integration test failed */
    SYSTEM_JSON_HELPER_ERROR,

    /** StringHelper integration test failed */
    SYSTEM_STRING_HELPER_ERROR,

    /** Database connection or operation failed */
    SYSTEM_DATABASE_ERROR,

    /** File system operation failed */
    SYSTEM_FILE_SYSTEM_ERROR,

    // ============================================
    // SETTINGS VALIDATION ERRORS
    // ============================================

    /** Invalid theme setting */
    SETTINGS_INVALID_THEME,

    /** Invalid week start day setting */
    SETTINGS_INVALID_WEEK_START,

    /** Invalid reminder time setting */
    SETTINGS_INVALID_REMINDER_TIME,

    /** Invalid data retention period */
    SETTINGS_INVALID_RETENTION_DAYS,

    /** Invalid backup frequency setting */
    SETTINGS_INVALID_BACKUP_FREQUENCY,

    /** Invalid notification priority */
    SETTINGS_INVALID_NOTIFICATION_PRIORITY,

    // ============================================
    // BUSINESS LOGIC ERRORS
    // ============================================

    /** Business rule violation */
    BUSINESS_RULE_VIOLATION,

    /** Concurrent modification detected */
    BUSINESS_CONCURRENT_MODIFICATION,

    /** Resource already exists */
    BUSINESS_RESOURCE_EXISTS,

    /** Resource not found */
    BUSINESS_RESOURCE_NOT_FOUND,

    /** Operation not permitted */
    BUSINESS_OPERATION_NOT_PERMITTED,

    /** Insufficient permissions */
    BUSINESS_INSUFFICIENT_PERMISSIONS;

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get category prefix for this error code
     */
    public String getCategory() {
        String name = this.name();
        int firstUnderscore = name.indexOf('_');
        return firstUnderscore > 0 ? name.substring(0, firstUnderscore) : "UNKNOWN";
    }

    /**
     * Get specific error name without category prefix
     */
    public String getSpecificError() {
        String name = this.name();
        int firstUnderscore = name.indexOf('_');
        return firstUnderscore > 0 ? name.substring(firstUnderscore + 1) : name;
    }

    /**
     * Check if this is a critical error that should block operations
     */
    public boolean isCritical() {
        return switch (this) {
            case SHIFT_TIME_OVERLAP, PATTERN_COVERAGE_GAP, FATIGUE_HIGH_RISK,
                 TEAM_WORKLOAD_EXCESSIVE, PATTERN_EXCESSIVE_CONSECUTIVE_WORK, SYSTEM_DATABASE_ERROR,
                 BUSINESS_OPERATION_NOT_PERMITTED -> true;
            default -> false;
        };
    }

    /**
     * Check if this is a warning that allows operation to continue
     */
    public boolean isWarning() {
        return switch (this) {
            case TEAM_COUNT_INAPPROPRIATE, PATTERN_LOW_EFFICIENCY,
                 FATIGUE_EXCESSIVE_DISRUPTIVE_SHIFTS, TEAM_WORKLOAD_UNBALANCED -> true;
            default -> false;
        };
    }

    /**
     * Get default English message for internal use and logging
     * This should NOT be shown to users - use proper i18n resolution instead
     */
    public String getInternalMessage() {
        return switch (this) {
            // Generic errors
            case GENERIC_REQUIRED_FIELD_MISSING -> "Required field is missing";
            case GENERIC_INVALID_FORMAT -> "Invalid format";
            case GENERIC_OUT_OF_RANGE -> "Value is out of range";
            case GENERIC_STRING_TOO_LONG -> "String exceeds maximum length";
            case GENERIC_STRING_TOO_SHORT -> "String is below minimum length";
            case GENERIC_FILE_NOT_FOUND -> "File not found";
            case GENERIC_FILE_TOO_LARGE -> "File size exceeds limit";
            case GENERIC_INVALID_EMAIL -> "Invalid email format";
            case GENERIC_INVALID_PHONE -> "Invalid phone number format";
            case GENERIC_INVALID_URL -> "Invalid URL format";
            case GENERIC_INVALID_JSON -> "Invalid JSON format";

            // Time errors
            case TIME_INVALID_FORMAT -> "Invalid time format (expected HH:mm)";
            case TIME_INVALID_DATE_FORMAT -> "Invalid date format (expected yyyy-MM-dd)";
            case TIME_RANGE_IDENTICAL -> "Start time equals end time";
            case TIME_RANGE_INVALID -> "Invalid time range";
            case TIME_DURATION_TOO_SHORT -> "Duration is too short";
            case TIME_DURATION_TOO_LONG -> "Duration is too long";
            case TIME_OUTSIDE_WORKING_HOURS -> "Time is outside working hours";

            // Shift errors
            case SHIFT_INVALID_NAME -> "Invalid shift name";
            case SHIFT_DURATION_TOO_SHORT -> "Shift duration too short (minimum 1 hour)";
            case SHIFT_DURATION_TOO_LONG -> "Shift duration too long (maximum 12 hours)";
            case SHIFT_DESCRIPTION_TOO_LONG ->
                    "Shift description too long (maximum 200 characters)";
            case SHIFT_INVALID_COLOR -> "Invalid color format (use hex #RRGGBB)";
            case SHIFT_TIME_OVERLAP -> "Shift time ranges overlap";
            case SHIFT_TOO_MANY_TYPES -> "Too many shift types (maximum 4 allowed)";
            case SHIFT_TYPE_NOT_FOUND -> "Shift type not found";
            case SHIFT_INVALID_NIGHT_SHIFT -> "Invalid night shift configuration";
            case SHIFT_CONFIGURATION_INVALID -> "Shift configuration is invalid";

            // Pattern errors
            case PATTERN_EMPTY -> "Pattern has no days defined";
            case PATTERN_INVALID_CYCLE_LENGTH -> "Invalid pattern cycle length";
            case PATTERN_COVERAGE_GAP -> "Pattern has gaps in 24-hour coverage";
            case PATTERN_NO_REST_DAYS -> "Pattern has no rest days";
            case PATTERN_EXCESSIVE_CONSECUTIVE_WORK -> "Too many consecutive working days";
            case PATTERN_HIGH_WORK_RATIO -> "Work-to-rest ratio is too high";
            case PATTERN_EXCESSIVE_DAILY_HOURS -> "Average daily working hours too high";
            case PATTERN_NOT_CONTINUOUS_SUITABLE -> "Pattern unsuitable for continuous operation";
            case PATTERN_LOW_EFFICIENCY -> "Pattern efficiency is low";

            // Team errors
            case TEAM_COUNT_TOO_LOW -> "Team count is too low";
            case TEAM_COUNT_TOO_HIGH -> "Team count is too high";
            case TEAM_COUNT_INAPPROPRIATE -> "Team count inappropriate for pattern";
            case TEAM_INVALID_OFFSET -> "Team offset is invalid";
            case TEAM_DUPLICATE_OFFSET -> "Duplicate team offsets detected";
            case TEAM_NO_OFFSETS_DEFINED -> "No team offsets defined";
            case TEAM_WORKLOAD_UNBALANCED -> "Team workload is unbalanced";
            case TEAM_WORKLOAD_EXCESSIVE -> "Team workload is excessive";

            // Fatigue errors
            case FATIGUE_HIGH_NIGHT_SHIFT_RATIO -> "High percentage of night shifts";
            case FATIGUE_HIGH_LONG_SHIFT_RATIO -> "High percentage of long shifts";
            case FATIGUE_EXCESSIVE_DISRUPTIVE_SHIFTS -> "Too many early or late shifts";
            case FATIGUE_INSUFFICIENT_REST_PERIOD -> "Insufficient rest period between shifts";
            case FATIGUE_HIGH_RISK -> "High fatigue risk detected";

            // Export/Import errors
            case EXPORT_UNSUPPORTED_FORMAT -> "Export format not supported";
            case EXPORT_INVALID_DESTINATION -> "Export destination is invalid";
            case IMPORT_INVALID_FILE -> "Import file is corrupted or invalid";
            case IMPORT_UNSUPPORTED_FORMAT -> "Import file format not recognized";
            case IMPORT_INVALID_DATA_STRUCTURE -> "Import data structure validation failed";

            // System errors
            case SYSTEM_DATETIME_HELPER_ERROR -> "DateTimeHelper integration failed";
            case SYSTEM_JSON_HELPER_ERROR -> "JsonHelper integration failed";
            case SYSTEM_STRING_HELPER_ERROR -> "StringHelper integration failed";
            case SYSTEM_DATABASE_ERROR -> "Database operation failed";
            case SYSTEM_FILE_SYSTEM_ERROR -> "File system operation failed";

            // Settings errors
            case SETTINGS_INVALID_THEME -> "Invalid theme setting";
            case SETTINGS_INVALID_WEEK_START -> "Invalid week start day setting";
            case SETTINGS_INVALID_REMINDER_TIME -> "Invalid reminder time setting";
            case SETTINGS_INVALID_RETENTION_DAYS -> "Invalid data retention period";
            case SETTINGS_INVALID_BACKUP_FREQUENCY -> "Invalid backup frequency setting";
            case SETTINGS_INVALID_NOTIFICATION_PRIORITY -> "Invalid notification priority";

            // Business errors
            case BUSINESS_RULE_VIOLATION -> "Business rule violation";
            case BUSINESS_CONCURRENT_MODIFICATION -> "Concurrent modification detected";
            case BUSINESS_RESOURCE_EXISTS -> "Resource already exists";
            case BUSINESS_RESOURCE_NOT_FOUND -> "Resource not found";
            case BUSINESS_OPERATION_NOT_PERMITTED -> "Operation not permitted";
            case BUSINESS_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions";
        };
    }

    /**
     * Create a string resource key for Android i18n
     * Format: "validation_error_{category}_{specific_error}"
     */
    public String getResourceKey() {
        return "validation_error_" + this.name().toLowerCase();
    }

    /**
     * Create a string resource key with prefix for Android i18n
     * Format: "{prefix}_{category}_{specific_error}"
     */
    public String getResourceKey(String prefix) {
        return prefix + "_" + this.name().toLowerCase();
    }
}