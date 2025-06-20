package net.calvuz.qdue.ui.events;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import net.calvuz.qdue.BuildConfig;
import net.calvuz.qdue.R;
import net.calvuz.qdue.events.EventPackageJson;
import net.calvuz.qdue.events.EventPackageManager;
import net.calvuz.qdue.events.backup.ExportManager;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.imports.EventsImportManager;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.tests.EventsSystemTester;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.utils.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.calvuz.qdue.events.backup.BackupIntegration;
import net.calvuz.qdue.events.backup.BackupManager;
import net.calvuz.qdue.events.backup.RestoreManager;

/**
 * Complete Events Management Activity
 * <p>
 * Handles:
 * - Display and manage local events
 * - Import events from JSON files (manual selection + intent handling)
 * - Export events to JSON files
 * - Event creation and editing
 * - Integration with EventPackageManager for JSON processing
 */
public class EventsActivity extends AppCompatActivity implements EventsAdapter.OnEventClickListener {

    private static final String TAG = "EventsActivity";

    // UI Components
    private RecyclerView mEventsRecyclerView;
    private EventsAdapter mEventsAdapter;
    private FloatingActionButton mFabAddEvent;
    private View mEmptyStateView;

    // Data Management
    private EventPackageManager mPackageManager;
    private List<LocalEvent> mEventsList;

    // File Operations
    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;
    private BackupIntegration mBackupIntegration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        initializeComponents();
        setupToolbar();
        setupRecyclerView();
        setupFileOperations();
        setupFab();

        // Check if opened via intent (JSON file selected)
        handleIntent(getIntent());

        // Load existing events
        loadEvents();

