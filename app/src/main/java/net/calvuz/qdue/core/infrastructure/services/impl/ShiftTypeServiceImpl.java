package net.calvuz.qdue.core.infrastructure.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.domain.quattrodue.models.ShiftType;
import net.calvuz.qdue.core.infrastructure.db.QDueDatabase;
import net.calvuz.qdue.core.infrastructure.db.dao.ShiftTypeDao;
import net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity;
import net.calvuz.qdue.core.infrastructure.services.ShiftTypeService;
import net.calvuz.qdue.core.infrastructure.services.models.OperationResult;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>Implementation of ShiftTypeService following the established patterns of other
 * service implementations in the application. Uses async operations, OperationResult
 * pattern, and proper error handling.</p>
 *
 * <p>Architectural consistency:</p>
 * <ul>
 *   <li>Constructor injection with Context and QDueDatabase</li>
 *   <li>All operations return CompletableFuture&lt;OperationResult&lt;T&gt;&gt;</li>
 *   <li>Consistent logging and error handling</li>
 *   <li>Proper lifecycle management with shutdown support</li>
 *   <li>Health monitoring capabilities</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 2.0
 * @since Database Version 5
 */
public class ShiftTypeServiceImpl implements ShiftTypeService {

    private static final String TAG = "ShiftTypeServiceImpl";

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final ShiftTypeDao mShiftTypeDao;
    private volatile boolean mIsShutdown = false;

    // ==================== ✅ CONSTRUCTOR FOLLOWING EXISTING PATTERN ====================

    /**
     * ✅ Constructor following existing service pattern
     *
     * @param context Application context
     * @param database Database instance
     */
    public ShiftTypeServiceImpl(Context context, QDueDatabase database) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mShiftTypeDao = database.shiftTypeDao();

