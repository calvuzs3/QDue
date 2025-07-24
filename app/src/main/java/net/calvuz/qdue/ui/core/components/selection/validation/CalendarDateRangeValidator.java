package net.calvuz.qdue.ui.core.components.selection.validation;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.validation.BaseSelectionValidator;

import java.time.LocalDate;
import java.util.Set;

/**
 * Validates date ranges for calendar actions
 */
public class CalendarDateRangeValidator extends BaseSelectionValidator<LocalDate> {

    private static final int MAX_VACATION_DAYS = 30;

    @Override
    @NonNull
    public SelectionValidationResult validate(
            @NonNull SelectionAction<LocalDate> action,
            @NonNull SelectionContext<LocalDate> context) {

        Set<LocalDate> selectedDates = context.getSelectedItems();

        // Check for past dates
        boolean hasPastDates = selectedDates.stream()
                .anyMatch(date -> date.isBefore(LocalDate.now()));

        if (hasPastDates) {
            switch (action.getId()) {
                case "action.vacation":
                case "action.overtime":
                case "action.permission":
                    return createInvalidResult(R.string.calendar_validation_past_dates);
            }
        }

        // Check vacation day limit
        if ("action.vacation".equals(action.getId())) {
            if (selectedDates.size() > MAX_VACATION_DAYS) {
                return createInvalidResult(
                        R.string.calendar_validation_too_many_vacation_days,
                        MAX_VACATION_DAYS
                );
            }
        }

        return SelectionValidationResult.valid();
    }

    /**
     * Create invalid result with message and format args
     */
    private SelectionValidationResult createInvalidResult(@StringRes int messageResId,
                                                          Object... formatArgs) {
        return SelectionValidationResult.withDetails(
                false,
                "Invalid",
                new SelectionValidationResult.Builder()
                        .detail("messageResId", messageResId)
                        .detail("formatArgs", formatArgs)
                        .build()
                        .getDetails()
        );
    }
}
