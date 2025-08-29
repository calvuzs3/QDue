package net.calvuz.qdue.ui.features.assignment.wizard.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.FragmentDatePositionSelectionBinding;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.assignment.wizard.interfaces.AssignmentWizardInterface;
import net.calvuz.qdue.ui.features.assignment.wizard.models.AssignmentWizardData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DatePositionSelectionFragment - Step 3: Date and Cycle Position Selection
 *
 * <p>Third step of assignment wizard allowing users to select assignment start date
 * and position within the pattern cycle.</p>
 *
 * <h3>Date Selection:</h3>
 * <ul>
 *   <li><strong>Quick Options</strong>: Today, Tomorrow, Next Monday shortcuts</li>
 *   <li><strong>Date Picker</strong>: Full calendar selection for custom dates</li>
 *   <li><strong>Future Only</strong>: Prevents selection of past dates</li>
 * </ul>
 *
 * <h3>Cycle Position:</h3>
 * <ul>
 *   <li><strong>QuattroDue</strong>: Position 1-16 in the standard cycle</li>
 *   <li><strong>Custom Patterns</strong>: Position 1-N based on pattern cycle length</li>
 *   <li><strong>Position Details</strong>: Shows shift type for selected position</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Date Position Selection Fragment
 * @since Clean Architecture Phase 2
 */
public class DatePositionSelectionFragment extends Fragment {

    private static final String TAG = "DatePositionSelectionFragment";

    // ==================== UI COMPONENTS ====================

    private FragmentDatePositionSelectionBinding mBinding;
    private AssignmentWizardInterface mWizardInterface;

    // ==================== STATE ====================

    private LocalDate mSelectedDate = null;
    private int mSelectedCyclePosition = 1;
    private int mMaxCyclePosition = 16; // Default for QuattroDue

    // ==================== FORMATTERS ====================

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

