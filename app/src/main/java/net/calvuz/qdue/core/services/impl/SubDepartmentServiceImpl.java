package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.SubDepartmentService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: SubDepartmentServiceImpl - Dependency Injection Compliant
 *
 * Centralized implementation for all SubDepartment operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 * - Hierarchical department management within macro departments
 *
 * Fully compliant with SubDepartmentService interface contract.
 */
public class SubDepartmentServiceImpl implements SubDepartmentService {

    private static final String TAG = "SubDepartmentServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
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
    public SubDepartmentServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mSubDepartmentDao = database.subDepartmentDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "SubDepartmentServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public SubDepartmentServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<SubDepartment>> createSubDepartment(SubDepartment subDepartment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate sub department input
                OperationResult<Void> validation = validateSubDepartment(subDepartment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for duplicates within macro department
                if (mSubDepartmentDao.existsByNameAndMacroDepartment(
                        subDepartment.getMacroDepartmentId(), subDepartment.getName())) {
                    return OperationResult.failure("Sub department with name already exists in this macro department: " +
                            subDepartment.getName(), OperationResult.OperationType.CREATE);
                }

                // Set creation timestamps
                subDepartment.setCreatedAt(LocalDate.now());
                subDepartment.setUpdatedAt(LocalDate.now());

                // Insert sub department and get generated ID
                long id = mSubDepartmentDao.insertSubDepartment(subDepartment);
                subDepartment.setId(id);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("sub_departments", "create");

                Log.d(TAG, "Sub department created successfully: " + subDepartment.getName());
                return OperationResult.success(subDepartment, "Sub department created successfully",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create sub department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<SubDepartment>>> createSubDepartments(List<SubDepartment> subDepartments) {
        // Make parameter effectively final for lambda
        final List<SubDepartment> finalSubDepartments = subDepartments != null ? subDepartments : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalSubDepartments.isEmpty()) {
                    return OperationResult.failure("No sub departments provided for creation",
                            OperationResult.OperationType.BULK_CREATE);
                }

                List<String> errors = new ArrayList<>();
                List<SubDepartment> created = new ArrayList<>();

                for (int i = 0; i < finalSubDepartments.size(); i++) {
                    SubDepartment subDept = finalSubDepartments.get(i);

                    // Validate each sub department
                    OperationResult<Void> validation = validateSubDepartment(subDept);
                    if (validation.isFailure()) {
                        errors.add("Sub department " + (i + 1) + ": " + validation.getFirstError());
                        continue;
                    }

                    // Check for duplicates
                    if (mSubDepartmentDao.existsByNameAndMacroDepartment(
                            subDept.getMacroDepartmentId(), subDept.getName())) {
                        errors.add("Sub department " + (i + 1) + ": Duplicate name in macro department");
                        continue;
                    }

                    try {
                        // Set timestamps and insert
                        subDept.setCreatedAt(LocalDate.now());
                        subDept.setUpdatedAt(LocalDate.now());
                        long id = mSubDepartmentDao.insertSubDepartment(subDept);
                        subDept.setId(id);
                        created.add(subDept);
                    } catch (Exception e) {
                        errors.add("Sub department " + (i + 1) + ": " + e.getMessage());
                    }
                }

                // Trigger backup if any sub departments were created
                if (!created.isEmpty()) {
                    mBackupManager.performAutoBackup("sub_departments", "bulk_create");
                }

                // Return appropriate result
                if (!created.isEmpty() && errors.isEmpty()) {
                    Log.d(TAG, "Created " + created.size() + " sub departments successfully");
                    return OperationResult.success(created, "Created " + created.size() + " sub departments",
                            OperationResult.OperationType.BULK_CREATE);
                } else if (!created.isEmpty() && !errors.isEmpty()) {
                    Log.w(TAG, "Partially created " + created.size() + " sub departments with " + errors.size() + " errors");
                    return OperationResult.success(created,
                            "Partially created " + created.size() + " sub departments (" + errors.size() + " failed)",
                            OperationResult.OperationType.BULK_CREATE);
                } else {
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to create sub departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<SubDepartment>> updateSubDepartment(SubDepartment subDepartment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate sub department input
                OperationResult<Void> validation = validateSubDepartment(subDepartment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check if sub department exists
                SubDepartment existing = mSubDepartmentDao.getSubDepartmentById(subDepartment.getId());
                if (existing == null) {
                    return OperationResult.failure("Sub department not found: " + subDepartment.getId(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check name uniqueness within macro department (excluding current sub department)
                if (!subDepartment.getName().equals(existing.getName())) {
                    if (mSubDepartmentDao.existsByNameAndMacroDepartment(
                            subDepartment.getMacroDepartmentId(), subDepartment.getName())) {
                        return OperationResult.failure("Sub department name already exists in this macro department: " +
                                subDepartment.getName(), OperationResult.OperationType.UPDATE);
                    }
                }

                // Set update timestamp
                subDepartment.setUpdatedAt(LocalDate.now());

                // Update sub department
                mSubDepartmentDao.updateSubDepartment(subDepartment);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("sub_departments", "update");

                Log.d(TAG, "Sub department updated successfully: " + subDepartment.getName());
                return OperationResult.success(subDepartment, "Sub department updated successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update sub department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<String>> deleteSubDepartment(long subDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get sub department before deletion for logging
                SubDepartment subDept = mSubDepartmentDao.getSubDepartmentById(subDepartmentId);
                if (subDept == null) {
                    return OperationResult.failure("Sub department not found: " + subDepartmentId,
                            OperationResult.OperationType.DELETE);
                }

                String subDeptName = subDept.getName();

                // Delete sub department
                mSubDepartmentDao.deleteSubDepartmentById(subDepartmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("sub_departments", "delete");

                Log.d(TAG, "Sub department deleted successfully: " + subDeptName);
                return OperationResult.success(subDeptName, "Sub department deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete sub department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllSubDepartments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get count before deletion
                List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();
                int count = subDepartments.size();

                if (count == 0) {
                    return OperationResult.success(0, "No sub departments to delete",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete all sub departments
                mSubDepartmentDao.deleteAllSubDepartments();

                // Trigger automatic backup
                mBackupManager.performAutoBackup("sub_departments", "bulk_delete");

                Log.d(TAG, "Deleted all " + count + " sub departments");
                return OperationResult.success(count, "Deleted all " + count + " sub departments",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all sub departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteSubDepartmentsByMacroDepartment(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Get sub departments before deletion for count
                List<SubDepartment> subDepartments = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId);
                int count = subDepartments.size();

                if (count == 0) {
                    return OperationResult.success(0, "No sub departments found for macro department: " + macroDepartmentId,
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete sub departments for macro department
                mSubDepartmentDao.deleteSubDepartmentsByMacroDepartment(macroDepartmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("sub_departments", "bulk_delete");

                Log.d(TAG, "Deleted " + count + " sub departments for macro department: " + macroDepartmentId);
                return OperationResult.success(count, "Deleted " + count + " sub departments",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete sub departments by macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    // ==================== QUERY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<SubDepartment>> getSubDepartmentById(long subDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (subDepartmentId <= 0) {
                    return OperationResult.failure("Invalid sub department ID: " + subDepartmentId,
                            OperationResult.OperationType.READ);
                }

                SubDepartment subDepartment = mSubDepartmentDao.getSubDepartmentById(subDepartmentId);
                if (subDepartment != null) {
                    return OperationResult.success(subDepartment, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Sub department not found: " + subDepartmentId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub department by ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<SubDepartment>>> getAllSubDepartments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();
                return OperationResult.success(subDepartments,
                        "Found " + subDepartments.size() + " sub departments",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all sub departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<SubDepartment>>> getSubDepartmentsByMacroDepartment(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }

                List<SubDepartment> subDepartments = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId);
                return OperationResult.success(subDepartments,
                        "Found " + subDepartments.size() + " sub departments for macro department",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub departments by macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<SubDepartment>> getSubDepartmentByNameAndMacroDepartment(long macroDepartmentId, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Sub department name is required",
                            OperationResult.OperationType.READ);
                }

                SubDepartment subDepartment = mSubDepartmentDao.getSubDepartmentByNameAndMacroDepartment(macroDepartmentId, name);
                if (subDepartment != null) {
                    return OperationResult.success(subDepartment, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Sub department not found: " + name + " in macro department " + macroDepartmentId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub department by name and macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateSubDepartment(SubDepartment subDepartment) {
        List<String> errors = new ArrayList<>();

        // Basic null check
        if (subDepartment == null) {
            errors.add("Sub department cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // Name validation
        if (subDepartment.getName() == null || subDepartment.getName().trim().isEmpty()) {
            errors.add("Sub department name is required");
        } else if (subDepartment.getName().length() > 100) {
            errors.add("Sub department name cannot exceed 100 characters");
        }

        // Macro department ID validation
        if (subDepartment.getMacroDepartmentId() <= 0) {
            errors.add("Valid macro department ID is required");
        }

        // Code validation (if provided)
        if (subDepartment.getCode() != null && subDepartment.getCode().length() > 20) {
            errors.add("Sub department code cannot exceed 20 characters");
        }

        // Description validation (if provided)
        if (subDepartment.getDescription() != null && subDepartment.getDescription().length() > 500) {
            errors.add("Sub department description cannot exceed 500 characters");
        }

        // Manager name validation (if provided)
        if (subDepartment.getManagerName() != null && subDepartment.getManagerName().length() > 100) {
            errors.add("Manager name cannot exceed 100 characters");
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> subDepartmentExists(long macroDepartmentId, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.VALIDATION);
                }
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Sub department name is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mSubDepartmentDao.existsByNameAndMacroDepartment(macroDepartmentId, name);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check sub department existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getSubDepartmentsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mSubDepartmentDao.getAllSubDepartments().size();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub departments count", e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> getSubDepartmentsCountByMacroDepartment(long macroDepartmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (macroDepartmentId <= 0) {
                    return OperationResult.failure("Invalid macro department ID: " + macroDepartmentId,
                            OperationResult.OperationType.COUNT);
                }

                int count = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId).size();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sub departments count by macro department: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
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
            Log.d(TAG, "SubDepartmentServiceImpl executor service shutdown");
        }
    }
}