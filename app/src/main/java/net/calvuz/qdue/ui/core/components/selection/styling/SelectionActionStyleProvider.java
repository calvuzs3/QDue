/**
 * Provider for selection action button styling
 * Allows customization of button appearance
 */
package net.calvuz.qdue.ui.core.components.selection.styling;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;

/**
 * Interface for providing custom styling to selection action buttons
 */
public interface SelectionActionStyleProvider {

    /**
     * Create a new action button with base styling
     *
     * @param context Android context
     * @param parent Parent view group
     * @return Styled MaterialButton
     */
    @NonNull
    MaterialButton createActionButton(@NonNull Context context, @NonNull ViewGroup parent);

    /**
     * Apply action-specific styling to a button
     *
     * @param button The button to style
     * @param action The action this button represents
     */
    <T> void styleActionButton(@NonNull MaterialButton button, @NonNull SelectionAction<T> action);

    /**
     * Get the recommended button size in pixels
     */
    int getButtonSize(@NonNull Context context);

    /**
     * Get the recommended icon size in pixels
     */
    int getIconSize(@NonNull Context context);

    /**
     * Get the recommended spacing between buttons in pixels
     */
    int getButtonSpacing(@NonNull Context context);
}