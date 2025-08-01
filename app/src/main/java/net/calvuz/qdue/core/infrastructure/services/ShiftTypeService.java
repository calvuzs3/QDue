package net.calvuz.qdue.core.infrastructure.services;

import net.calvuz.qdue.core.domain.quattrodue.models.ShiftType;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ REFACTORED: ShiftType Service following existing architecture patterns
 *
 * <p>Service interface for shift type management operations using consistent
 * async patterns with OperationResult for structured error handling.</p>
 *
 * <p>Key architectural features:</p>
 * <ul>
 *   <li>All operations return CompletableFuture&lt;OperationResult&lt;T&gt;&gt;</li>
 *   <li>Consistent error handling and logging</li>
 *   <li>Health monitoring and lifecycle management</li>
 *   <li>Integration with existing ServiceManager pattern</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 2.0
 * @since Database Version 5
 */
public interface ShiftTypeService {

    // ==================== RETRIEVAL OPERATIONS ====================

    /**
     * Gets all active shift types available for scheduling.
     *
     * @return CompletableFuture with list of active shift types
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getAllActiveShiftTypes();

    /**
     * Gets a shift type by its unique identifier.
     *
     * @param id shift type ID
     * @return CompletableFuture with shift type if found
     */
    CompletableFuture<OperationResult<ShiftType>> getShiftTypeById(Long id);

    /**
     * Gets a shift type by its name (case insensitive).
     *
     * @param name shift type name
     * @return CompletableFuture with shift type if found
     */
    CompletableFuture<OperationResult<ShiftType>> getShiftTypeByName(String name);

    /**
     * Gets all predefined system shift types.
     *
     * @return CompletableFuture with list of predefined shift types
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getPredefinedShiftTypes();

    /**
     * Gets all user-defined custom shift types.
     *
     * @return CompletableFuture with list of user-defined shift types
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getUserDefinedShiftTypes();

    /**
     * Gets all rest period shift types.
     *
     * @return CompletableFuture with list of rest periods
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getRestPeriods();

    /**
     * Gets all work shift types (excluding rest periods).
     *
     * @return CompletableFuture with list of work shifts
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getWorkShifts();

    // ==================== CREATION OPERATIONS ====================

    /**
     * Creates a new user-defined work shift.
     *
     * @param name shift type name (must be unique)
     * @param description optional description
     * @param startTime shift start time
     * @param endTime shift end time (can cross midnight)
     * @param colorHex hex color code for visualization
     * @return CompletableFuture with created shift type
     */
    CompletableFuture<OperationResult<ShiftType>> createWorkShift(String name, String description,
                                                                  LocalTime startTime, LocalTime endTime,
                                                                  String colorHex);

    /**
     * Creates a new user-defined rest period.
     *
     * @param name rest period name (must be unique)
     * @param description optional description
     * @param colorHex hex color code for visualization
     * @return CompletableFuture with created rest period
     */
    CompletableFuture<OperationResult<ShiftType>> createRestPeriod(String name, String description,
                                                                   String colorHex);

    /**
     * Creates a new user-defined work shift with break time.
     *
     * @param name shift type name (must be unique)
     * @param description optional description
     * @param startTime shift start time
     * @param endTime shift end time
     * @param colorHex hex color code for visualization
     * @param breakStartTime break start time
     * @param breakEndTime break end time
     * @return CompletableFuture with created shift type
     */
    CompletableFuture<OperationResult<ShiftType>> createWorkShiftWithBreak(String name, String description,
                                                                           LocalTime startTime, LocalTime endTime,
                                                                           String colorHex, LocalTime breakStartTime,
                                                                           LocalTime breakEndTime);

    // ==================== MODIFICATION OPERATIONS ====================

    /**
     * Updates an existing user-defined shift type.
     * Only user-defined shift types can be modified.
     *
     * @param shiftType shift type to update
     * @return CompletableFuture with updated shift type
     */
    CompletableFuture<OperationResult<ShiftType>> updateShiftType(ShiftType shiftType);

    /**
     * Deactivates a user-defined shift type (soft delete).
     * Only user-defined shift types can be deactivated.
     *
     * @param id shift type ID to deactivate
     * @return CompletableFuture with success/failure result
     */
    CompletableFuture<OperationResult<Void>> deactivateShiftType(Long id);

    /**
     * Reactivates a previously deactivated shift type.
     *
     * @param id shift type ID to reactivate
     * @return CompletableFuture with success/failure result
     */
    CompletableFuture<OperationResult<Void>> reactivateShiftType(Long id);

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validates a shift type configuration.
     *
     * @param shiftType shift type to validate
     * @return CompletableFuture with validation result
     */
    CompletableFuture<OperationResult<ShiftTypeValidationResult>> validateShiftType(ShiftType shiftType);

