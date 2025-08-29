package net.calvuz.qdue.ui.features.assignment.wizard.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.databinding.FragmentConfirmationBinding;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ConfirmationFragment - Step 4: Assignment Confirmation and Creation
 *
 * <p>Final step of assignment wizard displaying complete assignment summary
 * and handling the actual assignment creation process.</p>
 *
 * <h3>Confirmation Display:</h3>
 * <ul>
 *   <li><strong>Assignment Summary</strong>: Complete overview of selections</li>
 *   <li><strong>Pattern Details</strong>: Type-specific information display</li>
 *   <li><strong>Date and Position</strong>: Start date and cycle position confirmation</li>
 *   <li><strong>Conflict Warnings</strong>: Existing assignments that will be affected</li>
 * </ul>
 *
 * <h3>Creation Process:</h3>
 * <ul>
 *   <li><strong>Final Validation</strong>: Complete wizard state verification</li>
 *   <li><strong>Assignment Creation</strong>: Uses CreatePatternAssignmentUseCase</li>
 *   <li><strong>Progress Indication</strong>: Loading states during creation</li>
 *   <li><strong>Result Handling</strong>: Success/error feedback to user</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Confirmation Fragment
 * @since Clean Architecture Phase 2
 */
public class ConfirmationFragment extends Fragment {

    private static final String TAG = "ConfirmationFragment";

    // ==================== UI COMPONENTS ====================

    private FragmentConfirmationBinding mBinding;
    private AssignmentWizardInterface mWizardInterface;

    // ==================== STATE ====================

    private boolean mIsCreatingAssignment = false;
    private List<UserScheduleAssignment> mExistingAssignments;

    // ==================== FORMATTERS ====================

    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

