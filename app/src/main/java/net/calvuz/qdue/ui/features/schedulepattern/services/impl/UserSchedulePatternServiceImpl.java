package net.calvuz.qdue.ui.features.schedulepattern.services.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.engines.extensions.RecurrenceCalculatorExtensions;
import net.calvuz.qdue.domain.calendar.extensions.RecurrenceRuleExtensions;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;
import net.calvuz.qdue.ui.features.schedulepattern.services.UserSchedulePatternService;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UserSchedulePatternServiceImpl - Implementation of User Schedule Pattern Business Logic
 *
 * <p>Comprehensive implementation providing all business operations for user-defined work
 * schedule patterns. Handles conversion between UI models and domain models, validation,
 * and integration with domain repositories.</p>
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><strong>Pattern Conversion</strong>: PatternDay ↔ RecurrenceRule conversion</li>
 *   <li><strong>Validation Logic</strong>: Business rule validation and error messaging</li>
 *   <li><strong>Repository Coordination</strong>: Orchestrate multiple repository operations</li>
 *   <li><strong>Preview Generation</strong>: Calculate pattern preview using domain engines</li>
 *   <li><strong>Error Handling</strong>: Comprehensive error management with localization</li>
 * </ul>
 *
 * <h3>Architecture Integration:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Constructor-based DI following project patterns</li>
 *   <li><strong>Async Operations</strong>: CompletableFuture-based background processing</li>
 *   <li><strong>Clean Architecture</strong>: Domain model focused, UI-independent</li>
 *   <li><strong>Internationalization</strong>: Localized error messages and pattern names</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class UserSchedulePatternServiceImpl implements UserSchedulePatternService {

    private static final String TAG = "UserSchedulePatternServiceImpl";

    // ==================== CONSTANTS ====================

    private static final int MIN_PATTERN_DAYS = 1;
    private static final int MAX_PATTERN_DAYS = 365;
    private static final int DEFAULT_PREVIEW_DAYS = 30;

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final ShiftRepository mShiftRepository;
    private final UserScheduleAssignmentRepository mUserScheduleAssignmentRepository;
    private final RecurrenceRuleRepository mRecurrenceRuleRepository;
    private final LocaleManager mLocaleManager;
    private final ExecutorService mExecutorService;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new UserSchedulePatternServiceImpl with dependency injection.
     *
     * @param context Application context
     * @param shiftRepository Shift repository for validation
     * @param userScheduleAssignmentRepository Assignment repository for persistence
     * @param recurrenceRuleRepository RecurrenceRule repository for pattern storage
     * @param localeManager Locale manager for internationalization
     */
    public UserSchedulePatternServiceImpl(@NonNull Context context,
                                          @NonNull ShiftRepository shiftRepository,
                                          @NonNull UserScheduleAssignmentRepository userScheduleAssignmentRepository,
                                          @NonNull RecurrenceRuleRepository recurrenceRuleRepository,
                                          @NonNull LocaleManager localeManager) {
        this.mContext = context.getApplicationContext();
        this.mShiftRepository = shiftRepository;
        this.mUserScheduleAssignmentRepository = userScheduleAssignmentRepository;
        this.mRecurrenceRuleRepository = recurrenceRuleRepository;
        this.mLocaleManager = localeManager;
        this.mExecutorService = Executors.newFixedThreadPool(3);

        Log.d(TAG, "UserSchedulePatternServiceImpl initialized");
    }

    // ==================== PATTERN CREATION ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<UserScheduleAssignment>> createUserPattern(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @NonNull String patternName) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating user pattern: " + patternName + ", start: " + startDate);

                // Validate input
                OperationResult<Void> validation = validatePatternConfiguration(patternDays, startDate, patternName);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getErrorMessage(), OperationResult.OperationType.VALIDATION);
                }

                // Convert to RecurrenceRule
                OperationResult<RecurrenceRule> ruleResult = convertToRecurrenceRuleSync(patternDays, startDate);
                if (!ruleResult.isSuccess()) {
                    return OperationResult.failure("Failed to create recurrence rule: " + ruleResult.getErrorMessage(), OperationResult.OperationType.CREATE);
                }

                RecurrenceRule recurrenceRule = ruleResult.getData();

                // Save RecurrenceRule
                RecurrenceRule savedRule = mRecurrenceRuleRepository.saveRecurrenceRule(recurrenceRule).join();
                if (savedRule == null) {
                    return OperationResult.failure("Failed to save recurrence rule", OperationResult.OperationType.CREATE);
                }

                // Create UserScheduleAssignment
                UserScheduleAssignment assignment = createUserScheduleAssignment(
                        savedRule.getId(), startDate, patternName);

                // Save assignment
                OperationResult<UserScheduleAssignment> savedAssignmentResult = mUserScheduleAssignmentRepository.saveUserScheduleAssignment( assignment ).join();
                if (savedAssignmentResult.isSuccess()) {
                    UserScheduleAssignment savedAssignment = mUserScheduleAssignmentRepository
                            .saveUserScheduleAssignment(assignment).join().getData();
                    Log.d(TAG, "Successfully created user pattern: " + savedAssignment.getId());
                    return OperationResult.success(savedAssignment, OperationResult.OperationType.CREATE);
                } else {
                        return OperationResult.failure("Failed to save user schedule assignment", OperationResult.OperationType.CREATE);
                }

