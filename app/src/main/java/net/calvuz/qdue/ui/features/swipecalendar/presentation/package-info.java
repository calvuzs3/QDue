/**
* SwipeCalendar Integration Guide and Usage Examples
*
* This document provides comprehensive examples for integrating the SwipeCalendarFragment
* into existing QDue activities and implementing proper dependency injection.
  */

/**
* ==================== BASIC INTEGRATION EXAMPLE ====================
*
* Example Activity that hosts SwipeCalendarFragment with proper DI setup:
  */

/*
package net.calvuz.qdue.ui.features.swipecalendar.presentation;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.R;
import net.calvuz.qdue.core.infrastructure.di.Injectable;
import net.calvuz.qdue.core.infrastructure.di.ServiceProvider;
import net.calvuz.qdue.core.infrastructure.di.ServiceProviderImpl;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;

public class SwipeCalendarActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = "SwipeCalendarActivity";
    
    // Arguments for launching activity
    public static final String EXTRA_INITIAL_DATE = "initial_date";
    public static final String EXTRA_USER_ID = "user_id";
    
    private ServiceProvider mServiceProvider;
    private SwipeCalendarFragment mCalendarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_calendar);
        
        // Initialize dependency injection
        initializeDependencyInjection();
        
        // Setup fragment
        setupCalendarFragment(savedInstanceState);
        
        // Configure toolbar
        setupToolbar();
    }

    private void initializeDependencyInjection() {
        mServiceProvider = ServiceProviderImpl.getInstance(getApplicationContext());
        Log.d(TAG, "Dependency injection initialized");
    }

    private void setupCalendarFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Parse intent extras
            LocalDate initialDate = null;
            Long userId = null;
            
            String dateStr = getIntent().getStringExtra(EXTRA_INITIAL_DATE);
            if (dateStr != null) {
                try {
                    initialDate = LocalDate.parse(dateStr);
                } catch (Exception e) {
                    Log.w(TAG, "Invalid initial date: " + dateStr);
                }
            }
            
            if (getIntent().hasExtra(EXTRA_USER_ID)) {
                userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
                if (userId == -1) userId = null;
            }
            
            // Create fragment
            mCalendarFragment = SwipeCalendarFragment.newInstance(initialDate, userId);
            
            // Add to container
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.calendar_container, mCalendarFragment)
                    .commit();
                    
            Log.d(TAG, "SwipeCalendarFragment added to activity");
        } else {
            // Restore fragment reference
            mCalendarFragment = (SwipeCalendarFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.calendar_container);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.calendar_swipe_view_title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @NonNull
    public ServiceProvider getServiceProvider() {
        return mServiceProvider;
    }

    // Public methods for external navigation
    public void navigateToDate(@NonNull LocalDate date) {
        if (mCalendarFragment != null) {
            mCalendarFragment.navigateToMonth(YearMonth.from(date), true);
        }
    }
}
*/

/**
* ==================== FRAGMENT CONTAINER LAYOUT ====================
  */

/*
<?xml version="1.0" encoding="utf-8"?>
<!-- res/layout/activity_swipe_calendar.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:orientation="vertical">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:theme="@style/ThemeOverlay.Material3.Dark.ActionBar"
        app:popupTheme="@style/ThemeOverlay.Material3.Light" />

    <!-- Fragment Container -->
    <FrameLayout
        android:id="@+id/calendar_container"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
*/

/**
* ==================== INTEGRATION WITH EXISTING NAVIGATION ====================
*
* Example of integrating SwipeCalendarFragment into existing MainActivity:
  */

/*
// In MainActivity.java - add to navigation menu

private void setupBottomNavigation() {
BottomNavigationView navigation = findViewById(R.id.bottom_navigation);

    navigation.setOnNavigationItemSelectedListener(item -> {
        Fragment selectedFragment = null;
        
        switch (item.getItemId()) {
            case R.id.nav_calendar_infinite:
                selectedFragment = CalendarViewFragment.newInstance();
                break;
                
            case R.id.nav_calendar_swipe:
                // New swipe calendar option
                selectedFragment = SwipeCalendarFragment.newInstance(null, getCurrentUserId());
                break;
                
            case R.id.nav_dayslist:
                selectedFragment = DayslistViewFragment.newInstance();
                break;
                
            case R.id.nav_events:
                selectedFragment = EventsFragment.newInstance();
                break;
        }
        
        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_container, selectedFragment)
                    .commit();
            return true;
        }
        
        return false;
    });
}
*/

