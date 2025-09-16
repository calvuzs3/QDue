package net.calvuz.qdue.ui.features.events.local.presentation.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.Chip;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.features.events.local.presentation.LocalEventsActivity;
import net.calvuz.qdue.ui.features.events.local.viewmodels.LocalEventsViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * EventsSearchFilterFragment - Search and Filter Bottom Sheet (MVVM)
 *
 * <p>Bottom sheet dialog for filtering events with multiple criteria. Provides intuitive
 * interface for searching and filtering LocalEvents following Material Design guidelines
 * and MVVM architecture integration.</p>
 *
 * <h3>Filter Options:</h3>
 * <ul>
 *   <li><strong>Text Search</strong>: Search in title and description</li>
 *   <li><strong>Date Range</strong>: Start and end date filtering</li>
 *   <li><strong>Location Search</strong>: Location-based filtering</li>
 *   <li><strong>All-Day Toggle</strong>: Filter for all-day events only</li>
 * </ul>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Real-time Search</strong>: Live filtering as user types</li>
 *   <li><strong>Date Range Selection</strong>: Material date pickers</li>
 *   <li><strong>Filter Persistence</strong>: Maintains filter state</li>
 *   <li><strong>Clear All</strong>: Reset all filters quickly</li>
 *   <li><strong>MVVM Integration</strong>: Communicates with LocalEventsViewModel</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since LocalEvents MVVM Implementation
 */
public class EventsSearchFilterFragment extends BottomSheetDialogFragment {

    private static final String TAG = "EventsSearchFilterFragment";

    // ==================== UI COMPONENTS ====================

    // Search Fields
    private TextInputEditText mSearchQueryEditText;
    private TextInputEditText mLocationSearchEditText;

    // Date Range
    private MaterialButton mStartDateButton;
    private MaterialButton mEndDateButton;
    private Chip mStartDateChip;
    private Chip mEndDateChip;

    // Toggles
    private CheckBox mAllDayOnlyCheckBox;

    // Action Buttons
    private MaterialButton mApplyFiltersButton;
    private MaterialButton mClearAllButton;
    private MaterialButton mCancelButton;

    // ==================== VIEWMODELS ====================

    private LocalEventsViewModel mEventsViewModel;

    // ==================== FILTER STATE ====================

    private String mCurrentSearchQuery = "";
    private String mCurrentLocationQuery = "";
    private LocalDate mStartDate = null;
    private LocalDate mEndDate = null;
    private boolean mAllDayOnly = false;

    // ==================== DATE FORMATTER ====================

    private final DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== LIFECYCLE ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get ViewModel from parent Activity
        if (getActivity() instanceof LocalEventsActivity activity) {
            mEventsViewModel = activity.getEventsViewModel();
        }

        if (mEventsViewModel == null) {
            Log.e( TAG, "Could not get LocalEventsViewModel from parent Activity");
            dismiss();
            return;
        }

