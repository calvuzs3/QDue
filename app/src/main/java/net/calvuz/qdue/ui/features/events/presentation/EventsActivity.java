package net.calvuz.qdue.ui.features.events.presentation;

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
import net.calvuz.qdue.core.backup.ExportManager;
import net.calvuz.qdue.core.db.QDueDatabase;
import net.calvuz.qdue.ui.core.architecture.services.BackHandlingServiceImpl;
import net.calvuz.qdue.ui.features.events.components.imports.FileAccessAdapter;
import net.calvuz.qdue.ui.features.events.components.imports.EventsImportAdapter;
import net.calvuz.qdue.ui.core.common.permissions.PermissionManager;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlerFactory;
import net.calvuz.qdue.ui.core.architecture.di.BackHandlingModule;
import net.calvuz.qdue.ui.core.common.interfaces.BackHandlingService;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.interfaces.UnsavedChangesHandler;
import net.calvuz.qdue.events.dao.EventDao;
import net.calvuz.qdue.core.backup.BackupIntegration;
import net.calvuz.qdue.events.imports.EventsImportManager;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.core.common.listeners.EventDeletionListener;
import net.calvuz.qdue.core.common.interfaces.EventsDatabaseOperationsInterface;
import net.calvuz.qdue.core.common.interfaces.EventsOperationsInterface;
import net.calvuz.qdue.ui.core.common.interfaces.EventsFileOperationsInterface;
import net.calvuz.qdue.ui.features.events.interfaces.EventsUIStateInterface;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Main activity for events management with navigation and CRUD operations.
 * <p>
 * <p>Provides comprehensive event management including:
 * <ul>
 *   <li>Navigation between events list and detail fragments</li>
 *   <li>File import/export with validation</li>
 *   <li>Event creation, editing, and deletion</li>
 *   <li>Integration with system calendar</li>
 *   <li>Backup and restore functionality</li>
 * </ul>
 * <p>
 * <p>Architecture follows Navigation Component pattern with fragment-based UI
 * and dependency injection for service management.
 */
