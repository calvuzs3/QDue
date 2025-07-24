/**
 * Calendar-specific validators with i18n support
 * Validates actions based on work schedules and business rules
 */
package net.calvuz.qdue.ui.core.components.selection.validation;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.repository.WorkScheduleRepository;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionValidationResult;
import net.calvuz.qdue.ui.core.components.selection.validation.BaseSelectionValidator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Validates actions based on work schedule
 */
public class WorkScheduleValidator extends BaseSelectionValidator<LocalDate> {

    private final WorkScheduleRepository scheduleRepository;

    @Inject
    public WorkScheduleValidator(@NonNull WorkScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @NonNull
    public SelectionValidationResult validate(@NonNull SelectionAction<LocalDate> action,@NonNull SelectionContext<LocalDate> context) {

        // Get user's team
        HalfTeam userTeam = context.getMetadata("userTeam", HalfTeam.class);
        if (userTeam == null) {
            return createInvalidResult(R.string.selection_validation_invalid_selection);
        }

        // Analyze selection
        WorkAnalysis analysis = analyzeWorkDays(context.getSelectedItems(), userTeam);

        // Validate based on action type
        switch (action.getId()) {
            case "action.vacation":
            case "action.sick_leave":
            case "action.permission":
            case "action.law_104":
                // These actions require work days
                if (analysis.workDays == 0) {
                    return createInvalidResult(R.string.calendar_validation_no_work_days);
                }
                break;

            case "action.overtime":
                // Overtime requires off days
                if (analysis.offDays == 0) {
                    return createInvalidResult(R.string.calendar_validation_no_off_days);
                }
                break;

            case "action.add_event":
                // Always valid
                break;

            default:
                // Unknown action
                return createInvalidResult(R.string.selection_validation_invalid_selection);
        }

        return SelectionValidationResult.valid();
    }

    /**
     * Filter a list of actions to only valid ones
     *
     * @param selectionActions All possible actions
     * @param context          The selection context
     * @param maxActions       Maximum number of actions to return
     * @return List of valid actions, sorted by priority
     */
    @NonNull
    @Override
    public List<SelectionAction<LocalDate>> filterValidActions(@NonNull List<SelectionAction<LocalDate>> selectionActions, @NonNull SelectionContext<LocalDate> context, int maxActions) {
        return Collections.emptyList();
    }

    /**
     * Get a localized user-friendly message for validation result
     *
     * @param context          Android context for resources
     * @param action           The action that was validated
     * @param selectionContext The selection context
     * @param result           The validation result
     * @return Localized message for the user
     */
    @NonNull
    @Override
    public String getValidationMessage(@NonNull Context context, @NonNull SelectionAction<LocalDate> action, @NonNull SelectionContext<LocalDate> selectionContext, @NonNull SelectionValidationResult result) {
        return "";
    }

    /**
     * Analyze work days in selection
     */
    private WorkAnalysis analyzeWorkDays(@NonNull Set<LocalDate> dates, @NonNull HalfTeam userTeam) {
        WorkAnalysis analysis = new WorkAnalysis();

        for (LocalDate date : dates) {
            if (isUserWorkingOnDate(date, userTeam)) {
                analysis.workDays++;
            } else {
                analysis.offDays++;
            }

            if (isWeekend(date)) {
                analysis.weekendDays++;
            }
        }

        analysis.totalDays = dates.size();
        return analysis;
    }

    /**
     * Check if user is working on a specific date
     */
    private boolean isUserWorkingOnDate(@NonNull LocalDate date, @NonNull HalfTeam userTeam) {
        Day day = scheduleRepository.getDayForDate(date);

        if (day == null) {
            // No schedule data - assume traditional work week
            return !isWeekend(date);
        }

        // Check if user's team is scheduled
        for (Shift shift : day.getShifts()) {
            for (HalfTeam team : shift.getHalfTeams()) {
                if (team.isSameTeamAs(userTeam)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if date is weekend
     */
    private boolean isWeekend(@NonNull LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Create invalid result with message resource
     */
    private SelectionValidationResult createInvalidResult(@StringRes int messageResId) {
        return SelectionValidationResult.withDetails(
                false,
                "Invalid",
                new SelectionValidationResult.Builder()
                        .detail("messageResId", messageResId)
                        .build()
                        .getDetails()
        );
    }

    /**
     * Work analysis data
     */
    private static class WorkAnalysis {
        int totalDays = 0;
        int workDays = 0;
        int offDays = 0;
        int weekendDays = 0;
    }
}
