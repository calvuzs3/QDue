package net.calvuz.qdue.ui.features.assignment.wizard.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;

import java.time.LocalDate;

/**
 * AssignmentWizardData - State Management Model for Pattern Assignment Wizard
 *
 * <p>Centralized data container for managing wizard state across multiple fragments.
 * Maintains user selections and validates wizard completion state.</p>
 *
 * <h3>Wizard Flow State:</h3>
 * <ul>
 *   <li><strong>Step 1</strong>: Pattern Type Selection (QuattroDue vs Custom)</li>
 *   <li><strong>Step 2a</strong>: Team Selection (QuattroDue) or Custom Pattern Selection</li>
 *   <li><strong>Step 3</strong>: Assignment Date and Cycle Position Selection</li>
 *   <li><strong>Step 4</strong>: Confirmation and Assignment Creation</li>
 * </ul>
 *
 * <h3>State Validation:</h3>
 * <ul>
 *   <li><strong>Pattern Type Dependent</strong>: Validates selections based on chosen pattern type</li>
 *   <li><strong>Step Completion</strong>: Tracks which steps have been completed</li>
 *   <li><strong>Assignment Readiness</strong>: Determines if wizard can create assignment</li>
 * </ul>
 *
 * <h3>Pattern Types:</h3>
 * <ul>
 *   <li><strong>QUATTRODUE</strong>: Standard 4-on-2-off shift pattern with team-based scheduling</li>
 *   <li><strong>CUSTOM</strong>: User-defined patterns using existing RecurrenceRule templates</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment Wizard State Management
 * @since Clean Architecture Phase 2
 */
public class AssignmentWizardData {

    // ==================== PATTERN TYPE ENUMERATION ====================

    /**
     * Available pattern types for assignment creation.
     */
    public enum PatternType {
        QUATTRODUE( "quattrodue", "QuattroDue Standard Pattern" ),
        CUSTOM( "custom", "Custom User Pattern" );

        private final String key;
        private final String displayName;

        PatternType(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Check if this pattern type requires team selection.
         */
        public boolean requiresTeamSelection() {
            return this == QUATTRODUE;
        }

        /**
         * Check if this pattern type requires custom pattern selection.
         */
        public boolean requiresCustomPatternSelection() {
            return this == CUSTOM;
        }
    }

    // ==================== CORE WIZARD STATE ====================

    // User identification
    private String userId;
    private boolean isFirstAssignment = false;
    private String editingAssignmentId = null;

    // Pattern selection state
    private PatternType patternType = null; //PatternType.QUATTRODUE;

    // Team selection
    private Team selectedTeam = null;

    // Custom pattern specific selections
    private RecurrenceRule selectedCustomPattern = null;

    // Date and cycle selections
    private LocalDate assignmentDate = null; //LocalDate.now();
    private int cycleDayPosition = 1; // 1-based cycle position

    // Additional wizard metadata
    private boolean hasExistingAssignments = false;
    private String wizardTitle = null;

    // ==================== CONSTRUCTORS ====================

    public AssignmentWizardData() {
        // Default constructor
    }

    // Overload constructor
    public AssignmentWizardData(QDueUser user, Team selectedTeam ) {
        this.userId = user.getId();
        this.selectedTeam = selectedTeam;
    }

    // ==================== USER IDENTIFICATION ====================

    @Nullable
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    public boolean isFirstAssignment() {
        return isFirstAssignment;
    }

    public void setFirstAssignment(boolean firstAssignment) {
        this.isFirstAssignment = firstAssignment;
    }

    @Nullable
    public String getEditingAssignmentId() {
        return editingAssignmentId;
    }

    public void setEditingAssignmentId(@Nullable String editingAssignmentId) {
        this.editingAssignmentId = editingAssignmentId;
    }

    /**
     * Check if this wizard is in editing mode.
     */
    public boolean isEditingMode() {
        return editingAssignmentId != null && !editingAssignmentId.trim().isEmpty();
    }

    // ==================== PATTERN TYPE SELECTION ====================

    @Nullable
    public PatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(@Nullable PatternType patternType) {
        this.patternType = patternType;

        // Clear dependent selections when pattern type changes
        if (patternType != PatternType.QUATTRODUE) {
            selectedTeam = null;
        }
        if (patternType != PatternType.CUSTOM) {
            selectedCustomPattern = null;
        }
    }

    /**
     * Check if pattern type has been selected.
     */
    public boolean hasPatternTypeSelected() {
        return patternType != null;
    }

    // ==================== QUATTRODUE TEAM SELECTION ====================

    @Nullable
    public Team getSelectedTeam() {
        return selectedTeam;
    }

    public void setSelectedTeam(@Nullable Team selectedTeam) {
        this.selectedTeam = selectedTeam;
    }

    /**
     * Check if team selection is complete (for QuattroDue patterns).
     */
    public boolean hasTeamSelected() {
        return patternType != PatternType.QUATTRODUE || selectedTeam != null;
    }

    // ==================== CUSTOM PATTERN SELECTION ====================

    @Nullable
    public RecurrenceRule getSelectedCustomPattern() {
        return selectedCustomPattern;
    }

    public void setSelectedCustomPattern(@Nullable RecurrenceRule selectedCustomPattern) {
        this.selectedCustomPattern = selectedCustomPattern;
    }

    /**
     * Check if custom pattern selection is complete.
     */
    public boolean hasCustomPatternSelected() {
        return patternType != PatternType.CUSTOM || selectedCustomPattern != null;
    }

    // ==================== DATE AND CYCLE POSITION ====================

    @Nullable
    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(@Nullable LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }

    public int getCycleDayPosition() {
        return cycleDayPosition;
    }

