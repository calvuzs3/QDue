package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.ui.usecases.*;
import net.calvuz.qdue.smartshifts.data.repository.*;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;

/**
 * Hilt module for providing use case dependencies
 * Manages application layer use cases for ViewModels
 */
@Module
@InstallIn(SingletonComponent.class)
public class UseCaseModule {

    /**
     * Provide get user shifts use case
     */
    @Provides
    @ViewModelScoped
    public GetUserShiftsUseCase provideGetUserShiftsUseCase(
            SmartShiftsRepository repository,
            UserAssignmentRepository assignmentRepository
    ) {
        return new GetUserShiftsUseCase(repository, assignmentRepository);
    }

    /**
     * Provide create pattern use case
     */
    @Provides
    @ViewModelScoped
    public CreatePatternUseCase provideCreatePatternUseCase(
            ShiftPatternRepository patternRepository,
            ValidatePatternUseCase validateUseCase,
            @ApplicationContext Context context
    ) {
        return new CreatePatternUseCase(patternRepository, validateUseCase, context);
    }

    /**
     * Provide assign pattern use case
     */
    @Provides
    @ViewModelScoped
    public AssignPatternUseCase provideAssignPatternUseCase(
            UserAssignmentRepository assignmentRepository,
            SmartShiftsRepository shiftsRepository,
            @ApplicationContext Context context
    ) {
        return new AssignPatternUseCase(assignmentRepository, shiftsRepository, context);
    }

    /**
     * Provide validate pattern use case
     */
    @Provides
    @ViewModelScoped
    public ValidatePatternUseCase provideValidatePatternUseCase(
            ContinuousCycleValidator validator,
            ShiftPatternRepository patternRepository
    ) {
        return new ValidatePatternUseCase(validator, patternRepository);
    }

    /**
     * Provide manage contacts use case
     */
    @Provides
    @ViewModelScoped
    public ManageContactsUseCase provideManageContactsUseCase(
            TeamContactRepository contactRepository,
            @ApplicationContext Context context
    ) {
        return new ManageContactsUseCase(contactRepository, context);
    }
}