    // ==================== FRAGMENT LIFECYCLE ====================

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AssignmentWizardInterface) {
            mWizardInterface = (AssignmentWizardInterface) context;
        } else {
            throw new RuntimeException("Activity must implement AssignmentWizardInterface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentDatePositionSelectionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        setupDateSelection();
        setupCyclePositionSelection();
        restoreSelections();

        Log.d(TAG, "DatePositionSelectionFragment view created");
    }

    @Override
    public void onResume() {
        super.onResume();
        mWizardInterface.onStepBecameVisible(2);
        updateMaxCyclePosition();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mWizardInterface = null;
    }

    // ==================== UI SETUP ====================

    private void setupUI() {
        // Set internationalized strings
        mBinding.txtStepTitle.setText(R.string.wizard_step_date_position_title);
        mBinding.txtStepDescription.setText(R.string.wizard_step_date_position_description);

        // Date selection section
        mBinding.txtDateSectionLabel.setText(R.string.assignment_date_label);
        mBinding.editDateInput.setHint(R.string.assignment_date_hint);

        // Cycle position section
        mBinding.txtCyclePositionLabel.setText(R.string.cycle_position_label);
        mBinding.editCyclePositionInput.setHint(getString(R.string.cycle_position_hint, mMaxCyclePosition));

        // Quick date options
        setupQuickDateChips();
    }

    private void setupQuickDateChips() {
        // Today chip
        Chip todayChip = new Chip(getContext());
        todayChip.setText(R.string.assignment_date_today);
        todayChip.setCheckable(true);
        todayChip.setOnClickListener(v -> selectQuickDate(LocalDate.now()));
        mBinding.chipGroupQuickDates.addView(todayChip);

        // Tomorrow chip
        Chip tomorrowChip = new Chip(getContext());
        tomorrowChip.setText(R.string.assignment_date_tomorrow);
        tomorrowChip.setCheckable(true);
        tomorrowChip.setOnClickListener(v -> selectQuickDate(LocalDate.now().plusDays(1)));
        mBinding.chipGroupQuickDates.addView(tomorrowChip);

        // Next Monday chip
        LocalDate nextMonday = getNextMonday();
        Chip mondayChip = new Chip(getContext());
        mondayChip.setText(R.string.assignment_date_custom);
        mondayChip.setCheckable(true);
        mondayChip.setOnClickListener(v -> showDatePicker());
        mBinding.chipGroupQuickDates.addView(mondayChip);
    }

    private void setupDateSelection() {
        // Date input field click opens date picker
        mBinding.editDateInput.setOnClickListener(v -> showDatePicker());
        mBinding.editDateInput.setFocusable(false);

        // Clear date button
        mBinding.btnClearDate.setOnClickListener(v -> clearDateSelection());
    }

    private void setupCyclePositionSelection() {
        mBinding.editCyclePositionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                handleCyclePositionInput(s.toString());
            }
        });

        // Quick position buttons
        mBinding.btnPosition1.setOnClickListener(v -> setCyclePosition(1));
        mBinding.btnPositionAuto.setOnClickListener(v -> autoDetectPosition());
    }

    // ==================== DATE SELECTION LOGIC ====================

    private void selectQuickDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            showError(getString(R.string.error_date_in_past));
            return;
        }

        mSelectedDate = date;
        updateDateDisplay();
        notifyDateSelected();

        // Clear chip selections and select the appropriate one
        mBinding.chipGroupQuickDates.clearCheck();
        // TODO: Select appropriate chip based on date
    }

    private void showDatePicker() {
        LocalDate initialDate = mSelectedDate != null ? mSelectedDate : LocalDate.now();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                this::onDateSelected,
                initialDate.getYear(),
                initialDate.getMonthValue() - 1, // Calendar months are 0-based
                initialDate.getDayOfMonth()
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void onDateSelected(DatePicker view, int year, int month, int dayOfMonth) {
        LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth); // Calendar months are 0-based

        if (selectedDate.isBefore(LocalDate.now())) {
            showError(getString(R.string.error_date_in_past));
            return;
        }

        mSelectedDate = selectedDate;
        updateDateDisplay();
        notifyDateSelected();
        mBinding.chipGroupQuickDates.clearCheck();

        Log.d(TAG, "Date selected from picker: " + selectedDate);
    }

    private void clearDateSelection() {
        mSelectedDate = null;
        mBinding.editDateInput.setText("");
        mBinding.txtDateDisplay.setText("");
        mBinding.chipGroupQuickDates.clearCheck();

        // Clear cycle position dependent on date
        updateCyclePositionUI();
    }

    private void updateDateDisplay() {
        if (mSelectedDate != null) {
            mBinding.editDateInput.setText(mSelectedDate.format(DATE_FORMATTER));
            mBinding.txtDateDisplay.setText(mSelectedDate.format(DISPLAY_FORMATTER));
            mBinding.txtDateDisplay.setVisibility(View.VISIBLE);
        } else {
            mBinding.txtDateDisplay.setVisibility(View.GONE);
        }
        updateCyclePositionUI();
    }

    // ==================== CYCLE POSITION LOGIC ====================

    private void handleCyclePositionInput(String input) {
        try {
            if (input.trim().isEmpty()) {
                mSelectedCyclePosition = 1;
                updateCyclePositionDetails(1);
                return;
            }

            int position = Integer.parseInt(input.trim());
            if (position >= 1 && position <= mMaxCyclePosition) {
                mSelectedCyclePosition = position;
                updateCyclePositionDetails(position);
                notifyCyclePositionSelected();
                mBinding.textInputCyclePosition.setError(null);
            } else {
                mBinding.textInputCyclePosition.setError(
                        getString(R.string.error_invalid_cycle_position, mMaxCyclePosition));
            }
        } catch (NumberFormatException e) {
            mBinding.textInputCyclePosition.setError(
                    getString(R.string.error_invalid_cycle_position, mMaxCyclePosition));
        }
    }

    private void setCyclePosition(int position) {
        if (position >= 1 && position <= mMaxCyclePosition) {
            mSelectedCyclePosition = position;
            mBinding.editCyclePositionInput.setText(String.valueOf(position));
            updateCyclePositionDetails(position);
            notifyCyclePositionSelected();
        }
    }

    private void autoDetectPosition() {
        // Simple auto-detect: start at position 1
        setCyclePosition(1);
        Log.d(TAG, "Auto-detected cycle position: 1");
    }

    private void updateCyclePositionDetails(int position) {
        String details = getCyclePositionDescription(position);
        mBinding.txtCyclePositionDetails.setText(
                getString(R.string.cycle_position_details_format, position, details));
        mBinding.txtCyclePositionDetails.setVisibility(View.VISIBLE);
    }

    private void updateCyclePositionUI() {
        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.QUATTRODUE) {
            mBinding.txtCyclePositionDescription.setText(R.string.cycle_position_description_quattrodue);
            mMaxCyclePosition = 16;
        } else {
            mBinding.txtCyclePositionDescription.setText(R.string.cycle_position_description_custom);
            // TODO: Get actual cycle length from selected custom pattern
            mMaxCyclePosition = 14; // Default fallback
        }

        mBinding.editCyclePositionInput.setHint(getString(R.string.cycle_position_hint, mMaxCyclePosition));
    }

    // ==================== CYCLE POSITION DESCRIPTIONS ====================

    private String getCyclePositionDescription(int position) {
        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.QUATTRODUE) {
            return getQuattroDuePositionDescription(position);
        } else {
            return getString(R.string.cycle_day_work); // Generic for custom patterns
        }
    }

    private String getQuattroDuePositionDescription(int position) {
        // QuattroDue cycle: 4M, 2R, 4N, 2R, 4P, 2R (16 days)
        if (position >= 1 && position <= 4) {
            return getString(R.string.cycle_day_morning);
        } else if (position >= 5 && position <= 6) {
            return getString(R.string.cycle_day_rest);
        } else if (position >= 7 && position <= 10) {
            return getString(R.string.cycle_day_night);
        } else if (position >= 11 && position <= 12) {
            return getString(R.string.cycle_day_rest);
        } else if (position >= 13 && position <= 16) {
            return getString(R.string.cycle_day_afternoon);
        } else {
            return getString(R.string.cycle_day_work);
        }
    }

    // ==================== UTILITY METHODS ====================

    private LocalDate getNextMonday() {
        LocalDate now = LocalDate.now();
        int daysUntilMonday = (8 - now.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7; // Next Monday if today is Monday
        return now.plusDays(daysUntilMonday);
    }

    private void updateMaxCyclePosition() {
        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        if (wizardData.getPatternType() == AssignmentWizardData.PatternType.CUSTOM
                && wizardData.getSelectedCustomPattern() != null) {
            mMaxCyclePosition = wizardData.getSelectedCustomPattern().getCycleLength();
        } else {
            mMaxCyclePosition = 16; // QuattroDue default
        }

        updateCyclePositionUI();
    }

    // ==================== WIZARD INTERFACE NOTIFICATIONS ====================

    private void notifyDateSelected() {
        if (mSelectedDate != null) {
            mWizardInterface.onDateSelected(mSelectedDate);
        }
    }

    private void notifyCyclePositionSelected() {
        mWizardInterface.onCycleDayPositionSelected(mSelectedCyclePosition);
    }

    // ==================== STATE MANAGEMENT ====================

    private void restoreSelections() {
        if (mWizardInterface == null) return;

        AssignmentWizardData wizardData = mWizardInterface.getWizardData();

        // Restore date selection
        LocalDate savedDate = wizardData.getAssignmentDate();
        if (savedDate != null) {
            mSelectedDate = savedDate;
            updateDateDisplay();
        }

        // Restore cycle position
        int savedPosition = wizardData.getCycleDayPosition();
        if (savedPosition > 0) {
            mSelectedCyclePosition = savedPosition;
            mBinding.editCyclePositionInput.setText(String.valueOf(savedPosition));
            updateCyclePositionDetails(savedPosition);
        }
    }

    // ==================== ERROR HANDLING ====================

    private void showError(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== VALIDATION ====================

    public boolean isValid() {
        return mWizardInterface != null && mWizardInterface.getWizardData().hasDateAndPositionSelected();
    }

    @Nullable
    public String getValidationError() {
        if (mSelectedDate == null) {
            return getString(R.string.validation_date_required);
        }
        if (mSelectedCyclePosition < 1) {
            return getString(R.string.validation_cycle_position_required);
        }
        return null;
    }
}