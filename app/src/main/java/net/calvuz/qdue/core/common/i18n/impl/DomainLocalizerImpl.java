package net.calvuz.qdue.core.common.i18n.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.core.common.i18n.LocaleManager;
import net.calvuz.qdue.domain.common.i18n.DomainLocalizer;
import net.calvuz.qdue.ui.core.common.utils.Log;

/**
 * DomainLocalizerImpl - Core Layer Bridge to LocaleManager
 *
 * <p>Provides the bridge between domain layer localization needs and the core
 * LocaleManager infrastructure. This implementation is in the CORE layer to
 * maintain clean architecture principles - the domain layer cannot depend on
 * infrastructure components directly.</p>
 *
 * <h3>Clean Architecture Compliance:</h3>
 * <ul>
 *   <li><strong>Core Layer Implementation</strong>: Located in core, not domain</li>
 *   <li><strong>Domain Interface</strong>: Implements domain DomainLocalizer interface</li>
 *   <li><strong>Infrastructure Bridge</strong>: Bridges to LocaleManager capabilities</li>
 *   <li><strong>Dependency Direction</strong>: Core → Domain (interfaces only)</li>
 * </ul>
 *
 * <h3>LocaleManager Integration:</h3>
 * <p>Uses LocaleManager's comprehensive localization capabilities:</p>
 * <ul>
 *   <li><strong>getLocalizedString()</strong>: Main localization method with fallback</li>
 *   <li><strong>Resource Resolution</strong>: Android string resource integration</li>
 *   <li><strong>Context Management</strong>: Proper context handling for resources</li>
 *   <li><strong>Locale Awareness</strong>: Current locale-specific formatting</li>
 * </ul>
 *
 * <h3>Scoped Localization Pattern:</h3>
 * <pre>
 * // Android resource naming with underscores:
 * domain_{scope}_{category}_{item}
 *
 * Examples:
 * - domain_calendar_recurrence_frequency_daily
 * - domain_calendar_exceptions_type_vacation
 * - domain_calendar_validation_conflict_detected
 * </pre>
 */
public class DomainLocalizerImpl implements DomainLocalizer {

    private static final String TAG = "DomainLocalizerImpl";

    // Domain localization key prefix for string resources
    private static final String DOMAIN_KEY_PREFIX = "domain_";

    // Dependencies
    private final LocaleManager mLocaleManager;
    private final Context mContext;
    private final String mScope;

    // ==================== CONSTRUCTORS ====================

    /**
     * Create root domain localizer (no scope).
     *
     * @param context Application context for resource access
     * @param localeManager Core LocaleManager for i18n infrastructure
     */
    public DomainLocalizerImpl(
            @NonNull Context context,
            @NonNull LocaleManager localeManager
    ) {
        this(context, localeManager, "");
    }

    /**
     * Create scoped domain localizer.
     *
     * @param context Application context for resource access
     * @param localeManager Core LocaleManager for i18n infrastructure
     * @param scope Localization scope for key prefixing
     */
    private DomainLocalizerImpl(
            @NonNull Context context,
            @NonNull LocaleManager localeManager,
            @NonNull String scope
    ) {
        this.mContext = context.getApplicationContext();
        this.mLocaleManager = localeManager;
        this.mScope = scope;
    }

    // ==================== DOMAIN LOCALIZER INTERFACE ====================

    /**
     * Localize a string key with optional parameters.
     *
     * @param key    Localization key (e.g., "recurrence.frequency.daily")
     * @param params Optional parameters for string formatting
     * @return Localized string or fallback if key not found
     */
    @NonNull
    @Override
    public String localize(@NonNull String key, @Nullable Object... params) {
        return localize( key, key, params );
    }

