package net.calvuz.qdue.ui.features.events.local.presentation.dialogs;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.events.validation.JsonSchemaValidator;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * FileImportDialogFragment - JSON File Import Dialog (MVVM)
 *
 * <p>Dialog fragment for importing events from JSON files with comprehensive validation
 * using JsonSchemaValidator. Provides user-friendly interface for file selection,
 * validation preview, and import options.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>JSON Format Support</strong>: Validates against event schema</li>
 *   <li><strong>File Selection</strong>: Material design file picker integration</li>
 *   <li><strong>Validation Preview</strong>: Shows errors and warnings before import</li>
 *   <li><strong>Import Options</strong>: Merge or replace existing events</li>
 *   <li><strong>Progress Feedback</strong>: Real-time import progress</li>
 *   <li><strong>MVVM Integration</strong>: Uses ViewModels for operations</li>
 *   <li><strong>Graceful Degradation</strong>: Handles missing file service</li>
 * </ul>
 *
 * <h3>Validation Flow:</h3>
 * <ol>
 *   <li>User selects JSON file</li>
 *   <li>File content is validated using JsonSchemaValidator</li>
 *   <li>Validation results are displayed to user</li>
 *   <li>User confirms import with selected options</li>
 *   <li>Events are imported through ViewModel or direct service</li>
 * </ol>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class FileImportDialogFragment extends DialogFragment {

    private static final String TAG = "FileImportDialogFragment";

    // ==================== IMPORT MODES ====================

    public enum ImportMode {
        MERGE,    // Add new events, keep existing ones
        REPLACE   // Replace all existing events
    }

    // ==================== UI COMPONENTS ====================

    // File Selection
    private MaterialCardView mFileSelectionCard;
    private TextView mSelectedFileText;
    private MaterialButton mSelectFileButton;

    // Import Options
    private RadioGroup mImportModeRadioGroup;

    // Validation Preview
    private MaterialCardView mValidationCard;
    private TextView mValidationSummaryText;
    private RecyclerView mValidationResultsRecyclerView;

    // Progress and Actions
    private LinearProgressIndicator mProgressIndicator;
    private MaterialButton mImportButton;
    private MaterialButton mCancelButton;

    // Status Display
    private TextView mStatusText;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;
    private LocalEventsFileOperationsViewModel mFileOpsViewModel;

    // ==================== STATE ====================

    private Uri mSelectedFileUri = null;
    private String mFileContent = null;
    private JsonSchemaValidator.ValidationResult mValidationResult = null;
    private ImportMode mSelectedImportMode = ImportMode.MERGE;
    private boolean mIsValidationInProgress = false;
    private boolean mIsImportInProgress = false;
    private boolean mFileOperationsAvailable = false;

    // ==================== ADAPTERS ====================

    private ValidationResultsAdapter mValidationAdapter;

    // ==================== LISTENERS ====================

    private BaseViewModel.LoadingStateListener mLoadingStateListener;
    private BaseViewModel.ErrorStateListener mErrorStateListener;
    private BaseViewModel.EventListener mEventListener;

    // ==================== FILE PICKER ====================

    private final ActivityResultLauncher<String[]> mFilePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleFileSelected);

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO Set dialog style
        //setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_QDue_Dialog_FullScreen);

        // Get ViewModels from parent Activity
        if (getActivity() instanceof LocalEventsActivity activity) {
            mEventsViewModel = activity.getEventsViewModel();

            // Try to get file operations ViewModel
            try {
                mFileOpsViewModel = activity.getFileOperationsViewModel();
                mFileOperationsAvailable = true;
            } catch (UnsupportedOperationException e) {
                Log.w( TAG, "File operations not available: " + e.getMessage());
                mFileOperationsAvailable = false;
            }
        }

        if (mEventsViewModel == null) {
            Log.e(TAG, "Could not get LocalEventsViewModel from parent Activity");
            dismiss();
            return;
        }

        // Initialize validation adapter
        mValidationAdapter = new ValidationResultsAdapter();

        // Setup listeners
        setupViewModelListeners();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_import_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners();
        setupRecyclerView();
        updateUIState();

        // Show warning if file operations not available
        if (!mFileOperationsAvailable) {
            showFileOpsUnavailableWarning();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove listeners to prevent memory leaks
        removeViewModelListeners();
    }

    // ==================== INITIALIZATION ====================

    private void setupViewModelListeners() {
        if (mFileOpsViewModel != null) {
            // File operations loading state listener
            mLoadingStateListener = (operation, loading) -> {
                if (LocalEventsFileOperationsViewModel.OP_VALIDATE_FILE.equals(operation) ||
                        LocalEventsFileOperationsViewModel.OP_IMPORT_FILE.equals(operation)) {
                    handleLoadingState(operation, loading);
                }
            };
            mFileOpsViewModel.addLoadingStateListener(mLoadingStateListener);

            // File operations error state listener
            mErrorStateListener = (operation, error) -> {
                if (LocalEventsFileOperationsViewModel.OP_VALIDATE_FILE.equals(operation) ||
                        LocalEventsFileOperationsViewModel.OP_IMPORT_FILE.equals(operation)) {
                    handleErrorState(operation, error);
                }
            };
            mFileOpsViewModel.addErrorStateListener(mErrorStateListener);

            // File operations event listener
            mEventListener = event -> {
                if (event instanceof BaseViewModel.UIActionEvent) {
                    BaseViewModel.UIActionEvent uiEvent = (BaseViewModel.UIActionEvent) event;
                    handleFileOperationEvent(uiEvent);
                }
            };
            mFileOpsViewModel.addEventListener(mEventListener);
        }
    }

    private void removeViewModelListeners() {
        if (mFileOpsViewModel != null) {
            if (mLoadingStateListener != null) {
                mFileOpsViewModel.removeLoadingStateListener(mLoadingStateListener);
            }
            if (mErrorStateListener != null) {
                mFileOpsViewModel.removeErrorStateListener(mErrorStateListener);
            }
            if (mEventListener != null) {
                mFileOpsViewModel.removeEventListener(mEventListener);
            }
        }
    }

    private void initializeViews(@NonNull View view) {
        // File Selection
        mFileSelectionCard = view.findViewById(R.id.file_selection_card);
        mSelectedFileText = view.findViewById(R.id.selected_file_text);
        mSelectFileButton = view.findViewById(R.id.select_file_button);

        // Import Options
        mImportModeRadioGroup = view.findViewById(R.id.import_mode_radio_group);

        // Validation Preview
        mValidationCard = view.findViewById(R.id.validation_card);
        mValidationSummaryText = view.findViewById(R.id.validation_summary_text);
        mValidationResultsRecyclerView = view.findViewById(R.id.validation_results_recycler_view);

        // Progress and Actions
        mProgressIndicator = view.findViewById(R.id.progress_indicator);
        mImportButton = view.findViewById(R.id.import_button);
        mCancelButton = view.findViewById(R.id.cancel_button);

        // Status Display
        mStatusText = view.findViewById(R.id.status_text);
    }

    private void setupClickListeners() {
        // File selection
        mSelectFileButton.setOnClickListener(v -> openFilePicker());

        // Import mode radio group
        mImportModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.merge_mode_radio) {
                mSelectedImportMode = ImportMode.MERGE;
            } else if (checkedId == R.id.replace_mode_radio) {
                mSelectedImportMode = ImportMode.REPLACE;
            }
        });

        // Action buttons
        mImportButton.setOnClickListener(v -> performImport());
        mCancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        mValidationResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mValidationResultsRecyclerView.setAdapter(mValidationAdapter);
    }

    // ==================== FILE OPERATIONS ====================

    private void openFilePicker() {
        // Open file picker for JSON files
        mFilePickerLauncher.launch(new String[]{"application/json", "text/plain"});
    }

    private void handleFileSelected(@Nullable Uri uri) {
        if (uri == null) {
            Log.d(TAG, "No file selected");
            return;
        }

        mSelectedFileUri = uri;
        mSelectedFileText.setText(getFileName(uri));
        mSelectedFileText.setVisibility(View.VISIBLE);

        // Start file validation
        validateSelectedFile();
    }

    private String getFileName(@NonNull Uri uri) {
        String path = uri.getPath();
        if (path != null && path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return LocaleManager.getLocalizedString(getContext(), "selected_file", "Selected file");
    }

    // ==================== FILE VALIDATION ====================

    private void validateSelectedFile() {
        if (mSelectedFileUri == null) {
            return;
        }

        mIsValidationInProgress = true;
        updateUIState();

        // Read file content in background thread
        new Thread(() -> {
            try {
                mFileContent = readFileContent(mSelectedFileUri);

                // Validate using JsonSchemaValidator
                mValidationResult = JsonSchemaValidator.validateEventPackage(mFileContent);

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        mIsValidationInProgress = false;
                        handleValidationComplete();
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Error reading file: " + e.getMessage(), e);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        mIsValidationInProgress = false;
                        handleValidationError(e.getMessage());
                    });
                }
            }
        }).start();
    }

    private String readFileContent(@NonNull Uri uri) throws IOException {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    private void handleValidationComplete() {
        updateValidationResults();
        updateUIState();

        Log.d(TAG, "Validation complete. Valid: " + mValidationResult.isValid() +
                ", Errors: " + mValidationResult.getErrors().size() +
                ", Warnings: " + mValidationResult.getWarnings().size());
    }

    private void handleValidationError(@NonNull String errorMessage) {
        mStatusText.setText(LocaleManager.getLocalizedString(getContext(),
                                                             "import_validation_error", "Validation error") + ": " + errorMessage);
        mStatusText.setVisibility(View.VISIBLE);
        updateUIState();

        Log.e(TAG, "Validation error: " + errorMessage);
    }

    private void updateValidationResults() {
        if (mValidationResult == null) {
            return;
        }

        // Update validation summary
        String summaryText;
        if (mValidationResult.isValid()) {
            summaryText = LocaleManager.getLocalizedString(getContext(),
                                                           "validation_success", "Validation successful") +
                    " (" + mValidationResult.getEventCount() + " events)";

            if (!mValidationResult.getWarnings().isEmpty()) {
                summaryText += " - " + mValidationResult.getWarnings().size() + " warnings";
            }
        } else {
            summaryText = LocaleManager.getLocalizedString(getContext(),
                                                           "validation_failed", "Validation failed") +
                    " (" + mValidationResult.getErrors().size() + " errors)";
        }

        mValidationSummaryText.setText(summaryText);

        // Update validation results list
        mValidationAdapter.updateResults(mValidationResult.getErrors(), mValidationResult.getWarnings());
    }

    // ==================== IMPORT OPERATIONS ====================

    private void performImport() {
        if (mValidationResult == null || !mValidationResult.isValid()) {
            showError(LocaleManager.getLocalizedString(getContext(),
                                                       "import_validation_required", "Please select and validate a file first"));
            return;
        }

        mIsImportInProgress = true;
        updateUIState();

        if (mFileOperationsAvailable && mFileOpsViewModel != null) {
            // Use file operations ViewModel
            performImportViaFileOpsViewModel();
        } else {
            // Fallback to basic import via events ViewModel
            performBasicImport();
        }
    }

    private void performImportViaFileOpsViewModel() {
        if (mFileOpsViewModel != null && mSelectedFileUri != null) {
            mFileOpsViewModel.importEventsFromFile(mSelectedFileUri);
        }
    }

    private void performBasicImport() {
        // Basic fallback implementation
        // This would need to be implemented based on your specific needs
        showError(LocaleManager.getLocalizedString(getContext(),
                                                   "import_not_available", "Import functionality is not available"));
        mIsImportInProgress = false;
        updateUIState();
    }

    // ==================== EVENT HANDLERS ====================

    private void handleLoadingState(@NonNull String operation, boolean loading) {
        if (LocalEventsFileOperationsViewModel.OP_VALIDATE_FILE.equals(operation)) {
            mIsValidationInProgress = loading;
        } else if (LocalEventsFileOperationsViewModel.OP_IMPORT_FILE.equals(operation)) {
            mIsImportInProgress = loading;
        }
        updateUIState();
    }

    private void handleErrorState(@NonNull String operation, @Nullable String error) {
        if (error != null) {
            showError(error);
        }
    }

    private void handleFileOperationEvent(@NonNull BaseViewModel.UIActionEvent event) {
        String action = event.getAction();

        if ("IMPORT_SUCCESS".equals(action)) {
            handleImportSuccess();
        } else if ("IMPORT_FAILED".equals(action)) {
            handleImportFailed();
        } else if ("REFRESH_EVENTS".equals(action)) {
            // Events should be refreshed
            if (mEventsViewModel != null) {
                mEventsViewModel.refreshEvents();
            }
        }
    }

    private void handleImportSuccess() {
        mIsImportInProgress = false;
        showSuccess(LocaleManager.getLocalizedString(getContext(),
                                                     "import_success", "Events imported successfully"));

        // Auto-dismiss after short delay
        mStatusText.postDelayed(() -> dismiss(), 1500);
    }

    private void handleImportFailed() {
        mIsImportInProgress = false;
        showError(LocaleManager.getLocalizedString(getContext(),
                                                   "import_failed", "Import failed"));
        updateUIState();
    }

    // ==================== UI STATE MANAGEMENT ====================

    private void updateUIState() {
        // File selection state
        boolean fileSelected = mSelectedFileUri != null;

        // Validation state
        boolean validationComplete = mValidationResult != null && !mIsValidationInProgress;
        boolean validationSuccess = validationComplete && mValidationResult.isValid();

        mValidationCard.setVisibility(validationComplete ? View.VISIBLE : View.GONE);

        // Progress indicator
        mProgressIndicator.setVisibility(
                (mIsValidationInProgress || mIsImportInProgress) ? View.VISIBLE : View.GONE);

        // Import button
        mImportButton.setEnabled(validationSuccess && !mIsImportInProgress && !mIsValidationInProgress);

        // Cancel button
        mCancelButton.setEnabled(!mIsImportInProgress);
    }

    private void showFileOpsUnavailableWarning() {
        mStatusText.setText(LocaleManager.getLocalizedString(getContext(),
                                                             "file_ops_unavailable", "File operations are not fully available"));
        mStatusText.setVisibility(View.VISIBLE);
    }

    // ==================== UTILITY METHODS ====================

    private void showError(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
        Log.e(TAG, "Error: " + message);
    }

    private void showSuccess(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
        Log.d(TAG, "Success: " + message);
    }

    // ==================== VALIDATION RESULTS ADAPTER ====================

    private static class ValidationResultsAdapter extends RecyclerView.Adapter<ValidationResultsAdapter.ViewHolder> {

        private List<String> mErrors = List.of();
        private List<String> mWarnings = List.of();

        public void updateResults(@NonNull List<String> errors, @NonNull List<String> warnings) {
            mErrors = errors;
            mWarnings = warnings;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_validation_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < mErrors.size()) {
                holder.bind(mErrors.get(position), true);
            } else {
                int warningIndex = position - mErrors.size();
                holder.bind(mWarnings.get(warningIndex), false);
            }
        }

        @Override
        public int getItemCount() {
            return mErrors.size() + mWarnings.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mMessageText;
            private final View mSeverityIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                mMessageText = itemView.findViewById(R.id.message_text);
                mSeverityIndicator = itemView.findViewById(R.id.severity_indicator);
            }

            public void bind(@NonNull String message, boolean isError) {
                mMessageText.setText(message);
                mSeverityIndicator.setBackgroundResource(
                        isError ? R.color.error_color : R.color.warning_color);
            }
        }
    }

    // ==================== FACTORY METHOD ====================

    /**
     * Create new instance of FileImportDialogFragment
     */
    public static FileImportDialogFragment newInstance() {
        return new FileImportDialogFragment();
    }
}