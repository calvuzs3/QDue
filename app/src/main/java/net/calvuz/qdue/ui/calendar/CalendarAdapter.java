package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.QDue.Debug.DEBUG_BASEADAPTER;
import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.R;

import java.util.List;

/**
 * Enhanced Calendar Adapter with improved text visibility and contrast.
 *
 * Key improvements:
 * - Larger text sizes for better readability
 * - Enhanced color contrast
 * - Better today highlighting
 * - More prominent shift indicators
 */
public class CalendarAdapter extends BaseAdapter {

    private static final String TAG = "CalendarAdapter";

    public CalendarAdapter(Context context, List<SharedViewModels.ViewItem> items,
                           HalfTeam userHalfTeam) {
        super(context, items, userHalfTeam, 1); // Calendar doesn't show shift details
    }

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use calendar-specific layout
        View view = inflater.inflate(R.layout.item_calendar_day_original, parent, false);
        return new CalendarDayViewHolder(view);
    }

    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        if (!(holder instanceof CalendarDayViewHolder)) {
            Log.d("DEBUG", "Wrong ViewHolder, calling super");
            super.bindDay(holder, dayItem, position);
            return;
        }

        CalendarDayViewHolder calendarHolder = (CalendarDayViewHolder) holder;
        Day day = dayItem.day;

        if (day == null) {
            // Empty calendar cell
            setupEmptyCell(calendarHolder);
            return;
        }

        // Setup day number with enhanced visibility
        setupDayNumber(calendarHolder, day, dayItem);

        // Setup day name (hidden for calendar view)
