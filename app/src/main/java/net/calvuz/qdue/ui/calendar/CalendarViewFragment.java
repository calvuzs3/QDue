package net.calvuz.qdue.ui.calendar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enhanced CalendarViewFragment with events support.
 *
 * CALENDAR EVENTS INTEGRATION - IMPLEMENTATION:
 * - Extends BaseFragment with events database integration
 * - Uses enhanced CalendarAdapter with events indicators
 * - Maintains visual consistency with DaysListFragment
 * - Optimized for calendar grid layout with compact events display
 *
 * Key Features:
 * - 7-column calendar grid layout
 * - Simple events indicators (count badges)
 * - Automatic events loading from database
 * - Visual consistency with DaysListFragment
 * - Performance optimized for scrolling
 */
public class CalendarViewFragment extends BaseFragment {

    // TAG
    private static final String TAG = "CalendarViewFragment";

    // Enhanced adapter with events support
    private CalendarAdapter mAdapter;

    // ==================== LIFECYCLE METHODS ====================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "=== onViewCreated CALLED ===");

        // FIX: Programmare aggiornamento eventi dopo che la view è creata
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "=== FORCING EVENTS FROM onViewCreated ===");
            forceEventsUpdate();
        }, 3000); // 3 secondi dopo creazione view
    }

    /**
     * Se notifyEventsDataChanged() non viene chiamato, aggiungere questo in onResume():
     */
    @Override
    public void onResume() {
        Log.d(TAG, "=== onResume CALLED - START ===");
        super.onResume();

        Log.d(TAG, "=== FORCING EVENTS UPDATE FROM onResume ===");

        // FIX: Aggiornamento immediato
        forceEventsUpdate();

        // FIX: Aggiornamento ritardato
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "=== DELAYED FORCING FROM onResume ===");
            forceEventsUpdate();
        }, 1500);

        Log.d(TAG, "=== onResume COMPLETED ===");
    }




    // ==================== BASE FRAGMENT IMPLEMENTATION ====================

    @Override
    protected BaseAdapter getFragmentAdapter() {
        return mAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        this.mAdapter = (CalendarAdapter) adapter;
    }

    @Override
    protected void findViews(View rootView) {
        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
    }

    /**
     * Return 7 columns for calendar grid layout.
     * This creates the traditional week-based calendar view.
     */
    @Override
    protected int getGridColumnCount() {
        return 7; // Seven columns for calendar days (Sun-Sat)
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // For calendar, convert including empty cells to complete the grid
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        Log.d(TAG, "=== setupAdapter CALLED ===");

        mAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam()
        );

        mRecyclerView.setAdapter(mAdapter);
        Log.d(TAG, "CalendarAdapter set to RecyclerView");

        // FIX: FORZARE AGGIORNAMENTO EVENTI SUBITO
        Log.d(TAG, "=== FORCING EVENTS IN SETUP ADAPTER ===");
        forceEventsUpdate();

        // FIX: Programmare aggiornamenti multipli per essere sicuri
        scheduleDelayedEventsUpdates();

        loadAndUpdateEvents();
        Log.d(TAG, "=== setupAdapter COMPLETED ===");
    }


    /**
     * Metodo helper per forzare aggiornamento eventi
     */
    private void forceEventsUpdate() {
        Log.d(TAG, "forceEventsUpdate: START");

        if (mAdapter != null) {
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            Log.d(TAG, "forceEventsUpdate: Cache has " + eventsCache.size() + " dates");

            // Debug contenuto cache
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsCache.entrySet()) {
                LocalDate date = entry.getKey();
                List<LocalEvent> events = entry.getValue();
                Log.d(TAG, "forceEventsUpdate: " + date + " → " + events.size() + " events");

                for (LocalEvent event : events) {
                    Log.d(TAG, "  - " + event.getTitle());
                }
            }

            // APPLICARE eventi all'adapter
            mAdapter.updateEventsData(eventsCache);
            Log.d(TAG, "forceEventsUpdate: updateEventsData called");

        } else {
            Log.e(TAG, "forceEventsUpdate: mAdapter is NULL!");
        }

        Log.d(TAG, "forceEventsUpdate: END");
    }


    /**
     * Programmare aggiornamenti ritardati multipli
     */
    private void scheduleDelayedEventsUpdates() {
        Log.d(TAG, "scheduleDelayedEventsUpdates: Scheduling updates");

        // Update dopo 500ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "=== DELAYED UPDATE 500ms ===");
            forceEventsUpdate();
        }, 500);

        // Update dopo 1 secondo
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "=== DELAYED UPDATE 1000ms ===");
            forceEventsUpdate();
        }, 1000);

        // Update dopo 2 secondi
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "=== DELAYED UPDATE 2000ms ===");
            forceEventsUpdate();
        }, 2000);
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            // For grid calendar, calculate if today is visible
            // Check if today is within the visible range
            showFab = !(firstVisible <= mTodayPosition && mTodayPosition <= lastVisible);
        }

        // Animate FAB visibility
        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

    // ==================== EVENTS INTEGRATION METHODS ====================

    /**
     * STEP 1: Initialize events database integration.
     * Called during fragment setup.
     */
    private void initializeEventsDatabase() {
        if (mEventsDatabase == null) {
            Log.v(TAG, "Initializing events database");
            // mEventsDatabase is inherited from BaseFragment
            // It's already initialized in BaseFragment constructor
        }
    }

    /**
     * STEP 2: Load events from database and update adapter.
     * This is the main integration method that connects events to calendar display.
     */
    private void loadAndUpdateEvents() {
        if (mAdapter == null) {
            Log.v(TAG, "Adapter not ready, will load events later");
            return;
        }

        Log.v(TAG, "Loading events for calendar display");

        // Use the enhanced CalendarAdapter's async loading method
        mAdapter.loadEventsAsync();
    }

    /**
     * STEP 3: Update events data from external source.
     * PUBLIC method for integration with other components.
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        if (mAdapter != null) {
            mAdapter.updateEventsData(eventsMap);
            Log.d(TAG, "Updated calendar with events data for " + eventsMap.size() + " dates");
        }
    }

    /**
     * STEP 4: Refresh events data.
     * Called when events are modified externally.
     */
    public void refreshEvents() {
        Log.v(TAG, "Refreshing events data");
        loadAndUpdateEvents();
    }

    // ==================== EVENTS INTERFACE IMPLEMENTATION ====================

    /**
     * Implementation of NotifyUpdatesInterface for events updates.
     * Called when events data changes in the system.
     */
