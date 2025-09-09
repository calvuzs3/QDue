package net.calvuz.qdue.ui.features.assignment.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.data.services.UserSchedulePatternService;
import net.calvuz.qdue.domain.qdueuser.models.QDueUser;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.list.adapter.AssignmentListAdapter;
import net.calvuz.qdue.ui.features.assignment.list.component.AssignmentListItemDecoration;
import net.calvuz.qdue.ui.features.assignment.list.dialog.AssignmentDeleteConfirmationDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * AssignmentListFragment - Assignment List Display and Management
 *
 * <p>Fragment displaying user's schedule assignments in a RecyclerView with
 * swipe-to-refresh functionality and empty state handling.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>RecyclerView</strong>: Scrollable list of assignment cards</li>
 *   <li><strong>Swipe Refresh</strong>: Pull-to-refresh functionality</li>
 *   <li><strong>Empty State</strong>: Friendly message when no assignments</li>
 *   <li><strong>FAB</strong>: Create new assignment floating action button</li>
 *   <li><strong>Error Handling</strong>: User-friendly error messages</li>
 * </ul>
 *
 * <h3>Assignment Actions:</h3>
 * <ul>
 *   <li><strong>View Details</strong>: Tap to view assignment information</li>
 *   <li><strong>Edit Assignment</strong>: Edit button opens wizard</li>
 *   <li><strong>Delete Assignment</strong>: Delete with confirmation dialog</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment List UI
 * @since Clean Architecture Phase 2
 */