public class EventsActivity extends AppCompatActivity implements
        EventsFileOperationsInterface,
        EventsDatabaseOperationsInterface,
        EventsOperationsInterface,
        EventsUIStateInterface {

    private static final String TAG = "EventsActivity";

    // ==================== ACTIVITY RESULT CONSTANTS ====================

    public static final String EXTRA_EVENTS_CHANGED = "events_changed";
    public static final String EXTRA_EVENTS_COUNT = "events_count";
    public static final String EXTRA_CHANGE_TYPE = "change_type";

    public static final String CHANGE_TYPE_IMPORT = "import";
    public static final String CHANGE_TYPE_DELETE = "delete";
    public static final String CHANGE_TYPE_CREATE = "create";
    public static final String CHANGE_TYPE_MODIFY = "modify";

    // ==================== NAVIGATION COMPONENTS ====================

    private NavController mNavController;
    private NavHostFragment mNavHostFragment;

    // ==================== DEPENDENCY INJECTION ====================

    private BackHandlerFactory mBackHandlerFactory;
    private BackHandlingService mBackHandlingService;

    // ==================== UI COMPONENTS ====================

    private MaterialToolbar mToolbar;
    private FloatingActionButton mFabAddEvent;
    private View mLoadingStateView;

    // ==================== STATE MANAGEMENT ====================

    private boolean mHasEvents = false;
    private int mTotalEventsCount = 0;
    private EventsListFragment mCurrentListFragment;

    // ==================== FILE OPERATIONS ====================

    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mFileSaverLauncher;
    private FileAccessAdapter mFileAccessAdapter;
    private EventsImportAdapter mEventsImportAdapter;
    private PermissionManager mPermissionManager;
    private List<LocalEvent> mPendingExportEvents = null;

    // ==================== DATA LAYER ====================

    private QDueDatabase mDatabase;
    private ExportManager mExportManager;

    // ==================== PENDING OPERATIONS ====================

    private PendingEventCreation mPendingEventCreation = null;

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        mDatabase = QDueDatabase.getInstance(this);

        initializeViews();
        initializeDependencies();
        setupToolbar();
        setupNavigation();
        setupBackHandling();
        setupFab();
        setupFileOperations();

        handleIntentWithNavigation(getIntent());
        checkInitialEventsState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPendingEventCreation = null;

        if (mFileAccessAdapter != null) {
            mFileAccessAdapter.clearPendingCallback();
        }

        if (mPermissionManager != null) {
            mPermissionManager.clearPendingCallbacks();
        }

        mBackHandlingService.unregisterComponent(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInitialEventsState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentWithNavigation(intent);
    }

    // ==================== BACK PRESS HANDLING ====================

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

    /**
     * Gets the currently active fragment from the navigation host.
     * <p>
     *
     * @return Current fragment or null if not found
     */
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

    // ==================== INITIALIZATION METHODS ====================

    private void initializeViews() {
        mToolbar = findViewById(R.id.toolbar_events);
        mFabAddEvent = findViewById(R.id.fab_add_event);
        mLoadingStateView = findViewById(R.id.loading_state_events);
    }

    private void initializeDependencies() {
        mBackHandlingService = BackHandlingModule.getBackHandlingService(this);
        mBackHandlerFactory = BackHandlingModule.getBackHandlerFactory(this);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_eventi);
        }

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     * Sets up Navigation Component with fragment management and destination listeners.
     */
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

    private void setupBackHandling() {
        mBackHandlerFactory.forComponent(this)
                .withPriority(10)
                .withDescription("EventsActivity navigation handler")
                .register(this::handleActivityNavigation);
    }

    private void setupFab() {
        mFabAddEvent.setOnClickListener(v -> triggerCreateNewEvent());
        mFabAddEvent.hide();
    }

    /**
     * Sets up file operation launchers for import/export functionality.
     */
    private void setupFileOperations() {
        mFileAccessAdapter = new FileAccessAdapter(this);
        mEventsImportAdapter = new EventsImportAdapter(this);
        mPermissionManager = new PermissionManager(this);

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

        mFileSaverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleExportEventsToFile(fileUri);
                        }
                    }
                });
    }

    // ==================== NAVIGATION MANAGEMENT ====================

    private void onNavigationDestinationChanged(int destinationId, Bundle arguments) {
        if (destinationId == R.id.nav_events_list) {
            updateFabVisibility();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.nav_eventi);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updateCurrentListFragmentReference();
                checkAndExecutePendingEventCreation();
            }, 100);

        } else if (destinationId == R.id.nav_event_detail) {
            mFabAddEvent.hide();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.event_detail_title);
            }
        }
    }

    private void updateCurrentListFragmentReference() {
        try {
            if (mNavHostFragment != null) {
                var currentFragment = mNavHostFragment.getChildFragmentManager()
                        .getPrimaryNavigationFragment();
                if (currentFragment instanceof EventsListFragment) {
                    mCurrentListFragment = (EventsListFragment) currentFragment;
                } else {
                    mCurrentListFragment = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating list fragment reference: " + e.getMessage());
            mCurrentListFragment = null;
        }
    }

    private boolean handleActivityNavigation() {
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

    // ==================== INTENT HANDLING ====================

    /**
     * Handles incoming intents with support for direct navigation to specific fragments.
     * <p>
     *
     * @param intent The intent to process
     */
    private void handleIntentWithNavigation(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getStringExtra("action");

        switch (action != null ? action : "") {
            case "view_detail":
                handleDirectDetailNavigation(intent);
                break;
            case "edit_event":
                handleDirectEditorNavigation(intent);
                break;
            case "create_event":
                handleNewEventCreation(intent);
                break;
            case "view_list":
                handleListNavigationWithFocus(intent);
                break;
            default:
                handleIntent(intent);
                break;
        }
    }

    private void handleDirectDetailNavigation(Intent intent) {
        String eventId = intent.getStringExtra("event_id");
        String sourceDate = intent.getStringExtra("source_date");
        String sourceFragment = intent.getStringExtra("source_fragment");

        if (eventId == null || eventId.trim().isEmpty()) {
            Library.showToast(this, "Errore: ID evento non valido");
            return;
        }

        verifyEventAndNavigate(eventId, sourceDate, sourceFragment, "view_detail");
    }

    private void handleDirectEditorNavigation(Intent intent) {
        String eventId = intent.getStringExtra("event_id");
        String sourceDate = intent.getStringExtra("source_date");
        String sourceFragment = intent.getStringExtra("source_fragment");

        if (eventId == null || eventId.trim().isEmpty()) {
            Library.showToast(this, "Errore: ID evento non valido");
            return;
        }

        verifyEventAndNavigate(eventId, sourceDate, sourceFragment, "edit_event");
    }

    private void handleNewEventCreation(Intent intent) {
        String targetDate = intent.getStringExtra("target_date");
        String sourceFragment = intent.getStringExtra("source_fragment");
        String editMode = intent.getStringExtra("edit_mode");

        if (targetDate == null || targetDate.trim().isEmpty()) {
            targetDate = LocalDate.now().toString();
        }

        mPendingEventCreation = new PendingEventCreation(targetDate, sourceFragment, editMode);
    }

    private void handleListNavigationWithFocus(Intent intent) {
        // Events list is the default destination
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri fileUri = intent.getData();
            if (fileUri != null) {
                String scheme = fileUri.getScheme();
                if ("file".equals(scheme) || "content".equals(scheme)) {
                    importEventsFromFile(fileUri);
                }
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) {
                importEventsFromFile(sharedUri);
            }
        }
    }

    /**
     * Verifies event exists in database before navigation.
     * <p>
     *
     * @param eventId        Event ID to verify
     * @param sourceDate     Source date context
     * @param sourceFragment Source fragment name
     * @param action         Navigation action to perform
     */
    private void verifyEventAndNavigate(String eventId, String sourceDate, String sourceFragment, String action) {
        CompletableFuture.supplyAsync(() -> {
            try {
                EventDao eventDao = mDatabase.eventDao();
                return eventDao.getEventById(eventId);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(event -> {
            runOnUiThread(() -> {
                if (event != null) {
                    switch (action) {
                        case "edit_event":
                            navigateToEventEditor(eventId, sourceDate, sourceFragment);
                            break;
                        case "view_detail":
                            navigateToEventDetail(eventId, sourceDate, sourceFragment);
                            break;
                    }
                } else {
                    Library.showToast(this, "Evento non trovato");
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> Library.showToast(this, "Errore nel caricamento dell'evento"));
            return null;
        });
    }

    private void navigateToEventDetail(String eventId, String sourceDate, String sourceFragment) {
        if (mNavController == null) return;

        try {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            args.putString("sourceDate", sourceDate);
            args.putString("sourceFragment", sourceFragment);

            mNavController.navigate(R.id.action_events_list_to_event_detail, args);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            Library.showToast(this, "Errore nella navigazione");
        }
    }

    private void navigateToEventEditor(String eventId, String sourceDate, String sourceFragment) {
        if (mNavController == null) return;

        try {
            Bundle detailArgs = new Bundle();
            detailArgs.putString("eventId", eventId);
            detailArgs.putString("sourceDate", sourceDate);
            detailArgs.putString("sourceFragment", sourceFragment);

            mNavController.navigate(R.id.action_events_list_to_event_detail, detailArgs);

            Bundle editArgs = new Bundle();
            editArgs.putString("eventId", eventId);
            editArgs.putString("sourceDate", sourceDate);
            editArgs.putString("sourceFragment", sourceFragment);
            editArgs.putString("editMode", "existing_event");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    mNavController.navigate(R.id.action_event_detail_to_edit, editArgs);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to edit: " + e.getMessage());
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            Library.showToast(this, "Errore nella navigazione all'editor");
        }
    }

    // ==================== EVENT CREATION ====================

    public void handleCreateNewEvent() {
        showCreateEventDialog();
    }

    private void showCreateEventDialog() {
        showCreateEventDialog(LocalDate.now());
    }

    /**
     * Shows dialog for creating new event with specified date.
     * <p>
     *
     * @param date Target date for the new event
     */
    private void showCreateEventDialog(LocalDate date) {
        if (mCurrentListFragment == null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mCurrentListFragment != null) {
                    showCreateEventDialog(date);
                } else if (mPendingEventCreation == null) {
                    mPendingEventCreation = new PendingEventCreation(
                            date.toString(), "delayed_retry", "new_event"
                    );
                }
            }, 300);
            return;
        }

        EditText editTitle = new EditText(this);
        editTitle.setHint("Titolo evento");
        editTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editTitle.setSingleLine(true);

        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        new AlertDialog.Builder(this)
                .setTitle("Nuovo Evento")
                .setMessage("Crea evento per " + formattedDate + ":")
                .setView(editTitle)
                .setPositiveButton("Crea", (dialog, which) -> {
                    String title = editTitle.getText().toString().trim();
                    if (!title.isEmpty()) {
                        createNewEvent(title, date);
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();

        editTitle.requestFocus();
    }

    /**
     * Creates new event in database and updates UI.
     * <p>
     *
     * @param title Event title
     * @param date  Event date
     */
    private void createNewEvent(@NonNull String title, @NonNull LocalDate date) {
        new Thread(() -> {
            try {
                LocalEvent newEvent = new LocalEvent(title, date);
                newEvent.setDescription("My Event");

                long result = mDatabase.eventDao().insertEvent(newEvent);

                runOnUiThread(() -> {
                    if (result > 0) {
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.onEventCreated(newEvent);
                        }

                        showSuccess("✅ " + title);

                        mHasEvents = true;
                        mTotalEventsCount++;
                        updateFabVisibility();
                        notifyEventsChanged(CHANGE_TYPE_CREATE, 1);
                    } else {
                        showError("❌ Error creating event " + title);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating event: " + e.getMessage());
                runOnUiThread(() -> showError("❌ Error creating event"));
            }
        }).start();
    }

    private void checkAndExecutePendingEventCreation() {
        if (mPendingEventCreation == null) {
            return;
        }

        try {
            LocalDate targetDate = LocalDate.parse(mPendingEventCreation.targetDate);
            showCreateEventDialog(targetDate);
        } catch (Exception e) {
            showCreateEventDialog(LocalDate.now());
        } finally {
            mPendingEventCreation = null;
        }
    }

    // ==================== FILE IMPORT/EXPORT ====================

    /**
     * Imports events from selected file with validation and progress feedback.
     * <p>
     *
     * @param fileUri URI of file to import
     */
    private void importEventsFromFile(Uri fileUri) {
        try {
            String fileInfo = mFileAccessAdapter.getFileDisplayInfo(fileUri);

            if (!mFileAccessAdapter.isSupportedFile(fileUri)) {
                showError("❌ Unsupported file type. " + FileAccessAdapter.getSupportedFileTypesDescription());
                return;
            }

            showImportDialog(fileUri);

        } catch (Exception e) {
            Log.e(TAG, "Error starting import", e);
            showError("❌ Error reading file: " + e.getMessage());
        }
    }

    private void showImportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import, null);

        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Import Options")
                .setView(dialogView)
                .setPositiveButton(R.string.text_import, null)
                .setNegativeButton(R.string.text_cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button advancedImport = dialog.findViewById(R.id.btn_advanced_import);
            advancedImport.setOnClickListener(v -> {
                dialog.dismiss();
                showAdvancedImportDialog(fileUri);
            });

            Button importButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            importButton.setOnClickListener(v -> {
                EventsImportManager.ImportOptions options = createImportOptionsFromDialog(dialogView);
                dialog.dismiss();
                performImport(fileUri, options);
            });
        });

        dialog.show();
    }

    private void showAdvancedImportDialog(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_advanced_import, null);

        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        CheckBox checkboxShowWarnings = dialogView.findViewById(R.id.checkbox_show_warnings);
        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        RadioButton radioSkipDuplicates = dialogView.findViewById(R.id.radio_skip_duplicates);
        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);
        CheckBox checkboxPreserveExisting = dialogView.findViewById(R.id.checkbox_preserve_existing);
        CheckBox checkboxAllowPartialImport = dialogView.findViewById(R.id.checkbox_allow_partial_import);

        checkboxValidateBeforeImport.setChecked(true);
        checkboxShowWarnings.setChecked(true);
        radioSkipDuplicates.setChecked(true);
        checkboxShowProgress.setChecked(true);
        checkboxPreserveExisting.setChecked(true);
        checkboxAllowPartialImport.setChecked(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Advanced Import Options")
                .setView(dialogView)
                .setPositiveButton(R.string.text_import, null)
                .setNegativeButton(R.string.text_cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button importButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            importButton.setOnClickListener(v -> {
                EventsImportManager.ImportOptions options = createImportOptionsFromAdvancedDialog(dialogView);
                dialog.dismiss();
                performImport(fileUri, options);
            });
        });

        dialog.show();
    }

    private EventsImportManager.ImportOptions createImportOptionsFromDialog(View dialogView) {
        EventsImportManager.ImportOptions options = new EventsImportManager.ImportOptions();

        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        options.validateBeforeImport = checkboxValidateBeforeImport.isChecked();

        RadioGroup radioGroupConflictResolution = dialogView.findViewById(R.id.radio_group_conflict_resolution);
        int selectedRadioId = radioGroupConflictResolution.getCheckedRadioButtonId();

        if (selectedRadioId == R.id.radio_skip_duplicates) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.SKIP_DUPLICATE;
        } else if (selectedRadioId == R.id.radio_replace_existing) {
            options.conflictResolution = EventsImportManager.ImportOptions.ConflictResolution.REPLACE_EXISTING;
        }

        return options;
    }

    private EventsImportManager.ImportOptions createImportOptionsFromAdvancedDialog(View dialogView) {
        EventsImportManager.ImportOptions options = new EventsImportManager.ImportOptions();

        CheckBox checkboxValidateBeforeImport = dialogView.findViewById(R.id.checkbox_validate_before_import);
        options.validateBeforeImport = checkboxValidateBeforeImport.isChecked();

        CheckBox checkboxShowProgress = dialogView.findViewById(R.id.checkbox_show_progress);
        options.reportProgress = checkboxShowProgress.isChecked();

        CheckBox checkboxPreserveExisting = dialogView.findViewById(R.id.checkbox_preserve_existing);
        options.preserveExistingEvents = checkboxPreserveExisting.isChecked();

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
     * Performs import operation with progress tracking and error handling.
     * <p>
     *
     * @param fileUri File to import
     * @param options Import configuration options
     */
    private void performImport(Uri fileUri, EventsImportManager.ImportOptions options) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Importing Events");
        progressDialog.setMessage("Reading file...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

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
                    progressDialog.setMessage(String.format(QDue.getLocale(),
                            "Importing... (%d/%d) %s", processed, total, currentEvent));
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
                });
            }
        });
    }

    private void handleSuccessfulImport(EventsImportManager.ImportResult result) {
        showImportResultDialog(result);

        if (result.success && result.importedEvents > 0) {
            if (mCurrentListFragment != null) {
                mCurrentListFragment.refreshEvents();
            }

            mHasEvents = true;
            mTotalEventsCount += result.importedEvents;
            updateFabVisibility();
            notifyEventsChanged(CHANGE_TYPE_IMPORT, result.importedEvents);

            BackupIntegration.integrateWithImport(
                    EventsActivity.this,
                    mCurrentListFragment != null ? mCurrentListFragment.getEventsList() : null,
                    result.importedEvents
            );
        }
    }

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message.toString())
                .setPositiveButton("OK", null);

        if (result.success && result.importedEvents > 0) {
            builder.setNeutralButton("View Events", (dialog, which) -> {
                Toast.makeText(this, "Showing " + result.importedEvents + " imported events", Toast.LENGTH_SHORT).show();
            });
        }

        if (result.hasIssues()) {
            builder.setNegativeButton("Export Log", (dialog, which) -> {
                Toast.makeText(this, "Export log functionality coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    public void handleImportEventsFromUrl() {
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    public void handleExportEventsToFile(@Nullable Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        if (fileUri != null) {
            intent.putExtra(Intent.EXTRA_TITLE, fileUri.getLastPathSegment());
        } else {
            intent.putExtra(Intent.EXTRA_TITLE, "qdue_events_" + System.currentTimeMillis() + ".json");
        }

        try {
            mFileSaverLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file saver: " + e.getMessage());
            Toast.makeText(this, "Errore apertura file saver", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exports selected events to file with user-friendly filename generation.
     * <p>
     *
     * @param fileUri        Optional target file URI
     * @param selectedEvents List of events to export
     */
    public void handleExportSelectedEventsToFile(Uri fileUri, List<LocalEvent> selectedEvents) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename;

        if (fileUri != null) {
            filename = fileUri.getLastPathSegment();
        } else {
            filename = "qdue_events_" + selectedEvents.size() + "_" + timestamp + ".json";
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        try {
            mPendingExportEvents = selectedEvents;
            mFileSaverLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file saver for selected events: " + e.getMessage());
            Toast.makeText(this, "Errore apertura file saver", Toast.LENGTH_SHORT).show();
            mPendingExportEvents = null;
        }
    }

    private void exportSelectedEventsToFile(Uri fileUri, List<LocalEvent> selectedEvents) {
        try {
            showGlobalLoading(true, "Esportando " + selectedEvents.size() + " eventi selezionati...");

            if (mExportManager != null) {
                ExportManager.ExportOptions options = new ExportManager.ExportOptions();
                options.includeCustomProperties = true;

                mExportManager.exportToUri(selectedEvents, fileUri, options, new ExportManager.ExportCallback() {
                    @Override
                    public void onExportComplete(ExportManager.ExportResult result) {
                        runOnUiThread(() -> {
                            showGlobalLoading(false, null);
                            if (result.success) {
                                Library.showSuccess(EventsActivity.this, result.getSummary(), Toast.LENGTH_LONG);
                            } else {
                                Library.showError(EventsActivity.this, "Export fallito: " + result.getSummary(), Toast.LENGTH_LONG);
                            }
                        });
                    }

                    @Override
                    public void onExportError(String error, Exception exception) {
                        runOnUiThread(() -> {
                            showGlobalLoading(false, null);
                            String userMessage = "Errore durante l'export dei " + selectedEvents.size() + " eventi selezionati:\n" + error;
                            Library.showError(EventsActivity.this, userMessage, Toast.LENGTH_LONG);
                        });
                    }

                    @Override
                    public void onExportProgress(int processed, int total, String currentEvent) {
                        runOnUiThread(() -> {
                            String progressMessage = String.format(QDue.getLocale(),
                                    "Esportando eventi selezionati... %d/%d\n%s",
                                    processed, total, currentEvent
                            );
                            showGlobalLoading(true, progressMessage);
                        });
                    }
                });
            }
        } catch (Exception e) {
            showGlobalLoading(false, null);
            Log.e(TAG, "Error exporting selected events: " + e.getMessage(), e);
            String errorMessage = "Errore durante l'export di " + selectedEvents.size() + " eventi: " + e.getMessage();
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    // ==================== DATABASE OPERATIONS ====================

    void handleDeleteAllEvents() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Events")
                .setMessage("Are you sure you want to delete all local events? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> deleteAllEvents())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes all events from database and updates UI state.
     */
    private void deleteAllEvents() {
        new Thread(() -> {
            try {
                int result = mDatabase.eventDao().deleteAllEvents();

                runOnUiThread(() -> {
                    if (result > 0) {
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.onEventsCleared(result, true);
                        }

                        showSuccess("All events cleared");

                        mHasEvents = false;
                        mTotalEventsCount = 0;
                        updateFabVisibility();
                        notifyEventsChanged(CHANGE_TYPE_DELETE, result);
                    } else {
                        showError("❌ Error deleting all events");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error clearing events from database", e);
                runOnUiThread(() -> showError("❌ Error deleting all events"));
            }
        }).start();
    }

    // ==================== EVENT OPERATIONS ====================

    /**
     * Initiates event deletion with undo functionality.
     * <p>
     *
     * @param event    Event to delete
     * @param listener Callback for deletion events
     */
    @Override
    public void triggerEventDeletion(LocalEvent event, EventDeletionListener listener) {
        if (event == null || event.getId() == null) {
            listener.onDeletionCompleted(false, "Evento non valido");
            return;
        }

        showEventDeletionDialog(event, listener);
    }

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
                    startPendingEventDeletion(event);
                    listener.onDeletionRequested();
                })
                .setNegativeButton(R.string.text_cancel, (dialog, which) -> {
                    listener.onDeletionCancelled();
                })
                .show();
    }

    /**
     * Starts pending deletion with undo option via Snackbar.
     * <p>
     *
     * @param event Event to delete
     */
    private void startPendingEventDeletion(LocalEvent event) {
        String eventTitle = event.getTitle();
        String eventId = event.getId();

        if (mCurrentListFragment != null) {
            mCurrentListFragment.addToPendingDeletion(eventId);
            mCurrentListFragment.suppressRefresh(true);
        }

        removeEventFromList(eventId);

        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                eventTitle + " " + R.string.text_deleted,
                Snackbar.LENGTH_LONG
        );

        snackbar.setAction(R.string.text_capital_cancel, v -> {
            cancelEventDeletion(event);
        });

        snackbar.setBackgroundTint(getColor(R.color.warning_color));
        snackbar.setActionTextColor(getColor(android.R.color.white));

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    performActualEventDeletion(eventId, eventTitle);
                }
            }
        });

        snackbar.show();
    }

    private void cancelEventDeletion(LocalEvent event) {
        String eventId = event.getId();

        if (mCurrentListFragment != null) {
            mCurrentListFragment.removeFromPendingDeletion(eventId);
            mCurrentListFragment.suppressRefresh(false);
            mCurrentListFragment.addEvent(event);
        }

        Toast.makeText(this, "Eliminazione annullata", Toast.LENGTH_SHORT).show();
    }

    private void performActualEventDeletion(String eventId, String eventTitle) {
        new Thread(() -> {
            try {
                EventDao eventDao = mDatabase.eventDao();
                int deletedRows = eventDao.deleteEventById(eventId);

                runOnUiThread(() -> {
                    if (mCurrentListFragment != null) {
                        mCurrentListFragment.removeFromPendingDeletion(eventId);
                        mCurrentListFragment.suppressRefresh(false);
                    }

                    if (deletedRows > 0) {
                        showSuccess("Event deleted permanently");

                        mTotalEventsCount = Math.max(0, mTotalEventsCount - 1);
                        mHasEvents = mTotalEventsCount > 0;
                        updateFabVisibility();

                        notifyEventsChanged(CHANGE_TYPE_DELETE, 1);
                        triggerBackupAfterDeletion(eventTitle);
                    } else {
                        showError("Event not found in database");
                        if (mCurrentListFragment != null) {
                            mCurrentListFragment.refreshEvents();
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting event: " + e.getMessage());
                runOnUiThread(() -> {
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

    @Override
    public void triggerEventEdit(LocalEvent event) {
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
    public void triggerEventEditFromList(LocalEvent event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());

        try {
            if (mNavController != null) {
                mNavController.navigate(R.id.action_events_list_to_event_edit, args);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to edit: " + e.getMessage());
            Toast.makeText(this, "Errore navigazione edit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void triggerEventDuplicate(LocalEvent event) {
        Toast.makeText(this, "Duplica evento - TODO", Toast.LENGTH_SHORT).show();
    }

    /**
     * Shares event details via system share intent.
     * <p>
     *
     * @param event Event to share
     */
    @Override
    public void triggerEventShare(LocalEvent event) {
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

    /**
     * Adds event to system calendar application.
     * <p>
     *
     * @param event Event to add to calendar
     */
    @Override
    public void triggerAddToCalendar(LocalEvent event) {
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

    // ==================== UI STATE MANAGEMENT ====================

    private void showGlobalLoading(boolean show, String message) {
        if (show) {
            mLoadingStateView.setVisibility(View.VISIBLE);
        } else {
            mLoadingStateView.setVisibility(View.GONE);
        }
    }

    private void showSuccess(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.success_color))
                .show();
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.error_color))
                .show();
    }

    /**
     * Updates FAB visibility based on current navigation state and events availability.
     */
    private void updateFabVisibility() {
        if (mNavController != null) {
            int currentDestination = mNavController.getCurrentDestination() != null ?
                    mNavController.getCurrentDestination().getId() : -1;

            if (currentDestination == R.id.nav_events_list) {
                if (mHasEvents) {
                    mFabAddEvent.show();
                } else {
                    mFabAddEvent.hide();
                }
            } else {
                mFabAddEvent.hide();
            }
        }
    }

    private void checkInitialEventsState() {
        new Thread(() -> {
            try {
                int count = mDatabase.eventDao().getEventsCount();
                runOnUiThread(() -> {
                    mHasEvents = count > 0;
                    mTotalEventsCount = count;
                    updateFabVisibility();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking initial events state", e);
            }
        }).start();
    }

    private void updateEventsCount() {
        new Thread(() -> {
            try {
                int count = mDatabase.eventDao().getEventsCount();
                runOnUiThread(() -> {
                    mTotalEventsCount = count;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating events count", e);
            }
        }).start();
    }

    // ==================== BACK HANDLING REGISTRATION ====================

    /**
     * Registers fragment for unsaved changes handling.
     * <p>
     *
     * @param fragment Fragment with unsaved changes
     * @param handler  Handler for unsaved changes confirmation
     */
    public void registerFragmentUnsavedChanges(@NonNull Fragment fragment, @NonNull UnsavedChangesHandler handler) {
        mBackHandlerFactory.forComponent(fragment)
                .withPriority(100)
                .withDescription(fragment.getClass().getSimpleName() + " unsaved changes")
                .registerUnsavedChanges(handler);
    }

    public void registerUnsavedChangesHandler(@NonNull Object component, @NonNull UnsavedChangesHandler handler) {
        mBackHandlerFactory.forComponent(component)
                .withPriority(100)
                .withDescription(component.getClass().getSimpleName() + " unsaved changes")
                .registerUnsavedChanges(handler);
    }

    public void registerSelectionModeHandler(@NonNull Object component, @NonNull BackPressHandler selectionHandler) {
        mBackHandlerFactory.forComponent(component)
                .withPriority(50)
                .withDescription(component.getClass().getSimpleName() + " selection mode")
                .register(selectionHandler);
    }

    public void unregisterBackHandler(@NonNull Object component) {
        mBackHandlingService.unregisterComponent(component);
    }

    public boolean hasAnyUnsavedChanges() {
        return mBackHandlingService.hasUnsavedChanges(this);
    }

    // ==================== INTERFACE IMPLEMENTATIONS ====================

    @Override
    public void triggerDeleteAllEvents() {
        handleDeleteAllEvents();
    }

    @Override
    public void triggerImportEventsFromFile() {
        mFileAccessAdapter.selectFile(new FileAccessAdapter.FileSelectionCallback() {
            @Override
            public void onFileSelected(@NonNull Uri fileUri) {
                importEventsFromFile(fileUri);
            }

            @Override
            public void onSelectionError(@NonNull String error) {
                Library.showError(QDue.getContext(), "File selection failed: " + error);
            }

            @Override
            public void onSelectionCancelled() {
                // User cancelled - no action needed
            }
        });
    }

    @Override
    public void triggerImportEventsFromUrl() {
        handleImportEventsFromUrl();
    }

    @Override
    public void triggerExportEventsToFile(Uri fileUri) {
        handleExportEventsToFile(fileUri);
    }

    @Override
    public void triggerExportSelectedEventsToFile(Uri fileUri, Set<String> selectedEventIds, List<LocalEvent> selectedEvents) {
        handleExportSelectedEventsToFile(fileUri, selectedEvents);
    }

    @Override
    public void triggerCreateNewEvent() {
        handleCreateNewEvent();
    }

    @Override
    public void onEventsListStateChanged(boolean hasEvents) {
        mHasEvents = hasEvents;
        updateFabVisibility();

        if (hasEvents) {
            updateEventsCount();
        } else {
            mTotalEventsCount = 0;
        }
    }

    // ==================== ACTIVITY RESULT MANAGEMENT ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFileAccessAdapter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Notifies MainActivity of events changes for proper UI synchronization.
     * <p>
     *
     * @param changeType Type of change (create, delete, import, modify)
     * @param eventCount Number of events affected
     */
    private void notifyEventsChanged(String changeType, int eventCount) {
        if (changeType == null) {
            changeType = CHANGE_TYPE_MODIFY;
        }

        if (eventCount < 0) {
            eventCount = 0;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EVENTS_CHANGED, true);
        resultIntent.putExtra(EXTRA_EVENTS_COUNT, eventCount);
        resultIntent.putExtra(EXTRA_CHANGE_TYPE, changeType);
        resultIntent.putExtra("timestamp", System.currentTimeMillis());
        resultIntent.putExtra("total_events", mTotalEventsCount);

        setResult(RESULT_OK, resultIntent);
    }

    // ==================== PUBLIC UTILITY METHODS ====================

    public NavController getNavController() {
        return mNavController;
    }

    public void removeEventFromList(String eventId) {
        if (mCurrentListFragment != null && eventId != null) {
            mCurrentListFragment.removeEvent(eventId);
        }
    }

    public void updateEventInList(LocalEvent event) {
        if (mCurrentListFragment != null && event != null) {
            mCurrentListFragment.updateEvent(event);
        }
    }

    public void triggerPendingEventCreationCheck() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkAndExecutePendingEventCreation();
        }, 200);
    }

    public boolean hasPendingEventCreation() {
        return mPendingEventCreation != null;
    }

    // ==================== HELPER METHODS ====================

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

    private void triggerBackupAfterDeletion(String deletedEventTitle) {
        if (mCurrentListFragment != null) {
            BackupIntegration.integrateWithEventDeletion(
                    this,
                    mCurrentListFragment.getEventsList(),
                    deletedEventTitle
            );
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * DEBUG: Check activity state
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
     * DEBUG: Force notification test
     */
    public void debugForceNotification(String testChangeType, int testEventCount) {
        Log.d(TAG, "=== DEBUG FORCE NOTIFICATION ===");
        Log.d(TAG, "Test notification: " + testChangeType + " (" + testEventCount + " events)");
        notifyEventsChanged(testChangeType, testEventCount);
        Log.d(TAG, "=== END DEBUG FORCE NOTIFICATION ===");
    }

    /**
     * 🔄 DEBUG: Editor navigation info
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

    /**
     * 🆕 DEBUG: Back handlers info
     */
    public String debugGetBackHandlingDebugInfo() {
        return BackHandlingModule.debugGetInfo() + "\n" +
                (mBackHandlingService instanceof BackHandlingServiceImpl ?
                        ((BackHandlingServiceImpl) mBackHandlingService).debugGetInfo() :
                        "Service debug info not available");
    }

    /**
     * 🆕 DEBUG: Force back press handling
     */
    public boolean debugForceBackPress(@NonNull Object component) {
        return mBackHandlingService.handleBackPress(component);
    }

    // ==================== INNER CLASSES ====================

    /**
     * Holds pending event creation data for delayed execution.
     */
    private static class PendingEventCreation {
        public final String targetDate;
        public final String sourceFragment;
        public final String editMode;
        public final long timestamp;

        public PendingEventCreation(String targetDate, String sourceFragment, String editMode) {
            this.targetDate = targetDate;
            this.sourceFragment = sourceFragment;
            this.editMode = editMode;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format(QDue.getLocale(), "PendingEventCreation{date=%s, source=%s, mode=%s, time=%d}",
                    targetDate, sourceFragment, editMode, timestamp);
        }
    }
}