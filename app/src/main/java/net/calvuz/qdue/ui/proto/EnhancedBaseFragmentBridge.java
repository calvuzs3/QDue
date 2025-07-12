package net.calvuz.qdue.ui.proto;

import static net.calvuz.qdue.QDue.VirtualScrollingSettings.ENABLE_VIRTUAL_SCROLLING;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.architecture.base.BaseFragment;
import net.calvuz.qdue.ui.shared.models.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Migration helper that extends existing BaseFragment with virtual scrolling capabilities
 * This allows gradual migration of existing fragments
 */
public abstract class EnhancedBaseFragmentBridge extends BaseFragment {

    private static final String TAG = "BF-Bridge";

    protected AdapterBridge adapterBridge;
    protected VirtualScrollListener virtualScrollListener;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ENABLE_VIRTUAL_SCROLLING) {
            setupVirtualScrolling();
            //loadInitialVirtualData(); // populate - moved into setupAdapter
        }
    }

    /**
     * Setup virtual scrolling enhancements
     */
    protected void setupVirtualScrolling() {
        if (mRecyclerView != null) {
            // Add virtual scroll listener
            virtualScrollListener = new VirtualScrollListener();
            mRecyclerView.addOnScrollListener(virtualScrollListener);

            Log.d(TAG, "Virtual scrolling setup completed");
        }
    }

    protected void loadInitialVirtualData() {
        if (adapterBridge != null) {
            // ✅ Access VirtualDataManager through AdapterBridge
            adapterBridge.requestInitialData();

            Log.d(TAG, "loadInitialVirtualData: ✅ Requesting initial virtual data via AdapterBridge");
        } else {
            Log.e(TAG, "loadInitialVirtualData: ❌ AdapterBridge is null - cannot load virtual data");
        }
    }

    @Override
    protected void setupScrollToToday() {
        if (ENABLE_VIRTUAL_SCROLLING) {
            // ✅ VIRTUAL: Simplified scroll-to-today
            setupVirtualScrollToToday();
            return;
        }
        super.setupScrollToToday();
    }

    private void setupVirtualScrollToToday() {
        if (mFabGoToToday != null) {
            mFabGoToToday.setOnClickListener(v -> {
                // Simple: scroll to center of viewport
                if (mRecyclerView != null && adapterBridge != null) {
                    int centerPosition = adapterBridge.getItemCount() / 2;
                    mRecyclerView.smoothScrollToPosition(centerPosition);
                }
            });
        }
    }

    @Override
    protected void ensureAdapterSyncWithCache() {
        if (ENABLE_VIRTUAL_SCROLLING) {
            // ✅ SKIP: Virtual adapter doesn't use cache
            return;
        }
        super.ensureAdapterSyncWithCache();
    }

    /**
     * Enhanced scroll listener for virtual scrolling
     */
    private class VirtualScrollListener extends RecyclerView.OnScrollListener {

        private long lastScrollTime = 0;
        private VirtualCalendarDataManager.ScrollDirection lastDirection =
                VirtualCalendarDataManager.ScrollDirection.STATIONARY;

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (!ENABLE_VIRTUAL_SCROLLING || adapterBridge == null) {
                return;
            }

            long currentTime = System.currentTimeMillis();

            // Calculate scroll direction and velocity
            VirtualCalendarDataManager.ScrollDirection direction = determineScrollDirection(dy);
            int velocity = calculateVelocity(dy, currentTime);

            // Get current visible month (you'll need to implement this based on your layout)
            LocalDate visibleMonth = getCurrentVisibleMonth();

            // Update adapter with scroll behavior
            adapterBridge.updateScrollBehavior(direction, velocity, visibleMonth);

            lastScrollTime = currentTime;
            lastDirection = direction;
        }

        private VirtualCalendarDataManager.ScrollDirection determineScrollDirection(int dy) {
            if (dy > 5) return VirtualCalendarDataManager.ScrollDirection.FORWARD;
            if (dy < -5) return VirtualCalendarDataManager.ScrollDirection.BACKWARD;
            return VirtualCalendarDataManager.ScrollDirection.STATIONARY;
        }

        private int calculateVelocity(int dy, long currentTime) {
            if (lastScrollTime == 0) return 0;
            long timeDiff = currentTime - lastScrollTime;
            if (timeDiff <= 0) return 0;
            return (int) Math.abs((dy * 1000.0) / timeDiff);
        }
    }

    /**
     * Get currently visible month - subclasses should implement based on their layout
     */
    protected LocalDate getCurrentVisibleMonth() {
        // Default implementation - subclasses should override
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Override adapter setup to use bridge adapter if ENABLE_VIRTUAL_SCROLLING is true
     */
    @Override
    protected void setupAdapter() {
        final String mTAG = "setupAdapter: ";

        if (ENABLE_VIRTUAL_SCROLLING) {
            // Create bridge adapter instead of regular adapter
            adapterBridge = new AdapterBridge(
                    getContext(),
                    mItemsCache,
                    getHalfTeam(),
                    getShiftsToShow()
            );

            mRecyclerView.setAdapter(adapterBridge);
            setFragmentAdapter(adapterBridge);

            // ✅ NOW call data loading - adapter exists!
            loadInitialVirtualData();

            Log.d(TAG, mTAG + "✅ Bridge adapter setup completed");
            return;
        } else {

            // Legacy direct implementation
            setupLegacyAdapter();
            Log.d(TAG, mTAG + "✅ Legacy adapter setup completed");
        }
    }

    /**
     * The parent class still works with a bigger cache
     */
    @Override
    protected void setupInfiniteScrolling() {
        if (ENABLE_VIRTUAL_SCROLLING) {
            // ✅ VIRTUAL: Use virtual scrolling logic
            setupVirtualInfiniteScrolling();
            setupHybridVirtualScrolling();
            return;
        }
        super.setupInfiniteScrolling();
    }

    private void setupVirtualInfiniteScrolling() {
        // Virtual scrolling doesn't need infinite scroll
        // The VirtualCalendarDataManager handles loading on demand
        Log.d(TAG, "setupVirtualInfiniteScrolling: ✅ Virtual infinite scrolling setup - handled by VirtualCalendarDataManager");
    }

    private void setupHybridVirtualScrolling() {
        final String mTAG = "setupHybridVirtualScrolling: ";
        Log.d(TAG, mTAG + "called");

        try {
            // ✅ 1. Reset flags (from BaseFragment)
            resetControlFlags();

            // ✅ 2. Initialize cache essentials
            mCurrentDate = QDue.getQuattrodue().getCursorDate();
            mItemsCache = new ArrayList<>();

            // ✅ 3. Generate minimal cache for compatibility
            // Virtual mode doesn't need full cache, but BaseFragment expects some items
            generateMinimalCacheForVirtual();

            // ✅ 4. Setup adapter (now mItemsCache has items)
            setupAdapter();

            // ✅ 5. Calculate today position for compatibility
            mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

            // ✅ 6. Setup scroll listeners
            setupScrollListeners();

            Log.d(TAG, mTAG + "Hybrid virtual setup completed: " + mItemsCache.size() +
                    " compatibility items, today at: " + mTodayPosition);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error: " + e.getMessage());
        }
    }

    /**
     * Generate minimal cache items for BaseFragment compatibility
     * Virtual adapter will override these, but BaseFragment needs non-empty cache
     */
    private void generateMinimalCacheForVirtual() {
        // Generate current month only for compatibility
        LocalDate currentMonth = mCurrentDate.withDayOfMonth(1);

        // Add month header
        mItemsCache.add(new SharedViewModels.MonthHeader(currentMonth));

        // Add placeholder days for current month
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate dayDate = currentMonth.withDayOfMonth(day);
            // Create minimal day item
            Day placeholderDay = new Day(dayDate);
            mItemsCache.add(new SharedViewModels.DayItem(placeholderDay, currentMonth));
        }

        Log.d(TAG, "Generated " + mItemsCache.size() + " compatibility items for virtual mode");
    }

    // ==================== CLEANUP ====================

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (adapterBridge != null) {
            adapterBridge.cleanup();
        }

        if (virtualScrollListener != null && mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(virtualScrollListener);
        }
    }

    // ==================== ABSTRACT METHODS FOR SUBCLASSES ====================

    /**
     * Get user half team for adapter
     */
    protected abstract HalfTeam getHalfTeam();

    /**
     * Get number of shifts to show
     */
    protected abstract int getShiftsToShow();

    /**
     * Setup Legacy Adapter
     */
    protected abstract void setupLegacyAdapter();
}