package net.calvuz.qdue.ui.core.components.selection.validation;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.validation.SelectionValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base implementation with common functionality
 */
abstract class BaseSelectionValidator<T> implements SelectionValidator<T> {

    @Override
    @NonNull
    public List<SelectionAction<T>> filterValidActions(
            @NonNull List<SelectionAction<T>> actions,
            @NonNull SelectionContext<T> context,
            int maxActions) {

        // Filter valid actions
        List<SelectionAction<T>> validActions = actions.stream()
                .filter(action -> validate(action, context).isValid())
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .limit(maxActions)
                .collect(Collectors.toList());

        return validActions;
    }

    @Override
    @NonNull
    public String getValidationMessage(
            @NonNull Context context,
            @NonNull SelectionAction<T> action,
            @NonNull SelectionContext<T> selectionContext,
            @NonNull SelectionValidationResult result) {

        if (result.isValid()) {
            return "";
        }

        // Check if result contains a message resource ID
        Integer messageResId = result.getDetail("messageResId", Integer.class);
        if (messageResId != null) {
            Object[] formatArgs = result.getDetail("formatArgs", Object[].class);
            if (formatArgs != null) {
                return context.getString(messageResId, formatArgs);
            } else {
                return context.getString(messageResId);
            }
        }

        // Fallback to generic message
        return context.getString(R.string.selection_validation_invalid_selection);
    }
}
