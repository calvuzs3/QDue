package net.calvuz.qdue.ui.features.schedulepattern.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * UserSchedulePatternService - Business Logic Service for User Schedule Pattern Creation
 *
 * <p>Provides high-level business operations for creating, editing, and managing user-defined
 * work schedule patterns. This service bridges between the UI layer and domain layer,
 * handling the conversion from UI models (PatternDay) to domain models (RecurrenceRule).</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Pattern Creation</strong>: Convert PatternDay list to RecurrenceRule</li>
 *   <li><strong>Pattern Validation</strong>: Ensure pattern completeness and consistency</li>
 *   <li><strong>Schedule Assignment</strong>: Create UserScheduleAssignment for user</li>
 *   <li><strong>Pattern Preview</strong>: Generate preview of pattern application</li>
 *   <li><strong>Pattern Modification</strong>: Edit existing user patterns</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><strong>Pattern Length</strong>: Must have at least one day</li>
 *   <li><strong>Start Date</strong>: Cannot be in the past</li>
 *   <li><strong>Shift Validation</strong>: All shifts must exist in system</li>
 *   <li><strong>Rest Day Handling</strong>: Rest days represented as null shifts</li>
 *   <li><strong>Cycle Calculation</strong>: Pattern length determines recurrence cycle</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li><strong>ShiftRepository</strong>: Validate shift references</li>
 *   <li><strong>RecurrenceRuleRepository</strong>: Persist pattern rules</li>
 *   <li><strong>UserScheduleAssignmentRepository</strong>: Create user assignments</li>
 *   <li><strong>RecurrenceCalculator</strong>: Preview pattern application</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public interface UserSchedulePatternService {

    // ==================== PATTERN CREATION ====================

    /**
     * Create a new user schedule pattern from pattern days.
     *
     * @param patternDays List of pattern days defining the sequence
     * @param startDate Date when pattern should start
     * @param patternName User-friendly name for the pattern
     * @return CompletableFuture with OperationResult containing UserScheduleAssignment
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> createUserPattern(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @NonNull String patternName);

    /**
     * Create a new user schedule pattern with auto-generated name.
     *
     * @param patternDays List of pattern days defining the sequence
     * @param startDate Date when pattern should start
     * @return CompletableFuture with OperationResult containing UserScheduleAssignment
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> createUserPattern(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate);

    // ==================== PATTERN MODIFICATION ====================

    /**
     * Update an existing user schedule pattern.
     *
     * @param assignmentId ID of existing UserScheduleAssignment
     * @param patternDays Updated list of pattern days
     * @param startDate Updated start date
     * @param patternName Updated pattern name
     * @return CompletableFuture with OperationResult containing updated UserScheduleAssignment
     */
    @NonNull
    CompletableFuture<OperationResult<UserScheduleAssignment>> updateUserPattern(
            @NonNull String assignmentId,
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @NonNull String patternName);

    /**
     * Delete a user schedule pattern.
     *
     * @param assignmentId ID of UserScheduleAssignment to delete
     * @return CompletableFuture with OperationResult indicating success
     */
    @NonNull
    CompletableFuture<OperationResult<Boolean>> deleteUserPattern(@NonNull String assignmentId);

    // ==================== PATTERN LOADING ====================

    /**
     * Load existing user pattern for editing.
     *
     * @param assignmentId ID of UserScheduleAssignment to load
     * @return CompletableFuture with OperationResult containing PatternEditingData
     */
    @NonNull
    CompletableFuture<OperationResult<PatternEditingData>> loadUserPatternForEditing(
            @NonNull String assignmentId);

    /**
     * Get all user schedule patterns for current user.
     *
     * @return CompletableFuture with OperationResult containing list of UserScheduleAssignments
     */
    @NonNull
    CompletableFuture<OperationResult<List<UserScheduleAssignment>>> getUserPatterns(@NonNull String userId);

    // ==================== PATTERN VALIDATION ====================

    /**
     * Validate pattern days for consistency and completeness.
     *
     * @param patternDays List of pattern days to validate
     * @return OperationResult with validation status and messages
     */
    @NonNull
    OperationResult<Void> validatePatternDays(@NonNull List<PatternDay> patternDays);

    /**
     * Validate start date for pattern creation.
     *
     * @param startDate Start date to validate
     * @return OperationResult with validation status and messages
     */
    @NonNull
    OperationResult<Void> validateStartDate(@NonNull LocalDate startDate);

    /**
     * Validate complete pattern configuration.
     *
     * @param patternDays List of pattern days
     * @param startDate Pattern start date
     * @param patternName Pattern name (can be null for auto-generation)
     * @return OperationResult with validation status and messages
     */
    @NonNull
    OperationResult<Void> validatePatternConfiguration(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            @Nullable String patternName);

    // ==================== PATTERN PREVIEW ====================

    /**
     * Generate preview of pattern application for specified date range.
     *
     * @param patternDays List of pattern days to preview
     * @param startDate Pattern start date
     * @param previewDays Number of days to preview
     * @return CompletableFuture with OperationResult containing list of WorkScheduleDays
     */
    @NonNull
    CompletableFuture<OperationResult<List<WorkScheduleDay>>> generatePatternPreview(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate,
            int previewDays);

    /**
     * Calculate pattern statistics for information display.
     *
     * @param patternDays List of pattern days to analyze
     * @return OperationResult containing PatternStatistics
     */
    @NonNull
    OperationResult<PatternStatistics> calculatePatternStatistics(
            @NonNull List<PatternDay> patternDays);

    // ==================== HELPER METHODS ====================

    /**
     * Generate automatic pattern name based on pattern characteristics.
     *
     * @param patternDays List of pattern days to analyze
     * @param startDate Pattern start date
     * @return Generated pattern name
     */
    @NonNull
    String generatePatternName(@NonNull List<PatternDay> patternDays, @NonNull LocalDate startDate);

    /**
     * Convert PatternDay list to RecurrenceRule domain model.
     *
     * @param patternDays List of pattern days
     * @param startDate Pattern start date
     * @return CompletableFuture with OperationResult containing RecurrenceRule
     */
    @NonNull
    CompletableFuture<OperationResult<RecurrenceRule>> convertToRecurrenceRule(
            @NonNull List<PatternDay> patternDays,
            @NonNull LocalDate startDate);

    /**
     * Convert RecurrenceRule and assignment back to PatternDay list for editing.
     *
     * @param recurrenceRule RecurrenceRule to convert
     * @param assignment UserScheduleAssignment context
     * @return CompletableFuture with OperationResult containing list of PatternDays
     */
    @NonNull
    CompletableFuture<OperationResult<List<PatternDay>>> convertFromRecurrenceRule(
            @NonNull RecurrenceRule recurrenceRule,
            @NonNull UserScheduleAssignment assignment);

    // ==================== DATA CLASSES ====================

    /**
     * Data container for pattern editing session.
     */
    class PatternEditingData {
        private final UserScheduleAssignment assignment;
        private final RecurrenceRule recurrenceRule;
        private final List<PatternDay> patternDays;

        public PatternEditingData(@NonNull UserScheduleAssignment assignment,
                                  @NonNull RecurrenceRule recurrenceRule,
                                  @NonNull List<PatternDay> patternDays) {
            this.assignment = assignment;
            this.recurrenceRule = recurrenceRule;
            this.patternDays = patternDays;
        }

        @NonNull
        public UserScheduleAssignment getAssignment() {
            return assignment;
        }

        @NonNull
        public RecurrenceRule getRecurrenceRule() {
            return recurrenceRule;
        }

        @NonNull
        public List<PatternDay> getPatternDays() {
            return patternDays;
        }

        @NonNull
        public LocalDate getStartDate() {
            return assignment.getStartDate();
        }

        @Nullable
        public String getPatternName() {
            return assignment.getAssignedByUserName(); // .getName();
        }
    }

    /**
     * Pattern statistics for display and analysis.
     */
    class PatternStatistics {
        private final int totalDays;
        private final int workDays;
        private final int restDays;
        private final double workDayPercentage;
        private final List<String> shiftTypes;

        public PatternStatistics(int totalDays, int workDays, int restDays,
                                 double workDayPercentage, @NonNull List<String> shiftTypes) {
            this.totalDays = totalDays;
            this.workDays = workDays;
            this.restDays = restDays;
            this.workDayPercentage = workDayPercentage;
            this.shiftTypes = shiftTypes;
        }

        public int getTotalDays() {
            return totalDays;
        }

        public int getWorkDays() {
            return workDays;
        }

        public int getRestDays() {
            return restDays;
        }

        public double getWorkDayPercentage() {
            return workDayPercentage;
        }

        @NonNull
        public List<String> getShiftTypes() {
            return shiftTypes;
        }
    }
}