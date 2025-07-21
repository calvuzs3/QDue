package net.calvuz.qdue.smartshifts.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult.OperationType;
import net.calvuz.qdue.smartshifts.utils.UiHelper;
import net.calvuz.qdue.smartshifts.ui.settings.fragments.RootSettingsFragment;
import net.calvuz.qdue.smartshifts.ui.settings.fragments.CalendarSettingsFragment;
//import net.calvuz.qdue.smartshifts.ui.settings.fragments.NotificationSettingsFragment;
//import net.calvuz.qdue.smartshifts.ui.settings.fragments.DataManagementSettingsFragment;
//import net.calvuz.qdue.smartshifts.ui.settings.fragments.AboutSettingsFragment;
import net.calvuz.qdue.smartshifts.ui.settings.viewmodel.SmartShiftsSettingsViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * SmartShifts Settings Activity - Refactored with UILibrary integration.
 * <p>
 * Now uses SmartShiftsUILibrary for all UI operations, providing:
 * - Consistent UI patterns across the application
 * - Centralized error and success handling
 * - Material Design 3 compliant components
 * - Unified operation result handling
 * - Shared utility methods for common operations
 * <p>
 * All UI feedback methods have been moved to SmartShiftsUILibrary
 * for reuse across other SmartShifts components.
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features (UILibrary Integration)
 */