    // ==================== FRAGMENT LIFECYCLE ====================

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AssignmentWizardInterface) {
            mWizardInterface = (AssignmentWizardInterface) context;
        } else {
            throw new RuntimeException("Activity must implement AssignmentWizardInterface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentConfirmationBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupClickListeners();
        loadExistingAssignments();

        Log.d(TAG, "ConfirmationFragment view created");
    }

    @Override
    public void onResume() {
        super.onResume();
        mWizardInterface.onStepBecameVisible(3);
        updateSummaryDisplay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mWizardInterface = null;
    }

    // ==================== UI SETUP ====================

    private void setupUI() {
        // Set internationalized strings
        mBinding.txtStepTitle.setText(R.string.wizard_step_confirmation_title);
        mBinding.txtStepDescription.setText(R.string.wizard_step_confirmation_description);

        // Summary card
        mBinding.txtSummaryTitle.setText(R.string.confirmation_assignment_summary);

        // Warning card
        mBinding.txtExistingAssignmentWarning.setText(R.string.warning_existing_assignments_will_be_closed);

        // Action buttons
        mBinding.btnEditSettings.setText(R.string.btn_edit_settings);
        mBinding.btnConfirmCreate.setText(R.string.btn_confirm_create);

        // Initially hide warning until we check for existing assignments
        mBinding.warningExistingAssignments.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        mBinding.btnEditSettings.setOnClickListener(v -> {
            // Navigate back to allow editing
            mWizardInterface.onNavigateToPreviousStep();
        });

        mBinding.btnConfirmCreate.setOnClickListener(v -> {
            if (!mIsCreatingAssignment) {
                createAssignment();
            }
        });
    }

    // ==================== SUMMARY DISPLAY ====================

    private void updateSummaryDisplay() {
        if (mWizardInterface == null) return;

        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        if (!wizardData.isWizardComplete()) {
            showIncompleteWarning();
            return;
        }

        updatePatternTypeSummary(wizardData);
        updateSelectionSummary(wizardData);
        updateDateSummary(wizardData);
        updateCyclePositionSummary(wizardData);
        updateFullDescription(wizardData);
    }

    private void updatePatternTypeSummary(AssignmentWizardData wizardData) {
        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.QUATTRODUE) {
            mBinding.txtPatternTypeSummary.setText(R.string.summary_pattern_quattrodue);
        } else {
            mBinding.txtPatternTypeSummary.setText(R.string.summary_pattern_custom);
        }
    }

    private void updateSelectionSummary(AssignmentWizardData wizardData) {
        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.QUATTRODUE) {
            // Show team summary
            if (wizardData.getSelectedTeam() != null) {
                String teamSummary = getString(R.string.summary_team_format,
                        wizardData.getSelectedTeam().getName());
                mBinding.txtTeamSummary.setText(teamSummary);
                mBinding.txtTeamSummary.setVisibility(View.VISIBLE);

//                if (wizardData.getSelectedTeam().getQdueOffset() != null) {
                    String offsetSummary = getString(R.string.summary_team_offset_format,
                            wizardData.getSelectedTeam().getQdueOffset());
                    mBinding.txtTeamOffset.setText(offsetSummary);
                    mBinding.txtTeamOffset.setVisibility(View.VISIBLE);
//                } else {
//                    mBinding.txtTeamOffset.setVisibility(View.GONE);
//                }
            }

            mBinding.txtCustomPatternSummary.setVisibility(View.GONE);
            mBinding.txtPatternCycleLength.setVisibility(View.GONE);

        } else {
            // Show custom pattern summary
            if (wizardData.getSelectedCustomPattern() != null) {
                String patternSummary = getString(R.string.summary_custom_pattern_format,
                        wizardData.getSelectedCustomPattern().getName());
                mBinding.txtCustomPatternSummary.setText(patternSummary);
                mBinding.txtCustomPatternSummary.setVisibility(View.VISIBLE);

                String cycleSummary = getString(R.string.summary_cycle_length_format,
                        wizardData.getSelectedCustomPattern().getCycleLength());
                mBinding.txtPatternCycleLength.setText(cycleSummary);
                mBinding.txtPatternCycleLength.setVisibility(View.VISIBLE);
            }

            mBinding.txtTeamSummary.setVisibility(View.GONE);
            mBinding.txtTeamOffset.setVisibility(View.GONE);
        }
    }

    private void updateDateSummary(AssignmentWizardData wizardData) {
        if (wizardData.getAssignmentDate() != null) {
            String dateSummary = getString(R.string.summary_start_date_format,
                    wizardData.getAssignmentDate().format(DATE_DISPLAY_FORMATTER));
            mBinding.txtDateSummary.setText(dateSummary);
        }
    }

    private void updateCyclePositionSummary(AssignmentWizardData wizardData) {
        String positionSummary = getString(R.string.summary_cycle_position_format_one,
                wizardData.getCycleDayPosition());
        mBinding.txtCyclePositionSummary.setText(positionSummary);
    }

    private void updateFullDescription(AssignmentWizardData wizardData) {
        String description = wizardData.getAssignmentSummary();
        mBinding.txtFullDescription.setText(description);
    }

    private void showIncompleteWarning() {
        mBinding.txtPatternTypeSummary.setText(R.string.validation_wizard_incomplete);
        mBinding.btnConfirmCreate.setEnabled(false);
    }

    // ==================== EXISTING ASSIGNMENTS CHECK ====================

    private void loadExistingAssignments() {
        if (mWizardInterface == null) return;

        Log.d(TAG, "Checking for existing assignments");

        Long userId = mWizardInterface.getWizardData().getUserId();
        if (userId == null) return;

        mWizardInterface.getAssignmentUseCase()
                .getUserActiveAssignments(userId)
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handleExistingAssignmentsLoaded(result));
                    }
                });
    }

    private void handleExistingAssignmentsLoaded(OperationResult<List<UserScheduleAssignment>> result) {
        if (result.isSuccess()) {
            mExistingAssignments = result.getData();

            if (mExistingAssignments != null && !mExistingAssignments.isEmpty()) {
                Log.d(TAG, "Found " + mExistingAssignments.size() + " existing assignments");
                showExistingAssignmentsWarning();
            } else {
                Log.d(TAG, "No existing assignments found");
                mBinding.warningExistingAssignments.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "Failed to load existing assignments: " + result.getErrorMessage());
            // Don't show warning on error - assume no conflicts
            mBinding.warningExistingAssignments.setVisibility(View.GONE);
        }
    }

    private void showExistingAssignmentsWarning() {
        if (mExistingAssignments == null || mExistingAssignments.isEmpty()) return;

        String warningDetails = getString(R.string.warning_existing_assignments_details,
                mExistingAssignments.size());
        mBinding.txtExistingAssignmentWarning.setText(
                MessageFormat.format( getString(R.string.warning_existing_assignments_will_be_closed_format), warningDetails) );

        mBinding.warningExistingAssignments.setVisibility(View.VISIBLE);
    }

    // ==================== ASSIGNMENT CREATION ====================

    private void createAssignment() {
        if (mIsCreatingAssignment) return;

        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        if (!wizardData.isValidForAssignmentCreation()) {
            showError(wizardData.validateCurrentState());
            return;
        }

        mIsCreatingAssignment = true;
        showCreationProgress(true);

        Log.d(TAG, "Starting assignment creation process");

        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.QUATTRODUE) {
            createQuattroDueAssignment(wizardData);
        } else {
            createCustomPatternAssignment(wizardData);
        }
    }

    private void createQuattroDueAssignment(AssignmentWizardData wizardData) {
        mWizardInterface.getAssignmentUseCase()
                .createQuattroDueAssignment(
                        wizardData.getUserId(),
                        wizardData.getSelectedTeam().getName(),
                        wizardData.getAssignmentDate(),
                        wizardData.getCycleDayPosition()
                )
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handleAssignmentCreationResult(result));
                    }
                });
    }

    private void createCustomPatternAssignment(AssignmentWizardData wizardData) {
        mWizardInterface.getAssignmentUseCase()
                .createCustomPatternAssignment(
                        wizardData.getUserId(),
                        wizardData.getSelectedCustomPattern().getId(),
                        "A", // Default team for custom patterns
                        wizardData.getAssignmentDate(),
                        wizardData.getCycleDayPosition()
                )
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handleAssignmentCreationResult(result));
                    }
                });
    }

    private void handleAssignmentCreationResult(OperationResult<UserScheduleAssignment> result) {
        mIsCreatingAssignment = false;
        showCreationProgress(false);

        if (result.isSuccess()) {
            Log.d(TAG, "Assignment created successfully: " + result.getData().getId());
            showSuccess(getString(R.string.success_assignment_created));

            // Notify parent activity of success
            if (getActivity() != null) {
                getActivity().setResult(android.app.Activity.RESULT_OK);
                getActivity().finish();
            }
        } else {
            Log.e(TAG, "Assignment creation failed: " + result.getErrorMessage());
            showError(getString(R.string.error_assignment_creation_failed, result.getErrorMessage()));
        }
    }

    // ==================== UI FEEDBACK ====================

    private void showCreationProgress(boolean show) {
        if (show) {
            mBinding.progressBar.setVisibility(View.VISIBLE);
            mBinding.txtCreatingMessage.setVisibility(View.VISIBLE);
            mBinding.txtCreatingMessage.setText(R.string.creating_assignment);
            mBinding.btnConfirmCreate.setEnabled(false);
            mBinding.btnEditSettings.setEnabled(false);
        } else {
            mBinding.progressBar.setVisibility(View.GONE);
            mBinding.txtCreatingMessage.setVisibility(View.GONE);
            mBinding.btnConfirmCreate.setEnabled(true);
            mBinding.btnEditSettings.setEnabled(true);
        }
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    // ==================== VALIDATION ====================

    public boolean isValid() {
        return mWizardInterface != null && mWizardInterface.getWizardData().isWizardComplete();
    }

    @Nullable
    public String getValidationError() {
        if (mWizardInterface != null) {
            return mWizardInterface.getWizardData().validateCurrentState();
        }
        return "Wizard interface not available";
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Check if assignment creation is in progress.
     * @return true if creation is running
     */
    public boolean isCreatingAssignment() {
        return mIsCreatingAssignment;
    }

    /**
     * Get existing assignments that will be affected.
     * @return List of existing assignments or null if not loaded
     */
    @Nullable
    public List<UserScheduleAssignment> getExistingAssignments() {
        return mExistingAssignments;
    }

    /**
     * Force refresh of existing assignments check.
     */
    public void refreshExistingAssignments() {
        loadExistingAssignments();
    }
}