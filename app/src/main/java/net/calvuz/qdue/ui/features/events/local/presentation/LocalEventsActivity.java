package net.calvuz.qdue.ui.features.events.local.presentation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.events.models.LocalEvent;
import net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlerFactory;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlingModule;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LocalEvents Activity (MVVM)
 *
 * <p>Modern MVVM implementation of events management activity that replaces the traditional
 * EventsActivity with proper separation of concerns. This activity serves as the presentation
 * layer coordinator, managing ViewModels, navigation, and UI state while delegating
 * business logic to the ViewModel layer.</p>
 *
 * <h3>MVVM Architecture:</h3>
 * <ul>
 *   <li><strong>Model</strong>: LocalEvent domain models and repository data</li>
 *   <li><strong>View</strong>: Activity and Fragment UI components</li>
 *   <li><strong>ViewModel</strong>: LocalEventsViewModel and FileOperationsViewModel</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Clean DI through LocalEventsModule</li>
 *   <li><strong>Reactive UI</strong>: Observable state changes from ViewModels</li>
 *   <li><strong>Event Handling</strong>: Navigation and UI action events</li>
 *   <li><strong>Lifecycle Management</strong>: Proper ViewModel lifecycle coordination</li>
 *   <li><strong>Error Handling</strong>: Centralized error state management</li>
 * </ul>
 *
 * <h3>Replaced Functionality:</h3>
 * <p>This Activity replaces EventsActivity with the following improvements:</p>
 * <ul>
 *   <li>Business logic moved to ViewModels and Services</li>
 *   <li>Observable state management instead of manual UI updates</li>
 *   <li>Proper separation of file operations logic</li>
 *   <li>Clean dependency injection architecture</li>
 *   <li>Improved testability and maintainability</li>
 * </ul>
 *
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel
 * @see net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel
 * @see net.calvuz.qdue.ui.features.events.local.di.LocalEventsModule
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class LocalEventsActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = "LocalEventsActivity";

    // ==================== ACTIVITY RESULT CONSTANTS ====================

    public static final String EXTRA_EVENTS_CHANGED = "events_changed";
    public static final String EXTRA_EVENTS_COUNT = "events_count";
    public static final String EXTRA_CHANGE_TYPE = "change_type";
    public static final String EXTRA_FILTER_DATE = "filter_date";

    public static final String CHANGE_TYPE_IMPORT = "import";
    public static final String CHANGE_TYPE_EXPORT = "export";
    public static final String CHANGE_TYPE_DELETE = "delete";
    public static final String CHANGE_TYPE_CREATE = "create";
    public static final String CHANGE_TYPE_MODIFY = "modify";

    // ==================== DEPENDENCY INJECTION ====================

    private ServiceProvider mServiceProvider;
    private CalendarServiceProvider mCalendarServiceProvider;
    private LocalEventsModule mLocalEventsModule;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;
    private LocalEventsFileOperationsViewModel mFileOperationsViewModel;

    // ==================== UI COMPONENTS ====================

    private MaterialToolbar mToolbar;
    private FloatingActionButton mFabAddEvent;
    private View mLoadingStateView;
    private NavController mNavController;
    private NavHostFragment mNavHostFragment;

    // ==================== BACK HANDLING ====================

    private BackHandlingService mBackHandlingService;
    private BackHandlerFactory mBackHandlerFactory;

    // ==================== FILE OPERATIONS ====================

    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;

    // ==================== STATE MANAGEMENT ====================

    private boolean mIsInitialized = false;

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_events);

        Log.d(TAG, "Creating LocalEventsActivity (MVVM)");

        initializeViews();
        initializeDependencies();
        initializeViewModels();
        setupObservers();
        setupNavigation();
        setupBackHandling();
        setupFileOperations();
        setupToolbar();
        setupFab();

        handleIntentWithNavigation(getIntent());

        mIsInitialized = true;
        Log.d(TAG, "LocalEventsActivity initialization completed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying LocalEventsActivity (MVVM)");

        cleanup();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsInitialized && mEventsViewModel != null) {
            // Refresh events when returning to activity
            mEventsViewModel.refreshEvents();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentWithNavigation(intent);
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(@NonNull ServiceProvider serviceProvider) {
        Log.d(TAG, "Injecting dependencies");

        this.mServiceProvider = serviceProvider;
        this.mCalendarServiceProvider = serviceProvider.getCalendarServiceProvider();

        Log.d(TAG, "Dependencies injected successfully");
    }

    @Override
    public boolean areDependenciesReady() {
        return mServiceProvider != null && mCalendarServiceProvider != null;
    }

    // ==================== INITIALIZATION METHODS ====================

    private void initializeViews() {
        mToolbar = findViewById(R.id.toolbar_events);
        mFabAddEvent = findViewById(R.id.fab_add_event);
        mLoadingStateView = findViewById(R.id.loading_state_events);
    }

    private void initializeDependencies() {
        if (!areDependenciesReady()) {
            throw new IllegalStateException("Dependencies not injected. Call inject() first.");
        }

        // Initialize LocalEvents module with dependencies
        mLocalEventsModule = new LocalEventsModule(this, mCalendarServiceProvider);

        // Initialize services and ViewModels
        mLocalEventsModule
                .initializeServices()
                .initializeViewModels();

        // Initialize back handling
        mBackHandlingService = BackHandlingModule.getBackHandlingService(this);
        mBackHandlerFactory = BackHandlingModule.getBackHandlerFactory(this);

        Log.d(TAG, "All dependencies initialized");
    }

    private void initializeViewModels() {
        // Get ViewModels from module
        mEventsViewModel = mLocalEventsModule.getLocalEventsViewModel();

        try {
            mFileOperationsViewModel = mLocalEventsModule.getFileOperationsViewModel();
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "File operations not available: " + e.getMessage());
            mFileOperationsViewModel = null;
        }

        Log.d(TAG, "ViewModels initialized");
    }

    private void setupObservers() {
        setupEventsViewModelObservers();
        setupFileOperationsViewModelObservers();
    }

    private void setupEventsViewModelObservers() {
        if (mEventsViewModel == null) return;

        // State change observers
        mEventsViewModel.addStateChangeListener(LocalEventsViewModel.STATE_HAS_EVENTS,
                                                (key, value) -> updateFabVisibility((Boolean) value));

        mEventsViewModel.addStateChangeListener(LocalEventsViewModel.STATE_EVENTS_COUNT,
                                                (key, value) -> updateEventsCountDisplay((Integer) value));

        mEventsViewModel.addStateChangeListener(LocalEventsViewModel.STATE_SELECTION_MODE,
                                                (key, value) -> updateSelectionModeUI((Boolean) value));

        // Loading state observers
        mEventsViewModel.addLoadingStateListener(this::updateLoadingState);

        // Error state observers
        mEventsViewModel.addErrorStateListener(this::handleErrorState);

        // Event observers
        mEventsViewModel.addEventListener(this::handleViewModelEvent);

        Log.d(TAG, "Events ViewModel observers setup completed");
    }

    private void setupFileOperationsViewModelObservers() {
        if (mFileOperationsViewModel == null) return;

        // Loading state observers
        mFileOperationsViewModel.addLoadingStateListener(this::updateFileOperationLoadingState);

        // Error state observers
        mFileOperationsViewModel.addErrorStateListener(this::handleFileOperationErrorState);

        // Event observers
        mFileOperationsViewModel.addEventListener(this::handleFileOperationEvent);

        Log.d(TAG, "File operations ViewModel observers setup completed");
    }

    // ==================== NAVIGATION SETUP ====================

    private void setupNavigation() {
        try {
            mNavHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_events);

            if (mNavHostFragment != null) {
                mNavController = mNavHostFragment.getNavController();
                mNavController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    onNavigationDestinationChanged(destination.getId(), arguments);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation: " + e.getMessage());
        }
    }

    private void onNavigationDestinationChanged(int destinationId, Bundle arguments) {
        if (destinationId == R.id.nav_events_list) {
            updateFabVisibility();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.nav_events );
            }
        } else if (destinationId == R.id.nav_event_detail) {
            mFabAddEvent.hide();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.event_detail_title);
            }
        }
    }

    // ==================== BACK HANDLING SETUP ====================

    private void setupBackHandling() {
        mBackHandlerFactory.forComponent(this)
                .withPriority(10)
                .withDescription("LocalEventsActivity navigation handler")
                .register(this::handleActivityNavigation);
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getCurrentFragment();

        if (currentFragment != null) {
            boolean handled = mBackHandlingService.handleBackPress(currentFragment);
            if (handled) {
                return;
            }
        }

        boolean handled = mBackHandlingService.handleBackPress(this);
        if (!handled) {
            super.onBackPressed();
        }
    }

    private boolean handleActivityNavigation() {
        if (mEventsViewModel != null && mEventsViewModel.isSelectionMode()) {
            // Exit selection mode on back press
            mEventsViewModel.toggleSelectionMode();
            return true;
        }

        if (mNavController != null) {
            if (mNavController.popBackStack()) {
                return true;
            } else {
                finish();
                return true;
            }
        } else {
            finish();
            return true;
        }
    }

    private Fragment getCurrentFragment() {
        try {
            if (mNavHostFragment != null) {
                return mNavHostFragment.getChildFragmentManager()
                        .getPrimaryNavigationFragment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current fragment: " + e.getMessage());
        }
        return null;
    }

    // ==================== FILE OPERATIONS SETUP ====================

    private void setupFileOperations() {
        mFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null && mFileOperationsViewModel != null) {
                            mFileOperationsViewModel.importEventsFromFile(fileUri);
                        }
                    }
                });

        mFileSaverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null && mFileOperationsViewModel != null) {
                            // Check if we have selected events to export
                            if (mEventsViewModel != null && mEventsViewModel.getSelectedEventsCount() > 0) {
                                List<LocalEvent> selectedEvents = mEventsViewModel.getSelectedEvents();
                                mFileOperationsViewModel.exportSelectedEventsToFile(fileUri, selectedEvents);
                            } else {
                                mFileOperationsViewModel.exportAllEventsToFile(fileUri);
                            }
                        }
                    }
                });
    }

    // ==================== UI SETUP ====================

    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_events );
        }

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupFab() {
        mFabAddEvent.setOnClickListener(v -> {
            if (mEventsViewModel != null) {
                mEventsViewModel.navigateToCreateEvent();
            }
        });
        mFabAddEvent.hide();
    }

    // ==================== EVENT HANDLING ====================

    private void handleViewModelEvent(@NonNull BaseViewModel.ViewModelEvent event) {
        Log.d(TAG, "Handling ViewModel event: " + event.getType());

        if (event instanceof BaseViewModel.NavigationEvent) {
            handleNavigationEvent((BaseViewModel.NavigationEvent) event);
        } else if (event instanceof BaseViewModel.UIActionEvent) {
            handleUIActionEvent((BaseViewModel.UIActionEvent) event);
        }
    }

    private void handleNavigationEvent(@NonNull BaseViewModel.NavigationEvent event) {
        String destination = event.getDestination();
        Map<String, Object> arguments = event.getArguments();

        try {
            if (mNavController == null) return;

            Bundle args = new Bundle();
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    args.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    args.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    args.putLong(key, (Long) value);
                } else if (value instanceof Boolean) {
                    args.putBoolean(key, (Boolean) value);
                }
            }

            switch (destination) {
                case "event_detail":
                    mNavController.navigate(R.id.action_events_list_to_event_detail, args);
                    break;
                case "create_event":
                    //mNavController.navigate(R.id.action_local_events_list_to_create_event, args);
                    mNavController.navigate(R.id.action_events_list_to_event_edit, args);
                    break;
                case "edit_event":
                    //mNavController.navigate(R.id.action_event_detail_to_edit_event, args);
                    mNavController.navigate(R.id.action_event_detail_to_edit, args);
                    break;
                default:
                    Log.w(TAG, "Unknown navigation destination: " + destination);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            showError("Navigation error occurred");
        }
    }

    private void handleUIActionEvent(@NonNull BaseViewModel.UIActionEvent event) {
        String action = event.getAction();
        Map<String, Object> data = event.getData();

        switch (action) {
            case "SHOW_SUCCESS":
                String successMessage = (String) data.get("message");
                if (successMessage != null) {
                    showSuccess(successMessage);
                }
                break;
            case "SHOW_ERROR":
                String errorMessage = (String) data.get("message");
                if (errorMessage != null) {
                    showError(errorMessage);
                }
                break;
            case "REFRESH_EVENTS":
                handleEventsRefresh(data);
                break;
            default:
                Log.d(TAG, "Unhandled UI action: " + action);
                break;
        }
    }

    private void handleFileOperationEvent(@NonNull BaseViewModel.ViewModelEvent event) {
        if (!(event instanceof BaseViewModel.UIActionEvent)) return;

        BaseViewModel.UIActionEvent uiEvent = (BaseViewModel.UIActionEvent) event;
        String action = uiEvent.getAction();
        Map<String, Object> data = uiEvent.getData();

        switch (action) {
            case "IMPORT_SUCCESS":
                handleImportSuccess(data);
                break;
            case "IMPORT_FAILED":
                handleImportFailed(data);
                break;
            case "EXPORT_SUCCESS":
                handleExportSuccess(data);
                break;
            case "EXPORT_FAILED":
                handleExportFailed(data);
                break;
            case "REFRESH_EVENTS":
                if (mEventsViewModel != null) {
                    mEventsViewModel.refreshEvents();
                }
                break;
            default:
                Log.d(TAG, "Unhandled file operation event: " + action);
                break;
        }
    }

    // ==================== STATE UPDATE METHODS ====================

    private void updateFabVisibility() {
        if (mEventsViewModel != null) {
            updateFabVisibility(mEventsViewModel.hasEvents());
        }
    }

    private void updateFabVisibility(boolean hasEvents) {
        if (mNavController != null) {
            int currentDestination = mNavController.getCurrentDestination() != null ?
                    mNavController.getCurrentDestination().getId() : -1;

            if (currentDestination == R.id.nav_events_list) {
                if (hasEvents) {
                    mFabAddEvent.show();
                } else {
                    mFabAddEvent.hide();
                }
            } else {
                mFabAddEvent.hide();
            }
        }
    }

    private void updateEventsCountDisplay(int count) {
        // Update any UI components that display events count
        Log.d(TAG, "Events count updated: " + count);
    }

    private void updateSelectionModeUI(boolean selectionMode) {
        // Update UI for selection mode (could change toolbar, show/hide buttons, etc.)
        Log.d(TAG, "Selection mode: " + selectionMode);
    }

    private void updateLoadingState(@NonNull String operation, boolean loading) {
        if (LocalEventsViewModel.OP_LOAD_EVENTS.equals(operation)) {
            mLoadingStateView.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void updateFileOperationLoadingState(@NonNull String operation, boolean loading) {
        if (loading) {
            showGlobalLoading(true, "Processing file...");
        } else {
            showGlobalLoading(false, null);
        }
    }

    private void handleErrorState(@NonNull String operation, @Nullable String error) {
        if (error != null) {
            showError(error);
        }
    }

    private void handleFileOperationErrorState(@NonNull String operation, @Nullable String error) {
        if (error != null) {
            showError("File operation failed: " + error);
        }
    }

    // ==================== FILE OPERATION HANDLERS ====================

    private void handleImportSuccess(@NonNull Map<String, Object> data) {
        // Handle successful import
        showSuccess("Events imported successfully");

        if (mEventsViewModel != null) {
            mEventsViewModel.refreshEvents();
        }

        notifyEventsChanged(CHANGE_TYPE_IMPORT, 0);
    }

    private void handleImportFailed(@NonNull Map<String, Object> data) {
        showError("Import failed");
    }

    private void handleExportSuccess(@NonNull Map<String, Object> data) {
        showSuccess("Events exported successfully");
        notifyEventsChanged(CHANGE_TYPE_EXPORT, 0);
    }

    private void handleExportFailed(@NonNull Map<String, Object> data) {
        showError("Export failed");
    }

    private void handleEventsRefresh(@NonNull Map<String, Object> data) {
        String reason = (String) data.get("reason");
        Integer count = (Integer) data.get("importedCount");

        Log.d(TAG, "Events refresh requested: " + reason + " (count: " + count + ")");

        if (mEventsViewModel != null) {
            mEventsViewModel.refreshEvents();
        }
    }

    // ==================== INTENT HANDLING ====================

    private void handleIntentWithNavigation(Intent intent) {
        if (intent == null) return;

        String action = intent.getStringExtra("action");
        if (action != null) {
            switch (action) {
                case "view_detail":
                    handleDirectDetailNavigation(intent);
                    break;
                case "edit_event":
                    handleDirectEditorNavigation(intent);
                    break;
                case "create_event":
                    handleNewEventCreation(intent);
                    break;
                default:
                    handleIntent(intent);
                    break;
            }
        } else {
            handleIntent(intent);
        }
    }

    private void handleDirectDetailNavigation(Intent intent) {
        String eventId = intent.getStringExtra("event_id");
        if (eventId != null && mEventsViewModel != null) {
            mEventsViewModel.navigateToEventDetail(eventId);
        }
    }

    private void handleDirectEditorNavigation(Intent intent) {
        String eventId = intent.getStringExtra("event_id");
        if (eventId != null && mEventsViewModel != null) {
            mEventsViewModel.navigateToEditEvent(eventId);
        }
    }

    private void handleNewEventCreation(Intent intent) {
        if (mEventsViewModel != null) {
            mEventsViewModel.navigateToCreateEvent();
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri fileUri = intent.getData();
            if (fileUri != null && mFileOperationsViewModel != null) {
                mFileOperationsViewModel.importEventsFromFile(fileUri);
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null && mFileOperationsViewModel != null) {
                mFileOperationsViewModel.importEventsFromFile(sharedUri);
            }
        }
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Get the main events ViewModel.
     */
    @Nullable
    public LocalEventsViewModel getEventsViewModel() {
        return mEventsViewModel;
    }

    /**
     * Get the file operations ViewModel.
     */
    @Nullable
    public LocalEventsFileOperationsViewModel getFileOperationsViewModel() {
        return mFileOperationsViewModel;
    }

    /**
     * Trigger file import operation.
     */
    public void triggerFileImport() {
        if (mFileOperationsViewModel == null) {
            showError("File operations not available");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "application/qdue"});

        try {
            mFilePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker: " + e.getMessage());
            showError("Could not open file picker");
        }
    }

    /**
     * Trigger file export operation.
     */
    public void triggerFileExport() {
        triggerFileExport(null);
    }

    /**
     * Trigger file export operation with selected events.
     */
    public void triggerFileExport(@Nullable Set<String> selectedEventIds) {
        if (mFileOperationsViewModel == null) {
            showError("File operations not available");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "qdue_local_events_" + timestamp + ".qdue";
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        try {
            mFileSaverLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file saver: " + e.getMessage());
            showError("Could not open file saver");
        }
    }

    /**
     * Register unsaved changes handler for fragment.
     */
    public void registerFragmentUnsavedChanges(@NonNull Fragment fragment,
                                               @NonNull UnsavedChangesHandler handler) {
        mBackHandlerFactory.forComponent(fragment)
                .withPriority(100)
                .withDescription(fragment.getClass().getSimpleName() + " unsaved changes")
                .registerUnsavedChanges(handler);
    }

    /**
     * Register selection mode handler.
     */
    public void registerSelectionModeHandler(@NonNull Object component,
                                             @NonNull BackPressHandler selectionHandler) {
        mBackHandlerFactory.forComponent(component)
                .withPriority(50)
                .withDescription(component.getClass().getSimpleName() + " selection mode")
                .register(selectionHandler);
    }

    /**
     * Unregister back handler for component.
     */
    public void unregisterBackHandler(@NonNull Object component) {
        mBackHandlingService.unregisterComponent(component);
    }

    // ==================== UTILITY METHODS ====================

    private void showGlobalLoading(boolean show, @Nullable String message) {
        if (show) {
            mLoadingStateView.setVisibility(View.VISIBLE);
        } else {
            mLoadingStateView.setVisibility(View.GONE);
        }
    }

    private void showSuccess(@NonNull String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.success_color))
                .show();
    }

    private void showError(@NonNull String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.error_color))
                .show();
    }

    private void notifyEventsChanged(@NonNull String changeType, int eventCount) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EVENTS_CHANGED, true);
        resultIntent.putExtra(EXTRA_EVENTS_COUNT, eventCount);
        resultIntent.putExtra(EXTRA_CHANGE_TYPE, changeType);
        resultIntent.putExtra("timestamp", System.currentTimeMillis());

        if (mEventsViewModel != null) {
            resultIntent.putExtra("total_events", mEventsViewModel.getEventsCount());
        }

        setResult(RESULT_OK, resultIntent);
    }

    private void cleanup() {
        try {
            // Cleanup ViewModels
            if (mEventsViewModel != null) {
                mEventsViewModel.onDestroy();
            }

            if (mFileOperationsViewModel != null) {
                mFileOperationsViewModel.onDestroy();
            }

            // Cleanup module
            if (mLocalEventsModule != null) {
                mLocalEventsModule.cleanup();
            }

            // Unregister back handling
            if (mBackHandlingService != null) {
                mBackHandlingService.unregisterComponent(this);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Get debug information about activity state.
     */
    @NonNull
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("LocalEventsActivity{");
        info.append("initialized=").append(mIsInitialized);
        info.append(", hasEventsVM=").append(mEventsViewModel != null);
        info.append(", hasFileOpsVM=").append(mFileOperationsViewModel != null);
        info.append(", moduleReady=").append(mLocalEventsModule != null && mLocalEventsModule.isReady());

        if (mEventsViewModel != null) {
            info.append(", eventsCount=").append(mEventsViewModel.getEventsCount());
            info.append(", hasEvents=").append(mEventsViewModel.hasEvents());
            info.append(", selectionMode=").append(mEventsViewModel.isSelectionMode());
        }

        info.append("}");
        return info.toString();
    }
}