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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.SharedViewModels;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

/**
 * Enhanced DaysListAdapter with Material Design compliance and events support
 * Minimal changes to the proven original adapter
 */
public class SimpleEnhancedDaysListAdapter extends BaseAdapter {

    // TAG
    private static final String TAG = "SimpleEnhancedDaysListAdapter";

    // Simple events tracking (just count for now)
    private Map<LocalDate, Integer> mEventsCount = new HashMap<>();

    public SimpleEnhancedDaysListAdapter(Context context, List<SharedViewModels.ViewItem> items,
                                         HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);
    }

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use original layout - no changes needed
        View view = inflater.inflate(R.layout.item_simple_dayslist_row, parent, false);
        return new MaterialDayViewHolder(view);
    }

    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        // Call parent to do all the original work
        super.bindDay(holder, dayItem, position);

        // Add our material design enhancements
        if (holder instanceof MaterialDayViewHolder) {
            MaterialDayViewHolder materialHolder = (MaterialDayViewHolder) holder;
            applyMaterialBackground(materialHolder, dayItem);
            addEventsIndicator(materialHolder, dayItem);
        }
    }

    /**
     * Apply Material Design compliant backgrounds
     */
    private void applyMaterialBackground(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        View rootView = holder.itemView;

        if (dayItem.isToday()) {
            // Today: Material primary container
            rootView.setBackgroundColor(getColorByThemeAttr(mContext, R.attr.colorTodayUserBackground ));
//            rootView.setBackgroundColor(getColorByThemeAttr(mContext,
//                    com.google.android.material.R.attr.colorPrimaryContainer));
        } else if (dayItem.isSunday()) {
            // Sunday: Material surface variant
            rootView.setBackgroundColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorSurfaceVariant));

        } else if (hasUserShift(dayItem)) {
            // User shift day: Subtle tint
            rootView.setBackgroundColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorSurfaceContainerLow));
//            rootView.setBackgroundColor(getColorByThemeAttr(mContext,
//                    com.google.android.material.R.attr.colorSurfaceContainerLow));
        } else {
            // Regular day: Default surface
            rootView.setBackgroundColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorSurface));
        }

        // Add ripple effect for touch feedback
//        rootView.setForeground(getColorByThemeAttr(mContext,
//                androidx.appcompat.R.attr.selectableItemBackground));
    }

    /**
     * Add events indicator at bottom of row
     */
    private void addEventsIndicator(MaterialDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) {
            holder.eventsIndicator.setVisibility(View.GONE);
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
     * Check if user has shift on this day
     */
    private boolean hasUserShift(SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null || mUserHalfTeam == null) return false;
        return dayItem.day.getInWichTeamIsHalfTeam(mUserHalfTeam) >= 0;
    }

    /**
     * Update events count for days
     */
    public void updateEventsCount(Map<LocalDate, Integer> eventsCount) {
        mEventsCount = eventsCount != null ? eventsCount : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * Enhanced ViewHolder with events indicator
     */
    public class MaterialDayViewHolder extends DayViewHolder {
        public TextView eventsIndicator;

        public MaterialDayViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find or create events indicator
            eventsIndicator = itemView.findViewById(R.id.tv_events_indicator);
            if (eventsIndicator == null) {
                // Create events indicator dynamically if not in layout
                TextView indicator = new TextView(itemView.getContext());
                indicator.setId(R.id.tv_events_indicator);
                indicator.setTextSize(10f);
                indicator.setPadding(8, 2, 8, 2);
                indicator.setBackgroundColor(Color.TRANSPARENT);

                // Add to parent layout
                if (itemView instanceof ViewGroup) {
                    ((ViewGroup) itemView).addView(indicator);
                }
                this.eventsIndicator = indicator;
            } else {
                // Events indicator exists in layout
                this.eventsIndicator = eventsIndicator;
            }
        }
    }
}

// ==================== LAYOUT UPDATE NEEDED ====================

/**
 * UPDATE item_dayslist_row.xml to add events indicator:
 *
 * Add this BEFORE the closing </LinearLayout> tag:
 */

/* 
    <!-- Events indicator -->
    <TextView
        android:id="@+id/tv_events_indicator"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:layout_marginTop="2dp"
        android:layout_marginEnd="8dp"
        android:textAppearance="@style/TextAppearance.Material3.LabelSmall"
        android:textColor="?attr/colorPrimary"
        android:background="@drawable/events_indicator_background"
        android:padding="4dp"
        android:visibility="gone"
        tools:text="2 eventi"
        tools:visibility="visible" />
*/