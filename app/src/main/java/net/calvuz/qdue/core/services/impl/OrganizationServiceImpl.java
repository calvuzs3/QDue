package net.calvuz.qdue.core.services.impl;

import android.content.Context;

import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.OrganizationService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED: OrganizationServiceImpl - Dependency Injection Compliant
 *
 * Composite service that manages organizational hierarchy operations with:
 * - Manual dependency injection via constructor
 * - Consistent OperationResult pattern throughout
 * - Automatic backup integration
 * - Background operations with CompletableFuture
 * - High-level operations across multiple organizational entities
 * - Import/Export capabilities
 * - Hierarchical search functionality
 *
 * Fully compliant with OrganizationService interface contract.
 */
public class OrganizationServiceImpl implements OrganizationService {

    private static final String TAG = "OrganizationServiceImpl";

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
    public OrganizationServiceImpl(Context context, QDueDatabase database, CoreBackupManager backupManager) {
        this.mContext = context.getApplicationContext();
        this.mDatabase = database;
        this.mEstablishmentDao = database.establishmentDao();
        this.mMacroDepartmentDao = database.macroDepartmentDao();
        this.mSubDepartmentDao = database.subDepartmentDao();
        this.mBackupManager = backupManager;
        this.mExecutorService = Executors.newFixedThreadPool(4);

        Log.d(TAG, "OrganizationServiceImpl initialized via dependency injection");
    }

    /**
     * Alternative constructor with automatic backup manager creation
     * For backward compatibility
     */
    public OrganizationServiceImpl(Context context, QDueDatabase database) {
        this(context, database, new CoreBackupManager(context, database));
    }

