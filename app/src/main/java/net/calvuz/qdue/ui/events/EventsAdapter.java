package net.calvuz.qdue.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * EventsAdapter - RecyclerView adapter for events list with navigation support
 *
 * Features:
 * - Click handling for navigation to event details
 * - Long click for context menu actions
 * - Material Design item layout
 * - Status indication (past/current/upcoming)
 * - Duration and location display
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private static final String TAG = "EventsAdapter";

    // Date formatters
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Data and listener
    private List<LocalEvent> mEvents;
    private OnEventClickListener mClickListener;

    /**
     * Interface for event click handling
     */
    public interface OnEventClickListener {
        /**
         * Called when an event item is clicked
         * @param event The clicked event
         */
        void onEventClick(LocalEvent event);

        /**
         * Called when an event item is long clicked
         * @param event The long clicked event
         */
        void onEventLongClick(LocalEvent event);
    }

    /**
     * Constructor
     * @param events List of events to display
     * @param clickListener Listener for click events
     */
    public EventsAdapter(List<LocalEvent> events, OnEventClickListener clickListener) {
        this.mEvents = events;
        this.mClickListener = clickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        LocalEvent event = mEvents.get(position);
        holder.bind(event);

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

    /**
     * ViewHolder for event items
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitleView;
        private TextView mDescriptionView;
        private TextView mTimeView;
        private TextView mLocationView;
        private TextView mStatusView;
        private View mStatusIndicator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitleView = itemView.findViewById(R.id.tv_event_title);
            mDescriptionView = itemView.findViewById(R.id.tv_event_description);
            mTimeView = itemView.findViewById(R.id.tv_event_time);
            mLocationView = itemView.findViewById(R.id.tv_event_location);
            mStatusView = itemView.findViewById(R.id.tv_event_status);
            mStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
        }

        /**
         * Bind event data to views
         */
        public void bind(LocalEvent event) {
            if (event == null) return;

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
         * Format time display based on event type and duration
         */
        private void formatTimeDisplay(LocalEvent event) {
            if (event.getStartTime() == null) {
                mTimeView.setText("Orario non specificato");
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
                timeText.append(" (Tutto il giorno)");
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
                mStatusView.setText("Stato sconosciuto");
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_unknown));
                return;
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            if (now.isBefore(event.getStartTime())) {
                // Upcoming event
                mStatusView.setText("In programma");
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_upcoming));
            } else if (now.isAfter(event.getEndTime())) {
                // Past event
                mStatusView.setText("Terminato");
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_past));
            } else {
                // Current event
                mStatusView.setText("In corso");
                mStatusIndicator.setBackgroundColor(getColorFromResource(R.color.status_current));
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