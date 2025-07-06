package net.calvuz.qdue.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.core.file.FileAccessAdapter;
import net.calvuz.qdue.core.file.EventsImportAdapter;
import net.calvuz.qdue.core.permissions.PermissionManager;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.core.backup.BackupIntegration;
import net.calvuz.qdue.events.imports.EventsImportManager;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.ui.events.interfaces.BackPressHandler;
import net.calvuz.qdue.core.listeners.EventDeletionListener;
import net.calvuz.qdue.core.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.core.interfaces.EventsOperationsInterface;
import net.calvuz.qdue.core.interfaces.EventsFileOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsUIStateInterface;
import net.calvuz.qdue.utils.Library;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * EventsActivity - Main activity for events management
 * <p>
 * Features:
 * - Navigation Component integration with EventsListFragment and EventDetailFragment
 * - File import/export functionality
 * - FAB for creating new events
 * - Responsive layout support (phone/tablet)
 * - Toolbar and navigation management
 * <p>
 * Navigation Flow:
 * EventsListFragment → EventDetailFragment → Back to List
 * <p>
 * Architecture:
 * - Uses Navigation Component with separate navigation graph
 * - Fragments handle their own UI and business logic
 * - Activity coordinates global actions (import/export/create)
 * <p>
 * * ACTIVITY RESULT PATTERN - COMPLETE IMPLEMENTATION
 * *
 * * This implementation provides a simple and reliable way to notify MainActivity
 * * when events are changed in EventsActivity using the standard Android
 * * Activity Result pattern.
 * *
 * * IMPLEMENTATION STEPS:
 * * 1. Add constants and notification method to EventsActivity
 * * 2. Update MainActivity to use ActivityResultLauncher
 * * 3. Add refresh logic to MainActivity
 * * 4. Call notification after event operations
 */
