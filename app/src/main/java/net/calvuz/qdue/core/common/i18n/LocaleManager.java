package net.calvuz.qdue.core.common.i18n;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * LocaleManager - Dynamic Language Switching for QDue Application.
 *
 * <p>Provides comprehensive internationalization support with dynamic locale switching,
 * cultural adaptation, and seamless language changes without app restart. Supports
 * the gradual rollout strategy for QDue's international expansion.</p>
 *
 * <h3>Supported Languages:</h3>
 * <ul>
 *   <li><strong>Italian (IT)</strong> - Primary language (complete)</li>
 *   <li><strong>English (EN)</strong> - Secondary language (Phase 2)</li>
 *   <li><strong>German (DE)</strong> - Planned for Phase 3 (industrial focus)</li>
 *   <li><strong>French (FR)</strong> - Planned for Phase 3</li>
 * </ul>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Dynamic locale switching without app restart</li>
 *   <li>Cultural date/time formatting adaptation</li>
 *   <li>Automatic system locale detection</li>
 *   <li>Fallback to primary language for missing translations</li>
 *   <li>Work schedule terminology localization</li>
 * </ul>
 */
public class LocaleManager {

    private static final String TAG = "LocaleManager";

    // Preference keys
    private static final String PREF_SELECTED_LANGUAGE = "selected_language";
    private static final String PREF_USE_SYSTEM_LANGUAGE = "use_system_language";

    // Supported language codes
    public static final String LANGUAGE_ITALIAN = "it";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_GERMAN = "de";    // Planned Phase 3
    public static final String LANGUAGE_FRENCH = "fr";    // Planned Phase 3
    public static final String LANGUAGE_SYSTEM = "system";

    // Default language (Italian - primary market)
    public static final String DEFAULT_LANGUAGE = LANGUAGE_ITALIAN;

    // Dependencies
    private final Context mContext;
    private final SharedPreferences mPreferences;

    // Current locale state
    private Locale mCurrentLocale;
    private boolean mUseSystemLanguage;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for LocaleManager with dependency injection.
     *
     * @param context Application context for accessing resources and preferences
     */
    public LocaleManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Initialize current state
        initializeLocale();