//        calendarHolder.tvDayName.setVisibility(View.GONE);

        // Setup background and highlighting
        setupCellBackground(calendarHolder, dayItem);

        // Setup shift indicator
        setupShiftIndicator(calendarHolder, day);

        if (DEBUG_BASEADAPTER) {
            // PROTO debugging
            TextView dayNumberView = holder.itemView.findViewById(R.id.tv_day_number);
            Log.d("DEBUG", "TextView found: " + (dayNumberView != null));
            Log.d("DEBUG", "TextView text set to: " + dayNumberView.getText());
            Log.d("DEBUG", "TextView visibility: " + dayNumberView.getVisibility());
        }
    }


    /**
     * Setup day number with enhanced text visibility
     */
    private void setupDayNumber(CalendarDayViewHolder holder, Day day, SharedViewModels.DayItem dayItem) {
        holder.tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));

        // Ensure full opacity for day cells with content
        holder.itemView.setAlpha(1.0f);

        // Enhanced text appearance based on day type
        if (dayItem.isToday()) {
            // Today: Bold, larger text, high contrast
            holder.tvDayNumber.setTextSize(18f);
            holder.tvDayNumber.setTypeface(holder.tvDayNumber.getTypeface(), Typeface.BOLD);
            holder.tvDayNumber.setTextColor(getColorByThemeAttr(mContext, R.attr.colorOnTodayBackground));
        } else if (dayItem.isSunday()) {
            // Sunday: Bold, accent color
            holder.tvDayNumber.setTextSize(16f);
            holder.tvDayNumber.setTypeface(holder.tvDayNumber.getTypeface(), Typeface.BOLD);
            holder.tvDayNumber.setTextColor(mCachedSundayTextColor);
        } else {
            // Regular day: Normal size, good contrast
            holder.tvDayNumber.setTextSize(16f);
            holder.tvDayNumber.setTypeface(holder.tvDayNumber.getTypeface(), Typeface.NORMAL);
            holder.tvDayNumber.setTextColor(mCachedNormalTextColor);
        }
    }

    /**
     * Setup shift indicator with enhanced visibility
     */
    private void setupShiftIndicator(CalendarDayViewHolder holder, Day day) {
        // Find user's shift position
        int userShiftPosition = -1;
        if (mUserHalfTeam != null) {
            userShiftPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
        }

        if (userShiftPosition >= 0) {
            // User has a shift: Show prominent indicator
            holder.vShiftIndicator.setVisibility(View.VISIBLE);

            int shiftColor = getShiftColor(day, userShiftPosition);
            if (shiftColor != 0) {
                Log.d("DEBUG", "Color: userShifPosition - " + userShiftPosition);
                holder.vShiftIndicator.setBackgroundColor(shiftColor);
            } else {
                // Fallback to user shift color
                Log.d("DEBUG",  "Color fallback: mCachedUserShiftBackgroundColor - " + mCachedUserShiftBackgroundColor);
                holder.vShiftIndicator.setBackgroundColor(mCachedUserShiftBackgroundColor);
            }

            // Make shift indicator more prominent
            ViewGroup.LayoutParams params = holder.vShiftIndicator.getLayoutParams();
            params.height = (int) (6 * mContext.getResources().getDisplayMetrics().density); // 6dp
            holder.vShiftIndicator.setLayoutParams(params);

        } else {
            // No shift: Hide indicator
            holder.vShiftIndicator.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Get shift color with better error handling
     */
    private int getShiftColor(Day day, int shiftPosition) {
        try {
            if (shiftPosition >= 0 && shiftPosition < day.getShifts().size()) {
                return day.getShifts().get(shiftPosition).getShiftType().getColor();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting shift color: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Override loading ViewHolder creation for calendar-specific layout
     */
    @Override
    protected RecyclerView.ViewHolder createLoadingViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_loading_calendar, parent, false);

        // Ensure loading view has proper dimensions for calendar grid
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Set minimum height for visibility
        int minHeight = (int) (60 * mContext.getResources().getDisplayMetrics().density); // 60dp
        view.setMinimumHeight(minHeight);

        return new LoadingViewHolder(view);
    }

    /**
     * Enhanced loading binding with better visibility
     */
    @Override
    protected void bindLoading(LoadingViewHolder holder, SharedViewModels.LoadingItem loading) {
        super.bindLoading(holder, loading);

        // Make loading text more prominent
        if (holder.loadingText != null) {
            holder.loadingText.setText("Loading " + loading.loadingType.toString().toLowerCase() + "...");
            holder.loadingText.setTextColor(getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary));
            holder.loadingText.setTextSize(14f);
            holder.loadingText.setTypeface(holder.loadingText.getTypeface(), Typeface.BOLD);
        }

        // Subtle background for loading items
        holder.itemView.setBackgroundColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface)
        );
        holder.itemView.setAlpha(0.8f);

        Log.d(TAG, "Binding loading item: " + loading.loadingType);
    }

    /**
     * Calendar-specific ViewHolder with enhanced day cell components
     */
    public class CalendarDayViewHolder extends DayViewHolder {
        final String mTAG = "CalendarDayViewHolder";

        public final TextView tvDayNumber;
//        public final TextView tvDayName;
        public final View vShiftIndicator;

        /**
         * UPDATE CalendarDayViewHolder constructor to use initialization
         */
        public CalendarDayViewHolder(@NonNull View itemView) {
            super(itemView);

            Log.v(TAG, mTAG + "Constructor invoked");

            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);

            // PERFORMANCE: Initialize MaterialCardView for optimal state
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                initializeCardView((com.google.android.material.card.MaterialCardView) itemView);
            }

            // Ensure minimum touch target size for accessibility (API 29+ requirement)
            itemView.setMinimumHeight(
                    (int) (48 * itemView.getContext().getResources().getDisplayMetrics().density)
            );

            // Performance optimization: Set click listener only once
            itemView.setOnClickListener(v -> {
                // Handle day click if needed
                Log.v(TAG, mTAG + "Day clicked: " + getAdapterPosition());
            });
        }
    }


    /**
     * IMMEDIATE FIX for CalendarAdapter.java background error
     *
     * Replace the problematic setupCellBackground and setupEmptyCell methods
     * with MaterialCardView-compatible versions optimized for API 29+ and performance
     */

    private void setupCellBackground(CalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (!(holder.itemView instanceof com.google.android.material.card.MaterialCardView)) {
            // Fallback for non-MaterialCardView (shouldn't happen with current layout)
            setupLegacyBackground(holder.itemView, dayItem);
            return;
        }

        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) holder.itemView;

        // Performance optimization: Cache view state to avoid repeated lookups
        if (dayItem.isToday()) {
            setupTodayCardStyle(cardView, dayItem);
        } else if (dayItem.isSunday()) {
            setupSundayCardStyle(cardView, dayItem);
        } else {
            setupRegularCardStyle(cardView, dayItem);
        }

        // Apply user shift highlighting as overlay (if applicable)
