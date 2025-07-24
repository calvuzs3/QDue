package net.calvuz.qdue.ui.core.components.selection.validation;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;

/**
 * Permission-based validator
 */
public class PermissionBasedValidator<T> extends BaseSelectionValidator<T> {

    @Override
    @NonNull
    public SelectionValidationResult validate(
            @NonNull SelectionAction<T> action,
            @NonNull SelectionContext<T> context) {

        // Check if user has permission (example implementation)
        Boolean hasPermission = context.getMetadata("hasPermission_" + action.getId(), Boolean.class);

        if (hasPermission != null && !hasPermission) {
            return SelectionValidationResult.withDetails(
                    false,
                    "No permission",
                    new SelectionValidationResult.Builder()
                            .detail("messageResId", R.string.selection_validation_no_permission)
                            .build()
                            .getDetails()
            );
        }

        return SelectionValidationResult.valid();
    }
}
