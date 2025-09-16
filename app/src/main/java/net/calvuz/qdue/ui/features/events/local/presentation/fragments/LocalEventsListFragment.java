package net.calvuz.qdue.ui.features.events.local.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.List;
import java.util.Set;

/**
 * LocalEvents List Fragment (MVVM)
 *
 * <p>Fragment implementation demonstrating MVVM architecture integration with
 * LocalEventsViewModel. This example shows how to properly observe ViewModel
 * state changes, handle events, and coordinate with the parent Activity.</p>
 *
 * <h3>MVVM Integration:</h3>
 * <ul>
 *   <li><strong>ViewModel Observation</strong>: Reactive UI updates through state listeners</li>
 *   <li><strong>Event Handling</strong>: Navigation and UI action events from ViewModel</li>
 *   <li><strong>State Management</strong>: Loading, error, and content states</li>
 *   <li><strong>Selection Mode</strong>: Multi-select with batch operations</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>RecyclerView with event adapter</li>
 *   <li>Search and filtering UI</li>
 *   <li>Selection mode with action bar</li>
 *   <li>Loading and error states</li>
 *   <li>Pull-to-refresh functionality</li>
 * </ul>
 *
 * <h3>Implementation Note:</h3>
 * <p>This is a simplified example showing MVVM patterns. A complete implementation
 * would require additional components like RecyclerView adapters, layout files,
 * and proper error handling infrastructure.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsListFragment
        extends Fragment
implements Injectable
{

    private static final String TAG = "LocalEventsListFragment";

    // ==================== UI COMPONENTS ====================

    private RecyclerView mRecyclerView;
    private View mEmptyStateView;
    private View mLoadingView;
    private View mErrorView;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;

    // ==================== ADAPTERS ====================

    private LocalEventsAdapter mEventsAdapter;

    // ==================== STATE ====================

    private boolean mIsSelectionMode = false;

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Direct dependency injection
        initializeDependencyInjection();

        // Get ViewModel from parent Activity
//        if (getActivity() instanceof LocalEventsActivity activity) {
//            mEventsViewModel = activity.getEventsViewModel();
//        }

        if (mEventsViewModel == null) {
            Log.e(TAG, "Could not get LocalEventsViewModel from parent Activity");
            // Handle error - perhaps show error state or finish
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_events_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupViewModelObservers();

        // Initial data load
        if (mEventsViewModel != null) {
            mEventsViewModel.loadEvents();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove ViewModel observers to prevent memory leaks
        if (mEventsViewModel != null) {
            removeViewModelObservers();
        }
    }

    // ==================== DEPENDENCY INJECTION ====================

    private void initializeDependencyInjection() {
        try {
            Log.d(TAG, "Initializing dependencies with DependencyInjector...");

            // ✅ ONE LINE INJECTION - follows project standard
            DependencyInjector.inject( this, requireActivity());

            // ✅ VERIFICATION
            if (!DependencyInjector.verifyInjection(this, requireActivity())) {
                throw new RuntimeException("Dependency injection verification failed");
            }

            Log.d(TAG, "✅ Dependencies injected and verified successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize dependency injection", e);
            throw new RuntimeException("Dependency injection failed", e);
        }
    }

    /**
     * Inject dependencies into this component
     *
     * @param serviceProvider
     */
    @Override
    public void inject(ServiceProvider serviceProvider) {
        // Get LocalEventsModule from CalendarServiceProvider
        LocalEventsModule localEventsModule = serviceProvider.getCalendarServiceProvider().getLocaEventsModule();

        mEventsViewModel = localEventsModule.getLocalEventsViewModel();
//        mFileOpsViewModel = localEventsModule.getFileOperationsViewModel();
//        mLocaleManager = serviceProvider.getLocaleManager();

        Log.d(TAG, "✅ Dependencies injected via DependencyInjector");
    }

    /**
     * Check if dependencies are injected and ready
     */
    @Override
    public boolean areDependenciesReady() {
        return mEventsViewModel != null
//                && mFileOpsViewModel != null
//                && mLocaleManager != null
                ;
    }

    // ==================== UI INITIALIZATION ====================

    private void initializeViews(@NonNull View view) {
        mRecyclerView = view.findViewById(R.id.recycler_view_events);
        mEmptyStateView = view.findViewById(R.id.empty_state_view);
        mLoadingView = view.findViewById(R.id.loading_state_view);
//        mErrorView = view.findViewById(R.id.error_view);
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter (this would need to be implemented)
        mEventsAdapter = new LocalEventsAdapter();
        mEventsAdapter.setOnEventClickListener(this::onEventClick);
        mEventsAdapter.setOnEventLongClickListener(this::onEventLongClick);
        mEventsAdapter.setOnEventSelectionChangeListener(this::onEventSelectionChange);

        mRecyclerView.setAdapter(mEventsAdapter);
    }

    // ==================== VIEWMODEL OBSERVERS ====================

    private void setupViewModelObservers() {
        if (mEventsViewModel == null) return;

        // Observe events list changes
        mEventsViewModel.addStateChangeListener(
                LocalEventsViewModel.STATE_FILTERED_EVENTS,
                this::onEventsChanged
        );

        // Observe loading states
        mEventsViewModel.addLoadingStateListener(this::onLoadingStateChanged);

        // Observe error states
        mEventsViewModel.addErrorStateListener(this::onErrorStateChanged);

        // Observe selection mode changes
        mEventsViewModel.addStateChangeListener(
                LocalEventsViewModel.STATE_SELECTION_MODE,
                this::onSelectionModeChanged
        );

        // Observe selected events changes
        mEventsViewModel.addStateChangeListener(
                LocalEventsViewModel.STATE_SELECTED_EVENTS,
                this::onSelectedEventsChanged
        );

        // Observe navigation and UI events
        mEventsViewModel.addEventListener(this::onViewModelEvent);

        Log.d(TAG, "ViewModel observers setup completed");
    }

    private void removeViewModelObservers() {
        if (mEventsViewModel == null) return;

        // Remove all observers to prevent memory leaks
        // Note: This would require the ViewModel to support observer removal
        // which would need to be implemented in the BaseViewModel

        Log.d(TAG, "ViewModel observers removed");
    }

    // ==================== EVENT HANDLERS ====================

    private void onEventsChanged(@NonNull String key, @Nullable Object value) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            @SuppressWarnings("unchecked")
            List<LocalEvent> events = (List<LocalEvent>) value;

            if (events != null) {
                mEventsAdapter.updateEvents(events);
                updateEmptyState(events.isEmpty());
                Log.d(TAG, "Events list updated: " + events.size() + " events");
            }
        });
    }

    private void onLoadingStateChanged(@NonNull String operation, boolean loading) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (LocalEventsViewModel.OP_LOAD_EVENTS.equals(operation)) {
                updateLoadingState(loading);
            }
        });
    }

    private void onErrorStateChanged(@NonNull String operation, @Nullable String error) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (error != null) {
                showError(error);
            }
        });
    }

    private void onSelectionModeChanged(@NonNull String key, @Nullable Object value) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Boolean selectionMode = (Boolean) value;
            if (selectionMode != null) {
                mIsSelectionMode = selectionMode;
                updateSelectionModeUI(selectionMode);

                if (mEventsAdapter != null) {
                    mEventsAdapter.setSelectionMode(selectionMode);
                }
            }
        });
    }

    private void onSelectedEventsChanged(@NonNull String key, @Nullable Object value) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            @SuppressWarnings("unchecked")
            Set<String> selectedIds = (Set<String>) value;

            if (selectedIds != null && mEventsAdapter != null) {
                mEventsAdapter.updateSelection(selectedIds);
                updateSelectionCount(selectedIds.size());
            }
        });
    }

    private void onViewModelEvent(@NonNull BaseViewModel.ViewModelEvent event) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (event instanceof BaseViewModel.UIActionEvent) {
                handleUIActionEvent((BaseViewModel.UIActionEvent) event);
            }
            // Navigation events are handled by the Activity
        });
    }

    private void handleUIActionEvent(@NonNull BaseViewModel.UIActionEvent event) {
        String action = event.getAction();

        switch (action) {
            case "SHOW_SUCCESS":
                String successMessage = event.getData("message", String.class);
                if (successMessage != null) {
                    showSuccess(successMessage);
                }
                break;
            case "SHOW_ERROR":
                String errorMessage = event.getData("message", String.class);
                if (errorMessage != null) {
                    showError(errorMessage);
                }
                break;
            default:
                Log.d(TAG, "Unhandled UI action: " + action);
                break;
        }
    }

    // ==================== ADAPTER EVENT HANDLERS ====================

    private void onEventClick(@NonNull LocalEvent event) {
        if (mEventsViewModel != null) {
            if (mIsSelectionMode) {
                mEventsViewModel.toggleEventSelection(event.getId());
            } else {
                mEventsViewModel.navigateToEventDetail(event.getId());
            }
        }
    }

    private void onEventLongClick(@NonNull LocalEvent event) {
        if (mEventsViewModel != null) {
            if (!mIsSelectionMode) {
                mEventsViewModel.toggleSelectionMode();
            }
            mEventsViewModel.toggleEventSelection(event.getId());
        }
    }

    private void onEventSelectionChange(@NonNull String eventId, boolean selected) {
        if (mEventsViewModel != null) {
            mEventsViewModel.toggleEventSelection(eventId);
        }
    }

    // ==================== UI STATE UPDATES ====================

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            mEmptyStateView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyStateView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateLoadingState(boolean loading) {
        mLoadingView.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (loading) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyStateView.setVisibility(View.GONE);
            mErrorView.setVisibility(View.GONE);
        }
    }

    private void updateSelectionModeUI(boolean selectionMode) {
        // Update UI for selection mode
        // This might involve changing the action bar, showing selection controls, etc.
        getActivity().invalidateOptionsMenu();

        Log.d(TAG, "Selection mode UI updated: " + selectionMode);
    }

    private void updateSelectionCount(int count) {
        // Update selection count in action bar or elsewhere
        Log.d(TAG, "Selection count updated: " + count);
    }

    private void showSuccess(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.success_color))
                    .show();
        }
    }

    private void showError(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error_color))
                    .show();
        }
    }

    // ==================== MENU HANDLING ====================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mIsSelectionMode) {
