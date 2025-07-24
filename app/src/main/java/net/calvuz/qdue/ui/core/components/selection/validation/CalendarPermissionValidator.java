package net.calvuz.qdue.ui.core.components.selection.validation;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.validation.BaseSelectionValidator;

import java.time.LocalDate;

/**
 * Validates based on user permissions
 */
public class CalendarPermissionValidator extends BaseSelectionValidator<LocalDate> {

    @Override
    @NonNull
    public SelectionValidationResult validate(
            @NonNull SelectionAction<LocalDate> action,
            @NonNull SelectionContext<LocalDate> context) {

        // Check user permissions from context
        String permission = getRequiredPermission(action);
        if (permission != null) {
            Boolean hasPermission = context.getMetadata("permission." + permission, Boolean.class);

            if (hasPermission == null || !hasPermission) {
                return createInvalidResult(R.string.selection_validation_no_permission);
            }
        }

        return SelectionValidationResult.valid();
    }

    /**
     * Get required permission for action
     */
    private String getRequiredPermission(@NonNull SelectionAction<LocalDate> action) {
        switch (action.getId()) {
            case "action.vacation":
                return "request_vacation";
            case "action.sick_leave":
                return "report_sick_leave";
            case "action.permission":
                return "request_permission";
            case "action.law_104":
                return "request_law_104";
            case "action.overtime":
                return "register_overtime";
            default:
                return null;
        }
    }

    private SelectionValidationResult createInvalidResult(@StringRes int messageResId) {
        return SelectionValidationResult.withDetails(
                false,
                "No permission",
                new SelectionValidationResult.Builder()
                        .detail("messageResId", messageResId)
                        .build()
                        .getDetails()
        );
    }
}