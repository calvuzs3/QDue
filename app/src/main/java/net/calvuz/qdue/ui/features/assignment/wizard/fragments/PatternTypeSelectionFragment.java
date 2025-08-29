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
import net.calvuz.qdue.databinding.FragmentPatternTypeSelectionBinding;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

/**
 * PatternTypeSelectionFragment - Step 1: Pattern Type Selection
 *
 * <p>First step of assignment wizard allowing users to choose between
 * QuattroDue standard pattern and custom user-defined patterns.</p>
 *
 * <h3>Pattern Types:</h3>
 * <ul>
 *   <li><strong>QuattroDue</strong>: Standard 4-on-2-off shift pattern with team-based scheduling</li>
 *   <li><strong>Custom</strong>: User-defined patterns using existing RecurrenceRule templates</li>
 * </ul>
 *
 * <h3>User Interface:</h3>
 * <ul>
 *   <li><strong>Radio Button Selection</strong>: Clear choice between pattern types</li>
 *   <li><strong>Descriptive Information</strong>: Detailed explanation of each pattern type</li>
 *   <li><strong>Visual Indicators</strong>: Icons and formatting to distinguish options</li>
 *   <li><strong>Immediate Feedback</strong>: Selection immediately updates wizard state</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Pattern Type Selection Fragment
 * @since Clean Architecture Phase 2
 */
public class PatternTypeSelectionFragment extends Fragment {

    private static final String TAG = "PatternTypeSelectionFragment";

    // ==================== UI COMPONENTS ====================

    private FragmentPatternTypeSelectionBinding mBinding;
    private AssignmentWizardInterface mWizardInterface;

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
        mBinding = FragmentPatternTypeSelectionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupClickListeners();
        restoreSelections();

        Log.d(TAG, "PatternTypeSelectionFragment view created");
    }

    @Override
    public void onResume() {
        super.onResume();
        mWizardInterface.onStepBecameVisible(0);
        updateUI();
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
        mBinding.txtStepTitle.setText(R.string.wizard_step_pattern_type_title);
        mBinding.txtStepDescription.setText(R.string.wizard_step_pattern_type_description);

        // Setup QuattroDue option
        mBinding.txtQuattroDueTitle.setText(R.string.pattern_type_quattrodue_title);
        mBinding.txtQuattroDueDescription.setText(R.string.pattern_type_quattrodue_description);
        mBinding.txtQuattroDueDetails.setText(R.string.pattern_type_quattrodue_details);

        // Setup Custom option
        mBinding.txtCustomTitle.setText(R.string.pattern_type_custom_title);
        mBinding.txtCustomDescription.setText(R.string.pattern_type_custom_description);
        mBinding.txtCustomDetails.setText(R.string.pattern_type_custom_details);

        // Set accessibility content descriptions
        mBinding.cardQuattroDue.setContentDescription(
                getString(R.string.accessibility_pattern_type_selection) + " - " +
                        getString(R.string.pattern_type_quattrodue_title));
        mBinding.cardCustom.setContentDescription(
                getString(R.string.accessibility_pattern_type_selection) + " - " +
                        getString(R.string.pattern_type_custom_title));
    }

    private void setupClickListeners() {
        // QuattroDue pattern selection
        mBinding.cardQuattroDue.setOnClickListener(v -> selectPatternType(AssignmentWizardData.PatternType.QUATTRODUE) );

        // Custom pattern selection
        mBinding.cardCustom.setOnClickListener(v -> selectPatternType(AssignmentWizardData.PatternType.CUSTOM) );

        // Radio button clicks (delegate to card clicks)
        mBinding.radioQuattroDue.setOnClickListener(v -> selectPatternType(AssignmentWizardData.PatternType.QUATTRODUE) );

        mBinding.radioCustom.setOnClickListener(v -> selectPatternType(AssignmentWizardData.PatternType.CUSTOM) );
    }

    // ==================== PATTERN SELECTION ====================

    private void selectPatternType(AssignmentWizardData.PatternType patternType) {
        Log.d(TAG, "Pattern type selected: " + patternType);

        // Update radio button states
        mBinding.radioQuattroDue.setChecked(patternType == AssignmentWizardData.PatternType.QUATTRODUE);
        mBinding.radioCustom.setChecked(patternType == AssignmentWizardData.PatternType.CUSTOM);

        // Update card selection states
        mBinding.cardQuattroDue.setChecked(patternType == AssignmentWizardData.PatternType.QUATTRODUE);
        mBinding.cardCustom.setChecked(patternType == AssignmentWizardData.PatternType.CUSTOM);

        // Notify wizard interface
        mWizardInterface.onPatternTypeSelected(patternType);

        // Provide haptic feedback
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            mBinding.getRoot().performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        }

        Log.d(TAG, "Pattern type selection completed and wizard notified");
    }

    // ==================== STATE MANAGEMENT ====================

    private void restoreSelections() {
        if (mWizardInterface == null) return;

        AssignmentWizardData wizardData = mWizardInterface.getWizardData();
        AssignmentWizardData.PatternType selectedType = wizardData.getPatternType();

        if (selectedType != null) {
            Log.d(TAG, "Restoring pattern type selection: " + selectedType);
            selectPatternType(selectedType);
        }
    }

    private void updateUI() {
        if (mBinding == null || mWizardInterface == null) return;

        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        // Update selection states based on current wizard data
        AssignmentWizardData.PatternType currentSelection = wizardData.getPatternType();

        mBinding.radioQuattroDue.setChecked(currentSelection == AssignmentWizardData.PatternType.QUATTRODUE);
        mBinding.radioCustom.setChecked(currentSelection == AssignmentWizardData.PatternType.CUSTOM);
        mBinding.cardQuattroDue.setChecked(currentSelection == AssignmentWizardData.PatternType.QUATTRODUE);
        mBinding.cardCustom.setChecked(currentSelection == AssignmentWizardData.PatternType.CUSTOM);

        // Update UI based on editing mode
        if (wizardData.isEditingMode()) {
            mBinding.txtStepTitle.setText(R.string.title_change_assignment_wizard);
        } else if (wizardData.isFirstAssignment()) {
            mBinding.txtStepTitle.setText(R.string.title_first_assignment_wizard);
        }

        Log.d(TAG, "UI updated with current pattern type: " + currentSelection);
    }

    // ==================== VALIDATION ====================

    /**
     * Check if current fragment state is valid for navigation.
     * @return true if pattern type is selected
     */
    public boolean isValid() {
        return mWizardInterface != null && mWizardInterface.getWizardData().hasPatternTypeSelected();
    }

    /**
     * Get validation error message if fragment is invalid.
     * @return error message or null if valid
     */
    @Nullable
    public String getValidationError() {
        if (!isValid()) {
            return getString(R.string.validation_pattern_type_required);
        }
        return null;
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Programmatically select pattern type.
     * Used for testing or external navigation.
     *
     * @param patternType Pattern type to select
     */
    public void setPatternType(@NonNull AssignmentWizardData.PatternType patternType) {
        selectPatternType(patternType);
    }

    /**
     * Get currently selected pattern type.
     * @return Selected pattern type or null
     */
    @Nullable
    public AssignmentWizardData.PatternType getSelectedPatternType() {
        if (mWizardInterface != null) {
            return mWizardInterface.getWizardData().getPatternType();
        }
        return null;
    }
}