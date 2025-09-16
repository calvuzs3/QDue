package net.calvuz.qdue.events.validation;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enhanced JSON Schema Validator for Q-Due Events
 *
 * <p>Advanced validation for Q-Due event packages with comprehensive error reporting,
 * flexible date/time parsing, and support for both object and string input validation.
 * Designed for use with FileImportDialogFragment and other validation scenarios.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>String and Object Input</strong>: Validates both JSON strings and parsed objects</li>
 *   <li><strong>Comprehensive Reporting</strong>: Detailed errors, warnings, and event counts</li>
 *   <li><strong>Flexible Date Parsing</strong>: Multiple ISO and custom date formats</li>
 *   <li><strong>Q-Due Compliance</strong>: Validates against Q-Due event schema</li>
 *   <li><strong>Performance Optimized</strong>: Efficient validation for large event sets</li>
 * </ul>
 *
 * <h3>Supported Date Formats:</h3>
 * <ul>
 *   <li>ISO 8601 dates: "2025-01-15"</li>
 *   <li>ISO 8601 datetime: "2025-01-15T10:30:00Z"</li>
 *   <li>ISO 8601 with timezone: "2025-01-15T10:30:00+01:00"</li>
 *   <li>Custom formats: "2025-01-15 10:30:00"</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Enhanced for FileImport Integration
 * @since LocalEvents MVVM Implementation
 */
public class JsonSchemaValidator {

    private static final String TAG = "JsonSchemaValidator";

    // Validation constants
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_LOCATION_LENGTH = 200;
    private static final int MAX_EVENTS_PER_PACKAGE = 1000;
    private static final int MAX_TAGS_PER_EVENT = 10;
    private static final int MAX_CUSTOM_PROPERTIES = 20;

