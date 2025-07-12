package net.calvuz.qdue.ui.core.architecture.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.QDueMainActivity;
import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.ServiceProviderImpl;
import net.calvuz.qdue.core.services.EventsService;
import net.calvuz.qdue.core.services.UserService;
import net.calvuz.qdue.core.services.models.EventPreview;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.core.services.models.QuickEventRequest;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.features.events.presentation.EventsActivity;
import net.calvuz.qdue.ui.core.components.toolbars.BottomSelectionToolbar;
import net.calvuz.qdue.ui.core.components.widgets.EventsPreviewManager;
import net.calvuz.qdue.ui.core.common.enums.ToolbarAction;
import net.calvuz.qdue.ui.core.common.interfaces.DayLongClickListener;
import net.calvuz.qdue.ui.core.common.interfaces.EventsPreviewInterface;
import net.calvuz.qdue.ui.core.common.models.SharedViewModels;
import net.calvuz.qdue.ui.features.events.quickevents.QuickEventConfirmationDialog;
import net.calvuz.qdue.ui.features.events.quickevents.QuickEventTemplate;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Base fragment with long-click and selection support
 * Enhanced with Quick Events creation system
 * Intermediate layer between BaseFragment and specific implementations
 * <p>
 * PHASE 3: BaseInteractiveFragment - Service Integration
 * <p>
 * REFACTORED VERSION:
 * - ✅ ADDED: Dependency injection support (Injectable)
 * - ✅ ADDED: Service-based quick event creation
 * - ✅ ADDED: Proper error handling with OperationResult
 * - ✅ ADDED: Async operations with CompletableFuture
 * - ❌ REMOVED: Direct business logic
 * - ❌ REMOVED: Manual validation in UI layer
 * - ✅ KEPT: All UI functionality and user experience
 * - ✅ ENHANCED: Better error handling and user feedback
 */
public abstract class BaseInteractiveFragment extends BaseFragment implements
        Injectable,
        DayLongClickListener {
//    public abstract class BaseInteractiveFragment extends BaseFragment implements DayLongClickListener {

    private static final String TAG = "BaseInteractiveFragment";

    // ==================== DEPENDENCIES (DI) ====================

    // ✅ Service dependencies (injected)
    private EventsService mEventsService;
    private UserService mUserService;

    // old untouched
    // Bottom Selection Toolbar
    protected BottomSelectionToolbar mBottomToolbar;

    // Selection mode support
    protected boolean mHasSelectionHiddenFab = false;
    protected MenuItem mSelectAllMenuItem;
    protected MenuItem mClearSelectionMenuItem;
    protected MenuItem mExitSelectionMenuItem;
    //

    // ==================== NEW: Quick Events Integration ====================

    // ✅ Service-based Quick Events (no more manual business logic)
    private Map<ToolbarAction, QuickEventTemplate> mQuickEventTemplates;
    private boolean mQuickEventsInitialized = false;

    // User context for event creation
    private Long mCurrentUserId = null; // Will be set by subclass or user session

    // ==================== EXISTING UI COMPONENTS ====================

    // ✅ Existing UI components (unchanged)
    protected CoordinatorLayout mCoordinatorLayout;

    // Events Preview Integration
    protected EventsPreviewManager mEventsPreviewManager;
    protected boolean mEventsPreviewEnabled = true;

    // ==================== ABSTRACT METHODS (UNCHANGED) ====================

    protected abstract BaseInteractiveAdapter getClickAdapter();

    protected abstract String getFragmentName();

    protected abstract EventsPreviewManager.ViewType getEventsPreviewViewType();

    protected ViewGroup getToolbarContainer() {
        return (ViewGroup) mCoordinatorLayout;
    }

    // ==================== LIFECYCLE (ENHANCED WITH DI) ====================

    /**
     * Find and setup views.
     *
     * @param rootView
     */
    @Override
    protected void findViews(View rootView) {

    }

    /**
     * Convert data in fragment specific format.
     *
     * @param days
     * @param monthDate
     */
    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return Collections.emptyList();
    }

    /**
     * Fragment specific adapter setup.
     * Subclass create a specific Adapter instance
     * which is given to the RecyclerView with mRecyclerView,setAdapter( adapter )
     */
    @Override
    protected void setupAdapter() {

    }

    /**
     * Get the current adapter.
     */
    @Override
    protected BaseAdapter getFragmentAdapter() {
        return null;
    }

    /**
     * Set adapter, an extension of BaseAdapter
     *
     * @param adapter
     */
    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {

    }

    /**
     * Abstract method for subclasses to specify column count.
     * DayslistViewFragment returns 1, CalendarViewFragment returns 7.
     */
    @Override
    protected int getGridColumnCount() {
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Initialize dependency injection
        initializeDependencyInjection();
    }

    /**
     * Enhanced onPause with events preview cleanup
     */
    @Override
    public void onPause() {
        super.onPause();

        // Force exit selection on pause per UX
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
        }

        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cleanup delegato all'adapter
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            adapter.onDestroy();
        }

        // Events Preview Click
        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.onDestroy();
            mEventsPreviewManager = null;
        }

        // ✅ NEW: Quick Events cleanup
        cleanupQuickEventsSystem();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Initialize bottom toolbar first
        setupBottomToolbar();

        // ✅ NEW: Initialize Quick Events system with services
        initializeQuickEventsSystemWithServices();
//
//        // ✅ NEW: Initialize Quick Events system
//        initializeQuickEventsSystem();
//
        // Super call
        super.onViewCreated(view, savedInstanceState);

        // Setup regular click listener for events preview
        setupAdapterRegularClickListener();

        // Setup long-click listener in adapter if available
        setupAdapterLongClickListener();

        // Initialize events preview after base setup
        initializeEventsPreview();
