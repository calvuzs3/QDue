package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * CreatePatternAssignmentUseCase - Business Use Case for Pattern Assignment Creation
 *
 * <p>Handles the creation of both QuattroDue standard and custom pattern assignments.
 * Implements business rules for assignment conflicts, team validation, and gap-filling strategy.</p>
 *
 * <h3>Core Business Rules:</h3>
 * <ul>
 *   <li><strong>QuattroDue Assignments</strong>: Validate team exists and has qdue_offset, apply cycle_position</li>
 *   <li><strong>Custom Pattern Assignments</strong>: Validate RecurrenceRule exists and is active</li>
 *   <li><strong>Gap-Filling Strategy</strong>: Close existing overlapping assignments before creating new ones</li>
 *   <li><strong>Date Validation</strong>: Ensure start date is not in the past</li>
 * </ul>
 *
 * <h3>Assignment Types:</h3>
 * <ul>
 *   <li><strong>QuattroDue</strong>: Standard 4-on-2-off pattern with team-based offset and cycle position</li>
 *   <li><strong>Custom Pattern</strong>: User-defined patterns using existing RecurrenceRule</li>
 * </ul>
 *
 * <h3>Conflict Resolution:</h3>
 * <ul>
 *   <li><strong>Overlapping Assignments</strong>: Automatically close (soft delete) existing assignments</li>
 *   <li><strong>Future Assignments</strong>: Cancel future assignments that would conflict</li>
 *   <li><strong>Active Assignments</strong>: End current assignments at new assignment start date</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Pattern Assignment Creation Use Case
 * @since Clean Architecture Phase 2
 */
public class CreatePatternAssignmentUseCase {

    private static final String TAG = "CreatePatternAssignmentUseCase";

    // ==================== REPOSITORIES ====================

    private final TeamRepository mTeamRepository;
    private final RecurrenceRuleRepository mRecurrenceRuleRepository;
    private final UserScheduleAssignmentRepository mUserScheduleAssignmentRepository;

    // ==================== CONSTANTS ====================

    private static final String QUATTRODUE_PATTERN_PREFIX = "QD";
    private static final String CUSTOM_PATTERN_PREFIX = "CP";
    private static final String DEFAULT_CUSTOM_TEAM = "A"; // Default team for custom patterns

    // ==================== CONSTRUCTOR ====================

    public CreatePatternAssignmentUseCase(@NonNull TeamRepository teamRepository,
                                          @NonNull RecurrenceRuleRepository recurrenceRuleRepository,
                                          @NonNull UserScheduleAssignmentRepository userScheduleAssignmentRepository) {
        this.mTeamRepository = teamRepository;
        this.mRecurrenceRuleRepository = recurrenceRuleRepository;
        this.mUserScheduleAssignmentRepository = userScheduleAssignmentRepository;
    }

    // ==================== REPOSITORIES ====================

    @NonNull
    public CompletableFuture<OperationResult<List<Team>>> getTeamsWithQuattroDueOffset() {
        return mTeamRepository.getTeamsWithQuattroDueOffset();
    }

    @NonNull
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getUserActiveAssignments(@NonNull Long userId) {
        return mUserScheduleAssignmentRepository.getUserActiveAssignments(userId);
    }

    // ==================== QUATTRODUE ASSIGNMENTS ====================

