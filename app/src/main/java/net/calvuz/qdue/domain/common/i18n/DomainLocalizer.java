package net.calvuz.qdue.domain.common.i18n;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * DomainLocalizer - Universal localization interface for domain layer.
 *
 * <p>Provides clean architecture compliant localization for all domain models.
 * Designed to be optional and injectable through dependency injection.</p>
 *
 * <h3>Usage:</h3>
 * <ul>
 *   <li>Domain models accept optional localizer in constructor</li>
 *   <li>Presentation layer injects implementation via DI</li>
 *   <li>Testing uses mock or null localizer</li>
 *   <li>Background operations work without localizer</li>
 * </ul>
 */
public interface DomainLocalizer {

    /**
     * Localize a string key with optional parameters.
     *
     * @param key Localization key (e.g., "recurrence.frequency.daily")
     * @param params Optional parameters for string formatting
     * @return Localized string or fallback if key not found
     */
    @NonNull
    String localize(@NonNull String key, @Nullable Object... params);

    /**
     * Check if a localization key exists.
     *
     * @param key Localization key to check
     * @return true if key has translation available
     */
    boolean hasLocalization(@NonNull String key);

    /**
     * Get current locale identifier.
     *
     * @return Locale identifier (e.g., "it", "en", "de")
     */
    @NonNull
    String getCurrentLocale();

    /**
     * Create a scoped localizer for specific domain context.
     * Useful for prefixing keys automatically.
     *
     * @param scope Key prefix scope (e.g., "recurrence", "shift", "team")
     * @return Scoped localizer that automatically prefixes keys
     */
    @NonNull
    DomainLocalizer scope(@NonNull String scope);
}