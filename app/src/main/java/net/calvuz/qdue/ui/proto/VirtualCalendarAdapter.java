package net.calvuz.qdue.ui.proto;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROTOTYPE: Virtual Calendar Adapter with Skeleton Loading
 * Adapter that provides immediate UI responsiveness with skeleton loading
 * and progressive data enhancement
 * This adapter implements:
 * 1. Immediate skeleton display for smooth scrolling
 * 2. Progressive data enhancement when available
 * 3. Smooth transitions from skeleton to real data
 * 4. Memory-efficient virtual scrolling
 */
public class VirtualCalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements VirtualCalendarDataManager.DataAvailabilityCallback {

    private static final String TAG = "VirtualCalendarAdapter";

    // View types for different states
    private static final int TYPE_SKELETON_DAY = 1;
    private static final int TYPE_LOADED_DAY = 2;
    private static final int TYPE_LOADING_PROGRESS = 3;
    private static final int TYPE_MONTH_HEADER = 4;
    private static final int TYPE_ERROR_DAY = 5;

    // Data management
    private final VirtualCalendarDataManager dataManager;
    private final Context context;
    private final Map<Integer, VirtualItem> virtualItems = new LinkedHashMap<>();
    private final Map<LocalDate, Integer> monthToProgressMap = new ConcurrentHashMap<>();

    // Scroll behavior tracking
    private VirtualCalendarDataManager.ScrollDirection lastScrollDirection =
            VirtualCalendarDataManager.ScrollDirection.STATIONARY;
    private int lastScrollVelocity = 0;
    private LocalDate currentCenterMonth;

    // Animation helpers
    private final ValueAnimator dataTransitionAnimator = ValueAnimator.ofFloat(0f, 1f);

    public VirtualCalendarAdapter(Context context, VirtualCalendarDataManager dataManager) {
        this.context = context;
        this.dataManager = dataManager;
        this.dataManager.setDataAvailabilityCallback(this);

        // Initialize with current month
        this.currentCenterMonth = LocalDate.now().withDayOfMonth(1);
        generateVirtualItems(currentCenterMonth);

        // Setup data transition animations
        setupDataTransitionAnimation();

        setHasStableIds(true); // Enable stable IDs for smooth animations
    }

    // ==================== 2. VIRTUAL ITEM MANAGEMENT ====================

    /**
     * Represents a virtual item that may or may not have data loaded
     */
    private static class VirtualItem {
        final LocalDate date;           // Date this item represents
        final LocalDate monthDate;     // Month this item belongs to
        final ItemType type;           // Type of item
        Day loadedData;                // Actual data when available
        VirtualCalendarDataManager.DataState dataState;

        enum ItemType {
            MONTH_HEADER, DAY_CELL
        }

        VirtualItem(LocalDate date, LocalDate monthDate, ItemType type) {
            this.date = date;
            this.monthDate = monthDate;
            this.type = type;
            this.dataState = VirtualCalendarDataManager.DataState.NOT_REQUESTED;
        }
    }

    /**
     * Generate virtual items for viewport around center month
     * This creates the "infinite scroll" illusion with minimal memory
     */
    private void generateVirtualItems(LocalDate centerMonth) {
        virtualItems.clear();
        int position = 0;

        // Generate for 3 months (previous, current, next)
        for (int monthOffset = -1; monthOffset <= 1; monthOffset++) {
            LocalDate month = centerMonth.plusMonths(monthOffset);

            // Add month header
            VirtualItem header = new VirtualItem(month, month, VirtualItem.ItemType.MONTH_HEADER);
            virtualItems.put(position++, header);

            // Add days for this month
            LocalDate firstDay = month.withDayOfMonth(1);
            int daysInMonth = month.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate dayDate = firstDay.withDayOfMonth(day);
                VirtualItem dayItem = new VirtualItem(dayDate, month, VirtualItem.ItemType.DAY_CELL);
                virtualItems.put(position++, dayItem);
            }
        }

