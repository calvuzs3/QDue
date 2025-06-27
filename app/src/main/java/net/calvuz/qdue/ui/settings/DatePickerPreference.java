package net.calvuz.qdue.ui.settings;


import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.Costants;
import net.calvuz.qdue.quattrodue.Preferences;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * Custom DatePicker Preference for Scheme Start Date
 *
 * Handles the complex workflow:
 * 1. Warning dialog about impact
 * 2. Date picker
 * 3. Confirmation dialog
 * 4. Schema regeneration with rollback support
 */
public class DatePickerPreference extends Preference {

    private static final String TAG = "DatePickerPreference";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Current values
    private LocalDate currentDate;
    private LocalDate backupDate; // For rollback

    public DatePickerPreference(Context context) {
        super(context);
        init();
    }

    public DatePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DatePickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialize preference with current values
     */
    private void init() {
        // Load current date from preferences
        loadCurrentDate();

        // Update summary display
        updateSummary();
    }

    /**
     * Load current scheme start date from preferences
     */
    private void loadCurrentDate() {
        Context context = getContext();

        int day = Preferences.getSharedPreference(context,
                Preferences.KEY_SCHEME_START_DAY, Costants.QD_SCHEME_START_DAY);
        int month = Preferences.getSharedPreference(context,
                Preferences.KEY_SCHEME_START_MONTH, Costants.QD_SCHEME_START_MONTH);
        int year = Preferences.getSharedPreference(context,
                Preferences.KEY_SCHEME_START_YEAR, Costants.QD_SCHEME_START_YEAR);

        currentDate = LocalDate.of(year, month, day);
        backupDate = currentDate; // Store for potential rollback

        Log.v(TAG, "Loaded current scheme date: " + currentDate);
    }

    /**
     * Update preference summary with current date
     */
    private void updateSummary() {
        String formattedDate = currentDate.format(DISPLAY_FORMATTER);
        String summary = getContext().getString(R.string.settings_scheme_date_current, formattedDate);
        setSummary(summary);
    }

    @Override
    protected void onClick() {
        // Step 1: Show warning dialog
        showWarningDialog();
    }

    /**
     * Step 1: Warning dialog about scheme impact
     */
    private void showWarningDialog() {
        Context context = getContext();
        String currentFormatted = currentDate.format(DISPLAY_FORMATTER);
        String currentLabel = context.getString(R.string.scheme_date_current_label, currentFormatted);

        String message = context.getString(R.string.scheme_date_warning_message) + "\n\n" + currentLabel;

        new AlertDialog.Builder(context)
                .setTitle(R.string.scheme_date_warning_title)
                .setMessage(message)
                .setPositiveButton(R.string.scheme_date_proceed, (dialog, which) -> {
                    // Step 2: Show date picker
                    showDatePicker();
                })
                .setNegativeButton(R.string.scheme_date_cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Step 2: Show date picker dialog
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                this::onDateSelected,
                currentDate.getYear(),
                currentDate.getMonthValue() - 1, // Calendar months are 0-based
                currentDate.getDayOfMonth()
        );

        datePickerDialog.setTitle(getTitle());
        datePickerDialog.show();
    }

    /**
     * Date picker callback
     */
    private void onDateSelected(DatePicker view, int year, int month, int dayOfMonth) {
        LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth); // Calendar months are 0-based

        // Check if date actually changed
        if (selectedDate.equals(currentDate)) {
            Log.v(TAG, "Date unchanged, no action needed");
            return;
        }

        // Step 3: Show confirmation dialog
        showConfirmationDialog(selectedDate);
    }

    /**
     * Step 3: Confirmation dialog before applying changes
     */
    private void showConfirmationDialog(LocalDate newDate) {
        Context context = getContext();
        String formattedDate = newDate.format(DISPLAY_FORMATTER);
        String message = context.getString(R.string.scheme_date_confirm_message, formattedDate);

        new AlertDialog.Builder(context)
                .setTitle(R.string.scheme_date_confirm_title)
                .setMessage(message)
                .setPositiveButton(R.string.scheme_date_confirm_yes, (dialog, which) -> {
                    // Step 4: Apply changes
                    applyDateChange(newDate);
                })
                .setNegativeButton(R.string.scheme_date_confirm_no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Step 4: Apply date change and regenerate schema
     */
    private void applyDateChange(LocalDate newDate) {
        Context context = getContext();

        try {
            Log.v(TAG, "Applying scheme date change from " + currentDate + " to " + newDate);

            // Save new date to preferences
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_DAY, newDate.getDayOfMonth());
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_MONTH, newDate.getMonthValue());
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_YEAR, newDate.getYear());

            // Update current date
            currentDate = newDate;

            // Regenerate schema immediately
            QuattroDue quattroDue = QuattroDue.getInstance(context);
            quattroDue.setRefresh(true); // Force refresh

            // Update UI
            updateSummary();

            // Show success message
            Toast.makeText(context, R.string.scheme_date_success, Toast.LENGTH_SHORT).show();

            Log.v(TAG, "Scheme date successfully updated to " + newDate);

        } catch (Exception e) {
            Log.e(TAG, "Error updating scheme date: " + e.getMessage(), e);

            // Rollback on error
            rollbackDate();

            // Show error message
            Toast.makeText(context, R.string.scheme_date_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Rollback to previous date in case of error
     */
    private void rollbackDate() {
        Context context = getContext();

        try {
            Log.w(TAG, "Rolling back scheme date to " + backupDate);

            // Restore backup date
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_DAY, backupDate.getDayOfMonth());
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_MONTH, backupDate.getMonthValue());
            Preferences.setSharedPreference(context, Preferences.KEY_SCHEME_START_YEAR, backupDate.getYear());

            // Update current date
            currentDate = backupDate;

            // Update UI
            updateSummary();

            // Show rollback message
            Toast.makeText(context, R.string.scheme_date_rollback, Toast.LENGTH_LONG).show();

        } catch (Exception rollbackError) {
            Log.e(TAG, "Critical error during rollback: " + rollbackError.getMessage(), rollbackError);
        }
    }

    /**
     * Update backup date when preference is successfully loaded
     */
    public void updateBackupDate() {
        loadCurrentDate();
        backupDate = currentDate;
        Log.v(TAG, "Updated backup date to " + backupDate);
    }
}