//
//        // DEBUG
//        debugSelectionState();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up bottom toolbar
        if (mBottomToolbar != null) {
            mBottomToolbar.destroy();
            mBottomToolbar = null;
        }
    }

    /**
     * ✅ REFACTORED: Back press - semplificato
     */
    public boolean onBackPressed() {
        if (isSelectionMode()) {
            Log.d(TAG, getFragmentName() + ": Back press in selection mode");

            BaseInteractiveAdapter adapter = getClickAdapter();
            if (adapter != null) {
                // ✅ Un solo punto di exit
                adapter.setSelectionMode(false);
            } else {
                exitSelectionMode(); // ✅ Fallback
            }

            return true; // Consumed
        }

        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            return adapter.onBackPressed();
        }

        return false; // Not consumed
    }

    // ==================== 🆕 DEPENDENCY INJECTION INTEGRATION ====================

    /**
     * ✅ Initialize dependency injection
     */
    private void initializeDependencyInjection() {
        try {
            if (getContext() != null) {
                ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(getContext());
                inject(serviceProvider);

                if (!areDependenciesReady()) {
                    Log.e(TAG, "Dependencies not ready for " + getFragmentName());
                } else {
                    Log.d(TAG, "Dependencies injected successfully for " + getFragmentName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize DI for " + getFragmentName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void inject(ServiceProvider serviceProvider) {
        mEventsService = serviceProvider.getEventsService();
        mUserService = serviceProvider.getUserService();

        Log.d(TAG, "Services injected into " + getFragmentName());
    }

    @Override
    public boolean areDependenciesReady() {
        return mEventsService != null && mUserService != null;
    }

    // ==================== 🆕 REFACTORED: Quick Events System ====================

    /**
     * ✅ REFACTORED: Initialize Quick Events system with service validation
     */
    private void initializeQuickEventsSystemWithServices() {
        if (!areDependenciesReady()) {
            Log.w(TAG, "Cannot initialize Quick Events - services not ready");
            return;
        }

        try {
            // ✅ Create templates (UI only, no business logic)
            mQuickEventTemplates = QuickEventTemplate.getAllTemplates();

            // ✅ Validate templates structure (UI validation only)
            if (QuickEventTemplate.validateAllTemplates()) {
                mQuickEventsInitialized = true;
                Log.d(TAG, getFragmentName() + ": Quick Events system initialized with " +
                        mQuickEventTemplates.size() + " templates");
            } else {
                Log.e(TAG, "Quick Events template validation failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Quick Events system: " + e.getMessage(), e);
            mQuickEventsInitialized = false;
        }
    }

    /**
     * ✅ REFACTORED: Service-based quick event creation
     */
    protected void createQuickEvent(ToolbarAction action, LocalDate date) {
        if (!areDependenciesReady()) {
            showQuickEventError("Services not available", action, date);
            return;
        }

        if (!mQuickEventsInitialized) {
            showQuickEventError("Quick Events system not initialized", action, date);
            return;
        }

        QuickEventTemplate template = getQuickEventTemplate(action);
        if (template == null) {
            showQuickEventError("Template not found for " + action.getEventDisplayName(), action, date);
            return;
        }

        Log.d(TAG, getFragmentName() + ": Creating quick event: " + action + " for " + date);

        // ✅ Check if event can be created (service-based validation)
        mEventsService.canCreateQuickEvent(action, date, mCurrentUserId)
                .thenAccept(result -> handleCanCreateResult(result, action, date, template))
                .exceptionally(throwable -> handleCanCreateException(throwable, action, date));
    }

    /**
     * ✅ REFACTORED: Service-based bulk quick event creation
     */
    protected void handleBulkQuickEventCreation(ToolbarAction action, Set<LocalDate> selectedDates) {
        if (!areDependenciesReady()) {
            showQuickEventError("Services not available", action, null);
            return;
        }

        if (selectedDates.isEmpty()) {
            showQuickEventError("No dates selected", action, null);
            return;
        }

        Log.d(TAG, "handleBulkQuickEventCreation: Creating " + action + " for " + selectedDates.size() + " dates");

        QuickEventTemplate template = getQuickEventTemplate(action);
        if (template == null) {
            showQuickEventError("Template not found for " + action.getEventDisplayName(), action, null);
            return;
        }

        // ✅ Check availability for all dates using service
        List<LocalDate> datesList = new ArrayList<>(selectedDates);
        mEventsService.canCreateQuickEventsForDates(action, datesList, mCurrentUserId)
                .thenAccept(result -> handleBulkAvailabilityResult(result, action, datesList, template))
                .exceptionally(throwable -> handleBulkAvailabilityException(throwable, action));
    }

// ==================== 🆕 SERVICE RESULT HANDLERS ====================

    /**
     * Handle single event availability result
     */
    private void handleCanCreateResult(OperationResult<Boolean> result, ToolbarAction action,
                                       LocalDate date, QuickEventTemplate template) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (result.isSuccess() && result.getData()) {
                    showQuickEventDialog(action, date, template);
                } else {
                    String error = result.isSuccess() ?
                            "Cannot create event for this date" :
                            result.getFormattedErrorMessage();
                    showQuickEventError(error, action, date);
                }
            });
        }
    }

    /**
     * Handle bulk availability result
     */
    private void handleBulkAvailabilityResult(OperationResult<Map<LocalDate, Boolean>> result,
                                              ToolbarAction action, List<LocalDate> dates,
                                              QuickEventTemplate template) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Map<LocalDate, Boolean> availability = result.getData();
                    List<LocalDate> validDates = new ArrayList<>();

                    for (Map.Entry<LocalDate, Boolean> entry : availability.entrySet()) {
                        if (entry.getValue()) {
                            validDates.add(entry.getKey());
                        }
                    }

                    if (validDates.isEmpty()) {
                        showQuickEventError("No valid dates for event creation", action, null);
                    } else {
                        showBulkEventConfirmationDialog(action, validDates, template);
                    }
                } else {
                    showQuickEventError(result.getFormattedErrorMessage(), action, null);
                }
            });
        }
    }

    /**
     * Handle service exceptions
     */
    private Void handleCanCreateException(Throwable throwable, ToolbarAction action, LocalDate date) {
        Log.e(TAG, "Error checking event availability", throwable);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showQuickEventError("Error checking availability: " + throwable.getMessage(), action, date);
            });
        }
        return null;
    }

    /**
     * Handle bulk availability exception
     */
    private Void handleBulkAvailabilityException(Throwable throwable, ToolbarAction action) {
        Log.e(TAG, "Error checking bulk availability", throwable);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showQuickEventError("Error checking availability: " + throwable.getMessage(), action, null);
            });
        }
        return null;
    }

    // ==================== 🆕 DIALOG CREATION (SERVICE-BASED) ====================

    /**
     * ✅ REFACTORED: Show quick event dialog with service integration
     */
    private void showQuickEventDialog(ToolbarAction action, LocalDate date, QuickEventTemplate template) {
        try {
            if (!areDependenciesReady() || getContext() == null) {
                showQuickEventError("Services not available", action, date);
                return;
            }

            ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(getContext());

            QuickEventConfirmationDialog dialog = new QuickEventConfirmationDialog(
                    requireContext(),
                    serviceProvider,
                    template,
                    date,
                    mCurrentUserId,
                    new QuickEventCreationListener()
            );

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to show quick event dialog", e);
            showQuickEventError("Failed to create event dialog: " + e.getMessage(), action, date);
        }
    }

    /**
     * ✅ ENHANCED: Show bulk confirmation dialog with service validation
     */
    private void showBulkEventConfirmationDialog(ToolbarAction action, List<LocalDate> validDates,
                                                 QuickEventTemplate template) {
        if (getContext() == null) return;

        String eventName = template.getDisplayName();
        int validCount = validDates.size();

        // Build confirmation message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("Create '%s' event for:", eventName));
        messageBuilder.append("\n\n");
        messageBuilder.append("✅ ").append(validCount).append(" valid dates");
        messageBuilder.append("\n\nEvents will be created for all valid dates.");

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Confirm Bulk Event Creation")
                .setMessage(messageBuilder.toString())
                .setPositiveButton("Create", (dialog, which) -> {
                    createBulkQuickEvents(action, validDates, template);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✅ NEW: Create bulk events using service
     */
    private void createBulkQuickEvents(ToolbarAction action, List<LocalDate> dates, QuickEventTemplate template) {
        if (!areDependenciesReady()) {
            showQuickEventError("Services not available", action, null);
            return;
        }

        try {
            // Create base request from template
            LocalDate firstDate = dates.get(0);
            QuickEventRequest baseRequest = template.createEventRequest(firstDate, mCurrentUserId);

            // Use service for bulk creation
            mEventsService.createQuickEventsForDates(baseRequest, dates)
                    .thenAccept(result -> handleBulkCreationResult(result, action))
                    .exceptionally(throwable -> handleBulkCreationException(throwable, action));

        } catch (Exception e) {
            Log.e(TAG, "Error creating bulk events", e);
            showQuickEventError("Error creating events: " + e.getMessage(), action, null);
        }
    }

    /**
     * Handle bulk creation result
     */
    private void handleBulkCreationResult(OperationResult<List<LocalEvent>> result, ToolbarAction action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (result.isSuccess()) {
                    List<LocalEvent> events = result.getData();
                    String message = "Created " + events.size() + " events successfully!";
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                    // Trigger refresh
                    onForceEventsRefresh();
                    //onEventsRefreshNeeded();

                    Log.d(TAG, "Bulk events created: " + events.size());
                } else {
                    showQuickEventError(result.getFormattedErrorMessage(), action, null);
                }
            });
        }
    }

    /**
     * Handle bulk creation exception
     */
    private Void handleBulkCreationException(Throwable throwable, ToolbarAction action) {
        Log.e(TAG, "Bulk event creation failed", throwable);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showQuickEventError("Failed to create events: " + throwable.getMessage(), action, null);
            });
        }
        return null;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get quick event template for action
     */
    protected QuickEventTemplate getQuickEventTemplate(ToolbarAction action) {
        return mQuickEventsInitialized ? mQuickEventTemplates.get(action) : null;
    }

    /**
     * Check if quick events system is available
     */
    public boolean isQuickEventsAvailable() {
        return mQuickEventsInitialized && areDependenciesReady();
    }

    /**
     * Show quick event error
     */
    protected void showQuickEventError(String message, ToolbarAction action, LocalDate date) {
        if (getContext() != null) {
            Library.showError(getContext(), "Quick Event Error: " + message, Toast.LENGTH_SHORT);
        }
        Log.e(TAG, "Quick Event Error [" + action + ", " + date + "]: " + message);
    }

    /**
     * Cleanup quick events system
     */
    private void cleanupQuickEventsSystem() {
        if (mQuickEventTemplates != null) {
            mQuickEventTemplates.clear();
            mQuickEventTemplates = null;
        }
        mQuickEventsInitialized = false;
        Log.d(TAG, "Quick Events system cleaned up");
    }

    // ==================== EVENT CREATION LISTENER ====================

    /**
     * ✅ Service-based event creation listener
     */
    private class QuickEventCreationListener implements QuickEventConfirmationDialog.EventCreationListener {

        @Override
        public void onEventCreated(LocalEvent createdEvent, ToolbarAction sourceAction, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Quick event created: " + createdEvent.getId());

            // Exit selection mode
            forceExitSelectionMode();

            // Trigger UI refresh
            onEventsRefreshNeeded();

            // Show success feedback
//            if (getContext() != null) {
//                Library.showSuccess(getContext(),
//                        "Event '" + createdEvent.getTitle() + "' creato!",
//                        Toast.LENGTH_SHORT);
//            }
        }

        @Override
        public void onEventCreationCancelled(ToolbarAction sourceAction, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Quick event creation cancelled");
        }

        @Override
        public void onEventCreationFailed(ToolbarAction sourceAction, LocalDate date, String errorMessage) {
            Log.e(TAG, getFragmentName() + ": Quick event creation failed: " + errorMessage);
            showQuickEventError(errorMessage, sourceAction, date);
        }

        /**
         * Called when events need to be refreshed (after successful creation)
         */
        @Override
        public void onEventsRefreshNeeded() {
            // Should pass though to the parent fragment (BaseFragment) ?
            onForceEventsRefresh();

        }
    }

    // ==================== HELPER INTERFACES ====================

    /**
     * Interface for refreshable adapters
     */
    public interface Refreshable {
        void refreshData();
    }

    /**
     * Interface for activities that can be notified of events changes
     */
    public interface EventsRefreshNotifiable {
        void onEventsDataChanged();
    }

    // ==================== NEW: Quick Events System Initialization ====================

//    /**
//     * ✅ NEW: Initialize Quick Events system
//     * Sets up templates and validates configuration
//     */
//    private void initializeQuickEventsSystem() {
//        try {
//            Log.d(TAG, "Initializing Quick Events system...");
//
//            // Load all quick event templates
//            mQuickEventTemplates = QuickEventTemplate.getAllTemplates();
//
//            // Validate templates
//            if (QuickEventTemplate.validateAllTemplates()) {
//                mQuickEventsInitialized = true;
//                Log.d(TAG, "✅ Quick Events system initialized successfully with " +
//                        mQuickEventTemplates.size() + " templates");
//            } else {
//                Log.e(TAG, "❌ Quick Events template validation failed");
//                mQuickEventsInitialized = false;
//            }
//
//            // Initialize user context (can be overridden by subclasses)
//            initializeUserContext();
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Failed to initialize Quick Events system: " + e.getMessage());
//            mQuickEventsInitialized = false;
//        }
//    }

    /**
     * ✅ NEW: Initialize user context for event creation
     * Subclasses can override to provide specific user context
     */
    protected void initializeUserContext() {
        // Default implementation - get user from session/preferences
        // Subclasses should override with specific user management logic
        mCurrentUserId = getCurrentUserId();

        Log.d(TAG, "User context initialized: userId=" + mCurrentUserId);
    }

    /**
     * ✅ NEW: Get current user ID
     * Subclasses should override with actual user management integration
     */
    protected Long getCurrentUserId() {
        // TODO: Integrate with actual user session management
        // For now, return a default user ID or null
        return null; // Will be handled gracefully by QuickEventLogicAdapter
    }

