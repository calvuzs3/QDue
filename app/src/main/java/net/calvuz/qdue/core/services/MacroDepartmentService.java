package net.calvuz.qdue.core.services;

import net.calvuz.qdue.core.services.models.OperationResult;
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

// ==================== MACRO DEPARTMENT SERVICE ====================

/**
 * Centralized service for ALL MacroDepartment operations
 * Manages macro departments within establishments
 * ✅ REFACTORED: MacroDepartmentService - Complete OperationResult compliance
 */
public interface MacroDepartmentService {

    // CRUD Operations
    CompletableFuture<OperationResult<MacroDepartment>> createMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<OperationResult<List<MacroDepartment>>> createMacroDepartments(List<MacroDepartment> macroDepartments);
    CompletableFuture<OperationResult<MacroDepartment>> updateMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<OperationResult<String>> deleteMacroDepartment(long macroDepartmentId);
    CompletableFuture<OperationResult<Integer>> deleteAllMacroDepartments();
    CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentsByEstablishment(long establishmentId);

    // ✅ Query Operations - ALL converted to OperationResult
    CompletableFuture<OperationResult<MacroDepartment>> getMacroDepartmentById(long macroDepartmentId);
    CompletableFuture<OperationResult<List<MacroDepartment>>> getAllMacroDepartments();
    CompletableFuture<OperationResult<List<MacroDepartment>>> getMacroDepartmentsByEstablishment(long establishmentId);
    CompletableFuture<OperationResult<MacroDepartment>> getMacroDepartmentByNameAndEstablishment(long establishmentId, String name);

    // Validation
    OperationResult<Void> validateMacroDepartment(MacroDepartment macroDepartment);
    CompletableFuture<OperationResult<Boolean>> macroDepartmentExists(long establishmentId, String name);

    // ✅ Statistics - ALL converted to OperationResult
    CompletableFuture<OperationResult<Integer>> getMacroDepartmentsCount();
    CompletableFuture<OperationResult<Integer>> getMacroDepartmentsCountByEstablishment(long establishmentId);

    // ✅ Hierarchy Operations - ALL converted to OperationResult
    CompletableFuture<OperationResult<List<SubDepartment>>> getSubDepartments(long macroDepartmentId);
    CompletableFuture<OperationResult<Integer>> deleteMacroDepartmentWithDependencies(long macroDepartmentId);
}