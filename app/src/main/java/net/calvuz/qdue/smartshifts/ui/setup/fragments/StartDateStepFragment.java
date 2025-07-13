package net.calvuz.qdue.smartshifts.ui.setup.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import net.calvuz.qdue.databinding.FragmentStartDateSelectionBinding;
import net.calvuz.qdue.smartshifts.ui.setup.SetupWizardViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

/**
 * Start date selection step fragment
 * Allows user to select when their shift cycle begins
 */
public class StartDateStepFragment extends Fragment {

    private FragmentStartDateSelectionBinding binding;
    private SetupWizardViewModel viewModel;
    private LocalDate selectedDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStartDateSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SetupWizardViewModel.class);

        setupDateSelection();
        setupObservers();

        // Default to today
        setSelectedDate(LocalDate.now());
    }

    /**
     * Setup date selection UI
     */
    private void setupDateSelection() {
        binding.btnSelectDate.setOnClickListener(v -> showDatePicker());
        binding.selectedDateCard.setOnClickListener(v -> showDatePicker());
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe any relevant data if needed
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate newDate = LocalDate.of(year, month + 1, dayOfMonth);
                    setSelectedDate(newDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    /**
     * Set selected date and update UI
     */
    private void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        updateDateDisplay();
        viewModel.setSelectedStartDate(date);
    }

    /**
     * Update date display
     */
    private void updateDateDisplay() {
        if (selectedDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ITALIAN);
            String formattedDate = selectedDate.format(formatter);

            binding.selectedDateText.setText(capitalize(formattedDate));
            binding.selectedDateCard.setVisibility(View.VISIBLE);
            binding.btnSelectDate.setText("Cambia Data");
        }
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
