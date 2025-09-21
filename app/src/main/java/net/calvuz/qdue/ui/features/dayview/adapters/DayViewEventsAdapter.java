package net.calvuz.qdue.ui.features.dayview.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.ui.features.dayview.components.DayViewStateManager;
import net.calvuz.qdue.ui.features.dayview.components.DayViewEventOperations;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * DayViewEventsAdapter - Unified RecyclerView Adapter for Day View Events
 *
 * <p>RecyclerView adapter that handles display of multiple event types (LocalEvent, WorkScheduleDay)
 * with unified interface, multi-selection support, and comprehensive interaction handling.
 * Designed with extensible architecture for future event types.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-Type Support</strong>: LocalEvent and WorkScheduleDay with different layouts</li>
 *   <li><strong>Multi-Selection</strong>: Checkbox selection for bulk operations</li>
 *   <li><strong>Interactive Events</strong>: Click, long-click, and selection handling</li>
 *   <li><strong>State Management</strong>: Integration with DayViewStateManager</li>
 *   <li><strong>Accessibility</strong>: Full accessibility support with content descriptions</li>
 * </ul>
 *
 * <h3>View Types:</h3>
 * <ul>
 *   <li><strong>TYPE_LOCAL_EVENT</strong>: User-created events with full edit capabilities</li>
 *   <li><strong>TYPE_WORK_SCHEDULE</strong>: Work schedule items with read-only display</li>
 *   <li><strong>TYPE_SECTION_HEADER</strong>: Section separators for event grouping</li>
 *   <li><strong>Future Types</strong>: Extensible for additional event types</li>
 * </ul>
 *
 * <h3>Interaction Patterns:</h3>
 * <ul>
 *   <li><strong>Single Click</strong>: View/edit event details</li>
 *   <li><strong>Long Click</strong>: Enter selection mode</li>
 *   <li><strong>Selection Mode</strong>: Checkbox-based multi-select</li>
 *   <li><strong>Context Actions</strong>: Edit, delete, share, duplicate</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since DayView Implementation
 */
public class DayViewEventsAdapter extends RecyclerView.Adapter<DayViewEventsAdapter.BaseViewHolder> {

    private static final String TAG = "DayViewEventsAdapter";

    // ==================== VIEW TYPES ====================

    private static final int TYPE_SECTION_HEADER = 0;
    private static final int TYPE_LOCAL_EVENT = 1;
    private static final int TYPE_WORK_SCHEDULE = 2;
    // Future event types can be added here:
    // private static final int TYPE_CALENDAR_EVENT = 3;
    // private static final int TYPE_EXTERNAL_EVENT = 4;

    // ==================== ADAPTER ITEMS ====================

    /**
     * Base class for all adapter items.
     */
    public static abstract class AdapterItem {
        public abstract int getViewType();
        public abstract String getItemId();
    }

    /**
     * Section header item for grouping events.
     */
    public static class SectionHeaderItem extends AdapterItem {
        private final String mTitle;
        private final int mItemCount;

        public SectionHeaderItem(@NonNull String title, int itemCount) {
            mTitle = title;
            mItemCount = itemCount;
        }

        public String getTitle() { return mTitle; }
        public int getItemCount() { return mItemCount; }

        @Override
        public int getViewType() { return TYPE_SECTION_HEADER; }

        @Override
        public String getItemId() { return "header_" + mTitle; }
    }

    /**
     * Local event adapter item.
     */
    public static class LocalEventItem extends AdapterItem {
        private final LocalEvent mEvent;

        public LocalEventItem(@NonNull LocalEvent event) {
            mEvent = event;
        }

        public LocalEvent getEvent() { return mEvent; }

        @Override
        public int getViewType() { return TYPE_LOCAL_EVENT; }

        @Override
        public String getItemId() { return mEvent.getId(); }
    }

    /**
     * Work schedule adapter item.
     */
    public static class WorkScheduleItem extends AdapterItem {
        private final WorkScheduleDay mWorkScheduleDay;

        public WorkScheduleItem(@NonNull WorkScheduleDay workScheduleDay) {
            mWorkScheduleDay = workScheduleDay;
        }

