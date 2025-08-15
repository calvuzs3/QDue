package net.calvuz.qdue.domain.calendar.usecases;

import static net.calvuz.qdue.domain.common.DomainLibrary.logDebug;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

/**
 * UseCaseFactory - Clean Architecture Use Case Factory
 *
 * <p>Centralized factory for creating and managing calendar use case instances.
 * Provides dependency injection, caching, and lifecycle management for all
 * calendar-related business operations following clean architecture principles.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI for all use cases</li>
 *   <li><strong>Instance Caching</strong>: Lazy initialization with singleton pattern</li>
 *   <li><strong>Clean Architecture</strong>: No infrastructure dependencies</li>
 *   <li><strong>Thread Safe</strong>: Safe for concurrent access</li>
 * </ul>
 *
 * <h3>Available Use Cases:</h3>
 * <ul>
 *   <li><strong>GenerateUserScheduleUseCase</strong>: Individual user schedule generation</li>
 *   <li><strong>GenerateTeamScheduleUseCase</strong>: Team coordination and management</li>
 *   <li><strong>ApplyShiftExceptionsUseCase</strong>: Exception handling workflow</li>
 *   <li><strong>GetScheduleStatsUseCase</strong>: Analytics and validation</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Create factory with repository
 * UseCaseFactory factory = new UseCaseFactory(workScheduleRepository);
 *
 * // Get use cases (cached instances)
 * GenerateUserScheduleUseCase userScheduleUC = factory.getUserScheduleUseCase();
 * GenerateTeamScheduleUseCase teamScheduleUC = factory.getTeamScheduleUseCase();
 *
 * // Execute operations
 * CompletableFuture&lt;OperationResult&lt;WorkScheduleDay&gt;&gt; result =
 *     userScheduleUC.executeForDate(userId, date);
 * </pre>
 *
 * @author QDue Development Team
 * @version 2.0.0 - Clean Architecture Implementation
 * @since Clean Architecture Migration
 */
public class UseCaseFactory {

    private static final String TAG = "UseCaseFactory";

    // ==================== DEPENDENCIES ====================

    private final WorkScheduleRepository mWorkScheduleRepository;

    // ==================== CACHED INSTANCES ====================

    // Core use cases (lazy initialization)
    private volatile GenerateUserScheduleUseCase mGenerateUserScheduleUseCase;
    private volatile GenerateTeamScheduleUseCase mGenerateTeamScheduleUseCase;
    private volatile ApplyShiftExceptionsUseCase mApplyShiftExceptionsUseCase;
    private volatile GetScheduleStatsUseCase mGetScheduleStatsUseCase;

    // Lock objects for thread-safe lazy initialization
    private final Object mUserScheduleUseCaseLock = new Object();
    private final Object mTeamScheduleUseCaseLock = new Object();
    private final Object mShiftExceptionsUseCaseLock = new Object();
    private final Object mScheduleStatsUseCaseLock = new Object();

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for dependency injection.
     *
     * @param workScheduleRepository Repository for work schedule operations
     */
    public UseCaseFactory(@NonNull WorkScheduleRepository workScheduleRepository) {
        this.mWorkScheduleRepository = workScheduleRepository;
        logDebug("UseCaseFactory created with repository: " +
                workScheduleRepository.getClass().getSimpleName());
    }

    // ==================== USE CASE PROVIDERS ====================

    /**
     * Get GenerateUserScheduleUseCase for individual user schedule operations.
     *
     * <p>Provides comprehensive user schedule generation with recurrence rules,
     * exception handling, team assignments, and business rule validation.</p>
     *
     * @return GenerateUserScheduleUseCase instance (cached)
     */
    @NonNull
    public GenerateUserScheduleUseCase getUserScheduleUseCase() {
        if (mGenerateUserScheduleUseCase == null) {
            synchronized (mUserScheduleUseCaseLock) {
                if (mGenerateUserScheduleUseCase == null) {
                    mGenerateUserScheduleUseCase = new GenerateUserScheduleUseCase(mWorkScheduleRepository);
                    logDebug("Created GenerateUserScheduleUseCase instance");
                }
            }
        }
        return mGenerateUserScheduleUseCase;
    }

    /**
     * Get GenerateTeamScheduleUseCase for team coordination operations.
     *
     * <p>Provides team-wide schedule generation with coverage analysis,
     * resource optimization, conflict detection, and multi-team coordination.</p>
     *
     * @return GenerateTeamScheduleUseCase instance (cached)
     */
    @NonNull
    public GenerateTeamScheduleUseCase getTeamScheduleUseCase() {
        if (mGenerateTeamScheduleUseCase == null) {
            synchronized (mTeamScheduleUseCaseLock) {
                if (mGenerateTeamScheduleUseCase == null) {
                    mGenerateTeamScheduleUseCase = new GenerateTeamScheduleUseCase(mWorkScheduleRepository);
                    logDebug("Created GenerateTeamScheduleUseCase instance");
                }
            }
        }
        return mGenerateTeamScheduleUseCase;
    }

    /**
     * Get ApplyShiftExceptionsUseCase for exception handling workflow.
     *
     * <p>Provides comprehensive shift exception processing with approval
     * workflows, conflict resolution, and business rule validation.</p>
     *
     * @return ApplyShiftExceptionsUseCase instance (cached)
     */
    @NonNull
    public ApplyShiftExceptionsUseCase getShiftExceptionsUseCase() {
        if (mApplyShiftExceptionsUseCase == null) {
            synchronized (mShiftExceptionsUseCaseLock) {
                if (mApplyShiftExceptionsUseCase == null) {
                    mApplyShiftExceptionsUseCase = new ApplyShiftExceptionsUseCase(mWorkScheduleRepository);
                    logDebug("Created ApplyShiftExceptionsUseCase instance");
                }
            }
        }
        return mApplyShiftExceptionsUseCase;
    }