        Log.d(TAG, "ShiftTypeServiceImpl initialized");
    }

    // ==================== RETRIEVAL OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getAllActiveShiftTypes() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving all active shift types");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getAllActiveShiftTypes();
                List<ShiftType> shiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shiftTypes.size() + " active shift types");
                return OperationResult.success(shiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve active shift types", e);
                return OperationResult.failure("Failed to retrieve shift types: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<ShiftType>> getShiftTypeById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            if (id == null) {
                return OperationResult.failure("Shift type ID cannot be null", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving shift type by ID: " + id);

                ShiftTypeEntity entity = mShiftTypeDao.getShiftTypeById(id);
                if (entity == null) {
                    Log.w(TAG, "Shift type not found with ID: " + id);
                    return OperationResult.failure("Shift type not found", OperationResult.OperationType.READ);
                }

                ShiftType shiftType = mapToModel(entity);
                Log.d(TAG, "Retrieved shift type: " + shiftType.getName());
                return OperationResult.success(shiftType, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve shift type by ID: " + id, e);
                return OperationResult.failure("Failed to retrieve shift type: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<ShiftType>> getShiftTypeByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            if (name == null || name.trim().isEmpty()) {
                return OperationResult.failure("Shift type name cannot be null or empty", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving shift type by name: " + name);

                ShiftTypeEntity entity = mShiftTypeDao.getShiftTypeByName(name.trim());
                if (entity == null) {
                    Log.w(TAG, "Shift type not found with name: " + name);
                    return OperationResult.failure("Shift type not found", OperationResult.OperationType.READ);
                }

                ShiftType shiftType = mapToModel(entity);
                Log.d(TAG, "Retrieved shift type: " + shiftType.getName());
                return OperationResult.success(shiftType, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve shift type by name: " + name, e);
                return OperationResult.failure("Failed to retrieve shift type: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getPredefinedShiftTypes() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving predefined shift types");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getPredefinedShiftTypes();
                List<ShiftType> shiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shiftTypes.size() + " predefined shift types");
                return OperationResult.success(shiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve predefined shift types", e);
                return OperationResult.failure("Failed to retrieve predefined shift types: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getUserDefinedShiftTypes() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving user-defined shift types");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getUserDefinedShiftTypes();
                List<ShiftType> shiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shiftTypes.size() + " user-defined shift types");
                return OperationResult.success(shiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve user-defined shift types", e);
                return OperationResult.failure("Failed to retrieve user-defined shift types: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getRestPeriods() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving rest period shift types");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getRestPeriodShiftTypes();
                List<ShiftType> shiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shiftTypes.size() + " rest period shift types");
                return OperationResult.success(shiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve rest period shift types", e);
                return OperationResult.failure("Failed to retrieve rest periods: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getWorkShifts() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Retrieving work shift types");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getWorkShiftTypes();
                List<ShiftType> shiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + shiftTypes.size() + " work shift types");
                return OperationResult.success(shiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve work shift types", e);
                return OperationResult.failure("Failed to retrieve work shifts: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== CREATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<ShiftType>> createWorkShift(String name, String description,
                                                                         LocalTime startTime, LocalTime endTime,
                                                                         String colorHex) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.CREATE);
            }

            try {
                Log.d(TAG, "Creating work shift: " + name);

                // Validate input
                OperationResult<Void> validation = validateWorkShiftInput(name, startTime, endTime, colorHex);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getFormattedErrorMessage(), OperationResult.OperationType.CREATE);
                }

                // Check name uniqueness
                if (mShiftTypeDao.existsByName(name.trim())) {
                    Log.w(TAG, "Shift type name already exists: " + name);
                    return OperationResult.failure("Shift type name '" + name + "' already exists",
                            OperationResult.OperationType.CREATE);
                }

                // Create shift type
                ShiftType shiftType = new ShiftType(name.trim(), description, startTime, endTime, colorHex);
                shiftType.setUserDefined(true);

                // Validate business rules
                if (!shiftType.isValid()) {
                    Log.w(TAG, "Invalid shift type configuration: " + shiftType);
                    return OperationResult.failure("Invalid shift type configuration", OperationResult.OperationType.CREATE);
                }

                // Save to database
                ShiftTypeEntity entity = mapToEntity(shiftType);
                long id = mShiftTypeDao.insertShiftType(entity);
                shiftType.setId(id);

                Log.d(TAG, "Created work shift successfully: " + name + " (ID: " + id + ")");
                return OperationResult.success(shiftType, OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create work shift: " + name, e);
                return OperationResult.failure("Failed to create work shift: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<ShiftType>> createRestPeriod(String name, String description,
                                                                          String colorHex) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.CREATE);
            }

            try {
                Log.d(TAG, "Creating rest period: " + name);

                // Validate input
                OperationResult<Void> validation = validateRestPeriodInput(name, colorHex);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getFormattedErrorMessage(), OperationResult.OperationType.CREATE);
                }

                // Check name uniqueness
                if (mShiftTypeDao.existsByName(name.trim())) {
                    Log.w(TAG, "Shift type name already exists: " + name);
                    return OperationResult.failure("Shift type name '" + name + "' already exists",
                            OperationResult.OperationType.CREATE);
                }

                // Create rest period
                ShiftType shiftType = new ShiftType(name.trim(), description, colorHex, true);
                shiftType.setUserDefined(true);

                // Validate business rules
                if (!shiftType.isValid()) {
                    Log.w(TAG, "Invalid rest period configuration: " + shiftType);
                    return OperationResult.failure("Invalid rest period configuration", OperationResult.OperationType.CREATE);
                }

                // Save to database
                ShiftTypeEntity entity = mapToEntity(shiftType);
                long id = mShiftTypeDao.insertShiftType(entity);
                shiftType.setId(id);

                Log.d(TAG, "Created rest period successfully: " + name + " (ID: " + id + ")");
                return OperationResult.success(shiftType, OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create rest period: " + name, e);
                return OperationResult.failure("Failed to create rest period: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<ShiftType>> createWorkShiftWithBreak(String name, String description,
                                                                                  LocalTime startTime, LocalTime endTime,
                                                                                  String colorHex, LocalTime breakStartTime,
                                                                                  LocalTime breakEndTime) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.CREATE);
            }

            try {
                Log.d(TAG, "Creating work shift with break: " + name);

                // Validate input
                OperationResult<Void> validation = validateWorkShiftWithBreakInput(name, startTime, endTime,
                        colorHex, breakStartTime, breakEndTime);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getFormattedErrorMessage(), OperationResult.OperationType.CREATE);
                }

                // Check name uniqueness
                if (mShiftTypeDao.existsByName(name.trim())) {
                    Log.w(TAG, "Shift type name already exists: " + name);
                    return OperationResult.failure("Shift type name '" + name + "' already exists",
                            OperationResult.OperationType.CREATE);
                }

                // Create shift type with break
                ShiftType shiftType = new ShiftType(name.trim(), description, startTime, endTime, colorHex,
                        breakStartTime, breakEndTime);
                shiftType.setUserDefined(true);

                // Validate business rules
                if (!shiftType.isValid()) {
                    Log.w(TAG, "Invalid shift type with break configuration: " + shiftType);
                    return OperationResult.failure("Invalid shift type configuration", OperationResult.OperationType.CREATE);
                }

                // Save to database
                ShiftTypeEntity entity = mapToEntity(shiftType);
                long id = mShiftTypeDao.insertShiftType(entity);
                shiftType.setId(id);

                Log.d(TAG, "Created work shift with break successfully: " + name + " (ID: " + id + ")");
                return OperationResult.success(shiftType, OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create work shift with break: " + name, e);
                return OperationResult.failure("Failed to create work shift with break: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        });
    }

    // ==================== MODIFICATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<ShiftType>> updateShiftType(ShiftType shiftType) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.UPDATE);
            }

            if (shiftType == null || shiftType.getId() == null) {
                return OperationResult.failure("Shift type and ID cannot be null for update",
                        OperationResult.OperationType.UPDATE);
            }

            try {
                Log.d(TAG, "Updating shift type: " + shiftType.getName() + " (ID: " + shiftType.getId() + ")");

                // Check if shift type exists
                ShiftTypeEntity existingEntity = mShiftTypeDao.getShiftTypeById(shiftType.getId());
                if (existingEntity == null) {
                    Log.w(TAG, "Shift type not found for update: " + shiftType.getId());
                    return OperationResult.failure("Shift type not found", OperationResult.OperationType.UPDATE);
                }

                // Only user-defined shift types can be modified
                if (!existingEntity.isUserDefined()) {
                    Log.w(TAG, "Attempt to modify predefined shift type: " + shiftType.getId());
                    return OperationResult.failure("Cannot modify predefined shift types",
                            OperationResult.OperationType.UPDATE);
                }

                // Validate business rules
                if (!shiftType.isValid()) {
                    Log.w(TAG, "Invalid shift type configuration for update: " + shiftType);
                    return OperationResult.failure("Invalid shift type configuration", OperationResult.OperationType.UPDATE);
                }

                // Check name uniqueness (excluding current ID)
                if (mShiftTypeDao.existsByNameExcludingId(shiftType.getName(), shiftType.getId())) {
                    Log.w(TAG, "Shift type name already exists: " + shiftType.getName());
                    return OperationResult.failure("Shift type name '" + shiftType.getName() + "' already exists",
                            OperationResult.OperationType.UPDATE);
                }

                // Update shift type
                shiftType.markAsUpdated();
                ShiftTypeEntity entity = mapToEntity(shiftType);
                mShiftTypeDao.updateShiftType(entity);

                Log.d(TAG, "Updated shift type successfully: " + shiftType.getName());
                return OperationResult.success(shiftType, OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update shift type: " + shiftType.getId(), e);
                return OperationResult.failure("Failed to update shift type: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<Void>> deactivateShiftType(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.UPDATE);
            }

            if (id == null) {
                return OperationResult.failure("Shift type ID cannot be null", OperationResult.OperationType.UPDATE);
            }

            try {
                Log.d(TAG, "Deactivating shift type: " + id);

                // Check if shift type exists
                ShiftTypeEntity entity = mShiftTypeDao.getShiftTypeById(id);
                if (entity == null) {
                    Log.w(TAG, "Shift type not found for deactivation: " + id);
                    return OperationResult.failure("Shift type not found", OperationResult.OperationType.UPDATE);
                }

                // Only user-defined shift types can be deactivated
                if (!entity.isUserDefined()) {
                    Log.w(TAG, "Attempt to deactivate predefined shift type: " + id);
                    return OperationResult.failure("Cannot deactivate predefined shift types",
                            OperationResult.OperationType.UPDATE);
                }

                // Deactivate shift type
                mShiftTypeDao.deactivateShiftType(id);

                Log.d(TAG, "Deactivated shift type successfully: " + id);
                return OperationResult.success("Deactivated Successfully", OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to deactivate shift type: " + id, e);
                return OperationResult.failure("Failed to deactivate shift type: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<Void>> reactivateShiftType(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.UPDATE);
            }

            if (id == null) {
                return OperationResult.failure("Shift type ID cannot be null", OperationResult.OperationType.UPDATE);
            }

            try {
                Log.d(TAG, "Reactivating shift type: " + id);

                // Check if shift type exists (including inactive)
                ShiftTypeEntity entity = mShiftTypeDao.getShiftTypeById(id);
                if (entity == null) {
                    Log.w(TAG, "Shift type not found for reactivation: " + id);
                    return OperationResult.failure("Shift type not found", OperationResult.OperationType.UPDATE);
                }

                // Reactivate shift type
                mShiftTypeDao.reactivateShiftType(id);

                Log.d(TAG, "Reactivated shift type successfully: " + id);
                return OperationResult.success("Reactivated Successfully", OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to reactivate shift type: " + id, e);
                return OperationResult.failure("Failed to reactivate shift type: " + e.getMessage(),
                        OperationResult.OperationType.UPDATE);
            }
        });
    }

    // ==================== VALIDATION OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<ShiftTypeValidationResult>> validateShiftType(ShiftType shiftType) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.VALIDATION);
            }

            try {
                Log.d(TAG, "Validating shift type: " + (shiftType != null ? shiftType.getName() : "null"));

                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                if (shiftType == null) {
                    errors.add("Shift type cannot be null");
                } else {
                    validateShiftTypeBusinessRules(shiftType, errors, warnings);
                }

                boolean isValid = errors.isEmpty();
                ShiftTypeValidationResult result = new ShiftTypeValidationResult(isValid, errors, warnings);

                Log.d(TAG, "Shift type validation completed - Valid: " + isValid +
                        ", Errors: " + errors.size() + ", Warnings: " + warnings.size());

                return OperationResult.success(result, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to validate shift type", e);
                return OperationResult.failure("Validation failed: " + e.getMessage(),
                        OperationResult.OperationType.VALIDATION);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> isNameAvailable(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.VALIDATION);
            }

            if (name == null || name.trim().isEmpty()) {
                return OperationResult.success(false, OperationResult.OperationType.VALIDATION);
            }

            try {
                Log.d(TAG, "Checking name availability: " + name);

                boolean exists = mShiftTypeDao.existsByName(name.trim());
                boolean available = !exists;

                Log.d(TAG, "Name availability check for '" + name + "': " + available);
                return OperationResult.success(available, OperationResult.OperationType.VALIDATION);

            } catch (Exception e) {
                Log.e(TAG, "Failed to check name availability: " + name, e);
                return OperationResult.failure("Failed to check name availability: " + e.getMessage(),
                        OperationResult.OperationType.VALIDATION);
            }
        });
    }

    // ==================== BUSINESS LOGIC OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<Double>> calculateWorkHours(ShiftType shiftType) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            if (shiftType == null) {
                return OperationResult.failure("Shift type cannot be null", OperationResult.OperationType.READ);
            }

            try {
                double workHours = shiftType.getWorkHours();
                Log.d(TAG, "Calculated work hours for '" + shiftType.getName() + "': " + workHours);
                return OperationResult.success(workHours, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to calculate work hours for shift type: " + shiftType.getName(), e);
                return OperationResult.failure("Failed to calculate work hours: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<List<ShiftType>>> getShiftTypesForScheduleBuilder(
            boolean includeRestPeriods, int minDurationHours, int maxDurationHours) {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Getting shift types for schedule builder - Rest: " + includeRestPeriods +
                        ", Min: " + minDurationHours + "h, Max: " + maxDurationHours + "h");

                List<ShiftTypeEntity> entities = mShiftTypeDao.getAllActiveShiftTypes();
                List<ShiftType> filteredShiftTypes = entities.stream()
                        .map(this::mapToModel)
                        .filter(shiftType -> {
                            // Filter by rest period inclusion
                            if (!includeRestPeriods && shiftType.isRestPeriod()) {
                                return false;
                            }

                            // Filter by duration for work shifts
                            if (!shiftType.isRestPeriod()) {
                                double hours = shiftType.getWorkHours();

                                if (minDurationHours > 0 && hours < minDurationHours) {
                                    return false;
                                }

                                return maxDurationHours <= 0 || !(hours > maxDurationHours);
                            }

                            return true;
                        })
                        .collect(Collectors.toList());

                Log.d(TAG, "Retrieved " + filteredShiftTypes.size() + " shift types for schedule builder");
                return OperationResult.success(filteredShiftTypes, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get shift types for schedule builder", e);
                return OperationResult.failure("Failed to get shift types for schedule builder: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== STATISTICS AND REPORTING ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getShiftTypesCount() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                int count = mShiftTypeDao.getActiveShiftTypeCount();
                Log.d(TAG, "Active shift types count: " + count);
                return OperationResult.success(count, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get shift types count", e);
                return OperationResult.failure("Failed to get shift types count: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<Map<String, Integer>>> getShiftTypesCountByCategory() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Map<String, Integer> counts = new HashMap<>();
                counts.put("totalActive", mShiftTypeDao.getActiveShiftTypeCount());
                counts.put("predefined", mShiftTypeDao.getPredefinedShiftTypeCount());
                counts.put("userDefined", mShiftTypeDao.getUserDefinedShiftTypeCount());
                counts.put("restPeriods", mShiftTypeDao.getRestPeriodShiftTypes().size());
                counts.put("workShifts", mShiftTypeDao.getWorkShiftTypes().size());

                Log.d(TAG, "Shift types count by category: " + counts);
                return OperationResult.success(counts, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get shift types count by category", e);
                return OperationResult.failure("Failed to get count by category: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    @Override
    public CompletableFuture<OperationResult<ShiftTypeStatistics>> getShiftTypeStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            if (mIsShutdown) {
                return OperationResult.failure("Service has been shut down", OperationResult.OperationType.READ);
            }

            try {
                Log.d(TAG, "Collecting shift type statistics");

                int totalActive = mShiftTypeDao.getActiveShiftTypeCount();
                int predefined = mShiftTypeDao.getPredefinedShiftTypeCount();
                int userDefined = mShiftTypeDao.getUserDefinedShiftTypeCount();
                int restPeriods = mShiftTypeDao.getRestPeriodShiftTypes().size();
                int workShifts = mShiftTypeDao.getWorkShiftTypes().size();

                // Calculate deactivated count
                List<ShiftTypeEntity> allEntities = mShiftTypeDao.getAllShiftTypes();
                int deactivated = allEntities.size() - totalActive;

                ShiftTypeStatistics stats = new ShiftTypeStatistics(totalActive, predefined, userDefined,
                        restPeriods, workShifts, deactivated);

                Log.d(TAG, "Shift type statistics collected: " + stats.getSummary());
                return OperationResult.success(stats, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to collect shift type statistics", e);
                return OperationResult.failure("Failed to collect statistics: " + e.getMessage(),
                        OperationResult.OperationType.READ);
            }
        });
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down ShiftTypeService...");
        mIsShutdown = true;
        Log.d(TAG, "ShiftTypeService shut down successfully");
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Maps a database entity to a domain model.
     */
    private ShiftType mapToModel(ShiftTypeEntity entity) {
        if (entity == null) return null;

        ShiftType shiftType = new ShiftType();
        shiftType.setId(entity.getId());
        shiftType.setName(entity.getName());
        shiftType.setDescription(entity.getDescription());
        shiftType.setStartTime(entity.getStartTime());
        shiftType.setEndTime(entity.getEndTime());
        shiftType.setColorHex(entity.getColorHex());
        shiftType.setRestPeriod(entity.isRestPeriod());
        shiftType.setHasBreakTime(entity.isHasBreakTime());
        shiftType.setBreakStartTime(entity.getBreakStartTime());
        shiftType.setBreakEndTime(entity.getBreakEndTime());
        shiftType.setUserDefined(entity.isUserDefined());
        shiftType.setActive(entity.isActive());

        // Convert timestamps
        if (entity.getCreatedAt() != null) {
            shiftType.setCreatedAt(entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (entity.getUpdatedAt() != null) {
            shiftType.setUpdatedAt(entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        return shiftType;
    }

    /**
     * Maps a domain model to a database entity.
     */
    private ShiftTypeEntity mapToEntity(ShiftType shiftType) {
        if (shiftType == null) return null;

        ShiftTypeEntity entity = new ShiftTypeEntity();
        if (shiftType.getId() != null) {
            entity.setId(shiftType.getId());
        }
        entity.setName(shiftType.getName());
        entity.setDescription(shiftType.getDescription());
        entity.setStartTime(shiftType.getStartTime());
        entity.setEndTime(shiftType.getEndTime());
        entity.setColorHex(shiftType.getColorHex());
        entity.setRestPeriod(shiftType.isRestPeriod());
        entity.setHasBreakTime(shiftType.isHasBreakTime());
        entity.setBreakStartTime(shiftType.getBreakStartTime());
        entity.setBreakEndTime(shiftType.getBreakEndTime());
        entity.setUserDefined(shiftType.isUserDefined());
        entity.setActive(shiftType.isActive());

        return entity;
    }

    /**
     * Validates work shift input parameters.
     */
    private OperationResult<Void> validateWorkShiftInput(String name, LocalTime startTime,
                                                         LocalTime endTime, String colorHex) {
        if (name == null || name.trim().isEmpty()) {
            return OperationResult.failure("Shift type name is required", OperationResult.OperationType.VALIDATION);
        }
        if (startTime == null) {
            return OperationResult.failure("Start time is required for work shifts", OperationResult.OperationType.VALIDATION);
        }
        if (endTime == null) {
            return OperationResult.failure("End time is required for work shifts", OperationResult.OperationType.VALIDATION);
        }
        if (!isValidHexColor( colorHex )) {
            return OperationResult.failure("Valid hex color code is required", OperationResult.OperationType.VALIDATION);
        }
        return OperationResult.success("Valid Input", OperationResult.OperationType.VALIDATION);
    }

    /**
     * Validates rest period input parameters.
     */
    private OperationResult<Void> validateRestPeriodInput(String name, String colorHex) {
        if (name == null || name.trim().isEmpty()) {
            return OperationResult.failure("Rest period name is required", OperationResult.OperationType.VALIDATION);
        }
        if (!isValidHexColor( colorHex )) {
            return OperationResult.failure("Valid hex color code is required", OperationResult.OperationType.VALIDATION);
        }
        return OperationResult.success("Valid Input", OperationResult.OperationType.VALIDATION);
    }

    /**
     * Validates work shift with break input parameters.
     */
    private OperationResult<Void> validateWorkShiftWithBreakInput(String name, LocalTime startTime,
                                                                  LocalTime endTime, String colorHex,
                                                                  LocalTime breakStartTime, LocalTime breakEndTime) {
        OperationResult<Void> baseValidation = validateWorkShiftInput(name, startTime, endTime, colorHex);
        if (!baseValidation.isSuccess()) {
            return baseValidation;
        }

        if (breakStartTime == null) {
            return OperationResult.failure("Break start time is required when break is specified",
                    OperationResult.OperationType.VALIDATION);
        }
        if (breakEndTime == null) {
            return OperationResult.failure("Break end time is required when break is specified",
                    OperationResult.OperationType.VALIDATION);
        }
        if (!breakStartTime.isBefore(breakEndTime)) {
            return OperationResult.failure("Break start time must be before break end time",
                    OperationResult.OperationType.VALIDATION);
        }

        return OperationResult.success("Valid Input", OperationResult.OperationType.VALIDATION);
    }

    /**
     * Validates shift type business rules.
     */
    private void validateShiftTypeBusinessRules(ShiftType shiftType, List<String> errors, List<String> warnings) {
        // Name validation
        if (shiftType.getName() == null || shiftType.getName().trim().isEmpty()) {
            errors.add("Shift type name is required");
        } else if (shiftType.getName().length() > 50) {
            errors.add("Shift type name cannot exceed 50 characters");
        }

        // Color validation
        if (shiftType.getColorHex() == null || !isValidHexColor(shiftType.getColorHex())) {
            errors.add("Valid hex color code is required (e.g., #FF5733)");
        }

        // Work shift validation
        if (!shiftType.isRestPeriod()) {
            if (shiftType.getStartTime() == null) {
                errors.add("Start time is required for work shifts");
            }
            if (shiftType.getEndTime() == null) {
                errors.add("End time is required for work shifts");
            }

            // Duration validation
            if (shiftType.getStartTime() != null && shiftType.getEndTime() != null) {
                int durationMinutes = shiftType.getDurationMinutes();
                if (durationMinutes < 30) {
                    errors.add("Shift duration must be at least 30 minutes");
                } else if (durationMinutes < 60) {
                    warnings.add("Shift duration is less than 1 hour");
                }
                if (durationMinutes > 24 * 60) {
                    errors.add("Shift duration cannot exceed 24 hours");
                } else if (durationMinutes > 12 * 60) {
                    warnings.add("Shift duration exceeds 12 hours");
                }
            }

            // Break time validation
            if (shiftType.isHasBreakTime()) {
                if (shiftType.getBreakStartTime() == null) {
                    errors.add("Break start time is required when break is enabled");
                }
                if (shiftType.getBreakEndTime() == null) {
                    errors.add("Break end time is required when break is enabled");
                }

                if (shiftType.getBreakStartTime() != null && shiftType.getBreakEndTime() != null &&
                        !shiftType.getBreakStartTime().isBefore(shiftType.getBreakEndTime())) {
                    errors.add("Break start time must be before break end time");
                }
            }
        }
    }

    /**
     * Validates hex color format.
     */
    private boolean isValidHexColor(String color) {
        if (color == null) return false;
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }
}