package net.calvuz.qdue.ui.features.calendar.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.styling.MaterialSelectionActionStyleProvider;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.FragmentScoped;

/**
 * Optional: Custom style provider for calendar-specific styling
 */
@FragmentScoped
class CalendarSelectionStyleProvider extends MaterialSelectionActionStyleProvider {

    @Inject
    public CalendarSelectionStyleProvider(@ApplicationContext Context context) {
        super(context);
    }

    @Override
    public <T> void styleActionButton(@NonNull MaterialButton button,
                                      @NonNull SelectionAction<T> action) {
        super.styleActionButton(button, action);

        // Add calendar-specific styling if needed
        switch (action.getId()) {
            case "action.vacation":
                // Could add vacation-specific styling
                break;
            case "action.law_104":
                // Could add special styling for protected leave
                break;
        }
    }
}