//    /**
//     * ✅ NEW: Cleanup Quick Events system
//     */
//    private void cleanupQuickEventsSystem() {
//        if (mQuickEventTemplates != null) {
//            mQuickEventTemplates.clear();
//            mQuickEventTemplates = null;
//        }
//        mQuickEventsInitialized = false;
//        Log.d(TAG, "Quick Events system cleaned up");
//    }

    // ===========================================
    // Setup Methods (Enhanced)
    // ===========================================

    /**
     * Initialize bottom selection toolbar
     */
    private void setupBottomToolbar() {
        if (getContext() == null) return;

        mBottomToolbar = new BottomSelectionToolbar(getContext());
        Log.d(TAG, "setupBottomToolbar: ✅ Bottom toolbar initialized");
    }

    /**
     * Setup regular click listener for events preview
     */
    protected void setupAdapterRegularClickListener() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            // Setup regular click listener for events preview
            adapter.setRegularClickListener(new BaseInteractiveAdapter.DayRegularClickListener() {
                @Override
                public void onDayRegularClick(Day day, LocalDate date, View itemView, int position) {
                    handleDayRegularClick(day, date, itemView, position);
                }
            });

            Log.d(TAG, getFragmentName() + ": Regular click listener setup in adapter");
        } else {
            Log.e(TAG, "setupAdapterRegularClickListener: ❌ Adapter is null, cannot setup regular click listener");
        }
    }

    /**
     * Setup long-click listener in adapter
     */
    protected void setupAdapterLongClickListener() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            adapter.setLongClickListener(this);
            Log.d(TAG, "setupAdapterLongClickListener: ✅ Long-click listener setup in adapter");
        } else {
            Log.e(TAG, "setupAdapterLongClickListener: ❌ Adapter is null, cannot setup long-click listener ");
        }
    }

    // ===========================================
    // DayLongClickListener Implementation
    // ===========================================

    @Override
    public void onDayLongClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, "onDayLongClick: Day long-clicked: " + date);

        // ❌ remove: enterSelectionMode()
        // ✅ ONLY feedback UI non-state
        updateSelectionUI(); // Update UI, no state change
    }

    /**
     * Handle toolbar action selected
     *
     * @param action The selected action
     * @param day    The Day object
     * @param date   The LocalDate
     */
    @Override
    public void onToolbarActionSelected(ToolbarAction action, Day day, LocalDate date) {
        Log.d(TAG, "onToolbarActionSelected: Toolbar action selected: " + action + " for date: " + date);

        switch (action) {
            case ADD_EVENT:
                openEventEditor(date);
                break;

            case VIEW_EVENTS:
                showEventsDialog(date);
                break;

            // ✅ ENHANCED: Quick Event Actions with proper integration
            case FERIE:
            case MALATTIA:
            case LEGGE_104:
            case PERMESSO:
            case PERMESSO_SINDACALE:
            case STRAORDINARIO:
                // These are handled by adapter, but we can show confirmation
//                handleQuickEventCreation(action, date); break;  // OLD version without services
                createQuickEvent(action, date);
                break;

            default:
                Log.w(TAG, "Unhandled toolbar action: " + action);
        }

        // Exit selection mode after action
        exitSelectionMode();
    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        Log.d(TAG, getFragmentName() + ": Selection mode changed: " + isSelectionMode +
                ", count: " + selectedCount);

        // ✅ SOLO UI UPDATES
        // selectedCount is still 0.. ( && selectedCount > 0 )
        if (isSelectionMode) {
            enterSelectionMode();
            Log.d(TAG, "UI updated: (true) " + selectedCount);
        } else {
            exitSelectionMode();
            Log.d(TAG, "UI updated: (false) " + selectedCount);
        }

//
//        // 🔧 FIX: Store previous mode before updating
//        boolean previousMode = mIsSelectionMode;
//        mIsSelectionMode = isSelectionMode;

//        if (isSelectionMode && selectedCount > 0) {
//            // Enter/update selection mode
//            if (!mIsSelectionMode) {
//                enterSelectionMode();
//            }
//            updateBottomToolbar();
//        } else {
//            // Exit selection mode
//            exitSelectionMode();
//        }
//    }

//        if (isSelectionMode && !previousMode) {
//            // Entering selection mode
//            Log.d(TAG, getFragmentName() + ": Entering selection mode");
//            enterSelectionMode();
//        } else if (!isSelectionMode && previousMode) {
//            // Exiting selection mode
//            Log.d(TAG, getFragmentName() + ": Exiting selection mode");
//            exitSelectionMode();
//        }
//
//        updateSelectionUI();
//        updateActionBarTitle(selectedCount);
//
//        // 🔧 FIX: Special handling for auto-exit (now previousMode is correctly defined)
//        if (!isSelectionMode && previousMode && selectedCount == 0) {
//            Log.d(TAG, getFragmentName() + ": Auto-exit from selection mode detected (no items selected)");
//            // Additional cleanup or notifications can be added here
//            onAutoExitSelectionMode();
//        }
    }

    /**
     * Called when day selection changes in multi-select mode
     *
     * @param day        The Day object
     * @param date       The LocalDate
     * @param isSelected Whether the day is now selected
     */
    @Override
    public void onDaySelectionChanged(Day day, LocalDate date, boolean isSelected) {

    }

//    /**
//     * 🔧 NEW: Called when selection mode is auto-exited due to no items selected
//     * Subclasses can override for specific behavior
//     */
//    protected void onAutoExitSelectionMode() {
//        Log.d(TAG, getFragmentName() + ": Auto-exit selection mode completed");
//
//        // Default implementation - subclasses can override for specific behavior
//        // For example: show toast, update status, etc.
//
//        // Optional: Show subtle feedback that selection mode was exited
//        if (getContext() != null) {
//            // Uncomment if you want user feedback:
//            // Toast.makeText(getContext(), "Selezione annullata", Toast.LENGTH_SHORT).show();
//        }
//    }

//        BaseInteractiveAdapter adapter = getClickAdapter();
//        if (adapter != null) {
//            int selectedCount = adapter.getSelectedCount();
//            updateActionBarTitle(selectedCount);
//
//            // 🔧 NEW: Log selection count for debugging
//            Log.d(TAG, getFragmentName() + ": Current selection count: " + selectedCount);
//        }
//    }

    // ==================== NEW: Quick Event Creation Logic ====================

//    /**
//     * ✅ ENHANCED: Handle quick event creation with confirmation dialog
//     * Main entry point for toolbar action → confirmation dialog → actual event creation
//     */
//    private void handleQuickEventCreation(ToolbarAction action, LocalDate date) {
//        Log.d(TAG, "handleQuickEventCreation: Creating quick event " + action + " for date " + date);
//
//        // Validate Quick Events system initialization
//        if (!mQuickEventsInitialized) {
//            Log.e(TAG, "❌ Quick Events system not initialized");
//            showQuickEventError("Sistema eventi rapidi non disponibile", action, date);
//            return;
//        }
//
//        // Get template for this action
//        QuickEventTemplate template = mQuickEventTemplates.get(action);
//        if (template == null) {
//            Log.e(TAG, "❌ No template found for action: " + action);
//            showQuickEventError("Template non trovato per " + action.getEventDisplayName(), action, date);
//            return;
//        }
//
//        // Validate template can be used on this date
//        if (!template.canUseOnDate(date, mCurrentUserId)) {
//            Log.w(TAG, "⚠️ Template cannot be used on date: " + date + " for action: " + action);
//            String eventName = template.getDisplayName();
//            showQuickEventError("Impossibile creare '" + eventName + "' per " + date, action, date);
//            return;
//        }
//
//        // ✅ NEW: Show confirmation dialog instead of creating event directly
//        showQuickEventConfirmationDialog(action, date, template);
//    }

//    /**
//     * ✅ NEW: Show confirmation dialog for quick event creation
//     * Provides preview and editing capabilities before final creation
//     */
//    private void showQuickEventConfirmationDialog(ToolbarAction action, LocalDate date, QuickEventTemplate template) {
//        Log.d(TAG, "showQuickEventConfirmationDialog: Showing dialog for " + action + " on " + date);
//
//        try {
//            // Create and show confirmation dialog with event creation listener
//            QuickEventConfirmationDialog.showConfirmationDialog(
//                    getContext(),
//                    action,
//                    date,
//                    mCurrentUserId,
//                    template,
//                    new QuickEventConfirmationDialogListener()
//            );
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Failed to show confirmation dialog: " + e.getMessage());
//            showQuickEventError("Errore nell'apertura del dialog: " + e.getMessage(), action, date);
//        }
//    }

//    /**
//     * ✅ NEW: Event creation listener for confirmation dialog callbacks
//     * Handles results from the confirmation dialog
//     */
//    private class QuickEventConfirmationDialogListener implements QuickEventConfirmationDialog.EventCreationListener {
//
//        @Override
//        public void onEventCreated(LocalEvent createdEvent, ToolbarAction sourceAction, LocalDate date) {
//            Log.d(TAG, "onEventCreated: Event created successfully via dialog: " + createdEvent.getTitle());
//
//            // Trigger fragment-level event handling
//            onQuickEventCreated(sourceAction, date);
//
//            // Analytics or additional tracking can be added here
//            trackQuickEventCreation(sourceAction, date, true);
//        }
//
//
//        @Override
//        public void onEventCreationCancelled(ToolbarAction sourceAction, LocalDate date) {
//            Log.d(TAG, "onEventCreationCancelled: User cancelled event creation for " + sourceAction + " on " + date);
//
//            // Optional: Analytics tracking for cancellations
//            trackQuickEventCreation(sourceAction, date, false);
//        }
//
//        @Override
//        public void onEventCreationFailed(ToolbarAction sourceAction, LocalDate date, String errorMessage) {
//            Log.e(TAG, "onEventCreationFailed: " + errorMessage + " for " + sourceAction + " on " + date);
//
//            // Show error to user (confirmation dialog already shows Toast, but we can add more here)
//            onQuickEventCreationError(sourceAction, date, errorMessage);
//        }
//
//        @Override
//        public void onEventsRefreshNeeded() {
//            Log.d(TAG, "onEventsRefreshNeeded: Triggering events refresh");
//
//            // Trigger events refresh in the fragment
//            onForceEventsRefresh();
//        }
//    }

