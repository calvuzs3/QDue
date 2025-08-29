package net.calvuz.qdue.ui.features.assignment.wizard.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.fragments.ConfirmationFragment;
import net.calvuz.qdue.ui.features.assignment.wizard.fragments.CustomPatternSelectionFragment;
import net.calvuz.qdue.ui.features.assignment.wizard.fragments.DatePositionSelectionFragment;
import net.calvuz.qdue.ui.features.assignment.wizard.fragments.PatternTypeSelectionFragment;
import net.calvuz.qdue.ui.features.assignment.wizard.fragments.TeamSelectionFragment;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

/**
 * PatternAssignmentWizardAdapter - ViewPager2 Adapter for Assignment Wizard
 *
 * <p>Manages fragment creation and navigation for the multi-step assignment wizard.
 * Provides dynamic fragment selection based on wizard state and pattern type.</p>
 *
 * <h3>Wizard Flow:</h3>
 * <ul>
 *   <li><strong>Step 0</strong>: Pattern Type Selection (QuattroDue vs Custom)</li>
 *   <li><strong>Step 1</strong>: Team Selection (QuattroDue) OR Custom Pattern Selection (Custom)</li>
 *   <li><strong>Step 2</strong>: Date & Cycle Position Selection</li>
 *   <li><strong>Step 3</strong>: Confirmation & Assignment Creation</li>
 * </ul>
 *
 * <h3>Dynamic Fragment Selection:</h3>
 * <ul>
 *   <li><strong>QuattroDue Flow</strong>: PatternType → Team → DatePosition → Confirmation</li>
 *   <li><strong>Custom Flow</strong>: PatternType → CustomPattern → DatePosition → Confirmation</li>
 *   <li><strong>Adaptive Navigation</strong>: Fragment creation adapts to selected pattern type</li>
 * </ul>
 *
 * <h3>Fragment State Management:</h3>
 * <ul>
 *   <li><strong>Stateful Fragments</strong>: Maintains fragment instances for back navigation</li>
 *   <li><strong>Data Synchronization</strong>: All fragments access shared AssignmentWizardData</li>
 *   <li><strong>Dynamic Updates</strong>: Adapter updates when pattern type changes</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment Wizard ViewPager Adapter
 * @since Clean Architecture Phase 2
 */
public class PatternAssignmentWizardAdapter extends FragmentStateAdapter {

    private static final String TAG = "PatternAssignmentWizardAdapter";

    // ==================== FRAGMENT STEP INDICES ====================

    public static final int STEP_PATTERN_TYPE = 0;
    public static final int STEP_SELECTION = 1; // Team or Custom Pattern
    public static final int STEP_DATE_POSITION = 2;
    public static final int STEP_CONFIRMATION = 3;
    public static final int TOTAL_STEPS = 4;

    // ==================== STATE MANAGEMENT ====================

    private final AssignmentWizardData mWizardData;

    // Fragment instances for state preservation
    private PatternTypeSelectionFragment mPatternTypeFragment;
    private TeamSelectionFragment mTeamSelectionFragment;
    private CustomPatternSelectionFragment mCustomPatternSelectionFragment;
    private DatePositionSelectionFragment mDatePositionFragment;
    private ConfirmationFragment mConfirmationFragment;

    // ==================== CONSTRUCTOR ====================

    public PatternAssignmentWizardAdapter(@NonNull FragmentActivity fragmentActivity,
                                          @NonNull AssignmentWizardData wizardData) {
        super(fragmentActivity);
        this.mWizardData = wizardData;
    }

