package net.calvuz.qdue.ui.features.welcome.presentation;

import static net.calvuz.qdue.QDue.Settings.QD_KEY_QDUEUSER_EMAIL;
import static net.calvuz.qdue.QDue.Settings.QD_KEY_QDUEUSER_NICKNAME;
import static net.calvuz.qdue.QDue.Settings.QD_KEY_QDUEUSER_ONBOARDING_COMPLETED;

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
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;

import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.features.swipecalendar.presentation.SwipeCalendarActivity;
import net.calvuz.qdue.ui.features.welcome.interfaces.WelcomeInterface;
import net.calvuz.qdue.ui.features.welcome.adapters.WelcomeFragmentAdapter;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * Welcome Activity for QDue app introduction and initial setup.
 * Guides users through team selection, view preferences, and feature overview.
 * <p></p>
 * Features:
 * - Animated logo introduction with clock-like Q and 2 rotation
 * - Feature overview with expandable functionality
 * - User information input
 * - Team selection (9 teams available)
 * - First Assignment Wizard if no assignments (optional)
 * - Dynamic colors configuration
 * - Smooth transitions between steps
 */
public class WelcomeActivity extends AppCompatActivity implements WelcomeInterface, Injectable {
    private final static String TAG = "WelcomeActivity";

    // Dependencies
    private ServiceProvider mServiceProvider;

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
    private final int totalSteps = 5; // Welcome, Features, QDueUser, Assignment, Colors, Complete

    // Email validation pattern
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize dependency injection FIRST
        initializeDependencyInjection();

        // Initialize preferences
        preferences = getSharedPreferences( QDue.Settings.QD_PREF_NAME, MODE_PRIVATE );

        // Check if welcome was already completed
        if (preferences.getBoolean( QDue.Settings.QD_KEY_WELCOME_COMPLETED, false )) {
            // Skip welcome and go to main activity
            try {
                wait( 500 ); // TEST
                startMainActivity();
            } catch (InterruptedException e) {
                throw new RuntimeException( e );
            }
            finish(); // Close welcome activity - return;
        }

        setContentView( R.layout.activity_welcome );

        // Initialize views
        initializeViews();

