package net.calvuz.qdue.ui.features.calendar.components;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.domain.events.models.EventPriority;
import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.infrastructure.db.entities.ShiftTypeEntity;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarEventListener;
import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * CalendarDayViewHolder - ViewHolder for individual day cell in calendar grid.
 *
 * <p>Displays a single day with:</p>
 * <ul>
 *   <li>Day number text</li>
 *   <li>Shift background color from ShiftType</li>
 *   <li>Event indicators with EventType icons</li>
 *   <li>Loading and error states</li>
 *   <li>Click and long-click interactions</li>
 * </ul>
 *
 * <p>Visual States:</p>
 * <ul>
 *   <li>Current month vs adjacent month styling</li>
 *   <li>Today highlighting</li>
 *   <li>Weekend styling</li>
 *   <li>Shift color background</li>
 *   <li>Event count indicators</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarDayViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = "CalendarDayViewHolder";

    // Visual configuration
    private static final float ADJACENT_MONTH_ALPHA = 0.4f;
    private static final float CURRENT_MONTH_ALPHA = 1.0f;
    private static final int TODAY_STROKE_WIDTH_DP = 2;

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private CalendarEventListener mEventListener;

    // ==================== UI COMPONENTS ====================

    // Container
    private final MaterialCardView mDayCard;

    // Day content
    private final TextView mDayNumberText;
    private final View mTodayIndicator;

    // Shift background
    private final View mShiftBackground;

    // Event indicators
    private final View mEventIndicatorsContainer;
    private final ImageView mHighPriorityEventIcon;
    private final TextView mEventCountText;

    // State indicators
    private final View mLoadingIndicator;
    private final View mErrorIndicator;

    // ==================== STATE VARIABLES ====================

    private CalendarDay mCurrentCalendarDay;
    private boolean mIsCurrentMonth = true;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates CalendarDayViewHolder with UI components.
     *
     * @param itemView Layout view for day cell
     * @param eventListener Listener for day interactions (can be null)
     */
    public CalendarDayViewHolder(@NonNull View itemView, @Nullable CalendarEventListener eventListener) {
        super(itemView);

        mContext = itemView.getContext();
        mEventListener = eventListener;

        // Initialize UI components
        mDayCard = itemView.findViewById(R.id.day_card);
        mDayNumberText = itemView.findViewById(R.id.day_number_text);
        mTodayIndicator = itemView.findViewById(R.id.today_indicator);
        mShiftBackground = itemView.findViewById(R.id.shift_background);
        mEventIndicatorsContainer = itemView.findViewById(R.id.event_indicators_container);
        mHighPriorityEventIcon = itemView.findViewById(R.id.high_priority_event_icon);
        mEventCountText = itemView.findViewById(R.id.event_count_text);
        mLoadingIndicator = itemView.findViewById(R.id.loading_indicator);
        mErrorIndicator = itemView.findViewById(R.id.error_indicator);

        // Setup click listeners
        setupClickListeners();

        Log.v(TAG, "CalendarDayViewHolder created");
    }

    // ==================== INITIALIZATION ====================

    /**
     * Setup click and long-click listeners.
     */
    private void setupClickListeners() {
        if (mDayCard != null) {
            mDayCard.setOnClickListener(v -> handleDayClick());
            mDayCard.setOnLongClickListener(v -> handleDayLongClick());
        }
    }

    // ==================== BINDING METHODS ====================

    /**
     * Bind calendar day data to this view holder.
     *
     * @param calendarDay Calendar day data
     * @param isCurrentMonth Whether day belongs to current month
     */
    public void bindCalendarDay(@NonNull CalendarDay calendarDay, boolean isCurrentMonth) {
        mCurrentCalendarDay = calendarDay;
        mIsCurrentMonth = isCurrentMonth;

        // Update all UI components
        updateDayNumber(calendarDay);
        updateTodayIndicator(calendarDay);
        updateShiftBackground(calendarDay);
        updateEventIndicators(calendarDay);
        updateStateIndicators(calendarDay);
        updateVisualStyle(calendarDay, isCurrentMonth);

        Log.v(TAG, "Day bound: " + calendarDay.getDate() +
                ", currentMonth=" + isCurrentMonth);
    }

    // ==================== UI UPDATE METHODS ====================

    /**
     * Update day number text.
     */
    private void updateDayNumber(@NonNull CalendarDay calendarDay) {
        if (mDayNumberText != null) {
            int dayOfMonth = calendarDay.getDate().getDayOfMonth();
            mDayNumberText.setText(String.valueOf(dayOfMonth));

            // Set text color based on shift background
            int textColor = calculateTextColor(calendarDay);
            mDayNumberText.setTextColor(textColor);
        }
    }

    /**
     * Update today indicator visibility.
     */
    private void updateTodayIndicator(@NonNull CalendarDay calendarDay) {
        if (mTodayIndicator != null) {
            if (calendarDay.isToday() && mIsCurrentMonth) {
                mTodayIndicator.setVisibility(View.VISIBLE);

                // Set indicator color
                int todayColor = ContextCompat.getColor(mContext, R.color.today_indicator_color);
                mTodayIndicator.setBackgroundColor(todayColor);
            } else {
                mTodayIndicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Update shift background color.
     */
    private void updateShiftBackground(@NonNull CalendarDay calendarDay) {
        if (mShiftBackground == null) return;

        ShiftTypeEntity effectiveShift = calendarDay.getEffectiveShift();

        if (effectiveShift != null && mIsCurrentMonth) {
            // Parse hex color from shift type
            int shiftColor = parseShiftColor(effectiveShift.getColorHex());
            mShiftBackground.setBackgroundColor(shiftColor);
            mShiftBackground.setVisibility(View.VISIBLE);

            // Add subtle modification indicator if shift was modified
            if (calendarDay.hasShiftModification()) {
                mShiftBackground.setAlpha(0.8f);
            } else {
                mShiftBackground.setAlpha(1.0f);
            }
        } else {
            // No shift or adjacent month - use default background
            int defaultColor = ContextCompat.getColor(mContext, R.color.calendar_day_default_background);
            mShiftBackground.setBackgroundColor(defaultColor);
            mShiftBackground.setVisibility(View.VISIBLE);
            mShiftBackground.setAlpha(1.0f);
        }
    }

    /**
     * Update event indicators.
     */
    private void updateEventIndicators(@NonNull CalendarDay calendarDay) {
        if (mEventIndicatorsContainer == null) return;

        List<LocalEvent> events = calendarDay.getEvents();

        if (events.isEmpty()) {
            mEventIndicatorsContainer.setVisibility(View.GONE);
            return;
        }

        mEventIndicatorsContainer.setVisibility(View.VISIBLE);

        // Update high priority event icon
        updateHighPriorityEventIcon(calendarDay);

        // Update event count
        updateEventCount(calendarDay);
    }

    /**
     * Update high priority event icon.
     */
    private void updateHighPriorityEventIcon(@NonNull CalendarDay calendarDay) {
        if (mHighPriorityEventIcon == null) return;

        if (calendarDay.hasHighPriorityEvents()) {
            // Find highest priority event
            LocalEvent highestPriorityEvent = findHighestPriorityEvent(calendarDay.getEvents());

            if (highestPriorityEvent != null) {
                // Set icon from EventType
                int iconRes = highestPriorityEvent.getEventType().getIconRes();
                mHighPriorityEventIcon.setImageResource(iconRes);

                // Set icon color
                int iconColor = highestPriorityEvent.getEventType().getColor();
                mHighPriorityEventIcon.setColorFilter(iconColor);

                mHighPriorityEventIcon.setVisibility(View.VISIBLE);
            } else {
                mHighPriorityEventIcon.setVisibility(View.GONE);
            }
        } else {
            mHighPriorityEventIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Update event count text.
     */
    private void updateEventCount(@NonNull CalendarDay calendarDay) {
        if (mEventCountText == null) return;

        int eventCount = calendarDay.getEventCount();

        if (eventCount > 1) {
            mEventCountText.setText(String.valueOf(eventCount));
            mEventCountText.setVisibility(View.VISIBLE);
        } else {
            mEventCountText.setVisibility(View.GONE);
        }
    }

    /**
     * Update loading and error state indicators.
     */
    private void updateStateIndicators(@NonNull CalendarDay calendarDay) {
        // Loading indicator
        if (mLoadingIndicator != null) {
            if (calendarDay.isLoading()) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
            } else {
                mLoadingIndicator.setVisibility(View.GONE);
            }
        }

        // Error indicator
        if (mErrorIndicator != null) {
            if (calendarDay.hasError()) {
                mErrorIndicator.setVisibility(View.VISIBLE);
            } else {
                mErrorIndicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Update overall visual style based on month and state.
     */
    private void updateVisualStyle(@NonNull CalendarDay calendarDay, boolean isCurrentMonth) {
        if (mDayCard == null) return;

        // Set card alpha based on month
        float alpha = isCurrentMonth ? CURRENT_MONTH_ALPHA : ADJACENT_MONTH_ALPHA;
        mDayCard.setAlpha(alpha);

        // Set card elevation
        if (calendarDay.isToday() && isCurrentMonth) {
            mDayCard.setCardElevation(dpToPx(4));
        } else {
            mDayCard.setCardElevation(dpToPx(1));
        }

        // Update stroke for today
        if (calendarDay.isToday() && isCurrentMonth) {
            int todayColor = ContextCompat.getColor(mContext, R.color.today_stroke_color);
            mDayCard.setStrokeColor(todayColor);
            mDayCard.setStrokeWidth(dpToPx(TODAY_STROKE_WIDTH_DP));
        } else {
            mDayCard.setStrokeWidth(0);
        }

        // Weekend styling
        if (calendarDay.isWeekend() && isCurrentMonth) {
            // Could add special weekend styling here
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculate appropriate text color based on background.
     */
    private int calculateTextColor(@NonNull CalendarDay calendarDay) {
        ShiftTypeEntity effectiveShift = calendarDay.getEffectiveShift();

        if (effectiveShift != null && mIsCurrentMonth) {
            int backgroundColor = parseShiftColor(effectiveShift.getColorHex());
            return getContrastingTextColor(backgroundColor);
        } else {
            // Default text color
            return ContextCompat.getColor(mContext, R.color.calendar_day_text_color);
        }
    }

    /**
     * Parse hex color string to int.
     */
    private int parseShiftColor(@Nullable String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            return ContextCompat.getColor(mContext, R.color.calendar_day_default_background);
        }

        try {
            // Ensure hex string starts with #
            if (!colorHex.startsWith("#")) {
                colorHex = "#" + colorHex;
            }
            return Color.parseColor(colorHex);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid color hex: " + colorHex);
            return ContextCompat.getColor(mContext, R.color.calendar_day_default_background);
        }
    }

    /**
     * Get contrasting text color for background.
     */
    private int getContrastingTextColor(int backgroundColor) {
        // Calculate luminance
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        if (luminance > 0.5) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    /**
     * Find highest priority event in list.
     */
    @Nullable
    private LocalEvent findHighestPriorityEvent(@NonNull List<LocalEvent> events) {
        LocalEvent highest = null;
        EventPriority highestPriority = null;

        for (LocalEvent event : events) {
            EventPriority priority = event.getPriority();
            if (priority != null && (highestPriority == null || priority.ordinal() > highestPriority.ordinal())) {
                highest = event;
                highestPriority = priority;
            }
        }

        return highest;
    }

    /**
     * Convert DP to pixels.
     */
    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ==================== CLICK HANDLING ====================

    /**
     * Handle day click.
     */
    private void handleDayClick() {
        if (mEventListener != null && mCurrentCalendarDay != null) {
            LocalDate date = mCurrentCalendarDay.getDate();
            mEventListener.onDayClick(date, mCurrentCalendarDay);

            Log.d(TAG, "Day clicked: " + date);
        }
    }

    /**
     * Handle day long click.
     */
    private boolean handleDayLongClick() {
        if (mEventListener != null && mCurrentCalendarDay != null) {
            LocalDate date = mCurrentCalendarDay.getDate();
            mEventListener.onDayLongClick(date, mCurrentCalendarDay);

            Log.d(TAG, "Day long clicked: " + date);
            return true; // Consume the event
        }
        return false;
    }

    // ==================== GETTER METHODS ====================

    /**
     * Get currently bound calendar day.
     *
     * @return Current CalendarDay or null if not bound
     */
    @Nullable
    public CalendarDay getCurrentCalendarDay() {
        return mCurrentCalendarDay;
    }

    /**
     * Check if displaying current month date.
     *
     * @return true if current month date
     */
    public boolean isCurrentMonth() {
        return mIsCurrentMonth;
    }

    /**
     * Set event listener.
     *
     * @param eventListener New event listener
     */
    public void setEventListener(@Nullable CalendarEventListener eventListener) {
        mEventListener = eventListener;
    }
}