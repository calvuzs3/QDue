package net.calvuz.qdue.core.services;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.user.data.entities.User;

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

// ==================== USER SERVICE ====================

/**
 * Centralized service for ALL User operations
 * Ensures consistent backup, validation, and business logic
 * ✅ REFACTORED: UserService - Complete OperationResult compliance
 */
public interface UserService {

    // CRUD Operations
    CompletableFuture<OperationResult<User>> createUser(User user);
    CompletableFuture<OperationResult<User>> updateUser(User user);
    CompletableFuture<OperationResult<String>> deleteUser(long userId);
    CompletableFuture<OperationResult<Integer>> deleteAllUsers();

    // ✅ Query Operations - ALL converted to OperationResult
    CompletableFuture<OperationResult<User>> getUserById(long userId);
    CompletableFuture<OperationResult<User>> getUserByEmail(String email);
    CompletableFuture<OperationResult<User>> getUserByGoogleId(String googleId);
    CompletableFuture<OperationResult<User>> getUserByEmployeeId(String employeeId);
    CompletableFuture<OperationResult<User>> getActiveUser();
    CompletableFuture<OperationResult<List<User>>> getAllUsers();

    // Authentication Operations
    CompletableFuture<OperationResult<User>> authenticateUser(String email, String provider);
    CompletableFuture<OperationResult<User>> registerUser(User user);
    CompletableFuture<OperationResult<Void>> logoutUser(long userId);
    CompletableFuture<OperationResult<Void>> setActiveUser(long userId);

    // Profile Operations
    CompletableFuture<OperationResult<User>> updateProfile(User user);
    CompletableFuture<OperationResult<List<User>>> getIncompleteProfiles();
    CompletableFuture<OperationResult<Void>> completeProfile(long userId);

    // Validation
    OperationResult<Void> validateUser(User user);
    CompletableFuture<OperationResult<Boolean>> userExists(String email);
    CompletableFuture<OperationResult<Boolean>> employeeIdExists(String employeeId);

    // ✅ Statistics - ALL converted to OperationResult
    CompletableFuture<OperationResult<Integer>> getUsersCount();
    CompletableFuture<OperationResult<List<User>>> getUsersByAuthProvider(String provider);
}