        Log.d(TAG, "LocaleManager initialized. Current locale: " + mCurrentLocale);
    }

    // ==================== LOCALE INITIALIZATION ====================

    /**
     * Initialize locale settings from preferences or system default.
     */
    private void initializeLocale() {
        mUseSystemLanguage = mPreferences.getBoolean(PREF_USE_SYSTEM_LANGUAGE, true);

        if (mUseSystemLanguage) {
            mCurrentLocale = getSystemLocale();
        } else {
            String savedLanguage = mPreferences.getString(PREF_SELECTED_LANGUAGE, DEFAULT_LANGUAGE);
            mCurrentLocale = getLocaleFromLanguageCode(savedLanguage);
        }

        // Validate that we support this locale
        if (!isSupportedLocale(mCurrentLocale)) {
            Log.w(TAG, "Unsupported locale detected: " + mCurrentLocale + ". Falling back to default.");
            mCurrentLocale = getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
        }
    }

    // ==================== LOCALE MANAGEMENT ====================

    /**
     * Get current application locale.
     *
     * @return Current Locale object
     */
    @NonNull
    public Locale getCurrentLocale() {
        return mCurrentLocale;
    }

    /**
     * Set application language with immediate effect.
     *
     * @param languageCode Language code (e.g., "it", "en", "de", "fr")
     * @return true if language was changed successfully
     */
    public boolean setLanguage(@NonNull String languageCode) {
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return setUseSystemLanguage(true);
        }

        if (!isSupportedLanguage(languageCode)) {
            Log.w(TAG, "Attempted to set unsupported language: " + languageCode);
            return false;
        }

        Locale newLocale = getLocaleFromLanguageCode(languageCode);
        if (newLocale.equals(mCurrentLocale)) {
            return false; // No change needed
        }

        // Save preference
        mPreferences.edit()
                .putString(PREF_SELECTED_LANGUAGE, languageCode)
                .putBoolean(PREF_USE_SYSTEM_LANGUAGE, false)
                .apply();

        // Update current state
        mCurrentLocale = newLocale;
        mUseSystemLanguage = false;

        // Apply locale change
        applyLocaleChange();

        Log.d(TAG, "Language changed to: " + languageCode + " (" + newLocale + ")");
        return true;
    }

    /**
     * Set whether to use system language.
     *
     * @param useSystemLanguage true to follow system language
     * @return true if setting was changed
     */
    public boolean setUseSystemLanguage(boolean useSystemLanguage) {
        if (mUseSystemLanguage == useSystemLanguage) {
            return false; // No change needed
        }

        mUseSystemLanguage = useSystemLanguage;

        if (useSystemLanguage) {
            Locale systemLocale = getSystemLocale();
            if (!systemLocale.equals(mCurrentLocale)) {
                mCurrentLocale = isSupportedLocale(systemLocale) ?
                        systemLocale : getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
                applyLocaleChange();
            }
        }

        // Save preference
        mPreferences.edit()
                .putBoolean(PREF_USE_SYSTEM_LANGUAGE, useSystemLanguage)
                .apply();

        Log.d(TAG, "Use system language: " + useSystemLanguage + ". Current locale: " + mCurrentLocale);
        return true;
    }

    /**
     * Check if currently using system language setting.
     *
     * @return true if following system language
     */
    public boolean isUsingSystemLanguage() {
        return mUseSystemLanguage;
    }

    // ==================== LOCALE UTILITIES ====================

    /**
     * Get system default locale.
     *
     * @return System default Locale
     */
    @NonNull
    private Locale getSystemLocale() {
        LocaleList localeList = mContext.getResources().getConfiguration().getLocales();
        if (!localeList.isEmpty()) {
            // Find first supported locale in system preferences
            for (int i = 0; i < localeList.size(); i++) {
                Locale locale = localeList.get(i);
                if (isSupportedLocale(locale)) {
                    return locale;
                }
            }
        }

        // Fallback for older Android versions or no supported locale found
        Locale systemLocale = Locale.getDefault();
        return isSupportedLocale(systemLocale) ? systemLocale :
                getLocaleFromLanguageCode(DEFAULT_LANGUAGE);
    }

    /**
     * Convert language code to Locale object.
     *
     * @param languageCode Language code (e.g., "it", "en")
     * @return Corresponding Locale object
     */
    @NonNull
    private Locale getLocaleFromLanguageCode(@NonNull String languageCode) {
        switch (languageCode.toLowerCase()) {
            case LANGUAGE_ITALIAN:
                return new Locale("it", "IT");
            case LANGUAGE_ENGLISH:
                return new Locale("en", "US"); // Default to US English
            case LANGUAGE_GERMAN:
                return new Locale("de", "DE");
            case LANGUAGE_FRENCH:
                return new Locale("fr", "FR");
            default:
                Log.w(TAG, "Unknown language code: " + languageCode + ". Using default.");
                return new Locale("it", "IT"); // Default to Italian
        }
    }

    /**
     * Check if a language code is supported.
     *
     * @param languageCode Language code to check
     * @return true if supported
     */
    public boolean isSupportedLanguage(@Nullable String languageCode) {
        if (languageCode == null) return false;

        return getSupportedLanguages().contains(languageCode.toLowerCase());
    }

    /**
     * Check if a locale is supported.
     *
     * @param locale Locale to check
     * @return true if supported
     */
    public boolean isSupportedLocale(@Nullable Locale locale) {
        if (locale == null) return false;

        return isSupportedLanguage(locale.getLanguage());
    }

    /**
     * Get list of supported language codes.
     *
     * @return List of supported language codes
     */
    @NonNull
    public List<String> getSupportedLanguages() {
        return Arrays.asList(
                LANGUAGE_ITALIAN        // Primary - complete
                //LANGUAGE_ENGLISH      // Phase 2 - planned
                // LANGUAGE_GERMAN,     // Phase 3 - planned
                // LANGUAGE_FRENCH      // Phase 3 - planned
        );
    }

    /**
     * Get list of all planned languages (including future phases).
     *
     * @return List of all planned language codes
     */
    @NonNull
    public List<String> getAllPlannedLanguages() {
        return Arrays.asList(
                LANGUAGE_ITALIAN,
                LANGUAGE_ENGLISH,   // Phase 2
                LANGUAGE_GERMAN,    // Phase 3
                LANGUAGE_FRENCH     // Phase 3
        );
    }

    // ==================== LOCALE APPLICATION ====================

    /**
     * Apply locale change to application context.
     * This method updates the app's resources to reflect the new locale.
     */
    private void applyLocaleChange() {
        Resources resources = mContext.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        configuration.setLocale( mCurrentLocale );

        // Update configuration
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Set as default for the JVM (affects date formatting, etc.)
        Locale.setDefault(mCurrentLocale);

        Log.d(TAG, "Applied locale change: " + mCurrentLocale);
    }

    /**
     * Update context with current locale. Use this method to ensure
     * Activities and Services use the correct locale.
     *
     * @param context Context to update
     * @return Context with updated locale
     */
    @NonNull
    public Context updateContextLocale(@NonNull Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());

        configuration.setLocale( mCurrentLocale );

        return context.createConfigurationContext( configuration );
    }

    // ==================== LANGUAGE INFORMATION ====================

    /**
     * Get display name for a language code in current locale.
     *
     * @param languageCode Language code
     * @return Display name in current language
     */
    @NonNull
    public String getLanguageDisplayName(@NonNull String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        return locale.getDisplayName(mCurrentLocale);
    }

    /**
     * Get native display name for a language code.
     *
     * @param languageCode Language code
     * @return Display name in the language itself
     */
    @NonNull
    public String getLanguageNativeName(@NonNull String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        return locale.getDisplayName(locale);
    }

    /**
     * Get current language code.
     *
     * @return Current language code (e.g., "it", "en")
     */
    @NonNull
    public String getCurrentLanguageCode() {
        return mCurrentLocale.getLanguage();
    }

    // ==================== CULTURAL ADAPTATIONS ====================

    /**
     * Check if current locale uses 24-hour time format.
     *
     * @return true if 24-hour format should be used
     */
    public boolean is24HourFormat() {
        String language = getCurrentLanguageCode();

        // Most European languages prefer 24-hour format
        // US English typically uses 12-hour format
        switch (language) {
            case LANGUAGE_ENGLISH:
                // Check if it's US English (12-hour) or UK English (24-hour)
                return !"US".equals(mCurrentLocale.getCountry());
            case LANGUAGE_ITALIAN:
            case LANGUAGE_GERMAN:
            case LANGUAGE_FRENCH:
            default:
                return true; // Default to 24-hour for European languages
        }
    }

    /**
     * Get appropriate date format pattern for current locale.
     *
     * @return Date format pattern string
     */
    @NonNull
    public String getDateFormatPattern() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                // US: MM/dd/yyyy, UK: dd/MM/yyyy
                return "US".equals(mCurrentLocale.getCountry()) ? "MM/dd/yyyy" : "dd/MM/yyyy";
            case LANGUAGE_GERMAN:
                return "dd.MM.yyyy";
            case LANGUAGE_FRENCH:
            case LANGUAGE_ITALIAN:
            default:
                return "dd/MM/yyyy";
        }
    }

    /**
     * Get quattrodue system terminology in current language.
     *
     * @return Localized name for the four-two system
     */
    @NonNull
    public String getQuattrodueTerminology() {
        String language = getCurrentLanguageCode();

        switch (language) {
            case LANGUAGE_ENGLISH:
                return "Four-Two System";
            case LANGUAGE_GERMAN:
                return "Vier-Zwei-System";
            case LANGUAGE_FRENCH:
                return "Système Quatre-Deux";
            case LANGUAGE_ITALIAN:
            default:
                return "Sistema Quattro-Due";
        }
    }

    // ==================== DEBUGGING AND UTILITIES ====================

    /**
     * Get debug information about current locale state.
     *
     * @return Debug information string
     */
    @NonNull
    public String getDebugInfo() {
        return "LocaleManager Debug Info:\n" +
                "Current Locale: " + mCurrentLocale + "\n" +
                "Language Code: " + getCurrentLanguageCode() + "\n" +
                "Use System Language: " + mUseSystemLanguage + "\n" +
                "System Locale: " + getSystemLocale() + "\n" +
                "Supported Languages: " + getSupportedLanguages() + "\n" +
                "24-Hour Format: " + is24HourFormat() + "\n" +
                "Date Pattern: " + getDateFormatPattern();
    }
}