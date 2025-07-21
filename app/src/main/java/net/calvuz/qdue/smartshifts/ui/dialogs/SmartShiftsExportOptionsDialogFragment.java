package net.calvuz.qdue.smartshifts.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.DialogSmartshiftsExportOptionsBinding;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;

import java.util.List;

/**
 * Dialog for customizing SmartShifts export options
 * REFACTORED: Proper View Binding usage + Centralized Enums + Clean Architecture
 */
public class SmartShiftsExportOptionsDialogFragment extends DialogFragment {

    private static final String TAG = "SmartShiftsExportDialog";

    public interface ExportOptionsListener {
        void onExportOptionsSelected(SmartShiftsExportImportManager.SelectiveExportConfiguration config);
        void onExportCancelled();
    }

    private DialogSmartshiftsExportOptionsBinding binding;
    private ExportOptionsListener listener;

    public static SmartShiftsExportOptionsDialogFragment newInstance() {
        return new SmartShiftsExportOptionsDialogFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ExportOptionsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ExportOptionsListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogSmartshiftsExportOptionsBinding.inflate(getLayoutInflater());

        initializeViews();
        setupFormatSpinner();
        setDefaultValues();

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.smartshifts_export_options_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.action_export, null) // Set in onStart to prevent auto-dismiss
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                    if (listener != null) {
                        listener.onExportCancelled();
                    }
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateInput()) {
                    SmartShiftsExportImportManager.SelectiveExportConfiguration config = createExportConfiguration();
                    if (listener != null) {
                        listener.onExportOptionsSelected(config);
                    }
                    dismiss();
                }
            });
        }
    }

    /**
     * Initialize views from binding - NO MORE findViewById!
     * All view references go through binding
     */
    private void initializeViews() {
        // Views are now accessed via binding - no individual field declarations needed
        // binding.editPackageName, binding.editDescription, etc. are available directly

        // Any additional view setup can go here
        // For example, setting up listeners or initial states
    }

    /**
     * Setup format spinner using centralized enums
     */
    private void setupFormatSpinner() {
        List<SmartShiftsExportImportManager.ExportFormat> formats =
                SmartShiftsExportImportManager.getAvailableExportFormats();

        String[] formatDisplayNames = formats.stream()
                .map(SmartShiftsExportImportManager.ExportFormat::getName)
                .toArray(String[]::new);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                formatDisplayNames
        );

        binding.spinnerFormat.setAdapter(adapter);
        binding.spinnerFormat.setText(formatDisplayNames[0], false); // Default to first format
    }

    /**
     * Set default values using binding
     */
    private void setDefaultValues() {
        // Package name with timestamp
        String defaultName = getString(R.string.smartshifts_export_default_name) +
                "_" + System.currentTimeMillis();
        binding.editPackageName.setText(defaultName);

        // Default description
        binding.editDescription.setText(getString(R.string.smartshifts_export_default_description));

        // Default data selection - privacy-conscious defaults
        binding.checkboxIncludeShiftTypes.setChecked(true);
        binding.checkboxIncludePatterns.setChecked(true);
        binding.checkboxIncludeAssignments.setChecked(true);
        binding.checkboxIncludeEvents.setChecked(true);
        binding.checkboxIncludeContacts.setChecked(false); // Privacy default
        binding.checkboxCompressOutput.setChecked(false);

        // Default date range to last 3 months
        binding.radioGroupDateRange.check(R.id.radio_last_3_months);
    }

    /**
     * Validate input using binding
     */
    private boolean validateInput() {
        // Validate package name
        String packageName = binding.editPackageName.getText().toString().trim();
        if (TextUtils.isEmpty(packageName)) {
            binding.editPackageName.setError(getString(R.string.error_package_name_required));
            binding.editPackageName.requestFocus();
            return false;
        }

        // Validate at least one data type is selected
        if (!isAnyDataTypeSelected()) {
            showNoDataSelectedError();
            return false;
        }

        return true;
    }

    /**
     * Check if any data type is selected - cleaner logic
     */
    private boolean isAnyDataTypeSelected() {
        return binding.checkboxIncludeShiftTypes.isChecked() ||
                binding.checkboxIncludePatterns.isChecked() ||
                binding.checkboxIncludeAssignments.isChecked() ||
                binding.checkboxIncludeEvents.isChecked() ||
                binding.checkboxIncludeContacts.isChecked();
    }

    /**
     * Show error dialog for no data selection
     */
    private void showNoDataSelectedError() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_no_data_selected)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    /**
     * Create export configuration using centralized enums
     */
    private SmartShiftsExportImportManager.SelectiveExportConfiguration createExportConfiguration() {
        SmartShiftsExportImportManager.SelectiveExportConfiguration config =
                new SmartShiftsExportImportManager.SelectiveExportConfiguration();

        // Set format using centralized enum logic
        SmartShiftsExportImportManager.ExportFormat selectedFormat = getSelectedFormat();
        config.setFormat(selectedFormat.getId());
        config.setExportType(SmartShiftsExportImportManager.EXPORT_TYPE_CUSTOM);

        // Set date range using centralized enum
        SmartShiftsExportImportManager.ExportDateRange dateRange = getSelectedDateRange();
        config.setStartDate(dateRange.getStartDate());
        config.setEndDate(dateRange.getEndDate());

        // Data inclusion flags using binding
        config.setIncludeShiftTypes(binding.checkboxIncludeShiftTypes.isChecked());
        config.setIncludePatterns(binding.checkboxIncludePatterns.isChecked());
        config.setIncludeAssignments(binding.checkboxIncludeAssignments.isChecked());
        config.setIncludeEvents(binding.checkboxIncludeEvents.isChecked());
        config.setIncludeContacts(binding.checkboxIncludeContacts.isChecked());

        return config;
    }

    /**
     * Get selected format using centralized enum logic
     */
    private SmartShiftsExportImportManager.ExportFormat getSelectedFormat() {
        String selectedFormatName = binding.spinnerFormat.getText().toString();

        List<SmartShiftsExportImportManager.ExportFormat> formats =
                SmartShiftsExportImportManager.getAvailableExportFormats();

        return formats.stream()
                .filter(format -> format.getName().equals(selectedFormatName))
                .findFirst()
                .orElse(SmartShiftsExportImportManager.ExportFormat.JSON); // Safe default
    }

    /**
     * Get selected date range using centralized enum
     */
    private SmartShiftsExportImportManager.ExportDateRange getSelectedDateRange() {
        int selectedRadioId = binding.radioGroupDateRange.getCheckedRadioButtonId();

        if (selectedRadioId == R.id.radio_all_data) {
            return SmartShiftsExportImportManager.ExportDateRange.ALL;
        } else if (selectedRadioId == R.id.radio_last_month) {
            return SmartShiftsExportImportManager.ExportDateRange.LAST_MONTH;
        } else if (selectedRadioId == R.id.radio_last_3_months) {
            return SmartShiftsExportImportManager.ExportDateRange.LAST_3_MONTHS;
        } else if (selectedRadioId == R.id.radio_last_6_months) {
            return SmartShiftsExportImportManager.ExportDateRange.LAST_6_MONTHS;
        } else if (selectedRadioId == R.id.radio_last_year) {
            return SmartShiftsExportImportManager.ExportDateRange.LAST_YEAR;
        } else if (selectedRadioId == R.id.radio_current_year) {
            return SmartShiftsExportImportManager.ExportDateRange.CURRENT_YEAR;
        }

        return SmartShiftsExportImportManager.ExportDateRange.LAST_3_MONTHS; // Safe default
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}