//    /**
//     * ✅ NEW: Handle quick event creation error
//     * Called when event creation fails for any reason
//     */
//    protected void onQuickEventCreationError(ToolbarAction action, LocalDate date, String errorMessage) {
//        Log.e(TAG, "Quick event creation error: " + errorMessage + " for " + action + " on " + date);
//
//        // Subclasses can override for specific error handling
//        // Default implementation: just log (error Toast already shown by dialog)
//    }

    /**
     * ✅ NEW: Track quick event creation for analytics
     * Optional analytics tracking for usage patterns
     */
    protected void trackQuickEventCreation(ToolbarAction action, LocalDate date, boolean success) {
        // Subclasses can override for analytics integration
        // Default implementation: just log
        Log.d(TAG, "Quick event tracking: " + action + " for " + date + " success=" + success);
    }

    /**
     * ✅ DEPRECATED: Old method - now redirects to new confirmation dialog system
     * Keeping for backward compatibility
     */
    @Deprecated
    protected boolean saveQuickEvent(LocalEvent event, ToolbarAction action, LocalDate date) {
        Log.w(TAG, "saveQuickEvent: This method is deprecated. Use QuickEventConfirmationDialog instead.");

        // This method is now handled by QuickEventConfirmationDialog
        // Keeping minimal implementation for backward compatibility
        return false;
    }

//    /**
//     * ✅ DEPRECATED: Old method - now handled by confirmation dialog
//     * Keeping for backward compatibility
//     */
//    @Deprecated
//    private void showQuickEventSuccess(LocalEvent event, ToolbarAction action, LocalDate date) {
//        Log.w(TAG, "showQuickEventSuccess: This method is deprecated. Use QuickEventConfirmationDialog instead.");
//
//        // Success feedback is now handled by QuickEventConfirmationDialog
//    }

//    /**
//     * ✅ NEW: Show error message for quick event creation
//     */
//    private void showQuickEventError(String errorMessage, ToolbarAction action, LocalDate date) {
//        if (getContext() == null) return;
//
//        String message = "❌ " + errorMessage;
//        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
//
//        Log.e(TAG, "Quick event error shown: " + errorMessage + " for action " + action + " on " + date);
//    }

//    /**
//     * ✅ NEW: Format date for user display
//     * Can be overridden for localized formatting
//     */
//    protected String formatDateForUser(LocalDate date) {
//        // Simple formatting - can be enhanced with proper localization
//        return date.toString(); // TODO: Use DateTimeFormatter with locale
//    }

    // ==================== ENHANCED: Event Creation Methods ====================

//    /**
//     * ✅ ENHANCED: Show quick event confirmation with template preview
//     * Replaces the simple Toast implementation
//     */
//    protected void showQuickEventConfirmation(ToolbarAction action, LocalDate date) {
//        // This method is now handled by handleQuickEventCreation
//        // Keeping for backward compatibility
//        handleQuickEventCreation(action, date);
//        //onQuickEventCreated(action, date);
//    }

    // ===========================================
    // Selection Mode UI Management
    // ===========================================

    /**
     * Proxy for ADAPTER: Enter selection mode (update UI accordingly?)
     */
    protected void enterSelectionMode() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null && !adapter.isSelectionMode()) {
            adapter.setSelectionMode(true);
        }

        hideFabForSelection();
        showBottomToolbar();
        updateActionBarForSelection(isSelectionMode());
        updateBottomToolbar();
        // TODO: update fab visibility
        //toggleFabVisibility(mFabGoToToday);
        updateActionBarTitle(getCurrentSelectionCount());

        Log.d(TAG, "enterSelectionMode: Entered selection mode");
    }

    /**
     * ✅ REFACTORED: Exit selection mode - eliminato clear diretto
     */
    protected void exitSelectionMode() {
        if (isSelectionMode()) return; // false is ok(already updated)

        // ✅ Update action bar/toolbar
        updateActionBarForSelection(false);

        // ✅ Show FAB again
        showFabAfterSelection();

        // ✅ Hide bottom toolbar
        hideBottomToolbar();

        // ✅ SOLO setSelectionMode - il clear è gestito dall'adapter
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false); // ✅ Questo gestirà automaticamente il clear se necessario
        }

        Log.d(TAG, "exitSelectionMode: Exited selection mode");
    }
//    /**
//     * Exit selection mode - restore normal UI
//     */
//    protected void exitSelectionMode() {
//        // Exit if in selection mode
//        if (!isSelectionMode()) return;
//
//        // Update action bar/toolbar
//        updateActionBarForSelection(false);
//
//        // Show FAB again
//        showFabAfterSelection();
//
//        // Hide bottom toolbar
//        hideBottomToolbar();
//
//        // Disable selection mode in adapter
//        BaseInteractiveAdapter adapter = getClickAdapter();
//        if (adapter != null && adapter.isSelectionMode()) {
//            adapter.setSelectionMode(false);
//            adapter.clearSelections();
//        }
//
//        Log.d(TAG, "exitSelectionMode: Exited selection mode");
//    }

    /**
     * Show bottom toolbar with current selection
     */
    private void showBottomToolbar() {
        if (mBottomToolbar == null) {
            Log.e(TAG, "showBottomToolbar: ❌ Bottom toolbar is null");
            return;
        }

        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter == null) return;

        ViewGroup container = getToolbarContainer();
        if (container == null) {
            Log.e(TAG, "showBottomToolbar: ❌ No toolbar container available");
            return;
        }

        Set<LocalDate> selectedDates = adapter.getSelectedDates();
        mBottomToolbar.show(container, selectedDates, this);
    }

    /**
     * Hide bottom toolbar
     */
    private void hideBottomToolbar() {
        if (mBottomToolbar != null) {
            mBottomToolbar.hide();
        } else {
            Log.e(TAG, "hideBottomToolbar: ❌ Bottom toolbar is null");
        }
    }

    /**
     * Hide FAB during selection mode
     */
    protected void hideFabForSelection() {

        if (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mHasSelectionHiddenFab = true;
            toggleFabVisibility(mFabGoToToday);
            mFabGoToToday.hide();
        }
    }

    /**
     * Show FAB after exiting selection mode
     */
    protected void showFabAfterSelection() {
        if (mFabGoToToday != null && mHasSelectionHiddenFab) {
            mHasSelectionHiddenFab = false;
            toggleFabVisibility(mFabGoToToday);
            mFabGoToToday.show();
        }
    }

    /**
     * Setup action bar for selection mode
     * mock default implementation
     */
    protected void setupSelectionActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific selection action bar setup
        // For now, just update the title

        updateActionBarTitle(getCurrentSelectionCount());

    }

    /**
     * Restore normal action bar
     * mock default implementation
     */
    protected void restoreNormalActionBar(QDueMainActivity mainActivity) {
        // Subclasses can override for specific action bar restoration
        // For now, just reset the title

        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(getFragmentName());
        }
    }

    // ===========================================
    // UI Update Methods
    // ===========================================

    /**
     * Update UI elements based on current selection state
     */
    protected void updateSelectionUI() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter == null) return;

        int selectedCount = adapter.getSelectedCount();

        // Update toolbar
        updateBottomToolbar();

        // Update action bar title
        updateActionBarTitle(selectedCount);

        // Update any other UI elements
        updateToolbarMenuItems();
    }

    /**
     * Update action bar title with selection count
     */
    protected void updateActionBarTitle(int selectedCount) {
        if (isSelectionMode() && getActivity() != null) {
            String title = selectedCount > 0 ?
                    String.format(QDue.getLocale(), "%d selezionati", selectedCount) :
                    "Seleziona";

            // Update action bar title
            if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(title);
            }

            Log.d(TAG, "Updated action bar title: " + title);
        }
    }

    /**
     * Update action bar for selection mode
     */
    protected void updateActionBarForSelection(boolean isSelectionMode) {
        if (getActivity() instanceof QDueMainActivity mainActivity) {

            // Custom action bar if needed
            if (isSelectionMode) {
                // Show selection action bar
                setupSelectionActionBar(mainActivity);
                Log.d(TAG, "Showing selection action bar");
            } else {
                // Restore normal action bar
                restoreNormalActionBar(mainActivity);
                Log.d(TAG, "Restoring normal action bar");
            }
        }
    }

    /**
     * Update toolbar menu items
     */
    protected void updateToolbarMenuItems() {
        // Update menu items based on selection state
        if (mSelectAllMenuItem != null) {
            mSelectAllMenuItem.setVisible(isSelectionMode());
        }
        if (mClearSelectionMenuItem != null) {
            mClearSelectionMenuItem.setVisible(isSelectionMode());
        }
        if (mExitSelectionMenuItem != null) {
            mExitSelectionMenuItem.setVisible(isSelectionMode());
        }
    }

    /**
     * Update bottom toolbar with current selection
     */
    private void updateBottomToolbar() {
        if (mBottomToolbar == null || !mBottomToolbar.isVisible()) return;

        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            Set<LocalDate> selectedDates = adapter.getSelectedDates();
            mBottomToolbar.updateSelection(selectedDates);
        }
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * Check if currently in selection mode with items selected
     */
    public boolean hasActiveSelection() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        return isSelectionMode() && adapter != null && adapter.getSelectedCount() > 0;
    }

    /**
     * Get current selection count safely
     */
    public int getCurrentSelectionCount() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }

    // ===========================================
    // Event Handling Methods
    // ===========================================

    /**
     * Show events dialog for the specified date
     */
    protected void showEventsDialog(LocalDate date) {
        // Get events for this date
        List<LocalEvent> events = getEventsForDate(date);
        onShowEventsDialog(date, events);
    }
