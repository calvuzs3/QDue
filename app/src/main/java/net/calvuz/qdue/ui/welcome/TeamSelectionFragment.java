package net.calvuz.qdue.ui.welcome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Team Selection Fragment - Second step of welcome flow
 * <p></p>
 * Allows user to select their team from 9 available teams.
 * Each team follows the "quattro-due" (4-2) shift system:
 * - 4 consecutive work days
 * - 2 consecutive rest days
 * <p></p>
 * Teams are numbered 1-9 and displayed in a grid layout
 * with visual indicators for the shift cycle.
 */
public class TeamSelectionFragment extends Fragment implements TeamSelectionAdapter.OnTeamSelectedListener {

    // Team data class
    public static class Team {
        public final int teamNumber;
        public final String teamName;
        public final String description;
        public final int colorRes;
        public final boolean isSelected;

        public Team(int teamNumber, String teamName, String description, int colorRes, boolean isSelected) {
            this.teamNumber = teamNumber;
            this.teamName = teamName;
            this.description = description;
            this.colorRes = colorRes;
            this.isSelected = isSelected;
        }
    }

    // View components
    private RecyclerView teamsRecyclerView;
    private TeamSelectionAdapter adapter;
    private TextView titleText;
    private TextView subtitleText;
    private MaterialCardView explanationCard;

    // Selected team tracking
    private int selectedTeamNumber = -1;

    // Preferences
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_team_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupTeamsList();
        startEntranceAnimation();
    }

    /**
     * Initialize view components
     */
    private void initializeViews(View view) {
        titleText = view.findViewById(R.id.team_title);
        subtitleText = view.findViewById(R.id.team_subtitle);
        explanationCard = view.findViewById(R.id.explanation_card);
        teamsRecyclerView = view.findViewById(R.id.teams_recycler_view);

        // Get preferences from parent activity
        if (getActivity() != null) {
            preferences = getActivity().getSharedPreferences("qdue_welcome_prefs", getActivity().MODE_PRIVATE);
            selectedTeamNumber = preferences.getInt("selected_team", -1);
        }

        // Setup RecyclerView with grid layout (3 columns)
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        teamsRecyclerView.setLayoutManager(layoutManager);
        teamsRecyclerView.setHasFixedSize(true);
    }

    /**
     * Setup teams list with all 9 available teams
     */
    private void setupTeamsList() {
        List<Team> teams = createTeamsList();
        adapter = new TeamSelectionAdapter(teams, this);
        teamsRecyclerView.setAdapter(adapter);
    }

    /**
     * Create list of all 9 teams with shift cycle information
     */
    private List<Team> createTeamsList() {
        List<Team> teams = new ArrayList<>();

        // Team colors for visual distinction
        int[] teamColors = {
                R.color.team_1_color,
                R.color.team_2_color,
                R.color.team_3_color,
                R.color.team_4_color,
                R.color.team_5_color,
                R.color.team_6_color,
                R.color.team_7_color,
                R.color.team_8_color,
                R.color.team_9_color
        };

        // Create teams 1-9
        for (int i = 1; i <= 9; i++) {
            String teamName = getString(R.string.team_name_format, i);
            String description = getString(R.string.team_description_format, i);
            boolean isSelected = (i == selectedTeamNumber);

            teams.add(new Team(
                    i,
                    teamName,
                    description,
                    teamColors[i - 1],
                    isSelected
            ));
        }

        return teams;
    }

    /**
     * Start entrance animation for the fragment
     */
    private void startEntranceAnimation() {
        // Initial state - elements start from different positions
        titleText.setTranslationY(-50f);
        titleText.setAlpha(0f);

        subtitleText.setTranslationY(-30f);
        subtitleText.setAlpha(0f);

        explanationCard.setTranslationY(50f);
        explanationCard.setAlpha(0f);

        teamsRecyclerView.setTranslationY(100f);
        teamsRecyclerView.setAlpha(0f);

        // Animate title
        titleText.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .start();

        // Animate subtitle
        subtitleText.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(100)
                .start();

        // Animate explanation card
        explanationCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(200)
                .start();

        // Animate teams grid
        teamsRecyclerView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(400)
                .start();
    }

    /**
     * Handle team selection from adapter
     */
    @Override
    public void onTeamSelected(int teamNumber) {
        selectedTeamNumber = teamNumber;

        // Save selection to preferences
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("selected_team", teamNumber);
            editor.apply();
        }

        // Update adapter to reflect new selection
        adapter.updateSelectedTeam(teamNumber);

        // Provide visual feedback
        showTeamSelectedFeedback(teamNumber);

        // Enable next button in parent activity
        if (getActivity() instanceof WelcomeActivity) {
            // Could notify parent activity here if needed
        }
    }

    /**
     * Show visual feedback when team is selected
     */
    private void showTeamSelectedFeedback(int teamNumber) {
        if (getView() != null) {
            TextView selectedTeamText = getView().findViewById(R.id.selected_team_text);
            if (selectedTeamText != null) {
                selectedTeamText.setText(getString(R.string.team_selected_format, teamNumber));
                selectedTeamText.setVisibility(View.VISIBLE);

                // Animate the feedback text
                selectedTeamText.setAlpha(0f);
                selectedTeamText.setScaleX(0.8f);
                selectedTeamText.setScaleY(0.8f);

                selectedTeamText.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start();
            }
        }
    }

    /**
     * Get currently selected team number
     */
    public int getSelectedTeam() {
        return selectedTeamNumber;
    }

    /**
     * Check if user has selected a team
     */
    public boolean hasTeamSelected() {
        return selectedTeamNumber > 0 && selectedTeamNumber <= 9;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restart animation if fragment becomes visible
        if (isVisible()) {
            startEntranceAnimation();
        }
    }
}