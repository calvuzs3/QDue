package net.calvuz.qdue.ui.features.calendar.presentation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.domain.events.models.LocalEvent;
import net.calvuz.qdue.core.domain.quattrodue.models.WorkScheduleEvent;
import net.calvuz.qdue.core.infrastructure.di.Injectable;
import net.calvuz.qdue.core.infrastructure.di.ServiceProvider;
import net.calvuz.qdue.core.infrastructure.services.EventsService;
import net.calvuz.qdue.core.infrastructure.services.UserService;
import net.calvuz.qdue.core.infrastructure.services.WorkScheduleService;
import net.calvuz.qdue.ui.core.architecture.base.BaseInteractiveFragment;
import net.calvuz.qdue.ui.features.calendar.adapters.CalendarPagerAdapter;
import net.calvuz.qdue.ui.features.calendar.di.CalendarModule;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarDataLoadingListener;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarEventListener;
import net.calvuz.qdue.ui.features.calendar.interfaces.CalendarNavigationListener;
import net.calvuz.qdue.ui.features.calendar.models.CalendarDay;
import net.calvuz.qdue.ui.features.calendar.models.CalendarLoadingState;
import net.calvuz.qdue.ui.features.calendar.models.CalendarViewMode;
import net.calvuz.qdue.ui.core.common.interfaces.BackPressHandler;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * CalendarViewFragment - Main fragment for calendar view functionality.
 *
 * <p>Provides Google Calendar-like interface with month navigation using ViewPager2.
 * Integrates with work schedule system and events to show unified calendar view.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Monthly calendar grid with smooth swipe navigation</li>
 *   <li>Work schedule integration with shift color coding</li>
 *   <li>Event overlay indicators with EventType icons</li>
 *   <li>Loading states and skeleton UI</li>
 *   <li>Quick event creation and navigation</li>
 * </ul>
 *
 * @author Calendar App Team
 * @version 1.0
 * @since Database Version 6
 */
