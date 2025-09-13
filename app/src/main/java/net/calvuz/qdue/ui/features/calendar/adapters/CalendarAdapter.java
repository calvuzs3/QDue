package net.calvuz.qdue.ui.features.calendar.adapters;

import static net.calvuz.qdue.ui.core.common.utils.Library.getColorByThemeAttr;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.core.architecture.base.BaseAdapter;
import net.calvuz.qdue.ui.core.architecture.base.BaseInteractiveAdapter;
import net.calvuz.qdue.ui.core.common.utils.HighlightingHelper;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.utils.EventIndicatorHelper;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CalendarAdapter
 */
public class CalendarAdapter extends BaseInteractiveAdapter {

    // TAG
    private static final String TAG = "CalendarLgsAdp";

    // Events data management
    private Map<LocalDate, List<LocalEvent>> mEventsData = new HashMap<>();
    private Map<LocalDate, Integer> mEventsCount = new HashMap<>();

    // Event Indicator
    private EventIndicatorHelper mEventHelper;

    // Database integration fields
    private QDueDatabase mEventsDatabase;
    private final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);

    // EventsService
    private EventsService mEventsService;
    /// //////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor for CalendarAdapter
     *
     * @param context      Context of the adapter
     * @param service      EventsService instance injected
     * @param items        List of view items
     * @param userHalfTeam User's half team
     */
    public CalendarAdapter(Context context, EventsService service, List<SharedViewModels.ViewItem> items,
                           HalfTeam userHalfTeam) {
        super(context, items, userHalfTeam, 1); // Calendar doesn't show detailed shift info

        // Initialize events service
        mEventsService = service;

        setupMembers(context);
    }

    /**
     * Constructor for CalendarAdapter
     *
     * @param context      Context of the adapter
     * @param items        List of view items
     * @param userHalfTeam User's half team
     */
    public CalendarAdapter(Context context, List<SharedViewModels.ViewItem> items,
                           HalfTeam userHalfTeam) {
        super(context, items, userHalfTeam, 1); // Calendar doesn't show detailed shift info

        mEventsService = null;
        setupMembers(context);
    }

    /**
     * Setup members of the adapter
     * @param context   Context of the adapter
     */
    private void setupMembers(Context context) {
        // Initialize events support
        mEventHelper = new EventIndicatorHelper(context);

        // Initialize database and load real events
        mEventsDatabase = QDueDatabase.getInstance(context);
        loadEventsFromDatabase();

        Log.v(TAG, "✅ CalendarAdapter: initialized");
    }

    /**
     * Create a new calendar day view holder
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return New calendar day view holder
     */
    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use improved calendar layout
        View view = inflater.inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarDayViewHolder((MaterialCardView) view);
    }

    /**
     * Bind data to a calendar day view holder
     *
     * @param dayHolder ViewHolder to bind data to
     * @param dayItem   Day item data to bind
     * @param position  Position in the adapter
     */
    @Override
    public void bindDay(BaseAdapter.DayViewHolder dayHolder, SharedViewModels.DayItem dayItem, int position) {
        Log.v(TAG, "bindDay: " + dayItem.day.getLocalDate());

        MaterialCardView holder = dayHolder.mView;
        CalendarDayViewHolder calendarHolder = (CalendarDayViewHolder) dayHolder;

        // Setup long-click and selection support
        setupLongClickSupport(calendarHolder, dayItem, position);

        // ✅ STEP 1: Reset visual state (SOLO ELEMENTI NON-TEXT)
        resetCalendarCellState(calendarHolder);

        // ✅ STEP 2: Setup content (non-styling)
        setupDayNumber(calendarHolder, dayItem);
        setupEventsIndicator(calendarHolder, dayItem);
        setupShiftDisplay(calendarHolder, dayItem);

        // ✅ STEP 3: Apply text highlighting UNIFICATO (DOPO il setup content)
        LocalDate date = dayItem.day != null ? dayItem.day.getLocalDate() : null;
        if (date != null) {
            // ✅ IMPORTANTE: Includere anche tvShiftName se visibile
            if (calendarHolder.tvShiftName != null && calendarHolder.tvShiftName.getVisibility() == View.VISIBLE) {
                HighlightingHelper.applyUnifiedTextHighlighting(mContext, date,
                        calendarHolder.tvDayNumber, calendarHolder.tvShiftName);
            } else {
                HighlightingHelper.applyUnifiedTextHighlighting(mContext, date,
                        calendarHolder.tvDayNumber);
            }
        }

        // ✅ STEP 4: Apply background highlighting UNIFICATO
        if (date != null) {
            List<LocalEvent> events = getEventsForDate(date);
            HighlightingHelper.applyUnifiedHighlighting(mContext, holder, date, events, mEventHelper);
        }

    }


    /// /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Reset all visual state for consistent appearance
     *
     * @param holder Calendar day view holder to reset
     */
    private void resetCalendarCellState(CalendarDayViewHolder holder) {
        // ❌ DON'T RESET colors
        // HighlightingHelper does it

        // Reset events indicators
        if (holder.vEventsDot != null) {
            holder.vEventsDot.setVisibility(View.GONE);
        }
        if (holder.tvEventsCount != null) {
            holder.tvEventsCount.setVisibility(View.GONE);
        }

        // Reset shift elements (visibility only, not colors)
        if (holder.tvShiftName != null) {
            holder.tvShiftName.setVisibility(View.GONE);
            // ❌ NON FARE: holder.tvShiftName.setTextColor(...)
        }
        if (holder.vShiftIndicator != null) {
            holder.vShiftIndicator.setVisibility(View.INVISIBLE);
        }

        // Reset card styling
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) holder.itemView;
            HighlightingHelper.setupRegularCardStyle(mContext, cardView);
        }
    }

    /**
     * Setup day number in top-left corner with smaller font
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void setupDayNumber(CalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.tvDayNumber == null || dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        holder.tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));
    }

    /**
     * Setup events indicator in top-right corner
     * Dot for presence + badge for count if > 1
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void setupEventsIndicator(CalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        final String mTAG = "setupEventsIndicator: ";

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            // No events - hide both indicators
            if (holder.vEventsDot != null) {
                holder.vEventsDot.setVisibility(View.GONE);
            }
            if (holder.tvEventsCount != null) {
                holder.tvEventsCount.setVisibility(View.GONE);
            }
        } else {
            int eventCount = events.size();

            // FIX: Get priority color for tinting
            int priorityColor = mEventHelper.getHighestPriorityColor(events);

            Log.i(TAG, mTAG + "✅ Setting up events indicator for " + eventCount + " events");
            Log.i(TAG, mTAG + "✅ Priority color: " + Integer.toHexString(priorityColor));

            // Multiple events - show count badge with color
            if (holder.vEventsDot != null) {
                holder.vEventsDot.setVisibility(View.GONE);
            }
            if (holder.tvEventsCount != null) {
                holder.tvEventsCount.setVisibility(View.VISIBLE);
                if (eventCount == 1) {
                    Log.i(TAG, mTAG + "✅ Single event - showing empty badge");
                    holder.tvEventsCount.setText("");
                } else {
                    Log.i(TAG, mTAG + "✅ Multiple events - showing badge: " + eventCount);
                    holder.tvEventsCount.setText(eventCount > 9 ? "9+" : String.valueOf(eventCount));
                }

                // CRITICAL: Apply background tint
                holder.tvEventsCount.getBackground().setTint(priorityColor);
                holder.tvEventsCount.setTextColor(getContrastingTextColor(priorityColor));

            } else {
                Log.e(TAG, mTAG + "tvEventsCount is null");
            }

        }
    }

    /**
     * HELPER - contrasting text color for background
     *
     * @param backgroundColor Background color
     */
    private int getContrastingTextColor(int backgroundColor) {
        // Calculate luminance
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        // Return white for dark backgrounds, black for light
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Setup shift name (first letter) and visibility.
     * Setup shift indicator (colored bar) and visibility.
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void setupShiftDisplay(CalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) return;

        Day day = dayItem.day;

        // Check if user has a shift on this day
        if (day.getInWichTeamIsHalfTeam(mUserHalfTeam) >= 0) {
            // Get user's shift for this day
            Shift userShift = day.getShifts().get(day.getInWichTeamIsHalfTeam(mUserHalfTeam));

            if (userShift != null) {
                // Show shift name (first letter)
                if (holder.tvShiftName != null) {
                    String shiftName = userShift.getShiftType().getShortName();
                    String firstLetter = shiftName.length() > 0 ?
                            shiftName.substring(0, 1).toUpperCase() : "S";
                    holder.tvShiftName.setText(firstLetter);
                    holder.tvShiftName.setVisibility(View.VISIBLE);
                }

                // Show shift indicator bar
                if (holder.vShiftIndicator != null) {
                    holder.vShiftIndicator.setVisibility(View.VISIBLE);
                    int shiftColor = getShiftColor(userShift);
                    holder.vShiftIndicator.setBackgroundColor(shiftColor);
                }
            }
        } else {
            // No shift - hide both elements
            if (holder.tvShiftName != null) {
                holder.tvShiftName.setVisibility(View.INVISIBLE);
            }
            if (holder.vShiftIndicator != null) {
                holder.vShiftIndicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Load events from database asynchronously.
     */
    private void loadEventsFromDatabase() {
        final String mTAG = "loadEventsFromDatabase: ";
        Log.v(TAG, mTAG + "called.");

        if (mIsLoadingEvents.get()) {
            Log.w(TAG, mTAG + "Events loading already in progress");
            return;
        }

        mIsLoadingEvents.set(true);
        Log.i(TAG, mTAG + "✅ Starting to load events from database");

        // Calculate date range (current month ± 2 months for visible range)
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(2).withDayOfMonth(1);
        LocalDate endDate = now.plusMonths(2).withDayOfMonth(now.plusMonths(2).lengthOfMonth());

        // Load events asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                // Convert LocalDate to LocalDateTime for DAO method
                java.time.LocalDateTime startDateTime = startDate.atStartOfDay();
                java.time.LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

                List<LocalEvent> events = mEventsDatabase.eventDao().getEventsForDateRange(startDateTime, endDateTime);

                Log.i(TAG, mTAG + "Loaded " + events.size() + " events from database");
                return events;

            } catch (Exception e) {

                Log.e(TAG, mTAG + "Error loading events from database: " + e.getMessage());
                return new ArrayList<LocalEvent>();
            }
        }).thenAccept(events -> {
            // Process on main thread
            if (mContext instanceof android.app.Activity) {
                ((android.app.Activity) mContext).runOnUiThread(() -> {
                    processLoadedEvents(events);
                    mIsLoadingEvents.set(false);
                });
            }
        }).exceptionally(throwable -> {

            Log.e(TAG, mTAG + "Failed to load events: " + throwable.getMessage());
            mIsLoadingEvents.set(false);
            return null;
        });
    }

    /**
     * Process events loaded from database and expand multi-day events.
     * This ensures multi-day events appear on every day they span.
     *
     * @param events List of events to process
     */
    private void processLoadedEvents(List<LocalEvent> events) {
        final String mTAG = "processLoadedEvents: ";
        Log.v(TAG, mTAG + "called with " + events.size() + " events");

        Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();

        // Process each event and expand multi-day events
        for (LocalEvent event : events) {
            expandEventAcrossDays(event, eventsMap);
        }

        // Update adapter with real data
        updateEventsData(eventsMap);

        Log.i(TAG, mTAG + "Processed " + events.size() + " events into " + eventsMap.size() + " dates");

        // If no real events found
        if (eventsMap.isEmpty()) {

            Log.d(TAG, mTAG + "No real events found");
        }
    }

    /**
     * Expand a single event across all days it covers.
     * This ensures multi-day events appear on every day they span.
     *
     * @param event     Event to expand
     * @param eventsMap Map to add expanded events to
     *                  Key: Date,
     *                  Value: List of events
     */
    private void expandEventAcrossDays(LocalEvent event, Map<LocalDate, List<LocalEvent>> eventsMap) {
        final String mTAG = "expandEventAcrossDays: ";
        Log.v(TAG, mTAG + "called with event: " + event.getTitle());

        try {
            LocalDate startDate = event.getStartDate();
            LocalDate endDate = event.getEndDate();

            // Handle null end date
            if (endDate == null) {
                endDate = startDate;
            }

            // Ensure end date is not before start date
            if (endDate.isBefore(startDate)) {
                Log.w(TAG, mTAG + "Event end date before start date, using start date only: " + event.getTitle());
                endDate = startDate;
            }

            // Add event to every day it spans
            LocalDate currentDate = startDate;
            int dayCount = 0;

            while (!currentDate.isAfter(endDate) && dayCount < 365) { // Safety limit to prevent infinite loops
                eventsMap.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(event);
                currentDate = currentDate.plusDays(1);
                dayCount++;
            }

            // Log multi-day events for debugging
            if (dayCount > 1) {
                Log.d(TAG, mTAG + "Expanded event '" + event.getTitle() + "' across " + dayCount + " days (" + startDate + " to " + endDate + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error expanding event '" + event.getTitle() + "': " + e.getMessage());

            // Fallback: add to start date only
            LocalDate startDate = event.getStartDate();
            if (startDate != null) {
                eventsMap.computeIfAbsent(startDate, k -> new ArrayList<>()).add(event);
            }
        }
    }

    /**
     * Update events data from external source
     * This is called from the main thread to avoid overwriting the data
     *
     * @param eventsMap Map of events to update
     *                  Key: Date,
     *                  Value: List of events
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        // FIX: Assicurarsi che l'aggiornamento avvenga nel main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // Se non siamo nel main thread, fare post al main thread
            new Handler(Looper.getMainLooper()).post(() -> updateEventsData(eventsMap));
            return;
        }

        Log.i(TAG, "updateEventsData on main thread with " +
                (eventsMap != null ? eventsMap.size() : "null") + " entries");

        // Creare copia defensive per evitare modifiche concorrenti
        if (eventsMap != null) {
            this.mEventsData = new HashMap<>();
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsMap.entrySet()) {
                this.mEventsData.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        } else {
            this.mEventsData = new HashMap<>();
        }

        Log.i(TAG, "mEventsData updated with " + mEventsData.size() + " dates");
    }

    /**
     * Get events for a specific date
     *
     * @param date Date to get events for
     */
    private List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> events = mEventsData.get(date);

        return (events != null) ? events : new ArrayList<LocalEvent>();
    }

    /**
     * Load events from database asynchronously
     */
    public void loadEventsAsync() {
        if (mIsLoadingEvents.getAndSet(true)) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return mEventsDatabase.eventDao().getAllEvents();
            } catch (Exception e) {
                Log.e(TAG, "Error loading events", e);
                return new ArrayList<LocalEvent>();
            }
        }).thenAccept(events -> {
            Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();
            for (LocalEvent event : events) {
                LocalDate eventDate = event.getStartDate();
                eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            }

            if (mContext instanceof android.app.Activity) {
                ((android.app.Activity) mContext).runOnUiThread(() -> {
                    updateEventsData(eventsMap);
                    mIsLoadingEvents.set(false);
                });
            }
        });
    }

    /**
     * Notify events data changed
     */
    public void notifyEventsDataChanged() {
        // Brutal
        notifyDataSetChanged();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get color for shift indicator based on shift type
     * If no shift type is set, use theme color
     * If no color is set, use shift name
     *
     * @param shift Shift to get color for
     */
    private int getShiftColor(Shift shift) {
        if (shift == null || shift.getShiftType() == null) {
            return getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary);
        }

        // Use the color from ShiftType
        int shiftColor = shift.getShiftType().getColor();

        // If no color set, use theme-based colors by shift name
        if (shiftColor == 0) {
            String shiftName = shift.getShiftType().getName().toLowerCase();

            if (shiftName.contains("mattino") || shiftName.contains("morning") || shiftName.contains("m")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftMorning);
            } else if (shiftName.contains("pomeriggio") || shiftName.contains("afternoon") || shiftName.contains("p")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftAfternoon);
            } else if (shiftName.contains("notte") || shiftName.contains("night") || shiftName.contains("n")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftNight);
            } else {
                return getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary);
            }
        }

        return shiftColor;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////

    /**
     * CalendarDayViewHolder
     */
    public class CalendarDayViewHolder extends BaseMaterialDayViewHolder {
        final String mTAG = TAG + "CalendarDayViewHolder: ";

        // Day number (top-left, smaller)
        public final TextView tvDayNumber;

        // Events indicators (top-right area)
        public final FrameLayout eventsContainer;
        public final View vEventsDot;
        public final TextView tvEventsCount;

        // Shift display (bottom-right area)
        public final TextView tvShiftName;
        public final View vShiftIndicator;

        public CalendarDayViewHolder(@NonNull MaterialCardView itemView) {
            super(itemView);

            // Initialize all elements
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            eventsContainer = itemView.findViewById(R.id.events_container);
            vEventsDot = itemView.findViewById(R.id.v_swipecalendar_event_dot );
            tvEventsCount = itemView.findViewById(R.id.tv_events_count);
            tvShiftName = itemView.findViewById(R.id.tv_shift_name);
            vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);
        }
    }

    public void setEventsService(EventsService service) {
        this.mEventsService = service;
    }
}