package net.calvuz.qdue.services;

import net.calvuz.qdue.services.models.OperationResult;
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

// ==================== SUB DEPARTMENT SERVICE ====================

/**
 * Centralized service for ALL SubDepartment operations
 * Manages sub departments within macro departments
 */
public interface SubDepartmentService {

    // CRUD Operations
    CompletableFuture<OperationResult<SubDepartment>> createSubDepartment(SubDepartment subDepartment);
    CompletableFuture<OperationResult<List<SubDepartment>>> createSubDepartments(List<SubDepartment> subDepartments);
    CompletableFuture<OperationResult<SubDepartment>> updateSubDepartment(SubDepartment subDepartment);
    CompletableFuture<OperationResult<String>> deleteSubDepartment(long subDepartmentId);
    CompletableFuture<OperationResult<Integer>> deleteAllSubDepartments();
    CompletableFuture<OperationResult<Integer>> deleteSubDepartmentsByMacroDepartment(long macroDepartmentId);

    // Query Operations
    CompletableFuture<SubDepartment> getSubDepartmentById(long subDepartmentId);
    CompletableFuture<List<SubDepartment>> getAllSubDepartments();
    CompletableFuture<List<SubDepartment>> getSubDepartmentsByMacroDepartment(long macroDepartmentId);
    CompletableFuture<SubDepartment> getSubDepartmentByNameAndMacroDepartment(long macroDepartmentId, String name);

    // Validation
    OperationResult<Void> validateSubDepartment(SubDepartment subDepartment);
    CompletableFuture<Boolean> subDepartmentExists(long macroDepartmentId, String name);

    // Statistics
    CompletableFuture<Integer> getSubDepartmentsCount();
    CompletableFuture<Integer> getSubDepartmentsCountByMacroDepartment(long macroDepartmentId);
}
