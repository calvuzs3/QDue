package net.calvuz.qdue.domain.qdueuser.repositories;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * QDueUserRepository - Clean Architecture Domain Repository Interface
 *
 * <p>Domain layer repository interface for QDueUser entity following clean architecture principles.
 * Provides abstraction for data persistence operations with consistent error handling using OperationResult pattern.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Domain Layer</strong>: Interface only, no implementation details</li>
 *   <li><strong>Dependency Direction</strong>: Data layer depends on this interface</li>
 *   <li><strong>Business Focused</strong>: Operations reflect business needs, not database structure</li>
 *   <li><strong>Consistent Results</strong>: All operations return OperationResult for uniform error handling</li>
 * </ul>
 *
 * <h3>Repository Operations:</h3>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Create, Read, Update, Delete with validation</li>
 *   <li><strong>Business Queries</strong>: Find by email, check existence, get active user</li>
 *   <li><strong>Onboarding Support</strong>: Minimal user creation for onboarding flow</li>
 *   <li><strong>Profile Management</strong>: Complete profile updates</li>
 * </ul>
 *
 * <h3>Implementation Notes:</h3>
 * <ul>
 *   <li>All operations are asynchronous using CompletableFuture</li>
 *   <li>ID auto-generation starts from 1L</li>
 *   <li>Email validation performed at domain level</li>
 *   <li>Empty strings used as defaults for optional fields</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Domain Repository
 * @since Clean Architecture Phase 3
 */
public interface QDueUserRepository {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create new QDueUser with auto-generated ID.
     *
     * @param qDueUser User to create (ID will be auto-generated)
     * @return CompletableFuture with created user including generated ID
     */
    CompletableFuture<OperationResult<QDueUser>> createUser(@NonNull QDueUser qDueUser);

    /**
     * Get QDueUser by ID.
     *
     * @param userId User ID to find
     * @return CompletableFuture with found user or failure if not exists
     */
    CompletableFuture<OperationResult<QDueUser>> getUserById(@NonNull Long userId);

    /**
     * Update existing QDueUser (complete update).
     *
     * @param qDueUser User with updated data (must have valid ID)
     * @return CompletableFuture with updated user
     */
    CompletableFuture<OperationResult<QDueUser>> updateUser(@NonNull QDueUser qDueUser);

    /**
     * Delete QDueUser by ID.
     *
     * @param userId User ID to delete
     * @return CompletableFuture with success message or failure
     */
    CompletableFuture<OperationResult<String>> deleteUser(@NonNull Long userId);

    /**
     * Delete all QDueUsers (for testing/reset purposes).
     *
     * @return CompletableFuture with count of deleted users
     */
    CompletableFuture<OperationResult<Integer>> deleteAllUsers();

    // ==================== BUSINESS QUERIES ====================

    /**
     * Find QDueUser by email address.
     *
     * @param email Email to search for
     * @return CompletableFuture with found user or failure if not exists
     */
    CompletableFuture<OperationResult<QDueUser>> getUserByEmail(@NonNull String email);

    /**
     * Get all QDueUsers (for admin/management purposes).
     *
     * @return CompletableFuture with list of all users
     */
    CompletableFuture<OperationResult<List<QDueUser>>> getAllUsers();

    /**
     * Get the primary/active user (typically user with ID = 1L).
     *
     * @return CompletableFuture with primary user or failure if none exists
     */
    CompletableFuture<OperationResult<QDueUser>> getPrimaryUser();

    // ==================== EXISTENCE CHECKS ====================

    /**
     * Check if user exists by ID.
     *
     * @param userId User ID to check
     * @return CompletableFuture with true if user exists
     */
    CompletableFuture<OperationResult<Boolean>> userExists(@NonNull Long userId);

    /**
     * Check if user exists by email.
     *
     * @param email Email to check
     * @return CompletableFuture with true if user with email exists
     */
    CompletableFuture<OperationResult<Boolean>> userExistsByEmail(@NonNull String email);

    // ==================== STATISTICS ====================

    /**
     * Get total count of QDueUsers.
     *
     * @return CompletableFuture with user count
     */
    CompletableFuture<OperationResult<Integer>> getUsersCount();

    /**
     * Get count of users with complete profiles (both nickname and email).
     *
     * @return CompletableFuture with complete profile count
     */
    CompletableFuture<OperationResult<Integer>> getCompleteProfilesCount();

    /**
     * Get users with incomplete profiles for onboarding completion.
     *
     * @return CompletableFuture with list of users needing profile completion
     */
    CompletableFuture<OperationResult<List<QDueUser>>> getIncompleteProfiles();
}