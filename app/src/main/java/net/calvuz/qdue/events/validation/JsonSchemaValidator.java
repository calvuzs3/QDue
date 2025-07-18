package net.calvuz.qdue.events.validation;

import android.text.TextUtils;

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
 * FIXED: Advanced JSON Schema Validator for Q-Due Events
 * <p>
 * Fixed date validation to support multiple formats:
 * - ISO 8601 dates: "2025-01-15"
 * - ISO 8601 datetime: "2025-01-15T10:30:00Z"
 * - ISO 8601 datetime with timezone: "2025-01-15T10:30:00+01:00"
 * - Flexible date formats
 */
public class JsonSchemaValidator {

    private static final String TAG = "EV_JSON_VALID";

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

    // FIXED: Multiple date/time formatters for flexible parsing
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

    /**
     * Validation result with detailed error information
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final List<String> warnings;
        public final List<String> detailedErrors;

        public ValidationResult(boolean isValid, String errorMessage,
                                List<String> warnings, List<String> detailedErrors) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.detailedErrors = detailedErrors != null ? detailedErrors : new ArrayList<>();
        }

        public static ValidationResult valid(List<String> warnings) {
            return new ValidationResult(true, null, warnings, null);
        }

        public static ValidationResult invalid(String error, List<String> detailedErrors) {
            return new ValidationResult(false, error, null, detailedErrors);
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getFullErrorMessage() {
            if (isValid) return null;

            StringBuilder sb = new StringBuilder(errorMessage);
            if (!detailedErrors.isEmpty()) {
                sb.append("\n\nDetailed errors:");
                for (String error : detailedErrors) {
                    sb.append("\n• ").append(error);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Main validation method for complete package
     */
    public static ValidationResult validatePackage(EventPackageJson packageJson) {
        Log.d(TAG, "Starting comprehensive package validation");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Basic structure validation
            if (packageJson == null) {
                return ValidationResult.invalid("Package is null", null);
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
                return ValidationResult.invalid(mainError, errors);
            }

            Log.d(TAG, "Package validation completed successfully with " + warnings.size() + " warnings");
            return ValidationResult.valid(warnings);

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during validation", e);
            return ValidationResult.invalid("Validation failed: " + e.getMessage(), null);
        }
    }

    /**
     * FIXED: Validate package_info section with flexible date parsing
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

        // FIXED: Flexible date validation
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
     * FIXED: Validate event date/time logic with flexible parsing
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

    /**
     * FIXED: Flexible date field validation supporting multiple formats
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
     * FIXED: Parse date with multiple format support
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
     * FIXED: Parse time with multiple format support
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