    /**
     * Create QuattroDue pattern assignment with team and cycle position.
     *
     * @param userId User ID for assignment
     * @param teamName Team name (must have qdue_offset defined)
     * @param assignmentDate Assignment start date
     * @param cycleDayPosition Position in QuattroDue cycle (1-based, 1-16)
     * @return CompletableFuture with created UserScheduleAssignment
     */
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> createQuattroDueAssignment(
            @NonNull Long userId,
            @NonNull String teamName,
            @NonNull LocalDate assignmentDate,
            int cycleDayPosition) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating QuattroDue assignment for user: " + userId +
                        ", team: " + teamName + ", date: " + assignmentDate +
                        ", cycle position: " + cycleDayPosition);

                // Validate inputs
                OperationResult<Void> validation = validateQuattroDueInputs(
                        userId, teamName, assignmentDate, cycleDayPosition);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getErrorMessage(),
                            OperationResult.OperationType.VALIDATION);
                }

                // Load and validate team
                //OperationResult<Team> teamResult = mTeamRepository.getTeamByName(teamName).join();
                Team teamResult = mTeamRepository.getTeamByName(teamName).join();

                if (teamResult != null) {
                    return OperationResult.failure("Team not found: " + teamName,
                            OperationResult.OperationType.VALIDATION);
                }

                if ((teamResult.getQdueOffset() < 0) || (teamResult.getQdueOffset() > 16)) {
                    return OperationResult.failure("Team " + teamName + " is not configured for QuattroDue patterns",
                            OperationResult.OperationType.VALIDATION);
                }

                // Get QuattroDue RecurrenceRule
                OperationResult<RecurrenceRule> ruleResult = getQuattroDueRecurrenceRule();
                if (!ruleResult.isSuccess()) {
                    return OperationResult.failure("QuattroDue pattern not available: " + ruleResult.getErrorMessage(),
                            OperationResult.OperationType.VALIDATION);
                }

                RecurrenceRule quattroDueRule = ruleResult.getData();

                // Apply gap-filling strategy
                OperationResult<Void> gapFillingResult = closeConflictingAssignments(userId, assignmentDate);
                if (!gapFillingResult.isSuccess()) {
                    Log.w(TAG, "Gap-filling encountered issues: " + gapFillingResult.getErrorMessage());
                    // Continue - gap-filling failures are not critical
                }

                // Generate pattern name
                String patternName = generateQuattroDuePatternName(teamResult, cycleDayPosition);

                // Create UserScheduleAssignment with cycle position
                UserScheduleAssignment assignment = UserScheduleAssignment.createPatternAssignment(
                        userId,
                        teamResult.getId(),
                        quattroDueRule.getId(),
                        assignmentDate,
                        patternName,
                        cycleDayPosition
                );

                // Save assignment
                OperationResult<UserScheduleAssignment> saveResult =
                        mUserScheduleAssignmentRepository.insertUserScheduleAssignment(assignment).join();

                if (saveResult.isSuccess()) {
                    Log.d(TAG, "QuattroDue assignment created successfully: " + assignment.getId() +
                            " with cycle position: " + cycleDayPosition);
                    return OperationResult.success(saveResult.getData(),
                            "QuattroDue assignment created successfully",
                            OperationResult.OperationType.CREATE);
                } else {
                    return OperationResult.failure("Failed to save QuattroDue assignment: " + saveResult.getErrorMessage(),
                            OperationResult.OperationType.CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating QuattroDue assignment", e);
                return OperationResult.failure("Unexpected error creating QuattroDue assignment: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        });
    }

    // ==================== CUSTOM PATTERN ASSIGNMENTS ====================

    /**
     * Create custom pattern assignment using existing RecurrenceRule.
     *
     * @param userId User ID for assignment
     * @param customPatternId RecurrenceRule ID for custom pattern
     * @param teamName Team name (typically "A" for custom patterns)
     * @param assignmentDate Assignment start date
     * @param cycleDayPosition Starting position in pattern cycle (1-based)
     * @return CompletableFuture with created UserScheduleAssignment
     */
    @NonNull
    public CompletableFuture<OperationResult<UserScheduleAssignment>> createCustomPatternAssignment(
            @NonNull Long userId,
            @NonNull String customPatternId,
            @NonNull String teamName,
            @NonNull LocalDate assignmentDate,
            int cycleDayPosition) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating custom pattern assignment for user: " + userId +
                        ", pattern: " + customPatternId + ", date: " + assignmentDate +
                        ", cycle position: " + cycleDayPosition);

                // Validate inputs
                OperationResult<Void> validation = validateCustomPatternInputs(
                        userId, customPatternId, assignmentDate, cycleDayPosition);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getErrorMessage(),
                            OperationResult.OperationType.VALIDATION);
                }

                // Load and validate custom pattern
