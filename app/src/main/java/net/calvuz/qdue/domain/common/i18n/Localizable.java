package net.calvuz.qdue.domain.common.i18n;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Localizable - Marker interface for domain models that support localization.
 *
 * <p>Provides standard contract for domain models that can be localized
 * through optional DomainLocalizer injection.</p>
 */
public interface Localizable {

    /**
     * Get the injected localizer instance.
     *
     * @return DomainLocalizer instance or null if not injected
     */
    @Nullable
    DomainLocalizer getLocalizer();

    /**
     * Check if this instance has localization support.
     *
     * @return true if localizer is available
     */
    default boolean hasLocalizationSupport() {
        return getLocalizer() != null;
    }

    /**
     * Create a copy of this object with localizer injected.
     * Useful for adding localization to existing instances.
     *
     * @param localizer DomainLocalizer to inject
     * @return New instance with localizer support
     */
    @NonNull
    Localizable withLocalizer(@NonNull DomainLocalizer localizer);
}