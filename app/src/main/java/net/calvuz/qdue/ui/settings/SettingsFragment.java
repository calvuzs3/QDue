package net.calvuz.qdue.ui.settings;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Settings Fragment with Scheme Date Preference handling
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "SettingsFragment";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Preference schemeDatePreference;
    private LocalDate currentSchemeDate;
    private LocalDate backupSchemeDate;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Initialize scheme date preference
        initializeSchemeDatePreference();
    }

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

            Log.v(TAG, "Scheme date preference initialized");
        }
    }

    /**
     * Load current scheme date from preferences
     */
    private void loadCurrentSchemeDate() {
        Context context = getContext();
        if (context == null) return;

        currentSchemeDate = Preferences.getSchemeStartDate(context);
        backupSchemeDate = currentSchemeDate; // Backup for rollback

        Log.v(TAG, "Loaded scheme date: " + currentSchemeDate);
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
                .setIcon(android.R.drawable.ic_dialog_alert)
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
            Log.v(TAG, "Applying scheme date change: " + currentSchemeDate + " -> " + newDate);

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
                Log.v(TAG, "Scheme date successfully updated");
            } else {
                // Error during regeneration
                throw new Exception("Schema regeneration failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating scheme date: " + e.getMessage(), e);
            rollbackDate();
            Toast.makeText(context, R.string.scheme_date_error, Toast.LENGTH_LONG).show();
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

            Toast.makeText(context, R.string.scheme_date_rollback, Toast.LENGTH_LONG).show();

        } catch (Exception rollbackError) {
            Log.e(TAG, "Critical rollback error: " + rollbackError.getMessage(), rollbackError);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh scheme date display
        loadCurrentSchemeDate();
        updateSchemeDateSummary();

        // Update QuattroDue
        Context context = getContext();
        if (context != null) {
            QuattroDue.getInstance(context).updatePreferences(context);
        }
    }
}