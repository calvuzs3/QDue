package net.calvuz.qdue.ui.shared;

import static net.calvuz.qdue.QDue.Debug.DEBUG_BASEADAPTER;
import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
import net.calvuz.qdue.ui.shared.utils.EventIndicatorHelper;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base unified adapter for both DaysList and Calendar views.
 * <p>
 * This abstract class provides common functionality for displaying shift schedule data
 * in RecyclerView components. Subclasses only need to specialize specific binding logic
 * while maintaining shared functionality.
 * <p>
 * The adapter supports multiple view types including:
 * - Month headers
 * - Day items with shift information
 * - Loading indicators
 * - Empty placeholder items
 * <p>
 * Features:
 * - Color theme support with cached theme colors
 * - User team highlighting
 * - Special day highlighting (today, Sunday)
 * - Extensible view type system for subclass customization
 *
 * @author Updated with English comments and JavaDoc
 * @version 2.0
 * @since 2025
 */
public abstract class BaseAdapterLegacy extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // TAG
    protected static final String TAG = "BaseAdapterLegacy";

    // CONSTANTS
    protected static final int VIEW_TYPE_MONTH_HEADER = 0;
    protected static final int VIEW_TYPE_DAY = 1;
    protected static final int VIEW_TYPE_LOADING = 2;
    protected static final int VIEW_TYPE_EMPTY = 3;

    // MEMBERS
    protected final Context mContext;
    protected List<SharedViewModels.ViewItem> mItems;
    protected HalfTeam mUserHalfTeam;
    protected final int mNumShifts;

    // CACHED THEME COLORS
    protected int mCachedNormalTextColor = 0;
    protected int mCachedSundayTextColor = 0;
    protected int mCachedTodayBackgroundColor = 0;
    protected int mCachedUserShiftBackgroundColor = 0;
    protected int mCachedUserShiftTextColor = 0;

    /**
     * Constructs a new BaseAdapterLegacy with the specified parameters.
     *
     * @param context      Application context for resource access
     * @param items        List of view items to display
     * @param userHalfTeam Current user's team for highlighting
     * @param numShifts    Number of shifts to display per day
     */
    public BaseAdapterLegacy(Context context, List<SharedViewModels.ViewItem> items,
                             HalfTeam userHalfTeam, int numShifts) {
        this.mContext = context;
        this.mItems = items;
        this.mUserHalfTeam = userHalfTeam;
        this.mNumShifts = numShifts;
        initializeColorCache();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Initializes the color cache by retrieving theme colors.
     * This optimization prevents repeated theme color lookups during binding.
     */
    protected void initializeColorCache() {
        if (mCachedNormalTextColor == 0) {
            mCachedNormalTextColor = getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOnSurface);
            mCachedSundayTextColor = getColorByThemeAttr(mContext, R.attr.colorOnSundayBackground);
            mCachedTodayBackgroundColor = getColorByThemeAttr(mContext, R.attr.colorTodayBackground);
            mCachedUserShiftBackgroundColor = getColorByThemeAttr(mContext, R.attr.colorUserShiftBackground);
            mCachedUserShiftTextColor = getColorByThemeAttr(mContext, R.attr.colorOnUserShift);
        }
    }

    // ==================== METHODS ====================

    /**
     * Updates the adapter's data set and refreshes the display.
     *
     * @param newItems New list of view items to display
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<SharedViewModels.ViewItem> newItems) {
        this.mItems = newItems;
        notifyDataSetChanged();
    }

    /**
     * Returns the total number of items in the adapter.
     *
     * @return Item count, or 0 if items list is null
     */
    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    /**
     * Determines the view type for the item at the specified position.
     *
     * @param position Position of the item in the adapter
     * @return View type constant corresponding to the item type
     */
    @Override
    public int getItemViewType(int position) {
        if (position >= mItems.size()) return VIEW_TYPE_DAY;

        SharedViewModels.ViewItem item = mItems.get(position);
        switch (item.getType()) {
            case HEADER:
                return VIEW_TYPE_MONTH_HEADER;
            case LOADING:
                return VIEW_TYPE_LOADING;
            case EMPTY:
                return VIEW_TYPE_EMPTY;
            case DAY:
            default:
                return VIEW_TYPE_DAY;
        }
    }

    /**
     * Creates ViewHolder instances based on the specified view type.
     *
     * @param parent   Parent ViewGroup for the new view
     * @param viewType Type of view to create
     * @return Appropriate ViewHolder for the view type
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_MONTH_HEADER:
                return createMonthHeaderViewHolder(inflater, parent);

            case VIEW_TYPE_LOADING:
                return createLoadingViewHolder(inflater, parent);

            case VIEW_TYPE_EMPTY:
                return createEmptyViewHolder(inflater, parent);

            case VIEW_TYPE_DAY:
            default:
                return createDayViewHolder(inflater, parent);

        }
    }

    /**
     * Binds data to ViewHolder instances based on their type.
     *
     * @param holder   ViewHolder to bind data to
     * @param position Position of the item in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= mItems.size()) return;

        if (DEBUG_BASEADAPTER) {
            Log.d(TAG, "onBindViewHolder: ViewHolder = " + holder.getClass().getSimpleName());
        }

        SharedViewModels.ViewItem item = mItems.get(position);

        if (holder instanceof MonthHeaderViewHolder) {
            SharedViewModels.MonthHeader header = (SharedViewModels.MonthHeader) item;
            bindMonthHeader((MonthHeaderViewHolder) holder, header);

        } else if (holder instanceof LoadingViewHolder) {
            SharedViewModels.LoadingItem loading = (SharedViewModels.LoadingItem) item;
            bindLoading((LoadingViewHolder) holder, loading);

        } else if (holder instanceof EmptyViewHolder) {
            bindEmpty((EmptyViewHolder) holder);

        } else if (holder instanceof DayViewHolder) {
            SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
            bindDay((DayViewHolder) holder, dayItem, position);

        }

        if (DEBUG_BASEADAPTER) {
            Log.d(TAG, "onBindViewHolder: mItems.size() = " + mItems.size());
            Log.d(TAG, "onBindViewHolder: position = " + position);
            Log.d(TAG, "onBindViewHolder: item type = " + item.getClass().getSimpleName());
        }
    }

    // ==================== FACTORY METHODS (CAN BE OVERRIDDEN) ====================

    /**
     * Creates a ViewHolder for month header items.
     * Subclasses can override to provide custom month header layouts.
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return MonthHeaderViewHolder instance
     */
    protected RecyclerView.ViewHolder createMonthHeaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_month_header, parent, false);
        return new EnhancedMonthHeaderViewHolder(view);
    }

    /**
     * Creates a ViewHolder for loading indicator items.
     * Subclasses can override to provide custom loading layouts.
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return LoadingViewHolder instance
     */
    protected RecyclerView.ViewHolder createLoadingViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_loading_calendar, parent, false);
        return new LoadingViewHolder(view);
    }

    /**
     * Creates a ViewHolder for empty placeholder items.
     * Uses a simple invisible view for empty calendar cells.
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return EmptyViewHolder instance
     */
    protected RecyclerView.ViewHolder createEmptyViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // For empty cells, use a simple or invisible layout
        View view = new View(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new EmptyViewHolder(view);
    }

    /**
     * Creates a ViewHolder for day items.
     * Subclasses can override to provide custom day layouts.
     *
     * @param inflater Layout inflater
     * @param parent   Parent ViewGroup
     * @return DayViewHolder instance
     */
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_dayslist_row, parent, false);
        return new DayViewHolder((MaterialCardView) view);
    }

    // ===========================================================

    /**
     * Binds data to a month header ViewHolder.
     * Enhanced binding for month header with informative and decorative elements.
     *
     * @param holder ViewHolder to bind data to
     * @param header Month header data to bind
     */
    protected void bindMonthHeader(MonthHeaderViewHolder holder, SharedViewModels.MonthHeader header) {
        if (!(holder instanceof EnhancedMonthHeaderViewHolder enhancedHolder)) {
            // Fallback to basic binding
            holder.tvMonthTitle.setText(header.title);
            return;
        }

        try {
            LocalDate monthDate = header.monthDate;
            LocalDate today = LocalDate.now();

            // Format month name (only month for current year, month + year for others)
            String monthName;
            if (monthDate.getYear() == today.getYear()) {
                monthName = monthDate.format(DateTimeFormatter.ofPattern("MMMM", QDue.getLocale()));
            } else {
                monthName = monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", QDue.getLocale()));
            }

            enhancedHolder.tvMonthTitle.setText(monthName);

            // Show year only if different from current year
            if (monthDate.getYear() != today.getYear()) {
                enhancedHolder.tvYear.setText(String.valueOf(monthDate.getYear()));
                enhancedHolder.tvYear.setVisibility(View.VISIBLE);
            } else {
                enhancedHolder.tvYear.setVisibility(View.GONE);
            }

            // Show days count for the month
            int daysInMonth = monthDate.lengthOfMonth();
            enhancedHolder.tvDaysCount.setText(String.valueOf(daysInMonth));

            // Set appropriate icon based on month or season
            int iconResource = getSeasonalIcon(monthDate.getMonthValue());
            enhancedHolder.ivMonthIcon.setImageResource(iconResource);

            // Add subtle animation for current month
            if (monthDate.getYear() == today.getYear() &&
                    monthDate.getMonthValue() == today.getMonthValue()) {
                enhancedHolder.startAnimation();
            } else {
                enhancedHolder.stopAnimation();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding enhanced month header: " + e.getMessage());
            // Fallback to basic title
            enhancedHolder.tvMonthTitle.setText(header.title);
            enhancedHolder.tvYear.setVisibility(View.GONE);
            enhancedHolder.tvDaysCount.setText("");
            enhancedHolder.stopAnimation();
        }
    }

// ===========================================================

    /**
     * Binds data to a loading indicator ViewHolder.
     *
     * @param holder  ViewHolder to bind data to
     * @param loading Loading item data to bind
     */
    protected void bindLoading(LoadingViewHolder holder, SharedViewModels.LoadingItem loading) {
        holder.loadingText.setText(loading.message);
        holder.progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Binds data to an empty placeholder ViewHolder.
     * Empty cells have no content and are made invisible.
     *
     * @param holder ViewHolder to bind data to
     */
    protected void bindEmpty(EmptyViewHolder holder) {
        // Empty cells have no content
        holder.itemView.setVisibility(View.INVISIBLE);
    }

    /**
     * Binds data to a day ViewHolder with complete shift information.
     * This is the main binding method that handles:
     * - Day number and weekday name
     * - Shift team assignments
     * - Rest team information
     * - User team highlighting
     * - Special day colors (today, Sunday)
     *
     * @param holder   ViewHolder to bind data to
     * @param dayItem  Day item data to bind
     * @param position Position in the adapter
     */
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        Day day = dayItem.day;
        if (day == null) return;

        android.content.res.Resources r = mContext.getResources();
        boolean isSunday = dayItem.isSunday();
        boolean isToday = dayItem.isToday();

        // Set day number
        holder.tday.setText(r.getString(R.string.str_scheme_num, day.getDayOfMonth()));

        // Set weekday name
        holder.twday.setText(r.getString(R.string.str_scheme, day.getDayOfWeekAsString()));

        // Set shift texts
        bindShiftsToDay(holder, day); // No background implied

        // Set rest teams text
        String restTeams = day.getOffWorkHalfTeamsAsString();
        holder.ttR.setText(restTeams != null && !restTeams.isEmpty() ?
                r.getString(R.string.str_scheme, restTeams) : "");

//        // Find and highlight user shift
//        highlightUserShift(holder, day);
//
//        // Apply special day colors (today and Sunday)
//        applySpecialDayColors(holder, isToday, isSunday);

        if (DEBUG_BASEADAPTER) {
            Log.d("DEBUG", "Day date: " + dayItem.day.getDate());
            Log.d("DEBUG", "Day number: " + dayItem.day.getDayOfMonth());
            Log.d("DEBUG", "ItemView type: " + holder.itemView.getClass().getSimpleName());
            Log.d("DEBUG", "ItemView child count: " + ((ViewGroup) holder.itemView).getChildCount());
        }
    }

// ==================== HELPER METHODS FOR BINDING ====================

    /**
     * Binds shift team information to the day ViewHolder.
     * Populates shift TextViews with team assignments.
     *
     * @param holder ViewHolder containing shift TextViews
     * @param day    Day containing shift information
     */
    protected void bindShiftsToDay(DayViewHolder holder, Day day) {
        List<Shift> shifts = day.getShifts();
        int numShifts = Math.min(shifts.size(), mNumShifts);

        for (int i = 0; i < numShifts; i++) {
            if (holder.shiftTexts[i] != null) {
                try {
                    String teamText = shifts.get(i).getTeamsAsString();
                    holder.shiftTexts[i].setText(teamText != null && !teamText.isEmpty() ?
                            mContext.getString(R.string.str_scheme, teamText) : "");
                } catch (Exception e) {
                    holder.shiftTexts[i].setText("");
                }
            }
        }
    }

// ===========================================================

    /**
     * Get seasonal or month-appropriate icon with fallback.
     * Falls back to standard calendar icon if seasonal icons don't exist.
     */
    private int getSeasonalIcon(int month) {
        try {
            switch (month) {
                case 12:
                case 1:
                case 2: // Winter
                    return getDrawableResourceSafely("ic_calendar_winter", R.drawable.ic_calendar);
                case 3:
                case 4:
                case 5: // Spring
                    return getDrawableResourceSafely("ic_calendar_spring", R.drawable.ic_calendar);
                case 6:
                case 7:
                case 8: // Summer
                    return getDrawableResourceSafely("ic_calendar_summer", R.drawable.ic_calendar);
                case 9:
                case 10:
                case 11: // Autumn
                    return getDrawableResourceSafely("ic_calendar_autumn", R.drawable.ic_calendar);
                default:
                    return R.drawable.ic_calendar; // Default calendar icon
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting seasonal icon, using default: " + e.getMessage());
            return R.drawable.ic_calendar;
        }
    }

    /**
     * Safely get drawable resource with fallback.
     * Returns fallback resource if the requested resource doesn't exist.
     */
    private int getDrawableResourceSafely(String resourceName, int fallbackResource) {
        try {
            // Try to get the resource ID
            int resourceId = mContext.getResources().getIdentifier(
                    resourceName, "drawable", mContext.getPackageName());

            // If resource exists, return it; otherwise return fallback
            return resourceId != 0 ? resourceId : fallbackResource;

        } catch (Exception e) {
            Log.w(TAG, "Resource " + resourceName + " not found, using fallback");
            return fallbackResource;
        }
    }

    /**
     * Enhanced ViewHolder for month headers with self-managed animations.
     * Encapsulates animation logic within the ViewHolder for better architecture.
     */
    public static class EnhancedMonthHeaderViewHolder extends MonthHeaderViewHolder {
        public final TextView tvYear;
        public final TextView tvDaysCount;
        public final ImageView ivMonthIcon;

        // Store animator reference directly in ViewHolder
        private android.animation.ObjectAnimator currentAnimator;
        private boolean isAnimating = false;

        public EnhancedMonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvDaysCount = itemView.findViewById(R.id.tv_days_count);
            ivMonthIcon = itemView.findViewById(R.id.iv_month_icon);
        }

        /**
         * Start subtle pulsing animation for current month.
         * Creates a gentle alpha animation that pulses the month icon.
         */
        public void startAnimation() {
            if (isAnimating) return; // Already animating

            stopAnimation(); // Ensure clean state

            try {
                currentAnimator = android.animation.ObjectAnimator.ofFloat(
                        ivMonthIcon, "alpha", 1.0f, 0.7f);
                currentAnimator.setDuration(1500); // Slower, more subtle
                currentAnimator.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
                currentAnimator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
                currentAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

                // Add listener to track animation state
                currentAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(android.animation.Animator animation) {
                        isAnimating = true;
                    }

                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        isAnimating = false;
                    }

                    @Override
                    public void onAnimationCancel(android.animation.Animator animation) {
                        isAnimating = false;
                    }
                });

                currentAnimator.start();

            } catch (Exception e) {
                // Log error but don't crash
                Log.e("EnhancedMonthHeader", "Error starting animation: " + e.getMessage());
                isAnimating = false;
            }
        }

        /**
         * Stop animation and cleanup resources.
         * Ensures proper cleanup to prevent memory leaks.
         */
        public void stopAnimation() {
            try {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                    currentAnimator = null;
                }

                // Clear any old-style animations as well
                ivMonthIcon.clearAnimation();

                // Reset to normal state
                ivMonthIcon.setAlpha(1.0f);
                isAnimating = false;

            } catch (Exception e) {
                // Log error but don't crash
                Log.e("EnhancedMonthHeader", "Error stopping animation: " + e.getMessage());
                isAnimating = false;
            }
        }

        /**
         * Check if currently animating.
         * Useful for preventing multiple animations.
         */
        public boolean isAnimating() {
            return isAnimating;
        }

        /**
         * Cleanup when ViewHolder is recycled.
         * Called automatically by RecyclerView.
         */
        @Override
        protected void finalize() throws Throwable {
            stopAnimation(); // Ensure cleanup
            super.finalize();
        }
    }

