package net.calvuz.qdue.ui.features.schedulepattern.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PatternDayItemAdapter - RecyclerView Adapter for Pattern Day Management
 *
 * <p>Displays user's work schedule pattern as a vertical list of pattern days.
 * Each item represents a single day in the repeating sequence, showing either
 * a work shift or a rest day with appropriate visual styling.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Pattern Day Display</strong>: Shows day number, shift name, and timing</li>
 *   <li><strong>Rest Day Handling</strong>: Special styling for rest days (no shift)</li>
 *   <li><strong>Interactive Controls</strong>: Edit and remove buttons for each day</li>
 *   <li><strong>Visual Hierarchy</strong>: Clear day numbering and shift information</li>
 *   <li><strong>Material Design</strong>: Consistent with app design system</li>
 * </ul>
 *
 * <h3>Interaction Patterns:</h3>
 * <ul>
 *   <li><strong>Edit Pattern Day</strong>: Tap to modify shift assignment</li>
 *   <li><strong>Remove Pattern Day</strong>: Delete day from pattern sequence</li>
 *   <li><strong>Visual Feedback</strong>: Hover states and click animations</li>
 *   <li><strong>Accessibility</strong>: Full content description support</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class PatternDayItemAdapter extends RecyclerView.Adapter<PatternDayItemAdapter.PatternDayViewHolder> {

    private static final String TAG = "PatternDayItemAdapter";

    // ==================== DATA ====================

    private final List<PatternDay> mPatternDays;
    private final OnPatternDayInteractionListener mListener;
    private final LayoutInflater mInflater;
    private final Context mContext;

    // ==================== CONSTRUCTOR ====================

    /**
     * Create new PatternDayItemAdapter.
     *
     * @param patternDays List of pattern days to display
     * @param listener    Interaction listener for user actions
     */
    public PatternDayItemAdapter(@NonNull List<PatternDay> patternDays,
                                 @NonNull OnPatternDayInteractionListener listener) {
        this.mPatternDays = patternDays;
        this.mListener = listener;
        this.mInflater = null; // Will be set in onCreateViewHolder
        this.mContext = null; // Will be set in onCreateViewHolder
    }

    // ==================== ADAPTER METHODS ====================

    @NonNull
    @Override
    public PatternDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from( parent.getContext() )
                .inflate( R.layout.item_pattern_day, parent, false );
        return new PatternDayViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull PatternDayViewHolder holder, int position) {
        PatternDay patternDay = mPatternDays.get( position );
        holder.bind( patternDay, position );
    }

    @Override
    public int getItemCount() {
        return mPatternDays.size();
    }

    // ==================== VIEW HOLDER ====================

    /**
     * ViewHolder for individual pattern day items.
     */
    public class PatternDayViewHolder extends RecyclerView.ViewHolder {

        // ==================== UI COMPONENTS ====================

        private final View mItemContainer;
        private final TextView mDayNumberText;
        private final ImageView mShiftTypeIcon;
        private final TextView mShiftNameText;
        private final TextView mShiftTimingText;
        private final TextView mRestDayIndicator;
        private final ImageButton mEditButton;
        private final ImageButton mRemoveButton;
        private final View mDividerLine;

        // ==================== CONSTRUCTOR ====================

        public PatternDayViewHolder(@NonNull View itemView) {
            super( itemView );

            // Find views
            mItemContainer = itemView.findViewById( R.id.container_pattern_day_item );
            mDayNumberText = itemView.findViewById( R.id.tv_day_number );
            mShiftTypeIcon = itemView.findViewById( R.id.iv_shift_type_icon );
            mShiftNameText = itemView.findViewById( R.id.tv_shift_name );
            mShiftTimingText = itemView.findViewById( R.id.tv_shift_timing );
            mRestDayIndicator = itemView.findViewById( R.id.tv_rest_day_indicator );
            mEditButton = itemView.findViewById( R.id.btn_edit_pattern_day );
            mRemoveButton = itemView.findViewById( R.id.btn_remove_pattern_day );
            mDividerLine = itemView.findViewById( R.id.divider_line );

            // Setup click listeners
            setupClickListeners();
        }

        // ==================== BINDING ====================

        /**
         * Bind pattern day data to views.
         *
         * @param patternDay Pattern day to display
         * @param position   Position in adapter
         */
        public void bind(@NonNull PatternDay patternDay, int position) {
            Context context = itemView.getContext();

            // Set day number
            mDayNumberText.setText( String.valueOf( patternDay.getDayNumber() ) );

            if (patternDay.isRestDay()) {
                bindRestDay( patternDay, context );
            } else {
                bindWorkDay( patternDay, context );
            }

            // Handle editability
            boolean isEditable = patternDay.isEditable();
            mEditButton.setVisibility( isEditable ? View.VISIBLE : View.GONE );
            mRemoveButton.setVisibility( isEditable ? View.VISIBLE : View.GONE );
            mEditButton.setEnabled( isEditable );
            mRemoveButton.setEnabled( isEditable );

            // Handle divider (hide for last item)
            boolean isLastItem = position == getItemCount() - 1;
            mDividerLine.setVisibility( isLastItem ? View.GONE : View.VISIBLE );

            // Accessibility
            setupAccessibility( patternDay, context );
        }

        /**
         * Bind rest day specific data and styling.
         */
        private void bindRestDay(@NonNull PatternDay patternDay, @NonNull Context context) {
            // Hide work day elements
            mShiftTypeIcon.setVisibility( View.GONE );
            mShiftNameText.setVisibility( View.GONE );
            mShiftTimingText.setVisibility( View.GONE );

            // Show rest day indicator
            mRestDayIndicator.setVisibility( View.VISIBLE );
            mRestDayIndicator.setText( context.getString( R.string.pattern_day_rest ) );

            // Apply rest day styling
            mItemContainer.setBackgroundResource( R.drawable.bg_pattern_day_rest );
            mDayNumberText.setTextColor( ContextCompat.getColor( context, R.color.text_rest_day ) );
            mRestDayIndicator.setTextColor( ContextCompat.getColor( context, R.color.text_rest_day_secondary ) );
        }

        /**
         * Bind work day specific data and styling.
         */
        private void bindWorkDay(@NonNull PatternDay patternDay, @NonNull Context context) {
            Shift shift = patternDay.getShift();
            if (shift == null) {
                // This shouldn't happen for work days, but handle gracefully
                bindRestDay( patternDay, context );
                return;
            }

            // Hide rest day elements
            mRestDayIndicator.setVisibility( View.GONE );

            // Show work day elements
            mShiftTypeIcon.setVisibility( View.VISIBLE );
            mShiftNameText.setVisibility( View.VISIBLE );
            mShiftTimingText.setVisibility( View.VISIBLE );

            // Set shift information
            mShiftNameText.setText( shift.getName() );

            // Format shift timing
            String timingText = formatShiftTiming( shift );
            mShiftTimingText.setText( timingText );

            // Set shift type icon
            int iconResource = getShiftTypeIcon( shift );
            mShiftTypeIcon.setImageResource( iconResource );
            mShiftTypeIcon.setColorFilter( ContextCompat.getColor( context, getShiftTypeColor( shift ) ) );

            // Apply work day styling
            mItemContainer.setBackgroundResource( R.drawable.bg_pattern_day_work );
            mDayNumberText.setTextColor( ContextCompat.getColor( context, R.color.text_work_day ) );
            mShiftNameText.setTextColor( ContextCompat.getColor( context, R.color.text_work_day_primary ) );
            mShiftTimingText.setTextColor( ContextCompat.getColor( context, R.color.text_work_day_secondary ) );
        }

        // ==================== HELPER METHODS ====================

        /**
         * Format shift timing display text.
         */
        @NonNull
        private String formatShiftTiming(@NonNull Shift shift) {
            if (shift.getStartTime() != null && shift.getEndTime() != null) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern( "HH:mm" );
                String startTime = shift.getStartTime().format( timeFormatter );
                String endTime = shift.getEndTime().format( timeFormatter );
                return String.format( "%s - %s", startTime, endTime );
            } else if (shift.getStartTime() != null) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern( "HH:mm" );
                return itemView.getContext().getString( R.string.shift_start_time_format,
                        shift.getStartTime().format( timeFormatter ) );
            } else {
                return itemView.getContext().getString( R.string.shift_timing_not_specified );
            }
        }

        /**
         * Get appropriate icon for shift type.
         */
        private int getShiftTypeIcon(@NonNull Shift shift) {
            String shiftName = shift.getName().toLowerCase();

            if (shiftName.contains( "morning" ) || shiftName.contains( "mattina" )) {
                return R.drawable.ic_rounded_wb_sunny_24;
            } else if (shiftName.contains( "afternoon" ) || shiftName.contains( "pomeriggio" )) {
                return R.drawable.ic_rounded_wb_sunny_24;
            } else if (shiftName.contains( "night" ) || shiftName.contains( "notte" )) {
                return R.drawable.ic_rounded_nightlight_24;
            } else {
                return R.drawable.ic_rounded_work_24;
            }
        }

        /**
         * Get appropriate color for shift type.
         */
        private int getShiftTypeColor(@NonNull Shift shift) {
            String shiftName = shift.getName().toLowerCase();

            if (shiftName.contains( "morning" ) || shiftName.contains( "mattina" )) {
                return R.color.shift_morning_color;
            } else if (shiftName.contains( "afternoon" ) || shiftName.contains( "pomeriggio" )) {
                return R.color.shift_afternoon_color;
            } else if (shiftName.contains( "night" ) || shiftName.contains( "notte" )) {
                return R.color.shift_night_color;
            } else {
                return R.color.shift_default_color;
            }
        }

        /**
         * Setup accessibility content descriptions.
         */
        private void setupAccessibility(@NonNull PatternDay patternDay, @NonNull Context context) {
            String contentDescription;

            if (patternDay.isRestDay()) {
                contentDescription = context.getString( R.string.content_desc_pattern_day_rest,
                        patternDay.getDayNumber() );
            } else {
                Shift shift = patternDay.getShift();
                String shiftName = shift != null ? shift.getName() :
                        context.getString( R.string.shift_type_unknown );
                contentDescription = context.getString( R.string.content_desc_pattern_day_work,
                        patternDay.getDayNumber(), shiftName );
            }

            mItemContainer.setContentDescription( contentDescription );

            // Button content descriptions
            mEditButton.setContentDescription(
                    context.getString( R.string.content_desc_edit_pattern_day ) );
            //context.getString(R.string.content_desc_edit_pattern_day, patternDay.getDayNumber()));
            mRemoveButton.setContentDescription(
                    context.getString( R.string.content_desc_remove_pattern_day ) );
            //context.getString(R.string.content_desc_remove_pattern_day, patternDay.getDayNumber()));
        }

        /**
         * Setup click listeners for interactive elements.
         */
        private void setupClickListeners() {
            mEditButton.setOnClickListener( v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onEditPatternDay( position );
                }
            } );

            mRemoveButton.setOnClickListener( v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onRemovePatternDay( position );
                }
            } );

            // Make whole item clickable for edit
            mItemContainer.setOnClickListener( v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    PatternDay patternDay = mPatternDays.get( position );
                    if (patternDay.isEditable()) {
                        mListener.onEditPatternDay( position );
                    }
                }
            } );
        }
    }

    // ==================== INTERACTION LISTENER ====================

    /**
     * Interface for handling pattern day interactions.
     */
    public interface OnPatternDayInteractionListener {

        /**
         * Called when user wants to remove a pattern day.
         *
         * @param position Position of the day to remove
         */
        void onRemovePatternDay(int position);

        /**
         * Called when user wants to edit a pattern day.
         *
         * @param position Position of the day to edit
         */
        void onEditPatternDay(int position);
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Get pattern day at specific position.
     *
     * @param position Position in adapter
     * @return PatternDay at position or null if invalid
     */
    @Nullable
    public PatternDay getPatternDayAt(int position) {
        if (position >= 0 && position < mPatternDays.size()) {
            return mPatternDays.get( position );
        }
        return null;
    }

    /**
     * Check if adapter has any pattern days.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return mPatternDays.isEmpty();
    }

    /**
     * Get total number of work days in pattern.
     *
     * @return Count of work days
     */
    public int getWorkDaysCount() {
        int count = 0;
        for (PatternDay day : mPatternDays) {
            if (day.isWorkDay()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get total number of rest days in pattern.
     *
     * @return Count of rest days
     */
    public int getRestDaysCount() {
        int count = 0;
        for (PatternDay day : mPatternDays) {
            if (day.isRestDay()) {
                count++;
            }
        }
        return count;
    }
}