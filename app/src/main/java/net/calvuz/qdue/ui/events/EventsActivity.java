package net.calvuz.qdue.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

/**
 * EventsActivity - Main activity for events management
 *
 * Features:
 * - Navigation Component integration with EventsListFragment and EventDetailFragment
 * - File import/export functionality
 * - FAB for creating new events
 * - Responsive layout support (phone/tablet)
 * - Toolbar and navigation management
 *
 * Navigation Flow:
 * EventsListFragment → EventDetailFragment → Back to List
 *
 * Architecture:
 * - Uses Navigation Component with separate navigation graph
 * - Fragments handle their own UI and business logic
 * - Activity coordinates global actions (import/export/create)
 */
public class EventsActivity extends AppCompatActivity {

    private static final String TAG = "EventsActivity";

    // Navigation
    private NavController mNavController;
    private NavHostFragment mNavHostFragment;

    // UI Components
    private MaterialToolbar mToolbar;
    private FloatingActionButton mFabAddEvent;
    private View mEmptyStateView;
    private View mLoadingStateView;

    // File operation launchers
    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;

    // Current fragment reference for communication
    private EventsListFragment mCurrentListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        initializeViews();
        setupToolbar();
        setupNavigation();
        setupFab();
        setupFileOperations();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Initialize view references
     */
    private void initializeViews() {
        mToolbar = findViewById(R.id.toolbar_events);
        mFabAddEvent = findViewById(R.id.fab_add_event);
        mEmptyStateView = findViewById(R.id.empty_state_events);
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
        mToolbar.setNavigationOnClickListener(v -> {
            // Check if we can navigate back within the events navigation
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
        mFabAddEvent.setOnClickListener(v -> handleCreateNewEvent());

        // Initially show FAB (will be controlled by destination changes)
        mFabAddEvent.show();
    }

    /**
     * Setup file operation launchers for import/export
     */
    private void setupFileOperations() {
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
            // On events list - show FAB
            mFabAddEvent.show();

            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.nav_eventi);
            }

            // Get reference to current list fragment for communication
            updateCurrentListFragmentReference();

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
                    Log.d(TAG, "Updated EventsListFragment reference");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating list fragment reference: " + e.getMessage());
        }
    }

    // ==================== MENU HANDLING ====================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle toolbar navigation
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Try to navigate back within the events navigation
        if (mNavController != null && mNavController.popBackStack()) {
            // Successfully navigated back within navigation graph
            return;
        }

        // No back stack, finish activity
        super.onBackPressed();
    }

    // ==================== ACTION HANDLERS ====================

    /**
     * Handle create new event action
     */
    private void handleCreateNewEvent() {
        // TODO: Implement event creation dialog or navigate to creation fragment
        showCreateEventDialog();
    }

    /**
     * Show create event dialog
     */
    private void showCreateEventDialog() {
        // TODO: Implement actual event creation dialog
        Toast.makeText(this, "Creazione nuovo evento - TODO", Toast.LENGTH_SHORT).show();

        // Placeholder: After event creation, add to list
        // LocalEvent newEvent = createEventFromDialog();
        // if (mCurrentListFragment != null) {
        //     mCurrentListFragment.addEvent(newEvent);
        // }
    }

    /**
     * Handle import events from file
     */
    public void importEventsFromFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            mFilePickerLauncher.launch(Intent.createChooser(intent, "Seleziona file JSON eventi"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker: " + e.getMessage());
            Toast.makeText(this, "Errore apertura file picker", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Import events from selected file URI
     */
    private void importEventsFromFile(Uri fileUri) {
        // TODO: Integrate with existing import functionality
        Log.d(TAG, "Importing events from: " + fileUri.toString());

        try {
            // Show loading state
            showGlobalLoading(true, "Importando eventi...");

            // TODO: Use existing EventPackageManager or similar for import
            // EventPackageManager.importFromUri(fileUri, new ImportCallback() {
            //     @Override
            //     public void onSuccess(List<LocalEvent> events) {
            //         runOnUiThread(() -> {
            //             showGlobalLoading(false, null);
            //             if (mCurrentListFragment != null) {
            //                 mCurrentListFragment.refreshEvents();
            //             }
            //             Toast.makeText(EventsActivity.this,
            //                 "Importati " + events.size() + " eventi", Toast.LENGTH_SHORT).show();
            //         });
            //     }
            //
            //     @Override
            //     public void onError(String error) {
            //         runOnUiThread(() -> {
            //             showGlobalLoading(false, null);
            //             Toast.makeText(EventsActivity.this,
            //                 "Errore import: " + error, Toast.LENGTH_LONG).show();
            //         });
            //     }
            // });

            // Placeholder implementation
            showGlobalLoading(false, null);
            Toast.makeText(this, "Import eventi - TODO: Integrare con EventPackageManager", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            showGlobalLoading(false, null);
            Log.e(TAG, "Error importing events: " + e.getMessage());
            Toast.makeText(this, "Errore durante l'import: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
                    Log.d(TAG, "Handling file intent: " + fileUri.toString());
                    importEventsFromFile(fileUri);
                }
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            // Handle sharing intent
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) {
                Log.d(TAG, "Handling shared file: " + sharedUri.toString());
                importEventsFromFile(sharedUri);
            }
        }
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
    public void triggerImportEvents() {
        importEventsFromFile();
    }

    /**
     * Trigger export from fragments
     */
    public void triggerExportEvents() {
        exportEventsToFile();
    }

    /**
     * Navigate to event detail
     */
    public void navigateToEventDetail(String eventId) {
        if (mNavController != null && eventId != null) {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);

            try {
                mNavController.navigate(R.id.action_events_list_to_event_detail, args);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to event detail: " + e.getMessage());
                Toast.makeText(this, "Errore navigazione", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Navigate back to events list
     */
    public void navigateBackToEventsList() {
        if (mNavController != null) {
            try {
                mNavController.popBackStack(R.id.nav_events_list, false);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating back to events list: " + e.getMessage());
            }
        }
    }

    /**
     * Refresh events list
     */
    public void refreshEventsList() {
        if (mCurrentListFragment != null) {
            mCurrentListFragment.refreshEvents();
        }
    }

    /**
     * Add event to list (after creation)
     */
    public void addEventToList(LocalEvent event) {
        if (mCurrentListFragment != null && event != null) {
            mCurrentListFragment.addEvent(event);
        }
    }

    /**
     * Remove event from list (after deletion)
     */
    public void removeEventFromList(String eventId) {
        if (mCurrentListFragment != null && eventId != null) {
            mCurrentListFragment.removeEvent(eventId);
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
}