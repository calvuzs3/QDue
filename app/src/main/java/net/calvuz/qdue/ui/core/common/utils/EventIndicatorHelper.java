package net.calvuz.qdue.ui.core.common.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;

import java.util.List;

/**
 * Helper class for managing event indicators in calendar and days list views.
 * Handles color mapping, priority logic, and visual setup for event type indicators and priority badges.
 * <p>
 * Key responsibilities:
 * - Determine dominant event type and priority for a given day
 * - Map event types to colors and priorities to badge styles
 * - Setup visual indicators in ViewHolders
 * - Provide utility methods for event visual representation
 */
public class EventIndicatorHelper {

    private static final String TAG = "EventIndicatorHelper";

    // Context for resource access
    private final Context mContext;

    /**
     * Constructor
     * @param context Context for accessing resources
     */
    public EventIndicatorHelper(Context context) {
        this.mContext = context;
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * STEP 1: Simple setup - only shows event count.
     * @param eventsIndicator TextView to show event count
     * @param events List of events for the day
     */
    public void setupSimpleEventIndicator(TextView eventsIndicator, List<LocalEvent> events) {
        if (eventsIndicator == null) return;

        if (events == null || events.isEmpty()) {
            eventsIndicator.setVisibility(View.GONE);
            return;
        }

        // Show simple count
        eventsIndicator.setVisibility(View.VISIBLE);
        int count = events.size();
        eventsIndicator.setText(count == 1 ? "1 evento" : count + " eventi");

        // Set color based on highest priority
        int color = getHighestPriorityColor(events);
        eventsIndicator.setTextColor(color);
    }

    /**
     * Get color for highest priority event in the list.
     */
    public int getHighestPriorityColor(List<LocalEvent> events) {
        EventPriority highestPriority = EventPriority.LOW;

        for (LocalEvent event : events) {
            EventPriority priority = event.getPriority();
            if (priority != null && getPriorityScore(priority) > getPriorityScore(highestPriority)) {
                highestPriority = priority;
            }
        }

        return getPriorityColor(highestPriority);
    }

    /**
     * Get color for priority level.
     */
    private int getPriorityColor(EventPriority priority) {
        if (priority == null) {
            return ContextCompat.getColor(mContext, R.color.priority_low);
        }

        switch (priority) {
            case URGENT:
            case HIGH:
                return ContextCompat.getColor(mContext, R.color.priority_high);
            case NORMAL:
                return ContextCompat.getColor(mContext, R.color.priority_normal);
            case LOW:
            default:
                return ContextCompat.getColor(mContext, R.color.priority_low);
        }
    }

    /**
     * Setup event indicators for a day cell/row.
     * This is the main method called from adapters.
     *
     * @param typeIndicator The colored bar View for event type
     * @param priorityBadge The badge View for priority
     * @param events List of events for this day
     */
    public void setupEventIndicators(View typeIndicator, View priorityBadge, List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            hideIndicators(typeIndicator, priorityBadge);
            return;
        }

        // Determine dominant event type and priority
        EventType dominantType = getDominantEventType(events);
        EventPriority dominantPriority = getDominantPriority(events);

        // Setup type indicator
        setupTypeIndicator(typeIndicator, dominantType);

        // Setup priority badge
        setupPriorityBadge(priorityBadge, dominantPriority);
    }



    /**
     * Check if a day has any events.
     * @param events List of events for the day
     * @return true if there are events
     */
    public boolean hasEvents(List<LocalEvent> events) {
        return events != null && !events.isEmpty();
    }

    /**
     * Get events count for display.
     * @param events List of events
     * @return Count of events
     */
    public int getEventsCount(List<LocalEvent> events) {
        return events != null ? events.size() : 0;
    }

    // ==================== PRIVATE METHODS ====================



    /**
     * STEP 5 FIX: Get numeric score for priority (higher = more urgent).
     */
    private int getPriorityScore(EventPriority priority) {
        if (priority == null) return 1;

        switch (priority) {
            case URGENT: return 4;   // Più urgente
            case HIGH: return 3;
            case NORMAL: return 2;
            case LOW:
            default: return 1;       // Meno urgente
        }
    }

    /**
     * Setup the type indicator bar with appropriate color.
     */
    private void setupTypeIndicator(View typeIndicator, EventType eventType) {
        if (typeIndicator == null) return;

        typeIndicator.setVisibility(View.VISIBLE);
        int color = getEventTypeColor(eventType);
        typeIndicator.setBackgroundColor(color);
    }

    /**
     * Hide both indicators when no events.
     */
    private void hideIndicators(View typeIndicator, View priorityBadge) {
        if (typeIndicator != null) {
            typeIndicator.setVisibility(View.GONE);
        }
        if (priorityBadge != null) {
            priorityBadge.setVisibility(View.GONE);
        }
    }


    // ==================== UTILITY METHODS ====================

