package net.calvuz.qdue.ui.core.components.selection.validation;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;

/**
 * Selection size validator
 */
public class SelectionSizeValidator<T> extends BaseSelectionValidator<T> {

    private final int minSize;
    private final int maxSize;

    public SelectionSizeValidator(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    @NonNull
    public SelectionValidationResult validate(@NonNull SelectionAction<T> action, @NonNull SelectionContext<T> context) {

        int count = context.getSelectionCount();

        if (count < minSize) {
            return SelectionValidationResult.withDetails(
                    false,
                    "Too few items",
                    new SelectionValidationResult.Builder()
                            .detail("messageResId", R.string.selection_validation_too_few_items)
                            .detail("formatArgs", new Object[]{minSize})
                            .build()
                            .getDetails()
            );
        }

        if (count > maxSize) {
            return SelectionValidationResult.withDetails(
                    false,
                    "Too many items",
                    new SelectionValidationResult.Builder()
                            .detail("messageResId", R.string.selection_validation_too_many_items)
                            .detail("formatArgs", new Object[]{maxSize})
                            .build()
                            .getDetails()
            );
        }

        return SelectionValidationResult.valid();
    }
}