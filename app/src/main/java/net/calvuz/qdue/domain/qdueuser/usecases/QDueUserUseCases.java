package net.calvuz.qdue.domain.qdueuser.usecases;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.domain.qdueuser.repositories.QDueUserRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * QDueUser Use Cases - Clean Architecture Business Logic Layer
 *
 * <p>Domain use cases for QDueUser management following clean architecture principles.
 * Each use case encapsulates specific business logic and coordinates with repository layer.</p>
 */
public class QDueUserUseCases
{
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
        public CompletableFuture<OperationResult<QDueUser>> execute(
                @Nullable String nickname,
                @Nullable String email
        ) {
            return CompletableFuture.supplyAsync( () -> {
                try {
                    String _id = UUID.randomUUID().toString();
                    String _nickname = nickname == null ? "" : nickname;
                    String _email = email == null ? "" : email;

                    // Create user object with business validation
                    QDueUser newUser = QDueUser.builder()
                            .id( _id )
                            .nickname( _nickname )
                            .email( _email )
                            .build();

                    // Validate email format if provided (empty is valid)
                    if (!newUser.isEmailValid()) {
                        Log.e( TAG, "Invalid email format: " + _email );
                        return null; // Will be handled as validation failure
                    }

                    return newUser;
                } catch (Exception e) {
                    Log.e( TAG, "Failed to validate QDueUser", e );
                    return null; // Will be handled as validation failure
                }
            } ).thenCompose( validatedUser -> {
                if (validatedUser == null) {
                    return CompletableFuture.completedFuture(
                            OperationResult.failure(
                                    "Invalid email format: " + email,
                                    OperationResult.OperationType.VALIDATION
                            )
                    );
                }
                return mQDueUserRepository.createUser( validatedUser );
            } );
        }
    }

    /**
     * ReadQDueUserUseCase - Handle user retrieval operations.
     */
    public class ReadQDueUserUseCase {

        /**
         * Get user by ID with business logic.
         *
         * @param userId User ID to retrieve
         * @return CompletableFuture with user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(
                @NonNull String userId
        ) {
            return mQDueUserRepository.readUser( userId );
        }

        /**
         * Get user by email with business logic.
         *
         * @param email Email to search for
         * @return CompletableFuture with user result
         */
        public CompletableFuture<OperationResult<QDueUser>> executeByEmail(
                @NonNull String email
        ) {
            if (email.trim().isEmpty()) {
                return CompletableFuture.completedFuture(
                        OperationResult.failure(
                                "Email cannot be empty for search",
                                OperationResult.OperationType.VALIDATION
                        )
                );
            }
            return mQDueUserRepository.readUserByEmail( email.trim() );
        }

        /**
         * Get primary user (typically first user created).
         *
         * @return CompletableFuture with primary user result
         */
        public CompletableFuture<OperationResult<QDueUser>> execute(
        ) {
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
        public CompletableFuture<OperationResult<QDueUser>> execute(
                @NonNull String userId,
                @NonNull String nickname,
                @NonNull String email,
                @NonNull Long createdAt
        ) {
            return CompletableFuture.supplyAsync( () -> {
                try {
                    // Create updated user object
                    QDueUser updatedUser = QDueUser.builder()
                            .id( userId )
                            .nickname( nickname )
                            .email( email )
                            .createdAt( createdAt )
                            .updatedAt( System.currentTimeMillis() )
                            .build();

                    // Validate email format if provided
                    if (!updatedUser.isEmailValid()) {
                        Log.e( TAG, "Invalid email format: " + email );
                        return null; // Will be handled as validation failure
                    }
                    return updatedUser;
                } catch (Exception e) {
                    Log.e( TAG, "Failed to validate user update", e );
                    return null; // Will be handled as validation failure
                }
            } ).thenCompose( validatedUser -> {
                if (validatedUser == null) {
                    return CompletableFuture.completedFuture(
                            OperationResult.failure(
                                    "Invalid email format: " + email,
                                    OperationResult.OperationType.VALIDATION
                            )
                    );
                }
                return mQDueUserRepository.updateUser( validatedUser );
            } );
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
                @NonNull String email
        ) {
            // Ensure strings are not null
            String safeNickname = nickname.trim();
            String safeEmail = email.trim();

            // Use create use case for onboarding (same business logic)
            return new CreateQDueUserUseCase().execute( safeNickname, safeEmail )
                    .thenApply( result -> {
                        if (result.isSuccess()) {
                            assert result.getData() != null;
                            QDueUser onboardedUser = result.getData();

                            // Return success with onboarding-specific message
                            return OperationResult.success(
                                    onboardedUser,
                                    "OnboardingUseCase completed successfully",
                                    OperationResult.OperationType.CREATE
                            );
                        }
                        return result;
                    } );
        }
    }

    /**
     * IsOnboardingNeededUseCase - Handle minimal onboarding flow.
     */
    public class IsOnboardingNeededUseCase
    {
        /**
         * Check if onboarding is needed (no users exist).
         *
         * @return CompletableFuture with true if onboarding needed
         */
        public CompletableFuture<OperationResult<Boolean>> execute()
        {
            return mQDueUserRepository.getUsersCount()
                    .thenApply( countResult -> {
                        if (countResult.isSuccess()) {
                            assert countResult.getData() != null;

                            boolean needsOnboarding = countResult.getData() == 0;
                            return OperationResult.success( needsOnboarding, OperationResult.OperationType.READ );
                        }
                        return OperationResult.failure(
                                "Failed to check onboarding status: " + countResult.getErrorMessage(),
                                OperationResult.OperationType.READ
                        );
                    } );
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
    public ReadQDueUserUseCase getReadUserUseCase() {
        return new ReadQDueUserUseCase();
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

    /**
     * Get is onboarding needed use case instance.
     */
    @NonNull
    public IsOnboardingNeededUseCase getIsOnboardingNeededUseCase() { return new IsOnboardingNeededUseCase(); }
}