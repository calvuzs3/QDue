package net.calvuz.qdue.ui.dayslist;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

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
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.EventsMiniAdapter;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.calendar.CalendarAdapter;
import net.calvuz.qdue.ui.events.EventsAdapter;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.SharedViewModels;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ENHANCED DaysListAdapter with MaterialCardView unified design
 *
 * Features:
 * - MaterialCardView consistency with CalendarAdapter
 * - Events support for each day
 * - Performance optimizations for large datasets
 * - Unified background styling system
 */
public class EnhancedDaysListAdapter extends BaseAdapter {

    private static final String TAG = "EnhancedDaysListAdapter";

    // TODO: find out EVENT class
    // Events management
    private Map<LocalDate, List<LocalEvent>> mEventsMap = new HashMap<>();


    /* In attesa di sviluppo ulteriore */
//    private EventsAdapter.OnEventClickListener mEventClickListener;
    /**
     * FIX: EventAdapter - Replace missing EventsAdapter reference
     *
     * This replaces the EventsAdapter.OnEventClickListener reference
     * in EnhancedDaysListAdapter with the correct EventsMiniAdapter.OnEventClickListener
     */

// ==================== 1. CORRECT INTERFACE DECLARATION ====================

    /**
     * FIXED: Use correct interface in EnhancedDaysListAdapter
     * Replace the existing interface declaration
     */

// In EnhancedDaysListAdapter.java, change this line:
// private EventsAdapter.OnEventClickListener mEventClickListener;

// To this:
    private EventsMiniAdapter.OnEventClickListener mEventClickListener;




    // Performance optimization
    private RecyclerView.RecycledViewPool mEventsViewPool;



    // ==================== 2. UPDATED ENHANCED DAYSLIST ADAPTER METHODS ====================

    /**
     * FIXED: Constructor and methods in EnhancedDaysListAdapter
     * These are the corrected methods to replace in your existing adapter
     */

// Replace the constructor with:
    public EnhancedDaysListAdapter(Context context, List<SharedViewModels.ViewItem> items,
                                   HalfTeam userHalfTeam) {
        super(context, items, userHalfTeam, 3); // DaysList shows 3 shifts

        // Enable stable IDs for better performance
        setHasStableIds(true);

        // Initialize shared ViewPool for events RecyclerViews (performance boost)
        mEventsViewPool = new RecyclerView.RecycledViewPool();
        mEventsViewPool.setMaxRecycledViews(0, 20); // Max 20 event items cached
    }

// Replace the setEventClickListener method with:
    /**
     * Set event click listener
     */
    public void setEventClickListener(EventsMiniAdapter.OnEventClickListener listener) {
        mEventClickListener = listener;
    }
//
//    public EnhancedDaysListAdapter(Context context, List<SharedViewModels.ViewItem> items,
//                                   HalfTeam userHalfTeam) {
//        super(context, items, userHalfTeam, 3); // DaysList shows 3 shifts
//
//        // Initialize shared ViewPool for events RecyclerViews (performance boost)
//        mEventsViewPool = new RecyclerView.RecycledViewPool();
//        mEventsViewPool.setMaxRecycledViews(0, 20); // Max 20 event items cached
//    }

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use enhanced dayslist layout with MaterialCardView
        View view = inflater.inflate(R.layout.item_dayslist_row, parent, false);
        return new EnhancedDayViewHolder(view);
    }

    // ==================== FIX 3: REMOVE PROBLEMATIC USER SHIFT HIGHLIGHTING ====================

    /**
     * REMOVED: The applyUserShiftHighlight method was causing over-highlighting
     * Replace the bindDay method to remove this call
     */
    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        if (!(holder instanceof EnhancedDayViewHolder)) {
            super.bindDay(holder, dayItem, position);
            return;
        }

        EnhancedDayViewHolder enhancedHolder = (EnhancedDayViewHolder) holder;
        Day day = dayItem.day;

        if (day == null) {
            setupEmptyRow(enhancedHolder);
            return;
        }

        // Setup basic day info
        setupDayInfo(enhancedHolder, day, dayItem);

        // Setup shift columns (FIXED)
        setupShiftColumns(enhancedHolder, day);

        // Apply unified background styling (FIXED)
        setupUnifiedBackground(enhancedHolder.itemView, dayItem);

        // Setup events (safe)
        setupEventsSafe(enhancedHolder, day, dayItem);
    }
