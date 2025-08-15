package net.calvuz.qdue.domain.common.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.domain.common.i18n.Localizable;

/**
 * LocalizableDomainModel - Abstract base for localizable domain models.
 *
 * <p>Provides common localization infrastructure that can be extended
 * by all domain models requiring i18n support.</p>
 */
public abstract class LocalizableDomainModel implements Localizable {

    protected final DomainLocalizer mLocalizer;
    protected final DomainLocalizer mScopedLocalizer;

    /**
     * Constructor with optional localizer.
     *
     * @param localizer Optional DomainLocalizer instance
     * @param scope Localization scope for this model type
     */
    protected LocalizableDomainModel(@Nullable DomainLocalizer localizer, @NonNull String scope) {
        this.mLocalizer = localizer;
        this.mScopedLocalizer = localizer != null ? localizer.scope(scope) : null;
    }

    @Override
    @Nullable
    public DomainLocalizer getLocalizer() {
        return mLocalizer;
    }

    /**
     * Get scoped localizer for this model type.
     *
     * @return Scoped DomainLocalizer or null if not available
     */
    @Nullable
    protected DomainLocalizer getScopedLocalizer() {
        return mScopedLocalizer;
    }

    /**
     * Localize a key within this model's scope.
     *
     * @param key Key to localize (automatically scoped)
     * @param fallback Fallback value if localization unavailable
     * @param params Optional parameters for formatting
     * @return Localized string or fallback
     */
    @NonNull
    protected String localize(@NonNull String key, @NonNull String fallback, @Nullable Object... params) {
        if (mScopedLocalizer != null) {
            return mScopedLocalizer.localize(key, params);
        }
        return fallback;
    }

    /**
     * Localize a key with automatic fallback to the key itself.
     *
     * @param key Key to localize
     * @param params Optional parameters
     * @return Localized string or key as fallback
     */
    @NonNull
    protected String localizeOrKey(@NonNull String key, @Nullable Object... params) {
        return localize(key, key, params);
    }
}