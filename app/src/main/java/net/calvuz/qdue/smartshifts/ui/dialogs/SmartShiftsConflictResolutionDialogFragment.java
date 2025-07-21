package net.calvuz.qdue.smartshifts.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.domain.common.UnifiedOperationResult;
import net.calvuz.qdue.smartshifts.domain.exportimport.SmartShiftsExportImportManager;
import net.calvuz.qdue.smartshifts.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced Conflict Resolution Dialog for SmartShifts Import Operations.
 *
 * Now fully integrated with the unified architecture:
 * - Uses centralized enum types from SmartShiftsExportImportManager
 * - Integrates with UnifiedOperationResult system
 * - Uses SmartShiftsUILibrary for consistent UI patterns
 * - Material Design 3 compliant with enhanced UX
 * - Supports batch conflict resolution with "Apply to All"
 * - Enhanced error handling and validation
 * - Progress tracking integration
 * - Accessibility improvements
 *
 * Key improvements:
 * - No more local enum duplication
 * - Unified result handling
 * - Enhanced UI feedback
 * - Better conflict description and recommendations
 * - Smart default resolution based on conflict type
 * - Conflict severity indication
 *
 * @author SmartShifts Team
 * @since Phase 4 - Advanced Features (Unified Architecture)
 */
public class SmartShiftsConflictResolutionDialogFragment extends DialogFragment {

    private static final String TAG = "SmartShiftsConflictDialog";

    // Arguments keys
    private static final String ARG_CONFLICTS = "conflicts";
    private static final String ARG_CONFLICT_TYPE = "conflict_type";
    private static final String ARG_OPERATION_TYPE = "operation_type";
    private static final String ARG_CONFLICT_COUNT = "conflict_count";
    private static final String ARG_TOTAL_ITEMS = "total_items";

    // ============================================
    // UNIFIED INTERFACES
    // ============================================

    /**
     * Enhanced listener interface using unified types
     */
    public interface ConflictResolutionListener {
        /**
         * Called when user resolves conflicts
         */
        void onConflictResolved(
                @NonNull SmartShiftsExportImportManager.ConflictType conflictType,
                @NonNull SmartShiftsExportImportManager.ConflictResolutionStrategy resolution,
                boolean applyToAll,
                @NonNull ConflictResolutionMetadata metadata
        );

        /**
         * Called when user skips conflict resolution
         */
        void onConflictResolutionSkipped(
                @NonNull SmartShiftsExportImportManager.ConflictType conflictType,
                @NonNull String reason
        );

        /**
         * Called when user cancels the dialog
         */
        void onConflictResolutionCancelled(
                @NonNull SmartShiftsExportImportManager.ConflictType conflictType
        );
    }

    /**
     * Metadata for conflict resolution result
     */
    public static class ConflictResolutionMetadata {
        private final long resolutionTimestamp;
        private final String userChoice;
        private final int conflictsCount;
        private final boolean isDestructive;
        private final String resolutionDescription;

        public ConflictResolutionMetadata(
                String userChoice,
                int conflictsCount,
                boolean isDestructive,
                String resolutionDescription
        ) {
            this.resolutionTimestamp = System.currentTimeMillis();
            this.userChoice = userChoice;
            this.conflictsCount = conflictsCount;
            this.isDestructive = isDestructive;
            this.resolutionDescription = resolutionDescription;
        }

        // Getters
        public long getResolutionTimestamp() { return resolutionTimestamp; }
        public String getUserChoice() { return userChoice; }
        public int getConflictsCount() { return conflictsCount; }
        public boolean isDestructive() { return isDestructive; }
        public String getResolutionDescription() { return resolutionDescription; }
    }

    // ============================================
    // FIELDS
    // ============================================

    private ConflictResolutionListener listener;
    private List<String> conflicts;
    private SmartShiftsExportImportManager.ConflictType conflictType;
    private UnifiedOperationResult.OperationType operationType;
    private int totalItems;

