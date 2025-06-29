package net.calvuz.qdue.ui.welcome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;

/**
 * Welcome Activity for QDue app introduction and initial setup.
 * Guides users through team selection, view preferences, and feature overview.
 * <p></p>
 * Features:
 * - Animated logo introduction with clock-like Q and 2 rotation
 * - Team selection (9 teams available)
 * - View mode selection (Calendar vs DaysList)
 * - Feature overview with expandable functionality
 * - Dynamic colors configuration
 * - Smooth transitions between steps
 */
public class WelcomeActivity extends AppCompatActivity implements  WelcomeInterface{

    // View components
    private ViewPager2 viewPager;
    private TabLayout tabIndicator;
    private MaterialButton nextButton;
    private MaterialButton skipButton;
    private TextView progressText;

    // Welcome fragments adapter
    private WelcomeFragmentAdapter adapter;

    // Configuration storage
    private SharedPreferences preferences;

    // Current step tracking
    private int currentStep = 0;
    private final int totalSteps = 5; // Welcome, Team, View, Features, Colors, Complete

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences
        preferences = getSharedPreferences(QDue.Settings.QD_PREF_NAME, MODE_PRIVATE);

        // Check if welcome was already completed
        if (preferences.getBoolean(QDue.Settings.QD_KEY_WELCOME_COMPLETED, false)) {
            // Skip welcome and go to main activity
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_welcome);

        // Initialize views
        initializeViews();

