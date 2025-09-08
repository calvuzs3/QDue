package net.calvuz.qdue.domain.calendar.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.Shift;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ShiftRepository - Domain interface for shift template management and operations.
 *
 * <p>Defines business-oriented operations for managing shift templates, predefined shifts,
 * and shift-related operations within the work scheduling system. Focuses on shift
 * definitions that can be used in schedule generation and exception handling.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Shift Template Management</strong>: CRUD operations for shift definitions</li>
 *   <li><strong>Predefined Shifts</strong>: Access to system default shifts</li>
 *   <li><strong>Shift Validation</strong>: Ensure shift validity and consistency</li>
 *   <li><strong>Shift Discovery</strong>: Find shifts by various criteria</li>
 * </ul>
 *
 * <h3>Shift Categories:</h3>
 * <ul>
 *   <li><strong>System Shifts</strong>: Predefined morning, afternoon, night shifts</li>
 *   <li><strong>Custom Shifts</strong>: User-defined shift templates</li>
 *   <li><strong>Exception Shifts</strong>: Temporary shifts for exception handling</li>
 *   <li><strong>Pattern Shifts</strong>: Shifts defined within recurring patterns</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><strong>Unique Identification</strong>: Shift IDs must be unique across system</li>
 *   <li><strong>Time Validation</strong>: Start/end times must be valid</li>
 *   <li><strong>Midnight Crossing</strong>: Support for shifts spanning midnight</li>
 *   <li><strong>Template Reuse</strong>: Shifts can be used in multiple contexts</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Domain Interface
 * @since Clean Architecture Phase 2
 */
public interface ShiftRepository {

    // ==================== SHIFT CRUD OPERATIONS ====================

    /**
     * Get shift by unique identifier.
     *
     * @param shiftId Shift unique identifier
     * @return CompletableFuture with Shift or null if not found
     */
    @NonNull
    CompletableFuture<Shift> getShiftById(@NonNull String shiftId);

    /**
     * Get shift by name.
     *
     * @param shiftName Shift name
     * @return CompletableFuture with Shift or null if not found
     */
    @NonNull
    CompletableFuture<Shift> getShiftByName(@NonNull String shiftName);

    /**
     * Get all available shifts in the system.
     *
     * @return CompletableFuture with List of all Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> getAllShifts();

    /**
     * Get all shifts of specific type.
     *
     * @param shiftType Shift type to filter by
     * @return CompletableFuture with List of Shift objects of specified type
     */
    @NonNull
    CompletableFuture<List<Shift>> getShiftsByType(@NonNull Shift.ShiftType shiftType);

    /**
     * Save shift (create or update).
     *
     * @param shift Shift to save
     * @return CompletableFuture with saved Shift
     */
    @NonNull
    CompletableFuture<Shift> saveShift(@NonNull Shift shift);

    /**
     * Delete shift.
     *
     * @param shiftId Shift ID to delete
     * @return CompletableFuture with boolean indicating success
     */
    @NonNull
    CompletableFuture<Boolean> deleteShift(@NonNull String shiftId);

    // ==================== PREDEFINED SHIFTS ====================

    /**
     * Get standard system shift templates.
     *
     * <p>Returns the predefined shifts used by the system including
     * morning, afternoon, night, and QuattroDue cycle shifts.</p>
     *
     * @return CompletableFuture with List of system default Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> getSystemDefaultShifts();

    /**
     * Get morning shift template.
     *
     * @return CompletableFuture with morning Shift template
     */
    @NonNull
    CompletableFuture<Shift> getMorningShift();

    /**
     * Get afternoon shift template.
     *
     * @return CompletableFuture with afternoon Shift template
     */
    @NonNull
    CompletableFuture<Shift> getAfternoonShift();

    /**
     * Get night shift template.
     *
     * @return CompletableFuture with night Shift template
     */
    @NonNull
    CompletableFuture<Shift> getNightShift();

    /**
     * Get QuattroDue cycle shift template.
     *
     * @return CompletableFuture with QuattroDue Shift template
     */
    @NonNull
    CompletableFuture<Shift> getQuattroDueCycleShift();

    // ==================== SHIFT VALIDATION ====================

    /**
     * Validate shift definition.
     *
     * @param shift Shift to validate
     * @return CompletableFuture with ValidationResult
     */
    @NonNull
    CompletableFuture<ValidationResult> validateShift(@NonNull Shift shift);

    /**
     * Check if shift ID exists.
     *
     * @param shiftId Shift ID to check
     * @return CompletableFuture with boolean indicating existence
     */
    @NonNull
    CompletableFuture<Boolean> shiftExists(@NonNull String shiftId);

    /**
     * Check if shift name is available.
     *
     * @param shiftName Shift name to check
     * @return CompletableFuture with boolean indicating availability
     */
    @NonNull
    CompletableFuture<Boolean> isShiftNameAvailable(@NonNull String shiftName);