//
//    @Override
//    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
//        if (!(holder instanceof EnhancedDayViewHolder)) {
//            super.bindDay(holder, dayItem, position);
//            return;
//        }
//
//        EnhancedDayViewHolder enhancedHolder = (EnhancedDayViewHolder) holder;
//        Day day = dayItem.day;
//
//        if (day == null) {
//            setupEmptyRow(enhancedHolder);
//            return;
//        }
//
//        // Setup basic day info
//        setupDayInfo(enhancedHolder, day, dayItem);
//
//        // Setup shift columns
//        setupShiftColumns(enhancedHolder, day);
//
//        // Apply unified MaterialCardView background styling
//        setupUnifiedBackground(enhancedHolder.itemView, dayItem);
//
//        // Setup events (lazy loading for performance)
//        setupEvents(enhancedHolder, day, dayItem);
//    }

    // ==================== FIX 2: CORRECT BACKGROUND HIGHLIGHTING ====================

    /**
     * FIXED: Apply background highlighting only when appropriate
     * Replace this method in EnhancedDaysListAdapter
     */
    private void setupUnifiedBackground(View itemView, SharedViewModels.DayItem dayItem) {
        if (!(itemView instanceof com.google.android.material.card.MaterialCardView)) {
            // Handle regular layout if not MaterialCardView
            setupRegularBackground(itemView, dayItem);
            return;
        }

        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) itemView;

        // FIXED: Only apply special styling for special days or user shifts
        if (dayItem.isToday()) {
            setupTodayRowStyle(cardView);
        } else if (dayItem.isSunday()) {
            setupSundayRowStyle(cardView);
        } else if (hasUserShift(dayItem)) {
            // Only highlight if user has a shift this day
            setupUserShiftRowStyle(cardView);
        } else {
            // Regular day - minimal styling
            setupRegularRowStyle(cardView);
        }
    }

    /**
     * Check if user has a shift on this day
     */
    private boolean hasUserShift(SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null || mUserHalfTeam == null) return false;
        return dayItem.day.getInWichTeamIsHalfTeam(mUserHalfTeam) >= 0;
    }

    /**
     * Setup styling for days when user has a shift
     */
    private void setupUserShiftRowStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setCardBackgroundColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface));
        cardView.setStrokeColor(getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary));
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(1f); // Slight elevation for user shift days
        cardView.setRippleColor(ColorStateList.valueOf(
                getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary) & 0x20FFFFFF));
    }

    /**
     * FIXED: Regular row styling - minimal highlighting
     */
    private void setupRegularRowStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setCardBackgroundColor(Color.TRANSPARENT); // No background for regular days
        cardView.setStrokeColor(Color.TRANSPARENT); // No stroke for regular days
        cardView.setStrokeWidth(0);
        cardView.setCardElevation(0f);
        cardView.setRippleColor(ColorStateList.valueOf(
                getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary) & 0x10FFFFFF));
    }

    /**
     * Handle regular background for non-MaterialCardView layouts
     */
    private void setupRegularBackground(View itemView, SharedViewModels.DayItem dayItem) {
        if (dayItem.isToday()) {
            itemView.setBackgroundColor(mCachedTodayBackgroundColor);
        } else if (dayItem.isSunday()) {
            itemView.setBackgroundColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorSurfaceVariant));
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }
//
//    /**
//     * UNIFIED: Setup MaterialCardView background (same system as CalendarAdapter)
//     */
//    private void setupUnifiedBackground(View itemView, SharedViewModels.DayItem dayItem) {
//        if (!(itemView instanceof com.google.android.material.card.MaterialCardView)) {
//            return;
//        }
//
//        com.google.android.material.card.MaterialCardView cardView =
//                (com.google.android.material.card.MaterialCardView) itemView;
//
//        if (dayItem.isToday()) {
//            setupTodayRowStyle(cardView);
//        } else if (dayItem.isSunday()) {
//            setupSundayRowStyle(cardView);
//        } else {
//            setupRegularRowStyle(cardView);
//        }
//
//        // Apply user shift highlighting
//        applyUserShiftHighlight(cardView, dayItem);
//    }
//
//    /**
//     * Regular row styling
//     */
//    private void setupRegularRowStyle(com.google.android.material.card.MaterialCardView cardView) {
//        cardView.setCardBackgroundColor(
//                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface));
//        cardView.setStrokeColor(
//                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOutlineVariant));
//        cardView.setStrokeWidth(1);
//        cardView.setCardElevation(0f);
//        cardView.setRippleColor(ColorStateList.valueOf(
//                getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary) & 0x20FFFFFF));
//    }

    /**
     * Today row styling (consistent with calendar)
     */
    private void setupTodayRowStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setCardBackgroundColor(mCachedTodayBackgroundColor);
        cardView.setStrokeColor(getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary));
        cardView.setStrokeWidth(2);
        cardView.setCardElevation(2f); // Slight elevation for today
        cardView.setRippleColor(ColorStateList.valueOf(
                getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary) & 0x20FFFFFF));
    }

    /**
     * Sunday row styling
     */
    private void setupSundayRowStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setCardBackgroundColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurfaceVariant));
        cardView.setStrokeColor(mCachedSundayTextColor);
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(0f);
        cardView.setRippleColor(ColorStateList.valueOf(mCachedSundayTextColor & 0x30FFFFFF));
    }

    /**
     * User shift highlighting (same logic as calendar)
     */
    private void applyUserShiftHighlight(com.google.android.material.card.MaterialCardView cardView,
                                         SharedViewModels.DayItem dayItem) {
        Day day = dayItem.day;
        if (day == null || mUserHalfTeam == null) return;

        int userShiftPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
        if (userShiftPosition < 0) return;

        // Increase stroke for user shifts
        int currentStrokeWidth = (int) cardView.getStrokeWidth();
        cardView.setStrokeWidth(Math.max(2, currentStrokeWidth + 1));

        // Subtle background tint
        int currentBgColor = cardView.getCardBackgroundColor().getDefaultColor();
        int blendedColor = blendColorsPerformant(currentBgColor, mCachedUserShiftBackgroundColor, 0.15f);
        cardView.setCardBackgroundColor(blendedColor);
    }

    /**
     * Setup basic day information
     */
    private void setupDayInfo(EnhancedDayViewHolder holder, Day day, SharedViewModels.DayItem dayItem) {
        // Day number
        holder.tday.setText(String.valueOf(day.getDayOfMonth()));

        // Day of week
        String dayOfWeek = day.getLocalDate().getDayOfWeek()
                .getDisplayName(TextStyle.FULL, QDue.getLocale());
        holder.twday.setText(dayOfWeek);

        // Enhanced text styling
        if (dayItem.isToday()) {
            holder.tday.setTextColor(getColorByThemeAttr(mContext, R.attr.colorOnTodayBackground));
            holder.twday.setTextColor(getColorByThemeAttr(mContext, R.attr.colorOnTodayBackground));
            holder.tday.setTypeface(holder.tday.getTypeface(), Typeface.BOLD);
        } else if (dayItem.isSunday()) {
            holder.tday.setTextColor(mCachedSundayTextColor);
            holder.twday.setTextColor(mCachedSundayTextColor);
            holder.tday.setTypeface(holder.tday.getTypeface(), Typeface.BOLD);
        } else {
            holder.tday.setTextColor(mCachedNormalTextColor);
            holder.twday.setTextColor(mCachedNormalTextColor);
            holder.tday.setTypeface(holder.tday.getTypeface(), Typeface.NORMAL);
        }
    }



    /**
     * CRITICAL FIXES for EnhancedDaysListAdapter
     *
     * Fixes the wrong data display by:
     * 1. Showing half teams (A,B,C,D,E) instead of shift type names
     * 2. Fixing background highlighting logic
     * 3. Correctly clearing unused shift columns
     */

