package net.calvuz.qdue.services;

import net.calvuz.qdue.services.models.OperationResult;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * STEP 1: Core Service Interfaces for User and Organization Management
 *
 * Provides centralized service layer for all user and organizational operations
 * with consistent backup, validation, and business logic.
 *
 * These interfaces prepare the foundation for extending the Core Backup System
 * to all entities beyond just events.
 */

// ==================== MACRO DEPARTMENT SERVICE ====================

/**
 * Centralized service for ALL MacroDepartment operations
 * Manages macro departments within establishments
 */
public interface MacroDepartmentService {

    // CRUD Operations
    CompletableFuture<OperationResult<MacroDepartment>> createMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<OperationResult<List<MacroDepartment>>> createMacroDepartments(List<MacroDepartment> macroDepartments);
    CompletableFuture<OperationResult<MacroDepartment>> updateMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<OperationResult<String>> deleteMacroDepartment(long macroDepartmentId);
    CompletableFuture<OperationResult<Integer>> deleteAllMacroDepartments();
    CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentsByEstablishment(long establishmentId);

    // Query Operations
    CompletableFuture<MacroDepartment> getMacroDepartmentById(long macroDepartmentId);
    CompletableFuture<List<MacroDepartment>> getAllMacroDepartments();
    CompletableFuture<List<MacroDepartment>> getMacroDepartmentsByEstablishment(long establishmentId);
    CompletableFuture<MacroDepartment> getMacroDepartmentByNameAndEstablishment(long establishmentId, String name);

    // Validation
    OperationResult<Void> validateMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<Boolean> macroDepartmentExists(long establishmentId, String name);

    // Statistics
    CompletableFuture<Integer> getMacroDepartmentsCount();
    CompletableFuture<Integer> getMacroDepartmentsCountByEstablishment(long establishmentId);

    // Hierarchy Operations
    CompletableFuture<List<SubDepartment>> getSubDepartments(long macroDepartmentId);
    CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentWithDependencies(long macroDepartmentId);
}
