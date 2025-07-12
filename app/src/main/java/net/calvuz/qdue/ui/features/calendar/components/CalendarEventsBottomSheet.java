package net.calvuz.qdue.ui.features.calendar.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.features.events.adapters.EventsAdapter;
import net.calvuz.qdue.ui.core.architecture.base.BaseEventsPreview;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * CalendarEventsBottomSheet - Bottom Sheet implementation for Calendar events preview
 * <p>
 * Features:
 * - Standard Bottom Sheet (non-modal) behavior
 * - Formatted date header with weekday
 * - RecyclerView with existing EventsAdapter
 * - Action buttons for add event and navigate to events activity
 * - Integrates with existing click handling system
 */
public class CalendarEventsBottomSheet extends BaseEventsPreview {

    private static final String TAG = "CalendarEventsBS";

    // UI Components
    private BottomSheetDialog mBottomSheetDialog;
    private View mBottomSheetView;
    private TextView mDateHeaderText;
    private TextView mEventsCountText;
    private RecyclerView mEventsRecyclerView;
    private MaterialButton mAddEventButton;
    private MaterialButton mNavigateToEventsButton;
    private View mEmptyStateView;
    private TextView mEmptyStateText;

    // Data and Adapter
    private EventsAdapter mEventsAdapter;
    private final DateTimeFormatter mDateFormatter;
    private final DateTimeFormatter mWeekdayFormatter;

