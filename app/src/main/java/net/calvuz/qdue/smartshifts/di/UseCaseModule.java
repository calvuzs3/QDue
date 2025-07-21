package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.domain.usecases.AssignPatternUseCase;
import net.calvuz.qdue.smartshifts.domain.usecases.CreatePatternUseCase;
import net.calvuz.qdue.smartshifts.domain.usecases.GetUserShiftsUseCase;
import net.calvuz.qdue.smartshifts.domain.usecases.ManageContactsUseCase;
import net.calvuz.qdue.smartshifts.domain.usecases.ValidatePatternUseCase;
import net.calvuz.qdue.smartshifts.data.repository.*;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;

import javax.inject.Singleton;

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
    @Singleton // ✅ Cambiato da @ViewModelScoped a @Singleton
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
    @Singleton // ✅ Cambiato da @ViewModelScoped a @Singleton
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
    @Singleton // ✅ Cambiato da @ViewModelScoped a @Singleton
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
    @Singleton // ✅ Cambiato da @ViewModelScoped a @Singleton
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
    @Singleton // ✅ Cambiato da @ViewModelScoped a @Singleton
    public ManageContactsUseCase provideManageContactsUseCase(
            TeamContactRepository contactRepository,
            @ApplicationContext Context context
    ) {
        return new ManageContactsUseCase(contactRepository, context);
    }
}