// ==================== FIX 1: CORRECT SHIFT TEXT DISPLAY ====================

    /**
     * FIXED: Setup shift columns to show half teams like original adapter
     * Replace this entire method in EnhancedDaysListAdapter
     */
    private void setupShiftColumns(EnhancedDayViewHolder holder, Day day) {
        TextView[] shiftTextViews = {holder.tt1, holder.tt2, holder.tt3};

        // Clear all shift columns first
        for (TextView shiftView : shiftTextViews) {
            shiftView.setText("");
            shiftView.setBackgroundColor(Color.TRANSPARENT);
            shiftView.setTextColor(mCachedNormalTextColor);
            shiftView.setTypeface(shiftView.getTypeface(), Typeface.NORMAL);
        }

        // Fill shift columns with actual shift data
        for (int i = 0; i < Math.min(shiftTextViews.length, day.getShifts().size()); i++) {
            TextView shiftView = shiftTextViews[i];
            Shift shift = day.getShifts().get(i);

            // FIXED: Show half teams like original adapter
            String halfTeamsText = getHalfTeamsText(shift);
            shiftView.setText(halfTeamsText);

            // Set shift background color
            int backgroundColor = shift.getShiftType().getColor();
            shiftView.setBackgroundColor(backgroundColor);

            // FIXED: Calculate readable text color
            int textColor = calculateTextColorForBackground(backgroundColor);
            shiftView.setTextColor(textColor);

            // FIXED: Highlight user shift properly
            if (mUserHalfTeam != null && shift.containsHalfTeam(mUserHalfTeam)) {
                shiftView.setTypeface(shiftView.getTypeface(), Typeface.BOLD);
                // Make text white for user shift for better visibility
                shiftView.setTextColor(Color.WHITE);
            }
        }

        // Setup rest teams
        setupRestTeams(holder, day);
    }

    /**
     * Get half teams text for a shift (like original adapter)
     */
    private String getHalfTeamsText(Shift shift) {
        Set<HalfTeam> halfTeams = shift.getHalfTeams();
        if (halfTeams == null || halfTeams.isEmpty()) {
            return ""; // Empty if no teams
        }

        StringBuilder text = new StringBuilder();
        for (HalfTeam halfTeam : halfTeams) text.append(halfTeam.getShortName());
        return text.toString();
    }

    /**
     * Calculate readable text color for background
     */
    private int calculateTextColorForBackground(int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

//    /**
//     * Setup shift columns (existing logic enhanced)
//     */
//    private void setupShiftColumns(EnhancedDayViewHolder holder, Day day) {
//        TextView[] shiftTextViews = {holder.tt1, holder.tt2, holder.tt3};
//
//        for (int i = 0; i < Math.min(shiftTextViews.length, day.getShifts().size()); i++) {
//            TextView shiftView = shiftTextViews[i];
//            Shift shift = day.getShifts().get(i);
//
//            // Set shift text
//            shiftView.setText(shift.getShiftType().getShortName());
//
//            // Set shift colors
//            shiftView.setBackgroundColor(shift.getShiftType().getColor());
//            shiftView.setTextColor(shift.getShiftType().getTextColor());
//
//            // Highlight user shift
//            if (mUserHalfTeam != null && shift.containsHalfTeam(mUserHalfTeam)) {
//                shiftView.setTypeface(shiftView.getTypeface(), Typeface.BOLD);
//                // Add subtle border for user shift
//                shiftView.setBackgroundResource(R.drawable.user_shift_cell_background);
//            }
//        }
//
//        // Setup rest teams
//        setupRestTeams(holder, day);
//    }

    /**
     * Setup rest teams column
     */
    private void setupRestTeams(EnhancedDayViewHolder holder, Day day) {
        List<String> restTeams = new ArrayList<>();

        // Collect teams not in active shifts
        for (Shift shift : day.getShifts()) {
            if (shift.getShiftType().isRestType()) {
                for (HalfTeam team : shift.getHalfTeams()) {
                    restTeams.add(team.getShortName());
                }
            }
        }

        if (!restTeams.isEmpty()) {
            holder.ttR.setText(String.join(",", restTeams));
            holder.ttR.setVisibility(View.VISIBLE);
        } else {
            holder.ttR.setVisibility(View.GONE);
        }
    }

    /**
     * PERFORMANCE: Setup events with lazy loading
     */
    private void setupEvents(EnhancedDayViewHolder holder, Day day, SharedViewModels.DayItem dayItem) {
        LocalDate date = day.getLocalDate();
        List<LocalEvent> localEvents = mEventsMap.get(date);

        if (localEvents == null || localEvents.isEmpty()) {
            // No localEvents - hide container and indicator
            holder.llEventsContainer.setVisibility(View.GONE);
            holder.tvEventsIndicator.setVisibility(View.GONE);
            return;
        }

        // Setup localEvents indicator (collapsed state)
        String eventsText = localEvents.size() == 1 ? "1 evento" : localEvents.size() + " eventi";
        holder.tvEventsIndicator.setText(eventsText);
        holder.tvEventsIndicator.setVisibility(View.VISIBLE);

        // Setup expand/collapse click listener
        holder.tvEventsIndicator.setOnClickListener(v -> toggleEventsVisibility(holder, localEvents));

        // Keep container collapsed by default for performance
        holder.llEventsContainer.setVisibility(View.GONE);
    }


// ==================== FIX 4: SAFE EVENTS HANDLING ====================

    /**
     * FIXED: Safe events setup that handles missing views
     */
    private void setupEventsSafe(EnhancedDayViewHolder holder, Day day, SharedViewModels.DayItem dayItem) {
        // Check if events views exist (they may not in original layout)
        if (holder.llEventsContainer == null || holder.tvEventsIndicator == null) {
            return; // Skip events if views don't exist
        }

        LocalDate date = day.getLocalDate();
        List<LocalEvent> localEvents = mEventsMap.get(date);

        if (localEvents == null || localEvents.isEmpty()) {
            // No events - hide container and indicator
            holder.llEventsContainer.setVisibility(View.GONE);
            holder.tvEventsIndicator.setVisibility(View.GONE);
            return;
        }

        // Setup events indicator (collapsed state)
        String eventsText = localEvents.size() == 1 ? "1 evento" : localEvents.size() + " eventi";
        holder.tvEventsIndicator.setText(eventsText);
        holder.tvEventsIndicator.setVisibility(View.VISIBLE);

        // Setup expand/collapse click listener
        holder.tvEventsIndicator.setOnClickListener(v -> toggleEventsVisibility(holder, localEvents));

        // Keep container collapsed by default for performance
        holder.llEventsContainer.setVisibility(View.GONE);
    }

    /**
     * Toggle localEvents visibility with animation
     */
    private void toggleEventsVisibility(EnhancedDayViewHolder holder, List<LocalEvent> localEvents) {
        if (holder.llEventsContainer.getVisibility() == View.GONE) {
            // Expand: Setup RecyclerView and show
            setupEventsRecyclerView(holder, localEvents);
            holder.llEventsContainer.setVisibility(View.VISIBLE);
            holder.tvEventsIndicator.setText("Nascondi eventi");
        } else {
            // Collapse: Hide and clear RecyclerView
            holder.llEventsContainer.setVisibility(View.GONE);
            holder.rvEvents.setAdapter(null); // Free memory
            String eventsText = localEvents.size() == 1 ? "1 evento" : localEvents.size() + " eventi";
            holder.tvEventsIndicator.setText(eventsText);
        }
    }

    /**
     * PERFORMANCE: Setup localEvents RecyclerView with shared ViewPool
     */
    private void setupEventsRecyclerView(EnhancedDayViewHolder holder, List<LocalEvent> localEvents) {
        if (holder.rvEvents.getAdapter() != null) return; // Already setup

        EventsMiniAdapter adapter = new EventsMiniAdapter(mContext, localEvents, mEventClickListener);
        holder.rvEvents.setAdapter(adapter);
        holder.rvEvents.setRecycledViewPool(mEventsViewPool); // Share ViewPool for performance
    }


// ==================== FIX 5: SAFE EMPTY ROW SETUP ====================

    /**
     * FIXED: Safe empty row setup
     */
    private void setupEmptyRow(EnhancedDayViewHolder holder) {
        // Clear basic info
        holder.tday.setText("");
        holder.twday.setText("");

        // Clear shift columns
        holder.tt1.setText("");
        holder.tt2.setText("");
        holder.tt3.setText("");
        holder.ttR.setText("");

        // Reset backgrounds
        holder.tt1.setBackgroundColor(Color.TRANSPARENT);
        holder.tt2.setBackgroundColor(Color.TRANSPARENT);
        holder.tt3.setBackgroundColor(Color.TRANSPARENT);

        // Hide events (safely)
        if (holder.llEventsContainer != null) {
            holder.llEventsContainer.setVisibility(View.GONE);
        }
        if (holder.tvEventsIndicator != null) {
            holder.tvEventsIndicator.setVisibility(View.GONE);
        }
    }
//
//    /**
//     * Setup empty row
//     */
//    private void setupEmptyRow(EnhancedDayViewHolder holder) {
//        holder.tday.setText("");
//        holder.twday.setText("");
//        holder.tt1.setText("");
//        holder.tt2.setText("");
//        holder.tt3.setText("");
//        holder.ttR.setText("");
//        holder.llEventsContainer.setVisibility(View.GONE);
//        holder.tvEventsIndicator.setVisibility(View.GONE);
//    }

    /**
     * Performance-optimized color blending
     */
    private int blendColorsPerformant(int color1, int color2, float ratio) {
        int invRatio = (int) ((1f - ratio) * 256);
        int ratioInt = (int) (ratio * 256);

        int r = ((Color.red(color1) * invRatio) + (Color.red(color2) * ratioInt)) >> 8;
        int g = ((Color.green(color1) * invRatio) + (Color.green(color2) * ratioInt)) >> 8;
        int b = ((Color.blue(color1) * invRatio) + (Color.blue(color2) * ratioInt)) >> 8;

        return Color.rgb(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
        );
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Update events data
     */
    public void updateEvents(Map<LocalDate, List<LocalEvent>> eventsMap) {
        mEventsMap = eventsMap != null ? eventsMap : new HashMap<>();
        notifyDataSetChanged();
    }

//    /**
//     * Set event click listener
//     */
//    public void setEventClickListener(EventsMiniAdapter.OnEventClickListener listener) {
//        mEventClickListener = listener;
//    }
//    public void setEventClickListener(EventsAdapter.OnEventClickListener listener) {
//        mEventClickListener = listener;
//    }

    // ==================== ENHANCED VIEW HOLDER ====================

    /**
     * Enhanced ViewHolder with MaterialCardView and events support
     */
    public class EnhancedDayViewHolder extends DayViewHolder {
        // Basic day info
        public final TextView tday;
        public final TextView twday;

        // Shift columns
        public final TextView tt1;
        public final TextView tt2;
        public final TextView tt3;
        public final TextView ttR;

        // Events components
        public final LinearLayout llEventsContainer;
        public final TextView tvEventsHeader;
        public final RecyclerView rvEvents;
        public final TextView tvEventsIndicator;

        public EnhancedDayViewHolder(@NonNull View itemView) {
            super(itemView);

            // Basic day info
            tday = itemView.findViewById(R.id.tday);
            twday = itemView.findViewById(R.id.twday);

            // Shift columns
            tt1 = itemView.findViewById(R.id.tt1);
            tt2 = itemView.findViewById(R.id.tt2);
            tt3 = itemView.findViewById(R.id.tt3);
            ttR = itemView.findViewById(R.id.ttR);

            // Events components
            llEventsContainer = itemView.findViewById(R.id.ll_events_container);
            tvEventsHeader = itemView.findViewById(R.id.tv_events_header);
            rvEvents = itemView.findViewById(R.id.rv_events);
            tvEventsIndicator = itemView.findViewById(R.id.tv_events_indicator);

            // Initialize MaterialCardView for optimal performance
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
//                CalendarAdapter.initializeCardView(mContext, (com.google.android.material.card.MaterialCardView) itemView);
            }

            // Accessibility
            itemView.setMinimumHeight(
                    (int) (56 * itemView.getContext().getResources().getDisplayMetrics().density)
            );
        }
    }


    // ==================== 5. ENHANCED DAYSLIST ADAPTER EVENTS INTEGRATION ====================

/**
 * COMPLETE: Methods to add to EnhancedDaysListAdapter for events integration
 */

// Add this method to EnhancedDaysListAdapter:
    /**
     * Update events data for the adapter
     */
    public void updateEventsData(List<LocalEvent> events) {
        // Convert list to map grouped by date
        Map<LocalDate, List<LocalEvent>> eventsMap = events.stream()
                .collect(Collectors.groupingBy(LocalEvent::getDate));

        updateEvents(eventsMap);
    }

// Add this method to get events for a specific day:
    /**
     * Get events for a specific date (for integration with your data loading)
     */
    private List<LocalEvent> getEventsForDate(LocalDate date) {
        List<LocalEvent> dayEvents = mEventsMap.get(date);
        return dayEvents != null ? dayEvents : new ArrayList<>();
    }

}