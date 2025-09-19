package net.calvuz.qdue.data.services;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * QDueUserService - Core Service Interface for Simplified User Management
 *
 * <p>Application service interface providing business-level APIs for QDueUser operations
 * following clean architecture principles and established service patterns.</p>
 *
 * <h3>Service Layer Responsibilities:</h3>
 * <ul>
 *   <li><strong>Business Orchestration</strong>: Coordinates use cases and business workflows</li>
 *   <li><strong>Transaction Boundaries</strong>: Manages complex multi-step operations</li>
 *   <li><strong>Service Integration</strong>: Integrates with other application services</li>
 *   <li><strong>API Facade</strong>: Provides stable interface for UI and external clients</li>
 * </ul>
 *
 * <h3>Clean Architecture Position:</h3>
 * <ul>
 *   <li><strong>Layer</strong>: Application Service (between UI and Domain)</li>
 *   <li><strong>Dependencies</strong>: Domain use cases via dependency injection</li>
 *   <li><strong>Clients</strong>: UI components, fragments, activities</li>
 *   <li><strong>Abstraction</strong>: Hides domain complexity from UI layer</li>
 * </ul>
 *
 * <h3>Business Operations:</h3>
 * <ul>
 *   <li><strong>Onboarding</strong>: Minimal user onboarding with optional fields</li>
 *   <li><strong>Profile Management</strong>: User profile CRUD operations</li>
 *   <li><strong>Validation</strong>: Business rule validation and error handling</li>
 *   <li><strong>Statistics</strong>: User count and profile completion metrics</li>
 * </ul>
 */
public interface QDueUserService {

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create new QDueUser with business validation.
     *
     * @param nickname User nickname (can be empty for minimal onboarding)
     * @param email    User email (can be empty, validated if present)
     * @return CompletableFuture with created user including generated ID
     */
    CompletableFuture<OperationResult<QDueUser>> createUser(@NonNull String nickname, @NonNull String email);

    /**
     * Update existing QDueUser with complete profile replacement.
     *
     * @param userId   User ID to update
     * @param nickname New nickname (can be empty)
     * @param email    New email (can be empty, validated if present)
     * @return CompletableFuture with updated user
     */
    CompletableFuture<OperationResult<QDueUser>> updateUser(@NonNull String userId, @NonNull String nickname, @NonNull String email);

    /**
     * Delete QDueUser by ID.
     *
     * @param userId User ID to delete
     * @return CompletableFuture with success message or failure
     */
    CompletableFuture<OperationResult<String>> deleteUser(@NonNull String userId);

    // ==================== BUSINESS QUERIES ====================

    /**
     * Get QDueUser by ID.
     *
     * @param userId User ID to retrieve
     * @return CompletableFuture with found user or failure if not exists
     */
    CompletableFuture<OperationResult<QDueUser>> getUserById(@NonNull String userId);

    /**
     * Find QDueUser by email address.
     *
     * @param email Email to search for (must not be empty)
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
     * Get the primary user (first user created, typically for single-user scenarios).
     *
     * @return CompletableFuture with primary user or failure if none exists
     */
    CompletableFuture<OperationResult<QDueUser>> getPrimaryUser();

    // ==================== ONBOARDING OPERATIONS ====================

    /**
     * Execute minimal onboarding flow for new user.
     * Designed for single-step onboarding with optional fields.
     *
     * @param nickname User nickname (optional, empty string if not provided)
     * @param email    User email (optional, empty string if not provided)
     * @return CompletableFuture with onboarded user result
     */
    CompletableFuture<OperationResult<QDueUser>> onboardUser(@NonNull String nickname, @NonNull String email);

    /**
     * Check if onboarding is needed (no users exist).
     *
     * @return CompletableFuture with true if onboarding is needed
     */
    CompletableFuture<OperationResult<Boolean>> isOnboardingNeeded();


    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Check if email format is valid (if not empty).
     *
     * @param email Email to validate
     * @return OperationResult with validation status
     */
    OperationResult<Boolean> isEmailValid(@NonNull String email);

    // ==================== MAINTENANCE OPERATIONS ====================

    /**
     * Delete all QDueUsers (for testing/reset purposes).
     * Use with extreme caution in production.
     *
     * @return CompletableFuture with count of deleted users
     */
    CompletableFuture<OperationResult<Integer>> deleteAllUsers();

    /**
     * Get service health status for monitoring.
     *
     * @return CompletableFuture with service status information
     */
    CompletableFuture<OperationResult<String>> getServiceStatus();
}