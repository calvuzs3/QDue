package net.calvuz.qdue.smartshifts.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ActivityShiftSetupWizardBinding;
import net.calvuz.qdue.smartshifts.ui.main.SmartShiftsActivity;
import net.calvuz.qdue.smartshifts.ui.setup.fragments.WelcomeStepFragment;
import net.calvuz.qdue.smartshifts.ui.setup.fragments.PatternSelectionStepFragment;
import net.calvuz.qdue.smartshifts.ui.setup.fragments.StartDateStepFragment;
import net.calvuz.qdue.smartshifts.ui.setup.fragments.ConfirmationStepFragment;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Setup wizard activity for first-time configuration
 * Guides user through pattern selection and setup
 */
@AndroidEntryPoint
public class ShiftSetupWizardActivity extends AppCompatActivity {

    private ActivityShiftSetupWizardBinding binding;
    private SetupWizardViewModel viewModel;

    private List<Fragment> wizardSteps;
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityShiftSetupWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SetupWizardViewModel.class);

        setupToolbar();
        setupWizardSteps();
        setupNavigation();
        setupObservers();

        // Show first step
        if (savedInstanceState == null) {
            showCurrentStep();
        }
    }

    /**
     * Setup toolbar
     */
    private void setupToolbar() {
//        setSupportActionBar(binding.toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(R.string.setup_wizard_title);
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
    }

    /**
     * Setup wizard steps
     */
    private void setupWizardSteps() {
        wizardSteps = Arrays.asList(
                new WelcomeStepFragment(),
                new PatternSelectionStepFragment(),
                new StartDateStepFragment(),
                new ConfirmationStepFragment()
        );
    }

    /**
     * Setup navigation buttons
     */
    private void setupNavigation() {
        binding.btnPrevious.setOnClickListener(v -> goToPreviousStep());
        binding.btnNext.setOnClickListener(v -> goToNextStep());
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        viewModel.isSetupComplete().observe(this, isComplete -> {
            if (isComplete) {
                finishSetup();
            }
        });

        viewModel.canProceed().observe(this, canProceed -> {
            binding.btnNext.setEnabled(canProceed);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                showError(errorMessage);
                viewModel.clearErrorMessage();
            }
        });
    }

    /**
     * Show current step fragment
     */
    private void showCurrentStep() {
        if (currentStep >= 0 && currentStep < wizardSteps.size()) {
            Fragment fragment = wizardSteps.get(currentStep);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

            updateNavigation();
            updateProgress();
        }
    }

    /**
     * Go to next step
     */
    private void goToNextStep() {
        if (currentStep < wizardSteps.size() - 1) {
            // Validate current step
            if (viewModel.validateCurrentStep(currentStep)) {
                currentStep++;
                showCurrentStep();
            }
        } else {
            // Complete setup
            viewModel.completeSetup();
        }
    }

    /**
     * Go to previous step
     */
    private void goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            showCurrentStep();
        }
    }

    /**
     * Update navigation buttons
     */
    private void updateNavigation() {
        // Previous button
        binding.btnPrevious.setVisibility(currentStep > 0 ? android.view.View.VISIBLE : android.view.View.GONE);

        // Next button text
        if (currentStep == wizardSteps.size() - 1) {
            binding.btnNext.setText(R.string.btn_finish);
        } else {
            binding.btnNext.setText(R.string.btn_next);
        }
    }

    /**
     * Update progress indicator
     */
    private void updateProgress() {
        int progress = (int) (((float) (currentStep + 1) / wizardSteps.size()) * 100);
        binding.setupProgress.setProgress(progress);
    }

    /**
     * Finish setup and go to main activity
     */
    private void finishSetup() {
        Intent intent = new Intent(this, SmartShiftsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        if (currentStep > 0) {
            goToPreviousStep();
        } else {
            showExitConfirmation();
        }
    }

    /**
     * Handle up navigation
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show exit confirmation dialog
     */
    private void showExitConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_exit_setup_title)
                .setMessage(R.string.dialog_exit_setup_message)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        com.google.android.material.snackbar.Snackbar.make(
                binding.getRoot(),
                message,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