        public WorkScheduleDay getWorkScheduleDay() { return mWorkScheduleDay; }

        @Override
        public int getViewType() { return TYPE_WORK_SCHEDULE; }

        @Override
        public String getItemId() { return mWorkScheduleDay.getId(); }
    }

    // ==================== ADAPTER STATE ====================

    private final LayoutInflater mInflater;
    private final DayViewStateManager mStateManager;
    private final DayViewEventOperations mEventOperations;

    private final List<AdapterItem> mItems = new ArrayList<>();

    // Event listeners
    private OnEventClickListener mOnEventClickListener;
    private OnEventLongClickListener mOnEventLongClickListener;

    // ==================== EVENT LISTENERS ====================

    /**
     * Listener for event click interactions.
     */
    public interface OnEventClickListener {
        void onLocalEventClick(@NonNull LocalEvent event);
        void onWorkScheduleClick(@NonNull WorkScheduleDay workScheduleDay);
    }

    /**
     * Listener for event long click interactions.
     */
    public interface OnEventLongClickListener {
        boolean onLocalEventLongClick(@NonNull LocalEvent event);
        boolean onWorkScheduleLongClick(@NonNull WorkScheduleDay workScheduleDay);
    }

    // ==================== CONSTRUCTOR ====================

    /**
     * Create DayViewEventsAdapter with required dependencies.
     *
     * @param context          Application context
     * @param stateManager     State manager for selection and UI state
     * @param eventOperations  Event operations for CRUD actions
     */
    public DayViewEventsAdapter(
            @NonNull Context context,
            @NonNull DayViewStateManager stateManager,
            @NonNull DayViewEventOperations eventOperations
    ) {
        this.mInflater = LayoutInflater.from(context);
        this.mStateManager = stateManager;
        this.mEventOperations = eventOperations;

        // Enable stable IDs for better performance
        setHasStableIds(true);

        Log.d(TAG, "DayViewEventsAdapter created");
    }

    // ==================== LISTENER SETTERS ====================

    public void setOnEventClickListener(@Nullable OnEventClickListener listener) {
        mOnEventClickListener = listener;
    }

    public void setOnEventLongClickListener(@Nullable OnEventLongClickListener listener) {
        mOnEventLongClickListener = listener;
    }

    // ==================== DATA MANAGEMENT ====================

    /**
     * Update adapter with new events from multiple sources.
     *
     * @param localEvents      List of local events
     * @param workScheduleDays List of work schedule days
     */
    public void updateEvents(@NonNull List<LocalEvent> localEvents,
                             @NonNull List<WorkScheduleDay> workScheduleDays) {
        Log.d(TAG, "Updating events: " + localEvents.size() + " local, " + workScheduleDays.size() + " work schedule");

        mItems.clear();

        // Add local events section
        if (!localEvents.isEmpty()) {
            mItems.add(new SectionHeaderItem("Personal Events", localEvents.size()));
            for (LocalEvent event : localEvents) {
                mItems.add(new LocalEventItem(event));
            }
        }

        // Add work schedule section
        if (!workScheduleDays.isEmpty()) {
            mItems.add(new SectionHeaderItem("Work Schedule", workScheduleDays.size()));
            for (WorkScheduleDay day : workScheduleDays) {
                mItems.add(new WorkScheduleItem(day));
            }
        }

        // Future event types can be added here:
        // if (!calendarEvents.isEmpty()) {
        //     mItems.add(new SectionHeaderItem("Calendar Events", calendarEvents.size()));
        //     for (CalendarEvent event : calendarEvents) {
        //         mItems.add(new CalendarEventItem(event));
        //     }
        // }

        notifyDataSetChanged();
        Log.d(TAG, "Events updated, total items: " + mItems.size());
    }

    /**
     * Clear all events from adapter.
     */
    public void clearEvents() {
        mItems.clear();
        notifyDataSetChanged();
        Log.d(TAG, "Events cleared");
    }

