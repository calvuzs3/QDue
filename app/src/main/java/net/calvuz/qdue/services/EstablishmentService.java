package net.calvuz.qdue.services;

import net.calvuz.qdue.services.models.OperationResult;
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
 */
public interface EstablishmentService {

    // CRUD Operations
    CompletableFuture<OperationResult<Establishment>> createEstablishment(Establishment establishment);
    CompletableFuture<OperationResult<List<Establishment>>> createEstablishments(List<Establishment> establishments);
    CompletableFuture<OperationResult<Establishment>> updateEstablishment(Establishment establishment);
    CompletableFuture<OperationResult<String>> deleteEstablishment(long establishmentId);
    CompletableFuture<OperationResult<Integer>> deleteAllEstablishments();

    // Query Operations
    CompletableFuture<Establishment> getEstablishmentById(long establishmentId);
    CompletableFuture<Establishment> getEstablishmentByName(String name);
    CompletableFuture<Establishment> getEstablishmentByCode(String code);
    CompletableFuture<List<Establishment>> getAllEstablishments();

    // Validation
    OperationResult<Void> validateEstablishment(Establishment establishment);
    CompletableFuture<Boolean> establishmentExists(String name);
    CompletableFuture<Boolean> establishmentCodeExists(String code);

    // Statistics
    CompletableFuture<Integer> getEstablishmentsCount();

    // Organization Hierarchy
    CompletableFuture<List<MacroDepartment>> getMacroDepartments(long establishmentId);
    CompletableFuture<OperationResult<Integer>> deleteEstablishmentWithDependencies(long establishmentId);
}