        // Start with logo animation
        startLogoAnimation();
    }

    /**
     * Initialize all view components and setup listeners
     */
    private void initializeViews() {
        viewPager = findViewById(R.id.welcome_viewpager);
        tabIndicator = findViewById(R.id.tab_indicator);
        nextButton = findViewById(R.id.btn_next);
        skipButton = findViewById(R.id.btn_skip);
        progressText = findViewById(R.id.progress_text);

        // Setup ViewPager with adapter
        adapter = new WelcomeFragmentAdapter(this);
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager with custom tabs
        new TabLayoutMediator(tabIndicator, viewPager, (tab, position) -> {
            // Add icons and content descriptions for each step
            switch (position) {
                case 0: // Introduction
                    tab.setIcon(R.drawable.ic_rounded_home_24);
                    //tab.setContentDescription(getString(R.string.welcome_step_intro));
                    break;
                case 1: // Team Selection
                    tab.setIcon(R.drawable.ic_rounded_group_24);
                    //tab.setContentDescription(getString(R.string.welcome_step_team));
                    break;
                case 2: // View Mode
                    tab.setIcon(R.drawable.ic_rounded_visibility_24);
                    //tab.setContentDescription(getString(R.string.welcome_step_view));
                    break;
                case 3: // Features
                    tab.setIcon(R.drawable.ic_rounded_star_24);
                    //tab.setContentDescription(getString(R.string.welcome_step_features));
                    break;
                case 4: // Personalization
                    tab.setIcon(R.drawable.ic_rounded_palette_24);
                    //tab.setContentDescription(getString(R.string.welcome_step_personalization));
                    break;
            }
        }).attach();

        // Setup button listeners
        setupButtonListeners();

        // Initially hide the main welcome content
        hideWelcomeContent();

        // Update progress text
        updateProgressText();
    }

    /**
     * Setup click listeners for navigation buttons
     */
    private void setupButtonListeners() {
        nextButton.setOnClickListener(v -> {
            if (currentStep < totalSteps - 1) {
                currentStep++;
                viewPager.setCurrentItem(currentStep, true);
                updateProgressText();
                updateButtonStates();
            } else {
                // Complete welcome process
                setWelcomeCompleted();
            }
        });

        skipButton.setOnClickListener(v -> {
            // Set default values and complete welcome
            setDefaultPreferences();
            setWelcomeCompleted();
        });

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentStep = position;
                updateProgressText();
                updateButtonStates();
            }
        });
    }

    /**
     * Start the animated logo introduction
     * Animates Q and 2 like clock hands
     */
    private void startLogoAnimation() {
        ImageView logoQ = findViewById(R.id.logo_q);
//        ImageView logo2 = findViewById(R.id.logo_2);
        TextView welcomeTitle = findViewById(R.id.welcome_title);
        TextView welcomeSubtitle = findViewById(R.id.welcome_subtitle);

        // Q rotates like hour hand (slower)
        ObjectAnimator qRotation = ObjectAnimator.ofFloat(logoQ, "rotation", 0f, 360f);
        qRotation.setDuration(QDue.Settings.QD_WELCOME_LOGO_ANIMATION_DURATION);
        qRotation.setInterpolator(new LinearInterpolator());
        qRotation.setRepeatCount(1); // Rotate twice

        // 2 rotates like minute hand (faster)
//        ObjectAnimator twoRotation = ObjectAnimator.ofFloat(logo2, "rotation", 0f, 720f); // 2 full rotations
//        twoRotation.setDuration(QD_WELCOME_LOGO_ANIMATION_DURATION);
//        twoRotation.setInterpolator(new LinearInterpolator());

        // Fade in title and subtitle after animation
        welcomeTitle.setAlpha(0f);
        welcomeSubtitle.setAlpha(0f);

        // Start rotations
        qRotation.start();
//        twoRotation.start();

        // Add listener to show content after animation
        qRotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Fade in welcome text
                welcomeTitle.animate().alpha(1f).setDuration(500);
                welcomeSubtitle.animate().alpha(1f).setDuration(500).setStartDelay(200);

                // Show main content after delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showWelcomeContent();
                }, QDue.Settings.QD_WELCOME_DISPLAY_DURATION);
            }
        });
    }

    /**
     * Hide welcome content during logo animation
     */
    private void hideWelcomeContent() {
        MaterialCardView welcomeContent = findViewById(R.id.welcome_content);
        welcomeContent.setVisibility(View.INVISIBLE);
        welcomeContent.setAlpha(0f);
    }

    /**
     * Show welcome content with smooth animation
     */
    private void showWelcomeContent() {
        MaterialCardView logoContainer = findViewById(R.id.logo_container);
        MaterialCardView welcomeContent = findViewById(R.id.welcome_content);

        // Hide logo container
        logoContainer.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        logoContainer.setVisibility(View.GONE);
                    }
                });

        // Show welcome content
        welcomeContent.setVisibility(View.VISIBLE);
        welcomeContent.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(200);
    }

    /**
     * Update progress text display
     */
    private void updateProgressText() {
        String progress = getString(R.string.welcome_progress, currentStep + 1, totalSteps);
        progressText.setText(progress);
    }

    /**
     * Update button states based on current step
     */
    private void updateButtonStates() {
        if (currentStep == totalSteps - 1) {
            nextButton.setText(R.string.welcome_get_started);
            nextButton.setIcon(getDrawable(R.drawable.ic_rounded_check_24));
        } else {
            nextButton.setText(R.string.welcome_next);
            nextButton.setIcon(getDrawable(R.drawable.ic_rounded_arrow_forward_24));
        }

        // Show skip button only on first few steps
        skipButton.setVisibility(currentStep < 2 ? View.VISIBLE : View.GONE);
    }

    /**
     * Set default preferences when user skips welcome
     */
    private void setDefaultPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1); // Default to team 1
        editor.putString(QDue.Settings.QD_KEY_VIEW_MODE, "calendar"); // Default to calendar view
        editor.putBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, true); // Enable dynamic colors by default
        editor.apply();
    }

    /**
     * Navigate to main activity
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, QDueMainActivity.class);
        startActivity(intent);
        finish(); // Close welcome activity

        // Add smooth transition
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        if (currentStep > 0) {
            // Go to previous step
            currentStep--;
            viewPager.setCurrentItem(currentStep, true);
            updateProgressText();
            updateButtonStates();
        } else {
            // Exit app on first step
            super.onBackPressed();
        }
    }

    // =============================== WELCOME INTERFACE ==================================

    /**
     * Get SharedPreferences instance
     * @return SharedPreferences instance
     */
    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Set selected team preference
     * @param teamIndex Selected team ID
     */
    @Override
    public void setSelectedTeam(int teamIndex) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(QDue.Settings.QD_KEY_SELECTED_TEAM, teamIndex);
        editor.apply();
    }

    /**
     * Get selected team preference
     * @return Selected team ID or default (1)
     */
    @Override
    public int getSelectedTeam() {
        return preferences.getInt(QDue.Settings.QD_KEY_SELECTED_TEAM, 1);
    }

    /**
     * Set view mode preference
     * @param viewMode View mode to set
     */
    @Override
    public void setViewMode(String viewMode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(QDue.Settings.QD_KEY_VIEW_MODE, viewMode);
        editor.apply();
    }

    /**
     * Get view mode preference
     * @return View mode or default ("dayslist")
     */
    @Override
    public String getViewMode() {
        return preferences.getString(QDue.Settings.QD_KEY_VIEW_MODE, "dayslist");
    }

    /**
     * Set dynamic colors enabled preference
     * @param enabled True if enabled, false otherwise
     */
    @Override
    public void setDynamicColorsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, enabled);
        editor.apply();
    }

    /**
     * Get dynamic colors enabled preference
     * @return True if enabled, false otherwise
     */
    @Override
    public boolean isDynamicColorsEnabled() {
        return preferences.getBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, true);
    }

    /**
     * Set welcome as completed
     */
    @Override
    public void setWelcomeCompleted() {
        // Mark welcome as completed
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(QDue.Settings.QD_KEY_WELCOME_COMPLETED, true);
        editor.apply();

        // Start main activity with smooth transition
        startMainActivity();
    }
}