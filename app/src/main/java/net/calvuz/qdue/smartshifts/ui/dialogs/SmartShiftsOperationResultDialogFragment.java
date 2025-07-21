package net.calvuz.qdue.smartshifts.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;

import java.util.Locale;

/**
 * Dialog showing results of SmartShifts export/import operations
 * FIXED: Now uses correct result classes from SmartShiftsExportImportManager
 */
public class SmartShiftsOperationResultDialogFragment extends DialogFragment {

    private static final String TAG = "SmartShiftsResultDialog";
    private static final String ARG_RESULT_TYPE = "result_type";
    private static final String ARG_OPERATION_TYPE = "operation_type";

    public enum OperationType {
        EXPORT, IMPORT, BACKUP, RESTORE
    }

    public enum ResultType {
        EXPORT_RESULT, IMPORT_RESULT, BACKUP_RESULT, SYNC_RESULT
    }

    public interface ResultActionListener {
        void onShareResult(String filePath);

        void onOpenFile(String filePath);

        void onRetryOperation();
    }

    private ResultActionListener listener;
    private Object result; // Can be ExportResult, ImportResult, BackupResult, or SyncResult
    private OperationType operationType;
    private ResultType resultType;

    // UI Components
    private ImageView imageStatus;
    private TextView textTitle;
    private TextView textMessage;
    private TextView textDetails;
    private MaterialButton buttonPrimary;
    private MaterialButton buttonSecondary;
    private MaterialButton buttonClose;

