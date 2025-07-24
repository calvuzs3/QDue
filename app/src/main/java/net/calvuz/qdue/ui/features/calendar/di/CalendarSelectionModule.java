/**
 * Dagger module for calendar selection toolbar
 * Provides all dependencies for calendar-specific selection handling
 */
package net.calvuz.qdue.ui.calendar.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.repository.CalendarRepository;
import net.calvuz.qdue.repository.WorkScheduleRepository;
import net.calvuz.qdue.ui.calendar.selection.actions.*;
import net.calvuz.qdue.ui.calendar.selection.validation.*;
import net.calvuz.qdue.ui.core.components.selection.SelectionToolbar;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.styling.MaterialSelectionActionStyleProvider;
import net.calvuz.qdue.ui.core.components.selection.styling.SelectionActionStyleProvider;
import net.calvuz.qdue.ui.core.components.selection.validation.CompositeSelectionValidator;
import net.calvuz.qdue.ui.core.components.selection.validation.SelectionValidator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.FragmentScoped;

/**
 * Provides calendar-specific selection toolbar dependencies
 */
@Module
@InstallIn(FragmentComponent.class)
public class CalendarSelectionModule {

    // ==================== ACTIONS ====================

    /**
     * Provide all available calendar actions
     */
    @Provides
    @FragmentScoped
    @Named("availableActions")
    public List<SelectionAction<LocalDate>> provideCalendarActions(
            VacationAction vacationAction,
            SickLeaveAction sickLeaveAction,
            PermissionAction permissionAction,
            Law104Action law104Action,
            OvertimeAction overtimeAction,
            AddEventAction addEventAction) {

        return Arrays.asList(
                vacationAction,      // Priority: 100
                sickLeaveAction,     // Priority: 90
                permissionAction,    // Priority: 80
                law104Action,        // Priority: 70
                overtimeAction,      // Priority: 60
                addEventAction       // Priority: 50 (fallback)
        );
    }

    /**
     * Provide vacation action
     */
    @Provides
    @FragmentScoped
    public VacationAction provideVacationAction(CalendarRepository repository) {
        return new VacationAction(repository);
    }

    /**
     * Provide sick leave action
     */
    @Provides
    @FragmentScoped
    public SickLeaveAction provideSickLeaveAction(CalendarRepository repository) {
        return new SickLeaveAction(repository);
    }

    /**
     * Provide permission action
     */
    @Provides
    @FragmentScoped
    public PermissionAction providePermissionAction(CalendarRepository repository) {
        return new PermissionAction(repository);
    }

    /**
     * Provide Law 104 action
     */
    @Provides
    @FragmentScoped
    public Law104Action provideLaw104Action(CalendarRepository repository) {
        return new Law104Action(repository);
    }

    /**
     * Provide overtime action
     */
    @Provides
    @FragmentScoped
    public OvertimeAction provideOvertimeAction(CalendarRepository repository) {
        return new OvertimeAction(repository);
    }

    /**
     * Provide add event action
     */
    @Provides
    @FragmentScoped
    public AddEventAction provideAddEventAction(EventCreationDialogLauncher launcher) {
        return new AddEventAction(launcher);
    }

    // ==================== VALIDATORS ====================

    /**
     * Provide composite validator for calendar actions
     */
    @Provides
    @FragmentScoped
    public SelectionValidator<LocalDate> provideCalendarValidator(
            CalendarPermissionValidator permissionValidator,
            CalendarDateRangeValidator dateRangeValidator,
            WorkScheduleValidator workScheduleValidator) {

        // Order matters: check permissions first, then dates, then work schedule
        return new CompositeSelectionValidator<>(Arrays.asList(
                permissionValidator,    // Check user permissions
                dateRangeValidator,     // Check date validity (past dates, ranges)
                workScheduleValidator   // Check work schedule rules
        ));
    }

    /**
     * Provide permission validator
     */
    @Provides
    @FragmentScoped
    public CalendarPermissionValidator providePermissionValidator() {
        return new CalendarPermissionValidator();
    }

    /**
     * Provide date range validator
     */
    @Provides
    @FragmentScoped
    public CalendarDateRangeValidator provideDateRangeValidator() {
        return new CalendarDateRangeValidator();
    }

    /**
     * Provide work schedule validator
     */
    @Provides
    @FragmentScoped
    public WorkScheduleValidator provideWorkScheduleValidator(
            WorkScheduleRepository scheduleRepository) {
        return new WorkScheduleValidator(scheduleRepository);
    }

    // ==================== STYLING ====================

    /**
     * Provide style provider for calendar actions
     * Can be overridden for custom styling
     */
    @Provides
    @FragmentScoped
    public SelectionActionStyleProvider provideStyleProvider(
            @ApplicationContext Context context) {
        return new MaterialSelectionActionStyleProvider(context);
    }

    /**
     * Provide maximum actions for calendar toolbar
     */
    @Provides
    @Named("maxActions")
    public int provideMaxActions() {
        return 5; // Show up to 5 actions in calendar toolbar
    }

    // ==================== TOOLBAR ====================

    /**
     * Provide configured selection toolbar for calendar
     */
    @Provides
    @FragmentScoped
    public SelectionToolbar<LocalDate> provideCalendarSelectionToolbar(
            @ApplicationContext Context context,
            @Named("availableActions") List<SelectionAction<LocalDate>> actions,
            SelectionValidator<LocalDate> validator,
            SelectionActionStyleProvider styleProvider,
            @Named("maxActions") int maxActions) {

        return new SelectionToolbar<>(
                context,
                actions,
                validator,
                styleProvider,
                maxActions
        );
    }

    // ==================== SUPPORTING SERVICES ====================

    /**
     * Provide event creation dialog launcher
     */
    @Provides
    @FragmentScoped
    public EventCreationDialogLauncher provideEventCreationDialogLauncher(
            FragmentManager fragmentManager) {
        return new EventCreationDialogLauncher(fragmentManager);
    }
}
