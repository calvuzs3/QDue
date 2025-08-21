package net.calvuz.qdue.ui.features.settings.presentation;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Settings Fragment with Scheme Date Preference handling
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== FIELDS ====================

    private Preference schemeDatePreference;
    private Preference customPatternsPreference;
    private LocalDate currentSchemeDate;
    private LocalDate backupSchemeDate;

    // ==================== PREFERENCE NAVIGATION ====================

    /**
     * Interface for communicating navigation requests to the host activity
     */
    public interface SettingsNavigationCallback {
        /**
         * Navigate to a specific fragment from settings
         */
        void navigateToFragment(@NonNull Fragment fragment, @NonNull String title);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from XML
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Initialize preferences
        initializeSchemeDatePreference();
        initializeCustomPatternsPreference();

        // Setup view mode preference
        setupViewModePreference();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register as listener for preference changes
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                .registerOnSharedPreferenceChangeListener(this);

        // Refresh scheme date display
        loadCurrentSchemeDate();
        updateSchemeDateSummary();

        // Update QuattroDue
        Context context = getContext();
        if (context != null) {
            QuattroDue.getInstance(context).updatePreferences(context);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister listener to prevent memory leaks
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    // ==================== PREFERENCE INITIALIZATION ====================

    /**
     * Initialize scheme date preference with click listener
     */
    private void initializeSchemeDatePreference() {
        schemeDatePreference = findPreference(getString(R.string.qd_preference_scheme_start_date));

        if (schemeDatePreference != null) {
            // Load current date
            loadCurrentSchemeDate();

            // Set click listener
            schemeDatePreference.setOnPreferenceClickListener(preference -> {
                showSchemeDateWarning();
                return true;
            });

            // Update summary
            updateSchemeDateSummary();
        }
    }

    /**
     * Initialize custom patterns preference with navigation
     */
    private void initializeCustomPatternsPreference() {
        customPatternsPreference = findPreference(getString(R.string.qd_preference_pattern_custom));

        if (customPatternsPreference != null) {
            customPatternsPreference.setOnPreferenceClickListener(preference -> {
                navigateToCustomPatterns();
                return true;
            });
        } else {
            Log.w(TAG, "Custom patterns preference not found in root_preferences.xml");
        }
    }

    // ==================== NAVIGATION METHODS ====================

    /**
     * Navigate to Custom Patterns management screen
     */
    private void navigateToCustomPatterns() {
        try {
            if (getActivity() instanceof SettingsNavigationCallback) {
                CustomPatternPreferencesFragment customPatternFragment = new CustomPatternPreferencesFragment();

                String title = getString(R.string.pref_custom_patterns_title);
                ((SettingsNavigationCallback) getActivity()).navigateToFragment(customPatternFragment, title);

                Log.d(TAG, "Navigating to CustomPatternPreferencesFragment");
            } else {
                Log.e(TAG, "Activity does not implement SettingsNavigationCallback");
                showNavigationError();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to custom patterns", e);
            showNavigationError();
        }
    }

    /**
     * Show navigation error to user
     */
    private void showNavigationError() {
        if (getContext() != null) {
            Toast.makeText(getContext(),
                    getString(R.string.error_navigation_custom_patterns),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== SCHEME DATE HANDLING ====================

    /**
     * Load current scheme date from preferences
     */
    private void loadCurrentSchemeDate() {
        Context context = getContext();
        if (context == null) return;

        currentSchemeDate = Preferences.getSchemeStartDate(context);
        backupSchemeDate = currentSchemeDate; // Backup for rollback
    }

    /**
     * Update preference summary with current date
     */
    private void updateSchemeDateSummary() {
        if (schemeDatePreference != null && currentSchemeDate != null) {
            String formattedDate = currentSchemeDate.format(DISPLAY_FORMATTER);
            String summary = getString(R.string.settings_scheme_date_current, formattedDate);
            schemeDatePreference.setSummary(summary);
        }
    }

    /**
     * Step 1: Show warning dialog
     */
    private void showSchemeDateWarning() {
        Context context = getContext();
        if (context == null) return;

        String currentFormatted = currentSchemeDate.format(DISPLAY_FORMATTER);
        String currentLabel = getString(R.string.scheme_date_current_label, currentFormatted);
        String message = getString(R.string.scheme_date_warning_message) + "\n\n" + currentLabel;

        new AlertDialog.Builder(context)
                .setTitle(R.string.scheme_date_warning_title)
                .setMessage(message)
                .setPositiveButton(R.string.scheme_date_proceed, (dialog, which) -> {
                    showDatePicker();
                })
                .setNegativeButton(R.string.scheme_date_cancel, null)
                .setIcon(R.drawable.ic_rounded_warning_24)
                .show();
    }

    /**
     * Step 2: Show date picker
     */
    private void showDatePicker() {
        Context context = getContext();
        if (context == null) return;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                this::onDateSelected,
                currentSchemeDate.getYear(),
                currentSchemeDate.getMonthValue() - 1, // Calendar months are 0-based
                currentSchemeDate.getDayOfMonth()
        );

        datePickerDialog.setTitle(getString(R.string.settings_scheme_date_title));
        datePickerDialog.show();
    }

    /**
     * Date picker callback
     */
    private void onDateSelected(DatePicker view, int year, int month, int dayOfMonth) {
        LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);

        // Check if date changed
        if (selectedDate.equals(currentSchemeDate)) {
            Log.v(TAG, "Date unchanged");
            return;
        }

        // Step 3: Show confirmation
        showConfirmationDialog(selectedDate);
    }

    /**
     * Step 3: Confirmation dialog
     */
    private void showConfirmationDialog(LocalDate newDate) {
        Context context = getContext();
        if (context == null) return;

        String formattedDate = newDate.format(DISPLAY_FORMATTER);
        String message = getString(R.string.scheme_date_confirm_message, formattedDate);

        new AlertDialog.Builder(context)
                .setTitle(R.string.scheme_date_confirm_title)
                .setMessage(message)
                .setPositiveButton(R.string.scheme_date_confirm_yes, (dialog, which) -> {
                    applyDateChange(newDate);
                })
                .setNegativeButton(R.string.scheme_date_confirm_no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Step 4: Apply date change
     */
    private void applyDateChange(LocalDate newDate) {
        Context context = getContext();
        if (context == null) return;

        try {
            Log.d(TAG, "Applying scheme date change: " + currentSchemeDate + " -> " + newDate);

            // Save to preferences
            Preferences.setSchemeStartDate(context, newDate);

            // Update current date
            currentSchemeDate = newDate;

            // Regenerate schema
            QuattroDue quattroDue = QuattroDue.getInstance(context);
            if (quattroDue.regenerateSchemeWithNewDate(context)) {
                // Success
                updateSchemeDateSummary();
                Toast.makeText(context, R.string.scheme_date_success, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Scheme date successfully updated");
            } else {
                // Error during regeneration
                throw new Exception("Schema regeneration failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating scheme date: " + e.getMessage(), e);
            rollbackDate();
            Library.showError(context, R.string.scheme_date_error, Toast.LENGTH_LONG);
        }
    }

    /**
     * Rollback on error
     */
    private void rollbackDate() {
        Context context = getContext();
        if (context == null) return;

        try {
            Log.w(TAG, "Rolling back to: " + backupSchemeDate);

            Preferences.setSchemeStartDate(context, backupSchemeDate);
            currentSchemeDate = backupSchemeDate;
            updateSchemeDateSummary();

            Library.showSuccess(context, R.string.scheme_date_rollback, Toast.LENGTH_LONG);

        } catch (Exception rollbackError) {
            Log.e(TAG, "Critical rollback error: " + rollbackError.getMessage(), rollbackError);
        }
    }

    /**
     * Setup view mode preference with current value
     * This ensures the preference shows the correct current selection
     */
    private void setupViewModePreference() {
        ListPreference viewModePref = findPreference(QDue.Settings.QD_KEY_VIEW_MODE);
        if (viewModePref != null) {
            // Set current value from QDuePreferences
            String currentViewMode = QDuePreferences.getDefaultViewMode(requireContext());
            viewModePref.setValue(currentViewMode);
        } else {
            Log.w(TAG, "View mode preference not found in XML - check your root_preferences.xml");
        }
    }

    /**
     * Handle preference changes
     * This method is called when any preference value changes
     *
     * @param sharedPreferences The SharedPreferences that received the change
     * @param key               The key of the preference that was changed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference changed: " + key);

        if (QDue.Settings.QD_KEY_VIEW_MODE.equals(key)) {
            handleViewModePreferenceChange(sharedPreferences, key);
        } else if (QDue.Settings.QD_KEY_DYNAMIC_COLORS.equals(key)) {
            handleDynamicColorsPreferenceChange(sharedPreferences, key);
        } else {
            Log.d(TAG, "Unhandled preference change: " + key);
        }
    }

    /**
     * Handle view mode preference change
     * Updates QDuePreferences and notifies MainActivity
     */
    private void handleViewModePreferenceChange(SharedPreferences sharedPreferences, String key) {
        String newViewMode = sharedPreferences.getString(key, QDue.Settings.VIEW_MODE_CALENDAR);

        Log.d(TAG, "* View mode preference changed: " + newViewMode);

        try {
            // Save using QDuePreferences for consistency
            QDuePreferences.setDefaultViewMode(requireContext(), newViewMode);

            // Notify MainActivity if available
            if (getActivity() instanceof QDueMainActivity) {
                ((QDueMainActivity) getActivity()).onViewModeChanged(newViewMode);
                Log.d(TAG, "Notified MainActivity of view mode change");
            } else {
                Log.w(TAG, "Parent activity is not QDueMainActivity - cannot notify of view mode change");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling view mode preference change", e);
        }
    }

    /**
     * Handle dynamic colors preference change
     * Updates QDuePreferences for consistency
     */
    private void handleDynamicColorsPreferenceChange(SharedPreferences sharedPreferences, String key) {
        boolean dynamicColorsEnabled = sharedPreferences.getBoolean(key, true);

        Log.d(TAG, "Dynamic colors preference changed to: " + dynamicColorsEnabled);

        try {
            // Save using QDuePreferences for consistency
            QDuePreferences.setDynamicColorsEnabled(requireContext(), dynamicColorsEnabled);

            // Note: Dynamic colors change requires app restart to take full effect
            if (dynamicColorsEnabled) {
                Library.showSuccess(requireContext(),
                        R.string.text_settings_reboot_app_to_apply_dynamic_colors,
                        Toast.LENGTH_LONG);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling dynamic colors preference change", e);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Refresh all preference values from storage
     * Called when returning from sub-screens that might have changed values
     */
    public void refreshPreferenceValues() {
        try {
            // Refresh view mode
            ListPreference viewModePref = findPreference(QDue.Settings.QD_KEY_VIEW_MODE);
            if (viewModePref != null) {
                String currentViewMode = QDuePreferences.getDefaultViewMode(requireContext());
                viewModePref.setValue(currentViewMode);
                Log.d(TAG, "Refreshed view mode preference to: " + currentViewMode);
            }

            // Refresh scheme date
            //loadCurrentSchemeDate();
            //updateSchemeDateSummary();

            // Refresh other preferences as needed...

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing preference values", e);
        }
    }

    /**
     * Get display name for view mode
     * Converts internal view mode constants to user-friendly names
     *
     * @param viewMode The view mode constant
     * @return User-friendly display name
     */
    public String getViewModeDisplayName(String viewMode) {
        if (QDue.Settings.VIEW_MODE_DAYSLIST.equals(viewMode)) {
            return getString(R.string.menu_dayslist);
        } else {
            return getString(R.string.menu_calendar);
        }
    }





    // ================================== DEBUG ==================================

    /**
     * Debug method to log all current preferences
     * Only logs if debugging is enabled
     */
    private void debugLogAllPreferences() {
        if (!QDue.Debug.DEBUG_ACTIVITY) return;

        Log.d(TAG, "=== SettingsFragment Preferences Debug ===");

        try {
            QDuePreferences.logAllPreferences(requireContext());

            // Also log the raw SharedPreferences values
            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            Log.d(TAG, "Raw SharedPreferences:");
            Log.d(TAG, " - View Mode: " + prefs.getString(QDue.Settings.QD_KEY_VIEW_MODE, "NOT_SET"));
            Log.d(TAG, " - Dynamic Colors: " + prefs.getBoolean(QDue.Settings.QD_KEY_DYNAMIC_COLORS, false));

        } catch (Exception e) {
            Log.e(TAG, "Error logging preferences debug info", e);
        }

        Log.d(TAG, "=== End Preferences Debug ===");
    }

}