//        applyUserShiftHighlight(cardView, dayItem); // TODO: optimize blending color, 'cause it's strange
    }

    /**
     * Setup today card with prominent styling
     */
    private void setupTodayCardStyle(com.google.android.material.card.MaterialCardView cardView,
                                     SharedViewModels.DayItem dayItem) {
        // Use setCardBackgroundColor instead of setBackgroundColor
        cardView.setCardBackgroundColor(mCachedTodayBackgroundColor);
        cardView.setStrokeColor(getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary));
        cardView.setStrokeWidth(3);
        cardView.setCardElevation(4f);
        cardView.setRadius(12f); // Slightly larger radius for today

        // Performance: Set ripple color for better touch feedback
        cardView.setRippleColor(ColorStateList.valueOf(getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary & 0x30FFFFFF))); // 20% alpha
    }

    /**
     * Setup Sunday card with weekend styling
     */
    private void setupSundayCardStyle(com.google.android.material.card.MaterialCardView cardView,
                                      SharedViewModels.DayItem dayItem) {
        cardView.setCardBackgroundColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurfaceVariant));
        cardView.setStrokeColor(mCachedSundayTextColor);
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(1f);
        cardView.setRadius(8f);
        cardView.setRippleColor(ColorStateList.valueOf(mCachedSundayTextColor & 0x30FFFFFF)); // 20% alpha
    }

    /**
     * Setup regular day card with subtle styling
     */
    private void setupRegularCardStyle(com.google.android.material.card.MaterialCardView cardView,
                                       SharedViewModels.DayItem dayItem) {
        cardView.setCardBackgroundColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorSurface));
        cardView.setStrokeColor(
                getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOutlineVariant));
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(0f);
        cardView.setRadius(8f);
        cardView.setRippleColor(
                ColorStateList.valueOf(androidx.appcompat.R.attr.colorPrimary & 0x20FFFFFF)); // 12% alpha
    }

    /**
     * Apply user shift highlighting as overlay effect
     * Performance optimized: Only applies if user has shift on this day
     */
    private void applyUserShiftHighlight(com.google.android.material.card.MaterialCardView cardView,
                                         SharedViewModels.DayItem dayItem) {
        Day day = dayItem.day;
        if (day == null || mUserHalfTeam == null) return;

        // Check if user has shift (performance: early exit if no shift)
        int userShiftPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
        if (userShiftPosition < 0) return;

        // Apply user shift overlay effect
        int currentStrokeWidth = (int) cardView.getStrokeWidth();
        cardView.setStrokeWidth(Math.max(2, currentStrokeWidth + 1)); // Increase stroke

        // Blend current background with user shift color (subtle overlay)
        int currentBgColor = cardView.getCardBackgroundColor().getDefaultColor();
        int blendedColor = blendColorsPerformant(currentBgColor, mCachedUserShiftBackgroundColor, 0.25f);
        cardView.setCardBackgroundColor(blendedColor);
    }

    /**
     * FIXED: Setup empty calendar cell for MaterialCardView
     */
    private void setupEmptyCell(CalendarDayViewHolder holder) {
        holder.tvDayNumber.setText("");
        holder.vShiftIndicator.setVisibility(View.INVISIBLE);

        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) holder.itemView;

            // Empty cell: Transparent and minimal
            cardView.setCardBackgroundColor(Color.TRANSPARENT);
            cardView.setStrokeWidth(0);
            cardView.setCardElevation(0f);
            cardView.setRadius(0f); // No rounded corners for empty cells
            cardView.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT)); // No ripple for empty cells
        }

        holder.itemView.setAlpha(0.2f); // Very subtle for empty cells
    }

    /**
     * Performance-optimized color blending (avoiding expensive float operations)
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

    /**
     * Legacy fallback for non-MaterialCardView items (backward compatibility)
     */
    private void setupLegacyBackground(View itemView, SharedViewModels.DayItem dayItem) {
        if (dayItem.isToday()) {
            itemView.setBackgroundColor(mCachedTodayBackgroundColor);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * ENHANCED: Initialize MaterialCardView in ViewHolder constructor
     * Call this in CalendarDayViewHolder constructor for better performance
     */
    public static void initializeCardView(com.google.android.material.card.MaterialCardView cardView) {
        if (cardView == null) return;

        // Set sensible defaults to avoid state conflicts
        cardView.setUseCompatPadding(false); // Better performance on API 29+
        cardView.setPreventCornerOverlap(true); // Better visual consistency
        cardView.setCardElevation(0f); // Start with no elevation
        cardView.setRadius(8f); // Default corner radius
        cardView.setCardBackgroundColor(Color.TRANSPARENT); // Start transparent

        // Performance: Disable unnecessary features for calendar cells
        cardView.setStateListAnimator(null); // Simpler touch feedback
    }

}