    /**
     * Get a translucent version of an event type color for backgrounds.
     * @param eventType The event type
     * @param alpha Alpha value (0-255)
     * @return Translucent color
     */
    public int getEventTypeColorWithAlpha(EventType eventType, int alpha) {
        int color = getEventTypeColor(eventType);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Check if an event type is production-related (stop or maintenance).
     * @param eventType The event type to check
     * @return true if production-related
     */
    public boolean isProductionEvent(EventType eventType) {
        if (eventType == null) return false;
        return eventType == EventType.STOP_PLANNED ||
                eventType == EventType.STOP_UNPLANNED ||
                eventType == EventType.STOP_SHORTAGE ||
                eventType == EventType.MAINTENANCE;
    }

    /**
     * Get human-readable description for indicator combination.
     * Useful for accessibility and debugging.
     * @param eventType The dominant event type
     * @param priority The dominant priority
     * @return Description string
     */
    public String getIndicatorDescription(EventType eventType, EventPriority priority) {
        String typeDesc = eventType != null ? eventType.getDisplayName() : "Generale";
        String priorityDesc = priority != null ? priority.getDisplayName() : "Normale";
        return typeDesc + " - Priorità " + priorityDesc;
    }


    /**
     * STEP 5 FIX: Setup advanced event indicators with visual elements.
     * @param typeIndicator Colored bar for event type
     * @param priorityBadge Badge for priority level
     * @param textIndicator Text indicator for event count
     * @param events List of events for the day
     */
    public void setupAdvancedEventIndicators(View typeIndicator, View priorityBadge,
                                             TextView textIndicator, List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            hideAdvancedIndicators(typeIndicator, priorityBadge, textIndicator);
            return;
        }

        // STEP 5 FIX: Get dominant values directly (no EventTypeResult)
        EventType dominantType = getDominantEventType(events);
        EventPriority dominantPriority = getDominantPriority(events);

        // Setup type indicator (colored bar)
        setupTypeIndicatorBar(typeIndicator, dominantType);

        // Setup priority badge
        setupPriorityBadge(priorityBadge, dominantPriority);

        // Setup text indicator (existing logic)
        setupSimpleEventIndicator(textIndicator, events);
    }


    /**
     * STEP 5: Setup type indicator bar with event type color.
     */
    private void setupTypeIndicatorBar(View typeIndicator, EventType eventType) {
        if (typeIndicator == null) return;

        typeIndicator.setVisibility(View.VISIBLE);
        int color = getEventTypeColor(eventType);
        typeIndicator.setBackgroundColor(color);
    }

    /**
     * STEP 5: Setup priority badge with appropriate style.
     */
    private void setupPriorityBadge(View priorityBadge, EventPriority priority) {
        if (priorityBadge == null) return;

        priorityBadge.setVisibility(View.VISIBLE);
        int drawableRes = getPriorityBadgeDrawable(priority);
        priorityBadge.setBackgroundResource(drawableRes);
    }

    /**
     * STEP 5: Hide all advanced indicators.
     */
    private void hideAdvancedIndicators(View typeIndicator, View priorityBadge, TextView textIndicator) {
        if (typeIndicator != null) {
            typeIndicator.setVisibility(View.GONE);
        }
        if (priorityBadge != null) {
            priorityBadge.setVisibility(View.GONE);
        }
        if (textIndicator != null) {
            textIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * STEP 5: Get dominant event type for visual indicator.
     */
    private EventType getDominantEventType(List<LocalEvent> events) {
        EventType dominantType = EventType.GENERAL;
        int dominantScore = 0;

        for (LocalEvent event : events) {
            int typeScore = getEventTypeScore(event.getEventType());
            if (typeScore > dominantScore) {
                dominantScore = typeScore;
                dominantType = event.getEventType();
            }
        }

        return dominantType != null ? dominantType : EventType.GENERAL;
    }

    /**
     * STEP 5 FIX: Get dominant priority for visual indicator.
     * Returns EventPriority directly.
     */
    private EventPriority getDominantPriority(List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            return EventPriority.LOW;
        }

        EventPriority dominantPriority = EventPriority.LOW;
        int dominantScore = 0;

        for (LocalEvent event : events) {
            EventPriority priority = event.getPriority();
            int priorityScore = getPriorityScore(priority);

            if (priorityScore > dominantScore) {
                dominantScore = priorityScore;
                dominantPriority = priority;
            }
        }

        return dominantPriority != null ? dominantPriority : EventPriority.LOW;
    }


    /**
     * STEP 5 FIX: Get score for event type (higher = more critical).
     */
    private int getEventTypeScore(EventType eventType) {
        if (eventType == null) return 1;

        switch (eventType) {
            case STOP_UNPLANNED: return 10;  // Più critico
            case STOP_SHORTAGE: return 9;
            case STOP_PLANNED: return 8;
            case MAINTENANCE: return 7;
            case EMERGENCY: return 6;
            case MEETING: return 3;
            case TRAINING: return 2;
            case GENERAL:
            default: return 1;  // Meno critico
        }
    }

    /**
     * STEP 5: Get event type color.
     */
    private int getEventTypeColor(EventType eventType) {
        if (eventType == null) {
            return ContextCompat.getColor(mContext, R.color.event_type_general);
        }
        switch (eventType) {
            case STOP_UNPLANNED:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_unplanned);
            case STOP_SHORTAGE:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_shortage);
            case STOP_PLANNED:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_planned);
            case MAINTENANCE:
                return ContextCompat.getColor(mContext, R.color.event_type_maintenance);
            case MEETING:
                return ContextCompat.getColor(mContext, R.color.event_type_meeting);
            case TRAINING:
                return ContextCompat.getColor(mContext, R.color.event_type_training);
            default:
                return ContextCompat.getColor(mContext, R.color.event_type_general);
        }
    }

    /**
     * STEP 5: Get priority badge drawable.
     */
    private int getPriorityBadgeDrawable(EventPriority priority) {
        if (priority == null) {
            return R.drawable.event_priority_badge_low;
        }
        switch (priority) {
            case URGENT:
            case HIGH:
                return R.drawable.event_priority_badge_high;
            case NORMAL:
                return R.drawable.event_priority_badge_normal;
            case LOW:
            default:
                return R.drawable.event_priority_badge_low;
        }
    }


}