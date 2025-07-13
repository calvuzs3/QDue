package net.calvuz.qdue.smartshifts.di.qualifiers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Qualifier for system preferences
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemPreferences {
}
