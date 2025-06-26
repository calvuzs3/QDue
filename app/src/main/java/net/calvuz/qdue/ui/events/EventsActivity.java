package net.calvuz.qdue.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.InputType;
import android.view.MenuItem;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.file.FileAccessAdapter;
import net.calvuz.qdue.core.file.EventsImportAdapter;
import net.calvuz.qdue.core.permissions.PermissionManager;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.events.backup.BackupIntegration;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.imports.EventsImportManager;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.ui.events.interfaces.EventDeletionListener;
import net.calvuz.qdue.ui.events.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsEventOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsFileOperationsInterface;
import net.calvuz.qdue.ui.events.interfaces.EventsUIStateInterface;
import net.calvuz.qdue.utils.Log;

import java.time.format.DateTimeFormatter;

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
 */
public class EventsActivity extends AppCompatActivity implements
        EventsFileOperationsInterface,
        EventsDatabaseOperationsInterface,
        EventsEventOperationsInterface,
        EventsUIStateInterface {

    private static final String TAG = "EventsActivity";

    // Navigation
    private NavController mNavController;
    private NavHostFragment mNavHostFragment;

    // UI Components
    private MaterialToolbar mToolbar;
    private FloatingActionButton mFabAddEvent;
    private View mEmptyStateView;
    private View mLoadingStateView;

    // Empty State
    private boolean mHasEvents = false;

    // File operation launchers
    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;

    // Current fragment reference for communication
    private EventsListFragment mCurrentListFragment;

    // File Access
    private FileAccessAdapter mFileAccessAdapter;
    private EventsImportAdapter mEventsImportAdapter;
    private PermissionManager mPermissionManager;

    // Interfaces
//    private EventsOperationListener mEventsOperationListener;

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

        // Set up interfaces
//        mEventsOperationListener = (EventsOperationListener) this;
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

    // ==================== MENU HANDLING ====================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle toolbar navigation
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
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
    public void handleCreateNewEvent() {
        showCreateEventDialog();
    }

    /**
     * Handle import events from file
     */
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
     * Handle import events from URL
     */
    public void handleImportEventsFromUrl() {
        // TODO: to implement
        Log.d(TAG, "TODO: Handle import events from URL");
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
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
                    Log.d(TAG, "New Event Title: " + editTitle.getText());
                    String title = editTitle.getText().toString().trim();
                    Log.d(TAG, "New Event Title: " + title);

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

                EventsDatabase database = EventsDatabase.getInstance(this);
                long result = database.eventDao().insertEvent(newEvent);

                // Back to UI thread to update UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d(TAG, "DB insertResult = " + result);
                    if (result > 0) {
                        Log.d(TAG, "Evento creato (ID: " + newEvent.getId() +
                                ") (ROW ID: " + result + ")");
                        mCurrentListFragment.onEventCreated(newEvent);
                        showSuccess("Evento creato: " + title);

                    } else {
                        showError("Errore creando l'evento " + title);

                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating event: " + e.getMessage());
                runOnUiThread(() -> {
                    showError("Errore creando l'evento");
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
            Log.d(TAG, "Starting SAF-based import from: " + fileUri.toString());

            // Step 1: Show file info to user
            String fileInfo = mFileAccessAdapter.getFileDisplayInfo(fileUri);
            Log.d(TAG, "Selected file: " + fileInfo);

            // Step 2: Check if supported file type
            if (!mFileAccessAdapter.isSupportedFile(fileUri)) {
                showError("Unsupported file type. " + FileAccessAdapter.getSupportedFileTypesDescription());
                return;
            }

            // Step 3: Show import dialog with existing logic (no changes needed!)
            showImportDialog(fileUri);

        } catch (Exception e) {
            Log.e(TAG, "Error starting import", e);
            showError("Error reading file: " + e.getMessage());
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
        // TODO: Integrate with existing import functionality
        Log.d(TAG, "Importing events from: " + fileUri.toString());
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
                .setPositiveButton("Ok", null)  // Set later to prevent auto-close
                .setNegativeButton("Cancel", null)
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

                Log.d(TAG, "Advanced import options configured: " + getOptionsDescription(options));

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
        AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Advanced Import Options")
                .setView(dialogView)
                .setPositiveButton("Import", null) // Set later to prevent auto-close
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button importButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            importButton.setOnClickListener(v -> {
                // Create import options from dialog settings
                EventsImportManager.ImportOptions options = createImportOptionsFromAdvancedDialog(dialogView);

                Log.d(TAG, "Advanced import options configured: " + getOptionsDescription(options));

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
                    progressDialog.setMessage(String.format("Importing... (%d/%d) %s",
                            processed, total, currentEvent));
                });
            }

            @Override
            public void onComplete(EventsImportManager.ImportResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    //handleImportResult(result); // in the case of exporting next snippet

                    //  From OLD code ---
                    showImportResultDialog(result);

                    if (result.success && result.importedEvents > 0) {

                        // get Fragment actions..
                        mCurrentListFragment.refreshEvents(); // Refresh the list

                        // TRIGGER AUTO BACKUP AFTER IMPORT
                        BackupIntegration.integrateWithImport(
                                EventsActivity.this,
                                mCurrentListFragment.getEventsList(),
                                result.importedEvents
                        );
                    }
                    // ---
                });
            }

            @Override
            public void onError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Import failed: " + error);
                    Log.e(TAG, "Import error", exception);
                });
            }
        });

