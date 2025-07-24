package net.calvuz.qdue.ui.core.components.selection.validation;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.validation.SelectionValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite validator that combines multiple validators
 */
public class CompositeSelectionValidator<T> extends BaseSelectionValidator<T> {

    private final List<SelectionValidator<T>> validators;

    public CompositeSelectionValidator(@NonNull List<SelectionValidator<T>> validators) {
        this.validators = new ArrayList<>(validators);
    }

    @Override
    @NonNull
    public SelectionValidationResult validate(@NonNull SelectionAction<T> action, @NonNull SelectionContext<T> context) {

        // All validators must pass
        for (SelectionValidator<T> validator : validators) {
            SelectionValidationResult result = validator.validate(action, context);
            if (!result.isValid()) {
                return result;
            }
        }

        return SelectionValidationResult.valid();
    }

    @Override
    @NonNull
    public String getValidationMessage(
            @NonNull Context context,
            @NonNull SelectionAction<T> action,
            @NonNull SelectionContext<T> selectionContext,
            @NonNull SelectionValidationResult result) {

        // Use first validator that can provide a message
        for (SelectionValidator<T> validator : validators) {
            String message = validator.getValidationMessage(context, action, selectionContext, result);
            if (!message.isEmpty()) {
                return message;
            }
        }

        return super.getValidationMessage(context, action, selectionContext, result);
    }
}
