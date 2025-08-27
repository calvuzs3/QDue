package net.calvuz.qdue.core.services;

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
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Application Service
 * @since Clean Architecture Phase 3
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
     * Get QDueUser by ID.
     *
     * @param userId User ID to retrieve
     * @return CompletableFuture with found user or failure if not exists
     */
    CompletableFuture<OperationResult<QDueUser>> getUserById(@NonNull Long userId);

    /**
     * Update existing QDueUser with complete profile replacement.
     *
     * @param userId   User ID to update
     * @param nickname New nickname (can be empty)
     * @param email    New email (can be empty, validated if present)
     * @return CompletableFuture with updated user
     */
    CompletableFuture<OperationResult<QDueUser>> updateUser(@NonNull Long userId, @NonNull String nickname, @NonNull String email);

    /**
     * Delete QDueUser by ID.
     *
     * @param userId User ID to delete
     * @return CompletableFuture with success message or failure
     */
    CompletableFuture<OperationResult<String>> deleteUser(@NonNull Long userId);

    // ==================== BUSINESS QUERIES ====================

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

    /**
     * Complete user profile after initial onboarding.
     *
     * @param userId   User ID to complete profile for
     * @param nickname Complete nickname
     * @param email    Complete email address
     * @return CompletableFuture with completed user profile
     */
    CompletableFuture<OperationResult<QDueUser>> completeUserProfile(@NonNull Long userId, @NonNull String nickname, @NonNull String email);

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validate user data without persisting.
     * Useful for real-time form validation.
     *
     * @param nickname User nickname to validate
     * @param email    User email to validate (format checked if not empty)
     * @return OperationResult with validation status and errors
     */
    OperationResult<Void> validateUserData(@NonNull String nickname, @NonNull String email);

    /**
     * Check if email format is valid (if not empty).
     *
     * @param email Email to validate
     * @return OperationResult with validation status
     */
    OperationResult<Boolean> isEmailValid(@NonNull String email);

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
     * @param email Email to check (must not be empty)
     * @return CompletableFuture with true if user with email exists
     */
    CompletableFuture<OperationResult<Boolean>> userExistsByEmail(@NonNull String email);

    // ==================== STATISTICS AND ANALYTICS ====================

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
     * Get users with incomplete profiles for UI notifications.
     *
     * @return CompletableFuture with list of users needing profile completion
     */
    CompletableFuture<OperationResult<List<QDueUser>>> getIncompleteProfiles();

    /**
     * Get profile completion percentage.
     *
     * @return CompletableFuture with percentage (0-100) of complete profiles
     */
    CompletableFuture<OperationResult<Integer>> getProfileCompletionPercentage();

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