        // Start with logo animation
        startLogoAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensure current fragment has dependencies if needed
        if (areDependenciesReady()) {
            injectDependenciesForCurrentStep();
        }
    }

    @Override
    protected void onDestroy() {
        // Log final preferences state if welcome was completed
        if (preferences.getBoolean( QDue.Settings.QD_KEY_WELCOME_COMPLETED, false )) {
            Log.d( TAG, "onDestroy: Welcome completed successfully" );
        } else {
            Log.e( TAG, "onDestroy: Welcome activity destroyed without completion" );
        }

        // Clean up references
        mServiceProvider = null;

        super.onDestroy();
    }

    // ==================== ADD DEPENDENCY INJECTION METHODS ====================

    /**
     * Initialize dependency injection for WelcomeActivity
     */
    private void initializeDependencyInjection() {
        Log.d( TAG, "Initializing dependency injection for WelcomeActivity" );

        try {
            // Get ServiceProvider instance
            DependencyInjector.inject( this, this );

            // Initialize services if needed
            if (!mServiceProvider.areServicesReady()) {
                mServiceProvider.initializeServices();
            }

            Log.d( TAG, "✅ Dependency injection completed successfully" );
        } catch (Exception e) {
            Log.e( TAG, "❌ Failed to initialize dependency injection", e );

            // Continue without QDueUserService (graceful degradation)
            Log.w( TAG, "⚠️ Continuing without QDueUserService" );
        }
    }

    /**
     * Injectable interface implementation
     */
    @Override
    public void inject(ServiceProvider serviceProvider) {
        this.mServiceProvider = serviceProvider;
    }

    /**
     * Injectable interface implementation
     */
    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null &&
                mServiceProvider.areServicesReady();
    }

    // ==================== VIEW INITIALIZATION METHODS ====================

    /**
     * Initialize all view components and setup listeners
     */
    private void initializeViews() {
        viewPager = findViewById( R.id.welcome_viewpager );
        tabIndicator = findViewById( R.id.tab_indicator );
        nextButton = findViewById( R.id.btn_next );
        skipButton = findViewById( R.id.btn_skip );
        progressText = findViewById( R.id.progress_text );

        // Setup ViewPager with adapter
        adapter = new WelcomeFragmentAdapter( this );
        viewPager.setAdapter( adapter );

        // Connect TabLayout with ViewPager with custom tabs
        new TabLayoutMediator( tabIndicator, viewPager, (tab, position) -> {
            // Add icons and content descriptions for each step
            switch (position) {
                case 0: // Introduction
                    tab.setIcon( R.drawable.ic_rounded_home_24 );
                    //tab.setContentDescription(getString(R.string.welcome_step_intro));
                    break;
                case 1: // QDueUser Preferences
                    tab.setIcon( R.drawable.ic_rounded_person_24 );
                    //tab.setContentDescription(getString(R.string.welcome_step_team));
                    break;
//                case 1: // Team Selection
//                    tab.setIcon(R.drawable.ic_rounded_group_24);
//                    //tab.setContentDescription(getString(R.string.welcome_step_team));
//                    break;
                case 2: // View Mode
                    tab.setIcon( R.drawable.ic_rounded_visibility_24 );
                    //tab.setContentDescription(getString(R.string.welcome_step_view));
                    break;
                case 3: // Features
                    tab.setIcon( R.drawable.ic_rounded_star_24 );
                    //tab.setContentDescription(getString(R.string.welcome_step_features));
                    break;
                case 4: // Personalization (completed)
                    tab.setIcon( R.drawable.ic_rounded_palette_24 );
                    //tab.setContentDescription(getString(R.string.welcome_step_personalization));
                    break;
            }
        } ).attach();

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
        nextButton.setOnClickListener( v -> {
            if (currentStep < totalSteps - 1) {
                currentStep++;
                viewPager.setCurrentItem( currentStep, true );
                updateProgressText();
                updateButtonStates();

                // Inject dependencies when navigating to QDueUserOnboardingFragment
                injectDependenciesForCurrentStep();
            } else {
                // Complete welcome process
                setWelcomeCompleted();
            }
        } );

        skipButton.setOnClickListener( v -> {
            // Set default values and complete welcome
            setDefaultPreferences();
            // Complete welcome process
            setWelcomeCompleted();
        } );

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback( new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentStep = position;
                updateProgressText();
                updateButtonStates();

                // Inject dependencies when navigating to QDueUserOnboardingFragment
                injectDependenciesForCurrentStep();
            }
        } );
    }

    /**
     * Set welcome as completed
     */
    @Override
    public void setWelcomeCompleted() {
        Log.d( TAG, "Completing welcome flow" );

        // Mark welcome as completed (existing logic)
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean( QDue.Settings.QD_KEY_WELCOME_COMPLETED, true );
        editor.apply();

        // Save QDueUser
        triggerQDueUserOnboarding();

        // Final Log - preferences state for debugging
        Log.i( TAG, "Welcome Completed successfully" );
        QDuePreferences.logAllPreferences( this );

        // Start main activity with smooth transition (existing logic)
        startMainActivity();
    }

