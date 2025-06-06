package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.content.Context;
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
        View view = inflater.inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarDayViewHolder(view);
    }

    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        if (!(holder instanceof CalendarDayViewHolder)) {
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
    }

    /**
     * Setup empty calendar cell
     */
    private void setupEmptyCell(CalendarDayViewHolder holder) {
        holder.tvDayNumber.setText("");
//        holder.tvDayName.setText("");
        holder.vShiftIndicator.setVisibility(View.INVISIBLE);
        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        holder.itemView.setAlpha(0.3f); // Make empty cells less prominent
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
     * Setup cell background and today highlighting
     */
    private void setupCellBackground(CalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.isToday()) {
            // Today: Prominent background
            holder.itemView.setBackgroundColor(mCachedTodayBackgroundColor);
            // Add subtle border for today
            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView cardView =
                        (com.google.android.material.card.MaterialCardView) holder.itemView;
                cardView.setStrokeColor(getColorByThemeAttr(mContext , androidx.appcompat.R.attr.colorPrimary));
                cardView.setStrokeWidth(3);
            }
        } else {
            // Regular day: Transparent background
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView cardView =
                        (com.google.android.material.card.MaterialCardView) holder.itemView;
                cardView.setStrokeColor(getColorByThemeAttr(mContext, com.google.android.material.R.attr.colorOutline));
                cardView.setStrokeWidth(1);
            }
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
                holder.vShiftIndicator.setBackgroundColor(shiftColor);
            } else {
                // Fallback to user shift color
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
        public final TextView tvDayNumber;
//        public final TextView tvDayName;
        public final View vShiftIndicator;

        public CalendarDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
//            tvDayName = itemView.findViewById(R.id.tv_day_name);
            vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);

            // Ensure minimum touch target size for accessibility
            itemView.setMinimumHeight(
                    (int) (48 * itemView.getContext().getResources().getDisplayMetrics().density)
            );
        }
    }
}