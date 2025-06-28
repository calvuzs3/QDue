package net.calvuz.qdue.ui.welcome;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import net.calvuz.qdue.R;

/**
 * Personalization Fragment - Final step of welcome flow
 * <p></p>
 * Allows user to configure final preferences:
 * - Dynamic colors (Android 12+ Material You)
 * - Theme selection (Light/Dark/System)
 * - Notification preferences
 * - Privacy settings
 * <p></p>
 * Provides summary of all previous selections.
 */
public class PersonalizationFragment extends Fragment {

    // View components
    private MaterialSwitch dynamicColorsSwitch;
    private MaterialSwitch notificationsSwitch;
    private MaterialCardView lightThemeCard;
    private MaterialCardView darkThemeCard;
    private MaterialCardView systemThemeCard;

    // Summary components
    private TextView selectedTeamSummary;
    private TextView selectedViewSummary;
    private TextView configurationStatus;

    // Settings state
    private boolean isDynamicColorsEnabled = true;
    private boolean areNotificationsEnabled = true;
    private String selectedTheme = "system"; // system, light, dark

    // Preferences
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_personalization, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        loadPreviousSelections();
        setupSwitchListeners();
        setupThemeSelection();
        checkDynamicColorsSupport();
        startEntranceAnimation();
    }

    /**
     * Initialize view components
     */
    private void initializeViews(View view) {
        // Switches
        dynamicColorsSwitch = view.findViewById(R.id.dynamic_colors_switch);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);

        // Theme cards
        lightThemeCard = view.findViewById(R.id.light_theme_card);
        darkThemeCard = view.findViewById(R.id.dark_theme_card);
        systemThemeCard = view.findViewById(R.id.system_theme_card);

        // Summary components
        selectedTeamSummary = view.findViewById(R.id.selected_team_summary);
        selectedViewSummary = view.findViewById(R.id.selected_view_summary);
        configurationStatus = view.findViewById(R.id.configuration_status);

        // Get preferences from parent activity
        if (getActivity() != null) {
            preferences = getActivity().getSharedPreferences("qdue_welcome_prefs", getActivity().MODE_PRIVATE);
        }
    }

    /**
     * Load previous selections from welcome flow
     */
    private void loadPreviousSelections() {
        if (preferences == null) return;

        // Load team selection
        int selectedTeam = preferences.getInt("selected_team", -1);
        if (selectedTeam > 0) {
            selectedTeamSummary.setText(getString(R.string.completion_team_format, selectedTeam));
            selectedTeamSummary.setVisibility(View.VISIBLE);
        }

        // Load view mode selection
        String viewMode = preferences.getString("view_mode", null);
        if (viewMode != null) {
            String viewDisplayName = viewMode.equals("calendar") ?
                    getString(R.string.view_calendar_title) : getString(R.string.view_dayslist_title);
            selectedViewSummary.setText(getString(R.string.completion_view_format, viewDisplayName));
            selectedViewSummary.setVisibility(View.VISIBLE);
        }

        // Load saved personalization preferences
        isDynamicColorsEnabled = preferences.getBoolean("dynamic_colors_enabled", false);
        areNotificationsEnabled = preferences.getBoolean("notifications_enabled", false);
        selectedTheme = preferences.getString("selected_theme", "system");

        // Update UI with loaded values
        dynamicColorsSwitch.setChecked(isDynamicColorsEnabled);
        notificationsSwitch.setChecked(areNotificationsEnabled);
        updateThemeSelection();
    }

    /**
     * Setup switch listeners for settings
     */
    private void setupSwitchListeners() {
        dynamicColorsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDynamicColorsEnabled = isChecked;
            saveDynamicColorsPreference(isChecked);
            animateSwitchFeedback(dynamicColorsSwitch);
            updateConfigurationStatus();
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            areNotificationsEnabled = isChecked;
            saveNotificationsPreference(isChecked);
            animateSwitchFeedback(notificationsSwitch);
            updateConfigurationStatus();
        });
    }

    /**
     * Setup theme selection cards
     */
    private void setupThemeSelection() {
        lightThemeCard.setOnClickListener(v -> selectTheme("light"));
        darkThemeCard.setOnClickListener(v -> selectTheme("dark"));
        systemThemeCard.setOnClickListener(v -> selectTheme("system"));
    }

    /**
     * Handle theme selection
     * @param theme Selected theme ("light", "dark", "system")
     */
    private void selectTheme(String theme) {
        selectedTheme = theme;
        saveThemePreference(theme);
        updateThemeSelection();
        animateThemeSelection(theme);
        updateConfigurationStatus();
    }

    /**
     * Update visual state of theme selection
     */
    private void updateThemeSelection() {
        // Reset all cards
        resetThemeCards();

        // Apply selected state
        final MaterialCardView selectedCard ;
        switch (selectedTheme) {
            case "light":
                selectedCard = lightThemeCard;
                break;
            case "dark":
                selectedCard = darkThemeCard;
                break;
            case "system":
            default:
                selectedCard = systemThemeCard;
                break;
        }

        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(getResources().getColor(R.color.md_theme_light_primaryContainer, null));
            selectedCard.setStrokeColor(getResources().getColor(R.color.md_theme_light_primary, null));
            selectedCard.setStrokeWidth(2);
            selectedCard.setCardElevation(6f);

            // Show selection indicator
            View indicator = selectedCard.findViewById(R.id.selection_indicator);
            if (indicator != null) {
                indicator.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Reset all theme cards to unselected state
     */
    private void resetThemeCards() {
        MaterialCardView[] cards = {lightThemeCard, darkThemeCard, systemThemeCard};

        for (MaterialCardView card : cards) {
            card.setCardBackgroundColor(getResources().getColor(R.color.md_theme_light_surface, null));
            card.setStrokeColor(getResources().getColor(R.color.md_theme_light_outlineVariant, null));
            card.setStrokeWidth(1);
            card.setCardElevation(2f);

            View indicator = card.findViewById(R.id.selection_indicator);
            if (indicator != null) {
                indicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Check if dynamic colors are supported and update UI accordingly
     */
    private void checkDynamicColorsSupport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Dynamic colors not supported on Android < 12
            dynamicColorsSwitch.setEnabled(false);
            dynamicColorsSwitch.setChecked(false);
            isDynamicColorsEnabled = false;

            View dynamicColorsCard = getView().findViewById(R.id.dynamic_colors_card);
            if (dynamicColorsCard != null) {
                dynamicColorsCard.setAlpha(0.6f);
            }

            TextView dynamicColorsDesc = getView().findViewById(R.id.dynamic_colors_description);
            if (dynamicColorsDesc != null) {
                dynamicColorsDesc.setText(R.string.dynamic_colors_not_supported);
            }
        }
    }

    /**
     * Animate switch feedback when toggled
     */
    private void animateSwitchFeedback(MaterialSwitch materialSwitch) {
        materialSwitch.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    materialSwitch.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Animate theme selection feedback
     */
    private void animateThemeSelection(String theme) {
        MaterialCardView selectedCard = null;
        switch (theme) {
            case "light": selectedCard = lightThemeCard; break;
            case "dark": selectedCard = darkThemeCard; break;
            case "system": selectedCard = systemThemeCard; break;
        }

        if (selectedCard != null) {
            MaterialCardView finalSelectedCard = selectedCard;
            selectedCard.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        finalSelectedCard.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Update configuration status display
     */
    private void updateConfigurationStatus() {
        if (configurationStatus != null) {
            configurationStatus.setText(R.string.configuration_complete);
            configurationStatus.setTextColor(getResources().getColor(R.color.md_theme_light_primary, null));

            // Animate status update
            configurationStatus.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        configurationStatus.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Save preferences to SharedPreferences
     */
    private void saveDynamicColorsPreference(boolean enabled) {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("dynamic_colors_enabled", enabled);
            editor.apply();
        }
    }

    private void saveNotificationsPreference(boolean enabled) {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("notifications_enabled", enabled);
            editor.apply();
        }
    }

    private void saveThemePreference(String theme) {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("selected_theme", theme);
            editor.apply();
        }
    }

    /**
     * Start entrance animation for the fragment
     */
    private void startEntranceAnimation() {
        // Staggered entrance animation for all sections
        View[] animatedViews = {
                getView().findViewById(R.id.summary_card),
                getView().findViewById(R.id.dynamic_colors_card),
                getView().findViewById(R.id.theme_section),
                getView().findViewById(R.id.notifications_card)
        };

        for (int i = 0; i < animatedViews.length; i++) {
            if (animatedViews[i] != null) {
                animatedViews[i].setTranslationY(50f);
                animatedViews[i].setAlpha(0f);

                animatedViews[i].animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(i * 150L)
                        .start();
            }
        }
    }

    /**
     * Get personalization summary for completion screen
     */
    public String getPersonalizationSummary() {
        StringBuilder summary = new StringBuilder();

        // Dynamic colors status
        if (isDynamicColorsEnabled) {
            summary.append(getString(R.string.dynamic_colors_enabled));
        } else {
            summary.append(getString(R.string.dynamic_colors_disabled));
        }

        summary.append(" • ");

        // Theme selection
        switch (selectedTheme) {
            case "light":
                summary.append(getString(R.string.theme_light));
                break;
            case "dark":
                summary.append(getString(R.string.theme_dark));
                break;
            case "system":
                summary.append(getString(R.string.theme_system));
                break;
        }

        summary.append(" • ");

        // Notifications status
        if (areNotificationsEnabled) {
            summary.append(getString(R.string.notifications_enabled));
        } else {
            summary.append(getString(R.string.notifications_disabled));
        }

        return summary.toString();
    }

    /**
     * Check if all personalization settings are configured
     */
    public boolean isPersonalizationComplete() {
        return selectedTheme != null && !selectedTheme.isEmpty();
    }

    /**
     * Get current personalization settings
     */
    public PersonalizationSettings getPersonalizationSettings() {
        return new PersonalizationSettings(
                isDynamicColorsEnabled,
                areNotificationsEnabled,
                selectedTheme
        );
    }

    /**
     * Data class for personalization settings
     */
    public static class PersonalizationSettings {
        public final boolean dynamicColorsEnabled;
        public final boolean notificationsEnabled;
        public final String theme;

        public PersonalizationSettings(boolean dynamicColorsEnabled,
                                       boolean notificationsEnabled,
                                       String theme) {
            this.dynamicColorsEnabled = dynamicColorsEnabled;
            this.notificationsEnabled = notificationsEnabled;
            this.theme = theme;
        }
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