    /**
     * Checks if a shift type name is available for use.
     *
     * @param name name to check
     * @return CompletableFuture with availability result
     */
    CompletableFuture<OperationResult<Boolean>> isNameAvailable(String name);

    // ==================== BUSINESS LOGIC OPERATIONS ====================

    /**
     * Calculates work hours for a shift type.
     *
     * @param shiftType shift type to calculate for
     * @return CompletableFuture with work hours in decimal format
     */
    CompletableFuture<OperationResult<Double>> calculateWorkHours(ShiftType shiftType);

    /**
     * Gets shift types suitable for schedule building with criteria.
     *
     * @param includeRestPeriods whether to include rest periods
     * @param minDurationHours minimum duration in hours (0 for no minimum)
     * @param maxDurationHours maximum duration in hours (0 for no maximum)
     * @return CompletableFuture with filtered shift types
     */
    CompletableFuture<OperationResult<List<ShiftType>>> getShiftTypesForScheduleBuilder(
            boolean includeRestPeriods, int minDurationHours, int maxDurationHours);

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Gets count of all active shift types.
     *
     * @return CompletableFuture with count
     */
    CompletableFuture<OperationResult<Integer>> getShiftTypesCount();

    /**
     * Gets count breakdown by shift type categories.
     *
     * @return CompletableFuture with count breakdown
     */
    CompletableFuture<OperationResult<Map<String, Integer>>> getShiftTypesCountByCategory();

    /**
     * Gets comprehensive shift type statistics.
     *
     * @return CompletableFuture with statistics
     */
    CompletableFuture<OperationResult<ShiftTypeStatistics>> getShiftTypeStatistics();

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Shuts down the service and releases resources.
     */
    void shutdown();

    // ==================== RESULT CLASSES ====================

    /**
     * Validation result for shift type operations.
     */
    class ShiftTypeValidationResult {
        private final boolean valid;
        private final List<String> errorMessages;
        private final List<String> warningMessages;

        public ShiftTypeValidationResult(boolean valid, List<String> errorMessages, List<String> warningMessages) {
            this.valid = valid;
            this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
            this.warningMessages = warningMessages != null ? warningMessages : new ArrayList<>();
        }

        public boolean isValid() { return valid; }
        public List<String> getErrorMessages() { return errorMessages; }
        public List<String> getWarningMessages() { return warningMessages; }
        public boolean hasWarnings() { return !warningMessages.isEmpty(); }

        public String getFormattedErrorMessage() {
            return errorMessages.isEmpty() ? "" : String.join(", ", errorMessages);
        }

        public String getFormattedWarningMessage() {
            return warningMessages.isEmpty() ? "" : String.join(", ", warningMessages);
        }
    }

    /**
     * Statistics for shift type usage and distribution.
     */
    class ShiftTypeStatistics {
        private final int totalActive;
        private final int predefined;
        private final int userDefined;
        private final int restPeriods;
        private final int workShifts;
        private final int deactivated;
        private final long collectionTime;

        public ShiftTypeStatistics(int totalActive, int predefined, int userDefined,
                                   int restPeriods, int workShifts, int deactivated) {
            this.totalActive = totalActive;
            this.predefined = predefined;
            this.userDefined = userDefined;
            this.restPeriods = restPeriods;
            this.workShifts = workShifts;
            this.deactivated = deactivated;
            this.collectionTime = System.currentTimeMillis();
        }

        public int getTotalActive() { return totalActive; }
        public int getPredefined() { return predefined; }
        public int getUserDefined() { return userDefined; }
        public int getRestPeriods() { return restPeriods; }
        public int getWorkShifts() { return workShifts; }
        public int getDeactivated() { return deactivated; }
        public long getCollectionTime() { return collectionTime; }

        public String getSummary() {
            return "ShiftType Statistics: " + totalActive + " active (" +
                    predefined + " predefined, " + userDefined + " custom), " +
                    deactivated + " deactivated";
        }

        public Map<String, Integer> toMap() {
            Map<String, Integer> map = new HashMap<>();
            map.put("totalActive", totalActive);
            map.put("predefined", predefined);
            map.put("userDefined", userDefined);
            map.put("restPeriods", restPeriods);
            map.put("workShifts", workShifts);
            map.put("deactivated", deactivated);
            return map;
        }
    }
}
