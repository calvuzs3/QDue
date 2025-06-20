/**
 * SIMPLE Enhanced DaysListAdapter
 *
 * Extends original DaysListAdapter with:
 * - Material Design compliant backgrounds
 * - Events indicator at bottom of rows
 * - Minimal changes to proven working code
 */

package net.calvuz.qdue.ui.dayslist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.EventIndicatorHelper;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

/**
 * Enhanced DaysListAdapter with Material Design compliance and events support
 * Minimal changes to the proven original adapter
 */
public class SimpleEnhancedDaysListAdapter extends BaseAdapter {

    // TAG
    private static final String TAG = "SimpleEnhancedDaysListAdapter";

    // Simple events tracking (just count for now)
    private Map<LocalDate, List<LocalEvent>> mEventsData = new HashMap<>();
    private Map<LocalDate, Integer> mEventsCount = new HashMap<>();

    // Event Indicator
    private EventIndicatorHelper mEventHelper;

    // STEP 4: Database integration fields
    private EventsDatabase mEventsDatabase;
    private final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);

    public SimpleEnhancedDaysListAdapter(Context context, List<SharedViewModels.ViewItem> items,
                                         HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);

        // STEP 2: Initialize event helper
        mEventHelper = new EventIndicatorHelper(context);

        // STEP 3: Create mock events for testing
        //createMockEventsForTesting();

        // STEP 4: Initialize database and load real events
        mEventsDatabase = EventsDatabase.getInstance(context);
        loadEventsFromDatabase();
    }

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use original layout - no changes needed
        View view = inflater.inflate(R.layout.item_simple_dayslist_row, parent, false);
        return new MaterialDayViewHolder(view);
    }

    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        // Call parent to do ALL the work
        super.bindDay(holder, dayItem, position);

        // Only add our enhancements if it's our ViewHolder
        if (holder instanceof MaterialDayViewHolder) {
            MaterialDayViewHolder materialHolder = (MaterialDayViewHolder) holder;

            // FIX: SEMPRE reset TUTTO prima di applicare nuovi stili
            resetAllViewHolderState(materialHolder);

            // Apply background styling (improved)
            applyMaterialBackground(materialHolder, dayItem);

            // Apply special Sunday styling ONLY if it's Sunday
            if (dayItem.isSunday()) {
                applySundaySpecialStyling(materialHolder, dayItem);
            }

            // Add events indicator
            addWorkingEventsIndicator(materialHolder, dayItem);
        }
    }



    /**
     * Bind data specifically for MaterialDayViewHolder.
     * This method handles all the day binding logic that was previously in bindDay.
     */
    private void bindMaterialDay(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {

        // Call the original binding logic from BaseAdapter
        bindOriginalDayData(holder, dayItem, position);

        // Add our material design enhancements
        applyMaterialBackground(holder, dayItem);

        // Add events indicator
        addEventsIndicator(holder, dayItem);
    }

    /**
     * Replicate the original BaseAdapter.bindDay logic for MaterialDayViewHolder.
     * This ensures we maintain all the original functionality.
     */
    private void bindOriginalDayData(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        Day day = dayItem.day;

        if (day == null) {
            // Empty day
            holder.tday.setText("");
            holder.twday.setText("");
            for (TextView shiftText : holder.shiftTexts) {
                if (shiftText != null) {
                    shiftText.setText("");
                    shiftText.setBackgroundColor(Color.TRANSPARENT);
                }
            }
            if (holder.ttR != null) {
                holder.ttR.setText("");
            }
            return;
        }

        // Set day number and day name
        holder.tday.setText(String.valueOf(day.getDayOfMonth()));
        holder.twday.setText(day.getDayOfWeekAsString());

        // Set shift information
        List<Shift> shifts = day.getShifts();
        for (int i = 0; i < holder.shiftTexts.length; i++) {
            TextView shiftText = holder.shiftTexts[i];
            if (shiftText == null) continue;

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

        // Set rest teams
        if (holder.ttR != null) {
            holder.ttR.setText(day.getOffWorkHalfTeamsAsString());
        }
    }

    /**
     * STEP 6: Enhanced Material Design backgrounds with event type colors.
     */
    private void applyMaterialBackground(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        View rootView = holder.itemView;

        // Get events for this day
        LocalDate date = dayItem.day != null ? dayItem.day.getLocalDate() : null;
        List<LocalEvent> events = date != null ? getEventsForDate(date) : new ArrayList<>();

        // STEP 6 FIX: Simplified background logic for better readability
        if (!events.isEmpty()) {
            // ONLY apply event colors when there are events
            int eventColor = getDominantEventTypeColor(events);
            int blendedColor = blendEventColorWithBackground(
                    getBaseBackgroundColor(), // Always use neutral base
                    eventColor
            );
            rootView.setBackgroundColor(blendedColor);

        } else if (dayItem.isToday()) {
            // Today without events: special today color
            rootView.setBackgroundColor(getColorByThemeAttr(mContext, R.attr.colorTodayUserBackground));

        } else {
            // STEP 6 FIX: All other days use neutral background for better readability
            rootView.setBackgroundColor(getBaseBackgroundColor());
        }
    }

// 2. AGGIUNGI QUESTO METODO HELPER:

    /**
     * STEP 6 FIX: Get neutral base background color.
     */
    private int getBaseBackgroundColor() {
        return getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface);
    }

// 2. AGGIUNGI QUESTO METODO PER BLEND COLORI:

    /**
     * STEP 6: Blend event color with background color for subtle indication.
     * Creates a translucent overlay effect.
     * STEP 6 FIX: More subtle color blending for better readability.
     */
    private int blendEventColorWithBackground(int backgroundColor, int eventColor) {
        // Extract RGB components
        int bgRed = android.graphics.Color.red(backgroundColor);
        int bgGreen = android.graphics.Color.green(backgroundColor);
        int bgBlue = android.graphics.Color.blue(backgroundColor);

        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        // STEP 6 FIX: Even more subtle blend (8% event color, 92% background)
        float eventWeight = 0.08f;  // Reduced from 0.15f
        float bgWeight = 0.92f;

        int blendedRed = (int) (bgRed * bgWeight + eventRed * eventWeight);
        int blendedGreen = (int) (bgGreen * bgWeight + eventGreen * eventWeight);
        int blendedBlue = (int) (bgBlue * bgWeight + eventBlue * eventWeight);

        // Ensure values are in valid range
        blendedRed = Math.max(0, Math.min(255, blendedRed));
        blendedGreen = Math.max(0, Math.min(255, blendedGreen));
        blendedBlue = Math.max(0, Math.min(255, blendedBlue));

        return android.graphics.Color.rgb(blendedRed, blendedGreen, blendedBlue);
    }


    /**
     * STEP 6: Get color for dominant event type.
     */
    private int getDominantEventTypeColor(List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            return ContextCompat.getColor(mContext, R.color.event_type_general);
        }

        EventType dominantType = EventType.GENERAL;
        int dominantScore = 0;

        for (LocalEvent event : events) {
            EventType eventType = event.getEventType();
            int typeScore = getEventTypeScore(eventType);

            if (typeScore > dominantScore) {
                dominantScore = typeScore;
                dominantType = eventType;
            }
        }

        return getEventTypeColor(dominantType);
    }


    /**
     * STEP 6: Get score for event type (higher = more critical).
     */
    private int getEventTypeScore(EventType eventType) {
        if (eventType == null) return 1;

        switch (eventType) {
            case STOP_UNPLANNED: return 10;  // Più critico
            case STOP_SHORTAGE: return 9;
            case STOP_PLANNED: return 8;
            case EMERGENCY: return 7;
            case MAINTENANCE: return 6;
            case MEETING: return 3;
            case TRAINING: return 2;
            case GENERAL:
            default: return 1;               // Meno critico
        }
    }

    /**
     * STEP 6: Get color for event type.
     */
    private int getEventTypeColor(EventType eventType) {
        if (eventType == null) {
            return ContextCompat.getColor(mContext, R.color.event_type_general);
        }

        switch (eventType) {
            case STOP_UNPLANNED:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_unplanned);  // Rosso
            case STOP_SHORTAGE:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_shortage);   // Viola
            case STOP_PLANNED:
                return ContextCompat.getColor(mContext, R.color.event_type_stop_planned);    // Arancione
            case MAINTENANCE:
                return ContextCompat.getColor(mContext, R.color.event_type_maintenance);     // Verde
            case EMERGENCY:
                return ContextCompat.getColor(mContext, R.color.error_color);                // Rosso scuro
            case MEETING:
                return ContextCompat.getColor(mContext, R.color.event_type_meeting);         // Blu
            case TRAINING:
                return ContextCompat.getColor(mContext, R.color.event_type_training);        // Arancione chiaro
            case GENERAL:
            default:
                return ContextCompat.getColor(mContext, R.color.event_type_general);         // Grigio
        }
    }

    /**
     * STEP 6: Enhanced working events indicator with improved colors.
     * STEP 6 FIX: Working events indicator method (was missing).
     */
    private void addWorkingEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.eventsIndicator == null || dayItem.day == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            // FIX: Instead of GONE, make it INVISIBLE (keeps space)
            holder.eventsIndicator.setVisibility(View.INVISIBLE);
            holder.eventsIndicator.setText(""); // Clear text
        } else {
            // Show indicator with count
            holder.eventsIndicator.setVisibility(View.VISIBLE);
            int count = events.size();
            holder.eventsIndicator.setText(count == 1 ? "1" : String.valueOf(count));

            // Set text color based on event type
            int textColor = getDominantEventTypeColor(events);
            holder.eventsIndicator.setTextColor(textColor);

            // Keep original background
            holder.eventsIndicator.setBackgroundResource(R.drawable.events_indicator_background);
        }
    }

    /**
     * STEP 6 FIX: Apply special styling for Sunday.
     */
    private void applySundaySpecialStyling(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (!dayItem.isSunday()) return;

        // STEP 6 FIX: Red text for Sunday day number
        if (holder.tday != null) {
            holder.tday.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            holder.tday.setTypeface(holder.tday.getTypeface(), android.graphics.Typeface.BOLD);
        }

        // STEP 6 FIX: Red text for Sunday day name
        if (holder.twday != null) {
            holder.twday.setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            holder.twday.setTypeface(holder.twday.getTypeface(), android.graphics.Typeface.BOLD);
        }

        // STEP 6 FIX: Red separator line for Sunday
        View separator = holder.itemView.findViewById(R.id.item_row_separator);
        if (separator != null) {
            separator.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
        }
    }

    /**
     * FIX: Reset ALL ViewHolder state to prevent RecyclerView reuse issues.
     * This method ALWAYS resets everything to default state.
     */
    private void resetAllViewHolderState(MaterialDayViewHolder holder) {

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

        // FIX: Reset separator line to normal color
        View separator = holder.itemView.findViewById(R.id.item_row_separator);
        if (separator != null) {
            separator.setBackgroundColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOutline));
        }

        // FIX: Reset events indicator
        if (holder.eventsIndicator != null) {
            holder.eventsIndicator.setVisibility(View.GONE);
            holder.eventsIndicator.setTextColor(getColorByThemeAttr(mContext,
                    androidx.appcompat.R.attr.colorPrimary));            holder.eventsIndicator.setBackgroundResource(R.drawable.events_indicator_background);
        }

        // FIX: Reset root background to default
        holder.itemView.setBackgroundColor(getBaseBackgroundColor());

        Log.v(TAG, "FIX: Reset all ViewHolder state for position");
    }


    private void addSimpleEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.eventsIndicator == null || dayItem.day == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();

        // STEP 2: Get events for this date (empty for now, will add data in STEP 3)
        List<LocalEvent> events = getEventsForDate(date);

        // STEP 2: Use EventIndicatorHelper to setup indicator
        mEventHelper.setupSimpleEventIndicator(holder.eventsIndicator, events);
    }

    /**
     * STEP 2: Get events for a specific date.
     * For now returns empty list - will be populated in STEP 3.
     */
    private List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> events = mEventsData.get(date);
        List<LocalEvent> result = events != null ? events : new ArrayList<>();

        // STEP 3: Debug logging
        if (!result.isEmpty()) {
            Log.d(TAG, "STEP 3: Found " + result.size() + " events for date " + date);
        }

        return result;
    }

    // 6. AGGIUNGI QUESTO METODO PUBLIC (per future integration):
    /**
     * STEP 2: Method to update events data (will be used in STEP 3).
     * For now just stores the data.
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        this.mEventsData = eventsMap != null ? eventsMap : new HashMap<>();

        Log.d(TAG, "STEP 4: Updated events data - " + mEventsData.size() + " dates with events");

        // Notify adapter to refresh indicators
        notifyDataSetChanged();
    }


    /**
     * Check if user has shift on this day.
     */
    private boolean hasUserShift(SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null || mUserHalfTeam == null) return false;
        return dayItem.day.getInWichTeamIsHalfTeam(mUserHalfTeam) >= 0;
    }



    /**
     * Add events indicator at bottom of row.
     * Updated to work with MaterialDayViewHolder.
     */
    private void addEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null || holder.eventsIndicator == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();
        Integer eventCount = mEventsCount.get(date);

        if (eventCount != null && eventCount > 0) {
            holder.eventsIndicator.setVisibility(View.VISIBLE);
            holder.eventsIndicator.setText(eventCount == 1 ? "1 evento" : eventCount + " eventi");
            holder.eventsIndicator.setTextColor(getColorByThemeAttr(mContext,
                    androidx.appcompat.R.attr.colorPrimary));
        } else {
            holder.eventsIndicator.setVisibility(View.GONE);
        }
    }



    /**
     * Update events count for days
     */
    public void updateEventsCount(Map<LocalDate, Integer> eventsCount) {
        mEventsCount = eventsCount != null ? eventsCount : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * Minimal events integration that doesn't break existing layout.
     * Add this method to SimpleEnhancedDaysListAdapter.
     */
    private void setupMinimalEventIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null || holder.eventsIndicator == null) {
            return;
        }

        LocalDate date = dayItem.day.getLocalDate();

        // For now, just show a simple indicator if there might be events
        // TODO: Replace with actual events data when integrated
        boolean hasEvents = false; // Placeholder logic

        if (hasEvents) {
            holder.eventsIndicator.setVisibility(View.VISIBLE);
            holder.eventsIndicator.setText("Eventi");
        } else {
            holder.eventsIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Enhanced ViewHolder with events indicator
     * Extends the base DayViewHolder to add event visual indicators.
     */
    public class MaterialDayViewHolder extends DayViewHolder {

        // Solo l'indicatore eventi testuale (che esiste nel layout)
        public TextView eventsIndicator;

        public MaterialDayViewHolder(@NonNull View itemView) {
            super(itemView);

            // FIX: Solo la TextView che esiste davvero nel layout
            eventsIndicator = itemView.findViewById(R.id.tv_events_indicator);

            // Hide by default
            if (eventsIndicator != null) {
                eventsIndicator.setVisibility(View.GONE);
            }

            Log.d(TAG, "FIX: MaterialDayViewHolder initialized correctly");
        }

    }



    /**
     * STEP 3: Debug method to check events data.
     */
    private void debugEventsData() {
        Log.d(TAG, "STEP 3: Events data contains " + mEventsData.size() + " dates");
        for (Map.Entry<LocalDate, List<LocalEvent>> entry : mEventsData.entrySet()) {
            LocalDate date = entry.getKey();
            List<LocalEvent> events = entry.getValue();
            Log.d(TAG, "STEP 3: Date " + date + " has " + events.size() + " events");
        }
    }

    /**
     * STEP 4: Load events from database asynchronously.
     */
    private void loadEventsFromDatabase() {
        if (mIsLoadingEvents.get()) {
            Log.d(TAG, "STEP 4: Events loading already in progress");
            return;
        }

        mIsLoadingEvents.set(true);
        Log.d(TAG, "STEP 4: Starting to load events from database");

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
                Log.d(TAG, "STEP 4: Loaded " + events.size() + " events from database");
                return events;

            } catch (Exception e) {
                Log.e(TAG, "STEP 4: Error loading events from database: " + e.getMessage());
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
            Log.e(TAG, "STEP 4: Failed to load events: " + throwable.getMessage());

            // Fallback to mock events if database fails
            if (mContext instanceof android.app.Activity) {
                ((android.app.Activity) mContext).runOnUiThread(() -> {
                    Log.d(TAG, "STEP 4: Using mock events as fallback");
                    createMockEventsForTesting();
                    mIsLoadingEvents.set(false);
                });
            }
            return null;
        });
    }

