package net.calvuz.qdue.ui.dayslist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import net.calvuz.qdue.ui.shared.DayLongClickListener;
import net.calvuz.qdue.ui.shared.EventIndicatorHelper;
import net.calvuz.qdue.ui.shared.HighlightingHelper;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.ui.shared.ToolbarAction;
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
 * Enhanced DaysListAdapter with Material Design compliance and events support
 * Minimal changes to the proven original adapter
 * <p>
 * Extends original DaysListAdapter with:
 * - Material Design compliant backgrounds
 * - Events indicator at bottom of rows
 * - Minimal changes to proven working code
 */
public class DaysListAdapterLegacy extends BaseClickAdapterLegacy {

    // TAG
    private static final String TAG = "D-Adapter";

    // Simple events tracking (just count for now)
    private Map<LocalDate, List<LocalEvent>> mEventsData = new HashMap<>();
    private Map<LocalDate, Integer> mEventsCount = new HashMap<>();

    // Event Indicator
    private final EventIndicatorHelper mEventHelper;

    // Database integration fields
    private final QDueDatabase mEventsDatabase;
    private final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);

    /// //////////////////////////////////////////////////////////////////////////////////

    public DaysListAdapterLegacy(Context context, List<SharedViewModels.ViewItem> items,
                                 HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);

        // Initialize event helper
        mEventHelper = new EventIndicatorHelper(context);

        // Initialize database and load real events
        mEventsDatabase = QDueDatabase.getInstance(context);
        loadEventsFromDatabase();

        Log.d(TAG, "DayslistAdapterLegacy: ✅ initialized with BaseClickAdapterLegacy");
    }

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use original layout - no changes needed
        View view = inflater.inflate(R.layout.item_dayslist_row, parent, false);
        return new MaterialDayViewHolder((MaterialCardView) view);
    }

    @Override
    protected void bindDay(DayViewHolder dayHolder, SharedViewModels.DayItem dayItem, int position) {
        Log.v(TAG, "bindDay: " + dayItem.day.getLocalDate());

        // Call parent to do ALL the work
        super.bindDay(dayHolder, dayItem, position);

        MaterialCardView holder = dayHolder.mView;

        // Only add our enhancements if it's our ViewHolder
        if (dayHolder instanceof MaterialDayViewHolder materialHolder) {


            // NEW: Setup long-click and selection support
            setupLongClickSupport(materialHolder, dayItem, position);

            // STEP 1: Reset visual state
            resetDayslistCellState(materialHolder);

            // STEP 2: Setup day number (top-left, smaller font)

            // STEP 3: Setup events indicator (top-right, dot with badge)
            setupEventsIndicator(materialHolder, dayItem);

            // STEP 4: Setup shift name and indicator (bottom-right area)
            setupShiftDisplay(materialHolder, dayItem);

            // STEP 5: Apply today/special day styling

            // STEP 6. Apply background styling (improved)
            //applyMaterialBackground(materialHolder, dayItem);
            applyMaterialBackgroundWithWhite(materialHolder, dayItem);

            // STEP 7. Add events indicator
            addWorkingEventsIndicator(materialHolder, dayItem);

            // STEP 8. Background Styling
            applyBackgroundStyling(holder, dayItem);

            // 🔧 DEBUG: Log per verifica
            Log.d(TAG, "bindDay completed for position " + position +
                    ", date: " + (dayItem.day != null ? dayItem.day.getLocalDate() : "null") +
                    ", ViewHolder: MaterialDayViewHolder");
        }
    }

    // ===========================================
    // Toolbar Action Handling
    // ===========================================

    /**
     * Handle toolbar action execution
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
     */
    private void createQuickEvent(String title, LocalDate date, EventType eventType) {
        Log.d(TAG, "Creating quick event: " + title + " for date: " + date);

        // TODO: Create actual LocalEvent and save to database
        // For now, just log the action

        // Example implementation:
        /*
        LocalEvent quickEvent = new LocalEvent();
        quickEvent.setTitle(title);
        quickEvent.setStartDate(date);
        quickEvent.setEndDate(date);
        quickEvent.setEventType(eventType);
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
     */
    private void resetDayslistCellState(MaterialDayViewHolder holder) {
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
     */
    private void setupEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
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
     */
    private void setupShiftDisplay(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
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
//    /**
//     * Bind data specifically for MaterialDayViewHolder.
//     * This method handles all the day binding logic that was previously in bindDay.
//     */
//    private void bindMaterialDay(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
//
//        // Call the original binding logic from BaseAdapterLegacy
//        bindOriginalDayData(holder, dayItem, position);
//
//        // Add our material design enhancements
//        //applyMaterialBackground(holder, dayItem);
//        applyMaterialBackgroundWithWhite(holder, dayItem);
//
//        // Add events indicator
//        addEventsIndicator(holder, dayItem);
//    }
//
//    /**
//     * Replicate the original BaseAdapterLegacy.bindDay logic for MaterialDayViewHolder.
//     * This ensures we maintain all the original functionality.
//     */
//    private void bindOriginalDayData(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
//        Day day = dayItem.day;
//
//        if (day == null) {
//            // Empty day
//            holder.tday.setText("");
//            holder.twday.setText("");
//            for (TextView shiftText : holder.shiftTexts) {
//                if (shiftText != null) {
//                    shiftText.setText("");
//                    shiftText.setBackgroundColor(Color.TRANSPARENT);
//                }
//            }
//            if (holder.ttR != null) {
//                holder.ttR.setText("");
//            }
//            return;
//        }
//
//        // Set day number and day name
//        holder.tday.setText(String.valueOf(day.getDayOfMonth()));
//        holder.twday.setText(day.getDayOfWeekAsString());
//
//        // Set shift information
//        List<Shift> shifts = day.getShifts();
//        for (int i = 0; i < holder.shiftTexts.length; i++) {
//            TextView shiftText = holder.shiftTexts[i];
//            if (shiftText == null) continue;
//
//            if (i < shifts.size()) {
//                Shift shift = shifts.get(i);
//                shiftText.setText(shift.getTeamsAsString());
//
//                // Apply shift colors if needed
//                if (mUserHalfTeam != null && shift.containsHalfTeam(mUserHalfTeam)) {
//                    shiftText.setBackgroundColor(shift.getShiftType().getColor());
//                } else {
//                    shiftText.setBackgroundColor(Color.TRANSPARENT);
//                }
//            } else {
//                shiftText.setText("");
//                shiftText.setBackgroundColor(Color.TRANSPARENT);
//            }
//        }
//
//        // Set rest teams
//        if (holder.ttR != null) {
//            holder.ttR.setText(day.getOffWorkHalfTeamsAsString());
//        }
//    }

    /**
     * Apply background styling for visual hierarchy
     * Provides subtle backgrounds for better calendar readability
     */
    private void applyBackgroundStyling(MaterialCardView cardView, SharedViewModels.DayItem dayItem) {

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        LocalDate today = LocalDate.now();

        // NUOVO: Controllare se ci sono eventi per questa data
        List<LocalEvent> events = getEventsForDate(date);

        if (date.equals(today)) {
            // Today: Special background with elevation
            HighlightingHelper.setupTodayCardStyle(mContext, cardView);
        } else if (date.getDayOfWeek().getValue() == 7) { // Sunday
            // Sunday: Light background highlighting
            HighlightingHelper.setupSundayCardStyle(mContext, cardView);
        } else if (!events.isEmpty()) {
            // Events: colored background
            HighlightingHelper.setupEventsCardStyle(mContext, mEventHelper, cardView, events);
        } else {
            // Regular days: Standard subtle background
            HighlightingHelper.setupRegularCardStyle(mContext, cardView);
        }

        if (date.isBefore(today)) {
            // Past days - slightly faded
            HighlightingHelper.setupOldDaysCardStyle(cardView);
        }
    }

    /**
     * Usare il blend con bianco nella applyMaterialBackground:
     */
    private void applyMaterialBackgroundWithWhite(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) holder.itemView;
        View rootView = holder.itemView;

        LocalDate date = dayItem.day != null ? dayItem.day.getLocalDate() : null;
        LocalDate today = LocalDate.now();

        List<LocalEvent> events = date != null ? getEventsForDate(date) : new ArrayList<>();

        // Apply special SUNDAY styling ONLY if it's Sunday
        if (dayItem.isSunday()) {
//            applySundaySpecialStyling(holder, dayItem);
        }

        // Apply TODAY STYLING
        if (dayItem.isToday()) {
            HighlightingHelper.setupTodayCardStyle(mContext, cardView);
        }

    }

    /**
     * HELPER - Get neutral base background color.
     */
    private int getBaseBackgroundColor() {
        return getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface);
    }

// 2. AGGIUNGI QUESTO METODO PER BLEND COLORI:

    /**
     * STEP 6: Blend event color with background color for subtle indication.
     * Creates a translucent overlay effect.
     * STEP 6 FIX: More subtle color blending for better readability.
     * STEP 6 FIX: Blend molto più sottile per leggibilità
     */
    private int blendEventColorWithBackground(int backgroundColor, int eventColor) {
        // Extract RGB components
        int bgRed = android.graphics.Color.red(backgroundColor);
        int bgGreen = android.graphics.Color.green(backgroundColor);
        int bgBlue = android.graphics.Color.blue(backgroundColor);

        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        // FIX CONTRAST: Blend ancora più sottile (3% event color, 97% background)
        float eventWeight = 0.03f;  // Ridotto drasticamente da 0.08f
        float bgWeight = 0.97f;

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
     * ALTERNATIVA: Blend colore evento con bianco per garantire chiarezza
     */
    private int blendEventColorWithWhite(int eventColor) {
        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        // Blend con bianco (95% bianco, 5% evento)
        float eventWeight = 0.16f;
        float whiteWeight = 0.84f;

        int blendedRed = (int) (255 * whiteWeight + eventRed * eventWeight);
        int blendedGreen = (int) (255 * whiteWeight + eventGreen * eventWeight);
        int blendedBlue = (int) (255 * whiteWeight + eventBlue * eventWeight);

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
            case STOP_UNPLANNED:
                return 10;  // Più critico
            case STOP_SHORTAGE:
                return 9;
            case STOP_PLANNED:
                return 8;
            case EMERGENCY:
                return 7;
            case MAINTENANCE:
                return 6;
            case MEETING:
                return 3;
            case TRAINING:
                return 2;
            case GENERAL:
            default:
                return 1;               // Meno critico
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
     * FIX: Migliorare styling del badge eventi per maggiore visibilità
     */
    private void addWorkingEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
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
     * Helper per colore testo contrastante
     */
    private int getContrastingTextColor(int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
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

//        // STEP 6 FIX: Red separator line for Sunday
//        View separator = holder.itemView.findViewById(R.id.item_row_separator);
//        if (separator != null) {
//            separator.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
//        }
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
     * Get events for a specific date.
     * For now returns empty list - will be populated in STEP 3.
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
     * STEP 2: Method to update events data (will be used in STEP 3).
     * For now just stores the data.
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        final String mTAG = "updateEventsData: ";

        this.mEventsData = eventsMap != null ? eventsMap : new HashMap<>();

        Log.d(TAG, mTAG + "Updated events data - " + mEventsData.size() + " dates with events");

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
     * Add this method to DaysListAdapterLegacy.
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
    public class MaterialDayViewHolder extends BaseMaterialDayViewHolder  {
        final String mTAG = TAG + "MaterialDayViewHolder: ";

        // Solo l'indicatore eventi testuale (che esiste nel layout)
        public TextView eventsIndicator;


        public MaterialDayViewHolder(@NonNull MaterialCardView itemView) {
            super(itemView);

            // FIX: Solo la TextView che esiste davvero nel layout
            eventsIndicator = itemView.findViewById(R.id.tv_events_indicator);

            // Hide by default
            if (eventsIndicator != null) {
                eventsIndicator.setVisibility(View.GONE);
            }

            Log.v(TAG, mTAG + "initialized");
        }
    }

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
     * Expand a single event across all days it covers.
     * This ensures multi-day events appear on every day they span.
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