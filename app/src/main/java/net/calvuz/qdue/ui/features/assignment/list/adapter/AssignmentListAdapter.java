package net.calvuz.qdue.ui.features.assignment.list.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.common.enums.Status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * AssignmentListAdapter - RecyclerView Adapter for UserScheduleAssignment List
 *
 * <p>Adapter for displaying user schedule assignments in a RecyclerView with
 * Material Design card layout and action buttons.</p>
 *
 * <h3>Card Components:</h3>
 * <ul>
 *   <li><strong>Assignment Title</strong>: Pattern name or auto-generated title</li>
 *   <li><strong>Team Name</strong>: Assigned team display name</li>
 *   <li><strong>Date Range</strong>: Start date → End date (or "Permanent")</li>
 *   <li><strong>Status Chip</strong>: Color-coded status indicator</li>
 *   <li><strong>Action Buttons</strong>: Edit and Delete buttons</li>
 * </ul>
 *
 * <h3>Status Colors:</h3>
 * <ul>
 *   <li><strong>ACTIVE</strong>: Green - Currently active assignment</li>
 *   <li><strong>PENDING</strong>: Blue - Future assignment</li>
 *   <li><strong>EXPIRED</strong>: Gray - Past assignment</li>
 *   <li><strong>CANCELLED</strong>: Red - Cancelled assignment</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment List UI
 * @since Clean Architecture Phase 2
 */
public class AssignmentListAdapter extends RecyclerView.Adapter<AssignmentListAdapter.AssignmentViewHolder>
{
    // ==================== INTERFACES ====================

    /**
     * Callback interface for assignment actions.
     */
    public interface OnAssignmentActionListener {
        void onEditAssignment(@NonNull UserScheduleAssignment assignment);
        void onDeleteAssignment(@NonNull UserScheduleAssignment assignment);
        void onViewAssignmentDetails(@NonNull UserScheduleAssignment assignment);
    }

    // ==================== STATE ====================

    private final List<UserScheduleAssignment> mAssignments;
    private final OnAssignmentActionListener mActionListener;
    private final DateTimeFormatter mDateFormatter;

    // ==================== CONSTRUCTOR ====================

    public AssignmentListAdapter(@NonNull List<UserScheduleAssignment> assignments,
                                 @Nullable OnAssignmentActionListener actionListener) {
        this.mAssignments = assignments;
        this.mActionListener = actionListener;
        this.mDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    }

