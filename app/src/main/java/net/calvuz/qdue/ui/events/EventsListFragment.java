package net.calvuz.qdue.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.backup.BackupIntegration;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.ui.events.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsFileOperationsInterface;
import net.calvuz.qdue.utils.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EventsListFragment - Display list of events with navigation to details
 * <p>
 * Features:
 * - RecyclerView with EventsAdapter
 * - Click handling for navigation to details
 * - Menu integration for import/export
 * - Empty state management
 * - Search functionality
 * <p>
 * Navigation:
 * - Click event → EventDetailFragment with eventId argument
 */
public class EventsListFragment extends Fragment implements
        EventsAdapter.OnEventClickListener {

    private static final String TAG = "EventsListFragment";

    // Views
    private RecyclerView mEventsRecyclerView;
    private View mEmptyStateView;
    private View mLoadingStateView;

    // Data
    private EventsAdapter mEventsAdapter;
    private List<LocalEvent> mEventsList;

    // Interfaces
    private EventsFileOperationsInterface mFileOperationsInterface;
    private EventsDatabaseOperationsInterface mDataOperationsInterface;

    // Deletions
    private Set<String> mPendingDeletionIds = new HashSet<>();
    private boolean mIsRefreshSuppressed = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize data
        mEventsList = new ArrayList<>();
        mFileOperationsInterface = (EventsFileOperationsInterface) getActivity();
        mDataOperationsInterface = (EventsDatabaseOperationsInterface) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        loadEvents();
    }

    /**
     * Initialize view references
     */
    private void initializeViews(View view) {
        mEventsRecyclerView = view.findViewById(R.id.recycler_view_events);
        mEmptyStateView = view.findViewById(R.id.empty_state_events);
        mLoadingStateView = view.findViewById(R.id.loading_state_events);
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        mEventsAdapter = new EventsAdapter(mEventsList, this);
        mEventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mEventsRecyclerView.setAdapter(mEventsAdapter);
    }

    /**
     * Load events from database
     */
    private void loadEvents() {
        // Skip loading if refresh is suppressed (during pending deletions)
        if (mIsRefreshSuppressed) {
            Log.d(TAG, "loadEvents() skipped - refresh suppressed during pending operations");
            return;
        }

        showLoading(true);

        try {
            // Get database instance and DAO
            EventDao eventDao = EventsDatabase.getInstance(requireContext()).eventDao();

            // Load all events asynchronously
            new Thread(() -> {
                try {
                    List<LocalEvent> events = eventDao.getAllEvents();

                    // Update UI on main thread
                    requireActivity().runOnUiThread(() -> {
                        // Filter out events with pending deletion
                        List<LocalEvent> filteredEvents = filterPendingDeletions(events);

                        mEventsList.clear();
                        if (filteredEvents != null && !filteredEvents.isEmpty()) {
                            mEventsList.addAll(filteredEvents);
                            Log.d(TAG, "Loaded " + filteredEvents.size() + " events from database (filtered " +
                                    (events.size() - filteredEvents.size()) + " pending deletions)");
                        }
                        mEventsAdapter.notifyDataSetChanged();
                        updateViewState();
                        showLoading(false);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error loading events from database: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Errore caricamento eventi", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error getting database instance: " + e.getMessage());
            updateViewState();
            showLoading(false);
        }
    }

    /**
     * Filter out events that are pending deletion
     */
    private List<LocalEvent> filterPendingDeletions(List<LocalEvent> events) {
        if (mPendingDeletionIds.isEmpty() || events == null) {
            return events;
        }

        List<LocalEvent> filtered = new ArrayList<>();
        for (LocalEvent event : events) {
            if (event != null && event.getId() != null &&
                    !mPendingDeletionIds.contains(event.getId())) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Add event to pending deletion list
     */
    public void addToPendingDeletion(String eventId) {
        if (eventId != null) {
            mPendingDeletionIds.add(eventId);
            Log.d(TAG, "Added to pending deletion: " + eventId);
        }
    }

    /**
     * Remove event from pending deletion list (when deletion confirmed or cancelled)
     */
    public void removeFromPendingDeletion(String eventId) {
        if (eventId != null) {
            boolean removed = mPendingDeletionIds.remove(eventId);
            Log.d(TAG, "Removed from pending deletion: " + eventId + " (was present: " + removed + ")");
        }
    }

    /**
     * Clear all pending deletions
     */
    public void clearPendingDeletions() {
        mPendingDeletionIds.clear();
        Log.d(TAG, "Cleared all pending deletions");
    }

    /**
     * Suppress refresh temporarily
     */
    public void suppressRefresh(boolean suppress) {
        mIsRefreshSuppressed = suppress;
        Log.d(TAG, "Refresh suppressed: " + suppress);
    }


    /**
     * Refresh events list (called from parent activity)
     */
    public void refreshEvents() {
        Log.d(TAG, "refreshEvents() called - forcing refresh");
        mIsRefreshSuppressed = false; // Always allow explicit refresh
        loadEvents();
    }

    /**
     * Update view state based on data availability
     */
    private void updateViewState() {
        if (mEventsList.isEmpty()) {
            mEventsRecyclerView.setVisibility(View.GONE);
            mEmptyStateView.setVisibility(View.VISIBLE);
        } else {
            mEventsRecyclerView.setVisibility(View.VISIBLE);
            mEmptyStateView.setVisibility(View.GONE);
        }
    }

    /**
     * Show/hide loading state
     */
    private void showLoading(boolean show) {
        if (show) {
            mLoadingStateView.setVisibility(View.VISIBLE);
            mEventsRecyclerView.setVisibility(View.GONE);
            mEmptyStateView.setVisibility(View.GONE);
        } else {
            mLoadingStateView.setVisibility(View.GONE);
        }
    }

    // ==================== MENU HANDLING ====================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_events_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_import_events_file) {
            handleImportEventsFromFile();
            return true;
        } else if (itemId == R.id.action_import_events_url) {
            handleImportEventsFromUrl();
            return true;
        } else if (itemId == R.id.action_clear_all) {
            handleClearAllEvents();
            return true;
        } else if (itemId == R.id.action_refresh_events) {
            handleRefreshEvents();
            return true;
        }
//        else if (itemId == R.id.action_export_events) {
//            handleExportEvents();
//            return true;
//        }  else if (itemId == R.id.action_search_events) {
//            handleSearchEvents();
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== EVENT CLICK HANDLING ====================

    /**
     * Handle event item click - Navigate to detail fragment
     */
    @Override
    public void onEventClick(LocalEvent event) {
        Log.d(TAG, "onEventClick called for: " + (event != null ? event.getTitle() : "null"));

        if (event == null || event.getId() == null) {
            Toast.makeText(getContext(), "Errore: evento non valido", Toast.LENGTH_SHORT).show();
            return;
        }

        // DEBUG: Log current state before navigation
        logCurrentEvents();

        // Navigate to EventDetailFragment using Navigation Component
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());

        try {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_events_list_to_event_detail, args);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            Toast.makeText(getContext(), "Errore navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle event long click - Show context menu
     */
    @Override
    public void onEventLongClick(LocalEvent event) {
        if (event == null) return;

        // TODO: Show context menu with options (edit, delete, share, etc.)
        showEventContextMenu(event);
    }

    // ==================== ACTION HANDLERS ====================

    private void handleImportEventsFromFile() {
        if (mFileOperationsInterface != null) {
            mFileOperationsInterface.triggerImportEventsFromFile();
        }
    }

    private void handleImportEventsFromUrl() {
        if (mFileOperationsInterface != null) {
            mFileOperationsInterface.triggerImportEventsFromUrl();
        }
    }

    private void handleClearAllEvents() {
        if (mDataOperationsInterface != null) {
            mDataOperationsInterface.triggerDeleteAllEvents();
        }
    }

    //    private void handleExportEvents() {
//        // TODO: Integrate with existing export functionality from EventsActivity
//        Toast.makeText(getContext(), "Export eventi - TODO", Toast.LENGTH_SHORT).show();
//    }

    private void handleRefreshEvents() {
        // Simply reload events from db
        loadEvents();
        Toast.makeText(getContext(), "Eventi aggiornati", Toast.LENGTH_SHORT).show();
    }

//    private void handleSearchEvents() {
//        // TODO: Implement search functionality
//        Toast.makeText(getContext(), "Ricerca eventi - TODO", Toast.LENGTH_SHORT).show();
//    }
//
    private void showEventContextMenu(LocalEvent event) {
        // TODO: Show context menu with options
        Toast.makeText(getContext(), "Menu contestuale per: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Add new event to list (called after creation)
     */
    public void addEvent(LocalEvent event) {
        Log.d(TAG, "addEvent called for: " + (event != null ? event.getTitle() : "null"));

        if (event != null) {
            // Check if event already exists to avoid duplicates
            boolean exists = false;
            for (LocalEvent existingEvent : mEventsList) {
                if (existingEvent != null && event.getId() != null &&
                        event.getId().equals(existingEvent.getId())) {
                    Log.w(TAG, "Event already exists in list: " + event.getTitle());
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                mEventsList.add(0, event); // Add at top
                mEventsAdapter.notifyItemInserted(0);
                mEventsRecyclerView.scrollToPosition(0);
                updateViewState();
                Log.d(TAG, "Successfully added event: " + event.getTitle());
            }
        } else {
            Log.w(TAG, "Cannot add null event to list");
        }
    }

    /**
     * Remove event from list (called after deletion)
     */
    public void removeEvent(String eventId) {
        Log.d(TAG, "removeEvent called for eventId: " + eventId);

        if (eventId == null || eventId.trim().isEmpty()) {
            Log.w(TAG, "Cannot remove event - eventId is null or empty");
            return;
        }

        boolean found = false;
        for (int i = 0; i < mEventsList.size(); i++) {
            LocalEvent event = mEventsList.get(i);
            if (event != null && eventId.equals(event.getId())) {
                String eventTitle = event.getTitle();
                mEventsList.remove(i);
                mEventsAdapter.notifyItemRemoved(i);

                // Also notify range changed for items after the removed one
                if (i < mEventsList.size()) {
                    mEventsAdapter.notifyItemRangeChanged(i, mEventsList.size() - i);
                }

                updateViewState();
                found = true;
                Log.d(TAG, "Successfully removed event: " + eventTitle + " (ID: " + eventId + ") at position: " + i);
                break;
            }
        }

        if (!found) {
            Log.w(TAG, "Event not found in list for removal: " + eventId);
            // Force a complete refresh as fallback
            refreshEvents();
        }
    }



    /**
     * Update existing event in list (called after editing)
     */
    public void updateEvent(LocalEvent updatedEvent) {
        if (updatedEvent == null || updatedEvent.getId() == null) return;

        for (int i = 0; i < mEventsList.size(); i++) {
            if (mEventsList.get(i).getId().equals(updatedEvent.getId())) {
                mEventsList.set(i, updatedEvent);
                mEventsAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Get events list
     *
     * @return LocalEvent List
     */
    public List<LocalEvent> getEventsList() {
        return mEventsList;
    }

    // ==================== PUBLIC INTERFACE ====================

    public void onEventsCleared(int clearedCount, boolean createBackup) {
        // Clear all
        mEventsList.clear();
        onRefreshRequired(null);

        // Trigger AUTO BACKUP after clear
        if (createBackup)
            BackupIntegration.integrateWithClearAll(getContext(), mEventsList);
    }

    public void onEventCreated(LocalEvent event) {
        // Add Event
        addEvent(event);
        // Gentle Notify
        onRefreshRequired(null);
    }

    public void onRefreshRequired(String reason) {
        // Brutal Notify
        mEventsAdapter.notifyDataSetChanged();
    }


    // ================================== DEBUG =================================

    /**
     * DEBUG:
     */
    public void logCurrentEvents() {
        Log.d(TAG, "=== CURRENT EVENTS LIST ===");
        Log.d(TAG, "Total events: " + mEventsList.size());
        for (int i = 0; i < mEventsList.size(); i++) {
            LocalEvent event = mEventsList.get(i);
            if (event != null) {
                Log.d(TAG, i + ": " + event.getTitle() + " (ID: " + event.getId() + ")");
            } else {
                Log.d(TAG, i + ": NULL EVENT");
            }
        }
        Log.d(TAG, "=== END EVENTS LIST ===");
    }

}