/**
* ==================== PROGRAMMATIC NAVIGATION EXAMPLES ====================
  */

/*
// Navigate to specific date from external code
public void openCalendarAtDate(LocalDate targetDate) {
SwipeCalendarFragment fragment = SwipeCalendarFragment.newInstance(targetDate, getCurrentUserId());

    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack("calendar")
            .commit();
}

// Navigate to today with animation
public void openCalendarToday() {
SwipeCalendarFragment fragment = SwipeCalendarFragment.newInstance(null, getCurrentUserId());

    getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.container, fragment)
            .addToBackStack("calendar")
            .commit();
}

// Navigate from event detail to calendar
public void openCalendarForEvent(LocalEvent event) {
LocalDate eventDate = event.getDate();
openCalendarAtDate(eventDate);
}
*/

/**
* ==================== PREFERENCE INTEGRATION ====================
*
* Add to settings to allow users to choose between calendar views:
  */

/*
// In SettingsFragment.java

private void setupCalendarViewPreference() {
ListPreference calendarViewPref = findPreference("calendar_view_type");

    if (calendarViewPref != null) {
        calendarViewPref.setEntries(new CharSequence[]{
                "Vista calendario infinita",
                "Vista calendario mensile",
                "Vista lista giorni"
        });
        
        calendarViewPref.setEntryValues(new CharSequence[]{
                "infinite",
                "swipe",
                "dayslist"
        });
        
        calendarViewPref.setOnPreferenceChangeListener((preference, newValue) -> {
            String viewType = (String) newValue;
            
            // Save preference and update UI
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            prefs.edit().putString("default_calendar_view", viewType).apply();
            
            // Show restart hint if needed
            if (getActivity() instanceof QDueMainActivity) {
                ((QDueMainActivity) getActivity()).refreshCalendarView();
            }
            
            return true;
        });
    }
}
*/

/**
* ==================== EVENT HANDLING EXAMPLES ====================
  */

/*
// Custom event listener implementation
public class CalendarEventHandler implements SwipeCalendarFragment.OnCalendarInteractionListener {

    private final Context mContext;
    private final FragmentManager mFragmentManager;
    
    public CalendarEventHandler(Context context, FragmentManager fragmentManager) {
        this.mContext = context;
        this.mFragmentManager = fragmentManager;
    }
    
    @Override
    public void onDayClick(LocalDate date, Day day, List<LocalEvent> events) {
        if (events.isEmpty()) {
            // No events - show quick event creation
            QuickEventDialog dialog = QuickEventDialog.newInstance(date);
            dialog.show(mFragmentManager, "quick_event");
        } else {
            // Has events - show events for day
            EventsForDayDialog dialog = EventsForDayDialog.newInstance(date, events);
            dialog.show(mFragmentManager, "events_for_day");
        }
    }
    
    @Override
    public void onDayLongClick(LocalDate date, Day day, View view) {
        // Show context menu with options
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.getMenuInflater().inflate(R.menu.calendar_day_context_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_create_event:
                    openEventCreation(date);
                    return true;
                case R.id.action_view_day_detail:
                    openDayDetail(date);
                    return true;
                case R.id.action_copy_date:
                    copyDateToClipboard(date);
                    return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void openEventCreation(LocalDate date) {
        Intent intent = new Intent(mContext, EventsActivity.class);
        intent.putExtra(EventsActivity.EXTRA_CREATE_EVENT_DATE, date.toString());
        mContext.startActivity(intent);
    }
    
    private void openDayDetail(LocalDate date) {
        DayDetailDialog dialog = DayDetailDialog.newInstance(date);
        dialog.show(mFragmentManager, "day_detail");
    }
    
    private void copyDateToClipboard(LocalDate date) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Date", date.toString());
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(mContext, "Data copiata negli appunti", Toast.LENGTH_SHORT).show();
    }
}
*/

/**
* ==================== STATE MANAGEMENT EXAMPLES ====================
  */

