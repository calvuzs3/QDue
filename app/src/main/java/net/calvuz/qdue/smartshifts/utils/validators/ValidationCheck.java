package net.calvuz.qdue.smartshifts.utils.validators;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Functional interface for validation operations.
 * <p>
 * Provides a standard contract for validation checks that can be used
 * with lambda expressions and method references for clean, composable
 * validation logic.
 * <p>
 * Example usage:
 * ```java
 * ValidationCheck emailCheck = () ->
 *     ValidationHelper.isValidEmail(email) ?
 *         ValidationResult.success() :
 *         ValidationResult.error(ValidationError.GENERIC_INVALID_EMAIL);
 * <p>
 * ValidationCheck lengthCheck = () ->
 *     ValidationHelper.isValidStringLength(name, 2, 50) ?
 *         ValidationResult.success() :
 *         ValidationResult.error(ValidationError.GENERIC_STRING_TOO_LONG);
 * <p>
 * MultiValidationResult result = ValidationHelper.validateMultiple(emailCheck, lengthCheck);
 * ```
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
@FunctionalInterface
public interface ValidationCheck {

    /**
     * Perform the validation check
     *
     * @return ValidationResult indicating success or failure with error details
     */
    @NonNull
    ValidationResult validate();

    // ============================================
    // DEFAULT METHODS FOR COMPOSITION
    // ============================================

    /**
     * Combine this validation check with another using AND logic
     * Both checks must pass for the result to be valid
     *
     * @param other the validation check to combine with
     * @return a new ValidationCheck that performs both validations
     */
    @NonNull
    default ValidationCheck and(@NonNull ValidationCheck other) {
        return () -> {
            ValidationResult thisResult = this.validate();
            if (thisResult.isInvalid()) {
                return thisResult; // Short-circuit on first failure
            }

            ValidationResult otherResult = other.validate();
            return thisResult.combine(otherResult);
        };
    }

    /**
     * Combine this validation check with another using OR logic
     * At least one check must pass for the result to be valid
     *
     * @param other the validation check to combine with
     * @return a new ValidationCheck that requires only one validation to pass
     */
    @NonNull
    default ValidationCheck or(@NonNull ValidationCheck other) {
        return () -> {
            ValidationResult thisResult = this.validate();
            if (thisResult.isValid()) {
                return thisResult; // Short-circuit on first success
            }

            ValidationResult otherResult = other.validate();
            if (otherResult.isValid()) {
                return otherResult;
            }

            // Both failed, return the first failure
            return thisResult;
        };
    }

    /**
     * Create a conditional validation check that only runs if a condition is met
     *
     * @param condition the condition to check before running validation
     * @return a new ValidationCheck that runs conditionally
     */
    @NonNull
    default ValidationCheck when(boolean condition) {
        return () -> condition ? this.validate() : ValidationResult.success();
    }

    /**
     * Create a conditional validation check based on a supplier condition
     *
     * @param conditionSupplier supplier that provides the condition to check
     * @return a new ValidationCheck that runs conditionally
     */
    @NonNull
    default ValidationCheck when(@NonNull java.util.function.BooleanSupplier conditionSupplier) {
        return () -> conditionSupplier.getAsBoolean() ? this.validate() : ValidationResult.success();
    }

    /**
     * Transform the validation result if it fails
     *
     * @param transformer function to transform failed results
     * @return a new ValidationCheck with transformed failure results
     */
    @NonNull
    default ValidationCheck mapError(@NonNull java.util.function.Function<ValidationResult, ValidationResult> transformer) {
        return () -> {
            ValidationResult result = this.validate();
            return result.isInvalid() ? transformer.apply(result) : result;
        };
    }

    // ============================================
    // STATIC FACTORY METHODS
    // ============================================

    /**
     * Create a validation check that always succeeds
     *
     * @return ValidationCheck that always returns success
     */
    @NonNull
    static ValidationCheck alwaysValid() {
        return ValidationResult::success;
    }

    /**
     * Create a validation check that always fails with specified error
     *
     * @param errorCode the error code for the failure
     * @return ValidationCheck that always returns the specified error
     */
    @NonNull
    static ValidationCheck alwaysInvalid(@NonNull ValidationError errorCode) {
        return () -> ValidationResult.error(errorCode);
    }

    /**
     * Create a validation check from a boolean condition
     *
     * @param condition the condition to check
     * @param errorCode the error code if condition is false
     * @return ValidationCheck based on the condition
     */
    @NonNull
    static ValidationCheck fromCondition(boolean condition, @NonNull ValidationError errorCode) {
        return () -> condition ? ValidationResult.success() : ValidationResult.error(errorCode);
    }

    /**
     * Create a validation check from a boolean supplier
     *
     * @param conditionSupplier supplier that provides the condition
     * @param errorCode the error code if condition is false
     * @return ValidationCheck based on the condition supplier
     */
    @NonNull
    static ValidationCheck fromCondition(@NonNull java.util.function.BooleanSupplier conditionSupplier,
                                         @NonNull ValidationError errorCode) {
        return () -> conditionSupplier.getAsBoolean() ? ValidationResult.success() : ValidationResult.error(errorCode);
    }

