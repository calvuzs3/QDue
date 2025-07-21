package net.calvuz.qdue.smartshifts.ui.exportimport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.ui.exportimport.viewmodel.ExportImportViewModel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Recent Operations RecyclerView in Export/Import Activity.
 * <p>
 * Displays a list of recent export/import operations with their status,
 * details, and action buttons. Supports expandable details view and
 * quick actions like share and retry.
 * <p>
 * Features:
 * - Success/Error status indicators
 * - Time and date formatting
 * - Expandable details section
 * - Quick action buttons (share, retry)
 * - Click listeners for item interactions
 * <p>
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
public class RecentOperationsAdapter extends RecyclerView.Adapter<RecentOperationsAdapter.OperationViewHolder> {

    private static final String TAG = "RecentOperationsAdapter";

    private final Context context;
    private List<ExportImportViewModel.RecentOperation> operations;
    private OnOperationClickListener clickListener;

    public RecentOperationsAdapter(@NonNull Context context) {
        this.context = context;
        this.operations = new ArrayList<>();
    }

    /**
     * Interface for handling operation item clicks
     */
    public interface OnOperationClickListener {
        void onOperationClick(ExportImportViewModel.RecentOperation operation);
        void onOperationLongClick(ExportImportViewModel.RecentOperation operation);
        void onShareClick(ExportImportViewModel.RecentOperation operation);
        void onRetryClick(ExportImportViewModel.RecentOperation operation);
        void onActionMenuClick(ExportImportViewModel.RecentOperation operation);
    }

