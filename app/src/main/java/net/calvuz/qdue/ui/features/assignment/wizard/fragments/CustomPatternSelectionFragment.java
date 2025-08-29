package net.calvuz.qdue.ui.features.assignment.wizard.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.databinding.FragmentCustomPatternSelectionBinding;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.adapters.CustomPatternSelectionAdapter;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;
import net.calvuz.qdue.ui.features.schedulepattern.UserSchedulePatternCreationActivity;

import java.util.List;

/**
 * CustomPatternSelectionFragment - Step 2B: Custom Pattern Selection
 *
 * <p>Second step of assignment wizard for custom patterns, allowing users
 * to select from their previously created RecurrenceRule templates.</p>
 *
 * <h3>Custom Pattern Requirements:</h3>
 * <ul>
 *   <li><strong>User Created</strong>: Only patterns created by the current user</li>
 *   <li><strong>Active Status</strong>: Only active/enabled RecurrenceRules are selectable</li>
 *   <li><strong>Valid Configuration</strong>: Patterns must have complete cycle definitions</li>
 * </ul>
 *
 * <h3>User Interface:</h3>
 * <ul>
 *   <li><strong>RecyclerView List</strong>: Scrollable list of available custom patterns</li>
 *   <li><strong>Pattern Information</strong>: Name, cycle length, work/rest ratios</li>
 *   <li><strong>Single Selection</strong>: Radio button selection model</li>
 *   <li><strong>Create New Pattern</strong>: Quick access to pattern creation</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Custom Pattern Selection Fragment
 * @since Clean Architecture Phase 2
 */
public class CustomPatternSelectionFragment extends Fragment {

    private static final String TAG = "CustomPatternSelectionFragment";

    // ==================== UI COMPONENTS ====================

    private FragmentCustomPatternSelectionBinding mBinding;
    private AssignmentWizardInterface mWizardInterface;
    private CustomPatternSelectionAdapter mPatternAdapter;

    // ==================== STATE ====================

    private List<RecurrenceRule> mAvailablePatterns;
    private boolean mIsLoading = false;

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
        mBinding = FragmentCustomPatternSelectionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupRecyclerView();
        loadAvailablePatterns();

        Log.d(TAG, "CustomPatternSelectionFragment view created");
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
        mBinding.txtStepTitle.setText(R.string.wizard_step_custom_pattern_selection_title);
        mBinding.txtStepDescription.setText(R.string.wizard_step_custom_pattern_selection_description);

        // Setup empty state
        mBinding.txtEmptyStateTitle.setText(R.string.no_custom_patterns_title);
        mBinding.txtEmptyStateDescription.setText(R.string.no_custom_patterns_description);
        mBinding.btnCreateCustomPattern.setText(R.string.btn_create_custom_pattern);

        // Setup loading state
        mBinding.txtLoadingMessage.setText(R.string.loading_custom_patterns);

        // Setup create pattern button
        mBinding.btnCreateCustomPattern.setOnClickListener(v -> {
            openPatternCreationActivity();
        });

