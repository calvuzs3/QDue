package net.calvuz.qdue.domain.qdueuser.usecases;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.concurrent.CompletableFuture;

/**
 * QDueUser Use Cases - Clean Architecture Business Logic Layer
 *
 * <p>Domain use cases for QDueUser management following clean architecture principles.
 * Each use case encapsulates specific business logic and coordinates with repository layer.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Clean Architecture Use Cases
 * @since Clean Architecture Phase 3
 */
public class QDueUserUseCases {

    private static final String TAG = "QDueUserUseCases";

    // ==================== DEPENDENCIES ====================

    private final QDueUserRepository mQDueUserRepository;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param qDueUserRepository Repository for QDueUser operations
     */
    public QDueUserUseCases(@NonNull QDueUserRepository qDueUserRepository) {
        this.mQDueUserRepository = qDueUserRepository;
        Log.d(TAG, "QDueUserUseCases initialized with repository");
    }

    // ==================== USE CASE CLASSES ====================

    /**
     * CreateQDueUserUseCase - Handle user creation with business validation.
     */
    public class CreateQDueUserUseCase {

        /**
         * Execute user creation with business rules validation.
         *
         * @param nickname User nickname (can be empty)
         * @param email    User email (can be empty, validated if present)
         * @return CompletableFuture with created user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(@NonNull String nickname, @NonNull String email) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Log.d(TAG, "Creating QDueUser with nickname: '" + nickname + "', email: '" + email + "'");

                    // Create user object with business validation
                    QDueUser newUser = new QDueUser(nickname, email);

                    // Validate email format if provided
                    if (!newUser.isEmailValid()) {
                        Log.e(TAG, "❌ Invalid email format: " + email);
                        return null; // Will be handled as validation failure
                    }

                    Log.d(TAG, "✅ User validation passed, proceeding with creation");
                    return newUser;

                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to validate QDueUser", e);
                    return null; // Will be handled as validation failure
                }
            }).thenCompose(validatedUser -> {
                if (validatedUser == null) {
                    return CompletableFuture.completedFuture(
                            OperationResult.failure(
                                    "Invalid email format: " + email,
                                    OperationResult.OperationType.VALIDATION
                            )
                    );
                }
                return mQDueUserRepository.createUser(validatedUser);
            });
        }
    }

    /**
     * GetQDueUserUseCase - Handle user retrieval operations.
     */
    public class GetQDueUserUseCase {

        /**
         * Get user by ID with business logic.
         *
         * @param userId User ID to retrieve
         * @return CompletableFuture with user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(@NonNull Long userId) {
            Log.d(TAG, "Getting QDueUser by ID: " + userId);
            return mQDueUserRepository.getUserById(userId);
        }

        /**
         * Get user by email with business logic.
         *
         * @param email Email to search for
         * @return CompletableFuture with user result
         */
        public CompletableFuture<OperationResult<QDueUser>> executeByEmail(@NonNull String email) {
            if (email.trim().isEmpty()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Email cannot be empty for search",
                                OperationResult.OperationType.VALIDATION
                        )
                );
            }

            Log.d(TAG, "Getting QDueUser by email: " + email);
            return mQDueUserRepository.getUserByEmail(email.trim());
        }

