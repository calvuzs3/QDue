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
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * EventsListFragment - Display list of events with navigation to details
 *
 * Features:
 * - RecyclerView with EventsAdapter
 * - Click handling for navigation to details
 * - Menu integration for import/export
 * - Empty state management
 * - Search functionality
 *
 * Navigation:
 * - Click event → EventDetailFragment with eventId argument
 */
public class EventsListFragment extends Fragment implements EventsAdapter.OnEventClickListener {

    private static final String TAG = "EventsListFragment";

    // Views
    private RecyclerView mEventsRecyclerView;
    private View mEmptyStateView;
    private View mLoadingStateView;

    // Data
    private EventsAdapter mEventsAdapter;
    private List<LocalEvent> mEventsList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize data
        mEventsList = new ArrayList<>();
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
                        mEventsList.clear();
                        if (events != null && !events.isEmpty()) {
                            mEventsList.addAll(events);
                            Log.d(TAG, "Loaded " + events.size() + " events from database");
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

                        // Fallback to sample events for testing
                        loadSampleEvents();
                        updateViewState();
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error getting database instance: " + e.getMessage());
            showLoading(false);

            // Fallback to sample events
            loadSampleEvents();
            updateViewState();
        }
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

        if (itemId == R.id.action_import_events) {
            handleImportEvents();
            return true;
        } else if (itemId == R.id.action_export_events) {
            handleExportEvents();
            return true;
        } else if (itemId == R.id.action_refresh_events) {
            handleRefreshEvents();
            return true;
        } else if (itemId == R.id.action_search_events) {
            handleSearchEvents();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== EVENT CLICK HANDLING ====================

    /**
     * Handle event item click - Navigate to detail fragment
     */
    @Override
    public void onEventClick(LocalEvent event) {
        if (event == null || event.getId() == null) {
            Toast.makeText(getContext(), "Errore: evento non valido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to EventDetailFragment using Navigation Component
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());

        try {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_events_list_to_event_detail, args);
        } catch (Exception e) {
            android.util.Log.e("EventsListFragment", "Navigation error: " + e.getMessage());
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

    private void handleImportEvents() {
        // TODO: Integrate with existing import functionality from EventsActivity
        Toast.makeText(getContext(), "Import eventi - TODO", Toast.LENGTH_SHORT).show();
    }

    private void handleExportEvents() {
        // TODO: Integrate with existing export functionality from EventsActivity
        Toast.makeText(getContext(), "Export eventi - TODO", Toast.LENGTH_SHORT).show();
    }

    private void handleRefreshEvents() {
        loadEvents();
        Toast.makeText(getContext(), "Eventi aggiornati", Toast.LENGTH_SHORT).show();
    }

    private void handleSearchEvents() {
        // TODO: Implement search functionality
        Toast.makeText(getContext(), "Ricerca eventi - TODO", Toast.LENGTH_SHORT).show();
    }

    private void showEventContextMenu(LocalEvent event) {
        // TODO: Show context menu with options
        Toast.makeText(getContext(), "Menu contestuale per: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // ==================== SAMPLE DATA FOR TESTING ====================

    /**
     * Load sample events for testing purposes
     * TODO: Remove when real database integration is implemented
     */
    private void loadSampleEvents() {
        mEventsList.clear();

        // Sample Event 1
        LocalEvent event1 = new LocalEvent();
        event1.setId("sample_001");
        event1.setTitle("Fermata Programmata Linea A");
        event1.setDescription("Manutenzione ordinaria programmata");
        event1.setStartTime(java.time.LocalDateTime.now().plusDays(2));
        event1.setEndTime(java.time.LocalDateTime.now().plusDays(2).plusHours(8));
        event1.setLocation("Stabilimento Nord - Linea A");
        mEventsList.add(event1);

        // Sample Event 2
        LocalEvent event2 = new LocalEvent();
        event2.setId("sample_002");
        event2.setTitle("Riunione Team Produzione");
        event2.setDescription("Pianificazione attività settimanali");
        event2.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        event2.setEndTime(java.time.LocalDateTime.now().plusDays(1).plusHours(2));
        event2.setLocation("Sala Riunioni B");
        mEventsList.add(event2);

        // Sample Event 3
        LocalEvent event3 = new LocalEvent();
        event3.setId("sample_003");
        event3.setTitle("Controllo Qualità Mensile");
        event3.setDescription("Verifica standard qualità prodotti");
        event3.setStartTime(java.time.LocalDateTime.now().plusDays(7));
        event3.setEndTime(java.time.LocalDateTime.now().plusDays(7).plusHours(4));
        event3.setLocation("Laboratorio Qualità");
        mEventsList.add(event3);

        // Sample Event 4 - Past event
        LocalEvent event4 = new LocalEvent();
        event4.setId("sample_004");
        event4.setTitle("Formazione Sicurezza");
        event4.setDescription("Corso aggiornamento sicurezza sul lavoro");
        event4.setStartTime(java.time.LocalDateTime.now().minusDays(3));
        event4.setEndTime(java.time.LocalDateTime.now().minusDays(3).plusHours(6));
        event4.setLocation("Aula Formazione");
        mEventsList.add(event4);

        // Sample Event 5 - All day event
        LocalEvent event5 = new LocalEvent();
        event5.setId("sample_005");
        event5.setTitle("Inventario Generale");
        event5.setDescription("Inventario annuale di tutti i materiali");
        event5.setStartTime(java.time.LocalDateTime.now().plusDays(14).withHour(0).withMinute(0));
        event5.setEndTime(java.time.LocalDateTime.now().plusDays(14).withHour(23).withMinute(59));
        event5.setLocation("Tutti i reparti");
        event5.setAllDay(true);
        mEventsList.add(event5);

        mEventsAdapter.notifyDataSetChanged();
    }

    // ==================== PUBLIC INTERFACE ====================

    /**
     * Refresh events list (called from parent activity)
     */
    public void refreshEvents() {
        loadEvents();
    }

    /**
     * Add new event to list (called after creation)
     */
    public void addEvent(LocalEvent event) {
        if (event != null) {
            mEventsList.add(0, event); // Add at top
            mEventsAdapter.notifyItemInserted(0);
            mEventsRecyclerView.scrollToPosition(0);
            updateViewState();
        }
    }

    /**
     * Remove event from list (called after deletion)
     */
    public void removeEvent(String eventId) {
        for (int i = 0; i < mEventsList.size(); i++) {
            if (mEventsList.get(i).getId().equals(eventId)) {
                mEventsList.remove(i);
                mEventsAdapter.notifyItemRemoved(i);
                updateViewState();
                break;
            }
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
}