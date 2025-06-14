package net.calvuz.qdue.ui.events;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RecyclerView Adapter for displaying events in EventsActivity
 *
 * Features:
 * - Display event details with proper formatting
 * - Color-coded event types and priorities
 * - Click handling for view/edit events
 * - Context menu for additional actions
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private static final String TAG = "EventsAdapter";

    private final List<LocalEvent> mEvents;
    private final OnEventClickListener mClickListener;
    private final LayoutInflater mInflater;

    // Time formatters
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");

    /**
     * Interface for handling event clicks
     */
    public interface OnEventClickListener {
        void onEventClick(LocalEvent event);
        void onEventLongClick(LocalEvent event);
    }

    public EventsAdapter(List<LocalEvent> events, OnEventClickListener clickListener) {
        this.mEvents = events;
        this.mClickListener = clickListener;
        this.mInflater = LayoutInflater.from(((Context) clickListener));
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        LocalEvent event = mEvents.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    /**
     * ViewHolder for individual event items
     */
    public class EventViewHolder extends RecyclerView.ViewHolder {

        // Views
        private final View mTypeIndicator;
        private final TextView mTitleText;
        private final TextView mTimeText;
        private final TextView mDescriptionText;
        private final TextView mLocationText;
        private final ImageView mLocationIcon;
        private final Chip mPriorityChip;
        private final ImageButton mMenuButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find views
            mTypeIndicator = itemView.findViewById(R.id.indicator_event_type);
            mTitleText = itemView.findViewById(R.id.text_event_title);
            mTimeText = itemView.findViewById(R.id.text_event_time);
            mDescriptionText = itemView.findViewById(R.id.text_event_description);
            mLocationText = itemView.findViewById(R.id.text_event_location);
            mLocationIcon = itemView.findViewById(R.id.icon_location);
            mPriorityChip = itemView.findViewById(R.id.chip_event_priority);
            mMenuButton = itemView.findViewById(R.id.btn_event_menu);

            // Setup click listeners
            itemView.setOnClickListener(v -> {
                if (mClickListener != null) {
                    LocalEvent event = mEvents.get(getAdapterPosition());
                    mClickListener.onEventClick(event);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (mClickListener != null) {
                    LocalEvent event = mEvents.get(getAdapterPosition());
                    mClickListener.onEventLongClick(event);
                    return true;
                }
                return false;
            });

            mMenuButton.setOnClickListener(v -> {
                if (mClickListener != null) {
                    LocalEvent event = mEvents.get(getAdapterPosition());
                    mClickListener.onEventLongClick(event);
                }
            });
        }

        /**
         * Bind event data to views
         */
        public void bind(LocalEvent event) {
            // Basic info
            mTitleText.setText(event.getTitle());

            // Time display
            if (event.isAllDay()) {
                mTimeText.setText("All Day");
            } else if (event.hasTime()) {
                String timeText = event.getStartTime().format(TIME_FORMATTER);
                if (event.getEndTime() != null &&
                        !event.getStartTime().toLocalDate().equals(event.getEndTime().toLocalDate())) {
                    // Multi-day event
                    timeText += " - " + event.getEndTime().format(DATE_FORMATTER);
                } else if (event.getEndTime() != null) {
                    // Same day event
                    timeText += " - " + event.getEndTime().format(TIME_FORMATTER);
                }
                mTimeText.setText(timeText);
            } else {
                mTimeText.setText("");
            }

            // Description
            if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
                mDescriptionText.setText(event.getDescription());
                mDescriptionText.setVisibility(View.VISIBLE);
            } else {
                mDescriptionText.setVisibility(View.GONE);
            }

            // Location
            if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
                mLocationText.setText(event.getLocation());
                mLocationText.setVisibility(View.VISIBLE);
                mLocationIcon.setVisibility(View.VISIBLE);
            } else {
                mLocationText.setVisibility(View.GONE);
                mLocationIcon.setVisibility(View.GONE);
            }

            // Event type indicator color
            int typeColor = getEventTypeColor(event.getEventType());
            mTypeIndicator.setBackgroundColor(typeColor);

            // Priority chip
            setupPriorityChip(event.getPriority());
        }

        /**
         * Setup priority chip appearance
         */
        private void setupPriorityChip(EventPriority priority) {
            if (priority == null) {
                mPriorityChip.setVisibility(View.GONE);
                return;
            }

            mPriorityChip.setVisibility(View.VISIBLE);

            switch (priority) {
                case HIGH:
                    mPriorityChip.setText("HIGH");
                    mPriorityChip.setChipBackgroundColorResource(R.color.priority_high);
                    mPriorityChip.setTextColor(Color.WHITE);
                    break;
                case NORMAL:
                    mPriorityChip.setText("NORMAL");
                    mPriorityChip.setChipBackgroundColorResource(R.color.priority_normal);
                    mPriorityChip.setTextColor(Color.BLACK);
                    break;
                case LOW:
                    mPriorityChip.setText("LOW");
                    mPriorityChip.setChipBackgroundColorResource(R.color.priority_low);
                    mPriorityChip.setTextColor(Color.BLACK);
                    break;
                default:
                    mPriorityChip.setVisibility(View.GONE);
                    break;
            }
        }

        /**
         * Get color for event type indicator
         */
        private int getEventTypeColor(EventType eventType) {
            Context context = itemView.getContext();

            if (eventType == null) {
                return ContextCompat.getColor(context, R.color.event_type_general);
            }

            switch (eventType) {
                case STOP_PLANNED:
                    return ContextCompat.getColor(context, R.color.event_type_stop_planned);
                case STOP_UNPLANNED:
                    return ContextCompat.getColor(context, R.color.event_type_stop_unplanned);
                case STOP_SHORTAGE:
                    return ContextCompat.getColor(context, R.color.event_type_stop_shortage);
                case MAINTENANCE:
                    return ContextCompat.getColor(context, R.color.event_type_maintenance);
                case MEETING:
                    return ContextCompat.getColor(context, R.color.event_type_meeting);
                case TRAINING:
                    return ContextCompat.getColor(context, R.color.event_type_training);
                case GENERAL:
                default:
                    return ContextCompat.getColor(context, R.color.event_type_general);
            }
        }
    }

    /**
     * Update the events list and refresh the adapter
     */
    public void updateEvents(List<LocalEvent> newEvents) {
        mEvents.clear();
        mEvents.addAll(newEvents);
        notifyDataSetChanged();
    }

    /**
     * Add a new event to the list
     */
    public void addEvent(LocalEvent event) {
        mEvents.add(event);
        notifyItemInserted(mEvents.size() - 1);
    }

    /**
     * Remove an event from the list
     */
    public void removeEvent(int position) {
        if (position >= 0 && position < mEvents.size()) {
            mEvents.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Remove an event by object reference
     */
    public void removeEvent(LocalEvent event) {
        int position = mEvents.indexOf(event);
        if (position != -1) {
            removeEvent(position);
        }
    }

    /**
     * Clear all events
     */
    public void clearEvents() {
        int size = mEvents.size();
        mEvents.clear();
        notifyItemRangeRemoved(0, size);
    }
}