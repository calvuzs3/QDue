// TeamSelectionFragment.java
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
import net.calvuz.qdue.ui.events.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.utils.Log;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.Throws;

/**
 * Team Selection Fragment - Second step of welcome flow
 * <p>
 * Allows user to select their team from 9 available teams.
 * Each team follows the "quattro-due" (4-2) shift system:
 * - 4 consecutive work days
 * - 2 consecutive rest days
 * <p>
 * Teams are numbered 1-9 and displayed in a grid layout
 * with visual indicators for the shift cycle.
 */
public class TeamSelectionFragment extends Fragment implements TeamSelectionAdapter.OnTeamSelectedListener {

    // Team data class aligned with QDue system
    public static class Team {
        public final int teamIndex;        // 0-8 (for preferences storage)
        public final String teamLetter;   // A-I (QDue identifier)
        public final String teamName;     // "Squadra A" (display name)
        public final String description;  // Full description
        public final int colorRes;        // Color resource
        public final boolean isSelected;  // Selection state

        public Team(int teamIndex, String teamLetter, String teamName, String description, int colorRes, boolean isSelected) {
            this.teamIndex = teamIndex;
            this.teamLetter = teamLetter;
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

    // Interface
    private WelcomeInterface welcomeInterface;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_welcome_team_selection, container, false);

        if (getActivity() instanceof WelcomeInterface)
            welcomeInterface = (WelcomeInterface) getActivity();
        else
            throw new ClassCastException("Activity does not implement WelcomeInterface");

        return inflate;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        loadSavedPreferences();
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

        // Setup RecyclerView with grid layout (3 columns)
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        teamsRecyclerView.setLayoutManager(layoutManager);
        teamsRecyclerView.setHasFixedSize(true);
    }

    private void loadSavedPreferences() {
        selectedTeamNumber = welcomeInterface.getSelectedTeam();
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
     * Create list of all 9 teams using QDue's letter system
     */
    private List<Team> createTeamsList() {
        List<Team> teams = new ArrayList<>();

        // Get team letters from QDue resources (A-I)
        String[] teamLetters;
        int[] teamColors = {
                R.color.team_1_color, R.color.team_2_color, R.color.team_3_color,
                R.color.team_4_color, R.color.team_5_color, R.color.team_6_color,
                R.color.team_7_color, R.color.team_8_color, R.color.team_9_color
        };

        if (getResources() != null) {
            // Use QDue's existing team letters (A-I)
            teamLetters = getResources().getStringArray(R.array.pref_entries_user_team);
        } else {
            // Fallback to manual letters
            teamLetters = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        }

        // Create teams using letters (A-I) with corresponding indices (0-8)
        for (int i = 0; i < teamLetters.length && i < 9; i++) {
            String teamLetter = teamLetters[i];
            String teamName = getString(R.string.team_letter_format, teamLetter); // "Squadra A"
            String description = getString(R.string.team_letter_description_format, teamLetter); // "Turni squadra A"
            boolean isSelected = (i == selectedTeamNumber); // selectedTeamNumber is 0-8 index

            teams.add(new Team(
                    i,                    // teamNumber: 0-8 (for storage)
                    teamLetter,          // teamName: A-I (for display)
                    teamName,            // displayName: "Squadra A"
                    description,         // description
                    teamColors[i],       // color
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
     * We hace a duplicated value, one is the old quattrodue system,
     * the other is the new QDue system.
     * Let's update them both.
     */
    @Override
    public void onTeamSelected(int teamIndex) {
        selectedTeamNumber = teamIndex; // Store 0-8 index (compatible with QDue)

        // Save selection to preferences using QDue's key format
        welcomeInterface.setSelectedTeam(teamIndex);

        // Save selection for quattrodue
        if (getContext() != null) {
            SharedPreferences qDuePrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor qDueEditor = qDuePrefs.edit();
            qDueEditor.putString(getString(R.string.qd_preference_user_team), String.valueOf(teamIndex));
            qDueEditor.apply();
        }

        // Update adapter to reflect new selection
        adapter.updateSelectedTeam(teamIndex);

        // Provide visual feedback with team letter
        String[] teamLetters = getResources().getStringArray(R.array.pref_entries_user_team);
        if (teamIndex >= 0 && teamIndex < teamLetters.length) {
            showTeamSelectedFeedback(teamLetters[teamIndex]); // Show "A", "B", etc.
        }
    }

    /**
     * Show visual feedback when team is selected
     */
    private void showTeamSelectedFeedback(String teamLetter) {
        if (getView() != null) {
            TextView selectedTeamText = getView().findViewById(R.id.selected_team_text);
            if (selectedTeamText != null) {
                selectedTeamText.setText(getString(R.string.team_selected_letter_format, teamLetter));
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