        // Load current filter state from ViewModel
        loadCurrentFilterState();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events_search_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners();
        setupTextWatchers();
        updateUIWithCurrentState();
    }

    // ==================== INITIALIZATION ====================

    private void initializeViews(@NonNull View view) {
        // Search Fields
        mSearchQueryEditText = view.findViewById(R.id.search_query_edit_text);
        mLocationSearchEditText = view.findViewById(R.id.location_search_edit_text);

        // Date Range
        mStartDateButton = view.findViewById(R.id.start_date_button);
        mEndDateButton = view.findViewById(R.id.end_date_button);
        mStartDateChip = view.findViewById(R.id.start_date_chip);
        mEndDateChip = view.findViewById(R.id.end_date_chip);

        // Toggles
        mAllDayOnlyCheckBox = view.findViewById(R.id.all_day_only_checkbox);

        // Action Buttons
        mApplyFiltersButton = view.findViewById(R.id.apply_filters_button);
        mClearAllButton = view.findViewById(R.id.clear_all_button);
        mCancelButton = view.findViewById(R.id.cancel_button);
    }

    private void setupClickListeners() {
        // Date Range Buttons
        mStartDateButton.setOnClickListener(v -> showStartDatePicker());
        mEndDateButton.setOnClickListener(v -> showEndDatePicker());

        // Date Chips (for clearing individual dates)
        mStartDateChip.setOnCloseIconClickListener(v -> clearStartDate());
        mEndDateChip.setOnCloseIconClickListener(v -> clearEndDate());

        // Action Buttons
        mApplyFiltersButton.setOnClickListener(v -> applyFilters());
        mClearAllButton.setOnClickListener(v -> clearAllFilters());
        mCancelButton.setOnClickListener(v -> dismiss());

        // All-day checkbox
        mAllDayOnlyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAllDayOnly = isChecked;
            updateApplyButtonState();
        });
    }

    private void setupTextWatchers() {
        // Search query text watcher
        mSearchQueryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentSearchQuery = s.toString().trim();
                updateApplyButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Location search text watcher
        mLocationSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentLocationQuery = s.toString().trim();
                updateApplyButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ==================== FILTER STATE MANAGEMENT ====================

    private void loadCurrentFilterState() {
        if (mEventsViewModel != null) {
            // Get current search query
            mCurrentSearchQuery = mEventsViewModel.getSearchQuery();

            // Get current filter
            LocalEventsViewModel.EventFilter filter = mEventsViewModel.getCurrentFilter();

            // Extract date range from filter
            if (filter.getStartDateFrom() != null) {
                mStartDate = filter.getStartDateFrom().toLocalDate();
            }
            if (filter.getStartDateTo() != null) {
                mEndDate = filter.getStartDateTo().toLocalDate();
            }

            // Extract all-day filter
            if (filter.getAllDay() != null) {
                mAllDayOnly = filter.getAllDay();
            }
        }
    }

    private void updateUIWithCurrentState() {
        // Update search fields
        mSearchQueryEditText.setText(mCurrentSearchQuery);
        mLocationSearchEditText.setText(mCurrentLocationQuery);

        // Update date chips
        updateDateChips();

        // Update checkboxes
        mAllDayOnlyCheckBox.setChecked(mAllDayOnly);

        // Update apply button
        updateApplyButtonState();
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

    private void updateApplyButtonState() {
        boolean hasFilters = !mCurrentSearchQuery.isEmpty() ||
                !mCurrentLocationQuery.isEmpty() ||
                mStartDate != null ||
                mEndDate != null ||
                mAllDayOnly;

        mApplyFiltersButton.setEnabled(hasFilters);
        mClearAllButton.setEnabled(hasFilters);
    }

    // ==================== DATE PICKERS ====================

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
                    updateApplyButtonState();
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
                    updateApplyButtonState();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    // ==================== DATE CLEARING ====================

    private void clearStartDate() {
        mStartDate = null;
        updateDateChips();
        updateApplyButtonState();
    }

    private void clearEndDate() {
        mEndDate = null;
        updateDateChips();
        updateApplyButtonState();
    }

    // ==================== FILTER ACTIONS ====================

    private void applyFilters() {
        if (mEventsViewModel == null) {
            return;
        }

        // Apply search if there's a query
        if (!mCurrentSearchQuery.isEmpty()) {
            mEventsViewModel.searchEvents(mCurrentSearchQuery);
        }

        // Apply location-based search if specified
        if (!mCurrentLocationQuery.isEmpty()) {
            // For now, we'll treat location as part of the general search
            // This could be enhanced to be a separate filter
            String combinedQuery = mCurrentSearchQuery;
            if (!combinedQuery.isEmpty() && !mCurrentLocationQuery.isEmpty()) {
                combinedQuery += " " + mCurrentLocationQuery;
            } else if (!mCurrentLocationQuery.isEmpty()) {
                combinedQuery = mCurrentLocationQuery;
            }

            if (!combinedQuery.isEmpty()) {
                mEventsViewModel.searchEvents(combinedQuery);
            }
        }

        // Create EventFilter for date range and all-day filtering
        LocalEventsViewModel.EventFilter filter = new LocalEventsViewModel.EventFilter();

        if (mStartDate != null) {
            filter.setStartDateFrom(mStartDate.atStartOfDay());
        }
        if (mEndDate != null) {
            filter.setStartDateTo(mEndDate.atTime(23, 59));
        }
        if (mAllDayOnly) {
            filter.setAllDay(true);
        }

        // Apply filter if any criteria is set
        if (!filter.isEmpty()) {
            mEventsViewModel.applyFilter(filter);
        }

        // If no search and no filter, just use current filters
        if (mCurrentSearchQuery.isEmpty() && mCurrentLocationQuery.isEmpty() && filter.isEmpty()) {
            // Do nothing - keep current state
        }

        Log.d(TAG, "Applied filters - search: '" + mCurrentSearchQuery +
                "', location: '" + mCurrentLocationQuery +
                "', dateRange: " + mStartDate + " to " + mEndDate +
                ", allDay: " + mAllDayOnly);

        dismiss();
    }

    private void clearAllFilters() {
        if (mEventsViewModel == null) {
            return;
        }

        // Reset all filter state
        mCurrentSearchQuery = "";
        mCurrentLocationQuery = "";
        mStartDate = null;
        mEndDate = null;
        mAllDayOnly = false;

        // Update UI
        updateUIWithCurrentState();

        // Clear filters in ViewModel
        mEventsViewModel.clearFilter();

        Log.d(TAG, "Cleared all filters");

        dismiss();
    }

    // ==================== FACTORY METHOD ====================

    /**
     * Create new instance of EventsSearchFilterFragment
     */
    public static EventsSearchFilterFragment newInstance() {
        return new EventsSearchFilterFragment();
    }
}