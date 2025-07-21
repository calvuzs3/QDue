package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.MacroDepartmentService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: MacroDepartmentServiceImpl - Dependency Injection Compliant
 * <p>
 * Centralized implementation for all MacroDepartment operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 * - Hierarchical department management
 * <p>
 * Fully compliant with MacroDepartmentService interface contract.
 */
public class MacroDepartmentServiceImpl implements MacroDepartmentService {

    private static final String TAG = "MacroDepartmentServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final MacroDepartmentDao mMacroDepartmentDao;
    private final SubDepartmentDao mSubDepartmentDao;
    private final CoreBackupManager mBackupManager;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR FOR DEPENDENCY INJECTION ====================

    /**
     * Constructor for dependency injection
     *
     * @param context Application context
     * @param database QDue database instance
     * @param backupManager Core backup manager instance
     */
    public MacroDepartmentServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mMacroDepartmentDao = database.macroDepartmentDao();
        this.mSubDepartmentDao = database.subDepartmentDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "MacroDepartmentServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public MacroDepartmentServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<MacroDepartment>> createMacroDepartment(MacroDepartment macroDepartment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate macro department input
                OperationResult<Void> validation = validateMacroDepartment(macroDepartment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for duplicates within establishment
                if (mMacroDepartmentDao.existsByNameAndEstablishment(
                        macroDepartment.getEstablishmentId(), macroDepartment.getName())) {
                    return OperationResult.failure("Macro department with name already exists in this establishment: " +
                            macroDepartment.getName(), OperationResult.OperationType.CREATE);
                }

                // Set creation timestamps
                macroDepartment.setCreatedAt(LocalDate.now());
                macroDepartment.setUpdatedAt(LocalDate.now());

                // Insert macro department and get generated ID
                long id = mMacroDepartmentDao.insertMacroDepartment(macroDepartment);
                macroDepartment.setId(id);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "create");

