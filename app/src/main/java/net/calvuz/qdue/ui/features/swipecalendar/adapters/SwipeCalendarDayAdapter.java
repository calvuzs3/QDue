package net.calvuz.qdue.ui.features.swipecalendar.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.preferences.QDuePreferences;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SwipeCalendarDayAdapter - RecyclerView adapter for individual month day grid.
 *
 * <p>Manages 7x6 grid layout (42 cells) representing a complete month view with
 * proper handling of previous/next month overflow days. Integrates with existing
 * calendar infrastructure for events and work schedule display.</p>
 *
 * <h3>Grid Layout:</h3>
 * <ul>
 *   <li><strong>7 Columns</strong>: Monday through Sunday (ISO 8601 standard)</li>
 *   <li><strong>6 Rows</strong>: Complete weeks to show full month context</li>
 *   <li><strong>42 Total Cells</strong>: Fixed grid size for consistent UI</li>
 *   <li><strong>Overflow Handling</strong>: Previous/next month days shown in muted style</li>
 * </ul>
 *
 * <h3>Event Integration:</h3>
 * <ul>
 *   <li>LocalEvent display with type indicators</li>
 *   <li>Work schedule (Quattrodue) pattern visualization</li>
 *   <li>Multiple events per day aggregation</li>
 *   <li>Priority-based event highlighting</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class SwipeCalendarDayAdapter extends RecyclerView.Adapter<SwipeCalendarDayAdapter.DayViewHolder> {

    private static final String TAG = "SwipeCalendarDayAdapter";
    private static final int GRID_SIZE = 42; // 7 columns x 6 rows

    // ==================== INTERFACES ====================

    /**
     * Interface for day click events.
     */
    public interface OnDayClickListener {
        /**
         * Called when user clicks on a day cell.
         *
         * @param date      Selected date
         * @param day       Day data (may be null for overflow days)
         * @param dayEvents Events for this day (may be empty)
         */
        void onDayClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull List<LocalEvent> dayEvents);

        /**
         * Called when user long-clicks on a day cell.
         *
         * @param date Selected date
         * @param day  Day data (may be null for overflow days)
         * @param view Clicked view for animations
         */
        void onDayLongClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull View view);
    }

    // ==================== DATA CLASSES ====================

    /**
     * Represents a single day cell in the calendar grid.
     *
     * @param dayData May be null for overflow days
     */
    private record CalendarDayItem(LocalDate date, boolean isCurrentMonth, boolean isToday,
                                   WorkScheduleDay dayData, List<LocalEvent> events) {
        private CalendarDayItem(@NonNull LocalDate date, boolean isCurrentMonth, boolean isToday,
                                @Nullable WorkScheduleDay dayData, @NonNull List<LocalEvent> events) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.dayData = dayData;
            this.events = new ArrayList<>( events );
        }
    }

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final LayoutInflater mInflater;

    // ==================== DATA ====================

    private YearMonth mCurrentMonth;
    private List<CalendarDayItem> mDayItems = new ArrayList<>();

    // Events cache by date
    private Map<LocalDate, List<LocalEvent>> mEventsCache = new ConcurrentHashMap<>();

    // Work schedule data cache by date
    private Map<LocalDate, WorkScheduleDay> mWorkScheduleCache = new ConcurrentHashMap<>();

    // ==================== LISTENERS ====================

    private OnDayClickListener mDayClickListener;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates SwipeCalendarDayAdapter for month display.
     *
     * @param context Context for resource access
     * @param month   Initial month to display
     */
    public SwipeCalendarDayAdapter(@NonNull Context context, @NonNull YearMonth month) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from( context );
        this.mCurrentMonth = month;

        Log.d( TAG, "Created adapter for month: " + month );
        generateDayItems();
    }

    // ==================== ADAPTER IMPLEMENTATION ====================

    @Override
    public int getItemCount() {
        return GRID_SIZE;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate( R.layout.item_swipe_calendar_day, parent, false );
        return new DayViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        if (position < 0 || position >= mDayItems.size()) {
            Log.w( TAG, "Invalid position: " + position );
            return;
        }

        CalendarDayItem dayItem = mDayItems.get( position );
        holder.bind( dayItem );
    }

    // ==================== VIEWHOLDER ====================

    /**
     * ViewHolder for individual day cells.
     * Reuses existing item_calendar_day.xml layout.
     */
    class DayViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView dayNumberText;

        // Event indicators (from item_calendar_day.xml)
        private final View eventTypeIndicator;
        private final View eventPriorityBadge;
        private final TextView eventCountText;
        private final TextView workScheduleText;

        // Work schedule indicator
        //private final View workScheduleIndicator;

        private CalendarDayItem currentDayItem;

        public DayViewHolder(@NonNull View itemView) {
            super( itemView );

            // Find views from item_calendar_day.xml
            cardView = (MaterialCardView) itemView;
            dayNumberText = itemView.findViewById( R.id.tv_day_number );

            // Event indicators (may be null if not present in layout)
            eventTypeIndicator = itemView.findViewById( R.id.event_type_indicator );
            eventPriorityBadge = itemView.findViewById( R.id.event_priority_badge );
            eventCountText = itemView.findViewById( R.id.tv_events_count );

            // Work schedule indicators
            workScheduleText = itemView.findViewById( R.id.tv_work_schedule );
            //workScheduleIndicator = itemView.findViewById(R.id.v_shift_indicator);

            // Set click listeners
            itemView.setOnClickListener( this::onItemClick );
            itemView.setOnLongClickListener( this::onItemLongClick );
        }

        /**
         * Bind day item data to views.
         */
        public void bind(@NonNull CalendarDayItem dayItem) {
            this.currentDayItem = dayItem;

            // Set day number
            dayNumberText.setText( String.valueOf( dayItem.date.getDayOfMonth() ) );

            // Configure card appearance based on day type
            configureCardAppearance( dayItem );

            // Configure event indicators
            configureEventIndicators( dayItem );

            // Configure work schedule indicators
            configureWorkScheduleIndicators( dayItem );

            // Set content description for accessibility
            setContentDescription( dayItem );
        }

        /**
         * Configure card appearance based on day type.
         */
        private void configureCardAppearance(@NonNull CalendarDayItem dayItem) {
            Context context = mContext;

            if (dayItem.isToday) {
                // Today - highlighted
                cardView.setCardBackgroundColor( context.getColor( R.color.calendar_day_today_background ) );
                dayNumberText.setTextColor( context.getColor( R.color.calendar_day_today_text ) );
                cardView.setStrokeColor( context.getColor( R.color.calendar_day_today_stroke ) );
                cardView.setStrokeWidth( context.getResources().getDimensionPixelSize( R.dimen.calendar_day_today_stroke_width ) );
            } else if (!dayItem.isCurrentMonth) {
                // Previous/next month - muted
                cardView.setCardBackgroundColor( context.getColor( R.color.calendar_day_other_month_background ) );
                dayNumberText.setTextColor( context.getColor( R.color.calendar_day_other_month_text ) );
                cardView.setStrokeWidth( 0 );
            } else {
                // Current month - normal
                cardView.setCardBackgroundColor( context.getColor( R.color.calendar_day_current_month_background ) );
                dayNumberText.setTextColor( context.getColor( R.color.calendar_day_current_month_text ) );
                cardView.setStrokeWidth( 0 );
            }

            // Apply weekend styling if applicable
            if (dayItem.date.getDayOfWeek().getValue() >= 6) { // Saturday=6, Sunday=7
                dayNumberText.setTextColor( context.getColor( R.color.calendar_day_weekend_text ) );
            }

            // Configure work schedule Highlight
            if (dayItem.dayData != null) {
                configureWorkScheduleIndicators( dayItem );
            }
        }

        /**
         * Configure work schedule indicator based on day data.
         */
        private void configureWorkScheduleIndicators(@NonNull CalendarDayItem dayItem) {
            if (dayItem.dayData == null) {
                return;
            }

            String userTeam = QDuePreferences.getSelectedTeamNameForRepository( mContext ); // QuattroDue.getInstance( mContext ).getUserHalfTeam();
            boolean hasWorkSchedule = dayItem.dayData.isTeamWorking( userTeam );

            // WorkSchedule Text
            if (hasWorkSchedule) {
                // Set text
                workScheduleText.setVisibility( View.VISIBLE );
                workScheduleText.setText( getWorkScheduleText( dayItem.dayData, userTeam ) );
                // Set card background color
                String scheduleColor = getWorkScheduleColor( dayItem.dayData, userTeam );
                cardView.findViewById( R.id.layout_work_indicators ).setBackgroundColor( Color.parseColor( scheduleColor ) ); //" ) .setCardBackgroundColor( Color.parseColor( scheduleColor ) );
            } else {
                workScheduleText.setVisibility( View.GONE );
            }
        }

        /**
         * Configure event indicators based on day events.
         */
        private void configureEventIndicators(@NonNull CalendarDayItem dayItem) {
            boolean hasEvents = !dayItem.events.isEmpty();

            // Event type indicator
            if (eventTypeIndicator != null) {
                if (hasEvents) {
                    eventTypeIndicator.setVisibility( View.VISIBLE );
                    // Set color based on highest priority event type
                    LocalEvent primaryEvent = dayItem.events.get( 0 ); // Assume sorted by priority
                    int eventColor = getEventTypeColor( primaryEvent );
                    eventTypeIndicator.setBackgroundColor( eventColor );
                } else {
                    eventTypeIndicator.setVisibility( View.GONE );
                }
            }

            // Event priority badge
            if (eventPriorityBadge != null) {
                boolean hasHighPriorityEvent = dayItem.events.stream()
                        .anyMatch( event -> event.getPriority() != null && event.getPriority().isHigh() );
                eventPriorityBadge.setVisibility( hasHighPriorityEvent ? View.VISIBLE : View.GONE );
            }

            // Event count text
            if (eventCountText != null) {
                if (hasEvents && dayItem.events.size() > 1) {
                    eventCountText.setVisibility( View.VISIBLE );
                    eventCountText.setText( String.valueOf( dayItem.events.size() ) );
                } else {
                    eventCountText.setVisibility( View.GONE );
                }
            }
        }

        /**
         * Set accessibility content description.
         */
        private void setContentDescription(@NonNull CalendarDayItem dayItem) {
            StringBuilder description = new StringBuilder();

            // Date information
            String dayName = dayItem.date.getDayOfWeek().getDisplayName( TextStyle.FULL, Locale.getDefault() );
            String monthName = dayItem.date.getMonth().getDisplayName( TextStyle.FULL, Locale.getDefault() );

            // Team information
            String userTeam = QDuePreferences.getSelectedTeamNameForRepository( mContext ); // QuattroDue.getInstance( mContext ).getUserHalfTeam();

            description.append( MessageFormat.format( "{0}, {1} {2} {3}",
                    dayName, dayItem.date.getDayOfMonth(), monthName, dayItem.date.getYear() ) );

            // Today indicator
            if (dayItem.isToday) {
                description.append( ", " ).append( mContext.getString( R.string.calendar_accessibility_today ) );
            }

            // Events information
            if (!dayItem.events.isEmpty()) {
                description.append( ", " ).append( mContext.getResources().getQuantityString(
                        R.plurals.calendar_accessibility_events_count,
                        dayItem.events.size(),
                        dayItem.events.size() ) );
            }

            // Work schedule information
            if (dayItem.dayData != null && dayItem.dayData.isTeamWorking( userTeam )) {
                description.append( ", " ).append( mContext.getString( R.string.calendar_accessibility_work_scheduled ) );
            }

            itemView.setContentDescription( description.toString() );
        }

        /**
         * Handle item click.
         */
        private void onItemClick(View view) {
            if (currentDayItem != null && mDayClickListener != null) {
                mDayClickListener.onDayClick( currentDayItem.date, currentDayItem.dayData, currentDayItem.events );
            }
        }

        /**
         * Handle item long click.
         */
        private boolean onItemLongClick(View view) {
            if (currentDayItem != null && mDayClickListener != null) {
                mDayClickListener.onDayLongClick( currentDayItem.date, currentDayItem.dayData, view );
                return true;
            }
            return false;
        }

        /**
         * Get color for event type.
         */
        private int getEventTypeColor(@NonNull LocalEvent event) {
            // TODO: Implement event type color mapping
            return mContext.getColor( R.color.calendar_event_default_color );
        }

        /**
         * Get color for work schedule.
         */
        private String getWorkScheduleColor(@NonNull WorkScheduleDay day, String userTeam) {
            WorkScheduleShift shift = day.getShifts().get( day.findTeamShiftIndex( userTeam ) );

            return ( shift.getShift().getColorHex() );
        }

        /**
         * Get short name for work schedule.
         */
        private String getWorkScheduleText(@NonNull WorkScheduleDay day, String userTeam) {
            WorkScheduleShift shift = day.getShifts().get( day.findTeamShiftIndex( userTeam ) );

            return shift.getShift().getShortName(); // .getName();
        }
    }

    // ==================== DATA MANAGEMENT ====================

    /**
     * Update month and regenerate day items.
     *
     * @param month New month to display
     */
    public void updateMonth(@NonNull YearMonth month) {
        if (!month.equals( mCurrentMonth )) {
            mCurrentMonth = month;
            generateDayItems();
            notifyDataSetChanged();
            Log.d( TAG, "Updated to month: " + month );
        }
    }

    /**
     * Update events data for the current month.
     *
     * @param eventsMap Events mapped by date
     */
    public void updateEvents(@NonNull Map<LocalDate, List<LocalEvent>> eventsMap) {
        mEventsCache.clear();
        mEventsCache.putAll( eventsMap );

        // Regenerate day items with new events
        generateDayItems();
        notifyDataSetChanged();

        Log.d( TAG, "Updated events for " + eventsMap.size() + " dates" );
    }

    /**
     * Update work schedule data for the current month.
     *
     * @param workScheduleMap Work schedule mapped by date
     */
    public void updateWorkSchedule(@NonNull Map<LocalDate, WorkScheduleDay> workScheduleMap) {
        mWorkScheduleCache.clear();
        mWorkScheduleCache.putAll( workScheduleMap );

        // Regenerate day items with new work schedule
        generateDayItems();
        notifyDataSetChanged();

        Log.d( TAG, "Updated work schedule for " + workScheduleMap.size() + " dates" );
    }

    /**
     * Set day click listener.
     *
     * @param listener Click listener
     */
    public void setOnDayClickListener(@Nullable OnDayClickListener listener) {
        this.mDayClickListener = listener;
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Generate 42 day items for the current month grid.
     * Includes overflow days from previous and next months.
     */
    private void generateDayItems() {
        mDayItems.clear();

        // Get first day of month and find the Monday of that week
        LocalDate firstDayOfMonth = mCurrentMonth.atDay( 1 );
        LocalDate startDate = firstDayOfMonth.minusDays( firstDayOfMonth.getDayOfWeek().getValue() - 1 );

        LocalDate today = LocalDate.now();

        // Generate 42 days (6 weeks)
        for (int i = 0; i < GRID_SIZE; i++) {
            LocalDate currentDate = startDate.plusDays( i );

            boolean isCurrentMonth = currentDate.getMonth() == mCurrentMonth.getMonth() &&
                    currentDate.getYear() == mCurrentMonth.getYear();
            boolean isToday = currentDate.equals( today );

            // Get day data and events for this date
            WorkScheduleDay dayData = mWorkScheduleCache.get( currentDate );
            List<LocalEvent> events = mEventsCache.getOrDefault( currentDate, new ArrayList<>() );

            CalendarDayItem dayItem = new CalendarDayItem( currentDate, isCurrentMonth, isToday, dayData, events );
            mDayItems.add( dayItem );
        }

        Log.v( TAG, "Generated " + mDayItems.size() + " day items for " + mCurrentMonth );
    }
}