    // ==================== RECYCLERVIEW ADAPTER METHODS ====================

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    public int getViewType(int position) {
        return mItems.get(position).getViewType();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getItemId().hashCode();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_SECTION_HEADER:
                View headerView = mInflater.inflate(R.layout.item_day_view_section_header, parent, false);
                return new SectionHeaderViewHolder( headerView );

            case TYPE_LOCAL_EVENT:
                View localEventView = mInflater.inflate(R.layout.item_day_view_local_event, parent, false);
                return new LocalEventViewHolder(localEventView);

            case TYPE_WORK_SCHEDULE:
                View workScheduleView = mInflater.inflate(R.layout.item_day_view_work_schedule, parent, false);
                return new WorkScheduleViewHolder(workScheduleView);

            // Future view types can be added here:
            // case TYPE_CALENDAR_EVENT:
            //     View calendarEventView = mInflater.inflate(R.layout.item_day_view_calendar_event, parent, false);
            //     return new CalendarEventViewHolder(calendarEventView);

            default:
                Log.e(TAG, "Unknown view type: " + viewType);
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        AdapterItem item = mItems.get(position);
        holder.bind(item);
    }

    // ==================== BASE VIEW HOLDER ====================

    /**
     * Base ViewHolder class for all adapter items.
     */
    public abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(@NonNull AdapterItem item);
    }

    // ==================== SECTION HEADER VIEW HOLDER ====================

    public static class SectionHeaderViewHolder extends BaseViewHolder {
        private final TextView mTitleText;
        private final TextView mCountText;

        public SectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitleText = itemView.findViewById(R.id.section_title);
            mCountText = itemView.findViewById(R.id.section_count);
        }

        @Override
        public void bind(@NonNull AdapterItem item) {
            SectionHeaderItem headerItem = (SectionHeaderItem) item;

            mTitleText.setText(headerItem.getTitle());
            mCountText.setText(String.valueOf(headerItem.getItemCount()));

            // Accessibility
            itemView.setContentDescription(
                    headerItem.getTitle() + " section with " + headerItem.getItemCount() + " items"
            );
        }
    }

    // ==================== LOCAL EVENT VIEW HOLDER ====================

    public class LocalEventViewHolder extends BaseViewHolder {
        private final MaterialCardView mCardView;
        private final MaterialCheckBox mSelectionCheckbox;
        private final TextView mTitleText;
        private final TextView mDescriptionText;
        private final TextView mTimeText;
        private final ImageView mEventIcon;
        private final View mEditButton;
        private final View mDeleteButton;

        public LocalEventViewHolder(@NonNull View itemView) {
            super(itemView);
            mCardView = (MaterialCardView) itemView;
            mSelectionCheckbox = itemView.findViewById(R.id.selection_checkbox);
            mTitleText = itemView.findViewById(R.id.event_title);
            mDescriptionText = itemView.findViewById(R.id.event_description);
            mTimeText = itemView.findViewById(R.id.event_time);
            mEventIcon = itemView.findViewById(R.id.event_icon);
            mEditButton = itemView.findViewById(R.id.edit_button);
            mDeleteButton = itemView.findViewById(R.id.delete_button);
        }

        @Override
        public void bind(@NonNull AdapterItem item) {
            LocalEventItem eventItem = (LocalEventItem) item;
            LocalEvent event = eventItem.getEvent();

            // Set event data
            mTitleText.setText(event.getTitle());

            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                mDescriptionText.setText(event.getDescription());
                mDescriptionText.setVisibility(View.VISIBLE);
            } else {
                mDescriptionText.setVisibility(View.GONE);
            }

            // Format time display
            String timeDisplay = formatEventTime(event);
            if (timeDisplay != null) {
                mTimeText.setText(timeDisplay);
                mTimeText.setVisibility(View.VISIBLE);
            } else {
                mTimeText.setVisibility(View.GONE);
            }

            // Selection state
            boolean isSelected = mStateManager.isEventSelected(event.getId());
            boolean isSelectionMode = mStateManager.isSelectionMode();

            mSelectionCheckbox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            mSelectionCheckbox.setChecked(isSelected);

            // Action buttons visibility
            mEditButton.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);

            // Card selection visual state
            mCardView.setChecked(isSelected);
            mCardView.setStrokeWidth(isSelected ? 4 : 0);

            // Click listeners
            setupClickListeners(event);

            // Accessibility
            updateAccessibility(event, isSelected, isSelectionMode);
        }

        private void setupClickListeners(@NonNull LocalEvent event) {
            // Main click listener
            mCardView.setOnClickListener(v -> {
                if (mStateManager.isSelectionMode()) {
                    // Toggle selection
                    boolean newState = mStateManager.toggleEventSelection(event.getId());
                    mSelectionCheckbox.setChecked(newState);
                    mCardView.setChecked(newState);
                } else {
                    // Normal click
                    if (mOnEventClickListener != null) {
                        mOnEventClickListener.onLocalEventClick(event);
                    }
                }
            });

            // Long click listener
            mCardView.setOnLongClickListener(v -> {
                if (!mStateManager.isSelectionMode()) {
                    // Enter selection mode
                    mStateManager.setSelectionMode(true);
                    mStateManager.toggleEventSelection(event.getId());

                    if (mOnEventLongClickListener != null) {
                        return mOnEventLongClickListener.onLocalEventLongClick(event);
                    }
                    return true;
                }
                return false;
            });

            // Checkbox click listener
            mSelectionCheckbox.setOnClickListener(v -> {
                boolean newState = mStateManager.toggleEventSelection(event.getId());
                mSelectionCheckbox.setChecked(newState);
                mCardView.setChecked(newState);
            });

            // Edit button
            mEditButton.setOnClickListener(v -> {
                if (mOnEventClickListener != null) {
                    mOnEventClickListener.onLocalEventClick(event);
                }
            });

            // Delete button
            mDeleteButton.setOnClickListener(v -> {
                mEventOperations.deleteLocalEvent(event.getId())
                        .thenRun(() -> {
                            // Event will be automatically removed through state updates
                            Log.d(TAG, "Event deleted: " + event.getId());
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Failed to delete event: " + event.getId(), throwable);
                            return null;
                        });
            });
        }

        private void updateAccessibility(@NonNull LocalEvent event, boolean isSelected, boolean isSelectionMode) {
            StringBuilder contentDesc = new StringBuilder();
            contentDesc.append("Event: ").append(event.getTitle());

            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                contentDesc.append(", Description: ").append(event.getDescription());
            }

            String timeDisplay = formatEventTime(event);
            if (timeDisplay != null) {
                contentDesc.append(", Time: ").append(timeDisplay);
            }

            if (isSelectionMode) {
                contentDesc.append(isSelected ? ", Selected" : ", Not selected");
            }

            mCardView.setContentDescription(contentDesc.toString());
        }

        @Nullable
        private String formatEventTime(@NonNull LocalEvent event) {
            if (!event.getStartTime().toString().isEmpty()) {
                if (!event.getEndTime().toString().isEmpty()) {
                    return event.getStartTime() + " - " + event.getEndTime();
                } else {
                    return event.getStartTime().toString();
                }
            }
            return null;
        }
    }

    // ==================== WORK SCHEDULE VIEW HOLDER ====================

    public class WorkScheduleViewHolder extends BaseViewHolder {
        private final MaterialCardView mCardView;
        private final MaterialCheckBox mSelectionCheckbox;
        private final TextView mShiftNameText;
        private final TextView mShiftTimeText;
        private final TextView mShiftDescriptionText;
        private final ImageView mWorkIcon;

        public WorkScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            mCardView = (MaterialCardView) itemView;
            mSelectionCheckbox = itemView.findViewById(R.id.selection_checkbox);
            mShiftNameText = itemView.findViewById(R.id.shift_name);
            mShiftTimeText = itemView.findViewById(R.id.shift_time);
            mShiftDescriptionText = itemView.findViewById(R.id.shift_description);
            mWorkIcon = itemView.findViewById(R.id.work_icon);
        }

        @Override
        public void bind(@NonNull AdapterItem item) {
            WorkScheduleItem scheduleItem = (WorkScheduleItem) item;
            WorkScheduleDay workDay = scheduleItem.getWorkScheduleDay();

            // Set work schedule data
            if (workDay.getShiftCount() >0) {
                mShiftNameText.setText(workDay.getShift(0).getDisplayName());

                // Format shift time based on actual WorkScheduleDay structure
                String shiftTime = formatWorkScheduleTime(workDay);
                if (shiftTime != null) {
                    mShiftTimeText.setText(shiftTime);
                    mShiftTimeText.setVisibility(View.VISIBLE);
                } else {
                    mShiftTimeText.setVisibility(View.GONE);
                }

                // Shift description
                String description = workDay.getShift(0).getDescription();
                if (!description.isEmpty()) {
                    mShiftDescriptionText.setText(description);
                    mShiftDescriptionText.setVisibility(View.VISIBLE);
                } else {
                    mShiftDescriptionText.setVisibility(View.GONE);
                }
            } else {
                mShiftNameText.setText("Work Schedule");
                mShiftTimeText.setVisibility(View.GONE);
                mShiftDescriptionText.setVisibility(View.GONE);
            }

            // Selection state
            boolean isSelected = mStateManager.isEventSelected(workDay.getId());
            boolean isSelectionMode = mStateManager.isSelectionMode();

            mSelectionCheckbox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            mSelectionCheckbox.setChecked(isSelected);

            // Card selection visual state
            mCardView.setChecked(isSelected);
            mCardView.setStrokeWidth(isSelected ? 4 : 0);

            // Click listeners
            setupClickListeners(workDay);

            // Accessibility
            updateAccessibility(workDay, isSelected, isSelectionMode);
        }

        private void setupClickListeners(@NonNull WorkScheduleDay workDay) {
            // Main click listener
            mCardView.setOnClickListener(v -> {
                if (mStateManager.isSelectionMode()) {
                    // Toggle selection
                    boolean newState = mStateManager.toggleEventSelection(workDay.getId());
                    mSelectionCheckbox.setChecked(newState);
                    mCardView.setChecked(newState);
                } else {
                    // Normal click
                    if (mOnEventClickListener != null) {
                        mOnEventClickListener.onWorkScheduleClick(workDay);
                    }
                }
            });

            // Long click listener
            mCardView.setOnLongClickListener(v -> {
                if (!mStateManager.isSelectionMode()) {
                    // Enter selection mode
                    mStateManager.setSelectionMode(true);
                    mStateManager.toggleEventSelection(workDay.getId());

                    if (mOnEventLongClickListener != null) {
                        return mOnEventLongClickListener.onWorkScheduleLongClick(workDay);
                    }
                    return true;
                }
                return false;
            });

            // Checkbox click listener
            mSelectionCheckbox.setOnClickListener(v -> {
                boolean newState = mStateManager.toggleEventSelection(workDay.getId());
                mSelectionCheckbox.setChecked(newState);
                mCardView.setChecked(newState);
            });
        }

        private void updateAccessibility(@NonNull WorkScheduleDay workDay, boolean isSelected, boolean isSelectionMode) {
            StringBuilder contentDesc = new StringBuilder();
            contentDesc.append("Work schedule");

            if (workDay.getShiftCount() >0) {
                contentDesc.append(": ").append(workDay.getShift(0).getDisplayName());

                String shiftTime = formatWorkScheduleTime(workDay);
                if (shiftTime != null) {
                    contentDesc.append(", Time: ").append(shiftTime);
                }
            }

            if (isSelectionMode) {
                contentDesc.append(isSelected ? ", Selected" : ", Not selected");
            }

            mCardView.setContentDescription(contentDesc.toString());
        }

        @Nullable
        private String formatWorkScheduleTime(@NonNull WorkScheduleDay workDay) {
            // Format time based on actual WorkScheduleDay structure
            // This would depend on how time information is stored in WorkScheduleDay
            if (workDay.getShiftCount() >0) {
                // Example formatting - adjust based on actual data structure
                return workDay.getShift(0).getStartTime() + " - " + workDay.getShift(0).getEndTime();
            }
            return null;
        }
    }

    // ==================== CLEANUP ====================

    /**
     * Clean up adapter resources.
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up DayViewEventsAdapter");
        mItems.clear();
        mOnEventClickListener = null;
        mOnEventLongClickListener = null;
        notifyDataSetChanged();
    }
}