package net.calvuz.qdue.ui.features.assignment.wizard.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.core.di.impl.ServiceProviderImpl;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;

/**
 * AssignmentWizardModule - Dependency Injection module for Pattern Assignment Wizard
 *
 * <p>Provides all dependencies needed by the Pattern Assignment Wizard components.
 * Integrates with existing ServiceProvider pattern while maintaining clean separation.</p>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Clean Architecture Phase 2
 */
public class AssignmentWizardModule {

    private final Context mContext;
    private final CalendarServiceProvider mCalendarServiceProvider;

    // Lazy-loaded dependencies
    private CreatePatternAssignmentUseCase mCreatePatternAssignmentUseCase;

    // ==================== CONSTRUCTOR ====================

    public AssignmentWizardModule(@NonNull Context context, @NonNull CalendarServiceProvider calendarServiceProvider) {
        this.mContext = context.getApplicationContext();
        this.mCalendarServiceProvider = calendarServiceProvider;
    }

    // ==================== USE CASES ====================

    /**
     * Provides CreatePatternAssignmentUseCase with all required repositories.
     */
    @NonNull
    public CreatePatternAssignmentUseCase getCreatePatternAssignmentUseCase() {
        return mCalendarServiceProvider.getCreatePatternAssignmentUseCase();
    }

    // ==================== REPOSITORIES ====================

    @NonNull
    public TeamRepository getTeamRepository() {
        return mCalendarServiceProvider.getTeamRepository();
    }

    @NonNull
    public RecurrenceRuleRepository getRecurrenceRuleRepository() {
        return mCalendarServiceProvider.getRecurrenceRuleRepository();
    }

    @NonNull
    public UserScheduleAssignmentRepository getUserScheduleAssignmentRepository() {
        return mCalendarServiceProvider.getUserScheduleAssignmentRepository();
    }

    // ==================== CONTEXT ====================

    @NonNull
    public Context getContext() {
        return mContext;
    }

    // ==================== FACTORY METHOD ====================

    /**
     * Factory method for creating AssignmentWizardModule instances.
     */
    @NonNull
    public static AssignmentWizardModule create(@NonNull Context context) {
        ServiceProvider serviceProvider = ServiceProviderImpl.getInstance(context);
        return new AssignmentWizardModule(context,
                serviceProvider.getCalendarService().getCalendarServiceProvider());
    }
}