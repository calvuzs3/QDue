package net.calvuz.qdue.ui.features.assignment.wizard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.databinding.ActivityPatternAssignmentWizardBinding;
import net.calvuz.qdue.domain.calendar.models.RecurrenceRule;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.UserTeamAssignment;
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;
import net.calvuz.qdue.domain.calendar.usecases.UserTeamAssignmentUseCases;
import net.calvuz.qdue.domain.common.enums.Pattern;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.adapters.PatternAssignmentWizardAdapter;
import net.calvuz.qdue.ui.features.assignment.wizard.di.AssignmentWizardModule;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

import java.time.LocalDate;

/**
 * PatternAssignmentWizardActivity - Multi-Step Pattern Assignment Wizard
 *
 * <p>Comprehensive wizard for creating both QuattroDue standard and custom pattern assignments.
 * Uses ViewPager2 with multiple fragments to guide users through pattern selection, team
 * assignment, date selection, and confirmation.</p>
 *
 * <h3>Wizard Steps:</h3>
 * <ul>
 *   <li><strong>Step 1</strong>: Pattern Type Selection (QuattroDue vs Custom)</li>
 *   <li><strong>Step 2a</strong>: Team Selection (for QuattroDue patterns)</li>
 *   <li><strong>Step 2b</strong>: Custom Pattern Selection (for custom patterns)</li>
 *   <li><strong>Step 3</strong>: Date & Cycle Position Selection</li>
 *   <li><strong>Step 4</strong>: Confirmation & Assignment Creation</li>
 * </ul>
 *
 * <h3>Architecture:</h3>
 * <ul>
 *   <li><strong>Communication</strong>: AssignmentWizardInterface for fragment coordination</li>
 *   <li><strong>State Management</strong>: AssignmentWizardData model for wizard state</li>
 *   <li><strong>Business Logic</strong>: CreatePatternAssignmentUseCase integration</li>
 *   <li><strong>Navigation</strong>: Dynamic step flow based on pattern type selection</li>
 * </ul>
 *
 * <h3>Gap-Filling Strategy:</h3>
 * <ul>
 *   <li><strong>Permanent Assignments</strong>: Automatically close previous assignments</li>
 *   <li><strong>Validation</strong>: Check for conflicting assignments before creation</li>
 *   <li><strong>User Confirmation</strong>: Show impact of closing existing assignments</li>
 * </ul>
 */
public class PatternAssignmentWizardActivity extends AppCompatActivity implements
        Injectable,
        AssignmentWizardInterface