//        // Show progress dialog
//        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
//        progressDialog.setTitle("Importing Events");
//        progressDialog.setMessage("Processing...");
//        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//
//        EventsImportManager importManager = new EventsImportManager(this);
//
//        importManager.importFromFile(fileUri, options, new EventsImportManager.ImportCallback() {
//            @Override
//            public void onValidationComplete(JsonSchemaValidator.ValidationResult validationResult) {
//                runOnUiThread(() -> {
//                    if (!validationResult.isValid) {
//                        progressDialog.setMessage("Validation failed, importing anyway...");
//                    }
//                });
//            }
//
//            @Override
//            public void onProgress(int processed, int total, String currentEvent) {
//                runOnUiThread(() -> {
//                    progressDialog.setMax(total);
//                    progressDialog.setProgress(processed);
//                    progressDialog.setMessage("Processing: " + currentEvent);
//                });
//            }
//
//            @Override
//            public void onComplete(EventsImportManager.ImportResult result) {
//                runOnUiThread(() -> {
//                    progressDialog.dismiss();
//                    showImportResultDialog(result);
//
//                    if (result.success && result.importedEvents > 0) {
//
//                        // get Fragment actions..
//                        mCurrentListFragment.refreshEvents(); // Refresh the list
////                        loadEvents(); // Refresh the list (->now in fragment)
//
//                        // TRIGGER AUTO BACKUP AFTER IMPORT
//                        BackupIntegration.integrateWithImport(
//                                EventsActivity.this,
//                                mCurrentListFragment.getEventsList(),
////                                mEventsList, // (-> now in fragment)
//                                result.importedEvents
//                        );
//                    }
//                });
//            }
//
//            @Override
//            public void onError(String error, Exception exception) {
//                runOnUiThread(() -> {
//                    progressDialog.dismiss();
//                    showError("Import failed: " + error);
//                });
//            }
//        });
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

    // ==================== DATABASE MANAGEMENT ====================

    /**
     * Clear all events Room database
     */
    private void deleteAllEvents() {
        new Thread(() -> {
            try {
                EventsDatabase database = EventsDatabase.getInstance(this);
                int result = database.eventDao().deleteAllEvents();
//                database.eventDao().deleteAllLocalEvents(); // clears only local events

                // Run on UI thread to update UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result > 0) {
                        mCurrentListFragment.onEventsCleared(result, true);
                        showSuccess("All events cleared");
                    } else {
                        showError("Errore nel cancellare tutti  gli eventi");
                    }

                });

            } catch (Exception e) {
                Log.e(TAG, "Error clearing events from database", e);
                runOnUiThread(() -> {
                    showError("Errore nel cancellare tutti  gli eventi");
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

    // ==================== IMPLEMENT EventsEventOperationsInterface ====================

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
                "Sei sicuro di voler eliminare l'evento?\n\n" +
                        "Titolo: %s\n" +
                        "Data: %s\n\n" +
                        "Potrai annullare l'operazione per alcuni secondi dopo la conferma.",
                event.getTitle(),
                formatEventDateForDialog(event)
        );

        new AlertDialog.Builder(this)
                .setTitle("Elimina Evento")
                .setMessage(dialogMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Elimina", (dialog, which) -> {
                    // User confirmed deletion
                    startPendingEventDeletion(event);
                    listener.onDeletionRequested();
                })
                .setNegativeButton("Annulla", (dialog, which) -> {
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
                findViewById(android.R.id.content),
                "Evento \"" + eventTitle + "\" eliminato",
                Snackbar.LENGTH_LONG // 4 seconds
        );

        // Add undo action
        snackbar.setAction("ANNULLA", v -> {
            cancelEventDeletion(event);
            Log.d(TAG, "Event deletion cancelled by user: " + eventTitle);
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
                    Log.d(TAG, "Snackbar dismissed, performing actual deletion: " + eventTitle);
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
                EventDao eventDao = EventsDatabase.getInstance(this).eventDao();
                int deletedRows = eventDao.deleteEventById(eventId);

                runOnUiThread(() -> {
                    // Always remove from pending deletion
                    if (mCurrentListFragment != null) {
                        mCurrentListFragment.removeFromPendingDeletion(eventId);
                        mCurrentListFragment.suppressRefresh(false); // Re-enable refresh
                    }

                    if (deletedRows > 0) {
                        Log.d(TAG, "Event permanently deleted: " + eventTitle + " (ID: " + eventId + ")");
                        showSuccess("Evento eliminato definitivamente");

                        // Trigger backup after deletion
                        triggerBackupAfterDeletion(eventTitle);
                    } else {
                        Log.w(TAG, "No rows deleted for event ID: " + eventId);
                        showError("Evento non trovato nel database");

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
                        mCurrentListFragment.refreshEvents(); // Force refresh on error
                    }
                    showError("Errore durante l'eliminazione: " + e.getMessage());
                });
            }
        }).start();
    }


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

    /// //////////////////////////////////////////////////////////////

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
        }else {
            Log.w(TAG, "Cannot add event - fragment or event is null");
        }
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

    // ========================== FAB RELATED ==============================

    /**
     * Implementation of EventsUIStateInterface
     * Called by EventsListFragment to control FAB visibility
     */
    public void onEventsListStateChanged(boolean hasEvents) {
        Log.d(TAG, "onEventsListStateChanged called - hasEvents: " + hasEvents);
        mHasEvents = hasEvents;
        updateFabVisibility();
    }

    /**
     * Update FAB visibility based on current navigation destination and events availability
     */
    private void updateFabVisibility() {
        // Only show FAB if we're on events list AND there are events
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


}