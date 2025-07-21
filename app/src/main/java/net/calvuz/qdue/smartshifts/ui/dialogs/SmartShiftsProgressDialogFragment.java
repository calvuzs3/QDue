package net.calvuz.qdue.smartshifts.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.calvuz.qdue.R;

/**
 * Progress dialog for SmartShifts export/import operations
 * Shows progress with cancel support and detailed status
 */
public class SmartShiftsProgressDialogFragment extends DialogFragment {

    private static final String TAG = "SmartShiftsProgressDialog";
    private static final String ARG_TITLE = "title";
    private static final String ARG_OPERATION_TYPE = "operation_type";
    private static final String ARG_ALLOW_CANCEL = "allow_cancel";

    public enum OperationType {
        EXPORT, IMPORT, BACKUP, RESTORE
    }

    public interface ProgressListener {
        void onProgressCancelled(OperationType operationType);
    }

    private ProgressListener listener;
    private String title;
    private OperationType operationType;
    private boolean allowCancel;
    private boolean isCancelled = false;

    // UI Components
    private TextView textTitle;
    private TextView textStatus;
    private TextView textProgress;
    private ProgressBar progressBar;
    private Button buttonCancel;

    public static SmartShiftsProgressDialogFragment newInstance(
            String title,
            OperationType operationType,
            boolean allowCancel) {

        SmartShiftsProgressDialogFragment fragment = new SmartShiftsProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_OPERATION_TYPE, operationType);
        args.putBoolean(ARG_ALLOW_CANCEL, allowCancel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            operationType = (OperationType) getArguments().getSerializable(ARG_OPERATION_TYPE);
            allowCancel = getArguments().getBoolean(ARG_ALLOW_CANCEL, true);
        }

        // Non-cancellable dialog
        setCancelable(false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ProgressListener) context;
        } catch (ClassCastException e) {
            // Listener is optional for progress dialog
            listener = null;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_smartshifts_progress, null);

        initializeViews(dialogView);
        setupProgressDialog();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(dialogView);

        AlertDialog dialog = builder.create();

        // Prevent dismissal by back button or outside touch
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    private void initializeViews(View dialogView) {
        textTitle = dialogView.findViewById(R.id.text_title);
        textStatus = dialogView.findViewById(R.id.text_status);
        textProgress = dialogView.findViewById(R.id.text_progress);
        progressBar = dialogView.findViewById(R.id.progress_bar);
        buttonCancel = dialogView.findViewById(R.id.button_cancel);
    }

    private void setupProgressDialog() {
        // Set title
        if (title != null) {
            textTitle.setText(title);
        } else {
            // Set default title based on operation type
            switch (operationType) {
                case EXPORT:
                    textTitle.setText(R.string.smartshifts_progress_export_title);
                    break;
                case IMPORT:
                    textTitle.setText(R.string.smartshifts_progress_import_title);
                    break;
                case BACKUP:
                    textTitle.setText(R.string.smartshifts_progress_backup_title);
                    break;
                case RESTORE:
                    textTitle.setText(R.string.smartshifts_progress_restore_title);
                    break;
            }
        }

        // Setup cancel button
        if (allowCancel) {
            buttonCancel.setVisibility(View.VISIBLE);
            buttonCancel.setOnClickListener(v -> {
                isCancelled = true;
                if (listener != null) {
                    listener.onProgressCancelled(operationType);
                }
                dismiss();
            });
        } else {
            buttonCancel.setVisibility(View.GONE);
        }

        // Initial status
        updateStatus(getString(R.string.smartshifts_progress_initializing));
        updateProgress(0, 0, "");
    }

    /**
     * Update progress dialog status message
     */
    public void updateStatus(String status) {
        if (textStatus != null && !isCancelled) {
            textStatus.setText(status);
        }
    }

    /**
     * Update progress with current/total counts and item name
     */
    public void updateProgress(int current, int total, String currentItem) {
        if (isCancelled) return;

        if (progressBar != null) {
            if (total > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setMax(total);
                progressBar.setProgress(current);
            } else {
                progressBar.setIndeterminate(true);
            }
        }

        if (textProgress != null) {
            if (total > 0) {
                String progressText;
                if (currentItem != null && !currentItem.isEmpty()) {
                    progressText = getString(R.string.smartshifts_progress_with_item,
                            current, total, currentItem);
                } else {
                    progressText = getString(R.string.smartshifts_progress_without_item,
                            current, total);
                }
                textProgress.setText(progressText);
            } else {
                textProgress.setText(R.string.smartshifts_progress_processing);
            }
        }
    }

    /**
     * Update progress with percentage
     */
    public void updateProgress(int percentage, String currentItem) {
        if (isCancelled) return;

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(percentage);
        }

        if (textProgress != null) {
            String progressText;
            if (currentItem != null && !currentItem.isEmpty()) {
                progressText = getString(R.string.smartshifts_progress_percentage_with_item,
                        percentage, currentItem);
            } else {
                progressText = getString(R.string.smartshifts_progress_percentage, percentage);
            }
            textProgress.setText(progressText);
        }
    }

    /**
     * Show indeterminate progress
     */
    public void showIndeterminateProgress(String message) {
        if (isCancelled) return;

        if (progressBar != null) {
            progressBar.setIndeterminate(true);
        }

        if (textProgress != null) {
            textProgress.setText(message != null ? message :
                    getString(R.string.smartshifts_progress_processing));
        }
    }

    /**
     * Check if operation was cancelled
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Complete progress and dismiss dialog
     */
    public void complete() {
        if (!isCancelled) {
            dismiss();
        }
    }

    /**
     * Complete with error message
     */
    public void completeWithError(String errorMessage) {
        if (!isCancelled) {
            updateStatus(errorMessage);
            if (buttonCancel != null) {
                buttonCancel.setText(R.string.action_close);
                buttonCancel.setVisibility(View.VISIBLE);
                buttonCancel.setOnClickListener(v -> dismiss());
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}