{

    private static final String TAG = "PatternAssignmentWizard";

    // ==================== INTENT EXTRAS ====================

    private static final String EXTRA_IS_FIRST_ASSIGNMENT = "is_first_assignment";
    private static final String EXTRA_EDIT_ASSIGNMENT_ID = "edit_assignment_id";
    private static final String DEFAULT_CUSTOM_PATTERN_TEAM = "A";

    // ==================== UI COMPONENTS ====================

    private ActivityPatternAssignmentWizardBinding mBinding;
    private PatternAssignmentWizardAdapter mWizardAdapter;
    private TabLayoutMediator mTabMediator;

    // ==================== STATE MANAGEMENT ====================

    private AssignmentWizardData mWizardData;
    private int mCurrentStep = 0;
    private final int mTotalSteps = 4; // Dynamic based on pattern type

    // ==================== DEPENDENCIES ====================

    private CreatePatternAssignmentUseCase mAssignmentUseCase;
    private UserTeamAssignmentUseCases mUserTeamAssignmentUseCase;
    private QDueUserService mQDueUserService;
    private CalendarServiceProvider mCalendarServiceProvider;

    // ==================== FACTORY METHODS ====================

    @NonNull
    public static Intent createIntent(@NonNull Context context, boolean isFirstAssignment) {
        Intent intent = new Intent( context, PatternAssignmentWizardActivity.class );
        intent.putExtra( EXTRA_IS_FIRST_ASSIGNMENT, isFirstAssignment );
        return intent;
    }

    @NonNull
    public static Intent createEditIntent(@NonNull Context context, @NonNull String assignmentId) {
        Intent intent = new Intent( context, PatternAssignmentWizardActivity.class );
        intent.putExtra( EXTRA_EDIT_ASSIGNMENT_ID, assignmentId );
        return intent;
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize data binding
        mBinding = ActivityPatternAssignmentWizardBinding.inflate( getLayoutInflater() );
        setContentView( mBinding.getRoot() );

        // Setup toolbar
        setupToolbar();

        // Initialize dependencies
        initializeDependencies();

        // Initialize wizard state
        initializeWizardData();

        // Setup ViewPager and wizard components
        setupWizard();

        // Setup navigation buttons
        setupNavigationButtons();

        // Update UI for initial state
        updateUI();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onBackPressed() {
        if (mCurrentStep > 0) {
            navigateToPreviousStep();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mTabMediator != null) {
            mTabMediator.detach();
        }
        super.onDestroy();
    }

    // ==================== INITIALIZATION ====================

    private void setupToolbar() {
        setSupportActionBar( mBinding.toolbar );
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled( true );
            getSupportActionBar().setTitle(
                    isFirstAssignment() ?
                            R.string.title_first_assignment_wizard :
                            R.string.title_change_assignment_wizard
            );
        }
    }

    private void initializeDependencies() {
        try {
            // Get ServiceProvider instance
            DependencyInjector.inject( this, this );

            AssignmentWizardModule wizardModule = AssignmentWizardModule.create( this );
            mAssignmentUseCase = wizardModule.getCreatePatternAssignmentUseCase();
            mUserTeamAssignmentUseCase = wizardModule.getUserTeamAssignmentUseCases();
        } catch (Exception e) {
            Log.e( TAG, "Error initializing dependencies", e );
            throw new RuntimeException( "Critical error: Dependency injection failed", e );
        }
    }

    private void initializeWizardData() {

        // Get Default User
        QDueUser currentUser = mQDueUserService.getPrimaryUser().join().getData();
        assert currentUser != null;

        // Get Team
        Team currentUserTeam = null;
        OperationResult<UserTeamAssignment> currentUserTeamAssignment = mCalendarServiceProvider
                .getUserTeamAssignmentUseCases()
                .getGetUserTeamAssignmentForDateUseCase()
                .execute(
                        currentUser.getId(),
                        LocalDate.now() )
                .join();
        if (currentUserTeamAssignment.isSuccess()) {
            String currentUserTeamID = currentUserTeamAssignment.getData().getTeamID();
            assert currentUserTeamID != null;

            OperationResult<Team> team = mCalendarServiceProvider.getTeamUseCases().getGetTeamUseCase().execute( currentUserTeamID ).join();
            if (team.isSuccess()) {
                currentUserTeam = team.getData();
                assert currentUserTeam != null;
            }
        }

        mWizardData = new AssignmentWizardData( currentUser, currentUserTeam );
//        mWizardData.setUserId( QDuePreferences.getUserId( this ) );       //   ):X
        mWizardData.setFirstAssignment( isFirstAssignment() );

        String editAssignmentId = getIntent().getStringExtra( EXTRA_EDIT_ASSIGNMENT_ID );
        if (editAssignmentId != null) {
            mWizardData.setEditingAssignmentId( editAssignmentId );
            loadExistingAssignmentForEdit( editAssignmentId );
        }
    }

    private void setupWizard() {
        // Create adapter
        mWizardAdapter = new PatternAssignmentWizardAdapter( this, mWizardData );
        mBinding.viewPager.setAdapter( mWizardAdapter );

        // Setup ViewPager change listener
        mBinding.viewPager.registerOnPageChangeCallback( new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mCurrentStep = position;
                updateUI();
            }
        } );

        // Connect TabLayout with ViewPager
        setupTabIndicators();
    }

    private void setupTabIndicators() {
        mTabMediator = new TabLayoutMediator( mBinding.tabIndicator, mBinding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: // Pattern Type Selection
                            tab.setIcon( R.drawable.ic_rounded_category_24 );
                            tab.setContentDescription( getString( R.string.wizard_step_pattern_type ) );
                            break;
                        case 1: // Team/Custom Selection
                            tab.setIcon( mWizardData.getPattern() == Pattern.QUATTRODUE ?
                                    R.drawable.ic_rounded_group_24 : R.drawable.ic_rounded_edit_24 );
                            tab.setContentDescription( getString( R.string.wizard_step_selection ) );
                            break;
                        case 2: // Date & Position
                            tab.setIcon( R.drawable.ic_rounded_calendar_month_24 );
                            tab.setContentDescription( getString( R.string.wizard_step_date_position ) );
                            break;
                        case 3: // Confirmation
                            tab.setIcon( R.drawable.ic_rounded_check_circle_24 );
                            tab.setContentDescription( getString( R.string.wizard_step_confirmation ) );
                            break;
                    }
                } );
        mTabMediator.attach();
    }

    private void setupNavigationButtons() {
        mBinding.btnNext.setOnClickListener( v -> navigateToNextStep() );
        mBinding.btnPrevious.setOnClickListener( v -> navigateToPreviousStep() );
        mBinding.btnCancel.setOnClickListener( v -> finish() );
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * Inject dependencies into this component
     *
     * @param serviceProvider Service provider
     */
    @Override
    public void inject(ServiceProvider serviceProvider) {
        this.mQDueUserService = serviceProvider.getQDueUserService();
        this.mCalendarServiceProvider = serviceProvider.getCalendarServiceProvider();
    }

    /**
     * Check if dependencies are injected and ready
     */
    @Override
    public boolean areDependenciesReady() {
        return mCalendarServiceProvider != null &&
                mQDueUserService != null;
    }

    // ==================== NAVIGATION ====================

    private void navigateToNextStep() {
        if (mCurrentStep < mTotalSteps - 1) {
            if (validateCurrentStep()) {
                mCurrentStep++;
                mBinding.viewPager.setCurrentItem( mCurrentStep, true );
            }
        } else {
            // Final step - create assignment
            createAssignment();
        }
    }

    private void navigateToPreviousStep() {
        if (mCurrentStep > 0) {
            mCurrentStep--;
            mBinding.viewPager.setCurrentItem( mCurrentStep, true );
        }
    }

    private boolean validateCurrentStep() {
        switch (mCurrentStep) {
            case 0: // Pattern Type Selection
                return mWizardData.getPattern() != null;
            case 1: // Team/Custom Selection
                if (mWizardData.getPattern() == Pattern.QUATTRODUE) {
                    return mWizardData.getSelectedTeam() != null;
                } else {
                    return mWizardData.getSelectedCustomPattern() != null;
                }
            case 2: // Date & Position
                return mWizardData.getAssignmentDate() != null &&
                        mWizardData.getCycleDayPosition() > 0;
            case 3: // Confirmation
                return true; // Always valid
            default:
                return false;
        }
    }

    private void updateUI() {
        // Update progress text
        mBinding.progressText.setText( getString( R.string.wizard_progress, mCurrentStep + 1, mTotalSteps ) );

        // Update button states
        mBinding.btnPrevious.setVisibility( mCurrentStep > 0 ? View.VISIBLE : View.GONE );

        if (mCurrentStep == mTotalSteps - 1) {
            mBinding.btnNext.setText( R.string.wizard_create_assignment );
            mBinding.btnNext.setIcon( AppCompatResources.getDrawable( this, R.drawable.ic_rounded_check_24 ) );
        } else {
            mBinding.btnNext.setText( R.string.wizard_next );
            mBinding.btnNext.setIcon( AppCompatResources.getDrawable( this, R.drawable.ic_rounded_arrow_forward_24 ) );
        }

        // Update step validation
        mBinding.btnNext.setEnabled( validateCurrentStep() );

        Log.d( TAG, "UI updated for step " + (mCurrentStep + 1) + "/" + mTotalSteps );
    }

    // ==================== ASSIGNMENT CREATION ====================

    private void createAssignment() {
        mBinding.progressBar.setVisibility( View.VISIBLE );
        mBinding.btnNext.setEnabled( false );

        if (mWizardData.getPattern() == Pattern.QUATTRODUE) {
            createQuattroDueAssignment();
        } else {
            createCustomPatternAssignment();
        }
    }

    private void createQuattroDueAssignment() {
        assert mWizardData.getUserId() != null;
        assert mWizardData.getAssignmentDate() != null;
        assert mWizardData.getSelectedTeam() != null;

        mAssignmentUseCase.createQuattroDueAssignment(
                mWizardData.getUserId(),
                mWizardData.getSelectedTeam().getName(),
                mWizardData.getAssignmentDate(),
                mWizardData.getCycleDayPosition()
        ).thenAccept( result -> runOnUiThread( () -> handleAssignmentResult( result ) ) );
    }

    private void createCustomPatternAssignment() {
        assert mWizardData.getUserId() != null;
        assert mWizardData.getSelectedTeam() != null;
        assert mWizardData.getAssignmentDate() != null;
        assert mWizardData.getSelectedCustomPattern() != null;

        // Create Team Assignment
        mUserTeamAssignmentUseCase.getCreateUserTeamAssignmentUseCase().execute(
                mWizardData.getUserId(),
                mWizardData.getSelectedTeam().getId(),
                mWizardData.getAssignmentDate(),
                null,
                "Wizard Pattern Assignment",
                mWizardData.getUserId()
        ).thenAccept( result -> runOnUiThread( () ->
                Log.d( TAG, "Team assignment result: " + result.isSuccess() )
        ) );

        // Create Pattern Assignment
        mAssignmentUseCase.createCustomPatternAssignment(
                mWizardData.getUserId(),
                mWizardData.getSelectedCustomPattern().getId(),
                DEFAULT_CUSTOM_PATTERN_TEAM, // Default team A for custom patterns
                mWizardData.getAssignmentDate(),
                mWizardData.getCycleDayPosition()
        ).thenAccept( result -> runOnUiThread( () ->
                handleAssignmentResult( result )
        ) );
    }

    private void handleAssignmentResult(@NonNull OperationResult<UserScheduleAssignment> result) {
        mBinding.progressBar.setVisibility( View.GONE );
        mBinding.btnNext.setEnabled( true );

        if (result.isSuccess()) {
            Toast.makeText( this, R.string.success_assignment_created, Toast.LENGTH_SHORT ).show();
            setResult( RESULT_OK );
            finish();
        } else {
            Toast.makeText( this, getString( R.string.error_assignment_creation_failed, result.getErrorMessage() ),
                    Toast.LENGTH_LONG ).show();
            Log.e( TAG, "Assignment creation failed: " + result.getErrorMessage() );
        }
    }

    // ==================== ASSIGNMENT WIZARD INTERFACE ====================

    @Override
    public void onPatternSelected(@NonNull Pattern pattern) {
        mWizardData.setPattern( pattern );

        // Reset selections when pattern type changes
        mWizardData.setSelectedTeam( null );
        mWizardData.setSelectedCustomPattern( null );

        // Update adapter and UI
        mWizardAdapter.notifyPatternChanged();
        updateTabIndicators();
        updateUI();

        Log.d( TAG, "Pattern type selected: " + pattern );
    }

    @Override
    public void onTeamSelected(@NonNull Team team) {
        mWizardData.setSelectedTeam( team );
        updateUI();
        Log.d( TAG, "Team selected: " + team.getName() + " (offset: " + team.getQdueOffset() + ")" );
    }

    @Override
    public void onCustomPatternSelected(@NonNull RecurrenceRule customPattern) {
        mWizardData.setSelectedCustomPattern( customPattern );
        updateUI();
        Log.d( TAG, "Custom pattern selected: " + customPattern.getName() );
    }

    @Override
    public void onDateSelected(@NonNull LocalDate assignmentDate) {
        mWizardData.setAssignmentDate( assignmentDate );
        updateUI();
        Log.d( TAG, "Assignment date selected: " + assignmentDate );
    }

    @Override
    public void onCycleDayPositionSelected(int cycleDayPosition) {
        mWizardData.setCycleDayPosition( cycleDayPosition );
        updateUI();
        Log.d( TAG, "Cycle day position selected: " + cycleDayPosition );
    }

    @Override
    public void onNavigateToNextStep() {
        navigateToNextStep();
    }

    @Override
    public void onNavigateToPreviousStep() {
        navigateToPreviousStep();
    }

    @Override
    @NonNull
    public AssignmentWizardData getWizardData() {
        return mWizardData;
    }

    @Override
    @NonNull
    public CreatePatternAssignmentUseCase getAssignmentUseCase() {
        return mAssignmentUseCase;
    }

    // ==================== HELPERS ====================

    @Override
    public boolean isFirstAssignment() {
        return getIntent().getBooleanExtra( EXTRA_IS_FIRST_ASSIGNMENT, false );
    }

    private void updateTabIndicators() {
        if (mTabMediator != null) {
            mTabMediator.detach();
            setupTabIndicators();
        }
    }

    private void loadExistingAssignmentForEdit(@NonNull String assignmentId) {

        OperationResult<UserTeamAssignment> result =        mCalendarServiceProvider
                .getUserTeamAssignmentUseCases()
                .getGetUserTeamAssignmentUseCase()
                .execute( assignmentId ).join();


        // TODO: Load existing assignment data for editing
        Log.d( TAG, "Loading assignment for edit: " + assignmentId );
    }
}