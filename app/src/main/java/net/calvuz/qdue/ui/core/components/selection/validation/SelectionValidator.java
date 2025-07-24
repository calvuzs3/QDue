/**
 * Validator for selection actions with internationalization support
 * Determines which actions are valid for a given selection
 */
package net.calvuz.qdue.ui.core.components.selection.validation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates actions against a selection context with i18n support
 *
 * @param <T> The type of items being selected
 */
public interface SelectionValidator<T> {

    /**
     * Validate if an action is allowed for the given selection
     *
     * @param action The action to validate
     * @param context The selection context
     * @return Validation result with details
     */
    @NonNull
    SelectionValidationResult validate(
            @NonNull SelectionAction<T> action,
            @NonNull SelectionContext<T> context
    );

    /**
     * Filter a list of actions to only valid ones
     *
     * @param actions All possible actions
     * @param context The selection context
     * @param maxActions Maximum number of actions to return
     * @return List of valid actions, sorted by priority
     */
    @NonNull
    List<SelectionAction<T>> filterValidActions(
            @NonNull List<SelectionAction<T>> actions,
            @NonNull SelectionContext<T> context,
            int maxActions
    );

    /**
     * Get a localized user-friendly message for validation result
     *
     * @param context Android context for resources
     * @param action The action that was validated
     * @param selectionContext The selection context
     * @param result The validation result
     * @return Localized message for the user
     */
    @NonNull
    String getValidationMessage(
            @NonNull Context context,
            @NonNull SelectionAction<T> action,
            @NonNull SelectionContext<T> selectionContext,
            @NonNull SelectionValidationResult result
    );
}