//                OperationResult<RecurrenceRule> ruleResult =
//                        mRecurrenceRuleRepository.getRecurrenceRuleById(customPatternId).join();
//                if (!ruleResult.isSuccess()) {
//                    return OperationResult.failure("Custom pattern not found: " + customPatternId,
//                            OperationResult.OperationType.VALIDATION);
//                }
                RecurrenceRule ruleResult =
                        mRecurrenceRuleRepository.getRecurrenceRuleById(customPatternId).join();
                if (ruleResult==null) {
                    return OperationResult.failure("Custom pattern not found: " + customPatternId,
                            OperationResult.OperationType.VALIDATION);
                }

                if (!ruleResult.isActive()) {
                    return OperationResult.failure("Custom pattern is not active: " + ruleResult.getName(),
                            OperationResult.OperationType.VALIDATION);
                }

                // Validate or create team for custom patterns
                OperationResult<Team> teamResult = validateOrCreateCustomPatternTeam(teamName);
                if (!teamResult.isSuccess()) {
                    return OperationResult.failure("Team validation failed: " + teamResult.getErrorMessage(),
                            OperationResult.OperationType.VALIDATION);
                }

                Team team = teamResult.getData();
                assert team != null;

                // Apply gap-filling strategy
                OperationResult<Void> gapFillingResult = closeConflictingAssignments(userId, assignmentDate);
                if (!gapFillingResult.isSuccess()) {
                    Log.w(TAG, "Gap-filling encountered issues: " + gapFillingResult.getErrorMessage());
                    // Continue - gap-filling failures are not critical
                }

                // Generate pattern name
                String patternName = generateCustomPatternName(ruleResult, cycleDayPosition);

                // Create UserScheduleAssignment with cycle position
                UserScheduleAssignment assignment = UserScheduleAssignment.createPatternAssignment(
                        userId,
                        team.getId(),
                        ruleResult.getId(),
                        assignmentDate,
                        patternName,
                        cycleDayPosition
                );

                // Save assignment
                OperationResult<UserScheduleAssignment> saveResult =
                        mUserScheduleAssignmentRepository.insertUserScheduleAssignment(assignment).join();

                if (saveResult.isSuccess()) {
                    Log.d(TAG, "Custom pattern assignment created successfully: " + assignment.getId() +
                            " with cycle position: " + cycleDayPosition);

                    UserScheduleAssignment savedAssignment = saveResult.getData();
                    assert savedAssignment != null;

                    return OperationResult.success(savedAssignment,
                            "Custom pattern assignment created successfully",
                            OperationResult.OperationType.CREATE);
                } else {
                    return OperationResult.failure("Failed to save custom pattern assignment: " + saveResult.getErrorMessage(),
                            OperationResult.OperationType.CREATE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating custom pattern assignment", e);
                return OperationResult.failure("Unexpected error creating custom pattern assignment: " + e.getMessage(),
                        OperationResult.OperationType.CREATE);
            }
        });
    }

    // ==================== VALIDATION ====================

    @NonNull
    private OperationResult<Void> validateQuattroDueInputs(@NonNull Long userId, @NonNull String teamName,
                                                           @NonNull LocalDate assignmentDate, int cycleDayPosition) {
        if (userId <= 0) {
            return OperationResult.failure("Invalid user ID", OperationResult.OperationType.VALIDATION);
        }

        if (teamName.trim().isEmpty()) {
            return OperationResult.failure("Team name cannot be empty", OperationResult.OperationType.VALIDATION);
        }

        if (assignmentDate.isBefore(LocalDate.now())) {
            return OperationResult.failure("Assignment date cannot be in the past", OperationResult.OperationType.VALIDATION);
        }

        if (cycleDayPosition < 1 || cycleDayPosition > 16) {
            return OperationResult.failure("QuattroDue cycle position must be between 1 and 16",
                    OperationResult.OperationType.VALIDATION);
        }

        return OperationResult.success(null, OperationResult.OperationType.VALIDATION);
    }

    @NonNull
    private OperationResult<Void> validateCustomPatternInputs(@NonNull Long userId, @NonNull String customPatternId,
                                                              @NonNull LocalDate assignmentDate, int cycleDayPosition) {
        if (userId <= 0) {
            return OperationResult.failure("Invalid user ID", OperationResult.OperationType.VALIDATION);
        }

        if (customPatternId.trim().isEmpty()) {
            return OperationResult.failure("Custom pattern ID cannot be empty", OperationResult.OperationType.VALIDATION);
        }

        if (assignmentDate.isBefore(LocalDate.now())) {
            return OperationResult.failure("Assignment date cannot be in the past", OperationResult.OperationType.VALIDATION);
        }

        if (cycleDayPosition < 1) {
            return OperationResult.failure("Cycle position must be positive", OperationResult.OperationType.VALIDATION);
        }

        return OperationResult.success(null, OperationResult.OperationType.VALIDATION);
    }

    // ==================== GAP-FILLING STRATEGY ====================

    /**
     * Close conflicting assignments to avoid overlaps.
     * Implements gap-filling strategy by ending existing assignments.
     */
    @NonNull
    private OperationResult<Void> closeConflictingAssignments(@NonNull Long userId, @NonNull LocalDate newAssignmentDate) {
        try {
            Log.d(TAG, "Applying gap-filling strategy for user: " + userId + ", new assignment date: " + newAssignmentDate);

            // Get user's active assignments
            OperationResult<List<UserScheduleAssignment>> activeResult =
                    mUserScheduleAssignmentRepository.getUserActiveAssignments(userId).join();

            if (!activeResult.isSuccess()) {
                return OperationResult.failure("Failed to load existing assignments: " + activeResult.getErrorMessage(),
                        OperationResult.OperationType.READ);
            }

            List<UserScheduleAssignment> activeAssignments = activeResult.getData();
            if (activeAssignments.isEmpty()) {
                Log.d(TAG, "No existing active assignments to close");
                return OperationResult.success(null, OperationResult.OperationType.UPDATE);
            }

            // Close conflicting assignments
            for (UserScheduleAssignment existing : activeAssignments) {
                if (existing.appliesTo(newAssignmentDate) || existing.getStartDate().isAfter(newAssignmentDate)) {
                    // This assignment conflicts - close it
                    UserScheduleAssignment closedAssignment;

                    if (existing.getStartDate().isBefore(newAssignmentDate)) {
                        // End existing assignment one day before new assignment
                        closedAssignment = UserScheduleAssignment.createEndedAssignment(existing, newAssignmentDate.minusDays(1));
                    } else {
                        // Cancel future assignment completely
                        closedAssignment = UserScheduleAssignment.createDisabledAssignment(existing);
                    }

                    OperationResult<UserScheduleAssignment> updateResult =
                            mUserScheduleAssignmentRepository.updateUserScheduleAssignment(closedAssignment).join();

                    if (updateResult.isSuccess()) {
                        Log.d(TAG, "Closed conflicting assignment: " + existing.getId() +
                                " (new status: " + closedAssignment.getStatus() + ")");
                    } else {
                        Log.w(TAG, "Failed to close assignment: " + existing.getId() +
                                " - " + updateResult.getErrorMessage());
                    }
                }
            }

            return OperationResult.success(null, "Gap-filling completed successfully", OperationResult.OperationType.UPDATE);

        } catch (Exception e) {
            Log.e(TAG, "Error in gap-filling strategy", e);
            return OperationResult.failure("Gap-filling failed: " + e.getMessage(), OperationResult.OperationType.UPDATE);
        }
    }

    // ==================== PATTERN RETRIEVAL ====================

    /**
     * Retrive Custom Recurrence Rules
     * @param userId
     * @return
     */
    @NonNull
    public CompletableFuture<OperationResult<List<RecurrenceRule>>> getActiveUserRecurrenceRules( Long userId) {
        // Ignoring userId
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting active user recurrence rules");

                // Chiamata singola al repository
                OperationResult<List<RecurrenceRule>> result =
                        mRecurrenceRuleRepository.getActiveUserRecurrenceRules(userId).join();

                if (result.isSuccess()) {
                    List<RecurrenceRule> allRules = result.getData();

                    // Filtra per utente specifico (se implementato)
                    // Per ora restituisce tutte le regole attive
                    List<RecurrenceRule> userRules = allRules.stream()
                            .filter(rule -> rule.isActive()) // Filtro aggiuntivo per sicurezza
                            .collect(Collectors.toList());

                    Log.d(TAG, "Found " + userRules.size() + " active rules for user");
                    return OperationResult.success(userRules, OperationResult.OperationType.READ);
                } else {
                    return OperationResult.failure("Failed to load recurrence rules: " + result.getErrorMessage(),
                            OperationResult.OperationType.READ);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting active user recurrence rules", e);
                return OperationResult.failure("Error getting active user recurrence rules", e.getMessage(), OperationResult.OperationType.READ);

            }
        });

    }

    @NonNull
    private OperationResult<RecurrenceRule> getQuattroDueRecurrenceRule() {
        // Look for QuattroDue pattern in recurrence rules
        return mRecurrenceRuleRepository.getRecurrenceRuleByName("QuattroDue").join()
                .or( mRecurrenceRuleRepository.getRecurrenceRuleByName("4-2 Cycle").join())
                .or( mRecurrenceRuleRepository.getRecurrenceRuleByName("QD_STANDARD").join());
    }

    @NonNull
    private OperationResult<Team> validateOrCreateCustomPatternTeam(@NonNull String teamName) {
        Team teamResult = mTeamRepository.getTeamByName(teamName).join();

        if (teamResult != null) {
            //return teamResult;
            return OperationResult.success(teamResult,
                    OperationResult.OperationType.VALIDATION);
        } else {
            // Create default team for custom patterns if it doesn't exist
            Log.d(TAG, "Team " + teamName + " not found, using default team for custom patterns");
            //return mTeamRepository.getTeamByName(DEFAULT_CUSTOM_TEAM).join();
            Team defaultTeam = mTeamRepository.getTeamByName(DEFAULT_CUSTOM_TEAM).join();
            return OperationResult.success(defaultTeam, "Default Team " + teamName,
                    OperationResult.OperationType.VALIDATION);
        }
    }

    // ==================== PATTERN NAME GENERATION ====================

    @NonNull
    private String generateQuattroDuePatternName(@NonNull Team team, int cycleDayPosition) {
        return MessageFormat.format("{0} - Team {1} (Day {2})",
                QUATTRODUE_PATTERN_PREFIX,
                team.getName(),
                cycleDayPosition);
    }

    @NonNull
    private String generateCustomPatternName(@NonNull RecurrenceRule customRule, int cycleDayPosition) {
        return MessageFormat.format("{0} - {1} (Day {2})",
                CUSTOM_PATTERN_PREFIX,
                customRule.getName(),
                cycleDayPosition);
    }
}