public class CalendarViewFragment extends BaseInteractiveFragment implements
        CalendarEventListener, CalendarNavigationListener, CalendarDataLoadingListener,
        BackPressHandler {

    private static final String TAG = "CalendarViewFragment";

    // Arguments
    public static final String ARG_INITIAL_DATE = "initial_date";
    public static final String ARG_VIEW_MODE = "view_mode";
    public static final String ARG_USER_ID = "user_id";

    // ViewPager2 configuration
    private static final int MONTHS_BUFFER = 12; // Months to keep in memory
    private static final int INITIAL_POSITION = 1000; // Center position for infinite scroll

    // ==================== DEPENDENCIES ====================

    private CalendarModule mCalendarModule;
    private EventsService mEventsService;
    private UserService mUserService;
    private WorkScheduleService mWorkScheduleService;

    // ==================== UI COMPONENTS ====================

    // Main layout
    private View mRootView;
    private MaterialCardView mCalendarCard;

    // Header
    private TextView mMonthYearText;
    private MaterialButton mPreviousMonthButton;
    private MaterialButton mNextMonthButton;
    private MaterialButton mTodayButton;

    // Week days header
    private ViewGroup mWeekDaysHeader;

    // Calendar content
    private ViewPager2 mCalendarViewPager;
    private CalendarPagerAdapter mCalendarPagerAdapter;

    // Loading UI
    private LinearProgressIndicator mLoadingIndicator;
    private View mLoadingSkeletonView;
    private TextView mErrorText;
    private MaterialButton mRetryButton;

    // ==================== STATE VARIABLES ====================

    private LocalDate mInitialDate;
    private CalendarViewMode mViewMode = CalendarViewMode.MONTH;
    private Long mUserId;

    private YearMonth mCurrentMonth;
    private CalendarLoadingState mLoadingState = CalendarLoadingState.IDLE;

    private boolean mIsInitialized = false;
    private boolean mIsDestroyed = false;

    // Formatting
    private final DateTimeFormatter mMonthYearFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());

    // ==================== LIFECYCLE METHODS ====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Creating CalendarViewFragment");

        // Extract arguments
        extractArguments();

        // Initialize current month
        mCurrentMonth = mInitialDate.getMonth().atYear(mInitialDate.getYear());

        Log.d(TAG, "Initial configuration: date=" + mInitialDate +
                ", viewMode=" + mViewMode + ", userId=" + mUserId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_calendar_view, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "View created, initializing components");

        // Initialize UI components
        initializeViews(view);

        // Get calendar module from activity
        initializeCalendarModule();

        // Setup UI
        setupWeekDaysHeader();
        setupCalendarViewPager();
        setupClickListeners();

        // Load initial data
        loadInitialData();

        mIsInitialized = true;
        Log.d(TAG, "CalendarViewFragment initialization completed");
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "Destroying CalendarViewFragment view");

        mIsDestroyed = true;

        // Cleanup ViewPager2
        if (mCalendarViewPager != null) {
            mCalendarViewPager.setAdapter(null);
        }

        super.onDestroyView();
    }

    // ==================== DEPENDENCY INJECTION ====================

    @Override
    public void inject(ServiceProvider serviceProvider) {
        super.inject(serviceProvider);

        mEventsService = serviceProvider.getEventsService();
        mUserService = serviceProvider.getUserService();
        mWorkScheduleService = serviceProvider.getWorkScheduleService();

        Log.d(TAG, "Calendar services injected");
    }

    @Override
    public boolean areDependenciesReady() {
        return super.areDependenciesReady() &&
                mEventsService != null &&
                mUserService != null &&
                mWorkScheduleService != null;
    }

    // ==================== INITIALIZATION METHODS ====================

    /**
     * Extract arguments from bundle.
     */
    private void extractArguments() {
        Bundle args = getArguments();
        if (args == null) {
            mInitialDate = LocalDate.now();
            return;
        }

        // Initial date
        String dateStr = args.getString(ARG_INITIAL_DATE);
        if (dateStr != null) {
            try {
                mInitialDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                Log.w(TAG, "Invalid initial date: " + dateStr);
                mInitialDate = LocalDate.now();
            }
        } else {
            mInitialDate = LocalDate.now();
        }

        // View mode
        String viewModeStr = args.getString(ARG_VIEW_MODE);
        if (viewModeStr != null) {
            mViewMode = CalendarViewMode.fromString(viewModeStr);
        }

        // User ID
        if (args.containsKey(ARG_USER_ID)) {
            mUserId = args.getLong(ARG_USER_ID);
        }
    }

    /**
     * Initialize UI views.
     */
    private void initializeViews(@NonNull View view) {
        // Main layout
        mCalendarCard = view.findViewById(R.id.calendar_card);

        // Header
        mMonthYearText = view.findViewById(R.id.month_year_text);
        mPreviousMonthButton = view.findViewById(R.id.previous_month_button);
        mNextMonthButton = view.findViewById(R.id.next_month_button);
        mTodayButton = view.findViewById(R.id.today_button);

        // Week days header
        mWeekDaysHeader = view.findViewById(R.id.week_days_header);

        // Calendar content
        mCalendarViewPager = view.findViewById(R.id.calendar_view_pager);

        // Loading UI
        mLoadingIndicator = view.findViewById(R.id.loading_indicator);
        mLoadingSkeletonView = view.findViewById(R.id.loading_skeleton);
        mErrorText = view.findViewById(R.id.error_text);
        mRetryButton = view.findViewById(R.id.retry_button);

        Log.d(TAG, "Views initialized");
    }

    /**
     * Get calendar module from parent activity.
     */
    private void initializeCalendarModule() {
        if (getActivity() instanceof CalendarActivity) {
            CalendarActivity activity = (CalendarActivity) getActivity();
            mCalendarModule = activity.getCalendarModule();

            if (mCalendarModule != null) {
                // Configure module for this fragment
                mCalendarModule.setCurrentUser(mUserId);
                Log.d(TAG, "Calendar module obtained from activity");
            } else {
                Log.e(TAG, "Calendar module not available from activity");
            }
        } else {
            Log.e(TAG, "Parent activity is not CalendarActivity");
        }
    }

    /**
     * Setup week days header (Mon, Tue, Wed, etc.).
     */
    private void setupWeekDaysHeader() {
        if (mWeekDaysHeader == null) return;

        mWeekDaysHeader.removeAllViews();

        // Get weekday names in current locale
        String[] weekDays = getWeekDayNames();

        for (String dayName : weekDays) {
            TextView dayView = new TextView(requireContext());
            dayView.setText(dayName);
            dayView.setGravity(android.view.Gravity.CENTER);
            dayView.setLayoutParams(new ViewGroup.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT));
            ((ViewGroup.LayoutParams) dayView.getLayoutParams()).width = 0;
            if (dayView.getLayoutParams() instanceof android.widget.LinearLayout.LayoutParams) {
                ((android.widget.LinearLayout.LayoutParams) dayView.getLayoutParams()).weight = 1;
            }

            mWeekDaysHeader.addView(dayView);
        }

        Log.d(TAG, "Week days header setup completed");
    }

    /**
     * Setup ViewPager2 for month navigation.
     */
    private void setupCalendarViewPager() {
        if (mCalendarViewPager == null || mCalendarModule == null) {
            Log.e(TAG, "Cannot setup ViewPager2 - missing components");
            return;
        }

        // Create adapter
        mCalendarPagerAdapter = mCalendarModule.provideCalendarPagerAdapter(this, this);

        // Configure ViewPager2
        mCalendarViewPager.setAdapter(mCalendarPagerAdapter);
        mCalendarViewPager.setOffscreenPageLimit(3); // Keep 3 months in memory

        // Set initial position to current month
        int initialPosition = calculatePositionForMonth(mCurrentMonth);
        mCalendarViewPager.setCurrentItem(initialPosition, false);

        // Setup page change listener
        mCalendarViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                YearMonth newMonth = calculateMonthForPosition(position);
                if (!newMonth.equals(mCurrentMonth)) {
                    mCurrentMonth = newMonth;
                    updateMonthYearDisplay();
                    onMonthChanged(newMonth);
                }
            }
        });

        Log.d(TAG, "ViewPager2 setup completed");
    }

    /**
     * Setup click listeners for navigation buttons.
     */
    private void setupClickListeners() {
        if (mPreviousMonthButton != null) {
            mPreviousMonthButton.setOnClickListener(v -> navigateToPreviousMonth());
        }

        if (mNextMonthButton != null) {
            mNextMonthButton.setOnClickListener(v -> navigateToNextMonth());
        }

        if (mTodayButton != null) {
            mTodayButton.setOnClickListener(v -> navigateToToday());
        }

        if (mRetryButton != null) {
            mRetryButton.setOnClickListener(v -> retryLoadData());
        }

        Log.d(TAG, "Click listeners setup completed");
    }

    /**
     * Load initial calendar data.
     */
    private void loadInitialData() {
        updateMonthYearDisplay();
        setLoadingState(CalendarLoadingState.LOADING);

        if (mCalendarModule != null) {
            // Trigger data loading for current month
            mCalendarModule.refreshCalendarData(mCurrentMonth);
        }
    }

    // ==================== NAVIGATION METHODS ====================

    /**
     * Navigate to previous month.
     */
    private void navigateToPreviousMonth() {
        if (mCalendarViewPager != null) {
            int currentItem = mCalendarViewPager.getCurrentItem();
            mCalendarViewPager.setCurrentItem(currentItem - 1, true);
        }
    }

    /**
     * Navigate to next month.
     */
    private void navigateToNextMonth() {
        if (mCalendarViewPager != null) {
            int currentItem = mCalendarViewPager.getCurrentItem();
            mCalendarViewPager.setCurrentItem(currentItem + 1, true);
        }
    }

    /**
     * Navigate to today's month.
     */
    private void navigateToToday() {
        YearMonth todayMonth = YearMonth.now();
        navigateToMonth(todayMonth);
    }

    /**
     * Navigate to specific month.
     */
    private void navigateToMonth(@NonNull YearMonth targetMonth) {
        if (mCalendarViewPager != null) {
            int position = calculatePositionForMonth(targetMonth);
            mCalendarViewPager.setCurrentItem(position, true);
        }
    }

    // ==================== CALENDAR EVENT LISTENER ====================

    @Override
    public void onDayClick(@NonNull LocalDate date, @NonNull CalendarDay calendarDay) {
        Log.d(TAG, "Day clicked: " + date);

        // Handle day click - could show day details, create event, etc.
        showDayDetails(date, calendarDay);
    }

    @Override
    public void onDayLongClick(@NonNull LocalDate date, @NonNull CalendarDay calendarDay) {
        Log.d(TAG, "Day long clicked: " + date);

        // Handle long click - show context menu or quick actions
        showQuickActions(date, calendarDay);
    }

    @Override
    public void onEventClick(@NonNull LocalEvent event, @NonNull LocalDate date) {
        Log.d(TAG, "Event clicked: " + event.getTitle() + " on " + date);

        // Handle event click - open event details
        showEventDetails(event, date);
    }

    @Override
    public void onCreateEvent(@NonNull LocalDate date) {
        Log.d(TAG, "Create event requested for: " + date);

        // Handle create event request
        createNewEvent(date);
    }

    @Override
    public void onShiftDetailsRequested(@NonNull LocalDate date, @NonNull List<WorkScheduleEvent> workScheduleEvents) {
        Log.d(TAG, "Shift details requested for: " + date);

        // Handle shift details request
        showShiftDetails(date, workScheduleEvents);
    }

    // ==================== CALENDAR NAVIGATION LISTENER ====================

    @Override
    public void onMonthChanged(@NonNull YearMonth yearMonth) {
        Log.d(TAG, "Month changed to: " + yearMonth);

        mCurrentMonth = yearMonth;
        updateMonthYearDisplay();

        // Preload data for adjacent months
        preloadAdjacentMonths();
    }

    @Override
    public void onViewModeChangeRequested(@NonNull String viewMode) {
        Log.d(TAG, "View mode change requested: " + viewMode);

        // Handle view mode change
        CalendarViewMode newMode = CalendarViewMode.fromString(viewMode);
        if (newMode != mViewMode) {
            mViewMode = newMode;
            // Could switch to different fragments or reconfigure current view
        }
    }

    @Override
    public void onTodayRequested() {
        navigateToToday();
    }

    @Override
    public void onDateNavigationRequested(@NonNull LocalDate date) {
        YearMonth targetMonth = YearMonth.of(date.getYear(), date.getMonth());
        navigateToMonth(targetMonth);
    }

    // ==================== CALENDAR DATA LOADING LISTENER ====================

    @Override
    public void onLoadingStarted(@NonNull YearMonth yearMonth) {
        if (yearMonth.equals(mCurrentMonth)) {
            setLoadingState(CalendarLoadingState.LOADING);
        }
    }

    @Override
    public void onLoadingCompleted(@NonNull YearMonth yearMonth, @NonNull List<CalendarDay> calendarDays) {
        if (yearMonth.equals(mCurrentMonth)) {
            setLoadingState(CalendarLoadingState.LOADED);
        }

        // Notify adapter of data change
        if (mCalendarPagerAdapter != null) {
            mCalendarPagerAdapter.notifyMonthDataChanged(yearMonth);
        }
    }

    @Override
    public void onLoadingFailed(@NonNull YearMonth yearMonth, @NonNull Throwable error) {
        Log.e(TAG, "Loading failed for " + yearMonth, error);

        if (yearMonth.equals(mCurrentMonth)) {
            setLoadingState(CalendarLoadingState.ERROR);
        }
    }

    @Override
    public void onLoadingProgress(@NonNull YearMonth yearMonth, int progress) {
        if (yearMonth.equals(mCurrentMonth)) {
            updateLoadingProgress(progress);
        }
    }

    // ==================== UI UPDATE METHODS ====================

    /**
     * Update month/year display in header.
     */
    private void updateMonthYearDisplay() {
        if (mMonthYearText != null) {
            String monthYearText = mCurrentMonth.format(mMonthYearFormatter);
            mMonthYearText.setText(monthYearText);
        }
    }

    /**
     * Set loading state and update UI accordingly.
     */
    private void setLoadingState(@NonNull CalendarLoadingState state) {
        mLoadingState = state;

        if (mIsDestroyed) return;

        // Update on main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            updateLoadingUI(state);
        });
    }

    /**
     * Update loading UI based on state.
     */
    private void updateLoadingUI(@NonNull CalendarLoadingState state) {
        if (mLoadingIndicator == null) return;

        switch (state) {
            case LOADING:
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mLoadingSkeletonView.setVisibility(View.VISIBLE);
                mErrorText.setVisibility(View.GONE);
                mRetryButton.setVisibility(View.GONE);
                break;

            case LOADED:
                mLoadingIndicator.setVisibility(View.GONE);
                mLoadingSkeletonView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                mRetryButton.setVisibility(View.GONE);
                break;

            case ERROR:
                mLoadingIndicator.setVisibility(View.GONE);
                mLoadingSkeletonView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
                break;

            case REFRESHING:
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mLoadingSkeletonView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                mRetryButton.setVisibility(View.GONE);
                break;

            default:
                mLoadingIndicator.setVisibility(View.GONE);
                mLoadingSkeletonView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                mRetryButton.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Update loading progress.
     */
    private void updateLoadingProgress(int progress) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setProgress(progress);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get weekday names for header.
     */
    private String[] getWeekDayNames() {
        String[] names = new String[7];
        for (int i = 0; i < 7; i++) {
            // Monday = 1, Sunday = 7
            int dayOfWeek = (i + 1) % 7 + 1;
            if (dayOfWeek == 8) dayOfWeek = 1; // Adjust for Sunday
            names[i] = java.time.DayOfWeek.of(dayOfWeek)
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault());
        }
        return names;
    }

    /**
     * Calculate ViewPager2 position for given month.
     */
    private int calculatePositionForMonth(@NonNull YearMonth month) {
        YearMonth baseMonth = YearMonth.of(2020, 1); // Arbitrary base
        long monthsBetween = baseMonth.until(month, java.time.temporal.ChronoUnit.MONTHS);
        return INITIAL_POSITION + (int) monthsBetween;
    }

    /**
     * Calculate month for ViewPager2 position.
     */
    private YearMonth calculateMonthForPosition(int position) {
        YearMonth baseMonth = YearMonth.of(2020, 1); // Same base as above
        long offset = position - INITIAL_POSITION;
        return baseMonth.plusMonths(offset);
    }

    /**
     * Preload data for adjacent months.
     */
    private void preloadAdjacentMonths() {
        if (mCalendarModule != null) {
            YearMonth previousMonth = mCurrentMonth.minusMonths(1);
            YearMonth nextMonth = mCurrentMonth.plusMonths(1);

            mCalendarModule.refreshCalendarData(previousMonth);
            mCalendarModule.refreshCalendarData(nextMonth);
        }
    }

    /**
     * Retry loading data.
     */
    private void retryLoadData() {
        if (mCalendarModule != null) {
            setLoadingState(CalendarLoadingState.LOADING);
            mCalendarModule.refreshCalendarData(mCurrentMonth);
        }
    }

    // ==================== ACTION HANDLERS ====================

    /**
     * Show day details.
     */
    private void showDayDetails(@NonNull LocalDate date, @NonNull CalendarDay calendarDay) {
        // TODO: Implement day details dialog or navigation
        Log.d(TAG, "Showing day details for " + date);
    }

    /**
     * Show quick actions for a day.
     */
    private void showQuickActions(@NonNull LocalDate date, @NonNull CalendarDay calendarDay) {
        // TODO: Implement quick actions bottom sheet
        Log.d(TAG, "Showing quick actions for " + date);
    }

    /**
     * Show event details.
     */
    private void showEventDetails(@NonNull LocalEvent event, @NonNull LocalDate date) {
        // TODO: Navigate to event details screen
        Log.d(TAG, "Showing event details for " + event.getTitle());
    }

    /**
     * Create new event.
     */
    private void createNewEvent(@NonNull LocalDate date) {
        // TODO: Navigate to event creation screen
        Log.d(TAG, "Creating new event for " + date);
    }

    /**
     * Show shift details.
     */
    private void showShiftDetails(@NonNull LocalDate date, @NonNull List<WorkScheduleEvent> events) {
        // TODO: Show shift details dialog
        Log.d(TAG, "Showing shift details for " + date);
    }

    // ==================== BACK PRESS HANDLER ====================

    @Override
    public boolean onBackPressed() {
        // Handle back press - could exit selection mode, close dialogs, etc.
        return false; // Let activity handle it
    }
}