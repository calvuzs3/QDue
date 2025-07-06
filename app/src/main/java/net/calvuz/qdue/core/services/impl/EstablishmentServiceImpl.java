package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.EstablishmentService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: EstablishmentServiceImpl - Dependency Injection Compliant
 *
 * Centralized implementation for all Establishment operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - Comprehensive validation and error handling
 * - Organization hierarchy management
 *
 * Fully compliant with EstablishmentService interface contract.
 */
public class EstablishmentServiceImpl implements EstablishmentService {

    private static final String TAG = "EstablishmentServiceImpl";

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final QDueDatabase mDatabase;
    private final EstablishmentDao mEstablishmentDao;
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
    public EstablishmentServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mEstablishmentDao = database.establishmentDao();
        this.mMacroDepartmentDao = database.macroDepartmentDao();
        this.mSubDepartmentDao = database.subDepartmentDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "EstablishmentServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public EstablishmentServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<Establishment>> createEstablishment(Establishment establishment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate establishment input
                OperationResult<Void> validation = validateEstablishment(establishment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for name duplicates
                if (mEstablishmentDao.existsByName(establishment.getName())) {
                    return OperationResult.failure("Establishment with name already exists: " + establishment.getName(),
                            OperationResult.OperationType.CREATE);
                }

                // Check for code duplicates (if provided)
                if (establishment.getCode() != null && !establishment.getCode().trim().isEmpty() &&
                        mEstablishmentDao.existsByCode(establishment.getCode())) {
                    return OperationResult.failure("Establishment with code already exists: " + establishment.getCode(),
                            OperationResult.OperationType.CREATE);
                }

                // Set creation timestamps
                establishment.setCreatedAt(LocalDate.now());
                establishment.setUpdatedAt(LocalDate.now());

                // Insert establishment and get generated ID
                long id = mEstablishmentDao.insertEstablishment(establishment);
                establishment.setId(id);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("establishments", "create");

                Log.d(TAG, "Establishment created successfully: " + establishment.getName());
                return OperationResult.success(establishment, "Establishment created successfully",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<Establishment>>> createEstablishments(List<Establishment> establishments) {
        // Make parameter effectively final for lambda
        final List<Establishment> finalEstablishments = establishments != null ? establishments : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalEstablishments.isEmpty()) {
                    return OperationResult.failure("No establishments provided for creation",
                            OperationResult.OperationType.BULK_CREATE);
                }

                List<String> errors = new ArrayList<>();
                List<Establishment> created = new ArrayList<>();

                for (int i = 0; i < finalEstablishments.size(); i++) {
                    Establishment establishment = finalEstablishments.get(i);

                    // Validate each establishment
                    OperationResult<Void> validation = validateEstablishment(establishment);
                    if (validation.isFailure()) {
                        errors.add("Establishment " + (i + 1) + ": " + validation.getFirstError());
                        continue;
                    }

                    // Check for duplicates
                    if (mEstablishmentDao.existsByName(establishment.getName())) {
                        errors.add("Establishment " + (i + 1) + ": Duplicate name");
                        continue;
                    }

                    if (establishment.getCode() != null && !establishment.getCode().trim().isEmpty() &&
                            mEstablishmentDao.existsByCode(establishment.getCode())) {
                        errors.add("Establishment " + (i + 1) + ": Duplicate code");
                        continue;
                    }

                    try {
                        // Set timestamps and insert
                        establishment.setCreatedAt(LocalDate.now());
                        establishment.setUpdatedAt(LocalDate.now());
                        long id = mEstablishmentDao.insertEstablishment(establishment);
                        establishment.setId(id);
                        created.add(establishment);
                    } catch (Exception e) {
                        errors.add("Establishment " + (i + 1) + ": " + e.getMessage());
                    }
                }

                // Trigger backup if any establishments were created
                if (!created.isEmpty()) {
                    mBackupManager.performAutoBackup("establishments", "bulk_create");
                }

                // Return appropriate result
                if (!created.isEmpty() && errors.isEmpty()) {
                    Log.d(TAG, "Created " + created.size() + " establishments successfully");
                    return OperationResult.success(created, "Created " + created.size() + " establishments",
                            OperationResult.OperationType.BULK_CREATE);
                } else if (!created.isEmpty() && !errors.isEmpty()) {
                    Log.w(TAG, "Partially created " + created.size() + " establishments with " + errors.size() + " errors");
                    return OperationResult.success(created,
                            "Partially created " + created.size() + " establishments (" + errors.size() + " failed)",
                            OperationResult.OperationType.BULK_CREATE);
                } else {
                    return OperationResult.failure(errors, OperationResult.OperationType.BULK_CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to create establishments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Establishment>> updateEstablishment(Establishment establishment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate establishment input
                OperationResult<Void> validation = validateEstablishment(establishment);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check if establishment exists
                Establishment existing = mEstablishmentDao.getEstablishmentById(establishment.getId());
                if (existing == null) {
                    return OperationResult.failure("Establishment not found: " + establishment.getId(),
                            OperationResult.OperationType.UPDATE);
                }

                // Check name uniqueness (excluding current establishment)
                if (!establishment.getName().equals(existing.getName())) {
                    if (mEstablishmentDao.existsByName(establishment.getName())) {
                        return OperationResult.failure("Establishment name already exists: " + establishment.getName(),
                                OperationResult.OperationType.UPDATE);
                    }
                }

                // Check code uniqueness (excluding current establishment)
                if (establishment.getCode() != null && !establishment.getCode().equals(existing.getCode())) {
                    if (mEstablishmentDao.existsByCode(establishment.getCode())) {
                        return OperationResult.failure("Establishment code already exists: " + establishment.getCode(),
                                OperationResult.OperationType.UPDATE);
                    }
                }

                // Set update timestamp
                establishment.setUpdatedAt(LocalDate.now());

                // Update establishment
                mEstablishmentDao.updateEstablishment(establishment);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("establishments", "update");

                Log.d(TAG, "Establishment updated successfully: " + establishment.getName());
                return OperationResult.success(establishment, "Establishment updated successfully",
                        OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to update establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<String>> deleteEstablishment(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get establishment before deletion for logging
                Establishment establishment = mEstablishmentDao.getEstablishmentById(establishmentId);
                if (establishment == null) {
                    return OperationResult.failure("Establishment not found: " + establishmentId,
                            OperationResult.OperationType.DELETE);
                }

                String establishmentName = establishment.getName();

                // Delete establishment (will cascade to departments)
                mEstablishmentDao.deleteEstablishmentById(establishmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("establishments", "delete");

                Log.d(TAG, "Establishment deleted successfully: " + establishmentName);
                return OperationResult.success(establishmentName, "Establishment deleted successfully",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete establishment: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteAllEstablishments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get count before deletion
                List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();
                int count = establishments.size();

                if (count == 0) {
                    return OperationResult.success(0, "No establishments to delete",
                            OperationResult.OperationType.BULK_DELETE);
                }

                // Delete all establishments
                mEstablishmentDao.deleteAllEstablishments();

                // Trigger automatic backup
                mBackupManager.performAutoBackup("establishments", "bulk_delete");

                Log.d(TAG, "Deleted all " + count + " establishments");
                return OperationResult.success(count, "Deleted all " + count + " establishments",
                        OperationResult.OperationType.BULK_DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all establishments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.BULK_DELETE);
            }
        }, mExecutorService);
    }

    // ==================== QUERY OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<Establishment>> getEstablishmentById(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                Establishment establishment = mEstablishmentDao.getEstablishmentById(establishmentId);
                if (establishment != null) {
                    return OperationResult.success(establishment, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Establishment not found: " + establishmentId,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get establishment by ID: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Establishment>> getEstablishmentByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Establishment name cannot be null or empty",
                            OperationResult.OperationType.READ);
                }

                Establishment establishment = mEstablishmentDao.getEstablishmentByName(name);
                if (establishment != null) {
                    return OperationResult.success(establishment, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Establishment not found with name: " + name,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get establishment by name: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Establishment>> getEstablishmentByCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (code == null || code.trim().isEmpty()) {
                    return OperationResult.failure("Establishment code cannot be null or empty",
                            OperationResult.OperationType.READ);
                }

                Establishment establishment = mEstablishmentDao.getEstablishmentByCode(code);
                if (establishment != null) {
                    return OperationResult.success(establishment, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Establishment not found with code: " + code,
                            OperationResult.OperationType.READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get establishment by code: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<List<Establishment>>> getAllEstablishments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();
                return OperationResult.success(establishments,
                        "Found " + establishments.size() + " establishments",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all establishments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateEstablishment(Establishment establishment) {
        List<String> errors = new ArrayList<>();

        // Basic null check
        if (establishment == null) {
            errors.add("Establishment cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // Name validation
        if (establishment.getName() == null || establishment.getName().trim().isEmpty()) {
            errors.add("Establishment name is required");
        } else if (establishment.getName().length() > 100) {
            errors.add("Establishment name cannot exceed 100 characters");
        }

        // Code validation (optional but limited)
        if (establishment.getCode() != null) {
            if (establishment.getCode().trim().isEmpty()) {
                errors.add("Establishment code cannot be empty if provided");
            } else if (establishment.getCode().length() > 20) {
                errors.add("Establishment code cannot exceed 20 characters");
            }
        }

        // Address validation (optional but limited)
        if (establishment.getAddress() != null && establishment.getAddress().length() > 255) {
            errors.add("Establishment address cannot exceed 255 characters");
        }

        // Phone validation (optional but limited)
        if (establishment.getPhone() != null && establishment.getPhone().length() > 20) {
            errors.add("Establishment phone cannot exceed 20 characters");
        }

        // Email validation (optional but format check)
        if (establishment.getEmail() != null && !establishment.getEmail().trim().isEmpty()) {
            if (!isValidEmail(establishment.getEmail())) {
                errors.add("Invalid email format");
            } else if (establishment.getEmail().length() > 100) {
                errors.add("Establishment email cannot exceed 100 characters");
            }
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> establishmentExists(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.trim().isEmpty()) {
                    return OperationResult.failure("Establishment name is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mEstablishmentDao.existsByName(name);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check establishment existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Boolean>> establishmentCodeExists(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (code == null || code.trim().isEmpty()) {
                    return OperationResult.failure("Establishment code is required",
                            OperationResult.OperationType.VALIDATION);
                }

                boolean exists = mEstablishmentDao.existsByCode(code);
                return OperationResult.success(exists, OperationResult.OperationType.VALIDATION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check establishment code existence: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.VALIDATION);
            }
        }, mExecutorService);
    }

    // ==================== STATISTICS ====================

    @Override
    public CompletableFuture<OperationResult<Integer>> getEstablishmentsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int count = mEstablishmentDao.getEstablishmentCount();
                return OperationResult.success(count, OperationResult.OperationType.COUNT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get establishments count: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.COUNT);
            }
        }, mExecutorService);
    }

    // ==================== ORGANIZATION HIERARCHY ====================

    @Override
    public CompletableFuture<OperationResult<List<MacroDepartment>>> getMacroDepartments(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                // Verify establishment exists
                Establishment establishment = mEstablishmentDao.getEstablishmentById(establishmentId);
                if (establishment == null) {
                    return OperationResult.failure("Establishment not found: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                return OperationResult.success(macroDepartments,
                        "Found " + macroDepartments.size() + " macro departments for establishment",
                        OperationResult.OperationType.READ);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get macro departments: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteEstablishmentWithDependencies(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.DELETE);
                }

                // Verify establishment exists and get info for logging
                Establishment establishment = mEstablishmentDao.getEstablishmentById(establishmentId);
                if (establishment == null) {
                    return OperationResult.failure("Establishment not found: " + establishmentId,
                            OperationResult.OperationType.DELETE);
                }

                // Count dependencies before deletion
                List<MacroDepartment> macros = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                int subDepartmentsCount = 0;
                for (MacroDepartment macro : macros) {
                    subDepartmentsCount += mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macro.getId()).size();
                }

                int totalDeleted = 1 + macros.size() + subDepartmentsCount; // 1 establishment + macros + subs
                String establishmentName = establishment.getName();

                // Delete in hierarchical order (sub departments will be deleted by CASCADE)
                // But we do explicit deletion for better control and logging
                for (MacroDepartment macro : macros) {
                    mSubDepartmentDao.deleteSubDepartmentsByMacroDepartment(macro.getId());
                }
                mMacroDepartmentDao.deleteMacroDepartmentsByEstablishment(establishmentId);
                mEstablishmentDao.deleteEstablishmentById(establishmentId);

                // Trigger automatic backup
                mBackupManager.performAutoBackup("establishments", "delete");

                Log.d(TAG, "Deleted establishment with dependencies: " + establishmentName + " (" + totalDeleted + " entities)");
                return OperationResult.success(totalDeleted,
                        "Deleted establishment '" + establishmentName + "' with " + totalDeleted + " entities",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete establishment with dependencies: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate email format using regex
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when service is no longer needed
     * Should be called from DI container or application lifecycle
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "EstablishmentServiceImpl executor service shutdown");
        }
    }
}