    public CalendarEventsBottomSheet(@NonNull Context context) {
        super(context);

        // Initialize formatters for Italian locale
        mDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN);
        mWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ITALIAN);

        initializeBottomSheet();
        Log.d(TAG, "CalendarEventsBottomSheet initialized");
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize bottom sheet dialog and layout
     */
    private void initializeBottomSheet() {
        try {
            // Create bottom sheet dialog
            mBottomSheetDialog = new BottomSheetDialog(mContext);

            // Inflate layout
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mBottomSheetView = inflater.inflate(R.layout.bottom_sheet_events, null);

            // Setup dialog
            mBottomSheetDialog.setContentView(mBottomSheetView);

            // Configure behavior
            setupBottomSheetBehavior();

            // Initialize views
            initializeViews();

            // Setup event listeners
            setupEventListeners();

            Log.d(TAG, "Bottom sheet initialization completed");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing bottom sheet: " + e.getMessage());
        }
    }

    /**
     * Setup bottom sheet behavior (non-modal, standard)
     */
    private void setupBottomSheetBehavior() {
        View bottomSheet = mBottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            // Configure as standard bottom sheet
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setSkipCollapsed(false);
            behavior.setHideable(true);
            behavior.setPeekHeight(400); // Initial peek height

            // Add state change listener for debugging
            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    Log.d(TAG, "Bottom sheet state changed: " + getStateString(newState));

                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        // Auto-hide dialog when bottom sheet is hidden
                        hideEventsPreview();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // Optional: Add slide animations here
                }
            });

            Log.d(TAG, "Bottom sheet behavior configured");
        }
    }

    /**
     * Initialize all views from layout
     */
    private void initializeViews() {
        // Header views
        mDateHeaderText = mBottomSheetView.findViewById(R.id.tv_date_header);
        mEventsCountText = mBottomSheetView.findViewById(R.id.tv_events_count);

        // Events list
        mEventsRecyclerView = mBottomSheetView.findViewById(R.id.rv_events);

        // Action buttons
        mAddEventButton = mBottomSheetView.findViewById(R.id.btn_add_event);
        mNavigateToEventsButton = mBottomSheetView.findViewById(R.id.btn_navigate_to_events);

        // Empty state
        mEmptyStateView = mBottomSheetView.findViewById(R.id.empty_state_container);
        mEmptyStateText = mBottomSheetView.findViewById(R.id.tv_empty_state);

        // Setup RecyclerView
        if (mEventsRecyclerView != null) {
            mEventsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            mEventsRecyclerView.setHasFixedSize(false);
        }

        Log.d(TAG, "Views initialized - all components: " +
                (mDateHeaderText != null ? "✓" : "✗") + " " +
                (mEventsRecyclerView != null ? "✓" : "✗") + " " +
                (mAddEventButton != null ? "✓" : "✗"));
    }

    /**
     * Setup event listeners for interactive elements
     */
    private void setupEventListeners() {
        // Add Event button
        if (mAddEventButton != null) {
            mAddEventButton.setOnClickListener(v -> {
                Log.d(TAG, "Add event button clicked");
                if (mCurrentDate != null) {
                    onEventsGeneralAction(EventGeneralAction.ADD_EVENT, mCurrentDate);
                }
            });
        }

        // Navigate to Events button
        if (mNavigateToEventsButton != null) {
            mNavigateToEventsButton.setOnClickListener(v -> {
                Log.d(TAG, "Navigate to events button clicked");
                if (mCurrentDate != null) {
                    onEventsGeneralAction(EventGeneralAction.NAVIGATE_TO_EVENTS_ACTIVITY, mCurrentDate);
                }
            });
        }

        // Dialog dismiss listener
        mBottomSheetDialog.setOnDismissListener(dialog -> {
            Log.d(TAG, "Bottom sheet dialog dismissed");
            hideEventsPreview();
        });

        Log.d(TAG, "Event listeners setup completed");
    }

    // ==================== BASE EVENTS PREVIEW IMPLEMENTATION ====================

    @Override
    protected void showEventsPreviewImpl(LocalDate date, List<LocalEvent> events, View anchorView) {
        Log.d(TAG, "Showing events preview for " + date + " with " + events.size() + " events");

        try {
            // Update header with formatted date
            updateDateHeader(date, events.size());

            // Setup events list
            setupEventsList(events, date);

            // Show/hide empty state
            updateEmptyState(events.isEmpty(), date);

            // Show bottom sheet
            mBottomSheetDialog.show();

            Log.d(TAG, "Events preview shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing events preview: " + e.getMessage());
        }
    }

    @Override
    protected void hideEventsPreviewImpl() {
        Log.d(TAG, "Hiding events preview");

        try {
            if (mBottomSheetDialog.isShowing()) {
                mBottomSheetDialog.dismiss();
            }

            // Clear adapter to prevent memory leaks
            if (mEventsAdapter != null) {
                mEventsAdapter = null;
                if (mEventsRecyclerView != null) {
                    mEventsRecyclerView.setAdapter(null);
                }
            }

            Log.d(TAG, "Events preview hidden successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error hiding events preview: " + e.getMessage());
        }
    }

    @Override
    protected String getViewType() {
        return "CalendarView-BottomSheet";
    }

    public LocalDate getCurrentDate() {
        return mCurrentDate;
    }

    // ==================== UI UPDATE METHODS ====================

    /**
     * Update date header with formatted date and weekday
     */
    private void updateDateHeader(LocalDate date, int eventCount) {
        if (mDateHeaderText != null) {
            try {
                // Format: "Martedì, 15 Gennaio 2025"
                String weekday = date.format(mWeekdayFormatter);
                String formattedDate = date.format(mDateFormatter);

                // Capitalize first letter of weekday
                weekday = weekday.substring(0, 1).toUpperCase() + weekday.substring(1);

                String headerText = weekday + ", " + formattedDate;
                mDateHeaderText.setText(headerText);

                Log.d(TAG, "Date header updated: " + headerText);

            } catch (Exception e) {
                Log.e(TAG, "Error formatting date header: " + e.getMessage());
                // Fallback to simple date string
                mDateHeaderText.setText(date.toString());
            }
        }

        // Update events count
        if (mEventsCountText != null) {
            String countText = formatEventCount(eventCount);
            mEventsCountText.setText(countText);
            Log.d(TAG, "Events count updated: " + countText);
        }
    }

    /**
     * Setup events list with adapter
     */
    private void setupEventsList(List<LocalEvent> events, LocalDate date) {
        if (mEventsRecyclerView == null) {
            Log.w(TAG, "Events RecyclerView is null, cannot setup events list");
            return;
        }

        try {
            // Create adapter with click listener
            mEventsAdapter = new EventsAdapter(events, new EventsAdapter.OnEventClickListener() {
                @Override
                public void onEventClick(LocalEvent event) {
                    Log.d(TAG, "Event clicked: " + event.getTitle());

                    // Trigger view detail action - VIEW_DETAIL
                    onEventQuickAction(EventQuickAction.VIEW_DETAIL, event, date);
                }

                @Override
                public void onEventLongClick(LocalEvent event) {
                    Log.d(TAG, "Event long clicked: " + event.getTitle());

                    // Could show context menu or quick actions
                    showEventQuickActions(event, date);
                }
            });

            mEventsRecyclerView.setAdapter(mEventsAdapter);

            Log.d(TAG, "Events list setup completed with " + events.size() + " events");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up events list: " + e.getMessage());
        }
    }

    /**
     * Update empty state visibility and message
     */
    private void updateEmptyState(boolean isEmpty, LocalDate date) {
        if (mEmptyStateView == null) return;

        if (isEmpty) {
            // Show empty state
            mEmptyStateView.setVisibility(View.VISIBLE);
            if (mEventsRecyclerView != null) {
                mEventsRecyclerView.setVisibility(View.GONE);
            }

            // Update empty state message
            if (mEmptyStateText != null) {
                String emptyMessage = "Nessun evento per " + date.format(mDateFormatter);
                mEmptyStateText.setText(emptyMessage);
            }

            Log.d(TAG, "Empty state shown for date: " + date);

        } else {
            // Hide empty state
            mEmptyStateView.setVisibility(View.GONE);
            if (mEventsRecyclerView != null) {
                mEventsRecyclerView.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "Empty state hidden - showing " +
                    (mEventsAdapter != null ? mEventsAdapter.getItemCount() : 0) + " events");
        }
    }

    // ==================== EVENT QUICK ACTIONS ====================

    /**
     * Show quick actions for an event (context menu style)
     */
    private void showEventQuickActions(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Showing quick actions for event: " + event.getTitle());

        // TODO: Implement quick actions popup/dialog
        // For now, just log available actions
        Log.d(TAG, "Available actions: EDIT, DELETE, DUPLICATE, TOGGLE_COMPLETE");

        // Example: Show simple dialog with actions
        if (mContext instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) mContext;

            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle(event.getTitle())
//                    .setItems(new String[]{"Modifica", "Elimina", "Duplica", "Completa"}, (dialog, which) -> {
                    .setItems(new String[]{"Modifica", "Elimina"}, (dialog, which) -> {
                        switch (which) {
                            case 0: // Edit
                                onEventQuickAction(EventQuickAction.EDIT, event, date);
                                break;
                            case 1: // Delete
                                onEventQuickAction(EventQuickAction.DELETE, event, date);
                                break;
                            case 2: // Duplicate
                                onEventQuickAction(EventQuickAction.DUPLICATE, event, date);
                                break;
                            case 3: // Toggle Complete
                                onEventQuickAction(EventQuickAction.TOGGLE_COMPLETE, event, date);
                                break;
                        }
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get human-readable state string for debugging
     */
    private String getStateString(int state) {
        switch (state) {
            case BottomSheetBehavior.STATE_COLLAPSED:
                return "COLLAPSED";
            case BottomSheetBehavior.STATE_EXPANDED:
                return "EXPANDED";
            case BottomSheetBehavior.STATE_HIDDEN:
                return "HIDDEN";
            case BottomSheetBehavior.STATE_DRAGGING:
                return "DRAGGING";
            case BottomSheetBehavior.STATE_SETTLING:
                return "SETTLING";
            case BottomSheetBehavior.STATE_HALF_EXPANDED:
                return "HALF_EXPANDED";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    public void onPause() {
        super.onPause();

        // Hide bottom sheet on pause to prevent window leaks
        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
            mBottomSheetDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cleanup resources
        if (mBottomSheetDialog != null) {
            mBottomSheetDialog.dismiss();
            mBottomSheetDialog = null;
        }

        if (mEventsAdapter != null) {
            mEventsAdapter = null;
        }

        Log.d(TAG, "CalendarEventsBottomSheet destroyed");
    }

    // ==================== DEBUG METHODS ====================

    @Override
    public void debugState() {
        super.debugState();

        Log.d(TAG, "=== CALENDAR BOTTOM SHEET DEBUG ===");
        Log.d(TAG, "Bottom Sheet Dialog: " + (mBottomSheetDialog != null ? "initialized" : "null"));
        Log.d(TAG, "Bottom Sheet Showing: " + (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()));
        Log.d(TAG, "Events Adapter: " + (mEventsAdapter != null ? "active" : "null"));
        Log.d(TAG, "Events Count: " + (mEventsAdapter != null ? mEventsAdapter.getItemCount() : 0));
        Log.d(TAG, "Date Formatter: " + mDateFormatter.toString());
        Log.d(TAG, "=== END CALENDAR BOTTOM SHEET DEBUG ===");
    }
}