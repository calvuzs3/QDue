// ===== PACKAGE: net.calvuz.qdue.smartshifts.ui.calendar =====

package net.calvuz.qdue.smartshifts.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.calvuz.qdue.databinding.FragmentSmartShiftsCalendarBinding;
import net.calvuz.qdue.smartshifts.ui.calendar.adapters.CalendarAdapter;
import net.calvuz.qdue.smartshifts.ui.calendar.adapters.ShiftLegendAdapter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment displaying calendar view of user shifts
 * Shows monthly calendar with shift indicators and legend
 */
@AndroidEntryPoint
public class SmartShiftsCalendarFragment extends Fragment {

    private FragmentSmartShiftsCalendarBinding binding;
    private CalendarViewModel viewModel;

    private CalendarAdapter calendarAdapter;
    private ShiftLegendAdapter legendAdapter;

    private YearMonth currentYearMonth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSmartShiftsCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        setupCalendar();
        setupLegend();
        setupNavigation();
        setupObservers();

        // Initialize to current month
        currentYearMonth = YearMonth.now();
        loadMonth(currentYearMonth);
    }

    /**
     * Setup calendar RecyclerView
     */
    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter(day -> {
            // Handle day click
            viewModel.selectDate(day.getDate());
        });

        binding.calendarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        binding.calendarRecyclerView.setAdapter(calendarAdapter);
    }

    /**
     * Setup shift legend RecyclerView
     */
    private void setupLegend() {
        legendAdapter = new ShiftLegendAdapter();

        binding.legendRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.legendRecyclerView.setAdapter(legendAdapter);
    }

    /**
     * Setup month navigation
     */
    private void setupNavigation() {
        binding.btnPreviousMonth.setOnClickListener(v -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            loadMonth(currentYearMonth);
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            loadMonth(currentYearMonth);
        });

        binding.btnToday.setOnClickListener(v -> {
            currentYearMonth = YearMonth.now();
            loadMonth(currentYearMonth);
            viewModel.selectDate(LocalDate.now());
        });

        binding.fabGoToToday.setOnClickListener(v -> {
            currentYearMonth = YearMonth.now();
            loadMonth(currentYearMonth);
            viewModel.selectDate(LocalDate.now());
        });
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe user assignment
        viewModel.getUserAssignment().observe(getViewLifecycleOwner(), assignment -> {
            if (assignment != null) {
                updateAssignmentCard(assignment);
            } else {
                hideAssignmentCard();
            }
        });

        // Observe calendar days
        viewModel.getCalendarDays().observe(getViewLifecycleOwner(), days -> {
            calendarAdapter.updateDays(days);
        });

        // Observe shift types for legend
        viewModel.getShiftTypes().observe(getViewLifecycleOwner(), shiftTypes -> {
            legendAdapter.updateShiftTypes(shiftTypes);
        });

        // Observe selected date
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), selectedDate -> {
            calendarAdapter.updateSelectedDate(selectedDate);
            // Could show shift details for selected date
        });

        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state if needed
        });
    }

    /**
     * Load specific month
     */
    private void loadMonth(YearMonth yearMonth) {
        // Update month display
        updateMonthDisplay(yearMonth);

        // Load month data
        viewModel.loadMonth(yearMonth);
    }

    /**
     * Update month display text
     */
    private void updateMonthDisplay(YearMonth yearMonth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN);
        String monthText = yearMonth.format(formatter);
        binding.currentMonthYear.setText(capitalize(monthText));
    }

    /**
     * Update assignment card display
     */
    private void updateAssignmentCard(net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment assignment) {
        binding.assignmentCard.setVisibility(View.VISIBLE);

        // Update title with pattern name - would need to join with pattern data
        binding.assignmentTitle.setText("Pattern Attivo");

        // Format assignment details
        String details = String.format("Inizio: %s",
                formatDate(assignment.startDate));
        if (assignment.teamName != null) {
            details += " • " + assignment.teamName;
        }

        binding.assignmentDescription.setText(details);
    }

    /**
     * Hide assignment card when no assignment
     */
    private void hideAssignmentCard() {
        binding.assignmentCard.setVisibility(View.GONE);
    }

    /**
     * Format date string for display
     */
    private String formatDate(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return date.format(formatter);
        } catch (Exception e) {
            return isoDate;
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