// ===========================================================

    /**
     * Highlights the user's shift with special background and text colors.
     * Finds which shift the user's team is assigned to and applies highlighting.
     *
     * @param holder ViewHolder containing shift TextViews
     * @param day    Day to search for user's team
     */
    protected void highlightUserShift(DayViewHolder holder, Day day) {
        int userPosition = -1;
        if (mUserHalfTeam != null) {
            userPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
        }

        if (userPosition >= 0 && userPosition < mNumShifts && holder.shiftTexts[userPosition] != null) {
            holder.shiftTexts[userPosition].setBackgroundColor(mCachedUserShiftBackgroundColor);
            holder.shiftTexts[userPosition].setTextColor(mCachedUserShiftTextColor);
        }
    }

    /**
     * Applies special color schemes for today and Sunday.
     *
     * @param holder   ViewHolder to apply colors to
     * @param isToday  Whether this day is today
     * @param isSunday Whether this day is Sunday
     */
    protected void applySpecialDayColors(DayViewHolder holder, boolean isToday, boolean isSunday) {
        if (isToday) {
//            holder.mView.setBackgroundColor(mCachedTodayBackgroundColor);
            setAllDayTextColors(holder, mCachedNormalTextColor);
        } else if (isSunday) {
            setAllDayTextColors(holder, mCachedSundayTextColor);
        } else {
            setAllDayTextColors(holder, mCachedNormalTextColor);
        }
    }

    /**
     * Sets text color for all day-related TextViews.
     * Preserves highlighting for user shifts.
     *
     * @param holder ViewHolder containing TextViews
     * @param color  Color to apply
     */
    protected void setAllDayTextColors(DayViewHolder holder, int color) {
        holder.tday.setTextColor(color);
        holder.twday.setTextColor(color);
        holder.ttR.setTextColor(color);

        for (TextView tv : holder.shiftTexts) {
            if (tv != null) {
                // Don't change color of highlighted shifts
                if (tv.getBackground() == null ||
                        ((android.graphics.drawable.ColorDrawable) tv.getBackground()).getColor() == Color.TRANSPARENT) {
                    tv.setTextColor(color);
                }
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Finds the adapter position for a specific date.
     * Used for scrolling to specific dates or highlighting.
     *
     * @param targetDate Date to find in the adapter
     * @return Position of the date, or -1 if not found
     */
    public int findPositionForDate(java.time.LocalDate targetDate) {
        if (mItems == null || targetDate == null) return -1;

        for (int i = 0; i < mItems.size(); i++) {
            SharedViewModels.ViewItem item = mItems.get(i);
            if (item instanceof SharedViewModels.DayItem) {
                SharedViewModels.DayItem dayItem = (SharedViewModels.DayItem) item;
                if (dayItem.day.getDate().equals(targetDate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Helper method to setup event indicators in any ViewHolder.
     * Can be called from both adapters with their respective ViewHolders.
     *
     * @param context       Context for EventIndicatorHelper
     * @param typeIndicator The type indicator View
     * @param priorityBadge The priority badge View
     * @param events        List of events for this day
     */
    public static void setupEventIndicatorsForDay(Context context,
                                                  View typeIndicator,
                                                  View priorityBadge,
                                                  List<LocalEvent> events) {
        EventIndicatorHelper helper = new EventIndicatorHelper(context);
        helper.setupEventIndicators(typeIndicator, priorityBadge, events);
    }

    /**
     * Helper method to get events for a specific date from events map.
     * Utility method that can be used by both adapters.
     *
     * @param eventsMap Map of date to events list
     * @param date      Date to get events for
     * @return List of events for the date, or empty list if none
     */
    public static List<LocalEvent> getEventsForDate(Map<LocalDate, List<LocalEvent>> eventsMap,
                                                    LocalDate date) {
        if (eventsMap == null || date == null) {
            return new ArrayList<>();
        }

        List<LocalEvent> events = eventsMap.get(date);
        return events != null ? events : new ArrayList<>();
    }

    // ==================== INNER VIEW HOLDER CLASSES ====================

    /**
     * ViewHolder for month header items.
     * Contains a single TextView for displaying month/year information.
     */
    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextView displaying the month title
         */
        public final TextView tvMonthTitle;

        /**
         * Constructs a new MonthHeaderViewHolder.
         *
         * @param itemView The view associated with this ViewHolder
         */
        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
        }
    }

    /**
     * ViewHolder for loading indicator items.
     * Contains a progress bar and text for displaying loading states.
     */
    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        /**
         * Progress bar for loading animation
         */
        public final ProgressBar progressBar;

        /**
         * TextView for loading message
         */
        public final TextView loadingText;

        /**
         * Constructs a new LoadingViewHolder.
         *
         * @param itemView The view associated with this ViewHolder
         */
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            loadingText = itemView.findViewById(R.id.tv_loading);
        }
    }

    /**
     * ViewHolder for empty placeholder items.
     * Used for calendar grid cells that don't contain actual data.
     */
    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        /**
         * Constructs a new EmptyViewHolder.
         *
         * @param itemView The view associated with this ViewHolder
         */
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder for day items displaying shift information.
     * Contains TextViews for day number, day name, shift teams, and rest teams.
     */
    public class DayViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextView for day number
         */
        public final TextView tday;

        /**
         * TextView for day of week name
         */
        public final TextView twday;

        /**
         * Array of TextViews for shift team information
         */
        public final TextView[] shiftTexts;

        /**
         * TextView for teams on rest
         */
        public final TextView ttR;

        /**
         * Root view of the item for background manipulation
         */
        public final MaterialCardView mView;

        /**
         * Constructs a new DayViewHolder and initializes shift TextViews.
         *
         * @param rootView The root view associated with this ViewHolder (MaterialCardView)
         */
        public DayViewHolder(@NonNull MaterialCardView rootView) {
            super(rootView);
            mView = rootView;
            tday = rootView.findViewById(R.id.tday);
            twday = rootView.findViewById(R.id.twday);
            ttR = rootView.findViewById(R.id.ttR);

            // Initialize shift text views dynamically based on number of shifts
            shiftTexts = new TextView[mNumShifts];
            for (int i = 0; i < mNumShifts && i < 5; i++) {
                @SuppressLint("DiscouragedApi")
                int resId = rootView.getResources().getIdentifier("tt" + (i + 1), "id",
                        rootView.getContext().getPackageName());
                if (resId != 0) {
                    shiftTexts[i] = rootView.findViewById(resId);
                }
            }
        }
    }
}