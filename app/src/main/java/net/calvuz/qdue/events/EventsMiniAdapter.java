package net.calvuz.qdue.events;

/**
 * COMPLETE EventsMiniAdapter Implementation
 *
 * Mini adapter for displaying events within day rows in DaysList view
 */


import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.models.EventPriority;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini adapter for events in day rows
 * Designed to be lightweight and efficient for embedding in other RecyclerViews
 */
public class EventsMiniAdapter extends RecyclerView.Adapter<EventsMiniAdapter.EventMiniViewHolder> {

    private static final String TAG = "EventsMiniAdapter";

    private final Context mContext;
    private final List<LocalEvent> mEvents;
    private OnEventClickListener mClickListener;

    // ==================== INTERFACES ====================

    /**
     * Interface for handling event clicks
     */
    public interface OnEventClickListener {
        void onEventClick(LocalEvent event);
        default void onEventLongClick(LocalEvent event) {
            // Optional long click handling
        }
    }

    // ==================== CONSTRUCTOR ====================

    public EventsMiniAdapter(Context context, List<LocalEvent> events, OnEventClickListener listener) {
        mContext = context;
        mEvents = events != null ? new ArrayList<>(events) : new ArrayList<>();
        mClickListener = listener;
    }

    // ==================== RECYCLERVIEW METHODS ====================

    @NonNull
    @Override
    public EventMiniViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_event_mini, parent, false);
        return new EventMiniViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventMiniViewHolder holder, int position) {
        LocalEvent event = mEvents.get(position);
        bindEventToHolder(holder, event);
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < mEvents.size()) {
            return mEvents.get(position).getId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ==================== BINDING METHODS ====================

    /**
     * Bind event data to view holder
     */
    private void bindEventToHolder(EventMiniViewHolder holder, LocalEvent event) {
        // Event title
        holder.tvEventTitle.setText(event.getTitle());

        // Event time (if available)
        setupEventTime(holder, event);

        // Event type indicator color
        setupEventIndicator(holder, event);

        // Priority indicator
        setupPriorityIndicator(holder, event);

        // Click listeners
        setupClickListeners(holder, event);

        // Accessibility
        setupAccessibility(holder, event);
    }

    /**
     * Setup event time display
     */
    private void setupEventTime(EventMiniViewHolder holder, LocalEvent event) {
        if (event.hasTime() && !event.isAllDay()) {
            holder.tvEventTime.setText(event.getTimeString());
            holder.tvEventTime.setVisibility(View.VISIBLE);
        } else if (event.isAllDay()) {
            holder.tvEventTime.setText("Tutto il giorno");
            holder.tvEventTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvEventTime.setVisibility(View.GONE);
        }
    }

    /**
     * Setup event type indicator color
     */
    private void setupEventIndicator(EventMiniViewHolder holder, LocalEvent event) {
        int eventColor = event.getEventType().getColor();
        holder.vEventIndicator.setBackgroundTintList(ColorStateList.valueOf(eventColor));
    }

    /**
     * Setup priority indicator
     */
    private void setupPriorityIndicator(EventMiniViewHolder holder, LocalEvent event) {
        EventPriority priority = event.getPriority();

        if (priority == EventPriority.HIGH || priority == EventPriority.URGENT) {
            holder.ivEventStatus.setVisibility(View.VISIBLE);

            int priorityColor = priority == EventPriority.URGENT ?
                    android.graphics.Color.RED : android.graphics.Color.parseColor("#FF8C00"); // Dark orange

            holder.ivEventStatus.setImageTintList(ColorStateList.valueOf(priorityColor));

            // Set appropriate icon based on priority
            if (priority == EventPriority.URGENT) {
                holder.ivEventStatus.setImageResource(R.drawable.ic_event_priority);
            } else {
                holder.ivEventStatus.setImageResource(R.drawable.ic_event_priority);
            }
        } else {
            holder.ivEventStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners(EventMiniViewHolder holder, LocalEvent event) {
        // Regular click
        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onEventClick(event);
            }
        });

        // Long click (optional)
        holder.itemView.setOnLongClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onEventLongClick(event);
                return true;
            }
            return false;
        });
    }

    /**
     * Setup accessibility
     */
    private void setupAccessibility(EventMiniViewHolder holder, LocalEvent event) {
        StringBuilder description = new StringBuilder();
        description.append("Evento: ").append(event.getTitle());

        if (event.hasTime()) {
            description.append(", ").append(event.getTimeString());
        }

        description.append(", Tipo: ").append(event.getEventType().getDisplayName());

        if (event.getPriority() != EventPriority.NORMAL) {
            description.append(", Priorità: ").append(event.getPriority().getDisplayName());
        }

        holder.itemView.setContentDescription(description.toString());
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Update events list
     */
    public void updateEvents(List<LocalEvent> newEvents) {
        mEvents.clear();
        if (newEvents != null) {
            mEvents.addAll(newEvents);
        }
        notifyDataSetChanged();
    }

    /**
     * Add single event
     */
    public void addEvent(LocalEvent event) {
        if (event != null) {
            mEvents.add(event);
            notifyItemInserted(mEvents.size() - 1);
        }
    }

    /**
     * Remove event at position
     */
    public void removeEvent(int position) {
        if (position >= 0 && position < mEvents.size()) {
            mEvents.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Set click listener
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        mClickListener = listener;
    }

    /**
     * Get event at position
     */
    public LocalEvent getEvent(int position) {
        if (position >= 0 && position < mEvents.size()) {
            return mEvents.get(position);
        }
        return null;
    }

    /**
     * Check if adapter is empty
     */
    public boolean isEmpty() {
        return mEvents.isEmpty();
    }

    // ==================== VIEW HOLDER ====================

    /**
     * ViewHolder for mini event items
     */
    public static class EventMiniViewHolder extends RecyclerView.ViewHolder {

        // Views
        public final TextView tvEventTitle;
        public final TextView tvEventTime;
        public final View vEventIndicator;
        public final ImageView ivEventStatus;
        public final LinearLayout llEventContainer;

        public EventMiniViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find views
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventTime = itemView.findViewById(R.id.tv_event_time);
            vEventIndicator = itemView.findViewById(R.id.v_event_indicator);
            ivEventStatus = itemView.findViewById(R.id.iv_event_status);

            // Find container (optional, for animations)
            llEventContainer = itemView instanceof LinearLayout ?
                    (LinearLayout) itemView : null;

            // Setup minimum touch target for accessibility
            itemView.setMinimumHeight(
                    (int) (32 * itemView.getContext().getResources().getDisplayMetrics().density)
            );

            // Enable ripple effect
            itemView.setClickable(true);
            itemView.setFocusable(true);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create simple adapter for single event (utility method)
     */
    public static EventsMiniAdapter createSingleEventAdapter(Context context, LocalEvent event,
                                                             OnEventClickListener listener) {
        List<LocalEvent> singleEventList = new ArrayList<>();
        singleEventList.add(event);
        return new EventsMiniAdapter(context, singleEventList, listener);
    }

    /**
     * Create empty adapter (utility method)
     */
    public static EventsMiniAdapter createEmptyAdapter(Context context, OnEventClickListener listener) {
        return new EventsMiniAdapter(context, new ArrayList<>(), listener);
    }
}