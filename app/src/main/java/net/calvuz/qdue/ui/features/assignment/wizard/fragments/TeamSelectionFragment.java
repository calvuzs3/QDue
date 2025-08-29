package net.calvuz.qdue.ui.features.assignment.wizard.fragments;

import android.content.Context;
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
import net.calvuz.qdue.databinding.FragmentTeamSelectionBinding;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.adapters.TeamSelectionAdapter;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;

import java.util.List;

/**
 * TeamSelectionFragment - Step 2A: Team Selection for QuattroDue Pattern
 *
 * <p>Second step of assignment wizard for QuattroDue patterns, allowing users
 * to select their team with proper qdue_offset configuration.</p>
 *
 * <h3>Team Requirements:</h3>
 * <ul>
 *   <li><strong>QuattroDue Compatible</strong>: Teams must have qdue_offset defined</li>
 *   <li><strong>Offset Range</strong>: Valid offset values for 16-day cycle positioning</li>
 *   <li><strong>Active Teams</strong>: Only active/enabled teams are selectable</li>
 * </ul>
 *
 * <h3>User Interface:</h3>
 * <ul>
 *   <li><strong>RecyclerView List</strong>: Scrollable list of available teams</li>
 *   <li><strong>Team Information</strong>: Name, offset, and cycle position details</li>
 *   <li><strong>Single Selection</strong>: Radio button selection model</li>
 *   <li><strong>Empty State</strong>: Fallback for no teams available</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Team Selection Fragment
 * @since Clean Architecture Phase 2
 */
public class TeamSelectionFragment extends Fragment {

    private static final String TAG = "TeamSelectionFragment";

    // ==================== UI COMPONENTS ====================

    private FragmentTeamSelectionBinding mBinding;
    private AssignmentWizardInterface mWizardInterface;
    private TeamSelectionAdapter mTeamAdapter;

    // ==================== STATE ====================

    private List<Team> mAvailableTeams;
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
        mBinding = FragmentTeamSelectionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupRecyclerView();
        loadAvailableTeams();

        Log.d(TAG, "TeamSelectionFragment view created");
    }

    @Override
    public void onResume() {
        super.onResume();
        mWizardInterface.onStepBecameVisible(1);
        restoreSelections();
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
        mBinding.txtStepTitle.setText(R.string.wizard_step_team_selection_title);
        mBinding.txtStepDescription.setText(R.string.wizard_step_team_selection_description);

        // Setup empty state
        mBinding.txtEmptyStateTitle.setText(R.string.no_teams_available_title);
        mBinding.txtEmptyStateDescription.setText(R.string.no_teams_available_description);
        mBinding.btnContactAdmin.setText(R.string.btn_contact_admin);

        // Setup loading state
        mBinding.txtLoadingMessage.setText(R.string.loading_teams);

        // Setup contact admin button
        mBinding.btnContactAdmin.setOnClickListener(v -> {
            // TODO: Implement contact admin functionality
            Log.d(TAG, "Contact admin button clicked");
        });

        // Set accessibility
        mBinding.recyclerViewTeams.setContentDescription(getString(R.string.accessibility_team_selection));
    }

    private void setupRecyclerView() {
        mBinding.recyclerViewTeams.setLayoutManager(new LinearLayoutManager(getContext()));

        mTeamAdapter = new TeamSelectionAdapter( new TeamSelectionAdapter.TeamSelectionCallback() {
            @Override
            public void onTeamSelected(@NonNull Team team) {

                Log.d(TAG, "Team selected: " + team.getName() + " (offset: " + team.getQdueOffset() + ")");
                mWizardInterface.onTeamSelected(team);

                // Provide haptic feedback
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    mBinding.getRoot().performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                }
            }
        } );

        mBinding.recyclerViewTeams.setAdapter(mTeamAdapter);
    }

    // ==================== DATA LOADING ====================

    private void loadAvailableTeams() {
        if (mIsLoading) return;

        mIsLoading = true;
        showLoadingState();

        Log.d(TAG, "Loading available teams for QuattroDue pattern");

        // Load teams through wizard interface dependencies
        mWizardInterface.getAssignmentUseCase()
                .getTeamsWithQuattroDueOffset()
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handleTeamsLoaded(result));
                    }
                });
    }

    private void handleTeamsLoaded(OperationResult<List<Team>> result) {
        mIsLoading = false;

        if (result.isSuccess()) {
            mAvailableTeams = result.getData();
            Log.d(TAG, "Loaded " + mAvailableTeams.size() + " teams with QuattroDue offset");

            if (mAvailableTeams.isEmpty()) {
                showEmptyState();
            } else {
                showTeamsList();
                mTeamAdapter.setTeams(mAvailableTeams);
                restoreSelections();
            }
        } else {
            Log.e(TAG, "Failed to load teams: " + result.getErrorMessage());
            showEmptyState(); // Show empty state on error too
        }
    }

    // ==================== UI STATE MANAGEMENT ====================

    private void showLoadingState() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
        mBinding.txtLoadingMessage.setVisibility(View.VISIBLE);
        mBinding.recyclerViewTeams.setVisibility(View.GONE);
        mBinding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void showTeamsList() {
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.txtLoadingMessage.setVisibility(View.GONE);
        mBinding.recyclerViewTeams.setVisibility(View.VISIBLE);
        mBinding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.txtLoadingMessage.setVisibility(View.GONE);
        mBinding.recyclerViewTeams.setVisibility(View.GONE);
        mBinding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    // ==================== STATE MANAGEMENT ====================

    private void restoreSelections() {
        if (mWizardInterface == null || mTeamAdapter == null) return;

        Team selectedTeam = mWizardInterface.getWizardData().getSelectedTeam();
        if (selectedTeam != null) {
            Log.d(TAG, "Restoring team selection: " + selectedTeam.getName());
            mTeamAdapter.setSelectedTeam(selectedTeam);
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Check if current fragment state is valid for navigation.
     * @return true if team is selected
     */
    public boolean isValid() {
        return mWizardInterface != null && mWizardInterface.getWizardData().hasTeamSelected();
    }

    /**
     * Get validation error message if fragment is invalid.
     * @return error message or null if valid
     */
    @Nullable
    public String getValidationError() {
        if (!isValid()) {
            return getString(R.string.validation_team_required);
        }
        return null;
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Programmatically select team.
     * Used for testing or external navigation.
     *
     * @param team Team to select
     */
    public void setSelectedTeam(@NonNull Team team) {
        if (mTeamAdapter != null) {
            mTeamAdapter.setSelectedTeam(team);
            mWizardInterface.onTeamSelected(team);
        }
    }

    /**
     * Get currently selected team.
     * @return Selected team or null
     */
    @Nullable
    public Team getSelectedTeam() {
        if (mWizardInterface != null) {
            return mWizardInterface.getWizardData().getSelectedTeam();
        }
        return null;
    }

    /**
     * Refresh teams list.
     * Used when teams might have been updated externally.
     */
    public void refreshTeams() {
        mAvailableTeams = null;
        loadAvailableTeams();
    }

    /**
     * Check if teams are currently being loaded.
     * @return true if loading in progress
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Get number of available teams.
     * @return number of teams or 0 if not loaded
     */
    public int getAvailableTeamsCount() {
        return mAvailableTeams != null ? mAvailableTeams.size() : 0;
    }
}