                Log.d(TAG, "Macro department created successfully: " + macroDepartment.getName());
                return OperationResult.success(macroDepartment, "Macro department created successfully",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<MacroDepartment>>> createMacroDepartments(List<MacroDepartment> macroDepartments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartments == null || macroDepartments.isEmpty()) {
                    return OperationResult.failure("No macro departments provided for creation",
                            OperationResult.OperationType.BULK_CREATE);
                }

                List<String> errors = new ArrayList<>();
                List<MacroDepartment> created = new ArrayList<>();

                for (int i = 0; i < macroDepartments.size(); i++) {
                    MacroDepartment dept = macroDepartments.get(i);

                    // Validate each department
                    OperationResult<Void> validation = validateMacroDepartment(dept);
                    if (validation.isFailure()) {
                        errors.add("Department " + (i + 1) + ": " + validation.getFirstError());
                        continue;
                    }

                    // Check for duplicates
                    if (mMacroDepartmentDao.existsByNameAndEstablishment(
                            dept.getEstablishmentId(), dept.getName())) {
                        errors.add("Department " + (i + 1) + ": Duplicate name in establishment");
                        continue;
                    }

                    try {
                        // Set timestamps and insert
                        dept.setCreatedAt(LocalDate.now());
                        dept.setUpdatedAt(LocalDate.now());
                        long id = mMacroDepartmentDao.insertMacroDepartment(dept);
                        dept.setId(id);
                        created.add(dept);
                    } catch (Exception e) {
                        errors.add("Department " + (i + 1) + ": " + e.getMessage());
                    }
                }

                // Trigger backup if any departments were created
                if (!created.isEmpty()) {
                    mBackupManager.performAutoBackup("macro_departments", "bulk_create");
                }

                // Return appropriate result
                if (!created.isEmpty() && errors.isEmpty()) {
                    Log.d(TAG, "Created " + created.size() + " macro departments successfully");
                    return OperationResult.success(created, "Created " + created.size() + " macro departments",
                            OperationResult.OperationType.BULK_CREATE);
                } else if (!created.isEmpty() && !errors.isEmpty()) {
                    Log.w(TAG, "Partially created " + created.size() + " departments with " + errors.size() + " errors");
                    return OperationResult.success(created,
                            "Partially created " + created.size() + " departments (" + errors.size() + " failed)",
                            OperationResult.OperationType.BULK_CREATE);
                } else {
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to create macro departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<MacroDepartment>> updateMacroDepartment(MacroDepartment macroDepartment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate macro department input
                OperationResult<Void> validation = validateMacroDepartment(macroDepartment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check if macro department exists
                MacroDepartment existing = mMacroDepartmentDao.getMacroDepartmentById(macroDepartment.getId());
                if (existing == null) {
                    return OperationResult.failure("Macro department not found: " + macroDepartment.getId(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check name uniqueness within establishment (excluding current department)
                if (!macroDepartment.getName().equals(existing.getName())) {
                    if (mMacroDepartmentDao.existsByNameAndEstablishment(
                            macroDepartment.getEstablishmentId(), macroDepartment.getName())) {
                        return OperationResult.failure("Macro department name already exists in this establishment: " +
                                macroDepartment.getName(), OperationResult.OperationType.UPDATE);
                    }
                }

                // Set update timestamp
                macroDepartment.setUpdatedAt(LocalDate.now());

                // Update macro department
                mMacroDepartmentDao.updateMacroDepartment(macroDepartment);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "update");

                Log.d(TAG, "Macro department updated successfully: " + macroDepartment.getName());
                return OperationResult.success(macroDepartment, "Macro department updated successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<String>> deleteMacroDepartment(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get department before deletion for logging
                MacroDepartment dept = mMacroDepartmentDao.getMacroDepartmentById(macroDepartmentId);
                if (dept == null) {
                    return OperationResult.failure("Macro department not found: " + macroDepartmentId,
                            OperationResult.OperationType.DELETE);
                }

                String deptName = dept.getName();

                // Delete macro department (sub-departments will be deleted by CASCADE)
                mMacroDepartmentDao.deleteMacroDepartmentById(macroDepartmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "delete");

                Log.d(TAG, "Macro department deleted successfully: " + deptName);
                return OperationResult.success(deptName, "Macro department deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllMacroDepartments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get count before deletion
                List<MacroDepartment> departments = mMacroDepartmentDao.getAllMacroDepartments();
                int count = departments.size();

                if (count == 0) {
                    return OperationResult.success(0, "No macro departments to delete",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete all macro departments
                mMacroDepartmentDao.deleteAllMacroDepartments();

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "bulk_delete");

                Log.d(TAG, "Deleted all " + count + " macro departments");
                return OperationResult.success(count, "Deleted all " + count + " macro departments",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all macro departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentsByEstablishment(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get departments before deletion for count
                List<MacroDepartment> departments = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                int count = departments.size();

                if (count == 0) {
                    return OperationResult.success(0, "No macro departments found for establishment: " + establishmentId,
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete macro departments for establishment
                mMacroDepartmentDao.deleteMacroDepartmentsByEstablishment(establishmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "bulk_delete");

                Log.d(TAG, "Deleted " + count + " macro departments for establishment: " + establishmentId);
                return OperationResult.success(count, "Deleted " + count + " macro departments",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete macro departments by establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    // ==================== QUERY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<MacroDepartment>> getMacroDepartmentById(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MacroDepartment department = mMacroDepartmentDao.getMacroDepartmentById(macroDepartmentId);
                if (department != null) {
                    return OperationResult.success(department, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Macro department not found: " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro department by ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<MacroDepartment>>> getAllMacroDepartments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<MacroDepartment> departments = mMacroDepartmentDao.getAllMacroDepartments();
                return OperationResult.success(departments,
                        "Found " + departments.size() + " macro departments",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all macro departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<MacroDepartment>>> getMacroDepartmentsByEstablishment(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                List<MacroDepartment> departments = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                return OperationResult.success(departments,
                        "Found " + departments.size() + " macro departments for establishment",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro departments by establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<MacroDepartment>> getMacroDepartmentByNameAndEstablishment(long establishmentId, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.READ);
                }
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Department name is required",
                            OperationResult.OperationType.READ);
                }

                MacroDepartment department = mMacroDepartmentDao.getMacroDepartmentByNameAndEstablishment(establishmentId, name);
                if (department != null) {
                    return OperationResult.success(department, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Macro department not found: " + name + " in establishment " + establishmentId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro department by name and establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateMacroDepartment(MacroDepartment macroDepartment) {
        List<String> errors = new ArrayList<>();

        // Basic null check
        if (macroDepartment == null) {
            errors.add("Macro department cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // Name validation
        if (macroDepartment.getName() == null || macroDepartment.getName().trim().isEmpty()) {
            errors.add("Macro department name is required");
        } else if (macroDepartment.getName().length() > 100) {
            errors.add("Macro department name cannot exceed 100 characters");
        }

        // Establishment ID validation
        if (macroDepartment.getEstablishmentId() <= 0) {
            errors.add("Valid establishment ID is required");
        }

        // Code validation (if provided)
        if (macroDepartment.getCode() != null && macroDepartment.getCode().length() > 20) {
            errors.add("Department code cannot exceed 20 characters");
        }

        // Description validation (if provided)
        if (macroDepartment.getDescription() != null && macroDepartment.getDescription().length() > 500) {
            errors.add("Department description cannot exceed 500 characters");
        }

        // Manager name validation (if provided)
        if (macroDepartment.getManagerName() != null && macroDepartment.getManagerName().length() > 100) {
            errors.add("Manager name cannot exceed 100 characters");
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> macroDepartmentExists(long establishmentId, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.VALIDATION);
                }
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Department name is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mMacroDepartmentDao.existsByNameAndEstablishment(establishmentId, name);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check macro department existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getMacroDepartmentsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mMacroDepartmentDao.getAllMacroDepartments().size();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro departments count", e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> getMacroDepartmentsCountByEstablishment(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.COUNT);
                }

                int count = mMacroDepartmentDao.getMacroDepartmentCountByEstablishment(establishmentId);
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro departments count by establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    // ==================== HIERARCHY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<SubDepartment>>> getSubDepartments(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }

                // Verify macro department exists
                MacroDepartment macroDept = mMacroDepartmentDao.getMacroDepartmentById(macroDepartmentId);
                if (macroDept == null) {
                    return OperationResult.failure("Macro department not found: " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }

                List<SubDepartment> subDepartments = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId);
                return OperationResult.success(subDepartments,
                        "Found " + subDepartments.size() + " sub-departments",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentWithDependencies(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify macro department exists
                MacroDepartment macroDept = mMacroDepartmentDao.getMacroDepartmentById(macroDepartmentId);
                if (macroDept == null) {
                    return OperationResult.failure("Macro department not found: " + macroDepartmentId,
                            OperationResult.OperationType.DELETE);
                }

                // Get count of sub-departments for reporting
                List<SubDepartment> subDepartments = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId);
                int subDepartmentCount = subDepartments.size();

                // Delete all sub-departments first (explicit deletion for better logging)
                if (subDepartmentCount > 0) {
                    mSubDepartmentDao.deleteSubDepartmentsByMacroDepartment(macroDepartmentId);
                    Log.d(TAG, "Deleted " + subDepartmentCount + " sub-departments for macro department: " + macroDepartmentId);
                }

                // Delete the macro department
                mMacroDepartmentDao.deleteMacroDepartmentById(macroDepartmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("macro_departments", "delete");

                int totalDeleted = 1 + subDepartmentCount; // 1 macro + N sub departments
                Log.d(TAG, "Deleted macro department with dependencies: " + macroDepartmentId +
                        " (total entities: " + totalDeleted + ")");

                return OperationResult.success(totalDeleted,
                        "Macro department and " + subDepartmentCount + " sub-departments deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete macro department with dependencies: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when service is no longer needed
     * Should be called from DI container or application lifecycle
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "MacroDepartmentServiceImpl executor service shutdown");
        }
    }
}