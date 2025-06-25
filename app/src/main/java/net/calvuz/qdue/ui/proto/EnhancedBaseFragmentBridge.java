package net.calvuz.qdue.ui.proto;

import static net.calvuz.qdue.QDue.VirtualScrollingSettings.ENABLE_VIRTUAL_SCROLLING;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;

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
        Log.v(TAG, mTAG + "called.");

        if (ENABLE_VIRTUAL_SCROLLING) {
            // Create bridge adapter instead of regular adapter
            adapterBridge = new AdapterBridge(
                    getContext(),
                    mItemsCache,
                    getHalfTeam(), // You'll need to implement this method
                    getShiftsToShow() // You'll need to implement this method
            );

            mRecyclerView.setAdapter(adapterBridge);
            setFragmentAdapter(adapterBridge);

            Log.d(TAG, "Bridge adapter setup completed");
        } else {

            // CRITICAL FIX
            // REMOVED: super.setupAdapter();
            // USE: legacy direct implementation
            setupLegacyAdapter();

            Log.d(TAG, "Legacy adapter setup completed");
        }
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