    // UI Components
    private TextView textConflictInfo;
    private TextView textConflictDetails;
    private TextView textRecommendation;
    private RadioGroup radioGroupResolution;
    private CheckBox checkboxApplyToAll;
    private View warningContainer;
    private TextView textWarning;

    // ============================================
    // FACTORY METHODS
    // ============================================

    /**
     * Create new instance with unified parameters
     */
    public static SmartShiftsConflictResolutionDialogFragment newInstance(
            @NonNull List<String> conflicts,
            @NonNull SmartShiftsExportImportManager.ConflictType conflictType,
            @NonNull UnifiedOperationResult.OperationType operationType,
            int totalItems
    ) {
        SmartShiftsConflictResolutionDialogFragment fragment =
                new SmartShiftsConflictResolutionDialogFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(ARG_CONFLICTS, new ArrayList<>(conflicts));
        args.putString(ARG_CONFLICT_TYPE, conflictType.getValue());
        args.putString(ARG_OPERATION_TYPE, operationType.getValue());
        args.putInt(ARG_CONFLICT_COUNT, conflicts.size());
        args.putInt(ARG_TOTAL_ITEMS, totalItems);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create instance for validation conflicts
     */
    public static SmartShiftsConflictResolutionDialogFragment newInstanceForValidation(
            @NonNull List<String> validationErrors,
            @NonNull UnifiedOperationResult.OperationType operationType
    ) {
        return newInstance(
                validationErrors,
                SmartShiftsExportImportManager.ConflictType.SHIFT_PATTERNS, // Default
                operationType,
                validationErrors.size()
        );
    }

    // ============================================
    // LIFECYCLE METHODS
    // ============================================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setupListener(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = createDialogView();
        AlertDialog dialog = createMaterialDialog(dialogView);

        setupDialogContent(dialogView);
        setupClickListeners(dialog);

        return dialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // ============================================
    // SETUP METHODS
    // ============================================

    /**
     * Parse bundle arguments with validation
     */
    private void parseArguments() {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("SmartShiftsConflictResolutionDialogFragment requires arguments");
        }

        conflicts = args.getStringArrayList(ARG_CONFLICTS);
        if (conflicts == null) conflicts = new ArrayList<>();

        String conflictTypeValue = args.getString(ARG_CONFLICT_TYPE);
        conflictType = SmartShiftsExportImportManager.ConflictType.fromValue(conflictTypeValue);

        String operationTypeValue = args.getString(ARG_OPERATION_TYPE);
        operationType = UnifiedOperationResult.OperationType.fromLegacyValue(operationTypeValue);

        totalItems = args.getInt(ARG_TOTAL_ITEMS, conflicts.size());
    }

    /**
     * Setup listener with type checking
     */
    private void setupListener(@NonNull Context context) {
        if (context instanceof ConflictResolutionListener) {
            listener = (ConflictResolutionListener) context;
        } else if (getParentFragment() instanceof ConflictResolutionListener) {
            listener = (ConflictResolutionListener) getParentFragment();
        } else if (getTargetFragment() instanceof ConflictResolutionListener) {
            listener = (ConflictResolutionListener) getTargetFragment();
        } else {
            throw new ClassCastException(
                    "Host must implement SmartShiftsConflictResolutionDialogFragment.ConflictResolutionListener"
            );
        }
    }