    // ==================== COMPOSITE OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<OrganizationHierarchy>> createCompleteOrganization(
            Establishment establishment,
            List<MacroDepartment> macroDepartments,
            List<SubDepartment> subDepartments) {

        // Make parameters effectively final for lambda
        final List<MacroDepartment> finalMacroDepartments = macroDepartments != null ? macroDepartments : new ArrayList<>();
        final List<SubDepartment> finalSubDepartments = subDepartments != null ? subDepartments : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating complete organization: " + establishment.getName());

                // Input validation
                if (establishment == null) {
                    return OperationResult.failure("Establishment is required",
                            OperationResult.OperationType.CREATE);
                }

                // Validate organization hierarchy structure
                List<MacroDepartmentWithSubs> tempMacroWithSubs = new ArrayList<>();
                OrganizationHierarchy tempHierarchy = new OrganizationHierarchy(establishment, tempMacroWithSubs);
                OperationResult<Void> validation = validateOrganizationHierarchy(tempHierarchy);
                if (validation.isFailure()) {
                    return OperationResult.failure(validation.getErrors(),
                            OperationResult.OperationType.CREATE);
                }

                // Set creation timestamps for establishment
                establishment.setCreatedAt(LocalDate.now());
                establishment.setUpdatedAt(LocalDate.now());

                // Create establishment first
                long establishmentId = mEstablishmentDao.insertEstablishment(establishment);
                establishment.setId(establishmentId);

                // Create macro departments and associate sub departments
                List<MacroDepartmentWithSubs> finalMacroWithSubsList = new ArrayList<>();

                for (MacroDepartment macro : finalMacroDepartments) {
                    // Set establishment reference and timestamps
                    macro.setEstablishmentId(establishmentId);
                    macro.setCreatedAt(LocalDate.now());
                    macro.setUpdatedAt(LocalDate.now());

                    // Insert macro department
                    long macroId = mMacroDepartmentDao.insertMacroDepartment(macro);
                    macro.setId(macroId);

                    // Find and create sub departments for this macro department
                    List<SubDepartment> subsForThisMacro = new ArrayList<>();
                    for (SubDepartment sub : finalSubDepartments) {
                        // Associate unassigned sub-departments or those specifically assigned to this macro
                        if (sub.getMacroDepartmentId() == 0 ||
                                sub.getMacroDepartmentId() == macro.getId()) {

                            sub.setMacroDepartmentId(macroId);
                            sub.setCreatedAt(LocalDate.now());
                            sub.setUpdatedAt(LocalDate.now());

                            long subId = mSubDepartmentDao.insertSubDepartment(sub);
                            sub.setId(subId);
                            subsForThisMacro.add(sub);
                        }
                    }

                    finalMacroWithSubsList.add(new MacroDepartmentWithSubs(macro, subsForThisMacro));
                }

                // Create final hierarchy structure
                OrganizationHierarchy finalHierarchy = new OrganizationHierarchy(establishment, finalMacroWithSubsList);

                // Trigger comprehensive backup for complete organization creation
                mBackupManager.performFullApplicationBackup();

                int totalEntities = 1 + finalMacroDepartments.size() + finalSubDepartments.size();
                Log.d(TAG, "Complete organization created successfully: " + establishment.getName() +
                        " (" + totalEntities + " entities)");

                return OperationResult.success(finalHierarchy,
                        "Complete organization created with " + totalEntities + " entities",
                        OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create complete organization: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<OrganizationHierarchy>> getCompleteOrganizationHierarchy(long establishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (establishmentId <= 0) {
                    return OperationResult.failure("Invalid establishment ID: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                // Get establishment
                Establishment establishment = mEstablishmentDao.getEstablishmentById(establishmentId);
                if (establishment == null) {
                    return OperationResult.failure("Establishment not found: " + establishmentId,
                            OperationResult.OperationType.READ);
                }

                // Get macro departments for this establishment
                List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);

                // Build hierarchy with sub departments
                List<MacroDepartmentWithSubs> macroWithSubs = new ArrayList<>();
                int totalSubDepartments = 0;

                for (MacroDepartment macro : macroDepartments) {
                    // Get sub departments for this macro department
                    List<SubDepartment> subDepartments = mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macro.getId());
                    totalSubDepartments += subDepartments.size();
                    macroWithSubs.add(new MacroDepartmentWithSubs(macro, subDepartments));
                }

                OrganizationHierarchy hierarchy = new OrganizationHierarchy(establishment, macroWithSubs);

                Log.d(TAG, "Retrieved complete organization hierarchy for: " + establishment.getName() +
                        " (" + macroDepartments.size() + " macros, " + totalSubDepartments + " subs)");

                return OperationResult.success(hierarchy,
                        "Retrieved hierarchy with " + macroDepartments.size() + " macro departments and " +
                                totalSubDepartments + " sub departments",
                        OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get complete organization hierarchy: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<Integer>> deleteCompleteOrganization(long establishmentId) {
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

                // Count entities before deletion for accurate reporting
                List<MacroDepartment> macros = mMacroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                int subDepartmentsCount = 0;
                for (MacroDepartment macro : macros) {
                    subDepartmentsCount += mSubDepartmentDao.getSubDepartmentsByMacroDepartment(macro.getId()).size();
                }

                int totalDeleted = 1 + macros.size() + subDepartmentsCount; // 1 establishment + macros + subs
                String establishmentName = establishment.getName();

                // Delete in hierarchical order (sub departments will be deleted by CASCADE constraints)
                // But we do explicit deletion for better control and logging
                for (MacroDepartment macro : macros) {
                    mSubDepartmentDao.deleteSubDepartmentsByMacroDepartment(macro.getId());
                }
                mMacroDepartmentDao.deleteMacroDepartmentsByEstablishment(establishmentId);
                mEstablishmentDao.deleteEstablishmentById(establishmentId);

                // Trigger comprehensive backup after major deletion
                mBackupManager.performFullApplicationBackup();

                Log.d(TAG, "Deleted complete organization: " + establishmentName + " (" + totalDeleted + " entities)");
                return OperationResult.success(totalDeleted,
                        "Deleted complete organization '" + establishmentName + "' with " + totalDeleted + " entities",
                        OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete complete organization: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    // ==================== SEARCH OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<List<OrganizationSearchResult>>> searchOrganization(String searchTerm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (searchTerm == null || searchTerm.trim().isEmpty()) {
                    return OperationResult.failure("Search term is required",
                            OperationResult.OperationType.SEARCH);
                }

                List<OrganizationSearchResult> results = new ArrayList<>();
                final String normalizedSearchTerm = searchTerm.toLowerCase().trim();

                // Search establishments
                List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();
                for (Establishment est : establishments) {
                    if (est.getName().toLowerCase().contains(normalizedSearchTerm)) {
                        results.add(new OrganizationSearchResult("establishment", est.getId(), est.getName(), null));
                    }
                }

                // Search macro departments
                List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getAllMacroDepartments();
                for (MacroDepartment macro : macroDepartments) {
                    if (macro.getName().toLowerCase().contains(normalizedSearchTerm)) {
                        Establishment parent = mEstablishmentDao.getEstablishmentById(macro.getEstablishmentId());
                        String parentName = parent != null ? parent.getName() : "Unknown Establishment";
                        results.add(new OrganizationSearchResult("macro_department", macro.getId(), macro.getName(), parentName));
                    }
                }

                // Search sub departments
                List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();
                for (SubDepartment sub : subDepartments) {
                    if (sub.getName().toLowerCase().contains(normalizedSearchTerm)) {
                        MacroDepartment parent = mMacroDepartmentDao.getMacroDepartmentById(sub.getMacroDepartmentId());
                        String parentName = parent != null ? parent.getName() : "Unknown Macro Department";
                        results.add(new OrganizationSearchResult("sub_department", sub.getId(), sub.getName(), parentName));
                    }
                }

                Log.d(TAG, "Organization search for '" + searchTerm + "' returned " + results.size() + " results");
                return OperationResult.success(results,
                        "Found " + results.size() + " results for '" + searchTerm + "'",
                        OperationResult.OperationType.SEARCH);

            } catch (Exception e) {
                Log.e(TAG, "Failed to search organization: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.SEARCH);
            }
        }, mExecutorService);
    }

    // ==================== IMPORT/EXPORT OPERATIONS ====================

    @Override
    public CompletableFuture<OperationResult<OrganizationImportResult>> importOrganizationData(
            List<Establishment> establishments,
            List<MacroDepartment> macroDepartments,
            List<SubDepartment> subDepartments) {

        // Make parameters effectively final for lambda
        final List<Establishment> finalEstablishments = establishments != null ? establishments : new ArrayList<>();
        final List<MacroDepartment> finalMacroDepartments = macroDepartments != null ? macroDepartments : new ArrayList<>();
        final List<SubDepartment> finalSubDepartments = subDepartments != null ? subDepartments : new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (finalEstablishments.isEmpty() && finalMacroDepartments.isEmpty() && finalSubDepartments.isEmpty()) {
                    return OperationResult.failure("No data provided for import",
                            OperationResult.OperationType.IMPORT);
                }

                Log.d(TAG, "Importing organization data: " + finalEstablishments.size() + " establishments, " +
                        finalMacroDepartments.size() + " macro departments, " + finalSubDepartments.size() + " sub departments");

                List<String> errors = new ArrayList<>();
                int establishmentsImported = 0;
                int macroDepartmentsImported = 0;
                int subDepartmentsImported = 0;

                // Import establishments first (they are the root of the hierarchy)
                for (int i = 0; i < finalEstablishments.size(); i++) {
                    Establishment est = finalEstablishments.get(i);
                    try {
                        // Set import timestamps
                        est.setCreatedAt(LocalDate.now());
                        est.setUpdatedAt(LocalDate.now());

                        mEstablishmentDao.insertEstablishment(est);
                        establishmentsImported++;
                    } catch (Exception e) {
                        errors.add("Establishment " + (i + 1) + " '" + est.getName() + "': " + e.getMessage());
                    }
                }

                // Import macro departments
                for (int i = 0; i < finalMacroDepartments.size(); i++) {
                    MacroDepartment macro = finalMacroDepartments.get(i);
                    try {
                        // Set import timestamps
                        macro.setCreatedAt(LocalDate.now());
                        macro.setUpdatedAt(LocalDate.now());

                        mMacroDepartmentDao.insertMacroDepartment(macro);
                        macroDepartmentsImported++;
                    } catch (Exception e) {
                        errors.add("Macro department " + (i + 1) + " '" + macro.getName() + "': " + e.getMessage());
                    }
                }

                // Import sub departments
                for (int i = 0; i < finalSubDepartments.size(); i++) {
                    SubDepartment sub = finalSubDepartments.get(i);
                    try {
                        // Set import timestamps
                        sub.setCreatedAt(LocalDate.now());
                        sub.setUpdatedAt(LocalDate.now());

                        mSubDepartmentDao.insertSubDepartment(sub);
                        subDepartmentsImported++;
                    } catch (Exception e) {
                        errors.add("Sub department " + (i + 1) + " '" + sub.getName() + "': " + e.getMessage());
                    }
                }

                // Trigger comprehensive backup after import
                mBackupManager.performFullApplicationBackup();

                // Create import result
                OrganizationImportResult result = new OrganizationImportResult(
                        establishmentsImported, macroDepartmentsImported, subDepartmentsImported, errors);

                int totalImported = establishmentsImported + macroDepartmentsImported + subDepartmentsImported;

                if (!errors.isEmpty() && totalImported == 0) {
                    Log.e(TAG, "Organization import failed completely with " + errors.size() + " errors");
                    return OperationResult.failure(errors, OperationResult.OperationType.IMPORT);
                } else if (!errors.isEmpty()) {
                    Log.w(TAG, "Organization import completed partially: " + totalImported + " imported, " + errors.size() + " errors");
                    return OperationResult.success(result,
                            "Partial import: " + totalImported + " entities imported, " + errors.size() + " failed",
                            OperationResult.OperationType.IMPORT);
                } else {
                    Log.d(TAG, "Organization import completed successfully: " + totalImported + " entities");
                    return OperationResult.success(result,
                            "Successfully imported " + totalImported + " organization entities",
                            OperationResult.OperationType.IMPORT);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to import organization data: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.IMPORT);
            }
        }, mExecutorService);
    }

    @Override
    public CompletableFuture<OperationResult<OrganizationExportData>> exportOrganizationData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Retrieve all organizational data
                List<Establishment> establishments = mEstablishmentDao.getAllEstablishments();
                List<MacroDepartment> macroDepartments = mMacroDepartmentDao.getAllMacroDepartments();
                List<SubDepartment> subDepartments = mSubDepartmentDao.getAllSubDepartments();

                // Create export data structure
                OrganizationExportData exportData = new OrganizationExportData(
                        establishments, macroDepartments, subDepartments);

                int totalEntities = establishments.size() + macroDepartments.size() + subDepartments.size();

                if (totalEntities == 0) {
                    Log.w(TAG, "No organization data available for export");
                    return OperationResult.success(exportData, "No organization data available for export",
                            OperationResult.OperationType.EXPORT);
                }

                Log.d(TAG, "Exported organization data: " + establishments.size() + " establishments, " +
                        macroDepartments.size() + " macro departments, " + subDepartments.size() + " sub departments");

                return OperationResult.success(exportData,
                        "Exported " + totalEntities + " organization entities (" +
                                establishments.size() + " establishments, " +
                                macroDepartments.size() + " macro departments, " +
                                subDepartments.size() + " sub departments)",
                        OperationResult.OperationType.EXPORT);

            } catch (Exception e) {
                Log.e(TAG, "Failed to export organization data: " + e.getMessage(), e);
                return OperationResult.failure(e, OperationResult.OperationType.EXPORT);
            }
        }, mExecutorService);
    }

    // ==================== VALIDATION ====================

    @Override
    public OperationResult<Void> validateOrganizationHierarchy(OrganizationHierarchy hierarchy) {
        List<String> errors = new ArrayList<>();

        // Basic null checks
        if (hierarchy == null) {
            errors.add("Organization hierarchy cannot be null");
            return OperationResult.validationFailure(errors);
        }

        // Establishment validation
        if (hierarchy.establishment == null) {
            errors.add("Establishment is required");
        } else {
            if (hierarchy.establishment.getName() == null || hierarchy.establishment.getName().trim().isEmpty()) {
                errors.add("Establishment name is required");
            } else if (hierarchy.establishment.getName().length() > 100) {
                errors.add("Establishment name cannot exceed 100 characters");
            }
        }

        // Macro departments validation
        if (hierarchy.macroDepartments != null) {
            for (int i = 0; i < hierarchy.macroDepartments.size(); i++) {
                MacroDepartmentWithSubs macroWithSubs = hierarchy.macroDepartments.get(i);

                if (macroWithSubs == null) {
                    errors.add("Macro department entry " + (i + 1) + " cannot be null");
                    continue;
                }

                if (macroWithSubs.macroDepartment == null) {
                    errors.add("Macro department " + (i + 1) + " cannot be null");
                    continue;
                }

                // Validate macro department
                if (macroWithSubs.macroDepartment.getName() == null ||
                        macroWithSubs.macroDepartment.getName().trim().isEmpty()) {
                    errors.add("Macro department " + (i + 1) + " name is required");
                } else if (macroWithSubs.macroDepartment.getName().length() > 100) {
                    errors.add("Macro department " + (i + 1) + " name cannot exceed 100 characters");
                }

                // Validate sub departments for this macro
                if (macroWithSubs.subDepartments != null) {
                    for (int j = 0; j < macroWithSubs.subDepartments.size(); j++) {
                        SubDepartment sub = macroWithSubs.subDepartments.get(j);
                        if (sub == null) {
                            errors.add("Sub department " + (j + 1) + " in macro department " + (i + 1) + " cannot be null");
                        } else if (sub.getName() == null || sub.getName().trim().isEmpty()) {
                            errors.add("Sub department " + (j + 1) + " in macro department " + (i + 1) + " name is required");
                        } else if (sub.getName().length() > 100) {
                            errors.add("Sub department " + (j + 1) + " in macro department " + (i + 1) + " name cannot exceed 100 characters");
                        }
                    }
                }
            }
        }

        return errors.isEmpty() ? OperationResult.validationSuccess() : OperationResult.validationFailure(errors);
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clean up resources when service is no longer needed
     * Should be called from DI container or application lifecycle
     */
    public void shutdown() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            Log.d(TAG, "OrganizationServiceImpl executor service shutdown");
        }
    }
}