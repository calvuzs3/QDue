// WelcomeFragmentAdapter.java
package net.calvuz.qdue.ui.features.welcome;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * FragmentStateAdapter for Welcome ViewPager2
 * Manages the sequence of welcome screen fragments
 * <p>
 * Welcome Flow Steps:
 * 1. IntroductionFragment - App introduction and overview
 * 2. TeamSelectionFragment - Choose user's team (1-9)
 * 3. ViewModeFragment - Select preferred view (Calendar vs DaysList)
 * 4. FeaturesOverviewFragment - Showcase app capabilities
 * 5. PersonalizationFragment - Configure colors and preferences
 */
public class WelcomeFragmentAdapter extends FragmentStateAdapter {

    // List of welcome step fragments
    private final List<Fragment> fragments;

    /**
     * Constructor - initializes all welcome fragments
     * @param fragmentActivity The parent activity
     */
    public WelcomeFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragments = createWelcomeFragments();
    }

    /**
     * Create and return the list of welcome fragments in order
     * @return List of welcome step fragments
     */
    private List<Fragment> createWelcomeFragments() {
        List<Fragment> fragmentList = new ArrayList<>();

        // Step 0: Introduction and App Overview
        fragmentList.add(new IntroductionFragment());

        // Step 1: Features Overview (moved in into section)
//        fragmentList.add(new FeaturesOverviewFragment());

        // Step 2: Team Selection (9 teams available)
        fragmentList.add(new TeamSelectionFragment());

        // Step 3: View Mode Selection
        fragmentList.add(new ViewModeFragment());

        // Step 4: Personalization (colors, etc.)
        fragmentList.add(new PersonalizationFragment());

        // Last Step: Complete
        // change "continue" button with "get started" text

        return fragmentList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return the fragment at the specified position
        if (position >= 0 && position < fragments.size()) {
            return fragments.get(position);
        }

        // Fallback to introduction fragment if position is invalid
        return new IntroductionFragment();
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    /**
     * Get fragment at specific position (utility method)
     * @param position Fragment position
     * @return Fragment instance or null if invalid position
     */
    public Fragment getFragmentAt(int position) {
        if (position >= 0 && position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }

    /**
     * Get total number of welcome steps
     * @return Total step count
     */
    public int getTotalSteps() {
        return fragments.size();
    }

    /**
     * Check if position is the last step
     * @param position Current position
     * @return True if this is the final step
     */
    public boolean isLastStep(int position) {
        return position == (getItemCount() - 1);
    }

    /**
     * Check if position is the first step
     * @param position Current position
     * @return True if this is the first step
     */
    public boolean isFirstStep(int position) {
        return position == 0;
    }
}