    public void setCycleDayPosition(int cycleDayPosition) {
        this.cycleDayPosition = Math.max( 1, cycleDayPosition ); // Ensure 1-based minimum
    }

    /**
     * Check if date and cycle position are selected.
     */
    public boolean hasDateAndPositionSelected() {
        return assignmentDate != null && cycleDayPosition > 0;
    }

    // ==================== WIZARD METADATA ====================

    public boolean hasExistingAssignments() {
        return hasExistingAssignments;
    }

    public void setHasExistingAssignments(boolean hasExistingAssignments) {
        this.hasExistingAssignments = hasExistingAssignments;
    }

    @Nullable
    public String getWizardTitle() {
        return wizardTitle;
    }

    public void setWizardTitle(@Nullable String wizardTitle) {
        this.wizardTitle = wizardTitle;
    }

    // ==================== WIZARD COMPLETION VALIDATION ====================

    /**
     * Check if Step 1 (Pattern Type Selection) is complete.
     */
    public boolean isStep1Complete() {

        return hasPatternTypeSelected();
    }

    /**
     * Check if Step 2 (Team/Custom Pattern Selection) is complete.
     */
    public boolean isStep2Complete() {
        if (!isStep1Complete()) return false;

        return switch (patternType) {
            case QUATTRODUE -> hasTeamSelected();
            case CUSTOM -> hasCustomPatternSelected();
            default -> false;
        };
    }

    /**
     * Check if Step 3 (Date & Position Selection) is complete.
     */
    public boolean isStep3Complete() {

        return isStep2Complete() && hasDateAndPositionSelected();
    }

    /**
     * Check if all wizard steps are complete and ready for assignment creation.
     */
    public boolean isWizardComplete() {

        return isStep3Complete();
    }

    /**
     * Get completion percentage for progress indicators.
     *
     * @return Progress percentage (0-100)
     */
    public int getCompletionPercentage() {
        if (isStep3Complete()) return 100;
        if (isStep2Complete()) return 75;
        if (isStep1Complete()) return 50;
        return 25;
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * Validate current state and return validation errors if any.
     *
     * @return Validation error message or null if valid
     */
    @Nullable
    public String validateCurrentState() {
        if (userId.trim().isEmpty()) {
            return "User ID cannot be empty";
        }

        if (!hasPatternTypeSelected()) {
            return "Pattern type must be selected";
        }

        if (patternType == PatternType.QUATTRODUE && !hasTeamSelected()) {
            return "Team must be selected for QuattroDue patterns";
        }

        if (patternType == PatternType.CUSTOM && !hasCustomPatternSelected()) {
            return "Custom pattern must be selected";
        }

        if (!hasDateAndPositionSelected()) {
            return "Assignment date and cycle position must be selected";
        }

        if (assignmentDate == null) {
            return "Assignment date cannot be null";
        }

        if (patternType == PatternType.QUATTRODUE && (cycleDayPosition < 1 || cycleDayPosition > 18)) {
            return "QuattroDue cycle position must be between 1 and 18";
        }

        return null; // Valid state
    }

    /**
     * Check if wizard state is valid for assignment creation.
     */
    public boolean isValidForAssignmentCreation() {
        return validateCurrentState() == null;
    }

    // ==================== DISPLAY HELPERS ====================

    /**
     * Get display summary for confirmation step.
     */
    @NonNull
    public String getAssignmentSummary() {
        if (!isWizardComplete()) {
            return "Incomplete assignment configuration";
        }

        StringBuilder summary = new StringBuilder();

        summary.append( "Pattern: " ).append( patternType.getDisplayName() );

        if (patternType == PatternType.QUATTRODUE && selectedTeam != null) {
            summary.append( "\nTeam: " ).append( selectedTeam.getName() );
            summary.append( " (Offset: " ).append( selectedTeam.getQdueOffset() ).append( ")" );
        } else if (patternType == PatternType.CUSTOM && selectedCustomPattern != null) {
            summary.append( "\nCustom Pattern: " ).append( selectedCustomPattern.getName() );
        }

        if (assignmentDate != null) {
            summary.append( "\nStart Date: " ).append( assignmentDate.toString() );
        }

        summary.append( "\nCycle Position: " ).append( cycleDayPosition );

        return summary.toString();
    }

    /**
     * Get pattern type display name safely.
     */
    @NonNull
    public String getPatternTypeDisplayName() {
        return patternType != null ? patternType.getDisplayName() : "Not Selected";
    }

    // ==================== RESET METHODS ====================

    /**
     * Reset all selections (keep user info).
     */
    public void resetSelections() {
        patternType = null;
        selectedTeam = null;
        selectedCustomPattern = null;
        assignmentDate = null;
        cycleDayPosition = 1;
    }

    /**
     * Reset wizard to initial state.
     */
    public void resetWizard() {
        resetSelections();
        hasExistingAssignments = false;
        wizardTitle = null;
        // Keep userID, isFirstAssignment, editingAssignmentId
    }

    // ==================== DEBUG / LOGGING ====================

    @Override
    @NonNull
    public String toString() {
        return "AssignmentWizardData{" +
                "userID=" + userId +
                ", isFirstAssignment=" + isFirstAssignment +
                ", editingAssignmentId='" + editingAssignmentId + '\'' +
                ", patternType=" + patternType +
                ", selectedTeam=" + (selectedTeam != null ? selectedTeam.getName() : null) +
                ", selectedCustomPattern=" + (selectedCustomPattern != null ? selectedCustomPattern.getName() : null) +
                ", assignmentDate=" + assignmentDate +
                ", cycleDayPosition=" + cycleDayPosition +
                ", hasExistingAssignments=" + hasExistingAssignments +
                ", isComplete=" + isWizardComplete() +
                '}';
    }
}