/*
// Custom state management for complex scenarios
public class CalendarStateController {

    private final SwipeCalendarStateManager mStateManager;
    private final SharedPreferences mPreferences;
    
    public CalendarStateController(Context context) {
        this.mStateManager = new SwipeCalendarStateManager(context);
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    // Save additional state beyond just position
    public void saveExtendedState(YearMonth currentMonth, String viewMode, boolean isSelectionMode) {
        mPreferences.edit()
                .putString("calendar_current_month", currentMonth.toString())
                .putString("calendar_view_mode", viewMode)
                .putBoolean("calendar_selection_mode", isSelectionMode)
                .apply();
    }
    
    // Restore extended state
    public CalendarState restoreExtendedState() {
        String monthStr = mPreferences.getString("calendar_current_month", null);
        String viewMode = mPreferences.getString("calendar_view_mode", "normal");
        boolean isSelectionMode = mPreferences.getBoolean("calendar_selection_mode", false);
        
        YearMonth month = null;
        if (monthStr != null) {
            try {
                month = YearMonth.parse(monthStr);
            } catch (Exception e) {
                month = YearMonth.now();
            }
        }
        
        return new CalendarState(month, viewMode, isSelectionMode);
    }
    
    // Clear all state
    public void resetState() {
        mStateManager.clearState();
        mPreferences.edit()
                .remove("calendar_current_month")
                .remove("calendar_view_mode")
                .remove("calendar_selection_mode")
                .apply();
    }
    
    public static class CalendarState {
        public final YearMonth month;
        public final String viewMode;
        public final boolean isSelectionMode;
        
        public CalendarState(YearMonth month, String viewMode, boolean isSelectionMode) {
            this.month = month;
            this.viewMode = viewMode;
            this.isSelectionMode = isSelectionMode;
        }
    }
}
*/

/**
* ==================== TESTING EXAMPLES ====================
  */

/*
// Unit test example for SwipeCalendarModule
@Test
public void testSwipeCalendarModuleDependencyInjection() {
// Arrange
Context context = ApplicationProvider.getApplicationContext();
EventsService mockEventsService = mock(EventsService.class);
UserService mockUserService = mock(UserService.class);
WorkScheduleService mockWorkScheduleService = mock(WorkScheduleService.class);

    // Act
    SwipeCalendarModule module = new SwipeCalendarModule(
            context, mockEventsService, mockUserService, mockWorkScheduleService);
    
    // Assert
    assertTrue(module.areDependenciesReady());
    assertNotNull(module.provideStateManager());
    assertNotNull(module.providePagerAdapter());
}

// Instrumentation test example for fragment
@Test
public void testSwipeCalendarFragmentCreation() {
// Arrange
LocalDate testDate = LocalDate.of(2024, 1, 15);
Long testUserId = 123L;

    // Act
    SwipeCalendarFragment fragment = SwipeCalendarFragment.newInstance(testDate, testUserId);
    
    // Assert
    assertNotNull(fragment);
    Bundle args = fragment.getArguments();
    assertNotNull(args);
    assertEquals(testDate.toString(), args.getString(SwipeCalendarFragment.ARG_INITIAL_DATE));
    assertEquals(testUserId.longValue(), args.getLong(SwipeCalendarFragment.ARG_USER_ID));
}
*/

/**
* ==================== IMPLEMENTATION NOTES ====================
*
* Key Points for Successful Integration:
*
* 1. DEPENDENCY INJECTION:
*    - Always ensure hosting Activity implements Injectable
*    - Use ServiceProvider pattern consistently
*    - Initialize modules in onCreate() before fragment setup
*
* 2. STATE MANAGEMENT:
*    - Handle onPause() for proper state persistence
*    - Clear state appropriately in onDestroy()
*    - Consider user preferences for initial position
*
* 3. PERFORMANCE:
*    - ViewPager2 manages memory automatically
*    - Data loading is asynchronous and cached
*    - Use proper lifecycle management to prevent leaks
*
* 4. ACCESSIBILITY:
*    - All UI components have proper content descriptions
*    - Touch targets meet minimum size requirements
*    - Support for high contrast and large text
*
* 5. ERROR HANDLING:
*    - Graceful degradation on data loading failures
*    - User-friendly error messages with retry options
*    - Proper logging for debugging
*
* 6. INTEGRATION WITH EXISTING FEATURES:
*    - Reuses existing event and work schedule services
*    - Compatible with existing EventsActivity for detail views
*    - Follows established UI patterns and Material Design
       */

package net.calvuz.qdue.ui.features.swipecalendar.presentation;

