package net.calvuz.qdue.smartshifts.ui.exportimport;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.databinding.ActivitySmartshiftsExportImportBinding;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;
import net.calvuz.qdue.smartshifts.ui.exportimport.viewmodel.ExportImportViewModel;

import java.io.File;
import java.time.LocalDate;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Advanced Export/Import Activity for SmartShifts.
 *
 * Provides comprehensive data export and import functionality including:
 * - Multiple export formats (JSON, CSV, XML, iCal, Excel)
 * - Selective data export with filtering options
 * - Intelligent import with conflict resolution
 * - Calendar format export for external apps
 * - Cloud storage integration
 * - Automated backup scheduling
 *
 * Features:
 * - Progress tracking with cancellation support
 * - File picker integration for import/export destinations
 * - Format-specific configuration options
 * - Data validation and error handling
 * - Batch operations for multiple files
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features
 */
@Deprecated
@AndroidEntryPoint
public class SmartShiftsExportImportActivity extends AppCompatActivity
        implements SmartShiftsExportImportManager.ExportImportListener {

    private static final String TAG = "SmartShiftsExportImport";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    // View binding
    private ActivitySmartshiftsExportImportBinding binding;

    // ViewModel
    private ExportImportViewModel viewModel;

    // Progress tracking
    private LinearProgressIndicator progressIndicator;
    private boolean operationInProgress = false;

    // File picker launchers
    private ActivityResultLauncher<Intent> exportDirectoryLauncher;
    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<String[]> multipleFilesLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySmartshiftsExportImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ExportImportViewModel.class);

        setupToolbar();
        setupViews();
        setupFilePickers();
        setupObservers();
        checkPermissions();
    }

    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Export/Import");
        }

        binding.toolbar.setNavigationOnClickListener(v -> {
            if (operationInProgress) {
                showCancelOperationDialog();
            } else {
                finish();
            }
        });
    }

    /**
     * Setup UI components and click listeners
     */
    private void setupViews() {
        progressIndicator = binding.progressIndicator;

        // Export section click listeners
        binding.cardExportComplete.setOnClickListener(v -> showExportCompleteDialog());
        binding.cardExportSelective.setOnClickListener(v -> showExportSelectiveDialog());
        binding.cardExportCalendar.setOnClickListener(v -> showExportCalendarDialog());
        binding.cardExportBackup.setOnClickListener(v -> showBackupDialog());

        // Import section click listeners
        binding.cardImportFile.setOnClickListener(v -> showImportFileDialog());
        binding.cardImportCloud.setOnClickListener(v -> showImportCloudDialog());
        binding.cardRestoreBackup.setOnClickListener(v -> showRestoreBackupDialog());

        // Quick actions
        binding.fabQuickExport.setOnClickListener(v -> performQuickExport());
        binding.btnCancelOperation.setOnClickListener(v -> cancelCurrentOperation());

        // Initial state
        updateUIState(false);
    }

    /**
     * Setup file picker activity launchers
     */
    private void setupFilePickers() {
        // Export directory picker
        exportDirectoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleExportDirectorySelected(uri);
                        }
                    }
                }
        );

        // Import file picker
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleImportFileSelected(uri);
                        }
                    }
                }
        );

        // Multiple files picker for batch operations
        multipleFilesLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        handleMultipleFilesSelected(uris);
                    }
                }
        );
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe export results
        viewModel.getExportResult().observe(this, result -> {
            if (result != null) {
                handleExportResult(result);
            }
        });

        // Observe import results
        viewModel.getImportResult().observe(this, result -> {
            if (result != null) {
                handleImportResult(result);
            }
        });

        // Observe available formats
        viewModel.getAvailableFormats().observe(this, formats -> {
            if (formats != null) {
                updateAvailableFormats(formats);
            }
        });

        // Observe operation status
        viewModel.getOperationStatus().observe(this, status -> {
            if (status != null) {
                updateOperationStatus(status);
            }
        });
    }

    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    // ============================================
    // EXPORT OPERATIONS
    // ============================================

    /**
     * Show complete export configuration dialog
     */
    private void showExportCompleteDialog() {
//        ExportCompleteDialogFragment dialog = new ExportCompleteDialogFragment();
//        dialog.setExportListener(this::performCompleteExport);
//        dialog.show(getSupportFragmentManager(), "export_complete");
    }

    /**
     * Show selective export configuration dialog
     */
    private void showExportSelectiveDialog() {
//        ExportSelectiveDialogFragment dialog = new ExportSelectiveDialogFragment();
//        dialog.setExportListener(this::performSelectiveExport);
//        dialog.show(getSupportFragmentManager(), "export_selective");
    }

    /**
     * Show calendar export configuration dialog
     */
    private void showExportCalendarDialog() {
//        ExportCalendarDialogFragment dialog = new ExportCalendarDialogFragment();
//        dialog.setExportListener(this::performCalendarExport);
//        dialog.show(getSupportFragmentManager(), "export_calendar");
    }

    /**
     * Show backup configuration dialog
     */
    private void showBackupDialog() {
//        BackupDialogFragment dialog = new BackupDialogFragment();
//        dialog.setBackupListener(this::performBackup);
//        dialog.show(getSupportFragmentManager(), "backup");
    }

    /**
     * Perform complete data export
     */
    private void performCompleteExport(SmartShiftsExportImportManager.ExportConfiguration config) {
        updateUIState(true);
        viewModel.exportCompleteData(config);
    }

    /**
     * Perform selective data export
     */
    private void performSelectiveExport(SmartShiftsExportImportManager.SelectiveExportConfiguration config) {
        updateUIState(true);
        viewModel.exportSelectiveData(config);
    }

    /**
     * Perform calendar export
     */
    private void performCalendarExport(SmartShiftsExportImportManager.CalendarExportConfiguration config) {
        updateUIState(true);
        viewModel.exportToCalendar(config);
    }

    /**
     * Perform backup operation
     */
    private void performBackup(SmartShiftsExportImportManager.BackupConfiguration config) {
        updateUIState(true);
        viewModel.executeBackup(config);
    }

    /**
     * Perform quick export with default settings
     */
    private void performQuickExport() {
        // Quick export with JSON format to Downloads folder
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        SmartShiftsExportImportManager.ExportConfiguration config =
                new SmartShiftsExportImportManager.ExportConfiguration();
        config.setFormat(SmartShiftsExportImportManager.FORMAT_JSON);
        config.setDestination(downloadsDir.getAbsolutePath());
        config.setExportType(SmartShiftsExportImportManager.EXPORT_TYPE_COMPLETE);
        config.setStartDate(LocalDate.now().minusMonths(3));
        config.setEndDate(LocalDate.now().plusMonths(3));

        updateUIState(true);
        viewModel.exportCompleteData(config);
    }

    // ============================================
    // IMPORT OPERATIONS
    // ============================================

    /**
     * Show import file dialog
     */
    private void showImportFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Set supported file types
        String[] mimeTypes = {
                "application/json",
                "text/csv",
                "application/xml",
                "text/calendar",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        importFileLauncher.launch(intent);
    }

    /**
     * Show cloud import dialog
     */
    private void showImportCloudDialog() {
//        CloudImportDialogFragment dialog = new CloudImportDialogFragment();
//        dialog.setImportListener(this::performCloudImport);
//        dialog.show(getSupportFragmentManager(), "import_cloud");
    }

    /**
     * Show restore backup dialog
     */
    private void showRestoreBackupDialog() {
//        RestoreBackupDialogFragment dialog = new RestoreBackupDialogFragment();
//        dialog.setRestoreListener(this::performRestoreBackup);
//        dialog.show(getSupportFragmentManager(), "restore_backup");
    }

    /**
     * Handle import file selection
     */
    private void handleImportFileSelected(Uri uri) {
        // Show import configuration dialog
//        ImportConfigurationDialogFragment dialog = new ImportConfigurationDialogFragment();
//        dialog.setFileUri(uri);
//        dialog.setImportListener(config -> performFileImport(config));
//        dialog.show(getSupportFragmentManager(), "import_config");
    }

    /**
     * Handle multiple files selection for batch import
     */
    private void handleMultipleFilesSelected(java.util.List<Uri> uris) {
//        BatchImportDialogFragment dialog = new BatchImportDialogFragment();
//        dialog.setFileUris(uris);
//        dialog.setImportListener(this::performBatchImport);
//        dialog.show(getSupportFragmentManager(), "batch_import");
    }

    /**
     * Handle export directory selection
     */
    private void handleExportDirectorySelected(Uri uri) {
        // Convert URI to file path if possible
        String path = getPathFromUri(uri);
        if (path != null) {
            // Update current export configuration with selected directory
            viewModel.setExportDestination(path);
            showMessage("Directory selezionata: " + path);
        }
    }

    /**
     * Perform file import
     */
    private void performFileImport(SmartShiftsExportImportManager.ImportConfiguration config) {
        updateUIState(true);
        viewModel.importFromFile(config);
    }

    /**
     * Perform cloud import
     */
    private void performCloudImport(SmartShiftsExportImportManager.CloudImportConfiguration config) {
        updateUIState(true);
        viewModel.importFromCloud(config);
    }

    /**
     * Perform backup restore
     */
    private void performRestoreBackup(SmartShiftsExportImportManager.ImportConfiguration config) {
        updateUIState(true);
        viewModel.restoreFromBackup(config);
    }

    /**
     * Perform batch import of multiple files
     */
    private void performBatchImport(java.util.List<SmartShiftsExportImportManager.ImportConfiguration> configs) {
        updateUIState(true);
//        viewModel.performBatchImport(configs);
    }

    // ============================================
    // OPERATION MANAGEMENT
    // ============================================

    /**
     * Cancel current operation
     */
    private void cancelCurrentOperation() {
        if (operationInProgress) {
            viewModel.cancelCurrentOperation();
            updateUIState(false);
            showMessage("Operazione annullata");
        }
    }

    /**
     * Show cancel operation confirmation dialog
     */
    private void showCancelOperationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Annulla Operazione")
                .setMessage("Sei sicuro di voler annullare l'operazione in corso?")
                .setPositiveButton("Annulla Operazione", (dialog, which) -> {
                    cancelCurrentOperation();
                    finish();
                })
                .setNegativeButton("Continua", null)
                .show();
    }

    // ============================================
    // UI STATE MANAGEMENT
    // ============================================

    /**
     * Update UI state based on operation progress
     */
    private void updateUIState(boolean inProgress) {
        operationInProgress = inProgress;

        if (inProgress) {
            // Show progress indicators
            binding.layoutProgress.setVisibility(View.VISIBLE);
            binding.layoutMainContent.setAlpha(0.5f);
            binding.fabQuickExport.setEnabled(false);
            progressIndicator.setIndeterminate(true);

            // Update toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Operazione in corso...");
            }
        } else {
            // Hide progress indicators
            binding.layoutProgress.setVisibility(View.GONE);
            binding.layoutMainContent.setAlpha(1.0f);
            binding.fabQuickExport.setEnabled(true);
            progressIndicator.setProgress(0);

            // Reset toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Export/Import");
            }
        }
    }

    /**
     * Update operation status display
     */
    private void updateOperationStatus(String status) {
        binding.textOperationStatus.setText(status);
    }

    /**
     * Update available formats display
     */
    private void updateAvailableFormats(java.util.List<SmartShiftsExportImportManager.ExportFormat> formats) {
        // Update format chips or spinner
        // This would populate UI elements showing available export formats
    }

    // ============================================
    // RESULT HANDLERS
    // ============================================

    /**
     * Handle export operation result
     */
    private void handleExportResult(SmartShiftsExportImportManager.ExportResult result) {
        updateUIState(false);

        if (result.isSuccess()) {
            showExportSuccessDialog(result);
        } else {
            showErrorDialog("Export Fallito", result.getErrorMessage());
        }
    }

    /**
     * Handle import operation result
     */
    private void handleImportResult(SmartShiftsExportImportManager.ImportResult result) {
        updateUIState(false);

        if (result.isSuccess()) {
            showImportSuccessDialog(result);
        } else {
            showErrorDialog("Import Fallito", result.getErrorMessage());
        }
    }

    /**
     * Show export success dialog with sharing options
     */
    private void showExportSuccessDialog(SmartShiftsExportImportManager.ExportResult result) {
        String message = String.format(
                "Export completato con successo!\n\n" +
                        "File: %s\n" +
                        "Records: %d\n" +
                        "Dimensione: %s\n" +
                        "Formato: %s",
                result.getExportFile().getName(),
                result.getRecordCount(),
                formatFileSize(result.getFileSize()),
                result.getFormat().toUpperCase()
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Completato")
                .setMessage(message)
                .setPositiveButton("Condividi", (dialog, which) -> shareExportFile(result.getExportFile()))
                .setNegativeButton("Chiudi", null)
                .setNeutralButton("Apri Cartella", (dialog, which) -> openFileLocation(result.getExportFile()))
                .show();
    }

    /**
     * Show import success dialog
     */
    private void showImportSuccessDialog(SmartShiftsExportImportManager.ImportResult result) {
        String message = String.format(
                "Import completato con successo!\n\n" +
                        "Records importati: %d\n" +
                        "Records saltati: %d",
                result.getImportedRecords(),
                result.getSkippedRecords()
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle("Import Completato")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ============================================
    // EXPORTIMPORTLISTENER IMPLEMENTATION
    // ============================================

    @Override
    public void onProgress(int progress, String message) {
        runOnUiThread(() -> {
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgress(progress);
            updateOperationStatus(message);
        });
    }

    @Override
    public void onSuccess(Object result) {
        runOnUiThread(() -> {
            if (result instanceof SmartShiftsExportImportManager.ExportResult) {
                handleExportResult((SmartShiftsExportImportManager.ExportResult) result);
            } else if (result instanceof SmartShiftsExportImportManager.ImportResult) {
                handleImportResult((SmartShiftsExportImportManager.ImportResult) result);
            } else if (result instanceof SmartShiftsExportImportManager.BackupResult) {
                handleBackupResult((SmartShiftsExportImportManager.BackupResult) result);
            }
        });
    }

    @Override
    public void onError(Exception error) {
        runOnUiThread(() -> {
            updateUIState(false);
            showErrorDialog("Errore", error.getMessage());
        });
    }

    /**
     * Handle backup operation result
     */
    private void handleBackupResult(SmartShiftsExportImportManager.BackupResult result) {
        updateUIState(false);

        if (result.isSuccess()) {
            String message = String.format(
                    "Backup completato con successo!\n\n" +
                            "File: %s\n" +
                            "Dimensione: %s\n" +
                            "Data: %s",
                    result.getBackupFile().getName(),
                    formatFileSize(result.getBackupSize()),
                    formatTimestamp(result.getTimestamp())
            );

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Backup Completato")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            showErrorDialog("Backup Fallito", result.getErrorMessage());
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Share export file using system share dialog
     */
    private void shareExportFile(File file) {
        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SmartShifts Export");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Export dati SmartShifts");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Condividi Export"));
    }

    /**
     * Open file location in file manager
     */
    private void openFileLocation(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file.getParentFile()
        );
        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            showMessage("Impossibile aprire la cartella");
        }
    }

    /**
     * Get file path from content URI
     */
    private String getPathFromUri(Uri uri) {
        // This is a simplified implementation
        // In practice, you might need to use DocumentsContract for Android 10+
        return uri.getPath();
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(long timestamp) {
        return java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(timestamp));
    }

    /**
     * Show message using Snackbar
     */
    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage("Permessi storage concessi");
            } else {
                showMessage("Permessi storage necessari per l'export/import");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (operationInProgress) {
            viewModel.cancelCurrentOperation();
        }
    }

    @Override
    public void onBackPressed() {
        if (operationInProgress) {
            showCancelOperationDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        return super.deleteSharedPreferences(name);
    }
}