    public static SmartShiftsOperationResultDialogFragment newInstanceForExport(
            SmartShiftsExportImportManager.ExportResult result) {
        SmartShiftsOperationResultDialogFragment fragment =
                new SmartShiftsOperationResultDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION_TYPE, OperationType.EXPORT);
        args.putSerializable(ARG_RESULT_TYPE, ResultType.EXPORT_RESULT);
        fragment.setArguments(args);
        fragment.result = result;
        return fragment;
    }

    public static SmartShiftsOperationResultDialogFragment newInstanceForImport(
            SmartShiftsExportImportManager.ImportResult result) {
        SmartShiftsOperationResultDialogFragment fragment =
                new SmartShiftsOperationResultDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION_TYPE, OperationType.IMPORT);
        args.putSerializable(ARG_RESULT_TYPE, ResultType.IMPORT_RESULT);
        fragment.setArguments(args);
        fragment.result = result;
        return fragment;
    }

    public static SmartShiftsOperationResultDialogFragment newInstanceForBackup(
            SmartShiftsExportImportManager.BackupResult result) {
        SmartShiftsOperationResultDialogFragment fragment =
                new SmartShiftsOperationResultDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION_TYPE, OperationType.BACKUP);
        args.putSerializable(ARG_RESULT_TYPE, ResultType.BACKUP_RESULT);
        fragment.setArguments(args);
        fragment.result = result;
        return fragment;
    }

    public static SmartShiftsOperationResultDialogFragment newInstanceForSync(
            SmartShiftsExportImportManager.SyncResult result) {
        SmartShiftsOperationResultDialogFragment fragment =
                new SmartShiftsOperationResultDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION_TYPE, OperationType.RESTORE); // Using RESTORE for sync
        args.putSerializable(ARG_RESULT_TYPE, ResultType.SYNC_RESULT);
        fragment.setArguments(args);
        fragment.result = result;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            operationType = (OperationType) getArguments().getSerializable(ARG_OPERATION_TYPE);
            resultType = (ResultType) getArguments().getSerializable(ARG_RESULT_TYPE);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ResultActionListener) context;
        } catch (ClassCastException e) {
            // Listener is optional
            listener = null;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_smartshifts_operation_result, null);

        initializeViews(dialogView);
        setupResultDisplay();
        setupActionButtons();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(dialogView);

        return builder.create();
    }

    private void initializeViews(View dialogView) {
        imageStatus = dialogView.findViewById(R.id.image_status);
        textTitle = dialogView.findViewById(R.id.text_title);
        textMessage = dialogView.findViewById(R.id.text_message);
        textDetails = dialogView.findViewById(R.id.text_details);
        buttonPrimary = dialogView.findViewById(R.id.button_primary);
        buttonSecondary = dialogView.findViewById(R.id.button_secondary);
        buttonClose = dialogView.findViewById(R.id.button_close);
    }

    private void setupResultDisplay() {
        if (result == null) return;

        boolean success = isResultSuccessful();
        String message = getResultMessage();
        String details = buildDetailsText();

        // Set status icon and colors
        if (success) {
            imageStatus.setImageResource(R.drawable.ic_rounded_check_circle_24);
            imageStatus.setColorFilter(getResources().getColor(R.color.smartshifts_success, null));
            textTitle.setText(getSuccessTitle());
            textTitle.setTextColor(getResources().getColor(R.color.smartshifts_success, null));
        } else {
            imageStatus.setImageResource(R.drawable.ic_rounded_error_24);
            imageStatus.setColorFilter(getResources().getColor(R.color.smartshifts_error, null));
            textTitle.setText(getErrorTitle());
            textTitle.setTextColor(getResources().getColor(R.color.smartshifts_error, null));
        }

        // Set message
        textMessage.setText(message != null ? message : getDefaultMessage());

        // Set details
        if (details != null && !details.isEmpty()) {
            textDetails.setText(details);
            textDetails.setVisibility(View.VISIBLE);
        } else {
            textDetails.setVisibility(View.GONE);
        }
    }

    private boolean isResultSuccessful() {
        if (result instanceof SmartShiftsExportImportManager.ExportResult) {
            return ((SmartShiftsExportImportManager.ExportResult) result).isSuccess();
        } else if (result instanceof SmartShiftsExportImportManager.ImportResult) {
            return ((SmartShiftsExportImportManager.ImportResult) result).isSuccess();
        } else if (result instanceof SmartShiftsExportImportManager.BackupResult) {
            return ((SmartShiftsExportImportManager.BackupResult) result).isSuccess();
        } else if (result instanceof SmartShiftsExportImportManager.SyncResult) {
            return ((SmartShiftsExportImportManager.SyncResult) result).isSuccess();
        }
        return false;
    }

    private String getResultMessage() {
        if (result instanceof SmartShiftsExportImportManager.ExportResult) {
            return ((SmartShiftsExportImportManager.ExportResult) result).getErrorMessage();
        } else if (result instanceof SmartShiftsExportImportManager.ImportResult) {
            return ((SmartShiftsExportImportManager.ImportResult) result).getErrorMessage();
        } else if (result instanceof SmartShiftsExportImportManager.BackupResult) {
            return ((SmartShiftsExportImportManager.BackupResult) result).getErrorMessage();
        } else if (result instanceof SmartShiftsExportImportManager.SyncResult) {
            return ((SmartShiftsExportImportManager.SyncResult) result).getErrorMessage();
        }
        return null;
    }

    private String getResultFilePath() {
        if (result instanceof SmartShiftsExportImportManager.ExportResult) {
            return ((SmartShiftsExportImportManager.ExportResult) result).getExportFile() != null ?
                    ((SmartShiftsExportImportManager.ExportResult) result).getExportFile().getAbsolutePath() : null;
        } else if (result instanceof SmartShiftsExportImportManager.BackupResult) {
            return ((SmartShiftsExportImportManager.BackupResult) result).getBackupFile() != null ?
                    ((SmartShiftsExportImportManager.BackupResult) result).getBackupFile().getAbsolutePath() : null;
        }
        return null;
    }

    private String getSuccessTitle() {
        switch (operationType) {
            case EXPORT:
                return getString(R.string.smartshifts_export_success_title);
            case IMPORT:
                return getString(R.string.smartshifts_import_success_title);
            case BACKUP:
                return getString(R.string.smartshifts_backup_success_title);
            case RESTORE:
                return getString(R.string.smartshifts_restore_success_title);
            default:
                return getString(R.string.smartshifts_operation_success_title);
        }
    }

    private String getErrorTitle() {
        switch (operationType) {
            case EXPORT:
                return getString(R.string.smartshifts_export_error_title);
            case IMPORT:
                return getString(R.string.smartshifts_import_error_title);
            case BACKUP:
                return getString(R.string.smartshifts_backup_error_title);
            case RESTORE:
                return getString(R.string.smartshifts_restore_error_title);
            default:
                return getString(R.string.smartshifts_operation_error_title);
        }
    }

    private String getDefaultMessage() {
        boolean success = isResultSuccessful();
        if (success) {
            switch (operationType) {
                case EXPORT:
                    return getString(R.string.smartshifts_export_success_message);
                case IMPORT:
                    return getString(R.string.smartshifts_import_success_message);
                case BACKUP:
                    return getString(R.string.smartshifts_backup_success_message);
                case RESTORE:
                    return getString(R.string.smartshifts_restore_success_message);
            }
        } else {
            switch (operationType) {
                case EXPORT:
                    return getString(R.string.smartshifts_export_error_message);
                case IMPORT:
                    return getString(R.string.smartshifts_import_error_message);
                case BACKUP:
                    return getString(R.string.smartshifts_backup_error_message);
                case RESTORE:
                    return getString(R.string.smartshifts_restore_error_message);
            }
        }
        return "";
    }

    private String buildDetailsText() {
        if (result == null) return "";

        StringBuilder details = new StringBuilder();

        // Add statistics based on result type
        if (result instanceof SmartShiftsExportImportManager.ExportResult) {
            SmartShiftsExportImportManager.ExportResult exportResult =
                    (SmartShiftsExportImportManager.ExportResult) result;

            if (exportResult.getRecordCount() > 0) {
                details.append(getString(R.string.smartshifts_result_processed_count,
                        exportResult.getRecordCount()));
                details.append("\n");
            }

            if (exportResult.isSuccess() && exportResult.getExportFile() != null) {
                details.append(getString(R.string.smartshifts_result_file_location,
                        exportResult.getExportFile().getAbsolutePath()));
                details.append("\n");
            }

            if (exportResult.getFileSize() > 0) {
                String sizeText = formatFileSize(exportResult.getFileSize());
                details.append(getString(R.string.smartshifts_result_file_size, sizeText));
                details.append("\n");
            }

        } else if (result instanceof SmartShiftsExportImportManager.ImportResult) {
            SmartShiftsExportImportManager.ImportResult importResult =
                    (SmartShiftsExportImportManager.ImportResult) result;

            details.append(getString(R.string.smartshifts_result_imported_count,
                    importResult.getImportedRecords()));
            details.append("\n");

            if (importResult.getSkippedRecords() > 0) {
                details.append(getString(R.string.smartshifts_result_skipped_count,
                        importResult.getSkippedRecords()));
                details.append("\n");
            }

        } else if (result instanceof SmartShiftsExportImportManager.BackupResult) {
            SmartShiftsExportImportManager.BackupResult backupResult =
                    (SmartShiftsExportImportManager.BackupResult) result;

            if (backupResult.isSuccess() && backupResult.getBackupFile() != null) {
                details.append(getString(R.string.smartshifts_result_file_location,
                        backupResult.getBackupFile().getAbsolutePath()));
                details.append("\n");
            }

            if (backupResult.getBackupSize() > 0) {
                String sizeText = formatFileSize(backupResult.getBackupSize());
                details.append(getString(R.string.smartshifts_result_file_size, sizeText));
                details.append("\n");
            }

        } else if (result instanceof SmartShiftsExportImportManager.SyncResult) {
            SmartShiftsExportImportManager.SyncResult syncResult =
                    (SmartShiftsExportImportManager.SyncResult) result;

            details.append(getString(R.string.smartshifts_result_sync_applied,
                    syncResult.getAppliedChanges()));
            details.append("\n");

            if (syncResult.getLocalChanges() > 0) {
                details.append(getString(R.string.smartshifts_result_sync_local,
                        syncResult.getLocalChanges()));
                details.append("\n");
            }

            if (syncResult.getRemoteChanges() > 0) {
                details.append(getString(R.string.smartshifts_result_sync_remote,
                        syncResult.getRemoteChanges()));
                details.append("\n");
            }

            if (syncResult.getConflicts() > 0) {
                details.append(getString(R.string.smartshifts_result_sync_conflicts,
                        syncResult.getConflicts()));
                details.append("\n");
            }
        }

        return details.toString().trim();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(),"%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void setupActionButtons() {
        // Close button - always visible
        buttonClose.setOnClickListener(v -> dismiss());

        boolean success = isResultSuccessful();
        if (success) {
            setupSuccessButtons();
        } else {
            setupErrorButtons();
        }
    }

    private void setupSuccessButtons() {
        String filePath = getResultFilePath();

        // Primary button - Share or Open file
        if (filePath != null) {
            if (operationType == OperationType.EXPORT) {
                buttonPrimary.setText(R.string.action_share);
                buttonPrimary.setIcon(getResources().getDrawable(R.drawable.ic_rounded_share_24, null));
                buttonPrimary.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onShareResult(filePath);
                    }
                    dismiss();
                });
                buttonPrimary.setVisibility(View.VISIBLE);

                // Secondary button - Open file
                buttonSecondary.setText(R.string.action_open);
                buttonSecondary.setIcon(getResources().getDrawable(R.drawable.ic_rounded_open_in_new_24, null));
                buttonSecondary.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOpenFile(filePath);
                    }
                    dismiss();
                });
                buttonSecondary.setVisibility(View.VISIBLE);
            } else {
                // For import/backup/restore, just show "Open location"
                buttonPrimary.setText(R.string.action_open_location);
                buttonPrimary.setIcon(getResources().getDrawable(R.drawable.ic_rounded_folder_open_24, null));
                buttonPrimary.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOpenFile(filePath);
                    }
                    dismiss();
                });
                buttonPrimary.setVisibility(View.VISIBLE);
                buttonSecondary.setVisibility(View.GONE);
            }
        } else {
            buttonPrimary.setVisibility(View.GONE);
            buttonSecondary.setVisibility(View.GONE);
        }
    }

    private void setupErrorButtons() {
        // Primary button - Retry
        buttonPrimary.setText(R.string.action_retry);
        buttonPrimary.setIcon(getResources().getDrawable(R.drawable.ic_rounded_refresh_24, null));
        buttonPrimary.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRetryOperation();
            }
            dismiss();
        });
        buttonPrimary.setVisibility(View.VISIBLE);

        // Hide secondary button for errors
        buttonSecondary.setVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}