    /**
     * Create a validation check that validates a required string field
     *
     * @param fieldName the name of the field being validated
     * @param value the value to validate
     * @return ValidationCheck for required string validation
     */
    @NonNull
    static ValidationCheck requiredString(@NonNull String fieldName, String value) {
        return () -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.requiredField(fieldName);
            }
            return ValidationResult.success();
        };
    }

    /**
     * Create a validation check for string length
     *
     * @param fieldName the name of the field being validated
     * @param value the value to validate
     * @param minLength minimum required length
     * @param maxLength maximum allowed length
     * @return ValidationCheck for string length validation
     */
    @NonNull
    static ValidationCheck stringLength(@NonNull String fieldName, String value, int minLength, int maxLength) {
        return () -> {
            if (value == null) {
                return ValidationResult.success(); // Let required check handle null
            }

            int length = value.trim().length();
            if (length < minLength) {
                return ValidationResult.stringTooShort(fieldName, minLength);
            }
            if (length > maxLength) {
                return ValidationResult.stringTooLong(fieldName, maxLength);
            }

            return ValidationResult.success();
        };
    }

    /**
     * Create a validation check for numeric range
     *
     * @param fieldName the name of the field being validated
     * @param value the value to validate
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return ValidationCheck for numeric range validation
     */
    @NonNull
    static ValidationCheck numericRange(@NonNull String fieldName, String value, int min, int max) {
        return () -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.success(); // Let required check handle empty
            }

            try {
                int intValue = Integer.parseInt(value.trim());
                if (intValue < min || intValue > max) {
                    return ValidationResult.outOfRange(fieldName, min, max);
                }
                return ValidationResult.success();
            } catch (NumberFormatException e) {
                return ValidationResult.invalidFormat(fieldName);
            }
        };
    }

    /**
     * Create a validation check for email format
     *
     * @param fieldName the name of the field being validated
     * @param email the email to validate
     * @return ValidationCheck for email validation
     */
    @NonNull
    static ValidationCheck emailFormat(@NonNull String fieldName, String email) {
        return () -> {
            if (email == null || email.trim().isEmpty()) {
                return ValidationResult.success(); // Let required check handle empty
            }

            // Use StringHelper for email validation
            if (!net.calvuz.qdue.smartshifts.utils.StringHelper.isValidEmail(email)) {
                return ValidationResult.error(ValidationError.GENERIC_INVALID_EMAIL, fieldName);
            }

            return ValidationResult.success();
        };
    }

    /**
     * Create a validation check for phone number format
     *
     * @param fieldName the name of the field being validated
     * @param phone the phone number to validate
     * @return ValidationCheck for phone validation
     */
    @NonNull
    static ValidationCheck phoneFormat(@NonNull String fieldName, String phone) {
        return () -> {
            if (phone == null || phone.trim().isEmpty()) {
                return ValidationResult.success(); // Let required check handle empty
            }

            // Use StringHelper for phone validation
            if (!net.calvuz.qdue.smartshifts.utils.StringHelper.isValidPhoneNumber(phone)) {
                return ValidationResult.error(ValidationError.GENERIC_INVALID_PHONE, fieldName);
            }

            return ValidationResult.success();
        };
    }

    /**
     * Create a validation check for time format
     *
     * @param fieldName the name of the field being validated
     * @param timeString the time string to validate
     * @return ValidationCheck for time format validation
     */
    @NonNull
    static ValidationCheck timeFormat(@NonNull String fieldName, String timeString) {
        return () -> {
            if (timeString == null || timeString.trim().isEmpty()) {
                return ValidationResult.success(); // Let required check handle empty
            }

            // Use ValidationHelper for time validation
            if (!net.calvuz.qdue.smartshifts.utils.ValidationHelper.isValidTimeFormat(timeString)) {
                return ValidationResult.invalidTimeFormat(fieldName);
            }

            return ValidationResult.success();
        };
    }

    // ============================================
    // COMBINATION UTILITIES
    // ============================================

    /**
     * Combine multiple validation checks with AND logic
     * All checks must pass for the result to be valid
     *
     * @param checks the validation checks to combine
     * @return a new ValidationCheck that performs all validations
     */
    @NonNull
    static ValidationCheck all(@NonNull ValidationCheck... checks) {
        return () -> {
            for (ValidationCheck check : checks) {
                ValidationResult result = check.validate();
                if (result.isInvalid()) {
                    return result; // Short-circuit on first failure
                }
            }
            return ValidationResult.success();
        };
    }

    /**
     * Combine multiple validation checks with OR logic
     * At least one check must pass for the result to be valid
     *
     * @param checks the validation checks to combine
     * @return a new ValidationCheck that requires only one validation to pass
     */
    @NonNull
    static ValidationCheck any(@NonNull ValidationCheck... checks) {
        return () -> {
            ValidationResult firstFailure = null;

            for (ValidationCheck check : checks) {
                ValidationResult result = check.validate();
                if (result.isValid()) {
                    return result; // Short-circuit on first success
                }
                if (firstFailure == null) {
                    firstFailure = result;
                }
            }

            // All failed, return the first failure
            return firstFailure != null ? firstFailure :
                    ValidationResult.error(ValidationError.GENERIC_INVALID_FORMAT);
        };
    }

    /**
     * Create a validation check that collects all results without short-circuiting
     * Useful when you want to see all validation errors at once
     *
     * @param checks the validation checks to run
     * @return a ValidationCheck that returns a MultiValidationResult
     */
    @NonNull
    static ValidationCheck collectAll(@NonNull ValidationCheck... checks) {
        return () -> {
            MultiValidationResult multiResult = new MultiValidationResult();

            for (ValidationCheck check : checks) {
                ValidationResult result = check.validate();
                multiResult.addResult(result);
            }

            // Return the first error if any, otherwise success
            return multiResult.isValid() ? ValidationResult.success() : Objects.requireNonNull(multiResult.getFirstError());
        };
    }
}