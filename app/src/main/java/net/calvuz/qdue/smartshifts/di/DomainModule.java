package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.domain.generators.ShiftGeneratorEngine;
import net.calvuz.qdue.smartshifts.domain.generators.RecurrenceRuleParser;
import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.domain.validators.ContinuousCycleValidator;
import net.calvuz.qdue.smartshifts.domain.validators.ShiftTimeValidator;

import javax.inject.Singleton;

/**
 * Hilt module for providing domain layer dependencies
 * Manages business logic components and generators
 */
@Module
@InstallIn(SingletonComponent.class)
public class DomainModule {

    /**
     * Provide shift generator engine
     */
    @Provides
    @Singleton
    public ShiftGeneratorEngine provideShiftGeneratorEngine(
            ShiftTypeDao shiftTypeDao,
            ShiftPatternDao patternDao,
            RecurrenceRuleParser ruleParser,
            @ApplicationContext Context context
    ) {
        return new ShiftGeneratorEngine(
                shiftTypeDao,
                patternDao,
                ruleParser,
                context
        );
    }

    /**
     * Provide recurrence rule parser
     */
    @Provides
    @Singleton
    public RecurrenceRuleParser provideRecurrenceRuleParser() {
        return new RecurrenceRuleParser();
    }

    /**
     * Provide continuous cycle validator (now STATIC METHODS)
     */
    @Provides
    @Singleton
    public ContinuousCycleValidator provideContinuousCycleValidator(
            ShiftTimeValidator timeValidator
    ) {
        return new ContinuousCycleValidator(timeValidator);
    }

    /**
     * Provide shift time validator (now STATIC METHODS)
     */
    @Provides
    @Singleton
    public ShiftTimeValidator provideShiftTimeValidator() {
        return new ShiftTimeValidator();
    }
}