    /**
     * Set click listener for operation interactions
     */
    public void setOnOperationClickListener(OnOperationClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Update operations list
     */
    public void setOperations(@NonNull List<ExportImportViewModel.RecentOperation> operations) {
        this.operations = new ArrayList<>(operations);
        notifyDataSetChanged();
    }

    /**
     * Add single operation to the list
     */
    public void addOperation(@NonNull ExportImportViewModel.RecentOperation operation) {
        this.operations.add(0, operation); // Add at beginning
        notifyItemInserted(0);
    }

    /**
     * Remove operation from the list
     */
    public void removeOperation(int position) {
        if (position >= 0 && position < operations.size()) {
            operations.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public OperationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_operation, parent, false);
        return new OperationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OperationViewHolder holder, int position) {
        ExportImportViewModel.RecentOperation operation = operations.get(position);
        holder.bind(operation);
    }

    @Override
    public int getItemCount() {
        return operations.size();
    }

    /**
     * ViewHolder for operation items
     */
    public class OperationViewHolder extends RecyclerView.ViewHolder {

        // Main views
        private final ImageView iconSuccess;
        private final ImageView iconError;
        private final View backgroundCircle;
        private final TextView textOperationType;
        private final TextView textOperationTime;
        private final TextView textOperationDetails;
        private final TextView textOperationDate;

        // Action views
        private final ImageButton btnAction;
        private final LinearLayout layoutStatusBadge;
        private final ImageView badgeIcon;
        private final TextView badgeText;

        // Expandable details
        private final LinearLayout layoutExpandableDetails;
        private final TextView textExtendedFileName;
        private final TextView textExtendedFileSize;
        private final TextView textExtendedDuration;
        private final MaterialButton btnShareResult;
        private final MaterialButton btnRetryOperation;

        // State
        private boolean isExpanded = false;

        public OperationViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize main views
            iconSuccess = itemView.findViewById(R.id.icon_success);
            iconError = itemView.findViewById(R.id.icon_error);
            backgroundCircle = itemView.findViewById(R.id.background_circle);
            textOperationType = itemView.findViewById(R.id.text_operation_type);
            textOperationTime = itemView.findViewById(R.id.text_operation_time);
            textOperationDetails = itemView.findViewById(R.id.text_operation_details);
            textOperationDate = itemView.findViewById(R.id.text_operation_date);

            // Initialize action views
            btnAction = itemView.findViewById(R.id.btn_action);
            layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
            badgeIcon = itemView.findViewById(R.id.badge_icon);
            badgeText = itemView.findViewById(R.id.badge_text);

            // Initialize expandable details
            layoutExpandableDetails = itemView.findViewById(R.id.layout_expandable_details);
            textExtendedFileName = itemView.findViewById(R.id.text_extended_file_name);
            textExtendedFileSize = itemView.findViewById(R.id.text_extended_file_size);
            textExtendedDuration = itemView.findViewById(R.id.text_extended_duration);
            btnShareResult = itemView.findViewById(R.id.btn_share_result);
            btnRetryOperation = itemView.findViewById(R.id.btn_retry_operation);

            // Setup click listeners
            setupClickListeners();
        }

        /**
         * Setup click listeners for all interactive elements
         */
        private void setupClickListeners() {
            // Main item click for expansion
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    ExportImportViewModel.RecentOperation operation = operations.get(getAdapterPosition());
                    clickListener.onOperationClick(operation);
                }
                toggleExpansion();
            });

            // Long click for context actions
            itemView.setOnLongClickListener(v -> {
                if (clickListener != null) {
                    ExportImportViewModel.RecentOperation operation = operations.get(getAdapterPosition());
                    clickListener.onOperationLongClick(operation);
                }
                return true;
            });

            // Action menu button
            if (btnAction != null) {
                btnAction.setOnClickListener(v -> {
                    if (clickListener != null) {
                        ExportImportViewModel.RecentOperation operation = operations.get(getAdapterPosition());
                        clickListener.onActionMenuClick(operation);
                    }
                });
            }

            // Share button
            if (btnShareResult != null) {
                btnShareResult.setOnClickListener(v -> {
                    if (clickListener != null) {
                        ExportImportViewModel.RecentOperation operation = operations.get(getAdapterPosition());
                        clickListener.onShareClick(operation);
                    }
                });
            }

            // Retry button
            if (btnRetryOperation != null) {
                btnRetryOperation.setOnClickListener(v -> {
                    if (clickListener != null) {
                        ExportImportViewModel.RecentOperation operation = operations.get(getAdapterPosition());
                        clickListener.onRetryClick(operation);
                    }
                });
            }
        }

        /**
         * Bind operation data to views
         */
        public void bind(@NonNull ExportImportViewModel.RecentOperation operation) {
            // Set operation type
            textOperationType.setText(formatOperationType(operation.getOperationType()));

            // Set time
            textOperationTime.setText(operation.getFormattedTime());

            // Set details
            textOperationDetails.setText(operation.getDetails());

            // Set status icons and colors
            updateStatusDisplay(operation.isSuccess());

            // Set date visibility (show only for operations not from today)
            updateDateDisplay(operation.getTimestamp());

            // Update expandable details
            updateExpandableDetails(operation);

            // Update action buttons visibility
            updateActionButtons(operation);
        }

        /**
         * Format operation type for display
         */
        private String formatOperationType(String operationType) {
            switch (operationType.toLowerCase()) {
                case "export":
                    return "Export Dati";
                case "import":
                    return "Import Dati";
                case "backup":
                    return "Backup";
                case "sync":
                    return "Sincronizzazione";
                case "restore":
                    return "Ripristino";
                default:
                    return operationType;
            }
        }

        /**
         * Update status display based on success/failure
         */
        private void updateStatusDisplay(boolean success) {
            if (success) {
                iconSuccess.setVisibility(View.VISIBLE);
                iconError.setVisibility(View.GONE);
                backgroundCircle.setBackgroundResource(R.drawable.circle_background_success);

                // Update badge if used
                if (layoutStatusBadge.getVisibility() == View.VISIBLE) {
                    badgeIcon.setImageResource(R.drawable.ic_rounded_check_24);
                    badgeText.setText(R.string.common_ok);
                }
            } else {
                iconSuccess.setVisibility(View.GONE);
                iconError.setVisibility(View.VISIBLE);
                backgroundCircle.setBackgroundResource(R.drawable.circle_background_error);

                // Update badge if used
                if (layoutStatusBadge.getVisibility() == View.VISIBLE) {
                    badgeIcon.setImageResource(R.drawable.ic_rounded_error_24);
                    badgeText.setText(R.string.error);
                }
            }
        }

        /**
         * Update date display visibility
         */
        private void updateDateDisplay(long timestamp) {
            LocalDate operationDate = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000));
            LocalDate today = LocalDate.now();

            long daysDifference = ChronoUnit.DAYS.between(operationDate, today);

            if (daysDifference == 0) {
                // Today - hide date
                textOperationDate.setVisibility(View.GONE);
            } else if (daysDifference == 1) {
                // Yesterday
                textOperationDate.setText(R.string.common_yesterday);
                textOperationDate.setVisibility(View.VISIBLE);
            } else if (daysDifference < 7) {
                // Within a week
                textOperationDate.setText(daysDifference + " giorni fa");
                textOperationDate.setVisibility(View.VISIBLE);
            } else {
                // Show actual date
                textOperationDate.setText(operationDate.toString());
                textOperationDate.setVisibility(View.VISIBLE);
            }
        }

        /**
         * Update expandable details section
         */
        private void updateExpandableDetails(@NonNull ExportImportViewModel.RecentOperation operation) {
            // This would be populated with actual file details in a real implementation
            // For now, show placeholder data

            if (textExtendedFileName != null) {
                String fileName = generateFileName(operation);
                textExtendedFileName.setText(fileName);
            }

            if (textExtendedFileSize != null) {
                String fileSize = estimateFileSize(operation);
                textExtendedFileSize.setText(fileSize);
            }

            if (textExtendedDuration != null) {
                String duration = estimateDuration(operation);
                textExtendedDuration.setText(duration);
            }
        }

        /**
         * Update action buttons based on operation type and status
         */
        private void updateActionButtons(@NonNull ExportImportViewModel.RecentOperation operation) {
            if (btnShareResult != null) {
                // Show share button only for successful exports
                boolean showShare = operation.isSuccess() &&
                        (operation.getOperationType().toLowerCase().contains("export") ||
                                operation.getOperationType().toLowerCase().contains("backup"));
                btnShareResult.setVisibility(showShare ? View.VISIBLE : View.GONE);
            }

            if (btnRetryOperation != null) {
                // Show retry button for failed operations
                btnRetryOperation.setVisibility(operation.isSuccess() ? View.GONE : View.VISIBLE);
            }
        }

        /**
         * Toggle expansion of details section
         */
        private void toggleExpansion() {
            if (layoutExpandableDetails != null) {
                isExpanded = !isExpanded;
                layoutExpandableDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                // Animate the expansion (optional)
                if (isExpanded) {
                    layoutExpandableDetails.setAlpha(0f);
                    layoutExpandableDetails.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                }
            }
        }

        // Helper methods for generating placeholder data

        private String generateFileName(@NonNull ExportImportViewModel.RecentOperation operation) {
            String type = operation.getOperationType().toLowerCase();

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
            String timestamp = sdf.format(new java.util.Date(operation.getTimestamp()));


            switch (type) {
                case "export":
                    return "smartshifts_export_" + timestamp + ".json";
                case "backup":
                    return "smartshifts_backup_" + timestamp + ".json";
                case "import":
                    return "imported_data.json";
                default:
                    return "smartshifts_" + type + "_" + timestamp + ".json";
            }
        }

        private String estimateFileSize(@NonNull ExportImportViewModel.RecentOperation operation) {
            // Placeholder file size estimation
            if (operation.isSuccess()) {
                return Math.random() > 0.5 ? "2.3 MB" : "1.8 MB";
            } else {
                return "N/A";
            }
        }

        private String estimateDuration(@NonNull ExportImportViewModel.RecentOperation operation) {
            // Placeholder duration estimation
            if (operation.isSuccess()) {
                int seconds = (int) (Math.random() * 30) + 5;
                return seconds + " secondi";
            } else {
                return "Interrotto";
            }
        }
    }

    /**
     * Helper method to check if list is empty
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    /**
     * Clear all operations
     */
    public void clearOperations() {
        int size = operations.size();
        operations.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Get operation at specific position
     */
    public ExportImportViewModel.RecentOperation getOperationAt(int position) {
        if (position >= 0 && position < operations.size()) {
            return operations.get(position);
        }
        return null;
    }

    /**
     * Update single operation
     */
    public void updateOperation(int position, @NonNull ExportImportViewModel.RecentOperation operation) {
        if (position >= 0 && position < operations.size()) {
            operations.set(position, operation);
            notifyItemChanged(position);
        }
    }
}