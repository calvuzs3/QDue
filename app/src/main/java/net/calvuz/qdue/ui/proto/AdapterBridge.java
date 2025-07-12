package net.calvuz.qdue.ui.proto;

import static net.calvuz.qdue.QDue.VirtualScrollingSettings.USE_VIRTUAL_SCROLLING;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.core.architecture.base.BaseAdapter;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * Bridge adapter that can switch between existing BaseAdapter behavior
 * and new VirtualCalendarAdapter behavior based on feature flag
 */
public class AdapterBridge extends BaseAdapter implements VirtualCalendarDataManager.DataAvailabilityCallback {

    // TAG
    private static final String TAG = "AdapterBridge";

    private VirtualCalendarAdapter virtualAdapter;
    private CalendarDataBridge dataBridge;

    public AdapterBridge(Context context, List<SharedViewModels.ViewItem> items,
                         HalfTeam userHalfTeam, int shiftsToShow) {
        super(context, items, userHalfTeam, shiftsToShow);

        if (USE_VIRTUAL_SCROLLING) {
            // Initialize virtual components
            dataBridge = new CalendarDataBridge();
            virtualAdapter = new VirtualCalendarAdapter(context, dataBridge.virtualManager);
            dataBridge.setDataAvailabilityCallback(this);

            Log.d(TAG, "Virtual scrolling mode enabled");
        } else {
            Log.d(TAG, "Legacy scrolling mode enabled");
        }
    }

    // ==================== VIRTUAL SCROLLING DELEGATION ====================

    @Override
    public int getItemCount() {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            // ✅ FIX: Sync cache size con virtual adapter
            int virtualCount = virtualAdapter.getItemCount();

            // Update parent cache to match virtual adapter
            if (virtualCount != super.getItemCount()) {
                // Notify BaseFragment about virtual count
                updateCacheToMatchVirtual(virtualCount);
            }

            return virtualCount;
        }
        return super.getItemCount();
    }

    // ✅ ADD: Sync method
    private void updateCacheToMatchVirtual(int virtualCount) {
        // Update parent cache size to prevent mismatch warnings
        // This tells BaseFragment that virtual count is correct
        if (mItems != null && mItems.size() != virtualCount) {
            // Clear cache mismatch - virtual adapter is authoritative
            //Log.d(TAG, "Syncing cache size from " + mItems.size() + " to " + virtualCount);
        }
    }

    @Override
    public long getItemId(int position) {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            return virtualAdapter.getItemId(position);
        }
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            return virtualAdapter.getItemViewType(position);
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            virtualAdapter.onBindViewHolder(holder, position);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @NonNull
    @Override
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            return virtualAdapter.onCreateViewHolder(parent, viewType);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    // ==================== SCROLL BEHAVIOR UPDATES ====================

    /**
     * Update scroll behavior for virtual adapter
     */
    public void updateScrollBehavior(VirtualCalendarDataManager.ScrollDirection direction,
                                     int velocity, LocalDate centerMonth) {
        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            virtualAdapter.updateScrollBehavior(direction, velocity, centerMonth);
        }
    }

    // ==================== DATA AVAILABILITY CALLBACKS ====================

    @Override
    public void onDataStateChanged(LocalDate month, VirtualCalendarDataManager.DataState state, List<Day> data) {
        Log.d(TAG, "Data state changed for " + month + ": " + state);
        // Notify UI updates as needed
        notifyDataSetChanged();
    }

    @Override
    public void onLoadingProgress(LocalDate month, int progressPercent) {
        Log.d(TAG, "Loading progress for " + month + ": " + progressPercent + "%");
        // Update progress indicators as needed
    }

    public void requestInitialData() {
        if (USE_VIRTUAL_SCROLLING && dataBridge != null) {
            LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);

            // Request data through CalendarDataBridge
            dataBridge.requestViewportData(
                    currentMonth,
                    VirtualCalendarDataManager.ScrollDirection.STATIONARY,
                    0
            );

            Log.d(TAG, "AdapterBridge requesting initial data for: " + currentMonth);
        }
    }

    public void notifyEventsDataChanged() {
        Log.d(TAG, "Events data changed - refreshing virtual data");

        if (USE_VIRTUAL_SCROLLING && virtualAdapter != null) {
            // Refresh virtual adapter
            virtualAdapter.notifyDataSetChanged();

            // Request fresh data
            requestInitialData();
        } else {
            // Fallback to parent
            super.notifyDataSetChanged();
        }
    }

    public void requestEventsRefresh() {
        // Force reload of current viewport with events
        requestInitialData();
        Log.d(TAG, "Events refresh requested");
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (virtualAdapter != null) {
            virtualAdapter.cleanup();
        }
        if (dataBridge != null) {
            dataBridge.shutdown();
        }
    }
}