//                if (savedAssignment == null) {
//                    return OperationResult.failure("Failed to save user schedule assignment");
//                }
//
//                Log.d(TAG, "Successfully created user pattern: " + savedAssignment.getId());
//                return OperationResult.success(savedAssignment);

            } catch (Exception e) {
                Log.e(TAG, "Error creating user pattern", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<UserScheduleAssignment>> createUserPattern(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate) {

        String autoGeneratedName = generatePatternName(patternDays, startDate);
        return createUserPattern(patternDays, startDate, autoGeneratedName);
    }

    // ==================== PATTERN MODIFICATION ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<UserScheduleAssignment>> updateUserPattern(
            @NonNull String assignmentId,
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @NonNull String patternName) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Updating user pattern: " + assignmentId);

                // Validate input
                OperationResult<Void> validation = validatePatternConfiguration(patternDays, startDate, patternName);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getErrorMessage(), OperationResult.OperationType.VALIDATION);
                }

                // Load existing assignment
                OperationResult<UserScheduleAssignment> existingAssignmentResult = mUserScheduleAssignmentRepository
                        .getUserScheduleAssignmentById(assignmentId).join();
                if (existingAssignmentResult.isFailure()) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                UserScheduleAssignment existingAssignment = existingAssignmentResult.getData();

                if (existingAssignment == null) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                // Convert to new RecurrenceRule
                OperationResult<RecurrenceRule> ruleResult = convertToRecurrenceRuleSync(patternDays, startDate);
                if (!ruleResult.isSuccess()) {
                    return OperationResult.failure("Failed to create recurrence rule: " + ruleResult.getErrorMessage(), OperationResult.OperationType.READ);
                }

                RecurrenceRule newRule = ruleResult.getData();

                // Save new RecurrenceRule
                RecurrenceRule savedRule = mRecurrenceRuleRepository.saveRecurrenceRule(newRule).join();
                if (savedRule == null) {
                    return OperationResult.failure("Failed to save updated recurrence rule", OperationResult.OperationType.UPDATE);
                }

                // Update assignment
                UserScheduleAssignment updatedAssignment = createUpdatedAssignment(
                        existingAssignment, savedRule.getId(), startDate, patternName);

                // Save updated assignment
                OperationResult<UserScheduleAssignment> savedAssignmentResult = mUserScheduleAssignmentRepository
                        .saveUserScheduleAssignment( updatedAssignment ).join();
                if (savedAssignmentResult.isFailure()) {
                    return OperationResult.failure("Failed to save updated assignment", OperationResult.OperationType.UPDATE);
                }

                UserScheduleAssignment savedAssignment = savedAssignmentResult.getData();

                if (savedAssignment == null) {
                    return OperationResult.failure("Failed to save updated assignment", OperationResult.OperationType.UPDATE);
                }

                // Clean up old recurrence rule if different
                if (!existingAssignment.getRecurrenceRuleId().equals(savedRule.getId())) {
                    mRecurrenceRuleRepository.deleteRecurrenceRule(existingAssignment.getRecurrenceRuleId());
                }

                Log.d(TAG, "Successfully updated user pattern: " + assignmentId);
                return OperationResult.success(savedAssignment, OperationResult.OperationType.UPDATE);

            } catch (Exception e) {
                Log.e(TAG, "Error updating user pattern", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.UPDATE);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<Boolean>> deleteUserPattern(@NonNull String assignmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Deleting user pattern: " + assignmentId);

                // Load assignment to get recurrence rule ID
                OperationResult<UserScheduleAssignment> assignmentResult = mUserScheduleAssignmentRepository
                        .getUserScheduleAssignmentById( assignmentId ).join();

                if (assignmentResult.isFailure()) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                UserScheduleAssignment assignment = assignmentResult.getData();
                if (assignment == null) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                // Delete assignment
                OperationResult<Boolean> assignmentDeletedResult = mUserScheduleAssignmentRepository
                        .deleteUserScheduleAssignment( assignmentId ).join();
                if (assignmentDeletedResult.isFailure()) {
                    return OperationResult.failure("Failed to delete assignment", OperationResult.OperationType.DELETE);
                }

                boolean assignmentDeleted = Boolean.TRUE.equals( assignmentDeletedResult.getData() );

                if (!assignmentDeleted) {
                    return OperationResult.failure("Failed to delete assignment", OperationResult.OperationType.DELETE);
                }

                // Delete recurrence rule if not used by other assignments
                String ruleId = assignment.getRecurrenceRuleId();
                if (ruleId != null) {
                    boolean ruleInUse = mRecurrenceRuleRepository.isRecurrenceRuleInUse(ruleId).join();
                    if (!ruleInUse) {
                        mRecurrenceRuleRepository.deleteRecurrenceRule(ruleId);
                    }
                }

                Log.d(TAG, "Successfully deleted user pattern: " + assignmentId);
                return OperationResult.success("deleteUserPattern", OperationResult.OperationType.DELETE);

            } catch (Exception e) {
                Log.e(TAG, "Error deleting user pattern", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.DELETE);
            }
        }, mExecutorService);
    }

    // ==================== PATTERN LOADING ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<PatternEditingData>> loadUserPatternForEditing(
            @NonNull String assignmentId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Loading user pattern for editing: " + assignmentId);

                // Load assignment
                OperationResult<UserScheduleAssignment> assignmentResult = mUserScheduleAssignmentRepository
                        .getUserScheduleAssignmentById(assignmentId).join();
                if (assignmentResult.isFailure()) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                UserScheduleAssignment assignment = assignmentResult.getData();

                if (assignment == null) {
                    return OperationResult.failure("Assignment not found: " + assignmentId, OperationResult.OperationType.READ);
                }

                // Load recurrence rule
                RecurrenceRule recurrenceRule = mRecurrenceRuleRepository
                        .getRecurrenceRuleById(assignment.getRecurrenceRuleId()).join();

                if (recurrenceRule == null) {
                    return OperationResult.failure("Recurrence rule not found: " + assignment.getRecurrenceRuleId(), OperationResult.OperationType.READ);
                }

                // Convert to pattern days
                OperationResult<List<PatternDay>> patternResult = convertFromRecurrenceRuleSync(recurrenceRule, assignment);
                if (!patternResult.isSuccess()) {
                    return OperationResult.failure("Failed to convert pattern: " + patternResult.getErrorMessage(), OperationResult.OperationType.VALIDATION);
                }

                PatternEditingData editingData = new PatternEditingData(assignment, recurrenceRule, patternResult.getData());

                Log.d(TAG, "Successfully loaded pattern for editing: " + assignmentId);
                return OperationResult.success(editingData, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error loading pattern for editing", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getUserPatterns() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Loading user patterns");

                // TODO: remove hardcoded user ID
                OperationResult<List<UserScheduleAssignment>> assignmentResult = mUserScheduleAssignmentRepository
                        .getActiveAssignmentsForUser( 1L ).join();
                if (assignmentResult.isFailure()) {
                    return OperationResult.failure("No user patterns found", OperationResult.OperationType.READ);
                }

                List<UserScheduleAssignment> assignments = assignmentResult.getData();
                if (assignments == null) {
                    return OperationResult.failure("No user patterns found", OperationResult.OperationType.READ);
                }

                Log.d(TAG, "Loaded " + assignments.size() + " user patterns");
                return OperationResult.success(assignments, OperationResult.OperationType.READ);

            } catch (Exception e) {
                Log.e(TAG, "Error loading user patterns", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.READ);
            }
        }, mExecutorService);
    }

    // ==================== PATTERN VALIDATION ====================

    @NonNull
    @Override
    public OperationResult<Void> validatePatternDays(@NonNull List<PatternDay> patternDays) {
        try {
            // Check minimum pattern length
            if (patternDays.isEmpty()) {
                return OperationResult.failure("Pattern must contain at least one day", OperationResult.OperationType.VALIDATION);
            }

            if (patternDays.size() < MIN_PATTERN_DAYS) {
                return OperationResult.failure("Pattern must contain at least " + MIN_PATTERN_DAYS + " day(s)", OperationResult.OperationType.VALIDATION);
            }

            if (patternDays.size() > MAX_PATTERN_DAYS) {
                return OperationResult.failure("Pattern cannot exceed " + MAX_PATTERN_DAYS + " days", OperationResult.OperationType.VALIDATION);
            }

            // Validate day numbers are sequential
            for (int i = 0; i < patternDays.size(); i++) {
                PatternDay day = patternDays.get(i);
                if (day.getDayNumber() != i + 1) {
                    return OperationResult.failure("Pattern days must be in sequential order", OperationResult.OperationType.VALIDATION);
                }
            }

            // Validate shift references (for work days)
            for (PatternDay day : patternDays) {
                if (day.isWorkDay()) {
                    Shift shift = day.getShift();
                    if (shift == null || shift.getId() == null || shift.getId().trim().isEmpty()) {
                        return OperationResult.failure("Work day " + day.getDayNumber() + " has invalid shift reference", OperationResult.OperationType.VALIDATION);
                    }
                }
            }

            Log.d(TAG, "Pattern days validation successful");
            return OperationResult.success("Pattern days validation successful", OperationResult.OperationType.VALIDATION);

        } catch (Exception e) {
            Log.e(TAG, "Error validating pattern days", e);
            return OperationResult.failure("Validation error: " + e.getMessage(), OperationResult.OperationType.VALIDATION);
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> validateStartDate(@NonNull LocalDate startDate) {
        try {
            LocalDate today = LocalDate.now();

            if (startDate.isBefore(today)) {
                return OperationResult.failure("Start date cannot be in the past", OperationResult.OperationType.VALIDATION);
            }

            Log.d(TAG, "Start date validation successful");
            return OperationResult.success("Start date validation successful", OperationResult.OperationType.VALIDATION);

        } catch (Exception e) {
            Log.e(TAG, "Error validating start date", e);
            return OperationResult.failure("Validation error: " + e.getMessage(), OperationResult.OperationType.VALIDATION);
        }
    }

    @NonNull
    @Override
    public OperationResult<Void> validatePatternConfiguration(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @Nullable String patternName) {

        // Validate pattern days
        OperationResult<Void> patternValidation = validatePatternDays(patternDays);
        if (!patternValidation.isSuccess()) {
            return patternValidation;
        }

        // Validate start date
        OperationResult<Void> dateValidation = validateStartDate(startDate);
        if (!dateValidation.isSuccess()) {
            return dateValidation;
        }

        // Pattern name validation (if provided)
        if (patternName != null && patternName.trim().length() > 100) {
            return OperationResult.failure("Pattern name cannot exceed 100 characters", OperationResult.OperationType.VALIDATION);
        }

        Log.d(TAG, "Pattern configuration validation successful");
        return OperationResult.success("Pattern configuration validation successful", OperationResult.OperationType.VALIDATION);
    }

    // ==================== PATTERN PREVIEW ====================

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<WorkScheduleDay>>> generatePatternPreview(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            int previewDays) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Generating pattern preview for " + previewDays + " days");

                // Validate input
                OperationResult<Void> validation = validatePatternDays(patternDays);
                if (!validation.isSuccess()) {
                    return OperationResult.failure(validation.getErrorMessage(), OperationResult.OperationType.VALIDATION);
                }

                // Use enhanced preview generation with RecurrenceCalculator extensions
                return generatePatternPreviewInternal(patternDays, startDate, previewDays).join();

            } catch (Exception e) {
                Log.e(TAG, "Error generating pattern preview", e);
                return OperationResult.failure("Unexpected error: " + e.getMessage(), OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }

    @NonNull
    @Override
    public OperationResult<PatternStatistics> calculatePatternStatistics(
            @NonNull List<PatternDay> patternDays) {

        try {
            int totalDays = patternDays.size();
            int workDays = 0;
            int restDays = 0;
            Set<String> shiftTypes = new HashSet<>();

            for (PatternDay day : patternDays) {
                if (day.isWorkDay()) {
                    workDays++;
                    Shift shift = day.getShift();
                    if (shift != null && shift.getName() != null) {
                        shiftTypes.add(shift.getName());
                    }
                } else {
                    restDays++;
                }
            }

            double workDayPercentage = totalDays > 0 ? (workDays * 100.0) / totalDays : 0.0;

            PatternStatistics statistics = new PatternStatistics(
                    totalDays, workDays, restDays, workDayPercentage,
                    new ArrayList<>(shiftTypes));

            Log.d(TAG, "Calculated pattern statistics: " + workDays + " work days, " + restDays + " rest days");
            return OperationResult.success(statistics, OperationResult.OperationType.CREATE);

        } catch (Exception e) {
            Log.e(TAG, "Error calculating pattern statistics", e);
            return OperationResult.failure("Calculation error: " + e.getMessage(), OperationResult.OperationType.VALIDATION);
        }
    }

    // ==================== HELPER METHODS ====================

    @NonNull
    @Override
    public String generatePatternName(@NonNull List<PatternDay> patternDays, @NonNull LocalDate startDate) {
        try {
            OperationResult<PatternStatistics> statsResult = calculatePatternStatistics(patternDays);

            if (statsResult.isSuccess()) {
                PatternStatistics stats = statsResult.getData();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                return String.format("Schema %d giorni - dal %s",
                        stats.getTotalDays(),
                        startDate.format(formatter));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return String.format("Schema Lavoro - dal %s", startDate.format(formatter));
            }

        } catch (Exception e) {
            Log.w(TAG, "Error generating pattern name, using fallback", e);
            return "Schema Lavoro Personalizzato";
        }
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<RecurrenceRule>> convertToRecurrenceRule(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate) {

        return CompletableFuture.supplyAsync(() -> convertToRecurrenceRuleSync(patternDays, startDate), mExecutorService);
    }

    @NonNull
    @Override
    public CompletableFuture<OperationResult<List<PatternDay>>> convertFromRecurrenceRule(
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        return CompletableFuture.supplyAsync(() -> convertFromRecurrenceRuleSync(recurrenceRule, assignment), mExecutorService);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    @NonNull
    private OperationResult<RecurrenceRule> convertToRecurrenceRuleSync(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate) {

        try {
            // Validate pattern first
            RecurrenceRuleExtensions.ValidationResult validation =
                    RecurrenceRuleExtensions.validateCustomPattern(patternDays);

            if (!validation.isValid) {
                return OperationResult.failure("Pattern validation failed: " + validation.message, OperationResult.OperationType.VALIDATION);
            }

            // Generate pattern name
            String autoPatternName = generatePatternName(patternDays, startDate);

            // Create custom recurrence rule using extensions
            RecurrenceRule rule = RecurrenceRuleExtensions.createCustomPattern(
                    autoPatternName, patternDays, startDate);

            Log.d(TAG, "Successfully converted PatternDays to RecurrenceRule: " + rule.getId());
            return OperationResult.success("Successfully converted PatternDays to RecurrenceRule", OperationResult.OperationType.CREATE);

        } catch (Exception e) {
            Log.e(TAG, "Error converting to recurrence rule", e);
            return OperationResult.failure("Conversion error: " + e.getMessage(), OperationResult.OperationType.CREATE);
        }
    }

    @NonNull
    private OperationResult<List<PatternDay>> convertFromRecurrenceRuleSync(
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment) {

        try {
            // Check if this is a custom pattern
            if (!RecurrenceRuleExtensions.isCustomPattern(recurrenceRule)) {
                Log.w(TAG, "RecurrenceRule is not a custom pattern, cannot convert");
                return OperationResult.failure("Not a custom pattern rule", OperationResult.OperationType.VALIDATION);
            }

            // Extract pattern days using extensions
            List<PatternDay> patternDays = RecurrenceRuleExtensions.extractPatternDays(recurrenceRule);

            if (patternDays.isEmpty()) {
                return OperationResult.failure("No pattern data found in recurrence rule", OperationResult.OperationType.VALIDATION);
            }

            // Validate extracted pattern
            RecurrenceRuleExtensions.ValidationResult validation =
                    RecurrenceRuleExtensions.validateCustomPattern(patternDays);

            if (!validation.isValid) {
                Log.w(TAG, "Extracted pattern validation failed: " + validation.message);
                // Don't fail completely, just log warning and return what we have
            }

            Log.d(TAG, "Successfully converted RecurrenceRule to " + patternDays.size() + " PatternDays");
            return OperationResult.success("Successfully converted RecurrenceRule to " + patternDays.size() + " PatternDays", OperationResult.OperationType.CREATE);

        } catch (Exception e) {
            Log.e(TAG, "Error converting from recurrence rule", e);
            return OperationResult.failure("Conversion error: " + e.getMessage(), OperationResult.OperationType.CREATE);
        }
    }

    @NonNull
    private UserScheduleAssignment createUserScheduleAssignment(
            @NonNull String recurrenceRuleId,
            @NonNull LocalDate startDate,
            @NonNull String patternName) {

        String assignmentId = "assignment_" + UUID.randomUUID().toString();

        // TODO: Create UserScheduleAssignment using builder pattern
        // This depends on the actual UserScheduleAssignment implementation

        return UserScheduleAssignment.builder()
                .id(assignmentId)
                .recurrenceRuleId(recurrenceRuleId)
                .startDate(startDate)
//                .name(patternName)
//                .isActive(true)
                .build();
    }

    @NonNull
    private UserScheduleAssignment createUpdatedAssignment(
            @NonNull UserScheduleAssignment existing,
            @NonNull String newRecurrenceRuleId,
            @NonNull LocalDate newStartDate,
            @NonNull String newPatternName) {

        return UserScheduleAssignment.builder()
                .id(existing.getId())
                .recurrenceRuleId(newRecurrenceRuleId)
                .startDate(newStartDate)
//                .name(newPatternName)
//                .isActive(existing.isActive())
                .build();
    }

    @NonNull
    private WorkScheduleDay createWorkScheduleDayFromPattern(
            @NonNull LocalDate date,
            @NonNull PatternDay patternDay) {

        WorkScheduleDay.Builder builder = WorkScheduleDay.builder(date);

        if (patternDay.isWorkDay() && patternDay.getShift() != null) {
            Shift shift = patternDay.getShift();
            WorkScheduleShift workShift = WorkScheduleShift.builder()
                    .shift(shift)
                    .startTime(shift.getStartTime())
                    .endTime(shift.getEndTime())
                    .build();

            builder.addShift(workShift);
        }

        return builder.build();
    }

    // ==================== ENHANCED PREVIEW GENERATION ====================

    /**
     * Generate preview using pattern-based calculation.
     * Creates a temporary RecurrenceRule and UserScheduleAssignment for preview.
     */
    @NonNull
    private CompletableFuture<OperationResult<List<WorkScheduleDay>>> generatePatternPreviewInternal(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            int previewDays) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create temporary RecurrenceRule for preview
                RecurrenceRule tempRule = RecurrenceRuleExtensions.createCustomPattern(
                        "Preview Pattern", patternDays, startDate);

                // Create temporary UserScheduleAssignment for preview
                UserScheduleAssignment tempAssignment = UserScheduleAssignment.builder()
                        .id("temp_preview_assignment")
                        .userId(0L) // Temp user ID
                        .teamId("temp_team")
                        .recurrenceRuleId(tempRule.getId())
                        .startDate(startDate)
                        .status(UserScheduleAssignment.Status.ACTIVE)
                        .priority(UserScheduleAssignment.Priority.NORMAL)
                        .build();

                // Generate preview using RecurrenceCalculator extensions
                LocalDate endDate = startDate.plusDays(previewDays - 1);
                List<WorkScheduleDay> previewSchedule = RecurrenceCalculatorExtensions
                        .generateCustomPatternWorkScheduleDays(startDate, endDate, tempRule, tempAssignment);

                return OperationResult.success(previewSchedule, OperationResult.OperationType.CREATE);

            } catch (Exception e) {
                Log.e(TAG, "Error generating pattern preview", e);
                return OperationResult.failure("Preview generation error: " + e.getMessage(), OperationResult.OperationType.CREATE);
            }
        }, mExecutorService);
    }
}