public class AssignmentListFragment extends Fragment implements
        AssignmentListAdapter.OnAssignmentActionListener {

    private static final String TAG = "AssignmentListFragment";
    private static final String ARG_USER_ID = "user_id";

    // ==================== UI COMPONENTS ====================

    private RecyclerView mRecyclerView;
    private TextView mEmptyStateText;
    private View mEmptyStateContainer;
    private FloatingActionButton mFab;

    // ==================== STATE ====================

    private QDueUser mUser;
    private AssignmentListAdapter mAdapter;
    private UserSchedulePatternService mPatternService;
    private final List<UserScheduleAssignment> mAssignments = new ArrayList<>();

    // ==================== FACTORY ====================

    /**
     * Create new instance of AssignmentListFragment.
     *
     * @param user User ID for assignments
     * @return Fragment instance
     */
    @NonNull
    public static AssignmentListFragment newInstance(@NonNull QDueUser user) {
        AssignmentListFragment fragment = new AssignmentListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER_ID, user);
        fragment.setArguments(args);
        return fragment;
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get arguments
        if (getArguments() != null) {
            mUser = (QDueUser) getArguments().getSerializable( ARG_USER_ID);
        }

        if (mUser == null) {
            Log.e(TAG, "User ID is required");
            throw new IllegalArgumentException("User ID is required");
        }

        Log.d(TAG, "AssignmentListFragment created for user: " + mUser );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assignment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get pattern service from activity
        if (getActivity() instanceof AssignmentListActivity) {
            mPatternService = ((AssignmentListActivity) getActivity()).getPatternService();
        }

        if (mPatternService == null) {
            Log.e(TAG, "Pattern service not available");
            return;
        }

        // Initialize UI
        initializeViews(view);
        setupRecyclerView();
        setupFab();

        // Load assignments
        loadAssignments();
    }

    // ==================== UI INITIALIZATION ====================

    /**
     * Initialize view references.
     */
    private void initializeViews(@NonNull View view) {
        mRecyclerView = view.findViewById(R.id.recycler_view_assignments);
        mEmptyStateContainer = view.findViewById(R.id.empty_state_container);
        mEmptyStateText = view.findViewById(R.id.text_empty_state);
        mFab = view.findViewById(R.id.fab_create_assignment);
    }

    /**
     * Setup RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        mAdapter = new AssignmentListAdapter(mAssignments, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Add item decoration for spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.assignment_card_spacing);
        mRecyclerView.addItemDecoration(new AssignmentListItemDecoration( spacing));
    }

    /**
     * Setup floating action button for creating assignments.
     */
    private void setupFab() {
        mFab.setOnClickListener(v -> {
            if (getActivity() instanceof AssignmentListActivity) {
                ((AssignmentListActivity) getActivity()).startCreateAssignment();
            }
        });
    }

    // ==================== DATA OPERATIONS ====================

    /**
     * Load assignments from service.
     */
    private void loadAssignments() {
        setLoadingState(true);

        mPatternService.getUserAssignmentsList( mUser.getId() )
                .thenAccept(result -> {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);

                        if (result.isSuccess()) {
                            handleAssignmentsLoaded(result.getData());
                        } else {
                            handleLoadError(result.getErrorMessage());
                        }
                    });
                })
                .exceptionally(throwable -> {
                    if (getActivity() == null) return null;

                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        handleLoadError("Failed to load assignments: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Refresh assignments (called by swipe-to-refresh or activity).
     */
    public void refreshAssignments() {
        loadAssignments();
    }

    /**
     * Handle successful assignment loading.
     */
    private void handleAssignmentsLoaded(@Nullable List<UserScheduleAssignment> assignments) {
        if (assignments == null) {
            assignments = new ArrayList<>();
        }

        mAssignments.clear();
        mAssignments.addAll(assignments);
        mAdapter.notifyDataSetChanged();

        updateEmptyState();

        Log.d(TAG, "Loaded " + assignments.size() + " assignments");
    }

    /**
     * Handle assignment loading error.
     */
    private void handleLoadError(@NonNull String errorMessage) {
        Log.e(TAG, "Error loading assignments: " + errorMessage);

        if (getView() != null) {
            Snackbar.make(getView(),
                          getString(R.string.error_loading_assignments),
                          Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, v -> loadAssignments())
                    .show();
        }

        updateEmptyState();
    }

    // ==================== UI STATE MANAGEMENT ====================

    /**
     * Set loading state for UI components.
     * Note: Simplified version without SwipeRefreshLayout
     */
    private void setLoadingState(boolean loading) {
        // Since we removed SwipeRefreshLayout, this is now a no-op
        // You could add a progress bar here if needed
        Log.d(TAG, "Loading state: " + loading);
    }

    /**
     * Update empty state visibility based on assignment count.
     */
    private void updateEmptyState() {
        boolean isEmpty = mAssignments.isEmpty();

        if (mEmptyStateContainer != null) {
            mEmptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        if (isEmpty && mEmptyStateText != null) {
            mEmptyStateText.setText(R.string.assignment_list_empty_message);
        }
    }

    // ==================== ASSIGNMENT ACTION LISTENER ====================

    @Override
    public void onEditAssignment(@NonNull UserScheduleAssignment assignment) {
        Log.d(TAG, "Edit assignment requested: " + assignment.getId());

        if (getActivity() instanceof AssignmentListActivity) {
            ((AssignmentListActivity) getActivity()).startEditAssignment(assignment.getId());
        }
    }

    @Override
    public void onDeleteAssignment(@NonNull UserScheduleAssignment assignment) {
        Log.d(TAG, "Delete assignment requested: " + assignment.getId());

        // Show confirmation dialog
        AssignmentDeleteConfirmationDialog.newInstance( assignment)
                .setOnConfirmListener(() -> performDeleteAssignment(assignment))
                .show(getParentFragmentManager(), "delete_confirmation");
    }

    @Override
    public void onViewAssignmentDetails(@NonNull UserScheduleAssignment assignment) {
        Log.d(TAG, "View assignment details: " + assignment.getId());

        // For now, just show details in a snackbar
        // In future, could open a detail view or bottom sheet
        if (getView() != null) {
            String detailMessage = getString(R.string.assignment_details_format,
                                             assignment.getTitle() != null ? assignment.getTitle() : "Assignment",
                                             assignment.getTeamName() != null ? assignment.getTeamName() : "Unknown Team");

            Snackbar.make(getView(), detailMessage, Snackbar.LENGTH_LONG).show();
        }
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Perform assignment deletion after confirmation.
     */
    private void performDeleteAssignment(@NonNull UserScheduleAssignment assignment) {
        Log.d(TAG, "Performing delete for assignment: " + assignment.getId());

        mPatternService.deleteUserAssignmentWithValidation( assignment.getId(), mUser.getId() )
                .thenAccept(result -> {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            handleDeleteSuccess(assignment);
                        } else {
                            handleDeleteError(assignment, result.getErrorMessage());
                        }
                    });
                })
                .exceptionally(throwable -> {
                    if (getActivity() == null) return null;

                    getActivity().runOnUiThread(() ->
                                                        handleDeleteError(assignment, throwable.getMessage()));
                    return null;
                });
    }

    /**
     * Handle successful assignment deletion.
     */
    private void handleDeleteSuccess(@NonNull UserScheduleAssignment assignment) {
        // Remove from local list
        mAssignments.remove(assignment);
        mAdapter.notifyDataSetChanged();
        updateEmptyState();

        // Show success message
        if (getView() != null) {
            Snackbar.make(getView(),
                          R.string.assignment_deleted_successfully,
                          Snackbar.LENGTH_SHORT).show();
        }

        Log.d(TAG, "Assignment deleted successfully: " + assignment.getId());
    }

    /**
     * Handle assignment deletion error.
     */
    private void handleDeleteError(@NonNull UserScheduleAssignment assignment,
                                   @NonNull String errorMessage) {
        Log.e(TAG, "Error deleting assignment " + assignment.getId() + ": " + errorMessage);

        if (getView() != null) {
            Snackbar.make(getView(),
                          getString(R.string.error_deleting_assignment),
                          Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, v -> performDeleteAssignment(assignment))
                    .show();
        }
    }
}