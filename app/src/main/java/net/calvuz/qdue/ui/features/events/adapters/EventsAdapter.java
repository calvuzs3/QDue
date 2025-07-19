package net.calvuz.qdue.ui.features.events.adapters;

import static net.calvuz.qdue.ui.features.events.presentation.EventsListFragment.DEFAULT_SELECTION_MODE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.backup.CoreBackupManager;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.enums.SelectionMode;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.presentation.EventsListFragment;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EventsAdapter - RecyclerView adapter for events list with navigation support
 * <p>
 * Features:
 * - Click handling for navigation to event details
 * - Long click for context menu actions
 * - Material Design item layout
 * - Status indication (past/current/upcoming)
 * - Duration and location display
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private static final String TAG = "EventsAdapter";

    private Context mContext;
    private EventsService mEventsService;
    private CoreBackupManager mBackupManager;

    // Date formatters
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Data and listener
    private List<LocalEvent> mEvents;
    private OnEventClickListener mClickListener;

    // Enhanced selection mode
    private SelectionMode mSelectionMode = SelectionMode.NONE;
    private Set<String> mSelectedEventIds = new HashSet<>();

    /**
     * Interface for event click handling
     */
    public interface OnEventClickListener {
        /**
         * Called when an event item is clicked
         *
         * @param event The clicked event
         */
        void onEventClick(LocalEvent event);

        /**
         * Called when an event item is long clicked
         *
         * @param event The long clicked event
         */
        void onEventLongClick(LocalEvent event);

        /**
         * Called when selection state changes
         *
         * @param selectedCount  Number of selected items
         * @param selectedEvents Set of selected event IDs
         */
        default void onSelectionChanged(int selectedCount, Set<String> selectedEvents) {
            // Optional implementation
        }
    }

    /**
     * Constructor DI module ready
     *
     * @param context       Application context
     * @param eventsService Events service instance
     * @param backupManager Backup manager instance
     */
    public EventsAdapter(Context context, EventsService eventsService, CoreBackupManager backupManager) {
        mContext = context;
        mEventsService = eventsService;
        mBackupManager = backupManager;
        mEvents = null;
        mClickListener = null;
    }

    /**
     * Constructor
     *
     * @param events        List of events to display
     * @param clickListener Listener for click events
     */
    public EventsAdapter(List<LocalEvent> events, OnEventClickListener clickListener) {
        this.mEvents = events;
        this.mClickListener = clickListener;
        mContext = null;
        mEventsService = null;
        mBackupManager = null;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    public void onDestroy() {
        mEvents = null;
        mClickListener = null;
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        LocalEvent event = mEvents.get(position);
        holder.bind(event, mSelectionMode, isEventSelected(event.getId()));

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onEventClick(event);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onEventLongClick(event);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mEvents != null ? mEvents.size() : 0;
    }

    /**
     * Update events list and notify adapter
     */
    public void updateEvents(List<LocalEvent> newEvents) {
        this.mEvents = newEvents;
        notifyDataSetChanged();
    }

    // ==================== SELECTION METHODS ====================

    /**
     * Set selection mode on/off
     *
     * @param selectionMode true to enable selection mode
     */
    public void setSelectionMode(SelectionMode selectionMode) {
        boolean wasInSelectionMode = (mSelectionMode != SelectionMode.NONE);
        boolean isEnteringSelectionMode = (selectionMode != SelectionMode.NONE);

        this.mSelectionMode = selectionMode;

        // If exiting selection mode, clear selections
        if (wasInSelectionMode && !isEnteringSelectionMode) {
            mSelectedEventIds.clear();
        }

        // Notify all items to update their visual state
        notifyDataSetChanged();

        Log.d(TAG, MessageFormat.format("Selection mode changed to: {0} (selected: {1})", selectionMode, mSelectedEventIds.size()));
    }

    /**
     * Backward compatibility method
     */
    @Deprecated
    public void setSelectionMode(boolean selectionMode) {
        setSelectionMode(selectionMode ? EventsListFragment.DEFAULT_SELECTION_MODE : SelectionMode.NONE);
    }

    /**
     * Get current selection mode
     */
    public SelectionMode getSelectionMode() {
        return mSelectionMode;
    }

    /**
     * Check if adapter is in selection mode
     * <p>
     *
     * @return true if in selection mode
     */
    public boolean isInSelectionMode() {
        return mSelectionMode != SelectionMode.NONE;
    }

    /**
     * Toggle selection for an event
     * <p>
     *
     * @param eventId Event ID to toggle
     */
    public void toggleEventSelection(String eventId) {
        if (eventId == null || mSelectionMode == SelectionMode.NONE) {
            return;
        }

        boolean wasSelected = mSelectedEventIds.contains(eventId);

        if (mSelectionMode == SelectionMode.SINGLE) {
            // Single selection: clear all others and select this one (if not already selected)
            mSelectedEventIds.clear();
            if (!wasSelected) {
                mSelectedEventIds.add(eventId);
            }
        } else if (mSelectionMode == SelectionMode.MULTIPLE) {
            // Multiple selection: toggle this item
            if (wasSelected) {
                mSelectedEventIds.remove(eventId);
            } else {
                mSelectedEventIds.add(eventId);
            }
        }

        // Notify selection change
        notifyDataSetChanged();

        if (mClickListener != null) {
            mClickListener.onSelectionChanged(mSelectedEventIds.size(), new HashSet<>(mSelectedEventIds));
        }

        Log.d(TAG, "Event " + eventId + " selection toggled. Total selected: " + mSelectedEventIds.size());
    }

    /**
     * Update selected event IDs
     *
     * @param selectedIds Set of selected event IDs
     */
    public void updateSelections(Set<String> selectedIds) {
        this.mSelectedEventIds.clear();
        if (selectedIds != null) {
            this.mSelectedEventIds.addAll(selectedIds);
        }
        notifyDataSetChanged(); // Update all items to show/hide selection indicators

        if (mClickListener != null) {
            mClickListener.onSelectionChanged(mSelectedEventIds.size(), new HashSet<>(mSelectedEventIds));
        }
    }

    /**
     * Clear all selections
     */
    public void clearSelections() {
        mSelectedEventIds.clear();
        notifyDataSetChanged();

        if (mClickListener != null) {
            mClickListener.onSelectionChanged(0, new HashSet<>());
        }

        Log.d(TAG, "All selections cleared");
    }

    /**
     * Check if an event is selected
     *
     * @param eventId Event ID to check
     * @return true if selected
     */
    public boolean isEventSelected(String eventId) {
        return mSelectedEventIds.contains(eventId);
    }

    /**
     * Get selected event IDs
     *
     * @return Set of selected event IDs
     */
    public Set<String> getSelectedEventIds() {
        return new HashSet<>(mSelectedEventIds);
    }

    /**
     * Get count of selected items
     */
    public int getSelectedItemCount() {
        return mSelectedEventIds.size();
    }

    /**
     * Select all events (only in MULTIPLE mode)
     */
    public void selectAll() {
        if (mSelectionMode != SelectionMode.MULTIPLE || mEvents == null) {
            return;
        }

        mSelectedEventIds.clear();
        for (LocalEvent event : mEvents) {
            if (event != null && event.getId() != null) {
                mSelectedEventIds.add(event.getId());
            }
        }

        notifyDataSetChanged();

        if (mClickListener != null) {
            mClickListener.onSelectionChanged(mSelectedEventIds.size(), new HashSet<>(mSelectedEventIds));
        }

        Log.d(TAG, "All events selected: " + mSelectedEventIds.size());
    }

    // ==================== VIEW HOLDER CLASS ====================

    /**
     * ViewHolder for event items
     * <p>
     * TODO: implement selection mode (indicator goes with cardview)
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTitleView;
        private final TextView mDescriptionView;
        private final TextView mTimeView;
        private final TextView mLocationView;
        private final TextView mStatusView;
        private final View mStatusIndicator;

        // Selection components
        private final MaterialCardView mCardView;
        private final ImageView mActionIndicator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitleView = itemView.findViewById(R.id.tv_event_title);
            mDescriptionView = itemView.findViewById(R.id.tv_event_description);
            mTimeView = itemView.findViewById(R.id.tv_event_time);
            mLocationView = itemView.findViewById(R.id.tv_event_location);
            mStatusView = itemView.findViewById(R.id.tv_event_status);
            mStatusIndicator = itemView.findViewById(R.id.view_status_indicator);

            // Selection components
            mCardView = (MaterialCardView) itemView;
            mActionIndicator = itemView.findViewById(R.id.iv_action_indicator);
        }

        /**
         * Bind event data to views
         */
        public void bind(LocalEvent event,  SelectionMode selectionMode, boolean isSelected) {
            if (event == null) return;

            // Bind basic event data
            bindEventData(event);

            // Update selection state
            updateSelectionState(selectionMode, isSelected);
        }

        /**
         * Bind basic event data (unchanged from original)
         */
        private void bindEventData(LocalEvent event) {
            // Title
            mTitleView.setText(event.getTitle());

            // Description (with null check)
            if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
                mDescriptionView.setText(event.getDescription());
                mDescriptionView.setVisibility(View.VISIBLE);
            } else {
                mDescriptionView.setVisibility(View.GONE);
            }

            // Time formatting
            formatTimeDisplay(event);

            // Location (with null check)
            if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
                mLocationView.setText(event.getLocation());
                mLocationView.setVisibility(View.VISIBLE);
            } else {
                mLocationView.setVisibility(View.GONE);
            }

            // Status and indicator
            setEventStatus(event);
        }

        /**
         * Update selection visual state using MaterialCardView native properties
         */
        private void updateSelectionState(SelectionMode selectionMode, boolean isSelected) {
            // Update card checkable state and checked status
            boolean isCheckable = (selectionMode != SelectionMode.NONE);
//            mCardView.setCheckable(isCheckable);
            mCardView.setChecked(isSelected);

            // Update action indicator based on mode
            if (isCheckable) {
                // In selection mode, hide the info indicator (checkmark will show automatically)
                mActionIndicator.setVisibility(View.INVISIBLE);
            } else {
                // Normal mode, show info indicator
                mActionIndicator.setVisibility(View.VISIBLE);
                //mActionIndicator.setImageResource(android.R.drawable.ic_menu_info_details);
            }

            // Optional: Adjust card elevation for better visual feedback
            if (isSelected) {
                mCardView.setCardElevation(6f);
            } else {
                mCardView.setCardElevation(2f);
            }
        }

        /**
         * Format time display based on event type and duration
         */
        private void formatTimeDisplay(LocalEvent event) {
            if (event.getStartTime() == null) {
                mTimeView.setText(R.string.text_hour_not_specified);
                return;
            }

            StringBuilder timeText = new StringBuilder();

            if (event.isAllDay()) {
                // All day event
                timeText.append(event.getStartTime().format(DATE_FORMATTER));
                if (event.getEndTime() != null &&
                        !event.getStartTime().toLocalDate().equals(event.getEndTime().toLocalDate())) {
                    timeText.append(" - ").append(event.getEndTime().format(DATE_FORMATTER));
                }
                timeText.append(MessageFormat.format(" ({0})", R.string.text_all_day));
            } else {
                // Timed event
                timeText.append(event.getStartTime().format(DATE_TIME_FORMATTER));
                if (event.getEndTime() != null) {
                    if (event.getStartTime().toLocalDate().equals(event.getEndTime().toLocalDate())) {
                        // Same day - show only time for end
                        timeText.append(" - ").append(event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    } else {
                        // Different day - show full date/time for end
                        timeText.append(" - ").append(event.getEndTime().format(DATE_TIME_FORMATTER));
                    }
                }
            }

            mTimeView.setText(timeText.toString());
        }

        /**
         * Set event status and color indicator
         */
        private void setEventStatus(LocalEvent event) {
            if (event.getStartTime() == null || event.getEndTime() == null) {
                mStatusView.setText(R.string.text_unknown_state);
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_unknown));
                return;
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            if (now.isBefore(event.getStartTime())) {
                // Upcoming event - use priority color
                mStatusView.setText(R.string.text_scheduled_on);
                mStatusIndicator.setBackgroundColor(getPriorityColorForEvent(event, R.color.status_upcoming));
            } else if (now.isAfter(event.getEndTime())) {
                // Past event - use standard gray color
                mStatusView.setText(R.string.text_finished);
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_past));
            } else {
                // Current event - use priority color
                mStatusView.setText(R.string.text_in_progress);
                mStatusIndicator.setBackgroundColor(getPriorityColorForEvent(event, R.color.status_current));
            }
        }

        /**
         * Get priority-based color for current and upcoming events
         * Falls back to default status color if priority is null or LOW
         */
        private int getPriorityColorForEvent(LocalEvent event, int defaultColorRes) {
            if (event.getPriority() == null) {
                return getColorFromResource(defaultColorRes);
            }

            switch (event.getPriority()) {
                case URGENT:
                    return event.getPriority().getColor(); // Red
                case HIGH:
                    return event.getPriority().getColor(); // Dark Gray
                case NORMAL:
                    return event.getPriority().getColor(); // Gray
                case LOW:
                default:
                    // For LOW priority, use the default status color (blue for upcoming, green for current)
                    return getColorFromResource(defaultColorRes);
            }
        }

        /**
         * Get color from resources with fallback
         */
        private int getColorFromResource(int colorResId) {
            try {
                return itemView.getContext().getColor(colorResId);
            } catch (Exception e) {
                // Fallback colors if resources not found
                if (colorResId == R.color.status_upcoming) {
                    return 0xFF2196F3; // Blue
                } else if (colorResId == R.color.status_current) {
                    return 0xFF4CAF50; // Green
                } else if (colorResId == R.color.status_past) {
                    return 0xFF757575; // Gray
                } else {
                    return 0xFF9E9E9E; // Default gray
                }
            }
        }
    }
}