public class EventsActivity extends AppCompatActivity implements
        EventsFileOperationsInterface,
        EventsDatabaseOperationsInterface,
        EventsOperationsInterface,
        EventsUIStateInterface {

    private static final String TAG = "EventsActivity";

    // Navigation
    private NavController mNavController;
    private NavHostFragment mNavHostFragment;

    // UI Components
    private MaterialToolbar mToolbar;
    private FloatingActionButton mFabAddEvent;
    private View mLoadingStateView;

    // Empty State
    private boolean mHasEvents = false;
    private int mTotalEventsCount = 0;

    // File operation launchers
    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;

    // Current fragment reference for communication
    private EventsListFragment mCurrentListFragment;

    // CORREZIONE: Consistent database usage
    private QDueDatabase mDatabase;  // ✅ USA LO STESSO DATABASE

    // File Access
    private FileAccessAdapter mFileAccessAdapter;
    private EventsImportAdapter mEventsImportAdapter;
    private PermissionManager mPermissionManager;

    // Activity Result
    // Result constants for MainActivity communication
    public static final String EXTRA_EVENTS_CHANGED = "events_changed";
    public static final String EXTRA_EVENTS_COUNT = "events_count";
    public static final String EXTRA_CHANGE_TYPE = "change_type";

    // Change type constants
    public static final String CHANGE_TYPE_IMPORT = "import";
    public static final String CHANGE_TYPE_DELETE = "delete";
    public static final String CHANGE_TYPE_CREATE = "create";
    public static final String CHANGE_TYPE_MODIFY = "modify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        // Initialize database consistently
        mDatabase = QDueDatabase.getInstance(this);  // ✅ STESSO DATABASE

        initializeViews();
        setupToolbar();
        setupNavigation();
        setupFab();
        setupFileOperations();
        handleIntent(getIntent());

        // Set up interfaces
//        mEventsOperationListener = (EventsOperationListener) this;

        // 🆕 ENHANCED: Handle intent with direct navigation support
        handleIntentWithNavigation(getIntent());

        // Initialize events state
        checkInitialEventsState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ADD THESE LINES to your existing onDestroy:
        if (mFileAccessAdapter != null) {
            mFileAccessAdapter.clearPendingCallback();
        }
        if (mPermissionManager != null) {
            mPermissionManager.clearPendingCallbacks();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh events state when returning to activity
        checkInitialEventsState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Ensure we notify MainActivity of any pending changes when leaving
        if (mHasEvents) {
            // Could implement logic to detect if changes occurred during session
        }
    }

    @Override
    public void finish() {
        // Ensure final notification is sent before finishing
        Log.d(TAG, "Activity finishing - ensuring MainActivity notification");
        super.finish();
    }

    /**
     * Initialize view references
     */
    private void initializeViews() {
        mToolbar = findViewById(R.id.toolbar_events);
        mFabAddEvent = findViewById(R.id.fab_add_event);
        mLoadingStateView = findViewById(R.id.loading_state_events);
    }

    /**
     * Setup toolbar with navigation and menu
     */
    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_eventi);
        }

        // Handle navigation icon click (back to main activity)
        // CORREZIONE: Sostituire setNavigationOnClickListener con check del fragment
        mToolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "=== TOOLBAR NAVIGATION CLICKED ===");

            // NUOVO: Check se siamo in EventEditFragment
            if (mNavController != null && mNavController.getCurrentDestination() != null) {
                int currentDest = mNavController.getCurrentDestination().getId();
                Log.d(TAG, "Current destination: " + currentDest);

                if (currentDest == R.id.nav_event_edit) {
                    Log.d(TAG, "🎯 In EVENT EDIT - checking for unsaved changes");

                    try {
                        Fragment currentFragment = mNavHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (currentFragment instanceof BackPressHandler) {
                            Log.d(TAG, "🎯 Fragment implements BackPressHandler");
                            boolean handled = ((BackPressHandler) currentFragment).onBackPressed();
                            Log.d(TAG, "Fragment handled: " + handled);

                            if (handled) {
                                Log.d(TAG, "✋ Navigation BLOCKED by fragment");
                                return; // DON'T navigate - fragment is handling it
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking fragment: " + e.getMessage());
                    }
                }
            }

            Log.d(TAG, "🔄 Proceeding with normal navigation");

            // ESISTENTE: Check if we can navigate back within the events navigation
            if (mNavController != null && !mNavController.popBackStack()) {
                // If no back stack, finish activity to return to main
                finish();
            }
        });
    }

    /**
     * Setup Navigation Component
     */
    private void setupNavigation() {
        try {
            // Get NavHostFragment
            mNavHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_events);

            if (mNavHostFragment != null) {
                mNavController = mNavHostFragment.getNavController();

                // Add destination change listener for UI coordination
                mNavController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    onNavigationDestinationChanged(destination.getId(), arguments);
                });
                Log.d(TAG, "Navigation Component setup completed");
            } else {
                Log.e(TAG, "NavHostFragment not found!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation: " + e.getMessage());
        }
    }

    /**
     * Setup FloatingActionButton for creating new events
     */
    private void setupFab() {
        mFabAddEvent.setOnClickListener(v -> triggerCreateNewEvent());

        // Initially hide FAB - will be shown/hidden based on events state
        mFabAddEvent.hide();
        Log.d(TAG, "setupFab: completed (hidden)");
    }

    /**
     * Setup file operation launchers for import/export
     */
    private void setupFileOperations() {
        // File  Operation Managers and Adapters (transition)
        mFileAccessAdapter = new FileAccessAdapter(this);
        mEventsImportAdapter = new EventsImportAdapter(this);
        mPermissionManager = new PermissionManager(this);

        // File picker for JSON import
        mFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            importEventsFromFile(fileUri);
                        }
                    }
                });

        // File saver for JSON export
        mFileSaverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            exportEventsToFile(fileUri);
                        }
                    }
                });
    }

    /**
     * Handle navigation destination changes for UI coordination
     */
    private void onNavigationDestinationChanged(int destinationId, Bundle arguments) {
        Log.d(TAG, "Navigation destination changed to: " + destinationId);

        if (destinationId == R.id.nav_events_list) {
            // On events list - update FAB based on events availability
            updateFabVisibility();

            // On events list - show FAB and update fragment reference
//            mFabAddEvent.show();

            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.nav_eventi);
            }

            // CRITICAL: Update fragment reference AFTER navigation completes
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updateCurrentListFragmentReference();
            }, 100); // Small delay to ensure fragment is fully attached

        } else if (destinationId == R.id.nav_event_detail) {
            // On event detail - hide FAB
            mFabAddEvent.hide();

            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.event_detail_title);
            }
        }
    }

    /**
     * Update reference to current EventsListFragment
     */
    private void updateCurrentListFragmentReference() {
        try {
            if (mNavHostFragment != null) {
                // Get the current fragment from NavHostFragment
                var currentFragment = mNavHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                if (currentFragment instanceof EventsListFragment) {
                    mCurrentListFragment = (EventsListFragment) currentFragment;
                } else {
                    Log.w(TAG, "Primary navigation fragment is not EventsListFragment: " +
                            (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                    mCurrentListFragment = null;
                }
            } else {
                Log.w(TAG, "NavHostFragment is null, cannot update fragment reference");
                mCurrentListFragment = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating list fragment reference: " + e.getMessage());
            mCurrentListFragment = null;
        }
    }

    // ==================== ACTION HANDLERS ====================

    /**
     * Handle create new event action
     */
    public void handleCreateNewEvent() {
        showCreateEventDialog();
    }

    /**
     * Handle import events from file
     */
    @Deprecated
    public void handleImportEventsFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            mFilePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            Toast.makeText(this, "Errore nell'aprire il file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle Successful Import action
     */
    private void handleSuccessfulImport(EventsImportManager.ImportResult result) {
        showImportResultDialog(result);

        if (result.success && result.importedEvents > 0) {
            // Update fragment
            if (mCurrentListFragment != null) {
                mCurrentListFragment.refreshEvents();
            }

            // Update state and notify MainActivity
            mHasEvents = true;
            mTotalEventsCount += result.importedEvents;
            updateFabVisibility();
            notifyEventsChanged(CHANGE_TYPE_IMPORT, result.importedEvents);

            // TRIGGER AUTO BACKUP AFTER IMPORT
            BackupIntegration.integrateWithImport(
                    EventsActivity.this,
                    mCurrentListFragment != null ? mCurrentListFragment.getEventsList() : null,
                    result.importedEvents
            );

            Log.d(TAG, String.format(QDue.getLocale(),
                    "handleSuccessfulImport: ✅ Successfully imported %d events", result.importedEvents));
            Log.d(TAG, "handleSuccessfulImport: ✅ MainActivity notified");
        }
    }

    /**
     * Handle import events from URL
     */
    public void handleImportEventsFromUrl() {
        // TODO: to implement handleImportEventsFromUrl
        Log.d(TAG, "TODO: Handle import events from URL");
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        // Notify MainActivity
    }

    /**
     * Handle import events from URL
     */
    public void handleExportEventsToFile() {
        // TODO: to implement handleExportEventsToFile
        Log.d(TAG, "TODO: Handle export events to file");
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        // Notify MainActivity
    }

    /**
     * Handle Delete All Events action
     */
    void handleDeleteAllEvents() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Events")
                .setMessage("Are you sure you want to delete all local events? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> deleteAllEvents())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 🆕 NEW: Enhanced intent handling with direct navigation support
     * Supports both general navigation and direct detail navigation
     *
     * @param intent The intent that started this activity
     */
    private void handleIntentWithNavigation(Intent intent) {
        final String mTAG = "handleIntentWithNavigation: ";
        Log.d(TAG, mTAG + "Processing intent");

        if (intent == null) {
            Log.d(TAG, mTAG + "No intent provided, using default navigation");
            return;
        }

        String action = intent.getStringExtra("action");
        Log.d(TAG, mTAG + "Intent action: " + action);

        if ("view_detail".equals(action)) {
            // Direct navigation to event detail
            handleDirectDetailNavigation(intent);
        } else if ("edit_event".equals(action)) {
            // 🆕 NEW: Direct navigation to event editor
            handleDirectEditorNavigation(intent);
        } else if ("create_event".equals(action)) {
            // 🆕 NEW: Direct navigation to new event creation
            handleNewEventCreation(intent);
        } else if ("view_list".equals(action)) {
            // Navigation to events list with optional focus
            handleListNavigationWithFocus(intent);
        } else {
            // Default behavior - handle existing intent logic
            handleIntent(intent);
        }
    }

    /**
     * 🆕 NEW: Handle direct navigation to event detail
     * Creates the proper navigation stack: EventsList → EventDetail
     *
     * @param intent Intent containing event detail parameters
     */
    private void handleDirectDetailNavigation(Intent intent) {
        final String mTAG = "handleDirectDetailNavigation: ";

        String eventId = intent.getStringExtra("event_id");
        String sourceDate = intent.getStringExtra("source_date");
        String sourceFragment = intent.getStringExtra("source_fragment");
        String action = intent.getStringExtra("action");

        Log.d(TAG, mTAG + "Event ID: " + eventId);
        Log.d(TAG, mTAG + "Source Date: " + sourceDate);
        Log.d(TAG, mTAG + "Source Fragment: " + sourceFragment);

        if (eventId == null || eventId.trim().isEmpty()) {
            Log.e(TAG, mTAG + "Event ID is null or empty, cannot navigate to detail");
            Library.showToast(this, "Errore: ID evento non valido");
            return;
        }

        // Verify event exists before navigation
        verifyEventAndNavigate(eventId, sourceDate, sourceFragment, action);
    }

    /**
     * 🆕 NEW: Handle direct navigation to event editor
     * Creates the proper navigation stack: EventsList → EventEdit
     *
     * @param intent Intent containing event editor parameters
     */
    private void handleDirectEditorNavigation(Intent intent) {
        final String mTAG = "handleDirectEditorNavigation: ";

        String eventId = intent.getStringExtra("event_id");
        String sourceDate = intent.getStringExtra("source_date");
        String sourceFragment = intent.getStringExtra("source_fragment");
        String editMode = intent.getStringExtra("edit_mode");
        String action = intent.getStringExtra("action");

        Log.d(TAG, mTAG + "Event ID: " + eventId);
        Log.d(TAG, mTAG + "Source Date: " + sourceDate);
        Log.d(TAG, mTAG + "Source Fragment: " + sourceFragment);
        Log.d(TAG, mTAG + "Edit Mode: " + editMode);

        if (eventId == null || eventId.trim().isEmpty()) {
            Log.e(TAG, mTAG + "Event ID is null or empty, cannot navigate to editor");
            Library.showToast(this, "Errore: ID evento non valido");
            return;
        }

        // Verify event exists before navigation to editor
        verifyEventAndNavigate(eventId, sourceDate, sourceFragment, action);
    }

    /**
     * 🆕 NEW: Handle navigation to new event creation
     * Creates the proper navigation stack: EventsList → EventEdit (new)
     *
     * @param intent Intent containing new event creation parameters
     */
    private void handleNewEventCreation(Intent intent) {
        final String mTAG = "handleNewEventCreation: ";

        String targetDate = intent.getStringExtra("target_date");
        String sourceFragment = intent.getStringExtra("source_fragment");
        String editMode = intent.getStringExtra("edit_mode");

        Log.d(TAG, mTAG + "Target Date: " + targetDate);
        Log.d(TAG, mTAG + "Source Fragment: " + sourceFragment);
        Log.d(TAG, mTAG + "Edit Mode: " + editMode);

        if (targetDate == null || targetDate.trim().isEmpty()) {
            Log.e(TAG, mTAG + "Target date is null or empty, using today");
            targetDate = LocalDate.now().toString();
        }

        // Navigate directly to new event creation
        navigateToNewEventCreation(targetDate, sourceFragment);
    }

    // ==================== NAVIGATE FUNCTIONALITY ====================

    /**
     * 🆕 NEW: Verify event exists and navigate to detail
     *
     * @param eventId        The event ID to verify and navigate to
     * @param sourceDate     The source date context
     * @param sourceFragment The source fragment name
     */
    private void verifyEventAndNavigate(String eventId, String sourceDate, String sourceFragment) {
        final String mTAG = "verifyEventAndNavigate: ";

        // Use background thread for database operation
        CompletableFuture.supplyAsync(() -> {
            try {
                EventDao eventDao = mDatabase.eventDao();
                LocalEvent event = eventDao.getEventById(eventId);
                return event;
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Database error: " + e.getMessage());
                return null;
            }
        }).thenAccept(event -> {
            // Back to main thread for UI operations
            runOnUiThread(() -> {
                if (event != null) {
                    Log.d(TAG, mTAG + "✅ Event found: " + event.getTitle());
                    navigateToEventDetail(eventId, sourceDate, sourceFragment);
                } else {
                    Log.e(TAG, mTAG + "❌ Event not found with ID: " + eventId);
                    Library.showToast(this, "Evento non trovato");
                    // Fall back to events list
                    // (Navigation component will show EventsListFragment by default)
                }
            });
        }).exceptionally(throwable -> {
            Log.e(TAG, mTAG + "Error verifying event: " + throwable.getMessage());
            runOnUiThread(() -> Library.showToast(this, "Errore nel caricamento dell'evento"));
            return null;
        });
    }

    /**
     * 🆕 NEW: Verify event exists and navigate to detail
     *
     * @param eventId        The event ID to verify and navigate to
     * @param sourceDate     The source date context
     * @param sourceFragment The source fragment name
     */
    private void verifyEventAndNavigate(String eventId, String sourceDate, String sourceFragment, String action) {
        final String mTAG = "verifyEventAndNavigate: ";

        // Use background thread for database operation
        CompletableFuture.supplyAsync(() -> {
            try {
                EventDao eventDao = mDatabase.eventDao();
                LocalEvent event = eventDao.getEventById(eventId);
                return event;
            } catch (Exception e) {
                Log.e(TAG, mTAG + "Database error: " + e.getMessage());
                return null;
            }
        }).thenAccept(event -> {
            // Back to main thread for UI operations
            runOnUiThread(() -> {
                if (event != null) {
                    Log.d(TAG, mTAG + "✅ Event found: " + event.getTitle());
                    switch (action) {
                        case "edit_event":
                            navigateToEventEditor(eventId, sourceDate, sourceFragment);
                            break;
                        case "view_detail":
                            navigateToEventDetail(eventId, sourceDate, sourceFragment);
                            break;
                        default:
                            Log.e(TAG, mTAG + "❌ Invalid action: " + action);
                    }

                } else {
                    Log.e(TAG, mTAG + "❌ Event not found with ID: " + eventId);
                    Library.showToast(this, "Evento non trovato");
                    // Fall back to events list
                    // (Navigation component will show EventsListFragment by default)
                }
            });
        }).exceptionally(throwable -> {
            Log.e(TAG, mTAG + "Error verifying event: " + throwable.getMessage());
            runOnUiThread(() -> Library.showToast(this, "Errore nel caricamento dell'evento"));
            return null;
        });
    }

    /**
     * 🆕 NEW: Navigate to event detail with proper back stack
     * This ensures the back button goes: EventDetail → EventsList → MainActivity
     *
     * @param eventId        The event ID
     * @param sourceDate     The source date context
     * @param sourceFragment The source fragment name
     */
    private void navigateToEventDetail(String eventId, String sourceDate, String sourceFragment) {
        final String mTAG = "navigateToEventDetail: ";
        Log.d(TAG, mTAG + "Navigating to detail for event: " + eventId);

        if (mNavController == null) {
            Log.e(TAG, mTAG + "NavController is null, cannot navigate");
            return;
        }

        try {
            // Create bundle with event ID
            Bundle args = new Bundle();
            args.putString("eventId", eventId);

            // Add source context for potential future use
            args.putString("sourceDate", sourceDate);
            args.putString("sourceFragment", sourceFragment);

            // Navigate to detail fragment
            // This automatically creates the proper back stack:
            // EventDetailFragment (current) ← EventsListFragment (back) ← MainActivity (back)
            mNavController.navigate(R.id.action_events_list_to_event_detail, args);

            Log.d(TAG, mTAG + "✅ Navigation to event detail completed");
            Log.d(TAG, mTAG + "Back stack: EventDetail ← EventsList ← MainActivity");

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Navigation error: " + e.getMessage());
            Library.showToast(this, "Errore nella navigazione");
        }
    }

    /**
     * 🆕 NEW: Navigate to event editor with proper back stack
     * This ensures the back button goes: EventEdit → EventsList → MainActivity
     *
     * @param eventId        The event ID to edit
     * @param sourceDate     The source date context
     * @param sourceFragment The source fragment name
     */
    private void navigateToEventEditor(String eventId, String sourceDate, String sourceFragment) {
        final String mTAG = "navigateToEventEditor: ";
        Log.d(TAG, mTAG + "Navigating to editor for event: " + eventId);

        if (mNavController == null) {
            Log.e(TAG, mTAG + "NavController is null, cannot navigate");
            return;
        }

        try {
            // First navigate to detail fragment
            Bundle detailArgs = new Bundle();
            detailArgs.putString("eventId", eventId);
            detailArgs.putString("sourceDate", sourceDate);
            detailArgs.putString("sourceFragment", sourceFragment);

            mNavController.navigate(R.id.action_events_list_to_event_detail, detailArgs);

            // Then navigate to edit fragment
            Bundle editArgs = new Bundle();
            editArgs.putString("eventId", eventId);
            editArgs.putString("sourceDate", sourceDate);
            editArgs.putString("sourceFragment", sourceFragment);
            editArgs.putString("editMode", "existing_event");

            // Small delay to ensure detail fragment is loaded
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    mNavController.navigate(R.id.action_event_detail_to_edit, editArgs);
                    Log.d(TAG, mTAG + "✅ Navigation to event editor completed");
                    Log.d(TAG, mTAG + "Back stack: EventEdit ← EventDetail ← EventsList ← MainActivity");
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error navigating to edit: " + e.getMessage());
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Navigation error: " + e.getMessage());
            Library.showToast(this, "Errore nella navigazione all'editor");
        }
    }

    /**
     * 🆕 NEW: Navigate to new event creation
     *
     * @param targetDate     The target date for the new event
     * @param sourceFragment The source fragment name
     */
    private void navigateToNewEventCreation(String targetDate, String sourceFragment) {
        final String mTAG = "navigateToNewEventCreation: ";
        Log.d(TAG, mTAG + "Navigating to new event creation for date: " + targetDate);

        if (mNavController == null) {
            Log.e(TAG, mTAG + "NavController is null, cannot navigate");
            return;
        }

        try {
            // For new event creation, we might need a different approach
            // depending on how EventEditFragment handles new vs existing events

            // Option 1: Use existing edit fragment with special parameters
            Bundle args = new Bundle();
            args.putString("eventId", ""); // Empty ID indicates new event
            args.putString("targetDate", targetDate);
            args.putString("sourceFragment", sourceFragment);
            args.putString("editMode", "new_event");

            // Navigate directly to edit fragment for new event
            // Note: This may need adjustment based on actual EventEditFragment implementation
            Log.d(TAG, mTAG + "Creating new event for date: " + targetDate);
            Log.d(TAG, mTAG + "TODO: Verify EventEditFragment supports new event creation");

            // For now, show the events list (user can use FAB to create new event)
            Log.d(TAG, mTAG + "Showing events list - user can use FAB for new event");

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Navigation error: " + e.getMessage());
            Library.showToast(this, "Errore nella creazione nuovo evento");
        }
    }

    /**
     * 🆕 NEW: Handle navigation to events list with optional focus
     *
     * @param intent Intent containing list navigation parameters
     */
    private void handleListNavigationWithFocus(Intent intent) {
        final String mTAG = "handleListNavigationWithFocus: ";

        String focusDate = intent.getStringExtra("focus_date");
        String sourceFragment = intent.getStringExtra("source_fragment");

        Log.d(TAG, mTAG + "Focus Date: " + focusDate);
        Log.d(TAG, mTAG + "Source Fragment: " + sourceFragment);

        // Events list is the default destination, so no special navigation needed
        // Future enhancement: could scroll to events near focusDate
        Log.d(TAG, mTAG + "Showing events list (default destination)");
    }

    /**
     * 🆕 NEW: Enhanced onNewIntent to handle runtime navigation changes
     *
     * @param intent New intent received while activity is running
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(TAG, "onNewIntent: Handling new intent");
        setIntent(intent);

        // Handle new intent with navigation support
        handleIntentWithNavigation(intent);
    }

    // ==================== CREATE FUNCTIONALITY ====================

    /**
     * Show event creation dialog
     */
    private void showCreateEventDialog() {
        // Simple event creation for now
        EditText editTitle = new EditText(this);
        editTitle.setHint("Event title");
        editTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        editTitle.setSingleLine(true);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Create New Event")
                .setMessage("Enter event title:")
                .setView(editTitle)
                .setPositiveButton("Create", (dialog, which) -> {
                    Log.d(TAG, "showCreateEventDialog: New Event Title: " + editTitle.getText());
                    String title = editTitle.getText().toString().trim();
                    Log.d(TAG, "showCreateEventDialog: New Event Title: " + title);

                    if (!title.isEmpty()) {
                        createNewEvent(title);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createNewEvent(String title) {
        new Thread(() -> {
            try {
                LocalEvent newEvent = new LocalEvent(title, java.time.LocalDate.now());
                newEvent.setDescription("My Event");

                long result = mDatabase.eventDao().insertEvent(newEvent);

                runOnUiThread(() -> {
                    if (result > 0) {
                        Log.d(TAG, "createNewEvent: ✅ Event created (ID: " + newEvent.getId() +
                                ") (ROW ID: " + result + ")");

                        // Update fragment
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.onEventCreated(newEvent);
                        }

                        showSuccess("✅ " + title);

                        // Update state and notify MainActivity
                        mHasEvents = true;
                        mTotalEventsCount++;
                        updateFabVisibility();
                        notifyEventsChanged(CHANGE_TYPE_CREATE, 1);

                        Log.d(TAG, "createNewEvent: Event created, MainActivity notified");
                    } else {
                        showError("❌ Error creating event " + title);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "createNewEvent: ❌ Error creating event: " + e.getMessage());
                runOnUiThread(() -> {
                    showError("❌ Error creating event");
                });
            }
        }).start();
    }

    // ==================== IMPORT FUNCTIONALITY ====================

    /**
     * Import events from selected file URI
     */
    private void importEventsFromFile(Uri fileUri) {
        try {
            Log.d(TAG, "importEventsFromFile: Starting SAF-based import from: " + fileUri.toString());

            // Step 1: Show file info to user
            String fileInfo = mFileAccessAdapter.getFileDisplayInfo(fileUri);
            Log.d(TAG, "importEventsFromFile: Selected file: " + fileInfo);

            // Step 2: Check if supported file type
            if (!mFileAccessAdapter.isSupportedFile(fileUri)) {
                showError("❌ Unsupported file type. " + FileAccessAdapter.getSupportedFileTypesDescription());
                return;
            }

            // Step 3: Show import dialog with existing logic (no changes needed!)
            showImportDialog(fileUri);

        } catch (Exception e) {
            Log.e(TAG, "importEventsFromFile: ❌ Error starting import", e);
            showError("❌ Error reading file: " + e.getMessage());
        }

//        Log.d(TAG, "Importing events from: " + fileUri.toString());
//
//        try {
//            Log.d(TAG, "Starting enhanced import from file: " + fileUri.toString());
//
//            // Show format detection info
//            EventsImportManager.FileFormatInfo formatInfo = EventsImportManager.detectFileFormat(fileUri);
//            if (!formatInfo.supported) {
//                showError("Unsupported file format: " + formatInfo.description);
//                return;
//            }
//
//            // Show import dialog with options
//            showImportDialog(fileUri);
//
//            // Show import dialog with options
////            showAdvancedImportDialog(fileUri);
//
//        } catch (Exception e) {
//            showGlobalLoading(false, null);
//            Log.e(TAG, "Error importing events: " + e.getMessage());
//            Toast.makeText(this, "Errore durante l'import: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
    }

    /**
     * Import events from selected file URI
     */
    private void importEventsFromUrl(Uri fileUri) {
        // TODO: importEventsFromUrl(Uri fileUri) Integrate with existing import functionality
        Log.d(TAG, "Importing events from: " + fileUri.toString());
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show import options dialog to user
     */
    private void showImportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import, null);

        // Get UI elements from dialog layout
        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        RadioButton radioSkipDuplicates = dialogView.findViewById(R.id.radio_skip_duplicates);
        RadioButton radioReplaceExisting = dialogView.findViewById(R.id.radio_replace_existing);

        AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Import Options")
                .setView(dialogView)
                .setPositiveButton(R.string.text_import, null)  // Set later to prevent auto-close
                .setNegativeButton(R.string.text_cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button advancedImport = dialog.findViewById(R.id.btn_advanced_import);
            advancedImport.setOnClickListener((View v) -> {
                // Launch the Advanced Options Dialog
                dialog.dismiss();
                showAdvancedImportDialog(fileUri);
            });
            Button importButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            importButton.setOnClickListener(v -> {
                // Create import options from dialog settings
                EventsImportManager.ImportOptions options = createImportOptionsFromDialog(dialogView);

                Log.d(TAG, "showImportDialog: Advanced import options configured: " + getOptionsDescription(options));

                dialog.dismiss();
                performImport(fileUri, options);
            });
        });

        dialog.show();
    }

    /**
     * Show advanced import options dialog with complete UI implementation
     */
    private void showAdvancedImportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_advanced_import, null);

        // Get UI elements from dialog layout
        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        CheckBox checkboxShowWarnings = dialogView.findViewById(R.id.checkbox_show_warnings);

        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        RadioButton radioSkipDuplicates = dialogView.findViewById(R.id.radio_skip_duplicates);
        RadioButton radioReplaceExisting = dialogView.findViewById(R.id.radio_replace_existing);
        RadioButton radioRenameDuplicates = dialogView.findViewById(R.id.radio_rename_duplicates);

        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);
        CheckBox checkboxPreserveExisting = dialogView.findViewById(R.id.checkbox_preserve_existing);
        CheckBox checkboxAllowPartialImport = dialogView.findViewById(R.id.checkbox_allow_partial_import);

        // Set default values
        checkboxValidateBeforeImport.setChecked(true);
        checkboxShowWarnings.setChecked(true);
        radioSkipDuplicates.setChecked(true);
        checkboxShowProgress.setChecked(true);
        checkboxPreserveExisting.setChecked(true);
        checkboxAllowPartialImport.setChecked(true);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Advanced Import Options")
                .setView(dialogView)
                .setPositiveButton(R.string.text_import, null) // Set later to prevent auto-close
                .setNegativeButton(R.string.text_cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button importButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            importButton.setOnClickListener(v -> {
                // Create import options from dialog settings
                EventsImportManager.ImportOptions options = createImportOptionsFromAdvancedDialog(dialogView);

                Log.d(TAG, "showAdvancedImportDialog: Advanced import options configured: " + getOptionsDescription(options));

                dialog.dismiss();
                performImport(fileUri, options);
            });
        });

        dialog.show();
    }

    /**
     * Create ImportOptions from advanced dialog selections
     */
    private EventsImportManager.ImportOptions createImportOptionsFromDialog(View dialogView) {
        EventsImportManager.ImportOptions options = new EventsImportManager.ImportOptions();

        // Validation options
        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        options.validateBeforeImport = checkboxValidateBeforeImport.isChecked();

        // Conflict resolution
        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        int selectedRadioId = radioGroupConflictResolution.getCheckedRadioButtonId();

        if (selectedRadioId == R.id.radio_skip_duplicates) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.SKIP_DUPLICATE;
        } else if (selectedRadioId == R.id.radio_replace_existing) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.REPLACE_EXISTING;
        }

        return options;
    }

    /**
     * Create ImportOptions from advanced dialog selections
     */
    private EventsImportManager.ImportOptions createImportOptionsFromAdvancedDialog(View dialogView) {
        EventsImportManager.ImportOptions options = new EventsImportManager.ImportOptions();

        // Validation options
        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        options.validateBeforeImport = checkboxValidateBeforeImport.isChecked();

        // Progress reporting
        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);
        options.reportProgress = checkboxShowProgress.isChecked();

        // Preserve existing events
        CheckBox checkboxPreserveExisting = dialogView.findViewById(R.id.checkbox_preserve_existing);
        options.preserveExistingEvents = checkboxPreserveExisting.isChecked();

        // Allow partial import
        CheckBox checkboxAllowPartialImport = dialogView.findViewById(R.id.checkbox_allow_partial_import);
        // Note: This would need to be added to ImportOptions class if not present

        // Conflict resolution
        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        int selectedRadioId = radioGroupConflictResolution.getCheckedRadioButtonId();

        if (selectedRadioId == R.id.radio_skip_duplicates) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.SKIP_DUPLICATE;
        } else if (selectedRadioId == R.id.radio_replace_existing) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.REPLACE_EXISTING;
        } else if (selectedRadioId == R.id.radio_rename_duplicates) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.RENAME_DUPLICATE;
        }

        return options;
    }

    /**
     * Show validation preview before import
     */
    private void performValidationPreview(Uri fileUri) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Validating JSON file...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EventsImportManager importManager = new EventsImportManager(this);

        importManager.validateFileOnly(fileUri, new EventsImportManager.ValidationCallback() {
            @Override
            public void onValidationComplete(JsonSchemaValidator.ValidationResult result, EventPackageJson packageJson) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showValidationResultDialog(result, packageJson, fileUri);
                });
            }

            @Override
            public void onValidationError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Validation failed: " + error);
                });
            }
        });
    }

    /**
     * Show validation results to user
     */
    private void showValidationResultDialog(JsonSchemaValidator.ValidationResult result,
                                            EventPackageJson packageJson, Uri fileUri) {

        StringBuilder message = new StringBuilder();

        if (result.isValid) {
            message.append("✅ Validation PASSED\n\n");
            message.append("Package: ").append(packageJson.package_info.name).append("\n");
            message.append("Version: ").append(packageJson.package_info.version).append("\n");
            message.append("Events: ").append(packageJson.events.size()).append("\n");

            if (result.hasWarnings()) {
                message.append("\n⚠️ Warnings (").append(result.warnings.size()).append("):\n");
                for (String warning : result.warnings) {
                    message.append("• ").append(warning).append("\n");
                }
            }

            // Show proceed dialog
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Validation Results")
                    .setMessage(message.toString())
                    .setPositiveButton("Import Now", (dialog, which) ->
                            performImport(fileUri, EventsImportManager.createDefaultOptions()))
                    .setNeutralButton("Import Options", (dialog, which) ->
                            showImportDialog(fileUri))
                    .setNegativeButton("Cancel", null)
                    .show();

        } else {
            message.append("❌ Validation FAILED\n\n");
            message.append("Error: ").append(result.errorMessage).append("\n");

            if (!result.detailedErrors.isEmpty()) {
                message.append("\nDetailed errors:\n");
                for (String error : result.detailedErrors) {
                    message.append("• ").append(error).append("\n");
                }
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Validation Failed")
                    .setMessage(message.toString())
                    .setPositiveButton("Import Anyway", (dialog, which) -> {
                        EventsImportManager.ImportOptions permissiveOptions = EventsImportManager.createPermissiveOptions();
                        performImport(fileUri, permissiveOptions);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * Perform the actual import with progress feedback
     */
    private void performImport(Uri fileUri, EventsImportManager.ImportOptions options) {
        // Show progress dialog (your existing code)
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Importing Events");
        progressDialog.setMessage("Reading file...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Use SAF-based import adapter instead of direct EventsImportManager
        mEventsImportAdapter.importFromSAFFile(fileUri, options, new EventsImportManager.ImportCallback() {
            @Override
            public void onValidationComplete(JsonSchemaValidator.ValidationResult validationResult) {
                runOnUiThread(() -> {
                    if (validationResult.isValid) {
                        progressDialog.setMessage("Validation passed. Importing events...");
                    } else {
                        progressDialog.setMessage("Validation issues found. Continuing...");
                    }
                });
            }

            @Override
            public void onProgress(int processed, int total, String currentEvent) {
                runOnUiThread(() -> {
                    progressDialog.setMessage(String.format(QDue.getLocale(), "Importing... (%d/%d) %s",
                            processed, total, currentEvent));
                });
            }

            @Override
            public void onComplete(EventsImportManager.ImportResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    handleSuccessfulImport(result);
                });
            }

            @Override
            public void onError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Import failed: " + error);
                    Log.e(TAG, "performImport: ❌ Import error", exception);
                });
            }
        });
    }

    /**
     * Show detailed import results
     */
    private void showImportResultDialog(EventsImportManager.ImportResult result) {
        String title = result.success ? "Import Completed" : "Import Failed";

        StringBuilder message = new StringBuilder();
        message.append(result.getDetailedSummary());

        if (result.hasIssues()) {
            message.append("\n📋 Issues Found:\n");

            for (String warning : result.warnings) {
                message.append("⚠️ ").append(warning).append("\n");
            }

            for (String error : result.errors) {
                message.append("❌ ").append(error).append("\n");
            }
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message.toString())
                .setPositiveButton("OK", null);

        // Add action buttons based on result
        if (result.success && result.importedEvents > 0) {
            builder.setNeutralButton("View Events", (dialog, which) -> {
                // Scroll to imported events or highlight them
                Toast.makeText(this, "Showing " + result.importedEvents + " imported events", Toast.LENGTH_SHORT).show();
            });
        }

        if (result.hasIssues()) {
            builder.setNegativeButton("Export Log", (dialog, which) -> {
                // TODO: Export detailed log to file
                Toast.makeText(this, "Export log functionality coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    /**
     * Get human-readable description of import options for logging
     */
    private String getOptionsDescription(EventsImportManager.ImportOptions options) {
        StringBuilder desc = new StringBuilder();
        desc.append("Validation: ").append(options.validateBeforeImport ? "ON" : "OFF");
        desc.append(", Progress: ").append(options.reportProgress ? "ON" : "OFF");
        desc.append(", Preserve: ").append(options.preserveExistingEvents ? "ON" : "OFF");
        desc.append(", Conflicts: ").append(options.conflictResolution.name());
        return desc.toString();
    }

    /**
     * Handle export events to file
     */
    public void exportEventsToFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "eventi_export_" + System.currentTimeMillis() + ".json");

        try {
            mFileSaverLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file saver: " + e.getMessage());
            Toast.makeText(this, "Errore apertura file saver", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Export events to selected file URI
     */
    private void exportEventsToFile(Uri fileUri) {
        // TODO: Integrate with existing export functionality
        Log.d(TAG, "Exporting events to: " + fileUri.toString());

        try {
            showGlobalLoading(true, "Esportando eventi...");

            // TODO: Use existing ExportManager or similar for export
            // ExportManager.exportToUri(fileUri, new ExportCallback() {
            //     @Override
            //     public void onSuccess(int eventCount) {
            //         runOnUiThread(() -> {
            //             showGlobalLoading(false, null);
            //             Toast.makeText(EventsActivity.this,
            //                 "Esportati " + eventCount + " eventi", Toast.LENGTH_SHORT).show();
            //         });
            //     }
            //
            //     @Override
            //     public void onError(String error) {
            //         runOnUiThread(() -> {
            //             showGlobalLoading(false, null);
            //             Toast.makeText(EventsActivity.this,
            //                 "Errore export: " + error, Toast.LENGTH_LONG).show();
            //         });
            //     }
            // });

            // Placeholder implementation
            showGlobalLoading(false, null);
            Toast.makeText(this, "Export eventi - TODO: Integrare con ExportManager", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            showGlobalLoading(false, null);
            Log.e(TAG, "Error exporting events: " + e.getMessage());
            Toast.makeText(this, "Errore durante l'export: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle incoming intents (file opening, sharing)
     */
    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            // Handle file opening intent
            Uri fileUri = intent.getData();
            if (fileUri != null) {
                String scheme = fileUri.getScheme();
                if ("file".equals(scheme) || "content".equals(scheme)) {
                    Log.d(TAG, "Handling file intent: " + fileUri);
                    importEventsFromFile(fileUri);
                }
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            // Handle sharing intent
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) {
                Log.d(TAG, "Handling shared file: " + sharedUri);
                importEventsFromFile(sharedUri);
            }
        }
    }

    // ==================== DATABASE MANAGEMENT ====================

    /**
     * Clear all events Room database
     */
    private void deleteAllEvents() {
        new Thread(() -> {
            try {
                int result = mDatabase.eventDao().deleteAllEvents();

                runOnUiThread(() -> {
                    if (result > 0) {
                        // Update fragment
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.onEventsCleared(result, true);
                        }

                        showSuccess("All events cleared");

                        // Update state and notify MainActivity
                        mHasEvents = false;
                        mTotalEventsCount = 0;
                        updateFabVisibility();
                        notifyEventsChanged(CHANGE_TYPE_DELETE, result);

                        Log.d(TAG, "deleteAllEvents: ✅ MainActivity notified");
                    } else {
                        showError("❌ Error deleting all events");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "deleteAllEvents: ❌ Error clearing events from database", e);
                runOnUiThread(() -> {
                    showError("❌ Error deleting all events");
                });
            }
        }).start();
    }

    // ==================== UI STATE MANAGEMENT ====================

    /**
     * Show/hide global loading state
     */
    private void showGlobalLoading(boolean show, String message) {
        if (show) {
            mLoadingStateView.setVisibility(View.VISIBLE);

            // Update loading message if provided
            if (message != null) {
                // TODO: Update loading message in loading layout
                // TextView loadingDesc = mLoadingStateView.findViewById(R.id.tv_loading_description);
                // if (loadingDesc != null) {
                //     loadingDesc.setText(message);
                // }
            }
        } else {
            mLoadingStateView.setVisibility(View.GONE);
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.success_color))
                .show();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.error_color))
                .show();
    }

    // ==================== PUBLIC INTERFACE FOR FRAGMENTS ====================

    /**
     * Get NavController for fragment navigation
     */
    public NavController getNavController() {
        return mNavController;
    }

    /**
     * Trigger import from fragments
     */
    @Override
    public void triggerImportEventsFromFile() {
        // Use SAF-based file selection instead of traditional file picker
        mFileAccessAdapter.selectFile(new FileAccessAdapter.FileSelectionCallback() {
            @Override
            public void onFileSelected(@NonNull Uri fileUri) {
                // Delegate to existing import logic - zero changes needed!
                importEventsFromFile(fileUri);
            }

            @Override
            public void onSelectionError(@NonNull String error) {
                showError("File selection failed: " + error);
            }

            @Override
            public void onSelectionCancelled() {
                Log.d(TAG, "File selection cancelled");
                // No action needed - user cancelled
            }
        });

        // Old one
        //handleImportEventsFromFile();
    }

    /**
     * Trigger import from fragments
     */
    @Override
    public void triggerImportEventsFromUrl() {
        handleImportEventsFromUrl();
    }

    /**
     * Trigger export events to file
     */
    @Override
    public void triggerExportEventsToFile() { handleExportEventsToFile(); }

    // ==================== HELPER METHODS ====================

    /**
     * Create share text for event
     */
    private String createEventShareText(LocalEvent event) {
        StringBuilder text = new StringBuilder();
        text.append(event.getTitle()).append("\n\n");

        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            text.append(event.getDescription()).append("\n\n");
        }

        if (event.getStartTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            text.append("Inizio: ").append(event.getStartTime().format(formatter)).append("\n");
        }

        if (event.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            text.append("Fine: ").append(event.getEndTime().format(formatter)).append("\n");
        }

        if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
            text.append("Luogo: ").append(event.getLocation()).append("\n");
        }

        return text.toString();
    }

    /**
     * Format event date for dialog display
     */
    private String formatEventDateForDialog(LocalEvent event) {
        if (event.getStartTime() == null) {
            return "Data non specificata";
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        if (event.isAllDay()) {
            if (event.getEndTime() != null &&
                    !event.getStartTime().toLocalDate().equals(event.getEndTime().toLocalDate())) {
                return event.getStartTime().format(dateFormatter) +
                        " - " + event.getEndTime().format(dateFormatter);
            } else {
                return event.getStartTime().format(dateFormatter) + " (Tutto il giorno)";
            }
        } else {
            if (event.getEndTime() != null) {
                if (event.getStartTime().toLocalDate().equals(event.getEndTime().toLocalDate())) {
                    return event.getStartTime().format(dateTimeFormatter) +
                            " - " + event.getEndTime().format(timeFormatter);
                } else {
                    return event.getStartTime().format(dateTimeFormatter) +
                            " - " + event.getEndTime().format(dateTimeFormatter);
                }
            } else {
                return event.getStartTime().format(dateTimeFormatter);
            }
        }
    }

    /**
     * Trigger backup after successful deletion
     */
    private void triggerBackupAfterDeletion(String deletedEventTitle) {
        if (mCurrentListFragment != null) {
            // Use backup integration system
            BackupIntegration.integrateWithEventDeletion(
                    this,
                    mCurrentListFragment.getEventsList(),
                    deletedEventTitle // 1 event deleted
            );
        }
    }

    // ============================= TRIGGERS ==============================

    /**
     * Trigger delete all events from db
     */
    @Override
    public void triggerDeleteAllEvents() {
        handleDeleteAllEvents();
    }

    /**
     * Trigger create new event
     */
    @Override
    public void triggerCreateNewEvent() {
        handleCreateNewEvent();
    }

    /**
     * Trigger export from fragments
     */
    public void triggerExportEvents() {
        exportEventsToFile();
    }

    // ==================== IMPLEMENT EventsOperationsInterface ====================

    @Override
    public void triggerEventDeletion(LocalEvent event, EventDeletionListener listener) {
        if (event == null || event.getId() == null) {
            listener.onDeletionCompleted(false, "Evento non valido");
            return;
        }

        // Show confirmation dialog
        showEventDeletionDialog(event, listener);
    }

    @Override
    public void triggerEventEdit(LocalEvent event) {
        // Navigate to edit fragment
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());

        try {
            if (mNavController != null) {
                mNavController.navigate(R.id.action_event_detail_to_edit, args);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to edit: " + e.getMessage());
            Toast.makeText(this, "Errore navigazione edit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void triggerEventDuplicate(LocalEvent event) {
        // TODO: Implement event duplication
        Toast.makeText(this, "Duplica evento - TODO", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void triggerEventShare(LocalEvent event) {
        // Create share intent
        String shareText = createEventShareText(event);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Evento: " + event.getTitle());

        try {
            startActivity(Intent.createChooser(shareIntent, "Condividi Evento"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing event: " + e.getMessage());
            Toast.makeText(this, "Errore condivisione", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void triggerAddToCalendar(LocalEvent event) {
        // Add to system calendar
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, event.getTitle());
        intent.putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription());
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
        intent.putExtra(CalendarContract.Events.ALL_DAY, event.isAllDay());

        if (event.getStartTime() != null) {
            long startMillis = event.getStartTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
        }

        if (event.getEndTime() != null) {
            long endMillis = event.getEndTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error adding to calendar: " + e.getMessage());
            Toast.makeText(this, "Impossibile aprire il calendario", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== DELETION IMPLEMENTATION ====================

    /**
     * Show deletion confirmation dialog
     */
    private void showEventDeletionDialog(LocalEvent event, EventDeletionListener listener) {
        String dialogMessage = String.format(
                getString(R.string.dialog_event_delete_confirmation),
                event.getTitle(),
                formatEventDateForDialog(event)
        );

        new AlertDialog.Builder(this)
                .setTitle("Elimina Evento")
                .setMessage(dialogMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.text_delete, (dialog, which) -> {
                    // User confirmed deletion
                    startPendingEventDeletion(event);
                    listener.onDeletionRequested();
                })
                .setNegativeButton(R.string.text_cancel, (dialog, which) -> {
                    // User cancelled deletion
                    listener.onDeletionCancelled();
                })
                .show();
    }

    /**
     * Start pending deletion with undo option
     */
    private void startPendingEventDeletion(LocalEvent event) {
        String eventTitle = event.getTitle();
        String eventId = event.getId();

        // STEP 1: Add to pending deletion in fragment
        if (mCurrentListFragment != null) {
            mCurrentListFragment.addToPendingDeletion(eventId);
            mCurrentListFragment.suppressRefresh(true); // Suppress auto-refresh
        }

        // STEP 2: Remove event from UI immediately (soft delete)
        removeEventFromList(eventId);

        // STEP 3: Show undo snackbar
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content), eventTitle + " " + R.string.text_deleted,
                Snackbar.LENGTH_LONG // 4 seconds
        );

        // Add undo action
        snackbar.setAction(R.string.text_capital_cancel, v -> {
            cancelEventDeletion(event);
            Log.d(TAG, "startPendingEventDeletion: Event deletion cancelled by user: " + eventTitle);
        });

        // Configure snackbar appearance
        snackbar.setBackgroundTint(getColor(R.color.warning_color));
        snackbar.setActionTextColor(getColor(android.R.color.white));

        // Set callback for when snackbar disappears
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);

                // If dismissed without undo action, perform actual deletion
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    Log.d(TAG, "startPendingEventDeletion: Snackbar dismissed, performing actual deletion: " + eventTitle);
                    performActualEventDeletion(eventId, eventTitle);
                }
            }
        });

        // Show the snackbar
        snackbar.show();
    }

    /**
     * Cancel pending deletion and restore event
     */
    private void cancelEventDeletion(LocalEvent event) {
        String eventId = event.getId();

        // Remove from pending deletion and restore event to list
        if (mCurrentListFragment != null) {
            mCurrentListFragment.removeFromPendingDeletion(eventId);
            mCurrentListFragment.suppressRefresh(false); // Re-enable refresh
            mCurrentListFragment.addEvent(event); // Add back to UI
        }

        // Show confirmation
        Toast.makeText(this, "Eliminazione annullata", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Event deletion cancelled: " + event.getTitle());
    }

    /**
     * Perform actual deletion from database
     */
    private void performActualEventDeletion(String eventId, String eventTitle) {
        new Thread(() -> {
            try {
                EventDao eventDao = mDatabase.eventDao();
                int deletedRows = eventDao.deleteEventById(eventId);

                runOnUiThread(() -> {
                    // Always remove from pending deletion
                    if (mCurrentListFragment != null) {
                        mCurrentListFragment.removeFromPendingDeletion(eventId);
                        mCurrentListFragment.suppressRefresh(false);
                    }

                    if (deletedRows > 0) {
                        Log.d(TAG, "Event permanently deleted: " + eventTitle + " (ID: " + eventId + ")");
                        showSuccess("Event deleted permanently");

                        // Update state
                        mTotalEventsCount = Math.max(0, mTotalEventsCount - 1);
                        mHasEvents = mTotalEventsCount > 0;
                        updateFabVisibility();

                        // Notify MainActivity
                        notifyEventsChanged(CHANGE_TYPE_DELETE, 1);

                        // Trigger backup after deletion
                        triggerBackupAfterDeletion(eventTitle);
                    } else {
                        Log.w(TAG, "No rows deleted for event ID: " + eventId);
                        showError("Event not found in database");

                        // Since deletion failed, force refresh to restore correct state
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.refreshEvents();
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting event: " + e.getMessage());
                runOnUiThread(() -> {
                    // Remove from pending and restore correct state
                    if (mCurrentListFragment != null) {
                        mCurrentListFragment.removeFromPendingDeletion(eventId);
                        mCurrentListFragment.suppressRefresh(false);
                        mCurrentListFragment.refreshEvents();
                    }
                    showError("Error during deletion: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Remove event from list (after deletion)
     */
    public void removeEventFromList(String eventId) {
        if (mCurrentListFragment != null && eventId != null) {
            mCurrentListFragment.removeEvent(eventId);
        } else {
            Log.w(TAG, "Error removing event from list: eventId is null");
        }
    }

    /**
     * Update event in list (after editing)
     */
    public void updateEventInList(LocalEvent event) {
        if (mCurrentListFragment != null && event != null) {
            mCurrentListFragment.updateEvent(event);
        }
    }

    // ====================== PERMISSIONS RELATED ==========================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // ADD THIS LINE to your existing onActivityResult:
        mFileAccessAdapter.onActivityResult(requestCode, resultCode, data);

        // ... your existing onActivityResult code remains unchanged ...
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // ADD THIS LINE to your existing onRequestPermissionsResult:
        mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // ... your existing permission handling code remains unchanged ...
    }

    // ==================== CORREZIONE 1: INTERFACE IMPLEMENTATION ====================

    /**
     * Implementation of EventsUIStateInterface
     * Called by EventsListFragment to control FAB visibility
     */
    @Override
    public void onEventsListStateChanged(boolean hasEvents) {
        Log.d(TAG, "onEventsListStateChanged called - hasEvents: " + hasEvents);
        mHasEvents = hasEvents;
        updateFabVisibility();

        // Update internal state tracking
        if (hasEvents) {
            updateEventsCount();
        } else {
            mTotalEventsCount = 0;
        }
    }

    /**
     * Update events count for better state tracking
     */
    private void updateEventsCount() {
        new Thread(() -> {
            try {
                int count = mDatabase.eventDao().getEventsCount();
                runOnUiThread(() -> {
                    mTotalEventsCount = count;
                    Log.d(TAG, "Updated events count: " + count);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating events count", e);
            }
        }).start();
    }

    /**
     * Check initial events state on activity start
     */
    private void checkInitialEventsState() {
        new Thread(() -> {
            try {
                int count = mDatabase.eventDao().getEventsCount();
                runOnUiThread(() -> {
                    mHasEvents = count > 0;
                    mTotalEventsCount = count;
                    updateFabVisibility();
                    Log.d(TAG, "Initial events state: " + count + " events, hasEvents: " + mHasEvents);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking initial events state", e);
            }
        }).start();
    }

    /**
     * Update FAB visibility based on current navigation destination and events availability
     */
    private void updateFabVisibility() {
        if (mNavController != null) {
            int currentDestination = mNavController.getCurrentDestination() != null ?
                    mNavController.getCurrentDestination().getId() : -1;

            if (currentDestination == R.id.nav_events_list) {
                if (mHasEvents) {
                    mFabAddEvent.show();
                    Log.d(TAG, "FAB shown - events list with events present");
                } else {
                    mFabAddEvent.hide();
                    Log.d(TAG, "FAB hidden - events list is empty");
                }
            } else {
                // Other destinations (event detail, edit) - always hide FAB
                mFabAddEvent.hide();
                Log.d(TAG, "FAB hidden - not on events list");
            }
        }
    }

// ==================== CORREZIONE 2: ENHANCED NOTIFICATION SYSTEM ====================

    /**
     * Enhanced notification system with better validation
     */
    private void notifyEventsChanged(String changeType, int eventCount) {
        Log.d(TAG, String.format(QDue.getLocale(),
                "Notifying events changed: %s (%d events)", changeType, eventCount));

        // Validate parameters
        if (changeType == null) {
            Log.w(TAG, "Change type is null, using default");
            changeType = CHANGE_TYPE_MODIFY;
        }

        if (eventCount < 0) {
            Log.w(TAG, "Invalid event count: " + eventCount);
            eventCount = 0;
        }

        // Create result intent with enhanced data
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EVENTS_CHANGED, true);
        resultIntent.putExtra(EXTRA_EVENTS_COUNT, eventCount);
        resultIntent.putExtra(EXTRA_CHANGE_TYPE, changeType);

        // Add additional metadata
        resultIntent.putExtra("timestamp", System.currentTimeMillis());
        resultIntent.putExtra("total_events", mTotalEventsCount);

        setResult(RESULT_OK, resultIntent);

        Log.d(TAG, String.format(QDue.getLocale(),
                "Set activity result: %s, count=%d, total=%d",
                changeType, eventCount, mTotalEventsCount));
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method to check activity state
     */
    public void debugActivityState() {
        Log.d(TAG, "=== EVENTS ACTIVITY STATE DEBUG ===");
        Log.d(TAG, "Has Events: " + mHasEvents);
        Log.d(TAG, "Total Events Count: " + mTotalEventsCount);
        Log.d(TAG, "Database: " + (mDatabase != null ? "initialized" : "null"));
        Log.d(TAG, "Current Fragment: " + (mCurrentListFragment != null ? "available" : "null"));
        Log.d(TAG, "FAB Visibility: " + (mFabAddEvent != null ?
                (mFabAddEvent.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN") : "null"));

        if (mNavController != null && mNavController.getCurrentDestination() != null) {
            Log.d(TAG, "Current Destination: " + mNavController.getCurrentDestination().getId());
        }

        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * Debug method to force notification test
     */
    public void debugForceNotification(String testChangeType, int testEventCount) {
        Log.d(TAG, "=== DEBUG FORCE NOTIFICATION ===");
        Log.d(TAG, "Test notification: " + testChangeType + " (" + testEventCount + " events)");
        notifyEventsChanged(testChangeType, testEventCount);
        Log.d(TAG, "=== END DEBUG FORCE NOTIFICATION ===");
    }

    /**
     * 🔄 ENHANCED: Debug method with editor navigation info
     */
    public void debugNavigationState() {
        Log.d(TAG, "=== EVENTS ACTIVITY NAVIGATION DEBUG ===");
        Log.d(TAG, "NavController: " + (mNavController != null ? "available" : "null"));
        Log.d(TAG, "Database: " + (mDatabase != null ? "available" : "null"));

        if (mNavController != null) {
            try {
                Log.d(TAG, "Current Destination: " + mNavController.getCurrentDestination());
            } catch (Exception e) {
                Log.e(TAG, "Error getting current destination: " + e.getMessage());
            }
        }

        // Log intent parameters if available
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "Current Intent Action: " + intent.getStringExtra("action"));
            Log.d(TAG, "Current Intent Event ID: " + intent.getStringExtra("event_id"));
            Log.d(TAG, "Current Intent Target Date: " + intent.getStringExtra("target_date"));
        }

        Log.d(TAG, "=== END NAVIGATION DEBUG ===");
    }
}