    // Regular expressions for validation
    private static final Pattern PACKAGE_ID_PATTERN = Pattern.compile("^[a-z0-9_]{3,50}$");
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    // Multiple date/time formatters for flexible parsing
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),                    // 2025-01-15
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),     // 2025-01-15T10:30:00Z
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),     // 2025-01-15T10:30:00+01:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),        // 2025-01-15T10:30:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),          // 2025-01-15 10:30:00
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,                       // Built-in ISO format
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,                      // Built-in ISO with offset
            DateTimeFormatter.ISO_INSTANT                                // Built-in instant format
    );

    private static final List<DateTimeFormatter> TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("HH:mm"),                        // 14:30
            DateTimeFormatter.ofPattern("HH:mm:ss"),                     // 14:30:00
            DateTimeFormatter.ofPattern("H:mm"),                         // 8:30
            DateTimeFormatter.ISO_LOCAL_TIME                             // Built-in time format
    );

    // Valid Q-Due event types and priorities
    private static final Set<String> VALID_EVENT_TYPES = new HashSet<>(Arrays.asList(
            "GENERAL", "STOP_PLANNED", "STOP_UNPLANNED", "STOP_SHORTAGE", "STOP_ORDERS", "STOP_CASSA",
            "MAINTENANCE", "MEETING", "TRAINING", "HOLIDAY", "EMERGENCY", "SHIFT_CHANGE",
            "OVERTIME", "SAFETY_DRILL", "AUDIT", "IMPORTED"
    ));

    private static final Set<String> VALID_PRIORITIES = new HashSet<>(Arrays.asList(
            "LOW", "NORMAL", "HIGH", "URGENT"
    ));

    // Gson instance for JSON parsing
    private static final Gson gson = new Gson();

    // ==================== VALIDATION RESULT CLASS ====================

    /**
     * Enhanced validation result with comprehensive error reporting and event counting.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        private final List<String> warnings;
        private final List<String> errors;
        private final int eventCount;
        private final String packageName;

        public ValidationResult(boolean isValid, String errorMessage, List<String> errors,
                                List<String> warnings, int eventCount, String packageName) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
            this.eventCount = eventCount;
            this.packageName = packageName;
        }

        // ==================== COMPATIBILITY METHODS ====================

        /**
         * Check if validation passed.
         */
        public boolean isValid() {
            return isValid;
        }

        /**
         * Get main error message.
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * Get list of all errors.
         */
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        /**
         * Get list of all warnings.
         */
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        /**
         * Get total count of events in the package.
         */
        public int getEventCount() {
            return eventCount;
        }

        /**
         * Get package name if available.
         */
        public String getPackageName() {
            return packageName;
        }

        // ==================== UTILITY METHODS ====================

        /**
         * Check if there are any warnings.
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * Check if there are any errors.
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Get full error message with details.
         */
        public String getFullErrorMessage() {
            if (isValid) return null;

            StringBuilder sb = new StringBuilder();
            if (errorMessage != null) {
                sb.append(errorMessage);
            }

            if (!errors.isEmpty()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("Detailed errors:");
                for (String error : errors) {
                    sb.append("\n• ").append(error);
                }
            }
            return sb.toString();
        }

        /**
         * Get summary string for UI display.
         */
        public String getSummary() {
            if (isValid) {
                String summary = "Validation successful (" + eventCount + " events)";
                if (hasWarnings()) {
                    summary += " - " + warnings.size() + " warnings";
                }
                return summary;
            } else {
                return "Validation failed (" + errors.size() + " errors)";
            }
        }

        // ==================== FACTORY METHODS ====================

        public static ValidationResult valid(List<String> warnings, int eventCount, String packageName) {
            return new ValidationResult(true, null, null, warnings, eventCount, packageName);
        }

        public static ValidationResult invalid(String error, List<String> detailedErrors, int eventCount) {
            return new ValidationResult(false, error, detailedErrors, null, eventCount, null);
        }

        public static ValidationResult parseError(String error) {
            return new ValidationResult(false, error, List.of(error), null, 0, null);
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, eventCount=%d, errors=%d, warnings=%d}",
                                 isValid, eventCount, errors.size(), warnings.size());
        }
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Validate event package from JSON string.
     * This is the main entry point for FileImportDialogFragment.
     *
     * @param jsonContent JSON string content to validate
     * @return ValidationResult with comprehensive validation information
     */
    public static ValidationResult validateEventPackage(String jsonContent) {
        Log.d(TAG, "Starting event package validation from JSON string");

        if (isEmpty(jsonContent)) {
            return ValidationResult.parseError("JSON content is empty");
        }

        try {
            // Parse JSON string to EventPackageJson object
            EventPackageJson packageJson = gson.fromJson(jsonContent, EventPackageJson.class);

            if (packageJson == null) {
                return ValidationResult.parseError("Failed to parse JSON content");
            }

            // Validate the parsed object
            return validatePackage(packageJson);

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
            return ValidationResult.parseError("Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected validation error: " + e.getMessage(), e);
            return ValidationResult.parseError("Validation error: " + e.getMessage());
        }
    }

    /**
     * Validate event package from parsed object.
     *
     * @param packageJson Parsed EventPackageJson object to validate
     * @return ValidationResult with comprehensive validation information
     */
    public static ValidationResult validatePackage(EventPackageJson packageJson) {
        Log.d(TAG, "Starting comprehensive package validation");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int eventCount = 0;
        String packageName = null;

        try {
            // Basic structure validation
            if (packageJson == null) {
                return ValidationResult.invalid("Package is null", List.of("Package is null"), 0);
            }

            // Get package info for result
            if (packageJson.package_info != null) {
                packageName = packageJson.package_info.name;
            }

            // Get event count
            if (packageJson.events != null) {
                eventCount = packageJson.events.size();
            }

            // Validate package info
            validatePackageInfo(packageJson.package_info, errors, warnings);

            // Validate events array
            validateEventsArray(packageJson.events, errors, warnings);

            // Cross-validation (relationships between fields)
            performCrossValidation(packageJson, errors, warnings);

            // Return result
            if (!errors.isEmpty()) {
                String mainError = errors.get(0);
                return ValidationResult.invalid(mainError, errors, eventCount);
            }

            Log.d(TAG, "Package validation completed successfully with " + warnings.size() + " warnings");
            return ValidationResult.valid(warnings, eventCount, packageName);

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during validation", e);
            List<String> errorList = new ArrayList<>();
            errorList.add("Validation failed: " + e.getMessage());
            return ValidationResult.invalid("Validation failed: " + e.getMessage(), errorList, eventCount);
        }
    }

    /**
     * Quick validation check for JSON string.
     * Returns only basic validation status without detailed analysis.
     *
     * @param jsonContent JSON string to check
     * @return true if JSON is valid and parseable, false otherwise
     */
    public static boolean isValidJson(String jsonContent) {
        if (isEmpty(jsonContent)) {
            return false;
        }

        try {
            EventPackageJson packageJson = gson.fromJson(jsonContent, EventPackageJson.class);
            return packageJson != null &&
                    packageJson.package_info != null &&
                    packageJson.events != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get supported file format description.
     */
    public static String getSupportedFormatsDescription() {
        return "Supported format: JSON files containing Q-Due event packages with package_info and events arrays";
    }

    // ==================== PRIVATE VALIDATION METHODS ====================

    /**
     * Validate package_info section with flexible date parsing
     */
    private static void validatePackageInfo(EventPackageJson.PackageInfo packageInfo,
                                            List<String> errors, List<String> warnings) {

        if (packageInfo == null) {
            errors.add("Missing 'package_info' section");
            return;
        }

        // Required fields
        if (isEmpty(packageInfo.id)) {
            errors.add("Package ID is required");
        } else if (!PACKAGE_ID_PATTERN.matcher(packageInfo.id).matches()) {
            errors.add("Package ID must be lowercase, 3-50 chars, alphanumeric + underscore only");
        }

        if (isEmpty(packageInfo.name)) {
            errors.add("Package name is required");
        } else if (packageInfo.name.length() > 100) {
            errors.add("Package name too long (max 100 characters)");
        }

        if (isEmpty(packageInfo.version)) {
            errors.add("Package version is required");
        } else if (!VERSION_PATTERN.matcher(packageInfo.version).matches()) {
            errors.add("Version must follow semantic versioning (e.g., '1.2.3')");
        }

        // Optional fields validation
        if (!isEmpty(packageInfo.contact_email) &&
                !EMAIL_PATTERN.matcher(packageInfo.contact_email).matches()) {
            warnings.add("Contact email format appears invalid");
        }

        // Flexible date validation
        validateFlexibleDateField(packageInfo.created_date, "created_date", false, errors, warnings);
        validateFlexibleDateField(packageInfo.valid_from, "valid_from", true, errors, warnings);
        validateFlexibleDateField(packageInfo.valid_to, "valid_to", true, errors, warnings);

        // Check date logic
        if (!isEmpty(packageInfo.valid_from) && !isEmpty(packageInfo.valid_to)) {
            try {
                LocalDate fromDate = parseFlexibleDate(packageInfo.valid_from);
                LocalDate toDate = parseFlexibleDate(packageInfo.valid_to);
                if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
                    errors.add("valid_to date must be after valid_from date");
                }
            } catch (Exception e) {
                // Already handled in individual field validation
                Log.d(TAG, "Date logic validation skipped due to parse error: " + e.getMessage());
            }
        }
    }

    /**
     * Validate events array
     */
    private static void validateEventsArray(List<EventPackageJson.EventJson> events,
                                            List<String> errors, List<String> warnings) {

        if (events == null) {
            errors.add("Missing 'events' array");
            return;
        }

        if (events.isEmpty()) {
            warnings.add("Events array is empty");
            return;
        }

        if (events.size() > MAX_EVENTS_PER_PACKAGE) {
            errors.add("Too many events (" + events.size() + "), maximum is " + MAX_EVENTS_PER_PACKAGE);
        }

        // Track duplicate IDs
        Set<String> eventIds = new HashSet<>();

        // Validate each event
        for (int i = 0; i < events.size(); i++) {
            EventPackageJson.EventJson event = events.get(i);
            String eventPrefix = "Event " + (i + 1);

            validateSingleEvent(event, eventPrefix, eventIds, errors, warnings);
        }
    }

    /**
     * Validate individual event
     */
    private static void validateSingleEvent(EventPackageJson.EventJson event, String eventPrefix,
                                            Set<String> eventIds, List<String> errors, List<String> warnings) {

        if (event == null) {
            errors.add(eventPrefix + ": Event is null");
            return;
        }

        // Required fields
        if (isEmpty(event.id)) {
            errors.add(eventPrefix + ": Event ID is required");
        } else {
            if (!EVENT_ID_PATTERN.matcher(event.id).matches()) {
                errors.add(eventPrefix + ": Event ID contains invalid characters");
            }
            if (eventIds.contains(event.id)) {
                errors.add(eventPrefix + ": Duplicate event ID '" + event.id + "'");
            } else {
                eventIds.add(event.id);
            }
        }

        if (isEmpty(event.title)) {
            errors.add(eventPrefix + ": Title is required");
        } else if (event.title.length() > MAX_TITLE_LENGTH) {
            errors.add(eventPrefix + ": Title too long (max " + MAX_TITLE_LENGTH + " characters)");
        }

        if (isEmpty(event.start_date)) {
            errors.add(eventPrefix + ": start_date is required");
        } else {
            validateFlexibleDateField(event.start_date, eventPrefix + " start_date", true, errors, warnings);
        }

        // Optional field validation
        if (!isEmpty(event.description) && event.description.length() > MAX_DESCRIPTION_LENGTH) {
            warnings.add(eventPrefix + ": Description is very long (" + event.description.length() + " chars)");
        }

        if (!isEmpty(event.location) && event.location.length() > MAX_LOCATION_LENGTH) {
            warnings.add(eventPrefix + ": Location name is very long");
        }

        // Date/time logic validation
        validateEventDateTime(event, eventPrefix, errors, warnings);

        // Q-Due specific field validation
        validateQDueFields(event, eventPrefix, errors, warnings);

        // Tags validation
        validateTags(event.tags, eventPrefix, errors, warnings);

        // Custom properties validation
        validateCustomProperties(event.custom_properties, eventPrefix, errors, warnings);
    }

    /**
     * Validate event date/time logic with flexible parsing
     */
    private static void validateEventDateTime(EventPackageJson.EventJson event, String eventPrefix,
                                              List<String> errors, List<String> warnings) {

        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalTime startTime = null;
        LocalTime endTime = null;

        // Parse dates with flexible parsing
        try {
            if (!isEmpty(event.start_date)) {
                startDate = parseFlexibleDate(event.start_date);
            }
        } catch (Exception e) {
            // Already handled in field validation
            return;
        }

        try {
            if (!isEmpty(event.end_date)) {
                endDate = parseFlexibleDate(event.end_date);
            }
        } catch (Exception e) {
            errors.add(eventPrefix + ": Invalid end_date format");
            return;
        }

        // Parse times if not all-day
        if (!event.all_day) {
            try {
                if (!isEmpty(event.start_time)) {
                    startTime = parseFlexibleTime(event.start_time);
                }
            } catch (Exception e) {
                errors.add(eventPrefix + ": Invalid start_time format (use HH:mm or HH:mm:ss)");
            }

            try {
                if (!isEmpty(event.end_time)) {
                    endTime = parseFlexibleTime(event.end_time);
                }
            } catch (Exception e) {
                errors.add(eventPrefix + ": Invalid end_time format (use HH:mm or HH:mm:ss)");
            }
        }

        // Logic validation
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.add(eventPrefix + ": end_date cannot be before start_date");
        }

        if (!event.all_day && startTime != null && endTime != null) {
            if (startDate != null && endDate != null && startDate.equals(endDate)) {
                // Same day event - check time logic
                if (endTime.isBefore(startTime)) {
                    errors.add(eventPrefix + ": end_time cannot be before start_time on same day");
                }
                if (startTime.equals(endTime)) {
                    warnings.add(eventPrefix + ": start_time and end_time are identical");
                }
            }
        }

        // All-day validation
        if (event.all_day && (!isEmpty(event.start_time) || !isEmpty(event.end_time))) {
            warnings.add(eventPrefix + ": Times specified for all-day event (will be ignored)");
        }
    }

    /**
     * Validate Q-Due specific fields
     */
    private static void validateQDueFields(EventPackageJson.EventJson event, String eventPrefix,
                                           List<String> errors, List<String> warnings) {

        // Event type validation
        if (!isEmpty(event.event_type)) {
            if (!VALID_EVENT_TYPES.contains(event.event_type.toUpperCase())) {
                warnings.add(eventPrefix + ": Unknown event_type '" + event.event_type +
                                     "', will default to GENERAL");
            }
        }

        // Priority validation
        if (!isEmpty(event.priority)) {
            if (!VALID_PRIORITIES.contains(event.priority.toUpperCase())) {
                warnings.add(eventPrefix + ": Unknown priority '" + event.priority +
                                     "', will default to NORMAL");
            }
        }
    }

    /**
     * Validate tags array
     */
    private static void validateTags(List<String> tags, String eventPrefix,
                                     List<String> errors, List<String> warnings) {

        if (tags == null) return;

        if (tags.size() > MAX_TAGS_PER_EVENT) {
            warnings.add(eventPrefix + ": Too many tags (" + tags.size() + "), consider reducing");
        }

        Set<String> uniqueTags = new HashSet<>();
        for (String tag : tags) {
            if (isEmpty(tag)) {
                warnings.add(eventPrefix + ": Empty tag found");
            } else if (!uniqueTags.add(tag.toLowerCase())) {
                warnings.add(eventPrefix + ": Duplicate tag '" + tag + "'");
            }
        }
    }

    /**
     * Validate custom properties
     */
    private static void validateCustomProperties(java.util.Map<String, String> properties,
                                                 String eventPrefix, List<String> errors, List<String> warnings) {

        if (properties == null) return;

        if (properties.size() > MAX_CUSTOM_PROPERTIES) {
            warnings.add(eventPrefix + ": Many custom properties (" + properties.size() +
                                 "), consider consolidating");
        }

        for (java.util.Map.Entry<String, String> entry : properties.entrySet()) {
            if (isEmpty(entry.getKey())) {
                warnings.add(eventPrefix + ": Empty key in custom_properties");
            }
            if (isEmpty(entry.getValue())) {
                warnings.add(eventPrefix + ": Empty value for property '" + entry.getKey() + "'");
            }
        }
    }

    /**
     * Perform cross-validation between fields
     */
    private static void performCrossValidation(EventPackageJson packageJson,
                                               List<String> errors, List<String> warnings) {

        if (packageJson.package_info == null || packageJson.events == null) {
            return; // Basic validation already failed
        }

        // Check if events fall within package validity period
        if (!isEmpty(packageJson.package_info.valid_from) &&
                !isEmpty(packageJson.package_info.valid_to)) {

            try {
                LocalDate validFrom = parseFlexibleDate(packageJson.package_info.valid_from);
                LocalDate validTo = parseFlexibleDate(packageJson.package_info.valid_to);

                if (validFrom != null && validTo != null) {
                    for (EventPackageJson.EventJson event : packageJson.events) {
                        if (!isEmpty(event.start_date)) {
                            try {
                                LocalDate eventDate = parseFlexibleDate(event.start_date);
                                if (eventDate != null &&
                                        (eventDate.isBefore(validFrom) || eventDate.isAfter(validTo))) {
                                    warnings.add("Event '" + event.title +
                                                         "' date is outside package validity period");
                                }
                            } catch (Exception e) {
                                // Already handled
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Already handled in package info validation
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Flexible date field validation supporting multiple formats
     */
    private static void validateFlexibleDateField(String dateValue, String fieldName, boolean dateOnly,
                                                  List<String> errors, List<String> warnings) {

        if (isEmpty(dateValue)) return;

        try {
            if (dateOnly) {
                LocalDate parsedDate = parseFlexibleDate(dateValue);
                if (parsedDate == null) {
                    errors.add("Invalid " + fieldName + " format (expected: YYYY-MM-DD or ISO format)");
                }
            } else {
                // Try to parse as date or datetime
                LocalDate parsedDate = parseFlexibleDate(dateValue);
                if (parsedDate == null) {
                    errors.add("Invalid " + fieldName + " format (expected: YYYY-MM-DD or ISO datetime)");
                }
            }
        } catch (Exception e) {
            errors.add("Invalid " + fieldName + " format: " + e.getMessage());
        }
    }

    /**
     * Parse date with multiple format support
     */
    private static LocalDate parseFlexibleDate(String dateString) {
        if (isEmpty(dateString)) return null;

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // Try to parse as LocalDateTime first, then extract date
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                    return dateTime.toLocalDate();
                } catch (DateTimeParseException e1) {
                    // Try to parse as LocalDate directly
                    try {
                        return LocalDate.parse(dateString, formatter);
                    } catch (DateTimeParseException e2) {
                        // Continue to next formatter
                    }
                }
            } catch (Exception e) {
                // Continue to next formatter
                Log.d(TAG, "Date parsing failed with formatter " + formatter + ": " + e.getMessage());
            }
        }

        Log.w(TAG, "Failed to parse date string: " + dateString);
        return null;
    }

    /**
     * Parse time with multiple format support
     */
    private static LocalTime parseFlexibleTime(String timeString) {
        if (isEmpty(timeString)) return null;

        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(timeString, formatter);
            } catch (DateTimeParseException e) {
                // Continue to next formatter
                Log.d(TAG, "Time parsing failed with formatter " + formatter + ": " + e.getMessage());
            }
        }

        Log.w(TAG, "Failed to parse time string: " + timeString);
        throw new DateTimeParseException("Unable to parse time", timeString, 0);
    }

    /**
     * Utility method to check if string is empty or null
     */
    private static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str) || str.trim().isEmpty();
    }
}