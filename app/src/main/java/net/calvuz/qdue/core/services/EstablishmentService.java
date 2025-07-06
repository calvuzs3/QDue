package net.calvuz.qdue.core.services;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;

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

// ==================== ESTABLISHMENT SERVICE ====================

/**
 * Centralized service for ALL Establishment operations
 * Manages company/establishment data with automatic backup
 * ✅ REFACTORED: EstablishmentService - Complete OperationResult compliance
 */
public interface EstablishmentService {

    // CRUD Operations
    CompletableFuture<OperationResult<Establishment>> createEstablishment(Establishment establishment);
    CompletableFuture<OperationResult<List<Establishment>>> createEstablishments(List<Establishment> establishments);
    CompletableFuture<OperationResult<Establishment>> updateEstablishment(Establishment establishment);
    CompletableFuture<OperationResult<String>> deleteEstablishment(long establishmentId);
    CompletableFuture<OperationResult<Integer>> deleteAllEstablishments();

    // ✅ Query Operations - ALL converted to OperationResult
    CompletableFuture<OperationResult<Establishment>> getEstablishmentById(long establishmentId);
    CompletableFuture<OperationResult<Establishment>> getEstablishmentByName(String name);
    CompletableFuture<OperationResult<Establishment>> getEstablishmentByCode(String code);
    CompletableFuture<OperationResult<List<Establishment>>> getAllEstablishments();

    // Validation
    OperationResult<Void> validateEstablishment(Establishment establishment);
    CompletableFuture<OperationResult<Boolean>> establishmentExists(String name);
    CompletableFuture<OperationResult<Boolean>> establishmentCodeExists(String code);

    // ✅ Statistics - ALL converted to OperationResult
    CompletableFuture<OperationResult<Integer>> getEstablishmentsCount();

    // ✅ Organization Hierarchy - ALL converted to OperationResult
    CompletableFuture<OperationResult<List<MacroDepartment>>> getMacroDepartments(long establishmentId);
    CompletableFuture<OperationResult<Integer>> deleteEstablishmentWithDependencies(long establishmentId);
}