//        Log.d(TAG, "Showing events dialog for date: " + date);
//
//        // Get events for this date
//        Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
//        List<LocalEvent> events = eventsCache.get(date);
//
//        if (events == null || events.isEmpty()) {
//            // Show "no events" message
//            showNoEventsMessage(date);
//        } else {
//            // Show events list dialog
//            showEventsListDialog(date, events);
//        }
//
//        // Delegate to subclass for specific handling
//        onShowEventsDialog(date, events);
//    }

    /**
     * Get events for a specific date from cache
     * default implementation -  sub classes should override
     */
    protected List<LocalEvent> getEventsForDate(LocalDate date) {
        Map<LocalDate, List<LocalEvent>> eventsCache = getEventsCache();
        List<LocalEvent> events = eventsCache.get(date);
        return events != null ? events : new ArrayList<>();
    }

    // ===========================================
    // Template Methods for Subclasses
    // ===========================================

    /**
     * Called when event editor should be opened
     * Subclasses should override for specific navigation
     */
    protected void onOpenEventEditor(LocalDate date) {
        Log.d(TAG, "Opening event editor for date: " + date);
        // Default implementation - subclasses can override
    }

    /**
     * Called when events dialog should be shown
     */
    protected void onShowEventsDialog(LocalDate date, @Nullable List<LocalEvent> events) {
        Log.d(TAG, "Showing events dialog for date: " + date);
        // Default implementation - subclasses can override
    }

    /**
     * ✅ ENHANCED: Called when a quick event is created
     * Now includes actual event creation logic
     */
    protected void onQuickEventCreated(ToolbarAction action, LocalDate date) {
        Log.d(TAG, "Quick event created: " + action + " for date: " + date);

        // Notify subclasses that an event was created
        // Subclasses can override for specific handling (analytics, notifications, etc.)
    }

    /**
     * ✅ NEW: Called when quick events system needs user context
     * Subclasses can override to provide specific user ID logic
     */
    protected void onQuickEventsUserContextNeeded() {
        Log.d(TAG, "Quick events system requesting user context");

        // Re-initialize user context
        initializeUserContext();
    }

    // ===========================================
    // Menu Handling
    // ===========================================

    /**
     * Handle options menu item selection for selection mode
     */
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter == null) return false;

        // Handle selection mode menu items
        int itemId = item.getItemId();

        // These would be defined in menu XML
        /*
        if (itemId == R.id.action_select_all) {

        for (LocalDate date : getAllAvailableDates()) {
        if (!mSelectedDates.contains(date)) {
            toggleDateSelection(date);
        }
    }

            //adapter.selectAllDays();
            return true;
        } else if (itemId == R.id.action_clear_selection) {
        if (!mSelectedDates.isEmpty()) {
        Set<LocalDate> toDeselect = new HashSet<>(mSelectedDates);
        for (LocalDate date : toDeselect) {
            toggleDateSelection(date);
        }
    }
            //adapter.clearSelections(); // avoid this -> adapter.setSelectionMode(false)
            return true;
        } else if (itemId == R.id.action_exit_selection) {
            exitSelectionMode();
            return true;
        }
        */

        return false;
    }

    // ===========================================
    // Placeholder Methods (to be replaced by actual implementations)
    // ===========================================

    /**
     * Show placeholder for event editor
     */
    protected void showEventEditorPlaceholder(LocalDate date) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Apertura editor eventi per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show no events message
     */
    protected void showNoEventsMessage(LocalDate date) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Nessun evento per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show events list dialog
     */
    protected void showEventsListDialog(LocalDate date, List<LocalEvent> events) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    events.size() + " eventi per " + date,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ===========================================
    // Public API Methods
    // ===========================================

    /**
     * Public method to get selected dates
     */
    public Set<LocalDate> getSelectedDates() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedDates() : new java.util.HashSet<>();
    }

    /**
     * Proxy for ADAPTER: check if it's in selection mode
     */
    public boolean isSelectionMode() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        return adapter != null && adapter.isSelectionMode();
    }

    /**
     * Public method to get selected count
     */
    public int getSelectedCount() {
        BaseInteractiveAdapter adapter = getClickAdapter();
        return adapter != null ? adapter.getSelectedCount() : 0;
    }

    /**
     * ✅ REFACTORED: Force exit - eliminato clear diretto
     */
    public void forceExitSelectionMode() {
        Log.d(TAG, getFragmentName() + ": Force exit selection mode requested");

        if (isSelectionMode()) {
            BaseInteractiveAdapter adapter = getClickAdapter();
            if (adapter != null) {
                adapter.setSelectionMode(false); // ✅ Questo gestirà tutto automaticamente
            } else {
                exitSelectionMode(); // ✅ Fallback
            }
        }
    }
//    /**
//     * 🔧 NEW: Force exit selection mode (public method for external calls)
//     */
//    public void forceExitSelectionMode() {
//        Log.d(TAG, getFragmentName() + ": Force exit selection mode requested");
//
//        if (isSelectionMode()) {
//            BaseInteractiveAdapter adapter = getClickAdapter();
//            if (adapter != null) {
//                adapter.deselectAll(); // This will trigger auto-exit
//            } else {
//                // Fallback: exit directly
//                exitSelectionMode();
//            }
//        }
//    }

//    /**
//     * ✅ NEW: Get quick event template for action
//     */
//    public QuickEventTemplate getQuickEventTemplate(ToolbarAction action) {
//        return mQuickEventTemplates != null ? mQuickEventTemplates.get(action) : null;
//    }

