package net.calvuz.qdue.ui.dayslist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.shared.BaseClickAdapterLegacy;
import net.calvuz.qdue.ui.shared.utils.EventIndicatorHelper;
import net.calvuz.qdue.ui.shared.utils.HighlightingHelper;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import com.google.android.material.card.MaterialCardView;

/**
 * DaysListLegacyAdapter
 */
public class DaysListAdapterLegacy extends BaseClickAdapterLegacy {

    // TAG
    private static final String TAG = "DayslistLgsAdp";

    // Simple events tracking (just count for now)
    private Map<LocalDate, List<LocalEvent>> mEventsData = new HashMap<>();
    private Map<LocalDate, Integer> mEventsCount = new HashMap<>();

    // Event Indicator
    private final EventIndicatorHelper mEventHelper;

    // Database integration fields
    private final QDueDatabase mEventsDatabase;
    private final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);

    // Add expansion support flag
    private boolean mSupportsExpansion;

    /// //////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor for DaysListLegacyAdapter
     *
     * @param context      Context of the adapter
     * @param items        List of view items
     * @param userHalfTeam User's half team
     * @param numShifts    Number of shifts per day
     */
    public DaysListAdapterLegacy(Context context, List<SharedViewModels.ViewItem> items,
                                 HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);

        // Set support expansion
        boolean mSupportsExpansion = true;

        // Initialize event helper
        mEventHelper = new EventIndicatorHelper(context);

        // Initialize database and load real events
        mEventsDatabase = QDueDatabase.getInstance(context);
        loadEventsFromDatabase();

        Log.d(TAG, "DayslistAdapterLegacy: ✅ initialized");
    }

    /**
     * Create a new dayslist day view holder
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return New dayslist day view holder
     */
    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use original layout - no changes needed
        View view = inflater.inflate(R.layout.item_dayslist_row, parent, false);
        return new DayslistDayViewHolder((MaterialCardView) view);
    }

    /**
     * Bind data to a dayslist day view holder
     *
     * @param dayHolder ViewHolder to bind data to
     * @param dayItem   Day item data to bind
     * @param position  Position in the adapter
     */
    @Override
    protected void bindDay(DayViewHolder dayHolder, SharedViewModels.DayItem dayItem, int position) {
        Log.v(TAG, "bindDay: " + dayItem.day.getLocalDate());

        // Call parent to do ALL the work
        super.bindDay(dayHolder, dayItem, position);

        MaterialCardView holder = dayHolder.mView;

        // Only add our enhancements if it's our ViewHolder
        if (dayHolder instanceof DayslistDayViewHolder dayslistHolder) {

            // Validate expansion support
            if (!dayslistHolder.supportsExpansion()) {
                Log.w(TAG, "ViewHolder does not support expansion - check layout structure");
            }

            // The order is IMPORTANT

            // Setup expansion-aware click handling
            setupExpansionAwareClicks(dayslistHolder, dayItem, position);

            // Apply expansion state if this card was previously expanded
            restoreExpansionState(dayslistHolder, dayItem);

            // NEW: Setup long-click and selection support
            setupLongClickSupport(dayslistHolder, dayItem, position);

            // STEP 1: Reset visual state
            resetDayslistCellState(dayslistHolder);

            // STEP 2: Setup day number (top-left, smaller font)

            // STEP 3: Setup events indicator (top-right, dot with badge)
            setupEventsIndicator(dayslistHolder, dayItem);

            // STEP 4: Setup shift name and indicator (bottom-right area)
            setupShiftDisplay(dayslistHolder, dayItem);

            // STEP 5: Apply today/special day styling
            applySundaySpecialStyling(dayslistHolder, dayItem);

            // STEP 6. Apply background styling (improved)
            //applyMaterialBackground(dayslistHolder, dayItem);
            applyMaterialBackgroundWithWhite(dayslistHolder, dayItem);

            // STEP 7. Add events indicator
            addWorkingEventsIndicator(dayslistHolder, dayItem);

            // STEP 8. Background Styling
            applyBackgroundStyling(holder, dayItem);

            // 🔧 DEBUG: Log per verifica
            Log.d(TAG, "bindDay completed for position " + position +
                    ", date: " + (dayItem.day != null ? dayItem.day.getLocalDate() : "null") +
                    ", ViewHolder: DayslistDayViewHolder");
        }
    }

    private LinearLayout getExpandableContainer(MaterialCardView cardView) {
        try {
            if (cardView.getChildCount() == 0) return null;

            View firstChild = cardView.getChildAt(0);
            if (firstChild instanceof LinearLayout layout) {
                return layout.getOrientation() == LinearLayout.VERTICAL ? layout : null;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Setup expansion-aware click handling
     */
    private void setupExpansionAwareClicks(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        // This enhances the existing click setup with expansion awareness

        // Add subtle visual feedback for clickable cards with events
        List<LocalEvent> events = getEventsForDate(dayItem.day.getLocalDate());
        if (!events.isEmpty()) {
            // Add ripple effect or subtle indication that card is expandable
//            holder.itemView.setBackgroundResource(R.drawable.expandable_card_background);
        }
    }

    /**
     * Restore expansion state for recycled ViewHolders
     */
    private void restoreExpansionState(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        // This would check if this date was previously expanded and restore state
        // For now, ensure all cards start in collapsed state

        // Reset any expansion artifacts from recycled views
        ViewGroup cardContainer = (ViewGroup) getExpandableContainer((MaterialCardView) holder.itemView) ;
        if (cardContainer instanceof LinearLayout linearContainer) {

            // Remove any previously added expanded content (from recycling)
            if (linearContainer.getChildCount() > 1) {
                // Remove extra children (should only have the original row content)
                for (int i = linearContainer.getChildCount() - 1; i > 0; i--) {
                    linearContainer.removeViewAt(i);
                }
            }
        }

        // Reset card height to wrap_content
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        holder.itemView.setLayoutParams(params);
    }

    // ===========================================
    // Toolbar Action Handling
    // ===========================================

    /**
     * Handle toolbar action execution
     *
     * @param action Toolbar action to handle
     * @param day    Day associated with the action
     * @param date   Date associated with the action
     */
    @Override
    protected void handleToolbarAction(ToolbarAction action, Day day, LocalDate date) {
        switch (action) {
            case FERIE:
                createQuickEvent("Ferie", date, EventType.GENERAL);
                break;

            case MALATTIA:
                createQuickEvent("Malattia", date, EventType.GENERAL);
                break;

            case LEGGE_104:
                createQuickEvent("Legge 104", date, EventType.GENERAL);
                break;

            case PERMESSO:
                createQuickEvent("Permesso", date, EventType.GENERAL);
                break;

            case PERMESSO_SINDACALE:
                createQuickEvent("Permesso Sindacale", date, EventType.GENERAL);
                break;

            case ADD_EVENT:
                // This will be handled by fragment to open event editor
                Log.d(TAG, "ADD_EVENT action - delegating to fragment");
                break;

            case VIEW_EVENTS:
                // This will be handled by fragment to show events list
                Log.d(TAG, "VIEW_EVENTS action - delegating to fragment");
                break;

            default:
                Log.w(TAG, "Unknown toolbar action: " + action);
        }
    }

    /**
     * Create a quick event for the specified date
     * This is a simplified event creation - full implementation would create LocalEvent
     *
     * @param title Title of the event
     * @param date  Date of the event
     * @param type  Type of the event
     */
    private void createQuickEvent(String title, LocalDate date, EventType type) {
        Log.d(TAG, "Creating quick event: " + title + " for date: " + date);

        // TODO: Create actual LocalEvent and save to database
        // For now, just log the action

        // Example implementation:
        /*
        LocalEvent quickEvent = new LocalEvent();
        quickEvent.setTitle(title);
        quickEvent.setStartDate(date);
        quickEvent.setEndDate(date);
        quickEvent.setEventType(type);
        quickEvent.setAllDay(true);
        quickEvent.setCreatedAt(LocalDateTime.now());

        // Save to database asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                mEventsDatabase.eventDao().insert(quickEvent);

                // Refresh events data on main thread
                ((Activity) mContext).runOnUiThread(() -> {
                    refreshEventsFromDatabase();
                    Log.d(TAG, "Quick event created and saved: " + title);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving quick event: " + e.getMessage());
            }
        });
        */
    }

    /// /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Reset all visual state for consistent appearance
     *
     * @param holder ViewHolder to reset
     */
    private void resetDayslistCellState(DayslistDayViewHolder holder) {
        // FIX: Reset day number text to normal state
        if (holder.tday != null) {
            holder.tday.setTextColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOnSurface));
            holder.tday.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            holder.tday.setTextSize(14f); // Reset size if needed
        }

        // FIX: Reset day name text to normal state
        if (holder.twday != null) {
            holder.twday.setTextColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOnSurface));
            holder.twday.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.NORMAL);
            holder.twday.setTextSize(14f); // Reset size if needed
        }

        // FIX: Reset shift TextViews to normal state
        for (TextView shiftText : holder.shiftTexts) {
            if (shiftText != null) {
                shiftText.setTextColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOnSurface));
                shiftText.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.NORMAL);
                shiftText.setTextSize(12f); // Reset size if needed
            }
        }

        // FIX: Reset rest teams TextView
        if (holder.ttR != null) {
            holder.ttR.setTextColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOnSurface));
            holder.ttR.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.NORMAL);
            holder.ttR.setTextSize(12f); // Reset size if needed
        }

        // FIX: Reset events indicator
        if (holder.eventsIndicator != null) {
            holder.eventsIndicator.setVisibility(View.GONE);
            holder.eventsIndicator.setTextColor(getColorByThemeAttr(mContext,
                    androidx.appcompat.R.attr.colorPrimary));
            holder.eventsIndicator.setBackgroundResource(R.drawable.events_indicator_background);
        }

        // Reset card styling - IMPORTANT: Reset all MaterialCardView properties
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) holder.itemView;

            HighlightingHelper.setupRegularCardStyle(mContext, cardView);
        }
    }

    /**
     * Setup events indicator in top-right corner
     * Dot for presence + badge for count if > 1
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void setupEventsIndicator(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        final String mTAG = "setupEventsIndicator: ";
        Log.v(TAG, mTAG + "called.");

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            // No events - hide indicator
            if (holder.eventsIndicator != null) {
                holder.eventsIndicator.setVisibility(View.GONE);
            }
        } else {
            int eventCount = events.size();

            // FIX: Get priority color for tinting
            int priorityColor = mEventHelper.getHighestPriorityColor(events);

            Log.d(TAG, mTAG + "Showing badge: " + eventCount);
            Log.d(TAG, mTAG + "Priority color: " + Integer.toHexString(priorityColor));

            if (holder.eventsIndicator != null) {
                holder.eventsIndicator.setVisibility(View.VISIBLE);
                holder.eventsIndicator.setText(eventCount > 9 ? "9+" : String.valueOf(eventCount));

                // CRITICAL: Apply background tint
                holder.eventsIndicator.getBackground().setTint(priorityColor);
                holder.eventsIndicator.setTextColor(getContrastingTextColor(priorityColor));

                Log.d(TAG, mTAG + "Badge set: text=" + eventCount + ", visibility=VISIBLE");
            } else {
                Log.e(TAG, mTAG + "tvEventsCount is NULL!");
            }

        }
    }

    /**
     * Setup shift name (first letter) and shift indicator
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void setupShiftDisplay(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) return;

        Day day = dayItem.day;

        // Set shift information
        List<Shift> shifts = day.getShifts();
        for (int i = 0; i < holder.shiftTexts.length; i++) {
            TextView shiftText = holder.shiftTexts[i];

            if (shiftText != null) {
                if (i < shifts.size()) {
                    Shift shift = shifts.get(i);
                    shiftText.setText(shift.getTeamsAsString());

                    // Apply shift colors if needed
                    if (mUserHalfTeam != null && shift.containsHalfTeam(mUserHalfTeam)) {
                        shiftText.setBackgroundColor(shift.getShiftType().getColor());
                    } else {
                        shiftText.setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    shiftText.setText("");
                    shiftText.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    /**
     * Apply background styling for visual hierarchy
     * Provides subtle backgrounds for better calendar readability
     *
     * @param cardView View to apply styling to
     * @param dayItem  Day item data to bind
     */
    private void applyBackgroundStyling(MaterialCardView cardView, SharedViewModels.DayItem dayItem) {

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        LocalDate today = LocalDate.now();

        // NUOVO: Controllare se ci sono eventi per questa data
        List<LocalEvent> events = getEventsForDate(date);

        // Regular days: Standard subtle background
        HighlightingHelper.setupRegularCardStyle(mContext, cardView);

        if (date.equals(today)) {
            // Today: Special background with elevation
            HighlightingHelper.setupTodayCardStyle(mContext, cardView);
        }
        if (date.getDayOfWeek().getValue() == 7) { // Sunday
            // Sunday: Light background highlighting
            HighlightingHelper.setupSundayCardStyle(mContext, cardView);
        }
        if (!events.isEmpty()) {
            // Events: colored background
            HighlightingHelper.setupEventsCardStyle(mContext, mEventHelper, cardView, events);
        }
            // Regular days: Standard subtle background
            HighlightingHelper.setupRegularCardStyle(mContext, cardView);

//        if (date.equals(today)) {
//            // Today: Special background with elevation
//            HighlightingHelper.setupTodayCardStyle(mContext, cardView);
//        } else if (date.getDayOfWeek().getValue() == 7) { // Sunday
//            // Sunday: Light background highlighting
//            HighlightingHelper.setupSundayCardStyle(mContext, cardView);
//        } else if (!events.isEmpty()) {
//            // Events: colored background
//            HighlightingHelper.setupEventsCardStyle(mContext, mEventHelper, cardView, events);
//        } else {
//            // Regular days: Standard subtle background
//            HighlightingHelper.setupRegularCardStyle(mContext, cardView);
//        }

        if (date.isBefore(today)) {
            // Past days - slightly faded
            HighlightingHelper.setupOldDaysCardStyle(cardView);
        }
    }

    /**
     * Usare il blend con bianco nella applyMaterialBackground
     *
     * @param holder  DayslistViewHolder to apply styling to
     * @param dayItem Day item data to bind
     */
    private void applyMaterialBackgroundWithWhite(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) holder.itemView;
        View rootView = holder.itemView;

        LocalDate date = dayItem.day != null ? dayItem.day.getLocalDate() : null;
        LocalDate today = LocalDate.now();

        List<LocalEvent> events = date != null ? getEventsForDate(date) : new ArrayList<>();

        // Apply TODAY STYLING
        if (dayItem.isToday()) {
            HighlightingHelper.setupTodayCardStyle(mContext, cardView);
        }

    }

    /**
     * Working events indicator with improved colors.
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void addWorkingEventsIndicator(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.eventsIndicator == null || dayItem.day == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            holder.eventsIndicator.setVisibility(View.INVISIBLE); // Mantiene spazio
            holder.eventsIndicator.setText("");
        } else {
            holder.eventsIndicator.setVisibility(View.VISIBLE);
            int count = events.size();
            holder.eventsIndicator.setText(count == 1 ? "1" : String.valueOf(count));

            // FIX: Colore badge più visibile basato su priorità
            int priorityColor = mEventHelper.getHighestPriorityColor(events);

            // Applicare colore al background del badge
            holder.eventsIndicator.setBackgroundTintList(ColorStateList.valueOf(priorityColor));

            // Testo contrastante
            holder.eventsIndicator.setTextColor(getContrastingTextColor(priorityColor));

            // FIX: Styling badge più prominente
            holder.eventsIndicator.setTextSize(10f);
            holder.eventsIndicator.setTypeface(holder.eventsIndicator.getTypeface(), Typeface.BOLD);
        }
    }

    /**
     * HELPER - contrasting text color
     *
     * @param backgroundColor Background color
     */
    private int getContrastingTextColor(int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Apply special styling for Sunday.
     *
     * @param holder  DayslistViewHolder to apply styling to
     * @param dayItem Day item data to bind
     */
    private void applySundaySpecialStyling(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (!dayItem.isSunday()) return;

        // Red text for Sunday day number
        if (holder.tday != null) {
            holder.tday.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            holder.tday.setTypeface(holder.tday.getTypeface(), android.graphics.Typeface.BOLD);
        }

        // Red text for Sunday day name
        if (holder.twday != null) {
            holder.twday.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            holder.twday.setTypeface(holder.twday.getTypeface(), android.graphics.Typeface.BOLD);
        }

        for (TextView shiftText : holder.shiftTexts){
            // Red text for Sunday day name
            if (shiftText != null) {
                shiftText.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
                shiftText.setTypeface(shiftText.getTypeface(), android.graphics.Typeface.BOLD);
            }
        }

        // Red text for Sunday day name
        if (holder.ttR != null) {
            holder.ttR.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            holder.ttR.setTypeface(holder.ttR.getTypeface(), android.graphics.Typeface.BOLD);
        }
    }

    /**
     * Add events indicator
     *
     * @param holder  ViewHolder to setup
     * @param dayItem Day item data to bind
     */
    private void addSimpleEventsIndicator(DayslistDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.eventsIndicator == null || dayItem.day == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();

        // STEP 2: Get events for this date (empty for now, will add data in STEP 3)
        List<LocalEvent> events = getEventsForDate(date);

        // STEP 2: Use EventIndicatorHelper to setup indicator
        mEventHelper.setupSimpleEventIndicator(holder.eventsIndicator, events);
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
        Log.d(TAG, mTAG + "Starting to load events from database");

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

                List<LocalEvent> events = mEventsDatabase.eventDao().getEventsInDateRange(startDateTime, endDateTime);

                Log.d(TAG, mTAG + "Loaded " + events.size() + " events from database");
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
     * @param events List of events loaded from database
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

        Log.d(TAG, mTAG + "Processed " + events.size() + " events into " + eventsMap.size() + " dates");

        // If no real events found
        if (eventsMap.isEmpty()) {

            Log.d(TAG, mTAG + "No real events found");
        }
    }

    /**
     * Get events for a specific date.
     * For now returns empty list - will be populated in STEP 3.
     *
     * @param date Date to get events for
     * @return List of events for the specified date (or empty list)
     */
    private List<LocalEvent> getEventsForDate(LocalDate date) {
        final String mTAG = "getEventsForDate: ";

        List<LocalEvent> events = mEventsData.get(date);
        List<LocalEvent> result = events != null ? events : new ArrayList<>();

        // Debug logging
        if (!result.isEmpty()) {
            Log.d(TAG, mTAG + "Found " + result.size() + " events for date " + date);
        }

        return result;
    }

    /**
     * Method to update events data (will be used in STEP 3).
     * For now just stores the data.
     *
     * @param eventsMap Map of events for each date
     *                  Key: Date
     *                  Value: List of events
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        final String mTAG = "updateEventsData: ";

        this.mEventsData = eventsMap != null ? eventsMap : new HashMap<>();

        Log.d(TAG, mTAG + "Updated events data - " + mEventsData.size() + " dates with events");

        // Notify adapter to refresh indicators
        notifyDataSetChanged();
    }

    /**
     * Update events count for days
     *
     * @param eventsCount Map of events count for each date
     *                    Key: Date
     *                    Value: Number of events
     */
    public void updateEventsCount(Map<LocalDate, Integer> eventsCount) {
        mEventsCount = eventsCount != null ? eventsCount : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * Expand a single event across all days it covers.
     * This ensures multi-day events appear on every day they span.
     *
     * @param event Event to expand
     *              Key: Date
     *              Value: List of events
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
     * Get the end date of an event safely.
     * Handles cases where endTime might be null or same day.
     *
     * @param event Event to get end date for
     */
    private LocalDate getEventEndDate(LocalEvent event) {
        try {
            if (event.getEndTime() != null) {
                return event.getEndTime().toLocalDate();
            } else if (event.getStartTime() != null) {
                return event.getStartTime().toLocalDate();
            } else {
                return event.getStartDate();
            }
        } catch (Exception e) {
            Log.w(TAG, "getEventEndDate: Error getting end date for event, using start date: " + e.getMessage());
            return event.getStartDate();
        }
    }

    /**
     * Public method to refresh events data.
     * Call this when events are added/modified in the database.
     */
    public void refreshEventsFromDatabase() {
        Log.d(TAG, "refreshEventsFromDatabase: Refreshing events from database");
        mEventsData.clear();
        loadEventsFromDatabase();
    }

    /// /////////////////////////////////////////////////////////////////////////////////////

    // Add validation method to adapter
    private void validateExpansionSupport() {
        // This method can be called during adapter initialization
        // to ensure all ViewHolders support expansion
        Log.d(TAG, "Validating expansion support for DaysList rows");

        // The validation happens in ViewHolder constructor
        // This method serves as a placeholder for future checks
    }

    // Update setupLongClickSupport to include expansion support check
    @Override
    protected void setupLongClickSupport(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        super.setupLongClickSupport(holder, dayItem, position);

        // Additional check for expansion support
        if (holder instanceof DayslistDayViewHolder dayslistHolder) {
            if (!dayslistHolder.supportsExpansion()) {
                Log.w(TAG, "ViewHolder at position " + position + " does not support expansion");
            }
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////

    /**
     * DayslistDayViewHolder
     */
    public class DayslistDayViewHolder extends BaseMaterialDayViewHolder {
        public TextView eventsIndicator;

        public DayslistDayViewHolder(@NonNull MaterialCardView itemView) {
            super(itemView);

            // Events Indicator
            eventsIndicator = itemView.findViewById(R.id.tv_events_indicator);

            // Ensure the card layout supports expansion
            View childLayout = itemView.getChildAt(0);
            if (!(childLayout instanceof LinearLayout linearLayout)) {
                Log.w(TAG, "DayslistDayViewHolder: Child layout is not LinearLayout, expansion may not work properly");
                mSupportsExpansion = false;
            } else {
                if (linearLayout.getOrientation() != LinearLayout.VERTICAL) {
                    Log.w(TAG, "DayslistDayViewHolder: LinearLayout is not vertical, expansion may not work properly");
                    mSupportsExpansion = false;
                }
            }

            // Hide by default
            if (eventsIndicator != null) {
                eventsIndicator.setVisibility(View.GONE);
            }

            Log.d(TAG, "DayslistDayViewHolder: initialized");
        }

        /**
         * API: Check if expansion is supported
         */
        public boolean supportsExpansion() {
            return mSupportsExpansion;
        }
    }
}