    // ==================== FRAGMENTSTATEADAPTER IMPLEMENTATION ====================

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "Creating fragment for position: " + position +
                ", pattern type: " + mWizardData.getPatternType());

        switch (position) {
            case STEP_PATTERN_TYPE:
                return createPatternTypeSelectionFragment();

            case STEP_SELECTION:
                return createSelectionFragment();

            case STEP_DATE_POSITION:
                return createDatePositionSelectionFragment();

            case STEP_CONFIRMATION:
                return createConfirmationFragment();

            default:
                Log.w(TAG, "Invalid fragment position: " + position + ", creating pattern type fragment");
                return createPatternTypeSelectionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TOTAL_STEPS;
    }

    // ==================== FRAGMENT CREATION METHODS ====================

    @NonNull
    private Fragment createPatternTypeSelectionFragment() {
        if (mPatternTypeFragment == null) {
            mPatternTypeFragment = new PatternTypeSelectionFragment();
            Log.d(TAG, "Created new PatternTypeSelectionFragment");
        }
        return mPatternTypeFragment;
    }

    @NonNull
    private Fragment createSelectionFragment() {
        AssignmentWizardData.PatternType patternType = mWizardData.getPatternType();

        if (patternType == null) {
            Log.w(TAG, "Pattern type not selected, defaulting to team selection fragment");
            return createTeamSelectionFragment();
        }

        switch (patternType) {
            case QUATTRODUE:
                return createTeamSelectionFragment();
            case CUSTOM:
                return createCustomPatternSelectionFragment();
            default:
                Log.w(TAG, "Unknown pattern type: " + patternType + ", defaulting to team selection");
                return createTeamSelectionFragment();
        }
    }

    @NonNull
    private Fragment createTeamSelectionFragment() {
        if (mTeamSelectionFragment == null) {
            mTeamSelectionFragment = new TeamSelectionFragment();
            Log.d(TAG, "Created new TeamSelectionFragment");
        }
        return mTeamSelectionFragment;
    }

    @NonNull
    private Fragment createCustomPatternSelectionFragment() {
        if (mCustomPatternSelectionFragment == null) {
            mCustomPatternSelectionFragment = new CustomPatternSelectionFragment();
            Log.d(TAG, "Created new CustomPatternSelectionFragment");
        }
        return mCustomPatternSelectionFragment;
    }

    @NonNull
    private Fragment createDatePositionSelectionFragment() {
        if (mDatePositionFragment == null) {
            mDatePositionFragment = new DatePositionSelectionFragment();
            Log.d(TAG, "Created new DatePositionSelectionFragment");
        }
        return mDatePositionFragment;
    }

    @NonNull
    private Fragment createConfirmationFragment() {
        if (mConfirmationFragment == null) {
            mConfirmationFragment = new ConfirmationFragment();
            Log.d(TAG, "Created new ConfirmationFragment");
        }
        return mConfirmationFragment;
    }

    // ==================== DYNAMIC ADAPTER UPDATES ====================

    /**
     * Notify adapter that pattern type has changed.
     * This may require recreating Step 1 fragment with different type.
     */
    public void notifyPatternTypeChanged() {
        Log.d(TAG, "Pattern type changed to: " + mWizardData.getPatternType() +
                ", clearing selection fragments");

        // Clear cached selection fragments since pattern type determines which one to use
        mTeamSelectionFragment = null;
        mCustomPatternSelectionFragment = null;

        // Notify adapter of potential changes
        notifyItemChanged(STEP_SELECTION);

        Log.d(TAG, "Selection fragment cleared and adapter notified");
    }

    /**
     * Reset all fragments for fresh wizard start.
     * Used when wizard is restarted or data is reset.
     */
    public void resetAllFragments() {
        Log.d(TAG, "Resetting all wizard fragments");

        mPatternTypeFragment = null;
        mTeamSelectionFragment = null;
        mCustomPatternSelectionFragment = null;
        mDatePositionFragment = null;
        mConfirmationFragment = null;

        notifyDataSetChanged();
    }

    // ==================== FRAGMENT ACCESS METHODS ====================

    /**
     * Get current fragment instance for given position.
     * Returns null if fragment hasn't been created yet.
     *
     * @param position Fragment position
     * @return Fragment instance or null
     */
    @NonNull
    public Fragment getFragmentAt(int position) {
        switch (position) {
            case STEP_PATTERN_TYPE:
                return mPatternTypeFragment != null ? mPatternTypeFragment : createPatternTypeSelectionFragment();
            case STEP_SELECTION:
                return createSelectionFragment(); // Always create dynamically based on pattern type
            case STEP_DATE_POSITION:
                return mDatePositionFragment != null ? mDatePositionFragment : createDatePositionSelectionFragment();
            case STEP_CONFIRMATION:
                return mConfirmationFragment != null ? mConfirmationFragment : createConfirmationFragment();
            default:
                return createPatternTypeSelectionFragment();
        }
    }

    /**
     * Check if fragment at position has been created and is available.
     *
     * @param position Fragment position
     * @return true if fragment exists
     */
    public boolean hasFragmentAt(int position) {
        switch (position) {
            case STEP_PATTERN_TYPE:
                return mPatternTypeFragment != null;
            case STEP_SELECTION:
                AssignmentWizardData.PatternType patternType = mWizardData.getPatternType();
                if (patternType == AssignmentWizardData.PatternType.QUATTRODUE) {
                    return mTeamSelectionFragment != null;
                } else if (patternType == AssignmentWizardData.PatternType.CUSTOM) {
                    return mCustomPatternSelectionFragment != null;
                } else {
                    return false;
                }
            case STEP_DATE_POSITION:
                return mDatePositionFragment != null;
            case STEP_CONFIRMATION:
                return mConfirmationFragment != null;
            default:
                return false;
        }
    }

    // ==================== FRAGMENT VALIDATION HELPERS ====================

    /**
     * Get fragment title for given position.
     * Used for tab indicators and accessibility.
     *
     * @param position Fragment position
     * @return Fragment title
     */
    @NonNull
    public String getFragmentTitle(int position) {
        switch (position) {
            case STEP_PATTERN_TYPE:
                return "Pattern Type";
            case STEP_SELECTION:
                AssignmentWizardData.PatternType patternType = mWizardData.getPatternType();
                if (patternType == AssignmentWizardData.PatternType.QUATTRODUE) {
                    return "Team Selection";
                } else if (patternType == AssignmentWizardData.PatternType.CUSTOM) {
                    return "Custom Pattern";
                } else {
                    return "Selection";
                }
            case STEP_DATE_POSITION:
                return "Date & Position";
            case STEP_CONFIRMATION:
                return "Confirmation";
            default:
                return "Step " + (position + 1);
        }
    }

    /**
     * Check if fragment at position is ready for navigation away.
     * Validates fragment-specific completion requirements.
     *
     * @param position Fragment position to validate
     * @return true if fragment is complete and ready for navigation
     */
    public boolean isFragmentReadyForNavigation(int position) {
        switch (position) {
            case STEP_PATTERN_TYPE:
                return mWizardData.isStep1Complete();
            case STEP_SELECTION:
                return mWizardData.isStep2Complete();
            case STEP_DATE_POSITION:
                return mWizardData.isStep3Complete();
            case STEP_CONFIRMATION:
                return mWizardData.isWizardComplete();
            default:
                return false;
        }
    }

    // ==================== DEBUGGING AND LOGGING ====================

    /**
     * Get debug information about current adapter state.
     * Used for troubleshooting fragment creation and state issues.
     *
     * @return Debug information string
     */
    @NonNull
    public String getDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("PatternAssignmentWizardAdapter Debug Info:\n");
        debug.append("  Pattern Type: ").append(mWizardData.getPatternType()).append("\n");
        debug.append("  Total Steps: ").append(getItemCount()).append("\n");
        debug.append("  Fragment States:\n");
        debug.append("    PatternType: ").append(mPatternTypeFragment != null ? "Created" : "Not Created").append("\n");
        debug.append("    TeamSelection: ").append(mTeamSelectionFragment != null ? "Created" : "Not Created").append("\n");
        debug.append("    CustomPattern: ").append(mCustomPatternSelectionFragment != null ? "Created" : "Not Created").append("\n");
        debug.append("    DatePosition: ").append(mDatePositionFragment != null ? "Created" : "Not Created").append("\n");
        debug.append("    Confirmation: ").append(mConfirmationFragment != null ? "Created" : "Not Created").append("\n");
        debug.append("  Wizard Completion: ").append(mWizardData.getCompletionPercentage()).append("%\n");

        return debug.toString();
    }

    /**
     * Log current adapter state for debugging.
     */
    public void logCurrentState() {
        Log.d(TAG, getDebugInfo());
    }
}