        // Init Backup
        mBackupIntegration = BackupIntegration.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPackageManager != null) {
            mPackageManager.cleanup();  // Avoid memory leak
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Sync backup settings with preferences
        mBackupIntegration.syncWithPreferences();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Initialize core components
     */
    private void initializeComponents() {
        mPackageManager = new EventPackageManager(this);
        mEventsList = new ArrayList<>();

        // Initialize backup integration
        mBackupIntegration = BackupIntegration.getInstance(this);

        // Find views
        mEventsRecyclerView = findViewById(R.id.recycler_events);
        mFabAddEvent = findViewById(R.id.fab_add_event);
        mEmptyStateView = findViewById(R.id.empty_state_events);
    }

    /**
     * Setup toolbar with navigation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_events);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_eventi);
        }
    }

    /**
     * Setup RecyclerView for events display
     */
    private void setupRecyclerView() {
        mEventsAdapter = new EventsAdapter(mEventsList, this);
        mEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mEventsRecyclerView.setAdapter(mEventsAdapter);
    }

    /**
     * Setup file picker and saver launchers for import/export
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
     * Setup FloatingActionButton for adding new events
     */
    private void setupFab() {
        mFabAddEvent.setOnClickListener(v -> {
            // TODO: Open event creation dialog/activity
            showCreateEventDialog();
        });
    }

    /**
     * Handle incoming intents (file opening, sharing)
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if ("application/json".equals(type) || intent.getData().getPath().endsWith(".json")) {
                Uri fileUri = intent.getData();
                if (fileUri != null) {
                    Log.d(TAG, "Opening JSON file from external intent: " + fileUri.toString());
                    importEventsFromFile(fileUri);
                }
            }
        }
    }

    // ==================== IMPORT FUNCTIONALITY ====================

    /**
     * Enhanced import with comprehensive validation and user feedback
     */
    private void importEventsFromFile(Uri fileUri) {
        try {
            Log.d(TAG, "Starting enhanced import from file: " + fileUri.toString());

            // Show format detection info
            EventsImportManager.FileFormatInfo formatInfo = EventsImportManager.detectFileFormat(fileUri);
            if (!formatInfo.supported) {
                showError("Unsupported file format: " + formatInfo.description);
                return;
            }

            // Show import dialog with options
//            showImportOptionsDialog(fileUri);

            // MOCK: Show import dialog with options
            showAdvancedImportDialog(fileUri);

        } catch (Exception e) {
            Log.e(TAG, "Error starting import from file", e);
            showError("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Show import options dialog to user
     */
    private void showImportOptionsDialog(Uri fileUri) {
        String[] options = {
                "Quick Import (Skip duplicates)",
                "Replace Existing Events",
                "Validate First (Preview)",
                "Advanced Options..."
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Import Options")
                .setMessage("Choose how to import events from:\n" + fileUri.getLastPathSegment())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Quick import
                            performImport(fileUri, EventsImportManager.createDefaultOptions());
                            break;
                        case 1: // Replace existing
                            EventsImportManager.ImportOptions replaceOptions = EventsImportManager.createDefaultOptions();
                            replaceOptions.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.REPLACE_EXISTING;
                            performImport(fileUri, replaceOptions);
                            break;
                        case 2: // Validate first
                            performValidationPreview(fileUri);
                            break;
                        case 3: // Advanced options
                            showAdvancedImportDialog(fileUri);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                            showImportOptionsDialog(fileUri))
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
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Importing Events");
        progressDialog.setMessage("Processing...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        EventsImportManager importManager = new EventsImportManager(this);

        importManager.importFromFile(fileUri, options, new EventsImportManager.ImportCallback() {
            @Override
            public void onValidationComplete(JsonSchemaValidator.ValidationResult validationResult) {
                runOnUiThread(() -> {
                    if (!validationResult.isValid) {
                        progressDialog.setMessage("Validation failed, importing anyway...");
                    }
                });
            }

            @Override
            public void onProgress(int processed, int total, String currentEvent) {
                runOnUiThread(() -> {
                    progressDialog.setMax(total);
                    progressDialog.setProgress(processed);
                    progressDialog.setMessage("Processing: " + currentEvent);
                });
            }

            @Override
            public void onComplete(EventsImportManager.ImportResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showImportResultDialog(result);

                    if (result.success && result.importedEvents > 0) {
                        loadEvents(); // Refresh the list

                        // TRIGGER AUTO BACKUP AFTER IMPORT
                        BackupIntegration.integrateWithImport(
                                EventsActivity.this,
                                mEventsList,
                                result.importedEvents
                        );
                    }
                });
            }

            @Override
            public void onError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Import failed: " + error);
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
                EventsImportManager.ImportOptions options = createImportOptionsFromDialog(dialogView);

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

    // ==================== EXPORT FUNCTIONALITY ====================

    /**
     * Export current events to JSON file
     */
    private void exportEventsToFile(Uri fileUri) {
        try {
            Log.d(TAG, "Starting export to URI: " + fileUri.toString());

            if (mEventsList.isEmpty()) {
                showError("No events to export");
                return;
            }

            // Show export options dialog
            showExportOptionsDialog(fileUri);

        } catch (Exception e) {
            Log.e(TAG, "Error starting export to file", e);
            showError("Export initialization failed: " + e.getMessage());
        }
    }

    /**
     * Show export options dialog
     */
    private void showExportOptionsDialog(Uri fileUri) {
        String[] options = {
                "Export All Events",
                "Export Recent Events (Last 30 days)",
                "Export Custom Package",
                "Create Backup"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Export Options")
                .setMessage("Choose export type for " + mEventsList.size() + " events")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Export all
                            performExport(fileUri, mEventsList, ExportManager.ExportOptions.createDefault());
                            break;
                        case 1: // Recent events
                            exportRecentEvents(fileUri);
                            break;
                        case 2: // Custom package
                            showCustomExportDialog(fileUri);
                            break;
                        case 3: // Backup
                            performExport(fileUri, mEventsList, ExportManager.ExportOptions.createBackup());
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show advanced custom export dialog with filters
     */
    private void showAdvancedCustomExportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_export, null);

        // Get UI elements
        EditText editPackageName = dialogView.findViewById(R.id.edit_package_name);
        EditText editPackageDescription = dialogView.findViewById(R.id.edit_package_description);

        CheckBox checkboxDateFilter = dialogView.findViewById(R.id.checkbox_date_filter);
        TextInputLayout layoutFromDate = dialogView.findViewById(R.id.layout_from_date);
        TextInputLayout layoutToDate = dialogView.findViewById(R.id.layout_to_date);
        EditText editFromDate = dialogView.findViewById(R.id.edit_from_date);
        EditText editToDate = dialogView.findViewById(R.id.edit_to_date);

        CheckBox checkboxTypeFilter = dialogView.findViewById(R.id.checkbox_type_filter);
        LinearLayout layoutEventTypes = dialogView.findViewById(R.id.layout_event_types);

        CheckBox checkboxIncludeCustomProps = dialogView.findViewById(R.id.checkbox_include_custom_properties);
        CheckBox checkboxPrettyPrint = dialogView.findViewById(R.id.checkbox_pretty_print);
        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);

        TextView textExportSummary = dialogView.findViewById(R.id.text_export_summary);

        // Setup date filter toggle
        checkboxDateFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutFromDate.setEnabled(isChecked);
            layoutToDate.setEnabled(isChecked);
            updateExportSummary(dialogView);
        });

        // Setup type filter toggle
        checkboxTypeFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setEventTypeCheckboxesEnabled(layoutEventTypes, isChecked);
            updateExportSummary(dialogView);
        });

        // Setup date pickers
        setupDatePicker(editFromDate, "Select start date");
        setupDatePicker(editToDate, "Select end date");

        // Set default values
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        editPackageName.setText("Custom Export " + timestamp);
        editPackageDescription.setText("Custom export of filtered events");

        // Initial summary update
        updateExportSummary(dialogView);

        // Create dialog
        AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Custom Export Options")
                .setView(dialogView)
                .setPositiveButton("Export", null) // Set later to prevent auto-close
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button exportButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            exportButton.setOnClickListener(v -> {
                // Validate and perform export
                ExportManager.ExportOptions options = createExportOptionsFromDialog(dialogView);
                List<LocalEvent> filteredEvents = filterEventsFromDialog(dialogView);

                if (filteredEvents.isEmpty()) {
                    showError("No events match the selected filters");
                    return;
                }

                dialog.dismiss();
                performExport(fileUri, filteredEvents, options);
            });
        });

        dialog.show();
    }

    /**
     * Export recent events (last 30 days)
     */
    private void exportRecentEvents(Uri fileUri) {
        LocalDate cutoffDate = LocalDate.now().minusDays(30);
        List<LocalEvent> recentEvents = new ArrayList<>();

        for (LocalEvent event : mEventsList) {
            if (event.getDate().isAfter(cutoffDate) || event.getDate().equals(cutoffDate)) {
                recentEvents.add(event);
            }
        }

        if (recentEvents.isEmpty()) {
            showError("No recent events found (last 30 days)");
            return;
        }

        ExportManager.ExportOptions options = ExportManager.ExportOptions.createDefault();
        options.packageName = "Recent Events Export";
        options.packageDescription = "Events from the last 30 days";

        performExport(fileUri, recentEvents, options);
    }

    /**
     * Show custom export options
     */
    private void showCustomExportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_export, null);

        // TODO: Setup custom export dialog with filters
        // For now, use default export
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Custom Export")
                .setMessage("Custom export options coming soon.\n\nUsing default export for now.")
                .setPositiveButton("Export", (dialog, which) ->
                        performExport(fileUri, mEventsList, ExportManager.ExportOptions.createDefault()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Perform the actual export operation
     */
    private void performExport(Uri fileUri, List<LocalEvent> eventsToExport,
                               ExportManager.ExportOptions options) {

        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Exporting Events");
        progressDialog.setMessage("Preparing export...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExportManager exportManager = mBackupIntegration.getExportManager();

        exportManager.exportToUri(eventsToExport, fileUri, options, new ExportManager.ExportCallback() {
            @Override
            public void onExportComplete(ExportManager.ExportResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showExportResultDialog(result);
                });
            }

            @Override
            public void onExportProgress(int processed, int total, String currentEvent) {
                runOnUiThread(() -> {
                    progressDialog.setMax(total);
                    progressDialog.setProgress(processed);
                    progressDialog.setMessage("Exporting: " + currentEvent);
                });
            }

            @Override
            public void onExportError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Export failed: " + error);
                });
            }
        });
    }

    /**
     * Show export result dialog
     */
    private void showExportResultDialog(ExportManager.ExportResult result) {
        String title = result.success ? "Export Completed" : "Export Failed";

        StringBuilder message = new StringBuilder();
        message.append(result.getSummary()).append("\n\n");

        if (result.success) {
            message.append("File: ").append(result.exportPath).append("\n");
            message.append("Size: ").append(result.getFormattedSize()).append("\n");
            message.append("Time: ").append(result.exportTime).append("\n");
        }

        if (result.hasWarnings()) {
            message.append("\n⚠️ Warnings:\n");
            for (String warning : result.warnings) {
                message.append("• ").append(warning).append("\n");
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Update export summary text based on current selections
     */
    private void updateExportSummary(View dialogView) {
        TextView textSummary = dialogView.findViewById(R.id.text_export_summary);
        List<LocalEvent> filteredEvents = filterEventsFromDialog(dialogView);

        StringBuilder summary = new StringBuilder();
        summary.append("Ready to export ").append(filteredEvents.size())
                .append(" of ").append(mEventsList.size()).append(" events");

        CheckBox dateFilter = dialogView.findViewById(R.id.checkbox_date_filter);
        CheckBox typeFilter = dialogView.findViewById(R.id.checkbox_type_filter);

        if (dateFilter.isChecked() || typeFilter.isChecked()) {
            summary.append(" (filtered)");
        }

        textSummary.setText(summary.toString());
    }

    /**
     * Create export options from dialog selections
     */
    private ExportManager.ExportOptions createExportOptionsFromDialog(View dialogView) {
        ExportManager.ExportOptions options = ExportManager.ExportOptions.createDefault();

        EditText editPackageName = dialogView.findViewById(R.id.edit_package_name);
        EditText editPackageDescription = dialogView.findViewById(R.id.edit_package_description);
        CheckBox checkboxIncludeCustomProps = dialogView.findViewById(R.id.checkbox_include_custom_properties);
        CheckBox checkboxPrettyPrint = dialogView.findViewById(R.id.checkbox_pretty_print);
        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);

        options.packageName = editPackageName.getText().toString().trim();
        options.packageDescription = editPackageDescription.getText().toString().trim();
        options.includeCustomProperties = checkboxIncludeCustomProps.isChecked();
        options.prettyPrint = checkboxPrettyPrint.isChecked();
        options.reportProgress = checkboxShowProgress.isChecked();

        if (options.packageName.isEmpty()) {
            options.packageName = "Custom Export";
        }

        return options;
    }

    /// ////////////////////////////////////

    /**
     * Read content from URI as string
     */
    private String readFileContent(Uri uri) throws Exception {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    /**
     * Load events from database and update UI
     */
    private void loadEvents() {
        // Load events from database via EventDao
        Log.d(TAG, "Loading events from Room Database");

        // Use Room database directly
        new Thread(() -> {
            try {
                EventsDatabase database = EventsDatabase.getInstance(this);
                List<LocalEvent> allEvents = database.eventDao().getAllEvents();

                // Switch back to main thread for UI updates
                runOnUiThread(() -> {
                    mEventsList.clear();

                    if (allEvents != null && !allEvents.isEmpty()) {
                        mEventsList.addAll(allEvents);
                        Log.d(TAG, "Loaded " + allEvents.size() + " events from database");
                    } else {
                        Log.d(TAG, "No events found in database");
                    }

                    mEventsAdapter.notifyDataSetChanged();
                    updateEmptyState();

                    Log.d(TAG, "Events loaded successfully, total: " + mEventsList.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading events from database", e);
                runOnUiThread(() -> {
                    Toast.makeText(EventsActivity.this, "Errore caricamento eventi", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (mEventsList.isEmpty()) {
            mEventsRecyclerView.setVisibility(View.GONE);
            mEmptyStateView.setVisibility(View.VISIBLE);
        } else {
            mEventsRecyclerView.setVisibility(View.VISIBLE);
            mEmptyStateView.setVisibility(View.GONE);
        }
    }

    /**
     * Show event creation dialog
     */
    private void showCreateEventDialog() {
        // Simple event creation for now
        android.widget.EditText editTitle = new android.widget.EditText(this);
        editTitle.setHint("Event title");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Create New Event")
                .setMessage("Enter event title:")
                .setView(editTitle)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = editTitle.getText().toString().trim();
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
                newEvent.setDescription("Created via EventsActivity");

                EventsDatabase database = EventsDatabase.getInstance(this);
                database.eventDao().insertEvent(newEvent);

                runOnUiThread(() -> {
                    loadEvents(); // Refresh list
                    showSuccess("Event created: " + title);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating event", e);
                runOnUiThread(() -> {
                    showError("Error creating event");
                });
            }
        }).start();
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

    // ==================== MENU HANDLING ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_events, menu);

        // Show test option only in debug builds
        MenuItem testItem = menu.findItem(R.id.action_run_tests);
        if (testItem != null) {
            testItem.setVisible(BuildConfig.DEBUG);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_import_file) {
            openFilePicker();
            return true;
        } else if (itemId == R.id.action_export_file) {
            openFileSaver();
            return true;
        } else if (itemId == R.id.action_clear_all) {
            showClearAllConfirmation();
            return true;
        } else if (itemId == R.id.action_settings) {
            openEventsSettings();
            return true;
        } else if (itemId == R.id.action_run_tests) { // DEBUG ONLY
            runEventsSystemTests();
            return true;
        }

        // ==================== NEW BACKUP/RESTORE MENU HANDLERS ====================

        else if (itemId == R.id.action_manual_backup) {
            performManualBackup();
            return true;
        } else if (itemId == R.id.action_restore_backup) {
            showRestoreBackupDialog();
            return true;
        } else if (itemId == R.id.action_auto_backup_toggle) {
            showAutoBackupSettings();
            return true;
        } else if (itemId == R.id.action_backup_status) {
            showBackupStatus();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open file picker for JSON import
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            mFilePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            Toast.makeText(this, "Error opening file picker", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open file saver for JSON export
     */
    private void openFileSaver() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "qdue_events_" +
                java.time.LocalDate.now().toString() + ".json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            mFileSaverLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file saver", e);
            Toast.makeText(this, "Error opening file saver", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show confirmation dialog for clearing all events
     */
    private void showClearAllConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Events")
                .setMessage("Are you sure you want to delete all local events? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllEvents())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Clear all local events Room database
     */
    private void clearAllEvents() {
        new Thread(() -> {
            try {
                EventsDatabase database = EventsDatabase.getInstance(this);
                database.eventDao().deleteAllEvents();
//                database.eventDao().deleteAllLocalEvents();

                runOnUiThread(() -> {
                    mEventsList.clear();
                    mEventsAdapter.notifyDataSetChanged();
                    updateEmptyState();

                    // TRIGGER AUTO BACKUP AFTER CLEAR
                    BackupIntegration.integrateWithClearAll(this, mEventsList);

                    showSuccess("All events cleared");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error clearing events from database", e);
                runOnUiThread(() -> {
                    showError("Error clearing events");
                });
            }
        }).start();
    }

    /**
     * Open events settings
     */
    private void openEventsSettings() {
        // TODO: Open EventsPreferenceFragment or settings activity
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
    }

    // ==================== 2. ENHANCED BACKUP SYSTEM ====================

    /**
     * Perform manual backup of current events
     */
    private void performManualBackup() {
        if (mEventsList.isEmpty()) {
            showError("No events to backup");
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Creating backup...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mBackupIntegration.performManualBackup(mEventsList, new BackupManager.BackupCallback() {
            @Override
            public void onBackupComplete(BackupManager.BackupResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (result.success) {
                        showSuccess(String.format("Backup created: %d events saved", result.eventsCount));
                    } else {
                        showError("Backup failed");
                    }
                });
            }

            @Override
            public void onBackupError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Backup failed: " + error);
                });
            }
        });
    }

    /// /////////////////////////////////////

    /**
     * Enhanced manual backup with more options
     */
    private void performManualBackupEnhanced() {
        if (mEventsList.isEmpty()) {
            showError("No events to backup");
            return;
        }

        String[] backupOptions = {
                "Quick Backup (Auto settings)",
                "Full Backup with Custom Name",
                "Incremental Backup",
                "Export to External File"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Options")
                .setMessage("Choose backup type for " + mEventsList.size() + " events")
                .setItems(backupOptions, (dialog, which) -> {
                    switch (which) {
                        case 0: // Quick backup
                            performQuickBackup();
                            break;
                        case 1: // Full backup with custom name
                            showCustomBackupDialog();
                            break;
                        case 2: // Incremental backup
                            performIncrementalBackup();
                            break;
                        case 3: // Export to external file
                            openFileSaver();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Quick backup with auto settings
     */
    private void performQuickBackup() {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Creating quick backup...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mBackupIntegration.performManualBackup(mEventsList, new BackupManager.BackupCallback() {
            @Override
            public void onBackupComplete(BackupManager.BackupResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (result.success) {
                        showSuccess(String.format("Quick backup created: %d events saved", result.eventsCount));

                        // Show backup details
                        showBackupDetailsDialog(result);
                    } else {
                        showError("Quick backup failed");
                    }
                });
            }

            @Override
            public void onBackupError(String error, Exception exception) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Quick backup failed: " + error);
                });
            }
        });
    }

    /**
     * Show backup details after creation
     */
    private void showBackupDetailsDialog(BackupManager.BackupResult result) {
        String message = String.format(
                "Backup Details:\n\n" +
                        "Events: %d\n" +
                        "Size: %s\n" +
                        "File: %s\n" +
                        "Time: %s",
                result.eventsCount,
                formatFileSize(result.backupSizeBytes),
                new java.io.File(result.backupFilePath).getName(),
                result.timestamp
        );

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Created")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("View All Backups", (dialog, which) -> showBackupStatus())
                .show();
    }

    /**
     * Show custom backup dialog
     */
    private void showCustomBackupDialog() {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("Backup name (optional)");

        String defaultName = "Manual_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        editText.setText(defaultName);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Custom Backup")
                .setMessage("Enter a name for this backup:")
                .setView(editText)
                .setPositiveButton("Create Backup", (dialog, which) -> {
                    String backupName = editText.getText().toString().trim();
                    if (backupName.isEmpty()) {
                        backupName = defaultName;
                    }
                    performCustomNamedBackup(backupName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Perform backup with custom name
     */
    private void performCustomNamedBackup(String backupName) {
        // TODO: Implement custom named backup
        // For now, perform regular backup
        Log.d(TAG, "Creating custom backup with name: " + backupName);
        performQuickBackup();
    }

    /**
     * Perform incremental backup (only changed events)
     */
    private void performIncrementalBackup() {
        // TODO: Implement incremental backup logic
        // For now, show coming soon message
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Incremental Backup")
                .setMessage("Incremental backup functionality coming soon.\n\nThis will backup only events that have changed since the last backup.")
                .setPositiveButton("Use Full Backup", (dialog, which) -> performQuickBackup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== 3. ENHANCED RESTORE SYSTEM ====================

    /**
     * Show restore backup dialog with available backups
     */
    private void showRestoreBackupDialog() {
        List<BackupManager.BackupInfo> backups = mBackupIntegration.getBackupManager().getAvailableBackups();

        if (backups.isEmpty()) {
            showError("No backups available");
            return;
        }

        // Create backup selection dialog
        String[] backupNames = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            BackupManager.BackupInfo backup = backups.get(i);
            backupNames[i] = String.format("%s\n%s - %s",
                    backup.filename,
                    backup.getFormattedDate(),
                    backup.getFormattedSize());
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Backup to Restore")
                .setItems(backupNames, (dialog, which) -> {
                    BackupManager.BackupInfo selectedBackup = backups.get(which);
                    showRestoreOptionsDialog(selectedBackup);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

/// ///////////////////////////////////////////
    /**
     * Enhanced restore with better preview and options
     */
    private void showRestoreBackupDialogEnhanced() {
        List<BackupManager.BackupInfo> backups = mBackupIntegration.getBackupManager().getAvailableBackups();

        if (backups.isEmpty()) {
            showError("No backups available");
            return;
        }

        // Create enhanced backup selection dialog
        String[] backupDescriptions = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            BackupManager.BackupInfo backup = backups.get(i);
            backupDescriptions[i] = String.format(
                    "%s\n%s • %s • %s",
                    backup.filename.replace("events_backup_", "").replace(".json", ""),
                    backup.getFormattedDate(),
                    backup.getFormattedSize(),
                    "Ready to restore"
            );
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Backup to Restore (" + backups.size() + " available)")
                .setItems(backupDescriptions, (dialog, which) -> {
                    BackupManager.BackupInfo selectedBackup = backups.get(which);
                    showEnhancedRestoreOptionsDialog(selectedBackup);
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Backup Manager", (dialog, which) -> showBackupManagerDialog())
                .show();
    }

    /**
     * Show enhanced restore options with detailed preview
     */
    private void showEnhancedRestoreOptionsDialog(BackupManager.BackupInfo backup) {
        // Show loading dialog while previewing
        android.app.ProgressDialog loadingDialog = new android.app.ProgressDialog(this);
        loadingDialog.setMessage("Analyzing backup...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        mBackupIntegration.getRestoreManager().previewBackup(backup.fullPath,
                new RestoreManager.PreviewCallback() {
                    @Override
                    public void onPreviewComplete(RestoreManager.PreviewResult result) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            showDetailedRestorePreview(result, backup);
                        });
                    }

                    @Override
                    public void onPreviewError(String error, Exception exception) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            showError("Cannot analyze backup: " + error);
                        });
                    }
                });
    }

    /**
     * Show detailed restore preview with statistics
     */
    private void showDetailedRestorePreview(RestoreManager.PreviewResult preview,
                                            BackupManager.BackupInfo backup) {
        StringBuilder message = new StringBuilder();

        // Backup file info
        message.append("📁 Backup Information:\n");
        message.append("File: ").append(backup.filename).append("\n");
        message.append("Created: ").append(backup.getFormattedDate()).append("\n");
        message.append("Size: ").append(backup.getFormattedSize()).append("\n\n");

        if (preview.success) {
            // Package info
            message.append("📦 Package Details:\n");
            message.append("Name: ").append(preview.packageInfo.name).append("\n");
            message.append("Version: ").append(preview.packageInfo.version).append("\n");
            message.append("Events: ").append(preview.getEventsCount()).append("\n\n");

            // Event type breakdown
            message.append("📊 Event Statistics:\n");
            Map<String, Integer> typeCount = new HashMap<>();
            for (RestoreManager.EventSummary summary : preview.eventSummaries) {
                typeCount.put(summary.eventType, typeCount.getOrDefault(summary.eventType, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                message.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }

            // Warnings
            if (preview.hasWarnings()) {
                message.append("\n⚠️ Warnings (").append(preview.warnings.size()).append("):\n");
                for (String warning : preview.warnings) {
                    message.append("• ").append(warning).append("\n");
                }
            }

            // Current state info
            message.append("\n📋 Current State:\n");
            message.append("Current events: ").append(mEventsList.size()).append("\n");
            message.append("After merge: ~").append(mEventsList.size() + preview.getEventsCount()).append(" events\n");

        } else {
            message.append("❌ Preview failed: ").append(preview.errorMessage);
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Restore Preview")
                        .setMessage(message.toString());

        if (preview.success) {
            builder.setPositiveButton("Merge with Current", (dialog, which) ->
                            showRestoreMethodDialog(backup, RestoreManager.RestoreOptions.createDefault()))
                    .setNeutralButton("Replace All Events", (dialog, which) ->
                            confirmReplaceAllRestore(backup))
                    .setNegativeButton("Cancel", null);
        } else {
            builder.setPositiveButton("Try Anyway", (dialog, which) ->
                            showRestoreMethodDialog(backup, RestoreManager.RestoreOptions.createDefault()))
                    .setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    /**
     * Confirm replace all restore operation
     */
    private void confirmReplaceAllRestore(BackupManager.BackupInfo backup) {
        String message = String.format(
                "⚠️ WARNING: This will DELETE all %d current events and replace them with events from the backup.\n\n" +
                        "This action CANNOT be undone!\n\n" +
                        "A backup of current events will be created automatically before proceeding.",
                mEventsList.size()
        );

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Replace All Events")
                .setMessage(message)
                .setPositiveButton("REPLACE ALL", (dialog, which) ->
                        showRestoreMethodDialog(backup, RestoreManager.RestoreOptions.createReplaceAll()))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Show restore method selection dialog
     */
    private void showRestoreMethodDialog(BackupManager.BackupInfo backup, RestoreManager.RestoreOptions baseOptions) {
        String[] methods = {
                "Standard Restore",
                "Create Backup First",
                "Detailed Progress",
                "Advanced Options"
        };

        String[] descriptions = {
                "Quick restore with default settings",
                "Create backup of current events before restore",
                "Show detailed progress and event names",
                "Custom conflict resolution and filters"
        };

        // Create custom dialog with descriptions
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Restore Method");

        StringBuilder dialogMessage = new StringBuilder();
        dialogMessage.append("Choose restore method for:\n").append(backup.filename).append("\n\n");
        for (int i = 0; i < methods.length; i++) {
            dialogMessage.append(methods[i]).append("\n").append(descriptions[i]).append("\n\n");
        }

        builder.setMessage(dialogMessage.toString());

        builder.setItems(methods, (dialog, which) -> {
            RestoreManager.RestoreOptions options = new RestoreManager.RestoreOptions();
            options.mode = baseOptions.mode;

            switch (which) {
                case 0: // Standard
                    options.validateBeforeRestore = true;
                    options.createBackupBeforeRestore = false;
                    options.reportProgress = false;
                    break;
                case 1: // Create backup first
                    options.validateBeforeRestore = true;
                    options.createBackupBeforeRestore = true;
                    options.reportProgress = false;
                    break;
                case 2: // Detailed progress
                    options.validateBeforeRestore = true;
                    options.createBackupBeforeRestore = false;
                    options.reportProgress = true;
                    break;
                case 3: // Advanced options
                    showAdvancedRestoreDialog(backup, baseOptions);
                    return;
            }

            performRestore(backup, options);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Show advanced restore options
     */
    private void showAdvancedRestoreDialog(BackupManager.BackupInfo backup, RestoreManager.RestoreOptions baseOptions) {
        // TODO: Create advanced restore dialog with checkboxes
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Advanced Restore Options")
                .setMessage("Advanced restore options coming soon.\n\nUsing standard restore for now.")
                .setPositiveButton("Standard Restore", (dialog, which) ->
                        performRestore(backup, baseOptions))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /// ///////////////////////////////////////////////

    /**
     * Show restore options for selected backup
     */
    private void showRestoreOptionsDialog(BackupManager.BackupInfo backup) {
        // First, preview the backup
        mBackupIntegration.getRestoreManager().previewBackup(backup.fullPath,
                new RestoreManager.PreviewCallback() {
                    @Override
                    public void onPreviewComplete(RestoreManager.PreviewResult result) {
                        runOnUiThread(() -> showRestorePreviewDialog(result, backup));
                    }

                    @Override
                    public void onPreviewError(String error, Exception exception) {
                        runOnUiThread(() -> showError("Cannot preview backup: " + error));
                    }
                });
    }

    /**
     * Show restore preview and options
     */
    private void showRestorePreviewDialog(RestoreManager.PreviewResult preview,
                                          BackupManager.BackupInfo backup) {
        StringBuilder message = new StringBuilder();
        message.append("Backup: ").append(backup.filename).append("\n");
        message.append("Created: ").append(backup.getFormattedDate()).append("\n");
        message.append("Size: ").append(backup.getFormattedSize()).append("\n\n");

        if (preview.success) {
            message.append("Package: ").append(preview.packageInfo.name).append("\n");
            message.append("Events: ").append(preview.getEventsCount()).append("\n");

            if (preview.hasWarnings()) {
                message.append("\n⚠️ Warnings:\n");
                for (String warning : preview.warnings) {
                    message.append("• ").append(warning).append("\n");
                }
            }
        } else {
            message.append("❌ Preview failed: ").append(preview.errorMessage);
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Restore Preview")
                        .setMessage(message.toString());

        if (preview.success) {
            builder.setPositiveButton("Restore (Merge)", (dialog, which) ->
                            performRestore(backup, RestoreManager.RestoreOptions.createDefault()))
                    .setNeutralButton("Restore (Replace All)", (dialog, which) ->
                            performRestore(backup, RestoreManager.RestoreOptions.createReplaceAll()));
        }

        builder.setNegativeButton("Cancel", null).show();
    }

    /**
     * Perform restore operation
     */
    private void performRestore(BackupManager.BackupInfo backup, RestoreManager.RestoreOptions options) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Restoring Events");
        progressDialog.setMessage("Processing...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        mBackupIntegration.getRestoreManager().restoreFromBackup(backup.fullPath, options,
                new RestoreManager.RestoreCallback() {
                    @Override
                    public void onRestoreComplete(RestoreManager.RestoreResult result) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showRestoreResultDialog(result);
                            if (result.success && result.restoredEvents > 0) {
                                loadEvents(); // Refresh the list
                            }
                        });
                    }

                    @Override
                    public void onRestoreProgress(int processed, int total, String currentEvent) {
                        runOnUiThread(() -> {
                            progressDialog.setMax(total);
                            progressDialog.setProgress(processed);
                            progressDialog.setMessage("Restoring: " + currentEvent);
                        });
                    }

                    @Override
                    public void onRestoreError(String error, Exception exception) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showError("Restore failed: " + error);
                        });
                    }
                });
    }

    /**
     * Show restore result dialog
     */
    private void showRestoreResultDialog(RestoreManager.RestoreResult result) {
        String title = result.success ? "Restore Completed" : "Restore Failed";
        String message = result.getSummary();

        if (result.hasIssues()) {
            message += "\n\nIssues found:\n";
            for (String warning : result.warnings) {
                message += "⚠️ " + warning + "\n";
            }
            for (String error : result.errors) {
                message += "❌ " + error + "\n";
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

// ==================== 4. BACKUP MANAGER DIALOG ====================

    /**
     * Show auto backup settings
     */
    private void showAutoBackupSettings() {
        BackupIntegration.BackupSummary summary = mBackupIntegration.getBackupSummary();

        String message = "Current Status: " + summary.getStatusText() + "\n\n" +
                "Total Backups Created: " + summary.totalBackupsCreated + "\n" +
                "Available Backups: " + summary.availableBackups;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Auto Backup Settings")
                .setMessage(message)
                .setPositiveButton(summary.autoBackupEnabled ? "Disable Auto Backup" : "Enable Auto Backup",
                        (dialog, which) -> toggleAutoBackup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Toggle auto backup setting
     */
    private void toggleAutoBackup() {
        boolean currentState = mBackupIntegration.getBackupManager().isAutoBackupEnabled();
        mBackupIntegration.updatePreferences(!currentState);

        String message = !currentState ? "Auto backup enabled" : "Auto backup disabled";
        showSuccess(message);
    }

    /**
     * Show backup status information
     */
    private void showBackupStatus() {
        BackupIntegration.BackupSummary summary = mBackupIntegration.getBackupSummary();
        List<BackupManager.BackupInfo> backups = mBackupIntegration.getBackupManager().getAvailableBackups();

        StringBuilder message = new StringBuilder();
        message.append("📊 Backup Status\n\n");
        message.append("Auto Backup: ").append(summary.autoBackupEnabled ? "ON" : "OFF").append("\n");
        message.append("Last Backup: ").append(summary.lastBackupTime != null ? summary.lastBackupTime : "Never").append("\n");
        message.append("Total Created: ").append(summary.totalBackupsCreated).append("\n");
        message.append("Available: ").append(summary.availableBackups).append("\n\n");

        if (!backups.isEmpty()) {
            message.append("📁 Recent Backups:\n");
            for (int i = 0; i < Math.min(3, backups.size()); i++) {
                BackupManager.BackupInfo backup = backups.get(i);
                message.append("• ").append(backup.filename).append("\n");
                message.append("  ").append(backup.getFormattedDate()).append(" - ").append(backup.getFormattedSize()).append("\n");
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Status")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Clean Old Backups", (dialog, which) -> showCleanBackupsConfirmation())
                .show();
    }

/// //////////////////////////////////////////

    /**
     * Show comprehensive backup manager dialog
     */
    private void showBackupManagerDialog() {
        List<BackupManager.BackupInfo> backups = mBackupIntegration.getBackupManager().getAvailableBackups();
        BackupIntegration.BackupSummary summary = mBackupIntegration.getBackupSummary();

        StringBuilder message = new StringBuilder();
        message.append("🔧 Backup Manager\n\n");

        // Status
        message.append("📊 Status:\n");
        message.append("Auto Backup: ").append(summary.autoBackupEnabled ? "ON" : "OFF").append("\n");
        message.append("Total Created: ").append(summary.totalBackupsCreated).append("\n");
        message.append("Available: ").append(summary.availableBackups).append("\n");
        message.append("Last Backup: ").append(summary.lastBackupTime != null ? summary.lastBackupTime : "Never").append("\n\n");

        // Storage info
        long totalSize = 0;
        for (BackupManager.BackupInfo backup : backups) {
            totalSize += backup.sizeBytes;
        }
        message.append("💾 Storage:\n");
        message.append("Total Size: ").append(formatFileSize(totalSize)).append("\n");
        message.append("Average: ").append(backups.isEmpty() ? "0 B" : formatFileSize(totalSize / backups.size())).append("\n\n");

        // Recent backups
        if (!backups.isEmpty()) {
            message.append("📁 Recent Backups:\n");
            for (int i = 0; i < Math.min(3, backups.size()); i++) {
                BackupManager.BackupInfo backup = backups.get(i);
                message.append("• ").append(backup.filename).append("\n");
                message.append("  ").append(backup.getFormattedDate()).append(" - ").append(backup.getFormattedSize()).append("\n");
            }
            if (backups.size() > 3) {
                message.append("... and ").append(backups.size() - 3).append(" more\n");
            }
        }

        String[] actions = {
                "Toggle Auto Backup",
                "Create Manual Backup",
                "Clean Old Backups",
                "View All Backups",
                "Export Backup Settings"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Manager")
                .setMessage(message.toString())
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: // Toggle auto backup
                            toggleAutoBackup();
                            break;
                        case 1: // Manual backup
                            performManualBackupEnhanced();
                            break;
                        case 2: // Clean backups
                            showCleanBackupsConfirmation();
                            break;
                        case 3: // View all
                            showAllBackupsDialog();
                            break;
                        case 4: // Export settings
                            exportBackupSettings();
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Show all backups with detailed info
     */
    private void showAllBackupsDialog() {
        List<BackupManager.BackupInfo> backups = mBackupIntegration.getBackupManager().getAvailableBackups();

        if (backups.isEmpty()) {
            showError("No backups available");
            return;
        }

        String[] backupDetails = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            BackupManager.BackupInfo backup = backups.get(i);
            backupDetails[i] = String.format(
                    "%s\n%s • %s",
                    backup.filename.replace("events_backup_", "").replace(".json", ""),
                    backup.getFormattedDate(),
                    backup.getFormattedSize()
            );
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("All Backups (" + backups.size() + ")")
                .setItems(backupDetails, (dialog, which) -> {
                    BackupManager.BackupInfo selectedBackup = backups.get(which);
                    showBackupActionsDialog(selectedBackup);
                })
                .setNegativeButton("Close", null)
                .setNeutralButton("Manager", (dialog, which) -> showBackupManagerDialog())
                .show();
    }

    /**
     * Show actions for specific backup
     */
    private void showBackupActionsDialog(BackupManager.BackupInfo backup) {
        String[] actions = {
                "Restore from this Backup",
                "Preview Backup Contents",
                "Export to External File",
                "Delete this Backup",
                "Backup Information"
        };

        String title = "Backup Actions\n" + backup.filename;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: // Restore
                            showEnhancedRestoreOptionsDialog(backup);
                            break;
                        case 1: // Preview
                            previewBackupContents(backup);
                            break;
                        case 2: // Export
                            exportBackupToExternalFile(backup);
                            break;
                        case 3: // Delete
                            confirmDeleteBackup(backup);
                            break;
                        case 4: // Info
                            showBackupInformation(backup);
                            break;
                    }
                })
                .setNegativeButton("Back", null)
                .show();
    }

    /**
     * Preview backup contents without restoring
     */
    private void previewBackupContents(BackupManager.BackupInfo backup) {
        android.app.ProgressDialog loadingDialog = new android.app.ProgressDialog(this);
        loadingDialog.setMessage("Loading backup contents...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        mBackupIntegration.getRestoreManager().previewBackup(backup.fullPath,
                new RestoreManager.PreviewCallback() {
                    @Override
                    public void onPreviewComplete(RestoreManager.PreviewResult result) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            showBackupContentsDialog(result, backup);
                        });
                    }

                    @Override
                    public void onPreviewError(String error, Exception exception) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            showError("Cannot load backup contents: " + error);
                        });
                    }
                });
    }

    /**
     * Show backup contents in readable format
     */
    private void showBackupContentsDialog(RestoreManager.PreviewResult preview, BackupManager.BackupInfo backup) {
        StringBuilder content = new StringBuilder();

        content.append("📋 Backup Contents Preview\n\n");
        content.append("File: ").append(backup.filename).append("\n");
        content.append("Package: ").append(preview.packageInfo.name).append("\n");
        content.append("Events: ").append(preview.getEventsCount()).append("\n\n");

        if (preview.getEventsCount() > 0) {
            content.append("📅 Events List:\n");
            for (int i = 0; i < Math.min(10, preview.eventSummaries.size()); i++) {
                RestoreManager.EventSummary summary = preview.eventSummaries.get(i);
                content.append(String.format("• %s\n  %s\n", summary.title, summary.dateRange));
            }

            if (preview.eventSummaries.size() > 10) {
                content.append("... and ").append(preview.eventSummaries.size() - 10).append(" more events\n");
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Contents")
                .setMessage(content.toString())
                .setPositiveButton("Restore", (dialog, which) -> showEnhancedRestoreOptionsDialog(backup))
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Export backup to external file
     */
    private void exportBackupToExternalFile(BackupManager.BackupInfo backup) {
        // TODO: Implement export to external file
        Toast.makeText(this, "Export to external file coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Confirm backup deletion
     */
    private void confirmDeleteBackup(BackupManager.BackupInfo backup) {
        String message = String.format(
                "Delete backup?\n\n" +
                        "File: %s\n" +
                        "Created: %s\n" +
                        "Size: %s\n\n" +
                        "This action cannot be undone.",
                backup.filename,
                backup.getFormattedDate(),
                backup.getFormattedSize()
        );

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Backup")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> deleteBackup(backup))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Delete backup file
     */
    private void deleteBackup(BackupManager.BackupInfo backup) {
        try {
            java.io.File backupFile = new java.io.File(backup.fullPath);
            boolean deleted = backupFile.delete();

            if (deleted) {
                showSuccess("Backup deleted: " + backup.filename);
            } else {
                showError("Failed to delete backup file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting backup", e);
            showError("Error deleting backup: " + e.getMessage());
        }
    }

    /**
     * Show detailed backup information
     */
    private void showBackupInformation(BackupManager.BackupInfo backup) {
        StringBuilder info = new StringBuilder();
        info.append("📁 Backup Information\n\n");
        info.append("Filename: ").append(backup.filename).append("\n");
        info.append("Full Path: ").append(backup.fullPath).append("\n");
        info.append("Size: ").append(backup.getFormattedSize()).append(" (").append(backup.sizeBytes).append(" bytes)\n");
        info.append("Created: ").append(backup.getFormattedDate()).append("\n");
        info.append("Modified: ").append(new java.util.Date(backup.createdTime)).append("\n");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Backup Information")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Export backup settings
     */
    private void exportBackupSettings() {
        // TODO: Implement backup settings export
        Toast.makeText(this, "Export backup settings coming soon", Toast.LENGTH_SHORT).show();
    }

    /// /////////////////////////////////////

    /**
     * Show confirmation for cleaning old backups
     */
    private void showCleanBackupsConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clean Backups")
                .setMessage("This will delete all backup files. This action cannot be undone.\n\nAre you sure?")
                .setPositiveButton("Clean All", (dialog, which) -> {
                    mBackupIntegration.getBackupManager().cleanAllBackups();
                    showSuccess("All backups cleaned");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== 5. UTILITY METHODS ====================

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Update menu items based on backup status
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Update backup menu items
        BackupIntegration.MenuItems menuItems = mBackupIntegration.getMenuItems();

        MenuItem autoBackupItem = menu.findItem(R.id.action_auto_backup_toggle);
        if (autoBackupItem != null) {
            autoBackupItem.setTitle(menuItems.getBackupStatusMenuTitle());
        }

        MenuItem restoreItem = menu.findItem(R.id.action_restore_backup);
        if (restoreItem != null) {
            restoreItem.setTitle(menuItems.getRestoreMenuTitle());
            restoreItem.setEnabled(menuItems.showRestoreOption);
        }

        return true;
    }

    /**
     * Setup date picker for EditText
     */
    private void setupDatePicker(EditText editText, String title) {
        editText.setOnClickListener(v -> {
            LocalDate currentDate = LocalDate.now();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                        editText.setText(selectedDate.toString());
                    },
                    currentDate.getYear(),
                    currentDate.getMonthValue() - 1,
                    currentDate.getDayOfMonth()
            );

            datePickerDialog.setTitle(title);
            datePickerDialog.show();
        });
    }

    /**
     * Enable/disable event type checkboxes
     */
    private void setEventTypeCheckboxesEnabled(LinearLayout layoutEventTypes, boolean enabled) {
        for (int i = 0; i < layoutEventTypes.getChildCount(); i++) {
            View child = layoutEventTypes.getChildAt(i);
            if (child instanceof CheckBox) {
                child.setEnabled(enabled);
                if (!enabled) {
                    ((CheckBox) child).setChecked(false);
                }
            }
        }
    }

    /**
     * Filter events based on dialog selections
     */
    private List<LocalEvent> filterEventsFromDialog(View dialogView) {
        List<LocalEvent> filteredEvents = new ArrayList<>(mEventsList);

        CheckBox dateFilter = dialogView.findViewById(R.id.checkbox_date_filter);
        CheckBox typeFilter = dialogView.findViewById(R.id.checkbox_type_filter);

        // Apply date filter
        if (dateFilter.isChecked()) {
            EditText editFromDate = dialogView.findViewById(R.id.edit_from_date);
            EditText editToDate = dialogView.findViewById(R.id.edit_to_date);

            String fromDateStr = editFromDate.getText().toString().trim();
            String toDateStr = editToDate.getText().toString().trim();

            if (!fromDateStr.isEmpty() && !toDateStr.isEmpty()) {
                try {
                    LocalDate fromDate = LocalDate.parse(fromDateStr);
                    LocalDate toDate = LocalDate.parse(toDateStr);

                    filteredEvents = filteredEvents.stream()
                            .filter(event -> {
                                LocalDate eventDate = event.getDate();
                                return !eventDate.isBefore(fromDate) && !eventDate.isAfter(toDate);
                            })
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing date filter: " + e.getMessage());
                }
            }
        }

        // Apply type filter
        if (typeFilter.isChecked()) {
            Set<EventType> selectedTypes = getSelectedEventTypes(dialogView);
            if (!selectedTypes.isEmpty()) {
                filteredEvents = filteredEvents.stream()
                        .filter(event -> selectedTypes.contains(event.getEventType()))
                        .collect(Collectors.toList());
            }
        }

        return filteredEvents;
    }

    /**
     * Get selected event types from checkboxes
     */
    private Set<EventType> getSelectedEventTypes(View dialogView) {
        Set<EventType> selectedTypes = new HashSet<>();

        CheckBox checkboxGeneral = dialogView.findViewById(R.id.checkbox_type_general);
        CheckBox checkboxStopPlanned = dialogView.findViewById(R.id.checkbox_type_stop_planned);
        CheckBox checkboxStopUnplanned = dialogView.findViewById(R.id.checkbox_type_stop_unplanned);
        CheckBox checkboxMaintenance = dialogView.findViewById(R.id.checkbox_type_maintenance);
        CheckBox checkboxMeeting = dialogView.findViewById(R.id.checkbox_type_meeting);

        if (checkboxGeneral.isChecked()) selectedTypes.add(EventType.GENERAL);
        if (checkboxStopPlanned.isChecked()) selectedTypes.add(EventType.STOP_PLANNED);
        if (checkboxStopUnplanned.isChecked()) selectedTypes.add(EventType.STOP_UNPLANNED);
        if (checkboxMaintenance.isChecked()) selectedTypes.add(EventType.MAINTENANCE);
        if (checkboxMeeting.isChecked()) selectedTypes.add(EventType.MEETING);

        return selectedTypes;
    }

    // ==================== ADAPTER CALLBACKS ====================

    @Override
    public void onEventClick(LocalEvent event) {
        // TODO: Open event details or edit dialog
        Toast.makeText(this, "Clicked: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventLongClick(LocalEvent event) {
        // TODO: Show context menu for edit/delete
        Toast.makeText(this, "Long clicked: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // ==================== EVENTS TESTING UTILITIES ====================

    /**
     * Debug method to test the events system (only in debug builds)
     */
    private void runEventsSystemTests() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Running events system tests in debug mode");
            EventsSystemTester.runAllTests(this);

            // Add test events to current list for manual testing
            List<LocalEvent> testEvents = EventsSystemTester.createTestEvents();
            mEventsList.addAll(testEvents);
            mEventsAdapter.notifyDataSetChanged();
            updateEmptyState();

            showSuccess("Test events added - check logs for test results\nRefresh for real events");
        } else {
            Toast.makeText(this, "Tests only available in debug mode", Toast.LENGTH_LONG).show();
        }
    }
}