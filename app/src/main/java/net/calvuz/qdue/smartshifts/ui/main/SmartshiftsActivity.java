// ===== PACKAGE: net.calvuz.qdue.smartshifts.ui.main =====

package net.calvuz.qdue.smartshifts.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.navigation.NavigationBarView;

import dagger.hilt.android.AndroidEntryPoint;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.ActivitySmartShiftsBinding;
import net.calvuz.qdue.smartshifts.ui.calendar.SmartShiftsCalendarFragment;
import net.calvuz.qdue.smartshifts.ui.patterns.ShiftPatternsFragment;
import net.calvuz.qdue.smartshifts.ui.contacts.TeamContactsFragment;
import net.calvuz.qdue.smartshifts.ui.settings.SmartShiftsSettingsFragment;
import net.calvuz.qdue.smartshifts.ui.setup.ShiftSetupWizardActivity;

/**
 * Main activity for SmartShifts system
 * Manages bottom navigation and fragment switching
 */
@AndroidEntryPoint
public class SmartshiftsActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivitySmartShiftsBinding binding;
    private SmartShiftsViewModel viewModel;

    // Fragment instances
    private SmartShiftsCalendarFragment calendarFragment;
    private ShiftPatternsFragment patternsFragment;
    private TeamContactsFragment contactsFragment;
    private SmartShiftsSettingsFragment settingsFragment;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySmartShiftsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SmartShiftsViewModel.class);

        setupToolbar();
        setupBottomNavigation();
        setupObservers();

        // Check if user needs setup
        checkFirstTimeSetup();

        // Load default fragment if no saved state
        if (savedInstanceState == null) {
            loadFragment(createCalendarFragment(), false);
        }
    }

    /**
     * Setup toolbar with title and actions
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.menu_smart_shifts);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Setup bottom navigation
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(this);
        binding.bottomNavigation.setSelectedItemId(R.id.nav_calendar);
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe if user has active assignment
        viewModel.hasActiveAssignment().observe(this, hasAssignment -> {
            if (!hasAssignment) {
                // User needs to complete setup
                startSetupWizard();
            }
        });

        // Observe loading state
        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                showError(errorMessage);
                viewModel.clearErrorMessage();
            }
        });
    }

    /**
     * Check if user needs first-time setup
     */
    private void checkFirstTimeSetup() {
        viewModel.checkFirstTimeSetup();
    }

    /**
     * Start setup wizard
     */
    private void startSetupWizard() {
        Intent intent = new Intent(this, ShiftSetupWizardActivity.class);
        startActivity(intent);
        // Don't finish this activity - let user return if they cancel setup
    }

    /**
     * Handle bottom navigation item selection
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_calendar) {
            fragment = createCalendarFragment();
        } else if (itemId == R.id.nav_patterns) {
            fragment = createPatternsFragment();
        } else if (itemId == R.id.nav_contacts) {
            fragment = createContactsFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = createSettingsFragment();
        }

        if (fragment != null) {
            loadFragment(fragment, true);
            return true;
        }

        return false;
    }

    /**
     * Load fragment into container
     */
    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        if (fragment == currentFragment) {
            return; // Already loaded
        }

        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
        currentFragment = fragment;
    }

    /**
     * Create or get calendar fragment
     */
    private Fragment createCalendarFragment() {
        if (calendarFragment == null) {
            calendarFragment = new SmartShiftsCalendarFragment();
        }
        return calendarFragment;
    }

    /**
     * Create or get patterns fragment
     */
    private Fragment createPatternsFragment() {
        if (patternsFragment == null) {
            patternsFragment = new ShiftPatternsFragment();
        }
        return patternsFragment;
    }

    /**
     * Create or get contacts fragment
     */
    private Fragment createContactsFragment() {
        if (contactsFragment == null) {
            contactsFragment = new TeamContactsFragment();
        }
        return contactsFragment;
    }

    /**
     * Create or get settings fragment
     */
    private Fragment createSettingsFragment() {
        if (settingsFragment == null) {
            settingsFragment = new SmartShiftsSettingsFragment();
        }
        return settingsFragment;
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Handle up navigation
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        com.google.android.material.snackbar.Snackbar.make(
                binding.getRoot(),
                message,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
