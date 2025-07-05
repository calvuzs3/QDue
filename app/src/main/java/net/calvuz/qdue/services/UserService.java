package net.calvuz.qdue.services;

import net.calvuz.qdue.services.models.OperationResult;
import net.calvuz.qdue.user.data.entities.User;

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

// ==================== USER SERVICE ====================

/**
 * Centralized service for ALL User operations
 * Ensures consistent backup, validation, and business logic
 */
public interface UserService {

    // CRUD Operations
    CompletableFuture<OperationResult<User>> createUser(User user);
    CompletableFuture<OperationResult<User>> updateUser(User user);
    CompletableFuture<OperationResult<String>> deleteUser(long userId);
    CompletableFuture<OperationResult<Integer>> deleteAllUsers();

    // Query Operations
    CompletableFuture<User> getUserById(long userId);
    CompletableFuture<User> getUserByEmail(String email);
    CompletableFuture<User> getUserByGoogleId(String googleId);
    CompletableFuture<User> getUserByEmployeeId(String employeeId);
    CompletableFuture<User> getActiveUser();
    CompletableFuture<List<User>> getAllUsers();

    // Authentication Operations
    CompletableFuture<OperationResult<User>> authenticateUser(String email, String provider);
    CompletableFuture<OperationResult<User>> registerUser(User user);
    CompletableFuture<OperationResult<Void>> logoutUser(long userId);
    CompletableFuture<OperationResult<Void>> setActiveUser(long userId);

    // Profile Operations
    CompletableFuture<OperationResult<User>> updateProfile(User user);
    CompletableFuture<List<User>> getIncompleteProfiles();
    CompletableFuture<OperationResult<Void>> completeProfile(long userId);

    // Validation
    OperationResult<Void> validateUser(User user);
    CompletableFuture<Boolean> userExists(String email);
    CompletableFuture<Boolean> employeeIdExists(String employeeId);

    // Statistics
    CompletableFuture<Integer> getUsersCount();
    CompletableFuture<List<User>> getUsersByAuthProvider(String provider);
}


