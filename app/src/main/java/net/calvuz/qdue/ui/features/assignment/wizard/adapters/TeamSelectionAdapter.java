package net.calvuz.qdue.ui.features.assignment.wizard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.databinding.ItemTeamSelectionNewBinding;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * TeamSelectionAdapter - Database-driven team selection with QuattroDue offset display
 *
 * <p>RecyclerView adapter for team selection that displays teams loaded from database
 * with their QuattroDue offset information. Supports single selection with visual feedback.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Database Integration</strong>: Uses Team domain model from database</li>
 *   <li><strong>QuattroDue Offset</strong>: Displays team offset information</li>
 *   <li><strong>Single Selection</strong>: Radio button style selection behavior</li>
 *   <li><strong>Selection Callback</strong>: Notifies parent when team is selected</li>
 *   <li><strong>Visual Feedback</strong>: Highlights selected team</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Clean Architecture Phase 2
 */
public class TeamSelectionAdapter extends RecyclerView.Adapter<TeamSelectionAdapter.TeamViewHolder> {

    private static final String TAG = "TeamSelectionAdapter";

    // ==================== DATA & CALLBACKS ====================

    private List<Team> mTeams;
    private Team mSelectedTeam;
    private final TeamSelectionCallback mCallback;

    // ==================== CALLBACK INTERFACE ====================

    public interface TeamSelectionCallback {
        void onTeamSelected(@NonNull Team team);
    }

    // ==================== CONSTRUCTOR ====================

    public TeamSelectionAdapter(@NonNull TeamSelectionCallback callback) {
        this.mCallback = callback;
        this.mTeams = new ArrayList<>();
        this.mSelectedTeam = null;
    }

    // ==================== DATA MANAGEMENT ====================

    public void setTeams(@NonNull List<Team> teams) {
        this.mTeams = new ArrayList<>(teams);
        notifyDataSetChanged();
        Log.d(TAG, "Teams updated: " + teams.size() + " teams available");
    }

    public void setSelectedTeam(@Nullable Team selectedTeam) {
        Team previousSelection = this.mSelectedTeam;
        this.mSelectedTeam = selectedTeam;

        // Update UI efficiently
        if (previousSelection != null) {
            int previousIndex = findTeamIndex(previousSelection);
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex);
            }
        }

        if (selectedTeam != null) {
            int currentIndex = findTeamIndex(selectedTeam);
            if (currentIndex >= 0) {
                notifyItemChanged(currentIndex);
            }
        }

        Log.d(TAG, "Selected team: " + (selectedTeam != null ? selectedTeam.getName() : "none"));
    }

    @Nullable
    public Team getSelectedTeam() {
        return mSelectedTeam;
    }

    private int findTeamIndex(@NonNull Team team) {
        for (int i = 0; i < mTeams.size(); i++) {
            if (mTeams.get(i).getName().equals(team.getName())) {
                return i;
            }
        }
        return -1;
    }

    // ==================== RECYCLERVIEW ADAPTER METHODS ====================

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTeamSelectionNewBinding binding = ItemTeamSelectionNewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TeamViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = mTeams.get(position);
        holder.bind(team);
    }

    @Override
    public int getItemCount() {
        return mTeams.size();
    }

    // ==================== VIEW HOLDER ====================

    public class TeamViewHolder extends RecyclerView.ViewHolder {

        private final ItemTeamSelectionNewBinding mBinding;

        public TeamViewHolder(@NonNull ItemTeamSelectionNewBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            // Set click listener
            mBinding.getRoot().setOnClickListener(this::onItemClick);
        }

        public void bind(@NonNull Team team) {
            // Team basic information
            mBinding.txtTeamName.setText(team.getDisplayName() != null ?
                    team.getDisplayName() : team.getName());
            mBinding.txtTeamId.setText("Team " + team.getName());

            // QuattroDue offset information
            int offset = team.getQdueOffset();
            mBinding.txtTeamOffset.setText("Offset: " + offset + " days");

            // Team description with offset explanation
            String description = team.getDescription();
            if (description == null || description.isEmpty()) {
                if (offset == 0) {
                    description = "Base QuattroDue pattern (no offset)";
                } else {
                    description = "QuattroDue pattern starts " + offset + " days after base pattern";
                }
            }
            mBinding.txtTeamDescription.setText(description);

            // Selection state
            boolean isSelected = mSelectedTeam != null && mSelectedTeam.getName().equals(team.getName());
            mBinding.radioTeamSelection.setChecked(isSelected);

            // Visual selection feedback
            mBinding.cardTeam.setSelected(isSelected);
            mBinding.cardTeam.setCardElevation(isSelected ? 8f : 2f);

            // Team color (if available)
            if (team.getColorHex() != null) {
                try {
                    int color = android.graphics.Color.parseColor(team.getColorHex());
                    mBinding.viewTeamColor.setBackgroundColor(color);
                    mBinding.viewTeamColor.setVisibility(View.VISIBLE);
                } catch (IllegalArgumentException e) {
                    mBinding.viewTeamColor.setVisibility(View.GONE);
                }
            } else {
                // Default color based on team name
                int defaultColor = getDefaultTeamColor(team.getName());
                mBinding.viewTeamColor.setBackgroundColor(defaultColor);
                mBinding.viewTeamColor.setVisibility(View.VISIBLE);
            }

            // Accessibility
            mBinding.getRoot().setContentDescription(
                    "Team " + team.getName() + ", offset " + offset + " days" +
                            (isSelected ? ", selected" : ""));
        }

        private void onItemClick(View view) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < mTeams.size()) {
                Team clickedTeam = mTeams.get(position);

                // Update selection
                setSelectedTeam(clickedTeam);

                // Notify callback
                mCallback.onTeamSelected(clickedTeam);

                Log.d(TAG, "Team selected: " + clickedTeam.getName() +
                        " (offset: " + clickedTeam.getQdueOffset() + ")");
            }
        }

        private int getDefaultTeamColor(@NonNull String teamName) {
            // Generate consistent colors for teams A-I
            switch (teamName) {
                case "A": return 0xFF4CAF50; // Green
                case "B": return 0xFF2196F3; // Blue
                case "C": return 0xFFFF9800; // Orange
                case "D": return 0xFF9C27B0; // Purple
                case "E": return 0xFFE91E63; // Pink
                case "F": return 0xFF00BCD4; // Cyan
                case "G": return 0xFF8BC34A; // Light Green
                case "H": return 0xFFFF5722; // Deep Orange
                case "I": return 0xFF795548; // Brown
                default: return 0xFF9E9E9E; // Grey
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear selection (for reset scenarios)
     */
    public void clearSelection() {
        setSelectedTeam(null);
    }

    /**
     * Get team by name for validation
     */
    @Nullable
    public Team getTeamByName(@NonNull String teamName) {
        for (Team team : mTeams) {
            if (team.getName().equals(teamName)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Check if adapter has teams
     */
    public boolean hasTeams() {
        return !mTeams.isEmpty();
    }

    /**
     * Get team count
     */
    public int getTeamCount() {
        return mTeams.size();
    }
}