        /**
         * Get primary user (typically first user created).
         *
         * @return CompletableFuture with primary user result
         */
        public CompletableFuture<OperationResult<QDueUser>> getPrimaryUser() {
            Log.d(TAG, "Getting primary QDueUser");
            return mQDueUserRepository.getPrimaryUser();
        }
    }

    /**
     * UpdateQDueUserUseCase - Handle complete user updates.
     */
    public class UpdateQDueUserUseCase {

        /**
         * Execute complete user update with business validation.
         *
         * @param userId   User ID to update
         * @param nickname New nickname (can be empty)
         * @param email    New email (can be empty, validated if present)
         * @return CompletableFuture with updated user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(@NonNull Long userId, @NonNull String nickname, @NonNull String email) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Log.d(TAG, "Updating QDueUser ID: " + userId + " with nickname: '" + nickname + "', email: '" + email + "'");

                    // Create updated user object
                    QDueUser updatedUser = new QDueUser(userId, nickname, email);

                    // Validate email format if provided
                    if (!updatedUser.isEmailValid()) {
                        Log.e(TAG, "❌ Invalid email format: " + email);
                        return null; // Will be handled as validation failure
                    }

                    Log.d(TAG, "✅ Update validation passed");
                    return updatedUser;

                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to validate user update", e);
                    return null; // Will be handled as validation failure
                }
            }).thenCompose(validatedUser -> {
                if (validatedUser == null) {
                    return CompletableFuture.completedFuture(
                            OperationResult.failure(
                                    "Invalid email format: " + email,
                                    OperationResult.OperationType.VALIDATION
                            )
                    );
                }
                return mQDueUserRepository.updateUser(validatedUser);
            });
        }
    }

    /**
     * OnboardingUseCase - Handle minimal onboarding flow.
     */
    public class OnboardingUseCase {

        /**
         * Execute minimal onboarding - create user with optional fields.
         * Designed for single-step onboarding fragment.
         *
         * @param nickname User nickname (optional, empty string if not provided)
         * @param email    User email (optional, empty string if not provided)
         * @return CompletableFuture with onboarded user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(
                @NonNull String nickname,
                @NonNull String email)
        {
            Log.d(TAG, "Starting minimal onboarding for user");

            // Ensure strings are not null
            String safeNickname = nickname.trim();
            String safeEmail = email.trim();

            // Use create use case for onboarding (same business logic)
            return new CreateQDueUserUseCase().execute(safeNickname, safeEmail)
                    .thenApply(result -> {
                        if (result.isSuccess()) {
                            assert result.getData() != null;
                            QDueUser onboardedUser = result.getData();

                            Log.d(TAG, "✅ Onboarding completed for user: " + onboardedUser.getDisplayName());

                            // Return success with onboarding-specific message
                            return OperationResult.success(
                                    onboardedUser,
                                    "User onboarding completed successfully",
                                    OperationResult.OperationType.CREATE
                            );
                        }
                        return result;
                    });
        }

        /**
         * Check if onboarding is needed (no users exist).
         *
         * @return CompletableFuture with true if onboarding needed
         */
        public CompletableFuture<OperationResult<Boolean>> isOnboardingNeeded() {
            return mQDueUserRepository.getUsersCount()
                    .thenApply(countResult -> {
                        if (countResult.isSuccess()) {
                            assert countResult.getData() != null;

                            boolean needsOnboarding = countResult.getData() == 0;
                            Log.d(TAG, "Onboarding needed: " + needsOnboarding + " (users count: " + countResult.getData() + ")");
                            return OperationResult.success(needsOnboarding, OperationResult.OperationType.READ);
                        }
                        return OperationResult.failure(
                                "Failed to check onboarding status: " + countResult.getErrorMessage(),
                                OperationResult.OperationType.READ
                        );
                    });
        }
    }

    // ==================== USE CASE INSTANCE PROVIDERS ====================

    /**
     * Get create user use case instance.
     */
    @NonNull
    public CreateQDueUserUseCase getCreateUserUseCase() {
        return new CreateQDueUserUseCase();
    }

    /**
     * Get user retrieval use case instance.
     */
    @NonNull
    public GetQDueUserUseCase getUserUseCase() {
        return new GetQDueUserUseCase();
    }

    /**
     * Get user update use case instance.
     */
    @NonNull
    public UpdateQDueUserUseCase getUpdateUserUseCase() {
        return new UpdateQDueUserUseCase();
    }

    /**
     * Get onboarding use case instance.
     */
    @NonNull
    public OnboardingUseCase getOnboardingUseCase() {
        return new OnboardingUseCase();
    }
}