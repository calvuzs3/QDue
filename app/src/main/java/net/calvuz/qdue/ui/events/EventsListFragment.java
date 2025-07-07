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

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.backup.BackupIntegration;
import net.calvuz.qdue.core.ui.interfaces.SelectionModeHandler;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.core.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.core.interfaces.EventsOperationsInterface;
import net.calvuz.qdue.core.interfaces.EventsFileOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsUIStateInterface;
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
        EventsAdapter.OnEventClickListener,
        SelectionModeHandler {

    private static final String TAG = "EventsList";

    // Views
    private RecyclerView mEventsRecyclerView;
    private View mEmptyStateView;
    private View mLoadingStateView;
    private MaterialButton mBtnEmptyImportEvents;
    private MaterialButton mBtnEmptyCreateEvent;

    // Data
    private EventsAdapter mEventsAdapter;
    private List<LocalEvent> mEventsList;

    // Interfaces
    private EventsFileOperationsInterface mFileOperationsInterface;
    private EventsDatabaseOperationsInterface mDataOperationsInterface;
    private EventsUIStateInterface mUIStateInterface;

    // Deletions
    private Set<String> mPendingDeletionIds = new HashSet<>();
    private boolean mIsRefreshSuppressed = false;

    // Selection mode state
    private boolean mIsInSelectionMode = false;
    private Set<String> mSelectedEventIds = new HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize data
        mEventsList = new ArrayList<>();
        mFileOperationsInterface = (EventsFileOperationsInterface) getActivity();
        mDataOperationsInterface = (EventsDatabaseOperationsInterface) getActivity();
        mUIStateInterface = (EventsUIStateInterface) getActivity();
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
        setupBackHandling();
        loadEvents();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Unregister back handler
        if (getActivity() instanceof EventsActivity) {
            ((EventsActivity) getActivity()).unregisterBackHandler(this);
            Log.d(TAG, "Unregistered back handler from EventsActivity");
        }

        // Exit selection mode if active
        if (mIsInSelectionMode) {
            exitSelectionMode();
        }
    }

    /**
     * Initialize view references
     */
    private void initializeViews(View view) {
        mEventsRecyclerView = view.findViewById(R.id.recycler_view_events);
        mEmptyStateView = view.findViewById(R.id.empty_state_events);
        mLoadingStateView = view.findViewById(R.id.loading_state_events);
        mBtnEmptyImportEvents = mEmptyStateView.findViewById(R.id.btn_empty_import_events);
        mBtnEmptyCreateEvent = mEmptyStateView.findViewById(R.id.btn_empty_create_event);

        setupEmptyStateButtons();
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
     * 🆕 Setup back handling for this fragment
     */
    private void setupBackHandling() {
        if (getActivity() instanceof EventsActivity activity) {
            activity.registerSelectionModeHandler(this, this);
            Log.d(TAG, "Back handling registered for EventsActivity");
        } else {
            Log.w(TAG, "Parent activity is not EventsActivity, cannot register back handler");
        }
    }

    /**
     * Setup empty state buttons click handlers
     */
    private void setupEmptyStateButtons() {
        if (mBtnEmptyImportEvents != null) {
            mBtnEmptyImportEvents.setOnClickListener(v -> {
                Log.d(TAG, "Empty state import button clicked");
                if (mFileOperationsInterface != null) {
                    mFileOperationsInterface.triggerImportEventsFromFile();
                }
            });
        }

        if (mBtnEmptyCreateEvent != null) {
            mBtnEmptyCreateEvent.setOnClickListener(v -> {
                Log.d(TAG, "Empty state create button clicked");
                // Trigger the same action as FAB - delegate to activity
                if (getActivity() instanceof EventsOperationsInterface) {
                    ((EventsOperationsInterface) getActivity()).triggerCreateNewEvent();
                }
            });
        }
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
            EventDao eventDao = QDueDatabase.getInstance(requireContext()).eventDao();

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

    // ==================== IMPLEMENTAZIONE SelectionModeHandler ====================

    @Override
    public boolean isInSelectionMode() {
        return mIsInSelectionMode;
    }

    @Override
    public boolean exitSelectionMode() {
        if (mIsInSelectionMode) {
            Log.d(TAG, "Exiting selection mode");

            mIsInSelectionMode = false;
            mSelectedEventIds.clear();

            // Update UI - notify adapter to remove selection indicators
            if (mEventsAdapter != null) {
                mEventsAdapter.setSelectionMode(false);
                mEventsAdapter.clearSelections();
                mEventsAdapter.notifyDataSetChanged();
            }

            // Update toolbar/menu if needed
            updateSelectionModeUI();

            Log.d(TAG, "✅ Selection mode exited successfully");
            return true;
        }

        Log.d(TAG, "Not in selection mode, nothing to exit");
        return false;
    }

    @Override
    public int getSelectedItemCount() {
        return mSelectedEventIds.size();
    }

    /**
     * 🆕 NEW: Enter selection mode (called when user long-presses an item)
     */
    public void enterSelectionMode() {
        if (!mIsInSelectionMode) {
            Log.d(TAG, "Entering selection mode");

            mIsInSelectionMode = true;

            // Update UI
            if (mEventsAdapter != null) {
                mEventsAdapter.setSelectionMode(true);
                mEventsAdapter.notifyDataSetChanged();
            }

            updateSelectionModeUI();

            Log.d(TAG, "✅ Selection mode entered successfully");
        }
    }

    /**
     * 🆕 NEW: Toggle selection for an event
     */
    public void toggleEventSelection(String eventId) {
        if (eventId == null) return;

        if (mSelectedEventIds.contains(eventId)) {
            mSelectedEventIds.remove(eventId);
            Log.d(TAG, "Deselected event: " + eventId);
        } else {
            mSelectedEventIds.add(eventId);
            Log.d(TAG, "Selected event: " + eventId);
        }

        // If no items selected, exit selection mode
        if (mSelectedEventIds.isEmpty() && mIsInSelectionMode) {
            exitSelectionMode();
        }

        // Update UI
        updateSelectionModeUI();

        // Notify adapter about selection change
        if (mEventsAdapter != null) {
            mEventsAdapter.updateSelections(mSelectedEventIds);
        }
    }

    /**
     * 🆕 NEW: Check if an event is selected
     */
    public boolean isEventSelected(String eventId) {
        return mSelectedEventIds.contains(eventId);
    }

    /**
     * 🆕 NEW: Update UI based on selection mode state
     */
    private void updateSelectionModeUI() {
        if (mIsInSelectionMode) {
            // Could update toolbar, show selection count, etc.
            Log.d(TAG, "Selection mode UI updated - " + mSelectedEventIds.size() + " items selected");

            // Example: Update activity title with selection count
            if (getActivity() != null) {
                String title = mSelectedEventIds.size() + " selected";
                getActivity().setTitle(title);
            }
        } else {
            // Restore normal UI
            Log.d(TAG, "Normal mode UI restored");

            // Restore original title
            if (getActivity() != null) {
                getActivity().setTitle(R.string.nav_eventi);
            }
        }
    }

    // ==================== EVENT CLICK HANDLING ====================

    /**
     * Handle event item click - Navigate to detail fragment or handle selection
     */
    //@Override
    public void onEventClick(LocalEvent event) {
        Log.d(TAG, "onEventClick called for: " + (event != null ? event.getTitle() : "null"));

        if (event == null || event.getId() == null) {
            Toast.makeText(getContext(), "Errore: evento non valido", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ MODIFICA: Handle selection mode
        if (mIsInSelectionMode) {
            // In selection mode, toggle selection instead of navigating
            toggleEventSelection(event.getId());
            return;
        }

        // Normal mode: navigate to detail
        debugLogCurrentEvents();

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
     * Handle event long click - Enter selection mode
     */
    @Override
    public void onEventLongClick(LocalEvent event) {
        if (event == null || event.getId() == null) return;

        Log.d(TAG, "onEventLongClick called for: " + event.getTitle());

        // ✅ MODIFICA: Enter selection mode and select this item
        if (!mIsInSelectionMode) {
            enterSelectionMode();
        }

        // Select the long-clicked item
        toggleEventSelection(event.getId());

        // Only show context menu for a single event
        // TODO: implements multile selection
        showEventContextMenu(event);
    }

    // ==================== ACTION METHODS ====================

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
     * with better state management
     */
    public void refreshEvents() {
        final String mTAG = "refreshEvents: ";
        Log.d(TAG, mTAG + "Called - forcing refresh");

        // Clear suppression flag to allow refresh
        mIsRefreshSuppressed = false;

        // Clear pending deletions since we're doing a full refresh
        clearPendingDeletions();

        // Load events (this will automatically update view state when complete)
        loadEvents();

        Log.d(TAG, mTAG + "✅ Refresh initiated");
    }

    /**
     * Update view state based on data availability
     * with better logging and error handling
     */
    private void updateViewState() {
        final String mTAG = "updateViewState: ";

        try {
            boolean hasEvents = !mEventsList.isEmpty();

            Log.d(TAG, mTAG + "Updating view state - hasEvents: " + hasEvents +
                    " (total: " + mEventsList.size() + ")");

            if (hasEvents) {
                // Show events list
                mEventsRecyclerView.setVisibility(View.VISIBLE);
                mEmptyStateView.setVisibility(View.GONE);
                Log.d(TAG, mTAG + "📋 Showing events list (" + mEventsList.size() + " events)");
            } else {
                // Show empty state
                mEventsRecyclerView.setVisibility(View.GONE);
                mEmptyStateView.setVisibility(View.VISIBLE);
                Log.d(TAG, mTAG + "📭 Showing empty state");
            }

            // Notify activity about events state change for FAB management
            if (mUIStateInterface != null) {
                mUIStateInterface.onEventsListStateChanged(hasEvents);
                Log.d(TAG, mTAG + "✅ Notified activity of state change: hasEvents=" + hasEvents);
            } else {
                Log.w(TAG, mTAG + "UIStateInterface is null, cannot notify activity");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error updating view state: " + e.getMessage());
        }
    }

    /**
     * Show/hide loading state
     * with better state management
     */
    private void showLoading(boolean show) {
        final String mTAG = "showLoading: ";
        Log.v(TAG, mTAG + "Called with show=" + show);

        try {
            if (show) {
                // Hide both other states and show loading
                mEventsRecyclerView.setVisibility(View.GONE);
                mEmptyStateView.setVisibility(View.GONE);
                mLoadingStateView.setVisibility(View.VISIBLE);
                Log.d(TAG, mTAG + "⏳ Showing loading state");
            } else {
                // Hide loading and determine correct state to show
                mLoadingStateView.setVisibility(View.GONE);
                updateViewState(); // This will show either events list or empty state
                Log.d(TAG, mTAG + "✅ Loading hidden, updated to appropriate state");
            }
        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error managing loading state: " + e.getMessage());
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
     * with proper state handling
     */
    public void addEvent(LocalEvent event) {
        final String mTAG = "addEvent: ";
        Log.d(TAG, mTAG + "Called for: " + (event != null ? event.getTitle() : "null"));

        if (event == null) {
            Log.w(TAG, mTAG + "Cannot add null event to list");
            return;
        }

        // Ensure we're on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // Check if event already exists to avoid duplicates
                    boolean exists = false;
                    for (LocalEvent existingEvent : mEventsList) {
                        if (existingEvent != null && event.getId() != null &&
                                event.getId().equals(existingEvent.getId())) {
                            Log.w(TAG, mTAG + "Event already exists in list: " + event.getTitle());
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        boolean wasEmpty = mEventsList.isEmpty();

                        mEventsList.add(0, event); // Add at top
                        mEventsAdapter.notifyItemInserted(0);
                        mEventsRecyclerView.scrollToPosition(0);

                        // CRITICAL: Update view state (this will hide empty state if it was showing)
                        updateViewState();

                        Log.d(TAG, mTAG + "✅ Successfully added event: " + event.getTitle());
                        Log.d(TAG, mTAG + "Total events now: " + mEventsList.size());

                        if (wasEmpty) {
                            Log.d(TAG, mTAG + "📝 List was empty, now has events - empty state should be hidden");
                        }

                    } else {
                        Log.w(TAG, mTAG + "Event already exists, not adding duplicate");
                    }

                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error adding event: " + e.getMessage());
                }
            });
        } else {
            Log.w(TAG, mTAG + "Activity is null, cannot update UI");
        }
    }

    /**
     * Remove event from list (called after deletion)
     * with proper empty state handling
     */
    public void removeEvent(String eventId) {
        final String mTAG = "removeEvent: ";
        Log.d(TAG, mTAG + "Called for eventId: " + eventId);

        if (eventId == null || eventId.trim().isEmpty()) {
            Log.w(TAG, mTAG + "Cannot remove event - eventId is null or empty");
            return;
        }

        // Ensure we're on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    boolean found = false;
                    int removedPosition = -1;
                    String removedTitle = "";

                    for (int i = 0; i < mEventsList.size(); i++) {
                        LocalEvent event = mEventsList.get(i);
                        if (event != null && eventId.equals(event.getId())) {
                            removedTitle = event.getTitle();
                            removedPosition = i;
                            mEventsList.remove(i);
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        // Notify adapter of removal
                        mEventsAdapter.notifyItemRemoved(removedPosition);

                        // Notify range changed for items after the removed one
                        if (removedPosition < mEventsList.size()) {
                            mEventsAdapter.notifyItemRangeChanged(removedPosition,
                                    mEventsList.size() - removedPosition);
                        }

                        // CRITICAL: Update view state (this will show empty state if list is now empty)
                        updateViewState();

                        Log.d(TAG, mTAG + "✅ Successfully removed event: " + removedTitle +
                                " (ID: " + eventId + ") at position: " + removedPosition);
                        Log.d(TAG, mTAG + "Remaining events: " + mEventsList.size());

                        // Log if we're now showing empty state
                        if (mEventsList.isEmpty()) {
                            Log.d(TAG, mTAG + "📭 List is now empty - empty state should be visible");
                        }

                    } else {
                        Log.w(TAG, mTAG + "Event not found in list for removal: " + eventId);
                        // Force a complete refresh as fallback
                        Log.d(TAG, mTAG + "Forcing complete refresh as fallback");
                        refreshEvents();
                    }

                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error removing event: " + e.getMessage());
                    // Fallback to refresh
                    refreshEvents();
                }
            });
        } else {
            Log.w(TAG, mTAG + "Activity is null, cannot update UI");
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
        final String mTAG = "onEventsCleared: ";
        Log.d(TAG, mTAG + "Called with clearedCount=" + clearedCount + ", createBackup=" + createBackup);

        // Ensure we're on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // Clear the list
                    int previousSize = mEventsList.size();
                    mEventsList.clear();

                    // Clear pending deletions
                    clearPendingDeletions();

                    // Notify adapter of data change
                    mEventsAdapter.notifyDataSetChanged();

                    // CRITICAL: Update view state to show empty state
                    updateViewState();

                    Log.d(TAG, mTAG + "✅ Successfully cleared " + previousSize +
                            " events and updated to empty state");

                    // Trigger backup if requested
                    if (createBackup) {
                        BackupIntegration.integrateWithClearAll(getContext(), mEventsList);
                        Log.d(TAG, mTAG + "Backup integration triggered");
                    }

                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error clearing events: " + e.getMessage());
                }
            });
        } else {
            Log.w(TAG, mTAG + "Activity is null, cannot update UI");
        }
    }

    /**
     * ENHANCED: Handle event creation with proper state management
     */
    public void onEventCreated(LocalEvent event) {
        final String mTAG = "onEventCreated: ";
        Log.d(TAG, mTAG + "Called for: " + (event != null ? event.getTitle() : "null"));

        if (event != null) {
            // Add event (this will automatically update view state)
            addEvent(event);

            Log.d(TAG, mTAG + "✅ Event creation handled successfully");
        } else {
            Log.w(TAG, mTAG + "Cannot handle creation of null event");
        }
    }

    /**
     * Handle refresh required - deprecated method, should use specific methods
     */
    @Deprecated
    public void onRefreshRequired(String reason) {
        final String mTAG = "onRefreshRequired: ";
        Log.d(TAG, mTAG + "Called with reason: " + reason + " - delegating to refreshEvents()");

        // Instead of brutal notify, use proper refresh
        refreshEvents();
    }

    /**
     * Helper method for visibility debugging
     */
    private String getVisibilityString(View view) {
        if (view == null) return "null";
        switch (view.getVisibility()) {
            case View.VISIBLE:
                return "VISIBLE";
            case View.GONE:
                return "GONE";
            case View.INVISIBLE:
                return "INVISIBLE";
            default:
                return "UNKNOWN";
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * DEBUG: Check fragment state
     */
    public void debugFragmentState() {
        Log.d(TAG, "=== EVENTS LIST FRAGMENT STATE DEBUG ===");
        Log.d(TAG, "Events List Size: " + mEventsList.size());
        Log.d(TAG, "Pending Deletions: " + mPendingDeletionIds.size());
        Log.d(TAG, "Refresh Suppressed: " + mIsRefreshSuppressed);

        // ✅ AGGIUNGI: Selection mode debug info
        Log.d(TAG, "Selection Mode Active: " + mIsInSelectionMode);
        Log.d(TAG, "Selected Items Count: " + mSelectedEventIds.size());

        if (!mSelectedEventIds.isEmpty()) {
            Log.d(TAG, "Selected Event IDs: " + mSelectedEventIds);
        }

        // View visibility states (existing...)
        Log.d(TAG, "RecyclerView Visibility: " + getVisibilityString(mEventsRecyclerView));
        Log.d(TAG, "Empty State Visibility: " + getVisibilityString(mEmptyStateView));
        Log.d(TAG, "Loading State Visibility: " + getVisibilityString(mLoadingStateView));

        // Interface states (existing...)
        Log.d(TAG, "File Operations Interface: " + (mFileOperationsInterface != null ? "available" : "null"));
        Log.d(TAG, "Data Operations Interface: " + (mDataOperationsInterface != null ? "available" : "null"));
        Log.d(TAG, "UI State Interface: " + (mUIStateInterface != null ? "available" : "null"));

        Log.d(TAG, "=== END FRAGMENT STATE DEBUG ===");
    }

    /**
     * 🆕 DEBUG: Test selection mode functionality
     */
    public void debugTestSelectionMode() {
        Log.d(TAG, "=== TESTING SELECTION MODE ===");

        if (!mEventsList.isEmpty()) {
            LocalEvent firstEvent = mEventsList.get(0);
            if (firstEvent != null) {
                Log.d(TAG, "Testing with first event: " + firstEvent.getTitle());

                // Simulate long click to enter selection mode
                onEventLongClick(firstEvent);

                Log.d(TAG, "Selection mode should now be active: " + mIsInSelectionMode);
                Log.d(TAG, "Selected items: " + mSelectedEventIds.size());
            }
        } else {
            Log.d(TAG, "No events available for selection mode test");
        }

        Log.d(TAG, "=== END SELECTION MODE TEST ===");
    }

    /**
     * DEBUG: Log Current Events List
     */
    public void debugLogCurrentEvents() {
        Log.d(TAG, "=== CURRENT EVENTS LIST DEBUG ===");
        Log.d(TAG, "Total events: " + mEventsList.size());
        Log.d(TAG, "Should show empty state: " + mEventsList.isEmpty());

        for (int i = 0; i < Math.min(mEventsList.size(), 5); i++) { // Log max 5 events
            LocalEvent event = mEventsList.get(i);
            if (event != null) {
                Log.d(TAG, i + ": " + event.getTitle() + " (ID: " + event.getId() + ")");
            } else {
                Log.d(TAG, i + ": NULL EVENT");
            }
        }

        if (mEventsList.size() > 5) {
            Log.d(TAG, "... and " + (mEventsList.size() - 5) + " more events");
        }

        Log.d(TAG, "=== END EVENTS LIST DEBUG ===");
    }
}