    // ==================== RECYCLERVIEW ADAPTER ====================

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment_card, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        UserScheduleAssignment assignment = mAssignments.get(position);
        holder.bind(assignment);
    }

    @Override
    public int getItemCount() {
        return mAssignments.size();
    }

    // ==================== VIEW HOLDER ====================

    /**
     * ViewHolder for assignment card items.
     */
    public class AssignmentViewHolder extends RecyclerView.ViewHolder {

        // UI Components
        private final MaterialCardView mCardView;
        private final TextView mTitleText;
        private final TextView mTeamText;
        private final TextView mDateRangeText;
        private final Chip mStatusChip;
        private final ImageButton mEditButton;
        private final ImageButton mDeleteButton;

        // State
        private UserScheduleAssignment mCurrentAssignment;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            mCardView = itemView.findViewById(R.id.card_assignment);
            mTitleText = itemView.findViewById(R.id.text_assignment_title);
            mTeamText = itemView.findViewById(R.id.text_team_name);
            mDateRangeText = itemView.findViewById(R.id.text_date_range);
            mStatusChip = itemView.findViewById(R.id.chip_status);
            mEditButton = itemView.findViewById(R.id.button_edit);
            mDeleteButton = itemView.findViewById(R.id.button_delete);

            // Setup click listeners
            setupClickListeners();
        }

        /**
         * Setup click listeners for card and action buttons.
         */
        private void setupClickListeners() {
            // Card click for details view
            mCardView.setOnClickListener(v -> {
                if (mActionListener != null && mCurrentAssignment != null) {
                    mActionListener.onViewAssignmentDetails(mCurrentAssignment);
                }
            });

            // Edit button
            mEditButton.setOnClickListener(v -> {
                if (mActionListener != null && mCurrentAssignment != null) {
                    mActionListener.onEditAssignment(mCurrentAssignment);
                }
            });

            // Delete button
            mDeleteButton.setOnClickListener(v -> {
                if (mActionListener != null && mCurrentAssignment != null) {
                    mActionListener.onDeleteAssignment(mCurrentAssignment);
                }
            });
        }

        /**
         * Bind assignment data to view components.
         */
        public void bind(@NonNull UserScheduleAssignment assignment) {
            mCurrentAssignment = assignment;
            Context context = itemView.getContext();

            // Assignment title
            bindTitle(assignment, context);

            // Team name
            bindTeam(assignment, context);

            // Date range
            bindDateRange(assignment, context);

            // Status chip
            bindStatusChip(assignment, context);

            // Action button states
            updateActionButtons(assignment);
        }

        /**
         * Bind assignment title with fallback.
         */
        private void bindTitle(@NonNull UserScheduleAssignment assignment, @NonNull Context context) {
            String title = assignment.getTitle();
            if (title == null || title.trim().isEmpty()) {
                // Fallback to auto-generated name
                title = assignment.getAssignedByUserName();
                if (title == null || title.trim().isEmpty()) {
                    title = context.getString(R.string.assignment_default_title);
                }
            }
            mTitleText.setText(title);
        }

        /**
         * Bind team name with fallback.
         */
        private void bindTeam(@NonNull UserScheduleAssignment assignment, @NonNull Context context) {
            String teamName = assignment.getTeamName();
            if (teamName == null || teamName.trim().isEmpty()) {
                teamName = assignment.getTeamId(); // Fallback to ID
            }
            mTeamText.setText(context.getString(R.string.assignment_team_format, teamName));
        }

        /**
         * Bind date range display.
         */
        private void bindDateRange(@NonNull UserScheduleAssignment assignment, @NonNull Context context) {
            LocalDate startDate = assignment.getStartDate();
            LocalDate endDate = assignment.getEndDate();

            String dateRangeText;
            if (endDate == null) {
                // Permanent assignment
                dateRangeText = context.getString(R.string.assignment_date_permanent,
                                                  startDate.format(mDateFormatter));
            } else {
                // Time-bounded assignment
                dateRangeText = context.getString(R.string.assignment_date_range,
                                                  startDate.format(mDateFormatter),
                                                  endDate.format(mDateFormatter));
            }

            mDateRangeText.setText(dateRangeText);
        }

        /**
         * Bind status chip with appropriate color and text.
         */
        private void bindStatusChip(@NonNull UserScheduleAssignment assignment, @NonNull Context context) {
            Status status = assignment.getStatus();

            // Set status text
            String statusText = getStatusDisplayText(status, context);
            mStatusChip.setText(statusText);

            // Set status color
            int chipBackgroundColor = getStatusColor(status, context);
            mStatusChip.setChipBackgroundColor(
                    ContextCompat.getColorStateList(context, chipBackgroundColor));

            // Set text color for readability
            int textColor = getStatusTextColor(status, context);
            mStatusChip.setTextColor(ContextCompat.getColor(context, textColor));
        }

        /**
         * Update action button states based on assignment.
         */
        private void updateActionButtons(@NonNull UserScheduleAssignment assignment) {
            // Edit button - always enabled for now
            mEditButton.setEnabled(true);
            mEditButton.setAlpha(1.0f);

            // Delete button - always enabled, validation handled in service
            mDeleteButton.setEnabled(true);
            mDeleteButton.setAlpha(1.0f);

            // Could add logic here to disable buttons based on business rules
            // For example:
            // - Disable delete for critical active assignments
            // - Disable edit for assignments that have started
        }

        /**
         * Get display text for assignment status.
         */
        @NonNull
        private String getStatusDisplayText(@NonNull Status status,
                                            @NonNull Context context) {
            return switch (status) {
                case ACTIVE -> context.getString( R.string.assignment_status_active );
                case PENDING -> context.getString( R.string.assignment_status_pending );
                case EXPIRED -> context.getString( R.string.assignment_status_expired );
                case CANCELLED -> context.getString( R.string.assignment_status_cancelled );
                default -> context.getString( R.string.assignment_status_unknown );
            };
        }

        /**
         * Get background color resource for status chip.
         */
        private int getStatusColor(@NonNull Status status, @NonNull Context context) {
            return switch (status) {
                case ACTIVE -> R.color.status_active_background;
                case PENDING -> R.color.status_pending_background;
                case EXPIRED -> R.color.status_expired_background;
                case CANCELLED -> R.color.status_cancelled_background;
                default -> R.color.status_unknown_background;
            };
        }

        /**
         * Get text color resource for status chip.
         */
        private int getStatusTextColor(@NonNull Status status, @NonNull Context context) {
            return switch (status) {
                case ACTIVE -> R.color.status_active_text;
                case PENDING -> R.color.status_pending_text;
                case EXPIRED -> R.color.status_expired_text;
                case CANCELLED -> R.color.status_cancelled_text;
                default -> R.color.status_unknown_text;
            };
        }
    }
}