package net.calvuz.qdue.ui.features.schedulepattern;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.services.CalendarService;
import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.Shift;
import net.calvuz.qdue.domain.calendar.models.UserScheduleAssignment;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleShift;
import net.calvuz.qdue.domain.calendar.repositories.ShiftRepository;
import net.calvuz.qdue.ui.features.schedulepattern.adapters.PatternDayItemAdapter;
import net.calvuz.qdue.ui.features.schedulepattern.adapters.ShiftSelectionAdapter;
import net.calvuz.qdue.ui.features.schedulepattern.models.PatternDay;
import net.calvuz.qdue.ui.features.schedulepattern.di.SchedulePatternModule;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.data.services.UserSchedulePatternService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * UserSchedulePatternCreationActivity - User Work Schedule Pattern Creation Interface
 *
 * <p>Standalone activity that allows users to create their personalized work schedule pattern
 * (recurrence rule) by selecting a start date and defining a sequence of shift assignments
 * that will repeat cyclically indefinitely.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Start Date Selection</strong>: DatePicker for pattern start date</li>
 *   <li><strong>Pattern Construction</strong>: Add/remove shift tiles in sequence</li>
 *   <li><strong>Shift Selection</strong>: Choose from database shifts (morning, night, etc.)</li>
 *   <li><strong>Rest Day Support</strong>: Handle rest as absence of shifts</li>
 *   <li><strong>Pattern Preview</strong>: Visual representation of created pattern</li>
 *   <li><strong>Modification Support</strong>: Edit existing patterns</li>
 * </ul>
 *
 * <h3>Architecture:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Implements Injectable pattern</li>
 *   <li><strong>Single Page Form</strong>: All functionality in one view</li>
 *   <li><strong>Clean Architecture</strong>: Domain models and services integration</li>
 *   <li><strong>Internationalization</strong>: Localized using project i18n system</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Initial Implementation
 * @since Clean Architecture Phase 2
 */
public class UserSchedulePatternCreationActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = "UserSchedulePatternCreationActivity";

    // ==================== CONSTANTS ====================

    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";
    public static final String EXTRA_ASSIGNMENT_ID = "extra_assignment_id";

    // ==================== INJECTED DEPENDENCIES ====================

    private ServiceProvider mServiceProvider;
    private CalendarService mCalendarService;
    private ShiftRepository mShiftRepository;
    private LocaleManager mLocaleManager;

    // ==================== UI COMPONENTS ====================

    private Toolbar mToolbar;
    private TextView mStartDateLabel;
    private TextView mStartDateValue;
    private Button mSelectStartDateButton;
    private TextView mPatternStatusLabel;
    private TextView mPatternLengthValue;
    private RecyclerView mPatternDaysRecyclerView;
    private RecyclerView mAvailableShiftsRecyclerView;
    private FloatingActionButton mAddPatternDayFab;
    private Button mSavePatternButton;
    private Button mPreviewPatternButton;
    private ProgressBar mLoadingProgressBar;
    private View mMainContentView;

    // ==================== ADAPTERS ====================

    private PatternDayItemAdapter mPatternDaysAdapter;
    private ShiftSelectionAdapter mShiftSelectionAdapter;

    // ==================== DATA ====================

    private LocalDate mSelectedStartDate;
    private List<PatternDay> mPatternDays;
    private List<Shift> mAvailableShifts;
    private boolean mIsEditMode;
    private String mEditingAssignmentId;

    // ==================== LIFECYCLE ====================

    /**
     * Static factory method for creating intent to launch this activity.
     *
     * @param context Context to create intent from
     * @return Intent configured for this activity
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, UserSchedulePatternCreationActivity.class);
    }

    /**
     * Static factory method for editing existing pattern.
     *
     * @param context Context to create intent from
     * @param assignmentId UserScheduleAssignment ID to edit
     * @return Intent configured for editing mode
     */
    @NonNull
    public static Intent createEditIntent(@NonNull Context context, @NonNull String assignmentId) {
        Intent intent = new Intent(context, UserSchedulePatternCreationActivity.class);
        intent.putExtra(EXTRA_EDIT_MODE, true);
        intent.putExtra(EXTRA_ASSIGNMENT_ID, assignmentId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_schedule_pattern_creation);

        // Dependency injection
        DependencyInjector.inject(this, this);

        // Verify dependencies
        if (!areDependenciesReady()) {
            Log.e(TAG, "Dependencies not ready, finishing activity");
            showError(getString(R.string.error_dependencies_not_ready));
            finish();
            return;
        }

        // Initialize data
        initializeData();

        // Setup UI
        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();

        // Load initial data
        loadAvailableShifts();

        Log.d(TAG, "UserSchedulePatternCreationActivity created successfully");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_schedule_pattern_creation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_clear_pattern) {
            clearPattern();
            return true;
        } else if (itemId == R.id.action_help) {
            showHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(@NonNull ServiceProvider serviceProvider) {

        //Save serviceProvider
        mServiceProvider = serviceProvider;

        // Get calendar-related services
        mCalendarService = serviceProvider.getCalendarService();

        // Get repository through DI module
        SchedulePatternModule module = new SchedulePatternModule(this, serviceProvider);
        mShiftRepository = module.getShiftRepository();
        mLocaleManager = module.getLocaleManager();

        Log.d(TAG, "Dependencies injected successfully");
    }

    @Override
    public boolean areDependenciesReady() {
        return mCalendarService != null &&
                mShiftRepository != null &&
                mLocaleManager != null;
    }

    // ==================== INITIALIZATION ====================

    private void initializeData() {
        // Parse intent extras
        Intent intent = getIntent();
        mIsEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false);
        mEditingAssignmentId = intent.getStringExtra(EXTRA_ASSIGNMENT_ID);

        // Initialize collections
        mPatternDays = new ArrayList<>();
        mAvailableShifts = new ArrayList<>();

        // Set default start date to tomorrow
        mSelectedStartDate = LocalDate.now().plusDays(1);

        Log.d(TAG, "Data initialized - Edit mode: " + mIsEditMode);
    }

    private void initializeViews() {
        // Find views
        mToolbar = findViewById(R.id.toolbar);
        mStartDateLabel = findViewById(R.id.tv_start_date_label);
        mStartDateValue = findViewById(R.id.tv_start_date_value);
        mSelectStartDateButton = findViewById(R.id.btn_select_start_date);
        mPatternStatusLabel = findViewById(R.id.tv_pattern_status_label);
        mPatternLengthValue = findViewById(R.id.tv_pattern_length_value);
        mPatternDaysRecyclerView = findViewById(R.id.rv_pattern_days);
        mAvailableShiftsRecyclerView = findViewById(R.id.rv_available_shifts);
        mAddPatternDayFab = findViewById(R.id.fab_add_pattern_day);
        mSavePatternButton = findViewById(R.id.btn_save_pattern);
        mPreviewPatternButton = findViewById(R.id.btn_preview_pattern);
        mLoadingProgressBar = findViewById(R.id.progress_loading);
        mMainContentView = findViewById(R.id.main_content);

        // Update start date display
        updateStartDateDisplay();
        updatePatternStatusDisplay();

        Log.d(TAG, "Views initialized");
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIsEditMode ?
                    R.string.title_edit_schedule_pattern :
                    R.string.title_create_schedule_pattern);
        }
    }

    private void setupRecyclerViews() {
        // Pattern days RecyclerView
        mPatternDaysAdapter = new PatternDayItemAdapter(mPatternDays, new PatternDayItemAdapter.OnPatternDayInteractionListener() {
            @Override
            public void onRemovePatternDay(int position) {
                removePatternDay(position);
            }

            @Override
            public void onEditPatternDay(int position) {
                editPatternDay(position);
            }
        });

        mPatternDaysRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mPatternDaysRecyclerView.setAdapter(mPatternDaysAdapter);

        // Available shifts RecyclerView
        mShiftSelectionAdapter = new ShiftSelectionAdapter(mAvailableShifts, new ShiftSelectionAdapter.OnShiftSelectionListener() {
            @Override
            public void onShiftSelected(@NonNull Shift shift) {
                addPatternDayWithShift(shift);
            }

            @Override
            public void onRestDaySelected() {
                addRestDay();
            }
        });

        mAvailableShiftsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mAvailableShiftsRecyclerView.setAdapter(mShiftSelectionAdapter);

        Log.d(TAG, "RecyclerViews setup completed");
    }

    private void setupClickListeners() {
        mSelectStartDateButton.setOnClickListener(v -> showStartDatePicker());
        mAddPatternDayFab.setOnClickListener(v -> showShiftSelectionDialog());
        mSavePatternButton.setOnClickListener(v -> savePattern());
        mPreviewPatternButton.setOnClickListener(v -> previewPattern());
    }

    // ==================== DATA LOADING ====================

    private void loadAvailableShifts() {
        showLoading(true);

        mShiftRepository.getAllShifts()
                .thenAccept(shifts -> runOnUiThread(() -> {
                    if (shifts != null && !shifts.isEmpty()) {
                        mAvailableShifts.clear();
                        mAvailableShifts.addAll(shifts);
                        mShiftSelectionAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + shifts.size() + " available shifts");
                    } else {
                        showError(getString(R.string.error_no_shifts_available));
                    }
                    showLoading(false);
                }))
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error loading shifts", throwable);
                        showError(getString(R.string.error_loading_shifts));
                        showLoading(false);
                    });
                    return null;
                });
    }

    // ==================== UI UPDATES ====================

    private void updateStartDateDisplay() {
        if (mSelectedStartDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
            String formattedDate = mSelectedStartDate.format(formatter);
            mStartDateValue.setText(formattedDate);
        }
    }

    private void updatePatternStatusDisplay() {
        int patternLength = mPatternDays.size();
        mPatternLengthValue.setText(getString(R.string.pattern_length_days, patternLength));

        // Update save button state
        mSavePatternButton.setEnabled(patternLength > 0 && mSelectedStartDate != null);
        mPreviewPatternButton.setEnabled(patternLength > 0);

        // Update empty state visibility
        updateEmptyStateVisibility();
    }

    private void updateEmptyStateVisibility() {
        View emptyLayout = findViewById(R.id.layout_empty_pattern);
        if (emptyLayout != null) {
            boolean isEmpty = mPatternDays.isEmpty();
            emptyLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            mPatternDaysRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private String generatePatternNameFromCurrent() {
        if (mPatternDays.isEmpty() || mSelectedStartDate == null) {
            return "Schema Lavoro Personalizzato";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return String.format("Schema %d giorni - dal %s",
                mPatternDays.size(),
                mSelectedStartDate.format(formatter));
    }

    private void showPatternPreviewDialog(@NonNull List<WorkScheduleDay> previewDays) {
        if (previewDays.isEmpty()) {
            showError("No preview data available");
            return;
        }

        // Create preview text
        StringBuilder previewText = new StringBuilder();
        previewText.append(getString(R.string.preview_cycle_info, mPatternDays.size()));
        previewText.append("\n\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM (EEEE)");

        for (int i = 0; i < Math.min(previewDays.size(), 7); i++) { // Show first 7 days
            WorkScheduleDay day = previewDays.get(i);
            previewText.append(day.getDate().format(dateFormatter));
            previewText.append(": ");

            if (day.getWorkShifts().isEmpty()) {
                previewText.append(getString(R.string.pattern_day_rest));
            } else {
                // Show first shift (assuming one shift per day in pattern)
                WorkScheduleShift shift = day.getWorkShifts().get( 0);
                previewText.append(shift.getShift().getName());
                if (shift.getStartTime() != null && shift.getEndTime() != null) {
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                    previewText.append(" (")
                            .append(shift.getStartTime().format(timeFormatter))
                            .append(" - ")
                            .append(shift.getEndTime().format(timeFormatter))
                            .append(")");
                }
            }
            previewText.append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.preview_title)
                .setMessage(previewText.toString())
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private ServiceProvider getServiceProvider() {
        return mServiceProvider;
    }

    private void showLoading(boolean show) {
        mLoadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mMainContentView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(@NonNull String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void showSuccess(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ==================== UI INTERACTIONS ====================

    private void showStartDatePicker() {
        LocalDate initialDate = mSelectedStartDate != null ? mSelectedStartDate : LocalDate.now().plusDays(1);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    mSelectedStartDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateStartDateDisplay();
                    updatePatternStatusDisplay();
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showShiftSelectionDialog() {
        if (mAvailableShifts.isEmpty()) {
            showError(getString(R.string.error_no_shifts_available));
            return;
        }

        // Create dialog with shift options
        String[] shiftNames = new String[mAvailableShifts.size() + 1]; // +1 for rest day

        // Add shift names
        for (int i = 0; i < mAvailableShifts.size(); i++) {
            shiftNames[i] = mAvailableShifts.get(i).getName();
        }

        // Add rest day option
        shiftNames[mAvailableShifts.size()] = getString(R.string.pattern_day_rest);

        new AlertDialog.Builder(this)
                .setTitle(R.string.shift_selection_choose_shift)
                .setItems(shiftNames, (dialog, which) -> {
                    if (which < mAvailableShifts.size()) {
                        // Selected a shift
                        Shift selectedShift = mAvailableShifts.get(which);
                        addPatternDayWithShift(selectedShift);
                    } else {
                        // Selected rest day
                        addRestDay();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void addPatternDayWithShift(@NonNull Shift shift) {
        PatternDay patternDay = new PatternDay(mPatternDays.size() + 1, shift);
        mPatternDays.add(patternDay);
        mPatternDaysAdapter.notifyItemInserted(mPatternDays.size() - 1);
        updatePatternStatusDisplay();

        Log.d(TAG, "Added pattern day with shift: " + shift.getName());
    }

    private void addRestDay() {
        PatternDay patternDay = new PatternDay(mPatternDays.size() + 1, null); // null = rest day
        mPatternDays.add(patternDay);
        mPatternDaysAdapter.notifyItemInserted(mPatternDays.size() - 1);
        updatePatternStatusDisplay();

        Log.d(TAG, "Added rest day to pattern");
    }

    private void removePatternDay(int position) {
        if (position >= 0 && position < mPatternDays.size()) {
            mPatternDays.remove(position);

            // Update day numbers
            for (int i = position; i < mPatternDays.size(); i++) {
                mPatternDays.get(i).setDayNumber(i + 1);
            }

            mPatternDaysAdapter.notifyDataSetChanged();
            updatePatternStatusDisplay();

            Log.d(TAG, "Removed pattern day at position: " + position);
        }
    }

    private void editPatternDay(int position) {
        if (position >= 0 && position < mPatternDays.size()) {
            PatternDay currentDay = mPatternDays.get(position);

            if (mAvailableShifts.isEmpty()) {
                showError(getString(R.string.error_no_shifts_available));
                return;
            }

            // Create dialog with current selection highlighted
            String[] shiftNames = new String[mAvailableShifts.size() + 1];
            int currentSelection = -1;

            // Add shift names and find current selection
            for (int i = 0; i < mAvailableShifts.size(); i++) {
                Shift shift = mAvailableShifts.get(i);
                shiftNames[i] = shift.getName();

                if (currentDay.isWorkDay() && currentDay.getShift() != null &&
                        shift.getId().equals(currentDay.getShift().getId())) {
                    currentSelection = i;
                }
            }

            // Add rest day option
            shiftNames[mAvailableShifts.size()] = getString(R.string.pattern_day_rest);
            if (currentDay.isRestDay()) {
                currentSelection = mAvailableShifts.size();
            }

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_edit_pattern_day_title, currentDay.getDayNumber()))
                    .setSingleChoiceItems(shiftNames, currentSelection, (dialog, which) -> {
                        PatternDay updatedDay;
                        if (which < mAvailableShifts.size()) {
                            // Selected a shift
                            Shift selectedShift = mAvailableShifts.get(which);
                            updatedDay = new PatternDay(currentDay.getDayNumber(), selectedShift);
                        } else {
                            // Selected rest day
                            updatedDay = new PatternDay(currentDay.getDayNumber(), null);
                        }

                        mPatternDays.set(position, updatedDay);
                        mPatternDaysAdapter.notifyItemChanged(position);
                        updatePatternStatusDisplay();

                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }
    }

    private void clearPattern() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_clear_pattern_title)
                .setMessage(R.string.dialog_clear_pattern_message)
                .setPositiveButton(R.string.action_clear, (dialog, which) -> {
                    mPatternDays.clear();
                    mPatternDaysAdapter.notifyDataSetChanged();
                    updatePatternStatusDisplay();
                    showSuccess(getString(R.string.message_pattern_cleared));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void savePattern() {
        // Validate input
        if (mSelectedStartDate == null) {
            showError(getString(R.string.validation_start_date_required));
            return;
        }

        if (mPatternDays.isEmpty()) {
            showError(getString(R.string.validation_pattern_empty));
            return;
        }

        // Check if start date is not in the past
        if (mSelectedStartDate.isBefore(LocalDate.now())) {
            showError(getString(R.string.validation_start_date_past));
            return;
        }

        showLoading(true);

        // Get pattern service from DI module
        SchedulePatternModule module = new SchedulePatternModule(this, getServiceProvider());
        UserSchedulePatternService patternService = module.getUserSchedulePatternService();

        // Create pattern
        CompletableFuture<OperationResult<UserScheduleAssignment>> createFuture;

        if (mIsEditMode && mEditingAssignmentId != null) {
            // Update existing pattern
            String patternName = generatePatternNameFromCurrent();
            createFuture = patternService.updateUserPattern(
                    mEditingAssignmentId, mPatternDays, mSelectedStartDate, patternName);
        } else {
            // Create new pattern
            createFuture = patternService.createUserPattern(mPatternDays, mSelectedStartDate);
        }

        createFuture
                .thenAccept(result -> runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        showSuccess(getString(R.string.message_pattern_saved));
                        Log.d(TAG, "Pattern saved successfully: " + result.getData().getId());

                        // Return to previous screen
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showError(getString(R.string.error_saving_pattern) + ": " + result.getErrorMessage());
                    }
                }))
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Log.e(TAG, "Error saving pattern", throwable);
                        showError(getString(R.string.error_saving_pattern));
                    });
                    return null;
                });
    }

    private void previewPattern() {
        if (mPatternDays.isEmpty() || mSelectedStartDate == null) {
            showError(getString(R.string.error_empty_pattern));
            return;
        }

        showLoading(true);

        // Get pattern service from DI module
        SchedulePatternModule module = new SchedulePatternModule(this, getServiceProvider());
        UserSchedulePatternService patternService = module.getUserSchedulePatternService();

        // Generate preview for next 14 days
        patternService.generatePatternPreview(mPatternDays, mSelectedStartDate, 14)
                .thenAccept(result -> runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        showPatternPreviewDialog(result.getData());
                    } else {
                        showError("Preview error: " + result.getErrorMessage());
                    }
                }))
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Log.e(TAG, "Error generating preview", throwable);
                        showError("Error generating preview");
                    });
                    return null;
                });
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_help_title)
                .setMessage(R.string.dialog_help_schedule_pattern_message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }
}