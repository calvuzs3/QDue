package net.calvuz.qdue.smartshifts.di.qualifiers;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for legacy QDue database
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyQDueDb {
}
