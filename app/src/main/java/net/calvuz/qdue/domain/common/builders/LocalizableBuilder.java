package net.calvuz.qdue.domain.common.builders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;

/**
 * LocalizableBuilder - Base builder class for localizable domain models.
 *
 * @param <T> The domain model type being built
 * @param <B> The builder type (self-referencing for fluent API)
 */
public abstract class LocalizableBuilder<T, B extends LocalizableBuilder<T, B>> {

    // Public for ..
    public DomainLocalizer mLocalizer;

    /**
     * Set localizer for the domain model being built.
     *
     * @param localizer DomainLocalizer instance
     * @return Builder instance for chaining
     */
    @NonNull
    public B localizer(@Nullable DomainLocalizer localizer) {
        this.mLocalizer = localizer;
        return self();
    }

    /**
     * Copy all localizable fields from another instance.
     *
     * @param source Source instance to copy from
     * @return Builder instance for chaining
     */
    @NonNull
    public B copyLocalizableFrom(@NonNull T source) {
        if (source instanceof net.calvuz.qdue.domain.common.i18n.Localizable) {
            this.mLocalizer = ((net.calvuz.qdue.domain.common.i18n.Localizable) source).getLocalizer();
        }
        return self();
    }

    /**
     * Abstract method that returns the concrete builder type.
     * Required for fluent API with inheritance.
     *
     * @return This builder instance cast to concrete type
     */
    @NonNull
    protected abstract B self();

    /**
     * Build the domain model instance.
     *
     * @return Built domain model
     */
    @NonNull
    public abstract T build();
}