package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.data.repository.SmartShiftsRepository;
import net.calvuz.qdue.smartshifts.data.repository.ShiftPatternRepository;
import net.calvuz.qdue.smartshifts.data.repository.TeamContactRepository;
import net.calvuz.qdue.smartshifts.data.repository.UserAssignmentRepository;
import net.calvuz.qdue.smartshifts.domain.generators.ShiftGeneratorEngine;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;
import net.calvuz.qdue.smartshifts.data.dao.*;

import javax.inject.Singleton;

/**
 * Hilt module for providing repository dependencies
 * Manages repository pattern implementation and business logic
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    /**
     * Provide main SmartShifts repository
     */
    @Provides
    @Singleton
    public SmartShiftsRepository provideSmartShiftsRepository(
            ShiftPatternDao patternDao,
            UserShiftAssignmentDao assignmentDao,
            SmartShiftEventDao eventDao,
            ShiftGeneratorEngine generatorEngine,
            @ApplicationContext Context context
    ) {
        return new SmartShiftsRepository(
                patternDao,
                assignmentDao,
                eventDao,
                generatorEngine,
                context
        );
    }

    /**
     * Provide ShiftPattern repository
     */
    @Provides
    @Singleton
    public ShiftPatternRepository provideShiftPatternRepository(
            ShiftPatternDao patternDao,
            ShiftTypeDao shiftTypeDao,
            ContinuousCycleValidator validator,
            @ApplicationContext Context context
    ) {
        return new ShiftPatternRepository(
                patternDao,
                shiftTypeDao,
                validator,
                context
        );
    }

    /**
     * Provide UserAssignment repository
     */
    @Provides
    @Singleton
    public UserAssignmentRepository provideUserAssignmentRepository(
            UserShiftAssignmentDao assignmentDao,
            ShiftPatternDao patternDao,
            SmartShiftEventDao eventDao,
            ShiftGeneratorEngine generatorEngine
    ) {
        return new UserAssignmentRepository(
                assignmentDao,
                patternDao,
                eventDao,
                generatorEngine
        );
    }

    /**
     * Provide TeamContact repository
     */
    @Provides
    @Singleton
    public TeamContactRepository provideTeamContactRepository(
            TeamContactDao contactDao,
            @ApplicationContext Context context
    ) {
        return new TeamContactRepository(contactDao, context);
    }
}

// =====================================================================