        Log.d(TAG, "Generated " + virtualItems.size() + " virtual items around " + centerMonth);
    }

    // ==================== 3. RECYCLER VIEW ADAPTER METHODS ====================

    @Override
    public int getItemCount() {
        return virtualItems.size();
    }

    @Override
    public long getItemId(int position) {
        VirtualItem item = virtualItems.get(position);
        if (item == null) return RecyclerView.NO_ID;

        // Create stable ID from date
        return item.date.toEpochDay();
    }

    @Override
    public int getItemViewType(int position) {
        VirtualItem item = virtualItems.get(position);
        if (item == null) return TYPE_SKELETON_DAY;

        if (item.type == VirtualItem.ItemType.MONTH_HEADER) {
            return TYPE_MONTH_HEADER;
        }

        // For day cells, return type based on data availability
        switch (item.dataState) {
            case AVAILABLE:
                return TYPE_LOADED_DAY;
            case LOADING:
                return TYPE_LOADING_PROGRESS;
            case ERROR:
                return TYPE_ERROR_DAY;
            default:
                return TYPE_SKELETON_DAY;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (viewType) {
            case TYPE_MONTH_HEADER:
                return new MonthHeaderViewHolder(
                        inflater.inflate(R.layout.item_month_header, parent, false));

            case TYPE_LOADED_DAY:
                return new LoadedDayViewHolder(
                        inflater.inflate(R.layout.item_calendar_day, parent, false));

            case TYPE_LOADING_PROGRESS:
                return new LoadingProgressViewHolder(
                        inflater.inflate(R.layout.item_calendar_day_loading, parent, false));

            case TYPE_ERROR_DAY:
                return new ErrorDayViewHolder(
                        inflater.inflate(R.layout.item_calendar_day_error, parent, false));

            case TYPE_SKELETON_DAY:
            default:
                return new SkeletonDayViewHolder(
                        inflater.inflate(R.layout.item_calendar_day_skeleton, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VirtualItem item = virtualItems.get(position);
        if (item == null) return;

        switch (holder.getItemViewType()) {
            case TYPE_MONTH_HEADER:
                bindMonthHeader((MonthHeaderViewHolder) holder, item);
                break;

            case TYPE_LOADED_DAY:
                bindLoadedDay((LoadedDayViewHolder) holder, item);
                break;

            case TYPE_LOADING_PROGRESS:
                bindLoadingProgress((LoadingProgressViewHolder) holder, item);
                break;

            case TYPE_ERROR_DAY:
                bindErrorDay((ErrorDayViewHolder) holder, item);
                break;

            case TYPE_SKELETON_DAY:
                bindSkeletonDay((SkeletonDayViewHolder) holder, item);
                break;
        }
    }

    // ==================== 4. VIEW HOLDERS ====================

    /**
     * ViewHolder for month headers
     */
    private static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView monthTitle;

        MonthHeaderViewHolder(View itemView) {
            super(itemView);
            monthTitle = itemView.findViewById(R.id.tv_month_title);
        }
    }

    /**
     * ViewHolder for fully loaded day cells
     * DEBUG changing name from LoadedDayViewHolder to ProtoCalendarDayViewHolder
     */
    private static class LoadedDayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;
        final TextView shiftIndicator;
        final View backgroundCard;

        LoadedDayViewHolder(View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.tv_day_number);
//            shiftIndicator = itemView.findViewById(R.id.tv_shift_indicator);
            shiftIndicator = itemView.findViewById(R.id.shift_indicator);
//            backgroundCard = itemView.findViewById(R.id.card_day_background);
            backgroundCard = null;
        }
    }

    /**
     * ViewHolder for skeleton loading state
     */
    private static class SkeletonDayViewHolder extends RecyclerView.ViewHolder {
        final View skeletonShimmer;
        final TextView dayNumber;

        SkeletonDayViewHolder(View itemView) {
            super(itemView);
            skeletonShimmer = itemView.findViewById(R.id.skeleton_shimmer);
            dayNumber = itemView.findViewById(R.id.tv_day_number);

            // Start shimmer animation
            startShimmerAnimation();
        }

        private void startShimmerAnimation() {
            if (skeletonShimmer != null) {
                ObjectAnimator shimmer = ObjectAnimator.ofFloat(skeletonShimmer, "alpha", 0.3f, 1.0f);
                shimmer.setDuration(1200);
                shimmer.setRepeatMode(ValueAnimator.REVERSE);
                shimmer.setRepeatCount(ValueAnimator.INFINITE);
                shimmer.start();
            }
        }
    }

    /**
     * ViewHolder for loading progress state
     */
    private static class LoadingProgressViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;
        final ProgressBar loadingProgress;

        LoadingProgressViewHolder(View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.tv_day_number);
            loadingProgress = itemView.findViewById(R.id.progress_loading);
        }
    }

    /**
     * ViewHolder for error state
     */
    private static class ErrorDayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;
        final View errorIndicator;

        ErrorDayViewHolder(View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.tv_day_number);
            errorIndicator = itemView.findViewById(R.id.error_indicator);
        }
    }

    // ==================== 5. BINDING METHODS ====================

    private void bindMonthHeader(MonthHeaderViewHolder holder, VirtualItem item) {
        String monthTitle = item.date.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        holder.monthTitle.setText(monthTitle);
    }

    private void bindSkeletonDay(SkeletonDayViewHolder holder, VirtualItem item) {
        holder.dayNumber.setText(String.valueOf(item.date.getDayOfMonth()));

        // Request data for this month if not already requested
        dataManager.requestViewportData(item.monthDate, lastScrollDirection, lastScrollVelocity);
    }

    private void bindLoadedDay(LoadedDayViewHolder holder, VirtualItem item) {
        if (item.loadedData == null) return;

        holder.dayNumber.setText(String.valueOf(item.date.getDayOfMonth()));

        // Display shift information
        if (item.loadedData.getIsToday()) {   // hasShifts() - TODO
            holder.shiftIndicator.setText(item.loadedData.getOffWorkHalfTeamsAsString()); // getShiftSummary()); - TODO
            holder.shiftIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.shiftIndicator.setVisibility(View.GONE);
        }

        // Apply smooth transition animation if transitioning from skeleton
        animateDataAppearance(holder.itemView);
    }

    private void bindLoadingProgress(LoadingProgressViewHolder holder, VirtualItem item) {
        holder.dayNumber.setText(String.valueOf(item.date.getDayOfMonth()));

        // Show loading progress for this month
        Integer progress = monthToProgressMap.get(item.monthDate);
        if (progress != null) {
            holder.loadingProgress.setProgress(progress);
        }
    }

    private void bindErrorDay(ErrorDayViewHolder holder, VirtualItem item) {
        holder.dayNumber.setText(String.valueOf(item.date.getDayOfMonth()));
        holder.errorIndicator.setVisibility(View.VISIBLE);

        // Add retry click listener
        holder.itemView.setOnClickListener(v -> {
            dataManager.refreshMonth(item.monthDate);
        });
    }

    // ==================== 6. DATA AVAILABILITY CALLBACKS ====================

    @Override
    public void onDataStateChanged(LocalDate month, VirtualCalendarDataManager.DataState state, List<Day> data) {
        Log.d(TAG, "Data state changed for " + month + ": " + state);

        // Update all items for this month
        updateMonthItems(month, state, data);
    }

    @Override
    public void onLoadingProgress(LocalDate month, int progressPercent) {
        monthToProgressMap.put(month, progressPercent);

        // Update progress for loading items in this month
        updateMonthProgress(month, progressPercent);
    }

    /**
     * Update all virtual items for a specific month
     */
    private void updateMonthItems(LocalDate month, VirtualCalendarDataManager.DataState state, List<Day> data) {
        List<Integer> positionsToUpdate = new ArrayList<>();

        // Find all items for this month
        for (Map.Entry<Integer, VirtualItem> entry : virtualItems.entrySet()) {
            VirtualItem item = entry.getValue();
            if (item.monthDate.equals(month) && item.type == VirtualItem.ItemType.DAY_CELL) {
                item.dataState = state;

                // Update loaded data if available
                if (data != null) {
                    item.loadedData = findDayInList(data, item.date);
                }

                positionsToUpdate.add(entry.getKey());
            }
        }

        // Notify adapter of changes
        for (Integer position : positionsToUpdate) {
            notifyItemChanged(position);
        }
    }

    /**
     * Update loading progress for a specific month
     */
    private void updateMonthProgress(LocalDate month, int progressPercent) {
        for (Map.Entry<Integer, VirtualItem> entry : virtualItems.entrySet()) {
            VirtualItem item = entry.getValue();
            if (item.monthDate.equals(month) &&
                    item.dataState == VirtualCalendarDataManager.DataState.LOADING) {
                notifyItemChanged(entry.getKey());
            }
        }
    }

    // ==================== 7. SCROLL HANDLING ====================

    /**
     * Update scroll behavior for intelligent prefetching
     */
    public void updateScrollBehavior(VirtualCalendarDataManager.ScrollDirection direction, int velocity, LocalDate centerMonth) {
        this.lastScrollDirection = direction;
        this.lastScrollVelocity = velocity;

        // Check if we need to regenerate virtual items (month change)
        if (!centerMonth.equals(this.currentCenterMonth)) {
            this.currentCenterMonth = centerMonth;
            regenerateVirtualItemsIfNeeded(centerMonth);
        }
    }

    /**
     * Regenerate virtual items if user scrolled to a different month range
     */
    private void regenerateVirtualItemsIfNeeded(LocalDate newCenterMonth) {
        // Check if current virtual items cover the new center month
        boolean needsRegeneration = virtualItems.values().stream()
                .noneMatch(item -> item.monthDate.equals(newCenterMonth));

        if (needsRegeneration) {
            Log.d(TAG, "Regenerating virtual items for new center month: " + newCenterMonth);
            generateVirtualItems(newCenterMonth);
            notifyDataSetChanged(); // Full refresh needed
        }
    }

    // ==================== 8. ANIMATION HELPERS ====================

    /**
     * Setup smooth transition animation for data appearance
     */
    private void setupDataTransitionAnimation() {
        dataTransitionAnimator.setDuration(300);
        dataTransitionAnimator.setInterpolator(new DecelerateInterpolator());
    }

    /**
     * Animate transition from skeleton to loaded data
     */
    private void animateDataAppearance(View itemView) {
        itemView.setAlpha(0.7f);
        itemView.setScaleX(0.95f);
        itemView.setScaleY(0.95f);

        itemView.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();
    }

    // ==================== 9. UTILITY METHODS ====================

    /**
     * Find a specific day in the loaded data list
     */
    private Day findDayInList(List<Day> dayList, LocalDate targetDate) {
        return dayList.stream()
                .filter(day -> day.getDate().equals(targetDate))
                .findFirst()
                .orElse(null);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        dataManager.shutdown();
        monthToProgressMap.clear();
        virtualItems.clear();
    }
}