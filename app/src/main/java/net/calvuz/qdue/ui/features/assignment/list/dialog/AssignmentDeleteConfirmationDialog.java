package net.calvuz.qdue.ui.features.assignment.list.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * AssignmentDeleteConfirmationDialog - Confirmation Dialog for Assignment Deletion
 *
 * <p>Material Design confirmation dialog for assignment deletion with detailed
 * information about the assignment being deleted and clear action buttons.</p>
 *
 * <h3>Dialog Content:</h3>
 * <ul>
 *   <li><strong>Title</strong>: Clear deletion confirmation title</li>
 *   <li><strong>Message</strong>: Assignment details and deletion warning</li>
 *   <li><strong>Positive Button</strong>: "Delete" with destructive styling</li>
 *   <li><strong>Negative Button</strong>: "Cancel" for safe cancellation</li>
 * </ul>
 *
 * <h3>Assignment Information:</h3>
 * <ul>
 *   <li><strong>Assignment Name</strong>: Pattern title or auto-generated name</li>
 *   <li><strong>Team Assignment</strong>: Team name for context</li>
 *   <li><strong>Date Range</strong>: Assignment validity period</li>
 *   <li><strong>Status Warning</strong>: Special warnings for active assignments</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment Management
 * @since Clean Architecture Phase 2
 */
public class AssignmentDeleteConfirmationDialog extends DialogFragment {

    private static final String TAG = "AssignmentDeleteConfirmationDialog";
    private static final String ARG_ASSIGNMENT = "assignment";

    // ==================== CALLBACK INTERFACE ====================

    /**
     * Callback interface for deletion confirmation.
     */
    public interface OnConfirmDeleteListener {
        void onConfirmDelete();
    }

    // ==================== STATE ====================

    private UserScheduleAssignment mAssignment;
    private OnConfirmDeleteListener mConfirmListener;

    // ==================== FACTORY ====================

    /**
     * Create new instance of delete confirmation dialog.
     *
     * @param assignment Assignment to be deleted
     * @return Dialog instance
     */
    @NonNull
    public static AssignmentDeleteConfirmationDialog newInstance(@NonNull UserScheduleAssignment assignment) {
        AssignmentDeleteConfirmationDialog dialog = new AssignmentDeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ASSIGNMENT, assignment);
        dialog.setArguments(args);
        return dialog;
    }

    // ==================== CONFIGURATION ====================

    /**
     * Set confirmation listener.
     *
     * @param listener Callback for deletion confirmation
     * @return This dialog for method chaining
     */
    @NonNull
    public AssignmentDeleteConfirmationDialog setOnConfirmListener(@Nullable OnConfirmDeleteListener listener) {
        this.mConfirmListener = listener;
        return this;
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get assignment from arguments
        if (getArguments() != null) {
            mAssignment = (UserScheduleAssignment) getArguments().getSerializable(ARG_ASSIGNMENT);
        }

        if (mAssignment == null) {
            Log.e(TAG, "Assignment is required for delete confirmation");
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (mAssignment == null || getContext() == null) {
            // Fallback dialog if something goes wrong
            return new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(R.string.assignment_delete_error_missing_data)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        // Build confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Dialog title
        builder.setTitle(R.string.assignment_delete_confirmation_title);

        // Dialog message with assignment details
        String message = buildConfirmationMessage();
        builder.setMessage(message);

        // Positive button (Delete) - destructive action
        builder.setPositiveButton(R.string.assignment_delete_confirm_button,
                                  (dialog, which) -> {
                                      Log.d(TAG, "User confirmed deletion of assignment: " + mAssignment.getId());
                                      if (mConfirmListener != null) {
                                          mConfirmListener.onConfirmDelete();
                                      }
                                  });

        // Negative button (Cancel) - safe action
        builder.setNegativeButton(android.R.string.cancel,
                                  (dialog, which) -> {
                                      Log.d(TAG, "User cancelled deletion of assignment: " + mAssignment.getId());
                                      dialog.dismiss();
                                  });

        // Create dialog
        AlertDialog dialog = builder.create();

        // Style the positive button as destructive
        dialog.setOnShowListener(dialogInterface -> dialog.getButton( DialogInterface.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.error_color, getContext().getTheme())) );

        return dialog;
    }

    // ==================== MESSAGE BUILDING ====================

    /**
     * Build detailed confirmation message with assignment information.
     */
    @NonNull
    private String buildConfirmationMessage() {
        if (getContext() == null) {
            return "Are you sure you want to delete this assignment?";
        }

        StringBuilder messageBuilder = new StringBuilder();

        // Base confirmation message
        messageBuilder.append(getString(R.string.assignment_delete_confirmation_message));
        messageBuilder.append("\n\n");

        // Assignment details
        messageBuilder.append(getString(R.string.assignment_delete_details_header));
        messageBuilder.append("\n");

        // Assignment name
        String assignmentName = getAssignmentDisplayName();
        messageBuilder.append("• ").append(getString(R.string.assignment_delete_detail_name, assignmentName)).append("\n");

        // Team assignment
        String teamName = getTeamDisplayName();
        messageBuilder.append("• ").append(getString(R.string.assignment_delete_detail_team, teamName)).append("\n");

        // Date range
        String dateRange = getDateRangeDisplayText();
        messageBuilder.append("• ").append(getString(R.string.assignment_delete_detail_dates, dateRange)).append("\n");

        // Status-specific warnings
        String statusWarning = getStatusWarning();
        if (statusWarning != null) {
            messageBuilder.append("\n");
            messageBuilder.append(statusWarning);
        }

        // Final warning
        messageBuilder.append("\n\n");
        messageBuilder.append(getString(R.string.assignment_delete_final_warning));

        return messageBuilder.toString();
    }

    /**
     * Get assignment display name with fallback.
     */
    @NonNull
    private String getAssignmentDisplayName() {
        String title = mAssignment.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = mAssignment.getAssignedByUserName();
            if (title == null || title.trim().isEmpty()) {
                title = getString(R.string.assignment_default_title);
            }
        }
        return title;
    }

    /**
     * Get team display name with fallback.
     */
    @NonNull
    private String getTeamDisplayName() {
        String teamName = mAssignment.getTeamName();
        if (teamName == null || teamName.trim().isEmpty()) {
            teamName = mAssignment.getTeamId();
        }
        return teamName;
    }

    /**
     * Get formatted date range display text.
     */
    @NonNull
    private String getDateRangeDisplayText() {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        if (mAssignment.getEndDate() == null) {
            // Permanent assignment
            return getString(R.string.assignment_date_permanent_short,
                             mAssignment.getStartDate().format(formatter));
        } else {
            // Time-bounded assignment
            return getString(R.string.assignment_date_range_short,
                             mAssignment.getStartDate().format(formatter),
                             mAssignment.getEndDate().format(formatter));
        }
    }

    /**
     * Get status-specific warning message.
     */
    @Nullable
    private String getStatusWarning() {
        if (getContext() == null) {
            return null;
        }

        return switch (mAssignment.getStatus()) {
            case ACTIVE -> getString( R.string.assignment_delete_warning_active );
            case PENDING -> getString( R.string.assignment_delete_warning_pending );
            case EXPIRED, CANCELLED ->
                // No special warning for expired/cancelled assignments
                    null;
            default -> null;
        };
    }
}