    @NonNull
    public String localize(@NonNull String key, @NonNull String fallback, @Nullable Object... params) {
        try {
            // Build Android resource key with domain prefix and scope
            String resourceKey = buildResourceKey(key);

            // Get localized context for proper resource resolution
            Context localizedContext = mLocaleManager.updateContextLocale(mContext);

            // Try to get localized string from LocaleManager using static method
            String localizedString = LocaleManager.getLocalizedString(localizedContext, resourceKey, fallback);

            // If we have parameters, format the string with current locale
            if ((params != null) && params.length > 0) {
                try {
                    return String.format(mLocaleManager.getCurrentLocale(), localizedString, params);
                } catch (Exception formatError) {
                    Log.w(TAG, "Error formatting localized string with parameters", formatError);
                    return localizedString; // Return unformatted string
                }
            }

            return localizedString;

        } catch (Exception e) {
            Log.e(TAG, "Error localizing domain key: " + key + ", scope: " + mScope, e);

            // Try fallback with parameters if provided
            if ((params != null) && params.length > 0) {
                try {
                    return String.format(mLocaleManager.getCurrentLocale(), fallback, params);
                } catch (Exception formatError) {
                    Log.w(TAG, "Error formatting fallback string", formatError);
                }
            }

            return fallback;
        }
    }



    /**
     * Check if a localization key exists.
     *
     * @param key Localization key to check
     * @return true if key has translation available
     */
    @Override
    public boolean hasLocalization(@NonNull String key) {
        // Usa getIdentifier() per verificare esistenza risorsa Android
        int resourceId = mContext.getResources().getIdentifier(
                buildResourceKey( key ), "string", mContext.getPackageName());
        return resourceId != 0;
    }

    /**
     * Get current locale identifier.
     *
     * @return Locale identifier (e.g., "it", "en", "de")
     */
    @NonNull
    @Override
    public String getCurrentLocale() {
        return "";
    }

    @Override
    @NonNull
    public DomainLocalizer scope(@NonNull String nestedScope) {
        // Create nested scope: existing scope + new scope
        String newScope = mScope.isEmpty() ? nestedScope : mScope + "_" + nestedScope;
        return new DomainLocalizerImpl(mContext, mLocaleManager, newScope);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Build Android resource key from domain localization key.
     *
     * <p>Converts domain hierarchical keys to Android resource naming conventions:</p>
     * <ul>
     *   <li>domain.calendar.recurrence.frequency.daily → domain_calendar_recurrence_frequency_daily</li>
     *   <li>scope: recurrence, key: frequency.daily → domain_recurrence_frequency_daily</li>
     * </ul>
     *
     * @param key Base localization key
     * @return Android-compatible resource key
     */
    @NonNull
    private String buildResourceKey(@NonNull String key) {
        StringBuilder keyBuilder = new StringBuilder();

        // Add domain prefix
        keyBuilder.append(DOMAIN_KEY_PREFIX);

        // Add scope if present (convert dots to underscores for Android resources)
        if (!mScope.isEmpty()) {
            keyBuilder.append(mScope.replace(".", "_")).append("_");
        }

        // Add base key (convert dots to underscores for Android resources)
        keyBuilder.append(key.replace(".", "_"));

        return keyBuilder.toString();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get current scope for debugging/logging.
     *
     * @return Current localization scope
     */
    @NonNull
    public String getCurrentScope() {
        return mScope.isEmpty() ? "root" : mScope;
    }

    /**
     * Check if scope is root (no scope prefix).
     *
     * @return true if this is the root localizer
     */
    public boolean isRootScope() {
        return mScope.isEmpty();
    }

    /**
     * Get the underlying LocaleManager for advanced operations.
     *
     * @return LocaleManager instance
     */
    @NonNull
    public LocaleManager getLocaleManager() {
        return mLocaleManager;
    }

    @NonNull
    @Override
    public String toString() {
        return "DomainLocalizerImpl{" +
                "scope='" + getCurrentScope() + '\'' +
                ", localeManager=" + mLocaleManager +
                '}';
    }
}