@AndroidEntryPoint
public class SmartShiftsSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SmartShiftsSettings";

    // Fragment tags for navigation
    private static final String FRAGMENT_TAG_ROOT = "settings_root";
    private static final String FRAGMENT_TAG_CALENDAR = "settings_calendar";
    private static final String FRAGMENT_TAG_NOTIFICATIONS = "settings_notifications";
    private static final String FRAGMENT_TAG_DATA = "settings_data";
    private static final String FRAGMENT_TAG_ABOUT = "settings_about";

    private SmartShiftsSettingsViewModel viewModel;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private View progressContainer;
    private TextView progressText;
    private TextView progressSecondaryText;
    private MaterialButton cancelButton;
    private FloatingActionButton helpFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smartshifts_settings);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SmartShiftsSettingsViewModel.class);

        setupViews();
        setupFragments(savedInstanceState);
        observeViewModel();
    }

    /**
     * Setup all UI components
     */
    private void setupViews() {
        setupToolbar();
        setupProgressIndicators();
        setupHelpFab();
    }

    /**
     * Setup toolbar with back navigation and dynamic title
     */
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.smartshifts_settings_title);
        }
    }

    /**
     * Setup progress indicators with enhanced features using UILibrary utilities
     */
    private void setupProgressIndicators() {
        progressBar = findViewById(R.id.progress_bar_settings);
        progressContainer = findViewById(R.id.progress_container_settings);
        progressText = findViewById(R.id.progress_text_settings);
        progressSecondaryText = findViewById(R.id.progress_secondary_text_settings);

        // Cancel button for long operations
        cancelButton = findViewById(R.id.btn_cancel_operation);
        UiHelper.setOnClickListenerSafely(cancelButton, v -> cancelCurrentOperation());

        // Initially hidden using UILibrary utility
        UiHelper.setVisibilitySafely(progressContainer, View.GONE);
    }

    /**
     * Setup help FAB using UILibrary utilities
     */
    private void setupHelpFab() {
        helpFab = findViewById(R.id.fab_settings_help);
        UiHelper.setOnClickListenerSafely(helpFab, v -> showSettingsHelp());

        // Initially hidden - can be shown based on user preferences
        UiHelper.setVisibilitySafely(helpFab, View.GONE);
    }

    /**
     * Setup preference fragments navigation
     */
    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Load root settings fragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new RootSettingsFragment(), FRAGMENT_TAG_ROOT)
                    .commit();
        }

        // Handle fragment navigation from intent extras
        handleNavigationIntent(getIntent());
    }

    /**
     * Setup enhanced ViewModel observers - NEW METHOD
     */
    private void setupEnhancedViewModelObservers() {
        // ✅ Observe unified operation results
        viewModel.getOperationResults().observe(this, result -> {
            if (result != null) {
                UiHelper.handleOperationResult(this, result, () -> handleCustomSuccessActions(result));
            }
        });

        // ✅ Observe current operation info for secondary text
        viewModel.getCurrentOperationInfo().observe(this, operationInfo -> {
            if (operationInfo != null && progressSecondaryText != null) {
                if (operationInfo.isActive()) {
                    String text = "Eseguendo " + operationInfo.getOperationDisplayName() + "...";
                    String duration = operationInfo.getFormattedDuration();
                    String fullText = text + " (" + duration + ")";
                    UiHelper.setTextSafely(progressSecondaryText, fullText);
                } else {
                    UiHelper.setTextSafely(progressSecondaryText, "");
                }
            }
        });

        // ✅ Observe progress milestones for better UX
        viewModel.getProgressMilestone().observe(this, milestone -> {
            if (milestone != null && milestone.isImportant()) {
                UiHelper.showProgressMessage(this, milestone.getMessage());
            }
        });

        // ✅ Observe cancellation capability
        viewModel.getCanCancelOperation().observe(this, canCancel -> {
            UiHelper.setVisibilitySafely(
                    cancelButton,
                    (canCancel != null && canCancel) ? View.VISIBLE : View.GONE
            );
            UiHelper.setEnabledSafely(cancelButton, canCancel != null && canCancel);
        });

        // ✅ Observe cancellation results
        viewModel.getCancellationReason().observe(this, reason -> {
            if (reason != null) {
                if (reason.isSuccessful()) {
                    UiHelper.showSuccessMessage(this, reason.getReason());
                } else {
                    UiHelper.showWarningMessage(this, reason.getReason());
                }
            }
        });
    }


    /**
     * Handle custom success actions for specific operation types - NEW METHOD
     */
    private void handleCustomSuccessActions(UnifiedOperationResult<Object> result) {
        switch (result.getOperationType()) {
            case EXPORT_COMPLETE:
            case EXPORT_SELECTIVE:
            case BACKUP_NOW:
                if (result.hasFileOutput()) {
                    assert result.getResultFile() != null;
                    UiHelper.offerFileSharing(this, result.getResultFile(), "Condividi File");
                }
                break;

            case IMPORT_FILE:
            case IMPORT_CLOUD:
            case RESTORE_BACKUP:
                // Restart activity to reflect imported data
                recreate();
                break;

            case RESET_SETTINGS:
                // Restart activity to reflect reset settings
                recreate();
                break;

            default:
                // No custom action needed
                break;
        }
    }

    /**
     * Handle direct navigation to specific settings sections
     */
    private void handleNavigationIntent(Intent intent) {
        if (intent == null) return;

        String targetSection = intent.getStringExtra("target_section");
        if (targetSection != null) {
            navigateToSection(targetSection);
        }
    }

    /**
     * Navigate to specific settings section
     */
    public void navigateToSection(String section) {
        PreferenceFragmentCompat fragment = null;
        String tag = null;

        switch (section) {
            case "calendar":
                fragment = new CalendarSettingsFragment();
                tag = FRAGMENT_TAG_CALENDAR;
                break;
//            case "notifications":
//                fragment = new NotificationSettingsFragment();
//                tag = FRAGMENT_TAG_NOTIFICATIONS;
//                break;
//            case "data":
//                fragment = new DataManagementSettingsFragment();
//                tag = FRAGMENT_TAG_DATA;
//                break;
//            case "about":
//                fragment = new AboutSettingsFragment();
//                tag = FRAGMENT_TAG_ABOUT;
//                break;
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, tag)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Observe ViewModel for all changes using unified system and UILibrary
     */
    private void observeViewModel() {
        // Observe navigation events
        viewModel.getNavigationEvent().observe(this, event -> {
            if (event != null) {
                navigateToSection(event);
            }
        });

        // Observe toolbar title changes
        viewModel.getToolbarTitle().observe(this, title -> {
            if (getSupportActionBar() != null && title != null) {
                getSupportActionBar().setTitle(title);
            }
        });

        // Observe validation results using UILibrary
        viewModel.getValidationResults().observe(this, result -> {
            if (result != null) {
                UiHelper.handleOperationResult(this, result);
            }
        });

        // Observe all operation results using UILibrary
        viewModel.getOperationResults().observe(this, result -> {
            if (result != null) {
                handleOperationResultWithCustomActions(result);
            }
        });

        // Observe loading state using UILibrary utilities
        viewModel.getIsLoading().observe(this, isLoading -> {
            updateLoadingState(isLoading != null && isLoading);
        });

        // Observe operation progress
        viewModel.getOperationProgress().observe(this, progress -> {
            if (progress != null) {
                updateOperationProgress(progress);
            }
        });

        // ✅ Enhanced ViewModel Observers
        setupEnhancedViewModelObservers();
    }

    /**
     * Handle operation results with custom actions for specific operations
     */
    private void handleOperationResultWithCustomActions(UnifiedOperationResult<Object> result) {
        // Handle success with custom actions
        if (result.isSuccess()) {
            switch (result.getOperationType()) {
                case RESET_SETTINGS:
                    UiHelper.showSuccessDialog(
                            this,
                            result.getSuccessMessage() + "\n\nL'app verrà riavviata.",
                            () -> recreate()
                    );
                    break;

                case RESTORE_BACKUP:
                    UiHelper.showSuccessDialog(
                            this,
                            result.getSuccessMessage() + "\n\nI dati sono stati ripristinati. L'app verrà riavviata.",
                            () -> recreate()
                    );
                    break;

                case IMPORT_FILE:
                case IMPORT_CLOUD:
                    UiHelper.showSuccessDialog(
                            this,
                            formatImportSuccessMessage(result),
                            () -> recreate()
                    );
                    break;

                case EXPORT_COMPLETE:
                case EXPORT_SELECTIVE:
                case EXPORT_JSON:
                case EXPORT_CSV:
                case EXPORT_XML:
                case EXPORT_EXCEL:
                case EXPORT_ICAL:
                case BACKUP_NOW:
                    if (result.hasFileOutput()) {
                        UiHelper.showSuccessDialog(
                                this,
                                formatFileOutputSuccessMessage(result),
                                () -> UiHelper.offerFileSharing(this, result.getResultFile(), "Condividi File")
                        );
                    } else {
                        UiHelper.showSuccessMessage(this, result.getSuccessMessage());
                    }
                    break;

                default:
                    // Use standard UILibrary handling for other operations
                    UiHelper.handleOperationResult(this, result);
                    break;
            }
        } else {
            // Use standard UILibrary error handling
            UiHelper.handleOperationResult(this, result);
        }
    }

    /**
     * Format import success message with details
     */
    private String formatImportSuccessMessage(UnifiedOperationResult<Object> result) {
        StringBuilder message = new StringBuilder(result.getSuccessMessage());

        if (result.hasRecordCount()) {
            message.append("\nRecord importati: ").append(result.getRecordCount());
        }

        if (result.getMetadata() != null) {
            UnifiedOperationResult.OperationMetadata metadata = result.getMetadata();
            if (metadata.getRecordsSkipped() > 0) {
                message.append("\nRecord saltati: ").append(metadata.getRecordsSkipped());
            }
            if (metadata.getConflictsResolved() > 0) {
                message.append("\nConflitti risolti: ").append(metadata.getConflictsResolved());
            }
        }

        return message.toString();
    }

    /**
     * Format file output success message with details
     */
    private String formatFileOutputSuccessMessage(UnifiedOperationResult<Object> result) {
        StringBuilder message = new StringBuilder(result.getSuccessMessage());

        if (result.hasFileOutput()) {
            message.append("\nFile: ").append(result.getResultFile().getName());
        }

        if (result.hasRecordCount()) {
            message.append("\nRecord: ").append(result.getRecordCount());
        }

        if (result.getFileSize() > 0) {
            message.append("\nDimensione: ").append(UiHelper.formatBytes(result.getFileSize()));
        }

        return message.toString();
    }

    /**
     * Update loading state using UILibrary animations
     */
    private void updateLoadingState(boolean isLoading) {
        if (isLoading) {
            UiHelper.showLoadingWithAnimation(progressContainer);
        } else {
            UiHelper.hideLoadingWithAnimation(progressContainer);
        }
    }

    /**
     * Update operation progress with enhanced features
     */
    private void updateOperationProgress(SmartShiftsSettingsViewModel.OperationProgress progress) {
        // Update progress bar using UILibrary utility
        UiHelper.updateProgressWithAnimation(progressBar, progress.getProgress());

        // Update progress text using UILibrary utility
        UiHelper.setTextSafely(progressText, progress.getMessage());

        // ✅ Update secondary text using ViewModel method
        String secondaryText = viewModel.getSecondaryProgressText();
        UiHelper.setTextSafely(progressSecondaryText, secondaryText);

        // Show/hide cancel button for long operations
        boolean showCancel = progress.getProgress() > 10 && progress.getProgress() < 90;
        UiHelper.setVisibilitySafely(cancelButton, showCancel ? View.VISIBLE : View.GONE);

        // Update secondary text with operation type
        updateProgressSecondaryText();

        // Show progress message as snackbar for important milestones
        if (isImportantProgressMilestone(progress.getProgress())) {
            UiHelper.showProgressMessage(this, progress.getMessage());
        }
    }

//    /**
//     * Update secondary progress text with current operation details
//     */
//    private void updateProgressSecondaryText() {
//        if (viewModel.getExportImportManager() != null) {
//            var currentOp = viewModel.getExportImportManager().getCurrentOperation();
//            if (currentOp != null && progressSecondaryText != null) {
//                String operationName = currentOp.getType().name().toLowerCase().replace("_", " ");
//                String secondaryText = "Eseguendo " + operationName + "...";
//                UiHelper.setTextSafely(progressSecondaryText, secondaryText);
//            }
//        }
//    }

    /**
     * Update secondary progress text with current operation details - CORRECTED
     */
    private void updateProgressSecondaryText() {
        // ✅ SOLUZIONE 1: Usare il metodo dedicato del ViewModel
        String secondaryText = viewModel.getSecondaryProgressText();
        UiHelper.setTextSafely(progressSecondaryText, secondaryText);
    }

    /**
     * Check if progress represents an important milestone
     */
    private boolean isImportantProgressMilestone(int progress) {
        return progress == 25 || progress == 50 || progress == 75;
    }

    /**
     * Cancel current operation with confirmation
     */
    private void cancelCurrentOperation() {

        if (!viewModel.canCancelCurrentOperation()) {
            UiHelper.showWarningMessage(this, "Impossibile annullare l'operazione corrente");
            return;
        }

        UiHelper.showConfirmationDialog(
                this,
                "Annulla Operazione",
                "Sei sicuro di voler annullare l'operazione in corso?",
                "Annulla Operazione",
                "Continua",
                () -> {
                    // Chiamare il metodo ViewModel enhanced
                    viewModel.cancelCurrentOperation();

                    // Osservare il risultato della cancellazione
                    viewModel.getCancellationReason().observe(this, reason -> {
                        if (reason != null) {
                            if (reason.isSuccessful()) {
                                UiHelper.showSuccessMessage(this, reason.getReason());
                            } else {
                                UiHelper.showErrorMessage(this, reason.getReason());
                            }
                        }
                    });

                    updateLoadingState(false);
                },
                null
        );
//
//        UiHelper.showConfirmationDialog(
//                this,
//                "Annulla Operazione",
//                "Sei sicuro di voler annullare l'operazione in corso?",
//                "Annulla Operazione",
//                "Continua",
//                () -> {
//                    if (viewModel.getExportImportManager() != null) {
//                        viewModel.getExportImportManager().cancelCurrentOperation();
//                    }
//                    updateLoadingState(false);
//                    UiHelper.showWarningMessage(this, "Operazione annullata dall'utente");
//                },
//                null
//        );
    }

    /**
     * Show settings help dialog using UILibrary
     */
    private void showSettingsHelp() {
        String helpMessage = "Questa sezione permette di configurare tutte le impostazioni dell'app SmartShifts:\n\n" +
                "• Calendario: Personalizza visualizzazione e comportamento\n" +
                "• Notifiche: Gestisci promemoria e avvisi\n" +
                "• Dati: Backup, ripristino ed esportazione\n" +
                "• Informazioni: Versione app e supporto";

        UiHelper.showSuccessDialog(
                this,
                "Aiuto Impostazioni",
                helpMessage,
                null
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle back button in toolbar
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // Handle back button press
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    // ============================================
    // PUBLIC METHODS FOR FRAGMENTS
    // ============================================

    /**
     * Public method to update toolbar title from fragments
     */
    public void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    /**
     * Execute operation from fragments using unified system with confirmation
     */
    public void executeOperation(OperationType operationType) {
        executeOperation(operationType, null);
    }

    /**
     * Execute operation with configuration from fragments
     */
    public void executeOperation(OperationType operationType, Object configuration) {
        // Show confirmation dialog for destructive operations using UILibrary
        if (operationType.requiresConfirmation()) {
            showOperationConfirmationDialog(operationType, configuration);
        } else {
            viewModel.executeOperation(operationType, configuration);
        }
    }

    /**
     * Show confirmation dialog for destructive operations using UILibrary
     */
    private void showOperationConfirmationDialog(OperationType operationType, Object configuration) {
        String title = "Conferma " + operationType.getDisplayName();
        String message = getConfirmationMessage(operationType);

        UiHelper.showDestructiveConfirmationDialog(
                this,
                title,
                message,
                () -> viewModel.executeOperation(operationType, configuration)
        );
    }

    /**
     * Get confirmation message for operation type
     */
    private String getConfirmationMessage(OperationType operationType) {
        switch (operationType) {
            case RESET_SETTINGS:
                return "Tutte le impostazioni verranno ripristinate ai valori predefiniti. " +
                        "Questa operazione non può essere annullata. Continuare?";

            case CLEAR_CACHE:
                return "La cache dell'applicazione verrà eliminata. " +
                        "Questo potrebbe rallentare temporaneamente l'app. Continuare?";

            case RESTORE_BACKUP:
                return "I dati attuali verranno sostituiti con quelli del backup. " +
                        "Questa operazione non può essere annullata. Continuare?";

            case DATA_CLEANUP:
                return "I dati obsoleti e non validi verranno eliminati permanentemente. " +
                        "Questa operazione non può essere annullata. Continuare?";

            case DATA_MIGRATION:
                return "I dati verranno migrati al nuovo formato. " +
                        "Si consiglia di effettuare un backup prima di procedere. Continuare?";

            default:
                return "Confermi l'esecuzione dell'operazione " +
                        operationType.getDisplayName() + "?";
        }
    }

    /**
     * Helper method for fragments to access ViewModel (legacy compatibility)
     */
    public SmartShiftsSettingsViewModel getSettingsViewModel() {
        return viewModel;
    }

    /**
     * Show success message from fragments using UILibrary
     */
    public void showSuccessMessage(String message) {
        UiHelper.showSuccessMessage(this, message);
    }

    /**
     * Show error message from fragments using UILibrary
     */
    public void showErrorMessage(String message) {
        UiHelper.showErrorMessage(this, message);
    }

    /**
     * Show warning message from fragments using UILibrary
     */
    public void showWarningMessage(String message) {
        UiHelper.showWarningMessage(this, message);
    }

    /**
     * Show info message from fragments using UILibrary
     */
    public void showInfoMessage(String message) {
        UiHelper.showInfoMessage(this, message);
    }

    /**
     * Show validation errors from fragments using UILibrary
     */
    public void showValidationErrors(java.util.List<String> errors) {
        UiHelper.showValidationErrors(this, errors);
    }

    // ============================================
    // LEGACY COMPATIBILITY METHODS
    // ============================================

    /**
     * Legacy method for backward compatibility
     * @deprecated Use executeOperation(OperationType) instead
     */
    @Deprecated
    public void triggerDataOperation(SmartShiftsSettingsViewModel.DataOperationType operationType) {
        OperationType newOperationType = convertLegacyOperationType(operationType);
        executeOperation(newOperationType);
    }

    /**
     * Convert legacy operation type to new unified type
     */
    private OperationType convertLegacyOperationType(SmartShiftsSettingsViewModel.DataOperationType legacyType) {
        switch (legacyType) {
            case BACKUP_NOW:
                return OperationType.BACKUP_NOW;
            case RESTORE_BACKUP:
                return OperationType.RESTORE_BACKUP;
            case EXPORT_DATA:
                return OperationType.EXPORT_COMPLETE;
            case IMPORT_DATA:
                return OperationType.IMPORT_FILE;
            case RESET_SETTINGS:
                return OperationType.RESET_SETTINGS;
            case CLEAR_CACHE:
                return OperationType.CLEAR_CACHE;
            default:
                return OperationType.UNKNOWN;
        }
    }

    /**
     * Legacy message display method (now using UILibrary)
     * @deprecated Use SmartShiftsUILibrary methods directly
     */
    @Deprecated
    private void showMessage(String message) {
        UiHelper.showInfoMessage(this, message);
    }
}