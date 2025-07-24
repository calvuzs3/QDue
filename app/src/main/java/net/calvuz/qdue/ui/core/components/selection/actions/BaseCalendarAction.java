/**
 * Calendar-specific selection actions with full i18n support
 * Implementation example for vacation management system
 */
package net.calvuz.qdue.ui.core.components.selection.actions;

import androidx.annotation.NonNull;

import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionContext;
import net.calvuz.qdue.repository.CalendarRepository;

import java.time.LocalDate;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Base class for calendar actions with common functionality
 */
public abstract class BaseCalendarAction implements SelectionAction<LocalDate> {

    protected final CalendarRepository repository;

    protected BaseCalendarAction(@NonNull CalendarRepository repository) {
        this.repository = repository;
    }

    /**
     * Get user ID from selection context
     */
    protected String getUserId(@NonNull SelectionContext<LocalDate> context) {
        String userId = context.getMetadata("userId", String.class);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in context");
        }
        return userId;
    }
}