    /**
     * Create dialog view with Material Design 3
     */
    @NonNull
    private View createDialogView() {
        return LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_smartshifts_conflict_resolution, null);
    }

    /**
     * Create Material Design 3 compliant dialog
     */
    @NonNull
    private AlertDialog createMaterialDialog(@NonNull View dialogView) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getConflictTitle())
                .setIcon(getConflictIcon())
                .setView(dialogView)
                .setPositiveButton(R.string.smartshifts_action_resolve, null) // Set later
                .setNegativeButton(R.string.smartshifts_action_skip_all, null) // Set later
                .setNeutralButton(R.string.smartshifts_action_cancel, null) // Set later
                .setCancelable(false) // Force user decision
                .create();
    }

    /**
     * Setup dialog content and validation
     */
    private void setupDialogContent(@NonNull View dialogView) {
        findViews(dialogView);
        setupConflictInformation();
        setupResolutionOptions();
        setupRecommendations();
        setupWarnings();
    }

    /**
     * Setup click listeners with validation
     */
    private void setupClickListeners(@NonNull AlertDialog dialog) {
        dialog.setOnShowListener(dialogInterface -> {
            // Resolve button
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateSelection()) {
                    handleConflictResolution();
                    dialog.dismiss();
                }
            });

            // Skip button
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                handleConflictSkip();
                dialog.dismiss();
            });

            // Cancel button
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                handleConflictCancellation();
                dialog.dismiss();
            });

            // Resolution change listener
            if (radioGroupResolution != null) {
                radioGroupResolution.setOnCheckedChangeListener((group, checkedId) -> {
                    updateWarnings();
                    updateApplyToAllVisibility();
                });
            }
        });
    }

    // ============================================
    // UI SETUP METHODS
    // ============================================

    /**
     * Find all views with null checks
     */
    private void findViews(@NonNull View dialogView) {
        textConflictInfo = dialogView.findViewById(R.id.text_conflict_info);
        textConflictDetails = dialogView.findViewById(R.id.text_conflict_details);
        textRecommendation = dialogView.findViewById(R.id.text_recommendation);
        radioGroupResolution = dialogView.findViewById(R.id.radio_group_resolution);
        checkboxApplyToAll = dialogView.findViewById(R.id.checkbox_apply_to_all);
        warningContainer = dialogView.findViewById(R.id.warning_container);
        textWarning = dialogView.findViewById(R.id.text_warning);
    }

    /**
     * Setup conflict information display
     */
    private void setupConflictInformation() {
        // Main conflict info
        String conflictInfo = getEnhancedConflictInfoText();
        UiHelper.setTextSafely(textConflictInfo, conflictInfo);

        // Detailed conflict list
        String conflictDetails = buildConflictDetailsList();
        UiHelper.setTextSafely(textConflictDetails, conflictDetails);
    }

    /**
     * Setup resolution options with smart defaults
     */
    private void setupResolutionOptions() {
        if (radioGroupResolution != null) {
            // Set smart default based on conflict type and operation
            int defaultResolution = getSmartDefaultResolution();
            radioGroupResolution.check(defaultResolution);
        }
    }

    /**
     * Setup intelligent recommendations
     */
    private void setupRecommendations() {
        String recommendation = getConflictRecommendation();
        UiHelper.setTextSafely(textRecommendation, recommendation);
    }

    /**
     * Setup warning system
     */
    private void setupWarnings() {
        updateWarnings();
    }

    // ============================================
    // CONTENT GENERATION METHODS
    // ============================================

    /**
     * Get enhanced conflict title with context
     */
    @NonNull
    private String getConflictTitle() {
        return getString(R.string.smartshifts_conflict_resolution_title,
                conflictType.getDisplayName(),
                conflicts.size());
    }

    /**
     * Get conflict icon based on severity
     */
    private int getConflictIcon() {
        switch (conflictType) {
            case SHIFT_PATTERNS:
            case USER_ASSIGNMENTS:
                return R.drawable.ic_rounded_warning_24; // High impact
            case TEAM_CONTACTS:
                return R.drawable.ic_rounded_info_24; // Medium impact
            case SHIFT_EVENTS:
            case SETTINGS:
                return R.drawable.ic_rounded_help_24; // Low impact
            default:
                return R.drawable.ic_rounded_info_24;
        }
    }

    /**
     * Build enhanced conflict info text
     */
    @NonNull
    private String getEnhancedConflictInfoText() {
        int conflictCount = conflicts.size();
        double conflictPercentage = totalItems > 0 ? (conflictCount * 100.0 / totalItems) : 0;

        return getString(R.string.smartshifts_conflict_info_enhanced,
                conflictType.getDisplayName(),
                conflictCount,
                totalItems,
                conflictPercentage,
                operationType.getDisplayName());
    }

    /**
     * Build detailed conflicts list with truncation
     */
    @NonNull
    private String buildConflictDetailsList() {
        if (conflicts.isEmpty()) {
            return getString(R.string.smartshifts_no_conflicts_details);
        }

        StringBuilder builder = new StringBuilder();
        int maxDisplay = 5; // Show max 5 items

        for (int i = 0; i < Math.min(conflicts.size(), maxDisplay); i++) {
            builder.append("• ").append(conflicts.get(i)).append("\n");
        }

        if (conflicts.size() > maxDisplay) {
            int remaining = conflicts.size() - maxDisplay;
            builder.append(getString(R.string.smartshifts_conflict_and_more, remaining));
        }

        return builder.toString().trim();
    }

    /**
     * Get intelligent conflict recommendation
     */
    @NonNull
    private String getConflictRecommendation() {
        SmartShiftsExportImportManager.ConflictResolutionStrategy recommendedStrategy =
                SmartShiftsExportImportManager.getRecommendedConflictStrategy(
                        convertToManagerOperationType(operationType)
                );

        return getString(R.string.smartshifts_conflict_recommendation,
                recommendedStrategy.getDisplayName(),
                recommendedStrategy.getDescription());
    }

    /**
     * Get smart default resolution based on context
     */
    private int getSmartDefaultResolution() {
        SmartShiftsExportImportManager.ConflictResolutionStrategy recommended =
                SmartShiftsExportImportManager.getRecommendedConflictStrategy(
                        convertToManagerOperationType(operationType)
                );

        switch (recommended) {
            case SKIP:
                return R.id.radio_skip_existing;
            case REPLACE:
                return R.id.radio_replace_existing;
            case MERGE:
                return R.id.radio_merge_data;
            case RENAME:
                return R.id.radio_rename_conflicts;
            default:
                return R.id.radio_skip_existing; // Safest default
        }
    }

    // ============================================
    // VALIDATION AND WARNING METHODS
    // ============================================

    /**
     * Validate user selection
     */
    private boolean validateSelection() {
        if (radioGroupResolution == null) {
            UiHelper.showErrorMessage(
                    requireActivity(),
                    "Errore nell'interfaccia: impossibile ottenere la selezione"
            );
            return false;
        }

        int selectedId = radioGroupResolution.getCheckedRadioButtonId();
        if (selectedId == -1) {
            UiHelper.showWarningMessage(
                    requireActivity(),
                    "Seleziona una strategia di risoluzione"
            );
            return false;
        }

        return true;
    }

    /**
     * Update warnings based on current selection
     */
    private void updateWarnings() {
        SmartShiftsExportImportManager.ConflictResolutionStrategy selected = getSelectedResolution();

        if (selected != null && selected.isDestructive()) {
            showDestructiveWarning(selected);
        } else {
            hideWarning();
        }
    }

    /**
     * Show warning for destructive operations
     */
    private void showDestructiveWarning(@NonNull SmartShiftsExportImportManager.ConflictResolutionStrategy strategy) {
        if (warningContainer != null && textWarning != null) {
            String warningText = getString(R.string.smartshifts_destructive_operation_warning,
                    strategy.getDisplayName());
            UiHelper.setTextSafely(textWarning, warningText);
            UiHelper.setVisibilitySafely(warningContainer, View.VISIBLE);
        }
    }

    /**
     * Hide warning container
     */
    private void hideWarning() {
        UiHelper.setVisibilitySafely(warningContainer, View.GONE);
    }

    /**
     * Update Apply to All checkbox visibility
     */
    private void updateApplyToAllVisibility() {
        if (checkboxApplyToAll != null) {
            // Show "Apply to All" only for non-destructive operations with multiple conflicts
            SmartShiftsExportImportManager.ConflictResolutionStrategy selected = getSelectedResolution();
            boolean showApplyToAll = conflicts.size() > 1 &&
                    (selected == null || selected.preservesExistingData());

            UiHelper.setVisibilitySafely(
                    checkboxApplyToAll,
                    showApplyToAll ? View.VISIBLE : View.GONE
            );
        }
    }

    // ============================================
    // ACTION HANDLERS
    // ============================================

    /**
     * Handle conflict resolution
     */
    private void handleConflictResolution() {
        SmartShiftsExportImportManager.ConflictResolutionStrategy resolution = getSelectedResolution();
        boolean applyToAll = checkboxApplyToAll != null && checkboxApplyToAll.isChecked();

        ConflictResolutionMetadata metadata = new ConflictResolutionMetadata(
                resolution.getDisplayName(),
                conflicts.size(),
                resolution.isDestructive(),
                resolution.getDescription()
        );

        if (listener != null) {
            listener.onConflictResolved(conflictType, resolution, applyToAll, metadata);
        }
    }

    /**
     * Handle conflict skip
     */
    private void handleConflictSkip() {
        if (listener != null) {
            listener.onConflictResolutionSkipped(
                    conflictType,
                    "User chose to skip conflict resolution"
            );
        }
    }

    /**
     * Handle conflict cancellation
     */
    private void handleConflictCancellation() {
        if (listener != null) {
            listener.onConflictResolutionCancelled(conflictType);
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get selected resolution strategy
     */
    @Nullable
    private SmartShiftsExportImportManager.ConflictResolutionStrategy getSelectedResolution() {
        if (radioGroupResolution == null) return null;

        int selectedId = radioGroupResolution.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_skip_existing) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.SKIP;
        } else if (selectedId == R.id.radio_replace_existing) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.REPLACE;
        } else if (selectedId == R.id.radio_merge_data) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.MERGE;
        } else if (selectedId == R.id.radio_rename_conflicts) {
            return SmartShiftsExportImportManager.ConflictResolutionStrategy.RENAME;
        }

        return SmartShiftsExportImportManager.ConflictResolutionStrategy.SKIP; // Safe default
    }

    /**
     * Convert unified operation type to manager operation type
     */
    @NonNull
    private SmartShiftsExportImportManager.OperationType convertToManagerOperationType(
            @NonNull UnifiedOperationResult.OperationType unifiedType
    ) {
        switch (unifiedType) {
            case IMPORT_FILE:
            case IMPORT_JSON:
            case IMPORT_CSV:
            case IMPORT_XML:
            case IMPORT_EXCEL:
            case IMPORT_ICAL:
                return SmartShiftsExportImportManager.OperationType.IMPORT;
            case IMPORT_CLOUD:
                return SmartShiftsExportImportManager.OperationType.IMPORT_CLOUD;
            case RESTORE_BACKUP:
                return SmartShiftsExportImportManager.OperationType.RESTORE;
            default:
                return SmartShiftsExportImportManager.OperationType.IMPORT; // Default
        }
    }

    // ============================================
    // STATIC UTILITY METHODS
    // ============================================

    /**
     * Show conflict resolution dialog from Activity/Fragment
     */
    public static void showConflictDialog(
            @NonNull androidx.fragment.app.FragmentManager fragmentManager,
            @NonNull List<String> conflicts,
            @NonNull SmartShiftsExportImportManager.ConflictType conflictType,
            @NonNull UnifiedOperationResult.OperationType operationType,
            int totalItems
    ) {
        SmartShiftsConflictResolutionDialogFragment dialog = newInstance(
                conflicts, conflictType, operationType, totalItems
        );
        dialog.show(fragmentManager, TAG);
    }

    /**
     * Show validation conflict dialog
     */
    public static void showValidationDialog(
            @NonNull androidx.fragment.app.FragmentManager fragmentManager,
            @NonNull List<String> validationErrors,
            @NonNull UnifiedOperationResult.OperationType operationType
    ) {
        SmartShiftsConflictResolutionDialogFragment dialog = newInstanceForValidation(
                validationErrors, operationType
        );
        dialog.show(fragmentManager, TAG);
    }
}