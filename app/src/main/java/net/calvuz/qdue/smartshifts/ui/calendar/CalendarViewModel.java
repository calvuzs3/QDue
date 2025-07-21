package net.calvuz.qdue.smartshifts.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import net.calvuz.qdue.smartshifts.data.entities.SmartShiftEvent;
import net.calvuz.qdue.smartshifts.data.entities.UserShiftAssignment;
import net.calvuz.qdue.smartshifts.data.entities.ShiftType;
import net.calvuz.qdue.smartshifts.domain.usecases.GetUserShiftsUseCase;
import net.calvuz.qdue.smartshifts.ui.calendar.models.CalendarDay;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for calendar fragment
 * Manages calendar data and user interactions
 */
@HiltViewModel
public class CalendarViewModel extends ViewModel {

    private final GetUserShiftsUseCase getUserShiftsUseCase;

    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CalendarDay>> calendarDays = new MutableLiveData<>();

    // Current user ID - in real app would come from authentication
    private static final String CURRENT_USER_ID = "user_default";

    @Inject
    public CalendarViewModel(GetUserShiftsUseCase getUserShiftsUseCase) {
        this.getUserShiftsUseCase = getUserShiftsUseCase;
    }

    /**
     * Load calendar data for specific month
     */
    public void loadMonth(YearMonth yearMonth) {
        currentMonth.setValue(yearMonth);
        isLoading.setValue(true);

        // Get shifts for the month
        LiveData<List<SmartShiftEvent>> monthShifts = getUserShiftsUseCase.getShiftsForMonth(CURRENT_USER_ID, yearMonth);

        // Transform shifts to calendar days
        LiveData<List<CalendarDay>> calendarDaysLive = Transformations.map(monthShifts, shifts -> {
            isLoading.setValue(false);
            return generateCalendarDays(yearMonth, shifts);
        });

        // Observe the transformed data
        calendarDaysLive.observeForever(days -> {
            calendarDays.setValue(days);
        });
    }

    /**
     * Generate calendar days for month with shift data
     */
    private List<CalendarDay> generateCalendarDays(YearMonth yearMonth, List<SmartShiftEvent> shifts) {
        List<CalendarDay> days = new ArrayList<>();

        // Create map of shifts by date for quick lookup
        Map<String, SmartShiftEvent> shiftsByDate = shifts.stream()
                .collect(Collectors.toMap(
                        SmartShiftEvent::getEventDate,
                        shift -> shift,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));

        // Calculate calendar grid (42 days: 6 weeks * 7 days)
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate startDate = firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeek().getValue() - 1);

        for (int i = 0; i < 42; i++) {
            LocalDate date = startDate.plusDays(i);
            boolean isCurrentMonth = date.getMonth() == yearMonth.getMonth();
            boolean isToday = date.equals(LocalDate.now());

            SmartShiftEvent shift = shiftsByDate.get(date.toString());

            CalendarDay calendarDay = new CalendarDay(
                    date,
                    date.getDayOfMonth(),
                    isCurrentMonth,
                    isToday,
                    shift
            );

            days.add(calendarDay);
        }

        return days;
    }

    /**
     * Select specific date
     */
    public void selectDate(LocalDate date) {
        selectedDate.setValue(date);
    }

    /**
     * Get user's active assignment
     */
    public LiveData<UserShiftAssignment> getUserAssignment() {
        return getUserShiftsUseCase.getUserActiveAssignment(CURRENT_USER_ID);
    }

    /**
     * Get available shift types for legend
     */
    public LiveData<List<ShiftType>> getShiftTypes() {
        // This would be implemented with a use case
        // For now return empty list
        return new MutableLiveData<>(new ArrayList<>());
    }

    // Getters for LiveData
    public LiveData<List<CalendarDay>> getCalendarDays() {
        return calendarDays;
    }

    public LiveData<LocalDate> getSelectedDate() {
        return selectedDate;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
}
