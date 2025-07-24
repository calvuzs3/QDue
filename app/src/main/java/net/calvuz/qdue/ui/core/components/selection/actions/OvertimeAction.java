package net.calvuz.qdue.ui.core.components.selection.actions;

import android.net.http.NetworkException;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;

import java.time.LocalDate;
import java.util.Set;

import javax.inject.Inject;

/**
 * Overtime registration action
 */
public class OvertimeAction extends BaseCalendarAction {

    @Inject
    public OvertimeAction(@NonNull CalendarRepository repository) {
        super(repository);
    }

    @Override
    @NonNull
    public String getId() {
        return "action.overtime";
    }

    @Override
    @StringRes
    public int getLabelResource() {
        return R.string.action_overtime_label;
    }

    @Override
    @DrawableRes
    public int getIconResource() {
        return R.drawable.ic_rounded_overtime_gears_24;
    }

    @Override
    @StringRes
    public int getContentDescriptionResource() {
        return R.string.action_overtime_desc;
    }

    @Override
    @NonNull
    public ActionCategory getCategory() {
        return ActionCategory.SECONDARY;
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public void execute(@NonNull SelectionContext<LocalDate> context,
                        @NonNull ActionCallback callback) {

        Set<LocalDate> selectedDates = context.getSelectedItems();
        String userId = getUserId(context);

        repository.registerOvertime(selectedDates, userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> callback.onSuccess(R.string.calendar_overtime_registered, selectedDates.size()),
                        error -> {
                            if (error instanceof NetworkException) {
                                callback.onError(R.string.calendar_error_network);
                            } else {
                                callback.onError(R.string.calendar_error_overtime_register);
                            }
                        }
                );
    }
}
