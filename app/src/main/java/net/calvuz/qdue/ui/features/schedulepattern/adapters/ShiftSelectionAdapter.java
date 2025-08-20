package net.calvuz.qdue.ui.features.schedulepattern.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.Shift;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ShiftSelectionAdapter - Horizontal RecyclerView Adapter for Shift Selection
 *
 * <p>Displays available work shifts in a horizontal scrollable list, allowing users
 * to select shifts to add to their work pattern. Includes special handling for
 * rest days (represented as a special "no shift" option).</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Horizontal Layout</strong>: Card-based horizontal scrolling</li>
 *   <li><strong>Shift Cards</strong>: Visual representation of each shift type</li>
 *   <li><strong>Rest Day Option</strong>: Special card for adding rest days</li>
 *   <li><strong>Visual Styling</strong>: Distinct colors and icons per shift type</li>
 *   <li><strong>Touch Feedback</strong>: Material ripple effects and animations</li>
 * </ul>
 *
 * <h3>Shift Types Handling:</h3>
 * <ul>
 *   <li><strong>Morning Shifts</strong>: Sun icon with morning colors</li>
 *   <li><strong>Afternoon Shifts</strong>: Afternoon sun icon and colors</li>
 *   <li><strong>Night Shifts</strong>: Moon icon with night colors</li>
 *   <li><strong>Rest Days</strong>: Special rest icon and styling</li>
 *   <li><strong>Custom Shifts</strong>: Generic work icon for other shifts</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class ShiftSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ShiftSelectionAdapter";

    // ==================== VIEW TYPES ====================

    private static final int VIEW_TYPE_SHIFT = 1;
    private static final int VIEW_TYPE_REST_DAY = 2;

    // ==================== DATA ====================

    private final List<Shift> mShifts;
    private final OnShiftSelectionListener mListener;
    private final boolean mIncludeRestDay;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new ShiftSelectionAdapter with rest day option.
     *
     * @param shifts List of available shifts
     * @param listener Selection listener for user actions
     */
    public ShiftSelectionAdapter(@NonNull List<Shift> shifts,
                                 @NonNull OnShiftSelectionListener listener) {
        this(shifts, listener, true);
    }

    /**
     * Create new ShiftSelectionAdapter with optional rest day.
     *
     * @param shifts List of available shifts
     * @param listener Selection listener for user actions
     * @param includeRestDay Whether to include rest day option
     */
    public ShiftSelectionAdapter(@NonNull List<Shift> shifts,
                                 @NonNull OnShiftSelectionListener listener,
                                 boolean includeRestDay) {
        this.mShifts = shifts;
        this.mListener = listener;
        this.mIncludeRestDay = includeRestDay;
    }

    // ==================== ADAPTER METHODS ====================

    @Override
    public int getItemViewType(int position) {
        if (mIncludeRestDay && position == getItemCount() - 1) {
            return VIEW_TYPE_REST_DAY;
        } else {
            return VIEW_TYPE_SHIFT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_REST_DAY) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pattern_shift_selection_rest, parent, false);
            return new RestDayViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pattern_shift_selection, parent, false);
            return new ShiftViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ShiftViewHolder) {
            Shift shift = mShifts.get(position);
            ((ShiftViewHolder) holder).bind(shift);
        } else if (holder instanceof RestDayViewHolder) {
            ((RestDayViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        int shiftCount = mShifts.size();
        return mIncludeRestDay ? shiftCount + 1 : shiftCount;
    }

    // ==================== SHIFT VIEW HOLDER ====================

    /**
     * ViewHolder for individual shift selection items.
     */
    public class ShiftViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView mCardContainer;
        private final ImageView mShiftIcon;
        private final TextView mShiftNameText;
        private final TextView mShiftTimingText;
        private final View mShiftColorIndicator;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);

            mCardContainer = itemView.findViewById(R.id.card_shift_selection);
            mShiftIcon = itemView.findViewById(R.id.iv_shift_icon);
            mShiftNameText = itemView.findViewById(R.id.tv_shift_name);
            mShiftTimingText = itemView.findViewById(R.id.tv_shift_timing);
            mShiftColorIndicator = itemView.findViewById(R.id.view_shift_color_indicator);

            // Setup click listener
            mCardContainer.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < mShifts.size()) {
                    Shift shift = mShifts.get(position);
                    mListener.onShiftSelected(shift);
                }
            });
        }

        public void bind(@NonNull Shift shift) {
            Context context = itemView.getContext();

            // Set shift name
            mShiftNameText.setText(shift.getName());

            // Set shift timing
            String timingText = formatShiftTiming(shift, context);
            mShiftTimingText.setText(timingText);

            // Set shift icon and colors
            ShiftTypeInfo typeInfo = getShiftTypeInfo(shift, context);
            mShiftIcon.setImageResource(typeInfo.iconResource);
            mShiftIcon.setColorFilter(ContextCompat.getColor(context, typeInfo.iconColor));
            mShiftColorIndicator.setBackgroundColor(ContextCompat.getColor(context, typeInfo.accentColor));

            // Apply card styling based on shift type
            mCardContainer.setStrokeColor(ContextCompat.getColor(context, typeInfo.strokeColor));
            mCardContainer.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.shift_card_stroke_width));

            // Accessibility
            String contentDescription = context.getString(R.string.content_desc_select_shift,
                    shift.getName(), timingText);
            mCardContainer.setContentDescription(contentDescription);
        }
    }

    // ==================== REST DAY VIEW HOLDER ====================

    /**
     * ViewHolder for rest day selection item.
     */
    public class RestDayViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView mCardContainer;
        private final ImageView mRestIcon;
        private final TextView mRestText;
        private final TextView mRestDescription;

        public RestDayViewHolder(@NonNull View itemView) {
            super(itemView);

            mCardContainer = itemView.findViewById(R.id.card_rest_day_selection);
            mRestIcon = itemView.findViewById(R.id.iv_rest_icon);
            mRestText = itemView.findViewById(R.id.tv_rest_text);
            mRestDescription = itemView.findViewById(R.id.tv_rest_description);

            // Setup click listener
            mCardContainer.setOnClickListener(v -> mListener.onRestDaySelected());
        }

        public void bind() {
            Context context = itemView.getContext();

            // Set rest day text
            mRestText.setText(context.getString(R.string.pattern_day_rest));
            mRestDescription.setText(context.getString(R.string.shift_selection_rest_day_description));

            // Set rest day icon and colors
            mRestIcon.setImageResource(R.drawable.ic_rounded_hotel_24);
            mRestIcon.setColorFilter(ContextCompat.getColor(context, R.color.rest_day_icon_color));

            // Apply rest day card styling
            mCardContainer.setStrokeColor(ContextCompat.getColor(context, R.color.rest_day_stroke_color));
            mCardContainer.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.rest_card_stroke_width));
            mCardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rest_day_background));

            // Accessibility
            String contentDescription = context.getString(R.string.content_desc_select_rest_day);
            mCardContainer.setContentDescription(contentDescription);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Format shift timing for display.
     */
    @NonNull
    private String formatShiftTiming(@NonNull Shift shift, @NonNull Context context) {
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String startTime = shift.getStartTime().format(timeFormatter);
            String endTime = shift.getEndTime().format(timeFormatter);
            return String.format("%s - %s", startTime, endTime);
        } else if (shift.getStartTime() != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            return context.getString(R.string.shift_start_time_format,
                    shift.getStartTime().format(timeFormatter));
        } else {
            return context.getString(R.string.shift_timing_flexible);
        }
    }

    /**
     * Get shift type information for styling.
     */
    @NonNull
    private ShiftTypeInfo getShiftTypeInfo(@NonNull Shift shift, @NonNull Context context) {
        String shiftName = shift.getName().toLowerCase();

        if (shiftName.contains("morning") || shiftName.contains("mattina")) {
            return new ShiftTypeInfo(
                    R.drawable.ic_rounded_wb_sunny_24,
                    R.color.shift_morning_icon_color,
                    R.color.shift_morning_accent_color,
                    R.color.shift_morning_stroke_color
            );
        } else if (shiftName.contains("afternoon") || shiftName.contains("pomeriggio")) {
            return new ShiftTypeInfo(
                    R.drawable.ic_rounded_wb_sunny_24,
                    R.color.shift_afternoon_icon_color,
                    R.color.shift_afternoon_accent_color,
                    R.color.shift_afternoon_stroke_color
            );
        } else if (shiftName.contains("night") || shiftName.contains("notte")) {
            return new ShiftTypeInfo(
                    R.drawable.ic_rounded_nightlight_24,
                    R.color.shift_night_icon_color,
                    R.color.shift_night_accent_color,
                    R.color.shift_night_stroke_color
            );
        } else {
            return new ShiftTypeInfo(
                    R.drawable.ic_rounded_work_24,
                    R.color.shift_default_icon_color,
                    R.color.shift_default_accent_color,
                    R.color.shift_default_stroke_color
            );
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Container for shift type styling information.
     */
    private static class ShiftTypeInfo {
        final int iconResource;
        final int iconColor;
        final int accentColor;
        final int strokeColor;

        ShiftTypeInfo(int iconResource, int iconColor, int accentColor, int strokeColor) {
            this.iconResource = iconResource;
            this.iconColor = iconColor;
            this.accentColor = accentColor;
            this.strokeColor = strokeColor;
        }
    }

    // ==================== LISTENER INTERFACE ====================

    /**
     * Interface for handling shift selection interactions.
     */
    public interface OnShiftSelectionListener {

        /**
         * Called when user selects a work shift.
         *
         * @param shift Selected shift
         */
        void onShiftSelected(@NonNull Shift shift);

        /**
         * Called when user selects a rest day.
         */
        void onRestDaySelected();
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Get shift at specific position.
     *
     * @param position Position in adapter
     * @return Shift at position or null if rest day or invalid position
     */
    @NonNull
    public Shift getShiftAt(int position) {
        if (position >= 0 && position < mShifts.size()) {
            return mShifts.get(position);
        }
        throw new IndexOutOfBoundsException("Invalid shift position: " + position);
    }

    /**
     * Check if position represents a rest day.
     *
     * @param position Position to check
     * @return true if rest day position, false otherwise
     */
    public boolean isRestDayPosition(int position) {
        return mIncludeRestDay && position == getItemCount() - 1;
    }

    /**
     * Get number of actual shifts (excluding rest day).
     *
     * @return Number of shifts
     */
    public int getShiftsCount() {
        return mShifts.size();
    }

    /**
     * Check if adapter is empty.
     *
     * @return true if no shifts available
     */
    public boolean isEmpty() {
        return mShifts.isEmpty();
    }

    /**
     * Check if rest day is included in selection.
     *
     * @return true if rest day option is available
     */
    public boolean hasRestDayOption() {
        return mIncludeRestDay;
    }
}