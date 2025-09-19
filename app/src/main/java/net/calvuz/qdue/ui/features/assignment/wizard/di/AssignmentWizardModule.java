package net.calvuz.qdue.ui.features.assignment.wizard.di;

import android.content.Context;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.di.DependencyInjector;
import net.calvuz.qdue.core.di.Injectable;
import net.calvuz.qdue.core.di.ServiceProvider;
import net.calvuz.qdue.data.services.QDueUserService;
import net.calvuz.qdue.data.di.CalendarServiceProvider;
import net.calvuz.qdue.domain.calendar.repositories.RecurrenceRuleRepository;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.domain.calendar.repositories.UserScheduleAssignmentRepository;
import net.calvuz.qdue.domain.calendar.usecases.CreatePatternAssignmentUseCase;
import net.calvuz.qdue.domain.calendar.usecases.UserTeamAssignmentUseCases;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * AssignmentWizardModule - Dependency Injection module for Pattern Assignment Wizard
 *
 * <p>Provides all dependencies needed by the Pattern Assignment Wizard components.
 * Integrates with existing ServiceProvider pattern while maintaining clean separation.</p>
 */
public class AssignmentWizardModule implements Injectable {

    private CalendarServiceProvider mCalendarServiceProvider;
    private ServiceProvider mServiceProvider;

    // ==================== CONSTRUCTOR ====================

    public AssignmentWizardModule(@NonNull Context context) {
        DependencyInjector.inject( this, context );
    }

    // ==================== DEPENDENCY INJECTION ====================

    /**
     * Inject dependencies into this component
     *
     * @param serviceProvider ServiceProvider instance
     */
    @Override
    public void inject(ServiceProvider serviceProvider) {
        Log.i( "AssignmentWizardModule", "Injecting dependencies into AssignmentWizardModule" );

        this.mCalendarServiceProvider = serviceProvider.getCalendarService().getCalendarServiceProvider(); //mcalendarServiceProvider;
        this.mServiceProvider = serviceProvider;
    }

    /**
     * Check if dependencies are injected and ready
     */
    @Override
    public boolean areDependenciesReady() {
        return true;
    }

    // ==================== USE CASES ====================

    /**
     * Provides CreatePatternAssignmentUseCase with all required repositories.
     */
    @NonNull
    public CreatePatternAssignmentUseCase getCreatePatternAssignmentUseCase() {
        return mCalendarServiceProvider.getCreatePatternAssignmentUseCase();
    }

    @NonNull
    public UserTeamAssignmentUseCases getUserTeamAssignmentUseCases() {
        return mCalendarServiceProvider.getUserTeamAssignmentUseCases();
    }

    @NonNull
    public QDueUserService getQDueUserService() {
        return mServiceProvider.getQDueUserService();
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

    // ==================== FACTORY METHOD ====================

    /**
     * Factory method for creating AssignmentWizardModule instances.
     */
    @NonNull
    public static AssignmentWizardModule create(@NonNull Context context) {
        return new AssignmentWizardModule( context );
    }

}