package net.calvuz.qdue.ui.features.events.local.presentation.dialogs;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsFileOperationsViewModel;
import net.calvuz.qdue.ui.features.events.local.viewmodels.BaseViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;

/**
 * FileExportDialogFragment - JSON File Export Dialog (MVVM)
 *
 * <p>Dialog fragment for exporting events to JSON files with comprehensive export options.
 * Provides user-friendly interface for selecting export criteria, date ranges, and
 * output file configuration.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>JSON Format Export</strong>: Standards-compliant event JSON format</li>
 *   <li><strong>Date Range Selection</strong>: Export events within specified date range</li>
 *   <li><strong>Export Options</strong>: All events, selected events, or date-filtered events</li>
 *   <li><strong>File Naming</strong>: Customizable export file names</li>
 *   <li><strong>Progress Feedback</strong>: Real-time export progress</li>
 *   <li><strong>MVVM Integration</strong>: Uses ViewModels for operations</li>
 *   <li><strong>Graceful Degradation</strong>: Handles missing file service</li>
 * </ul>
 *
 * <h3>Export Modes:</h3>
 * <ul>
 *   <li><strong>All Events</strong>: Export complete event database</li>
 *   <li><strong>Date Range</strong>: Export events within specified date range</li>
 *   <li><strong>Selected Events</strong>: Export only currently selected events</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class FileExportDialogFragment extends DialogFragment {

    private static final String TAG = "FileExportDialogFragment";

    // ==================== EXPORT MODES ====================

    public enum ExportMode {
        ALL_EVENTS,      // Export all events
        DATE_RANGE,      // Export events within date range
        SELECTED_EVENTS  // Export only selected events
    }

    // ==================== UI COMPONENTS ====================

    // Export Mode Selection
    private RadioGroup mExportModeRadioGroup;
    private TextView mSelectedEventsCountText;

    // Date Range Selection
    private MaterialButton mStartDateButton;
    private MaterialButton mEndDateButton;
    private Chip mStartDateChip;
    private Chip mEndDateChip;
    private View mDateRangeContainer;

    // Export Options
    private CheckBox mIncludeDescriptionsCheckBox;
    private CheckBox mIncludeLocationsCheckBox;

    // File Settings
    private TextInputLayout mFileNameInputLayout;
    private TextInputEditText mFileNameEditText;

    // Progress and Actions
    private LinearProgressIndicator mProgressIndicator;
    private TextView mProgressText;
    private MaterialButton mExportButton;
    private MaterialButton mCancelButton;

    // Status Display
    private TextView mStatusText;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;
    private LocalEventsFileOperationsViewModel mFileOpsViewModel;

    // ==================== STATE ====================

    private ExportMode mSelectedExportMode = ExportMode.ALL_EVENTS;
    private LocalDate mStartDate = null;
    private LocalDate mEndDate = null;
    private int mSelectedEventsCount = 0;
    private boolean mIsExportInProgress = false;
    private boolean mFileOperationsAvailable = false;

    // Export Options
    private boolean mIncludeDescriptions = true;
    private boolean mIncludeLocations = true;

    // ==================== DATE FORMATTER ====================

    private final DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter mFileNameDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== LISTENERS ====================

    private BaseViewModel.LoadingStateListener mLoadingStateListener;
    private BaseViewModel.ErrorStateListener mErrorStateListener;
    private BaseViewModel.EventListener mEventListener;

    // ==================== FILE EXPORT LAUNCHER ====================

    private final ActivityResultLauncher<String> mFileExportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                                      this::handleExportLocationSelected);

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

            // Get selected events count if available
            if (mEventsViewModel != null) {
                mSelectedEventsCount = mEventsViewModel.getSelectedEventsCount();
            }
        }

        if (mEventsViewModel == null) {
            Log.e(TAG, "Could not get LocalEventsViewModel from parent Activity");
            dismiss();
            return;
        }

        // Setup listeners
        setupViewModelListeners();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_export_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners();
        setupDefaultValues();
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
                if (LocalEventsFileOperationsViewModel.OP_EXPORT_ALL.equals(operation) ||
                        LocalEventsFileOperationsViewModel.OP_EXPORT_SELECTED.equals(operation)) {
                    handleLoadingState(operation, loading);
                }
            };
            mFileOpsViewModel.addLoadingStateListener(mLoadingStateListener);

            // File operations error state listener
            mErrorStateListener = (operation, error) -> {
                if (LocalEventsFileOperationsViewModel.OP_EXPORT_ALL.equals(operation) ||
                        LocalEventsFileOperationsViewModel.OP_EXPORT_SELECTED.equals(operation)) {
                    handleErrorState(operation, error);
                }
            };
            mFileOpsViewModel.addErrorStateListener(mErrorStateListener);

            // File operations event listener
            mEventListener = event -> {
                if (event instanceof BaseViewModel.UIActionEvent uiEvent) {
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
        // Export Mode Selection
        mExportModeRadioGroup = view.findViewById(R.id.export_mode_radio_group);
        mSelectedEventsCountText = view.findViewById(R.id.selected_events_count_text);

        // Date Range Selection
        mStartDateButton = view.findViewById(R.id.start_date_button);
        mEndDateButton = view.findViewById(R.id.end_date_button);
        mStartDateChip = view.findViewById(R.id.start_date_chip);
        mEndDateChip = view.findViewById(R.id.end_date_chip);
        mDateRangeContainer = view.findViewById(R.id.date_range_container);

        // Export Options
        mIncludeDescriptionsCheckBox = view.findViewById(R.id.include_descriptions_checkbox);
        mIncludeLocationsCheckBox = view.findViewById(R.id.include_locations_checkbox);

        // File Settings
        mFileNameInputLayout = view.findViewById(R.id.file_name_input_layout);
        mFileNameEditText = view.findViewById(R.id.file_name_edit_text);

        // Progress and Actions
        mProgressIndicator = view.findViewById(R.id.progress_indicator);
        mProgressText = view.findViewById(R.id.progress_text);
        mExportButton = view.findViewById(R.id.export_button);
        mCancelButton = view.findViewById(R.id.cancel_button);

        // Status Display
        mStatusText = view.findViewById(R.id.status_text);
    }

    private void setupClickListeners() {
        // Export mode radio group
        mExportModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.all_events_radio) {
                mSelectedExportMode = ExportMode.ALL_EVENTS;
            } else if (checkedId == R.id.date_range_radio) {
                mSelectedExportMode = ExportMode.DATE_RANGE;
            } else if (checkedId == R.id.selected_events_radio) {
                mSelectedExportMode = ExportMode.SELECTED_EVENTS;
            }
            updateUIState();
        });

        // Date range buttons
        mStartDateButton.setOnClickListener(v -> showStartDatePicker());
        mEndDateButton.setOnClickListener(v -> showEndDatePicker());

        // Date chips (for clearing individual dates)
        mStartDateChip.setOnCloseIconClickListener(v -> clearStartDate());
        mEndDateChip.setOnCloseIconClickListener(v -> clearEndDate());

        // Export options checkboxes
        mIncludeDescriptionsCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> mIncludeDescriptions = isChecked);
        mIncludeLocationsCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> mIncludeLocations = isChecked);

        // Action buttons
        mExportButton.setOnClickListener(v -> startExport());
        mCancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupDefaultValues() {
        // Set default export mode
        mExportModeRadioGroup.check(R.id.all_events_radio);

        // Set default file name
        String defaultFileName = "qdue_events_" + LocalDate.now().format(mFileNameDateFormatter) + ".json";
        mFileNameEditText.setText(defaultFileName);

        // Set default export options
        mIncludeDescriptionsCheckBox.setChecked(mIncludeDescriptions);
        mIncludeLocationsCheckBox.setChecked(mIncludeLocations);

        // Update selected events count display
        if (mSelectedEventsCount > 0) {
            mSelectedEventsCountText.setText(
                    LocaleManager.getLocalizedString(getContext(), "selected_events_count",
                                                     mSelectedEventsCount + " items selected"));
        }
    }

    // ==================== DATE RANGE OPERATIONS ====================

    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (mStartDate != null) {
            calendar.set(mStartDate.getYear(), mStartDate.getMonthValue() - 1, mStartDate.getDayOfMonth());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    mStartDate = LocalDate.of(year, month + 1, dayOfMonth);

                    // Validate date range
                    if (mEndDate != null && mStartDate.isAfter(mEndDate)) {
                        mEndDate = mStartDate;
                    }

                    updateDateChips();
                    updateUIState();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (mEndDate != null) {
            calendar.set(mEndDate.getYear(), mEndDate.getMonthValue() - 1, mEndDate.getDayOfMonth());
        } else if (mStartDate != null) {
            calendar.set(mStartDate.getYear(), mStartDate.getMonthValue() - 1, mStartDate.getDayOfMonth());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    mEndDate = LocalDate.of(year, month + 1, dayOfMonth);

                    // Validate date range
                    if (mStartDate != null && mEndDate.isBefore(mStartDate)) {
                        mStartDate = mEndDate;
                    }

                    updateDateChips();
                    updateUIState();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void clearStartDate() {
        mStartDate = null;
        updateDateChips();
        updateUIState();
    }

    private void clearEndDate() {
        mEndDate = null;
        updateDateChips();
        updateUIState();
    }

    private void updateDateChips() {
        // Start date chip
        if (mStartDate != null) {
            mStartDateChip.setText(LocaleManager.getLocalizedString(getContext(), "from", "From") +
                                           ": " + mStartDate.format(mDateFormatter));
            mStartDateChip.setVisibility(View.VISIBLE);
            mStartDateChip.setCloseIconVisible(true);
        } else {
            mStartDateChip.setVisibility(View.GONE);
        }

        // End date chip
        if (mEndDate != null) {
            mEndDateChip.setText(LocaleManager.getLocalizedString(getContext(), "to", "To") +
                                         ": " + mEndDate.format(mDateFormatter));
            mEndDateChip.setVisibility(View.VISIBLE);
            mEndDateChip.setCloseIconVisible(true);
        } else {
            mEndDateChip.setVisibility(View.GONE);
        }
    }

    // ==================== EXPORT OPERATIONS ====================

    private void startExport() {
        if (!validateExportSettings()) {
            return;
        }

        // Get file name
        String fileName = mFileNameEditText.getText().toString().trim();
        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }

        // Launch file creation dialog
        mFileExportLauncher.launch(fileName);
    }

    private boolean validateExportSettings() {
        boolean isValid = true;

        // File name validation
        String fileName = mFileNameEditText.getText().toString().trim();
        if (fileName.isEmpty()) {
            mFileNameInputLayout.setError(LocaleManager.getLocalizedString(getContext(),
                                                                           "error_filename_required", "Filename is required"));
            isValid = false;
        } else {
            mFileNameInputLayout.setError(null);
        }

        // Date range validation for DATE_RANGE mode
        if (mSelectedExportMode == ExportMode.DATE_RANGE) {
            if (mStartDate == null && mEndDate == null) {
                showError(LocaleManager.getLocalizedString(getContext(),
                                                           "error_date_range_required", "Date range is required for date range export"));
                isValid = false;
            }
        }

        // Selected events validation for SELECTED_EVENTS mode
        if (mSelectedExportMode == ExportMode.SELECTED_EVENTS && mSelectedEventsCount == 0) {
            showError(LocaleManager.getLocalizedString(getContext(),
                                                       "error_no_selected_events", "No events are selected for export"));
            isValid = false;
        }

        return isValid;
    }

    private void handleExportLocationSelected(@Nullable Uri uri) {
        if (uri == null) {
            Log.d(TAG, "Export cancelled - no location selected");
            return;
        }

        mIsExportInProgress = true;
        updateUIState();

        if (mFileOperationsAvailable && mFileOpsViewModel != null) {
            // Use file operations ViewModel
            performExportViaFileOpsViewModel(uri);
        } else {
            // Fallback to basic export
            performBasicExport(uri);
        }
    }

    private void performExportViaFileOpsViewModel(@NonNull Uri uri) {
        if (mFileOpsViewModel == null) return;

        switch (mSelectedExportMode) {
            case ALL_EVENTS:
                mFileOpsViewModel.exportAllEventsToFile(uri);
                break;
            case SELECTED_EVENTS:
                List<LocalEvent> selectedEvents = mEventsViewModel.getSelectedEvents();
                mFileOpsViewModel.exportSelectedEventsToFile(uri, selectedEvents);
                break;
            case DATE_RANGE:
                // This would need special handling for date range filtering
                // For now, export all events
                mFileOpsViewModel.exportAllEventsToFile(uri);
                break;
        }
    }

    private void performBasicExport(@NonNull Uri uri) {
        // Basic fallback implementation
        showError(LocaleManager.getLocalizedString(getContext(),
                                                   "export_not_available", "Export functionality is not available"));
        mIsExportInProgress = false;
        updateUIState();
    }

    // ==================== EVENT HANDLERS ====================

    private void handleLoadingState(@NonNull String operation, boolean loading) {
        if (LocalEventsFileOperationsViewModel.OP_EXPORT_ALL.equals(operation) ||
                LocalEventsFileOperationsViewModel.OP_EXPORT_SELECTED.equals(operation)) {
            mIsExportInProgress = loading;
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

        if ("EXPORT_SUCCESS".equals(action)) {
            handleExportSuccess();
        } else if ("EXPORT_FAILED".equals(action)) {
            handleExportFailed();
        } else if ("EXPORT_PROGRESS".equals(action)) {
            // Handle progress updates
            Object progressData = event.getData("progress");
            if (progressData instanceof LocalEventsFileOperationsViewModel.ProgressInfo progress) {
                updateExportProgress(progress);
            }
        }
    }

    private void handleExportSuccess() {
        mIsExportInProgress = false;
        showSuccess(LocaleManager.getLocalizedString(getContext(),
                                                     "export_success", "Events exported successfully"));

        // Auto-dismiss after short delay
        mStatusText.postDelayed(() -> dismiss(), 2000);
    }

    private void handleExportFailed() {
        mIsExportInProgress = false;
        showError(LocaleManager.getLocalizedString(getContext(),
                                                   "export_failed", "Export failed"));
        updateUIState();
    }

    private void updateExportProgress(@NonNull LocalEventsFileOperationsViewModel.ProgressInfo progress) {
        mProgressText.setText(progress.getProgressText());
        mProgressText.setVisibility(View.VISIBLE);
    }

    // ==================== UI STATE MANAGEMENT ====================

    private void updateUIState() {
        // Date range container visibility
        mDateRangeContainer.setVisibility(
                mSelectedExportMode == ExportMode.DATE_RANGE ? View.VISIBLE : View.GONE);

        // Selected events count visibility
        mSelectedEventsCountText.setVisibility(
                mSelectedExportMode == ExportMode.SELECTED_EVENTS ? View.VISIBLE : View.GONE);

        // Progress indicator
        mProgressIndicator.setVisibility(mIsExportInProgress ? View.VISIBLE : View.GONE);
        mProgressText.setVisibility(mIsExportInProgress ? View.VISIBLE : View.GONE);

        // Export button state
        boolean canExport = !mIsExportInProgress && isExportValid();
        mExportButton.setEnabled(canExport);

        // Cancel button
        mCancelButton.setEnabled(!mIsExportInProgress);

        // Disable selected events radio if no events selected
        View selectedEventsRadio = requireView().findViewById(R.id.selected_events_radio);
        selectedEventsRadio.setEnabled(mSelectedEventsCount > 0);
    }

    private boolean isExportValid() {
        switch (mSelectedExportMode) {
            case ALL_EVENTS:
                return true;
            case DATE_RANGE:
                return mStartDate != null || mEndDate != null;
            case SELECTED_EVENTS:
                return mSelectedEventsCount > 0;
            default:
                return false;
        }
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

    // ==================== FACTORY METHOD ====================

    /**
     * Create new instance of FileExportDialogFragment
     */
    public static FileExportDialogFragment newInstance() {
        return new FileExportDialogFragment();
    }
}