//    @Override
    public void onEventsUpdated() {
        Log.v(TAG, "Events updated notification received");
        refreshEvents();
    }

    // ==================== DEBUGGING AND UTILITY ====================

    /**
     * Debug method to check events integration status.
     * Can be called from debugging tools.
     */
    public void debugEventsIntegration() {
        Log.d(TAG, "=== CALENDAR EVENTS DEBUG ===");
        Log.d(TAG, "Adapter: " + (mAdapter != null ? "initialized" : "null"));
        Log.d(TAG, "Database: " + (mEventsDatabase != null ? "initialized" : "null"));

        if (mAdapter != null) {
            // This would require adding a debug method to CalendarAdapter
            Log.d(TAG, "Adapter events status: integrated");
        }

        Log.d(TAG, "=== END DEBUG ===");
    }




    /**
     * Metodo di debug per forzare reload eventi
     */
    public void debugForceEventsReload() {
        Log.d(TAG, "=== DEBUG FORCE EVENTS RELOAD ===");

        // 1. Clear cache eventi
        mEventsCache.clear();

        // 2. Force reload da database
        loadEventsForCurrentPeriod();

        // 3. Aspetta un po' e poi aggiorna adapter
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mAdapter != null) {
                Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
                Log.d(TAG, "Force reload: updating adapter with " + eventsCache.size() + " dates");
                mAdapter.updateEventsData(eventsCache);
            }
        }, 1000); // 1 secondo di delay

        Log.d(TAG, "=== END DEBUG FORCE RELOAD ===");
    }

    /**
     * Override da BaseFragment - QUESTO METODO MANCA COMPLETAMENTE!
     * Chiamato quando BaseFragment carica eventi dal database
     * ASSICURARSI che questo metodo sia implementato CORRETTAMENTE:
     */

    @Override
    protected void notifyEventsDataChanged() {
        Log.d(TAG, "=== notifyEventsDataChanged OVERRIDE CALLED ===");

        // Chiamare super first
        super.notifyEventsDataChanged();

        // FIX: Forzare aggiornamento
        forceEventsUpdate();

        Log.d(TAG, "=== notifyEventsDataChanged OVERRIDE COMPLETED ===");
    }

// ========================================
// VERIFICA: CalendarViewFragment.java - Metodo pubblico per test
// ========================================

    /**
     * Metodo pubblico per testare manualmente l'aggiornamento eventi
     */
    public void debugManualEventsUpdate() {
        Log.d(TAG, "=== MANUAL EVENTS UPDATE TRIGGERED ===");

        if (mAdapter != null) {
            Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
            Log.d(TAG, "Manual update: cache has " + eventsCache.size() + " dates");

            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsCache.entrySet()) {
                Log.d(TAG, "Manual: " + entry.getKey() + " → " + entry.getValue().size() + " events");
            }

            mAdapter.updateEventsData(eventsCache);
            Log.d(TAG, "Manual update completed");
        }
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate CALLED ===");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "=== onStart CALLED ===");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "=== onPause CALLED ===");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "=== onStop CALLED ===");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== onDestroy CALLED ===");
    }
}