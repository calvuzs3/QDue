package net.calvuz.qdue.smartshifts.utils.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for multiple validation results with aggregation capabilities.
 * <p>
 * Collects validation results from multiple fields or operations and provides
 * summary information about overall validation state. Supports filtering by
 * severity and field-specific error reporting.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class MultiValidationResult {

    // ============================================
    // FIELDS
    // ============================================

    private final List<ValidationResult> results;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    /**
     * Create empty multi-validation result
     */
    public MultiValidationResult() {
        this.results = new ArrayList<>();
    }

    /**
     * Create multi-validation result with initial results
     */
    public MultiValidationResult(@NonNull ValidationResult... initialResults) {
        this.results = new ArrayList<>(Arrays.asList(initialResults));
    }

    /**
     * Create multi-validation result from collection
     */
    public MultiValidationResult(@NonNull Collection<ValidationResult> initialResults) {
        this.results = new ArrayList<>(initialResults);
    }

    // ============================================
    // RESULT MANAGEMENT
    // ============================================

    /**
     * Add a validation result
     */
    public void addResult(@NonNull ValidationResult result) {
        results.add(result);
    }

    /**
     * Add multiple validation results
     */
    public void addResults(@NonNull ValidationResult... newResults) {
        results.addAll(Arrays.asList(newResults));
    }

    /**
     * Add multiple validation results from collection
     */
    public void addResults(@NonNull Collection<ValidationResult> newResults) {
        results.addAll(newResults);
    }

    /**
     * Add error result
     */
    public void addError(@NonNull ValidationResult errorResult) {
        if (errorResult.isInvalid()) {
            results.add(errorResult);
        }
    }

    /**
     * Add error with code
     */
    public void addError(@NonNull ValidationError errorCode) {
        results.add(ValidationResult.error(errorCode));
    }

    /**
     * Add error with code and field
     */
    public void addError(@NonNull ValidationError errorCode, @NonNull String fieldName) {
        results.add(ValidationResult.error(errorCode, fieldName));
    }

    /**
     * Add warning result
     */
    public void addWarning(@NonNull ValidationError errorCode) {
        results.add(ValidationResult.warning(errorCode));
    }

    /**
     * Add warning with parameters
     */
    public void addWarning(@NonNull ValidationError errorCode, @NonNull Object... params) {
        results.add(ValidationResult.warning(errorCode, params));
    }

    /**
     * Clear all results
     */
    public void clear() {
        results.clear();
    }

    // ============================================
    // QUERY METHODS
    // ============================================

    /**
     * @return true if all validations passed (no errors)
     */
    public boolean isValid() {
        return getErrorCount() == 0;
    }

    /**
     * @return true if any validation failed
     */
    public boolean isInvalid() {
        return !isValid();
    }

    /**
     * @return true if no results have been added
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * @return true if has any results
     */
    public boolean hasResults() {
        return !results.isEmpty();
    }

    /**
     * @return true if has any warnings
     */
    public boolean hasWarnings() {
        return getWarningCount() > 0;
    }

    /**
     * @return true if has any critical errors
     */
    public boolean hasCriticalErrors() {
        return results.stream().anyMatch(ValidationResult::isCritical);
    }

    // ============================================
    // COUNT METHODS
    // ============================================

    /**
     * @return total number of validation results
     */
    public int getTotalCount() {
        return results.size();
    }

    /**
     * @return number of validation errors
     */
    public int getErrorCount() {
        return (int) results.stream().filter(ValidationResult::isInvalid).count();
    }

    /**
     * @return number of validation warnings
     */
    public int getWarningCount() {
        return (int) results.stream().filter(ValidationResult::isWarning).count();
    }

    /**
     * @return number of critical errors
     */
    public int getCriticalErrorCount() {
        return (int) results.stream().filter(ValidationResult::isCritical).count();
    }

    /**
     * @return number of info messages
     */
    public int getInfoCount() {
        return (int) results.stream().filter(ValidationResult::isInfo).count();
    }

    // ============================================
    // RESULT RETRIEVAL
    // ============================================

    /**
     * @return all validation results (immutable copy)
     */
    @NonNull
    public List<ValidationResult> getAllResults() {
        return new ArrayList<>(results);
    }

    /**
     * @return only error results
     */
    @NonNull
    public List<ValidationResult> getErrors() {
        return results.stream()
                .filter(ValidationResult::isInvalid)
                .collect(Collectors.toList());
    }

    /**
     * @return only warning results
     */
    @NonNull
    public List<ValidationResult> getWarnings() {
        return results.stream()
                .filter(ValidationResult::isWarning)
                .collect(Collectors.toList());
    }

    /**
     * @return only critical error results
     */
    @NonNull
    public List<ValidationResult> getCriticalErrors() {
        return results.stream()
                .filter(ValidationResult::isCritical)
                .collect(Collectors.toList());
    }

    /**
     * @return results for specific field
     */
    @NonNull
    public List<ValidationResult> getResultsForField(@NonNull String fieldName) {
        return results.stream()
                .filter(r -> fieldName.equals(r.getFieldName()))
                .collect(Collectors.toList());
    }

    /**
     * @return errors for specific field
     */
    @NonNull
    public List<ValidationResult> getErrorsForField(@NonNull String fieldName) {
        return results.stream()
                .filter(r -> r.isInvalid() && fieldName.equals(r.getFieldName()))
                .collect(Collectors.toList());
    }

    /**
     * @return first error result, null if none
     */
    @Nullable
    public ValidationResult getFirstError() {
        return results.stream()
                .filter(ValidationResult::isInvalid)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return first critical error result, null if none
     */
    @Nullable
    public ValidationResult getFirstCriticalError() {
        return results.stream()
                .filter(ValidationResult::isCritical)
                .findFirst()
                .orElse(null);
    }

    // ============================================
    // SUMMARY METHODS
    // ============================================

    /**
     * Get error summary for internal logging (English)
     */
    @NonNull
    public String getInternalErrorSummary() {
        if (isValid()) {
            return "No validation errors";
        }

        List<ValidationResult> errors = getErrors();
        if (errors.size() == 1) {
            return errors.get(0).getFormattedInternalMessage();
        }

        StringBuilder summary = new StringBuilder();
        summary.append(errors.size()).append(" validation errors: ");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) summary.append("; ");
            ValidationResult error = errors.get(i);
            if (error.hasFieldName()) {
                summary.append(error.getFieldName()).append(": ");
            }
            summary.append(error.getFormattedInternalMessage());
        }

        return summary.toString();
    }

    /**
     * Get warning summary for internal logging (English)
     */
    @NonNull
    public String getInternalWarningSummary() {
        List<ValidationResult> warnings = getWarnings();
        if (warnings.isEmpty()) {
            return "No validation warnings";
        }

        if (warnings.size() == 1) {
            return warnings.get(0).getFormattedInternalMessage();
        }

        StringBuilder summary = new StringBuilder();
        summary.append(warnings.size()).append(" validation warnings: ");
        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) summary.append("; ");
            ValidationResult warning = warnings.get(i);
            if (warning.hasFieldName()) {
                summary.append(warning.getFieldName()).append(": ");
            }
            summary.append(warning.getFormattedInternalMessage());
        }

        return summary.toString();
    }

    /**
     * Get complete summary for internal logging (English)
     */
    @NonNull
    public String getInternalSummary() {
        if (isEmpty()) {
            return "No validation results";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Validation Summary: ");
        summary.append("Total=").append(getTotalCount());
        summary.append(", Errors=").append(getErrorCount());
        summary.append(", Warnings=").append(getWarningCount());
        summary.append(", Critical=").append(getCriticalErrorCount());

        if (isInvalid()) {
            summary.append("\nErrors: ").append(getInternalErrorSummary());
        }

        if (hasWarnings()) {
            summary.append("\nWarnings: ").append(getInternalWarningSummary());
        }

        return summary.toString();
    }

    /**
     * Get error codes for UI i18n resolution
     */
    @NonNull
    public List<ValidationError> getErrorCodes() {
        return results.stream()
                .filter(ValidationResult::isInvalid)
                .map(ValidationResult::getErrorCode)
                .filter(code -> code != null)
                .collect(Collectors.toList());
    }

    /**
     * Get warning codes for UI i18n resolution
     */
    @NonNull
    public List<ValidationError> getWarningCodes() {
        return results.stream()
                .filter(ValidationResult::isWarning)
                .map(ValidationResult::getErrorCode)
                .filter(code -> code != null)
                .collect(Collectors.toList());
    }

    // ============================================
    // FIELD-SPECIFIC METHODS
    // ============================================

    /**
     * Check if specific field has errors
     */
    public boolean hasErrorsForField(@NonNull String fieldName) {
        return results.stream()
                .anyMatch(r -> r.isInvalid() && fieldName.equals(r.getFieldName()));
    }

    /**
     * Check if specific field has warnings
     */
    public boolean hasWarningsForField(@NonNull String fieldName) {
        return results.stream()
                .anyMatch(r -> r.isWarning() && fieldName.equals(r.getFieldName()));
    }

    /**
     * Get first error for specific field
     */
    @Nullable
    public ValidationResult getFirstErrorForField(@NonNull String fieldName) {
        return results.stream()
                .filter(r -> r.isInvalid() && fieldName.equals(r.getFieldName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all field names that have errors
     */
    @NonNull
    public List<String> getFieldsWithErrors() {
        return results.stream()
                .filter(ValidationResult::isInvalid)
                .map(ValidationResult::getFieldName)
                .filter(name -> name != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get all field names that have warnings
     */
    @NonNull
    public List<String> getFieldsWithWarnings() {
        return results.stream()
                .filter(ValidationResult::isWarning)
                .map(ValidationResult::getFieldName)
                .filter(name -> name != null)
                .distinct()
                .collect(Collectors.toList());
    }

    // ============================================
    // COMBINING METHODS
    // ============================================

    /**
     * Combine with another MultiValidationResult
     */
    @NonNull
    public MultiValidationResult combine(@NonNull MultiValidationResult other) {
        MultiValidationResult combined = new MultiValidationResult();
        combined.addResults(this.results);
        combined.addResults(other.results);
        return combined;
    }

    /**
     * Create a copy with only errors
     */
    @NonNull
    public MultiValidationResult errorsOnly() {
        return new MultiValidationResult(getErrors());
    }

    /**
     * Create a copy with only warnings
     */
    @NonNull
    public MultiValidationResult warningsOnly() {
        return new MultiValidationResult(getWarnings());
    }

    /**
     * Create a copy with only critical errors
     */
    @NonNull
    public MultiValidationResult criticalErrorsOnly() {
        return new MultiValidationResult(getCriticalErrors());
    }

    // ============================================
    // STATIC FACTORY METHODS
    // ============================================

    /**
     * Create from single validation result
     */
    @NonNull
    public static MultiValidationResult of(@NonNull ValidationResult result) {
        return new MultiValidationResult(result);
    }

    /**
     * Create from multiple validation results
     */
    @NonNull
    public static MultiValidationResult of(@NonNull ValidationResult... results) {
        return new MultiValidationResult(results);
    }

    /**
     * Create from collection of validation results
     */
    @NonNull
    public static MultiValidationResult of(@NonNull Collection<ValidationResult> results) {
        return new MultiValidationResult(results);
    }

    /**
     * Create empty result
     */
    @NonNull
    public static MultiValidationResult empty() {
        return new MultiValidationResult();
    }

    /**
     * Create valid result (no errors)
     */
    @NonNull
    public static MultiValidationResult valid() {
        MultiValidationResult result = new MultiValidationResult();
        result.addResult(ValidationResult.success());
        return result;
    }

    // ============================================
    // OBJECT METHODS
    // ============================================

    @NonNull
    @Override
    public String toString() {
        return "MultiValidationResult{" +
                "total=" + getTotalCount() +
                ", errors=" + getErrorCount() +
                ", warnings=" + getWarningCount() +
                ", critical=" + getCriticalErrorCount() +
                ", valid=" + isValid() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiValidationResult that = (MultiValidationResult) o;
        return results.equals(that.results);
    }

    @Override
    public int hashCode() {
        return results.hashCode();
    }
}