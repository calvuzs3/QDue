package net.calvuz.qdue.smartshifts.utils.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Enhanced validation result container with internationalization support.
 * <p>
 * Provides validation results with error codes that can be resolved to
 * localized messages in the UI layer, while maintaining internal English
 * messages for logging and debugging.
 * <p>
 * Key features:
 * - Error codes for i18n support
 * - Internal English messages for logs
 * - Message parameters for dynamic content
 * - Field-specific validation results
 * - Severity levels (error, warning, info)
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class ValidationResult {

    // ============================================
    // CONSTANTS
    // ============================================

    /** Success result instance for reuse */
    public static final ValidationResult SUCCESS = new ValidationResult(true, null, "Validation passed", null, null);

    // ============================================
    // FIELDS
    // ============================================

    private final boolean valid;
    private final ValidationError errorCode;
    private final String internalMessage;
    private final String fieldName;
    private final Object[] messageParams;
    private final Severity severity;

    // ============================================
    // ENUMS
    // ============================================

    /**
     * Validation result severity levels
     */
    public enum Severity {
        /** Critical error that blocks operation */
        ERROR,
        /** Warning that allows operation but indicates issues */
        WARNING,
        /** Informational message */
        INFO
    }

    // ============================================
    // CONSTRUCTORS
    // ============================================

    /**
     * Create a successful validation result
     */
    public ValidationResult(boolean valid, @Nullable ValidationError errorCode, @NonNull String internalMessage,
                            @Nullable String fieldName, @Nullable Object[] messageParams) {
        this.valid = valid;
        this.errorCode = errorCode;
        this.internalMessage = internalMessage;
        this.fieldName = fieldName;
        this.messageParams = messageParams != null ? Arrays.copyOf(messageParams, messageParams.length) : null;
        this.severity = determineSeverity(valid, errorCode);
    }

    /**
     * Create validation result without field name
     */
    public ValidationResult(boolean valid, @Nullable ValidationError errorCode, @NonNull String internalMessage) {
        this(valid, errorCode, internalMessage, null, null);
    }

    /**
     * Create validation result with message parameters
     */
    public ValidationResult(boolean valid, @Nullable ValidationError errorCode, @NonNull String internalMessage,
                            @Nullable Object[] messageParams) {
        this(valid, errorCode, internalMessage, null, messageParams);
    }

    /**
     * Create error result with error code
     */
    public static ValidationResult error(@NonNull ValidationError errorCode) {
        return new ValidationResult(false, errorCode, errorCode.getInternalMessage());
    }

    /**
     * Create error result with error code and field
     */
    public static ValidationResult error(@NonNull ValidationError errorCode, @NonNull String fieldName) {
        return new ValidationResult(false, errorCode, errorCode.getInternalMessage(), fieldName, null);
    }

    /**
     * Create error result with error code and parameters
     */
    public static ValidationResult error(@NonNull ValidationError errorCode, @Nullable Object... params) {
        return new ValidationResult(false, errorCode, errorCode.getInternalMessage(), params);
    }

    /**
     * Create error result with error code, field, and parameters
     */
    public static ValidationResult error(@NonNull ValidationError errorCode, @NonNull String fieldName,
                                         @Nullable Object... params) {
        return new ValidationResult(false, errorCode, errorCode.getInternalMessage(), fieldName, params);
    }

    /**
     * Create warning result
     */
    public static ValidationResult warning(@NonNull ValidationError errorCode) {
        return new ValidationResult(true, errorCode, errorCode.getInternalMessage());
    }

    /**
     * Create warning result with parameters
     */
    public static ValidationResult warning(@NonNull ValidationError errorCode, @Nullable Object... params) {
        return new ValidationResult(true, errorCode, errorCode.getInternalMessage(), params);
    }

    /**
     * Create success result
     */
    public static ValidationResult success() {
        return SUCCESS;
    }

    /**
     * Create success result with custom message
     */
    public static ValidationResult success(@NonNull String internalMessage) {
        return new ValidationResult(true, null, internalMessage);
    }

    // ============================================
    // PRIVATE METHODS
    // ============================================

    /**
     * Determine severity based on validation state and error code
     */
    private Severity determineSeverity(boolean valid, @Nullable ValidationError errorCode) {
        if (valid && errorCode == null) {
            return Severity.INFO;
        }

        if (valid) {
            return Severity.WARNING; // Valid but with warning
        }

        return Severity.ERROR; // Default for invalid results
    }

    // ============================================
    // GETTERS
    // ============================================

    /**
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return true if validation failed
     */
    public boolean isInvalid() {
        return !valid;
    }

    /**
     * @return the error code for i18n resolution, null if valid
     */
    @Nullable
    public ValidationError getErrorCode() {
        return errorCode;
    }

    /**
     * @return internal English message for logging
     */
    @NonNull
    public String getInternalMessage() {
        return internalMessage;
    }

    /**
     * @return field name that failed validation, null if not field-specific
     */
    @Nullable
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return message parameters for dynamic i18n, null if none
     */
    @Nullable
    public Object[] getMessageParams() {
        return messageParams != null ? Arrays.copyOf(messageParams, messageParams.length) : null;
    }

    /**
     * @return validation result severity
     */
    @NonNull
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return true if this is a critical error
     */
    public boolean isCritical() {
        return severity == Severity.ERROR && (errorCode == null || errorCode.isCritical());
    }

    /**
     * @return true if this is a warning
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    /**
     * @return true if this is informational
     */
    public boolean isInfo() {
        return severity == Severity.INFO;
    }

    /**
     * @return true if has error code
     */
    public boolean hasErrorCode() {
        return errorCode != null;
    }

    /**
     * @return true if has field name
     */
    public boolean hasFieldName() {
        return fieldName != null;
    }

    /**
     * @return true if has message parameters
     */
    public boolean hasMessageParams() {
        return messageParams != null && messageParams.length > 0;
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get Android string resource key for this error
     * Format: "validation_error_{error_code}"
     */
    @Nullable
    public String getResourceKey() {
        return errorCode != null ? errorCode.getResourceKey() : null;
    }

    /**
     * Get Android string resource key with custom prefix
     */
    @Nullable
    public String getResourceKey(@NonNull String prefix) {
        return errorCode != null ? errorCode.getResourceKey(prefix) : null;
    }

    /**
     * Format internal message with parameters for logging
     */
    @NonNull
    public String getFormattedInternalMessage() {
        if (messageParams != null && messageParams.length > 0) {
            try {
                return String.format(Locale.ENGLISH, internalMessage, messageParams);
            } catch (Exception e) {
                // If formatting fails, return original message with params appended
                StringBuilder sb = new StringBuilder(internalMessage);
                sb.append(" [");
                for (int i = 0; i < messageParams.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(messageParams[i]);
                }
                sb.append("]");
                return sb.toString();
            }
        }
        return internalMessage;
    }

    /**
     * Create a new ValidationResult with different field name
     */
    @NonNull
    public ValidationResult withFieldName(@NonNull String newFieldName) {
        return new ValidationResult(valid, errorCode, internalMessage, newFieldName, messageParams);
    }

    /**
     * Create a new ValidationResult with additional parameters
     */
    @NonNull
    public ValidationResult withParams(@NonNull Object... params) {
        return new ValidationResult(valid, errorCode, internalMessage, fieldName, params);
    }

    /**
     * Combine this result with another (returns first error found)
     */
    @NonNull
    public ValidationResult combine(@NonNull ValidationResult other) {
        if (this.isInvalid()) {
            return this;
        }
        if (other.isInvalid()) {
            return other;
        }
        if (this.isWarning()) {
            return this;
        }
        if (other.isWarning()) {
            return other;
        }
        return this; // Both are success
    }

    // ============================================
    // OBJECT METHODS
    // ============================================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("valid=").append(valid);
        sb.append(", severity=").append(severity);
        if (errorCode != null) {
            sb.append(", errorCode=").append(errorCode);
        }
        sb.append(", message='").append(internalMessage).append('\'');
        if (fieldName != null) {
            sb.append(", field='").append(fieldName).append('\'');
        }
        if (messageParams != null && messageParams.length > 0) {
            sb.append(", params=").append(Arrays.toString(messageParams));
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationResult that = (ValidationResult) o;

        if (valid != that.valid) return false;
        if (errorCode != that.errorCode) return false;
        if (!internalMessage.equals(that.internalMessage)) return false;
        if (!Objects.equals(fieldName, that.fieldName)) return false;
        if (!Arrays.equals(messageParams, that.messageParams)) return false;
        return severity == that.severity;
    }

    @Override
    public int hashCode() {
        int result = (valid ? 1 : 0);
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        result = 31 * result + internalMessage.hashCode();
        result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(messageParams);
        result = 31 * result + (severity != null ? severity.hashCode() : 0);
        return result;
    }

    // ============================================
    // CONVENIENCE FACTORY METHODS
    // ============================================

    /**
     * Create field-specific required validation error
     */
    public static ValidationResult requiredField(@NonNull String fieldName) {
        return error(ValidationError.GENERIC_REQUIRED_FIELD_MISSING, fieldName);
    }

    /**
     * Create field-specific format validation error
     */
    public static ValidationResult invalidFormat(@NonNull String fieldName) {
        return error(ValidationError.GENERIC_INVALID_FORMAT, fieldName);
    }

    /**
     * Create field-specific range validation error
     */
    public static ValidationResult outOfRange(@NonNull String fieldName, Object min, Object max) {
        return error(ValidationError.GENERIC_OUT_OF_RANGE, fieldName, min, max);
    }

    /**
     * Create string length validation error
     */
    public static ValidationResult stringTooLong(@NonNull String fieldName, int maxLength) {
        return error(ValidationError.GENERIC_STRING_TOO_LONG, fieldName, maxLength);
    }

    /**
     * Create string length validation error
     */
    public static ValidationResult stringTooShort(@NonNull String fieldName, int minLength) {
        return error(ValidationError.GENERIC_STRING_TOO_SHORT, fieldName, minLength);
    }

    /**
     * Create time format validation error
     */
    public static ValidationResult invalidTimeFormat(@NonNull String fieldName) {
        return error(ValidationError.TIME_INVALID_FORMAT, fieldName);
    }

    /**
     * Create shift duration validation error
     */
    public static ValidationResult shiftDurationTooShort() {
        return error(ValidationError.SHIFT_DURATION_TOO_SHORT);
    }

    /**
     * Create shift duration validation error
     */
    public static ValidationResult shiftDurationTooLong() {
        return error(ValidationError.SHIFT_DURATION_TOO_LONG);
    }

    /**
     * Create shift overlap validation error
     */
    public static ValidationResult shiftTimeOverlap(@NonNull String shift1, @NonNull String shift2) {
        return error(ValidationError.SHIFT_TIME_OVERLAP, shift1, shift2);
    }

    /**
     * Create team count validation error
     */
    public static ValidationResult teamCountInvalid(int count, int min, int max) {
        if (count < min) {
            return error(ValidationError.TEAM_COUNT_TOO_LOW, count, min);
        } else {
            return error(ValidationError.TEAM_COUNT_TOO_HIGH, count, max);
        }
    }

    /**
     * Create pattern coverage gap error
     */
    public static ValidationResult coverageGap(@NonNull String startTime, @NonNull String endTime) {
        return error(ValidationError.PATTERN_COVERAGE_GAP, startTime, endTime);
    }

    /**
     * Create fatigue risk warning
     */
    public static ValidationResult fatigueRiskWarning(@NonNull String riskType, double percentage) {
        return warning(ValidationError.FATIGUE_HIGH_NIGHT_SHIFT_RATIO, riskType, percentage);
    }
}