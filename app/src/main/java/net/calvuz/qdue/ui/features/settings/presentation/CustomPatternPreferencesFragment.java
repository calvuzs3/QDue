package net.calvuz.qdue.ui.features.settings.presentation;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.ui.features.schedulepattern.UserSchedulePatternCreationActivity;
import net.calvuz.qdue.ui.features.schedulepattern.di.SchedulePatternModule;
import net.calvuz.qdue.ui.features.schedulepattern.services.UserSchedulePatternService;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CustomPatternPreferencesFragment - Preferences for Custom Schedule Patterns
 *
 * <p>PreferenceFragment that allows users to manage their custom work schedule patterns
 * through the settings interface. Provides access to create, edit, and delete custom patterns.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Preferences Integration
 * @since Clean Architecture Phase 2
 */
public class CustomPatternPreferencesFragment extends PreferenceFragmentCompat implements Injectable {

    private static final String TAG = "CustomPatternPreferencesFragment";

    private static final int REQUEST_CREATE_PATTERN = 2001;
    private static final int REQUEST_EDIT_PATTERN = 2002;

    // ==================== INJECTED DEPENDENCIES ====================

    private UserSchedulePatternService mPatternService;
    private ServiceProvider mServiceProvider;

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Dependency injection
        DependencyInjector.inject(this, getActivity());

        if (!areDependenciesReady()) {
            Log.e(TAG, "Dependencies not ready");
            return;
        }

        // Set title for QDueSettingsActivity
        if (getActivity() != null) {
            getActivity().setTitle(R.string.pref_custom_patterns_title);
        }

        // Create preference screen
        createPreferenceScreen();

        // Load existing patterns
        loadExistingPatterns();

        Log.d(TAG, "CustomPatternPreferencesFragment created");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh patterns when returning from pattern creation/editing
        loadExistingPatterns();
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(@NonNull ServiceProvider serviceProvider) {
        mServiceProvider = serviceProvider;

        SchedulePatternModule module = new SchedulePatternModule(requireContext(), serviceProvider);
        mPatternService = module.getUserSchedulePatternService();

        Log.d(TAG, "Dependencies injected successfully");
    }

    @Override
    public boolean areDependenciesReady() {
        return mPatternService != null && mServiceProvider != null;
    }

    // ==================== PREFERENCE SCREEN CREATION ====================

    private void createPreferenceScreen() {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        // Create new pattern preference
        Preference createNewPreference = new Preference(requireContext());
        createNewPreference.setKey(getString( R.string.qd_preference_pattern_create_new_pattern ));
        createNewPreference.setTitle(R.string.pref_create_new_pattern_title);
        createNewPreference.setSummary(R.string.pref_create_new_pattern_summary);
        createNewPreference.setIcon(R.drawable.ic_rounded_add_24);
        createNewPreference.setOnPreferenceClickListener(preference -> {
            launchPatternCreation();
            return true;
        });
        screen.addPreference(createNewPreference);

        // Create category for existing patterns
        PreferenceCategory existingPatternsCategory = new PreferenceCategory(requireContext());
        existingPatternsCategory.setKey(getString( R.string.qd_preference_pattern_existing_patterns_category ));
        existingPatternsCategory.setTitle(R.string.pref_existing_patterns_category_title);
        screen.addPreference(existingPatternsCategory);
    }

    // ==================== PATTERN MANAGEMENT ====================

    private void launchPatternCreation() {
        Intent intent = UserSchedulePatternCreationActivity.createIntent(requireContext());
        startActivityForResult(intent, REQUEST_CREATE_PATTERN);
        Log.d(TAG, "Launched pattern creation activity");
    }

    private void launchPatternEditing(@NonNull String assignmentId) {
        Intent intent = UserSchedulePatternCreationActivity.createEditIntent(requireContext(), assignmentId);
        startActivityForResult(intent, REQUEST_EDIT_PATTERN);
        Log.d(TAG, "Launched pattern editing for: " + assignmentId);
    }

