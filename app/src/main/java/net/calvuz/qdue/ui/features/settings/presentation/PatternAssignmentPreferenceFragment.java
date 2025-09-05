package net.calvuz.qdue.ui.features.settings.presentation;

import static net.calvuz.qdue.QDue.REQUEST_CHANGE_ASSIGNMENT;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.ui.features.assignment.wizard.PatternAssignmentWizardActivity;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;

/**
 * PatternAssignmentPreferenceFragment - Manage pattern assignments from settings
 */
public class PatternAssignmentPreferenceFragment extends PreferenceFragmentCompat {

    private static final String TAG = "PatternAssignmentPreferenceFragment";

    private UserScheduleAssignmentRepository mAssignmentRepository;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pattern_assignment_preferences, rootKey);

        // Initialize repository
        // TODO refactor
        ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(requireContext());
        CalendarServiceProvider provider = serviceProvider.getCalendarService().getCalendarServiceProvider();
        mAssignmentRepository = provider.getUserScheduleAssignmentRepository();

        setupPreferences();
        loadCurrentAssignment();
    }

    private void setupPreferences() {
        // Change Assignment Preference
        Preference changeAssignmentPref = findPreference("change_assignment");
        if (changeAssignmentPref != null) {
            changeAssignmentPref.setOnPreferenceClickListener(preference -> {
                launchChangeAssignmentWizard();
                return true;
            });
        }

        // View Assignment History
        Preference historyPref = findPreference("assignment_history");
        if (historyPref != null) {
            historyPref.setOnPreferenceClickListener(preference -> {
                // Launch assignment history activity
                // TODO: Implement assignment history view
                return true;
            });
        }
    }

    private void loadCurrentAssignment() {
        String userId = QDuePreferences.getUserId(requireContext());
        LocalDate today = LocalDate.now();

        mAssignmentRepository.getActiveAssignmentForUser(userId, today)
                .thenAccept(result -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateCurrentAssignmentDisplay(result));
                    }
                });
    }

    private void updateCurrentAssignmentDisplay(@NonNull OperationResult<UserScheduleAssignment> result) {
        Preference currentAssignmentPref = findPreference("current_assignment");
        if (currentAssignmentPref == null) return;

        if (result.isSuccess() && result.getData() != null) {
            UserScheduleAssignment assignment = result.getData();
            String title = "Current Assignment: " + assignment.getDisplayTitle();
            String summary = assignment.getDisplayPeriod() + "\n" + assignment.getStatusDisplayName();

            currentAssignmentPref.setTitle(title);
            currentAssignmentPref.setSummary(summary);
            currentAssignmentPref.setEnabled(true);
        } else {
            currentAssignmentPref.setTitle("No Active Assignment");
            currentAssignmentPref.setSummary("Tap to create your first assignment");
            currentAssignmentPref.setEnabled(true);

            // Make it clickable to start wizard
            currentAssignmentPref.setOnPreferenceClickListener(preference -> {
                launchFirstAssignmentWizard();
                return true;
            });
        }
    }

    private void launchChangeAssignmentWizard() {
        Intent intent = PatternAssignmentWizardActivity.createIntent(requireContext(), false);
        startActivityForResult(intent, REQUEST_CHANGE_ASSIGNMENT);
    }

    private void launchFirstAssignmentWizard() {
        Intent intent = PatternAssignmentWizardActivity.createIntent(requireContext(), true);
        startActivityForResult(intent, REQUEST_CHANGE_ASSIGNMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHANGE_ASSIGNMENT && resultCode == FragmentActivity.RESULT_OK) {
            // Assignment was created/changed - refresh display
            loadCurrentAssignment();
            Log.d(TAG, "Assignment updated from settings");
        }
    }
}