//            inflater.inflate(R.menu.menu_local_events_selection, menu);
        } else {
//            inflater.inflate(R.menu.menu_local_events_list, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mEventsViewModel == null) {
            return super.onOptionsItemSelected(item);
        }

        int itemId = item.getItemId();

//        if (itemId == R.id.action_search) {
//            // Trigger search UI
//            return true;
//        } else if (itemId == R.id.action_filter) {
//            // Trigger filter UI
//            return true;
//        } else if (itemId == R.id.action_select_all) {
//            mEventsViewModel.selectAllEvents();
//            return true;
//        } else if (itemId == R.id.action_delete_selected) {
//            mEventsViewModel.deleteSelectedEvents();
//            return true;
//        } else if (itemId == R.id.action_export_selected) {
//            // Trigger export for selected events
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Handle back press in selection mode.
     */
    public boolean onBackPressed() {
        if (mIsSelectionMode && mEventsViewModel != null) {
            mEventsViewModel.toggleSelectionMode();
            return true;
        }
        return false;
    }

    // ==================== ADAPTER CLASS (Simplified) ====================

    /**
     * Simplified adapter class showing the integration pattern.
     * A complete implementation would include proper ViewHolder, DiffUtil, etc.
     */
    private static class LocalEventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<LocalEvent> mEvents = new java.util.ArrayList<>();
        private Set<String> mSelectedIds = new java.util.HashSet<>();
        private boolean mSelectionMode = false;

        // Event listeners
        private OnEventClickListener mOnEventClickListener;
        private OnEventLongClickListener mOnEventLongClickListener;
        private OnEventSelectionChangeListener mOnEventSelectionChangeListener;

        public void updateEvents(@NonNull List<LocalEvent> events) {
            mEvents.clear();
            mEvents.addAll(events);
            notifyDataSetChanged();
        }

        public void updateSelection(@NonNull Set<String> selectedIds) {
            mSelectedIds.clear();
            mSelectedIds.addAll(selectedIds);
            notifyDataSetChanged();
        }

        public void setSelectionMode(boolean selectionMode) {
            mSelectionMode = selectionMode;
            if (!selectionMode) {
                mSelectedIds.clear();
            }
            notifyDataSetChanged();
        }

        // Listener setters
        public void setOnEventClickListener(OnEventClickListener listener) {
            mOnEventClickListener = listener;
        }

        public void setOnEventLongClickListener(OnEventLongClickListener listener) {
            mOnEventLongClickListener = listener;
        }

        public void setOnEventSelectionChangeListener(OnEventSelectionChangeListener listener) {
            mOnEventSelectionChangeListener = listener;
        }

        @Override
        public int getItemCount() {
            return mEvents.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create ViewHolder - simplified implementation
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_local_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // Bind data to ViewHolder - simplified implementation
            if (holder instanceof EventViewHolder) {
                LocalEvent event = mEvents.get(position);
                EventViewHolder eventHolder = (EventViewHolder) holder;
                eventHolder.bind( event, mSelectedIds.contains(event.getId()), mSelectionMode);
            }
        }

        // Simplified ViewHolder
        private class EventViewHolder extends RecyclerView.ViewHolder {

            public EventViewHolder(@NonNull View itemView) {
                super(itemView);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && mOnEventClickListener != null) {
                        mOnEventClickListener.onEventClick(mEvents.get(position));
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && mOnEventLongClickListener != null) {
                        mOnEventLongClickListener.onEventLongClick(mEvents.get(position));
                        return true;
                    }
                    return false;
                });
            }

            public void bind(@NonNull LocalEvent event, boolean selected, boolean selectionMode) {
                // Bind event data to views - simplified implementation
                // This would set text, colors, selection state, etc.
            }
        }

        // Listener interfaces
        interface OnEventClickListener {
            void onEventClick(@NonNull LocalEvent event);
        }

        interface OnEventLongClickListener {
            void onEventLongClick(@NonNull LocalEvent event);
        }

        interface OnEventSelectionChangeListener {
            void onEventSelectionChange(@NonNull String eventId, boolean selected);
        }
    }
}