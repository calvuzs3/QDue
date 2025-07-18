// ViewModeFragment.java
package net.calvuz.qdue.ui.features.welcome.presentation;

import static net.calvuz.qdue.QDue.Settings.VIEW_MODE_CALENDAR;
import static net.calvuz.qdue.QDue.Settings.VIEW_MODE_DAYSLIST;

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

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.features.welcome.interfaces.WelcomeInterface;

/**
 * View Mode Selection Fragment - Third step of welcome flow
 * <p>
 * Allows user to choose between two main view modes:
 * 1. Calendar View - Shows only user's team shifts in monthly calendar format
 * 2. DaysList View - Shows all 9 teams' shifts in daily list format
 * <p>
 * Features visual previews and detailed explanations of each mode.
 */
public class ViewModeFragment extends Fragment {

    // View components
    private MaterialCardView calendarCard;
    private MaterialCardView dayslistCard;
    private TextView selectedModeText;

    // Selection state
    private String selectedViewMode = null;

    // Interface
    private WelcomeInterface welcomeInterface;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_welcome_view_mode, container, false);

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
        setupViewsList();
        startEntranceAnimation();
    }

    /**
     * Initialize view components
     */
    private void initializeViews(View view) {
        calendarCard = view.findViewById(R.id.calendar_view_card);
        dayslistCard = view.findViewById(R.id.dayslist_view_card);
        selectedModeText = view.findViewById(R.id.selected_mode_text);
    }

    /**
     * Load previously saved view mode preference
     */
    private void loadSavedPreferences() {
            selectedViewMode = welcomeInterface.getViewMode();
            updateSelectionVisuals();
    }

    /**
     * Setup teams list with all 9 available teams
     */
    private void setupViewsList() {
        setupClickListeners();
    }

    /**
     * Setup click listeners for both view mode cards
     */
    private void setupClickListeners() {
        calendarCard.setOnClickListener(v -> selectViewMode(VIEW_MODE_CALENDAR));
        dayslistCard.setOnClickListener(v -> selectViewMode(VIEW_MODE_DAYSLIST));
    }

    /**
     * Handle view mode selection
     * @param viewMode Selected view mode constant
     */
    private void selectViewMode(String viewMode) {
        selectedViewMode = viewMode;

        // Save selection to preferences
        welcomeInterface.setViewMode(viewMode);

        // Update visual state
        updateSelectionVisuals();

        // Show selection feedback
        showSelectionFeedback(viewMode);

        // Animate selection
        animateSelection(viewMode);
    }

    /**
     * Update visual state of selection cards
     */
    private void updateSelectionVisuals() {
        if (selectedViewMode == null) {
            // No selection state
            resetCardStates();
            selectedModeText.setVisibility(View.GONE);
            return;
        }

        // Reset both cards
        resetCardStates();

        // Apply selected state
        MaterialCardView selectedCard = selectedViewMode.equals(VIEW_MODE_CALENDAR) ? calendarCard : dayslistCard;

        selectedCard.setCardBackgroundColor(getResources().getColor(R.color.md_theme_light_primaryContainer, null));
        selectedCard.setStrokeColor(getResources().getColor(R.color.md_theme_light_primary, null));
        selectedCard.setStrokeWidth(3);
        selectedCard.setCardElevation(8f);

        // Update selection indicator
        View selectionIndicator = selectedCard.findViewById(R.id.selection_indicator);
        if (selectionIndicator != null) {
            selectionIndicator.setVisibility(View.VISIBLE);
        }

        // Show selection text
        selectedModeText.setVisibility(View.VISIBLE);
        String modeName = selectedViewMode.equals(VIEW_MODE_CALENDAR) ?
                getString(R.string.view_calendar_title) : getString(R.string.view_dayslist_title);
        selectedModeText.setText(getString(R.string.view_mode_selected_format, modeName));
    }

    /**
     * Reset both cards to unselected state
     */
    private void resetCardStates() {
        // Reset calendar card
        calendarCard.setCardBackgroundColor(getResources().getColor(R.color.md_theme_light_surface, null));
        calendarCard.setStrokeColor(getResources().getColor(R.color.md_theme_light_outlineVariant, null));
        calendarCard.setStrokeWidth(1);
        calendarCard.setCardElevation(2f);

        View calendarIndicator = calendarCard.findViewById(R.id.selection_indicator);
        if (calendarIndicator != null) {
            calendarIndicator.setVisibility(View.GONE);
        }

        // Reset dayslist card
        dayslistCard.setCardBackgroundColor(getResources().getColor(R.color.md_theme_light_surface, null));
        dayslistCard.setStrokeColor(getResources().getColor(R.color.md_theme_light_outlineVariant, null));
        dayslistCard.setStrokeWidth(1);
        dayslistCard.setCardElevation(2f);

        View dayslistIndicator = dayslistCard.findViewById(R.id.selection_indicator);
        if (dayslistIndicator != null) {
            dayslistIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Show selection feedback with animation
     * @param viewMode Selected view mode
     */
    private void showSelectionFeedback(String viewMode) {
        if (selectedModeText != null) {
            selectedModeText.setAlpha(0f);
            selectedModeText.setScaleX(0.8f);
            selectedModeText.setScaleY(0.8f);

            selectedModeText.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start();
        }
    }

    /**
     * Animate card selection with scale effect
     * @param viewMode Selected view mode
     */
    private void animateSelection(String viewMode) {
        MaterialCardView selectedCard = viewMode.equals(VIEW_MODE_CALENDAR) ? calendarCard : dayslistCard;

        // Scale down then up animation
        selectedCard.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    selectedCard.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                })
                .start();

        // Pulse animation for preview image
        ImageView previewImage = selectedCard.findViewById(R.id.preview_image);
        if (previewImage != null) {
            previewImage.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        previewImage.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Start entrance animation for the fragment
     */
    private void startEntranceAnimation() {
        // Initial state
        calendarCard.setTranslationX(-200f);
        calendarCard.setAlpha(0f);

        dayslistCard.setTranslationX(200f);
        dayslistCard.setAlpha(0f);

        // Animate calendar card from left
        calendarCard.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(200)
                .start();

        // Animate dayslist card from right
        dayslistCard.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(400)
                .start();
    }

    /**
     * Get currently selected view mode
     * @return Selected view mode or null if none selected
     */
    public String getSelectedViewMode() {
        return selectedViewMode;
    }

    /**
     * Check if user has selected a view mode
     * @return True if a view mode is selected
     */
    public boolean hasViewModeSelected() {
        return selectedViewMode != null &&
                (selectedViewMode.equals(VIEW_MODE_CALENDAR) || selectedViewMode.equals(VIEW_MODE_DAYSLIST));
    }

    /**
     * Get user-friendly name for selected view mode
     * @return Display name of selected view mode
     */
    public String getSelectedViewModeDisplayName() {
        if (selectedViewMode == null) return "";

        return selectedViewMode.equals(VIEW_MODE_CALENDAR) ?
                getString(R.string.view_calendar_title) : getString(R.string.view_dayslist_title);
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