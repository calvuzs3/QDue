package net.calvuz.qdue.core.services;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * STEP 1: Core Service Interfaces for User and Organization Management
 * <p>
 * Provides centralized service layer for all user and organizational operations
 * with consistent backup, validation, and business logic.
 * <p>
 * These interfaces prepare the foundation for extending the Core Backup System
 * to all entities beyond just events.
 */

// ==================== ORGANIZATION SERVICE ====================

/**
 * Composite service that manages organizational hierarchy operations
 * Provides high-level operations across multiple organizational entities
 * ✅ REFACTORED: OrganizationService - Complete OperationResult compliance
 */
public interface OrganizationService {

    // Composite Operations
    CompletableFuture<OperationResult<OrganizationHierarchy>> createCompleteOrganization(
            Establishment establishment,
            List<MacroDepartment> macroDepartments,
            List<SubDepartment> subDepartments);

    CompletableFuture<OperationResult<OrganizationHierarchy>> getCompleteOrganizationHierarchy(long establishmentId);

    CompletableFuture<OperationResult<Integer>> deleteCompleteOrganization(long establishmentId);

    // ✅ Search Operations - ALL converted to OperationResult
    CompletableFuture<OperationResult<List<OrganizationSearchResult>>> searchOrganization(String searchTerm);

    // Import/Export Operations
    CompletableFuture<OperationResult<OrganizationImportResult>> importOrganizationData(
            List<Establishment> establishments,
            List<MacroDepartment> macroDepartments,
            List<SubDepartment> subDepartments);

    CompletableFuture<OperationResult<OrganizationExportData>> exportOrganizationData();

    // Validation
    OperationResult<Void> validateOrganizationHierarchy(OrganizationHierarchy hierarchy);

    // ==================== INNER CLASSES ====================

    /**
     * Complete organization hierarchy
     */
    class OrganizationHierarchy {
        public final Establishment establishment;
        public final List<MacroDepartmentWithSubs> macroDepartments;

        public OrganizationHierarchy(Establishment establishment, List<MacroDepartmentWithSubs> macroDepartments) {
            this.establishment = establishment;
            this.macroDepartments = macroDepartments;
        }
    }

    /**
     * Macro department with its sub departments
     */
    class MacroDepartmentWithSubs {
        public final MacroDepartment macroDepartment;
        public final List<SubDepartment> subDepartments;

        public MacroDepartmentWithSubs(MacroDepartment macroDepartment, List<SubDepartment> subDepartments) {
            this.macroDepartment = macroDepartment;
            this.subDepartments = subDepartments;
        }
    }

    /**
     * Organization search result
     */
    class OrganizationSearchResult {
        public final String type; // "establishment", "macro_department", "sub_department"
        public final long id;
        public final String name;
        public final String parentName;

        public OrganizationSearchResult(String type, long id, String name, String parentName) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.parentName = parentName;
        }
    }

    /**
     * Organization import result
     */
    class OrganizationImportResult {
        public final int establishmentsImported;
        public final int macroDepartmentsImported;
        public final int subDepartmentsImported;
        public final List<String> errors;

        public OrganizationImportResult(int establishmentsImported, int macroDepartmentsImported,
                                        int subDepartmentsImported, List<String> errors) {
            this.establishmentsImported = establishmentsImported;
            this.macroDepartmentsImported = macroDepartmentsImported;
            this.subDepartmentsImported = subDepartmentsImported;
            this.errors = errors;
        }
    }

    /**
     * Organization export data
     */
    class OrganizationExportData {
        public final List<Establishment> establishments;
        public final List<MacroDepartment> macroDepartments;
        public final List<SubDepartment> subDepartments;

        public OrganizationExportData(List<Establishment> establishments,
                                      List<MacroDepartment> macroDepartments,
                                      List<SubDepartment> subDepartments) {
            this.establishments = establishments;
            this.macroDepartments = macroDepartments;
            this.subDepartments = subDepartments;
        }
    }
}