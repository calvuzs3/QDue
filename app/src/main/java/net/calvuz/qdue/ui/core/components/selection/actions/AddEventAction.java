package net.calvuz.qdue.ui.core.components.selection.actions;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;

import java.time.LocalDate;

import javax.inject.Inject;

/**
 * Generic add event action
 */
public class AddEventAction implements SelectionAction<LocalDate> {

    private final EventCreationDialogLauncher dialogLauncher;

    @Inject
    public AddEventAction(@NonNull EventCreationDialogLauncher dialogLauncher) {
        this.dialogLauncher = dialogLauncher;
    }

    @Override
    @NonNull
    public String getId() {
        return "action.add_event";
    }

    @Override
    @StringRes
    public int getLabelResource() {
        return R.string.action_add_event_label;
    }

    @Override
    @DrawableRes
    public int getIconResource() {
        return R.drawable.ic_rounded_calendar_add_on_24;
    }

    @Override
    @StringRes
    public int getContentDescriptionResource() {
        return R.string.action_add_event_desc;
    }

    @Override
    @NonNull
    public ActionCategory getCategory() {
        return ActionCategory.PRIMARY;
    }

    @Override
    public int getPriority() {
        return 50; // Lower priority - fallback action
    }

    /**
     * Execute the action on the selection
     *
     * @param context  The selection context
     * @param callback Result callback
     */
    @Override
    public void execute(@NonNull SelectionContext<LocalDate> context, @NonNull ActionCallback callback) {

        // Launch event creation dialog
        dialogLauncher.showEventCreationDialog(
                context.getSelectedItems(),
                new EventCreationDialogLauncher.Callback() {
                    @Override
                    public void onEventCreated() {
                        callback.onSuccess(R.string.calendar_event_added);
                    }

                    @Override
                    public void onError() {
                        callback.onError(R.string.calendar_error_add_event);
                    }

                    @Override
                    public void onCancelled() {
                        callback.onCancelled();
                    }
                }
        );
    }
}