    private void loadExistingPatterns() {
        PreferenceCategory category = findPreference(getString( R.string.qd_preference_pattern_existing_patterns_category ));
        if (category == null) {
            Log.w(TAG, "Existing patterns category not found");
            return;
        }

        // Clear existing pattern preferences
        category.removeAll();

        // Load patterns from service
        mPatternService.getUserPatterns()
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> handlePatternsLoaded(result, category));
                    }
                })
                .exceptionally(throwable -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Error loading patterns", throwable);
                            showEmptyPatternsMessage(category);
                        });
                    }
                    return null;
                });
    }

    private void handlePatternsLoaded(@NonNull OperationResult<List<UserScheduleAssignment>> result,
                                      @NonNull PreferenceCategory category) {
        if (result.isSuccess() && !result.getData().isEmpty()) {
            List<UserScheduleAssignment> patterns = result.getData();
            Log.d(TAG, "Loaded " + patterns.size() + " user patterns");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (UserScheduleAssignment pattern : patterns) {
                Preference patternPreference = createPatternPreference(pattern, dateFormatter);
                category.addPreference(patternPreference);
            }
        } else {
            showEmptyPatternsMessage(category);
        }
    }

    @NonNull
    private Preference createPatternPreference(@NonNull UserScheduleAssignment pattern,
                                               @NonNull DateTimeFormatter dateFormatter) {
        Preference patternPreference = new Preference(requireContext());

        String key = getString( R.string.qd_preference_pattern_prefix ) + pattern.getId();
        patternPreference.setKey(key);

        // Set title and summary
        String title = pattern.getTitle() != null ? pattern.getTitle() :
                getString(R.string.pref_pattern_default_name);
        patternPreference.setTitle(title);

        String summary = getString(R.string.pref_pattern_summary_format,
                pattern.getStartDate().format(dateFormatter),
                pattern.getStatus().name());
        patternPreference.setSummary(summary);

        // Set icon based on status
        int iconRes = pattern.getStatus() == UserScheduleAssignment.Status.ACTIVE ?
                R.drawable.ic_rounded_schedule_24 : R.drawable.ic_empty_schedule_24;
        patternPreference.setIcon(iconRes);

        // Set click listener
        patternPreference.setOnPreferenceClickListener(preference -> {
            showPatternOptionsDialog(pattern);
            return true;
        });

        return patternPreference;
    }

    private void showEmptyPatternsMessage(@NonNull PreferenceCategory category) {
        Preference emptyPreference = new Preference(requireContext());
        emptyPreference.setTitle(R.string.pref_no_patterns_title);
        emptyPreference.setSummary(R.string.pref_no_patterns_summary);
        emptyPreference.setIcon(R.drawable.ic_rounded_info_24);
        emptyPreference.setSelectable(false);
        category.addPreference(emptyPreference);
    }

    private void showPatternOptionsDialog(@NonNull UserScheduleAssignment pattern) {
        String[] options = {
                getString(R.string.pref_pattern_action_edit),
                getString(R.string.pref_pattern_action_duplicate),
                getString(R.string.pref_pattern_action_delete)
        };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(pattern.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            launchPatternEditing(pattern.getId());
                            break;
                        case 1: // Duplicate
                            duplicatePattern(pattern);
                            break;
                        case 2: // Delete
                            confirmDeletePattern(pattern);
                            break;
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void duplicatePattern(@NonNull UserScheduleAssignment pattern) {
        // Load pattern for editing, then create new one
        mPatternService.loadUserPatternForEditing(pattern.getId())
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        UserSchedulePatternService.PatternEditingData editingData = result.getData();

                        // Create duplicate with new name
                        String newName = getString(R.string.pref_pattern_duplicate_name_format,
                                editingData.getPatternName());

                        mPatternService.createUserPattern(
                                        editingData.getPatternDays(),
                                        editingData.getStartDate().plusDays(1), // Start day after original
                                        newName)
                                .thenAccept(createResult -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            if (createResult.isSuccess()) {
                                                Log.d(TAG, "Pattern duplicated successfully");
                                                loadExistingPatterns(); // Refresh list
                                            } else {
                                                Log.e(TAG, "Failed to duplicate pattern: " + createResult.getErrorMessage());
                                            }
                                        });
                                    }
                                });
                    }
                });
    }

    private void confirmDeletePattern(@NonNull UserScheduleAssignment pattern) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_delete_pattern_dialog_title)
                .setMessage(getString(R.string.pref_delete_pattern_dialog_message, pattern.getDisplayTitle()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deletePattern(pattern))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deletePattern(@NonNull UserScheduleAssignment pattern) {
        mPatternService.deleteUserPattern(pattern.getId())
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                Log.d(TAG, "Pattern deleted successfully: " + pattern.getId());
                                loadExistingPatterns(); // Refresh list
                            } else {
                                Log.e(TAG, "Failed to delete pattern: " + result.getErrorMessage());
                            }
                        });
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_PATTERN:
                case REQUEST_EDIT_PATTERN:
                    Log.d(TAG, "Pattern activity completed successfully, refreshing list");
                    loadExistingPatterns();
                    break;
            }
        }
    }
}