// ==================== ADD NEW INJECTION METHOD ====================

    /**
     * Inject dependencies into fragments that need them
     */
    private void injectDependenciesForCurrentStep() {
        if (!areDependenciesReady()) {
            Log.w( TAG, "⚠️ Dependencies not ready, skipping injection for step " + currentStep );
            return;
        }

        try {
            // Get current fragment from adapter
            Fragment currentFragment = adapter.getFragmentAt( currentStep );

            if (currentFragment instanceof QDueUserOnboardingFragment onboardingFragment) {
                Log.d( TAG, "Injecting dependencies into QDueUserOnboardingFragment" );

                // Set the WelcomeInterface (this activity)
                onboardingFragment.setWelcomeInterface( this );

                // Also inject ServiceProvider if needed for validation
                onboardingFragment.inject( mServiceProvider );

                Log.d( TAG, "QDueUserOnboardingFragment setup completed" );
            } else if (currentFragment instanceof Injectable) {
                Log.d( TAG, "Injecting dependencies into Injectable fragment: " +
                        currentFragment.getClass().getSimpleName() );

                ((Injectable) currentFragment).inject( mServiceProvider );
            } else {
                Log.d( TAG, "Current fragment does not require injection: " +
                        (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null") );
            }
        } catch (Exception e) {
            Log.e( TAG, "Error injecting dependencies for step " + currentStep, e );
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Start the animated logo introduction
     * Animates Q and 2 like clock hands
     */
    private void startLogoAnimation() {
        ImageView logoQ = findViewById( R.id.logo_q );
//        ImageView logo2 = findViewById(R.id.logo_2);
        TextView welcomeTitle = findViewById( R.id.welcome_title );
        TextView welcomeSubtitle = findViewById( R.id.welcome_subtitle );

        // Q rotates like hour hand (slower)
        ObjectAnimator qRotation = ObjectAnimator.ofFloat( logoQ, "rotation", 0f, 360f );
        qRotation.setDuration( QDue.Settings.QD_WELCOME_LOGO_ANIMATION_DURATION );
        qRotation.setInterpolator( new LinearInterpolator() );
        qRotation.setRepeatCount( 1 ); // Rotate twice

        // 2 rotates like minute hand (faster)
//        ObjectAnimator twoRotation = ObjectAnimator.ofFloat(logo2, "rotation", 0f, 720f); // 2 full rotations
//        twoRotation.setDuration(QD_WELCOME_LOGO_ANIMATION_DURATION);
//        twoRotation.setInterpolator(new LinearInterpolator());

        // Fade in title and subtitle after animation
        welcomeTitle.setAlpha( 0f );
        welcomeSubtitle.setAlpha( 0f );

        // Start rotations
        qRotation.start();
//        twoRotation.start();

        // Add listener to show content after animation
        qRotation.addListener( new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Fade in welcome text
                welcomeTitle.animate().alpha( 1f ).setDuration( 500 );
                welcomeSubtitle.animate().alpha( 1f ).setDuration( 500 ).setStartDelay( 200 );

                // Show main content after delay
                new Handler( Looper.getMainLooper() ).postDelayed( () -> {
                    showWelcomeContent();
                }, QDue.Settings.QD_WELCOME_DISPLAY_DURATION );
            }
        } );
    }

    /**
     * Hide welcome content during logo animation
     */
    private void hideWelcomeContent() {
        MaterialCardView welcomeContent = findViewById( R.id.welcome_content );
        welcomeContent.setVisibility( View.INVISIBLE );
        welcomeContent.setAlpha( 0f );
    }

    /**
     * Show welcome content with smooth animation
     */
    private void showWelcomeContent() {
        MaterialCardView logoContainer = findViewById( R.id.logo_container );
        MaterialCardView welcomeContent = findViewById( R.id.welcome_content );

        // Hide logo container
        logoContainer.animate()
                .alpha( 0f )
                .scaleX( 0.8f )
                .scaleY( 0.8f )
                .setDuration( 400 )
                .setListener( new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        logoContainer.setVisibility( View.GONE );
                    }
                } );

        // Show welcome content
        welcomeContent.setVisibility( View.VISIBLE );
        welcomeContent.animate()
                .alpha( 1f )
                .scaleX( 1f )
                .scaleY( 1f )
                .setDuration( 500 )
                .setStartDelay( 200 );
    }

    /**
     * Update progress text display
     */
    private void updateProgressText() {
        String progress = getString( R.string.welcome_progress, currentStep + 1, totalSteps );
        progressText.setText( progress );
    }

    /**
     * Update button states based on current step
     */
    private void updateButtonStates() {
        if (currentStep == totalSteps - 1) {
            nextButton.setText( R.string.welcome_get_started );
            nextButton.setIcon( getDrawable( R.drawable.ic_rounded_check_24 ) );
        } else {
            nextButton.setText( R.string.welcome_next );
            nextButton.setIcon( getDrawable( R.drawable.ic_rounded_arrow_forward_24 ) );
        }

        // Show skip button only on first few steps
        skipButton.setVisibility( currentStep < 2 ? View.VISIBLE : View.GONE );
    }

    /**
     * Set default preferences when user skips welcome
     */
    private void setDefaultPreferences() {
        final String mTAG = "setDefaultPreferences: ";
        Log.d( TAG, mTAG + "Setting default preferences for skipped welcome" );

        SharedPreferences.Editor editor = preferences.edit();

        // Set defaults using same values as WelcomeActivity
        editor.putInt( QDue.Settings.QD_KEY_SELECTED_TEAM, 1 ); // Default to team 1
        editor.putString( QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR ); // Default to calendar
        editor.putBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, true ); // Enable dynamic colors by default

        // QDueUser defaults (empty strings)
        editor.putString( QD_KEY_QDUEUSER_NICKNAME, "" );
        editor.putString( QD_KEY_QDUEUSER_EMAIL, "" );
        editor.putBoolean( QD_KEY_QDUEUSER_ONBOARDING_COMPLETED, false );

        editor.apply();

        // Trigger QDueUser onboarding with empty values
        performQDueUserOnboarding( "", "" );

        editor.apply();

        // ENHANCEMENT: Use QDuePreferences for consistency logging
        Log.d( TAG, mTAG + "Default preferences applied:" );
        Log.d( TAG, mTAG + " - Team: " + QDuePreferences.getSelectedTeam( this ) );
        Log.d( TAG, mTAG + " - View Mode: " + QDuePreferences.getDefaultViewMode( this ) );
        Log.d( TAG, mTAG + " - Dynamic Colors: " + QDuePreferences.isDynamicColorsEnabled( this ) );
    }

    /**
     * Ensures all preferences are properly synchronized
     */
    public void validateAndSyncPreferences() {
        final String mTAG = "validateAndSyncPreferences: ";
        Log.d( TAG, mTAG + "Validating and synchronizing preferences" );

        try {
            // Ensure view mode is properly set
            String currentViewMode = getViewMode();
            if (currentViewMode == null || currentViewMode.isEmpty()) {
                Log.w( TAG, mTAG + "View mode not set, applying default" );
                setViewMode( QDue.Settings.VIEW_MODE_CALENDAR );
            }

            // Ensure team is properly set
            int currentTeam = getSelectedTeam();
            if (currentTeam <= 0) {
                Log.w( TAG, mTAG + "Team not properly set, applying default" );
                setSelectedTeam( 1 );
            }

            // Log final state
            Log.d( TAG, mTAG + "Preferences validation completed:" );
            Log.d( TAG, mTAG + " - View Mode: " + getViewMode() );
            Log.d( TAG, mTAG + " - Selected Team: " + getSelectedTeam() );
            Log.d( TAG, mTAG + " - Dynamic Colors: " + isDynamicColorsEnabled() );
        } catch (Exception e) {
            Log.e( TAG, mTAG + "Error validating preferences", e );
            // Apply safe defaults
            setDefaultPreferences();
        }
    }

    /**
     * Navigate to main activity
     */
    private void startMainActivity() {
        Log.d( TAG, "startMainActivity: Starting main activity" );

        try {
            // Validate preferences before starting main activity
            validateAndSyncPreferences();

            // Create intent to main activity (existing logic)
            // TODO verify the activity after all debugs
            Intent intent = new Intent( this, SwipeCalendarActivity.class );

            // Pass information about welcome completion
            intent.putExtra( "from_welcome", true );
            intent.putExtra( "selected_view_mode", getViewMode() );

            startActivity( intent );
            finish(); // Close welcome activity

            // Add smooth transition (existing logic)
            overridePendingTransition( R.anim.slide_in_right, R.anim.slide_out_left );

            Log.d( TAG, "startMainActivity: Successfully started main activity with view mode: " + getViewMode() );
        } catch (Exception e) {
            Log.e( TAG, "startMainActivity: Error starting main activity", e );
            // Fallback: start without extras
            Intent fallbackIntent = new Intent( this, QDueMainActivity.class );
            startActivity( fallbackIntent );
            finish();
        }
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        if (currentStep > 0) {
            // Go to previous step
            currentStep--;
            viewPager.setCurrentItem( currentStep, true );
            updateProgressText();
            updateButtonStates();
        } else {
            // Exit app on first step
            super.onBackPressed();
        }
    }

    // ==================== FRAGMENT COMMUNICATION ENHANCEMENTS ====================

    /**
     * ADD this method to help fragments communicate preference changes
     * This can be called by welcome fragments when they update preferences
     */
    public void notifyPreferenceChanged(String preferenceKey) {
        Log.d( TAG, "notifyPreferenceChanged: Preference change notification: " + preferenceKey );

        // Update progress or UI based on preference changes
        updateProgressBasedOnPreferences();

        // Validate current state
        validateAndSyncPreferences();
    }

    /**
     * ADD this method to update progress based on completed preferences
     */
    private void updateProgressBasedOnPreferences() {
        // This method can be used to update UI state based on how many preferences are set
        // Implementation depends on your specific welcome flow requirements

        boolean hasTeam = getSelectedTeam() > 0;
        boolean hasViewMode = getViewMode() != null && !getViewMode().isEmpty();
        boolean hasDynamicColors = true; // This has a default value

        int completedSteps = 0;
        if (hasTeam) completedSteps++;
        if (hasViewMode) completedSteps++;
        if (hasDynamicColors) completedSteps++;

        Log.d( TAG, "updateProgressBasedOnPreferences: Preference completion: " + completedSteps + "/3 steps" );
    }

    // ==================== ERROR HANDLING ====================

    /**
     * ADD this method for robust error handling during preference operations
     */
    private void handlePreferenceError(String operation, Exception error) {
        Log.e( TAG, "handlePreferenceError: Error during preference operation: " + operation, error );

        // Show user feedback
        if (this != null && !isFinishing()) {
            runOnUiThread( () -> {
                // You can show a toast or snackbar here
                Log.w( TAG, "handlePreferenceError: Preference error occurred, using defaults" );
            } );
        }

        // Apply safe defaults
        try {
            setDefaultPreferences();
        } catch (Exception fallbackError) {
            Log.e( TAG, "handlePreferenceError: Error applying fallback preferences", fallbackError );
        }
    }

    // ==================== QDUEUSER SERVICE INTEGRATION ====================

    /**
     * Check if QDueUser data is ready and trigger actual service onboarding
     */
    private void triggerQDueUserOnboarding() {
        if (!areDependenciesReady() || isQDueUserOnboardingCompleted()) {
            return;
        }

        String nickname = getQDueUserNickname();
        String email = getQDueUserEmail();

        performQDueUserOnboarding( nickname, email );
    }

    /**
     * Perform actual QDueUser onboarding using the service
     */
    private void performQDueUserOnboarding(String nickname, String email) {
        Log.d( TAG, "QDueUser Onboarding {nickname='" + nickname + "', email='" + email + "'}" );

        try {
            if (areDependenciesReady()) {
                QDueUserService qDueUserService = mServiceProvider.getQDueUserService();

                if (qDueUserService != null) {
                    qDueUserService.onboardUser( nickname, email )
                            .thenAccept( result -> {
                                runOnUiThread( () -> {
                                    if (result.isSuccess()) {
                                        net.calvuz.qdue.domain.qdueuser.models.QDueUser user = result.getData();
                                        Log.d( TAG, "QDueUser Onboarding Success {User=" + user.getDisplayName() + "}" );
                                        setQDueUserOnboardingCompleted();
                                    } else {
                                        Log.e( TAG, "QDueUser Onboarding Failed - " + result.getErrorMessage() );
                                    }
                                } );
                            } )
                            .exceptionally( throwable -> {
                                Log.e( TAG, "QDueUser Onboarding Exception", throwable );
                                return null;
                            } );
                } else {
                    Log.e( TAG, "performQDueUserOnboarding: QDueUserService not available" );
                }
            } else {
                Log.e( TAG, "performQDueUserOnboarding: Dependencies not ready" );
            }
        } catch (Exception e) {
            Log.e( TAG, "performQDueUserOnboarding: Exception", e );
        }
    }

    // =============================== WELCOME INTERFACE ==================================

    /**
     * Get SharedPreferences instance
     *
     * @return SharedPreferences instance
     */
    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Set selected team preference
     *
     * @param teamIndex Selected team ID
     */
    @Override
    public void setSelectedTeam(int teamIndex) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt( QDue.Settings.QD_KEY_SELECTED_TEAM, teamIndex );
        editor.apply();
    }

    /**
     * Get selected team preference
     *
     * @return Selected team ID or default (1)
     */
    @Override
    public int getSelectedTeam() {
        return preferences.getInt( QDue.Settings.QD_KEY_SELECTED_TEAM, 1 );
    }

    /**
     * Set view mode preference
     *
     * @param viewMode View mode to set
     */
    @Override
    public void setViewMode(String viewMode) {
        Log.d( TAG, "setViewMode: Setting view mode from welcome: " + viewMode );

        // Normalize the view mode value for consistency
        String normalizedViewMode;
        if ("dayslist".equals( viewMode ) || QDue.Settings.VIEW_MODE_DAYSLIST.equals( viewMode )) {
            normalizedViewMode = QDue.Settings.VIEW_MODE_DAYSLIST;
        } else {
            normalizedViewMode = QDue.Settings.VIEW_MODE_CALENDAR;
        }

        // Save using both systems for compatibility
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString( QDue.Settings.QD_KEY_VIEW_MODE, normalizedViewMode );
        editor.apply();

        Log.d( TAG, "setViewMode: View mode normalized and saved: " + normalizedViewMode );
    }

    /**
     * Get view mode preference
     *
     * @return View mode or default ("dayslist")
     */
    @Override
    public String getViewMode() {
        String viewMode = preferences.getString( QDue.Settings.QD_KEY_VIEW_MODE, QDue.Settings.VIEW_MODE_CALENDAR );
        Log.d( TAG, "getViewMode: Getting view mode from welcome: " + viewMode );
        return viewMode;
    }

    /**
     * Set dynamic colors enabled preference
     *
     * @param enabled True if enabled, false otherwise
     */
    @Override
    public void setDynamicColorsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, enabled );
        editor.apply();
    }

    /**
     * Get dynamic colors enabled preference
     *
     * @return True if enabled, false otherwise
     */
    @Override
    public boolean isDynamicColorsEnabled() {
        return preferences.getBoolean( QDue.Settings.QD_KEY_DYNAMIC_COLORS, true );
    }

    // ==================== FRAGMENT COMMUNICATION - QDUEUSER ====================

    @Override
    public void setQDueUserNickname(String nickname) {
        String safeNickname = nickname != null ? nickname.trim() : "";
        //Log.d(TAG, "setQDueUserNickname: Setting nickname: '" + safeNickname + "'");

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString( QD_KEY_QDUEUSER_NICKNAME, safeNickname );
        editor.apply();
    }

    @Override
    public String getQDueUserNickname() {
        String nickname = preferences.getString( QD_KEY_QDUEUSER_NICKNAME, "" );
        Log.d( TAG, "getQDueUserNickname: Retrieved nickname: '" + nickname + "'" );
        return nickname;
    }

    @Override
    public void setQDueUserEmail(String email) {
        String safeEmail = email != null ? email.trim() : "";
        //Log.d(TAG, "setQDueUserEmail: Setting email: '" + safeEmail + "'");

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString( QD_KEY_QDUEUSER_EMAIL, safeEmail );
        editor.apply();
    }

    @Override
    public String getQDueUserEmail() {
        String email = preferences.getString( QD_KEY_QDUEUSER_EMAIL, "" );
        Log.d( TAG, "getQDueUserEmail: Retrieved email: '" + email + "'" );
        return email;
    }

    @Override
    public boolean isQDueUserOnboardingCompleted() {
        boolean completed = preferences.getBoolean( QD_KEY_QDUEUSER_ONBOARDING_COMPLETED, false );
        Log.d( TAG, "isQDueUserOnboardingCompleted: " + completed );
        return completed;
    }

    @Override
    public void setQDueUserOnboardingCompleted() {
        Log.d( TAG, "setQDueUserOnboardingCompleted: Marking QDueUser onboarding as completed" );

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean( QD_KEY_QDUEUSER_ONBOARDING_COMPLETED, true );
        editor.apply();
    }

    @Override
    public boolean validateQDueUserData(String nickname, String email) {
        Log.d( TAG, "validateQDueUserData: Validating nickname: '" + nickname + "', email: '" + email + "'" );

        // Nickname validation (optional, so always valid)
        // Email validation (optional, but must be valid format if provided)
        if (email != null && !email.trim().isEmpty()) {
            boolean emailValid = EMAIL_PATTERN.matcher( email.trim() ).matches();
            Log.d( TAG, "validateQDueUserData: Email format valid: " + emailValid );
            return emailValid;
        }

        Log.d( TAG, "validateQDueUserData: Validation passed" );
        return true;
    }

    // ==================== FRAGMENT COMMUNICATION - PATTERN ASSIGNMENT ====================

    @Override
    public void onPatternAssignmentCompleted(boolean success) {
        Log.d( TAG, "Pattern assignment completed during onboarding: " + success );

        if (success) {
            // User completed pattern assignment
            Library.showSuccess( this, "Pattern di lavoro configurato" );
        } else {
            // User skipped - will use default assignment
            Library.showSuccess( this, "Potrai configurare il pattern in seguito nelle impostazioni" );
        }
    }
}