    /**
     * Find conflicting shifts.
     *
     * <p>Finds shifts that have overlapping time ranges or identical timing.</p>
     *
     * @param shift Shift to check for conflicts
     * @return CompletableFuture with List of conflicting Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> findConflictingShifts(@NonNull Shift shift);

    // ==================== BUSINESS OPERATIONS ====================

    /**
     * Get shifts suitable for replacement in exceptions.
     *
     * <p>Returns shifts that can be used as replacements in shift exceptions,
     * filtered by compatibility and business rules.</p>
     *
     * @param originalShiftId Original shift being replaced
     * @return CompletableFuture with List of suitable replacement Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> getReplacementShiftsFor(@NonNull String originalShiftId);

    /**
     * Get shifts compatible with specific time requirements.
     *
     * @param requiredStartTime Required start time
     * @param requiredEndTime   Required end time
     * @param allowTimeVariance Whether to allow small time variances
     * @return CompletableFuture with List of compatible Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> getCompatibleShifts(@NonNull LocalTime requiredStartTime,
                                                       @NonNull LocalTime requiredEndTime,
                                                       boolean allowTimeVariance);

    /**
     * Create shift template from parameters.
     *
     * <p>Creates a temporary shift template for exception handling without
     * persisting it to the database.</p>
     *
     * @param name      Shift name
     * @param startTime Start time
     * @param endTime   End time
     * @param shiftType Optional shift type
     * @return CompletableFuture with created Shift template
     */
    @NonNull
    CompletableFuture<Shift> createShiftTemplate(@NonNull String name,
                                                 @NonNull LocalTime startTime,
                                                 @NonNull LocalTime endTime,
                                                 @Nullable Shift.ShiftType shiftType);

    /**
     * Duplicate shift with modifications.
     *
     * @param sourceShiftId Source shift ID to duplicate
     * @param newName       New name for duplicated shift
     * @param modifications Optional modifications to apply
     * @return CompletableFuture with duplicated Shift
     */
    @NonNull
    CompletableFuture<Shift> duplicateShift(@NonNull String sourceShiftId,
                                            @NonNull String newName,
                                            @Nullable ShiftModifications modifications);

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get shift usage statistics.
     *
     * @return CompletableFuture with ShiftStatistics
     */
    @NonNull
    CompletableFuture<ShiftStatistics> getShiftStatistics();

    /**
     * Get most used shifts.
     *
     * @param limit Maximum number of shifts to return
     * @return CompletableFuture with List of most used Shift objects
     */
    @NonNull
    CompletableFuture<List<Shift>> getMostUsedShifts(int limit);

    /**
     * Get shifts by usage frequency.
     *
     * @param ascending True for ascending order, false for descending
     * @return CompletableFuture with List of Shift objects ordered by usage
     */
    @NonNull
    CompletableFuture<List<Shift>> getShiftsByUsageFrequency(boolean ascending);

    // ==================== BULK OPERATIONS ====================

    /**
     * Save multiple shifts.
     *
     * @param shifts List of shifts to save
     * @return CompletableFuture with count of successfully saved shifts
     */
    @NonNull
    CompletableFuture<Integer> saveShifts(@NonNull List<Shift> shifts);

    /**
     * Delete multiple shifts.
     *
     * @param shiftIds List of shift IDs to delete
     * @return CompletableFuture with count of successfully deleted shifts
     */
    @NonNull
    CompletableFuture<Integer> deleteShifts(@NonNull List<String> shiftIds);

    /**
     * Import shifts from external source.
     *
     * @param shifts            List of shifts to import
     * @param overwriteExisting Whether to overwrite existing shifts
     * @return CompletableFuture with ImportResult
     */
    @NonNull
    CompletableFuture<ImportResult> importShifts(@NonNull List<Shift> shifts, boolean overwriteExisting);

    // ==================== INNER CLASSES ====================

    /**
     * Shift validation result.
     */
    record ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
        public ValidationResult(boolean isValid, @NonNull List<String> errors, @NonNull List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Shift modifications for duplication.
     */
    record ShiftModifications(@Nullable LocalTime newStartTime, @Nullable LocalTime newEndTime,
                              @Nullable String newColorHex, @Nullable Duration newBreakDuration,
                              @Nullable Shift.ShiftType newShiftType) {
    }

    /**
     * Shift usage statistics.
     */
    record ShiftStatistics(int totalShifts, int systemShifts, int customShifts,
                           int shiftsWithBreaks, int midnightCrossingShifts,
                           long averageDurationMinutes, Shift mostUsedShift, Shift longestShift,
                           Shift shortestShift) {
        public ShiftStatistics(int totalShifts, int systemShifts, int customShifts,
                               int shiftsWithBreaks, int midnightCrossingShifts,
                               long averageDurationMinutes,
                               @Nullable Shift mostUsedShift, @Nullable Shift longestShift,
                               @Nullable Shift shortestShift) {
            this.totalShifts = totalShifts;
            this.systemShifts = systemShifts;
            this.customShifts = customShifts;
            this.shiftsWithBreaks = shiftsWithBreaks;
            this.midnightCrossingShifts = midnightCrossingShifts;
            this.averageDurationMinutes = averageDurationMinutes;
            this.mostUsedShift = mostUsedShift;
            this.longestShift = longestShift;
            this.shortestShift = shortestShift;
        }
    }

    /**
     * Shift import result.
     */
    record ImportResult(int totalShifts, int successfulImports, int skippedShifts,
                        int failedImports, List<String> errors) {
        public ImportResult(int totalShifts, int successfulImports, int skippedShifts,
                            int failedImports, @NonNull List<String> errors) {
            this.totalShifts = totalShifts;
            this.successfulImports = successfulImports;
            this.skippedShifts = skippedShifts;
            this.failedImports = failedImports;
            this.errors = errors;
        }

        public boolean isSuccessful() {
            return failedImports == 0;
        }

        public boolean hasPartialSuccess() {
            return successfulImports > 0 && failedImports > 0;
        }
    }
}