        // Set accessibility
        mBinding.recyclerViewPatterns.setContentDescription(
                getString(R.string.accessibility_custom_pattern_selection));
    }

    private void setupRecyclerView() {
        mBinding.recyclerViewPatterns.setLayoutManager(new LinearLayoutManager(getContext()));

        mPatternAdapter = new CustomPatternSelectionAdapter( new CustomPatternSelectionAdapter.CustomPatternSelectionCallback() {
            @Override
            public void onCustomPatternSelected(@NonNull RecurrenceRule pattern) {
                Log.d(TAG, "Custom pattern selected: " + pattern.getName() +
                        " (cycle length: " + pattern.getCycleLength() + ")");
                mWizardInterface.onCustomPatternSelected(pattern);

                // Provide haptic feedback
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    mBinding.getRoot().performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                }
            }
        } );

        mBinding.recyclerViewPatterns.setAdapter(mPatternAdapter);
    }

    // ==================== DATA LOADING ====================

    private void loadAvailablePatterns() {
        if (mIsLoading) return;

        mIsLoading = true;
        showLoadingState();

        Log.d(TAG, "Loading available custom patterns for user");

        // Load custom patterns through wizard interface dependencies
        mWizardInterface.getAssignmentUseCase()
                .getActiveUserRecurrenceRules(mWizardInterface.getWizardData().getUserId())
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handlePatternsLoaded(result));
                    }
                });
    }

    private void handlePatternsLoaded(OperationResult<List<RecurrenceRule>> result) {
        mIsLoading = false;

        if (result.isSuccess()) {
            mAvailablePatterns = result.getData();
            Log.d(TAG, "Loaded " + mAvailablePatterns.size() + " custom patterns");

            if (mAvailablePatterns.isEmpty()) {
                showEmptyState();
            } else {
                showPatternsList();
                mPatternAdapter.setCustomPatterns(mAvailablePatterns);
                restoreSelections();
            }
        } else {
            Log.e(TAG, "Failed to load custom patterns: " + result.getErrorMessage());
            showEmptyState(); // Show empty state on error too
        }
    }

    // ==================== UI STATE MANAGEMENT ====================

    private void showLoadingState() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
        mBinding.txtLoadingMessage.setVisibility(View.VISIBLE);
        mBinding.recyclerViewPatterns.setVisibility(View.GONE);
        mBinding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void showPatternsList() {
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.txtLoadingMessage.setVisibility(View.GONE);
        mBinding.recyclerViewPatterns.setVisibility(View.VISIBLE);
        mBinding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.txtLoadingMessage.setVisibility(View.GONE);
        mBinding.recyclerViewPatterns.setVisibility(View.GONE);
        mBinding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    // ==================== NAVIGATION ====================

    private void openPatternCreationActivity() {
        Log.d(TAG, "Opening pattern creation activity");

        Intent intent = UserSchedulePatternCreationActivity.createIntent(getContext());
        startActivity(intent);

        // Note: When user returns, patterns will be refreshed in onResume
    }

    @Override
    public void onResume() {
        super.onResume();
        mWizardInterface.onStepBecameVisible(1);

        // Refresh patterns in case new ones were created
        if (mAvailablePatterns != null) {
            refreshPatterns();
        }

        restoreSelections();
    }
//    @Override
//    public void onResume() {
//        super.onResume();
//        mWizardInterface.onStepBecameVisible(1);
//        restoreSelections();
//    }

    // ==================== STATE MANAGEMENT ====================

    private void restoreSelections() {
        if (mWizardInterface == null || mPatternAdapter == null) return;

        RecurrenceRule selectedPattern = mWizardInterface.getWizardData().getSelectedCustomPattern();
        if (selectedPattern != null) {
            Log.d(TAG, "Restoring custom pattern selection: " + selectedPattern.getName());
            mPatternAdapter.setSelectedPattern(selectedPattern);
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Check if current fragment state is valid for navigation.
     * @return true if custom pattern is selected
     */
    public boolean isValid() {
        return mWizardInterface != null && mWizardInterface.getWizardData().hasCustomPatternSelected();
    }

    /**
     * Get validation error message if fragment is invalid.
     * @return error message or null if valid
     */
    @Nullable
    public String getValidationError() {
        if (!isValid()) {
            return getString(R.string.validation_custom_pattern_required);
        }
        return null;
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Programmatically select custom pattern.
     * Used for testing or external navigation.
     *
     * @param pattern Pattern to select
     */
    public void setSelectedPattern(@NonNull RecurrenceRule pattern) {
        if (mPatternAdapter != null) {
            mPatternAdapter.setSelectedPattern(pattern);
            mWizardInterface.onCustomPatternSelected(pattern);
        }
    }

    /**
     * Get currently selected custom pattern.
     * @return Selected pattern or null
     */
    @Nullable
    public RecurrenceRule getSelectedPattern() {
        if (mWizardInterface != null) {
            return mWizardInterface.getWizardData().getSelectedCustomPattern();
        }
        return null;
    }

    /**
     * Refresh patterns list.
     * Used when patterns might have been updated externally.
     */
    public void refreshPatterns() {
        mAvailablePatterns = null;
        loadAvailablePatterns();
    }

    /**
     * Check if patterns are currently being loaded.
     * @return true if loading in progress
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Get number of available patterns.
     * @return number of patterns or 0 if not loaded
     */
    public int getAvailablePatternsCount() {
        return mAvailablePatterns != null ? mAvailablePatterns.size() : 0;
    }

    /**
     * Check if user has any custom patterns available.
     * @return true if patterns are available
     */
    public boolean hasAvailablePatterns() {
        return mAvailablePatterns != null && !mAvailablePatterns.isEmpty();
    }
}