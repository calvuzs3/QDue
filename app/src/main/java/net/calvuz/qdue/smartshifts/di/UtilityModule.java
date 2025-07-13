package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.utils.DateTimeHelper;
import net.calvuz.qdue.smartshifts.utils.ColorHelper;
import net.calvuz.qdue.smartshifts.utils.StringHelper;
import net.calvuz.qdue.smartshifts.utils.ValidationHelper;
import net.calvuz.qdue.smartshifts.utils.JsonHelper;


import javax.inject.Singleton;

/**
 * Hilt module for providing utility dependencies
 * Manages helper classes and utility components
 */
@Module
@InstallIn(SingletonComponent.class)
public class UtilityModule {

    /**
     * Provide date time helper
     */
    @Provides
    @Singleton
    public DateTimeHelper provideDateTimeHelper() {
        return new DateTimeHelper();
    }

    /**
     * Provide color helper
     */
    @Provides
    @Singleton
    public ColorHelper provideColorHelper() {
        return new ColorHelper();
    }

    /**
     * Provide string helper
     */
    @Provides
    @Singleton
    public StringHelper provideStringHelper() {
        return new StringHelper();
    }

    /**
     * Provide validation helper
     */
    @Provides
    @Singleton
    public ValidationHelper provideValidationHelper() {
        return new ValidationHelper();
    }

    /**
     * Provide JSON helper
     */
    @Provides
    @Singleton
    public JsonHelper provideJsonHelper() {
        return new JsonHelper();
    }
}
