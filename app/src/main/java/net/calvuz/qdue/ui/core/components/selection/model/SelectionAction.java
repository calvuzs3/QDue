/**
 * Generic selection action interface with internationalization support
 * Allows any action type to be used in the selection toolbar
 */
package net.calvuz.qdue.ui.core.components.selection.model;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Represents an action that can be performed on a selection
 * with full internationalization support
 *
 * @param <T> The type of items being selected
 */
public interface SelectionAction<T> {

    /**
     * Unique identifier for this action
     */
    @NonNull
    String getId();

    /**
     * Display label resource for the action
     */
    @StringRes
    int getLabelResource();

    /**
     * Get localized label
     */
    @NonNull
    default String getLabel(@NonNull Context context) {
        return context.getString(getLabelResource());
    }

    /**
     * Icon resource for the action
     */
    @DrawableRes
    int getIconResource();

    /**
     * Content description resource for accessibility
     */
    @StringRes
    int getContentDescriptionResource();

    /**
     * Get localized content description
     */
    @NonNull
    default String getContentDescription(@NonNull Context context) {
        return context.getString(getContentDescriptionResource());
    }

    /**
     * Action category for styling purposes
     */
    @NonNull
    ActionCategory getCategory();

    /**
     * Priority for display ordering (higher = more important)
     */
    int getPriority();

    /**
     * Execute the action on the selection
     *
     * @param context The selection context
     * @param callback Result callback
     */
    void execute(@NonNull SelectionContext<T> context, @NonNull ActionCallback callback);

    /**
     * Action categories for consistent styling
     */
    enum ActionCategory {
        PRIMARY,     // Main actions (e.g., create, add)
        SECONDARY,   // Common actions (e.g., edit, share)
        SPECIAL,     // Protected or special actions
        DESTRUCTIVE  // Delete or remove actions
    }

    /**
     * Callback for action execution results with i18n support
     */
    interface ActionCallback {
        /**
         * Called on successful execution
         */
        void onSuccess();

        /**
         * Called on successful execution with message
         * @param messageResId String resource ID for success message
         */
        void onSuccess(@StringRes int messageResId);

        /**
         * Called on error with localized message
         * @param messageResId String resource ID for error message
         */
        void onError(@StringRes int messageResId);

        /**
         * Called on error with localized message and format args
         * @param messageResId String resource ID for error message
         * @param formatArgs Format arguments for the message
         */
        void onError(@StringRes int messageResId, Object... formatArgs);

        /**
         * Called when action is cancelled
         */
        void onCancelled();
    }
}