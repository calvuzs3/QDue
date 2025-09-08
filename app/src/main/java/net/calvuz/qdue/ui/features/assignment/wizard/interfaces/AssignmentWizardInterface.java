package net.calvuz.qdue.ui.features.assignment.wizard.interfaces;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

import java.time.LocalDate;

/**
 * AssignmentWizardInterface - Communication Contract for Assignment Wizard Components
 *
 * <p>Defines the communication interface between PatternAssignmentWizardActivity and its fragments.
 * Provides callbacks for user selections, navigation control, and dependency access.</p>
 *
 * <h3>Communication Flow:</h3>
 * <ul>
 *   <li><strong>Selection Callbacks</strong>: Fragments notify activity of user selections</li>
 *   <li><strong>Navigation Control</strong>: Fragments can trigger wizard navigation</li>
 *   <li><strong>Data Access</strong>: Fragments access shared wizard state and dependencies</li>
 *   <li><strong>Validation Feedback</strong>: Activity provides validation results to fragments</li>
 * </ul>
 *
 * <h3>Fragment Integration:</h3>
 * <ul>
 *   <li><strong>PatternTypeSelectionFragment</strong>: Calls onPatternTypeSelected()</li>
 *   <li><strong>TeamSelectionFragment</strong>: Calls onTeamSelected()</li>
 *   <li><strong>CustomPatternSelectionFragment</strong>: Calls onCustomPatternSelected()</li>
 *   <li><strong>DatePositionSelectionFragment</strong>: Calls onDateSelected() and onCycleDayPositionSelected()</li>
 *   <li><strong>ConfirmationFragment</strong>: Uses getWizardData() for summary display</li>
 * </ul>
 *
 * <h3>Dependency Injection:</h3>
 * <ul>
 *   <li><strong>AssignmentWizardData</strong>: Shared state across all fragments</li>
 *   <li><strong>CreatePatternAssignmentUseCase</strong>: Business logic for assignment creation</li>
 * </ul>
 */
public interface AssignmentWizardInterface {

    // ==================== PATTERN SELECTION CALLBACKS ====================

    /**
     * Called when user selects a pattern type (QuattroDue vs Custom).
     * Updates wizard state and triggers UI updates for dependent steps.
     *
     * @param patternType Selected pattern type
     */
    void onPatternTypeSelected(@NonNull AssignmentWizardData.PatternType patternType);

    /**
     * Called when user selects a team for QuattroDue pattern.
     * Only valid when pattern type is QUATTRODUE.
     *
     * @param team Selected team with qdue_offset configured
     */
    void onTeamSelected(@NonNull Team team);

    /**
     * Called when user selects a custom pattern from available RecurrenceRules.
     * Only valid when pattern type is CUSTOM.
     *
     * @param customPattern Selected custom pattern RecurrenceRule
     */
    void onCustomPatternSelected(@NonNull RecurrenceRule customPattern);

    // ==================== DATE AND POSITION CALLBACKS ====================

    /**
     * Called when user selects assignment start date.
     * Date must be today or in the future.
     *
     * @param assignmentDate Selected assignment start date
     */
    void onDateSelected(@NonNull LocalDate assignmentDate);

    /**
     * Called when user selects cycle day position.
     * Position is 1-based and must be valid for the selected pattern type.
     *
     * @param cycleDayPosition Selected cycle day position (1-based)
     */
    void onCycleDayPositionSelected(int cycleDayPosition);

    // ==================== NAVIGATION CONTROL ====================

    /**
     * Request navigation to next wizard step.
     * Activity will validate current step before proceeding.
     */
    void onNavigateToNextStep();

    /**
     * Request navigation to previous wizard step.
     * Activity will navigate back if not on first step.
     */
    void onNavigateToPreviousStep();

    // ==================== DATA ACCESS ====================

    /**
     * Get current wizard data state.
     * Fragments use this to access shared state and validate selections.
     *
     * @return Current AssignmentWizardData instance
     */
    @NonNull
    AssignmentWizardData getWizardData();

    /**
     * Get CreatePatternAssignmentUseCase for business operations.
     * Fragments can use this for validation or preview operations.
     *
     * @return CreatePatternAssignmentUseCase instance
     */
    @NonNull
    CreatePatternAssignmentUseCase getAssignmentUseCase();

    // ==================== VALIDATION AND FEEDBACK ====================

    /**
     * Validate current wizard state for specific step.
     * Fragments can call this to provide real-time validation feedback.
     *
     * @param stepIndex Step to validate (0-based)
     * @return true if step is valid, false otherwise
     */
    default boolean isStepValid(int stepIndex) {
        AssignmentWizardData data = getWizardData();
        return switch (stepIndex) {
            case 0 -> data.isStep1Complete();
            case 1 -> data.isStep2Complete();
            case 2 -> data.isStep3Complete();
            case 3 -> data.isWizardComplete();
            default -> false;
        };
    }

    /**
     * Get validation error message for current wizard state.
     * Fragments can use this to display specific validation errors.
     *
     * @return Validation error message or null if valid
     */
    @NonNull
    default String getValidationError() {
        String error = getWizardData().validateCurrentState();
        return error != null ? error : "";
    }

    /**
     * Check if wizard is ready for assignment creation.
     * Used by confirmation fragment to enable/disable creation button.
     *
     * @return true if ready for assignment creation
     */
    default boolean isReadyForAssignmentCreation() {
        return getWizardData().isValidForAssignmentCreation();
    }

    // ==================== FRAGMENT LIFECYCLE CALLBACKS ====================

    /**
     * Called when a fragment becomes visible.
     * Activity can use this to update UI state and validate fragment readiness.
     *
     * @param stepIndex Step that became visible (0-based)
     */
    default void onStepBecameVisible(int stepIndex) {
        // Default implementation - activity can override if needed
    }

    /**
     * Called when a fragment is about to be hidden.
     * Fragment can perform cleanup or validation before step change.
     *
     * @param stepIndex Step that will be hidden (0-based)
     * @return true if fragment is ready to be hidden, false to prevent navigation
     */
    default boolean onStepWillHide(int stepIndex) {
        return isStepValid(stepIndex);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get current step index (0-based).
     * Fragments can use this to determine their position in the wizard.
     *
     * @return Current step index
     */
    default int getCurrentStepIndex() {
        AssignmentWizardData data = getWizardData();
        if (!data.isStep1Complete()) return 0;
        if (!data.isStep2Complete()) return 1;
        if (!data.isStep3Complete()) return 2;
        return 3;
    }

    /**
     * Get total number of steps in wizard.
     * May vary based on pattern type selection.
     *
     * @return Total step count
     */
    default int getTotalSteps() {
        return 4; // Fixed for current implementation
    }

    /**
     * Check if wizard is in editing mode.
     * Fragments can adjust UI behavior for editing existing assignments.
     *
     * @return true if editing existing assignment
     */
    default boolean isEditingMode() {
        return getWizardData().isEditingMode();
    }

    /**
     * Check if this is a first assignment setup.
     * Fragments can provide different UI/messaging for first-time users.
     *
     * @return true if this is user's first assignment
     */
    default boolean isFirstAssignment() {
        return getWizardData().isFirstAssignment();
    }
}