//    /**
//     * ✅ NEW: Check if quick events system is available
//     */
//    public boolean isQuickEventsAvailable() {
//        return mQuickEventsInitialized && mQuickEventTemplates != null;
//    }

    /**
     * ✅ NEW: Get preview for quick event creation
     */
    public EventPreview getQuickEventPreview(ToolbarAction action, LocalDate date) {
        QuickEventTemplate template = getQuickEventTemplate(action);
        return template != null ? template.getPreview(date) : null;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////

    // ===========================================
    // NUOVO: Events Preview Integration
    // ===========================================

    /**
     * Initialize events preview manager in onCreate or onViewCreated
     */
    protected void initializeEventsPreview() {
        mEventsPreviewManager = new EventsPreviewManager(requireContext());
        mEventsPreviewManager.setEventsPreviewListener(new EventsPreviewListenerImpl());

        // Set view type based on fragment type
        EventsPreviewManager.ViewType viewType = getEventsPreviewViewType();
        mEventsPreviewManager.setViewType(viewType);

        Log.d(TAG, getFragmentName() + ": Events preview initialized with type: " + viewType);
    }
//
//    /**
//     * Abstract method for subclasses to specify their view type
//     */
//    protected abstract EventsPreviewManager.ViewType getEventsPreviewViewType();

    /**
     * Handle regular click on day (non-selection mode)
     */
    protected void handleDayRegularClick(Day day, LocalDate date, View itemView, int position) {
        Log.d(TAG, getFragmentName() + ": Regular click on date: " + date);

        if (!mEventsPreviewEnabled) {
            Log.v(TAG, "Events preview disabled, ignoring click");
            return;
        }

        // Get events for this date
        List<LocalEvent> events = getEventsForDate(date);

        if (events.isEmpty()) {
            // No events - show "add event" option or ignore
            handleNoEventsClick(date, itemView);
        } else {
            // Has events - show preview
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.showEventsPreview(date, events, itemView);
            }
        }
    }

    /**
     * Handle click on date with no events
     */
    protected void handleNoEventsClick(LocalDate date, View itemView) {
        Log.d(TAG, getFragmentName() + ": Click on date with no events: " + date);

        // TODO: offer to add event
//        showAddEventOption(date);
    }

    /**
     * Show option to add event for date with no events
     */
    protected void showAddEventOption(LocalDate date) {
        if (getContext() != null) {
            // Simple toast for now - can be enhanced with quick add dialog
            String message = "Nessun evento per " + date + ". Tocca a lungo per aggiungere.";
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

// ===========================================
// Events Preview Listener Implementation
// ===========================================

    /**
     * Implementation of EventsPreviewListener for handling events preview callbacks
     */
    private class EventsPreviewListenerImpl implements EventsPreviewInterface.EventsPreviewListener {

        @Override
        public void onEventQuickAction(EventsPreviewInterface.EventQuickAction action,
                                       LocalEvent event, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Event quick action: " + action + " for event: " + event.getTitle());

            switch (action) {
                case VIEW_DETAIL:
                    openEventDetail(event, date);
                    break;
                case EDIT:
                    openEventEditor(event, date);
                    break;
                case DELETE:
                    confirmDeleteEvent(event, date);
                    break;
                case DUPLICATE:
                    duplicateEvent(event, date);
                    break;
                case TOGGLE_COMPLETE:
                    toggleEventComplete(event, date);
                    break;
            }

            // Hide preview after action
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.hideEventsPreview();
            }
        }

        @Override
        public void onEventsGeneralAction(EventsPreviewInterface.EventGeneralAction action, LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Events general action: " + action + " for date: " + date);

            switch (action) {
                case ADD_EVENT:
                    openEventEditor(date);
                    break;
                case NAVIGATE_TO_EVENTS_ACTIVITY:
                    navigateToEventsActivity(date);
                    break;
                case REFRESH_EVENTS:
                    onForceEventsRefresh();
                    break;
            }

            // Hide preview after action
            if (mEventsPreviewManager != null) {
                mEventsPreviewManager.hideEventsPreview();
            }
        }

        @Override
        public void onEventsPreviewShown(LocalDate date, int eventCount) {
            Log.d(TAG, getFragmentName() + ": Events preview shown for " + date + " with " + eventCount + " events");

            // Optional: Analytics, state tracking, etc.
        }

        @Override
        public void onEventsPreviewHidden(LocalDate date) {
            Log.d(TAG, getFragmentName() + ": Events preview hidden for " + date);

            // Optional: Cleanup, state tracking, etc.
        }
    }

    // ===========================================
    // Event Action Handlers
    // ===========================================

    /**
     * 🆕 NEW: Open event detail view with proper navigation stack
     * This creates the desired flow: Detail → EventsList → MainActivity
     *
     * @param event The event to view in detail
     * @param date  The source date context
     */
    protected void openEventDetail(@NonNull LocalEvent event, LocalDate date) {
        final String mTAG = "openEventDetail: ";
        Log.d(TAG, mTAG + "Opening detail for event: " + event.getTitle());

        if (event.getId() == null) {
            Log.e(TAG, mTAG + "Event or event ID is null");
            Library.showToast(getContext(), "Errore: evento non valido");
            return;
        }

        try {
            // Create intent for EventsActivity with detail navigation
            Intent eventsIntent = createEventDetailIntent(event, date);

            // Start activity
            if (getActivity() != null) {
                getActivity().startActivity(eventsIntent);
                Log.d(TAG, mTAG + "✅ EventsActivity started for event detail: " + event.getId());
            } else {
                Log.e(TAG, mTAG + "Activity is null, cannot start EventsActivity");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error opening event detail: " + e.getMessage());
            Library.showToast(getContext(), "Errore nell'apertura del dettaglio evento");
        }
    }

    /**
     * Open event editor for existing event
     */
    protected void openEventEditor(LocalEvent event, LocalDate date) {
        Log.d(TAG, "Opening event editor for event: " + event.getTitle());
        final String mTAG = "openEventEditor: ";
        Log.d(TAG, mTAG + "Opening editor for event: " + event.getTitle());

        if (event == null || event.getId() == null) {
            Log.e(TAG, mTAG + "Event or event ID is null");
            Library.showToast(getContext(), "Errore: evento non valido");
            return;
        }

        try {
            // Create intent for EventsActivity with editor navigation
            Intent eventsIntent = createEventEditorIntent(event, date);

            // Start activity
            if (getActivity() != null) {
                getActivity().startActivity(eventsIntent);
                Log.d(TAG, mTAG + "✅ EventsActivity started for event editor: " + event.getId());
            } else {
                Log.e(TAG, mTAG + "Activity is null, cannot start EventsActivity");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error opening event editor: " + e.getMessage());
            Library.showToast(getContext(), "Errore nell'apertura dell'editor evento");
        }
    }

    /**
     * 🆕 NEW: Open event editor for new event creation on specific date
     * This overload handles creation of new events from preview
     *
     * @param date The date for the new event
     */
    protected void openEventEditor(LocalDate date) {
        final String mTAG = "openEventEditor(date): ";
        Log.d(TAG, mTAG + "Opening editor for new event on date: " + date);

        try {
            // Create intent for EventsActivity with new event creation
            Intent eventsIntent = createNewEventEditorIntent(date);

            // Start activity
            if (getActivity() != null) {
                getActivity().startActivity(eventsIntent);
                Log.d(TAG, mTAG + "✅ EventsActivity started for new event creation on: " + date);
            } else {
                Log.e(TAG, mTAG + "Activity is null, cannot start EventsActivity");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error opening event editor for new event: " + e.getMessage());
            Library.showToast(getContext(), "Errore nell'apertura dell'editor per nuovo evento");
        }
    }

    /**
     * 🔄 ENHANCED: Confirm and delete event
     * Replaces mock implementation with proper dialog and database operation
     *
     * @param event The event to delete
     * @param date  The source date context
     */
    protected void confirmDeleteEvent(LocalEvent event, LocalDate date) {
        final String mTAG = "confirmDeleteEvent: ";
        Log.d(TAG, mTAG + "Confirming deletion for event: " + event.getTitle());

        if (event == null || event.getId() == null) {
            Log.e(TAG, mTAG + "Event or event ID is null");
            Library.showToast(getContext(), "Errore: evento non valido");
            return;
        }

        if (getContext() == null) {
            Log.e(TAG, mTAG + "Context is null, cannot show dialog");
            return;
        }

        // Create confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Elimina Evento")
                .setMessage("Sei sicuro di voler eliminare \"" + event.getTitle() + "\"?")
                .setIcon(R.drawable.ic_rounded_delete_24)
                .setPositiveButton("Elimina", (dialog, which) -> {
                    Log.d(TAG, mTAG + "User confirmed deletion");
                    performEventDeletion(event, date);
                })
                .setNegativeButton("Annulla", (dialog, which) -> {
                    Log.d(TAG, mTAG + "User cancelled deletion");
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * 🆕 NEW: Perform actual event deletion
     *
     * @param event The event to delete
     * @param date  The source date context
     */
    private void performEventDeletion(LocalEvent event, LocalDate date) {
        final String mTAG = "performEventDeletion: ";
        Log.d(TAG, mTAG + "Performing deletion for event: " + event.getId());

        // Use background thread for database operation
        CompletableFuture.runAsync(() -> {
            try {
                QDueDatabase database = QDueDatabase.getInstance(requireContext());
                EventDao eventDao = database.eventDao();
                eventDao.deleteEvent(event);

                Log.d(TAG, mTAG + "✅ Event deleted successfully: " + event.getId());

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Database error during deletion: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).thenRun(() -> {
            // Back to main thread for UI updates
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Library.showToast(getContext(), "Evento eliminato: " + event.getTitle());

                    // Refresh events in current view
                    onForceEventsRefresh();

                    Log.d(TAG, mTAG + "✅ UI updated after deletion");
                });
            }
        }).exceptionally(throwable -> {
            Log.e(TAG, mTAG + "Error during deletion: " + throwable.getMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Library.showToast(getContext(), "Errore durante l'eliminazione");
                });
            }
            return null;
        });
    }

    /**
     * 🔄 ENHANCED: Duplicate event with proper implementation
     * Replaces mock implementation with actual duplication logic
     *
     * @param event The event to duplicate
     * @param date  The source date context
     */
    protected void duplicateEvent(LocalEvent event, LocalDate date) {
        final String mTAG = "duplicateEvent: ";
        Log.d(TAG, mTAG + "Duplicating event: " + event.getTitle());

        if (event == null || event.getId() == null) {
            Log.e(TAG, mTAG + "Event or event ID is null");
            Library.showToast(getContext(), "Errore: evento non valido");
            return;
        }

        try {
            // Create duplicate with modified properties
            LocalEvent duplicatedEvent = createDuplicateEvent(event, date);

            // Save duplicate to database
            saveDuplicatedEvent(duplicatedEvent, event);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error duplicating event: " + e.getMessage());
            Library.showToast(getContext(), "Errore durante la duplicazione");
        }
    }

    /**
     * 🆕 NEW: Create duplicate event with modified properties
     *
     * @param originalEvent The original event to duplicate
     * @param targetDate    The target date for the duplicate
     * @return New LocalEvent with duplicated data
     */
    private LocalEvent createDuplicateEvent(LocalEvent originalEvent, LocalDate targetDate) {
        LocalEvent duplicate = new LocalEvent();

        // Generate new ID
        duplicate.setId(java.util.UUID.randomUUID().toString());

        // Copy basic properties
        duplicate.setTitle(originalEvent.getTitle() + " (Copia)");
        duplicate.setDescription(originalEvent.getDescription());
        duplicate.setLocation(originalEvent.getLocation());
        duplicate.setAllDay(originalEvent.isAllDay());

        // Adjust dates - maintain time but change date
        if (originalEvent.getStartTime() != null) {
            LocalDateTime originalStart = originalEvent.getStartTime();
            LocalDateTime newStart = targetDate.atTime(originalStart.toLocalTime());
            duplicate.setStartTime(newStart);
        }

        if (originalEvent.getEndTime() != null) {
            LocalDateTime originalEnd = originalEvent.getEndTime();
            // Calculate duration and apply to new date
            if (originalEvent.getStartTime() != null) {
                java.time.Duration duration = java.time.Duration.between(
                        originalEvent.getStartTime(), originalEvent.getEndTime());
                duplicate.setEndTime(duplicate.getStartTime().plus(duration));
            } else {
                LocalDateTime newEnd = targetDate.atTime(originalEnd.toLocalTime());
                duplicate.setEndTime(newEnd);
            }
        }

        // Copy other properties
        duplicate.setEventType(originalEvent.getEventType());
        duplicate.setPriority(originalEvent.getPriority());
        duplicate.setCustomProperties(originalEvent.getCustomProperties());
        duplicate.setPackageId(originalEvent.getPackageId());
        duplicate.setSourceUrl(originalEvent.getSourceUrl());
        duplicate.setPackageVersion(originalEvent.getPackageVersion());

        Log.d(TAG, "Created duplicate event: " + duplicate.getTitle() + " for date: " + targetDate);

        return duplicate;
    }

    /**
     * 🆕 NEW: Save duplicated event to database
     *
     * @param duplicatedEvent The duplicated event to save
     * @param originalEvent   The original event (for logging)
     */
    private void saveDuplicatedEvent(LocalEvent duplicatedEvent, LocalEvent originalEvent) {
        final String mTAG = "saveDuplicatedEvent: ";

        // Use background thread for database operation
        CompletableFuture.runAsync(() -> {
            try {
                QDueDatabase database = QDueDatabase.getInstance(requireContext());
                EventDao eventDao = database.eventDao();
                eventDao.insertEvent(duplicatedEvent);

                Log.d(TAG, mTAG + "✅ Duplicated event saved: " + duplicatedEvent.getId());

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Database error during duplication save: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).thenRun(() -> {
            // Back to main thread for UI updates
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Library.showToast(getContext(), "Evento duplicato: " + duplicatedEvent.getTitle());

                    // Refresh events in current view
                    onForceEventsRefresh();

                    Log.d(TAG, mTAG + "✅ UI updated after duplication");
                });
            }
        }).exceptionally(throwable -> {
            Log.e(TAG, mTAG + "Error during duplication save: " + throwable.getMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Library.showToast(getContext(), "Errore durante il salvataggio della copia");
                });
            }
            return null;
        });
    }

    /**
     * 🔄 ENHANCED: Toggle event completion status
     * Replaces mock implementation with actual status toggle
     *
     * @param event The event to toggle
     * @param date  The source date context
     */
    protected void toggleEventComplete(LocalEvent event, LocalDate date) {
        final String mTAG = "toggleEventComplete: ";
        Log.d(TAG, mTAG + "Toggling completion for event: " + event.getTitle());

        if (event == null || event.getId() == null) {
            Log.e(TAG, mTAG + "Event or event ID is null");
            Library.showToast(getContext(), "Errore: evento non valido");
            return;
        }

        // Check if event has custom properties for completion status
        Map<String, String> customProps = event.getCustomProperties();
        if (customProps == null) {
            customProps = new HashMap<>();
        }

        // Toggle completion status
        String currentStatus = customProps.get("completed");
        boolean isCompleted = "true".equals(currentStatus);
        boolean newStatus = !isCompleted;

        customProps.put("completed", String.valueOf(newStatus));
        customProps.put("completion_date", newStatus ? LocalDate.now().toString() : null);

        event.setCustomProperties(customProps);

        // Save updated event to database
        saveEventCompletionStatus(event, newStatus);
    }

    /**
     * 🆕 NEW: Save event completion status to database
     *
     * @param event       The event with updated completion status
     * @param isCompleted The new completion status
     */
    private void saveEventCompletionStatus(LocalEvent event, boolean isCompleted) {
        final String mTAG = "saveEventCompletionStatus: ";

        // Use background thread for database operation
        CompletableFuture.runAsync(() -> {
            try {
                QDueDatabase database = QDueDatabase.getInstance(requireContext());
                EventDao eventDao = database.eventDao();
                eventDao.updateEvent(event);

                Log.d(TAG, mTAG + "✅ Event completion status updated: " + event.getId() + " -> " + isCompleted);

            } catch (Exception e) {
                Log.e(TAG, mTAG + "Database error during completion update: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).thenRun(() -> {
            // Back to main thread for UI updates
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String statusMessage = isCompleted ? "completato" : "rimosso dai completati";
                    Library.showToast(getContext(), "Evento " + statusMessage + ": " + event.getTitle());

                    // Refresh events in current view
                    onForceEventsRefresh();

                    Log.d(TAG, mTAG + "✅ UI updated after completion toggle");
                });
            }
        }).exceptionally(throwable -> {
            Log.e(TAG, mTAG + "Error during completion update: " + throwable.getMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Library.showToast(getContext(), "Errore durante l'aggiornamento dello stato");
                });
            }
            return null;
        });
    }

    /**
     * Navigate to events activity
     */
    protected void navigateToEventsActivity(LocalDate date) {
        final String mTAG = "navigateToEventsActivity: ";
        Log.d(TAG, mTAG + "Opening EventsActivity for date: " + date);

        try {
            Intent eventsIntent = new Intent(getActivity(), EventsActivity.class);

            // General navigation (not direct to detail)
            eventsIntent.putExtra("action", "view_list");
            eventsIntent.putExtra("focus_date", date.toString());
            eventsIntent.putExtra("source_fragment", getFragmentName());

            if (getActivity() != null) {
                getActivity().startActivity(eventsIntent);
                Log.d(TAG, mTAG + "✅ EventsActivity started for general navigation");
            }

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error opening EventsActivity: " + e.getMessage());
            Library.showToast(getContext(), "Errore nell'apertura della lista eventi");
        }
    }

    /**
     * 🆕 HELPER: Create intent for EventsActivity with detail navigation parameters
     *
     * @param event The target event
     * @param date  The source date context
     * @return Configured Intent for EventsActivity
     */
    private Intent createEventDetailIntent(LocalEvent event, LocalDate date) {
        Intent eventsIntent = new Intent(getActivity(), EventsActivity.class);

        // Navigation action: direct to detail
        eventsIntent.putExtra("action", "view_detail");
        eventsIntent.putExtra("event_id", event.getId());

        // Source context for proper back navigation
        eventsIntent.putExtra("source_date", date.toString());
        eventsIntent.putExtra("source_fragment", getFragmentName());
        eventsIntent.putExtra("source_view_type", getEventsPreviewViewType().name());

        // Additional context that might be useful
        eventsIntent.putExtra("source_activity", "MainActivity");
        eventsIntent.putExtra("navigation_timestamp", System.currentTimeMillis());

        Log.d(TAG, "Created intent with parameters:");
        Log.d(TAG, "- action: view_detail");
        Log.d(TAG, "- event_id: " + event.getId());
        Log.d(TAG, "- source_date: " + date);
        Log.d(TAG, "- source_fragment: " + getFragmentName());
        Log.d(TAG, "- source_view_type: " + getEventsPreviewViewType().name());

        return eventsIntent;
    }

    /**
     * 🆕 NEW: Create intent for EventsActivity with event editor navigation parameters
     *
     * @param event The target event to edit
     * @param date  The source date context
     * @return Configured Intent for EventsActivity with editor parameters
     */
    private Intent createEventEditorIntent(LocalEvent event, LocalDate date) {
        Intent eventsIntent = new Intent(getActivity(), EventsActivity.class);

        // Navigation action: edit existing event
        eventsIntent.putExtra("action", "edit_event");
        eventsIntent.putExtra("event_id", event.getId());

        // Source context for proper back navigation
        eventsIntent.putExtra("source_date", date.toString());
        eventsIntent.putExtra("source_fragment", getFragmentName());
        eventsIntent.putExtra("source_view_type", getEventsPreviewViewType().name());

        // Additional context for editor
        eventsIntent.putExtra("source_activity", "MainActivity");
        eventsIntent.putExtra("edit_mode", "existing_event");
        eventsIntent.putExtra("navigation_timestamp", System.currentTimeMillis());

        // Event context for pre-population if needed
        eventsIntent.putExtra("event_title", event.getTitle());
        eventsIntent.putExtra("event_start_date", event.getStartTime() != null ?
                event.getStartTime().toLocalDate().toString() : date.toString());

        Log.d(TAG, "Created editor intent with parameters:");
        Log.d(TAG, "- action: edit_event");
        Log.d(TAG, "- event_id: " + event.getId());
        Log.d(TAG, "- source_date: " + date);
        Log.d(TAG, "- source_fragment: " + getFragmentName());
        Log.d(TAG, "- source_view_type: " + getEventsPreviewViewType().name());
        Log.d(TAG, "- edit_mode: existing_event");

        return eventsIntent;
    }

    /**
     * 🆕 NEW: Create intent for EventsActivity with new event creation parameters
     *
     * @param date The date for the new event
     * @return Configured Intent for EventsActivity with new event parameters
     */
    private Intent createNewEventEditorIntent(LocalDate date) {
        Intent eventsIntent = new Intent(getActivity(), EventsActivity.class);

        // Navigation action: create new event
        eventsIntent.putExtra("action", "create_event");
        eventsIntent.putExtra("target_date", date.toString());

        // Source context for proper back navigation
        eventsIntent.putExtra("source_date", date.toString());
        eventsIntent.putExtra("source_fragment", getFragmentName());
        eventsIntent.putExtra("source_view_type", getEventsPreviewViewType().name());

        // Additional context for editor
        eventsIntent.putExtra("source_activity", "MainActivity");
        eventsIntent.putExtra("edit_mode", "new_event");
        eventsIntent.putExtra("navigation_timestamp", System.currentTimeMillis());

        // Pre-population context
        eventsIntent.putExtra("default_start_date", date.toString());
        eventsIntent.putExtra("creation_source", "preview_add_button");

        Log.d(TAG, "Created new event editor intent with parameters:");
        Log.d(TAG, "- action: create_event");
        Log.d(TAG, "- target_date: " + date);
        Log.d(TAG, "- source_fragment: " + getFragmentName());
        Log.d(TAG, "- edit_mode: new_event");

        return eventsIntent;
    }

