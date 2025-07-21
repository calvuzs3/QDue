package net.calvuz.qdue.smartshifts.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.DialogSmartshiftsImportOptionsBinding;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;

import java.io.File;

/**
 * Dialog for configuring SmartShifts import options
 * FIXED: Now uses correct data structures from SmartShiftsExportImportManager
 */
public class SmartShiftsImportOptionsDialogFragment extends DialogFragment {

    private static final String TAG = "SmartShiftsImportDialog";

    private static final String ARG_FILE_URI = "smartshifts_file_uri";
    private static final String ARG_FILE_NAME = "smartshifts_file_name";

    public interface ImportOptionsListener {
        void onImportOptionsSelected(File sourceFile, SmartShiftsExportImportManager.ImportConfiguration config);
        void onImportCancelled();
    }

    private DialogSmartshiftsImportOptionsBinding binding;
    private ImportOptionsListener listener;
    private Uri fileUri;
    private String fileName;

    // UI Components
    private TextView textFileName;
    private CheckBox checkboxValidateBeforeImport;
    private CheckBox checkboxShowProgress;
    private CheckBox checkboxBackupBeforeImport;
    private RadioGroup radioGroupConflictResolution;
    private CheckBox checkboxPreserveExistingAssignments;
    private CheckBox checkboxUpdateExistingPatterns;
    private CheckBox checkboxAllowPartialImport;

    public static SmartShiftsImportOptionsDialogFragment newInstance(Uri fileUri, String fileName) {
        SmartShiftsImportOptionsDialogFragment fragment = new SmartShiftsImportOptionsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE_URI, fileUri);
        args.putString(ARG_FILE_NAME, fileName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fileUri = getArguments().getParcelable(ARG_FILE_URI);
            fileName = getArguments().getString(ARG_FILE_NAME);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ImportOptionsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ImportOptionsListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogSmartshiftsImportOptionsBinding.inflate(getLayoutInflater());


        initializeViews();
        setDefaultValues();
        updateFileInfo();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.smartshifts_import_options_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.action_import, null) // Set in onStart to prevent auto-dismiss
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                    if (listener != null) {
                        listener.onImportCancelled();
                    }
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                SmartShiftsExportImportManager.ImportConfiguration config = createImportConfiguration();
                if (listener != null) {
                    // Convert URI to File - this might need to be handled differently based on the URI scheme
                    File sourceFile = convertUriToFile(fileUri);
                    listener.onImportOptionsSelected(sourceFile, config);
                }
                dismiss();
            });
        }
    }

    private void initializeViews() {
        textFileName = binding.textFileName;
        checkboxValidateBeforeImport = binding.checkboxValidateBeforeImport;
        checkboxShowProgress = binding.checkboxShowProgress;
        checkboxBackupBeforeImport = binding.checkboxBackupBeforeImport;
        radioGroupConflictResolution = binding.radioGroupConflictResolution;
        checkboxPreserveExistingAssignments = binding.checkboxPreserveExistingAssignments;
        checkboxUpdateExistingPatterns = binding.checkboxUpdateExistingPatterns;
        checkboxAllowPartialImport = binding.checkboxAllowPartialImport;
    }

    private void setDefaultValues() {
        // Default import behavior
        checkboxValidateBeforeImport.setChecked(true);
        checkboxShowProgress.setChecked(true);
        checkboxBackupBeforeImport.setChecked(true);
        checkboxPreserveExistingAssignments.setChecked(true);
        checkboxUpdateExistingPatterns.setChecked(false);
        checkboxAllowPartialImport.setChecked(true);

        // Default conflict resolution to merge (as used in manager)
        radioGroupConflictResolution.check(R.id.radio_merge_data);
    }

    private void updateFileInfo() {
        if (fileName != null && textFileName != null) {
            textFileName.setText(getString(R.string.smartshifts_import_file_info, fileName));
        }
    }

    private SmartShiftsExportImportManager.ImportConfiguration createImportConfiguration() {
        File sourceFile = convertUriToFile(fileUri);
        SmartShiftsExportImportManager.ImportConfiguration config =
                new SmartShiftsExportImportManager.ImportConfiguration(sourceFile);

        config.setStrictValidation(checkboxValidateBeforeImport.isChecked());

        // Use centralized conflict resolution enum
        SmartShiftsExportImportManager.ConflictResolutionStrategy strategy = getSelectedConflictStrategy();
        config.setConflictStrategy(strategy.getValue());

        return config;
    }

    private SmartShiftsExportImportManager.ConflictResolutionStrategy getSelectedConflictStrategy() {
        int selectedId = radioGroupConflictResolution.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_skip_conflicts) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.SKIP;
        } else if (selectedId == R.id.radio_replace_existing) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.REPLACE;
        } else if (selectedId == R.id.radio_merge_data) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.MERGE;
        }

        return SmartShiftsExportImportManager.ConflictResolutionStrategy.SKIP; // Default
    }

    /**
     * Convert URI to File - this is a simplified implementation
     * In a real app, you might need more sophisticated URI handling
     */
    private File convertUriToFile(Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else {
            // For content:// URIs, you might need to copy to a temporary file
            // This is a placeholder - implement based on your app's needs
            return new File(uri.toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}