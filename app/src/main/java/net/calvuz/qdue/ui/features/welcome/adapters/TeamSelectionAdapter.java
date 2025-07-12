// TeamSelectionAdapter.java
package net.calvuz.qdue.ui.features.welcome.adapters;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.welcome.presentation.TeamSelectionFragment.Team;

import java.util.List;

/**
 * RecyclerView Adapter for team selection in 3x3 grid layout
 *
 * Features:
 * - Visual team cards with distinct colors
 * - Selection state management
 * - Smooth animations for selection feedback
 * - Team numbers 1-9 displayed in grid format
 * - Material 3 design with elevation changes
 */
public class TeamSelectionAdapter extends RecyclerView.Adapter<TeamSelectionAdapter.TeamViewHolder> {

    // Team selection listener interface
    public interface OnTeamSelectedListener {
        void onTeamSelected(int teamNumber);
    }

    private final List<Team> teams;
    private final OnTeamSelectedListener listener;
    private int selectedPosition = -1;

    /**
     * Constructor
     * @param teams List of team objects
     * @param listener Selection callback listener
     */
    public TeamSelectionAdapter(List<Team> teams, OnTeamSelectedListener listener) {
        this.teams = teams;
        this.listener = listener;

        // Find initially selected team
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).isSelected) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_selection, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.bind(team, position, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    /**
     * Update selected team and refresh affected items
     * @param teamIndex Selected team index (0-8)
     */
    public void updateSelectedTeam(int teamIndex) {
        if (teamIndex >= 0 && teamIndex < teams.size()) {
            int oldPosition = selectedPosition;
            selectedPosition = teamIndex; // Use direct index

            // Update old selection
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }

            // Update new selection
            notifyItemChanged(selectedPosition);
        }
    }

    /**
     * ViewHolder for team selection cards
     */
    class TeamViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView teamNumberText;
        private final TextView teamNameText;
        private final View selectionIndicator;
        private final View teamColorBar;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.team_card);
            teamNumberText = itemView.findViewById(R.id.team_number);
            teamNameText = itemView.findViewById(R.id.team_name);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);
            teamColorBar = itemView.findViewById(R.id.team_color_bar);
        }

        /**
         * Bind team data to views
         * @param team Team data object
         * @param position Item position
         * @param isSelected Whether this team is currently selected
         */
        public void bind(Team team, int position, boolean isSelected) {
            // Set team content
            teamNumberText.setText(String.valueOf(team.teamLetter));
            teamNameText.setText(team.teamName);

            // Set team color
            int teamColor = itemView.getContext().getColor(team.colorRes);
            teamColorBar.setBackgroundColor(teamColor);

            // Update selection state
            updateSelectionState(isSelected, teamColor);

            // Setup click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamSelected(team.teamIndex);
                    animateSelection();
                }
            });

            // Add entrance animation with staggered timing
            startEntranceAnimation(position);
        }

        /**
         * Update visual state based on selection
         * @param isSelected Whether this team is selected
         * @param teamColor Team's primary color
         */
        private void updateSelectionState(boolean isSelected, int teamColor) {
            if (isSelected) {
                // Selected state styling
                cardView.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.md_theme_light_primaryContainer)
                );
                cardView.setCardElevation(8f);
                cardView.setStrokeColor(teamColor);
                cardView.setStrokeWidth(3);

                selectionIndicator.setVisibility(View.VISIBLE);
                selectionIndicator.setBackgroundColor(teamColor);

                teamNumberText.setTextColor(teamColor);
                teamNameText.setTextColor(
                        itemView.getContext().getColor(R.color.md_theme_light_onPrimaryContainer)
                );

            } else {
                // Unselected state styling
                cardView.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.md_theme_light_surface)
                );
                cardView.setCardElevation(2f);
                cardView.setStrokeColor(
                        itemView.getContext().getColor(R.color.md_theme_light_outlineVariant)
                );
                cardView.setStrokeWidth(1);

                selectionIndicator.setVisibility(View.GONE);

                teamNumberText.setTextColor(
                        itemView.getContext().getColor(R.color.md_theme_light_onSurface)
                );
                teamNameText.setTextColor(
                        itemView.getContext().getColor(R.color.md_theme_light_onSurfaceVariant)
                );
            }
        }

        /**
         * Animate selection feedback
         */
        private void animateSelection() {
            // Scale animation
            cardView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        cardView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            // Pulse animation for team number
            ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(teamNumberText, "scaleX", 1f, 1.2f, 1f);
            pulseAnimator.setDuration(300);
            pulseAnimator.start();

            ObjectAnimator pulseAnimatorY = ObjectAnimator.ofFloat(teamNumberText, "scaleY", 1f, 1.2f, 1f);
            pulseAnimatorY.setDuration(300);
            pulseAnimatorY.start();
        }

        /**
         * Entrance animation with staggered timing
         * @param position Item position for delay calculation
         */
        private void startEntranceAnimation(int position) {
            // Initial state
            cardView.setScaleX(0.8f);
            cardView.setScaleY(0.8f);
            cardView.setAlpha(0f);

            // Calculate delay based on position (diagonal pattern)
            int row = position / 3;
            int col = position % 3;
            long delay = (row + col) * 100L;

            // Animate to final state
            cardView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(delay)
                    .start();
        }
    }
}