// ===========================================
// Public API Methods
// ===========================================

    /**
     * Enable/disable events preview
     */
    public void setEventsPreviewEnabled(boolean enabled) {
        mEventsPreviewEnabled = enabled;

        if (!enabled && mEventsPreviewManager != null) {
            mEventsPreviewManager.hideEventsPreview();
        }
    }

    /**
     * Check if events preview is enabled
     */
    public boolean isEventsPreviewEnabled() {
        return mEventsPreviewEnabled;
    }

    /**
     * Force hide events preview
     */
    public void hideEventsPreview() {
        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.hideEventsPreview();
        }
    }

    /**
     * Check if events preview is currently showing
     */
    public boolean isEventsPreviewShowing() {
        return mEventsPreviewManager != null && mEventsPreviewManager.isEventsPreviewShowing();
    }

    // ==================== NEW: Bulk Quick Events Creation ====================

//    /**
//     * ✅ ENHANCED: Handle bulk quick event creation with confirmation
//     * Now shows preview and confirmation for bulk operations
//     */
//    public void handleBulkQuickEventCreation(ToolbarAction action, Set<LocalDate> selectedDates) {
//        if (selectedDates == null || selectedDates.isEmpty()) {
//            Log.w(TAG, "No dates selected for bulk event creation");
//            return;
//        }
//
//        Log.d(TAG, "handleBulkQuickEventCreation: Creating " + action + " for " + selectedDates.size() + " dates");
//
//        if (!mQuickEventsInitialized) {
//            showQuickEventError("Sistema eventi rapidi non disponibile", action, null);
//            return;
//        }
//
//        QuickEventTemplate template = mQuickEventTemplates.get(action);
//        if (template == null) {
//            showQuickEventError("Template non trovato per " + action.getEventDisplayName(), action, null);
//            return;
//        }
//
//        // ✅ ENHANCED: Show bulk confirmation dialog with better preview
//        showBulkEventConfirmationDialog(action, selectedDates, template);
//    }

    /**
     * ✅ ENHANCED: Show confirmation dialog for bulk event creation with validation preview
     */
    private void showBulkEventConfirmationDialog(ToolbarAction action, Set<LocalDate> selectedDates, QuickEventTemplate template) {
        if (getContext() == null) return;

        String eventName = template.getDisplayName();
        int totalDates = selectedDates.size();
        int validDates = getValidDatesCount(action, selectedDates);
        int invalidDates = totalDates - validDates;

        // Build confirmation message with validation info
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("Creare evento '%s' per:", eventName));
        messageBuilder.append("\n\n");
        messageBuilder.append("✅ ").append(validDates).append(" date valide");

        if (invalidDates > 0) {
            messageBuilder.append("\n❌ ").append(invalidDates).append(" date non disponibili");
            messageBuilder.append("\n\nGli eventi saranno creati solo per le date valide.");
        }

        String title = String.format("Conferma %d eventi", validDates);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(messageBuilder.toString())
                .setPositiveButton("Crea", (dialog, which) -> {
                    if (validDates > 0) {
                        executeBulkEventCreation(action, selectedDates, template);
                    }
                })
                .setNegativeButton("Annulla", (dialog, which) -> {
                    trackQuickEventCreation(action, null, false);
                })
                .setIcon(template.getEventType().getEmoji().codePointAt(0)) // Use emoji as icon
                .show();
    }

    /**
     * ✅ ENHANCED: Execute bulk event creation with proper database integration
     * Now uses real EventDao for persistence
     */
    private void executeBulkEventCreation(ToolbarAction action, Set<LocalDate> selectedDates, QuickEventTemplate template) {
        Log.d(TAG, "executeBulkEventCreation: Creating events for " + selectedDates.size() + " dates");

        // Get database access
        EventDao eventDao = QDueDatabase.getInstance(getContext()).eventDao();

        // Execute in background thread
        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<LocalEvent> createdEvents = new ArrayList<>();

            for (LocalDate date : selectedDates) {
                try {
                    if (template.canUseOnDate(date, mCurrentUserId)) {
                        LocalEvent event = template.createEvent(date, mCurrentUserId);

                        if (event != null) {
                            // Save to database using EventDao
                            long rowId = eventDao.insertEvent(event);

                            if (rowId > 0) {
                                successCount++;
                                createdEvents.add(event);
                                Log.d(TAG, "✅ Bulk event created: " + event.getTitle() + " for " + date);
                            } else {
                                errorCount++;
                                errors.add("Errore salvataggio per " + date);
                            }
                        } else {
                            errorCount++;
                            errors.add("Errore creazione per " + date);
                        }
                    } else {
                        errorCount++;
                        errors.add("Non disponibile per " + date);
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Errore per " + date + ": " + e.getMessage());
                    Log.e(TAG, "Error creating bulk event for date " + date, e);
                }
            }

            final int finalSuccessCount = successCount;
            final int finalErrorCount = errorCount;
            final List<String> finalErrors = errors;

            // Switch back to main thread for UI updates
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Show results
                    showBulkEventResults(action, finalSuccessCount, finalErrorCount, errors);

                    // Refresh events if any were created successfully
                    if (finalSuccessCount > 0) {
                        onForceEventsRefresh();
                        exitSelectionMode(); // Exit selection mode after successful bulk creation

                        // Track successful bulk creation
                        trackQuickEventCreation(action, null, true);
                    } else {
                        // Track failed bulk creation
                        trackQuickEventCreation(action, null, false);
                    }
                });
            }
        });
    }

    /**
     * ✅ NEW: Show results of bulk event creation
     */
    private void showBulkEventResults(ToolbarAction action, int successCount, int errorCount, List<String> errors) {
        if (getContext() == null) return;

        String eventName = action.getEventDisplayName();

        if (errorCount == 0) {
            // All successful
            String message = String.format(QDue.getLocale(),
                    "✅ %d eventi '%s' creati con successo", successCount, eventName);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        } else if (successCount == 0) {
            // All failed
            String message = "❌ Nessun evento creato. Errori riscontrati.";
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        } else {
            // Mixed results
            String message = String.format(QDue.getLocale(),
                    "⚠️ %d eventi creati, %d errori", successCount, errorCount);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, String.format(QDue.getLocale(), "Bulk event results: %d success, %d errors for action %s",
                successCount, errorCount, action));
    }

    // ==================== NEW: Quick Events Validation ====================

    /**
     * ✅ NEW: Validate quick event creation before showing UI
     * Can be used by toolbars/adapters to enable/disable actions
     */
    public boolean canCreateQuickEvent(ToolbarAction action, LocalDate date) {
        if (!mQuickEventsInitialized) return false;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return false;

        return template.canUseOnDate(date, mCurrentUserId);
    }

    /**
     * ✅ NEW: Validate bulk quick event creation
     */
    public boolean canCreateBulkQuickEvents(ToolbarAction action, Set<LocalDate> dates) {
        if (!mQuickEventsInitialized || dates == null || dates.isEmpty()) return false;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return false;

        // Check if at least one date is valid
        return dates.stream().anyMatch(date -> template.canUseOnDate(date, mCurrentUserId));
    }

    /**
     * ✅ NEW: Get count of valid dates for bulk creation
     */
    public int getValidDatesCount(ToolbarAction action, Set<LocalDate> dates) {
        if (!mQuickEventsInitialized || dates == null) return 0;

        QuickEventTemplate template = mQuickEventTemplates.get(action);
        if (template == null) return 0;

        return (int) dates.stream()
                .filter(date -> template.canUseOnDate(date, mCurrentUserId))
                .count();
    }

    // ===========================================
    // Debug Methods Enhanced
    // ===========================================

    /**
     * Debug long-click integration
     */
    public void debugLongClickIntegration() {
        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " LONG-CLICK INTEGRATION DEBUG ===");

        debugSelectionState();

        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            // Call adapter debug if available
            try {
                java.lang.reflect.Method debugMethod = adapter.getClass().getMethod("debugSelectionState");
                debugMethod.invoke(adapter);
            } catch (Exception e) {
                Log.d(TAG, "Adapter debug method not available: " + e.getMessage());
            }
        }

        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " LONG-CLICK DEBUG ===");
    }

    /**
     * Enhanced debug with events preview state
     */
    public void debugSelectionState() {
        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " SELECTION STATE DEBUG ===");
        Log.d(TAG, "Fragment Selection Mode: " + isSelectionMode());

        BaseInteractiveAdapter adapter = getClickAdapter();
        if (adapter != null) {
            Log.d(TAG, "Adapter Selection Mode: " + adapter.isSelectionMode());
            Log.d(TAG, "Selected Count: " + adapter.getSelectedCount());
            Log.d(TAG, "Selected Dates: " + adapter.getSelectedDates());
            Log.d(TAG, "Has Active Selection: " + hasActiveSelection());

            // 🔧 NEW: Check for inconsistencies
            if (isSelectionMode() != adapter.isSelectionMode()) {
                Log.w(TAG, "⚠️ INCONSISTENCY: Fragment and Adapter selection mode mismatch!");
            }

            if (isSelectionMode() && adapter.getSelectedCount() == 0) {
                Log.w(TAG, "⚠️ WARNING: In selection mode but no items selected (should auto-exit)");
            }
        } else {
            Log.d(TAG, "Adapter: null");
        }

        Log.d(TAG, "FAB Visible: " + (mFabGoToToday != null && mFabGoToToday.getVisibility() == View.VISIBLE));
        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " SELECTION DEBUG ===");

        //super.debugSelectionState();

        Log.d(TAG, "=== " + getFragmentName().toUpperCase() + " EVENTS PREVIEW DEBUG ===");
        Log.d(TAG, "Events Preview Enabled: " + mEventsPreviewEnabled);

        if (mEventsPreviewManager != null) {
            mEventsPreviewManager.debugState();
        } else {
            Log.d(TAG, "Events Preview Manager: null");
        }

        Log.d(TAG, "=== END " + getFragmentName().toUpperCase() + " EVENTS PREVIEW DEBUG ===");
    }

    /**
     * ✅ NEW: Debug quick events system specifically
     */
    public void debugQuickEventsSystem() {
        Log.d(TAG, "=== QUICK EVENTS SYSTEM DEBUG ===");
        Log.d(TAG, "System Initialized: " + mQuickEventsInitialized);
        Log.d(TAG, "Templates Count: " + (mQuickEventTemplates != null ? mQuickEventTemplates.size() : 0));
        Log.d(TAG, "User Context: " + mCurrentUserId);

        if (mQuickEventTemplates != null) {
            Log.d(TAG, "Available Templates:");
            for (ToolbarAction action : mQuickEventTemplates.keySet()) {
                QuickEventTemplate template = mQuickEventTemplates.get(action);
                Log.d(TAG, "  " + action + " -> " + (template != null ? template.getDisplayName() : "NULL"));
            }
        }

        // Test validation for today
        LocalDate today = LocalDate.now();
        Log.d(TAG, "Validation test for today (" + today + "):");
        if (mQuickEventTemplates != null) {
            for (ToolbarAction action : ToolbarAction.getQuickEventActions()) {
                boolean canCreate = canCreateQuickEvent(action, today);
                Log.d(TAG, "  " + action + ": " + (canCreate ? "✅" : "❌"));
            }
        }

        Log.d(TAG, "=== END QUICK EVENTS DEBUG ===");
    }

    /**
     * Enhanced logging for editor operations
     *
     * @param operation The operation being performed
     * @param event     The target event (can be null for new events)
     * @param date      The date context
     */
    private void debugEditorOperation(String operation, LocalEvent event, LocalDate date) {
        Log.d(TAG, "=== EDITOR OPERATION ===");
        Log.d(TAG, "Operation: " + operation);
        Log.d(TAG, "Date: " + date);
        Log.d(TAG, "Event: " + (event != null ? event.getTitle() + " (" + event.getId() + ")" : "NEW"));
        Log.d(TAG, "Fragment: " + getFragmentName());
        Log.d(TAG, "View Type: " + getEventsPreviewViewType().name());
        Log.d(TAG, "========================");
    }
}