    /**
     * Get GetScheduleStatsUseCase for analytics and reporting.
     *
     * <p>Provides comprehensive schedule analytics, statistics generation,
     * validation capabilities, and reporting for management and planning.</p>
     *
     * @return GetScheduleStatsUseCase instance (cached)
     */
    @NonNull
    public GetScheduleStatsUseCase getScheduleStatsUseCase() {
        if (mGetScheduleStatsUseCase == null) {
            synchronized (mScheduleStatsUseCaseLock) {
                if (mGetScheduleStatsUseCase == null) {
                    mGetScheduleStatsUseCase = new GetScheduleStatsUseCase(mWorkScheduleRepository);
                    logDebug("Created GetScheduleStatsUseCase instance");
                }
            }
        }
        return mGetScheduleStatsUseCase;
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Get all available use cases as a bundle.
     *
     * <p>Useful for scenarios where multiple use cases are needed together.</p>
     *
     * @return UseCaseBundle with all use cases
     */
    @NonNull
    public UseCaseBundle getAllUseCases() {
        return new UseCaseBundle(
                getUserScheduleUseCase(),
                getTeamScheduleUseCase(),
                getShiftExceptionsUseCase(),
                getScheduleStatsUseCase()
        );
    }

    /**
     * Check if factory is properly initialized.
     *
     * @return true if factory has valid repository
     */
    public boolean isInitialized() {
        return mWorkScheduleRepository != null;
    }

    /**
     * Get information about the factory and its dependencies.
     *
     * @return Factory information string
     */
    @NonNull
    public String getFactoryInfo() {
        StringBuilder info = new StringBuilder();
        info.append("UseCaseFactory Information:\n");
        info.append("Repository: ").append(mWorkScheduleRepository.getClass().getSimpleName()).append("\n");
        info.append("Initialized Use Cases:\n");
        info.append("- GenerateUserScheduleUseCase: ").append(mGenerateUserScheduleUseCase != null).append("\n");
        info.append("- GenerateTeamScheduleUseCase: ").append(mGenerateTeamScheduleUseCase != null).append("\n");
        info.append("- ApplyShiftExceptionsUseCase: ").append(mApplyShiftExceptionsUseCase != null).append("\n");
        info.append("- GetScheduleStatsUseCase: ").append(mGetScheduleStatsUseCase != null).append("\n");
        return info.toString();
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Clear all cached use case instances.
     *
     * <p>Forces recreation of all use cases on next access. Use when
     * underlying dependencies have changed or for memory cleanup.</p>
     */
    public void clearCache() {
        synchronized (mUserScheduleUseCaseLock) {
            mGenerateUserScheduleUseCase = null;
        }
        synchronized (mTeamScheduleUseCaseLock) {
            mGenerateTeamScheduleUseCase = null;
        }
        synchronized (mShiftExceptionsUseCaseLock) {
            mApplyShiftExceptionsUseCase = null;
        }
        synchronized (mScheduleStatsUseCaseLock) {
            mGetScheduleStatsUseCase = null;
        }
        logDebug("Cleared all cached use case instances");
    }

    /**
     * Cleanup factory resources.
     *
     * <p>Called during application shutdown or when factory is no longer needed.</p>
     */
    public void cleanup() {
        logDebug("Cleaning up UseCaseFactory");
        clearCache();
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Create factory builder for advanced configuration.
     *
     * @return UseCaseFactoryBuilder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating UseCaseFactory with advanced configuration.
     */
    public static class Builder {
        private WorkScheduleRepository mRepository;

        /**
         * Set the work schedule repository.
         *
         * @param repository WorkScheduleRepository implementation
         * @return Builder instance for chaining
         */
        @NonNull
        public Builder workScheduleRepository(@NonNull WorkScheduleRepository repository) {
            this.mRepository = repository;
            return this;
        }

        /**
         * Build the UseCaseFactory instance.
         *
         * @return Configured UseCaseFactory
         * @throws IllegalStateException if repository is not set
         */
        @NonNull
        public UseCaseFactory build() {
            if (mRepository == null) {
                throw new IllegalStateException("WorkScheduleRepository must be set");
            }
            return new UseCaseFactory(mRepository);
        }
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Bundle of all available use cases for convenient access.
     */
    public static class UseCaseBundle {
        @NonNull public final GenerateUserScheduleUseCase userScheduleUseCase;
        @NonNull public final GenerateTeamScheduleUseCase teamScheduleUseCase;
        @NonNull public final ApplyShiftExceptionsUseCase shiftExceptionsUseCase;
        @NonNull public final GetScheduleStatsUseCase scheduleStatsUseCase;

        public UseCaseBundle(@NonNull GenerateUserScheduleUseCase userScheduleUseCase,
                             @NonNull GenerateTeamScheduleUseCase teamScheduleUseCase,
                             @NonNull ApplyShiftExceptionsUseCase shiftExceptionsUseCase,
                             @NonNull GetScheduleStatsUseCase scheduleStatsUseCase) {
            this.userScheduleUseCase = userScheduleUseCase;
            this.teamScheduleUseCase = teamScheduleUseCase;
            this.shiftExceptionsUseCase = shiftExceptionsUseCase;
            this.scheduleStatsUseCase = scheduleStatsUseCase;
        }
    }
}