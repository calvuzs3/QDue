package net.calvuz.qdue.ui.features.welcome.presentation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.FragmentPatternAssignmentOnboardingBinding;
import net.calvuz.qdue.ui.features.assignment.wizard.PatternAssignmentWizardActivity;
import net.calvuz.qdue.ui.features.welcome.interfaces.WelcomeInterface;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * PatternAssignmentOnboardingFragment - Onboarding step for pattern assignment
 */
public class PatternAssignmentOnboardingFragment extends Fragment {

    private static final String TAG = "PatternAssignmentOnboardingFragment";

    private FragmentPatternAssignmentOnboardingBinding mBinding;
    private WelcomeInterface mWelcomeInterface;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWelcomeInterface = (WelcomeInterface) requireActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentPatternAssignmentOnboardingBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupActions();
    }

    private void setupUI() {
        // Setup introductory content explaining pattern assignment
        mBinding.txtTitle.setText(R.string.onboarding_pattern_assignment_title);
        mBinding.txtDescription.setText(R.string.onboarding_pattern_assignment_description);
    }

    private void setupActions() {
        // Launch full wizard
        mBinding.btnAssignNow.setOnClickListener(v -> launchPatternWizard());

        // Skip for now - continue to next onboarding step
        mBinding.btnSkipForNow.setOnClickListener(v -> skipPatternAssignment());
    }

    private void launchPatternWizard() {
        Intent intent = PatternAssignmentWizardActivity.createIntent(requireContext(), true);
        startActivityForResult(intent, QDue.REQUEST_PATTERN_ASSIGNMENT);
    }

    private void skipPatternAssignment() {
        // Mark as completed but with default assignment
        Log.d(TAG, "Pattern assignment skipped during onboarding");

        // Continue to next welcome step
        if (mWelcomeInterface != null) {
            mWelcomeInterface.onPatternAssignmentCompleted(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QDue.REQUEST_PATTERN_ASSIGNMENT) {
            boolean success = resultCode == Activity.RESULT_OK;

            if (mWelcomeInterface != null) {
                mWelcomeInterface.onPatternAssignmentCompleted(success);
            }

            Log.d(TAG, "Pattern assignment completed: " + success);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}