// 5. AGGIUNGI QUESTO METODO PER PROCESSARE GLI EVENTI CARICATI:
    // ==================== STEP 4 FIX: ESPANSIONE EVENTI MULTI-DAY ====================

// SOSTITUISCI IL METODO processLoadedEvents CON QUESTA VERSIONE CORRETTA:

    /**
     * STEP 4 FIX: Process events loaded from database and expand multi-day events.
     */
    private void processLoadedEvents(List<LocalEvent> events) {
        Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();

        // Process each event and expand multi-day events
        for (LocalEvent event : events) {
            expandEventAcrossDays(event, eventsMap);
        }

        // Update adapter with real data
        updateEventsData(eventsMap);

        Log.d(TAG, "STEP 4 FIX: Processed " + events.size() + " events into " + eventsMap.size() + " dates");

        // If no real events found, use mock as fallback
        if (eventsMap.isEmpty()) {
            Log.d(TAG, "STEP 4 FIX: No real events found, using mock events");
            createMockEventsForTesting();
        }
    }

// AGGIUNGI QUESTO NUOVO METODO:

    /**
     * STEP 4 FIX: Expand a single event across all days it covers.
     * This ensures multi-day events appear on every day they span.
     */
    private void expandEventAcrossDays(LocalEvent event, Map<LocalDate, List<LocalEvent>> eventsMap) {
        try {
            LocalDate startDate = event.getStartDate();
            LocalDate endDate = event.getEndDate();

            // Handle null end date
            if (endDate == null) {
                endDate = startDate;
            }

            // Ensure end date is not before start date
            if (endDate.isBefore(startDate)) {
                Log.w(TAG, "STEP 4 FIX: Event end date before start date, using start date only: " + event.getTitle());
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
                Log.d(TAG, "STEP 4 FIX: Expanded event '" + event.getTitle() + "' across " + dayCount + " days (" + startDate + " to " + endDate + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, "STEP 4 FIX: Error expanding event '" + event.getTitle() + "': " + e.getMessage());

            // Fallback: add to start date only
            LocalDate startDate = event.getStartDate();
            if (startDate != null) {
                eventsMap.computeIfAbsent(startDate, k -> new ArrayList<>()).add(event);
            }
        }
    }

// AGGIUNGI QUESTO METODO UTILITY:

    /**
     * STEP 4 FIX: Get the end date of an event safely.
     * Handles cases where endTime might be null or same day.
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
            Log.w(TAG, "STEP 4 FIX: Error getting end date for event, using start date: " + e.getMessage());
            return event.getStartDate();
        }
    }

// OPZIONALE: AGGIUNGI METODO PER CREARE EVENTI MULTI-DAY DI TEST:

    /**
     * STEP 4 FIX: Create mock events including multi-day events for testing.
     */
    private void createMockEventsForTesting() {
        Log.d(TAG, "STEP 4 FIX: Creating mock events with multi-day examples");

        Map<LocalDate, List<LocalEvent>> mockEvents = new HashMap<>();
        LocalDate today = LocalDate.now();

        try {
            // Single day event
            LocalEvent event1 = new LocalEvent("📋 Demo Event", today);
            event1.setDescription("Evento demo - singolo giorno");
            event1.setEventType(EventType.GENERAL);
            event1.setPriority(EventPriority.LOW);
            event1.setStartTime(today.atTime(9, 0));
            event1.setEndTime(today.atTime(17, 0));

            // Multi-day event (3 days)
            LocalEvent event2 = new LocalEvent("🔧 Manutenzione Estesa", today.plusDays(1));
            event2.setDescription("Manutenzione programmata su 3 giorni");
            event2.setEventType(EventType.MAINTENANCE);
            event2.setPriority(EventPriority.HIGH);
            event2.setStartTime(today.plusDays(1).atTime(8, 0));
            event2.setEndTime(today.plusDays(3).atTime(18, 0)); // 3 days total

            // Process events through expansion logic
            expandEventAcrossDays(event1, mockEvents);
            expandEventAcrossDays(event2, mockEvents);

            updateEventsData(mockEvents);

            Log.d(TAG, "STEP 4 FIX: Created fallback mock events with multi-day example");

        } catch (Exception e) {
            Log.e(TAG, "STEP 4 FIX: Error creating fallback mock events: " + e.getMessage());
        }
    }

// 6. AGGIUNGI QUESTO METODO PUBLIC PER REFRESH:
    /**
     * STEP 4: Public method to refresh events data.
     * Call this when events are added/modified in the database.
     */
    public void refreshEventsFromDatabase() {
        Log.d(TAG, "STEP 4: Refreshing events from database");
        mEventsData.clear();
        loadEventsFromDatabase();
    }
}